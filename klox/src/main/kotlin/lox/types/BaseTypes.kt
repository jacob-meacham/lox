package lox.types

import lox.Interpreter

interface LoxIterable<Any> {
    fun iterator(): Iterator<Any>
}

interface LoxIndexable<Any> {
    fun getAt(index: Int): Any
    fun getRange(start: Int, end: Int): Any
}

interface LoxCallable {
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
}