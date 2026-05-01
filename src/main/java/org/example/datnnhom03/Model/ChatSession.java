package org.example.datnnhom03.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ChatSession")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sessionCode", nullable = false, unique = true, length = 100)
    private String sessionCode;

    @Column(name = "customerName", length = 255)
    private String customerName;

    @Column(name = "customerEmail", length = 100)
    private String customerEmail;

    @Column(name = "customerPhone", length = 20)
    private String customerPhone;

    @ManyToOne
    @JoinColumn(name = "assignedEmployeeId", referencedColumnName = "id")
    private NhanVien assignedEmployee;

    @Column(name = "assignedEmployeeId", insertable = false, updatable = false)
    private Integer assignedEmployeeId;

    @Column(name = "assignedEmployeeName", length = 255)
    private String assignedEmployeeName;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // Active, Closed, Waiting, etc.

    @Column(name = "chatMode", length = 50)
    private String chatMode; // AUTO (AI), HUMAN, TRANSFER, etc.

    @Column(name = "acceptedAt")
    private LocalDateTime acceptedAt;

    @Column(name = "lastMessageAt")
    private LocalDateTime lastMessageAt;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages;

    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatSessionEvent> events;

    // Constructors
    public ChatSession() {}

    public ChatSession(String sessionCode, String customerEmail) {
        this.sessionCode = sessionCode;
        this.customerEmail = customerEmail;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public NhanVien getAssignedEmployee() {
        return assignedEmployee;
    }

    public void setAssignedEmployee(NhanVien assignedEmployee) {
        this.assignedEmployee = assignedEmployee;
    }

    public Integer getAssignedEmployeeId() {
        return assignedEmployeeId;
    }

    public void setAssignedEmployeeId(Integer assignedEmployeeId) {
        this.assignedEmployeeId = assignedEmployeeId;
    }

    public String getAssignedEmployeeName() {
        return assignedEmployeeName;
    }

    public void setAssignedEmployeeName(String assignedEmployeeName) {
        this.assignedEmployeeName = assignedEmployeeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChatMode() {
        return chatMode;
    }

    public void setChatMode(String chatMode) {
        this.chatMode = chatMode;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public List<ChatSessionEvent> getEvents() {
        return events;
    }

    public void setEvents(List<ChatSessionEvent> events) {
        this.events = events;
    }
}
