package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Model.NhanVien;
import org.example.datnnhom03.Repository.NhanVienRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/nhan-vien")
public class AdminNhanVienController {

    private final NhanVienRepository nhanVienRepository;

    public AdminNhanVienController(NhanVienRepository nhanVienRepository) {
        this.nhanVienRepository = nhanVienRepository;
    }

    /* =========================
       LIST
       ========================= */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("activeMenu", "nhan-vien");
        model.addAttribute("list", nhanVienRepository.findAll());
        return "admin/nhan-vien/list-nhan-vien";
    }

    /* =========================
       OPEN FORM - CREATE
       ========================= */
    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("activeMenu", "nhan-vien");
        model.addAttribute("nv", new NhanVien());
        return "admin/nhan-vien/form-nhan-vien";
    }

    /* =========================
       SUBMIT CREATE
       ========================= */
    @PostMapping("/store")
    public String store(@ModelAttribute("nv") NhanVien nv) {
        nhanVienRepository.save(nv);
        return "redirect:/admin/nhan-vien";
    }

    /* =========================
       OPEN FORM - UPDATE
       ========================= */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {

        NhanVien nv = nhanVienRepository.findById(id).orElse(null);
        if (nv == null) {
            return "redirect:/admin/nhan-vien";
        }

        model.addAttribute("activeMenu", "nhan-vien");
        model.addAttribute("nv", nv);
        return "admin/nhan-vien/form-nhan-vien";
    }

    /* =========================
       SUBMIT UPDATE
       ========================= */
    @PostMapping("/update")
    public String update(@ModelAttribute("nv") NhanVien nv) {
        nhanVienRepository.save(nv); // có id => update
        return "redirect:/admin/nhan-vien";
    }

    /* =========================
       DELETE
       ========================= */
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        nhanVienRepository.deleteById(id);
        return "redirect:/admin/nhan-vien";
    }
}
