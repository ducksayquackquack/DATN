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
public class OrderLookupIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public OrderLookupIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.ORDER_LOOKUP; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        String keyword = extractOrderCode(context.getMessage());
        if (keyword.isBlank()) keyword = digitsOnly(context.getContext().get("customerPhone"));
        if (keyword.isBlank()) keyword = context.getMessage();
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("keyword", keyword);
        Map<String, Object> data = call(context, "find_order", args, result);
        result.getPayload().put("orderLookup", data);
        List<Map<String, Object>> items = items(data);
        result.setGrounded(!items.isEmpty());
        if (items.isEmpty()) {
            result.setReply("Em chưa thấy đơn khớp với thông tin vừa gửi. Anh/chị kiểm tra lại mã hóa đơn hoặc số điện thoại nhận hàng giúp em.");
            return result;
        }
        Map<String, Object> first = items.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("Em đã tìm thấy ").append(items.size()).append(" đơn phù hợp.");
        sb.append(" Đơn gần nhất là ").append(stringValue(first.get("maHoaDon")));
        sb.append(", trạng thái ").append(stringValue(first.get("trangThai")));
        if (!stringValue(first.get("tenKhachHang")).isBlank()) {
            sb.append(", khách ").append(stringValue(first.get("tenKhachHang")));
        }
        if (!stringValue(first.get("thanhTien")).isBlank()) {
            sb.append(", giá trị ").append(money(first.get("thanhTien")));
        }
        if (!stringValue(first.get("ngayNhanHangDuKien")).isBlank()) {
            sb.append(", dự kiến giao ").append(stringValue(first.get("ngayNhanHangDuKien")));
        }
        sb.append(".");
        result.setReply(sb.toString());
        return result;
    }
}
