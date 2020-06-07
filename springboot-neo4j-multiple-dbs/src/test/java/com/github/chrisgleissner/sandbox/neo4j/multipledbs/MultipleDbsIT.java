package com.github.chrisgleissner.sandbox.neo4j.multipledbs;

import com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain.Person;
import com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain.PersonRepo;
import com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain2.Person2;
import com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain2.Person2Repo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
}