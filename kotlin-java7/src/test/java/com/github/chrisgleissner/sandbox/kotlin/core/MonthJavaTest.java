package com.github.chrisgleissner.sandbox.kotlin.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MonthJavaTest {

    @Test
    public void works() {
        assertThat(Month.FEBRUARY.toString()).isEqualTo("february");
    }
}
