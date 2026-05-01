package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Model.SanPhamChiTiet;
import org.example.datnnhom03.Repository.SanPhamChiTietRepository;
import org.example.datnnhom03.Repository.SanPhamRepository;
import org.example.datnnhom03.Service.SanPhamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SanPhamServiceImpl implements SanPhamService {

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Override
    @Transactional
    public SanPham create(SanPham sanPham) {
        String providedCode = normalizeText(sanPham.getMaSanPham());
        int maxAttempts = providedCode.isBlank() ? 6 : 1;

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String codeToUse = providedCode.isBlank() ? generateNextProductCode() : providedCode;
            try {
                return persistCreate(sanPham, codeToUse);
            } catch (DataIntegrityViolationException ex) {
                if (!providedCode.isBlank()) {
                    throw new RuntimeException("Ma san pham da ton tai", ex);
                }
                lastFailure = new RuntimeException("Khong the tu dong sinh ma san pham", ex);
            }
        }

        throw (lastFailure != null)
                ? lastFailure
                : new RuntimeException("Khong the tao san pham");
    }

    @Override
    @Transactional
    public SanPham update(Integer id, SanPham sanPham) {
        SanPham sp = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        if (sanPham.getMaSanPham() != null && !sanPham.getMaSanPham().isBlank()) {
            sp.setMaSanPham(sanPham.getMaSanPham());
        }
        sp.setTenSanPham(sanPham.getTenSanPham());
        sp.setMoTa(sanPham.getMoTa());
        sp.setTrangThai(sanPham.getTrangThai());
        sp.setNgaySua(LocalDateTime.now());

        SanPham saved = sanPhamRepository.save(sp);

        if (sanPham.getSanPhamChiTiets() != null) {
            upsertVariants(saved, sanPham.getSanPhamChiTiets());
        }

        saved.setSanPhamChiTiets(sanPhamChiTietRepository.findBySanPhamId(saved.getId()));
        return saved;
    }

    private void saveVariants(SanPham sanPham, List<SanPhamChiTiet> variants, boolean isCreate) {
        if (variants == null || variants.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (SanPhamChiTiet incoming : variants) {
            SanPhamChiTiet target = new SanPhamChiTiet();
            mapVariantFields(target, incoming);
            target.setSanPham(sanPham);
            if (isCreate) {
                target.setNgayTao(target.getNgayTao() != null ? target.getNgayTao() : now);
            }
            target.setNgaySua(now);
            sanPhamChiTietRepository.save(target);
        }
    }

    private void upsertVariants(SanPham sanPham, List<SanPhamChiTiet> incomingVariants) {
        Map<Integer, SanPhamChiTiet> existingById = new HashMap<>();
        for (SanPhamChiTiet existing : sanPhamChiTietRepository.findBySanPhamId(sanPham.getId())) {
            if (existing.getId() != null) {
                existingById.put(existing.getId(), existing);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (SanPhamChiTiet incoming : incomingVariants) {
            SanPhamChiTiet target = null;
            if (incoming.getId() != null) {
                target = existingById.get(incoming.getId());
            }

            if (target == null) {
                target = new SanPhamChiTiet();
                target.setSanPham(sanPham);
                target.setNgayTao(now);
            }

            mapVariantFields(target, incoming);
            target.setSanPham(sanPham);
            target.setNgaySua(now);
            sanPhamChiTietRepository.save(target);
        }
    }

    private void mapVariantFields(SanPhamChiTiet target, SanPhamChiTiet source) {
        target.setMa(source.getMa());
        target.setGiaNhap(source.getGiaNhap());
        target.setGiaBan(source.getGiaBan());
        target.setSoLuong(source.getSoLuong());
        target.setChatLieu(source.getChatLieu());
        target.setMauSac(source.getMauSac());
        target.setKichThuoc(source.getKichThuoc());
        target.setHang(source.getHang());
        target.setXuatSu(source.getXuatSu());
        target.setDanhMuc(source.getDanhMuc());
        target.setLoai(source.getLoai());
        target.setNguoiTao(source.getNguoiTao());
        target.setNguoiSua(source.getNguoiSua());
        target.setTrangThai(source.getTrangThai());
        target.setBarcode(source.getBarcode());
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        SanPham sp = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        LocalDateTime now = LocalDateTime.now();
        sp.setTrangThai("Ngừng hoạt động");
        sp.setNgaySua(now);
        sanPhamRepository.save(sp);

        List<SanPhamChiTiet> variants = sanPhamChiTietRepository.findBySanPhamId(id);
        for (SanPhamChiTiet variant : variants) {
            variant.setTrangThai("Ngừng hoạt động");
            variant.setNgaySua(now);
        }
        sanPhamChiTietRepository.saveAll(variants);
    }

    @Override
    public SanPham findById(Integer id) {
        return sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
    }

    @Override
    public List<SanPham> findAll() {
        return sanPhamRepository.findAll();
    }

    // ===== BỔ SUNG PHỤC VỤ HOME =====
    @Override
    public List<SanPham> getSanPhamHoatDong() {
        return sanPhamRepository.findByTrangThai("Hoạt động");
    }

    @Override
    public SanPhamChiTiet getOneActiveSanPhamChiTiet(Integer sanPhamId) {
        return sanPhamChiTietRepository
                .findFirstBySanPhamIdAndTrangThai(sanPhamId, "Hoạt động")
                .orElse(null);
    }

    private SanPham persistCreate(SanPham source, String maSanPham) {
        LocalDateTime now = LocalDateTime.now();

        SanPham toSave = new SanPham();
        toSave.setIdKhuyenMai(source.getIdKhuyenMai());
        toSave.setMaSanPham(maSanPham);
        toSave.setTenSanPham(source.getTenSanPham());
        toSave.setMoTa(source.getMoTa());
        toSave.setNguoiTao(source.getNguoiTao());
        toSave.setNguoiSua(source.getNguoiSua());
        toSave.setTrangThai(normalizeText(source.getTrangThai()).isBlank() ? "Hoạt động" : source.getTrangThai());
        toSave.setNgayTao(now);
        toSave.setNgaySua(now);

        SanPham saved = sanPhamRepository.save(toSave);
        saveVariants(saved, source.getSanPhamChiTiets(), true);
        saved.setSanPhamChiTiets(sanPhamChiTietRepository.findBySanPhamId(saved.getId()));
        return saved;
    }

    private String generateNextProductCode() {
        int max = Math.max(0, sanPhamRepository.findMaxSanPhamCodeNumber() == null
                ? 0
                : sanPhamRepository.findMaxSanPhamCodeNumber());
        return "SP" + String.format("%03d", max + 1);
    }

    private String normalizeText(String value) {
        return String.valueOf(value == null ? "" : value).trim();
    }
}