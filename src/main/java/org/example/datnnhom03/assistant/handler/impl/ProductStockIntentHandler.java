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
public class ProductStockIntentHandler extends AbstractToolIntentHandler implements IntentHandler {
    public ProductStockIntentHandler(ToolDispatcher toolDispatcher) { super(toolDispatcher); }
    @Override public AssistantIntent supportedIntent() { return AssistantIntent.PRODUCT_STOCK; }

    @Override
    public AssistantHandlerResult handle(AssistantRequestContext context) {
        AssistantHandlerResult result = AssistantHandlerResult.of("", false);
        String normalized = context.getNormalizedMessage();
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("maSanPham", extractProductCode(context.getMessage()));
        args.put("maBienThe", extractVariantCode(context.getMessage()));
        args.put("keyword", buildStockKeyword(context.getMessage()));
        args.put("color", extractColor(normalized));
        args.put("size", extractSize(normalized));
        args.put("category", extractCategory(normalized));
        Map<String, Object> data = call(context, "check_product_stock", args, result);
        result.getPayload().put("productStock", data);
        List<Map<String, Object>> exactVariants = mapList(data.get("exactVariants"));
        List<Map<String, Object>> items = items(data);
        result.setGrounded(!exactVariants.isEmpty() || !items.isEmpty());
        if (!exactVariants.isEmpty()) {
            Map<String, Object> variant = exactVariants.get(0);
            result.setReply("Biến thể " + stringValue(variant.get("maBienThe")) + " - "
                    + stringValue(variant.get("tenSanPham")) + " hiện còn " + stringValue(variant.get("soLuong"))
                    + " sản phẩm"
                    + (stringValue(variant.get("mauSac")).isBlank() ? "" : ", màu " + stringValue(variant.get("mauSac")))
                    + (stringValue(variant.get("kichThuoc")).isBlank() ? "" : ", size " + stringValue(variant.get("kichThuoc")))
                    + ".");
            return result;
        }
        if (!items.isEmpty()) {
            Map<String, Object> first = items.get(0);
            String requestedName = normalize(buildStockKeyword(context.getMessage()));
            String firstName = normalize(stringValue(first.get("tenSanPham")));
            if (!requestedName.isBlank() && (firstName.contains(requestedName) || requestedName.contains(firstName))) {
                result.setReply(stringValue(first.get("tenSanPham")) + " hiện còn khoảng "
                        + stringValue(first.getOrDefault("tongTon", first.get("soLuong"))) + " chiếc.");
            } else {
                result.setReply("Em đã tìm thấy " + items.size() + " sản phẩm phù hợp. Mẫu nổi bật là "
                        + stringValue(first.get("tenSanPham")) + ", tổng tồn khoảng " + stringValue(first.getOrDefault("tongTon", first.get("soLuong"))) + ".");
            }
            return result;
        }
        result.setReply("Hiện em chưa thấy tồn kho phù hợp với mô tả này. Anh/chị thử gửi mã SPCT, màu hoặc size cụ thể hơn giúp em.");
        return result;
    }

    private String buildStockKeyword(String message) {
        String text = normalize(message);
        text = text.replace("?", " ");
        text = text.replaceAll("c[oò]n bao nh[iê]u chi[eế]c", " ");
        text = text.replaceAll("bao nh[iê]u chi[eế]c", " ");
        text = text.replaceAll("c[oò]n hàng", " ");
        text = text.replaceAll("t[oồ]n kho", " ");
        text = text.replaceAll("hi[eệ]n c[oò]n", " ");
        text = text.replaceAll("\s+", " ").trim();
        return text;
    }
}
