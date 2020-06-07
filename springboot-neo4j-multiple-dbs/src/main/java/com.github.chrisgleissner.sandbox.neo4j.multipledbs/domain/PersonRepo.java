package com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.stream.Stream;

public interface PersonRepo extends Neo4jRepository<Person, Long> {
    Stream<Person> readAllByNameNotNull();
}
