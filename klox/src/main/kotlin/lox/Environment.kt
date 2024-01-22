package lox

import lox.parser.Token

class Environment(private val enclosing: Environment?) {
    private val values = mutableMapOf<String, Any?>()

    constructor(): this(null)

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }

        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }

        throw InterpreterError(name, "Undefined variable '$name'.")
    }

    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }
        if (enclosing != null) {
            return enclosing.get(name)
        }

        throw InterpreterError(name,"Undefined variable '$name'.")
    }
}