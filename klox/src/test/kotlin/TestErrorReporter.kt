data class Issue(val line: Int, val column: Int, val location: String, val message: String, val isError: Boolean)
class TestErrorReporter : ErrorReporter {
    val issues: MutableList<Issue> = mutableListOf()
    override fun error(line: Int, column: Int, location: String, message: String) {
        issues.add(Issue(line, column, location, message, true))
    }

    override fun warn(line: Int, column: Int, location: String, message: String) {
        issues.add(Issue(line, column, location, message, false))
    }

}