package com.github.chrisgleissner.sandbox.log4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
 * Filters messages with a certain level and a log message or stack trace matching a specified regex.
 */
@NoArgsConstructor
public class RegexFilter extends Filter {
    private static final ConcurrentHashMap<Level, AtomicLong> deniedCountByLevel = new ConcurrentHashMap<>();

    /**
     * Comma-separated config paths. Each config file line needs to satisfy this pattern: levelThreshold ' ' regex
     */
    @Getter private String configPathsString;
    @Getter @Setter private boolean checkStackTrace = true;
    private Config config;

    public static Map<Level, Long> getDeniedCountByLevel() {
        Map<Level, Long> deniedCountByLevel = new HashMap<>();
        for (Map.Entry<Level, AtomicLong> entry : RegexFilter.deniedCountByLevel.entrySet()) {
            deniedCountByLevel.put(entry.getKey(), entry.getValue().get());
        }
        return deniedCountByLevel;
    }

    public void setConfigPaths(String s) {
        this.configPathsString = s;
        this.config = new Config(s);
    }

    public int decide(LoggingEvent event) {
        String msg = event.getRenderedMessage();
        if (msg != null && config != null) {
            for (Config.FilterItem filterItem : config.getFilterItems()) {
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
            final List<FilterItem> allFilterItems = new ArrayList<>();
            for (final String configPathName : configPaths) {
                try {
                    allFilterItems.addAll(loadFilterItems(Paths.get(configPathName)));
                } catch (IOException e) {
                    LogLog.warn("Failed to read config for " + RegexFilter.class.getName() + " from " + configPathName, e);
                }
            }
            return allFilterItems;
        }

        List<FilterItem> loadFilterItems(Path path) throws IOException {
            val filterItems = new ArrayList<FilterItem>();
            if (path.toFile().exists()) {
                try (FileInputStream fis = new FileInputStream(path.toFile())) {
                    Map<String, Object> yaml = new Yaml().load(fis);
                    if (yaml != null) {
                        List<Map<String, Object>> yamlFilters = (List<Map<String, Object>>) yaml.get("filters");
                        if (yamlFilters != null) {
                            for (Map<String, Object> yamlFilterItem : yamlFilters) {
                                filterItems.add(FilterItem.of(yamlFilterItem));
                            }
                        }
                    }
                }
            }
            LogLog.debug("Read config for " + RegexFilter.class.getName() + " from " + path + ": " + filterItems);
            return filterItems;
        }

        static abstract class FilterItem {

            static FilterItem of(Map<String, Object> yamlFilterItem) {
                String level = (String) yamlFilterItem.get("level");
                String message = (String) yamlFilterItem.get("message");
                boolean regex = getOrDefault(yamlFilterItem, "regex", false);
                boolean checkStackTrace = getOrDefault(yamlFilterItem, "checkStackTrace", false);
                return regex
                        ? new RegexFilterItem(Level.toLevel(level), Pattern.compile(message), checkStackTrace)
                        : new StringFilterItem(Level.toLevel(level), message, checkStackTrace);
            }

            static boolean getOrDefault(Map<String, Object> map, String key, boolean defaultValue) {
                Boolean value = (Boolean) map.get(key);
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
                        String[] throwableStrRep = event.getThrowableStrRep();
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
