package br.com.sobrabem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SobrabemApplication {

	public static void main(String[] args) {
		SpringApplication.run(SobrabemApplication.class, args);
	}
}