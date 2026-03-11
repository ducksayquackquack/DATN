package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Model.HoaDonChiTiet;
import org.example.datnnhom03.Service.HoaDonService;
import org.example.datnnhom03.Repository.HoaDonChiTietRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hoa-don")
public class HoaDonApiController {

    private final HoaDonService hoaDonService;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;

    public HoaDonApiController(HoaDonService hoaDonService,
                               HoaDonChiTietRepository hoaDonChiTietRepository) {
        this.hoaDonService = hoaDonService;
        this.hoaDonChiTietRepository = hoaDonChiTietRepository;
    }

    @GetMapping
    public List<HoaDon> getAll() {
        return hoaDonService.findAll();
    }

    @GetMapping("/{id}")
    public HoaDon getById(@PathVariable Integer id) {
        return hoaDonService.findById(id);
    }

    // ⭐ THÊM API CREATE
    @PostMapping
    public HoaDon create(@RequestBody HoaDon hoaDon) {
        return hoaDonService.save(hoaDon);
    }

    @PutMapping("/{id}")
    public HoaDon update(@PathVariable Integer id,
                         @RequestBody HoaDon request) {
        return hoaDonService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        hoaDonService.delete(id);
    }

    @GetMapping("/{id}/chi-tiet")
    public List<HoaDonChiTiet> getChiTiet(@PathVariable Integer id) {
        return hoaDonChiTietRepository.findByHoaDonId(id);
    }
}
