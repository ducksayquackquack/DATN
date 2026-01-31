package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.DanhMuc;
import org.example.datnnhom03.Repository.DanhMucRepository;
import org.example.datnnhom03.Service.DanhMucService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DanhMucServiceImpl implements DanhMucService {

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Override
    public List<DanhMuc> findAll() {
        return danhMucRepository.findAll();
    }

    @Override
    public DanhMuc findById(Integer id) {
        return danhMucRepository.findById(id).orElse(null);
    }

    @Override
    public DanhMuc save(DanhMuc danhMuc) {
        return danhMucRepository.save(danhMuc);
    }

    @Override
    public void delete(Integer id) {
        danhMucRepository.deleteById(id);
    }
}

