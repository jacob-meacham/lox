package lox

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions.*

class ScannerTest {

    private lateinit var scanner: Scanner
    private lateinit var errorReporter: ErrorReporter

    @Test
    fun testSmokeTest() {
        val source = """
            // This is a line comment
            /* This is a block comment */
            /* This is a block comment
            // that continues to go
            /* here is another that is embedded */
            */
            var a = "foo";
            var b = 123.456 + 121;
            b?.test()
            var c = "foo\nbar"
            if (c == true) {
                print("true")
            } else {
                print(false)
            }
            
            for (var a in 0..9) {
                print(a)
            }
            
            @
            
            var q = switch(b) {
                "foo" -> "bar"
                "baz" -> "foo"
                else -> "default"
            }
            
            var d = nil
            var e = d ?: "null"
            while (a <= b) {
            }
            
            fun testFn(a, b, c="foo") {
                return c
            }
            /* Test
        """.trimIndent()

        val expected =
                listOf(Token(type= TokenType.VAR, lexeme="var", offset=149),
                        Token(type= TokenType.IDENTIFIER, lexeme="a", offset=153),
                        Token(type= TokenType.EQUAL, lexeme="=", offset=155),
                        Token(type= TokenType.STRING, lexeme="foo", offset=157),
                        Token(type= TokenType.SEMICOLON, lexeme=";", offset=162),
                        Token(type= TokenType.VAR, lexeme="var", offset=164),
                        Token(type= TokenType.IDENTIFIER, lexeme="b", offset=168),
                        Token(type= TokenType.EQUAL, lexeme="=", offset=170),
                        Token(type= TokenType.NUMBER, lexeme="123.456", offset=172),
                        Token(type= TokenType.PLUS, lexeme="+", offset=180),
                        Token(type= TokenType.NUMBER, lexeme="121", offset=182),
                        Token(type= TokenType.SEMICOLON, lexeme=";", offset=185),
                        Token(type= TokenType.IDENTIFIER, lexeme="b", offset=187),
                        Token(type= TokenType.SAFE_NAVIGATION, lexeme="?.", offset=188),
                        Token(type= TokenType.IDENTIFIER, lexeme="test", offset=190),
                        Token(type= TokenType.LEFT_PAREN, lexeme="(", offset=194),
                        Token(type= TokenType.RIGHT_PAREN, lexeme=")", offset=195),
                        Token(type= TokenType.NEWLINE, lexeme="", offset=196),
                        Token(type= TokenType.VAR, lexeme="var", offset=197),
                        Token(type= TokenType.IDENTIFIER, lexeme="c", offset=201),
                        Token(type= TokenType.EQUAL, lexeme="=", offset=203),
                        Token(type= TokenType.STRING, lexeme="foo\nbar", offset=205),
                        Token(type= TokenType.NEWLINE, lexeme="", offset=215),
                        Token(type= TokenType.IF, lexeme="if", offset=216),
                        Token(type= TokenType.LEFT_PAREN, lexeme="(", offset=219),
                        Token(type= TokenType.IDENTIFIER, lexeme="c", offset=220),
                        Token(type= TokenType.EQUAL_EQUAL, lexeme="==", offset=222),
                        Token(type= TokenType.TRUE, lexeme="true", offset=225),
                        Token(type= TokenType.RIGHT_PAREN, lexeme=")", offset=229),
                        Token(type= TokenType.LEFT_BRACE, lexeme="{", offset=231),
                        Token(type= TokenType.PRINT, lexeme="print", offset=237),
                        Token(type= TokenType.LEFT_PAREN, lexeme="(", offset=242),
                        Token(type= TokenType.STRING, lexeme="true", offset=243),
                        Token(type= TokenType.RIGHT_PAREN, lexeme=")", offset=249),
                        Token(type= TokenType.NEWLINE, lexeme="", offset=250),
                        Token(type= TokenType.RIGHT_BRACE, lexeme="}", offset=251),
                        Token(type= TokenType.ELSE, lexeme="else", offset=253),
                        Token(type= TokenType.LEFT_BRACE, lexeme="{", offset=258),
                        Token(type= TokenType.PRINT, lexeme="print", offset=264),
                        Token(type= TokenType.LEFT_PAREN, lexeme="(", offset=269),
                        Token(type= TokenType.FALSE, lexeme="false", offset=270),
                        Token(type= TokenType.RIGHT_PAREN, lexeme=")", offset=275),
                        Token(type= TokenType.NEWLINE, lexeme="", offset=276),
                        Token(type= TokenType.RIGHT_BRACE, lexeme="}", offset=277),
                        Token(type= TokenType.FOR, lexeme="for", offset=280),
                        Token(type= TokenType.LEFT_PAREN, lexeme="(", offset=284),
                        Token(type= TokenType.VAR, lexeme="var", offset=285),
                        Token(type= TokenType.IDENTIFIER, lexeme="a", offset=289),
                        Token(type= TokenType.IN, lexeme="in", offset=291),
                        Token(type= TokenType.NUMBER, lexeme="0", offset=294),
                        Token(type= TokenType.RANGE, lexeme="..", offset=295),
                        Token(type= TokenType.NUMBER, lexeme="9", offset=297),
                        Token(type= TokenType.RIGHT_PAREN, lexeme=")", offset=298),
                        Token(type= TokenType.LEFT_BRACE, lexeme="{", offset=300),
                        Token(type= TokenType.PRINT, lexeme="print", offset=306),
                        Token(type= TokenType.LEFT_PAREN, lexeme="(", offset=311),
                        Token(type= TokenType.IDENTIFIER, lexeme="a", offset=312),
                        Token(type= TokenType.RIGHT_PAREN, lexeme=")", offset=313),
                        Token(type= TokenType.NEWLINE, lexeme="", offset=314),
                        Token(type= TokenType.RIGHT_BRACE, lexeme="}", offset=315),
                        Token(type= TokenType.VAR, lexeme="var", offset=321),
                        Token(type= TokenType.IDENTIFIER, lexeme="q", offset=325),
                        Token(type= TokenType.EQUAL, lexeme="=", offset=327),
                        Token(type= TokenType.SWITCH, lexeme="switch", offset=329),
                        Token(type= TokenType.LEFT_PAREN, lexeme="(", offset=335),
                        Token(type= TokenType.IDENTIFIER, lexeme="b", offset=336),
                        Token(type= TokenType.RIGHT_PAREN, lexeme=")", offset=337),
                        Token(type= TokenType.LEFT_BRACE, lexeme="{", offset=339),
                        Token(type= TokenType.STRING, lexeme="foo", offset=345),
                        Token(type= TokenType.POINT_TO, lexeme="->", offset=351),
                        Token(type= TokenType.STRING, lexeme="bar", offset=354),
                        Token(type= TokenType.NEWLINE, lexeme="", offset=359),
                        Token(type= TokenType.STRING, lexeme="baz", offset=364),
                        Token(type= TokenType.POINT_TO, lexeme="->", offset=370),
                        Token(type= TokenType.STRING, lexeme="foo", offset=373),
                        Token(type= TokenType.NEWLINE, lexeme="", offset=378),
                        Token(type= TokenType.ELSE, lexeme="else", offset=383),
                        Token(type= TokenType.POINT_TO, lexeme="->", offset=388),
                        Token(type= TokenType.STRING, lexeme="default", offset=391),
                        Token(type= TokenType.NEWLINE, lexeme="", offset=400),
                        Token(type= TokenType.RIGHT_BRACE, lexeme="}", offset=401),
                        Token(type= TokenType.VAR, lexeme="var", offset=404),
                        Token(type= TokenType.IDENTIFIER, lexeme="d", offset=408),
                        Token(type= TokenType.EQUAL, lexeme="=", offset=410),
                        Token(type= TokenType.NIL, lexeme="nil", offset=412),
                        Token(type= TokenType.NEWLINE, lexeme="", offset=415),
                        Token(type= TokenType.VAR, lexeme="var", offset=416),
                        Token(type= TokenType.IDENTIFIER, lexeme="e", offset=420),
                        Token(type= TokenType.EQUAL, lexeme="=", offset=422),
                        Token(type= TokenType.IDENTIFIER, lexeme="d", offset=424),
                        Token(type= TokenType.ELVIS, lexeme="?:", offset=426),
                        Token(type= TokenType.STRING, lexeme="null", offset=429),
                        Token(type= TokenType.NEWLINE, lexeme="", offset=435),
                        Token(type= TokenType.WHILE, lexeme="while", offset=436),
                        Token(type= TokenType.LEFT_PAREN, lexeme="(", offset=442),
                        Token(type= TokenType.IDENTIFIER, lexeme="a", offset=443),
                        Token(type= TokenType.LESS_EQUAL, lexeme="<=", offset=445),
                        Token(type= TokenType.IDENTIFIER, lexeme="b", offset=448),
                        Token(type= TokenType.RIGHT_PAREN, lexeme=")", offset=449),
                        Token(type= TokenType.LEFT_BRACE, lexeme="{", offset=451),
                        Token(type= TokenType.RIGHT_BRACE, lexeme="}", offset=453),
                        Token(type= TokenType.FUN, lexeme="fun", offset=456),
                        Token(type= TokenType.IDENTIFIER, lexeme="testFn", offset=460),
                        Token(type= TokenType.LEFT_PAREN, lexeme="(", offset=466),
                        Token(type= TokenType.IDENTIFIER, lexeme="a", offset=467),
                        Token(type= TokenType.COMMA, lexeme=",", offset=468),
                        Token(type= TokenType.IDENTIFIER, lexeme="b", offset=470),
                        Token(type= TokenType.COMMA, lexeme=",", offset=471),
                        Token(type= TokenType.IDENTIFIER, lexeme="c", offset=473),
                        Token(type= TokenType.EQUAL, lexeme="=", offset=474),
                        Token(type= TokenType.STRING, lexeme="foo", offset=475),
                        Token(type= TokenType.RIGHT_PAREN, lexeme=")", offset=480),
                        Token(type= TokenType.LEFT_BRACE, lexeme="{", offset=482),
                        Token(type= TokenType.RETURN, lexeme="return", offset=488),
                        Token(type= TokenType.IDENTIFIER, lexeme="c", offset=495),
                        Token(type= TokenType.NEWLINE, lexeme="", offset=496),
                        Token(type= TokenType.RIGHT_BRACE, lexeme="}", offset=497),
                        Token(type= TokenType.EOF, lexeme="", offset=506))

        errorReporter = TestErrorReporter()  // Assumes default constructor for ErrorReporter.
        scanner = Scanner("test", source, errorReporter)
        val tokens: Iterable<Token> = scanner.scanTokens()
        assertIterableEquals(expected, tokens)
    }
}