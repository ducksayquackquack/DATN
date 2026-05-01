package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Repository.HoaDonRepository;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class OrderFilterTool implements AssistantTool {
    private final HoaDonRepository hoaDonRepository;
    public OrderFilterTool(HoaDonRepository hoaDonRepository) { this.hoaDonRepository = hoaDonRepository; }

    @Override public String getName() { return "filter_orders_natural"; }
    @Override public boolean supports(AssistantRole role) { return true; }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        String query = normalize(args.get("query"));
        boolean undelivered = containsAny(query, "chưa giao", "chua giao", "chưa nhận", "dang giao", "đang giao", "cho giao");
        boolean hanoi = containsAny(query, "hà nội", "ha noi");
        List<HoaDon> matched = hoaDonRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(hd -> !undelivered || !containsAny(normalize(hd.getTrangThai()), "đã giao", "da giao", "hoàn thành", "hoan thanh"))
                .filter(hd -> !hanoi || containsAny(normalize(hd.getDiaChiNhanHang()), "hà nội", "ha noi"))
                .sorted(Comparator.comparing(HoaDon::getNgayTao, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(20)
                .collect(Collectors.toList());
        List<Map<String, Object>> items = matched.stream().map(this::toRow).collect(Collectors.toList());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", items.size());
        result.put("items", items);
        result.put("summary", items.isEmpty() ? "Em chưa thấy đơn phù hợp với bộ lọc này." : "Em tìm thấy " + items.size() + " đơn phù hợp" + (undelivered ? " chưa giao" : "") + (hanoi ? " ở Hà Nội" : "") + ".");
        return result;
    }

    private Map<String, Object> toRow(HoaDon hd) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("maHoaDon", hd.getMaHoaDon());
        row.put("tenKhachHang", hd.getTenKhachHang());
        row.put("soDienThoaiNhanHang", hd.getSoDienThoaiNhanHang());
        row.put("diaChiNhanHang", hd.getDiaChiNhanHang());
        row.put("trangThai", hd.getTrangThai());
        row.put("ngayTao", hd.getNgayTao());
        row.put("thanhTien", hd.getThanhTien());
        return row;
    }

    private boolean containsAny(String text, String... keywords) {
        String t = normalize(text);
        for (String k : keywords) if (!normalize(k).isBlank() && t.contains(normalize(k))) return true;
        return false;
    }
    private String normalize(Object value) { return value == null ? "" : String.valueOf(value).trim().toLowerCase(Locale.ROOT); }
}
