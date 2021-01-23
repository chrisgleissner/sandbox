package com.github.chrisgleissner.sandbox.log4j;

import lombok.Value;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
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

public class Log4jRegexFilter extends Filter {
    private String configPathsString;
    private Config config;
    private static final ConcurrentHashMap<Level, AtomicLong> deniedCountByLevel = new ConcurrentHashMap<>();


    public static Map<Level, Long> getDeniedCountByLevel() {
        Map<Level, Long> deniedCountByLevel = new HashMap<>();
        for (Map.Entry<Level, AtomicLong> entry : Log4jRegexFilter.deniedCountByLevel.entrySet()) {
            deniedCountByLevel.put(entry.getKey(), entry.getValue().get());
        }
        return deniedCountByLevel;
    }

    public Log4jRegexFilter() {
    }

    public String getConfigPaths() {
        return this.configPathsString;
    }

    public void setConfigPaths(String s) {
        this.configPathsString = s;
        this.config = new Config(s);
    }

    public int decide(LoggingEvent event) {
        String msg = event.getRenderedMessage();
        if (msg != null) {
            for (ConfigItem configItem : config.getConfigItems()) {
                if (configItem.getLevel() == event.getLevel() && configItem.getPattern().matcher(event.getRenderedMessage()).matches()) {
                    deniedCountByLevel.putIfAbsent(event.getLevel(), new AtomicLong());
                    deniedCountByLevel.get(event.getLevel()).incrementAndGet();
                    return Filter.DENY;
                }
            }
        }
        return Filter.NEUTRAL;
    }

    static class Config {
        private final List<String> configPaths;
        private List<ConfigItem> configItems;

        Config(String configFileNames) {
            this.configPaths = Arrays.asList(configFileNames.split(","));
            this.configItems = loadConfigItems();
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
            }, 5, 5, TimeUnit.SECONDS);
        }

        private List<ConfigItem> loadConfigItems() {
            List<ConfigItem> allConfigItems = new ArrayList<>();
            for (String filePath : configPaths) {
                try {
                    List<ConfigItem> configItems = new ArrayList<>();
                    Path path = Paths.get(filePath);
                    if (path.toFile().exists()) {
                        String fileContent = new String(Files.readAllBytes(path));
                        for (String line : fileContent.split("\n")) {
                            configItems.add(ConfigItem.of(line));
                        }
                    }
                    LogLog.debug("Read config for " + this.getClass().getName() + " from " + path + ": " + configItems);
                    allConfigItems.addAll(configItems);
                } catch (IOException e) {
                    LogLog.warn("Failed to read from " + filePath, e);
                }
            }
            return allConfigItems;
        }

        public List<ConfigItem> getConfigItems() {
            return configItems;
        }
    }

    @Value
    private static class ConfigItem {
        Level level;
        Pattern pattern;

        public static ConfigItem of(String s) {
            String[] segs = s.split(",");
            return new ConfigItem(Level.toLevel(segs[0].trim()), Pattern.compile(segs[1].trim()));
        }
    }
}
