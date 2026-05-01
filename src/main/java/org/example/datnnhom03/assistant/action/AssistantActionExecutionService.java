package org.example.datnnhom03.assistant.action;

import org.example.datnnhom03.Model.KhuyenMai;
import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Service.KhuyenMaiService;
import org.example.datnnhom03.Service.SanPhamService;
import org.example.datnnhom03.assistant.audit.AssistantActionAuditService;
import org.example.datnnhom03.assistant.handler.AssistantHandlerResult;
import org.example.datnnhom03.assistant.service.AssistantActionSafetyService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class AssistantActionExecutionService {

    private final KhuyenMaiService khuyenMaiService;
    private final SanPhamService sanPhamService;
    private final AssistantActionSafetyService safetyService;
    private final AssistantActionAuditService auditService;

    public AssistantActionExecutionService(KhuyenMaiService khuyenMaiService,
                                           SanPhamService sanPhamService,
                                           AssistantActionSafetyService safetyService,
                                           AssistantActionAuditService auditService) {
        this.khuyenMaiService = khuyenMaiService;
        this.sanPhamService = sanPhamService;
        this.safetyService = safetyService;
        this.auditService = auditService;
    }

    public AssistantHandlerResult execute(PendingAssistantAction action) {
        if (action == null) {
            AssistantHandlerResult result = AssistantHandlerResult.of("Hiện không có hành động chờ xác nhận.", false);
            result.getMetadata().put("actionStatus", "NO_PENDING_ACTION");
            result.getMetadata().put("auditId", auditService.recordExecution(null, result));
            return result;
        }
        if (action.isExpired()) {
            AssistantHandlerResult result = AssistantHandlerResult.of("Yêu cầu xác nhận đã hết hạn. Anh/chị tạo lại lệnh giúp em.", false);
            result.getMetadata().put("actionStatus", "EXPIRED");
            result.getMetadata().put("auditId", auditService.recordExecution(action, result));
            return result;
        }

        AssistantActionSafetyService.SafetyCheckResult safety = safetyService.validateForExecution(action.getIntent(), action.getPayload());
        if (!safety.isAllowed()) {
            AssistantHandlerResult result = AssistantHandlerResult.of("Em đã chặn thực thi vì payload còn thiếu hoặc chưa an toàn. Anh/chị kiểm tra lại thông tin rồi tạo thao tác mới giúp em.", false);
            result.getMetadata().put("actionStatus", "BLOCKED_BY_SAFETY");
            result.getMetadata().put("warnings", safety.getWarnings());
            result.getPayload().putAll(action.getPayload());
            result.getPayload().put("safety", safety.toMap());
            result.getMetadata().put("auditId", auditService.recordExecution(action, result));
            return result;
        }

        if (!action.isExecutable()) {
            AssistantHandlerResult result = AssistantHandlerResult.of("Em đã khóa đơn nháp. Anh/chị có thể mở workflow tạo đơn để kiểm tra và hoàn tất đơn chính thức.", true);
            result.getMetadata().put("actionStatus", "CONFIRMED");
            result.getMetadata().put("warnings", safety.getWarnings());
            result.getPayload().putAll(action.getPayload());
            result.getPayload().put("draftOrder", new LinkedHashMap<>(action.getPayload()));
            result.getPayload().put("safety", safety.toMap());
            result.getSuggestedActions().add(Map.of("type", "open_order_workflow", "label", "Mở workflow tạo đơn", "priority", "high"));
            result.getSuggestedActions().add(Map.of("type", "cancel_draft", "label", "Hủy nháp", "priority", "medium"));
            result.getMetadata().put("auditId", auditService.recordExecution(action, result));
            return result;
        }

        AssistantHandlerResult result = switch (action.getIntent()) {
            case CREATE_VOUCHER -> executeCreateVoucher(action, safety);
            case PRODUCT_UPSERT -> executeProductUpsert(action, safety);
            default -> {
                AssistantHandlerResult fallback = AssistantHandlerResult.of("Luồng này đã sẵn sàng ở mức preview nhưng chưa bật execute trực tiếp.", true);
                fallback.getMetadata().put("actionStatus", "NOT_IMPLEMENTED");
                fallback.getPayload().putAll(action.getPayload());
                fallback.getPayload().put("safety", safety.toMap());
                fallback.getMetadata().put("warnings", safety.getWarnings());
                yield fallback;
            }
        };
        result.getMetadata().put("auditId", auditService.recordExecution(action, result));
        return result;
    }

    private AssistantHandlerResult executeCreateVoucher(PendingAssistantAction action,
                                                        AssistantActionSafetyService.SafetyCheckResult safety) {
        KhuyenMai km = new KhuyenMai();
        km.setMaKhuyenMai(stringValue(action.getPayload().get("maKhuyenMai")));
        km.setTenKhuyenMai(stringValue(action.getPayload().get("tenKhuyenMai")));
        km.setGiaTri(bigDecimalValue(action.getPayload().get("giaTri"), BigDecimal.TEN));
        km.setDonViGiam(stringValue(action.getPayload().getOrDefault("donViGiam", "PERCENT")).toUpperCase(Locale.ROOT));
        km.setNgayBatDau((LocalDateTime) action.getPayload().getOrDefault("ngayBatDau", LocalDateTime.now()));
        km.setNgayKetThuc((LocalDateTime) action.getPayload().getOrDefault("ngayKetThuc", LocalDateTime.now().plusDays(7)));
        km.setTrangThai(stringValue(action.getPayload().getOrDefault("trangThai", "Hoạt động")));
        KhuyenMai saved = khuyenMaiService.create(km);

        AssistantHandlerResult result = AssistantHandlerResult.of(
                "Đã tạo khuyến mãi " + saved.getMaKhuyenMai() + " - " + saved.getTenKhuyenMai() + " thành công.", true);
        result.getPayload().put("id", saved.getId());
        result.getPayload().put("maKhuyenMai", saved.getMaKhuyenMai());
        result.getPayload().put("tenKhuyenMai", saved.getTenKhuyenMai());
        result.getPayload().put("giaTri", saved.getGiaTri());
        result.getPayload().put("safety", safety.toMap());
        result.getMetadata().put("actionStatus", "EXECUTED");
        result.getMetadata().put("warnings", safety.getWarnings());
        return result;
    }

    private AssistantHandlerResult executeProductUpsert(PendingAssistantAction action,
                                                        AssistantActionSafetyService.SafetyCheckResult safety) {
        String productIdRaw = stringValue(action.getPayload().get("productId"));
        String maSanPham = stringValue(action.getPayload().get("maSanPham"));
        SanPham sp = new SanPham();
        sp.setMaSanPham(maSanPham);
        sp.setTenSanPham(stringValue(action.getPayload().get("tenSanPham")));
        sp.setMoTa(stringValue(action.getPayload().get("moTa")));
        sp.setTrangThai(stringValue(action.getPayload().getOrDefault("trangThai", "Hoạt động")));
        sp.setNguoiSua(stringValue(action.getPayload().getOrDefault("nguoiSua", "assistant")));
        sp.setNguoiTao(stringValue(action.getPayload().getOrDefault("nguoiTao", sp.getNguoiSua())));
        SanPham saved;
        if (!productIdRaw.isBlank()) {
            saved = sanPhamService.update(Integer.parseInt(productIdRaw), sp);
        } else {
            saved = sanPhamService.create(sp);
        }

        AssistantHandlerResult result = AssistantHandlerResult.of(
                (productIdRaw.isBlank() ? "Đã tạo" : "Đã cập nhật") + " sản phẩm " + saved.getMaSanPham() + " - " + saved.getTenSanPham() + ".", true);
        result.getPayload().put("id", saved.getId());
        result.getPayload().put("maSanPham", saved.getMaSanPham());
        result.getPayload().put("tenSanPham", saved.getTenSanPham());
        result.getPayload().put("safety", safety.toMap());
        result.getMetadata().put("actionStatus", "EXECUTED");
        result.getMetadata().put("warnings", safety.getWarnings());
        return result;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private BigDecimal bigDecimalValue(Object value, BigDecimal defaultValue) {
        try {
            if (value == null) return defaultValue;
            return value instanceof BigDecimal bd ? bd : new BigDecimal(String.valueOf(value).trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }
}
