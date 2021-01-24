package com.github.chrisgleissner.sandbox.log4j;

import org.junit.Test;
import org.slf4j.Logger;

import static com.github.chrisgleissner.sandbox.log4j.MessageFilter.getDeniedCount;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

public class MessageFilterTest {
    private static final Logger log = getLogger(MessageFilterTest.class);
    private int deniedCount = 0;

    @Test
    public void canFilter() throws InterruptedException {
        log.info("msg");
        assertAccepted();

        log.info("info message to filter");
        assertDenied();

        log.warn("msg");
        assertAccepted();

        log.warn("filter");
        assertDenied();

        log.error("msg", new RuntimeException("filter because of this text"));
        assertDenied();

        log.error("ignore");
        assertAccepted();

        log.info("ignore");
        assertDenied();
    }

    private void assertDenied() {
        assertEquals(++deniedCount, getDeniedCount());
    }

    private void assertAccepted() {
        assertEquals(deniedCount, getDeniedCount());
    }
}