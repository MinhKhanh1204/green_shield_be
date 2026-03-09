package com.chatbox.chatbox.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Service
public class TextToSpeechService {

    private final String apiKey;
    private final WebClient webClient;

    public TextToSpeechService(@Value("${google.tts.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        if (this.apiKey != null && !this.apiKey.isBlank()) {
            String masked = this.apiKey.length() > 8
                    ? this.apiKey.substring(0, 4) + "..." + this.apiKey.substring(this.apiKey.length() - 4)
                    : this.apiKey;
            System.out.println("TTS using apiKey=" + masked + " (len=" + this.apiKey.length() + ")");
        } else {
            System.out.println("TTS apiKey is blank/undefined");
        }
        this.webClient = WebClient.builder()
                .baseUrl("https://texttospeech.googleapis.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public byte[] synthesizeVi(String text) {
        if (text == null || text.isBlank() || apiKey == null || apiKey.isBlank()) {
            return new byte[0];
        }

        Map<String, Object> body = Map.of(
                "input", Map.of("text", text),
                "voice", Map.of(
                        "languageCode", "vi-VN",
                        "ssmlGender", "NEUTRAL"
                ),
                "audioConfig", Map.of(
                        "audioEncoding", "MP3",
                        "speakingRate", 1.0,
                        "pitch", 0.0
                )
        );

        try {
            Map<?, ?> response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/text:synthesize")
                            .queryParam("key", apiKey)
                            .build())
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            r -> r.bodyToMono(String.class).map(bodyStr -> {
                                System.err.println("TTS HTTP " + r.statusCode() + ": " + bodyStr);
                                return new RuntimeException("TTS HTTP " + r.statusCode());
                            })
                    )
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));

            if (response == null || !response.containsKey("audioContent")) {
                System.err.println("TTS: empty response or missing audioContent");
                return new byte[0];
            }
            String base64 = String.valueOf(response.get("audioContent"));
            return Base64.getDecoder().decode(base64);
        } catch (Exception e) {
            System.err.println("TTS error: " + e.getMessage());
            return new byte[0];
        }
    }
}

