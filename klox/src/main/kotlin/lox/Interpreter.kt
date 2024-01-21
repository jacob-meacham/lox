package lox

import lox.parser.*
import lox.types.LoxCallable

// TODO: Let's pass the environment everywhere in a map
class InterpreterError : RuntimeException()

class Break : RuntimeException()
class Continue : RuntimeException()

// TODO: Should I have the error reporter here or just throw it in the error?
// TODO: Probably just throw it
class Interpreter(private val errorReporter: ErrorReporter, private val rootEnvironment: Environment) : ExprVisitor<Any?>,
    StmtVisitor<Any?> {
    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                execute(statement, rootEnvironment)
            }
        } catch (error: InterpreterError) {
            // TODO
        }
    }

    internal fun execute(stmt: Stmt, environment: Environment): Any? {
        return stmt.accept(this, environment)
    }

    internal fun evaluate(expr: Expr, environment: Environment): Any? {
        return expr.accept(this, environment)
    }

    override fun visitBreak(stmt: lox.parser.Break, environment: Environment): Any? {
        throw Break()
    }

    override fun visitContinue(stmt: lox.parser.Continue, environment: Environment): Any? {
        throw Continue()
    }

    override fun visitExpressionStatement(stmt: ExpressionStatement, environment: Environment): Any? {
        return evaluate(stmt.expression, environment)
    }

    override fun visitForStatement(stmt: ForStatement, environment: Environment) {
        // TODO: Loop over needs to be an iterable type
        val loopOver = evaluate(stmt.loopOver, environment)
        if(loopOver !is List<*>) {
            // TODO: Better error messae
            throw this.error(stmt.loopVariable,"Can't iterate over non-iterable")
        }

        val loopEnvironment = Environment(environment)
        for (l in loopOver) {
            try {
                loopEnvironment.define(stmt.loopVariable.lexeme, l)
                evaluate(stmt.block, loopEnvironment)
            } catch (b: Break) {
                break
            } catch (c: Continue) {
                continue
            }
        }
    }

    override fun visitVarStatement(stmt: VarStatement, environment: Environment) {
        val value = stmt.initializer?.let { evaluate(it, environment) }
        environment.define(stmt.name.lexeme, value)
    }

    override fun visitBinary(expr: Binary, environment: Environment): Any? {
        val left = evaluate(expr.left, environment)
        val right = evaluate(expr.right, environment)

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
            TokenType.RANGE -> {
                // For now, only allow ranges of numbers
                // TODO: Make a range type
                val t = (getInt(expr.operator, left)..getInt(expr.operator, right)).toList()
                return t
            }
            else -> throw error(expr.operator, "Bad Parser")
        }
    }

    override fun visitCall(expr: Call, environment: Environment): Any? {
        val calleeValue: Any? = evaluate(expr.callee, environment)
        if (calleeValue !is LoxCallable) {
            throw error(expr.paren, "Can only call functions and classes")
        }
        val arguments = expr.arguments.map { it: Expr -> evaluate(it, environment) }
        return calleeValue.call(this, arguments)
    }

    override fun visitGrouping(expr: Grouping, environment: Environment): Any? {
        return evaluate(expr.expression, environment)
    }

    override fun visitBlock(expr: Block, environment: Environment): Any? {
        var final : Any? = null
        val scopedEnvironment = Environment(environment)
        for (statement in expr.statements) {
            final = execute(statement, scopedEnvironment)
        }

        return final
    }

    override fun visitWhen(expr: When, environment: Environment): Any? {
        val initValue = expr.initializer?.let {
            evaluate(expr.initializer, environment)
        }

        for (case in expr.cases) {
            val caseVal = evaluate(case.first, environment)
            if ((initValue != null && initValue == caseVal) || caseVal == true) {
                return evaluate(case.second, environment)
            }
        }

        expr.catchall?.let {
            return evaluate(expr.catchall, environment)
        }

        throw error("Fell through without an else branch")
    }

    override fun visitLiteral(expr: Literal, environment: Environment): Any? {
        if (expr.value is List<*>) {
            return expr.value.map { evaluate(it as Expr, environment) }
        }
        return expr.value
    }

    override fun visitUnary(expr: Unary, environment: Environment): Any {
        val right = evaluate(expr.right, environment)

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

    override fun visitVariableExpression(expr: VariableExpression, environment: Environment): Any? {
        return environment.get(expr.name.lexeme)
    }

    // TODO: I need these to include a token
    // TODO: DRY up subscription and slice
    override fun visitSubscription(expr: Subscription, environment: Environment): Any? {
        val left = evaluate(expr.left, environment)
        val index = evaluate(expr.index, environment)

        // Check for valid index type once, as it applies for both List and String
        if (index !is Int) throw error(null, "Can't index with $index")

        return when (left) {
            is List<*> -> left[index]
            is String -> left[index]
            else -> throw error(null, "$left is not subscriptable")
        }
    }

    // TODO: Add the slice of [4:] onto the Slice class instead?
    override fun visitSlice(expr: Slice, environment: Environment): Any {
        val left = evaluate(expr.left, environment)
        val start = evaluate(expr.start, environment)
        var end = evaluate(expr.end, environment)

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

    internal fun error(at: Token?, message: String): InterpreterError {
        // TODO: We need to be able to get the column from the source. Probably the error reporter just holds the source?
        // TODO: Location also wrong
        at?.let { errorReporter.runtimeError(it.offset, "Interpreter", message) }
        return InterpreterError()
    }
}