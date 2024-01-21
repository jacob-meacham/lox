package lox.stdlib

import lox.Interpreter
import lox.types.LoxCallable

class Print : LoxCallable {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val sb = StringBuilder()
        arguments.forEach { sb.append(it?.toString()) }

        println(sb.toString())
        return null
    }

}