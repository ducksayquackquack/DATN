package org.example.datnnhom03.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.datnnhom03.assistant.service.OpenAiGateway;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CustomerSemanticRouterService {

    private static final Set<String> ALLOWED_INTENTS = Set.of(
            "GREETING",
            "ORDER_LOOKUP",
            "PRODUCT_STOCK",
            "PRODUCT_RECOMMENDATION",
            "PRODUCT_SEARCH",
            "BEST_SELLER_QUERY",
            "PROMOTION_QUERY",
            "SIZE_ADVICE",
            "PAYMENT_QUERY",
            "POLICY_QUERY",
            "SHOP_INFO_QUERY",
            "SHIPPING_QUERY",
            "UNKNOWN"
    );

    private final OpenAiGateway openAiGateway;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomerSemanticRouterService(OpenAiGateway openAiGateway) {
        this.openAiGateway = openAiGateway;
    }

    public CustomerIntentResult route(String message) {
        String normalized = normalize(message);
        CustomerIntentResult heuristic = heuristicRoute(message, normalized);
        if (!openAiGateway.isReady()) {
            return heuristic;
        }

        CustomerIntentResult structured = tryStructuredRoute(message);
        if (structured == null || structured.getIntent() == null || !ALLOWED_INTENTS.contains(structured.getIntent())) {
            return heuristic;
        }

        Map<String, Object> mergedEntities = new LinkedHashMap<>(heuristic.getEntities());
        mergedEntities.putAll(structured.getEntities());
        structured.setEntities(mergedEntities);

        if ("UNKNOWN".equals(structured.getIntent()) && !"UNKNOWN".equals(heuristic.getIntent())) {
            structured.setIntent(heuristic.getIntent());
        }
        if (structured.getConfidence() == null || structured.getConfidence().isBlank()) {
            structured.setConfidence(heuristic.getConfidence());
        }
        if (structured.getMissingFields().isEmpty()) {
            structured.setMissingFields(heuristic.getMissingFields());
        }
        if (!structured.isClarificationNeeded() && heuristic.isClarificationNeeded()) {
            structured.setClarificationNeeded(true);
        }
        return structured;
    }

    private CustomerIntentResult tryStructuredRoute(String message) {
        try {
            String response = openAiGateway.askStructured(
                    """
                    Bạn là bộ phân loại intent cho chatbot bán hàng DirtyWave.
                    Hãy trả về JSON hợp lệ duy nhất, không có markdown, theo schema:
                    {
                      \"intent\": \"...\",
                      \"confidence\": \"HIGH|MEDIUM|LOW\",
                      \"clarificationNeeded\": true/false,
                      \"missingFields\": [\"...\"],
                      \"entities\": {
                        \"orderCode\": \"\",
                        \"productCode\": \"\",
                        \"variantCode\": \"\",
                        \"category\": \"\",
                        \"color\": \"\",
                        \"size\": \"\",
                        \"maxPrice\": 0,
                        \"requestedCount\": 0,
                        \"heightCm\": 0,
                        \"weightKg\": 0,
                        \"purpose\": \"\",
                        \"style\": \"\"
                      }
                    }

                    Danh sách intent hợp lệ:
                    GREETING, ORDER_LOOKUP, PRODUCT_STOCK, PRODUCT_RECOMMENDATION, PRODUCT_SEARCH,
                    BEST_SELLER_QUERY, PROMOTION_QUERY, SIZE_ADVICE, PAYMENT_QUERY, POLICY_QUERY,
                    SHOP_INFO_QUERY, SHIPPING_QUERY, UNKNOWN.

                    
                    Quy tắc:
            - Nếu câu có "khuyến mãi", "giảm giá", "sale", "voucher", "ưu đãi" thì map về PROMOTION_QUERY, kể cả khi câu có mã SP/SPCT.
            - Nếu người dùng hỏi kiểu tương đương nhau như "hot nhất", "best seller", "bán chạy" thì map về BEST_SELLER_QUERY.
            - Nếu hỏi "còn hàng", "có sẵn", "giao ngay", "tồn kho", mã SPCT/SP mà không nhắc khuyến mãi/giảm giá/sale/voucher thì map về PRODUCT_STOCK.
            - Nếu hỏi phí ship, tổng chi phí, tổng thanh toán của đơn hiện có hoặc nhắc mã HD thì ưu tiên map về ORDER_LOOKUP.
            - Nếu chỉ hỏi chung về giao hàng hoặc ship mà không gắn với đơn cụ thể thì map về SHIPPING_QUERY.
            - Nếu hỏi gợi ý mẫu, phối đồ, đi làm, mua nhanh, dễ bán thì map về PRODUCT_RECOMMENDATION hoặc PRODUCT_SEARCH.
            - Nếu không chắc hoặc thiếu dữ liệu, đặt clarificationNeeded=true và nêu missingFields.
            - Không bịa dữ liệu không có trong câu.
                    """,
                                        message == null ? "" : message
                                );
                                if (response == null || response.isBlank()) {
                                    return null;
                                }
                                Map<String, Object> raw = objectMapper.readValue(response, new TypeReference<>() {});
                                CustomerIntentResult result = new CustomerIntentResult();
                                result.setIntent(stringValue(raw.get("intent")).toUpperCase(Locale.ROOT));
                                result.setConfidence(stringValue(raw.get("confidence")).toUpperCase(Locale.ROOT));
                                result.setClarificationNeeded(Boolean.TRUE.equals(raw.get("clarificationNeeded")));
                                Object missingFields = raw.get("missingFields");
                                if (missingFields instanceof List<?> list) {
                                    result.setMissingFields(list.stream().map(String::valueOf).toList());
                                }
                                Object entities = raw.get("entities");
                                if (entities instanceof Map<?, ?> map) {
                                    Map<String, Object> entityMap = new LinkedHashMap<>();
                                    map.forEach((k, v) -> {
                                        if (k != null && v != null && !String.valueOf(v).isBlank() && !"0".equals(String.valueOf(v).trim())) {
                                            entityMap.put(String.valueOf(k), v);
                                        }
                                    });
                                    result.setEntities(entityMap);
                                }
                                return result;
                            } catch (Exception ignored) {
                                return null;
                            }
                        }

                        private CustomerIntentResult heuristicRoute(String message, String normalized) {
                            CustomerIntentResult result;

                            if (containsAny(normalized, "xin chao", "chao shop", "hello") || "hi".equals(normalized) || "hey".equals(normalized)) {
                                result = CustomerIntentResult.of("GREETING", "HIGH");

                            } else if (hasOrderSignal(normalized, message)) {
                                result = CustomerIntentResult.of("ORDER_LOOKUP", "HIGH");

                                // Khuyến mãi phải đứng trước tồn kho.
                                // Ví dụ: "SP001 có khuyến mãi k" có mã SP nhưng intent đúng là PROMOTION_QUERY, không phải PRODUCT_STOCK.
                            } else if (hasPromotionSignal(normalized)) {
                                result = CustomerIntentResult.of("PROMOTION_QUERY", "HIGH");

                            } else if (hasStockSignal(normalized, message)) {
                                result = CustomerIntentResult.of("PRODUCT_STOCK", "HIGH");

                            } else if (containsAny(normalized, "ban chay", "bán chạy", "best seller", "hot nhat", "hot nhất", "mau nao hot", "mẫu nào hot")) {
                                result = CustomerIntentResult.of("BEST_SELLER_QUERY", "HIGH");

                            } else if (containsAny(normalized, "thanh toan", "thanh toán", "cod", "pttt", "chuyen khoan", "chuyển khoản", "ví", "vi dien tu")) {
                                result = CustomerIntentResult.of("PAYMENT_QUERY", "HIGH");

                            } else if (containsAny(normalized, "doi tra", "đổi trả", "hoan tien", "hoàn tiền", "bao hanh", "bảo hành", "doi size", "đổi size", "chinh sach", "chính sách")) {
                                result = CustomerIntentResult.of("POLICY_QUERY", "HIGH");

                            } else if (containsAny(normalized, "cua hang", "cửa hàng", "offline", "gio lam", "giờ làm", "hotline", "lien he", "liên hệ", "chinh hang", "chính hãng", "hoa don", "hóa đơn")
                                    && !hasOrderSignal(normalized, message)) {
                                result = CustomerIntentResult.of("SHOP_INFO_QUERY", "MEDIUM");

                            } else if (containsAny(normalized, "giao hang", "giao hàng", "phi ship", "phí ship", "ship", "bao gio giao", "bao giờ giao")) {
                                result = CustomerIntentResult.of("SHIPPING_QUERY", "MEDIUM");

                            } else if (containsAny(normalized, "size", "kích thước", "kich thuoc", "cao", "nang", "nặng", "form rong", "form rộng", "mac size gi", "mặc size gì")
                                    || hasBodyMetricSignal(normalized)) {
                                result = CustomerIntentResult.of("SIZE_ADVICE", "HIGH");

                            } else if (containsAny(normalized, "goi y", "gợi ý", "phu hop", "phù hợp", "de phoi", "dễ phối", "di lam", "đi làm", "mua nhanh", "upsell", "tu van", "tư vấn")) {
                                result = CustomerIntentResult.of("PRODUCT_RECOMMENDATION", "MEDIUM");

                            } else if (containsAny(normalized, "hoodie", "bomber", "coach", "ao khoac", "áo khoác", "san pham", "sản phẩm", "ao", "mau den", "màu đen")
                                    || extractMaxPrice(normalized) != null
                                    || !extractColor(normalized).isBlank()) {
                                result = CustomerIntentResult.of("PRODUCT_SEARCH", "MEDIUM");

                            } else {
                                result = CustomerIntentResult.of("UNKNOWN", "LOW");
                            }

                            Map<String, Object> entities = extractEntities(message, normalized);
                            result.setEntities(entities);
                            postProcessClarification(result);
                            return result;
                        }

                        private void postProcessClarification(CustomerIntentResult result) {
                            Map<String, Object> entities = result.getEntities();
                            List<String> missing = new ArrayList<>();

                            if ("ORDER_LOOKUP".equals(result.getIntent())) {
                                boolean hasOrderCode = hasText(entities.get("orderCode"));
                                boolean hasPhone = hasText(entities.get("phone"));
                                if (!hasOrderCode && !hasPhone) {
                                    missing.add("orderCodeOrPhone");
                                }
                            }

                            if ("PRODUCT_STOCK".equals(result.getIntent())) {
                                boolean hasSpecificProduct = hasText(entities.get("variantCode"))
                                        || hasText(entities.get("productCode"))
                                        || hasText(entities.get("category"));
                                if (!hasSpecificProduct) {
                                    missing.add("product");
                                }
                            }

                            if ("SIZE_ADVICE".equals(result.getIntent())) {
                                boolean hasHeight = entities.get("heightCm") instanceof Number;
                                boolean hasWeight = entities.get("weightKg") instanceof Number;
                                if (!hasHeight) missing.add("heightCm");
                                if (!hasWeight) missing.add("weightKg");
                            }

                            result.setMissingFields(missing);
                            result.setClarificationNeeded(!missing.isEmpty() && !"LOW".equals(result.getConfidence()));
                        }

                        private Map<String, Object> extractEntities(String rawMessage, String normalized) {
                            Map<String, Object> entities = new LinkedHashMap<>();
                            putIfText(entities, "variantCode", match(rawMessage, "(?i)\\bSPCT\\d+\\b"));
                            putIfText(entities, "productCode", match(rawMessage, "(?i)\\bSP\\d+\\b"));
                            putIfText(entities, "orderCode", match(rawMessage, "(?i)\\bHD[0-9A-Z]+\\b"));
                            putIfText(entities, "phone", match(rawMessage, "(?<!\\d)(0\\d{8,10})(?!\\d)"));

                            String category = extractCategory(normalized);
                            putIfText(entities, "category", category);
                            String color = extractColor(normalized);
                            putIfText(entities, "color", color);
                            String size = extractSize(normalized);
                            putIfText(entities, "size", size);
                            Integer maxPrice = extractMaxPrice(normalized);
                            if (maxPrice != null) entities.put("maxPrice", maxPrice);
                            Integer requestedCount = extractRequestedCount(normalized);
                            if (requestedCount != null) entities.put("requestedCount", requestedCount);
                            Integer weightKg = extractWeightKg(normalized);
                            if (weightKg != null) entities.put("weightKg", weightKg);
                            Integer heightCm = extractHeightCm(normalized);
                            if (heightCm != null) entities.put("heightCm", heightCm);

                            if (containsAny(normalized, "di lam", "đi làm")) entities.put("purpose", "đi làm");
                            if (containsAny(normalized, "de phoi", "dễ phối")) entities.put("style", "dễ phối");
                            if (containsAny(normalized, "mua nhanh")) entities.put("purpose", "mua nhanh");
                            return entities;
                        }

                        private boolean hasOrderSignal(String normalized, String rawMessage) {
                            return match(rawMessage, "(?i)\\bHD[0-9A-Z]+\\b") != null
                                    || match(rawMessage, "(?<!\\d)(0\\d{8,10})(?!\\d)") != null
                                    || containsAny(normalized,
                                    "don hang", "đơn hàng",
                                    "kiem tra don", "kiểm tra đơn",
                                    "hoa don", "hóa đơn",
                                    "trang thai don", "trạng thái đơn",
                                    "tong chi phi", "tổng chi phí",
                                    "tong thanh toan", "tổng thanh toán",
                                    "tong cong", "tổng cộng",
                                    "tong don", "tổng đơn",
                                    "so dien thoai dat hang", "số điện thoại đặt hàng",
                                    "sdt dat hang", "sdt đặt hàng",
                                    "so dien thoai", "số điện thoại",
                                    "sdt");
                        }

                        private boolean hasStockSignal(String normalized, String rawMessage) {
                            return match(rawMessage, "(?i)\\bSPCT\\d+\\b") != null
                                    || match(rawMessage, "(?i)\\bSP\\d+\\b") != null
                                    || containsAny(normalized, "con hang", "còn hàng", "co san", "có sẵn", "giao ngay", "ton kho", "tồn kho");
                        }

                        private Integer extractRequestedCount(String normalized) {
                            Matcher matcher = Pattern.compile("(?:goi y|gợi ý|chon|chọn)\\s*(\\d{1,2})").matcher(normalized);
                            if (matcher.find()) {
                                return parseInt(matcher.group(1));
                            }
                            return null;
                        }

                        private Integer extractMaxPrice(String normalized) {
                            Matcher matcher = Pattern.compile("(\\d{2,4})\\s*k").matcher(normalized);
                            if (matcher.find() && containsAny(normalized, "duoi", "dưới", "toi da", "tối đa", "re hon", "rẻ hơn")) {
                                return parseInt(matcher.group(1)) * 1000;
                            }
                            return null;
                        }

                        private Integer extractWeightKg(String normalized) {
                            Matcher matcher = Pattern.compile("(\\d{2,3})\\s*kg").matcher(normalized);
                            if (matcher.find()) {
                                return parseInt(matcher.group(1));
                            }
                            return null;
                        }

                        private Integer extractHeightCm(String normalized) {
                            Matcher fullCm = Pattern.compile("(1\\d{2})\\s*cm").matcher(normalized);
                            if (fullCm.find()) {
                                return parseInt(fullCm.group(1));
                            }

                            Matcher meter = Pattern.compile("1\\s*[mM]\\s*(\\d{1,2})").matcher(normalized);
                            if (meter.find()) {
                                int extra = parseInt(meter.group(1));
                                return 100 + extra;
                            }

                            Matcher compactMeter = Pattern.compile("1[.,](\\d{2})").matcher(normalized);
                            if (compactMeter.find()) {
                                return 100 + parseInt(compactMeter.group(1));
                            }
                            return null;
                        }

                        private String extractCategory(String normalized) {
                            if (containsAny(normalized, "bomber")) return "bomber";
                            if (containsAny(normalized, "hoodie")) return "hoodie";
                            if (containsAny(normalized, "coach")) return "coach";
                            if (containsAny(normalized, "ao khoac", "áo khoác", "jacket")) return "áo khoác";
                            return "";
                        }

                        private String extractColor(String normalized) {
                            if (containsAny(normalized, "den", "đen")) return "Đen";
                            if (containsAny(normalized, "trang", "trắng")) return "Trắng";
                            if (containsAny(normalized, "xam", "xám")) return "Xám";
                            if (containsAny(normalized, "xanh", "navy")) return "Xanh";
                            if (containsAny(normalized, "nau", "nâu")) return "Nâu";
                            if (containsAny(normalized, "be")) return "Be";
                            if (containsAny(normalized, "do", "đỏ")) return "Đỏ";
                            return "";
                        }

                        private String extractSize(String normalized) {
                            Matcher matcher = Pattern.compile("\\b(xx?l|3xl|4xl|5xl|xs|s|m|l|xl)\\b", Pattern.CASE_INSENSITIVE).matcher(normalized);
                            return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : "";
                        }

                        private boolean containsAny(String text, String... keywords) {
                            if (text == null || text.isBlank()) return false;
                            for (String keyword : keywords) {
                                if (keyword != null && !keyword.isBlank() && text.contains(normalize(keyword))) {
                                    return true;
                                }
                            }
                            return false;
                        }

                        private void putIfText(Map<String, Object> map, String key, String value) {
                            if (value != null && !value.isBlank()) {
                                map.put(key, value.trim());
                            }
                        }

                        private String match(String text, String regex) {
                            Matcher matcher = Pattern.compile(regex).matcher(text == null ? "" : text);
                            return matcher.find() ? matcher.group().trim() : null;
                        }

                        private boolean hasText(Object value) {
                            return value != null && !String.valueOf(value).isBlank();
                        }

                        private int parseInt(String raw) {
                            try {
                                return Integer.parseInt(raw);
                            } catch (Exception e) {
                                return 0;
                            }
                        }

                        private String stringValue(Object value) {
                            return value == null ? "" : String.valueOf(value).trim();
                        }

                        private String normalize(String text) {
                            if (text == null) return "";
                            String s = text.toLowerCase(Locale.ROOT).trim();
                            s = s.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
                            s = s.replaceAll("[èéẹẻẽêềếệểễ]", "e");
                            s = s.replaceAll("[ìíịỉĩ]", "i");
                            s = s.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
                            s = s.replaceAll("[ùúụủũưừứựửữ]", "u");
                            s = s.replaceAll("[ỳýỵỷỹ]", "y");
                            s = s.replaceAll("[đ]", "d");
                            return s;
                        }

                        private boolean hasBodyMetricSignal(String normalized) {
                            return Pattern.compile("\\b1\\s*m\\s*\\d{1,2}\\b").matcher(normalized).find()
                                    || Pattern.compile("\\b1[\\.,]\\d{2}\\b").matcher(normalized).find()
                                    || Pattern.compile("\\b1\\d{2}\\s*cm\\b").matcher(normalized).find()
                                    || Pattern.compile("\\b\\d{2,3}\\s*kg\\b").matcher(normalized).find();
                        }

                        private boolean hasPromotionSignal(String normalized) {
                            return containsAny(normalized,
                                    "voucher",
                                    "khuyen mai",
                                    "khuyến mãi",
                                    "co khuyen mai",
                                    "có khuyến mãi",
                                    "khuyen mai khong",
                                    "khuyến mãi không",
                                    "khuyen mai k",
                                    "khuyến mãi k",
                                    "sale",
                                    "uu dai",
                                    "ưu đãi",
                                    "giam gia",
                                    "giảm giá");
                        }
                    }
