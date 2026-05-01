package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {
    Optional<NhanVien> findByTaiKhoan_Id(Integer taiKhoanId);
}
