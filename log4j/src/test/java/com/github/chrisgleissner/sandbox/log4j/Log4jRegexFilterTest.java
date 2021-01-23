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
        // show
        log.info("msg");
        log.warn("msg");

        // filter
        log.info("filter this");
        log.warn("filter this");
        log.error("msg", new RuntimeException("filter because of this stack line"));

        Assert.assertEquals(1L, (long) Log4jRegexFilter.getDeniedCountByLevel().get(Level.INFO));
        Assert.assertEquals(1L, (long) Log4jRegexFilter.getDeniedCountByLevel().get(Level.WARN));
        Assert.assertEquals(1L, (long) Log4jRegexFilter.getDeniedCountByLevel().get(Level.ERROR));
    }
}