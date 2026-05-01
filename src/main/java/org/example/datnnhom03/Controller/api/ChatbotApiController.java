package org.example.datnnhom03.Controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.datnnhom03.Model.ChatMessage;
import org.example.datnnhom03.Model.ChatSession;
import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Model.SanPham;
import org.example.datnnhom03.Model.SanPhamChiTiet;
import org.example.datnnhom03.Repository.HoaDonRepository;
import org.example.datnnhom03.Repository.PhieuGiamGiaRepository;
import org.example.datnnhom03.Repository.SanPhamChiTietRepository;
import org.example.datnnhom03.Repository.SanPhamRepository;
import org.example.datnnhom03.Service.ChatMessageService;
import org.example.datnnhom03.Service.ChatSessionService;
import org.example.datnnhom03.assistant.service.OpenAiGateway;
import org.example.datnnhom03.chatbot.service.CustomerBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatbotApiController {

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private OpenAiGateway openAiGateway;

    @Autowired
    private CustomerBotService customerBotService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ───────── keyword / colour / category constants ─────────

    private static final String[][] CATEGORY_KEYWORDS = {
            {"bomber"},
            {"hoodie"},
            {"coach"},
            {"jacket", "khoác", "khoac", "áo khoác", "ao khoac"}
    };

    private static final String[][] COLOR_MAP = {
            {"đen", "den"},
            {"trắng", "trang"},
            {"xám", "xam"},
            {"xanh"},
            {"nâu", "nau"},
            {"be"},
            {"đỏ", "do"},
            {"navy"}
    };

    private static final String[] COLOR_DISPLAY = {
            "Đen", "Trắng", "Xám", "Xanh", "Nâu", "Be", "Đỏ", "Navy"
    };

    // ───────── HTTP endpoints ─────────

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "DirtyWave Chatbot");
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/message")
    @Transactional
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== CHATBOT MESSAGE START ===");

            String sessionCode = String.valueOf(request.getOrDefault("sessionId", "")).trim();
            String messageText = String.valueOf(request.getOrDefault("message", "")).trim();
            String customerName = (String) request.getOrDefault("customerName", "Customer");
            String customerEmail = (String) request.get("customerEmail");
            String customerPhone = (String) request.get("customerPhone");

            if (sessionCode.isEmpty()) {
                return ResponseEntity.ok(quickMsg(
                        "Không xác định được phiên chat. Anh/chị vui lòng tải lại trang.",
                        "Bomber đen dưới 700k", "Hoodie size L"));
            }

            if (messageText.isEmpty()) {
                return ResponseEntity.ok(quickMsg(
                        "Anh/chị chưa nhập nội dung cần hỗ trợ.",
                        "Bomber đen dưới 700k", "Hoodie size L", "Tư vấn size"));
            }

            System.out.println("Message: " + messageText);

            // Get or create session
            ChatSession session = chatSessionService.getSessionByCode(sessionCode);
            if (session == null) {
                session = new ChatSession();
                session.setSessionCode(sessionCode);
                session.setCustomerName(customerName);
                session.setCustomerEmail(customerEmail);
                session.setCustomerPhone(customerPhone);
                session.setStatus("OPEN");
                session.setChatMode("AUTO");
                session.setCreatedAt(LocalDateTime.now());
                session.setUpdatedAt(LocalDateTime.now());
                session = chatSessionService.saveSession(session);
            }

            // Save customer message
            ChatMessage customerMessage = new ChatMessage();
            customerMessage.setChatSession(session);
            customerMessage.setSenderType("CUSTOMER");
            customerMessage.setSenderName(customerName);
            customerMessage.setContent(messageText);
            customerMessage.setMessageType("TEXT");
            customerMessage.setIsread(false);
            customerMessage.setCreatedAt(LocalDateTime.now());
            chatMessageService.saveMessage(customerMessage);

            session.setLastMessageAt(LocalDateTime.now());
            chatSessionService.saveSession(session);

            String currentStatus = String.valueOf(session.getStatus() == null ? "" : session.getStatus()).trim().toUpperCase();
            String currentMode = String.valueOf(session.getChatMode() == null ? "" : session.getChatMode()).trim().toUpperCase();
            boolean humanOwnedSession = "HUMAN".equals(currentMode)
                    || "WAITING_EMPLOYEE".equals(currentStatus)
                    || "IN_PROGRESS".equals(currentStatus);

            if (humanOwnedSession) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Shop đã nhận được tin nhắn của anh/chị. Nhân viên sẽ phản hồi trong cùng khung chat sớm nhất.");
                response.put("mode", "HUMAN");
                response.put("sessionStatus", currentStatus.isBlank() ? "WAITING_EMPLOYEE" : currentStatus);
                response.put("assignedEmployeeName", session.getAssignedEmployeeName());
                response.put("quickReplies", Collections.emptyList());
                response.put("suppressBotMessage", true);
                return ResponseEntity.ok(response);
            }

            // Generate bot response
            Map<String, Object> response = customerBotService.generateBotResponse(messageText, session);

            System.out.println("Bot response keys: " + response.keySet());

            // Save bot response
            if (response.get("message") != null) {
                ChatMessage botMessage = new ChatMessage();
                botMessage.setChatSession(session);
                botMessage.setSenderType("SYSTEM");
                botMessage.setSenderName("DirtyWave Assistant");
                botMessage.setContent((String) response.get("message"));
                botMessage.setMessageType("TEXT");
                botMessage.setIsread(false);
                botMessage.setCreatedAt(LocalDateTime.now());

                if (response.get("quickReplies") != null || response.get("products") != null) {
                    Map<String, Object> metadata = new HashMap<>();
                    if (response.get("quickReplies") != null) metadata.put("quickReplies", response.get("quickReplies"));
                    if (response.get("products") != null) metadata.put("products", response.get("products"));
                    try {
                        botMessage.setMetadataJson(objectMapper.writeValueAsString(metadata));
                    } catch (Exception ignored) {}
                }
                chatMessageService.saveMessage(botMessage);
            }

            System.out.println("=== CHATBOT MESSAGE END ===");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("=== CHATBOT ERROR ===");
            e.printStackTrace();
            return ResponseEntity.ok(quickMsg(
                    "Xin lỗi, em đang gặp sự cố. Anh/chị thử lại sau nhé.",
                    "Bomber đen dưới 700k", "Hoodie size L"));
        }
    }

    @GetMapping("/status/{sessionCode}")
    public ResponseEntity<?> getStatus(@PathVariable String sessionCode) {
        try {
            ChatSession session = chatSessionService.getSessionByCode(sessionCode);
            if (session == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("sessionStatus", "OPEN");
                response.put("chatMode", "AUTO");
                return ResponseEntity.ok(response);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("sessionStatus", session.getStatus());
            response.put("chatMode", session.getChatMode());
            response.put("assignedEmployeeName", session.getAssignedEmployeeName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/history/{sessionCode}")
    public ResponseEntity<?> getHistory(@PathVariable String sessionCode) {
        try {
            ChatSession session = chatSessionService.getSessionByCode(sessionCode);
            if (session == null) return ResponseEntity.ok(Collections.emptyList());

            List<ChatMessage> messages = chatMessageService.getMessagesBySessionId(session.getId());
            List<Map<String, Object>> dtos = messages.stream().map(msg -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", msg.getId());
                dto.put("sessionId", msg.getSessionId());
                dto.put("senderType", msg.getSenderType());
                dto.put("senderName", msg.getSenderName());
                dto.put("content", msg.getContent());
                dto.put("messageType", msg.getMessageType());
                dto.put("isread", msg.getIsread());
                dto.put("createdAt", msg.getCreatedAt());
                dto.put("metadataJson", msg.getMetadataJson());
                return dto;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @PostMapping("/request-human")
    public ResponseEntity<?> requestHuman(@RequestBody Map<String, Object> request) {
        try {
            String sessionCode = (String) request.get("sessionId");
            String customerName = (String) request.get("customerName");
            String customerEmail = (String) request.get("customerEmail");
            String customerPhone = (String) request.get("customerPhone");

            ChatSession session = chatSessionService.getSessionByCode(sessionCode);
            if (session == null) {
                session = new ChatSession();
                session.setSessionCode(sessionCode);
                session.setCustomerName(customerName);
                session.setCustomerEmail(customerEmail);
                session.setCustomerPhone(customerPhone);
                session.setCreatedAt(LocalDateTime.now());
            }

            session.setStatus("WAITING_EMPLOYEE");
            session.setChatMode("HUMAN");
            session.setUpdatedAt(LocalDateTime.now());
            session = chatSessionService.saveSession(session);

            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setChatSession(session);
            systemMessage.setSenderType("SYSTEM");
            systemMessage.setSenderName("System");
            systemMessage.setContent("Khách hàng yêu cầu hỗ trợ từ nhân viên");
            systemMessage.setMessageType("SYSTEM");
            systemMessage.setCreatedAt(LocalDateTime.now());
            Map<String, Object> meta = new HashMap<>();
            meta.put("sessionStatus", "WAITING_EMPLOYEE");
            try { systemMessage.setMetadataJson(objectMapper.writeValueAsString(meta)); } catch (Exception ignored) {}
            chatMessageService.saveMessage(systemMessage);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Shop đã ghi nhận yêu cầu. Nhân viên sẽ tiếp nhận và hỗ trợ anh/chị sớm nhất.");
            response.put("sessionStatus", "WAITING_EMPLOYEE");
            response.put("quickReplies", Collections.emptyList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(quickMsg("Không thể chuyển sang nhân viên lúc này. Vui lòng thử lại."));
        }
    }

    @GetMapping("/products/search")
    @Transactional(readOnly = true)
    public ResponseEntity<?> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Integer maxPrice
    ) {
        return ResponseEntity.ok(doSearchProducts(q, color, maxPrice));
    }

    // ───────── Core private search method ─────────

    private List<Map<String, Object>> doSearchProducts(String q, String color, Integer maxPrice) {
        try {
            List<SanPham> allProducts = sanPhamRepository.findAll();
            List<Map<String, Object>> results = new ArrayList<>();

            String normalizedQ = normalizeVn(q);
            boolean broadOuterwearSearch = normalizedQ.isBlank() || normalizedQ.contains("ao khoac") || normalizedQ.contains("jacket");

            for (SanPham sp : allProducts) {
                if (sp.getTrangThai() == null || !normalizeVn(sp.getTrangThai()).contains("hoat dong")) {
                    continue;
                }

                List<SanPhamChiTiet> variants = sp.getSanPhamChiTiets();
                if (variants == null || variants.isEmpty()) {
                    try {
                        variants = sanPhamChiTietRepository.findBySanPhamId(sp.getId());
                    } catch (Exception e) {
                        System.err.println("Failed to load variants for product " + sp.getId() + ": " + e.getMessage());
                        continue;
                    }
                }

                if (variants == null || variants.isEmpty()) continue;

                variants = variants.stream()
                        .filter(v -> v.getSoLuong() != null && v.getSoLuong() > 0)
                        .filter(v -> v.getGiaBan() != null)
                        .collect(Collectors.toList());

                if (variants.isEmpty()) continue;

                if (!matchesProductQuery(sp, variants, normalizedQ, broadOuterwearSearch)) {
                    continue;
                }

                if (color != null && !color.isBlank()) {
                    String normalizedColor = normalizeVn(color);
                    variants = variants.stream().filter(v -> {
                        try {
                            if (v.getMauSac() == null || v.getMauSac().getTenMau() == null) return false;
                            String tenMau = normalizeVn(v.getMauSac().getTenMau());
                            return tenMau.contains(normalizedColor);
                        } catch (Exception e) {
                            return false;
                        }
                    }).collect(Collectors.toList());

                    if (variants.isEmpty()) continue;
                }

                SanPhamChiTiet cheapestVariant = variants.stream()
                        .min(Comparator.comparing(v -> v.getGiaBan().doubleValue()))
                        .orElse(null);

                if (cheapestVariant == null) continue;

                double minPrice = cheapestVariant.getGiaBan().doubleValue();
                if (maxPrice != null && minPrice > maxPrice) continue;

                results.add(buildProductMap(sp, variants, cheapestVariant, minPrice));
            }

            results.sort((a, b) -> {
                double pa = ((Number) a.getOrDefault("price", 0)).doubleValue();
                double pb = ((Number) b.getOrDefault("price", 0)).doubleValue();
                return Double.compare(pa, pb);
            });

            if (results.size() > 10) {
                results = new ArrayList<>(results.subList(0, 10));
            }

            System.out.println("doSearchProducts(q=" + q + ", color=" + color + ", maxPrice=" + maxPrice + ") => " + results.size() + " products");
            return results;
        } catch (Exception e) {
            System.err.println("doSearchProducts FAILED: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private boolean matchesProductQuery(SanPham sp,
                                        List<SanPhamChiTiet> variants,
                                        String normalizedQ,
                                        boolean broadOuterwearSearch) {
        if (normalizedQ == null || normalizedQ.isBlank()) {
            return true;
        }

        String name = normalizeVn(sp.getTenSanPham());
        String desc = normalizeVn(sp.getMoTa());
        if (name.contains(normalizedQ) || desc.contains(normalizedQ)) {
            return true;
        }

        String variantText = variants.stream().map(v -> String.join(" ",
                        normalizeVn(v.getMa()),
                        normalizeVn(v.getLoai() != null ? v.getLoai().getTenLoai() : ""),
                        normalizeVn(v.getDanhMuc() != null ? v.getDanhMuc().getTenDanhMuc() : ""),
                        normalizeVn(v.getMauSac() != null ? v.getMauSac().getTenMau() : ""),
                        normalizeVn(v.getKichThuoc() != null ? v.getKichThuoc().getTenKichThuoc() : "")))
                .collect(Collectors.joining(" "));

        if (variantText.contains(normalizedQ)) {
            return true;
        }

        if (broadOuterwearSearch) {
            return variantText.contains("ao khoac")
                    || variantText.contains("hoodie")
                    || variantText.contains("bomber")
                    || variantText.contains("coach")
                    || name.contains("hoodie")
                    || name.contains("bomber")
                    || name.contains("coach");
        }

        return false;
    }

    private Map<String, Object> buildProductMap(SanPham sp, List<SanPhamChiTiet> variants, SanPhamChiTiet selectedVariant, double minPrice) {
        Set<String> sizes = new LinkedHashSet<>();
        Set<String> colors = new LinkedHashSet<>();
        int totalStock = 0;

        for (SanPhamChiTiet v : variants) {
            int qty = v.getSoLuong() != null ? v.getSoLuong() : 0;
            totalStock += qty;

            if (qty > 0) {
                try {
                    if (v.getKichThuoc() != null && v.getKichThuoc().getTenKichThuoc() != null) {
                        sizes.add(v.getKichThuoc().getTenKichThuoc());
                    }
                } catch (Exception ignored) {
                }

                try {
                    if (v.getMauSac() != null && v.getMauSac().getTenMau() != null) {
                        colors.add(v.getMauSac().getTenMau());
                    }
                } catch (Exception ignored) {
                }
            }
        }

        String category = "";
        try {
            if (selectedVariant.getDanhMuc() != null) {
                category = selectedVariant.getDanhMuc().getTenDanhMuc();
            }
        } catch (Exception ignored) {
        }

        String defSize = "";
        try {
            if (selectedVariant.getKichThuoc() != null) {
                defSize = selectedVariant.getKichThuoc().getTenKichThuoc();
            }
        } catch (Exception ignored) {
        }

        String defColor = "";
        try {
            if (selectedVariant.getMauSac() != null) {
                defColor = selectedVariant.getMauSac().getTenMau();
            }
        } catch (Exception ignored) {
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", sp.getId());
        m.put("name", sp.getTenSanPham());
        m.put("price", minPrice);
        m.put("stock", totalStock);
        m.put("category", category);
        m.put("summary", sp.getMoTa() != null ? sp.getMoTa() : "");
        m.put("sizes", new ArrayList<>(sizes));
        m.put("colors", new ArrayList<>(colors));
        m.put("image", "/chatbot/fallback/" + sp.getId());
        m.put("defaultVariantId", selectedVariant.getId());
        m.put("defaultSize", defSize);
        m.put("defaultColor", defColor);
        return m;
    }

    // ───────── Bot response generator ─────────

    private Map<String, Object> generateBotResponse(String message, ChatSession session) {
        Map<String, Object> response = new HashMap<>();
        String lower = normalizeVn(message);
        String intent = detectCustomerIntent(lower);

        response.put("intent", intent);
        response.put("grounded", false);
        response.put("confidence", "LOW");

        switch (intent) {
            case "ORDER_LOOKUP" -> {
                List<HoaDon> orders = findMatchingOrders(message, session);
                if (!orders.isEmpty()) {
                    HoaDon first = orders.get(0);
                    String groundedContext = buildOrderGroundedContext(first);
                    String aiText = tryComposeChatbotReply(
                            buildCustomerSystemPrompt("ORDER_LOOKUP"),
                            message,
                            groundedContext
                    );

                    response.put("message", !aiText.isBlank() ? aiText : buildOrderReply(first));
                    response.put("quickReplies", Arrays.asList("Bao giờ giao tới?", "Gặp nhân viên hỗ trợ", "Xem sản phẩm khác"));
                    response.put("grounded", true);
                    response.put("confidence", "HIGH");
                    response.put("matchedOrders", orders.stream().map(this::toOrderPreview).collect(Collectors.toList()));
                    return response;
                }

                response.put("message", "Em chưa tìm thấy đơn khớp với thông tin vừa gửi. Anh/chị cho em mã đơn dạng HD... hoặc đúng số điện thoại đặt hàng để em kiểm tra lại nhé.");
                response.put("quickReplies", Arrays.asList("Gửi mã đơn HD...", "Gặp nhân viên hỗ trợ"));
                response.put("handoffSuggested", true);
                return response;
            }
            case "PRODUCT_STOCK" -> {
                List<Map<String, Object>> matchedVariants = lookupStockVariants(message);
                if (!matchedVariants.isEmpty()) {
                    Map<String, Object> first = matchedVariants.get(0);
                    String groundedContext = "VARIANTS = " + matchedVariants;
                    String aiText = tryComposeChatbotReply(
                            buildCustomerSystemPrompt("PRODUCT_STOCK"),
                            message,
                            groundedContext
                    );
                    response.put("message", !aiText.isBlank() ? aiText : buildVariantStockReply(first, matchedVariants.size()));
                    response.put("quickReplies", Arrays.asList("Xem sản phẩm liên quan", "Tư vấn size", "Gặp nhân viên hỗ trợ"));
                    response.put("grounded", true);
                    response.put("confidence", "HIGH");
                    response.put("variants", matchedVariants);
                    return response;
                }

                response.put("message", "Em chưa thấy đúng biến thể anh/chị đang tìm. Anh/chị thử gửi thêm mã SPCT..., màu hoặc size cụ thể để em kiểm tra lại chuẩn hơn nhé.");
                response.put("quickReplies", Arrays.asList("SPCT001 còn hàng không?", "Bomber đen size M", "Gặp nhân viên hỗ trợ"));
                return response;
            }
            case "PRODUCT_SEARCH" -> {
                String keyword = extractKeyword(lower);
                String color = extractColor(lower);
                Integer maxPrice = extractMaxPrice(lower);
                Integer requestedCount = extractRequestedCount(lower);

                List<Map<String, Object>> products = doSearchProducts(keyword, color, maxPrice);
                if (!products.isEmpty()) {
                    if (requestedCount != null && requestedCount > 0 && products.size() > requestedCount) {
                        products = new ArrayList<>(products.subList(0, requestedCount));
                    }
                    String groundedContext = "PRODUCTS = " + products;
                    String aiText = tryComposeChatbotReply(
                            buildCustomerSystemPrompt("PRODUCT_SEARCH"),
                            message,
                            groundedContext
                    );
                    if (aiText.isBlank()) {
                        String desc = keyword == null || keyword.isBlank() ? "sản phẩm" : keyword;
                        String colorDesc = color != null ? " màu " + color : "";
                        String priceDesc = maxPrice != null ? " dưới " + (maxPrice / 1000) + "k" : "";
                        aiText = String.format("Em tìm được %d %s%s%s phù hợp cho anh/chị:", products.size(), desc, colorDesc, priceDesc);
                    }
                    response.put("message", aiText);
                    response.put("products", products);
                    response.put("quickReplies", Arrays.asList("Xem mẫu rẻ hơn", "Tư vấn size", "Gặp nhân viên hỗ trợ"));
                    response.put("grounded", true);
                    response.put("confidence", "HIGH");
                    return response;
                }

                response.put("message", "Em chưa tìm thấy mẫu khớp hoàn toàn. Anh/chị thử đổi màu, tăng ngân sách hoặc nhắn loại áo cụ thể như bomber / hoodie / coach nhé.");
                response.put("quickReplies", Arrays.asList("Bomber đen", "Hoodie dưới 600k", "Xem tất cả"));
                return response;
            }
            case "PRICE_RANGE" -> {
                Map<String, Object> priceInfo = buildLivePriceInfo();
                response.put("message", String.format(
                        "Giá sản phẩm của shop hiện dao động khoảng %s đến %s. Nếu anh/chị muốn, em có thể lọc ngay theo khoảng giá và loại áo.",
                        formatMoney(priceInfo.get("minPrice")),
                        formatMoney(priceInfo.get("maxPrice"))
                ));
                response.put("quickReplies", Arrays.asList("Bomber dưới 600k", "Hoodie dưới 500k", "Xem tất cả"));
                response.put("grounded", true);
                response.put("confidence", "MEDIUM");
                return response;
            }
            case "PROMOTION" -> {
                List<Map<String, Object>> vouchers = getActiveVoucherPreview();
                String groundedContext = "VOUCHERS = " + vouchers;
                String aiText = tryComposeChatbotReply(buildCustomerSystemPrompt("PROMOTION"), message, groundedContext);
                if (aiText.isBlank()) {
                    aiText = buildPromotionReply(vouchers);
                }
                response.put("message", aiText);
                response.put("quickReplies", Arrays.asList("Voucher đơn từ 500k", "Sản phẩm dưới 600k", "Gặp nhân viên hỗ trợ"));
                response.put("grounded", !vouchers.isEmpty());
                response.put("confidence", vouchers.isEmpty() ? "LOW" : "MEDIUM");
                return response;
            }
            case "SHIPPING" -> {
                response.put("message", "Shop hỗ trợ giao hàng toàn quốc. Nội thành Hà Nội khoảng 30k, các tỉnh khác khoảng 50k và có thể thay đổi theo khu vực. Nếu anh/chị đã có đơn cụ thể, em có thể kiểm tra thêm trạng thái giao hàng hoặc chuyển nhân viên hỗ trợ.");
                response.put("quickReplies", Arrays.asList("Kiểm tra đơn hàng", "Xem sản phẩm", "Gặp nhân viên hỗ trợ"));
                response.put("confidence", "MEDIUM");
                return response;
            }
            case "SIZE_GUIDE" -> {
                response.put("message", "Bảng size tham khảo DirtyWave: S khoảng 45-55kg, M khoảng 55-65kg, L khoảng 65-75kg, XL khoảng 75-85kg. Anh/chị gửi giúp em chiều cao và cân nặng, em sẽ gợi ý size sát hơn.");
                response.put("quickReplies", Arrays.asList("Mình cao 1m7 nặng 65kg", "Xem hoodie", "Xem bomber"));
                response.put("confidence", "MEDIUM");
                return response;
            }
            case "POLICY" -> {
                response.put("message", "Shop hỗ trợ đổi size trong 7 ngày nếu sản phẩm còn nguyên tag và chưa qua sử dụng. Với các trường hợp lỗi từ nhà sản xuất hoặc cần xử lý đặc biệt, em khuyên chuyển nhân viên xác nhận để tránh cam kết sai.");
                response.put("quickReplies", Arrays.asList("Gặp nhân viên hỗ trợ", "Kiểm tra đơn hàng"));
                response.put("handoffSuggested", true);
                response.put("confidence", "MEDIUM");
                return response;
            }
            case "GREETING" -> {
                response.put("message", "Xin chào anh/chị! Em là trợ lý DirtyWave. Em có thể giúp tìm áo khoác, kiểm tra đơn hàng, tư vấn size hoặc thông tin voucher. Anh/chị cần em hỗ trợ gì ạ?");
                response.put("quickReplies", Arrays.asList("Bomber đen dưới 700k", "Kiểm tra đơn hàng", "Tư vấn size", "Voucher hiện có"));
                response.put("confidence", "MEDIUM");
                return response;
            }
            default -> {
                response.put("message", "Em có thể hỗ trợ tìm sản phẩm, kiểm tra đơn hàng, tư vấn size, voucher và thông tin giao hàng. Anh/chị thử nhắn cụ thể như ‘bomber đen dưới 700k’, ‘SPCT001 còn hàng không?’ hoặc ‘kiểm tra đơn HD...’ nhé.");
                response.put("quickReplies", Arrays.asList("Xem tất cả", "Kiểm tra đơn hàng", "SPCT001 còn hàng không?", "Gặp nhân viên hỗ trợ"));
                return response;
            }
        }
    }

    private String detectCustomerIntent(String normalizedMsg) {
        if (normalizedMsg.contains("xin chao") || normalizedMsg.contains("hello") || normalizedMsg.equals("hi") || normalizedMsg.equals("hey")) {
            return "GREETING";
        }
        if (normalizedMsg.matches(".*hd[0-9a-z]+.*") || normalizedMsg.contains("đơn hàng") || normalizedMsg.contains("hoa don") || normalizedMsg.contains("hóa đơn") || normalizedMsg.contains("kiểm tra đơn")) {
            return "ORDER_LOOKUP";
        }
        if (normalizedMsg.matches(".*spct[0-9a-z]+.*")
                || normalizedMsg.contains("còn hàng")
                || normalizedMsg.contains("con hang")
                || normalizedMsg.contains("tồn kho")
                || normalizedMsg.contains("ton kho")) {
            return "PRODUCT_STOCK";
        }
        if (normalizedMsg.contains("voucher") || normalizedMsg.contains("khuyen mai") || normalizedMsg.contains("khuyến mãi") || normalizedMsg.contains("giam gia") || normalizedMsg.contains("giảm giá")) {
            return "PROMOTION";
        }
        if ((normalizedMsg.contains("size") || normalizedMsg.contains("kich thuoc") || normalizedMsg.contains("kích thước") || normalizedMsg.contains("tu van size"))
                && !isProductSearch(normalizedMsg)) {
            return "SIZE_GUIDE";
        }
        if (normalizedMsg.contains("ship") || normalizedMsg.contains("giao hang") || normalizedMsg.contains("vận chuyển") || normalizedMsg.contains("van chuyen") || normalizedMsg.contains("phi ship")) {
            return "SHIPPING";
        }
        if (normalizedMsg.contains("doi") || normalizedMsg.contains("tra") || normalizedMsg.contains("bao hanh") || normalizedMsg.contains("chính sách") || normalizedMsg.contains("chinh sach")) {
            return "POLICY";
        }
        if (normalizedMsg.contains("gia") || normalizedMsg.contains("bao nhieu") || normalizedMsg.contains("duoi ")) {
            if (isProductSearch(normalizedMsg)) {
                return "PRODUCT_SEARCH";
            }
            return "PRICE_RANGE";
        }
        if (isProductSearch(normalizedMsg)) {
            return "PRODUCT_SEARCH";
        }
        return "GENERAL_QA";
    }

    private List<HoaDon> findMatchingOrders(String message, ChatSession session) {
        String lower = normalizeVn(message);
        String explicitCode = extractOrderCode(lower, message);
        String phone = session != null ? String.valueOf(session.getCustomerPhone() == null ? "" : session.getCustomerPhone()).trim() : "";
        String email = session != null ? String.valueOf(session.getCustomerEmail() == null ? "" : session.getCustomerEmail()).trim().toLowerCase(Locale.ROOT) : "";
        String customerName = session != null ? normalizeVn(session.getCustomerName()) : "";

        return hoaDonRepository.findAll().stream()
                .filter(hd -> matchesOrder(hd, explicitCode, phone, email, customerName))
                .sorted(Comparator.comparing(HoaDon::getNgayTao, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    private boolean matchesOrder(HoaDon hd, String explicitCode, String phone, String email, String customerName) {
        String ma = normalizeVn(hd.getMaHoaDon());
        String hdPhone = String.valueOf(hd.getSoDienThoaiNhanHang() == null ? "" : hd.getSoDienThoaiNhanHang()).trim();
        String hdName = normalizeVn(hd.getTenKhachHang());

        if (explicitCode != null && !explicitCode.isBlank()) {
            return ma.contains(normalizeVn(explicitCode));
        }
        if (!phone.isBlank() && hdPhone.contains(phone)) {
            return true;
        }
        if (!customerName.isBlank() && hdName.contains(customerName)) {
            return true;
        }
        return !email.isBlank() && hdName.contains(email);
    }

    private String extractOrderCode(String normalized, String raw) {
        int hdIndex = normalized.indexOf("hd");
        if (hdIndex >= 0) {
            return raw.substring(hdIndex).split("\\s+")[0].trim();
        }
        return "";
    }

    private String buildOrderReply(HoaDon order) {
        return String.format(
                "Em kiểm tra giúp anh/chị: đơn %s hiện đang ở trạng thái %s. Người nhận là %s, số điện thoại %s. Nếu anh/chị cần em chuyển nhân viên hỗ trợ thêm về giao hàng hoặc xử lý đặc biệt, em hỗ trợ ngay ạ.",
                order.getMaHoaDon(),
                nullableText(order.getTrangThai(), "không rõ"),
                nullableText(order.getTenKhachHang(), "không rõ"),
                nullableText(order.getSoDienThoaiNhanHang(), "không rõ")
        );
    }

    private String buildOrderGroundedContext(HoaDon order) {
        return "ORDER = {maHoaDon=" + order.getMaHoaDon()
                + ", trangThai=" + order.getTrangThai()
                + ", tenKhachHang=" + order.getTenKhachHang()
                + ", soDienThoaiNhanHang=" + order.getSoDienThoaiNhanHang()
                + ", ngayNhanHangDuKien=" + order.getNgayNhanHangDuKien()
                + ", thanhTien=" + order.getThanhTien()
                + ", phuongThucThanhToan=" + order.getPhuongThucThanhToan() + "}";
    }

    private Map<String, Object> toOrderPreview(HoaDon order) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("maHoaDon", order.getMaHoaDon());
        row.put("trangThai", order.getTrangThai());
        row.put("ngayTao", order.getNgayTao());
        row.put("thanhTien", order.getThanhTien());
        return row;
    }

    private List<Map<String, Object>> lookupStockVariants(String message) {
        String raw = message == null ? "" : message.trim();
        String normalized = normalizeVn(raw);
        String variantCode = extractVariantCode(raw);
        String productCode = extractProductCode(raw);
        String color = extractColor(normalized);
        String size = extractSize(normalized);
        String category = extractCategory(normalized);

        return sanPhamChiTietRepository.findAll().stream()
                .filter(spct -> matchesVariant(spct, variantCode, productCode, color, size, category, normalized))
                .sorted(Comparator.comparing((SanPhamChiTiet spct) -> spct.getSoLuong() == null ? 0 : spct.getSoLuong()).reversed())
                .limit(10)
                .map(this::toVariantPreview)
                .collect(Collectors.toList());
    }

    private boolean matchesVariant(SanPhamChiTiet spct,
                                   String variantCode,
                                   String productCode,
                                   String color,
                                   String size,
                                   String category,
                                   String normalizedMessage) {
        String maSpct = normalizeVn(spct.getMa());
        String maSp = normalizeVn(spct.getSanPham() != null ? spct.getSanPham().getMaSanPham() : "");
        String tenSp = normalizeVn(spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "");
        String mau = normalizeVn(spct.getMauSac() != null ? spct.getMauSac().getTenMau() : "");
        String kichThuoc = normalizeVn(spct.getKichThuoc() != null ? spct.getKichThuoc().getTenKichThuoc() : "");
        String loai = normalizeVn(spct.getLoai() != null ? spct.getLoai().getTenLoai() : "");
        String danhMuc = normalizeVn(spct.getDanhMuc() != null ? spct.getDanhMuc().getTenDanhMuc() : "");

        if (!variantCode.isBlank()) {
            return maSpct.equals(normalizeVn(variantCode));
        }
        if (!productCode.isBlank() && !maSp.equals(normalizeVn(productCode))) {
            return false;
        }
        if (color != null && !color.isBlank() && !mau.contains(normalizeVn(color))) {
            return false;
        }
        if (size != null && !size.isBlank() && !kichThuoc.equals(normalizeVn(size))) {
            return false;
        }
        if (category != null && !category.isBlank()) {
            String normalizedCategory = normalizeVn(category);
            if (!(tenSp.contains(normalizedCategory) || loai.contains(normalizedCategory) || danhMuc.contains(normalizedCategory))) {
                return false;
            }
        }

        if (productCode.isBlank() && (color == null || color.isBlank()) && (size == null || size.isBlank()) && (category == null || category.isBlank())) {
            return tenSp.contains(normalizedMessage) || maSp.contains(normalizedMessage) || maSpct.contains(normalizedMessage);
        }
        return true;
    }

    private Map<String, Object> toVariantPreview(SanPhamChiTiet spct) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("ma", spct.getMa());
        row.put("maSanPham", spct.getSanPham() == null ? null : spct.getSanPham().getMaSanPham());
        row.put("tenSanPham", spct.getSanPham() == null ? null : spct.getSanPham().getTenSanPham());
        row.put("soLuong", spct.getSoLuong());
        row.put("giaBan", spct.getGiaBan());
        row.put("mauSac", spct.getMauSac() != null ? spct.getMauSac().getTenMau() : null);
        row.put("kichThuoc", spct.getKichThuoc() != null ? spct.getKichThuoc().getTenKichThuoc() : null);
        row.put("loai", spct.getLoai() != null ? spct.getLoai().getTenLoai() : null);
        return row;
    }

    private String buildVariantStockReply(Map<String, Object> variant, int totalMatches) {
        String base = "Em kiểm tra giúp anh/chị: " + nullableText((String) variant.get("tenSanPham"), "mẫu này")
                + " - mã " + nullableText((String) variant.get("ma"), "không rõ")
                + ", màu " + nullableText((String) variant.get("mauSac"), "không rõ")
                + ", size " + nullableText((String) variant.get("kichThuoc"), "không rõ")
                + " hiện còn " + String.valueOf(variant.getOrDefault("soLuong", 0)) + " sản phẩm.";
        if (totalMatches > 1) {
            base += " Em cũng thấy thêm " + (totalMatches - 1) + " biến thể gần giống để anh/chị tham khảo.";
        }
        return base;
    }

    private Map<String, Object> buildLivePriceInfo() {
        double min = sanPhamChiTietRepository.findAll().stream()
                .map(SanPhamChiTiet::getGiaBan)
                .filter(Objects::nonNull)
                .mapToDouble(v -> v.doubleValue())
                .min().orElse(0);
        double max = sanPhamChiTietRepository.findAll().stream()
                .map(SanPhamChiTiet::getGiaBan)
                .filter(Objects::nonNull)
                .mapToDouble(v -> v.doubleValue())
                .max().orElse(0);
        Map<String, Object> info = new HashMap<>();
        info.put("minPrice", min);
        info.put("maxPrice", max);
        return info;
    }

    private List<Map<String, Object>> getActiveVoucherPreview() {
        LocalDate today = LocalDate.now();
        return phieuGiamGiaRepository.findAll().stream()
                .filter(v -> v.getNgayBatDau() == null || !v.getNgayBatDau().isAfter(today))
                .filter(v -> v.getNgayKetThuc() == null || !v.getNgayKetThuc().isBefore(today))
                .limit(5)
                .map(v -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("ma", v.getMaPhieuGiamGia());
                    row.put("ten", v.getTenPhieuGiamGia());
                    row.put("giaTri", v.getGiaTriGiamGia());
                    row.put("hinhThuc", Boolean.TRUE.equals(v.getHinhThucGiam()) ? "%" : "VND");
                    row.put("hoaDonToiThieu", v.getHoaDonToiThieu());
                    return row;
                })
                .collect(Collectors.toList());
    }

    private String buildPromotionReply(List<Map<String, Object>> vouchers) {
        if (vouchers.isEmpty()) {
            return "Hiện em chưa thấy voucher khả dụng trong hệ thống. Nếu anh/chị cần em có thể chuyển nhân viên kiểm tra chương trình áp dụng cụ thể cho đơn của mình.";
        }
        Map<String, Object> first = vouchers.get(0);
        return "Hiện shop đang có một số ưu đãi khả dụng. Ví dụ: “" + first.get("ten") + "” với giá trị "
                + first.get("giaTri") + " " + first.get("hinhThuc")
                + ". Nếu anh/chị muốn, em có thể gợi ý sản phẩm phù hợp với mức đơn để áp voucher dễ hơn.";
    }

    private String buildCustomerSystemPrompt(String intent) {
        return """
                Bạn là chatbot bán hàng cho khách mua DirtyWave.
                Intent hiện tại: %s.
                Chỉ được dùng dữ liệu thực tế đã cung cấp.
                Trả lời ngắn gọn, lịch sự, dễ hiểu, không bịa chính sách và không cam kết quá mức.
                Nếu dữ liệu chưa đủ, yêu cầu khách cung cấp thêm mã đơn hoặc thông tin cần thiết.
                """.formatted(intent);
    }

    private String tryComposeChatbotReply(String systemPrompt, String userMessage, String groundedContext) {
        String aiText = openAiGateway.askChatbot(systemPrompt, userMessage, groundedContext);
        return aiText == null ? "" : aiText.trim();
    }

    private String formatMoney(Object value) {
        if (value == null) return "0đ";
        try {
            double amount = value instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(value));
            return String.format("%,.0fđ", amount).replace(',', '.');
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String nullableText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isProductSearch(String normalizedMsg) {
        String[] triggers = {
                "bomber", "hoodie", "coach", "jacket", "khoac", "ao khoac",
                "ao", "san pham", "mau", "duoi", "tren",
                "xem mau", "tim", "gia re", "goi y"
        };
        for (String t : triggers) {
            if (normalizedMsg.contains(t)) return true;
        }
        return false;
    }

    private String extractKeyword(String normalizedMsg) {
        for (String[] group : CATEGORY_KEYWORDS) {
            for (String kw : group) {
                if (normalizedMsg.contains(normalizeVn(kw))) return group[0];
            }
        }
        return normalizedMsg.contains("ao khoac") ? "áo khoác" : "";
    }

    private String extractColor(String normalizedMsg) {
        for (int i = 0; i < COLOR_MAP.length; i++) {
            for (String alias : COLOR_MAP[i]) {
                if (normalizedMsg.contains(normalizeVn(alias))) return COLOR_DISPLAY[i];
            }
        }
        return null;
    }

    private String extractSize(String normalizedMsg) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(xx?l|3xl|4xl|5xl|xs|s|m|l|xl)\\b", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(normalizedMsg);
        return m.find() ? m.group().toUpperCase(java.util.Locale.ROOT) : "";
    }

    private String extractCategory(String normalizedMsg) {
        if (normalizedMsg.contains("bomber")) return "bomber";
        if (normalizedMsg.contains("hoodie")) return "hoodie";
        if (normalizedMsg.contains("coach")) return "coach";
        if (normalizedMsg.contains("ao khoac") || normalizedMsg.contains("jacket")) return "áo khoác";
        return "";
    }

    private String extractVariantCode(String raw) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)\\bSPCT\\d+\\b").matcher(raw == null ? "" : raw);
        return m.find() ? m.group().trim() : "";
    }

    private String extractProductCode(String raw) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)\\bSP\\d+\\b").matcher(raw == null ? "" : raw);
        return m.find() ? m.group().trim() : "";
    }

    private Integer extractMaxPrice(String normalizedMsg) {
        if (normalizedMsg.contains("duoi") || normalizedMsg.contains("toi da") || normalizedMsg.contains("re")) {
            String[] parts = normalizedMsg.split("\\s+");
            for (String part : parts) {
                if (part.endsWith("k") || part.endsWith("K")) {
                    try {
                        String numStr = part.replaceAll("[^0-9]", "");
                        if (!numStr.isEmpty()) return Integer.parseInt(numStr) * 1000;
                    } catch (Exception ignored) {}
                }
            }
        }
        // Also check for standalone patterns like "500k", "700k" near price words
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{2,4})k").matcher(normalizedMsg);
        if (m.find() && (normalizedMsg.contains("duoi") || normalizedMsg.contains("toi da") || normalizedMsg.contains("re hon"))) {
            try { return Integer.parseInt(m.group(1)) * 1000; } catch (Exception ignored) {}
        }
        return null;
    }

    // ───────── Vietnamese normalizer ─────────

    private String normalizeVn(String text) {
        if (text == null) return "";
        String s = text.toLowerCase().trim();
        s = s.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        s = s.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        s = s.replaceAll("[ìíịỉĩ]", "i");
        s = s.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        s = s.replaceAll("[ùúụủũưừứựửữ]", "u");
        s = s.replaceAll("[ỳýỵỷỹ]", "y");
        s = s.replaceAll("[đ]", "d");
        return s;
    }

    // ───────── Utility ─────────

    private Map<String, Object> quickMsg(String message, String... replies) {
        Map<String, Object> m = new HashMap<>();
        m.put("message", message);
        m.put("quickReplies", replies.length > 0 ? Arrays.asList(replies) : Collections.emptyList());
        return m;
    }

    private Integer extractRequestedCount(String normalizedMsg) {
        if (normalizedMsg == null || normalizedMsg.isBlank()) return null;

        if (normalizedMsg.contains("1 san pham")
                || normalizedMsg.contains("mot san pham")
                || normalizedMsg.contains("1 mau")
                || normalizedMsg.contains("mot mau")) {
            return 1;
        }

        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("(\\d+)\\s*(san pham|mau|ao)").matcher(normalizedMsg);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
