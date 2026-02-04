package org.example.datnnhom03.Repsotiory;

import org.example.datnnhom03.Model.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {

    // Dùng cho login (Spring Security)
    Optional<TaiKhoan> findByEmail(String email);

    // Dùng cho register (check trùng email)
    boolean existsByEmail(String email);
}
