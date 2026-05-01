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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CreateVoucherIntentHandler extends AbstractToolIntentHandler implements IntentHandler {

    private final AssistantPendingActionService pendingActionService;
    private final AssistantActionSafetyService safetyService;
    private final AssistantActionAuditService auditService;

    public CreateVoucherIntentHandler(org.example.datnnhom03.assistant.service.ToolDispatcher toolDispatcher,
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
        return AssistantIntent.CREATE_VOUCHER;
    }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        String normalized = context.getNormalizedMessage();
        BigDecimal value = extractDiscountValue(normalized);
        String unit = containsAny(normalized, "%", "phan tram", "phần trăm") ? "PERCENT" : "VND";
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            value = BigDecimal.TEN;
            unit = "PERCENT";
        }
        String name = extractVoucherName(context.getMessage(), value, unit);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenKhuyenMai", name);
        payload.put("giaTri", value);
        payload.put("donViGiam", unit);
        payload.put("ngayBatDau", LocalDateTime.now());
        payload.put("ngayKetThuc", LocalDateTime.now().plusDays(7));
        payload.put("trangThai", "Hoạt động");

        AssistantActionSafetyService.SafetyCheckResult safety = safetyService.validateForPreview(supportedIntent(), payload);
        String preview = "Em đã chuẩn bị voucher nháp: " + name + " - giảm " + value.stripTrailingZeros().toPlainString()
                + ("PERCENT".equals(unit) ? "%" : "đ") + " trong 7 ngày tới. Anh/chị xác nhận để em tạo luôn.";
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("previewType", "voucher_create");
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
        result.getSuggestedActions().add(Map.of("type", "confirm_action", "label", "Xác nhận tạo voucher", "priority", "high", "token", pending.getToken()));
        result.getSuggestedActions().add(Map.of("type", "cancel_action", "label", "Hủy nháp", "priority", "medium"));
        return result;
    }

    private BigDecimal extractDiscountValue(String normalized) {
        Matcher matcher = Pattern.compile("(\\d{1,3})(\\s*%|\\s*phan tram|\\s*phần trăm|\\s*k|\\s*000|\\s*đ|\\s*vnd)?", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (matcher.find()) {
            String raw = matcher.group(1);
            String unit = matcher.group(2) == null ? "" : matcher.group(2).toLowerCase();
            BigDecimal value = new BigDecimal(raw);
            if (unit.contains("k")) return value.multiply(BigDecimal.valueOf(1000));
            if (unit.contains("000")) return value.multiply(BigDecimal.valueOf(1000));
            return value;
        }
        return BigDecimal.ZERO;
    }

    private String extractVoucherName(String message, BigDecimal value, String unit) {
        Matcher named = Pattern.compile("(?:tạo|tao|them)\\s+voucher\\s+(.+)", Pattern.CASE_INSENSITIVE).matcher(message);
        if (named.find()) {
            String tail = named.group(1).trim();
            if (!tail.isBlank()) return tail;
        }
        return "Voucher trợ lý " + value.stripTrailingZeros().toPlainString() + ("PERCENT".equals(unit) ? "%" : "đ");
    }
}
