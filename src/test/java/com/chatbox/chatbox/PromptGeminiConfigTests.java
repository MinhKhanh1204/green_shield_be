package com.chatbox.chatbox;

import com.chatbox.chatbox.config.PromptGeminiConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptGeminiConfigTests {

    private final PromptGeminiConfig promptConfig = new PromptGeminiConfig();

    @Test
    void requiresEnglishResponsesWhenEnglishIsSelected() {
        String prompt = promptConfig.buildPrompt(
                "Sản phẩm",
                "Dữ liệu tiếng Việt",
                "Người dùng: Hello",
                "What products do you sell?",
                "en"
        );

        assertThat(prompt)
                .contains("Ngôn ngữ trả lời bắt buộc: English")
                .contains("Always answer entirely in English")
                .contains("What products do you sell?");
    }

    @Test
    void defaultsToVietnameseResponses() {
        String prompt = promptConfig.buildPrompt("", "", "", "GreenShield là gì?", null);

        assertThat(prompt)
                .contains("Ngôn ngữ trả lời bắt buộc: Tiếng Việt")
                .contains("Luôn trả lời hoàn toàn bằng tiếng Việt");
    }
}
