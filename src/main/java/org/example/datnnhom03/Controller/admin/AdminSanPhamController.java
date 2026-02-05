package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Service.SanPhamService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/san-pham")
public class AdminSanPhamController {

    private final SanPhamService sanPhamService;

    public AdminSanPhamController(SanPhamService sanPhamService) {
        this.sanPhamService = sanPhamService;
    }


    @GetMapping
    public String index(Model model) {
        model.addAttribute("list", sanPhamService.findAll());
        model.addAttribute("activeMenu", "san-pham");
        return "admin/san-pham/list";
    }


    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("sp", new SanPham());
        model.addAttribute("activeMenu", "san-pham");
        return "admin/san-pham/form";
    }


    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        SanPham sp = sanPhamService.findById(id);
        if (sp == null) {
            return "redirect:/admin/san-pham";
        }
        model.addAttribute("sp", sp);
        model.addAttribute("activeMenu", "san-pham");
        return "admin/san-pham/form";
    }


    @PostMapping("/save")
    public String save(@ModelAttribute("sp") SanPham sp) {

        if (sp.getId() == null) {
            sp.setNgayTao(LocalDateTime.now());
            sp.setNgaySua(LocalDateTime.now());
            sanPhamService.create(sp);
        } else {
            SanPham old = sanPhamService.findById(sp.getId());
            if (old != null) {
                old.setMaSanPham(sp.getMaSanPham());
                old.setTenSanPham(sp.getTenSanPham());
                old.setMoTa(sp.getMoTa());
                old.setTrangThai(sp.getTrangThai());
                old.setNgaySua(LocalDateTime.now());
                sanPhamService.update(old.getId(), old);
            }
        }

        return "redirect:/admin/san-pham";
    }


    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        sanPhamService.delete(id);
        return "redirect:/admin/san-pham";
    }
}

