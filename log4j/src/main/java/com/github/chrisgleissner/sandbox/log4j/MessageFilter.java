package com.github.chrisgleissner.sandbox.log4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.val;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final ConcurrentHashMap<Level, AtomicLong> deniedCountByLevel = new ConcurrentHashMap<>();

    @Getter private String configPathsString;
    private Config config;

    public static Map<Level, Long> getDeniedCountByLevel() {
        val deniedCountByLevel = new HashMap<Level, Long>();
        for (val entry : MessageFilter.deniedCountByLevel.entrySet()) {
            deniedCountByLevel.put(entry.getKey(), entry.getValue().get());
        }
        return deniedCountByLevel;
    }

    public void setConfigPaths(String s) {
        this.configPathsString = s;
        this.config = new Config(s);
    }

    public int decide(LoggingEvent event) {
        val msg = event.getRenderedMessage();
        if (msg != null && config != null) {
            for (val filterItem : config.getFilterItems()) {
                if (filterItem.matches(event)) {
                    deniedCountByLevel.putIfAbsent(event.getLevel(), new AtomicLong());
                    deniedCountByLevel.get(event.getLevel()).incrementAndGet();
                    return Filter.DENY;
                }
            }
        }
        return Filter.NEUTRAL;
    }

    private static class Config {
        private static final long CONFIG_REFRESH_INTERVAL_IN_SECONDS = 5 * 60;

        private final List<String> configPaths;
        @Getter private List<FilterItem> filterItems;

        Config(String configFileNames) {
            this.configPaths = Arrays.asList(configFileNames.split(","));
            this.filterItems = loadFilterItems();
            enablePeriodicConfigRefresh();
        }

        void enablePeriodicConfigRefresh() {
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
            }, CONFIG_REFRESH_INTERVAL_IN_SECONDS, CONFIG_REFRESH_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
        }

        List<FilterItem> loadFilterItems() {
            val allFilterItems = new ArrayList<FilterItem>();
            for (val configPathName : configPaths) {
                try {
                    allFilterItems.addAll(loadFilterItems(Paths.get(configPathName)));
                } catch (IOException e) {
                    LogLog.warn("Failed to read config for " + MessageFilter.class.getName() + " from " + configPathName, e);
                }
            }
            return allFilterItems;
        }

        List<FilterItem> loadFilterItems(Path path) throws IOException {
            val filterItems = new ArrayList<FilterItem>();
            if (path.toFile().exists()) {
                try (val fis = new FileInputStream(path.toFile())) {
                    val yaml = (List<Map<String, Object>>) new Yaml().load(fis);
                    for (val yamlFilterItem : yaml) {
                        filterItems.add(FilterItem.of(yamlFilterItem));
                    }
                }
            }
            LogLog.debug("Read config for " + MessageFilter.class.getName() + " from " + path + ": " + filterItems);
            return filterItems;
        }

        static abstract class FilterItem {

            static FilterItem of(Map<String, Object> yamlFilterItem) {
                val level = (String) yamlFilterItem.get("level");
                if (level == null)
                    throw new RuntimeException("Missing level");
                val message = (String) yamlFilterItem.get("message");
                if (message == null)
                    throw new RuntimeException("Missing message");
                val regex = getOrDefault(yamlFilterItem, "regex", false);
                val checkStackTrace = getOrDefault(yamlFilterItem, "checkStackTrace", false);
                return regex
                        ? new RegexFilterItem(Level.toLevel(level), Pattern.compile(message), checkStackTrace)
                        : new StringFilterItem(Level.toLevel(level), message, checkStackTrace);
            }

            static boolean getOrDefault(Map<String, Object> map, String key, boolean defaultValue) {
                val value = (Boolean) map.get(key);
                return value == null ? defaultValue : value;
            }

            abstract Level getLevel();

            abstract boolean isCheckStackTrace();

            abstract boolean matches(String s);

            boolean matches(LoggingEvent event) {
                boolean matches = false;
                if (getLevel().equals(event.getLevel())) {
                    matches = matches(event.getRenderedMessage());
                    if (!matches && isCheckStackTrace()) {
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

        }

        @Value
        static class StringFilterItem extends FilterItem {
            Level level;
            String message;
            boolean checkStackTrace;

            @Override boolean matches(String s) {
                return s.indexOf(message) != -1;
            }
        }

        @Value
        static class RegexFilterItem extends FilterItem {
            Level level;
            Pattern message;
            boolean checkStackTrace;

            @Override boolean matches(String s) {
                return message.matcher(s).matches();
            }
        }
    }
}
