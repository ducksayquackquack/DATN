package org.example.datnnhom03.assistant.dto;

import java.util.HashMap;
import java.util.Map;

public class AssistantChatRequest {
    private String sessionCode;
    private String message;
    private String role;   // ADMIN | EMPLOYEE
    private String source; // ADMIN_PANEL | EMPLOYEE_PANEL
    private String pendingActionToken;
    private Map<String, Object> context = new HashMap<>();

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPendingActionToken() {
        return pendingActionToken;
    }

    public void setPendingActionToken(String pendingActionToken) {
        this.pendingActionToken = pendingActionToken;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context != null ? context : new HashMap<>();
    }
}