<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue"
import { useRouter } from "vue-router"
import { useRoute } from "vue-router"
import {
  FileText,
  Shirt,
  Tag,
  BookOpen,
  Layers,
  Ruler,
  Palette,
  CreditCard,
  Users,
  UserCog,
  House,
  Bell,
  Search,
  BarChart3,
  ChevronDown,
  ShoppingCart
} from "lucide-vue-next"
import taiKhoanService from '../../../services/taiKhoanService'
import { getAllNhanVien, getNhanVienByTaiKhoanId } from '../../../services/nhanVienService'
import { useNotifications } from '../../../composables/useNotifications'
import {
  AUTH_CONTEXT_CHANGED_EVENT,
  extractAccountList,
  resolveAccountByRole
} from '../../../utils/authContext'

import logo from '../../../assets/img/logo/new logo.png?url'

const router = useRouter()
const route = useRoute()
const OPS_TOAST_SHIFT_CLASS = 'ops-toast-shift'

const openSections = ref({
  vanhanh: true,
  lichlamviec: true,
  danhmuc: true,
  taikhoan: true
})

const currentTime = ref(new Date())
const globalSearchText = ref("")
const openUserMenu = ref(false)
const userAvatar = ref('')
const userDisplayName = ref('Admin')
const userRole = ref('Quản trị viên')
const { unreadCount: notificationCount } = useNotifications('admin')
const notificationToastShown = ref(false)
let notificationToastBootstrapTimer = null

const avatarInitials = computed(() => {
  const name = String(userDisplayName.value || 'AD').trim()
  const words = name.split(/\s+/).filter(Boolean)
  if (words.length >= 2) return `${words[0][0]}${words[words.length - 1][0]}`.toUpperCase()
  return name.slice(0, 2).toUpperCase()
})

const getAvatarStorageKey = (accountId) => `avatar:${accountId}`

const toDisplayNameFromEmail = (email = '') => {
  const localPart = String(email || '').split('@')[0]
  const cleaned = localPart.replace(/[._-]+/g, ' ').trim()
  if (!cleaned) return 'Admin'
  return cleaned
    .split(/\s+/)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

const resolveLoggedInAdminAccount = async () => {
  return resolveAccountByRole({
    service: taiKhoanService,
    expectedRoles: ['ADMIN']
  })
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
  userRole.value = 'Quản trị viên'

  const resolvedAccount = await resolveLoggedInAdminAccount()

  if (!resolvedAccount?.id) {
    return
  }

  const resolvedId = String(resolvedAccount.id)

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

const maybeToastNotifications = (count) => {
  if (notificationToastShown.value || !Number(count)) return
  notificationToastShown.value = true
  window.toast?.info?.(`Bạn có ${count} thông báo cần xử lý`, 4500)
}

const startNotificationToastBootstrap = () => {
  const startedAt = Date.now()
  if (notificationToastBootstrapTimer) clearInterval(notificationToastBootstrapTimer)

  notificationToastBootstrapTimer = setInterval(() => {
    const count = Number(notificationCount.value || 0)
    if (count > 0) {
      maybeToastNotifications(count)
      clearInterval(notificationToastBootstrapTimer)
      notificationToastBootstrapTimer = null
      return
    }

    if (Date.now() - startedAt > 7000) {
      clearInterval(notificationToastBootstrapTimer)
      notificationToastBootstrapTimer = null
    }
  }, 300)
}

const syncOpsToastOffset = (isOpen) => {
  const isMobile = window.matchMedia('(max-width: 768px)').matches
  const topValue = isOpen
    ? (isMobile ? '146px' : '188px')
    : (isMobile ? '74px' : '92px')
  document.body.style.setProperty('--ops-toast-top', topValue)
}

watch(notificationCount, (count) => {
  if (!Number(count)) {
    notificationToastShown.value = false
    return
  }
  maybeToastNotifications(count)
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

const toggleSection = (section) => {
  openSections.value[section] = !openSections.value[section]
}

function logout() {
  localStorage.removeItem("role")
  localStorage.removeItem("userId")
  localStorage.removeItem("userEmail")
  router.push("/auth/staff-login")
}

function goToProfile() {
  router.push("/admin/profile")
  openUserMenu.value = false
}

function goToNotifications() {
  router.push('/admin/notifications')
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
  document.addEventListener("click", closeUserMenu)
  window.addEventListener(AUTH_CONTEXT_CHANGED_EVENT, handleAuthContextChanged)
  loadTopbarUser()
  startNotificationToastBootstrap()
  syncOpsToastOffset(false)
})

onUnmounted(() => {
  document.removeEventListener("click", closeUserMenu)
  window.removeEventListener(AUTH_CONTEXT_CHANGED_EVENT, handleAuthContextChanged)
  document.body.classList.remove(OPS_TOAST_SHIFT_CLASS)
  document.body.style.removeProperty('--ops-toast-top')
  if (notificationToastBootstrapTimer) {
    clearInterval(notificationToastBootstrapTimer)
    notificationToastBootstrapTimer = null
  }
})

watch(() => route.fullPath, () => {
  loadTopbarUser()
})

function handleGlobalSearch() {
  const keyword = globalSearchText.value.trim()

  if (!keyword) {
    router.push('/admin/hoa-don/list')
    return
  }

  router.push({
    path: '/admin/hoa-don/list',
    query: { keyword }
  })
}
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <div class="brand admin-brand">
        <span class="admin-brand__wordmark">
          <img :src="logo" alt="D" class="admin-brand__d-icon" />
          <span class="admin-brand__rest">irtyWave</span>
        </span>
      </div>

      <div class="nav">
        <div class="navgrp">
          <RouterLink to="/admin/trang-chu" class="nav-single">
            <span class="left">
              <House class="icon" /> Trang chủ
            </span>
          </RouterLink>
        </div>

        <!-- Bán hàng - Top level -->
        <div class="navgrp">
          <RouterLink to="/admin/ban-hang" class="nav-single nav-sales">
            <span class="left">
              <ShoppingCart class="icon" /> Bán hàng
            </span>
          </RouterLink>
        </div>

        <!-- Thống kê - Single button -->
        <div class="navgrp">
          <RouterLink to="/admin/thong-ke/doanh-thu" class="nav-single">
            <span class="left">
              <BarChart3 class="icon" /> Thống kê doanh thu
            </span>
          </RouterLink>
        </div>

        <!-- Vận hành -->
        <div class="navgrp">
          <div class="navgrp-header" @click="toggleSection('vanhanh')">
            <h4>Vận hành</h4>
            <ChevronDown :class="['chevron', { open: openSections.vanhanh }]" :size="18" />
          </div>
          
          <transition name="dropdown">
            <div v-show="openSections.vanhanh" class="navgrp-content">
              <RouterLink to="/admin/hoa-don/list">
                <span class="left">
                  <FileText class="icon" /> Hoá đơn
                </span>
              </RouterLink>

              <RouterLink to="/admin/san-pham/list">
                <span class="left">
                  <Shirt class="icon" /> Sản phẩm
                </span>
              </RouterLink>

              <RouterLink to="/admin/san-pham/bien-the">
                <span class="left">
                  <span class="material-icons-outlined icon-material">tune</span>
                  Biến thể sản phẩm
                </span>
              </RouterLink>

              <RouterLink to="/admin/khuyen-mai/list">
                <span class="left">
                  <Tag class="icon" /> Khuyến mãi
                </span>
              </RouterLink>
            </div>
          </transition>
        </div>

        <!-- Lịch làm việc -->
        <div class="navgrp">
          <div class="navgrp-header" @click="toggleSection('lichlamviec')">
            <h4>Lịch làm việc</h4>
            <ChevronDown :class="['chevron', { open: openSections.lichlamviec }]" :size="18" />
          </div>

          <transition name="dropdown">
            <div v-show="openSections.lichlamviec" class="navgrp-content">
              <RouterLink to="/admin/lich-lam-viec/lich-lam-viec">
                <span class="left">
                  <span class="material-icons-outlined icon-material">event</span>
                  Lịch làm việc
                </span>
              </RouterLink>

              <RouterLink to="/admin/lich-lam-viec/ca-lam">
                <span class="left">
                  <span class="material-icons-outlined icon-material">schedule</span>
                  Lịch ca làm
                </span>
              </RouterLink>

              <RouterLink to="/admin/lich-lam-viec/su-hoat-dong">
                <span class="left">
                  <span class="material-icons-outlined icon-material">history</span>
                  Lịch sử hoạt động
                </span>
              </RouterLink>
            </div>
          </transition>
        </div>

        <!-- Danh mục -->
        <div class="navgrp">
          <div class="navgrp-header" @click="toggleSection('danhmuc')">
            <h4>Danh mục</h4>
            <ChevronDown :class="['chevron', { open: openSections.danhmuc }]" :size="18" />
          </div>

          <transition name="dropdown">
            <div v-show="openSections.danhmuc" class="navgrp-content">
              <RouterLink to="/admin/danh-muc/list">
                <span class="left">
                  <BookOpen class="icon" /> Danh mục
                </span>
              </RouterLink>

              <RouterLink to="/admin/loai/list">
                <span class="left">
                  <Layers class="icon" /> Loại
                </span>
              </RouterLink>

              <RouterLink to="/admin/kich-thuoc/list">
                <span class="left">
                  <Ruler class="icon" /> Kích thước
                </span>
              </RouterLink>

              <RouterLink to="/admin/mau-sac/list">
                <span class="left">
                  <Palette class="icon" /> Màu sắc
                </span>
              </RouterLink>

              <RouterLink to="/admin/phuong-thuc-thanh-toan/list">
                <span class="left">
                  <CreditCard class="icon" /> Phương thức TT
                </span>
              </RouterLink>
            </div>
          </transition>
        </div>

        <!-- Tài khoản -->
        <div class="navgrp">
          <div class="navgrp-header" @click="toggleSection('taikhoan')">
            <h4>Tài khoản</h4>
            <ChevronDown :class="['chevron', { open: openSections.taikhoan }]" :size="18" />
          </div>

          <transition name="dropdown">
            <div v-show="openSections.taikhoan" class="navgrp-content">
              <RouterLink to="/admin/khach-hang/list">
                <span class="left">
                  <Users class="icon" /> Khách hàng
                </span>
              </RouterLink>

              <RouterLink to="/admin/nhan-vien/list">
                <span class="left">
                  <UserCog class="icon" /> Nhân viên
                </span>
              </RouterLink>
            </div>
          </transition>
        </div>
      </div>
    </aside>

    <div class="content">
      <div class="topbar">
        <div class="row">
          <div class="search topbar-search">
            <button class="search-icon-btn" type="button" @click="handleGlobalSearch" aria-label="Tìm kiếm">
              <Search size="18" />
            </button>
            <input
              v-model="globalSearchText"
              placeholder="Tìm mã hoá đơn, tên sản phẩm, khách hàng..."
              @keyup.enter="handleGlobalSearch"
            />
          </div>

          <div class="userbox">
            <div class="time-display">
              <div class="time">{{ formatTime() }}</div>
              <div class="date">{{ formatDate() }}</div>
            </div>

            <button class="btn notify-btn" @click="goToNotifications">
              <Bell size="18" />
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

<style>
@import "../../../assets/admin.css";
@import url('https://fonts.googleapis.com/icon?family=Material+Icons+Outlined');

.icon {
  width: 18px;
  height: 18px;
  margin-right: 10px;
  transition: all 0.25s ease;
}

.admin-brand {
  padding: 12px 12px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 12px;
}

.admin-brand__wordmark {
  display: inline-flex;
  align-items: center;
  line-height: 1;
  user-select: none;
  font-size: 30px;
}

.admin-brand__d-icon {
  width: 1.08em;
  height: 1.08em;
  object-fit: contain;
  margin-right: -0.04em;
  filter:
    sepia(1)
    saturate(14)
    hue-rotate(326deg)
    brightness(0.98)
    contrast(1.16)
    drop-shadow(0 3px 8px rgba(197, 22, 45, 0.24));
  transition: transform 0.25s ease, filter 0.25s ease;
}

.admin-brand:hover .admin-brand__d-icon {
  transform: scale(1.08) rotate(-3deg);
  filter:
    sepia(1)
    saturate(15)
    hue-rotate(326deg)
    brightness(1.03)
    contrast(1.18)
    drop-shadow(0 4px 12px rgba(197, 22, 45, 0.32));
}

.admin-brand__rest {
  font-size: 1em;
  font-weight: 800;
  letter-spacing: -0.035em;
  color: #111827;
  transition: color 0.25s ease;
}

.admin-brand:hover .admin-brand__rest {
  color: #c5162d;
}

.icon-material {
  font-size: 18px;
  margin-right: 10px;
  transition: all 0.25s ease;
}

.left {
  display: flex;
  align-items: center;
}

.navgrp {
  margin-bottom: 8px;
}

.navgrp-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 10px;
  transition: all 0.2s;
  user-select: none;
}

.navgrp-header:hover {
  background: #f1f5f9;
}

.navgrp-header h4 {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: #64748b;
}

.chevron {
  transition: transform 0.3s ease;
  color: #94a3b8;
}

.chevron.open {
  transform: rotate(180deg);
}

.navgrp-content {
  overflow: hidden;
}

.nav a,
.nav-single {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  margin: 2px 0;
  border-radius: 10px;
  transition: all 0.25s ease;
}

.nav a:hover,
.nav-single:hover {
  background: #f1f5f9;
  transform: translateX(4px);
}

.nav a:hover .icon,
.nav a:hover .icon-material,
.nav-single:hover .icon {
  transform: scale(1.15);
}

.nav a.router-link-active,
.nav-single.router-link-active {
  background: rgba(220,38,38,.10);
  border-left: 3px solid #dc2626;
}

.nav-sales {
  font-weight: inherit;
}

.nav-sales.router-link-active {
  background: rgba(220,38,38,.10);
  border-left: 3px solid #dc2626;
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
  border: 1px solid var(--line);
  transition: all 0.2s;
}

.avatar-wrapper--open {
  z-index: 10050;
}

.avatar-wrapper:hover {
  background: var(--line-light);
  border-color: var(--accent);
  transform: translateY(-1px);
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  line-height: 1.2;
}

.user-role {
  font-size: 12px;
  color: var(--muted);
  line-height: 1.2;
  margin-top: 2px;
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid rgba(255,255,255,0.08);
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
}

.dropdown-item {
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: 0.2s;
}

.dropdown-item:hover {
  background: #f3f4f6;
}

.logout {
  color: #dc2626;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

/* Dropdown animation */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.dropdown-enter-from {
  opacity: 0;
  max-height: 0;
  transform: translateY(-10px);
}

.dropdown-enter-to {
  opacity: 1;
  max-height: 500px;
  transform: translateY(0);
}

.dropdown-leave-from {
  opacity: 1;
  max-height: 500px;
  transform: translateY(0);
}

.dropdown-leave-to {
  opacity: 0;
  max-height: 0;
  transform: translateY(-10px);
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
</style>
