package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Model.Loai;
import org.example.datnnhom03.Service.LoaiService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@Controller
@RequestMapping("/admin/loai")
public class AdminLoaiController {

    private final LoaiService loaiService;

    public AdminLoaiController(LoaiService loaiService) {
        this.loaiService = loaiService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("activeMenu", "loai");
        model.addAttribute("loais", loaiService.findAll());
        return "admin/loai/list";
    }

    @GetMapping("/create")
    public String openCreate(Model model) {
        model.addAttribute("activeMenu", "loai");
        model.addAttribute("loais", loaiService.findAll());
        model.addAttribute("loai", new Loai());
        model.addAttribute("errors", new ArrayList<>());
        return "admin/loai/list";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute Loai loai) {
        loaiService.create(loai);
        return "redirect:/admin/loai";
    }

    @GetMapping("/update/{id}")
    public String openUpdate(@PathVariable Integer id, Model model) {
        model.addAttribute("activeMenu", "loai");
        model.addAttribute("loais", loaiService.findAll());
        model.addAttribute("loai", loaiService.findById(id));
        model.addAttribute("errors", new ArrayList<>());
        return "admin/loai/list";
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable Integer id,
                         @ModelAttribute Loai loai) {
        loaiService.update(id, loai);
        return "redirect:/admin/loai";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        loaiService.delete(id);
        return "redirect:/admin/loai";
    }
}
