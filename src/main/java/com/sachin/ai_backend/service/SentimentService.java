package com.sachin.ai_backend.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SentimentService {

    private static final Logger log = LoggerFactory.getLogger(SentimentService.class);
    private final ChatClient chatClient;

    public SentimentService(@Qualifier("openAiChatModel") OpenAiChatModel openAiChatModel) {
        this.chatClient = ChatClient.create(openAiChatModel);
        log.info("✅ Sentiment Service initialized with Groq");
    }

    public String analyzeSentiment(String text) {
        try {
            String response = chatClient.prompt()
                .system("""
                    You are a sentiment analyzer. Analyze the sentiment of the user's message.
                    Respond with only one word: POSITIVE, NEGATIVE, or NEUTRAL.
                    Do not add any explanation or additional text.
                    """)
                .user(text)
                .call()
                .content();
            
            log.debug("Sentiment: {} for text: {}", response, text);
            return response.trim().toUpperCase();
            
        } catch (Exception e) {
            log.error("Sentiment analysis failed: {}", e.getMessage());
            return "UNKNOWN";
        }
    }
}