package lox.parser

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
    SAFE_NAVIGATION, POINT_TO,
    RANGE,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    RETURN, SUPER, THIS, TRUE, VAR, WHILE, WHEN, IN,
    CONTINUE, BREAK,

    EOF;

    companion object {
        val synchronizedTokens: Set<TokenType> = setOf(
            CLASS,
            FUN,
            VAR,
            FOR,
            IF,
            WHILE,
            RETURN
        )
    }
}
data class Token(val type: TokenType, val lexeme: String, val offset: Int, val location: String) {
    val length: Int
        get() = lexeme.length
}