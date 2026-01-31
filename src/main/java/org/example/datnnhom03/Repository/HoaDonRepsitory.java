package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoaDonRepsitory extends JpaRepository<HoaDon, Integer> {
}
