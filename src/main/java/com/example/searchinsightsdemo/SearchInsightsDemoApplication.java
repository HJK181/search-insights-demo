package com.example.searchinsightsdemo;

import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.searchinsightsdemo.service.FileService;

@SpringBootApplication
public class SearchInsightsDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SearchInsightsDemoApplication.class, args);
	}

	@Bean
	CommandLineRunner init(FileService fileService) {
		return (args) -> {
			fileService.init();
		};
	}

	@Bean
	Settings athenaSettings() {
		return new Settings().withStatementType(StatementType.STATIC_STATEMENT).withRenderQuotedNames(RenderQuotedNames.NEVER);
	}
}
