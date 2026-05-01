package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.KhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface KhachHangService {

    KhachHang create(KhachHang khachHang);

    KhachHang update(Integer id, KhachHang khachHang);

    void delete(Integer id);

    KhachHang findById(Integer id);

    List<KhachHang> findAll();

    Page<KhachHang> findAll(Pageable pageable);

    Optional<KhachHang> findByTaiKhoanId(Integer taiKhoanId);
}