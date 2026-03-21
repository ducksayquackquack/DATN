package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.LichCaLam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichCaLamRepository extends JpaRepository<LichCaLam, Integer> {
}