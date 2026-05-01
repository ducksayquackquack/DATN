package org.example.datnnhom03.assistant.service;

import org.example.datnnhom03.assistant.dto.AssistantChatRequest;
import org.example.datnnhom03.assistant.model.AssistantIntent;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AssistantSlotFillingService {

    public void enrichRequestContext(AssistantChatRequest request, AssistantIntent intent) {
        if (request == null) return;
        if (request.getContext() == null) {
            request.setContext(new LinkedHashMap<>());
        }
        Map<String, Object> context = request.getContext();
        String message = normalize(request.getMessage());

        putIfAbsent(context, "orderCode", extractOrderCode(message));
        putIfPresent(context, "customerPhone", extractPhoneNumber(message));
        putIfAbsent(context, "discountValue", extractDiscountValue(message));

        String explicitQuantity = extractQuantity(message);
        if (!explicitQuantity.isBlank()) {
            context.put("quantity", explicitQuantity);
        }

        putIfAbsent(context, "targetStatus", extractTargetStatus(message));
        putIfPresent(context, "customerName", extractCustomerName(request.getMessage(), intent));
        putIfPresent(context, "shippingAddress", extractShippingAddress(request.getMessage()));
        putIfPresent(context, "productHint", extractProductHint(request.getMessage(), intent));
        putIfPresent(context, "productName", extractProductName(request.getMessage(), intent));
        putIfPresent(context, "size", extractSize(message));

        Integer heightCm = extractHeightCm(message);
        Integer weightKg = extractWeightKg(message);
        if (heightCm != null) putIfPresent(context, "heightCm", heightCm);
        if (weightKg != null) putIfPresent(context, "weightKg", weightKg);
    }

    public List<String> findMissingSlots(AssistantIntent intent, AssistantChatRequest request) {
        Map<String, Object> context = request.getContext() == null ? Map.of() : request.getContext();
        String message = normalize(request.getMessage());
        List<String> missing = new ArrayList<>();

        if (intent == AssistantIntent.ORDER_LOOKUP) {
            if (extractOrderCode(message).isBlank() && digitsOnly(stringValue(context.get("customerPhone"))).isBlank()) {
                missing.add("orderCodeOrPhone");
            }
        }

        if (intent == AssistantIntent.CUSTOMER_PROFILE) {
            boolean hasIdentity = !digitsOnly(stringValue(context.get("customerPhone"))).isBlank()
                    || !stringValue(context.get("customerEmail")).isBlank()
                    || !stringValue(context.get("customerName")).isBlank();
            if (!hasIdentity && !looksLikeCustomerLookup(message)) {
                missing.add("customerIdentity");
            }
        }

        if (intent == AssistantIntent.SIZE_ADVICE) {
            Integer heightCm = extractHeightCm(message);
            Integer weightKg = extractWeightKg(message);
            if (heightCm == null && intValue(context.get("heightCm")) <= 0) missing.add("heightCm");
            if (weightKg == null && intValue(context.get("weightKg")) <= 0) missing.add("weightKg");
        }

        if (intent == AssistantIntent.CUSTOMER_REPLY_DRAFT) {
            if (stringValue(context.get("lastCustomerMessage")).isBlank() && request.getMessage() != null && request.getMessage().trim().length() < 8) {
                missing.add("lastCustomerMessage");
            }
        }

        if (intent == AssistantIntent.CREATE_ORDER) {
            if (digitsOnly(stringValue(context.get("customerPhone"))).length() < 10) missing.add("customerPhone");
            if (stringValue(context.get("shippingAddress")).isBlank()) missing.add("shippingAddress");
            if (stringValue(context.get("productHint")).isBlank()) missing.add("productHint");
            if (intValue(context.get("quantity")) <= 0) missing.add("quantity");
        }

        if (intent == AssistantIntent.UPDATE_ORDER_STATUS) {
            if (extractOrderCode(message).isBlank() && stringValue(context.get("orderCode")).isBlank()) missing.add("orderCode");
            if (!looksLikeStatusChange(message) && stringValue(context.get("targetStatus")).isBlank()) missing.add("targetStatus");
        }

        if (intent == AssistantIntent.CREATE_VOUCHER) {
            if (!containsDiscountSignal(message) && stringValue(context.get("discountValue")).isBlank()) missing.add("discountValue");
        }

        if (intent == AssistantIntent.PRODUCT_UPSERT) {
            if (stringValue(context.get("productName")).isBlank() && request.getMessage() != null && request.getMessage().trim().length() < 10) {
                missing.add("productName");
            }
        }

        return missing;
    }

    public String buildMissingSlotQuestion(AssistantIntent intent, List<String> missingSlots) {
        if (missingSlots.contains("orderCodeOrPhone")) {
            return "Để tra cứu đúng đơn, anh/chị gửi giúp em mã hóa đơn hoặc số điện thoại nhận hàng.";
        }
        if (missingSlots.contains("customerIdentity")) {
            return "Để mở hồ sơ khách chính xác, anh/chị cho em số điện thoại, email hoặc tên khách cần kiểm tra.";
        }
        if (missingSlots.contains("heightCm") || missingSlots.contains("weightKg")) {
            return "Để gợi ý size sát hơn, anh/chị cho em chiều cao và cân nặng của khách.";
        }
        if (missingSlots.contains("lastCustomerMessage")) {
            return "Anh/chị dán giúp em tin nhắn gần nhất của khách để em soạn câu trả lời sát ngữ cảnh hơn.";
        }
        if (missingSlots.contains("customerPhone") || missingSlots.contains("shippingAddress") || missingSlots.contains("productHint") || missingSlots.contains("quantity")) {
            return "Để lên đơn nháp, anh/chị gửi giúp em số điện thoại khách, địa chỉ nhận hàng, sản phẩm cần mua và số lượng.";
        }
        if (missingSlots.contains("orderCode") || missingSlots.contains("targetStatus")) {
            return "Để chuẩn bị đổi trạng thái, anh/chị cho em mã đơn và trạng thái đích muốn chuyển.";
        }
        if (missingSlots.contains("discountValue")) {
            return "Anh/chị cho em mức giảm muốn tạo, ví dụ 10% hoặc 50000đ.";
        }
        if (missingSlots.contains("productName")) {
            return "Anh/chị cho em tên sản phẩm cần tạo hoặc cập nhật.";
        }
        return "Anh/chị cho em thêm thông tin để em xử lý chính xác hơn.";
    }

    private void putIfAbsent(Map<String, Object> context, String key, Object value) {
        if (context == null || key == null || key.isBlank() || value == null) return;
        String current = stringValue(context.get(key));
        String incoming = stringValue(value);
        if (current.isBlank() && !incoming.isBlank()) {
            context.put(key, value);
        }
    }

    private void putIfPresent(Map<String, Object> context, String key, Object value) {
        if (context == null || key == null || key.isBlank() || value == null) return;
        String incoming = stringValue(value);
        if (!incoming.isBlank()) {
            context.put(key, value);
        }
    }

    private boolean looksLikeCustomerLookup(String message) {
        return message.contains("khách") || message.contains("sdt") || message.contains("email") || message.contains("so dien thoai");
    }

    private boolean looksLikeStatusChange(String message) {
        return message.contains("trạng thái") || message.contains("trang thai") || message.contains("đang giao") || message.contains("da giao") || message.contains("đã giao");
    }

    private boolean containsDiscountSignal(String message) {
        return message.contains("%") || message.contains("k") || message.contains("đ") || message.contains("vnd") || message.contains("giam") || message.contains("giảm");
    }

    private String extractOrderCode(String text) {
        Matcher matcher = Pattern.compile("\\bhd[0-9a-z-]+\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : "";
    }

    private String extractPhoneNumber(String text) {
        Matcher matcher = Pattern.compile("(?:0|84)?\\d{9,10}").matcher(text.replaceAll("[.\\s-]+", ""));
        return matcher.find() ? matcher.group() : "";
    }

    private String extractDiscountValue(String normalized) {
        Matcher matcher = Pattern.compile("(\\d{1,3})(\\s*%|\\s*k|\\s*000|\\s*đ|\\s*vnd)?", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (!matcher.find()) return "";
        String raw = matcher.group(1);
        String unit = matcher.group(2) == null ? "" : matcher.group(2).toLowerCase(Locale.ROOT);
        if (unit.contains("k") || unit.contains("000")) {
            return String.valueOf(Integer.parseInt(raw) * 1000);
        }
        return raw;
    }

    private String extractQuantity(String normalized) {
        Matcher matcher = Pattern.compile("(?:x|sl|so luong|số lượng)\\s*[:=]?\\s*(\\d{1,3})", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (matcher.find()) return matcher.group(1);
        Matcher prefixed = Pattern.compile("(?:co|có|gom|gồm)\\s+(\\d{1,3})\\s+(?:sp|san pham|sản phẩm|chi[eế]c|cai|cái|ao|áo|quan|quần|bomber|hoodie|coach)", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (prefixed.find()) return prefixed.group(1);
        Matcher nounBased = Pattern.compile("\\b(\\d{1,3})\\s+(?:sp|san pham|sản phẩm|chi[eế]c|cai|cái|ao|áo|quan|quần|bomber|hoodie|coach)\\b", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (nounBased.find()) return nounBased.group(1);
        return "";
    }

    private String extractTargetStatus(String normalized) {
        if (containsAny(normalized, "dang giao", "đang giao")) return "Đang giao";
        if (containsAny(normalized, "da giao", "đã giao")) return "Đã giao";
        if (containsAny(normalized, "cho xac nhan", "chờ xác nhận")) return "Chờ xác nhận";
        if (containsAny(normalized, "da xac nhan", "đã xác nhận")) return "Đã xác nhận";
        if (containsAny(normalized, "huy", "hủy")) return "Hủy";
        return "";
    }

    private String extractShippingAddress(String original) {
        String text = original == null ? "" : original.trim();
        Matcher matcher = Pattern.compile("(?:giao về|giao toi|giao tới|địa chỉ|dia chi)\\s*[:\\-]?\\s*(.+)", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String extractCustomerName(String original, AssistantIntent intent) {
        if (intent != AssistantIntent.CUSTOMER_PROFILE && intent != AssistantIntent.CREATE_ORDER) {
            return "";
        }
        String text = original == null ? "" : original.trim();
        Matcher createOrderMatcher = Pattern.compile("kh(?:á|a)ch\\s+h(?:à|a)ng\\s+([^,;:.]+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (createOrderMatcher.find()) {
            return createOrderMatcher.group(1).trim();
        }
        Matcher profileMatcher = Pattern.compile("(?:hồ sơ khách hàng|ho so khach hang|tóm tắt hồ sơ khách hàng|tom tat ho so khach hang|khách hàng)\\s+([^?.,:;]+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (profileMatcher.find()) {
            String candidate = profileMatcher.group(1).trim();
            if (!candidate.equalsIgnoreCase("này") && !candidate.equalsIgnoreCase("nay")) {
                return candidate;
            }
        }
        return "";
    }

    private String extractProductHint(String original, AssistantIntent intent) {
        if (intent != AssistantIntent.CREATE_ORDER && intent != AssistantIntent.PRODUCT_STOCK && intent != AssistantIntent.PRODUCT_SEARCH) {
            return "";
        }
        String text = original == null ? "" : original.trim();
        Matcher codeMatcher = Pattern.compile("\\bSP\\d{3,}\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        if (codeMatcher.find()) {
            return codeMatcher.group().toUpperCase(Locale.ROOT);
        }
        Matcher matcher = Pattern.compile("(?:mua|lấy|lay|them|thêm|tao don|tạo đơn|can|cần|co|có)\\s+(.+?)(?:\\s+(?:x|sl|so luong|số lượng)\\s*\\d+|$)", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String extractProductName(String original, AssistantIntent intent) {
        if (intent != AssistantIntent.PRODUCT_UPSERT) {
            return "";
        }
        String text = original == null ? "" : original.trim();
        Matcher matcher = Pattern.compile("(?:san pham|sản phẩm)\\s+(.+)", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String extractSize(String normalized) {
        Matcher matcher = Pattern.compile("\\b(xx?l|3xl|4xl|5xl|xs|s|m|l|xl)\\b", Pattern.CASE_INSENSITIVE).matcher(normalized);
        return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : "";
    }

    private Integer extractHeightCm(String normalized) {
        Matcher keyValueMatcher = Pattern.compile("(?:heightcm|height|chi[eề]u cao)\\s*[:=]?\\s*(1\\d{2})", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (keyValueMatcher.find()) return Integer.parseInt(keyValueMatcher.group(1));
        Matcher cmMatcher = Pattern.compile("(1\\d{2})\\s*cm", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (cmMatcher.find()) return Integer.parseInt(cmMatcher.group(1));
        Matcher meterMatcher = Pattern.compile("1\\s*m\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (meterMatcher.find()) return 100 + Integer.parseInt(meterMatcher.group(1));
        return null;
    }

    private Integer extractWeightKg(String normalized) {
        Matcher keyValueMatcher = Pattern.compile("(?:weightkg|weight|c[aâ]n n[aặ]ng)\\s*[:=]?\\s*(\\d{2,3})", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (keyValueMatcher.find()) return Integer.parseInt(keyValueMatcher.group(1));
        Matcher matcher = Pattern.compile("(\\d{2,3})\\s*kg", Pattern.CASE_INSENSITIVE).matcher(normalized);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private int intValue(Object value) {
        try {
            return Integer.parseInt(stringValue(value));
        } catch (Exception ex) {
            return 0;
        }
    }

    private boolean containsAny(String text, String... keywords) {
        String normalized = normalize(text);
        for (String keyword : keywords) {
            if (!normalize(keyword).isBlank() && normalized.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D+", "");
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
