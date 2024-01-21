package lox

interface ErrorReporter {
    fun error(offset: Int, location: String, message: String)
    fun runtimeError(offset: Int, location: String, message: String)
    fun warn(offset: Int, location: String, message: String)
}

class ConsoleErrorReporter(val source: String) : ErrorReporter {
    private fun getLineAndColumn(pos: Int) : Pair<Int, Int> {
        val line = source.substring(0, pos).count { it == '\n' } + 1
        val column = pos - source.lastIndexOf('\n', pos - 1)

        return line to column
    }

    override fun error(offset: Int, location: String, message: String) {
        val lc: Pair<Int, Int> = getLineAndColumn(offset)
        println("$location:${lc.first}:${lc.second}: $message")
    }

    override fun runtimeError(offset: Int, location: String, message: String) {
        error(offset, location, message)
    }

    override fun warn(offset: Int, location: String, message: String) {
        error(offset, location, message)
    }
}