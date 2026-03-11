package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface HoaDonRepository
        extends JpaRepository<HoaDon, Integer>,
        JpaSpecificationExecutor<HoaDon> {

    List<HoaDon> findByTrangThai(String trangThai);
    List<HoaDon> findByKhachHang_Id(Integer id);

    long countByKhachHang_Id(Integer id);
}