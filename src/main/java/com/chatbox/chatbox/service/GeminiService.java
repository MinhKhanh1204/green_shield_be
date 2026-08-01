package com.chatbox.chatbox.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final WebClient webClient;
    private final boolean configured;
    private final String model;

    public GeminiService(
            @Value("${spring.gemini.api.key:}") String apiKey,
            @Value("${spring.gemini.api.model:gemini-flash-latest}") String model) {
        this.configured = apiKey != null && !apiKey.isBlank();
        this.model = model != null && !model.isBlank() ? model.trim() : "gemini-flash-latest";
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + (apiKey != null ? apiKey : ""))
                .build();
    }

    @SuppressWarnings("unchecked")
    public String generateContent(String message) {
        ensureConfigured();
        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", message))
        );

        Map<String, Object> response = webClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("choices")) {
            return "No response from Gemini";
        }

        Object rawChoices = response.get("choices");
        if (!(rawChoices instanceof List<?> choicesList) || choicesList.isEmpty()) {
            return "Empty or invalid response format";
        }

        Object firstChoice = choicesList.get(0);
        if (!(firstChoice instanceof Map<?, ?> firstChoiceMap)) {
            return "Invalid choice format";
        }

        Object msgObj = firstChoiceMap.get("message");
        if (!(msgObj instanceof Map<?, ?> msgMap)) {
            return "Invalid message format";
        }

        Object content = msgMap.get("content");
        return content instanceof String ? (String) content : "No message content";
    }

    // ✅ NEW: Chat bằng hình ảnh
    @SuppressWarnings("unchecked")
    public String generateContentWithImage(String message, String imageBase64) {
        ensureConfigured();
        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", message),
                                Map.of("type", "image_url", "image_url", Map.of(
                                        "url", "data:image/png;base64," + imageBase64
                                ))
                        )
                ))
        );

        Map<String, Object> response = webClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("choices")) {
            return "No response from Gemini";
        }

        Object rawChoices = response.get("choices");
        if (!(rawChoices instanceof List<?> choicesList) || choicesList.isEmpty()) {
            return "Empty or invalid response format";
        }

        Object firstChoice = choicesList.get(0);
        if (!(firstChoice instanceof Map<?, ?> firstChoiceMap)) {
            return "Invalid choice format";
        }

        Object msgObj = firstChoiceMap.get("message");
        if (!(msgObj instanceof Map<?, ?> msgMap)) {
            return "Invalid message format";
        }

        Object content = msgMap.get("content");
        return content instanceof String ? (String) content : "No message content";
    }

    private void ensureConfigured() {
        if (!configured) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }
    }
}
