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
public class LowStockIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public LowStockIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.LOW_STOCK; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        Map<String, Object> data = call(context, "get_low_stock_products", Map.of("threshold", 10), result);
        List<Map<String, Object>> items = items(data);
        result.setGrounded(!items.isEmpty());
        String normalized = normalize(context.getMessage());
        if (containsAny(normalized, "gửi lại", "gui lai", "lọc tiếp", "loc tiep", "danh sách", "danh sach") && context.getState() != null) {
            normalized = normalize(String.valueOf(context.getState().getSlots().getOrDefault("lastUserMessage", context.getMessage())) + " " + normalized);
        }
        if (items.isEmpty()) {
            result.setReply("Hiện chưa có sản phẩm nào xuống dưới ngưỡng tồn thấp mặc định.");
            return result;
        }
        if (containsAny(normalized, "danh sách", "danh sach", "gửi lại", "gui lai", "liệt kê", "liet ke", "lọc tiếp", "loc tiep")) {
            String list = items.stream().limit(8)
                    .map(it -> stringValue(it.get("tenSanPham")) + " - SL " + stringValue(it.get("soLuong"))
                            + (stringValue(it.get("kichThuoc")).isBlank() ? "" : " - size " + stringValue(it.get("kichThuoc")))
                            + (stringValue(it.get("mauSac")).isBlank() ? "" : " - màu " + stringValue(it.get("mauSac"))))
                    .collect(Collectors.joining("; "));
            result.setReply("Danh sách sản phẩm sắp hết hàng: " + list + ".");
            return result;
        }
        result.setReply("Hiện có " + items.size() + " sản phẩm sắp chạm ngưỡng tồn thấp. Em có thể lọc tiếp để ưu tiên nhập hàng hoặc đẩy bán.");
        return result;
    }
}
