package com.github.chrisgleissner.sandbox.sqllog;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "com.github.chrisgleissner.sandbox.sqllog")
public class SqlLogAutoConfiguration extends ActiveMQAutoConfiguration {

    @Bean
    SqlLog jdbcLog() {
        return new SqlLog();
    }
}
