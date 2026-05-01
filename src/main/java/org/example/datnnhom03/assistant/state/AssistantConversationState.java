package org.example.datnnhom03.assistant.state;

import org.example.datnnhom03.assistant.model.AssistantIntent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class AssistantConversationState {
    private String sessionCode;
    private String pageType;
    private AssistantIntent lastIntent;
    private Instant updatedAt;
    private String pendingActionToken;
    private final Map<String, Object> slots = new LinkedHashMap<>();

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public AssistantIntent getLastIntent() {
        return lastIntent;
    }

    public void setLastIntent(AssistantIntent lastIntent) {
        this.lastIntent = lastIntent;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPendingActionToken() {
        return pendingActionToken;
    }

    public void setPendingActionToken(String pendingActionToken) {
        this.pendingActionToken = pendingActionToken;
    }

    public Map<String, Object> getSlots() {
        return slots;
    }
}
