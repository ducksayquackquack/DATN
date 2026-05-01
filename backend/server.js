const http = require("http")
require("dotenv").config()
const express = require("express")
const crypto = require("crypto")
const qs = require("qs")
const cors = require("cors")
const sql = require("mssql")
const nodemailer = require("nodemailer")
const { WebSocketServer } = require("ws")

const app = express()
const httpServer = http.createServer(app)

app.use(cors())
app.use(express.json())

// ─── SQL Server connection pool for DirtyWave DB ───────────────────────────
const sqlConfig = {
  user: "sa",
  password: "123456",
  server: "localhost",
  port: 1433,
  database: "DirtyWave",
  options: { encrypt: false, trustServerCertificate: true }
}

const AUTO_SEED_CAMPAIGN_PRODUCTS = String(process.env.AUTO_SEED_CAMPAIGN_PRODUCTS || "true").toLowerCase() !== "false"
const AUTO_SEED_CAMPAIGN_ID = Number(process.env.AUTO_SEED_CAMPAIGN_ID || 0)
const AUTO_SEED_CAMPAIGN_CODE = String(process.env.AUTO_SEED_CAMPAIGN_CODE || "KM006").trim()
const AUTO_SEED_PRODUCT_COUNT = Math.max(1, Number(process.env.AUTO_SEED_PRODUCT_COUNT || 5) || 5)

const SMTP_HOST = String(
  process.env.SMTP_HOST ||
  process.env.SPRING_MAIL_HOST ||
  process.env.MAIL_HOST ||
  ""
).trim()
const SMTP_PORT = Number(
  process.env.SMTP_PORT ||
  process.env.SPRING_MAIL_PORT ||
  process.env.MAIL_PORT ||
  587
)
const SMTP_USER = String(
  process.env.SMTP_USER ||
  process.env.SPRING_MAIL_USERNAME ||
  process.env.MAIL_USERNAME ||
  ""
).trim()
const SMTP_PASS = String(
  process.env.SMTP_PASS ||
  process.env.SPRING_MAIL_PASSWORD ||
  process.env.MAIL_PASSWORD ||
  ""
).trim()
const SMTP_FROM = String(
  process.env.SMTP_FROM ||
  process.env.MAIL_FROM ||
  process.env.LOOKUP_MAIL_FROM ||
  SMTP_USER ||
  ""
).trim()
let mailTransporter = null

function getMailTransporter() {
  if (mailTransporter) return mailTransporter
  if (!SMTP_HOST || !SMTP_PORT || !SMTP_USER || !SMTP_PASS) return null

  mailTransporter = nodemailer.createTransport({
    host: SMTP_HOST,
    port: SMTP_PORT,
    secure: SMTP_PORT === 465,
    auth: {
      user: SMTP_USER,
      pass: SMTP_PASS,
    },
  })

  return mailTransporter
}

let poolPromise = null
function getPool() {
  if (!poolPromise) {
    poolPromise = sql.connect(sqlConfig).catch((err) => {
      poolPromise = null
      throw err
    })
  }
  return poolPromise
}

async function resolveSeedCampaign(pool) {
  if (Number.isFinite(AUTO_SEED_CAMPAIGN_ID) && AUTO_SEED_CAMPAIGN_ID > 0) {
    const byId = await pool.request()
      .input("kmId", sql.Int, AUTO_SEED_CAMPAIGN_ID)
      .query("SELECT TOP 1 id, maKhuyenMai, tenKhuyenMai FROM KhuyenMai WHERE id = @kmId")
    if (byId.recordset.length > 0) return byId.recordset[0]
  }

  if (AUTO_SEED_CAMPAIGN_CODE) {
    const byCode = await pool.request()
      .input("kmCode", sql.NVarChar(50), AUTO_SEED_CAMPAIGN_CODE)
      .query("SELECT TOP 1 id, maKhuyenMai, tenKhuyenMai FROM KhuyenMai WHERE maKhuyenMai = @kmCode")
    if (byCode.recordset.length > 0) return byCode.recordset[0]
  }

  const fallback = await pool.request().query("SELECT TOP 1 id, maKhuyenMai, tenKhuyenMai FROM KhuyenMai ORDER BY id DESC")
  return fallback.recordset[0] || null
}

async function autoSeedCampaignProductsOnStartup() {
  if (!AUTO_SEED_CAMPAIGN_PRODUCTS) {
    console.log("[seed] AUTO_SEED_CAMPAIGN_PRODUCTS=false, skip")
    return
  }

  const pool = await getPool()
  const campaign = await resolveSeedCampaign(pool)
  if (!campaign?.id) {
    console.log("[seed] No campaign found, skip")
    return
  }

  const kmId = Number(campaign.id)
  const current = await pool.request()
    .input("kmId", sql.Int, kmId)
    .query("SELECT COUNT(1) AS total FROM SanPham WHERE idKhuyenMai = @kmId")

  const existingCount = Number(current.recordset?.[0]?.total || 0)
  if (existingCount > 0) {
    console.log(`[seed] Campaign ${campaign.maKhuyenMai || kmId} already has ${existingCount} product(s), skip`)
    return
  }

  const pickResult = await pool.request()
    .input("count", sql.Int, AUTO_SEED_PRODUCT_COUNT)
    .query(`
      SELECT TOP (@count) id
      FROM SanPham
      WHERE idKhuyenMai IS NULL
      ORDER BY NEWID()
    `)

  const productIds = (pickResult.recordset || [])
    .map((r) => Number(r?.id))
    .filter((id) => Number.isFinite(id) && id > 0)

  if (!productIds.length) {
    console.log("[seed] No available products with idKhuyenMai IS NULL, skip")
    return
  }

  const req = pool.request().input("kmId", sql.Int, kmId)
  const idPlaceholders = productIds.map((id, idx) => {
    const key = `sp${idx}`
    req.input(key, sql.Int, id)
    return `@${key}`
  })

  await req.query(`
    UPDATE SanPham
    SET idKhuyenMai = @kmId
    WHERE id IN (${idPlaceholders.join(",")})
  `)

  console.log(
    `[seed] Seeded ${productIds.length} product(s) for campaign ${campaign.maKhuyenMai || kmId}: ${productIds.join(", ")}`
  )
}

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

app.post("/api/mail/send-order-lookup", async (req, res) => {
  const email = String(req.body?.email || "").trim()
  const maHoaDon = String(req.body?.maHoaDon || "").trim()
  const soDienThoai = String(req.body?.soDienThoai || "").trim()
  const trackingUrl = String(req.body?.trackingUrl || "").trim()

  if (!email || !maHoaDon) {
    return res.status(400).json({ message: "email và maHoaDon là bắt buộc" })
  }

  const transporter = getMailTransporter()
  if (!transporter) {
    return res.status(501).json({
      message: "MailSender chưa được cấu hình SMTP. Hãy thiết lập SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SMTP_FROM"
    })
  }

  try {
    const html = `
      <div style="font-family:Arial,sans-serif;line-height:1.6;color:#111">
        <h2 style="margin:0 0 8px">Tra cứu đơn hàng DirtyWave</h2>
        <p>Xin chào,</p>
        <p>Đơn hàng của bạn đã được ghi nhận với mã: <strong>${maHoaDon}</strong>.</p>
        ${soDienThoai ? `<p>Số điện thoại nhận hàng: <strong>${soDienThoai}</strong></p>` : ""}
        ${trackingUrl ? `<p>Bạn có thể theo dõi đơn tại: <a href="${trackingUrl}" target="_blank">${trackingUrl}</a></p>` : ""}
        <p>Cảm ơn bạn đã mua sắm tại DirtyWave.</p>
      </div>
    `

    const info = await transporter.sendMail({
      from: SMTP_FROM || SMTP_USER,
      to: email,
      subject: `Tra cứu đơn hàng ${maHoaDon} - DirtyWave`,
      html,
    })

    return res.json({ success: true, messageId: info?.messageId || null })
  } catch (error) {
    console.error("POST /api/mail/send-order-lookup error:", error)
    return res.status(500).json({ message: "Gửi email thất bại" })
  }
})

// ══════════════════════════════════════════════════════════════════════════════
// PHIEU THU CHI (Receipts / Payments during a shift)
// ══════════════════════════════════════════════════════════════════════════════

/**
 * GET /api/phieu-thu-chi?idNhanVien=&ngayLam=&idCaLam=
 * Returns all active phieu for a given shift
 */
app.get("/api/phieu-thu-chi", async (req, res) => {
  const idNhanVien = Number(req.query.idNhanVien)
  const ngayLam = String(req.query.ngayLam || "").trim()
  const idCaLam = Number(req.query.idCaLam || 0)

  if (!idNhanVien || !ngayLam) {
    return res.status(400).json({ message: "idNhanVien and ngayLam are required" })
  }

  try {
    const pool = await getPool()
    let query = "SELECT * FROM PhieuThuChi WHERE idNhanVien = @idNhanVien AND ngayLam = @ngayLam AND trangThai = N'ACTIVE'"
    const request = pool.request()
      .input("idNhanVien", sql.Int, idNhanVien)
      .input("ngayLam", sql.NVarChar(10), ngayLam)

    if (idCaLam > 0) {
      query += " AND idCaLam = @idCaLam"
      request.input("idCaLam", sql.Int, idCaLam)
    }

    query += " ORDER BY ngayTao DESC"
    const result = await request.query(query)
    res.json(result.recordset)
  } catch (err) {
    console.error("GET /api/phieu-thu-chi error:", err)
    res.status(500).json({ message: "Internal Server Error" })
  }
})

/**
 * POST /api/phieu-thu-chi
 * Create a new phieu thu or phieu chi
 */
app.post("/api/phieu-thu-chi", async (req, res) => {
  const { loai, phuongThuc, soTien, lyDo, idNhanVien, ngayLam, idCaLam, nguoiTao } = req.body || {}

  if (!loai || !["THU", "CHI"].includes(String(loai).toUpperCase())) {
    return res.status(400).json({ message: "loai must be THU or CHI" })
  }
  if (!Number.isFinite(Number(soTien)) || Number(soTien) <= 0) {
    return res.status(400).json({ message: "soTien must be > 0" })
  }
  if (!idNhanVien || !ngayLam) {
    return res.status(400).json({ message: "idNhanVien and ngayLam are required" })
  }

  try {
    const pool = await getPool()
    const result = await pool.request()
      .input("loai", sql.NVarChar(10), String(loai).toUpperCase())
      .input("phuongThuc", sql.NVarChar(20), String(phuongThuc || "TIEN_MAT").toUpperCase())
      .input("soTien", sql.Decimal(18, 0), Number(soTien))
      .input("lyDo", sql.NVarChar(500), String(lyDo || "").slice(0, 500))
      .input("idNhanVien", sql.Int, Number(idNhanVien))
      .input("ngayLam", sql.NVarChar(10), String(ngayLam).trim())
      .input("idCaLam", sql.Int, Number(idCaLam || 0))
      .input("nguoiTao", sql.NVarChar(100), String(nguoiTao || "").slice(0, 100))
      .query(`INSERT INTO PhieuThuChi (loai, phuongThuc, soTien, lyDo, idNhanVien, ngayLam, idCaLam, nguoiTao)
              OUTPUT INSERTED.*
              VALUES (@loai, @phuongThuc, @soTien, @lyDo, @idNhanVien, @ngayLam, @idCaLam, @nguoiTao)`)

    res.status(201).json(result.recordset[0])
  } catch (err) {
    console.error("POST /api/phieu-thu-chi error:", err)
    res.status(500).json({ message: "Internal Server Error" })
  }
})

/**
 * DELETE /api/phieu-thu-chi/:id   (soft delete)
 */
app.delete("/api/phieu-thu-chi/:id", async (req, res) => {
  const id = Number(req.params.id)
  if (!id) return res.status(400).json({ message: "Invalid id" })

  try {
    const pool = await getPool()
    await pool.request()
      .input("id", sql.Int, id)
      .query("UPDATE PhieuThuChi SET trangThai = N'DELETED' WHERE id = @id")
    res.json({ success: true })
  } catch (err) {
    console.error("DELETE /api/phieu-thu-chi error:", err)
    res.status(500).json({ message: "Internal Server Error" })
  }
})

/**
 * GET /api/phieu-thu-chi/summary?startDate=&endDate=
 * Aggregate all active phieu thu/chi in a date range
 */
app.get("/api/phieu-thu-chi/summary", async (req, res) => {
  const startDate = String(req.query.startDate || "").trim()
  const endDate = String(req.query.endDate || "").trim()

  if (!startDate || !endDate) {
    return res.status(400).json({ message: "startDate and endDate are required" })
  }

  try {
    const pool = await getPool()
    const result = await pool.request()
      .input("startDate", sql.NVarChar(10), startDate)
      .input("endDate", sql.NVarChar(10), endDate)
      .query(`
        SELECT loai, ISNULL(SUM(soTien), 0) AS tongTien, COUNT(*) AS soLuong
        FROM PhieuThuChi
        WHERE trangThai = N'ACTIVE'
          AND ngayLam >= @startDate AND ngayLam <= @endDate
        GROUP BY loai
      `)
    const rows = result.recordset || []
    const thu = rows.find(r => r.loai === 'THU') || { tongTien: 0, soLuong: 0 }
    const chi = rows.find(r => r.loai === 'CHI') || { tongTien: 0, soLuong: 0 }
    res.json({
      thu: { total: thu.tongTien, count: thu.soLuong },
      chi: { total: chi.tongTien, count: chi.soLuong }
    })
  } catch (err) {
    console.error("GET /api/phieu-thu-chi/summary error:", err)
    res.status(500).json({ message: "Internal Server Error" })
  }
})

// ─── Khuyến Mãi ↔ SanPham association (direct SQL) ─────────────────────────

// GET products applied to a specific KhuyenMai campaign
app.get("/api/khuyen-mai-products/:khuyenMaiId", async (req, res) => {
  try {
    const pool = await getPool()
    const kmId = Number(req.params.khuyenMaiId)
    if (!Number.isFinite(kmId) || kmId <= 0) return res.status(400).json({ message: "Invalid khuyenMaiId" })

    const result = await pool.request()
      .input("kmId", sql.Int, kmId)
      .query(`
        SELECT sp.id, sp.maSanPham, sp.tenSanPham, sp.idKhuyenMai,
               km.maKhuyenMai, km.tenKhuyenMai, km.giaTri, km.donViGiam,
               km.ngayBatDau, km.ngayKetThuc, km.trangThai AS trangThaiKM
        FROM SanPham sp
        LEFT JOIN KhuyenMai km ON sp.idKhuyenMai = km.id
        WHERE sp.idKhuyenMai = @kmId
      `)
    res.json(result.recordset)
  } catch (err) {
    console.error("GET /api/khuyen-mai-products error:", err)
    res.status(500).json({ message: "Internal Server Error" })
  }
})

// GET all products with their KhuyenMai info (for customer-facing discount display)
app.get("/api/khuyen-mai-products", async (req, res) => {
  try {
    const pool = await getPool()
    const result = await pool.request().query(`
      SELECT sp.id, sp.maSanPham, sp.tenSanPham, sp.idKhuyenMai,
             km.maKhuyenMai, km.tenKhuyenMai, km.giaTri, km.donViGiam,
             km.ngayBatDau, km.ngayKetThuc, km.trangThai AS trangThaiKM
      FROM SanPham sp
      LEFT JOIN KhuyenMai km ON sp.idKhuyenMai = km.id
      WHERE sp.idKhuyenMai IS NOT NULL
    `)
    res.json(result.recordset)
  } catch (err) {
    console.error("GET /api/khuyen-mai-products error:", err)
    res.status(500).json({ message: "Internal Server Error" })
  }
})

// POST sync product-discount associations
// Body: { khuyenMaiId: number, sanPhamIds: number[] }
// Sets idKhuyenMai on selected products, clears it from previously-associated products
app.post("/api/khuyen-mai-products/sync", async (req, res) => {
  try {
    const pool = await getPool()
    const kmId = Number(req.body?.khuyenMaiId)
    const sanPhamIds = (Array.isArray(req.body?.sanPhamIds) ? req.body.sanPhamIds : [])
      .map(Number).filter(n => Number.isFinite(n) && n > 0)

    if (!Number.isFinite(kmId) || kmId <= 0) return res.status(400).json({ message: "Invalid khuyenMaiId" })

    // Verify the KhuyenMai exists
    const kmCheck = await pool.request().input("kmId", sql.Int, kmId)
      .query("SELECT id FROM KhuyenMai WHERE id = @kmId")
    if (!kmCheck.recordset.length) return res.status(404).json({ message: "KhuyenMai not found" })

    const transaction = new sql.Transaction(pool)
    await transaction.begin()
    try {
      // Clear existing associations for this KhuyenMai
      await new sql.Request(transaction).input("kmId", sql.Int, kmId)
        .query("UPDATE SanPham SET idKhuyenMai = NULL WHERE idKhuyenMai = @kmId")

      // Set new associations
      if (sanPhamIds.length > 0) {
        // Use parameterized IN list (safe from injection)
        const idList = sanPhamIds.map((_, i) => `@sp${i}`).join(",")
        const req2 = new sql.Request(transaction).input("kmId", sql.Int, kmId)
        sanPhamIds.forEach((id, i) => req2.input(`sp${i}`, sql.Int, id))
        await req2.query(`UPDATE SanPham SET idKhuyenMai = @kmId WHERE id IN (${idList})`)
      }

      await transaction.commit()
      res.json({ success: true, khuyenMaiId: kmId, sanPhamCount: sanPhamIds.length })
    } catch (txErr) {
      await transaction.rollback()
      throw txErr
    }
  } catch (err) {
    console.error("POST /api/khuyen-mai-products/sync error:", err)
    res.status(500).json({ message: "Internal Server Error" })
  }
})

/**
 * GET /api/stock/sold-by-variant
 * Returns sold quantities per SanPhamChiTiet ID.
 * Queries SQL Server directly – no Spring Security auth required.
 * Used by customer-facing pages that cannot call the Spring /api/hoa-don endpoint.
 *
 * Response: [{ spctId: number, sold: number }, ...]
 */
app.get('/api/stock/sold-by-variant', async (req, res) => {
  try {
    const pool = await getPool()

    // Discover the column name used for SPCT ID in HoaDonChiTiet
    const spctColRes = await pool.request().query(`
      SELECT COLUMN_NAME
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_NAME = 'HoaDonChiTiet'
        AND COLUMN_NAME IN ('spctId', 'idSanPhamChiTiet', 'sanPhamChiTietId', 'spct_id', 'id_san_pham_chi_tiet')
    `)
    const spctPriority = ['spctId', 'idSanPhamChiTiet', 'sanPhamChiTietId', 'spct_id', 'id_san_pham_chi_tiet']
    const foundSpct = (spctColRes.recordset || []).map((r) => r.COLUMN_NAME)
    const spctCol = spctPriority.find((n) => foundSpct.includes(n))

    if (!spctCol) {
      console.warn('[/api/stock/sold-by-variant] Could not find SPCT ID column in HoaDonChiTiet. Found:', foundSpct)
      return res.json([])
    }

    // Discover the FK column to HoaDon in HoaDonChiTiet
    const fkColRes = await pool.request().query(`
      SELECT COLUMN_NAME
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_NAME = 'HoaDonChiTiet'
        AND COLUMN_NAME IN ('idHoaDon', 'hoaDonId', 'hoa_don_id', 'id_hoa_don')
    `)
    const fkPriority = ['idHoaDon', 'hoaDonId', 'hoa_don_id', 'id_hoa_don']
    const foundFk = (fkColRes.recordset || []).map((r) => r.COLUMN_NAME)
    const fkCol = fkPriority.find((n) => foundFk.includes(n))

    if (!fkCol) {
      console.warn('[/api/stock/sold-by-variant] Could not find HoaDon FK column in HoaDonChiTiet. Found:', foundFk)
      return res.json([])
    }

    // Query total sold per variant, excluding cancelled / returned orders
    const result = await pool.request().query(`
      SELECT
        hdct.[${spctCol}] AS spctId,
        SUM(hdct.soLuong) AS sold
      FROM HoaDonChiTiet hdct
      INNER JOIN HoaDon hd ON hd.id = hdct.[${fkCol}]
      WHERE hdct.[${spctCol}] IS NOT NULL
        AND hdct.soLuong > 0
        AND hd.trangThai NOT LIKE N'%Hủy%'
        AND hd.trangThai NOT LIKE N'%Huỷ%'
        AND hd.trangThai NOT LIKE N'%hủy%'
        AND hd.trangThai NOT LIKE N'%huỷ%'
        AND hd.trangThai NOT LIKE N'%HUY%'
        AND hd.trangThai NOT LIKE N'%hoàn về%'
        AND hd.trangThai NOT LIKE N'%trả hàng%'
        AND hd.trangThai NOT LIKE N'%giao thất bại%'
      GROUP BY hdct.[${spctCol}]
    `)

    res.json(result.recordset || [])
  } catch (err) {
    console.error('GET /api/stock/sold-by-variant error:', err)
    res.json([])
  }
})

// ══════════════════════════════════════════════════════════════════════════════
// WEBSOCKET SERVER  –  ws://localhost:3000/ws/hoa-don
// ══════════════════════════════════════════════════════════════════════════════

const wss = new WebSocketServer({ server: httpServer, path: "/ws/hoa-don" })

/** Send a JSON message to every open client */
function broadcastWs(payload) {
  const data = JSON.stringify(payload)
  wss.clients.forEach((client) => {
    if (client.readyState === 1 /* OPEN */) {
      client.send(data)
    }
  })
}

wss.on("connection", (ws) => {
  ws.send(JSON.stringify({ type: "CONNECTED", ts: Date.now() }))
  ws.on("error", () => {}) // silence uncaught errors on a single socket
})

/** Allow any backend / Spring to trigger a frontend refresh */
// ══════════════════════════════════════════════════════════════════════════════
// MAIL – Gửi thông tin tài khoản nhân viên mới
// ══════════════════════════════════════════════════════════════════════════════

app.post("/api/mail/send-nhanvien-account", async (req, res) => {
  const email = String(req.body?.email || "").trim()
  const tenNhanVien = String(req.body?.tenNhanVien || "Nhân viên").trim()
  const matKhau = String(req.body?.matKhau || "").trim()
  const vaiTro = String(req.body?.vaiTro || "EMPLOYEE").trim()

  if (!email || !matKhau) {
    return res.status(400).json({ message: "email và matKhau là bắt buộc" })
  }

  const transporter = getMailTransporter()
  if (!transporter) {
    return res.status(501).json({
      message: "MailSender chưa được cấu hình SMTP. Hãy thiết lập SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS"
    })
  }

  const vaiTroHienThi = vaiTro === "ADMIN" ? "Quản trị viên" : "Nhân viên"
  const year = new Date().getFullYear()

  const html = `<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Tài khoản nhân viên mới</title>
</head>
<body style="margin:0;padding:24px 0;background:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif;color:#111827">
  <div style="max-width:560px;margin:0 auto;border-radius:14px;overflow:hidden;border:1px solid #e2e8f0;box-shadow:0 8px 26px rgba(15,23,42,.08)">
    <div style="background:linear-gradient(140deg,#111827 0%,#b91c1c 45%,#dc2626 100%);padding:28px 24px;text-align:center">
      <div style="display:inline-block;padding:7px 14px;border-radius:999px;background:rgba(255,255,255,.12);border:1px solid rgba(255,255,255,.25);font-size:12px;letter-spacing:.08em;color:#fff;font-weight:700;text-transform:uppercase">DirtyWave Internal</div>
      <h1 style="margin:14px 0 6px;font-size:28px;line-height:1.12;color:#fff;letter-spacing:.02em">TÀI KHOẢN NHÂN VIÊN MỚI</h1>
      <p style="margin:0;font-size:13px;color:rgba(255,255,255,.88)">Hệ thống quản lý nội bộ DirtyWave</p>
    </div>

    <div style="background:#fff;padding:26px 24px 20px">
      <p style="margin:0 0 8px;font-size:16px;font-weight:700;color:#0f172a">Xin chào, ${tenNhanVien}!</p>
      <p style="margin:0 0 12px;font-size:15px;font-weight:600;color:#b91c1c;line-height:1.6">Chúc mừng bạn đã được tạo tài khoản nhân viên mới.</p>
      <p style="margin:0 0 10px;font-size:13px;color:#64748b;line-height:1.6">Mã thông báo mẫu mới: DW-MAIL-V2</p>
      <p style="margin:0 0 16px;font-size:14px;color:#475569;line-height:1.6">Dưới đây là thông tin đăng nhập và hướng dẫn cơ bản để truy cập hệ thống nội bộ:</p>

      <div style="border:1px solid #fecaca;background:linear-gradient(180deg,#fff7f7,#fff);border-radius:12px;padding:16px 16px 10px">
        <table style="width:100%;border-collapse:collapse;font-size:14px">
          <tr>
            <td style="padding:9px 0;color:#6b7280">Tên tài khoản</td>
            <td style="padding:9px 0;text-align:right;font-weight:800;color:#b91c1c">${email}</td>
          </tr>
          <tr><td colspan="2" style="border-top:1px dashed #fecaca"></td></tr>
          <tr>
            <td style="padding:9px 0;color:#6b7280">Mật khẩu tạm thời</td>
            <td style="padding:9px 0;text-align:right;font-weight:700;color:#0f172a">${matKhau}</td>
          </tr>
          <tr><td colspan="2" style="border-top:1px dashed #fecaca"></td></tr>
          <tr>
            <td style="padding:9px 0;color:#6b7280">Vai trò</td>
            <td style="padding:9px 0;text-align:right;font-weight:700;color:#0f172a">${vaiTroHienThi}</td>
          </tr>
        </table>
      </div>

      <div style="margin-top:14px;padding:10px 12px;border-left:3px solid #ef4444;background:#fff1f2;border-radius:8px;font-size:13px;color:#9f1239">
        Vui lòng đổi mật khẩu ngay sau khi đăng nhập lần đầu để bảo mật tài khoản.
      </div>

      <div style="margin-top:10px;padding:10px 12px;border-left:3px solid #1d4ed8;background:#eff6ff;border-radius:8px;font-size:13px;color:#1e3a8a">
        Nếu không đăng nhập được, hãy liên hệ quản trị viên để được cấp lại mật khẩu.
      </div>

      <p style="margin:14px 0 0;font-size:13px;color:#475569">
        Đăng nhập tại: <a href="http://localhost:5173/auth/staff-login" style="color:#b91c1c;font-weight:700">Trang nội bộ nhân viên</a>
      </p>
    </div>

    <div style="background:#f8fafc;padding:13px 20px;border-top:1px solid #e2e8f0;text-align:center;font-size:12px;color:#94a3b8">
      © ${year} DirtyWave - Email tự động, vui lòng không trả lời.
    </div>
  </div>
</body>
</html>`

  try {
    await transporter.sendMail({
      from: SMTP_FROM || SMTP_USER,
      to: email,
      subject: `[DirtyWave] Chúc mừng, tài khoản nhân viên mới – ${tenNhanVien}`,
      html,
    })
    return res.json({ success: true })
  } catch (err) {
    console.error("POST /api/mail/send-nhanvien-account error:", err)
    return res.status(500).json({ message: "Gửi email thất bại" })
  }
})

/** Allow any backend / Spring to trigger a frontend refresh */
app.post("/api/ws/broadcast", (req, res) => {
  const payload = req.body || {}
  broadcastWs({ type: "REFRESH", ...payload, ts: Date.now() })
  res.json({ ok: true, clients: wss.clients.size })
})

httpServer.listen(3000, () => {
  console.log("DirtyWave Node backend  http://localhost:3000  ws://localhost:3000/ws/hoa-don")
  autoSeedCampaignProductsOnStartup().catch((err) => {
    console.error("[seed] Auto-seed campaign products failed:", err?.message || err)
  })
})
