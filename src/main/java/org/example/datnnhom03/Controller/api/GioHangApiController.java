package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.GioHang;
import org.example.datnnhom03.Service.GioHangService;
import org.example.datnnhom03.dto.GioHangDTO;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/gio-hang")
@CrossOrigin("*")
public class GioHangApiController {

    private final GioHangService service;

    public GioHangApiController(GioHangService service) {
        this.service = service;
    }

    @GetMapping
    public List<GioHangDTO> getAll() {
        List<GioHang> list = service.findAll();
        List<GioHangDTO> result = new ArrayList<>();

        for (GioHang gh : list) {
            GioHangDTO dto = new GioHangDTO();
            dto.setId(gh.getId());
            dto.setKhachHangId(
                    gh.getKhachHang() != null ? gh.getKhachHang().getId() : null
            );
            result.add(dto);
        }
        return result;
    }

    @GetMapping("/{id}")
    public GioHangDTO getById(@PathVariable Integer id) {
        GioHang gh = service.findById(id);
        if (gh == null) return null;

        GioHangDTO dto = new GioHangDTO();
        dto.setId(gh.getId());
        dto.setKhachHangId(
                gh.getKhachHang() != null ? gh.getKhachHang().getId() : null
        );
        return dto;
    }

    @PostMapping
    public GioHang create(@RequestBody GioHang gioHang) {
        return service.create(gioHang);
    }

    @PutMapping("/{id}")
    public GioHang update(@PathVariable Integer id,
                          @RequestBody GioHang gioHang) {

        gioHang.setId(id);
        return service.update(id, gioHang);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}