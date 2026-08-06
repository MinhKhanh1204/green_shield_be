package com.chatbox.chatbox.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiServiceModelRotationTests {

    private final Queue<Integer> responseStatuses = new ConcurrentLinkedQueue<>();
    private final List<String> requestBodies = new CopyOnWriteArrayList<>();
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", this::handleRequest);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void rotatesImmediatelyWhenThePrimaryModelReturns404() {
        responseStatuses.addAll(List.of(404, 200));
        GeminiService service = createService(List.of("missing-model", "fallback-model"), 1);

        String response = service.generateContent("Hello");

        assertThat(response).isEqualTo("OK");
        assertThat(requestBodies).hasSize(2);
        assertThat(requestBodies.get(0)).contains("\"model\":\"missing-model\"");
        assertThat(requestBodies.get(1))
                .contains("\"model\":\"fallback-model\"")
                .contains("\"max_tokens\":256")
                .contains("\"temperature\":0.2");
    }

    @Test
    void rotatesWhenAModelRejectsTheRequestAsUnsupported() {
        responseStatuses.addAll(List.of(400, 200));
        GeminiService service = createService(List.of("unsupported-model", "fallback-model"), 1);

        String response = service.generateContent("Hello");

        assertThat(response).isEqualTo("OK");
        assertThat(requestBodies).hasSize(2);
        assertThat(requestBodies.get(0)).contains("\"model\":\"unsupported-model\"");
        assertThat(requestBodies.get(1)).contains("\"model\":\"fallback-model\"");
    }

    @Test
    void retriesRateLimitOnceThenRotatesToTheNextModel() {
        responseStatuses.addAll(List.of(429, 429, 200));
        GeminiService service = createService(List.of("busy-model", "fallback-model"), 1);

        String response = service.generateContent("Hello");

        assertThat(response).isEqualTo("OK");
        assertThat(requestBodies).hasSize(3);
        assertThat(requestBodies.get(0)).contains("\"model\":\"busy-model\"");
        assertThat(requestBodies.get(1)).contains("\"model\":\"busy-model\"");
        assertThat(requestBodies.get(2)).contains("\"model\":\"fallback-model\"");
    }

    @Test
    void returnsDailyLimitMessageWhenAllModelsAreRateLimited() {
        responseStatuses.addAll(List.of(429, 429, 429));
        GeminiService service = createService(List.of("model-a", "model-b", "model-c"), 0);

        String response = service.generateContent("Hello");

        assertThat(response).isEqualTo("Bạn đã đạt giới hạn câu hỏi hôm nay!");
        assertThat(requestBodies).hasSize(3);
    }

    @Test
    void returnsUnavailableMessageWhenEveryConfiguredModelIsMissing() {
        responseStatuses.addAll(List.of(404, 404));
        GeminiService service = createService(List.of("missing-a", "missing-b"), 1);

        String response = service.generateContent("Hello");

        assertThat(response).isEqualTo(GeminiService.UNAVAILABLE_MESSAGE);
        assertThat(requestBodies).hasSize(2);
    }

    @Test
    void imageRequestsUseTheDedicatedImageModelList() {
        responseStatuses.add(200);
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        GeminiService service = new GeminiService(
                "test-key",
                List.of("text-only-model"),
                List.of("image-model"),
                baseUrl,
                256,
                0.2,
                0
        );

        String response = service.generateContentWithImage("Mô tả ảnh", "AAAA");

        assertThat(response).isEqualTo("OK");
        assertThat(requestBodies).singleElement().satisfies(body -> assertThat(body)
                .contains("\"model\":\"image-model\"")
                .doesNotContain("text-only-model")
                .contains("data:image/png;base64,AAAA"));
    }

    private GeminiService createService(List<String> models, int retriesPerModel) {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new GeminiService("test-key", models, baseUrl, 256, 0.2, retriesPerModel);
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        Integer configuredStatus = responseStatuses.poll();
        int status = configuredStatus != null ? configuredStatus : 200;
        exchange.getResponseHeaders().add("Content-Type", "application/json");

        if (status == 429) {
            exchange.getResponseHeaders().add("Retry-After", "0");
        }

        String body = status == 200
                ? "{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}"
                : "{\"error\":{\"message\":\"Test error\"}}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
