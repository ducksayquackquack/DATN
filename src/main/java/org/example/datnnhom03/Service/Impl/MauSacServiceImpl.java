package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.MauSac;
import org.example.datnnhom03.Repository.MauSacRepository;
import org.example.datnnhom03.Service.MauSacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MauSacServiceImpl implements MauSacService {

    @Autowired
    private MauSacRepository mauSacRepository;

    @Override
    public MauSac create(MauSac mauSac) {
        return mauSacRepository.save(mauSac);
    }

    @Override
    public MauSac update(Integer id, MauSac mauSac) {
        return mauSacRepository.save(mauSac);
    }

    @Override
    public void delete(Integer id) {
        mauSacRepository.deleteById(id);
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

