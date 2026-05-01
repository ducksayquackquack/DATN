package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Model.SanPhamChiTiet;
import org.example.datnnhom03.Repository.SanPhamChiTietRepository;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class LowStockTool implements AssistantTool {

    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    public LowStockTool(SanPhamChiTietRepository sanPhamChiTietRepository) {
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
    }

    @Override
    public String getName() {
        return "get_low_stock_products";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return true;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        int threshold = Integer.parseInt(String.valueOf(args.getOrDefault("threshold", 10)));

        List<Map<String, Object>> items = sanPhamChiTietRepository.findAll()
                .stream()
                .filter(spct -> spct.getSoLuong() != null && spct.getSoLuong() <= threshold)
                .sorted(Comparator.comparing(spct -> Optional.ofNullable(spct.getSoLuong()).orElse(0)))
                .limit(20)
                .map(this::toMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("threshold", threshold);
        result.put("count", items.size());
        result.put("items", items);
        return result;
    }

    private Map<String, Object> toMap(SanPhamChiTiet spct) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", spct.getId());
        row.put("ma", spct.getMa());
        row.put("soLuong", spct.getSoLuong());
        row.put("giaBan", spct.getGiaBan());

        if (spct.getSanPham() != null) {
            row.put("tenSanPham", spct.getSanPham().getTenSanPham());
            row.put("maSanPham", spct.getSanPham().getMaSanPham());
        }
        if (spct.getMauSac() != null) {
            row.put("mauSac", spct.getMauSac().getTenMau());
        }
        if (spct.getKichThuoc() != null) {
            row.put("kichThuoc", spct.getKichThuoc().getTenKichThuoc());
        }
        return row;
    }
}