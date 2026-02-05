package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Service.PhuongThucThanhToanService;
import org.example.datnnhom03.dto.PhuongThucThanhToanDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@Controller
@RequestMapping("/admin/phuong-thuc-thanh-toan")
public class AdminPhuongThucThanhToanController {

    private final PhuongThucThanhToanService service;

    public AdminPhuongThucThanhToanController(PhuongThucThanhToanService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("activeMenu", "pttt");
        model.addAttribute("pttts", service.findAll());
        return "admin/phuong-thuc-thanh-toan/list";
    }

    @GetMapping("/create")
    public String openCreate(Model model) {
        model.addAttribute("activeMenu", "pttt");
        model.addAttribute("pttts", service.findAll());
        model.addAttribute("pttt", new PhuongThucThanhToanDTO());
        model.addAttribute("errors", new ArrayList<>());
        return "admin/phuong-thuc-thanh-toan/list";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute("pttt") PhuongThucThanhToanDTO dto,
                         Model model) {

        service.create(dto);
        return "redirect:/admin/phuong-thuc-thanh-toan";
    }

    @GetMapping("/update/{id}")
    public String openUpdate(@PathVariable Integer id, Model model) {

        PhuongThucThanhToanDTO dto = service.findById(id);
        if (dto == null) {
            return "redirect:/admin/phuong-thuc-thanh-toan";
        }

        model.addAttribute("activeMenu", "pttt");
        model.addAttribute("pttts", service.findAll());
        model.addAttribute("pttt", dto);
        model.addAttribute("errors", new ArrayList<>());
        return "admin/phuong-thuc-thanh-toan/list";
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable Integer id,
                         @ModelAttribute("pttt") PhuongThucThanhToanDTO dto,
                         Model model) {

        service.update(id, dto);
        return "redirect:/admin/phuong-thuc-thanh-toan";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        service.delete(id);
        return "redirect:/admin/phuong-thuc-thanh-toan";
    }
}
