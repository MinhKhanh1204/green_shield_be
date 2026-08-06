package com.chatbox.chatbox.Controller;

import com.chatbox.chatbox.config.TopicConfig;
import com.chatbox.chatbox.dto.ChatRequest;
import com.chatbox.chatbox.model.ConversationSession;
import com.chatbox.chatbox.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;
    private final TopicConfig topicConfig;

    public ChatController(ChatService chatService, TopicConfig topicConfig) {
        this.chatService = chatService;
        this.topicConfig = topicConfig;
    }

    @PostMapping("/select-topic")
    public String selectTopic(@RequestBody Map<String, String> body, HttpSession session) {
        String topic = body.get("topic");
        String language = resolveLanguage(body.get("language"), session);
        session.setAttribute("topic", topic);
        session.setAttribute("language", language);
        String topicLabel = topicConfig.getTopicLabel(topic, language);
        return "en".equals(language)
                ? "You are now chatting about: " + topicLabel + "."
                : "Bạn đang trò chuyện về: " + topicLabel + ".";
    }

//    @PostMapping("/message")
//    public String chat(@RequestBody ChatRequest chatRequest, HttpSession session) {
//        String topic = (String) session.getAttribute("topic");
//        if (topic != null) {
//            chatRequest.setTopic(topic);
//        }
//        return chatService.chatWithKnowledge(chatRequest);
//    }

    @PostMapping(value = "/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String chatOnlyMessage(@RequestBody Map<String, String> payload, HttpSession session) {
        String language = resolveLanguage(payload.get("language"), session);
        try {
            String message = payload.get("message");
            String topic = (String) session.getAttribute("topic");
            session.setAttribute("language", language);
            if (topic == null) {
                return "en".equals(language) ? "Please select a topic first." : "Vui lòng chọn chủ đề trước.";
            }
            if (message == null || message.isBlank()) {
                return "en".equals(language) ? "The message cannot be empty." : "Tin nhắn không được để trống.";
            }

            ConversationSession convo = (ConversationSession) session.getAttribute("conversation");
            if (convo == null || !topic.equals(convo.getTopic())) {
                convo = new ConversationSession(topic);
                session.setAttribute("conversation", convo);
            }

            convo.addMessage("en".equals(language) ? "User" : "Người dùng", message);

            ChatRequest chatRequest = new ChatRequest(message, topic, language);
            String reply = chatService.chatWithKnowledge(chatRequest, convo);

            convo.addMessage("en".equals(language) ? "Assistant" : "Trợ lý", reply);
            session.setAttribute("conversation", convo);

            return reply;

        } catch (Exception e) {
            e.printStackTrace();
            return "en".equals(language)
                    ? "An error occurred: " + e.getMessage()
                    : "Đã xảy ra lỗi: " + e.getMessage();
        }
    }

    @GetMapping("/topics")
    public Map<String, String> getChatTopics() {
        return topicConfig.getAllTopics();
    }

    private String resolveLanguage(String requestedLanguage, HttpSession session) {
        String language = requestedLanguage;
        if (language == null || language.isBlank()) {
            language = (String) session.getAttribute("language");
        }
        return language != null && language.toLowerCase(Locale.ROOT).startsWith("en") ? "en" : "vi";
    }
}
