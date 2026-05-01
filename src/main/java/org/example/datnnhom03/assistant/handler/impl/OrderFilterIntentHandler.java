package org.example.datnnhom03.assistant.handler.impl;

import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.handler.AssistantRequestContext;
import org.example.datnnhom03.assistant.handler.IntentHandler;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.service.ToolDispatcher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OrderFilterIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public OrderFilterIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.ORDER_FILTER; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        String query = context.getMessage();
        if (containsAny(normalize(query), "gửi lại", "gui lai", "lọc tiếp", "loc tiep", "danh sách", "danh sach", "chi tiết", "chi tiet")
                && context.getState() != null) {
            query = String.valueOf(context.getState().getSlots().getOrDefault("lastUserMessage", query));
        }
        Map<String, Object> data = call(context, "filter_orders_natural", Map.of("query", query), result);
        List<Map<String, Object>> items = items(data);
        result.getPayload().put("orderFilter", data);
        result.setGrounded(!items.isEmpty() || String.valueOf(data.getOrDefault("summary", "")).length() > 0);
        if (items.isEmpty()) {
            result.setReply(String.valueOf(data.getOrDefault("summary", "Em chưa thấy đơn phù hợp với bộ lọc này.")));
            return result;
        }
        String list = items.stream().limit(5).map(it -> String.valueOf(it.getOrDefault("maHoaDon", "")) + " (" + String.valueOf(it.getOrDefault("trangThai", "")) + ")").collect(Collectors.joining(", "));
        result.setReply(String.valueOf(data.getOrDefault("summary", "Em đã lọc được " + items.size() + " đơn.")) + " Danh sách gần nhất: " + list + ".");
        return result;
    }
}
