package com.chatbox.chatbox.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AiGenerateBagDesignResponse {

    /**
     * Base64 PNG (không kèm tiền tố data URL) cho patch mặt trước.
     */
    private String frontImageBase64;

    /**
     * Base64 PNG (không kèm tiền tố data URL) cho patch mặt sau.
     */
    private String backImageBase64;
}

