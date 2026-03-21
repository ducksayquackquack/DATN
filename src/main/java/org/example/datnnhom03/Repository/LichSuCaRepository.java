package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.LichSuCa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LichSuCaRepository extends JpaRepository<LichSuCa, Integer> {

    List<LichSuCa> findByIdNhanVien(Integer idNhanVien);

}