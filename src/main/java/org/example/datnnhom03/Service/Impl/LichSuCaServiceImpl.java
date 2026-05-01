package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.LichSuCa;
import org.example.datnnhom03.Repository.LichSuCaRepository;
import org.example.datnnhom03.Service.LichSuCaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LichSuCaServiceImpl implements LichSuCaService {

    @Autowired
    private LichSuCaRepository repository;

    @Override
    public List<LichSuCa> findAll() {
        return repository.findAll();
    }

    @Override
    public LichSuCa create(LichSuCa lichSuCa) {
        return repository.save(lichSuCa);
    }

    @Override
    public LichSuCa update(Integer id, LichSuCa lichSuCa) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setIdNhanVien(lichSuCa.getIdNhanVien());
                    existing.setIdCaLam(lichSuCa.getIdCaLam());
                    existing.setNgay(lichSuCa.getNgay());
                    existing.setTienCa(lichSuCa.getTienCa());
                    existing.setTienDauCa(lichSuCa.getTienDauCa());
                    existing.setDoanhThu(lichSuCa.getDoanhThu());
                    existing.setTienChuyenKhoan(lichSuCa.getTienChuyenKhoan());
                    existing.setGhiChu(lichSuCa.getGhiChu());
                    existing.setTrangThai(lichSuCa.getTrangThai());
                    return repository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch sử ca với id = " + id));
    }

    @Override
    public void delete(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public List<LichSuCa> findByNhanVien(Integer idNhanVien) {
        return repository.findByIdNhanVien(idNhanVien);
    }
}