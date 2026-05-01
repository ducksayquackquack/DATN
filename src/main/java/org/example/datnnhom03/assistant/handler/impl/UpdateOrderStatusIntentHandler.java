package org.example.datnnhom03.assistant.handler.impl;

import org.example.datnnhom03.assistant.action.AssistantPendingActionService;
import org.example.datnnhom03.assistant.action.PendingAssistantAction;
import org.example.datnnhom03.assistant.audit.AssistantActionAuditService;
import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.handler.AssistantRequestContext;
import org.example.datnnhom03.assistant.handler.IntentHandler;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.service.AssistantActionSafetyService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class UpdateOrderStatusIntentHandler extends AbstractToolIntentHandler implements IntentHandler {

    private final AssistantPendingActionService pendingActionService;
    private final AssistantActionSafetyService safetyService;
    private final AssistantActionAuditService auditService;

    public UpdateOrderStatusIntentHandler(org.example.datnnhom03.assistant.service.ToolDispatcher toolDispatcher,
                                          AssistantPendingActionService pendingActionService,
                                          AssistantActionSafetyService safetyService,
                                          AssistantActionAuditService auditService) {
        super(toolDispatcher);
        this.pendingActionService = pendingActionService;
        this.safetyService = safetyService;
        this.auditService = auditService;
    }

    @Override
    public AssistantIntent supportedIntent() {
        return AssistantIntent.UPDATE_ORDER_STATUS;
    }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderCode", stringValue(context.getContext().get("orderCode")));
        payload.put("targetStatus", stringValue(context.getContext().get("targetStatus")));

        AssistantActionSafetyService.SafetyCheckResult safety = safetyService.validateForPreview(supportedIntent(), payload);
        String preview = "Em đã chuẩn bị yêu cầu đổi trạng thái đơn " + payload.get("orderCode") + " sang " + payload.get("targetStatus")
                + ". Luồng này hiện vẫn để ở chế độ handoff an toàn để tránh cập nhật sai trạng thái.";

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("previewType", "order_status_update");
        metadata.put("safety", safety.toMap());
        String auditId = auditService.recordDraft(context.getSessionCode(), supportedIntent(), context.getRole(), preview, payload, metadata);
        metadata.put("auditId", auditId);

        PendingAssistantAction pending = pendingActionService.create(
                context.getSessionCode(), supportedIntent(), context.getRole(), false, preview, payload, metadata);

        AssistantHandlerResult result = AssistantHandlerResult.of(preview, true);
        result.getMetadata().put("pendingActionToken", pending.getToken());
        result.getMetadata().put("actionStatus", "HANDOFF_REQUIRED");
        result.getMetadata().put("auditId", auditId);
        result.getMetadata().put("warnings", safety.getWarnings());
        result.getPayload().putAll(payload);
        result.getPayload().put("safety", safety.toMap());
        result.getSuggestedActions().add(Map.of("type", "handoff", "label", "Mở workflow đổi trạng thái", "priority", "high"));
        result.getSuggestedActions().add(Map.of("type", "cancel_action", "label", "Hủy nháp", "priority", "medium"));
        return result;
    }

    protected String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
