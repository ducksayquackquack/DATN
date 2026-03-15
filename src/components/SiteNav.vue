<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { Bell, Menu, Search, ShoppingCart } from "lucide-vue-next"
import taiKhoanService from "../services/taiKhoanService"
import { getKhachHangByTaiKhoanId } from "../services/KhachHangService"
import { getVietnameseNameByEmail } from "../utils/vietnameseNames"
import { useNotifications } from "../composables/useNotifications"
import { useToast } from "../composables/useToast"
import {
  AUTH_CONTEXT_CHANGED_EVENT,
  resolveAccountByRole
} from "../utils/authContext"
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
const userAvatar = ref("")
const userDisplayName = ref("Tài khoản")
const userRoleLabel = ref("Khách hàng")
const internalCartCount = ref(0)
const { notifications, unreadCount: notificationCount } = useNotifications("customer")

const getNotificationToastKey = () => {
  const userId = String(localStorage.getItem("userId") || "guest").trim() || "guest"
  return `customer:notification-toast:last-signature:${userId}`
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

const refreshCartCount = () => {
  const stored = JSON.parse(localStorage.getItem("cart") || "{}")
  internalCartCount.value = Object.values(stored).reduce((s, v) => s + Number(v || 0), 0)
}

const loadCurrentUser = async () => {
  const storedRole = normalizeRole(localStorage.getItem("role"))
  const userEmail = String(localStorage.getItem("userEmail") || "").trim().toLowerCase()
  userAvatar.value = ""
  userDisplayName.value = "Tài khoản"
  userRoleLabel.value = "Khách hàng"

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
    router.push("/san-pham")
    return
  }
  router.push({ path: "/san-pham", query: { q: searchQuery.value.trim() } })
}

const handleCartClick = () => {
  if (route.path === "/gio-hang") return
  router.push("/gio-hang")
}

const openNotificationsPage = () => {
  router.push("/customer/notifications")
}

const goHome = () => router.push("/home")

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

const logout = () => {
  profileOpen.value = false
  localStorage.removeItem("role")
  localStorage.removeItem("userId")
  localStorage.removeItem("userEmail")
  router.push("/login")
}

const handleDocumentClick = (event) => {
  const target = event.target
  if (!(target instanceof Element)) return
  if (!target.closest(".sn-profile-wrapper")) {
    profileOpen.value = false
  }
}

const maybeToastNotifications = (count) => {
  const numericCount = Number(count || 0)
  if (!numericCount) return

  const signature = unreadSignature.value
  if (!signature) return

  let lastSignature = ""
  try {
    lastSignature = String(sessionStorage.getItem(getNotificationToastKey()) || "")
  } catch {
    lastSignature = ""
  }

  if (signature === lastSignature) return

  try {
    sessionStorage.setItem(getNotificationToastKey(), signature)
  } catch {
    // Ignore storage issues and still show once for this render.
  }

  toast.info(`Bạn có ${count} thông báo mới`)
}

watch([notificationCount, unreadSignature], ([count]) => {
  maybeToastNotifications(count)
})

onMounted(async () => {
  refreshCartCount()
  await loadCurrentUser()
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
          <RouterLink class="sn-menu-link" to="/home">Trang chủ</RouterLink>
          <RouterLink class="sn-menu-link" to="/san-pham">Sản phẩm</RouterLink>
          <RouterLink class="sn-menu-link" to="/gioi-thieu">Giới thiệu</RouterLink>
          <RouterLink class="sn-menu-link" to="/tin-tuc">Tin tức</RouterLink>
          <RouterLink class="sn-menu-link" to="/lien-he">Liên hệ</RouterLink>
        </nav>

        <div class="sn-actions">
          <form class="sn-search" @submit.prevent="handleSearch">
            <Search :size="18" :stroke-width="2" />
            <input v-model="searchQuery" type="text" placeholder="Tìm áo, quần, jeans, phụ kiện..." />
          </form>

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
            <div v-if="profileOpen" class="sn-profile-dropdown">
              <button type="button" class="sn-dropdown-item" @click="openProfilePage">Tài khoản</button>
              <button type="button" class="sn-dropdown-item" @click="openOrdersPage">Đơn hàng</button>
              <button type="button" class="sn-dropdown-item sn-dropdown-item--danger" @click="logout">Đăng xuất</button>
            </div>
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

      <div v-show="mobileOpen" class="sn-mobile-menu">
        <div class="sn-mobile-menu__panel">
          <button type="button" @click="navigateTo('/home')">Trang chủ</button>
          <button type="button" @click="navigateTo('/san-pham')">Sản phẩm</button>
          <button type="button" @click="navigateTo('/gioi-thieu')">Giới thiệu</button>
          <button type="button" @click="navigateTo('/tin-tuc')">Tin tức</button>
          <button type="button" @click="navigateTo('/lien-he')">Liên hệ</button>
        </div>
      </div>
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
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid rgba(143, 17, 33, 0.08);
  backdrop-filter: blur(14px);
  box-shadow: 0 12px 28px rgba(21, 21, 21, 0.08);
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
  width: 1.08em;
  height: 1.08em;
  object-fit: contain;
  margin-right: -0.04em;
  vertical-align: middle;
  filter:
    sepia(1)
    saturate(14)
    hue-rotate(326deg)
    brightness(0.98)
    contrast(1.16)
    drop-shadow(0 3px 8px rgba(197, 22, 45, 0.22));
  transition: transform 0.25s ease, filter 0.25s ease;
}

.sn-brand:hover .sn-brand__d-icon {
  transform: scale(1.08) rotate(-3deg);
  filter:
    sepia(1)
    saturate(15)
    hue-rotate(326deg)
    brightness(1.03)
    contrast(1.18)
    drop-shadow(0 4px 12px rgba(197, 22, 45, 0.3));
}

.sn-brand__rest {
  font-size: 1em;
  font-weight: 800;
  letter-spacing: -0.035em;
  color: #151515;
  font-family: 'Segoe UI', 'Inter', 'Helvetica Neue', Arial, sans-serif;
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

.sn-menu > a,
.sn-dropdown > a {
  white-space: nowrap;
  padding: 10px 8px;
  border-radius: 12px;
  color: #151515;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  border: 0;
  background: transparent;
  cursor: pointer;
  display: inline-block;
}

.sn-menu-link.router-link-active,
.sn-menu-link.router-link-exact-active {
  color: #8f1121;
  background: rgba(197, 22, 45, 0.1);
}

.sn-menu > a:hover,
.sn-dropdown > a:hover {
  background: rgba(197, 22, 45, 0.06);
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

.sn-search {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 320px;
  padding: 10px 14px;
  border: 1px solid #e8d8db;
  background: white;
  border-radius: 999px;
  box-shadow: none;
}

.sn-search svg {
  color: #7b7276;
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
  color: #151515;
  font-size: 14px;
}

.sn-search input::placeholder {
  color: #9b9094;
}

.sn-icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border: 1px solid #e8d8db;
  border-radius: 12px;
  background: white;
  color: #151515;
  cursor: pointer;
}

.sn-icon-button:hover {
  border-color: rgba(197, 22, 45, 0.28);
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
  border: 1px solid #e8d8db;
  background: #fff;
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
  color: #151515;
}

.sn-role {
  font-size: 11px;
  color: #6f6a6d;
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
  border: 1px solid rgba(143, 17, 33, 0.1);
  border-radius: 18px;
  background: white;
  box-shadow: 0 8px 20px rgba(143, 17, 33, 0.08);
}

.sn-mobile-menu__panel button {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-height: 42px;
  padding: 0 14px;
  border: 0;
  border-radius: 12px;
  background: #fdf4f5;
  color: #8f1121;
  font-weight: 700;
  cursor: pointer;
  font: inherit;
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
}

@media (max-width: 640px) {
  .sn-search {
    display: none;
  }
}
</style>
