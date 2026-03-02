package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.DiaChi;
import org.example.datnnhom03.Service.DiaChiService;
import org.example.datnnhom03.dto.DiaChiDTO;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/dia-chi")
@CrossOrigin("*")
public class DiaChiApiController {

    private final DiaChiService service;

    public DiaChiApiController(DiaChiService service) {
        this.service = service;
    }

    @GetMapping
    public List<DiaChiDTO> getAll() {
        List<DiaChi> list = service.findAll();
        List<DiaChiDTO> result = new ArrayList<>();

        for (DiaChi dc : list) {
            DiaChiDTO dto = mapToDTO(dc);
            result.add(dto);
        }
        return result;
    }

    @GetMapping("/{id}")
    public DiaChiDTO getById(@PathVariable Integer id) {
        DiaChi dc = service.findById(id);
        if (dc == null) return null;
        return mapToDTO(dc);
    }

    @GetMapping("/khach-hang/{khachHangId}")
    public List<DiaChiDTO> getByKhachHang(@PathVariable Integer khachHangId) {
        List<DiaChi> list = service.findByKhachHangId(khachHangId);
        List<DiaChiDTO> result = new ArrayList<>();

        for (DiaChi dc : list) {
            result.add(mapToDTO(dc));
        }
        return result;
    }

    @PostMapping
    public DiaChi create(@RequestBody DiaChi diaChi) {
        return service.create(diaChi);
    }

    @PutMapping("/{id}")
    public DiaChi update(@PathVariable Integer id,
                         @RequestBody DiaChi diaChi) {

        diaChi.setId(id);
        return service.update(id, diaChi);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }

    private DiaChiDTO mapToDTO(DiaChi dc) {
        DiaChiDTO dto = new DiaChiDTO();
        dto.setId(dc.getId());
        dto.setKhachHangId(
                dc.getKhachHang() != null ? dc.getKhachHang().getId() : null
        );
        dto.setDiaChiCuThe(dc.getDiaChiCuThe());
        dto.setTinhThanh(dc.getTinhThanh());
        dto.setQuanHuyen(dc.getQuanHuyen());
        dto.setPhuongXa(dc.getPhuongXa());
        dto.setTrangThai(dc.getTrangThai());
        return dto;
    }
}