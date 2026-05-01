package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.ChucVu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChucVuRepository extends JpaRepository<ChucVu, Integer> {

    Optional<ChucVu> findByMaChucVuIgnoreCase(String maChucVu);

    Optional<ChucVu> findFirstByTenChucVuContainingIgnoreCase(String tenChucVu);
}
