package lox.types

import lox.Environment
import lox.Interpreter

class LoxString(value: String) : LoxIndexable<Any>, LoxInstance<String>(value) {
    init {
        fields["length"] = value.length
        methods["split"] = LoxNativeFunction(this::split)
    }

    internal fun split(interpreter: Interpreter, environment: Environment, arguments: List<Any?>): LoxArray {
        if (arguments.size != 1) {
            // TODO: Better exception handling
            throw RuntimeException()
        }

        val splitOn = arguments[0] as? LoxString ?: throw RuntimeException()
        return LoxArray(value.split(splitOn.value))
    }

    override fun getAt(index: Int): Char {
        return value[index]
    }

    override fun getRange(start: Int, end: Int): String {
        var finalEnd = end
        if (finalEnd == -1) {
            finalEnd = value.length
        }
        return value.substring(start, finalEnd)
    }
}