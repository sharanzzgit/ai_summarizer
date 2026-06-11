package com.sharan.ai_summary.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient geminiWebClient(GeminiProperties props){
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("x-google-api-key", props.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
