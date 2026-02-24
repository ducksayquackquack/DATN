package org.example.datnnhom03.Service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.datnnhom03.Model.TaiKhoan;
import org.example.datnnhom03.Repository.TaiKhoanRepository;
import org.example.datnnhom03.Service.TaiKhoanService;
import org.example.datnnhom03.dto.TaiKhoanDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaiKhoanServiceImpl implements TaiKhoanService {

    private final TaiKhoanRepository taiKhoanRepository;

    private TaiKhoanDTO toDTO(TaiKhoan e) {
        TaiKhoanDTO dto = new TaiKhoanDTO();
        dto.setId(e.getId());
        dto.setEmail(e.getEmail());
        dto.setVaiTro(e.getVaiTro());
        dto.setAvatar(e.getAvatar());
        dto.setTrangThaiHoatDong(e.getTrangThaiHoatDong());
        dto.setTrangThaiTaiKhoan(e.getTrangThaiTaiKhoan());
        return dto;
    }

    @Override
    public List<TaiKhoanDTO> findAll() {
        return taiKhoanRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public TaiKhoanDTO findById(Integer id) {
        return taiKhoanRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    public TaiKhoanDTO create(TaiKhoanDTO dto) {
        TaiKhoan entity = new TaiKhoan();
        entity.setEmail(dto.getEmail());
        entity.setVaiTro(dto.getVaiTro());
        entity.setAvatar(dto.getAvatar());
        entity.setTrangThaiHoatDong(dto.getTrangThaiHoatDong());
        entity.setTrangThaiTaiKhoan(dto.getTrangThaiTaiKhoan());

        return toDTO(taiKhoanRepository.save(entity));
    }

    @Override
    public TaiKhoanDTO update(Integer id, TaiKhoanDTO dto) {
        TaiKhoan entity = taiKhoanRepository.findById(id).orElseThrow();

        entity.setVaiTro(dto.getVaiTro());
        entity.setAvatar(dto.getAvatar());
        entity.setTrangThaiHoatDong(dto.getTrangThaiHoatDong());
        entity.setTrangThaiTaiKhoan(dto.getTrangThaiTaiKhoan());

        return toDTO(taiKhoanRepository.save(entity));
    }

    @Override
    public void delete(Integer id) {
        TaiKhoan entity = taiKhoanRepository.findById(id).orElseThrow();
        entity.setTrangThaiTaiKhoan("Khóa"); // soft delete
        taiKhoanRepository.save(entity);
    }
}
