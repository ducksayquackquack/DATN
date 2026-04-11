<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { createHoaDon, addHoaDonItem, updateHoaDon, updateHoaDonBySystemEvent, getAllHoaDon, getHoaDonById } from "../../../services/hoaDonService"
import { getAllSanPham } from "../../../services/sanPhamService"
import { createKhachHang, getAllKhachHang } from "../../../services/KhachHangService"
import { getAllNhanVien, getNhanVienByTaiKhoanId } from "../../../services/nhanVienService"
import { getDiaChiByKhachHang } from "../../../services/diaChiService"
import { quoteShippingFeeAll } from "../../../services/shippingQuoteService"
import { Plus, Search, Trash2, X, Minus, ShoppingBag, MapPin, Truck } from "lucide-vue-next"
import { appendPaymentFlowTag, PAYMENT_FLOW_TAGS } from "../../../utils/paymentWorkflow"
import { buildOrderLookupTrackingUrl } from "../../../utils/publicTrackingUrl"
import VoucherSelector from "../../../components/voucher/VoucherSelector.vue"
import { resolveApiOrigin } from "../../../utils/apiOrigin"
import { getProductImageOverride, getProductImageConfig } from "../../../utils/productImageOverrides"
import { fallbackImageForVariant } from "../../../utils/productImageFallback"
import { getActiveCampaignMap } from "../../../services/campaignPricingService"
import logoFallback from "../../../assets/img/logo/new logo.png?url"
import img1 from "../../../assets/img/Jackets/bomber/bomber-da-lon.jpg?url"
import img2 from "../../../assets/img/Jackets/bomber/bomber-dang-lung.jpg?url"
import img3 from "../../../assets/img/Jackets/bomber/bomber-gia-da.jpg?url"
import img4 from "../../../assets/img/Jackets/bomber/bomber-nhe-cotton.jpg?url"
import img5 from "../../../assets/img/Jackets/hoodie/hoodie-dang-hop.jpg?url"
import img6 from "../../../assets/img/Jackets/hoodie/hoodie-in-hinh.jpg?url"
import img7 from "../../../assets/img/Jackets/hoodie/hoodie-keo-khoa.jpg?url"
import img8 from "../../../assets/img/Jackets/coach/coach-cach-nhiet.jpg?url"
import img9 from "../../../assets/img/Jackets/coach/coach-da-asos.jpg?url"
import img10 from "../../../assets/img/Jackets/coach/coach-gia-da.jpg?url"
import img11 from "../../../assets/img/Jackets/coach/coach-long-cuu.jpg?url"
// New products
import img12 from "../../../assets/img/Jackets/bomber/bomber-astronaut/bomber-astronaut-black.PNG?url"
import img13 from "../../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-black.PNG?url"
import img14 from "../../../assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-black.PNG?url"
import img15 from "../../../assets/img/Jackets/coach/coach-leopard/coach-leopard.PNG?url"
import img16 from "../../../assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-black.PNG?url"
import img17 from "../../../assets/img/Jackets/coach/coach-tiger-stripe/coach-tiger-stripe.PNG?url"
import img18 from "../../../assets/img/Jackets/hoodie/hoodie-camo/hoodie-camo-black.PNG?url"
import img19 from "../../../assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-blue.PNG?url"
import img20 from "../../../assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-black.PNG?url"
import img12b from "../../../assets/img/Jackets/bomber/bomber-astronaut/bomber-astronaut-blue.PNG?url"
import img13b from "../../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-green.PNG?url"
import img13c from "../../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-brown.PNG?url"
import img13d from "../../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-red.PNG?url"
import img13e from "../../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-white.PNG?url"
import img14b from "../../../assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-blue.PNG?url"
import img14c from "../../../assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-green.PNG?url"
import img16b from "../../../assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-red.PNG?url"
import img16c from "../../../assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-white.PNG?url"
import img18b from "../../../assets/img/Jackets/hoodie/hoodie-camo/hoodie-camo-white.PNG?url"
import img19b from "../../../assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-white.PNG?url"
import img20b from "../../../assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-gray.PNG?url"
import img20c from "../../../assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-red.PNG?url"

const router = useRouter()
const route = useRoute()
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")

const panelBasePath = computed(() => route.path.startsWith('/employee/') ? '/employee' : '/admin')
const isEmployeePanel = computed(() => route.path.startsWith('/employee/'))

const loading = ref(false)
const saving = ref(false)
const currentEmployeeId = ref(null)
const currentSellerName = ref("")

const nhanVienList = ref([])
const khachHangList = ref([])
const variants = ref([])

const cashierId = ref(null)
const customerId = ref(null)
const taiQuay = ref(true)
const paymentMethod = ref("CASH")
const orderNote = ref("")
const discount = ref(0)
const selectedVoucher = ref(null)
const creatingCustomer = ref(false)
const showCustomerSearchModal = ref(false)
const showHeaderMenu = ref(false)
const customerPhoneSearch = ref("")
const customerDraftName = ref("")
const customerSearchInputRef = ref(null)
const quickCustomer = ref({ tenKhachHang: "", soDienThoai: "", email: "" })

const lines = ref([])
const showProductModal = ref(false)
const productSearchKeyword = ref("")

const MAX_POS_TABS = 5
const posTabs = ref([null]) // array of draft snapshots; null = empty tab
const activeTabIndex = ref(0)

const getPosDraftKey = () => {
  const role = String(localStorage.getItem("role") || "").trim().toUpperCase()
  const userId = String(localStorage.getItem("userId") || "").trim()
  return `banhang:tabs:${role || "UNKNOWN"}:${userId || "0"}`
}

function captureTabState() {
  return {
    taiQuay: Boolean(taiQuay.value),
    paymentMethod: String(paymentMethod.value || "CASH"),
    orderNote: String(orderNote.value || ""),
    discount: Number(discount.value || 0),
    selectedVoucher: selectedVoucher.value || null,
    cashierId: cashierId.value ? Number(cashierId.value) : null,
    customerId: customerId.value ? Number(customerId.value) : null,
    selectedAddressId: selectedAddressId.value ?? null,
    manualAddress: { ...manualAddress.value },
    shippingProvider: String(shippingProvider.value || "GHN"),
    lines: lines.value.map((line) => ({
      spctId: Number(line?.spctId || 0),
      soLuong: Math.max(Number(line?.soLuong || 1), 1)
    })).filter((line) => line.spctId > 0)
  }
}

function applyTabState(state) {
  if (!state) {
    taiQuay.value = true
    paymentMethod.value = "CASH"
    orderNote.value = ""
    discount.value = 0
    selectedVoucher.value = null
    customerId.value = null
    selectedAddressId.value = null
    manualAddress.value = { soDienThoai: "", diaChiCuThe: "", phuongXa: "", quanHuyen: "", tinhThanh: "" }
    shippingProvider.value = "GHN"
    lines.value = []
    return
  }
  taiQuay.value = Boolean(state.taiQuay)
  paymentMethod.value = String(state.paymentMethod || "CASH")
  orderNote.value = String(state.orderNote || "")
  discount.value = Number(state.discount || 0)
  selectedVoucher.value = state.selectedVoucher || null
  if (state.cashierId) cashierId.value = Number(state.cashierId)
  customerId.value = state.customerId ? Number(state.customerId) : null
  selectedAddressId.value = state.selectedAddressId ?? null
  if (state.manualAddress && typeof state.manualAddress === "object") {
    manualAddress.value = {
      soDienThoai: String(state.manualAddress.soDienThoai || ""),
      diaChiCuThe: String(state.manualAddress.diaChiCuThe || ""),
      phuongXa: String(state.manualAddress.phuongXa || ""),
      quanHuyen: String(state.manualAddress.quanHuyen || ""),
      tinhThanh: String(state.manualAddress.tinhThanh || "")
    }
  }
  shippingProvider.value = String(state.shippingProvider || "GHN")
  const lineRows = Array.isArray(state.lines) ? state.lines : []
  const restored = []
  for (const row of lineRows) {
    const spctId = Number(row?.spctId || 0)
    const qty = Math.max(Number(row?.soLuong || 1), 1)
    if (!spctId) continue
    const variant = variants.value.find((item) => Number(item?.spctId) === spctId)
    if (!variant) continue
    const maxQty = Math.max(Number(variant.soLuongTon || 0), 0)
    if (maxQty <= 0) continue
    restored.push({ ...variant, soLuong: Math.min(qty, maxQty) })
  }
  lines.value = restored
}

function switchPosTab(index) {
  if (index === activeTabIndex.value || index < 0 || index >= posTabs.value.length) return
  posTabs.value[activeTabIndex.value] = captureTabState()
  activeTabIndex.value = index
  applyTabState(posTabs.value[index])
  persistPosDraft()
}

function addPosTab() {
  if (posTabs.value.length >= MAX_POS_TABS) {
    window.toast?.warning?.("Tối đa " + MAX_POS_TABS + " hoá đơn chờ")
    return
  }
  posTabs.value[activeTabIndex.value] = captureTabState()
  posTabs.value.push(null)
  activeTabIndex.value = posTabs.value.length - 1
  applyTabState(null)
  persistPosDraft()
}

function removePosTab(index) {
  if (posTabs.value.length <= 1) {
    window.toast?.warning?.("Phải giữ ít nhất 1 hoá đơn")
    return
  }
  posTabs.value.splice(index, 1)
  if (activeTabIndex.value >= posTabs.value.length) {
    activeTabIndex.value = posTabs.value.length - 1
  }
  if (index === activeTabIndex.value || activeTabIndex.value >= posTabs.value.length) {
    activeTabIndex.value = Math.min(activeTabIndex.value, posTabs.value.length - 1)
    applyTabState(posTabs.value[activeTabIndex.value])
  }
  persistPosDraft()
}

function clearPosDraft() {
  try {
    localStorage.removeItem(getPosDraftKey())
  } catch { /* ignore */ }
}

function persistPosDraft() {
  try {
    posTabs.value[activeTabIndex.value] = captureTabState()
    localStorage.setItem(getPosDraftKey(), JSON.stringify({
      activeIndex: activeTabIndex.value,
      tabs: posTabs.value
    }))
  } catch { /* ignore */ }
}

function restorePosDraft() {
  try {
    const raw = localStorage.getItem(getPosDraftKey())
    if (!raw) { posTabs.value = [null]; activeTabIndex.value = 0; return }
    const data = JSON.parse(raw)
    if (data && Array.isArray(data.tabs) && data.tabs.length > 0) {
      posTabs.value = data.tabs
      activeTabIndex.value = Math.min(Number(data.activeIndex || 0), posTabs.value.length - 1)
    } else if (data && typeof data === "object" && !Array.isArray(data.tabs)) {
      // Migrate old single-draft format
      posTabs.value = [data]
      activeTabIndex.value = 0
    } else {
      posTabs.value = [null]
      activeTabIndex.value = 0
    }
    applyTabState(posTabs.value[activeTabIndex.value])
  } catch {
    posTabs.value = [null]
    activeTabIndex.value = 0
  }
}

// Shipping address (for delivery mode)
const customerAddresses = ref([])
const selectedAddressId = ref(null)
const manualAddress = ref({ soDienThoai: "", diaChiCuThe: "", phuongXa: "", quanHuyen: "", tinhThanh: "" })

// Payment modal
const showPaymentModal = ref(false)
const paymentCash = ref(0)
const paymentBank = ref(0)


function applyPaymentMode(mode) {
  if (mode === 'CASH') {
    // Single method: keep cash editable by staff, clear the other field.
    paymentBank.value = 0
  } else if (mode === 'BANK') {
    // Single method: keep bank editable by staff, clear the other field.
    paymentCash.value = 0
  }
}

// Shipping fee (delivery mode)
const shippingFee = ref(0)
const shippingLoading = ref(false)
const shippingProvider = ref("GHN")

const fallbackImages = [img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11, img12, img13, img14, img15, img16, img17, img18, img19, img20]
const mappedFallbackByCodeNum = {
  1: img1,
  2: img2,
  3: img3,
  4: img4,
  5: img5,
  6: img6,
  7: img7,
  8: img8,
  9: img9,
  10: img10,
  11: img11,
  12: img12,
  13: img13,
  14: img14,
  15: img15,
  16: img16,
  17: img17,
  18: img18,
  19: img19,
  20: img20
}
const mappedFallbackByCode = {
  SP001: img1,   // Bomber da lộn
  SP002: img2,   // Bomber dáng lửng
  SP003: img3,   // Bomber da có túi
  SP004: img4,   // Bomber cotton nhẹ
  SP005: img5,   // Hoodie dáng hộp
  SP006: img6,   // Hoodie in hình
  SP007: img7,   // Hoodie kéo khoá
  SP008: img8,   // Coach cách nhiệt
  SP009: img9,   // Coach da trơn
  SP010: img10,  // Coach giả da
  SP011: img11,  // Coach lông cừu
  SP012: img12,  // Bomber Astronaut
  SP013: img13,  // Bomber Embroidered Fuzzy
  SP014: img14,  // Bomber Windbreaker
  SP015: img15,  // Coach Leopard
  SP016: img16,  // Coach Longsleeve
  SP017: img17,  // Coach Tiger Stripe
  SP018: img18,  // Hoodie Camo
  SP019: img19,  // Hoodie Zip Boxy
  SP020: img20,  // Hoodie Zip Silk
  "ATID070-12": img12,
  "ATID070-13": img13,
  "ATID070-14": img14,
  "ATID070-15": img15,
  "ATID070-16": img16,
  "ATID070-17": img17,
  "ATID070-18": img18,
  "ATID070-19": img19,
  "ATID070-20": img20
}
const mappedFallbackByName = [
  { keywords: ["bomber", "da", "lon"], image: img1 },
  { keywords: ["bomber", "dang", "lung"], image: img2 },
  { keywords: ["bomber", "gia", "da"], image: img3 },
  { keywords: ["bomber", "cotton"], image: img4 },
  { keywords: ["hoodie", "dang", "hop"], image: img5 },
  { keywords: ["hoodie", "in", "hinh"], image: img6 },
  { keywords: ["hoodie", "keo", "khoa"], image: img7 },
  { keywords: ["coach", "cach", "nhiet"], image: img8 },
  { keywords: ["coach", "da", "asos"], image: img9 },
  { keywords: ["coach", "gia", "da"], image: img10 },
  { keywords: ["coach", "long", "cuu"], image: img11 },
  { keywords: ["astronaut"], image: img12 },
  { keywords: ["embroidered", "fuzzy"], image: img13 },
  { keywords: ["windbreaker"], image: img14 },
  { keywords: ["leopard"], image: img15 },
  { keywords: ["longsleeve"], image: img16 },
  { keywords: ["tiger", "stripe"], image: img17 },
  { keywords: ["camo"], image: img18 },
  { keywords: ["zip", "boxy"], image: img19 },
  { keywords: ["zip", "silk"], image: img20 }
]

const staticColorImageMap = {
  1: [img1],
  2: [img2, img3],
  4: [img4],
  5: [img5],
  6: [img6],
  7: [img7],
  8: [img8],
  9: [img9, img10],
  11: [img11],
  12: [img12, img12b],
  13: [img13, img13b, img13c, img13d, img13e],
  14: [img14, img14b, img14c],
  15: [img15],
  16: [img16, img16b, img16c],
  17: [img17],
  18: [img18, img18b],
  19: [img19, img19b],
  20: [img20, img20b, img20c]
}

const staticColorImageRulesByName = [
  { keywords: ["bomber", "da", "lon"], images: [img1] },
  { keywords: ["bomber", "dang", "lung"], images: [img2, img3] },
  { keywords: ["bomber", "cotton"], images: [img4] },
  { keywords: ["hoodie", "dang", "hop"], images: [img5] },
  { keywords: ["hoodie", "in", "hinh"], images: [img6] },
  { keywords: ["hoodie", "keo", "khoa"], images: [img7] },
  { keywords: ["coach", "cach", "nhiet"], images: [img8] },
  { keywords: ["coach", "da", "tron"], images: [img9, img10] },
  { keywords: ["coach", "long", "cuu"], images: [img11] },
  { keywords: ["astronaut"], images: [img12, img12b] },
  { keywords: ["embroidered", "fuzzy"], images: [img13, img13b, img13c, img13d, img13e] },
  { keywords: ["windbreaker"], images: [img14, img14b, img14c] },
  { keywords: ["leopard"], images: [img15] },
  { keywords: ["longsleeve"], images: [img16, img16b, img16c] },
  { keywords: ["tiger", "stripe"], images: [img17] },
  { keywords: ["camo"], images: [img18, img18b] },
  { keywords: ["zip", "boxy"], images: [img19, img19b] },
  { keywords: ["zip", "silk"], images: [img20, img20b, img20c] }
]

// ─── Helpers ─────────────────────────────────────────────────────────────────

const toList = (value) => {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.content)) return value.content
  if (Array.isArray(value?.items)) return value.items
  if (Array.isArray(value?.data)) return value.data
  if (Array.isArray(value?.data?.content)) return value.data.content
  if (Array.isArray(value?.data?.items)) return value.data.items
  if (Array.isArray(value?.result)) return value.result
  if (Array.isArray(value?.result?.content)) return value.result.content
  return []
}

const normalizeStatusText = (value = "") =>
  String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()

function isEntityActive(entity = {}, defaultValue = true) {
  const rawStatus = entity?.trangThai ?? entity?.status ?? entity?.trangThaiText
  if (typeof rawStatus === "boolean") return rawStatus
  if (typeof rawStatus === "number") return rawStatus !== 0

  const normalizedStatus = normalizeStatusText(rawStatus)
  if (normalizedStatus) {
    if (normalizedStatus.includes("ngung") || normalizedStatus.includes("inactive") || normalizedStatus.includes("disable")) {
      return false
    }
    if (normalizedStatus.includes("hoat dong") || normalizedStatus.includes("active") || normalizedStatus.includes("enable")) {
      return true
    }
  }

  if (typeof entity?.active === "boolean") return entity.active
  if (typeof entity?.isActive === "boolean") return entity.isActive
  if (typeof entity?.active === "number") return entity.active !== 0
  if (typeof entity?.isActive === "number") return entity.isActive !== 0

  return defaultValue
}

const shouldCountOrderForStock = (detail = {}) => {
  const order = detail?.hoaDon || detail || {}
  const statusCode = String(order?.orderStatusCode || detail?.orderStatusCode || "").trim().toUpperCase()
  const fulfillmentCode = String(order?.fulfillmentStatusCode || detail?.fulfillmentStatusCode || "").trim().toUpperCase()
  const statusText = normalizeStatusText(order?.orderStatusName || order?.trangThai || order?.status || "")
  const noteText = normalizeStatusText(order?.statusNote || detail?.statusNote || "")

  if (statusCode.includes("HUY") || statusText.includes("huy") || noteText.includes("huy") || statusText.includes("cancel")) {
    return false
  }

  const isFinalOrder = detail?.finalOrder === true || String(order?.businessClosureStatus || "").toUpperCase() === "CLOSED"
  return isFinalOrder || statusCode === "HOAN_THANH" || fulfillmentCode === "DELIVERED" || statusText.includes("hoan thanh")
}

async function loadSoldQtyBySpct() {
  try {
    const hoaDonRes = await getAllHoaDon()
    const hoaDons = toList(hoaDonRes?.data)
    const orderIds = hoaDons
      .map((order) => Number(order?.id))
      .filter((orderId) => Number.isFinite(orderId) && orderId > 0)

    if (!orderIds.length) return new Map()

    const detailResponses = await Promise.all(
      orderIds.map((orderId) => getHoaDonById(orderId).catch(() => null))
    )

    const soldBySpct = new Map()

    for (const detailRes of detailResponses) {
      const detail = detailRes?.data
      if (!detail || !shouldCountOrderForStock(detail)) continue

      const items = toList(detail?.items || detail?.hoaDonChiTiets || detail?.chiTietHoaDons || detail?.chiTiets)
      for (const item of items) {
        const spctId = Number(item?.spctId || item?.sanPhamChiTietId || item?.idSanPhamChiTiet || item?.chiTietSanPhamId || 0)
        const qty = Number(item?.soLuong || item?.quantity || item?.soLuongMua || 0)
        if (!Number.isFinite(spctId) || spctId <= 0 || !Number.isFinite(qty) || qty <= 0) continue
        soldBySpct.set(spctId, Number(soldBySpct.get(spctId) || 0) + qty)
      }
    }

    return soldBySpct
  } catch {
    return new Map()
  }
}

const toVariantList = (product) => {
  if (Array.isArray(product?.sanPhamChiTiets)) return product.sanPhamChiTiets
  if (Array.isArray(product?.variants)) return product.variants
  if (Array.isArray(product?.chiTiets)) return product.chiTiets
  if (Array.isArray(product?.danhSachBienThe)) return product.danhSachBienThe
  return []
}

function stripTrailingBrandToken(value = "") {
  return String(value || "")
    .trim()
    .replace(/[\s_-]*(dirty\s*wave|dirtywave)$/i, "")
    .trim()
}

function normalizeProductFamilyKey(name = "") {
  return stripTrailingBrandToken(name)
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\b(core|flame)\b/g, " ")
    .replace(/\b(xanh navy|xanh duong|xanh la)\b$/g, " ")
    .replace(/\b(den|do|xanh|xam|trang|nau|be|cam|vang|black|red|blue|navy|green|gray|grey|white|brown|beige|orange|yellow)\b$/g, " ")
    .replace(/\s+/g, " ")
    .trim()
}

function hasCoreOrFlameToken(name = "") {
  return /\b(core|flame)\b/i.test(String(name || ""))
}

function variantStockValue(variant) {
  return Number(variant?.soLuong ?? variant?.soLuongTon ?? variant?.tonKho ?? variant?.ton ?? 0)
}

function variantAvailableStockForPos(variant, soldBySpct = new Map()) {
  const baseStock = variantStockValue(variant)
  const spctId = Number(variant?.id || variant?.spctId || variant?.sanPhamChiTietId || 0)
  if (!Number.isFinite(spctId) || spctId <= 0) return baseStock
  const soldQty = Number(soldBySpct.get(spctId) || 0)
  return Math.max(0, baseStock - soldQty)
}

function variantIdentityKeyForPos(variant) {
  const colorKey = normalizeColorKey(
    variant?.mauSac?.tenMau ||
    variant?.mauSac?.tenMauSac ||
    variant?.mauSac?.name ||
    ""
  )
  const sizeKey = normalizeColorKey(
    variant?.kichThuoc?.tenKichThuoc ||
    variant?.kichThuoc?.name ||
    variant?.tenKichThuoc ||
    variant?.size ||
    ""
  )
  const priceKey = Number(variant?.giaBan ?? variant?.gia ?? 0)

  if (colorKey || sizeKey || priceKey > 0) {
    return `c:${colorKey}|s:${sizeKey}|p:${priceKey}`
  }

  const codeKey = String(variant?.ma || variant?.maSanPhamChiTiet || "").trim().toUpperCase()
  if (codeKey) return `code:${codeKey}`

  const idKey = Number(variant?.id || variant?.spctId || variant?.sanPhamChiTietId || 0)
  if (Number.isFinite(idKey) && idKey > 0) return `id:${idKey}`

  return ""
}

function mergeProductsForPos(products = []) {
  const grouped = new Map()

  for (const product of (Array.isArray(products) ? products : [])) {
    const familyKey = normalizeProductFamilyKey(product?.tenSanPham || "") || `id:${Number(product?.id || 0)}`
    if (!grouped.has(familyKey)) grouped.set(familyKey, [])
    grouped.get(familyKey).push(product)
  }

  const merged = []

  for (const familyProducts of grouped.values()) {
    const ranked = [...familyProducts].sort((left, right) => {
      const leftPenalty = hasCoreOrFlameToken(left?.tenSanPham) ? 1 : 0
      const rightPenalty = hasCoreOrFlameToken(right?.tenSanPham) ? 1 : 0
      if (leftPenalty !== rightPenalty) return leftPenalty - rightPenalty

      const leftVariantCount = toVariantList(left).length
      const rightVariantCount = toVariantList(right).length
      if (leftVariantCount !== rightVariantCount) return rightVariantCount - leftVariantCount

      const leftId = Number(left?.id)
      const rightId = Number(right?.id)
      const normalizedLeftId = Number.isFinite(leftId) && leftId > 0 ? leftId : Number.MAX_SAFE_INTEGER
      const normalizedRightId = Number.isFinite(rightId) && rightId > 0 ? rightId : Number.MAX_SAFE_INTEGER
      return normalizedLeftId - normalizedRightId
    })

    const canonicalProduct = ranked[0] || familyProducts[0]
    const variantByIdentity = new Map()

    for (const product of ranked) {
      for (const variant of toVariantList(product)) {
        const identityKey = variantIdentityKeyForPos(variant) || `id:${Number(variant?.id || 0)}`
        if (!variantByIdentity.has(identityKey)) {
          variantByIdentity.set(identityKey, variant)
        }
      }
    }

    merged.push({
      ...canonicalProduct,
      sanPhamChiTiets: [...variantByIdentity.values()]
    })
  }

  return merged
}

const formatCurrency = (value) =>
  new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + "₫"

function isImageString(value = "") {
  const raw = String(value || "").trim()
  if (!raw) return false
  if (/^data:image\//i.test(raw)) return true
  const normalized = raw.replace(/\\/g, "/").split(/[?#]/)[0]
  if (/\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i.test(normalized)) return true
  if (normalized.startsWith("/uploads/") || normalized.startsWith("uploads/")) return true
  return /^https?:\/\//i.test(raw)
}

function toImageUrl(value = "") {
  const raw = String(value || "").trim()
  if (!raw) return ""
  if (/^data:image\//i.test(raw)) return raw
  const normalized = raw.replace(/\\/g, "/")
  const uploadsMatch = normalized.match(/^.*?\/?(uploads\/.*)$/i)
  if (uploadsMatch?.[1]) return `${BACKEND_ORIGIN}/${uploadsMatch[1]}`
  if (/^https?:\/\//i.test(normalized)) return normalized
  if (normalized.startsWith("/")) return normalized
  if (normalized.startsWith("uploads/")) return `${BACKEND_ORIGIN}/${normalized}`
  return normalized.includes("/") ? `/${normalized.replace(/^\/+/, "")}` : normalized
}

function normalizeProductKey(value = "") {
  return String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
}

function stripTrailingBrandTokenForImage(value = "") {
  return String(value || "")
    .trim()
    .replace(/[\s_-]*(dirty\s*wave|dirtywave)$/i, "")
    .trim()
}

function normalizeSearchText(value = "") {
  return normalizeProductKey(value)
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
}

function getMappedFallbackByName(name = "") {
  const normalized = normalizeProductKey(name)
  if (!normalized) return ""

  const found = mappedFallbackByName.find((rule) =>
    rule.keywords.every((keyword) => normalized.includes(keyword))
  )
  return found?.image || ""
}

function fallbackIndexByName(name = "") {
  const normalized = normalizeSearchText(stripTrailingBrandTokenForImage(name))
  if (!normalized) return -1

  let hash = 0
  for (let i = 0; i < normalized.length; i += 1) {
    hash = ((hash << 5) - hash + normalized.charCodeAt(i)) | 0
  }
  return Math.abs(hash) % fallbackImages.length
}

function fallbackImageFor(id, code = "", name = "") {
  const normalizedCode = String(code || "").trim().toUpperCase()
  const allowCuratedCodeMap = /^ATID070-\d+$/i.test(normalizedCode) || /^SP\d+$/i.test(normalizedCode)

  if (mappedFallbackByCode[normalizedCode]) {
    return mappedFallbackByCode[normalizedCode]
  }

  const mappedByName = getMappedFallbackByName(name)
  if (mappedByName) return mappedByName

  if (allowCuratedCodeMap) {
    const codeDigits = String(normalizedCode).replace(/\D+/g, "")
    const codeNum = Number(codeDigits)
    if (Number.isFinite(codeNum) && codeNum > 0 && mappedFallbackByCodeNum[codeNum]) {
      return mappedFallbackByCodeNum[codeNum]
    }
  }

  const normalizedId = Number(id)
  if (Number.isFinite(normalizedId) && normalizedId > 0) {
    if (allowCuratedCodeMap && mappedFallbackByCodeNum[normalizedId]) return mappedFallbackByCodeNum[normalizedId]
    const nameIndex = fallbackIndexByName(name)
    if (nameIndex >= 0) return fallbackImages[nameIndex] || logoFallback
    return fallbackImages[(normalizedId - 1) % fallbackImages.length]
  }

  const digits = String(code || "").replace(/\D+/g, "")
  const codeNum = Number(digits)
  if (Number.isFinite(codeNum) && codeNum > 0) {
    if (allowCuratedCodeMap && mappedFallbackByCodeNum[codeNum]) return mappedFallbackByCodeNum[codeNum]
    const nameIndex = fallbackIndexByName(name)
    if (nameIndex >= 0) return fallbackImages[nameIndex] || logoFallback
    return fallbackImages[(codeNum - 1) % fallbackImages.length]
  }

  const nameIndex = fallbackIndexByName(name)
  if (nameIndex >= 0) return fallbackImages[nameIndex] || logoFallback

  return fallbackImages[0] || logoFallback
}

function extractProductCodeNumber(value = "") {
  const raw = String(value || "").trim().toUpperCase()
  if (!raw) return 0

  const spMatch = raw.match(/^SP0*(\d+)$/i)
  if (spMatch) return Number(spMatch[1] || 0)

  const suffixMatch = raw.match(/-(\d+)$/)
  if (suffixMatch) return Number(suffixMatch[1] || 0)

  const trailingDigits = raw.match(/(\d+)$/)
  if (trailingDigits) return Number(trailingDigits[1] || 0)

  return 0
}

function normalizeColorKey(value = "") {
  const normalized = String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .trim()

  if (normalized === "xanh navy" || normalized === "navy") return "xanh duong"
  return normalized
}

function normalizeDisplayColorName(value = "") {
  const raw = String(value || "").trim()
  const normalized = normalizeColorKey(raw)
  if (!normalized) return ""
  if (normalized === "xanh duong") return "Xanh dương"
  return raw
}

function colorSearchTokens(colorName = "") {
  const key = normalizeColorKey(colorName)
  if (!key) return []

  const tokenMap = {
    "den": ["black"],
    "do": ["red"],
    "xam": ["gray", "grey"],
    "ghi": ["gray", "grey"],
    "trang": ["white"],
    "nau": ["brown"],
    "be": ["beige"],
    "cam": ["orange"],
    "vang": ["yellow"],
    "xanh duong": ["blue"],
    "xanh la": ["green"],
    "xanh navy": ["navy", "blue"],
    "xanh": ["blue", "green"]
  }

  const mapped = tokenMap[key] || []
  return [...new Set(mapped)]
}

function escapeRegExp(value = "") {
  return String(value || "").replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
}

function imageHasColorToken(image = "", token = "") {
  const source = String(image || "").toLowerCase().replace(/\\/g, "/").split(/[?#]/)[0]
  const fileName = source.split("/").pop() || source
  const escaped = escapeRegExp(String(token || "").toLowerCase())
  if (!escaped) return false
  const pattern = new RegExp(`(?:^|[^a-z0-9])${escaped}(?:[^a-z0-9]|$)`)
  return pattern.test(fileName)
}

function pickImageFromListByColor(images = [], colorName = "") {
  const list = (Array.isArray(images) ? images : [])
    .map((img) => String(img || "").trim())
    .filter(Boolean)
  if (!list.length) return ""

  const tokens = colorSearchTokens(colorName)
  if (!tokens.length) return ""

  const found = list.find((img) => tokens.some((token) => imageHasColorToken(img, token)))
  return found || ""
}

function getStaticColorCandidates(productId, productLike = {}) {
  const normalizedName = normalizeSearchText(productLike?.tenSanPham || "")
  if (normalizedName) {
    const byName = staticColorImageRulesByName.find((rule) =>
      rule.keywords.every((keyword) => normalizedName.includes(keyword))
    )
    if (Array.isArray(byName?.images) && byName.images.length) return byName.images
  }

  const byProductId = staticColorImageMap[Number(productId)]
  if (Array.isArray(byProductId) && byProductId.length) return byProductId

  const codeNumber = extractProductCodeNumber(productLike?.maSanPham || productLike?.ma || productLike?.sku)
  const byCode = staticColorImageMap[Number(codeNumber)]
  if (Array.isArray(byCode) && byCode.length) return byCode

  return []
}

const staticColorImageByColor = {
  2:  { do: img2, red: img2, "xanh duong": img3, xanh: img3, blue: img3 },
  9:  { kem: img9, cream: img9, nau: img10, brown: img10 },
  12: { den: img12, black: img12, "xanh duong": img12b, xanh: img12b, blue: img12b },
  13: { den: img13, black: img13, "xanh la": img13b, xanh: img13b, green: img13b, nau: img13c, brown: img13c, do: img13d, red: img13d, trang: img13e, white: img13e },
  14: { den: img14, black: img14, "xanh duong": img14b, xanh: img14b, blue: img14b, "xanh la": img14c, green: img14c },
  16: { den: img16, black: img16, do: img16b, red: img16b, trang: img16c, white: img16c },
  18: { den: img18, black: img18, trang: img18b, white: img18b },
  19: { "xanh duong": img19, xanh: img19, blue: img19, trang: img19b, white: img19b },
  20: { den: img20, black: img20, xam: img20b, ghi: img20b, gray: img20b, grey: img20b, do: img20c, red: img20c },
}

function pickStaticColorImage(productId, colorName = "", productLike = {}) {
  const colorKey = normalizeColorKey(colorName)
  const pId = Number(productId)
  if (colorKey && staticColorImageByColor[pId]?.[colorKey]) return staticColorImageByColor[pId][colorKey]
  const codeNum = extractProductCodeNumber(productLike?.maSanPham || productLike?.ma || productLike?.sku || "")
  if (colorKey && staticColorImageByColor[codeNum]?.[colorKey]) return staticColorImageByColor[codeNum][colorKey]
  const candidates = getStaticColorCandidates(productId, productLike)
  if (!candidates.length) return ""
  if (candidates.length === 1) return candidates[0]
  return pickImageFromListByColor(candidates, colorName) || candidates[0]
}

function pickImageValue(entry) {
  if (!entry) return ""
  if (typeof entry === "string") return isImageString(entry) ? toImageUrl(entry) : ""
  if (Array.isArray(entry)) {
    for (const child of entry) {
      const found = pickImageValue(child)
      if (found) return found
    }
    return ""
  }
  if (typeof entry === "object") {
    for (const key of ["anh", "hinhAnh", "image", "imageUrl", "images", "listAnh", "anhChinh", "duongDanAnh", "src", "thumbnail", "url", "urlAnh", "anhMau", "hinhAnhMau", "duongDan", "imagePath", "filePath", "path", "anhDaiDien"]) {
      const found = pickImageValue(entry[key])
      if (found) return found
    }
  }
  return ""
}

function productShouldHideColorsByName(name = "") {
  const n = String(name || "")
    .trim()
    .replace(/[\s_-]*(dirty\s*wave|dirtywave)$/i, "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()

  if (!n) return false

  const forceNoColorFamilies = [
    "hoodie keo khoa",
    "hoodie keo khoa in hinh",
    "hoodie in hinh"
  ]

  return forceNoColorFamilies.some((keyword) => n.includes(keyword))
}

function buildVariantLabel(product, variant, options = {}) {
  const includeColor = options.includeColor !== false
  const parts = [product?.tenSanPham]
  const sizeName = String(variant?.kichThuoc?.tenKichThuoc || variant?.tenKichThuoc || variant?.size || "").trim()
  const colorName = normalizeDisplayColorName(variant?.mauSac?.tenMauSac || variant?.mauSac?.tenMau)
  if (sizeName) parts.push(`Size ${sizeName}`)
  if (includeColor && colorName) parts.push(colorName)
  return parts.filter(Boolean).join(" • ")
}

const flattenVariants = (products, soldBySpct = new Map()) =>
  products.flatMap((product) => {
    if (!isEntityActive(product, true)) return []

    const imageConfig = getProductImageConfig({ id: product?.id, maSanPham: product?.maSanPham })
    const overrideImage = getProductImageOverride({ id: product?.id, maSanPham: product?.maSanPham })[0] || ""
    const hideColorForProduct = false
    const productCode = String(product?.maSanPham || product?.ma || "").trim().toUpperCase()

    const activeVariants = toVariantList(product).filter((variant) => isEntityActive(variant, true))

    return activeVariants.map((v) => {
      const colorId = hideColorForProduct ? 0 : Number(v?.mauSac?.id || v?.mauSacId || v?.idMauSac || 0)
      const colorImage = (imageConfig?.colorImages || []).find((entry) => Number(entry?.colorId) === colorId)?.image || ""
      const variantDirectImage = pickImageValue(v)
      const productDirectImage = pickImageValue(product)
      const tenMauRaw = normalizeDisplayColorName(v?.mauSac?.tenMau || v?.mauSac?.tenMauSac)
      const tenMau = hideColorForProduct ? "" : tenMauRaw
      const tenSize = String(v?.kichThuoc?.tenKichThuoc || v?.tenKichThuoc || v?.size || "").trim()
      const soLuongTon = variantAvailableStockForPos(v, soldBySpct)
      const staticColorImage = hideColorForProduct ? "" : pickStaticColorImage(Number(product?.id || 0), tenMauRaw, product)
      const hasStaticPalette = hideColorForProduct ? false : getStaticColorCandidates(Number(product?.id || 0), product).length > 1
      const configuredColorImage = String(colorImage || "").trim()
      const configColorResolved =
        configuredColorImage && (!hasStaticPalette || pickImageFromListByColor([configuredColorImage], tenMauRaw))
          ? toImageUrl(configuredColorImage)
          : ""
      const configGalleryImage = String(imageConfig?.images?.[0] || "").trim()
        ? toImageUrl(String(imageConfig?.images?.[0] || "").trim())
        : ""
      const priorityStaticImage = hasStaticPalette ? staticColorImage : ""
      const codeNum = extractProductCodeNumber(productCode)
      const hasCuratedImage = !!(staticColorImageMap[codeNum] || mappedFallbackByCodeNum[codeNum] || mappedFallbackByCode[productCode])
      const forcedCatalogImage =
        /^SP\d+$/i.test(productCode) && hasCuratedImage
          ? (staticColorImage || fallbackImageFor(product?.id, productCode, product?.tenSanPham))
          : ""
      const resolvedImage =
        forcedCatalogImage ||
        priorityStaticImage ||
        variantDirectImage ||
        configColorResolved ||
        staticColorImage ||
        configGalleryImage ||
        productDirectImage ||
        (overrideImage ? toImageUrl(overrideImage) : "") ||
        fallbackImageForVariant({
          id: Number(product?.id || 0),
          maSanPham: productCode,
          tenSanPham: product?.tenSanPham,
          tenMauSac: tenMauRaw,
          maChiTietSanPham: String(v?.ma || v?.maSanPhamChiTiet || ""),
        }) ||
        fallbackImageFor(v?.id || product?.id, v?.ma || product?.maSanPham, product?.tenSanPham)

      return {
        spctId: v.id || v.spctId || v.sanPhamChiTietId,
        maSanPham: product.maSanPham || product.ma || "",
        maSanPhamChiTiet: v.ma || v.maSanPhamChiTiet || "",
        tenSanPham: product.tenSanPham || "Sản phẩm",
        label: buildVariantLabel(product, v, { includeColor: !hideColorForProduct }),
        tenMau,
        tenSize,
        idSanPham: Number(product?.id || 0),
        giaBan: Number(v?.giaBan ?? v?.gia ?? 0),
        soLuongTon,
        image: resolvedImage
      }
    })
  })

const lineQtyByVariant = computed(() => {
  const map = new Map()
  for (const line of lines.value) {
    const key = Number(line?.spctId)
    if (!Number.isFinite(key)) continue
    map.set(key, Number(map.get(key) || 0) + Number(line?.soLuong || 0))
  }
  return map
})

const availableStockForVariant = (variant) => {
  const key = Number(variant?.spctId)
  const total = Number(variant?.soLuongTon || 0)
  const inCart = Number(lineQtyByVariant.value.get(key) || 0)
  return Math.max(total - inCart, 0)
}

const rawStockForVariant = (variant) => Number(variant?.soLuongTon || 0)

// ─── Computed ────────────────────────────────────────────────────────────────

const filteredProductVariants = computed(() => {
  const kw = productSearchKeyword.value.trim().toLowerCase()
  const source = kw
    ? variants.value.filter((v) =>
    [v.maSanPham, v.maSanPhamChiTiet, v.tenSanPham, v.tenMau, v.tenSize, v.label]
      .join(" ").toLowerCase().includes(kw)
    )
    : variants.value

  const seen = new Set()
  return source.filter((v) => {
    const idKey = Number(v?.spctId || 0)
    const codeKey = String(v?.maSanPhamChiTiet || "").trim().toUpperCase()
    const key = idKey > 0 ? `id:${idKey}` : `code:${codeKey}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
})

const subtotal = computed(() =>
  lines.value.reduce((sum, l) => sum + Number(l.giaBan || 0) * Number(l.soLuong || 0), 0)
)
const grandTotal = computed(() => {
  const raw = Math.max(subtotal.value - Number(discount.value || 0) + (taiQuay.value ? 0 : Number(shippingFee.value || 0)), 0)
  return Math.ceil(raw / 1000) * 1000
})

const paymentEntered = computed(() => Number(paymentCash.value || 0) + Number(paymentBank.value || 0))
const paymentRemaining = computed(() => Math.max(grandTotal.value - paymentEntered.value, 0))
const paymentChange = computed(() => Math.max(paymentEntered.value - grandTotal.value, 0))
// Còn lại theo từng cột (KiotViet-style split)
const paymentRemainingCash = computed(() => Math.max(grandTotal.value - Number(paymentBank.value || 0), 0))
const paymentRemainingBank = computed(() => Math.max(grandTotal.value - Number(paymentCash.value || 0), 0))

const resolveApiPaymentMethod = (method) => {
  const normalized = String(method || "").toUpperCase()
  if (normalized === "BOTH") return "CASH_BANK"
  return normalized || "CASH"
}

const selectedCustomerInfo = computed(() =>
  khachHangList.value.find((k) => Number(k.id) === Number(customerId.value)) || null
)

const selectedCustomerLabel = computed(() => {
  return selectedCustomerInfo.value?.tenKhachHang || "Khách lẻ"
})

const selectedCustomerPhone = computed(() =>
  String(selectedCustomerInfo.value?.soDienThoai || "").trim()
)

const defaultStatusCode = computed(() =>
  paymentMethod.value.toUpperCase() === "VNPAY" ? "CHO_LAY_HANG" : "HOAN_THANH"
)

const selectedAddress = computed(() => {
  if (selectedAddressId.value === "manual") return null
  return customerAddresses.value.find((a) => Number(a.id) === Number(selectedAddressId.value)) || null
})

const shippingAddressText = computed(() => {
  if (taiQuay.value) return "Mua tại quầy"
  if (selectedAddress.value) {
    return [selectedAddress.value.diaChiCuThe, selectedAddress.value.phuongXa, selectedAddress.value.quanHuyen, selectedAddress.value.tinhThanh].filter(Boolean).join(", ")
  }
  if (selectedAddressId.value === "manual") {
    return [manualAddress.value.diaChiCuThe, manualAddress.value.phuongXa, manualAddress.value.quanHuyen, manualAddress.value.tinhThanh].filter(Boolean).join(", ")
  }
  return ""
})

const shippingPhone = computed(() => {
  if (taiQuay.value) return ""
  if (selectedAddress.value) return selectedAddress.value.soDienThoai || ""
  if (selectedAddressId.value === "manual") return manualAddress.value.soDienThoai || ""
  return selectedCustomerInfo.value?.soDienThoai || ""
})

// Shipping address parts for GHN quote
const shippingProvince = computed(() => {
  if (taiQuay.value) return ""
  if (selectedAddress.value) return selectedAddress.value.tinhThanh || ""
  if (selectedAddressId.value === "manual") return manualAddress.value.tinhThanh || ""
  return ""
})
const shippingDistrict = computed(() => {
  if (taiQuay.value) return ""
  if (selectedAddress.value) return selectedAddress.value.quanHuyen || ""
  if (selectedAddressId.value === "manual") return manualAddress.value.quanHuyen || ""
  return ""
})
const shippingWard = computed(() => {
  if (taiQuay.value) return ""
  if (selectedAddress.value) return selectedAddress.value.phuongXa || ""
  if (selectedAddressId.value === "manual") return manualAddress.value.phuongXa || ""
  return ""
})

const fetchShippingQuote = async () => {
  if (taiQuay.value) { shippingFee.value = 0; return }
  const toProvince = shippingProvince.value
  const toDistrict = shippingDistrict.value
  if (!toProvince || !toDistrict) { shippingFee.value = 0; return }
  shippingLoading.value = true
  try {
    const totalQty = lines.value.reduce((s, l) => s + Number(l.soLuong || 0), 0)
    const payload = {
      provider: shippingProvider.value,
      toProvince,
      toDistrict,
      toWard: shippingWard.value,
      weightGram: Math.max(500, totalQty * 450),
      lengthCm: 30, widthCm: 20, heightCm: 12
    }
    const response = await quoteShippingFeeAll(payload)
    const quotes = Array.isArray(response?.data?.quotes) ? response.data.quotes : []
    const matched = quotes.find((q) => String(q?.provider || "").toUpperCase() === shippingProvider.value) || quotes[0]
    shippingFee.value = Number(matched?.fee || 0)
  } catch {
    shippingFee.value = 30000
  } finally {
    shippingLoading.value = false
  }
}

// Watch shipping-related changes to re-quote
watch([taiQuay, shippingProvider, shippingProvince, shippingDistrict, shippingWard, () => lines.value.length], () => {
  fetchShippingQuote()
})

// Watch customer ID changes to load addresses
watch(customerId, async (newId) => {
  customerAddresses.value = []
  selectedAddressId.value = null
  if (!newId) return
  try {
    const res = await getDiaChiByKhachHang(newId)
    customerAddresses.value = Array.isArray(res?.data) ? res.data : []
    if (customerAddresses.value.length) selectedAddressId.value = Number(customerAddresses.value[0].id)
  } catch { /* no addresses */ }
})

watch(
  [
    taiQuay,
    paymentMethod,
    orderNote,
    discount,
    selectedVoucher,
    cashierId,
    customerId,
    selectedAddressId,
    shippingProvider,
    manualAddress,
    lines
  ],
  () => {
    persistPosDraft()
  },
  { deep: true }
)

// ─── Cart actions ────────────────────────────────────────────────────────────

function addProductFromModal(variant) {
  const available = availableStockForVariant(variant)
  if (available <= 0) {
    window.toast?.warning?.("Sản phẩm đã hết tồn trong giỏ hiện tại")
    return
  }
  const existed = lines.value.find((l) => Number(l.spctId) === Number(variant.spctId))
  if (existed) {
    existed.soLuong = Number(existed.soLuong || 0) + 1
  } else {
    lines.value.push({ ...variant, soLuong: 1 })
  }
  showProductModal.value = false
  productSearchKeyword.value = ""
}

function incQty(line) {
  if (line.soLuong >= line.soLuongTon) {
    window.toast?.warning?.("Đã đạt tồn kho tối đa")
    return
  }
  line.soLuong++
}

function decQty(line) {
  if (line.soLuong > 1) line.soLuong--
}

function setQty(line, value) {
  const num = Math.floor(Number(value) || 0)
  if (num <= 0) {
    line.soLuong = 1
    window.toast?.warning?.("Số lượng phải lớn hơn 0")
    return
  }
  if (num > line.soLuongTon) {
    line.soLuong = line.soLuongTon
    window.toast?.warning?.("Số lượng không được vượt tồn kho")
    return
  }
  line.soLuong = num
}

function removeLine(index) {
  lines.value.splice(index, 1)
}

const applyDiscount = (amount) => { discount.value = Number(amount || 0) }

const onImgError = (e) => { e.target.src = logoFallback }

function openPaymentModal() {
  if (!lines.value.length) { window.toast?.warning?.("Chưa có sản phẩm trong đơn"); return }
  if (!taiQuay.value && !shippingAddressText.value) { window.toast?.warning?.("Vui lòng chọn địa chỉ giao hàng"); return }
  // Default to CASH if no mode set yet
  if (!['CASH','BANK','BOTH','VNPAY'].includes(paymentMethod.value)) paymentMethod.value = 'CASH'
  // Staff should type amounts manually each time modal opens.
  paymentCash.value = 0
  paymentBank.value = 0
  applyPaymentMode(paymentMethod.value)
  showPaymentModal.value = true
}

function confirmPayment() {
  if (paymentRemaining.value > 0) { window.toast?.warning?.("Số tiền nhập chưa đủ"); return }
  showPaymentModal.value = false
  submitPosOrder()
}

// ─── Quick customer ──────────────────────────────────────────────────────────

const normalizedPhone = (value) => String(value || "").replace(/\s+/g, "")
const canonicalPhone = (value) => {
  const digits = String(value || "").replace(/\D+/g, "")
  if (!digits) return ""
  if (digits.startsWith("84") && digits.length >= 10) return `0${digits.slice(2)}`
  return digits
}
const isValidVietnamPhone = (value) => /^(0|\+84)\d{9,10}$/.test(normalizedPhone(value))

const customerSearchResults = computed(() => {
  const keyword = canonicalPhone(customerPhoneSearch.value)
  const base = khachHangList.value.filter((kh) => {
    const phone = canonicalPhone(kh?.soDienThoai)
    if (!phone) return false
    if (!keyword) return true
    return phone.includes(keyword)
  })
  return base.slice(0, 80)
})

const hasExactPhoneMatch = computed(() => {
  const keyword = canonicalPhone(customerPhoneSearch.value)
  if (!keyword) return false
  return khachHangList.value.some((kh) => canonicalPhone(kh?.soDienThoai) === keyword)
})

const canCreateCustomerFromSearch = computed(() =>
  isValidVietnamPhone(customerPhoneSearch.value) && !hasExactPhoneMatch.value
)

const openCustomerSearchModal = async () => {
  showCustomerSearchModal.value = true
  customerPhoneSearch.value = selectedCustomerPhone.value || ""
  customerDraftName.value = selectedCustomerInfo.value?.tenKhachHang || ""
  await nextTick()
  customerSearchInputRef.value?.focus?.()
}

const closeCustomerSearchModal = () => {
  showCustomerSearchModal.value = false
}

const selectCustomerFromSearch = (customer) => {
  customerId.value = Number(customer?.id) || null
  customerPhoneSearch.value = String(customer?.soDienThoai || "")
  customerDraftName.value = String(customer?.tenKhachHang || "")
  closeCustomerSearchModal()
}

const selectFirstCustomerFromSearch = () => {
  const first = customerSearchResults.value?.[0]
  if (first) selectCustomerFromSearch(first)
}

const chooseGuestCustomer = () => {
  customerId.value = null
  customerPhoneSearch.value = ""
  customerDraftName.value = ""
  closeCustomerSearchModal()
  window.toast?.success?.("Đã chọn khách lẻ")
}

const resolveCustomerIdFromResponse = (response) => {
  const candidates = [response?.data?.id, response?.data?.data?.id, response?.data?.khachHang?.id, response?.data?.content?.id]
  return Number(candidates.find((id) => Number(id) > 0)) || null
}

const resetQuickCustomerForm = () => {
  quickCustomer.value = { tenKhachHang: "", soDienThoai: "", email: "" }
}

const quickCreateCustomer = async (options = {}) => {
  const name = String(quickCustomer.value.tenKhachHang || "").trim()
  const phone = normalizedPhone(quickCustomer.value.soDienThoai)
  const email = String(quickCustomer.value.email || "").trim()
  if (!name) { window.toast?.warning?.("Vui lòng nhập tên khách hàng"); return }
  if (!isValidVietnamPhone(phone)) { window.toast?.warning?.("Số điện thoại không hợp lệ"); return }

  creatingCustomer.value = true
  try {
    const payloadCandidates = [
      { tenKhachHang: name, soDienThoai: phone, email, trangThai: "Hoạt động" },
      { tenKhachHang: name, soDienThoai: phone, taiKhoanEmail: email, trangThai: "Hoạt động" },
      { tenKhachHang: name, soDienThoai: phone, trangThai: "Hoạt động" }
    ]
    let createdId = null
    let lastError = null
    for (const payload of payloadCandidates) {
      try {
        const createRes = await createKhachHang(payload)
        createdId = resolveCustomerIdFromResponse(createRes)
        if (createdId) break
      } catch (error) { lastError = error }
    }
    const khRes = await getAllKhachHang(0, 200)
    khachHangList.value = toList(khRes?.data)
    if (!createdId) {
      const found = khachHangList.value.find((kh) => normalizedPhone(kh?.soDienThoai) === phone && String(kh?.tenKhachHang || "").trim() === name)
      createdId = found?.id ? Number(found.id) : null
    }
    if (!createdId) throw lastError || new Error("Không lấy được khách hàng vừa tạo")
    customerId.value = createdId
    customerDraftName.value = name
    customerPhoneSearch.value = phone
    if (options.closeCustomerModal) closeCustomerSearchModal()
    resetQuickCustomerForm()
    window.toast?.success?.("Tạo nhanh khách hàng thành công")
  } catch (error) {
    window.toast?.error?.(error?.response?.data?.message || error?.message || "Không thể tạo nhanh khách hàng")
  } finally {
    creatingCustomer.value = false
  }
}

const createCustomerFromSearch = async () => {
  quickCustomer.value = {
    tenKhachHang: customerDraftName.value,
    soDienThoai: customerPhoneSearch.value,
    email: ""
  }
  await quickCreateCustomer({ closeCustomerModal: true })
}

// ─── Employee context ────────────────────────────────────────────────────────

const resolveCurrentSellerContext = async () => {
  // Try to get nhanVien linked to the current taiKhoan
  try {
    const parsed = JSON.parse(localStorage.getItem("user") || sessionStorage.getItem("user") || "null")
    if (parsed?.idNhanVien) {
      const nv = nhanVienList.value.find((n) => Number(n.id) === Number(parsed.idNhanVien))
      if (nv) { currentSellerName.value = nv.tenNhanVien || ""; return Number(nv.id) }
    }
    if (parsed?.id && parsed?.tenNhanVien) {
      currentSellerName.value = parsed.tenNhanVien
      return Number(parsed.id)
    }
  } catch { /* fallback */ }

  const taiKhoanId = Number(localStorage.getItem("userId") || 0)
  if (taiKhoanId > 0) {
    // Try API lookup
    try {
      const res = await getNhanVienByTaiKhoanId(taiKhoanId)
      const first = Array.isArray(res?.data) ? res.data[0] : res?.data
      if (first?.id) {
        currentSellerName.value = first.tenNhanVien || ""
        return Number(first.id)
      }
    } catch { /* fallback */ }
    // Try matching from loaded NV list
    const mapped = nhanVienList.value.find((nv) => Number(nv?.idTaiKhoan || nv?.taiKhoan?.id || 0) === taiKhoanId)
    if (mapped?.id) {
      currentSellerName.value = mapped.tenNhanVien || ""
      return Number(mapped.id)
    }
  }

  // For admin: use stored name from login
  const storedName = localStorage.getItem("userName") || ""
  if (storedName) currentSellerName.value = storedName

  return null
}

// ─── Submit POS order ────────────────────────────────────────────────────────

const submitPosOrder = async () => {
  if (!cashierId.value) { window.toast?.warning?.("Vui lòng chọn nhân viên bán hàng"); return }
  if (!lines.value.length) { window.toast?.warning?.("Chưa có sản phẩm trong đơn"); return }
  if (!taiQuay.value && !shippingAddressText.value) { window.toast?.warning?.("Vui lòng chọn địa chỉ giao hàng"); return }

  const invalidQtyLines = lines.value.filter((l) => Number(l.soLuong || 0) <= 0)
  if (invalidQtyLines.length) {
    window.toast?.error?.("Số lượng sản phẩm phải lớn hơn 0")
    return
  }

  saving.value = true
  try {
    const selectedCustomer = khachHangList.value.find((kh) => Number(kh.id) === Number(customerId.value)) ?? null
    const orderType = taiQuay.value ? "POS" : "DELIVERY"
    const addressText = shippingAddressText.value || "Mua tại quầy"
    const phone = shippingPhone.value || selectedCustomer?.soDienThoai || ""

    const shipCost = taiQuay.value ? 0 : Number(shippingFee.value || 0)

    const createPayload = {
      nhanVienId: Number(cashierId.value),
      soDienThoaiNhanHang: phone,
      diaChiNhanHang: addressText,
      phiShip: shipCost,
      phuongThucThanhToan: resolveApiPaymentMethod(paymentMethod.value),
      orderType
    }
    if (selectedCustomer?.id) createPayload.khachHangId = Number(selectedCustomer.id)

    const createRes = await createHoaDon(createPayload)
    const orderId = createRes?.data?.hoaDon?.id ?? createRes?.data?.id
    if (!orderId) throw new Error("Không lấy được mã hóa đơn")

    for (const line of lines.value) {
      await addHoaDonItem(orderId, { spctId: line.spctId, soLuong: Number(line.soLuong), giaBan: Number(line.giaBan) })
    }

    const isVnpay = paymentMethod.value.toUpperCase() === "VNPAY"
    const apiPayMethod = resolveApiPaymentMethod(paymentMethod.value)

    try {
      const updatePayload = {
        nhanVienId: Number(cashierId.value),
        soDienThoaiNhanHang: phone,
        diaChiNhanHang: addressText,
        phiShip: shipCost,
        giaSauGiamGia: Number(discount.value || 0),
        thanhTien: Number(grandTotal.value || 0),
        phuongThucThanhToan: apiPayMethod,
        orderType,
        statusNote: isVnpay
          ? appendPaymentFlowTag(`[${orderType}] ${orderNote.value || "Đơn bán hàng"}`, PAYMENT_FLOW_TAGS.VN_PAY_EMPLOYEE_CONFIRMED, "NV xác nhận VNPay")
          : `[${orderType}] ${orderNote.value || "Đơn bán hàng"}`
      }
      if (selectedCustomer?.id) updatePayload.khachHangId = Number(selectedCustomer.id)
      await updateHoaDon(orderId, updatePayload)
    } catch (updateErr) {
      console.warn("updateHoaDon failed, retrying without extra fields:", updateErr)
      try {
        await updateHoaDon(orderId, {
          nhanVienId: Number(cashierId.value),
          phuongThucThanhToan: apiPayMethod,
          statusNote: `[${orderType}] ${orderNote.value || "Đơn bán hàng"}`
        })
      } catch { /* order was already created, continue */ }
    }

    if (taiQuay.value && !isVnpay) {
      try {
        const trackingUrl = buildOrderLookupTrackingUrl({ maHoaDon: createRes?.data?.hoaDon?.maHoaDon || createRes?.data?.maHoaDon })
        await updateHoaDonBySystemEvent(orderId, "HOAN_TAT_POS", "Hoàn tất bán hàng tại quầy", trackingUrl)
        window.toast?.success?.("Tạo đơn thành công")
      } catch {
        window.toast?.warning?.("Đơn đã tạo nhưng chưa hoàn tất")
      }
    } else if (!taiQuay.value) {
      window.toast?.success?.("Tạo đơn giao hàng thành công")
    } else {
      window.toast?.success?.("Tạo đơn thành công — chờ khách xác nhận VNPay")
    }

    // Remove current tab after successful order
    if (posTabs.value.length > 1) {
      posTabs.value.splice(activeTabIndex.value, 1)
      if (activeTabIndex.value >= posTabs.value.length) activeTabIndex.value = posTabs.value.length - 1
    } else {
      posTabs.value = [null]
      activeTabIndex.value = 0
    }
    persistPosDraft()
    router.push(`${panelBasePath.value}/hoa-don/detail/${orderId}`)
  } catch (error) {
    window.toast?.error?.(error?.response?.data?.message || error.message || "Không thể tạo đơn")
  } finally {
    saving.value = false
  }
}

// ─── Load data ───────────────────────────────────────────────────────────────

const loadData = async () => {
  loading.value = true
  try {
    const [nvRes, khRes, spRes, soldBySpct] = await Promise.all([
      getAllNhanVien(),
      getAllKhachHang(0, 200),
      getAllSanPham(),
      loadSoldQtyBySpct()
    ])
    nhanVienList.value = toList(nvRes?.data)
    khachHangList.value = toList(khRes?.data)
    const rawProducts = toList(spRes?.data)
    const mergedProducts = mergeProductsForPos(rawProducts)
    variants.value = flattenVariants(mergedProducts, soldBySpct)

    // Apply active campaign discounts to giaBan
    try {
      const cMap = await getActiveCampaignMap()
      if (cMap.size > 0) {
        variants.value = variants.value.map((v) => {
          const camp = v.idSanPham ? cMap.get(v.idSanPham) : null
          if (!camp) return v
          const pct = Math.max(0, Math.min(100, camp.giaTri))
          if (pct <= 0) return v
          const base = v.giaBan
          return { ...v, giaBan: Math.round(base * (1 - pct / 100)), giaBanGoc: base, campaignPercent: pct, campaignName: camp.tenKhuyenMai }
        })
      }
    } catch { /* Node backend offline — use shelf price */ }

    restorePosDraft()

    // Resolve current seller for both admin and employee
    currentEmployeeId.value = await resolveCurrentSellerContext()
    if (currentEmployeeId.value) {
      cashierId.value = Number(currentEmployeeId.value)
    } else if (!cashierId.value && nhanVienList.value.length) {
      cashierId.value = Number(nhanVienList.value[0].id)
      const nv = nhanVienList.value[0]
      currentSellerName.value = nv?.tenNhanVien || ""
    }
  } catch (error) {
    window.toast?.error?.(error?.message || "Không thể tải dữ liệu bán hàng")
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
onBeforeUnmount(() => {
  persistPosDraft()
})
</script>

<template>
  <main class="bh-page">
    <!-- Header -->
    <header class="bh-header">
      <div>
        <h1 class="bh-title">Bán hàng</h1>
        <p class="bh-subtitle">Người bán: <strong>{{ currentSellerName || nhanVienList.find(nv => Number(nv.id) === Number(cashierId))?.tenNhanVien || '—' }}</strong></p>
      </div>
      <div class="bh-header-actions">
        <div class="bh-dropdown-wrap">
          <button class="bh-btn bh-btn-primary" type="button" @click="showHeaderMenu = !showHeaderMenu">
            <span>+ Tạo đơn hàng</span>
          </button>
          <div v-if="showHeaderMenu" class="bh-dropdown-menu" @click="showHeaderMenu = false">
            <button type="button" @click="router.push(`${panelBasePath}/hoa-don/list`)">Quản lý đơn hàng</button>
            <button type="button" @click="router.push(`${panelBasePath}/lich-lam-viec/ca-lam`)">Ca làm việc</button>
          </div>
        </div>
      </div>
    </header>

    <!-- POS Tabs (hoá đơn chờ) -->
    <div class="bh-tabs-bar">
      <button
        v-for="(tab, idx) in posTabs" :key="idx"
        class="bh-tab-btn" :class="{ active: idx === activeTabIndex }"
        @click="switchPosTab(idx)"
      >
        Hoá đơn {{ idx + 1 }}
        <span v-if="posTabs.length > 1" class="bh-tab-close" @click.stop="removePosTab(idx)">&times;</span>
      </button>
      <button v-if="posTabs.length < MAX_POS_TABS" class="bh-tab-btn bh-tab-add" @click="addPosTab()">+</button>
    </div>

    <div v-if="loading" class="bh-loading">Đang tải dữ liệu bán hàng...</div>

    <template v-else>
      <div class="bh-layout">
        <!-- ─── Left: Sản phẩm ─── -->
        <div class="bh-main">
          <div class="bh-card">
            <!-- Products header -->
            <div class="bh-card-head">
              <h2>Sản phẩm</h2>
              <div class="bh-card-actions">
                <button class="bh-btn bh-btn-primary" type="button" @click="showProductModal = true">
                  <Plus :size="15" /><span>THÊM SẢN PHẨM</span>
                </button>
              </div>
            </div>

            <!-- Cart items -->
            <div class="bh-cart">
              <div v-if="!lines.length" class="bh-cart-empty">
                <ShoppingBag :size="36" />
                <p>Chưa có sản phẩm — bấm "Thêm sản phẩm" để bắt đầu</p>
              </div>

              <div v-for="(line, idx) in lines" :key="line.spctId" class="bh-cart-item">
                <!-- Thumbnail -->
                <div class="bh-cart-thumb">
                  <img :src="line.image || logoFallback" :alt="line.tenSanPham" @error="onImgError" />
                </div>

                <!-- Info -->
                <div class="bh-cart-info">
                  <div class="bh-cart-name">{{ line.tenSanPham }}</div>
                  <div class="bh-cart-meta">
                    <span class="bh-cart-code">{{ line.maSanPhamChiTiet }}</span>
                    <span v-if="line.tenMau" class="bh-dot">·</span>
                    <span v-if="line.tenMau">{{ line.tenMau }}</span>
                    <span v-if="line.tenSize" class="bh-dot">·</span>
                    <span v-if="line.tenSize">{{ line.tenSize }}</span>
                  </div>
                </div>

                <!-- Price -->
                <div class="bh-cart-price">
                  {{ formatCurrency(line.giaBan) }}
                  <span v-if="line.campaignPercent" class="bh-cart-campaign-badge">-{{ line.campaignPercent }}%</span>
                </div>

                <!-- Qty controls -->
                <div class="bh-cart-qty">
                  <button class="bh-qty-btn" type="button" @click="decQty(line)" :disabled="line.soLuong <= 1">
                    <Minus :size="14" />
                  </button>
                  <input type="number" class="bh-qty-input" :value="line.soLuong" min="1" :max="line.soLuongTon" @change="setQty(line, $event.target.value)" />
                  <button class="bh-qty-btn" type="button" @click="incQty(line)" :disabled="line.soLuong >= line.soLuongTon">
                    <Plus :size="14" />
                  </button>
                </div>

                <!-- Delete -->
                <button class="bh-cart-delete" type="button" @click="removeLine(idx)">
                  <Trash2 :size="16" />
                </button>
              </div>

              <!-- Cart total -->
              <div v-if="lines.length" class="bh-cart-total">
                <span>Tổng tiền</span>
                <strong>{{ formatCurrency(subtotal) }}</strong>
              </div>
            </div>
          </div>
        </div>

        <!-- ─── Right: Customer + Payment ─── -->
        <div class="bh-sidebar">
          <!-- Customer panel -->
          <div class="bh-card">
            <div class="bh-card-head">
              <h2>Thông tin khách hàng</h2>
              <div class="bh-card-actions">
                <button class="bh-btn bh-btn-outline" type="button" @click="openCustomerSearchModal">
                  Tìm khách hàng
                </button>
              </div>
            </div>
            <div class="bh-card-body">
              <div class="bh-kv">
                <span class="bh-k">Tên khách hàng</span>
                <span class="bh-v">{{ selectedCustomerLabel }}</span>
              </div>

              <div class="bh-kv">
                <span class="bh-k">Số điện thoại</span>
                <span class="bh-v">{{ selectedCustomerPhone || '—' }}</span>
              </div>

              <!-- Address section (for delivery mode) -->
              <template v-if="!taiQuay">
                <div class="bh-address-section">
                  <div class="bh-address-label"><MapPin :size="14" /> Địa chỉ giao hàng</div>
                  <select v-if="customerAddresses.length" class="bh-select" v-model="selectedAddressId">
                    <option v-for="addr in customerAddresses" :key="addr.id" :value="Number(addr.id)">
                      {{ [addr.diaChiCuThe, addr.phuongXa, addr.quanHuyen, addr.tinhThanh].filter(Boolean).join(', ') }}
                    </option>
                    <option value="manual">Nhập địa chỉ mới</option>
                  </select>
                  <div v-if="!customerAddresses.length || selectedAddressId === 'manual'" class="bh-address-form">
                    <input v-model="manualAddress.soDienThoai" type="text" placeholder="Số điện thoại nhận hàng" class="bh-input" />
                    <input v-model="manualAddress.diaChiCuThe" type="text" placeholder="Số nhà, tên đường..." class="bh-input" />
                    <input v-model="manualAddress.phuongXa" type="text" placeholder="Phường/Xã" class="bh-input" />
                    <input v-model="manualAddress.quanHuyen" type="text" placeholder="Quận/Huyện" class="bh-input" />
                    <input v-model="manualAddress.tinhThanh" type="text" placeholder="Tỉnh/Thành phố" class="bh-input" />
                  </div>
                </div>
              </template>


              <!-- Shipping fee (delivery mode) -->
              <div v-if="!taiQuay" class="bh-shipping-fee">
                <div class="bh-shipping-fee-row">
                  <span><Truck :size="14" /> Đơn vị vận chuyển</span>
                  <select class="bh-select bh-select-sm" v-model="shippingProvider">
                    <option value="GHN">GHN (Giao Hàng Nhanh)</option>
                    <option value="GHTK">GHTK (Giao Hàng Tiết Kiệm)</option>
                  </select>
                </div>
                <div v-if="shippingProvince" class="bh-shipping-fee-row" style="margin-top: 8px;">
                  <span>Phí vận chuyển</span>
                  <strong v-if="shippingLoading">Đang tính...</strong>
                  <strong v-else>{{ formatCurrency(shippingFee) }}</strong>
                </div>
              </div>

              <p v-if="taiQuay" class="bh-hint">Tại quầy: chỉ cần chọn sản phẩm và thanh toán.</p>
            </div>
          </div>

          <!-- Payment panel -->
          <div class="bh-card">
            <div class="bh-card-head bh-payment-head">
              <h2>Thông tin thanh toán</h2>
              <label class="bh-toggle">
                <span class="bh-toggle-label">{{ taiQuay ? 'Tại quầy' : 'Giao hàng' }}</span>
                <input type="checkbox" v-model="taiQuay" />
                <span class="bh-toggle-track"></span>
              </label>
            </div>
            <div class="bh-card-body">
              <div class="bh-field">
                <label>Người tạo đơn</label>
                <select class="bh-select" v-model.number="cashierId" :disabled="isEmployeePanel">
                  <option :value="null">Chọn nhân viên</option>
                  <option v-for="nv in nhanVienList" :key="nv.id" :value="Number(nv.id)">
                    {{ nv.tenNhanVien || `NV #${nv.id}` }}
                  </option>
                </select>
              </div>

              <div class="bh-field">
                <label>Giảm giá (voucher)</label>
                <VoucherSelector
                  :subtotal="subtotal"
                  :customer-id="customerId"
                  :auto-select="true"
                  @update:voucher="selectedVoucher = $event"
                  @discount-changed="applyDiscount"
                />
              </div>

              <div class="bh-field">
                <label>Ghi chú</label>
                <input class="bh-input" v-model="orderNote" type="text" placeholder="Ghi chú đơn hàng..." />
              </div>

              <!-- Summary -->
              <div class="bh-summary">
                <div class="bh-pay-row"><span>Tiền hàng</span><strong>{{ formatCurrency(subtotal) }}</strong></div>
                <div class="bh-pay-row bh-pay-discount"><span>Giảm giá</span><strong>- {{ formatCurrency(discount) }}</strong></div>
                <div v-if="!taiQuay" class="bh-pay-row"><span><Truck :size="14" /> Phí ship ({{ shippingProvider }})</span><strong>{{ shippingLoading ? '...' : formatCurrency(shippingFee) }}</strong></div>
                <div class="bh-pay-row bh-pay-total"><span>Tổng số tiền</span><strong>{{ formatCurrency(grandTotal) }}</strong></div>
              </div>

              <button
                class="bh-btn bh-btn-primary bh-btn-submit"
                type="button"
                :disabled="saving || !lines.length"
                @click="openPaymentModal"
              >
                {{ saving ? 'ĐANG TẠO ĐƠN...' : 'XÁC NHẬN ĐẶT HÀNG' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ─── Customer search modal ─── -->
    <Teleport to="body">
      <Transition name="bh-modal">
        <div
          v-if="showCustomerSearchModal"
          class="bh-modal-overlay"
          @click.self="closeCustomerSearchModal"
          @keydown.esc="closeCustomerSearchModal"
        >
          <div class="bh-modal-box bh-customer-modal">
            <div class="bh-modal-head bh-customer-modal-head">
              <div class="bh-customer-head-copy">
                <h2>Tìm khách hàng theo SĐT</h2>
              </div>
              <div class="bh-customer-modal-tools">
                <div class="bh-customer-counter">{{ customerSearchResults.length }} kết quả</div>
                <button class="bh-btn bh-btn-sm bh-customer-guest-btn" type="button" @click="chooseGuestCustomer">
                  Khách lẻ
                </button>
              </div>
              <button class="bh-modal-close" @click="closeCustomerSearchModal"><X :size="20" /></button>
            </div>
            <div class="bh-modal-search">
              <Search :size="18" class="bh-modal-search-icon" />
              <input
                ref="customerSearchInputRef"
                v-model="customerPhoneSearch"
                type="text"
                placeholder="Nhập số điện thoại khách hàng..."
                @keydown.enter.prevent="selectFirstCustomerFromSearch"
              />
            </div>

            <div class="bh-customer-modal-body">
              <div class="bh-customer-table-wrap">
                <table v-if="customerSearchResults.length" class="bh-customer-table">
                  <thead>
                    <tr>
                      <th>Tên khách hàng</th>
                      <th>Số điện thoại</th>
                      <th class="right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="kh in customerSearchResults"
                      :key="kh.id"
                      :class="{ 'is-selected': Number(kh.id) === Number(customerId) }"
                      @dblclick="selectCustomerFromSearch(kh)"
                    >
                      <td>{{ kh.tenKhachHang || `KH #${kh.id}` }}</td>
                      <td>{{ kh.soDienThoai || '—' }}</td>
                      <td class="right">
                        <button class="bh-btn bh-btn-outline bh-btn-sm" type="button" @click="selectCustomerFromSearch(kh)">
                          Chọn
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <div v-else class="bh-modal-empty">Không tìm thấy khách hàng theo số điện thoại</div>
              </div>

              <div v-if="canCreateCustomerFromSearch" class="bh-customer-create">
                <p>Chưa có khách hàng này. Nhập tên để tạo mới:</p>
                <input
                  v-model="customerDraftName"
                  class="bh-input"
                  type="text"
                  placeholder="Tên khách hàng"
                  :disabled="creatingCustomer"
                  @keydown.enter.prevent="createCustomerFromSearch"
                />
                <div class="bh-customer-create-actions">
                  <button class="bh-btn bh-btn-primary bh-btn-sm" type="button" :disabled="creatingCustomer || !customerDraftName.trim()" @click="createCustomerFromSearch">
                    {{ creatingCustomer ? 'Đang tạo...' : 'Tạo mới và chọn' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ─── Product selection modal ─── -->
    <Teleport to="body">
      <Transition name="bh-modal">
        <div v-if="showProductModal" class="bh-modal-overlay" @click.self="showProductModal = false">
          <div class="bh-modal-box bh-product-modal">
            <div class="bh-modal-head">
              <div class="bh-product-modal-head-copy">
                <h2>Thêm sản phẩm</h2>
                <p>Chọn nhanh biến thể theo màu, size và tồn kho khả dụng.</p>
              </div>
              <div class="bh-modal-counter">{{ filteredProductVariants.length }} biến thể</div>
              <button class="bh-modal-close" @click="showProductModal = false"><X :size="20" /></button>
            </div>
            <div class="bh-modal-search">
              <Search :size="18" class="bh-modal-search-icon" />
              <input v-model="productSearchKeyword" type="text" placeholder="Tìm tên, mã sản phẩm, màu, size..." />
            </div>
            <div class="bh-modal-list bh-product-grid">
              <div v-if="!filteredProductVariants.length" class="bh-modal-empty">Không tìm thấy sản phẩm phù hợp</div>
              <div
                v-for="variant in filteredProductVariants"
                :key="variant.spctId"
                class="bh-modal-item"
                :class="{ 'bh-modal-item-disabled': availableStockForVariant(variant) <= 0 }"
                @click="availableStockForVariant(variant) > 0 && addProductFromModal(variant)"
              >
                <div class="bh-modal-item-img">
                  <img :src="variant.image || logoFallback" :alt="variant.tenSanPham" @error="onImgError" />
                </div>
                <div class="bh-modal-item-info">
                  <div class="bh-modal-item-top">
                    <div class="bh-modal-item-code">{{ variant.maSanPhamChiTiet || variant.maSanPham }}</div>
                    <div class="bh-modal-item-stock" :class="{ 'is-low': rawStockForVariant(variant) <= 5 }">
                      Tồn: {{ rawStockForVariant(variant) }}
                    </div>
                  </div>
                  <div class="bh-modal-item-name">{{ variant.tenSanPham }}</div>
                  <div class="bh-modal-item-variant-line">
                    <span class="bh-modal-item-variant-pill">Màu: <strong>{{ variant.tenMau || 'Mặc định' }}</strong></span>
                    <span class="bh-modal-item-variant-pill">Size: <strong>{{ variant.tenSize || 'Free size' }}</strong></span>
                  </div>
                  <div class="bh-modal-item-price">
                    <span v-if="variant.campaignPercent" class="bh-modal-item-price-original">{{ formatCurrency(variant.giaBanGoc) }}</span>
                    {{ formatCurrency(variant.giaBan) }}
                    <span v-if="variant.campaignPercent" class="bh-modal-campaign-badge">-{{ variant.campaignPercent }}%</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ─── Payment modal ─── -->
    <Teleport to="body">
      <Transition name="bh-modal">
        <div v-if="showPaymentModal" class="bh-modal-overlay" @click.self="showPaymentModal = false">
          <div class="bh-modal-box bh-pay-modal">
            <div class="bh-modal-head">
              <h2>Thanh toán</h2>
              <button class="bh-modal-close" @click="showPaymentModal = false"><X :size="20" /></button>
            </div>
            <div class="bh-pay-modal-body">
              <!-- Tổng tiền -->
              <div class="bh-pay-modal-total">
                <div class="bh-pay-modal-total-main">
                  <span>Tổng số tiền</span>
                  <strong>{{ formatCurrency(grandTotal) }}</strong>
                </div>
              </div>

              <!-- Mode selector: 4 tabs -->
              <div class="bh-pay-mode-tabs">
                <button
                  type="button"
                  class="bh-pay-mode-tab"
                  :class="{ 'is-active': paymentMethod === 'CASH' }"
                  @click="paymentMethod = 'CASH'; applyPaymentMode('CASH')"
                >Tiền mặt</button>
                <button
                  type="button"
                  class="bh-pay-mode-tab"
                  :class="{ 'is-active': paymentMethod === 'BANK' }"
                  @click="paymentMethod = 'BANK'; applyPaymentMode('BANK')"
                >Chuyển khoản</button>

                <button
                  type="button"
                  class="bh-pay-mode-tab"
                  :class="{ 'is-active': paymentMethod === 'BOTH' }"
                  @click="paymentMethod = 'BOTH'; applyPaymentMode('BOTH')"
                >Tiền mặt + CK</button>
              </div>

              <!-- Inputs -->
              <div class="bh-pay-modal-inputs" :class="{ 'bh-pay-modal-inputs--single': paymentMethod !== 'BOTH' }">
                <!-- Tiền mặt -->
                <div
                  v-if="paymentMethod === 'CASH' || paymentMethod === 'BOTH'"
                  class="bh-pay-modal-col"
                  :class="{ 'bh-pay-modal-col--single': paymentMethod !== 'BOTH' }"
                >
                  <div class="bh-pay-modal-col-head">
                    <label>Tiền mặt</label>
                    <span v-if="paymentMethod === 'BOTH'" class="bh-pay-modal-remaining">
                      Còn lại: <b>{{ formatCurrency(paymentRemainingCash) }}</b>
                    </span>
                  </div>
                  <input
                    type="number"
                    class="bh-input"
                    v-model.number="paymentCash"
                    min="0"
                    placeholder="Nhập tiền mặt..."
                  />
                </div>
                <!-- Chuyển khoản -->
                <div
                  v-if="paymentMethod === 'BANK' || paymentMethod === 'BOTH'"
                  class="bh-pay-modal-col"
                  :class="{ 'bh-pay-modal-col--single': paymentMethod !== 'BOTH' }"
                >
                  <div class="bh-pay-modal-col-head">
                    <label>Chuyển khoản</label>
                    <span v-if="paymentMethod === 'BOTH'" class="bh-pay-modal-remaining">
                      Còn lại: <b>{{ formatCurrency(paymentRemainingBank) }}</b>
                    </span>
                  </div>
                  <input
                    type="number"
                    class="bh-input"
                    v-model.number="paymentBank"
                    min="0"
                    placeholder="Nhập chuyển khoản..."
                  />
                </div>
              </div>



              <!-- Summary -->
              <div class="bh-pay-modal-summary">
                <div class="bh-pay-modal-row"><span>Đã nhập</span><strong>{{ formatCurrency(paymentEntered) }}</strong></div>
                <div class="bh-pay-modal-row"><span>Tiền thiếu</span><strong class="bh-text-danger">{{ formatCurrency(paymentRemaining) }}</strong></div>
                <div class="bh-pay-modal-row"><span>Tiền thừa</span><strong class="bh-text-success">{{ formatCurrency(paymentChange) }}</strong></div>
              </div>

              <div class="bh-pay-modal-actions">
                <button class="bh-btn bh-btn-outline" type="button" @click="showPaymentModal = false">Đóng</button>
                <button
                  class="bh-btn bh-btn-primary"
                  type="button"
                  :disabled="saving || paymentRemaining > 0"
                  @click="confirmPayment"
                >
                  {{ saving ? 'Đang xử lý...' : 'Xong' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </main>
</template>

<style scoped>
/* ── POS Tabs ── */
.bh-tabs-bar {
  display: flex;
  gap: 6px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.bh-tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #d1d5db;
  border-radius: 10px 10px 0 0;
  background: #f1f5f9;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all .15s;
}
.bh-tab-btn.active {
  background: #fff;
  color: #b91c1c;
  border-bottom-color: #fff;
  box-shadow: 0 -2px 6px rgba(0,0,0,.05);
}
.bh-tab-btn:hover:not(.active) { background: #e2e8f0; }
.bh-tab-close {
  font-size: 16px;
  line-height: 1;
  color: #94a3b8;
  cursor: pointer;
}
.bh-tab-close:hover { color: #ef4444; }
.bh-tab-add {
  font-size: 18px;
  font-weight: 700;
  color: #b91c1c;
  border-style: dashed;
}
/* ── Page ── */
.bh-page {
  padding: 24px;
  min-height: 100vh;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
}
.bh-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.bh-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #111827;
}
.bh-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
}
.bh-header-actions { position: relative; }
.bh-dropdown-wrap { position: relative; display: inline-block; }
.bh-dropdown-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 4px);
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.10);
  z-index: 50;
  min-width: 180px;
  padding: 4px 0;
}
.bh-dropdown-menu button {
  display: block;
  width: 100%;
  text-align: left;
  padding: 10px 16px;
  border: none;
  background: none;
  font-size: 14px;
  color: #334155;
  cursor: pointer;
}
.bh-dropdown-menu button:hover { background: #f1f5f9; }
.bh-loading {
  text-align: center;
  padding: 40px;
  color: #64748b;
  font-weight: 600;
}

/* ── Layout ── */
.bh-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(360px, 1fr);
  gap: 20px;
  align-items: start;
}
.bh-main {
  display: grid;
  gap: 20px;
}
.bh-sidebar {
  display: grid;
  gap: 16px;
  position: sticky;
  top: 80px;
}

/* ── Card ── */
.bh-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  overflow: hidden;
}
.bh-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eef2f7;
}
.bh-card-head h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: #111827;
}
.bh-card-actions {
  display: flex;
  gap: 8px;
}
.bh-card-body {
  padding: 18px 20px;
  display: grid;
  gap: 14px;
}

/* ── Buttons ── */
.bh-btn {
  border: none;
  border-radius: 10px;
  padding: 10px 16px;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.15s ease;
}
.bh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.bh-btn-primary {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: #fff;
  box-shadow: 0 4px 12px rgba(185,28,28,0.25);
}
.bh-btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(185,28,28,0.3);
}
.bh-btn-outline {
  background: #fff;
  color: #374151;
  border: 1px solid #d1d5db;
}
.bh-btn-outline:hover:not(:disabled) {
  border-color: #9ca3af;
  background: #f9fafb;
}
.bh-btn-sm {
  padding: 7px 12px;
  font-size: 12px;
}
.bh-btn-submit {
  width: 100%;
  justify-content: center;
  padding: 14px;
  font-size: 15px;
  letter-spacing: 0.04em;
  border-radius: 12px;
  margin-top: 4px;
}

/* ── Cart ── */
.bh-cart {
  padding: 6px 0;
}
.bh-cart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 40px 20px;
  color: #94a3b8;
}
.bh-cart-empty p {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
}
.bh-cart-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 20px;
  border-bottom: 1px solid #f3f4f6;
  transition: background 0.15s;
}
.bh-cart-item:last-of-type {
  border-bottom: none;
}
.bh-cart-item:hover {
  background: #fafbfc;
}

/* Thumbnail */
.bh-cart-thumb {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  overflow: hidden;
  background: #f1f5f9;
  flex-shrink: 0;
  border: 1px solid #e5e7eb;
}
.bh-cart-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.bh-cart-thumb-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: #94a3b8;
}

/* Info */
.bh-cart-info {
  flex: 1;
  min-width: 0;
}
.bh-cart-name {
  font-weight: 700;
  font-size: 14px;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bh-cart-meta {
  display: flex;
  gap: 4px;
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}
.bh-cart-code {
  color: #9ca3af;
}
.bh-dot {
  color: #d1d5db;
}

/* Price */
.bh-cart-price {
  font-weight: 700;
  font-size: 14px;
  color: #111827;
  min-width: 100px;
  text-align: right;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  flex-wrap: wrap;
}
.bh-cart-campaign-badge {
  font-size: 11px;
  font-weight: 700;
  background: #dc2626;
  color: #fff;
  padding: 1px 5px;
  border-radius: 4px;
}

/* Qty controls */
.bh-cart-qty {
  display: flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  overflow: hidden;
}
.bh-qty-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #f9fafb;
  color: #374151;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.15s;
}
.bh-qty-btn:hover:not(:disabled) {
  background: #f1f5f9;
}
.bh-qty-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.bh-qty-value {
  width: 36px;
  text-align: center;
  font-weight: 700;
  font-size: 14px;
  color: #111827;
  border-left: 1px solid #d1d5db;
  border-right: 1px solid #d1d5db;
  line-height: 32px;
}

.bh-qty-input {
  width: 48px;
  height: 32px;
  border: 0;
  border-left: 1px solid #d1d5db;
  border-right: 1px solid #d1d5db;
  background: transparent;
  text-align: center;
  font-weight: 700;
  font-size: 14px;
  color: #111827;
  outline: none;
  -moz-appearance: textfield;
}
.bh-qty-input::-webkit-outer-spin-button,
.bh-qty-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

/* Delete */
.bh-cart-delete {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: none;
  background: #fef2f2;
  color: #dc2626;
  display: grid;
  place-items: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s;
}
.bh-cart-delete:hover {
  background: #fee2e2;
}

/* Cart total */
.bh-cart-total {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  border-top: 1px solid #e5e7eb;
  font-size: 15px;
  color: #374151;
}
.bh-cart-total strong {
  font-size: 18px;
  color: #dc2626;
  font-weight: 800;
}

/* ── Customer panel ── */
.bh-kv {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}
.bh-k {
  color: #64748b;
  font-weight: 600;
}
.bh-v {
  color: #111827;
  font-weight: 700;
}
.bh-hint {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;
}
.bh-select {
  width: 100%;
  padding: 10px 34px 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  background-color: #fff;
}
.bh-select:focus {
  outline: none;
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220,38,38,0.1);
}
.bh-select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.bh-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  background: #fff;
}
.bh-input:focus {
  outline: none;
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220,38,38,0.1);
}

.bh-quick-form {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px dashed #d1d5db;
  border-radius: 10px;
  background: #f9fafb;
}
.bh-quick-form input {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font: inherit;
  font-size: 13px;
  color: #111827;
}
.bh-quick-form input:focus {
  outline: none;
  border-color: #dc2626;
}
.bh-quick-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

/* ── Payment panel ── */
.bh-payment-head {
  gap: 12px;
}
.bh-field {
  display: grid;
  gap: 6px;
}
.bh-field label {
  font-size: 13px;
  font-weight: 700;
  color: #374151;
}
.bh-summary {
  display: grid;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}
.bh-pay-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #374151;
}
.bh-pay-row strong {
  font-weight: 700;
}
.bh-pay-discount strong {
  color: #dc2626;
}
.bh-pay-total {
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px dashed #d1d5db;
  font-size: 16px;
  font-weight: 800;
}
.bh-pay-total strong {
  font-size: 18px;
  color: #dc2626;
  font-weight: 800;
}

.bh-customer-modal {
  width: min(980px, calc(100vw - 36px));
  max-width: 980px;
  max-height: 90vh;
  grid-template-rows: auto auto 1fr;
}
.bh-customer-modal-head {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  padding: 18px 72px 14px 22px;
  align-items: center;
  gap: 12px;
}
.bh-customer-modal .bh-modal-close {
  position: absolute;
  top: 16px;
  right: 16px;
}
.bh-customer-modal .bh-customer-head-copy h2 {
  margin: 0;
  font-size: 30px;
  font-weight: 900;
  letter-spacing: -0.02em;
  line-height: 1.15;
}
.bh-customer-modal-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bh-customer-counter {
  padding: 8px 14px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}
.bh-customer-guest-btn {
  min-width: 108px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  border: 1px solid #dc2626;
  background: #dc2626;
  color: #ffffff;
  font-weight: 800;
}
.bh-customer-guest-btn:hover {
  background: #b91c1c;
  border-color: #b91c1c;
}
.bh-customer-modal-body {
  padding: 16px;
  display: grid;
  gap: 14px;
  overflow: auto;
}
.bh-customer-table-wrap {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: auto;
  max-height: 60vh;
}
.bh-customer-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 15px;
}
.bh-customer-table th,
.bh-customer-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #eef2f7;
  text-align: left;
}
.bh-customer-table thead th {
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  position: sticky;
  top: 0;
  z-index: 1;
}
.bh-customer-table tbody tr {
  transition: background-color 0.15s ease;
}
.bh-customer-table tbody tr:hover {
  background: #f8fafc;
}
.bh-customer-table tbody tr.is-selected {
  background: #eef2ff;
}
.bh-customer-table tbody td:first-child {
  font-weight: 700;
  color: #111827;
}
.right {
  text-align: right !important;
}
.bh-customer-create {
  border: 1px dashed #d1d5db;
  border-radius: 12px;
  background: #f9fafb;
  padding: 12px;
  display: grid;
  gap: 8px;
}
.bh-customer-create p {
  margin: 0;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}
.bh-customer-create-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .bh-customer-modal {
    width: 100%;
    max-width: 100%;
    max-height: 94vh;
  }
  .bh-customer-modal-head {
    grid-template-columns: 1fr;
    padding: 16px 56px 12px 16px;
    gap: 10px;
    align-items: start;
  }
  .bh-customer-modal .bh-customer-head-copy h2 {
    font-size: 20px;
  }
  .bh-customer-modal-tools {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
  .bh-customer-table {
    font-size: 14px;
  }
  .bh-customer-table-wrap {
    max-height: 62vh;
  }
}

/* ── Modal ── */
.bh-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15,23,42,0.5);
  backdrop-filter: blur(4px);
  display: grid;
  place-items: center;
  z-index: 9999;
  padding: 20px;
  overflow-x: hidden;
  overflow-y: auto;
  box-sizing: border-box;
}
.bh-modal-box {
  background: #fff;
  border-radius: 20px;
  width: min(100%, 640px);
  max-width: 640px;
  max-height: 80vh;
  display: grid;
  grid-template-rows: auto auto 1fr;
  box-shadow: 0 24px 60px rgba(15,23,42,0.2);
  overflow: hidden;
  box-sizing: border-box;
}
.bh-product-modal {
  width: min(1180px, calc(100vw - 40px));
  max-width: 1180px;
  max-height: 88vh;
  background:
    radial-gradient(circle at top right, rgba(239, 68, 68, 0.08), transparent 24%),
    linear-gradient(180deg, #ffffff 0%, #fffafa 100%);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-sizing: border-box;
}
.bh-modal-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px 14px;
  border-bottom: 1px solid #eef2f7;
}
.bh-modal-head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}
.bh-product-modal .bh-modal-head {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 14px;
  padding: 18px 70px 12px 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(255, 250, 250, 0.96));
}
.bh-product-modal-head-copy {
  min-width: 0;
  flex: 1;
}
.bh-product-modal-head-copy h2 {
  font-size: 16px;
  letter-spacing: -0.02em;
}
.bh-product-modal-head-copy p {
  margin: 4px 0 0;
  color: #7c6b70;
  font-size: 12px;
  font-weight: 600;
}
.bh-modal-counter {
  padding: 10px 16px;
  border-radius: 999px;
  background: linear-gradient(135deg, #fee2e2, #fecaca);
  border: 1px solid rgba(185, 28, 28, 0.3);
  color: #991b1b;
  font-size: 14px;
  font-weight: 900;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}
.bh-modal-close {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: #f1f5f9;
  color: #475569;
  display: grid;
  place-items: center;
  cursor: pointer;
}
.bh-product-modal .bh-modal-close {
  position: absolute;
  top: 18px;
  right: 20px;
}
.bh-modal-close:hover {
  background: #fee2e2;
  color: #dc2626;
}
.bh-modal-search {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 24px;
  border-bottom: 1px solid #eef2f7;
}
.bh-modal-search-icon {
  color: #94a3b8;
  flex-shrink: 0;
}
.bh-modal-search input {
  width: 100%;
  border: none;
  outline: none;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  background: transparent;
}
.bh-modal-search input::placeholder {
  color: #94a3b8;
}
.bh-product-modal .bh-modal-search {
  margin: 0 18px;
  padding: 12px 16px;
  border: 1px solid #eadde0;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255,255,255,0.98), rgba(255,250,250,0.98));
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.7);
  box-sizing: border-box;
}
.bh-product-modal .bh-modal-search:focus-within {
  border-color: #f87171;
  box-shadow: 0 0 0 4px rgba(248, 113, 113, 0.14);
}
.bh-modal-list {
  overflow-y: auto;
  overflow-x: hidden;
  padding: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  grid-auto-rows: max-content;
  align-items: start;
  gap: 14px;
}
.bh-product-grid {
  padding: 14px 18px 18px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  align-content: start;
  box-sizing: border-box;
}
.bh-modal-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 32px;
  color: #94a3b8;
  font-weight: 600;
}
.bh-modal-item {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0;
  padding: 0;
  border-radius: 16px;
  border: 2px solid #1a1a1a;
  background: #ffffff;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.22s ease;
  position: relative;
  overflow: hidden;
}
.bh-modal-item:hover {
  transform: translateY(-3px);
  border-color: #000000;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.18);
}
.bh-modal-item-disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.bh-modal-item-disabled:hover {
  transform: none;
  border-color: #d1d5db;
  box-shadow: none;
}
.bh-modal-item-img {
  width: 100%;
  height: 250px;
  overflow: hidden;
  background: #f5f5f5;
  flex-shrink: 0;
}
.bh-modal-item-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center center;
  display: block;
}
.bh-modal-item-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: #94a3b8;
}
.bh-modal-item-info {
  padding: 14px 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}
.bh-modal-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.bh-modal-item-code {
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 4px 10px;
  border-radius: 999px;
  background: #f3f4f6;
  color: #374151;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid #d1d5db;
}
.bh-modal-item-stock {
  flex-shrink: 0;
  padding: 5px 11px;
  border-radius: 999px;
  background: #bbf7d0;
  color: #14532d;
  font-size: 13px;
  font-weight: 800;
  border: 1px solid #86efac;
}
.bh-modal-item-stock.is-low {
  background: #fed7aa;
  color: #9a3412;
  border-color: #fdba74;
}
.bh-modal-item-name {
  font-weight: 700;
  font-size: 17px;
  color: #111827;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.bh-modal-item-variant-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.bh-modal-item-variant-pill {
  padding: 7px 14px;
  border-radius: 999px;
  background: #e5e7eb;
  border: 1px solid #9ca3af;
  color: #111827;
  font-size: 13px;
  font-weight: 800;
}
.bh-modal-item-variant-pill strong {
  font-weight: 900;
}
.bh-modal-item-price {
  font-weight: 900;
  color: #b91c1c;
  font-size: 24px;
  line-height: 1.1;
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px solid #e5e7eb;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.65);
  display: flex;
  align-items: baseline;
  gap: 6px;
  flex-wrap: wrap;
}
.bh-modal-item-price-original {
  font-size: 14px;
  font-weight: 500;
  color: #9ca3af;
  text-decoration: line-through;
  text-shadow: none;
}
.bh-modal-campaign-badge {
  font-size: 12px;
  font-weight: 700;
  background: #dc2626;
  color: #fff;
  padding: 1px 6px;
  border-radius: 4px;
  text-shadow: none;
}

/* ── Modal transition ── */
.bh-modal-enter-active { transition: opacity 0.2s ease; }
.bh-modal-enter-active .bh-modal-box { transition: transform 0.25s cubic-bezier(.25,.8,.25,1), opacity 0.2s ease; }
.bh-modal-leave-active { transition: opacity 0.15s ease; }
.bh-modal-leave-active .bh-modal-box { transition: transform 0.15s ease, opacity 0.15s ease; }
.bh-modal-enter-from { opacity: 0; }
.bh-modal-enter-from .bh-modal-box { transform: translateY(20px) scale(0.97); opacity: 0; }
.bh-modal-leave-to { opacity: 0; }
.bh-modal-leave-to .bh-modal-box { transform: translateY(10px) scale(0.98); opacity: 0; }

/* ── Responsive ── */
@media (max-width: 900px) {
  .bh-product-modal-head-copy p {
    display: none;
  }
  .bh-layout {
    grid-template-columns: 1fr;
  }
  .bh-sidebar {
    position: static;
  }
  .bh-cart-item {
    flex-wrap: wrap;
    gap: 10px;
  }
  .bh-cart-price {
    min-width: auto;
  }
  .bh-product-modal {
    width: min(96vw, 960px);
    max-width: 96vw;
  }
  .bh-product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    padding: 10px;
  }
  .bh-modal-item-img {
    height: 220px;
  }
  .bh-modal-item-price {
    font-size: 19px;
  }
}

@media (max-width: 768px) {
  .bh-modal-overlay {
    padding: 8px;
  }
  .bh-product-modal {
    width: 100%;
    max-width: 100%;
  }
  .bh-product-modal .bh-modal-head {
    grid-template-columns: 1fr;
    gap: 10px;
    padding: 16px 58px 10px 16px;
  }
  .bh-modal-counter {
    justify-self: start;
  }
  .bh-product-modal .bh-modal-search {
    margin: 0 12px;
    padding: 10px 14px;
  }
  .bh-product-grid {
    grid-template-columns: 1fr;
    padding: 8px;
  }
  .bh-modal-item-img {
    height: 210px;
  }
  .bh-modal-item-top {
    align-items: flex-start;
    flex-direction: column;
  }
  .bh-modal-item-code {
    max-width: 100%;
  }
}


/* ── Toggle switch ── */
.bh-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}
.bh-toggle-label {
  font-size: 13px;
  font-weight: 700;
  color: #374151;
}
.bh-toggle input {
  display: none;
}
.bh-toggle-track {
  position: relative;
  width: 42px;
  height: 24px;
  background: #d1d5db;
  border-radius: 999px;
  transition: background 0.2s;
}
.bh-toggle-track::after {
  content: "";
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  background: #fff;
  border-radius: 50%;
  transition: transform 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
.bh-toggle input:checked + .bh-toggle-track {
  background: #dc2626;
}
.bh-toggle input:checked + .bh-toggle-track::after {
  transform: translateX(18px);
}

/* ── Address section ── */
.bh-address-section {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f9fafb;
}
.bh-address-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #374151;
}
.bh-address-form {
  display: grid;
  gap: 6px;
}

/* ── Shipping fee ── */
.bh-shipping-fee {
  padding: 10px 12px;
  border: 1px solid #fecaca;
  border-radius: 10px;
  background: #fef2f2;
}
.bh-shipping-fee-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #374151;
}
.bh-shipping-fee-row span {
  display: flex;
  align-items: center;
  gap: 6px;
}
.bh-shipping-fee-row strong {
  color: #b91c1c;
}
.bh-select-sm {
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 6px;
  border: 1px solid #fecaca;
  background-color: #fff;
  color: #374151;
}

/* ── Payment modal ── */
.bh-pay-modal {
  max-width: 760px;
  width: min(96vw, 760px);
}
.bh-pay-modal-body {
  padding: 32px 36px 36px;
  display: grid;
  gap: 26px;
}
.bh-pay-modal-total {
  display: grid;
  gap: 10px;
  font-size: 20px;
  color: #111827;
  padding-bottom: 18px;
  border-bottom: 1px solid #e5e7eb;
}
.bh-pay-modal-total-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 700;
}
.bh-pay-modal-total strong {
  font-size: 32px;
  color: #dc2626;
  font-weight: 800;
}
/* Mode tabs */
.bh-pay-mode-tabs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.bh-pay-mode-tab {
  padding: 11px 10px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  background: #f9fafb;
  color: #374151;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.bh-pay-mode-tab:hover {
  border-color: #dc2626;
  color: #dc2626;
}
.bh-pay-mode-tab.is-active {
  border-color: #dc2626;
  background: #fef2f2;
  color: #dc2626;
}

/* Inputs grid */
.bh-pay-modal-inputs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.bh-pay-modal-inputs--single {
  grid-template-columns: minmax(360px, 460px);
  justify-content: center;
}
.bh-pay-modal-col {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
}
.bh-pay-modal-col-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.bh-pay-modal-col label {
  font-size: 17px;
  font-weight: 700;
  color: #111827;
}
.bh-pay-modal-col .bh-input {
  font-size: 20px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #d1d5db;
}
.bh-pay-modal-col--single .bh-pay-modal-col-head {
  justify-content: center;
  text-align: center;
}
.bh-pay-modal-col--single .bh-input {
  text-align: center;
}
.bh-pay-modal-remaining {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
}
.bh-pay-modal-remaining b {
  color: #dc2626;
}
.bh-pay-modal-summary {
  display: grid;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}
.bh-pay-modal-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  color: #374151;
}
.bh-pay-modal-row strong {
  font-weight: 700;
}
.bh-text-danger {
  color: #dc2626;
}
.bh-text-success {
  color: #16a34a;
}
.bh-pay-modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 8px;
}
.bh-pay-modal-actions .bh-btn {
  min-width: 96px;
  min-height: 44px;
  font-size: 15px;
  justify-content: center;
}


</style>
