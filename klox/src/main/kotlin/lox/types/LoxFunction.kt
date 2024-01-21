package lox.types

import lox.Environment
import lox.Interpreter
import lox.parser.Expr
import lox.parser.Token


// TODO: This should be a type returned by the parser
// TODO: Since we need the params also
// TODO: Since we bifurcated Statement and Expression, we need to bring in some type of base class here?
// TODO: Add arity so we can easily check arity
class LoxFunction(private val expr: Expr, private val params: List<Token>, private val closure: Environment) : LoxCallable {
    override fun call(interpreter: Interpreter, environment: Environment, arguments: List<Any?>, unbox: Boolean): Any? {
        for (i in params.indices) {
            environment.define(params[i].lexeme, arguments[i])
        }

        return interpreter.evaluate(expr, closure, unbox)
    }
}