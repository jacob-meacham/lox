package lox.parser

import lox.ErrorReporter


class ParseError : RuntimeException()

class Parser(private val tokens: List<Token>, private val errorReporter: ErrorReporter) {
    private var currentTok = 0

    // program → declaration* EOF ;
    fun parse(): List<Stmt> {
        val statements: MutableList<Stmt> = ArrayList()
        while (!isEOF()) {
            declaration()?.let { statements.add(it) }
        }

        return statements
    }

    // declaration    → varDecl
    //               | statement ;
    private fun declaration(): Stmt? {
        try {
            val stmt = when {
                match(TokenType.VAR) -> varDeclaration()
                match(TokenType.FOR) -> forStatement()
                match(TokenType.FUN) -> functionStatement()
                match(TokenType.RETURN) -> returnStatement()
                match(TokenType.BREAK) -> BreakStatement(previous())
                match(TokenType.CONTINUE) -> ContinueStatement(previous())
                //match(TokenType.WHILE) -> whileStatement()
                else -> statement()
            }

            // TODO: Write a consume for this
            while(match(TokenType.SEMICOLON, TokenType.NEWLINE)) {
                // any number of newlines or semicolons are OK after a statement
            }

            return stmt
        } catch (error: ParseError) {
            synchronize()
            return null
        }
    }

    private fun varDeclaration(): Stmt {
        val name: Token = consume(TokenType.IDENTIFIER)

        val initializer = when(match(TokenType.EQUAL)) {
            true -> expression()
            false -> null
        }

        // TODO: Turn this into an expect() instead of match() (like consume)
        if (!match(TokenType.SEMICOLON, TokenType.NEWLINE)) {
            throw error(peek(), "Unexpected tokens use ';' to separate expressions on the same line)")
        }

        return VarStatement(name, initializer)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        var value: Expr? = null
        if (!check(TokenType.SEMICOLON) && !check(TokenType.NEWLINE)) {
            value = expression()
        }

        return ReturnStatement(keyword, value)
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN)
        val loopVariable: Token = consume(TokenType.IDENTIFIER)
        consume(TokenType.IN)

        val loopOver: Expr = expression()
        consume(TokenType.RIGHT_PAREN)
        consume(TokenType.LEFT_BRACE) // TODO: Should this happen here? I want to force braces
        val block = block()

        return ForStatement(loopVariable, loopOver, block)
    }

    private fun statement(): Stmt {
        return expressionStatement()
    }

    private fun expressionStatement(): ExpressionStatement {
        val expr = expression()
        if (!match(TokenType.NEWLINE, TokenType.SEMICOLON)) {
            throw error(peek(), "Unexpected tokens (use ';' to separate expressions on the same line)")
        }
        return ExpressionStatement(expr)
    }

    // expression → block
    private fun expression(): Expr {
        return when {
            match(TokenType.LEFT_BRACE) -> block() // TODO: Is this the right place for this?
            else -> assignment()
        }
    }

    private fun whenExpr(): Expr {
        var initializer: Expr? = null
        if (match(TokenType.LEFT_PAREN)) {
            initializer = expression()
            consume(TokenType.RIGHT_PAREN)
        }

        consume(TokenType.LEFT_BRACE)

        var catchall: Expr? = null
        val cases: MutableList<CasePair> = ArrayList()
        while (!check(TokenType.RIGHT_BRACE) && !isEOF()) {
            when {
                match(TokenType.IN) -> {
                    // TODO: Do an IN match
                }

                match(TokenType.ELSE) -> {
                    consume(TokenType.POINT_TO)
                    catchall = expressionStatement().expression
                    break
                }

                else -> {
                    val condition = expression()
                    consume(TokenType.POINT_TO)
                    val then = expressionStatement()

                    cases.add(Pair(condition, then.expression))
                }
            }
        }

        consume(TokenType.RIGHT_BRACE)

        return When(initializer, cases, catchall)
    }

//    private fun ifExpr(): Expr {
//        consume(TokenType.LEFT_PAREN)
//        var condition = expression()
//    }

    private fun functionStatement(): Stmt {
        val name = consume(TokenType.IDENTIFIER)
        val parameters = functionParameters()

        consume(TokenType.LEFT_BRACE)
        val body = block()
        return FunctionStatement(name, parameters, body)
    }

    private fun functionExpression(): Expr {
        val parameters = functionParameters()

        consume(TokenType.LEFT_BRACE)
        val body = block()
        return FunctionExpression(parameters, body)
    }

    private fun functionParameters(): List<Token> {
        val parameters: MutableList<Token> = ArrayList()
        if (match(TokenType.LEFT_PAREN)) {
            if (!check(TokenType.RIGHT_PAREN)) {
                do {
                    parameters.add(consume(TokenType.IDENTIFIER))
                } while (match(TokenType.COMMA))
            }
            consume(TokenType.RIGHT_PAREN)
        }

        return parameters
    }

    private fun block(): Block {
        val statements: MutableList<Stmt> = ArrayList()
        while (!check(TokenType.RIGHT_BRACE) && !isEOF()) {
            declaration()?.let { it: Stmt -> statements.add(it) }
        }

        // TODO: Make consumer throw better errors
        consume(TokenType.RIGHT_BRACE)

        return Block(statements)
    }

    private fun binaryZeroOrMore(nextGrammar: () -> (Expr), vararg tokenTypes: TokenType): Expr {
        var expr = nextGrammar()
        while (match(*tokenTypes)) {
            val operator = previous()
            val right = nextGrammar()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    // assignment → IDENTIFIER "=" assignment
    //            | equality ;

    private fun assignment(): Expr {
        val expr = equality()
        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is VariableExpression) {
                val name: Token = expr.name
                return Assign(name, value)
            }

            error(equals, "Invalid assignment target.")
        }

        return expr
    }

    // equality → comparison ( ( "!=" | "==" ) comparison )* ;
    private fun equality(): Expr {
        return binaryZeroOrMore(this::comparison, TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)
    }

    // comparison → range ( ( ">" | ">=" | "<" | "<=" ) range )* ;
    private fun comparison(): Expr {
        return binaryZeroOrMore(this::range,
            TokenType.LESS,
            TokenType.LESS_EQUAL,
            TokenType.GREATER,
            TokenType.GREATER_EQUAL
        )
    }

    // range → term ( ".." ) term ;
    private fun range(): Expr {
        return binaryZeroOrMore(this::term, TokenType.RANGE)
    }

    // term → factor ( ( "-" | "+" ) factor )* ;
    private fun term(): Expr {
        return binaryZeroOrMore(this::factor, TokenType.MINUS, TokenType.PLUS)
    }

    // factor → unary ( ( "/" | "*" ) unary )* ;
    private fun factor(): Expr {
        return binaryZeroOrMore(this::unary, TokenType.SLASH, TokenType.STAR)
    }

    // unary → ( "!" | "-" ) unary
    //       | subscription
    internal fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator: Token = previous()
            val right: Expr = unary()
            return Unary(operator, right)
        }

        return subscription()
    }

    // TODO: Refactor this, it's ugly
    // subscription → call("["expression"]" | "["expression:expression"]")*
    internal fun subscription(): Expr {
        var expr = call()
        while (match(TokenType.LEFT_BRACKET)) {
            val left = previous()
            if (match(TokenType.COLON)) {
                // A slice of the form [:expr]
                val end: Expr = expression()
                val right = consume(TokenType.RIGHT_BRACKET)
                expr = Slice(expr, left, Literal(0), end, right)
                continue
            }

            val start = expression()
            if (match(TokenType.COLON)) {
                if (peek()?.type == TokenType.RIGHT_BRACKET) {
                    // A slice of the form [expr:]
                    consume(TokenType.RIGHT_BRACKET)
                    expr = Slice(expr, left, start, Literal(-1), previous())
                    continue
                }

                // A slice of the form [expr:expr]
                val end: Expr = expression()
                val right = consume(TokenType.RIGHT_BRACKET)
                expr = Slice(expr, left, start, end, right)
                continue
            } else {
                val right = consume(TokenType.RIGHT_BRACKET)
                expr = Subscription(expr, left, start, right)
            }
        }

        return expr
    }

    // TODO: Should move above subscription
    internal fun call(): Expr {
        var expr: Expr = primary()

        while (true) {
            when {
                // TODO: I need to write the grammar out here.
                match(TokenType.LEFT_PAREN) -> expr = finishCall(expr)
                // TODO: DRY this all up
                match(TokenType.LEFT_BRACE) -> {
                    val body = block()

                    // TODO: previous?
                    // TODO: Location
                    return Call(expr, previous(), listOf(FunctionExpression(listOf(Token(TokenType.IDENTIFIER,"it", previous().offset, "parser")), body)))
                }
                match(TokenType.DOT) -> {
                    val name = consume(TokenType.IDENTIFIER)
                    expr = Get(expr, false, name)
                }
                match(TokenType.SAFE_NAVIGATION) -> {
                    val name = consume(TokenType.IDENTIFIER)
                    expr = Get(expr, true, name)
                }
                else -> break
            }
        }

        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments: MutableList<Expr> = ArrayList()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }

        // TODO: Make error messages better out of consume here
        val paren = consume(TokenType.RIGHT_PAREN)

        if (match(TokenType.LEFT_BRACE)) {
            arguments.add(block())
        }

        return Call(callee, paren, arguments)
    }

    //primary → NUMBER | STRING | "true" | "false" | "nil"
    //        | "[" expression ( "," expression)* "]"
    //        | "(" expression ")" ;
    internal fun primary(): Expr {
        if (match(TokenType.FALSE)) return Literal(false)
        if (match(TokenType.TRUE)) return Literal(true)
        if (match(TokenType.NIL)) return Literal(null)
        if (match(TokenType.STRING)) {
            return Literal(previous().lexeme)
        }
        if (match(TokenType.NUMBER)) {
            if ('.' in previous().lexeme) {
                return Literal(previous().lexeme.toDouble())
            }
            return Literal(previous().lexeme.toInt())
        }

        if (match(TokenType.IDENTIFIER)) {
            return VariableExpression(previous())
        }

        // Non-lazy list
        // TODO: Is there a better way to do this? Seems like this is an expression?
        if (match(TokenType.LEFT_BRACKET)) {
            val exprList = mutableListOf<Expr>()
            while (!check(TokenType.RIGHT_BRACKET) && !check(TokenType.EOF)) {
                val prev = expression()
                exprList.add(prev)

                if (!match(TokenType.COMMA)) {
                    break
                }
            }
            consume(TokenType.RIGHT_BRACKET)
            return Literal(exprList)
        }

        if (match(TokenType.LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN)
            return Grouping(expr)
        }

//        if (match(TokenType.IF)) {
//            return ifExpr()
//        }

        if (match(TokenType.WHEN)) {
            return whenExpr()
        }

        if (match(TokenType.FUN)) {
            return functionExpression()
        }

        throw error(peek(), "Expected expression")
    }

    internal fun synchronize() {
        advance()

        while (true) {
            if (previous().type == TokenType.SEMICOLON || previous().type == TokenType.NEWLINE) {
                return
            }

            when (peek()?.type) {
                in TokenType.synchronizedTokens -> return
                TokenType.EOF -> return
                else -> advance()
            }
        }
    }

    internal fun isEOF(): Boolean {
        return peek()?.let { it.type == TokenType.EOF } ?: true
    }

    internal fun match(vararg types: TokenType) : Boolean {
        val match = types.firstOrNull { check(it) }
        return match?.let {
            advance()
            true
        } ?: false
    }

    // TODO: These should probably all return Token? instead of simply Token
    internal fun check(tokenType: TokenType): Boolean {
        return peek()?.type == tokenType
    }

    internal fun advance(): Token {
        if (!isEOF()) {
            currentTok++
        }
        return tokens[currentTok]
    }

    internal fun consume(expected: TokenType): Token {
        if (check(expected)) {
            // TODO: I don't like this
            advance()
            return previous()
        } else {
            throw error(peek(), "Expecting ${expected}.")
        }
    }

    internal fun peek(lookahead: Int = 0) : Token {
        return tokens[currentTok + lookahead]
    }

    internal fun previous(): Token {
        return tokens[currentTok - 1]
    }

    private fun error(at: Token, message: String): ParseError {
        errorReporter.error(at.offset, at.length,at.location, message)
        return ParseError()
    }
}
