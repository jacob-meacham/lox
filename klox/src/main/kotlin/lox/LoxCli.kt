package lox

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.system.exitProcess

fun run(location: String, script: String) {
    val errorReporter = ConsoleErrorReporter()
    val scanner = Scanner(location, script, errorReporter)
    val tokens = scanner.scanTokens()

    val parser = Parser(tokens, errorReporter)
    val expression = parser.parse()

    val interpreter = Interpreter(errorReporter)
    try {
        expression?.let {
            println(interpreter.evaluate(it))
        }
    } catch (_: InterpreterError) {

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