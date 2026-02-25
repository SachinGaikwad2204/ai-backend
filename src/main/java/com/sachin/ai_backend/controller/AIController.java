package com.sachin.ai_backend.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.beans.factory.annotation.Autowired;
import com.sachin.ai_backend.model.ChatMessage;
import com.sachin.ai_backend.repository.ChatMessageRepository;
import com.sachin.ai_backend.model.ChatSession;
import com.sachin.ai_backend.repository.ChatSessionRepository;
import com.sachin.ai_backend.service.AIService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    private final AIService aiService;
    private final ChatSessionRepository chatSessionRepository;

    public AIController(AIService aiService,
                        ChatSessionRepository chatSessionRepository) {
        this.aiService = aiService;
        this.chatSessionRepository = chatSessionRepository;
    }

    @PostMapping("/chat/{sessionId}")
	public String chat(
    @PathVariable Long sessionId,
    @RequestBody String prompt
       ) throws Exception {
      return aiService.getResponse(prompt, sessionId);
     }


    @GetMapping("/sessions")
    public List<ChatSession> getSessions() {
        return chatSessionRepository.findAll();
    }

    @PostMapping("/chat/stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamChat(
        @RequestParam(required = false) Long sessionId,
        @RequestBody String prompt
    ) {
    return aiService.streamResponse(prompt, sessionId);
}

@PostMapping("/sessions")
public ChatSession createSession() {
    ChatSession session = new ChatSession();
    session.setTitle("New Chat");
    return chatSessionRepository.save(session);
}



@GetMapping("/sessions/{id}")
public List<ChatMessage> getMessagesBySession(@PathVariable Long id) {
    return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(id);
}

@DeleteMapping("/sessions/{id}")
public void deleteSession(@PathVariable Long id) {
    chatSessionRepository.deleteById(id);
}



@PutMapping("/sessions/{id}")
public ChatSession updateTitle(
        @PathVariable Long id,
        @RequestBody ChatSession updatedSession
) {
    ChatSession session = chatSessionRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("Session not found"));

    session.setTitle(updatedSession.getTitle());

    return chatSessionRepository.save(session);
}
}
