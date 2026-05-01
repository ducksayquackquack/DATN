<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { Bell, Menu, Search, ShoppingCart } from "lucide-vue-next"
import taiKhoanService from "../services/taiKhoanService"
import { getAllSanPham } from "../services/sanPhamService"
import { getKhachHangByTaiKhoanId } from "../services/KhachHangService"
import { getVietnameseNameByEmail } from "../utils/vietnameseNames"
import { useNotifications } from "../composables/useNotifications"
import { useToast } from "../composables/useToast"
import {
  AUTH_CONTEXT_CHANGED_EVENT,
  resolveAccountByRole
} from "../utils/authContext"
import { resolveApiOrigin } from "../utils/apiOrigin"
import { getProductImageOverride } from "../utils/productImageOverrides"
import { readCartObject } from "../utils/cartStorage"
import { fallbackImageFor } from "../utils/productImageFallback"
import logo from "../assets/img/logo/new logo.png?url"

const props = defineProps({
  cartCount: { type: Number, default: null }
})
const CART_UPDATED_EVENT = "dirtywave:cart-updated"

const route = useRoute()
const router = useRouter()
const toast = useToast()

const profileOpen = ref(false)
const mobileOpen = ref(false)
const searchQuery = ref("")
const searchOpen = ref(false)
const searchLoading = ref(false)
const searchProducts = ref([])
const userAvatar = ref("")
const userDisplayName = ref("Tài khoản")
const userRoleLabel = ref("Khách hàng")
const internalCartCount = ref(0)
const { notifications, unreadCount: notificationCount } = useNotifications("customer")

const toNumber = (value) => {
  const n = Number(value)
  return Number.isFinite(n) ? n : 0
}

const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")

const normalizeText = (value = "") => String(value).trim().toLowerCase()

const normalizeKeyword = (value = "") => String(value)
  .normalize("NFD")
  .replace(/[\u0300-\u036f]/g, "")
  .toLowerCase()
  .trim()

const isAbsoluteUrl = (value = "") => /^https?:\/\//i.test(value) || /^data:image\//i.test(value)

const IMAGE_EXT_RE = /\.(png|jpe?g|webp|gif|bmp|svg)$/i

const looksLikeImagePath = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return false
  if (IMAGE_EXT_RE.test(raw)) return true
  return /uploads?|images?|anh|hinh/i.test(raw)
}

const toImageUrl = (value) => {
  if (!value) return ""
  const raw = String(value).trim()
  if (!raw) return ""

  if (isAbsoluteUrl(raw)) return raw

  const normalizedSlash = raw.replace(/\\/g, "/")

  const uploadsMatch = normalizedSlash.match(/(?:^|\/)(uploads\/.*)$/i)
  if (uploadsMatch?.[1]) {
    return `${BACKEND_ORIGIN}/${uploadsMatch[1]}`
  }

  if (normalizedSlash.startsWith("/uploads/")) {
    return `${BACKEND_ORIGIN}${normalizedSlash}`
  }

  if (normalizedSlash.startsWith("uploads/")) {
    return `${BACKEND_ORIGIN}/${normalizedSlash}`
  }

  if (normalizedSlash.startsWith("assets/") || normalizedSlash.startsWith("img/")) {
    return `/${normalizedSlash}`
  }

  if (normalizedSlash.startsWith("/")) {
    return normalizedSlash
  }

  if (looksLikeImagePath(normalizedSlash) && !normalizedSlash.includes("/")) {
    return `${BACKEND_ORIGIN}/uploads/${normalizedSlash}`
  }

  return looksLikeImagePath(normalizedSlash) ? normalizedSlash : ""
}

const pickImageValue = (entry) => {
  if (!entry) return ""
  if (typeof entry === "string") {
    return looksLikeImagePath(entry) ? toImageUrl(entry) : ""
  }
  if (Array.isArray(entry)) {
    for (const child of entry) {
      const found = pickImageValue(child)
      if (found) return found
    }
    return ""
  }
  if (typeof entry === "object") {
    const keys = [
      "anh", "hinhAnh", "image", "imageUrl", "url", "duongDan", "duongDanAnh", "link", "path",
      "tenFile", "fileName", "src", "thumbnail", "avatar", "anhSanPham", "anhPhu", "duLieuAnh"
    ]
    for (const key of keys) {
      const found = pickImageValue(entry[key])
      if (found) return found
    }

    for (const [key, value] of Object.entries(entry)) {
      if (/anh|hinh|image|img|url|path|file/i.test(key)) {
        const found = pickImageValue(value)
        if (found) return found
      }
    }
  }
  return ""
}

const normalizeSearchProduct = (item) => {
  const variants = Array.isArray(item?.sanPhamChiTiets) ? item.sanPhamChiTiets : []
  const variantPrices = variants.map((v) => toNumber(v?.giaBan)).filter((v) => v > 0)
  const id = Number(item?.id)
  const code = String(item?.maSanPham || "")
  const extractedImage = pickImageValue([item?.anh, item?.hinhAnh, item?.images, item?.image, item?.listAnh, item?.anhChinh, variants])
  const overrideImage = getProductImageOverride({ id, maSanPham: code })[0]

  return {
    id,
    name: String(item?.tenSanPham || item?.name || "Sản phẩm"),
    code,
    category: String(item?.danhMuc?.tenDanhMuc || item?.loai?.tenLoai || "Thời trang nam"),
    price: variantPrices.length ? Math.min(...variantPrices) : toNumber(item?.giaBan || item?.gia || 0),
    image: extractedImage || overrideImage || fallbackImageFor(id, code),
  }
}

const formatVND = (value) => new Intl.NumberFormat("vi-VN").format(toNumber(value)) + "₫"

const searchMatches = computed(() => {
  const keyword = normalizeKeyword(searchQuery.value)
  if (!keyword) return []

  const list = searchProducts.value
    .map((item) => {
      const name = normalizeKeyword(item.name)
      const code = normalizeKeyword(item.code)
      const category = normalizeKeyword(item.category)
      const isMatch = name.includes(keyword) || code.includes(keyword) || category.includes(keyword)
      if (!isMatch) return null

      let score = 0
      if (name.startsWith(keyword)) score += 3
      if (code.startsWith(keyword)) score += 2
      if (category.startsWith(keyword)) score += 1
      return { ...item, score }
    })
    .filter(Boolean)
    .sort((a, b) => b.score - a.score || a.name.localeCompare(b.name, "vi"))

  return list.slice(0, 7)
})

const lastToastSignature = ref("")
const lastToastIdentity = ref("")

const getNotificationToastStorageKey = (identity) => `customer:notification-toast-signature:${identity || "guest"}`

const readLastToastSignature = (identity) => {
  try {
    return String(sessionStorage.getItem(getNotificationToastStorageKey(identity)) || "")
  } catch {
    return ""
  }
}

const writeLastToastSignature = (identity, signature) => {
  try {
    sessionStorage.setItem(getNotificationToastStorageKey(identity), String(signature || ""))
  } catch {
    // Ignore storage access errors.
  }
}

const getGlobalNotificationToastState = () => {
  if (typeof window === "undefined") return null
  const key = "__dirtywaveNotificationToastState"
  if (!window[key]) {
    window[key] = { signature: "", at: 0 }
  }
  return window[key]
}

const getCurrentCustomerIdentity = () => {
  const userId = String(localStorage.getItem("userId") || "guest").trim() || "guest"
  const userEmail = String(localStorage.getItem("userEmail") || "").trim().toLowerCase() || "guest"
  return `${userId}:${userEmail}`
}

const unreadSignature = computed(() => {
  return notifications.value
    .filter((item) => !item.read)
    .map((item) => String(item.id || "").trim())
    .filter(Boolean)
    .sort()
    .join("|")
})

const getAvatarStorageKey = (accountId) => `avatar:${accountId}`

const normalizeRole = (role) => String(role || "").trim().toUpperCase().replace(/^ROLE_/, "")

const toRoleLabel = (role) => {
  const normalized = normalizeRole(role)
  if (normalized === "ADMIN") return "Quản trị viên"
  if (normalized === "EMPLOYEE") return "Nhân viên"
  return "Khách hàng"
}

const isCustomerRole = (role) => {
  const normalized = normalizeRole(role)
  return normalized === "CUSTOMER" || normalized === "KHACH_HANG" || normalized === "KHACHHANG" || normalized === "USER"
}

const toDisplayNameFromEmail = (email = "") => {
  const localPart = String(email || "").split("@")[0].replace(/[._-]+/g, " ").trim()
  if (!localPart) return "Tài khoản"
  return localPart.split(/\s+/).map((w) => w.charAt(0).toUpperCase() + w.slice(1)).join(" ")
}

const userInitials = computed(() => {
  const words = String(userDisplayName.value || "TK").trim().split(/\s+/).filter(Boolean)
  if (words.length >= 2) return `${words[0][0]}${words[words.length - 1][0]}`.toUpperCase()
  return String(userDisplayName.value || "TK").slice(0, 2).toUpperCase()
})

const resolvedCartCount = computed(() => {
  if (props.cartCount !== null && props.cartCount !== undefined) return props.cartCount
  return internalCartCount.value
})

const isLoggedInAsCustomer = ref(false)

const refreshCartCount = () => {
  const stored = readCartObject()
  internalCartCount.value = Object.values(stored).reduce((s, v) => s + Number(v || 0), 0)
}

const loadCurrentUser = async () => {
  const storedRole = normalizeRole(localStorage.getItem("role"))
  const userEmail = String(localStorage.getItem("userEmail") || "").trim().toLowerCase()
  userAvatar.value = ""
  userDisplayName.value = "Tài khoản"
  userRoleLabel.value = "Khách hàng"
  isLoggedInAsCustomer.value = false

  // Customer navbar should never render admin/employee identity.
  if (storedRole && !isCustomerRole(storedRole)) {
    return
  }

  if (userEmail) userDisplayName.value = toDisplayNameFromEmail(userEmail)

  try {
    const account = await resolveAccountByRole({
      service: taiKhoanService,
      expectedRoles: ['CUSTOMER', 'KHACH_HANG', 'KHACHHANG', 'USER'],
      allowFallback: false
    })
    if (!account) return

    isLoggedInAsCustomer.value = true
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

const navigateTo = (path) => {
  mobileOpen.value = false
  if (route.path === path) return
  router.push(path)
}

const handleSearch = () => {
  if (!searchQuery.value.trim()) {
    searchOpen.value = false
    router.push("/san-pham")
    return
  }
  searchOpen.value = false
  router.push({ path: "/san-pham", query: { q: searchQuery.value.trim() } })
}

const openSearchResultsPage = () => {
  if (!searchQuery.value.trim()) return
  searchOpen.value = false
  router.push({ path: "/san-pham", query: { q: searchQuery.value.trim() } })
}

const openProductFromSearch = (item) => {
  if (!item?.id) {
    openSearchResultsPage()
    return
  }
  searchOpen.value = false
  router.push(`/product/${item.id}`)
}

const loadSearchProducts = async () => {
  searchLoading.value = true
  try {
    const response = await getAllSanPham()
    const source = Array.isArray(response?.data) ? response.data : []
    searchProducts.value = source.map(normalizeSearchProduct)
  } catch {
    searchProducts.value = []
  } finally {
    searchLoading.value = false
  }
}

const handleCartClick = () => {
  if (route.path === "/gio-hang") return
  router.push("/gio-hang")
}

const openNotificationsPage = () => {
  router.push("/customer/notifications")
}

const goHome = () => router.push("/trang-chu")

const toggleMobileMenu = () => {
  mobileOpen.value = !mobileOpen.value
}

const toggleProfileMenu = (event) => {
  event.stopPropagation()
  profileOpen.value = !profileOpen.value
}

const openProfilePage = () => {
  profileOpen.value = false
  router.push("/customer/profile")
}

const openOrdersPage = () => {
  profileOpen.value = false
  router.push({ path: "/customer/profile", query: { tab: "orders" } })
}

const openOrderLookupPage = () => {
  profileOpen.value = false
  router.push({ path: "/customer/profile", query: { tab: "lookup" } })
}

const logout = () => {
  profileOpen.value = false
  localStorage.removeItem("role")
  localStorage.removeItem("userId")
  localStorage.removeItem("userEmail")
  window.dispatchEvent(new Event("auth-context-changed"))
  router.push("/auth/customer-login")
}

const handleDocumentClick = (event) => {
  const target = event.target
  if (!(target instanceof Element)) return
  if (!target.closest(".sn-profile-wrapper")) {
    profileOpen.value = false
  }
  if (!target.closest(".sn-search-shell")) {
    searchOpen.value = false
  }
}

const onSearchFocus = () => {
  if (searchQuery.value.trim()) {
    searchOpen.value = true
  }
}

watch(searchQuery, (value) => {
  searchOpen.value = !!String(value || "").trim()
})

const maybeToastNotifications = (count) => {
  const numericCount = Number(count || 0)
  if (!numericCount) return

  const signature = unreadSignature.value
  if (!signature) return

  const identity = getCurrentCustomerIdentity()
  if (identity !== lastToastIdentity.value) {
    lastToastIdentity.value = identity
    lastToastSignature.value = readLastToastSignature(identity)
  }

  const persistedSignature = readLastToastSignature(identity)
  if (signature === persistedSignature) {
    lastToastSignature.value = persistedSignature
    return
  }

  if (signature === lastToastSignature.value) return

  const globalState = getGlobalNotificationToastState()
  const now = Date.now()
  if (globalState?.signature === signature && now - Number(globalState?.at || 0) < 6000) {
    return
  }

  lastToastSignature.value = signature
  writeLastToastSignature(identity, signature)
  if (globalState) {
    globalState.signature = signature
    globalState.at = now
  }

  toast.info(`Bạn có ${count} thông báo mới`)
}

watch([notificationCount, unreadSignature], ([count]) => {
  maybeToastNotifications(count)
})

onMounted(async () => {
  lastToastIdentity.value = getCurrentCustomerIdentity()
  lastToastSignature.value = readLastToastSignature(lastToastIdentity.value)
  refreshCartCount()
  await Promise.all([loadCurrentUser(), loadSearchProducts()])
  document.addEventListener("click", handleDocumentClick)
  window.addEventListener("storage", refreshCartCount)
  window.addEventListener(CART_UPDATED_EVENT, refreshCartCount)
  window.addEventListener(AUTH_CONTEXT_CHANGED_EVENT, loadCurrentUser)
})

onUnmounted(() => {
  document.removeEventListener("click", handleDocumentClick)
  window.removeEventListener("storage", refreshCartCount)
  window.removeEventListener(CART_UPDATED_EVENT, refreshCartCount)
  window.removeEventListener(AUTH_CONTEXT_CHANGED_EVENT, loadCurrentUser)
})
</script>

<template>
  <div class="sn-topbar">
    <div class="sn-topbar__inner">
      <div class="sn-marquee">
        <div class="sn-marquee__track">
          Ưu đãi theo mốc đơn • Freeship toàn quốc • Đổi size dễ dàng • Hỗ trợ nhanh qua chat • Chất lượng đảm bảo
        </div>
      </div>
    </div>
  </div>

  <header class="sn-site-header">
    <div class="sn-site-header__inner">
      <div class="sn-nav-shell">

        <button class="sn-brand" type="button" @click="goHome">
          <span class="sn-brand__wordmark">
            <img :src="logo" alt="D" class="sn-brand__d-icon" />
            <span class="sn-brand__rest">irtyWave</span>
          </span>
        </button>

        <nav class="sn-menu">
          <RouterLink class="sn-menu-link" to="/trang-chu">Trang chủ</RouterLink>
          <RouterLink class="sn-menu-link" to="/san-pham">Sản phẩm</RouterLink>
          <RouterLink class="sn-menu-link" to="/gioi-thieu">Giới thiệu</RouterLink>
          <RouterLink class="sn-menu-link" to="/lien-he">Liên hệ</RouterLink>
        </nav>

        <div class="sn-actions">
          <div class="sn-search-shell">
            <form class="sn-search" @submit.prevent="handleSearch">
              <Search :size="18" :stroke-width="2" />
              <input
                v-model="searchQuery"
                type="text"
                placeholder="Tìm áo, quần, jeans, phụ kiện..."
                @focus="onSearchFocus"
              />
            </form>

            <transition name="sn-pop">
            <div v-if="searchOpen" class="sn-search-dropdown">
              <div class="sn-search-dropdown__meta">
                <span v-if="searchLoading">Đang tải sản phẩm...</span>
                <span v-else-if="searchMatches.length">Tìm thấy {{ searchMatches.length }} kết quả gần nhất</span>
                <span v-else>Không có kết quả phù hợp</span>
              </div>

              <div v-if="searchMatches.length" class="sn-search-list">
                <button
                  v-for="item in searchMatches"
                  :key="item.id || `${item.name}-${item.code}`"
                  type="button"
                  class="sn-search-item"
                  @click="openProductFromSearch(item)"
                >
                  <div class="sn-search-item__info">
                    <p class="sn-search-item__category">{{ item.category }}</p>
                    <h4>{{ item.name }}</h4>
                    <p class="sn-search-item__meta">{{ item.code || `SP-${item.id || 0}` }}</p>
                    <strong>{{ formatVND(item.price) }}</strong>
                  </div>
                  <div class="sn-search-item__thumb">
                    <img :src="item.image" :alt="item.name" />
                  </div>
                </button>
              </div>

              <button v-if="searchMatches.length" type="button" class="sn-search-view-all" @click="openSearchResultsPage">
                Xem toàn bộ kết quả →
              </button>
            </div>
            </transition>
          </div>

          <button class="sn-icon-button sn-hamburger" type="button" aria-label="Mở menu" @click="toggleMobileMenu">
            <Menu :size="18" />
          </button>

          <div class="sn-profile-wrapper">
            <button class="sn-user-btn" type="button" aria-label="Tài khoản" @click="toggleProfileMenu">
              <span class="sn-avatar-chip">
                <img v-if="userAvatar" :src="userAvatar" alt="avatar" class="sn-avatar-img" />
                <span v-else>{{ userInitials }}</span>
              </span>
              <span class="sn-identity">
                <span class="sn-name">{{ userDisplayName }}</span>
                <span class="sn-role">{{ userRoleLabel }}</span>
              </span>
            </button>
            <transition name="sn-pop">
            <div v-if="profileOpen" class="sn-profile-dropdown">
              <template v-if="isLoggedInAsCustomer">
                <button type="button" class="sn-dropdown-item" @click="openProfilePage">Tài khoản</button>
                <button type="button" class="sn-dropdown-item" @click="openOrdersPage">Đơn hàng</button>
                <button type="button" class="sn-dropdown-item" @click="openOrderLookupPage">Tra cứu đơn hàng</button>
                <button type="button" class="sn-dropdown-item sn-dropdown-item--danger" @click="logout">Đăng xuất</button>
              </template>
              <template v-else>
                <button type="button" class="sn-dropdown-item" @click="navigateTo('/auth/customer-login')">Đăng nhập</button>
                <button type="button" class="sn-dropdown-item" @click="navigateTo('/auth/customer-register')">Tạo tài khoản</button>
              </template>
            </div>
            </transition>
          </div>

          <button class="sn-icon-button sn-notify-icon" type="button" aria-label="Thông báo" @click="openNotificationsPage">
            <Bell :size="18" :stroke-width="2" />
            <span v-if="notificationCount" class="sn-notify-count">{{ notificationCount }}</span>
          </button>

          <button class="sn-icon-button sn-cart-icon" type="button" aria-label="Giỏ hàng" @click="handleCartClick">
            <ShoppingCart :size="18" :stroke-width="2" />
            <span v-if="resolvedCartCount" class="sn-cart-count">{{ resolvedCartCount }}</span>
          </button>
        </div>
      </div>

      <transition name="sn-drawer">
      <div v-if="mobileOpen" class="sn-mobile-menu">
        <div class="sn-mobile-menu__panel">
          <button type="button" @click="navigateTo('/trang-chu')">Trang chủ</button>
          <button type="button" @click="navigateTo('/san-pham')">Sản phẩm</button>
          <button type="button" @click="navigateTo('/gioi-thieu')">Giới thiệu</button>
          <button type="button" @click="navigateTo('/lien-he')">Liên hệ</button>
        </div>
      </div>
      </transition>
    </div>
  </header>
</template>

<style>
/* =============================================
   SiteNav — global styles, sn- prefix
   ============================================= */
.sn-topbar {
  position: relative;
  overflow: hidden;
  border-bottom: 1px solid rgba(255, 255, 255, 0.18);
  background: linear-gradient(135deg, #c5162d 0%, #8f1121 100%);
}

.sn-topbar::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.32;
  background-image:
    repeating-linear-gradient(45deg, rgba(255, 255, 255, 0.08) 0 1px, transparent 1px 18px),
    repeating-linear-gradient(-45deg, rgba(255, 255, 255, 0.06) 0 1px, transparent 1px 18px);
}

.sn-topbar__inner {
  width: 100%;
  padding: 10px 18px;
}

.sn-marquee {
  overflow: hidden;
  white-space: nowrap;
}

.sn-marquee__track {
  display: inline-block;
  padding-left: 100%;
  color: white;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.04em;
  animation: sn-marquee 24s linear infinite;
}

@keyframes sn-marquee {
  0%   { transform: translateX(0); }
  100% { transform: translateX(-100%); }
}

.sn-site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background:
    radial-gradient(circle at 10% 18%, rgba(239, 68, 68, 0.17), transparent 42%),
    linear-gradient(155deg, #09090d 0%, #17161e 45%, #08080c 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(14px);
  box-shadow: 0 12px 28px rgba(8, 10, 20, 0.35);
  overflow: visible;
}

.sn-site-header::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.25;
  background-image:
    repeating-linear-gradient(0deg, rgba(255, 255, 255, 0.06) 0 1px, transparent 1px 16px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 0 1px, transparent 1px);
  background-size: auto, 16px 100%;
}

.sn-site-header__inner {
  position: relative;
  z-index: 1;
}

.sn-site-header__inner {
  width: min(1260px, calc(100% - 36px));
  margin: 0 auto;
  padding: 12px 0 10px;
}

.sn-nav-shell {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 24px;
}

/* Brand */
.sn-brand {
  border: 0;
  padding: 0;
  background: transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0;
}

.sn-brand__wordmark {
  display: inline-flex;
  align-items: center;
  line-height: 1;
  user-select: none;
  transform: translateY(-1px);
  font-size: 28px;
}

.sn-brand__d-icon {
  width: 1.4em;
  height: 1.4em;
  object-fit: contain;
  margin-right: -0.04em;
  margin-top: -0.15em;
  vertical-align: middle;
  filter:
    sepia(1)
    saturate(20)
    hue-rotate(326deg)
    brightness(1.3)
    contrast(1.1);
  transition: transform 0.25s ease, filter 0.25s ease;
}

.sn-brand:hover .sn-brand__d-icon {
  transform: scale(1.1) rotate(-3deg);
  filter:
    sepia(1)
    saturate(22)
    hue-rotate(326deg)
    brightness(1.4)
    contrast(1.12);
}

.sn-brand__rest {
  font-size: 1em;
  font-weight: 800;
  letter-spacing: -0.035em;
  color: #ffffff;
  font-family: "Be Vietnam Pro", "Segoe UI", Tahoma, sans-serif;
  transition: color 0.25s ease;
}

.sn-brand:hover .sn-brand__rest {
  color: #c5162d;
}

/* Menu */
.sn-menu {
  display: flex;
  align-items: center;
  gap: 24px;
  min-width: 0;
  overflow: visible;
}

.sn-site-header .sn-menu > a,
.sn-site-header .sn-dropdown > a {
  white-space: nowrap;
  padding: 10px 12px;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.76);
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
  border: 0;
  background: transparent;
  cursor: pointer;
  display: inline-block;
  position: relative;
}

.sn-site-header .sn-menu-link.router-link-active,
.sn-site-header .sn-menu-link.router-link-exact-active {
  color: #ff6b6b;
  background: rgba(220, 38, 38, 0.2);
  box-shadow: inset 0 0 0 1px rgba(255, 107, 107, 0.28);
}

.sn-site-header .sn-menu-link.router-link-active::after,
.sn-site-header .sn-menu-link.router-link-exact-active::after {
  content: "";
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 6px;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, #ff6b6b 0%, #dc2626 100%);
}

.sn-site-header .sn-menu > a:hover,
.sn-site-header .sn-dropdown > a:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.sn-dropdown {
  position: relative;
  padding-bottom: 14px;
  margin-bottom: -14px;
}

.sn-dropdown::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  top: 100%;
  height: 14px;
}

.sn-dropdown:hover .sn-dropdown-panel {
  display: block;
}

.sn-dropdown-panel {
  position: absolute;
  top: 100%;
  margin-top: 2px;
  left: 0;
  display: none;
  width: min(720px, 90vw);
  padding: 16px;
  border: 1px solid rgba(143, 17, 33, 0.12);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.99);
  box-shadow: 0 20px 50px rgba(21, 21, 21, 0.12);
  z-index: 200;
}

.sn-dropdown-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 14px;
}

.sn-panel-block {
  border: 1px solid rgba(143, 17, 33, 0.09);
  border-radius: 16px;
  padding: 16px;
  background: linear-gradient(180deg, #fffefe 0%, #fff9fa 100%);
}

.sn-panel-block h4 {
  margin: 0 0 10px;
  color: #6f6a6d;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.sn-panel-block p {
  margin: 8px 0 0;
  color: #6f6a6d;
  font-size: 13px;
  line-height: 1.6;
}

.sn-panel-links {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.sn-panel-links a {
  padding: 10px 12px;
  border: 1px solid rgba(143, 17, 33, 0.09);
  border-radius: 12px;
  background: white;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  color: #151515;
  display: block;
}

.sn-panel-links a:hover {
  border-color: rgba(197, 22, 45, 0.3);
  color: #8f1121;
}

.sn-panel-block--cta {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(135deg, rgba(197, 22, 45, 0.96) 0%, rgba(143, 17, 33, 0.98) 100%);
}

.sn-panel-block--cta h4,
.sn-panel-block--cta p {
  color: rgba(255, 255, 255, 0.9);
}

.sn-panel-block--soft {
  background: linear-gradient(135deg, #ffe6ea 0%, #ffd6dd 100%);
}

.sn-panel-block--soft h4,
.sn-panel-block--soft p {
  color: #8f1121;
}

.sn-pill-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  align-self: flex-start;
  min-height: 40px;
  padding: 0 16px;
  border: 0;
  border-radius: 999px;
  background: white;
  color: #8f1121;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

/* Actions */
.sn-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sn-search-shell {
  position: relative;
}

.sn-search {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 320px;
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
  border-radius: 999px;
  box-shadow: none;
}

.sn-search svg {
  color: rgba(255, 255, 255, 0.68);
  flex-shrink: 0;
}

.sn-search input {
  width: 100%;
  border: 0 !important;
  outline: 0;
  background: transparent !important;
  box-shadow: none !important;
  border-radius: 0 !important;
  padding: 0 !important;
  appearance: none;
  -webkit-appearance: none;
  font: inherit;
  color: #f8fafc;
  font-size: 14px;
}

.sn-search input::placeholder {
  color: rgba(255, 255, 255, 0.52);
}

.sn-search-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  width: min(620px, 90vw);
  max-height: 70vh;
  overflow: auto;
  padding: 10px;
  border: 1px solid rgba(143, 17, 33, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.99);
  box-shadow: 0 20px 44px rgba(21, 21, 21, 0.18);
  z-index: 240;
  animation: sn-search-drop-in 0.2s ease;
}

@keyframes sn-search-drop-in {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.sn-search-dropdown__meta {
  padding: 4px 6px 10px;
  color: #6f6a6d;
  font-size: 13px;
}

.sn-search-list {
  display: grid;
  gap: 8px;
}

.sn-search-item {
  width: 100%;
  border: 1px solid #eddde0;
  border-radius: 14px;
  background: #fff;
  padding: 10px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 116px;
  gap: 12px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.sn-search-item:hover {
  border-color: rgba(197, 22, 45, 0.38);
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(143, 17, 33, 0.1);
}

.sn-search-item__info {
  display: grid;
  align-content: start;
  gap: 2px;
}

.sn-search-item__category {
  margin: 0;
  font-size: 12px;
  color: #8f1121;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.sn-search-item__info h4 {
  margin: 0;
  color: #1b1719;
  font-size: 15px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.sn-search-item__meta {
  margin: 0;
  color: #6f6a6d;
  font-size: 12px;
}

.sn-search-item__info strong {
  color: #c5162d;
  font-size: 16px;
}

.sn-search-item__thumb {
  width: 100%;
  height: 88px;
  border-radius: 10px;
  overflow: hidden;
  background: #f8f1f3;
}

.sn-search-item__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sn-search-view-all {
  margin-top: 8px;
  width: 100%;
  min-height: 38px;
  border: 1px solid #e8d8db;
  border-radius: 10px;
  background: #fff;
  color: #8f1121;
  font-weight: 700;
  cursor: pointer;
}

.sn-icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  color: #ffffff;
  cursor: pointer;
}

.sn-icon-button:hover {
  border-color: rgba(255, 107, 107, 0.38);
  background: rgba(220, 38, 38, 0.2);
}

.sn-hamburger {
  display: none;
}

.sn-cart-icon {
  position: relative;
}

.sn-notify-icon {
  position: relative;
}

.sn-cart-count {
  position: absolute;
  top: -7px;
  right: -7px;
  display: inline-grid;
  place-items: center;
  min-width: 19px;
  height: 19px;
  padding: 0 5px;
  border-radius: 999px;
  background: #c5162d;
  color: white;
  font-size: 11px;
  font-weight: 700;
}

.sn-notify-count {
  position: absolute;
  top: -7px;
  right: -7px;
  display: inline-grid;
  place-items: center;
  min-width: 19px;
  height: 19px;
  padding: 0 5px;
  border-radius: 999px;
  background: #c5162d;
  color: white;
  font-size: 11px;
  font-weight: 700;
}

/* Profile */
.sn-profile-wrapper {
  position: relative;
}

.sn-user-btn {
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  min-height: 42px;
  padding: 6px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.sn-avatar-chip {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffe4e8 0%, #ffd3da 100%);
  color: #8f1121;
  font-size: 12px;
  font-weight: 700;
  overflow: hidden;
  flex-shrink: 0;
}

.sn-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sn-identity {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.15;
}

.sn-name {
  font-size: 13px;
  font-weight: 700;
  color: #ffffff;
}

.sn-role {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.64);
}

.sn-profile-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 190px;
  padding: 8px;
  border: 1px solid #e8d8db;
  border-radius: 14px;
  background: white;
  box-shadow: 0 18px 40px rgba(21, 21, 21, 0.14);
  z-index: 200;
}

.sn-dropdown-item {
  display: block;
  width: 100%;
  padding: 12px 14px;
  text-align: left;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #151515;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.sn-dropdown-item:hover {
  background: #f8f0f2;
}

.sn-dropdown-item--danger {
  color: #c5162d;
}

.sn-dropdown-item--danger:hover {
  background: #fee7eb;
}

/* Mobile menu */
.sn-mobile-menu {
  padding-top: 12px;
}

.sn-mobile-menu__panel {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 18px;
  background: #1e1e2d;
  box-shadow: 0 10px 24px rgba(6, 8, 16, 0.38);
}

.sn-mobile-menu__panel button {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-height: 42px;
  padding: 0 14px;
  border: 0;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.88);
  font-weight: 700;
  cursor: pointer;
  font: inherit;
}

.sn-mobile-menu__panel button:hover {
  background: rgba(220, 38, 38, 0.2);
  color: #ff6b6b;
}

.sn-pop-enter-active,
.sn-pop-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.sn-pop-enter-from,
.sn-pop-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.985);
}

.sn-drawer-enter-active,
.sn-drawer-leave-active {
  transition: opacity 0.2s ease, transform 0.22s ease;
}

.sn-drawer-enter-from,
.sn-drawer-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Responsive */
@media (max-width: 900px) {
  .sn-menu {
    display: none;
  }

  .sn-hamburger {
    display: inline-flex;
  }

  .sn-search {
    min-width: 160px;
  }

  .sn-search-dropdown {
    width: min(520px, 92vw);
    left: 0;
  }
}

@media (max-width: 1200px) {
  .sn-site-header__inner {
    width: min(1260px, calc(100% - 20px));
  }

  .sn-nav-shell {
    gap: 12px;
  }

  .sn-menu {
    gap: 8px;
  }

  .sn-site-header .sn-menu > a,
  .sn-site-header .sn-dropdown > a {
    padding: 8px 8px;
    font-size: 13px;
  }

  .sn-actions {
    gap: 8px;
  }

  .sn-search {
    width: min(280px, 36vw);
  }

  .sn-identity {
    max-width: 140px;
  }
}

@media (max-width: 1024px) {
  .sn-menu {
    display: none;
  }

  .sn-hamburger {
    display: inline-flex;
  }

  .sn-search {
    width: min(240px, 42vw);
    min-width: 150px;
  }

  .sn-identity {
    display: none;
  }
}

@media (max-width: 640px) {
  .sn-site-header__inner {
    width: calc(100% - 16px);
    padding: 10px 0 8px;
  }

  .sn-nav-shell {
    gap: 8px;
    grid-template-columns: auto 1fr auto;
  }

  .sn-actions {
    gap: 6px;
  }

  .sn-icon-button {
    width: 38px;
    height: 38px;
  }

  .sn-user-btn {
    padding: 4px 8px;
    min-height: 38px;
    gap: 6px;
  }

  .sn-identity {
    display: none;
  }

  .sn-search {
    display: none;
  }
}
</style>


