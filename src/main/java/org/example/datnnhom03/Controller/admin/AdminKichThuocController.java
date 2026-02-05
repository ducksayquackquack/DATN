package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Model.KichThuoc;
import org.example.datnnhom03.Service.KichThuocService;
import org.example.datnnhom03.dto.KichThuocDTO;
import org.example.datnnhom03.validation.KichThuocValidation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/kich-thuoc")
public class AdminKichThuocController {

    private final KichThuocService kichThuocService;
    private final KichThuocValidation kichThuocValidation;

    public AdminKichThuocController(KichThuocService kichThuocService,
                                    KichThuocValidation kichThuocValidation) {
        this.kichThuocService = kichThuocService;
        this.kichThuocValidation = kichThuocValidation;
    }

    /* LIST */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("activeMenu", "kich-thuoc");
        model.addAttribute("kichThuocs", kichThuocService.findAll());
        return "admin/kich-thuoc/list";
    }

    /* OPEN MODAL - CREATE */
    @GetMapping("/create")
    public String openCreateModal(Model model) {
        model.addAttribute("activeMenu", "kich-thuoc");
        model.addAttribute("kichThuocs", kichThuocService.findAll());
        model.addAttribute("kichThuoc", new KichThuocDTO());
        model.addAttribute("errors", new ArrayList<>());
        return "admin/kich-thuoc/list";
    }

    /* SUBMIT CREATE */
    @PostMapping("/create")
    public String handleCreate(@ModelAttribute("kichThuoc") KichThuocDTO dto,
                               Model model) {

        List<String> errors = kichThuocValidation.validate(dto);

        if (!errors.isEmpty()) {
            model.addAttribute("activeMenu", "kich-thuoc");
            model.addAttribute("kichThuocs", kichThuocService.findAll());
            model.addAttribute("errors", errors);
            return "admin/kich-thuoc/list";
        }

        kichThuocService.create(mapToEntity(dto));
        return "redirect:/admin/kich-thuoc";
    }

    /* OPEN MODAL - UPDATE */
    @GetMapping("/update/{id}")
    public String openUpdateModal(@PathVariable Integer id, Model model) {

        KichThuoc entity = kichThuocService.findById(id);
        if (entity == null) {
            return "redirect:/admin/kich-thuoc";
        }

        model.addAttribute("activeMenu", "kich-thuoc");
        model.addAttribute("kichThuocs", kichThuocService.findAll());
        model.addAttribute("kichThuoc", mapToDto(entity));
        model.addAttribute("errors", new ArrayList<>());
        return "admin/kich-thuoc/list";
    }

    /* SUBMIT UPDATE */
    @PostMapping("/update/{id}")
    public String handleUpdate(@PathVariable Integer id,
                               @ModelAttribute("kichThuoc") KichThuocDTO dto,
                               Model model) {

        List<String> errors = kichThuocValidation.validate(dto);

        if (!errors.isEmpty()) {
            model.addAttribute("activeMenu", "kich-thuoc");
            model.addAttribute("kichThuocs", kichThuocService.findAll());
            model.addAttribute("errors", errors);
            return "admin/kich-thuoc/list";
        }

        kichThuocService.update(id, mapToEntity(dto));
        return "redirect:/admin/kich-thuoc";
    }

    /* DELETE */
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        kichThuocService.delete(id);
        return "redirect:/admin/kich-thuoc";
    }

    /* MAPPING */
    private KichThuoc mapToEntity(KichThuocDTO dto) {
        KichThuoc entity = new KichThuoc();
        entity.setId(dto.getId());
        entity.setMaKichThuoc(dto.getMaKichThuoc());
        entity.setTenKichThuoc(dto.getTenKichThuoc());
        entity.setTrangThai(dto.getTrangThai());
        return entity;
    }

    private KichThuocDTO mapToDto(KichThuoc entity) {
        KichThuocDTO dto = new KichThuocDTO();
        dto.setId(entity.getId());
        dto.setMaKichThuoc(entity.getMaKichThuoc());
        dto.setTenKichThuoc(entity.getTenKichThuoc());
        dto.setTrangThai(entity.getTrangThai());
        return dto;
    }
}
