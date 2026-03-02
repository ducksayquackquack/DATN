package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.ChucVu;

import java.util.List;

public interface ChucVuService {

    ChucVu create(ChucVu chucVu);

    ChucVu update(Integer id, ChucVu chucVu);

    void delete(Integer id);

    ChucVu findById(Integer id);

    List<ChucVu> findAll();
}