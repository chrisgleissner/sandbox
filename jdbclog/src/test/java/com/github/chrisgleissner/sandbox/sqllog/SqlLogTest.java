package com.github.chrisgleissner.sandbox.sqllog;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
@TestPropertySource(properties = "com.github.chrisgleissner.sandbox.sqllog=true")
@SpringBootTest
public class SqlLogTest {

    @Autowired
    private CustomerRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SqlLog sqlLog;

    @Before
    public void setUp() {
        sqlLog.clear();
    }

    @Test
    public void getLogsContaining() {
        repository.save(new Customer("Jack", "Bauer"));
        Collection<String> matchingLogs = sqlLog.getLogsContaining("insert into customer");
        assertThat(matchingLogs).containsExactly(
                "{\"success\":true, \"type\":\"Prepared\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"call next value for hibernate_sequence\"], \"params\":[[]]}",
                "{\"success\":true, \"type\":\"Prepared\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"insert into customer (first_name, last_name, id) values (?, ?, ?)\"], \"params\":[[\"Jack\",\"Bauer\",\"1\"]]}");
    }

    @Test
    public void getLogsContainingRegex() {
        repository.findAll();
        Collection<String> matchingLogs = sqlLog.getLogsContainingRegex("select.*?from customer");
        assertThat(matchingLogs).containsExactly(
                "{\"success\":true, \"type\":\"Prepared\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"select customer0_.id as id1_0_, customer0_.first_name as first_na2_0_, customer0_.last_name as last_nam3_0_ from customer customer0_\"], \"params\":[[]]}");
    }

    @Test
    public void getLogsForThreadId() {
        String id = UUID.randomUUID().toString();
        sqlLog.setThreadId(id);
        repository.findAll();
        String msg = "{\"success\":true, \"type\":\"Prepared\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"select customer0_.id as id1_0_, customer0_.first_name as first_na2_0_, customer0_.last_name as last_nam3_0_ from customer customer0_\"], \"params\":[[]]}";
        assertThat(sqlLog.getLogsForThreadId(id)).containsExactly(msg);

        repository.findByLastName("Bauer");
        String[] msgs = {"{\"success\":true, \"type\":\"Prepared\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"select customer0_.id as id1_0_, customer0_.first_name as first_na2_0_, customer0_.last_name as last_nam3_0_ from customer customer0_\"], \"params\":[[]]}",
                "{\"success\":true, \"type\":\"Prepared\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"select customer0_.id as id1_0_, customer0_.first_name as first_na2_0_, customer0_.last_name as last_nam3_0_ from customer customer0_ where customer0_.last_name=?\"], \"params\":[[\"Bauer\"]]}"};
        assertThat(sqlLog.getLogsForThreadId(id)).containsExactly(msgs);
        assertThat(sqlLog.getAndClearLogsForThreadId(id)).containsExactly(msgs);
        assertThat(sqlLog.getLogsForThreadId(id)).isEmpty();

        repository.findAll();
        assertThat(sqlLog.getLogsForThreadId(id)).containsExactly(msg);
    }

    @Test
    public void getLogsContainingForJdbc() {
        try {
            jdbcTemplate.execute("create table foo (id int)");
            jdbcTemplate.execute("insert into foo (id) values (1)");
            assertThat(jdbcTemplate.queryForObject("select count (*) from foo where id = 1", Integer.class)).isEqualTo(1);

            String[] msgs = {"{\"success\":true, \"type\":\"Statement\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"create table foo (id int)\"], \"params\":[]}",
                    "{\"success\":true, \"type\":\"Statement\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"insert into foo (id) values (1)\"], \"params\":[]}",
                    "{\"success\":true, \"type\":\"Statement\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"select count (*) from foo where id = 1\"], \"params\":[]}"};
            assertThat(sqlLog.getLogsContaining("table foo")).containsExactlyInAnyOrder(msgs);
            assertThat(sqlLog.getLogsContaining("foo")).containsExactlyInAnyOrder(msgs);
        } finally {
            jdbcTemplate.execute("drop table foo");
        }
    }


    @Transactional
    @Test
    public void getLogsContainingForTransactionalJdbc() {
        try {
            jdbcTemplate.execute("create table foo (id int)");
            jdbcTemplate.execute("insert into foo (id) values (1)");
            assertThat(jdbcTemplate.queryForObject("select count (*) from foo where id = 1", Integer.class)).isEqualTo(1);

            Collection<String> matchingLogs = sqlLog.getLogsContaining("table foo");
            String[] msgs = {"{\"success\":true, \"type\":\"Statement\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"create table foo (id int)\"], \"params\":[]}",
                    "{\"success\":true, \"type\":\"Statement\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"insert into foo (id) values (1)\"], \"params\":[]}",
                    "{\"success\":true, \"type\":\"Statement\", \"batch\":false, \"querySize\":1, \"batchSize\":0, \"query\":[\"select count (*) from foo where id = 1\"], \"params\":[]}"};
            assertThat(matchingLogs).containsExactly(msgs);
            matchingLogs = sqlLog.getLogsContaining("foo");
            assertThat(matchingLogs).containsExactly(msgs);
        } finally {
            jdbcTemplate.execute("drop table foo");
        }
    }

    @Test
    public void getLogsForThreadIdForConcurrentJdbc() throws InterruptedException {
        try {
            List<String> ids = new ArrayList<>();
            ids.add(UUID.randomUUID().toString());
            ids.add(UUID.randomUUID().toString());

            assertThat(sqlLog.getLogs()).hasSize(0);
            jdbcTemplate.execute("create table foo (id varchar)");
            assertThat(sqlLog.getLogs()).hasSize(1);

            CountDownLatch endLatch = new CountDownLatch(ids.size());
            ids.forEach(id ->
                    new Thread(() -> {
                        sqlLog.setThreadId(id);
                        jdbcTemplate.execute(format("insert into foo (id) values ('%s')", id));
                        endLatch.countDown();
                    }, id).start());
            endLatch.await(2, SECONDS);

            ids.forEach(id -> assertThat(sqlLog.getLogsForThreadId(id)).containsExactly(format(
                    "{\"success\":true, \"type\":\"Statement\", \"batch\":false, \"querySize\":1, " +
                            "\"batchSize\":0, \"query\":[\"insert into foo (id) values ('%s')\"], \"params\":[]}", id)));
            assertThat(sqlLog.getLogs()).hasSize(ids.size() + 1);
        } finally {
            jdbcTemplate.execute("drop table foo");
        }
    }
}

