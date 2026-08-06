package com.chatbox.chatbox.config;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class PromptGeminiConfig {

    private static final String PROMPT_TEMPLATE = """
        Bạn là trợ lý tư vấn của GreenShield Mekong.
        Ngôn ngữ trả lời bắt buộc: %s
        %s

        Quy tắc trả lời:
        - Trả lời ngắn gọn, rõ ràng; ưu tiên 1-3 câu nếu câu hỏi đơn giản.
        - Không lặp lại câu hỏi của người dùng.
        - Chỉ sử dụng dữ liệu có trong kho tri thức đã chọn. Nếu chưa có thông tin, hãy nói rõ GreenShield chưa công bố.
        - Không tự tạo giá, chính sách, chứng nhận, số liệu, thông tin liên hệ hoặc đường dẫn.
        - Khi người dùng hỏi website, link hoặc cách truy cập tính năng, phải dùng đúng URL tuyệt đối trong kho tri thức.
        - Trình bày URL dưới dạng liên kết Markdown có nhãn rõ ràng bằng ngôn ngữ trả lời; không dịch hoặc thay đổi URL.
        - Nếu đường dẫn được ghi nhận là chưa hoạt động, hãy thông báo và hướng người dùng về trang chủ chính thức.

        Chủ đề đang chọn: %s

        Kho tri thức liên quan:
        %s

        Lịch sử trò chuyện gần đây:
        %s

        Câu hỏi mới:
        %s
        """;

    public String buildPrompt(
            String topicContext,
            String knowledgeContext,
            String conversationHistory,
            String userMessage,
            String language) {
        boolean english = language != null && language.toLowerCase(Locale.ROOT).startsWith("en");
        String responseLanguage = english ? "English" : "Tiếng Việt";
        String languageInstruction = english
                ? "Always answer entirely in English, even when the knowledge base, topic, or conversation history is written in Vietnamese. Translate the relevant facts naturally into English."
                : "Luôn trả lời hoàn toàn bằng tiếng Việt, kể cả khi câu hỏi hoặc lịch sử trò chuyện có nội dung bằng ngôn ngữ khác.";
        return String.format(
                PROMPT_TEMPLATE,
                responseLanguage,
                languageInstruction,
                topicContext,
                knowledgeContext,
                conversationHistory,
                userMessage
        );
    }
}
