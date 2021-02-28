package com.github.chrisgleissner.sandbox.kotlin;

import lombok.val;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PersonJavaTest {

    @Test
    public void likes() {
        val john = new Person("john");
        john.likes(new Person("jill"));
        assertThat(john.getLikedPeople()).containsExactly(new Person("jill"));
    }
}
