package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Repository.HoaDonRepository;
import org.example.datnnhom03.utils.VNPayUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin("*")
public class PaymentApiController {

    @Value("${vnpay.tmn-code:IMI81S82}")
    private String tmnCode;

    @Value("${vnpay.hash-secret:G9JXFVMDCBFH0N5KGFRJOS0BV6DNLN6X}")
    private String hashSecret;

    @Value("${vnpay.return-url:http://localhost:5173/payment-return}")
    private String defaultReturnUrl;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    //
    // 1️⃣ Tạo link thanh toán
    //
    @GetMapping("/create")
    public ResponseEntity<?> createPayment(@RequestParam long amount,
                                           @RequestParam(required = false) String orderId,
                                           @RequestParam(required = false) String bankCode,
                                           @RequestParam(required = false) String returnUrl,
                                           @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
                                           @RequestHeader(value = "X-Real-IP", required = false) String realIp,
                                           HttpServletRequest request) {

        return createPaymentInternal(amount, orderId, bankCode, returnUrl, forwardedFor, realIp, request);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createPaymentPost(@RequestBody(required = false) Map<String, Object> payload,
                                               @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
                                               @RequestHeader(value = "X-Real-IP", required = false) String realIp,
                                               HttpServletRequest request) {

        long amount = 0;
        String orderId = null;
        String bankCode = null;
        String returnUrl = null;

        if (payload != null) {
            Object amountValue = payload.get("amount");
            if (amountValue != null) {
                try {
                    amount = Long.parseLong(String.valueOf(amountValue));
                } catch (NumberFormatException ex) {
                    return ResponseEntity.badRequest().body("Số tiền không hợp lệ");
                }
            }
            orderId = payload.get("orderId") == null ? null : String.valueOf(payload.get("orderId"));
            bankCode = payload.get("bankCode") == null ? null : String.valueOf(payload.get("bankCode"));
            returnUrl = payload.get("returnUrl") == null ? null : String.valueOf(payload.get("returnUrl"));
        }

        return createPaymentInternal(amount, orderId, bankCode, returnUrl, forwardedFor, realIp, request);
    }

    private ResponseEntity<?> createPaymentInternal(long amount,
                                                    String orderId,
                                                    String bankCode,
                                                    String returnUrl,
                                                    String forwardedFor,
                                                    String realIp,
                                                    HttpServletRequest request) {

        try {
            if (amount <= 0) {
                return ResponseEntity.badRequest().body("Số tiền phải lớn hơn 0");
            }

            String txnRef = (orderId == null || orderId.trim().isEmpty())
                    ? String.valueOf(System.currentTimeMillis())
                    : orderId.trim();

            String normalizedBankCode = bankCode == null ? "" : bankCode.trim().toUpperCase();
            if (!("".equals(normalizedBankCode)
                    || "VNPAYQR".equals(normalizedBankCode)
                    || "VNBANK".equals(normalizedBankCode)
                    || "INTCARD".equals(normalizedBankCode))) {
                normalizedBankCode = "";
            }

            String resolvedReturnUrl = (returnUrl == null || returnUrl.trim().isEmpty())
                    ? defaultReturnUrl
                    : returnUrl.trim();

            String clientIp = resolveClientIp(forwardedFor, realIp, request);

            String paymentUrl = VNPayUtils.createPaymentUrl(
                    amount,
                    txnRef,
                    tmnCode,
                    hashSecret,
                    resolvedReturnUrl,
                    normalizedBankCode,
                    clientIp
            );

            Map<String,String> response = new HashMap<>();
            response.put("paymentUrl", paymentUrl);
            response.put("url", paymentUrl);
            response.put("txnRef", txnRef);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body("Lỗi tạo thanh toán: " + e.getMessage());
        }
    }

    private String resolveClientIp(String forwardedFor,
                                   String realIp,
                                   HttpServletRequest request) {
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            String[] ips = forwardedFor.split(",");
            if (ips.length > 0 && !ips[0].trim().isEmpty()) {
                return ips[0].trim();
            }
        }

        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }

        String remoteAddr = request == null ? null : request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.trim().isEmpty()) {
            return "127.0.0.1";
        }
        return remoteAddr;
    }

    // ==========================
    // 2️⃣ VNPay return
    // ==========================
    @GetMapping("/vnpay-return")
    public ResponseEntity<?> paymentReturn(
            @RequestParam Map<String,String> params
    ){

        try {

            boolean validSignature =
                    VNPayUtils.validateSignature(params, hashSecret);

            if(!validSignature){

                return ResponseEntity.badRequest()
                        .body("Sai chữ ký VNPay");
            }

            String responseCode = params.get("vnp_ResponseCode");
            String txnRef = params.get("vnp_TxnRef");
            String amount = params.get("vnp_Amount");

            Map<String,Object> result = new HashMap<>();

            // ==========================
            // Thanh toán thành công
            // ==========================
            if("00".equals(responseCode)){

                HoaDon hd = new HoaDon();

                hd.setMaHoaDon("HD" + System.currentTimeMillis());
                hd.setTrangThai("CHO_THANH_TOAN");
                hd.setPhuongThucThanhToan("VNPAY");
                hd.setNgayTao(LocalDateTime.now());

                hd.setThanhTien(new java.math.BigDecimal(Long.parseLong(amount) / 100));

                hoaDonRepository.save(hd);

                result.put("status","SUCCESS");
                result.put("message","Thanh toán thành công");
                result.put("orderCode",txnRef);
                result.put("amount",amount);

                return ResponseEntity.ok(result);

            } else {

                result.put("status","FAILED");
                result.put("message","Thanh toán thất bại");
                result.put("orderCode",txnRef);

                return ResponseEntity.ok(result);
            }

        } catch (Exception e){

            return ResponseEntity.badRequest()
                    .body("Lỗi xử lý VNPay: " + e.getMessage());
        }
    }
}