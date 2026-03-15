const stripVietnamese = (value) => {
  return String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
}

const normalizeKey = (value) => {
  return stripVietnamese(value)
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

const titleCase = (value) => {
  return String(value || '')
    .toLowerCase()
    .split(' ')
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export const normalizeAdminStatusLabel = (value) => {
  const raw = String(value || '').trim()
  if (!raw) return 'Không xác định'

  const normalized = normalizeKey(raw)

  if (normalized.includes('da huy') || normalized.includes('huy')) return 'Đã hủy'
  if (normalized.includes('hoan ve')) return 'Hoàn về'
  if (normalized.includes('that bai')) return 'Thất bại'
  if (normalized.includes('xoa') || normalized.includes('deleted') || normalized.includes('delete')) return 'Đã xóa'
  if (normalized.includes('ngung hoat dong') || normalized.includes('inactive') || normalized.includes('blocked')) return 'Ngừng hoạt động'
  if (normalized.includes('cho xac nhan')) return 'Chờ xác nhận'
  if (normalized.includes('cho xu ly') || normalized.includes('dang xu ly') || normalized.includes('processing')) return 'Đang xử lý'
  if (normalized.includes('dang giao')) return 'Đang giao'
  if (normalized.includes('da giao') || normalized.includes('hoan thanh')) return 'Hoàn thành'
  if (normalized.includes('cho lay hang')) return 'Chờ lấy hàng'
  if (normalized.includes('cho ket toan')) return 'Chờ kết toán'
  if (normalized.includes('hoat dong') || normalized.includes('active') || normalized.includes('kich hoat')) return 'Hoạt động'
  if (normalized === 'an') return 'Ngừng hoạt động'
  if (normalized.includes('dang ban')) return 'Hoạt động'
  if (normalized.includes('da phan cong')) return 'Đã phân công'

  return titleCase(raw.replace(/_/g, ' '))
}

const KNOWN_ORDER_STATUS_CODES = new Set([
  'CHO_XAC_NHAN',
  'CHO_LAY_HANG',
  'DANG_GIAO',
  'DA_GIAO',
  'GIAO_THAT_BAI',
  'HOAN_VE',
  'HOAN_THANH',
  'HUY'
])

export const normalizeOrderStatusCode = (...candidates) => {
  for (const candidate of candidates) {
    const raw = String(candidate || '').trim()
    if (!raw) continue

    const normalizedCode = raw
      .toUpperCase()
      .replace(/\s+/g, '_')

    if (KNOWN_ORDER_STATUS_CODES.has(normalizedCode)) {
      return normalizedCode
    }

    const key = normalizeKey(raw)

    if (key.includes('cho xac nhan')) return 'CHO_XAC_NHAN'
    if (key.includes('cho lay hang')) return 'CHO_LAY_HANG'
    if (key.includes('dang giao')) return 'DANG_GIAO'
    if (key.includes('da giao')) return 'DA_GIAO'
    if (key.includes('giao that bai') || key.includes('that bai')) return 'GIAO_THAT_BAI'
    if (key.includes('hoan ve')) return 'HOAN_VE'
    if (key.includes('hoan thanh')) return 'HOAN_THANH'
    if (key.includes('da huy') || key.includes('huy')) return 'HUY'
  }

  return 'CHO_XAC_NHAN'
}

export const getAdminStatusTone = (value) => {
  const normalized = normalizeKey(value)

  if (
    normalized.includes('da huy')
    || normalized.includes('huy')
    || normalized.includes('that bai')
    || normalized.includes('hoan ve')
    || normalized.includes('xoa')
    || normalized.includes('ngung hoat dong')
    || normalized.includes('inactive')
    || normalized.includes('blocked')
    || normalized.includes('deleted')
    || normalized.includes('delete')
  ) {
    return 'danger'
  }

  if (
    normalized.includes('dang xu ly')
    || normalized.includes('cho xu ly')
    || normalized.includes('cho xac nhan')
    || normalized.includes('processing')
    || normalized.includes('upcoming')
    || normalized.includes('chua bat dau')
    || normalized.includes('cho ket toan')
  ) {
    return 'warning'
  }

  if (
    normalized.includes('da giao')
    || normalized.includes('hoan thanh')
    || normalized.includes('hoat dong')
    || normalized.includes('active')
    || normalized.includes('kich hoat')
    || normalized.includes('dang ban')
    || normalized.includes('da phan cong')
  ) {
    return 'success'
  }

  return 'warning'
}