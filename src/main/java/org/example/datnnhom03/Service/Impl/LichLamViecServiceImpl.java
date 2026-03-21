package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.LichLamViec;
import org.example.datnnhom03.Repository.LichLamViecRepository;
import org.example.datnnhom03.Service.LichLamViecService;
import org.example.datnnhom03.dto.LichLamViecDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LichLamViecServiceImpl implements LichLamViecService {

    @Autowired
    private LichLamViecRepository repository;

    @Override
    public List<LichLamViec> findAll() {
        return repository.findAll();
    }

    @Override
    public List<LichLamViec> findByNhanVien(Integer idNhanVien) {
        return repository.findByIdNhanVien(idNhanVien);
    }

    @Override
    public LichLamViec create(LichLamViec lichLamViec) {
        return repository.save(lichLamViec);
    }

    @Override
    public void delete(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public List<LichLamViecDTO> getFullSchedule() {
        return repository.getFullSchedule();
    }
}