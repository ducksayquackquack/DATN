package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.MauSac;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MauSacRepository extends JpaRepository<MauSac, Integer> {
    java.util.Optional<MauSac> findByTenMauIgnoreCase(String tenMau);
}
