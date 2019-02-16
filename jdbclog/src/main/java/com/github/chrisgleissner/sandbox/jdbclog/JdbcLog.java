package com.github.chrisgleissner.sandbox.jdbclog;

import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.NoOpQueryExecutionListener;
import net.ttddyy.dsproxy.listener.logging.DefaultJsonQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.QueryLogEntryCreator;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * Maintains a log book of JDBC statements.
 */
@Component @Getter @ToString @Slf4j
public class JdbcLog implements BeanPostProcessor {
    private final ConnectionLogs connectionLogs = new ConnectionLogs();
    private final static QueryLogEntryCreator logCreator = new DefaultJsonQueryLogEntryCreator() {
        protected void writeTimeEntry(StringBuilder sb, ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        }
    };

    public Collection<ConnectionLog> getLogsContainingRegex(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return connectionLogs.logsByConnectionId.values().stream().filter(v -> v.getLogs().stream()
                .anyMatch(s -> pattern.matcher(s).find())).collect(Collectors.toList());
    }

    public Collection<ConnectionLog> getLogsContaining(String expectedString) {
        return connectionLogs.logsByConnectionId.values().stream().filter(v -> v.getLogs().stream()
                .anyMatch(s -> s.contains(expectedString))).collect(Collectors.toList());
    }

    public static class ConnectionLogs extends NoOpQueryExecutionListener implements Iterable<JdbcLog.ConnectionLog> {
        private Map<String, ConnectionLog> logsByConnectionId = new ConcurrentHashMap<>();

        @Override
        public void afterQuery(ExecutionInfo executionInfo, List<QueryInfo> list) {
            String logMsg = logCreator.getLogEntry(executionInfo, list, false, false);
            logsByConnectionId.computeIfAbsent(executionInfo.getConnectionId(), ConnectionLog::new).add(logMsg);
            log.info("{}", logMsg);
        }

        public String toString() {
            return String.format("ConnectionLogs(%s)", logsByConnectionId.entrySet().stream().map(e ->
                    String.format("%s=%s", e.getKey(), e.getValue())).collect(joining("\n")));
        }

        @Override
        public Iterator<ConnectionLog> iterator() {
            return logsByConnectionId.values().iterator();
        }
    }

    @Value
    public static class ConnectionLog {
        private final String connectionId;
        private final Collection<String> logs = new ConcurrentLinkedQueue<>();

        public ConnectionLog(String connectionId) {
            this.connectionId = connectionId;
        }

        public void add(String msg) {
            logs.add(msg);
        }

        public void clear() {
            logs.clear();
        }

        public Collection<String> getAll() {
            return new ArrayList<>(logs);
        }

        public String toString() {
            Collection<String> msgs = getAll();
            return String.format("ConnectionLog(connectionId=%s, count=%s):%s", connectionId, msgs.size(),
                    msgs.isEmpty() ? " empty" : "\n" + msgs.stream().collect(joining("\n")));
        }
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        return bean instanceof DataSource ? ProxyDataSourceBuilder.create((DataSource) bean).listener(connectionLogs).build() : bean;
    }
}
