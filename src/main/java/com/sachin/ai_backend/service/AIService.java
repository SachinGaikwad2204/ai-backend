package com.sachin.ai_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sachin.ai_backend.model.ChatMessage;
import com.sachin.ai_backend.model.ChatSession;
import com.sachin.ai_backend.repository.ChatMessageRepository;
import com.sachin.ai_backend.repository.ChatSessionRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AIService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final HttpClient httpClient;

    public AIService(ChatSessionRepository chatSessionRepository,
                     ChatMessageRepository chatMessageRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String getResponse(String prompt, Long sessionId) throws Exception {

        ChatSession session;

        if (sessionId == null) {
            session = new ChatSession();
            session.setTitle("New Chat");
            session = chatSessionRepository.save(session);
        } else {
            session = chatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));
        }

        // Save USER message
        ChatMessage userMessage = new ChatMessage("USER", prompt, session);
        chatMessageRepository.save(userMessage);

        // Build Groq request
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "llama3-8b-8192");

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        requestMap.put("messages", messages);

        String requestBody = mapper.writeValueAsString(requestMap);

        String apiKey = System.getenv("GROQ_API_KEY");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Groq RAW RESPONSE: " + response.body());

        JsonNode jsonNode = mapper.readTree(response.body());

        if (jsonNode.has("choices")
                && jsonNode.get("choices").isArray()
                && jsonNode.get("choices").size() > 0) {

            String aiResponse = jsonNode
                    .get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

            ChatMessage aiMessage = new ChatMessage("AI", aiResponse, session);
            chatMessageRepository.save(aiMessage);

            return aiResponse;

        } else {

            return "AI Error: " + response.body();
        }
    }
}
