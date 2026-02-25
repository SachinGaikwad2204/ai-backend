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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // Get conversation history
        List<ChatMessage> history =
                chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

        StringBuilder fullPrompt = new StringBuilder();

        for (ChatMessage msg : history) {
            fullPrompt.append(msg.getRole())
                    .append(": ")
                    .append(msg.getContent())
                    .append("\n");
        }

        // Build safe JSON request
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("model", "llama3");
        requestMap.put("prompt", fullPrompt.toString());
        requestMap.put("stream", false);

        String requestBody = mapper.writeValueAsString(requestMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("OLLAMA RESPONSE: " + response.body());

        JsonNode jsonNode = mapper.readTree(response.body());

        if (jsonNode.has("response")) {

            String aiResponse = jsonNode.get("response").asText();

            ChatMessage aiMessage = new ChatMessage("AI", aiResponse, session);
            chatMessageRepository.save(aiMessage);

            return aiResponse;

        } else {

            System.out.println("Ollama error: " + response.body());
            return "AI Error: " + response.body();
        }
    }

public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamResponse(String prompt, Long sessionId) {

    org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter =
            new org.springframework.web.servlet.mvc.method.annotation.SseEmitter();

    new Thread(() -> {
        try {

            ChatSession session;

            if (sessionId == null) {
                session = new ChatSession();
                session.setTitle("New Chat");
                session = chatSessionRepository.save(session);
            } else {
                session = chatSessionRepository.findById(sessionId)
                        .orElseThrow(() -> new RuntimeException("Session not found"));
            }

            ChatMessage userMessage = new ChatMessage("USER", prompt, session);
            chatMessageRepository.save(userMessage);

            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", "llama3");
            requestMap.put("prompt", prompt);
            requestMap.put("stream", true);

            String requestBody = mapper.writeValueAsString(requestMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<java.io.InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            java.io.BufferedReader reader =
                    new java.io.BufferedReader(new java.io.InputStreamReader(response.body()));

            StringBuilder fullResponse = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {

                JsonNode node = mapper.readTree(line);

                if (node.has("response")) {
                    String chunk = node.get("response").asText();
                    fullResponse.append(chunk);
                    emitter.send(chunk);
                }

                if (node.has("done") && node.get("done").asBoolean()) {
                    break;
                }
            }

            ChatMessage aiMessage = new ChatMessage("AI", fullResponse.toString(), session);
            chatMessageRepository.save(aiMessage);

            emitter.complete();

        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }).start();

    return emitter;
}
}
