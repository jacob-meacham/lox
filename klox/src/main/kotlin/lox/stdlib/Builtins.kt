package lox.stdlib

import lox.Environment
import lox.Interpreter
import lox.types.LoxCallable

class Print : LoxCallable {
    override fun call(interpreter: Interpreter, environment: Environment, arguments: List<Any?>, unbox: Boolean): Any? {
        val sb = StringBuilder()
        arguments.forEach { sb.append(it?.toString()) }

        println(sb.toString())
        return null
    }
}

class Clock : LoxCallable {
    override fun call(interpreter: Interpreter, environment: Environment, arguments: List<Any?>, unbox: Boolean): Any {
        return System.currentTimeMillis().toDouble() / 1000.0
    }
}


fun addToEnvironment(environment: Environment) {
    environment.define("print", Print())
    environment.define("clock", Clock())
}