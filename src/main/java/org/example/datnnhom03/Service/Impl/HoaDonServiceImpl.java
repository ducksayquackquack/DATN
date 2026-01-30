package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Model.NhanVien;
import org.example.datnnhom03.Repsotiory.HoaDonRepsitory;
import org.example.datnnhom03.Repsotiory.NhanVienRepository;
import org.example.datnnhom03.Service.HoaDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HoaDonServiceImpl implements HoaDonService {

    @Autowired
    private HoaDonRepsitory hoaDonRepsitory;

    @Override
    public HoaDon create(HoaDon hoaDon) {
        return hoaDonRepsitory.save(hoaDon);
    }

    @Override
    public HoaDon update(Integer id, HoaDon hoaDon) {
        HoaDon hd = hoaDonRepsitory.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        return hoaDonRepsitory.save(hd);
    }

    @Override
    public void delete(Integer id) {
        hoaDonRepsitory.deleteById(id);
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
