package lox

import lox.parser.Token

class Environment(private val enclosing: Environment?) {
    private val values = mutableMapOf<String, Any?>()

    constructor(): this(null)

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun assign(name: Token, value: Any?) {
        // TODO: I don't like the returns out of this
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }

        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }

        // TODO: Use error instead
        throw RuntimeException("Undefined variable '$name'.")
    }

    fun get(name: String): Any? {
        if (values.containsKey(name)) {
            return values[name]
        }
        if (enclosing != null) {
            return enclosing.get(name)
        }
        // TODO: Use error instead
        throw RuntimeException("Undefined variable '$name'.")
    }
}