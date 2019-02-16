package com.github.chrisgleissner.sandbox.jdbclog;

import com.github.chrisgleissner.sandbox.jdbclog.repo.Customer;
import com.github.chrisgleissner.sandbox.jdbclog.repo.CustomerRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JdbcLogTest {

    @Autowired
    CustomerRepository repository;

    @Autowired
    JdbcLog log;

    @Test
    public void getLogsContaining() {
        repository.save(new Customer("Jack", "Bauer"));
        Collection<JdbcLog.ConnectionLog> matchingLogs = log.getLogsContaining("insert");
        assertThat(matchingLogs).hasSize(1);
        assertThat(matchingLogs).flatExtracting(l -> l.getLogs()).containsExactly(
                "{\"success\":true, \"type\":\"Prepared\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"call next value for hibernate_sequence\"], \"params\":[[]]}",
                "{\"success\":true, \"type\":\"Prepared\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"insert into customer (first_name, last_name, id) values (?, ?, ?)\"], \"params\":[[\"Jack\",\"Bauer\",\"1\"]]}");
        matchingLogs.forEach(JdbcLog.ConnectionLog::clear);
    }

    @Test
    public void getLogsContainingRegex() {
        repository.findAll();
        Collection<JdbcLog.ConnectionLog> matchingLogs = log.getLogsContainingRegex("select.*?from customer");
        assertThat(matchingLogs).hasSize(1);
        assertThat(matchingLogs).flatExtracting(l -> l.getLogs()).containsExactly(
                "{\"success\":true, \"type\":\"Prepared\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"select customer0_.id as id1_0_, customer0_.first_name as first_na2_0_, customer0_.last_name as last_nam3_0_ from customer customer0_\"], \"params\":[[]]}");
        matchingLogs.forEach(JdbcLog.ConnectionLog::clear);
    }
}

