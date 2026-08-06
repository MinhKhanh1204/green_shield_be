package com.chatbox.chatbox.service;

import com.chatbox.chatbox.config.PromptGeminiConfig;
import com.chatbox.chatbox.config.TopicConfig;
import com.chatbox.chatbox.dto.ChatRequest;
import com.chatbox.chatbox.model.ConversationSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ChatService {

    private final GeminiService geminiService;

    @Value("${app.ai.chat.max-history-messages:8}")
    private int maxHistoryMessages = 8;

    @Value("${app.ai.chat.max-history-chars:4000}")
    private int maxHistoryChars = 4_000;

    @Autowired
    private KnowledgeLoaderService knowledgeLoaderService;

    @Autowired
    private TopicConfig topicConfig;

    @Autowired
    private PromptGeminiConfig promptGeminiConfig;

    public ChatService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * Chat bình thường (text only)
     */
    public String chatWithKnowledge(ChatRequest request, ConversationSession convo) {
        String language = normalizeLanguage(request.getLanguage());
        String topic = (convo.getTopic() != null) ? convo.getTopic().trim().toLowerCase() : "";
        String topicContext = topicConfig.getTopicContext(topic);
        String selectionQuery = topic + " " + topicContext + " " + request.getMessage();
        String knowledgeContext = knowledgeLoaderService.loadKnowledge(selectionQuery);
        String conversationHistory = getRecentConversationHistory(convo);

        String prompt = promptGeminiConfig.buildPrompt(
                topicContext,
                knowledgeContext,
                conversationHistory,
                request.getMessage(),
                language
        );

        return localizeServiceMessage(geminiService.generateContent(prompt), language);
    }

    /**
     * Chat bằng hình ảnh (image + text)
     */
    public String chatWithImage(ChatRequest request, ConversationSession convo, String imageBase64) {
        String language = normalizeLanguage(request.getLanguage());
        String topic = (convo.getTopic() != null) ? convo.getTopic().trim().toLowerCase() : "";
        String topicContext = topicConfig.getTopicContext(topic);
        String selectionQuery = topic + " " + topicContext + " " + request.getMessage();
        String knowledgeContext = knowledgeLoaderService.loadKnowledge(selectionQuery);
        String conversationHistory = getRecentConversationHistory(convo);

        String prompt = promptGeminiConfig.buildPrompt(
                topicContext,
                knowledgeContext,
                conversationHistory,
                request.getMessage(),
                language
        );

        return localizeServiceMessage(geminiService.generateContentWithImage(prompt, imageBase64), language);
    }

    private String normalizeLanguage(String language) {
        return language != null && language.toLowerCase(Locale.ROOT).startsWith("en") ? "en" : "vi";
    }

    private String localizeServiceMessage(String response, String language) {
        if (!"en".equals(language)) {
            return response;
        }
        if (GeminiService.DAILY_LIMIT_MESSAGE.equals(response)) {
            return "You have reached today's question limit!";
        }
        if (GeminiService.UNAVAILABLE_MESSAGE.equals(response)) {
            return "The AI service is temporarily unavailable. Please try again later!";
        }
        return response;
    }

    private String getRecentConversationHistory(ConversationSession convo) {
        List<String> messages = convo.getMessages();
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        int historyMessageLimit = Math.max(1, maxHistoryMessages);
        int start = Math.max(0, messages.size() - historyMessageLimit);
        String history = String.join("\n", messages.subList(start, messages.size()));
        int historyCharLimit = Math.max(500, maxHistoryChars);
        if (history.length() <= historyCharLimit) {
            return history;
        }
        return history.substring(history.length() - historyCharLimit);
    }
}
