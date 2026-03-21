package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.Loai;
import org.example.datnnhom03.Service.LoaiService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loai")
@CrossOrigin("*")
public class LoaiApiController {

    private final LoaiService service;

    public LoaiApiController(LoaiService service) {
        this.service = service;
    }

    @GetMapping
    public List<Loai> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Loai getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public Loai create(@RequestBody Loai loai) {
        return service.create(loai);
    }

    @PutMapping("/{id}")
    public Loai update(@PathVariable Integer id,
                       @RequestBody Loai loai) {

        loai.setId(id);
        return service.update(id, loai);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}