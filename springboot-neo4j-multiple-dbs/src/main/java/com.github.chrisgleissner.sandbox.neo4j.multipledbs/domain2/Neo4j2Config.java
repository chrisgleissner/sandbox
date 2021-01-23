package com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain2;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;

import static com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain2.Neo4j2Config.SESSION_FACTORY;
import static com.github.chrisgleissner.sandbox.neo4j.multipledbs.domain2.Neo4j2Config.TRANSACTION_MANAGER;

@Configuration
@EnableNeo4jRepositories(sessionFactoryRef = SESSION_FACTORY, transactionManagerRef = TRANSACTION_MANAGER,
        basePackageClasses = Person2Repo.class, sessionBeanName = "sessionBean2")
public class Neo4j2Config {
    public static final String SESSION_FACTORY = "sessionFactory2";
    public static final String TRANSACTION_MANAGER = "transactionManager2";

    @Bean
    @ConfigurationProperties("spring.data.neo4j.domain2")
    Neo4jProperties neo4jProperties2() {
        return new Neo4jProperties();
    }

    @Bean
    org.neo4j.ogm.config.Configuration ogmConfiguration2() {
        return neo4jProperties2().createConfiguration();
    }

    @Bean(name = SESSION_FACTORY)
    SessionFactory sessionFactory() {
        return new SessionFactory(ogmConfiguration2(), Person2Repo.class.getPackageName());
    }

    @Bean(name = TRANSACTION_MANAGER)
    Neo4jTransactionManager transactionManager() {
        return new Neo4jTransactionManager(sessionFactory());
    }
}

