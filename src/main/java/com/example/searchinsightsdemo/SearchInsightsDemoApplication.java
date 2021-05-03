package com.example.searchinsightsdemo;

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
}
