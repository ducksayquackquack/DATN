import axios from "axios"

const API = "http://localhost:8080/api/khuyen-mai"
const VOUCHER_API = "http://localhost:8080/api/phieu-giam-gia"

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
export const getAllVouchers = () =>
  axios.get(VOUCHER_API)

export const getVoucherById = (id) =>
  axios.get(`${VOUCHER_API}/${id}`)

export const createVoucher = (data) =>
  axios.post(VOUCHER_API, data)

export const updateVoucher = (id, data) =>
  axios.put(`${VOUCHER_API}/${id}`, data)

export const deleteVoucher = (id) =>
  axios.delete(`${VOUCHER_API}/${id}`)

// Get active vouchers for POS
export const getActiveVouchers = async () => {
  const response = await axios.get(`${VOUCHER_API}/active`, {
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
  if (!voucher || subtotal < (voucher.hoaDonToiThieu || 0)) {
    return 0
  }

  let discount = 0
  
  if (voucher.hinhThucGiam) {
    // Percentage discount
    discount = (subtotal * voucher.giaTriGiamGia) / 100
    
    // Apply max discount cap if exists
    if (voucher.soTienGiamToiDa > 0) {
      discount = Math.min(discount, voucher.soTienGiamToiDa)
    }
  } else {
    // Fixed amount discount
    discount = voucher.giaTriGiamGia
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
  if (!voucher) return false

  // Backend status flags are inconsistent across environments (e.g. active rows may return false/0),
  // so use date + remaining quantity as source of truth for storefront applicability.

  // Check date validity
  const now = new Date()
  const startDate = new Date(voucher.ngayBatDau)
  const endDate = new Date(voucher.ngayKetThuc)
  
  if (now < startDate || now > endDate) return false

  // Check minimum order value
  if (subtotal < (voucher.hoaDonToiThieu || 0)) return false

  // Check usage limit
  if (voucher.soLuongSuDung !== null && voucher.soLuongSuDung <= 0) return false

  // Check if voucher is for specific customer (loaiPhieuGiamGia = true means personal)
  if (voucher.loaiPhieuGiamGia && customerId) {
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