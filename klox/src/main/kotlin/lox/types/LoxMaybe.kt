package lox.types

class LoxMaybe(value: Any?) : LoxInstance<Any?>(value) {
    override fun get(name: String): Any? {
        if (value != null) {
            return super.get(name)
        }

        return this
    }
}