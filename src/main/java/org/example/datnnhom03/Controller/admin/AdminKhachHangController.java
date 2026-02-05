package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Model.KhachHang;
import org.example.datnnhom03.Service.KhachHangService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/khach-hang")
public class AdminKhachHangController {

    private final KhachHangService khachHangService;

    public AdminKhachHangController(KhachHangService khachHangService) {
        this.khachHangService = khachHangService;
    }


    @GetMapping
    public String index(Model model) {
        model.addAttribute("activeMenu", "khach-hang");
        model.addAttribute("list", khachHangService.findAll());
        return "admin/khach-hang/list";
    }


    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("activeMenu", "khach-hang");
        model.addAttribute("kh", new KhachHang()); // ✅ BẮT BUỘC
        return "admin/khach-hang/form";
    }


    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        KhachHang kh = khachHangService.findById(id);
        if (kh == null) {
            return "redirect:/admin/khach-hang";
        }

        model.addAttribute("activeMenu", "khach-hang");
        model.addAttribute("kh", kh);
        return "admin/khach-hang/form";
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
                khachHangService.update(old.getId(), old);
            }
        }

        return "redirect:/admin/khach-hang";
    }


    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        khachHangService.delete(id);
        return "redirect:/admin/khach-hang";
    }
}
