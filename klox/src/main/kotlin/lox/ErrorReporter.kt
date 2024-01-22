package lox

interface ErrorReporter {
    fun error(offset: Int, length: Int, location: String, message: String)
    fun runtimeError(offset: Int, length: Int, location: String, message: String)
    fun warn(offset: Int, length: Int, location: String, message: String)

    fun numErrors(): Int
    fun numWarnings(): Int
}

class ConsoleErrorReporter(private val source: String) : ErrorReporter {
    var numErrors: Int = 0
    var numWarnings: Int = 0

    private fun getLineAndColumn(pos: Int) : Pair<Int, Int> {
        val line = source.substring(0, pos).count { it == '\n' } + 1
        val column = pos - source.lastIndexOf('\n', pos - 1)

        return line to column
    }

    private fun printError(offset: Int, length: Int, location: String, message: String) {
        val lc: Pair<Int, Int> = getLineAndColumn(offset)
        println("$location:${lc.first}:${lc.second}: $message")
        var start = (offset - 10).coerceAtLeast(0)
        for (i in start..<offset) {
            if (source[i] == '\n') {
                start = i+1
            }
        }

        var end = (offset + length + 10).coerceAtMost(source.length)
        for (i in offset..<end) {
            if (source[i] == '\n') {
                end = i
                break
            }
        }

        val sourceString = source.substring(start, end)
        println(sourceString)

        val space = " "
        val pointer = "^"
        println(space.repeat(offset-start) + pointer.repeat(length))
    }

    override fun error(offset: Int, length: Int, location: String, message: String) {
        numErrors++
        printError(offset, length, location, message)
    }

    override fun runtimeError(offset: Int, length: Int, location: String, message: String) {
        numErrors++
        printError(offset, length, location, message)
    }

    override fun warn(offset: Int, length: Int, location: String, message: String) {
        numWarnings++
        error(offset, length, location, message)
    }

    override fun numErrors(): Int {
        return numErrors
    }

    override fun numWarnings(): Int {
        return numWarnings
    }
}