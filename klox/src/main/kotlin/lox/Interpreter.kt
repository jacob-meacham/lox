package lox

import lox.parser.*
import lox.types.*


// TODO: Let's pass the environment everywhere in a map
class InterpreterError : RuntimeException()

class Break : RuntimeException()
class Continue : RuntimeException()

class Return(val value: Any?) : RuntimeException()

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

    // TODO: Should we split this for those that know they want unboxed?
    internal fun evaluate(expr: Expr, environment: Environment, unbox: Boolean = false): Any? {
        val result = expr.accept(this, environment)
        return if (unbox && result is LoxInstance<*>) {
            result.value
        } else {
            result
        }
    }

    override fun visitBreakStatement(stmt: BreakStatement, environment: Environment): Any? {
        throw Break()
    }

    override fun visitContinueStatement(stmt: ContinueStatement, environment: Environment): Any? {
        throw Continue()
    }

    override fun visitReturnStatement(stmt: ReturnStatement, environment: Environment): Any? {
        val ret = stmt.value?.let { evaluate(it, environment) }
        throw Return(ret)
    }

    override fun visitExpressionStatement(stmt: ExpressionStatement, environment: Environment): Any? {
        return evaluate(stmt.expression, environment)
    }

    override fun visitForStatement(stmt: ForStatement, environment: Environment) {
        // TODO: Loop over needs to be an iterable type
        val loopOver = evaluate(stmt.loopOver, environment)
        if(loopOver !is LoxArray) {
            // TODO: Better error message
            throw this.error(stmt.loopVariable,"Can't iterate over non-iterable")
        }

        val loopEnvironment = Environment(environment)
        for (l in loopOver.value) {
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

    override fun visitFunctionStatement(stmt: FunctionStatement, environment: Environment): Any? {
        val function = LoxFunction(stmt.body, stmt.params, environment)
        environment.define(stmt.name.lexeme, function)
        return null
    }

    override fun visitBinary(expr: Binary, environment: Environment): Any {
        val left = evaluate(expr.left, environment, true)
        val right = evaluate(expr.right, environment, true)

        return when (expr.operator.type) {
            TokenType.EQUAL_EQUAL -> isEqual(left, right)
            TokenType.BANG_EQUAL -> !isEqual(left, right)
            TokenType.GREATER -> {
                LoxInstance(getNumber(expr.operator, left) > getNumber(expr.operator, right))
            }
            TokenType.GREATER_EQUAL -> {
                LoxInstance(getNumber(expr.operator, left) >= getNumber(expr.operator, right))
            }
            TokenType.LESS -> {
                LoxInstance(getNumber(expr.operator, left) < getNumber(expr.operator, right))
            }
            TokenType.LESS_EQUAL -> {
                LoxInstance(getNumber(expr.operator, left) <= getNumber(expr.operator, right))
            }
            TokenType.PLUS -> {
                if (left is Number && right is Number) {
                    LoxInstance(left.toDouble() + right.toDouble())
                } else if (left is List<*> && right is List<*>) {
                    LoxArray(left + right)
                } else if (left is String || right is String) {
                    if (left is String) LoxString(left + right.toString()) else LoxString(left.toString() + right)
                } else {
                    throw error(expr.operator, "Mismatched types $left + $right")
                }
            }
            TokenType.MINUS -> {
                LoxInstance(getNumber(expr.operator, left) - getNumber(expr.operator, right))
            }
            TokenType.SLASH -> {
                LoxInstance(getNumber(expr.operator, left) / getNumber(expr.operator, right))
            }
            TokenType.STAR -> {
                LoxInstance(getNumber(expr.operator, left) * getNumber(expr.operator, right))
            }
            TokenType.RANGE -> {
                // For now, only allow ranges of numbers
                // TODO: Make a range type
                return LoxArray((getInt(expr.operator, left)..getInt(expr.operator, right)).toList())
            }
            else -> throw error(expr.operator, "Bad Parser")
        }
    }

    override fun visitCall(expr: Call, environment: Environment): Any? {
        val calleeValue: Any? = evaluate(expr.callee, environment, false)
        if (calleeValue is LoxMaybe) {
            return null // Can just return null here
        }

        if (calleeValue !is LoxCallable) {
            throw error(expr.paren, "Can only call functions and classes")
        }
        val arguments = expr.arguments.map { it: Expr -> evaluate(it, environment) }

        try {
            return calleeValue.call(this, environment, arguments)
        } catch (ret: Return) {
            return ret.value
        }
    }

    override fun visitGet(expr: Get, environment: Environment): Any? {
        val v = evaluate(expr.obj, environment, false) as LoxInstance<*>

        if (v.value == null) {
            if (expr.safe) {
                // TODO: This isn't really a Maybe type...
                return LoxMaybe(v)
            }

            throw error(expr.name, "is null")
        }

        return v.get(expr.name.lexeme) ?: throw error(expr.name, "No method named ${expr.name.lexeme}")
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

    override fun visitIfElse(expr: IfElse, environment: Environment): Any? {
        TODO("Not yet implemented")
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

    override fun visitFunctionExpression(expr: FunctionExpression, environment: Environment): Any? {
        return LoxFunction(expr.body, expr.params, environment)
    }

    override fun visitLiteral(expr: Literal, environment: Environment): Any? {
        return when(expr.value) {
            is List<*> -> LoxArray(expr.value.map { evaluate(it as Expr, environment) })
            is String -> LoxString(expr.value)
            else -> LoxInstance(expr.value)
        }
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

    override fun visitAssign(expr: Assign, environment: Environment) {
        environment.assign(expr.name, evaluate(expr.value, environment))
    }

    override fun visitVariableExpression(expr: VariableExpression, environment: Environment): Any? {
        return environment.get(expr.name.lexeme)
    }

    // TODO: I need these to include a token
    // TODO: DRY up subscription and slice
    override fun visitSubscription(expr: Subscription, environment: Environment): Any? {
        val left = evaluate(expr.left, environment)
        val index = evaluate(expr.index, environment, true)

        // Check for valid index type once, as it applies for both List and String
        if (index !is Int) throw error(expr.startBracket, "Can't index with $index")

        return when (left) {
            is LoxIndexable<*> -> left.getAt(index)
            else -> throw error(expr.startBracket, "$left is not subscriptable")
        }
    }

    // TODO: Add the slice of [4:] onto the Slice class instead?
    override fun visitSlice(expr: Slice, environment: Environment): Any? {
        val left = evaluate(expr.left, environment)
        val start = evaluate(expr.start, environment, true)
        val end = evaluate(expr.end, environment, true)

        // Check for valid index type once, as it applies for both List and String
        if (start !is Int || end !is Int) throw error(expr.startBracket, "Can't index between $start and $end")
        return when (left) {
            is LoxIndexable<*> -> {
                left.getRange(start, end)
            }
            else -> throw error(expr.startBracket, "$left is not subscriptable")
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

    internal fun error(at: Token, message: String): InterpreterError {
        // TODO: We need to be able to get the column from the source. Probably the error reporter just holds the source?
        // TODO: Location also wrong
        errorReporter.runtimeError(at.offset, at.length, at.location, message)
        return InterpreterError()
    }
}