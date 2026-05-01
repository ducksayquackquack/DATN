package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Model.HoaDonChiTiet;
import org.example.datnnhom03.Repository.HoaDonChiTietRepository;
import org.example.datnnhom03.Repository.HoaDonRepository;
import org.example.datnnhom03.Repository.OrderStatusHistoryRepository;
import org.example.datnnhom03.Repository.projection.OrderStatusHistoryView;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class OrderLookupTool implements AssistantTool {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    public OrderLookupTool(HoaDonRepository hoaDonRepository,
                           HoaDonChiTietRepository hoaDonChiTietRepository,
                           OrderStatusHistoryRepository orderStatusHistoryRepository) {
        this.hoaDonRepository = hoaDonRepository;
        this.hoaDonChiTietRepository = hoaDonChiTietRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
    }

    @Override
    public String getName() {
        return "find_order";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return true;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        String keyword = String.valueOf(args.getOrDefault("keyword", "")).trim().toLowerCase();

        List<HoaDon> matched = hoaDonRepository.findAll()
                .stream()
                .filter(hd -> matches(hd, keyword))
                .sorted(Comparator.comparing(HoaDon::getNgayTao, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(10)
                .collect(Collectors.toList());

        List<Map<String, Object>> items = matched.stream().map(this::toMap).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keyword", keyword);
        result.put("count", items.size());
        result.put("items", items);
        return result;
    }

    private boolean matches(HoaDon hd, String keyword) {
        if (keyword.isBlank()) return false;

        String maHoaDon = safe(hd.getMaHoaDon());
        String tenKhachHang = safe(hd.getTenKhachHang());
        String sdt = safe(hd.getSoDienThoaiNhanHang());
        String trangThai = safe(hd.getTrangThai());

        return maHoaDon.contains(keyword)
                || tenKhachHang.contains(keyword)
                || sdt.contains(keyword)
                || trangThai.contains(keyword);
    }

    private Map<String, Object> toMap(HoaDon hd) {
        List<HoaDonChiTiet> orderItems = hoaDonChiTietRepository.findAll().stream()
                .filter(item -> item.getHoaDon() != null && Objects.equals(item.getHoaDon().getId(), hd.getId()))
                .collect(Collectors.toList());

        List<Map<String, Object>> items = orderItems.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("maSanPhamChiTiet", item.getSanPhamChiTiet() == null ? null : item.getSanPhamChiTiet().getMa());
            row.put("tenSanPham", item.getSanPhamChiTiet() != null && item.getSanPhamChiTiet().getSanPham() != null
                    ? item.getSanPhamChiTiet().getSanPham().getTenSanPham() : null);
            row.put("soLuong", item.getSoLuong());
            row.put("thanhTien", item.getThanhTien());
            return row;
        }).collect(Collectors.toList());

        List<Map<String, Object>> timeline = orderStatusHistoryRepository.viewByHoaDonId(hd.getId()).stream()
                .limit(5)
                .map(this::toTimelineRow)
                .collect(Collectors.toList());

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", hd.getId());
        row.put("maHoaDon", hd.getMaHoaDon());
        row.put("tenKhachHang", hd.getTenKhachHang());
        row.put("soDienThoaiNhanHang", hd.getSoDienThoaiNhanHang());
        row.put("diaChiNhanHang", hd.getDiaChiNhanHang());
        row.put("trangThai", hd.getTrangThai());
        row.put("thanhTien", hd.getThanhTien());
        row.put("phiShip", hd.getPhiShip());
        row.put("ngayTao", hd.getNgayTao());
        row.put("ngayNhanHangDuKien", hd.getNgayNhanHangDuKien());
        row.put("phuongThucThanhToan", hd.getPhuongThucThanhToan());
        row.put("items", items);
        row.put("timeline", timeline);
        return row;
    }

    private Map<String, Object> toTimelineRow(OrderStatusHistoryView row) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("changedAt", row.getChangedAt());
        item.put("fromStatus", row.getFromStatus());
        item.put("toStatus", row.getToStatus());
        item.put("note", row.getNote());
        return item;
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString().trim().toLowerCase();
    }
}
