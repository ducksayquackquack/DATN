package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {
    List<SanPham> findTop10ByTenSanPhamContainingIgnoreCase(String q);

    List<SanPham> findByTrangThai(String trangThai);

    boolean existsByMaSanPhamIgnoreCase(String maSanPham);

    boolean existsByMaSanPhamIgnoreCaseAndIdNot(String maSanPham, Integer id);

    @Query(value = """
        SELECT MAX(TRY_CAST(SUBSTRING(maSanPham, 3, LEN(maSanPham) - 2) AS INT))
        FROM SanPham
        WHERE maSanPham LIKE 'SP[0-9]%'
    """, nativeQuery = true)
    Integer findMaxSanPhamCodeNumber();
}