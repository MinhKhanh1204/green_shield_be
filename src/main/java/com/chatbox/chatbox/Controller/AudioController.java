package com.chatbox.chatbox.Controller;

import com.chatbox.chatbox.model.AiAudioGenerationLog;
import com.chatbox.chatbox.repository.AiAudioGenerationLogRepository;
import com.chatbox.chatbox.service.TextToSpeechService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/audio")
public class AudioController {

    private final TextToSpeechService textToSpeechService;
    private final AiAudioGenerationLogRepository logRepository;
    private final int dailyLimit;

    public AudioController(
            TextToSpeechService textToSpeechService,
            AiAudioGenerationLogRepository logRepository,
            @Value("${app.ai.daily-audio-limit:200}") int dailyLimit
    ) {
        this.textToSpeechService = textToSpeechService;
        this.logRepository = logRepository;
        this.dailyLimit = dailyLimit;
    }

    @GetMapping("/{code}")
    public ResponseEntity<byte[]> play(@PathVariable String code) {
        String text = decodeBase64Url(code);
        if (text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Instant startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        long countToday = logRepository.countSince(startOfDay);
        if (countToday >= dailyLimit) {
            return ResponseEntity.status(429)
                    .header("X-AI-Audio-Limit", String.valueOf(dailyLimit))
                    .header("X-AI-Audio-Used", String.valueOf(countToday))
                    .build();
        }

        byte[] audio = textToSpeechService.synthesizeVi(text);
        if (audio.length == 0) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        logRepository.save(AiAudioGenerationLog.builder().createdAt(Instant.now()).build());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("audio/mpeg"));
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

        return new ResponseEntity<>(audio, headers, HttpStatus.OK);
    }

    private String decodeBase64Url(String code) {
        try {
            String base = code.replace('-', '+').replace('_', '/');
            int mod = base.length() % 4;
            if (mod == 2) base += "==";
            else if (mod == 3) base += "=";
            byte[] bytes = Base64.getDecoder().decode(base);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }
}

