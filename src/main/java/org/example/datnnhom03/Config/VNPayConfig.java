package org.example.datnnhom03.Config;

import java.util.Random;

public class VNPayConfig {
    //
    public static String vnp_TmnCode = "IMI81S82";
    public static String vnp_HashSecret = "G9JXFVMDCBFH0N5KGFRJOS0BV6DNLN6X";
    public static String vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static String vnp_ReturnUrl = "http://localhost:5173/payment-return";

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

