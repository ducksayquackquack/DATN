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
public class ProductUpsertIntentHandler extends AbstractToolIntentHandler implements IntentHandler {

    private final AssistantPendingActionService pendingActionService;
    private final AssistantActionSafetyService safetyService;
    private final AssistantActionAuditService auditService;

    public ProductUpsertIntentHandler(org.example.datnnhom03.assistant.service.ToolDispatcher toolDispatcher,
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
        return AssistantIntent.PRODUCT_UPSERT;
    }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> reqCtx = context.getContext();
        payload.put("productId", stringValue(reqCtx.get("productId")));
        payload.put("maSanPham", stringValue(reqCtx.get("productCode")));
        payload.put("tenSanPham", !stringValue(reqCtx.get("productName")).isBlank() ? stringValue(reqCtx.get("productName")) : extractName(context.getMessage()));
        payload.put("moTa", stringValue(reqCtx.get("productDescription")));
        payload.put("trangThai", !stringValue(reqCtx.get("productStatus")).isBlank() ? stringValue(reqCtx.get("productStatus")) : "Hoạt động");
        payload.put("nguoiSua", !stringValue(reqCtx.get("actorEmail")).isBlank() ? stringValue(reqCtx.get("actorEmail")) : "assistant");
        payload.put("nguoiTao", payload.get("nguoiSua"));

        boolean update = !stringValue(payload.get("productId")).isBlank();
        AssistantActionSafetyService.SafetyCheckResult safety = safetyService.validateForPreview(supportedIntent(), payload);
        String preview = (update ? "Em đã chuẩn bị cập nhật" : "Em đã chuẩn bị tạo")
                + " sản phẩm: " + stringValue(payload.get("tenSanPham"))
                + (stringValue(payload.get("maSanPham")).isBlank() ? "" : " (" + payload.get("maSanPham") + ")")
                + ". Anh/chị xác nhận để em thực hiện.";

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("previewType", update ? "product_update" : "product_create");
        metadata.put("safety", safety.toMap());
        String auditId = auditService.recordDraft(context.getSessionCode(), supportedIntent(), context.getRole(), preview, payload, metadata);
        metadata.put("auditId", auditId);
        PendingAssistantAction pending = pendingActionService.create(
                context.getSessionCode(), supportedIntent(), context.getRole(), true, preview, payload, metadata);

        AssistantHandlerResult result = AssistantHandlerResult.of(preview, true);
        result.getMetadata().put("pendingActionToken", pending.getToken());
        result.getMetadata().put("actionStatus", "PENDING_CONFIRMATION");
        result.getMetadata().put("auditId", auditId);
        result.getMetadata().put("warnings", safety.getWarnings());
        result.getPayload().putAll(payload);
        result.getPayload().put("safety", safety.toMap());
        result.getSuggestedActions().add(Map.of("type", "confirm_action", "label", update ? "Xác nhận cập nhật" : "Xác nhận tạo sản phẩm", "priority", "high", "token", pending.getToken()));
        result.getSuggestedActions().add(Map.of("type", "cancel_action", "label", "Hủy nháp", "priority", "medium"));
        return result;
    }

    private String extractName(String message) {
        String cleaned = message == null ? "" : message.replaceAll("(?i)(tạo|tao|cap nhat|cập nhật|sản phẩm|san pham|mã|ma)\\s*", " ").trim();
        return cleaned.isBlank() ? "Sản phẩm mới từ assistant" : cleaned;
    }

    protected String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
