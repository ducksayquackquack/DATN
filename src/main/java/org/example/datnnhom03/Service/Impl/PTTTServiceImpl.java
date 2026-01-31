package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.PTTT;
import org.example.datnnhom03.Repository.PTTTRepository;
import org.example.datnnhom03.Service.PTTTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PTTTServiceImpl implements PTTTService {

    @Autowired
    private PTTTRepository ptttRepository;

    @Override
    public PTTT create(PTTT pttt) {
        return ptttRepository.save(pttt);
    }

    @Override
    public PTTT update(Integer id, PTTT pttt) {
        return ptttRepository.save(pttt);
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

