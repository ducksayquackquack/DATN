package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.PTTT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PTTTRepository extends JpaRepository<PTTT,Integer> {
}
