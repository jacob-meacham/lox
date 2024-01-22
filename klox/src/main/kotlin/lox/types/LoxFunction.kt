package lox.types

import lox.Environment
import lox.Interpreter
import lox.parser.Expr
import lox.parser.Token


// TODO: Add arity so we can easily check arity
class LoxFunction(private val expr: Expr, private val params: List<Token>, private val closure: Environment) : LoxCallable {
    override fun call(interpreter: Interpreter, environment: Environment, arguments: List<Any?>, unbox: Boolean): Any? {
        val funEnvironment = Environment(closure)
        for (i in params.indices) {
            funEnvironment.define(params[i].lexeme, arguments[i])
        }

        return interpreter.evaluate(expr, funEnvironment, unbox)
    }
}