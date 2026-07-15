package com.sachin.ai_backend.service;

import com.sachin.ai_backend.model.ChatMessage;
import com.sachin.ai_backend.model.ChatSession;
import com.sachin.ai_backend.repository.ChatMessageRepository;
import com.sachin.ai_backend.repository.ChatSessionRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    private final ChatClient chatClient;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final SentimentService sentimentService;

    public AIService(
            @Qualifier("openAiChatModel") OpenAiChatModel openAiChatModel,
            ChatSessionRepository sessionRepository,
            ChatMessageRepository messageRepository,
            SentimentService sentimentService) {
        
        this.chatClient = ChatClient.create(openAiChatModel);
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.sentimentService = sentimentService;
        log.info("✅ AI Service initialized with Groq");
    }

    public String getResponse(String prompt, Long sessionId) throws Exception {
        log.info("🤖 Generating response using Groq");
        
        try {
            String sentiment = sentimentService.analyzeSentiment(prompt);
            log.info("📊 User sentiment: {}", sentiment);
            
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            saveMessage(sessionId, "user", prompt);
            saveMessage(sessionId, "assistant", response);
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ Groq failed: {}", e.getMessage());
            return "I'm sorry, the AI service is currently unavailable. Please try again later.";
        }
    }

    @Transactional
    public void saveMessage(Long sessionId, String role, String content) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        ChatMessage message = new ChatMessage(role, content, session);
        messageRepository.save(message);
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        sessionRepository.deleteById(sessionId);
    }

    @Transactional
    public void renameSession(Long sessionId, String newTitle) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setTitle(newTitle);
        sessionRepository.save(session);
    }
}