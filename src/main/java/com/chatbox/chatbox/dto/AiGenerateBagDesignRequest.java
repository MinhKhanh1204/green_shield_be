package com.chatbox.chatbox.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiGenerateBagDesignRequest {

    private String prompt;

    private Long templateId;

    private Boolean generateFront;

    private Boolean generateBack;

    public boolean isGenerateFront() {
        return generateFront == null || Boolean.TRUE.equals(generateFront);
    }

    public boolean isGenerateBack() {
        return generateBack == null || Boolean.TRUE.equals(generateBack);
    }
}

