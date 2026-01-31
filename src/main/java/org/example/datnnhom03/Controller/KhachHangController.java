package org.example.datnnhom03.Controller;

import org.example.datnnhom03.Model.KhachHang;
import org.example.datnnhom03.Service.KhachHangService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/khach-hang")
public class KhachHangController {

    private final KhachHangService khachHangService;

    public KhachHangController(KhachHangService khachHangService) {
        this.khachHangService = khachHangService;
    }

    // GET: /api/khach-hang
    @GetMapping
    public ResponseEntity<List<KhachHang>> getAll() {
        return ResponseEntity.ok(khachHangService.findAll());
    }

    // GET: /api/khach-hang/{id}
    @GetMapping("/{id}")
    public ResponseEntity<KhachHang> getById(@PathVariable Integer id) {
        KhachHang kh = khachHangService.findById(id);
        if (kh == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(kh);
    }

    // POST: /api/khach-hang
    @PostMapping
    public ResponseEntity<KhachHang> create(@RequestBody KhachHang khachHang) {
        KhachHang created = khachHangService.create(khachHang);
        return ResponseEntity.ok(created);
    }

    // PUT: /api/khach-hang/{id}
    @PutMapping("/{id}")
    public ResponseEntity<KhachHang> update(@PathVariable Integer id, @RequestBody KhachHang khachHang) {
        try {
            KhachHang updated = khachHangService.update(id, khachHang);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            // "Không tìm thấy khách hàng"
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE: /api/khach-hang/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        khachHangService.delete(id);
        return ResponseEntity.noContent().build();
    }
}