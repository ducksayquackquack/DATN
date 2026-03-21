package org.example.datnnhom03.Controller.api;

import lombok.RequiredArgsConstructor;
import org.example.datnnhom03.Model.LichCaLam;
import org.example.datnnhom03.Service.LichCaLamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lich-ca-lam")
@RequiredArgsConstructor
@CrossOrigin("*")
public class LichCaLamApiController {

    private final LichCaLamService service;

    @GetMapping
    public List<LichCaLam> getAll() {
        return service.findAll();
    }

    @PostMapping
    public LichCaLam create(@RequestBody LichCaLam ca) {
        return service.create(ca);
    }

    @PutMapping("/{id}")
    public LichCaLam update(@PathVariable Integer id,
                            @RequestBody LichCaLam ca) {
        return service.update(id, ca);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}