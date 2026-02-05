package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.DanhMuc;
import java.util.List;

public interface DanhMucService {
    DanhMuc create(DanhMuc danhMuc);
    DanhMuc update(Integer id, DanhMuc danhMuc);
    void delete(Integer id);
    DanhMuc findById(Integer id);
    List<DanhMuc> findAll();
}
