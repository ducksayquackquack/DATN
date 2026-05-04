<script setup>
import { computed, onMounted, onBeforeUnmount, reactive, ref, watch } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  createSanPham,
  updateSanPham,
  getSanPhamById,
  getAllSanPham
} from "../../../services/sanPhamService"
import { getAllLoai } from "../../../services/loaiService"
import { getAllKichThuoc } from "../../../services/kichThuocService"
import { getAllMauSac } from "../../../services/mauSacService"
import { resolveApiOrigin } from "../../../utils/apiOrigin"
import { clearProductImageOverride, getProductImageConfig, setProductImageConfig } from "../../../utils/productImageOverrides"
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
const id = route.params.id

const routeBase = computed(() => {
  if (route.path.startsWith("/employee/")) return "/employee"
  return "/admin"
})

const imageConfigSyncReady = ref(!id)

const forceColorlessFromRoute = computed(() => {
  return String(route.query?.hasColors || "").trim() === "0"
})
const hasRealColorsFromData = ref(null)
const forceColorlessMode = computed(() => {
  if (forceColorlessFromRoute.value) return true
  if (!id) return false
  return hasRealColorsFromData.value === false
})

const backTarget = computed(() => {
  const returnTo = String(route.query?.returnTo || "").trim()
  if (returnTo && (returnTo.startsWith("/admin/") || returnTo.startsWith("/employee/"))) {
    return returnTo
  }

  const from = String(route.query?.from || "").toLowerCase()
  if (from === "thong-ke") {
    return `${routeBase.value}/thong-ke/doanh-thu`
  }
  if (from === "bien-the") {
    return `${routeBase.value}/san-pham/bien-the`
  }
  return `${routeBase.value}/san-pham/list`
})

// colorId từ query param khi vào từ BienTheSanPham
const focusColorId = computed(() => Number(route.query?.colorId || 0))

const editScopeProductIds = computed(() => {
  const raw = String(route.query?.productIds || "").trim()
  if (!raw) return []

  const parsed = raw
    .split(",")
    .map((item) => Number(String(item || "").trim()))
    .filter((value) => Number.isFinite(value) && value > 0)

  return [...new Set(parsed)]
})

const form = reactive({
  sku: "",
  name: "",
  description: "",
  status: "Đang bán",
  giaBan: 0,
  giaGoc: 0,
  ton: 0,
  loaiId: null
})

const loaiOptions = ref([])
const sizeOptions = ref([])
const colorOptions = ref([])
const variantRows = ref([])
const imageCards = ref([])
const originalVariantIds = ref([])
const removedVariantRows = ref([])
const knownVariantCodes = ref([])
const soldBySpct = ref(new Map())

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

const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")
const fallbackImages = [img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11, img12, img13, img14, img15, img16, img17, img18, img19, img20]
const fallbackImageSet = new Set([
  ...fallbackImages,
  img12b,
  img13b,
  img13c,
  img13d,
  img13e,
  img14b,
  img14c,
  img16b,
  img16c,
  img18b,
  img19b,
  img20b,
  img20c
])

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
  const allowCuratedCodeMap = isCuratedCatalogCode(normalizedCode)

  if (mappedFallbackByCode[normalizedCode]) {
    return mappedFallbackByCode[normalizedCode]
  }

  const mappedByName = getMappedFallbackByName(name)
  if (mappedByName) return mappedByName

  if (!allowCuratedCodeMap) {
    return ""
  }

  const codeDigits = String(normalizedCode).replace(/\D+/g, "")
  const codeNum = Number(codeDigits)
  if (Number.isFinite(codeNum) && codeNum > 0 && mappedFallbackByCodeNum[codeNum]) {
    return mappedFallbackByCodeNum[codeNum]
  }

  const normalizedId = Number(id)
  if (Number.isFinite(normalizedId) && normalizedId > 0) {
    if (mappedFallbackByCodeNum[normalizedId]) return mappedFallbackByCodeNum[normalizedId]
    const nameIndex = fallbackIndexByName(name)
    if (nameIndex >= 0) return fallbackImages[nameIndex] || logo
    return fallbackImages[(normalizedId - 1) % fallbackImages.length]
  }

  const digits = String(code || "").replace(/\D+/g, "")
  const trailingCodeNum = Number(digits)
  if (Number.isFinite(trailingCodeNum) && trailingCodeNum > 0) {
    if (allowCuratedCodeMap && mappedFallbackByCodeNum[trailingCodeNum]) return mappedFallbackByCodeNum[trailingCodeNum]
    const nameIndex = fallbackIndexByName(name)
    if (nameIndex >= 0) return fallbackImages[nameIndex] || logo
    return fallbackImages[(trailingCodeNum - 1) % fallbackImages.length]
  }

  const nameIndex = fallbackIndexByName(name)
  if (nameIndex >= 0) return fallbackImages[nameIndex] || logo

  return fallbackImages[0] || logo
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

const extractProductCodeNumber = (value = "") => {
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

  const isCuratedCatalogCode = (value = "") => {
    const codeNum = extractProductCodeNumber(value)
    return Number.isFinite(codeNum) && codeNum >= 1 && codeNum <= 20
  }

  const shouldStripFallbackImagesForStorage = (productLike = {}) => {
    const code = String(
      productLike?.maSanPham ||
      productLike?.ma ||
      productLike?.sku ||
      form.sku ||
      ""
    ).trim()

    return isCuratedCatalogCode(code)
  }

const normalizeColorKey = (value = "") => {
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

const colorSearchTokens = (colorName = "") => {
  const key = normalizeColorKey(colorName)
  if (!key) return []

  const tokenMap = {
    den: ["black"],
    do: ["red"],
    xam: ["gray", "grey"],
    ghi: ["gray", "grey"],
    trang: ["white"],
    nau: ["brown"],
    be: ["beige"],
    cam: ["orange"],
    vang: ["yellow"],
    "xanh duong": ["blue"],
    "xanh la": ["green"],
    "xanh navy": ["navy", "blue"],
    xanh: ["blue", "green"]
  }

  return [...new Set(tokenMap[key] || [])]
}

const escapeRegExp = (value = "") => String(value || "").replace(/[.*+?^${}()|[\]\\]/g, "\\$&")

const imageHasColorToken = (image = "", token = "") => {
  const source = String(image || "").toLowerCase().replace(/\\/g, "/").split(/[?#]/)[0]
  const fileName = source.split("/").pop() || source
  const escaped = escapeRegExp(String(token || "").toLowerCase())
  if (!escaped) return false
  const pattern = new RegExp(`(?:^|[^a-z0-9])${escaped}(?:[^a-z0-9]|$)`)
  return pattern.test(fileName)
}

const pickImageFromListByColor = (images = [], colorName = "") => {
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

const getStaticColorCandidates = (productId, productLike = {}) => {
  const normalizedName = normalizeSearchText(productLike?.tenSanPham || form.name || "")
  if (normalizedName) {
    const byName = staticColorImageRulesByName.find((rule) =>
      rule.keywords.every((keyword) => normalizedName.includes(keyword))
    )
    if (Array.isArray(byName?.images) && byName.images.length) return byName.images
  }

  const byProductId = staticColorImageMap[Number(productId)]
  if (Array.isArray(byProductId) && byProductId.length) return byProductId

  const codeNumber = extractProductCodeNumber(
    productLike?.maSanPham || productLike?.ma || productLike?.sku || form.sku
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

const pickStaticColorImage = (productId, colorName = "", productLike = {}) => {
  const colorKey = normalizeColorKey(colorName)
  const pId = Number(productId)
  if (colorKey && staticColorImageByColor[pId]?.[colorKey]) return staticColorImageByColor[pId][colorKey]
  const codeNum = extractProductCodeNumber(productLike?.maSanPham || productLike?.ma || productLike?.sku || form.sku)
  if (colorKey && staticColorImageByColor[codeNum]?.[colorKey]) return staticColorImageByColor[codeNum][colorKey]
  const candidates = getStaticColorCandidates(productId, productLike)
  if (!candidates.length) return ""
  if (candidates.length === 1) return candidates[0]
  return pickImageFromListByColor(candidates, colorName) || candidates[0]
}

const resolveFallbackColorImage = (color = {}, index = 0, productLike = {}, fallbackGallery = []) => {
  const normalizedGallery = (Array.isArray(fallbackGallery) ? fallbackGallery : [])
    .map((img) => String(img || "").trim())
    .filter(Boolean)

  const galleryByColor = pickImageFromListByColor(normalizedGallery, color?.name || "")
  if (galleryByColor) return galleryByColor

  const productId = Number(productLike?.id || id || route.params?.id || 0)
  const staticByColor = pickStaticColorImage(productId, color?.name || "", productLike)
  if (staticByColor) return staticByColor

  return String(normalizedGallery[index] || normalizedGallery[0] || "").trim()
}

const resolveConfiguredColorImage = (configuredImage = "", color = {}, index = 0, productLike = {}, fallbackGallery = []) => {
  const current = String(configuredImage || "").trim()
  const fallback = resolveFallbackColorImage(color, index, productLike, fallbackGallery)
  if (!current) return fallback

  const hasFolderPalette =
    folderColorPaletteByName(productLike?.tenSanPham || form.name).length > 1 ||
    getStaticColorCandidates(Number(productLike?.id || id || route.params?.id || 0), productLike).length > 1
  if (hasFolderPalette) {
    return fallback || current
  }

  if (/^data:image\//i.test(current)) return current
  if (/^https?:\/\//i.test(current)) return current
  if (/uploads\//i.test(current)) return current

  const hasTokenMatch = Boolean(pickImageFromListByColor([current], color?.name || ""))
  if (hasTokenMatch) return current

  // Only switch to fallback when current is one of our built-in fallback assets.
  // Never override user-chosen or backend-provided image just because color changed.
  if (fallback && fallbackImageSet.has(current)) {
    return fallback
  }

  return current
}

const createImageCard = (src = "") => ({
  key: `${Date.now()}-${Math.random()}`,
  src: String(src || "").trim()
})

const isImageString = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return false
  if (/^data:image\//i.test(raw)) return true

  const normalized = raw.replace(/\\/g, "/").split(/[?#]/)[0]

  if (/^https?:\/\//i.test(raw)) {
    return /\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i.test(normalized)
  }

  if (normalized.startsWith("/uploads/") || normalized.startsWith("uploads/")) return true
  if (/\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i.test(normalized)) return true

  return false
}

const toImageUrl = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return ""
  if (/^data:image\//i.test(raw)) return raw

  const normalized = raw.replace(/\\/g, "/")
  const uploadsMatch = normalized.match(/^.*?\/?(uploads\/.*)$/i)

  if (uploadsMatch?.[1]) {
    return `${BACKEND_ORIGIN}/${uploadsMatch[1]}`
  }

  if (/^https?:\/\//i.test(normalized)) return normalized
  if (normalized.startsWith("/")) return normalized
  if (normalized.startsWith("uploads/")) return `${BACKEND_ORIGIN}/${normalized}`
  return normalized.includes("/") ? `/${normalized.replace(/^\/+/, "")}` : normalized
}

const extractImageList = (data) => {
  const bucket = []

  const walk = (entry) => {
    if (!entry) return

    if (typeof entry === "string") {
      const trimmed = entry.trim()
      if (trimmed && isImageString(trimmed)) {
        bucket.push(toImageUrl(trimmed))
      }
      return
    }

    if (Array.isArray(entry)) {
      entry.forEach(walk)
      return
    }

    if (typeof entry === "object") {
      const keys = ["anh", "hinhAnh", "image", "imageUrl", "images", "listAnh", "anhChinh", "duongDanAnh"]
      keys.forEach((key) => walk(entry[key]))
    }
  }

  walk(data)
  return [...new Set(bucket)]
}

const isUploadedImagePath = (value = "") => {
  const normalized = String(value || "").trim().replace(/\\/g, "/")
  if (!normalized) return false
  return /(?:^|\/)uploads\//i.test(normalized)
}

const resolvePreferredGalleryImages = (images = [], productLike = {}) => {
  const normalizedImages = [...new Set(
    (Array.isArray(images) ? images : [images])
      .map((image) => String(image || "").trim())
      .filter(Boolean)
  )]

  const uploadedImages = normalizedImages.filter((image) => isUploadedImagePath(image))
  if (uploadedImages.length) return uploadedImages

  // Keep real images coming from backend payload (URL/base64) before any static fallback.
  // Static fallbacks should only be used when the backend provides no image at all.
  if (normalizedImages.length) return normalizedImages

  const staticCandidates = getStaticColorCandidates(Number(productLike?.id || 0), productLike)
  if (staticCandidates.length) return staticCandidates

  const mappedByName = getMappedFallbackByName(productLike?.tenSanPham || productLike?.name || form.name)
  if (mappedByName) return [mappedByName]

  return normalizedImages
}

const colorMappedCardKeySet = computed(() => {
  return new Set(
    Object.values(colorImageToCardKey.value)
      .map((value) => String(value || "").trim())
      .filter(Boolean)
  )
})

const extraImageCards = computed(() => {
  return imageCards.value
    .map((item, sourceIndex) => ({
      key: String(item?.key || "").trim(),
      src: String(item?.src || "").trim(),
      sourceIndex
    }))
    .filter((item) => item.key && !colorMappedCardKeySet.value.has(item.key))
})

const allImageValues = computed(() => {
  const colorMapped = colorImageDisplayColors.value
    .map((color) => {
      const index = colorImageDisplayColors.value.findIndex((item) => normalizeName(item?.name || "") === normalizeName(color?.name || ""))
      const key = toColorStorageKey(color, index >= 0 ? index : 0)
      const mappedCardKey = colorImageToCardKey.value[key]
      const mappedCard = imageCards.value.find((item) => item.key === mappedCardKey)
      return String(colorImages.value[key]?.previewUrl || mappedCard?.src || "").trim()
    })
    .filter(Boolean)

  const extraImages = extraImageCards.value
    .map((item) => String(item?.src || "").trim())
    .filter(Boolean)

  const merged = [...new Set([...colorMapped, ...extraImages])]
  if (merged.length) return merged

  if (id) {
    return [fallbackImageFor(Number(route.params.id), form.sku || route.params.id, form.name)]
  }

  return []
})

const resolveImageCardSrc = (imageCard, index) => {
  const raw = String(imageCard?.src || "").trim()
  if (raw) return raw
  if (id && index === 0) {
    return fallbackImageFor(Number(route.params.id), form.sku || route.params.id, form.name)
  }
  return ""
}

const removeImageAt = (index) => {
  imageCards.value.splice(index, 1)
}

const readImageFile = async (file) => {
  if (!file?.type?.startsWith("image/")) return ""

  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ""))
    reader.onerror = () => reject(new Error("Không đọc được ảnh"))
    reader.readAsDataURL(file)
  }).catch(() => "")
}

const appendEmptyImageCard = () => {
  imageCards.value.push(createImageCard())
}

const stripTrailingBrandToken = (value = "") => {
  return String(value || "")
    .trim()
    .replace(/[\s_-]*(dirty\s*wave|dirtywave)$/i, "")
    .trim()
}

const normalizeProductFamilyKey = (name = "") => {
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

const dedupeVariantList = (variants = []) => {
  const list = Array.isArray(variants) ? variants : []
  const seen = new Set()

  return list.filter((variant) => {
    const variantId = Number(variant?.id)
    const colorKey = normalizeColorKey(
      variant?.mauSac?.tenMau ||
      variant?.mauSac?.tenMauSac ||
      variant?.mauSac?.name ||
      ""
    )
    const sizeKey = normalizeColorKey(
      variant?.kichThuoc?.tenKichThuoc ||
      variant?.kichThuoc?.name ||
      ""
    )
    const priceKey = Number(variant?.giaBan || 0)

    const key = Number.isFinite(variantId) && variantId > 0
      ? `id:${variantId}`
      : `c:${colorKey}|s:${sizeKey}|p:${priceKey}`

    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

const mergeProductImageConfigs = (products = []) => {
  const list = Array.isArray(products) ? products : []
  const seenImages = new Set()
  const mergedImages = []
  const mergedColorImagesMap = new Map()

  list.forEach((product) => {
    const config = getProductImageConfig({
      id: Number(product?.id || 0),
      maSanPham: product?.maSanPham || product?.ma || ""
    })

    ;(Array.isArray(config?.images) ? config.images : []).forEach((image) => {
      const normalized = String(image || "").trim()
      if (!normalized || seenImages.has(normalized)) return
      seenImages.add(normalized)
      mergedImages.push(normalized)
    })

    ;(Array.isArray(config?.colorImages) ? config.colorImages : []).forEach((entry) => {
      const colorId = Number(entry?.colorId)
      const image = String(entry?.image || "").trim()
      if (!Number.isFinite(colorId) || colorId <= 0 || !image) return
      if (!mergedColorImagesMap.has(colorId)) {
        mergedColorImagesMap.set(colorId, image)
      }
    })
  })

  const mergedColorImages = [...mergedColorImagesMap.entries()].map(([colorId, image], index) => ({
    colorId: Number(colorId),
    image,
    order: index
  }))

  return {
    images: mergedImages,
    colorImages: mergedColorImages
  }
}

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

const normalizeSearchTextForFolder = (value = "") =>
  String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()

const folderImageCountByName = (name = "") => {
  const normalized = normalizeSearchTextForFolder(stripTrailingBrandToken(name))
  if (!normalized) return 0

  const found = folderImageCountRules.find((rule) =>
    rule.keywords.every((keyword) => normalized.includes(keyword))
  )

  return Number(found?.count || 0)
}

const folderColorPaletteByName = (name = "") => {
  const normalized = normalizeSearchTextForFolder(stripTrailingBrandToken(name))
  if (!normalized) return []

  const found = folderColorPaletteRules.find((rule) =>
    rule.keywords.every((keyword) => normalized.includes(keyword))
  )

  return Array.isArray(found?.colors) ? found.colors : []
}

const displaySizeOptions = computed(() => sizeOptions.value)

const prioritizeImagesForSave = (images = []) => {
  const normalized = images.map((item) => String(item || "").trim()).filter(Boolean)
  if (!normalized.length) return []

  const nonFallback = normalized.filter((item) => !fallbackImageSet.has(item))
  const fallbackOnly = normalized.filter((item) => fallbackImageSet.has(item))
  return [...nonFallback, ...fallbackOnly]
}

const handleImageCardFileChange = async (index, event) => {
  const files = Array.from(event?.target?.files || [])
  if (!files.length) return

  const next = []
  for (const file of files) {
    const dataUrl = await readImageFile(file)
    if (dataUrl) next.push(dataUrl)
  }

  if (!next.length) {
    window.toast.warning("Không có tệp ảnh hợp lệ")
    return
  }

  const normalizedCurrent = imageCards.value.map((item) => ({ ...item }))

  if (!normalizedCurrent[index]) {
    normalizedCurrent[index] = createImageCard()
  }

  // Chon anh luon thay the card hien tai, khong tao card moi khi chi co 1 file.
  normalizedCurrent[index].src = next[0]

  // Neu nguoi dung chon nhieu anh cung luc, cac anh con lai moi them sau card hien tai.
  if (next.length > 1) {
    const extras = next.slice(1).map((src) => createImageCard(src))
    normalizedCurrent.splice(index + 1, 0, ...extras)
  }

  imageCards.value = normalizedCurrent

  event.target.value = ""
  window.toast.success(`Đã thêm ${next.length} ảnh`)
}

const handleExtraImageUpload = async (event) => {
  const files = Array.from(event?.target?.files || [])
  if (!files.length) return

  const next = []
  for (const file of files) {
    const dataUrl = await readImageFile(file)
    if (dataUrl) next.push(dataUrl)
  }

  if (!next.length) {
    window.toast.warning("Không có tệp ảnh hợp lệ")
    event.target.value = ""
    return
  }

  imageCards.value = [...imageCards.value, ...next.map((src) => createImageCard(src))]
  event.target.value = ""
  window.toast.success(`Đã thêm ${next.length} ảnh sản phẩm`)
}

function goBack() {
  router.push(backTarget.value)
}

const normalizeName = (value = "") => {
  const normalized = String(value)
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .trim()
    .toLowerCase()

  if (normalized === "xanh navy" || normalized === "navy") return "xanh duong"
  return normalized
}

const normalizeStatusText = (value = "") =>
  String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()

const isVariantActive = (variant = {}) => {
  const rawStatus = variant?.trangThai ?? variant?.status

  if (typeof rawStatus === "boolean") return rawStatus
  if (typeof rawStatus === "number") return rawStatus !== 0

  const normalized = normalizeStatusText(rawStatus)
  if (normalized) {
    if (normalized.includes("ngung") || normalized.includes("inactive") || normalized.includes("disable")) {
      return false
    }
    if (normalized.includes("hoat dong") || normalized.includes("active") || normalized.includes("enable")) {
      return true
    }
  }

  if (typeof variant?.active === "boolean") return variant.active
  if (typeof variant?.isActive === "boolean") return variant.isActive
  if (typeof variant?.active === "number") return variant.active !== 0
  if (typeof variant?.isActive === "number") return variant.isActive !== 0

  return true
}

const normalizeDisplayColorName = (value = "") => {
  const raw = String(value || "").trim()
  const normalized = normalizeName(raw)
  if (!normalized) return ""
  if (normalized === "xanh duong") return "Xanh dương"
  return raw
}

const getSizeLabel = (sizeId) => {
  return sizeOptions.value.find((item) => Number(item.id) === Number(sizeId))?.name || ""
}

const getColorLabel = (colorId) => {
  return colorOptions.value.find((item) => Number(item.id) === Number(colorId))?.name || ""
}

const colorHexByName = (name = "") => {
  if (/^#([0-9a-f]{3}|[0-9a-f]{6})$/i.test(String(name || "").trim())) return String(name).trim()
  const n = normalizeName(name)
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

const getColorMeta = (colorId) => {
  const numericId = Number(colorId)
  const fromMaster = colorOptions.value.find((item) => Number(item.id) === numericId)
  if (fromMaster) return fromMaster

  const fromVirtual = virtualColorMetaById.value[String(colorId)] || virtualColorMetaById.value[String(numericId)]
  if (fromVirtual) return fromVirtual

  return null
}

const getSizeMeta = (sizeId) => {
  return sizeOptions.value.find((item) => Number(item.id) === Number(sizeId)) || null
}

const createEmptyVariantRow = () => ({
  key: `${Date.now()}-${Math.random()}`,
  id: null,
  ma: "",
  sizeId: null,
  colorId: null,
  giaBan: Number(form.giaBan || 0),
  soLuong: Number(form.ton || 0),
  trangThai: "Hoạt động"
})

function addVariantRow(payload = {}) {
  variantRows.value.push({
    ...createEmptyVariantRow(),
    ...payload
  })
}

function sortVariantRows() {
  const colorOrder = new Map(selectedColors.value.map((c, i) => [Number(c.id), i]))
  const sizeOrder = new Map(selectedSizes.value.map((s, i) => [Number(s.id), i]))
  variantRows.value.sort((a, b) => {
    const ca = colorOrder.get(Number(a.colorId)) ?? 9999
    const cb = colorOrder.get(Number(b.colorId)) ?? 9999
    if (ca !== cb) return ca - cb
    const sa = sizeOrder.get(Number(a.sizeId)) ?? 9999
    const sb = sizeOrder.get(Number(b.sizeId)) ?? 9999
    return sa - sb
  })
}

function removeVariantRow(index) {
  const row = variantRows.value[index]
  if (row && Number(row?.id) > 0) {
    const existing = removedVariantRows.value.some((item) => Number(item?.id) === Number(row.id))
    if (!existing) {
      removedVariantRows.value = [...removedVariantRows.value, { ...row }]
    }
  }
  variantRows.value.splice(index, 1)
}

function removeColorGroup(colorId) {
  const targetColorName = normalizeName(getColorMeta(colorId)?.name || "")

  // Delete the rows entirely so no orphan "Chưa phân màu" group appears
  const removedRows = variantRows.value.filter((row) => {
    const isMatch = targetColorName
      ? normalizeName(getColorMeta(row?.colorId)?.name || "") === targetColorName
      : Number(row?.colorId) === Number(colorId)
    return isMatch
  })

  for (const row of removedRows) {
    if (Number(row?.id) > 0) {
      const existing = removedVariantRows.value.some((item) => Number(item?.id) === Number(row.id))
      if (!existing) {
        removedVariantRows.value = [...removedVariantRows.value, { ...row }]
      }
    }
  }

  variantRows.value = variantRows.value.filter((row) => {
    const isMatch = targetColorName
      ? normalizeName(getColorMeta(row?.colorId)?.name || "") === targetColorName
      : Number(row?.colorId) === Number(colorId)
    return !isMatch
  })

  if (!targetColorName) {
    const idx = selectedColors.value.findIndex((c) => Number(c.id) === Number(colorId))
    if (idx >= 0) selectedColors.value.splice(idx, 1)
    return
  }

  selectedColors.value = selectedColors.value.filter(
    (color) => normalizeName(color?.name || "") !== targetColorName
  )
}

function findOptionByName(options, candidateName) {
  const normalized = normalizeName(candidateName)
  return options.find((item) => normalizeName(item.name) === normalized) || null
}

function dedupeColorOptionsByName(options = []) {
  const grouped = new Map()
  for (const option of (Array.isArray(options) ? options : [])) {
    const normalized = normalizeName(option?.name || "")
    if (!normalized) continue
    if (!grouped.has(normalized)) {
      grouped.set(normalized, option)
    }
  }
  return [...grouped.values()]
}

const VIRTUAL_COLOR_ID_SEED = -100000
const virtualColorMetaById = ref({})
const virtualColorIdByName = ref({})
const virtualColorCounter = ref(0)

const resolveOrCreateVirtualColorId = (colorLike = {}) => {
  const colorName = normalizeDisplayColorName(colorLike?.name || colorLike)
  const normalized = normalizeName(colorName)
  if (!normalized) return null

  const existingByName = Number(virtualColorIdByName.value[normalized])
  if (Number.isFinite(existingByName)) return existingByName

  const existingReal = findOptionByName(colorOptions.value, colorName)
  if (existingReal) return Number(existingReal.id)

  const nextId = VIRTUAL_COLOR_ID_SEED - Number(virtualColorCounter.value || 0)
  virtualColorCounter.value += 1
  const meta = {
    id: nextId,
    name: colorName,
    hex: colorHexByName(colorLike?.hex || colorName),
    isVirtual: true
  }

  virtualColorMetaById.value = {
    ...virtualColorMetaById.value,
    [String(nextId)]: meta
  }
  virtualColorIdByName.value = {
    ...virtualColorIdByName.value,
    [normalized]: nextId
  }

  return nextId
}

// ─── Variant Builder: multi-select chip state ────────────────────────────────
const selectedColors = ref([])
const selectedSizes = ref([])
const colorImages = ref({})
const colorImageToCardKey = ref({})
const draggedColorIndex = ref(null)
const dragOverColorIndex = ref(null)
const bulkPanel = ref({ colorId: null, soLuong: '', giaBan: '' })

// ─── Color combobox state ────────────────────────────────────────────────────
const colorSearchQuery = ref("")
const colorDropdownOpen = ref(false)
const colorComboboxRef = ref(null)

// ─── Size combobox state ─────────────────────────────────────────────────────
const sizeSearchQuery = ref("")
const sizeDropdownOpen = ref(false)
const sizeComboboxRef = ref(null)
const extraImageUploadRef = ref(null)

const toColorStorageKey = (colorLike, index = 0) => {
  if (colorLike && typeof colorLike === "object") {
    const numericId = Number(colorLike.id)
    if (Number.isFinite(numericId) && numericId > 0) return String(numericId)
    const byName = normalizeName(colorLike.name || "")
    return `virtual:${byName || index}`
  }

  const numeric = Number(colorLike)
  if (Number.isFinite(numeric) && numeric > 0) return String(numeric)
  return `virtual:${normalizeName(String(colorLike || "")) || index}`
}

const toColorInputId = (colorLike, index = 0) =>
  `color-img-${toColorStorageKey(colorLike, index).replace(/[^a-z0-9_-]/gi, "-")}`

const builderColorOptions = computed(() => {
  if (forceColorlessMode.value) {
    return dedupeColorOptionsByName(colorOptions.value)
  }

  const base = dedupeColorOptionsByName(colorOptions.value)
  const seen = new Set(base.map((color) => normalizeName(color?.name || "")).filter(Boolean))
  const palette = folderColorPaletteByName(form.name)

  if (palette.length > 0) {
    palette.forEach((colorName, index) => {
      const normalized = normalizeName(colorName)
      if (normalized && seen.has(normalized)) return

      const found = findOptionByName(colorOptions.value, colorName)
      if (found) {
        base.push(found)
      } else {
        base.push({
          id: `virtual-builder-${normalized || index}`,
          name: colorName,
          hex: colorHexByName(colorName),
          isVirtual: true
        })
      }

      if (normalized) seen.add(normalized)
    })
  }

  return base
})

const selectedColorTags = computed(() => {
  return builderColorOptions.value.filter((c) => isColorChipActive(c))
})

const filteredBuilderColorOptions = computed(() => {
  const q = String(colorSearchQuery.value || "").trim().toLowerCase()
  if (!q) return builderColorOptions.value
  return builderColorOptions.value.filter((c) => {
    const name = String(c?.name || "").toLowerCase()
    const code = String(c?.code || c?.ma || "").toLowerCase()
    return name.includes(q) || code.includes(q)
  })
})

const filteredSizeOptions = computed(() => {
  const q = String(sizeSearchQuery.value || "").trim().toLowerCase()
  if (!q) return displaySizeOptions.value
  return displaySizeOptions.value.filter((s) => {
    return String(s?.name || "").toLowerCase().includes(q)
  })
})

const colorImageDisplayColors = computed(() => {
  if (forceColorlessMode.value) {
    return []
  }

  const palette = folderColorPaletteByName(form.name)

  if (palette.length > 1) {
    const base = []
    const seen = new Set()

    palette.forEach((colorName, index) => {
      const normalized = normalizeName(colorName)
      if (normalized && seen.has(normalized)) return

      const found =
        findOptionByName(selectedColors.value, colorName) ||
        findOptionByName(colorOptions.value, colorName)

      if (found) {
        base.push(found)
      } else {
        base.push({
          id: `virtual-${normalized || index}`,
          name: colorName,
          hex: colorHexByName(colorName),
          isVirtual: true
        })
      }

      if (normalized) seen.add(normalized)
    })

    return base
  }

  const base = Array.isArray(selectedColors.value) ? [...selectedColors.value] : []
  const seen = new Set(base.map((color) => normalizeName(color?.name || "")).filter(Boolean))

  if (palette.length > 0) {
    palette.forEach((colorName, index) => {
      const normalized = normalizeName(colorName)
      if (normalized && seen.has(normalized)) return

      const found =
        findOptionByName(selectedColors.value, colorName) ||
        findOptionByName(colorOptions.value, colorName)

      if (found) {
        base.push(found)
      } else {
        base.push({
          id: `virtual-${normalized || index}`,
          name: colorName,
          hex: colorHexByName(colorName),
          isVirtual: true
        })
      }

      if (normalized) seen.add(normalized)
    })
  }

  return base
})

const visibleColorImageDisplayColors = computed(() => {
  return colorImageDisplayColors.value
})

const builderSelectedColorCount = computed(() => {
  const variantColorNames = new Set(
    variantRows.value
      .map((row) => normalizeName(getColorMeta(row?.colorId)?.name || ""))
      .filter(Boolean)
  )

  if (variantColorNames.size) return variantColorNames.size

  const used = new Set(
    selectedColors.value
      .map((color) => normalizeName(color?.name || ""))
      .filter(Boolean)
  )

  return used.size
})

const activeBuilderColorNames = computed(() => {
  const fromVariants = new Set(
    variantRows.value
      .map((row) => normalizeName(getColorMeta(row?.colorId)?.name || ""))
      .filter(Boolean)
  )

  if (fromVariants.size) return fromVariants

  return new Set(
    selectedColors.value
      .map((color) => normalizeName(color?.name || ""))
      .filter(Boolean)
  )
})

const isColorChipActive = (color) => {
  const normalized = normalizeName(color?.name || "")
  if (!normalized) return false
  return activeBuilderColorNames.value.has(normalized)
}

function handleBuilderColorChipClick(color) {
  if (color?.isVirtual) {
    window.toast.info(`Màu ${color.name} chưa có trong danh mục màu, chỉ hiển thị theo ảnh thư mục`)
    return
  }
  toggleColorSelect(color)
}

function selectColorFromDropdown(color) {
  handleBuilderColorChipClick(color)
  colorDropdownOpen.value = false
  colorSearchQuery.value = ""
}

function removeColorFromCombobox(color) {
  toggleColorSelect(color)
}

function selectSizeFromDropdown(size) {
  toggleSizeSelect(size)
  sizeDropdownOpen.value = false
  sizeSearchQuery.value = ""
}

function removeSizeFromCombobox(size) {
  toggleSizeSelect(size)
}

const pairKeyForVariant = (sizeId, colorId) => `${Number(sizeId) || 0}-${Number(colorId) || 0}`

const hasDuplicateVariantPair = (targetRow, nextSizeId, nextColorId) => {
  const nextKey = pairKeyForVariant(nextSizeId, nextColorId)
  return variantRows.value.some((row) => {
    if (row === targetRow) return false
    return pairKeyForVariant(row?.sizeId, row?.colorId) === nextKey
  })
}

function preserveColorImageOnColorChange(previousColorId, nextColorId) {
  const fromId = Number(previousColorId)
  const toId = Number(nextColorId)
  if (!Number.isFinite(fromId) || fromId <= 0) return
  if (!Number.isFinite(toId) || toId <= 0) return
  if (fromId === toId) return

  const fromKey = toColorStorageKey(fromId, 0)
  const toKey = toColorStorageKey(toId, 0)
  if (!fromKey || !toKey || fromKey === toKey) return

  const fromPreview = String(colorImages.value[fromKey]?.previewUrl || "").trim()
  const fromCardKey = String(colorImageToCardKey.value[fromKey] || "").trim()
  const hasToPreview = String(colorImages.value[toKey]?.previewUrl || "").trim().length > 0
  const hasToCardKey = String(colorImageToCardKey.value[toKey] || "").trim().length > 0

  if (!hasToPreview && fromPreview) {
    colorImages.value = {
      ...colorImages.value,
      [toKey]: { previewUrl: fromPreview }
    }
  }

  if (!hasToCardKey && fromCardKey) {
    colorImageToCardKey.value = {
      ...colorImageToCardKey.value,
      [toKey]: fromCardKey
    }
  }

  // Move mapping from old color key to new key to avoid transient duplicate mappings
  // that can cause visual jump in the image card area.
  if (!hasToPreview && fromPreview) {
    const nextImages = { ...colorImages.value }
    delete nextImages[fromKey]
    nextImages[toKey] = { previewUrl: fromPreview }
    colorImages.value = nextImages
  }

  if (!hasToCardKey && fromCardKey) {
    const nextMap = { ...colorImageToCardKey.value }
    delete nextMap[fromKey]
    nextMap[toKey] = fromCardKey
    colorImageToCardKey.value = nextMap
  }
}

function handleVariantSizeChange(variant, event) {
  const raw = String(event?.target?.value ?? "").trim()
  const nextSizeId = raw ? Number(raw) : null
  const currentSizeId = Number(variant?.sizeId) || null
  const currentColorId = Number(variant?.colorId) || 0

  if (hasDuplicateVariantPair(variant, nextSizeId, currentColorId)) {
    window.toast?.warning?.("Tổ hợp màu + kích cỡ đã tồn tại")
    if (event?.target) {
      event.target.value = currentSizeId == null ? "" : String(currentSizeId)
    }
    return
  }

  variant.sizeId = nextSizeId
}

function handleVariantColorChange(variant, event) {
  const raw = String(event?.target?.value ?? "").trim()
  const nextColorId = raw ? Number(raw) : null
  const currentSizeId = Number(variant?.sizeId) || 0
  const previousColorId = Number(variant?.colorId) || null

  if (hasDuplicateVariantPair(variant, currentSizeId, nextColorId)) {
    window.toast?.warning?.("Tổ hợp màu + kích cỡ đã tồn tại")
    if (event?.target) {
      event.target.value = previousColorId == null ? "" : String(previousColorId)
    }
    return
  }

  preserveColorImageOnColorChange(previousColorId, nextColorId)

  variant.colorId = nextColorId
  syncSelectedColorsFromVariants()
}

const groupedVariants = computed(() => {
  const map = {}
  for (const row of variantRows.value) {
    const meta = row?.colorId == null ? null : getColorMeta(row?.colorId)
    const normalizedColorName = normalizeName(meta?.name || "")
    const groupKey = normalizedColorName || '__none__'

    if (!map[groupKey]) {
      map[groupKey] = {
        colorId: row?.colorId ?? null,
        colorName: meta?.name || '— Chưa phân màu —',
        colorHex: meta?.hex || '#94a3b8',
        items: []
      }
    }

    if ((map[groupKey].colorId == null || map[groupKey].colorId === "") && row?.colorId != null) {
      map[groupKey].colorId = row.colorId
    }

    map[groupKey].items.push(row)
  }
  return Object.values(map)
})

async function toggleColorSelect(color) {
  const normalizedColorName = normalizeName(color?.name || "")
  const idx = selectedColors.value.findIndex(
    (c) => normalizeName(c?.name || "") === normalizedColorName
  )
  const matchingRows = variantRows.value.filter((row) => {
    const rowColorName = normalizeName(getColorMeta(row?.colorId)?.name || "")
    return rowColorName && rowColorName === normalizedColorName
  })

  if (idx >= 0 && matchingRows.length) {
    const confirmed = typeof window?.confirmDialog === "function"
      ? await window.confirmDialog(
        `Bỏ chọn màu "${color.name}" sẽ xoá ${matchingRows.length} biến thể liên quan. Tiếp tục?`,
        { confirmText: 'Xoá', cancelText: 'Huỷ' }
      )
      : window.confirm(`Bỏ chọn màu "${color.name}" sẽ xoá ${matchingRows.length} biến thể liên quan. Tiếp tục?`)
    if (!confirmed) return
    for (const row of matchingRows) {
      if (Number(row?.id) > 0) {
        const existing = removedVariantRows.value.some((item) => Number(item?.id) === Number(row.id))
        if (!existing) {
          removedVariantRows.value = [...removedVariantRows.value, { ...row }]
        }
      }
    }
    const removeIds = new Set(matchingRows.map(r => r.key))
    variantRows.value = variantRows.value.filter(r => !removeIds.has(r.key))
    selectedColors.value.splice(idx, 1)
    window.toast.success(`Đã xoá ${matchingRows.length} biến thể của màu ${color.name}`)
    return
  }

  if (idx >= 0) {
    selectedColors.value.splice(idx, 1)
  } else {
    selectedColors.value.push(color)
    // Auto-create variant rows for all selected sizes with the new color
    if (selectedSizes.value.length) {
      const existingPairs = new Set(
        variantRows.value.map((row) => `${Number(row.sizeId)}-${Number(row.colorId) || 0}`)
      )
      let created = 0
      for (const size of selectedSizes.value) {
        const pairKey = `${Number(size.id)}-${Number(color.id)}`
        if (existingPairs.has(pairKey)) continue
        existingPairs.add(pairKey)
        addVariantRow({ sizeId: Number(size.id), colorId: Number(color.id) })
        created++
      }
      if (created) window.toast.success(`Đã tạo ${created} biến thể cho màu ${color.name}`)
    }
    sortVariantRows()
  }
}

async function toggleSizeSelect(size) {
  const idx = selectedSizes.value.findIndex((s) => s.id === size.id)
  const matchingRows = variantRows.value.filter((row) => Number(row.sizeId) === Number(size.id))
  if (idx >= 0 && matchingRows.length) {
    const confirmed = typeof window?.confirmDialog === "function"
      ? await window.confirmDialog(
        `Bỏ chọn kích cỡ "${size.name}" sẽ xoá ${matchingRows.length} biến thể liên quan. Tiếp tục?`,
        { confirmText: 'Xoá', cancelText: 'Huỷ' }
      )
      : window.confirm(`Bỏ chọn kích cỡ "${size.name}" sẽ xoá ${matchingRows.length} biến thể liên quan. Tiếp tục?`)
    if (!confirmed) return
    const removeIds = new Set(matchingRows.map(r => r.key))
    variantRows.value = variantRows.value.filter(r => !removeIds.has(r.key))
    selectedSizes.value.splice(idx, 1)
    window.toast.success(`Đã xoá ${matchingRows.length} biến thể của size ${size.name}`)
    return
  }
  if (idx >= 0) {
    selectedSizes.value.splice(idx, 1)
  } else {
    selectedSizes.value.push(size)
    // Auto-create variant rows for all selected colors with the new size
    const colorsToUse = selectedColors.value.length ? selectedColors.value : [null]
    const existingPairs = new Set(
      variantRows.value.map((row) => `${Number(row.sizeId)}-${Number(row.colorId) || 0}`)
    )
    let created = 0
    for (const color of colorsToUse) {
      const colorId = color ? Number(color.id) : 0
      const pairKey = `${Number(size.id)}-${colorId}`
      if (existingPairs.has(pairKey)) continue
      existingPairs.add(pairKey)
      addVariantRow({ sizeId: Number(size.id), colorId: color ? Number(color.id) : null })
      created++
    }
    if (created) window.toast.success(`Đã tạo ${created} biến thể cho size ${size.name}`)
    sortVariantRows()
  }
}

function generateVariantsAuto() {
  if (!selectedSizes.value.length) {
    window.toast.warning('Vui lòng chọn ít nhất 1 kích cỡ trước khi tạo biến thể')
    return
  }
  const existingPairs = new Set(
    variantRows.value.map((row) => `${Number(row.sizeId)}-${Number(row.colorId) || 0}`)
  )
  let created = 0

  if (selectedColors.value.length) {
    for (const color of selectedColors.value) {
      for (const size of selectedSizes.value) {
        const key = `${Number(size.id)}-${Number(color.id)}`
        if (existingPairs.has(key)) continue
        existingPairs.add(key)
        addVariantRow({ sizeId: Number(size.id), colorId: Number(color.id) })
        created++
      }
    }
  } else {
    for (const size of selectedSizes.value) {
      const key = `${Number(size.id)}-0`
      if (existingPairs.has(key)) continue
      existingPairs.add(key)
      addVariantRow({ sizeId: Number(size.id), colorId: null })
      created++
    }
  }

  if (created) window.toast.success(`Đã tạo ${created} biến thể mới`)
  else window.toast.info('Không có biến thể mới (các tổ hợp đã tồn tại)')
  sortVariantRows()
}

function addVariantRowForColor(colorId) {
  addVariantRow({ colorId: Number(colorId) })
}

function openBulkPanel(colorId) {
  bulkPanel.value = { colorId, soLuong: '', giaBan: '' }
}

const parseCurrencyInput = (value) => {
  const digits = String(value ?? '').replace(/[^\d]/g, '')
  if (!digits) return ''
  return Number(digits)
}

const formatCurrencyInput = (value) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed < 0) return ''
  return new Intl.NumberFormat('vi-VN').format(parsed)
}

const onBulkPriceInput = (event) => {
  bulkPanel.value.giaBan = parseCurrencyInput(event?.target?.value)
}

const onVariantPriceInput = (variant, event) => {
  if (!variant) return
  variant.giaBan = parseCurrencyInput(event?.target?.value)
}

function closeBulkPanel() {
  bulkPanel.value.colorId = null
}

function applyBulkToGroup() {
  const { colorId, soLuong, giaBan } = bulkPanel.value
  const targetColorName = normalizeName(getColorMeta(colorId)?.name || "")
  let applied = 0

  for (const row of variantRows.value) {
    const rowColorName = normalizeName(getColorMeta(row?.colorId)?.name || "")
    const sameColor = targetColorName
      ? rowColorName === targetColorName
      : String(row.colorId) === String(colorId)

    if (sameColor) {
      if (String(soLuong).trim() !== '') row.soLuong = Number(soLuong)
      if (String(giaBan).trim() !== '') row.giaBan = Number(giaBan)
      applied++
    }
  }
  if (applied) window.toast.success(`Đã áp dụng cho ${applied} biến thể`)
  closeBulkPanel()
}

const handleColorImageChange = async (colorLike, event, index = 0) => {
  const file = event?.target?.files?.[0]
  if (!file?.type?.startsWith('image/')) return
  const dataUrl = await readImageFile(file)
  if (dataUrl) {
    const key = toColorStorageKey(colorLike, index)
    const colorLabel = typeof colorLike === "object"
      ? String(colorLike?.name || "").trim()
      : getColorLabel(colorLike)

    colorImages.value = { ...colorImages.value, [key]: { previewUrl: dataUrl } }

    const mappedKey = colorImageToCardKey.value[key]
    const foundIndex = imageCards.value.findIndex((item) => item.key === mappedKey)
    if (foundIndex >= 0) {
      imageCards.value[foundIndex].src = dataUrl
    } else {
      const card = createImageCard(dataUrl)
      imageCards.value.push(card)
      colorImageToCardKey.value = { ...colorImageToCardKey.value, [key]: card.key }
    }

    window.toast.success(`Đã thêm ảnh cho màu ${colorLabel || "đã chọn"}`)
  }
  event.target.value = ''
}

const handleGroupColorImageChange = async (colorLike, event, index = 0) => {
  await handleColorImageChange(colorLike, event, index)
}

const handleColorCardDragStart = (index, event) => {
  draggedColorIndex.value = index
  dragOverColorIndex.value = index
  if (event?.dataTransfer) {
    event.dataTransfer.effectAllowed = "move"
    event.dataTransfer.setData("text/plain", String(index))
  }
}

const handleColorCardDragOver = (index, event) => {
  if (event) event.preventDefault()
  dragOverColorIndex.value = index
}

const handleColorCardDrop = (dropIndex, event) => {
  if (event) event.preventDefault()
  const from = Number(draggedColorIndex.value)
  const to = Number(dropIndex)
  if (!Number.isFinite(from) || !Number.isFinite(to) || from === to) {
    draggedColorIndex.value = null
    dragOverColorIndex.value = null
    return
  }
  const rows = [...selectedColors.value]
  const [picked] = rows.splice(from, 1)
  rows.splice(to, 0, picked)
  selectedColors.value = rows
  draggedColorIndex.value = null
  dragOverColorIndex.value = null
}

const handleColorCardDragEnd = () => {
  draggedColorIndex.value = null
  dragOverColorIndex.value = null
}

const removeColorImage = (colorLike, index = 0) => {
  const key = toColorStorageKey(colorLike, index)
  const mappedCardKey = colorImageToCardKey.value[key]
  colorImages.value = { ...colorImages.value, [key]: { previewUrl: "" } }
  if (mappedCardKey) {
    imageCards.value = imageCards.value.filter((item) => item.key !== mappedCardKey)
  }
  const nextMapping = { ...colorImageToCardKey.value }
  delete nextMapping[key]
  colorImageToCardKey.value = nextMapping
}

const hydrateColorImagesFromGallery = (images = [], productLike = {}) => {
  const nextCards = []
  const nextMapping = {}
  const nextColorImages = {}
  const seenSrcs = new Set()

  colorImageDisplayColors.value.forEach((color, index) => {
    const src = resolveFallbackColorImage(color, index, productLike, images)
    if (!src) return
    // Avoid showing identical fallback images for different colors
    const isDuplicate = seenSrcs.has(src)
    seenSrcs.add(src)
    const finalSrc = isDuplicate ? '' : src
    const card = createImageCard(finalSrc)
    const key = toColorStorageKey(color, index)
    nextCards.push(card)
    nextMapping[key] = card.key
    if (finalSrc) {
      nextColorImages[key] = { previewUrl: finalSrc }
    }
  })

  imageCards.value = nextCards.length ? nextCards : []
  colorImageToCardKey.value = nextMapping
  colorImages.value = nextColorImages
}

const hydrateColorImagesFromConfig = (entries = [], fallbackImages = [], productLike = {}) => {
  const normalizedEntries = (Array.isArray(entries) ? entries : [])
    .map((entry) => ({
      colorId: Number(entry?.colorId),
      image: String(entry?.image || "").trim(),
      order: Number.isFinite(Number(entry?.order)) ? Number(entry.order) : 0
    }))
    .filter((entry) => Number.isFinite(entry.colorId) && entry.colorId > 0 && entry.image)
    .sort((left, right) => left.order - right.order)

  if (!normalizedEntries.length) {
    hydrateColorImagesFromGallery(fallbackImages, productLike)
    return
  }

  const normalizedByColorId = new Map(
    normalizedEntries.map((entry) => [Number(entry.colorId), entry.image])
  )

  const duplicateConfiguredImages = new Set()
  const configuredImageCounts = new Map()
  normalizedEntries.forEach((entry) => {
    const image = String(entry.image || "").trim()
    if (!image) return
    configuredImageCounts.set(image, Number(configuredImageCounts.get(image) || 0) + 1)
  })
  configuredImageCounts.forEach((count, image) => {
    if (count > 1) duplicateConfiguredImages.add(image)
  })

  const hasFolderPalette =
    folderColorPaletteByName(productLike?.tenSanPham || form.name).length > 1 ||
    getStaticColorCandidates(Number(productLike?.id || id || route.params?.id || 0), productLike).length > 1

  // Build set of images that are explicitly saved for specific colors
  const configuredImageValues = new Set(
    normalizedEntries.map((e) => e.image).filter(Boolean)
  )

  const nextCards = []
  const nextMapping = {}
  const nextColorImages = {}
  const seenSrcs = new Set()

  // Process colors WITH configured images first, then colors without,
  // so fallback logic never steals a configured image from another color.
  const displayColors = colorImageDisplayColors.value.map((color, index) => ({ color, index }))
  const configuredFirst = [
    ...displayColors.filter(({ color }) => {
      const cid = Number(color?.id)
      return Number.isFinite(cid) && cid > 0 && normalizedByColorId.has(cid)
    }),
    ...displayColors.filter(({ color }) => {
      const cid = Number(color?.id)
      return !(Number.isFinite(cid) && cid > 0 && normalizedByColorId.has(cid))
    })
  ]

  configuredFirst.forEach(({ color, index }) => {
    const colorId = Number(color?.id)
    const configuredImage = Number.isFinite(colorId) && colorId > 0
      ? String(normalizedByColorId.get(colorId) || "").trim()
      : ""

    const isConfigured = !!configuredImage

    const shouldUseFallbackInstead =
      hasFolderPalette && configuredImage && duplicateConfiguredImages.has(configuredImage)

    const src = String(
      resolveConfiguredColorImage(
        shouldUseFallbackInstead ? "" : configuredImage,
        color,
        index,
        productLike,
        fallbackImages
      )
    ).trim()
    if (!src) return

    // Skip fallback images that are the configured image of another color
    if (!isConfigured && configuredImageValues.has(src)) return

    // Avoid showing identical fallback images for different colors
    // but never dedup a color's own configured image
    const isDuplicate = !isConfigured && seenSrcs.has(src)
    seenSrcs.add(src)
    const finalSrc = isDuplicate ? '' : src
    const card = createImageCard(finalSrc)
    const key = toColorStorageKey(color, index)
    nextCards.push(card)
    nextMapping[key] = card.key
    if (finalSrc) {
      nextColorImages[key] = { previewUrl: finalSrc }
    }
  })

  if (!nextCards.length) {
    normalizedEntries.forEach((entry) => {
      const src = String(entry.image || "").trim()
      if (!src) return
      const card = createImageCard(src)
      nextCards.push(card)
      nextMapping[String(entry.colorId)] = card.key
      nextColorImages[String(entry.colorId)] = { previewUrl: src }
    })
  }

  imageCards.value = nextCards.length ? nextCards : []
  colorImageToCardKey.value = nextMapping
  colorImages.value = nextColorImages
}

const syncSelectedColorsFromVariants = () => {
  const colorIds = [...new Set(
    variantRows.value
      .map((row) => Number(row?.colorId))
      .filter((cid) => Number.isFinite(cid) && cid > 0)
  )]

  const selected = colorOptions.value.filter((color) => colorIds.includes(Number(color.id)))

  // Preserve saved color order from image config if available
  const imageConfig = id
    ? getProductImageConfig({ id, maSanPham: form.sku })
    : { colorImages: [] }
  const savedOrder = new Map(
    (Array.isArray(imageConfig?.colorImages) ? imageConfig.colorImages : [])
      .map((entry) => [Number(entry?.colorId), Number(entry?.order ?? 999)])
  )

  const sorted = [...selected].sort((a, b) => {
    const orderA = savedOrder.get(Number(a.id)) ?? 999
    const orderB = savedOrder.get(Number(b.id)) ?? 999
    return orderA - orderB
  })

  selectedColors.value = dedupeColorOptionsByName(sorted)
}

const ensureVariantRowsCoverDisplayColors = () => {
  if (forceColorlessMode.value) return
  if (!variantRows.value.length) return

  const displayColors = dedupeColorOptionsByName(colorImageDisplayColors.value)

  if (displayColors.length <= 1) return

  const existingColorNames = new Set(
    variantRows.value
      .map((row) => normalizeName(getColorMeta(row?.colorId)?.name || ""))
      .filter(Boolean)
  )

  if (existingColorNames.size >= displayColors.length) return

  const missingColors = displayColors.filter((color) => {
    const normalized = normalizeName(color?.name || "")
    return normalized && !existingColorNames.has(normalized)
  })

  if (!missingColors.length) return

  const templateRowsBySize = new Map()
  for (const row of variantRows.value) {
    const sizeId = Number(row?.sizeId)
    if (!Number.isFinite(sizeId) || sizeId <= 0) continue
    if (!templateRowsBySize.has(sizeId)) {
      templateRowsBySize.set(sizeId, row)
    }
  }

  const sizeIds = [...templateRowsBySize.keys()]
  if (!sizeIds.length) return

  const existingPairs = new Set(
    variantRows.value
      .map((row) => {
        const sizeId = Number(row?.sizeId)
        const colorName = normalizeName(getColorMeta(row?.colorId)?.name || "")
        if (!Number.isFinite(sizeId) || sizeId <= 0 || !colorName) return ""
        return `${sizeId}:${colorName}`
      })
      .filter(Boolean)
  )

  const newRows = []

  for (const color of missingColors) {
    const numericColorId = Number(color?.id)
    const colorId = Number.isFinite(numericColorId) && numericColorId > 0
      ? numericColorId
      : resolveOrCreateVirtualColorId(color)
    const colorName = normalizeName(color?.name || "")
    if (!Number.isFinite(Number(colorId)) || !colorName) continue

    for (const sizeId of sizeIds) {
      const pairKey = `${sizeId}:${colorName}`
      if (existingPairs.has(pairKey)) continue
      existingPairs.add(pairKey)

      const template = templateRowsBySize.get(sizeId) || variantRows.value[0] || createEmptyVariantRow()
      newRows.push({
        ...createEmptyVariantRow(),
        sizeId,
        colorId,
        giaBan: Number(template?.giaBan || form.giaBan || 0),
        soLuong: 0,
        trangThai: template?.trangThai || "Hoạt động"
      })
    }
  }

  if (!newRows.length) return

  variantRows.value = [...variantRows.value, ...newRows]
}

const syncSelectedSizesFromVariants = () => {
  const sizeIds = [...new Set(
    variantRows.value
      .map((row) => Number(row.sizeId))
      .filter((sid) => Number.isFinite(sid) && sid > 0)
  )]

  selectedSizes.value = sizeOptions.value.filter((size) => sizeIds.includes(Number(size.id)))
}

const clearAllVariants = () => {
  for (const row of variantRows.value) {
    if (Number(row?.id) > 0) {
      const existing = removedVariantRows.value.some((item) => Number(item?.id) === Number(row.id))
      if (!existing) {
        removedVariantRows.value = [...removedVariantRows.value, { ...row }]
      }
    }
  }
  variantRows.value = []
  selectedColors.value = []
  selectedSizes.value = []
  colorImages.value = {}
  colorImageToCardKey.value = {}
  virtualColorMetaById.value = {}
  virtualColorIdByName.value = {}
  virtualColorCounter.value = 0
  imageCards.value = []
}

async function generateProductCode() {
  try {
    const res = await getAllSanPham()
    const rows = Array.isArray(res?.data) ? res.data : []
    const maxCode = rows.reduce((max, item) => {
      const code = String(item?.maSanPham || "")
      const matched = code.match(/^SP(\d+)$/i)
      if (!matched) return max
      return Math.max(max, Number(matched[1] || 0))
    }, 0)
    return `SP${String(maxCode + 1).padStart(3, "0")}`
  } catch {
    return `SP${String(Date.now()).slice(-6)}`
  }
}

const normalizeErrorText = (value = "") => String(value || "")
  .normalize("NFD")
  .replace(/[\u0300-\u036f]/g, "")
  .replace(/đ/g, "d")
  .replace(/Đ/g, "D")
  .toLowerCase()
  .trim()

const isDuplicateProductCodeError = (error) => {
  const message = normalizeErrorText(error?.response?.data?.message || error?.message || "")
  if (!message) return false
  return message.includes("ma san pham da ton tai")
    || message.includes("duplicate")
    || message.includes("already exists")
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function createSanPhamWithRetry(payload, maxAttempts = 3) {
  let attempt = 0
  let lastError = null

  while (attempt < maxAttempts) {
    try {
      return await createSanPham(payload)
    } catch (error) {
      lastError = error
      if (!isDuplicateProductCodeError(error)) throw error

      attempt += 1
      if (attempt >= maxAttempts) break

      const refreshedCode = await generateProductCode()
      form.sku = refreshedCode
      payload.maSanPham = refreshedCode
      await sleep(200)
    }
  }

  throw lastError || new Error("Không thể tạo sản phẩm")
}

function extractSkuNumber(value = "") {
  const matched = String(value).trim().match(/^SP(\d+)$/i)
  if (!matched) return null
  const numeric = Number(matched[1])
  return Number.isFinite(numeric) ? numeric : null
}

function formatSku(num) {
  return `SP${String(num).padStart(3, "0")}`
}

function alphaSuffixFromIndex(index = 0) {
  if (!Number.isFinite(index) || index <= 0) return ""
  let n = Math.floor(index)
  let out = ""
  while (n > 0) {
    n -= 1
    out = String.fromCharCode(65 + (n % 26)) + out
    n = Math.floor(n / 26)
  }
  return out
}

function nextAvailableVariantCode(baseCode, usedCodes = new Set()) {
  for (let i = 0; i < 9999; i += 1) {
    const candidate = `${baseCode}${alphaSuffixFromIndex(i)}`
    if (!usedCodes.has(candidate)) {
      usedCodes.add(candidate)
      return candidate
    }
  }
  return `${baseCode}${Date.now()}`
}

const colorImagePayloads = computed(() => {
  return colorImageDisplayColors.value
    .map((color, index) => {
      const colorId = Number(color?.id)
      const image = resolveColorDisplayImage(color, index)
      return {
        colorId,
        image: String(image || "").trim(),
        order: index
      }
    })
    .filter((entry) => Number.isFinite(entry.colorId) && entry.colorId > 0 && entry.image)
})

function resolveColorDisplayImage(colorLike, index = 0) {
  const key = toColorStorageKey(colorLike, index)
  const mappedCardKey = colorImageToCardKey.value[key]
  const mappedCard = imageCards.value.find((item) => item.key === mappedCardKey)
  return String(colorImages.value[key]?.previewUrl || mappedCard?.src || "").trim()
}

function syncDraftImageConfigToStorage() {
  if (!imageConfigSyncReady.value) return

  const sku = String(form.sku || '').trim()
  const imageOwner = { id: id || null, maSanPham: sku }
  if (!sku && !id) return

  const orderedImages = prioritizeImagesForSave(allImageValues.value)
  const overrideImages = shouldStripFallbackImagesForStorage(imageOwner)
    ? orderedImages.filter((item) => !fallbackImageSet.has(item))
    : orderedImages
  const hasImageConfig = overrideImages.length > 0 || colorImagePayloads.value.length > 0

  if (hasImageConfig) {
    setProductImageConfig(imageOwner, { images: overrideImages, colorImages: colorImagePayloads.value })
  } else {
    clearProductImageOverride(imageOwner)
  }
}

watch(
  [() => form.sku, allImageValues, colorImagePayloads],
  () => {
    syncDraftImageConfigToStorage()
  },
  { deep: true }
)

function createVariantPayloads() {
  const parsedLoaiId = Number(form.loaiId)
  const rows = variantRows.value.filter((row) => !getColorMeta(row?.colorId)?.isVirtual)

  const skuNum = String(form.sku || "").replace(/\D/g, "")
  const baseCode = skuNum ? `SPCT${skuNum.padStart(3, "0")}` : "SPCT"

  const usedCodes = new Set(
    [
      ...(Array.isArray(knownVariantCodes.value) ? knownVariantCodes.value : []),
      ...rows.map((row) => String(row?.ma || "").trim().toUpperCase()),
      ...removedVariantRows.value.map((row) => String(row?.ma || "").trim().toUpperCase())
    ].filter(Boolean)
  )

  const activePayloads = rows.map((row) => {
    const currentCode = String(row?.ma || "").trim().toUpperCase()
    const resolvedCode = currentCode || nextAvailableVariantCode(baseCode, usedCodes)
    return {
      id: row.id,
      ma: resolvedCode,
      giaNhap: 0,
      giaBan: Number(row.giaBan || 0),
      soLuong: Number(row.soLuong || 0),
      chatLieu: { id: 1 },
      mauSac: { id: Number(row.colorId) },
      kichThuoc: { id: Number(row.sizeId) },
      hang: { id: 1 },
      xuatSu: { id: 1 },
      danhMuc: { id: 1 },
      loai: { id: parsedLoaiId },
      trangThai: row.trangThai || "Hoạt động"
    }
  })

  const activeIds = new Set(
    rows
      .map((row) => Number(row?.id))
      .filter((id) => Number.isFinite(id) && id > 0)
  )

  const inactivePayloads = removedVariantRows.value
    .filter((row) => {
      const rowId = Number(row?.id)
      if (!Number.isFinite(rowId) || rowId <= 0) return false
      if (activeIds.has(rowId)) return false
      const colorId = Number(row?.colorId)
      const sizeId = Number(row?.sizeId)
      return Number.isFinite(colorId) && colorId > 0 && Number.isFinite(sizeId) && sizeId > 0
    })
    .map((row) => {
      const currentCode = String(row?.ma || "").trim().toUpperCase()
      const resolvedCode = currentCode || nextAvailableVariantCode(baseCode, usedCodes)
      return {
        id: row.id,
        ma: resolvedCode,
        giaNhap: 0,
        giaBan: Number(row.giaBan || 0),
        soLuong: Number(row.soLuong || 0),
        chatLieu: { id: 1 },
        mauSac: { id: Number(row.colorId) },
        kichThuoc: { id: Number(row.sizeId) },
        hang: { id: 1 },
        xuatSu: { id: 1 },
        danhMuc: { id: 1 },
        loai: { id: parsedLoaiId },
        trangThai: "Ngừng hoạt động"
      }
    })

  return [...activePayloads, ...inactivePayloads]
}

function validateVariants() {
  if (!variantRows.value.length) return "Sản phẩm phải có ít nhất 1 biến thể"

  const pairSet = new Set()
  for (const row of variantRows.value) {
    const sizeId = Number(row.sizeId)
    const colorId = Number(row.colorId) || 0
    const giaBan = Number(row.giaBan)
    const soLuong = Number(row.soLuong)

    if (!Number.isFinite(sizeId) || sizeId <= 0) return "Vui lòng chọn size cho tất cả biến thể"
    if (!Number.isFinite(giaBan) || giaBan <= 0) return "Giá bán biến thể phải lớn hơn 0"
    if (!Number.isFinite(soLuong) || soLuong < 0) return "Tồn kho biến thể không hợp lệ"

    const pairKey = `${sizeId}-${colorId}`
    if (pairSet.has(pairKey)) {
      const sizeName = getSizeLabel(sizeId) || sizeId
      const colorName = colorId > 0 ? (getColorLabel(colorId) || colorId) : "không màu"
      return `Biến thể size ${sizeName} / màu ${colorName} đang bị trùng`
    }
    pairSet.add(pairKey)
  }

  return ""
}

function validateProduct() {
  if (!String(form.name || "").trim()) return "Tên sản phẩm không được để trống"
  if (!String(form.sku || "").trim()) return "Mã sản phẩm không được để trống"
  if (!/^SP\d+/i.test(String(form.sku || "").trim())) return "Mã sản phẩm phải bắt đầu bằng SP + số (ví dụ: SP001)"
  if (!Number.isFinite(Number(form.loaiId)) || Number(form.loaiId) <= 0) return "Vui lòng chọn loại sản phẩm hợp lệ"

  // Require at least one uploaded image for new products
  if (!id) {
    const uploadedImages = allImageValues.value.filter((img) => !fallbackImageSet.has(img))
    if (!uploadedImages.length) return "Vui lòng tải lên ít nhất 1 ảnh sản phẩm"
  }

  // Require image for every selected color (both create and update)
  if (colorImageDisplayColors.value.length) {
    const missingColorImage = colorImageDisplayColors.value.find((color, index) => {
      return !resolveColorDisplayImage(color, index)
    })
    if (missingColorImage) {
      return `Vui lòng tải ảnh cho màu ${missingColorImage?.name || "đã chọn"}`
    }
  }

  const variantError = validateVariants()
  if (variantError) return variantError

  return ""
}

async function loadLoaiOptions() {
  try {
    const res = await getAllLoai()
    const rows = Array.isArray(res?.data) ? res.data : []
    loaiOptions.value = rows
      .map((item) => ({
        id: Number(item?.id),
        name: String(item?.tenLoai || item?.name || "").trim()
      }))
      .filter((item) => Number.isFinite(item.id) && item.id > 0 && item.name)

    if (!loaiOptions.value.length) {
      loaiOptions.value = [
        { id: 1, name: "Hoodie Jacket" },
        { id: 2, name: "Bomber Jacket" },
        { id: 3, name: "Coach Jacket" }
      ]
    }

    if (!Number.isFinite(Number(form.loaiId)) || Number(form.loaiId) <= 0) {
      form.loaiId = loaiOptions.value[0].id
    }
  } catch {
    loaiOptions.value = [
      { id: 1, name: "Hoodie Jacket" },
      { id: 2, name: "Bomber Jacket" },
      { id: 3, name: "Coach Jacket" }
    ]

    if (!Number.isFinite(Number(form.loaiId)) || Number(form.loaiId) <= 0) {
      form.loaiId = 1
    }
  }
}

async function loadVariantMasterData() {
  try {
    const [sizesRes, colorsRes] = await Promise.all([getAllKichThuoc(), getAllMauSac()])
    const sizes = toList(sizesRes?.data)
    const colors = toList(colorsRes?.data)

    sizeOptions.value = sizes
      .map((item) => ({
        id: Number(item?.id),
        name: String(item?.tenKichThuoc || item?.name || "").trim()
      }))
      .filter((item) => Number.isFinite(item.id) && item.id > 0 && item.name)

    colorOptions.value = colors
      .map((item) => ({
        id: Number(item?.id),
        name: normalizeDisplayColorName(item?.tenMau || item?.tenMauSac || item?.name),
        hex: colorHexByName(item?.hex || item?.tenMau || item?.tenMauSac || item?.name || item?.maMau || item?.ma || ""),
        code: String(item?.maMau || item?.ma || `MS${String(item?.id || 0).padStart(5, "0")}`).trim()
      }))
      .filter((item) => Number.isFinite(item.id) && item.id > 0 && item.name)
  } catch {
    sizeOptions.value = []
    colorOptions.value = []
  }
}

function mapExistingVariants(variants, options = {}) {
  const forceColorless = Boolean(options?.forceColorless)

  originalVariantIds.value = variants
    .map((row) => Number(row?.id))
    .filter((id) => Number.isFinite(id) && id > 0)

  removedVariantRows.value = []

  variantRows.value = variants.map((row) => ({
    key: `${row.id || Date.now()}-${Math.random()}`,
    id: row.id || null,
    ma: String(row?.ma || "").trim(),
    sizeId: Number(row?.kichThuoc?.id || 0) || null,
    colorId: forceColorless ? null : (Number(row?.mauSac?.id || 0) || null),
    giaBan: Number(row?.giaBan || 0),
    soLuong: Number(row?.soLuong || 0),
    trangThai: row?.trangThai || "Hoạt động"
  }))
}

import { computeSoldBySpct as _computeSoldBySpct, variantAvailableStock } from "@/utils/stockCalculation"

const _loadSoldBySpct = async () => {
  soldBySpct.value = await _computeSoldBySpct()
}

function getSoldQty(variant) {
  if (!variant?.id) return 0
  return soldBySpct.value.get(variant.id) || 0
}

function getAvailableStock(variant) {
  if (!variant?.id) return null
  return variantAvailableStock(variant, soldBySpct.value)
}

onMounted(async () => {
  // Click-outside handler for comboboxes
  const handleClickOutside = (e) => {
    if (colorComboboxRef.value && !colorComboboxRef.value.contains(e.target)) {
      colorDropdownOpen.value = false
      colorSearchQuery.value = ""
    }
    if (sizeComboboxRef.value && !sizeComboboxRef.value.contains(e.target)) {
      sizeDropdownOpen.value = false
      sizeSearchQuery.value = ""
    }
  }
  document.addEventListener("click", handleClickOutside)
  onBeforeUnmount(() => document.removeEventListener("click", handleClickOutside))

  await Promise.all([loadLoaiOptions(), loadVariantMasterData()])
  virtualColorMetaById.value = {}
  virtualColorIdByName.value = {}
  virtualColorCounter.value = 0
  imageCards.value = []

  if (!id) {
    form.sku = await generateProductCode()
    return
  }

  const res = await getSanPhamById(id)
  const primaryData = res.data || {}

  let productsInScope = [primaryData]
  const shouldLoadFamilyScope =
    String(route.query?.scope || "").toLowerCase() === "family" &&
    editScopeProductIds.value.length > 1
  if (shouldLoadFamilyScope) {
    try {
      const allRes = await getAllSanPham()
      const allProducts = Array.isArray(allRes?.data) ? allRes.data : []
      const requestedIds = new Set(editScopeProductIds.value)
      const primaryId = Number(primaryData?.id)
      if (Number.isFinite(primaryId) && primaryId > 0) requestedIds.add(primaryId)

      const familyKey = normalizeProductFamilyKey(primaryData?.tenSanPham || "")
      const scopedProducts = allProducts.filter((product) => {
        const productId = Number(product?.id)
        if (Number.isFinite(productId) && productId > 0 && requestedIds.has(productId)) return true
        if (!familyKey) return false
        return normalizeProductFamilyKey(product?.tenSanPham || "") === familyKey
      })

      if (scopedProducts.length) {
        productsInScope = scopedProducts
      }
    } catch {
      productsInScope = [primaryData]
    }
  }

  const data = productsInScope.find((product) => Number(product?.id) === Number(primaryData?.id)) || primaryData
  const allVariantsFromScope = dedupeVariantList(
    productsInScope
      .flatMap((product) => (Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []))
  )

  knownVariantCodes.value = [...new Set(
    allVariantsFromScope
      .map((variant) => String(variant?.ma || variant?.maSanPhamChiTiet || "").trim().toUpperCase())
      .filter(Boolean)
  )]

  const variants = dedupeVariantList(
    allVariantsFromScope.filter((variant) => isVariantActive(variant))
  )
  const colorNameSet = new Set(
    variants
      .map((variant) => normalizeName(
        variant?.mauSac?.tenMau ||
        variant?.mauSac?.tenMauSac ||
        variant?.mauSac?.name ||
        ""
      ))
      .filter(Boolean)
  )
  hasRealColorsFromData.value = colorNameSet.size >= 1
  const firstVariant = variants[0] || null

  form.sku = data.maSanPham || ""
  form.name = data.tenSanPham || ""
  form.description = data.moTa || ""
  form.giaBan = Number(firstVariant?.giaBan || 0)
  form.giaGoc = 0
  form.ton = Number(firstVariant?.soLuong || 0)
  form.loaiId = Number(firstVariant?.loai?.id || loaiOptions.value[0]?.id || 1)
  form.status = data.trangThai === "Hoạt động" ? "Đang bán" : "Ẩn"
  const extractedImages = extractImageList([productsInScope, variants])
  const imageConfig = mergeProductImageConfigs(productsInScope)
  const overrideImages = imageConfig.images
  const resolvedImages = overrideImages.length
    ? overrideImages
    : resolvePreferredGalleryImages(extractedImages, data)

  if (variants.length) {
    mapExistingVariants(variants, { forceColorless: forceColorlessMode.value })

    syncSelectedColorsFromVariants()
    if (forceColorlessMode.value) {
      selectedColors.value = []
      colorImages.value = {}
      colorImageToCardKey.value = {}
      const fallbackImages = resolvedImages.length
        ? resolvedImages
        : [fallbackImageFor(data.id, data.maSanPham, data.tenSanPham)]
      imageCards.value = fallbackImages
        .map((src) => String(src || '').trim())
        .filter(Boolean)
        .map((src) => createImageCard(src))
    } else {
      hydrateColorImagesFromConfig(
        imageConfig.colorImages,
        resolvedImages.length ? resolvedImages : [fallbackImageFor(data.id, data.maSanPham, data.tenSanPham)],
        { id: data.id, maSanPham: data.maSanPham, tenSanPham: data.tenSanPham }
      )
      ensureVariantRowsCoverDisplayColors()
      syncSelectedColorsFromVariants()
    }
    syncSelectedSizesFromVariants()
  } else {
    imageCards.value = [createImageCard(
      resolvedImages[0] || fallbackImageFor(data.id, data.maSanPham, data.tenSanPham)
    )]
  }

  if (id) _loadSoldBySpct()
  imageConfigSyncReady.value = true
  syncDraftImageConfigToStorage()
})

async function saveProduct() {
  if (!id && !String(form.status || "").trim()) {
    form.status = "Đang bán"
  }

  const error = validateProduct()
  if (error) {
    window.toast.warning(error)
    return
  }

  const action = id ? "cập nhật" : "tạo mới"
  const confirmed = typeof window?.confirmDialog === "function"
    ? await window.confirmDialog(`Bạn có chắc chắn muốn ${action} sản phẩm "${form.name}"?`)
    : window.confirm(`Bạn có chắc chắn muốn ${action} sản phẩm "${form.name}"?`)
  if (!confirmed) return

  try {
    const orderedImages = prioritizeImagesForSave(allImageValues.value)
    const imageOwner = { id: id || null, maSanPham: form.sku, sku: form.sku }
    const overrideImages = shouldStripFallbackImagesForStorage(imageOwner)
      ? orderedImages.filter((item) => !fallbackImageSet.has(item))
      : orderedImages
    const hasImageConfig = overrideImages.length > 0 || colorImagePayloads.value.length > 0
    const skippedVirtualVariantCount = variantRows.value.filter((row) => getColorMeta(row?.colorId)?.isVirtual).length
    const currentVariantIds = variantRows.value
      .map((row) => Number(row?.id))
      .filter((rowId) => Number.isFinite(rowId) && rowId > 0)
    const deletedVariantIds = originalVariantIds.value.filter((rowId) => !currentVariantIds.includes(rowId))

    if (skippedVirtualVariantCount > 0) {
      window.toast?.info(`Có ${skippedVirtualVariantCount} màu ảnh chưa có trong bảng màu, hệ thống tạm không lưu các biến thể màu này`)
    }

    const payload = {
      maSanPham: form.sku,
      tenSanPham: form.name,
      moTa: form.description,
      trangThai: form.status === "Đang bán" ? "Hoạt động" : "Ngừng hoạt động",
      anh: orderedImages[0] || "",
      anhChinh: orderedImages[0] || "",
      hinhAnh: orderedImages[0] || "",
      images: orderedImages,
      listAnh: orderedImages,
      mauSacHinhAnhs: colorImagePayloads.value,
      anhTheoMauSac: colorImagePayloads.value,
      colorImages: colorImagePayloads.value,
      sanPhamChiTiets: createVariantPayloads(),
      deletedVariantIds,
      sanPhamChiTietIdsXoa: deletedVariantIds,
      xoaSanPhamChiTietIds: deletedVariantIds
    }

    if (id) {
      await updateSanPham(id, payload)
      if (hasImageConfig) {
        setProductImageConfig({ id, maSanPham: form.sku }, { images: overrideImages, colorImages: colorImagePayloads.value })
      } else {
        clearProductImageOverride({ id, maSanPham: form.sku })
      }
      window.toast.success("Cập nhật sản phẩm thành công")
      originalVariantIds.value = currentVariantIds
      removedVariantRows.value = []
    } else {
      const response = await createSanPhamWithRetry(payload)
      const savedId = response?.data?.id || response?.data?.data?.id || null
      if (hasImageConfig) {
        setProductImageConfig({ id: savedId, maSanPham: form.sku }, { images: overrideImages, colorImages: colorImagePayloads.value })
      } else {
        clearProductImageOverride({ id: savedId, maSanPham: form.sku })
      }
      window.toast.success("Tạo sản phẩm thành công")

      const numericSavedId = Number(savedId)
      if (Number.isFinite(numericSavedId) && numericSavedId > 0) {
        const queryString = new URLSearchParams(route.query || {}).toString()
        const nextPath = `/admin/san-pham/form/${numericSavedId}${queryString ? `?${queryString}` : ""}`
        // Force reload to initialize form in edit mode with new route param.
        window.location.assign(nextPath)
        return
      }
    }
  } catch (err) {
    console.error("SAVE ERROR:", err)
    window.toast.error("Lưu thất bại: " + (err.response?.data?.message || err.message))
  }
}
</script>

<template>
  <div class="card product-form-page">
    <!-- ── Tiêu đề trang ───────────────────────────────────────────── -->
    <div class="head product-form-header">
      <div>
        <h1>{{ id ? "Sửa sản phẩm" : "Thêm sản phẩm" }}</h1>
        <small class="muted">Mã tự sinh theo hệ thống</small>
      </div>
      <div class="header-actions">
        <button type="button" class="btn with-icon" @click="goBack">
          <span class="material-icons-outlined btn-icon">arrow_back</span>
          Quay lại
        </button>
        <button class="btn primary with-icon" @click.prevent="saveProduct">
          <span class="material-icons-outlined btn-icon">save</span>
          Lưu sản phẩm
        </button>
      </div>
    </div>

    <div class="body product-form-body">

      <!-- ── TOP: 2 cột (Thông tin | Cấu hình biến thể) ────────────── -->
      <div class="top-grid">

        <!-- Cột trái: Thông tin sản phẩm -->
        <section class="product-panel">
          <div class="panel-head">
            <h2>Thông tin sản phẩm</h2>
            <p>Điền thông tin cơ bản. Mã sản phẩm được tự sinh.</p>
          </div>

          <div class="info-fields">
            <div class="field">
              <label>Mã sản phẩm</label>
              <input type="text" readonly :value="id ? 'Mã tự sinh' : 'Mã tự sinh khi lưu'" class="auto-code-input" />
            </div>

            <div class="field">
              <label>Tên sản phẩm</label>
              <input v-model="form.name" type="text" placeholder="VD: Áo hoodie kéo khoá DirtyWave" />
            </div>

            <div class="field">
              <label>Loại sản phẩm</label>
              <select v-model.number="form.loaiId">
                <option v-for="loaiOption in loaiOptions" :key="loaiOption.id" :value="loaiOption.id">
                  {{ loaiOption.name }}
                </option>
              </select>
            </div>

            <div class="field">
              <label>Trạng thái</label>
              <select v-model="form.status">
                <option>Đang bán</option>
                <option>Ẩn</option>
              </select>
            </div>

            <div class="field field-mota">
              <label>Mô tả</label>
              <textarea v-model="form.description" placeholder="Mô tả ngắn gọn về chất liệu, form, cách sử dụng..."></textarea>
            </div>
          </div>
        </section>

        <!-- Cột phải: Cấu hình biến thể -->
        <section class="product-panel variant-builder-panel">
          <div class="panel-head">
            <h2>Cấu hình biến thể</h2>
            <p>Chọn màu và cỡ, tạo biến thể tự động theo tổ hợp.</p>
          </div>

          <!-- Màu sắc -->
          <div v-if="builderColorOptions.length" class="builder-section">
            <div class="builder-label-row">
              <span class="builder-label">Màu sắc <span class="required">*</span></span>
              <span class="builder-count">{{ builderSelectedColorCount }} đã chọn</span>
            </div>
            <div class="color-combobox" ref="colorComboboxRef">
              <div class="combobox-control" @click="colorDropdownOpen = true">
                <div class="combobox-tags">
                  <span
                    v-for="color in selectedColorTags"
                    :key="color.id"
                    class="combobox-tag"
                  >
                    <span class="color-dot" :style="{ background: color.hex }"></span>
                    {{ color.name }}
                    <button type="button" class="tag-remove" @click.stop="removeColorFromCombobox(color)">&times;</button>
                  </span>
                  <input
                    v-model="colorSearchQuery"
                    type="text"
                    class="combobox-input"
                    placeholder="Chọn màu..."
                    @focus="colorDropdownOpen = true"
                    @input="colorDropdownOpen = true"
                  />
                </div>
                <span class="combobox-arrow" :class="{ open: colorDropdownOpen }">
                  <span class="material-icons-outlined" style="font-size:18px">expand_more</span>
                </span>
              </div>
              <transition name="dropdown-fade">
                <ul v-if="colorDropdownOpen" class="combobox-dropdown">
                  <li
                    v-for="color in filteredBuilderColorOptions"
                    :key="color.id"
                    class="combobox-option"
                    :class="{ active: isColorChipActive(color) }"
                    @mousedown.prevent="selectColorFromDropdown(color)"
                  >
                    <span class="color-dot" :style="{ background: color.hex }"></span>
                    <span class="option-name">{{ color.name }}</span>
                    <span class="option-code">{{ color.code || '' }}</span>
                    <span v-if="isColorChipActive(color)" class="option-check material-icons-outlined">check</span>
                  </li>
                  <li v-if="!filteredBuilderColorOptions.length" class="combobox-empty">Không tìm thấy màu</li>
                </ul>
              </transition>
            </div>
          </div>
          
          <!-- Kích cỡ -->
          <div class="builder-section">
            <div class="builder-label-row">
              <span class="builder-label">Kích cỡ <span class="required">*</span></span>
              <span class="builder-count">{{ selectedSizes.length }} đã chọn</span>
            </div>
            <div class="color-combobox" ref="sizeComboboxRef">
              <div class="combobox-control" @click="sizeDropdownOpen = true">
                <div class="combobox-tags">
                  <span
                    v-for="size in selectedSizes"
                    :key="size.id"
                    class="combobox-tag size-tag"
                  >
                    {{ size.name }}
                    <button type="button" class="tag-remove" @click.stop="removeSizeFromCombobox(size)">&times;</button>
                  </span>
                  <input
                    v-model="sizeSearchQuery"
                    type="text"
                    class="combobox-input"
                    placeholder="Chọn size..."
                    @focus="sizeDropdownOpen = true"
                    @input="sizeDropdownOpen = true"
                  />
                </div>
                <span class="combobox-arrow" :class="{ open: sizeDropdownOpen }">
                  <span class="material-icons-outlined" style="font-size:18px">expand_more</span>
                </span>
              </div>
              <transition name="dropdown-fade">
                <ul v-if="sizeDropdownOpen" class="combobox-dropdown">
                  <li
                    v-for="size in filteredSizeOptions"
                    :key="size.id"
                    class="combobox-option"
                    :class="{ active: selectedSizes.some((s) => s.id === size.id) }"
                    @mousedown.prevent="selectSizeFromDropdown(size)"
                  >
                    <span class="option-name">{{ size.name }}</span>
                    <span v-if="selectedSizes.some((s) => s.id === size.id)" class="option-check material-icons-outlined">check</span>
                  </li>
                  <li v-if="!filteredSizeOptions.length" class="combobox-empty">Không tìm thấy size</li>
                </ul>
              </transition>
            </div>
          </div>
        </section>
      </div>
      <!-- end top-grid -->

      <!-- ── GIỮA: Danh sách biến thể (nhóm theo màu) ───────────────── -->
      <section class="product-panel variant-section">
        <div class="panel-head panel-head-inline">
          <div>
            <h2>Danh sách biến thể</h2>
            <p class="muted">{{ variantRows.length }} biến thể</p>
          </div>
          <button
            v-if="variantRows.length"
            type="button"
            class="btn danger btn-small"
            @click="clearAllVariants()"
          >
            Xóa tất cả
          </button>
        </div>

        <!-- Empty state -->
        <div v-if="!variantRows.length" class="variant-empty-state">
          <p>Chưa có biến thể. Chọn màu + cỡ bên trên rồi nhấn <strong>Tạo biến thể tự động</strong>.</p>
        </div>

        <!-- Grouped by color -->
        <div v-else class="variant-groups">
          <div v-for="group in groupedVariants" :key="String(group.colorId)" class="variant-group">
            <!-- Group header -->
            <div class="group-header">
              <div class="group-header-left">
                <span class="color-dot lg" :style="{ background: group.colorHex }"></span>
                <strong>{{ group.colorName }}</strong>
                <span class="group-count">{{ group.items.length }} cỡ</span>
              </div>
              <div class="group-header-right">
                <template v-if="bulkPanel.colorId === group.colorId">
                  <input
                    v-model.number="bulkPanel.soLuong"
                    type="number"
                    placeholder="Số lượng"
                    min="0"
                    class="bulk-input"
                  />
                  <input
                    :value="formatCurrencyInput(bulkPanel.giaBan)"
                    type="text"
                    inputmode="numeric"
                    placeholder="Giá bán"
                    class="bulk-input"
                    @input="onBulkPriceInput"
                  />
                  <button type="button" class="btn primary btn-small" @click="applyBulkToGroup">Áp dụng</button>
                  <button type="button" class="btn btn-small" @click="closeBulkPanel">Bỏ</button>
                </template>
                <template v-else>
                  <template v-if="group.colorId">
                    <input
                      :id="`group-color-img-${group.colorId}`"
                      class="image-file-input"
                      type="file"
                      accept="image/*"
                      @change="handleGroupColorImageChange(group.colorId, $event)"
                    />
                    <label class="btn btn-small group-action-btn" :for="`group-color-img-${group.colorId}`">Ảnh màu</label>
                  </template>
                    <button type="button" class="btn btn-small with-icon group-action-btn" @click="openBulkPanel(group.colorId)">
                      <span class="material-icons-outlined btn-icon">bolt</span>
                      Thêm nhanh
                  </button>
                    <button type="button" class="btn btn-small with-icon group-action-btn" @click="addVariantRowForColor(group.colorId)">
                      <span class="material-icons-outlined btn-icon">add</span>
                      Thêm cỡ
                  </button>
                    <button type="button" class="btn danger btn-small with-icon group-action-btn" @click="removeColorGroup(group.colorId)">
                      <span class="material-icons-outlined btn-icon">remove_circle_outline</span>
                      Bỏ màu
                  </button>
                </template>
              </div>
            </div>

            <!-- Sub-table -->
            <div class="sub-table-wrap">
              <table class="variant-table">
                <thead>
                  <tr>
                    <th>Kích cỡ</th>
                    <th>Màu sắc</th>
                    <th>Số lượng</th>
                    <th>Đã bán</th>
                    <th>Còn</th>
                    <th>Giá bán (VNĐ)</th>
                    <th class="col-act"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="variant in group.items" :key="variant.key">
                    <td>
                      <select :value="variant.sizeId == null ? '' : String(variant.sizeId)" @change="handleVariantSizeChange(variant, $event)">
                        <option value="">Chọn size</option>
                        <option v-for="size in sizeOptions" :key="size.id" :value="size.id">
                          {{ size.name }}
                        </option>
                      </select>
                    </td>
                    <td>
                      <select :value="variant.colorId == null ? '' : String(variant.colorId)" @change="handleVariantColorChange(variant, $event)">
                        <option value="">Chọn màu</option>
                        <option v-for="color in colorOptions" :key="color.id" :value="color.id">
                          {{ color.name }}
                        </option>
                      </select>
                    </td>
                    <td><input v-model.number="variant.soLuong" type="number" min="0" placeholder="0" /></td>
                    <td>
                      <span class="stock-chip stock-chip-sold">{{ variant.id ? getSoldQty(variant) : 0 }}</span>
                    </td>
                    <td>
                      <span class="stock-chip stock-chip-avail" :class="{ 'danger': (variant.id ? getAvailableStock(variant) : variant.soLuong) <= 0 }">
                        {{ variant.id ? getAvailableStock(variant) : variant.soLuong }}
                      </span>
                    </td>
                    <td>
                      <input
                        :value="formatCurrencyInput(variant.giaBan)"
                        type="text"
                        inputmode="numeric"
                        placeholder="0"
                        @input="onVariantPriceInput(variant, $event)"
                      />
                    </td>
                    <td class="col-act">
                      <button
                        type="button"
                        class="btn-delete-variant"
                        @click="removeVariantRow(variantRows.indexOf(variant))"
                        title="Xóa biến thể"
                      >
                        <span class="material-icons-outlined">close</span>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </section>

      <!-- ── DƯỚI: Ảnh sản phẩm ──────────────────────────────────────── -->
      <section v-if="colorImageDisplayColors.length || extraImageCards.length || id" class="product-panel color-images-section standalone">
          <div class="panel-head panel-head-inline" style="margin-bottom: 14px;">
            <div>
              <h2>Ảnh sản phẩm</h2>
              <p class="muted">Ảnh theo từng màu sắc. Kéo thả để đổi thứ tự.</p>
            </div>
            <button type="button" class="btn btn-small with-icon" @click="extraImageUploadRef?.click()">
              <span class="material-icons-outlined btn-icon">add_photo_alternate</span>
              Thêm ảnh
            </button>
            <input
              ref="extraImageUploadRef"
              class="image-file-input"
              type="file"
              accept="image/*"
              multiple
              @change="handleExtraImageUpload"
            />
          </div>
          <div v-if="colorImageDisplayColors.length" class="color-images-grid">
            <div
              v-for="(color, index) in visibleColorImageDisplayColors"
              :key="toColorStorageKey(color, index)"
              class="color-image-card"
              :class="{ 'is-drag-over': dragOverColorIndex === index }"
              draggable="true"
              @dragstart="handleColorCardDragStart(index, $event)"
              @dragover="handleColorCardDragOver(index, $event)"
              @drop="handleColorCardDrop(index, $event)"
              @dragend="handleColorCardDragEnd"
            >
              <div class="color-image-header">
                <span class="color-dot" :style="{ background: color.hex }"></span>
                <span>{{ color.name }}</span>
              </div>
              <button
                v-if="colorImages[toColorStorageKey(color, index)]?.previewUrl"
                type="button"
                class="image-remove-icon"
                aria-label="Xóa ảnh màu"
                @click="removeColorImage(color, index)"
              >
                ×
              </button>
              <div class="image-card-media">
                <img
                  v-if="colorImages[toColorStorageKey(color, index)]?.previewUrl"
                  :src="colorImages[toColorStorageKey(color, index)].previewUrl"
                  alt=""
                />
                <div v-else class="image-placeholder">Chưa có ảnh</div>
              </div>
              <input
                :id="toColorInputId(color, index)"
                class="image-file-input"
                type="file"
                accept="image/*"
                @change="handleColorImageChange(color, $event, index)"
              />
              <div class="image-card-actions">
                <span class="image-order">#{{ index + 1 }}</span>
                <span class="image-drag-hint">Kéo thả để đổi thứ tự</span>
                <label class="btn image-select-btn" :for="toColorInputId(color, index)">Chọn ảnh</label>
              </div>
            </div>
          </div>

          <div v-if="extraImageCards.length" class="color-images-grid extra-images-grid" style="margin-top: 16px;">
            <div
              v-for="(imageCard, index) in extraImageCards"
              :key="imageCard.key"
              class="color-image-card extra-image-card"
            >
              <div class="color-image-header">
                <span class="color-dot" :style="{ background: '#9ca3af' }"></span>
                <span>Ảnh sản phẩm</span>
              </div>
              <button
                v-if="resolveImageCardSrc(imageCard, imageCard.sourceIndex)"
                type="button"
                class="image-remove-icon"
                aria-label="Xóa ảnh"
                @click="removeImageAt(imageCard.sourceIndex)"
              >
                ×
              </button>
              <div class="image-card-media">
                <img
                  v-if="resolveImageCardSrc(imageCard, imageCard.sourceIndex)"
                  :src="resolveImageCardSrc(imageCard, imageCard.sourceIndex)"
                  alt=""
                />
                <div v-else class="image-placeholder">Chưa có ảnh</div>
              </div>
              <input
                :id="`extra-product-image-upload-${imageCard.key}`"
                class="image-file-input"
                type="file"
                accept="image/*"
                @change="handleImageCardFileChange(imageCard.sourceIndex, $event)"
              />
              <div class="image-card-actions">
                <span class="image-order">#{{ index + 1 }}</span>
                <label class="btn image-select-btn" :for="`extra-product-image-upload-${imageCard.key}`">Chọn ảnh</label>
              </div>
            </div>
          </div>
      </section>

    </div>
  </div>
</template>

<style scoped>
/* ── Layout ────────────────────────────────────────────────────────── */
.product-form-page {
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.product-form-header {
  align-items: flex-start;
  gap: 16px;
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
  display: block;
}

.product-form-body {
  display: grid;
  gap: 20px;
}

/* TOP 2-column grid */
.top-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
  align-items: start;
}

/* ── Card panel ────────────────────────────────────────────────────── */
.product-panel {
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 20px;
  background: #ffffff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.panel-head {
  margin-bottom: 18px;
}

.panel-head h2 {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
}

.panel-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 14px;
}

.panel-head-inline {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

/* ── Thông tin sản phẩm ────────────────────────────────────────────── */
.info-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.field-mota {
  grid-column: 1 / -1;
}

/* ── Inputs ────────────────────────────────────────────────────────── */
input,
select,
textarea {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d8dee9;
  background-color: #fff;
  font-size: 14px;
  box-sizing: border-box;
}
select {
  padding-right: 34px;
}

input:focus,
select:focus,
textarea:focus {
  outline: none;
  border-color: #c5162d;
}

textarea {
  min-height: 100px;
  resize: vertical;
}

.auto-code-input {
  background: #f1f5f9;
  color: #64748b;
  border-color: #cbd5e1;
  font-weight: 600;
}

/* ── Variant builder (right panel) ─────────────────────────────────── */
.variant-builder-panel {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.builder-section {
  padding: 14px 0;
  border-bottom: 1px solid #f1f5f9;
}

.builder-section:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.builder-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.builder-label {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.builder-count {
  font-size: 12px;
  color: #64748b;
}

/* Color chip multi-select */
.color-chips-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.color-chip-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
  color: #374151;
  transition: border-color 0.2s, background 0.2s, color 0.2s;
}

.color-chip-btn:hover {
  border-color: #c5162d;
}

.color-chip-btn.active {
  border-color: #c5162d;
  background: #fff1f2;
  color: #c5162d;
  font-weight: 600;
}

.color-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid rgba(15, 23, 42, 0.15);
  flex-shrink: 0;
}

.color-dot.lg {
  width: 18px;
  height: 18px;
}

/* Size chip selector */
.size-chips-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.size-chip-btn {
  padding: 6px 14px;
  border-radius: 8px;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  color: #374151;
  transition: border-color 0.2s, background 0.2s, color 0.2s;
  min-width: 42px;
  text-align: center;
}

.size-chip-btn:hover {
  border-color: #c5162d;
}

.size-chip-btn.active {
  border-color: #c5162d;
  background: #c5162d;
  color: #fff;
}

/* Generate buttons */
.builder-footer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 14px;
}

.builder-defaults {
  border-top: 1px solid #e5e7eb;
  padding-top: 14px;
}

.defaults-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-top: 8px;
}

.defaults-field label {
  display: block;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.defaults-field input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 13px;
}

.btn-generate {
  width: 100%;
  justify-content: center;
}

.combo-count {
  font-size: 12px;
  opacity: 0.85;
  margin-left: 4px;
}

/* ── Variant section ────────────────────────────────────────────────── */
.variant-section .muted {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.variant-empty-state {
  text-align: center;
  padding: 32px 16px;
  color: #64748b;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
}

.variant-groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.variant-group {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.variant-group + .variant-group {
  margin-top: 14px;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  gap: 12px;
  flex-wrap: wrap;
}

.group-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.group-header-right {
  display: flex;
  align-items: stretch;
  gap: 8px;
  flex-wrap: wrap;
}

.group-action-btn {
  min-height: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  line-height: 1;
}

.group-count {
  font-size: 12px;
  color: #64748b;
  background: #e2e8f0;
  padding: 2px 8px;
  border-radius: 999px;
}

.bulk-input {
  width: 110px !important;
  padding: 6px 10px !important;
  font-size: 13px !important;
}

.sub-table-wrap {
  overflow-x: auto;
}

.variant-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 560px;
}

.variant-table th,
.variant-table td {
  border-bottom: 1px solid #f1f5f9;
  padding: 10px 14px;
  text-align: left;
  vertical-align: top;
}

.variant-table tbody td:nth-child(1),
.variant-table tbody td:nth-child(2),
.variant-table tbody td:nth-child(4),
.variant-table tbody td:nth-child(5) {
  vertical-align: middle;
}

.variant-table thead th:nth-child(4),
.variant-table thead th:nth-child(5),
.variant-table tbody td:nth-child(4),
.variant-table tbody td:nth-child(5) {
  text-align: center;
  padding-left: 6px;
  padding-right: 6px;
  white-space: nowrap;
}

.variant-table tbody td.col-act {
  vertical-align: middle;
}

.variant-table thead th {
  background: #f8fafc;
  font-size: 11px;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 8px 14px;
}

.variant-table tbody tr {
  transition: background 0.12s;
}
.variant-table tbody tr:hover {
  background: #f8fafc;
}

.variant-table tbody tr:last-child td {
  border-bottom: none;
}

.stock-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
}

.stock-chip-sold {
  background: #eef2f7;
  color: #64748b;
}

.stock-chip-avail {
  background: #dcfce7;
  color: #16a34a;
}

.stock-chip-avail.danger {
  background: #fee2e2;
  color: #dc2626;
}

.variant-table td input,
.variant-table td select {
  padding: 8px 10px;
  font-size: 13px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  transition: border-color 0.15s, box-shadow 0.15s;
  outline: none;
}

.variant-table td input:focus,
.variant-table td select:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99,102,241,0.1);
}

/* ── Images ─────────────────────────────────────────────────────────── */
.image-file-input {
  display: none;
}

.image-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}

.image-preview-grid.large {
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.image-preview-card {
  border: 1px solid #dbe4f0;
  border-radius: 16px;
  padding: 12px;
  background: #fff;
  display: grid;
  gap: 12px;
  position: relative;
  transition: border-color 0.2s ease;
}

.image-preview-card.is-empty {
  border-style: dashed;
}

.image-preview-card.is-drag-over {
  border-color: #c5162d;
  box-shadow: 0 0 0 2px rgba(197, 22, 45, 0.18);
}

.image-card-media {
  width: 100%;
  aspect-ratio: 3 / 4;
  border-radius: 12px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
}

.image-preview-card img,
.color-image-card .image-card-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #94a3b8;
  padding: 16px;
  font-size: 13px;
}

.image-card-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.image-order {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  padding: 4px 10px;
}

.image-drag-hint {
  font-size: 12px;
  color: #64748b;
}

.image-select-btn {
  min-width: 110px;
  text-align: center;
}

.image-remove-icon {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.56);
  color: #fff;
  font-size: 20px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s, background 0.2s;
}

.image-preview-card:hover .image-remove-icon,
.color-image-card:hover .image-remove-icon {
  opacity: 1;
}

.image-remove-icon:hover {
  background: #dc2626;
}

/* Per-color images */
.color-images-section {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid #f1f5f9;
}

.color-images-section.standalone {
  margin-top: 0;
  padding-top: 20px;
  border-top: 0;
}

.color-images-heading {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.color-images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.color-image-card {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 14px;
  display: grid;
  gap: 12px;
  background: #fff;
  position: relative;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.24s ease;
}

.color-image-card.is-drag-over {
  border-color: #c5162d;
  box-shadow: 0 0 0 2px rgba(197, 22, 45, 0.18);
}

.color-image-card .image-card-media {
  aspect-ratio: 3 / 4;
}

.color-image-card .image-card-media img {
  object-fit: contain;
  background: #f8fafc;
}

.color-image-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.extra-images-section {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px dashed #e2e8f0;
  display: grid;
  gap: 12px;
}

.extra-images-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.extra-images-title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.extra-images-grid {
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
}

.extra-image-card {
  padding: 12px;
}

.extra-images-empty {
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  padding: 16px;
  color: #64748b;
  font-size: 13px;
  text-align: center;
}

.color-image-move {
  transition: transform 0.24s ease, opacity 0.24s ease;
}

.info-message {
  padding: 12px 16px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 8px;
  color: #0c4a6e;
  font-size: 14px;
}

.info-message p {
  margin: 0;
}

.color-image-enter-active,
.color-image-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.color-image-enter-from,
.color-image-leave-to {
  opacity: 0;
  transform: scale(0.98);
}

/* ── Utility ─────────────────────────────────────────────────────────── */
.btn.danger {
  border-color: #fecaca;
  color: #b91c1c;
  background: #fff1f2;
}

.btn-small {
  padding: 7px 12px;
  font-size: 13px;
}

/* ── Responsive ──────────────────────────────────────────────────────── */
@media (max-width: 900px) {
  .top-grid {
    grid-template-columns: 1fr;
  }
  .info-fields {
    grid-template-columns: 1fr;
  }
  .field-mota {
    grid-column: 1;
  }
}

@media (max-width: 600px) {
  .panel-head-inline {
    flex-direction: column;
    align-items: stretch;
  }
  .group-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* ── Color combobox ──────────────────────────────────────────────────── */
.color-combobox {
  position: relative;
}

.combobox-control {
  display: flex;
  align-items: center;
  border: 1.5px solid #d8dee9;
  border-radius: 10px;
  background: #fff;
  padding: 4px 8px;
  min-height: 42px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.combobox-control:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.08);
}

.combobox-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  flex: 1;
  align-items: center;
}

.combobox-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px 3px 6px;
  border-radius: 6px;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  font-size: 13px;
  font-weight: 500;
  color: #3730a3;
  white-space: nowrap;
}

.combobox-tag.size-tag {
  background: #f0fdf4;
  border-color: #bbf7d0;
  color: #166534;
}

.tag-remove {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 15px;
  line-height: 1;
  color: #6366f1;
  padding: 0 1px;
  border-radius: 3px;
  transition: background 0.15s;
}

.tag-remove:hover {
  background: #c7d2fe;
  color: #4338ca;
}

.combobox-input {
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  min-width: 80px;
  flex: 1;
  padding: 4px 2px;
}

.combobox-arrow {
  display: flex;
  align-items: center;
  color: #94a3b8;
  transition: transform 0.2s;
  flex-shrink: 0;
}

.combobox-arrow.open {
  transform: rotate(180deg);
}

.combobox-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 50;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  max-height: 240px;
  overflow-y: auto;
  list-style: none;
  padding: 4px;
  margin: 0;
}

.combobox-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.12s;
}

.combobox-option:hover {
  background: #f1f5f9;
}

.combobox-option.active {
  background: #eef2ff;
}

.option-name {
  flex: 1;
  font-weight: 500;
  color: #0f172a;
}

.option-code {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 600;
}

.option-check {
  font-size: 16px;
  color: #3b82f6;
}

.combobox-empty {
  padding: 12px 10px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}

.dropdown-fade-enter-active,
.dropdown-fade-leave-active {
  transition: opacity 0.15s, transform 0.15s;
}
.dropdown-fade-enter-from,
.dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ── Variant delete button ───────────────────────────────────────────── */
.btn-delete-variant {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #fca5a5;
  background: #fee2e2;
  color: #dc2626;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-delete-variant:hover {
  background: #f87171;
  border-color: #ef4444;
  color: #fff;
}

.btn-delete-variant .material-icons-outlined {
  font-size: 16px;
}

.variant-table .col-act {
  width: 50px;
  text-align: center;
  vertical-align: middle;
}
</style>