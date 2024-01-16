package lox

class InterpreterError : RuntimeException()

// TODO: Should I have the error reporter here or just throw it in the error?
class Interpreter(private val errorReporter: ErrorReporter) : ExprVisitor<Any?> {
    override fun visitBinary(expr: Binary): Any? {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.EQUAL_EQUAL -> isEqual(left, right)
            TokenType.BANG_EQUAL -> !isEqual(left, right)
            TokenType.GREATER -> {
                getNumber(expr.operator, left) > getNumber(expr.operator, right)
            }
            TokenType.GREATER_EQUAL -> {
                getNumber(expr.operator, left) >= getNumber(expr.operator, right)
            }
            TokenType.LESS -> {
                getNumber(expr.operator, left) < getNumber(expr.operator, right)
            }
            TokenType.LESS_EQUAL -> {
                getNumber(expr.operator, left) <= getNumber(expr.operator, right)
            }
            TokenType.PLUS -> {
                if (left is Number && right is Number) {
                    (left as Double) + (right as Double)
                } else if (left is String && right is String) {
                    left + right
                } else {
                    throw error(expr.operator, "Mismatched types $left + $right")
                }
            }
            TokenType.MINUS -> {
                getNumber(expr.operator, left) - getNumber(expr.operator, right)
            }
            TokenType.SLASH -> {
                getNumber(expr.operator, left) / getNumber(expr.operator, right)
            }
            TokenType.STAR -> {
                getNumber(expr.operator, left) * getNumber(expr.operator, right)
            }
            TokenType.COMMA -> {
                right
            }
            else -> throw error(expr.operator, "Bad Parser")
        }
    }

    override fun visitGrouping(expr: Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteral(expr: Literal): Any? {
        return expr.value
    }

    override fun visitUnary(expr: Unary): Any {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.MINUS -> {
                -getNumber(expr.operator, right)
            }

            TokenType.BANG -> {
                isTruthy(right)
            }

            else -> throw error(expr.operator, "Bad Parser")
        }
    }

    private fun getNumber(token: Token, value: Any?): Double {
        return (value as? Double) ?: throw error(token, "$value is not a number")
    }

    private fun isEqual(left: Any?, right: Any?): Boolean {
        if (left == null && right == null) {
            return true
        }
        if (left == null) {
            return false
        }

        return left == right
    }

    private fun isTruthy(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        return true
    }

    internal fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    internal fun error(at: Token?, message: String): InterpreterError {
        // TODO: We need to be able to get the column from the source. Probably the error reporter just holds the source?
        // TODO: Location also wrong
        at?.let { errorReporter.error(it.offset, it.length, "Interpreter", message) }
        return InterpreterError()
    }
}