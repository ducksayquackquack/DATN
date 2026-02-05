package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Repository.HoaDonRepsitory;
import org.example.datnnhom03.Repository.KhachHangRepository;
import org.example.datnnhom03.Repository.NhanVienRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;

@Controller
public class AdminHoaDonController {
    private final HoaDonRepsitory hoaDonRepsitory;
    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;

    public AdminHoaDonController(HoaDonRepsitory hoaDonRepsitory, NhanVienRepository nhanVienRepository, KhachHangRepository khachHangRepository) {
        this.hoaDonRepsitory = hoaDonRepsitory;
        this.nhanVienRepository = nhanVienRepository;
        this.khachHangRepository = khachHangRepository;
    }

    @GetMapping("/admin/hoa-don")
    public String index(Model model) {
        model.addAttribute("list", hoaDonRepsitory.findAll());
        return "admin/hoa-don/form-hoa-don";
    }


    // HIỂN THỊ FORM ADD
    @GetMapping("/admin/hoa-don/create")
    public String create(Model model) {
        model.addAttribute("hd", new HoaDon());
        model.addAttribute("listNV", nhanVienRepository.findAll());
        model.addAttribute("listKH", khachHangRepository.findAll());
        return "admin/hoa-don/add-hoa-don";
    }

    // LƯU HÓA ĐƠN
    @PostMapping("/admin/hoa-don/store")
    public String store(@ModelAttribute("hd") HoaDon hd) {
        hd.setNgayTao(LocalDateTime.now());
        hoaDonRepsitory.save(hd);
        return "redirect:/admin/hoa-don";
    }
    // XÓA HÓA ĐƠN
    @GetMapping("/admin/hoa-don/delete/{id}")
    public String delete(@PathVariable Integer id) {
        hoaDonRepsitory.deleteById(id);
        return "redirect:/admin/hoa-don";
    }
    // SỬA HÓA ĐƠN
    @GetMapping("/admin/hoa-don/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        HoaDon hd = hoaDonRepsitory.findById(id).orElse(null);
        model.addAttribute("hd", hd);
        model.addAttribute("listNV", nhanVienRepository.findAll());
        model.addAttribute("listKH", khachHangRepository.findAll());
        return "admin/hoa-don/edit-hoa-don";
    }

    // CẬP NHẬT HÓA ĐƠN
    @PostMapping("/admin/hoa-don/update")
    public String update(@ModelAttribute("hd") HoaDon formHd) {

        HoaDon dbHd = hoaDonRepsitory.findById(formHd.getId()).orElse(null);

        if (dbHd != null) {
            dbHd.setMaHoaDon(formHd.getMaHoaDon());
            dbHd.setNhanVien(formHd.getNhanVien());
            dbHd.setKhachHang(formHd.getKhachHang());
            dbHd.setSoDienThoaiNhanHang(formHd.getSoDienThoaiNhanHang());
            dbHd.setDiaChiNhanHang(formHd.getDiaChiNhanHang());
            dbHd.setPhiShip(formHd.getPhiShip());
            dbHd.setGiaSauGiamGia(formHd.getGiaSauGiamGia());
            dbHd.setThanhTien(formHd.getThanhTien());
            dbHd.setNgayNhanHangDuKien(formHd.getNgayNhanHangDuKien());
            dbHd.setNgayNhanHangMongMuon(formHd.getNgayNhanHangMongMuon());
            dbHd.setTrangThai(formHd.getTrangThai());

            hoaDonRepsitory.save(dbHd);
        }

        return "redirect:/admin/hoa-don";
    }

}
