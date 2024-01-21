package lox.parser

import lox.*


class ParseError : RuntimeException()

class Parser(val tokens: List<Token>, val errorReporter: ErrorReporter) {
    var currentTok = 0

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
            return when {
                match(TokenType.VAR) -> varDeclaration()
                else -> statement()
            }
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
            throw error(peek(), "Unexpected tokens (use ';' to separate expressions on the same line)")
        }

        return VarStatement(name, initializer)
    }

    private fun statement(): Stmt {
        return expressionStatement()
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        if (!match(TokenType.NEWLINE, TokenType.SEMICOLON)) {
            throw error(peek(), "Unexpected tokens (use ';' to separate expressions on the same line)")
        }
        return ExpressionStatement(expr)
    }

    // expression → block
    private fun expression(): Expr {
        if (match(TokenType.LEFT_BRACE)) {
            return block()
        }

        return equality()
    }

    private fun block(): Expr {
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
            if (match(TokenType.COLON)) {
                // A slice of the form [:expr]
                val end: Expr = expression()
                consume(TokenType.RIGHT_BRACKET)
                expr = Slice(expr, Literal(0), end)
                continue
            }

            val start = expression()
            if (match(TokenType.COLON)) {
                if (peek()?.type == TokenType.RIGHT_BRACKET) {
                    // A slice of the form [expr:]
                    consume(TokenType.RIGHT_BRACKET)
                    expr = Slice(expr, start, Literal(-1))
                    continue
                }

                // A slice of the form [expr:expr]
                val end: Expr = expression()
                consume(TokenType.RIGHT_BRACKET)
                expr = Slice(expr, start, end)
                continue
            } else {
                consume(TokenType.RIGHT_BRACKET)
                expr = Subscription(expr, start)
            }
        }

        return expr
    }

    // TODO: Should move above subscription
    internal fun call(): Expr {
        var expr: Expr = primary()

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            } else {
                break
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

    internal fun peek(lookahead: Int = 0) : Token? {
        return tokens.getOrNull(currentTok + lookahead)
    }

    internal fun previous(): Token {
        return tokens[currentTok - 1]
    }

    private fun error(at: Token?, message: String): ParseError {
        // TODO: We need to be able to get the column from the source. Probably the error reporter just holds the source?
        // TODO: Location also wrong
        at?.let { errorReporter.error(it.offset, "Parser", message) }
        return ParseError()
    }
}
