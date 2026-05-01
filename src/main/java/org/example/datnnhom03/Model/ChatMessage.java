package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChatMessage")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sessionId", nullable = false, referencedColumnName = "id")
    private ChatSession chatSession;

    @Column(name = "sessionId", nullable = false, insertable = false, updatable = false)
    private Integer sessionId;

    @Column(name = "senderType", nullable = false, length = 50)
    private String senderType; // CUSTOMER, EMPLOYEE, SYSTEM

    @Column(name = "senderName", nullable = false, length = 255)
    private String senderName;

    @Column(name = "senderId", length = 100)
    private String senderId;

    @Column(name = "content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "messageType", length = 50)
    private String messageType; // TEXT, IMAGE, FILE, SYSTEM, etc.

    @Column(name = "attachmentUrl", columnDefinition = "NVARCHAR(MAX)")
    private String attachmentUrl;

    @Column(name = "isread")
    private Boolean isread = false;

    @Column(name = "metadataJson", columnDefinition = "NVARCHAR(MAX)")
    private String metadataJson;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public ChatMessage() {}

    public ChatMessage(ChatSession chatSession, String senderType, String senderName, String content) {
        this.chatSession = chatSession;
        this.senderType = senderType;
        this.senderName = senderName;
        this.content = content;
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

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public Boolean getIsread() {
        return isread;
    }

    public void setIsread(Boolean isread) {
        this.isread = isread;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
