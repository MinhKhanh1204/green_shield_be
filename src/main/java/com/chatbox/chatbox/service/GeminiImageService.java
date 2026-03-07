package com.chatbox.chatbox.service;

import com.chatbox.chatbox.model.AiImageGenerationLog;
import com.chatbox.chatbox.repository.AiImageGenerationLogRepository;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class GeminiImageService {

    private static final String MODEL = "gemini-3.1-flash-image-preview";
    private final String apiKey;
    private final AiImageGenerationLogRepository logRepository;
    private final int dailyLimit;

    public GeminiImageService(
            @Value("${spring.gemini.api.key:}") String apiKey,
            @Value("${app.ai.daily-image-limit:20}") int dailyLimit,
            AiImageGenerationLogRepository logRepository) {
        this.apiKey = (apiKey != null && !apiKey.isBlank()) ? apiKey : "";
        this.dailyLimit = dailyLimit;
        this.logRepository = logRepository;
    }

    /** Returns base64 PNG (no data URL prefix), or throws if limit exceeded / API error. */
    public String generateImage(String prompt) {
        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        long countToday = logRepository.countSince(startOfDay);
        if (countToday >= dailyLimit) {
            throw new DailyLimitExceededException(
                    "Hệ thống đã đạt giới hạn tạo ảnh AI trong ngày (" + dailyLimit + " ảnh). Vui lòng thử lại vào ngày mai.");
        }

        if (this.apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key not configured");
        }

        try (Client client = Client.builder().apiKey(this.apiKey).build()) {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities("TEXT", "IMAGE")
                    .build();

            String textPrompt = (prompt != null && !prompt.isBlank()) ? prompt : "A simple illustration";
            GenerateContentResponse response = client.models.generateContent(MODEL, textPrompt, config);

            String base64Image = null;
            for (Part part : response.parts()) {
                if (part.inlineData().isPresent()) {
                    var blob = part.inlineData().get();
                    if (blob.data().isPresent()) {
                        byte[] bytes = blob.data().get();
                        base64Image = Base64.getEncoder().encodeToString(bytes);
                        break;
                    }
                }
            }

            if (base64Image == null) {
                throw new RuntimeException("Không nhận được ảnh từ AI. Thử lại với prompt khác.");
            }

            logRepository.save(AiImageGenerationLog.builder().createdAt(Instant.now()).build());
            return base64Image;
        } catch (DailyLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage() != null ? e.getMessage() : "Lỗi tạo ảnh AI.", e);
        }
    }

    public static class DailyLimitExceededException extends RuntimeException {
        public DailyLimitExceededException(String message) {
            super(message);
        }
    }
}
