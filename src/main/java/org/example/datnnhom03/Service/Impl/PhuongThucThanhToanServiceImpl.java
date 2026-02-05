package org.example.datnnhom03.Service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.datnnhom03.Model.PhuongThucThanhToan;
import org.example.datnnhom03.Repository.PhuongThucThanhToanRepository;
import org.example.datnnhom03.Service.PhuongThucThanhToanService;
import org.example.datnnhom03.dto.PhuongThucThanhToanDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhuongThucThanhToanServiceImpl implements PhuongThucThanhToanService {

    private final PhuongThucThanhToanRepository repository;

    @Override
    public List<PhuongThucThanhToanDTO> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PhuongThucThanhToanDTO findById(Integer id) {
        return repository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    public void create(PhuongThucThanhToanDTO dto) {
        repository.save(toEntity(dto));
    }

    @Override
    public void update(Integer id, PhuongThucThanhToanDTO dto) {
        PhuongThucThanhToan e = toEntity(dto);
        e.setId(id);
        repository.save(e);
    }

    @Override
    public void delete(Integer id) {
        repository.deleteById(id);
    }

    /* ===== mapper ===== */

    private PhuongThucThanhToanDTO toDTO(PhuongThucThanhToan e) {
        PhuongThucThanhToanDTO dto = new PhuongThucThanhToanDTO();
        dto.setId(e.getId());
        dto.setMa(e.getMa());
        dto.setTen(e.getTen());
        dto.setMoTa(e.getMoTa());
        dto.setTrangThai(e.getTrangThai());
        return dto;
    }

    private PhuongThucThanhToan toEntity(PhuongThucThanhToanDTO dto) {
        PhuongThucThanhToan e = new PhuongThucThanhToan();
        e.setMa(dto.getMa());
        e.setTen(dto.getTen());
        e.setMoTa(dto.getMoTa());
        e.setTrangThai(dto.getTrangThai());
        return e;
    }
}


