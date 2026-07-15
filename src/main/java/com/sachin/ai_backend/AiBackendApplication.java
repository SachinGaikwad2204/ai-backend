package com.sachin.ai_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class AiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiBackendApplication.class, args);
        System.out.println("🚀 AI Backend started successfully!");
        System.out.println("📊 Features: Multi-LLM, RAG, Sentiment Analysis");
        System.out.println("🔗 H2 Console: http://localhost:8080/h2-console");
    }
}