package com.github.chrisgleissner.sandbox.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PersonTest {

    @Test
    fun likes() {
        val john = Person("john")
        john likes Person("jill")
        assertThat(john.likedPeople).containsExactly(Person("jill"))
    }
}