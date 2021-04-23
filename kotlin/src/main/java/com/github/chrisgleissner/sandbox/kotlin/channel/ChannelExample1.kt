package com.github.chrisgleissner.sandbox.kotlin.channel

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking {
    val channel = Channel<Int>(10)
    launch {
        // this might be heavy CPU-consuming computation or async logic, we'll just send five squares
        for (x in 1..5) channel.send(x * x)
        channel.close()
        println("closed!")
    }
    // here we print five received integers:
    channel.consumeEach {
        println(it)
        Thread.sleep(1000)
    }
    println("Done!")
}