package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.KichThuoc;
import org.example.datnnhom03.Service.KichThuocService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/kich-thuoc")
@CrossOrigin("*")
public class KichThuocApiController {

    private final KichThuocService service;

    public KichThuocApiController(KichThuocService service) {
        this.service = service;
    }

    @GetMapping
    public List<KichThuoc> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public KichThuoc getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public KichThuoc create(@RequestBody KichThuoc kichThuoc) {
        kichThuoc.setNgayTao(LocalDateTime.now());
        kichThuoc.setNgaySua(LocalDateTime.now());
        return service.create(kichThuoc);
    }

    @PutMapping("/{id}")
    public KichThuoc update(@PathVariable Integer id,
                            @RequestBody KichThuoc kichThuoc) {

        kichThuoc.setId(id);
        kichThuoc.setNgaySua(LocalDateTime.now());
        return service.update(id, kichThuoc);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}