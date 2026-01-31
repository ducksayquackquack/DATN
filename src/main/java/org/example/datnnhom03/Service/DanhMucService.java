package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.DanhMuc;
import java.util.List;

public interface DanhMucService {

    List<DanhMuc> findAll();

    DanhMuc findById(Integer id);

    DanhMuc save(DanhMuc danhMuc);

    void delete(Integer id);
}

