package com.github.chrisgleissner.sandbox.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HelloTest {

    @Test
    fun getHelloStringWorks() {
        assertThat("Hello, world!").isEqualTo(getHelloString())
    }

    @Test
    fun timesWorks() {
        assertThat(2 times "Hello").isEqualTo("HelloHello");
    }

}

