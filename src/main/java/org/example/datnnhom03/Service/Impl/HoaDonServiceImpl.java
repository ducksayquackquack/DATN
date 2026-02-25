package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Repository.HoaDonRepsitory;
import org.example.datnnhom03.Service.HoaDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HoaDonServiceImpl implements HoaDonService {

    @Autowired
    private HoaDonRepsitory hoaDonRepsitory;

    @Override
    public HoaDon create(HoaDon hoaDon) {
        hoaDon.setNgayTao(LocalDateTime.now());
        hoaDon.setNgayHuy(LocalDateTime.now());
        return hoaDonRepsitory.save(hoaDon);
    }

    @Override
    public HoaDon update(Integer id, HoaDon hoaDon) {

        HoaDon hd = hoaDonRepsitory.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        hd.setMaHoaDon(hoaDon.getMaHoaDon());
        hd.setTrangThai(hoaDon.getTrangThai());
        hd.setNgayHuy(LocalDateTime.now());

        return hoaDonRepsitory.save(hd);
    }

    @Override
    public void delete(Integer id) {

        HoaDon hd = hoaDonRepsitory.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        hd.setTrangThai("Đã hủy");
        hoaDonRepsitory.save(hd);
    }

    @Override
    public HoaDon findById(Integer id) {
        return hoaDonRepsitory.findById(id).orElse(null);
    }

    @Override
    public List<HoaDon> findAll() {
        return hoaDonRepsitory.findAll();
    }
}