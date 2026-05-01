package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Model.SanPhamChiTiet;
import org.example.datnnhom03.Repository.SanPhamRepository;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProductSearchTool implements AssistantTool {

    private final SanPhamRepository sanPhamRepository;

    public ProductSearchTool(SanPhamRepository sanPhamRepository) {
        this.sanPhamRepository = sanPhamRepository;
    }

    @Override
    public String getName() {
        return "search_product";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return true;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        String keyword = normalize(args.getOrDefault("keyword", ""));
        List<String> tokens = Arrays.stream(keyword.split("\\s+"))
                .filter(t -> t != null && t.length() >= 2)
                .toList();

        List<SanPham> matched = sanPhamRepository.findAll()
                .stream()
                .filter(this::isActive)
                .map(sp -> Map.entry(sp, score(sp, keyword, tokens)))
                .filter(entry -> keyword.isBlank() || entry.getValue() > 0)
                .sorted(
                        Comparator.<Map.Entry<SanPham, Integer>>comparingInt(Map.Entry::getValue)
                                .reversed()
                                .thenComparing(entry -> normalize(entry.getKey().getTenSanPham()))
                )
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Map<String, Object>> items = matched.stream().map(this::toRow).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keyword", keyword);
        result.put("count", items.size());
        result.put("items", items);
        return result;
    }

    private boolean isActive(SanPham sp) {
        String trangThai = normalize(sp.getTrangThai());
        return trangThai.isBlank() || trangThai.contains("hoat dong");
    }

    private Map<String, Object> toRow(SanPham sp) {
        List<SanPhamChiTiet> variants = sp.getSanPhamChiTiets() == null ? List.of() : sp.getSanPhamChiTiets();

        int totalStock = 0;
        BigDecimal minPrice = null;
        Set<String> colors = new LinkedHashSet<>();
        Set<String> sizes = new LinkedHashSet<>();
        Set<String> categories = new LinkedHashSet<>();

        for (SanPhamChiTiet variant : variants) {
            if (variant.getSoLuong() != null) {
                totalStock += Math.max(variant.getSoLuong(), 0);
            }
            if (variant.getGiaBan() != null) {
                if (minPrice == null || variant.getGiaBan().compareTo(minPrice) < 0) {
                    minPrice = variant.getGiaBan();
                }
            }
            tryAdd(colors, variant.getMauSac() == null ? null : variant.getMauSac().getTenMau());
            tryAdd(sizes, variant.getKichThuoc() == null ? null : variant.getKichThuoc().getTenKichThuoc());
            tryAdd(categories, variant.getLoai() == null ? null : variant.getLoai().getTenLoai());
            tryAdd(categories, variant.getDanhMuc() == null ? null : variant.getDanhMuc().getTenDanhMuc());
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", sp.getId());
        row.put("maSanPham", sp.getMaSanPham());
        row.put("tenSanPham", sp.getTenSanPham());
        row.put("trangThai", sp.getTrangThai());
        row.put("moTa", sp.getMoTa());
        row.put("giaTu", minPrice);
        row.put("tongTon", totalStock);
        row.put("mauSac", new ArrayList<>(colors));
        row.put("kichThuoc", new ArrayList<>(sizes));
        row.put("loai", new ArrayList<>(categories));
        return row;
    }

    private void tryAdd(Set<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value.trim());
        }
    }

    private int score(SanPham sp, String keyword, List<String> tokens) {
        List<SanPhamChiTiet> variants = sp.getSanPhamChiTiets() == null ? List.of() : sp.getSanPhamChiTiets();
        String ten = normalize(sp.getTenSanPham());
        String ma = normalize(sp.getMaSanPham());
        String moTa = normalize(sp.getMoTa());
        String categories = variants.stream()
                .flatMap(v -> java.util.stream.Stream.of(
                        normalize(v.getLoai() == null ? null : v.getLoai().getTenLoai()),
                        normalize(v.getDanhMuc() == null ? null : v.getDanhMuc().getTenDanhMuc()),
                        normalize(v.getMauSac() == null ? null : v.getMauSac().getTenMau()),
                        normalize(v.getKichThuoc() == null ? null : v.getKichThuoc().getTenKichThuoc())
                ))
                .collect(Collectors.joining(" "));

        if (keyword.isBlank()) return 1;

        int score = 0;
        if (ten.contains(keyword) || ma.contains(keyword) || moTa.contains(keyword) || categories.contains(keyword)) {
            score += 10;
        }
        for (String token : tokens) {
            if (ten.contains(token) || ma.contains(token) || moTa.contains(token) || categories.contains(token)) {
                score += 2;
            }
        }
        if (keyword.contains("ao khoac") || keyword.contains("jacket")) {
            if (categories.contains("ao khoac") || categories.contains("hoodie") || categories.contains("bomber") || categories.contains("coach")) {
                score += 8;
            }
        }
        return score;
    }

    private String normalize(Object value) {
        return value == null ? "" : value.toString().trim().toLowerCase(Locale.ROOT);
    }
}
