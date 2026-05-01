package org.example.datnnhom03.assistant.handler.impl;

import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.handler.AssistantRequestContext;
import org.example.datnnhom03.assistant.service.ToolDispatcher;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractToolIntentHandler {
    protected final ToolDispatcher toolDispatcher;

    protected AbstractToolIntentHandler(ToolDispatcher toolDispatcher) {
        this.toolDispatcher = toolDispatcher;
    }

    protected Map<String, Object> call(AssistantRequestContext context, String toolName, Map<String, Object> args, AssistantHandlerResult result) {
        Map<String, Object> data = toolDispatcher.dispatch(toolName, context.getRole(), args);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("tool", toolName);
        record.put("args", args);
        record.put("resultPreview", preview(data));
        result.getToolCalls().add(record);
        return data;
    }

    protected String preview(Map<String, Object> data) {
        if (data == null || data.isEmpty()) return "";
        if (data.containsKey("summary")) return String.valueOf(data.get("summary"));
        if (data.containsKey("count")) return "count=" + data.get("count");
        if (data.containsKey("totalRevenue")) return String.valueOf(data.get("totalRevenue"));
        return data.keySet().toString();
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> items(Map<String, Object> data) {
        Object value = data == null ? null : data.get("items");
        if (value instanceof List<?> list) {
            return list.stream().filter(Map.class::isInstance).map(v -> (Map<String, Object>) v).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> mapList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().filter(Map.class::isInstance).map(v -> (Map<String, Object>) v).toList();
        }
        return List.of();
    }

    protected String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    protected String digitsOnly(Object value) {
        return stringValue(value).replaceAll("\\D+", "");
    }

    protected String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    protected String extractOrderCode(String text) {
        Matcher matcher = Pattern.compile("\\bhd[0-9a-z-]+\\b", Pattern.CASE_INSENSITIVE).matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : "";
    }

    protected String extractProductCode(String text) {
        Matcher matcher = Pattern.compile("\\bsp[0-9a-z-]+\\b", Pattern.CASE_INSENSITIVE).matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : "";
    }

    protected String extractVariantCode(String text) {
        Matcher matcher = Pattern.compile("\\bspct[0-9a-z-]+\\b", Pattern.CASE_INSENSITIVE).matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : "";
    }

    protected String extractSize(String normalized) {
        Matcher matcher = Pattern.compile("\\b(xx?l|3xl|4xl|5xl|xs|s|m|l|xl)\\b", Pattern.CASE_INSENSITIVE).matcher(normalized == null ? "" : normalized);
        return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : "";
    }

    protected String extractColor(String normalized) {
        if (containsAny(normalized, "đen", "den")) return "đen";
        if (containsAny(normalized, "trắng", "trang")) return "trắng";
        if (containsAny(normalized, "xanh", "navy")) return "xanh";
        if (containsAny(normalized, "xám", "xam")) return "xám";
        if (containsAny(normalized, "đỏ", "do")) return "đỏ";
        return "";
    }

    protected String extractCategory(String normalized) {
        if (containsAny(normalized, "hoodie")) return "hoodie";
        if (containsAny(normalized, "bomber")) return "bomber";
        if (containsAny(normalized, "coach")) return "coach";
        if (containsAny(normalized, "áo khoác", "ao khoac", "jacket")) return "áo khoác";
        return "";
    }

    protected Integer extractHeightCm(String normalized) {
        Matcher cmMatcher = Pattern.compile("(1\\d{2})\\s*cm", Pattern.CASE_INSENSITIVE).matcher(normalized == null ? "" : normalized);
        if (cmMatcher.find()) return Integer.parseInt(cmMatcher.group(1));
        Matcher meterMatcher = Pattern.compile("1\\s*m\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE).matcher(normalized == null ? "" : normalized);
        if (meterMatcher.find()) return 100 + Integer.parseInt(meterMatcher.group(1));
        return null;
    }

    protected Integer extractWeightKg(String normalized) {
        Matcher matcher = Pattern.compile("(\\d{2,3})\\s*kg", Pattern.CASE_INSENSITIVE).matcher(normalized == null ? "" : normalized);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    protected boolean containsAny(String text, String... keywords) {
        String haystack = normalize(text);
        for (String keyword : keywords) {
            if (!normalize(keyword).isBlank() && haystack.contains(normalize(keyword))) return true;
        }
        return false;
    }

    protected String money(Object value) {
        try {
            Number number = value instanceof Number ? (Number) value : Double.parseDouble(String.valueOf(value));
            NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            formatter.setMaximumFractionDigits(0);
            return formatter.format(number) + " đ";
        } catch (Exception ex) {
            return stringValue(value);
        }
    }

    protected Map<String, Object> currentMonthRange(int limit) {
        LocalDate now = LocalDate.now();
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("from", now.withDayOfMonth(1).toString());
        args.put("to", now.toString());
        if (limit > 0) args.put("limit", limit);
        return args;
    }
}
