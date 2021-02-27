package com.github.chrisgleissner.sandbox.kotlin.core

import mu.KotlinLogging

object MathUtil {
    private val log = KotlinLogging.logger {}

    @JvmStatic
    fun add(i: Int, j: Int): Int {
        val sum = i + j
        log.info("$i + $j = $sum")
        return sum
    }

    @JvmStatic
    fun main(args: Array<String>) {
        for (i in 9 downTo 0 step 3) println(i)
    }
}