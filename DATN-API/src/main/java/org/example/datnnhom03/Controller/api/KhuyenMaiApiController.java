package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.KhuyenMai;
import org.example.datnnhom03.Service.KhuyenMaiService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/khuyen-mai")
@CrossOrigin("*")
public class KhuyenMaiApiController {

    private final KhuyenMaiService service;

    public KhuyenMaiApiController(KhuyenMaiService service) {
        this.service = service;
    }

    @GetMapping
    public List<KhuyenMai> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public KhuyenMai getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public KhuyenMai create(@RequestBody KhuyenMai khuyenMai) {
        return service.create(khuyenMai);
    }

    @PutMapping("/{id}")
    public KhuyenMai update(@PathVariable Integer id,
                            @RequestBody KhuyenMai khuyenMai) {

        khuyenMai.setId(id);
        return service.update(id, khuyenMai);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}