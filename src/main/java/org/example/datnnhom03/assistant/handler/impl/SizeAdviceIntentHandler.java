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
public class SizeAdviceIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public SizeAdviceIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.SIZE_ADVICE; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        Map<String, Object> args = new LinkedHashMap<>();
        Integer height = extractHeightCm(context.getNormalizedMessage());
        Integer weight = extractWeightKg(context.getNormalizedMessage());
        if (height != null) args.put("heightCm", height);
        if (weight != null) args.put("weightKg", weight);
        Map<String, Object> data = call(context, "size_advisor", args, result);
        result.setGrounded(Boolean.TRUE.equals(data.get("found")));
        result.setReply(stringValue(data.getOrDefault("summary", "Em chưa đủ dữ liệu để gợi ý size.")));
        return result;
    }
}
