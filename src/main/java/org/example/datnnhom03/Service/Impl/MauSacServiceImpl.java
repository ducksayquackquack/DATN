package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.MauSac;
import org.example.datnnhom03.Repository.MauSacRepository;
import org.example.datnnhom03.Service.MauSacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MauSacServiceImpl implements MauSacService {

    @Autowired
    private MauSacRepository mauSacRepository;

    @Override
    public MauSac create(MauSac mauSac) {
        mauSac.setNgayTao(LocalDateTime.now());
        mauSac.setNgaySua(LocalDateTime.now());
        return mauSacRepository.save(mauSac);
    }

    @Override
    public MauSac update(Integer id, MauSac mauSac) {

        MauSac existing = mauSacRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu sắc"));

        existing.setMaMau(mauSac.getMaMau());
        existing.setTenMau(mauSac.getTenMau());
        existing.setTrangThai(mauSac.getTrangThai());
        existing.setNgaySua(LocalDateTime.now());

        return mauSacRepository.save(existing);
    }

    @Override
    public void delete(Integer id) {
        MauSac ms = mauSacRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy màu sắc"));

        ms.setTrangThai("Ngừng hoạt động");
        mauSacRepository.save(ms);
    }

    @Override
    public MauSac findById(Integer id) {
        return mauSacRepository.findById(id).orElse(null);
    }

    @Override
    public List<MauSac> findAll() {
        return mauSacRepository.findAll();
    }
}

