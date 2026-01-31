package org.example.datnnhom03.Controller;

import org.example.datnnhom03.Model.Loai;
import org.example.datnnhom03.Repository.LoaiRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoaiController {

    private final LoaiRepository loaiRepository;

    public LoaiController(LoaiRepository loaiRepository) {
        this.loaiRepository = loaiRepository;
    }

    @GetMapping("/loai")
    public String index(Model model) {
        model.addAttribute("list", loaiRepository.findAll());
        return "loai/index";
    }

    @GetMapping("/loai/create")
    public String create(Model model) {
        model.addAttribute("loai", new Loai());
        return "loai/create";
    }

    @PostMapping("/loai/store")
    public String store(@ModelAttribute("loai") Loai loai) {
        loaiRepository.save(loai);
        return "redirect:/loai";
    }

    @GetMapping("/loai/delete/{id}")
    public String delete(@PathVariable Integer id) {
        loaiRepository.deleteById(id);
        return "redirect:/loai";
    }

    @GetMapping("/loai/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        Loai loai = loaiRepository.findById(id).orElse(null);
        model.addAttribute("loai", loai);
        return "loai/edit";
    }

    @PostMapping("/loai/update")
    public String update(@ModelAttribute("loai") Loai loai) {
        loaiRepository.save(loai);
        return "redirect:/loai";
    }
}
