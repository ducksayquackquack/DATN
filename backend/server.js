const express = require("express")
const crypto = require("crypto")
const qs = require("qs")
const cors = require("cors")
const sql = require("mssql")
const nodemailer = require("nodemailer")

const app = express()

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

const SMTP_HOST = String(process.env.SMTP_HOST || "").trim()
const SMTP_PORT = Number(process.env.SMTP_PORT || 587)
const SMTP_USER = String(process.env.SMTP_USER || "").trim()
const SMTP_PASS = String(process.env.SMTP_PASS || "").trim()
const SMTP_FROM = String(process.env.SMTP_FROM || SMTP_USER || "").trim()
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

app.listen(3000, () => {
  console.log("VNPay backend running on http://localhost:3000")
  autoSeedCampaignProductsOnStartup().catch((err) => {
    console.error("[seed] Auto-seed campaign products failed:", err?.message || err)
  })
})
