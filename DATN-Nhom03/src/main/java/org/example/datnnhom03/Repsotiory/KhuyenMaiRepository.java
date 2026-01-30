package org.example.datnnhom03.Repsotiory;

import org.example.datnnhom03.Model.KhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Integer> {
}
