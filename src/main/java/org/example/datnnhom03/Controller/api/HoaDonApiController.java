package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Model.HoaDonChiTiet;
import org.example.datnnhom03.Repository.HoaDonChiTietRepository;
import org.example.datnnhom03.Service.HoaDonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hoa-don")
@CrossOrigin("*")
public class HoaDonApiController {

    private final HoaDonService service;

    public HoaDonApiController(HoaDonService service) {
        this.service = service;
    }

    @GetMapping
    public List<HoaDon> getAll() {
        System.out.println("TABLE COUNT: " + service.findAll().size());
        return service.findAll();
    }

    @GetMapping("/{id}")
    public HoaDon getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public HoaDon create(@RequestBody HoaDon hoaDon) {
        System.out.println("POST HIT");
        return service.create(hoaDon);
    }

    @PutMapping("/{id}")
    public HoaDon update(@PathVariable Integer id,
                         @RequestBody HoaDon hoaDon) {

        hoaDon.setId(id);
        return service.update(id, hoaDon);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @GetMapping("/{id}/chi-tiet")
    public List<HoaDonChiTiet> getChiTiet(@PathVariable Integer id) {
        return hoaDonChiTietRepository.findByHoaDon_Id(id);
    }


}