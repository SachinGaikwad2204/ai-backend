package com.sachin.ai_backend.repository;

import com.sachin.ai_backend.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
}
