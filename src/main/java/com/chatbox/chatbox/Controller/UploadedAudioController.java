package com.chatbox.chatbox.Controller;

import com.chatbox.chatbox.model.UploadedAudio;
import com.chatbox.chatbox.repository.UploadedAudioRepository;
import com.chatbox.chatbox.service.CloudinaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audio")
public class UploadedAudioController {

    private static final long MAX_AUDIO_BYTES = 5L * 1024 * 1024;

    private final CloudinaryService cloudinaryService;
    private final UploadedAudioRepository uploadedAudioRepository;

    public UploadedAudioController(CloudinaryService cloudinaryService, UploadedAudioRepository uploadedAudioRepository) {
        this.cloudinaryService = cloudinaryService;
        this.uploadedAudioRepository = uploadedAudioRepository;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(error("FILE_EMPTY", "File audio không hợp lệ."));
        }
        if (file.getSize() > MAX_AUDIO_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(error("FILE_TOO_LARGE", "File audio vượt quá 5MB."));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("audio/")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(error("UNSUPPORTED_TYPE", "Chỉ hỗ trợ file audio (audio/*)."));
        }

        try {
            Map<String, Object> up = cloudinaryService.uploadAudio(file, "greenshield/audio");
            String secureUrl = (String) up.get("secure_url");
            String publicId = (String) up.get("public_id");
            if (secureUrl == null || secureUrl.isBlank() || publicId == null || publicId.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(error("UPLOAD_FAILED", "Không thể upload audio. Vui lòng thử lại."));
            }

            UploadedAudio saved = uploadedAudioRepository.save(UploadedAudio.builder()
                    .secureUrl(secureUrl)
                    .publicId(publicId)
                    .contentType(contentType)
                    .bytes(file.getSize())
                    .originalName(file.getOriginalFilename())
                    .build());

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", saved.getId());
            resp.put("secureUrl", saved.getSecureUrl());
            resp.put("contentType", saved.getContentType());
            resp.put("bytes", saved.getBytes());
            resp.put("originalName", saved.getOriginalName());
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(error("CLOUDINARY_NOT_CONFIGURED", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(error("UPLOAD_ERROR", "Không thể upload audio. Vui lòng thử lại."));
        }
    }

    @GetMapping("/file/{id}")
    public ResponseEntity<?> getFile(@PathVariable String id) {
        return uploadedAudioRepository.findById(id)
                .map(a -> {
                    if (a.getContentType() == null || !a.getContentType().toLowerCase().startsWith("audio/")) {
                        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                                .body(error("NOT_AUDIO", "Nội dung không phải audio."));
                    }
                    if (a.getSecureUrl() == null || a.getSecureUrl().isBlank()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(error("NOT_FOUND", "Không tìm thấy audio."));
                    }

                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(a.getSecureUrl()))
                            .build();
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(error("NOT_FOUND", "Không tìm thấy audio.")));
    }

    private Map<String, Object> error(String code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("message", message);
        return m;
    }
}

