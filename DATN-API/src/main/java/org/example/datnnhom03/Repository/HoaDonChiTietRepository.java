package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.HoaDonChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {
//    List<HoaDonChiTiet> findByHoaDon_Id(Integer id);
    List<HoaDonChiTiet> findByHoaDonId(Integer id);
}