package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.GioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GioHangRepository extends JpaRepository<GioHang, Integer> {

    Optional<GioHang> findByKhachHangId(Integer khachHangId);

}