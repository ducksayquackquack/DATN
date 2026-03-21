package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.LichLamViec;
import org.example.datnnhom03.Service.LichLamViecService;
import org.example.datnnhom03.dto.LichLamViecDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lich-lam-viec")
@CrossOrigin("*")
public class LichLamViecApiController {

    @Autowired
    private LichLamViecService service;

    @GetMapping
    public List<LichLamViec> getAll() {
        return service.findAll();
    }

    @GetMapping("/nhan-vien/{id}")
    public List<LichLamViec> getByNhanVien(@PathVariable Integer id) {
        return service.findByNhanVien(id);
    }

    @GetMapping("/full")
    public List<LichLamViecDTO> getFullSchedule() {
        return service.getFullSchedule();
    }

    @PostMapping
    public LichLamViec create(@RequestBody LichLamViec lichLamViec) {
        return service.create(lichLamViec);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }

}