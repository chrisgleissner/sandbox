package com.github.chrisgleissner.sandbox.log4j;

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class Log4jRegexFilterTest {
    private static final Logger log = getLogger(Log4jRegexFilterTest.class);

    @Test
    public void canFilter() {
        log.info("info msg");
        log.info("info message");
        log.warn("warn msg");
        Assert.assertEquals(1L, (long) Log4jRegexFilter.getDeniedCountByLevel().get(Level.INFO));
        Assert.assertEquals(1L, (long) Log4jRegexFilter.getDeniedCountByLevel().get(Level.WARN));
    }
}