package lox

class ParseError : RuntimeException()

class Parser(val tokens: List<Token>, val errorReporter: ErrorReporter) {
    var currentTok = 0

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

    // block -> equality ( , equality )*
    private fun block(): Expr {
        var expr = equality()

        while (match(TokenType.COMMA)) {
            val operator = previous()
            val right = block()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    // equality → comparison ( ( "!=" | "==" ) comparison )* ;
    private fun equality(): Expr {
        var expr = comparison()

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    // comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
    private fun comparison(): Expr {
        var expr = term()
        while (match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            val operator: Token = previous()
            val right: Expr = term()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    // term → factor ( ( "-" | "+" ) factor )* ;
    private fun term(): Expr {
        var expr = factor()
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator: Token = previous()
            val right: Expr = factor()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    // factor → unary ( ( "/" | "*" ) unary )* ;
    private fun factor(): Expr {
        var expr = unary()
        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator: Token = previous()
            val right: Expr = factor()
            expr = Binary(expr, operator, right)
        }

        return expr
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
    //        | "(" expression ")" ;
    internal fun primary(): Expr {
        if (match(TokenType.FALSE)) return Literal(false)
        if (match(TokenType.TRUE)) return Literal(true)
        if (match(TokenType.NIL)) return Literal(null) // TODO: Should I allow this, or have my own null type?
        if (match(TokenType.NUMBER)) {
            return Literal(previous().lexeme.toDouble())
        }
        if (match(TokenType.STRING)) {
            return Literal(previous().lexeme)
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
