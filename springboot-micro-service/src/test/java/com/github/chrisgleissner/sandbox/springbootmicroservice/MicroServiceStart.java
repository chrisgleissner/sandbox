package com.github.chrisgleissner.sandbox.springbootmicroservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MicroServiceStart {

    @Test
    public void startAndWait() throws InterruptedException {
        Thread.sleep(Long.MAX_VALUE);
    }
}
