package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.Loai;
import org.example.datnnhom03.Repository.LoaiRepository;
import org.example.datnnhom03.Service.LoaiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoaiServiceImpl implements LoaiService {

    @Autowired
    private LoaiRepository loaiRepository;

    @Override
    public Loai create(Loai loai) {
        loai.setNgayTao(LocalDateTime.now());
        loai.setNgaySua(LocalDateTime.now());
        return loaiRepository.save(loai);
    }

    @Override
    public Loai update(Integer id, Loai loai) {
        return loaiRepository.save(loai);
    }

    @Override
    public void delete(Integer id) {
        Loai loai = loaiRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại"));

        loai.setTrangThai("Ngừng hoạt động");
        loaiRepository.save(loai);
    }

    @Override
    public Loai findById(Integer id) {
        return loaiRepository.findById(id).orElse(null);
    }

    @Override
    public List<Loai> findAll() {
        return loaiRepository.findAll();
    }
}

