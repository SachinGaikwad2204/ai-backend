package com.sachin.ai_backend.controller;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.sachin.ai_backend.model.ChatMessage;
import com.sachin.ai_backend.model.ChatSession;
import com.sachin.ai_backend.repository.ChatMessageRepository;
import com.sachin.ai_backend.repository.ChatSessionRepository;
import com.sachin.ai_backend.service.AIService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {


    private final AIService aiService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public AIController(AIService aiService,
                        ChatSessionRepository chatSessionRepository,
                        ChatMessageRepository chatMessageRepository) {
        this.aiService = aiService;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    // CREATE SESSION
    @PostMapping("/sessions")
    public ChatSession createSession() {
        ChatSession session = new ChatSession();
        session.setTitle("New Chat");
        return chatSessionRepository.save(session);
    }

    // GET ALL SESSIONS
    @GetMapping("/sessions")
    public List<ChatSession> getAllSessions() {
        return chatSessionRepository.findAll();
    }

    // GET MESSAGES BY SESSION
    @GetMapping("/sessions/{id}")
    public List<ChatMessage> getMessagesBySession(@PathVariable Long id) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(id);
    }

    // SEND MESSAGE
    @PostMapping("/chat/{sessionId}")
    public String chat(@PathVariable Long sessionId,
                       @RequestBody String prompt) throws Exception {
        return aiService.getResponse(prompt, sessionId);
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


@GetMapping(value = "/chat/stream/{sessionId}", produces = "text/event-stream")
public SseEmitter streamResponse(@PathVariable Long sessionId,
                                 @RequestParam String prompt) {

    SseEmitter emitter = new SseEmitter(60000L); // 60s timeout

    new Thread(() -> {
        try {
            String response = aiService.getResponse(prompt, sessionId);

            for (char c : response.toCharArray()) {
                emitter.send(String.valueOf(c));
                Thread.sleep(20);
            }

            emitter.complete();

        } catch (Exception e) {
            try {
                emitter.send("Error: AI service unavailable");
            } catch (Exception ignored) {}
            emitter.complete();
        }
    }).start();

    return emitter;
}

}
