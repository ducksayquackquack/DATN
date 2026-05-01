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
public class TopProductsIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public TopProductsIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.TOP_PRODUCTS; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        Map<String, Object> data = call(context, "get_top_products", currentMonthRange(5), result);
        result.getPayload().put("topProducts", data);
        List<Map<String, Object>> items = items(data);
        result.setGrounded(!items.isEmpty());
        if (items.isEmpty()) {
            result.setReply("Hiện chưa có dữ liệu top sản phẩm đủ rõ cho khoảng thời gian này.");
            return result;
        }
        Map<String, Object> first = items.get(0);
        result.setReply("Top sản phẩm hiện tại đang dẫn đầu là " + stringValue(first.get("tenSanPham"))
                + ", bán được " + stringValue(first.get("soLuongDaBan")) + " sản phẩm.");
        return result;
    }
}
