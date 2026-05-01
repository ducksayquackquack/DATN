package org.example.datnnhom03.Service;

import org.example.datnnhom03.Model.LichLamViec;
import org.example.datnnhom03.dto.LichLamViecDTO;

import java.util.List;

public interface LichLamViecService {

    List<LichLamViec> findAll();

    List<LichLamViec> findByNhanVien(Integer idNhanVien);

    LichLamViec create(LichLamViec lichLamViec);

    void delete(Integer id);

    List<LichLamViecDTO> getFullSchedule();

}