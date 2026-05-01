package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Model.PhieuGiamGia;
import org.example.datnnhom03.Repository.PhieuGiamGiaRepository;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PromotionTool implements AssistantTool {

    private final PhieuGiamGiaRepository phieuGiamGiaRepository;

    public PromotionTool(PhieuGiamGiaRepository phieuGiamGiaRepository) {
        this.phieuGiamGiaRepository = phieuGiamGiaRepository;
    }

    @Override
    public String getName() {
        return "get_promotions";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return true;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> items = phieuGiamGiaRepository.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getTrangThai()) || v.getTrangThai() == null)
                .filter(v -> v.getNgayBatDau() == null || !v.getNgayBatDau().isAfter(today))
                .filter(v -> v.getNgayKetThuc() == null || !v.getNgayKetThuc().isBefore(today))
                .sorted(Comparator.comparing(PhieuGiamGia::getGiaTriGiamGia, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(this::toRow)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", items.size());
        result.put("items", items);
        return result;
    }

    private Map<String, Object> toRow(PhieuGiamGia voucher) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ma", voucher.getMaPhieuGiamGia());
        row.put("ten", voucher.getTenPhieuGiamGia());
        row.put("giaTri", voucher.getGiaTriGiamGia());
        row.put("hinhThuc", Boolean.TRUE.equals(voucher.getHinhThucGiam()) ? "%" : "VND");
        row.put("hoaDonToiThieu", voucher.getHoaDonToiThieu());
        row.put("ngayKetThuc", voucher.getNgayKetThuc());
        return row;
    }
}
