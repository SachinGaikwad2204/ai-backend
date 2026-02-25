package com.sachin.ai_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String role;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "chat_id")
    private ChatSession session;

    public ChatMessage() {}

    public ChatMessage(String role, String content, ChatSession session) {
        this.role = role;
        this.content = content;
        this.session = session;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public ChatSession getSession() { return session; }
}
