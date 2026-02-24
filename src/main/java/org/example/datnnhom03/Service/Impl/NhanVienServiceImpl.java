package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.NhanVien;
import org.example.datnnhom03.Repository.NhanVienRepository;
import org.example.datnnhom03.Service.NhanVienService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NhanVienServiceImpl implements NhanVienService {

    private final NhanVienRepository nhanVienRepository;

    public NhanVienServiceImpl(NhanVienRepository nhanVienRepository) {
        this.nhanVienRepository = nhanVienRepository;
    }

    @Override
    public NhanVien create(NhanVien nhanVien) {
        nhanVien.setNgayTao(LocalDateTime.now());
        nhanVien.setNgaySua(LocalDateTime.now());
        return nhanVienRepository.save(nhanVien);
    }

    @Override
    public NhanVien update(Integer id, NhanVien nhanVien) {
        NhanVien nv = nhanVienRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        return nhanVienRepository.save(nhanVien);
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