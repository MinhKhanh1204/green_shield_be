package com.chatbox.chatbox.service;

import com.chatbox.chatbox.model.AiImageGenerationLog;
import com.chatbox.chatbox.model.BagTemplate;
import com.chatbox.chatbox.repository.AiImageGenerationLogRepository;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiImageService {

    private static final String MODEL = "gemini-3.1-flash-image-preview";
    private final String apiKey;
    private final AiImageGenerationLogRepository logRepository;
    private final int dailyLimit;

    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    /**
     * Generate artwork patches for bag front/back custom areas.
     * Returns base64 PNG (no data URL prefix) for each side when available.
     */
    public BagDesignResult generateBagDesign(String userPrompt, BagTemplate template, boolean generateFront, boolean generateBack) {
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
            DesignArea frontArea = generateFront ? parseArea(template.getFrontCustomArea()) : null;
            DesignArea backArea = generateBack ? parseArea(template.getBackCustomArea()) : null;

            String prompt = buildBagDesignPrompt(userPrompt, template, frontArea, backArea, generateFront, generateBack);

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities("IMAGE")
                    .build();

            GenerateContentResponse response = client.models.generateContent(MODEL, prompt, config);

            List<String> images = new ArrayList<>();
            for (Part part : response.parts()) {
                if (part.inlineData().isPresent()) {
                    var blob = part.inlineData().get();
                    if (blob.data().isPresent()) {
                        byte[] bytes = blob.data().get();
                        images.add(Base64.getEncoder().encodeToString(bytes));
                    }
                }
            }

            if (images.isEmpty()) {
                throw new RuntimeException("Không nhận được ảnh từ AI cho thiết kế túi. Thử lại với prompt khác.");
            }

            String frontBase64 = null;
            String backBase64 = null;

            int idx = 0;
            if (generateFront && idx < images.size()) {
                frontBase64 = images.get(idx++);
            }
            if (generateBack && idx < images.size()) {
                backBase64 = images.get(idx++);
            }
            // Fallback: nếu chỉ có 1 ảnh, dùng chung cho cả hai mặt khi được yêu cầu
            if (generateBack && backBase64 == null && !images.isEmpty()) {
                backBase64 = images.get(0);
            }

            logRepository.save(AiImageGenerationLog.builder().createdAt(Instant.now()).build());
            return new BagDesignResult(frontBase64, backBase64);
        } catch (DailyLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage() != null ? e.getMessage() : "Lỗi tạo thiết kế túi bằng AI.", e);
        }
    }

    private DesignArea parseArea(String json) {
        if (json == null || json.isBlank()) {
            return new DesignArea(10, 10, 80, 80);
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            double x = node.path("x").asDouble(10);
            double y = node.path("y").asDouble(10);
            double width = node.path("width").asDouble(80);
            double height = node.path("height").asDouble(80);
            if (width <= 0) width = 80;
            if (height <= 0) height = 80;
            return new DesignArea(x, y, width, height);
        } catch (Exception e) {
            return new DesignArea(10, 10, 80, 80);
        }
    }

    private String buildBagDesignPrompt(String userPrompt,
                                        BagTemplate template,
                                        DesignArea frontArea,
                                        DesignArea backArea,
                                        boolean generateFront,
                                        boolean generateBack) {
        String safePrompt = (userPrompt != null && !userPrompt.isBlank())
                ? userPrompt.trim()
                : "Một minh hoạ tối giản, hiện đại, cảm hứng xanh và tích cực.";

        StringBuilder sb = new StringBuilder();
        sb.append("You are designing artwork patches for a cotton tote bag called GreenShield.\n");
        sb.append("The bag has two printable areas (front and back). ");
        sb.append("For each side, you will generate ONLY the artwork patch that fits inside the allowed rectangle. ");
        sb.append("Do NOT include the bag, mockup, human models, or any background outside this rectangle. ");
        sb.append("Return clean artwork with transparent or solid background that can be overlaid on top of the bag image.\n\n");

        sb.append("General style:\n");
        sb.append("- Eco-friendly, nature-inspired, soft colors, clean lines.\n");
        sb.append("- The design should be readable on fabric and not too noisy.\n");
        sb.append("- Avoid putting important details too close to the edges because the patch might be slightly cropped.\n\n");

        sb.append("User idea for the message or theme:\n");
        sb.append(safePrompt).append("\n\n");

        if (generateFront && frontArea != null) {
            sb.append("Front patch:\n");
            sb.append("- Target aspect ratio approximately ")
                    .append(Math.round(frontArea.getWidth()))
                    .append(":")
                    .append(Math.round(frontArea.getHeight()))
                    .append(" (width:height based on the allowed area).\n");
            sb.append("- Focus on a composition that works well on the front of a tote bag.\n\n");
        }

        if (generateBack && backArea != null) {
            sb.append("Back patch:\n");
            sb.append("- Target aspect ratio approximately ")
                    .append(Math.round(backArea.getWidth()))
                    .append(":")
                    .append(Math.round(backArea.getHeight()))
                    .append(" (width:height based on the allowed area).\n");
            sb.append("- Optionally a variation or complementary design to the front side.\n\n");
        }

        sb.append("Output:\n");
        sb.append("- Produce one image for each requested side (front/back), in PNG format.\n");
        sb.append("- Each image should only contain the artwork patch, not the bag mockup.\n");

        return sb.toString();
    }

    private static class DesignArea {
        private final double x;
        private final double y;
        private final double width;
        private final double height;

        DesignArea(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }
    }

    public static class BagDesignResult {
        private final String frontImageBase64;
        private final String backImageBase64;

        public BagDesignResult(String frontImageBase64, String backImageBase64) {
            this.frontImageBase64 = frontImageBase64;
            this.backImageBase64 = backImageBase64;
        }

        public String getFrontImageBase64() {
            return frontImageBase64;
        }

        public String getBackImageBase64() {
            return backImageBase64;
        }
    }

    public static class DailyLimitExceededException extends RuntimeException {
        public DailyLimitExceededException(String message) {
            super(message);
        }
    }
}
