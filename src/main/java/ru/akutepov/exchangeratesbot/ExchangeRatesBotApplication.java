package ru.akutepov.exchangeratesbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class ExchangeRatesBotApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Almaty"));
		System.err.println("=== Application started ===: version 1.0.2б timezone set to " + TimeZone.getDefault().getID());
		SpringApplication.run(ExchangeRatesBotApplication.class, args);
	}
}
