package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Model.NhanVien;
import org.example.datnnhom03.Repository.NhanVienRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminNhanVienController {

    private final NhanVienRepository nhanVienRepository;

    public AdminNhanVienController(NhanVienRepository nhanVienRepository) {
        this.nhanVienRepository = nhanVienRepository;
    }

    @GetMapping("/admin/nhan-vien")
    public String index(Model model) {
        model.addAttribute("list", nhanVienRepository.findAll());
        return "admin/nhan-vien/form-nhan-vien";
    }


    // HIỂN THỊ FORM ADD
    @GetMapping("/nhan-vien/create")
    public String create(Model model) {
        model.addAttribute("nv", new NhanVien());
        return "admin/nhan-vien/add-nhan-vien";
    }

    // LƯU NHÂN VIÊN
    @PostMapping("/admin/nhan-vien/store")
    public String store(@ModelAttribute("nv") NhanVien nv) {
        nhanVienRepository.save(nv);
        return "redirect:/admin/nhan-vien";
    }
    // XÓA NHÂN VIÊN
    @GetMapping("/admin/nhan-vien/delete/{id}")
    public String delete(@PathVariable Integer id) {
        nhanVienRepository.deleteById(id);
        return "redirect:/nhan-vien";
    }
    // SỬA NHÂN VIÊN
    @GetMapping("/admin/nhan-vien/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        NhanVien nv = nhanVienRepository.findById(id).orElse(null);
        model.addAttribute("nv", nv);
        return "admin/nhan-vien/edit-nhan-vien";
    }

    // CẬP NHẬT NHÂN VIÊN
    @PostMapping("/admin/nhan-vien/update")
    public String update(@ModelAttribute("nv") NhanVien nv) {
        nhanVienRepository.save(nv); // có id => UPDATE
        return "redirect:/admin/nhan-vien";
    }
}