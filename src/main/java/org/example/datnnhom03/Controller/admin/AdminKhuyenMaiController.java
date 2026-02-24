package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Model.KhuyenMai;
import org.example.datnnhom03.Service.KhuyenMaiService;
import org.example.datnnhom03.dto.KhuyenMaiDTO;
import org.example.datnnhom03.validation.KhuyenMaiValidation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/khuyen-mai")
public class AdminKhuyenMaiController {

    private final KhuyenMaiService khuyenMaiService;
    private final KhuyenMaiValidation khuyenMaiValidation;

    public AdminKhuyenMaiController(KhuyenMaiService khuyenMaiService,
                                    KhuyenMaiValidation khuyenMaiValidation) {
        this.khuyenMaiService = khuyenMaiService;
        this.khuyenMaiValidation = khuyenMaiValidation;
    }

    /* =========================
       LIST (không modal)
       ========================= */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("activeMenu", "khuyen-mai");
        model.addAttribute("khuyenMais", khuyenMaiService.findAll());
        return "admin/khuyen-mai/list";
    }

    /* =========================
       OPEN MODAL - CREATE
       ========================= */
    @GetMapping("/create")
    public String openCreateModal(Model model) {
        model.addAttribute("activeMenu", "khuyen-mai");
        model.addAttribute("khuyenMais", khuyenMaiService.findAll());
        model.addAttribute("khuyenMai", new KhuyenMaiDTO());
        model.addAttribute("errors", new ArrayList<String>());
        return "admin/khuyen-mai/list";
    }

    /* =========================
       SUBMIT CREATE
       ========================= */
    @PostMapping("/create")
    public String handleCreate(@ModelAttribute("khuyenMai") KhuyenMaiDTO dto,
                               Model model) {

        List<String> errors = khuyenMaiValidation.validate(dto);

        if (!errors.isEmpty()) {
            model.addAttribute("activeMenu", "khuyen-mai");
            model.addAttribute("khuyenMais", khuyenMaiService.findAll());
            model.addAttribute("errors", errors);
            return "admin/khuyen-mai/list"; // giữ modal
        }

        khuyenMaiService.create(mapToEntity(dto));
        return "redirect:/admin/khuyen-mai";
    }

    /* =========================
       OPEN MODAL - UPDATE
       ========================= */
    @GetMapping("/update/{id}")
    public String openUpdateModal(@PathVariable Integer id, Model model) {

        KhuyenMai entity = khuyenMaiService.findById(id);
        if (entity == null) {
            return "redirect:/admin/khuyen-mai";
        }

        model.addAttribute("activeMenu", "khuyen-mai");
        model.addAttribute("khuyenMais", khuyenMaiService.findAll());
        model.addAttribute("khuyenMai", mapToDto(entity));
        model.addAttribute("errors", new ArrayList<String>());

        return "admin/khuyen-mai/list";
    }

    /* =========================
       SUBMIT UPDATE
       ========================= */
    @PostMapping("/update/{id}")
    public String handleUpdate(@PathVariable Integer id,
                               @ModelAttribute("khuyenMai") KhuyenMaiDTO dto,
                               Model model) {

        List<String> errors = khuyenMaiValidation.validate(dto);

        if (!errors.isEmpty()) {
            model.addAttribute("activeMenu", "khuyen-mai");
            model.addAttribute("khuyenMais", khuyenMaiService.findAll());
            model.addAttribute("errors", errors);
            return "admin/khuyen-mai/list"; // giữ modal
        }

        khuyenMaiService.update(id, mapToEntity(dto));
        return "redirect:/admin/khuyen-mai";
    }

    /* =========================
       DELETE
       ========================= */
    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        khuyenMaiService.delete(id);
        return "redirect:/admin/khuyen-mai";
    }

    /* =========================
       MAPPING DTO <-> ENTITY
       ========================= */
    private KhuyenMai mapToEntity(KhuyenMaiDTO dto) {
        KhuyenMai entity = new KhuyenMai();
        entity.setId(dto.getId());
        entity.setMaKhuyenMai(dto.getMaKhuyenMai());
        entity.setTenKhuyenMai(dto.getTenKhuyenMai());

        entity.setGiaTri(
                dto.getGiaTriGiam() == null ? null : BigDecimal.valueOf(dto.getGiaTriGiam())
        );

        entity.setNgayBatDau(
                dto.getNgayBatDau() == null ? null : dto.getNgayBatDau().atStartOfDay()
        );
        entity.setNgayKetThuc(
                dto.getNgayKetThuc() == null ? null : dto.getNgayKetThuc().atStartOfDay()
        );

        entity.setTrangThai(dto.getTrangThai());
        return entity;
    }

    private KhuyenMaiDTO mapToDto(KhuyenMai entity) {
        KhuyenMaiDTO dto = new KhuyenMaiDTO();
        dto.setId(entity.getId());
        dto.setMaKhuyenMai(entity.getMaKhuyenMai());
        dto.setTenKhuyenMai(entity.getTenKhuyenMai());

        dto.setGiaTriGiam(
                entity.getGiaTri() == null ? null : entity.getGiaTri().intValue()
        );

        dto.setNgayBatDau(
                entity.getNgayBatDau() == null ? null : entity.getNgayBatDau().toLocalDate()
        );
        dto.setNgayKetThuc(
                entity.getNgayKetThuc() == null ? null : entity.getNgayKetThuc().toLocalDate()
        );

        dto.setTrangThai(entity.getTrangThai());
        return dto;
    }
}
