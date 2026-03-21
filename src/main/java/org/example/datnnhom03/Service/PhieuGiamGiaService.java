package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.PhieuGiamGia;

import java.util.List;

public interface PhieuGiamGiaService {

    PhieuGiamGia create(PhieuGiamGia phieuGiamGia);

    PhieuGiamGia update(Integer id, PhieuGiamGia phieuGiamGia);

    void delete(Integer id);

    PhieuGiamGia findById(Integer id);

    List<PhieuGiamGia> findAll();

    List<PhieuGiamGia> findActive();
}