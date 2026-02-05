package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.KhachHang;

import java.util.List;

public interface KhachHangService {
    KhachHang create(KhachHang khachHang);
    KhachHang update(Integer id, KhachHang khachHang);
    void delete(Integer id);
    KhachHang findById(Integer id);
    List<KhachHang> findAll();
}

