package com.github.chrisgleissner.sandbox.log4j;

import org.apache.log4j.Level;
import org.junit.Test;
import org.slf4j.Logger;

import static com.github.chrisgleissner.sandbox.log4j.RegexFilter.getDeniedCountByLevel;
import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

public class RegexFilterTest {
    private static final Logger log = getLogger(RegexFilterTest.class);

    @Test
    public void canFilter() {
        log.info("msg");
        log.info("info message to filter"); // filter
        assertEquals(1L, (long) getDeniedCountByLevel().get(Level.INFO));

        log.warn("msg");
        log.warn("filter"); // filter
        assertEquals(1L, (long) getDeniedCountByLevel().get(Level.WARN));

        log.error("msg", new RuntimeException("filter because of this stack line")); // filter
        assertEquals(1L, (long) getDeniedCountByLevel().get(Level.ERROR));

        log.error("ignore");
        log.info("ignore"); // filter
        assertEquals(2L, (long) getDeniedCountByLevel().get(Level.INFO));
    }
}