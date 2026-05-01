package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.KhuyenMai;
import org.example.datnnhom03.Repository.KhuyenMaiRepository;
import org.example.datnnhom03.Service.KhuyenMaiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class KhuyenMaiServiceImpl implements KhuyenMaiService {

    @Autowired
    private KhuyenMaiRepository khuyenMaiRepository;

    @Override
    public KhuyenMai create(KhuyenMai khuyenMai) {
        String providedCode = normalizeText(khuyenMai.getMaKhuyenMai()).toUpperCase();
        int maxAttempts = providedCode.isBlank() ? 6 : 1;

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String codeToUse = providedCode.isBlank() ? generateNextKhuyenMaiCode() : providedCode;
            try {
                KhuyenMai toSave = new KhuyenMai();
                toSave.setMaKhuyenMai(codeToUse);
                toSave.setTenKhuyenMai(khuyenMai.getTenKhuyenMai());
                toSave.setGiaTri(khuyenMai.getGiaTri());
                toSave.setDonViGiam(resolveDonViGiam(khuyenMai.getDonViGiam()));
                toSave.setNgayBatDau(khuyenMai.getNgayBatDau());
                toSave.setNgayKetThuc(khuyenMai.getNgayKetThuc());
                toSave.setTrangThai(normalizeText(khuyenMai.getTrangThai()).isBlank() ? "Hoạt động" : khuyenMai.getTrangThai());
                return khuyenMaiRepository.save(toSave);
            } catch (DataIntegrityViolationException ex) {
                if (!providedCode.isBlank()) {
                    throw new RuntimeException("Ma khuyen mai da ton tai", ex);
                }
                lastFailure = new RuntimeException("Khong the tu dong sinh ma khuyen mai", ex);
            }
        }

        throw (lastFailure != null)
                ? lastFailure
                : new RuntimeException("Khong the tao khuyen mai");
    }

    @Override
    public KhuyenMai update(Integer id, KhuyenMai khuyenMai) {

        KhuyenMai existing = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));

        String maKhuyenMai = normalizeText(khuyenMai.getMaKhuyenMai());
        if (!maKhuyenMai.isBlank()) {
            existing.setMaKhuyenMai(maKhuyenMai.toUpperCase());
        }
        existing.setTenKhuyenMai(khuyenMai.getTenKhuyenMai());
        existing.setGiaTri(khuyenMai.getGiaTri());
        existing.setDonViGiam(resolveDonViGiam(khuyenMai.getDonViGiam()));
        existing.setNgayBatDau(khuyenMai.getNgayBatDau());
        existing.setNgayKetThuc(khuyenMai.getNgayKetThuc());
        existing.setTrangThai(khuyenMai.getTrangThai());

        return khuyenMaiRepository.save(existing);
    }

    @Override
    public void delete(Integer id) {
        KhuyenMai km = khuyenMaiRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));

        km.setTrangThai("Ngừng hoạt động");
        khuyenMaiRepository.save(km);
    }

    @Override
    public KhuyenMai findById(Integer id) {
        return khuyenMaiRepository.findById(id).orElse(null);
    }

    @Override
    public List<KhuyenMai> findAll() {
        return khuyenMaiRepository.findAll();
    }

    // ===== BỔ SUNG PHỤC VỤ HOME =====
    @Override
    public BigDecimal getGiaTriKhuyenMai(Integer khuyenMaiId) {
        return khuyenMaiRepository.findById(khuyenMaiId)
                .map(KhuyenMai::getGiaTri)
                .orElse(BigDecimal.ZERO);
    }

    private String generateNextKhuyenMaiCode() {
        int max = Math.max(0, khuyenMaiRepository.findMaxKhuyenMaiCodeNumber() == null
                ? 0
                : khuyenMaiRepository.findMaxKhuyenMaiCodeNumber());
        return "KM" + String.format("%03d", max + 1);
    }

    private String resolveDonViGiam(String donViGiam) {
        String normalized = normalizeText(donViGiam).toUpperCase();
        return normalized.isBlank() ? "PERCENT" : normalized;
    }

    private String normalizeText(String value) {
        return String.valueOf(value == null ? "" : value).trim();
    }
}