package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.KhuyenMai;
import org.example.datnnhom03.Repository.KhuyenMaiRepository;
import org.example.datnnhom03.Service.KhuyenMaiService;
import org.springframework.beans.factory.annotation.Autowired;
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
        khuyenMai.setNgayBatDau(LocalDateTime.now());
        khuyenMai.setNgayKetThuc(LocalDateTime.now());
        return khuyenMaiRepository.save(khuyenMai);
    }

    @Override
    public KhuyenMai update(Integer id, KhuyenMai khuyenMai) {
        return khuyenMaiRepository.save(khuyenMai);
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
}