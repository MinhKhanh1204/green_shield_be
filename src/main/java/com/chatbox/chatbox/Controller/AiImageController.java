package com.chatbox.chatbox.Controller;

import com.chatbox.chatbox.dto.AiGenerateImageRequest;
import com.chatbox.chatbox.dto.AiGenerateImageResponse;
import com.chatbox.chatbox.dto.AiGenerateBagDesignRequest;
import com.chatbox.chatbox.dto.AiGenerateBagDesignResponse;
import com.chatbox.chatbox.model.BagTemplate;
import com.chatbox.chatbox.repository.BagTemplateRepository;
import com.chatbox.chatbox.service.GeminiImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiImageController {

    private final GeminiImageService geminiImageService;
    private final BagTemplateRepository bagTemplateRepository;

    public AiImageController(GeminiImageService geminiImageService, BagTemplateRepository bagTemplateRepository) {
        this.geminiImageService = geminiImageService;
        this.bagTemplateRepository = bagTemplateRepository;
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

    @PostMapping("/generate-bag-design")
    public ResponseEntity<?> generateBagDesign(@RequestBody AiGenerateBagDesignRequest request) {
        String prompt = request != null && request.getPrompt() != null ? request.getPrompt().trim() : "";
        if (prompt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt không được để trống."));
        }
        if (request == null || request.getTemplateId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Thiếu templateId."));
        }

        BagTemplate template = bagTemplateRepository.findById(request.getTemplateId())
                .orElse(null);
        if (template == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Bag template không tồn tại."));
        }

        boolean generateFront = request.isGenerateFront();
        boolean generateBack = request.isGenerateBack();
        if (!generateFront && !generateBack) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ít nhất phải chọn một mặt để tạo (front/back)."));
        }

        try {
            GeminiImageService.BagDesignResult result =
                    geminiImageService.generateBagDesign(prompt, template, generateFront, generateBack);

            AiGenerateBagDesignResponse resp = AiGenerateBagDesignResponse.builder()
                    .frontImageBase64(result.getFrontImageBase64())
                    .backImageBase64(result.getBackImageBase64())
                    .build();
            return ResponseEntity.ok(resp);
        } catch (GeminiImageService.DailyLimitExceededException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Lỗi tạo thiết kế túi bằng AI. Vui lòng thử lại."));
        }
    }
}
