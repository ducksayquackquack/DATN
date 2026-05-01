<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import {
  Bot,
  Check,
  CheckCheck,
  CircleDashed,
  ExternalLink,
  Facebook,
  Instagram,
  MessageCircleMore,
  Minus,
  Plus,
  SendHorizonal,
  ShoppingBag,
  Sparkles,
  X
} from "lucide-vue-next"
import {
  getChatHistory,
  getChatbotStatus,
  requestHumanSupport,
  sendChatMessage
} from "../../services/chatbotService"
import { searchChatProducts } from "../../services/chatProductService"
import { getAllSanPham } from "../../services/sanPhamService"
import { getAllKhachHang } from "../../services/KhachHangService"
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
import img12 from "../../assets/img/Jackets/bomber/bomber-astronaut/IMG_4435.PNG?url"
import img13 from "../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/IMG_4437.PNG?url"
import img14 from "../../assets/img/Jackets/bomber/bomber-windbreaker/IMG_4432.PNG?url"
import img15 from "../../assets/img/Jackets/coach/coach-leopard/IMG_4445.PNG?url"
import img16 from "../../assets/img/Jackets/coach/coach-longsleeve/IMG_4442.PNG?url"
import img17 from "../../assets/img/Jackets/coach/coach-tiger-stripe/IMG_4446.PNG?url"
import img18 from "../../assets/img/Jackets/hoodie/hoodie-camo/IMG_4450.PNG?url"
import img19 from "../../assets/img/Jackets/hoodie/hoodie-zip-boxy/IMG_4452.PNG?url"
import img20 from "../../assets/img/Jackets/hoodie/hoodie-zip-silk/IMG_4447.PNG?url"

const route = useRoute()
const router = useRouter()

let historyPollingTimer = null

const CART_UPDATED_EVENT = "dirtywave:cart-updated"
const BOT_NAME = "DirtyWave Support"

const CONTACTS = [
  { label: "Zalo", href: "https://zalo.me/xxxxxxxxxx", icon: ExternalLink, className: "is-zalo" },
  { label: "Instagram", href: "https://instagram.com/dirtywave.official", icon: Instagram, className: "is-instagram" },
  { label: "Facebook", href: "https://facebook.com/dirtywave", icon: Facebook, className: "is-facebook" }
]

const faqSuggestions = [
  "Bomber đen dưới 700k",
  "Mình cao 1m68 nặng 58kg mặc size gì?",
  "Đơn hàng của tôi đang ở đâu?"
]

const fallbackImages = [img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11, img12, img13, img14, img15, img16, img17, img18, img19, img20]

const supportMode = ref("BOT")
const supportLabel = ref("Bot đang hỗ trợ")
const isOpen = ref(false)
const launcherHovered = ref(false)
const isTyping = ref(false)
const input = ref("")
const bodyRef = ref(null)
const botOnline = ref(typeof navigator !== "undefined" ? navigator.onLine : true)
const chatBackendIssue = ref("")
const lastBackendErrorLogAt = ref(0)
const messages = ref([])
const sessionId = ref(loadOrCreateSessionId())
const sessionMemory = ref(loadSessionMemory())
const productQty = ref(loadProductQty())
const sessionStatus = ref("OPEN")
const visibleMessages = computed(() => messages.value.filter((message) => !message.hidden))
const chatCatalog = ref([])
const chatCatalogLoaded = ref(false)
const chatboxWidth = ref(Number(localStorage.getItem("dirtywave_chatbox_width") || 410))
const chatboxHeight = ref(Number(localStorage.getItem("dirtywave_chatbox_height") || 680))
const resizingState = ref(null)
let cachedCustomerProfile = null

const MIN_CHATBOX_WIDTH = 340
const MIN_CHATBOX_HEIGHT = 520

const chatboxStyle = computed(() => {
  const maxWidth = Math.max(MIN_CHATBOX_WIDTH, window.innerWidth - 24)
  const maxHeight = Math.max(MIN_CHATBOX_HEIGHT, window.innerHeight - 110)
  const width = Math.min(maxWidth, Math.max(MIN_CHATBOX_WIDTH, Number(chatboxWidth.value || 410)))
  const height = Math.min(maxHeight, Math.max(MIN_CHATBOX_HEIGHT, Number(chatboxHeight.value || 680)))

  return {
    width: `${width}px`,
    height: `${height}px`
  }
})

const hiddenRoutes = computed(() => {
  const path = route.path || ""
  return (
    path.startsWith("/auth") ||
    path.includes("/login") ||
    path.includes("/register") ||
    path.startsWith("/admin") ||
    path.startsWith("/employee")
  )
})

const launcherTitle = computed(() =>
  launcherHovered.value ? "Liên hệ nhanh với DirtyWave" : "Mở chat với DirtyWave Assistant"
)

function getGuestIdentity() {
  const key = "dirtywave_guest_id"
  let guestId = localStorage.getItem(key)

  if (!guestId) {
    guestId = crypto.randomUUID()
    localStorage.setItem(key, guestId)
  }

  return guestId
}

function getIdentityKey() {
  const userId = String(localStorage.getItem("userId") || "").trim()
  const email = String(
    localStorage.getItem("userEmail") ||
    localStorage.getItem("email") ||
    ""
  ).trim().toLowerCase()
  const phone = String(
    localStorage.getItem("userPhone") ||
    localStorage.getItem("phone") ||
    ""
  ).trim()

  return userId || email || phone || getGuestIdentity()
}

function buildSessionCode(identity, withTimestamp = false) {
  return withTimestamp
    ? `dw-customer-${identity}-${Date.now()}`
    : `dw-customer-${identity}`
}

function loadOrCreateSessionId() {
  const identity = getIdentityKey()
  const key = `dirtywave_chat_session_id:${identity}`

  const found = localStorage.getItem(key)
  if (found) return found

  const created = buildSessionCode(identity)
  localStorage.setItem(key, created)
  return created
}

function loadSessionMemory() {
  const identity = getIdentityKey()
  const key = `dirtywave_chat_memory:${identity}`

  try {
    return JSON.parse(localStorage.getItem(key) || "[]")
  } catch {
    return []
  }
}

function saveSessionMemory() {
  const identity = getIdentityKey()
  const key = `dirtywave_chat_memory:${identity}`
  localStorage.setItem(key, JSON.stringify(sessionMemory.value))
}

function loadProductQty() {
  try {
    return JSON.parse(sessionStorage.getItem("dirtywave_chat_product_qty") || "{}")
  } catch {
    return {}
  }
}

function saveProductQty() {
  sessionStorage.setItem("dirtywave_chat_product_qty", JSON.stringify(productQty.value))
}

function safeJsonParse(value, fallback = null) {
  if (!value) return fallback
  try {
    return typeof value === "string" ? JSON.parse(value) : value
  } catch {
    return fallback
  }
}

const getFallbackImage = (id) => {
  const n = Number(id || 1)
  return fallbackImages[(Math.max(n, 1) - 1) % fallbackImages.length]
}

const normalizeSearchText = (value = "") =>
  String(value || "")
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/\s+/g, " ")
    .trim()

const splitSearchTokens = (value = "") =>
  normalizeSearchText(value)
    .split(/[^a-z0-9]+/)
    .filter((token) => token.length >= 2)

const extractListFromPayload = (payload) => {
  if (Array.isArray(payload)) return payload
  if (!payload || typeof payload !== "object") return []

  const candidates = [
    payload?.content,
    payload?.items,
    payload?.data,
    payload?.data?.content,
    payload?.data?.items,
    payload?.data?.data,
    payload?.data?.data?.content,
    payload?.result,
    payload?.result?.content
  ]

  for (const candidate of candidates) {
    if (Array.isArray(candidate)) return candidate
  }

  return []
}

const isInactiveStatus = (value = "") => {
  const normalized = normalizeSearchText(value)

  return normalized.includes("ngung")
    || normalized.includes("khong hoat dong")
    || normalized.includes("inactive")
    || normalized.includes("disabled")
    || normalized.includes("tat")
}

const isActiveProductStatus = (value = "") => {
  const normalized = normalizeSearchText(value)
  if (isInactiveStatus(normalized)) return false
  return !normalized || normalized.includes("hoat dong") || normalized.includes("active")
}

const isActiveVariantStatus = (value = "") => {
  const normalized = normalizeSearchText(value)
  if (isInactiveStatus(normalized)) return false
  return !normalized || normalized.includes("hoat dong") || normalized.includes("active")
}

const toLabelList = (items = []) => [...new Set(
  (Array.isArray(items) ? items : [])
    .map((item) => String(item || "").trim())
    .filter(Boolean)
)]

function mapCatalogProduct(rawProduct = {}) {
  const id = Number(rawProduct?.id || 0)
  const maSanPham = String(rawProduct?.maSanPham || rawProduct?.ma || "").trim().toUpperCase()
  const name = String(rawProduct?.tenSanPham || rawProduct?.name || maSanPham || "").trim()
  const category = String(rawProduct?.danhMuc?.tenDanhMuc || rawProduct?.loai?.tenLoai || rawProduct?.loai || "").trim()
  const summary = String(rawProduct?.moTa || rawProduct?.summary || "").trim()

  const variants = (Array.isArray(rawProduct?.sanPhamChiTiets) ? rawProduct.sanPhamChiTiets : [])
    .filter((variant) => isActiveVariantStatus(variant?.trangThai || variant?.status))

  const variantPrices = variants
    .map((variant) => Number(variant?.giaBan || 0))
    .filter((value) => Number.isFinite(value) && value > 0)

  const price = variantPrices.length
    ? Math.min(...variantPrices)
    : Number(rawProduct?.giaBan || rawProduct?.gia || 0)

  const stock = variants.length
    ? variants.reduce((sum, variant) => sum + Math.max(0, Number(variant?.soLuong || 0)), 0)
    : Math.max(0, Number(rawProduct?.soLuong || rawProduct?.tongTon || 0))

  const colors = toLabelList(variants.map((variant) =>
    variant?.mauSac?.tenMau || variant?.mauSac?.tenMauSac || variant?.tenMauSac || variant?.tenMau
  ))

  const sizes = toLabelList(variants.map((variant) =>
    variant?.kichThuoc?.tenKichThuoc || variant?.tenKichThuoc || variant?.size
  ))

  const defaultVariant = variants.find((variant) => Number(variant?.soLuong || 0) > 0) || variants[0] || null
  const defaultColor = String(defaultVariant?.mauSac?.tenMau || defaultVariant?.tenMauSac || colors[0] || "").trim()
  const defaultSize = String(defaultVariant?.kichThuoc?.tenKichThuoc || defaultVariant?.tenKichThuoc || sizes[0] || "").trim()

  const directImage = String(
    defaultVariant?.hinhAnh ||
    defaultVariant?.anh ||
    rawProduct?.hinhAnh ||
    rawProduct?.anh ||
    ""
  ).trim()

  const image = directImage || fallbackImageForVariant({
    id,
    maSanPham,
    maChiTietSanPham: String(defaultVariant?.maChiTietSanPham || "").trim().toUpperCase(),
    tenSanPham: name,
    tenMauSac: defaultColor
  }) || getFallbackImage(id || 1)

  return {
    id,
    maSanPham,
    name,
    category,
    summary,
    price: Number(price || 0),
    stock,
    image,
    sizes,
    colors,
    defaultVariantId: defaultVariant?.id ?? null,
    defaultSize,
    defaultColor,
    _search: normalizeSearchText([maSanPham, name, category, summary, colors.join(" "), sizes.join(" ")].join(" "))
  }
}

async function ensureChatCatalogLoaded(forceRefresh = false) {
  if (chatCatalogLoaded.value && !forceRefresh) return

  try {
    const response = await getAllSanPham({ page: 0, size: 1000 })
    const rawList = extractListFromPayload(response?.data)

    chatCatalog.value = rawList
      .filter((item) => isActiveProductStatus(item?.trangThai || item?.status))
      .map(mapCatalogProduct)
      .filter((item) => Number(item?.id || 0) > 0)
    chatCatalogLoaded.value = true
  } catch (error) {
    console.error("Khong dong bo duoc catalog san pham cho chat", error)
    chatCatalog.value = []
    chatCatalogLoaded.value = false
  }
}

function findCatalogProductMatch(product = {}) {
  const productCode = String(product?.maSanPham || product?.code || product?.sku || "").trim().toUpperCase()
  const productNameKey = normalizeSearchText(product?.name || product?.tenSanPham || "")
  const productTokens = splitSearchTokens(productNameKey)

  const exact = chatCatalog.value.find((item) => {
    if (productCode && item?.maSanPham && item.maSanPham === productCode) return true
    return productNameKey && normalizeSearchText(item?.name || "") === productNameKey
  })

  if (exact) return exact

  if (!productTokens.length) return null

  let best = null
  let bestScore = 0

  for (const item of chatCatalog.value) {
    const itemText = normalizeSearchText([item?.name, item?.maSanPham, item?.category].filter(Boolean).join(" "))
    if (!itemText) continue

    const overlap = productTokens.filter((token) => itemText.includes(token)).length
    const score = overlap / productTokens.length

    if (score > bestScore) {
      bestScore = score
      best = item
    }
  }

  return bestScore >= 0.5 ? best : null
}

function searchCatalogProducts({ text = "", keyword = "", color = "", size = "", maxPrice = null } = {}) {
  const normalizedText = normalizeSearchText(text)
  const normalizedKeyword = normalizeSearchText(keyword)
  const normalizedColor = normalizeSearchText(color)
  const normalizedSize = String(size || "").trim().toUpperCase()
  const ceilingPrice = Number(maxPrice)

  return chatCatalog.value.filter((item) => {
    const searchField = item?._search || ""

    if (normalizedKeyword && !searchField.includes(normalizedKeyword)) return false
    if (normalizedText && !searchField.includes(normalizedText)) return false

    if (normalizedColor) {
      const hasColor = (Array.isArray(item?.colors) ? item.colors : []).some((entry) =>
        normalizeSearchText(entry).includes(normalizedColor)
      )
      if (!hasColor) return false
    }

    if (normalizedSize) {
      const hasSize = (Array.isArray(item?.sizes) ? item.sizes : []).some((entry) =>
        String(entry || "").trim().toUpperCase() === normalizedSize
      )
      if (!hasSize) return false
    }

    if (Number.isFinite(ceilingPrice) && ceilingPrice > 0 && Number(item?.price || 0) > ceilingPrice) {
      return false
    }

    return Number(item?.stock || 0) > 0
  })
}

const normalizeProduct = (product = {}) => {
  const catalogMatch = findCatalogProductMatch(product)
  const normalizedId = Number(product.id || 0)
  const normalizedCode = String(
    catalogMatch?.maSanPham ||
    product.maSanPham ||
    product.code ||
    product.sku ||
    ""
  ).trim().toUpperCase()
  const normalizedVariantCode = String(
    product.maChiTietSanPham ||
    product.variantCode ||
    ""
  ).trim().toUpperCase()
  const directImage = String(product.image || product.anh || "").trim()
  const shouldKeepDirectImage =
    directImage && !directImage.startsWith("/chatbot/fallback/")
  const resolvedFallbackImage = fallbackImageForVariant({
    id: normalizedId || Number(catalogMatch?.id || 0),
    maSanPham: normalizedCode,
    maChiTietSanPham: normalizedVariantCode,
    tenSanPham: catalogMatch?.name || product.name || product.tenSanPham || "",
    tenMauSac: catalogMatch?.defaultColor || product.defaultColor || product.tenMauSac || ""
  })

  return {
    ...product,
    id: Number(catalogMatch?.id || normalizedId || 0),
    maSanPham: normalizedCode,
    maChiTietSanPham: normalizedVariantCode,
    tenMauSac: product.tenMauSac || "",
    name: catalogMatch?.name || product.name || product.tenSanPham || normalizedCode || "",
    price: Number(catalogMatch?.price || product.price || product.giaBan || product.giaTu || 0),
stock: Number(
  product.tongTon ??
  product.soLuongTon ??
  product.stock ??
  product.soLuong ??
  catalogMatch?.stock ??
  0
),
    image: shouldKeepDirectImage
      ? directImage
      : (catalogMatch?.image || resolvedFallbackImage || getFallbackImage(normalizedId || 1)),
    sizes: Array.isArray(catalogMatch?.sizes)
      ? catalogMatch.sizes
      : Array.isArray(product.sizes)
      ? product.sizes
      : Array.isArray(product.kichThuoc)
        ? product.kichThuoc
        : [],
    colors: Array.isArray(catalogMatch?.colors)
      ? catalogMatch.colors
      : Array.isArray(product.colors)
      ? product.colors
      : Array.isArray(product.mauSac)
        ? product.mauSac
        : [],
    category: catalogMatch?.category || product.category || product.loai || product.danhMuc || "",
    summary: product.summary || catalogMatch?.summary || product.moTa || "",
    promotionText: product.promotionText || product.promotionName || "",
    promotionCode: product.promotionCode || "",
    discountValue: product.discountValue ?? null,
    discountUnit: product.discountUnit || "",
    defaultVariantId: catalogMatch?.defaultVariantId ?? product.defaultVariantId ?? null,
    defaultSize: catalogMatch?.defaultSize || product.defaultSize || "",
    defaultColor: catalogMatch?.defaultColor || product.defaultColor || ""
  }
}

function clampChatboxSize() {
  const maxWidth = Math.max(MIN_CHATBOX_WIDTH, window.innerWidth - 24)
  const maxHeight = Math.max(MIN_CHATBOX_HEIGHT, window.innerHeight - 110)
  chatboxWidth.value = Math.min(maxWidth, Math.max(MIN_CHATBOX_WIDTH, Number(chatboxWidth.value || 410)))
  chatboxHeight.value = Math.min(maxHeight, Math.max(MIN_CHATBOX_HEIGHT, Number(chatboxHeight.value || 680)))
}

function handleResizeMove(event) {
  if (!resizingState.value) return

  const clientX = Number(event.clientX || 0)
  const clientY = Number(event.clientY || 0)
  if (!Number.isFinite(clientX) || !Number.isFinite(clientY)) return

  const deltaX = clientX - resizingState.value.startX
  const deltaY = clientY - resizingState.value.startY
  const direction = String(resizingState.value.direction || "se")

  const maxWidth = Math.max(MIN_CHATBOX_WIDTH, window.innerWidth - 24)
  const maxHeight = Math.max(MIN_CHATBOX_HEIGHT, window.innerHeight - 110)

  let nextWidth = Number(resizingState.value.startWidth || 410)
  let nextHeight = Number(resizingState.value.startHeight || 680)

  if (direction.includes("e")) nextWidth = resizingState.value.startWidth + deltaX
  if (direction.includes("w")) nextWidth = resizingState.value.startWidth - deltaX
  if (direction.includes("s")) nextHeight = resizingState.value.startHeight + deltaY
  if (direction.includes("n")) nextHeight = resizingState.value.startHeight - deltaY

  chatboxWidth.value = Math.min(maxWidth, Math.max(MIN_CHATBOX_WIDTH, nextWidth))
  chatboxHeight.value = Math.min(maxHeight, Math.max(MIN_CHATBOX_HEIGHT, nextHeight))
}

function stopResizing() {
  if (!resizingState.value) return

  resizingState.value = null
  localStorage.setItem("dirtywave_chatbox_width", String(Math.round(chatboxWidth.value || 410)))
  localStorage.setItem("dirtywave_chatbox_height", String(Math.round(chatboxHeight.value || 680)))

  window.removeEventListener("pointermove", handleResizeMove)
  window.removeEventListener("pointerup", stopResizing)
  window.removeEventListener("pointercancel", stopResizing)
}

function startResizing(event) {
  event.preventDefault()
  clampChatboxSize()

  const clientX = Number(event.clientX || 0)
  const clientY = Number(event.clientY || 0)
  const direction = String(event?.currentTarget?.dataset?.dir || "se")
  resizingState.value = {
    startX: clientX,
    startY: clientY,
    startWidth: Number(chatboxWidth.value || 410),
    startHeight: Number(chatboxHeight.value || 680),
    direction
  }

  window.addEventListener("pointermove", handleResizeMove)
  window.addEventListener("pointerup", stopResizing)
  window.addEventListener("pointercancel", stopResizing)
}

function extractProducts(payload = {}) {
  const meta = safeJsonParse(payload?.metadataJson, {}) || {}

  const raw =
    payload?.products ??
    payload?.sanPhams ??
    payload?.productList ??
    payload?.items ??
    payload?.data?.products ??
    meta?.products ??
    meta?.sanPhams ??
    []

  return Array.isArray(raw) ? raw.map(normalizeProduct) : []
}

function parseMessageMeta(message) {
  const raw = message?.metadataJson
  if (!raw) return {}

  try {
    return typeof raw === "string" ? JSON.parse(raw) : raw
  } catch {
    return {}
  }
}

function normalizeMessageText(value) {
  return String(value || "").trim().replace(/\s+/g, " ").replace(/[?!.，。？！]+$/, "").toLowerCase()
}

function isSameLogicalMessage(localMessage, serverMessage) {
  const sameRole = String(localMessage?.role || "") === String(serverMessage?.role || "")
  const sameText = normalizeMessageText(localMessage?.text) === normalizeMessageText(serverMessage?.text)

  const localTime = new Date(localMessage?.createdAt || 0).getTime()
  const serverTime = new Date(serverMessage?.createdAt || 0).getTime()
  const closeInTime = Math.abs(localTime - serverTime) <= 15000

  // Nếu local message có products thì đừng merge quá dễ,
  // tránh lần bấm thứ 2 bị nuốt mất card
  const localHasProducts = Array.isArray(localMessage?.products) && localMessage.products.length > 0
  const serverHasProducts = Array.isArray(serverMessage?.products) && serverMessage.products.length > 0

  if (localHasProducts !== serverHasProducts) {
    return false
  }

  return sameRole && sameText && closeInTime
}

function hydrateServerMessageWithLocal(localMessage, serverMessage) {
  const localProducts = Array.isArray(localMessage?.products) ? localMessage.products : []
  const serverProducts = Array.isArray(serverMessage?.products) ? serverMessage.products : []

  const localQuickReplies = Array.isArray(localMessage?.quickReplies) ? localMessage.quickReplies : []
  const serverQuickReplies = Array.isArray(serverMessage?.quickReplies) ? serverMessage.quickReplies : []

  return {
    ...serverMessage,
    products: serverProducts.length ? serverProducts : localProducts,
    quickReplies: serverQuickReplies.length ? serverQuickReplies : localQuickReplies,
    sessionStatus: serverMessage?.sessionStatus || localMessage?.sessionStatus || "",
    assignedEmployeeName:
      serverMessage?.assignedEmployeeName || localMessage?.assignedEmployeeName || null
  }
}

function extractPriceCeiling(text = "") {
  const normalized = String(text)
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, " ")
    .trim()

  const matchK = normalized.match(/(?:duoi|toi da|<=?)\s*(\d+)\s*k\b/)
  if (matchK) return Number(matchK[1]) * 1000

  const matchRaw = normalized.match(/(?:duoi|toi da|<=?)\s*(\d{5,9})\b/)
  if (matchRaw) return Number(matchRaw[1])

  return null
}

function extractSizeKeyword(text = "") {
  const normalized = String(text)
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, " ")
    .trim()

  const match = normalized.match(/\b(xs|s|m|l|xl|xxl)\b/)
  return match ? match[1].toUpperCase() : ""
}

function looksLikePromotionQuery(text = "") {
  const normalized = normalizeIntentText(text)
  return normalized.includes("khuyen mai")
    || normalized.includes("voucher")
    || normalized.includes("uu dai")
    || normalized.includes("sale")
}

function looksLikeProductSearch(text = "") {
  const normalized = String(text).toLowerCase()

  if (looksLikePromotionQuery(text)) return false

  const triggers = [
    "sản phẩm", "áo", "bomber", "hoodie", "coach", "jacket", "khoác",
    "dưới", "trên", "size", "màu",
    "xem tất cả", "xem sản phẩm", "tìm", "mẫu", "giá rẻ",
    "xem mẫu", "tiếp tục tìm"
  ]

  return triggers.some((t) => normalized.includes(t))
}
function normalizeIntentText(text = "") {
  return String(text || "")
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/\s+/g, " ")
    .trim()
}

function isProductCountIntent(text = "") {
  const normalized = normalizeIntentText(text)
  if (!normalized) return false

  const mentionsProduct =
    normalized.includes("san pham") || normalized.includes("mat hang")

  const asksForCount =
    normalized.includes("bao nhieu") ||
    normalized.includes("tong cong") ||
    normalized.includes("so luong") ||
    normalized.includes("co may") ||
    normalized.includes("dem")

  return mentionsProduct && asksForCount
}

async function fetchCatalogProductCount() {
  try {
    const response = await getAllSanPham({ page: 0, size: 1 })
    const payload = response?.data
    const totalFromMeta = Number(
      payload?.totalElements ??
      payload?.total ??
      payload?.count ??
      payload?.totalItems ??
      NaN
    )

    if (Number.isFinite(totalFromMeta) && totalFromMeta >= 0) {
      return totalFromMeta
    }

    const list = Array.isArray(payload)
      ? payload
      : (Array.isArray(payload?.content) ? payload.content : [])

    return list.length
  } catch (error) {
    console.error("Khong dem duoc tong so san pham", error)
    return null
  }
}

function normalizeQuickReplyLabel(value = "") {
  const normalized = String(value || "").trim()

  const map = {
    "Mẫu đang sale": "Mẫu màu xám còn hàng",
    "Mẫu màu be còn hàng": "Mẫu màu xám còn hàng"
  }

  return map[normalized] || normalized
}

function normalizeQuickReplies(list = []) {
  if (!Array.isArray(list)) return []

  return [...new Set(
    list
      .map((item) => normalizeQuickReplyLabel(item))
      .filter(Boolean)
  )]
}

async function resolveCustomerProfile() {
  const email = String(
    localStorage.getItem("userEmail") || localStorage.getItem("email") || ""
  ).trim().toLowerCase()

  if (!email) {
    return { name: "", email: "", phone: "" }
  }

  if (cachedCustomerProfile?.email === email) {
    return cachedCustomerProfile
  }

  try {
    const customerRes = await getAllKhachHang(0, 300)
    const customers = Array.isArray(customerRes?.data)
      ? customerRes.data
      : (Array.isArray(customerRes?.data?.content) ? customerRes.data.content : [])

    const matchedCustomer = customers.find((customer) => {
      const customerEmail = String(customer?.taiKhoan?.email || "").trim().toLowerCase()
      return customerEmail === email
    })

    const name = String(
      matchedCustomer?.tenKhachHang ||
      localStorage.getItem("userName") ||
      localStorage.getItem("displayName") ||
      localStorage.getItem("fullName") ||
      localStorage.getItem("username") ||
      email.split("@")[0] ||
      ""
    ).trim()

    const phone = String(
      matchedCustomer?.soDienThoai ||
      localStorage.getItem("userPhone") ||
      localStorage.getItem("phone") ||
      ""
    ).trim()

    if (name) {
      localStorage.setItem("userName", name)
      localStorage.setItem("displayName", name)
      localStorage.setItem("fullName", name)
      localStorage.setItem("username", name)
    }

    if (phone) {
      localStorage.setItem("userPhone", phone)
      localStorage.setItem("phone", phone)
    }

    cachedCustomerProfile = { name, email, phone }
    return cachedCustomerProfile
  } catch (error) {
    console.error("Không resolve được profile khách hàng", error)

    const fallback = {
      name: String(
        localStorage.getItem("userName") ||
        localStorage.getItem("displayName") ||
        localStorage.getItem("fullName") ||
        localStorage.getItem("username") ||
        email.split("@")[0] ||
        ""
      ).trim(),
      email,
      phone: String(
        localStorage.getItem("userPhone") ||
        localStorage.getItem("phone") ||
        ""
      ).trim()
    }

    cachedCustomerProfile = fallback
    return fallback
  }
}

function applySessionState(status, assignedEmployeeName = "") {
  const normalized = String(status || "").trim().toUpperCase()
  if (!normalized) return

  sessionStatus.value = normalized

  if (normalized === "WAITING_EMPLOYEE") {
    supportMode.value = "HUMAN"
    supportLabel.value = "Nhân viên sẽ sớm hỗ trợ"
    return
  }

  if (normalized === "IN_PROGRESS") {
    supportMode.value = "HUMAN"
    supportLabel.value = assignedEmployeeName
      ? `Nhân viên ${assignedEmployeeName} đang hỗ trợ`
      : "Nhân viên đang hỗ trợ"
    return
  }

  if (normalized === "CLOSED") {
    supportMode.value = "BOT"
    supportLabel.value = "Phiên chat đã kết thúc"
    return
  }

  supportMode.value = "BOT"
  supportLabel.value = "Bot đang hỗ trợ"
}

async function fetchSupportStatus() {
  try {
    const { data } = await getChatbotStatus(sessionId.value)
    const nextStatus = String(data?.sessionStatus || "OPEN").trim().toUpperCase()
    sessionStatus.value = nextStatus || "OPEN"
    applySessionState(nextStatus, data?.assignedEmployeeName || "")
    clearBackendIssue()
  } catch (error) {
    setBackendIssue("Không thể kết nối trạng thái chat. Hệ thống sẽ tự thử lại.", error)
    sessionStatus.value = "OPEN"
    supportMode.value = "BOT"
    supportLabel.value = "Bot đang hỗ trợ"
  }
}

function mapServerMessageToUi(message) {
  const senderType = String(message?.senderType || "").toUpperCase()
  const meta = parseMessageMeta(message)
  const messageStatus = String(
    meta?.sessionStatus || meta?.status || ""
  ).trim().toUpperCase()

  let role = "bot"
  if (senderType === "CUSTOMER") role = "user"
  if (senderType === "EMPLOYEE") role = "bot"
  if (senderType === "SYSTEM") role = "bot"
  if (senderType === "BOT") role = "bot"

  const isClosedSystemNotice =
    senderType === "SYSTEM" &&
    messageStatus === "CLOSED"

  let quickReplies = normalizeQuickReplies(meta?.quickReplies || [])

  if (isClosedSystemNotice) {
    quickReplies = []
  }

  return {
    id: `server-${message.id}`,
    serverId: message.id,
    pending: false,
    hidden: isClosedSystemNotice,
    role,
    text: message.content || "",
    statusLabel:
      senderType === "EMPLOYEE"
        ? "Nhân viên đã gửi"
        : senderType === "CUSTOMER"
          ? "Đã gửi"
          : "Đã trả lời",
    deliveryState: "Đã gửi",
    readState: "Đã xem",
    createdAt: message.createdAt || new Date().toISOString(),
    quickReplies,
    products: extractProducts({
      ...message,
      metadataJson: message?.metadataJson
    }),
    sessionStatus: messageStatus,
    assignedEmployeeName: meta?.assignedEmployeeName || null
  }
}

async function syncHistoryFromServer() {
  try {
    const shouldStickToBottom = isNearBottom()
    const { data } = await getChatHistory(sessionId.value)

    const rawServerMessages = Array.isArray(data) ? data.map(mapServerMessageToUi) : []

    if (!rawServerMessages.length) {
      if (!messages.value.length) {
        pushWelcome()
        await syncScroll()
      }
      return
    }

    // Giữ lại products/quickReplies từ local pending nếu server chưa có
    const serverMessages = rawServerMessages.map((serverMsg) => {
      const matchedLocalPending = messages.value.find(
        (localMsg) => localMsg?.pending && isSameLogicalMessage(localMsg, serverMsg)
      )

      return matchedLocalPending
        ? hydrateServerMessageWithLocal(matchedLocalPending, serverMsg)
        : serverMsg
    })

    // Chỉ bỏ local pending khi đã có bản server tương ứng
    const localMessages = messages.value.filter((localMsg) => {
      if (!localMsg?.pending) return true
      return !serverMessages.some((serverMsg) => isSameLogicalMessage(localMsg, serverMsg))
    })

    const merged = [...localMessages]

    for (const serverMsg of serverMessages) {
      const alreadyExists = merged.some((existing) => {
        if (existing?.serverId && serverMsg?.serverId) {
          return String(existing.serverId) === String(serverMsg.serverId)
        }
        return isSameLogicalMessage(existing, serverMsg)
      })

      if (!alreadyExists) {
        merged.push(serverMsg)
      }
    }

    merged.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
    messages.value = merged

    const latestStateMessage = [...merged]
      .reverse()
      .find((item) => String(item?.sessionStatus || "").trim())

    if (latestStateMessage) {
      applySessionState(
        latestStateMessage.sessionStatus,
        latestStateMessage.assignedEmployeeName
      )
    }

    if (shouldStickToBottom) {
      await syncScroll()
    }
  } catch (e) {
    setBackendIssue("Không tải được lịch sử chat lúc này. Vui lòng kiểm tra kết nối.", e)
  }
}

function startHistoryPolling() {
  stopHistoryPolling()
  historyPollingTimer = setInterval(() => {
    syncHistoryFromServer()
  }, 3000)
}

function stopHistoryPolling() {
  if (historyPollingTimer) {
    clearInterval(historyPollingTimer)
    historyPollingTimer = null
  }
}

function handleLauncherEnter() {
  if (isOpen.value) return
  launcherHovered.value = true
}

function handleLauncherLeave() {
  launcherHovered.value = false
}

function isNearBottom() {
  if (!bodyRef.value) return true
  const el = bodyRef.value
  return el.scrollHeight - el.scrollTop - el.clientHeight < 80
}

const currency = (value) =>
  `${new Intl.NumberFormat("vi-VN").format(Number(value || 0))}đ`

const timeLabel = (date) =>
  new Intl.DateTimeFormat("vi-VN", {
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(date))

function scrollToBottom() {
  if (!bodyRef.value) return
  bodyRef.value.scrollTop = bodyRef.value.scrollHeight
}

function setBackendIssue(message, error = null) {
  chatBackendIssue.value = String(message || "Kết nối chat đang gián đoạn.").trim()

  const now = Date.now()
  if (error && now - lastBackendErrorLogAt.value > 10000) {
    lastBackendErrorLogAt.value = now
    console.error("Chat backend issue:", error)
  }
}

function clearBackendIssue() {
  chatBackendIssue.value = ""
}

async function retryBackendConnection() {
  await fetchSupportStatus()
  await syncHistoryFromServer()
}

async function syncScroll() {
  await nextTick()
  scrollToBottom()
}

function pushWelcome() {
  if (messages.value.length) return

  const introText =
    supportMode.value === "HUMAN"
      ? "Xin chào anh/chị 👋 Hiện đang trong giờ làm việc nên nhân viên DirtyWave sẽ trực tiếp hỗ trợ anh/chị. Toàn bộ cuộc trò chuyện vẫn được hiển thị trong cùng khung chat này."
      : "Xin chào anh/chị 👋 Hiện shop đang ngoài giờ làm việc nên em là DirtyWave Assistant sẽ hỗ trợ anh/chị trước. Em có thể tư vấn áo khoác, lọc sản phẩm theo màu, size, giá và gợi ý mẫu phù hợp ngay trong khung chat."

  const quickReplies = normalizeQuickReplies(
    supportMode.value === "HUMAN"
      ? ["Áo bomber", "Hoodie", "Mẫu màu xám còn hàng", "Tư vấn size"]
      : ["Bomber đen dưới 700k", "Bomber size L", "Coach dễ phối đồ", "Mẫu màu xám còn hàng"]
  )

  messages.value.push({
    id: crypto.randomUUID(),
    role: "bot",
    text: introText,
    statusLabel: "Đã gửi",
    deliveryState: "Đã gửi",
    readState: "Đã xem",
    createdAt: new Date().toISOString(),
    quickReplies,
    products: []
  })
}

function pushUserMessage(text) {
  const msg = {
    id: crypto.randomUUID(),
    role: "user",
    text,
    statusLabel: "Đang gửi",
    deliveryState: "Đang gửi",
    readState: "Chưa đọc",
    createdAt: new Date().toISOString(),
    quickReplies: [],
    products: [],
    pending: true,
    failed: false,
    serverId: null
  }

  messages.value.push(msg)
  sessionMemory.value.push({ role: "user", text })
  saveSessionMemory()
}

function updateLatestPendingUserMessage(state = "sent") {
  const target = [...messages.value]
    .reverse()
    .find((item) => item?.role === "user" && item?.pending)

  if (!target) return

  const map = {
    sent: {
      statusLabel: "Đã gửi",
      deliveryState: "Đã gửi",
      readState: "Đã nhận",
      pending: false,
      failed: false
    },
    read: {
      statusLabel: "Đã gửi",
      deliveryState: "Đã gửi",
      readState: "Đã đọc",
      pending: false,
      failed: false
    },
    failed: {
      statusLabel: "Gửi lỗi",
      deliveryState: "Gửi lỗi",
      readState: "Chưa gửi",
      pending: false,
      failed: true
    }
  }

  Object.assign(target, map[state] || map.sent)
}

function pushBotMessage(payload = {}) {
  const payloadStatus = String(payload?.sessionStatus || "").trim().toUpperCase()

  if (payload?.mode) {
    supportMode.value = payload.mode
    supportLabel.value =
      payload.mode === "HUMAN"
        ? "Nhân viên đang hỗ trợ"
        : "Bot đang hỗ trợ"
  }

  if (payloadStatus) {
    applySessionState(payloadStatus, payload?.assignedEmployeeName || "")
  }

  let quickReplies = normalizeQuickReplies(payload?.quickReplies || [])

  if (payloadStatus === "CLOSED" && !quickReplies.length) {
    quickReplies = ["Bắt đầu cuộc trò chuyện mới"]
  }

  const msg = {
    id: crypto.randomUUID(),
    role: "bot",
    text: payload?.message || "Em đã nhận được yêu cầu của anh/chị.",
    statusLabel: payload?.mode === "HUMAN" ? "Đã tiếp nhận" : "Đã trả lời",
    deliveryState: "Đã gửi",
    readState: "Đã xem",
    createdAt: new Date().toISOString(),
    quickReplies,
    products: extractProducts(payload),
    sessionStatus: payloadStatus,
    assignedEmployeeName: payload?.assignedEmployeeName || null,
    pending: true,
    serverId: null,
    preserveProductsOnSync: true
  }

  messages.value.push(msg)
  sessionMemory.value.push({
    role: "bot",
    text: msg.text,
    products: msg.products.map((p) => ({ id: p.id, name: p.name }))
  })
  saveSessionMemory()
}

async function requestHumanSupportFromBot() {
  if (sessionStatus.value === "CLOSED" || isTyping.value) return

  if (!isOpen.value) {
    isOpen.value = true
    await fetchSupportStatus()
    await syncHistoryFromServer()
    if (!messages.value.length) pushWelcome()
    startHistoryPolling()
    await syncScroll()
  }

  try {
    isTyping.value = true

    const profile = await resolveCustomerProfile()

    const { data } = await requestHumanSupport({
      sessionId: sessionId.value,
      customerName: profile.name,
      customerEmail: profile.email,
      customerPhone: profile.phone
    })

    const nextStatus = String(data?.sessionStatus || "WAITING_EMPLOYEE").trim().toUpperCase()
    const responseProducts = extractProducts(data)

    applySessionState(nextStatus, data?.assignedEmployeeName || "")
    clearBackendIssue()

    if (data?.message || data?.quickReplies?.length || responseProducts.length) {
      pushBotMessage({
        ...(data || {}),
        mode: "HUMAN",
        products: responseProducts
      })
    } else {
      pushBotMessage({
        mode: "HUMAN",
        sessionStatus: "WAITING_EMPLOYEE",
        message: "Shop đã ghi nhận yêu cầu. Nhân viên sẽ tiếp nhận và hỗ trợ anh/chị sớm nhất.",
        quickReplies: []
      })
    }
  } catch (error) {
    console.error("Không chuyển được sang nhân viên hỗ trợ", error)
    pushBotMessage({
      mode: "BOT",
      message: "Hiện em chưa chuyển được sang nhân viên. Anh/chị thử lại sau ít phút giúp em nhé.",
      quickReplies: ["Bomber đen dưới 700k", "Hoodie size L", "Tư vấn size"]
    })
  } finally {
    isTyping.value = false
    await syncScroll()
  }
}

async function startNewConversation() {
  stopHistoryPolling()

  const identity = getIdentityKey()
  const sessionKey = `dirtywave_chat_session_id:${identity}`
  const memoryKey = `dirtywave_chat_memory:${identity}`

  const created = buildSessionCode(identity, true)
  localStorage.setItem(sessionKey, created)
  localStorage.removeItem(memoryKey)

  sessionId.value = created
  sessionMemory.value = []
  messages.value = []
  input.value = ""
  isTyping.value = false
  sessionStatus.value = "OPEN"
  supportMode.value = "BOT"
  supportLabel.value = "Bot đang hỗ trợ"

  saveSessionMemory()
  pushWelcome()
  await syncScroll()

  if (isOpen.value) {
    startHistoryPolling()
  }
}

async function openWidget() {
  launcherHovered.value = false
  isOpen.value = true
  await ensureChatCatalogLoaded(true)

  await fetchSupportStatus()
  await syncHistoryFromServer()

  if (!messages.value.length) {
    pushWelcome()
  }

  startHistoryPolling()
  await syncScroll()
}

function closeWidget() {
  isOpen.value = false
  stopHistoryPolling()
}

async function handleFaqSuggestion(question) {
  if (isTyping.value || sessionStatus.value === "CLOSED") return
  input.value = question
  await submitMessage()
}

async function handleQuickReply(value) {
  if (isTyping.value) return
  const normalizedValue = normalizeQuickReplyLabel(value)

  if (normalizedValue === "Bắt đầu cuộc trò chuyện mới") {
    await startNewConversation()
    return
  }

  if (normalizedValue === "Xem giỏ hàng") {
    openCart()
    return
  }

  if (normalizedValue === "Gặp nhân viên hỗ trợ") {
    await requestHumanSupportFromBot()
    return
  }

  if (normalizedValue === "Đặt hàng ngay") {
    openCart()
    return
  }

  if (normalizedValue === "Xem sản phẩm" || normalizedValue === "Tiếp tục tìm mẫu khác") {
    input.value = "Xem tất cả"
    await submitMessage()
    return
  }

  input.value = normalizedValue
  await submitMessage()
}

function goToProduct(productId) {
  router.push(`/product/${productId}`)
  closeWidget()
}

function changeQty(productId, delta) {
  const key = String(productId)
  const current = Number(productQty.value[key] || 1)
  const next = Math.max(1, current + delta)
  productQty.value = { ...productQty.value, [key]: next }
  saveProductQty()
}

function getQty(productId) {
  return Number(productQty.value[String(productId)] || 1)
}

function notifyCartUpdated() {
  window.dispatchEvent(new Event(CART_UPDATED_EVENT))
}

function addToCart(product) {
  if (!product?.id) return

  const key = String(product.id)
  const qty = getQty(product.id)

  const hasMultipleChoices =
    (Array.isArray(product.colors) && product.colors.length > 1) ||
    (Array.isArray(product.sizes) && product.sizes.length > 1)

  const hasDefaultVariant =
    product.defaultVariantId || product.defaultColor || product.defaultSize

  if (hasMultipleChoices && !hasDefaultVariant) {
    messages.value.push({
      id: crypto.randomUUID(),
      role: "bot",
      text: `Mẫu "${product.name}" có nhiều màu hoặc size. Em đưa anh/chị sang trang chi tiết để chọn đúng phiên bản nhé.`,
      statusLabel: "Đã gửi",
      deliveryState: "Đã gửi",
      readState: "Đã xem",
      createdAt: new Date().toISOString(),
      quickReplies: [],
      products: []
    })

    goToProduct(product.id)
    return
  }

  const storedCart = JSON.parse(localStorage.getItem("cart") || "{}")
  storedCart[key] = Number(storedCart[key] || 0) + qty
  localStorage.setItem("cart", JSON.stringify(storedCart))

  if (hasDefaultVariant) {
    const storedVariants = JSON.parse(localStorage.getItem("cartVariants") || "{}")
    storedVariants[key] = {
      spctId: product.defaultVariantId || null,
      color: product.defaultColor || "",
      size: product.defaultSize || ""
    }
    localStorage.setItem("cartVariants", JSON.stringify(storedVariants))
  }

  notifyCartUpdated()

  messages.value.push({
    id: crypto.randomUUID(),
    role: "bot",
    text: `Em đã thêm ${qty} sản phẩm "${product.name}" vào giỏ hàng cho anh/chị rồi ạ.`,
    statusLabel: "Đã gửi",
    deliveryState: "Đã gửi",
    readState: "Đã xem",
    createdAt: new Date().toISOString(),
    quickReplies: ["Xem giỏ hàng", "Tiếp tục tìm mẫu khác"],
    products: []
  })

  syncScroll()
}

function openCart() {
  router.push("/gio-hang")
  closeWidget()
}

async function submitMessage() {
  const text = String(input.value || "").trim()
  if (!text || isTyping.value || sessionStatus.value === "CLOSED") return
  const productCountIntent = isProductCountIntent(text)

  // Push user message and clear input BEFORE opening widget
  // so syncHistoryFromServer inside openWidget doesn't wipe it
  pushUserMessage(text)
  input.value = ""
  isTyping.value = true

  if (!isOpen.value) {
    isOpen.value = true
    await fetchSupportStatus()
    startHistoryPolling()
  }

  await syncScroll()

  try {
    const profile = await resolveCustomerProfile()

    const { data } = await sendChatMessage({
      sessionId: sessionId.value,
      message: text,
      memory: sessionMemory.value.slice(-8),
      customerName: profile.name,
      customerEmail: profile.email,
      customerPhone: profile.phone
    })

    const serverProducts = extractProducts(data)
    let fallbackProducts = []
    let totalCatalogProducts = null

    if (productCountIntent) {
      totalCatalogProducts = await fetchCatalogProductCount()
    }

    if (!serverProducts.length && looksLikeProductSearch(text) && !productCountIntent) {
      try {
        const maxPrice = extractPriceCeiling(text)
        const { keyword, color, size } = buildProductSearchKeyword(text)

        await ensureChatCatalogLoaded(true)
        const catalogProducts = searchCatalogProducts({
          text,
          keyword,
          color,
          size,
          maxPrice
        })

        if (catalogProducts.length) {
          fallbackProducts = catalogProducts.slice(0, 8).map(normalizeProduct)
        } else {
          const productRes = await searchChatProducts({
            q: keyword || "",
            color: color || "",
            maxPrice
          })

          console.log("PRODUCT API RAW =", productRes?.data)

          let fetchedProducts = Array.isArray(productRes?.data)
            ? productRes.data
                .filter((item) => isActiveProductStatus(item?.trangThai || item?.status))
                .map(normalizeProduct)
            : []

          if (size) {
            fetchedProducts = fetchedProducts.filter((product) =>
              Array.isArray(product?.sizes) &&
              product.sizes.some(
                (item) => String(item).trim().toUpperCase() === size
              )
            )
          }

          fallbackProducts = fetchedProducts
        }

        console.log("USER TEXT =", text)
        console.log("PARSED KEYWORD =", {
          keyword,
          color,
          size,
          maxPrice
        })
        console.log("FALLBACK PRODUCTS AFTER FILTER =", fallbackProducts)
      } catch (e) {
        console.error("Fallback search sản phẩm thất bại", e)
      }
    }

    const nextStatus = String(data?.sessionStatus || "").trim().toUpperCase()
    if (nextStatus) {
      applySessionState(nextStatus, data?.assignedEmployeeName || "")
    }
    clearBackendIssue()
    updateLatestPendingUserMessage("sent")

    const isProductIntent = looksLikeProductSearch(text) && !productCountIntent
    const waitingHumanButStillCanSuggest =
      supportMode.value === "HUMAN" &&
      sessionStatus.value === "WAITING_EMPLOYEE" &&
      isProductIntent
    const shouldSuppressBotMessage =
      Boolean(data?.suppressBotMessage) && !waitingHumanButStillCanSuggest
    const finalProducts = productCountIntent
      ? []
      : (serverProducts.length ? serverProducts : fallbackProducts)

    const productCountMessage = Number.isFinite(totalCatalogProducts)
      ? `Hiện tại shop có ${totalCatalogProducts} sản phẩm trong hệ thống.`
      : ""

    const messageText =
      (productCountIntent
        ? productCountMessage
        : String(data?.message || "").trim()) ||
      (finalProducts.length
        ? `Em tìm được ${finalProducts.length} sản phẩm phù hợp cho yêu cầu "${text}":`
        : "")

    const quickReplies = normalizeQuickReplies(
      Array.isArray(data?.quickReplies) && data.quickReplies.length
        ? data.quickReplies
        : (
            productCountIntent
              ? ["Xem sản phẩm", "Bomber đen dưới 700k", "Hoodie size L"]
              : finalProducts.length
              ? ["Xem mẫu rẻ hơn", "Tư vấn size", "Màu đen"]
              : []
          )
    )

    console.log("CHATBOT RESPONSE =", data)
    console.log("SERVER PRODUCTS =", serverProducts)
    console.log("FINAL PRODUCTS =", finalProducts)

    if (!shouldSuppressBotMessage && (messageText || quickReplies.length || finalProducts.length)) {
      pushBotMessage({
        ...(data || {}),
        message: messageText,
        quickReplies,
        products: finalProducts
      })
      updateLatestPendingUserMessage("read")
    }
  } catch (error) {
    updateLatestPendingUserMessage("failed")
    setBackendIssue("Không gửi được tin nhắn. Hệ thống sẽ thử lại khi kết nối ổn định.", error)
    pushBotMessage({
      mode: supportMode.value || "BOT",
      message:
        supportMode.value === "HUMAN"
          ? "Shop đã nhận được tin nhắn của anh/chị. Nhân viên sẽ phản hồi sớm nhất."
          : "Em đang bận một chút nhưng vẫn hỗ trợ được ạ. Anh/chị thử nhắn cụ thể hơn như “bomber đen size L dưới 700k” hoặc “hoodie màu trắng” nhé.",
      quickReplies: normalizeQuickReplies(
        supportMode.value === "HUMAN"
          ? ["Áo bomber", "Hoodie", "Mẫu màu xám còn hàng", "Tư vấn size"]
          : ["Bomber đen dưới 700k", "Bomber size L", "Coach màu đen", "Mẫu màu xám còn hàng"]
      ),
      products: []
    })
  } finally {
    isTyping.value = false
    await syncScroll()
  }
}

function buildProductSearchKeyword(text = "") {
  const normalized = String(text)
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\./g, " ")
    .replace(/\s+/g, " ")
    .trim()

  const categoryKeywords = ["bomber", "hoodie", "coach", "jacket", "khoac"]
  const colorKeywords = [
    { key: "den", display: "Đen" },
    { key: "trang", display: "Trắng" },
    { key: "xam", display: "Xám" },
    { key: "xanh", display: "Xanh" },
    { key: "nau", display: "Nâu" },
    { key: "be", display: "Be" },
    { key: "do", display: "Đỏ" },
    { key: "navy", display: "Navy" }
  ]

  const matchedCategory = categoryKeywords.find((kw) => normalized.includes(kw)) || ""
  const matchedColor = colorKeywords.find((c) => normalized.includes(c.key))
  const matchedSize = extractSizeKeyword(normalized)

  return {
    keyword: matchedCategory,
    color: matchedColor?.display || "",
    size: matchedSize
  }
}

function onKeydown(event) {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault()
    submitMessage()
  }
}

function handleOnline() {
  botOnline.value = true
}

function handleOffline() {
  botOnline.value = false
}

function handleAuthContextChanged() {
  stopHistoryPolling()

  // Recompute identity key — may have changed after logout/login
  const newIdentity = getIdentityKey()
  const newSessionKey = `dirtywave_chat_session_id:${newIdentity}`
  const existing = localStorage.getItem(newSessionKey)

  const newSessionId = existing || buildSessionCode(newIdentity)
  if (!existing) localStorage.setItem(newSessionKey, newSessionId)

  sessionId.value = newSessionId
  messages.value = []
  sessionMemory.value = loadSessionMemory()
  input.value = ""
  isTyping.value = false
  isOpen.value = false
  sessionStatus.value = "OPEN"
  supportMode.value = "BOT"
  supportLabel.value = "Bot đang hỗ trợ"
  cachedCustomerProfile = null
}

watch(
  () => route.fullPath,
  () => {
    if (!hiddenRoutes.value && !messages.value.length) {
      pushWelcome()
    }
  }
)

watch(isOpen, async (value) => {
  if (value) await syncScroll()
})

onMounted(async () => {
  clampChatboxSize()
  await ensureChatCatalogLoaded()
  await fetchSupportStatus()

  window.addEventListener("online", handleOnline)
  window.addEventListener("offline", handleOffline)
  window.addEventListener("auth-context-changed", handleAuthContextChanged)
  window.addEventListener("resize", clampChatboxSize)

  if (isOpen.value) {
    await syncHistoryFromServer()
    if (!messages.value.length) pushWelcome()
    startHistoryPolling()
  }
})

onBeforeUnmount(() => {
  stopResizing()
  window.removeEventListener("online", handleOnline)
  window.removeEventListener("offline", handleOffline)
  window.removeEventListener("auth-context-changed", handleAuthContextChanged)
  window.removeEventListener("resize", clampChatboxSize)
  stopHistoryPolling()
})
</script>

<template>
  <div v-if="!hiddenRoutes" class="dw-chat-root">
    <Transition name="dw-widget">
      <section v-if="isOpen" class="dw-chatbox" :style="chatboxStyle">
        <header class="dw-chatbox__header">
          <div class="dw-chatbox__identity">
            <div class="dw-avatar">
              <Bot :size="20" />
            </div>
            <div>
              <strong>{{ BOT_NAME }}</strong>
              <div class="dw-status-line">
                <span :class="['dw-status-dot', { offline: !botOnline }]" />
                {{ botOnline ? "Online" : "Offline" }}
              </div>
              <div class="dw-support-mode" :class="supportMode === 'HUMAN' ? 'is-human' : 'is-bot'">
                {{ supportLabel }}
              </div>
            </div>
          </div>
          <button type="button" class="dw-close-btn" @click="closeWidget">
            <X :size="18" />
          </button>
        </header>

        <div ref="bodyRef" class="dw-chatbox__body">
          <div v-if="chatBackendIssue" class="dw-backend-alert">
            <span>{{ chatBackendIssue }}</span>
            <div class="dw-backend-alert__actions">
              <button type="button" @click="retryBackendConnection">Thử lại</button>
            </div>
          </div>

          <div v-if="supportMode === 'BOT' && sessionStatus === 'OPEN'" class="dw-support-cta">
            <button type="button" class="dw-support-cta__btn" @click="requestHumanSupportFromBot">
              Gặp nhân viên hỗ trợ
            </button>
          </div>

          <div v-if="supportMode === 'BOT' && sessionStatus === 'OPEN' && visibleMessages.length <= 1" class="dw-faq-box">
            <div class="dw-faq-title">💡 Câu hỏi gợi ý:</div>
            <div class="dw-faq-list">
              <button
                v-for="question in faqSuggestions"
                :key="question"
                type="button"
                class="dw-faq-chip"
                @click="handleFaqSuggestion(question)"
              >
                {{ question }}
              </button>
            </div>
          </div>

          <article
            v-for="message in visibleMessages"
            :key="message.id"
            :class="['dw-message', `is-${message.role}`]"
          >
            <div class="dw-bubble">
              <p>{{ message.text }}</p>

              <div v-if="message.products?.length" class="dw-product-list-wrap">
                <div class="dw-product-list-hint">Vuốt hoặc kéo sang phải để xem thêm</div>
                <div class="dw-product-list">
                  <article v-for="product in message.products" :key="product.id" class="dw-product-card">
                    <img :src="product.image" :alt="product.name" class="dw-product-card__image" />
                    <div class="dw-product-card__content">
                      <div class="dw-product-card__top">
                        <div>
                          <strong>{{ product.name }}</strong>
                          <small>{{ product.category }} • Tồn {{ product.stock }}</small>
                          <span v-if="product.promotionText" class="dw-product-promo">
                            {{ product.promotionText }}
                          </span>
                        </div>
                        <span class="dw-product-price">{{ currency(product.price) }}</span>
                      </div>
                      <p class="dw-product-summary">{{ product.summary }}</p>
                      <div class="dw-product-meta">
                        <span v-if="product.colors?.length">Màu: {{ product.colors.join(", ") }}</span>
                        <span v-if="product.sizes?.length">Size: {{ product.sizes.join(", ") }}</span>
                      </div>
                      <div class="dw-product-actions">
                        <div class="dw-qty">
                          <button type="button" @click="changeQty(product.id, -1)"><Minus :size="14" /></button>
                          <span>{{ getQty(product.id) }}</span>
                          <button type="button" @click="changeQty(product.id, 1)"><Plus :size="14" /></button>
                        </div>
                        <button type="button" class="ghost" @click="goToProduct(product.id)">Xem ngay</button>
                        <button type="button" class="primary" @click="addToCart(product)">
                          <ShoppingBag :size="14" />
                          Thêm giỏ
                        </button>
                      </div>
                    </div>
                  </article>
                </div>
              </div>

              <div v-if="message.quickReplies?.length" class="dw-quick-replies">
                <button
                  v-for="reply in message.quickReplies"
                  :key="reply"
                  type="button"
                  @click="reply === 'Xem giỏ hàng' ? openCart() : handleQuickReply(reply)"
                >
                  <Sparkles :size="12" />
                  {{ reply }}
                </button>
              </div>
            </div>

            <div class="dw-meta">
              <span>{{ timeLabel(message.createdAt) }}</span>
              <span>• {{ message.statusLabel }}</span>
              <span v-if="message.role === 'user'" class="dw-meta__icon">
                <Check :size="12" />
                {{ message.readState }}
              </span>
              <span v-else class="dw-meta__icon">
                <CheckCheck :size="12" />
                {{ message.readState }}
              </span>
            </div>
          </article>

          <div v-if="isTyping" class="dw-message is-bot">
            <div class="dw-bubble dw-bubble--typing">
              <CircleDashed :size="14" class="spin" />
              <span>Đang trả lời</span>
            </div>
            <div class="dw-meta">
              <span>{{ timeLabel(new Date()) }}</span>
              <span>• Đã nhận tin nhắn</span>
            </div>
          </div>
        </div>

        <footer v-if="sessionStatus !== 'CLOSED'" class="dw-chatbox__footer">
          <textarea
            v-model="input"
            rows="1"
            placeholder="Nhắn nhu cầu của anh/chị..."
            @keydown="onKeydown"
          />
          <button
            type="button"
            class="dw-send-btn"
            :disabled="isTyping || !String(input || '').trim()"
            @click="submitMessage"
          >
            <SendHorizonal :size="16" />
          </button>
        </footer>

        <footer v-else class="dw-chatbox__footer dw-chatbox__footer--closed">
          <div class="dw-closed-box">
            <strong>Phiên hỗ trợ đã kết thúc</strong>
            <p>Anh/chị có thể bắt đầu cuộc trò chuyện mới nếu cần thêm hỗ trợ.</p>
            <button type="button" class="dw-restart-btn" @click="startNewConversation">
              Bắt đầu cuộc trò chuyện mới
            </button>
          </div>
        </footer>

        <button type="button" class="dw-resize-handle dw-resize-handle--n" data-dir="n" aria-label="Resize top" @pointerdown="startResizing" />
        <button type="button" class="dw-resize-handle dw-resize-handle--s" data-dir="s" aria-label="Resize bottom" @pointerdown="startResizing" />
        <button type="button" class="dw-resize-handle dw-resize-handle--e" data-dir="e" aria-label="Resize right" @pointerdown="startResizing" />
        <button type="button" class="dw-resize-handle dw-resize-handle--w" data-dir="w" aria-label="Resize left" @pointerdown="startResizing" />
        <button type="button" class="dw-resize-handle dw-resize-handle--ne" data-dir="ne" aria-label="Resize top-right" @pointerdown="startResizing" />
        <button type="button" class="dw-resize-handle dw-resize-handle--nw" data-dir="nw" aria-label="Resize top-left" @pointerdown="startResizing" />
        <button type="button" class="dw-resize-handle dw-resize-handle--se" data-dir="se" aria-label="Resize bottom-right" @pointerdown="startResizing" />
        <button type="button" class="dw-resize-handle dw-resize-handle--sw" data-dir="sw" aria-label="Resize bottom-left" @pointerdown="startResizing" />
      </section>
    </Transition>

    <div
      class="dw-launcher-wrap"
      :class="{ 'is-open': isOpen }"
    >
      <a
        v-for="(item, index) in CONTACTS"
        :key="item.label"
        :href="item.href"
        target="_blank"
        rel="noreferrer"
        :class="['dw-contact-pill', item.className]"
        :style="{ '--dw-index': index }"
      >
        <component :is="item.icon" :size="16" />
        <span>{{ item.label }}</span>
      </a>

      <button
        type="button"
        class="dw-launcher-btn"
        :class="{ active: isOpen }"
        title="Chat với DirtyWave"
        @click="isOpen ? closeWidget() : openWidget()"
      >
        <MessageCircleMore :size="24" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.dw-chat-root {
  position: fixed;
  right: clamp(36px, 6vw, 96px);
  bottom: 28px;
  z-index: 120;
}

.dw-chatbox {
  position: relative;
  width: min(410px, calc(100vw - 24px));
  height: min(680px, calc(100vh - 110px));
  margin: 0 0 16px auto;
  border-radius: 28px;
  overflow: hidden;
  display: grid;
  grid-template-rows: auto 1fr auto;
  background:
    radial-gradient(circle at top right, rgba(197,22,45,0.1), transparent 32%),
    linear-gradient(180deg, #ffffff 0%, #fff8f8 100%);
  border: 1px solid rgba(197, 22, 45, 0.16);
  box-shadow: 0 32px 72px rgba(88, 14, 28, 0.22);
  backdrop-filter: blur(12px);
}

.dw-resize-handle {
  position: absolute;
  width: 14px;
  height: 14px;
  border: none;
  border-radius: 999px;
  background: rgba(143, 17, 33, 0.24);
  opacity: 0;
  z-index: 4;
  touch-action: none;
  transition: opacity 0.2s ease;
}

.dw-chatbox:hover .dw-resize-handle { opacity: 0.62; }

.dw-resize-handle--n,
.dw-resize-handle--s {
  left: 50%;
  transform: translateX(-50%);
  width: 70px;
  height: 10px;
}

.dw-resize-handle--e,
.dw-resize-handle--w {
  top: 50%;
  transform: translateY(-50%);
  width: 10px;
  height: 70px;
}

.dw-resize-handle--n { top: 2px; cursor: ns-resize; }
.dw-resize-handle--s { bottom: 2px; cursor: ns-resize; }
.dw-resize-handle--e { right: 2px; cursor: ew-resize; }
.dw-resize-handle--w { left: 2px; cursor: ew-resize; }
.dw-resize-handle--ne { top: 4px; right: 4px; cursor: nesw-resize; }
.dw-resize-handle--nw { top: 4px; left: 4px; cursor: nwse-resize; }
.dw-resize-handle--se { right: 4px; bottom: 4px; cursor: nwse-resize; }
.dw-resize-handle--sw { left: 4px; bottom: 4px; cursor: nesw-resize; }

.dw-chatbox__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  color: #fff;
  background: linear-gradient(135deg, #6e0d1b 0%, #b91028 55%, #dd5267 100%);
}

.dw-chatbox__identity {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dw-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(255,255,255,0.18);
  border: 1px solid rgba(255,255,255,0.22);
  flex-shrink: 0;
}

.dw-status-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
  font-size: 12px;
  opacity: 0.95;
}

.dw-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 0 4px rgba(74,222,128,0.18);
  animation: dw-pulse-green 2s ease-in-out infinite;
}

@keyframes dw-pulse-green {
  0%, 100% { box-shadow: 0 0 0 4px rgba(74,222,128,0.18); }
  50% { box-shadow: 0 0 0 6px rgba(74,222,128,0.3); }
}

.dw-status-dot.offline {
  background: #facc15;
  box-shadow: 0 0 0 4px rgba(250,204,21,0.18);
  animation: none;
}

.dw-close-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 50%;
  color: #fff;
  background: rgba(255,255,255,0.12);
  cursor: pointer;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  transition: background 0.2s ease, transform 0.2s ease;
}

.dw-close-btn:hover {
  background: rgba(255,255,255,0.24);
  transform: scale(1.08);
}

.dw-chatbox__body {
  padding: 16px;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dw-backend-alert {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid #fecaca;
  background: #fff1f2;
  color: #9f1239;
  border-radius: 12px;
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.35;
}

.dw-backend-alert__actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.dw-backend-alert button {
  border: 1px solid #fda4af;
  background: #fff;
  color: #be123c;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.dw-message {
  display: flex;
  flex-direction: column;
  gap: 6px;
  animation: dw-msg-in 0.3s ease both;
  .dw-backend-alert {
    flex-direction: column;
    align-items: stretch;
  }
  .dw-backend-alert__actions {
    width: 100%;
    flex-direction: column;
  }
  .dw-backend-alert button {
    width: 100%;
  }
}

@keyframes dw-msg-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.dw-message.is-user { align-items: flex-end; }
.dw-message.is-bot { align-items: flex-start; }

.dw-bubble {
  max-width: 92%;
  border-radius: 22px;
  padding: 12px 14px;
  border: 1px solid #f1d8dd;
  background: #ffffff;
  box-shadow: 0 10px 26px rgba(103,23,33,0.08);
}

.dw-message.is-user .dw-bubble {
  background: linear-gradient(135deg, #7b0f1e, #c5162d);
  color: #fff;
  border-color: transparent;
}

.dw-bubble p { margin: 0; line-height: 1.48; white-space: pre-wrap; }

.dw-bubble--typing { display: inline-flex; align-items: center; gap: 8px; }

.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.dw-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 4px;
  font-size: 12px;
  color: #7b7073;
}

.dw-meta__icon { display: inline-flex; align-items: center; gap: 4px; }

.dw-product-list {
  margin-top: 12px;
  display: flex;
  gap: 14px;
  overflow-x: auto;
  overflow-y: hidden;
  scroll-snap-type: x mandatory;
  padding: 2px 2px 12px;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: auto;
  scrollbar-color: rgba(143,17,33,0.65) rgba(143,17,33,0.14);
}

.dw-product-list::-webkit-scrollbar { height: 14px; }
.dw-product-list::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, rgba(123,15,30,0.9), rgba(197,22,45,0.85));
  border-radius: 999px;
  border: 2px solid rgba(255,255,255,0.85);
}
.dw-product-list::-webkit-scrollbar-track { background: rgba(143,17,33,0.12); border-radius: 999px; }

.dw-product-card {
  flex: 0 0 328px;
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 10px;
  padding: 12px;
  border-radius: 18px;
  background: #fff6f7;
  border: 1px solid #efd6db;
  scroll-snap-align: start;
  box-shadow: 0 8px 20px rgba(103,23,33,0.08);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.dw-product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(103,23,33,0.14);
  border-color: rgba(197,22,45,0.3);
}

.dw-product-card__image { width: 88px; height: 88px; object-fit: cover; border-radius: 14px; flex-shrink: 0; }
.dw-product-card__content { display: grid; gap: 8px; min-width: 0; }
.dw-product-card__top { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.dw-product-card__top strong { display: block; color: #241b1d; font-size: 14px; line-height: 1.35; }
.dw-product-card__top small { display: block; margin-top: 2px; color: #72686c; font-size: 12px; }
.dw-product-price { color: #991126; font-weight: 700; white-space: nowrap; font-size: 13px; }
.dw-product-summary { margin: 0; font-size: 13px; color: #5e5658; line-height: 1.4; }

.dw-product-promo {
  display: inline-flex;
  width: fit-content;
  margin-top: 4px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #fff0f2;
  border: 1px solid #f3b8c2;
  color: #b11226;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.3;
}

.dw-product-meta { display: flex; flex-wrap: wrap; gap: 6px; font-size: 12px; color: #7b6f73; }
.dw-product-meta span { padding: 4px 8px; border-radius: 999px; background: #fff; border: 1px solid #eed8dd; }

.dw-product-actions { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }

.dw-qty {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid #ead2d7;
  padding: 4px 8px;
}

.dw-qty button {
  width: 24px; height: 24px; border: none; background: #f7e3e7; color: #8f1121;
  border-radius: 50%; display: grid; place-items: center; cursor: pointer;
  transition: background 0.15s ease, transform 0.15s ease;
}
.dw-qty button:hover { background: #f0cdd3; transform: scale(1.12); }
.dw-qty button:active { transform: scale(0.95); }

.dw-product-actions .ghost,
.dw-product-actions .primary {
  border: none; border-radius: 999px; padding: 8px 12px; cursor: pointer; font-weight: 600; font-size: 13px;
}

.dw-product-actions .ghost {
  background: #fff; color: #8f1121; border: 1px solid #e7c3cb;
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.15s ease;
}
.dw-product-actions .ghost:hover { background: #fff0f2; border-color: #d4a0aa; }
.dw-product-actions .ghost:active { transform: scale(0.96); }

.dw-product-actions .primary {
  display: inline-flex; align-items: center; gap: 6px; color: #fff;
  background: linear-gradient(135deg, #7b0f1e, #c5162d);
  transition: filter 0.2s ease, transform 0.15s ease, box-shadow 0.2s ease;
}
.dw-product-actions .primary:hover { filter: brightness(1.12); box-shadow: 0 4px 12px rgba(143,17,33,0.3); }
.dw-product-actions .primary:active { transform: scale(0.96); }

.dw-quick-replies { margin-top: 12px; display: flex; flex-wrap: wrap; gap: 8px; }
.dw-quick-replies button {
  display: inline-flex; align-items: center; gap: 6px;
  border: 1px solid #e8c5cb; background: #fff; color: #8f1121;
  padding: 8px 12px; border-radius: 999px; cursor: pointer; font-size: 12px;
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.15s ease, box-shadow 0.2s ease;
}
.dw-quick-replies button:hover { background: #fff0f2; border-color: #d4a0aa; box-shadow: 0 2px 8px rgba(143,17,33,0.1); }
.dw-quick-replies button:active { transform: scale(0.95); background: #ffe8ec; }

.dw-chatbox__footer {
  display: grid; grid-template-columns: 1fr auto; gap: 10px;
  padding: 14px 16px 16px; border-top: 1px solid #f1d8dd; background: #fff;
}

.dw-chatbox__footer textarea {
  resize: none; min-height: 48px; max-height: 120px;
  padding: 12px 14px; border-radius: 18px; border: 1px solid #ebd3d7;
  outline: none; font: inherit; font-size: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.dw-chatbox__footer textarea:focus {
  border-color: #c5162d;
  box-shadow: 0 0 0 3px rgba(197,22,45,0.1);
}

.dw-send-btn {
  width: 48px; height: 48px; border-radius: 16px; border: none;
  background: #8f1121; color: #fff; display: grid; place-items: center;
  cursor: pointer; align-self: end;
  box-shadow: 0 12px 24px rgba(143,17,33,0.28);
  transition: background 0.2s ease, transform 0.15s ease, box-shadow 0.2s ease;
}
.dw-send-btn:not(:disabled):hover { background: #a81530; transform: scale(1.06); box-shadow: 0 14px 28px rgba(143,17,33,0.36); }
.dw-send-btn:not(:disabled):active { transform: scale(0.96); }
.dw-send-btn:disabled { opacity: 0.55; cursor: not-allowed; box-shadow: none; }

.dw-launcher-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding-top: 280px;
  cursor: pointer;
}

.dw-launcher-wrap.is-open {
  padding-top: 0;
  pointer-events: none;
}

.dw-launcher-wrap.is-open .dw-launcher-btn {
  pointer-events: auto;
}

.dw-launcher-btn {
  width: 64px;
  height: 64px;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: #fff;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #7b0f1e, #c5162d 60%, #de4c61);
  box-shadow: 0 18px 36px rgba(143,17,33,0.35);
  transition: transform 0.35s ease, box-shadow 0.25s ease;
}

.dw-launcher-btn:hover {
  box-shadow: 0 22px 44px rgba(143,17,33,0.45);
}

.dw-launcher-btn.active {
  transform: rotate(180deg);
}

.dw-contact-pill {
  position: absolute;
  right: 6px;
  bottom: 6px;
  transform: translateY(0) scale(0.84);
  opacity: 0;
  pointer-events: none;
  transition: transform 0.32s ease, opacity 0.32s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #fff;
  border-radius: 999px;
  padding: 12px 14px;
  min-width: 124px;
  box-shadow: 0 14px 28px rgba(0,0,0,0.16);
}

.dw-launcher-wrap:not(.is-open):hover .dw-contact-pill {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(calc(-84px * (var(--dw-index) + 1))) scale(1);
}

.dw-contact-pill:hover {
  filter: brightness(1.1);
  box-shadow: 0 16px 32px rgba(0,0,0,0.22);
}

.dw-contact-pill span {
  font-size: 13px;
  font-weight: 600;
}

.is-zalo {
  background: linear-gradient(135deg, #0068ff, #4cb4ff);
}
.is-instagram {
  background: linear-gradient(135deg, #833ab4, #e1306c, #f77737);
}
.is-facebook {
  background: linear-gradient(135deg, #1877f2, #53a4ff);
}

.dw-widget-enter-active,
.dw-widget-leave-active {
  transition: all 0.24s ease;
}

.dw-widget-enter-from,
.dw-widget-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.97);
}

@media (max-width: 640px) {
  .dw-chat-root {
    right: 12px;
    bottom: 12px;
    left: 12px;
  }

  .dw-chatbox {
    width: 100%;
  }

  .dw-resize-handle { display: none; }

  .dw-contact-pill span {
    display: none;
  }

  .dw-product-card {
    grid-template-columns: 1fr;
  }

  .dw-product-card__image {
    width: 100%;
    height: 160px;
  }
}

.dw-product-list-wrap {
  margin-top: 12px;
}

.dw-product-list-hint {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #8f1121;
  opacity: 0.9;
}

.dw-support-mode {
  margin-top: 6px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  width: fit-content;
}

.dw-support-mode.is-human {
  background: rgba(255, 255, 255, 0.18);
  color: #fff7d6;
  border: 1px solid rgba(255, 244, 179, 0.25);
}

.dw-support-mode.is-bot {
  background: rgba(255, 255, 255, 0.18);
  color: #e8f6ff;
  border: 1px solid rgba(190, 232, 255, 0.25);
}

.dw-chatbox__footer--closed {
  display: block;
}

.dw-closed-box {
  border: 1px solid #ebd3d7;
  background: #fff7f8;
  border-radius: 16px;
  padding: 14px;
}

.dw-closed-box p {
  margin: 8px 0 12px;
  color: #6b5f63;
}

.dw-restart-btn {
  border: none;
  border-radius: 12px;
  padding: 10px 14px;
  background: #8f1121;
  color: #fff;
  cursor: pointer;
  font-weight: 700;
  transition: background 0.2s ease, transform 0.15s ease;
}

.dw-restart-btn:hover {
  background: #a81530;
}

.dw-restart-btn:active {
  transform: scale(0.97);
}

.dw-faq-box {
  margin-bottom: 12px;
}

.dw-faq-title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 700;
  color: #7f6b70;
}

.dw-faq-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dw-faq-chip {
  border: 1px solid #e7c3cb;
  background: #fff;
  color: #8f1121;
  border-radius: 999px;
  padding: 8px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.15s ease, box-shadow 0.2s ease;
}

.dw-faq-chip:hover {
  background: #fff0f2;
  border-color: #d4a0aa;
  box-shadow: 0 2px 8px rgba(143, 17, 33, 0.1);
}

.dw-faq-chip:active {
  transform: scale(0.95);
}

.dw-support-cta {
  margin-bottom: 10px;
}

.dw-support-cta__btn {
  border: none;
  border-radius: 999px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #7b0f1e, #c5162d);
  color: #fff;
  cursor: pointer;
  font-weight: 700;
  box-shadow: 0 10px 24px rgba(143, 17, 33, 0.22);
  transition: filter 0.2s ease, transform 0.15s ease, box-shadow 0.2s ease;
}

.dw-support-cta__btn:hover {
  filter: brightness(1.12);
  box-shadow: 0 12px 28px rgba(143, 17, 33, 0.32);
}

.dw-support-cta__btn:active {
  transform: scale(0.97);
}
</style>











