package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.DanhMuc;
import org.example.datnnhom03.Repository.DanhMucRepository;
import org.example.datnnhom03.Service.DanhMucService;
import org.example.datnnhom03.dto.home.DanhMucDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DanhMucServiceImpl implements DanhMucService {

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Override
    public DanhMuc create(DanhMuc danhMuc) {
        danhMuc.setNgayTao(LocalDateTime.now());
        danhMuc.setNgaySua(LocalDateTime.now());
        return danhMucRepository.save(danhMuc);
    }

    @Override
    public DanhMuc update(Integer id, DanhMuc danhMuc) {
        DanhMuc dm = danhMucRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        dm.setMaDanhMuc(danhMuc.getMaDanhMuc());
        dm.setTenDanhMuc(danhMuc.getTenDanhMuc());
        dm.setTrangThai(danhMuc.getTrangThai());
        dm.setNgaySua(LocalDateTime.now());

        return danhMucRepository.save(dm);
    }

    @Override
    public void delete(Integer id) {
        DanhMuc dm = danhMucRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        dm.setTrangThai("Ngừng hoạt động");
        danhMucRepository.save(dm);
    }

    @Override
    public DanhMuc findById(Integer id) {
        return danhMucRepository.findById(id).orElse(null);
    }

    @Override
    public List<DanhMuc> findAll() {
        return danhMucRepository.findByTrangThai("Hoạt động");
    }

    // ===== BỔ SUNG PHỤC VỤ HOME =====
    @Override
    public List<DanhMucDTO> getActiveDanhMucDTO() {
        List<DanhMuc> danhMucs = danhMucRepository.findByTrangThai("Hoạt động");
        List<DanhMucDTO> result = new ArrayList<>();

        for (DanhMuc dm : danhMucs) {
            DanhMucDTO dto = new DanhMucDTO();
            dto.setId(dm.getId());
            dto.setMaDanhMuc(dm.getMaDanhMuc());
            dto.setTenDanhMuc(dm.getTenDanhMuc());
            result.add(dto);
        }
        return result;
    }
}