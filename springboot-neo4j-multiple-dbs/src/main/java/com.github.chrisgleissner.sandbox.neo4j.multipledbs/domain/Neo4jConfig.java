package com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;

@Configuration
@EnableNeo4jRepositories(basePackageClasses = PersonRepo.class)
public class Neo4jConfig {

	@Primary @Bean @ConfigurationProperties("spring.data.neo4j")
    Neo4jProperties neo4jProperties() {
		return new Neo4jProperties();
	}

	@Primary @Bean
    org.neo4j.ogm.config.Configuration ogmConfiguration() {
		return neo4jProperties().createConfiguration();
	}

	@Primary @Bean
    SessionFactory sessionFactory() {
		return new SessionFactory(ogmConfiguration(), PersonRepo.class.getPackageName());
	}

	@Bean
    Neo4jTransactionManager transactionManager() {
		return new Neo4jTransactionManager(sessionFactory());
	}
}

