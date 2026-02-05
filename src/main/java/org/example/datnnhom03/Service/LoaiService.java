package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.DanhMuc;
import org.example.datnnhom03.Model.Loai;

import java.util.List;

public interface LoaiService {
    Loai create(Loai loai);
    Loai update(Integer id, Loai loai);
    void delete(Integer id);
    Loai findById(Integer id);
    List<Loai> findAll();
}

