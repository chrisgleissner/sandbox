package com.github.chrisgleissner.sandbox.kotlin.coroutine

import kotlinx.coroutines.*

// https://kotlinlang.org/docs/coroutines-basics.html#structured-concurrency
// outer coroutine awaits all of its nested coroutines to end
fun main() = runBlocking {
    launch {
        world()
    }
    launch {
        moon()
    }
    println("Hello,")
}

private suspend fun moon() {
    delay(550L)
    println("Moon!")
}

private suspend fun world() {
    delay(500L)
    println("World!")
}