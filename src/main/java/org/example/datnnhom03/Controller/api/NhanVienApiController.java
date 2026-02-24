package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.NhanVien;
import org.example.datnnhom03.Service.NhanVienService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nhan-vien")
@CrossOrigin("*")
public class NhanVienApiController {

    private final NhanVienService service;

    public NhanVienApiController(NhanVienService service) {
        this.service = service;
    }

    @GetMapping
    public List<NhanVien> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public NhanVien getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public NhanVien create(@RequestBody NhanVien nhanVien) {
        return service.create(nhanVien);
    }

    @PutMapping("/{id}")
    public NhanVien update(@PathVariable Integer id,
                           @RequestBody NhanVien nhanVien) {
        nhanVien.setId(id);
        return service.update(id, nhanVien);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}