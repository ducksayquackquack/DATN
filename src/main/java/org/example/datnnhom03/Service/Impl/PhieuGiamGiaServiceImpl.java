package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.PhieuGiamGia;
import org.example.datnnhom03.Repository.PhieuGiamGiaRepository;
import org.example.datnnhom03.Service.PhieuGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class PhieuGiamGiaServiceImpl implements PhieuGiamGiaService {

    @Autowired
    private PhieuGiamGiaRepository repository;

    @Override
    public PhieuGiamGia create(PhieuGiamGia phieuGiamGia) {
        return repository.save(phieuGiamGia);
    }

    @Override
    public PhieuGiamGia update(Integer id, PhieuGiamGia phieuGiamGia) {

        PhieuGiamGia existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu giảm giá"));

        existing.setMaPhieuGiamGia(phieuGiamGia.getMaPhieuGiamGia());
        existing.setTenPhieuGiamGia(phieuGiamGia.getTenPhieuGiamGia());
        existing.setLoaiPhieuGiamGia(phieuGiamGia.getLoaiPhieuGiamGia());
        existing.setHinhThucGiam(phieuGiamGia.getHinhThucGiam());
        existing.setGiaTriGiamGia(phieuGiamGia.getGiaTriGiamGia());
        existing.setHoaDonToiThieu(phieuGiamGia.getHoaDonToiThieu());
        existing.setSoLuongSuDung(phieuGiamGia.getSoLuongSuDung());
        existing.setNgayBatDau(phieuGiamGia.getNgayBatDau());
        existing.setNgayKetThuc(phieuGiamGia.getNgayKetThuc());
        existing.setTrangThai(phieuGiamGia.getTrangThai());

        return repository.save(existing);
    }

    @Override
    public void delete(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public PhieuGiamGia findById(Integer id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<PhieuGiamGia> findAll() {
        return repository.findAll();
    }

    @Override
    public List<PhieuGiamGia> findActive(Integer limit) {
        final LocalDate today = LocalDate.now();
        final int maxItems = (limit == null || limit <= 0) ? 4 : limit;

        return repository.findAll().stream()
                .filter(v -> v != null)
                .filter(v -> v.getNgayBatDau() == null || !today.isBefore(v.getNgayBatDau()))
                .filter(v -> v.getNgayKetThuc() == null || !today.isAfter(v.getNgayKetThuc()))
                .filter(v -> v.getSoLuongSuDung() == null || v.getSoLuongSuDung() > 0)
                .sorted(Comparator.comparing(PhieuGiamGia::getNgayKetThuc, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PhieuGiamGia::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(maxItems)
                .toList();
    }
}