package org.example.datnnhom03.config;

import java.util.Random;

public class VNPayConfig {
//
    public static String vnp_TmnCode = "JMJVT9AP";
    public static String vnp_HashSecret = "PW6HUCY3MNVTALCF7Z8ESU6O06KXPKSL";
    public static String vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static String vnp_ReturnUrl = "http://localhost:8080/api/payment/vnpay-return";

    public static String getRandomNumber(int len) {
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder();
        Random rd = new Random();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
