package lox

enum class TokenType {
    // Single-character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,
    NEWLINE, COLON, LEFT_BRACKET, RIGHT_BRACKET,

    // One or two character tokens
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    ELVIS, SAFE_NAVIGATION, POINT_TO,
    RANGE,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE, SWITCH, IN,

    EOF;

    companion object {
        val synchronizedTokens: Set<TokenType> = setOf(
            TokenType.CLASS,
            TokenType.FUN,
            TokenType.VAR,
            TokenType.FOR,
            TokenType.IF,
            TokenType.WHILE,
            TokenType.PRINT,
            TokenType.RETURN
        )
    }
}

data class Token(val type: TokenType, val lexeme: String, val offset: Int) {
    val length: Int
        get() = lexeme.length
}