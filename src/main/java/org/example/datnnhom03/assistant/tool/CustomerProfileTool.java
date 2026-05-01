package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Model.ChatSession;
import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Model.KhachHang;
import org.example.datnnhom03.Repository.ChatSessionRepository;
import org.example.datnnhom03.Repository.HoaDonRepository;
import org.example.datnnhom03.Repository.KhachHangRepository;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class CustomerProfileTool implements AssistantTool {

    private final KhachHangRepository khachHangRepository;
    private final HoaDonRepository hoaDonRepository;
    private final ChatSessionRepository chatSessionRepository;

    public CustomerProfileTool(KhachHangRepository khachHangRepository,
                               HoaDonRepository hoaDonRepository,
                               ChatSessionRepository chatSessionRepository) {
        this.khachHangRepository = khachHangRepository;
        this.hoaDonRepository = hoaDonRepository;
        this.chatSessionRepository = chatSessionRepository;
    }

    @Override
    public String getName() {
        return "find_customer_profile";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return true;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        String keyword = normalize(args.get("keyword"));
        String sessionCode = safe(args.get("sessionCode"));
        String customerPhone = digitsOnly(args.get("customerPhone"));
        String customerEmail = normalize(args.get("customerEmail"));
        String customerName = normalize(args.get("customerName"));

        ChatSession linkedSession = (!customerPhone.isBlank() || !customerEmail.isBlank() || !customerName.isBlank())
                ? null
                : resolveSession(sessionCode, customerPhone, customerEmail, customerName, keyword);
        if (linkedSession != null) {
            if (customerPhone.isBlank()) customerPhone = digitsOnly(linkedSession.getCustomerPhone());
            if (customerEmail.isBlank()) customerEmail = normalize(linkedSession.getCustomerEmail());
            if (customerName.isBlank()) customerName = normalize(linkedSession.getCustomerName());
        }

        final String phoneKey = customerPhone;
        final String emailKey = customerEmail;
        final String nameKey = customerName;
        final String keywordKey = keyword;
        final boolean hasStrongIdentity = !phoneKey.isBlank() || !emailKey.isBlank() || !nameKey.isBlank();
        final boolean recent30Days = keywordKey.contains("30 ngay") || keywordKey.contains("30 ngày") || keywordKey.contains("gan day") || keywordKey.contains("gần đây");

        List<KhachHang> matchedCustomers = khachHangRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(kh -> matches(kh, phoneKey, emailKey, nameKey, keywordKey, hasStrongIdentity))
                .limit(5)
                .collect(Collectors.toList());

        List<HoaDon> relatedOrders = hoaDonRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(order -> matches(order, phoneKey, emailKey, nameKey, keywordKey, hasStrongIdentity))
                .filter(order -> !recent30Days || (order.getNgayTao() != null && !order.getNgayTao().isBefore(LocalDateTime.now().minusDays(30))))
                .sorted(Comparator.comparing(HoaDon::getNgayTao, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(6)
                .collect(Collectors.toList());

        List<ChatSession> relatedSessions = chatSessionRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(session -> matches(session, phoneKey, emailKey, nameKey, keywordKey, hasStrongIdentity))
                .sorted(Comparator.comparing(ChatSession::getLastMessageAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(6)
                .collect(Collectors.toList());

        BigDecimal totalSpent = relatedOrders.stream()
                .map(HoaDon::getThanhTien)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keyword", keyword);
        result.put("count", Math.max(matchedCustomers.size(), relatedOrders.size() + relatedSessions.size() > 0 ? 1 : 0));
        result.put("matchedCustomers", matchedCustomers.stream().map(this::toCustomerRow).collect(Collectors.toList()));
        result.put("recentOrders", relatedOrders.stream().map(this::toOrderRow).collect(Collectors.toList()));
        result.put("recentSessions", relatedSessions.stream().map(this::toSessionRow).collect(Collectors.toList()));
        result.put("orderCount", relatedOrders.size());
        result.put("chatSessionCount", relatedSessions.size());
        result.put("totalSpent", totalSpent);
        result.put("summary", buildSummary(matchedCustomers, relatedOrders, relatedSessions, totalSpent, linkedSession));
        return result;
    }

    private ChatSession resolveSession(String sessionCode,
                                       String customerPhone,
                                       String customerEmail,
                                       String customerName,
                                       String keyword) {
        if (!sessionCode.isBlank()) {
            ChatSession byCode = chatSessionRepository.findBySessionCode(sessionCode).orElse(null);
            if (byCode != null) return byCode;
        }

        return chatSessionRepository.findAll().stream()
                .filter(session -> matches(session, customerPhone, customerEmail, customerName, keyword, !customerPhone.isBlank() || !customerEmail.isBlank() || !customerName.isBlank()))
                .max(Comparator.comparing(ChatSession::getLastMessageAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ChatSession::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private boolean matches(KhachHang customer,
                            String customerPhone,
                            String customerEmail,
                            String customerName,
                            String keyword,
                            boolean hasStrongIdentity) {
        String phone = digitsOnly(customer.getSoDienThoai());
        String name = normalize(customer.getTenKhachHang());
        String email = customer.getTaiKhoan() == null ? "" : normalize(customer.getTaiKhoan().getEmail());
        return matchesPhone(phone, customerPhone)
                || matchesEmail(email, customerEmail)
                || matchesName(name, customerName)
                || (!hasStrongIdentity && matchesKeyword(name, keyword))
                || (!hasStrongIdentity && matchesKeyword(email, keyword))
                || (!hasStrongIdentity && matchesKeyword(phone, digitsOnly(keyword)));
    }

    private boolean matches(HoaDon order,
                            String customerPhone,
                            String customerEmail,
                            String customerName,
                            String keyword,
                            boolean hasStrongIdentity) {
        String phone = digitsOnly(order.getSoDienThoaiNhanHang());
        String name = normalize(order.getTenKhachHang());
        String email = order.getKhachHang() != null && order.getKhachHang().getTaiKhoan() != null
                ? normalize(order.getKhachHang().getTaiKhoan().getEmail()) : "";
        return matchesPhone(phone, customerPhone)
                || matchesEmail(email, customerEmail)
                || matchesName(name, customerName)
                || (!hasStrongIdentity && matchesKeyword(name, keyword))
                || (!hasStrongIdentity && matchesKeyword(phone, digitsOnly(keyword)));
    }

    private boolean matches(ChatSession session,
                            String customerPhone,
                            String customerEmail,
                            String customerName,
                            String keyword,
                            boolean hasStrongIdentity) {
        String phone = digitsOnly(session.getCustomerPhone());
        String name = normalize(session.getCustomerName());
        String email = normalize(session.getCustomerEmail());
        return matchesPhone(phone, customerPhone)
                || matchesEmail(email, customerEmail)
                || matchesName(name, customerName)
                || (!hasStrongIdentity && matchesKeyword(name, keyword))
                || (!hasStrongIdentity && matchesKeyword(email, keyword))
                || (!hasStrongIdentity && matchesKeyword(phone, digitsOnly(keyword)));
    }

    private boolean matchesPhone(String haystack, String phoneNeedle) {
        if (phoneNeedle == null || phoneNeedle.isBlank()) return false;
        return haystack.equals(phoneNeedle) || haystack.endsWith(phoneNeedle) || haystack.contains(phoneNeedle);
    }

    private boolean matchesEmail(String haystack, String emailNeedle) {
        return emailNeedle != null && !emailNeedle.isBlank() && haystack.equals(emailNeedle);
    }

    private boolean matchesName(String haystack, String nameNeedle) {
        if (nameNeedle == null || nameNeedle.isBlank()) return false;
        if (haystack.equals(nameNeedle) || haystack.contains(nameNeedle) || nameNeedle.contains(haystack)) return true;
        String[] tokens = nameNeedle.split("\s+");
        int matched = 0;
        int meaningful = 0;
        for (String token : tokens) {
            if (token == null || token.isBlank() || token.length() < 2) continue;
            meaningful++;
            if (haystack.contains(token)) matched++;
        }
        return meaningful > 0 && matched == meaningful;
    }

    private boolean matchesKeyword(String haystack, String keyword) {
        return keyword != null && !keyword.isBlank() && haystack.contains(keyword);
    }

    private Map<String, Object> toCustomerRow(KhachHang customer) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", customer.getId());
        row.put("maKhachHang", customer.getMaKhachHang());
        row.put("tenKhachHang", customer.getTenKhachHang());
        row.put("soDienThoai", customer.getSoDienThoai());
        row.put("email", customer.getTaiKhoan() == null ? null : customer.getTaiKhoan().getEmail());
        row.put("trangThai", customer.getTrangThai());
        return row;
    }

    private Map<String, Object> toOrderRow(HoaDon order) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("maHoaDon", order.getMaHoaDon());
        row.put("tenKhachHang", order.getTenKhachHang());
        row.put("soDienThoaiNhanHang", order.getSoDienThoaiNhanHang());
        row.put("trangThai", order.getTrangThai());
        row.put("thanhTien", order.getThanhTien());
        row.put("ngayTao", order.getNgayTao());
        return row;
    }

    private Map<String, Object> toSessionRow(ChatSession session) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("sessionCode", session.getSessionCode());
        row.put("customerName", session.getCustomerName());
        row.put("customerEmail", session.getCustomerEmail());
        row.put("customerPhone", session.getCustomerPhone());
        row.put("status", session.getStatus());
        row.put("chatMode", session.getChatMode());
        row.put("lastMessageAt", session.getLastMessageAt());
        return row;
    }

    private String buildSummary(List<KhachHang> customers,
                                List<HoaDon> orders,
                                List<ChatSession> sessions,
                                BigDecimal totalSpent,
                                ChatSession linkedSession) {
        String displayName = customers.stream()
                .map(KhachHang::getTenKhachHang)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .findFirst()
                .orElseGet(() -> linkedSession != null ? safe(linkedSession.getCustomerName()) : "khách này");

        StringBuilder sb = new StringBuilder();
        sb.append("Hồ sơ nhanh của ").append(displayName.isBlank() ? "khách này" : displayName).append(": ");
        if (!customers.isEmpty()) {
            KhachHang first = customers.get(0);
            sb.append("có hồ sơ khách hàng, SĐT ").append(safe(first.getSoDienThoai())).append(". ");
        }
        if (orders.isEmpty()) {
            sb.append("hiện tại khách này chưa có đơn hàng nào. ");
        } else {
            sb.append("đã tìm thấy ").append(orders.size()).append(" đơn gần đây, tổng chi tiêu khoảng ").append(totalSpent).append(". ");
        }
        sb.append("Có ").append(sessions.size()).append(" phiên chat liên quan.");
        return sb.toString();
    }

    private String normalize(Object value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private String digitsOnly(Object value) {
        return value == null ? "" : String.valueOf(value).replaceAll("[^0-9]", "");
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
