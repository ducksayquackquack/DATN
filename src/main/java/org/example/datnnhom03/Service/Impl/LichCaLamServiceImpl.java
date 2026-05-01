package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.LichCaLam;
import org.example.datnnhom03.Repository.LichCaLamRepository;
import org.example.datnnhom03.Service.LichCaLamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LichCaLamServiceImpl implements LichCaLamService {

    @Autowired
    private LichCaLamRepository repository;

    @Override
    public List<LichCaLam> findAll() {
        return repository.findAll();
    }

    @Override
    public LichCaLam create(LichCaLam caLam) {
        return repository.save(caLam);
    }

    @Override
    public LichCaLam update(Integer id, LichCaLam caLam) {
        LichCaLam ca = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm"));

        ca.setTenCa(caLam.getTenCa());
        ca.setMoTa(caLam.getMoTa());
        ca.setGioBatDau(caLam.getGioBatDau());
        ca.setGioKetThuc(caLam.getGioKetThuc());
        ca.setTrangThai(caLam.getTrangThai());

        return repository.save(ca);
    }

    @Override
    public void delete(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public LichCaLam findById(Integer id) {
        return repository.findById(id).orElse(null);
    }
}