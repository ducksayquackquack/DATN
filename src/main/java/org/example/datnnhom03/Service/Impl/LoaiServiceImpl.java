package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.Loai;
import org.example.datnnhom03.Repository.LoaiRepository;
import org.example.datnnhom03.Service.LoaiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoaiServiceImpl implements LoaiService {

    @Autowired
    private LoaiRepository loaiRepository;

    @Override
    public Loai create(Loai loai) {
        return loaiRepository.save(loai);
    }

    @Override
    public Loai update(Integer id, Loai loai) {
        return loaiRepository.save(loai);
    }

    @Override
    public void delete(Integer id) {
        loaiRepository.deleteById(id);
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

