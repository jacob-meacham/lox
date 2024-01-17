package lox

import kotlin.math.exp

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

    // TODO: I need these to include a token
    // TODO: DRY up subscription and slice
    override fun visitSubscription(expr: Subscription): Any? {
        val left = evaluate(expr.left)
        val index = evaluate(expr.index)

        // Check for valid index type once, as it applies for both List and String
        if (index !is Int) throw error(null, "Can't index with $index")

        return when (left) {
            is List<*> -> left[index]
            is String -> left[index]
            else -> throw error(null, "$left is not subscriptable")
        }
    }

    // TODO: Add the slice of [4:] onto the Slice class instead?
    override fun visitSlice(expr: Slice): Any {
        val left = evaluate(expr.left)
        val start = evaluate(expr.start)
        var end = evaluate(expr.end)

        // Check for valid index type once, as it applies for both List and String
        if (start !is Int || end !is Int) throw error(null, "Can't index between $start and $end")
        return when (left) {
            // TODO: Catch
            is List<*> -> {
                if (end == -1) {
                    end = left.size
                }
                left.subList(start,end)
            }
            is String -> {
                if (end == -1) {
                    end = left.length
                }
                left.substring(start..end)
            }
            else -> throw error(null, "$left is not subscriptable")
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