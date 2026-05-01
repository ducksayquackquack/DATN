package org.example.datnnhom03.assistant.planner;

import org.example.datnnhom03.assistant.dto.AssistantChatRequest;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.example.datnnhom03.assistant.state.AssistantConversationState;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AssistantPlannerService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?:0|84)(?:\\d[ .-]?){8,10}");
    private static final Pattern ORDER_PATTERN = Pattern.compile("\\b(?:HD|DH)[-_ ]?\\d{4,}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PERCENT_PATTERN = Pattern.compile("\\b(\\d{1,2})\\s?%");
    private static final Pattern MONEY_PATTERN = Pattern.compile("\\b(\\d{2,9})\\s?(k|nghìn|nghin|triệu|trieu|đ|vnd)\\b", Pattern.CASE_INSENSITIVE);

    public AssistantExecutionPlan plan(AssistantChatRequest request,
                                       String normalizedMessage,
                                       String pageType,
                                       AssistantRole role,
                                       AssistantIntent detectedIntent,
                                       AssistantConversationState state) {
        AssistantExecutionPlan plan = new AssistantExecutionPlan();
        plan.setDetectedIntent(detectedIntent);
        plan.setPlannedIntent(detectedIntent);
        plan.setConfidence("MEDIUM");
        plan.setReasoning("dùng detector mặc định");

        extractContext(normalizedMessage, plan);

        if (state != null && state.getLastIntent() != null && shouldContinuePreviousFlow(normalizedMessage)) {
            AssistantIntent continuedIntent = state.getLastIntent();
            String previousUserMessage = stringValue(state.getSlots().get("lastUserMessage"));
            if (continuedIntent == AssistantIntent.SIZE_ADVICE
                    && containsAny(previousUserMessage, "tao don", "tạo đơn", "hoa don", "hóa đơn", "len don", "lên đơn")) {
                continuedIntent = AssistantIntent.CREATE_ORDER;
                plan.setReasoning("đang bổ sung slot cho luồng tạo đơn nên quay lại intent tạo đơn thay vì tách riêng tư vấn size");
                plan.setConfidence("MEDIUM");
            } else {
                plan.setReasoning("câu hiện tại ngắn và mang tính tiếp nối nên bám theo intent trước đó");
                plan.setConfidence("LOW");
            }
            plan.setPlannedIntent(continuedIntent);
            plan.setRoute("STATE_CONTINUATION");
            plan.setUsedStateHints(true);
        }

        if (containsAny(normalizedMessage, "upsell", "cross sell", "cross-sell", "combo", "gợi ý sản phẩm", "goi y san pham", "nên bán", "nen ban")) {
            plan.setPlannedIntent(AssistantIntent.PRODUCT_SEARCH);
            plan.setRoute("PLANNER_RULE_RECOMMEND_PRODUCT");
            plan.setReasoning("nội dung nghiêng về gợi ý bán hàng và tìm sản phẩm phù hợp");
            plan.setConfidence("MEDIUM");
        }

        if (containsAny(normalizedMessage, "voucher", "mã giảm", "ma giam", "khuyến mãi", "khuyen mai")) {
            plan.setPlannedIntent(AssistantIntent.PROMOTION_QUERY);
            plan.setRoute("PLANNER_RULE_PROMOTION_QUERY");
            plan.setReasoning("bot hiện chỉ tư vấn ưu đãi đang có, không tạo voucher mới.");
            plan.setConfidence("MEDIUM");
        }

        if (containsAny(normalizedMessage, "bất thường", "bat thuong", "rủi ro", "rui ro", "lệch thanh toán", "lech thanh toan", "nghi ngờ", "nghi ngo")) {
            plan.setPlannedIntent(AssistantIntent.PAYMENT_ANOMALY);
            plan.setRoute("PLANNER_RULE_PAYMENT_ANOMALY");
            plan.setReasoning("nội dung có dấu hiệu yêu cầu kiểm tra bất thường thanh toán");
            plan.setConfidence("HIGH");
        }

        if (containsAny(normalizedMessage, "đổi trạng thái", "doi trang thai", "chuyển trạng thái", "chuyen trang thai", "xác nhận giao", "xac nhan giao")) {
            plan.setPlannedIntent(AssistantIntent.UPDATE_ORDER_STATUS);
            plan.setRoute("PLANNER_RULE_UPDATE_STATUS");
            plan.setReasoning("nội dung đang hướng tới cập nhật trạng thái đơn");
            plan.setConfidence("HIGH");
        }

        if (containsAny(normalizedMessage, "tạo đơn", "tao don", "lên đơn", "len don", "đặt đơn", "dat don")) {
            plan.setPlannedIntent(AssistantIntent.CREATE_ORDER);
            plan.setRoute("PLANNER_RULE_CREATE_ORDER");
            plan.setReasoning("nội dung đang hướng tới thao tác tạo đơn");
            plan.setConfidence("HIGH");
        }

        if ((containsAny(normalizedMessage, "bao nhiêu đơn", "bao nhieu don", "có bao nhiêu đơn", "co bao nhieu don")
                && containsAny(normalizedMessage, "chờ xử lý", "cho xu ly", "hoàn thành", "hoan thanh", "đã giao", "da giao", "thành công", "thanh cong"))) {
            plan.setPlannedIntent(AssistantIntent.ORDER_STATUS_COUNT);
            plan.setRoute("PLANNER_RULE_ORDER_STATUS_COUNT");
            plan.setReasoning("nội dung đang hỏi số lượng đơn theo trạng thái");
            plan.setConfidence("HIGH");
        }

        if (containsAny(normalizedMessage, "phí vận chuyển", "phi van chuyen", "đổi trả", "doi tra", "bao lâu thì giao", "bao lau thi giao", "giao tới", "giao toi", "chính sách", "chinh sach")) {
            plan.setPlannedIntent(AssistantIntent.POLICY_FAQ);
            plan.setRoute("PLANNER_RULE_POLICY_FAQ");
            plan.setReasoning("nội dung hỏi về chính sách đổi trả, phí ship hoặc thời gian giao hàng");
            plan.setConfidence("HIGH");
        }

        if (containsAny(normalizedMessage, "đơn chưa giao", "don chua giao", "lọc đơn", "loc don", "xem các đơn", "xem cac don", "ở hà nội", "o ha noi", "ha noi")
                && containsAny(normalizedMessage, "đơn", "don")) {
            plan.setPlannedIntent(AssistantIntent.ORDER_FILTER);
            plan.setRoute("PLANNER_RULE_ORDER_FILTER");
            plan.setReasoning("nội dung đang yêu cầu lọc danh sách đơn theo điều kiện tự nhiên");
            plan.setConfidence("HIGH");
        }

        if (pageType != null && pageType.toUpperCase(Locale.ROOT).contains("CUSTOMER_CHAT")
                && detectedIntent == AssistantIntent.GENERAL_QA
                && containsAny(normalizedMessage, "trả lời khách", "tra loi khach", "rep khách", "reply khách", "soạn tin", "soan tin")) {
            plan.setPlannedIntent(AssistantIntent.CUSTOMER_REPLY_DRAFT);
            plan.setRoute("PLANNER_RULE_CUSTOMER_CHAT_REPLY");
            plan.setReasoning("ngữ cảnh chat khách và yêu cầu soạn phản hồi");
            plan.setConfidence("MEDIUM");
        }

        return plan;
    }

    private void extractContext(String normalizedMessage, AssistantExecutionPlan plan) {
        String phone = firstMatch(PHONE_PATTERN, normalizedMessage).replaceAll("[^0-9]", "");
        if (!phone.isBlank()) {
            plan.getExtractedContext().put("customerPhone", phone);
            plan.getExtractedContext().put("orderCodeOrPhone", phone);
        }

        String orderCode = firstMatch(ORDER_PATTERN, normalizedMessage).toUpperCase(Locale.ROOT).replace(" ", "");
        if (!orderCode.isBlank()) {
            plan.getExtractedContext().put("orderCode", orderCode);
            plan.getExtractedContext().put("orderCodeOrPhone", orderCode);
        }

        String percent = firstCapture(PERCENT_PATTERN, normalizedMessage);
        if (!percent.isBlank()) {
            plan.getExtractedContext().put("discountValue", percent + "%");
        }

        String money = firstMatch(MONEY_PATTERN, normalizedMessage);
        if (!money.isBlank() && !plan.getExtractedContext().containsKey("discountValue")) {
            plan.getExtractedContext().put("discountValue", money.trim());
        }
    }

    private String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group() : "";
    }

    private String firstCapture(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        return matcher.find() && matcher.groupCount() >= 1 ? matcher.group(1) : "";
    }

    private boolean shouldContinuePreviousFlow(String normalizedMessage) {
        return containsAny(normalizedMessage,
                "đơn nào", "don nao", "cái nào", "cai nao", "mẫu nào", "mau nao",
                "loại nào", "loai nao", "còn cái nào", "con cai nao", "còn đơn nào", "con don nao",
                "voucher nào", "voucher nao", "mã nào", "ma nao", "cái trước", "cai truoc", "gửi lại", "gui lai", "gửi lại danh sách", "gui lai danh sach", "lọc tiếp", "loc tiep", "chi tiết hơn", "chi tiet hon", "danh sách", "danh sach",
                "heightcm", "weightkg", "chiều cao", "chieu cao", "cân nặng", "can nang")
                || normalizedMessage.matches(".*(?:heightcm|weightkg)\\s*[:=].*")
                || normalizedMessage.matches(".*(?:chiều cao|chieu cao|cân nặng|can nang)\\s*[:=]?.*");
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
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
