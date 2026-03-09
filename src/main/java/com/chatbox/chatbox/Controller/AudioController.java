package com.chatbox.chatbox.Controller;

import com.chatbox.chatbox.service.TextToSpeechService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@RequestMapping("/api/v1/audio")
public class AudioController {

    private final TextToSpeechService textToSpeechService;

    public AudioController(TextToSpeechService textToSpeechService) {
        this.textToSpeechService = textToSpeechService;
    }

    @GetMapping("/{code}")
    public ResponseEntity<byte[]> play(@PathVariable String code) {
        String text = decodeBase64Url(code);
        if (text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] audio = textToSpeechService.synthesizeVi(text);
        if (audio.length == 0) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

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

