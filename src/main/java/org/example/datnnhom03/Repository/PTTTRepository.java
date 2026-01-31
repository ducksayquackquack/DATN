package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.PTTT;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PTTTRepository extends JpaRepository<PTTT, Integer> {

    List<PTTT> findByTrangThai(String trangThai);
}
