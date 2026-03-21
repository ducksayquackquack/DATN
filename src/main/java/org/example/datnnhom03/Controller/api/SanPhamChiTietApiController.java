package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.SanPhamChiTiet;
import org.example.datnnhom03.Repository.SanPhamChiTietRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/san-pham-chi-tiet")
@CrossOrigin("*")
public class SanPhamChiTietApiController {

    private final SanPhamChiTietRepository repository;

    public SanPhamChiTietApiController(SanPhamChiTietRepository repository) {
        this.repository = repository;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        SanPhamChiTiet spct = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể"));

        // Soft delete
        spct.setTrangThai("Ngừng hoạt động");
        spct.setNgaySua(LocalDateTime.now());
        repository.save(spct);
    }

    @PutMapping("/{id}")
    public SanPhamChiTiet update(@PathVariable Integer id, @RequestBody SanPhamChiTiet body) {
        SanPhamChiTiet spct = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy biến thể"));

        if (body.getGiaBan() != null) spct.setGiaBan(body.getGiaBan());
        if (body.getGiaNhap() != null) spct.setGiaNhap(body.getGiaNhap());
        if (body.getSoLuong() != null) spct.setSoLuong(body.getSoLuong());
        if (body.getTrangThai() != null) spct.setTrangThai(body.getTrangThai());
        if (body.getMa() != null) spct.setMa(body.getMa());
        spct.setNgaySua(LocalDateTime.now());

        return repository.save(spct);
    }
}
