package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HoaDonRepository
        extends JpaRepository<HoaDon, Integer>,
        JpaSpecificationExecutor<HoaDon> {

    List<HoaDon> findByTrangThai(String trangThai);
    List<HoaDon> findByKhachHang_Id(Integer id);

    long countByKhachHang_Id(Integer id);

    @Query("""
select h from HoaDon h
where (:statusId is null or h.orderStatusId = :statusId)
  and (
        :keyword is null or :keyword = '' 
        or lower(h.maHoaDon) like lower(concat('%', :keyword, '%'))
        or lower(h.soDienThoaiNhanHang) like lower(concat('%', :keyword, '%'))
      )
order by h.id desc
""")
    List<HoaDon> search(@Param("keyword") String keyword,
                        @Param("statusId") Integer statusId);
}