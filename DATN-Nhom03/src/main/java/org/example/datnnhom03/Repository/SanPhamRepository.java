package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SanPhamRepository  extends JpaRepository<SanPham, Integer> {
}
