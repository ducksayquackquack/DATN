package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.DanhMuc;
import org.example.datnnhom03.dto.home.DanhMucDTO;

import java.util.List;

public interface DanhMucService {

    DanhMuc create(DanhMuc danhMuc);

    DanhMuc update(Integer id, DanhMuc danhMuc);

    void delete(Integer id);

    DanhMuc findById(Integer id);

    List<DanhMuc> findAll();

    // ===== BỔ SUNG PHỤC VỤ HOME =====
    List<DanhMucDTO> getActiveDanhMucDTO();
}