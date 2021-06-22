package com.marklogic.r2m;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class R2MApplication {

	private static Logger LOG = LoggerFactory.getLogger(R2MApplication.class);

	public static void main(String[] args) {
		LOG.info("Init R2M Client...");
		SpringApplication.run(R2MApplication.class, args);
		LOG.info("Shutting down R2M Client...");
	}
}
