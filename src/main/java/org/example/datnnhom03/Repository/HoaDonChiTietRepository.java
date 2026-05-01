package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.HoaDonChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {

    List<HoaDonChiTiet> findByHoaDon_Id(Integer hoaDonId);

    Optional<HoaDonChiTiet> findByHoaDon_IdAndSanPhamChiTiet_Id(Integer hoaDonId, Integer spctId);

    boolean existsBySanPhamChiTiet_Id(Integer sanPhamChiTietId);
}