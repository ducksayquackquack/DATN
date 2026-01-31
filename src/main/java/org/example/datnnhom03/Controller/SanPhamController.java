package org.example.datnnhom03.Controller;

import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Service.SanPhamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/san-pham")
public class SanPhamController {

    @Autowired
    private SanPhamService sanPhamService;

    // CREATE
    @PostMapping
    public ResponseEntity<SanPham> create(@RequestBody SanPham sanPham) {
        SanPham created = sanPhamService.create(sanPham);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<SanPham> update(@PathVariable Integer id, @RequestBody SanPham sanPham) {
        SanPham updated = sanPhamService.update(id, sanPham);
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        sanPhamService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // FIND BY ID
    @GetMapping("/{id}")
    public ResponseEntity<SanPham> findById(@PathVariable Integer id) {
        SanPham sp = sanPhamService.findById(id);
        return ResponseEntity.ok(sp);
    }

    // FIND ALL
    @GetMapping
    public ResponseEntity<List<SanPham>> findAll() {
        List<SanPham> list = sanPhamService.findAll();
        return ResponseEntity.ok(list);
    }
}