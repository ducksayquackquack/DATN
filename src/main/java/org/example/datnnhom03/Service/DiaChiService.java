package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.DiaChi;

import java.util.List;

public interface DiaChiService {

    DiaChi create(DiaChi diaChi);

    DiaChi update(Integer id, DiaChi diaChi);

    void delete(Integer id);

    DiaChi findById(Integer id);

    List<DiaChi> findAll();

    List<DiaChi> findByKhachHangId(Integer khachHangId);
}