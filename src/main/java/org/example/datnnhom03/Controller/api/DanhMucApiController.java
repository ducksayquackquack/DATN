package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.DanhMuc;
import org.example.datnnhom03.Service.DanhMucService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/danh-muc")
@CrossOrigin("*")
public class DanhMucApiController {

    private final DanhMucService service;

    public DanhMucApiController(DanhMucService service) {
        this.service = service;
    }

    @GetMapping
    public List<DanhMuc> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public DanhMuc getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public DanhMuc create(@RequestBody DanhMuc danhMuc) {
        System.out.println("POST HIT");
        return service.create(danhMuc);
    }

    @PutMapping("/{id}")
    public DanhMuc update(@PathVariable Integer id,
                          @RequestBody DanhMuc danhMuc) {

        danhMuc.setId(id);
        return service.update(id, danhMuc);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}