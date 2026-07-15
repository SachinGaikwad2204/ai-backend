package com.sachin.ai_backend.controller;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.ResponseEntity;
import com.sachin.ai_backend.model.ChatMessage;
import com.sachin.ai_backend.model.ChatSession;
import com.sachin.ai_backend.repository.ChatMessageRepository;
import com.sachin.ai_backend.repository.ChatSessionRepository;
import com.sachin.ai_backend.service.AIService;
import com.sachin.ai_backend.service.SentimentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    private final AIService aiService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SentimentService sentimentService;

    public AIController(AIService aiService,
                        ChatSessionRepository chatSessionRepository,
                        ChatMessageRepository chatMessageRepository,
                        SentimentService sentimentService) {
        this.aiService = aiService;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.sentimentService = sentimentService;
    }

    // ========== SESSION ENDPOINTS ==========
    
    @PostMapping("/sessions")
    public ChatSession createSession() {
        ChatSession session = new ChatSession();
        session.setTitle("New Chat");
        return chatSessionRepository.save(session);
    }

    @GetMapping("/sessions")
    public List<ChatSession> getAllSessions() {
        return chatSessionRepository.findAll();
    }

    @GetMapping("/sessions/{id}")
    public List<ChatMessage> getMessagesBySession(@PathVariable Long id) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(id);
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        aiService.deleteSession(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/sessions/{id}/rename")
    public ResponseEntity<Void> renameSession(
            @PathVariable Long id,
            @RequestBody String newTitle) {
        aiService.renameSession(id, newTitle);
        return ResponseEntity.ok().build();
    }

    // ========== CHAT ENDPOINTS ==========

    @PostMapping("/chat/{sessionId}")
    public ResponseEntity<String> chat(@PathVariable Long sessionId,
                                        @RequestBody String prompt) {
        try {
            String response = aiService.getResponse(prompt, sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping(value = "/chat/stream/{sessionId}", produces = "text/event-stream")
    public SseEmitter streamResponse(@PathVariable Long sessionId,
                                     @RequestParam String prompt) {
        SseEmitter emitter = new SseEmitter(60000L);

        new Thread(() -> {
            try {
                String response = aiService.getResponse(prompt, sessionId);
                
                for (int i = 0; i < response.length(); i += 3) {
                    int end = Math.min(i + 3, response.length());
                    emitter.send(response.substring(i, end));
                    Thread.sleep(30);
                }
                emitter.complete();

            } catch (Exception e) {
                try {
                    emitter.send("Error: AI service temporarily unavailable");
                } catch (Exception ignored) {}
                emitter.complete();
            }
        }).start();

        return emitter;
    }

    // ========== SENTIMENT ENDPOINT ==========

    @PostMapping("/sentiment")
    public ResponseEntity<Map<String, String>> analyzeSentiment(@RequestBody String text) {
        try {
            String sentiment = sentimentService.analyzeSentiment(text);
            return ResponseEntity.ok(Map.of(
                "text", text,
                "sentiment", sentiment,
                "status", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "status", "failed"
            ));
        }
    }

    // ========== HEALTH ENDPOINT ==========

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "ai-backend",
            "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
}