package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.DiaChi;
import org.example.datnnhom03.Repository.DiaChiRepository;
import org.example.datnnhom03.Service.DiaChiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiaChiServiceImpl implements DiaChiService {

    @Autowired
    private DiaChiRepository diaChiRepository;

    @Override
    public DiaChi create(DiaChi diaChi) {
        return diaChiRepository.save(diaChi);
    }

    @Override
    public DiaChi update(Integer id, DiaChi diaChi) {
        DiaChi dc = diaChiRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        dc.setKhachHang(diaChi.getKhachHang());
        dc.setDiaChiCuThe(diaChi.getDiaChiCuThe());
        dc.setTinhThanh(diaChi.getTinhThanh());
        dc.setQuanHuyen(diaChi.getQuanHuyen());
        dc.setPhuongXa(diaChi.getPhuongXa());
        dc.setTrangThai(diaChi.getTrangThai());

        return diaChiRepository.save(dc);
    }

    @Override
    public void delete(Integer id) {
        DiaChi dc = diaChiRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        dc.setTrangThai("Ngừng hoạt động");
        diaChiRepository.save(dc);
    }

    @Override
    public DiaChi findById(Integer id) {
        return diaChiRepository.findById(id).orElse(null);
    }

    @Override
    public List<DiaChi> findAll() {
        return diaChiRepository.findByTrangThai("Hoạt động");
    }

    @Override
    public List<DiaChi> findByKhachHangId(Integer khachHangId) {
        return diaChiRepository.findByKhachHangId(khachHangId);
    }
}