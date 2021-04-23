package com.github.chrisgleissner.sandbox.kotlin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonJavaTest {

    @Test
    void likes() {
        Person john = new Person("john");
        john.likes(new Person("jill"));
        assertThat(john.getLikedPeople()).containsExactly(new Person("jill"));
    }
}
