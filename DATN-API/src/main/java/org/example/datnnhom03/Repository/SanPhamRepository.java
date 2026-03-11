package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SanPhamRepository  extends JpaRepository<SanPham, Integer> {
    List<SanPham> findByTrangThai(String trangThai);
}
