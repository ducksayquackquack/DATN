package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.CaLam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaLamRepository extends JpaRepository<CaLam,Integer> {
}
