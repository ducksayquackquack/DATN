package org.example.datnnhom03.assistant.service;

import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AssistantIntentDetector {

    public AssistantIntent detect(String normalized, String pageType) {
        if (containsAny(normalized, "xac nhan", "xác nhận", "dong y", "đồng ý", "ok tao", "ok tạo", "confirm")) {
            return AssistantIntent.CONFIRM_ACTION;
        }
        if (containsAny(normalized, "huy thao tac", "hủy thao tác", "bo qua", "bỏ qua", "cancel")) {
            return AssistantIntent.CANCEL_ACTION;
        }
        if (containsAny(normalized, "hoàn tiền", "refund", "huy don", "hủy đơn", "sua ton", "sửa tồn", "xoa", "xóa", "doi vai tro", "đổi vai trò")) {
            return AssistantIntent.BLOCKED_ACTION;
        }
                if (containsAny(normalized, "tao don", "tạo đơn", "len don", "lên đơn", "chot don", "chốt đơn")
                || (containsAny(normalized, "tao", "tạo", "lap", "lập") && containsAny(normalized, "hoa don", "hóa đơn"))) {
            return AssistantIntent.CREATE_ORDER;
        }
        if (containsAny(normalized, "cap nhat trang thai", "cập nhật trạng thái", "doi trang thai", "đổi trạng thái", "chuyen sang dang giao", "chuyển sang đang giao", "da giao", "đã giao")) {
            return AssistantIntent.UPDATE_ORDER_STATUS;
        }
        if (containsAny(normalized, "tao san pham", "tạo sản phẩm", "cap nhat san pham", "cập nhật sản phẩm", "upsert san pham", "doi mo ta san pham", "đổi mô tả sản phẩm")) {
            return AssistantIntent.PRODUCT_UPSERT;
        }
        if (containsAny(normalized, "bat thuong thanh toan", "bất thường thanh toán", "payment anomaly", "cod treo", "thanh toan loi", "thanh toán lỗi")) {
            return AssistantIntent.PAYMENT_ANOMALY;
        }
        if ((containsAny(normalized, "bao nhieu don", "bao nhiêu đơn", "có bao nhiêu đơn", "co bao nhieu don")
                && containsAny(normalized, "cho xu ly", "chờ xử lý", "hoàn thành", "hoan thanh", "đã giao", "da giao", "thành công", "thanh cong"))
                || containsAny(normalized, "bao nhiêu đơn chờ xử lý", "bao nhieu don cho xu ly", "bao nhiêu đơn hoàn thành", "bao nhieu don hoan thanh")) {
            return AssistantIntent.ORDER_STATUS_COUNT;
        }
        if (containsAny(normalized, "khách này từng mua", "khach nay tung mua", "hồ sơ khách", "ho so khach", "thông tin khách", "thong tin khach", "khách hàng này", "khach hang nay", "lịch sử khách", "lich su khach", "mua gì", "mua gi", "đã mua gì", "da mua gi", "gần đây", "gan day", "tóm tắt hồ sơ khách", "tom tat ho so khach", "hồ sơ khách hàng", "ho so khach hang")) {
            return AssistantIntent.CUSTOMER_PROFILE;
        }
        if ("CUSTOMER_CHAT".equals(pageType) && containsAny(normalized,
                "tóm tắt khách", "tom tat khach", "tóm tắt hội thoại", "tom tat hoi thoai", "phiên chat này", "phien chat nay")) {
            return AssistantIntent.CUSTOMER_INSIGHT;
        }
        if ("CUSTOMER_CHAT".equals(pageType) && containsAny(normalized, "viết", "soạn", "trả lời khách", "tra loi khach", "nhắn khách", "chốt sale", "upsell", "mềm hơn", "lich su hon", "lịch sự hơn", "rút gọn", "viet lai", "viết lại")) {
            return AssistantIntent.CUSTOMER_REPLY_DRAFT;
        }
        if (containsAny(normalized, "hàng đợi chat", "hang doi chat", "chat chờ", "chat cho", "khách chờ", "khach cho", "queue", "phiên chờ", "phien cho")) {
            return AssistantIntent.CHAT_QUEUE_SUMMARY;
        }
        if (containsAny(normalized, "đơn trễ", "don tre", "đơn giao trễ", "don giao tre", "giao chậm", "giao cham", "quá hạn giao", "qua han giao", "trễ cần xử lý", "tre can xu ly", "đơn treo", "don treo", "bị treo", "bi treo")) {
            return AssistantIntent.OVERDUE_ORDERS;
        }
        if (containsAny(normalized, "doanh thu", "trung bình mỗi đơn", "bao nhiêu tiền", "hôm nay", "hom nay", "hôm qua", "hom qua", "tuần này", "tuan nay", "tuần trước", "tuan truoc", "tháng này", "thang nay", "tháng", "thang", "quý", "quy", "năm", "nam")) {
            return AssistantIntent.REVENUE_SUMMARY;
        }
        if (containsAny(normalized, "top sản phẩm", "bán chạy", "ban chay", "best seller", "hot nhat", "hot nhất", "mẫu nào hot", "mau nao hot", "top 5", "top 10", "de ban", "dễ bán")) {
            return AssistantIntent.TOP_PRODUCTS;
        }
        if (containsAny(normalized, "lịch làm", "lich lam", "ca làm", "ca lam", "phân công", "phan cong", "lịch trực", "gần nhất", "gan nhat", "nhân viên nào", "nhan vien nao")) {
            return AssistantIntent.WORK_SCHEDULE;
        }
        if (containsAny(normalized, "voucher", "khuyến mãi", "khuyen mai", "ưu đãi", "uu dai", "sale")) {
            return AssistantIntent.PROMOTION_QUERY;
        }
        if (containsAny(normalized, "thanh toán", "thanh toan", "cod", "chuyển khoản", "chuyen khoan", "ví điện tử", "vi dien tu")) {
            return AssistantIntent.PAYMENT_QUERY;
        }
        if (containsAny(normalized, "size", "kích thước", "kich thuoc", "cao", "nặng", "nang", "oversize", "form rộng", "form rong")) {
            return AssistantIntent.SIZE_ADVICE;
        }
        if (containsAny(normalized, "tồn thấp", "ton thap", "sắp hết", "sap het", "tồn kho thấp")) {
            return AssistantIntent.LOW_STOCK;
        }
        if (containsAny(normalized, "phí vận chuyển", "phi van chuyen", "ship bao nhiêu", "ship bao nhieu", "đổi trả", "doi tra", "giao tới", "giao toi", "bao lâu thì giao", "bao lau thi giao", "chính sách", "chinh sach")) {
            return AssistantIntent.POLICY_FAQ;
        }
        if (containsAny(normalized, "đơn chưa giao", "don chua giao", "lọc đơn", "loc don", "xem các đơn", "xem cac don", "ở hà nội", "o ha noi", "ha noi")
                && containsAny(normalized, "đơn", "don")) {
            return AssistantIntent.ORDER_FILTER;
        }
        if (normalized.matches(".*hd[0-9a-z]+.*") || containsAny(normalized, "mã hd", "hoa don", "hóa đơn", "đơn hàng", "don hang")) {
            return AssistantIntent.ORDER_LOOKUP;
        }
        if (normalized.matches(".*spct[0-9a-z]+.*")
                || containsAny(normalized, "còn hàng", "con hang", "tồn kho", "ton kho", "còn bao nhiêu chiếc", "con bao nhieu chiec", "bao nhiêu chiếc", "bao nhieu chiec")
                || (!extractCategory(normalized).isBlank() && !extractSize(normalized).isBlank())
                || (!extractCategory(normalized).isBlank() && !extractColor(normalized).isBlank())) {
            return AssistantIntent.PRODUCT_STOCK;
        }
        if (containsAny(normalized, "hoodie", "bomber", "coach", "áo khoác", "ao khoac", "sản phẩm", "san pham", "gợi ý", "goi y")) {
            return AssistantIntent.PRODUCT_SEARCH;
        }
        return AssistantIntent.GENERAL_QA;
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isBlank()) return false;
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String extractColor(String normalized) {
        if (containsAny(normalized, "đen", "den")) return "đen";
        if (containsAny(normalized, "trắng", "trang")) return "trắng";
        if (containsAny(normalized, "xanh", "navy")) return "xanh";
        if (containsAny(normalized, "xám", "xam")) return "xám";
        if (containsAny(normalized, "đỏ", "do")) return "đỏ";
        return "";
    }

    private String extractSize(String normalized) {
        Matcher matcher = Pattern.compile("\\b(xx?l|3xl|4xl|5xl|xs|s|m|l|xl)\\b", Pattern.CASE_INSENSITIVE).matcher(normalized);
        return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : "";
    }

    private String extractCategory(String normalized) {
        if (containsAny(normalized, "hoodie")) return "hoodie";
        if (containsAny(normalized, "bomber")) return "bomber";
        if (containsAny(normalized, "coach")) return "coach";
        if (containsAny(normalized, "áo khoác", "ao khoac", "jacket")) return "áo khoác";
        return "";
    }
}
