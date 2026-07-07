package com.uit.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.uit.api", "com.uit.agentcore"})
public class UitcApplication {

	public static void main(String[] args) {
		SpringApplication.run(UitcApplication.class, args);
	}

}
