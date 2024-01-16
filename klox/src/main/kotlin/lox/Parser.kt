package lox

import kotlin.math.exp

class ParseError : RuntimeException()

class Parser(val tokens: List<Token>, val errorReporter: ErrorReporter) {
    var currentTok = 0
    var commasCanBeGroup = true

    fun parse(): Expr? {
        return try {
            expression()
        } catch (error: ParseError) {
            null
        }
    }

    // expression → equality
    private fun expression(): Expr {
        return block()
    }

    // TODO: Consider refactoring
    private fun binaryZeroOrMore(nextGrammar: () -> (Expr), vararg tokenTypes: TokenType): Expr {
        var expr = nextGrammar()
        while (match(*tokenTypes)) {
            val operator = previous()
            val right = nextGrammar()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    // block -> equality ( , equality )*
    private fun block(): Expr {
        var expr = elvis()

        while (commasCanBeGroup && match(TokenType.COMMA)) {
            val operator = previous()
            val right = elvis()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    // elvis → equality ( ( "?:" ) equality )* ;
    private fun elvis(): Expr {
        return binaryZeroOrMore(this::equality, TokenType.ELVIS)
    }

    // equality → comparison ( ( "!=" | "==" ) comparison )* ;
    private fun equality(): Expr {
        return binaryZeroOrMore(this::comparison, TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)
    }

    // comparison → range ( ( ">" | ">=" | "<" | "<=" ) range )* ;
    private fun comparison(): Expr {
        return binaryZeroOrMore(this::range, TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)
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
    //       | primary
    internal fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator: Token = previous()
            val right: Expr = unary()
            return Unary(operator, right)
        }

        return primary()
    }

    //primary → NUMBER | STRING | "true" | "false" | "nil"
    //        | "[" expression ( "," expression)* "]"
    //        | "(" expression ")" ;
    internal fun primary(): Expr {
        if (match(TokenType.FALSE)) return Literal(false)
        if (match(TokenType.TRUE)) return Literal(true)
        if (match(TokenType.NIL)) return Literal(null) // TODO: Should I allow this, or have my own null type?
        if (match(TokenType.STRING)) {
            return Literal(previous().lexeme)
        }
        if (match(TokenType.NUMBER)) {
            if ('.' in previous().lexeme) {
                return Literal(previous().lexeme.toDouble())
            }
            return Literal(previous().lexeme.toInt())
        }

        // Non-lazy list
        // TODO: Is there a better way to do this? Seems like this is an expression?
        if (match(TokenType.LEFT_BRACKET)) {
            commasCanBeGroup = false
            val exprList = mutableListOf<Expr>()
            while (!check(TokenType.RIGHT_BRACKET) && !check(TokenType.EOF)) {
                val prev = expression()
                exprList.add(prev)

                if (!match(TokenType.COMMA)) {
                    break
                }
            }
            consume(TokenType.RIGHT_BRACKET)
            commasCanBeGroup = true
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

    internal fun advance(): Token? {
        currentTok++
        return tokens.getOrNull(currentTok)
    }

    internal fun consume(expected: TokenType) {
        if (check(expected)) {
            advance()
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
        at?.let { errorReporter.error(it.offset, it.length, "Parser", message) }
        return ParseError()
    }
}
