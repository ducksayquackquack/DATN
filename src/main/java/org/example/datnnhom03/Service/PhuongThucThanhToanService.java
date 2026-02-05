package org.example.datnnhom03.Service;

import org.example.datnnhom03.dto.PhuongThucThanhToanDTO;
import java.util.List;

public interface PhuongThucThanhToanService {

    List<PhuongThucThanhToanDTO> findAll();

    PhuongThucThanhToanDTO findById(Integer id);

    void create(PhuongThucThanhToanDTO dto);

    void update(Integer id, PhuongThucThanhToanDTO dto);

    void delete(Integer id);
}

