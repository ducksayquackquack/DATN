package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoaDonRepsitory extends JpaRepository<HoaDon, Integer> {

    List<HoaDon> findByTrangThai(String trangThai);

}