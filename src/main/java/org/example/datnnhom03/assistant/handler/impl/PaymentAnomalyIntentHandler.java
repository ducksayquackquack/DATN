package org.example.datnnhom03.assistant.handler.impl;

import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.handler.AssistantRequestContext;
import org.example.datnnhom03.assistant.handler.IntentHandler;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.service.ToolDispatcher;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PaymentAnomalyIntentHandler extends AbstractToolIntentHandler implements IntentHandler {

    public PaymentAnomalyIntentHandler(ToolDispatcher toolDispatcher) {
        super(toolDispatcher);
    }

    @Override
    public AssistantIntent supportedIntent() {
        return AssistantIntent.PAYMENT_ANOMALY;
    }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", true);
        Map<String, Object> paymentData = call(context, "payment_method", Map.of(), result);
        Map<String, Object> overdueData = call(context, "overdue_orders", currentMonthRange(10), result);

        result.setReply("Em đã rà nhanh dấu hiệu bất thường thanh toán: ưu tiên kiểm tra đơn COD treo lâu, đơn chuyển khoản chưa chốt paidAt và nhóm phương thức có tỷ lệ pending cao. Em đã đính kèm dữ liệu gợi ý để admin soi tiếp.");
        result.getPayload().put("payment", paymentData);
        result.getPayload().put("overdue", overdueData);
        result.getSuggestedActions().add(Map.of("type", "open_payment_dashboard", "label", "Mở dashboard thanh toán", "priority", "high"));
        result.getSuggestedActions().add(Map.of("type", "filter_cod_anomaly", "label", "Lọc COD bất thường", "priority", "medium"));
        return result;
    }
}
