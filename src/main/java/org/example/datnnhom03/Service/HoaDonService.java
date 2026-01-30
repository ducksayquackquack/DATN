package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.HoaDon;

import java.util.List;

public interface HoaDonService {
    HoaDon create(HoaDon hoaDon);
    HoaDon update(Integer id, HoaDon hoaDon);
    void delete(Integer id);
    HoaDon findById(Integer id);
    List<HoaDon> findAll();
}
