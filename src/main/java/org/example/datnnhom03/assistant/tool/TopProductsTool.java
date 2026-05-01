package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Model.HoaDonChiTiet;
import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Model.SanPhamChiTiet;
import org.example.datnnhom03.Repository.HoaDonChiTietRepository;
import org.example.datnnhom03.Repository.HoaDonRepository;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TopProductsTool implements AssistantTool {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;

    public TopProductsTool(HoaDonRepository hoaDonRepository,
                           HoaDonChiTietRepository hoaDonChiTietRepository) {
        this.hoaDonRepository = hoaDonRepository;
        this.hoaDonChiTietRepository = hoaDonChiTietRepository;
    }

    @Override
    public String getName() {
        return "get_top_products";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return role == AssistantRole.ADMIN || role == AssistantRole.EMPLOYEE;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        LocalDate from = LocalDate.parse(String.valueOf(args.get("from")));
        LocalDate to = LocalDate.parse(String.valueOf(args.get("to")));
        int limit = Integer.parseInt(String.valueOf(args.getOrDefault("limit", 5)));

        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();

        Set<Integer> validOrderIds = hoaDonRepository.findAll()
                .stream()
                .filter(hd -> hd.getNgayTao() != null)
                .filter(hd -> !hd.getNgayTao().isBefore(fromDateTime) && hd.getNgayTao().isBefore(toDateTime))
                .filter(this::isCompletedOrder)
                .map(HoaDon::getId)
                .collect(Collectors.toSet());

        Map<Integer, ProductAggregate> aggregateMap = new HashMap<>();

        for (HoaDonChiTiet hdct : hoaDonChiTietRepository.findAll()) {
            if (hdct.getHoaDon() == null || hdct.getHoaDon().getId() == null) continue;
            if (!validOrderIds.contains(hdct.getHoaDon().getId())) continue;

            SanPhamChiTiet spct = hdct.getSanPhamChiTiet();
            if (spct == null) continue;

            SanPham sp = spct.getSanPham();
            if (sp == null || sp.getId() == null) continue;

            ProductAggregate agg = aggregateMap.computeIfAbsent(sp.getId(), id -> {
                ProductAggregate x = new ProductAggregate();
                x.productId = sp.getId();
                x.maSanPham = sp.getMaSanPham();
                x.tenSanPham = sp.getTenSanPham();
                return x;
            });

            int soLuong = hdct.getSoLuong() == null ? 0 : hdct.getSoLuong();
            double thanhTien = hdct.getThanhTien() == null ? 0.0 : hdct.getThanhTien().doubleValue();

            agg.totalQuantity += soLuong;
            agg.totalRevenue += thanhTien;
            agg.orderCount += 1;
        }

        List<Map<String, Object>> items = aggregateMap.values()
                .stream()
                .sorted(Comparator.comparingInt((ProductAggregate x) -> x.totalQuantity).reversed())
                .limit(limit)
                .map(ProductAggregate::toMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("from", from.toString());
        result.put("to", to.toString());
        result.put("limit", limit);
        result.put("count", items.size());
        result.put("items", items);
        return result;
    }

    private boolean isCompletedOrder(HoaDon hd) {
        String status = hd.getTrangThai() == null ? "" : hd.getTrangThai().trim().toLowerCase();
        return status.contains("đã giao")
                || status.contains("hoàn thành")
                || status.contains("da giao")
                || status.contains("hoan thanh");
    }

    private static class ProductAggregate {
        Integer productId;
        String maSanPham;
        String tenSanPham;
        int totalQuantity;
        double totalRevenue;
        int orderCount;

        Map<String, Object> toMap() {
            Map<String, Object> row = new HashMap<>();
            row.put("productId", productId);
            row.put("maSanPham", maSanPham);
            row.put("tenSanPham", tenSanPham);
            row.put("totalQuantity", totalQuantity);
            row.put("totalRevenue", totalRevenue);
            row.put("orderCount", orderCount);
            return row;
        }
    }
}