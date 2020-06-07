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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest @Slf4j
public class MigratorIT {
    @Autowired
    private PersonRepo personRepo; // Connected with Neo4J DB 1
    @Autowired
    private Person2Repo person2Repo; // Connected with Neo4J DB 2

    @Test
    public void migrate() {
        long startTime = System.nanoTime();
        int personListSize = 1000;
        List<Person> personList = IntStream.range(0, personListSize).mapToObj(Integer::toString).map(Person::new).collect(Collectors.toList());
        personRepo.saveAll(personList);
        log.info("Saved Person list of size {} in {}ms", personListSize, Duration.ofNanos(System.nanoTime() - startTime).toMillis());

        int count = Migrator.migrate(personRepo.readAllByNameNotNull(), person2Repo, p -> new Person2(p.getName()), 100);
        assertThat(person2Repo.count()).isEqualTo(personRepo.count());
        assertThat(count).isEqualTo(person2Repo.count());
    }
}
