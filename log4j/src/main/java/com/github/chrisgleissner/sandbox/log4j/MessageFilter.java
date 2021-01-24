package com.github.chrisgleissner.sandbox.log4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;
import lombok.val;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Filters messages with a certain level and a log message or stack trace matching a specified string or regex.
 */
@NoArgsConstructor
public class MessageFilter extends Filter {
    private static final AtomicLong deniedCount = new AtomicLong();
    private Config config;

    public static long getDeniedCount() {
        return deniedCount.get();
    }

    public void setConfigPaths(String s) {
        this.config = new Config(s);
    }

    public int decide(LoggingEvent event) {
        for (val filterItem : config.getFilterItems()) {
            if (filterItem.matches(event)) {
                deniedCount.incrementAndGet();
                return Filter.DENY;
            }
        }
        return Filter.NEUTRAL;
    }

    private static class Config {
        private final List<String> configPathNames;
        @Getter private List<FilterItem> filterItems;

        Config(String configPathNameString) {
            this.configPathNames = Arrays.asList(configPathNameString.split(","));
            this.filterItems = loadFilterItems();
            Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            }).scheduleAtFixedRate(new Runnable() {
                @Override public void run() {
                    Config.this.filterItems = loadFilterItems();
                }
            }, 5, 5, TimeUnit.MINUTES);
        }

        List<FilterItem> loadFilterItems() {
            val result = new ArrayList<FilterItem>();
            for (val configPathName : configPathNames) {
                try {
                    result.addAll(loadFilterItems(new File(configPathName)));
                } catch (Throwable e) {
                    LogLog.warn("Failed to read config for " + MessageFilter.class.getName() + " from " + configPathName, e);
                }
            }
            return result;
        }

        List<FilterItem> loadFilterItems(File file) throws IOException {
            val filterItems = new ArrayList<FilterItem>();
            if (file.isFile()) {
                try (val is = new FileInputStream(file)) {
                    for (val yamlFilterItem : new Yaml().<List<Map<String, Object>>>load(is)) {
                        filterItems.add(new FilterItem(yamlFilterItem));
                    }
                }
            }
            LogLog.debug("Read config for " + MessageFilter.class.getName() + " from " + file + ": " + filterItems);
            return filterItems;
        }
    }

    @Value
    static class FilterItem {
        String message;
        Level level;
        @ToString.Exclude Pattern messagePattern;
        boolean regex;
        boolean checkStackTrace;

        FilterItem(Map<String, Object> yamlFilterItem) {
            this.message = (String) yamlFilterItem.get("message");
            if (this.message == null)
                throw new IllegalArgumentException("Missing message");
            this.checkStackTrace = getBoolean(yamlFilterItem, "checkStackTrace");
            this.regex = getBoolean(yamlFilterItem, "regex");
            this.messagePattern = regex ? Pattern.compile(this.message) : null;
            this.level = Level.toLevel((String) yamlFilterItem.get("level"));
        }

        static boolean getBoolean(Map<String, Object> map, String key) {
            val value = (Boolean) map.get(key);
            return value != null && value;
        }

        boolean matches(LoggingEvent event) {
            boolean matches = false;
            if (level.equals(event.getLevel())) {
                matches = matches(event.getRenderedMessage());
                if (!matches && checkStackTrace) {
                    val throwableStrRep = event.getThrowableStrRep();
                    if (throwableStrRep != null) {
                        int i = 0;
                        while (!matches && i < throwableStrRep.length) {
                            matches = matches(throwableStrRep[i++]);
                        }
                    }
                }
            }
            return matches;
        }

        boolean matches(String s) {
            return messagePattern == null ? s.contains(message) : messagePattern.matcher(s).matches();
        }
    }
}
