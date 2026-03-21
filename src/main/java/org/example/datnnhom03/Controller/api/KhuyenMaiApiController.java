package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.KhuyenMai;
import org.example.datnnhom03.Repository.KhuyenMaiRepository;
import org.example.datnnhom03.Service.KhuyenMaiService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/khuyen-mai")
@CrossOrigin("*")
public class KhuyenMaiApiController {

    private final KhuyenMaiService service;
    private final KhuyenMaiRepository khuyenMaiRepository;

    public KhuyenMaiApiController(KhuyenMaiService service,
                                  KhuyenMaiRepository khuyenMaiRepository) {
        this.service = service;
        this.khuyenMaiRepository = khuyenMaiRepository;
    }

    @GetMapping
    public List<KhuyenMai> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public KhuyenMai getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public KhuyenMai create(@RequestBody KhuyenMai khuyenMai) {
        validateKhuyenMaiPayload(khuyenMai, true, null);
        return service.create(khuyenMai);
    }

    @PutMapping("/{id}")
    public KhuyenMai update(@PathVariable Integer id,
                            @RequestBody KhuyenMai khuyenMai) {
        validateKhuyenMaiPayload(khuyenMai, false, id);

        khuyenMai.setId(id);
        return service.update(id, khuyenMai);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }

    private void validateKhuyenMaiPayload(KhuyenMai khuyenMai, boolean isCreate, Integer id) {
        if (khuyenMai == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload khuyen mai khong hop le");
        }

        String maKhuyenMai = normalizeText(khuyenMai.getMaKhuyenMai());
        if (!isCreate && maKhuyenMai.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ma khuyen mai khong duoc de trong");
        }
        if (!maKhuyenMai.isEmpty() && !maKhuyenMai.matches("^KM\\d{3,}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ma khuyen mai phai theo dinh dang KM + so");
        }

        if (!maKhuyenMai.isEmpty()) {
            boolean duplicate = isCreate
                    ? khuyenMaiRepository.existsByMaKhuyenMaiIgnoreCase(maKhuyenMai)
                    : khuyenMaiRepository.existsByMaKhuyenMaiIgnoreCaseAndIdNot(maKhuyenMai, id);
            if (duplicate) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ma khuyen mai da ton tai");
            }
            khuyenMai.setMaKhuyenMai(maKhuyenMai);
        }

        String tenKhuyenMai = normalizeText(khuyenMai.getTenKhuyenMai());
        if (tenKhuyenMai.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ten khuyen mai khong duoc de trong");
        }
        khuyenMai.setTenKhuyenMai(tenKhuyenMai);

        BigDecimal giaTri = khuyenMai.getGiaTri();
        if (giaTri == null || giaTri.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gia tri giam phai lon hon 0");
        }

        String donVi = normalizeText(khuyenMai.getDonViGiam()).toUpperCase();
        if (donVi.isEmpty()) donVi = "PERCENT";
        if (!"PERCENT".equals(donVi) && !"VND".equals(donVi)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Don vi giam chi duoc la PERCENT hoac VND");
        }
        if ("PERCENT".equals(donVi) && giaTri.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gia tri giam theo % khong duoc vuot qua 100");
        }
        khuyenMai.setDonViGiam(donVi);

        if (khuyenMai.getNgayBatDau() == null || khuyenMai.getNgayKetThuc() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngay bat dau va ngay ket thuc khong duoc de trong");
        }
        if (khuyenMai.getNgayKetThuc().isBefore(khuyenMai.getNgayBatDau())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngay ket thuc phai sau ngay bat dau");
        }

        String trangThai = normalizeText(khuyenMai.getTrangThai());
        if (trangThai.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang thai khuyen mai khong duoc de trong");
        }
        khuyenMai.setTrangThai(trangThai);
    }

    private String normalizeText(String value) {
        return String.valueOf(value == null ? "" : value).trim();
    }
}