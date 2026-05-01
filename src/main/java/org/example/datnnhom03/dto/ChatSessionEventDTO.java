package org.example.datnnhom03.dto;

import java.time.LocalDateTime;

public class ChatSessionEventDTO {
    private Long id;
    private Integer sessionId;
    private String eventType; // REQUEST_HUMAN, ACCEPTED, CLOSED, TRANSFERRED
    private String actorType; // CUSTOMER, EMPLOYEE, SYSTEM
    private String actorId;
    private String actorName;
    private String payloadJson;
    private LocalDateTime createdAt;

    // Constructors
    public ChatSessionEventDTO() {}

    public ChatSessionEventDTO(Integer sessionId, String eventType, String actorType) {
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.actorType = actorType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
