package org.example.datnnhom03.Controller;

import org.example.datnnhom03.Repository.PTTTRepository;
import org.example.datnnhom03.Model.PTTT;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;


@Controller
@RequestMapping("/pttt")
public class PTTTController {

    private final PTTTRepository repo;

    public PTTTController(PTTTRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("list", repo.findAll());
        return "pttt/index";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Integer id) {
        PTTT p = repo.findById(id).orElseThrow();

        if ("Hoạt động".equals(p.getTrangThai())) {
            p.setTrangThai("Ngừng");
        } else {
            p.setTrangThai("Hoạt động");
        }

        repo.save(p);
        return "redirect:/pttt";
    }
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("pttt", new PTTT());
        return "pttt/create";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute PTTT pttt) {
        pttt.setTrangThai("Hoạt động");
        pttt.setNgayTao(LocalDateTime.now());
        pttt.setNgaySua(LocalDateTime.now());
        repo.save(pttt);
        return "redirect:/pttt";
    }

}