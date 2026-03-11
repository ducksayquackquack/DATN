package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Repository.HoaDonRepository;
import org.example.datnnhom03.Repository.KhachHangRepository;
import org.example.datnnhom03.Repository.NhanVienRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/hoa-don")
public class AdminHoaDonController {

    private final HoaDonRepository hoaDonRepository;
    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;

    public AdminHoaDonController(HoaDonRepository hoaDonRepository,
                                 NhanVienRepository nhanVienRepository,
                                 KhachHangRepository khachHangRepository) {
        this.hoaDonRepository = hoaDonRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.khachHangRepository = khachHangRepository;
    }

    /* =========================
       LIST
       ========================= */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("activeMenu", "hoa-don");
        model.addAttribute("list", hoaDonRepository.findAll());
        return "admin/hoa-don/list-hoa-don";
    }

    /* =========================
       OPEN FORM - CREATE
       ========================= */
    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("activeMenu", "hoa-don");
        model.addAttribute("hd", new HoaDon());
        model.addAttribute("listNV", nhanVienRepository.findAll());
        model.addAttribute("listKH", khachHangRepository.findAll());
        return "admin/hoa-don/form-hoa-don";
    }

    /* =========================
       SUBMIT CREATE
       ========================= */
    @PostMapping("/store")
    public String store(@ModelAttribute("hd") HoaDon hd) {
        hd.setNgayTao(LocalDateTime.now());
        hoaDonRepository.save(hd);
        return "redirect:/admin/hoa-don";
    }


    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {

        HoaDon hd = hoaDonRepository.findById(id).orElse(null);
        if (hd == null) {
            return "redirect:/admin/hoa-don";
        }

        model.addAttribute("activeMenu", "hoa-don");
        model.addAttribute("hd", hd);
        model.addAttribute("listNV", nhanVienRepository.findAll());
        model.addAttribute("listKH", khachHangRepository.findAll());
        return "admin/hoa-don/form-hoa-don";
    }

    /* =========================
       SUBMIT UPDATE
       ========================= */
    @PostMapping("/update")
    public String update(@ModelAttribute("hd") HoaDon formHd) {

        HoaDon dbHd = hoaDonRepository.findById(formHd.getId()).orElse(null);

        if (dbHd != null) {
            dbHd.setMaHoaDon(formHd.getMaHoaDon());
            dbHd.setNhanVien(formHd.getNhanVien());
            dbHd.setKhachHang(formHd.getKhachHang());
            dbHd.setSoDienThoaiNhanHang(formHd.getSoDienThoaiNhanHang());
            dbHd.setDiaChiNhanHang(formHd.getDiaChiNhanHang());
            dbHd.setPhiShip(formHd.getPhiShip());
            dbHd.setGiaSauGiamGia(formHd.getGiaSauGiamGia());
            dbHd.setThanhTien(formHd.getThanhTien());
            dbHd.setNgayNhanHangDuKien(formHd.getNgayNhanHangDuKien());
            dbHd.setNgayNhanHangMongMuon(formHd.getNgayNhanHangMongMuon());
            dbHd.setTrangThai(formHd.getTrangThai());

            hoaDonRepository.save(dbHd);
        }

        return "redirect:/admin/hoa-don";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        hoaDonRepository.deleteById(id);
        return "redirect:/admin/hoa-don";
    }
}
