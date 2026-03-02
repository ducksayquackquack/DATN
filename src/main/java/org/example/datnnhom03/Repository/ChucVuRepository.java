package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.ChucVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChucVuRepository extends JpaRepository<ChucVu, Integer> {

    List<ChucVu> findByTrangThai(String trangThai);

}