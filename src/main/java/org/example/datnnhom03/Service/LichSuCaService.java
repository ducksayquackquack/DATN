package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.LichSuCa;

import java.util.List;

public interface LichSuCaService {

    List<LichSuCa> findAll();

    LichSuCa create(LichSuCa lichSuCa);

    LichSuCa update(Integer id, LichSuCa lichSuCa);

    void delete(Integer id);

    List<LichSuCa> findByNhanVien(Integer idNhanVien);
}