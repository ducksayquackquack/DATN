package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.LichLamViec;
import org.example.datnnhom03.dto.LichLamViecDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichLamViecRepository extends JpaRepository<LichLamViec, Integer> {

    List<LichLamViec> findByIdNhanVien(Integer idNhanVien);

    @Query("""
        SELECT new org.example.datnnhom03.dto.LichLamViecDTO(
            llv.id,
            nv.id,
            nv.tenNhanVien,
            ca.id,
            ca.tenCa,
            ca.gioBatDau,
            ca.gioKetThuc,
            llv.ngayLam,
            llv.trangThai
        )
        FROM LichLamViec llv
        JOIN NhanVien nv ON llv.idNhanVien = nv.id
        JOIN LichCaLam ca ON llv.idCaLam = ca.id
        ORDER BY llv.ngayLam
    """)
    List<LichLamViecDTO> getFullSchedule();
}