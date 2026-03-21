package org.example.datnnhom03.Service.Impl;


import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Model.HoaDonChiTiet;
import org.example.datnnhom03.Repository.HoaDonChiTietRepository;
import org.example.datnnhom03.Repository.HoaDonRepository;
import org.example.datnnhom03.Service.HoaDonTinhTienService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.math.BigDecimal;
import java.util.List;

@Service
public class HoaDonTinhTienServiceImpl implements HoaDonTinhTienService {
    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;

    public HoaDonTinhTienServiceImpl(HoaDonRepository hoaDonRepository,
                                     HoaDonChiTietRepository hoaDonChiTietRepository) {
        this.hoaDonRepository = hoaDonRepository;
        this.hoaDonChiTietRepository = hoaDonChiTietRepository;
    }

    @Override
    @Transactional
    public void recalc(Integer hoaDonId) {
        HoaDon hd = hoaDonRepository.findById(hoaDonId).orElseThrow();
        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(hoaDonId);

        BigDecimal sumItems = BigDecimal.ZERO;
        for (HoaDonChiTiet it : items) {
            sumItems = sumItems.add(it.getThanhTien() == null ? BigDecimal.ZERO : it.getThanhTien());
        }

        BigDecimal ship = hd.getPhiShip() == null ? BigDecimal.ZERO : hd.getPhiShip();
        BigDecimal giam = hd.getGiaSauGiamGia() == null ? BigDecimal.ZERO : hd.getGiaSauGiamGia();

        BigDecimal total = sumItems.add(ship).subtract(giam);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        hd.setThanhTien(total);
        hoaDonRepository.save(hd);
    }
}
