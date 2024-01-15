import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.system.exitProcess

// TODO: Don't require semicolons, instead allow for \n like Go
// TODO: Add fold, scan, map, filter etc on array
// TODO: Add let like Kotlin
// TODO: Allow for x in y
// TODO: Allow for 1..10
// TODO: Add arrays and map types as builtins
// TODO: Allow array slices

fun run(location: String, script: String) {
    val errorReporter = ConsoleErrorReporter()
    val scanner = Scanner(location, script, errorReporter)
    val tokens = scanner.scanTokens()
    for (token in tokens) {
        println(token)
    }
}

fun runFile(path: String) {
    val fileContents: String = try {
        File(path).readText()
    } catch (e: Exception) {
        throw RuntimeException("An error occurred while trying to open the file: $e")
    }

    run(path, fileContents)
}

fun runPrompt() {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    while (true) {
        print("> ")
        val line = reader.readLine() ?: break
        run("REPL", line)
    }
}

fun main(args: Array<String>) {
    when (args.size) {
        0 -> runPrompt()
        1 -> runFile(args[0])
        else -> {
            println("Usage: klox [script]")
            exitProcess(2)
        }

    }
}