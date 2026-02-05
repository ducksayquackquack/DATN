package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.PTTT;

import java.util.List;

public interface PTTTService {
    PTTT create(PTTT pttt);
    PTTT update(Integer id, PTTT pttt);
    void delete(Integer id);
    PTTT findById(Integer id);
    List<PTTT> findAll();
}

