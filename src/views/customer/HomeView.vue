<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from "vue"
import { useRoute, useRouter } from "vue-router"
import { Eye, ShoppingCart, Truck, ShieldCheck, CreditCard, RefreshCw } from "lucide-vue-next"
import { getAllSanPham } from '../../services/sanPhamService'
import { applyCampaignPriceToVariants } from '../../services/campaignPricingService'
import { getAllHoaDon, getHoaDonById } from '../../services/hoaDonService'
import { computeSoldBySpct } from '../../utils/stockCalculation'
import { getActiveVouchers, getAllVouchers, normalizeVoucherData } from '../../services/khuyenMaiService'
import SiteNav from '../../components/SiteNav.vue'
import CustomerFooter from '../../components/customer/CustomerFooter.vue'
import { useToast } from '../../composables/useToast'
import { resolveApiOrigin } from '../../utils/apiOrigin'
import { getProductImageConfig } from '../../utils/productImageOverrides'
import { fallbackImageForVariant } from '../../utils/productImageFallback'
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
// New products
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
import momo from "../../assets/img/payments/momo.png?url"
import visa from "../../assets/img/payments/visa.png?url"
import mastercard from "../../assets/img/payments/mastercard.png?url"
import vnpay from "../../assets/img/payments/vnpay.png?url"
import { readCartObject, readCartVariantsObject, writeCartObject, writeCartVariantsObject } from "../../utils/cartStorage"
const router = useRouter()
const route = useRoute()
const { success: toastSuccess, cartAdded: toastCartAdded } = useToast()
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, '')
const VND = n => new Intl.NumberFormat("vi-VN").format(n) + "₫"
const CART_UPDATED_EVENT = "dirtywave:cart-updated"

const activeFilter = ref(null)
const sectionPulse = ref("")
const toastMessage = ref("")
const toastVisible = ref(false)
let toastTimer = null
const email = ref("")
const year = new Date().getFullYear()

const heroBenefits = [
  { icon: Truck, title: 'Giao hàng toàn quốc', text: 'Nhận hàng nhanh 2-5 ngày' },
  { icon: ShieldCheck, title: 'Đảm bảo chính hãng', text: 'Chất liệu và form được kiểm tra kỹ' },
  { icon: CreditCard, title: 'Thanh toán linh hoạt', text: 'Hỗ trợ nhiều phương thức thanh toán' },
  { icon: RefreshCw, title: 'Đổi trả dễ dàng', text: 'Hỗ trợ đổi size theo chính sách' },
]

const heroSlides = [
  {
    id: 1,
    cat: 'ALL',
    target: 'products',
    tag: 'Bộ sưu tập DirtyWave',
    title: 'DirtyWave Outerwear Collection',
    titleMain: 'DirtyWave Outerwear',
    titleDrop: 'Collection',
    subtitle: 'Hoodie, Bomber, Coach và nhiều phom áo khoác được yêu thích nhất',
    heroMain: img16,
    heroSideTop: img13,
    heroSideBottom: img14,
  },
  {
    id: 20,
    cat: 'Hoodie',
    target: 'products',
    tag: 'Bộ sưu tập DirtyWave',
    title: 'Hoodie Zip Silk DirtyWave',
    subtitle: 'Mềm nhẹ, bóng mịn, phối lớp cực nổi bật',
    heroMain: img20,
    heroSideTop: img20b,
    heroSideBottom: img20c,
  },
  {
    id: 14,
    cat: 'Bomber',
    target: 'products',
    tag: 'Bộ sưu tập DirtyWave',
    title: 'Bomber Windbreaker DirtyWave',
    subtitle: 'Chống gió tốt, phom khỏe, chất đường phố rõ nét',
    heroMain: img14,
    heroSideTop: img14b,
    heroSideBottom: img14c,
  },
]

const heroActiveIndex = ref(0)
const heroDirection = ref(1)

const activeHeroSlide = computed(() => heroSlides[heroActiveIndex.value] || heroSlides[0])
const heroTransitionName = computed(() =>
  heroDirection.value >= 0 ? 'dw-hero-swipe-next' : 'dw-hero-swipe-prev'
)

const nextHeroSlide = () => {
  heroDirection.value = 1
  heroActiveIndex.value = (heroActiveIndex.value + 1) % heroSlides.length
}

const prevHeroSlide = () => {
  heroDirection.value = -1
  heroActiveIndex.value = (heroActiveIndex.value - 1 + heroSlides.length) % heroSlides.length
}

const easeInOutCubic = (t) => (t < 0.5 ? 4 * t * t * t : 1 - ((-2 * t + 2) ** 3) / 2)

const smoothScrollToProductNav = () => {
  const productNav = document.getElementById('sanpham-nav')
  if (!productNav) return

  const header = document.querySelector('header')
  const headerOffset = header ? header.getBoundingClientRect().height + 12 : 12

  const startY = window.scrollY || window.pageYOffset
  const targetY = Math.max(0, productNav.getBoundingClientRect().top + startY - headerOffset)
  const distance = targetY - startY
  const duration = 760
  const startTime = performance.now()

  const step = (now) => {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = easeInOutCubic(progress)
    window.scrollTo(0, startY + distance * eased)
    if (progress < 1) window.requestAnimationFrame(step)
  }

  window.requestAnimationFrame(step)
}

const handleHeroAction = (slide) => {
  if (slide?.target === 'products' || slide?.id) {
    smoothScrollToProductNav()
    return
  }
  if (slide?.cat) focusCategory(slide.cat)
}


const getImage = (id) => {
  return new URL(`../../assets/img/product${id}.jpg`, import.meta.url).href
}

const products = ref([])

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
}

const normalizeCatalogProductCode = (value = '') => {
  const raw = String(value || '').trim().toUpperCase()
  if (!raw) return ''

  const spMatch = raw.match(/^SP0*(\d{1,3})$/i)
  if (spMatch?.[1]) {
    const n = Number(spMatch[1])
    if (Number.isFinite(n) && n >= 1 && n <= 20) return `SP${String(n).padStart(3, '0')}`
  }

  const legacyMatch = raw.match(/^ATID070-0*(\d{1,3})$/i)
  if (legacyMatch?.[1]) {
    const n = Number(legacyMatch[1])
    if (Number.isFinite(n) && n >= 1 && n <= 20) return `SP${String(n).padStart(3, '0')}`
  }

  return ''
}

const staticQuickImagesByCode = {
  SP002: [img2],
  SP009: [img9, img10],
  SP012: [img12, img12b],
  SP013: [img13, img13b, img13c, img13d, img13e],
  SP014: [img14, img14b, img14c],
  SP016: [img16, img16b, img16c],
  SP018: [img18, img18b],
  SP019: [img19, img19b],
  SP020: [img20, img20b, img20c]
}

// Map Vietnamese color names to English keywords in static image filenames
const colorKeywordMap = {
  den: "black", do: "red", trang: "white", xam: "gray", xanh: "blue",
  "xanh duong": "blue", "xanh la": "green", nau: "brown",
  vang: "yellow", hong: "pink", tim: "purple", cam: "orange",
  be: "beige", kem: "cream"
}

const normalizeColorKeyword = (value = "") =>
  String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .trim()

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

const normalizeProductKeyForImage = (value = "") =>
  String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()

const normalizeSearchTextForImage = (value = "") =>
  normalizeProductKeyForImage(value)
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()

const stripTrailingBrandTokenForImage = (value = "") =>
  String(value || "")
    .trim()
    .replace(/[\s_-]*(dirty\s*wave|dirtywave)$/i, "")
    .trim()

const getMappedFallbackByName = (name = "") => {
  const normalized = normalizeProductKeyForImage(name)
  if (!normalized) return ""
  const found = mappedFallbackByName.find((rule) =>
    rule.keywords.every((keyword) => normalized.includes(keyword))
  )
  return found?.image || ""
}

const fallbackIndexByName = (name = "") => {
  const normalized = normalizeSearchTextForImage(stripTrailingBrandTokenForImage(name))
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

  if (!/^ATID070-\d+$/i.test(normalizedCode) && !/^SP0*(?:[1-9]|1\d|20)$/i.test(normalizedCode)) {
    return ""
  }

  const mappedByName = getMappedFallbackByName(name)
  if (mappedByName) return mappedByName

  const numericId = Number(id)
  if (Number.isFinite(numericId) && numericId > 0) {
    if (allowCuratedCodeMap && mappedFallbackByCodeNum[numericId]) return mappedFallbackByCodeNum[numericId]
    const nameIndex = fallbackIndexByName(name)
    if (nameIndex >= 0) return fallbackImages[nameIndex] || img1
    return fallbackImages[(numericId - 1) % fallbackImages.length]
  }

  const digits = String(code || "").replace(/\D+/g, "")
  const codeNum = Number(digits)
  if (Number.isFinite(codeNum) && codeNum > 0) {
    if (allowCuratedCodeMap && mappedFallbackByCodeNum[codeNum]) return mappedFallbackByCodeNum[codeNum]
    const nameIndex = fallbackIndexByName(name)
    if (nameIndex >= 0) return fallbackImages[nameIndex] || img1
    return fallbackImages[(codeNum - 1) % fallbackImages.length]
  }

  const nameIndex = fallbackIndexByName(name)
  if (nameIndex >= 0) return fallbackImages[nameIndex] || img1
  return fallbackImages[0] || img1
}

const isAbsoluteUrl = (value = '') => /^https?:\/\//i.test(value) || /^data:image\//i.test(value)

const toImageUrl = (value) => {
  const raw = String(value || '').trim()
  if (!raw) return ''
  if (isAbsoluteUrl(raw)) return raw
  const normalized = raw.replace(/\\/g, '/')
  const uploadsMatch = normalized.match(/(?:^|\/)(uploads\/.*)$/i)
  if (uploadsMatch?.[1]) return `${BACKEND_ORIGIN}/${uploadsMatch[1]}`
  if (normalized.startsWith('/uploads/')) return `${BACKEND_ORIGIN}${normalized}`
  if (normalized.startsWith('uploads/')) return `${BACKEND_ORIGIN}/${normalized}`
  if (normalized.startsWith('assets/') || normalized.startsWith('img/')) return `/${normalized}`
  if (normalized.startsWith('/')) return normalized
  return normalized
}

const normalizeImageKey = (value = '') => {
  const raw = String(value || '').trim()
  if (!raw) return ''
  const withoutQuery = raw.split('#')[0].split('?')[0]
  const normalized = withoutQuery.replace(/\\/g, '/').toLowerCase()
  try {
    const url = new URL(normalized, window.location.origin)
    return url.pathname.toLowerCase()
  } catch {
    return normalized
  }
}

const isSameImage = (left = '', right = '') => {
  const l = normalizeImageKey(left)
  const r = normalizeImageKey(right)
  return !!l && !!r && l === r
}

const isLogoLikeUrl = (url) => /logo/i.test(String(url || ''))

const pickImageValue = (entry) => {
  if (!entry) return ''
  if (typeof entry === 'string') {
    const url = toImageUrl(entry)
    return isLogoLikeUrl(url) ? '' : url
  }
  if (Array.isArray(entry)) {
    for (const child of entry) {
      const found = pickImageValue(child)
      if (found) return found
    }
    return ''
  }
  if (typeof entry === 'object') {
    const keys = ['anh', 'hinhAnh', 'image', 'imageUrl', 'images', 'listAnh', 'anhChinh', 'url', 'path', 'previewUrl', 'src']
    for (const key of keys) {
      const found = pickImageValue(entry[key])
      if (found) return found
    }
  }
  return ''
}

const normalizeColorImageEntries = (entries = []) => {
  const list = Array.isArray(entries)
    ? entries
    : (entries && typeof entries === 'object'
      ? Object.entries(entries).map(([key, value], index) => {
          if (value && typeof value === 'object') {
            return {
              ...value,
              colorId: value.colorId ?? value.mauSacId ?? Number(key),
              order: value.order ?? value.thuTu ?? index
            }
          }

          return {
            colorId: Number(key),
            image: value,
            order: index
          }
        })
      : [])
  return list
    .map((entry, index) => {
      const image = pickImageValue([
        entry,
        entry?.image,
        entry?.previewUrl,
        entry?.url,
        entry?.path,
        entry?.anh,
        entry?.hinhAnh,
        entry?.duongDanAnh
      ])

      const colorId = Number(
        entry?.colorId
        || entry?.mauSacId
        || entry?.idMauSac
        || entry?.id_mau_sac
        || entry?.mauSac?.id
        || entry?.mau?.id
        || 0
      )

      const colorName = String(
        entry?.colorName
        || entry?.tenMau
        || entry?.mauSac?.tenMau
        || entry?.mauSac?.tenMauSac
        || entry?.mau?.tenMau
        || ''
      ).trim()

      const order = Number.isFinite(Number(entry?.order))
        ? Number(entry.order)
        : (Number.isFinite(Number(entry?.thuTu)) ? Number(entry.thuTu) : index)

      return { colorId, colorName, image, order }
    })
    .filter((entry) => entry.image && (entry.colorId > 0 || entry.colorName))
    .sort((left, right) => left.order - right.order)
}

const colorHexByName = (name = "") => {
  const n = String(name || "").normalize("NFD").replace(/\p{M}/gu, "").replace(/đ/g, "d").replace(/Đ/g, "D").toLowerCase().trim()
  if (n === "den" || n === "black") return "#111827"
  if (n === "trang" || n === "white") return "#e5dfd0"
  if (n === "do" || n === "red") return "#dc2626"
  if (n === "xanh duong" || n === "xanh" || n === "blue" || n === "navy") return "#2f4f75"
  if (n === "xanh la" || n === "xanh la cay") return "#4f6f52"
  if (n === "xam" || n === "ghi" || n === "gray" || n === "grey") return "#8c95a3"
  if (n === "nau" || n === "brown") return "#8B5E3C"
  if (n === "kem" || n === "be" || n === "cream") return "#d4c9a8"
  if (n === "vang" || n === "yellow") return "#d4a017"
  if (n === "hong" || n === "pink") return "#e8927c"
  if (n === "cam" || n === "orange") return "#e07020"
  if (n === "ruou" || n === "wine") return "#722f37"
  return "#9ca3af"
}

const mapBackendProductToHomeCard = (item, fallbackIndex = 0) => {
  const rawVariants = Array.isArray(item?.sanPhamChiTiets) ? item.sanPhamChiTiets : []
  const activeVariants = rawVariants.filter((variant) => isActiveStatus(variant?.trangThai || variant?.status))
  const variants = activeVariants.length ? activeVariants : rawVariants
  const variantPrices = variants.map((variant) => Number(variant?.giaBan || 0)).filter((n) => n > 0)
  const variantWithPrice = variants
    .map((variant) => ({
      currentPrice: Number(variant?.giaBan || 0),
      originalPrice: Number(variant?.giaBanGoc || 0),
    }))
    .filter((variant) => variant.currentPrice > 0)
  const id = Number(item?.id)
  const rawCode = String(item?.maSanPham || item?.ma || "").trim().toUpperCase()
  const normalizedCatalogCode = normalizeCatalogProductCode(rawCode)
  const code = normalizedCatalogCode || rawCode
  const productName = String(item?.tenSanPham || item?.name || `Sản phẩm ${id || fallbackIndex + 1}`)
  const imageConfig = getProductImageConfig({ id, maSanPham: item?.maSanPham })
  const configImages = Array.isArray(imageConfig?.images)
    ? imageConfig.images.map((entry) => String(entry || '').trim()).filter(Boolean)
    : []
  const overrideImage = configImages[0] || ''

  const variantStock = variants.reduce((sum, v) => sum + Number(v?.soLuong || 0), 0)
  const totalStock = variantStock > 0 ? variantStock : Number(item?.soLuong || 0)

  const colors = [...new Set(
    variants
      .map((variant) => String(variant?.mauSac?.tenMau || variant?.mauSac?.tenMauSac || variant?.mauSac?.name || variant?.tenMau || variant?.tenMauSac || "").trim())
      .filter(Boolean)
  )].map((name) => ({ name, hex: colorHexByName(name) }))

  const sizes = [...new Set(
    variants
      .map((variant) => String(variant?.kichThuoc?.tenKichThuoc || variant?.kichThuoc?.name || variant?.tenKichThuoc || variant?.size || "").trim())
      .filter(Boolean)
  )]

  const colorImageEntries = [...new Map([
    ...normalizeColorImageEntries(imageConfig?.colorImages || []).map((entry) => [
      `${Number(entry.colorId || 0)}|${normalizeNameKey(entry.colorName || '')}|${entry.image}`,
      entry
    ]),
    ...normalizeColorImageEntries(item?.colorImages || []).map((entry) => [`${entry.colorId}|${entry.image}`, entry]),
    ...normalizeColorImageEntries(item?.mauSacHinhAnhs || []).map((entry) => [`${entry.colorId}|${entry.image}`, entry]),
    ...normalizeColorImageEntries(item?.anhTheoMauSac || []).map((entry) => [`${entry.colorId}|${entry.image}`, entry]),
    ...normalizeColorImageEntries(item?.hinhAnhTheoMauSac || []).map((entry) => [`${entry.colorId}|${entry.image}`, entry]),
    ...normalizeColorImageEntries(item?.anhMauSac || []).map((entry) => [`${entry.colorId}|${entry.image}`, entry]),
    ...variants
      .map((variant, order) => {
        const colorId = Number(variant?.mauSac?.id || 0)
        const colorName = String(variant?.mauSac?.tenMau || variant?.mauSac?.tenMauSac || '').trim()
        const image = pickImageValue([
          variant,
          variant?.anh,
          variant?.hinhAnh,
          variant?.image,
          variant?.imageUrl,
          variant?.duongDanAnh
        ])
        return { colorId, colorName, image, order }
      })
      .filter((entry) => entry.image && (entry.colorId > 0 || entry.colorName))
      .map((entry) => [`${entry.colorId}|${entry.image}`, entry]),
  ]).values()]
    .filter((entry) => {
      if (!variants.length) return true

      const entryColorId = Number(entry?.colorId || 0)
      const entryColorName = String(entry?.colorName || '').trim()
      return variants.some((variant) => {
        const variantColorId = Number(variant?.mauSac?.id || 0)
        const variantColorName = String(
          variant?.mauSac?.tenMau || variant?.mauSac?.tenMauSac || variant?.mauSac?.name || variant?.tenMau || variant?.tenMauSac || ''
        ).trim()

        if (entryColorId > 0 && variantColorId > 0) {
          return entryColorId === variantColorId
        }

        return entryColorName && isSameColor(entryColorName, variantColorName)
      })
    })

  const sold = resolveSoldCount(item, variants)
  const rawCategory = String(item?.loai?.tenLoai || item?.danhMuc?.tenDanhMuc || 'Thời trang nam')
  const normalizedCategory = toCanonicalCategory(rawCategory) || inferCategoryFromName(productName) || rawCategory
  const lowestPricedVariant = variantWithPrice.length
    ? variantWithPrice.reduce((lowest, current) => (current.currentPrice < lowest.currentPrice ? current : lowest), variantWithPrice[0])
    : null
  const oldPriceFromCampaign = lowestPricedVariant && lowestPricedVariant.originalPrice > lowestPricedVariant.currentPrice
    ? lowestPricedVariant.originalPrice
    : null

  const staticImages = isCuratedQuickProductCode(code) && staticQuickImagesByCode[normalizeCatalogProductCode(code)]
    ? staticQuickImagesByCode[normalizeCatalogProductCode(code)]
    : []

  const firstVariant = variants[0] || null
  const firstVariantColor = String(
    firstVariant?.mauSac?.tenMau
    || firstVariant?.mauSac?.tenMauSac
    || firstVariant?.mauSac?.name
    || firstVariant?.tenMau
    || firstVariant?.tenMauSac
    || ''
  ).trim()
  const curatedPrimaryImage = isCuratedQuickProductCode(code)
    ? (fallbackImageForVariant({
        id: Number(firstVariant?.id || id || 0),
        maSanPham: normalizeCatalogProductCode(code) || code,
        tenSanPham: productName,
        tenMauSac: firstVariantColor,
        maChiTietSanPham: firstVariant?.maSanPhamChiTiet || firstVariant?.maChiTiet || ''
      }) || '')
    : ''

  const galleryImages = [...new Set([
    curatedPrimaryImage,
    ...configImages,
    ...colorImageEntries.map((entry) => String(entry?.image || '').trim()).filter(Boolean),
    ...variants.map((variant) => pickImageValue([
      variant,
      variant?.anh,
      variant?.hinhAnh,
      variant?.image,
      variant?.imageUrl,
      variant?.duongDanAnh
    ])).filter(Boolean),
    ...normalizeColorImageEntries(item?.images || []).map((entry) => entry.image).filter(Boolean),
    pickImageValue([item, item?.anh, item?.hinhAnh, item?.images, item?.image, item?.listAnh]),
    ...staticImages,
    overrideImage
  ].map((entry) => String(entry || '').trim()).filter(Boolean))]

  const heroImage = overrideImage || curatedPrimaryImage || galleryImages[0] || pickImageValue([item, variants]) || fallbackImageFor(id, code, productName)

  return {
    id,
    name: productName,
    cat: normalizedCategory,
    price: variantPrices.length ? Math.min(...variantPrices) : Number(item?.giaBan || item?.gia || 0),
    // Only show strikethrough price when campaign pricing is actually applied on this product.
    old: oldPriceFromCampaign,
    tag: totalStock > 0 ? '' : 'Sold out',
    sold,
    stock: totalStock,
    img: heroImage,
    images: galleryImages.length ? galleryImages : [heroImage],
    colors,
    sizes,
    variants,
    colorImageEntries,
    code,
  }
}

watch(() => route.query.category, async (cat) => {
  if (cat) {
    activeFilter.value = String(cat)
    await nextTick()
    scrollToId("best")
    pulseSection("best")
  }
})

onMounted(() => {

  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add("show")
      }
    })
  }, { threshold: 0.1 })

  document.querySelectorAll(".card").forEach(el => {
    el.classList.add("fade-in")
    observer.observe(el)
  })

  loadHomeBackendProducts()
  loadQuickPromoVouchers()

  if (route.query.category) {
    activeFilter.value = String(route.query.category)
  }

  const savedCart = readCartObject()
  if (savedCart && typeof savedCart === "object") {
    cart.value = savedCart
  }

})

const handleNavAnchor = (anchor) => {
  scrollToId(anchor)
}

const scrollToId = id => {
  const el = document.getElementById(id)
  if (!el) return
  const headerOffset = 96
  const top = el.getBoundingClientRect().top + window.scrollY - headerOffset
  window.scrollTo({ top, behavior: "smooth" })
}

const pulseSection = (id) => {
  sectionPulse.value = id
  window.setTimeout(() => {
    if (sectionPulse.value === id) sectionPulse.value = ""
  }, 700)
}

const filterBy = cat => {
  activeFilter.value = cat
  scrollToId("best")
  pulseSection("best")
}

const focusCategory = (cat) => {
  filterBy(cat)
  toast(`Đang lọc danh mục ${cat}`)
}

const clearFilter = () =>
  activeFilter.value = null

const openShop = () => {
  router.push("/san-pham")
}

const filteredBest = computed(() => {
  let productList = products.value
  productList = [...productList].sort((a, b) => {
    const soldDiff = Number(b?.sold || 0) - Number(a?.sold || 0)
    if (soldDiff !== 0) return soldDiff
    return Number(b?.id || 0) - Number(a?.id || 0)
  })
  
  if(!activeFilter.value) return productList
  return productList.filter((p) => {
    const canonical = toCanonicalCategory(p?.cat) || inferCategoryFromName(p?.name)
    return canonical === activeFilter.value
  })
})

const toast = msg => {
  toastMessage.value = msg
  toastVisible.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastVisible.value = false
  }, 5000)
}

// CART STATE
const cart = ref({})

const notifyCartUpdated = () => {
  window.dispatchEvent(new Event(CART_UPDATED_EVENT))
}

const addToCart = (id, qty = 1, cartToastPayload = null) => {
  if (!cart.value[id]) cart.value[id] = 0
  cart.value[id] += Number(qty || 1)

  cart.value = { ...cart.value }   // force Vue update
  notifyCartUpdated()
  if (cartToastPayload) {
    try {
      if (typeof toastCartAdded === 'function') {
        toastCartAdded(cartToastPayload)
      } else if (typeof window?.toast?.cartAdded === 'function') {
        window.toast.cartAdded(cartToastPayload)
      } else {
        toastSuccess('Đã thêm vào giỏ hàng')
      }
    } catch {
      toastSuccess('Đã thêm vào giỏ hàng')
    }
  } else {
    toastSuccess('Đã thêm vào giỏ hàng')
  }
}

watch(
  cart,
  (nextCart) => {
    writeCartObject(nextCart)
  },
  { deep: true }
)

const subscribe = () => {
  if(!email.value || !email.value.includes('@')) return toast('Nhập email hợp lệ')
  email.value = ''
  toast('Đã đăng ký nhận tin (demo)')
}

const selectedProduct = ref(null)
const quickQty = ref(1)
const quickSize = ref("S")
const quickColorIndex = ref(0)
const quickImageIndex = ref(0)
const quickImageAnimating = ref(false)
const quickVouchers = ref([])
const soldByProduct = ref({
  byId: {},
  byCode: {},
  byName: {}
})
const soldBySpct = ref(new Map())
const homeBackendProducts = ref([])

const normalizeKeyword = (value = '') =>
  String(value).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim()

const toCanonicalCategory = (value = '') => {
  const normalized = normalizeKeyword(value)
  if (!normalized) return ''
  if (normalized.includes('hoodie')) return 'Hoodie'
  if (normalized.includes('bomber')) return 'Bomber'
  if (normalized.includes('coach')) return 'Coach'
  return ''
}

const inferCategoryFromName = (name = '') => {
  const normalized = normalizeKeyword(name)
  if (normalized.includes('hoodie')) return 'Hoodie'
  if (normalized.includes('bomber')) return 'Bomber'
  if (normalized.includes('coach')) return 'Coach'
  return ''
}

const isActiveStatus = (value = '') => {
  const normalized = normalizeKeyword(value)
  if (!normalized) return true
  if (normalized.includes('ngung')) return false
  if (normalized.includes('inactive')) return false
  if (normalized.includes('an')) return false
  return true
}

const isBackendProductActive = (item) => {
  if (!isActiveStatus(item?.trangThai || item?.status)) return false

  const variants = Array.isArray(item?.sanPhamChiTiets) ? item.sanPhamChiTiets : []
  if (!variants.length) return true

  return variants.some((variant) => isActiveStatus(variant?.trangThai || variant?.status))
}

const normalizeNameKey = (value = '') => String(value || '')
  .normalize('NFD')
  .replace(/\p{M}/gu, '')
  .replace(/đ/g, 'd')
  .replace(/Đ/g, 'D')
  .toLowerCase()
  .replace(/[^a-z0-9]+/g, ' ')
  .trim()

const loadSoldFromOrders = async () => {
  try {
    const response = await getAllHoaDon()
    const orders = Array.isArray(response?.data) ? response.data : []

    const byId = {}
    const byCode = {}
    const byName = {}

    const addSold = (id, code, name, qty) => {
      const amount = Math.max(Number(qty || 0), 0)
      if (!amount) return

      const numericId = Number(id || 0)
      const normalizedCode = String(code || '').trim().toUpperCase()
      const normalizedName = normalizeNameKey(name)

      if (numericId > 0) byId[numericId] = Number(byId[numericId] || 0) + amount
      if (normalizedCode) byCode[normalizedCode] = Number(byCode[normalizedCode] || 0) + amount
      if (normalizedName) byName[normalizedName] = Number(byName[normalizedName] || 0) + amount
    }

    const isCanceledOrder = (order) => {
      const status = String(order?.trangThai || order?.status || order?.state || '').toLowerCase()
      return status.includes('huy') || status.includes('cancel')
    }

    const extractItemLists = (order) => {
      return [
        order?.items,
        order?.chiTietHoaDons,
        order?.hoaDonChiTiets,
        order?.chiTietDonHang,
        order?.details,
        order?.lineItems
      ].filter((list) => Array.isArray(list) && list.length)
    }

    const pendingDetailIds = []

    for (const order of orders) {
      if (isCanceledOrder(order)) continue

      const itemLists = extractItemLists(order)

      if (!itemLists.length) {
        const id = Number(order?.id || order?.hoaDonId || 0)
        if (id > 0) pendingDetailIds.push(id)
      }

      for (const itemList of itemLists) {
        for (const item of itemList) {
          const qty = Number(item?.soLuong || item?.quantity || item?.qty || 0)
          const productId = Number(item?.sanPhamId || item?.idSanPham || item?.productId || item?.sanPham?.id || 0)
          const productCode = String(item?.maSanPham || item?.productCode || item?.sanPham?.maSanPham || '').trim()
          const productName = String(item?.tenSanPham || item?.productName || item?.sanPham?.tenSanPham || '').trim()
          addSold(productId, productCode, productName, qty)
        }
      }
    }

    if (pendingDetailIds.length) {
      const uniqueIds = [...new Set(pendingDetailIds)].slice(0, 120)
      const detailResults = await Promise.allSettled(uniqueIds.map((id) => getHoaDonById(id)))
      for (const result of detailResults) {
        if (result.status !== 'fulfilled') continue
        const order = result.value?.data || {}
        if (isCanceledOrder(order)) continue
        const itemLists = extractItemLists(order)
        for (const itemList of itemLists) {
          for (const item of itemList) {
            const qty = Number(item?.soLuong || item?.quantity || item?.qty || 0)
            const productId = Number(item?.sanPhamId || item?.idSanPham || item?.productId || item?.sanPham?.id || 0)
            const productCode = String(item?.maSanPham || item?.productCode || item?.sanPham?.maSanPham || '').trim()
            const productName = String(item?.tenSanPham || item?.productName || item?.sanPham?.tenSanPham || '').trim()
            addSold(productId, productCode, productName, qty)
          }
        }
      }
    }

    soldByProduct.value = { byId, byCode, byName }
  } catch {
    soldByProduct.value = { byId: {}, byCode: {}, byName: {} }
  }
}

const resolveSoldCount = (item, variants = []) => {
  const activeVariantIds = [...new Set(
    (Array.isArray(variants) ? variants : [])
      .filter((variant) => isActiveStatus(variant?.trangThai || variant?.status))
      .map((variant) => Number(
        variant?.id || variant?.spctId || variant?.sanPhamChiTietId || variant?.chiTietSanPhamId || 0
      ))
      .filter((spctId) => Number.isFinite(spctId) && spctId > 0)
  )]

  const bySpct = activeVariantIds.reduce((sum, spctId) => {
    return sum + Number(soldBySpct.value.get(spctId) || 0)
  }, 0)

  if (bySpct > 0) return bySpct

  const id = Number(item?.id || 0)
  const code = String(item?.maSanPham || '').trim().toUpperCase()
  const nameKey = normalizeNameKey(item?.tenSanPham || item?.name || '')

  const byOrder = Math.max(
    Number(soldByProduct.value.byId?.[id] || 0),
    Number(soldByProduct.value.byCode?.[code] || 0),
    Number(soldByProduct.value.byName?.[nameKey] || 0)
  )

  if (byOrder > 0) return byOrder

  const rawSold = Number(item?.soLuongDaBan || item?.soldCount || 0)
  if (rawSold > 0) return rawSold

  return Math.max(0, variants.reduce((sum, variant) => sum + Number(variant?.soLuongDaBan || 0), 0))
}

const loadHomeBackendProducts = async () => {
  try {
    const [soldByOrderLoaded, soldBySpctMap] = await Promise.all([
      loadSoldFromOrders(),
      computeSoldBySpct()
    ])
    void soldByOrderLoaded
    soldBySpct.value = soldBySpctMap instanceof Map ? soldBySpctMap : new Map()

    const response = await getAllSanPham()
    const rawProducts = Array.isArray(response?.data) ? response.data : []
    const withCampaignPrice = await Promise.all(rawProducts.map(async (item) => {
      const variants = Array.isArray(item?.sanPhamChiTiets) ? item.sanPhamChiTiets : []
      if (!variants.length) return item
      const pricedVariants = await applyCampaignPriceToVariants(variants, item.id)
      return {
        ...item,
        sanPhamChiTiets: pricedVariants,
      }
    }))

    homeBackendProducts.value = withCampaignPrice
    if (homeBackendProducts.value.length) {
      const backendProductsMapped = homeBackendProducts.value
        .filter((item) => isBackendProductActive(item))
        .map((item, index) => mapBackendProductToHomeCard(item, index))
        .filter((item) => Number.isFinite(item.id) && item.id > 0)

      products.value = backendProductsMapped.filter((item) => item.price > 0)
    }
  } catch {
    soldBySpct.value = new Map()
    homeBackendProducts.value = []
    products.value = []
  }
}

const getQuickProductCode = (product) => {
  if (!product) return ''
  const directCode = normalizeCatalogProductCode(product?.code || product?.maSanPham || '')
  if (directCode) return directCode
  const byId = homeBackendProducts.value.find(item => Number(item?.id) === Number(product.id))
  const byIdCode = normalizeCatalogProductCode(byId?.maSanPham)
  if (byIdCode) return byIdCode
  const localName = normalizeKeyword(product.name)
  const byName = homeBackendProducts.value.find(item => normalizeKeyword(item?.tenSanPham) === localName)
  const byNameCode = normalizeCatalogProductCode(byName?.maSanPham)
  if (byNameCode) return byNameCode
  return normalizeCatalogProductCode(product?.code) || String(product.id)
}

const isCuratedQuickProductCode = (value = '') => Boolean(normalizeCatalogProductCode(value))

const getStaticQuickImagesForProduct = (product) => {
  const normalizedCode = normalizeCatalogProductCode(getQuickProductCode(product))
  if (!normalizedCode) return []
  return Array.isArray(staticQuickImagesByCode[normalizedCode]) ? staticQuickImagesByCode[normalizedCode] : []
}

const QUICK_SIZES = ["S", "M", "L", "XL"]

const openModal = (product) => {
  selectedProduct.value = product
  quickQty.value = 1
  quickSize.value = Array.isArray(product?.sizes) && product.sizes.length ? product.sizes[0] : "S"
  quickColorIndex.value = 0
  quickImageIndex.value = 0
  window.requestAnimationFrame(() => {
    selectQuickColor(0)
  })
}

const openProductDetail = (productId) => {
  router.push('/product/' + productId)
}
const cartCount = computed(() => {
  return Object.values(cart.value).reduce((a,b)=>a+b,0)
})

const closeModal = () => {
  selectedProduct.value = null
}

const quickPreviewImages = computed(() => {
  const product = selectedProduct.value
  if (!product) return []

  const candidates = []
  const curatedCode = normalizeCatalogProductCode(getQuickProductCode(product))
  const isCuratedCatalog = Boolean(curatedCode)

  if (Array.isArray(product.colors) && product.colors.length) {
    for (const color of product.colors) {
      const selectedColorName = String(color?.name || '').trim()
      if (!selectedColorName) continue
      const variant = findVariantFromQuickSelection(product, selectedColorName, quickSize.value)
      const colorImage = resolveQuickVariantImage(product, variant, selectedColorName)
      if (colorImage) {
        candidates.push(colorImage)
      }
    }
  }

  // Include all static color variant images for this product.
  const staticImages = getStaticQuickImagesForProduct(product)
  if (staticImages.length) {
    candidates.push(...staticImages.map((img) => String(img || '').trim()).filter(Boolean))
  }

  if (isCuratedCatalog && !candidates.length) {
    const firstColorName = String(product?.colors?.[0]?.name || '').trim()
    const firstVariant = findVariantFromQuickSelection(product, firstColorName, quickSize.value)
    const firstCurated = fallbackImageForVariant({
      id: Number(firstVariant?.id || product?.id || 0),
      maSanPham: curatedCode,
      tenSanPham: product?.name,
      tenMauSac: firstColorName,
      maChiTietSanPham: firstVariant?.maSanPhamChiTiet || firstVariant?.maChiTiet || firstVariant?.ma || ''
    })
    if (firstCurated) candidates.push(firstCurated)
  }

  // For curated catalog products, avoid raw backend images that can be mismatched.
  if (isCuratedCatalog) {
    return [...new Set(candidates.map((img) => String(img || '').trim()).filter(Boolean))]
  }

  // Keep backend/raw image sources at lower priority.
  if (Array.isArray(product.images)) {
    candidates.push(...product.images.map((img) => String(img || '').trim()).filter(Boolean))
  }

  if (product?.img) candidates.push(String(product.img))

  return [...new Set(candidates.map((img) => String(img || '').trim()).filter(Boolean))]
})

const quickActiveImage = computed(() => {
  const images = quickPreviewImages.value
  if (!images.length) return selectedProduct.value?.img || ''
  const safeIndex = ((quickImageIndex.value % images.length) + images.length) % images.length
  return images[safeIndex]
})

const triggerQuickImageAnimation = () => {
  quickImageAnimating.value = false
  window.requestAnimationFrame(() => {
    quickImageAnimating.value = true
  })
}

const syncQuickColorWithImage = () => {
  const product = selectedProduct.value
  if (!product || !Array.isArray(product.colors) || !product.colors.length) return
  const currentImage = quickActiveImage.value
  if (!currentImage) return

  const matchedIndex = product.colors.findIndex((color) => {
    const selectedColorName = String(color?.name || '').trim()
    if (!selectedColorName) return false
    const variant = findVariantFromQuickSelection(product, selectedColorName, quickSize.value)
    const image = resolveQuickVariantImage(product, variant, selectedColorName)
    return isSameImage(image, currentImage)
  })

  if (matchedIndex >= 0) {
    quickColorIndex.value = matchedIndex
    return
  }

  const imageIndex = quickPreviewImages.value.findIndex((img) => isSameImage(img, currentImage))
  if (imageIndex >= 0 && imageIndex < product.colors.length) {
    quickColorIndex.value = imageIndex
  }
}

const quickPrevImage = () => {
  const total = quickPreviewImages.value.length
  if (total <= 1) return
  quickImageIndex.value = (quickImageIndex.value - 1 + total) % total
  syncQuickColorWithImage()
  triggerQuickImageAnimation()
}

const quickNextImage = () => {
  const total = quickPreviewImages.value.length
  if (total <= 1) return
  quickImageIndex.value = (quickImageIndex.value + 1) % total
  syncQuickColorWithImage()
  triggerQuickImageAnimation()
}

const selectQuickColor = (index) => {
  quickColorIndex.value = index
  const product = selectedProduct.value
  if (!product) return

  const selectedColorName = String(product?.colors?.[index]?.name || '').trim()
  if (!selectedColorName) {
    if (index >= 0 && index < quickPreviewImages.value.length) {
      quickImageIndex.value = index
      triggerQuickImageAnimation()
    }
    return
  }
  const variant = findVariantFromQuickSelection(product, selectedColorName, quickSize.value)
  const colorImage = resolveQuickVariantImage(product, variant, selectedColorName)
  if (!colorImage) {
    if (index >= 0 && index < quickPreviewImages.value.length) {
      quickImageIndex.value = index
      triggerQuickImageAnimation()
    }
    return
  }

  const imageIndex = quickPreviewImages.value.findIndex((img) => isSameImage(img, colorImage))
  if (imageIndex >= 0) {
    quickImageIndex.value = imageIndex
    triggerQuickImageAnimation()
    return
  }

  const colorImageName = normalizeImageKey(colorImage).split('/').pop() || ''
  if (colorImageName) {
    const looseIndex = quickPreviewImages.value.findIndex((img) => {
      const currentName = normalizeImageKey(img).split('/').pop() || ''
      return currentName && currentName === colorImageName
    })
    if (looseIndex >= 0) {
      quickImageIndex.value = looseIndex
      triggerQuickImageAnimation()
      return
    }
  }

  if (index >= 0 && index < quickPreviewImages.value.length) {
    quickImageIndex.value = index
    triggerQuickImageAnimation()
  }
}

watch(quickPreviewImages, (images) => {
  if (!images.length) {
    quickImageIndex.value = 0
    return
  }
  if (quickImageIndex.value >= images.length) quickImageIndex.value = 0
})

watch(quickImageIndex, () => {
  syncQuickColorWithImage()
})

const quickDecrease = () => {
  quickQty.value = Math.max(1, quickQty.value - 1)
}

const quickIncrease = () => {
  quickQty.value += 1
}

const isSameColor = (left = '', right = '') => {
  const normalize = (value = '') => String(value)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')
    .trim()

  return normalize(left) === normalize(right)
}

const findVariantFromQuickSelection = (product, selectedColorName, selectedSizeName) => {
  const variants = Array.isArray(product?.variants) ? product.variants : []
  if (!variants.length) return null

  const exact = variants.find((variant) => {
    const variantColor = String(variant?.mauSac?.tenMau || variant?.mauSac?.tenMauSac || variant?.mauSac?.name || variant?.tenMau || variant?.tenMauSac || '').trim()
    const variantSize = String(variant?.kichThuoc?.tenKichThuoc || variant?.kichThuoc?.name || variant?.tenKichThuoc || variant?.size || '').trim()
    const sameColor = !selectedColorName || isSameColor(variantColor, selectedColorName)
    const sameSize = !selectedSizeName || variantSize === selectedSizeName
    return sameColor && sameSize
  })
  if (exact) return exact

  return variants.find((variant) => {
    const variantSize = String(variant?.kichThuoc?.tenKichThuoc || variant?.kichThuoc?.name || variant?.tenKichThuoc || variant?.size || '').trim()
    return selectedSizeName && variantSize === selectedSizeName
  }) || variants[0]
}

const resolveQuickVariantImage = (product, variant, selectedColorName = '') => {
  const productCode = getQuickProductCode(product)
  const fallbackColorName = String(
    selectedColorName
    || variant?.mauSac?.tenMau
    || variant?.mauSac?.tenMauSac
    || variant?.mauSac?.name
    || variant?.tenMau
    || variant?.tenMauSac
    || ''
  ).trim()

  if (isCuratedQuickProductCode(productCode)) {
    const curatedVariantImage = fallbackImageForVariant({
      id: Number(variant?.id || product?.id || 0),
      maSanPham: productCode,
      tenSanPham: product?.name,
      tenMauSac: fallbackColorName,
      maChiTietSanPham: variant?.maSanPhamChiTiet || variant?.maChiTiet || variant?.ma || ''
    })
    if (curatedVariantImage) return curatedVariantImage
  }

  const variantImage = pickImageValue([
    variant,
    variant?.anh,
    variant?.hinhAnh,
    variant?.image,
    variant?.imageUrl,
    variant?.duongDanAnh
  ])
  if (variantImage) return variantImage

  if (Array.isArray(product?.colorImageEntries) && product.colorImageEntries.length) {
    const byId = product.colorImageEntries.find((entry) => Number(entry?.colorId) === Number(variant?.mauSac?.id || 0))
    if (byId?.image) return byId.image

    const byName = product.colorImageEntries.find((entry) => isSameColor(entry?.colorName || '', selectedColorName))
    if (byName?.image) return byName.image
  }

  if (selectedColorName && Array.isArray(product?.colors)) {
    const idx = product.colors.findIndex((color) => isSameColor(color?.name || '', selectedColorName))
    if (idx >= 0 && Array.isArray(product?.images) && product.images[idx]) {
      return product.images[idx]
    }
  }

  // Fallback: match color name to static image filenames by keyword
  if (selectedColorName) {
    const code = getQuickProductCode(product)
    const staticImages = getStaticQuickImagesForProduct(product)
    if (staticImages.length) {
      const normalizedColor = normalizeColorKeyword(selectedColorName)
      const keyword = colorKeywordMap[normalizedColor]
        || Object.entries(colorKeywordMap).find(([k]) => normalizedColor.includes(k))?.[1]
        || ''
      if (keyword) {
        const matched = staticImages.find((img) => String(img || '').toLowerCase().includes(keyword))
        if (matched) return matched
      }
    }

    if (isCuratedQuickProductCode(code)) {
      const fbImg = fallbackImageForVariant({
        id: product.id,
        maSanPham: code,
        tenSanPham: product.name,
        tenMauSac: selectedColorName,
      })
      if (fbImg) return fbImg
    }
  }

  return String(product?.images?.[0] || product?.img || '').trim() || fallbackImageFor(product?.id, getQuickProductCode(product), product?.name)
}

const quickAddToCart = () => {
  if (!selectedProduct.value?.id) return

  const selectedColorName = Array.isArray(selectedProduct.value.colors)
    ? String(selectedProduct.value.colors[quickColorIndex.value]?.name || '').trim()
    : ''
  const selectedSizeName = String(quickSize.value || '').trim()
  const selectedVariant = findVariantFromQuickSelection(selectedProduct.value, selectedColorName, selectedSizeName)

  if (Array.isArray(selectedProduct.value.colors) && selectedProduct.value.colors.length > 0 && !selectedColorName) {
    toast('Vui lòng chọn màu trước khi thêm vào giỏ')
    return
  }

  if (Array.isArray(selectedProduct.value.sizes) && selectedProduct.value.sizes.length > 0 && !selectedSizeName) {
    toast('Vui lòng chọn size trước khi thêm vào giỏ')
    return
  }

  let variantImage = ''
  try {
    variantImage = resolveQuickVariantImage(selectedProduct.value, selectedVariant, selectedColorName)
  } catch {
    variantImage = selectedProduct.value?.img || ''
  }
  // Use fallbackImageForVariant only when no variant image was resolved
  if (!variantImage) {
    const fbImg = fallbackImageForVariant({
      id: selectedProduct.value.id,
      maSanPham: getQuickProductCode(selectedProduct.value),
      tenSanPham: selectedProduct.value.name,
      tenMauSac: selectedColorName,
    })
    variantImage = fbImg || selectedProduct.value?.img || ''
  }
  const cartKey = (selectedColorName || selectedSizeName)
    ? `${selectedProduct.value.id}__${selectedColorName}__${selectedSizeName}`
    : String(selectedProduct.value.id)

  addToCart(cartKey, quickQty.value, {
    image: variantImage,
    name: selectedProduct.value.name,
    color: selectedColorName,
    size: selectedSizeName,
    price: Number(selectedVariant?.giaBan || selectedVariant?.price || selectedProduct.value.price || 0),
    actionLabel: 'Xem giỏ hàng'
  })

  const variantsObj = readCartVariantsObject()
  variantsObj[cartKey] = {
    color: selectedColorName,
    size: selectedSizeName,
    spctId: selectedVariant?.id || null,
    image: variantImage
  }
  writeCartVariantsObject(variantsObj)

  notifyCartUpdated()
  closeModal()
}

const loadQuickPromoVouchers = async () => {
  try {
    const normalizeVoucher = (voucher) => ({
      ...normalizeVoucherData(voucher),
      id: voucher?.id ?? voucher?.phieuGiamGiaId ?? voucher?.maPhieuGiamGia ?? voucher?.maPhieu
    })

    let payload = []
    try {
      const response = await getActiveVouchers()
      const data = response?.data
      payload = Array.isArray(data) ? data : (Array.isArray(data?.content) ? data.content : [])
      if (!payload.length) {
        const fallbackResponse = await getAllVouchers()
        const fallbackData = fallbackResponse?.data
        payload = Array.isArray(fallbackData) ? fallbackData : (Array.isArray(fallbackData?.content) ? fallbackData.content : [])
      }
    } catch {
      const response = await getAllVouchers()
      const data = response?.data
      payload = Array.isArray(data) ? data : (Array.isArray(data?.content) ? data.content : [])
    }

    quickVouchers.value = payload.map(normalizeVoucher).filter((voucher) => voucher.maPhieuGiamGia)
  } catch {
    quickVouchers.value = []
  }
}

const quickPromoLines = computed(() => {
  return quickVouchers.value.slice(0, 3).map((voucher) => {
    const val = Number(voucher.giaTriGiamGia || 0)
    const discountLabel = voucher.hinhThucGiam
      ? `giảm ${val}%`
      : `giảm ${val >= 1000 ? Math.round(val / 1000) + 'K' : val + 'đ'}`
    return {
      code: voucher.maPhieuGiamGia,
      discountLabel,
      minLabel: VND(voucher.hoaDonToiThieu || 0)
    }
  })
})

onUnmounted(() => {
  if (toastTimer) {
    clearTimeout(toastTimer)
    toastTimer = null
  }
})


</script>

<template>
<div class="home-root">

<SiteNav :cart-count="cartCount" @require-filter="filterBy" @require-anchor="handleNavAnchor" />

<!-- Hero Banner -->
<section class="dw-hero">
  <div class="container">
    <div class="dw-hero__viewport">
      <div class="dw-hero__track">
        <Transition :name="heroTransitionName" mode="out-in">
          <article v-if="activeHeroSlide" :key="activeHeroSlide.id" class="dw-hero__slide is-active">
            <div class="dw-hero__content">
              <div class="dw-hero__copy">
                <span class="dw-hero__tag">{{ activeHeroSlide.tag }}</span>
                <h1>
                  <template v-if="activeHeroSlide.titleMain && activeHeroSlide.titleDrop">
                    <span class="dw-hero__title-main">{{ activeHeroSlide.titleMain }}</span>
                    <span class="dw-hero__title-drop">{{ activeHeroSlide.titleDrop }}</span>
                  </template>
                  <template v-else>
                    {{ activeHeroSlide.title }}
                  </template>
                </h1>
                <p>{{ activeHeroSlide.subtitle }}</p>
                <button class="dw-hero__cta" type="button" @click="handleHeroAction(activeHeroSlide)">Khám phá ngay →</button>
              </div>
              <div class="dw-hero__media" aria-hidden="true">
                <div class="dw-hero__main-tile">
                  <img :src="activeHeroSlide.heroMain" :alt="activeHeroSlide.title" class="dw-hero__img" />
                </div>
                <div class="dw-hero__side-tiles">
                  <img :src="activeHeroSlide.heroSideTop" :alt="`${activeHeroSlide.title} top`" class="dw-hero__img dw-hero__img--side" />
                  <img :src="activeHeroSlide.heroSideBottom" :alt="`${activeHeroSlide.title} bottom`" class="dw-hero__img dw-hero__img--side" />
                </div>
              </div>
            </div>
          </article>
        </Transition>
        <button class="dw-hero__arrow dw-hero__arrow--prev" type="button" @click="prevHeroSlide" aria-label="Banner trước">
          ←
        </button>
        <button class="dw-hero__arrow dw-hero__arrow--next" type="button" @click="nextHeroSlide" aria-label="Banner tiếp theo">
          →
        </button>
      </div>
    </div>
  </div>
  <div class="container dw-hero-benefits" aria-label="Lợi ích mua sắm">
    <article v-for="item in heroBenefits" :key="item.title" class="dw-hero-benefit-card">
      <span class="dw-hero-benefit-icon">
        <component :is="item.icon" :size="18" />
      </span>
      <div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.text }}</p>
      </div>
    </article>
  </div>
</section>

<!-- Categories -->
<section id="category">
<div class="container">

<h2 class="dw-heading">Danh mục nổi bật</h2>

<div class="dw-category-grid">
  <button class="dw-category-card" type="button" @click="focusCategory('Hoodie')">
    <img :src="img20" alt="Hoodie" />
    <span class="dw-category-label">Hoodie</span>
  </button>

  <button class="dw-category-card" type="button" @click="focusCategory('Bomber')">
    <img :src="img13" alt="Bomber" />
    <span class="dw-category-label">Bomber</span>
  </button>

  <button class="dw-category-card" type="button" @click="focusCategory('Coach')">
    <img :src="img17" alt="Coach" />
    <span class="dw-category-label">Coach</span>
  </button>
</div>
</div>
</section>

<!-- Voucher / Promo Section -->
<section id="voucher-promo" class="voucher-section">
  <div class="container">
    <h2 class="dw-heading">Ưu đãi dành cho bạn</h2>
    <div class="voucher-cards" v-if="quickVouchers.length">
      <div v-for="voucher in quickVouchers.slice(0, 4)" :key="voucher.maPhieuGiamGia" class="voucher-card">
        <div class="voucher-left">
          <div class="voucher-discount">
            {{ voucher.hinhThucGiam ? `${Number(voucher.giaTriGiamGia || 0)}%` : `${Number(voucher.giaTriGiamGia || 0) >= 1000 ? Math.round(Number(voucher.giaTriGiamGia || 0) / 1000) + 'K' : Number(voucher.giaTriGiamGia || 0) + 'đ'}` }}
          </div>
          <div class="voucher-label">GIẢM</div>
        </div>
        <div class="voucher-right">
          <div class="voucher-code">{{ voucher.maPhieuGiamGia }}</div>
          <div class="voucher-condition">Đơn từ {{ VND(voucher.hoaDonToiThieu || 0) }}</div>
        </div>
      </div>
    </div>
    <div v-else class="voucher-empty">
      <p>Hiện chưa có ưu đãi khả dụng. Hãy quay lại sau nhé!</p>
    </div>
  </div>
</section>

<!-- Best sellers -->
<section id="best" :class="{ 'section-pulse': sectionPulse === 'best' }">
<div class="container">

<div class="dw-section-header">
<h2 class="dw-heading">Sản phẩm bán chạy</h2>
<div id="sanpham-nav" class="dw-filters">
<button :class="['dw-filter', { active: !activeFilter }]" @click="clearFilter()">Tất cả</button>
<button :class="['dw-filter', { active: activeFilter === 'Hoodie' }]" @click="focusCategory('Hoodie')">Hoodie</button>
<button :class="['dw-filter', { active: activeFilter === 'Bomber' }]" @click="focusCategory('Bomber')">Bomber</button>
<button :class="['dw-filter', { active: activeFilter === 'Coach' }]" @click="focusCategory('Coach')">Coach</button>
</div>
</div>

<div class="cards">
<article
v-for="(p, index) in filteredBest"
:key="p.id"
class="card"
@click="openProductDetail(p.id)"
style="cursor:pointer">

<div class="thumb">
  <img :src="p.img" alt="" />
  <span v-if="index < 3" class="top-seller-badge">TOP #{{ index + 1 }}</span>
  <span v-if="p.tag" class="thumb-badge">{{ p.tag }}</span>
  <div class="overlay">
    <button class="overlay-icon-btn" type="button" @click.stop="openModal(p)" aria-label="Xem nhanh">
      <Eye :size="16" />
    </button>
    <button class="overlay-icon-btn" type="button" @click.stop="openProductDetail(p.id)" aria-label="Mở chi tiết sản phẩm">
      <ShoppingCart :size="16" />
    </button>
  </div>
</div>

<div class="body">
<div class="title">{{ p.name }}</div>
<div class="meta">
<span class="cat-label">{{ p.cat }}</span>
<span class="price">
<b>{{ VND(p.price) }}</b>
<span v-if="p.old" class="strike">{{ VND(p.old) }}</span>
<span v-if="p.old" class="discount-badge">-{{ Math.round(100 - (p.price / p.old) * 100) }}%</span>
</span>
</div>
</div>

<div class="footer">
<span v-if="p.tag" class="chip">{{ p.tag }}</span>
<span class="sold-count">Đã bán {{ p.sold > 999 ? (p.sold / 1000).toFixed(1) + 'k' : (p.sold || 0) }}</span>
</div>

</article>
</div>

</div>
</section>


<CustomerFooter />

</div>

<!-- Toast -->

<div class="toast" :class="{ show: toastVisible }">
  {{ toastMessage }}
</div>

<transition name="quick-modal-fade">
<div v-if="selectedProduct" class="quick-modal" @click.self="closeModal">
  <div class="quick-modal-content">
    <button class="quick-close" @click="closeModal">✕</button>

    <div class="quick-image-side">
      <img :key="quickActiveImage" :src="quickActiveImage" class="quick-image" :class="{ 'is-switching': quickImageAnimating }" />
      <button v-if="quickPreviewImages.length > 1" class="quick-nav quick-prev" type="button" aria-label="Ảnh trước" @click="quickPrevImage">‹</button>
      <button v-if="quickPreviewImages.length > 1" class="quick-nav quick-next" type="button" aria-label="Ảnh sau" @click="quickNextImage">›</button>
    </div>

    <div class="quick-info-side">
      <h2>{{ selectedProduct.name }}</h2>
      <p class="quick-sku">Mã sản phẩm: {{ getQuickProductCode(selectedProduct) }}</p>

      <div class="quick-price">{{ VND(selectedProduct.price) }}</div>

      <div v-if="Array.isArray(selectedProduct.colors) && selectedProduct.colors.length >= 1" class="quick-row">
        <span>Màu sắc:</span>
        <b>{{ selectedProduct.colors[quickColorIndex]?.name || '' }}</b>
      </div>
      <div v-if="Array.isArray(selectedProduct.colors) && selectedProduct.colors.length >= 1" class="quick-swatches">
        <button
          v-for="(color, index) in selectedProduct.colors"
          :key="`${selectedProduct.id}-${color.name}`"
          class="quick-swatch"
          :class="{ active: quickColorIndex === index }"
          :style="{ background: color.hex }"
          @click="selectQuickColor(index)"
        ></button>
      </div>

      <div class="quick-row"><span>Kích thước:</span> <b>{{ quickSize }}</b></div>
      <div class="quick-sizes">
        <button
          v-for="size in (Array.isArray(selectedProduct.sizes) && selectedProduct.sizes.length ? selectedProduct.sizes : QUICK_SIZES)"
          :key="size"
          type="button"
          class="quick-size"
          :class="{ active: quickSize === size }"
          @click="quickSize = size; selectQuickColor(quickColorIndex)"
        >
          {{ size }}
        </button>
      </div>

      <div class="quick-promo-box">
        <div class="quick-promo-title">ƯU ĐÃI ONLINE</div>
        <ul>
          <li v-for="line in quickPromoLines" :key="line.code">
            Nhập mã <b>{{ line.code }}</b> {{ line.discountLabel }} đơn từ {{ line.minLabel }}
          </li>
          <li v-if="!quickPromoLines.length" class="quick-promo-empty">Hiện chưa có ưu đãi online khả dụng.</li>
        </ul>
      </div>

      <div class="quick-action-row">
        <div class="quick-qty">
          <button type="button" @click="quickDecrease">−</button>
          <strong>{{ quickQty }}</strong>
          <button type="button" @click="quickIncrease">+</button>
        </div>
        <button class="btn" type="button" @click="quickAddToCart">THÊM VÀO GIỎ</button>
      </div>

      <button class="quick-detail-link" type="button" @click="openProductDetail(selectedProduct.id)">Xem chi tiết »</button>
    </div>
  </div>
</div>
</transition>
</template>

<style scoped>
@import "./home.css";

/* Fix header overflow issues */

.home-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
header {
  overflow: visible !important;
}

header .container {
  overflow: visible !important;
}

.nav {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 20px;
  overflow: visible !important;
}

.dropdown-panel {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
}

.tile-content{
  display:flex;
  justify-content:space-between;
  align-items:center;
  width:100%;
}

/* Portrait category tile: image on top, text below */
a.tile {
  display: flex !important;
  flex-direction: column !important;
  align-items: stretch !important;
  padding: 0 !important;
  overflow: hidden !important;
  min-height: 260px !important;
  text-decoration: none !important;
  color: inherit !important;
}

a.tile * {
  text-decoration: none !important;
}

a.tile h3, a.tile span {
  color: inherit !important;
}

.cat-img{
  width:100% !important;
  height:190px !important;
  object-fit:cover;
  object-position: center 20%;
  border-radius:0;
  display:block;
  flex-shrink: 0;
}

.tile-text{
  padding: 14px 16px;
  flex: 1;
}

.category-photo{
  grid-column: span 1;
  border-radius:16px;
  overflow:hidden;
  height:120px;
}

.category-photo img{
  width:100%;
  height:100%;
  object-fit:cover;
}


.cart-foot{
  border-top:1px solid #eee;
  padding:18px;
  background:white;

  display:grid;
  grid-template-columns: 1fr auto;
  align-items:center;
  gap:20px;
}

.totals{
  display:flex;
  justify-content:space-between;
  align-items:center;
  font-size:15px;
}

.cart-actions{
  display:flex;
  gap:14px;
}

.checkout{
  min-width:120px;
  height:46px;
}

.continue{
  min-width:120px;
  height:46px;
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
  content: '';
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
  box-shadow: 0 10px 30px rgba(0,0,0,.15);
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
.img-row{
  display:flex;
  gap:12px;
  align-items:center;
  margin-top:10px;
}

.img-row img{
  height:28px;
  object-fit:contain;
}
.cart-img{
  width:60px;
  height:60px;
  object-fit:cover;
  border-radius:10px;
  background:#f5f5f5;
}
.dropdown{
  position:relative;
}

.dropdown-panel{
  position:absolute;
  top:100%;
  left:0;
  display:none;
  z-index:50;
}

.menu > *{
  white-space:nowrap;
}

.menu a{
  text-decoration:none;
  color:inherit;
  font-weight:500;
}
.menu > div:hover .dropdown-panel{
  display:block;
}
.cart-btn{
  position:relative;
}
.brand{
  flex:0 0 auto;
}

.menu{
  display:flex;
  align-items:center;
  gap:24px;
}
.actions{
  display:flex;
  align-items:center;
  gap:10px;
}
.cart-count{
  position:absolute;
  top:-6px;
  right:-6px;
  background:#ff3b30;
  color:white;
  font-size:11px;
  border-radius:50%;
  padding:2px 6px;
}
.actions{
  display:flex;
  align-items:center;
  gap:10px;
}

.interactive-pill {
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease;
}

.interactive-pill:hover {
  transform: translateY(-1px);
}

.interactive-pill.active {
  background: #111827 !important;
  color: #fff !important;
  border-color: #111827 !important;
}

.style-lab {
  padding: 22px 0;
}

.style-lab-wrap {
  border-radius: 24px;
  border: 1px solid #e7e7e7;
  background: linear-gradient(135deg, #fff8f1 0%, #f2f8ff 60%, #fff 100%);
  box-shadow: 0 18px 40px rgba(17, 24, 39, 0.08);
  padding: 22px;
  display: grid;
  gap: 14px;
}

.style-lab-wrap h3 {
  margin: 8px 0 6px;
  font-size: 30px;
  line-height: 1.1;
}

.style-lab-wrap p {
  margin: 0;
  color: #475569;
}

.style-moods,
.style-lab-actions,
.tier-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.section-pulse {
  animation: sectionPulse 0.65s ease;
}

@keyframes sectionPulse {
  0% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-6px);
  }
  100% {
    transform: translateY(0);
  }
}

.reveal {
  opacity: 0;
  transform: translateY(26px);
  animation: revealIn 0.7s ease forwards;
}

@keyframes revealIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 640px) {
  .style-lab-wrap h3 {
    font-size: 24px;
  }
}
</style>
