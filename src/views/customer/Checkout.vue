<script setup>
import { computed, onMounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import logo from "../../assets/img/logo/new logo.png?url"
import CustomerFooter from "../../components/customer/CustomerFooter.vue"
import momo from "../../assets/img/payments/momo.png?url"
import SiteNav from '../../components/SiteNav.vue'
import visa from "../../assets/img/payments/visa.png?url"
import mastercard from "../../assets/img/payments/mastercard.png?url"
import vnpay from "../../assets/img/payments/vnpay.png?url"
import { createVnpayPayment } from "../../services/vnpayService"
import { createBackendCheckoutOrder, isAccountActiveForCheckout, loadCheckoutContext } from "../../services/checkoutOrderService"
import { quoteShippingFeeAll } from "../../services/shippingQuoteService"
import { getAllSanPham } from "../../services/sanPhamService"
import { applyCampaignPriceToVariants } from "../../services/campaignPricingService"
import {
  calculateVoucherDiscount,
  findBestVoucher,
  getAllVouchers,
  getActiveVouchers,
  isVoucherApplicable,
  normalizeVoucherData
} from "../../services/khuyenMaiService"
import { useToast } from "../../composables/useToast"
import { useConfirm } from "../../composables/useConfirm"
import {
  readCheckoutCartArray,
  writeCheckoutCartArray,
  clearCheckoutCartArray,
  clearCartObject
} from "../../utils/cartStorage"
import { cleanLegacyVoucherStorage } from "../../utils/voucherSelectionStorage"
import { getProductImageConfig, getProductImageOverride } from "../../utils/productImageOverrides"
import { fallbackImageForVariant } from "../../utils/productImageFallback"
import img1 from "../../assets/img/Jackets/bomber/bomber-da-lon.jpg?url"
import img2 from "../../assets/img/Jackets/bomber/bomber-dang-lung.jpg?url"
import img3 from "../../assets/img/Jackets/bomber/bomber-gia-da.jpg?url"
import img4 from "../../assets/img/Jackets/bomber/bomber-nhe-cotton.jpg?url"
import img5 from "../../assets/img/Jackets/hoodie/hoodie-dang-hop.jpg?url"
import img6 from "../../assets/img/Jackets/hoodie/hoodie-in-hinh.jpg?url"
import img7 from "../../assets/img/Jackets/hoodie/hoodie-keo-khoa.jpg?url"
import img8 from "../../assets/img/Jackets/coach/coach-cach-nhiet.jpg?url"
import img9 from "../../assets/img/Jackets/coach/coach-da-asos.jpg?url"
import img10 from "../../assets/img/Jackets/coach/coach-gia-da.jpg?url"
import img11 from "../../assets/img/Jackets/coach/coach-long-cuu.jpg?url"
import img12 from "../../assets/img/Jackets/bomber/bomber-astronaut/bomber-astronaut-black.PNG?url"
import img13 from "../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-black.PNG?url"
import img14 from "../../assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-black.PNG?url"
import img15 from "../../assets/img/Jackets/coach/coach-leopard/coach-leopard.PNG?url"
import img16 from "../../assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-black.PNG?url"
import img17 from "../../assets/img/Jackets/coach/coach-tiger-stripe/coach-tiger-stripe.PNG?url"
import img18 from "../../assets/img/Jackets/hoodie/hoodie-camo/hoodie-camo-black.PNG?url"
import img19 from "../../assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-blue.PNG?url"
import img20 from "../../assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-black.PNG?url"
import img12b from "../../assets/img/Jackets/bomber/bomber-astronaut/bomber-astronaut-blue.PNG?url"
import img13b from "../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-green.PNG?url"
import img13c from "../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-brown.PNG?url"
import img13d from "../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-red.PNG?url"
import img13e from "../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-white.PNG?url"
import img14b from "../../assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-blue.PNG?url"
import img14c from "../../assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-green.PNG?url"
import img16b from "../../assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-red.PNG?url"
import img16c from "../../assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-white.PNG?url"
import img18b from "../../assets/img/Jackets/hoodie/hoodie-camo/hoodie-camo-white.PNG?url"
import img19b from "../../assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-white.PNG?url"
import img20b from "../../assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-gray.PNG?url"
import img20c from "../../assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-red.PNG?url"

const router = useRouter()
const route = useRoute()
const toast = useToast()
const { askConfirm } = useConfirm()
const year = new Date().getFullYear()

const submitting = ref(false)
const loadingProfile = ref(true)
const errorMsg = ref("")
const checkoutStorageWarningShown = ref(false)
const voucherCode = ref("")
const voucherHint = ref("")
const vouchers = ref([])
const loadingVouchers = ref(false)
const paymentMethod = ref("COD")
const vnpayChannel = ref("AUTO")
const restoreCheckoutPaymentState = () => {
  try {
    const savedOrder = JSON.parse(localStorage.getItem("currentOrder") || "null")
    const savedMethod = String(savedOrder?.paymentMethod || "").toUpperCase()
    const savedChannel = String(savedOrder?.vnpayChannel || "").toUpperCase()

    if (savedMethod === "COD" || savedMethod === "VNPAY") {
      paymentMethod.value = savedMethod
    }

    if (["VNPAYQR", "AUTO", "ATM", "INTCARD"].includes(savedChannel)) {
      vnpayChannel.value = savedChannel
    }
  } catch {}
}
const checkoutVoucherDrawerOpen = ref(false)
const activeVoucherItemKey = ref("")
const itemVoucherSelections = ref({})
const selectedOrderVoucher = ref(null)
const autoBestVoucherSignature = ref("")
const CHECKOUT_ITEM_VOUCHER_SELECTIONS_KEY = "checkoutItemVoucherSelections"
const CHECKOUT_PENDING_ITEM_VOUCHERS_KEY = "checkoutPendingItemVoucherSelections"
const ORDER_ITEM_VOUCHER_SNAPSHOTS_KEY = "orderItemVoucherSnapshots"

const cart = ref(readCheckoutCartArray())
const customerId = ref(null)
const variantCatalog = ref({})
const checkoutAccountActive = ref(true)

// ── Name/code-based image fallback (mirrors HomeView + CartPage) ──
const nameFallbackRules = [
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
  { keywords: ["zip", "silk"], image: img20 },
]
const codeFallbackMap = {
  SP001: img1, SP002: img2, SP003: img3, SP004: img4, SP005: img5,
  SP006: img6, SP007: img7, SP008: img8, SP009: img9, SP010: img10,
  SP011: img11, SP012: img12, SP013: img13, SP014: img14, SP015: img15,
  SP016: img16, SP017: img17, SP018: img18, SP019: img19, SP020: img20,
}
const getNameFallbackImage = (name = "") => {
  const normalized = String(name || "").normalize("NFD").replace(/\p{M}/gu, "").toLowerCase()
  if (!normalized) return ""
  const found = nameFallbackRules.find((rule) => rule.keywords.every((kw) => normalized.includes(kw)))
  return found?.image || ""
}
const getCodeFallbackImage = (code = "") => {
  const upper = String(code || "").trim().toUpperCase()
  return codeFallbackMap[upper] || ""
}

const colorImageFallbacks = {
  2: { 'Đỏ': img2, 'Do': img2, 'Xanh dương': img3, 'Xanh Duong': img3 },
  9: { 'Kem': img9, 'Nâu': img10, 'Nau': img10 },
  12: { 'Đen': img12, 'Xanh dương': img12b },
  13: { 'Đen': img13, 'Xanh lá': img13b, 'Nâu': img13c, 'Đỏ': img13d, 'Trắng': img13e },
  14: { 'Đen': img14, 'Xanh dương': img14b, 'Xanh lá': img14c },
  16: { 'Đen': img16, 'Đỏ': img16b, 'Trắng': img16c },
  18: { 'Đen': img18, 'Trắng': img18b },
  19: { 'Xanh dương': img19, 'Trắng': img19b },
  20: { 'Đen': img20, 'Xám': img20b, 'Đỏ': img20c },
}

const resolveColorImage = (productId, color) => {
  const map = colorImageFallbacks[Number(productId)]
  if (map && color && map[color]) return map[color]
  return ''
}

const normalizeVariantToken = (value) => {
  return String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "")
}

const isActiveVariantStatus = (value = "") => {
  const normalized = String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
  return !normalized.includes("ngung") && !normalized.includes("inactive")
}

const normalizeImage = (value) => String(value || "").trim()

const resolveVariantBackendImage = (variant) => {
  return normalizeImage(
    variant?.image
    || variant?.anh
    || variant?.hinhAnh
    || variant?.mauSac?.anh
    || ""
  )
}

const resolveProductImage = (product) => {
  const id = Number(product?.id)
  const code = String(product?.maSanPham || product?.ma || "").trim()
  const name = String(product?.tenSanPham || product?.name || "")
  const override = getProductImageOverride({ id, maSanPham: code })[0]
  if (override) return override
  const raw = product?.anh || product?.hinhAnh || product?.images?.[0] || ""
  if (raw && /^https?:\/\//i.test(String(raw))) return raw
  return getCodeFallbackImage(code) || getNameFallbackImage(name) || ""
}

const resolveOverrideColorImage = (item, variants = []) => {
  const productId = Number(item?.id)
  if (!Number.isFinite(productId) || productId <= 0) return ""

  const cfg = getProductImageConfig({ id: productId })
  const colorImages = Array.isArray(cfg?.colorImages) ? cfg.colorImages : []
  if (!colorImages.length) return ""

  const matched =
    variants.find((variant) => Number(variant?.spctId) === Number(item?.spctId))
    || variants.find((variant) => normalizeVariantToken(variant?.color) === normalizeVariantToken(item?.color)
      && normalizeVariantToken(variant?.size) === normalizeVariantToken(item?.size))
    || variants.find((variant) => normalizeVariantToken(variant?.color) === normalizeVariantToken(item?.color))

  const colorId = Number(matched?.colorId || 0)
  if (!Number.isFinite(colorId) || colorId <= 0) return ""

  const overrideEntry = colorImages.find((entry) => Number(entry?.colorId) === colorId)
  return normalizeImage(overrideEntry?.image)
}

/* ===================== DELIVERY ===================== */
const delivery = ref({
  name: "",
  phone: "",
  province: "",
  district: "",
  ward: "",
  addressDetail: ""
})

/* ===================== SAVED ADDRESSES ===================== */
const savedAddresses = ref([])
const selectedSavedAddress = ref(null)

/* ===================== ADDRESS DATA ===================== */
const provinces = ref([])
const districts = ref([])
const wards = ref([])

/* ===================== ADDRESS LOGIC ===================== */
const applyAddress = async (addr) => {
  if (!addr) return
  selectedSavedAddress.value = addr
  delivery.value.addressDetail = addr.diaChiCuThe || ""

  const matchedProvince = matchAddressName(provinces.value, addr.tinhThanh)
  if (matchedProvince) {
    delivery.value.province = matchedProvince.name
    try {
      const pRes = await fetch(`https://provinces.open-api.vn/api/p/${matchedProvince.code}?depth=2`)
      const pData = await pRes.json()
      districts.value = pData.districts || []
      const matchedDistrict = matchAddressName(districts.value, addr.quanHuyen)
      if (matchedDistrict) {
        delivery.value.district = matchedDistrict.name
        try {
          const dRes = await fetch(`https://provinces.open-api.vn/api/d/${matchedDistrict.code}?depth=2`)
          const dData = await dRes.json()
          wards.value = dData.wards || []
          const matchedWard = matchAddressName(wards.value, addr.phuongXa)
          if (matchedWard) delivery.value.ward = matchedWard.name
        } catch {}
      }
    } catch {}
  } else {
    delivery.value.province = addr.tinhThanh || ""
    delivery.value.district = addr.quanHuyen || ""
    delivery.value.ward = addr.phuongXa || ""
  }
  await fetchShippingQuote()
}

const clearAddressForManualEntry = () => {
  selectedSavedAddress.value = null
  delivery.value.province = ""
  delivery.value.district = ""
  delivery.value.ward = ""
  delivery.value.addressDetail = ""
  districts.value = []
  wards.value = []
}

const revertToDefaultAddress = async () => {
  if (savedAddresses.value.length > 0) {
    await applyAddress(savedAddresses.value[0])
  }
}

const onProvinceChange = async () => {
  delivery.value.district = ""
  delivery.value.ward = ""
  delivery.value.addressDetail = ""
  wards.value = []

  const p = provinces.value.find(x => x.name === delivery.value.province)
  if (!p) return

  const res = await fetch(`https://provinces.open-api.vn/api/p/${p.code}?depth=2`)
  const data = await res.json()
  districts.value = data.districts || []
}

const onDistrictChange = async () => {
  delivery.value.ward = ""
  delivery.value.addressDetail = ""

  const d = districts.value.find(x => x.name === delivery.value.district)
  if (!d) return

  const res = await fetch(`https://provinces.open-api.vn/api/d/${d.code}?depth=2`)
  const data = await res.json()
  wards.value = data.wards || []
}

const onWardChange = () => {
  delivery.value.addressDetail = ""
}

/* ===================== SHIPPING ===================== */
const shippingProvider = ref("GHN")
const shippingLoading = ref(false)
const shippingError = ref("")
const shippingQuote = ref(null)
const shippingRequestSeq = ref(0)
const DEFAULT_SHIPPING_FEE = 30000
const shippingSpec = ref({
  weightGram: 1200,
  lengthCm: 30,
  widthCm: 20,
  heightCm: 12,
  manualWeight: false
})

/* ===================== COMPUTED ===================== */
const VND = (n) => new Intl.NumberFormat("vi-VN").format(Number(n || 0)) + "₫"

const deliveryAddress = computed(() => {
  if (selectedSavedAddress.value) {
    const a = selectedSavedAddress.value
    return [a.diaChiCuThe, a.phuongXa, a.quanHuyen, a.tinhThanh]
      .map(p => String(p || "").trim()).filter(Boolean).join(", ")
  }
  return [
    delivery.value.addressDetail,
    delivery.value.ward,
    delivery.value.district,
    delivery.value.province
  ]
    .map((part) => String(part || "").trim())
    .filter(Boolean)
    .join(", ")
})

const hasShippingAddress = computed(() => {
  if (selectedSavedAddress.value) {
    const a = selectedSavedAddress.value
    return Boolean(String(a?.tinhThanh || "").trim() && String(a?.quanHuyen || "").trim())
  }

  const province = String(delivery.value.province || "").trim()
  const district = String(delivery.value.district || "").trim()
  const ward = String(delivery.value.ward || "").trim()
  const needsWard = wards.value.length > 0
  if (!province || !district) return false
  if (needsWard && !ward) return false
  return true
})

const shipping = computed(() => {
  const fee = Number(shippingQuote.value?.fee)
  if (Number.isFinite(fee) && fee > 0) return fee
  if (hasShippingAddress.value) return DEFAULT_SHIPPING_FEE
  return 0
})
const getItemKey = (item) => {
  const id = String(item?.id || "").trim()
  const color = String(item?.color || "").trim()
  const size = String(item?.size || "").trim()
  const voucherCode = String(item?.voucherCode || "").trim().toUpperCase()
  return `${id}__${color}__${size}__${voucherCode}`
}

const getLineSubtotal = (item) => Number(item?.price || 0) * Number(item?.quantity || 0)

const getSelectedVoucherForItem = (item) => {
  const key = getItemKey(item)
  const selected = itemVoucherSelections.value[key] || null
  if (selected) return selected

  const voucherCode = String(item?.voucherCode || "").trim().toUpperCase()
  if (!voucherCode) return null
  return vouchers.value.find((voucher) => String(voucher?.maPhieuGiamGia || "").trim().toUpperCase() === voucherCode) || null
}

const getItemVoucherDiscount = (item, voucherOverride = null) => {
  return 0
}

const getLineTotalAfterVoucher = (item) => getLineSubtotal(item)

const voucherDiscount = computed(() => {
  if (!selectedOrderVoucher.value) return 0
  if (!isVoucherApplicable(selectedOrderVoucher.value, subtotal.value, customerId.value)) return 0
  return calculateVoucherDiscount(selectedOrderVoucher.value, subtotal.value)
})

const subtotal = computed(() => {
  return cart.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0)
})

const estimatedWeightByCart = computed(() => {
  const totalQty = cart.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
  return Math.max(500, totalQty * 450)
})


const total = computed(() => Math.max(subtotal.value + shipping.value - voucherDiscount.value, 0))
const cartCount = computed(() => cart.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0))

const bestVoucherId = computed(() => {
  const best = findBestVoucher(vouchers.value, subtotal.value, customerId.value)
  return best?.id || best?.maPhieuGiamGia || null
})

const displayVouchers = computed(() => {
  const all = vouchers.value.slice(0, 8)
  const bestId = bestVoucherId.value
  if (!bestId) return all
  const bestIdx = all.findIndex(v => v.id === bestId || v.maPhieuGiamGia === bestId)
  if (bestIdx <= 0) return all
  const copy = [...all]
  const [best] = copy.splice(bestIdx, 1)
  copy.unshift(best)
  return copy
})

const applyGlobalVoucher = (voucher) => {
  if (!voucher) return
  if (!isVoucherApplicable(voucher, subtotal.value, customerId.value)) {
    const needed = Number(voucher?.hoaDonToiThieu || 0) - subtotal.value
    if (needed > 0) {
      toast.error(`Cần thêm ${VND(needed)} để áp dụng voucher này`)
    } else {
      toast.error('Voucher không khả dụng')
    }
    return
  }
  selectedOrderVoucher.value = voucher
  voucherCode.value = String(voucher?.maPhieuGiamGia || '')
  voucherHint.value = `Đã áp dụng ${voucherCode.value} cho toàn bộ đơn hàng`
  localStorage.setItem('checkoutSelectedVoucher', JSON.stringify(voucher))
  toast.success(`Đã áp dụng voucher ${voucherCode.value}`)
  if (checkoutVoucherDrawerOpen.value) {
    closeCheckoutVoucherDrawer()
  }
}

const clearGlobalVoucher = () => {
  selectedOrderVoucher.value = null
  voucherCode.value = ''
  voucherHint.value = ''
  localStorage.removeItem('checkoutSelectedVoucher')
}

const activeVoucherItem = computed(() => {
  const key = String(activeVoucherItemKey.value || "")
  if (!key) return null
  return cart.value.find((item) => getItemKey(item) === key) || null
})

const activeVoucherItemSubtotal = computed(() => {
  if (!activeVoucherItem.value) return 0
  return getLineSubtotal(activeVoucherItem.value)
})

const persistCheckoutCart = () => {
  const ok = writeCheckoutCartArray(cart.value)
  if (!ok) {
    if (!checkoutStorageWarningShown.value) {
      toast.error("Không thể lưu giỏ thanh toán do bộ nhớ trình duyệt đã đầy.")
      checkoutStorageWarningShown.value = true
    }
    return
  }
  checkoutStorageWarningShown.value = false
}

const clearCartStorage = () => {
  cart.value = []
  clearCartObject()
  clearCheckoutCartArray()
  localStorage.removeItem(CHECKOUT_ITEM_VOUCHER_SELECTIONS_KEY)
}

const loadItemVoucherSelections = () => {
  try {
    const raw = localStorage.getItem(CHECKOUT_ITEM_VOUCHER_SELECTIONS_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    itemVoucherSelections.value = parsed && typeof parsed === "object" ? parsed : {}
  } catch {
    itemVoucherSelections.value = {}
  }
}

const persistItemVoucherSelections = () => {
  try {
    localStorage.setItem(CHECKOUT_ITEM_VOUCHER_SELECTIONS_KEY, JSON.stringify(itemVoucherSelections.value || {}))
  } catch {}
}

const mergePendingProductDetailVouchers = () => {
  try {
    const rawPending = localStorage.getItem(CHECKOUT_PENDING_ITEM_VOUCHERS_KEY)
    if (!rawPending) return

    const parsedPending = JSON.parse(rawPending)
    const pendingMap = parsedPending && typeof parsedPending === "object" ? parsedPending : {}
    const nextSelections = { ...itemVoucherSelections.value }

    for (const [pendingKey, payload] of Object.entries(pendingMap)) {
      const voucherCandidate = payload?.voucher || payload
      const target = payload?.target || {}
      const voucher = normalizeVoucherData(voucherCandidate)
      if (!voucher?.maPhieuGiamGia) continue

      let matchedItem = cart.value.find((item) => getItemKey(item) === pendingKey)
      if (!matchedItem && target?.productId) {
        matchedItem = cart.value.find((item) => {
          const sameProduct = Number(item?.id || 0) === Number(target.productId || 0)
          if (!sameProduct) return false
          const sameColor = !target?.color || String(item?.color || "") === String(target.color)
          const sameSize = !target?.size || String(item?.size || "") === String(target.size)
          const sameVoucher = !target?.voucherCode || String(item?.voucherCode || "").trim().toUpperCase() === String(target.voucherCode || "").trim().toUpperCase()
          return sameColor && sameSize && sameVoucher
        })
      }

      if (!matchedItem) continue
      const lineSubtotal = getLineSubtotal(matchedItem)
      if (!isVoucherApplicable(voucher, lineSubtotal, customerId.value)) continue

      nextSelections[getItemKey(matchedItem)] = voucher
    }

    itemVoucherSelections.value = nextSelections
    persistItemVoucherSelections()
    localStorage.removeItem(CHECKOUT_PENDING_ITEM_VOUCHERS_KEY)
  } catch {
    // no-op
  }
}

const normalizeCartItemQuantity = (item) => {
  const qty = Number(item?.quantity || 1)
  item.quantity = Number.isFinite(qty) && qty > 0 ? Math.floor(qty) : 1
}

const getVariantStock = (variant = {}) => {
  const candidates = [
    variant?.soLuong,
    variant?.soLuongTon,
    variant?.tonKho,
    variant?.available,
  ]
  for (const value of candidates) {
    const n = Number(value)
    if (Number.isFinite(n) && n >= 0) return n
  }
  return null
}

const getAvailableStockForItem = (item) => {
  const variants = getVariantsForItem(item)
  if (!variants.length) return null

  const exact = variants.find((variant) => {
    return String(variant?.spctId || "") === String(item?.spctId || "")
      || (String(variant?.color || "") === String(item?.color || "")
        && String(variant?.size || "") === String(item?.size || ""))
  })

  const fallback = exact || variants[0]
  return getVariantStock(fallback)
}

const increaseItemQuantity = (item) => {
  normalizeCartItemQuantity(item)
  const availableStock = getAvailableStockForItem(item)
  if (Number.isFinite(availableStock) && item.quantity >= availableStock) {
    toast.warning(`Sản phẩm ${item?.name || ""} chỉ còn ${availableStock} sản phẩm trong kho`)
    return
  }
  item.quantity += 1
  persistCheckoutCart()
}

const decreaseItemQuantity = (item) => {
  normalizeCartItemQuantity(item)
  if (item.quantity <= 1) return
  item.quantity -= 1
  persistCheckoutCart()
}

const removeCartItem = (item) => {
  const itemKey = getItemKey(item)
  if (itemKey in itemVoucherSelections.value) {
    const next = { ...itemVoucherSelections.value }
    delete next[itemKey]
    itemVoucherSelections.value = next
  }
  cart.value = cart.value.filter((entry) => {
    return !(
      String(entry?.id) === String(item?.id)
      && String(entry?.size || "") === String(item?.size || "")
      && String(entry?.color || "") === String(item?.color || "")
      && String(entry?.voucherCode || "").trim().toUpperCase() === String(item?.voucherCode || "").trim().toUpperCase()
    )
  })
  persistCheckoutCart()
}

const getVariantsForItem = (item) => {
  const key = String(item?.id || "")
  return Array.isArray(variantCatalog.value[key]) ? variantCatalog.value[key] : []
}

const getColorOptions = (item) => {
  return [...new Set(getVariantsForItem(item).map((variant) => variant.color).filter(Boolean))]
}

const getSizeOptions = (item) => {
  const variants = getVariantsForItem(item)
  const filtered = item?.color
    ? variants.filter((variant) => variant.color === item.color)
    : variants
  return [...new Set(filtered.map((variant) => variant.size).filter(Boolean))]
}

const syncItemVariant = (item) => {
  const variants = getVariantsForItem(item)
  if (!variants.length) return

  let matched = variants.find((variant) => variant.color === item.color && variant.size === item.size)
  if (!matched && item.color) {
    matched = variants.find((variant) => variant.color === item.color)
  }
  if (!matched && item.size) {
    matched = variants.find((variant) => variant.size === item.size)
  }
  if (!matched) {
    matched = variants[0]
  }

  item.color = matched.color || item.color || ""
  item.size = matched.size || item.size || ""
  item.price = Number(matched.price || item.price || 0)
  item.basePrice = Number(matched.basePrice || item.basePrice || item.price || 0)
  item.dotGiamGiaPhanTram = Number(matched.dotGiamGiaPhanTram || item.dotGiamGiaPhanTram || 0)
  item.spctId = matched.spctId || item.spctId
  const overrideImg = resolveOverrideColorImage(item, variants)
  const colorImg = resolveColorImage(Number(item.id), item.color)
  // matched.image already includes fallbackImageForVariant from loadVariantCatalog
  const nextImage = overrideImg || matched.image || colorImg || getNameFallbackImage(item.name) || item.image
  item.image = nextImage
}

const updateItemColor = (item, color) => {
  const oldKey = getItemKey(item)
  item.color = color
  const sizeOptions = getSizeOptions(item)
  if (!sizeOptions.includes(item.size)) {
    item.size = sizeOptions[0] || ""
  }
  syncItemVariant(item)
  const newKey = getItemKey(item)
  if (oldKey !== newKey && itemVoucherSelections.value[oldKey]) {
    const next = { ...itemVoucherSelections.value }
    next[newKey] = next[oldKey]
    delete next[oldKey]
    itemVoucherSelections.value = next
  }
  persistCheckoutCart()
}

const updateItemSize = (item, size) => {
  const oldKey = getItemKey(item)
  item.size = size
  syncItemVariant(item)
  const newKey = getItemKey(item)
  if (oldKey !== newKey && itemVoucherSelections.value[oldKey]) {
    const next = { ...itemVoucherSelections.value }
    next[newKey] = next[oldKey]
    delete next[oldKey]
    itemVoucherSelections.value = next
  }
  persistCheckoutCart()
}

const loadVariantCatalog = async () => {
  try {
    const response = await getAllSanPham()
    const products = Array.isArray(response?.data) ? response.data : []
    const catalog = {}

    for (const product of products) {
      const productId = String(product?.id || "")
      const numId = Number(productId)
      const variants = Array.isArray(product?.sanPhamChiTiets)
        ? product.sanPhamChiTiets.filter((variant) => isActiveVariantStatus(variant?.trangThai || variant?.status))
        : []
      const pricedVariants = await applyCampaignPriceToVariants(variants, product.id)

      catalog[productId] = pricedVariants.map((variant) => {
        const varColor = String(variant?.mauSac?.tenMau || "").trim()
        const varImg = resolveVariantBackendImage(variant)
          || resolveColorImage(numId, varColor)
          || fallbackImageForVariant({
            id: numId,
            maSanPham: product?.maSanPham,
            tenSanPham: product?.tenSanPham,
            tenMauSac: varColor,
            maChiTietSanPham: variant?.ma,
          })
        const finalPrice = Number(variant?.giaBanSauDotGiamGia ?? variant?.giaBan ?? 0)
        return {
          spctId: variant?.id,
          colorId: variant?.mauSac?.id,
          color: varColor,
          size: String(variant?.kichThuoc?.tenKichThuoc || "").trim(),
          price: finalPrice,
          basePrice: Number(variant?.giaBanGoc ?? variant?.giaBan ?? 0),
          dotGiamGiaPhanTram: Number(variant?.dotGiamGiaPhanTram || 0),
          image: varImg
        }
      })
    }

    variantCatalog.value = catalog
    cart.value.forEach((item) => syncItemVariant(item))
    persistCheckoutCart()
  } catch (error) {
    console.error("Load variant catalog failed:", error)
  }
}

const canSubmit = computed(() => {
  if (submitting.value) return false
  if (!checkoutAccountActive.value) return false
  return Array.isArray(cart.value) && cart.value.length > 0
})

const goHome = () => {
  router.push("/trang-chu")
}

const checkoutVoucherChipLabel = (v) => {
  return String(v?.maPhieuGiamGia || "")
}

const openCheckoutVoucherDrawer = (item = null) => {
  if (item) {
    activeVoucherItemKey.value = getItemKey(item)
  }
  checkoutVoucherDrawerOpen.value = true
  document.body.style.overflow = 'hidden'
}

const closeCheckoutVoucherDrawer = () => {
  checkoutVoucherDrawerOpen.value = false
  activeVoucherItemKey.value = ""
  document.body.style.overflow = ''
}

const applyVoucherFromDrawer = (v) => {
  if (!activeVoucherItem.value) {
    toast.error('Vui lòng chọn sản phẩm cần áp voucher')
    return
  }

  const lineSubtotal = activeVoucherItemSubtotal.value
  if (!isVoucherApplicable(v, lineSubtotal, customerId.value)) {
    const needed = Number(v.hoaDonToiThieu || 0) - lineSubtotal
    if (needed > 0) {
      toast.error(`Cần thêm ${VND(needed)} để áp dụng voucher này`)
    } else {
      toast.error('Voucher không khả dụng')
    }
    return
  }

  const key = getItemKey(activeVoucherItem.value)
  itemVoucherSelections.value = {
    ...itemVoucherSelections.value,
    [key]: v,
  }
  voucherCode.value = v.maPhieuGiamGia
  voucherHint.value = `Đã áp dụng ${v.maPhieuGiamGia} cho ${activeVoucherItem.value?.name || 'sản phẩm'}`
  closeCheckoutVoucherDrawer()
}

const clearItemVoucher = (item) => {
  const key = getItemKey(item)
  if (!(key in itemVoucherSelections.value)) return
  const next = { ...itemVoucherSelections.value }
  delete next[key]
  itemVoucherSelections.value = next
}

const persistOrderVoucherSnapshot = ({ keys = [], itemVouchers = [] } = {}) => {
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
    // no-op
  }
}

const showDemoToast = (message) => {
  if (typeof toast?.info === "function") {
    toast.info(message)
    return
  }
  if (typeof toast?.success === "function") {
    toast.success(message)
  }
}

const loadVouchers = async () => {
  loadingVouchers.value = true
  try {
    const normalizeVoucher = (voucher) => ({
      ...normalizeVoucherData(voucher),
      id: voucher?.id ?? voucher?.phieuGiamGiaId ?? voucher?.maPhieuGiamGia ?? voucher?.maPhieu ?? voucher?.code
    })

    let payload = []

    try {
      const response = await getActiveVouchers()
      const activeData = response?.data

      payload = Array.isArray(activeData)
        ? activeData
        : (Array.isArray(activeData?.content) ? activeData.content : [])

      if (!payload.length) {
        const fallbackResponse = await getAllVouchers()
        const fallbackData = fallbackResponse?.data

        payload = Array.isArray(fallbackData)
          ? fallbackData
          : (Array.isArray(fallbackData?.content) ? fallbackData.content : [])
      }
    } catch {
      const response = await getAllVouchers()
      const fullData = response?.data

      payload = Array.isArray(fullData)
        ? fullData
        : (Array.isArray(fullData?.content) ? fullData.content : [])
    }

    vouchers.value = payload.map(normalizeVoucher)

  } catch (error) {
    vouchers.value = []
    console.error("Load vouchers failed:", error)
  } finally {
    loadingVouchers.value = false
  }
}

const applyVoucherCode = () => {
  const keyword = String(voucherCode.value || "").trim().toUpperCase()
  if (!keyword) {
    toast.error("Vui lòng nhập mã voucher")
    return
  }

  const found = vouchers.value.find((voucher) => {
    return String(voucher?.maPhieuGiamGia || "").trim().toUpperCase() === keyword
  })

  if (!found) {
    toast.error("Không tìm thấy voucher phù hợp")
    return
  }

  applyGlobalVoucher(found)
}

const removeVoucher = () => {
  clearGlobalVoucher()
}

const normalizeAddressText = (value = "") => {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim()
}

const matchAddressName = (options = [], value = "") => {
  const keyword = normalizeAddressText(value)
  if (!keyword) return null
  return options.find((entry) => normalizeAddressText(entry?.name) === keyword)
    || options.find((entry) => {
      const normalized = normalizeAddressText(entry?.name)
      return normalized && (normalized.includes(keyword) || keyword.includes(normalized))
    })
    || null
}

const adminPartKeywords = [
  "phuong", "xa", "quan", "huyen", "thi tran", "thi xa", "thanh pho", "tinh", "tp"
]

const cleanAddressDetailAfterLocationChange = () => {
  const raw = String(delivery.value.addressDetail || "").trim()
  if (!raw) return

  const normalizedSelections = [delivery.value.province, delivery.value.district, delivery.value.ward]
    .map((part) => normalizeAddressText(part))
    .filter(Boolean)

  const cleaned = raw
    .split(",")
    .map((part) => part.trim())
    .filter(Boolean)
    .filter((part) => {
      const normalizedPart = normalizeAddressText(part)
      if (!normalizedPart) return false
      if (normalizedSelections.includes(normalizedPart)) return false
      if (normalizedSelections.some((sel) => sel && (normalizedPart.includes(sel) || sel.includes(normalizedPart)))) return false
      if (adminPartKeywords.some((keyword) => normalizedPart.includes(keyword))) return false
      return true
    })

  delivery.value.addressDetail = cleaned.join(", ")
}

const parseAddressParts = (value = "") => {
  const parts = String(value || "")
    .split(",")
    .map((part) => part.trim())
    .filter(Boolean)

  if (!parts.length) {
    return {
      province: "",
      district: "",
      ward: "",
      addressDetail: ""
    }
  }

  const fromTail = {
    province: parts[parts.length - 1] || "",
    district: parts.length >= 2 ? parts[parts.length - 2] : "",
    ward: parts.length >= 3 ? parts[parts.length - 3] : "",
    addressDetail: parts.length >= 4
      ? parts.slice(0, parts.length - 3).join(", ")
      : ""
  }

  const fromHead = {
    province: parts[0] || "",
    district: parts.length >= 2 ? parts[1] : "",
    ward: parts.length >= 3 ? parts[2] : "",
    addressDetail: parts.length >= 4
      ? parts.slice(3).join(", ")
      : ""
  }

  return {
    fromTail,
    fromHead,
    raw: parts
  }
}

const fetchShippingQuote = async () => {
  shippingError.value = ""

  const toProvince = String(delivery.value.province || "").trim()
  const toDistrict = String(delivery.value.district || "").trim()
  const toWard = String(delivery.value.ward || "").trim()
  const expectedWard = String(selectedSavedAddress.value?.phuongXa || "").trim()
  const needsWard = Boolean(expectedWard || wards.value.length > 0)

  if (!toProvince || !toDistrict) {
    shippingQuote.value = null
    return
  }

  if (needsWard && !toWard) {
    shippingQuote.value = null
    return
  }

  const requestSeq = shippingRequestSeq.value + 1
  shippingRequestSeq.value = requestSeq
  shippingLoading.value = true
  try {
    const payload = {
      provider: shippingProvider.value,
      toProvince,
      toDistrict,
      toWard,
      weightGram: Number(shippingSpec.value.weightGram || 0),
      lengthCm: Number(shippingSpec.value.lengthCm || 0),
      widthCm: Number(shippingSpec.value.widthCm || 0),
      heightCm: Number(shippingSpec.value.heightCm || 0)
    }

    const response = await quoteShippingFeeAll(payload)
    if (requestSeq !== shippingRequestSeq.value) return
    const quotes = Array.isArray(response?.data?.quotes) ? response.data.quotes : []
    const matched = quotes.find((item) => String(item?.provider || "").toUpperCase() === shippingProvider.value)
    shippingQuote.value = matched || quotes[0] || null
  } catch (error) {
    if (requestSeq !== shippingRequestSeq.value) return
    shippingQuote.value = { fee: DEFAULT_SHIPPING_FEE }
    shippingError.value = error?.response?.data?.message || `Không lấy được phí ship realtime, tạm dùng mức ${VND(DEFAULT_SHIPPING_FEE)}`
  } finally {
    if (requestSeq === shippingRequestSeq.value) {
      shippingLoading.value = false
    }
  }
}

const isValidPhone = (value) => {
  const phone = String(value || "").replace(/\s+/g, "")
  return /^(0|\+84)\d{9,10}$/.test(phone)
}

const validateCheckout = () => {
  if (!Array.isArray(cart.value) || cart.value.length === 0) {
    return "Giỏ hàng đang trống"
  }

  const hasInvalidItem = cart.value.some((item) => {
    return Number(item?.quantity || 0) <= 0 || Number(item?.price || 0) <= 0 || !item?.name
  })
  if (hasInvalidItem) {
    return "Giỏ hàng có dữ liệu không hợp lệ, vui lòng quay lại trang chủ"
  }

  const exceededItem = cart.value.find((item) => {
    const availableStock = getAvailableStockForItem(item)
    if (!Number.isFinite(availableStock)) return false
    return Number(item?.quantity || 0) > availableStock
  })
  if (exceededItem) {
    const availableStock = getAvailableStockForItem(exceededItem)
    return `Sản phẩm ${exceededItem?.name || ""} chỉ còn ${availableStock} trong kho`
  }

  if (!delivery.value.name.trim()) return "Vui lòng nhập họ tên người nhận"
  if (!isValidPhone(delivery.value.phone)) return "Số điện thoại không hợp lệ"

  if (selectedSavedAddress.value) {
    if (!String(selectedSavedAddress.value.diaChiCuThe || "").trim()) return "Địa chỉ cụ thể cần chi tiết hơn"
  } else {
    if (!String(delivery.value.province || "").trim()) return "Vui lòng chọn Tỉnh/Thành phố nhận hàng"
    if (!String(delivery.value.district || "").trim()) return "Vui lòng nhập Quận/Huyện nhận hàng"
    if (String(delivery.value.addressDetail || "").trim().length < 6) return "Địa chỉ cụ thể cần chi tiết hơn"
  }

  if (hasShippingAddress.value && Number(shipping.value || 0) <= 0) {
    return "Không tính được phí vận chuyển, vui lòng kiểm tra lại địa chỉ giao hàng"
  }

  if (!checkoutAccountActive.value) {
    return "Tài khoản đang ngừng hoạt động, không thể thanh toán"
  }

  return ""
}

const getVoucherDescription = (v) => {
  if (!v) return ""

  const discountValue = Number(v.giaTriGiamGia ?? v.giaTriGiam ?? 0)
  const minimumOrder = Number(v.hoaDonToiThieu ?? v.dieuKienToiThieu ?? 0)
  const maxDiscount = Number(v.soTienGiamToiDa || 0)
  const isPercent = Boolean(v.hinhThucGiam)
    || String(v.loaiKhuyenMai || "").toUpperCase().includes("PHAN_TRAM")
    || String(v.loaiKhuyenMai || "").toUpperCase().includes("PERCENT")

  if (isPercent) {
    if (maxDiscount > 0) {
      return `Giảm ${discountValue}% (tối đa ${Math.round(maxDiscount / 1000)}K) đơn từ ${Math.round(minimumOrder / 1000)}K`
    }
    return `Giảm ${discountValue}% đơn từ ${Math.round(minimumOrder / 1000)}K`
  }

  return `Giảm ${Math.round(discountValue / 1000)}K đơn từ ${Math.round(minimumOrder / 1000)}K`
}

const formatExpiry = (dateStr) => {
    if (!dateStr) return "N/A"
    try {
      const d = new Date(dateStr)
      return isNaN(d) ? String(dateStr) : `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}/${d.getFullYear()}`
    } catch {
      return String(dateStr)
    }
}

const selectVoucherCard = (voucher) => {
  applyGlobalVoucher(voucher)
}

const autoSelectBestVoucher = (silent = false) => {
  if (!Array.isArray(vouchers.value) || !vouchers.value.length || subtotal.value <= 0) {
    return
  }

  const bestVoucher = findBestVoucher(vouchers.value, subtotal.value, customerId.value)
  if (!bestVoucher) {
    return
  }

  const current = selectedOrderVoucher.value
  const currentCode = String(current?.maPhieuGiamGia || "").trim().toUpperCase()
  const bestCode = String(bestVoucher?.maPhieuGiamGia || "").trim().toUpperCase()
  if (!bestCode || currentCode === bestCode) return

  const currentDiscount = current && isVoucherApplicable(current, subtotal.value, customerId.value)
    ? calculateVoucherDiscount(current, subtotal.value)
    : 0
  const bestDiscount = calculateVoucherDiscount(bestVoucher, subtotal.value)
  if (bestDiscount <= currentDiscount) return

  selectedOrderVoucher.value = bestVoucher
  voucherCode.value = bestVoucher.maPhieuGiamGia
  voucherHint.value = `Tự động chọn voucher tốt nhất: ${bestVoucher.maPhieuGiamGia}`
  localStorage.setItem("checkoutSelectedVoucher", JSON.stringify(bestVoucher))

  const signature = `${bestCode}::${Math.floor(subtotal.value / 10000)}`
  if (!silent && autoBestVoucherSignature.value !== signature) {
    autoBestVoucherSignature.value = signature
    toast.success(`Đã tự động áp dụng voucher tốt nhất ${bestVoucher.maPhieuGiamGia}`)
  }
}

const loadProfile = async () => {
  loadingProfile.value = true
  try {
    const context = await loadCheckoutContext()
    checkoutAccountActive.value = isAccountActiveForCheckout(context.account)
    customerId.value = context.customer?.id || null
    savedAddresses.value = Array.isArray(context.addresses) ? context.addresses : []

    if (savedAddresses.value.length > 0) {
      delivery.value.name = context.delivery?.name || ""
      delivery.value.phone = context.delivery?.phone || ""
      await applyAddress(savedAddresses.value[0])
    } else {
    const addr = context.delivery

    delivery.value.name = addr.name || ""
    delivery.value.phone = addr.phone || ""

    // Use separate DB fields when available, fall back to parsing combined string
    let rawProvince = addr.tinhThanh || ""
    let rawDistrict = addr.quanHuyen || ""
    let rawWard = addr.phuongXa || ""
    let rawDetail = addr.diaChiCuThe || ""

    if (!rawProvince && addr.address) {
      const parsedAddress = parseAddressParts(addr.address)
      const pref = [parsedAddress.fromTail, parsedAddress.fromHead]
        .find((c) => matchAddressName(provinces.value, c.province)) || parsedAddress.fromTail
      rawProvince = pref.province || ""
      rawDistrict = pref.district || ""
      rawWard = pref.ward || ""
      rawDetail = pref.addressDetail || ""
      if (!rawDetail) {
        const unresolved = (parsedAddress.raw || []).filter((part) => {
          const norm = normalizeAddressText(part)
          return norm && [rawProvince, rawDistrict, rawWard].every((sel) => normalizeAddressText(sel) !== norm)
        })
        rawDetail = unresolved.join(", ")
      }
    }

    // Set specific address detail from DB directly (no aggressive stripping)
    delivery.value.addressDetail = rawDetail

    // Cascading load: province -> districts -> district -> wards -> ward
    const matchedProvince = matchAddressName(provinces.value, rawProvince)
    if (matchedProvince) {
      delivery.value.province = matchedProvince.name
      try {
        const pRes = await fetch(`https://provinces.open-api.vn/api/p/${matchedProvince.code}?depth=2`)
        const pData = await pRes.json()
        districts.value = pData.districts || []

        const matchedDistrict = matchAddressName(districts.value, rawDistrict)
        if (matchedDistrict) {
          delivery.value.district = matchedDistrict.name
          try {
            const dRes = await fetch(`https://provinces.open-api.vn/api/d/${matchedDistrict.code}?depth=2`)
            const dData = await dRes.json()
            wards.value = dData.wards || []

            const matchedWard = matchAddressName(wards.value, rawWard)
            if (matchedWard) {
              delivery.value.ward = matchedWard.name
            }
          } catch { /* ward load failed */ }
        }
      } catch { /* district load failed */ }
    }
    } // end else (no saved addresses)
  } catch (error) {
    console.error("Load checkout profile failed:", error)
  } finally {
    loadingProfile.value = false
  }
}

const resolveBankCode = () => {
  if (vnpayChannel.value === "VNPAYQR") return "VNPAYQR"
  if (vnpayChannel.value === "ATM") return "VNBANK"
  if (vnpayChannel.value === "INTCARD") return "INTCARD"
  // AUTO lets VNPay render available methods.
  return ""
}

const submitOrder = async () => {
  const validationError = validateCheckout()
  if (validationError) {
    errorMsg.value = validationError
    toast.error(validationError)
    return
  }

  const ok = await askConfirm(
    `Bạn có chắc chắn muốn đặt hàng với tổng thanh toán ${VND(total.value)}?`,
    { title: "Xác nhận đặt hàng", confirmText: "Đặt hàng", cancelText: "Hủy" }
  )
  if (!ok) return

  errorMsg.value = ""
  submitting.value = true

  try {
    const orderId = `DH${Date.now()}`
    const normalizedPaymentMethod = paymentMethod.value === "COD" ? "COD" : "VNPAY"
    const itemVoucherPayload = []

    localStorage.setItem(
      "currentOrder",
      JSON.stringify({
        version: 2,
        id: orderId,
        customerId: customerId.value,
        delivery: {
          name: delivery.value.name.trim(),
          phone: delivery.value.phone.trim(),
          province: String(delivery.value.province || "").trim(),
          district: String(delivery.value.district || "").trim(),
          ward: String(delivery.value.ward || "").trim(),
          addressDetail: String(delivery.value.addressDetail || "").trim(),
          address: deliveryAddress.value
        },
        shipping: shipping.value,
        discount: voucherDiscount.value,
        voucher: selectedOrderVoucher.value,
        itemVouchers: itemVoucherPayload,
        items: cart.value,
        total: total.value,
        paymentMethod: normalizedPaymentMethod,
        vnpayChannel: vnpayChannel.value,
        createdAt: new Date().toISOString()
      })
    )

    if (normalizedPaymentMethod === "COD") {
      try {
        const codResult = await createBackendCheckoutOrder({
          cartItems: cart.value,
          delivery: {
            name: delivery.value.name.trim(),
            phone: delivery.value.phone.trim(),
            province: String(delivery.value.province || "").trim(),
            district: String(delivery.value.district || "").trim(),
            ward: String(delivery.value.ward || "").trim(),
            addressDetail: String(delivery.value.addressDetail || "").trim(),
            address: deliveryAddress.value
          },
          shipping: shipping.value,
          discount: voucherDiscount.value,
          total: total.value,
          paymentMethod: "COD",
          voucherCode: String(selectedOrderVoucher.value?.maPhieuGiamGia || "")
        })

        // Use real maHoaDon from backend if available
        const backendMaHoaDon = codResult?.order?.maHoaDon || ""
        const backendOrderId = codResult?.orderId || ""
        if (backendMaHoaDon || backendOrderId) {
          const saved = JSON.parse(localStorage.getItem("currentOrder") || "{}")
          saved.id = backendMaHoaDon || backendOrderId
          saved.maHoaDon = backendMaHoaDon
          saved.backendId = backendOrderId
          localStorage.setItem("currentOrder", JSON.stringify(saved))

          persistOrderVoucherSnapshot({
            keys: [saved.id, saved.maHoaDon, saved.backendId],
            itemVouchers: itemVoucherPayload,
          })
        }
      } catch (backendError) {
        throw backendError
      }

      localStorage.removeItem("currentOrder")
      clearCartStorage()
      toast.success("Đặt hàng COD thành công")
      router.push("/trang-chu")
      return
    }

    const res = await createVnpayPayment({
      amount: total.value,
      orderId,
      bankCode: resolveBankCode()
    })

    const url = res?.data?.url
    if (!url) {
      throw new Error("Không tạo được link thanh toán VNPay")
    }

    window.location.href = url
  } catch (error) {
    console.error("Checkout failed:", error)
    errorMsg.value = error?.response?.data?.message || error.message || "Không thể đặt hàng. Vui lòng thử lại."
    toast.error(errorMsg.value)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  cleanLegacyVoucherStorage()
  const res = await fetch("https://provinces.open-api.vn/api/?depth=1")
  provinces.value = await res.json()


  cart.value.forEach((item) => normalizeCartItemQuantity(item))
  persistCheckoutCart()
  shippingSpec.value.weightGram = estimatedWeightByCart.value
  await Promise.all([loadProfile(), loadVouchers()])
  await loadVariantCatalog()
  loadItemVoucherSelections()
  mergePendingProductDetailVouchers()

  const existingKeys = new Set(cart.value.map((item) => getItemKey(item)))
  const filteredSelections = Object.fromEntries(
    Object.entries(itemVoucherSelections.value || {}).filter(([key]) => existingKeys.has(key))
  )
  itemVoucherSelections.value = filteredSelections
  persistItemVoucherSelections()

  await fetchShippingQuote()

  // Pre-apply voucher selected from ProductDetail page (order-level voucher)
  const shouldOpenVoucherDrawer = String(route.query?.openVoucher || "") === "1"
  const savedVoucher = localStorage.getItem('checkoutSelectedVoucher')
  if (savedVoucher) {
    try {
      localStorage.removeItem('checkoutSelectedVoucher')
      const preSelected = JSON.parse(savedVoucher)
      const voucherSeed = preSelected?.voucher || preSelected
      const found = vouchers.value.find(v =>
        v.id === voucherSeed?.id || v.maPhieuGiamGia === voucherSeed?.maPhieuGiamGia
      )

      if (found && isVoucherApplicable(found, subtotal.value, customerId.value)) {
        selectedOrderVoucher.value = found
        voucherCode.value = found.maPhieuGiamGia
        voucherHint.value = `Đã áp dụng ${found.maPhieuGiamGia} cho toàn bộ đơn hàng`
      }
    } catch {}
  }

  if (shouldOpenVoucherDrawer) {
    openCheckoutVoucherDrawer()
    router.replace({ path: "/checkout" })
  }

  autoSelectBestVoucher(true)
})

watch(itemVoucherSelections, persistItemVoucherSelections, { deep: true })

watch(estimatedWeightByCart, (nextWeight) => {
  if (!shippingSpec.value.manualWeight) {
    shippingSpec.value.weightGram = nextWeight
  }
})

watch(
  () => [
    shippingProvider.value,
    delivery.value.province,
    delivery.value.district,
    delivery.value.ward,
    shippingSpec.value.weightGram,
    shippingSpec.value.lengthCm,
    shippingSpec.value.widthCm,
    shippingSpec.value.heightCm,
    cartCount.value
  ],
  () => {
    fetchShippingQuote()
  }
)

watch(
  () => [subtotal.value, vouchers.value.length, customerId.value],
  () => {
    autoSelectBestVoucher()
  }
)

</script>

<template>
  <div class="checkout-page page-shell">
    <SiteNav />

    <main class="checkout-shell">
      <section class="checkout-left">
        <div class="section-card">
          <div class="section-head">
            <h2>Thông tin đơn hàng</h2>
          </div>

          <div class="form-grid single">
            <input v-model="delivery.name" type="text" placeholder="Họ và tên" />
            <input v-model="delivery.phone" type="text" placeholder="Số điện thoại" />

            <!-- Saved address list -->
            <div v-if="savedAddresses.length" class="saved-addresses">
              <div class="saved-address-list">
                <button
                  v-for="(addr, i) in savedAddresses"
                  :key="addr.id"
                  type="button"
                  class="saved-address-btn"
                  :class="{ active: selectedSavedAddress?.id === addr.id }"
                  @click="applyAddress(addr)"
                >
                  {{ [addr.diaChiCuThe, addr.phuongXa, addr.quanHuyen, addr.tinhThanh].filter(Boolean).join(', ') }}
                </button>
              </div>
              <button
                v-if="selectedSavedAddress"
                type="button"
                class="btn-link-small"
                @click="clearAddressForManualEntry"
              >Nhập địa chỉ khác</button>
              <button
                v-if="!selectedSavedAddress"
                type="button"
                class="btn-link-small"
                @click="revertToDefaultAddress"
              >Quay lại địa chỉ mặc định</button>
            </div>

            <!-- Address dropdowns + detail — only visible when no saved address selected -->
            <template v-if="!selectedSavedAddress">
            <div class="address-row">
              <select v-model="delivery.province" @change="onProvinceChange">
                <option value="">Chọn Tỉnh / Thành phố</option>
                <option v-for="p in provinces" :key="p.code" :value="p.name">
                  {{ p.name }}
                </option>
              </select>

              <select v-model="delivery.district" @change="onDistrictChange">
                <option value="">Chọn Quận / Huyện</option>
                <option v-for="d in districts" :key="d.code" :value="d.name">
                  {{ d.name }}
                </option>
              </select>

              <select v-model="delivery.ward" @change="onWardChange">
                <option value="">Chọn Phường / Xã</option>
                <option v-for="w in wards" :key="w.code" :value="w.name">
                  {{ w.name }}
                </option>
              </select>
            </div>

            <input
              v-model="delivery.addressDetail"
              type="text"
              placeholder="Địa chỉ cụ thể (số nhà, tên đường)"
            />
            </template>
          </div>

          <div class="address-note" v-if="loadingProfile">Đang tải thông tin giao hàng...</div>
        </div>

        <div class="section-card">
          <h3>Phương thức vận chuyển</h3>
          <label class="option-card option-card-active">
            <div class="option-main">
              <span class="option-icon"><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2"/><path d="M15 18h2a1 1 0 0 0 1-1v-3.28a1 1 0 0 0-.684-.948l-1.923-.641a1 1 0 0 1-.684-.948V8a2 2 0 0 1 2-2h1.382a1 1 0 0 1 .894.553l1.448 2.894A1 1 0 0 0 21.382 10H22a1 1 0 0 1 1 1v6a1 1 0 0 1-1 1h-1"/><circle cx="7" cy="18" r="2"/><circle cx="19" cy="18" r="2"/></svg></span>
              <div>
                <strong>Đơn vị vận chuyển</strong>
                <p>{{ shippingLoading ? 'Đang tính phí vận chuyển...' : `Phí vận chuyển ${VND(shipping)}` }}</p>
              </div>
            </div>
          </label>

          <div class="shipping-grid">
            <select v-model="shippingProvider">
              <option value="GHN">GHN (Giao Hàng Nhanh)</option>
              <option value="GHTK">GHTK (Giao Hàng Tiết Kiệm)</option>
            </select>
          </div>

          <div class="shipping-auto-note">
            Phí vận chuyển được tính tự động theo địa chỉ giao hàng, khối lượng ước tính từ giỏ hàng và quy cách đóng gói mặc định.
          </div>

          <div class="address-note" v-if="shippingQuote">
            <div>Khoảng cách dự kiến: {{ shippingQuote.distanceKm }} km</div>
            <div>
              Trọng lượng tính phí: {{ shippingQuote.packageInfo?.chargeableWeightKg || 0 }} kg
              (thực: {{ shippingQuote.packageInfo?.actualWeightKg || 0 }} kg, quy đổi: {{ shippingQuote.packageInfo?.volumetricWeightKg || 0 }} kg)
            </div>
          </div>
          <div class="error-box" v-if="shippingError">{{ shippingError }}</div>
        </div>

        <div class="section-card">
          <h3>Hình thức thanh toán</h3>

          <label class="payment-card payment-card-cod" :class="{ active: paymentMethod === 'COD' }">
            <input type="radio" name="paymentMethod" value="COD" v-model="paymentMethod" />
            <div class="payment-badge payment-badge-cod">COD</div>
            <div class="payment-copy">
              <strong>Thanh toán khi giao hàng (COD)</strong>
              <p>- Khách hàng được kiểm tra hàng trước khi nhận hàng.</p>
            </div>
          </label>

          <label class="payment-card payment-card-vnpay" :class="{ active: paymentMethod === 'VNPAY' }">
            <input type="radio" name="paymentMethod" value="VNPAY" v-model="paymentMethod" />
            <div class="payment-badge payment-badge-vnpay">VNPAY</div>
            <div class="payment-copy">
              <strong>Ví điện tử VNPay</strong>
              <p>Thanh toán qua cổng VNPay (QR/ATM/Internet Banking).</p>
            </div>
          </label>

          <div class="payment-card payment-card-momo payment-disabled">
            <input type="radio" name="paymentMethod" disabled />
            <div class="payment-badge payment-badge-momo">MOMO</div>
            <div class="payment-copy">
              <strong>Thanh toán MoMo</strong>
              <p>Tính năng sắp ra mắt.</p>
            </div>
          </div>

          <div v-if="paymentMethod === 'VNPAY'" class="vnpay-channel-row">
            <label>
              <input type="radio" name="vnpayChannel" value="VNPAYQR" v-model="vnpayChannel" /> VNPay QR Code
            </label>
            <label>
              <input type="radio" name="vnpayChannel" value="ATM" v-model="vnpayChannel" /> ATM / Internet Banking
            </label>
            <label>
              <input type="radio" name="vnpayChannel" value="INTCARD" v-model="vnpayChannel" /> Thẻ quốc tế
            </label>
            <label>
              <input type="radio" name="vnpayChannel" value="AUTO" v-model="vnpayChannel" /> Tự chọn tại cổng VNPay
            </label>
          </div>
        </div>

        <div v-if="errorMsg" class="error-box">{{ errorMsg }}</div>
      </section>

      <aside class="checkout-right">
        <div class="cart-panel">
          <div class="cart-panel-head">
            <h2>Giỏ hàng</h2>
            <span>{{ cartCount }} sản phẩm</span>
          </div>

          <div v-if="!cart.length" class="empty-cart">Giỏ hàng đang trống</div>

          <div v-for="item in cart" :key="item.id + (item.size || '') + (item.color || '')" class="cart-item-icd">
            <img v-if="item.image" :src="item.image" :alt="item.name" class="cart-thumb" />
            <div v-else class="cart-thumb cart-thumb-fallback">SP</div>

            <div class="cart-body-icd">
              <div class="cart-top-icd">
                <strong>{{ item.name }}</strong>
                <button class="remove-btn" type="button" @click="removeCartItem(item)">×</button>
              </div>
              <div v-if="Number(item.dotGiamGiaPhanTram || 0) > 0" class="campaign-tag">
                🏷️ Đợt khuyến mãi -{{ Math.round(item.dotGiamGiaPhanTram) }}%
              </div>

              <div class="variant-line">
                <select class="variant-select" :value="item.color || ''" @change="updateItemColor(item, $event.target.value)">
                  <option value="" disabled>Chọn màu</option>
                  <option v-for="color in getColorOptions(item)" :key="`${item.id}-color-${color}`" :value="color">
                    Màu: {{ color }}
                  </option>
                </select>
                <select class="variant-select" :value="item.size || ''" @change="updateItemSize(item, $event.target.value)">
                  <option value="" disabled>Chọn size</option>
                  <option v-for="size in getSizeOptions(item)" :key="`${item.id}-size-${size}`" :value="size">
                    Size: {{ size }}
                  </option>
                </select>
              </div>

              <div class="qty-price-row">
                <div class="qty-chip">
                  <span class="qty-chip__label">Số lượng</span>
                  <b>{{ item.quantity }}</b>
                </div>
                <div class="cart-price-wrap">
                  <div class="cart-price">{{ VND(getLineSubtotal(item)) }}</div>
                </div>
              </div>
            </div>
          </div>

          <section class="order-voucher-box">
            <div class="order-voucher-head">
              <h3>Ưu đãi dành cho bạn</h3>
              <div class="order-voucher-actions">
                <button type="button" class="order-voucher-open" @click="openCheckoutVoucherDrawer()">Voucher chi tiết</button>
                <button
                  v-if="selectedOrderVoucher"
                  type="button"
                  class="order-voucher-clear"
                  @click="clearGlobalVoucher"
                >Bỏ voucher</button>
              </div>
            </div>
            <div class="voucher-picker">
              <button
                v-for="v in displayVouchers"
                :key="v.id || v.maPhieuGiamGia"
                type="button"
                class="voucher-chip"
                :class="{ 'voucher-chip-selected': selectedOrderVoucher?.maPhieuGiamGia === v.maPhieuGiamGia }"
                @click="selectVoucherCard(v)"
              >
                <span v-if="(v.id === bestVoucherId || v.maPhieuGiamGia === bestVoucherId)" class="vc-badge vc-badge-best">LỰA CHỌN TỐT NHẤT</span>
                <span v-else class="vc-badge">ƯU ĐÃI ONLINE</span>
                <div class="vc-code">{{ checkoutVoucherChipLabel(v) }}</div>
                <div class="vc-desc">{{ getVoucherDescription(v) }}</div>
                <div class="vc-expiry">HSD: {{ formatExpiry(v.ngayKetThuc) }}</div>
              </button>
            </div>
            <p v-if="voucherHint" class="order-voucher-hint">{{ voucherHint }}</p>
          </section>

          <div class="checkout-totals">
            <div class="checkout-total-row">
              <span>Tạm tính:</span>
              <span class="subtotal-value">
                {{ VND(subtotal) }}
                <small v-if="voucherDiscount > 0" class="saving-inline">Tiết kiệm: {{ VND(voucherDiscount) }}</small>
              </span>
            </div>
            <div class="checkout-total-row">
              <span>Phí vận chuyển:</span>
              <span class="free-label">{{ shippingLoading ? 'Đang tính...' : VND(shipping) }}</span>
            </div>
            <div class="checkout-total-row muted">
              <span>Voucher giảm giá:</span>
              <span>-{{ VND(voucherDiscount) }}</span>
            </div>
            <div class="checkout-grand-total">
              <span>Tổng:</span>
              <div class="grand-right">
                <strong>{{ VND(total) }}</strong>
                <div class="savings-note">(Đã giảm {{ VND(voucherDiscount) }} trên giá gốc)</div>
              </div>
            </div>
          </div>

          <button class="submit-btn" :disabled="!canSubmit" @click="submitOrder">
            <span v-if="submitting">Đang xử lý...</span>
            <span v-else>Thanh toán</span>
          </button>
        </div>
      </aside>
    </main>

    <CustomerFooter />

    <!-- Voucher Selector Drawer -->
    <transition name="co-drawer">
      <div v-if="checkoutVoucherDrawerOpen" class="co-drawer-overlay" @click.self="closeCheckoutVoucherDrawer">
        <aside class="co-drawer">
          <div class="co-drawer__head">
            <div>
              <small>Voucher giảm giá - áp dụng online</small>
              <h3>Chọn voucher cho toàn đơn hàng</h3>
              <p class="co-drawer__target">Tạm tính đơn: {{ VND(subtotal) }}</p>
            </div>
            <button type="button" class="co-drawer__close" @click="closeCheckoutVoucherDrawer">✕</button>
          </div>
          <div class="co-drawer__list">
            <article
              v-for="v in vouchers"
              :key="v.id || v.maPhieuGiamGia"
              class="co-drawer__voucher"
              :class="{ 'is-selected': selectedOrderVoucher?.maPhieuGiamGia === v.maPhieuGiamGia }"
            >
              <div class="co-drawer__info">
                <span class="co-drawer__label">{{ v.tenPhieuGiamGia || 'Voucher' }}</span>
                <strong class="co-drawer__amount">{{ VND(calculateVoucherDiscount(v, subtotal)) }}</strong>
                <p class="co-drawer__min">Đơn từ {{ VND(v.hoaDonToiThieu || 0) }}</p>
                <p v-if="v.soTienGiamToiDa > 0" class="co-drawer__min">Giảm tối đa {{ VND(v.soTienGiamToiDa) }}</p>
                <p class="co-drawer__min">Còn {{ v.soLuongSuDung ?? 0 }} • Đã dùng {{ v.soLuongDaDung ?? 0 }}</p>
                <p class="co-drawer__code">Mã: <b>{{ v.maPhieuGiamGia }}</b></p>
              </div>
              <button
                type="button"
                class="co-apply-btn"
                :class="{ 'is-applied': selectedOrderVoucher?.maPhieuGiamGia === v.maPhieuGiamGia }"
                @click="applyGlobalVoucher(v)"
              >
                {{ selectedOrderVoucher?.maPhieuGiamGia === v.maPhieuGiamGia ? '✓ Đã chọn' : 'Áp dụng' }}
              </button>
            </article>
            <p v-if="!vouchers.length" class="co-drawer__empty">Hiện không có phiếu giảm giá đang hoạt động.</p>
          </div>
        </aside>
      </div>
    </transition>

  </div>
</template>

<style scoped>
@import "./home.css";

.page-shell {
  --bg: #f7f8fc;
}

.checkout-page {
  min-height: 100vh;
  background: var(--bg);
  color: #111827;
  font-family: "Be Vietnam Pro", "Segoe UI", Tahoma, sans-serif;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: static;
}

.menu {
  gap: 12px;
}

.breadcrumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.crumb {
  border: none;
  background: none;
  color: inherit;
  cursor: pointer;
}

.crumb-active {
  color: #991b1b;
  font-weight: 700;
}

.back-home {
  margin-left: auto;
  color: #b91c1c;
  font-weight: 700;
}

.checkout-breadcrumb-wrap {
  max-width: 1280px;
  margin: 8px auto 0;
  padding: 0 24px;
}

.checkout-breadcrumb-inner {
  padding: 8px 2px 0;
}

.breadcrumbs-standalone {
  margin: 0;
}

.checkout-shell {
  max-width: 1280px;
  margin: 0 auto;
  padding: 18px 24px 40px;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(340px, 0.95fr);
  gap: 20px;
  align-items: start;
}

.section-card,
.cart-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 22px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.05);
  transition: box-shadow 0.2s;
}

.section-card + .section-card {
  margin-top: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-card h2,
.section-card h3,
.cart-panel h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.autofill-badge {
  background: #fff1f2;
  color: #b91c1c;
  border: 1px solid #fecdd3;
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 999px;
}

.form-grid.single {
  display: grid;
  gap: 10px;
}

.form-grid input {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  min-height: 44px;
  padding: 10px 14px;
  font-size: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-grid input:focus,
.shipping-grid input:focus,
.shipping-grid select:focus,
.address-row select:focus {
  outline: none;
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.12);
}

.shipping-grid {
  margin-top: 10px;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
}

.shipping-grid input,
.shipping-grid select {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 14px;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.shipping-auto-note {
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.address-note {
  margin-top: 10px;
  color: #6b7280;
  font-size: 12px;
}

.option-card,
.payment-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  border: 1.5px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
  cursor: pointer;
}

.option-card:hover,
.payment-card:hover {
  border-color: #d1d5db;
}

.option-card-active,
.payment-card.active {
  border-color: #dc2626;
  box-shadow: 0 0 0 1px #dc2626;
  background: #fff;
}

.payment-card + .payment-card {
  margin-top: 12px;
}

.payment-card-cod {
  border-color: #fecaca;
}

.payment-card-cod.active {
  border-color: #b91c1c;
  box-shadow: 0 0 0 1px #b91c1c;
}

.payment-card-vnpay.active {
  border-color: #dc2626;
  box-shadow: 0 0 0 1px #dc2626;
}

.payment-card-momo {
  margin-top: 12px;
}

.payment-disabled {
  opacity: 0.68;
}

.payment-card input,
.option-card input {
  margin-top: 4px;
}

.option-main {
  display: flex;
  gap: 12px;
}

.option-icon {
  font-size: 20px;
  display: flex;
  align-items: center;
  color: #111;
}

.option-main p,
.payment-copy p {
  margin: 4px 0 0;
  color: #475569;
  font-size: 13px;
}

.payment-badge {
  min-width: 44px;
  height: 24px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 800;
  color: #fff;
}

.payment-badge-cod {
  background: #b91c1c;
}

.payment-badge-vnpay {
  background: #dc2626;
}

.payment-badge-momo {
  background: #be185d;
}

.qr-tag {
  font-size: 11px;
  color: #dc2626;
}

.vnpay-channel-row {
  margin-top: 10px;
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #334155;
}

.error-box {
  margin-top: 16px;
  padding: 12px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 6px;
  color: #b91c1c;
  font-size: 14px;
}

.checkout-right {
  position: sticky;
  top: 16px;
}

.cart-panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.cart-panel-head span {
  color: #6b7280;
  font-size: 13px;
}

.cart-progress-line {
  height: 5px;
  background: #fee2e2;
  border-radius: 999px;
  margin-bottom: 8px;
}

.cart-progress-line span {
  display: block;
  width: 42%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #dc2626, #b91c1c);
}

.cart-offer {
  background: linear-gradient(90deg, #dc2626, #b91c1c);
  color: #fff;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 14px;
  line-height: 1.35;
}

.promo-box {
  border: 1px solid #dbe2ec;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 16px;
}

.promo-chips-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(92px, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.promo-chip {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  width: 100%;
  padding: 0 11px;
  border: 1px solid #ef4444;
  border-radius: 8px;
  color: #b91c1c;
  background: #ffffff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.promo-chip.is-selected {
  background: #b91c1c;
  border-color: #b91c1c;
  color: white;
}

/* ── Checkout Voucher Drawer ── */
.co-drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  justify-content: flex-end;
  background: rgba(17, 24, 39, 0.32);
}

.co-drawer {
  width: min(360px, 100%);
  height: 100%;
  padding: 18px;
  background: white;
  box-shadow: -20px 0 40px rgba(0, 0, 0, 0.12);
  overflow-y: auto;
}

.co-drawer__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.co-drawer__head small {
  color: #b91c1c;
  font-size: 11px;
  text-transform: uppercase;
}

.co-drawer__head h3 {
  margin: 4px 0 0;
  font-size: 18px;
}

.co-drawer__target {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.co-drawer__close {
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  background: white;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
}

.co-drawer__list {
  display: grid;
  gap: 12px;
}

.co-drawer__voucher {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px;
  border: 1px solid #f0dde1;
  background: #fff7f8;
  border-radius: 6px;
}

.co-drawer__voucher.is-selected {
  border-color: #b91c1c;
  background: #fff1f2;
}

.co-drawer__info {
  flex: 1;
  min-width: 0;
}

.co-drawer__label {
  display: block;
  color: #b91c1c;
  font-size: 11px;
  text-transform: uppercase;
  font-weight: 700;
}

.co-drawer__amount {
  display: block;
  margin-top: 4px;
  color: #991b1b;
  font-size: 20px;
}

.co-drawer__min,
.co-drawer__code {
  margin: 2px 0 0;
  color: #4b5563;
  font-size: 12px;
}

.co-drawer__code b { color: #991b1b; }

.co-apply-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  background: #991b1b;
  color: white;
  font-size: 12px;
  font-weight: 600;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
}

.co-apply-btn.is-applied { background: #059669; }

.co-drawer__empty {
  color: #6b7280;
  font-size: 13px;
  text-align: center;
  padding: 16px;
}

.co-drawer-enter-active,
.co-drawer-leave-active {
  transition: opacity 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.co-drawer-enter-active .co-drawer,
.co-drawer-leave-active .co-drawer {
  transition: transform 0.32s cubic-bezier(0.4, 0, 0.2, 1);
}

.co-drawer-enter-from,
.co-drawer-leave-to { opacity: 0; }

.co-drawer-enter-from .co-drawer,
.co-drawer-leave-to .co-drawer {
  transform: translateX(100%);
}

.promo-title {
  font-size: 22px;
  font-weight: 800;
  margin-bottom: 10px;
}

.promo-row {
  display: grid;
  grid-template-columns: 1fr 170px;
  gap: 10px;
}

.promo-select-row {
  margin-top: 8px;
}

.promo-select-btn {
  width: 100%;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  padding: 10px 12px;
  font-size: 14px;
  font-weight: 700;
  background: #fff;
  color: #0f172a;
  cursor: pointer;
}

.promo-select-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.promo-row input,
.promo-row button {
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  padding: 9px 12px;
  font-size: 14px;
}

.promo-row button {
  background: #b91c1c;
  color: #fff;
  font-weight: 700;
}

.promo-box small {
  display: block;
  margin-top: 8px;
  font-size: 12px;
}

.promo-ok {
  color: #047857;
}

.promo-error {
  color: #dc2626;
}

.promo-muted {
  color: #6b7280;
}

.voucher-picker {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 4px 0 6px;
}

.voucher-chip {
  min-width: 240px;
  border: 1px solid #fecaca;
  border-radius: 12px;
  padding: 12px 14px;
  background: linear-gradient(180deg, #fff5f5 0%, #fff 100%);
  cursor: pointer;
  position: relative;
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.voucher-chip:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(185, 28, 28, 0.12);
}

.voucher-chip-selected {
  border-color: #dc2626;
  box-shadow: inset 0 0 0 1px #dc2626, 0 8px 20px rgba(185, 28, 28, 0.16);
}

.order-voucher-box {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e5e7eb;
}

.order-voucher-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  gap: 10px;
}

.order-voucher-head h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.order-voucher-clear {
  border: 0;
  background: transparent;
  color: #b91c1c;
  font-weight: 600;
  cursor: pointer;
}

.order-voucher-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-voucher-open {
  border: 1px solid #fecaca;
  border-radius: 999px;
  background: #fff;
  color: #991b1b;
  font-weight: 700;
  padding: 6px 12px;
  cursor: pointer;
}

.order-voucher-open:hover {
  border-color: #dc2626;
}

.order-voucher-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #374151;
}

.vc-badge {
  position: absolute;
  top: 6px;
  right: 8px;
  font-size: 9px;
  font-weight: 800;
  color: #fff;
  background: #ef4444;
  border-radius: 4px;
  padding: 2px 6px;
  max-width: calc(100% - 16px);
  white-space: nowrap;
}

.vc-badge-best {
  background: #16a34a;
}

.vc-code {
  font-size: 30px;
  font-weight: 800;
  line-height: 1.1;
  margin: 20px 0 4px;
  color: #991b1b;
}

.vc-desc {
  font-size: 14px;
  color: #475569;
}

.vc-expiry {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.cart-item-icd {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #edf2f7;
}

.cart-body-icd {
  min-width: 0;
}

.cart-top-icd {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.cart-top-icd strong {
  font-size: 17px;
  line-height: 1.35;
}

.remove-btn {
  border: none;
  background: transparent;
  color: #111827;
  font-size: 24px;
  line-height: 1;
  font-weight: 700;
  cursor: pointer;
}

.delivery-tag {
  display: inline-flex;
  margin-top: 6px;
  border: 1px solid #fca5a5;
  color: #ef4444;
  background: #fff5f5;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
}

.campaign-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  border: 1px solid #dc2626;
  color: #b91c1c;
  background: #fff1f2;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
}

.variant-line {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.item-voucher-row {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.item-voucher-btn {
  flex: 1;
  min-height: 32px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fff7f7;
  color: #b91c1c;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.item-voucher-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.item-voucher-clear {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  min-height: 32px;
  padding: 0 10px;
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.item-voucher-detail {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  border-radius: 8px;
  background: #fff5f5;
  border: 1px dashed #fca5a5;
}

.item-voucher-code {
  font-size: 12px;
  font-weight: 800;
  color: #b91c1c;
}

.variant-select {
  min-width: 120px;
  min-height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 0 34px 0 10px;
  background-color: #f8fafc;
  color: #475569;
  font-size: 13px;
}

.qty-price-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.qty-chip {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  min-height: 32px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #475569;
  font-size: 14px;
  flex-shrink: 0;
}

.qty-chip b {
  min-width: 2.4ch;
  text-align: center;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.qty-chip__label {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.cart-price-wrap {
  text-align: right;
}

.qty-btn {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #111827;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-weight: 700;
  line-height: 1;
}

.empty-cart {
  color: #64748b;
  padding: 20px 0;
}

.cart-thumb {
  width: 128px;
  height: 160px;
  object-fit: cover;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.cart-thumb-fallback {
  display: grid;
  place-items: center;
  background: #f1f5f9;
  color: #94a3b8;
  font-weight: 700;
}

.cart-price {
  font-size: 24px;
  font-weight: 700;
}

.cart-price--discounted {
  color: #b91c1c;
}

.cart-price-before {
  margin-top: 2px;
  color: #9ca3af;
  font-size: 12px;
  text-decoration: line-through;
}

.checkout-totals {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #e5e7eb;
}

.checkout-total-row,
.checkout-grand-total {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  font-size: 15px;
}

.checkout-total-row.muted {
  color: #4b5563;
}

.subtotal-value {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.2;
}

.saving-inline {
  display: block;
  margin-top: 3px;
  font-size: 11px;
  color: #ef4444;
}

.free-label {
  color: #0f172a;
  font-weight: 500;
}

.checkout-grand-total {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #e5e7eb;
  font-size: 18px;
}

.grand-right {
  text-align: right;
}

.checkout-grand-total strong {
  color: #111827;
}

.savings-note {
  font-size: 10px;
  color: #ef4444;
}

.payment-card-cod .payment-copy p:first-of-type {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #d1d5db;
}

.submit-btn {
  width: 100%;
  margin-top: 16px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: #fff;
  padding: 14px 16px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(185, 28, 28, 0.25);
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
}

.submit-btn:hover:not(:disabled) {
  box-shadow: 0 6px 20px rgba(185, 28, 28, 0.35);
  transform: translateY(-1px);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.profile-wrapper {
  position: relative;
}

.user-account-btn {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 12px;
  min-height: 42px;
  padding: 6px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.profile-avatar-chip {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
  background: #f1f5f9;
  overflow: hidden;
}

.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-identity {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.15;
}

.profile-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text);
}

.profile-role {
  font-size: 11px;
  color: var(--muted);
}

.profile-wrapper::after {
  content: "";
  position: absolute;
  top: 100%;
  right: 0;
  width: 220px;
  height: 14px;
}

.profile-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  padding: 8px;
  display: flex;
  flex-direction: column;
  min-width: 180px;
  z-index: 100;
  border: 1px solid var(--line);
  animation: fadeInMenu 0.16s ease;
}

@keyframes fadeInMenu {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dropdown-item {
  padding: 12px 14px;
  border: none;
  background: none;
  text-align: left;
  font-size: 14px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text);
  font-weight: 500;
  transition: all 0.2s;
}

.dropdown-item:hover {
  background: #f3f4f6;
}

.dropdown-item.danger {
  color: #dc2626;
}

.dropdown-item.danger:hover {
  background: #fee2e2;
}

@media (max-width: 1024px) {
  .checkout-shell {
    grid-template-columns: 1fr;
  }

  .checkout-right {
    position: static;
  }
}

@media (max-width: 768px) {
  header .container,
  .checkout-shell,
  .checkout-breadcrumb-wrap {
    padding-left: 16px;
    padding-right: 16px;
  }

  .actions .search {
    display: none;
  }

  .promo-title {
    font-size: 24px;
  }

  .promo-row {
    grid-template-columns: 1fr;
  }

  .vc-code {
    font-size: 18px;
  }

  .vc-desc,
  .vc-expiry {
    font-size: 13px;
  }

  .checkout-total-row {
    font-size: 13px;
  }

  .checkout-grand-total {
    font-size: 18px;
  }

  .cart-item-icd {
    grid-template-columns: 92px minmax(0, 1fr);
  }

  .cart-thumb {
    width: 92px;
    height: 116px;
  }

  .cart-top-icd strong,
  .cart-price,
  .qty-chip,
  .variant-select,
  .delivery-tag {
    font-size: 13px;
  }
}

.saved-addresses {
  margin-top: 8px;
}
.saved-address-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.saved-address-btn {
  position: relative;
  padding: 10px 16px;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  text-align: left;
  transition: border-color .2s, background .2s, box-shadow .2s;
}
.saved-address-btn:hover {
  border-color: #9ca3af;
  background: #f9fafb;
}
.saved-address-btn.active {
  border-color: #dc2626;
  background: #fff1f2;
  color: #111;
  font-weight: 500;
  box-shadow: 0 0 0 1px #dc2626;
}
.addr-default-tag {
  display: inline-block;
  font-size: 10px;
  font-weight: 600;
  color: #6b7280;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
  margin-right: 4px;
  vertical-align: middle;
}
.btn-link-small {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  color: #374151;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-link-small:hover {
  border-color: #111;
  background: #f9fafb;
  color: #111;
}
.edit-address-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #fef2f2;
  border-radius: 8px;
  font-size: 13px;
  color: #374151;
}

.address-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 8px;
}

.address-row select {
  width: 100%;
  min-height: 44px;
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s;
}
@media (max-width: 768px) {
  .address-row {
    flex-direction: column;
  }
  .saved-address-list {
    flex-direction: column;
  }
}
</style>

