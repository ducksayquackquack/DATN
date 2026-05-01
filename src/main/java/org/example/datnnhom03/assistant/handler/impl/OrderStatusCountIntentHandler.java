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
public class OrderStatusCountIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public OrderStatusCountIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }

    @Override public AssistantIntent supportedIntent() { return AssistantIntent.ORDER_STATUS_COUNT; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        Map<String, Object> data = call(context, "get_order_status_summary", new LinkedHashMap<>(), result);
        result.getPayload().put("orderStatusSummary", data);
        result.setGrounded(true);

        String message = normalize(context.getMessage());
        int count;
        String label;
        if (containsAny(message, "cho xu ly", "chờ xử lý")) {
            count = intValue(data.get("processingCount"));
            label = "đơn chờ xử lý";
        } else if (containsAny(message, "hoàn thành", "hoan thanh", "đã giao", "da giao", "thành công", "thanh cong")) {
            count = intValue(data.get("completedCount"));
            label = "đơn hoàn thành";
        } else if (containsAny(message, "đã hủy", "da huy", "hủy")) {
            count = intValue(data.get("cancelledCount"));
            label = "đơn đã hủy";
        } else {
            count = intValue(data.get("processingCount")) + intValue(data.get("completedCount")) + intValue(data.get("cancelledCount"));
            label = "đơn theo trạng thái chính";
        }
        result.setReply("Hiện có " + count + " " + label + ".");
        return result;
    }

    private int intValue(Object value) {
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception ex) { return 0; }
    }
}
