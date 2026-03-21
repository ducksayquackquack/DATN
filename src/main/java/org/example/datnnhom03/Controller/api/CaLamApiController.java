package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.CaLam;
import org.example.datnnhom03.Repository.CaLamRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ca-lam")
@CrossOrigin("*")
public class CaLamApiController {

    private final CaLamRepository repository;

    public CaLamApiController(CaLamRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CaLam> getAll(){
        return repository.findAll();
    }

    @PostMapping
    public CaLam create(@RequestBody CaLam caLam){
        return repository.save(caLam);
    }

    @PutMapping("/{id}")
    public CaLam update(@PathVariable Integer id,@RequestBody CaLam caLam){
        caLam.setId(id);
        return repository.save(caLam);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id){
        repository.deleteById(id);
    }
}