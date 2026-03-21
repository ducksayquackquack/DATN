package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.PTTT;
import org.example.datnnhom03.Repository.PTTTRepository;
import org.example.datnnhom03.Service.PTTTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PTTTServiceImpl implements PTTTService {

    @Autowired
    private PTTTRepository ptttRepository;

    @Override
    public PTTT create(PTTT pttt) {
        pttt.setNgayTao(LocalDateTime.now());
        pttt.setNgaySua(LocalDateTime.now());
        return ptttRepository.save(pttt);
    }

    @Override
    public PTTT update(Integer id, PTTT pttt) {
        PTTT existing = ptttRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phương thức thanh toán"));

        existing.setMa(pttt.getMa());
        existing.setTen(pttt.getTen());
        existing.setMoTa(pttt.getMoTa());
        existing.setTrangThai(pttt.getTrangThai());
        existing.setNgaySua(LocalDateTime.now());

        return ptttRepository.save(existing);
    }

    @Override
    public void delete(Integer id) {
        ptttRepository.deleteById(id);
    }

    @Override
    public PTTT findById(Integer id) {
        return ptttRepository.findById(id).orElse(null);
    }

    @Override
    public List<PTTT> findAll() {
        return ptttRepository.findAll();
    }
}

