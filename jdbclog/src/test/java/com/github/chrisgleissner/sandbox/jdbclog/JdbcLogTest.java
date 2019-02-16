package com.github.chrisgleissner.sandbox.jdbclog;

import com.github.chrisgleissner.sandbox.jdbclog.repo.Customer;
import com.github.chrisgleissner.sandbox.jdbclog.repo.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class JdbcLogTest {

    @Autowired
    private CustomerRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcLog jdbcLog;

    @After
    public void tearDown() {
        log.info("Log: {}", jdbcLog);
    }

    @Test
    public void jpaGetLogsContaining() {
        repository.save(new Customer("Jack", "Bauer"));
        Collection<JdbcLog.ConnectionLog> matchingLogs = jdbcLog.getLogsContaining("insert into customer");
        assertThat(matchingLogs).flatExtracting(l -> l.getLogs()).containsExactly(
                "{\"success\":true, \"type\":\"Prepared\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"call next value for hibernate_sequence\"], \"params\":[[]]}",
                "{\"success\":true, \"type\":\"Prepared\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"insert into customer (first_name, last_name, id) values (?, ?, ?)\"], \"params\":[[\"Jack\",\"Bauer\",\"1\"]]}");
    }

    @Test
    public void jpaGetLogsContainingRegex() {
        repository.findAll();
        Collection<JdbcLog.ConnectionLog> matchingLogs = jdbcLog.getLogsContainingRegex("select.*?from customer");
        assertThat(matchingLogs).flatExtracting(l -> l.getLogs()).containsExactly(
                "{\"success\":true, \"type\":\"Prepared\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"select customer0_.id as id1_0_, customer0_.first_name as first_na2_0_, customer0_.last_name as last_nam3_0_ from customer customer0_\"], \"params\":[[]]}");
    }

    @Test
    public void jdbcWorks() {
        jdbcTemplate.execute("create table foo (id int)");
        jdbcTemplate.execute("insert into foo (id) values (1)");
        assertThat(jdbcTemplate.queryForObject("select count (*) from foo where id = 1", Integer.class)).isEqualTo(1);

        Collection<JdbcLog.ConnectionLog> matchingLogs = jdbcLog.getLogsContaining("table foo");
        assertThat(matchingLogs).flatExtracting(l -> l.getLogs()).containsExactly("{\"success\":true, \"type\":\"Statement\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"create table foo (id int)\"], \"params\":[]}");

        matchingLogs = jdbcLog.getLogsContaining("foo");
        assertThat(matchingLogs).flatExtracting(l -> l.getLogs()).containsExactly(
                "{\"success\":true, \"type\":\"Statement\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"create table foo (id int)\"], \"params\":[]}",
                "{\"success\":true, \"type\":\"Statement\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"insert into foo (id) values (1)\"], \"params\":[]}",
                "{\"success\":true, \"type\":\"Statement\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"select count (*) from foo where id = 1\"], \"params\":[]}");
    }
}

