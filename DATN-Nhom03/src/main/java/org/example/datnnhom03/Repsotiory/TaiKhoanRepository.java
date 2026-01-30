package org.example.datnnhom03.Repsotiory;

import org.example.datnnhom03.Model.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {

}
