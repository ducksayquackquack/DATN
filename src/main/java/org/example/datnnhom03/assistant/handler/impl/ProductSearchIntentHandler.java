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
public class ProductSearchIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public ProductSearchIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.PRODUCT_SEARCH; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("keyword", context.getMessage());
        Map<String, Object> data = call(context, "search_product", args, result);
        result.getPayload().put("productSearch", data);
        List<Map<String, Object>> items = items(data);
        result.setGrounded(!items.isEmpty());
        if (items.isEmpty()) {
            result.setReply("Em chưa tìm thấy mẫu phù hợp. Anh/chị cho em thêm form áo, khoảng giá hoặc màu muốn ưu tiên.");
            return result;
        }
        Map<String, Object> first = items.get(0);
        result.setReply("Em đã lọc được " + items.size() + " mẫu. Gợi ý đầu tiên là " + stringValue(first.get("tenSanPham"))
                + ", giá từ " + money(first.get("giaTu")) + ".");
        return result;
    }
}
