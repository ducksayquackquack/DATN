package org.example.datnnhom03.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class VNPayUtils {

    private static final String VNP_URL =
            "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    public static String createPaymentUrl(
            long amount,
            String tmnCode,
            String hashSecret,
            String returnUrl
    ) throws Exception {

        String txnRef = String.valueOf(System.currentTimeMillis());

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

        String createDate = formatter.format(cld.getTime());

        Map<String,String> params = new HashMap<>();

        params.put("vnp_Version","2.1.0");
        params.put("vnp_Command","pay");
        params.put("vnp_TmnCode",tmnCode);
        params.put("vnp_Amount",String.valueOf(amount * 100));
        params.put("vnp_CurrCode","VND");
        params.put("vnp_TxnRef",txnRef);
        params.put("vnp_OrderInfo","Thanh toan don hang");
        params.put("vnp_OrderType","other");
        params.put("vnp_Locale","vn");
        params.put("vnp_ReturnUrl",returnUrl);
        params.put("vnp_IpAddr","127.0.0.1");
        params.put("vnp_CreateDate",createDate);

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {

            String value = params.get(fieldName);

            hashData.append(fieldName)
                    .append("=")
                    .append(URLEncoder.encode(value,"UTF-8"));

            query.append(fieldName)
                    .append("=")
                    .append(URLEncoder.encode(value,"UTF-8"));

            if(fieldNames.indexOf(fieldName) != fieldNames.size()-1){
                hashData.append("&");
                query.append("&");
            }
        }

        String secureHash = hmacSHA512(hashSecret, hashData.toString());

        query.append("&vnp_SecureHash=").append(secureHash);

        return VNP_URL + "?" + query;
    }

    public static boolean validateSignature(
            Map<String,String> params,
            String secretKey
    ) throws Exception {

        String secureHash = params.get("vnp_SecureHash");

        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();

        for(String field : fieldNames){

            hashData.append(field)
                    .append("=")
                    .append(params.get(field));

            if(fieldNames.indexOf(field) != fieldNames.size()-1){
                hashData.append("&");
            }
        }

        String checkHash =
                hmacSHA512(secretKey, hashData.toString());

        return checkHash.equals(secureHash);
    }

    private static String hmacSHA512(
            String key,
            String data
    ) throws Exception {

        Mac hmac512 = Mac.getInstance("HmacSHA512");

        SecretKeySpec secretKey =
                new SecretKeySpec(key.getBytes(),"HmacSHA512");

        hmac512.init(secretKey);

        byte[] bytes =
                hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder hash = new StringBuilder();

        for (byte b : bytes) {
            hash.append(String.format("%02x", b));
        }

        return hash.toString();
    }
}