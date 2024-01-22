package lox

import lox.parser.*
import lox.types.*

class InterpreterError(val at: Token, override val message: String) : RuntimeException()

class Break : RuntimeException()
class Continue : RuntimeException()

class Return(val value: Any?) : RuntimeException()

class Interpreter(private val errorReporter: ErrorReporter, private val rootEnvironment: Environment) : ExprVisitor<Any?>,
    StmtVisitor<Any?> {
    fun interpret(statements: List<Stmt>) {
        try {
            for (statement in statements) {
                execute(statement, rootEnvironment)
            }
        } catch (error: InterpreterError) {
            errorReporter.runtimeError(error.at.offset, error.at.length, error.at.location,
                "Interpreter Error: ${error.message}")
        } catch (error: RuntimeException) {
            errorReporter.fatalError("Fatal Error: ${error.message}")
        }
    }

    internal fun execute(stmt: Stmt, environment: Environment): Any? {
        return stmt.accept(this, environment)
    }

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
        val loopOver = evaluate(stmt.loopOver, environment)
        if(loopOver !is LoxArray) {
            throw InterpreterError(stmt.loopVariable,"Can't iterate over $loopOver")
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
                    if (left is String) LoxString(left + right.toString())
                    else LoxString(left.toString() + right)
                } else {
                    throw InterpreterError(expr.operator, "Mismatched types $left + $right")
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
                return LoxArray((getInt(expr.operator, left)..getInt(expr.operator, right)).toList())
            }
            else -> throw InterpreterError(expr.operator, "Unknown operator ${expr.operator}")
        }
    }

    override fun visitCall(expr: Call, environment: Environment): Any? {
        val calleeValue: Any? = evaluate(expr.callee, environment, false)
        if (calleeValue is LoxMaybe) {
            return null // Can just return null here
        }

        if (calleeValue !is LoxCallable) {
            throw InterpreterError(expr.paren, "Can only call functions and classes")
        }
        val arguments = expr.arguments.map { it: Expr -> evaluate(it, environment) }

        return try {
            calleeValue.call(this, environment, arguments)
        } catch (ret: Return) {
            ret.value
        }
    }

    override fun visitGet(expr: Get, environment: Environment): Any {
        val v = evaluate(expr.obj, environment, false) as LoxInstance<*>

        if (v.value == null) {
            if (expr.safe) {
                // TODO: This isn't really a Maybe type...
                return LoxMaybe(v)
            }

            throw InterpreterError(expr.name, "${expr.name} is null")
        }

        return v.get(expr.name.lexeme) ?: throw InterpreterError(expr.name, "No method named ${expr.name.lexeme}")
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

    // TODO: Dry
    override fun visitWhenExpression(expr: WhenExpression, environment: Environment): Any? {
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

        throw InterpreterError(expr.token, "When expression must be exhaustive")
    }

    override fun visitWhenStatement(stmt: WhenStatement, environment: Environment) {
        val initValue = stmt.initializer?.let {
            evaluate(stmt.initializer, environment)
        }

        for (case in stmt.cases) {
            val caseVal = evaluate(case.first, environment)
            if ((initValue != null && initValue == caseVal) || caseVal == true) {
                evaluate(case.second, environment)
            }
        }

        stmt.catchall?.let {
            evaluate(stmt.catchall, environment)
        }
    }

    override fun visitFunctionExpression(expr: FunctionExpression, environment: Environment): Any {
        return LoxFunction(expr.body, expr.params, environment)
    }

    override fun visitLiteral(expr: Literal, environment: Environment): LoxInstance<*> {
        return when(expr.value) {
            is List<*> -> LoxArray(expr.value.map { evaluate(it as Expr, environment) })
            is String -> LoxString(expr.value)
            else -> LoxInstance(expr.value)
        }
    }

    override fun visitUnary(expr: Unary, environment: Environment): Any {
        val right = evaluate(expr.right, environment, true)

        return when (expr.operator.type) {
            TokenType.MINUS -> {
                -getNumber(expr.operator, right)
            }

            TokenType.BANG -> {
                isTruthy(right)
            }

            else -> throw InterpreterError(expr.operator, "Unknown operator ${expr.operator}")
        }
    }

    override fun visitAssign(expr: Assign, environment: Environment) {
        environment.assign(expr.name, evaluate(expr.value, environment))
    }

    override fun visitVariableExpression(expr: VariableExpression, environment: Environment): Any? {
        return environment.get(expr.name)
    }

    // TODO: DRY up subscription and slice
    override fun visitSubscription(expr: Subscription, environment: Environment): Any? {
        val left = evaluate(expr.left, environment)
        val index = evaluate(expr.index, environment, true)

        // Check for valid index type once, as it applies for both List and String
        if (index !is Int) throw InterpreterError(expr.startBracket, "Can't index with $index")

        return when (left) {
            is LoxIndexable<*> -> left.getAt(index)
            else -> throw InterpreterError(expr.startBracket, "$left is not subscriptable")
        }
    }

    override fun visitSlice(expr: Slice, environment: Environment): Any? {
        val left = evaluate(expr.left, environment)
        val start = evaluate(expr.start, environment, true)
        val end = evaluate(expr.end, environment, true)

        // Check for valid index type once, as it applies for both List and String
        if (start !is Int || end !is Int) throw InterpreterError(expr.startBracket, "Can't index between $start and $end")
        return when (left) {
            is LoxIndexable<*> -> {
                left.getRange(start, end)
            }
            else -> throw InterpreterError(expr.startBracket, "$left is not subscriptable")
        }
    }

    private fun getNumber(token: Token, value: Any?): Double {
        if (value is Int) {
            return value.toDouble()
        }

        return (value as? Double) ?: throw InterpreterError(token, "$value is not a number")
    }

    private fun getInt(token: Token, value: Any?): Int {
        return (value as? Int) ?: throw InterpreterError(token, "$value is not a number")
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
}