package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Model.SanPhamChiTiet;
import org.example.datnnhom03.Repository.SanPhamChiTietRepository;
import org.example.datnnhom03.Repository.SanPhamRepository;
import org.example.datnnhom03.Service.SanPhamService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/san-pham")
@CrossOrigin("*")
public class SanPhamApiController {

    private final SanPhamService service;

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    public SanPhamApiController(SanPhamService service,
                                SanPhamRepository sanPhamRepository,
                                SanPhamChiTietRepository sanPhamChiTietRepository) {
        this.service = service;
        this.sanPhamRepository = sanPhamRepository;
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
    }

    @GetMapping
    public List<SanPham> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SanPham getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public SanPham create(@RequestBody SanPham sanPham) {
        validateSanPhamPayload(sanPham, true, null);
        return service.create(sanPham);
    }

    @PutMapping("/{id}")
    public SanPham update(@PathVariable Integer id,
                          @RequestBody SanPham sanPham) {
        validateSanPhamPayload(sanPham, false, id);
        return service.update(id, sanPham);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }

    // 1) Search sản phẩm theo tên
    // GET /api/products/search?q=a
    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q) {
        return sanPhamRepository.findTop10ByTenSanPhamContainingIgnoreCase(q).stream()
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getTenSanPham());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // 2) Lấy size còn hàng theo productId
    // GET /api/products/{productId}/sizes
    @GetMapping("/{productId}/sizes")
    public List<Map<String, Object>> sizes(@PathVariable Integer productId) {
        // giả sử repo trả về List<Object[]> dạng: [sizeId, sizeName]
        List<Object[]> rows = sanPhamChiTietRepository.findSizesInStockRaw(productId);

        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ((Number) r[0]).intValue());
            m.put("name", String.valueOf(r[1]));
            out.add(m);
        }
        return out;
    }

    // 3) Lấy màu còn hàng theo productId + sizeId
    // GET /api/products/{productId}/sizes/{sizeId}/colors
    @GetMapping("/{productId}/sizes/{sizeId}/colors")
    public List<Map<String, Object>> colors(@PathVariable Integer productId,
                                            @PathVariable Integer sizeId) {
        // giả sử repo trả về List<Object[]> dạng: [colorId, colorName]
        List<Object[]> rows = sanPhamChiTietRepository.findColorsInStockRaw(productId, sizeId);

        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ((Number) r[0]).intValue());
            m.put("name", String.valueOf(r[1]));
            out.add(m);
        }
        return out;
    }

    private void validateSanPhamPayload(SanPham sanPham, boolean isCreate, Integer id) {
        if (sanPham == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload san pham khong hop le");
        }

        String maSanPham = normalizeText(sanPham.getMaSanPham());
        if (!maSanPham.isEmpty() && !maSanPham.matches("^SP\\d+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ma san pham phai theo dinh dang SP + so");
        }

        if (!maSanPham.isEmpty()) {
            boolean duplicate = isCreate
                    ? sanPhamRepository.existsByMaSanPhamIgnoreCase(maSanPham)
                    : sanPhamRepository.existsByMaSanPhamIgnoreCaseAndIdNot(maSanPham, id);
            if (duplicate) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ma san pham da ton tai");
            }
            sanPham.setMaSanPham(maSanPham);
        }

        String tenSanPham = normalizeText(sanPham.getTenSanPham());
        if (tenSanPham.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ten san pham khong duoc de trong");
        }
        sanPham.setTenSanPham(tenSanPham);

        String trangThai = normalizeText(sanPham.getTrangThai());
        if (!trangThai.isEmpty() && !isAllowedTrangThai(trangThai)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang thai san pham khong hop le");
        }

        List<SanPhamChiTiet> variants = sanPham.getSanPhamChiTiets();
        if (isCreate && (variants == null || variants.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "San pham phai co it nhat mot bien the");
        }

        if (variants != null && !variants.isEmpty()) {
            validateVariants(variants);
        }
    }

    private void validateVariants(List<SanPhamChiTiet> variants) {
        Set<String> uniqueVariantCodes = new HashSet<>();

        for (SanPhamChiTiet variant : variants) {
            if (variant == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Du lieu bien the khong hop le");
            }

            String maVariant = normalizeText(variant.getMa());
            if (!maVariant.isEmpty()) {
                String normalized = maVariant.toUpperCase();
                if (!uniqueVariantCodes.add(normalized)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ma bien the bi trung trong cung payload");
                }
                variant.setMa(maVariant);
            }

            BigDecimal giaBan = variant.getGiaBan();
            if (giaBan == null || giaBan.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gia ban bien the phai lon hon 0");
            }

            Integer soLuong = variant.getSoLuong();
            if (soLuong == null || soLuong < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "So luong bien the khong hop le");
            }

            if (variant.getLoai() == null || variant.getLoai().getId() == null || variant.getLoai().getId() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loai cua bien the khong hop le");
            }
        }
    }

    private boolean isAllowedTrangThai(String value) {
        return "Hoạt động".equalsIgnoreCase(value)
                || "Hoat dong".equalsIgnoreCase(value)
                || "Ngừng hoạt động".equalsIgnoreCase(value)
                || "Ngung hoat dong".equalsIgnoreCase(value)
                || "Ẩn".equalsIgnoreCase(value)
                || "An".equalsIgnoreCase(value);
    }

    private String normalizeText(String value) {
        return String.valueOf(value == null ? "" : value).trim();
    }
}