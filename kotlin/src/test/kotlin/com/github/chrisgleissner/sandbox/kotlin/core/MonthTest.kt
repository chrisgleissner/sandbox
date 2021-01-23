package com.github.chrisgleissner.sandbox.kotlin.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MonthTest {
    @Test
    fun toStringWorks() {
        assertThat(Month.JANUARY.toString()).isEqualTo("january")
    }
}