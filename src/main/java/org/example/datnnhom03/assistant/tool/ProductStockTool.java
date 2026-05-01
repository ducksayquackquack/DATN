package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Model.SanPhamChiTiet;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.example.datnnhom03.Repository.SanPhamChiTietRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProductStockTool implements AssistantTool {

    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    public ProductStockTool(SanPhamChiTietRepository sanPhamChiTietRepository) {
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
    }

    @Override
    public String getName() {
        return "check_product_stock";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return true;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        String keyword = cleanKeyword(normalize(args.getOrDefault("keyword", "")));
        String maSanPham = normalize(args.getOrDefault("maSanPham", ""));
        String maBienThe = normalize(args.getOrDefault("maBienThe", ""));
        String color = normalize(args.getOrDefault("color", ""));
        String size = normalize(args.getOrDefault("size", ""));
        String category = normalize(args.getOrDefault("category", ""));

        List<SanPhamChiTiet> matched = sanPhamChiTietRepository.findAll()
                .stream()
                .filter(spct -> matches(spct, keyword, maSanPham, maBienThe, color, size, category))
                .collect(Collectors.toList());

        List<Map<String, Object>> exactVariants = matched.stream()
                .map(this::toVariantRow)
                .collect(Collectors.toList());

        Map<Integer, ProductStockAggregate> aggregateMap = new LinkedHashMap<>();
        for (SanPhamChiTiet spct : matched) {
            SanPham sp = spct.getSanPham();
            if (sp == null || sp.getId() == null) continue;

            ProductStockAggregate agg = aggregateMap.computeIfAbsent(sp.getId(), id -> {
                ProductStockAggregate x = new ProductStockAggregate();
                x.productId = sp.getId();
                x.maSanPham = sp.getMaSanPham();
                x.tenSanPham = sp.getTenSanPham();
                return x;
            });

            int soLuong = spct.getSoLuong() == null ? 0 : spct.getSoLuong();
            agg.totalStock += soLuong;
            agg.variants.add(toVariantRow(spct));
        }

        List<Map<String, Object>> items = aggregateMap.values()
                .stream()
                .sorted(Comparator.comparing(ProductStockAggregate::primaryVariantStock).reversed())
                .map(ProductStockAggregate::toMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keyword", keyword);
        result.put("maSanPham", maSanPham);
        result.put("maBienThe", maBienThe);
        result.put("color", color);
        result.put("size", size);
        result.put("category", category);
        result.put("count", items.size());
        result.put("items", items);
        result.put("exactVariants", exactVariants);
        result.put("exact", !maBienThe.isBlank() || !maSanPham.isBlank());
        return result;
    }

    private boolean matches(SanPhamChiTiet spct,
                            String keyword,
                            String maSanPham,
                            String maBienThe,
                            String color,
                            String size,
                            String category) {
        SanPham sp = spct.getSanPham();
        if (sp == null) return false;

        String ten = safe(sp.getTenSanPham());
        String ma = safe(sp.getMaSanPham());
        String maSpct = safe(spct.getMa());
        String mau = safe(spct.getMauSac() == null ? null : spct.getMauSac().getTenMau());
        String kichThuoc = safe(spct.getKichThuoc() == null ? null : spct.getKichThuoc().getTenKichThuoc());
        String loai = safe(spct.getLoai() == null ? null : spct.getLoai().getTenLoai());
        String danhMuc = safe(spct.getDanhMuc() == null ? null : spct.getDanhMuc().getTenDanhMuc());

        if (!maBienThe.isBlank() && !maSpct.equals(maBienThe)) {
            return false;
        }
        if (!maSanPham.isBlank() && !ma.equals(maSanPham)) {
            return false;
        }
        if (!color.isBlank() && !mau.contains(color)) {
            return false;
        }
        if (!size.isBlank() && !kichThuoc.equals(size)) {
            return false;
        }
        if (!category.isBlank() && !(ten.contains(category) || loai.contains(category) || danhMuc.contains(category))) {
            return false;
        }

        return keyword.isBlank()
                || ten.contains(keyword)
                || allKeywordTokensMatch(keyword, ten)
                || ma.contains(keyword)
                || maSpct.contains(keyword)
                || mau.contains(keyword)
                || kichThuoc.contains(keyword)
                || loai.contains(keyword)
                || danhMuc.contains(keyword);
    }

    private Map<String, Object> toVariantRow(SanPhamChiTiet spct) {
        Map<String, Object> variant = new LinkedHashMap<>();
        variant.put("spctId", spct.getId());
        variant.put("ma", spct.getMa());
        variant.put("maSanPham", spct.getSanPham() == null ? null : spct.getSanPham().getMaSanPham());
        variant.put("tenSanPham", spct.getSanPham() == null ? null : spct.getSanPham().getTenSanPham());
        variant.put("soLuong", spct.getSoLuong() == null ? 0 : spct.getSoLuong());
        variant.put("giaBan", spct.getGiaBan());
        variant.put("mauSac", spct.getMauSac() != null ? spct.getMauSac().getTenMau() : null);
        variant.put("kichThuoc", spct.getKichThuoc() != null ? spct.getKichThuoc().getTenKichThuoc() : null);
        variant.put("loai", spct.getLoai() != null ? spct.getLoai().getTenLoai() : null);
        variant.put("danhMuc", spct.getDanhMuc() != null ? spct.getDanhMuc().getTenDanhMuc() : null);
        return variant;
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString().trim().toLowerCase(Locale.ROOT);
    }

    private String normalize(Object value) {
        return safe(value);
    }

    private String cleanKeyword(String keyword) {
        String cleaned = keyword == null ? "" : keyword;
        cleaned = cleaned.replace("còn bao nhiêu chiếc", " ")
                .replace("con bao nhieu chiec", " ")
                .replace("bao nhiêu chiếc", " ")
                .replace("bao nhieu chiec", " ")
                .replace("còn bao nhiêu", " ")
                .replace("con bao nhieu", " ")
                .replace("tồn kho", " ")
                .replace("ton kho", " ")
                .replace("còn hàng", " ")
                .replace("con hang", " ")
                .replace("bao nhiêu", " ")
                .replace("bao nhieu", " ")
                .replace("chiếc", " ")
                .replace("chiec", " ")
                .replaceAll("\s+", " ")
                .trim();
        return cleaned;
    }

    private boolean allKeywordTokensMatch(String keyword, String target) {
        if (keyword == null || keyword.isBlank()) return true;
        String[] tokens = keyword.trim().split("\s+");
        int meaningful = 0;
        for (String token : tokens) {
            if (token == null || token.isBlank() || token.length() < 2) continue;
            meaningful++;
            if (!target.contains(token)) {
                return false;
            }
        }
        return meaningful > 0;
    }

    private static class ProductStockAggregate {
        Integer productId;
        String maSanPham;
        String tenSanPham;
        int totalStock;
        List<Map<String, Object>> variants = new ArrayList<>();

        int primaryVariantStock() {
            if (variants.isEmpty()) return 0;
            Object value = variants.get(0).get("soLuong");
            return value instanceof Number number ? number.intValue() : 0;
        }

        Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", productId);
            row.put("maSanPham", maSanPham);
            row.put("tenSanPham", tenSanPham);
            row.put("tongTon", totalStock);
            row.put("variants", variants);
            row.put("soLuong", totalStock);
            if (!variants.isEmpty()) {
                row.putAll(new LinkedHashMap<>(variants.get(0)));
            }
            return row;
        }
    }
}
