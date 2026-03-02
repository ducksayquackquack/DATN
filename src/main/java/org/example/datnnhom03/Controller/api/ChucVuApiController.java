package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.ChucVu;
import org.example.datnnhom03.Service.ChucVuService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chuc-vu")
@CrossOrigin("*")
public class ChucVuApiController {

    private final ChucVuService service;

    public ChucVuApiController(ChucVuService service) {
        this.service = service;
    }

    @GetMapping
    public List<ChucVu> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ChucVu getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public ChucVu create(@RequestBody ChucVu chucVu) {
        return service.create(chucVu);
    }

    @PutMapping("/{id}")
    public ChucVu update(@PathVariable Integer id,
                         @RequestBody ChucVu chucVu) {

        chucVu.setId(id);
        return service.update(id, chucVu);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}