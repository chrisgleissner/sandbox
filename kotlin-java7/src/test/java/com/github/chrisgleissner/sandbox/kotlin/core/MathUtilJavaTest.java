package com.github.chrisgleissner.sandbox.kotlin.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MathUtilJavaTest {

    @Test
    public void add() {
        assertThat(MathUtil.add(1, 2)).isEqualTo(3);
    }
}
