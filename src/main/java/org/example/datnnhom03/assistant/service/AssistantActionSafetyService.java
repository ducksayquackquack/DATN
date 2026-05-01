package org.example.datnnhom03.assistant.service;

import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AssistantActionSafetyService {

    public SafetyCheckResult validateForPreview(AssistantIntent intent, Map<String, Object> payload) {
        return validate(intent, payload, false);
    }

    public SafetyCheckResult validateForExecution(AssistantIntent intent, Map<String, Object> payload) {
        return validate(intent, payload, true);
    }

    private SafetyCheckResult validate(AssistantIntent intent, Map<String, Object> payload, boolean strict) {
        SafetyCheckResult result = new SafetyCheckResult();
        if (intent == null) {
            result.addError("Thiếu intent để kiểm tra an toàn.");
            return result;
        }
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        switch (intent) {
            case CREATE_VOUCHER -> validateVoucher(safePayload, strict, result);
            case PRODUCT_UPSERT -> validateProduct(safePayload, strict, result);
            case CREATE_ORDER -> validateCreateOrder(safePayload, strict, result);
            case UPDATE_ORDER_STATUS -> validateUpdateOrderStatus(safePayload, strict, result);
            default -> {
            }
        }
        return result;
    }

    private void validateVoucher(Map<String, Object> payload, boolean strict, SafetyCheckResult result) {
        BigDecimal value = decimalValue(payload.get("giaTri"));
        String unit = stringValue(payload.get("donViGiam")).toUpperCase(Locale.ROOT);
        if (stringValue(payload.get("tenKhuyenMai")).isBlank()) {
            result.addError("Thiếu tên khuyến mãi.");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Giá trị giảm phải lớn hơn 0.");
        }
        if ("PERCENT".equals(unit) && value.compareTo(BigDecimal.valueOf(50)) > 0) {
            result.addWarning("Mức giảm vượt 50%. Nên kiểm tra lại trước khi tạo.");
        }
        if ("VND".equals(unit) && value.compareTo(BigDecimal.valueOf(500000)) > 0) {
            result.addWarning("Mức giảm tiền mặt khá lớn. Nên rà lại điều kiện áp dụng.");
        }
        if (strict && stringValue(payload.get("trangThai")).isBlank()) {
            result.addError("Thiếu trạng thái khuyến mãi.");
        }
    }

    private void validateProduct(Map<String, Object> payload, boolean strict, SafetyCheckResult result) {
        String productName = stringValue(payload.get("tenSanPham"));
        if (productName.isBlank()) {
            result.addError("Thiếu tên sản phẩm.");
        }
        if (productName.length() < 4) {
            result.addWarning("Tên sản phẩm khá ngắn. Nên kiểm tra lại để tránh tạo sai.");
        }
        if (strict && stringValue(payload.get("trangThai")).isBlank()) {
            result.addError("Thiếu trạng thái sản phẩm.");
        }
    }

    private void validateCreateOrder(Map<String, Object> payload, boolean strict, SafetyCheckResult result) {
        if (digitsOnly(payload.get("customerPhone")).length() < 10) {
            result.addError("Số điện thoại khách chưa hợp lệ.");
        }
        if (stringValue(payload.get("shippingAddress")).isBlank()) {
            result.addError("Thiếu địa chỉ giao hàng.");
        }
        if (stringValue(payload.get("productHint")).isBlank()) {
            result.addError("Thiếu sản phẩm cần lên đơn.");
        }
        int quantity = intValue(payload.get("quantity"));
        if (quantity <= 0) {
            result.addError("Số lượng phải lớn hơn 0.");
        }
        if (quantity >= 10) {
            result.addWarning("Đơn có số lượng lớn. Nên xác minh tồn kho trước khi tạo.");
        }
        if (strict) {
            result.addWarning("Luồng tạo đơn vẫn nên qua workflow nghiệp vụ để tránh sai lệch tồn và thanh toán.");
        }
    }

    private void validateUpdateOrderStatus(Map<String, Object> payload, boolean strict, SafetyCheckResult result) {
        if (stringValue(payload.get("orderCode")).isBlank()) {
            result.addError("Thiếu mã đơn cần cập nhật.");
        }
        String status = stringValue(payload.get("targetStatus"));
        if (status.isBlank()) {
            result.addError("Thiếu trạng thái đích.");
        }
        if (containsAny(status, "huy", "hủy", "refund", "hoan tien", "hoàn tiền")) {
            result.addWarning("Trạng thái nhạy cảm. Nên yêu cầu người phụ trách xác nhận ngoài bot.");
        }
        if (strict) {
            result.addWarning("Luồng đổi trạng thái đang được giữ ở chế độ handoff an toàn.");
        }
    }

    private boolean containsAny(String text, String... keywords) {
        String normalized = stringValue(text).toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (!keyword.isBlank() && normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String digitsOnly(Object value) {
        return stringValue(value).replaceAll("\\D+", "");
    }

    private int intValue(Object value) {
        try {
            return Integer.parseInt(stringValue(value));
        } catch (Exception ex) {
            return 0;
        }
    }

    private BigDecimal decimalValue(Object value) {
        try {
            return value instanceof BigDecimal bd ? bd : new BigDecimal(stringValue(value));
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    public static class SafetyCheckResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public boolean isAllowed() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("allowed", isAllowed());
            map.put("errors", new ArrayList<>(errors));
            map.put("warnings", new ArrayList<>(warnings));
            return map;
        }

        public void addError(String value) {
            if (value != null && !value.isBlank() && !errors.contains(value)) {
                errors.add(value);
            }
        }

        public void addWarning(String value) {
            if (value != null && !value.isBlank() && !warnings.contains(value)) {
                warnings.add(value);
            }
        }
    }
}
