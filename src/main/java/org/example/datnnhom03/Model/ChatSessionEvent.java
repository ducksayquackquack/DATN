package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "ChatSessionEvent")
public class ChatSessionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sessionId", nullable = false, referencedColumnName = "id")
    private ChatSession chatSession;

    @Column(name = "sessionId", nullable = false, insertable = false, updatable = false)
    private Integer sessionId;

    @Column(name = "eventType", nullable = false, length = 50)
    private String eventType; // REQUEST_HUMAN, ACCEPTED, CLOSED, TRANSFERRED, etc.

    @Column(name = "actorType", length = 20)
    private String actorType; // CUSTOMER, EMPLOYEE, SYSTEM

    @Column(name = "actorId", length = 100)
    private String actorId;

    @Column(name = "actorName", length = 255)
    private String actorName;

    @Column(name = "payloadJson", columnDefinition = "NVARCHAR(MAX)")
    private String payloadJson; // Additional event data as JSON

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public ChatSessionEvent() {}

    public ChatSessionEvent(ChatSession chatSession, String eventType, String actorType) {
        this.chatSession = chatSession;
        this.eventType = eventType;
        this.actorType = actorType;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChatSession getChatSession() {
        return chatSession;
    }

    public void setChatSession(ChatSession chatSession) {
        this.chatSession = chatSession;
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
