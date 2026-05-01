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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CreateOrderIntentHandler extends AbstractToolIntentHandler implements IntentHandler {

    private static final Pattern EXPLICIT_SIZE_PATTERN = Pattern.compile("(?:size|cỡ|co)\\s*[:=]?\\s*(xx?l|3xl|4xl|5xl|xs|s|m|l|xl)\\b", Pattern.CASE_INSENSITIVE);

    private final AssistantPendingActionService pendingActionService;
    private final AssistantActionSafetyService safetyService;
    private final AssistantActionAuditService auditService;

    public CreateOrderIntentHandler(org.example.datnnhom03.assistant.service.ToolDispatcher toolDispatcher,
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
        return AssistantIntent.CREATE_ORDER;
    }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerName", stringValue(context.getContext().get("customerName")));
        payload.put("customerPhone", stringValue(context.getContext().get("customerPhone")));
        payload.put("shippingAddress", stringValue(context.getContext().get("shippingAddress")));
        payload.put("productHint", stringValue(context.getContext().get("productHint")));
        String explicitSize = extractExplicitSize(context.getMessage());
        payload.put("size", explicitSize.isBlank() ? stringValue(context.getContext().get("size")) : explicitSize);
        payload.put("quantity", intValue(context.getContext().get("quantity")));
        payload.put("salesChannel", context.getPageType());
        payload.put("operatorRole", context.getRole().name());

        AssistantActionSafetyService.SafetyCheckResult safety = safetyService.validateForPreview(supportedIntent(), payload);
        String preview = "Em đã chuẩn bị đơn nháp cho khách "
                + (stringValue(payload.get("customerName")).isBlank() ? payload.get("customerPhone") : payload.get("customerName") + " (" + payload.get("customerPhone") + ")")
                + ": " + payload.get("productHint")
                + (stringValue(payload.get("size")).isBlank() ? "" : " size " + payload.get("size"))
                + " x" + payload.get("quantity")
                + ", giao tới " + payload.get("shippingAddress")
                + ". Đơn này đang ở chế độ an toàn và nên chuyển qua workflow tạo đơn chính thức để tránh lệch tồn/phí ship.";

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("previewType", "order_create");
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
        result.getSuggestedActions().add(Map.of("type", "open_order_workflow", "label", "Mở workflow tạo đơn", "priority", "high"));
        result.getSuggestedActions().add(Map.of("type", "cancel_draft", "label", "Hủy nháp", "priority", "medium"));
        return result;
    }

    private String extractExplicitSize(String message) {
        Matcher matcher = EXPLICIT_SIZE_PATTERN.matcher(message == null ? "" : message);
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : "";
    }

    private int intValue(Object value) {
        try {
            return Integer.parseInt(stringValue(value));
        } catch (Exception ex) {
            return 0;
        }
    }

    protected String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
