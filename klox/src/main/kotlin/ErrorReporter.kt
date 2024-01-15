interface ErrorReporter {
    fun error(line: Int, column: Int, location: String, message: String)
    fun warn(line: Int, column: Int, location: String, message: String)
}

class ConsoleErrorReporter : ErrorReporter {
    override fun error(line: Int, column: Int, location: String, message: String) {
        println("$location:$line:$column: $message")
    }

    override fun warn(line: Int, column: Int, location: String, message: String) {
        println("$location:$line:$column: $message")
    }
}