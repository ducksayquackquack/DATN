package org.example.datnnhom03.assistant.handler.impl;

import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.handler.AssistantRequestContext;
import org.example.datnnhom03.assistant.handler.IntentHandler;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.service.ToolDispatcher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OverdueOrdersIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public OverdueOrdersIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.OVERDUE_ORDERS; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        Map<String, Object> data = call(context, "get_overdue_orders", Map.of("limit", 10), result);
        result.setGrounded(((Number) data.getOrDefault("count", 0)).intValue() > 0 || data.containsKey("summary"));
        result.setReply(stringValue(data.getOrDefault("summary", "Em đã rà danh sách đơn trễ cần theo dõi.")));
        return result;
    }
}
