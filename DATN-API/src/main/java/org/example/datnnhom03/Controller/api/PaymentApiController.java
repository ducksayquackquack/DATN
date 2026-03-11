package org.example.datnnhom03.Controller.api;

import org.example.datnnhom03.Model.HoaDon;
import org.example.datnnhom03.Model.PhuongThucThanhToan;
import org.example.datnnhom03.Model.TrangThaiHoaDon;
import org.example.datnnhom03.Repository.HoaDonRepository;
import org.example.datnnhom03.utils.VNPayUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin("*")
public class PaymentApiController {

    private static final String TMN_CODE = "JMJVT9AP";
    private static final String HASH_SECRET = "PW6HUCY3MNVTALCF7Z8ESU6O06KXPKSL";
    private static final String RETURN_URL =
            "http://localhost:8080/api/payment/vnpay-return";

    @Autowired
    private HoaDonRepository hoaDonRepository;

    // ==========================
    // 1️⃣ Tạo link thanh toán
    // ==========================
    @GetMapping("/create")
    public ResponseEntity<?> createPayment(@RequestParam long amount) {

        try {

            String paymentUrl = VNPayUtils.createPaymentUrl(
                    amount,
                    TMN_CODE,
                    HASH_SECRET,
                    RETURN_URL
            );

            Map<String,String> response = new HashMap<>();
            response.put("paymentUrl", paymentUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body("Lỗi tạo thanh toán: " + e.getMessage());
        }
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
                    VNPayUtils.validateSignature(params, HASH_SECRET);

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
                hd.setTrangThai(TrangThaiHoaDon.valueOf("CHO_THANH_TOAN"));
                hd.setPhuongThucThanhToan(PhuongThucThanhToan.valueOf("VNPAY"));
                hd.setNgayTao(LocalDateTime.now());

                // VNPay trả amount * 100
                hd.setThanhTien((double) (Long.parseLong(amount) / 100));

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