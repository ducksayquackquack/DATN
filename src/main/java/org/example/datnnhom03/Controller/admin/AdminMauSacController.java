package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Model.MauSac;
import org.example.datnnhom03.Repository.MauSacRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/mau-sac")
public class AdminMauSacController {

    private final MauSacRepository mauSacRepository;

    public AdminMauSacController(MauSacRepository mauSacRepository) {
        this.mauSacRepository = mauSacRepository;
    }


    @GetMapping
    public String index(Model model) {
        model.addAttribute("list", mauSacRepository.findAll());
        model.addAttribute("activeMenu", "mau-sac");
        return "/admin/mau-sac/list";
    }


    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("ms", new MauSac());
        model.addAttribute("activeMenu", "mau-sac");
        return "/admin/mau-sac/form";
    }


    @PostMapping("/save")
    public String save(@ModelAttribute("ms") MauSac ms) {

        if (ms.getId() == null) {
            ms.setNgayTao(LocalDateTime.now());
            ms.setNgaySua(LocalDateTime.now());
            mauSacRepository.save(ms);
        } else {
            MauSac old = mauSacRepository.findById(ms.getId()).orElse(null);
            if (old != null) {
                old.setMaMau(ms.getMaMau());
                old.setTenMau(ms.getTenMau());
                old.setTrangThai(ms.getTrangThai());
                old.setNgaySua(LocalDateTime.now());
                mauSacRepository.save(old);
            }
        }

        return "redirect:/admin/mau-sac";
    }


    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        MauSac ms = mauSacRepository.findById(id).orElse(null);
        if (ms == null) {
            return "redirect:/admin/mau-sac";
        }
        model.addAttribute("ms", ms);
        model.addAttribute("activeMenu", "mau-sac");
        return "/admin/mau-sac/form";
    }


    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        mauSacRepository.deleteById(id);
        return "redirect:/admin/mau-sac";
    }
}
