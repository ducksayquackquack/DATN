package org.example.datnnhom03.chatbot.service;

import org.example.datnnhom03.Model.ChatSession;
import org.example.datnnhom03.assistant.service.OpenAiGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CustomerBotService {

    private final CustomerSemanticRouterService routerService;
    private final CustomerToolService toolService;
    private final OpenAiGateway openAiGateway;

    public CustomerBotService(CustomerSemanticRouterService routerService,
                              CustomerToolService toolService,
                              OpenAiGateway openAiGateway) {
        this.routerService = routerService;
        this.toolService = toolService;
        this.openAiGateway = openAiGateway;
    }

    public Map<String, Object> generateBotResponse(String message, ChatSession session) {
        CustomerIntentResult routed = routerService.route(message);
        Map<String, Object> entities = routed.getEntities() == null
                ? new HashMap<>()
                : routed.getEntities();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("intent", routed.getIntent());
        response.put("entities", entities);
        response.put("confidence", routed.getConfidence());
        response.put("grounded", false);

        if (isOrderCodePlaceholderMessage(message)) {
            response.put("message", "Anh/chị vui lòng nhập đúng mã đơn theo định dạng, ví dụ HD20260401001 hoặc HĐ20260401001, để em kiểm tra chính xác đơn hàng nhé.");
            response.put("quickReplies", List.of("Số điện thoại đặt hàng", "Gặp nhân viên hỗ trợ"));
            response.put("handoffSuggested", false);
            return response;
        }

        boolean canLookupOrderBySessionPhone =
                "ORDER_LOOKUP".equals(routed.getIntent()) && hasSessionPhone(session);

        if (routed.isClarificationNeeded() && !canLookupOrderBySessionPhone) {
            response.put("message", buildClarificationMessage(routed));
            response.put("quickReplies", clarificationReplies(routed.getIntent()));
            response.put("handoffSuggested", false);
            return response;
        }

        switch (routed.getIntent()) {
            case "GREETING" -> {
                String customerName = session == null ? "" : nullSafe(session.getCustomerName());
                String greetingPrefix = customerName.isBlank() ? "Xin chào anh/chị!" : "Xin chào " + customerName + "!";
                response.put("message", greetingPrefix + " Em là trợ lý DirtyWave. Em có thể giúp tìm sản phẩm, kiểm tra tồn kho, xem đơn hàng, tư vấn size và voucher hiện có.");
                response.put("quickReplies", List.of("Bomber đen dưới 700k", "SPCT001 còn hàng không?", "Tư vấn size", "Voucher hiện có"));
                return response;
            }
            case "ORDER_LOOKUP" -> {
                List<Map<String, Object>> orders = toolService.findOrders(entities, session);
                if (orders.isEmpty()) {
                    boolean hasOrderCode = hasLookupValue(entities.get("orderCode"));
                    boolean hasPhone = hasLookupValue(entities.get("phone")) || hasSessionPhone(session);
                    if (hasOrderCode) {
                        response.put("message", "Hiện tại quý khách chưa có đơn hàng nào đang được đặt mua, đóng gói hoặc vận chuyển.");
                        response.put("quickReplies", List.of("Xem sản phẩm", "Bomber đen dưới 700k", "Gặp nhân viên hỗ trợ"));
                    } else if (hasPhone) {
                        response.put("message", "Hiện tại quý khách chưa có đơn hàng nào đang được đặt mua, đóng gói hoặc vận chuyển.");
                        response.put("quickReplies", List.of("Xem sản phẩm", "Bomber đen dưới 700k", "Gặp nhân viên hỗ trợ"));
                    } else {
                        response.put("message", "Anh/chị gửi giúp em mã đơn dạng HD... hoặc số điện thoại đặt hàng để em kiểm tra đúng đơn nhé.");
                        response.put("quickReplies", List.of("Gửi mã đơn HD...", "Gửi số điện thoại đặt hàng", "Gặp nhân viên hỗ trợ"));
                    }
                    response.put("handoffSuggested", hasOrderCode || hasPhone);
                    return response;
                }
                response.put("matchedOrders", orders);
                response.put("message", buildOrderAwareReply(message, orders.get(0)));
                response.put("quickReplies", nextOrderQuickReplies(orders.get(0), message));
                response.put("grounded", true);
                return response;
            }
            case "PRODUCT_STOCK" -> {
                List<Map<String, Object>> variants = toolService.lookupStock(entities);
                if (variants.isEmpty()) {
                    response.put("message", "Hiện shop chưa có sản phẩm phù hợp tương ứng hoặc sản phẩm này đang không được kinh doanh. Anh/chị thử gửi mã SP/SPCT khác, màu hoặc size cụ thể để em kiểm tra lại nhé.");
                    response.put("quickReplies", List.of("SPCT001 còn hàng không?", "Bomber đen size M", "Gặp nhân viên hỗ trợ"));
                    return response;
                }
                response.put("variants", variants);
                response.put("message", composeWithAi(
                        "PRODUCT_STOCK",
                        message,
                        sessionContext(session) + "\nVARIANTS = " + variants,
                        buildStockReply(variants)
                ));
                response.put("quickReplies", nextVariantQuickReplies(variants));
                response.put("grounded", true);
                return response;
            }
            case "BEST_SELLER_QUERY" -> {
                List<Map<String, Object>> items = toolService.getBestSellers(5);
                response.put("products", items);
                response.put("message", composeWithAi(
                        "BEST_SELLER_QUERY",
                        message,
                        sessionContext(session) + "\nBEST_SELLERS = " + items,
                        buildBestSellerReply(items)
                ));
                response.put("quickReplies", List.of("Gợi ý 3 mẫu dễ mua", "Có gì đang sale?", "Bomber đen dưới 700k"));
                response.put("grounded", !items.isEmpty());
                return response;
            }
            case "PROMOTION_QUERY" -> {
                String productCode = stringValue(entities.get("productCode"));

                if (productCode.isBlank()) {
                    productCode = extractCode(message, "(?i)\\bSP\\d+\\b");
                }

                List<Map<String, Object>> promotions;
                List<Map<String, Object>> vouchers;
                List<Map<String, Object>> promotedProducts;

                if (!productCode.isBlank()) {
                    promotions = toolService.getPromotionsByProductCode(productCode);
                    promotedProducts = List.of();
                } else {
                    promotions = toolService.getActivePromotions();
                    promotedProducts = toolService.getPromotedProducts(10);
                }

                vouchers = toolService.getActiveVouchers();

                response.put("promotions", promotions);
                response.put("vouchers", vouchers);

                // BẮT BUỘC có dòng này thì frontend mới hiện card
                response.put("products", promotedProducts);

                response.put("message", buildPromotionReplyForProductOrList(productCode, promotions, vouchers, promotedProducts));
                response.put("quickReplies", List.of("Sản phẩm dưới 600k", "Bomber đang sale", "Gặp nhân viên hỗ trợ"));
                response.put("grounded", !promotions.isEmpty() || !vouchers.isEmpty() || !promotedProducts.isEmpty());

                return response;
            }
            case "PAYMENT_QUERY" -> {
                List<Map<String, Object>> methods = toolService.getPaymentMethods();
                response.put("paymentMethods", methods);
                response.put("message", composeWithAi(
                        "PAYMENT_QUERY",
                        message,
                        sessionContext(session) + "\nPAYMENT_METHODS = " + methods,
                        buildPaymentReply(methods)
                ));
                response.put("quickReplies", List.of("Có COD không?", "Kiểm tra đơn hàng", "Xem sản phẩm"));
                response.put("grounded", !methods.isEmpty());
                return response;
            }
            case "SIZE_ADVICE" -> {
                Map<String, Object> sizeAdvice = toolService.adviseSize(entities);
                response.put("sizeAdvice", sizeAdvice);
                response.put("message", String.valueOf(sizeAdvice.getOrDefault("summary", "Anh/chị gửi giúp em chiều cao và cân nặng để em tư vấn size sát hơn nhé.")));
                response.put("quickReplies", List.of("Mình cao 1m70 nặng 65kg", "Form rộng thì sao?", "Xem hoodie"));
                response.put("grounded", Boolean.TRUE.equals(sizeAdvice.get("found")));
                return response;
            }
            case "PRODUCT_RECOMMENDATION", "PRODUCT_SEARCH" -> {
                List<Map<String, Object>> products = toolService.searchProducts(entities);
                if (products.isEmpty()) {
                    response.put("message", "Em chưa tìm thấy mẫu khớp hoàn toàn. Anh/chị thử đổi màu, tăng ngân sách hoặc nhắn loại áo cụ thể như bomber / hoodie / coach nhé.");
                    response.put("quickReplies", List.of("Bomber đen", "Hoodie dưới 600k", "Xem tất cả"));
                    return response;
                }
                response.put("products", products);
                response.put("message", composeWithAi(
                        routed.getIntent(),
                        message,
                        sessionContext(session) + "\nPRODUCTS = " + products,
                        buildRecommendationReply(products, entities)
                ));
                response.put("quickReplies", nextRecommendationQuickReplies(products, entities));
                response.put("grounded", true);
                return response;
            }
            case "POLICY_QUERY" -> {
                response.put("message", "Hiện em chưa có nguồn dữ liệu chính thức đủ chắc để trả lời toàn bộ chính sách đổi trả, hoàn tiền hay bảo hành. Anh/chị cho em chuyển nhân viên hỗ trợ để tránh cam kết sai nhé.");
                response.put("quickReplies", List.of("Gặp nhân viên hỗ trợ", "Kiểm tra đơn hàng"));
                response.put("handoffSuggested", true);
                return response;
            }
            case "SHOP_INFO_QUERY" -> {
                response.put("message", "Một số thông tin như cửa hàng offline, giờ làm việc, hotline hay hóa đơn cần bám theo cấu hình chính thức của shop. Hiện em chưa có nguồn xác thực đủ chắc nên em xin phép chuyển nhân viên hỗ trợ anh/chị nhé.");
                response.put("quickReplies", List.of("Gặp nhân viên hỗ trợ", "Xem sản phẩm", "Kiểm tra đơn hàng"));
                response.put("handoffSuggested", true);
                return response;
            }
            case "SHIPPING_QUERY" -> {
                List<Map<String, Object>> orders = toolService.findOrders(entities, session);
                if (!orders.isEmpty()) {
                    response.put("matchedOrders", orders);
                    response.put("message", buildOrderAwareReply(message, orders.get(0)));
                    response.put("quickReplies", nextOrderQuickReplies(orders.get(0), message));
                    response.put("grounded", true);
                    return response;
                }
                response.put("message", hasSessionPhone(session) || hasLookupValue(entities.get("orderCode")) || hasLookupValue(entities.get("phone"))
                        ? "Hiện tại quý khách chưa có đơn hàng nào đang được đặt mua, đóng gói hoặc vận chuyển."
                        : "Shop có hỗ trợ giao hàng. Nếu anh/chị gửi giúp em mã đơn HD... hoặc số điện thoại đặt hàng, em sẽ kiểm tra trạng thái hiện tại và dự kiến giao sát hơn nhé.");
                response.put("quickReplies", List.of("Kiểm tra đơn HD...", "Có COD không?", "Gặp nhân viên hỗ trợ"));
                return response;
            }
            default -> {
                response.put("message", "Em có thể hỗ trợ tìm sản phẩm, kiểm tra đơn hàng, tư vấn size, voucher và tồn kho. Anh/chị thử nhắn cụ thể như ‘bomber đen dưới 700k’, ‘SPCT001 còn hàng không?’ hoặc ‘kiểm tra đơn HD...’ nhé.");
                response.put("quickReplies", List.of("Xem tất cả", "Kiểm tra đơn hàng", "SPCT001 còn hàng không?", "Tư vấn size"));
                return response;
            }
        }
    }

    private String buildClarificationMessage(CustomerIntentResult routed) {
        return switch (routed.getIntent()) {
            case "ORDER_LOOKUP" -> "Anh/chị gửi giúp em mã đơn dạng HD... hoặc số điện thoại đặt hàng để em kiểm tra đúng đơn nhé.";
            case "PRODUCT_STOCK" -> "Anh/chị cho em thêm mã SP/SPCT hoặc loại áo, màu, size cụ thể để em kiểm tra đúng tồn kho nhé.";
            case "SIZE_ADVICE" -> "Anh/chị gửi giúp em chiều cao và cân nặng, ví dụ 1m70 65kg, để em gợi ý size sát hơn nhé.";
            default -> "Anh/chị cho em thêm một chút thông tin để em hỗ trợ đúng hơn nhé.";
        };
    }

    private List<String> clarificationReplies(String intent) {
        return switch (intent) {
            case "ORDER_LOOKUP" -> List.of("Mã đơn HD...", "Số điện thoại đặt hàng", "Gặp nhân viên hỗ trợ");
            case "PRODUCT_STOCK" -> List.of("SPCT001 còn hàng không?", "Bomber đen size M", "Gặp nhân viên hỗ trợ");
            case "SIZE_ADVICE" -> List.of("1m70 65kg", "1m68 62kg", "Form rộng thì sao?");
            default -> List.of("Xem sản phẩm", "Kiểm tra đơn hàng", "Tư vấn size");
        };
    }

    private String buildOrderAwareReply(String userMessage, Map<String, Object> order) {
        String normalized = normalize(userMessage);
        boolean askShippingFee = asksShippingFee(normalized);
        boolean askGrandTotal = asksGrandTotal(normalized);
        boolean askDeliveryEta = asksDeliveryEta(normalized);

        if (askShippingFee && askGrandTotal) {
            return buildShippingAndTotalReply(order);
        }
        if (askShippingFee) {
            return buildShippingFeeReply(order);
        }
        if (askGrandTotal) {
            return buildGrandTotalReply(order);
        }
        if (askDeliveryEta) {
            return composeWithAi(
                    "SHIPPING_QUERY",
                    userMessage,
                    "ORDER = " + order,
                    buildShippingReply(order)
            );
        }

        String fallback = buildOrderReply(order);
        return composeWithAi(
                "ORDER_LOOKUP",
                userMessage,
                "ORDER = " + order,
                fallback
        );
    }

    private String buildOrderReply(Map<String, Object> order) {
        StringBuilder reply = new StringBuilder("Em kiểm tra giúp anh/chị: đơn ")
                .append(order.get("maHoaDon"))
                .append(" hiện đang ở trạng thái ")
                .append(nullSafe(order.get("trangThai")));
        if (!nullSafe(order.get("ngayNhanHangDuKien")).isBlank()) {
            reply.append(", dự kiến giao ").append(nullSafe(order.get("ngayNhanHangDuKien")));
        }
        if (!nullSafe(order.get("phuongThucThanhToan")).isBlank()) {
            reply.append(". Hình thức thanh toán: ").append(nullSafe(order.get("phuongThucThanhToan")));
        }
        reply.append(". Nếu anh/chị cần em hỗ trợ thêm về phí ship hoặc tổng thanh toán của đơn này, em kiểm tra tiếp ngay ạ.");
        return reply.toString();
    }

    private String buildShippingReply(Map<String, Object> order) {
        String eta = nullSafe(order.get("ngayNhanHangDuKien"));
        if (!eta.isBlank()) {
            return "Em kiểm tra giúp anh/chị: đơn " + nullSafe(order.get("maHoaDon"))
                    + " hiện ở trạng thái " + nullSafe(order.get("trangThai"))
                    + ", dự kiến giao vào " + eta + ". Nếu cần em có thể kiểm tra thêm phí ship hoặc tổng thanh toán của đơn này ạ.";
        }
        return "Em kiểm tra giúp anh/chị: đơn " + nullSafe(order.get("maHoaDon"))
                + " hiện ở trạng thái " + nullSafe(order.get("trangThai"))
                + ". Em chưa thấy ngày giao dự kiến rõ trong dữ liệu hiện tại, nhưng nếu anh/chị cần em có thể kiểm tra thêm phí ship hoặc tổng thanh toán của đơn này ạ.";
    }

    private String buildShippingFeeReply(Map<String, Object> order) {
        String orderCode = nullSafe(order.get("maHoaDon"));
        String shippingFee = formatMoney(order.get("phiShip"));
        String grandTotal = formatMoney(order.get("tongThanhToan"));
        if (isZeroMoney(order.get("phiShip"))) {
            return "Em kiểm tra giúp anh/chị: đơn " + orderCode
                    + " hiện chưa phát sinh phí ship hoặc đang được miễn phí vận chuyển. Tổng thanh toán hiện tại là " + grandTotal + ".";
        }
        return "Em kiểm tra giúp anh/chị: đơn " + orderCode
                + " có phí ship là " + shippingFee
                + ". Tổng thanh toán hiện tại là " + grandTotal + ".";
    }

    private String buildGrandTotalReply(Map<String, Object> order) {
        String orderCode = nullSafe(order.get("maHoaDon"));
        String grandTotal = formatMoney(order.get("tongThanhToan"));
        String shippingFee = formatMoney(order.get("phiShip"));
        String subtotal = formatMoney(order.get("tamTinh"));
        if (isZeroMoney(order.get("phiShip"))) {
            return "Em kiểm tra giúp anh/chị: đơn " + orderCode
                    + " hiện có tổng thanh toán là " + grandTotal
                    + ". Tạm tính hàng là " + subtotal + " và hiện chưa phát sinh phí ship.";
        }
        return "Em kiểm tra giúp anh/chị: đơn " + orderCode
                + " hiện có tổng thanh toán là " + grandTotal
                + ", trong đó tạm tính hàng là " + subtotal
                + " và phí ship là " + shippingFee + ".";
    }

    private String buildShippingAndTotalReply(Map<String, Object> order) {
        String orderCode = nullSafe(order.get("maHoaDon"));
        String shippingFee = formatMoney(order.get("phiShip"));
        String grandTotal = formatMoney(order.get("tongThanhToan"));
        if (isZeroMoney(order.get("phiShip"))) {
            return "Em kiểm tra giúp anh/chị: đơn " + orderCode
                    + " hiện chưa phát sinh phí ship hoặc đang được miễn phí vận chuyển. Tổng thanh toán hiện tại là " + grandTotal + ".";
        }
        return "Em kiểm tra giúp anh/chị: đơn " + orderCode
                + " có phí ship là " + shippingFee
                + " và tổng thanh toán hiện tại là " + grandTotal + ".";
    }

    private String buildStockReply(List<Map<String, Object>> variants) {
        Map<String, Object> variant = variants.get(0);
        String reply = "Em kiểm tra giúp anh/chị: " + nullSafe(variant.get("tenSanPham"))
                + " - mã " + nullSafe(variant.get("ma"))
                + ", màu " + nullSafe(variant.get("mauSac"))
                + ", size " + nullSafe(variant.get("kichThuoc"))
                + " hiện còn " + nullSafe(variant.get("soLuong")) + " sản phẩm.";
        if (variants.size() > 1) {
            List<String> alternates = variants.stream()
                    .skip(1)
                    .limit(2)
                    .map(v -> nullSafe(v.get("mauSac")) + " / " + nullSafe(v.get("kichThuoc")))
                    .filter(s -> !s.isBlank() && !"/".equals(s))
                    .toList();
            if (!alternates.isEmpty()) {
                reply += " Em cũng thấy thêm biến thể gần giống như " + String.join(", ", alternates) + ".";
            }
        }
        return reply;
    }

    private String buildBestSellerReply(List<Map<String, Object>> items) {
        if (items.isEmpty()) {
            return "Hiện em chưa tổng hợp được dữ liệu bán chạy đủ chắc chắn. Nếu anh/chị muốn, em có thể gợi ý ngay vài mẫu đang dễ chọn theo nhu cầu.";
        }
        Map<String, Object> first = items.get(0);
        return "Mẫu đang nổi bật nhất hiện tại là “" + nullSafe(first.get("tenSanPham")) + "”. Em cũng đã lọc thêm các sản phẩm bán tốt khác để anh/chị tham khảo nhanh.";
    }

    private String buildPromotionReply(List<Map<String, Object>> promotions,
                                       List<Map<String, Object>> vouchers) {
        if (promotions != null && !promotions.isEmpty()) {
            Map<String, Object> first = promotions.get(0);
            return "Hiện shop đang có chương trình khuyến mãi “" + nullSafe(first.get("ten")) + "”"
                    + ", giá trị " + nullSafe(first.get("giaTri")) + nullSafe(first.get("hinhThuc"))
                    + ". Anh/chị có thể gửi mẫu muốn mua để em kiểm tra ưu đãi phù hợp hơn.";
        }

        if (vouchers != null && !vouchers.isEmpty()) {
            Map<String, Object> first = vouchers.get(0);
            return "Hiện shop đang có voucher “" + nullSafe(first.get("ten")) + "” với giá trị "
                    + nullSafe(first.get("giaTri")) + " " + nullSafe(first.get("hinhThuc"))
                    + ". Nếu anh/chị muốn, em có thể gợi ý sản phẩm phù hợp để áp voucher dễ hơn.";
        }

        return "Hiện em chưa thấy chương trình khuyến mãi hoặc voucher khả dụng trong hệ thống. Anh/chị có thể nhắn mẫu muốn mua để em kiểm tra ưu đãi phù hợp hơn.";
    }

    private String buildPaymentReply(List<Map<String, Object>> methods) {
        if (methods.isEmpty()) {
            return "Hiện em chưa đọc được danh sách phương thức thanh toán từ hệ thống. Nếu anh/chị cần xác nhận đơn cụ thể, em có thể chuyển nhân viên hỗ trợ ngay.";
        }
        List<String> names = methods.stream().map(m -> nullSafe(m.get("ten"))).filter(s -> !s.isBlank()).limit(3).toList();
        return "Shop hiện có các hình thức thanh toán như " + String.join(", ", names) + ". Nếu anh/chị muốn em cũng có thể kiểm tra đơn hiện tại có hỗ trợ COD hay không.";
    }

    private String buildRecommendationReply(List<Map<String, Object>> products, Map<String, Object> entities) {
        Map<String, Object> first = products.get(0);
        String purpose = nullSafe(entities.get("purpose"));
        String prefix = purpose.isBlank() ? "Em tìm được vài lựa chọn phù hợp" : "Em tìm được vài lựa chọn phù hợp để " + purpose;
        String price = nullSafe(first.get("giaTu"));
        String stock = nullSafe(first.get("tongTon"));
        return prefix + ". Mẫu nổi bật là “" + nullSafe(first.get("tenSanPham")) + "”, giá từ " + price
                + (!stock.isBlank() ? ", hiện còn khoảng " + stock + " sản phẩm khả dụng." : ".");
    }

    private String composeWithAi(String intent, String userMessage, String groundedContext, String fallback) {
        String systemPrompt = """
                Bạn là chatbot bán hàng cho khách mua DirtyWave.
                Intent hiện tại: %s.
                Chỉ được dùng dữ liệu thực tế đã cung cấp.
                Trả lời tối đa 3 câu, giọng tư vấn thân thiện, tự nhiên, chốt bằng 1 bước tiếp theo rõ ràng.
                Không dùng markdown, không bịa chính sách, không cam kết quá mức và không nói như robot.
                Nếu khách đang phân vân, ưu tiên nhấn vào lợi ích mua hàng hoặc gợi ý lựa chọn kế tiếp.
                Nếu dữ liệu chưa đủ thì nói rõ phần nào chưa đủ.
                """.formatted(intent);
        String aiText = openAiGateway.askChatbot(systemPrompt, userMessage, groundedContext);
        return aiText == null || aiText.isBlank() ? fallback : sanitizeBotText(aiText);
    }

    private List<String> nextOrderQuickReplies(Map<String, Object> order, String userMessage) {
        String orderCode = nullSafe(order.get("maHoaDon"));
        String status = normalize(nullSafe(order.get("trangThai")));
        LinkedHashSet<String> replies = new LinkedHashSet<>();

        if (status.contains("giao") || status.contains("ship") || status.contains("van chuyen")) {
            replies.add("Phí ship " + orderCode + " bao nhiêu?");
            replies.add("Tổng chi phí " + orderCode + " bao nhiêu?");
            replies.add("Bao giờ giao tới?");
        } else {
            replies.add("Phí ship " + orderCode + " bao nhiêu?");
            replies.add("Tổng chi phí " + orderCode + " bao nhiêu?");
            replies.add("Có COD không?");
        }

        if (asksShippingFee(normalize(userMessage))) {
            replies.remove("Phí ship " + orderCode + " bao nhiêu?");
        }
        if (asksGrandTotal(normalize(userMessage))) {
            replies.remove("Tổng chi phí " + orderCode + " bao nhiêu?");
        }

        replies.add("Gặp nhân viên hỗ trợ");
        return new ArrayList<>(replies).subList(0, Math.min(replies.size(), 4));
    }

    private List<String> nextVariantQuickReplies(List<Map<String, Object>> variants) {
        LinkedHashSet<String> replies = new LinkedHashSet<>();
        replies.add("Tư vấn size");
        for (Map<String, Object> variant : variants) {
            String size = nullSafe(variant.get("kichThuoc"));
            String color = nullSafe(variant.get("mauSac"));
            if (!size.isBlank() && replies.size() < 4) {
                replies.add("Còn size " + size + " nào khác?");
            }
            if (!color.isBlank() && replies.size() < 4) {
                replies.add("Có màu " + color + " không?");
            }
        }
        replies.add("Gặp nhân viên hỗ trợ");
        return new ArrayList<>(replies).subList(0, Math.min(replies.size(), 4));
    }

    private List<String> nextRecommendationQuickReplies(List<Map<String, Object>> products, Map<String, Object> entities) {
        LinkedHashSet<String> replies = new LinkedHashSet<>();
        if (entities.get("maxPrice") != null) {
            replies.add("Xem mẫu rẻ hơn");
        }
        if (!products.isEmpty()) {
            replies.add("Tư vấn size");
            String name = nullSafe(products.get(0).get("tenSanPham"));
            if (!name.isBlank()) {
                replies.add("Còn màu nào khác?");
            }
        }
        replies.add("Có gì đang sale?");
        replies.add("Gặp nhân viên hỗ trợ");
        return new ArrayList<>(replies).subList(0, Math.min(replies.size(), 4));
    }

    private boolean asksShippingFee(String normalizedMessage) {
        return containsAny(normalizedMessage,
                "phi ship", "phí ship", "phi van chuyen", "phí vận chuyển",
                "ship bao nhieu", "ship bao nhiêu", "cuoc van chuyen", "cước vận chuyển");
    }

    private boolean asksGrandTotal(String normalizedMessage) {
        return containsAny(normalizedMessage,
                "tong chi phi", "tổng chi phí", "tong thanh toan", "tổng thanh toán",
                "tong cong", "tổng cộng", "bao nhieu tien", "bao nhiêu tiền",
                "het bao nhieu", "hết bao nhiêu", "tong don", "tổng đơn");
    }

    private boolean asksDeliveryEta(String normalizedMessage) {
        return containsAny(normalizedMessage,
                "bao gio giao", "bao giờ giao", "dang o dau", "đang ở đâu",
                "khi nao toi", "khi nào tới", "du kien giao", "dự kiến giao",
                "giao den dau", "giao đến đâu", "van chuyen den dau", "vận chuyển đến đâu");
    }

    private String formatMoney(Object value) {
        if (value == null) {
            return "0đ";
        }
        try {
            BigDecimal money = value instanceof BigDecimal bd ? bd : new BigDecimal(String.valueOf(value).replace(",", ""));
            return String.format(Locale.forLanguageTag("vi-VN"), "%,.0fđ", money.doubleValue());
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }

    private boolean isZeroMoney(Object value) {
        if (value == null) {
            return true;
        }
        try {
            BigDecimal money = value instanceof BigDecimal bd ? bd : new BigDecimal(String.valueOf(value).replace(",", ""));
            return money.compareTo(BigDecimal.ZERO) <= 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String normalize(String text) {
        if (text == null) return "";
        String s = text.toLowerCase(Locale.ROOT).trim();
        s = s.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        s = s.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        s = s.replaceAll("[ìíịỉĩ]", "i");
        s = s.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        s = s.replaceAll("[ùúụủũưừứựửữ]", "u");
        s = s.replaceAll("[ỳýỵỷỹ]", "y");
        s = s.replaceAll("[đ]", "d");
        return s;
    }

    private String sessionContext(ChatSession session) {
        if (session == null) {
            return "SESSION = {}";
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("customerName", nullSafe(session.getCustomerName()));
        ctx.put("customerEmail", nullSafe(session.getCustomerEmail()));
        ctx.put("customerPhone", nullSafe(session.getCustomerPhone()));
        return "SESSION = " + ctx;
    }

    private String sanitizeBotText(String aiText) {
        String cleaned = aiText == null ? "" : aiText.replace("**", "").replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 420) {
            cleaned = cleaned.substring(0, 420).trim();
            if (!cleaned.endsWith(".") && !cleaned.endsWith("!")) {
                cleaned += "...";
            }
        }
        return cleaned;
    }

    private boolean hasSessionPhone(ChatSession session) {
        return session != null && !nullSafe(session.getCustomerPhone()).isBlank();
    }

    private boolean hasLookupValue(Object value) {
        return value != null && !String.valueOf(value).trim().isBlank();
    }

    private String nullSafe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean containsAny(String text, String... candidates) {
        if (text == null || text.isBlank() || candidates == null || candidates.length == 0) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String buildPromotionReplyForProduct(String productCode,
                                                 List<Map<String, Object>> promotions,
                                                 List<Map<String, Object>> vouchers) {
        if (!productCode.isBlank()) {
            if (!promotions.isEmpty()) {
                Map<String, Object> first = promotions.get(0);
                return "Sản phẩm " + productCode + " đang áp dụng chương trình “"
                        + nullSafe(first.get("ten")) + "”, giảm "
                        + formatDiscount(first.get("giaTri"), first.get("hinhThuc")) + ".";
            }

            return "Hiện tại sản phẩm " + productCode + " chưa có chương trình khuyến mãi đang áp dụng.";
        }

        if (!promotions.isEmpty()) {
            Map<String, Object> first = promotions.get(0);
            return "Hiện shop đang có chương trình khuyến mãi “"
                    + nullSafe(first.get("ten")) + "”, giảm "
                    + formatDiscount(first.get("giaTri"), first.get("hinhThuc"))
                    + ". Anh/chị có thể gửi mã sản phẩm như SP001 để em kiểm tra mẫu đó có đang được áp dụng không.";
        }

        if (!vouchers.isEmpty()) {
            Map<String, Object> first = vouchers.get(0);
            return "Hiện shop có voucher “"
                    + nullSafe(first.get("ten")) + "” trị giá "
                    + formatDiscount(first.get("giaTri"), first.get("hinhThuc")) + ".";
        }

        return "Hiện chưa có chương trình khuyến mãi nào.";
    }

    private String formatDiscount(Object value, Object unit) {
        String unitText = nullSafe(unit);
        String valueText = nullSafe(value);

        try {
            java.math.BigDecimal amount = new java.math.BigDecimal(String.valueOf(value));
            amount = amount.stripTrailingZeros();
            valueText = amount.toPlainString();
        } catch (Exception ignored) {
        }

        if (unitText.equalsIgnoreCase("PERCENT") || unitText.equals("%")) {
            return valueText + "%";
        }

        if (unitText.equalsIgnoreCase("VND") || unitText.equalsIgnoreCase("VNĐ") || unitText.equalsIgnoreCase("đ")) {
            return valueText + "đ";
        }

        return valueText + (unitText.isBlank() ? "" : " " + unitText);
    }

    private String extractCode(String text, String regex) {
        if (text == null) return "";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group().trim().toUpperCase() : "";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean isOrderCodePlaceholderMessage(String message) {
        String normalized = normalize(message)
                .replace("…", "...")
                .replaceAll("\\s+", " ")
                .trim();

        return normalized.equals("ma don hd")
                || normalized.equals("ma don hd...")
                || normalized.equals("gui ma don hd")
                || normalized.equals("gui ma don hd...")
                || normalized.equals("mã đơn hd")
                || normalized.contains("ma don hd...")
                || normalized.contains("ma don dang hd")
                || normalized.contains("ma don dạng hd")
                || normalized.matches(".*\\bhd\\.\\.\\..*");
    }

    private String buildPromotionReplyForProductOrList(String productCode,
                                                       List<Map<String, Object>> promotions,
                                                       List<Map<String, Object>> vouchers,
                                                       List<Map<String, Object>> promotedProducts) {
        if (!productCode.isBlank()) {
            return buildPromotionReplyForProduct(productCode, promotions, vouchers);
        }

        if (promotedProducts != null && !promotedProducts.isEmpty()) {
            Map<String, Object> first = promotedProducts.get(0);
            String productName = nullSafe(first.get("tenSanPham"));
            String promoText = nullSafe(first.get("promotionText"));

            if (!productName.isBlank() && !promoText.isBlank()) {
                return "Em tìm được một số sản phẩm đang được khuyến mãi. Mẫu nổi bật là “"
                        + productName + "” đang áp dụng " + promoText + ". Anh/chị có thể vuốt sang phải để xem thêm.";
            }

            return "Em tìm được một số sản phẩm đang được khuyến mãi. Anh/chị có thể vuốt sang phải để xem thêm.";
        }

        return buildPromotionReplyForProduct(productCode, promotions, vouchers);
    }
}
