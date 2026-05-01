package org.example.datnnhom03.assistant.handler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AssistantHandlerResult {
    private boolean grounded;
    private String reply;
    private final List<Map<String, Object>> toolCalls = new ArrayList<>();
    private final Map<String, Object> payload = new LinkedHashMap<>();
    private final List<Map<String, Object>> suggestedActions = new ArrayList<>();
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    public static AssistantHandlerResult of(String reply, boolean grounded) {
        AssistantHandlerResult result = new AssistantHandlerResult();
        result.reply = reply;
        result.grounded = grounded;
        return result;
    }

    public boolean isGrounded() { return grounded; }
    public void setGrounded(boolean grounded) { this.grounded = grounded; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public List<Map<String, Object>> getToolCalls() { return toolCalls; }
    public Map<String, Object> getPayload() { return payload; }
    public List<Map<String, Object>> getSuggestedActions() { return suggestedActions; }
    public Map<String, Object> getMetadata() { return metadata; }
}
