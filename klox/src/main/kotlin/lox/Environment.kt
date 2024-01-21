package lox

class Environment(private val enclosing: Environment?) {
    private val values = mutableMapOf<String, Any?>()

    constructor(): this(null)

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun get(name: String): Any? {
        if (values.containsKey(name)) {
            return values[name]
        }
        if (enclosing != null) {
            return enclosing.get(name)
        }
        throw RuntimeException("Undefined variable '$name'.")
    }
}