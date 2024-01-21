package lox.parser

import lox.ErrorReporter

class Scanner(val location: String, val source: String, val errorReporter: ErrorReporter) {
    companion object {
        val reservedIdentifiers: HashMap<String, TokenType> = hashMapOf(
                "and" to TokenType.AND,
                "class" to TokenType.CLASS,
                "else" to TokenType.ELSE,
                "false" to TokenType.FALSE,
                "true" to TokenType.TRUE,
                "for" to TokenType.FOR,
                "fun" to TokenType.FUN,
                "if" to TokenType.IF,
                "nil" to TokenType.NIL,
                "or" to TokenType.OR,
                "return" to TokenType.RETURN,
                "super" to TokenType.SUPER,
                "switch" to TokenType.SWITCH,
                "this" to TokenType.THIS,
                "in" to TokenType.IN,
                "var" to TokenType.VAR,
                "while" to TokenType.WHILE
        )
    }

    private var startPos = 0
    private var currentPos = 0
    private var newlineRelevant = false

    private fun advance() : Char  {
        return source[currentPos++]
    }

    private fun peek(lookahead: Int = 0) : Char? {
        return source.getOrNull(currentPos + lookahead)
    }

    private fun match(s: Char) : Boolean {
        if (peek()?.let { it == s } == true) {
            advance()
            return true
        }

        return false
    }

    private fun scanLineComment() {
        while (peek()?.let { it != '\n' } == true) {
            advance()
        }
    }

    private fun scanBlockComment() {
        var level = 1
        while (true) {
            if (level == 0) {
                break
            }

            when (peek()) {
                '*' -> {
                    advance()
                    if (match('/')) {
                        level--
                    }
                }

                '/' -> {
                    advance()
                    if (match('*')) {
                        level++
                    }
                }

                null -> {
                    errorReporter.error(currentPos, location, "Unterminated block comment")
                    break
                }

                else -> {
                    advance()
                }
            }
        }
    }

    private fun scanStringLiteral(): Token? {
        val sb = StringBuilder()
        while (peek()?.let { it: Char -> it != '"' } == true) {
            when (peek()) {
                null -> {
                    errorReporter.error(currentPos, location, "Unterminated string literal")
                    return null
                }

                '\n' -> {
                    errorReporter.error(currentPos, location, "Newline in string literal")
                    return null
                }

                '\\' -> {
                    advance()
                    when (peek()) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        else -> {
                            errorReporter.error(currentPos, location, "Invalid escape sequence")
                            return null
                        }
                    }
                    advance()
                }

                else -> sb.append(advance())
            }
        }

        // Final quote
        advance()
        val lexeme = sb.toString()

        return Token(TokenType.STRING, lexeme, startPos)
    }

    private fun scanIdentifier(c: Char): Token {
        val sb = StringBuilder()
        sb.append(c)
        while (peek()?.let { it: Char -> it in 'a'..'z' || it in 'A'..'Z' || it == '_' || it in '0'..'9' } == true) {
            sb.append(advance())
        }

        val lexeme = sb.toString()
        return reservedIdentifiers.get(lexeme)?.let { Token(it, lexeme, startPos) } ?: Token(TokenType.IDENTIFIER, lexeme, startPos)
    }

    private fun scanNumber(): Token {
        while (peek()?.let { it in '0'..'9' } == true) {
            advance()
        }

        if (peek() == '.' && peek(1) in '0'..'9') {
            // Consume the "."
            advance()

            while (peek()?.let { it in '0'..'9' } == true) {
                advance()
            }
        }

        return Token(TokenType.NUMBER, source.substring(startPos, currentPos), startPos)
     }

    private fun simpleToken(tokenType: TokenType): Token {
        return Token(tokenType, source.substring(startPos..<currentPos).toString(), startPos)
    }

    // Not all lexemes turn into Tokens
    private fun scanNextLexeme() : Token? {
        val token = when (val c = advance()) {
            '(' -> simpleToken(TokenType.LEFT_PAREN)
            ')' -> simpleToken(TokenType.RIGHT_PAREN)
            '{' -> simpleToken(TokenType.LEFT_BRACE)
            '}' -> simpleToken(TokenType.RIGHT_BRACE)
            '[' -> simpleToken(TokenType.LEFT_BRACKET)
            ']' -> simpleToken(TokenType.RIGHT_BRACKET)
            ',' -> simpleToken(TokenType.COMMA)
            '.' -> simpleToken(if (match('.')) TokenType.RANGE else TokenType.DOT)
            '-' -> simpleToken(if (match('>')) TokenType.POINT_TO else TokenType.MINUS)
            '+' -> simpleToken(TokenType.PLUS)
            ':' -> simpleToken(TokenType.COLON)
            ';' -> simpleToken(TokenType.SEMICOLON)
            // TODO: Not sure if these will be needed
            '\n' -> if (newlineRelevant) Token(TokenType.NEWLINE, "", startPos) else null
            '!' -> simpleToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '=' -> simpleToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> simpleToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> simpleToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)
            '?' -> {
                if (match(':')) {
                    simpleToken(TokenType.ELVIS)
                } else if(match('.')) {
                    simpleToken(TokenType.SAFE_NAVIGATION)
                } else {
                    errorReporter.error(startPos, location, "Invalid character $c")
                    null
                }
            }
            '/' ->
                if (peek() == '/') {
                    scanLineComment()
                    null
                } else if (peek() == '*') {
                    scanBlockComment()
                    null
                } else {
                    simpleToken(TokenType.SLASH)
                }
            '*' -> simpleToken(TokenType.STAR)
            ' ', '\r', '\t' -> null
            '"' -> scanStringLiteral()
            in 'a' .. 'z', in 'A'..'Z', '_' -> scanIdentifier(c)
            in '0'.. '9' -> scanNumber()
            else -> {
                errorReporter.error(startPos, location, "Invalid character $c")
                null
            }
        }

        token?.let { setNewLineRelevant(token.type) }
        return token
    }

    fun scanTokens(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (currentPos < source.length) {
            startPos = currentPos

            scanNextLexeme()?.let { tokens.add(it) }
        }

        tokens.add(Token(TokenType.EOF, "", source.length))
        return tokens
    }

    private fun setNewLineRelevant(type: TokenType) {
        newlineRelevant = when(type) {
            TokenType.IDENTIFIER, TokenType.STRING, TokenType.NUMBER, TokenType.RIGHT_PAREN, TokenType.NIL -> true
            else -> false
        }
    }
}
