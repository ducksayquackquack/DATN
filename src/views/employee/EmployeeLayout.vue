<template>
  <div class="app">

    <aside class="sidebar">
      <div class="brand">
        <span class="brand-wordmark">
          <img :src="logo" alt="D" class="brand-d-icon" />
          <span class="brand-rest">irtyWave</span>
        </span>
      </div>

      <div class="nav">

        <div class="navgrp">
          <h4>Tổng quan</h4>

          <router-link to="/employee/trang-chu" class="navlink">
            <span class="left">
              <span class="material-icons-outlined" style="font-size: 18px;">home</span>
              Trang chủ
            </span>
          </router-link>

          <router-link to="/employee/ban-hang" class="navlink nav-sales">
            <span class="left">
              <span class="material-icons-outlined" style="font-size: 18px;">shopping_cart</span>
              Bán hàng
            </span>
          </router-link>

          <router-link to="/employee/dashboard" class="navlink">
            <span class="left">
              <BarChart3 :size="18" />
              Thống kê
            </span>
          </router-link>

          <router-link to="/employee/hoa-don/list" class="navlink">
            <span class="left">
              <FileText :size="18" />
              Hoá đơn
            </span>
          </router-link>

          <router-link to="/employee/san-pham/bien-the" class="navlink">
            <span class="left">
              <Shirt :size="18" />
              Sản phẩm
            </span>
          </router-link>

          <router-link to="/employee/giao-ca" class="navlink">
            <span class="left">
              <span class="material-icons-outlined" style="font-size: 18px;">schedule</span>
              Mở ca / Kết ca
            </span>
          </router-link>

          <!-- <router-link to="/employee/giao-ca-test" class="navlink">
            <span class="left">
              <span class="material-icons-outlined" style="font-size: 18px;">swap_horiz</span>
              Bàn giao ca (Test)
            </span>
          </router-link> -->

          <router-link to="/employee/dang-ky-doi-ca" class="navlink">
            <span class="left">
              <span class="material-icons-outlined" style="font-size: 18px;">swap_horiz</span>
              Đăng ký đổi ca
            </span>
          </router-link>

          <router-link to="/employee/chat" class="navlink">
            <span class="left">
              <MessageSquare :size="18" />
              Chat khách hàng
            </span>
          </router-link>

          <router-link to="/employee/khuyen-mai/list" class="navlink">
            <span class="left">
              <Tag :size="18" />
              Khuyến mãi
            </span>
          </router-link>
        </div>

      </div>

      <div style="margin-top:14px; padding-top:12px; border-top:1px solid rgba(255,255,255,0.08);" class="muted">
        <small style="color:rgba(255,255,255,0.5)">Ca làm: <b style="color:#ff6b6b">{{ currentShiftName }}</b></small>
      </div>
    </aside>

    <div class="content">

      <div class="topbar">
        <div class="row">

          <div class="user-menu">
            <div class="time-display">
              <div class="time">{{ formatTime() }}</div>
              <div class="date">{{ formatDate() }}</div>
            </div>

            <button class="btn notify-btn" @click="goToNotifications">
              <Bell :size="18" :stroke-width="2" />
              <span v-if="notificationCount" class="notify-badge">{{ notificationCount }}</span>
            </button>

            <div
              class="avatar-wrapper"
              :class="{ 'avatar-wrapper--open': openUserMenu }"
              @click.stop="toggleUserMenu"
              @mouseenter="openUserMenu = true"
              @mouseleave="openUserMenu = false"
            >
              <div class="avatar">
                <img v-if="userAvatar" :src="userAvatar" alt="Avatar" class="avatar-img" />
                <span v-else>{{ avatarInitials }}</span>
              </div>
              <div class="user-info">
                <div class="user-name">{{ userDisplayName }}</div>
                <div class="user-role">{{ userRole }}</div>
              </div>

              <transition name="menu-pop">
                <div v-if="openUserMenu" class="dropdown user-dropdown">
                  <button class="dropdown-item" type="button" @click="goToProfile">
                    Hồ sơ
                  </button>
                  <button class="dropdown-item logout" type="button" @click="logout">
                    Đăng xuất
                  </button>
                </div>
              </transition>
            </div>
          </div>

        </div>
      </div>

      <main class="wrap">
        <router-view v-slot="{ Component }">
          <transition name="slide-up" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>

    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { BarChart3, Bell, FileText, MessageSquare, Search, Shirt, Tag } from "lucide-vue-next"
import taiKhoanService from '../../services/taiKhoanService'
import { getAllNhanVien, getNhanVienByTaiKhoanId } from '../../services/nhanVienService'
import { useNotifications } from '../../composables/useNotifications'
import {
  AUTH_CONTEXT_CHANGED_EVENT,
  extractAccountList,
  resolveAccountByRole
} from '../../utils/authContext'
import logo from '../../assets/img/logo/new logo.png?url'
import "./dashboard.css"

const router = useRouter()
const route = useRoute()
const OPS_TOAST_SHIFT_CLASS = 'ops-toast-shift'
const openUserMenu = ref(false)
const userAvatar = ref('')
const userDisplayName = ref('Nhân viên')
const userRole = ref('Nhân viên')
const { notifications, unreadCount: notificationCount, refresh: refreshNotifications } = useNotifications('employee')
const lastToastSignature = ref('')
let notificationPollTimer = null
const NOTIFICATION_TOAST_SESSION_KEY = 'ops:notification-toast-signature:employee'

const currentTime = ref(new Date())

const avatarInitials = computed(() => {
  const name = String(userDisplayName.value || 'NV').trim()
  const words = name.split(/\s+/).filter(Boolean)
  if (words.length >= 2) return `${words[0][0]}${words[words.length - 1][0]}`.toUpperCase()
  return name.slice(0, 2).toUpperCase()
})

const getAvatarStorageKey = (accountId) => `avatar:${accountId}`

const toDisplayNameFromEmail = (email = '') => {
  const localPart = String(email || '').split('@')[0]
  const cleaned = localPart.replace(/[._-]+/g, ' ').trim()
  if (!cleaned) return 'Nhân viên'
  return cleaned
    .split(/\s+/)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

const findNhanVienProfileByTaiKhoanId = async (taiKhoanId) => {
  try {
    const byTaiKhoan = await getNhanVienByTaiKhoanId(taiKhoanId)
    const payload = byTaiKhoan?.data
    const profile = Array.isArray(payload)
      ? payload[0]
      : (Array.isArray(payload?.content) ? payload.content[0] : payload)
    if (profile?.tenNhanVien) return profile
  } catch {
    // Fallback below.
  }

  try {
    const allRes = await getAllNhanVien()
    const list = extractAccountList(allRes?.data)
    return list.find((item) => {
      const inlineTaiKhoanId = item?.taiKhoan?.id
      return Number(item?.idTaiKhoan || inlineTaiKhoanId) === Number(taiKhoanId)
    }) || null
  } catch {
    return null
  }
}

const loadTopbarUser = async () => {
  const userEmail = String(localStorage.getItem('userEmail') || '').trim().toLowerCase()
  userDisplayName.value = toDisplayNameFromEmail(userEmail)
  userAvatar.value = ''
  userRole.value = 'Nhân viên'

  const resolvedAccount = await resolveAccountByRole({
    service: taiKhoanService,
    expectedRoles: ['EMPLOYEE']
  })

  if (!resolvedAccount?.id) {
    return
  }

  const resolvedId = String(resolvedAccount.id)

  userRole.value = 'Nhân viên'
  userDisplayName.value = toDisplayNameFromEmail(resolvedAccount.email || userEmail)

  const profile = await findNhanVienProfileByTaiKhoanId(resolvedId)
  if (profile?.tenNhanVien) {
    userDisplayName.value = String(profile.tenNhanVien)
  }

  const localAvatar = localStorage.getItem(getAvatarStorageKey(resolvedId))
  userAvatar.value = localAvatar || resolvedAccount?.avatar || ''
}

const handleAuthContextChanged = () => {
  loadTopbarUser()
}

const readNotificationToastSignatureSession = () => {
  try {
    return String(sessionStorage.getItem(NOTIFICATION_TOAST_SESSION_KEY) || '')
  } catch {
    return ''
  }
}

const writeNotificationToastSignatureSession = (value) => {
  try {
    const nextValue = String(value || '')
    if (!nextValue) sessionStorage.removeItem(NOTIFICATION_TOAST_SESSION_KEY)
    else sessionStorage.setItem(NOTIFICATION_TOAST_SESSION_KEY, nextValue)
  } catch {
    // Ignore storage failures.
  }
}

const unreadSignature = computed(() => {
  return notifications.value
    .filter((item) => !item?.read)
    .map((item) => String(item?.id || ''))
    .filter(Boolean)
    .join('|')
})

const maybeToastNotifications = () => {
  const count = Number(notificationCount.value || 0)
  const signature = String(unreadSignature.value || '')

  if (!count || !signature) {
    lastToastSignature.value = ''
    writeNotificationToastSignatureSession('')
    return
  }

  if (signature === lastToastSignature.value) return

  lastToastSignature.value = signature
  writeNotificationToastSignatureSession(signature)
  window.toast?.info?.(`Bạn có ${count} thông báo cần xử lý`, 4500)
}

const startNotificationPolling = () => {
  if (notificationPollTimer) clearInterval(notificationPollTimer)
  notificationPollTimer = setInterval(() => {
    refreshNotifications()
  }, 12000)
}

const syncOpsToastOffset = (isOpen) => {
  const isMobile = window.matchMedia('(max-width: 768px)').matches
  const topValue = isOpen
    ? (isMobile ? '146px' : '188px')
    : (isMobile ? '74px' : '92px')
  document.body.style.setProperty('--ops-toast-top', topValue)
}

watch(unreadSignature, () => {
  maybeToastNotifications()
}, { immediate: true })

watch(openUserMenu, (isOpen) => {
  document.body.classList.toggle(OPS_TOAST_SHIFT_CLASS, Boolean(isOpen))
  syncOpsToastOffset(Boolean(isOpen))
})

// Update time every second
setInterval(() => {
  currentTime.value = new Date()
}, 1000)

const formatTime = () => {
  const hours = String(currentTime.value.getHours()).padStart(2, '0')
  const minutes = String(currentTime.value.getMinutes()).padStart(2, '0')
  const seconds = String(currentTime.value.getSeconds()).padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
}

const formatDate = () => {
  const day = String(currentTime.value.getDate()).padStart(2, '0')
  const month = String(currentTime.value.getMonth() + 1).padStart(2, '0')
  const year = currentTime.value.getFullYear()
  return `${day}/${month}/${year}`
}

// Get current shift based on time
const currentShiftName = computed(() => {
  const hour = currentTime.value.getHours()
  
  if (hour >= 7 && hour < 12) return 'Sáng (7h-12h)'
  if (hour >= 12 && hour < 17) return 'Chiều (12h-17h)'
  if (hour >= 17 && hour < 22) return 'Tối (17h-22h)'
  return 'Ngoài giờ'
})

function logout() {
  localStorage.removeItem("role")
  localStorage.removeItem("userId")
  localStorage.removeItem("userEmail")
  router.push("/auth/staff-login")
}

function goHome() {
  router.push("/trang-chu")
}

function goToProfile() {
  router.push("/employee/profile")
  openUserMenu.value = false
}

function goToNotifications() {
  router.push('/employee/notifications')
}

function toggleUserMenu() {
  openUserMenu.value = !openUserMenu.value
}

function closeUserMenu(event) {
  if (!event.target.closest(".avatar-wrapper")) {
    openUserMenu.value = false
  }
}

onMounted(() => {
  lastToastSignature.value = readNotificationToastSignatureSession()
  document.addEventListener("click", closeUserMenu)
  window.addEventListener(AUTH_CONTEXT_CHANGED_EVENT, handleAuthContextChanged)
  loadTopbarUser()
  refreshNotifications()
  startNotificationPolling()
  syncOpsToastOffset(false)
})

onUnmounted(() => {
  document.removeEventListener("click", closeUserMenu)
  window.removeEventListener(AUTH_CONTEXT_CHANGED_EVENT, handleAuthContextChanged)
  document.body.classList.remove(OPS_TOAST_SHIFT_CLASS)
  document.body.style.removeProperty('--ops-toast-top')
  if (notificationPollTimer) {
    clearInterval(notificationPollTimer)
    notificationPollTimer = null
  }
})

watch(() => route.fullPath, () => {
  loadTopbarUser()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/icon?family=Material+Icons+Outlined');

.brand {
  display: flex;
  align-items: center;
  gap: 0;
}

.brand-wordmark {
  display: inline-flex;
  align-items: center;
  line-height: 1;
  user-select: none;
  font-size: 30px;
}

.brand-d-icon {
  width: 1.4em;
  height: 1.4em;
  object-fit: contain;
  margin-right: -0.04em;
  margin-top: -0.15em;
  filter:
    sepia(1)
    saturate(20)
    hue-rotate(326deg)
    brightness(1.3)
    contrast(1.1);
  transition: transform 0.25s ease, filter 0.25s ease;
}

.brand:hover .brand-d-icon {
  transform: scale(1.1) rotate(-3deg);
  filter:
    sepia(1)
    saturate(22)
    hue-rotate(326deg)
    brightness(1.4)
    contrast(1.12);
}

.brand-rest {
  font-size: 1em;
  font-weight: 800;
  letter-spacing: -0.035em;
  color: #ffffff;
  transition: color 0.25s ease;
}

.brand:hover .brand-rest {
  color: #ff6b6b;
}

.navlink {
  display:flex;
  align-items:center;
  padding:10px;
  border-radius:12px;
  cursor:pointer;
  transition: all .25s ease;
  color: rgba(255,255,255,0.7);
  text-decoration: none;
}

.navlink .left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.navlink svg {
  stroke-width: 2;
  flex-shrink: 0;
}

.navlink:hover {
  background: rgba(255,255,255,0.06);
  transform: translateX(4px);
  color: #ffffff;
}

.router-link-active {
  background: rgba(220,38,38,0.18);
  border-left: 3px solid #dc2626;
  color: #ff6b6b;
}

/* Bán hàng link */
.nav-sales {
  font-weight: inherit;
}
.nav-sales.router-link-active {
  background: rgba(220,38,38,0.18);
  border-left: 3px solid #dc2626;
  color: #ff6b6b;
}

.user-menu {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-left: auto;
}

.time-display {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-right: 12px;
}

.time {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  font-family: inherit;
}

.date {
  font-size: 11px;
  color: #6b7280;
  margin-top: 2px;
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid rgba(255,255,255,0.08);
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  background: white;
  border: 1px solid #e5e7eb;
  transition: all 0.2s;
}

.avatar-wrapper--open {
  z-index: 10050;
}

.avatar-wrapper:hover {
  background: #f3f4f6;
  border-color: #1f2937;
  transform: translateY(-1px);
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  line-height: 1.2;
}

.user-role {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.2;
  margin-top: 2px;
}

.dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: #ffffff;
  border-radius: 12px;
  padding: 8px;
  width: 180px;
  box-shadow: 0 10px 25px rgba(0,0,0,.25);
  z-index: 10060;
}

.dropdown-item {
  border: 0;
  background: transparent;
  width: 100%;
  text-align: left;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: .2s;
}

.dropdown-item:hover {
  background: #f2f2f2;
}

.logout {
  color: #dc2626;
}

.menu-pop-enter-active,
.menu-pop-leave-active {
  transition: all 0.18s ease;
}

.menu-pop-enter-from,
.menu-pop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(40px);
}

.slide-up-enter-active {
  transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

.slide-up-enter-to {
  opacity: 1;
  transform: translateY(0);
}

.slide-up-leave-from {
  opacity: 1;
  transform: translateY(0);
}

.slide-up-leave-active {
  transition: all 0.2s ease;
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.notify-btn {
  position: relative;
}

.notify-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #dc2626;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: grid;
  place-items: center;
  padding: 0 5px;
}

@media (max-width: 1024px) {
  .time-display { display: none; }
  .user-info { display: none; }
}
@media (max-width: 768px) {
  .app { grid-template-columns: 1fr !important; }
  .sidebar {
    position: fixed; top: 0; left: 0; z-index: 999;
    width: 268px !important; height: 100vh;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
  }
  .app:not(.sidebar-collapsed) .sidebar {
    transform: translateX(0);
    opacity: 1 !important; pointer-events: auto !important;
  }
  .app.sidebar-collapsed .sidebar {
    transform: translateX(-100%);
    width: 268px !important; padding: 10px !important;
    opacity: 1 !important; pointer-events: auto !important;
  }
  .topbar .row { padding: 10px 12px; gap: 8px; }
  .wrap { padding: 12px; }
}
</style>

