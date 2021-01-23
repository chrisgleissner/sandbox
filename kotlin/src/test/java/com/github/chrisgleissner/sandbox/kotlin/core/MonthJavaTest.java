package com.github.chrisgleissner.sandbox.kotlin.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MonthJavaTest {

    @Test
    void works() {
        assertThat(Month.FEBRUARY.toString()).isEqualTo("february");
    }
}
