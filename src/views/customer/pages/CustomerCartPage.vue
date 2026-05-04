<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue"
import { useRouter } from "vue-router"
import { Minus, Plus, Trash2, ShoppingBag, ChevronRight, X, Pencil, Ticket } from "lucide-vue-next"
import SiteNav from "../../../components/SiteNav.vue"
import CustomerFooter from "../../../components/customer/CustomerFooter.vue"
import { getAllSanPham } from "../../../services/sanPhamService"
import { applyCampaignPriceToVariants } from "../../../services/campaignPricingService"
import {
  normalizeVoucherData,
  isVoucherApplicable,
  calculateVoucherDiscount,
  getActiveVouchers,
  getAllVouchers,
} from "../../../services/khuyenMaiService"
import {
  readCartObject,
  writeCartObject,
  readCartVariantsObject,
  writeCartVariantsObject,
  writeCheckoutCartArray
} from "../../../utils/cartStorage"
import { cleanLegacyVoucherStorage } from "../../../utils/voucherSelectionStorage"
import { resolveApiOrigin } from "../../../utils/apiOrigin"
import { getProductImageOverride } from "../../../utils/productImageOverrides"
import { fallbackImageForVariant } from "../../../utils/productImageFallback"

const router = useRouter()

const CART_UPDATED_EVENT = "dirtywave:cart-updated"
const year = new Date().getFullYear()
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")

const cart = ref({})
const cartVariants = ref({})
const selectedLineKeys = ref([])
const loadingProducts = ref(false)
const backendProducts = ref([])
const checkoutItemVoucherSelections = ref({})
const pendingItemVoucherSelections = ref({})
const vouchers = ref([])
const loadingVouchers = ref(false)

const CHECKOUT_ITEM_VOUCHER_SELECTIONS_KEY = "checkoutItemVoucherSelections"
const CHECKOUT_PENDING_ITEM_VOUCHERS_KEY = "checkoutPendingItemVoucherSelections"

const toNumber = (value) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const formatVND = (value) => new Intl.NumberFormat("vi-VN").format(toNumber(value)) + "₫"

const pickImageValue = (entry) => {
  if (!entry) return ""
  if (typeof entry === "string") return entry
  if (Array.isArray(entry)) {
    for (const child of entry) {
      const found = pickImageValue(child)
      if (found) return found
    }
    return ""
  }
  if (typeof entry === "object") {
    const keys = ["anh", "hinhAnh", "image", "imageUrl", "url", "duongDan", "duongDanAnh", "link", "path"]
    for (const key of keys) {
      const found = pickImageValue(entry[key])
      if (found) return found
    }
  }
  return ""
}

const normalizeImage = (value) => {
  const raw = String(value || "").trim()
  if (!raw) return ""
  if (/^https?:\/\//i.test(raw) || /^data:image\//i.test(raw)) return raw
  const normalized = raw.replace(/\\/g, "/")
  // Source paths under src/assets are not directly reachable at runtime URL form.
  if (normalized.startsWith("assets/") || normalized.startsWith("/assets/")) return ""
  const uploadsMatch = normalized.match(/(?:^|\/)(uploads\/.*)$/i)
  if (uploadsMatch?.[1]) return `${BACKEND_ORIGIN}/${uploadsMatch[1]}`
  if (normalized.startsWith("/uploads/")) return `${BACKEND_ORIGIN}${normalized}`
  if (normalized.startsWith("uploads/")) return `${BACKEND_ORIGIN}/${normalized}`
  if (normalized.startsWith("img/")) return `/${normalized}`
  if (normalized.startsWith("/")) return normalized
  return normalized
}

const isCuratedCatalogCode = (value = "") => /^SP0*(?:[1-9]|1\d|20)$/i.test(String(value || "").trim())

const isActiveVariantStatus = (value = "") => {
  const normalized = String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
  return !normalized.includes("ngung") && !normalized.includes("inactive")
}

const normalizeBackendProduct = (item) => {
  const variants = Array.isArray(item?.sanPhamChiTiets)
    ? item.sanPhamChiTiets.filter((variant) => isActiveVariantStatus(variant?.trangThai || variant?.status))
    : []
  const variantPrices = variants.map((v) => toNumber(v?.giaBan)).filter((v) => v > 0)
  const price = variantPrices.length ? Math.min(...variantPrices) : toNumber(item?.giaBan || item?.gia || 0)
  const id = Number(item?.id)
  const code = String(item?.maSanPham || "")
  const overrideImage = getProductImageOverride({ id, maSanPham: code })[0]
  const imageCandidate = pickImageValue([
    item?.anh,
    item?.hinhAnh,
    item?.images,
    item?.image,
    item?.listAnh,
    item?.anhChinh,
    variants,
  ])
  const firstVariant = variants[0] || null
  const normalizedCode = String(code || "").trim().toUpperCase()
  const curatedPrimaryImage = isCuratedCatalogCode(normalizedCode)
    ? (fallbackImageForVariant({
        id,
        maSanPham: normalizedCode,
        tenSanPham: item?.tenSanPham || item?.name,
        tenMauSac: firstVariant?.mauSac?.tenMau,
        maChiTietSanPham: firstVariant?.ma || firstVariant?.maSanPhamChiTiet || "",
      }) || "")
    : ""

  return {
    id,
    code,
    name: String(item?.tenSanPham || item?.name || "Sản phẩm"),
    cat: String(item?.danhMuc?.tenDanhMuc || item?.loai?.tenLoai || "Thời trang nam"),
    price,
    img: normalizeImage(overrideImage || curatedPrimaryImage || imageCandidate) || fallbackImageForVariant({
      id,
      maSanPham: code,
      tenSanPham: item?.tenSanPham || item?.name,
      tenMauSac: firstVariant?.mauSac?.tenMau,
      maChiTietSanPham: firstVariant?.ma || firstVariant?.maSanPhamChiTiet,
    }),
    variants: variants.map((v) => ({
      spctId: v?.id,
      ma: v?.ma,
      color: String(v?.mauSac?.tenMau || "").trim(),
      size: String(v?.kichThuoc?.tenKichThuoc || "").trim(),
      image: normalizeImage(
        (isCuratedCatalogCode(normalizedCode)
          ? fallbackImageForVariant({
              id,
              maSanPham: normalizedCode,
              tenSanPham: item?.tenSanPham || item?.name,
              tenMauSac: v?.mauSac?.tenMau,
              maChiTietSanPham: v?.ma || v?.maSanPhamChiTiet || "",
            })
          : "")
        || pickImageValue([v, v?.anh, v?.hinhAnh, v?.image, v?.imageUrl, v?.duongDanAnh])
      ) || fallbackImageForVariant({
        id,
        maSanPham: code,
        tenSanPham: item?.tenSanPham || item?.name,
        tenMauSac: v?.mauSac?.tenMau,
        maChiTietSanPham: v?.ma || v?.maSanPhamChiTiet,
      }),
      price: Number(v?.giaBan || 0),
      basePrice: Number(v?.giaBanGoc || v?.giaBan || 0),
      campaignPercent: Number(v?.dotGiamGiaPhanTram || 0),
      stock: Number(v?.soLuong || 0),
    })),
  }
}

const parseCartKey = (key) => {
  const raw = String(key || "")
  const [idPart, colorPart = "", sizePart = "", voucherPart = ""] = raw.split("__")
  const id = Number(idPart)
  return {
    raw,
    idPart,
    id: Number.isFinite(id) ? id : 0,
    colorPart,
    sizePart,
    voucherPart: String(voucherPart || "").trim().toUpperCase(),
  }
}

const buildCartKey = (id, color = "", size = "", voucherCode = "") => {
  const base = String(id || "").trim()
  if (!base) return ""
  const c = String(color || "").trim()
  const s = String(size || "").trim()
  const voucher = String(voucherCode || "").trim().toUpperCase()
  return (c || s || voucher) ? `${base}__${c}__${s}__${voucher}` : base
}

const backendMap = computed(() => {
  const map = new Map()
  for (const product of backendProducts.value) {
    map.set(product.id, product)
  }
  return map
})

const readCart = () => {
  const stored = readCartObject()
  cart.value = stored && typeof stored === "object" ? stored : {}
}

const readCartVariants = () => {
  const stored = readCartVariantsObject()
  cartVariants.value = stored && typeof stored === "object" ? stored : {}
}

const writeCartVariants = () => {
  writeCartVariantsObject(cartVariants.value)
}

const getColorOptions = (id) => {
  const product = backendMap.value.get(Number(id))
  if (!product?.variants?.length) return []
  return [...new Set(product.variants.map((v) => v.color).filter(Boolean))]
}

const getSizeOptionsForModal = (id, color) => {
  const product = backendMap.value.get(Number(id))
  if (!product?.variants?.length) return []
  const filtered = color ? product.variants.filter((v) => v.color === color) : product.variants
  return [...new Set(filtered.map((v) => v.size).filter(Boolean))]
}

const findVariantByChoice = (product, color, size) => {
  const rows = Array.isArray(product?.variants) ? product.variants : []
  if (!rows.length) return null
  return rows.find((v) => v.color === color && v.size === size)
    || rows.find((v) => v.color === color)
    || rows.find((v) => v.size === size)
    || rows[0]
}

// â”€â”€ Detail Modal â”€â”€
const detailModal = ref(null)
const modalColor = ref("")
const modalSize = ref("")
const modalQty = ref(1)
const modalVoucherCode = ref("")

const openDetailModal = (line) => {
  detailModal.value = line
  modalColor.value = line.color || getColorOptions(line.id)[0] || ""
  modalSize.value = line.size || getSizeOptionsForModal(line.id, modalColor.value)[0] || ""
  modalQty.value = line.quantity
  modalVoucherCode.value = String(getVoucherForLine(line)?.maPhieuGiamGia || line.voucherCode || "")
  document.body.style.overflow = "hidden"
}

const closeDetailModal = () => {
  detailModal.value = null
  document.body.style.overflow = ""
}

const selectModalColor = (color) => {
  modalColor.value = color
  const sizes = getSizeOptionsForModal(detailModal.value?.id, color)
  if (sizes.length && !sizes.includes(modalSize.value)) {
    modalSize.value = sizes[0]
  }
}

const modalResolvedVariant = computed(() => {
  if (!detailModal.value) return null
  const product = backendMap.value.get(Number(detailModal.value.id))
  return findVariantByChoice(product, modalColor.value, modalSize.value)
})

const modalPreviewImage = computed(() => {
  if (!detailModal.value) return ""
  const product = backendMap.value.get(Number(detailModal.value.id))
  const variant = modalResolvedVariant.value
  return normalizeImage(variant?.image)
    || normalizeImage(detailModal.value.image)
    || normalizeImage(product?.img)
    || fallbackImageForVariant({
      id: product?.id,
      maSanPham: product?.code,
      tenSanPham: product?.name,
      tenMauSac: modalColor.value,
      maChiTietSanPham: variant?.ma,
    })
})

const modalPreviewPrice = computed(() => {
  const variantPrice = Number(modalResolvedVariant.value?.price || 0)
  if (variantPrice > 0) return variantPrice
  return Number(detailModal.value?.price || 0)
})

const modalLineSubtotal = computed(() => {
  return Math.max(0, Number(modalPreviewPrice.value || 0) * Number(modalQty.value || 0))
})

const modalSelectedVoucher = computed(() => {
  const code = String(modalVoucherCode.value || "").trim().toUpperCase()
  if (!code) return null
  const found = vouchers.value.find((item) => {
    return String(item?.maPhieuGiamGia || "").trim().toUpperCase() === code
  })
  return found ? normalizeVoucherData(found) : null
})

const modalVoucherPreviewDiscount = computed(() => {
  const voucher = modalSelectedVoucher.value
  if (!voucher) return 0
  if (!isVoucherApplicable(voucher, modalLineSubtotal.value)) return 0
  return calculateVoucherDiscount(voucher, modalLineSubtotal.value)
})

const modalVoucherOptions = computed(() => {
  const subtotal = modalLineSubtotal.value
  return vouchers.value
    .map((voucher) => {
      const normalized = normalizeVoucherData(voucher)
      const applicable = isVoucherApplicable(normalized, subtotal)
      const discount = applicable ? calculateVoucherDiscount(normalized, subtotal) : 0
      return {
        code: String(normalized?.maPhieuGiamGia || "").trim(),
        applicable,
        discount,
        minOrder: Number(normalized?.hoaDonToiThieu || 0),
      }
    })
    .filter((row) => row.code)
})

const confirmDetail = () => {
  if (!detailModal.value) return
  const oldKey = String(detailModal.value.cartKey || detailModal.value.id)
  const newKey = buildCartKey(detailModal.value.id, modalColor.value, modalSize.value, modalVoucherCode.value) || oldKey

  const product = backendMap.value.get(Number(detailModal.value.id))
  const matched = findVariantByChoice(product, modalColor.value, modalSize.value)
  const resolvedImage = modalPreviewImage.value

  if (modalQty.value > 0) {
    if (newKey !== oldKey) {
      delete cart.value[oldKey]
    }
    cart.value[newKey] = Number(modalQty.value || 1)
    cart.value = { ...cart.value }
    writeCart()
  }

  cartVariants.value = {
    ...cartVariants.value,
    [newKey]: {
      color: modalColor.value,
      size: modalSize.value,
      voucherCode: String(modalVoucherCode.value || "").trim().toUpperCase(),
      spctId: matched?.spctId || null,
      image: resolvedImage,
    },
  }
  if (newKey !== oldKey) {
    delete cartVariants.value[oldKey]
  }
  writeCartVariants()

  const nextCheckoutSelections = { ...checkoutItemVoucherSelections.value }
  if (newKey !== oldKey && nextCheckoutSelections[oldKey]) {
    nextCheckoutSelections[newKey] = nextCheckoutSelections[oldKey]
    delete nextCheckoutSelections[oldKey]
  }

  const selectedVoucher = modalSelectedVoucher.value
  if (selectedVoucher && isVoucherApplicable(selectedVoucher, modalLineSubtotal.value)) {
    nextCheckoutSelections[newKey] = selectedVoucher
  } else {
    delete nextCheckoutSelections[newKey]
  }
  checkoutItemVoucherSelections.value = nextCheckoutSelections
  localStorage.setItem(CHECKOUT_ITEM_VOUCHER_SELECTIONS_KEY, JSON.stringify(nextCheckoutSelections))

  const nextPendingSelections = { ...pendingItemVoucherSelections.value }
  delete nextPendingSelections[oldKey]
  delete nextPendingSelections[newKey]
  pendingItemVoucherSelections.value = nextPendingSelections
  localStorage.setItem(CHECKOUT_PENDING_ITEM_VOUCHERS_KEY, JSON.stringify(nextPendingSelections))

  closeDetailModal()
}

const notifyCartUpdated = () => {
  window.dispatchEvent(new Event(CART_UPDATED_EVENT))
}

const writeCart = () => {
  writeCartObject(cart.value)
  notifyCartUpdated()
}

const resolveProduct = (id) => {
  const numericId = Number(id)
  const backendProduct = backendMap.value.get(numericId)
  if (backendProduct) return backendProduct
  return { id: numericId, name: `Sản phẩm #${numericId}`, code: "", cat: "Sản phẩm", price: 0, img: "", variants: [] }
}

const cartLines = computed(() => {
  return Object.entries(cart.value)
    .map(([cartKey, quantity]) => {
      const qty = toNumber(quantity)
      if (qty <= 0) return null

      const parsed = parseCartKey(cartKey)
      const product = resolveProduct(parsed.idPart)
      const variantByLine = cartVariants.value[String(cartKey)] || {}
      const variantByProduct = cartVariants.value[String(parsed.idPart)] || {}
      const chosenColor = variantByLine.color || parsed.colorPart || variantByProduct.color || ""
      const chosenSize = variantByLine.size || parsed.sizePart || variantByProduct.size || ""
      // Voucher identity must come from line key first; legacy variant fallback must not override it.
      const chosenVoucherCode = String(parsed.voucherPart || variantByLine.voucherCode || "").trim().toUpperCase()
      const matchedVariant = (product.variants || []).find((variant) => {
        const sameColor = !chosenColor || String(variant?.color || "") === String(chosenColor)
        const sameSize = !chosenSize || String(variant?.size || "") === String(chosenSize)
        return sameColor && sameSize
      }) || (product.variants || []).find((variant) => String(variant?.color || "") === String(chosenColor))

      const lineImage = normalizeImage(variantByLine.image)
        || normalizeImage(matchedVariant?.image)
        || normalizeImage(variantByProduct.image)
        || fallbackImageForVariant({
          id: product.id,
          maSanPham: product.code,
          tenSanPham: product.name,
          tenMauSac: chosenColor,
          maChiTietSanPham: matchedVariant?.ma,
        })
        || product.img

      return {
        id: parsed.id,
        cartKey: String(cartKey),
        lineKey: String(cartKey),
        quantity: qty,
        name: product.name,
        category: product.cat,
        image: lineImage,
        price: toNumber(matchedVariant?.price || product.price),
        basePrice: toNumber(matchedVariant?.basePrice || matchedVariant?.price || product.price),
        campaignPercent: toNumber(matchedVariant?.campaignPercent || 0),
        lineTotal: toNumber(matchedVariant?.price || product.price) * qty,
        color: chosenColor,
        size: chosenSize,
        voucherCode: chosenVoucherCode,
        spctId: variantByLine.spctId || variantByProduct.spctId || null,
        variants: product.variants || [],
      }
    })
    .filter(Boolean)
})

const syncSelectedLines = () => {
  const availableKeys = cartLines.value.map((line) => line.lineKey)
  if (!availableKeys.length) {
    selectedLineKeys.value = []
    return
  }

  if (!selectedLineKeys.value.length) {
    selectedLineKeys.value = [...availableKeys]
    return
  }

  selectedLineKeys.value = selectedLineKeys.value.filter((key) => availableKeys.includes(key))
  if (!selectedLineKeys.value.length) {
    selectedLineKeys.value = [...availableKeys]
  }
}

const allSelected = computed(() => {
  if (!cartLines.value.length) return false
  return cartLines.value.every((line) => selectedLineKeys.value.includes(line.lineKey))
})

const selectedCartLines = computed(() => {
  const keys = new Set(selectedLineKeys.value)
  return cartLines.value.filter((line) => keys.has(line.lineKey))
})

const getVoucherForLine = (line) => {
  const voucherFromLineKey = String(line?.voucherCode || "").trim().toUpperCase()
  if (voucherFromLineKey) {
    const matchedByLineKey = vouchers.value.find((voucher) => {
      return String(voucher?.maPhieuGiamGia || "").trim().toUpperCase() === voucherFromLineKey
    })
    const normalizedByLineKey = normalizeVoucherData(matchedByLineKey || { maPhieuGiamGia: voucherFromLineKey })
    return normalizedByLineKey?.maPhieuGiamGia ? normalizedByLineKey : null
  }

  const fromCheckout = checkoutItemVoucherSelections.value?.[line.lineKey]
  const fromPending = pendingItemVoucherSelections.value?.[line.lineKey]
  const raw = fromCheckout || fromPending
  if (raw) {
    const normalized = normalizeVoucherData(raw?.voucher || raw)
    return normalized?.maPhieuGiamGia ? normalized : null
  }

  return null
}

const getLineVoucherDiscount = (line) => {
  const voucher = getVoucherForLine(line)
  if (!voucher) return 0
  const lineSubtotal = Number(line?.lineTotal || 0)
  if (!isVoucherApplicable(voucher, lineSubtotal)) return 0
  return calculateVoucherDiscount(voucher, lineSubtotal)
}

const getLineTotalAfterVoucher = (line) => {
  return Math.max(0, Number(line?.lineTotal || 0) - getLineVoucherDiscount(line))
}

const selectedItemsCount = computed(() => selectedCartLines.value.reduce((sum, line) => sum + line.quantity, 0))

const selectedSubtotal = computed(() => selectedCartLines.value.reduce((sum, line) => sum + getLineTotalAfterVoucher(line), 0))

const toggleLineSelection = (lineKey) => {
  const exists = selectedLineKeys.value.includes(lineKey)
  if (exists) {
    selectedLineKeys.value = selectedLineKeys.value.filter((key) => key !== lineKey)
    return
  }
  selectedLineKeys.value = [...selectedLineKeys.value, lineKey]
}

const toggleSelectAll = () => {
  if (allSelected.value) {
    selectedLineKeys.value = []
    return
  }
  selectedLineKeys.value = cartLines.value.map((line) => line.lineKey)
}

const subtotal = computed(() => cartLines.value.reduce((sum, line) => sum + line.lineTotal, 0))
const totalItems = computed(() => cartLines.value.reduce((sum, line) => sum + line.quantity, 0))

const resolveLineStock = (line) => {
  if (!line?.variants?.length) return Infinity

  const matchedBySpct = line.variants.find((variant) => Number(variant?.spctId) === Number(line.spctId))
  if (matchedBySpct) return Number(matchedBySpct?.stock || 0)

  const matchedByColorSize = line.variants.find((variant) => {
    return String(variant?.color || "") === String(line?.color || "")
      && String(variant?.size || "") === String(line?.size || "")
  })
  if (matchedByColorSize) return Number(matchedByColorSize?.stock || 0)

  return Infinity
}

const increase = (cartKey) => {
  const key = String(cartKey)
  const line = cartLines.value.find((entry) => String(entry.cartKey) === key)
  const current = toNumber(cart.value[key])
  const next = current + 1
  const maxStock = resolveLineStock(line)

  if (Number.isFinite(maxStock) && maxStock >= 0 && next > maxStock) {
    window.toast?.warning?.("Số lượng đã đạt mức tồn kho tối đa")
    return
  }

  cart.value[key] = next
  cart.value = { ...cart.value }
  writeCart()
}

const decrease = (id) => {
  const key = String(id)
  const current = toNumber(cart.value[key])
  if (current <= 1) {
    delete cart.value[key]
  } else {
    cart.value[key] = current - 1
  }
  cart.value = { ...cart.value }
  writeCart()
}

const removeItem = (id) => {
  const key = String(id)
  if (!(key in cart.value)) return
  delete cart.value[key]
  cart.value = { ...cart.value }
  writeCart()
}

const clearCart = () => {
  cart.value = {}
  writeCart()
}

const continueShopping = () => {
  router.push("/san-pham")
}

const goProduct = (id) => {
  router.push(`/product/${id}`)
}

const beginCheckout = () => {
  const userId = String(localStorage.getItem("userId") || "").trim()
  const role = String(localStorage.getItem("role") || "").trim().toUpperCase()
  if (!userId || !role) {
    window.toast?.error?.("Vui lòng đăng nhập trước khi thanh toán")
    router.push("/auth/customer-login")
    return
  }

  if (!selectedCartLines.value.length) {
    window.toast?.warning?.("Vui lòng chọn ít nhất 1 sản phẩm để thanh toán")
    return
  }

  const outOfStockLines = selectedCartLines.value.filter((line) => {
    const maxStock = resolveLineStock(line)
    return Number.isFinite(maxStock) && maxStock >= 0 && Number(line.quantity || 0) > maxStock
  })
  if (outOfStockLines.length) {
    window.toast?.error?.("Một số sản phẩm vượt quá tồn kho, vui lòng kiểm tra lại giỏ hàng")
    return
  }

  const payload = selectedCartLines.value.map((line) => ({
    id: line.id,
    name: line.name,
    price: line.price,
    quantity: line.quantity,
    image: line.image,
    category: line.category,
    color: line.color || "",
    size: line.size || "",
    voucherCode: line.voucherCode || "",
    spctId: line.spctId || null,
  }))

  if (!writeCheckoutCartArray(payload)) {
    window.toast?.error?.("Không thể tiếp tục thanh toán vì bộ nhớ trình duyệt đã đầy. Vui lòng xóa bớt dữ liệu và thử lại.")
    return
  }
  router.push("/checkout")
}

const loadCheckoutItemVouchers = () => {
  try {
    const raw = localStorage.getItem(CHECKOUT_ITEM_VOUCHER_SELECTIONS_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    checkoutItemVoucherSelections.value = parsed && typeof parsed === "object" ? parsed : {}

    const rawPending = localStorage.getItem(CHECKOUT_PENDING_ITEM_VOUCHERS_KEY)
    const parsedPending = rawPending ? JSON.parse(rawPending) : {}
    pendingItemVoucherSelections.value = parsedPending && typeof parsedPending === "object" ? parsedPending : {}
  } catch {
    checkoutItemVoucherSelections.value = {}
    pendingItemVoucherSelections.value = {}
  }
}

const loadVouchers = async () => {
  loadingVouchers.value = true
  try {
    const normalizeVoucher = (voucher) => ({
      ...normalizeVoucherData(voucher),
      id: voucher?.id ?? voucher?.phieuGiamGiaId ?? voucher?.maPhieuGiamGia ?? voucher?.maPhieu ?? voucher?.code,
    })

    let payload = []
    try {
      const response = await getActiveVouchers(20)
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

    vouchers.value = payload.map(normalizeVoucher).filter((voucher) => voucher?.maPhieuGiamGia)
  } catch {
    vouchers.value = []
  } finally {
    loadingVouchers.value = false
  }
}

const cartSelectedVouchers = computed(() => {
  const map = new Map()
  for (const line of cartLines.value) {
    const voucher = getVoucherForLine(line)
    if (!voucher) continue
    const code = String(voucher?.maPhieuGiamGia || "").trim()
    if (!code) continue
    const groupKey = [line.id, String(line.color || "").trim(), String(line.size || "").trim(), code.toUpperCase()].join("__")
    if (!map.has(groupKey)) {
      map.set(groupKey, {
        key: groupKey,
        lineKey: line.lineKey,
        productName: line.name,
        color: line.color,
        size: line.size,
        code,
      })
    }
  }
  return Array.from(map.values())
})

const loadProducts = async () => {
  loadingProducts.value = true
  try {
    const response = await getAllSanPham()
    const rows = Array.isArray(response?.data) ? response.data : []
    const withCampaignPrice = await Promise.all(rows.map(async (item) => {
      const variants = Array.isArray(item?.sanPhamChiTiets)
        ? item.sanPhamChiTiets.filter((variant) => isActiveVariantStatus(variant?.trangThai || variant?.status))
        : []
      if (!variants.length) return item
      let pricedVariants = variants
      try {
        pricedVariants = await applyCampaignPriceToVariants(variants, item.id)
      } catch {
        pricedVariants = variants
      }
      return {
        ...item,
        sanPhamChiTiets: pricedVariants,
      }
    }))
    backendProducts.value = withCampaignPrice.map(normalizeBackendProduct).filter((item) => Number.isFinite(item.id))
  } catch {
    backendProducts.value = []
  } finally {
    loadingProducts.value = false
  }
}

onMounted(() => {
  cleanLegacyVoucherStorage()
  readCart()
  readCartVariants()
  loadProducts()
  loadVouchers()
  loadCheckoutItemVouchers()
  window.addEventListener("storage", readCart)
  window.addEventListener("storage", loadCheckoutItemVouchers)
})

watch(cartLines, syncSelectedLines, { immediate: true })

onUnmounted(() => {
  window.removeEventListener("storage", readCart)
  window.removeEventListener("storage", loadCheckoutItemVouchers)
})
</script>

<template>
  <div class="cart-page">
    <SiteNav :cart-count="totalItems" />

    <section class="cart-hero">
      <div class="cart-shell">
        <h1>Giỏ hàng <span class="cart-count-badge" v-if="totalItems">({{ totalItems }})</span></h1>
      </div>
    </section>

    <main class="cart-shell cart-main">
      <section class="cart-lines-card">
        <header class="cart-lines-head">
          <label v-if="cartLines.length" class="cart-select-all-label">
            <input type="checkbox" :checked="allSelected" @change="toggleSelectAll" />
            Chọn tất cả ({{ selectedItemsCount }}/{{ totalItems }})
          </label>
          <button v-if="cartLines.length" class="ghost-btn ghost-btn--danger" type="button" @click="clearCart">
            <Trash2 :size="14" />
            Xóa tất cả
          </button>
        </header>

        <div v-if="!cartLines.length" class="cart-empty">
          <ShoppingBag :size="48" stroke-width="1" style="color:#c5c8d4" />
          <strong>Giỏ hàng trống</strong>
          <p>Khám phá bộ sưu tập bomber, hoodie &amp; coach mới nhất.</p>
          <button class="primary-btn" type="button" @click="continueShopping">Xem sản phẩm</button>
        </div>

        <div v-else class="cart-lines-list">
          <article v-for="line in cartLines" :key="line.lineKey" class="cart-line" :class="{ 'cart-line--selected': selectedLineKeys.includes(line.lineKey) }">
            <label class="cart-line__check">
              <input
                type="checkbox"
                :checked="selectedLineKeys.includes(line.lineKey)"
                @change="toggleLineSelection(line.lineKey)"
              />
            </label>

            <button class="cart-line__thumb" type="button" @click="openDetailModal(line)">
              <img :src="line.image" :alt="line.name" />
            </button>

            <div class="cart-line__body">
              <h3 class="cart-line__name" @click="goProduct(line.id)">{{ line.name }}</h3>
              <div class="cart-line__meta">
                <span v-if="line.color" class="meta-chip">{{ line.color }}</span>
                <span v-if="line.size" class="meta-chip">{{ line.size }}</span>
                <span v-if="line.campaignPercent > 0" class="meta-chip meta-chip--sale">-{{ Math.round(line.campaignPercent) }}%</span>
              </div>
            </div>

            <div class="cart-line__right">
              <strong class="cart-line__price" :class="{ 'cart-line__price--discounted': getLineVoucherDiscount(line) > 0 }">{{ formatVND(getLineTotalAfterVoucher(line)) }}</strong>
              <small v-if="getLineVoucherDiscount(line) > 0" class="cart-line__price-before">{{ formatVND(line.lineTotal) }}</small>
              <div class="cart-line__qty">
                <button type="button" @click="decrease(line.cartKey)"><Minus :size="14" /></button>
                <span>{{ line.quantity }}</span>
                <button type="button" @click="increase(line.cartKey)"><Plus :size="14" /></button>
              </div>
              <div class="cart-line__actions">
                <button type="button" @click="openDetailModal(line)" title="Chỉnh sửa"><Pencil :size="13" /></button>
                <button type="button" @click="removeItem(line.cartKey)" title="Xóa"><Trash2 :size="13" /></button>
              </div>
            </div>
          </article>
        </div>

        <div v-if="cartSelectedVouchers.length" class="cart-selected-vouchers">
          <p class="cart-selected-vouchers__title"><Ticket :size="13" style="margin-right:5px;vertical-align:-2px" />Voucher đã chọn</p>
          <div v-for="row in cartSelectedVouchers" :key="row.key" class="cart-selected-vouchers__row">
            <span class="csv-product">{{ row.productName }} <small v-if="row.color || row.size">({{ row.color || "-" }} • {{ row.size || "-" }})</small></span>
            <span class="csv-code">{{ row.code }}</span>
          </div>
        </div>
      </section>

      <aside class="cart-summary-card">
        <h2>Tóm tắt đơn</h2>
        <div class="summary-grid">
          <span>Sản phẩm ({{ selectedItemsCount }})</span>
          <strong>{{ formatVND(selectedSubtotal) }}</strong>
          <span>Phí vận chuyển</span>
          <span class="summary-ship-note">Tính khi thanh toán</span>
        </div>

        <div class="summary-total">
          <span>Tổng cộng</span>
          <strong>{{ formatVND(selectedSubtotal) }}</strong>
        </div>

        <button class="primary-btn" type="button" :disabled="!selectedCartLines.length" @click="beginCheckout">
          Thanh toán ngay
          <ChevronRight :size="16" />
        </button>
        <button class="secondary-btn" type="button" @click="continueShopping">Tiếp tục mua sắm</button>
        <p class="summary-note" v-if="loadingProducts">Đang cập nhật giá...</p>
      </aside>
    </main>

    <transition name="pd-fade">
      <div v-if="detailModal" class="pd-overlay" @click.self="closeDetailModal">
        <div class="pd-card">
          <div class="pd-card-head">
            <p class="pd-cat-label">{{ detailModal.category }}</p>
            <button class="pd-close" type="button" @click="closeDetailModal">✕</button>
          </div>

          <div class="pd-image-wrap">
            <img :src="modalPreviewImage || detailModal.image" :alt="detailModal.name" class="pd-main-img" />
          </div>

          <div class="pd-info">
            <h3 class="pd-name">{{ detailModal.name }}</h3>
            <p class="pd-price">{{ formatVND(modalPreviewPrice || detailModal.price) }}</p>
          </div>

          <div v-if="getColorOptions(detailModal.id).length" class="pd-section">
            <p class="pd-label">Màu sắc</p>
            <div class="pd-option-grid">
              <button
                v-for="color in getColorOptions(detailModal.id)"
                :key="color"
                type="button"
                class="pd-opt-btn"
                :class="{ active: modalColor === color }"
                @click="selectModalColor(color)"
              >
                {{ color }}
              </button>
            </div>
          </div>

          <div v-if="getSizeOptionsForModal(detailModal.id, modalColor).length" class="pd-section">
            <p class="pd-label">Size</p>
            <div class="pd-option-grid">
              <button
                v-for="size in getSizeOptionsForModal(detailModal.id, modalColor)"
                :key="size"
                type="button"
                class="pd-opt-btn"
                :class="{ active: modalSize === size }"
                @click="modalSize = size"
              >
                {{ size }}
              </button>
            </div>
          </div>

          <div class="pd-section">
            <p class="pd-label">Số lượng</p>
            <div class="qty-box">
              <button type="button" @click="modalQty = Math.max(1, modalQty - 1)">−</button>
              <input type="number" v-model.number="modalQty" min="1" class="qty-input" @change="modalQty = Math.max(1, Math.floor(modalQty) || 1)" />
              <button type="button" @click="modalQty++">+</button>
            </div>
          </div>

          <div class="pd-section">
            <p class="pd-label">Voucher</p>
            <div class="pd-voucher-select-wrap">
              <select v-model="modalVoucherCode" class="pd-voucher-select">
                <option value="">Không áp dụng voucher</option>
                <option
                  v-for="voucher in modalVoucherOptions"
                  :key="voucher.code"
                  :value="voucher.code"
                  :disabled="!voucher.applicable"
                >
                  {{ voucher.code }}
                  {{ voucher.applicable ? `- giảm ${formatVND(voucher.discount)}` : `- cần từ ${formatVND(voucher.minOrder)}` }}
                </option>
              </select>
            </div>
            <p v-if="loadingVouchers" class="pd-voucher-note">Đang tải voucher...</p>
            <p v-else-if="modalVoucherCode && modalVoucherPreviewDiscount > 0" class="pd-voucher-note pd-voucher-note--ok">
              Ước tính giảm {{ formatVND(modalVoucherPreviewDiscount) }} cho sản phẩm này
            </p>
            <p v-else-if="modalVoucherCode" class="pd-voucher-note">
              Voucher chưa đủ điều kiện với tổng tiền hiện tại của sản phẩm
            </p>
          </div>

          <button class="pd-confirm-btn" type="button" @click="confirmDetail">Xác nhận</button>
        </div>
      </div>
    </transition>

    <CustomerFooter />
  </div>
</template>

<style scoped>
/* â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
   DirtyWave Cart â€” White + Red accent theme
   â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â• */
.cart-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f4f5f7;
}

.cart-shell {
  width: min(1120px, calc(100% - 32px));
  margin: 0 auto;
}

/* â”€â”€ Hero â”€â”€ */
.cart-hero {
  padding: 32px 0 10px;
}

.cart-hero h1 {
  margin: 0;
  font-size: clamp(24px, 3.5vw, 32px);
  font-weight: 800;
  color: #111827;
  letter-spacing: -0.01em;
}

.cart-count-badge {
  font-weight: 400;
  color: #6b7280;
  font-size: 0.7em;
}

/* â”€â”€ Main layout â”€â”€ */
.cart-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
  padding: 12px 0 56px;
  align-items: start;
}

/* â”€â”€ Cards â”€â”€ */
.cart-lines-card,
.cart-summary-card {
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.cart-lines-card {
  padding: 18px 22px;
}

/* â”€â”€ Head â”€â”€ */
.cart-lines-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f0f1f4;
}

.cart-select-all-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #374151;
  font-weight: 600;
  cursor: pointer;
}

.cart-select-all-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #dc2626;
  cursor: pointer;
}

.ghost-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #e5e7eb;
  background: #fff;
  height: 34px;
  padding: 0 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}
.ghost-btn:hover { border-color: #d1d5db; background: #f9fafb; }
.ghost-btn--danger:hover { border-color: #fca5a5; color: #dc2626; background: #fef2f2; }

/* â”€â”€ Empty â”€â”€ */
.cart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 56px 20px;
  text-align: center;
}

.cart-empty strong {
  font-size: 16px;
  color: #111827;
}

.cart-empty p {
  margin: 0;
  max-width: 360px;
  color: #6b7280;
  font-size: 14px;
}

/* â”€â”€ Cart lines â”€â”€ */
.cart-lines-list { display: grid; gap: 0; }

.cart-line {
  display: grid;
  grid-template-columns: 28px 84px minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #f3f4f6;
  transition: background 0.15s;
}

.cart-line:last-child { border-bottom: none; }

.cart-line--selected {
  background: #fff5f5;
  border-left: 3px solid #dc2626;
  margin: 0 -22px;
  padding-left: 19px;
  padding-right: 22px;
}

.cart-line__check {
  display: flex;
  align-items: center;
  justify-content: center;
}

.cart-line__check input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #dc2626;
  cursor: pointer;
}

/* â”€â”€ Thumbnail â”€â”€ */
.cart-line__thumb {
  width: 84px;
  height: 84px;
  border: 0;
  border-radius: 12px;
  overflow: hidden;
  padding: 0;
  background: #f3f4f6;
  cursor: pointer;
  flex-shrink: 0;
  transition: box-shadow 0.2s;
}
.cart-line__thumb:hover { box-shadow: 0 0 0 2px #dc2626; }

.cart-line__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* â”€â”€ Body â”€â”€ */
.cart-line__body { min-width: 0; }

.cart-line__name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  line-height: 1.35;
  cursor: pointer;
  transition: color 0.15s;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.cart-line__name:hover { color: #dc2626; }

.cart-line__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 7px;
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 9px;
  border-radius: 4px;
  background: #f3f4f6;
  color: #4b5563;
  font-size: 11px;
  font-weight: 600;
}

.meta-chip--sale {
  background: #fef2f2;
  color: #dc2626;
  font-weight: 700;
}

/* â”€â”€ Right column â”€â”€ */
.cart-line__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  min-width: 120px;
}

.cart-line__price {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}

.cart-line__price--discounted {
  color: #b91c1c;
}

.cart-line__price-before {
  color: #9ca3af;
  font-size: 11px;
  text-decoration: line-through;
  margin-top: -6px;
}

.cart-line__qty {
  display: inline-flex;
  align-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: visible;
  flex-shrink: 0;
}

.cart-line__qty button {
  width: 30px;
  height: 30px;
  border: 0;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #374151;
  transition: all 0.12s;
}
.cart-line__qty button:hover { background: #f3f4f6; }

.cart-line__qty span {
  min-width: 44px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  padding: 0 10px;
  border-left: 1px solid #e5e7eb;
  border-right: 1px solid #e5e7eb;
  height: 30px;
  line-height: 30px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.cart-line__actions { display: flex; gap: 4px; }

.cart-line__actions button {
  width: 28px;
  height: 28px;
  border: 0;
  background: transparent;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #9ca3af;
  transition: all 0.15s;
}
.cart-line__actions button:hover { background: #f3f4f6; color: #374151; }
.cart-line__actions button:last-child:hover { color: #dc2626; background: #fef2f2; }

.cart-selected-vouchers {
  margin-top: 14px;
  border-top: 1px dashed #fecaca;
  padding-top: 12px;
}

.cart-selected-vouchers__title {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  color: #b91c1c;
}

.cart-selected-vouchers__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #fecaca;
  background: #fff5f5;
}

.cart-selected-vouchers__row + .cart-selected-vouchers__row {
  margin-top: 6px;
}

.csv-product {
  color: #374151;
  font-size: 12px;
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.csv-product small {
  color: #6b7280;
  font-size: 11px;
}

.csv-code {
  color: #b91c1c;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

/* â”€â”€ Product Detail Modal â”€â”€ */
.pd-overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.pd-card {
  width: min(440px, 100%);
  background: #fff;
  border-radius: 20px;
  padding: 22px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
  max-height: 90vh;
  overflow-y: auto;
}

.pd-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.pd-cat-label {
  margin: 0;
  color: #6b7280;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.pd-close {
  width: 32px;
  height: 32px;
  border: 0;
  background: #f3f4f6;
  border-radius: 8px;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #374151;
  transition: background 0.15s;
}
.pd-close:hover { background: #e5e7eb; }

.pd-image-wrap {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 14px;
  overflow: hidden;
  background: #f3f4f6;
  margin-bottom: 16px;
}

.pd-main-img { width: 100%; height: 100%; object-fit: cover; }

.pd-info { margin-bottom: 14px; }

.pd-name {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 700;
  color: #111827;
  line-height: 1.3;
}

.pd-price {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #dc2626;
}

.pd-section { margin-bottom: 14px; }

.pd-label {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  display: flex;
  align-items: center;
}

.pd-option-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pd-opt-btn {
  min-width: 52px;
  padding: 6px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #374151;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.pd-opt-btn:hover { border-color: #dc2626; color: #dc2626; }
.pd-opt-btn.active {
  border-color: #dc2626;
  background: #fef2f2;
  color: #dc2626;
  box-shadow: inset 0 0 0 1px #dc2626;
}

.qty-box {
  display: inline-flex;
  align-items: center;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.qty-box button {
  width: 36px;
  height: 36px;
  border: 0;
  background: #fff;
  font-size: 18px;
  cursor: pointer;
  color: #374151;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.12s;
}
.qty-box button:hover { background: #f3f4f6; }

.qty-box strong {
  min-width: 38px;
  text-align: center;
  font-size: 14px;
  color: #111827;
  padding: 0 4px;
  font-weight: 600;
  border-left: 1px solid #e5e7eb;
  border-right: 1px solid #e5e7eb;
}

.qty-input {
  width: 50px;
  height: 36px;
  border: 0;
  border-left: 1px solid #e5e7eb;
  border-right: 1px solid #e5e7eb;
  background: transparent;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  outline: none;
  -moz-appearance: textfield;
}
.qty-input::-webkit-outer-spin-button,
.qty-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

/* â”€â”€ Modal Vouchers â”€â”€ */
.pd-voucher-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pd-voucher-item {
  border: 1.5px dashed #fca5a5;
  border-radius: 10px;
  padding: 9px 12px;
  background: #fff9f9;
}

.pd-voucher-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pd-voucher-code {
  font-size: 12px;
  font-weight: 800;
  color: #dc2626;
  font-family: 'Courier New', monospace;
  letter-spacing: 0.04em;
  background: #fef2f2;
  padding: 2px 7px;
  border-radius: 4px;
  border: 1px solid #fca5a5;
}

.pd-voucher-saving {
  font-size: 13px;
  font-weight: 700;
  color: #16a34a;
}

.pd-voucher-desc {
  margin: 4px 0 0;
  font-size: 11px;
  color: #6b7280;
  line-height: 1.4;
}

.pd-voucher-note {
  margin: 6px 0 0;
  font-size: 11px;
  color: #9ca3af;
  font-style: italic;
}

.pd-voucher-note--ok {
  color: #15803d;
}

.pd-voucher-select-wrap {
  width: 100%;
}

.pd-voucher-select {
  width: 100%;
  min-height: 38px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 7px 10px;
  font-size: 13px;
  color: #111827;
  background: #fff;
}

.pd-voucher-select:focus {
  outline: none;
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.12);
}

.pd-confirm-btn {
  width: 100%;
  min-height: 46px;
  margin-top: 10px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #dc2626 0%, #991b1b 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  letter-spacing: 0.02em;
  transition: opacity 0.15s, box-shadow 0.15s;
}
.pd-confirm-btn:hover { opacity: 0.9; box-shadow: 0 4px 18px rgba(220, 38, 38, 0.28); }

.pd-fade-enter-active,
.pd-fade-leave-active { transition: opacity 0.2s ease; }
.pd-fade-enter-from,
.pd-fade-leave-to { opacity: 0; }

/* â”€â”€ Summary â”€â”€ */
.cart-summary-card {
  padding: 22px;
  position: sticky;
  top: 100px;
}

.cart-summary-card h2 {
  margin: 0 0 18px;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px 8px;
  font-size: 14px;
  color: #6b7280;
}

.summary-grid strong { color: #111827; }

.summary-ship-note {
  color: #9ca3af;
  font-size: 13px;
  font-style: italic;
}

.summary-total {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.summary-total span {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.summary-total strong {
  font-size: 24px;
  font-weight: 800;
  color: #dc2626;
}

.primary-btn,
.secondary-btn {
  width: 100%;
  min-height: 46px;
  border-radius: 10px;
  border: 0;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: all 0.2s;
  letter-spacing: 0.01em;
}

.primary-btn {
  margin-top: 18px;
  background: linear-gradient(135deg, #dc2626 0%, #991b1b 100%);
  color: #fff;
}
.primary-btn:hover { box-shadow: 0 4px 18px rgba(220, 38, 38, 0.3); opacity: 0.93; }
.primary-btn:disabled { opacity: 0.35; cursor: not-allowed; box-shadow: none; }

.secondary-btn {
  margin-top: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #374151;
}
.secondary-btn:hover { background: #f9fafb; border-color: #d1d5db; }

.summary-voucher-selected {
  margin: 10px 0 0;
  padding: 9px 12px;
  border-radius: 10px;
  border: 1px solid #fecaca;
  background: #fff5f5;
  color: #b91c1c;
  font-size: 12px;
  font-weight: 700;
}

.summary-voucher-selected__btn {
  width: 100%;
  display: block;
  border: 0;
  padding: 0;
  margin: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.summary-note {
  margin: 10px 0 0;
  color: #9ca3af;
  font-size: 12px;
  text-align: center;
}

/* â”€â”€ Summary Vouchers â”€â”€ */
.summary-vouchers {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px dashed #fca5a5;
}

.summary-voucher-title {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  color: #dc2626;
  display: flex;
  align-items: center;
}

.summary-voucher-chip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px;
  border-radius: 7px;
  background: #fff9f9;
  border: 1px solid #fecaca;
  margin-bottom: 5px;
}

.svc-code {
  font-size: 12px;
  font-weight: 700;
  color: #dc2626;
  font-family: 'Courier New', monospace;
  letter-spacing: 0.04em;
}

.svc-save {
  font-size: 12px;
  font-weight: 700;
  color: #16a34a;
}

.summary-voucher-hint {
  margin: 4px 0 0;
  font-size: 11px;
  color: #9ca3af;
  font-style: italic;
}

/* â”€â”€ Responsive â”€â”€ */
@media (max-width: 1024px) {
  .cart-main { grid-template-columns: 1fr; }
  .cart-summary-card { position: static; }
}

@media (max-width: 640px) {
  .cart-shell { width: calc(100% - 24px); }

  .cart-line {
    grid-template-columns: 24px 68px minmax(0, 1fr);
    gap: 10px;
  }

  .cart-line__thumb { width: 68px; height: 68px; }

  .cart-line__right {
    grid-column: 2 / -1;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    min-width: 0;
  }
}
</style>
