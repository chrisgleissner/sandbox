package com.github.chrisgleissner.sandbox.log4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
            for (ConfigItem configItem : config.getConfigItems()) {
                if (matches(event, configItem)) {
                    deniedCountByLevel.putIfAbsent(event.getLevel(), new AtomicLong());
                    deniedCountByLevel.get(event.getLevel()).incrementAndGet();
                    return Filter.DENY;
                }
            }
        }
        return Filter.NEUTRAL;
    }

    private boolean matches(LoggingEvent event, ConfigItem configItem) {
        boolean matches = false;
        if (configItem.getLevel().equals(event.getLevel())) {
            matches = configItem.getPattern().matcher(event.getRenderedMessage()).matches();
            if (!matches && checkStackTrace) {
                String[] throwableStrRep = event.getThrowableStrRep();
                if (throwableStrRep != null) {
                    int i = 0;
                    while (!matches && i < throwableStrRep.length) {
                        matches = configItem.getPattern().matcher(throwableStrRep[i++]).matches();
                    }
                }
            }
        }
        return matches;
    }

    private static class Config {
        private static final long CONFIG_REFRESH_INTERVAL_IN_SECONDS = 5 * 60;

        private final List<String> configPaths;
        @Getter private List<ConfigItem> configItems;

        Config(String configFileNames) {
            this.configPaths = Arrays.asList(configFileNames.split(","));
            this.configItems = loadConfigItems();
            enablePeriodicConfigItemRefresh();
        }

        private void enablePeriodicConfigItemRefresh() {
            Executors.newScheduledThreadPool(1, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            }).scheduleAtFixedRate(new Runnable() {
                @Override public void run() {
                    Config.this.configItems = loadConfigItems();
                }
            }, CONFIG_REFRESH_INTERVAL_IN_SECONDS, CONFIG_REFRESH_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
        }

        List<ConfigItem> loadConfigItems() {
            final List<ConfigItem> allConfigItems = new ArrayList<>();
            for (final String filePath : configPaths) {
                try {
                    final List<ConfigItem> configItems = new ArrayList<>();
                    final Path path = Paths.get(filePath);
                    if (path.toFile().exists()) {
                        final String fileContent = new String(Files.readAllBytes(path), Charset.forName("UTF-8"));
                        for (final String line : fileContent.split("\n")) {
                            final String trimmedLine = line.trim();
                            if (trimmedLine.length() > 0) {
                                configItems.add(ConfigItem.of(trimmedLine));
                            }
                        }
                    }
                    LogLog.debug("Read config for " + RegexFilter.class.getName() + " from " + path + ": " + configItems);
                    allConfigItems.addAll(configItems);
                } catch (IOException e) {
                    LogLog.warn("Failed to read config for " + RegexFilter.class.getName() + " from " + filePath, e);
                }
            }
            return allConfigItems;
        }
    }

    @Value
    private static class ConfigItem {
        Level level;
        Pattern pattern;

        static ConfigItem of(String s) {
            int idx = s.indexOf(' ');
            String level = s.substring(0, idx).trim();
            String regex = s.substring(idx).trim();
            return new ConfigItem(Level.toLevel(level), Pattern.compile(regex));
        }
    }
}