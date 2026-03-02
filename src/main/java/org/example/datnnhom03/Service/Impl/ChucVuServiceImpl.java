package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.ChucVu;
import org.example.datnnhom03.Repository.ChucVuRepository;
import org.example.datnnhom03.Service.ChucVuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChucVuServiceImpl implements ChucVuService {

    @Autowired
    private ChucVuRepository chucVuRepository;

    @Override
    public ChucVu create(ChucVu chucVu) {
        chucVu.setNgaySua(LocalDateTime.now());
        return chucVuRepository.save(chucVu);
    }

    @Override
    public ChucVu update(Integer id, ChucVu chucVu) {
        ChucVu cv = chucVuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ"));

        cv.setMaChucVu(chucVu.getMaChucVu());
        cv.setTenChucVu(chucVu.getTenChucVu());
        cv.setTrangThai(chucVu.getTrangThai());
        cv.setNgaySua(LocalDateTime.now());

        return chucVuRepository.save(cv);
    }

    @Override
    public void delete(Integer id) {
        ChucVu cv = chucVuRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ"));

        cv.setTrangThai("Ngừng hoạt động");
        chucVuRepository.save(cv);
    }

    @Override
    public ChucVu findById(Integer id) {
        return chucVuRepository.findById(id).orElse(null);
    }

    @Override
    public List<ChucVu> findAll() {
        return chucVuRepository.findByTrangThai("Hoạt động");
    }
}