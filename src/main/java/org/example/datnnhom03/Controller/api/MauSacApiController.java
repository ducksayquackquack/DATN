package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.MauSac;
import org.example.datnnhom03.Service.MauSacService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/mau-sac")
@CrossOrigin("*")
public class MauSacApiController {

    private final MauSacService service;

    public MauSacApiController(MauSacService service) {
        this.service = service;
    }

    @GetMapping
    public List<MauSac> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public MauSac getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public MauSac create(@RequestBody MauSac mauSac) {
        mauSac.setNgayTao(LocalDateTime.now());
        mauSac.setNgaySua(LocalDateTime.now());
        return service.create(mauSac);
    }

    @PutMapping("/{id}")
    public MauSac update(@PathVariable Integer id,
                         @RequestBody MauSac mauSac) {

        mauSac.setId(id);
        mauSac.setNgaySua(LocalDateTime.now());
        return service.update(id, mauSac);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}