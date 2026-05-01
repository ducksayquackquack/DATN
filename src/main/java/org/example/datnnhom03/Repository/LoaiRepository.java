package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.Loai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoaiRepository  extends JpaRepository<Loai, Integer> {
}
