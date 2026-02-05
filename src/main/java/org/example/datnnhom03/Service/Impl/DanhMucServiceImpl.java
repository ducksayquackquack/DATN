package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.DanhMuc;
import org.example.datnnhom03.Repository.DanhMucRepository;
import org.example.datnnhom03.Service.DanhMucService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DanhMucServiceImpl implements DanhMucService {

    private final DanhMucRepository repository;

    public DanhMucServiceImpl(DanhMucRepository repository) {
        this.repository = repository;
    }

    @Override
    public DanhMuc create(DanhMuc danhMuc) {
        return repository.save(danhMuc);
    }

    @Override
    public DanhMuc update(Integer id, DanhMuc danhMuc) {
        danhMuc.setId(id);
        return repository.save(danhMuc);
    }

    @Override
    public void delete(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public DanhMuc findById(Integer id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<DanhMuc> findAll() {
        return repository.findAll();
    }
}
