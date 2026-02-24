package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.MauSac;

import java.util.List;

public interface MauSacService {
    MauSac create(MauSac danhMuc);
    MauSac update(Integer id, MauSac danhMuc);
    void delete(Integer id);
    MauSac findById(Integer id);
    List<MauSac> findAll();
}

