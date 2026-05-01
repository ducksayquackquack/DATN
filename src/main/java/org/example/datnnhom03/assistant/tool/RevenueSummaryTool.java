package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Repository.HoaDonRepository;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class RevenueSummaryTool implements AssistantTool {

    private final HoaDonRepository hoaDonRepository;

    public RevenueSummaryTool(HoaDonRepository hoaDonRepository) {
        this.hoaDonRepository = hoaDonRepository;
    }

    @Override
    public String getName() {
        return "get_revenue_summary";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return role == AssistantRole.ADMIN;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        LocalDate from = LocalDate.parse(String.valueOf(args.get("from")));
        LocalDate to = LocalDate.parse(String.valueOf(args.get("to")));

        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();

        List<HoaDon> completed = hoaDonRepository.findAll()
                .stream()
                .filter(hd -> hd.getNgayTao() != null)
                .filter(hd -> !hd.getNgayTao().isBefore(fromDateTime) && hd.getNgayTao().isBefore(toDateTime))
                .filter(hd -> {
                    String status = hd.getTrangThai() == null ? "" : hd.getTrangThai().trim().toLowerCase();
                    return status.contains("đã giao") || status.contains("hoàn thành") || status.contains("da giao") || status.contains("hoan thanh");
                })
                .toList();

        double totalRevenue = completed.stream()
                .map(HoaDon::getThanhTien)
                .filter(v -> v != null)
                .mapToDouble(v -> v.doubleValue())
                .sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from.toString());
        result.put("to", to.toString());
        result.put("period", String.valueOf(args.getOrDefault("period", "month")));
        result.put("chartGranularity", String.valueOf(args.getOrDefault("chartGranularity", "day")));
        result.put("label", String.valueOf(args.getOrDefault("label", "")));
        result.put("totalOrders", completed.size());
        result.put("totalRevenue", totalRevenue);
        result.put("averageOrderValue", completed.isEmpty() ? 0 : totalRevenue / completed.size());
        result.put("summary", buildSummary(from, to, args, totalRevenue, completed.size()));
        result.put("series", buildSeries(completed, String.valueOf(args.getOrDefault("chartGranularity", "day"))));
        return result;
    }

    private String buildSummary(LocalDate from, LocalDate to, Map<String, Object> args, double totalRevenue, int orderCount) {
        String label = String.valueOf(args.getOrDefault("label", ""));
        if (!label.isBlank()) {
            return "Tổng doanh thu " + label + ": " + Math.round(totalRevenue) + " đ với " + orderCount + " đơn hoàn thành.";
        }
        return "Doanh thu từ " + from + " đến " + to + " là " + Math.round(totalRevenue) + " với " + orderCount + " đơn hoàn thành.";
    }

    private List<Map<String, Object>> buildSeries(List<HoaDon> completed, String granularity) {
        Map<String, Double> bucket = new LinkedHashMap<>();
        for (HoaDon hd : completed) {
            LocalDateTime time = hd.getNgayTao();
            if (time == null) continue;
            String key;
            switch (granularity == null ? "day" : granularity.toLowerCase()) {
                case "hour" -> key = String.format("%02d:00", time.getHour());
                case "month" -> key = String.format("%d-%02d", time.getYear(), time.getMonthValue());
                default -> key = time.toLocalDate().toString();
            }
            bucket.put(key, bucket.getOrDefault(key, 0d) + (hd.getThanhTien() == null ? 0d : hd.getThanhTien().doubleValue()));
        }
        List<Map<String, Object>> series = new ArrayList<>();
        for (Map.Entry<String, Double> entry : bucket.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            series.add(row);
        }
        return series;
    }
}