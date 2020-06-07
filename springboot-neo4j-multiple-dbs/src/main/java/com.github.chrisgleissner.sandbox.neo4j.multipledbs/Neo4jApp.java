package com.github.chrisgleissner.sandbox.neo4j.multipledbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Neo4jApp {
    public static void main(String[] args) {
        SpringApplication.run(Neo4jApp.class, args);
    }
}
