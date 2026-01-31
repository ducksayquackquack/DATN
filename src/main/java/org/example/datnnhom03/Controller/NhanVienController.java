package org.example.datnnhom03.Controller;

import org.example.datnnhom03.Model.NhanVien;
import org.example.datnnhom03.Repsotiory.NhanVienRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class NhanVienController {

    private final NhanVienRepository nhanVienRepository;

    public NhanVienController(NhanVienRepository nhanVienRepository) {
        this.nhanVienRepository = nhanVienRepository;
    }

    @GetMapping("/nhan-vien")
    public String index(Model model) {
        model.addAttribute("list", nhanVienRepository.findAll());
        return "nhan-vien/form-nhan-vien";
    }


    // HIỂN THỊ FORM ADD
    @GetMapping("/nhan-vien/create")
    public String create(Model model) {
        model.addAttribute("nv", new NhanVien());
        return "nhan-vien/add-nhan-vien";
    }

    // LƯU NHÂN VIÊN
    @PostMapping("/nhan-vien/store")
    public String store(@ModelAttribute("nv") NhanVien nv) {
        nhanVienRepository.save(nv);
        return "redirect:/nhan-vien";
    }
    // XÓA NHÂN VIÊN
    @GetMapping("/nhan-vien/delete/{id}")
    public String delete(@PathVariable Integer id) {
        nhanVienRepository.deleteById(id);
        return "redirect:/nhan-vien";
    }
    // SỬA NHÂN VIÊN
    @GetMapping("/nhan-vien/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        NhanVien nv = nhanVienRepository.findById(id).orElse(null);
        model.addAttribute("nv", nv);
        return "nhan-vien/edit-nhan-vien";
    }

    // CẬP NHẬT NHÂN VIÊN
    @PostMapping("/nhan-vien/update")
    public String update(@ModelAttribute("nv") NhanVien nv) {
        nhanVienRepository.save(nv); // có id => UPDATE
        return "redirect:/nhan-vien";
    }
}