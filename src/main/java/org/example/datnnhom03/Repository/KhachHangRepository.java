package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {
	Optional<KhachHang> findByTaiKhoan_Id(Integer taiKhoanId);

	boolean existsByTaiKhoan_Id(Integer taiKhoanId);
}
