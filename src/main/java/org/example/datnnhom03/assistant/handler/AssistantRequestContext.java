package org.example.datnnhom03.assistant.handler;

import org.example.datnnhom03.assistant.dto.AssistantChatRequest;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.example.datnnhom03.assistant.state.AssistantConversationState;

import java.util.LinkedHashMap;
import java.util.Map;

public class AssistantRequestContext {
    private final AssistantChatRequest request;
    private final AssistantRole role;
    private final AssistantIntent intent;
    private final String pageType;
    private final String normalizedMessage;
    private final AssistantConversationState state;
    private final Map<String, Object> extractedEntities = new LinkedHashMap<>();
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    public AssistantRequestContext(AssistantChatRequest request,
                                   AssistantRole role,
                                   AssistantIntent intent,
                                   String pageType,
                                   String normalizedMessage,
                                   AssistantConversationState state) {
        this.request = request;
        this.role = role;
        this.intent = intent;
        this.pageType = pageType;
        this.normalizedMessage = normalizedMessage;
        this.state = state;
    }

    public AssistantChatRequest getRequest() { return request; }
    public AssistantRole getRole() { return role; }
    public AssistantIntent getIntent() { return intent; }
    public String getPageType() { return pageType; }
    public String getNormalizedMessage() { return normalizedMessage; }
    public AssistantConversationState getState() { return state; }
    public Map<String, Object> getExtractedEntities() { return extractedEntities; }
    public Map<String, Object> getMetadata() { return metadata; }

    public Map<String, Object> getContext() {
        return request.getContext() == null ? Map.of() : request.getContext();
    }

    public String getMessage() {
        return request.getMessage() == null ? "" : request.getMessage().trim();
    }

    public String getSessionCode() {
        if (request.getSessionCode() != null && !request.getSessionCode().isBlank()) {
            return request.getSessionCode().trim();
        }
        return "AI-LOCAL";
    }
}
