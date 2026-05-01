package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Model.PTTT;
import org.example.datnnhom03.Repository.PTTTRepository;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PaymentMethodTool implements AssistantTool {

    private final PTTTRepository ptttRepository;

    public PaymentMethodTool(PTTTRepository ptttRepository) {
        this.ptttRepository = ptttRepository;
    }

    @Override
    public String getName() {
        return "get_payment_methods";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return true;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        List<Map<String, Object>> items = ptttRepository.findAll().stream()
                .filter(this::isActive)
                .map(this::toRow)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", items.size());
        result.put("items", items);
        return result;
    }

    private boolean isActive(PTTT pttt) {
        String status = safe(pttt.getTrangThai());
        return status.isBlank() || status.contains("hoat dong") || status.contains("active") || status.contains("hien");
    }

    private Map<String, Object> toRow(PTTT method) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ma", method.getMa());
        row.put("ten", method.getTen());
        row.put("moTa", method.getMoTa());
        return row;
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                .replaceAll("[ìíịỉĩ]", "i")
                .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                .replaceAll("[ùúụủũưừứựửữ]", "u")
                .replaceAll("[ỳýỵỷỹ]", "y")
                .replaceAll("[đ]", "d")
                .trim();
    }
}
