package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.HoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface HoaDonService {

    HoaDon create(HoaDon hoaDon);

    HoaDon update(Integer id, HoaDon hoaDon);

    void delete(Integer id);

    HoaDon findById(Integer id);

    List<HoaDon> findAll();   // old version (can keep if needed)

    Page<HoaDon> findAll(Pageable pageable);  // new pagination version

    List<HoaDon> findByKhachHangId(Integer id);

    HoaDon save(HoaDon hoaDon);
}