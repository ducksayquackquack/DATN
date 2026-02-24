package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.KhachHang;
import org.example.datnnhom03.Service.KhachHangService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/khach-hang")
@CrossOrigin("*")
public class KhachHangApiController {

    private final KhachHangService service;

    public KhachHangApiController(KhachHangService service) {
        this.service = service;
    }

    @GetMapping
    public List<KhachHang> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public KhachHang getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public KhachHang create(@RequestBody KhachHang khachHang) {
        return service.create(khachHang);
    }

    @PutMapping("/{id}")
    public KhachHang update(@PathVariable Integer id,
                            @RequestBody KhachHang khachHang) {

        khachHang.setId(id);
        return service.update(id, khachHang);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}