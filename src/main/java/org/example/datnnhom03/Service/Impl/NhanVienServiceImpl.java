package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.NhanVien;
import org.example.datnnhom03.Repsotiory.NhanVienRepository;
import org.example.datnnhom03.Service.NhanVienService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NhanVienServiceImpl implements NhanVienService {

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Override
    public NhanVien create(NhanVien nhanVien) {
        return nhanVienRepository.save(nhanVien);
    }

    @Override
    public NhanVien update(Integer id, NhanVien nhanVien) {
        NhanVien nv = nhanVienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        return nhanVienRepository.save(nv);
    }

    @Override
    public void delete(Integer id) {
        nhanVienRepository.deleteById(id);
    }

    @Override
    public NhanVien findById(Integer id) {
        return nhanVienRepository.findById(id).orElse(null);
    }

    @Override
    public List<NhanVien> findAll() {
        return nhanVienRepository.findAll();
    }
}
