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

        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Save user message
        ChatMessage userMessage = new ChatMessage("USER", prompt, session);
        chatMessageRepository.save(userMessage);

        // Fetch full history
        List<ChatMessage> history =
                chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> requestMap = new HashMap<>();

        requestMap.put("model", "llama-3.1-8b-instant");

        List<Map<String, String>> messages = new ArrayList<>();

        for (ChatMessage msg : history) {
            Map<String, String> m = new HashMap<>();
            m.put("role", msg.getRole().equals("USER") ? "user" : "assistant");
            m.put("content", msg.getContent());
            messages.add(m);
        }

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

        JsonNode jsonNode = mapper.readTree(response.body());

        String aiResponse = jsonNode
                .get("choices")
                .get(0)
                .get("message")
                .get("content")
                .asText();

        // Save AI response
        ChatMessage aiMessage = new ChatMessage("AI", aiResponse, session);
        chatMessageRepository.save(aiMessage);

        return aiResponse;
    }
}
