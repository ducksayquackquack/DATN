package org.example.datnnhom03.assistant.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AssistantChatResponse {
    private boolean success;
    private String sessionCode;
    private String role;
    private String message;
    private String error;
    private String intent;
    private String confidence;
    private boolean grounded;
    private boolean handoffSuggested;
    private boolean requiresConfirmation;
    private String confirmationMessage;
    private String pendingActionToken;
    private String actionStatus;
    private String auditId;
    private List<String> warnings = new ArrayList<>();
    private List<String> missingSlots = new ArrayList<>();
    private List<String> quickReplies = new ArrayList<>();
    private List<Map<String, Object>> toolCalls = new ArrayList<>();
    private List<Map<String, Object>> suggestedActions = new ArrayList<>();
    private Map<String, Object> debug = new LinkedHashMap<>();
    private Map<String, Object> data = new LinkedHashMap<>();

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

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getSessionCode() { return sessionCode; }
    public void setSessionCode(String sessionCode) { this.sessionCode = sessionCode; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
    public boolean isGrounded() { return grounded; }
    public void setGrounded(boolean grounded) { this.grounded = grounded; }
    public boolean isHandoffSuggested() { return handoffSuggested; }
    public void setHandoffSuggested(boolean handoffSuggested) { this.handoffSuggested = handoffSuggested; }
    public boolean isRequiresConfirmation() { return requiresConfirmation; }
    public void setRequiresConfirmation(boolean requiresConfirmation) { this.requiresConfirmation = requiresConfirmation; }
    public String getConfirmationMessage() { return confirmationMessage; }
    public void setConfirmationMessage(String confirmationMessage) { this.confirmationMessage = confirmationMessage; }
    public String getPendingActionToken() { return pendingActionToken; }
    public void setPendingActionToken(String pendingActionToken) { this.pendingActionToken = pendingActionToken; }
    public String getActionStatus() { return actionStatus; }
    public void setActionStatus(String actionStatus) { this.actionStatus = actionStatus; }
    public String getAuditId() { return auditId; }
    public void setAuditId(String auditId) { this.auditId = auditId; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public List<String> getMissingSlots() { return missingSlots; }
    public void setMissingSlots(List<String> missingSlots) { this.missingSlots = missingSlots; }
    public List<String> getQuickReplies() { return quickReplies; }
    public void setQuickReplies(List<String> quickReplies) { this.quickReplies = quickReplies; }
    public List<Map<String, Object>> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<Map<String, Object>> toolCalls) { this.toolCalls = toolCalls; }
    public List<Map<String, Object>> getSuggestedActions() { return suggestedActions; }
    public void setSuggestedActions(List<Map<String, Object>> suggestedActions) { this.suggestedActions = suggestedActions; }
    public Map<String, Object> getDebug() { return debug; }
    public void setDebug(Map<String, Object> debug) { this.debug = debug; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}
