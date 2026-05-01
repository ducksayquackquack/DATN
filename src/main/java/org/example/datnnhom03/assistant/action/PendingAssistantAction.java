package org.example.datnnhom03.assistant.action;

import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.policy.AssistantRole;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class PendingAssistantAction {
    private String token;
    private String sessionCode;
    private AssistantIntent intent;
    private AssistantRole role;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean executable;
    private String previewMessage;
    private String auditId;
    private final Map<String, Object> payload = new LinkedHashMap<>();
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getSessionCode() { return sessionCode; }
    public void setSessionCode(String sessionCode) { this.sessionCode = sessionCode; }
    public AssistantIntent getIntent() { return intent; }
    public void setIntent(AssistantIntent intent) { this.intent = intent; }
    public AssistantRole getRole() { return role; }
    public void setRole(AssistantRole role) { this.role = role; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isExecutable() { return executable; }
    public void setExecutable(boolean executable) { this.executable = executable; }
    public String getPreviewMessage() { return previewMessage; }
    public void setPreviewMessage(String previewMessage) { this.previewMessage = previewMessage; }
    public String getAuditId() { return auditId; }
    public void setAuditId(String auditId) { this.auditId = auditId; }
    public Map<String, Object> getPayload() { return payload; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void mergeSlotValues(Map<String, Object> values) {
        if (values == null || values.isEmpty()) return;
        this.payload.putAll(values);
    }
    public boolean isExpired() { return expiresAt != null && Instant.now().isAfter(expiresAt); }
}
