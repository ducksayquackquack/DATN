package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.KhuyenMai;

import java.math.BigDecimal;
import java.util.List;

public interface KhuyenMaiService {

    KhuyenMai create(KhuyenMai khuyenMai);

    KhuyenMai update(Integer id, KhuyenMai khuyenMai);

    void delete(Integer id);

    KhuyenMai findById(Integer id);

    List<KhuyenMai> findAll();

    // ===== BỔ SUNG PHỤC VỤ HOME =====
    BigDecimal getGiaTriKhuyenMai(Integer khuyenMaiId);
}