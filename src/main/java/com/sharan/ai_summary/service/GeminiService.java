package com.sharan.ai_summary.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.sharan.ai_summary.config.GeminiProperties;

@Service
public class GeminiService {
    
    private final WebClient geminiWebClient;
    private final GeminiProperties props;

    public GeminiService(WebClient geminiWebClient, GeminiProperties props){
        this.geminiWebClient = geminiWebClient;
        this.props = props;
    }

    public String summarize(String text){
        String prompt = "Summarize the following text in 3-5 concise sentences:\n\n" + text;

        Map<String,Object> requestBody = Map.of(
            "contents",List.of(
                Map.of("parts",List.of(
                    Map.of("text",prompt)
                ))
            )
        );

        Map<String,Object> response = geminiWebClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/v1beta/models/{model}:generateContent")
                .queryParam("key",props.getApiKey())
                .build(props.getModel())
            )
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(status -> status.isError(), errorResponse ->
                errorResponse.bodyToMono(String.class)
                    .flatMap(body -> reactor.core.publisher.Mono.error(
                        new RuntimeException("Gemini API error: " + body)
                    ))
            )
            .bodyToMono(Map.class)
            .block();
        
        var candidates = (List<Map<String,Object>>) response.get("candidates");
        var content = (Map<String,Object>) candidates.get(0).get("content");
        var parts = (List<Map<String,Object>>)content.get("parts");
        return (String) parts.get(0).get("text");
    }
}
