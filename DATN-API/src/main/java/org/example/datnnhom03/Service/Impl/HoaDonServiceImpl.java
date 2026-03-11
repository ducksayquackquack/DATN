package org.example.datnnhom03.Service.Impl;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Model.TrangThaiHoaDon;
import org.example.datnnhom03.Repository.HoaDonRepository;
import org.example.datnnhom03.Service.HoaDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HoaDonServiceImpl implements HoaDonService {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Override
    public List<HoaDon> findByKhachHangId(Integer id) {
        return hoaDonRepository.findByKhachHang_Id(id);
    }

    @Override
    public Page<HoaDon> findAll(Pageable pageable) {
        return hoaDonRepository.findAll(pageable);
    }


    @Override
    public HoaDon create(HoaDon hoaDon) {
        hoaDon.setNgayTao(LocalDateTime.now());
        hoaDon.setTrangThai(TrangThaiHoaDon.CHO_THANH_TOAN);
        hoaDon.setNgayHuy(null);
        return hoaDonRepository.save(hoaDon);
    }

    @Override
    public HoaDon update(Integer id, HoaDon hoaDon) {

        HoaDon hd = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        hd.setTrangThai(hoaDon.getTrangThai());

        return hoaDonRepository.save(hd);
    }

    @Override
    public void delete(Integer id) {

        HoaDon hd = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));


        if (hd.getTrangThai() == TrangThaiHoaDon.DA_HUY) {
            throw new RuntimeException("Hóa đơn đã bị hủy rồi");
        }


        if (hd.getTrangThai() == TrangThaiHoaDon.DA_HOAN_THANH) {
            throw new RuntimeException("Không thể hủy đơn đã hoàn thành");
        }


        hd.setTrangThai(TrangThaiHoaDon.DA_HUY);


        hd.setNgayHuy(LocalDateTime.now());

        hoaDonRepository.save(hd);
    }

    @Override
    public HoaDon findById(Integer id) {
        return hoaDonRepository.findById(id).orElse(null);
    }

    @Override
    public List<HoaDon> findAll() {
        return hoaDonRepository.findAll();
    }

    @Override
    public HoaDon save(HoaDon hoaDon) {
        return hoaDonRepository.save(hoaDon);
    }
}