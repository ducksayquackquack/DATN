package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.DiaChi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaChiRepository extends JpaRepository<DiaChi, Integer> {

    List<DiaChi> findByTrangThai(String trangThai);

    List<DiaChi> findByKhachHangId(Integer khachHangId);

}