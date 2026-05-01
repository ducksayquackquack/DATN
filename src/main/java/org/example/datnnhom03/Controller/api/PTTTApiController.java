package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.PTTT;
import org.example.datnnhom03.Service.PTTTService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pttt")
@CrossOrigin("*")
public class PTTTApiController {

    private final PTTTService service;

    public PTTTApiController(PTTTService service) {
        this.service = service;
    }

    @GetMapping
    public List<PTTT> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PTTT getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public PTTT create(@RequestBody PTTT pttt) {
        return service.create(pttt);
    }

    @PutMapping("/{id}")
    public PTTT update(@PathVariable Integer id,
                       @RequestBody PTTT pttt) {

        pttt.setId(id);
        return service.update(id, pttt);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
