package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.PhieuGiamGia;
import org.example.datnnhom03.Service.PhieuGiamGiaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/phieu-giam-gia")
@CrossOrigin("*")
public class PhieuGiamGiaApiController {

    private final PhieuGiamGiaService service;

    public PhieuGiamGiaApiController(PhieuGiamGiaService service) {
        this.service = service;
    }

    @GetMapping
    public List<PhieuGiamGia> getAll() {
        return service.findAll();
    }

    @GetMapping("/active")
    public List<PhieuGiamGia> getActive() {
        return service.findActive();
    }

    @GetMapping("/{id}")
    public PhieuGiamGia getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public PhieuGiamGia create(@RequestBody PhieuGiamGia phieuGiamGia) {
        return service.create(phieuGiamGia);
    }

    @PutMapping("/{id}")
    public PhieuGiamGia update(@PathVariable Integer id,
                               @RequestBody PhieuGiamGia phieuGiamGia) {

        phieuGiamGia.setId(id);
        return service.update(id, phieuGiamGia);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}