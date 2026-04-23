<script setup>
import { ref, computed, onMounted, watch } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  getAllSanPham,
  updateSanPham
} from "../../../services/sanPhamService"
import { Eye } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel } from "../../../utils/adminStatus"
import { resolveApiOrigin } from "../../../utils/apiOrigin"
import { getProductImageOverride } from "../../../utils/productImageOverrides"
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
import img16b from "../../../assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-red.PNG?url"
import img18b from "../../../assets/img/Jackets/hoodie/hoodie-camo/hoodie-camo-white.PNG?url"
import img19b from "../../../assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-white.PNG?url"
import img20b from "../../../assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-gray.PNG?url"
import img20c from "../../../assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-red.PNG?url"

const router = useRouter()
const route = useRoute()

const search = ref("")
const showActiveOnly = ref(false)
const list = ref([])
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")
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

const normalizeProductKey = (value = "") =>
  String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()

const getMappedFallbackByName = (name = "") => {
  const normalized = normalizeProductKey(name)
  if (!normalized) return ""

  const found = mappedFallbackByName.find((rule) =>
    rule.keywords.every((keyword) => normalized.includes(keyword))
  )
  return found?.image || ""
}

const normalizeSearchText = (value = "") =>
  normalizeProductKey(value)
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()

const stripTrailingBrandTokenForImage = (value = "") =>
  String(value || "")
    .trim()
    .replace(/[\s_-]*(dirty\s*wave|dirtywave)$/i, "")
    .trim()

const fallbackIndexByName = (name = "") => {
  const normalized = normalizeSearchText(stripTrailingBrandTokenForImage(name))
  if (!normalized) return -1

  let hash = 0
  for (let i = 0; i < normalized.length; i += 1) {
    hash = ((hash << 5) - hash + normalized.charCodeAt(i)) | 0
  }
  return Math.abs(hash) % fallbackImages.length
}

const fallbackImageFor = (id, code = "", name = "") => {
  const normalizedCode = String(code || "").trim().toUpperCase()
  const allowCuratedCodeMap = /^ATID070-\d+$/i.test(normalizedCode) || /^SP\d+$/i.test(normalizedCode)

  if (mappedFallbackByCode[normalizedCode]) {
    return mappedFallbackByCode[normalizedCode]
  }

  if (allowCuratedCodeMap) {
    const codeDigits = String(normalizedCode).replace(/\D+/g, "")
    const codeNum = Number(codeDigits)
    if (Number.isFinite(codeNum) && codeNum > 0 && mappedFallbackByCodeNum[codeNum]) {
      return mappedFallbackByCodeNum[codeNum]
    }
  }

  const mappedByName = getMappedFallbackByName(name)
  if (mappedByName) return mappedByName

  const normalizedId = Number(id)
  if (Number.isFinite(normalizedId) && normalizedId > 0) {
    if (allowCuratedCodeMap && mappedFallbackByCodeNum[normalizedId]) return mappedFallbackByCodeNum[normalizedId]
    const nameIndex = fallbackIndexByName(name)
    if (nameIndex >= 0) return fallbackImages[nameIndex] || logo
    return fallbackImages[(normalizedId - 1) % fallbackImages.length]
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

const isImageString = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return false
  if (/^data:image\//i.test(raw)) return true

  const normalized = raw.replace(/\\/g, "/").split(/[?#]/)[0]
  if (/\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i.test(normalized)) return true
  if (normalized.startsWith("/uploads/") || normalized.startsWith("uploads/")) return true
  return /^https?:\/\//i.test(raw)
}

const toImageUrl = (value = "") => {
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

const pickImageValue = (entry) => {
  if (!entry) return ""

  if (typeof entry === "string") {
    return isImageString(entry) ? toImageUrl(entry) : ""
  }

  if (Array.isArray(entry)) {
    for (const child of entry) {
      const found = pickImageValue(child)
      if (found) return found
    }
    return ""
  }

  if (typeof entry === "object") {
    const keys = ["anh", "hinhAnh", "image", "imageUrl", "images", "listAnh", "anhChinh", "duongDanAnh", "src", "thumbnail"]
    for (const key of keys) {
      const found = pickImageValue(entry[key])
      if (found) return found
    }
  }

  return ""
}

const routeBase = computed(() => {
  if (route.path.startsWith("/employee/")) return "/employee"
  return "/admin"
})

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

import { computeSoldBySpct, variantAvailableStock as _variantAvailableStock, variantStockValue as _variantStockValue } from "@/utils/stockCalculation"

async function loadSoldQtyBySpct() {
  return computeSoldBySpct()
}

function goToCreate() {
  const q = new URLSearchParams()
  if (search.value.trim()) q.set("q", search.value.trim())
  if (showActiveOnly.value) q.set("active", "1")
  const pages = JSON.stringify(currentPageByType.value || {})
  if (pages && pages !== "{}") q.set("pages", pages)
  const returnTo = q.toString() ? `${route.path}?${q.toString()}` : route.path

  router.push({
    path: `${routeBase.value}/san-pham/form`,
    query: { returnTo }
  })
}

function goToEdit(id) {
  const q = new URLSearchParams()
  if (search.value.trim()) q.set("q", search.value.trim())
  if (showActiveOnly.value) q.set("active", "1")
  const pages = JSON.stringify(currentPageByType.value || {})
  if (pages && pages !== "{}") q.set("pages", pages)
  const returnTo = q.toString() ? `${route.path}?${q.toString()}` : route.path

  router.push({
    path: `${routeBase.value}/san-pham/form/${id}`,
    query: { returnTo }
  })
}

function formatCurrency(value) {
  return new Intl.NumberFormat("vi-VN").format(value || 0) + "₫"
}

function formatDateTime(value) {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString("vi-VN")
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

function isUploadedImagePath(value = "") {
  return /(?:^|\/)uploads\//i.test(String(value || "").replace(/\\/g, "/"))
}

function hasCoreOrFlameToken(name = "") {
  return /\b(core|flame)\b/i.test(String(name || ""))
}

function variantStockValue(variant = {}) {
  return _variantStockValue(variant)
}

function variantAvailableStockForList(variant = {}, soldBySpct = new Map()) {
  return _variantAvailableStock(variant, soldBySpct)
}

function variantIdentityKeyForList(variant = {}) {
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

function toVariantList(product = {}) {
  if (Array.isArray(product?.sanPhamChiTiets)) return product.sanPhamChiTiets
  if (Array.isArray(product?.variants)) return product.variants
  if (Array.isArray(product?.chiTiets)) return product.chiTiets
  if (Array.isArray(product?.danhSachBienThe)) return product.danhSachBienThe
  return []
}

function toActiveVariantList(product = {}) {
  const variants = toVariantList(product)
  const activeVariants = variants.filter((variant) => isEntityActive(variant, true))
  return activeVariants.length ? activeVariants : variants
}

function mergeProductsForList(products = []) {
  const grouped = new Map()

  for (const product of (Array.isArray(products) ? products : [])) {
    const productId = Number(product?.id || 0)
    const code = String(product?.maSanPham || product?.ma || "").trim().toUpperCase()
    const familyKey = (productId > 0) ? `id:${productId}` : (code ? `code:${code}` : (normalizeProductFamilyKey(product?.tenSanPham || "") || `rand:${Math.random()}`))
    if (!grouped.has(familyKey)) grouped.set(familyKey, [])
    grouped.get(familyKey).push(product)
  }

  const merged = []

  for (const familyProducts of grouped.values()) {
    const ranked = [...familyProducts].sort((left, right) => {
      const leftPenalty = hasCoreOrFlameToken(left?.tenSanPham) ? 1 : 0
      const rightPenalty = hasCoreOrFlameToken(right?.tenSanPham) ? 1 : 0
      if (leftPenalty !== rightPenalty) return leftPenalty - rightPenalty

      const leftVariantCount = toActiveVariantList(left).length
      const rightVariantCount = toActiveVariantList(right).length
      if (leftVariantCount !== rightVariantCount) return rightVariantCount - leftVariantCount

      const leftId = Number(left?.id)
      const rightId = Number(right?.id)
      const normalizedLeftId = Number.isFinite(leftId) && leftId > 0 ? leftId : Number.MAX_SAFE_INTEGER
      const normalizedRightId = Number.isFinite(rightId) && rightId > 0 ? rightId : Number.MAX_SAFE_INTEGER
      return normalizedLeftId - normalizedRightId
    })

    const canonicalProduct = ranked[0] || familyProducts[0]
    const variantsByIdentity = new Map()

    for (const product of ranked) {
      for (const variant of toActiveVariantList(product)) {
        const identityKey = variantIdentityKeyForList(variant) || `id:${Number(variant?.id || 0)}`
        if (!variantsByIdentity.has(identityKey)) {
          variantsByIdentity.set(identityKey, variant)
          continue
        }

        const existingVariant = variantsByIdentity.get(identityKey)
        const existingActive = isEntityActive(existingVariant, true)
        const incomingActive = isEntityActive(variant, true)

        if (!existingActive && incomingActive) {
          variantsByIdentity.set(identityKey, variant)
        }
      }
    }

    merged.push({
      ...canonicalProduct,
      sanPhamChiTiets: [...variantsByIdentity.values()]
    })
  }

  return merged
}

async function loadData() {
  const [res, soldBySpct] = await Promise.all([getAllSanPham(), loadSoldQtyBySpct()])

  const rawProducts = toList(res?.data)
  const mergedProducts = mergeProductsForList(rawProducts)

  list.value = mergedProducts.map(item => {
    const variants = toVariantList(item)
    const hasVariants = variants.length > 0
    const catalogImage = fallbackImageFor(item.id, item.maSanPham, item.tenSanPham)
    const directImage = pickImageValue([item, variants])
    const preferredDirectImage = isUploadedImagePath(directImage) ? directImage : ""

    const totalTon =
      hasVariants
        ? variants.reduce((sum, v) => sum + variantAvailableStockForList(v, soldBySpct), 0)
        : Number(item?.soLuong ?? item?.soLuongTon ?? item?.tonKho ?? item?.ton ?? 0)
    const firstVariant = variants.length > 0 ? variants[0] : null

    const loaiName =
      firstVariant?.loai?.tenLoai ||
      item.loai ||
      "Khác"

    const normalizedGia =
      firstVariant?.giaBan ??
      item.giaBan ??
      item.gia ??
      null

    const normalizedTon =
      hasVariants
        ? (totalTon ?? 0)
        : (Number.isFinite(Number(item.soLuong ?? item.ton)) ? Number(item.soLuong ?? item.ton) : null)

    return {
      id: item.id,
      raw: item,
      ma: item.maSanPham,
      name: item.tenSanPham,
      description: item.moTa,
      image:
        getProductImageOverride({ id: item.id, maSanPham: item.maSanPham })[0] ||
        preferredDirectImage ||
        directImage ||
        catalogImage ||
        fallbackImageFor(item.id, item.maSanPham, item.tenSanPham),

      // gia:
      //   firstVariant?.giaBan ??
      //   item.giaBan ??
      //   item.gia ??
      //   0,

      gia: normalizedGia,

      ton: normalizedTon,

      loai: loaiName,
      type: loaiName,
      hasVariants,
      missingVariantData: !hasVariants,
      ngayTao: item.ngayTao,
      ngaySua: item.ngaySua,

      status: normalizeAdminStatusLabel(item.trangThai)
    }
  })
}

onMounted(async () => {
  const q = route.query || {}
  if (String(q.q || "").trim()) search.value = String(q.q)
  showActiveOnly.value = String(q.active || "") === "1"
  if (String(q.pages || "").trim()) {
    try {
      const parsed = JSON.parse(String(q.pages))
      if (parsed && typeof parsed === "object") {
        currentPageByType.value = { ...parsed }
      }
    } catch {
      // Ignore invalid persisted page map
    }
  }
  await loadData()
})

const searchedList = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  if (!keyword) return list.value
  return list.value.filter((item) =>
    String(item.ma || "").toLowerCase().includes(keyword) ||
    String(item.name || "").toLowerCase().includes(keyword)
  )
})

const filteredList = computed(() => {
  if (!showActiveOnly.value) return searchedList.value
  return searchedList.value.filter((item) => isProductActive(item.status))
})

const categoryPageSize = 5
const currentPageByType = ref({})

const groupedProducts = computed(() => {
  const groups = {}

  filteredList.value.forEach(item => {
    const key = item.type || "Khác"

    if (!groups[key]) {
      groups[key] = []
    }

    groups[key].push(item)
  })

  return groups
})

const pagedGroupedProducts = computed(() => {
  return Object.entries(groupedProducts.value).map(([type, items]) => {
    const totalPages = Math.max(1, Math.ceil(items.length / categoryPageSize))
    const requested = Number(currentPageByType.value[type] || 1)
    const currentPage = Math.min(Math.max(1, requested), totalPages)
    const start = (currentPage - 1) * categoryPageSize
    return {
      type,
      items: items.slice(start, start + categoryPageSize),
      totalItems: items.length,
      totalPages,
      currentPage
    }
  })
})

function goToCategoryPage(type, page) {
  const groupItems = groupedProducts.value[type] || []
  const totalPages = Math.max(1, Math.ceil(groupItems.length / categoryPageSize))
  if (page < 1 || page > totalPages) return
  currentPageByType.value = {
    ...currentPageByType.value,
    [type]: page
  }
}

watch(() => searchedList.value.length, () => {
  currentPageByType.value = {}
})

function isProductActive(status) {
  const normalized = String(status || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
  return !normalized.includes("ngung") && !normalized.includes("inactive")
}

async function toggleProductStatus(item) {
  const target = list.value.find((row) => Number(row.id) === Number(item?.id))
  if (!target) return

  const nextStatus = isProductActive(target.status) ? "Ngừng hoạt động" : "Hoạt động"
  const actionText = nextStatus === "Hoạt động" ? "bật" : "tắt"
  const confirmed = window.confirm(`Bạn có chắc muốn ${actionText} sản phẩm \"${target.name || target.ma || ""}\"?`)
  if (!confirmed) return

  try {
    const raw = target.raw && typeof target.raw === "object" ? target.raw : {}
    const payloadCandidates = [
      {
        ...raw,
        id: Number(target.id),
        maSanPham: raw?.maSanPham || target.ma,
        tenSanPham: raw?.tenSanPham || target.name,
        moTa: raw?.moTa ?? target.description ?? "",
        trangThai: nextStatus,
        sanPhamChiTiets: Array.isArray(raw?.sanPhamChiTiets) ? raw.sanPhamChiTiets : []
      },
      {
        ...raw,
        id: Number(target.id),
        maSanPham: raw?.maSanPham || target.ma,
        tenSanPham: raw?.tenSanPham || target.name,
        moTa: raw?.moTa ?? target.description ?? "",
        trangThai: nextStatus
      },
      {
        tenSanPham: raw?.tenSanPham || target.name,
        maSanPham: raw?.maSanPham || target.ma,
        moTa: raw?.moTa ?? target.description ?? "",
        trangThai: nextStatus
      }
    ]

    let updated = false
    let lastError = null
    for (const payload of payloadCandidates) {
      try {
        await updateSanPham(target.id, payload)
        updated = true
        break
      } catch (error) {
        lastError = error
      }
    }

    if (!updated) {
      throw lastError || new Error("Không thể cập nhật trạng thái sản phẩm")
    }

    if (target.raw && typeof target.raw === "object") {
      target.raw.trangThai = nextStatus
    }
    target.status = normalizeAdminStatusLabel(nextStatus)
    window.toast?.success?.(nextStatus === "Hoạt động" ? "Đã bật sản phẩm" : "Đã tắt sản phẩm")
  } catch (err) {
    console.error("Toggle status failed:", err)
    window.toast?.error?.("Không thể cập nhật trạng thái sản phẩm")
  }
}

</script>

<template>
  <div class="card admin-product-page">

    <div class="head page-header">
      <div>
        <h1 class="page-title">Sản phẩm</h1>
        <small class="muted page-subtitle">
          Danh sách sản phẩm + tồn kho, trạng thái bán
        </small>
      </div>

      <button class="btn-create" @click="goToCreate">
        + Thêm sản phẩm
      </button>
    </div>

    <div class="body">

      <div class="toolbar">
        <div class="filters">
          <input
            v-model="search"
            class="form-input search-input"
            type="text"
            placeholder="Tìm theo mã / tên..."
          />
          <label class="active-only-toggle">
            <input v-model="showActiveOnly" type="checkbox" />
            <span>Chỉ hiện sản phẩm đang hoạt động</span>
          </label>
        </div>
      </div>

      <!-- CATEGORY FRAMES -->

      <div
        v-for="group in pagedGroupedProducts"
        :key="group.type"
        class="category-frame"
      >

        <div class="category-header">
          {{ group.type }}
          <span class="category-count">({{ group.totalItems }} sản phẩm)</span>
        </div>

        <table class="data-table">
          <thead>
            <tr>
              <th style="width:96px">Ảnh</th>
              <th style="width:120px">Mã</th>
              <th>Sản phẩm</th>
              <th style="width:140px">Loại</th>
              <th style="width:140px" class="right">Giá</th>
              <th style="width:120px" class="right">Tồn</th>
              <th style="width:170px;text-align:center">Ngày tạo</th>
              <th style="width:170px;text-align:center">Ngày sửa</th>
              <th style="width:140px;text-align:center">Trạng thái</th>
              <th style="width:160px;text-align:center">Thao tác</th>
            </tr>
          </thead>

          <tbody>

            <tr
              v-for="item in group.items"
              :key="item.id"
            >
              <td>
                <img class="product-thumb" :src="item.image" :alt="item.name" />
              </td>
              <td>{{ item.ma }}</td>

              <td>
                <b>{{ item.name }}</b>

                <div class="muted product-desc">
                  {{ item.description }}
                </div>

                <div v-if="item.missingVariantData" class="warn-inline">
                  Chưa cấu hình biến thể
                </div>
              </td>

              <td>{{ item.loai }}</td>

              <td class="right">
                <template v-if="item.gia === null">
                  <span class="muted">-</span>
                </template>
                <template v-else>
                  {{ formatCurrency(item.gia) }}
                </template>
              </td>

              <td class="right">
                <template v-if="item.ton === null">
                  <span class="muted">-</span>
                </template>
                <template v-else>
                  {{ item.ton }}
                </template>
              </td>

              <td style="text-align:center">{{ formatDateTime(item.ngayTao) }}</td>

              <td style="text-align:center">{{ formatDateTime(item.ngaySua) }}</td>

              <td style="text-align:center">
                <span
                  class="pill"
                  :class="`status-${getAdminStatusTone(item.status)}`"
                >
                  ● {{ item.status }}
                </span>
              </td>

              <td style="text-align:center">

                <div class="actions" style="justify-content:center">

                  <button
                    class="iconbtn"
                    @click="goToEdit(item.id)"
                    title="Xem chi tiết"
                  >
                    <Eye size="16" />
                  </button>
                  <button
                    type="button"
                    class="switch-btn"
                    :class="{ active: isProductActive(item.status) }"
                    :title="isProductActive(item.status) ? 'Đang bật - bấm để tắt' : 'Đang tắt - bấm để bật'"
                    @click="toggleProductStatus(item)"
                  >
                    <span class="switch-thumb"></span>
                  </button>
                </div>

              </td>
            </tr>

          </tbody>
        </table>

        <div v-if="group.totalPages > 1" class="category-pagination">
          <button
            type="button"
            :disabled="group.currentPage <= 1"
            @click="goToCategoryPage(group.type, group.currentPage - 1)"
          >&laquo;</button>
          <button
            v-for="p in group.totalPages"
            :key="`${group.type}-${p}`"
            :class="{ active: p === group.currentPage }"
            @click="goToCategoryPage(group.type, p)"
          >{{ p }}</button>
          <button
            type="button"
            :disabled="group.currentPage >= group.totalPages"
            @click="goToCategoryPage(group.type, group.currentPage + 1)"
          >&raquo;</button>
        </div>

      </div>

      <div
        v-if="filteredList.length === 0"
        class="empty"
      >
        Không có dữ liệu
      </div>

      <div class="pagination">
        <div>
          Hiển thị {{ filteredList.length }} sản phẩm, mỗi loại {{ categoryPageSize }} sản phẩm/trang
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap');

.admin-product-page {
  font-family: 'Be Vietnam Pro', 'Segoe UI', Tahoma, sans-serif;
  padding: 28px 30px;
  background: transparent;
  min-height: calc(100vh - 76px);
  border: 1px solid #e5eaf4;
  border-radius: 20px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.04);
  overflow: hidden;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.page-title {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.1;
  letter-spacing: -0.03em;
  font-weight: 800;
}

.page-subtitle {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 15px;
  font-weight: 500;
}

.btn-create {
  height: 44px;
  border: 0;
  border-radius: 12px;
  background: #ef2d2d;
  color: #fff;
  cursor: pointer;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.btn-create:hover {
  background: #dc2626;
  transform: translateY(-1px);
}

.body {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 16px;
}

.toolbar {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
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

.btn-open-variant {
  height: 40px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #fff;
  color: #1f2937;
  font-size: 13px;
  font-weight: 700;
  padding: 0 12px;
  cursor: pointer;
}

.btn-open-variant:hover {
  border-color: #c5162d;
  color: #c5162d;
}

.search-input {
  width: 320px;
  max-width: 100%;
}

.form-input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #fff;
  font-size: 14px;
  color: #111827;
}

.form-input:focus {
  outline: none;
  border-color: #93c5fd;
  box-shadow: 0 0 0 3px rgba(147, 197, 253, 0.26);
}

.category-frame{
  border:1px solid #e5e7eb;
  border-radius:12px;
  overflow:hidden;
  margin-bottom:14px;
  background:#fff;
}

.category-header{
  background:#f8fafc;
  padding:14px 16px;
  font-weight:700;
  font-size:15px;
  color: #334155;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.category-count {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.product-desc{
  font-size:12px;
  margin-top:3px;
  color: #94a3b8;
}

.product-thumb {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #dbe4f0;
  background: #f8fafc;
}

.warn-inline {
  margin-top: 6px;
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  color: #a16207;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 999px;
  padding: 2px 8px;
}

.actions{
  display:flex;
  justify-content:center;
  align-items:center;
  gap:14px;
}

.iconbtn{
  display:flex;
  align-items:center;
  justify-content:center;
  width:32px;
  height:32px;
  border-radius:8px;
  border:1px solid #d1d5db;
  background:#fff;
  cursor:pointer;
  color: #64748b;
}

.iconbtn:hover{
  background:#f1f5f9;
}

.switch-btn {
  position: relative;
  width: 44px;
  height: 24px;
  border: 1px solid #fecaca;
  border-radius: 999px;
  background: #dc2626;
  cursor: pointer;
  transition: background-color .2s ease;
  align-self: center;
}

.switch-btn.active {
  background: #b91c1c;
  border-color: #f87171;
}

.switch-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  transition: transform .2s ease;
}

.switch-btn.active .switch-thumb {
  transform: translateX(20px);
}

.textbtn {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 13px 16px;
  border-bottom: 1px solid #eef2f7;
  text-align: left;
}

.data-table thead th {
  color: #94a3b8;
  background: #f8fafc;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
}

.data-table tbody td {
  color: #334155;
  vertical-align: middle;
}

.col-actions {
  text-align: center !important;
}

.pill.ok {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #15803d;
  background: #dcfce7;
}

.pill.warn {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #b91c1c;
  background: #fee2e2;
}

.right{
  text-align:right;
}

.empty{
  text-align:center;
  padding:52px 12px;
  color:#64748b;
}

.pagination {
  color: #64748b;
  font-size: 13px;
}

.ap-pagination {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 10px;
}
.category-pagination {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin: 10px 0 12px;
}
.category-pagination button {
  min-width: 34px;
  height: 34px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}
.category-pagination button.active {
  background: #dc2626;
  color: #fff;
  border-color: #dc2626;
}
.category-pagination button:disabled {
  opacity: .4;
  cursor: default;
}
.ap-pagination button {
  min-width: 34px;
  height: 34px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}
.ap-pagination button.active {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}
.ap-pagination button:disabled {
  opacity: .4;
  cursor: default;
}

@media (max-width: 1024px) {
  .page-title {
    font-size: 20px;
  }
}

@media (max-width: 768px) {
  .admin-product-page {
    padding: 18px 14px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-title {
    font-size: 18px;
  }

  .btn-create {
    justify-content: center;
  }

  .search-input {
    width: 100%;
  }
}

</style>