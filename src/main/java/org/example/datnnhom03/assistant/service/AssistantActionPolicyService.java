package org.example.datnnhom03.assistant.service;

import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AssistantActionPolicyService {

    public boolean requiresConfirmation(AssistantIntent intent, Map<String, Object> payload) {
        if (intent == null) return false;
        return switch (intent) {
            case CREATE_ORDER, UPDATE_ORDER_STATUS, PRODUCT_UPSERT -> true;
            default -> false;
        };
    }

    public String confirmationMessage(AssistantIntent intent) {
        if (intent == AssistantIntent.CUSTOMER_REPLY_DRAFT) {
            return "Anh/chị xem lại nội dung trước khi gửi cho khách.";
        }
        if (intent == AssistantIntent.CREATE_ORDER) {
            return "Anh/chị xác nhận để em khóa payload đơn nháp và bàn giao qua bước tạo đơn.";
        }
        if (intent == AssistantIntent.UPDATE_ORDER_STATUS) {
            return "Anh/chị xác nhận để em bàn giao yêu cầu đổi trạng thái qua workflow an toàn.";
        }
        if (intent == AssistantIntent.PRODUCT_UPSERT) {
            return "Anh/chị xác nhận để em ghi thay đổi sản phẩm.";
        }
        return "Anh/chị xác nhận trước khi thực hiện hành động này.";
    }

    public List<Map<String, Object>> buildSuggestedActions(AssistantIntent intent) {
        return switch (intent) {
            case ORDER_LOOKUP -> List.of(
                    Map.of("type", "copy_order_code", "label", "Sao chép mã đơn", "priority", "high"),
                    Map.of("type", "check_shipping", "label", "Kiểm tra giao vận", "priority", "medium")
            );
            case PRODUCT_STOCK -> List.of(
                    Map.of("type", "open_product", "label", "Mở sản phẩm", "priority", "high"),
                    Map.of("type", "suggest_alt_variant", "label", "Gợi ý biến thể gần nhất", "priority", "medium")
            );
            case CUSTOMER_REPLY_DRAFT -> List.of(
                    Map.of("type", "polish_reply", "label", "Viết mềm hơn", "priority", "high"),
                    Map.of("type", "add_upsell", "label", "Thêm upsell nhẹ", "priority", "medium")
            );
            case CUSTOMER_PROFILE -> List.of(
                    Map.of("type", "open_customer", "label", "Mở hồ sơ khách", "priority", "high"),
                    Map.of("type", "recent_orders", "label", "Xem đơn gần đây", "priority", "medium")
            );
            case PRODUCT_UPSERT -> List.of(
                    Map.of("type", "confirm_action", "label", "Xác nhận cập nhật sản phẩm", "priority", "high"),
                    Map.of("type", "cancel_action", "label", "Hủy nháp", "priority", "medium")
            );
            case CREATE_ORDER, UPDATE_ORDER_STATUS -> List.of(
                    Map.of("type", "handoff", "label", "Mở workflow nghiệp vụ", "priority", "high"),
                    Map.of("type", "cancel_action", "label", "Hủy nháp", "priority", "medium")
            );
            default -> List.of();
        };
    }
}
