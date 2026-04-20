package com.dev.habr_parser;

import com.fasterxml.jackson.databind.ObjectMapper; // импорт
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; // импорт

@SpringBootApplication
public class HabrParserApplication {

	public static void main(String[] args) {
		SpringApplication.run(HabrParserApplication.class, args);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}