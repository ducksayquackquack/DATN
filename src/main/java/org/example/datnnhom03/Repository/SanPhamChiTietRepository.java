package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.SanPhamChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Integer> {

    Optional<SanPhamChiTiet> findFirstBySanPhamIdAndTrangThai(Integer sanPhamId, String trangThai);

}