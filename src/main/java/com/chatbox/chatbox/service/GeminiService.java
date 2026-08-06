package com.chatbox.chatbox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GeminiService {

    static final String DAILY_LIMIT_MESSAGE = "Bạn đã đạt giới hạn câu hỏi hôm nay!";
    static final String UNAVAILABLE_MESSAGE =
            "Hệ thống AI tạm thời không khả dụng. Vui lòng thử lại sau!";

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiService.class);
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai";
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final Duration MAX_RETRY_DELAY = Duration.ofSeconds(10);
    private static final Set<Integer> ROTATABLE_STATUS_CODES =
            Set.of(400, 403, 404, 408, 429, 500, 502, 503, 504);

    private final WebClient webClient;
    private final boolean configured;
    private final List<String> textModels;
    private final List<String> imageModels;
    private final int maxOutputTokens;
    private final double temperature;
    private final int retriesPerModel;

    @Autowired
    public GeminiService(
            @Value("${spring.gemini.api.key:}") String apiKey,
            @Value("${spring.gemini.api.model:gemini-3.5-flash-lite}") String primaryModel,
            @Value("${spring.gemini.api.fallback-models:gemini-3.1-flash-lite,gemma-4-31b-it,gemma-4-26b-a4b-it,gemini-3.6-flash,gemini-3.5-flash,gemini-3-flash-preview}")
            String fallbackModels,
            @Value("${spring.gemini.api.image-models:gemini-3.5-flash-lite,gemini-3.1-flash-lite,gemini-3.6-flash,gemini-3.5-flash,gemini-3-flash-preview}")
            String imageModels,
            @Value("${app.ai.chat.max-output-tokens:512}") int maxOutputTokens,
            @Value("${app.ai.chat.temperature:0.3}") double temperature,
            @Value("${app.ai.chat.retries-per-model:1}") int retriesPerModel) {
        this(
                apiKey,
                buildModelList(primaryModel, fallbackModels),
                parseModelList(imageModels),
                DEFAULT_BASE_URL,
                maxOutputTokens,
                temperature,
                retriesPerModel
        );
    }

    GeminiService(
            String apiKey,
            List<String> models,
            String baseUrl,
            int maxOutputTokens,
            double temperature,
            int retriesPerModel) {
        this(apiKey, models, models, baseUrl, maxOutputTokens, temperature, retriesPerModel);
    }

    GeminiService(
            String apiKey,
            List<String> textModels,
            List<String> imageModels,
            String baseUrl,
            int maxOutputTokens,
            double temperature,
            int retriesPerModel) {
        this.configured = apiKey != null && !apiKey.isBlank();
        this.textModels = sanitizeModels(textModels);
        List<String> configuredImageModels = sanitizeModels(imageModels);
        this.imageModels = configuredImageModels.isEmpty() ? this.textModels : configuredImageModels;
        this.maxOutputTokens = Math.max(64, maxOutputTokens);
        this.temperature = Math.max(0, Math.min(temperature, 2));
        this.retriesPerModel = Math.max(0, Math.min(retriesPerModel, 3));
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + (apiKey != null ? apiKey : ""))
                .build();
    }

    public String generateContent(String message) {
        return requestCompletion(message, textModels);
    }

    public String generateContentWithImage(String message, String imageBase64) {
        List<Map<String, Object>> content = List.of(
                Map.of("type", "text", "text", message),
                Map.of("type", "image_url", "image_url", Map.of(
                        "url", "data:image/png;base64," + imageBase64
                ))
        );
        return requestCompletion(content, imageModels);
    }

    private String requestCompletion(Object content, List<String> availableModels) {
        ensureConfigured();
        boolean sawRateLimit = false;
        WebClientResponseException lastRotatableException = null;

        for (int modelIndex = 0; modelIndex < availableModels.size(); modelIndex++) {
            String model = availableModels.get(modelIndex);
            for (int attempt = 0; attempt <= retriesPerModel; attempt++) {
                try {
                    Map<String, Object> response = sendCompletion(model, content);
                    if (modelIndex > 0) {
                        LOGGER.info("Gemini fallback model {} completed the request", model);
                    }
                    return extractMessageContent(response);
                } catch (WebClientResponseException exception) {
                    int statusCode = exception.getStatusCode().value();
                    if (!ROTATABLE_STATUS_CODES.contains(statusCode)) {
                        throw exception;
                    }

                    lastRotatableException = exception;
                    sawRateLimit = sawRateLimit || statusCode == 429;
                    boolean canRetrySameModel = shouldRetrySameModel(statusCode)
                            && attempt < retriesPerModel;
                    if (canRetrySameModel) {
                        Duration delay = getRetryDelay(exception, attempt);
                        LOGGER.warn(
                                "Gemini model {} returned {}; retrying after {} ms",
                                model,
                                statusCode,
                                delay.toMillis()
                        );
                        sleep(delay, exception);
                        continue;
                    }

                    if (modelIndex + 1 < availableModels.size()) {
                        LOGGER.warn(
                                "Gemini model {} returned {}; rotating to {}",
                                model,
                                statusCode,
                                availableModels.get(modelIndex + 1)
                        );
                    }
                    break;
                }
            }
        }

        if (sawRateLimit) {
            return DAILY_LIMIT_MESSAGE;
        }
        if (lastRotatableException != null) {
            LOGGER.error(
                    "All configured Gemini models failed; last status was {}",
                    lastRotatableException.getStatusCode().value()
            );
        }
        return UNAVAILABLE_MESSAGE;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendCompletion(String model, Object content) {
        Map<String, Object> payload = Map.of(
                "model", model,
                "max_tokens", maxOutputTokens,
                "temperature", temperature,
                "messages", List.of(Map.of("role", "user", "content", content))
        );

        return webClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private boolean shouldRetrySameModel(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private Duration getRetryDelay(WebClientResponseException exception, int attempt) {
        String retryAfter = exception.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (retryAfter != null) {
            try {
                long seconds = Long.parseLong(retryAfter.trim());
                return Duration.ofSeconds(Math.max(0, Math.min(seconds, MAX_RETRY_DELAY.toSeconds())));
            } catch (NumberFormatException ignored) {
                // Use exponential delay when Retry-After is an HTTP date or invalid.
            }
        }

        long seconds = Math.min(1L << attempt, MAX_RETRY_DELAY.toSeconds());
        return Duration.ofSeconds(seconds);
    }

    private void sleep(Duration delay, WebClientResponseException originalException) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw originalException;
        }
    }

    private String extractMessageContent(Map<String, Object> response) {
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

        Object message = firstChoiceMap.get("message");
        if (!(message instanceof Map<?, ?> messageMap)) {
            return "Invalid message format";
        }

        Object responseContent = messageMap.get("content");
        return responseContent instanceof String ? (String) responseContent : "No message content";
    }

    private void ensureConfigured() {
        if (!configured) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }
    }

    private static List<String> buildModelList(String primaryModel, String fallbackModels) {
        List<String> configuredModels = new ArrayList<>();
        configuredModels.add(primaryModel);
        if (fallbackModels != null) {
            configuredModels.addAll(List.of(fallbackModels.split(",")));
        }
        return sanitizeModels(configuredModels);
    }

    private static List<String> parseModelList(String configuredModels) {
        if (configuredModels == null || configuredModels.isBlank()) {
            return List.of();
        }
        return sanitizeModels(List.of(configuredModels.split(",")));
    }

    private static List<String> sanitizeModels(List<String> configuredModels) {
        LinkedHashSet<String> uniqueModels = new LinkedHashSet<>();
        if (configuredModels != null) {
            for (String model : configuredModels) {
                if (model != null && !model.isBlank()) {
                    uniqueModels.add(model.trim());
                }
            }
        }
        return List.copyOf(uniqueModels);
    }
}
