import { normalizeOrderStatusCode } from './adminStatus'

const CASH_METHOD_CODES = new Set(['CASH', 'COD', 'TIEN_MAT', 'TIENMAT', 'TM'])
const TRANSFER_METHOD_CODES = new Set(['BANK', 'VNPAY', 'CHUYEN_KHOAN', 'CHUYENKHOAN', 'CK', 'TRANSFER'])
const EXCLUDED_REVENUE_STATUS_CODES = new Set(['HUY', 'GIAO_THAT_BAI', 'HOAN_VE'])
const INCLUDED_REVENUE_STATUS_CODES = new Set(['HOAN_THANH', 'DA_GIAO'])

const normalizeDateKey = (dateValue) => {
  if (!dateValue) return ''
  const raw = String(dateValue).trim()
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw
  if (/^\d{4}-\d{2}-\d{2}T/.test(raw)) return raw.slice(0, 10)

  const match = raw.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/)
  if (!match) return ''

  const day = match[1].padStart(2, '0')
  const month = match[2].padStart(2, '0')
  return `${match[3]}-${month}-${day}`
}

export const resolveInvoiceEmployeeId = (invoice) => {
  const candidate = invoice?.nhanVienId ?? invoice?.idNhanVien
  const numeric = Number(candidate)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : null
}

export const resolveInvoiceDateKey = (invoice) => {
  return normalizeDateKey(invoice?.ngayTao || invoice?.ngayLap || invoice?.createdAt)
}

export const normalizePaymentMethodCode = (...candidates) => {
  for (const candidate of candidates) {
    const normalized = String(candidate || '').trim().toUpperCase().replace(/\s+/g, '_')
    if (normalized) return normalized
  }
  return ''
}

export const resolveOrderTypeCode = (invoice) => {
  const direct = String(invoice?.orderType || invoice?.orderTypeCode || invoice?.loaiDon || '').trim().toUpperCase()
  if (direct.includes('POS') || direct.includes('TAI_QUAY')) return 'POS'
  if (direct.includes('ONLINE')) return 'ONLINE'

  const note = String(invoice?.statusNote || '').toUpperCase()
  if (note.includes('[POS]')) return 'POS'
  if (note.includes('[ONLINE]')) return 'ONLINE'

  return 'ONLINE'
}

export const getRevenueBucket = (invoice) => {
  const paymentMethod = normalizePaymentMethodCode(invoice?.phuongThucThanhToan, invoice?.paymentMethod)
  if (TRANSFER_METHOD_CODES.has(paymentMethod)) return 'transfer'
  if (CASH_METHOD_CODES.has(paymentMethod)) {
    if (paymentMethod === 'COD') return 'cod'
    return 'cash'
  }

  const orderType = resolveOrderTypeCode(invoice)
  if (orderType === 'POS') return 'cash'

  return 'unknown'
}

export const resolveRevenueDateKey = (invoice, bucket = null) => {
  const normalizedBucket = String(bucket || getRevenueBucket(invoice)).trim().toLowerCase()

  if (normalizedBucket === 'cod') {
    return normalizeDateKey(invoice?.cashCollectedAt || invoice?.paidAt || invoice?.ngayTao || invoice?.ngayLap || invoice?.createdAt)
  }

  if (normalizedBucket === 'transfer') {
    return normalizeDateKey(invoice?.paidAt || invoice?.ngayTao || invoice?.ngayLap || invoice?.createdAt)
  }

  return normalizeDateKey(invoice?.paidAt || invoice?.ngayTao || invoice?.ngayLap || invoice?.createdAt)
}

export const isRevenueCountableOrder = (invoice) => {
  const amount = Number(invoice?.thanhTien || 0)
  if (!Number.isFinite(amount) || amount <= 0) return false

  const statusCode = normalizeOrderStatusCode(
    invoice?.orderStatusCode,
    invoice?.orderStatusName,
    invoice?.trangThai,
    invoice?.statusNote
  )

  if (EXCLUDED_REVENUE_STATUS_CODES.has(statusCode)) return false
  return INCLUDED_REVENUE_STATUS_CODES.has(statusCode)
}