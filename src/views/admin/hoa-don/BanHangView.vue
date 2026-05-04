<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { createHoaDon, addHoaDonItem, updateHoaDon, updateHoaDonBySystemEvent } from "../../../services/hoaDonService"
import { getAllSanPham, getSanPhamById } from "../../../services/sanPhamService"
import { createKhachHang, getAllKhachHang } from "../../../services/KhachHangService"
import taiKhoanService from "../../../services/taiKhoanService"
import { getAllNhanVien, getNhanVienByTaiKhoanId } from "../../../services/nhanVienService"
import { getDiaChiByKhachHang } from "../../../services/diaChiService"
import { quoteShippingFeeAll } from "../../../services/shippingQuoteService"
import { Plus, Search, X, Minus, ShoppingBag, MapPin, Truck } from "lucide-vue-next"
import { appendPaymentFlowTag, PAYMENT_FLOW_TAGS } from "../../../utils/paymentWorkflow"
import { buildOrderLookupTrackingUrl } from "../../../utils/publicTrackingUrl"
import VoucherSelector from "../../../components/voucher/VoucherSelector.vue"
import { resolveApiOrigin } from "../../../utils/apiOrigin"
import { getProductImageOverride, getProductImageConfig } from "../../../utils/productImageOverrides"
import { fallbackImageForVariant } from "../../../utils/productImageFallback"
import { applyCampaignPriceToVariants } from "../../../services/campaignPricingService"
import logoFallback from "../../../assets/img/logo/DirtyWaveLogo.png?url"
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
const detectedGuestCustomerId = ref(null)
const variants = ref([])
const soldQtyBySpctSnapshot = ref(new Map())

const cashierId = ref(null)
const customerId = ref(null)
const taiQuay = ref(true)
const paymentMethod = ref("CASH")
const orderNote = ref("")
const discount = ref(0)
const selectedVoucher = ref(null)
const showCustomerSearchModal = ref(false)
const showHeaderMenu = ref(false)
const customerPhoneSearch = ref("")
const customerDraftName = ref("")
const customerSearchInputRef = ref(null)

const lines = ref([])
const showProductModal = ref(false)
const productSearchKeyword = ref("")

const MAX_POS_TABS = 5
const MAX_POS_DRAFT_SIZE = 3_500_000
const posTabs = ref([null]) // array of draft snapshots; null = empty tab
const activeTabIndex = ref(0)

const posLayoutRef = ref(null)
const posLeftWidthPercent = ref(58)
const isResizingPanels = ref(false)
const POS_LEFT_MIN = 42
const POS_LEFT_MAX = 74
let resizeStartX = 0
let resizeStartWidth = 58

const clampPosLeftWidth = (value) => Math.min(POS_LEFT_MAX, Math.max(POS_LEFT_MIN, Number(value || 58)))

const onPanelResizeMove = (event) => {
  const rect = posLayoutRef.value?.getBoundingClientRect?.()
  if (!rect?.width) return
  const deltaPercent = ((Number(event?.clientX || 0) - resizeStartX) / rect.width) * 100
  posLeftWidthPercent.value = clampPosLeftWidth(resizeStartWidth + deltaPercent)
}

const stopPanelResize = () => {
  isResizingPanels.value = false
  window.removeEventListener("mousemove", onPanelResizeMove)
  window.removeEventListener("mouseup", stopPanelResize)
}

const startPanelResize = (event) => {
  if (window.innerWidth <= 900) return
  isResizingPanels.value = true
  resizeStartX = Number(event?.clientX || 0)
  resizeStartWidth = Number(posLeftWidthPercent.value || 58)
  window.addEventListener("mousemove", onPanelResizeMove)
  window.addEventListener("mouseup", stopPanelResize)
}

const getPosDraftKey = () => {
  const role = String(localStorage.getItem("role") || "").trim().toUpperCase()
  const userId = String(localStorage.getItem("userId") || "").trim()
  return `banhang:tabs:${role || "UNKNOWN"}:${userId || "0"}`
}

const ORDER_ITEM_PRICING_SNAPSHOTS_KEY = "orderItemPricingSnapshots"
const ORDER_ITEM_VOUCHER_SNAPSHOTS_KEY = "orderItemVoucherSnapshots"

function persistOrderPricingSnapshot(orderLike, orderLines) {
  try {
    const orderId = Number(orderLike?.id || 0)
    const orderCode = String(orderLike?.maHoaDon || "").trim()
    if (!orderId && !orderCode) return

    const snapshot = {
      orderId: orderId || null,
      maHoaDon: orderCode,
      updatedAt: new Date().toISOString(),
      itemPricing: (orderLines || []).map((line) => ({
        spctId: Number(line?.spctId || 0),
        productId: Number(line?.productId || line?.sanPhamId || 0),
        color: String(line?.mauSac || "").trim(),
        size: String(line?.kichThuoc || "").trim(),
        soLuong: Math.max(Number(line?.soLuong || 0), 0),
        giaBan: Number(line?.giaBan || 0),
        giaBanGoc: Number(line?.giaBanGoc || line?.giaBan || 0),
        thanhTien: Number(line?.giaBan || 0) * Math.max(Number(line?.soLuong || 0), 0),
        campaignPercent: Number(line?.campaignPercent || 0),
        campaignName: String(line?.campaignName || "").trim()
      })).filter((line) => line.spctId > 0 || line.productId > 0)
    }

    const raw = localStorage.getItem(ORDER_ITEM_PRICING_SNAPSHOTS_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    const snapshots = parsed && typeof parsed === "object" ? parsed : {}

    if (orderId) snapshots[String(orderId)] = snapshot
    if (orderCode) snapshots[orderCode] = snapshot

    localStorage.setItem(ORDER_ITEM_PRICING_SNAPSHOTS_KEY, JSON.stringify(snapshots))
  } catch {
    // ignore snapshot persistence errors
  }
}

function persistOrderVoucherSnapshot({ keys = [], itemVouchers = [] } = {}) {
  const normalizedKeys = Array.isArray(keys)
    ? keys.map((key) => String(key || "").trim()).filter(Boolean)
    : []
  if (!normalizedKeys.length) return

  try {
    const raw = localStorage.getItem(ORDER_ITEM_VOUCHER_SNAPSHOTS_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    const snapshots = parsed && typeof parsed === "object" ? parsed : {}
    const payload = {
      createdAt: new Date().toISOString(),
      itemVouchers: Array.isArray(itemVouchers) ? itemVouchers : [],
    }

    for (const key of normalizedKeys) {
      snapshots[key] = payload
    }

    localStorage.setItem(ORDER_ITEM_VOUCHER_SNAPSHOTS_KEY, JSON.stringify(snapshots))
  } catch {
    // ignore snapshot persistence errors
  }
}

function compactVoucherForDraft(voucher) {
  if (!voucher || typeof voucher !== "object") return null
  return {
    id: Number(voucher.id || 0) || null,
    tenPhieuGiamGia: String(voucher.tenPhieuGiamGia || ""),
    maPhieuGiamGia: String(voucher.maPhieuGiamGia || ""),
    hinhThucGiam: Boolean(voucher.hinhThucGiam),
    giaTriGiamGia: Number(voucher.giaTriGiamGia || 0),
    hoaDonToiThieu: Number(voucher.hoaDonToiThieu || 0),
    soTienGiamToiDa: Number(voucher.soTienGiamToiDa || 0)
  }
}

function safeDraftStringify(payload) {
  const seen = new WeakSet()
  return JSON.stringify(payload, (key, value) => {
    if (typeof value === "object" && value !== null) {
      if (seen.has(value)) return undefined
      seen.add(value)
    }
    if (typeof value === "string" && value.length > 500) {
      return value.slice(0, 500)
    }
    return value
  })
}

function captureTabState() {
  return {
    taiQuay: Boolean(taiQuay.value),
    paymentMethod: String(paymentMethod.value || "CASH"),
    orderNote: String(orderNote.value || ""),
    selectedVoucher: compactVoucherForDraft(selectedVoucher.value),
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
  discount.value = 0  // recalculated by VoucherSelector when voucher is restored
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
  restoreAddressCascade()
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
    let serialized = safeDraftStringify({
      activeIndex: activeTabIndex.value,
      tabs: posTabs.value
    })

    if (!serialized || serialized.length > MAX_POS_DRAFT_SIZE) {
      serialized = safeDraftStringify({
        activeIndex: 0,
        tabs: [posTabs.value[activeTabIndex.value] || null]
      })
    }

    if (!serialized || serialized.length > MAX_POS_DRAFT_SIZE) {
      clearPosDraft()
      return
    }

    localStorage.setItem(getPosDraftKey(), serialized)
  } catch { /* ignore */ }
}

function restorePosDraft() {
  try {
    const raw = localStorage.getItem(getPosDraftKey())
    if (!raw) { posTabs.value = [null]; activeTabIndex.value = 0; return }
    if (raw.length > MAX_POS_DRAFT_SIZE) {
      clearPosDraft()
      posTabs.value = [null]
      activeTabIndex.value = 0
      return
    }
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
    clearPosDraft()
    posTabs.value = [null]
    activeTabIndex.value = 0
  }
}

// Shipping address (for delivery mode)
const customerAddresses = ref([])
const selectedAddressId = ref(null)
const manualAddress = ref({ soDienThoai: "", diaChiCuThe: "", phuongXa: "", quanHuyen: "", tinhThanh: "" })

// Payment (inline, no modal)
const showPaymentModal = ref(false)
const paymentCash = ref(0)
const paymentBank = ref(0)

// Product grid search
const posProductSearch = ref("")

// Variant picker
const variantPickerProduct = ref(null)
const showVariantPicker = ref(false)

// QR payment
const showQrModal = ref(false)
const showQrPaidModal = ref(false)
const qrOrderId = ref(null)
const qrAmount = ref(0)
const qrContent = ref("")
const qrPolling = ref(false)
const qrState = ref("preview")
const qrSummary = ref(null)
const qrIsTaiQuay = ref(true)
let qrPollTimer = null

// Default VietQR config (can be edited in QR modal)
const DEFAULT_VIETQR_BANK_ID = "970422"
const DEFAULT_VIETQR_ACCOUNT = "0982345671"
const DEFAULT_VIETQR_ACCOUNT_NAME = "DIRTY WAVE STORE"

const qrBankId = ref(DEFAULT_VIETQR_BANK_ID)
const qrAccount = ref(DEFAULT_VIETQR_ACCOUNT)
const qrAccountName = ref(DEFAULT_VIETQR_ACCOUNT_NAME)

function buildVietQrUrl(amount, content) {
  const amt = Math.round(Number(amount || 0))
  const desc = encodeURIComponent(String(content || "").slice(0, 25))
  const bankId = String(qrBankId.value || DEFAULT_VIETQR_BANK_ID).trim()
  const account = String(qrAccount.value || "").trim()
  const accountName = String(qrAccountName.value || DEFAULT_VIETQR_ACCOUNT_NAME).trim()
  return `https://img.vietqr.io/image/${bankId}-${account}-compact2.png?amount=${amt}&addInfo=${desc}&accountName=${encodeURIComponent(accountName)}`
}

function openQrPayment(orderId, amount, content, isTaiQuay = true) {
  qrOrderId.value = orderId
  qrAmount.value = amount
  qrContent.value = content
  qrIsTaiQuay.value = isTaiQuay
  qrState.value = "preview"
  qrSummary.value = null
  showQrModal.value = true
}

function clearPosTabAfterCheckout() {
  if (posTabs.value.length > 1) {
    posTabs.value.splice(activeTabIndex.value, 1)
    if (activeTabIndex.value >= posTabs.value.length) activeTabIndex.value = posTabs.value.length - 1
  } else {
    posTabs.value = [null]
    activeTabIndex.value = 0
  }
  applyTabState(posTabs.value[activeTabIndex.value] || null)
  persistPosDraft()
}

async function confirmQrDemoPayment() {
  if (!qrOrderId.value || qrPolling.value) return

  qrPolling.value = true
  qrState.value = "processing"
  try {
    await new Promise((resolve) => setTimeout(resolve, 900))
    const trackingUrl = buildOrderLookupTrackingUrl({ maHoaDon: qrContent.value })
    if (qrIsTaiQuay.value) {
      // Prefer explicit completion update for compatibility with stock/accounting flows.
      try {
        await updateHoaDon(qrOrderId.value, {
          orderStatusCode: "HOAN_THANH",
          statusNote: "[POS] Đã xác nhận thanh toán chuyển khoản tại quầy"
        })
      } catch {
        await updateHoaDonBySystemEvent(qrOrderId.value, "HOAN_TAT_POS", "Hoàn tất bán hàng tại quầy", trackingUrl)
      }
    } else {
      // Online transfer is paid successfully, auto-close order to completed.
      try {
        await updateHoaDon(qrOrderId.value, {
          orderStatusCode: "HOAN_THANH",
          statusNote: appendPaymentFlowTag("[ONLINE] Đã xác nhận thanh toán chuyển khoản", PAYMENT_FLOW_TAGS.VN_PAY_EMPLOYEE_CONFIRMED, "NV xác nhận thanh toán")
        })
      } catch {
        await updateHoaDonBySystemEvent(qrOrderId.value, "XAC_NHAN_DON_HANG", "Đã xác nhận thanh toán chuyển khoản", trackingUrl)
      }
    }

    qrSummary.value = {
      orderId: qrOrderId.value,
      maHoaDon: qrContent.value,
      amount: Number(qrAmount.value || 0),
      customerName: selectedCustomerInfo.value?.tenKhachHang || "Khách lẻ",
      phone: shippingPhone.value || selectedCustomerInfo.value?.soDienThoai || "—",
      address: shippingAddressText.value || "Mua tại quầy",
      items: lines.value.map((line) => ({
        name: line.tenSanPham || line.tenSanPhamChiTiet || line.maSanPhamChiTiet || "Sản phẩm",
        color: line.mauSac || "",
        size: line.kichThuoc || "",
        qty: Number(line.soLuong || 0),
        price: Number(line.giaBan || 0),
        total: Number(line.thanhTien ?? (Number(line.soLuong || 0) * Number(line.giaBan || 0))),
        image: line.image || "",
        maSanPham: line.maSanPham || "",
        idSanPham: line.idSanPham || 0
      }))
    }

    applyLocalStockAfterCheckout(lines.value)
    clearPosTabAfterCheckout()
    showQrModal.value = false
    qrState.value = "paid"
    showQrPaidModal.value = true
    window.toast?.success?.("Đã thanh toán chuyển khoản thành công")
  } catch (error) {
    qrState.value = "preview"
    window.toast?.error?.(error?.response?.data?.message || error?.message || "Không thể hoàn tất thanh toán")
  } finally {
    qrPolling.value = false
  }
}

function openQrOrderDetail() {
  const id = Number(qrSummary.value?.orderId || qrOrderId.value || 0)
  if (!id) return
  showQrPaidModal.value = false
  router.push(`${panelBasePath.value}/hoa-don/detail/${id}`)
}

function closeQrPaidModal() {
  showQrPaidModal.value = false
  qrSummary.value = null
}

function clearQrPolling() {
  if (qrPollTimer) { clearInterval(qrPollTimer); qrPollTimer = null }
  qrPolling.value = false
}

function cancelQrPayment() {
  clearQrPolling()
  qrState.value = "preview"
  qrSummary.value = null
  showQrModal.value = false
}

function formatCampaignLabel(label, percent) {
  const normalized = String(label || "").trim().replace(/clearance/gi, "Giảm giá")
  return normalized || `Giảm ${Number(percent || 0)}%`
}

function productCodeOrder(value = "") {
  const code = String(value || "").trim().toUpperCase()
  const match = code.match(/^SP0*(\d+)$/)
  if (match?.[1]) {
    const numeric = Number(match[1])
    if (Number.isFinite(numeric)) return numeric
  }
  return Number.POSITIVE_INFINITY
}

// Product grouping for grid display
const productGroups = computed(() => {
  const kw = posProductSearch.value.trim().toLowerCase()
  const groupMap = new Map()
  for (const v of variants.value) {
    const productId = Number(v.idSanPham || 0)
    const key = productId || v.tenSanPham
    if (!groupMap.has(key)) {
      groupMap.set(key, {
        idSanPham: v.idSanPham,
        tenSanPham: v.tenSanPham,
        maSanPham: v.maSanPham,
        image: v.image,
        giaBan: v.giaBan,
        giaBanGoc: v.giaBanGoc,
        campaignPercent: v.campaignPercent,
        variants: []
      })
    }
    groupMap.get(key).variants.push(v)
  }
  let groups = [...groupMap.values()]
  // Set price range and best image
  for (const g of groups) {
    const prices = g.variants.map(v => v.giaBan).filter(Boolean)
    const basePrices = g.variants.map(v => Number(v.giaBanGoc || v.giaBan || 0)).filter(Boolean)
    const discountVariants = g.variants.filter((v) => Number(v.campaignPercent || 0) > 0)
    const featuredVariant = [...g.variants].sort((a, b) => Number(b.soLuongTon || 0) - Number(a.soLuongTon || 0))[0]
    g.minPrice = Math.min(...prices)
    g.maxPrice = Math.max(...prices)
    g.minBasePrice = basePrices.length ? Math.min(...basePrices) : g.minPrice
    g.maxBasePrice = basePrices.length ? Math.max(...basePrices) : g.maxPrice
    g.maxCampaignPercent = discountVariants.length
      ? Math.max(...discountVariants.map((v) => Number(v.campaignPercent || 0)))
      : 0
    g.hasDiscount = g.maxCampaignPercent > 0 && (g.minBasePrice > g.minPrice || g.maxBasePrice > g.maxPrice)
    const campaignNames = [...new Set(discountVariants.map((v) => String(v.campaignName || "").trim()).filter(Boolean))]
    const primaryCampaignName = campaignNames[0] || ""
    g.campaignLabel = g.hasDiscount ? formatCampaignLabel(primaryCampaignName, g.maxCampaignPercent) : ""
    g.totalStock = g.variants.reduce((s, v) => s + Number(v.soLuongTon || 0), 0)
    g.displayColor = String(featuredVariant?.tenMau || g.variants[0]?.tenMau || "").trim()
    g.displaySize = String(featuredVariant?.tenSize || g.variants[0]?.tenSize || "").trim()
    // Use first variant image that's not logoFallback
    const bestImg = g.variants.find(v => v.image && v.image !== logoFallback)
    g.image = bestImg?.image || g.image || fallbackImageFor(g.idSanPham, g.maSanPham, g.tenSanPham)
  }
  if (kw) {
    groups = groups.filter(g =>
      [g.tenSanPham, g.maSanPham, ...g.variants.map(v => [v.maSanPhamChiTiet, v.tenMau, v.tenSize].join(" "))].join(" ").toLowerCase().includes(kw)
    )
  }
  groups.sort((left, right) => {
    const leftOrder = productCodeOrder(left?.maSanPham)
    const rightOrder = productCodeOrder(right?.maSanPham)
    if (leftOrder !== rightOrder) return leftOrder - rightOrder

    const leftCode = String(left?.maSanPham || "")
    const rightCode = String(right?.maSanPham || "")
    const codeCompare = leftCode.localeCompare(rightCode, "vi", { numeric: true, sensitivity: "base" })
    if (codeCompare !== 0) return codeCompare

    return String(left?.tenSanPham || "").localeCompare(String(right?.tenSanPham || ""), "vi", {
      numeric: true,
      sensitivity: "base"
    })
  })
  return groups
})

async function syncGroupVariantsBeforeOpen(group) {
  const productId = Number(group?.idSanPham || 0)
  if (!Number.isFinite(productId) || productId <= 0) return group

  try {
    const detailRes = await getSanPhamById(productId)
    const detailRaw = detailRes?.data?.data || detailRes?.data || null
    if (!detailRaw || typeof detailRaw !== "object") return group

    const rawVariants = Array.isArray(detailRaw?.sanPhamChiTiets) ? detailRaw.sanPhamChiTiets : []
    const pricedVariants = rawVariants.length
      ? await applyCampaignPriceToVariants(rawVariants, productId)
      : rawVariants
    const pricedProduct = {
      ...detailRaw,
      sanPhamChiTiets: pricedVariants
    }

    const freshVariants = flattenVariants([pricedProduct], soldQtyBySpctSnapshot.value)
      .filter((item) => Number(item?.idSanPham || 0) === productId)

    if (!freshVariants.length) return group

    return {
      ...group,
      idSanPham: productId,
      tenSanPham: group?.tenSanPham || detailRaw?.tenSanPham,
      maSanPham: detailRaw?.maSanPham || group?.maSanPham,
      variants: freshVariants
    }
  } catch {
    return group
  }
}

async function handleProductClick(group) {
  // Always show variant picker so staff can see size/color/ton kho even for single-variant products.
  // Sync from backend first so modal never shows stale variant count.
  const syncedGroup = await syncGroupVariantsBeforeOpen(group)
  variantPickerProduct.value = syncedGroup
  showVariantPicker.value = true
}

function pickVariant(variant) {
  addProductFromModal(variant)
  showVariantPicker.value = false
  variantPickerProduct.value = null
}

// Quick money buttons
const quickMoneyAmounts = computed(() => {
  const total = grandTotal.value
  if (total <= 0) return []
  const amounts = [total]
  const rounded = [Math.ceil(total / 10000) * 10000, Math.ceil(total / 50000) * 50000, Math.ceil(total / 100000) * 100000]
  for (const r of rounded) {
    if (r > total && !amounts.includes(r)) amounts.push(r)
  }
  amounts.push(500000, 1000000)
  return [...new Set(amounts)].sort((a, b) => a - b).slice(0, 4)
})

function applyPaymentMode(mode) {
  if (mode === 'CASH') {
    paymentBank.value = 0
  } else if (mode === 'BANK') {
    paymentCash.value = 0
  }
}

// Shipping fee (delivery mode)
const shippingFee = ref(0)
const shippingLoading = ref(false)
const shippingProvider = ref("GHN")

// Address combobox data (provinces API)
const provinces = ref([])
const districts = ref([])
const wards = ref([])
const loadingProvinces = ref(false)

async function loadProvinces() {
  if (provinces.value.length) return
  loadingProvinces.value = true
  try {
    const res = await fetch("https://provinces.open-api.vn/api/?depth=1")
    provinces.value = await res.json()
  } catch { provinces.value = [] }
  finally { loadingProvinces.value = false }
}

async function restoreAddressCascade() {
  const { tinhThanh, quanHuyen } = manualAddress.value
  if (!tinhThanh) return
  await loadProvinces()
  const province = provinces.value.find(p => p.name === tinhThanh)
  if (!province) return
  try {
    const res = await fetch(`https://provinces.open-api.vn/api/p/${province.code}?depth=2`)
    const data = await res.json()
    districts.value = data.districts || []
  } catch { districts.value = []; return }
  if (!quanHuyen) return
  const district = districts.value.find(d => d.name === quanHuyen)
  if (!district) return
  try {
    const res = await fetch(`https://provinces.open-api.vn/api/d/${district.code}?depth=2`)
    const data = await res.json()
    wards.value = data.wards || []
  } catch { wards.value = [] }
}

async function onManualProvinceChange() {
  districts.value = []
  wards.value = []
  manualAddress.value.quanHuyen = ""
  manualAddress.value.phuongXa = ""
  await loadProvinces()
  const province = provinces.value.find(p => p.name === manualAddress.value.tinhThanh)
  if (!province) return
  try {
    const res = await fetch(`https://provinces.open-api.vn/api/p/${province.code}?depth=2`)
    const data = await res.json()
    districts.value = data.districts || []
  } catch { districts.value = [] }
}

async function onManualDistrictFocus() {
  if (districts.value.length || !manualAddress.value.tinhThanh) return
  await loadProvinces()
  const province = provinces.value.find(p => p.name === manualAddress.value.tinhThanh)
  if (!province) return
  try {
    const res = await fetch(`https://provinces.open-api.vn/api/p/${province.code}?depth=2`)
    const data = await res.json()
    districts.value = data.districts || []
  } catch { districts.value = [] }
}

async function onManualWardFocus() {
  if (wards.value.length || !manualAddress.value.quanHuyen) return
  const district = districts.value.find(d => d.name === manualAddress.value.quanHuyen)
  if (!district) return
  try {
    const res = await fetch(`https://provinces.open-api.vn/api/d/${district.code}?depth=2`)
    const data = await res.json()
    wards.value = data.wards || []
  } catch { wards.value = [] }
}

async function onManualDistrictChange() {
  wards.value = []
  manualAddress.value.phuongXa = ""
  const district = districts.value.find(d => d.name === manualAddress.value.quanHuyen)
  if (!district) return
  try {
    const res = await fetch(`https://provinces.open-api.vn/api/d/${district.code}?depth=2`)
    const data = await res.json()
    wards.value = data.wards || []
  } catch { wards.value = [] }
}

function onManualPhoneInput(event) {
  const raw = String(event?.target?.value || "")
  const digits = raw.replace(/\D+/g, "")
  manualAddress.value.soDienThoai = digits
}

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
  SP001: img1,   // Bomber da lớn
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

// ═══════ Helpers ═══════════════════════════════════════đ═══════════════════════════════════════đ═══════════════════════════════════════đ—đ—đ

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

import { computeSoldBySpct, variantAvailableStock as _variantAvailableStock, variantStockValue as _variantStockValue } from "@/utils/stockCalculation"

async function loadSoldQtyBySpct() {
  return computeSoldBySpct()
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
  return _variantStockValue(variant)
}

function variantAvailableStockForPos(variant, soldBySpct = new Map()) {
  return _variantAvailableStock(variant, soldBySpct)
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
  new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + " đ"

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

function normalizeDisplayName(value = "") {
  return String(value || "")
    .replace(/([\p{Ll}\p{Lo}\p{M}])I(?=[\p{Ll}\p{Lo}\p{M}])/gu, "$1i")
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

  // Only use code-number mapping if product name also matches a known product line
  const nameConfirmsCurated = !!getMappedFallbackByName(name)
  if (allowCuratedCodeMap && nameConfirmsCurated) {
    const codeDigits = String(normalizedCode).replace(/\D+/g, "")
    const codeNum = Number(codeDigits)
    if (Number.isFinite(codeNum) && codeNum > 0 && mappedFallbackByCodeNum[codeNum]) {
      return mappedFallbackByCodeNum[codeNum]
    }
  }

  // For products without a name match, return the logo fallback
  return logoFallback
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
    "hong": ["pink", "rose", "fuchsia", "lavender", "lilac"],
    "tim": ["purple", "violet"],
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

function collectProductGalleryImages(product = {}) {
  const candidates = [
    product?.images,
    product?.listAnh,
    product?.danhSachAnh,
    product?.anhSanPhams,
    product?.hinhAnhSanPhams,
    product?.gallery,
    product?.galleries,
  ]

  return [...new Set(
    candidates
      .flatMap((entry) => Array.isArray(entry) ? entry : [entry])
      .map((entry) => {
        if (!entry) return ""
        if (typeof entry === "string") return toImageUrl(entry)
        return pickImageValue(entry)
      })
      .filter(Boolean)
  )]
}

function pickProductGalleryImageForVariant(product = {}, variant = {}, colorName = "", variants = []) {
  const gallery = collectProductGalleryImages(product)
  if (!gallery.length) return ""
  if (gallery.length === 1) return gallery[0]

  const byColorToken = pickImageFromListByColor(gallery, colorName)
  if (byColorToken) return byColorToken

  const colorId = Number(variant?.mauSac?.id || variant?.mauSacId || variant?.idMauSac || 0)
  if (!Number.isFinite(colorId) || colorId <= 0) return gallery[0]

  const colorOrder = []
  for (const row of (Array.isArray(variants) ? variants : [])) {
    const rowColorId = Number(row?.mauSac?.id || row?.mauSacId || row?.idMauSac || 0)
    if (!Number.isFinite(rowColorId) || rowColorId <= 0 || colorOrder.includes(rowColorId)) continue
    colorOrder.push(rowColorId)
  }

  const colorIndex = colorOrder.indexOf(colorId)
  if (colorIndex >= 0) {
    return gallery[Math.min(colorIndex, gallery.length - 1)]
  }

  return gallery[0]
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
  return parts.filter(Boolean).join(" ⬢ ")
}

const flattenVariants = (products, soldBySpct = new Map()) =>
  products.flatMap((product) => {
    if (!isEntityActive(product, true)) return []

    const imageConfig = getProductImageConfig({ id: product?.id, maSanPham: product?.maSanPham })
    const overrideImage = getProductImageOverride({ id: product?.id, maSanPham: product?.maSanPham })[0] || ""
    const hideColorForProduct = false
    const productCode = String(product?.maSanPham || product?.ma || "").trim().toUpperCase()

    const activeVariants = toVariantList(product).filter((variant) => isEntityActive(variant, true))

    // If product has no variants at all, create a synthetic entry so it appears in POS
    if (!activeVariants.length) {
      const productDirectImage = pickImageValue(product)
      const resolvedImage =
        productDirectImage ||
        (overrideImage ? toImageUrl(overrideImage) : "") ||
        fallbackImageFor(product?.id, productCode, product?.tenSanPham)
      return [{
        spctId: product.id,
        maSanPham: productCode,
        maSanPhamChiTiet: productCode,
        tenSanPham: product.tenSanPham || "Sản phẩm",
        label: product.tenSanPham || "Sản phẩm",
        tenMau: "",
        tenSize: "",
        idSanPham: Number(product?.id || 0),
        giaBan: Number(product?.giaBan ?? 0),
        soLuongTon: Number(product?.soLuong ?? product?.soLuongTon ?? 0),
        image: resolvedImage
      }]
    }

    return activeVariants.map((v) => {
      const colorId = hideColorForProduct ? 0 : Number(v?.mauSac?.id || v?.mauSacId || v?.idMauSac || 0)
      const colorImage = (imageConfig?.colorImages || []).find((entry) => Number(entry?.colorId) === colorId)?.image || ""
      const variantDirectImage = pickImageValue(v)
      const productDirectImage = pickImageValue(product)
      const tenMauRaw = normalizeDisplayColorName(v?.mauSac?.tenMau || v?.mauSac?.tenMauSac)
      const productGalleryImage = pickProductGalleryImageForVariant(product, v, tenMauRaw, activeVariants)
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
      // Only use curated images for products whose name actually matches a known product line
      const nameMatchesCurated = !!(getMappedFallbackByName(product?.tenSanPham))
      const hasCuratedImage = nameMatchesCurated && !!(staticColorImageMap[codeNum] || mappedFallbackByCodeNum[codeNum] || mappedFallbackByCode[productCode])
      const forcedCatalogImage =
        /^SP\d+$/i.test(productCode) && hasCuratedImage
          ? (staticColorImage || fallbackImageFor(product?.id, productCode, product?.tenSanPham))
          : ""
      // Prefer real variant/product images first; curated fallbacks are only used
      // when API data has no usable image.
      const resolvedImage =
        priorityStaticImage ||
        variantDirectImage ||
        productGalleryImage ||
        productDirectImage ||
        configColorResolved ||
        staticColorImage ||
        configGalleryImage ||
        forcedCatalogImage ||
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
        giaBanGoc: Number(v?.giaBanGoc ?? v?.giaNiemYet ?? v?.giaBan ?? v?.gia ?? 0),
        campaignPercent: Number(v?.dotGiamGiaPhanTram ?? v?.campaignPercent ?? v?.phanTramGiamGia ?? 0),
        campaignName: String(v?.campaignInfo?.tenKhuyenMai || v?.campaignName || v?.tenKhuyenMai || "").trim(),
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

function applyLocalStockAfterCheckout(checkoutLines = []) {
  if (!Array.isArray(checkoutLines) || !checkoutLines.length) return

  const soldByVariant = new Map()
  for (const line of checkoutLines) {
    const spctId = Number(line?.spctId || 0)
    const qty = Math.max(Number(line?.soLuong || 0), 0)
    if (!spctId || qty <= 0) continue
    soldByVariant.set(spctId, Number(soldByVariant.get(spctId) || 0) + qty)
  }
  if (!soldByVariant.size) return

  variants.value = variants.value.map((variant) => {
    const spctId = Number(variant?.spctId || 0)
    const sold = Number(soldByVariant.get(spctId) || 0)
    if (!spctId || sold <= 0) return variant
    return {
      ...variant,
      soLuongTon: Math.max(Number(variant?.soLuongTon || 0) - sold, 0)
    }
  })
}

// ═══════ Computed ═══════════════════════════════════════đ═══════════════════════════════════════đ═══════════════════════════════════════đ—đ

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
  return Math.max(
    subtotal.value - Number(discount.value || 0) + (taiQuay.value ? 0 : Number(shippingFee.value || 0)),
    0
  )
})

const paymentEntered = computed(() => Number(paymentCash.value || 0) + Number(paymentBank.value || 0))
const paymentRemaining = computed(() => Math.max(grandTotal.value - paymentEntered.value, 0))
const paymentChange = computed(() => Math.max(paymentEntered.value - grandTotal.value, 0))
// Còn lại theo từng cột (KiotViet-style split)
const paymentRemainingCash = computed(() => Math.max(grandTotal.value - Number(paymentBank.value || 0), 0))
const paymentRemainingBank = computed(() => Math.max(grandTotal.value - Number(paymentCash.value || 0), 0))

const resolveApiPaymentMethod = (method) => {
  const normalized = String(method || "").toUpperCase()
  if (normalized === "VNPAY") return "VNPAY"
  if (normalized === "BANK") return "BANK"
  // Backend currently rejects mixed enum in some environments, fallback to CASH.
  if (normalized === "BOTH") return "CASH"
  return "CASH"
}

const selectedCustomerInfo = computed(() =>
  khachHangList.value.find((k) => Number(k.id) === Number(customerId.value)) || null
)

const selectedCustomerLabel = computed(() => {
  return selectedCustomerInfo.value?.tenKhachHang || "Khách lẻ"
})

const selectedCustomerPhone = computed(() =>
  (isGuestLikeCustomer(selectedCustomerInfo.value)
    ? ""
    : String(selectedCustomerInfo.value?.soDienThoai || "").trim())
)

const POS_GUEST_CACHE_KEY = "pos:guest-customer-id"
const POS_GUEST_ENV_ID = Number(import.meta.env.VITE_POS_GUEST_CUSTOMER_ID || 0)

const normalizeLooseText = (value = "") =>
  String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()

const isGuestLikeCustomer = (kh = {}) => {
  const name = normalizeLooseText(kh?.tenKhachHang || "")
  const code = normalizeLooseText(kh?.maKhachHang || "")
  const email = normalizeLooseText(kh?.email || kh?.taiKhoan?.email || "")
  return (
    name.includes("khach le") ||
    name.includes("khach vang lai") ||
    name.includes("guest") ||
    code.includes("khachle") ||
    code.includes("guest") ||
    email.startsWith("guest")
  )
}

const guestCustomerCandidate = computed(() => {
  const list = Array.isArray(khachHangList.value) ? khachHangList.value : []
  return list.find((kh) => isGuestLikeCustomer(kh)) || null
})

async function detectGuestCustomerIdAcrossPages() {
  try {
    const immediateId = Number(guestCustomerCandidate.value?.id || 0)
    if (immediateId > 0) {
      detectedGuestCustomerId.value = immediateId
      try { localStorage.setItem(POS_GUEST_CACHE_KEY, String(immediateId)) } catch { /* ignore */ }
      return immediateId
    }

    const pageSize = 200
    let page = 0
    let totalPages = 1
    const maxPages = 20

    while (page < totalPages && page < maxPages) {
      const res = await getAllKhachHang(page, pageSize)
      const payload = res?.data
      const list = toList(payload)
      const matched = list.find((kh) => isGuestLikeCustomer(kh))
      const matchedId = Number(matched?.id || 0)
      if (matchedId > 0) {
        detectedGuestCustomerId.value = matchedId
        try { localStorage.setItem(POS_GUEST_CACHE_KEY, String(matchedId)) } catch { /* ignore */ }
        return matchedId
      }

      const parsedTotalPages = Number(payload?.totalPages || 0)
      if (Number.isFinite(parsedTotalPages) && parsedTotalPages > 0) {
        totalPages = parsedTotalPages
      } else if (list.length < pageSize) {
        break
      } else {
        totalPages = page + 2
      }
      page += 1
    }
  } catch { /* ignore */ }
  return null
}

function resolveFallbackCustomerId() {
  if (guestCustomerCandidate.value?.id) {
    const resolvedId = Number(guestCustomerCandidate.value.id)
    if (resolvedId) {
      try { localStorage.setItem(POS_GUEST_CACHE_KEY, String(resolvedId)) } catch { /* ignore */ }
      return resolvedId
    }
  }

  const cachedId = Number(localStorage.getItem(POS_GUEST_CACHE_KEY) || 0)
  if (cachedId > 0) {
    return cachedId
  }

  if (Number(detectedGuestCustomerId.value || 0) > 0) {
    return Number(detectedGuestCustomerId.value)
  }

  if (POS_GUEST_ENV_ID > 0) {
    try { localStorage.setItem(POS_GUEST_CACHE_KEY, String(POS_GUEST_ENV_ID)) } catch { /* ignore */ }
    return POS_GUEST_ENV_ID
  }

  // Do not fallback to a real customer id (e.g. #1) because it will attach
  // guest orders to real customer profiles.
  return null
}

async function ensurePosGuestCustomerId() {
  const existingId = resolveFallbackCustomerId()
  if (existingId) return existingId

  const timestamp = Date.now().toString()
  const generatedPhone = `09${timestamp.slice(-8)}`
  const generatedEmail = `pos.guest.${timestamp}@dirtywave.local`
  const generatedPassword = "DW@123456"
  let accountId = 0

  const accountPayloadCandidates = [
    {
      email: generatedEmail,
      username: generatedEmail,
      matKhau: generatedPassword,
      vaiTro: "CUSTOMER",
      trangThaiHoatDong: "Hoạt động",
      trangThaiTaiKhoan: "Kích hoạt"
    },
    {
      email: generatedEmail,
      tenDangNhap: generatedEmail,
      matKhau: generatedPassword,
      vaiTro: "ROLE_CUSTOMER",
      trangThaiHoatDong: "Hoạt động",
      trangThaiTaiKhoan: "Kích hoạt"
    },
    {
      email: generatedEmail,
      username: generatedEmail,
      password: generatedPassword,
      vaiTro: "CUSTOMER",
      trangThaiHoatDong: "Hoạt động",
      trangThaiTaiKhoan: "Kích hoạt"
    }
  ]

  for (const accountPayload of accountPayloadCandidates) {
    try {
      const accountRes = await taiKhoanService.create(accountPayload)
      accountId = Number(accountRes?.data?.id || accountRes?.data?.data?.id || 0)
      if (accountId > 0) break
    } catch {
      // Try next payload shape.
    }
  }

  if (accountId > 0) {
    const customerPayloadCandidates = [
      {
        tenKhachHang: "Khách lẻ",
        gioiTinh: "Nam",
        ngaySinh: "1990-01-01",
        soDienThoai: generatedPhone,
        email: generatedEmail,
        diaChiNhanHang: "Mua tại quầy",
        idTaiKhoan: accountId,
        taiKhoan: { id: accountId },
        trangThai: "Hoạt động"
      },
      {
        tenKhachHang: "Khách lẻ",
        soDienThoai: generatedPhone,
        idTaiKhoan: accountId,
        taiKhoanId: accountId,
        trangThai: "Hoạt động"
      }
    ]

    for (const payload of customerPayloadCandidates) {
      try {
        const createRes = await createKhachHang(payload)
        const createdId = Number(
          createRes?.data?.id ||
          createRes?.data?.khachHang?.id ||
          createRes?.data?.data?.id ||
          0
        )
        if (createdId > 0) {
          try { localStorage.setItem(POS_GUEST_CACHE_KEY, String(createdId)) } catch { /* ignore */ }
          detectedGuestCustomerId.value = createdId
          return createdId
        }
      } catch {
        // Try next payload shape.
      }
    }
  }

  const detectedId = await detectGuestCustomerIdAcrossPages()
  if (Number(detectedId || 0) > 0) {
    return Number(detectedId)
  }

  return null
}

const defaultStatusCode = computed(() => "CHO_LAY_HANG")

const selectedAddress = computed(() => {
  if (selectedAddressId.value === "manual") return null
  return customerAddresses.value.find((a) => Number(a.id) === Number(selectedAddressId.value)) || null
})

const hasManualAddressInput = computed(() => {
  const a = manualAddress.value || {}
  return [a.soDienThoai, a.diaChiCuThe, a.tinhThanh, a.quanHuyen, a.phuongXa].some((v) => String(v || "").trim())
})

const useManualAddress = computed(() => {
  if (taiQuay.value) return false
  if (selectedAddress.value) return false
  if (selectedAddressId.value === "manual") return true
  if (!customerAddresses.value.length) return true
  return hasManualAddressInput.value
})

const shippingAddressText = computed(() => {
  if (taiQuay.value) return "Mua tại quầy"
  if (selectedAddress.value) {
    return [selectedAddress.value.diaChiCuThe, selectedAddress.value.phuongXa, selectedAddress.value.quanHuyen, selectedAddress.value.tinhThanh].filter(Boolean).join(", ")
  }
  if (useManualAddress.value) {
    return [manualAddress.value.diaChiCuThe, manualAddress.value.phuongXa, manualAddress.value.quanHuyen, manualAddress.value.tinhThanh].filter(Boolean).join(", ")
  }
  return ""
})

const shippingPhone = computed(() => {
  if (taiQuay.value) return ""
  if (selectedAddress.value) return selectedAddress.value.soDienThoai || ""
  if (useManualAddress.value) return manualAddress.value.soDienThoai || ""
  return selectedCustomerInfo.value?.soDienThoai || ""
})

// Shipping address parts for GHN quote
const shippingProvince = computed(() => {
  if (taiQuay.value) return ""
  if (selectedAddress.value) return selectedAddress.value.tinhThanh || ""
  if (useManualAddress.value) return manualAddress.value.tinhThanh || ""
  return ""
})
const shippingDistrict = computed(() => {
  if (taiQuay.value) return ""
  if (selectedAddress.value) return selectedAddress.value.quanHuyen || ""
  if (useManualAddress.value) return manualAddress.value.quanHuyen || ""
  return ""
})
const shippingWard = computed(() => {
  if (taiQuay.value) return ""
  if (selectedAddress.value) return selectedAddress.value.phuongXa || ""
  if (useManualAddress.value) return manualAddress.value.phuongXa || ""
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
  if (!newId) {
    if (!taiQuay.value) selectedAddressId.value = "manual"
    return
  }
  try {
    const res = await getDiaChiByKhachHang(newId)
    const raw = Array.isArray(res?.data) ? res.data : []
    const addrKey = a => [a.diaChiCuThe, a.phuongXa, a.quanHuyen, a.tinhThanh].filter(Boolean).join('|')
    customerAddresses.value = [...new Map(raw.map(a => [addrKey(a), a])).values()]
    if (customerAddresses.value.length) {
      selectedAddressId.value = Number(customerAddresses.value[0].id)
    } else {
      selectedAddressId.value = "manual"
    }
  } catch {
    selectedAddressId.value = "manual"
  }
})

// Load provinces when switching to delivery mode
watch(taiQuay, (val) => {
  if (!val) {
    loadProvinces()
    if (!selectedAddress.value && !customerAddresses.value.length) {
      selectedAddressId.value = "manual"
    }
  }
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

// ═══════ Cart actions ═══════════════════════════════════════đ═══════════════════════════════════════đ════════════════════════════════đ—đ—đ

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
  window.toast?.success?.(`Đã thêm ${variant.tenSanPham}`)
}

function getLineMaxQty(line) {
  const currentQty = Number(line?.soLuong || 0)
  return currentQty + availableStockForVariant(line)
}

function incQty(line) {
  const maxQty = getLineMaxQty(line)
  if (line.soLuong >= maxQty) {
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
  const maxQty = getLineMaxQty(line)
  if (num <= 0) {
    line.soLuong = 1
    window.toast?.warning?.("Số lượng phải lớn hơn 0")
    return
  }
  if (num > maxQty) {
    line.soLuong = maxQty
    window.toast?.warning?.("Số lượng không được vượt tồn kho")
    return
  }
  line.soLuong = num
}

function removeLine(index) {
  lines.value.splice(index, 1)
}

const applyDiscount = (amount) => { discount.value = Number(amount || 0) }

const onImgError = (e) => {
  const img = e?.target
  if (!img) return
  if (img.dataset.fallbackApplied === "1") {
    img.onerror = null
    return
  }
  img.dataset.fallbackApplied = "1"
  img.src = logoFallback
}

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
  // Check if customer is inactive
  if (selectedCustomerInfo.value && selectedCustomerInfo.value.trangThai && selectedCustomerInfo.value.trangThai !== 'Hoạt động') {
    window.toast?.error?.("Khách hàng này đã ngừng hoạt động. Không thể thanh toán.")
    return
  }

  // For BANK (chuyển khoản) skip the paymentRemaining check — QR will be shown after order creation
  if (paymentMethod.value !== 'BANK' && paymentRemaining.value > 0) {
    window.toast?.warning?.("Số tiền nhập chưa đủ"); return
  }
  submitPosOrder()
}

// ═══════ Quick customer ═══════════════════════════════════════đ═══════════════════════════════════════đ════════════════════════════════đ

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
    if (isGuestLikeCustomer(kh)) return false
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
  return khachHangList.value
    .filter((kh) => !isGuestLikeCustomer(kh))
    .some((kh) => canonicalPhone(kh?.soDienThoai) === keyword)
})

const openCustomerSearchModal = async () => {
  showCustomerSearchModal.value = true
  customerPhoneSearch.value = selectedCustomerPhone.value || ""
  await nextTick()
  customerSearchInputRef.value?.focus?.()
}

const closeCustomerSearchModal = () => {
  showCustomerSearchModal.value = false
}

const selectCustomerFromSearch = (customer) => {
  customerId.value = Number(customer?.id) || null
  customerPhoneSearch.value = ""
  customerDraftName.value = String(customer?.tenKhachHang || "")
  closeCustomerSearchModal()
}

const selectFirstCustomerFromSearch = () => {
  const first = customerSearchResults.value?.[0]
  if (first) selectCustomerFromSearch(first)
}

const chooseGuestCustomer = async () => {
  customerId.value = null
  customerAddresses.value = []
  selectedAddressId.value = "manual"
  customerPhoneSearch.value = ""
  closeCustomerSearchModal()

  const guestId = await ensurePosGuestCustomerId()
  if (Number(guestId || 0) > 0) {
    customerId.value = Number(guestId)
    window.toast?.success?.("Đã chọn khách lẻ")
    return
  }

  window.toast?.error?.("Chưa thể tạo khách lẻ tự động. Vui lòng thử lại hoặc chọn khách hàng cụ thể.")
}

// ═══════ Employee context ═══════════════════════════════════════đ═══════════════════════════════════════đ════════════════════đ═══════—đ

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

// ═══════ Submit POS order ═══════════════════════════════════════đ═══════════════════════════════════════đ════════════════════đ═══════—đ

const submitPosOrder = async () => {
  if (!cashierId.value) { window.toast?.warning?.("Vui lòng chọn nhân viên bán hàng"); return }
  if (!lines.value.length) { window.toast?.warning?.("Chưa có sản phẩm trong đơn"); return }
  if (!taiQuay.value && !shippingAddressText.value) { window.toast?.warning?.("Vui lòng chọn địa chỉ giao hàng"); return }

  const selectedCustomer = khachHangList.value.find((kh) => Number(kh.id) === Number(customerId.value)) ?? null
  const orderType = taiQuay.value ? "POS" : "ONLINE"
  const addressText = shippingAddressText.value || "Mua tại quầy"
  const phoneRaw = shippingPhone.value || selectedCustomer?.soDienThoai || ""
  const phone = canonicalPhone(phoneRaw)

  if (!taiQuay.value && useManualAddress.value) {
    const hasAllAddressParts = Boolean(
      String(manualAddress.value.diaChiCuThe || "").trim() &&
      String(manualAddress.value.tinhThanh || "").trim() &&
      String(manualAddress.value.quanHuyen || "").trim()
    )
    if (!hasAllAddressParts) {
      window.toast?.warning?.("Vui lòng nhập số nhà, tỉnh và quận/huyện (phường/xã có thể bỏ trống)")
      return
    }
    if (!phone || !/^0\d{8,10}$/.test(phone)) {
      window.toast?.warning?.("Số điện thoại nhận hàng không hợp lệ")
      return
    }
  }

  const invalidQtyLines = lines.value.filter((l) => Number(l.soLuong || 0) <= 0)
  if (invalidQtyLines.length) {
    window.toast?.error?.("Số lượng sản phẩm phải lớn hơn 0")
    return
  }

  saving.value = true
  try {
    let effectiveCustomerId = Number(selectedCustomer?.id || 0) || null
    if (!effectiveCustomerId) {
      effectiveCustomerId = resolveFallbackCustomerId()
    }
    if (!effectiveCustomerId) {
      const createdGuestId = await ensurePosGuestCustomerId()
      if (createdGuestId) {
        effectiveCustomerId = Number(createdGuestId)
        customerId.value = Number(createdGuestId)
        window.toast?.success?.("Đã tự tạo khách lẻ mặc định cho đơn tại quầy.")
      }
    }
    if (!effectiveCustomerId) {
      window.toast?.error?.("Backend hiện yêu cầu khách hàng bắt buộc. Vui lòng chọn khách hàng trước khi thanh toán.")
      saving.value = false
      return
    }

    const effectiveCustomer = khachHangList.value.find((kh) => Number(kh?.id) === Number(effectiveCustomerId)) || selectedCustomer
    const shipCost = taiQuay.value ? 0 : Number(shippingFee.value || 0)
    const apiPayMethod = resolveApiPaymentMethod(paymentMethod.value)
    const orderStatusCode = orderType === "POS"
      ? "CHO_LAY_HANG"
      : (paymentMethod.value.toUpperCase() === "VNPAY" ? "CHO_LAY_HANG" : "CHO_XAC_NHAN")
    const voucherDescriptor = String(
      selectedVoucher.value?.maPhieuGiamGia
      || selectedVoucher.value?.tenPhieuGiamGia
      || ""
    ).trim()
    const statusNote = [
      `[${orderType}] ${orderNote.value || "Đơn bán hàng"}`,
      voucherDescriptor ? `Áp dụng voucher ${voucherDescriptor}` : ""
    ].filter(Boolean).join(" | ")
    const normalizedPhone = phone || canonicalPhone(effectiveCustomer?.soDienThoai || "")

    const discountAmount = Number(discount.value || 0)
    const createPayload = {
      nhanVienId: Number(cashierId.value),
      idNhanVien: Number(cashierId.value),
      soDienThoaiNhanHang: normalizedPhone,
      diaChiNhanHang: addressText,
      phiShip: shipCost,
      giaSauGiamGia: discountAmount,
      giamGia: discountAmount,
      discountAmount,
      thanhTien: Number(grandTotal.value || 0),
      phuongThucThanhToan: apiPayMethod,
      paymentMethod: apiPayMethod,
      orderStatusCode,
      statusNote,
      orderType
    }
    if (effectiveCustomerId) {
      createPayload.khachHangId = Number(effectiveCustomerId)
      createPayload.idKhachHang = Number(effectiveCustomerId)
    }

    // Validate tồn kho lần cuối trước khi gửi API
    for (const line of lines.value) {
      const stockCap = Number(line.soLuongTon || 0)
      const qty = Number(line.soLuong || 0)
      if (stockCap > 0 && qty > stockCap) {
        window.toast?.error?.(`Sản phẩm "${line.tenSanPham || line.maSanPhamChiTiet}" vượt tồn kho (${qty} > ${stockCap})`)
        saving.value = false
        return
      }
    }

    console.log('[POS] createPayload:', JSON.stringify(createPayload))
    const createRes = await createHoaDon(createPayload)
    console.log('[POS] createRes:', JSON.stringify(createRes?.data))
    const orderId = createRes?.data?.hoaDon?.id ?? createRes?.data?.id
    if (!orderId) throw new Error("Không lấy được mã hóa đơn")

    const createdOrderSnapshot = {
      id: Number(orderId),
      maHoaDon: createRes?.data?.hoaDon?.maHoaDon ?? createRes?.data?.maHoaDon ?? ""
    }

    for (const line of lines.value) {
      await addHoaDonItem(orderId, {
        spctId: line.spctId,
        sanPhamChiTietId: line.spctId,
        idSanPhamChiTiet: line.spctId,
        soLuong: Number(line.soLuong),
        quantity: Number(line.soLuong),
        giaBan: Number(line.giaBan),
        donGia: Number(line.giaBan),
        giaSauGiam: Number(line.giaBan),
        giaBanSauDotGiamGia: Number(line.giaBan),
        giaBanGoc: Number(line.giaBanGoc || line.giaBan || 0),
        thanhTien: Number(line.giaBan || 0) * Number(line.soLuong || 0)
      })
    }

    persistOrderPricingSnapshot(createdOrderSnapshot, lines.value)
    persistOrderVoucherSnapshot({
      keys: [createdOrderSnapshot.id, createdOrderSnapshot.maHoaDon],
      itemVouchers: []
    })

    const isVnpay = paymentMethod.value.toUpperCase() === "VNPAY"
    const isBank = paymentMethod.value.toUpperCase() === "BANK"

    // For BANK (chuyển khoản): show QR payment modal instead of completing POS
    if (isBank) {
      const maHoaDon = createRes?.data?.hoaDon?.maHoaDon ?? createRes?.data?.maHoaDon ?? `HD${orderId}`
      openQrPayment(orderId, grandTotal.value, maHoaDon, taiQuay.value)
    } else if (taiQuay.value && !isVnpay) {
      try {
        const trackingUrl = buildOrderLookupTrackingUrl({ maHoaDon: createRes?.data?.hoaDon?.maHoaDon || createRes?.data?.maHoaDon })
        await updateHoaDonBySystemEvent(orderId, "HOAN_TAT_POS", "Hoàn tất bán hàng tại quầy", trackingUrl)
        window.toast?.success?.("Tạo đơn thành công")
      } catch {
        window.toast?.warning?.("Vui lòng mở lại hóa đơn để kiểm tra và hoàn tất")
      }
      applyLocalStockAfterCheckout(lines.value)
    } else if (!taiQuay.value) {
      window.toast?.success?.("Tạo đơn giao hàng thành công")
    } else {
      window.toast?.success?.("Tạo đơn thành công — chờ khách xác nhận VNPay")
    }

    // Remove current tab after successful order (skip for BANK — tab removed after QR confirms)
    if (!isBank) {
      if (posTabs.value.length > 1) {
        posTabs.value.splice(activeTabIndex.value, 1)
        if (activeTabIndex.value >= posTabs.value.length) activeTabIndex.value = posTabs.value.length - 1
      } else {
        posTabs.value = [null]
        activeTabIndex.value = 0
      }
      applyTabState(posTabs.value[activeTabIndex.value] || null)
      persistPosDraft()
      router.push(`${panelBasePath.value}/hoa-don/detail/${orderId}`)
    }
  } catch (error) {
    console.error('[POS] submitPosOrder error:', error?.response?.status, JSON.stringify(error?.response?.data), error.message)
    const responseData = error?.response?.data || {}
    const detailText = [
      responseData?.message,
      responseData?.error,
      responseData?.details,
      Array.isArray(responseData?.errors)
        ? responseData.errors.join("; ")
        : (responseData?.errors && typeof responseData.errors === "object"
          ? Object.values(responseData.errors).flat().join("; ")
          : "")
    ].filter(Boolean).join(" | ")
    window.toast?.error?.(detailText || error.message || "Không thể tạo đơn")
  } finally {
    saving.value = false
  }
}

// ═══════ Load data ═══════════════════════════════════════đ═══════════════════════════════════════đ═══════════════════════════════════════đ

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
    await detectGuestCustomerIdAcrossPages()
    const rawProducts = toList(spRes?.data)
    soldQtyBySpctSnapshot.value = soldBySpct
    const mergedProducts = mergeProductsForPos(rawProducts)
    const pricedProducts = await Promise.all(mergedProducts.map(async (product) => {
      const productId = Number(product?.id || 0)
      const rawVariants = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []
      if (!rawVariants.length || productId <= 0) return product

      return {
        ...product,
        sanPhamChiTiets: await applyCampaignPriceToVariants(rawVariants, productId)
      }
    }))
    variants.value = flattenVariants(pricedProducts, soldBySpct)

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

onMounted(() => {
  loadData()
  loadProvinces()
  // Suppress parent .content overflow to prevent double scrollbars on POS page
  const content = document.querySelector('.content')
  if (content) content.style.overflow = 'hidden'
})
onBeforeUnmount(() => {
  clearQrPolling()
  stopPanelResize()
  persistPosDraft()
  // Restore parent .content overflow for other pages
  const content = document.querySelector('.content')
  if (content) content.style.overflow = ''
})
</script>

<template>
  <main class="pos-page">
    <div v-if="loading" class="pos-loading">Đang tải dữ liệu bán hàng...</div>

    <div v-else ref="posLayoutRef" class="pos-layout" :class="{ 'is-resizing': isResizingPanels }" :style="{ '--pos-left-width': `${posLeftWidthPercent}%` }">
      <!-- ═══ LEFT: Trainify-style product grid ═══ -->
      <div class="pos-left">
        <div class="pos-left-header">
          <div class="pos-left-title">
            <ShoppingBag :size="18" />
            Thêm sản phẩm
          </div>
          <div class="pos-search-bar">
            <Search :size="15" class="pos-search-icon" />
            <input v-model="posProductSearch" type="text" placeholder="Tìm sản phẩm, mã, màu, size..." />
          </div>
        </div>
        <div class="pos-product-grid">
          <div
            v-for="group in productGroups"
            :key="group.idSanPham || group.tenSanPham"
            class="pos-product-card"
            :class="{ 'is-out': group.totalStock <= 0 }"
            @click="group.totalStock > 0 && handleProductClick(group)"
          >
            <div class="pos-product-img">
              <img :src="group.image || logoFallback" :alt="normalizeDisplayName(group.tenSanPham)" @error="onImgError" />
              <div class="pos-product-img-overlay">
                <div class="pos-product-hover-cta">
                  <span class="pos-product-hover-icon"><Plus :size="18" /></span>
                  <span class="pos-product-hover-text">Thêm vào hóa đơn</span>
                </div>
              </div>
            </div>
            <div class="pos-product-info">
              <div class="pos-product-name">{{ normalizeDisplayName(group.tenSanPham) }}</div>
              <div class="pos-product-code">{{ group.maSanPham }}</div>
              <div class="pos-product-price-row">
                <div class="pos-product-price-main">
                  <div class="pos-product-price">
                    {{ formatCurrency(group.minPrice) }}
                  </div>
                  <div class="pos-product-old-price" :class="{ 'is-placeholder': !group.hasDiscount }">
                    <template v-if="group.hasDiscount">
                      {{ formatCurrency(group.minBasePrice) }}
                    </template>
                    <template v-else>0 đ</template>
                  </div>
                </div>
                <div class="pos-product-discount-slot">
                  <span v-if="group.hasDiscount" class="pos-product-discount-pill">{{ group.campaignLabel }}</span>
                  <span v-else class="pos-product-discount-pill is-placeholder">Giảm giá</span>
                </div>
              </div>
            </div>
          </div>
          <div v-if="!productGroups.length" class="pos-empty-products">Không tìm thấy sản phẩm</div>
        </div>
      </div>

      <div
        class="pos-resize-gutter"
        title="Kéo để thu / mở rộng"
        @mousedown.prevent="startPanelResize"
      >
        <span class="pos-resize-handle"></span>
      </div>

      <!-- ═══ RIGHT: Invoice Panel ═══ -->
      <div class="pos-right">
        <!-- Invoice header with tabs -->
        <div class="pos-invoice-header">
          <div class="pos-invoice-title">
            <span class="material-icons-outlined" style="font-size:18px">receipt_long</span>
            Hoá đơn
          </div>
          <div class="pos-tabs">
            <button
              v-for="(tab, idx) in posTabs" :key="idx"
              class="pos-tab" :class="{ active: idx === activeTabIndex }"
              @click="switchPosTab(idx)"
            >
              #{{ idx + 1 }}
              <span v-if="posTabs.length > 1" class="pos-tab-x" @click.stop="removePosTab(idx)">&times;</span>
            </button>
            <button v-if="posTabs.length < MAX_POS_TABS" class="pos-tab pos-tab-add" @click="addPosTab()">+</button>
          </div>
          <div class="pos-invoice-actions">
            <div class="pos-order-mode" role="group" aria-label="Chế độ đơn hàng">
              <button type="button" :class="{ active: taiQuay }" @click="taiQuay = true">Tại quầy</button>
              <button type="button" :class="{ active: !taiQuay }" @click="taiQuay = false">Giao hàng</button>
            </div>
          </div>
        </div>

        <!-- Scrollable content area -->
        <div class="pos-right-scroll">
        <!-- Customer section -->
        <div class="pos-section pos-customer-section">
          <div class="pos-section-head">
            <span class="material-icons-outlined" style="font-size:16px">person</span>
            KHÁCH HÀNG
          </div>
          <div class="pos-customer-row">
            <input
              type="text"
              class="pos-input"
              placeholder="Tên hoặc SĐT khách hàng..."
              :value="selectedCustomerLabel !== 'Khách lẻ' ? selectedCustomerLabel : ''"
              readonly
              @click="openCustomerSearchModal"
            />
            <button class="pos-btn-outline pos-btn-sm" type="button" @click="chooseGuestCustomer">
              <span class="material-icons-outlined" style="font-size:14px">person_outline</span>
              Khách lẻ
            </button>
          </div>
          <div class="pos-cashier-row">
            <select class="pos-select pos-select-sm" v-model.number="cashierId" :disabled="isEmployeePanel">
              <option :value="null">Chọn Nhân viên bán hàng</option>
              <option v-for="nv in nhanVienList" :key="nv.id" :value="Number(nv.id)">
                {{ nv.tenNhanVien || `Nhân viên #${nv.id}` }}
              </option>
            </select>
          </div>
        </div>

        <!-- Cart items -->
        <div class="pos-cart-section" :class="{ 'is-scrollable': lines.length > 6 }">
          <div v-if="!lines.length" class="pos-cart-empty">
            <ShoppingBag :size="28" />
            <p>Chưa có sản phẩm</p>
            <small>Chọn sản phẩm từ danh sách bên trái</small>
          </div>

          <div v-for="(line, idx) in lines" :key="line.spctId" class="pos-cart-item">
            <div class="pos-cart-item-img">
              <img :src="line.image || logoFallback" :alt="normalizeDisplayName(line.tenSanPham)" @error="onImgError" />
            </div>
            <div class="pos-cart-item-body">
              <div class="pos-cart-item-name">{{ normalizeDisplayName(line.tenSanPham) }}</div>
              <div class="pos-cart-item-meta">
                <span>{{ line.maSanPhamChiTiet }}</span>
                <span v-if="line.tenMau">{{ line.tenMau }}</span>
                <span v-if="line.tenSize">{{ line.tenSize }}</span>
              </div>
            </div>
            <div class="pos-cart-item-right">
              <div class="pos-qty">
                <button @click="decQty(line)" :disabled="line.soLuong <= 1">−</button>
                <input type="number" :value="line.soLuong" min="1" @change="setQty(line, $event.target.value)" />
                <button @click="incQty(line)" :disabled="line.soLuong >= getLineMaxQty(line)">+</button>
              </div>
              <div class="pos-cart-price-stack">
                <div class="pos-cart-item-total">
                  {{ formatCurrency(line.giaBan * line.soLuong) }}
                  <span v-if="Number(line.giaBanGoc || 0) > Number(line.giaBan || 0)" class="pos-cart-item-old-price">{{ formatCurrency(line.giaBanGoc * line.soLuong) }}</span>
                </div>
                <div v-if="Number(line.campaignPercent || 0) > 0" class="pos-cart-discount-badge">
                  {{ formatCampaignLabel(line.campaignName, line.campaignPercent) }}
                </div>
              </div>
              <button class="pos-cart-item-remove" @click="removeLine(idx)"><X :size="16" /></button>
            </div>
          </div>
        </div>

        <!-- Discount -->
        <div class="pos-section">
          <div class="pos-section-head">
            <span class="material-icons-outlined" style="font-size:16px">local_offer</span>
            MÃ GIẢM GIÁ
          </div>
          <VoucherSelector
            :subtotal="subtotal"
            :customer-id="customerId"
            :auto-select="true"
            :initial-voucher-code="selectedVoucher?.maPhieuGiamGia || ''"
            :initial-voucher-name="selectedVoucher?.tenPhieuGiamGia || ''"
            @update:voucher="selectedVoucher = $event"
            @discount-changed="applyDiscount"
          />
        </div>

        <!-- Note -->
        <div class="pos-section">
          <div class="pos-section-head">
            <span class="material-icons-outlined" style="font-size:16px">edit_note</span>
            GHI CHÚ
          </div>
          <input class="pos-input" v-model="orderNote" type="text" placeholder="Ghi chú đơn hàng..." />
        </div>

        <!-- Address (delivery mode) -->
        <template v-if="!taiQuay">
          <div class="pos-section">
            <div class="pos-section-head">
              <MapPin :size="14" /> ĐỊA CHỈ GIAO HÀNG
            </div>
            <select v-if="customerAddresses.length" class="pos-select" v-model="selectedAddressId">
              <option v-for="addr in customerAddresses" :key="addr.id" :value="Number(addr.id)">
                {{ [addr.diaChiCuThe, addr.phuongXa, addr.quanHuyen, addr.tinhThanh].filter(Boolean).join(', ') }}
              </option>
              <option value="manual">Nhập địa chỉ mới</option>
            </select>
            <div v-if="!customerAddresses.length || selectedAddressId === 'manual'" class="pos-address-form">
              <input v-model="manualAddress.soDienThoai" type="text" inputmode="numeric" placeholder="SĐT nhận hàng" class="pos-input" @input="onManualPhoneInput" />
              <input v-model="manualAddress.diaChiCuThe" type="text" placeholder="Số nhà, tên đường..." class="pos-input" />
              <select v-model="manualAddress.tinhThanh" class="pos-select" @change="onManualProvinceChange" @focus="loadProvinces">
                <option value="">Tỉnh / Thành phố</option>
                <option v-for="p in provinces" :key="p.code" :value="p.name">{{ p.name }}</option>
              </select>
              <select v-model="manualAddress.quanHuyen" class="pos-select" :disabled="!manualAddress.tinhThanh" @change="onManualDistrictChange" @focus="onManualDistrictFocus">
                <option value="">Quận / Huyện</option>
                <option v-for="d in districts" :key="d.code" :value="d.name">{{ d.name }}</option>
              </select>
              <select v-model="manualAddress.phuongXa" class="pos-select" :disabled="!manualAddress.quanHuyen" @focus="onManualWardFocus">
                <option value="">Phường / Xã</option>
                <option v-for="w in wards" :key="w.code" :value="w.name">{{ w.name }}</option>
              </select>
            </div>
            <div class="pos-shipping-row">
              <select class="pos-select pos-select-sm" v-model="shippingProvider">
                <option value="GHN">Giao Hàng Nhanh</option>
                <option value="GHTK">Giao Hàng Tiết Kiệm</option>
              </select>
              <span v-if="shippingLoading">Đang tính...</span>
              <strong v-else>{{ formatCurrency(shippingFee) }}</strong>
            </div>
          </div>
        </template>
        <!-- Summary + Payment (INLINE) -->
        <div class="pos-payment-section">
          <div class="pos-summary-row">
            <span>Tạm tính</span>
            <span>{{ formatCurrency(subtotal) }}</span>
          </div>
          <div v-if="discount > 0" class="pos-summary-row pos-discount-row">
            <span>Giảm giá{{ selectedVoucher ? ` (${selectedVoucher.tenPhieuGiamGia || ''})` : '' }}</span>
            <span>-{{ formatCurrency(discount) }}</span>
          </div>
          <div v-if="!taiQuay" class="pos-summary-row">
            <span>Phí ship</span>
            <span>{{ formatCurrency(shippingFee) }}</span>
          </div>
          <div class="pos-summary-row pos-total-row">
            <strong>Tổng cộng</strong>
            <strong class="pos-total-amount">{{ formatCurrency(grandTotal) }}</strong>
          </div>

          <!-- Payment method -->
          <div class="pos-pay-methods">
            <span class="pos-section-head" style="margin-bottom:6px">
              <span class="material-icons-outlined" style="font-size:16px">payments</span>
              PHƯƠNG THỨC THANH TOÁN
            </span>
            <div class="pos-pay-tabs">
              <button
                :class="{ active: paymentMethod === 'CASH' }"
                @click="paymentMethod = 'CASH'; applyPaymentMode('CASH')"
              >
                <span class="material-icons-outlined" style="font-size:15px">payments</span>
                Tiền mặt
              </button>
              <button
                :class="{ active: paymentMethod === 'BANK' }"
                @click="paymentMethod = 'BANK'; applyPaymentMode('BANK')"
              >
                <span class="material-icons-outlined" style="font-size:15px">account_balance</span>
                Chuyển khoản
              </button>
            </div>
          </div>

          <!-- Tiền khách đưa -->
          <div class="pos-cash-input" v-if="paymentMethod === 'CASH' || paymentMethod === 'BOTH'">
            <div class="pos-section-head" style="margin-bottom:4px">
              <span class="material-icons-outlined" style="font-size:16px">account_balance_wallet</span>
              TIỀN KHÁCH ĐƯA
            </div>
            <input
              type="number"
              class="pos-input pos-input-lg"
              v-model.number="paymentCash"
              min="0"
              placeholder="0"
            />
            <div class="pos-quick-amounts">
              <button
                v-for="amt in quickMoneyAmounts" :key="amt"
                type="button"
                @click="paymentCash = amt"
              >{{ formatCurrency(amt) }}</button>
            </div>
          </div>

          <!-- Tiền thừa -->
          <div class="pos-change-section" v-if="paymentMethod === 'CASH' || paymentMethod === 'BOTH'">
            <div class="pos-summary-row">
              <span>Tiền thừa</span>
              <strong class="pos-change-amount">{{ formatCurrency(paymentChange) }}</strong>
            </div>
          </div>

          <!-- Action buttons -->
          <div class="pos-action-btns">
            <button
              class="pos-btn-pay"
              type="button"
              :disabled="saving || !lines.length || (paymentMethod === 'CASH' && paymentRemaining > 0)"
              @click="confirmPayment"
            >
              <span class="material-icons-outlined" style="font-size:18px">check_circle</span>
              {{ saving ? 'Đang xử lý...' : 'Thanh toán' }}
            </button>
            <button
              class="pos-btn-cancel"
              type="button"
              @click="applyTabState(null)"
              :disabled="!lines.length"
            >
              <span class="material-icons-outlined" style="font-size:16px">delete_outline</span>
              Huỷ đơn
            </button>
          </div>
        </div>
        </div><!-- end pos-right-scroll -->
      </div>
    </div>

    <!-- ═══ Variant Picker Modal ═══ -->
    <Teleport to="body">
      <Transition name="bh-modal">
        <div v-if="showVariantPicker && variantPickerProduct" class="bh-modal-overlay" @click.self="showVariantPicker = false">
          <div class="pos-variant-modal">
            <div class="pos-variant-modal-head">
              <div class="pos-variant-head-main">
                <h3>{{ variantPickerProduct.tenSanPham }}</h3>
                <p>Chọn biến thể để thêm nhanh vào hóa đơn</p>
              </div>
              <div class="pos-variant-head-actions">
                <div class="pos-variant-head-count">{{ variantPickerProduct.variants.length }} biến thể</div>
                <button @click="showVariantPicker = false"><X :size="18" /></button>
              </div>
            </div>
            <div class="pos-variant-grid">
              <div
                v-for="(v, idx) in variantPickerProduct.variants"
                :key="v.spctId"
                class="pos-variant-item"
                :class="{ 'is-out': availableStockForVariant(v) <= 0 }"
                :style="{ animationDelay: `${idx * 45}ms` }"
                @click="availableStockForVariant(v) > 0 && pickVariant(v)"
              >
                <div class="pos-variant-media">
                  <img :src="v.image || logoFallback" :alt="v.tenSanPham" @error="onImgError" />
                </div>
                <div class="pos-variant-details">
                  <div class="pos-variant-topline">
                    <span class="pos-variant-code">{{ v.maSanPhamChiTiet || v.maSanPham || `SPCT${v.spctId}` }}</span>
                    <span class="pos-variant-stock" :class="{ 'is-low': availableStockForVariant(v) <= 5 }">Còn {{ availableStockForVariant(v) }}</span>
                  </div>
                  <div class="pos-variant-attrs">
                    <span v-if="v.tenMau" class="pos-variant-pill pos-variant-pill-color">Màu: <b>{{ v.tenMau }}</b></span>
                    <span class="pos-variant-pill pos-variant-pill-size" :class="{ 'solo': !v.tenMau }">Size: <b>{{ v.tenSize || 'Free size' }}</b></span>
                  </div>
                  <div class="pos-variant-price-line">
                    <div class="pos-variant-price">{{ formatCurrency(v.giaBan) }}</div>
                    <div v-if="Number(v.giaBanGoc || 0) > Number(v.giaBan || 0)" class="pos-variant-old-price">{{ formatCurrency(v.giaBanGoc) }}</div>
                  </div>
                  <div class="pos-variant-action">Chạm để thêm vào hóa đơn</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ Product Select Modal ═══ -->
    <Teleport to="body">
      <Transition name="bh-modal">
        <div v-if="showProductModal" class="bh-modal-overlay" @click.self="showProductModal = false">
          <div class="bh-modal-box bh-product-modal">
            <div class="bh-modal-head bh-product-modal-head">
              <div>
                <h2>Thêm sản phẩm</h2>
                <p>Chọn trong form thay vì hiển thị hết ngoài màn hình chính.</p>
              </div>
              <button class="bh-modal-close" @click="showProductModal = false"><X :size="20" /></button>
            </div>
            <div class="bh-modal-search bh-product-search">
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
                    <div class="bh-modal-item-stock" :class="{ 'is-low': rawStockForVariant(variant) <= 5 }">Tồn: {{ rawStockForVariant(variant) }}</div>
                  </div>
                  <div class="bh-modal-item-name">{{ variant.tenSanPham }}</div>
                  <div class="bh-modal-item-variant-line">
                    <span class="bh-modal-item-variant-pill">Màu: <strong>{{ variant.tenMau || 'Mặc định' }}</strong></span>
                    <span class="bh-modal-item-variant-pill">Size: <strong>{{ variant.tenSize || 'Free size' }}</strong></span>
                  </div>
                  <div class="bh-modal-item-price">{{ formatCurrency(variant.giaBan) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ Customer Search Modal ═══ -->
    <Teleport to="body">
      <Transition name="bh-modal">
        <div
          v-if="showCustomerSearchModal"
          class="bh-modal-overlay"
          @click.self="closeCustomerSearchModal"
        >
          <div class="bh-modal-box bh-customer-modal">
            <div class="bh-modal-head bh-customer-modal-head">
              <h2>Tìm khách hàng</h2>
              <button class="bh-modal-close" @click="closeCustomerSearchModal"><X :size="20" /></button>
            </div>
            <div class="bh-modal-search">
              <Search :size="18" class="bh-modal-search-icon" />
              <input
                ref="customerSearchInputRef"
                v-model="customerPhoneSearch"
                type="text"
                placeholder="Nhập SĐT khách hàng..."
                @keydown.enter.prevent="selectFirstCustomerFromSearch"
              />
            </div>
            <div class="bh-customer-modal-body">
              <div class="bh-customer-table-wrap">
                <table v-if="customerSearchResults.length" class="bh-customer-table">
                  <thead><tr><th>Tên</th><th>SĐT</th><th class="right">Thao tác</th></tr></thead>
                  <tbody>
                    <tr v-for="kh in customerSearchResults" :key="kh.id" @dblclick="selectCustomerFromSearch(kh)">
                      <td>{{ kh.tenKhachHang || `KH #${kh.id}` }}</td>
                      <td>{{ kh.soDienThoai || '—' }}</td>
                      <td class="right">
                        <button class="pos-btn-outline pos-btn-sm" @click="selectCustomerFromSearch(kh)">Chọn</button>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <div v-else class="bh-modal-empty">Không tìm thấy</div>
              </div>

            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ QR Payment Modal ═══ -->
    <Teleport to="body">
      <Transition name="bh-modal">
        <div v-if="showQrModal" class="bh-modal-overlay" @click.self="cancelQrPayment">
          <div class="pos-qr-modal">
            <div class="pos-qr-header">
              <div class="pos-qr-header-main">
                <div class="pos-qr-badge">
                  <span class="material-icons-outlined">qr_code_2</span>
                </div>
                <div>
                  <div class="pos-qr-title">Thanh toán chuyển khoản</div>
                  <div class="pos-qr-sub">#{{ qrContent }}</div>
                </div>
              </div>
              <button class="pos-qr-close" type="button" @click="cancelQrPayment"><X :size="16" /></button>
            </div>
            <div class="pos-qr-body">
              <div class="pos-qr-amount">{{ formatCurrency(qrAmount) }}</div>
              <div class="pos-qr-img">
                <img :src="buildVietQrUrl(qrAmount, qrContent)" alt="VietQR" />
              </div>
              <div class="pos-qr-content">
                Nội dung CK: <strong>{{ qrContent }}</strong>
              </div>
              <div v-if="qrState === 'processing'" class="pos-qr-waiting">
                <span class="pos-qr-spinner"></span>
                Đang xác nhận thanh toán...
              </div>

              <p class="pos-qr-note">
                Sau khi nhận được tiền chuyển khoản, bấm nút bên dưới để xác nhận thanh toán.
              </p>

              <div class="pos-qr-actions">
                <button class="pos-btn-pay" style="width:100%" :disabled="qrPolling" @click="confirmQrDemoPayment">
                  {{ qrPolling ? 'Đang xác nhận thanh toán...' : 'Xác nhận đã thanh toán' }}
                </button>
                <button class="pos-btn-cancel" style="width:100%" @click="cancelQrPayment">
                  <X :size="14" /> Huỷ thanh toán
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ═══ QR Paid Detail Modal (separate, matches Customer Profile style) ═══ -->
    <Teleport to="body">
      <Transition name="bh-modal">
        <div v-if="showQrPaidModal && qrSummary" class="qr-paid-overlay" @click.self="closeQrPaidModal">
          <div class="qr-paid-modal">
            <div class="qr-paid-header">
              <div class="qr-paid-header-inner">
                <div class="qr-paid-check-icon">
                  <span class="material-icons-outlined">check_circle</span>
                </div>
                <div>
                  <div class="qr-paid-header-title">Thanh toán thành công</div>
                  <div class="qr-paid-header-code">#{{ qrSummary.maHoaDon }}</div>
                </div>
              </div>
              <button class="qr-paid-header-close" @click="closeQrPaidModal">✕</button>
            </div>
            <div class="qr-paid-body">
              <!-- Info grid -->
              <div class="qr-paid-info">
                <div class="qr-paid-info-item">
                  <span class="qr-paid-label">Trạng thái</span>
                  <span class="qr-paid-value"><span class="qr-paid-status-badge success">✓ Đã thanh toán</span></span>
                </div>
                <div class="qr-paid-info-item">
                  <span class="qr-paid-label">Phương thức</span>
                  <span class="qr-paid-value">Chuyển khoản</span>
                </div>
                <div class="qr-paid-info-item">
                  <span class="qr-paid-label">Khách hàng</span>
                  <span class="qr-paid-value">{{ qrSummary.customerName }}</span>
                </div>
                <div class="qr-paid-info-item">
                  <span class="qr-paid-label">Điện thoại</span>
                  <span class="qr-paid-value">{{ qrSummary.phone }}</span>
                </div>
                <div class="qr-paid-info-item full">
                  <span class="qr-paid-label">Địa chỉ</span>
                  <span class="qr-paid-value">{{ qrSummary.address }}</span>
                </div>
              </div>

              <!-- Product list -->
              <div class="qr-paid-products" v-if="qrSummary.items?.length">
                <h4>Sản phẩm</h4>
                <div class="qr-paid-product-list">
                  <div class="qr-paid-product-row" v-for="(item, idx) in qrSummary.items" :key="idx">
                    <div class="qr-paid-product-img">
                      <img
                        :src="item.image || fallbackImageFor(item.idSanPham, item.maSanPham, item.name)"
                        :alt="item.name"
                        @error="$event.target.src = fallbackImageFor(item.idSanPham, item.maSanPham, item.name) || logoFallback"
                      />
                    </div>
                    <div class="qr-paid-product-info">
                      <span class="qr-paid-product-name">{{ item.name }}</span>
                      <span class="qr-paid-product-meta" v-if="item.color || item.size">
                        <template v-if="item.color">Màu: {{ item.color }}</template>
                        <template v-if="item.color && item.size"> • </template>
                        <template v-if="item.size">Size: {{ item.size }}</template>
                      </span>
                      <span class="qr-paid-product-qty">SL: {{ item.qty }} × {{ formatCurrency(item.price) }}</span>
                    </div>
                    <div class="qr-paid-product-total">{{ formatCurrency(item.total) }}</div>
                  </div>
                </div>
              </div>

              <!-- Totals -->
              <div class="qr-paid-totals">
                <div class="qr-paid-total-line grand">
                  <span>Tổng cộng</span>
                  <strong>{{ formatCurrency(qrSummary.amount) }}</strong>
                </div>
              </div>

              <!-- Actions -->
              <div class="qr-paid-actions">
                <button class="qr-paid-detail-btn" @click="openQrOrderDetail">
                  <span class="material-icons-outlined" style="font-size:18px">open_in_new</span>
                  Xem chi tiết đầy đủ
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
/* PPP POS Page Layout PPP */
.pos-page {
  padding: 0;
  background: #eef1f6;
  margin: -8px -28px -28px;
  width: calc(100% + 56px);
  overflow: hidden;
  font-size: 16px;
  font-family: "Be Vietnam Pro", "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
  color: #0f172a;
}
.pos-loading { text-align: center; padding: 60px; color: #64748b; font-weight: 600; }

.pos-layout {
  display: grid;
  grid-template-columns: minmax(0, var(--pos-left-width, 58%)) 2px minmax(0, calc(100% - var(--pos-left-width, 58%)));
  gap: 12px;
  padding: 12px 16px;
  height: calc(100vh - 80px);
  max-height: calc(100vh - 80px);
  overflow: hidden;
  background: #eef1f6;
}

.pos-layout.is-resizing {
  user-select: none;
  cursor: col-resize;
}

.pos-resize-gutter {
  position: relative;
  width: 16px;
  background: transparent;
  cursor: col-resize;
  align-self: stretch;
  transition: background 0.2s ease;
  flex-shrink: 0;
}

.pos-resize-gutter::before {
  content: "";
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 2px;
  transform: translateX(-50%);
  background: #d1d5db;
  transition: background 0.2s ease;
  pointer-events: none;
}

.pos-resize-gutter:hover::before,
.pos-layout.is-resizing .pos-resize-gutter::before {
  background: #9ca3af;
}

.pos-resize-handle {
  display: none;
}

/* PPP Left Panel - Trainify product grid PPP */
.pos-left {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 1px 6px rgba(15, 23, 42, 0.06);
}
.pos-left-header {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 72px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  box-sizing: border-box;
  border-radius: 14px 14px 0 0;
}
.pos-left-title {
  display: flex;
  align-items: center;
  gap: 5px;
  font-weight: 800;
  font-size: 18px;
  color: #111;
  white-space: nowrap;
}
.pos-search-bar {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f3f4f6;
  border-radius: 10px;
  padding: 8px 12px;
}
.pos-search-bar input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font: inherit;
  font-size: 14px;
  color: #111;
}
.pos-search-icon { color: #9ca3af; flex-shrink: 0; }

/* Product Card Grid */
.pos-product-grid {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  align-content: start;
  background: #f8fafc;
  border-radius: 0 0 14px 14px;
}
.pos-product-card {
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  overflow: clip;
  cursor: pointer;
  transition: transform .15s ease, box-shadow .15s ease, border-color .15s ease;
  display: flex;
  flex-direction: column;
  box-shadow: none;
}
.pos-product-card:hover {
  transform: translateY(-1px);
  border-color: #94a3b8;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
}
.pos-product-card.is-out { opacity: .4; cursor: not-allowed; }
.pos-product-card.is-out:hover { transform: none; box-shadow: none; border-color: #e5e7eb; }
.pos-product-img {
  width: 100%;
  height: 308px;
  min-height: 308px;
  flex-shrink: 0;
  overflow: hidden;
  background: #e7edf2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  position: relative;
  box-sizing: border-box;
  border-bottom: 1px solid #e5e7eb;
}
.pos-product-img img {
  width: 100%;
  max-width: none;
  height: 100%;
  max-height: none;
  object-fit: cover;
  object-position: center 18%;
  display: block;
  border-radius: 0;
}
.pos-product-img-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.04), rgba(30, 41, 59, 0.05) 52%, rgba(239, 68, 68, 0.09));
  opacity: 0;
  transition: opacity .2s ease, background .2s ease;
  pointer-events: none;
}
.pos-product-card:hover .pos-product-img-overlay {
  opacity: 1;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.16), rgba(15, 23, 42, 0.3) 45%, rgba(220, 38, 38, 0.28));
}
.pos-product-hover-cta {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 9px;
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}
.pos-product-hover-icon {
  width: 46px;
  height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(12px) saturate(1.1);
  -webkit-backdrop-filter: blur(12px) saturate(1.1);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.2);
  opacity: 0;
  transform: translateY(2px) scale(0.98);
  transition: all .18s ease;
}
.pos-product-hover-text {
  opacity: 0;
  transform: translateY(5px);
  transition: all .18s ease;
}
.pos-product-card:hover .pos-product-hover-icon {
  opacity: 1;
  transform: translateY(0) scale(1);
}
.pos-product-card:hover .pos-product-hover-text {
  opacity: 1;
  transform: translateY(0);
}
.pos-stock-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  background: #bbf7d0;
  border: 1px solid #86efac;
  border-radius: 999px;
  padding: 2px 9px;
  font-size: 11px;
  font-weight: 800;
  color: #166534;
  line-height: 1.4;
}
.pos-stock-badge.is-low {
  background: #ffedd5;
  border-color: #fdba74;
  color: #c2410c;
}
.pos-product-info {
  padding: 12px 12px 12px;
  flex-shrink: 0;
  min-height: 94px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border-top: none;
}
.pos-product-name {
  font-weight: 800;
  font-size: 14px;
  color: #0f172a;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 2px;
}
.pos-product-code {
  font-size: 12.5px;
  color: #9ca3af;
  font-weight: 800;
}
.pos-product-attrs {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  margin-top: 2px;
}
.pos-attr-pill {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  border: 1px solid #cbd5e1;
  background: #edf1f5;
  color: #475569;
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.2;
}

.pos-product-price-row {
  margin-top: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-height: 34px;
}
.pos-product-price-main {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  flex-wrap: nowrap;
}
.pos-product-price {
  font-weight: 800;
  font-size: 18px;
  color: #0f172a;
  letter-spacing: -0.02em;
}
.pos-product-old-price {
  font-size: 13px;
  color: #94a3b8;
  text-decoration: line-through;
}
.pos-product-old-price.is-placeholder {
  visibility: hidden;
}
.pos-product-discount-slot {
  min-height: 17px;
  display: flex;
  align-items: flex-start;
}
.pos-product-discount-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 17px;
  padding: 2px 7px;
  border-radius: 9px;
  border: 1px solid #fecaca;
  background: #fff1f2;
  color: #dc2626;
  font-size: 11.5px;
  font-weight: 800;
  line-height: 1.2;
}
.pos-product-discount-pill.is-placeholder {
  visibility: hidden;
}
.pos-empty-products {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: #9ca3af;
  font-weight: 600;
  font-size: 14px;
}

/* PPP Right Panel PPP */
.pos-right {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 1px 6px rgba(15, 23, 42, 0.06);
  height: auto;
}
.pos-right-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding: 4px 14px 16px;
  background: #f8fafc;
  border-radius: 0 0 14px 14px;
}

/* Invoice header */
.pos-invoice-header {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 72px;
  padding: 10px 14px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  box-sizing: border-box;
  border-radius: 14px 14px 0 0;
}
.pos-invoice-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 800;
  font-size: 18px;
  color: #111;
  white-space: nowrap;
}
.pos-invoice-actions { margin-left: auto; }
.pos-invoice-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}
.pos-order-mode {
  display: inline-flex;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}
.pos-order-mode button {
  border: none;
  background: #fff;
  color: #64748b;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  padding: 6px 12px;
  cursor: pointer;
}
.pos-order-mode button + button {
  border-left: 1px solid #d1d5db;
}
.pos-order-mode button.active {
  background: #f1f5f9;
  color: #1e293b;
}
.pos-icon-btn {
  width: 32px; height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  display: grid;
  place-items: center;
  cursor: pointer;
}
.pos-icon-btn:hover { background: #f1f5f9; }

/* Tabs */
.pos-tabs {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
.pos-tab {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  font-size: 13px;
  font-weight: 700;
  color: #475569;
  cursor: pointer;
  transition: all .15s;
}
.pos-tab.active {
  background: #f1f5f9;
  color: #1e293b;
  border-color: #94a3b8;
}
.pos-tab-x {
  font-size: 14px;
  line-height: 1;
  color: #9ca3af;
  cursor: pointer;
}
.pos-tab-x:hover { color: #475569; }
.pos-tab-add { border-style: dashed; color: #475569; font-weight: 800; }

/* Sections — card style like Trainify */
.pos-section {
  padding: 16px 18px;
  background: #fff;
  border-radius: 12px;
  margin-top: 12px;
  border: 1px solid #ececec;
  box-shadow: 0 1px 4px rgba(15,23,42,0.04);
}
.pos-customer-section {
  padding: 16px 18px 18px;
  border-color: #e2e8f0;
  box-shadow: none;
}
.pos-section-head {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12.5px;
  font-weight: 800;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: .04em;
  margin-bottom: 12px;
}

/* Customer row */
.pos-customer-row {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}
.pos-customer-row .pos-input { flex: 1; }
.pos-cashier-row {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  background: transparent;
  border: none;
  border-radius: 0;
  padding: 0;
}
.pos-cashier-row::before {
  content: 'Nhân viên tạo đơn:';
  font-size: 11px;
  font-weight: 700;
  color: #475569;
  white-space: nowrap;
  line-height: 1.2;
}
.pos-cashier-row .pos-select-sm {
  flex: 0 0 auto;
  width: min(100%, 280px);
  border-color: #cbd5e1;
  background: #fff;
  min-height: 36px;
}

/* Inputs */
.pos-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font: inherit;
  font-size: 14px;
  color: #111;
  background: #fff;
  box-sizing: border-box;
}
.pos-input:focus { outline: none; border-color: #475569; box-shadow: 0 0 0 2px rgba(71,85,105,.12); }
.pos-input-lg { font-size: 15px; padding: 9px 12px; font-weight: 700; }

.pos-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font: inherit;
  font-size: 14px;
  color: #111;
  background: #fff;
  box-sizing: border-box;
}
.pos-select:focus { outline: none; border-color: #475569; }
.pos-select-sm { padding: 5px 8px; font-size: 12px; }

/* Buttons */
.pos-btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  padding: 7px 12px;
  cursor: pointer;
  white-space: nowrap;
}
.pos-btn-outline:hover { background: #f9fafb; border-color: #9ca3af; }
.pos-btn-sm { padding: 5px 10px; font-size: 12px; }

/* PPP Cart — card style PPP */
.pos-cart-section {
  background: #fff;
  border-radius: 12px;
  margin-top: 8px;
  border: 1px solid #ececec;
    box-shadow: 0 1px 4px rgba(15,23,42,0.04);
  overflow: hidden;
}
.pos-cart-section.is-scrollable {
  max-height: 560px;
  overflow-y: auto;
}
.pos-cart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 32px 16px;
  color: #9ca3af;
}
.pos-cart-empty p { margin: 0; font-size: 13px; font-weight: 600; }
.pos-cart-empty small { font-size: 11.5px; }

.pos-cart-item {
  display: flex;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #f0f0f0;
  align-items: center;
  transition: background .1s;
}
.pos-cart-item:last-child { border-bottom: none; }
.pos-cart-item:hover { background: #f8fafc; }
.pos-cart-item-img {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  overflow: hidden;
  background: #f3f4f6;
  flex-shrink: 0;
  border: 1px solid #e5e7eb;
  padding: 3px;
  box-sizing: border-box;
}
.pos-cart-item-img img { width: 100%; height: 100%; object-fit: contain; background: #f8fafc; border-radius: 6px; }
.pos-cart-item-body { flex: 1; min-width: 0; }
.pos-cart-item-name {
  font-weight: 700;
  font-size: 14.5px;
  color: #111;
  line-height: 1.35;
  white-space: normal;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.pos-cart-item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}
.pos-cart-item-old-price {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  text-decoration: line-through;
  font-weight: 600;
  margin-top: 1px;
  min-height: 14px;
}
.pos-cart-discount-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 6px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  font-size: 10.5px;
  font-weight: 700;
  white-space: nowrap;
}

.pos-cart-item-right {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-shrink: 0;
  margin-left: auto;
  padding-right: 8px;
}
.pos-cart-price-stack {
  order: 1;
  width: 168px;
  min-width: 168px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
}
.pos-cart-item-total {
  font-weight: 900;
  font-size: 16px;
  color: #111;
  text-align: right;
  line-height: 1.3;
  width: 100%;
  font-variant-numeric: tabular-nums;
}
.pos-cart-item-remove {
  order: 3;
  width: 26px; height: 26px;
  border: 1px solid #fecaca;
  border-radius: 999px;
  background: #fff1f2;
  color: #dc2626;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  line-height: 1;
  cursor: pointer;
  transition: background .1s;
  margin-left: 2px;
  align-self: center;
}
.pos-cart-item-remove :deep(svg) {
  width: 14px;
  height: 14px;
  display: block;
}
.pos-cart-item-remove:hover {
  background: #fecdd3;
  border-color: #ef4444;
  color: #991b1b;
  box-shadow: 0 0 0 1px rgba(239, 68, 68, 0.22);
}

/* Qty controls */
.pos-qty {
  order: 2;
  display: flex;
  align-items: center;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  overflow: hidden;
  margin-left: 0;
}
.pos-qty button {
  width: 34px;
  height: 28px;
  border: none;
  background: #f9fafb;
  color: #374151;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  display: grid;
  place-items: center;
}
.pos-qty button:hover:not(:disabled) { background: #e5e7eb; }
.pos-qty button:disabled { opacity: .3; cursor: not-allowed; }
.pos-qty input {
  width: 40px;
  height: 28px;
  border: none;
  border-left: 1px solid #d1d5db;
  border-right: 1px solid #d1d5db;
  text-align: center;
  font-weight: 800;
  font-size: 14px;
  color: #111;
  background: transparent;
  outline: none;
  appearance: textfield;
  -moz-appearance: textfield;
}
.pos-qty input::-webkit-outer-spin-button,
.pos-qty input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }

/* PPP Payment Section PPP */
.pos-payment-section {
  padding: 18px;
  background: #fff;
  border-radius: 12px;
  margin-top: 12px;
  margin-bottom: 4px;
  border: 1px solid #ececec;
  box-shadow: 0 1px 4px rgba(15,23,42,0.04);
}

.pos-summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13.5px;
  color: #374151;
  padding: 4px 0;
}
.pos-discount-row span:last-child { color: #166534; }
.pos-total-row {
  padding: 10px 0 6px;
  margin-top: 6px;
  border-top: 1px dashed #d1d5db;
  font-size: 16px;
}
.pos-total-amount { color: #0f172a; font-size: 22px; font-weight: 900; letter-spacing: -0.02em; }

/* Payment methods */
.pos-pay-methods { margin-top: 10px; }
.pos-pay-tabs {
  display: flex;
  gap: 8px;
}
.pos-pay-tabs button {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 10px 8px;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  background: #f9fafb;
  color: #374151;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all .15s;
}
.pos-pay-tabs button:hover { border-color: #94a3b8; background: #f8fafc; }
.pos-pay-tabs button.active {
  border-color: #1e293b;
  background: #f1f5f9;
  color: #1e293b;
}

/* Cash input */
.pos-cash-input { margin-top: 6px; }
.pos-quick-amounts {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 5px;
}
.pos-quick-amounts button {
  padding: 5px 10px;
  border: 1px solid #d1d5db;
  border-radius: 7px;
  background: #f9fafb;
  color: #374151;
  font: inherit;
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  transition: all .15s;
}
.pos-quick-amounts button:hover { border-color: #475569; background: #f1f5f9; color: #1e293b; }

.pos-change-section { margin-top: 8px; }
.pos-change-amount { color: #111; font-weight: 800; }

/* Toggle delivery */
.pos-toggle-delivery {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  font-size: 13px;
  font-weight: 700;
  color: #374151;
  cursor: pointer;
}
.pos-toggle-delivery input { accent-color: #dc2626; width: 16px; height: 16px; }

/* Address form */
.pos-address-form { display: grid; gap: 6px; margin-top: 6px; }
.pos-shipping-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-top: 8px;
  font-size: 13px;
}
.pos-shipping-row strong { color: #0f172a; white-space: nowrap; }

/* Action buttons */
.pos-action-btns {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
.pos-btn-cancel {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #6b7280;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.pos-btn-cancel:hover { background: #f1f5f9; color: #475569; border-color: #94a3b8; }
.pos-btn-cancel:disabled { opacity: .4; cursor: not-allowed; }
.pos-btn-pay {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 11px 14px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: #fff;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(220,38,38,.24);
  transition: all .15s;
}
.pos-btn-pay:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 7px 18px rgba(185,28,28,.3); }
.pos-btn-pay:disabled { opacity: .5; cursor: not-allowed; }

/* PPP Variant Picker Modal PPP */
.pos-variant-modal {
  background: #fff;
  border-radius: 20px;
  max-width: 760px;
  width: calc(100vw - 64px);
  max-height: 88vh;
  overflow: auto;
  box-shadow: 0 26px 70px rgba(15, 23, 42, .26);
}
.pos-variant-modal-head {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 12px;
  padding: 18px 24px;
  border-bottom: 1px solid #eef2f7;
}
.pos-variant-head-main { min-width: 0; }
.pos-variant-head-actions {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
.pos-variant-modal-head h3 {
  margin: 0;
  font-size: 25px;
  font-weight: 900;
  line-height: 1.25;
  color: #0f172a;
}
.pos-variant-head-main p {
  margin: 6px 0 0;
  font-size: 14px;
  color: #64748b;
  font-weight: 600;
}
.pos-variant-head-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 999px;
  border: 1px solid #fecaca;
  background: #fff1f2;
  color: #dc2626;
  font-size: 13px;
  font-weight: 800;
  padding: 7px 12px;
}
.pos-variant-modal-head button {
  width: 36px; height: 36px;
  border: none; border-radius: 10px;
  background: #f3f4f6; color: #475569;
  display: grid; place-items: center;
  cursor: pointer;
}
.pos-variant-modal-head button:hover { background: #fee2e2; color: #dc2626; }
.pos-variant-grid {
  padding: 14px 18px 18px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}
.pos-variant-item {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  align-items: stretch;
  gap: 14px;
  padding: 18px 16px;
  border: 1px solid #dbe2ea;
  border-radius: 14px;
  cursor: pointer;
  background: linear-gradient(180deg, #ffffff, #fbfcfe);
  transition: all .18s ease;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  animation: posVariantCardIn .36s ease both;
}
.pos-variant-item:hover {
  border-color: #fda4af;
  transform: translateY(-1px);
  box-shadow: 0 0 0 1px rgba(244, 63, 94, 0.24), 0 12px 28px rgba(220, 38, 38, 0.14), 0 0 24px rgba(37, 99, 235, 0.16);
}
.pos-variant-item.is-out { opacity: .4; cursor: not-allowed; }
.pos-variant-media {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #dbe2ea;
  background: radial-gradient(circle at 20% 20%, #f4f4ff 0%, #eef2ff 42%, #f8fafc 100%);
  min-height: 130px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pos-variant-item img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}
.pos-variant-details { flex: 1; min-width: 0; }
.pos-variant-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.pos-variant-code {
  font-size: 12px;
  font-weight: 800;
  color: #94a3b8;
}
.pos-variant-attrs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}
.pos-variant-pill {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  padding: 5px 12px;
  gap: 4px;
}
.pos-variant-pill-color {
  border: 1.5px solid #94a3b8;
  background: #1e293b;
  color: #f1f5f9;
}
.pos-variant-pill-color b { color: #fff; font-weight: 800; }
.pos-variant-pill-size {
  border: 1.5px solid #fda4af;
  background: #fff1f2;
  color: #be123c;
}
.pos-variant-pill-size b { color: #be123c; font-weight: 900; }
.pos-variant-pill-size.solo {
  font-size: 15px;
  padding: 7px 16px;
  border-width: 2px;
  background: #fce7f3;
  border-color: #f43f5e;
  color: #9f1239;
  box-shadow: 0 0 0 3px rgba(244,63,94,.12);
}
.pos-variant-pill-size.solo b { font-size: 16px; }
.pos-variant-stock {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1.5px solid #86efac;
  background: #dcfce7;
  color: #166534;
  font-size: 14px;
  font-weight: 900;
  padding: 4px 13px;
  line-height: 1.2;
}
.pos-variant-stock.is-low {
  border-color: #fdba74;
  background: #ffedd5;
  color: #c2410c;
}
.pos-variant-price-line {
  margin-top: 14px;
  display: flex;
  align-items: baseline;
  gap: 9px;
  flex-wrap: wrap;
}
.pos-variant-price {
  font-weight: 900;
  font-size: 28px;
  line-height: 1;
  letter-spacing: -0.025em;
  color: #0f172a;
}
.pos-variant-old-price {
  font-size: 14px;
  color: #94a3b8;
  font-weight: 700;
  text-decoration: line-through;
}
.pos-variant-action {
  margin-top: 12px;
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
}

@keyframes posVariantCardIn {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.992);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* PPP QR Modal PPP */
.pos-qr-modal {
  background: #f8fafc;
  border-radius: 20px;
  max-width: 520px;
  width: min(520px, calc(100vw - 28px));
  text-align: left;
  box-shadow: 0 20px 50px rgba(0,0,0,.18);
  overflow: hidden;
}
.pos-qr-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: #fff;
}
.pos-qr-header-main {
  display: flex;
  align-items: center;
  gap: 12px;
}
.pos-qr-body {
  padding: 18px 16px 16px;
}
.pos-qr-badge {
  width: 40px; height: 40px;
  border-radius: 10px;
  background: rgba(255,255,255,0.16);
  color: #fff;
  display: grid; place-items: center;
  flex-shrink: 0;
}
.pos-qr-title { font-weight: 800; font-size: 24px; line-height: 1.1; color: #fff; }
.pos-qr-sub { font-size: 12px; color: rgba(254, 226, 226, 0.95); font-weight: 600; margin-top: 2px; }
.pos-qr-close {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  border: 1px solid rgba(255,255,255,0.32);
  background: rgba(255,255,255,0.1);
  color: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
}
.pos-qr-close:hover { background: rgba(255,255,255,0.2); }
.pos-qr-amount {
  font-weight: 900;
  font-size: 42px;
  line-height: 1.05;
  color: #dc2626;
  letter-spacing: -0.02em;
  margin-bottom: 10px;
  text-align: center;
}
.pos-qr-img { margin: 0 auto 12px; max-width: 280px; }
.pos-qr-img img {
  width: 100%;
  border-radius: 14px;
  border: 1px solid #fecaca;
  background: #fff;
  padding: 8px;
}
.pos-qr-content {
  font-size: 14px;
  color: #475569;
  margin-bottom: 12px;
  text-align: center;
}
.pos-qr-waiting {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #b91c1c;
  margin-bottom: 12px;
  border: 1px solid #fecaca;
  border-radius: 10px;
  background: #fef2f2;
  min-height: 40px;
}
.pos-qr-spinner {
  width: 18px; height: 18px;
  border: 3px solid #fecaca;
  border-top-color: #dc2626;
  border-radius: 50%;
  animation: qr-spin 1s linear infinite;
}
@keyframes qr-spin { to { transform: rotate(360deg); } }
.pos-qr-note { font-size: 12px; color: #64748b; margin-bottom: 12px; text-align: center; }
.pos-qr-actions {
  display: grid;
  gap: 8px;
}

/* ═══ QR Paid Detail Modal (matches Customer Profile order-detail) ═══ */
.qr-paid-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.5);
  z-index: 1000;
  display: flex; align-items: center; justify-content: center;
  padding: 16px;
  animation: qrPaidFadeIn .2s ease;
}
@keyframes qrPaidFadeIn { from { opacity: 0 } to { opacity: 1 } }
.qr-paid-modal {
  background: #fff; border-radius: 20px;
  width: 100%; max-width: 660px; max-height: 92vh;
  overflow-y: auto;
  box-shadow: 0 24px 80px rgba(0,0,0,0.25);
  animation: qrPaidSlideUp .25s ease;
}
@keyframes qrPaidSlideUp { from { transform: translateY(24px); opacity: 0 } to { transform: translateY(0); opacity: 1 } }

/* Header — sidebar dark gradient + chess pattern */
.qr-paid-header {
  position: relative;
  isolation: isolate;
  display: flex; justify-content: space-between; align-items: center;
  padding: 18px 22px;
  background:
    radial-gradient(circle at 14% 10%, rgba(239, 68, 68, 0.2), transparent 44%),
    linear-gradient(165deg, #0a0a0f 0%, #17161e 46%, #07070b 100%);
  border-radius: 20px 20px 0 0;
}
.qr-paid-header::before {
  content: "";
  position: absolute; inset: 0;
  border-radius: 20px 20px 0 0;
  pointer-events: none;
  opacity: 0.25;
  z-index: 0;
  background-image:
    repeating-linear-gradient(45deg, rgba(255, 255, 255, 0.07) 0 1px, transparent 1px 18px),
    repeating-linear-gradient(-45deg, rgba(255, 255, 255, 0.05) 0 1px, transparent 1px 18px);
}
.qr-paid-header-inner {
  display: flex; align-items: center; gap: 12px;
  position: relative; z-index: 1;
}
.qr-paid-check-icon {
  width: 42px; height: 42px;
  border-radius: 12px;
  background: rgba(220, 38, 38, 0.25);
  border: 1px solid rgba(255,255,255,0.18);
  display: grid; place-items: center;
  color: #fca5a5;
  flex-shrink: 0;
}
.qr-paid-check-icon .material-icons-outlined { font-size: 24px; }
.qr-paid-header-title {
  font-weight: 800; font-size: 18px; color: #fff; line-height: 1.2;
}
.qr-paid-header-code {
  font-size: 12px; color: rgba(254, 226, 226, 0.9); font-weight: 600; margin-top: 2px;
}
.qr-paid-header-close {
  position: relative; z-index: 1;
  width: 32px; height: 32px; border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.3);
  background: rgba(255,255,255,0.1);
  color: #fff; font-size: 16px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background .15s;
}
.qr-paid-header-close:hover { background: rgba(255,255,255,0.2); }

.qr-paid-body { padding: 22px; display: flex; flex-direction: column; gap: 18px; }

/* Info grid */
.qr-paid-info {
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
  background: #f8fafc; border-radius: 14px; padding: 16px 18px;
}
.qr-paid-info-item { display: flex; flex-direction: column; gap: 3px; }
.qr-paid-info-item.full { grid-column: 1 / -1; }
.qr-paid-label {
  font-size: 11px; color: #94a3b8; text-transform: uppercase;
  letter-spacing: 0.5px; font-weight: 600;
}
.qr-paid-value { font-size: 15px; color: #1e293b; font-weight: 500; }
.qr-paid-status-badge {
  padding: 3px 10px; border-radius: 6px; font-size: 13px; font-weight: 600;
  display: inline-block; width: fit-content;
}
.qr-paid-status-badge.success { background: #dcfce7; color: #166534; }

/* Product list */
.qr-paid-products h4 { margin: 0 0 12px; font-size: 15px; font-weight: 700; color: #111; }
.qr-paid-product-list { display: flex; flex-direction: column; gap: 10px; }
.qr-paid-product-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; border-radius: 12px; background: #f9fafb;
  border: 1px solid #f1f5f9;
}
.qr-paid-product-img {
  flex-shrink: 0; width: 60px; height: 60px;
  border-radius: 10px; overflow: hidden;
  background: #f1f5f9;
}
.qr-paid-product-img img { width: 100%; height: 100%; object-fit: cover; }
.qr-paid-product-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.qr-paid-product-name {
  font-size: 14px; font-weight: 600; color: #1e293b;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.qr-paid-product-meta { font-size: 12px; color: #475569; }
.qr-paid-product-qty { font-size: 13px; color: #64748b; }
.qr-paid-product-total { font-size: 15px; font-weight: 700; color: #111; white-space: nowrap; }

/* Totals */
.qr-paid-totals {
  border-top: 1px solid #e5e7eb; padding-top: 14px;
  display: flex; flex-direction: column; gap: 8px;
}
.qr-paid-total-line { display: flex; justify-content: space-between; font-size: 15px; color: #334155; }
.qr-paid-total-line.grand {
  font-size: 17px; padding-top: 8px;
  border-top: 1px dashed #d1d5db; color: #111;
}
.qr-paid-total-line.grand strong { color: #dc2626; }

/* Actions */
.qr-paid-actions {
  padding-top: 4px;
}
.qr-paid-detail-btn {
  width: 100%;
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  padding: 14px 20px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: #fff;
  font-size: 15px; font-weight: 700;
  cursor: pointer;
  transition: opacity .15s, transform .1s;
  box-shadow: 0 3px 10px rgba(220, 38, 38, 0.28);
}
.qr-paid-detail-btn:hover { opacity: 0.92; transform: translateY(-1px); box-shadow: 0 5px 16px rgba(220,38,38,.35); }
.qr-paid-detail-btn:active { transform: translateY(0); }

/* PPP Customer Search Modal PPP */
.bh-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15,23,42,.5);
  backdrop-filter: blur(4px);
  display: grid;
  place-items: center;
  z-index: 9999;
  padding: 20px;
  overflow: auto;
}
.bh-modal-box {
  background: #fff;
  border-radius: 16px;
  max-width: 640px;
  width: 100%;
  max-height: 80vh;
  display: grid;
  grid-template-rows: auto auto 1fr;
  box-shadow: 0 20px 50px rgba(0,0,0,.2);
  overflow: hidden;
}
.bh-customer-modal {
  width: min(800px, calc(100vw - 40px));
  max-width: 800px;
}
.bh-customer-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}
.bh-customer-modal-head h2 { margin: 0; font-size: 18px; font-weight: 800; }
.bh-modal-close {
  width: 32px; height: 32px;
  border-radius: 8px; border: none;
  background: #f3f4f6; color: #475569;
  display: grid; place-items: center; cursor: pointer;
}
.bh-modal-close:hover { background: #fee2e2; color: #dc2626; }
.bh-modal-search {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  border-bottom: 1px solid #eee;
}
.bh-modal-search-icon { color: #9ca3af; flex-shrink: 0; }
.bh-modal-search input {
  flex: 1;
  border: none; outline: none;
  font: inherit; font-size: 14px; font-weight: 600;
  color: #111; background: transparent;
}
.bh-customer-modal-body { padding: 14px; overflow: auto; display: grid; gap: 12px; }
.bh-customer-table-wrap {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: auto;
  max-height: 50vh;
}
.bh-customer-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.bh-customer-table th,
.bh-customer-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
}
.bh-customer-table thead th {
  background: #f8fafc;
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  position: sticky;
  top: 0;
  z-index: 1;
}
.bh-customer-table tbody tr:hover { background: #f8fafc; }
.right { text-align: right !important; }
.bh-customer-create {
  border: 1px dashed #d1d5db;
  border-radius: 10px;
  background: #f9fafb;
  padding: 12px;
  display: grid;
  gap: 8px;
}
.bh-customer-create p { margin: 0; font-size: 13px; color: #374151; font-weight: 600; }
.bh-modal-empty {
  text-align: center;
  padding: 24px;

  /* PPP Product Modal Items PPP */
  .bh-product-modal {
    width: min(720px, calc(100vw - 32px));
    max-width: 720px;
  }
  .bh-modal-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding: 18px 20px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(to right, #fff 0%, #fef9f9 100%);
  }
  .bh-modal-head h2 { margin: 0; font-size: 18px; font-weight: 900; color: #111; }
  .bh-modal-head p { margin: 4px 0 0; font-size: 12px; color: #9ca3af; }
  .bh-modal-list { overflow-y: auto; flex: 1; }
  .bh-product-grid { display: grid; }
  .bh-modal-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 12px 18px;
    border-bottom: 1px solid #f4f4f4;
    cursor: pointer;
    transition: background .15s, box-shadow .15s;
  }
  .bh-modal-item:last-child { border-bottom: none; }
  .bh-modal-item:hover { background: #fef9f9; box-shadow: inset 3px 0 0 #fca5a5; }
  .bh-modal-item-disabled { opacity: .4; cursor: not-allowed; }
  .bh-modal-item-disabled:hover { background: transparent; box-shadow: none; }
  .bh-modal-item-img {
    width: 62px;
    height: 62px;
    border-radius: 10px;
    overflow: hidden;
    background: #f3f4f6;
    border: 1px solid #ebebeb;
    flex-shrink: 0;
    padding: 4px;
    box-sizing: border-box;
  }
  .bh-modal-item-img img { width: 100%; height: 100%; object-fit: contain; border-radius: 6px; }
  .bh-modal-item-info { flex: 1; min-width: 0; }
  .bh-modal-item-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 6px;
  }
  .bh-modal-item-code { font-size: 11.5px; font-weight: 800; color: #94a3b8; letter-spacing: .02em; }
  .bh-modal-item-stock {
    display: inline-flex;
    align-items: center;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 800;
    padding: 2px 9px;
    background: #dcfce7;
    color: #166534;
    border: 1px solid #86efac;
  }
  .bh-modal-item-stock.is-low { background: #ffedd5; color: #c2410c; border-color: #fdba74; }
  .bh-modal-item-name { font-size: 13.5px; font-weight: 700; color: #111; margin-top: 3px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .bh-modal-item-variant-line { display: flex; gap: 5px; flex-wrap: wrap; margin-top: 5px; }
  .bh-modal-item-variant-pill {
    display: inline-flex;
    align-items: center;
    border-radius: 999px;
    font-size: 11.5px;
    font-weight: 700;
    padding: 2px 9px;
    background: #f1f5f9;
    color: #475569;
    border: 1px solid #e2e8f0;
  }
  .bh-modal-item-variant-pill strong { margin-left: 2px; color: #111; font-weight: 800; }
  .bh-modal-item-price { font-size: 15px; font-weight: 900; color: #dc2626; margin-top: 5px; letter-spacing: -0.01em; }
  color: #9ca3af;
  font-weight: 600;
}

/* PPP Modal transition PPP */
.bh-modal-enter-active { transition: opacity .2s ease; }
.bh-modal-leave-active { transition: opacity .15s ease; }
.bh-modal-enter-from, .bh-modal-leave-to { opacity: 0; }

/* PPP Responsive PPP */
@media (max-width: 1400px) {
  .pos-product-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 1200px) {
  .pos-product-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 900px) {
  .pos-layout {
    grid-template-columns: 1fr;
    height: auto;
    padding: 8px;
    gap: 12px;
  }
  .pos-resize-gutter { display: none; }
  .pos-right { height: auto; }
  .pos-left { max-height: 50vh; }
  .pos-invoice-header {
    flex-wrap: wrap;
  }
  .bh-customer-modal { width: 100%; max-width: 100%; }
}
@media (max-width: 600px) {
  .pos-layout { grid-template-columns: 1fr; }
  .pos-product-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .pos-cart-item { flex-wrap: wrap; }
}
</style>