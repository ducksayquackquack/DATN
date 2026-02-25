package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.dto.TaiKhoanDTO;
import org.example.datnnhom03.Service.TaiKhoanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@Controller
@RequestMapping("/admin/tai-khoan")
public class AdminTaiKhoanController {

    private final TaiKhoanService taiKhoanService;

    public AdminTaiKhoanController(TaiKhoanService taiKhoanService) {
        this.taiKhoanService = taiKhoanService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("activeMenu", "tai-khoan");
        model.addAttribute("taiKhoans", taiKhoanService.findAll());
        return "admin/tai-khoan/list";
    }

    @GetMapping("/create")
    public String openCreate(Model model) {
        model.addAttribute("activeMenu", "tai-khoan");
        model.addAttribute("taiKhoans", taiKhoanService.findAll());
        model.addAttribute("taiKhoan", new TaiKhoanDTO());
        model.addAttribute("errors", new ArrayList<>());
        return "admin/tai-khoan/list";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute TaiKhoanDTO taiKhoanDTO) {
        taiKhoanService.create(taiKhoanDTO);
        return "redirect:/admin/tai-khoan";
    }

    @GetMapping("/update/{id}")
    public String openUpdate(@PathVariable Integer id, Model model) {
        model.addAttribute("activeMenu", "tai-khoan");
        model.addAttribute("taiKhoans", taiKhoanService.findAll());
        model.addAttribute("taiKhoan", taiKhoanService.findById(id));
        model.addAttribute("errors", new ArrayList<>());
        return "admin/tai-khoan/list";
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable Integer id,
                         @ModelAttribute TaiKhoanDTO taiKhoanDTO) {
        taiKhoanService.update(id, taiKhoanDTO);
        return "redirect:/admin/tai-khoan";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        taiKhoanService.delete(id);
        return "redirect:/admin/tai-khoan";
    }
}