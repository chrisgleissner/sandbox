package com.github.chrisgleissner.sandbox.neo4j.multipledbs;

import com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain.Person;
import com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain.PersonRepo;
import com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain2.Person2;
import com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain2.Person2Repo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest @Slf4j
public class MultipleDbsIT {
    @Autowired
    private PersonRepo personRepo; // Connected with Neo4J DB 1
    @Autowired
    private Person2Repo person2Repo; // Connected with Neo4J DB 2

    @Test
    void canAccessBothRepos() {
        assertThat(personRepo.count()).isEqualTo(0);
        assertThat(person2Repo.count()).isEqualTo(0);

        Person savedPerson = personRepo.save(new Person("John"));
        assertThat(savedPerson.getId()).isEqualTo(0);
        assertThat(personRepo.count()).isEqualTo(1);
        assertThat(person2Repo.count()).isEqualTo(0);
        assertThat(personRepo.findById(savedPerson.getId()).orElseThrow()).isEqualTo(savedPerson);
        assertThat(person2Repo.findById(savedPerson.getId())).isEmpty();

        Person2 savedPerson2 = person2Repo.save(new Person2("Jane"));
        assertThat(savedPerson2.getId()).isEqualTo(0);
        assertThat(person2Repo.findById(savedPerson2.getId()).orElseThrow()).isEqualTo(savedPerson2);
        assertThat(personRepo.count()).isEqualTo(1);
        assertThat(person2Repo.count()).isEqualTo(1);
    }

    @Test
    void migrateStreamedResults() {
        // Prepare data in DB 1
        int count = 300;
        long startTime = System.nanoTime();
        List<Person> personList = IntStream.range(0, count).mapToObj(Integer::toString).map(Person::new).collect(Collectors.toList());
        personRepo.saveAll(personList);
        log.info("Saved Person list of size {} in {}ms", count, Duration.ofNanos(System.nanoTime() - startTime).toMillis());

        // Stream from DB 1 and insert to DB 2
        startTime = System.nanoTime();
        AtomicInteger migrationCount = new AtomicInteger(0);
        long size = personRepo.count();
        try (Stream<Person> personStream = personRepo.readAllByNameNotNull()) {
            personStream.forEach(p -> {
                person2Repo.save(new Person2(p.getName()));
                if (migrationCount.incrementAndGet() % 100 == 0)
                    log.info("Migrated {} of {} entities", migrationCount.get(), size);
            });
        }
        log.info("Migrated {} entities in {}ms", size, Duration.ofNanos(System.nanoTime() - startTime).toMillis());

    }
}