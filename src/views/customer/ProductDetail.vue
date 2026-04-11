<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import {
  ChevronLeft,
  ChevronRight,
  Copy,
  Eye,
  Menu,
  Search,
  ShieldCheck,
  ShoppingCart,
  Ticket,
  Truck,
  Undo2,
  Wallet,
  X,
  Ruler
} from "lucide-vue-next"
import { getAllSanPham } from "../../services/sanPhamService"
import { applyCampaignPriceToVariants, getProductCampaignInfo } from "../../services/campaignPricingService"
import taiKhoanService from "../../services/taiKhoanService"
import { getKhachHangByTaiKhoanId } from "../../services/KhachHangService"
import { calculateVoucherDiscount, getActiveVouchers, getAllVouchers, isVoucherApplicable } from "../../services/khuyenMaiService"
import { getVietnameseNameByEmail } from "../../utils/vietnameseNames"
import { useToast } from "../../composables/useToast"
import {
  AUTH_CONTEXT_CHANGED_EVENT,
  resolveAccountByRole
} from "../../utils/authContext"
import {
  readCartObject,
  readCartVariantsObject,
  writeCartObject,
  writeCartVariantsObject,
  writeCheckoutCartArray
} from "../../utils/cartStorage"
import { resolveApiOrigin } from "../../utils/apiOrigin"
import { getProductImageOverride, getProductImageConfig } from "../../utils/productImageOverrides"
import { fallbackImageForVariant } from "../../utils/productImageFallback"
import SiteNav from "../../components/SiteNav.vue"
import logo from "../../assets/img/logo/new logo.png?url"
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
// New products from reorganized folders
import img12 from "../../assets/img/Jackets/bomber/bomber-astronaut/bomber-astronaut-black.PNG?url"
import img13 from "../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-black.PNG?url"
import img14 from "../../assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-black.PNG?url"
import img15 from "../../assets/img/Jackets/coach/coach-leopard/coach-leopard.PNG?url"
import img16 from "../../assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-black.PNG?url"
import img17 from "../../assets/img/Jackets/coach/coach-tiger-stripe/coach-tiger-stripe.PNG?url"
import img18 from "../../assets/img/Jackets/hoodie/hoodie-camo/hoodie-camo-black.PNG?url"
import img19 from "../../assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-blue.PNG?url"
import img20 from "../../assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-black.PNG?url"
// Additional color images
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

const route = useRoute()
const router = useRouter()
const toast = useToast()
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")
const fallbackImages = [img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11, img12, img13, img14, img15, img16, img17, img18, img19, img20]

const profileOpen = ref(false)
const mobileOpen = ref(false)
const voucherDrawerOpen = ref(false)
const selectedVoucherInDetail = ref(null)
const searchQuery = ref("")
const activeImage = ref("")
const galleryMotionDirection = ref("right")
const quantity = ref(1)
const selectedColor = ref("")
const selectedSize = ref("")
const activeTab = ref("description")
const vouchers = ref([])
const loadingVouchers = ref(false)
const productsLoading = ref(true)
const backendProducts = ref([])
const userAvatar = ref("")
const userDisplayName = ref("Khách hàng")
const userRoleLabel = ref("Khách hàng")
const cartVersion = ref(0)
const activeCampaignInfo = ref(null)
const CART_UPDATED_EVENT = "dirtywave:cart-updated"
const CHECKOUT_PENDING_ITEM_VOUCHERS_KEY = "checkoutPendingItemVoucherSelections"
const quickPreviewProduct = ref(null)
const quickPreviewColor = ref("")
const quickPreviewSize = ref("")
const quickPreviewQty = ref(1)
const quickPreviewImageIndex = ref(0)

const MOTION_PRESET_MAP = {
  snappy: { swipe: 200, fade: 170, ui: 180 },
  balanced: { swipe: 320, fade: 260, ui: 220 },
  cinematic: { swipe: 480, fade: 420, ui: 340 }
}

const motionPresetKeyRaw = String(localStorage.getItem("dirtywave:motion-preset") || "balanced").trim().toLowerCase()
const motionPresetKey = MOTION_PRESET_MAP[motionPresetKeyRaw] ? motionPresetKeyRaw : "balanced"
const motionPreset = MOTION_PRESET_MAP[motionPresetKey]
const galleryMotionVars = {
  "--pd-swipe-ms": `${motionPreset.swipe}ms`,
  "--pd-fade-ms": `${motionPreset.fade}ms`,
  "--pd-ui-ms": `${motionPreset.ui}ms`
}

const staticQuickImagesByCode = {
  SP002: [img2, img3],
  SP009: [img9, img10],
  SP012: [img12, img12b],
  SP013: [img13, img13b, img13c, img13d, img13e],
  SP014: [img14, img14b, img14c],
  SP016: [img16, img16b, img16c],
  SP018: [img18, img18b],
  SP019: [img19, img19b],
  SP020: [img20, img20b, img20c]
}

// Map Vietnamese color names to English keywords found in static image filenames
const colorKeywordMap = {
  den: "black", do: "red", trang: "white", xam: "gray", xanh: "blue",
  "xanh duong": "blue", "xanh la": "green", nau: "brown",
  vang: "yellow", hong: "pink", tim: "purple", cam: "orange",
  be: "beige", kem: "cream"
}

const getAvatarStorageKey = (accountId) => `avatar:${accountId}`

const normalizeRole = (role) => String(role || "").trim().toUpperCase().replace(/^ROLE_/, "")

const toRoleLabel = (role) => {
  const normalized = normalizeRole(role)
  if (normalized === "ADMIN") return "Quản trị viên"
  if (normalized === "EMPLOYEE") return "Nhân viên"
  return "Khách hàng"
}

const toDisplayNameFromEmail = (email = "") => {
  const localPart = String(email || "").split("@")[0].replace(/[._-]+/g, " ").trim()
  if (!localPart) return "Khách hàng"
  return localPart.split(/\s+/).map((word) => word.charAt(0).toUpperCase() + word.slice(1)).join(" ")
}

const VND = (value) => new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + "₫"

const normalizeKeyword = (value = "") => String(value)
  .normalize("NFD")
  .replace(/[\u0300-\u036f]/g, "")
  .replace(/đ/g, "d")
  .replace(/Đ/g, "D")
  .toLowerCase()
  .trim()

const resolveProductId = (item) => {
  const id = Number(item?.id ?? item?.idSanPham ?? item?.sanPhamId ?? item?.productId)
  return Number.isFinite(id) && id > 0 ? id : 0
}

const isAbsoluteUrl = (value = "") => /^https?:\/\//i.test(value) || /^data:image\//i.test(value)

const isImageString = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return false
  if (isAbsoluteUrl(raw)) return true

  const normalized = raw.replace(/\\/g, "/").split(/[?#]/)[0]
  if (normalized.startsWith("/uploads/") || normalized.startsWith("uploads/")) return true
  return /\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i.test(normalized)
}

const toImageUrl = (value) => {
  if (!value) return ""
  const raw = String(value).trim()
  if (!raw) return ""
  if (isAbsoluteUrl(raw)) return raw

  const normalized = raw.replace(/\\/g, "/")
  const uploadsMatch = normalized.match(/(?:^|\/)(uploads\/.*)$/i)
  if (uploadsMatch?.[1]) return `${BACKEND_ORIGIN}/${uploadsMatch[1]}`
  if (normalized.startsWith("/uploads/")) return `${BACKEND_ORIGIN}${normalized}`
  if (normalized.startsWith("uploads/")) return `${BACKEND_ORIGIN}/${normalized}`
  if (normalized.startsWith("assets/") || normalized.startsWith("img/")) return `/${normalized}`
  if (normalized.startsWith("/")) return normalized
  return normalized
}

const normalizeImageKey = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return ""
  const withoutQuery = raw.split("#")[0].split("?")[0]
  const normalized = withoutQuery.replace(/\\/g, "/").toLowerCase()
  try {
    const url = new URL(normalized, window.location.origin)
    return url.pathname.toLowerCase()
  } catch {
    return normalized
  }
}

const isSameImage = (left = "", right = "") => {
  const l = normalizeImageKey(left)
  const r = normalizeImageKey(right)
  return !!l && !!r && l === r
}

const pickImageValues = (entry) => {
  if (!entry) return []
  if (typeof entry === "string") {
    if (!isImageString(entry)) return []
    const parsed = toImageUrl(entry)
    return parsed ? [parsed] : []
  }
  if (Array.isArray(entry)) {
    return entry.flatMap((item) => pickImageValues(item)).filter(Boolean)
  }
  if (typeof entry === "object") {
    const bucket = []
    const directKeys = ["anh", "hinhAnh", "image", "imageUrl", "duongDanAnh", "images", "listAnh", "anhChinh", "thumbnail", "src"]
    for (const key of directKeys) {
      bucket.push(...pickImageValues(entry[key]))
    }
    return bucket
  }
  return []
}

const fallbackImageFor = (id, code = "") => {
  const numericId = Number(id)
  if (Number.isFinite(numericId) && numericId > 0) {
    return fallbackImages[(numericId - 1) % fallbackImages.length]
  }

  const codeNumber = Number(String(code || "").replace(/\D+/g, ""))
  if (Number.isFinite(codeNumber) && codeNumber > 0) {
    return fallbackImages[(codeNumber - 1) % fallbackImages.length]
  }

  return fallbackImages[0]
}

const isActiveStatus = (value = "") => {
  const normalized = String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
  return !normalized.includes("ngung") && !normalized.includes("inactive")
}

const getActiveVariants = (value) => {
  const variants = Array.isArray(value?.sanPhamChiTiets) ? value.sanPhamChiTiets : []
  return variants.filter((variant) => isActiveStatus(variant?.trangThai || variant?.status))
}

const normalizeBackendProduct = (item) => {
  const variants = getActiveVariants(item)
  const id = resolveProductId(item)
  const code = String(item?.maSanPham || item?.ma || "")
  const totalStock = variants.reduce((sum, variant) => sum + Math.max(Number(variant?.soLuong || 0), 0), 0)
  const isInactive = !isActiveStatus(item?.trangThai || item?.status) || variants.length === 0
  const category = String(item?.danhMuc?.tenDanhMuc || item?.loai?.tenLoai || "Thời trang nam")
  const variantPrices = variants.map((variant) => Number(variant?.giaBan || 0)).filter((n) => n > 0)
  const variantOriginalPrices = variants.map((variant) => Number(variant?.giaNhap || 0)).filter((n) => n > 0)
  const overrideImages = (Array.isArray(getProductImageOverride({ id, maSanPham: code }))
    ? getProductImageOverride({ id, maSanPham: code })
    : [])
    .map((entry) => toImageUrl(entry))
    .filter(Boolean)

  const staticImages = code && staticQuickImagesByCode[code]
    ? staticQuickImagesByCode[code]
    : []

  const backendImages = [...new Set(pickImageValues([
    item?.anh,
    item?.hinhAnh,
    item?.images,
    item?.image,
    item?.listAnh,
    item?.anhChinh,
    variants
  ]))]

  const images = overrideImages.length
    ? [...new Set(overrideImages)]
    : staticImages.length
      ? [...new Set(staticImages)]
      : backendImages.length
        ? [...new Set(backendImages)]
        : []

  const colors = [...new Set(
    variants
      .map((variant) => String(variant?.mauSac?.tenMau || "").trim())
      .filter(Boolean)
  )].map((name) => ({ name, hex: colorHexByName(name) }))

  const sizes = [...new Set(
    variants
      .map((variant) => String(variant?.kichThuoc?.tenKichThuoc || "").trim())
      .filter(Boolean)
  )]

  const price = variantPrices.length
    ? Math.min(...variantPrices)
    : Number(item?.giaBan || item?.gia || 0)

  const originalPrice = variantOriginalPrices.length
    ? Math.max(...variantOriginalPrices)
    : Number(item?.giaGoc || item?.giaNiemYet || 0)

  const descriptionText = String(item?.moTa || "").trim()

  const colorImages = {}
  if (staticImages.length) {
    // When static images exist, match colors by filename keyword
    for (const variant of variants) {
      const colorName = String(variant?.mauSac?.tenMau || "").trim()
      if (!colorName || colorImages[colorName]) continue
      const normalizedColor = normalizeKeyword(colorName)
      const keyword = colorKeywordMap[normalizedColor]
        || Object.entries(colorKeywordMap).find(([k]) => normalizedColor.includes(k))?.[1]
        || ""
      if (!keyword) continue
      const matched = staticImages.find((img) => String(img || "").toLowerCase().includes(keyword))
      if (matched) colorImages[colorName] = matched
    }
  } else {
    // Fallback: extract from backend variant fields
    for (const variant of variants) {
      const colorName = String(variant?.mauSac?.tenMau || "").trim()
      const image = pickImageValues([
        variant,
        variant?.anh,
        variant?.hinhAnh,
        variant?.image,
        variant?.imageUrl,
        variant?.duongDanAnh
      ])[0] || ""
      if (colorName && image && !colorImages[colorName]) {
        colorImages[colorName] = image
      }
    }
  }

  return {
    id,
    raw: item,
    name: String(item?.tenSanPham || item?.name || "Sản phẩm"),
    category,
    price,
    originalPrice,
    sku: code,
    badge: totalStock > 0 && !isInactive ? `Còn lại ${totalStock} sản phẩm` : "Hết hàng",
    badgeTone: totalStock > 0 && !isInactive ? "green" : "dark",
    images: images.length ? images : [fallbackImageFor(id, code)],
    colorImages,
    colors,
    sizes,
    material: "Chất liệu theo biến thể",
    fit: String(item?.loai?.tenLoai || "Form tiêu chuẩn"),
    bullets: [
      "Sản phẩm được đồng bộ trực tiếp từ dữ liệu hệ thống.",
      "Màu sắc và kích thước hiển thị theo biến thể hiện có.",
      "Giá bán được lấy theo biến thể thấp nhất đang khả dụng."
    ],
    description: {
      intro: descriptionText || "Sản phẩm đang được cập nhật mô tả chi tiết.",
      material: "Thông tin chất liệu được xác định theo từng biến thể của sản phẩm.",
      design: "Thiết kế và thông số chi tiết phụ thuộc dữ liệu quản trị đã nhập.",
      fit: "Form hiển thị theo loại sản phẩm trong hệ thống."
    }
  }
}

const EMPTY_PRODUCT = {
  id: 0,
  raw: null,
  name: "Không tìm thấy sản phẩm",
  category: "Sản phẩm",
  price: 0,
  originalPrice: 0,
  sku: "",
  badge: "Không khả dụng",
  badgeTone: "dark",
  images: [fallbackImages[0]],
  colors: [],
  sizes: [],
  material: "Đang cập nhật",
  fit: "Đang cập nhật",
  bullets: [],
  description: {
    intro: "Sản phẩm này không tồn tại hoặc đã bị ẩn khỏi hệ thống.",
    material: "",
    design: "",
    fit: ""
  }
}

const productCatalog = computed(() => {
  return backendProducts.value
    .map(normalizeBackendProduct)
    .filter((item) => Number(item.id) > 0)
})

const currentProduct = computed(() => {
  const routeId = Number(route.params.id)
  if (!Number.isFinite(routeId) || routeId <= 0) return EMPTY_PRODUCT
  return productCatalog.value.find((item) => Number(item.id) === routeId) || EMPTY_PRODUCT
})

const createPlaceholderImage = (index) => {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="900" height="1200" viewBox="0 0 900 1200"><defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#f8fafc"/><stop offset="100%" stop-color="#e5e7eb"/></linearGradient></defs><rect width="900" height="1200" fill="url(#g)"/><rect x="190" y="380" width="520" height="440" rx="16" fill="none" stroke="#cbd5e1" stroke-width="10" stroke-dasharray="20 16"/><circle cx="450" cy="600" r="60" fill="#e5e7eb"/><rect x="330" y="710" width="240" height="26" rx="13" fill="#e5e7eb"/><rect x="360" y="748" width="180" height="22" rx="11" fill="#e5e7eb"/></svg>`
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}

const displayImages = computed(() => {
  const baseImages = Array.isArray(currentProduct.value.images)
    ? currentProduct.value.images.filter(Boolean)
    : []

  const orderedColorImages = []
  for (const color of effectiveColors.value) {
    const colorId = Number(colorNameToIdMap.value?.[color?.name] || 0)
    const fromMap = colorId > 0 ? String(colorImageMap.value?.[colorId] || "").trim() : ""
    if (fromMap) orderedColorImages.push(fromMap)
  }

  const extraColorImages = Object.values(colorImageMap.value || {})
    .map((image) => String(image || "").trim())
    .filter(Boolean)

  const images = [...new Set([
    ...orderedColorImages,
    ...baseImages,
    ...extraColorImages
  ])]

  if (images.length) return images
  return [createPlaceholderImage(0)]
})

const hasSingleImage = computed(() => displayImages.value.length <= 1)

const colorImageMap = computed(() => {
  const map = {}
  const raw = currentProduct.value?.raw
  const variants = getActiveVariants(raw)
  const productId = currentProduct.value?.id || 0
  const productCode = currentProduct.value?.sku || ""

  // 1. HIGHEST PRIORITY: From staticQuickImagesByCode matched by color name in filename
  const staticImages = productCode && staticQuickImagesByCode[productCode]
    ? staticQuickImagesByCode[productCode]
    : []
  if (staticImages.length > 0) {
    for (const color of effectiveColors.value) {
      const colorId = Number(colorNameToIdMap.value?.[color?.name] || 0)
      if (colorId <= 0 || map[colorId]) continue
      const normalizedColor = normalizeKeyword(color?.name || "")
      const keyword = colorKeywordMap[normalizedColor]
        || Object.entries(colorKeywordMap).find(([k]) => normalizedColor.includes(k))?.[1]
        || ""
      if (!keyword) continue
      const matchedImage = staticImages.find((img) =>
        String(img || "").toLowerCase().includes(keyword)
      )
      if (matchedImage) map[colorId] = matchedImage
    }
  }

  // 2. From localStorage productImageConfig (admin-set colorImages)
  const config = getProductImageConfig({ id: productId, maSanPham: productCode })
  const storedColorImages = Array.isArray(config?.colorImages) ? config.colorImages : []
  for (const entry of storedColorImages) {
    const colorId = Number(entry?.colorId || 0)
    const image = String(entry?.image || "").trim()
    if (colorId > 0 && image && !map[colorId]) {
      map[colorId] = image
    }
  }

  // 3. From normalizeBackendProduct's colorImages (name-based)
  const namedColorImages = currentProduct.value?.colorImages || {}
  for (const [colorName, image] of Object.entries(namedColorImages)) {
    const colorId = Number(colorNameToIdMap.value?.[colorName] || 0)
    const normalizedImage = String(image || "").trim()
    if (colorId > 0 && normalizedImage && !map[colorId]) {
      map[colorId] = normalizedImage
    }
  }

  // 4. LOWEST PRIORITY: From backend variant image fields (only if no static images for this product)
  if (!staticImages.length) {
    for (const variant of variants) {
      const colorId = Number(variant?.mauSac?.id || 0)
      if (colorId <= 0 || map[colorId]) continue
      const image = pickImageValues([
        variant,
        variant?.anh,
        variant?.hinhAnh,
        variant?.image,
        variant?.imageUrl,
        variant?.duongDanAnh
      ])[0] || ""
      if (image) map[colorId] = image
    }
  }

  return map
})

const colorNameToIdMap = computed(() => {
  const raw = currentProduct.value?.raw
  if (!raw) return {}
  const variants = getActiveVariants(raw)
  const map = {}
  for (const v of variants) {
    const name = String(v?.mauSac?.tenMau || "").trim()
    const id = Number(v?.mauSac?.id || 0)
    if (name && id > 0 && !map[name]) map[name] = id
  }
  return map
})

const resolveColorImageByName = (colorName) => {
  const target = normalizeKeyword(colorName)
  if (!target) return ""

  const namedColorImages = currentProduct.value?.colorImages || {}
  for (const [name, image] of Object.entries(namedColorImages)) {
    if (normalizeKeyword(name) === target) {
      const normalizedImage = String(image || "").trim()
      if (normalizedImage) return normalizedImage
    }
  }

  const raw = currentProduct.value?.raw
  const variants = getActiveVariants(raw)
  for (const variant of variants) {
    const variantColor = normalizeKeyword(variant?.mauSac?.tenMau || "")
    if (variantColor !== target) continue
    const image = pickImageValues([
      variant,
      variant?.anh,
      variant?.hinhAnh,
      variant?.image,
      variant?.imageUrl,
      variant?.duongDanAnh
    ])[0] || ""
    if (image) return image
  }

  return ""
}

const activeImageIndex = computed(() => {
  const idx = displayImages.value.findIndex((image) => image === activeImage.value)
  return idx >= 0 ? idx : 0
})

const galleryTransitionName = computed(() => {
  return galleryMotionDirection.value === "left"
    ? "pd-image-swipe-left"
    : "pd-image-swipe-right"
})

const setActiveImageWithMotion = (nextImage, preferredDirection = "auto") => {
  const next = String(nextImage || "").trim()
  if (!next) return
  if (isSameImage(next, activeImage.value)) return

  if (preferredDirection === "left" || preferredDirection === "right") {
    galleryMotionDirection.value = preferredDirection
  } else {
    const currentIndex = displayImages.value.findIndex((image) => isSameImage(image, activeImage.value))
    const nextIndex = displayImages.value.findIndex((image) => isSameImage(image, next))
    if (currentIndex >= 0 && nextIndex >= 0) {
      galleryMotionDirection.value = nextIndex < currentIndex ? "left" : "right"
    } else {
      galleryMotionDirection.value = "right"
    }
  }

  activeImage.value = next
}

const forceScrollTop = () => {
  window.scrollTo({ top: 0, behavior: "auto" })
  requestAnimationFrame(() => {
    window.scrollTo({ top: 0, behavior: "auto" })
  })
}

const selectGalleryImage = (image) => {
  setActiveImageWithMotion(image, "auto")
}

const showPrevImage = () => {
  if (!displayImages.value.length) return
  const nextIndex = (activeImageIndex.value - 1 + displayImages.value.length) % displayImages.value.length
  setActiveImageWithMotion(displayImages.value[nextIndex], "left")
}

const showNextImage = () => {
  if (!displayImages.value.length) return
  const nextIndex = (activeImageIndex.value + 1) % displayImages.value.length
  setActiveImageWithMotion(displayImages.value[nextIndex], "right")
}

const matchedBackendProduct = computed(() => {
  return currentProduct.value?.raw || null
})

const displayedProductCode = computed(() => {
  return matchedBackendProduct.value?.maSanPham || currentProduct.value.sku
})

const backendVariants = computed(() => {
  const rows = getActiveVariants(matchedBackendProduct.value)

  return rows.map((variant, index) => ({
    id: variant?.id,
    maChiTietSanPham: String(variant?.maChiTietSanPham || variant?.maSanPhamChiTiet || "").trim(),
    colorName: String(variant?.mauSac?.tenMau || "").trim(),
    sizeName: String(variant?.kichThuoc?.tenKichThuoc || "").trim(),
    price: Number(variant?.giaBan || 0),
    soLuong: Number(variant?.soLuong || 0),
    giaBanGoc: Number(variant?.giaBanGoc || 0),
    sortIndex: index
  }))
})

const currentStock = computed(() => {
  const selected = Number(selectedBackendVariant.value?.soLuong || 0)
  if (selected > 0) return selected
  if (backendVariants.value.length) {
    return backendVariants.value.reduce((sum, variant) => sum + Math.max(Number(variant?.soLuong || 0), 0), 0)
  }
  return 0
})

const stockBadge = computed(() => {
  const qty = Number(currentStock.value || 0)
  if (qty > 0) return { text: `Còn lại ${qty} sản phẩm`, tone: "green" }
  return { text: "Hết hàng", tone: "dark" }
})

const colorHexByName = (name) => {
  const normalized = normalizeKeyword(name)
  if (normalized.includes("den")) return "#1a1a1a"
  if (normalized.includes("do") || normalized.includes("red")) return "#dc2626"
  if (normalized.includes("trang") || normalized.includes("kem")) return "#ded7ca"
  if (normalized.includes("xanh la") || normalized.includes("green")) return "#4f6f52"
  if (normalized.includes("nau")) return "#6b412c"
  if (normalized.includes("hong")) return "#d684a1"
  if (normalized.includes("vang") || normalized.includes("yellow")) return "#d4a017"
  if (normalized.includes("cam") || normalized.includes("orange")) return "#e07020"
  if (normalized.includes("xam") || normalized.includes("ghi")) return "#7f858f"
  if (normalized.includes("xanh")) return "#2f4f75"
  return "#9ca3af"
}

const effectiveColors = computed(() => {
  if (backendVariants.value.length) {
    const colorMap = new Map()
    for (const variant of backendVariants.value) {
      if (!variant.colorName || colorMap.has(variant.colorName)) continue
      colorMap.set(variant.colorName, { name: variant.colorName, hex: colorHexByName(variant.colorName) })
    }
    if (colorMap.size) return [...colorMap.values()]
  }

  return (currentProduct.value.colors || []).map((color) => ({
    name: color?.name || "",
    hex: color?.hex || colorHexByName(color?.name)
  }))
})

const effectiveSizes = computed(() => {
  if (backendVariants.value.length) {
    const options = backendVariants.value
      .filter((variant) => !selectedColor.value || variant.colorName === selectedColor.value)
      .map((variant) => variant.sizeName)
      .filter(Boolean)

    const unique = [...new Set(options)]
    if (unique.length) return unique
  }

  return currentProduct.value.sizes || []
})

const selectedBackendVariant = computed(() => {
  if (!backendVariants.value.length) return null
  const exact = backendVariants.value.find((variant) => {
    return variant.colorName === selectedColor.value && variant.sizeName === selectedSize.value
  })
  if (exact) return exact

  return backendVariants.value.find((variant) => variant.colorName === selectedColor.value)
    || backendVariants.value.find((variant) => variant.sizeName === selectedSize.value)
    || backendVariants.value[0]
})

const effectivePrice = computed(() => {
  const backendPrice = Number(selectedBackendVariant.value?.price || 0)
  if (backendPrice > 0) return backendPrice
  return Number(currentProduct.value.price || 0)
})

const effectiveOriginalPrice = computed(() => {
  const nowPrice = Number(effectivePrice.value || 0)
  // When a campaign discount is active, show the pre-campaign shelf price as the original
  if (activeCampaignInfo.value?.giaTri > 0 && nowPrice > 0) {
    const giaBanGoc = Number(selectedBackendVariant.value?.giaBanGoc || 0)
    if (giaBanGoc > nowPrice) return giaBanGoc
    // fallback: back-calculate from the discount percentage
    return Math.round(nowPrice / (1 - activeCampaignInfo.value.giaTri / 100))
  }
  const oldPrice = Number(currentProduct.value.originalPrice || 0)
  if (oldPrice > nowPrice) return oldPrice
  return 0
})

const relatedProducts = computed(() => {
  const currentId = currentProduct.value.id
  const source = productCatalog.value
  const sameCategory = source.filter((item) => item.id !== currentId && item.category === currentProduct.value.category)
  const otherProducts = source.filter((item) => item.id !== currentId && item.category !== currentProduct.value.category)
  return [...sameCategory, ...otherProducts].slice(0, 5)
})

const productDiscount = computed(() => {
  const oldPrice = Number(effectiveOriginalPrice.value || 0)
  const currentPrice = Number(effectivePrice.value || 0)
  if (!oldPrice || oldPrice <= currentPrice) return 0
  return Math.round(((oldPrice - currentPrice) / oldPrice) * 100)
})

const displayedSubtotalPrice = computed(() => Number(effectivePrice.value || 0) * Number(quantity.value || 1))

const displayedSubtotalOriginalPrice = computed(() => {
  const oldPrice = Number(effectiveOriginalPrice.value || 0)
  if (oldPrice <= 0) return 0
  return oldPrice * Number(quantity.value || 1)
})

const userInitials = computed(() => {
  const words = String(userDisplayName.value || "KH").trim().split(/\s+/).filter(Boolean)
  if (words.length >= 2) return `${words[0][0]}${words[words.length - 1][0]}`.toUpperCase()
  return String(userDisplayName.value || "KH").slice(0, 2).toUpperCase()
})

const cartCount = computed(() => {
  cartVersion.value
  const stored = readCartObject()
  return Object.values(stored).reduce((sum, value) => sum + Number(value || 0), 0)
})

const buildVariantCartKey = (id, color = "", size = "", voucherCode = "") => {
  const base = String(id || "").trim()
  const c = String(color || "").trim()
  const s = String(size || "").trim()
  const voucher = String(voucherCode || "").trim().toUpperCase()
  return c || s || voucher ? `${base}__${c}__${s}__${voucher}` : base
}

const parseVariantCartKey = (rawKey) => {
  const raw = String(rawKey || "")
  const [idPart, colorPart = "", sizePart = "", voucherPart = ""] = raw.split("__")
  return {
    productId: Number(idPart) || 0,
    color: String(colorPart || "").trim(),
    size: String(sizePart || "").trim(),
    voucherCode: String(voucherPart || "").trim().toUpperCase(),
  }
}

const resolveStoredLinePrice = (productId, color = "", size = "") => {
  const product = productCatalog.value.find((item) => Number(item.id) === Number(productId))
  if (!product) return 0

  const variants = getActiveVariants(product?.raw)
  const exact = variants.find((variant) => {
    const vColor = String(variant?.mauSac?.tenMau || "").trim()
    const vSize = String(variant?.kichThuoc?.tenKichThuoc || "").trim()
    return (!color || vColor === color) && (!size || vSize === size)
  })

  const matched = exact
    || variants.find((variant) => String(variant?.mauSac?.tenMau || "").trim() === color)
    || variants.find((variant) => String(variant?.kichThuoc?.tenKichThuoc || "").trim() === size)

  const variantPrice = Number(matched?.giaBan || 0)
  if (variantPrice > 0) return variantPrice
  return Number(product.price || 0)
}

const voucherBaseAmount = computed(() => Number(effectivePrice.value || 0) * Number(quantity.value || 1))

const estimatedVoucherSubtotal = computed(() => voucherBaseAmount.value)

const voucherDiscountAmount = computed(() => {
  if (!selectedVoucherInDetail.value) return 0
  return calculateVoucherDiscount(selectedVoucherInDetail.value, estimatedVoucherSubtotal.value)
})

const priceAfterVoucher = computed(() => Math.max(0, estimatedVoucherSubtotal.value - voucherDiscountAmount.value))

const applicableVouchers = computed(() => {
  return vouchers.value.filter((voucher) => isVoucherApplicable(voucher, estimatedVoucherSubtotal.value, null))
})

const promoLines = computed(() => {
  return vouchers.value.slice(0, 4).map((voucher) => {
    const val = Number(voucher.giaTriGiamGia || 0)
    const discountLabel = voucher.hinhThucGiam
      ? `giảm ${val}%`
      : `giảm ${val >= 1000 ? Math.round(val / 1000) + 'K' : val + 'đ'}`
    return { code: voucher.maPhieuGiamGia, discountLabel, min: VND(voucher.hoaDonToiThieu || 0), voucher }
  })
})

// Show all loaded vouchers – backend filters by active; no extra client-side blocking
const voucherChips = computed(() => vouchers.value.slice(0, 4))

const voucherPreview = computed(() => voucherChips.value)

const voucherChipLabel = (voucher) => {
  return String(voucher?.maPhieuGiamGia || "")
}

const syncProductState = () => {
  activeImage.value = displayImages.value[0] || ""
  selectedColor.value = effectiveColors.value[0]?.name || ""
  selectedSize.value = effectiveSizes.value[0] || ""
  quantity.value = 1
}

const loadBackendProducts = async () => {
  productsLoading.value = true
  try {
    const response = await getAllSanPham()
    const backendData = Array.isArray(response?.data) ? response.data : []
    const withCampaignPrice = await Promise.all(backendData.map(async (item) => {
      const variants = getActiveVariants(item)
      if (!variants.length) return item
      const productId = Number(item?.id || 0)
      const pricedVariants = await applyCampaignPriceToVariants(variants, productId)
      return {
        ...item,
        sanPhamChiTiets: pricedVariants,
      }
    }))
    backendProducts.value = withCampaignPrice
  } catch {
    backendProducts.value = []
  } finally {
    productsLoading.value = false
  }
}

const loadCurrentUser = async () => {
  const userEmail = String(localStorage.getItem("userEmail") || "").trim().toLowerCase()
  userAvatar.value = ""
  userRoleLabel.value = "Khách hàng"
  if (userEmail) userDisplayName.value = toDisplayNameFromEmail(userEmail)

  try {
    const account = await resolveAccountByRole({
      service: taiKhoanService,
      expectedRoles: ['CUSTOMER', 'KHACH_HANG', 'KHACHHANG', 'USER'],
      allowFallback: false
    })
    if (!account) return

    userRoleLabel.value = toRoleLabel(account?.vaiTro)
    if (account?.email) userDisplayName.value = toDisplayNameFromEmail(account.email)

    try {
      const customerRes = await getKhachHangByTaiKhoanId(account.id)
      if (customerRes?.data?.tenKhachHang) {
        userDisplayName.value = String(customerRes.data.tenKhachHang)
      }
    } catch {
      const mappedName = getVietnameseNameByEmail(account?.email)
      if (mappedName) userDisplayName.value = mappedName
    }

    const localAvatar = account?.id ? localStorage.getItem(getAvatarStorageKey(account.id)) : ""
    userAvatar.value = localAvatar || String(account?.avatar || "")
  } catch {
    userAvatar.value = ""
  }
}

const loadVouchers = async () => {
  loadingVouchers.value = true
  try {
    const normalizeVoucher = (voucher) => ({
      ...voucher,
      id: voucher?.id ?? voucher?.phieuGiamGiaId ?? voucher?.maPhieuGiamGia ?? voucher?.maPhieu,
      maPhieuGiamGia: String(voucher?.maPhieuGiamGia || voucher?.maPhieu || voucher?.maKhuyenMai || voucher?.code || "").trim(),
      tenPhieuGiamGia: String(voucher?.tenPhieuGiamGia || voucher?.tenKhuyenMai || voucher?.tenPhieu || "").trim(),
      giaTriGiamGia: Number(voucher?.giaTriGiamGia ?? voucher?.giaTriGiam ?? 0),
      hoaDonToiThieu: Number(voucher?.hoaDonToiThieu ?? voucher?.donToiThieu ?? voucher?.dieuKienToiThieu ?? 0),
      soTienGiamToiDa: Number(voucher?.soTienGiamToiDa ?? 0)
    })

    let payload = []
    try {
      const response = await getActiveVouchers()
      const activeData = response?.data
      payload = Array.isArray(activeData) ? activeData : (Array.isArray(activeData?.content) ? activeData.content : [])
      if (!payload.length) {
        const fallbackResponse = await getAllVouchers()
        const fallbackData = fallbackResponse?.data
        payload = Array.isArray(fallbackData) ? fallbackData : (Array.isArray(fallbackData?.content) ? fallbackData.content : [])
      }
    } catch {
      const response = await getAllVouchers()
      const allData = response?.data
      payload = Array.isArray(allData) ? allData : (Array.isArray(allData?.content) ? allData.content : [])
    }
    vouchers.value = payload.map(normalizeVoucher).filter((voucher) => voucher.maPhieuGiamGia).slice(0, 4)
  } catch {
    vouchers.value = []
  } finally {
    loadingVouchers.value = false
  }
}

const persistCheckoutVoucherSelection = (voucher) => {
  try {
    if (voucher) {
      localStorage.setItem('checkoutSelectedVoucher', JSON.stringify(voucher))
      localStorage.removeItem(CHECKOUT_PENDING_ITEM_VOUCHERS_KEY)
      return
    }

    localStorage.removeItem(CHECKOUT_PENDING_ITEM_VOUCHERS_KEY)
    localStorage.removeItem('checkoutSelectedVoucher')
  } catch {}
}

const selectVoucher = (voucher) => {
  if (!isVoucherApplicable(voucher, estimatedVoucherSubtotal.value, null)) {
    const needed = Number(voucher.hoaDonToiThieu || 0) - estimatedVoucherSubtotal.value
    if (needed > 0) {
      toast.error(`Cần thêm ${VND(needed)} để áp dụng voucher này`)
    } else {
      toast.error('Voucher không khả dụng')
    }
    return
  }
  selectedVoucherInDetail.value = voucher
  persistCheckoutVoucherSelection(voucher)
  toast.success(`Đã áp dụng ${voucher.maPhieuGiamGia} - tiết kiệm ${VND(calculateVoucherDiscount(voucher, estimatedVoucherSubtotal.value))}`)
  closeVoucherDrawer()
}

const validateSelection = () => {
  if (!selectedColor.value) {
    toast.error("Vui lòng chọn màu sắc")
    return false
  }
  if (!selectedSize.value) {
    toast.error("Vui lòng chọn kích thước")
    return false
  }

  const availableStock = Number(selectedBackendVariant.value?.soLuong || 0)
  if (availableStock > 0 && Number(quantity.value || 0) > availableStock) {
    toast.error(`Số lượng vượt tồn kho. Còn lại: ${availableStock}`)
    return false
  }

  if (Number(quantity.value || 0) <= 0) {
    toast.error("Số lượng phải lớn hơn 0")
    return false
  }

  return true
}

const refreshCartCount = () => {
  cartVersion.value += 1
}

const notifyCartUpdated = () => {
  window.dispatchEvent(new Event(CART_UPDATED_EVENT))
}

const isLoggedIn = () => {
  const userId = String(localStorage.getItem("userId") || "").trim()
  const role = String(localStorage.getItem("role") || "").trim().toUpperCase()
  return Boolean(userId && role)
}

const addToCart = () => {
  if (!isLoggedIn()) {
    toast.error("Vui lòng đăng nhập trước khi thêm vào giỏ hàng")
    router.push("/auth/customer-login")
    return
  }
  if (!validateSelection()) return
  const storedCart = readCartObject()
  const color = String(selectedColor.value || "").trim()
  const size = String(selectedSize.value || "").trim()
  const key = buildVariantCartKey(currentProduct.value.id, color, size, "")
  storedCart[key] = Number(storedCart[key] || 0) + quantity.value
  writeCartObject(storedCart)

  const cartImage = activeImage.value
    || currentProduct.value.images?.[0]
    || fallbackImageForVariant({
      id: currentProduct.value.id,
      maSanPham: currentProduct.value.sku,
      tenSanPham: currentProduct.value.name,
      tenMauSac: color,
      maChiTietSanPham: selectedBackendVariant.value?.maChiTietSanPham || ''
    })
    || ""
  const variantsObj = readCartVariantsObject()
  variantsObj[key] = {
    color,
    size,
    voucherCode: "",
    spctId: selectedBackendVariant.value?.id || null,
    image: cartImage
  }
  writeCartVariantsObject(variantsObj)

  if (selectedVoucherInDetail.value) {
    persistCheckoutVoucherSelection(selectedVoucherInDetail.value)
  }

  refreshCartCount()
  notifyCartUpdated()
  toast.cartAdded({
    name: currentProduct.value.name,
    color,
    size,
    image: cartImage,
    price: currentProduct.value.salePrice || currentProduct.value.price,
    actionLabel: "Xem giỏ hàng",
  })
}

const buyNow = () => {
  if (!isLoggedIn()) {
    toast.error("Vui lòng đăng nhập trước khi mua hàng")
    router.push("/auth/customer-login")
    return
  }
  if (!validateSelection()) return
  if (selectedVoucherInDetail.value) {
    persistCheckoutVoucherSelection(selectedVoucherInDetail.value)
  } else {
    localStorage.removeItem('checkoutSelectedVoucher')
  }
  const buyImg = activeImage.value
    || currentProduct.value.images?.[0]
    || fallbackImageForVariant({
      id: currentProduct.value.id,
      maSanPham: currentProduct.value.sku,
      tenSanPham: currentProduct.value.name,
      tenMauSac: selectedColor.value,
      maChiTietSanPham: selectedBackendVariant.value?.maChiTietSanPham || ''
    })
    || ""
  writeCheckoutCartArray([{
    id: currentProduct.value.id,
    name: currentProduct.value.name,
    price: effectivePrice.value,
    quantity: quantity.value,
    size: selectedSize.value,
    color: selectedColor.value,
    voucherCode: "",
    spctId: selectedBackendVariant.value?.id || null,
    image: buyImg
  }])
  router.push("/checkout")
}

const handleSearch = () => {
  if (!searchQuery.value.trim()) {
    router.push("/trang-chu")
    return
  }
  router.push({ path: "/trang-chu", query: { q: searchQuery.value.trim() } })
}

const closeVoucherDrawer = () => {
  voucherDrawerOpen.value = false
  document.body.style.overflow = ""
}

const openVoucherDrawer = () => {
  voucherDrawerOpen.value = true
  document.body.style.overflow = "hidden"
}

const openProfilePage = () => {
  profileOpen.value = false
  router.push("/customer/profile")
}

const openOrdersPage = () => {
  profileOpen.value = false
  router.push({ path: "/customer/profile", query: { tab: "orders" } })
}

const toggleMobileMenu = () => {
  mobileOpen.value = !mobileOpen.value
}

const toggleProfileMenu = (event) => {
  event.stopPropagation()
  profileOpen.value = !profileOpen.value
}

const browseCategory = (category) => {
  mobileOpen.value = false
  router.push({ path: "/trang-chu", query: category ? { category } : {} })
}

const openHomeAnchor = (hash = "") => {
  mobileOpen.value = false
  router.push(hash ? { path: "/trang-chu", hash: `#${hash}` } : "/trang-chu")
}

const goHome = () => router.push("/trang-chu")
const openCart = () => router.push("/gio-hang")
const goToProductDetail = (id) => {
  closeQuickPreview()
  forceScrollTop()
  router.push(`/product/${id}`)
}

const openQuickPreview = (product) => {
  quickPreviewProduct.value = product
  quickPreviewColor.value = product?.colors?.[0]?.name || ""
  quickPreviewSize.value = product?.sizes?.[0] || ""
  quickPreviewQty.value = 1
  quickPreviewImageIndex.value = 0
}

const closeQuickPreview = () => {
  quickPreviewProduct.value = null
}

const getQuickPreviewCode = (product) => {
  if (!product) return ""
  const byId = backendProducts.value.find(item => Number(item?.id) === Number(product.id))
  if (byId?.maSanPham) return byId.maSanPham
  const localName = normalizeKeyword(product.name)
  const byName = backendProducts.value.find(item => normalizeKeyword(item?.tenSanPham) === localName)
  return byName?.maSanPham || product.sku || String(product.id)
}

const getQuickPreviewBackendProduct = (product) => {
  if (!product) return null
  const byId = backendProducts.value.find((item) => Number(item?.id) === Number(product.id))
  if (byId) return byId
  const name = normalizeKeyword(product?.name)
  return backendProducts.value.find((item) => normalizeKeyword(item?.tenSanPham) === name) || null
}

const resolveQuickPreviewColorImage = (product, colorName = "", sizeName = "") => {
  if (!product || !colorName) return ""

  const backend = getQuickPreviewBackendProduct(product)
  const variants = getActiveVariants(backend)
  const exact = variants.find((variant) => {
    const variantColor = String(variant?.mauSac?.tenMau || "").trim()
    const variantSize = String(variant?.kichThuoc?.tenKichThuoc || "").trim()
    const sameColor = normalizeKeyword(variantColor) === normalizeKeyword(colorName)
    const sameSize = !sizeName || variantSize === sizeName
    return sameColor && sameSize
  }) || variants.find((variant) => normalizeKeyword(variant?.mauSac?.tenMau || "") === normalizeKeyword(colorName))

  const variantImages = pickImageValues([exact, exact?.anh, exact?.hinhAnh, exact?.image, exact?.imageUrl, exact?.duongDanAnh])
  if (variantImages.length) return variantImages[0]

  const colorIndex = Array.isArray(product?.colors)
    ? product.colors.findIndex((color) => normalizeKeyword(color?.name) === normalizeKeyword(colorName))
    : -1
  if (colorIndex >= 0 && Array.isArray(product?.images) && product.images[colorIndex]) {
    return product.images[colorIndex]
  }

  return String(product?.images?.[0] || "").trim()
}

const quickPreviewImages = computed(() => {
  const product = quickPreviewProduct.value
  if (!product) return []

  const candidates = []
  if (Array.isArray(product.images)) {
    candidates.push(...product.images.map((img) => String(img || "").trim()).filter(Boolean))
  }

  const colors = Array.isArray(product.colors) ? product.colors : []
  for (const color of colors) {
    const name = String(color?.name || "").trim()
    if (!name) continue
    const image = resolveQuickPreviewColorImage(product, name, quickPreviewSize.value)
    if (image) candidates.push(image)
  }

  if (product?.images?.[0]) candidates.push(String(product.images[0]).trim())

  return [...new Set(candidates.map((img) => String(img || "").trim()).filter(Boolean))]
})

const quickPreviewActiveImage = computed(() => {
  const images = quickPreviewImages.value
  if (!images.length) {
    return quickPreviewProduct.value?.images?.[0]
      || fallbackImageFor(quickPreviewProduct.value?.id || 0, getQuickPreviewCode(quickPreviewProduct.value))
  }
  const safeIndex = ((quickPreviewImageIndex.value % images.length) + images.length) % images.length
  return images[safeIndex]
})

const syncQuickPreviewColorWithImage = () => {
  const product = quickPreviewProduct.value
  if (!product || !Array.isArray(product.colors) || !product.colors.length) return
  const currentImage = quickPreviewActiveImage.value
  if (!currentImage) return

  const matchedByImage = product.colors.findIndex((color) => {
    const image = resolveQuickPreviewColorImage(product, String(color?.name || "").trim(), quickPreviewSize.value)
    return image && isSameImage(image, currentImage)
  })

  if (matchedByImage >= 0) {
    quickPreviewColor.value = product.colors[matchedByImage]?.name || quickPreviewColor.value
    return
  }

  const index = quickPreviewImages.value.findIndex((img) => isSameImage(img, currentImage))
  if (index >= 0 && index < product.colors.length) {
    quickPreviewColor.value = product.colors[index]?.name || quickPreviewColor.value
  }
}

const quickPreviewPrevImage = () => {
  const total = quickPreviewImages.value.length
  if (total <= 1) return
  quickPreviewImageIndex.value = (quickPreviewImageIndex.value - 1 + total) % total
  syncQuickPreviewColorWithImage()
}

const quickPreviewNextImage = () => {
  const total = quickPreviewImages.value.length
  if (total <= 1) return
  quickPreviewImageIndex.value = (quickPreviewImageIndex.value + 1) % total
  syncQuickPreviewColorWithImage()
}

const selectQuickPreviewColor = (colorName, index) => {
  quickPreviewColor.value = colorName
  const image = resolveQuickPreviewColorImage(quickPreviewProduct.value, colorName, quickPreviewSize.value)
  if (image) {
    const mappedIndex = quickPreviewImages.value.findIndex((img) => isSameImage(img, image))
    if (mappedIndex >= 0) {
      quickPreviewImageIndex.value = mappedIndex
      return
    }
  }

  if (Number.isFinite(Number(index)) && index >= 0 && index < quickPreviewImages.value.length) {
    quickPreviewImageIndex.value = index
  }
}

const quickPreviewAddToCart = () => {
  if (!quickPreviewProduct.value?.id) return
  const storedCart = readCartObject()
  const color = String(quickPreviewColor.value || "").trim()
  const size = String(quickPreviewSize.value || "").trim()
  const key = buildVariantCartKey(quickPreviewProduct.value.id, color, size, "")
  storedCart[key] = Number(storedCart[key] || 0) + quickPreviewQty.value
  writeCartObject(storedCart)

  const qpImage = quickPreviewActiveImage.value
    || quickPreviewProduct.value.images?.[0]
    || fallbackImageForVariant({
      id: quickPreviewProduct.value.id,
      maSanPham: quickPreviewProduct.value.sku,
      tenSanPham: quickPreviewProduct.value.name,
      tenMauSac: color,
    })
    || ""
  const variantsObj = readCartVariantsObject()
  variantsObj[key] = {
    color,
    size,
    voucherCode: "",
    spctId: null,
    image: qpImage
  }
  writeCartVariantsObject(variantsObj)

  refreshCartCount()
  notifyCartUpdated()
  toast.cartAdded({
    name: quickPreviewProduct.value.name,
    color,
    size,
    image: qpImage,
    price: quickPreviewProduct.value.salePrice || quickPreviewProduct.value.price,
    actionLabel: "Xem giỏ hàng",
  })
  closeQuickPreview()
}

const logout = () => {
  profileOpen.value = false
  localStorage.removeItem("role")
  localStorage.removeItem("userId")
  localStorage.removeItem("userEmail")
  router.push("/auth/customer-login")
}

const handleDocumentClick = (event) => {
  const target = event.target
  if (!(target instanceof Element)) return
  if (!target.closest(".pd-profile")) {
    profileOpen.value = false
  }
}

watch(() => route.params.id, () => {
  profileOpen.value = false
  mobileOpen.value = false
  closeVoucherDrawer()
  closeQuickPreview()
  forceScrollTop()
})

watch(
  () => currentProduct.value.id,
  async (productId) => {
    syncProductState()
    // Load campaign discount info for this product
    activeCampaignInfo.value = null
    if (productId > 0) {
      const info = await getProductCampaignInfo(productId).catch(() => null)
      activeCampaignInfo.value = info
      if (info && info.giaTri > 0) {
        const endDate = info.ngayKetThuc ? new Date(info.ngayKetThuc).toLocaleDateString("vi-VN") : ""
        toast.info(`Sản phẩm đang được giảm ${Math.round(info.giaTri)}%${endDate ? ` đến ${endDate}` : ""} với chương trình "${info.tenKhuyenMai}"`, 6000)
      }
    }
  },
  { immediate: true }
)

watch(selectedColor, (colorName) => {
  if (!effectiveSizes.value.includes(selectedSize.value)) {
    selectedSize.value = effectiveSizes.value[0] || ""
  }

  const colorId = colorNameToIdMap.value[colorName]
  if (colorId && colorImageMap.value[colorId]) {
    setActiveImageWithMotion(colorImageMap.value[colorId], "auto")
    return
  }
  const byName = resolveColorImageByName(colorName)
  if (byName) {
    setActiveImageWithMotion(byName, "auto")
    return
  }
})

watch(activeImage, (image) => {
  if (!image) return

  const selectedColorId = Number(colorNameToIdMap.value?.[selectedColor.value] || 0)
  if (selectedColorId > 0 && isSameImage(colorImageMap.value?.[selectedColorId] || "", image)) {
    return
  }

  for (const color of effectiveColors.value) {
    const colorId = Number(colorNameToIdMap.value?.[color?.name] || 0)
    const colorImage = colorId > 0 ? String(colorImageMap.value?.[colorId] || "") : ""
    const fallbackImage = resolveColorImageByName(color?.name || "")
    const targetImage = colorImage || fallbackImage
    if (targetImage && isSameImage(targetImage, image)) {
      if (selectedColor.value !== color.name) selectedColor.value = color.name
      return
    }
  }

  const displayIdx = displayImages.value.findIndex((entry) => isSameImage(entry, image))
  if (displayIdx >= 0 && displayIdx < effectiveColors.value.length) {
    const mappedColor = effectiveColors.value[displayIdx]?.name || ""
    if (mappedColor && selectedColor.value !== mappedColor) selectedColor.value = mappedColor
  }
})

watch(quickPreviewImages, (images) => {
  if (!images.length) {
    quickPreviewImageIndex.value = 0
    return
  }
  if (quickPreviewImageIndex.value >= images.length) quickPreviewImageIndex.value = 0
})

watch(quickPreviewImageIndex, () => {
  syncQuickPreviewColorWithImage()
})

watch(effectiveColors, (colors) => {
  if (!colors.some((color) => color.name === selectedColor.value)) {
    selectedColor.value = colors[0]?.name || ""
  }
})

onMounted(async () => {
  await Promise.all([loadCurrentUser(), loadVouchers(), loadBackendProducts()])
  syncProductState()
  forceScrollTop()
  document.addEventListener("click", handleDocumentClick)
  window.addEventListener("storage", refreshCartCount)
  window.addEventListener(AUTH_CONTEXT_CHANGED_EVENT, loadCurrentUser)
})

onUnmounted(() => {
  document.removeEventListener("click", handleDocumentClick)
  window.removeEventListener("storage", refreshCartCount)
  window.removeEventListener(AUTH_CONTEXT_CHANGED_EVENT, loadCurrentUser)
  document.body.style.overflow = ""
})
</script>

<template>
  <div class="product-detail-page" :style="galleryMotionVars">
    <SiteNav :cart-count="cartCount" />

    <main class="pd-shell">
      <div class="pd-breadcrumb">
        <button type="button" @click="goHome">Trang chủ</button>
        <ChevronRight :size="14" />
        <strong>{{ productsLoading ? 'Đang tải...' : currentProduct.name }}</strong>
      </div>

      <section v-if="productsLoading" class="pd-main" style="min-height:60vh;display:flex;align-items:center;justify-content:center">
        <span style="color:#888;font-size:1rem">Đang tải sản phẩm…</span>
      </section>

      <section v-else class="pd-main">
        <div class="pd-gallery" :class="{ 'pd-gallery--single': displayImages.length <= 1 }">
          <div v-if="displayImages.length > 1" class="pd-gallery__thumbs">
            <button
              v-for="(image, index) in displayImages"
              :key="`${image}-${index}`"
              type="button"
              class="pd-gallery__thumb"
              :class="{ 'is-active': image === activeImage }"
              @click="selectGalleryImage(image)"
            >
              <img :src="image" :alt="currentProduct.name" />
            </button>
          </div>

          <div class="pd-gallery__stage">
            <transition :name="galleryTransitionName" mode="out-in">
              <img
                :key="normalizeImageKey(activeImage || displayImages[0])"
                class="pd-gallery__image"
                :src="activeImage || displayImages[0]"
                :alt="currentProduct.name"
              />
            </transition>
            <button v-if="displayImages.length > 1" type="button" class="pd-gallery__arrow pd-gallery__arrow--left" aria-label="Ảnh trước" @click="showPrevImage">
              <ChevronLeft :size="18" />
            </button>
            <button v-if="displayImages.length > 1" type="button" class="pd-gallery__arrow pd-gallery__arrow--right" aria-label="Ảnh tiếp theo" @click="showNextImage">
              <ChevronRight :size="18" />
            </button>
          </div>
        </div>

        <div class="pd-info">
          <div class="pd-info__panel">
            <div class="pd-info__topline">
              <h1>{{ currentProduct.name }}</h1>
              <span class="pd-stock" :class="`is-${stockBadge.tone}`">{{ stockBadge.text }}</span>
            </div>

            <p class="pd-sku">Mã sản phẩm: {{ displayedProductCode }}</p>

            <div class="pd-price-row">
              <strong>{{ VND(displayedSubtotalPrice) }}</strong>
              <s v-if="displayedSubtotalOriginalPrice">{{ VND(displayedSubtotalOriginalPrice) }}</s>
              <span v-if="productDiscount" class="pd-discount">-{{ productDiscount }}%</span>
            </div>

            <div v-if="activeCampaignInfo && activeCampaignInfo.giaTri > 0" class="pd-campaign-banner">
              <span class="pd-campaign-banner__badge">🏷️ Giảm {{ Math.round(activeCampaignInfo.giaTri) }}%</span>
              <span class="pd-campaign-banner__period">
                Từ {{ new Date(activeCampaignInfo.ngayBatDau).toLocaleDateString('vi-VN') }}
                đến {{ new Date(activeCampaignInfo.ngayKetThuc).toLocaleDateString('vi-VN') }}
              </span>
            </div>

            <section class="pd-promo-box">
              <div class="pd-promo-box__title">
                <Ticket :size="15" />
                <strong>Ưu đãi online</strong>
              </div>
              <ul>
                <li
                  v-for="line in promoLines"
                  :key="line.code"
                  class="pd-promo-box__line"
                  @click="selectVoucher(line.voucher)"
                >
                  Nhập mã <b>{{ line.code }}</b> {{ line.discountLabel }} đơn từ {{ line.min }}
                </li>
                <li v-if="!promoLines.length">Hiện chưa có voucher khả dụng theo dữ liệu khuyến mãi hiện tại.</li>
                <li>Freeship đơn từ 299K</li>
              </ul>
            </section>

            <div v-if="effectiveColors.length > 0" class="pd-option-row">
              <span class="pd-label">Màu sắc: <b>{{ selectedColor }}</b></span>
              <div class="pd-colors">
                <button
                  v-for="color in effectiveColors"
                  :key="color.name"
                  type="button"
                  class="pd-color"
                  :class="{ 'is-active': selectedColor === color.name }"
                  :title="color.name"
                  @click="selectedColor = color.name"
                >
                  <span :style="{ background: color.hex }"></span>
                </button>
              </div>
            </div>

            <div class="pd-option-row pd-option-row--size">
              <div class="pd-size-head">
                <span class="pd-label">Kích thước</span>
                <button type="button" class="pd-size-guide">
                  <Ruler :size="14" />
                  Hướng dẫn chọn size
                </button>
              </div>
              <div class="pd-sizes">
                <button
                  v-for="size in effectiveSizes"
                  :key="size"
                  type="button"
                  class="pd-size"
                  :class="{ 'is-active': selectedSize === size }"
                  @click="selectedSize = size"
                >
                  {{ size }}
                </button>
              </div>
            </div>

            <div class="pd-buy-row">
              <div class="pd-qty">
                <button type="button" @click="quantity = Math.max(1, quantity - 1)">-</button>
                <input type="number" v-model.number="quantity" min="1" class="pd-qty-input" @change="quantity = Math.max(1, Math.floor(quantity) || 1)" />
                <button type="button" @click="quantity += 1">+</button>
              </div>
              <button type="button" class="pd-cart-button" @click="addToCart">THÊM VÀO GIỎ</button>
            </div>

            <button type="button" class="pd-buy-now" @click="buyNow">MUA NGAY</button>

            <div class="pd-service-grid">
              <article>
                <Truck :size="16" />
                <span>Freeship đơn từ 299K</span>
              </article>
              <article>
                <ShieldCheck :size="16" />
                <span>Cam kết chính hãng</span>
              </article>
              <article>
                <Wallet :size="16" />
                <span>Thanh toán COD</span>
              </article>
              <article>
                <Undo2 :size="16" />
                <span>Đổi hàng trong 15 ngày</span>
              </article>
            </div>
          </div>
        </div>
      </section>

      <section class="pd-tabs">
        <div class="pd-tabs__head">
          <button type="button" :class="{ active: activeTab === 'description' }" @click="activeTab = 'description'">Mô tả</button>
          <button type="button" :class="{ active: activeTab === 'shipping' }" @click="activeTab = 'shipping'">Chính sách giao hàng</button>
          <button type="button" :class="{ active: activeTab === 'return' }" @click="activeTab = 'return'">Chính sách đổi hàng</button>
        </div>

        <div v-if="activeTab === 'description'" class="pd-tabs__panel">
          <h2>{{ currentProduct.name }}</h2>
          <ul class="pd-bullets">
            <li><strong>Chất liệu:</strong> {{ currentProduct.material }}</li>
            <li><strong>Form:</strong> {{ currentProduct.fit }}</li>
          </ul>
          <p>{{ currentProduct.description.intro }}</p>
          <h3>Chất liệu</h3>
          <p>{{ currentProduct.description.material }}</p>
          <h3>Kỹ thuật thiết kế</h3>
          <p>{{ currentProduct.description.design }}</p>
          <h3>Form dáng</h3>
          <p>{{ currentProduct.description.fit }}</p>
        </div>

        <div v-else-if="activeTab === 'shipping'" class="pd-tabs__panel">
          <h2>Chính sách giao hàng</h2>
          <p>DirtyWave giao hàng toàn quốc. Đơn từ 299.000₫ được áp dụng ưu đãi phí vận chuyển theo từng thời điểm, và đơn từ 1.000.000₫ được miễn phí ship tại trang checkout hiện tại.</p>
          <p>Thời gian giao dự kiến từ 2 đến 5 ngày làm việc tuỳ khu vực nhận hàng.</p>
        </div>

        <div v-else class="pd-tabs__panel">
          <h2>Chính sách đổi hàng</h2>
          <p>Hỗ trợ đổi size trong vòng 15 ngày với sản phẩm còn nguyên tình trạng sử dụng, tem nhãn đầy đủ và không có dấu hiệu giặt tẩy.</p>
          <p>Bạn có thể theo dõi và quản lý đơn đổi trong trung tâm tài khoản.</p>
        </div>
      </section>

      <section class="pd-related">
        <h2>Sản phẩm cùng loại</h2>
        <div class="pd-related__grid">
          <article
            v-for="item in relatedProducts"
            :key="item.id"
            class="pd-related__card"
            @click="goToProductDetail(item.id)"
          >
            <div class="pd-related__image">
              <img :src="item.images[0]" :alt="item.name" />
              <div class="pd-related__actions">
                <button type="button" class="pd-related__action" @click.stop="openQuickPreview(item)">
                  <Eye :size="16" />
                </button>
                <button type="button" class="pd-related__action" @click.stop="goToProductDetail(item.id)">
                  <ShoppingCart :size="16" />
                </button>
              </div>
            </div>
            <div class="pd-related__body">
              <small>{{ item.category }}</small>
              <span class="pd-related__stock">{{ item.badgeTone === 'green' ? item.badge.replace('Còn lại ', 'Còn: ') : item.badge }}</span>
              <p>{{ item.name }}</p>
              <strong>{{ VND(item.price) }}</strong>
              <s v-if="item.originalPrice">{{ VND(item.originalPrice) }}</s>
              <div class="pd-related__dots">
                <span v-for="color in item.colors.slice(0, 3)" :key="color.name" :style="{ background: color.hex }"></span>
              </div>
            </div>
          </article>
        </div>
      </section>
    </main>

    <div v-if="quickPreviewProduct" class="pd-quick-view" @click.self="closeQuickPreview">
      <article class="pd-quick-view__card">
        <button type="button" class="pd-quick-view__close" @click="closeQuickPreview">
          <X :size="16" />
        </button>
        <div class="pd-quick-view__image">
          <img :src="quickPreviewActiveImage" :alt="quickPreviewProduct.name" />
          <button
            v-if="quickPreviewImages.length > 1"
            type="button"
            class="pd-quick-view__nav pd-quick-view__nav--prev"
            aria-label="Ảnh trước"
            @click="quickPreviewPrevImage"
          >‹</button>
          <button
            v-if="quickPreviewImages.length > 1"
            type="button"
            class="pd-quick-view__nav pd-quick-view__nav--next"
            aria-label="Ảnh sau"
            @click="quickPreviewNextImage"
          >›</button>
        </div>
        <div class="pd-quick-view__body">
          <h3>{{ quickPreviewProduct.name }}</h3>
          <small class="pd-quick-view__code">Mã sản phẩm: {{ getQuickPreviewCode(quickPreviewProduct) }}</small>
          <strong class="pd-quick-view__price">{{ VND(quickPreviewProduct.price) }}</strong>

          <div class="pd-quick-view__option-row">
            <span class="pd-quick-view__label">Màu sắc: <b>{{ quickPreviewColor }}</b></span>
            <div class="pd-quick-view__swatches">
              <button
                v-for="(color, index) in quickPreviewProduct.colors"
                :key="color.name"
                type="button"
                class="pd-quick-view__swatch"
                :class="{ 'is-active': quickPreviewColor === color.name }"
                :style="{ background: color.hex }"
                :title="color.name"
                @click="selectQuickPreviewColor(color.name, index)"
              ></button>
            </div>
          </div>

          <div class="pd-quick-view__option-row">
            <span class="pd-quick-view__label">Kích thước: <b>{{ quickPreviewSize }}</b></span>
            <div class="pd-quick-view__sizes">
              <button
                v-for="size in quickPreviewProduct.sizes"
                :key="size"
                type="button"
                class="pd-quick-view__size"
                :class="{ 'is-active': quickPreviewSize === size }"
                @click="quickPreviewSize = size"
              >{{ size }}</button>
            </div>
          </div>

          <section class="pd-quick-view__promo">
            <div class="pd-quick-view__promo-title">
              <Ticket :size="13" />
              <b>ƯU ĐÃI ONLINE</b>
            </div>
            <ul>
              <li v-for="line in promoLines.slice(0, 3)" :key="line.code">
                  Nhập mã <b>{{ line.code }}</b> {{ line.discountLabel }} đơn từ {{ line.min }}
              </li>
            </ul>
          </section>

          <div class="pd-quick-view__buy-row">
            <div class="pd-quick-view__qty">
              <button type="button" @click="quickPreviewQty = Math.max(1, quickPreviewQty - 1)">−</button>
              <strong>{{ quickPreviewQty }}</strong>
              <button type="button" @click="quickPreviewQty += 1">+</button>
            </div>
            <button type="button" class="pd-quick-view__cart-btn" @click="quickPreviewAddToCart">THÊM VÀO GIỎ</button>
          </div>

          <button type="button" class="pd-quick-view__detail-link" @click="goToProductDetail(quickPreviewProduct.id)">
            Xem chi tiết »
          </button>
        </div>
      </article>
    </div>

    <transition name="pd-drawer">
      <div v-if="voucherDrawerOpen" class="pd-drawer-overlay" @click.self="closeVoucherDrawer">
        <aside class="pd-drawer">
          <div class="pd-drawer__head">
            <div>
              <small>Áp dụng cho sản phẩm hiện tại</small>
              <h3>Kho voucher DirtyWave</h3>
            </div>
            <button type="button" class="pd-drawer__close" @click="closeVoucherDrawer">
              <X :size="18" />
            </button>
          </div>

          <div class="pd-drawer__list">
            <article
              v-for="voucher in voucherChips"
              :key="voucher.id || voucher.maPhieuGiamGia"
              class="pd-drawer__voucher"
              :class="{ 'is-selected': selectedVoucherInDetail?.maPhieuGiamGia === voucher.maPhieuGiamGia }"
            >
              <div class="pd-drawer__voucher-info">
                <span class="pd-drawer__label">{{ voucher.tenPhieuGiamGia || 'Voucher' }}</span>
                <strong class="pd-drawer__amount">{{ VND(calculateVoucherDiscount(voucher, estimatedVoucherSubtotal)) }}</strong>
                <p class="pd-drawer__min">Đơn từ {{ VND(voucher.hoaDonToiThieu || 0) }}</p>
                <p class="pd-drawer__code">Mã: <b>{{ voucher.maPhieuGiamGia }}</b></p>
              </div>
              <button
                type="button"
                class="pd-apply-btn"
                :class="{ 'is-applied': selectedVoucherInDetail?.maPhieuGiamGia === voucher.maPhieuGiamGia }"
                @click="selectVoucher(voucher)"
              >
                {{ selectedVoucherInDetail?.maPhieuGiamGia === voucher.maPhieuGiamGia ? '✓ Đã chọn' : 'Áp dụng' }}
              </button>
            </article>
            <p v-if="!voucherChips.length" class="pd-drawer__empty">
              Hiện không có phiếu giảm giá đang hoạt động.
            </p>
          </div>
        </aside>
      </div>
    </transition>
  </div>
</template>

<style>
.product-detail-page {
  --pd-red: #c5162d;
  --pd-red-dark: #8f1121;
  --pd-red-soft: #fff1f2;
  --pd-red-line: #f4c6cd;
  --pd-ink: #151515;
  --pd-muted: #6f6a6d;
  --pd-line: #e8d8db;
  --pd-bg: #fbf7f8;
  --pd-shadow: 0 18px 40px rgba(143, 17, 33, 0.08);
  min-height: 100vh;
  background: radial-gradient(circle at top right, rgba(197, 22, 45, 0.08), transparent 24%), linear-gradient(180deg, #fffdfd 0%, #f8f1f2 100%);
  color: var(--pd-ink);
}

.product-detail-page,
.product-detail-page * {
  box-sizing: border-box;
}

.product-detail-page button {
  appearance: none;
  -webkit-appearance: none;
}

.product-detail-page img {
  display: block;
  max-width: 100%;
}

.product-detail-page a {
  color: inherit;
  text-decoration: none;
}

.pd-topbar {
  position: relative;
  overflow: hidden;
  border-bottom: 1px solid rgba(255, 255, 255, 0.18);
  background: linear-gradient(135deg, var(--pd-red) 0%, var(--pd-red-dark) 100%);
}

.pd-site-header__inner,
.pd-shell {
  width: min(1260px, calc(100% - 36px));
  margin: 0 auto;
}

.pd-topbar__inner {
  width: 100%;
  padding: 10px 18px;
}

.pd-marquee {
  overflow: hidden;
  white-space: nowrap;
}

.pd-marquee__track {
  display: inline-block;
  padding-left: 100%;
  color: white;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.04em;
  animation: pd-marquee 24s linear infinite;
}

@keyframes pd-marquee {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-100%);
  }
}

.pd-site-header {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid rgba(143, 17, 33, 0.08);
  backdrop-filter: blur(14px);
  box-shadow: 0 12px 28px rgba(21, 21, 21, 0.08);
}

.pd-site-header__inner {
  padding: 12px 0 10px;
}

.pd-nav-shell {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 24px;
}

.pd-brand {
  border: 0;
  padding: 0;
  background: transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.pd-brand__logo {
  width: 54px;
  height: 54px;
  border-radius: 14px;
  overflow: hidden;
  background: #111827;
  box-shadow: var(--pd-shadow);
  border-bottom: 3px solid var(--pd-red);
}

.pd-brand__logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scale(1.65);
  transform-origin: center;
  margin-top: -8px;
}

.pd-brand__text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.05;
}

.pd-brand__text strong {
  font-size: 17px;
  letter-spacing: 0.03em;
}

.pd-brand__text small {
  color: var(--pd-muted);
  font-size: 12px;
}

.pd-menu {
  display: flex;
  align-items: center;
  gap: 24px;
  min-width: 0;
}

.pd-menu > a,
.pd-dropdown > a,
.profile-dropdown button,
.pd-breadcrumb button,
.pd-size-guide,
.pd-voucher-chip,
.pd-tabs__head button,
.pd-drawer__close,
.pd-copy,
.pd-store-note,
.pd-icon-button,
.pd-gallery__thumb,
.pd-color,
.pd-size,
.pd-cart-button,
.pd-buy-now,
.pd-mobile-menu__panel button,
.pd-pill-button {
  border: 0;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.pd-menu > a,
.pd-dropdown > a {
  white-space: nowrap;
  padding: 10px 8px;
  border-radius: 12px;
  color: var(--pd-ink);
  font-size: 14px;
  font-weight: 600;
}

.pd-menu > a:hover,
.pd-dropdown > a:hover {
  background: rgba(197, 22, 45, 0.06);
}

.pd-dropdown {
  position: relative;
}

.pd-dropdown:hover .pd-dropdown-panel {
  display: block;
}

.pd-dropdown-panel {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  display: none;
  width: min(720px, 90vw);
  padding: 16px;
  border: 1px solid rgba(143, 17, 33, 0.12);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.99);
  box-shadow: 0 20px 50px rgba(21, 21, 21, 0.12);
}

.pd-dropdown-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 14px;
}

.pd-panel-block {
  border: 1px solid rgba(143, 17, 33, 0.09);
  border-radius: 16px;
  padding: 16px;
  background: linear-gradient(180deg, #fffefe 0%, #fff9fa 100%);
}

.pd-panel-block h4 {
  margin: 0 0 10px;
  color: var(--pd-muted);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.pd-panel-block p {
  margin: 8px 0 0;
  color: var(--pd-muted);
  font-size: 13px;
  line-height: 1.6;
}

.pd-panel-links {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.pd-panel-links a {
  padding: 10px 12px;
  border: 1px solid rgba(143, 17, 33, 0.09);
  border-radius: 12px;
  background: white;
  font-size: 14px;
  font-weight: 600;
}

.pd-panel-links a:hover {
  border-color: rgba(197, 22, 45, 0.3);
  color: var(--pd-red-dark);
}

.pd-panel-block--cta {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(135deg, rgba(197, 22, 45, 0.96) 0%, rgba(143, 17, 33, 0.98) 100%);
}

.pd-panel-block--cta h4,
.pd-panel-block--cta p {
  color: rgba(255, 255, 255, 0.9);
}

.pd-panel-block--soft {
  background: linear-gradient(135deg, #ffe6ea 0%, #ffd6dd 100%);
}

.pd-panel-block--soft h4,
.pd-panel-block--soft p {
  color: var(--pd-red-dark);
}

.pd-pill-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  align-self: flex-start;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  background: white;
  color: var(--pd-red-dark);
  font-size: 13px;
  font-weight: 700;
}

.pd-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pd-search {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 320px;
  padding: 10px 14px;
  border: 1px solid var(--pd-line);
  background: white;
  border-radius: 999px;
  box-shadow: inset 0 0 0 1px rgba(197, 22, 45, 0.02);
}

.pd-search svg {
  color: #7b7276;
}

.pd-search input {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  font: inherit;
  color: var(--pd-ink);
}

.pd-search input::placeholder {
  color: #9b9094;
}

.pd-icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 42px;
  height: 42px;
  border: 1px solid var(--pd-line);
  border-radius: 12px;
  background: white;
  color: var(--pd-ink);
  box-shadow: var(--pd-shadow);
}

.pd-icon-button:hover {
  border-color: rgba(197, 22, 45, 0.28);
}

.pd-hamburger {
  display: none;
}

.pd-cart-icon {
  position: relative;
}

.cart-count {
  position: absolute;
  top: -7px;
  right: -7px;
  display: inline-grid;
  place-items: center;
  min-width: 19px;
  height: 19px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--pd-red);
  color: white;
  font-size: 11px;
  font-weight: 700;
}

.profile-wrapper {
  position: relative;
}

.user-account-btn {
  border: 1px solid var(--pd-line);
  background: #fff;
  border-radius: 12px;
  min-height: 42px;
  padding: 6px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: var(--pd-shadow);
}

.profile-avatar-chip {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffe4e8 0%, #ffd3da 100%);
  color: var(--pd-red-dark);
  font-size: 12px;
  font-weight: 700;
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
  color: var(--pd-ink);
}

.profile-role {
  font-size: 11px;
  color: var(--pd-muted);
}

.profile-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 190px;
  padding: 8px;
  border: 1px solid var(--pd-line);
  border-radius: 14px;
  background: white;
  box-shadow: 0 18px 40px rgba(21, 21, 21, 0.14);
}

.dropdown-item {
  display: block;
  width: 100%;
  padding: 12px 14px;
  text-align: left;
  border-radius: 8px;
  color: var(--pd-ink);
  font-size: 14px;
  font-weight: 500;
}

.dropdown-item:hover {
  background: #f8f0f2;
}

.dropdown-item.danger {
  color: var(--pd-red);
}

.dropdown-item.danger:hover {
  background: #fee7eb;
}

.pd-mobile-menu {
  display: none;
  padding-top: 12px;
}

.pd-mobile-menu__panel {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid rgba(143, 17, 33, 0.1);
  border-radius: 18px;
  background: white;
  box-shadow: var(--pd-shadow);
}

.pd-mobile-menu__panel button {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-height: 42px;
  padding: 0 14px;
  border-radius: 12px;
  background: #fdf4f5;
  color: var(--pd-red-dark);
  font-weight: 700;
}

.pd-shell {
  padding: 18px 0 56px;
}

.pd-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 18px;
  font-size: 12px;
  color: #6b7280;
}

.pd-breadcrumb strong {
  color: #111827;
}

.pd-main {
  display: grid;
  grid-template-columns: minmax(0, 520px) minmax(340px, 420px);
  justify-content: center;
  gap: 28px;
  align-items: start;
}

.pd-gallery {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  gap: 14px;
}

.pd-gallery--single {
  grid-template-columns: minmax(0, 1fr);
}

.pd-gallery__thumbs {
  display: grid;
  gap: 10px;
  align-content: start;
}

.pd-gallery__thumb {
  padding: 0;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  overflow: hidden;
  transition: transform var(--pd-ui-ms, 220ms) ease, border-color var(--pd-ui-ms, 220ms) ease, box-shadow var(--pd-ui-ms, 220ms) ease;
}

.pd-gallery__thumb:hover {
  transform: translateY(-2px);
}

.pd-gallery__thumb.is-active {
  border-color: var(--pd-red);
  box-shadow: 0 0 0 1px rgba(197, 22, 45, 0.22);
  transform: translateY(-1px);
}

.pd-gallery__thumb img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
}

.pd-gallery__stage {
  position: relative;
  overflow: hidden;
  border: 1px solid #eee4e6;
  background: linear-gradient(180deg, #eef1f1 0%, #e7e2de 100%);
  aspect-ratio: 3 / 4;
}

.pd-gallery__stage img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.pd-gallery__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.pd-image-swipe-right-enter-active,
.pd-image-swipe-right-leave-active,
.pd-image-swipe-left-enter-active,
.pd-image-swipe-left-leave-active {
  transition: transform var(--pd-swipe-ms, 320ms) cubic-bezier(0.22, 1, 0.36, 1), opacity var(--pd-fade-ms, 260ms) ease;
}

.pd-image-swipe-right-enter-from {
  opacity: 0.18;
  transform: translateX(26px) scale(0.985);
}

.pd-image-swipe-right-leave-to {
  opacity: 0;
  transform: translateX(-24px) scale(0.985);
}

.pd-image-swipe-left-enter-from {
  opacity: 0.18;
  transform: translateX(-26px) scale(0.985);
}

.pd-image-swipe-left-leave-to {
  opacity: 0;
  transform: translateX(24px) scale(0.985);
}

.pd-gallery__arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid #d1d5db;
  background: rgba(255, 255, 255, 0.9);
  color: #111827;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform var(--pd-ui-ms, 220ms) ease, background-color var(--pd-ui-ms, 220ms) ease, box-shadow var(--pd-ui-ms, 220ms) ease;
}

.pd-gallery__arrow:hover {
  transform: translateY(-50%) scale(1.08);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 8px 20px rgba(17, 24, 39, 0.18);
}

.pd-gallery__arrow:active {
  transform: translateY(-50%) scale(0.95);
}

.pd-gallery__arrow--left { left: 10px; }

.pd-gallery__arrow--right { right: 10px; }

.pd-info {
  padding-top: 2px;
}

.pd-info__panel {
  display: grid;
  gap: 12px;
  padding: 4px 0;
}

.pd-info__topline {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.pd-info__topline h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.18;
}

.pd-stock {
  display: inline-flex;
  align-items: center;
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.pd-stock.is-red {
  background: #fee7eb;
  color: var(--pd-red-dark);
}

.pd-stock.is-green {
  background: #e8f8ef;
  color: #047857;
}

.pd-stock.is-dark {
  background: #fee7eb;
  color: var(--pd-red-dark);
}

.pd-sku {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.pd-price-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.pd-price-row strong {
  font-size: 44px;
  color: var(--pd-red-dark);
  line-height: 1;
}

.pd-price-row s {
  color: #9ca3af;
  font-size: 19px;
}

.pd-discount {
  display: inline-flex;
  padding: 5px 9px;
  border-radius: 999px;
  background: #3a0e14;
  color: white;
  font-size: 12px;
  font-weight: 700;
}

.pd-campaign-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 8px;
  padding: 8px 14px;
  background: linear-gradient(135deg, rgba(220, 38, 38, 0.08), rgba(185, 28, 28, 0.06));
  border: 1px solid rgba(220, 38, 38, 0.25);
  border-radius: 10px;
  font-size: 13px;
  color: #7f1d1d;
}
.pd-campaign-banner__badge {
  background: #dc2626;
  color: white;
  padding: 3px 10px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 12px;
}
.pd-campaign-banner__name {
  font-weight: 600;
}
.pd-campaign-banner__period {
  opacity: 0.75;
  font-size: 12px;
}

  .pd-voucher-applied-row {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    margin-top: 6px;
    padding: 8px 14px;
    background: rgba(255, 77, 79, 0.07);
    border: 1px solid rgba(255, 77, 79, 0.22);
    border-radius: 10px;
    font-size: 14px;
    color: rgba(17, 24, 39, 0.75);
  }
  .pd-after-voucher-price {
    font-size: 20px;
    font-weight: 700;
    color: #b42324;
  }
  .pd-saved-amount {
    font-size: 12.5px;
    color: #15803d;
    font-weight: 500;
    background: rgba(21, 128, 61, 0.09);
    padding: 3px 8px;
    border-radius: 6px;
  }

.pd-promo-box {
  padding: 14px 16px;
  border: 1px dashed #ef6a78;
  border-radius: 6px;
  background: #fff8f8;
}

.pd-promo-box__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--pd-red);
  font-size: 12px;
  text-transform: uppercase;
}

.pd-promo-box ul {
  margin: 10px 0 0;
  padding-left: 16px;
  color: #374151;
  font-size: 13px;
  line-height: 1.8;
}

.pd-promo-box__line {
  cursor: pointer;
}

.pd-promo-box__line:hover {
  color: #991b1b;
}

.pd-voucher-row,
.pd-option-row {
  margin-top: 0;
}

.pd-voucher-row__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.pd-label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: #374151;
}

.pd-voucher-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  min-width: 86px;
  padding: 0 11px;
  border: 1px solid #ef4444;
  border-radius: 4px;
  color: #b91c1c;
  background: #ffffff;
  font-size: 11px;
  font-weight: 700;
  clip-path: polygon(0 0, 100% 0, 100% 44%, 98.5% 50%, 100% 56%, 100% 100%, 0 100%, 0 56%, 1.5% 50%, 0 44%);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.pd-voucher-chip.is-selected {
  background: var(--pd-red);
  border-color: var(--pd-red);
  color: white;
}

.pd-selected-voucher-hint {
  display: block;
  margin-top: 6px;
  color: #059669;
  font-size: 11px;
  font-weight: 600;
}

.pd-colors,
.pd-sizes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pd-color {
  width: 28px;
  height: 28px;
  padding: 3px;
  border: 1px solid #d6ccd0;
  border-radius: 50%;
  background: white;
  transition: transform var(--pd-ui-ms, 220ms) ease, border-color var(--pd-ui-ms, 220ms) ease, box-shadow var(--pd-ui-ms, 220ms) ease;
}

.pd-color:hover {
  transform: translateY(-2px) scale(1.04);
}

.pd-color span {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  transition: transform var(--pd-ui-ms, 220ms) ease;
}

.pd-color.is-active {
  border-color: var(--pd-red);
  box-shadow: 0 0 0 1px var(--pd-red);
  animation: pd-color-pop var(--pd-ui-ms, 220ms) ease;
}

.pd-color.is-active span {
  transform: scale(0.9);
}

@keyframes pd-color-pop {
  0% {
    transform: scale(0.88);
  }
  60% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}

.pd-size-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pd-size-guide {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--pd-muted);
  font-size: 12px;
}

.pd-size {
  min-width: 40px;
  height: 38px;
  padding: 0 12px;
  border: 1px solid #dbd2d5;
  background: white;
  font-size: 12px;
}

.pd-size.is-active {
  border-color: var(--pd-red);
  background: #fff4f5;
  color: var(--pd-red);
}

.pd-buy-row {
  display: grid;
  grid-template-columns: 98px minmax(0, 1fr);
  gap: 12px;
  align-items: stretch;
  margin-top: 4px;
}

.pd-qty {
  display: grid;
  grid-template-columns: 30px 1fr 30px;
  align-items: center;
  border: 1px solid rgba(197, 22, 45, 0.34);
  border-radius: 4px;
  height: 46px;
  background: white;
}

.pd-qty button,
.pd-qty strong {
  display: grid;
  place-items: center;
}

.pd-qty button {
  border: 0;
  background: transparent;
  color: var(--pd-red-dark);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.pd-qty strong {
  font-size: 14px;
  color: #111827;
}

.pd-qty-input {
  width: 100%;
  height: 100%;
  border: 0;
  background: transparent;
  text-align: center;
  font-size: 14px;
  font-weight: 700;
  color: #111827;
  outline: none;
  -moz-appearance: textfield;
}
.pd-qty-input::-webkit-outer-spin-button,
.pd-qty-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.pd-cart-button,
.pd-buy-now {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.pd-cart-button {
  border: 1px solid rgba(197, 22, 45, 0.5);
  border-radius: 4px;
  background: white;
  color: var(--pd-red-dark);
}

.pd-buy-now {
  width: 100%;
  margin-top: 0;
  border-radius: 4px;
  background: linear-gradient(135deg, var(--pd-red), var(--pd-red-dark));
  color: white;
  box-shadow: var(--pd-shadow);
}

.pd-service-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding-top: 2px;
}

.pd-service-grid article {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12px;
  color: #5d5558;
}

.pd-tabs {
  margin-top: 28px;
}

.pd-tabs__head {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid #ddd2d6;
}

.pd-tabs__head button {
  padding: 10px 14px;
  border: 1px solid #ddd2d6;
  border-bottom: 0;
  background: #fcf7f8;
  font-size: 12px;
  color: #374151;
}

.pd-tabs__head button.active {
  background: white;
  color: var(--pd-red);
}

.pd-tabs__panel {
  padding: 20px 0 0;
}

.pd-tabs__panel h2 {
  margin: 0 0 10px;
  font-size: 22px;
}

.pd-tabs__panel h3 {
  margin: 18px 0 8px;
  font-size: 17px;
}

.pd-tabs__panel p {
  margin: 0;
  line-height: 1.75;
  color: #374151;
}

.pd-bullets {
  margin: 0 0 16px;
  padding-left: 18px;
  line-height: 1.8;
}

.pd-related {
  margin-top: 36px;
}

.pd-related h2 {
  margin: 0 0 18px;
  font-size: 22px;
}

.pd-related__grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.pd-related__card {
  position: relative;
  padding: 0;
  overflow: hidden;
  border: 1px solid rgba(143, 17, 33, 0.1);
  border-radius: 18px;
  background: white;
  text-align: left;
  box-shadow: var(--pd-shadow);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.pd-related__card:hover {
  transform: translateY(-3px);
  box-shadow: 0 22px 44px rgba(143, 17, 33, 0.12);
}

.pd-related__image {
  position: relative;
  background: linear-gradient(180deg, #f8f5f5 0%, #f0eded 100%);
  aspect-ratio: 0.9;
}

.pd-related__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 35%;
}

.pd-related__body {
  display: grid;
  gap: 6px;
  padding: 12px 12px 14px;
}

.pd-related__stock {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  width: fit-content;
  padding: 3px 8px;
  border-radius: 999px;
  background: #e8f8ef;
  color: #047857;
  font-size: 10px;
  font-weight: 700;
}

.pd-related__actions {
  position: absolute;
  left: 50%;
  bottom: 10px;
  transform: translate(-50%, 8px);
  display: flex;
  gap: 8px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.pd-related__card:hover .pd-related__actions {
  opacity: 1;
  transform: translate(-50%, 0);
  pointer-events: auto;
}

.pd-related__action {
  width: 34px;
  height: 34px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #111827;
}

.pd-quick-view {
  position: fixed;
  inset: 0;
  z-index: 70;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(17, 24, 39, 0.55);
}

.pd-quick-view__card {
  width: min(820px, 100%);
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 320px);
  gap: 16px;
  border-radius: 16px;
  background: white;
  padding: 16px;
  position: relative;
}

.pd-quick-view__close {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: white;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.pd-quick-view__image {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(180deg, #f8f5f5 0%, #f0eded 100%);
}

.pd-quick-view__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 35%;
}

.pd-quick-view__nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 34px;
  height: 34px;
  border-radius: 999px;
  border: 1px solid rgba(17, 24, 39, 0.22);
  background: rgba(255, 255, 255, 0.9);
  color: #111827;
  font-size: 22px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.pd-quick-view__nav--prev {
  left: 10px;
}

.pd-quick-view__nav--next {
  right: 10px;
}

.pd-quick-view__body {
  display: grid;
  align-content: start;
  gap: 10px;
}

.pd-quick-view__body h3 {
  margin: 0;
  font-size: 22px;
}

.pd-quick-view__body small {
  color: #6b7280;
}

.pd-quick-view__body strong {
  color: var(--pd-red-dark);
  font-size: 28px;
}

.pd-quick-view__btn {
  min-height: 40px;
  border: 1px solid rgba(197, 22, 45, 0.4);
  border-radius: 8px;
  padding: 0 14px;
  background: #fff6f7;
  color: var(--pd-red-dark);
  font-weight: 700;
}

.pd-quick-view__code {
  color: #6b7280;
  font-size: 13px;
}

.pd-quick-view__price {
  color: var(--pd-red-dark) !important;
  font-size: 32px !important;
}

.pd-quick-view__option-row {
  display: grid;
  gap: 8px;
}

.pd-quick-view__label {
  font-size: 13px;
  color: #374151;
}

.pd-quick-view__swatches {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pd-quick-view__swatch {
  width: 26px;
  height: 26px;
  border-radius: 999px;
  border: 2px solid #e5e7eb;
  cursor: pointer;
  background: transparent;
}

.pd-quick-view__swatch.is-active {
  border-color: var(--pd-red);
  box-shadow: 0 0 0 1px var(--pd-red);
}

.pd-quick-view__sizes {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pd-quick-view__size {
  min-width: 38px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #dbd2d5;
  border-radius: 4px;
  background: white;
  font-size: 12px;
  cursor: pointer;
}

.pd-quick-view__size.is-active {
  border-color: var(--pd-red);
  background: #fff4f5;
  color: var(--pd-red);
}

.pd-quick-view__promo {
  padding: 10px 12px;
  border: 1px dashed #ef6a78;
  border-radius: 6px;
  background: #fff8f8;
}

.pd-quick-view__promo-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--pd-red);
  font-size: 11px;
  text-transform: uppercase;
}

.pd-quick-view__promo ul {
  margin: 6px 0 0;
  padding-left: 14px;
  font-size: 12px;
  line-height: 1.8;
  color: #374151;
}

.pd-quick-view__buy-row {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  gap: 10px;
  align-items: stretch;
}

.pd-quick-view__qty {
  height: 40px;
  border: 1px solid rgba(197, 22, 45, 0.34);
  border-radius: 4px;
  background: white;
  display: grid;
  grid-template-columns: 28px 1fr 28px;
  align-items: center;
}

.pd-quick-view__qty button {
  border: 0;
  background: transparent;
  color: var(--pd-red-dark);
  font-size: 16px;
  cursor: pointer;
  display: grid;
  place-items: center;
}

.pd-quick-view__qty strong {
  text-align: center;
  font-size: 13px;
  color: #111827;
}

.pd-quick-view__cart-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  border: 1px solid rgba(197, 22, 45, 0.5);
  border-radius: 4px;
  background: white;
  color: var(--pd-red-dark);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.03em;
  cursor: pointer;
}

.pd-quick-view__detail-link {
  border: 0;
  background: transparent;
  padding: 0;
  color: #6b7280;
  text-decoration: none;
  cursor: pointer;
  font-size: 13px;
  text-align: left;
}

.pd-related__body small {
  color: #8d7d81;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.pd-related__body p {
  margin: 0;
  min-height: 38px;
  color: #2f2a2c;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.45;
}

.pd-related__body strong {
  display: block;
  color: var(--pd-red-dark);
  font-size: 16px;
}

.pd-related__body s {
  color: #a99ca0;
  font-size: 12px;
}

.pd-related__dots {
  display: flex;
  gap: 5px;
  margin-top: 2px;
}

.pd-related__dots span {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.pd-drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 150;
  display: flex;
  justify-content: flex-end;
  background: rgba(17, 24, 39, 0.32);
}

.pd-drawer {
  width: min(360px, 100%);
  height: 100%;
  padding: 18px;
  background: white;
  box-shadow: -20px 0 40px rgba(0, 0, 0, 0.12);
}

.pd-drawer__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.pd-drawer__head small {
  color: var(--pd-red);
  font-size: 11px;
  text-transform: uppercase;
}

.pd-drawer__head h3 {
  margin: 4px 0 0;
  font-size: 20px;
}

.pd-drawer__close {
  width: 36px;
  height: 36px;
  border: 1px solid var(--pd-line);
  background: white;
}

.pd-drawer__list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.pd-drawer__voucher {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px;
  border: 1px solid #f0dde1;
  background: #fff7f8;
}

.pd-drawer__label {
  display: block;
  color: var(--pd-red);
  font-size: 10px;
  text-transform: uppercase;
}

.pd-drawer__voucher strong {
  display: block;
  margin-top: 4px;
  color: var(--pd-red-dark);
  font-size: 24px;
}

.pd-drawer__voucher p,
.pd-drawer__voucher small {
  color: #4b5563;
}

.pd-drawer__voucher-info {
  flex: 1;
  min-width: 0;
}

.pd-drawer__amount {
  display: block;
  margin-top: 4px;
  color: var(--pd-red-dark);
  font-size: 22px;
}

.pd-drawer__min,
.pd-drawer__code {
  margin: 2px 0 0;
  color: #4b5563;
  font-size: 12px;
}

.pd-drawer__code b {
  color: var(--pd-red-dark);
}

.pd-drawer__voucher.is-selected {
  border-color: var(--pd-red);
  background: #fff1f2;
}

.pd-apply-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  background: var(--pd-red-dark);
  color: white;
  font-size: 12px;
  font-weight: 600;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
}

.pd-apply-btn.is-applied {
  background: #059669;
}

.pd-copy {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  background: var(--pd-red-dark);
  color: white;
  font-size: 12px;
}

.pd-drawer-enter-active,
.pd-drawer-leave-active {
  transition: opacity 0.22s ease;
}

.pd-drawer-enter-active .pd-drawer,
.pd-drawer-leave-active .pd-drawer {
  transition: transform 0.22s ease;
}

.pd-drawer-enter-from,
.pd-drawer-leave-to {
  opacity: 0;
}

.pd-drawer-enter-from .pd-drawer,
.pd-drawer-leave-to .pd-drawer {
  transform: translateX(100%);
}

@media (max-width: 1100px) {
  .pd-site-header__inner,
  .pd-shell {
    width: min(100% - 24px, 1260px);
  }

  .pd-topbar__inner {
    padding: 10px 12px;
  }

  .pd-menu {
    display: none;
  }

  .pd-hamburger {
    display: inline-flex;
  }

  .pd-mobile-menu {
    display: block;
  }

  .pd-main {
    grid-template-columns: 1fr;
  }

  .pd-service-grid,
  .pd-related__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .pd-site-header__inner,
  .pd-shell {
    width: min(100% - 18px, 1260px);
  }

  .pd-topbar__inner {
    padding: 10px 8px;
  }

  .pd-nav-shell {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .pd-actions {
    flex-wrap: wrap;
    justify-content: space-between;
  }

  .pd-search {
    width: 100%;
    min-width: 0;
    order: 3;
  }

  .pd-gallery {
    grid-template-columns: 1fr;
  }

  .pd-gallery__thumbs {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .pd-gallery__stage {
    min-height: 360px;
  }

  .pd-buy-row,
  .pd-service-grid,
  .pd-related__grid {
    grid-template-columns: 1fr;
  }

  .pd-dropdown-panel {
    display: none !important;
  }

  .pd-size-head,
  .pd-info__topline {
    align-items: flex-start;
    flex-direction: column;
  }

  .pd-tabs__head {
    flex-wrap: wrap;
  }

  .user-account-btn {
    flex: 1;
  }

  .pd-quick-view__card {
    grid-template-columns: 1fr;
  }
}
</style>


