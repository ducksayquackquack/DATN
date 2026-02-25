package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Service.SanPhamService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/san-pham")
@CrossOrigin("*")
public class SanPhamApiController {

    private final SanPhamService service;

    public SanPhamApiController(SanPhamService service) {
        this.service = service;
    }

    @GetMapping
    public List<SanPham> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SanPham getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public SanPham create(@RequestBody SanPham sanPham) {
        return service.create(sanPham);
    }

    @PutMapping("/{id}")
    public SanPham update(@PathVariable Integer id,
                          @RequestBody SanPham sanPham) {
        return service.update(id, sanPham);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}