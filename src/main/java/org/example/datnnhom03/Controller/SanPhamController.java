package org.example.datnnhom03.Controller;

import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Service.SanPhamService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/san-pham")
public class SanPhamController {

    private final SanPhamService sanPhamService;

    public SanPhamController(SanPhamService sanPhamService) {
        this.sanPhamService = sanPhamService;
    }

    // Danh sách sản phẩm
    @GetMapping
    public String index(Model model) {
        model.addAttribute("list", sanPhamService.findAll());
        return "sanpham/san-pham";
    }

    // Form tạo mới sản phẩm
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("sp", new SanPham());
        return "sanpham/form-san-pham";
    }

    // Lưu tạo mới hoặc cập nhật sản phẩm
    @PostMapping("/save")
    public String save(@ModelAttribute("sp") SanPham sp) {
        if (sp.getId() == null) {
            sp.setNgayTao(LocalDateTime.now());
            sanPhamService.create(sp);
        } else {
            SanPham old = sanPhamService.findById(sp.getId());
            old.setTenSanPham(sp.getTenSanPham());
            old.setMoTa(sp.getMoTa());
            old.setTrangThai(sp.getTrangThai());
            old.setNgaySua(LocalDateTime.now());
            sanPhamService.update(old.getId(), old);
        }
        return "redirect:/san-pham";
    }

    // Form chỉnh sửa sản phẩm
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        SanPham sp = sanPhamService.findById(id);
        if (sp == null) {
            return "redirect:/san-pham";
        }
        model.addAttribute("sp", sp);
        return "sanpham/form-san-pham";
    }

    // Xóa sản phẩm
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        sanPhamService.delete(id);
        return "redirect:/san-pham";
    }
}