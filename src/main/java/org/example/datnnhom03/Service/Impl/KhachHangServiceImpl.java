package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.KhachHang;
import org.example.datnnhom03.Repository.KhachHangRepository;
import org.example.datnnhom03.Service.KhachHangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KhachHangServiceImpl implements KhachHangService {

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Override
    public KhachHang create(KhachHang khachHang) {
        Integer taiKhoanId = khachHang.getTaiKhoan() != null ? khachHang.getTaiKhoan().getId() : null;
        if (taiKhoanId == null) {
            throw new RuntimeException("Thiếu id tài khoản cho khách hàng");
        }

        KhachHang existing = khachHangRepository.findByTaiKhoan_Id(taiKhoanId).orElse(null);
        KhachHang entity = existing != null ? existing : khachHang;

        if (existing != null) {
            if (khachHang.getTenKhachHang() != null) existing.setTenKhachHang(khachHang.getTenKhachHang());
            if (khachHang.getGioiTinh() != null) existing.setGioiTinh(khachHang.getGioiTinh());
            if (khachHang.getNgaySinh() != null) existing.setNgaySinh(khachHang.getNgaySinh());
            if (khachHang.getSoDienThoai() != null) existing.setSoDienThoai(khachHang.getSoDienThoai());
            if (khachHang.getTrangThai() != null) existing.setTrangThai(khachHang.getTrangThai());
            if (khachHang.getMaKhachHang() != null && !khachHang.getMaKhachHang().isBlank()) {
                existing.setMaKhachHang(khachHang.getMaKhachHang());
            }
        }

        if (entity.getTrangThai() == null || entity.getTrangThai().isBlank()) {
            entity.setTrangThai("Hoạt động");
        }
        if (entity.getMaKhachHang() == null || entity.getMaKhachHang().isBlank()) {
            entity.setMaKhachHang("TMP-" + System.nanoTime());
        }

        KhachHang saved = khachHangRepository.save(entity);

        if (saved.getMaKhachHang() == null || saved.getMaKhachHang().startsWith("TMP-")) {
            saved.setMaKhachHang(String.format("KH%03d", saved.getId()));
            saved = khachHangRepository.save(saved);
        }

        return saved;
    }

    @Override
    public KhachHang update(Integer id, KhachHang khachHang) {

        KhachHang kh = khachHangRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        kh.setMaKhachHang(khachHang.getMaKhachHang());
        kh.setTenKhachHang(khachHang.getTenKhachHang());
        kh.setGioiTinh(khachHang.getGioiTinh());
        kh.setNgaySinh(khachHang.getNgaySinh());
        kh.setSoDienThoai(khachHang.getSoDienThoai());
        kh.setTrangThai(khachHang.getTrangThai());

        return khachHangRepository.save(kh);
    }

    @Override
    public void delete(Integer id) {
        khachHangRepository.deleteById(id);
    }

    @Override
    public KhachHang findById(Integer id) {
        return khachHangRepository.findById(id).orElse(null);
    }

    @Override
    public List<KhachHang> findAll() {
        return khachHangRepository.findAll();
    }

    @Override
    public Page<KhachHang> findAll(Pageable pageable) {
        return khachHangRepository.findAll(pageable);
    }
}