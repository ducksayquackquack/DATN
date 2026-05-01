package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderStatusSummaryTool implements AssistantTool {

    private final JdbcTemplate jdbcTemplate;

    public OrderStatusSummaryTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String getName() {
        return "get_order_status_summary";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return true;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        String sql = """
            SELECT trangThai, COUNT(*) AS total
            FROM HoaDon
            WHERE trangThai IS NOT NULL
            GROUP BY trangThai
            """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        int cancelledCount = 0;
        int processingCount = 0;
        int completedCount = 0;

        for (Map<String, Object> row : rows) {
            String status = String.valueOf(row.get("trangThai"));
            int total = ((Number) row.get("total")).intValue();

            if (equalsAny(status, "Đã hủy", "Da huy")) {
                cancelledCount += total;
            }
            if (equalsAny(status, "Chờ xử lý", "Cho xu ly")) {
                processingCount += total;
            }
            if (equalsAny(status, "Hoàn thành", "Hoan thanh")) {
                completedCount += total;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("cancelledCount", cancelledCount);
        result.put("processingCount", processingCount);
        result.put("completedCount", completedCount);
        result.put("items", rows);
        return result;
    }

    private boolean equalsAny(String value, String... candidates) {
        if (value == null) return false;
        for (String candidate : candidates) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }
}