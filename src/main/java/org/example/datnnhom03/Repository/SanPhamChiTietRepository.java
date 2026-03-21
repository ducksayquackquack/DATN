package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.SanPhamChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Integer> {

  List<SanPhamChiTiet> findBySanPhamId(Integer sanPhamId);

  Optional<SanPhamChiTiet> findByMa(String ma);

    // (Tuỳ chọn) lấy 1 spct theo productId + trangThai
    Optional<SanPhamChiTiet> findFirstBySanPhamIdAndTrangThai(Integer sanPhamId, String trangThai);

    // ✅ size instock theo product
    @Query("""
        select distinct kt.id as id, kt.tenKichThuoc as name
        from SanPhamChiTiet spct
        join spct.kichThuoc kt
        where spct.sanPham.id = :productId
          and spct.soLuong > 0
    """)
    List<IdNameView> findSizesInStock(@Param("productId") Integer productId);

    // ✅ màu instock theo product + size
    @Query("""
        select distinct ms.id as id, ms.tenMau as name
        from SanPhamChiTiet spct
        join spct.mauSac ms
        where spct.sanPham.id = :productId
          and spct.kichThuoc.id = :sizeId
          and spct.soLuong > 0
    """)
    List<IdNameView> findColorsInStock(@Param("productId") Integer productId,
                                       @Param("sizeId") Integer sizeId);

    // ✅ tìm đúng biến thể
    @Query("""
        select spct
        from SanPhamChiTiet spct
        where spct.sanPham.id = :productId
          and spct.kichThuoc.id = :sizeId
          and spct.mauSac.id = :colorId
    """)
    Optional<SanPhamChiTiet> findVariant(@Param("productId") Integer productId,
                                         @Param("sizeId") Integer sizeId,
                                         @Param("colorId") Integer colorId);

    // projection
    interface IdNameView {
        Integer getId();
        String getName();
    }


    // size còn hàng theo sản phẩm
    @Query(value = """
        SELECT DISTINCT kt.id, kt.tenKichThuoc
        FROM SanPhamChiTiet spct
        JOIN KichThuoc kt ON kt.id = spct.idKichThuoc
        WHERE spct.idSanPham = :productId
          AND spct.soLuong > 0
    """, nativeQuery = true)
    List<Object[]> findSizesInStockRaw(@Param("productId") Integer productId);

    // màu còn hàng theo sản phẩm + size
    @Query(value = """
        SELECT DISTINCT ms.id, ms.tenMau
        FROM SanPhamChiTiet spct
        JOIN MauSac ms ON ms.id = spct.idMauSac
        WHERE spct.idSanPham = :productId
          AND spct.idKichThuoc = :sizeId
          AND spct.soLuong > 0
    """, nativeQuery = true)
    List<Object[]> findColorsInStockRaw(@Param("productId") Integer productId,
                                        @Param("sizeId") Integer sizeId);
}