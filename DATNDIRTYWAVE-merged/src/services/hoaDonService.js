import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API = `${resolveApiOrigin()}/api/hoa-don`
const NODE_BACKEND = String(import.meta.env.VITE_NODE_BACKEND_URL || "").trim().replace(/\/$/, "")

const ORDER_LOOKUP_MAIL_FALLBACK_ENDPOINTS = [
  `/api/mail/send-order-lookup`,
  `${resolveApiOrigin()}/api/mail/send-order-lookup`,
  typeof window !== "undefined" && window.location?.origin
    ? `${window.location.origin}/api/mail/send-order-lookup`
    : "",
  NODE_BACKEND ? `${NODE_BACKEND}/api/mail/send-order-lookup` : ""
].filter(Boolean)

const normalizeHoaDonId = (id) => {
  const raw = String(id ?? "").trim()
  if (!raw) return null

  const lowered = raw.toLowerCase()
  if (["null", "undefined", "nan"].includes(lowered)) return null

  const parsed = Number(raw)
  if (!Number.isFinite(parsed) || parsed <= 0) return null
  return parsed
}

const ensureHoaDonId = (id) => {
  const normalized = normalizeHoaDonId(id)
  if (!normalized) {
    throw new Error("ID hóa đơn không hợp lệ")
  }
  return normalized
}

export const getAllHoaDon = () => {
  return axios.get(API)
}

export const getHoaDonPage = (params = {}) => {
  return axios.get(API, { params })
}

export const getHoaDonById = (id) => {
  return axios.get(`${API}/${ensureHoaDonId(id)}`)
}

export const lookupHoaDon = (maHoaDon, options = {}) => {
  const params = { maHoaDon }
  const soDienThoai = String(options?.soDienThoai || "").trim()
  const email = String(options?.email || "").trim()

  if (soDienThoai) {
    params.soDienThoai = soDienThoai
  }
  if (email) {
    params.email = email
  }

  return axios.get(`${API}/lookup`, {
    params
  })
}

export const sendOrderLookupMail = ({ maHoaDon, soDienThoai = "", email, trackingUrl = "" }) => {
  const payload = {
    maHoaDon,
    email,
    trackingUrl
  }

  if (String(soDienThoai || "").trim()) {
    payload.soDienThoai = String(soDienThoai).trim()
  }

  const tryFallback = async () => {
    let lastError = null
    for (const endpoint of ORDER_LOOKUP_MAIL_FALLBACK_ENDPOINTS) {
      try {
        return await axios.post(endpoint, payload)
      } catch (error) {
        lastError = error
      }
    }
    throw lastError || new Error("Không thể gửi mail tra cứu")
  }

  return axios.post(`${API}/lookup/send-mail`, payload).catch(() => tryFallback())
}

const PAYMENT_METHOD_ALIASES = {
  CASH: ["CASH", "TIEN_MAT", "COD"],
  // Keep BANK first, then transfer aliases; fallback to VNPAY/CASH for legacy backends.
  BANK: ["BANK", "CHUYEN_KHOAN", "BANK_TRANSFER", "VNPAY", "CASH"],
  VNPAY: ["VNPAY"]
}

const resolvePaymentCandidates = (method) => {
  const normalized = String(method || "").trim().toUpperCase()
  if (!normalized) return []

  const fromAlias = PAYMENT_METHOD_ALIASES[normalized]
  if (Array.isArray(fromAlias) && fromAlias.length) {
    return fromAlias
  }

  for (const values of Object.values(PAYMENT_METHOD_ALIASES)) {
    if (values.includes(normalized)) {
      return [normalized, ...values.filter((v) => v !== normalized)]
    }
  }

  return [normalized]
}

const withPaymentMethod = (payload, method) => ({
  ...payload,
  phuongThucThanhToan: method,
  paymentMethod: method,
  hinhThucThanhToan: method
})

const toPositiveNumber = (value) => {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? num : 0
}

const normalizeAmountPayload = (payload, method = "") => {
  const discountAmount = toPositiveNumber(
    payload?.giamGia ??
    payload?.discountAmount ??
    payload?.voucherDiscount ??
    payload?.giaSauGiamGia
  )

  const total = toPositiveNumber(
    payload?.thanhTien ||
    payload?.tongTienThanhToan ||
    payload?.tongTien
  )
  const normalizedMethod = String(method || payload?.phuongThucThanhToan || payload?.paymentMethod || "").trim().toUpperCase()
  const isBankLike = ["BANK", "CHUYEN_KHOAN", "BANK_TRANSFER", "VNPAY"].includes(normalizedMethod)

  const inputCash = toPositiveNumber(
    payload?.tienMat ||
    payload?.tienKhachDua ||
    payload?.tienKhachTra ||
    payload?.cashAmount
  )
  const inputTransfer = toPositiveNumber(
    payload?.tienChuyenKhoan ||
    payload?.soTienChuyenKhoan ||
    payload?.bankAmount ||
    payload?.transferAmount
  )

  let resolvedCash = inputCash
  let resolvedTransfer = inputTransfer

  if (resolvedCash <= 0 && resolvedTransfer <= 0 && total > 0) {
    if (isBankLike) {
      resolvedTransfer = total
    } else {
      resolvedCash = total
    }
  }

  const paid = toPositiveNumber(payload?.tienKhachDua || payload?.tienKhachTra || (resolvedCash + resolvedTransfer))
  const change = Math.max(paid - total, 0)

  return {
    ...payload,
    giaSauGiamGia: discountAmount,
    giamGia: discountAmount,
    discountAmount: discountAmount,
    thanhTien: total || Number(payload?.thanhTien || 0),
    tongTien: total || Number(payload?.tongTien || 0),
    tongTienThanhToan: total || Number(payload?.tongTienThanhToan || 0),
    soTienThanhToan: total || Number(payload?.soTienThanhToan || 0),
    tienMat: resolvedCash,
    cashAmount: resolvedCash,
    tienChuyenKhoan: resolvedTransfer,
    soTienChuyenKhoan: resolvedTransfer,
    transferAmount: resolvedTransfer,
    tienKhachDua: paid,
    tienKhachTra: paid,
    tienThua: change,
    tienTraLai: change
  }
}

export const createHoaDon = async (data) => {
  const basePayload = { ...(data || {}) }
  const initialMethod = String(
    basePayload?.phuongThucThanhToan ||
    basePayload?.paymentMethod ||
    basePayload?.hinhThucThanhToan ||
    ""
  ).trim().toUpperCase()
  const candidates = resolvePaymentCandidates(initialMethod)

  try {
    if (candidates.length) {
      const methodPayload = withPaymentMethod(basePayload, candidates[0])
      return await axios.post(API, normalizeAmountPayload(methodPayload, candidates[0]))
    }
    return await axios.post(API, normalizeAmountPayload(basePayload))
  } catch (error) {
    const status = Number(error?.response?.status || 0)
    if (status !== 400 || candidates.length <= 1) {
      throw error
    }

    let lastError = error
    for (const candidate of candidates.slice(1)) {
      try {
        const methodPayload = withPaymentMethod(basePayload, candidate)
        return await axios.post(API, normalizeAmountPayload(methodPayload, candidate))
      } catch (retryError) {
        lastError = retryError
      }
    }
    throw lastError
  }
}

export const updateHoaDon = (id, data) => {
  return axios.put(`${API}/${ensureHoaDonId(id)}`, data)
}

export const updateHoaDonBySystemEvent = (id, eventCode, note = "", trackingUrl = "") => {
  const normalizedTrackingUrl = String(trackingUrl || "").trim()
  const fallbackTrackingUrl = typeof window !== "undefined"
    ? `${window.location.origin}/tra-cuu-don-hang`
    : "/tra-cuu-don-hang"

  return axios.post(`${API}/${ensureHoaDonId(id)}/system-events`, {
    eventCode,
    note,
    trackingUrl: normalizedTrackingUrl || fallbackTrackingUrl
  })
}

export const addHoaDonItem = (id, data) => {
  const normalizedSpctId = Number(
    data?.spctId
    ?? data?.sanPhamChiTietId
    ?? data?.idSanPhamChiTiet
    ?? data?.chiTietSanPhamId
    ?? 0
  )
  const normalizedQty = Number(data?.soLuong ?? data?.quantity ?? 0)
  const normalizedUnitPrice = Number(
    data?.giaSauGiam
    ?? data?.giaBanSauDotGiamGia
    ?? data?.donGia
    ?? data?.giaBan
    ?? 0
  )
  const normalizedBasePrice = Number(
    data?.giaBanGoc
    ?? data?.giaNiemYet
    ?? data?.basePrice
    ?? normalizedUnitPrice
    ?? 0
  )
  const normalizedCampaignPercent = Number(
    data?.dotGiamGiaPhanTram
    ?? data?.campaignPercent
    ?? data?.phanTramGiamGia
    ?? 0
  )

  const payload = {
    ...data,
    spctId: normalizedSpctId > 0 ? normalizedSpctId : data?.spctId,
    sanPhamChiTietId: normalizedSpctId > 0 ? normalizedSpctId : data?.sanPhamChiTietId,
    idSanPhamChiTiet: normalizedSpctId > 0 ? normalizedSpctId : data?.idSanPhamChiTiet,
    chiTietSanPhamId: normalizedSpctId > 0 ? normalizedSpctId : data?.chiTietSanPhamId,
    soLuong: normalizedQty > 0 ? normalizedQty : data?.soLuong,
    quantity: normalizedQty > 0 ? normalizedQty : data?.quantity,
    giaBan: normalizedUnitPrice > 0 ? normalizedUnitPrice : data?.giaBan,
    donGia: normalizedUnitPrice > 0 ? normalizedUnitPrice : data?.donGia,
    giaSauGiam: normalizedUnitPrice > 0 ? normalizedUnitPrice : data?.giaSauGiam,
    giaBanSauDotGiamGia: normalizedUnitPrice > 0 ? normalizedUnitPrice : data?.giaBanSauDotGiamGia,
    giaBanGoc: normalizedBasePrice > 0 ? normalizedBasePrice : data?.giaBanGoc,
    giaNiemYet: normalizedBasePrice > 0 ? normalizedBasePrice : data?.giaNiemYet,
    dotGiamGiaPhanTram: normalizedCampaignPercent > 0 ? normalizedCampaignPercent : data?.dotGiamGiaPhanTram,
    campaignPercent: normalizedCampaignPercent > 0 ? normalizedCampaignPercent : data?.campaignPercent,
    campaignName: data?.campaignName,
    tenKhuyenMai: data?.tenKhuyenMai || data?.campaignName
  }

  if (normalizedQty > 0 && normalizedUnitPrice > 0) {
    payload.thanhTien = Number(data?.thanhTien || (normalizedQty * normalizedUnitPrice))
  }

  return axios.post(`${API}/${ensureHoaDonId(id)}/items`, payload)
}

export const updateHoaDonItemQty = (id, itemId, data) => {
  return axios.put(`${API}/${ensureHoaDonId(id)}/items/${itemId}`, data)
}

export const deleteHoaDonItem = (id, itemId) => {
  return axios.delete(`${API}/${ensureHoaDonId(id)}/items/${itemId}`)
}

const extractHoaDonItems = (payload) => {
  const candidates = [
    payload?.items,
    payload?.hoaDonChiTiets,
    payload?.chiTietHoaDons,
    payload?.chiTiets,
    payload?.chiTietDonHang,
    payload?.details,
    payload?.lineItems,
    payload?.data?.items,
    payload?.data?.hoaDonChiTiets,
    payload?.data?.chiTietHoaDons,
    payload?.data?.chiTiets,
    payload?.data?.chiTietDonHang,
    payload?.data?.details,
    payload?.data?.lineItems
  ]

  for (const candidate of candidates) {
    if (Array.isArray(candidate)) return candidate
  }

  return []
}

export const cancelHoaDon = (id, reason = "") => {
  return axios.post(`${API}/${id}/cancel`, null, {
    params: reason ? { reason } : {}
  })
}

export const getHoaDonChiTiet = async (id) => {
  const res = await getHoaDonById(id)
  const items = extractHoaDonItems(res.data)
  return {
    ...res,
    data: items
  }
}