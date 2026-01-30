package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.KhachHang;
import org.example.datnnhom03.Model.NhanVien;

import java.util.List;

public interface NhanVienService {
    NhanVien create(NhanVien nhanVien);
    NhanVien update(Integer id, NhanVien nhanVien);
    void delete(Integer id);
    NhanVien findById(Integer id);
    List<NhanVien> findAll();
}
