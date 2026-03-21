const express = require("express")
const crypto = require("crypto")
const qs = require("qs")
const cors = require("cors")

const app = express()

app.use(cors())
app.use(express.json())

/**
 * VNPay Sandbox Configuration
 * Test credentials provided by VNPay email dated 2026
 * See ../VNPAY_CONFIG.md for complete documentation and test information
 * 
 * Test Card: 9704198526191432198 | NGUYEN VAN A | OTP: 123456
 * 
 * IMPORTANT NOTES:
 * - This is for TESTING ONLY. Do NOT use in production.
 * - Before going live, replace these with PRODUCTION credentials from VNPay
 * - IPN endpoint must be publicly accessible for production (localhost won't work)
 * - See VNPAY_CONFIG.md "Before Production" section for migration checklist
 */
const tmnCode = "IMI81S82"                                    // Terminal ID (Mã Website)
const secretKey = "G9JXFVMDCBFH0N5KGFRJOS0BV6DNLN6X"        // Secret Key (Chuỗi bí mật)
const vnpUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html" // Test Payment Gateway
const returnUrl = "http://localhost:5173/payment-return"      // Return URL after payment
const supportedBankCodes = new Set(["VNPAYQR", "VNBANK", "INTCARD"])
const processedTxnRef = new Set() // In-memory tracking (for testing only - use DB for production)

function sortObject(obj) {
  const sorted = {}
  const keys = Object.keys(obj)
    .map((key) => encodeURIComponent(key))
    .sort()

  for (const encodedKey of keys) {
    sorted[encodedKey] = encodeURIComponent(obj[decodeURIComponent(encodedKey)]).replace(/%20/g, "+")
  }

  return sorted
}

/**
 * POST /api/vnpay/create-payment
 * Creates a VNPay payment URL that redirects user to VNPay's gateway
 * 
 * Request body:
 *   - amount: number (in VND, e.g., 100000 = 100,000 VND)
 *   - orderId: string (optional - defaults to timestamp)
 *   - bankCode: string (optional - filter by bank: VNPAYQR, VNBANK, INTCARD)
 * 
 * Response:
 *   - url: string (payment gateway URL with signed parameters)
 */
app.post("/api/vnpay/create-payment", (req, res) => {
  const amount = Number(req.body?.amount)
  const orderId = String(req.body?.orderId || Date.now())
  const bankCode = String(req.body?.bankCode || "").trim().toUpperCase()

  if (!Number.isFinite(amount) || amount <= 0) {
    res.status(400).json({ message: "amount must be > 0" })
    return
  }

  const now = new Date()
  const createDate =
    now.getFullYear() +
    String(now.getMonth() + 1).padStart(2, "0") +
    String(now.getDate()).padStart(2, "0") +
    String(now.getHours()).padStart(2, "0") +
    String(now.getMinutes()).padStart(2, "0") +
    String(now.getSeconds()).padStart(2, "0")

  let params = {
    vnp_Version: "2.1.0",
    vnp_Command: "pay",
    vnp_TmnCode: tmnCode,
    vnp_Amount: Math.round(amount * 100),
    vnp_CurrCode: "VND",
    vnp_TxnRef: orderId,
    vnp_OrderInfo: `Thanh toan don hang ${orderId}`,
    vnp_OrderType: "other",
    vnp_Locale: "vn",
    vnp_ReturnUrl: returnUrl,
    vnp_IpAddr: req.ip || "127.0.0.1",
    vnp_CreateDate: createDate
  }

  if (bankCode && supportedBankCodes.has(bankCode)) {
    params.vnp_BankCode = bankCode
  }

  params = sortObject(params)

  const signData = qs.stringify(params, { encode: false })
  const secureHash = crypto.createHmac("sha512", secretKey).update(signData).digest("hex")

  params.vnp_SecureHash = secureHash

  const paymentUrl = `${vnpUrl}?${qs.stringify(params, { encode: false })}`
  res.json({ url: paymentUrl })
})

/**
 * GET /api/vnpay/ipn
 * Webhook endpoint that receives payment confirmation from VNPay servers
 * Called by VNPay after payment processing (server-to-server notification)
 * 
 * Returns:
 *   - RspCode: Response code (00 = success, 97 = invalid signature, etc.)
 *   - Message: Response message
 * 
 * Security:
 *   - Verifies HMAC-SHA512 signature of all VNPay parameters
 *   - Prevents duplicate processing of same transaction
 *   - TODO: Add database checks for txnRef existence and amount validation
 */
app.get("/api/vnpay/ipn", (req, res) => {
  const inputData = {}
  for (const key of Object.keys(req.query || {})) {
    if (String(key).startsWith("vnp_")) {
      inputData[key] = req.query[key]
    }
  }

  const secureHash = String(inputData.vnp_SecureHash || "")
  const txnRef = String(inputData.vnp_TxnRef || "")
  const amount = Number(inputData.vnp_Amount || 0)
  const responseCode = String(inputData.vnp_ResponseCode || "")
  const transactionStatus = String(inputData.vnp_TransactionStatus || "")

  delete inputData.vnp_SecureHash
  delete inputData.vnp_SecureHashType

  const sorted = sortObject(inputData)
  const signData = qs.stringify(sorted, { encode: false })
  const expectedHash = crypto.createHmac("sha512", secretKey).update(signData).digest("hex")

  if (!secureHash || expectedHash !== secureHash) {
    res.json({ RspCode: "97", Message: "Invalid signature" })
    return
  }

  if (!txnRef) {
    res.json({ RspCode: "01", Message: "Order not found" })
    return
  }

  // TODO: replace these checks with DB checks (txnRef exists, amount matches order total, current status).
  if (!Number.isFinite(amount) || amount <= 0) {
    res.json({ RspCode: "04", Message: "Invalid amount" })
    return
  }

  if (processedTxnRef.has(txnRef)) {
    res.json({ RspCode: "02", Message: "Order already confirmed" })
    return
  }

  const isSuccess = responseCode === "00" && transactionStatus === "00"

  if (isSuccess) {
    processedTxnRef.add(txnRef)
    res.json({ RspCode: "00", Message: "Confirm Success" })
    return
  }

  // Signature is valid but payment failed at VNPay.
  res.json({ RspCode: "00", Message: "Confirm Success" })
})

app.get("/health", (_, res) => {
  res.json({ status: "ok" })
})

app.listen(3000, () => {
  console.log("VNPay backend running on http://localhost:3000")
})
