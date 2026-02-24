package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Model.PTTT;
import org.example.datnnhom03.Service.PTTTService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@Controller
@RequestMapping("/admin/phuong-thuc-thanh-toan")
public class AdminPhuongThucThanhToanController {

    private final PTTTService service;

    public AdminPhuongThucThanhToanController(PTTTService service) {
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
        model.addAttribute("pttt", new PTTT());
        model.addAttribute("errors", new ArrayList<>());
        return "admin/phuong-thuc-thanh-toan/list";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute("pttt") PTTT pttt) {
        service.create(pttt);
        return "redirect:/admin/phuong-thuc-thanh-toan";
    }

    @GetMapping("/update/{id}")
    public String openUpdate(@PathVariable Integer id, Model model) {

        PTTT pttt = service.findById(id);
        if (pttt == null) {
            return "redirect:/admin/phuong-thuc-thanh-toan";
        }

        model.addAttribute("activeMenu", "pttt");
        model.addAttribute("pttts", service.findAll());
        model.addAttribute("pttt", pttt);
        model.addAttribute("errors", new ArrayList<>());
        return "admin/phuong-thuc-thanh-toan/list";
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable Integer id,
                         @ModelAttribute("pttt") PTTT pttt) {

        pttt.setId(id);
        service.update(id, pttt);
        return "redirect:/admin/phuong-thuc-thanh-toan";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        service.delete(id);
        return "redirect:/admin/phuong-thuc-thanh-toan";
    }
}
