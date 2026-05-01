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
public class CustomerReplyDraftIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public CustomerReplyDraftIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.CUSTOMER_REPLY_DRAFT; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        String message = context.getMessage();
        String normalized = context.getNormalizedMessage();
        String lastCustomerMessage = stringValue(context.getContext().get("lastCustomerMessage"));

        if (!extractOrderCode(message + " " + lastCustomerMessage).isBlank()) {
            Map<String, Object> order = call(context, "find_order", Map.of("keyword", extractOrderCode(message + " " + lastCustomerMessage)), result);
            List<Map<String, Object>> items = items(order);
            if (!items.isEmpty()) {
                Map<String, Object> first = items.get(0);
                result.setGrounded(true);
                result.setReply("Anh/chị có thể trả lời khách như sau:\n\n“Dạ em đã kiểm tra giúp anh/chị. Đơn "
                        + stringValue(first.get("maHoaDon")) + " hiện đang ở trạng thái " + stringValue(first.get("trangThai"))
                        + ". Nếu mình cần em kiểm tra thêm tiến độ giao hoặc xác nhận lại thông tin nhận hàng, em hỗ trợ ngay ạ.”");
                return result;
            }
        }

        if (containsAny(normalized + " " + normalize(lastCustomerMessage), "còn hàng", "ton kho", "tồn kho", "size", "màu", "mau")) {
            Map<String, Object> stock = call(context, "check_product_stock", Map.of(
                    "keyword", lastCustomerMessage.isBlank() ? message : lastCustomerMessage,
                    "maSanPham", extractProductCode(message + " " + lastCustomerMessage),
                    "maBienThe", extractVariantCode(message + " " + lastCustomerMessage),
                    "color", extractColor(normalized + " " + normalize(lastCustomerMessage)),
                    "size", extractSize(normalized + " " + normalize(lastCustomerMessage)),
                    "category", extractCategory(normalized + " " + normalize(lastCustomerMessage))
            ), result);
            List<Map<String, Object>> variants = mapList(stock.get("exactVariants"));
            if (!variants.isEmpty()) {
                Map<String, Object> first = variants.get(0);
                result.setGrounded(true);
                result.setReply("Anh/chị có thể trả lời khách như sau:\n\n“Dạ em đã kiểm tra giúp anh/chị. "
                        + stringValue(first.get("tenSanPham"))
                        + (stringValue(first.get("mauSac")).isBlank() ? "" : " màu " + stringValue(first.get("mauSac")))
                        + (stringValue(first.get("kichThuoc")).isBlank() ? "" : ", size " + stringValue(first.get("kichThuoc")))
                        + " hiện còn " + stringValue(first.get("soLuong"))
                        + " sản phẩm. Nếu anh/chị muốn em có thể giữ giúp hoặc gợi ý thêm biến thể gần nhất ạ.”");
                return result;
            }
        }

        if (containsAny(normalized + " " + normalize(lastCustomerMessage), "voucher", "khuyến mãi", "uu dai", "ưu đãi")) {
            Map<String, Object> promo = call(context, "get_promotions", Map.of(), result);
            result.setGrounded(((Number) promo.getOrDefault("count", 0)).intValue() > 0);
            result.setReply("Anh/chị có thể trả lời khách như sau:\n\n“Dạ hiện shop đang có ưu đãi phù hợp. Em sẽ gửi anh/chị lựa chọn tốt nhất theo đơn hiện tại để mình chốt nhanh hơn ạ.”");
            return result;
        }

        result.setReply("Anh/chị có thể trả lời khách như sau:\n\n“Dạ em đã nhận thông tin của anh/chị. Em kiểm tra lại thật kỹ và phản hồi ngay để mình chốt nhanh nhất ạ.”");
        return result;
    }
}
