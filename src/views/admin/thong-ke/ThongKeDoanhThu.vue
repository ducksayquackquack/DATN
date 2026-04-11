<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useToast } from '../../../composables/useToast'
import { getAllHoaDon, getHoaDonChiTiet, getHoaDonPage } from '../../../services/hoaDonService'
import { getAllSanPham, getSanPhamPage } from '../../../services/sanPhamService'
import { normalizeOrderStatusCode } from '../../../utils/adminStatus'
import { getRevenueBucket, isRevenueCountableOrder } from '../../../utils/orderRevenue'
import { getPhieuSummary } from '../../../services/phieuThuChiService'
import { resolveApiOrigin } from '../../../utils/apiOrigin'
import { getProductImageOverride } from '../../../utils/productImageOverrides'
import logoImage from '../../../assets/img/logo/new logo.png?url'
import bomberWindbreakerImage from '../../../assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-black.PNG?url'
import hoodieCamoImage from '../../../assets/img/Jackets/hoodie/hoodie-camo/hoodie-camo-black.PNG?url'
import bomberAstronautImage from '../../../assets/img/Jackets/bomber/bomber-astronaut/bomber-astronaut-black.PNG?url'
import hoodieZipSilkImage from '../../../assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-black.PNG?url'
import coachTigerStripeImage from '../../../assets/img/Jackets/coach/coach-tiger-stripe/coach-tiger-stripe.PNG?url'
import coachLeopardImage from '../../../assets/img/Jackets/coach/coach-leopard/coach-leopard.PNG?url'
import hoodieZipBoxyImage from '../../../assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-blue.PNG?url'
import bomberEmbroideredFuzzyImage from '../../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-black.PNG?url'
import coachCachNhietImage from '../../../assets/img/Jackets/coach/coach-cach-nhiet.jpg?url'
import coachLongSleeveImage from '../../../assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-black.PNG?url'
import bomberDaLonImage from '../../../assets/img/Jackets/bomber/bomber-da-lon.jpg?url'
import bomberDangLungImage from '../../../assets/img/Jackets/bomber/bomber-dang-lung.jpg?url'
import bomberGiaDaImage from '../../../assets/img/Jackets/bomber/bomber-gia-da.jpg?url'
import bomberCottonNheImage from '../../../assets/img/Jackets/bomber/bomber-nhe-cotton.jpg?url'
import hoodieDangHopImage from '../../../assets/img/Jackets/hoodie/hoodie-dang-hop.jpg?url'
import hoodieInHinhImage from '../../../assets/img/Jackets/hoodie/hoodie-in-hinh.jpg?url'
import hoodieKeoKhoaImage from '../../../assets/img/Jackets/hoodie/hoodie-keo-khoa.jpg?url'
import coachDaTronImage from '../../../assets/img/Jackets/coach/coach-da-asos.jpg?url'
import coachGiaDaImage from '../../../assets/img/Jackets/coach/coach-gia-da.jpg?url'
import coachLongCuuImage from '../../../assets/img/Jackets/coach/coach-long-cuu.jpg?url'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const loading = ref(false)
const invoices = ref([])
const statisticsError = ref('')
const chartTooltip = ref({ visible: false, x: 0, y: 0, label: '', revenue: 0, orders: 0 })
const BACKEND_URL = 'http://localhost:8080'
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, '')

const curatedTopProductImageRules = [
  { keywords: ['bomber', 'windbreaker'], image: bomberWindbreakerImage },
  { keywords: ['hoodie', 'camo'], image: hoodieCamoImage },
  { keywords: ['bomber', 'astronaut'], image: bomberAstronautImage },
  { keywords: ['hoodie', 'zip', 'silk'], image: hoodieZipSilkImage },
  { keywords: ['coach', 'tiger', 'stripe'], image: coachTigerStripeImage },
  { keywords: ['hoodie', 'zip', 'boxy'], image: hoodieZipBoxyImage },
  { keywords: ['bomber', 'embroidered', 'fuzzy'], image: bomberEmbroideredFuzzyImage },
  { keywords: ['coach', 'cach', 'nhiet'], image: coachCachNhietImage },
  { keywords: ['coach', 'da', 'tron'], image: coachDaTronImage },
  { keywords: ['coach', 'gia', 'da'], image: coachGiaDaImage },
  { keywords: ['coach', 'long', 'cuu'], image: coachLongCuuImage },
  { keywords: ['coach', 'leopard'], image: coachLeopardImage },
  { keywords: ['coach', 'longsleeve'], image: coachLongSleeveImage },
  { keywords: ['bomber', 'da', 'lon'], image: bomberDaLonImage },
  { keywords: ['bomber', 'dang', 'lung'], image: bomberDangLungImage },
  { keywords: ['bomber', 'gia', 'da'], image: bomberGiaDaImage },
  { keywords: ['bomber', 'cotton', 'nhe'], image: bomberCottonNheImage },
  { keywords: ['hoodie', 'dang', 'hop'], image: hoodieDangHopImage },
  { keywords: ['hoodie', 'in', 'hinh'], image: hoodieInHinhImage },
  { keywords: ['hoodie', 'keo', 'khoa'], image: hoodieKeoKhoaImage }
]

const routeBase = computed(() => {
  if (route.path.startsWith('/employee/')) return '/employee'
  return '/admin'
})

const normalizeProductKey = (value = '') =>
  String(value || '')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .replace(/[đĐ]/g, (char) => (char === 'đ' ? 'd' : 'D'))
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()

const stripTrailingBrandTokenForImage = (value = '') =>
  String(value || '')
    .trim()
    .replace(/[\s_-]*(dirty\s*wave|dirtywave)$/i, '')
    .trim()

const resolveTopProductFallbackByName = (name = '') => {
  const normalized = normalizeProductKey(name)
  if (!normalized) return ''

  const found = curatedTopProductImageRules.find((rule) =>
    rule.keywords.every((keyword) => normalized.includes(keyword))
  )
  return found?.image || ''
}

const isImageString = (value = '') => {
  const raw = String(value || '').trim()
  if (!raw) return false
  if (/^data:image\//i.test(raw)) return true

  const normalized = raw.replace(/\\/g, '/').split(/[?#]/)[0]
  if (/\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i.test(normalized)) return true
  if (normalized.startsWith('/uploads/') || normalized.startsWith('uploads/')) return true
  return /^https?:\/\//i.test(raw)
}

const isLogoLikeImage = (value = '') => {
  const normalized = String(value || '').toLowerCase().replace(/\\/g, '/')
  if (!normalized) return false
  return (
    normalized.includes('/logo/') ||
    normalized.includes('new logo') ||
    normalized.includes('default') ||
    normalized.includes('placeholder')
  )
}

const toImageUrl = (value = '') => {
  const raw = String(value || '').trim()
  if (!raw) return ''
  if (/^data:image\//i.test(raw)) return raw

  const normalized = raw.replace(/\\/g, '/')
  const uploadsMatch = normalized.match(/^.*?\/?(uploads\/.*)$/i)
  if (uploadsMatch?.[1]) return `${BACKEND_ORIGIN}/${uploadsMatch[1]}`
  if (/^https?:\/\//i.test(normalized)) return normalized
  if (normalized.startsWith('/')) return normalized
  if (normalized.startsWith('uploads/')) return `${BACKEND_ORIGIN}/${normalized}`
  return normalized.includes('/') ? `/${normalized.replace(/^\/+/, '')}` : normalized
}

const pickImageValue = (entry) => {
  if (!entry) return ''

  if (typeof entry === 'string') {
    return isImageString(entry) ? toImageUrl(entry) : ''
  }

  if (Array.isArray(entry)) {
    for (const child of entry) {
      const found = pickImageValue(child)
      if (found) return found
    }
    return ''
  }

  if (typeof entry === 'object') {
    const keys = ['anh', 'hinhAnh', 'image', 'imageUrl', 'images', 'listAnh', 'anhChinh', 'duongDanAnh', 'duongDan', 'url', 'path', 'tenAnh', 'src', 'thumbnail']
    for (const key of keys) {
      const found = pickImageValue(entry[key])
      if (found) return found
    }
  }

  return ''
}

const resolveTopProductImage = (product = null, fallbackName = '') => {
  const override = getProductImageOverride({ id: product?.id, maSanPham: product?.maSanPham })[0]
  const overrideUrl = toImageUrl(override)
  if (overrideUrl && !isLogoLikeImage(overrideUrl)) return overrideUrl

  const directImage = pickImageValue([product, product?.sanPhamChiTiets])
  if (directImage && !isLogoLikeImage(directImage)) return directImage

  const nameFallback = resolveTopProductFallbackByName(product?.tenSanPham || fallbackName)
  if (nameFallback) return nameFallback

  if (product?.maSanPham) {
    const code = String(product.maSanPham).trim().toUpperCase()
    const codeMap = {
      SP001: bomberDaLonImage,
      SP002: bomberDangLungImage,
      SP003: bomberGiaDaImage,
      SP004: bomberCottonNheImage,
      SP005: hoodieDangHopImage,
      SP006: hoodieInHinhImage,
      SP007: hoodieKeoKhoaImage,
      SP008: coachCachNhietImage,
      SP009: coachDaTronImage,
      SP010: coachGiaDaImage,
      SP011: coachLongCuuImage,
      SP012: bomberAstronautImage,
      SP013: bomberEmbroideredFuzzyImage,
      SP014: bomberWindbreakerImage,
      SP015: coachLeopardImage,
      SP016: coachLongSleeveImage,
      SP017: coachTigerStripeImage,
      SP018: hoodieCamoImage,
      SP019: hoodieZipBoxyImage,
      SP020: hoodieZipSilkImage
    }
    if (codeMap[code]) return codeMap[code]
  }

  return logoImage
}

const handleTopProductImageError = (event) => {
  if (event?.target) {
    event.target.src = logoImage
  }
}

const openTopProductDetail = (product) => {
  const productId = Number(product?.productId || 0)
  if (!Number.isFinite(productId) || productId <= 0) {
    toast.warning('Không tìm thấy chi tiết sản phẩm cho dòng này.')
    return
  }

  router.push({
    path: `${routeBase.value}/san-pham/form/${productId}`,
    query: {
      from: 'thong-ke',
      hasColors: product?.hasColors ? '1' : '0'
    }
  })
}

const toInputDate = (date) => {
  const d = new Date(date)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const fmtDateDisplay = (val) => {
  if (!val) return ""
  const parts = val.split("-")
  if (parts.length !== 3) return val
  return `${parts[2]}/${parts[1]}/${parts[0]}`
}

const initialStartDate = (() => {
  const d = new Date()
  return toInputDate(new Date(d.getFullYear(), 0, 1))
})()

const initialEndDate = (() => {
  const d = new Date()
  return toInputDate(new Date(d.getFullYear(), 11, 31))
})()

const filters = ref({
  startDate: initialStartDate,
  endDate: initialEndDate,
  period: 'month'
})

const appliedFilters = ref({ ...filters.value })
const products = ref([])
const productNameByVariantId = ref(new Map())
const productNameByProductId = ref(new Map())
const productNameByMaSanPham = ref(new Map())
const hideTopProductRevenue = ref(false)
const searchMeta = ref({
  hasSearched: false,
  total: 0
})

const applyFilters = () => {
  if (filters.value.startDate && filters.value.endDate && filters.value.startDate > filters.value.endDate) {
    toast.warning('Từ ngày không được lớn hơn Đến ngày')
    return false
  }

  appliedFilters.value = { ...filters.value }
  return true
}

function onBarHover(event, item) {
  const outer = event.currentTarget.closest('.chart-outer')
  const outerRect = outer.getBoundingClientRect()
  const barEl = event.currentTarget.querySelector('.chart-bar')
  const barRect = barEl ? barEl.getBoundingClientRect() : event.currentTarget.getBoundingClientRect()
  const wrapperRect = event.currentTarget.getBoundingClientRect()
  const tooltipTop = Math.max(barRect.top - outerRect.top - 8, 20)
  chartTooltip.value = {
    visible: true,
    x: wrapperRect.left - outerRect.left + wrapperRect.width / 2,
    y: tooltipTop,
    label: item.month,
    revenue: item.revenue,
    orders: item.orders
  }
}

const barHeightStyle = (item) => {
  const pct = maxRevenue.value > 0 ? (Number(item?.revenue || 0) / maxRevenue.value) * 100 : 0
  const safePct = Number.isFinite(pct) ? pct : 0
  return {
    height: `max(${safePct}%, 24px)`
  }
}

const barValueVisible = (item) => {
  if (!item?.revenue || item.revenue <= 0) return false
  const pct = maxRevenue.value > 0 ? (item.revenue / maxRevenue.value) * 100 : 0
  return pct >= 14
}

const formatCompact = (value) => {
  const v = Number(value) || 0
  if (v >= 1_000_000_000) return `${(v / 1_000_000_000).toFixed(1)}T`
  if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(1)}M`
  if (v >= 1_000) return `${Math.round(v / 1_000)}K`
  return `${v}đ`
}

const COMPLETED_CODES = new Set(['HOAN_THANH', 'DA_GIAO'])
const CANCELLED_CODES = new Set(['HUY'])

const getInvoiceDetails = (invoice) => {
  if (!invoice || typeof invoice !== 'object') return []

  const detailCollections = [
    invoice.chiTietHoaDons,
    invoice.hoaDonChiTiets,
    invoice.chiTietDonHangs,
    invoice.chiTiets,
    invoice._chiTietHoaDons
  ]

  const found = detailCollections.find((items) => Array.isArray(items) && items.length > 0)
  return found || []
}

const parseDate = (value) => {
  if (!value) return null
  const date = value instanceof Date ? value : new Date(value)
  if (!Number.isNaN(date.getTime())) return date

  if (typeof value === 'string') {
    const match = value
      .trim()
      .match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})(?:\s+(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?)?$/)

    if (match) {
      const day = Number(match[1])
      const month = Number(match[2])
      const year = Number(match[3])
      const hour = Number(match[4] || 0)
      const minute = Number(match[5] || 0)
      const second = Number(match[6] || 0)

      const fallback = new Date(year, month - 1, day, hour, minute, second)
      if (!Number.isNaN(fallback.getTime())) return fallback
    }
  }

  return null
}

const inRangeInvoices = computed(() => {
  const start = parseDate(appliedFilters.value.startDate)
  const end = parseDate(appliedFilters.value.endDate)

  return invoices.value.filter((item) => {
    const d = parseDate(item.ngayTao)
    if (!d) return false
    if (start && d < start) return false
    if (end) {
      const endOfDay = new Date(end)
      endOfDay.setHours(23, 59, 59, 999)
      if (d > endOfDay) return false
    }
    return true
  })
})

const invoiceRevenue = (i) => {
  const gross = Number(i?.thanhTien) || Number(i?.tongTien) || Number(i?.tongTienThanhToan) || 0
  const shipping = Number(i?.phiVanChuyen || i?.phiShip || i?.shippingFee || 0)
  return Math.max(0, gross - shipping)
}

const stats = computed(() => {
  const source = inRangeInvoices.value
  const totalRevenue = source.reduce((sum, i) => sum + invoiceRevenue(i), 0)
  const totalOrders = source.length
  const completedOrders = source.filter((i) => {
    const code = normalizeOrderStatusCode(i.orderStatusCode, i.orderStatusName, i.trangThai)
    return COMPLETED_CODES.has(code)
  }).length
  const cancelledOrders = source.filter((i) => {
    const code = normalizeOrderStatusCode(i.orderStatusCode, i.orderStatusName, i.trangThai)
    return CANCELLED_CODES.has(code)
  }).length
  const pendingOrders = totalOrders - completedOrders - cancelledOrders

  return {
    totalRevenue,
    totalOrders,
    avgOrderValue: totalOrders > 0 ? Math.round(totalRevenue / totalOrders) : 0,
    completedOrders,
    pendingOrders: Math.max(pendingOrders, 0),
    cancelledOrders
  }
})

/* ── Payment method breakdown ── */
const paymentBreakdown = computed(() => {
  let cash = 0, transfer = 0
  inRangeInvoices.value.forEach((i) => {
    if (!isRevenueCountableOrder(i)) return
    const rev = invoiceRevenue(i)
    const bucket = getRevenueBucket(i)
    if (bucket === 'transfer') { transfer += rev }
    else { cash += rev }
  })
  return { cash, transfer }
})

/* ── Phiếu thu / chi summary ── */
const phieuSummary = ref({ thu: { total: 0, count: 0 }, chi: { total: 0, count: 0 } })

const fetchPhieuSummary = async () => {
  try {
    const res = await getPhieuSummary(appliedFilters.value.startDate, appliedFilters.value.endDate)
    phieuSummary.value = res.data || { thu: { total: 0, count: 0 }, chi: { total: 0, count: 0 } }
  } catch {
    // Node backend may be offline — silently ignore
    phieuSummary.value = { thu: { total: 0, count: 0 }, chi: { total: 0, count: 0 } }
  }
}

/* ── Net cash received ── */
const netCash = computed(() => {
  return paymentBreakdown.value.cash + phieuSummary.value.thu.total - phieuSummary.value.chi.total
})

const revenueByPeriod = computed(() => {
  const period = appliedFilters.value.period || 'month'
  const bucket = new Map()

  const getISOWeek = (date) => {
    const tmp = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
    tmp.setUTCDate(tmp.getUTCDate() + 4 - (tmp.getUTCDay() || 7))
    const yearStart = new Date(Date.UTC(tmp.getUTCFullYear(), 0, 1))
    return Math.ceil((((tmp - yearStart) / 86400000) + 1) / 7)
  }

  const toKey = (d) => {
    if (period === 'day') return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    if (period === 'week') return `${d.getFullYear()}-W${String(getISOWeek(d)).padStart(2, '0')}`
    if (period === 'year') return `${d.getFullYear()}`
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  }

  inRangeInvoices.value.forEach((item) => {
    const d = parseDate(item.ngayTao)
    if (!d) return
    const key = toKey(d)
    const current = bucket.get(key) || { revenue: 0, orders: 0 }
    current.revenue += invoiceRevenue(item)
    current.orders += 1
    bucket.set(key, current)
  })

  // Fill missing periods in selected range so the chart timeline is consistent.
  const start = parseDate(appliedFilters.value.startDate)
  const end = parseDate(appliedFilters.value.endDate)

  if (start && end) {
    const empty = () => ({ revenue: 0, orders: 0 })
    if (period === 'day') {
      const cursor = new Date(start.getFullYear(), start.getMonth(), start.getDate())
      while (cursor <= end) {
        const key = toKey(cursor)
        if (!bucket.has(key)) bucket.set(key, empty())
        cursor.setDate(cursor.getDate() + 1)
      }
    } else if (period === 'week') {
      const cursor = new Date(start.getFullYear(), start.getMonth(), start.getDate())
      cursor.setDate(cursor.getDate() - ((cursor.getDay() + 6) % 7)) // rewind to Monday
      while (cursor <= end) {
        const key = toKey(cursor)
        if (!bucket.has(key)) bucket.set(key, empty())
        cursor.setDate(cursor.getDate() + 7)
      }
    } else if (period === 'year') {
      for (let y = start.getFullYear(); y <= end.getFullYear(); y++) {
        const key = `${y}`
        if (!bucket.has(key)) bucket.set(key, empty())
      }
    } else {
      const cursor = new Date(start.getFullYear(), start.getMonth(), 1)
      const endMonth = new Date(end.getFullYear(), end.getMonth(), 1)
      while (cursor <= endMonth) {
        const key = toKey(cursor)
        if (!bucket.has(key)) bucket.set(key, empty())
        cursor.setMonth(cursor.getMonth() + 1)
      }
    }
  }

  const allKeys = [...bucket.keys()]

  const rows = allKeys
    .sort((a, b) => a.localeCompare(b))
    .map((key, index) => {
      const data = bucket.get(key)
      let label = key
      if (period === 'day') {
        const parts = key.split('-')
        label = `${parts[2]}/${parts[1]}`
      } else if (period === 'week') {
        label = `Tuần ${index + 1}`
      } else if (period === 'year') {
        label = key
      } else {
        const [, month] = key.split('-')
        label = `T${Number(month)}`
      }
      return { month: label, revenue: data.revenue, orders: data.orders }
    })

  return rows
})

const maxRevenue = computed(() => {
  return Math.max(...revenueByPeriod.value.map((item) => item.revenue), 1)
})

const hasRevenueData = computed(() => {
  return revenueByPeriod.value.length > 0 && revenueByPeriod.value.some((item) => item.revenue > 0)
})

const monthOverMonth = computed(() => {
  const pct = (curr, prev) => {
    if (!prev || prev === 0) return curr > 0 ? { value: 100, dir: 'up' } : { value: 0, dir: 'flat' }
    const change = ((curr - prev) / prev) * 100
    return { value: Math.abs(change).toFixed(1), dir: change > 0 ? 'up' : change < 0 ? 'down' : 'flat' }
  }

  // Bucket orders + revenue by month key
  const monthBucket = new Map()
  inRangeInvoices.value.forEach((item) => {
    const d = parseDate(item.ngayTao)
    if (!d) return
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    const cur = monthBucket.get(key) || { revenue: 0, orders: 0 }
    cur.revenue += invoiceRevenue(item)
    cur.orders += 1
    monthBucket.set(key, cur)
  })

  const keys = [...monthBucket.keys()].sort()
  if (keys.length < 2) return { revenue: { value: 0, dir: 'flat' }, orders: { value: 0, dir: 'flat' }, avg: { value: 0, dir: 'flat' } }

  const curr = monthBucket.get(keys[keys.length - 1])
  const prev = monthBucket.get(keys[keys.length - 2])
  const currAvg = curr.orders > 0 ? curr.revenue / curr.orders : 0
  const prevAvg = prev.orders > 0 ? prev.revenue / prev.orders : 0

  return {
    revenue: pct(curr.revenue, prev.revenue),
    orders: pct(curr.orders, prev.orders),
    avg: pct(currAvg, prevAvg)
  }
})

const trendCardClass = (dir) => {
  if (dir === 'up') return 'trend-up'
  if (dir === 'down') return 'trend-down'
  return 'trend-flat'
}

const topProducts = computed(() => {
  const productMap = new Map()
  const productLookupById = new Map()
  const productLookupByName = new Map()
  const productLookupByCode = new Map()

  const hasProductColors = (product) => {
    const variants = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []
    const colorNames = new Set(
      variants
        .map((variant) => normalizeProductKey(
          variant?.mauSac?.tenMau ||
          variant?.mauSac?.tenMauSac ||
          variant?.mauSac?.name ||
          ''
        ))
        .filter(Boolean)
    )
    return colorNames.size > 1
  }

  const getProductStock = (product) => {
    const variants = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []
    if (variants.length > 0) {
      return variants.reduce((sum, v) => sum + (Number(v?.soLuong) || 0), 0)
    }
    return Number(product?.soLuong ?? product?.ton ?? 0) || 0
  }

  // Seed full catalog so admin can see all current products.
  products.value.forEach((product) => {
    const name = product?.tenSanPham || product?.maSanPham || `SP #${product?.id || 'N/A'}`
    const productId = Number(product?.id) || 0
    const normalizedName = normalizeProductKey(stripTrailingBrandTokenForImage(name))
    const normalizedCode = String(product?.maSanPham || '').trim().toUpperCase()
    const productKey = productId > 0 ? `id:${productId}` : `name:${normalizedName || name}`

    if (productId > 0) productLookupById.set(productId, product)
    if (normalizedName) productLookupByName.set(normalizedName, product)
    if (normalizedCode) productLookupByCode.set(normalizedCode, product)

    if (!productMap.has(productKey)) {
      productMap.set(productKey, {
        name,
        sold: 0,
        stock: getProductStock(product),
        revenue: 0,
        image: resolveTopProductImage(product),
        productId: productId || null,
        hasColors: hasProductColors(product)
      })
    }
  })

  inRangeInvoices.value
    .filter((inv) => {
      const code = normalizeOrderStatusCode(inv.orderStatusCode, inv.orderStatusName, inv.trangThai)
      return COMPLETED_CODES.has(code)
    })
    .forEach((invoice) => {
    const details = getInvoiceDetails(invoice)
    details.forEach((d) => {
      const rawProductId = Number(d?.sanPhamId ?? d?.sanPham?.id ?? d?.sanPhamChiTiet?.sanPham?.id ?? 0)
      const productId = Number.isFinite(rawProductId) && rawProductId > 0 ? rawProductId : 0
      // Try to get product name from various possible fields
      let name = null
      
      // Check nested sanPhamChiTiet.sanPham structure
      if (d?.sanPhamChiTiet?.sanPham?.tenSanPham) {
        name = d.sanPhamChiTiet.sanPham.tenSanPham
      }
      // Check direct sanPham reference
      else if (d?.sanPham?.tenSanPham) {
        name = d.sanPham.tenSanPham
      }
      // Check direct tenSanPham field
      else if (d?.tenSanPham) {
        name = d.tenSanPham
      }
      // Fallback to product code if available
      else if (d?.sanPhamChiTiet?.sanPham?.maSanPham) {
        name = d.sanPhamChiTiet.sanPham.maSanPham
      }
      else if (d?.sanPham?.maSanPham) {
        name = d.sanPham.maSanPham
      }
      else if (d?.maSanPham) {
        name = productNameByMaSanPham.value.get(d.maSanPham) || d.maSanPham
      }
      
      // If still no name, use ID as last resort
      if (!name) {
        const variantId = d?.sanPhamChiTietId ?? d?.idSanPhamChiTiet ?? d?.spctId ?? d?.sanPhamChiTiet?.id

        if (variantId && productNameByVariantId.value.has(variantId)) {
          name = productNameByVariantId.value.get(variantId)
        } else if (productId && productNameByProductId.value.has(productId)) {
          name = productNameByProductId.value.get(productId)
        }
      }

      if (!name) {
        const fallbackId = d?.sanPhamChiTietId || d?.sanPhamId || d?.id
        name = fallbackId ? `Sản phẩm #${fallbackId}` : 'Sản phẩm không xác định'
      }

      const sold = Number(d?.soLuong ?? d?.quantity ?? 0) || 0
      const lineRevenue = Number(d?.thanhTien ?? d?.tongTien) || 0
      const unitPrice = Number(d?.donGia ?? d?.giaBan ?? d?.sanPhamChiTiet?.giaBan) || 0
      const revenue = lineRevenue > 0 ? lineRevenue : sold * unitPrice

      const normalizedName = normalizeProductKey(stripTrailingBrandTokenForImage(name))
      const productCode = String(
        d?.sanPhamChiTiet?.sanPham?.maSanPham ||
        d?.sanPham?.maSanPham ||
        d?.maSanPham ||
        ''
      ).trim().toUpperCase()
      const productKey = productId > 0 ? `id:${productId}` : `name:${normalizedName || name}`
      const matchedProduct =
        (productId && productLookupById.get(productId)) ||
        (productCode && productLookupByCode.get(productCode)) ||
        (normalizedName && productLookupByName.get(normalizedName)) ||
        null
      
      const prev = productMap.get(productKey) || {
        name,
        sold: 0,
        stock: 0,
        revenue: 0,
        image: resolveTopProductImage(matchedProduct, name),
        productId: Number(matchedProduct?.id || productId) || null,
        hasColors: hasProductColors(matchedProduct)
      }

      productMap.set(productKey, {
        name: prev.name || name,
        sold: prev.sold + sold,
        stock: prev.stock || 0,
        revenue: prev.revenue + revenue,
        image: prev.image || resolveTopProductImage(matchedProduct, name),
        productId: prev.productId || Number(matchedProduct?.id || productId) || null,
        hasColors: prev.hasColors || hasProductColors(matchedProduct)
      })
    })
  })

  // Show all products — including those with zero sales
  const rows = [...productMap.values()]
    .sort((a, b) => {
      if (b.sold !== a.sold) return b.sold - a.sold
      return a.name.localeCompare(b.name, 'vi')
    })
    .map((row) => ({
      ...row,
      image: row.image || resolveTopProductImage(null, row.name),
      productId: row.productId || null,
      hasColors: Boolean(row.hasColors)
    }))

  return rows.length > 0 ? rows : [
    { name: 'Chưa có dữ liệu sản phẩm', sold: 0, stock: 0, revenue: 0, image: logoImage, productId: null, hasColors: false }
  ]
})

const yAxisTicks = computed(() => {
  const max = maxRevenue.value || 1
  const steps = 4
  return Array.from({ length: steps + 1 }, (_, idx) => {
    const value = Math.round((max * (steps - idx)) / steps)
    return {
      label: formatCurrency(value)
    }
  })
})

const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(value || 0)
}

const isBackendUnavailableError = (error) => {
  if (!error) return false
  if (!error?.response) return true
  const status = Number(error?.response?.status || 0)
  return !status || status >= 500
}

const getStatisticsErrorMessage = (error) => {
  if (isBackendUnavailableError(error)) {
    return `Không kết nối được backend tại ${BACKEND_URL}. Hãy khởi động API rồi tải lại trang.`
  }
  return error?.response?.data?.message || 'Không tải được dữ liệu thống kê doanh thu.'
}

const applyStatisticsFilters = async () => {
  if (!applyFilters()) return
  searchMeta.value = {
    hasSearched: true,
    total: searchMeta.value.total
  }
  fetchPhieuSummary()
  await fetchStatistics(false)
}

const loadStatistics = () => {
  fetchPhieuSummary()
  fetchStatistics(true)
}

const exportReport = async () => {
  toast.info('Đang xuất báo cáo...')
  try {
    const xlsxModule = await import('xlsx')
    const XLSX = xlsxModule.default || xlsxModule
    const wb = XLSX.utils.book_new()

    const periodLabel = { day: 'Theo ngày', week: 'Theo tuần', month: 'Theo tháng', year: 'Theo năm' }[appliedFilters.value.period] || 'Theo tháng'
    const exportAt = new Date()
    const exportAtText = `${String(exportAt.getDate()).padStart(2, '0')}/${String(exportAt.getMonth() + 1).padStart(2, '0')}/${exportAt.getFullYear()} ${String(exportAt.getHours()).padStart(2, '0')}:${String(exportAt.getMinutes()).padStart(2, '0')}`

    const applyCurrencyFormat = (sheet, colLetters, startRow, endRow) => {
      colLetters.forEach((col) => {
        for (let row = startRow; row <= endRow; row += 1) {
          const cellRef = `${col}${row}`
          if (sheet[cellRef] && typeof sheet[cellRef].v === 'number') {
            sheet[cellRef].z = '#,##0 "đ"'
          }
        }
      })
    }

    // --- Sheet 1: Tổng quan ---
    const overviewData = [
      ['BÁO CÁO THỐNG KÊ DOANH THU'],
      ['Ngày xuất', exportAtText],
      ['Từ ngày', fmtDateDisplay(appliedFilters.value.startDate)],
      ['Đến ngày', fmtDateDisplay(appliedFilters.value.endDate)],
      ['Khoảng thời gian', periodLabel],
      [],
      ['CHỈ SỐ', 'GIÁ TRỊ'],
      ['Tổng doanh thu', stats.value.totalRevenue],
      ['Tổng đơn hàng', stats.value.totalOrders],
      ['Giá trị TB/đơn', stats.value.avgOrderValue],
      ['Đơn hoàn thành', stats.value.completedOrders],
      ['Đơn chờ xử lý', stats.value.pendingOrders],
      ['Đơn đã hủy', stats.value.cancelledOrders],
      [],
      ['THANH TOÁN', 'GIÁ TRỊ'],
      ['Tiền mặt (COD + POS)', paymentBreakdown.value.cash],
      ['Chuyển khoản (VNPay/Bank)', paymentBreakdown.value.transfer],
      ['Phiếu thu', phieuSummary.value.thu.total],
      ['Phiếu chi', phieuSummary.value.chi.total],
      ['Tiền mặt nhận thực tế', netCash.value],
    ]
    const ws1 = XLSX.utils.aoa_to_sheet(overviewData)
    ws1['!cols'] = [{ wch: 36 }, { wch: 24 }]
    ws1['!merges'] = [XLSX.utils.decode_range('A1:B1')]
    applyCurrencyFormat(ws1, ['B'], 8, 10)
    applyCurrencyFormat(ws1, ['B'], 16, 20)
    XLSX.utils.book_append_sheet(wb, ws1, 'Tổng quan')

    // --- Sheet 2: Doanh thu theo kỳ ---
    const periodRows = revenueByPeriod.value.map((row, idx) => [idx + 1, row.month, row.revenue, row.orders])
    const ws2 = XLSX.utils.aoa_to_sheet([
      ['STT', periodLabel.replace('Theo ', ''), 'Doanh thu (VNĐ)', 'Số đơn hàng'],
      ...periodRows
    ])
    ws2['!cols'] = [{ wch: 6 }, { wch: 18 }, { wch: 20 }, { wch: 14 }]
    ws2['!autofilter'] = { ref: `A1:D${Math.max(periodRows.length + 1, 2)}` }
    applyCurrencyFormat(ws2, ['C'], 2, periodRows.length + 1)
    XLSX.utils.book_append_sheet(wb, ws2, 'Doanh thu theo kỳ')

    // --- Sheet 3: Top sản phẩm ---
    const productRows = topProducts.value.map((p, i) => [i + 1, p.name, p.sold, p.revenue, p.stock])
    const ws3 = XLSX.utils.aoa_to_sheet([
      ['STT', 'Sản phẩm', 'Số lượng bán', 'Doanh thu (VNĐ)', 'Tồn kho'],
      ...productRows
    ])
    ws3['!cols'] = [{ wch: 6 }, { wch: 44 }, { wch: 16 }, { wch: 20 }, { wch: 12 }]
    ws3['!autofilter'] = { ref: `A1:E${Math.max(productRows.length + 1, 2)}` }
    applyCurrencyFormat(ws3, ['D'], 2, productRows.length + 1)
    XLSX.utils.book_append_sheet(wb, ws3, 'Top sản phẩm')

    // --- Sheet 4: Doanh thu theo trạng thái ---
    const statusMap = new Map()
    inRangeInvoices.value.forEach((inv) => {
      const statusCode = normalizeOrderStatusCode(inv.orderStatusCode, inv.orderStatusName, inv.trangThai)
      const label = statusCode === 'HOAN_THANH' || statusCode === 'DA_GIAO'
        ? 'Hoàn thành'
        : statusCode === 'HUY'
          ? 'Đã hủy'
          : 'Chờ xử lý'
      const current = statusMap.get(label) || { count: 0, revenue: 0 }
      current.count += 1
      current.revenue += invoiceRevenue(inv)
      statusMap.set(label, current)
    })
    const statusRows = [...statusMap.entries()].map(([label, data], idx) => [idx + 1, label, data.count, data.revenue])
    const ws4 = XLSX.utils.aoa_to_sheet([
      ['STT', 'Trạng thái', 'Số hóa đơn', 'Doanh thu (VNĐ)'],
      ...statusRows
    ])
    ws4['!cols'] = [{ wch: 6 }, { wch: 18 }, { wch: 14 }, { wch: 20 }]
    ws4['!autofilter'] = { ref: `A1:D${Math.max(statusRows.length + 1, 2)}` }
    applyCurrencyFormat(ws4, ['D'], 2, statusRows.length + 1)
    XLSX.utils.book_append_sheet(wb, ws4, 'Theo trạng thái')

    // --- Sheet 5: Chi tiết hóa đơn ---
    const invoiceRows = inRangeInvoices.value.map((inv, idx) => {
      const d = parseDate(inv.ngayTao)
      const dateStr = d
        ? `${String(d.getDate()).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')}/${d.getFullYear()}`
        : ''
      const statusCode = normalizeOrderStatusCode(inv.orderStatusCode, inv.orderStatusName, inv.trangThai)
      const statusLabel = statusCode === 'HOAN_THANH' || statusCode === 'DA_GIAO'
        ? 'Hoàn thành'
        : statusCode === 'HUY'
          ? 'Đã hủy'
          : 'Chờ xử lý'
      const details = getInvoiceDetails(inv)
      const qty = details.reduce((sum, item) => sum + (Number(item?.soLuong ?? item?.quantity ?? 0) || 0), 0)
      const gross = Number(inv?.thanhTien) || Number(inv?.tongTien) || Number(inv?.tongTienThanhToan) || 0
      const shipping = Number(inv?.phiVanChuyen || inv?.phiShip || inv?.shippingFee || 0)

      return [
        idx + 1,
        inv.maHoaDon || inv.id || '',
        dateStr,
        inv.tenKhachHang || inv.khachHang?.tenKhachHang || '',
        inv.soDienThoai || inv.khachHang?.soDienThoai || '',
        statusLabel,
        inv.phuongThucThanhToan || inv.paymentMethod || '',
        gross,
        shipping,
        invoiceRevenue(inv),
        details.length,
        qty
      ]
    })
    const ws5 = XLSX.utils.aoa_to_sheet([
      ['STT', 'Mã HĐ', 'Ngày tạo', 'Khách hàng', 'SĐT', 'Trạng thái', 'PT thanh toán', 'Tổng tiền gộp', 'Phí vận chuyển', 'Doanh thu thuần', 'Số dòng SP', 'Tổng SL'],
      ...invoiceRows
    ])
    ws5['!cols'] = [
      { wch: 6 },
      { wch: 16 },
      { wch: 12 },
      { wch: 24 },
      { wch: 14 },
      { wch: 14 },
      { wch: 18 },
      { wch: 18 },
      { wch: 16 },
      { wch: 18 },
      { wch: 12 },
      { wch: 10 }
    ]
    ws5['!autofilter'] = { ref: `A1:L${Math.max(invoiceRows.length + 1, 2)}` }
    applyCurrencyFormat(ws5, ['H', 'I', 'J'], 2, invoiceRows.length + 1)
    XLSX.utils.book_append_sheet(wb, ws5, 'Chi tiết hóa đơn')

    const startStr = appliedFilters.value.startDate?.replace(/-/g, '') || ''
    const endStr = appliedFilters.value.endDate?.replace(/-/g, '') || ''
    const fileName = `BaoCao_DoanhThu_ChiTiet_${startStr}_${endStr}.xlsx`
    XLSX.writeFile(wb, fileName)
    toast.success('Đã xuất báo cáo Excel chi tiết thành công!')
  } catch (err) {
    console.error('Export failed:', err)
    toast.error('Không thể xuất báo cáo. Vui lòng thử lại.')
  }
}

const extractList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

const fetchAllPages = async (fetchPageFn, fallbackFn) => {
  const firstRes = await fallbackFn()
  const firstPayload = firstRes?.data
  const firstItems = extractList(firstPayload)

  if (!firstPayload || typeof firstPayload !== 'object' || !Array.isArray(firstPayload.content)) {
    return firstItems
  }

  const totalPages = Number(firstPayload.totalPages) || 1
  if (totalPages <= 1) return firstItems

  const pageCalls = []
  for (let page = 1; page < totalPages; page += 1) {
    pageCalls.push(fetchPageFn(page))
  }

  const pageResponses = await Promise.all(pageCalls)
  const restItems = pageResponses.flatMap((res) => extractList(res?.data))
  return [...firstItems, ...restItems]
}

const fetchStatistics = async (showRefreshToast = false) => {
  loading.value = true
  statisticsError.value = ''
  try {
    const source = await fetchAllPages(
      (page) => getHoaDonPage({ page, size: 50 }),
      () => getAllHoaDon()
    )

    const enrichedInvoices = await Promise.all(
      source.map(async (invoice) => {
        const normalized = { ...invoice }

        if (getInvoiceDetails(normalized).length > 0 || !normalized?.id) {
          return normalized
        }

        try {
          const detailRes = await getHoaDonChiTiet(normalized.id)
          if (Array.isArray(detailRes.data)) {
            normalized._chiTietHoaDons = detailRes.data
          } else if (Array.isArray(detailRes.data?.content)) {
            normalized._chiTietHoaDons = detailRes.data.content
          } else {
            normalized._chiTietHoaDons = []
          }
        } catch {
          normalized._chiTietHoaDons = []
        }

        return normalized
      })
    )

    invoices.value = enrichedInvoices
    if (searchMeta.value.hasSearched) {
      searchMeta.value = {
        hasSearched: true,
        total: inRangeInvoices.value.length
      }
    }
    if (showRefreshToast) {
      toast.success('Đã cập nhật số liệu doanh thu')
    }
  } catch (err) {
    invoices.value = []
    statisticsError.value = getStatisticsErrorMessage(err)
    if (showRefreshToast) {
      toast.error(statisticsError.value)
    }
    console.error('Load statistics failed:', err)
  } finally {
    loading.value = false
  }
}

const fetchProducts = async () => {
  try {
    const source = await fetchAllPages(
      (page) => getSanPhamPage({ page, size: 100 }),
      () => getAllSanPham()
    )

    products.value = source.filter((p) => {
      const raw = String(p?.trangThai || '')
      const normalized = raw
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[đĐ]/g, (c) => (c === 'đ' ? 'd' : 'D'))
        .toLowerCase()
        .trim()
      return !normalized.includes('ngung') && normalized !== 'an' && normalized !== 'inactive'
    })

    const byVariant = new Map()
    const byProduct = new Map()

    source.forEach((product) => {
      const productName = product?.tenSanPham || product?.maSanPham || `SP #${product?.id || 'N/A'}`
      if (product?.id != null) {
        byProduct.set(product.id, productName)
      }

      const variants = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []
      variants.forEach((variant) => {
        if (variant?.id != null) {
          byVariant.set(variant.id, productName)
        }
      })
    })

    productNameByVariantId.value = byVariant
    productNameByProductId.value = byProduct

    const byMaSanPham = new Map()
    source.forEach((product) => {
      const productName = product?.tenSanPham || product?.maSanPham || `SP #${product?.id || 'N/A'}`
      if (product?.maSanPham) {
        byMaSanPham.set(product.maSanPham, productName)
      }
    })
    productNameByMaSanPham.value = byMaSanPham
  } catch (err) {
    if (!statisticsError.value) {
      statisticsError.value = getStatisticsErrorMessage(err)
    }
    console.error('Load products failed:', err)
  }
}

onMounted(() => {
  applyFilters()
  fetchProducts()
  fetchPhieuSummary()
  fetchStatistics(false)
  searchMeta.value = {
    hasSearched: true,
    total: 0
  }
})
</script>

<template>
  <main class="wrap">
    <div class="card thong-ke-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Thống kê doanh thu</h1>
        <p class="page-subtitle">Báo cáo doanh thu và đơn hàng</p>
      </div>
      <div class="header-actions">
        <button class="btn-secondary" @click="loadStatistics">
          <span class="material-icons-outlined">refresh</span>
          Làm mới
        </button>
        <button class="btn-primary" @click="exportReport">
          <span class="material-icons-outlined">download</span>
          Xuất báo cáo
        </button>
      </div>
    </div>
    <div v-if="statisticsError" class="stats-alert">
      {{ statisticsError }}
    </div>

    <!-- Filters -->
    <div class="filter-section">
      <div class="filter-group">
        <div class="filter-item">
          <label>Từ ngày</label>
          <div class="date-wrap">
            <input 
              v-model="filters.startDate" 
              type="date" 
              class="form-input date-native"
              @keydown.prevent
              @paste.prevent
            />
            <span class="date-overlay" v-if="filters.startDate">{{ fmtDateDisplay(filters.startDate) }}</span>
            <span class="date-overlay date-placeholder" v-else>dd/mm/yyyy</span>
          </div>
        </div>
        <div class="filter-item">
          <label>Đến ngày</label>
          <div class="date-wrap">
            <input 
              v-model="filters.endDate" 
              type="date" 
              class="form-input date-native"
              @keydown.prevent
              @paste.prevent
            />
            <span class="date-overlay" v-if="filters.endDate">{{ fmtDateDisplay(filters.endDate) }}</span>
            <span class="date-overlay date-placeholder" v-else>dd/mm/yyyy</span>
          </div>
        </div>
        <div class="filter-item">
          <label>Khoảng thời gian</label>
          <select v-model="filters.period" class="form-select">
            <option value="day">Theo ngày</option>
            <option value="week">Theo tuần</option>
            <option value="month">Theo tháng</option>
            <option value="year">Theo năm</option>
          </select>
        </div>
      </div>
      <button class="btn-search" :disabled="loading" @click="applyStatisticsFilters">
        <span class="material-icons-outlined">search</span>
        Tìm kiếm
      </button>
    </div>
    <p class="search-meta" v-if="searchMeta.hasSearched">
      Hiển thị {{ searchMeta.total }} đơn hàng trong khoảng thời gian đã chọn.
    </p>

    <!-- Main Stats -->
    <div class="stats-grid">
      <div class="stat-card" :class="trendCardClass(monthOverMonth.revenue.dir)">
        <div class="stat-header">
          <span class="stat-label">Tổng doanh thu</span>
          <div class="stat-icon revenue">
            <span class="material-icons-outlined">payments</span>
          </div>
        </div>
        <div class="stat-value">{{ formatCurrency(stats.totalRevenue) }}</div>
        <div v-if="monthOverMonth.revenue.dir !== 'flat'" class="stat-change" :class="monthOverMonth.revenue.dir === 'up' ? 'positive' : 'negative'">
          <span class="material-icons-outlined">{{ monthOverMonth.revenue.dir === 'up' ? 'trending_up' : 'trending_down' }}</span>
          {{ monthOverMonth.revenue.dir === 'up' ? '+' : '-' }}{{ monthOverMonth.revenue.value }}% so với tháng trước
        </div>
      </div>

      <div class="stat-card" :class="trendCardClass(monthOverMonth.orders.dir)">
        <div class="stat-header">
          <span class="stat-label">Tổng đơn hàng</span>
          <div class="stat-icon orders">
            <span class="material-icons-outlined">receipt_long</span>
          </div>
        </div>
        <div class="stat-value">{{ stats.totalOrders }}</div>
        <div v-if="monthOverMonth.orders.dir !== 'flat'" class="stat-change" :class="monthOverMonth.orders.dir === 'up' ? 'positive' : 'negative'">
          <span class="material-icons-outlined">{{ monthOverMonth.orders.dir === 'up' ? 'trending_up' : 'trending_down' }}</span>
          {{ monthOverMonth.orders.dir === 'up' ? '+' : '-' }}{{ monthOverMonth.orders.value }}% so với tháng trước
        </div>
      </div>

      <div class="stat-card" :class="trendCardClass(monthOverMonth.avg.dir)">
        <div class="stat-header">
          <span class="stat-label">Giá trị TB/đơn</span>
          <div class="stat-icon avg">
            <span class="material-icons-outlined">calculate</span>
          </div>
        </div>
        <div class="stat-value">{{ formatCurrency(stats.avgOrderValue) }}</div>
        <div v-if="monthOverMonth.avg.dir !== 'flat'" class="stat-change" :class="monthOverMonth.avg.dir === 'up' ? 'positive' : 'negative'">
          <span class="material-icons-outlined">{{ monthOverMonth.avg.dir === 'up' ? 'trending_up' : 'trending_down' }}</span>
          {{ monthOverMonth.avg.dir === 'up' ? '+' : '-' }}{{ monthOverMonth.avg.value }}% so với tháng trước
        </div>
      </div>
    </div>

    <!-- Order Status Stats -->
    <div class="stats-grid secondary">
      <div class="stat-card small">
        <div class="stat-icon-small success">
          <span class="material-icons-outlined">check_circle</span>
        </div>
        <div class="stat-content">
          <div class="stat-label">Đơn hoàn thành</div>
          <div class="stat-value-small">{{ stats.completedOrders }}</div>
        </div>
      </div>

      <div class="stat-card small">
        <div class="stat-icon-small warning">
          <span class="material-icons-outlined">pending</span>
        </div>
        <div class="stat-content">
          <div class="stat-label">Đơn chờ xử lý</div>
          <div class="stat-value-small">{{ stats.pendingOrders }}</div>
        </div>
      </div>

      <div class="stat-card small">
        <div class="stat-icon-small danger">
          <span class="material-icons-outlined">cancel</span>
        </div>
        <div class="stat-content">
          <div class="stat-label">Đơn đã hủy</div>
          <div class="stat-value-small">{{ stats.cancelledOrders }}</div>
        </div>
      </div>
    </div>

    <!-- Payment Method Breakdown -->
    <div class="breakdown-section">
      <div class="section-header">
        <h3>Phân loại thanh toán</h3>
        <p class="section-subtitle">Doanh thu theo phương thức thanh toán (đơn hoàn thành)</p>
      </div>
      <div class="breakdown-grid">
        <div class="breakdown-card cash">
          <div class="breakdown-icon">
            <span class="material-icons-outlined">account_balance_wallet</span>
          </div>
          <div class="breakdown-info">
            <div class="breakdown-label">Tiền mặt (COD + POS)</div>
            <div class="breakdown-value">{{ formatCurrency(paymentBreakdown.cash) }}</div>
          </div>
        </div>
        <div class="breakdown-card transfer">
          <div class="breakdown-icon">
            <span class="material-icons-outlined">account_balance</span>
          </div>
          <div class="breakdown-info">
            <div class="breakdown-label">Chuyển khoản (VNPay/Bank)</div>
            <div class="breakdown-value">{{ formatCurrency(paymentBreakdown.transfer) }}</div>
          </div>
        </div>
        <div class="breakdown-card thu">
          <div class="breakdown-icon">
            <span class="material-icons-outlined">arrow_downward</span>
          </div>
          <div class="breakdown-info">
            <div class="breakdown-label">Phiếu thu ({{ phieuSummary.thu.count }})</div>
            <div class="breakdown-value">{{ formatCurrency(phieuSummary.thu.total) }}</div>
          </div>
        </div>
        <div class="breakdown-card chi">
          <div class="breakdown-icon">
            <span class="material-icons-outlined">arrow_upward</span>
          </div>
          <div class="breakdown-info">
            <div class="breakdown-label">Phiếu chi ({{ phieuSummary.chi.count }})</div>
            <div class="breakdown-value">{{ formatCurrency(phieuSummary.chi.total) }}</div>
          </div>
        </div>
      </div>
      <div class="net-cash-bar">
        <span class="net-cash-label">Tiền mặt nhận thực tế</span>
        <span class="net-cash-value" :class="netCash >= 0 ? 'positive' : 'negative'">{{ formatCurrency(netCash) }}</span>
        <span class="net-cash-hint">= Tiền mặt bán hàng + Phiếu thu − Phiếu chi</span>
      </div>
    </div>

    <!-- Revenue Chart -->
    <div class="chart-section">
      <div class="section-header">
        <h3>Doanh thu theo {{ { day: 'ngày', week: 'tuần', month: 'tháng', year: 'năm' }[appliedFilters.period] || 'tháng' }}</h3>
        <p class="section-subtitle">Biểu đồ doanh thu theo khoảng thời gian đã chọn</p>
      </div>
      <div v-if="hasRevenueData" class="chart-outer">
        <div class="chart-container chart-scrollable">
          <div class="chart-layout" :style="{ minWidth: revenueByPeriod.length * 72 + 128 + 'px' }">
            <div class="chart-y-axis">
              <div v-for="(tick, idx) in yAxisTicks" :key="idx" class="y-tick">{{ tick.label }}</div>
            </div>
            <div class="chart-bars" :style="{ gridTemplateColumns: `repeat(${Math.max(revenueByPeriod.length, 1)}, minmax(56px, 1fr))` }">
              <div 
                v-for="item in revenueByPeriod" 
                :key="item.month"
                class="chart-bar-wrapper"
                @mouseenter="onBarHover($event, item)"
                @mouseleave="chartTooltip.visible = false"
              >
                <div class="bar-track">
                  <div class="chart-bar" :style="barHeightStyle(item)">
                    <div class="bar-value">{{ formatCompact(item.revenue) }}</div>
                  </div>
                </div>
                <div class="bar-label">{{ item.month }}</div>
              </div>
            </div>
          </div>
        </div>
        <!-- Hover tooltip (outside scroll container to avoid clipping) -->
        <Transition name="tooltip-pop">
          <div v-if="chartTooltip.visible" class="chart-tooltip" :style="{ left: chartTooltip.x + 'px', top: chartTooltip.y + 'px' }">
            <div class="chart-tooltip__label">{{ chartTooltip.label }}</div>
            <div class="chart-tooltip__row"><span>Doanh thu:</span> <strong>{{ formatCurrency(chartTooltip.revenue) }}</strong></div>
            <div class="chart-tooltip__row"><span>Đơn hàng:</span> <strong>{{ chartTooltip.orders }}</strong></div>
          </div>
        </Transition>
      </div>
      <div v-else class="chart-empty">Chưa có dữ liệu doanh thu theo khoảng thời gian đã chọn.</div>
    </div>

    <!-- Top Products -->
    <div class="table-section">
      <div class="section-header">
        <h3>Sản phẩm bán chạy</h3>
        <p class="section-subtitle">Top sản phẩm theo số lượng bán trong khoảng thời gian đã chọn</p>
      </div>
      <div class="top-products-wrap">
        <table class="data-table top-products-table">
          <thead>
            <tr>
              <th class="text-center">Hạng</th>
              <th>Sản phẩm</th>
              <th class="text-center">Số lượng bán</th>
              <th class="text-right">Doanh thu</th>
              <th class="text-center">Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(product, index) in topProducts" :key="index">
              <td class="text-center">
                <span class="rank-badge" :class="index < 3 ? `top-${index + 1}` : 'top-other'">#{{ index + 1 }}</span>
              </td>
              <td>
                <div class="product-cell">
                  <img
                    :src="product.image"
                    :alt="product.name"
                    class="product-thumb"
                    loading="lazy"
                    @error="handleTopProductImageError"
                  />
                  <div class="product-meta">
                    <div class="product-name">{{ product.name }}</div>
                  </div>
                </div>
              </td>
              <td class="text-center">{{ product.sold }}</td>
              <td class="text-right product-revenue">{{ hideTopProductRevenue ? 'Đang cập nhật' : formatCurrency(product.revenue) }}</td>
              <td class="text-center">
                <button
                  class="btn-eye"
                  :disabled="!product.productId"
                  :title="product.productId ? 'Xem chi tiết sản phẩm' : 'Không có sản phẩm để mở'"
                  @click="openTopProductDetail(product)"
                >
                  <span class="material-icons-outlined">visibility</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap');
@import url('https://fonts.googleapis.com/icon?family=Material+Icons+Outlined');

.thong-ke-page {
  font-family: 'Be Vietnam Pro', 'Segoe UI', Tahoma, sans-serif;
  padding: 24px 28px;
  background: transparent;
  min-height: auto;
  position: relative;
  overflow: hidden;
  isolation: isolate;
  --accent-red: #b11226;
  --accent-red-soft: #fee2e2;
  --accent-dark: #111827;
  --accent-muted: #6b7280;
  --surface-subtle: #f8fafc;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  animation: rise-fade 0.45s ease-out both;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.03em;
  margin: 0 0 6px;
}

.page-subtitle {
  font-size: 15px;
  color: #64748b;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn-primary,
.btn-secondary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(130deg, #8f1123, #c81f35 60%, #e13a4d);
  color: white;
  border: 1px solid rgba(146, 19, 36, 0.36);
  box-shadow: 0 8px 15px rgba(140, 20, 38, 0.22);
}

.btn-primary:hover {
  filter: brightness(1.02);
  transform: translateY(-1px);
}

.btn-secondary {
  background: white;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}

.filter-section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  border: 1px solid #e5e7eb;
  display: flex;
  gap: 16px;
  align-items: flex-end;
  animation: rise-fade 0.5s ease-out 0.04s both;
}

.search-meta {
  margin: -12px 0 18px;
  font-size: 13px;
  color: #4b5563;
}

.stats-alert {
  margin-bottom: 20px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #fecaca;
  background: #fff1f2;
  color: #991b1b;
  font-size: 14px;
  font-weight: 600;
}

.filter-group {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  flex: 1;
}

.filter-item {
  display: flex;
  flex-direction: column;
}

.filter-item label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
  white-space: nowrap;
}

.form-input,
.form-select {
  width: 100%;
  padding: 11px 14px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
}

.form-input:focus,
.form-select:focus {
  border-color: #111827;
}

/* Date DD/MM/YYYY overlay */
.date-wrap { position: relative; }
.date-native { color: transparent !important; }
.date-native::-webkit-datetime-edit { color: transparent; }
.date-native::-webkit-calendar-picker-indicator { position: relative; z-index: 2; cursor: pointer; }
.date-overlay { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); pointer-events: none; font-size: 14px; color: #374151; }
.date-placeholder { color: #9ca3af; }

.btn-search {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 11px 20px;
  background: linear-gradient(130deg, #8f1123, #c81f35 60%, #e13a4d);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 160px;
  width: auto;
  height: 44px;
  flex-shrink: 0;
}

.btn-search:hover {
  filter: brightness(1.02);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stats-grid.secondary {
  grid-template-columns: repeat(3, 1fr);
}

.stats-grid.secondary .stat-card:nth-child(1) { animation-delay: 0.18s; }
.stats-grid.secondary .stat-card:nth-child(2) { animation-delay: 0.24s; }
.stats-grid.secondary .stat-card:nth-child(3) { animation-delay: 0.3s; }

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #e5e7eb;
  transition: all 0.24s ease;
  position: relative;
  overflow: hidden;
  animation: rise-fade 0.45s ease-out both;
}

.stats-grid .stat-card:nth-child(1) {
  animation-delay: 0.06s;
}

.stats-grid .stat-card:nth-child(2) {
  animation-delay: 0.1s;
}

.stats-grid .stat-card:nth-child(3) {
  animation-delay: 0.14s;
}

.stat-card.trend-up {
  border-color: #bbf7d0;
  box-shadow: 0 0 0 1px rgba(34, 197, 94, 0.15) inset;
}

.stat-card.trend-down {
  border-color: #fecaca;
  box-shadow: 0 0 0 1px rgba(220, 38, 38, 0.15) inset;
}

.stat-card.trend-flat {
  border-color: #fca5a5;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stat-card.small {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.revenue {
  background: var(--accent-red-soft);
  color: var(--accent-red);
}

.stat-icon.orders {
  background: #fee2e2;
  color: #b91c1c;
}

.stat-icon.avg {
  background: #fee2e2;
  color: #b91c1c;
}

.stat-icon .material-icons-outlined {
  font-size: 24px;
}

.stat-icon-small {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-small.success {
  background: var(--accent-red-soft);
  color: var(--accent-red);
}

.stat-icon-small.warning {
  background: #fee2e2;
  color: #b91c1c;
}

.stat-icon-small.danger {
  background: #fee2e2;
  color: #b91c1c;
}

.stat-icon-small .material-icons-outlined {
  font-size: 28px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 8px;
}

.stat-value-small {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin-top: 4px;
}

.stat-change {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
}

.stat-change.positive {
  color: #166534;
  background: #dcfce7;
  padding: 4px 10px;
  border-radius: 999px;
}

.stat-change.negative {
  color: var(--accent-red);
  background: var(--accent-red-soft);
  padding: 4px 10px;
  border-radius: 999px;
}

.stat-change .material-icons-outlined {
  font-size: 18px;
}

.chart-section,
.table-section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #e5e7eb;
  margin-bottom: 24px;
  animation: rise-fade 0.55s ease-out 0.16s both;
}

.section-header {
  margin-bottom: 24px;
}

.section-header h3 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.section-subtitle {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.chart-outer {
  position: relative;
  overflow: visible;
}

.chart-container {
  min-height: 380px;
  padding: 12px 0 4px;
}

.chart-scrollable {
  overflow-x: auto;
  overflow-y: visible;
  -webkit-overflow-scrolling: touch;
}

.chart-layout {
  display: grid;
  grid-template-columns: 112px 1fr;
  gap: 16px;
  align-items: stretch;
  height: 100%;
}

.chart-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 4px 0 62px;
}

.y-tick {
  font-size: 11px;
  color: #6b7280;
}

.chart-bars {
  display: grid;
  align-items: stretch;
  height: 100%;
  gap: 12px;
  padding: 0 12px 18px;
  overflow: visible;
  border-left: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.chart-scrollable .chart-bars {
  gap: 8px;
  padding: 0 12px 18px;
}

.chart-bar-wrapper {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  position: relative;
}

.bar-track {
  height: 240px;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.chart-bar {
  width: min(72px, 100%);
  background: linear-gradient(180deg, #dc2626 0%, #b91c1c 100%);
  border-radius: 8px 8px 0 0;
  position: relative;
  min-height: 24px;
  overflow: visible;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 4px 2px 0;
  transition: all 0.3s;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.2);
  animation: bar-rise 0.65s ease-out both;
  transform-origin: bottom center;
}

.chart-bar-wrapper:nth-child(1) .chart-bar { animation-delay: 0.04s; }
.chart-bar-wrapper:nth-child(2) .chart-bar { animation-delay: 0.08s; }
.chart-bar-wrapper:nth-child(3) .chart-bar { animation-delay: 0.12s; }
.chart-bar-wrapper:nth-child(4) .chart-bar { animation-delay: 0.16s; }
.chart-bar-wrapper:nth-child(5) .chart-bar { animation-delay: 0.2s; }
.chart-bar-wrapper:nth-child(6) .chart-bar { animation-delay: 0.24s; }
.chart-bar-wrapper:nth-child(7) .chart-bar { animation-delay: 0.28s; }
.chart-bar-wrapper:nth-child(8) .chart-bar { animation-delay: 0.32s; }
.chart-bar-wrapper:nth-child(9) .chart-bar { animation-delay: 0.36s; }
.chart-bar-wrapper:nth-child(10) .chart-bar { animation-delay: 0.4s; }
.chart-bar-wrapper:nth-child(11) .chart-bar { animation-delay: 0.44s; }
.chart-bar-wrapper:nth-child(12) .chart-bar { animation-delay: 0.48s; }

.chart-bar:hover {
  opacity: 0.9;
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(220, 38, 38, 0.3);
}

.bar-value {
  position: relative;
  font-size: 9px;
  font-weight: 600;
  color: #ffffff;
  white-space: normal;
  max-width: 100%;
  text-align: center;
  word-break: break-word;
  line-height: 1.05;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.28);
  pointer-events: none;
}

.bar-label {
  margin-top: 12px;
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
}



.chart-tooltip {
  position: absolute;
  transform: translate(-50%, -100%);
  background: #1e293b;
  color: #fff;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  white-space: nowrap;
  pointer-events: none;
  z-index: 20;
  box-shadow: 0 6px 18px rgba(0,0,0,0.25);
  line-height: 1.6;
}

/* Tooltip transition */
.tooltip-pop-enter-active {
  animation: tooltip-fade-in 0.22s ease-out;
}
.tooltip-pop-leave-active {
  animation: tooltip-fade-in 0.15s ease-in reverse;
}
@keyframes tooltip-fade-in {
  from {
    opacity: 0;
    transform: translate(-50%, -90%) scale(0.92);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -100%) scale(1);
  }
}
.chart-tooltip::after {
  content: '';
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-top: 6px solid #1e293b;
}
.chart-tooltip__label {
  font-weight: 700;
  margin-bottom: 4px;
  font-size: 14px;
}
.chart-tooltip__row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
.chart-tooltip__row span { color: #94a3b8; }
.chart-tooltip__row strong { color: #fff; }

.top-products-wrap {
  overflow-x: auto;
  border: 1px solid rgba(145, 23, 41, 0.18);
  border-radius: 14px;
  background: linear-gradient(170deg, #ffffff 0%, #fff6f7 100%);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.top-products-table {
  min-width: 760px;
}

.data-table thead tr {
  background: linear-gradient(90deg, #7c1021 0%, #a9162d 35%, #cf243a 68%, #e64555 100%);
}

.data-table th {
  background: transparent;
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  border-bottom: 2px solid rgba(93, 14, 28, 0.5);
}

.data-table td {
  padding: 16px;
  border-bottom: 1px solid rgba(181, 34, 53, 0.1);
  font-size: 14px;
  color: #374151;
  background: #fff;
}

.data-table tbody tr:hover {
  background: #fff6f7;
}

.data-table tbody tr {
  transition: background 0.2s ease;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid #fecdd3;
  color: #9f1239;
  background: #fff1f2;
}

.rank-badge.top-1 {
  background: #dc2626;
  color: #fff;
  border-color: #dc2626;
}

.rank-badge.top-2 {
  background: #ef4444;
  color: #fff;
  border-color: #ef4444;
}

.rank-badge.top-3 {
  background: #f87171;
  color: #fff;
  border-color: #f87171;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-thumb {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid #fecdd3;
  background: #fff;
  box-shadow: 0 6px 14px rgba(220, 38, 38, 0.12);
}

.product-meta {
  min-width: 0;
}

.product-name {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  line-height: 1.35;
}

.product-revenue {
  font-weight: 700;
  color: #111827;
}

.btn-eye {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: 1px solid #fecdd3;
  background: linear-gradient(130deg, #8f1123, #c81f35 60%, #e13a4d);
  color: #be123c;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #fff;
}

.btn-eye .material-icons-outlined {
  font-size: 18px;
}

.btn-eye:hover:not(:disabled) {
  filter: brightness(1.03);
  transform: translateY(-1px);
  box-shadow: 0 8px 15px rgba(140, 20, 38, 0.22);
}

.btn-eye:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.text-center {
  text-align: center;
}

.text-right {
  text-align: right;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-grid.secondary {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .thong-ke-page {
    padding: 18px 14px;
  }

  .page-title {
    font-size: 18px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .header-actions {
    width: 100%;
  }
  
  .btn-primary,
  .btn-secondary {
    flex: 1;
  }
  
  .filter-section {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-group {
    grid-template-columns: 1fr;
  }

  .btn-search {
    width: 100%;
  }
  
  .chart-container {
    min-height: 280px;
  }

  .chart-layout {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .chart-y-axis {
    display: none;
  }
  
  .chart-bars {
    gap: 10px;
    padding: 0 8px 14px;
  }

  .bar-track {
    height: 180px;
  }
  
  .bar-amount {
    font-size: 10px;
  }

  .product-thumb {
    width: 44px;
    height: 44px;
  }

  .product-name {
    font-size: 13px;
  }
}

.chart-empty {
  padding: 60px 20px;
  text-align: center;
  color: #9ca3af;
  font-size: 15px;
  background: #f9fafb;
  border-radius: 12px;
}

/* ── Payment / Phiếu breakdown ── */
.breakdown-section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #e5e7eb;
  margin-bottom: 24px;
  animation: rise-fade 0.5s ease-out 0.12s both;
}

.breakdown-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.breakdown-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 16px;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s;
  animation: rise-fade 0.45s ease-out both;
}

.breakdown-grid .breakdown-card:nth-child(1) { animation-delay: 0.14s; }
.breakdown-grid .breakdown-card:nth-child(2) { animation-delay: 0.2s; }
.breakdown-grid .breakdown-card:nth-child(3) { animation-delay: 0.26s; }
.breakdown-grid .breakdown-card:nth-child(4) { animation-delay: 0.32s; }

.breakdown-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.breakdown-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.breakdown-card.cash .breakdown-icon {
  background: var(--accent-red-soft);
  color: var(--accent-red);
}

.breakdown-card.transfer .breakdown-icon {
  background: #fee2e2;
  color: #b91c1c;
}

.breakdown-card.thu .breakdown-icon {
  background: #fee2e2;
  color: #b91c1c;
}

.breakdown-card.chi .breakdown-icon {
  background: #fee2e2;
  color: #b91c1c;
}

.breakdown-icon .material-icons-outlined {
  font-size: 22px;
}

.breakdown-label {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
  margin-bottom: 4px;
}

.breakdown-value {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.net-cash-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-radius: 12px;
  background: #fff;
  color: #111827;
  border: 1px solid #d1d5db;
  box-shadow: 0 4px 10px rgba(17, 24, 39, 0.08);
  flex-wrap: wrap;
  animation: rise-fade 0.45s ease-out 0.38s both;
}

.net-cash-label {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.net-cash-value {
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #111827;
  text-shadow:
    -1px 0 #fff,
    0 1px #fff,
    1px 0 #fff,
    0 -1px #fff;
}

.net-cash-value.positive {
  color: #111827;
}

.net-cash-value.negative {
  color: #111827;
}

.net-cash-hint {
  font-size: 12px;
  color: #6b7280;
  margin-left: auto;
}

@keyframes rise-fade {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes bar-rise {
  from {
    opacity: 0;
    transform: scaleY(0.15);
  }
  to {
    opacity: 1;
    transform: scaleY(1);
  }
}

@media (max-width: 1200px) {
  .breakdown-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .breakdown-grid {
    grid-template-columns: 1fr;
  }

  .net-cash-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .net-cash-hint {
    margin-left: 0;
  }
}
</style>
