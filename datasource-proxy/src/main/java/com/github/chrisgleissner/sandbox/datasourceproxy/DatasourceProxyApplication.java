package com.github.chrisgleissner.sandbox.datasourceproxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication @Slf4j
public class DatasourceProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(DatasourceProxyApplication.class, args);
	}

	@Bean
	public CommandLineRunner demo(CustomerRepository repository) {
		return (args) -> {
			log.info("\n\nsave ----------------------");
			repository.save(new Customer("Jack", "Bauer"));
			repository.save(new Customer("Chloe", "O'Brian"));

			log.info("\n\nfindAll ----------------------");
			repository.findAll();

			log.info("\n\nfindById ----------------------");
			repository.findById(1L);

			log.info("\n\nfindByLastName ----------------------");
			repository.findByLastName("Bauer");
		};
	}

}

