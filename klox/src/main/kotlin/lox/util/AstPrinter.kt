package lox.util

import lox.*


class AstPrinter : ExprVisitor<String> {
    fun print(expr: Expr): String {
        return expr.accept(this)
    }

    override fun visitBinary(expr: Binary): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitGrouping(expr: Grouping): String {
        return parenthesize("group", expr.expression)
    }

    override fun visitLiteral(expr: Literal): String {
        return expr.value.toString();
    }

    override fun visitUnary(expr: Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()

        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ")
            builder.append(expr.accept(this))
        }
        builder.append(")")

        return builder.toString()
    }
}

fun main() {
    val expression = Binary(
        Unary(
            Token(TokenType.MINUS, "-", 0),
            Literal(123)
        ),
        Token(TokenType.STAR, "*", 0),
        Grouping(
            Literal(45.67)
        )
    )

    val printer = AstPrinter()
    println(printer.print(expression))
}