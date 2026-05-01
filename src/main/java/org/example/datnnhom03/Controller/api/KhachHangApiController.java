package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.KhachHang;
import org.example.datnnhom03.Service.KhachHangService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/khach-hang")
@CrossOrigin("*")
public class KhachHangApiController {

    private final KhachHangService service;

    public KhachHangApiController(KhachHangService service) {
        this.service = service;
    }

    @GetMapping
    public Page<KhachHang> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    public KhachHang getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @GetMapping("/by-tai-khoan/{taiKhoanId}")
    public ResponseEntity<KhachHang> getByTaiKhoanId(@PathVariable Integer taiKhoanId) {
        return service.findByTaiKhoanId(taiKhoanId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public KhachHang create(@RequestBody KhachHang khachHang) {
        return service.create(khachHang);
    }

    @PutMapping("/{id}")
    public KhachHang update(@PathVariable Integer id,
                            @RequestBody KhachHang khachHang) {
        khachHang.setId(id);
        return service.update(id, khachHang);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}