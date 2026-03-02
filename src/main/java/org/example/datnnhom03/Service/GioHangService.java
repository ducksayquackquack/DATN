package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.GioHang;

import java.util.List;

public interface GioHangService {

    GioHang create(GioHang gioHang);

    GioHang update(Integer id, GioHang gioHang);

    void delete(Integer id);

    GioHang findById(Integer id);

    List<GioHang> findAll();

    GioHang findByKhachHangId(Integer khachHangId);
}