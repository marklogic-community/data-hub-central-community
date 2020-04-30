package com.marklogic.grove.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"com.marklogic.grove.boot", "com.marklogic.hub", "com.marklogic.envision"})
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public CommonsRequestLoggingFilter requestLoggingFilter() {
		CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
		filter.setIncludeClientInfo(true);
		filter.setIncludeQueryString(true);
		return filter;
	}
}
