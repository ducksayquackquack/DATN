package org.example.datnnhom03.Controller.api;

import lombok.RequiredArgsConstructor;
import org.example.datnnhom03.Service.TaiKhoanService;
import org.example.datnnhom03.dto.TaiKhoanDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/taikhoan")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TaiKhoanApiController {

    private final TaiKhoanService taiKhoanService;

    @GetMapping
    public List<TaiKhoanDTO> getAll() {
        return taiKhoanService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaiKhoanDTO> getById(@PathVariable Integer id) {
        TaiKhoanDTO dto = taiKhoanService.findById(id);
        return dto == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<TaiKhoanDTO> create(@RequestBody TaiKhoanDTO dto) {
        return ResponseEntity.ok(taiKhoanService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaiKhoanDTO> update(
            @PathVariable Integer id,
            @RequestBody TaiKhoanDTO dto
    ) {
        return ResponseEntity.ok(taiKhoanService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        taiKhoanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
