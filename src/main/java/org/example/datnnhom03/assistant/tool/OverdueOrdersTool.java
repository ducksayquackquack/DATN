package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Repository.HoaDonRepository;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OverdueOrdersTool implements AssistantTool {

    private final HoaDonRepository hoaDonRepository;

    public OverdueOrdersTool(HoaDonRepository hoaDonRepository) {
        this.hoaDonRepository = hoaDonRepository;
    }

    @Override
    public String getName() {
        return "get_overdue_orders";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return true;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        int limit = parseInt(args.get("limit"), 10);
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> items = hoaDonRepository.findAll().stream()
                .filter(order -> order.getNgayNhanHangDuKien() != null)
                .filter(order -> order.getNgayNhanHangDuKien().isBefore(today))
                .filter(order -> !isClosedStatus(order.getTrangThai()))
                .sorted(Comparator
                        .comparing(HoaDon::getNgayNhanHangDuKien, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(HoaDon::getNgayTao, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(Math.max(limit, 1))
                .map(order -> toRow(order, today))
                .collect(Collectors.toList());

        long criticalCount = items.stream()
                .filter(item -> parseInt(item.get("daysLate"), 0) >= 3)
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", items.size());
        result.put("criticalCount", criticalCount);
        result.put("today", today.toString());
        result.put("items", items);
        result.put("summary", buildSummary(items, criticalCount));
        return result;
    }

    private Map<String, Object> toRow(HoaDon order, LocalDate today) {
        Map<String, Object> row = new LinkedHashMap<>();
        LocalDate expectedDate = order.getNgayNhanHangDuKien();
        int daysLate = expectedDate == null ? 0 : (int) (today.toEpochDay() - expectedDate.toEpochDay());
        row.put("id", order.getId());
        row.put("maHoaDon", order.getMaHoaDon());
        row.put("tenKhachHang", order.getTenKhachHang());
        row.put("soDienThoaiNhanHang", order.getSoDienThoaiNhanHang());
        row.put("trangThai", safe(order.getTrangThai()));
        row.put("ngayNhanHangDuKien", expectedDate == null ? null : expectedDate.toString());
        row.put("daysLate", daysLate);
        row.put("thanhTien", order.getThanhTien());
        return row;
    }

    private String buildSummary(List<Map<String, Object>> items, long criticalCount) {
        if (items.isEmpty()) {
            return "Hiện chưa có đơn trễ theo ngày nhận hàng dự kiến.";
        }

        Map<String, Object> first = items.get(0);
        return "Hiện có " + items.size() + " đơn trễ cần theo dõi"
                + (criticalCount > 0 ? ", trong đó " + criticalCount + " đơn trễ từ 3 ngày trở lên" : "")
                + ". Đơn cần ưu tiên: " + safe(first.get("maHoaDon"))
                + " của khách " + safe(first.get("tenKhachHang"))
                + ", trễ " + parseInt(first.get("daysLate"), 0) + " ngày.";
    }

    private boolean isClosedStatus(String status) {
        String normalized = normalize(status);
        return normalized.contains("hoan thanh")
                || normalized.contains("hoàn thành")
                || normalized.contains("da giao")
                || normalized.contains("đã giao")
                || normalized.contains("da huy")
                || normalized.contains("đã hủy")
                || normalized.contains("huy")
                || normalized.contains("giao that bai")
                || normalized.contains("giao thất bại")
                || normalized.contains("hoan ve")
                || normalized.contains("hoàn về");
    }

    private int parseInt(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
