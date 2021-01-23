package com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain2;

import lombok.Data;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity(label = "Person")
@Data
public class Person2 {
    @Id
    @GeneratedValue
    private Long id;
    private String name;

    public Person2(String name) {
        this.name = name;
    }
}
