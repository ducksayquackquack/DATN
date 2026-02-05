package org.example.datnnhom03.Service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.datnnhom03.Model.Loai;
import org.example.datnnhom03.Repository.LoaiRepository;
import org.example.datnnhom03.Service.LoaiService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoaiServiceImpl implements LoaiService {

    private final LoaiRepository repository;

    @Override
    public Loai create(Loai loai) {
        loai.setNgayTao(LocalDateTime.now());
        return repository.save(loai);
    }

    @Override
    public Loai update(Integer id, Loai loai) {
        Loai existing = repository.findById(id).orElse(null);
        if (existing == null) {
            return null;
        }

        loai.setId(id);
        loai.setNgayTao(existing.getNgayTao());
        loai.setNgaySua(LocalDateTime.now());
        return repository.save(loai);
    }

    @Override
    public void delete(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public Loai findById(Integer id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<Loai> findAll() {
        return repository.findAll();
    }
}
