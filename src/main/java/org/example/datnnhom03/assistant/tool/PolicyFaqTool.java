package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class PolicyFaqTool implements AssistantTool {
    @Override public String getName() { return "policy_faq"; }
    @Override public boolean supports(AssistantRole role) { return true; }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        String query = normalize(args.get("query"));
        Map<String, Object> result = new LinkedHashMap<>();
        if (containsAny(query, "phí vận chuyển", "phi van chuyen", "ship bao nhiêu", "ship bao nhieu")) {
            result.put("type", "shipping_fee");
            result.put("answer", "Phí vận chuyển phụ thuộc khu vực giao và phương thức giao hàng. Anh/chị gửi giúp em địa chỉ hoặc tỉnh/thành để em kiểm tra sát hơn, còn nội thành thường thấp hơn liên tỉnh.");
            return result;
        }
        if (containsAny(query, "đổi trả", "doi tra", "trả hàng", "tra hang")) {
            result.put("type", "return_policy");
            result.put("answer", "Chính sách đổi trả: sản phẩm cần còn nguyên tem mác, chưa qua sử dụng và còn trong thời hạn hỗ trợ đổi trả theo quy định của shop. Nếu anh/chị muốn, em có thể soạn sẵn câu trả lời ngắn gọn để gửi khách.");
            return result;
        }
        if (containsAny(query, "bao lâu", "giao tới", "giao toi", "mấy ngày", "may ngay", "khi nào tới", "khi nao toi")) {
            result.put("type", "delivery_eta");
            result.put("answer", "Thời gian giao hàng thường phụ thuộc khu vực nhận hàng và đơn vị vận chuyển. Nội thành thường nhanh hơn, còn liên tỉnh cần thêm thời gian xử lý và vận chuyển. Anh/chị gửi giúp em khu vực giao để em ước lượng sát hơn.");
            return result;
        }
        result.put("type", "general_policy");
        result.put("answer", "Em có thể hỗ trợ giải thích về phí vận chuyển, đổi trả hoặc thời gian giao hàng. Anh/chị nói rõ phần muốn kiểm tra là em trả lời sát hơn.");
        return result;
    }

    private boolean containsAny(String text, String... keywords) { for (String k : keywords) if (normalize(text).contains(normalize(k))) return true; return false; }
    private String normalize(Object value) { return value == null ? "" : String.valueOf(value).trim().toLowerCase(Locale.ROOT); }
}
