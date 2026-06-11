package com.sharan.ai_summary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiSummaryApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiSummaryApplication.class, args);
	}
}
