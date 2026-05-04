<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import taiKhoanService from '../../services/taiKhoanService'
import {
  getAllKhachHang,
  getKhachHangByTaiKhoanId,
  updateKhachHang
} from '../../services/KhachHangService'
import { getAllHoaDon, updateHoaDonBySystemEvent, getHoaDonById } from '../../services/hoaDonService'
import { getAllSanPham } from '../../services/sanPhamService'
import { getProductImageOverride } from '../../utils/productImageOverrides'
import { fallbackImageForVariant } from '../../utils/productImageFallback'
import { resolveApiOrigin } from '../../utils/apiOrigin'
import img1 from '../../assets/img/Jackets/bomber/bomber-da-lon.jpg?url'
import img2 from '../../assets/img/Jackets/bomber/bomber-dang-lung.jpg?url'
import img3 from '../../assets/img/Jackets/bomber/bomber-gia-da.jpg?url'
import img4 from '../../assets/img/Jackets/bomber/bomber-nhe-cotton.jpg?url'
import img5 from '../../assets/img/Jackets/hoodie/hoodie-dang-hop.jpg?url'
import img6 from '../../assets/img/Jackets/hoodie/hoodie-in-hinh.jpg?url'
import img7 from '../../assets/img/Jackets/hoodie/hoodie-keo-khoa.jpg?url'
import img8 from '../../assets/img/Jackets/coach/coach-cach-nhiet.jpg?url'
import img9 from '../../assets/img/Jackets/coach/coach-da-asos.jpg?url'
import img10 from '../../assets/img/Jackets/coach/coach-gia-da.jpg?url'
import img11 from '../../assets/img/Jackets/coach/coach-long-cuu.jpg?url'
import img12 from '../../assets/img/Jackets/bomber/bomber-astronaut/bomber-astronaut-black.PNG?url'
import img13 from '../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-black.PNG?url'
import img14 from '../../assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-black.PNG?url'
import img15 from '../../assets/img/Jackets/coach/coach-leopard/coach-leopard.PNG?url'
import img16 from '../../assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-black.PNG?url'
import img17 from '../../assets/img/Jackets/coach/coach-tiger-stripe/coach-tiger-stripe.PNG?url'
import img18 from '../../assets/img/Jackets/hoodie/hoodie-camo/hoodie-camo-black.PNG?url'
import img19 from '../../assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-blue.PNG?url'
import img20 from '../../assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-black.PNG?url'

const fallbackImages = [img1,img2,img3,img4,img5,img6,img7,img8,img9,img10,img11,img12,img13,img14,img15,img16,img17,img18,img19,img20]

const nameFallbackRules = [
  { keywords: ['bomber', 'da', 'lon'], image: img1 },
  { keywords: ['bomber', 'dang', 'lung'], image: img2 },
  { keywords: ['bomber', 'gia', 'da'], image: img3 },
  { keywords: ['bomber', 'cotton'], image: img4 },
  { keywords: ['hoodie', 'dang', 'hop'], image: img5 },
  { keywords: ['hoodie', 'in', 'hinh'], image: img6 },
  { keywords: ['hoodie', 'keo', 'khoa'], image: img7 },
  { keywords: ['coach', 'cach', 'nhiet'], image: img8 },
  { keywords: ['coach', 'da', 'asos'], image: img9 },
  { keywords: ['coach', 'gia', 'da'], image: img10 },
  { keywords: ['coach', 'long', 'cuu'], image: img11 },
  { keywords: ['astronaut'], image: img12 },
  { keywords: ['embroidered', 'fuzzy'], image: img13 },
  { keywords: ['windbreaker'], image: img14 },
  { keywords: ['leopard'], image: img15 },
  { keywords: ['longsleeve'], image: img16 },
  { keywords: ['tiger', 'stripe'], image: img17 },
  { keywords: ['camo'], image: img18 },
  { keywords: ['zip', 'boxy'], image: img19 },
  { keywords: ['zip', 'silk'], image: img20 },
]
const codeFallbackMap = {
  SP001: img1, SP002: img2, SP003: img3, SP004: img4, SP005: img5,
  SP006: img6, SP007: img7, SP008: img8, SP009: img9, SP010: img10,
  SP011: img11, SP012: img12, SP013: img13, SP014: img14, SP015: img15,
  SP016: img16, SP017: img17, SP018: img18, SP019: img19, SP020: img20,
}
const getNameFallbackImage = (name = '') => {
  const normalized = String(name || '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()
  if (!normalized) return ''
  const found = nameFallbackRules.find((rule) => rule.keywords.every((kw) => normalized.includes(kw)))
  return found?.image || ''
}
const getCodeFallbackImage = (code = '') => {
  const upper = String(code || '').trim().toUpperCase()
  return codeFallbackMap[upper] || ''
}
const fallbackImageFor = (id, name = '', code = '') => {
  const byCode = getCodeFallbackImage(code)
  if (byCode) return byCode
  const byName = getNameFallbackImage(name)
  if (byName) return byName
  const n = Number(id)
  if (Number.isFinite(n) && n > 0) return fallbackImages[(n - 1) % fallbackImages.length]
  return fallbackImages[0]
}
import {
  createDiaChi,
  deleteDiaChi,
  getDiaChiByKhachHang,
  updateDiaChi
} from '../../services/diaChiService'
import { useToast } from '../../composables/useToast'
import { resolvePublicAppOrigin } from '../../utils/publicTrackingUrl'
import { buildOrderLookupTrackingUrl } from '../../utils/publicTrackingUrl'
import { getVietnameseNameByEmail, isGenericVietnameseName } from '../../utils/vietnameseNames'
import { normalizeAdminStatusLabel } from '../../utils/adminStatus'
import { describePaymentFlowState } from '../../utils/paymentWorkflow'
import { dispatchAuthContextChanged, resolveAccountByRole } from '../../utils/authContext'
import SiteNav from '../../components/SiteNav.vue'

const router = useRouter()
const route = useRoute()
const toast = useToast()

const loading = ref(true)
const saving = ref(false)
const confirmingOrderId = ref(null)
const error = ref('')
const resolveInitialTab = (tab) => {
  if (tab === 'orders' || tab === 'delivery' || tab === 'lookup') return tab
  return 'profile'
}

const activeTab = ref(resolveInitialTab(route.query.tab))

const account = ref(null)
const customer = ref(null)
const orders = ref([])
const addresses = ref([])
const avatarInputRef = ref(null)
const selectedOrderId = ref(null)
const orderDetailData = ref(null)
const loadingOrderDetail = ref(false)
const productCatalog = ref(null)
const OFFLINE_ORDER_STORAGE_KEY = 'pendingOfflineOrders'
const ORDER_ITEM_VOUCHER_SNAPSHOTS_KEY = 'orderItemVoucherSnapshots'
const LOCAL_REGISTERED_PROFILES_KEY = 'localRegisteredProfiles'
const MISSING_PROFILE_TOAST_PREFIX = 'customer:missing-profile-warning:'

const profileForm = reactive({
  tenKhachHang: '',
  gioiTinh: 'Nam',
  ngaySinh: '',
  soDienThoai: '',
  email: '',
  avatar: ''
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const addressForm = reactive({
  id: null,
  diaChiCuThe: '',
  tinhThanh: '',
  quanHuyen: '',
  phuongXa: '',
  trangThai: 'Hoạt động'
})

const MAX_AVATAR_LENGTH = 4000

const getAvatarStorageKey = (accountId) => `avatar:${accountId}`

const loadLocalRegisteredProfile = (email) => {
  try {
    const raw = localStorage.getItem(LOCAL_REGISTERED_PROFILES_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    const safeObject = parsed && typeof parsed === 'object' ? parsed : {}
    const normalizedEmail = String(email || '').trim().toLowerCase()
    return safeObject[normalizedEmail] || null
  } catch {
    return null
  }
}

const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value || 0))
}

const hasCustomerProfile = computed(() => Boolean(customer.value?.id))

const hasAnyCustomerInput = () => {
  return Boolean(
    String(profileForm.tenKhachHang || '').trim()
    || String(profileForm.soDienThoai || '').trim()
    || String(profileForm.ngaySinh || '').trim()
    || String(addressForm.diaChiCuThe || '').trim()
    || String(addressForm.tinhThanh || '').trim()
    || String(addressForm.quanHuyen || '').trim()
    || String(addressForm.phuongXa || '').trim()
  )
}

const hasLocalRegisteredProfileSnapshot = () => {
  const localProfile = loadLocalRegisteredProfile(account.value?.email)
  if (!localProfile || typeof localProfile !== 'object') return false
  return Boolean(
    String(localProfile.fullName || '').trim()
    || String(localProfile.phone || '').trim()
    || String(localProfile.birthDate || '').trim()
    || String(localProfile.diaChiCuThe || '').trim()
    || String(localProfile.tinhThanh || '').trim()
    || String(localProfile.quanHuyen || '').trim()
    || String(localProfile.phuongXa || '').trim()
  )
}

const shouldWarnMissingCustomerProfile = () => {
  if (hasCustomerProfile.value) return false
  return !hasAnyCustomerInput() && !hasLocalRegisteredProfileSnapshot()
}

const formatDateTime = (value) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('vi-VN')
}

// Parse a backend date (possibly ISO with timezone) safely to local YYYY-MM-DD
const parseBackendDate = (val) => {
  if (!val) return ''
  const raw = String(val)
  if (!raw.includes('T')) return raw.slice(0, 10)
  const d = new Date(raw)
  if (isNaN(d.getTime())) return raw.slice(0, 10)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const getOrderTotal = (order) => {
  const directTotal = Number(
    order?.displayTotal
    ?? order?.thanhTien
    ?? order?.tongTien
    ?? order?.tongThanhToan
    ?? order?.total
    ?? 0
  )
  if (directTotal > 0) return directTotal

  const details = Array.isArray(order?.hoaDonChiTiets)
    ? order.hoaDonChiTiets
    : (Array.isArray(order?.items) ? order.items : [])

  const subtotal = details.reduce((sum, item) => {
    const lineTotal = Number(item?.thanhTien || 0)
    if (lineTotal > 0) return sum + lineTotal
    return sum + Number(item?.giaBan || item?.price || 0) * Number(item?.soLuong || item?.quantity || 0)
  }, 0)

  return Math.max(subtotal + Number(order?.phiShip || order?.shipping || 0) - Number(order?.giaSauGiamGia || order?.discount || 0), 0)
}

/* ── Product catalog & order detail ── */
const loadProductCatalog = async () => {
  if (productCatalog.value) return productCatalog.value
  try {
    const res = await getAllSanPham({ size: 9999 })
    const list = Array.isArray(res?.data?.content) ? res.data.content : (Array.isArray(res?.data) ? res.data : [])
    const apiBase = resolveApiOrigin()
    const toUrl = (raw) => {
      if (!raw) return ''
      if (raw.startsWith('http')) return raw
      return `${apiBase}${raw.startsWith('/') ? '' : '/'}${raw}`
    }
    const isCuratedCatalogCode = (value = '') => /^SP0*(?:[1-9]|1\d|20)$/i.test(String(value || '').trim())
    const pickImage = (product, variant) => {
      const productCode = String(product?.maSanPham || '').trim().toUpperCase()
      if (isCuratedCatalogCode(productCode)) {
        const curatedVariant = fallbackImageForVariant({
          id: product?.id,
          maSanPham: productCode,
          tenSanPham: product?.tenSanPham || product?.name,
          tenMauSac: variant?.mauSac?.tenMauSac || variant?.mauSac?.tenMau || variant?.tenMauSac || variant?.tenMau,
          maChiTietSanPham: variant?.ma,
        })
        if (curatedVariant) return curatedVariant
      }
      const variantImg = variant?.anh || variant?.hinhAnh || variant?.image
      if (variantImg) return toUrl(variantImg)
      const byVariantFallback = fallbackImageForVariant({
        id: product?.id,
        maSanPham: product?.maSanPham,
        tenSanPham: product?.tenSanPham || product?.name,
        tenMauSac: variant?.mauSac?.tenMauSac || variant?.mauSac?.tenMau || variant?.tenMauSac || variant?.tenMau,
        maChiTietSanPham: variant?.ma,
      })
      if (byVariantFallback) return byVariantFallback
      const override = getProductImageOverride({ id: product?.id, maSanPham: product?.maSanPham })
      const overrideUrl = Array.isArray(override) ? override[0] : override
      if (overrideUrl) return overrideUrl
      const productImg = product?.anh || product?.hinhAnh || product?.image || product?.images?.[0] || product?.listAnh?.[0]
      if (productImg) return toUrl(productImg)
      return fallbackImageFor(product?.id, product?.tenSanPham || product?.name || '', product?.maSanPham || '')
    }
    const map = new Map()
    for (const sp of list) {
      const spName = sp.tenSanPham || sp.ten || ''
      const variants = Array.isArray(sp.sanPhamChiTiets) ? sp.sanPhamChiTiets : []
      for (const v of variants) {
        const varImg = pickImage(sp, v)
        const colorName = v.mauSac?.tenMauSac || v.mauSac?.tenMau || v.tenMauSac || v.tenMau || ''
        const sizeName = v.kichThuoc?.tenKichThuoc || v.kichThuoc?.tenSize || v.tenKichThuoc || v.tenSize || ''
        const parts = [spName, colorName, sizeName].filter(Boolean)
        map.set(v.id, {
          ten: parts.join(' - '),
          anh: varImg,
          mauSac: String(colorName || '').trim(),
          kichThuoc: String(sizeName || '').trim(),
          maSanPhamChiTiet: String(v.ma || '').trim(),
        })
      }
    }
    productCatalog.value = map
    return map
  } catch {
    return new Map()
  }
}

const resolveItemName = (item, catalog) => {
  if (item.tenSanPhamChiTiet && !String(item.tenSanPhamChiTiet).startsWith('SP #')) return item.tenSanPhamChiTiet
  const entry = catalog?.get?.(item.spctId || item.sanPhamChiTietId)
  return entry?.ten || item.tenSanPhamChiTiet || item.name || `SP #${item.spctId || item.id}`
}
const resolveItemImage = (item, catalog) => {
  const spctId = Number(item.spctId || item.sanPhamChiTietId || item.id)
  const fromCatalog = catalog?.get?.(spctId)?.anh
  if (fromCatalog) return fromCatalog
  if (item.image || item.anh) return item.image || item.anh
  const itemName = item.tenSanPhamChiTiet || item.name || ''
  return fallbackImageForVariant({
    id: Number(item.productId || item.sanPhamId || spctId),
    maSanPham: item.maSanPham,
    tenSanPham: itemName,
    tenMauSac: item?.mauSac?.tenMauSac || item?.mauSac?.tenMau || item?.tenMauSac || item?.tenMau || item?.mauSac,
    maChiTietSanPham: item?.maSanPhamChiTiet,
  }) || fallbackImageFor(spctId, itemName)
}

const normalizeMetaText = (value) => {
  const normalized = String(value || '').trim()
  if (!normalized) return ''
  const lowered = normalized.toLowerCase()
  if (['null', 'undefined', 'n/a', 'na', 'khong ro', 'không rõ', 'chua ro', 'chưa rõ'].includes(lowered)) {
    return ''
  }
  return normalized
}

const getOrderItemColor = (item) => {
  const value = item?.mauSac?.tenMauSac
    || item?.mauSac?.tenMau
    || item?.tenMauSac
    || item?.tenMau
    || item?.mauSac
    || productCatalog.value?.get?.(Number(item?.spctId || item?.sanPhamChiTietId || item?.id))?.mauSac
  return normalizeMetaText(value)
}

const getOrderItemSize = (item) => {
  const value = item?.kichThuoc?.tenKichThuoc
    || item?.kichThuoc?.tenSize
    || item?.tenKichThuoc
    || item?.tenSize
    || item?.kichThuoc
    || productCatalog.value?.get?.(Number(item?.spctId || item?.sanPhamChiTietId || item?.id))?.kichThuoc
  return normalizeMetaText(value)
}

const getVoucherSnapshotMap = () => {
  try {
    const raw = localStorage.getItem(ORDER_ITEM_VOUCHER_SNAPSHOTS_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

const resolveOrderVoucherSnapshot = (order) => {
  if (!order) return null
  const map = getVoucherSnapshotMap()
  const keys = [
    String(order?.maHoaDon || '').trim(),
    String(order?.id || '').trim(),
  ].filter(Boolean)
  for (const key of keys) {
    if (map[key]) return map[key]
  }
  return null
}

const getOrderItemVouchers = (order, item) => {
  const vouchers = []

  const directCode = String(
    item?.maPhieuGiamGia
      || item?.voucherCode
      || item?.maVoucher
      || item?.phieuGiamGia
      || item?.voucher?.code
      || ''
  ).trim()
  const directDiscount = Number(
    item?.voucherDiscount
      || item?.giamGiaVoucher
      || item?.giaTriGiamGia
      || item?.giamGia
      || item?.discount
      || item?.voucher?.discount
      || 0
  )
  if (directCode || directDiscount > 0) {
    vouchers.push({ code: directCode, discount: directDiscount })
  }

  const snapshot = resolveOrderVoucherSnapshot(order)
  const rows = Array.isArray(snapshot?.itemVouchers) ? snapshot.itemVouchers : []
  if (rows.length) {
    const spctId = Number(item?.spctId || item?.sanPhamChiTietId || 0)
    const color = String(getOrderItemColor(item) || '').trim().toLowerCase()
    const size = String(getOrderItemSize(item) || '').trim().toLowerCase()
    const productId = Number(item?.id || item?.sanPhamId || 0)

    const matchedRows = rows.filter((row) => {
      const sameSpct = spctId > 0 && Number(row?.spctId || 0) === spctId
      if (sameSpct) return true
      const sameProduct = productId > 0 && Number(row?.productId || 0) === productId
      if (!sameProduct) return false
      const sameColor = !color || String(row?.color || '').trim().toLowerCase() === color
      const sameSize = !size || String(row?.size || '').trim().toLowerCase() === size
      return sameColor && sameSize
    })

    for (const row of matchedRows) {
      const code = String(row?.voucherCode || '').trim()
      const discount = Number(row?.discount || 0)
      if (code || discount > 0) {
        vouchers.push({ code, discount })
      }
    }
  }

  if (!vouchers.length) return []

  // Merge duplicate voucher rows (same code) for lines that were aggregated by backend.
  const mergedMap = new Map()
  for (const voucher of vouchers) {
    const code = String(voucher?.code || '').trim()
    const discount = Number(voucher?.discount || 0)
    const key = code || '__applied__'
    const current = mergedMap.get(key) || { code, discount: 0 }
    current.discount += discount
    if (!current.code && code) current.code = code
    mergedMap.set(key, current)
  }

  return Array.from(mergedMap.values())
    .filter((voucher) => voucher.code || voucher.discount > 0)
    .sort((a, b) => Number(b.discount || 0) - Number(a.discount || 0))
}

const openOrderDetail = async (order) => {
  selectedOrderId.value = order.id
  orderDetailData.value = null
  loadingOrderDetail.value = true
  try {
    if (order?.isOfflineFallback) {
      const catalog = await loadProductCatalog()
      const rawItems = Array.isArray(order.items) ? order.items : []
      const enriched = rawItems.map(item => ({
        ...item,
        tenSanPhamChiTiet: resolveItemName(item, catalog),
        anh: resolveItemImage(item, catalog)
      }))
      orderDetailData.value = { ...order, hoaDonChiTiets: enriched }
      return
    }
    if (!order?.id || String(order.id).startsWith('offline-')) return
    const [res, catalog] = await Promise.all([getHoaDonById(order.id), loadProductCatalog()])
    const detail = res?.data?.hoaDon || res?.data || {}
    const rawItems = Array.isArray(res?.data?.items) ? res.data.items
      : Array.isArray(detail?.hoaDonChiTiets) ? detail.hoaDonChiTiets
      : Array.isArray(detail?.items) ? detail.items : []
    const enriched = rawItems.map(item => ({
      ...item,
      tenSanPhamChiTiet: resolveItemName(item, catalog),
      anh: resolveItemImage(item, catalog)
    }))
    orderDetailData.value = { ...order, ...detail, hoaDonChiTiets: enriched }
  } catch {
    orderDetailData.value = order
  } finally {
    loadingOrderDetail.value = false
  }
}
const closeOrderDetail = () => { selectedOrderId.value = null; orderDetailData.value = null }
const selectedOrder = computed(() => orders.value.find(o => o.id === selectedOrderId.value) || null)
const getOrderItems = (order) => {
  const src = orderDetailData.value || order
  return Array.isArray(src?.hoaDonChiTiets) ? src.hoaDonChiTiets : (Array.isArray(src?.items) ? src.items : [])
}

const isVnpayOrder = (order) => {
  const paymentText = String(order?.phuongThucThanhToan || order?.paymentMethod || '').toUpperCase()
  const statusText = String(order?.statusNote || '').toUpperCase()
  return paymentText.includes('VNPAY') || statusText.includes('VNPAY')
}

const shouldShowPaymentFlow = (order) => {
  return Boolean(order?.isOfflineFallback || isVnpayOrder(order))
}

const mapOfflineOrders = (customerId) => {
  const stored = JSON.parse(localStorage.getItem(OFFLINE_ORDER_STORAGE_KEY) || '[]')
  if (!Array.isArray(stored)) return []

  return stored
    .filter((item) => Number(item?.customerId) === Number(customerId))
    .map((item) => ({
      ...item,
      id: `offline-${item.id}`,
      maHoaDon: item.id,
      ngayTao: item.createdAt,
      thanhTien: Number(item.total || 0),
      phuongThucThanhToan: item.paymentMethod || 'COD',
      trangThai: item.syncStatus === 'pending' ? 'Chờ đồng bộ' : 'Đã ghi nhận',
      statusNote: item.syncStatus === 'pending' ? 'Đơn COD đã lưu tạm trên trình duyệt, chờ đồng bộ vào hệ thống.' : '',
      isOfflineFallback: true
    }))
}

const getCurrentAccount = async () => {
  const matched = await resolveAccountByRole({
    service: taiKhoanService,
    expectedRoles: ['CUSTOMER', 'KHACH_HANG', 'KHACHHANG', 'USER'],
    allowFallback: false
  })

  if (!matched?.id) {
    throw new Error('Không tìm thấy tài khoản CUSTOMER hiện tại')
  }

  return matched
}

const extractCustomerList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const getTaiKhoanIdFromKhachHang = (item) => {
  return Number(item?.idTaiKhoan || item?.taiKhoan?.id || 0)
}

const findCustomerFromPages = async (accountId) => {
  const pageSize = 50
  let page = 0
  let totalPages = 1

  while (page < totalPages) {
    const listRes = await getAllKhachHang(page, pageSize)
    const pageData = listRes?.data
    const list = extractCustomerList(pageData)
    const matched = list.find((item) => getTaiKhoanIdFromKhachHang(item) === Number(accountId))
    if (matched) return matched

    const detectedTotalPages = Number(pageData?.totalPages)
    if (Number.isFinite(detectedTotalPages) && detectedTotalPages > 0) {
      totalPages = detectedTotalPages
    } else if (list.length < pageSize) {
      break
    } else {
      totalPages = page + 2
    }
    page += 1
  }

  return null
}

const getCurrentCustomer = async (accountId, accountEmail = '') => {
  try {
    const byTaiKhoan = await getKhachHangByTaiKhoanId(accountId)
    if (byTaiKhoan?.data?.id) return byTaiKhoan.data
  } catch {
    // Fallback below.
  }

  const byPages = await findCustomerFromPages(accountId)
  if (byPages?.id) return byPages

  const listRes = await getAllKhachHang(0, 500)
  const list = extractCustomerList(listRes?.data)
  const normalizedEmail = String(accountEmail || '').trim().toLowerCase()
  if (!normalizedEmail) return null

  return list.find((item) => {
    const email = String(item?.taiKhoan?.email || '').trim().toLowerCase()
    return email && email === normalizedEmail
  }) || null
}

const loadOrders = async (customerId) => {
  const offlineOrders = mapOfflineOrders(customerId)

  try {
    const res = await getAllHoaDon()
    const list = Array.isArray(res?.data) ? res.data : (Array.isArray(res?.data?.content) ? res.data.content : [])
    const backendOrders = list
      .filter((item) => Number(item?.khachHang?.id || item?.khachHangId) === Number(customerId))
      .map((item) => ({
        ...item,
        displayTotal: getOrderTotal(item)
      }))

    orders.value = [...offlineOrders, ...backendOrders]
      .sort((a, b) => String(b?.ngayTao || '').localeCompare(String(a?.ngayTao || '')))

    const newlyConfirmed = orders.value.filter((item) => {
      const state = describePaymentFlowState(item)
      if (state.code !== 'EMPLOYEE_CONFIRMED') return false
      const noticeKey = `vnpay_notice_${item.id}`
      if (localStorage.getItem(noticeKey)) return false
      localStorage.setItem(noticeKey, new Date().toISOString())
      return true
    })

    if (newlyConfirmed.length) {
      toast.success(`Có ${newlyConfirmed.length} đơn VNPay đã được xác nhận thanh toán thành công`)
    }
  } catch {
    orders.value = offlineOrders.sort((a, b) => String(b?.ngayTao || '').localeCompare(String(a?.ngayTao || '')))
  }
}

const getOrderStatusLabel = (order) => {
  return normalizeAdminStatusLabel(order?.orderStatusName || order?.trangThai || '')
}

const getOrderStatusTone = (order) => {
  const label = getOrderStatusLabel(order)
  if (/hoàn thành|thành công/i.test(label)) return 'success'
  if (/hủy|thất bại/i.test(label)) return 'danger'
  if (/chờ|đang/i.test(label)) return 'warning'
  return 'neutral'
}

const getOrderPaymentFlow = (order) => {
  if (order?.isOfflineFallback) {
    return {
      code: 'OFFLINE_PENDING',
      tone: 'pending',
      label: order?.statusNote || 'Đơn hàng đã được ghi nhận cục bộ và đang chờ đồng bộ lên hệ thống.'
    }
  }
  if (order?.paymentFlowCode) {
    return {
      code: String(order.paymentFlowCode || ''),
      tone: String(order.paymentFlowTone || 'neutral'),
      label: String(order.paymentFlowLabel || '')
    }
  }
  return describePaymentFlowState(order)
}

const canConfirmPayment = (order) => {
  if (order?.isOfflineFallback) return false
  const paymentText = String(order?.phuongThucThanhToan || order?.paymentMethod || '').toUpperCase()
  const isVnpay = paymentText.includes('VNPAY') || String(order?.statusNote || '').toUpperCase().includes('VNPAY')
  if (!isVnpay) return false
  return getOrderPaymentFlow(order).code === 'WAIT_CUSTOMER'
}

const confirmPayment = async (order) => {
  if (!order?.id || !canConfirmPayment(order)) return

  confirmingOrderId.value = order.id
  try {
    const trackingUrl = buildOrderLookupTrackingUrl({ maHoaDon: order.maHoaDon })
    await updateHoaDonBySystemEvent(
      order.id,
      'THANH_TOAN_KHACH_XAC_NHAN',
      'Khach hang da bam xac nhan thanh toan',
      trackingUrl
    )
    toast.success('Đã gửi yêu cầu xác nhận thanh toán tới nhân viên')
    await loadOrders(customer.value?.id)
  } catch (e) {
    console.error('Customer confirm payment failed:', e)
    toast.error(e?.response?.data?.message || 'Không thể gửi xác nhận thanh toán')
  } finally {
    confirmingOrderId.value = null
  }
}

const loadAddresses = async (customerId) => {
  try {
    const res = await getDiaChiByKhachHang(customerId)
    addresses.value = Array.isArray(res?.data) ? res.data : []
  } catch {
    addresses.value = []
  }
}

const fillProfileForm = () => {
  const localProfile = loadLocalRegisteredProfile(account.value?.email)
  profileForm.tenKhachHang = customer.value?.tenKhachHang || localProfile?.fullName || localStorage.getItem('userName') || ''
  profileForm.gioiTinh = customer.value?.gioiTinh || localProfile?.gender || localStorage.getItem('userGender') || 'Nam'
  profileForm.ngaySinh = customer.value?.ngaySinh
    ? parseBackendDate(customer.value.ngaySinh)
    : (localProfile?.birthDate || localStorage.getItem('userBirthDate') || '')
  profileForm.soDienThoai = customer.value?.soDienThoai || localProfile?.phone || localStorage.getItem('userPhone') || ''
  profileForm.email = account.value?.email || ''
  const localAvatar = account.value?.id ? localStorage.getItem(getAvatarStorageKey(account.value.id)) : ''
  profileForm.avatar = localAvatar || account.value?.avatar || account.value?.hinhAnh || account.value?.anhDaiDien || ''

  if (!addresses.value.length) {
    addressForm.diaChiCuThe = localProfile?.diaChiCuThe || localStorage.getItem('userDiaChiCuThe') || ''
    addressForm.tinhThanh = localProfile?.tinhThanh || localStorage.getItem('userTinhThanh') || ''
    addressForm.quanHuyen = localProfile?.quanHuyen || localStorage.getItem('userQuanHuyen') || ''
    addressForm.phuongXa = localProfile?.phuongXa || localStorage.getItem('userPhuongXa') || ''
  }
}

const ensureRealCustomerName = async () => {
  if (!account.value?.email || !customer.value?.id) return
  const mappedName = getVietnameseNameByEmail(account.value.email)
  if (!mappedName) return
  if (!isGenericVietnameseName(customer.value?.tenKhachHang)) return

  try {
    await updateKhachHang(customer.value.id, {
      ...customer.value,
      tenKhachHang: mappedName
    })
    customer.value = { ...customer.value, tenKhachHang: mappedName }
  } catch {
    // Keep current name if API update fails.
  }
}

const loadAccountCenter = async () => {
  loading.value = true
  error.value = ''
  orders.value = []
  addresses.value = []

  try {
    account.value = await getCurrentAccount()
    customer.value = await getCurrentCustomer(account.value.id, account.value?.email)
    fillProfileForm()

    if (customer.value?.id) {
      await ensureRealCustomerName()
      await Promise.all([
        loadOrders(customer.value.id),
        loadAddresses(customer.value.id)
      ])
      fillProfileForm()
    } else {
      activeTab.value = 'profile'
      fillProfileForm()
      if (shouldWarnMissingCustomerProfile()) {
        const accountKey = String(account.value?.id || account.value?.email || '').trim().toLowerCase()
        if (accountKey) {
          toast.showToast(
            'Tài khoản đã đăng nhập nhưng chưa có hồ sơ khách hàng. Bạn vẫn có thể cập nhật email, ảnh đại diện và mật khẩu.',
            'warning',
            {
              onceKey: `${MISSING_PROFILE_TOAST_PREFIX}${accountKey}`
            }
          )
        }
      }
    }
  } catch (e) {
    console.error('Load account center failed:', e)
    error.value = 'Không thể tải trung tâm tài khoản. Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}

const saveProfileInfo = async () => {
  if (!account.value?.id) return

  let avatarToPersist = String(profileForm.avatar || '').trim()
  if (avatarToPersist.startsWith('data:')) {
    localStorage.setItem(getAvatarStorageKey(account.value.id), avatarToPersist)
    // Data URL usually cannot fit DB avatar column; keep backend avatar unchanged.
    avatarToPersist = String(account.value?.avatar || '').trim()
    if (!avatarToPersist && profileForm.avatar.length > MAX_AVATAR_LENGTH) {
      toast.success('Đã lưu ảnh cục bộ cho trình duyệt hiện tại. Để đồng bộ mọi thiết bị, vui lòng dùng URL ảnh.')
    }
  } else if (account.value?.id) {
    localStorage.removeItem(getAvatarStorageKey(account.value.id))
  }

  // Validate required fields
  const hoTen = String(profileForm.tenKhachHang || '').trim()
  if (!hoTen) {
    toast.error('Vui lòng nhập họ tên.')
    return
  }

  const emailVal = String(profileForm.email || '').trim()
  if (!emailVal) {
    toast.error('Vui lòng nhập email.')
    return
  }

  const ngaySinhVal = String(profileForm.ngaySinh || '').trim()
  if (!ngaySinhVal) {
    toast.error('Vui lòng nhập ngày sinh.')
    return
  }

  // Validate phone number (must be exactly 10 digits, starting with 0)
  const phone = String(profileForm.soDienThoai || '').trim()
  if (phone && !/^0\d{9}$/.test(phone)) {
    toast.error('Số điện thoại không hợp lệ. Vui lòng nhập đúng 10 chữ số bắt đầu bằng 0.')
    return
  }

  saving.value = true
  try {
    await taiKhoanService.update(account.value.id, {
      ...account.value,
      email: String(profileForm.email || '').trim().toLowerCase(),
      avatar: avatarToPersist
    })

    if (customer.value?.id) {
      await updateKhachHang(customer.value.id, {
        ...customer.value,
        tenKhachHang: profileForm.tenKhachHang,
        gioiTinh: profileForm.gioiTinh,
        ngaySinh: profileForm.ngaySinh || null,
        soDienThoai: profileForm.soDienThoai
      })
    }

    localStorage.setItem('userEmail', String(profileForm.email || '').trim().toLowerCase())
    localStorage.setItem('userName', String(profileForm.tenKhachHang || '').trim())
    localStorage.setItem('userPhone', String(profileForm.soDienThoai || '').trim())
    dispatchAuthContextChanged()

    if (customer.value?.id) {
      toast.success('Cập nhật thông tin tài khoản thành công')
    } else {
      toast.success('Đã lưu thông tin tài khoản cơ bản. Hồ sơ khách hàng sẽ cập nhật sau khi backend sẵn sàng.')
    }

    await loadAccountCenter()
  } catch (e) {
    console.error('Save profile failed:', e)
    toast.error('Không thể cập nhật thông tin. Vui lòng kiểm tra lại dữ liệu.')
  } finally {
    saving.value = false
  }
}

const updateAvatarFromFile = (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = () => {
    profileForm.avatar = String(reader.result || '')
  }
  reader.readAsDataURL(file)
}

const triggerAvatarPicker = () => {
  avatarInputRef.value?.click()
}

const changePassword = async () => {
  if (!account.value?.id) {
    toast.error('Không tìm thấy thông tin tài khoản. Vui lòng đăng nhập lại.')
    return
  }

  // Check if customer account is inactive
  if (customer.value?.trangThai && customer.value.trangThai !== 'Hoạt động') {
    toast.error('Tài khoản của bạn đã ngừng hoạt động. Vui lòng liên hệ hỗ trợ.')
    return
  }

  if (!passwordForm.currentPassword) {
    toast.error('Vui lòng nhập mật khẩu hiện tại')
    return
  }

  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    toast.error('Mật khẩu mới phải có ít nhất 6 ký tự')
    return
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    toast.error('Mật khẩu xác nhận không khớp')
    return
  }

  try {
    await taiKhoanService.updatePassword(
      account.value.id,
      passwordForm.currentPassword,
      passwordForm.newPassword
    )
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    toast.success('Đổi mật khẩu thành công')
  } catch (e) {
    console.error('Change password failed:', e)
    toast.error('Không thể đổi mật khẩu. Vui lòng kiểm tra mật khẩu hiện tại.')
  }
}

const startEditAddress = (item) => {
  addressForm.id = item?.id || null
  addressForm.diaChiCuThe = item?.diaChiCuThe || ''
  addressForm.tinhThanh = item?.tinhThanh || ''
  addressForm.quanHuyen = item?.quanHuyen || ''
  addressForm.phuongXa = item?.phuongXa || ''
  addressForm.trangThai = item?.trangThai || 'Hoạt động'
}

const resetAddressForm = () => {
  addressForm.id = null
  addressForm.diaChiCuThe = ''
  addressForm.tinhThanh = ''
  addressForm.quanHuyen = ''
  addressForm.phuongXa = ''
  addressForm.trangThai = 'Hoạt động'
}

const saveAddress = async () => {
  if (!customer.value?.id) {
    toast.warning('Tài khoản này chưa có hồ sơ khách hàng, chưa thể lưu địa chỉ.')
    return
  }
  if (!addressForm.diaChiCuThe.trim()) {
    toast.error('Vui lòng nhập địa chỉ cụ thể')
    return
  }

  const payload = {
    khachHang: { id: customer.value.id },
    diaChiCuThe: addressForm.diaChiCuThe,
    tinhThanh: addressForm.tinhThanh,
    quanHuyen: addressForm.quanHuyen,
    phuongXa: addressForm.phuongXa,
    trangThai: addressForm.trangThai || 'Hoạt động'
  }

  try {
    if (addressForm.id) {
      await updateDiaChi(addressForm.id, payload)
      toast.success('Cập nhật địa chỉ thành công')
    } else {
      await createDiaChi(payload)
      toast.success('Thêm địa chỉ thành công')
    }
    resetAddressForm()
    await loadAddresses(customer.value.id)
  } catch (e) {
    console.error('Save address failed:', e)
    toast.error('Không thể lưu địa chỉ')
  }
}

const removeAddress = async (id) => {
  if (!id || !customer.value?.id) {
    toast.warning('Tài khoản này chưa có hồ sơ khách hàng, chưa thể xóa địa chỉ.')
    return
  }
  try {
    await deleteDiaChi(id)
    toast.success('Đã xóa địa chỉ')
    await loadAddresses(customer.value.id)
  } catch (e) {
    console.error('Delete address failed:', e)
    toast.error('Không thể xóa địa chỉ')
  }
}

const deliveryOrders = computed(() => {
  if (!hasCustomerProfile.value) return []
  return orders.value.filter((item) => {
    const statusCode = String(item?.orderStatusCode || '').trim().toUpperCase()
    return statusCode === 'DANG_GIAO' || statusCode === 'CHO_LAY_HANG'
  })
})

const goBackHome = () => {
  router.push('/trang-chu')
}

const goToOrderLookup = () => {
  const publicOrigin = resolvePublicAppOrigin()
  if (publicOrigin) {
    window.location.href = `${publicOrigin}/tra-cuu-don-hang`
    return
  }

  router.push('/tra-cuu-don-hang')
}

const logout = () => {
  localStorage.removeItem('role')
  localStorage.removeItem('userId')
  localStorage.removeItem('userEmail')
  router.push('/auth/customer-login')
}

onMounted(loadAccountCenter)

watch(() => route.query.tab, (tab) => {
  activeTab.value = resolveInitialTab(tab)
})

// Auto-open order detail when navigated from a notification link
const notificationOrderOpened = ref(false)
watch(orders, async (list) => {
  if (notificationOrderOpened.value) return
  const orderId = route.query.orderId
  if (!orderId || !list.length) return
  const order = list.find(o => String(o.id) === String(orderId))
  if (order) {
    notificationOrderOpened.value = true
    activeTab.value = 'orders'
    await nextTick()
    await openOrderDetail(order)
  }
})
</script>

<template>
  <div>
  <SiteNav />
  <main class="account-page">
    <div class="account-wrap">
      <header class="account-header">
        <div>
          <h1>Tài khoản của tôi</h1>
          <p>Thông tin người dùng, địa chỉ, mật khẩu và đơn hàng</p>
        </div>
        <div class="header-actions">
          <button class="btn" type="button" @click="goToOrderLookup">Tra cứu đơn hàng</button>
          <button class="btn" type="button" @click="goBackHome">Về trang chủ</button>
        </div>
      </header>

      <div class="tabs" v-if="!loading && !error">
        <button class="tab" :class="{ active: activeTab === 'profile' }" @click="activeTab = 'profile'">Thông tin người dùng</button>
        <button class="tab" :class="{ active: activeTab === 'orders' }" :disabled="!hasCustomerProfile" @click="activeTab = 'orders'">Lịch sử mua hàng</button>
        <button class="tab" :class="{ active: activeTab === 'delivery' }" :disabled="!hasCustomerProfile" @click="activeTab = 'delivery'">Đang giao</button>
        <button class="tab" :class="{ active: activeTab === 'lookup' }" @click="activeTab = 'lookup'">Tra cứu đơn hàng</button>
      </div>

      <section class="card" v-if="loading">Đang tải trung tâm tài khoản...</section>
      <section class="card error" v-if="!loading && error">{{ error }}</section>

      <template v-if="!loading && !error">
        <section class="card" v-show="activeTab === 'profile'">
          <h2>Thông tin người dùng</h2>

          <div class="avatar-block">
            <img v-if="profileForm.avatar" :src="profileForm.avatar" class="avatar" alt="Avatar" />
            <div v-else class="avatar avatar-fallback">{{ (profileForm.tenKhachHang || 'U').slice(0, 1) }}</div>
            <div class="avatar-actions">
              <input
                ref="avatarInputRef"
                class="avatar-input"
                type="file"
                accept="image/*"
                @change="updateAvatarFromFile"
              />
              <button type="button" class="btn avatar-btn" @click="triggerAvatarPicker">Đổi ảnh đại diện</button>
              <p class="avatar-help">Khuyến nghị ảnh vuông, nền sáng, định dạng JPG/PNG.</p>
            </div>
          </div>

          <div class="form-grid">
            <label>
              Họ tên
              <input type="text" v-model="profileForm.tenKhachHang" />
            </label>
            <label>
              Email
              <input type="email" v-model="profileForm.email" />
            </label>
            <label>
              Số điện thoại
              <input type="text" v-model="profileForm.soDienThoai" />
            </label>
            <label>
              Giới tính
              <select v-model="profileForm.gioiTinh">
                <option value="Nam">Nam</option>
                <option value="Nữ">Nữ</option>
                <option value="Khác">Khác</option>
              </select>
            </label>
            <label>
              Ngày sinh
              <input type="date" v-model="profileForm.ngaySinh" />
            </label>
          </div>

          <div class="section-actions">
            <button class="btn primary" :disabled="saving" @click="saveProfileInfo">
              {{ saving ? 'Đang lưu...' : 'Lưu thông tin' }}
            </button>
          </div>

          <h3>Đổi mật khẩu</h3>
          <div class="form-grid">
            <label>
              Mật khẩu hiện tại
              <input type="password" v-model="passwordForm.currentPassword" />
            </label>
            <label>
              Mật khẩu mới
              <input type="password" v-model="passwordForm.newPassword" />
            </label>
            <label>
              Xác nhận mật khẩu mới
              <input type="password" v-model="passwordForm.confirmPassword" />
            </label>
          </div>
          <div class="section-actions">
            <button class="btn" @click="changePassword">Đổi mật khẩu</button>
          </div>

          <h3>Địa chỉ nhận hàng</h3>
          <div class="address-form">
            <input type="text" v-model="addressForm.diaChiCuThe" placeholder="Số nhà, tên đường" />
            <input type="text" v-model="addressForm.phuongXa" placeholder="Phường/Xã" />
            <input type="text" v-model="addressForm.quanHuyen" placeholder="Quận/Huyện" />
            <input type="text" v-model="addressForm.tinhThanh" placeholder="Tỉnh/Thành phố" />
            <div class="section-actions">
              <button class="btn" @click="resetAddressForm">Làm mới</button>
              <button class="btn primary" @click="saveAddress">{{ addressForm.id ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ' }}</button>
            </div>
          </div>

          <div class="address-list" v-if="addresses.length">
            <article class="address-item" v-for="addr in addresses" :key="addr.id">
              <div>
                <strong>{{ addr.diaChiCuThe }}</strong>
                <p>{{ addr.phuongXa }}, {{ addr.quanHuyen }}, {{ addr.tinhThanh }}</p>
              </div>
              <div class="item-actions">
                <button class="btn" @click="startEditAddress(addr)">Sửa</button>
                <button class="btn danger" @click="removeAddress(addr.id)">Xóa</button>
              </div>
            </article>
          </div>
        </section>

        <section class="card" v-show="activeTab === 'orders'">
          <h2>Lịch sử mua hàng</h2>
          <div class="order-list" v-if="orders.length">
            <article class="order-item" v-for="order in orders" :key="order.id">
              <div class="order-head">
                <strong>#{{ order.maHoaDon || `HD-${order.id}` }}</strong>
                <span class="status-badge" :class="`status-${getOrderStatusTone(order)}`">{{ getOrderStatusLabel(order) }}</span>
              </div>
              <div class="order-meta">
                <span>Ngày tạo: {{ formatDateTime(order.ngayTao) }}</span>
                <span>Tổng tiền: {{ formatCurrency(getOrderTotal(order)) }}</span>
                <span>Phương thức: {{ order.phuongThucThanhToan || 'COD' }}</span>
              </div>
              <div
                v-if="shouldShowPaymentFlow(order)"
                class="payment-flow"
                :class="`payment-${getOrderPaymentFlow(order).tone}`"
              >
                {{ getOrderPaymentFlow(order).label }}
              </div>
              <div class="order-actions">
                <button class="btn" @click="openOrderDetail(order)">Xem chi tiết</button>
                <button
                  v-if="canConfirmPayment(order)"
                  class="btn primary"
                  :disabled="confirmingOrderId === order.id"
                  @click="confirmPayment(order)"
                >
                  {{ confirmingOrderId === order.id ? 'Đang gửi...' : 'Xác nhận thanh toán' }}
                </button>
              </div>
            </article>
          </div>
          <p class="muted" v-else>Bạn chưa có đơn hàng nào.</p>
        </section>

        <section class="card" v-show="activeTab === 'delivery'">
          <h2>Theo dõi đơn hàng đang giao</h2>
          <div class="order-list" v-if="deliveryOrders.length">
            <article class="order-item" v-for="order in deliveryOrders" :key="order.id">
              <div class="order-head">
                <strong>#{{ order.maHoaDon || `HD-${order.id}` }}</strong>
                <span class="status shipping">{{ getOrderStatusLabel(order) }}</span>
              </div>
              <div class="order-meta">
                <span>Ngày tạo: {{ formatDateTime(order.ngayTao) }}</span>
                <span>Địa chỉ nhận: {{ order.diaChiNhanHang || 'Đang cập nhật' }}</span>
              </div>
            </article>
          </div>
          <p class="muted" v-else>Hiện không có đơn hàng đang giao.</p>
        </section>

        <section class="card" v-show="activeTab === 'lookup'">
          <h2>Tra cứu đơn hàng</h2>
          <p class="muted">Mở công cụ tra cứu theo mã đơn và số điện thoại để kiểm tra nhanh trạng thái đơn hàng.</p>
          <div class="section-actions">
            <button class="btn primary" type="button" @click="goToOrderLookup">Mở trang tra cứu đơn hàng</button>
          </div>
        </section>

        <div class="page-bottom-actions" v-if="activeTab === 'profile'">
          <button class="btn danger" @click="logout">Đăng xuất</button>
        </div>
      </template>
    </div>
  </main>

  <!-- Modal chi tiết đơn hàng -->
  <div v-if="selectedOrder" class="order-detail-overlay" @click.self="closeOrderDetail">
    <div class="order-detail-modal">
      <div class="order-detail-header">
        <h3>Chi tiết đơn <span class="order-code">#{{ selectedOrder.maHoaDon || selectedOrder.id }}</span></h3>
        <button class="order-detail-close" @click="closeOrderDetail">✕</button>
      </div>
      <div class="order-detail-body">
        <div v-if="loadingOrderDetail" class="order-detail-loading">
          <span class="spinner"></span>
          <p>Đang tải chi tiết đơn hàng...</p>
        </div>
        <template v-else>
          <!-- Info grid -->
          <div class="order-detail-info">
            <div class="info-item">
              <span class="info-label">Trạng thái</span>
              <span class="info-value status-badge" :class="`status-${getOrderStatusTone(selectedOrder)}`">{{ getOrderStatusLabel(selectedOrder) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Ngày tạo</span>
              <span class="info-value">{{ formatDateTime(selectedOrder.ngayTao) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Phương thức thanh toán</span>
              <span class="info-value">{{ selectedOrder.phuongThucThanhToan || 'COD' }}</span>
            </div>
            <div class="info-item" v-if="selectedOrder.tenNguoiNhan || selectedOrder.tenKhachHang">
              <span class="info-label">Người nhận</span>
              <span class="info-value">{{ selectedOrder.tenNguoiNhan || selectedOrder.tenKhachHang }}</span>
            </div>
            <div class="info-item full" v-if="selectedOrder.diaChiNhanHang">
              <span class="info-label">Địa chỉ nhận hàng</span>
              <span class="info-value">{{ selectedOrder.diaChiNhanHang }}</span>
            </div>
            <div class="info-item" v-if="selectedOrder.soDienThoaiNhanHang">
              <span class="info-label">SĐT nhận hàng</span>
              <span class="info-value">{{ selectedOrder.soDienThoaiNhanHang }}</span>
            </div>
            <div class="info-item" v-if="selectedOrder.ngayNhanHangDuKien">
              <span class="info-label">Dự kiến giao</span>
              <span class="info-value">{{ formatDateTime(selectedOrder.ngayNhanHangDuKien) }}</span>
            </div>
            <div class="info-item" v-if="selectedOrder.ghiChu">
              <span class="info-label">Ghi chú</span>
              <span class="info-value">{{ selectedOrder.ghiChu }}</span>
            </div>
          </div>

          <!-- Product table -->
          <div class="order-detail-products" v-if="getOrderItems(selectedOrder).length">
            <h4>Sản phẩm</h4>
            <div class="product-list">
              <div class="product-row" v-for="(item, i) in getOrderItems(selectedOrder)" :key="i">
                <div class="product-img">
                  <img :src="item.anh || item.image || ''" @error="$event.target.src = fallbackImageForVariant({ id: item.productId || item.sanPhamId || item.spctId || item.sanPhamChiTietId || item.id, maSanPham: item.maSanPham, tenSanPham: item.tenSanPhamChiTiet || item.name || '', tenMauSac: getOrderItemColor(item), maChiTietSanPham: item.maSanPhamChiTiet }) || fallbackImageFor(item.spctId || item.sanPhamChiTietId || item.id, item.tenSanPhamChiTiet || item.name || '')" />
                </div>
                <div class="product-info">
                  <span class="product-name">{{ item.tenSanPhamChiTiet || item.name || `SP #${item.spctId || item.id}` }}</span>
                  <span v-if="getOrderItemColor(item) || getOrderItemSize(item)" class="product-meta-inline">
                    <template v-if="getOrderItemColor(item)">Màu: {{ getOrderItemColor(item) }}</template>
                    <template v-if="getOrderItemColor(item) && getOrderItemSize(item)"> • </template>
                    <template v-if="getOrderItemSize(item)">Size: {{ getOrderItemSize(item) }}</template>
                  </span>
                  <span class="product-qty">SL: {{ item.soLuong || item.quantity }} × {{ formatCurrency(item.giaBan || item.price || 0) }}</span>
                  <span
                    v-for="(voucher, voucherIndex) in getOrderItemVouchers(selectedOrder, item)"
                    :key="`${voucher.code || 'applied'}-${voucherIndex}`"
                    class="product-voucher-inline"
                  >
                    Voucher {{ voucher.code || 'đã áp dụng' }}
                    <template v-if="voucher.discount > 0"> • -{{ formatCurrency(voucher.discount) }}</template>
                  </span>
                </div>
                <div class="product-line-total">
                  {{ formatCurrency(item.thanhTien || (item.giaBan || item.price || 0) * (item.soLuong || item.quantity || 0)) }}
                </div>
              </div>
            </div>
          </div>
          <p class="muted" v-else>Không có thông tin sản phẩm.</p>

          <!-- Totals -->
          <div class="order-detail-totals">
            <div class="total-line" v-if="selectedOrder.phiShip">
              <span>Phí vận chuyển</span>
              <span>{{ formatCurrency(selectedOrder.phiShip) }}</span>
            </div>
            <div class="total-line discount" v-if="selectedOrder.giaSauGiamGia">
              <span>Giảm giá</span>
              <span>-{{ formatCurrency(selectedOrder.giaSauGiamGia) }}</span>
            </div>
            <div class="total-line grand">
              <span>Tổng cộng</span>
              <strong>{{ formatCurrency(getOrderTotal(selectedOrder)) }}</strong>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>

  </div>
</template>

<style scoped>
.account-page {
  min-height: 0;
  padding: 12px 10px;
  background: transparent;
}

.account-wrap {
  max-width: 920px;
  margin: 0 auto;
  border-radius: 24px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.7);
}

.account-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.account-header h1 {
  margin: 0;
  color: #0f172a;
}

.account-header p {
  margin: 6px 0 0;
  color: #64748b;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.tab {
  border: 1px solid #d4dde9;
  border-radius: 999px;
  background: #fff;
  color: #334155;
  padding: 8px 14px;
  cursor: pointer;
  font-weight: 600;
}

.tab.active {
  background: #111827;
  color: #fff;
  border-color: #111827;
}

.tab:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.card {
  background: #fff;
  border: 1px solid #d9e1ec;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  padding: 16px;
  margin-bottom: 12px;
}

.card h2 {
  margin-top: 0;
}

.card h3 {
  margin: 18px 0 10px;
}

.btn {
  border: 1px solid #d1dae6;
  border-radius: 12px;
  background: #fff;
  color: #0f172a;
  padding: 10px 14px;
  cursor: pointer;
  font-weight: 600;
}

.btn.primary {
  border-color: #dc2626;
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: #fff;
}

.btn.danger {
  border-color: #ef4444;
  color: #b91c1c;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #475569;
  font-weight: 600;
}

input,
select {
  border: 1px solid #d4dde9;
  border-radius: 10px;
  padding: 10px 12px;
}
select {
  padding-right: 34px;
}

.section-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.avatar-block {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #d4dde9;
}

.avatar-fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #111827;
  color: #fff;
  font-weight: 700;
}

.avatar-actions {
  flex: 1;
  display: grid;
  gap: 8px;
}

.avatar-input {
  display: none;
}

.avatar-btn {
  width: fit-content;
}

.avatar-help {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.page-bottom-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.address-form {
  display: grid;
  gap: 8px;
}

.address-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.address-item {
  border: 1px solid #e5ebf4;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.address-item p {
  margin: 6px 0 0;
  color: #64748b;
}

.item-actions {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.order-list {
  display: grid;
  gap: 10px;
}

.order-item {
  border: 1px solid #e5ebf4;
  border-radius: 12px;
  padding: 12px;
}

.order-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.status {
  background: #f1f5f9;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.status.shipping {
  background: #dbeafe;
  color: #1d4ed8;
}

.order-meta {
  margin-top: 8px;
  display: grid;
  gap: 4px;
  color: #64748b;
  font-size: 14px;
}

.payment-flow {
  margin-top: 10px;
  font-size: 13px;
  font-weight: 700;
}

.payment-success {
  color: #166534;
}

.payment-warning {
  color: #b45309;
}

.payment-neutral {
  color: #475569;
}

.order-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

/* Order detail modal */
.order-detail-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.5);
  z-index: 1000;
  display: flex; align-items: center; justify-content: center;
  padding: 16px;
  animation: fadeIn .2s ease;
}
@keyframes fadeIn { from { opacity: 0 } to { opacity: 1 } }
.order-detail-modal {
  background: #fff; border-radius: 20px;
  width: 100%; max-width: 720px; max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 24px 80px rgba(0,0,0,0.25);
  animation: slideUp .25s ease;
}
@keyframes slideUp { from { transform: translateY(24px); opacity: 0 } to { transform: translateY(0); opacity: 1 } }
.order-detail-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 24px; border-bottom: 1px solid #e5e7eb;
  position: sticky; top: 0; background: #fff; z-index: 1;
  border-radius: 20px 20px 0 0;
}
.order-detail-header h3 { margin: 0; font-size: 18px; font-weight: 700; color: #111; }
.order-code { color: #dc2626; }
.order-detail-close {
  width: 36px; height: 36px; border-radius: 50%;
  border: 1px solid #e5e7eb; background: #f9fafb;
  font-size: 18px; cursor: pointer; display: flex;
  align-items: center; justify-content: center;
  transition: background .15s;
}
.order-detail-close:hover { background: #f1f5f9; }
.order-detail-body { padding: 24px; display: flex; flex-direction: column; gap: 20px; }
.order-detail-loading { text-align: center; padding: 40px 0; color: #94a3b8; }
.order-detail-loading .spinner {
  display: inline-block; width: 28px; height: 28px;
  border: 3px solid #e5e7eb; border-top-color: #dc2626;
  border-radius: 50%; animation: spin .6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg) } }
.order-detail-loading p { margin-top: 12px; font-size: 14px; }

/* Info grid */
.order-detail-info {
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
  background: #f8fafc; border-radius: 14px; padding: 16px 18px;
}
.info-item { display: flex; flex-direction: column; gap: 3px; }
.info-item.full { grid-column: 1 / -1; }
.info-label { font-size: 11px; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; font-weight: 600; }
.info-value { font-size: 15px; color: #1e293b; font-weight: 500; }
.status-badge { padding: 3px 10px; border-radius: 6px; font-size: 13px; font-weight: 600; display: inline-block; width: fit-content; }
.status-success { background: #dcfce7; color: #166534; }
.status-danger { background: #fee2e2; color: #991b1b; }
.status-warning { background: #fef3c7; color: #92400e; }
.status-neutral { background: #f1f5f9; color: #475569; }

/* Product list */
.order-detail-products h4 { margin: 0 0 12px; font-size: 15px; font-weight: 700; color: #111; }
.product-list { display: flex; flex-direction: column; gap: 10px; }
.product-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; border-radius: 12px; background: #f9fafb;
  border: 1px solid #f1f5f9;
}
.product-img { flex-shrink: 0; width: 60px; height: 60px; border-radius: 10px; overflow: hidden; background: #f1f5f9; }
.product-img img { width: 100%; height: 100%; object-fit: cover; }
.product-img-placeholder { width: 100%; height: 100%; background: #e5e7eb; border-radius: 10px; }
.product-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.product-name { font-size: 14px; font-weight: 600; color: #1e293b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.product-meta-inline { font-size: 12px; color: #475569; }
.product-qty { font-size: 13px; color: #64748b; }
.product-voucher-inline { font-size: 12px; color: #b91c1c; font-weight: 600; }
.product-line-total { font-size: 15px; font-weight: 700; color: #111; white-space: nowrap; }

/* Totals */
.order-detail-totals {
  border-top: 1px solid #e5e7eb; padding-top: 14px;
  display: flex; flex-direction: column; gap: 8px;
}
.total-line { display: flex; justify-content: space-between; font-size: 15px; color: #334155; }
.total-line.discount { color: #dc2626; }
.total-line.grand { font-size: 17px; padding-top: 8px; border-top: 1px dashed #d1d5db; color: #111; }
.total-line.grand strong { color: #dc2626; }

.muted {
  color: #64748b;
}

.error {
  color: #b91c1c;
}

@media (max-width: 768px) {
  .account-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .btn {
    flex: 1;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .address-item {
    flex-direction: column;
  }
}
</style>


