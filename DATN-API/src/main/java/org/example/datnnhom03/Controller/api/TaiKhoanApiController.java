package org.example.datnnhom03.Controller.api;

import lombok.RequiredArgsConstructor;
import org.example.datnnhom03.Service.TaiKhoanService;
import org.example.datnnhom03.dto.TaiKhoanDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tai-khoan")
@CrossOrigin("*")
@RequiredArgsConstructor
public class TaiKhoanApiController {

    private final TaiKhoanService service;

    @GetMapping
    public List<TaiKhoanDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaiKhoanDTO> getById(@PathVariable Integer id) {
        TaiKhoanDTO dto = service.findById(id);
        return dto == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<TaiKhoanDTO> create(@RequestBody TaiKhoanDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaiKhoanDTO> update(
            @PathVariable Integer id,
            @RequestBody TaiKhoanDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}