# VNPay Integration Testing Guide

## Quick Start Testing (5 minutes)

### 1. Start Development Server
```bash
# Terminal 1: Start Vite dev server with VNPay plugin
npm run dev

# Terminal 2: (Optional) Start Node backend if using production mode
npm run server
```

### 2. Test Payment Creation via UI
1. Navigate to: `http://localhost:5173/customer/vnpay-test`
2. Enter amount (e.g., 100000 = 100,000 VND)
3. Click "Start Payment"
4. You will be redirected to VNPay's sandbox gateway

### 3. Complete Test Payment
- Use test card: **9704198526191432198**
- Card holder: **NGUYEN VAN A**
- Expiry: **07/15**
- OTP: **123456**

### 4. Verify Return
After payment:
- You are returned to: `http://localhost:5173/payment-return?vnp_...`
- Check response code in URL parameters
- Click "Xác nhận thanh toán" to confirm in your app

---

## Advanced Testing (cURL / Direct API)

### Create Payment via cURL
```powershell
# PowerShell (Windows)
$Body = @{
    amount = 100000
    orderId = "TEST-$(Get-Date -format 'yyyyMMddHHmmss')"
    bankCode = "VNPAYQR"
} | ConvertTo-Json

curl.exe -X POST http://localhost:5173/api/vnpay/create-payment `
  -H "Content-Type: application/json" `
  -d $Body
```

Expected response:
```json
{
  "url": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=10000000&vnp_Command=pay&..."
}
```

### Simulate IPN Callback (For Backend Testing)
```powershell
# Only works if backend is running on port 3000
curl.exe -s "http://localhost:3000/api/vnpay/ipn?vnp_TxnRef=TEST-001&vnp_Amount=10000000&vnp_ResponseCode=00&vnp_TransactionStatus=00&vnp_SecureHash=DUMMY_HASH"
```

---

## Test Scenarios

### ✅ Success Case
- ResponseCode: `00`
- TransactionStatus: `00`
- Expected: Order confirmed, transaction marked complete

### ❌ Payment Failed at Bank
- ResponseCode: `non-00` (e.g., `10`)
- TransactionStatus: `non-00`
- Expected: Display failure message, allow retry

### ❌ User Cancelled
- ResponseCode: `24`
- Used when customer cancels during payment
- Expected: Show cancellation message

### ❌ Invalid Signature
- Wrong or missing `vnp_SecureHash`
- Expected: IPN returns RspCode "97"
- Backend: Does NOT process transaction

### ❌ Duplicate Transaction
- Same `vnp_TxnRef` processed twice
- Expected: Second attempt returns RspCode "02"
- Backend: Prevents duplicate confirmation

---

## Step-by-Step Test Flow

### Scenario 1: Checkout Page Test
```
1. Go to /home → Add product to cart
2. Click checkout
3. Select "VNPAY" as payment method
4. Click "Pay with VNPAY"
5. Complete payment with test card
6. Verify order is created in admin
```

### Scenario 2: Direct Test Page
```
1. Go to /vnpay-test (admin only)
2. Enter custom amount (e.g., 250000)
3. Click "Tạo thanh toán"
4. Complete payment flow
5. Check localStorage for confirmation marks
```

---

## Debugging Checklist

### Issue: "Cannot tạo được URL VNPay"
- [ ] Backend is running (`npm run dev` or `npm run server`)
- [ ] `/api/vnpay/create-payment` endpoint exists
- [ ] Amount is > 0
- [ ] Check browser console for exact error

### Issue: "Invalid signature" after payment
- [ ] Secret key matches: `G9JXFVMDCBFH0N5KGFRJOS0BV6DNLN6X`
- [ ] Parameter encoding is correct (spaces → `+`)
- [ ] `vnp_SecureHash` is removed before hashing
- [ ] Check backend logs for hash mismatch

### Issue: "Order already confirmed"
- [ ] Clear browser localStorage: `localStorage.clear()`
- [ ] Clear `processedTxnRef` (requires backend restart)
- [ ] Use different `orderId` for next test

### Issue: IPN not being called
- [ ] Localhost IPN not possible without ngrok/public domain
- [ ] In production, configure public IPN URL in VNPay Admin
- [ ] Check VNPay Merchant Admin → Transaction logs

---

## Expected Behavior

### Payment Creation
1. Form validates amount > 0
2. Backend creates timestamp and sorted params
3. HMAC-SHA512 hash generated with secret key
4. Payment URL built with all signed parameters
5. User redirected to VNPay gateway

### Return Flow
1. VNPay redirects to `/payment-return?vnp_...`
2. Frontend validates signature
3. Frontend checks responseCode and transactionStatus
4. Shows success/failure message
5. On success, shows "Xác nhận thanh toán" button
6. User clicks button to confirm order creation

### IPN Flow
1. VNPay sends server-to-server request to `/api/vnpay/ipn`
2. Backend extracts and validates signature
3. Checks for duplicate transactions
4. Returns appropriate RspCode
5. No order update (done by return flow)

---

## Logging & Monitoring

### Frontend Logs
Open browser DevTools → Console to see:
- Payment creation requests
- Return URL parameters
- Signature validation results
- localStorage state

### Backend Logs
Run: `npm run server` to see:
- Received requests
- Parameter validation
- Hash verification results
- Processed transactions

### VNPay Admin Dashboard
Visit: https://sandbox.vnpayment.vn/merchantv2/
- View all transactions
- Check payment status
- Download transaction history
- Configure IPN URL (production)

---

## Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| CORS error | Backend not running | `npm run server` in separate terminal |
| 400 Bad Request | Invalid amount | Check amount > 0 |
| Redirect loop | Return URL mismatch | Verify matches `VNPAY_RETURN` |
| Hash mismatch | Wrong secret key | Check credentials in code |
| Order not created | IPN not triggering | Expected for localhost; use return flow |
| Duplicate error | Same txnRef twice | Restart backend or use new txnRef |

---

## Production Testing Considerations

Before deploying to production:

1. **Whitelist IP Address**
   - Register production server IP in VNPay Admin
   - Test IPN delivery from VNPay servers

2. **SSL Certificate**
   - Install SSL/TLS certificate
   - Update all URLs to use HTTPS
   - Test with HTTPS-only gateway

3. **Database Integration**
   - Replace in-memory `processedTxnRef` with DB queries
   - Store transaction logs in database
   - Implement proper error handling

4. **Return URL Handling**
   - Update return URL to production domain
   - Handle network timeouts (VNPay may retry)
   - Implement idempotency (same txnRef = same response)

5. **IPN Configuration**
   - Update IPN URL in VNPay Merchant Admin
   - Ensure endpoint is publicly accessible
   - Implement request signature verification
   - Log all IPN requests for audit trail

---

## Test Results Checklist

- [ ] Payment creation returns valid URL
- [ ] User can access VNPay gateway
- [ ] Test card payment completes
- [ ] Return URL is called correctly
- [ ] Response parameters are present
- [ ] Order is created after confirmation
- [ ] Duplicate submission is prevented
- [ ] Error handling works properly
- [ ] localStorage state is managed correctly
- [ ] Offline handling is tested

---

## Reference Documentation

- **VNPay Integration Guide**: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
- **Code Samples**: https://sandbox.vnpayment.vn/apis/vnpay-demo/code-demo-tích-hợp
- **Test Gateway**: https://sandbox.vnpayment.vn/apis/vnpay-demo/
- **Full Config**: See [VNPAY_CONFIG.md](VNPAY_CONFIG.md)
