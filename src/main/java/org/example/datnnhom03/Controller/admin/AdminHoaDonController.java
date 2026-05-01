package org.example.datnnhom03.Controller.admin;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Repository.*;
import org.example.datnnhom03.Service.HoaDonTinhTienService;
import org.example.datnnhom03.dto.hoadon.OrderStatusOptionDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/hoa-don")
public class AdminHoaDonController {

    private final HoaDonRepository hoaDonRepository;
    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;
    private final OrderStatusLookupRepository orderStatusLookupRepository;
    private final HoaDonTinhTienService hoaDonTinhTienService;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    public AdminHoaDonController(HoaDonRepository hoaDonRepository,
                                 NhanVienRepository nhanVienRepository,
                                 KhachHangRepository khachHangRepository,
                                 OrderStatusLookupRepository orderStatusLookupRepository,
                                 HoaDonTinhTienService hoaDonTinhTienService,
                                 SanPhamChiTietRepository sanPhamChiTietRepository,
                                 HoaDonChiTietRepository hoaDonChiTietRepository,
                                 OrderStatusHistoryRepository orderStatusHistoryRepository) {
        this.hoaDonRepository = hoaDonRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.khachHangRepository = khachHangRepository;
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
        this.orderStatusLookupRepository = orderStatusLookupRepository;
        this.hoaDonTinhTienService = hoaDonTinhTienService;
        this.hoaDonChiTietRepository = hoaDonChiTietRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
    }

    /* =========================
       LIST
       ========================= */
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer statusId,
                       Model model) {

        model.addAttribute("activeMenu", "hoa-don");

        // dropdown trạng thái
        var statusList = orderStatusLookupRepository.findAllOptions();
        model.addAttribute("statusList", statusList);

        model.addAttribute("keyword", keyword);
        model.addAttribute("statusId", statusId);

        // list hóa đơn đã filter
        var list = hoaDonRepository.search(keyword, statusId);
        model.addAttribute("list", list);

        // ✅ map OrderStatusId -> Name để hiển thị trong table
        var statusMap = statusList.stream()
                .collect(java.util.stream.Collectors.toMap(
                        OrderStatusOptionDto::id,
                        OrderStatusOptionDto::name
                ));
        model.addAttribute("statusMap", statusMap);

        return "admin/hoa-don/list-hoa-don";
    }

    /* =========================
       OPEN FORM - CREATE
       ========================= */
    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("activeMenu", "hoa-don");
        model.addAttribute("hd", new HoaDon());
        model.addAttribute("listNV", nhanVienRepository.findAll());
        model.addAttribute("listKH", khachHangRepository.findAll());
        return "admin/hoa-don/form-hoa-don";
    }

    /* =========================
       SUBMIT CREATE
       ========================= */
    @PostMapping("/store")
    public String store(@ModelAttribute("hd") HoaDon hd,
                        @RequestParam(required = false) String phiShipText,
                        Model model) {
        try {
            hd.setNgayTao(LocalDateTime.now());

            // ✅ set trạng thái mặc định
            Integer choXacNhanId = orderStatusLookupRepository.findIdByCode("CHO_XAC_NHAN");
            if (choXacNhanId == null) choXacNhanId = 1;
            hd.setOrderStatusId(choXacNhanId);

            // ✅ attach NV/KH từ DB
            if (hd.getNhanVien() != null && hd.getNhanVien().getId() != null) {
                hd.setNhanVien(nhanVienRepository.findById(hd.getNhanVien().getId()).orElseThrow());
            }
            if (hd.getKhachHang() != null && hd.getKhachHang().getId() != null) {
                hd.setKhachHang(khachHangRepository.findById(hd.getKhachHang().getId()).orElseThrow());
            }

            // ✅ parse phí ship từ text (chống lỗi locale 50.000 / 50,000)
            if (phiShipText != null) {
                String raw = phiShipText.trim()
                        .replace(" ", "")
                        .replace(".", "")
                        .replace(",", "");
                if (!raw.isEmpty()) {
                    hd.setPhiShip(new java.math.BigDecimal(raw));
                }
            }
            if (hd.getPhiShip() == null) hd.setPhiShip(java.math.BigDecimal.ZERO);

            // ✅ TRIM mã
            String ma = (hd.getMaHoaDon() == null) ? "" : hd.getMaHoaDon().trim();
            if (ma.isEmpty()) {
                ma = generateMaHoaDon();
            }
            hd.setMaHoaDon(ma);

            System.out.println("CREATE phiShip = " + hd.getPhiShip());

            hoaDonRepository.saveAndFlush(hd); // ✅ flush cho chắc chắn ghi DB ngay

            HoaDon saved = hoaDonRepository.findById(hd.getId()).orElseThrow();
            System.out.println("AFTER SAVE phiShip = " + saved.getPhiShip());

            logStatusChange(hd.getId(), null, hd.getOrderStatusId(), "Tạo đơn hàng");

            return "redirect:/admin/hoa-don/detail/" + hd.getId();

        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "❌ Mã hóa đơn bị trùng. Thử lưu lại!");
            model.addAttribute("hd", hd);
            model.addAttribute("listNV", nhanVienRepository.findAll());
            model.addAttribute("listKH", khachHangRepository.findAll());
            return "admin/hoa-don/form-hoa-don";
        }
    }

    // ✅ sinh mã: HD + yyyyMMddHHmmss + random 3 số
    private String generateMaHoaDon() {
        String time = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(LocalDateTime.now());
        int rnd = new java.util.Random().nextInt(900) + 100;
        return "HD" + time + rnd;
    }

    /* =========================
       OPEN FORM - UPDATE
       ========================= */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {

        HoaDon hd = hoaDonRepository.findById(id).orElse(null);
        if (hd == null) {
            return "redirect:/admin/hoa-don";
        }

        model.addAttribute("activeMenu", "hoa-don");
        model.addAttribute("hd", hd);
        model.addAttribute("listNV", nhanVienRepository.findAll());
        model.addAttribute("listKH", khachHangRepository.findAll());
        return "admin/hoa-don/form-hoa-don";
    }

    /* =========================
       SUBMIT UPDATE
       ========================= */
    @PostMapping("/update")
    public String update(@ModelAttribute("hd") HoaDon formHd) {
        HoaDon db = hoaDonRepository.findById(formHd.getId()).orElse(null);
        if (db == null) return "redirect:/admin/hoa-don";

        // ✅ chặn nếu đơn final (nghiệp vụ giống AoKhoac)
        String code = orderStatusLookupRepository.findCodeById(db.getOrderStatusId());
        if (isFinalCode(code)) return "redirect:/admin/hoa-don";

        db.setMaHoaDon(formHd.getMaHoaDon());

        // attach NV/KH từ DB (tránh binding bậy)
        if (formHd.getNhanVien() != null && formHd.getNhanVien().getId() != null) {
            db.setNhanVien(nhanVienRepository.findById(formHd.getNhanVien().getId()).orElse(null));
        }
        if (formHd.getKhachHang() != null && formHd.getKhachHang().getId() != null) {
            db.setKhachHang(khachHangRepository.findById(formHd.getKhachHang().getId()).orElse(null));
        }

        db.setSoDienThoaiNhanHang(formHd.getSoDienThoaiNhanHang());
        db.setDiaChiNhanHang(formHd.getDiaChiNhanHang());
        db.setNgayNhanHangDuKien(formHd.getNgayNhanHangDuKien());
        db.setNgayNhanHangMongMuon(formHd.getNgayNhanHangMongMuon());

        // ✅ chỉ cho sửa ship/giảm, còn thành tiền auto
        db.setPhiShip(formHd.getPhiShip());
        db.setGiaSauGiamGia(formHd.getGiaSauGiamGia());

        // ❌ không đụng thanhTien ở đây
        // db.setThanhTien(formHd.getThanhTien());

        hoaDonRepository.save(db);
        hoaDonTinhTienService.recalc(db.getId()); // ✅ tính lại tổng

        return "redirect:/admin/hoa-don";
    }

    private boolean isFinalCode(String code){
        return "HOAN_THANH".equals(code) || "HUY".equals(code) || "HOAN_VE".equals(code);
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Integer id,
                         @RequestParam String reason) {

        if (reason == null || reason.trim().isEmpty())
            return "redirect:/admin/hoa-don/detail/" + id + "?err=reason_required";

        HoaDon hd = hoaDonRepository.findById(id).orElse(null);
        if (hd == null) return "redirect:/admin/hoa-don";

        String code = orderStatusLookupRepository.findCodeById(hd.getOrderStatusId());
        if (isFinalCode(code)) return "redirect:/admin/hoa-don/detail/" + id;

        Integer fromId = hd.getOrderStatusId();
        Integer toId = orderStatusLookupRepository.findIdByCode("HUY");

        hd.setOrderStatusId(toId);
        hd.setNgayHuy(LocalDateTime.now());
        hoaDonRepository.save(hd);

        logStatusChange(id, fromId, toId, "Huỷ đơn: " + reason.trim()); // ✅ GHI HISTORY

        return "redirect:/admin/hoa-don/detail/" + id;
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        HoaDon hd = hoaDonRepository.findById(id).orElse(null);
        if (hd == null) return "redirect:/admin/hoa-don";

        String statusName = orderStatusLookupRepository.findNameById(hd.getOrderStatusId());
        String statusCode = orderStatusLookupRepository.findCodeById(hd.getOrderStatusId());

        model.addAttribute("activeMenu", "hoa-don");
        model.addAttribute("hd", hd);
        model.addAttribute("trangThaiName", statusName);

        // items + history
        model.addAttribute("items", hoaDonChiTietRepository.findByHoaDon_Id(id));
        model.addAttribute("history", orderStatusHistoryRepository.viewByHoaDonId(id));

        boolean finalOrder = isFinalCode(statusCode);
        model.addAttribute("finalOrder", finalOrder);
        model.addAttribute("statusCode", statusCode);

        return "admin/hoa-don/detail-hoa-don";
    }

    @PostMapping("/{id}/ship")
    public String ship(@PathVariable Integer id){
        HoaDon hd = hoaDonRepository.findById(id).orElse(null);
        if (hd == null) return "redirect:/admin/hoa-don";

        String code = orderStatusLookupRepository.findCodeById(hd.getOrderStatusId());
        if (isFinalCode(code)) return "redirect:/admin/hoa-don/detail/" + id;

        // CHO_LAY_HANG -> DANG_GIAO
        if (!"CHO_LAY_HANG".equals(code)) return "redirect:/admin/hoa-don/detail/" + id;

        Integer toId = orderStatusLookupRepository.findIdByCode("DANG_GIAO");
        hd.setOrderStatusId(toId);
        hoaDonRepository.save(hd);
        return "redirect:/admin/hoa-don/detail/" + id;
    }

    @PostMapping("/{id}/items/add-by-variant")
    public String addItemByVariant(@PathVariable Integer id,
                                   @RequestParam Integer productId,
                                   @RequestParam Integer sizeId,
                                   @RequestParam Integer colorId,
                                   @RequestParam Integer soLuong) {

        HoaDon hd = hoaDonRepository.findById(id).orElse(null);
        if (hd == null) return "redirect:/admin/hoa-don";
        String code = orderStatusLookupRepository.findCodeById(hd.getOrderStatusId());
        if (isFinalCode(code)) return "redirect:/admin/hoa-don/detail/" + id;

        var spct = sanPhamChiTietRepository.findVariant(productId, sizeId, colorId).orElse(null);
        if (spct == null) return "redirect:/admin/hoa-don/detail/" + id;

        // cộng dồn nếu đã có dòng
        var lineOpt = hoaDonChiTietRepository.findByHoaDon_IdAndSanPhamChiTiet_Id(id, spct.getId());
        var line = lineOpt.orElseGet(() -> {
            var x = new org.example.datnnhom03.Model.HoaDonChiTiet();
            x.setHoaDon(hd);
            x.setSanPhamChiTiet(spct);
            x.setSoLuong(0);
            x.setTrangThai("ACTIVE");
            x.setThanhTien(java.math.BigDecimal.ZERO);
            return x;
        });

        int oldQty = (line.getSoLuong() == null ? 0 : line.getSoLuong());
        int newQty = oldQty + (soLuong == null ? 0 : soLuong);
        if (newQty <= 0) return "redirect:/admin/hoa-don/detail/" + id;

        // check tồn kho
        Integer ton = spct.getSoLuong();
        if (ton != null && newQty > ton) return "redirect:/admin/hoa-don/detail/" + id;

        line.setSoLuong(newQty);

        java.math.BigDecimal giaBan = (spct.getGiaBan() == null) ? java.math.BigDecimal.ZERO : spct.getGiaBan();
        line.setThanhTien(giaBan.multiply(java.math.BigDecimal.valueOf(newQty)));

        hoaDonChiTietRepository.save(line);

        // tính lại tổng tiền
        hoaDonTinhTienService.recalc(id);

        return "redirect:/admin/hoa-don/detail/" + id;
    }


    @PostMapping("/{hoaDonId}/items/{itemId}/delete")
    public String deleteItem(@PathVariable Integer hoaDonId,
                             @PathVariable Integer itemId) {

        HoaDon hd = hoaDonRepository.findById(hoaDonId).orElse(null);
        if (hd == null) return "redirect:/admin/hoa-don";
        String code = orderStatusLookupRepository.findCodeById(hd.getOrderStatusId());
        if (isFinalCode(code)) return "redirect:/admin/hoa-don/detail/" + hoaDonId;

        hoaDonChiTietRepository.deleteById(itemId);
        hoaDonTinhTienService.recalc(hoaDonId);

        return "redirect:/admin/hoa-don/detail/" + hoaDonId;
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Integer id) {
        HoaDon hd = hoaDonRepository.findById(id).orElse(null);
        if (hd == null) return "redirect:/admin/hoa-don";

        String code = orderStatusLookupRepository.findCodeById(hd.getOrderStatusId());
        if (isFinalCode(code)) return "redirect:/admin/hoa-don/detail/" + id;
        if (!"CHO_XAC_NHAN".equals(code)) return "redirect:/admin/hoa-don/detail/" + id;

        Integer toId = orderStatusLookupRepository.findIdByCode("CHO_LAY_HANG");
        hd.setOrderStatusId(toId);
        hoaDonRepository.save(hd);

        // TODO: lưu history (mục E)
        return "redirect:/admin/hoa-don/detail/" + id;
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Integer id) {
        HoaDon hd = hoaDonRepository.findById(id).orElse(null);
        if (hd == null) return "redirect:/admin/hoa-don";

        String code = orderStatusLookupRepository.findCodeById(hd.getOrderStatusId());
        if (isFinalCode(code)) return "redirect:/admin/hoa-don/detail/" + id;
        if (!"DANG_GIAO".equals(code)) return "redirect:/admin/hoa-don/detail/" + id;

        Integer toId = orderStatusLookupRepository.findIdByCode("HOAN_THANH");
        hd.setOrderStatusId(toId);
        hoaDonRepository.save(hd);

        return "redirect:/admin/hoa-don/detail/" + id;
    }

    @PostMapping("/{id}/fail-delivery")
    public String failDelivery(@PathVariable Integer id,
                               @RequestParam(required = false) String note) {
        HoaDon hd = hoaDonRepository.findById(id).orElse(null);
        if (hd == null) return "redirect:/admin/hoa-don";

        String code = orderStatusLookupRepository.findCodeById(hd.getOrderStatusId());
        if (isFinalCode(code)) return "redirect:/admin/hoa-don/detail/" + id;
        if (!"DANG_GIAO".equals(code)) return "redirect:/admin/hoa-don/detail/" + id;

        Integer toId = orderStatusLookupRepository.findIdByCode("GIAO_THAT_BAI");
        hd.setOrderStatusId(toId);
        hoaDonRepository.save(hd);

        return "redirect:/admin/hoa-don/detail/" + id;
    }

    @PostMapping("/{id}/returned")
    public String returned(@PathVariable Integer id,
                           @RequestParam(required = false) String note) {
        HoaDon hd = hoaDonRepository.findById(id).orElse(null);
        if (hd == null) return "redirect:/admin/hoa-don";

        String code = orderStatusLookupRepository.findCodeById(hd.getOrderStatusId());
        if (isFinalCode(code)) return "redirect:/admin/hoa-don/detail/" + id;
        if (!"GIAO_THAT_BAI".equals(code)) return "redirect:/admin/hoa-don/detail/" + id;

        Integer toId = orderStatusLookupRepository.findIdByCode("HOAN_VE");
        hd.setOrderStatusId(toId);
        hoaDonRepository.save(hd);

        return "redirect:/admin/hoa-don/detail/" + id;
    }

    private void logStatusChange(Integer hoaDonId, Integer fromStatusId, Integer toStatusId, String note){
        orderStatusHistoryRepository.insertHistory(
                hoaDonId, fromStatusId, toStatusId, LocalDateTime.now(), note
        );
    }
}
