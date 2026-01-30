package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.SanPham;

import java.util.List;

public interface SanPhamService {

    SanPham create(SanPham sanPham);

    SanPham update(Integer id, SanPham sanPham);

    void delete(Integer id);

    SanPham findById(Integer id);

    List<SanPham> findAll();
}
