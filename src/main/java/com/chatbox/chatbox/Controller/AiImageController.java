package com.chatbox.chatbox.Controller;

import com.chatbox.chatbox.dto.AiGenerateImageRequest;
import com.chatbox.chatbox.dto.AiGenerateImageResponse;
import com.chatbox.chatbox.service.GeminiImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiImageController {

    private final GeminiImageService geminiImageService;

    public AiImageController(GeminiImageService geminiImageService) {
        this.geminiImageService = geminiImageService;
    }

    @PostMapping("/generate-image")
    public ResponseEntity<?> generateImage(@RequestBody AiGenerateImageRequest request) {
        String prompt = request != null && request.getPrompt() != null ? request.getPrompt().trim() : "";
        if (prompt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt không được để trống."));
        }
        try {
            String base64 = geminiImageService.generateImage(prompt);
            return ResponseEntity.ok(AiGenerateImageResponse.builder().imageBase64(base64).build());
        } catch (GeminiImageService.DailyLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Lỗi tạo ảnh. Vui lòng thử lại."));
        }
    }
}
