# VNPay Integration Configuration

## Test Environment Setup

### Merchant Credentials (Sandbox)
- **Terminal ID (vnp_TmnCode)**: IMI81S82
- **Secret Key (vnp_HashSecret)**: G9JXFVMDCBFH0N5KGFRJOS0BV6DNLN6X
- **Payment URL**: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
- **Return URL**: http://localhost:5173/payment-return (configured in server.js)

### Test Card Information
- **Bank**: NCB (Ngan Hang Co Phan A Chau)
- **Card Number**: 9704198526191432198
- **Card Holder**: NGUYEN VAN A
- **Issue Date**: 07/15
- **OTP Password**: 123456

### Merchant Admin Access
- **URL**: https://sandbox.vnpayment.vn/merchantv2/
- **Email**: dinhtung2517@gmail.com
- **Password**: (Set during merchant registration)

### IPN Testing Dashboard
- **URL**: https://sandbox.vnpayment.vn/vnpaygw-sit-testing/user/login
- **Email**: dinhtung2517@gmail.com
- **Password**: (Same as merchant registration password)

## Current Backend Implementation

### Payment Creation Endpoint
- **Route**: `POST /api/vnpay/create-payment`
- **Port**: 3000
- **Parameters**:
  - `amount`: Payment amount in VND (integer)
  - `orderId`: Order ID (string, optional - defaults to timestamp)
  - `bankCode`: Bank code filter (optional - VNPAYQR, VNBANK, INTCARD)

**Example Request**:
```bash
curl -X POST http://localhost:3000/api/vnpay/create-payment \
  -H "Content-Type: application/json" \
  -d '{"amount": 100000, "orderId": "ORDER-001", "bankCode": "VNPAYQR"}'
```

### IPN Webhook Endpoint
- **Route**: `GET /api/vnpay/ipn`
- **Purpose**: Receives payment status updates from VNPay
- **Parameters**: All VNPay response parameters (query string)

**Example**:
```
GET http://localhost:3000/api/vnpay/ipn?vnp_Amount=100000&vnp_ResponseCode=00&vnp_TxnRef=ORDER-001&...
```

## Payment Flow

1. **User initiates payment**: Frontend sends order details to `/api/vnpay/create-payment`
2. **Backend generates payment URL**: Creates secure payment URL with checksum
3. **User redirected to VNPay**: Opens payment gateway at `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html`
4. **Payment processing**: User enters card details and OTP
5. **Return to app**: VNPay redirects to `/payment-return` with transaction results
6. **IPN notification**: VNPay calls `/api/vnpay/ipn` (server-to-server confirmation)
7. **Order confirmation**: Frontend confirms payment after receiving both return and IPN

## Security Implementation

### Checksum Verification
- **Algorithm**: HMAC-SHA512
- **Secret**: `G9JXFVMDCBFH0N5KGFRJOS0BV6DNLN6X`
- **Process**:
  1. Parameters are sorted alphabetically
  2. URL-encoded values are concatenated
  3. HMAC-SHA512 hash is generated with secret key
  4. Hash must match `vnp_SecureHash` for valid transactions

### Duplicate Prevention
- Transactions are tracked in `processedTxnRef` Set
- IPN can only confirm each transaction once
- Subsequent IPN attempts for same transaction are rejected with code "02"

## Testing

### Local Testing with cURL
```bash
# Start backend
npm run server

# Create payment
curl.exe -X POST http://localhost:3000/api/vnpay/create-payment \
  -H "Content-Type: application/json" \
  -d "{\"amount\": 100000, \"orderId\": \"TEST-$(Get-Date -f 'yyyyMMddHHmmss')\"}"

# Simulate IPN (example)
curl.exe -s "http://localhost:3000/api/vnpay/ipn?vnp_TxnRef=TEST-001&vnp_Amount=10000000&vnp_ResponseCode=00&vnp_TransactionStatus=00&..."
```

### Test Case Scenarios
1. **Successful Payment**: ResponseCode=00, TransactionStatus=00
2. **Failed Payment**: ResponseCode=non-00 or TransactionStatus=non-00
3. **Invalid Signature**: Wrong checksum value
4. **Duplicate Transaction**: Same TxnRef processed twice
5. **Amount Mismatch**: IPN amount differs from expected

## Frontend Integration

### Payment Initiation (vnpayService.js)
```javascript
import axios from "axios"

export const createVnpayPayment = (payload) => {
  return axios.post("/api/vnpay/create-payment", payload)
}
```

### Return Handler (PaymentReturn.vue)
- Validates VNPay response signature
- Checks transaction status (responseCode, transactionStatus)
- Confirms order in database after customer approval
- Tracks confirmation state to prevent duplicates

### Environment Variables
- Vite dev server proxies `/api/vnpay/` to `http://localhost:3000`
- Production should configure backend URL accordingly

## Troubleshooting

### IPN Not Received
- Verify public IP/domain is accessible from VNPay servers
- Check VNPay Merchant Admin for IPN configuration
- Not needed for local testing (use return URL instead)

### Invalid Signature Errors
- Verify secret key matches: `G9JXFVMDCBFH0N5KGFRJOS0BV6DNLN6X`
- Check parameter encoding (spaces → +, special chars → %XX)
- Ensure vnp_SecureHash is removed before hashing

### Payment Amount Issues
- VNPay expects amount in units of 100 (cents equivalent)
- Frontend: 100000 VND → Backend converts to 10000000 units
- Always validate IPN amount against order total

## Documentation Links

- **Integration Guide**: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
- **Code Samples**: https://sandbox.vnpayment.vn/apis/vnpay-demo/code-demo-tích-hợp
- **Demo Gateway**: https://sandbox.vnpayment.vn/apis/vnpay-demo/
- **Support**: support.vnpayment@vnpay.vn | 1900 55 55 77

## Next Steps

### Before Production
1. [ ] Update credentials with production Terminal ID and Secret Key
2. [ ] Change `vnp_Url` to production gateway
3. [ ] Update `returnUrl` and `ipnUrl` to production domain
4. [ ] Implement database checks in IPN handler (replace TODO)
5. [ ] Set up proper IPN endpoint with ngrok/hosting
6. [ ] Register production IPN URL in VNPay Merchant Admin
7. [ ] Obtain SSL certificate and test HTTPS

### Recommended Improvements
1. Store processed transactions in database (instead of in-memory Set)
2. Add transaction logging for audit trail
3. Implement payment timeout handling
4. Add retry logic for failed IPN callbacks
5. Create admin panel for viewing transaction history
