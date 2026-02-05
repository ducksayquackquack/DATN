package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.KichThuoc;
import org.example.datnnhom03.Repository.KichThuocRepository;
import org.example.datnnhom03.Service.KichThuocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KichThuocServiceImpl implements KichThuocService {

    @Autowired
    private KichThuocRepository kichThuocRepository;

    @Override
    public KichThuoc create(KichThuoc kichThuoc) {
        return kichThuocRepository.save(kichThuoc);
    }

    @Override
    public KichThuoc update(Integer id, KichThuoc kichThuoc) {
        return kichThuocRepository.save(kichThuoc);
    }

    @Override
    public void delete(Integer id) {
        kichThuocRepository.deleteById(id);
    }

    @Override
    public KichThuoc findById(Integer id) {
        return kichThuocRepository.findById(id).orElse(null);
    }

    @Override
    public List<KichThuoc> findAll() {
        return kichThuocRepository.findAll();
    }
}

