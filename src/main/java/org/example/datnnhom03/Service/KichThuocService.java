package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.KichThuoc;

import java.util.List;

public interface KichThuocService {
    KichThuoc create(KichThuoc kichThuoc);
    KichThuoc update(Integer id, KichThuoc kichThuoc);
    void delete(Integer id);
    KichThuoc findById(Integer id);
    List<KichThuoc> findAll();
}

