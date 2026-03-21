package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Model.SanPhamChiTiet;
import org.example.datnnhom03.Model.MauSac;
import org.example.datnnhom03.Model.KichThuoc;
import org.example.datnnhom03.Repository.SanPhamChiTietRepository;
import org.example.datnnhom03.Repository.SanPhamRepository;
import org.example.datnnhom03.Repository.MauSacRepository;
import org.example.datnnhom03.Repository.KichThuocRepository;
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

    @Autowired
    private MauSacRepository mauSacRepository;

    @Autowired
    private KichThuocRepository kichThuocRepository;

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
        // Load tất cả biến thể hiện có của sản phẩm này vào map
        Map<Integer, SanPhamChiTiet> existingById = new HashMap<>();
        Map<String, SanPhamChiTiet> existingByMa = new HashMap<>();
        for (SanPhamChiTiet existing : sanPhamChiTietRepository.findBySanPhamId(sanPham.getId())) {
            if (existing.getId() != null) {
                existingById.put(existing.getId(), existing);
            }
            if (existing.getMa() != null && !existing.getMa().isBlank()) {
                existingByMa.put(existing.getMa().trim().toUpperCase(), existing);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (SanPhamChiTiet incoming : incomingVariants) {
            SanPhamChiTiet target = null;

            // 1. Tìm theo id trong map biến thể của sản phẩm này
            if (incoming.getId() != null) {
                target = existingById.get(incoming.getId());
            }

            // 2. Tìm theo mã trong map biến thể của sản phẩm này
            if (target == null && incoming.getMa() != null && !incoming.getMa().isBlank()) {
                target = existingByMa.get(incoming.getMa().trim().toUpperCase());
            }

            if (target != null) {
                // UPDATE: map fields vào managed entity đã load sẵn
                mapVariantFields(target, incoming);
                target.setNgaySua(now);
                sanPhamChiTietRepository.save(target);
            } else {
                // INSERT: chỉ tạo mới khi thực sự không tồn tại
                SanPhamChiTiet newVariant = new SanPhamChiTiet();
                newVariant.setSanPham(sanPham);
                newVariant.setNgayTao(now);
                newVariant.setNgaySua(now);
                mapVariantFields(newVariant, incoming);
                sanPhamChiTietRepository.save(newVariant);
            }
        }
    }

    private void mapVariantFields(SanPhamChiTiet target, SanPhamChiTiet source) {
        target.setMa(source.getMa());
        target.setGiaNhap(source.getGiaNhap());
        target.setGiaBan(source.getGiaBan());
        target.setSoLuong(source.getSoLuong());
        target.setChatLieu(source.getChatLieu());
        target.setMauSac(resolveMauSac(source.getMauSac()));
        target.setKichThuoc(resolveKichThuoc(source.getKichThuoc()));
        target.setHang(source.getHang());
        target.setXuatSu(source.getXuatSu());
        target.setDanhMuc(source.getDanhMuc());
        target.setLoai(source.getLoai());
        target.setNguoiTao(source.getNguoiTao());
        target.setNguoiSua(source.getNguoiSua());
        target.setTrangThai(source.getTrangThai());
        target.setBarcode(source.getBarcode());
        target.setHinhAnh(source.getHinhAnh());
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

    /**
     * Resolve MauSac: ưu tiên theo id, fallback tìm theo tên, nếu không có thì tạo mới
     */
    private MauSac resolveMauSac(MauSac source) {
        if (source == null) return null;
        if (source.getId() != null && source.getId() > 0) {
            return mauSacRepository.findById(source.getId()).orElse(null);
        }
        if (source.getTenMau() != null && !source.getTenMau().isBlank()) {
            return mauSacRepository.findByTenMauIgnoreCase(source.getTenMau().trim())
                    .orElseGet(() -> {
                        MauSac newMau = new MauSac();
                        newMau.setTenMau(source.getTenMau().trim());
                        newMau.setMaMau("MS" + System.currentTimeMillis());
                        newMau.setTrangThai("Hoạt động");
                        newMau.setNgayTao(LocalDateTime.now());
                        newMau.setNgaySua(LocalDateTime.now());
                        return mauSacRepository.save(newMau);
                    });
        }
        return null;
    }

    /**
     * Resolve KichThuoc: ưu tiên theo id, fallback tìm theo tên, nếu không có thì tạo mới
     */
    private KichThuoc resolveKichThuoc(KichThuoc source) {
        if (source == null) return null;
        if (source.getId() != null && source.getId() > 0) {
            return kichThuocRepository.findById(source.getId()).orElse(null);
        }
        if (source.getTenKichThuoc() != null && !source.getTenKichThuoc().isBlank()) {
            return kichThuocRepository.findByTenKichThuocIgnoreCase(source.getTenKichThuoc().trim())
                    .orElseGet(() -> {
                        KichThuoc newKich = new KichThuoc();
                        newKich.setTenKichThuoc(source.getTenKichThuoc().trim());
                        newKich.setMaKichThuoc("KT" + System.currentTimeMillis());
                        newKich.setTrangThai("Hoạt động");
                        newKich.setNgayTao(LocalDateTime.now());
                        newKich.setNgaySua(LocalDateTime.now());
                        return kichThuocRepository.save(newKich);
                    });
        }
        return null;
    }
}