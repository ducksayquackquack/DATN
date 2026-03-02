package org.example.datnnhom03.Repository;

import org.example.datnnhom03.Model.ChatLieu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatLieuRepository extends JpaRepository<ChatLieu, Integer> {

    List<ChatLieu> findByTrangThai(String trangThai);

}