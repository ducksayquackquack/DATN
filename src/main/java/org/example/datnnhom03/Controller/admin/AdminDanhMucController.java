package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Model.DanhMuc;
import org.example.datnnhom03.Service.DanhMucService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@Controller
@RequestMapping("/admin/danh-muc")
public class AdminDanhMucController {

    private final DanhMucService service;

    public AdminDanhMucController(DanhMucService service) {
        this.service = service;
    }

    /* LIST */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("activeMenu", "danh-muc");
        model.addAttribute("danhMucs", service.findAll());
        return "admin/danh-muc/list";
    }

    /* OPEN CREATE MODAL */
    @GetMapping("/create")
    public String openCreate(Model model) {
        model.addAttribute("activeMenu", "danh-muc");
        model.addAttribute("danhMucs", service.findAll());
        model.addAttribute("danhMuc", new DanhMuc());
        model.addAttribute("errors", new ArrayList<>());
        return "admin/danh-muc/list";
    }

    /* SUBMIT CREATE */
    @PostMapping("/create")
    public String create(@ModelAttribute DanhMuc danhMuc) {
        service.create(danhMuc);
        return "redirect:/admin/danh-muc";
    }

    /* OPEN UPDATE MODAL */
    @GetMapping("/update/{id}")
    public String openUpdate(@PathVariable Integer id, Model model) {
        model.addAttribute("activeMenu", "danh-muc");
        model.addAttribute("danhMucs", service.findAll());
        model.addAttribute("danhMuc", service.findById(id));
        model.addAttribute("errors", new ArrayList<>());
        return "admin/danh-muc/list";
    }

    /* SUBMIT UPDATE */
    @PostMapping("/update/{id}")
    public String update(@PathVariable Integer id,
                         @ModelAttribute DanhMuc danhMuc) {
        service.update(id, danhMuc);
        return "redirect:/admin/danh-muc";
    }

    /* DELETE */
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        service.delete(id);
        return "redirect:/admin/danh-muc";
    }
}
