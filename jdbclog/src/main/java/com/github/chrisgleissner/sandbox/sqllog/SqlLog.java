package com.github.chrisgleissner.sandbox.sqllog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.NoOpQueryExecutionListener;
import net.ttddyy.dsproxy.listener.logging.DefaultJsonQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.QueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.proxy.DefaultConnectionIdManager;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Maintains a log of JDBC statements.
 */
@Getter
@ToString
@Slf4j
public class SqlLog implements BeanPostProcessor {
    private final ConnectionLogs connectionLogs = new ConnectionLogs();
    private final static QueryLogEntryCreator logCreator = new DefaultJsonQueryLogEntryCreator() {
        protected void writeTimeEntry(StringBuilder sb, ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        }
    };
    private final static InheritableThreadLocal<String> id = new InheritableThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return "default";
        }
    };

    public void setThreadId(String id) {
        SqlLog.id.set(id);
    }

    public Collection<String> getAndClearLogsForThreadId(String id) {
        return connectionLogs.logsById.remove(id).getAll();
    }

    public Collection<String> getLogsForThreadId(String id) {
        return Optional.ofNullable(connectionLogs.logsById.get(id)).map(ConnectionLog::getAll).orElse(emptyList());
    }

    public Collection<String> getLogsContainingRegex(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return getLogs(v -> v.getAll().stream().anyMatch(s -> pattern.matcher(s).find()));
    }

    public Collection<String> getLogsContaining(String expectedString) {
        return getLogs(v -> v.getAll().stream().anyMatch(s -> s.contains(expectedString)));
    }

    private Collection<String> getLogs(Predicate<ConnectionLog> predicate) {
        return connectionLogs.logsById.values().stream()
                .filter(predicate)
                .flatMap(l -> l.getAll().stream()).collect(toList());
    }

    public Collection<String> getLogs() {
        return connectionLogs.logsById.values().stream().flatMap(l -> l.getAll().stream()).collect(toList());
    }

    public void clear() {
        connectionLogs.clear();
    }

    public static class ConnectionLogs extends NoOpQueryExecutionListener implements Iterable<SqlLog.ConnectionLog> {
        private ConcurrentHashMap<String, ConnectionLog> logsById = new ConcurrentHashMap<>();

        @Override
        public void afterQuery(ExecutionInfo executionInfo, List<QueryInfo> list) {
            String logMsg = logCreator.getLogEntry(executionInfo, list, false, false);
            String currentId = id.get();
            logsById.computeIfAbsent(currentId, ConnectionLog::new).add(logMsg);
            log.info("{}: {}", currentId, logMsg);
        }

        public String toString() {
            return String.format("ConnectionLogs(%s)", logsById.entrySet().stream().map(e ->
                    String.format("%s=%s", e.getKey(), e.getValue())).collect(joining("\n")));
        }

        @Override
        public Iterator<ConnectionLog> iterator() {
            return logsById.values().iterator();
        }

        public void clear() {
            logsById.clear();
        }
    }

    @RequiredArgsConstructor
    public static class ConnectionLog {

        @Getter
        private final String id;
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private final List<String> logs = new LinkedList<>();

        public void add(String msg) {
            lock.writeLock().lock();
            try {
                logs.add(msg);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public Collection<String> getAll() {
            lock.readLock().lock();
            try {
                return new ArrayList<>(logs);
            } finally {
                lock.readLock().unlock();
            }
        }

        public String toString() {
            Collection<String> msgs = getAll();
            return String.format("ConnectionLog(%s, count=%s):%s", id, msgs.size(),
                    msgs.isEmpty() ? " empty" : "\n" + msgs.stream().collect(joining("\n")));
        }
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        return bean instanceof DataSource ? ProxyDataSourceBuilder.create((DataSource) bean)
                .connectionIdManager(new DefaultConnectionIdManager())
                .traceMethods()
                .logQueryBySlf4j(SLF4JLogLevel.INFO)
                .listener(connectionLogs).build() : bean;
    }
}
