package lox.types

import lox.Environment
import lox.Interpreter

class LoxNativeFunction(private val f: (interpreter: Interpreter, environment: Environment, arguments: List<Any?>) -> Any?) : LoxCallable {
    override fun call(interpreter: Interpreter, environment: Environment, arguments: List<Any?>, unbox: Boolean): Any? {
        return f(interpreter, environment, arguments)
    }
}

open class LoxInstance<T>(val value: T) {
    protected val fields = mutableMapOf<String, Any?>()
    protected val methods = mutableMapOf<String, LoxCallable>()

    init {
        methods["let"] = LoxNativeFunction(::let)
    }

    open fun get(name: String): Any? {
        if (fields.containsKey(name)) {
            return fields[name]
        }

        if (methods.containsKey(name)) {
            return methods[name]
        }

        return null
    }

    private fun getInstanceValue(): T {
        return value
    }

    override fun toString(): String {
        return value.toString()
    }

    internal fun let(interpreter: Interpreter, environment: Environment, arguments: List<Any?>): Any? {
        if (arguments.size != 1) {
            // TODO: Better exception handling
            throw RuntimeException()
        }

        val letFn = arguments[0] as? LoxFunction ?: throw RuntimeException()
        return letFn.call(interpreter, environment, listOf(getInstanceValue()))
    }
}

interface LoxIndexable<Any> {
    fun getAt(index: Int): Any?
    fun getRange(start: Int, end: Int): Any
}

interface LoxCallable {
    fun call(interpreter: Interpreter, environment: Environment, arguments: List<Any?>, unbox: Boolean = false): Any?
}