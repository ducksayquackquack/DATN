package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Repository.SanPhamRepository;
import org.example.datnnhom03.Service.SanPhamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SanPhamServiceImpl implements SanPhamService {

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Override
    public SanPham create(SanPham sanPham) {
        sanPham.setNgayTao(LocalDateTime.now());
        return sanPhamRepository.save(sanPham);
    }

    @Override
    public SanPham update(Integer id, SanPham sanPham) {
        SanPham sp = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        sp.setTenSanPham(sanPham.getTenSanPham());
        sp.setMoTa(sanPham.getMoTa());
        sp.setTrangThai(sanPham.getTrangThai());
        sp.setNgaySua(LocalDateTime.now());

        return sanPhamRepository.save(sp);
    }

    @Override
    public void delete(Integer id) {
        sanPhamRepository.deleteById(id);
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
}
