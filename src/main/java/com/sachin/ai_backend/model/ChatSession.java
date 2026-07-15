package com.sachin.ai_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  // ← ADD THIS TO BREAK CIRCULAR REFERENCE
    private List<ChatMessage> messages;

    public ChatSession() {}

    public ChatSession(String title) {
        this.title = title;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<ChatMessage> getMessages() { return messages; }

    public void setTitle(String title) {
        this.title = title;
    }
}