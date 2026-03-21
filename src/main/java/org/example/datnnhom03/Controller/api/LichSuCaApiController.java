package org.example.datnnhom03.Controller.api;

import lombok.RequiredArgsConstructor;
import org.example.datnnhom03.Model.LichSuCa;
import org.example.datnnhom03.Service.LichSuCaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lich-su-ca")
@RequiredArgsConstructor
@CrossOrigin("*")
public class LichSuCaApiController {

    private final LichSuCaService service;

    @GetMapping
    public List<LichSuCa> getAll() {
        return service.findAll();
    }

    @PostMapping
    public LichSuCa create(@RequestBody LichSuCa lichSuCa) {
        return service.create(lichSuCa);
    }

    @PutMapping("/{id}")
    public LichSuCa update(@PathVariable Integer id, @RequestBody LichSuCa lichSuCa) {
        return service.update(id, lichSuCa);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }

    @GetMapping("/nhan-vien/{idNhanVien}")
    public List<LichSuCa> getByNhanVien(@PathVariable Integer idNhanVien) {
        return service.findByNhanVien(idNhanVien);
    }
}