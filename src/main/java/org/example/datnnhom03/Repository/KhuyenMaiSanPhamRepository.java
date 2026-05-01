package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.KhuyenMaiSanPham;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KhuyenMaiSanPhamRepository extends JpaRepository<KhuyenMaiSanPham, Integer> {

    List<KhuyenMaiSanPham> findBySanPham_MaSanPhamIgnoreCase(String maSanPham);
}