package org.example.datnnhom03.assistant.tool;

import org.example.datnnhom03.Model.ChatMessage;
import org.example.datnnhom03.Model.ChatSession;
import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Repository.ChatMessageRepository;
import org.example.datnnhom03.Repository.ChatSessionRepository;
import org.example.datnnhom03.Repository.HoaDonRepository;
import org.example.datnnhom03.assistant.policy.AssistantRole;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CustomerInsightTool implements AssistantTool {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final HoaDonRepository hoaDonRepository;

    public CustomerInsightTool(ChatSessionRepository chatSessionRepository,
                               ChatMessageRepository chatMessageRepository,
                               HoaDonRepository hoaDonRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.hoaDonRepository = hoaDonRepository;
    }

    @Override
    public String getName() {
        return "get_customer_insight";
    }

    @Override
    public boolean supports(AssistantRole role) {
        return true;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args) {
        String sessionCode = safe(args.get("sessionCode"));
        String customerEmail = safe(args.get("customerEmail"));
        String customerPhone = safe(args.get("customerPhone"));
        String customerNameHint = safe(args.get("customerName"));

        ChatSession session = resolveSession(sessionCode, customerEmail, customerPhone, customerNameHint);
        if (session == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("found", false);
            empty.put("summary", "Chưa có đủ dữ liệu khách để tóm tắt.");
            empty.put("reason", "missing_session_context");
            return empty;
        }

        final ChatSession resolvedSession = session;
        final String customerName = firstNonBlank(customerNameHint, resolvedSession.getCustomerName());
        final String effectivePhone = firstNonBlank(customerPhone, resolvedSession.getCustomerPhone());
        final String effectiveEmail = firstNonBlank(customerEmail, resolvedSession.getCustomerEmail());

        List<ChatMessage> recentMessages = chatMessageRepository
                .findRecentMessagesInSession(resolvedSession.getId(), 8)
                .stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
                .collect(Collectors.toList());

        List<String> customerMessages = recentMessages.stream()
                .filter(m -> "CUSTOMER".equalsIgnoreCase(m.getSenderType()))
                .map(ChatMessage::getContent)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        List<HoaDon> relatedOrders = hoaDonRepository.findAll().stream()
                .filter(hd -> matchesOrder(hd, effectivePhone, effectiveEmail, customerName))
                .sorted(Comparator.comparing(HoaDon::getNgayTao,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .collect(Collectors.toList());

        BigDecimal totalSpent = relatedOrders.stream()
                .map(HoaDon::getThanhTien)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("customerName", customerName);
        result.put("customerEmail", effectiveEmail);
        result.put("customerPhone", effectivePhone);
        result.put("sessionCode", safe(resolvedSession.getSessionCode()));
        result.put("sessionStatus", safe(resolvedSession.getStatus()));
        result.put("chatMode", safe(resolvedSession.getChatMode()));
        result.put("recentCustomerMessages", customerMessages);
        result.put("orderCount", relatedOrders.size());
        result.put("totalSpent", totalSpent);
        result.put("recentOrders", relatedOrders.stream().map(this::toOrderRow).collect(Collectors.toList()));
        result.put("summary", buildSummary(resolvedSession, customerMessages, relatedOrders, totalSpent));
        return result;
    }

    private ChatSession resolveSession(String sessionCode,
                                       String customerEmail,
                                       String customerPhone,
                                       String customerNameHint) {
        if (!sessionCode.isBlank()) {
            ChatSession byCode = chatSessionRepository.findBySessionCode(sessionCode).orElse(null);
            if (byCode != null) return byCode;
        }

        List<ChatSession> all = chatSessionRepository.findAll();
        return all.stream()
                .filter(Objects::nonNull)
                .filter(session -> matchesSession(session, customerEmail, customerPhone, customerNameHint))
                .max(Comparator.comparing(ChatSession::getLastMessageAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ChatSession::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private boolean matchesSession(ChatSession session,
                                   String customerEmail,
                                   String customerPhone,
                                   String customerNameHint) {
        String sessionEmail = safe(session.getCustomerEmail());
        String sessionPhone = digitsOnly(session.getCustomerPhone());
        String sessionName = safe(session.getCustomerName());

        if (!customerEmail.isBlank() && sessionEmail.equalsIgnoreCase(customerEmail)) {
            return true;
        }
        if (!customerPhone.isBlank() && (sessionPhone.equals(digitsOnly(customerPhone)) || sessionPhone.endsWith(digitsOnly(customerPhone)))) {
            return true;
        }
        return !customerNameHint.isBlank() && sessionName.equalsIgnoreCase(customerNameHint);
    }

    private boolean matchesOrder(HoaDon hd, String phone, String email, String customerName) {
        String hdPhone = digitsOnly(hd.getSoDienThoaiNhanHang());
        String hdName = safe(hd.getTenKhachHang());
        String normalizedCustomerName = safe(customerName);

        if (!phone.isBlank() && (hdPhone.equals(digitsOnly(phone)) || hdPhone.endsWith(digitsOnly(phone)))) {
            return true;
        }
        if (!normalizedCustomerName.isBlank() && hdName.equalsIgnoreCase(normalizedCustomerName)) {
            return true;
        }
        if (!email.isBlank() && hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
            String orderEmail = safe(hd.getKhachHang().getTaiKhoan().getEmail());
            return orderEmail.equalsIgnoreCase(email);
        }
        return false;
    }

    private Map<String, Object> toOrderRow(HoaDon hd) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("maHoaDon", hd.getMaHoaDon());
        row.put("trangThai", hd.getTrangThai());
        row.put("thanhTien", hd.getThanhTien());
        row.put("ngayTao", hd.getNgayTao());
        return row;
    }

    private String buildSummary(ChatSession session,
                                List<String> customerMessages,
                                List<HoaDon> relatedOrders,
                                BigDecimal totalSpent) {
        String lastCustomerMessage = customerMessages.isEmpty()
                ? "Chưa có tin nhắn khách gần đây."
                : customerMessages.get(customerMessages.size() - 1);

        String lastOrderSummary = relatedOrders.isEmpty()
                ? "Chưa thấy đơn hàng liên quan."
                : "Đơn gần nhất " + safe(relatedOrders.get(0).getMaHoaDon())
                + " đang ở trạng thái " + safe(relatedOrders.get(0).getTrangThai()) + ".";

        return "Khách " + safe(session.getCustomerName())
                + " đang ở phiên chat " + safe(session.getSessionCode())
                + ". Có " + relatedOrders.size() + " đơn liên quan, tổng giá trị gần đúng " + totalSpent
                + ". Tin nhắn gần nhất của khách: \"" + lastCustomerMessage + "\". "
                + lastOrderSummary;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String digitsOnly(Object value) {
        return value == null ? "" : value.toString().replaceAll("[^0-9]", "");
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
