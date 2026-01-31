package org.example.datnnhom03.Controller;

import org.example.datnnhom03.Model.KhachHang;
import org.example.datnnhom03.Service.KhachHangService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/khach-hang")
public class KhachHangController {

    private final KhachHangService khachHangService;

    public KhachHangController(KhachHangService khachHangService) {
        this.khachHangService = khachHangService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("list", khachHangService.findAll());
        return "khachhang/khach-hang";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("kh", new KhachHang());
        return "khachhang/form-khach-hang";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("kh") KhachHang kh) {
        if (kh.getId() == null) {
            khachHangService.create(kh);
        } else {
            KhachHang old = khachHangService.findById(kh.getId());
            if (old != null) {
                old.setMaKhachHang(kh.getMaKhachHang());
                old.setTenKhachHang(kh.getTenKhachHang());
                old.setGioiTinh(kh.getGioiTinh());
                old.setNgaySinh(kh.getNgaySinh());
                old.setSoDienThoai(kh.getSoDienThoai());
                old.setTrangThai(kh.getTrangThai());
                // có thể cập nhật thêm các trường khác nếu có
                khachHangService.update(old.getId(), old);
            }
        }
        return "redirect:/khach-hang";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        KhachHang kh = khachHangService.findById(id);
        if (kh == null) {
            return "redirect:/khach-hang";
        }
        model.addAttribute("kh", kh);
        return "khachhang/form-khach-hang";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        khachHangService.delete(id);
        return "redirect:/khach-hang";
    }
}