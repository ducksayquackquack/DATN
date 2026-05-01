package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SizeAdvisorTool implements AssistantTool {

    @Override
    public String getName() {
        return "size_advisor";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return true;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        Integer heightCm = intValue(args.get("heightCm"));
        Integer weightKg = intValue(args.get("weightKg"));

        Map<String, Object> result = new LinkedHashMap<>();
        if (heightCm == null && weightKg == null) {
            result.put("found", false);
            result.put("summary", "Chưa đủ dữ liệu chiều cao/cân nặng để tư vấn size.");
            return result;
        }

        String size = "M";
        if (weightKg != null) {
            if (weightKg < 55) size = "S";
            else if (weightKg < 66) size = "M";
            else if (weightKg < 76) size = "L";
            else size = "XL";
        }
        if (heightCm != null) {
            if (heightCm >= 178 && "M".equals(size)) size = "L";
            if (heightCm <= 162 && "L".equals(size)) size = "M";
        }

        result.put("found", true);
        result.put("size", size);
        result.put("heightCm", heightCm);
        result.put("weightKg", weightKg);
        result.put("summary", "Gợi ý size " + size + ". Nếu khách thích mặc oversize thì có thể tăng thêm 1 size.");
        return result;
    }

    private Integer intValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value).replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
