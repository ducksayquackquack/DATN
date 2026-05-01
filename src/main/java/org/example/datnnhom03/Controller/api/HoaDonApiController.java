package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Mapper.HoaDonMapper;
import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Model.HoaDonChiTiet;
import org.example.datnnhom03.Model.KhachHang;
import org.example.datnnhom03.Model.SanPhamChiTiet;
import org.example.datnnhom03.Repository.*;
import org.example.datnnhom03.Repository.projection.OrderStatusHistoryView;
import org.example.datnnhom03.Service.HoaDonTinhTienService;
import org.example.datnnhom03.dto.hoadon.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/hoa-don")
public class HoaDonApiController {

    private static final String ORDER_TYPE_ONLINE = "ONLINE";
    private static final String ORDER_TYPE_POS = "POS";
    private static final String PAYMENT_METHOD_COD = "COD";
    private static final String PAYMENT_METHOD_CASH = "CASH";
    private static final String PAYMENT_FLOW_CUSTOMER_CONFIRMED = "[PAYMENT_FLOW:VNPAY_CUSTOMER_CONFIRMED]";
    private static final String PAYMENT_FLOW_EMPLOYEE_CONFIRMED = "[PAYMENT_FLOW:VNPAY_EMPLOYEE_CONFIRMED]";
    private static final String DEFAULT_LOOKUP_URL = "http://localhost:5173/tra-cuu-don-hang";

    private static final String EVENT_GIAO_HANG_BAT_DAU = "GIAO_HANG_BAT_DAU";
    private static final String EVENT_GIAO_HANG_THANH_CONG = "GIAO_HANG_THANH_CONG";
    private static final String EVENT_GIAO_HANG_THAT_BAI = "GIAO_HANG_THAT_BAI";
    private static final String EVENT_GIAO_HANG_HOAN_VE = "GIAO_HANG_HOAN_VE";
    private static final String EVENT_XAC_NHAN_DON_HANG = "XAC_NHAN_DON_HANG";
    private static final String EVENT_THANH_TOAN_KHACH_XAC_NHAN = "THANH_TOAN_KHACH_XAC_NHAN";
    private static final String EVENT_THANH_TOAN_NHAN_VIEN_XAC_NHAN = "THANH_TOAN_NHAN_VIEN_XAC_NHAN";
    private static final String EVENT_HE_THONG_HUY_DON = "HE_THONG_HUY_DON";
    private static final String EVENT_HOAN_TAT_POS = "HOAN_TAT_POS";

    // Bộ mã sự kiện checkpoint vận chuyển GHN (không đổi trạng thái chính, chỉ ghi milestone)
    private static final String EVENT_GHN_LAY_HANG       = "GHN_LAY_HANG";
    private static final String EVENT_GHN_TRUNG_CHUYEN   = "GHN_TRUNG_CHUYEN";
    private static final String EVENT_GHN_GIAO_THAT_BAI  = "GHN_GIAO_THAT_BAI";
    private static final String EVENT_GHN_DANG_HOAN_VE   = "GHN_DANG_HOAN_VE";

    private static final Set<String> GHN_CHECKPOINT_EVENTS = Set.of(
            EVENT_GHN_LAY_HANG, EVENT_GHN_TRUNG_CHUYEN,
            EVENT_GHN_GIAO_THAT_BAI, EVENT_GHN_DANG_HOAN_VE);

    // Các trạng thái quan trọng cần tự động gửi mail thông báo cho khách
    private static final Set<String> AUTO_MAIL_STATUS_CODES = Set.of(
            "CHO_LAY_HANG", "DANG_GIAO", "HOAN_THANH", "GIAO_THAT_BAI", "HOAN_VE", "HUY", "DA_HUY");

    private static final Set<String> ROLE_QUAN_TRI = Set.of("ADMIN", "EMPLOYEE", "NHAN_VIEN", "NHANVIEN");
    private static final Set<String> ROLE_HE_THONG = Set.of("SYSTEM", "HE_THONG");

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "CHO_XAC_NHAN", Set.of("CHO_LAY_HANG", "HUY"),
        "CHO_LAY_HANG", Set.of("DANG_GIAO", "HUY", "HOAN_THANH"),
            "DANG_GIAO", Set.of("HOAN_THANH", "GIAO_THAT_BAI"),
            "GIAO_THAT_BAI", Set.of("HOAN_VE")
    );

    private final HoaDonRepository hoaDonRepo;
    private final HoaDonChiTietRepository hdctRepo;
    private final SanPhamChiTietRepository spctRepo;
    private final OrderStatusHistoryRepository historyRepo;
    private final OrderStatusLookupRepository statusLookup;
    private final NhanVienRepository nhanVienRepo;
    private final KhachHangRepository khachHangRepo;
    private final HoaDonTinhTienService tinhTienService;
    private final JavaMailSender mailSender;

    @Value("${app.lookup.mail.from:}")
    private String lookupMailFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.lookup.public-url:" + DEFAULT_LOOKUP_URL + "}")
    private String lookupPublicUrl;

    public HoaDonApiController(HoaDonRepository hoaDonRepo,
                               HoaDonChiTietRepository hdctRepo,
                               SanPhamChiTietRepository spctRepo,
                               OrderStatusHistoryRepository historyRepo,
                               OrderStatusLookupRepository statusLookup,
                               NhanVienRepository nhanVienRepo,
                               KhachHangRepository khachHangRepo,
                               HoaDonTinhTienService tinhTienService,
                               JavaMailSender mailSender) {
        this.hoaDonRepo = hoaDonRepo;
        this.hdctRepo = hdctRepo;
        this.spctRepo = spctRepo;
        this.historyRepo = historyRepo;
        this.statusLookup = statusLookup;
        this.nhanVienRepo = nhanVienRepo;
        this.khachHangRepo = khachHangRepo;
        this.tinhTienService = tinhTienService;
        this.mailSender = mailSender;
    }

    // 1) LIST (tạm: trả tất cả; sau đó anh gắn searchWithStatusName như AoKhoac)
    @GetMapping
    public List<HoaDonRowDTO> list() {
        return hoaDonRepo.findAll().stream().map(hd -> {
            String code = resolveStatusCode(hd);
            String name = resolveStatusName(hd);
            HoaDonRowDTO row = HoaDonMapper.toRow(hd, code, name);
            return enrichRow(hd, row);
        }).toList();
    }

    // 2) DETAIL
    @GetMapping("/{id}")
    public HoaDonDetailDTO detail(@PathVariable Integer id) {
        HoaDon hd = hoaDonRepo.findById(id).orElseThrow();

        String statusCode = resolveStatusCode(hd);
        String statusName = resolveStatusName(hd);

        var dto = new HoaDonDetailDTO();
        dto.hoaDon = HoaDonMapper.toRow(hd, statusCode, statusName);
        dto.hoaDon = enrichRow(hd, dto.hoaDon);

        dto.items = hdctRepo.findByHoaDon_Id(id).stream()
                .map(HoaDonMapper::toItem)
                .toList();

        try {
            dto.history = historyRepo.viewByHoaDonId(id).stream().map(h -> {
                OrderStatusHistoryDTO x = new OrderStatusHistoryDTO();
                x.changedAt = h.getChangedAt();
                x.fromStatus = h.getFromStatus();
                x.toStatus = h.getToStatus();
                x.note = h.getNote();
                return x;
            }).toList();
        } catch (Exception ignored) {
            dto.history = List.of();
        }

        dto.finalOrder = isFinalByStatusCode(statusCode);
        return dto;
    }

    @GetMapping("/lookup")
    public Map<String, Object> lookupOrder(@RequestParam String maHoaDon,
                                           @RequestParam(required = false) String soDienThoai,
                                           @RequestParam(required = false) String email) {
        HoaDon hd = findOrderForLookup(maHoaDon, soDienThoai, email);
        return buildLookupResponse(hd, soDienThoai);
    }

    @PostMapping("/lookup/send-mail")
    public ResponseEntity<Map<String, Object>> sendLookupMail(@RequestBody Map<String, String> req) {
        String maHoaDon = readReqValue(req, "maHoaDon");
        String soDienThoai = readReqValue(req, "soDienThoai");
        String requestedEmail = readReqValue(req, "email");
        String trackingUrl = readReqValue(req, "trackingUrl");

        HoaDon hd = findOrderForLookup(maHoaDon, soDienThoai, "");
        Map<String, Object> payload = buildLookupResponse(hd, soDienThoai);

        String resolvedEmail = !requestedEmail.isBlank()
                ? requestedEmail
                : extractCustomerEmail(hd).orElse("");

        if (resolvedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Không tìm thấy email khách hàng. Vui lòng nhập email để gửi thông báo");
        }

        if (trackingUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Thiếu trackingUrl hợp lệ. Hệ thống không tự đoán URL tra cứu để tránh gửi nhầm link tunnel cũ.");
        }

        String finalTrackingUrl = trackingUrl;

        try {
            sendTrackingEmail(resolvedEmail, payload, finalTrackingUrl);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã gửi email tra cứu đến " + resolvedEmail,
                "email", resolvedEmail,
                "maHoaDon", maHoaDon
            ));
        } catch (ResponseStatusException ex) {
            HttpStatusCode statusCode = ex.getStatusCode();
            HttpStatus status = statusCode instanceof HttpStatus
                ? (HttpStatus) statusCode
                : HttpStatus.SERVICE_UNAVAILABLE;
            String reason = String.valueOf(ex.getReason() == null ? "" : ex.getReason()).trim();
            String message = reason.isBlank()
                ? "Gửi email tra cứu thất bại. Vui lòng thử lại sau"
                : reason;

            return ResponseEntity.status(status).body(Map.of(
                "success", false,
                "status", status.value(),
                "message", message,
                "maHoaDon", maHoaDon,
                "email", resolvedEmail
            ));
        }
    }

    // 3) CREATE
    @PostMapping
    public HoaDonDetailDTO create(@RequestBody HoaDonCreateRequest req) {
        try {
        HoaDon hd = new HoaDon();
        hd.setNgayTao(LocalDateTime.now());

        // attach NV/KH
        if (req.nhanVienId != null) hd.setNhanVien(nhanVienRepo.findById(req.nhanVienId).orElseThrow());
        if (req.khachHangId != null) {
            KhachHang kh = khachHangRepo.findById(req.khachHangId).orElseThrow();
            hd.setKhachHang(kh);
            if (kh.getTenKhachHang() != null && !kh.getTenKhachHang().isBlank()) {
                hd.setTenKhachHang(kh.getTenKhachHang().trim());
            }
        }

        hd.setSoDienThoaiNhanHang(req.soDienThoaiNhanHang);
        hd.setDiaChiNhanHang(req.diaChiNhanHang);
        hd.setNgayNhanHangDuKien(req.ngayNhanHangDuKien);
        hd.setNgayNhanHangMongMuon(req.ngayNhanHangMongMuon);

        hd.setPhiShip(req.phiShip == null ? BigDecimal.ZERO : req.phiShip);
        hd.setGiaSauGiamGia(BigDecimal.ZERO);
        hd.setThanhTien(BigDecimal.ZERO);

        // status default
        String normalizedOrderType = normalizeOrderType(req.orderType, req.statusNote, req.diaChiNhanHang);
        String resolvedStatusCode = (req.orderStatusCode == null || req.orderStatusCode.isBlank())
            ? defaultStatusByOrderType(normalizedOrderType)
            : req.orderStatusCode.trim().toUpperCase();

        validateOrderTypeStatus(normalizedOrderType, resolvedStatusCode, req.phuongThucThanhToan);
        ensureKnownStatusCode(resolvedStatusCode);

        Integer stId = statusLookup.findIdByCode(resolvedStatusCode);
        if (stId == null) stId = 1;
        hd.setOrderStatusId(stId);
        hd.setTrangThai(toStatusName(resolvedStatusCode));

        // mã hóa đơn tự sinh (demo nhanh)
        hd.setMaHoaDon(genMaHoaDon());

        if (req.phuongThucThanhToan != null) hd.setPhuongThucThanhToan(req.phuongThucThanhToan);
        if (req.paidAt != null) hd.setPaidAt(req.paidAt);
        if (req.cashCollectedAt != null) hd.setCashCollectedAt(req.cashCollectedAt);

        String normalizedPaymentMethod = String.valueOf(hd.getPhuongThucThanhToan() == null ? "" : hd.getPhuongThucThanhToan())
            .trim()
            .toUpperCase();
        stampInitialPaymentTimes(hd, normalizedOrderType, normalizedPaymentMethod);

        String normalizedStatusNote = normalizeStatusNote(req.statusNote, normalizedOrderType);
        hd.setStatusNote(normalizedStatusNote);
        if (req.giaSauGiamGia != null) hd.setGiaSauGiamGia(req.giaSauGiamGia);
        if (req.thanhTien != null) hd.setThanhTien(req.thanhTien);

        hoaDonRepo.save(hd);
        tinhTienService.recalc(hd.getId());
        return detail(hd.getId());
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Du lieu hoa don khong hop le hoac thieu cot bat buoc trong DB", ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Khong tao duoc hoa don: " + ex.getMessage(), ex);
        }
    }

    // 4) UPDATE INFO
    @PutMapping("/{id}")
    public HoaDonDetailDTO update(@PathVariable Integer id,
                                  @RequestBody HoaDonUpdateRequest req,
                                  HttpServletRequest httpRequest) {
        try {
        requireRole(httpRequest, ROLE_QUAN_TRI,
                "Ban khong co quyen cap nhat hoa don");

        HoaDon hd = hoaDonRepo.findById(id).orElseThrow();

        String code = resolveStatusCode(hd);
        if (isFinalByStatusCode(code)) return detail(id);

        Integer fromStatusId = hd.getOrderStatusId();
        String currentStatusCode = resolveStatusCode(hd);
        String currentOrderType = normalizeOrderType(
            req.orderType,
            req.statusNote != null ? req.statusNote : hd.getStatusNote(),
            req.diaChiNhanHang != null ? req.diaChiNhanHang : hd.getDiaChiNhanHang()
        );
        String effectivePaymentMethod = req.phuongThucThanhToan != null ? req.phuongThucThanhToan : hd.getPhuongThucThanhToan();
        String effectiveStatusNote = req.statusNote != null
                ? normalizeStatusNote(req.statusNote.isBlank() ? null : req.statusNote.trim(), currentOrderType)
                : normalizeStatusNote(hd.getStatusNote(), currentOrderType);

        if (req.nhanVienId != null) hd.setNhanVien(nhanVienRepo.findById(req.nhanVienId).orElseThrow());
        if (req.khachHangId != null) hd.setKhachHang(khachHangRepo.findById(req.khachHangId).orElseThrow());

        if (req.soDienThoaiNhanHang != null) hd.setSoDienThoaiNhanHang(req.soDienThoaiNhanHang);
        if (req.diaChiNhanHang != null) hd.setDiaChiNhanHang(req.diaChiNhanHang);
        if (req.ngayNhanHangDuKien != null) hd.setNgayNhanHangDuKien(req.ngayNhanHangDuKien);
        if (req.ngayNhanHangMongMuon != null) hd.setNgayNhanHangMongMuon(req.ngayNhanHangMongMuon);

        if (req.phiShip != null) hd.setPhiShip(req.phiShip);
        if (req.giaSauGiamGia != null) hd.setGiaSauGiamGia(req.giaSauGiamGia);
        if (req.thanhTien != null) hd.setThanhTien(req.thanhTien);
        if (req.phuongThucThanhToan != null) hd.setPhuongThucThanhToan(req.phuongThucThanhToan);
        if (req.paidAt != null) hd.setPaidAt(req.paidAt);
        if (req.cashCollectedAt != null) hd.setCashCollectedAt(req.cashCollectedAt);
        if (req.statusNote != null) {
            hd.setStatusNote(effectiveStatusNote);
        } else {
            hd.setStatusNote(effectiveStatusNote);
        }

        if (hasStatusChangedRequest(req, currentStatusCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Khong duoc cap nhat trang thai truc tiep. Trang thai chi doi qua su kien he thong giao hang/thanh toan.");
        }

        hoaDonRepo.save(hd);
        tinhTienService.recalc(id);

        if (fromStatusId != null && hd.getOrderStatusId() != null && !fromStatusId.equals(hd.getOrderStatusId())) {
            try {
                historyRepo.insertHistory(
                        id,
                        fromStatusId,
                        hd.getOrderStatusId(),
                        LocalDateTime.now(),
                        req.statusNote == null || req.statusNote.isBlank() ? "Cập nhật trạng thái từ giao diện quản trị" : req.statusNote.trim()
                );
            } catch (Exception ignored) {
                // Legacy database may not have history tables yet.
            }
        }

        return detail(id);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Khong cap nhat duoc hoa don do rang buoc du lieu", ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Khong cap nhat duoc hoa don: " + ex.getMessage(), ex);
        }
    }

    // 5) SU KIEN HE THONG DOI TRANG THAI
    @PostMapping("/{id}/system-events")
    public HoaDonDetailDTO handleSystemEvent(@PathVariable Integer id,
                                              @RequestBody HoaDonSystemEventRequest req,
                                              HttpServletRequest httpRequest) {
        HoaDon hd = hoaDonRepo.findById(id).orElseThrow();
        String actorRole = resolveActorRole(httpRequest);
        requireRoleForSystemEvent(actorRole, req == null ? null : req.eventCode);

        String currentCode = resolveStatusCode(hd);
        if (isFinalByStatusCode(currentCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Don hang da ket thuc, khong the nhan su kien moi");
        }

        String orderType = normalizeOrderType(null, hd.getStatusNote(), hd.getDiaChiNhanHang());
        String paymentMethod = String.valueOf(hd.getPhuongThucThanhToan() == null ? "" : hd.getPhuongThucThanhToan()).trim().toUpperCase();

        String nextCode = applySystemEvent(hd, req, currentCode, orderType, paymentMethod);
        Integer fromStatusId = hd.getOrderStatusId();

        if (!currentCode.equals(nextCode)) {
            applyStatusCodeToHoaDon(hd, nextCode);
            if ("HUY".equals(nextCode) || "DA_HUY".equals(nextCode)) {
                hd.setNgayHuy(LocalDateTime.now());
            }
        }

        hoaDonRepo.save(hd);
        tinhTienService.recalc(id);

        boolean isGhnCheckpoint = req != null && req.eventCode != null
                && GHN_CHECKPOINT_EVENTS.contains(req.eventCode.trim().toUpperCase());
        if (isGhnCheckpoint) {
            // GHN checkpoint: trạng thái chính không đổi, nhưng vẫn ghi lịch sử với note milestone
            try {
                Integer statusId = hd.getOrderStatusId();
                if (statusId != null) {
                    String ghnNote = req.note != null && !req.note.isBlank()
                            ? req.note.trim()
                            : req.eventCode + " - Cap nhat checkpoint van chuyen GHN";
                    historyRepo.insertHistory(id, statusId, statusId, LocalDateTime.now(), ghnNote);
                }
            } catch (Exception ignored) {}
        } else if (fromStatusId != null && hd.getOrderStatusId() != null
                && !fromStatusId.equals(hd.getOrderStatusId())) {
            try {
                historyRepo.insertHistory(
                        id,
                        fromStatusId,
                        hd.getOrderStatusId(),
                        LocalDateTime.now(),
                        req != null && req.note != null && !req.note.isBlank()
                                ? req.note.trim()
                                : "Cap nhat trang thai tu su kien he thong"
                );
            } catch (Exception ignored) {
                // Legacy database may not have history tables yet.
            }
        }

        // Tự động gửi mail thông báo cho khách khi đạt mốc trạng thái quan trọng.
        // Bắt buộc trackingUrl từ frontend để tránh gửi nhầm link tunnel cũ.
        if (!currentCode.equals(nextCode) && AUTO_MAIL_STATUS_CODES.contains(nextCode)) {
            String trackingUrlOverride = req != null ? req.trackingUrl : "";
            if (trackingUrlOverride == null || trackingUrlOverride.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Thiếu trackingUrl cho sự kiện cập nhật trạng thái. Vui lòng gửi lại request kèm trackingUrl.");
            }
            sendMilestoneEmailAsync(hd, trackingUrlOverride);
        }

        return detail(id);
    }

    // 6) ADD ITEM (by spctId)
    @PostMapping("/{id}/items")
    public HoaDonDetailDTO addItem(@PathVariable Integer id, @RequestBody HoaDonAddItemRequest req) {
        HoaDon hd = hoaDonRepo.findById(id).orElseThrow();
        String code = resolveStatusCode(hd);
        if (isFinalByStatusCode(code)) return detail(id);

        if (req.spctId == null || req.soLuong == null || req.soLuong <= 0) return detail(id);

        SanPhamChiTiet spct = spctRepo.findById(req.spctId).orElseThrow();

        HoaDonChiTiet line = hdctRepo.findByHoaDon_IdAndSanPhamChiTiet_Id(id, req.spctId).orElse(null);
        int newQty = req.soLuong;

        if (line != null) {
            newQty = (line.getSoLuong() == null ? 0 : line.getSoLuong()) + req.soLuong;
            validateStock(spct, newQty);
            line.setSoLuong(newQty);
        } else {
            validateStock(spct, newQty);
            line = new HoaDonChiTiet();
            line.setHoaDon(hd);
            line.setSanPhamChiTiet(spct);
            line.setSoLuong(newQty);
            line.setTrangThai("ACTIVE");
        }

        BigDecimal giaBan = spct.getGiaBan() == null ? BigDecimal.ZERO : spct.getGiaBan();
        line.setThanhTien(giaBan.multiply(BigDecimal.valueOf(newQty)));

        hdctRepo.save(line);
        tinhTienService.recalc(id);

        return detail(id);
    }

    // 6) UPDATE QTY
    @PutMapping("/{id}/items/{itemId}")
    public HoaDonDetailDTO updateQty(@PathVariable Integer id,
                                     @PathVariable Integer itemId,
                                     @RequestBody HoaDonUpdateQtyRequest req) {

        HoaDon hd = hoaDonRepo.findById(id).orElseThrow();
        String code = resolveStatusCode(hd);
        if (isFinalByStatusCode(code)) return detail(id);

        HoaDonChiTiet line = hdctRepo.findById(itemId).orElseThrow();
        if (req.soLuong == null || req.soLuong <= 0) {
            hdctRepo.delete(line);
            tinhTienService.recalc(id);
            return detail(id);
        }

        validateStock(line.getSanPhamChiTiet(), req.soLuong);
        line.setSoLuong(req.soLuong);

        BigDecimal giaBan = line.getSanPhamChiTiet().getGiaBan() == null
                ? BigDecimal.ZERO : line.getSanPhamChiTiet().getGiaBan();

        line.setThanhTien(giaBan.multiply(BigDecimal.valueOf(req.soLuong)));
        hdctRepo.save(line);

        tinhTienService.recalc(id);
        return detail(id);
    }

    // 7) DELETE ITEM
    @DeleteMapping("/{id}/items/{itemId}")
    public HoaDonDetailDTO deleteItem(@PathVariable Integer id, @PathVariable Integer itemId) {
        HoaDon hd = hoaDonRepo.findById(id).orElseThrow();
        String code = resolveStatusCode(hd);
        if (isFinalByStatusCode(code)) return detail(id);

        hdctRepo.deleteById(itemId);
        tinhTienService.recalc(id);
        return detail(id);
    }

    // 9) CANCEL (giu endpoint de tuong thich, nhung chi he thong duoc huy)
    @PostMapping("/{id}/cancel")
    public HoaDonDetailDTO cancel(@PathVariable Integer id,
                                  @RequestParam(required = false) String reason,
                                  HttpServletRequest httpRequest) {
        HoaDonSystemEventRequest req = new HoaDonSystemEventRequest();
        req.eventCode = EVENT_HE_THONG_HUY_DON;
        req.note = reason;
        return handleSystemEvent(id, req, httpRequest);
    }

    // ===== helpers =====
    private boolean isFinalByStatusCode(String code) {
        if (code == null) return false;
        return code.equals("HOAN_THANH")
                || code.equals("HUY")
                || code.equals("DA_HUY")
                || code.equals("HOAN_VE")
                || code.equals("DA_GIAO");
    }

    private String resolveStatusCode(HoaDon hd) {
        String fromLookup = statusLookup.findCodeById(hd.getOrderStatusId());
        if (fromLookup != null && !fromLookup.isBlank()) {
            return fromLookup;
        }

        String raw = hd.getTrangThai();
        if (raw == null || raw.isBlank()) return "CHO_XAC_NHAN";

        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase();

        String collapsed = normalized
            .replace(' ', '_')
            .replaceAll("[^A-Z_]", "");

        if (normalized.contains("CHO XAC NHAN") || normalized.contains("CHO_XAC_NHAN")) return "CHO_XAC_NHAN";
        if (normalized.contains("CHO XU LY") || normalized.contains("CHO_XU_LY") || normalized.contains("CHO LAY HANG") || normalized.contains("CHO_LAY_HANG")) return "CHO_LAY_HANG";
        if (normalized.contains("DANG GIAO") || normalized.contains("DANG_GIAO")) return "DANG_GIAO";
        if (normalized.contains("DA GIAO") || normalized.contains("DA_GIAO")) return "DA_GIAO";
        if (normalized.contains("DA HUY") || normalized.contains("DA_HUY") || normalized.contains("HUY")) return "HUY";
        if (normalized.contains("GIAO THAT BAI") || normalized.contains("GIAO_THAT_BAI")) return "GIAO_THAT_BAI";
        if (normalized.contains("HOAN VE") || normalized.contains("HOAN_VE")) return "HOAN_VE";
        if (normalized.contains("HOAN THANH") || normalized.contains("HOAN_THANH")) return "HOAN_THANH";

        if (collapsed.contains("CHOXACNHAN") || collapsed.contains("CHO_XAC_NHAN")) return "CHO_XAC_NHAN";
        if (collapsed.contains("CHOLAYHANG") || collapsed.contains("CHO_XU_LY") || collapsed.contains("CHOLYHANG")) return "CHO_LAY_HANG";
        if (collapsed.contains("ANG_GIAO") || collapsed.contains("DANG_GIAO")) return "DANG_GIAO";
        if (collapsed.contains("A_GIAO") || collapsed.contains("DA_GIAO")) return "DA_GIAO";
        if (collapsed.contains("GIAOTHATBAI") || collapsed.contains("GIAO_THAT_BAI") || collapsed.contains("THATBAI")) return "GIAO_THAT_BAI";
        if (collapsed.contains("HOANVE") || collapsed.contains("HOAN_VE")) return "HOAN_VE";
        if (collapsed.contains("HOANTHANH") || collapsed.contains("HOAN_THANH")) return "HOAN_THANH";
        if (collapsed.contains("HUY") || collapsed.contains("DA_HUY")) return "HUY";

        return raw.trim().toUpperCase().replace(' ', '_');
    }

    private String resolveStatusName(HoaDon hd) {
        String fromLookup = statusLookup.findNameById(hd.getOrderStatusId());
        if (fromLookup != null && !fromLookup.isBlank()) {
            return fromLookup;
        }
        return toStatusName(resolveStatusCode(hd));
    }

    private String toStatusName(String code) {
        if (code == null || code.isBlank()) return "Chờ xác nhận";

        return switch (code.trim().toUpperCase()) {
            case "CHO_XAC_NHAN" -> "Chờ xác nhận";
            case "CHO_XU_LY", "CHO_LAY_HANG" -> "Chờ xử lý";
            case "DANG_GIAO" -> "Đang giao";
            case "DA_GIAO" -> "Đã giao";
            case "HUY", "DA_HUY" -> "Đã hủy";
            case "GIAO_THAT_BAI" -> "Giao thất bại";
            case "HOAN_VE" -> "Hoàn về";
            case "HOAN_THANH" -> "Hoàn thành";
            default -> code;
        };
    }

    private String genMaHoaDon() {
        String ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(LocalDateTime.now());
        int rnd = (int)(Math.random() * 900) + 100;
        return "HD" + ts + rnd;
    }

    private String normalizeOrderType(String orderType, String statusNote, String diaChiNhanHang) {
        String fromReq = String.valueOf(orderType == null ? "" : orderType).trim().toUpperCase();
        if (ORDER_TYPE_POS.equals(fromReq)) return ORDER_TYPE_POS;
        if (ORDER_TYPE_ONLINE.equals(fromReq)) return ORDER_TYPE_ONLINE;

        String note = String.valueOf(statusNote == null ? "" : statusNote).toUpperCase();
        if (note.contains("[POS]")) return ORDER_TYPE_POS;

        String address = String.valueOf(diaChiNhanHang == null ? "" : diaChiNhanHang)
                .trim()
                .toLowerCase();
        if (address.contains("mua tại quầy") || address.contains("mua tai quay")) {
            return ORDER_TYPE_POS;
        }

        return ORDER_TYPE_ONLINE;
    }

    private String normalizeStatusNote(String statusNote, String orderType) {
        String tag = ORDER_TYPE_POS.equals(orderType) ? "[POS]" : "[ONLINE]";
        String raw = String.valueOf(statusNote == null ? "" : statusNote).trim();
        if (raw.isEmpty()) return tag;
        if (raw.toUpperCase().startsWith("[POS]") || raw.toUpperCase().startsWith("[ONLINE]")) {
            return raw;
        }
        return tag + " " + raw;
    }

    private String defaultStatusByOrderType(String orderType) {
        return ORDER_TYPE_POS.equals(orderType) ? "CHO_LAY_HANG" : "CHO_XAC_NHAN";
    }

    private HoaDonRowDTO enrichRow(HoaDon hd, HoaDonRowDTO row) {
        row.orderType = normalizeOrderType(row.orderType, hd.getStatusNote(), hd.getDiaChiNhanHang());
        row.statusNote = normalizeStatusNote(hd.getStatusNote(), row.orderType);

        FulfillmentState fulfillmentState = resolveFulfillmentState(row.orderStatusCode);
        row.fulfillmentStatusCode = fulfillmentState.code();
        row.fulfillmentStatusName = fulfillmentState.label();
        row.businessClosureStatus = resolveBusinessClosureStatus(row.orderStatusCode);
        row.businessClosureStatusName = resolveBusinessClosureStatusName(row.businessClosureStatus);

        PaymentFlowState paymentFlowState = resolvePaymentFlowState(
                hd.getPhuongThucThanhToan(),
                row.statusNote,
                row.orderType
        );
        row.paymentFlowCode = paymentFlowState.code();
        row.paymentFlowLabel = paymentFlowState.label();
        row.paymentFlowTone = paymentFlowState.tone();
        return row;
    }

    private String resolveActorRole(HttpServletRequest httpRequest) {
        String role = String.valueOf(httpRequest.getHeader("X-User-Role") == null
                ? ""
                : httpRequest.getHeader("X-User-Role"))
                .trim()
                .toUpperCase();
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }
        return role;
    }

    private void requireRole(HttpServletRequest httpRequest, Set<String> allowedRoles, String message) {
        String role = resolveActorRole(httpRequest);
        if (!allowedRoles.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private void requireRoleForSystemEvent(String role, String eventCode) {
        String normalizedEvent = String.valueOf(eventCode == null ? "" : eventCode).trim().toUpperCase();
        if (normalizedEvent.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thieu ma su kien he thong");
        }

        if (EVENT_THANH_TOAN_KHACH_XAC_NHAN.equals(normalizedEvent)) {
            if (!("CUSTOMER".equals(role) || "KHACH_HANG".equals(role) || "KHACHHANG".equals(role) || ROLE_HE_THONG.contains(role))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Chi khach hang hoac he thong duoc gui su kien xac nhan thanh toan");
            }
            return;
        }

        if (EVENT_THANH_TOAN_NHAN_VIEN_XAC_NHAN.equals(normalizedEvent)) {
            if (!ROLE_QUAN_TRI.contains(role) && !ROLE_HE_THONG.contains(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Chi nhan vien hoac he thong duoc xac nhan thanh toan");
            }
            return;
        }

        if (EVENT_HOAN_TAT_POS.equals(normalizedEvent)) {
            if (!ROLE_QUAN_TRI.contains(role) && !ROLE_HE_THONG.contains(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Chi nhan vien hoac admin duoc hoan tat ban hang POS");
            }
            return;
        }

        if (EVENT_XAC_NHAN_DON_HANG.equals(normalizedEvent)) {
            if (!ROLE_QUAN_TRI.contains(role) && !ROLE_HE_THONG.contains(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Chi nhan vien hoac admin duoc xac nhan don hang");
            }
            return;
        }

        if (EVENT_GIAO_HANG_BAT_DAU.equals(normalizedEvent)
                || EVENT_GIAO_HANG_THANH_CONG.equals(normalizedEvent)
                || EVENT_GIAO_HANG_THAT_BAI.equals(normalizedEvent)
                || EVENT_GIAO_HANG_HOAN_VE.equals(normalizedEvent)
                || EVENT_HE_THONG_HUY_DON.equals(normalizedEvent)) {
            if (!ROLE_QUAN_TRI.contains(role) && !ROLE_HE_THONG.contains(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Chi nhan vien, admin hoac he thong duoc cap nhat giao hang");
            }
            return;
        }

        if (GHN_CHECKPOINT_EVENTS.contains(normalizedEvent)) {
            if (!ROLE_QUAN_TRI.contains(role) && !ROLE_HE_THONG.contains(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Chi nhan vien, admin hoac he thong duoc cap nhat checkpoint GHN");
            }
            return;
        }

        if (!ROLE_HE_THONG.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Chi he thong duoc phep gui su kien giao hang/trang thai");
        }
    }

    private boolean hasStatusChangedRequest(HoaDonUpdateRequest req, String currentStatusCode) {
        if (req.orderStatusCode == null || req.orderStatusCode.isBlank()) return false;
        String nextCode = String.valueOf(req.orderStatusCode).trim().toUpperCase();
        String currentCode = String.valueOf(currentStatusCode == null ? "" : currentStatusCode).trim().toUpperCase();
        return !nextCode.equals(currentCode);
    }

    private void applyStatusCodeToHoaDon(HoaDon hd, String nextCode) {
        ensureKnownStatusCode(nextCode);
        Integer nextStatusId = statusLookup.findIdByCode(nextCode);

        if (nextStatusId == null && "DA_GIAO".equals(nextCode)) {
            nextStatusId = statusLookup.findIdByCode("HOAN_THANH");
        }

        if (nextStatusId != null) {
            hd.setOrderStatusId(nextStatusId);
        }

        hd.setTrangThai(toStatusName(nextCode));
    }

    private String applySystemEvent(HoaDon hd,
                                    HoaDonSystemEventRequest req,
                                    String currentCode,
                                    String orderType,
                                    String paymentMethod) {
        if (req == null || req.eventCode == null || req.eventCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thieu ma su kien he thong");
        }

        String eventCode = req.eventCode.trim().toUpperCase();
        String currentStatus = String.valueOf(currentCode == null ? "" : currentCode).trim().toUpperCase();
        String normalizedOrderType = normalizeOrderType(orderType, hd.getStatusNote(), hd.getDiaChiNhanHang());
        String normalizedPaymentMethod = String.valueOf(paymentMethod == null ? "" : paymentMethod).trim().toUpperCase();
        String statusNote = hd.getStatusNote();

        switch (eventCode) {
            case EVENT_XAC_NHAN_DON_HANG -> {
                validateSystemEventTransition(eventCode, currentStatus, Set.of("CHO_XAC_NHAN"));
                return "CHO_LAY_HANG";
            }
            case EVENT_THANH_TOAN_KHACH_XAC_NHAN -> {
                if (!normalizedPaymentMethod.contains("VNPAY")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Su kien thanh toan VNPay chi ap dung cho don VNPay");
                }
                hd.setStatusNote(appendStatusTag(statusNote, PAYMENT_FLOW_CUSTOMER_CONFIRMED,
                        req.note == null || req.note.isBlank() ? "Khach hang da xac nhan thanh toan" : req.note.trim()));
                return currentStatus;
            }
            case EVENT_THANH_TOAN_NHAN_VIEN_XAC_NHAN -> {
                if (!normalizedPaymentMethod.contains("VNPAY")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Su kien xac nhan thanh toan chi ap dung cho don VNPay");
                }
                hd.setStatusNote(appendStatusTag(statusNote, PAYMENT_FLOW_EMPLOYEE_CONFIRMED,
                        req.note == null || req.note.isBlank() ? "Nhan vien da xac nhan thanh toan" : req.note.trim()));
                if (hd.getPaidAt() == null) {
                    hd.setPaidAt(LocalDateTime.now());
                }
                if ("CHO_XAC_NHAN".equals(currentStatus)) return "CHO_LAY_HANG";
                if ("DA_GIAO".equals(currentStatus)) return "HOAN_THANH";
                return currentStatus;
            }
            case EVENT_GIAO_HANG_BAT_DAU -> {
                validateSystemEventTransition(eventCode, currentStatus, Set.of("CHO_LAY_HANG"));
                if (ORDER_TYPE_POS.equals(normalizedOrderType)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Don tai quay khong ap dung su kien giao hang");
                }
                return "DANG_GIAO";
            }
            case EVENT_GIAO_HANG_THANH_CONG -> {
                validateSystemEventTransition(eventCode, currentStatus, Set.of("DANG_GIAO"));
                if (ORDER_TYPE_POS.equals(normalizedOrderType)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Don tai quay khong ap dung su kien giao hang");
                }
                if (PAYMENT_METHOD_COD.equals(normalizedPaymentMethod)) {
                    LocalDateTime now = LocalDateTime.now();
                    if (hd.getCashCollectedAt() == null) {
                        hd.setCashCollectedAt(now);
                    }
                    if (hd.getPaidAt() == null) {
                        hd.setPaidAt(now);
                    }
                }
                return "HOAN_THANH";
            }
            case EVENT_GIAO_HANG_THAT_BAI -> {
                validateSystemEventTransition(eventCode, currentStatus, Set.of("DANG_GIAO"));
                if (ORDER_TYPE_POS.equals(normalizedOrderType)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Don tai quay khong ap dung su kien giao hang");
                }
                return "GIAO_THAT_BAI";
            }
            case EVENT_GIAO_HANG_HOAN_VE -> {
                validateSystemEventTransition(eventCode, currentStatus, Set.of("GIAO_THAT_BAI"));
                if (ORDER_TYPE_POS.equals(normalizedOrderType)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Don tai quay khong ap dung su kien giao hang");
                }
                return "HOAN_VE";
            }
            case EVENT_HE_THONG_HUY_DON -> {
                validateSystemEventTransition(eventCode, currentStatus, Set.of("CHO_XAC_NHAN", "CHO_LAY_HANG"));
                return "HUY";
            }
            case EVENT_HOAN_TAT_POS -> {
                validateSystemEventTransition(eventCode, currentStatus, Set.of("CHO_LAY_HANG"));
                LocalDateTime now = LocalDateTime.now();
                if (PAYMENT_METHOD_CASH.equals(normalizedPaymentMethod) && hd.getCashCollectedAt() == null) {
                    hd.setCashCollectedAt(now);
                }
                if (hd.getPaidAt() == null) {
                    hd.setPaidAt(now);
                }
                return "HOAN_THANH";
            }
            // ---- GHN Checkpoint events (không đổi trạng thái chính, ghi milestone vào note) ----
            case EVENT_GHN_LAY_HANG -> {
                validateSystemEventTransition(eventCode, currentStatus,
                        Set.of("CHO_LAY_HANG", "DANG_GIAO"));
                hd.setStatusNote(appendStatusTag(hd.getStatusNote(), "[GHN:PICKED_UP]",
                        req.note == null || req.note.isBlank()
                                ? "Shipper da lay hang tu shop va dang tren duong van chuyen"
                                : req.note.trim()));
                return currentStatus;
            }
            case EVENT_GHN_TRUNG_CHUYEN -> {
                validateSystemEventTransition(eventCode, currentStatus,
                        Set.of("CHO_LAY_HANG", "DANG_GIAO"));
                hd.setStatusNote(appendStatusTag(hd.getStatusNote(), "[GHN:IN_TRANSIT]",
                        req.note == null || req.note.isBlank()
                                ? "Hang dang o trung tam trung chuyen / kho chia hang"
                                : req.note.trim()));
                return currentStatus;
            }
            case EVENT_GHN_GIAO_THAT_BAI -> {
                validateSystemEventTransition(eventCode, currentStatus,
                        Set.of("DANG_GIAO", "GIAO_THAT_BAI"));
                hd.setStatusNote(appendStatusTag(hd.getStatusNote(), "[GHN:DELIVERY_FAILED]",
                        req.note == null || req.note.isBlank()
                                ? "Giao hang khong thanh cong, shipper se thu lai hoac hoan hang"
                                : req.note.trim()));
                return currentStatus;
            }
            case EVENT_GHN_DANG_HOAN_VE -> {
                validateSystemEventTransition(eventCode, currentStatus,
                        Set.of("GIAO_THAT_BAI", "HOAN_VE"));
                hd.setStatusNote(appendStatusTag(hd.getStatusNote(), "[GHN:RETURNING]",
                        req.note == null || req.note.isBlank()
                                ? "Shipper dang tren duong hoan hang ve shop"
                                : req.note.trim()));
                return currentStatus;
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ma su kien he thong khong hop le: " + eventCode);
        }
    }

    private void validateSystemEventTransition(String eventCode, String currentStatus, Set<String> allowedFrom) {
        if (!allowedFrom.contains(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Su kien " + eventCode + " khong hop le tu trang thai " + currentStatus);
        }
    }

    private String appendStatusTag(String currentStatusNote, String tag, String message) {
        String current = String.valueOf(currentStatusNote == null ? "" : currentStatusNote).trim();
        String normalizedTag = String.valueOf(tag == null ? "" : tag).trim();
        String noteMessage = String.valueOf(message == null ? "" : message).trim();

        if (current.isBlank()) {
            return normalizedTag + (noteMessage.isBlank() ? "" : (" " + noteMessage));
        }

        if (current.toUpperCase().contains(normalizedTag.toUpperCase())) {
            return current;
        }

        if (noteMessage.isBlank()) {
            return current + " | " + normalizedTag;
        }

        return current + " | " + normalizedTag + " " + noteMessage;
    }

    private void validateOrderTypeStatus(String orderType, String statusCode, String paymentMethod) {
        String normalizedStatus = String.valueOf(statusCode == null ? "" : statusCode).trim().toUpperCase();
        if (ORDER_TYPE_POS.equals(orderType) && "CHO_XAC_NHAN".equals(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Don tai quay khong duoc o trang thai Cho xac nhan");
        }

        String pttt = String.valueOf(paymentMethod == null ? "" : paymentMethod).trim().toUpperCase();
        if (ORDER_TYPE_POS.equals(orderType) && pttt.contains("VNPAY") && "CHO_XAC_NHAN".equals(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Don tai quay thanh toan online phai o trang thai Cho lay hang hoac cao hon");
        }
    }

    private void validateTransition(String currentCode, String nextCode, String orderType, String paymentMethod, String statusNote) {
        String from = String.valueOf(currentCode == null ? "" : currentCode).trim().toUpperCase();
        String to = String.valueOf(nextCode == null ? "" : nextCode).trim().toUpperCase();
        String normalizedOrderType = normalizeOrderType(orderType, statusNote, null);
        String normalizedPaymentMethod = String.valueOf(paymentMethod == null ? "" : paymentMethod).trim().toUpperCase();

        validateOrderTypeStatus(normalizedOrderType, to, normalizedPaymentMethod);
        ensureKnownStatusCode(to);

        if (from.equals(to)) return;
        if (isFinalByStatusCode(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Khong the chuyen trang thai tu don da ket thuc");
        }

        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chuyen trang thai khong hop le: " + from + " -> " + to);
        }

        if (ORDER_TYPE_POS.equals(normalizedOrderType) && "DANG_GIAO".equals(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Don tai quay khong the chuyen sang Dang giao");
        }

        if (ORDER_TYPE_ONLINE.equals(normalizedOrderType) && "HOAN_THANH".equals(to) && !"DANG_GIAO".equals(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Don online chi co the Hoan thanh sau khi Dang giao");
        }

        if ("VNPAY".equals(normalizedPaymentMethod)
                && "HOAN_THANH".equals(to)
                && !hasPaymentFlowTag(statusNote, PAYMENT_FLOW_EMPLOYEE_CONFIRMED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Don VNPAY can nhan vien xac nhan thanh toan truoc khi Hoan thanh");
        }
    }

    private void ensureKnownStatusCode(String statusCode) {
        if (statusLookup.findIdByCode(statusCode) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ma trang thai don hang khong hop le: " + statusCode);
        }
    }

    private void validateStock(SanPhamChiTiet spct, int requestedQty) {
        int available = spct.getSoLuong() == null ? 0 : spct.getSoLuong();
        if (requestedQty > available) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "So luong vuot ton kho cho bien the " +
                            String.valueOf(spct.getMa() == null ? "SPCT" : spct.getMa()) +
                            " (ton kho: " + available + ")");
        }
    }

    private PaymentFlowState resolvePaymentFlowState(String paymentMethod, String statusNote, String orderType) {
        String normalizedPayment = String.valueOf(paymentMethod == null ? "" : paymentMethod).trim().toUpperCase();
        String normalizedNote = String.valueOf(statusNote == null ? "" : statusNote).trim();

        if (!normalizedPayment.contains("VNPAY") && !normalizedNote.toUpperCase().contains("VNPAY")) {
            return new PaymentFlowState("NOT_APPLICABLE", "Khong ap dung", "neutral");
        }

        if (hasPaymentFlowTag(normalizedNote, PAYMENT_FLOW_EMPLOYEE_CONFIRMED)) {
            return new PaymentFlowState("EMPLOYEE_CONFIRMED", "Thanh toan da duoc xac nhan", "success");
        }

        if (hasPaymentFlowTag(normalizedNote, PAYMENT_FLOW_CUSTOMER_CONFIRMED)) {
            return new PaymentFlowState("WAIT_EMPLOYEE", "Cho nhan vien kiem tra giao dich", "warning");
        }

        String label = ORDER_TYPE_POS.equals(orderType)
                ? "Cho khach xac nhan thanh toan tai quay"
                : "Cho khach xac nhan thanh toan";
        return new PaymentFlowState("WAIT_CUSTOMER", label, "neutral");
    }

    private boolean hasPaymentFlowTag(String statusNote, String tag) {
        String normalizedNote = String.valueOf(statusNote == null ? "" : statusNote).toUpperCase();
        return normalizedNote.contains(String.valueOf(tag).toUpperCase());
    }

    private FulfillmentState resolveFulfillmentState(String orderStatusCode) {
        String code = String.valueOf(orderStatusCode == null ? "" : orderStatusCode).trim().toUpperCase();
        return switch (code) {
            case "CHO_XAC_NHAN" -> new FulfillmentState("PENDING", "Chờ xác nhận");
            case "CHO_LAY_HANG", "CHO_XU_LY" -> new FulfillmentState("PACKED", "Chờ lấy hàng");
            case "DANG_GIAO" -> new FulfillmentState("SHIPPING", "Đang giao");
            case "DA_GIAO" -> new FulfillmentState("DELIVERED", "Đã giao");
            case "HOAN_THANH" -> new FulfillmentState("DELIVERED", "Hoàn thành");
            case "GIAO_THAT_BAI" -> new FulfillmentState("FAILED", "Giao thất bại");
            case "HOAN_VE" -> new FulfillmentState("RETURNED", "Hoàn về");
            case "HUY", "DA_HUY" -> new FulfillmentState("FAILED", "Đã hủy");
            default -> new FulfillmentState("PENDING", "Chờ xác nhận");
        };
    }

    private String resolveBusinessClosureStatus(String orderStatusCode) {
        String code = String.valueOf(orderStatusCode == null ? "" : orderStatusCode).trim().toUpperCase();
        return switch (code) {
            case "HOAN_THANH", "HUY", "DA_HUY", "HOAN_VE" -> "CLOSED";
            default -> "OPEN";
        };
    }

    private String resolveBusinessClosureStatusName(String businessClosureStatus) {
        String code = String.valueOf(businessClosureStatus == null ? "" : businessClosureStatus).trim().toUpperCase();
        return "CLOSED".equals(code) ? "Da chot" : "Dang mo";
    }

    private void stampInitialPaymentTimes(HoaDon hd, String orderType, String paymentMethod) {
        if (hd == null) return;

        String normalizedOrderType = String.valueOf(orderType == null ? "" : orderType).trim().toUpperCase();
        String normalizedPayment = String.valueOf(paymentMethod == null ? "" : paymentMethod).trim().toUpperCase();

        if (!ORDER_TYPE_POS.equals(normalizedOrderType)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        if ((PAYMENT_METHOD_CASH.equals(normalizedPayment) || "BANK".equals(normalizedPayment)) && hd.getPaidAt() == null) {
            hd.setPaidAt(now);
        }

        if (PAYMENT_METHOD_CASH.equals(normalizedPayment) && hd.getCashCollectedAt() == null) {
            hd.setCashCollectedAt(now);
        }
    }

    private HoaDon findOrderForLookup(String maHoaDon, String soDienThoai, String email) {
        String orderCode = String.valueOf(maHoaDon == null ? "" : maHoaDon).trim();
        String phone = String.valueOf(soDienThoai == null ? "" : soDienThoai).trim();
        String requestedEmail = String.valueOf(email == null ? "" : email).trim();

        if (orderCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Vui long nhap ma don hang");
        }

        HoaDon hd = hoaDonRepo
                .findFirstByMaHoaDonIgnoreCase(orderCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Khong tim thay don hang phu hop"));

        if (!phone.isBlank()) {
            String orderPhone = String.valueOf(hd.getSoDienThoaiNhanHang() == null ? "" : hd.getSoDienThoaiNhanHang()).trim();
            if (!orderPhone.equals(phone)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Khong tim thay don hang phu hop");
            }
        }

        if (!requestedEmail.isBlank()) {
            String orderEmail = extractCustomerEmail(hd).orElse("").trim();
            if (orderEmail.isBlank() || !orderEmail.equalsIgnoreCase(requestedEmail)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Khong tim thay don hang phu hop");
            }
        }

        return hd;
    }

    private String readReqValue(Map<String, String> req, String key) {
        if (req == null || key == null || key.isBlank()) return "";
        String value = req.get(key);
        return value == null ? "" : value.trim();
    }

    private Map<String, Object> buildLookupResponse(HoaDon hd, String soDienThoai) {
        String statusCode = resolveStatusCode(hd);
        String statusName = resolveStatusName(hd);

        List<HoaDonChiTiet> lines = hdctRepo.findByHoaDon_Id(hd.getId());
        BigDecimal subtotal = lines.stream()
                .map(item -> item.getThanhTien() == null ? BigDecimal.ZERO : item.getThanhTien())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shipping = hd.getPhiShip() == null ? BigDecimal.ZERO : hd.getPhiShip();
        BigDecimal grandTotal = hd.getThanhTien() == null
                ? subtotal.add(shipping)
                : hd.getThanhTien();
        BigDecimal discount = subtotal.add(shipping).subtract(grandTotal);
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }

        List<Map<String, Object>> items = lines.stream().map(item -> {
            SanPhamChiTiet spct = item.getSanPhamChiTiet();
            String tenSanPham = "San pham";
            String maSanPham = "";
            String maSanPhamChiTiet = "";

            if (spct != null) {
                maSanPhamChiTiet = String.valueOf(spct.getMa() == null ? "" : spct.getMa());
                if (spct.getSanPham() != null) {
                    tenSanPham = String.valueOf(spct.getSanPham().getTenSanPham() == null ? "San pham" : spct.getSanPham().getTenSanPham());
                    maSanPham = String.valueOf(spct.getSanPham().getMaSanPham() == null ? "" : spct.getSanPham().getMaSanPham());
                }
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("spctId", spct == null ? null : spct.getId());
            row.put("maSanPham", maSanPham);
            row.put("maSanPhamChiTiet", maSanPhamChiTiet);
            row.put("tenSanPham", tenSanPham);
            row.put("soLuong", item.getSoLuong());
            row.put("giaBan", (spct == null || spct.getGiaBan() == null) ? BigDecimal.ZERO : spct.getGiaBan());
            row.put("thanhTien", item.getThanhTien() == null ? BigDecimal.ZERO : item.getThanhTien());
            return row;
        }).toList();

        List<Map<String, Object>> timeline = buildTrackingTimeline(hd, statusCode);
        int progressPercent = resolveProgressPercent(timeline);

        String customerName = "";
        String phone = String.valueOf(soDienThoai == null ? "" : soDienThoai).trim();
        if (hd.getTenKhachHang() != null && !hd.getTenKhachHang().isBlank()) {
            customerName = hd.getTenKhachHang();
        } else if (hd.getKhachHang() != null && hd.getKhachHang().getTenKhachHang() != null) {
            customerName = hd.getKhachHang().getTenKhachHang();
        }
        if (phone.isBlank()) {
            phone = String.valueOf(hd.getSoDienThoaiNhanHang() == null ? "" : hd.getSoDienThoaiNhanHang());
        }

        Map<String, Object> orderMap = new LinkedHashMap<>();
        orderMap.put("id", hd.getId());
        orderMap.put("maHoaDon", hd.getMaHoaDon());
        orderMap.put("orderStatusCode", statusCode);
        orderMap.put("orderStatusName", statusName);
        orderMap.put("ngayTao", hd.getNgayTao());
        orderMap.put("statusNote", hd.getStatusNote());

        Map<String, Object> customerMap = new LinkedHashMap<>();
        customerMap.put("tenKhachHang", customerName);
        customerMap.put("soDienThoai", phone);
        customerMap.put("diaChiNhanHang", hd.getDiaChiNhanHang());
        customerMap.put("email", extractCustomerEmail(hd).orElse(""));

        Map<String, Object> totalsMap = new LinkedHashMap<>();
        totalsMap.put("subtotal", subtotal);
        totalsMap.put("shippingFee", shipping);
        totalsMap.put("discount", discount);
        totalsMap.put("grandTotal", grandTotal);

        Map<String, Object> trackingMap = new LinkedHashMap<>();
        trackingMap.put("progressPercent", progressPercent);
        trackingMap.put("timeline", timeline);
        trackingMap.put("terminalStatus", isFinalByStatusCode(statusCode) ? statusName : "");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("order", orderMap);
        response.put("customer", customerMap);
        response.put("items", items);
        response.put("totals", totalsMap);
        response.put("tracking", trackingMap);
        return response;
    }

    private List<Map<String, Object>> buildTrackingTimeline(HoaDon hd, String currentCode) {
        record StepDef(String code, String label) {}

        List<StepDef> defs = List.of(
                new StepDef("CREATED", "Đơn mới tạo"),
                new StepDef("CONFIRMED", "Shop đã xác nhận"),
                new StepDef("PICKED_UP", "Đã lấy hàng"),
                new StepDef("IN_TRANSIT", "Đang trung chuyển"),
                new StepDef("OUT_FOR_DELIVERY", "Đang giao hàng"),
                new StepDef("DELIVERED", "Giao hàng thành công"),
                new StepDef("DELIVERY_FAILED", "Giao hàng không thành công"),
                new StepDef("RETURNING", "Đang hoàn hàng"),
                new StepDef("RETURNED", "Hoàn trả thành công"),
                new StepDef("CANCELED", "Đã hủy")
        );

        Map<String, LocalDateTime> touchedAt = new LinkedHashMap<>();
        touchedAt.put("CREATED", hd.getNgayTao());

        List<OrderStatusHistoryView> historyRows;
        try {
            historyRows = historyRepo.viewByHoaDonId(hd.getId());
        } catch (Exception ignored) {
            historyRows = List.of();
        }

        List<OrderStatusHistoryView> ascRows = new ArrayList<>(historyRows);
        Collections.reverse(ascRows);
        for (OrderStatusHistoryView row : ascRows) {
            String mappedFromStatus = mapHistoryStatusToTrackingCode(row.getToStatus());
            if (!mappedFromStatus.isBlank()) {
                touchedAt.put(mappedFromStatus, row.getChangedAt());
            }

            String note = normalizeTrackingText(row.getNote());
            if (note.contains("LAY HANG") || note.contains("PICK")) {
                touchedAt.putIfAbsent("PICKED_UP", row.getChangedAt());
            }
            if (note.contains("TRUNG CHUYEN") || note.contains("TRANSIT") || note.contains("KHO CHIA") || note.contains("KHO VAN CHUYEN")) {
                touchedAt.putIfAbsent("IN_TRANSIT", row.getChangedAt());
            }
            if (note.contains("DANG GIAO") || note.contains("OUT FOR DELIVERY")) {
                touchedAt.putIfAbsent("OUT_FOR_DELIVERY", row.getChangedAt());
            }
            if (note.contains("THAT BAI") || note.contains("GIAO KHONG THANH CONG") || note.contains("FAILED")) {
                touchedAt.putIfAbsent("DELIVERY_FAILED", row.getChangedAt());
            }
            if (note.contains("DANG HOAN") || note.contains("RETURNING") || note.contains("HOAN HANG")) {
                touchedAt.putIfAbsent("RETURNING", row.getChangedAt());
            }
            if (note.contains("HOAN VE") || note.contains("RETURNED")) {
                touchedAt.putIfAbsent("RETURNED", row.getChangedAt());
            }
        }

        String derivedCurrent = mapOrderStatusToTrackingCode(currentCode);
        if ("HOAN_VE".equalsIgnoreCase(currentCode) && touchedAt.containsKey("DELIVERY_FAILED")) {
            touchedAt.putIfAbsent("RETURNING", touchedAt.get("DELIVERY_FAILED"));
        }
        touchedAt.putIfAbsent(derivedCurrent, LocalDateTime.now());

        List<String> visibleCodes = visibleTrackingCodes(derivedCurrent);
        int currentIndex = visibleCodes.indexOf(derivedCurrent);
        if (currentIndex < 0) currentIndex = visibleCodes.size() - 1;

        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < visibleCodes.size(); i++) {
            String code = visibleCodes.get(i);
            StepDef step = defs.stream().filter(def -> def.code().equals(code)).findFirst().orElse(new StepDef(code, code));
            String state;
            if (i == currentIndex) {
                state = "current";
            } else if (i < currentIndex) {
                boolean reached = "CREATED".equals(step.code()) || touchedAt.containsKey(step.code());
                state = reached ? "done" : "pending";
            } else {
                state = "pending";
            }

            Map<String, Object> line = new LinkedHashMap<>();
            line.put("code", step.code());
            line.put("label", step.label());
            line.put("state", state);
            line.put("changedAt", touchedAt.getOrDefault(step.code(), null));
            line.put("note", buildTrackingNote(step.code(), currentCode));
            rows.add(line);
        }

        return rows;
    }

    private int resolveProgressPercent(List<Map<String, Object>> timeline) {
        if (timeline == null || timeline.isEmpty()) return 0;

        String currentCode = "";
        for (Map<String, Object> row : timeline) {
            if ("current".equals(String.valueOf(row.get("state")))) {
                currentCode = String.valueOf(row.get("code"));
                break;
            }
        }

        if (currentCode.isBlank()) {
            currentCode = String.valueOf(timeline.get(timeline.size() - 1).get("code"));
        }

        return switch (currentCode) {
            case "CREATED" -> 10;
            case "CONFIRMED" -> 20;
            case "PICKED_UP" -> 35;
            case "IN_TRANSIT" -> 55;
            case "OUT_FOR_DELIVERY" -> 75;
            case "DELIVERED" -> 100;
            case "DELIVERY_FAILED" -> 80;
            case "RETURNING" -> 90;
            case "RETURNED" -> 100;
            case "CANCELED" -> 100;
            default -> 0;
        };
    }

    private String mapOrderStatusToTrackingCode(String statusCode) {
        String code = String.valueOf(statusCode == null ? "" : statusCode).trim().toUpperCase();
        return switch (code) {
            case "CHO_XAC_NHAN" -> "CREATED";
            case "CHO_LAY_HANG", "CHO_XU_LY" -> "CONFIRMED";
            case "DANG_GIAO" -> "OUT_FOR_DELIVERY";
            case "HOAN_THANH", "DA_GIAO" -> "DELIVERED";
            case "GIAO_THAT_BAI" -> "DELIVERY_FAILED";
            case "HOAN_VE" -> "RETURNED";
            case "HUY", "DA_HUY" -> "CANCELED";
            default -> "CREATED";
        };
    }

    private String mapHistoryStatusToTrackingCode(String statusName) {
        String normalized = normalizeTrackingText(statusName);
        if (normalized.contains("CHO XAC NHAN")) return "CREATED";
        if (normalized.contains("CHO LAY HANG") || normalized.contains("CHO XU LY")) return "CONFIRMED";
        if (normalized.contains("DANG GIAO")) return "OUT_FOR_DELIVERY";
        if (normalized.contains("HOAN THANH") || normalized.contains("DA GIAO")) return "DELIVERED";
        if (normalized.contains("GIAO THAT BAI")) return "DELIVERY_FAILED";
        if (normalized.contains("HOAN VE")) return "RETURNED";
        if (normalized.contains("HUY")) return "CANCELED";
        return "";
    }

    private List<String> visibleTrackingCodes(String currentCode) {
        if ("DELIVERED".equals(currentCode)) {
            return List.of("CREATED", "CONFIRMED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED");
        }
        if ("DELIVERY_FAILED".equals(currentCode)) {
            return List.of("CREATED", "CONFIRMED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERY_FAILED");
        }
        if ("RETURNING".equals(currentCode)) {
            return List.of("CREATED", "CONFIRMED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERY_FAILED", "RETURNING");
        }
        if ("RETURNED".equals(currentCode)) {
            return List.of("CREATED", "CONFIRMED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERY_FAILED", "RETURNING", "RETURNED");
        }
        if ("CANCELED".equals(currentCode)) {
            return List.of("CREATED", "CONFIRMED", "CANCELED");
        }
        return List.of("CREATED", "CONFIRMED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED");
    }

    private String buildTrackingNote(String trackingCode, String currentStatusCode) {
        String status = String.valueOf(currentStatusCode == null ? "" : currentStatusCode).trim().toUpperCase();
        if ("CANCELED".equals(trackingCode) && ("HUY".equals(status) || "DA_HUY".equals(status))) {
            return "Đơn đã bị hủy theo yêu cầu hoặc bởi hệ thống.";
        }
        if ("DELIVERY_FAILED".equals(trackingCode) && "GIAO_THAT_BAI".equals(status)) {
            return "Shipper đã giao không thành công, đơn đang chờ xử lý tiếp.";
        }
        if ("RETURNED".equals(trackingCode) && "HOAN_VE".equals(status)) {
            return "Đơn đã hoàn trả về kho/shop.";
        }
        return "";
    }

    private String normalizeTrackingText(String value) {
        String raw = String.valueOf(value == null ? "" : value).trim();
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
        return normalized.replaceAll("\\s+", " ").trim();
    }

    /**
     * Gửi mail tự động (bất đồng bộ, không block) khi đơn hàng đạt mốc trạng thái quan trọng.
     * Lỗi mail sẽ bị bỏ qua – không ảnh hưởng đến luồng cập nhật trạng thái.
     */
    private void sendMilestoneEmailAsync(HoaDon hd, String trackingUrlOverride) {
        try {
            Optional<String> emailOpt = extractCustomerEmail(hd);
            if (emailOpt.isEmpty()) return;

            String trackingUrl = String.valueOf(trackingUrlOverride == null ? "" : trackingUrlOverride).trim();
            if (trackingUrl.isBlank()) return;

            String sdt = String.valueOf(hd.getSoDienThoaiNhanHang() == null ? "" : hd.getSoDienThoaiNhanHang()).trim();

            Map<String, Object> payload = buildLookupResponse(hd, sdt);
            sendTrackingEmail(emailOpt.get(), payload, trackingUrl);
        } catch (Exception ignored) {
            // Mail failure must not block the status transition
        }
    }

    private Optional<String> extractCustomerEmail(HoaDon hd) {
        if (hd == null || hd.getKhachHang() == null || hd.getKhachHang().getTaiKhoan() == null) {
            return Optional.empty();
        }
        String email = String.valueOf(hd.getKhachHang().getTaiKhoan().getEmail() == null
                ? ""
                : hd.getKhachHang().getTaiKhoan().getEmail()).trim();
        return email.isBlank() ? Optional.empty() : Optional.of(email);
    }

    private String buildDefaultLookupUrl(String maHoaDon, String soDienThoai) {
        String baseUrl = String.valueOf(lookupPublicUrl == null ? "" : lookupPublicUrl).trim();
        if (baseUrl.isBlank()) {
            baseUrl = DEFAULT_LOOKUP_URL;
        }
        baseUrl = baseUrl.replaceAll("/+$", "");

        String ma = String.valueOf(maHoaDon == null ? "" : maHoaDon).trim();
        String sdt = String.valueOf(soDienThoai == null ? "" : soDienThoai).trim();

        return baseUrl
                + "?ma=" + URLEncoder.encode(ma, StandardCharsets.UTF_8)
                + "&sdt=" + URLEncoder.encode(sdt, StandardCharsets.UTF_8);
    }

    private void sendTrackingEmail(String toEmail, Map<String, Object> payload, String trackingUrl) {
        if (mailSender == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Hệ thống mail chưa sẵn sàng");
        }

        if (mailUsername == null || mailUsername.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Hệ thống chưa cấu hình MAIL_USERNAME/MAIL_PASSWORD");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> order = (Map<String, Object>) payload.get("order");
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) payload.get("customer");
            @SuppressWarnings("unchecked")
            Map<String, Object> totals = (Map<String, Object>) payload.get("totals");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

            String maHoaDon = String.valueOf(order.get("maHoaDon"));
            String orderStatus = String.valueOf(order.get("orderStatusName"));
            String customerName = String.valueOf(customer.get("tenKhachHang") == null ? "Ban" : customer.get("tenKhachHang"));
            BigDecimal subtotal = (BigDecimal) totals.get("subtotal");
            BigDecimal shipping = (BigDecimal) totals.get("shippingFee");
            BigDecimal grandTotal = (BigDecimal) totals.get("grandTotal");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(toEmail);
            if (lookupMailFrom != null && !lookupMailFrom.isBlank()) {
                helper.setFrom(lookupMailFrom);
            }
            helper.setSubject("[DIRTYWAVE] Cập nhật đơn hàng " + maHoaDon);
            helper.setText(buildTrackingMailHtml(
                    customerName,
                    maHoaDon,
                    orderStatus,
                    items,
                    subtotal,
                    shipping,
                    grandTotal,
                    trackingUrl
            ), true);

            mailSender.send(message);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gửi email thất bại. Vui lòng kiểm tra cấu hình SMTP (host/port/user/pass)", ex);
        }
    }

    private String buildTrackingMailHtml(String customerName,
                                         String maHoaDon,
                                         String orderStatus,
                                         List<Map<String, Object>> items,
                                         BigDecimal subtotal,
                                         BigDecimal shipping,
                                         BigDecimal grandTotal,
                                         String trackingUrl) {
        StringBuilder itemRows = new StringBuilder();
        for (Map<String, Object> item : items) {
            itemRows.append("<tr>")
                                        .append("<td style='padding:10px 8px;border-bottom:1px solid #ececec;color:#111827;word-break:break-word;'>").append(escapeHtml(String.valueOf(item.get("tenSanPham")))).append("</td>")
                                        .append("<td style='padding:10px 8px;border-bottom:1px solid #ececec;text-align:center;color:#111827;'>").append(escapeHtml(String.valueOf(item.get("soLuong")))).append("</td>")
                                        .append("<td style='padding:10px 8px;border-bottom:1px solid #ececec;text-align:right;color:#111827;white-space:nowrap;'>").append(formatMoney(item.get("giaBan"))).append("</td>")
                                        .append("<td style='padding:10px 8px;border-bottom:1px solid #ececec;text-align:right;color:#111827;font-weight:700;white-space:nowrap;'>").append(formatMoney(item.get("thanhTien"))).append("</td>")
                                        .append("</tr>");
        }

                String totalItems = String.valueOf(items.size());
                String safeCustomerName = escapeHtml(customerName);
                String safeOrderCode = escapeHtml(maHoaDon);
                String safeOrderStatus = escapeHtml(orderStatus);
                String safeTrackingUrl = escapeHtml(trackingUrl);

                return """
                                <div style='margin:0;padding:12px;background:#eef2f7;font-family:Segoe UI,Arial,Helvetica,sans-serif;color:#111827;'>
                                    <table role='presentation' cellspacing='0' cellpadding='0' border='0' width='100%%' style='max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #d9e1ec;border-radius:18px;overflow:hidden;'>
                                        <tr>
                                            <td style='background:linear-gradient(140deg,#9f1239 0%%,#be123c 45%%,#111827 100%%);padding:22px 18px 24px;color:#ffffff;'>
                                                <div style='font-size:12px;letter-spacing:1.5px;text-transform:uppercase;opacity:0.85;'>DirtyWave Order Service</div>
                                                <h1 style='margin:10px 0 8px;font-size:30px;line-height:1.15;letter-spacing:1.4px;'>DIRTYWAVE</h1>
                                                <p style='margin:0;color:#ffe4ec;font-size:14px;line-height:1.6;'>Trạng thái đơn hàng của bạn đã được cập nhật. Nhấn vào nút theo dõi bên dưới để xem chi tiết mới nhất.</p>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style='padding:18px 16px 8px;'>
                                                <h2 style='margin:0 0 8px;font-size:28px;line-height:1.2;color:#111827;'>Cảm ơn bạn đã đặt hàng</h2>
                                                <p style='margin:0;color:#475569;font-size:15px;line-height:1.7;'>Xin chào <strong style='color:#be123c;'>%s</strong>, đơn <strong>%s</strong> hiện đang ở trạng thái:</p>
                                                <div style='margin-top:10px;display:inline-block;padding:8px 14px;border-radius:999px;background:#fff1f2;border:1px solid #fecdd3;color:#be123c;font-size:13px;font-weight:700;'>%s</div>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style='padding:8px 16px 0;'>
                                                <table role='presentation' cellspacing='0' cellpadding='0' border='0' width='100%%' style='border-collapse:separate;border-spacing:0;width:100%%;'>
                                                    <tr>
                                                        <td style='width:33.33%%;padding:10px 8px;background:#f8fafc;border:1px solid #e2e8f0;border-right:none;border-radius:12px 0 0 12px;'>
                                                            <div style='font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.6px;'>Trạng thái</div>
                                                            <div style='margin-top:5px;font-size:15px;color:#047857;font-weight:700;'>Đang xử lý</div>
                                                        </td>
                                                        <td style='width:33.33%%;padding:10px 8px;background:#f8fafc;border:1px solid #e2e8f0;border-right:none;'>
                                                            <div style='font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.6px;'>Mã đơn</div>
                                                            <div style='margin-top:5px;font-size:16px;color:#be123c;font-weight:700;word-break:break-word;'>%s</div>
                                                        </td>
                                                        <td style='width:33.33%%;padding:10px 8px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:0 12px 12px 0;'>
                                                            <div style='font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:0.6px;'>Tổng sản phẩm</div>
                                                            <div style='margin-top:5px;font-size:16px;color:#0f172a;font-weight:700;'>%s</div>
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style='padding:14px 16px 0;'>
                                                <h3 style='margin:0 0 10px;font-size:19px;color:#0f172a;'>Chi tiết đơn hàng</h3>
                                                <table role='presentation' cellspacing='0' cellpadding='0' border='0' width='100%%' style='border-collapse:collapse;width:100%%;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;'>
                                                    <thead>
                                                        <tr style='background:#f8fafc;'>
                                                            <th style='padding:10px 8px;text-align:left;color:#334155;font-size:11px;text-transform:uppercase;letter-spacing:0.5px;'>Sản phẩm</th>
                                                            <th style='padding:10px 8px;text-align:center;color:#334155;font-size:11px;text-transform:uppercase;letter-spacing:0.5px;'>SL</th>
                                                            <th style='padding:10px 8px;text-align:right;color:#334155;font-size:11px;text-transform:uppercase;letter-spacing:0.5px;'>Đơn giá</th>
                                                            <th style='padding:10px 8px;text-align:right;color:#334155;font-size:11px;text-transform:uppercase;letter-spacing:0.5px;'>Thành tiền</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        %s
                                                    </tbody>
                                                </table>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style='padding:14px 16px 0;'>
                                                <div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:14px 14px 6px;'>
                                                    <table role='presentation' cellspacing='0' cellpadding='0' border='0' width='100%%' style='width:100%%;border-collapse:collapse;'>
                                                        <tr>
                                                            <td style='padding:6px 0;color:#475569;'>Tạm tính:</td>
                                                            <td style='padding:6px 0;color:#0f172a;font-weight:700;text-align:right;'>%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style='padding:6px 0;color:#475569;'>Phí vận chuyển:</td>
                                                            <td style='padding:6px 0;color:#0f172a;font-weight:700;text-align:right;'>%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style='padding:10px 0 6px;color:#be123c;font-size:20px;font-weight:700;'>Tổng cộng:</td>
                                                            <td style='padding:10px 0 6px;color:#be123c;font-size:24px;font-weight:800;text-align:right;'>%s</td>
                                                        </tr>
                                                    </table>
                                                </div>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style='padding:18px 16px 22px;text-align:center;'>
                                                <a href='%s' style='display:inline-block;padding:13px 26px;border-radius:10px;background:linear-gradient(145deg,#be123c,#9f1239);color:#ffffff;text-decoration:none;font-weight:700;font-size:15px;letter-spacing:0.3px;'>Theo dõi đơn hàng</a>
                                                <p style='margin:14px 0 0;color:#64748b;font-size:12px;line-height:1.6;'>Nếu bạn không thực hiện đơn hàng này, vui lòng liên hệ bộ phận CSKH DirtyWave để được hỗ trợ nhanh nhất.</p>
                                            </td>
                                        </tr>
                                    </table>
                                </div>
                                """.formatted(
                                safeCustomerName,
                                safeOrderCode,
                                safeOrderStatus,
                                safeOrderCode,
                                totalItems,
                                itemRows,
                                formatMoney(subtotal),
                                formatMoney(shipping),
                                formatMoney(grandTotal),
                                safeTrackingUrl
                );
    }

        private String escapeHtml(String value) {
                String raw = String.valueOf(value == null ? "" : value);
                return raw
                                .replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                                .replace("\"", "&quot;")
                                .replace("'", "&#39;");
        }

    private String formatMoney(Object value) {
        BigDecimal amount;
        if (value instanceof BigDecimal b) {
            amount = b;
        } else {
            try {
                amount = new BigDecimal(String.valueOf(value));
            } catch (Exception ex) {
                amount = BigDecimal.ZERO;
            }
        }
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }

    private record PaymentFlowState(String code, String label, String tone) {
    }

    private record FulfillmentState(String code, String label) {
    }
}