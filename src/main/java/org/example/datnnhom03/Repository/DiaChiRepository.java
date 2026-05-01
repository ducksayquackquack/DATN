package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.DiaChi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiaChiRepository extends JpaRepository<DiaChi, Integer> {
    List<DiaChi> findByKhachHang_Id(Integer idKhachHang);

    Optional<DiaChi> findTopByKhachHang_IdOrderByIdDesc(Integer idKhachHang);
}
