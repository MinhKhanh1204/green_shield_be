package com.chatbox.chatbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateImageResponse {
    /** Base64-encoded PNG (no data URL prefix). */
    private String imageBase64;
}
