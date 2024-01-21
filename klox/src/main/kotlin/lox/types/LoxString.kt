package lox.types

class LoxString(public val Value: String): LoxIndexable<Any>, LoxIterable<Any>  {
    override fun iterator(): Iterator<Any> {
        // TODO: Want this to support map, filter, reduce
        return Value.iterator()
    }

    override fun getAt(index: Int): Char {
        return Value[index]
    }

    override fun getRange(start: Int, end: Int): String {
        return Value.substring(start, end)
    }
}