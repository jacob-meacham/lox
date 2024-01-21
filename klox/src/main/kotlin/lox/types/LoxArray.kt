package lox.types

import lox.Environment
import lox.Interpreter


// TODO: Instead of Any everywhere, should create a Value typedef for values vs. expressions/statements
// TODO: Could probably do some more interesting DRY here
class LoxArray(list: List<Any?>) : LoxInstance<List<Any?>>(list), LoxIndexable<Any> {
    init {
        fields["length"] = this.value.size
        methods["map"] = LoxNativeFunction(this::map)
        methods["filter"] = LoxNativeFunction(this::filter)
        methods["fold"] = LoxNativeFunction(this::fold)
        methods["scan"] = LoxNativeFunction(this::scan)
    }

    internal fun map(interpreter: Interpreter, environment: Environment, arguments: List<Any?>): LoxArray {
        if (arguments.size != 1) {
            // TODO: Better exception handling
            throw RuntimeException()
        }

        val mapFn = arguments[0] as? LoxFunction ?: throw RuntimeException()
        val mappedList = mutableListOf<Any?>()
        for (item in value) {
            val result = mapFn.call(interpreter, environment, listOf(item))
            mappedList.add(result)
        }

        return LoxArray(mappedList)
    }

    internal fun filter(interpreter: Interpreter, environment: Environment, arguments: List<Any?>): LoxArray {
        if (arguments.size != 1) {
            throw RuntimeException()
        }

        val filterFn: LoxFunction = arguments[0] as? LoxFunction ?: throw RuntimeException()
        val filteredList: MutableList<Any?> = mutableListOf()
        for (item: Any? in value) {
            val result: Any? = filterFn.call(interpreter, environment, listOf(item), true)
            if (result == true) {
                filteredList.add(item)
            }
        }

        return LoxArray(filteredList)
    }

    internal fun fold(interpreter: Interpreter, environment: Environment, arguments: List<Any?>): Any? {
        if (arguments.size != 2) {
            throw RuntimeException()
        }

        val foldFn: LoxFunction = arguments[0] as? LoxFunction ?: throw RuntimeException()
        val initialValue: Any? = arguments[1]

        var accumulator: Any? = initialValue
        for (item: Any? in value) {
            accumulator = foldFn.call(interpreter, environment, listOf(accumulator, item), true)
        }

        return accumulator
    }

    internal fun scan(interpreter: Interpreter, environment: Environment, arguments: List<Any?>): Any? {
        if (arguments.size != 2) {
            throw RuntimeException()
        }

        val scanFn: LoxFunction = arguments[0] as? LoxFunction ?: throw RuntimeException()
        val initialValue: Any? = arguments[1]

        val scannedList: MutableList<Any?> = mutableListOf()
        var accumulator: Any? = initialValue
        for (item: Any? in value) {
            accumulator = scanFn.call(interpreter, environment, listOf(accumulator, item), true)
            scannedList.add(accumulator)
        }

        return LoxArray(scannedList)
    }

    override fun getAt(index: Int): Any? {
        return value[index]
    }

    override fun getRange(start: Int, end: Int): LoxArray {
        var finalEnd = end
        if (finalEnd == -1) {
            finalEnd = value.size
        }
        return LoxArray(value.subList(start, finalEnd))
    }
}