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
                    left.toDouble() + right.toDouble()
                } else if (left is String && right is String) {
                    left + right
                } else if (left is List<*> && right is List<*>) {
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
            TokenType.RANGE -> {
                // For now, only allow ranges of numbers
                return (getInt(expr.operator, left)..getInt(expr.operator, right)).toList()
            }
            TokenType.ELVIS -> {
                left ?: right
            }
            else -> throw error(expr.operator, "Bad Parser")
        }
    }

    override fun visitGrouping(expr: Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteral(expr: Literal): Any? {
        if (expr.value is List<*>) {
            return expr.value.map { evaluate(it as Expr) }
        }
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
        if (value is Int) {
            return value.toDouble()
        }

        return (value as? Double) ?: throw error(token, "$value is not a number")
    }

    private fun getInt(token: Token, value: Any?): Int {
        return (value as? Int) ?: throw error(token, "$value is not a number")
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