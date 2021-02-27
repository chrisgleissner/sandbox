package com.github.chrisgleissner.sandbox.kotlin

import mu.KotlinLogging

private val log = KotlinLogging.logger {}

fun getHelloString() : String {
    log.info("Hello world");
    return "Hello, world!"
}

infix fun Int.times(str: String) = str.repeat(this)

class Hello {

    fun main() {
        println(getHelloString())
    }
}


