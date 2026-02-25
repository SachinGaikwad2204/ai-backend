package com.sachin.ai_backend.dto;

public class ChatResponse {

    private String response;
    private Long sessionId;

    public ChatResponse(String response, Long sessionId) {
        this.response = response;
        this.sessionId = sessionId;
    }

    public String getResponse() {
        return response;
    }

    public Long getSessionId() {
        return sessionId;
    }
}
