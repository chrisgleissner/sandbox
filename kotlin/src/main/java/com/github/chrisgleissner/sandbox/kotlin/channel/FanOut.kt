package com.github.chrisgleissner.sandbox.kotlin.channel

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun main() = runBlocking {
    val producer: ReceiveChannel<Int> = produceNumbers(List(100) { it })
    repeat(10) { launchProcessor(it, producer) }
}

fun CoroutineScope.produceNumbers(ints: List<Int>) = produce(capacity = 3) { ints.forEach { send(it); delay(50) } }

fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
    for (msg in channel) {
        println("Processor #$id received $msg")
    }
}