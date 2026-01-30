package ru.akutepov.exchangeratesbot;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExchangeRatesBotApplication {

	@PostConstruct
	void init() {
		System.err.println("=== Application started ===: version 1.0.2");
	}
	public static void main(String[] args) {
		SpringApplication.run(ExchangeRatesBotApplication.class, args);
	}
}
