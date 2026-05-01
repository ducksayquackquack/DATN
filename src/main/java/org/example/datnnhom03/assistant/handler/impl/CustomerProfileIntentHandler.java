package org.example.datnnhom03.assistant.handler.impl;

import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.handler.AssistantRequestContext;
import org.example.datnnhom03.assistant.handler.IntentHandler;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.service.ToolDispatcher;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CustomerProfileIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public CustomerProfileIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.CUSTOMER_PROFILE; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        Map<String, Object> args = new LinkedHashMap<>();
        String explicitIdentity = !stringValue(context.getContext().get("customerPhone")).isBlank()
                ? stringValue(context.getContext().get("customerPhone"))
                : !stringValue(context.getContext().get("customerName")).isBlank()
                    ? stringValue(context.getContext().get("customerName"))
                    : context.getMessage();
        args.put("keyword", explicitIdentity);
        args.put("sessionCode", context.getSessionCode());
        args.put("customerPhone", context.getContext().getOrDefault("customerPhone", ""));
        args.put("customerEmail", context.getContext().getOrDefault("customerEmail", ""));
        args.put("customerName", context.getContext().getOrDefault("customerName", ""));
        Map<String, Object> data = call(context, "find_customer_profile", args, result);
        result.getPayload().put("customerProfile", data);
        result.setGrounded(((Number) data.getOrDefault("count", 0)).intValue() > 0);
        String summary = stringValue(data.get("summary"));
        if (!summary.isBlank()) {
            result.setReply(summary);
            return result;
        }
        List<Map<String, Object>> recentOrders = mapList(data.get("recentOrders"));
        if (!recentOrders.isEmpty()) {
            Map<String, Object> first = recentOrders.get(0);
            result.setReply("Khách này có " + data.getOrDefault("orderCount", 0) + " đơn gần đây. Đơn mới nhất là "
                    + stringValue(first.get("maHoaDon")) + ", trạng thái " + stringValue(first.get("trangThai")) + ".");
            return result;
        }
        result.setReply("Em chưa thấy hồ sơ khách rõ ràng từ dữ liệu hiện có.");
        return result;
    }
}
