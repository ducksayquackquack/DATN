package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.DiaChi;
import org.example.datnnhom03.Repository.DiaChiRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dia-chi")
@CrossOrigin("*")
public class DiaChiApiController {

    private final DiaChiRepository repository;

    public DiaChiApiController(DiaChiRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<DiaChi> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public DiaChi getById(@PathVariable Integer id) {
        return repository.findById(id).orElse(null);
    }

    @GetMapping("/khach-hang/{idKhachHang}")
    public List<DiaChi> getByKhachHang(@PathVariable Integer idKhachHang) {
        return repository.findByKhachHang_Id(idKhachHang);
    }

    @PostMapping
    public DiaChi create(@RequestBody DiaChi diaChi) {
        return repository.save(diaChi);
    }

    @PutMapping("/{id}")
    public DiaChi update(@PathVariable Integer id, @RequestBody DiaChi diaChi) {
        DiaChi entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        entity.setKhachHang(diaChi.getKhachHang());
        entity.setDiaChiCuThe(diaChi.getDiaChiCuThe());
        entity.setTinhThanh(diaChi.getTinhThanh());
        entity.setQuanHuyen(diaChi.getQuanHuyen());
        entity.setPhuongXa(diaChi.getPhuongXa());
        entity.setTrangThai(diaChi.getTrangThai());

        return repository.save(entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        repository.deleteById(id);
    }
}
