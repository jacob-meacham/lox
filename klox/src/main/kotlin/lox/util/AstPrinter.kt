package lox.util

import lox.parser.*


class AstPrinter : ExprVisitor<String> {
    fun print(expr: Expr): String {
        return expr.accept(this)
    }

    override fun visitBinary(expr: Binary): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitCall(expr: Call): String {
        TODO("Not yet implemented")
    }

    override fun visitGrouping(expr: Grouping): String {
        return parenthesize("group", expr.expression)
    }

    override fun visitBlock(expr: Block): String {
        TODO("Not yet implemented")
    }

    override fun visitLiteral(expr: Literal): String {
        return expr.value.toString();
    }

    override fun visitUnary(expr: Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    override fun visitVariableExpression(expr: VariableExpression): String {
        TODO("Not yet implemented")
    }

    override fun visitSubscription(expr: Subscription): String {
        TODO("Not yet implemented")
    }

    override fun visitSlice(expr: Slice): String {
        TODO("Not yet implemented")
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