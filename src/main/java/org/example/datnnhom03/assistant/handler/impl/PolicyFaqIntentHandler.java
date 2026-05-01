package org.example.datnnhom03.assistant.handler.impl;

import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.handler.AssistantRequestContext;
import org.example.datnnhom03.assistant.handler.IntentHandler;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.service.ToolDispatcher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PolicyFaqIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public PolicyFaqIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.POLICY_FAQ; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        Map<String, Object> data = call(context, "policy_faq", Map.of("query", context.getMessage()), result);
        result.getPayload().put("policyFaq", data);
        result.setGrounded(true);
        result.setReply(String.valueOf(data.getOrDefault("answer", "Em chưa lấy được thông tin chính sách phù hợp.")));
        return result;
    }
}
