package com.example.grocery_billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
		System.out.println("========================================");
		System.out.println("  Grocery Billing System Started!");
		System.out.println("  Open: http://localhost:8080");
		System.out.println("========================================");
	}

}




