package org.example.datnnhom03.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AssistantChatResponse {
    private boolean success;
    private String sessionCode;
    private String role;
    private String message;
    private String error;
    private List<String> quickReplies = new ArrayList<>();
    private List<Map<String, Object>> toolCalls = new ArrayList<>();

    public static AssistantChatResponse ok(String sessionCode, String role, String message) {
        AssistantChatResponse r = new AssistantChatResponse();
        r.setSuccess(true);
        r.setSessionCode(sessionCode);
        r.setRole(role);
        r.setMessage(message);
        return r;
    }

    public static AssistantChatResponse fail(String error) {
        AssistantChatResponse r = new AssistantChatResponse();
        r.setSuccess(false);
        r.setError(error);
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<String> getQuickReplies() {
        return quickReplies;
    }

    public void setQuickReplies(List<String> quickReplies) {
        this.quickReplies = quickReplies;
    }

    public List<Map<String, Object>> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<Map<String, Object>> toolCalls) {
        this.toolCalls = toolCalls;
    }
}