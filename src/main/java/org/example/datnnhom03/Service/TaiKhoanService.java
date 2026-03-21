package org.example.datnnhom03.Service;

import org.example.datnnhom03.dto.TaiKhoanDTO;
import org.example.datnnhom03.dto.TaiKhoanPasswordChangeDTO;

import java.util.List;

public interface TaiKhoanService {

    List<TaiKhoanDTO> findAll();

    TaiKhoanDTO findById(Integer id);

    TaiKhoanDTO create(TaiKhoanDTO dto);

    TaiKhoanDTO update(Integer id, TaiKhoanDTO dto);

    void changePassword(Integer id, TaiKhoanPasswordChangeDTO dto);

    void delete(Integer id);
}