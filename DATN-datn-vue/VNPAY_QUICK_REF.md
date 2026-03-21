# VNPay Quick Reference Card

## Test Credentials
```
Terminal ID:  IMI81S82
Secret Key:   G9JXFVMDCBFH0N5KGFRJOS0BV6DNLN6X
Gateway:      https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
```

## Test Card
```
Number:       9704198526191432198
Holder:       NGUYEN VAN A
Expiry:       07/15
OTP Password: 123456
```

## Merchant Access
```
Admin:    https://sandbox.vnpayment.vn/merchantv2/
Email:    dinhtung2517@gmail.com
Password: (From registration)

IPN Test: https://sandbox.vnpayment.vn/vnpaygw-sit-testing/user/login
```

## API Endpoints
```
POST /api/vnpay/create-payment
  Request:  { amount, orderId?, bankCode? }
  Response: { url }

GET /api/vnpay/ipn
  Purpose:  Receive payment confirmation from VNPay
```

## Testing URLs
```
Dev:              http://localhost:5173/vnpay-test
Test Page:        http://localhost:5173/customer/vnpay-test
Return Handler:   http://localhost:5173/payment-return
Backend:          http://localhost:3000 (if running separately)
```

## Start Development
```bash
# Option 1: Vite dev server (includes VNPay endpoint)
npm run dev

# Option 2: Separate backend server
cd backend && npm run dev
```

## Key Files
```
vite.config.js              - VNPay payment creation plugin
backend/server.js           - Production backend (payment + IPN endpoints)
src/services/vnpayService.js - Frontend API calls
src/views/PaymentReturn.vue - Return URL handler
src/views/customer/VNPayTest.vue - Test component

Documentation:
VNPAY_CONFIG.md    - Full configuration guide
VNPAY_TESTING.md   - Complete testing guide
INTEGRATION_STATUS.md - Implementation status & checklist
```

## Quick Test (5 minutes)
1. `npm run dev` - Start dev server
2. Navigate to http://localhost:5173/vnpay-test
3. Enter amount (e.g., 100000)
4. Click "Tạo thanh toán"
5. Use test card details above
6. Complete payment

## Common Issues
| Issue | Solution |
|-------|----------|
| 400 Bad Request | Check amount > 0 |
| Cannot create URL | Start backend: `npm run dev` or `npm run server` |
| Hash mismatch | Verify secret key is correct |
| Duplicate error | Restart backend or use new orderId |
| CORS error | Backend must be running |

## Support
```
Email:    support.vnpayment@vnpay.vn
Hotline:  1900 55 55 77
Docs:     https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
```

## Production Checklist
- [ ] Get production Terminal ID & Secret Key
- [ ] Update credentials in code
- [ ] Update payment gateway URL
- [ ] Change return URL to production domain
- [ ] Move transaction tracking to database
- [ ] Set up public IPN endpoint
- [ ] Configure IPN URL in VNPay Admin
- [ ] Install SSL certificate
- [ ] Test with sandbox first
- [ ] Go live

---
For full documentation and testing procedures, see:
- **VNPAY_CONFIG.md** - Configuration & troubleshooting
- **VNPAY_TESTING.md** - Testing & debugging
- **INTEGRATION_STATUS.md** - Implementation summary
