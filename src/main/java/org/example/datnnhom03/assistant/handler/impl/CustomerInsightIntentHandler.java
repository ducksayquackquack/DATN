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
public class CustomerInsightIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public CustomerInsightIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.CUSTOMER_INSIGHT; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("sessionCode", context.getSessionCode());
        args.put("customerPhone", context.getContext().getOrDefault("customerPhone", ""));
        args.put("customerEmail", context.getContext().getOrDefault("customerEmail", ""));
        args.put("customerName", context.getContext().getOrDefault("customerName", ""));
        Map<String, Object> data = call(context, "get_customer_insight", args, result);
        result.getPayload().put("customerInsight", data);
        result.setGrounded(Boolean.TRUE.equals(data.get("found")) || ((Number) data.getOrDefault("count", 0)).intValue() > 0);
        result.setReply(stringValue(data.getOrDefault("summary", "Em chưa có đủ dữ liệu để tóm tắt khách này.")));
        return result;
    }
}
