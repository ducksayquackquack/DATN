package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.HoaDonChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoaDonChiTietRepository extends JpaRepository<HoaDonChiTiet, Integer> {

    // list item theo hóa đơn
    List<HoaDonChiTiet> findByHoaDon_Id(Integer hoaDonId);

    // check dòng sản phẩm đã tồn tại trong hóa đơn hay chưa (để cộng dồn)
    Optional<HoaDonChiTiet> findByHoaDon_IdAndSanPhamChiTiet_Id(Integer hoaDonId, Integer spctId);
}