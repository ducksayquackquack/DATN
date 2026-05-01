package org.example.datnnhom03.assistant.audit;

import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.policy.AssistantRole;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class AssistantActionAuditRecord {
    private String auditId;
    private String sessionCode;
    private AssistantIntent intent;
    private AssistantRole role;
    private String stage;
    private String status;
    private String message;
    private Instant createdAt;
    private final Map<String, Object> payload = new LinkedHashMap<>();
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    public String getAuditId() { return auditId; }
    public void setAuditId(String auditId) { this.auditId = auditId; }
    public String getSessionCode() { return sessionCode; }
    public void setSessionCode(String sessionCode) { this.sessionCode = sessionCode; }
    public AssistantIntent getIntent() { return intent; }
    public void setIntent(AssistantIntent intent) { this.intent = intent; }
    public AssistantRole getRole() { return role; }
    public void setRole(AssistantRole role) { this.role = role; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Map<String, Object> getPayload() { return payload; }
    public Map<String, Object> getMetadata() { return metadata; }
}
