package org.example.datnnhom03.assistant.action;

import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AssistantPendingActionService {

    private final Map<String, PendingAssistantAction> actionsBySession = new ConcurrentHashMap<>();

    public PendingAssistantAction create(String sessionCode,
                                         AssistantIntent intent,
                                         AssistantRole role,
                                         boolean executable,
                                         String previewMessage,
                                         Map<String, Object> payload,
                                         Map<String, Object> metadata) {
        PendingAssistantAction action = new PendingAssistantAction();
        action.setToken(UUID.randomUUID().toString().replace("-", ""));
        action.setSessionCode(sessionCode);
        action.setIntent(intent);
        action.setRole(role);
        action.setExecutable(executable);
        action.setPreviewMessage(previewMessage);
        action.setCreatedAt(Instant.now());
        action.setExpiresAt(Instant.now().plus(Duration.ofMinutes(20)));
        if (payload != null) action.getPayload().putAll(new LinkedHashMap<>(payload));
        if (metadata != null) {
            action.getMetadata().putAll(new LinkedHashMap<>(metadata));
            Object auditId = metadata.get("auditId");
            if (auditId != null) {
                action.setAuditId(String.valueOf(auditId));
            }
        }
        actionsBySession.put(sessionCode, action);
        return action;
    }

    public PendingAssistantAction peek(String sessionCode) {
        PendingAssistantAction action = actionsBySession.get(sessionCode);
        if (action != null && action.isExpired()) {
            actionsBySession.remove(sessionCode);
            return null;
        }
        return action;
    }

    public PendingAssistantAction consume(String sessionCode, String token) {
        PendingAssistantAction action = peek(sessionCode);
        if (action == null) return null;
        if (token != null && !token.isBlank() && !action.getToken().equals(token.trim())) {
            return null;
        }
        actionsBySession.remove(sessionCode);
        return action;
    }

    public PendingAssistantAction cancel(String sessionCode) {
        return actionsBySession.remove(sessionCode);
    }
}
