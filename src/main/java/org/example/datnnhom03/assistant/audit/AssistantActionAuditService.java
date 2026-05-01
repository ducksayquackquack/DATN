package org.example.datnnhom03.assistant.audit;

import org.example.datnnhom03.assistant.action.PendingAssistantAction;
import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AssistantActionAuditService {

    private final Map<String, AssistantActionAuditRecord> auditsById = new ConcurrentHashMap<>();
    private final Map<String, List<String>> auditIdsBySession = new ConcurrentHashMap<>();

    public String recordDraft(String sessionCode,
                              AssistantIntent intent,
                              AssistantRole role,
                              String message,
                              Map<String, Object> payload,
                              Map<String, Object> metadata) {
        return save(sessionCode, intent, role, "DRAFT", "PENDING_CONFIRMATION", message, payload, metadata);
    }

    public String recordExecution(PendingAssistantAction action, AssistantHandlerResult result) {
        if (action == null) {
            return save("UNKNOWN", null, null, "EXECUTION", "NO_PENDING_ACTION", "Không có thao tác chờ để thực thi.", Map.of(), Map.of());
        }
        return save(action.getSessionCode(), action.getIntent(), action.getRole(), "EXECUTION",
                stringValue(result == null ? null : result.getMetadata().get("actionStatus"), "EXECUTED"),
                result == null ? action.getPreviewMessage() : result.getReply(),
                action.getPayload(),
                result == null ? action.getMetadata() : merge(action.getMetadata(), result.getMetadata()));
    }

    public String recordCancellation(PendingAssistantAction action, String message) {
        if (action == null) {
            return save("UNKNOWN", null, null, "CANCEL", "NO_PENDING_ACTION", message, Map.of(), Map.of());
        }
        return save(action.getSessionCode(), action.getIntent(), action.getRole(), "CANCEL", "CANCELED", message, action.getPayload(), action.getMetadata());
    }

    public AssistantActionAuditRecord get(String auditId) {
        return auditId == null ? null : auditsById.get(auditId);
    }

    public List<AssistantActionAuditRecord> recentBySession(String sessionCode) {
        List<String> ids = auditIdsBySession.getOrDefault(sessionCode, List.of());
        List<AssistantActionAuditRecord> records = new ArrayList<>();
        for (int i = ids.size() - 1; i >= 0; i--) {
            AssistantActionAuditRecord record = auditsById.get(ids.get(i));
            if (record != null) {
                records.add(record);
            }
            if (records.size() >= 10) break;
        }
        return records;
    }

    private String save(String sessionCode,
                        AssistantIntent intent,
                        AssistantRole role,
                        String stage,
                        String status,
                        String message,
                        Map<String, Object> payload,
                        Map<String, Object> metadata) {
        AssistantActionAuditRecord record = new AssistantActionAuditRecord();
        record.setAuditId(UUID.randomUUID().toString().replace("-", ""));
        record.setSessionCode(stringValue(sessionCode, "AI-LOCAL"));
        record.setIntent(intent);
        record.setRole(role);
        record.setStage(stage);
        record.setStatus(status);
        record.setMessage(stringValue(message, ""));
        record.setCreatedAt(Instant.now());
        if (payload != null) record.getPayload().putAll(new LinkedHashMap<>(payload));
        if (metadata != null) record.getMetadata().putAll(new LinkedHashMap<>(metadata));
        auditsById.put(record.getAuditId(), record);
        auditIdsBySession.computeIfAbsent(record.getSessionCode(), key -> new ArrayList<>()).add(record.getAuditId());
        return record.getAuditId();
    }

    private Map<String, Object> merge(Map<String, Object> left, Map<String, Object> right) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (left != null) merged.putAll(left);
        if (right != null) merged.putAll(right);
        return merged;
    }

    private String stringValue(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }
}
