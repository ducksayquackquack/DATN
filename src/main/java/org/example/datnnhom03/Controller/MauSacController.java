package org.example.datnnhom03.Controller;

import org.example.datnnhom03.Model.MauSac;
import org.example.datnnhom03.Service.MauSacService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mau-sac")
public class MauSacController {

    private final MauSacService mauSacService;

    public MauSacController(MauSacService mauSacService) {
        this.mauSacService = mauSacService;
    }

    // GET: /api/mau-sac
    @GetMapping
    public ResponseEntity<List<MauSac>> getAll() {
        return ResponseEntity.ok(mauSacService.findAll());
    }

    // GET: /api/mau-sac/{id}
    @GetMapping("/{id}")
    public ResponseEntity<MauSac> getById(@PathVariable Integer id) {
        MauSac ms = mauSacService.findById(id);
        if (ms == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ms);
    }

    // POST: /api/mau-sac
    @PostMapping
    public ResponseEntity<MauSac> create(@RequestBody MauSac mauSac) {
        MauSac created = mauSacService.create(mauSac);
        return ResponseEntity.ok(created);
    }

    // PUT: /api/mau-sac/{id}
    @PutMapping("/{id}")
    public ResponseEntity<MauSac> update(@PathVariable Integer id, @RequestBody MauSac mauSac) {
        // check tồn tại trước để trả về 404 đúng chuẩn
        MauSac existing = mauSacService.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        // đảm bảo update đúng bản ghi theo id trên URL
        mauSac.setId(id);

        MauSac updated = mauSacService.update(id, mauSac);
        return ResponseEntity.ok(updated);
    }

    // DELETE: /api/mau-sac/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        MauSac existing = mauSacService.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        mauSacService.delete(id);
        return ResponseEntity.noContent().build();
    }
}