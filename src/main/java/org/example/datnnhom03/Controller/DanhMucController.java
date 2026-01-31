package org.example.datnnhom03.Controller;

import org.example.datnnhom03.Model.DanhMuc;
import org.example.datnnhom03.Service.DanhMucService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/danh-muc")
public class DanhMucController {

    private final DanhMucService service;

    public DanhMucController(DanhMucService service) {
        this.service = service;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("list", service.findAll());
        return "danh-muc/index";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("dm", new DanhMuc());
        return "danh-muc/create";
    }

    @PostMapping("/store")
    public String store(@ModelAttribute("dm") DanhMuc dm) {
        service.save(dm);
        return "redirect:/danh-muc";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        service.delete(id);
        return "redirect:/danh-muc";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("dm", service.findById(id));
        return "danh-muc/edit";
    }

    @PostMapping("/update")
    public String update(@ModelAttribute("dm") DanhMuc dm) {
        service.save(dm);
        return "redirect:/danh-muc";
    }

}
