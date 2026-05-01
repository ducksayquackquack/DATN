package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.LichCaLam;

import java.util.List;

public interface LichCaLamService {

    List<LichCaLam> findAll();

    LichCaLam create(LichCaLam caLam);

    LichCaLam update(Integer id, LichCaLam caLam);

    void delete(Integer id);

    LichCaLam findById(Integer id);
}