package org.example.datnnhom03.assistant.recommendation;

import org.example.datnnhom03.assistant.dto.AssistantChatRequest;
import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.example.datnnhom03.assistant.state.AssistantConversationState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AssistantRecommendationService {

    public List<Map<String, Object>> recommend(AssistantChatRequest request,
                                               AssistantRole role,
                                               String pageType,
                                               AssistantIntent intent,
                                               AssistantHandlerResult result,
                                               AssistantConversationState state) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        String message = request.getMessage() == null ? "" : request.getMessage().toLowerCase(Locale.ROOT);

        if (intent == AssistantIntent.CUSTOMER_PROFILE || intent == AssistantIntent.CUSTOMER_INSIGHT) {
            recommendations.add(action("upsell", "Gợi ý upsell theo lịch sử mua", "high"));
            recommendations.add(action("voucher_fit", "Đề xuất voucher phù hợp khách này", "medium"));
        }

        if (intent == AssistantIntent.ORDER_LOOKUP || intent == AssistantIntent.OVERDUE_ORDERS) {
            recommendations.add(action("order_followup", "Soạn tin nhắn cập nhật đơn cho khách", "high"));
            recommendations.add(action("handoff", "Đánh dấu cần ưu tiên xử lý", "medium"));
        }

        if (intent == AssistantIntent.PRODUCT_SEARCH || intent == AssistantIntent.PRODUCT_STOCK || containsAny(message, "upsell", "cross sell", "combo")) {
            recommendations.add(action("cross_sell", "Gợi ý sản phẩm đi kèm", "high"));
            recommendations.add(action("stock_guard", "Cảnh báo tồn kho trước khi chốt", "medium"));
        }

        if (intent == AssistantIntent.PAYMENT_ANOMALY || intent == AssistantIntent.REVENUE_SUMMARY) {
            recommendations.add(action("drill_down", "Xem nhóm giao dịch cần rà soát", "high"));
            recommendations.add(action("export", "Chuẩn bị payload cho dashboard admin", "medium"));
        }

        if (role == AssistantRole.ADMIN && (intent == AssistantIntent.TOP_PRODUCTS || intent == AssistantIntent.LOW_STOCK)) {
            recommendations.add(action("campaign", "Gợi ý campaign đẩy bán nhanh", "medium"));
        }

        if ("CUSTOMER_CHAT".equalsIgnoreCase(pageType)) {
            recommendations.add(action("reply", "Viết phản hồi mềm hơn cho khách", "medium"));
        }

        if (state != null && state.getLastIntent() != null && state.getLastIntent() != intent) {
            recommendations.add(action("continue_previous", "So sánh với luồng trước đó", "low"));
        }

        if (result != null && result.getPayload().containsKey("previewPayload")) {
            recommendations.add(action("preview", "Xem lại payload trước khi xác nhận", "high"));
        }

        return dedupe(recommendations);
    }

    private Map<String, Object> action(String type, String label, String priority) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        map.put("label", label);
        map.put("priority", priority);
        return map;
    }

    private List<Map<String, Object>> dedupe(List<Map<String, Object>> actions) {
        List<Map<String, Object>> deduped = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (Map<String, Object> action : actions) {
            String key = String.valueOf(action.getOrDefault("type", "")) + "|" + String.valueOf(action.getOrDefault("label", ""));
            if (!seen.contains(key)) {
                seen.add(key);
                deduped.add(action);
            }
        }
        return deduped;
    }

    private boolean containsAny(String text, String... keywords) {
        String normalizedText = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && normalizedText.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
