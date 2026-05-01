package org.example.datnnhom03.assistant.handler.impl;

import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.handler.AssistantRequestContext;
import org.example.datnnhom03.assistant.handler.IntentHandler;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.service.ToolDispatcher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PaymentQueryIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public PaymentQueryIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.PAYMENT_QUERY; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        Map<String, Object> data = call(context, "get_payment_methods", Map.of(), result);
        List<Map<String, Object>> items = items(data);
        result.setGrounded(!items.isEmpty());
        result.setReply(items.isEmpty()
                ? "Em chưa lấy được cấu hình phương thức thanh toán hiện tại."
                : "Hiện shop đang hỗ trợ " + items.size() + " phương thức thanh toán. Em có thể giải thích thêm COD, chuyển khoản hoặc ví điện tử nếu anh/chị cần.");
        return result;
    }
}
