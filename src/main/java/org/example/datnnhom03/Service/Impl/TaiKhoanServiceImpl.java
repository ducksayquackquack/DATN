package org.example.datnnhom03.Service.Impl;

import lombok.RequiredArgsConstructor;
import org.example.datnnhom03.Model.TaiKhoan;
import org.example.datnnhom03.Repository.TaiKhoanRepository;
import org.example.datnnhom03.Service.TaiKhoanService;
import org.example.datnnhom03.dto.TaiKhoanDTO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaiKhoanServiceImpl implements TaiKhoanService {

    private final TaiKhoanRepository repository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private TaiKhoanDTO toDTO(TaiKhoan entity) {
        TaiKhoanDTO dto = new TaiKhoanDTO();
        dto.setId(entity.getId());
        dto.setEmail(entity.getEmail());
        dto.setVaiTro(entity.getVaiTro());
        dto.setAvatar(entity.getAvatar());
        dto.setTrangThaiHoatDong(entity.getTrangThaiHoatDong());
        dto.setTrangThaiTaiKhoan(entity.getTrangThaiTaiKhoan());
        return dto;
    }

    @Override
    public List<TaiKhoanDTO> findAll() {
        return repository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TaiKhoanDTO findById(Integer id) {
        return repository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    public TaiKhoanDTO create(TaiKhoanDTO dto) {

        if (repository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        TaiKhoan entity = new TaiKhoan();
        entity.setEmail(dto.getEmail());
        entity.setVaiTro(dto.getVaiTro());
        entity.setAvatar(dto.getAvatar());
        entity.setTrangThaiHoatDong("Hoạt động");
        entity.setTrangThaiTaiKhoan("Kích hoạt");

        // 🔐 Encode password
        entity.setMatKhau(passwordEncoder.encode(dto.getMatKhau()));

        return toDTO(repository.save(entity));
    }

    @Override
    public TaiKhoanDTO update(Integer id, TaiKhoanDTO dto) {

        TaiKhoan entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        entity.setVaiTro(dto.getVaiTro());
        entity.setAvatar(dto.getAvatar());
        entity.setTrangThaiHoatDong(dto.getTrangThaiHoatDong());
        entity.setTrangThaiTaiKhoan(dto.getTrangThaiTaiKhoan());

        return toDTO(repository.save(entity));
    }

    @Override
    public void delete(Integer id) {
        TaiKhoan entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        entity.setTrangThaiTaiKhoan("Khóa");
        repository.save(entity);
    }
}