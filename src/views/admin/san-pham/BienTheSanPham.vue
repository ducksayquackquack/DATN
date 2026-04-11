<script setup>
import { computed, onMounted, ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import { getAllSanPham, updateSanPham, updateSanPhamChiTietStatus } from "../../../services/sanPhamService"
import { getAllHoaDon, getHoaDonById } from "../../../services/hoaDonService"
import { resolveApiOrigin } from "../../../utils/apiOrigin"
import { getProductImageConfig } from "../../../utils/productImageOverrides"
import { getAdminStatusTone, normalizeAdminStatusLabel } from "../../../utils/adminStatus"
import logo from "../../../assets/img/logo/new logo.png?url"
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
const routeBase = computed(() => (route.path.startsWith("/employee/") ? "/employee" : "/admin"))
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

const folderImageCountRules = [
  { keywords: ["astronaut"], count: 2 },
  { keywords: ["embroidered", "fuzzy"], count: 5 },
  { keywords: ["windbreaker"], count: 3 },
  { keywords: ["leopard"], count: 1 },
  { keywords: ["longsleeve"], count: 3 },
  { keywords: ["tiger", "stripe"], count: 1 },
  { keywords: ["camo"], count: 2 },
  { keywords: ["zip", "boxy"], count: 2 },
  { keywords: ["zip", "silk"], count: 3 }
]

const folderColorPaletteRules = [
  { keywords: ["astronaut"], colors: ["Xanh", "Xám"] },
  { keywords: ["embroidered", "fuzzy"], colors: ["Đen", "Xanh lá", "Nâu", "Đỏ", "Trắng"] },
  { keywords: ["windbreaker"], colors: ["Đen", "Xanh dương", "Xanh lá"] },
  { keywords: ["leopard"], colors: ["Vàng"] },
  { keywords: ["longsleeve"], colors: ["Đen", "Đỏ", "Trắng"] },
  { keywords: ["tiger", "stripe"], colors: ["Trắng"] },
  { keywords: ["camo"], colors: ["Đen", "Trắng"] },
  { keywords: ["zip", "boxy"], colors: ["Xanh dương", "Trắng"] },
  { keywords: ["zip", "silk"], colors: ["Đen", "Xám", "Đỏ"] }
]

function normalizeDisplayColorName(value = "") {
  const raw = String(value || "").trim()
  const normalized = normalizeColorKey(raw)
  if (!normalized) return ""
  if (normalized === "xanh duong") return "Xanh dương"
  return raw
}

const trailingColorTokens = [
  "xanh navy",
  "xanh duong",
  "xanh la",
  "den",
  "do",
  "xanh",
  "xam",
  "trang",
  "nau",
  "be",
  "cam",
  "vang",
  "black",
  "red",
  "blue",
  "navy",
  "green",
  "gray",
  "grey",
  "white",
  "brown",
  "beige",
  "orange",
  "yellow"
]

function normalizeProductKey(value = "") {
  return String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
}

function getMappedFallbackByName(name = "") {
  const normalized = normalizeProductKey(name)
  if (!normalized) return ""

  const found = mappedFallbackByName.find((rule) =>
    rule.keywords.every((keyword) => normalized.includes(keyword))
  )
  return found?.image || ""
}

function normalizeSearchText(value = "") {
  return normalizeProductKey(value)
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()
}

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

function inferLoaiName(product = {}) {
  const fromData = String(product?.loai?.tenLoai || product?.danhMuc?.tenDanhMuc || "").trim()
  if (fromData) return fromData

  // Check variant-level loai
  const variants = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []
  for (const v of variants) {
    const variantLoai = String(v?.loai?.tenLoai || "").trim()
    if (variantLoai) return variantLoai
  }

  const n = normalizeSearchText(product?.tenSanPham || "")
  if (!n) return "—"
  if (n.includes("bomber")) return "Bomber"
  if (n.includes("hoodie")) return "Hoodie"
  if (n.includes("coach")) return "Coach"
  return "—"
}

function folderImageCountByName(name = "") {
  const normalized = normalizeSearchText(name)
  if (!normalized) return 0

  const found = folderImageCountRules.find((rule) =>
    rule.keywords.every((keyword) => normalized.includes(keyword))
  )
  return Number(found?.count || 0)
}

function folderColorPaletteByName(name = "") {
  const normalized = normalizeSearchText(name)
  if (!normalized) return []

  const found = folderColorPaletteRules.find((rule) =>
    rule.keywords.every((keyword) => normalized.includes(keyword))
  )

  return Array.isArray(found?.colors) ? found.colors : []
}

function hasTrailingColorName(name = "") {
  const normalized = normalizeSearchText(stripTrailingBrandToken(name))
  if (!normalized) return false
  return trailingColorTokens.some((token) => normalized === token || normalized.endsWith(` ${token}`))
}

function productSupportsColorGallery(product = {}) {
  const config = getProductImageConfig({
    id: product?.id,
    maSanPham: product?.maSanPham || product?.ma
  })

  const configuredColorImages = new Set(
    (Array.isArray(config?.colorImages) ? config.colorImages : [])
      .map((entry) => String(entry?.image || "").trim())
      .filter(Boolean)
  ).size

  const configuredGalleryImages = new Set(
    (Array.isArray(config?.images) ? config.images : [])
      .map((image) => String(image || "").trim())
      .filter(Boolean)
  ).size

  const folderImageCount = folderImageCountByName(product?.tenSanPham || "")
  return configuredColorImages > 1 || configuredGalleryImages > 1 || folderImageCount > 1
}

function fallbackIndexByName(name = "") {
  const normalized = normalizeSearchText(stripTrailingBrandToken(name))
  if (!normalized) return -1

  let hash = 0
  for (let i = 0; i < normalized.length; i += 1) {
    hash = ((hash << 5) - hash + normalized.charCodeAt(i)) | 0
  }
  return Math.abs(hash) % fallbackImages.length
}

// ── State ───────────────────────────────────────────────────────────
const allVariants = ref([])
const variantItems = ref([])
const loading = ref(false)
const viewMode = ref("products")

// Filters
const searchQuery = ref("")
const filterStatus = ref("")
const filterColorId = ref("")
const filterSizeId = ref("")
const priceMin = ref("")
const priceMax = ref("")
const showActiveOnly = ref(false)
const focusProductId = computed(() => {
  const value = Number(route.query?.productId)
  return Number.isFinite(value) && value > 0 ? value : 0
})

// Derived filter options
const colorOptions = computed(() => {
  const seen = new Set()
  const result = []
  const source = [...allVariants.value, ...variantItems.value]
  for (const v of source) {
    if (!Array.isArray(v.colors)) continue
    for (const c of v.colors) {
      const key = normalizeColorKey(c.name || "")
      if (!key || seen.has(key)) continue
      seen.add(key)
      result.push({ id: String(c.id), name: normalizeDisplayColorName(c.name), hex: c.hex })
    }
  }
  return result.sort((a, b) => a.name.localeCompare(b.name, "vi"))
})

const SIZE_ORDER = ["XS", "S", "M", "L", "XL", "XXL", "3XL", "4XL"]
const sizeOptions = computed(() => {
  const seen = new Set()
  const result = []
  const source = [...allVariants.value, ...variantItems.value]
  for (const v of source) {
    if (!Array.isArray(v.sizes)) continue
    for (const s of v.sizes) {
      if (!s || seen.has(s)) continue
      seen.add(s)
      result.push({ id: s, name: s })
    }
  }
  return result.sort((a, b) => {
    const ai = SIZE_ORDER.indexOf(a.name.toUpperCase())
    const bi = SIZE_ORDER.indexOf(b.name.toUpperCase())
    if (ai !== -1 && bi !== -1) return ai - bi
    if (ai !== -1) return -1
    if (bi !== -1) return 1
    return a.name.localeCompare(b.name)
  })
})

// ── Computed filtered list ─────────────────────────────────────────
const filteredVariants = computed(() => {
  let list = viewMode.value === "variants" ? variantItems.value : allVariants.value

  if (focusProductId.value > 0) {
    list = list.filter((v) =>
      Number(v.productId) === focusProductId.value ||
      (Array.isArray(v.productIds) && v.productIds.some((id) => Number(id) === focusProductId.value))
    )
  }

  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(
      (v) =>
        v.tenSanPham.toLowerCase().includes(q) ||
        v.maSanPham.toLowerCase().includes(q) ||
        (v.ma || "").toLowerCase().includes(q)
    )
  }

  if (filterStatus.value) list = list.filter((v) => v.trangThai === filterStatus.value)
  if (filterColorId.value) list = list.filter((v) => {
    const selectedColor = colorOptions.value.find(c => String(c.id) === filterColorId.value)
    if (!selectedColor) return false
    return Array.isArray(v.colors) && v.colors.some(c => normalizeColorKey(c.name) === normalizeColorKey(selectedColor.name))
  })
  if (filterSizeId.value) list = list.filter((v) =>
    Array.isArray(v.sizes) && v.sizes.includes(filterSizeId.value)
  )
  if (priceMin.value !== "") list = list.filter((v) => v.giaBan >= Number(priceMin.value))
  if (priceMax.value !== "") list = list.filter((v) => v.giaBan <= Number(priceMax.value))
  if (showActiveOnly.value) list = list.filter((v) => isActiveStatus(v.trangThai))

  return list
})

// ── Utils ────────────────────────────────────────────────────────────
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
  return normalized
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

function productHasColorsByVariants(variants = []) {
  const colors = new Set(
    (Array.isArray(variants) ? variants : [])
      .map((v) => normalizeColorKey(v?.mauSac?.tenMau || v?.mauSac?.tenMauSac || v?.mauSac?.name || ""))
      .filter(Boolean)
  )
  return colors.size >= 1
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

  const found = list.find((img) => {
    return tokens.some((token) => imageHasColorToken(img, token))
  })

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

  const codeNumber = extractProductCodeNumber(
    productLike?.maSanPham || productLike?.ma || productLike?.sku
  )
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

function colorHexByName(name = "") {
  if (/^#([0-9a-f]{3}|[0-9a-f]{6})$/i.test(String(name || "").trim())) return String(name).trim()
  const n = String(name || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .trim()
  if (n === "den" || n === "black") return "#111827"
  if (n === "trang" || n === "white") return "#e5dfd0"
  if (n === "do" || n === "red" || n === "ruou") return "#dc2626"
  if (n === "xanh navy" || n === "navy") return "#1e3a8a"
  if (n === "xanh la" || n === "xanh la cay") return "#4f6f52"
  if (n === "xanh" || n === "xanh duong" || n === "xanh lam" || n === "blue") return "#2f4f75"
  if (n === "xam" || n === "ghi" || n === "gray" || n === "grey") return "#8c95a3"
  if (n === "nau" || n === "brown") return "#7a4b2f"
  if (n === "hong" || n === "pink") return "#d684a1"
  if (n === "tim" || n === "purple") return "#7c3aed"
  if (n === "vang" || n === "yellow") return "#d4a017"
  if (n === "cam" || n === "orange") return "#e07b39"
  if (n === "kem" || n === "be" || n === "cream") return "#d4b896"
  // partial fallback
  if (n.includes("den") || n.includes("black")) return "#111827"
  if (n.includes("trang") || n.includes("white")) return "#e5dfd0"
  if (n.includes("navy")) return "#1e3a8a"
  if (n.includes("xanh la")) return "#4f6f52"
  if (n.includes("xanh")) return "#2f4f75"
  if (n.includes("xam") || n.includes("ghi") || n.includes("gray") || n.includes("grey")) return "#8c95a3"
  if (n.includes("nau")) return "#7a4b2f"
  if (n.includes("hong") || n.includes("pink")) return "#d684a1"
  if (n.includes("tim") || n.includes("purple")) return "#7c3aed"
  if (n.includes("vang") || n.includes("yellow")) return "#d4a017"
  if (n.includes("cam") || n.includes("orange")) return "#e07b39"
  if (n.includes("kem") || n.includes("cream")) return "#d4b896"
  if (/\bdo\b/.test(n) || n.includes("red") || n.includes("ruou")) return "#dc2626"
  return "#9ca3af"
}

function fallbackImageFor(id, code = "", name = "") {
  const normalizedCode = String(code || "").trim().toUpperCase()
  const allowCuratedCodeMap = /^ATID070-\d+$/i.test(normalizedCode) || /^SP\d+$/i.test(normalizedCode)

  if (mappedFallbackByCode[normalizedCode]) {
    return mappedFallbackByCode[normalizedCode]
  }

  if (allowCuratedCodeMap) {
    const codeDigits = String(normalizedCode).replace(/\D+/g, "")
    const curatedCodeNum = Number(codeDigits)
    if (Number.isFinite(curatedCodeNum) && curatedCodeNum > 0 && mappedFallbackByCodeNum[curatedCodeNum]) {
      return mappedFallbackByCodeNum[curatedCodeNum]
    }
  }

  const mappedByName = getMappedFallbackByName(name)
  if (mappedByName) return mappedByName

  const numericId = Number(id)
  if (Number.isFinite(numericId) && numericId > 0) {
    if (allowCuratedCodeMap && mappedFallbackByCodeNum[numericId]) return mappedFallbackByCodeNum[numericId]
    const nameIndex = fallbackIndexByName(name)
    if (nameIndex >= 0) return fallbackImages[nameIndex] || logo
    return fallbackImages[(numericId - 1) % fallbackImages.length]
  }

  const digits = String(code || "").replace(/\D+/g, "")
  const codeNum = Number(digits)
  if (Number.isFinite(codeNum) && codeNum > 0) {
    if (allowCuratedCodeMap && mappedFallbackByCodeNum[codeNum]) return mappedFallbackByCodeNum[codeNum]
    const nameIndex = fallbackIndexByName(name)
    if (nameIndex >= 0) return fallbackImages[nameIndex] || logo
    return fallbackImages[(codeNum - 1) % fallbackImages.length]
  }

  const nameIndex = fallbackIndexByName(name)
  if (nameIndex >= 0) return fallbackImages[nameIndex] || logo

  return fallbackImages[0] || logo
}

function pickProductImage(product, variant = null, colorId = null, colorName = "") {
  const productCode = String(product?.maSanPham || product?.ma || "").trim().toUpperCase();
  const imageConfig = getProductImageConfig({ id: product.id, maSanPham: product.maSanPham });

  // 1. Ưu tiên ảnh trực tiếp từ biến thể
  const variantDirect =
    variant?.anh ||
    variant?.anhChinh ||
    variant?.hinhAnh ||
    (Array.isArray(variant?.listAnh) ? variant.listAnh[0] : "") ||
    (Array.isArray(variant?.images) ? variant.images[0] : "") ||
    "";
  if (variantDirect) return toImageUrl(variantDirect);

  // 2. Ảnh theo colorId/colorName từ cấu hình màu của sản phẩm
  if (colorId || colorName) {
    // colorImages cấu hình riêng
    const colorImage = (Array.isArray(imageConfig.colorImages) ? imageConfig.colorImages : [])
      .find((entry) => Number(entry.colorId) === Number(colorId) || (colorName && entry.colorName === colorName));
    if (colorImage?.image) return toImageUrl(colorImage.image);

    // Tìm trong images có token màu
    const colorTokenImage = colorName ? pickImageFromListByColor(imageConfig.images, colorName) : "";
    if (colorTokenImage) return toImageUrl(colorTokenImage);
  }

  // 3. Ảnh tĩnh mapping theo màu (nếu có)
  const staticColor = pickStaticColorImage(product.id, colorName, product);
  if (staticColor) return staticColor;

  // 4. Ảnh đầu tiên của sản phẩm (nếu có)
  if (imageConfig.images?.length) return toImageUrl(imageConfig.images[0]);
  const direct =
    product.anh ||
    product.anhChinh ||
    product.hinhAnh ||
    (Array.isArray(product.listAnh) ? product.listAnh[0] : "") ||
    (Array.isArray(product.images) ? product.images[0] : "") ||
    "";
  if (direct) return toImageUrl(direct);

  // 5. Fallback cuối cùng
  return fallbackImageFor(product.id, product.maSanPham, product.tenSanPham);
}

const formatGia = (n) => Number(n || 0).toLocaleString("vi-VN") + "đ"

function clearFilters() {
  searchQuery.value = ""
  filterStatus.value = ""
  filterColorId.value = ""
  filterSizeId.value = ""
  priceMin.value = ""
  priceMax.value = ""
}

function isActiveStatus(status) {
  return isEntityActive({ trangThai: status }, true)
}

async function toggleGroupStatus(variantRow) {
  const variantIds = [...new Set(
    (Array.isArray(variantRow?.variantIds) ? variantRow.variantIds : [])
      .map((id) => Number(id))
      .filter((id) => Number.isFinite(id) && id > 0)
  )]

  if (!variantIds.length) {
    window.toast?.warning?.("Sản phẩm này không có biến thể để cập nhật trạng thái")
    return
  }

  const nextStatus = isActiveStatus(variantRow.trangThai) ? "Ngừng hoạt động" : "Hoạt động"
  try {
    const productIds = [...new Set(
      (Array.isArray(variantRow?.productIds) ? variantRow.productIds : [variantRow?.productId])
        .map((id) => Number(id))
        .filter((id) => Number.isFinite(id) && id > 0)
    )]

    const productPayloads = [
      { trangThai: nextStatus },
      { status: nextStatus },
      { isActive: nextStatus === "Hoạt động" },
      { active: nextStatus === "Hoạt động" }
    ]

    for (const productId of productIds) {
      let updatedProduct = false
      for (const payload of productPayloads) {
        try {
          await updateSanPham(productId, payload)
          updatedProduct = true
          break
        } catch {
          // Try next payload shape.
        }
      }
      if (!updatedProduct) {
        console.warn("Không cập nhật được trạng thái sản phẩm", productId)
      }
    }

    const results = await Promise.allSettled(
      variantIds.map((variantId) => updateSanPhamChiTietStatus(variantId, nextStatus))
    )
    const successCount = results.filter((result) => result.status === "fulfilled").length
    if (!successCount) throw new Error("Không có biến thể nào cập nhật thành công")

    await loadData()
    window.toast?.success?.(nextStatus === "Hoạt động" ? "Đã bật biến thể" : "Đã tắt biến thể")
  } catch (error) {
    console.error("Không cập nhật được trạng thái biến thể:", error)
    window.toast?.error?.("Không thể cập nhật trạng thái biến thể")
  }
}

function clearProductFocus() {
  const nextQuery = { ...route.query }
  delete nextQuery.productId
  delete nextQuery.from
  router.replace({
    path: `${routeBase.value}/san-pham/bien-the`,
    query: nextQuery
  })
}

function productShouldHideColorsByName(name = "") {
  const n = stripTrailingBrandToken(name)
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

function buildVariantCode(productCode = "", variant = {}) {
  const existing = String(variant?.ma || "").trim()
  if (existing) return existing
  const colorId = Number(variant?.mauSac?.id || 0)
  const sizeId = Number(variant?.kichThuoc?.id || 0)
  if (!productCode || !colorId || !sizeId) return "—"
  return `${productCode}-MS${String(colorId).padStart(2, "0")}-KT${String(sizeId).padStart(2, "0")}`
}

function aggregateVariantStatus(variants = [], fallbackStatus = "Hoạt động", product = null) {
  if (product && !isEntityActive(product, true)) {
    return normalizeAdminStatusLabel("Ngừng hoạt động")
  }

  const rows = Array.isArray(variants) ? variants : []
  if (!rows.length) return normalizeAdminStatusLabel(fallbackStatus)
  const hasActive = rows.some((variant) => isEntityActive(variant, true))
  return normalizeAdminStatusLabel(hasActive ? "Hoạt động" : "Ngừng hoạt động")
}

function toVariantList(product = {}) {
  if (Array.isArray(product?.sanPhamChiTiets)) return product.sanPhamChiTiets
  if (Array.isArray(product?.variants)) return product.variants
  if (Array.isArray(product?.chiTiets)) return product.chiTiets
  if (Array.isArray(product?.danhSachBienThe)) return product.danhSachBienThe
  return []
}

function hasCoreOrFlameToken(name = "") {
  return /\b(core|flame)\b/i.test(String(name || ""))
}

function variantStockValue(variant = {}) {
  return Number(variant?.soLuong ?? variant?.soLuongTon ?? variant?.tonKho ?? variant?.ton ?? 0)
}

function variantAvailableStockForAdmin(variant = {}, soldBySpct = new Map()) {
  const baseStock = variantStockValue(variant)
  const spctId = Number(variant?.id || variant?.spctId || variant?.sanPhamChiTietId || 0)
  if (!Number.isFinite(spctId) || spctId <= 0) return baseStock
  const soldQty = Number(soldBySpct.get(spctId) || 0)
  return Math.max(0, baseStock - soldQty)
}

function variantIdentityKeyForAdmin(variant = {}) {
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

function mergeProductsForAdmin(products = []) {
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
    const productIds = []

    for (const product of ranked) {
      const productId = Number(product?.id)
      if (Number.isFinite(productId) && productId > 0 && !productIds.includes(productId)) {
        productIds.push(productId)
      }

      for (const variant of toVariantList(product)) {
        const identityKey = variantIdentityKeyForAdmin(variant) || `id:${Number(variant?.id || 0)}`
        if (!variantByIdentity.has(identityKey)) {
          variantByIdentity.set(identityKey, variant)
        }
      }
    }

    merged.push({
      ...canonicalProduct,
      sanPhamChiTiets: [...variantByIdentity.values()],
      productIds
    })
  }

  return merged
}

function getStaticProductsForAdmin() {
  return [
    {
      id: 12,
      tenSanPham: "Bomber Astronaut DirtyWave",
      maSanPham: "ATID070-12",
      anh: img12,
      danhMuc: { tenDanhMuc: "Bomber" },
      giaBan: 549000,
      sanPhamChiTiets: [
        { id: 1201, mauSac: { id: 1, tenMau: "Xanh" }, kichThuoc: { id: 2, tenKichThuoc: "M" }, giaBan: 549000 },
        { id: 1202, mauSac: { id: 1, tenMau: "Xanh" }, kichThuoc: { id: 3, tenKichThuoc: "L" }, giaBan: 549000 },
        { id: 1203, mauSac: { id: 1, tenMau: "Xanh" }, kichThuoc: { id: 4, tenKichThuoc: "XL" }, giaBan: 549000 },
        { id: 1204, mauSac: { id: 2, tenMau: "Xám" }, kichThuoc: { id: 2, tenKichThuoc: "M" }, giaBan: 549000 },
        { id: 1205, mauSac: { id: 2, tenMau: "Xám" }, kichThuoc: { id: 3, tenKichThuoc: "L" }, giaBan: 549000 },
        { id: 1206, mauSac: { id: 2, tenMau: "Xám" }, kichThuoc: { id: 4, tenKichThuoc: "XL" }, giaBan: 549000 }
      ]
    },
    {
      id: 13,
      tenSanPham: "Bomber Embroidered Fuzzy DirtyWave",
      maSanPham: "ATID070-13",
      anh: img13,
      danhMuc: { tenDanhMuc: "Bomber" },
      giaBan: 599000,
      sanPhamChiTiets: [
        { id: 1301, mauSac: { id: 3, tenMau: "Đen" }, kichThuoc: { id: 1, tenKichThuoc: "S" }, giaBan: 599000 },
        { id: 1305, mauSac: { id: 4, tenMau: "Xanh lá" }, kichThuoc: { id: 1, tenKichThuoc: "S" }, giaBan: 599000 },
        { id: 1309, mauSac: { id: 5, tenMau: "Nâu" }, kichThuoc: { id: 1, tenKichThuoc: "S" }, giaBan: 599000 },
        { id: 1313, mauSac: { id: 6, tenMau: "Đỏ" }, kichThuoc: { id: 1, tenKichThuoc: "S" }, giaBan: 599000 },
        { id: 1317, mauSac: { id: 7, tenMau: "Trắng" }, kichThuoc: { id: 1, tenKichThuoc: "S" }, giaBan: 599000 }
      ]
    },
    {
      id: 14,
      tenSanPham: "Bomber Windbreaker DirtyWave",
      maSanPham: "ATID070-14",
      anh: img14,
      danhMuc: { tenDanhMuc: "Bomber" },
      giaBan: 479000,
      sanPhamChiTiets: [
        { id: 1401, mauSac: { id: 2, tenMau: "Đen" }, kichThuoc: { id: 1, tenKichThuoc: "S" }, giaBan: 479000 },
        { id: 1405, mauSac: { id: 5, tenMau: "Xanh dương" }, kichThuoc: { id: 1, tenKichThuoc: "S" }, giaBan: 479000 },
        { id: 1409, mauSac: { id: 8, tenMau: "Xanh lá" }, kichThuoc: { id: 1, tenKichThuoc: "S" }, giaBan: 479000 }
      ]
    },
    {
      id: 15,
      tenSanPham: "Coach Leopard DirtyWave",
      maSanPham: "ATID070-15",
      anh: img15,
      danhMuc: { tenDanhMuc: "Coach" },
      giaBan: 849000,
      sanPhamChiTiets: [
        { id: 1501, mauSac: { id: 5, tenMau: "Vàng" }, kichThuoc: { id: 2, tenKichThuoc: "M" }, giaBan: 849000 }
      ]
    },
    {
      id: 16,
      tenSanPham: "Coach Longsleeve DirtyWave",
      maSanPham: "ATID070-16",
      anh: img16,
      danhMuc: { tenDanhMuc: "Coach" },
      giaBan: 529000,
      sanPhamChiTiets: [
        { id: 1601, mauSac: { id: 1, tenMau: "Đen" }, kichThuoc: { id: 1, tenKichThuoc: "S" }, giaBan: 529000 },
        { id: 1605, mauSac: { id: 2, tenMau: "Đỏ" }, kichThuoc: { id: 1, tenKichThuoc: "S" }, giaBan: 529000 },
        { id: 1609, mauSac: { id: 7, tenMau: "Trắng" }, kichThuoc: { id: 1, tenKichThuoc: "S" }, giaBan: 529000 }
      ]
    },
    {
      id: 17,
      tenSanPham: "Coach Tiger Stripe DirtyWave",
      maSanPham: "ATID070-17",
      anh: img17,
      danhMuc: { tenDanhMuc: "Coach" },
      giaBan: 779000,
      sanPhamChiTiets: [
        { id: 1701, mauSac: { id: 3, tenMau: "Trắng" }, kichThuoc: { id: 2, tenKichThuoc: "M" }, giaBan: 779000 }
      ]
    },
    {
      id: 18,
      tenSanPham: "Hoodie Camo DirtyWave",
      maSanPham: "ATID070-18",
      anh: img18,
      danhMuc: { tenDanhMuc: "Hoodie" },
      giaBan: 459000,
      sanPhamChiTiets: [
        { id: 1801, mauSac: { id: 10, tenMau: "Đen" }, kichThuoc: { id: 1, tenKichThuoc: "S" }, giaBan: 459000 },
        { id: 1805, mauSac: { id: 11, tenMau: "Trắng" }, kichThuoc: { id: 1, tenKichThuoc: "S" }, giaBan: 459000 }
      ]
    },
    {
      id: 19,
      tenSanPham: "Hoodie Zip Boxy DirtyWave",
      maSanPham: "ATID070-19",
      anh: img19,
      danhMuc: { tenDanhMuc: "Hoodie" },
      giaBan: 519000,
      sanPhamChiTiets: [
        { id: 1901, mauSac: { id: 2, tenMau: "Xanh dương" }, kichThuoc: { id: 2, tenKichThuoc: "M" }, giaBan: 519000 },
        { id: 1904, mauSac: { id: 15, tenMau: "Trắng" }, kichThuoc: { id: 2, tenKichThuoc: "M" }, giaBan: 519000 }
      ]
    },
    {
      id: 20,
      tenSanPham: "Hoodie Zip Silk DirtyWave",
      maSanPham: "ATID070-20",
      anh: img20,
      danhMuc: { tenDanhMuc: "Hoodie" },
      giaBan: 679000,
      sanPhamChiTiets: [
        { id: 2001, mauSac: { id: 12, tenMau: "Đen" }, kichThuoc: { id: 2, tenKichThuoc: "M" }, giaBan: 679000 },
        { id: 2004, mauSac: { id: 13, tenMau: "Xám" }, kichThuoc: { id: 2, tenKichThuoc: "M" }, giaBan: 679000 },
        { id: 2007, mauSac: { id: 14, tenMau: "Đỏ" }, kichThuoc: { id: 2, tenKichThuoc: "M" }, giaBan: 679000 }
      ]
    }
  ]
}

// ── Load data ────────────────────────────────────────────────────────
async function loadData() {
  loading.value = true
  try {
    const [res, soldBySpct] = await Promise.all([getAllSanPham(), loadSoldQtyBySpct()])
    const backendProducts = toList(res?.data)
    const dedupedProducts = mergeProductsForAdmin(backendProducts)

    const productRows = []
    const variantRows = []
    for (const p of dedupedProducts) {
      const variants = Array.isArray(p.sanPhamChiTiets) ? p.sanPhamChiTiets : []
      const folderPalette = folderColorPaletteByName(p.tenSanPham)
      const productIds = Array.isArray(p.productIds) && p.productIds.length
        ? p.productIds
        : (Number.isFinite(Number(p.id)) && Number(p.id) > 0 ? [Number(p.id)] : [])
      const primaryProductId = productIds[0] || Number(p.id) || 0
      const hasColor =
        productHasColorsByVariants(variants) || folderPalette.length >= 1
      const productCode = String(p.maSanPham || p.ma || (Number.isFinite(Number(primaryProductId)) && Number(primaryProductId) > 0 ? `SP${String(primaryProductId).padStart(3, "0")}` : "")).trim()

      if (variants.length) {
        const colorMap = new Map()
        const sizeSet = new Set()
        const variantIds = []
        let soLuong = 0
        let minGiaBan = Number.POSITIVE_INFINITY

        for (const variant of variants) {
          const variantId = Number(variant?.id || 0)
          if (Number.isFinite(variantId) && variantId > 0) {
            variantIds.push(variantId)
          }

          const mauTen = normalizeDisplayColorName(
            variant?.mauSac?.tenMau || variant?.mauSac?.tenMauSac || variant?.mauSac?.name
          )
          const normalizedColorId = Number(variant?.mauSac?.id || 0)
          const mauSacId = Number.isFinite(normalizedColorId) && normalizedColorId > 0
            ? normalizedColorId
            : (folderPalette.indexOf(mauTen) + 1 || 1)
          if (mauTen) {
            const colorKey = normalizeColorKey(mauTen)
            if (!colorMap.has(colorKey)) {
              colorMap.set(colorKey, { id: mauSacId, name: mauTen, hex: colorHexByName(mauTen) })
            }
          }

          const sizeName = String(variant?.kichThuoc?.tenKichThuoc || variant?.kichThuoc?.name || "").trim()
          if (sizeName) sizeSet.add(sizeName)

          const stock = variantAvailableStockForAdmin(variant, soldBySpct)
          if (Number.isFinite(stock) && stock > 0) soLuong += stock

          const price = Number(variant?.giaBan || p?.giaBan || 0)
          if (Number.isFinite(price) && price > 0 && price < minGiaBan) {
            minGiaBan = price
          }

          const variantActive = isEntityActive(p, true) && isEntityActive(variant, true)
          const variantStatus = variantActive ? "Hoạt động" : "Ngừng hoạt động"

          variantRows.push({
            id: variantId || primaryProductId,
            rowKey: `variant-${primaryProductId}-${variantId || sizeName || mauTen || variantRows.length}`,
            variantId,
            variantIds: Number.isFinite(variantId) && variantId > 0 ? [variantId] : [],
            productId: primaryProductId || p.id,
            productIds,
            hasColors: Boolean(mauTen),
            maSanPham: productCode,
            tenSanPham: p.tenSanPham || "",
            image: pickProductImage(p, variant, mauSacId, mauTen),
            colors: mauTen ? [{ id: mauSacId || 0, name: mauTen, hex: colorHexByName(mauTen) }] : [],
            sizes: sizeName ? [sizeName] : [],
            loai: inferLoaiName(p),
            giaBan: Number(variant?.giaBan || p.giaBan || 0),
            soLuong: stock,
            trangThai: normalizeAdminStatusLabel(variantStatus),
            statusTone: getAdminStatusTone(variantStatus),
            mauSacId,
            mauSac: mauTen || "—",
            mauSacHex: colorHexByName(mauTen),
            kichThuoc: sizeName || "—",
            kichThuocId: sizeName || null,
            ma: buildVariantCode(productCode, variant)
          })
        }

        const colors = [...colorMap.values()]
        const sizes = [...sizeSet]
        const sampleVariant = variants.find((variant) => Number(variant?.id || 0) > 0) || variants[0]
        const rawStatus = aggregateVariantStatus(variants, p.trangThai || "Hoạt động", p)
        const normalizedStatus = normalizeAdminStatusLabel(rawStatus)

        productRows.push({
          id: primaryProductId || p.id,
          rowKey: `product-${primaryProductId || p.id}`,
          variantId: Number(sampleVariant?.id || 0),
          variantIds,
          productId: primaryProductId || p.id,
          productIds,
          hasColors: Boolean(colors.length),
          maSanPham: productCode,
          tenSanPham: p.tenSanPham || "",
          image: pickProductImage(p, sampleVariant, colors[0]?.id || null, colors[0]?.name || ""),
          colors,
          sizes,
          loai: inferLoaiName(p),
          giaBan: Number.isFinite(minGiaBan) ? minGiaBan : Number(p.giaBan || 0),
          soLuong,
          trangThai: normalizedStatus,
          statusTone: getAdminStatusTone(rawStatus),
          mauSacId: colors[0]?.id || null,
          mauSac: colors.map((c) => c.name).join(", ") || "—",
          mauSacHex: colors[0]?.hex || "#9ca3af",
          kichThuoc: sizes.join(", "),
          kichThuocId: sizes[0] || null,
          ma: buildVariantCode(productCode, sampleVariant),
        })
      } else {
        const productStatus = isEntityActive(p, true) ? "Hoạt động" : "Ngừng hoạt động"
        const normalizedStatus = normalizeAdminStatusLabel(productStatus)
        productRows.push({
          id: primaryProductId || p.id,
          rowKey: `product-${primaryProductId || p.id}`,
          variantId: 0,
          variantIds: [],
          productId: primaryProductId || p.id,
          productIds,
          hasColors: hasColor,
          maSanPham: productCode,
          tenSanPham: p.tenSanPham || "",
          image: pickProductImage(p),
          colors: [],
          sizes: [],
          loai: inferLoaiName(p),
          giaBan: Number(p.giaBan || 0),
          soLuong: 0,
          trangThai: normalizedStatus,
          statusTone: getAdminStatusTone(p.trangThai || normalizedStatus),
          mauSacId: null,
          mauSac: "—",
          mauSacHex: "#9ca3af",
          kichThuoc: "",
          kichThuocId: null,
          ma: productCode,
        })
      }
    }

    allVariants.value = productRows
    variantItems.value = variantRows
  } catch (err) {
    console.error("Lỗi tải biến thể:", err)
    window.toast?.error("Không tải được dữ liệu biến thể")
  } finally {
    loading.value = false
  }
}

function goEditProduct(productLike, colorId) {
  const isObjectInput = productLike && typeof productLike === "object"
  const productId = Number(isObjectInput ? (productLike?.productId || productLike?.id) : productLike)
  if (!Number.isFinite(productId) || productId <= 0) return

  const productIds = isObjectInput
    ? [...new Set(
      (Array.isArray(productLike?.productIds) ? productLike.productIds : [])
        .map((item) => Number(item))
        .filter((item) => Number.isFinite(item) && item > 0)
    )]
    : []

  const resolvedColorId = Number(
    colorId || (isObjectInput ? (productLike?.mauSacId || productLike?.colorId || 0) : 0)
  )

  const query = {
    from: "bien-the",
    hasColors: isObjectInput ? (productLike?.hasColors ? "1" : "0") : "1",
    ...(productIds.length > 1 ? { productIds: productIds.join(",") } : {}),
    ...(resolvedColorId > 0 ? { colorId: String(resolvedColorId) } : {})
  }

  router.push({
    path: `${routeBase.value}/san-pham/form/${productId}`,
    query
  })
}

function goCreateProduct() {
  router.push({
    path: `${routeBase.value}/san-pham/form`,
    query: { from: "bien-the" }
  })
}

onMounted(loadData)
</script>

<template>
  <div class="card bts-page">
    <!-- ── Header ──────────────────────────────────────────────────── -->
    <div class="head">
      <div>
        <h1>Sản phẩm</h1>
        <small class="muted">
          {{ filteredVariants.length }} / {{ viewMode === 'variants' ? variantItems.length : allVariants.length }}
          {{ viewMode === 'variants' ? 'biến thể' : 'sản phẩm' }}
        </small>
      </div>
      <div class="header-actions">
        <button class="btn with-icon" @click="loadData" :disabled="loading">
          <span class="material-icons-outlined btn-icon">autorenew</span>
          {{ loading ? "Đang tải..." : "Làm mới" }}
        </button>
        <button class="btn primary with-icon" @click="goCreateProduct">
          <span class="material-icons-outlined btn-icon">add</span>
          Thêm sản phẩm
        </button>
      </div>
    </div>

    <!-- ── Filter bar ──────────────────────────────────────────────── -->
    <div class="body">
      <div class="filter-bar">
        <div class="filter-field view-mode-field">
          <button
            type="button"
            class="btn btn-clear"
            :class="{ active: viewMode === 'products' }"
            @click="viewMode = 'products'"
          >Danh sách sản phẩm</button>
          <button
            type="button"
            class="btn btn-clear"
            :class="{ active: viewMode === 'variants' }"
            @click="viewMode = 'variants'"
          >Danh sách biến thể</button>
        </div>


        <!-- Search -->
        <div class="filter-field search-field">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Tìm tên SP, mã SP, mã biến thể..."
            class="filter-input"
          />
        </div>

        <!-- Status -->
        <div class="filter-field">
          <select v-model="filterStatus" class="filter-input">
            <option value="">Tất cả trạng thái</option>
            <option value="Hoạt động">Hoạt động</option>
            <option value="Ngừng hoạt động">Ngừng hoạt động</option>
          </select>
        </div>

        <!-- Size -->
        <div class="filter-field">
          <select v-model="filterSizeId" class="filter-input">
            <option value="">Tất cả kích cỡ</option>
            <option v-for="s in sizeOptions" :key="s.name" :value="s.name">{{ s.name }}</option>
          </select>
        </div>

        <!-- Color -->
        <div class="filter-field">
          <select v-model="filterColorId" class="filter-input">
            <option value="">Tất cả màu sắc</option>
            <option v-for="c in colorOptions" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
          </select>
        </div>

        <!-- Price range -->
        <div class="filter-field price-range">
          <input
            v-model.number="priceMin"
            type="number"
            placeholder="Giá từ"
            min="0"
            class="filter-input price-input"
          />
          <span class="price-sep">—</span>
          <input
            v-model.number="priceMax"
            type="number"
            placeholder="Đến"
            min="0"
            class="filter-input price-input"
          />
        </div>

        <!-- Clear -->
        <button class="btn btn-clear" @click="clearFilters">Xóa lọc</button>
      </div>

      <div v-if="focusProductId > 0" class="focus-bridge-note">
        Đang lọc theo sản phẩm ID #{{ focusProductId }} từ trang Sản phẩm.
        <button type="button" class="btn btn-clear" @click="clearProductFocus">Bỏ lọc sản phẩm</button>
      </div>

      <!-- ── Table ───────────────────────────────────────────────────── -->
      <div class="table-wrap">
        <div v-if="loading" class="loading-state">Đang tải dữ liệu...</div>

        <template v-else>
          <table class="bts-table" v-if="filteredVariants.length">
            <thead>
              <tr>
                <th class="col-stt">STT</th>
                <th class="col-img">Ảnh</th>
                <th>Tên sản phẩm</th>
                <th>Mã SP</th>
                <th>Màu sắc</th>
                <th>Kích cỡ</th>
                <th>Loại</th>
                <th class="col-num">Tồn kho</th>
                <th class="col-num">Giá bán</th>
                <th>Trạng thái</th>
                <th class="col-act">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(v, idx) in filteredVariants" :key="v.rowKey || v.id">
                <td class="col-stt muted">{{ idx + 1 }}</td>
                <td class="col-img">
                  <div class="product-thumb">
                    <img v-if="v.image" :src="v.image" :alt="v.tenSanPham" />
                    <span v-else class="thumb-placeholder">?</span>
                  </div>
                </td>
                <td>
                  <button class="link-btn" @click="goEditProduct(v)">
                    {{ v.tenSanPham }}
                  </button>
                </td>
                <td><span class="code-badge">{{ v.maSanPham }}</span></td>
                <td>
                  <div v-if="v.hasColors && v.colors && v.colors.length" class="color-chips-row">
                    <span v-for="c in v.colors" :key="c.id" class="color-chip-mini" :title="c.name">
                      <span class="cdot" :style="{ background: c.hex }"></span>
                      {{ c.name }}
                    </span>
                  </div>
                  <span v-else class="muted">—</span>
                </td>
                <td>
                  <div class="size-pills-row">
                    <span v-for="s in v.sizes" :key="s" class="size-pill">{{ s }}</span>
                    <span v-if="!v.sizes || !v.sizes.length" class="muted">—</span>
                  </div>
                </td>
                <td class="muted">{{ v.loai }}</td>
                <td class="col-num">
                  <span :class="['stock-badge', v.soLuong === 0 ? 'out' : v.soLuong < 5 ? 'low' : 'ok']">{{ v.soLuong }}</span>
                </td>
                <td class="col-num price">{{ formatGia(v.giaBan) }}</td>
                <td>
                  <span class="status-pill" :class="`tone-${v.statusTone}`">{{ v.trangThai }}</span>
                </td>
                <td class="col-act">
                  <div class="action-stack">
                    <button class="btn btn-small btn-edit btn-eye" @click="goEditProduct(v)" title="Xem chi tiết sản phẩm">
                      <span class="material-icons-outlined">visibility</span>
                    </button>
                    <button
                      v-if="Array.isArray(v.variantIds) && v.variantIds.length"
                      type="button"
                      class="variant-switch"
                      :class="{ active: isActiveStatus(v.trangThai) }"
                      :title="isActiveStatus(v.trangThai) ? 'Đang bật - bấm để tắt' : 'Đang tắt - bấm để bật'"
                      @click="toggleGroupStatus(v)"
                    >
                      <span class="variant-switch-thumb"></span>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-else class="empty-state">
            <p>Không có biến thể nào phù hợp với bộ lọc.</p>
            <button class="btn" @click="clearFilters">Xóa bộ lọc</button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── Layout ─────────────────────────────────────────────────────────── */
.bts-page .head {
  flex-wrap: wrap;
  gap: 12px;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.with-icon {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-icon {
  font-size: 16px;
  line-height: 1;
  color: currentColor;
}

/* ── Filter bar ──────────────────────────────────────────────────────── */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  padding: 14px 0 10px;
}

.filter-field {
  display: flex;
  align-items: center;
  gap: 4px;
}

.view-mode-field {
  gap: 8px;
  padding: 4px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
}

.view-mode-field .btn-clear {
  border-radius: 10px;
  border: 1px solid transparent;
  min-width: 160px;
  justify-content: center;
  font-weight: 700;
}

.view-mode-field .btn-clear.active {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  border-color: #b91c1c;
  color: #fff;
}

.active-only-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
}

.active-only-toggle input {
  width: 16px;
  height: 16px;
  accent-color: #dc2626;
}

.search-field {
  flex: 1 1 220px;
}

.filter-input {
  padding: 9px 34px 9px 12px;
  border: 1px solid #d8dee9;
  border-radius: 10px;
  font-size: 14px;
  background-color: #fff;
  width: 100%;
  box-sizing: border-box;
}

.filter-input:focus {
  outline: none;
  border-color: #c5162d;
}

.price-range {
  gap: 6px;
}

.price-input {
  width: 110px !important;
}

.price-sep {
  color: #94a3b8;
  font-size: 12px;
}

.btn-clear {
  white-space: nowrap;
}

.focus-bridge-note {
  margin: 6px 0 12px;
  padding: 10px 12px;
  border: 1px solid #fde68a;
  border-radius: 10px;
  background: #fffbeb;
  color: #92400e;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

/* ── Color chip filter ────────────────────────────────────────────────── */
.color-filter-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.chip-label {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  margin-right: 2px;
}

.color-filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
  color: #374151;
  transition: border-color 0.15s, background 0.15s;
}

.color-filter-chip:hover {
  border-color: #c5162d;
}

.color-filter-chip.active {
  border-color: #c5162d;
  background: #fff1f2;
  color: #c5162d;
  font-weight: 600;
}

/* ── Table ───────────────────────────────────────────────────────────── */
.table-wrap {
  overflow-x: auto;
  margin-top: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

.bts-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 860px;
}

.bts-table thead tr {
  background: linear-gradient(90deg, #7c1021 0%, #a9162d 35%, #cf243a 68%, #e64555 100%);
}

.bts-table th {
  padding: 10px 12px;
  background: transparent;
  font-size: 12px;
  font-weight: 600;
  color: #ffffff;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  text-align: left;
  border-bottom: 2px solid rgba(93, 14, 28, 0.5);
}

.bts-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 14px;
  vertical-align: middle;
}

.bts-table tbody tr:last-child td {
  border-bottom: none;
}

.bts-table tbody tr:hover td {
  background: #fff6f7;
}

.col-stt,
.col-num {
  text-align: center;
  width: 60px;
}

.col-img {
  width: 60px;
}

.col-act {
  width: 80px;
  text-align: center;
}

/* ── Cells ────────────────────────────────────────────────────────────── */
.product-thumb {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  overflow: hidden;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-placeholder {
  color: #94a3b8;
  font-size: 18px;
}

.link-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #0f172a;
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  padding: 0;
  text-decoration: none;
  transition: color 0.15s;
}

.link-btn:hover {
  color: #c5162d;
}

.code-badge {
  display: inline-block;
  padding: 2px 8px;
  background: #f1f5f9;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.code-badge.secondary {
  background: #f8fafc;
  color: #334155;
  border-color: #cbd5e1;
}

.color-cell {
  display: flex;
  align-items: center;
  gap: 7px;
}

.cdot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 1px solid rgba(15, 23, 42, 0.15);
}

.size-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.price {
  font-weight: 600;
  color: #c5162d;
}

/* Stock badge */
.stock-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.stock-badge.ok {
  background: #f1f5f9;
  color: #334155;
}

.stock-badge.low {
  background: #e2e8f0;
  color: #334155;
}

.stock-badge.out {
  background: #fee2e2;
  color: #dc2626;
}

/* Status pill */
.status-pill {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.status-pill.tone-success {
  background: #f1f5f9;
  color: #334155;
}

.status-pill.tone-danger {
  background: #fee2e2;
  color: #dc2626;
}

.status-pill.tone-warning {
  background: #f1f5f9;
  color: #334155;
}

.status-pill.tone-info {
  background: #f1f5f9;
  color: #334155;
}

.status-pill.tone-muted,
.status-pill.tone-default {
  background: #f1f5f9;
  color: #64748b;
}

/* Action button */
.btn-edit {
  border-color: #cbd5e1;
  color: #334155;
  background: #ffffff;
}

.btn-edit:hover {
  border-color: #c5162d;
  color: #c5162d;
  background: #fff1f2;
}

.btn-small {
  padding: 6px 12px;
  font-size: 13px;
}

.btn-eye {
  width: 34px;
  height: 34px;
  padding: 0;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-eye .material-icons-outlined {
  font-size: 18px;
}

.action-stack {
  display: flex;
  gap: 12px;
  justify-content: center;
  align-items: center;
}


.variant-switch {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  width: 44px;
  height: 24px;
  border: 1px solid #d1d5db;
  border-radius: 999px;
  background: #d1d5db; /* gray-300 */
  cursor: pointer;
  margin-top: 0;
}

.variant-switch.active {
  background: #b91c1c;
  border-color: #f87171;
}

.variant-switch-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  transition: transform .2s ease;
}

.variant-switch.active .variant-switch-thumb {
  transform: translateX(20px);
}

/* ── Empty / loading ──────────────────────────────────────────────────── */
.loading-state {
  text-align: center;
  padding: 40px;
  color: #64748b;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #64748b;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.muted {
  color: #64748b;
}

.color-chips-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.color-chip-mini {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 20px;
  background: #f1f5f9;
  font-size: 12px;
  white-space: nowrap;
}

.size-pills-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
