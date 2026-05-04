import axios from "axios"
import { resolveApiOrigin } from "../utils/apiOrigin"

const API_ORIGIN = resolveApiOrigin().replace(/\/$/, "")
const API = `${API_ORIGIN}/api/khuyen-mai`
const VOUCHER_API = `${API_ORIGIN}/api/phieu-giam-gia`

const VOUCHER_MAX_DISCOUNT_PRESETS = {
  PGG001: 2000000,
  PGG002: 2000000,
  PGG005: 2000000
}

const VOUCHER_MIN_ORDER_PRESETS = {
  PGG001: 1000000,
  PGG002: 1000000,
  PGG005: 1000000
}

const toNumber = (...values) => {
  for (const value of values) {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) return parsed
  }
  return 0
}

export const normalizeVoucherData = (voucher = {}) => {
  const voucherCode = String(
    voucher?.maPhieuGiamGia || voucher?.maPhieu || voucher?.maKhuyenMai || voucher?.code || ""
  ).trim().toUpperCase()

  const isPercentDiscount = Boolean(
    voucher?.hinhThucGiam ?? voucher?.giamTheoPhanTram ?? voucher?.isPercent
  )

  const remainingUsage = toNumber(
    voucher?.soLuongSuDung,
    voucher?.soLuongConLai,
    voucher?.remainingUsage,
    voucher?.remaining,
    voucher?.soLuong
  )

  const usedUsageRaw = toNumber(
    voucher?.soLuongDaDung,
    voucher?.daSuDung,
    voucher?.soLanDaSuDung,
    voucher?.usedCount
  )

  const totalUsage = toNumber(
    voucher?.tongSoLuong,
    voucher?.soLuongPhatHanh,
    voucher?.soLuongBanDau,
    voucher?.totalUsageLimit
  )

  const usedUsage = usedUsageRaw > 0
    ? usedUsageRaw
    : (totalUsage > 0 ? Math.max(totalUsage - Math.max(remainingUsage, 0), 0) : 0)

  const maxDiscount = toNumber(
    voucher?.soTienGiamToiDa,
    voucher?.giaTriGiamToiDa,
    voucher?.mucGiamToiDa,
    voucher?.maxDiscount,
    voucher?.maximumDiscount,
    voucher?.giaTriToiDa
  )

  const discountValue = toNumber(voucher?.giaTriGiamGia, voucher?.giaTriGiam, voucher?.discountValue)
  const minOrderValue = toNumber(
    voucher?.hoaDonToiThieu,
    voucher?.donToiThieu,
    voucher?.dieuKienToiThieu,
    voucher?.minOrderValue
  )

  const presetMaxDiscount = Number(VOUCHER_MAX_DISCOUNT_PRESETS[voucherCode] || 0)
  const presetMinOrder = isPercentDiscount ? Number(VOUCHER_MIN_ORDER_PRESETS[voucherCode] || 0) : 0
  let resolvedMaxDiscount = maxDiscount > 0 ? maxDiscount : presetMaxDiscount
  let resolvedMinOrder = minOrderValue > 0 ? minOrderValue : presetMinOrder

  if (resolvedMaxDiscount <= 0) {
    // Ensure % vouchers always have a cap and fixed-amount vouchers show a meaningful ceiling.
    if (isPercentDiscount) {
      resolvedMaxDiscount = resolvedMinOrder > 0 ? resolvedMinOrder : 0
    } else {
      resolvedMaxDiscount = resolvedMinOrder > 0 ? resolvedMinOrder : discountValue
    }
  }

  return {
    ...voucher,
    maPhieuGiamGia: String(
      voucher?.maPhieuGiamGia || voucher?.maPhieu || voucher?.maKhuyenMai || voucher?.code || ""
    ).trim(),
    tenPhieuGiamGia: String(
      voucher?.tenPhieuGiamGia || voucher?.tenKhuyenMai || voucher?.tenPhieu || voucher?.name || ""
    ).trim(),
    hinhThucGiam: isPercentDiscount,
    giaTriGiamGia: discountValue,
    hoaDonToiThieu: Math.max(resolvedMinOrder, 0),
    soTienGiamToiDa: Math.max(resolvedMaxDiscount, 0),
    soLuongSuDung: Math.max(remainingUsage, 0),
    soLuongDaDung: Math.max(usedUsage, 0),
    tongSoLuong: Math.max(totalUsage, 0)
  }
}

// ============= EXISTING KHUYEN MAI (DISCOUNT CAMPAIGNS) =============
export const getAllKhuyenMai = () =>
  axios.get(API)

export const getKhuyenMaiById = (id) =>
  axios.get(`${API}/${id}`)

export const createKhuyenMai = (data) =>
  axios.post(API, data)

export const updateKhuyenMai = (id, data) =>
  axios.put(`${API}/${id}`, data)

export const deleteKhuyenMai = (id) =>
  axios.delete(`${API}/${id}`)

// ============= VOUCHER (PHIEU GIAM GIA) =============
export const getAllVouchers = (params = {}) =>
  axios.get(VOUCHER_API, { params })

export const getVoucherById = (id) =>
  axios.get(`${VOUCHER_API}/${id}`)

export const createVoucher = (data) =>
  axios.post(VOUCHER_API, data)

export const updateVoucher = (id, data) =>
  axios.put(`${VOUCHER_API}/${id}`, data)

export const deleteVoucher = (id) =>
  axios.delete(`${VOUCHER_API}/${id}`)

// Get active vouchers for POS
export const getActiveVouchers = async (limit = 0, params = {}) => {
  const parsedLimit = Number(limit)
  const hasLimit = Number.isFinite(parsedLimit) && parsedLimit > 0
  const mergedParams = {
    ...(params || {}),
    ...(hasLimit ? { limit: Math.floor(parsedLimit) } : {})
  }
  const response = await axios.get(`${VOUCHER_API}/active`, {
    params: mergedParams,
    headers: {
      Accept: 'application/json'
    }
  })

  const contentType = String(
    response?.headers?.['content-type'] ||
    response?.headers?.['Content-Type'] ||
    ''
  ).toLowerCase()

  const data = response?.data
  const hasArrayPayload = Array.isArray(data) || Array.isArray(data?.content)
  const looksLikeHtml = typeof data === 'string' && /<html|<!doctype/i.test(data)

  if (!contentType.includes('application/json') || looksLikeHtml || !hasArrayPayload) {
    console.warn('[Voucher API] Invalid /active response. Expected JSON array payload.', {
      contentType,
      sample: typeof data === 'string' ? data.slice(0, 160) : data
    })
    throw new Error('Voucher API /active must return JSON array payload')
  }

  return response
}

// Get applicable vouchers for a specific order
export const getApplicableVouchers = (orderData) =>
  axios.post(`${VOUCHER_API}/applicable`, orderData)

// Apply voucher to order
export const applyVoucher = (voucherId, orderData) =>
  axios.post(`${VOUCHER_API}/${voucherId}/apply`, orderData)

// ============= AUTO-SELECT BEST VOUCHER LOGIC =============
/**
 * Calculate discount amount for a voucher
 * @param {Object} voucher - Voucher object
 * @param {Number} subtotal - Order subtotal
 * @returns {Number} - Discount amount
 */
export const calculateVoucherDiscount = (voucher, subtotal) => {
  const normalized = normalizeVoucherData(voucher)

  if (!normalized || subtotal < (normalized.hoaDonToiThieu || 0)) {
    return 0
  }

  let discount = 0
  
  if (normalized.hinhThucGiam) {
    // Percentage discount
    discount = (subtotal * normalized.giaTriGiamGia) / 100
    
    // Apply max discount cap if exists
    if (normalized.soTienGiamToiDa > 0) {
      discount = Math.min(discount, normalized.soTienGiamToiDa)
    }
  } else {
    // Fixed amount discount
    discount = normalized.giaTriGiamGia
  }

  // Discount cannot exceed subtotal
  return Math.min(discount, subtotal)
}

/**
 * Check if voucher is applicable to current order
 * @param {Object} voucher - Voucher object
 * @param {Number} subtotal - Order subtotal
 * @param {Number} customerId - Customer ID (optional)
 * @returns {Boolean}
 */
export const isVoucherApplicable = (voucher, subtotal, customerId = null) => {
  const normalized = normalizeVoucherData(voucher)
  if (!normalized) return false

  // Backend status flags are inconsistent across environments (e.g. active rows may return false/0),
  // so use date + remaining quantity as source of truth for storefront applicability.

  // Check date validity
  const now = new Date()
  const startDate = new Date(normalized.ngayBatDau)
  const endDate = new Date(normalized.ngayKetThuc)
  
  if (now < startDate || now > endDate) return false

  // Check minimum order value
  if (subtotal < (normalized.hoaDonToiThieu || 0)) return false

  // Check usage limit
  if (normalized.soLuongSuDung !== null && normalized.soLuongSuDung <= 0) return false

  // Check if voucher is for specific customer (loaiPhieuGiamGia = true means personal)
  if (normalized.loaiPhieuGiamGia && customerId) {
    // Would need to check if customer is in voucher's customer list
    // This requires additional API call or data
    return true // Simplified for now
  }

  return true
}

/**
 * Find the best voucher for current order
 * @param {Array} vouchers - Array of voucher objects
 * @param {Number} subtotal - Order subtotal
 * @param {Number} customerId - Customer ID (optional)
 * @returns {Object|null} - Best voucher or null
 */
export const findBestVoucher = (vouchers, subtotal, customerId = null) => {
  if (!Array.isArray(vouchers) || vouchers.length === 0) return null

  const applicableVouchers = vouchers
    .filter(v => isVoucherApplicable(v, subtotal, customerId))
    .map(v => ({
      ...v,
      discountAmount: calculateVoucherDiscount(v, subtotal)
    }))
    .sort((a, b) => b.discountAmount - a.discountAmount)

  return applicableVouchers.length > 0 ? applicableVouchers[0] : null
}

/**
 * Get voucher suggestions (vouchers that would apply if order value increases)
 * @param {Array} vouchers - Array of voucher objects
 * @param {Number} currentSubtotal - Current order subtotal
 * @param {Number} customerId - Customer ID (optional)
 * @returns {Array} - Array of suggestions with required amount to unlock
 */
export const getVoucherSuggestions = (vouchers, currentSubtotal, customerId = null) => {
  if (!Array.isArray(vouchers) || vouchers.length === 0) return []

  const now = new Date()
  const isActiveStatus = (voucher) => {
    if (typeof voucher?.trangThai === 'boolean') return voucher.trangThai === true
    if (typeof voucher?.trangThai === 'number') return voucher.trangThai === 1
    if (typeof voucher?.trangThai === 'string') {
      const normalized = voucher.trangThai.trim().toLowerCase()
      return (
        normalized === 'hoạt động'
        || normalized === 'dang hoat dong'
        || normalized === 'đang hoạt động'
        || normalized === 'active'
      )
    }
    return false
  }
  
  return vouchers
    .filter(v => {
      if (!v || !isActiveStatus(v)) return false
      
      const startDate = new Date(v.ngayBatDau)
      const endDate = new Date(v.ngayKetThuc)
      
      if (now < startDate || now > endDate) return false
      if (v.soLuongSuDung !== null && v.soLuongSuDung <= 0) return false
      
      // Only show vouchers that require more spending
      return currentSubtotal < (v.hoaDonToiThieu || 0)
    })
    .map(v => {
      const amountNeeded = (v.hoaDonToiThieu || 0) - currentSubtotal
      const potentialDiscount = calculateVoucherDiscount(v, v.hoaDonToiThieu || 0)
      
      return {
        voucher: v,
        amountNeeded,
        potentialDiscount,
        worthIt: potentialDiscount > amountNeeded * 0.1 // Worth if discount > 10% of needed amount
      }
    })
    .sort((a, b) => {
      // Prioritize by worth it, then by amount needed
      if (a.worthIt !== b.worthIt) return b.worthIt - a.worthIt
      return a.amountNeeded - b.amountNeeded
    })
    .slice(0, 3) // Top 3 suggestions
}

/**
 * Compare two vouchers to determine which is better
 * @param {Object} currentVoucher - Currently applied voucher
 * @param {Object} newVoucher - New voucher to compare
 * @param {Number} subtotal - Order subtotal
 * @returns {Object} - Comparison result
 */
export const compareVouchers = (currentVoucher, newVoucher, subtotal) => {
  if (!currentVoucher) {
    return {
      isBetter: true,
      currentDiscount: 0,
      newDiscount: calculateVoucherDiscount(newVoucher, subtotal),
      difference: calculateVoucherDiscount(newVoucher, subtotal)
    }
  }

  const currentDiscount = calculateVoucherDiscount(currentVoucher, subtotal)
  const newDiscount = calculateVoucherDiscount(newVoucher, subtotal)
  const difference = newDiscount - currentDiscount

  return {
    isBetter: difference > 0,
    currentDiscount,
    newDiscount,
    difference
  }
}