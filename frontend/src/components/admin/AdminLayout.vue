<template>
  <div class="admin-layout">

    <!-- SIDEBAR -->
    <div class="sidebar" :class="{ expanded }">

      <div class="admin-logo">
        <img :src="logo" class="logo-img" />
      </div>

      <!-- SCROLLABLE MENU -->
      <nav class="sidebar-scroll">

        <!-- Thống kê -->
        <router-link to="/admin/dashboard" class="menu-item">
          <BarChart3 class="icon" />
          <span v-if="expanded">Thống kê</span>
        </router-link>

        <!-- Bán hàng -->
        <router-link to="/admin/ban-hang" class="menu-item">
          <ShoppingCart class="icon" />
          <span v-if="expanded">Bán hàng tại quầy</span>
        </router-link>

        <!-- Đơn hàng -->
        <router-link to="/admin/hoa-don" class="menu-item">
          <FileText class="icon" />
          <span v-if="expanded">Quản lý đơn hàng</span>
        </router-link>

        <!-- Trả hàng -->
        <router-link to="/admin/tra-hang" class="menu-item">
          <RefreshCcw class="icon" />
          <span v-if="expanded">Trả hàng</span>
        </router-link>

        <!-- ================= PRODUCT DROPDOWN ================= -->
        <div class="menu-group">
          <div class="menu-title" @click="toggleProductMenu">
            <Shirt class="icon" />
            <span v-if="expanded">Quản lý sản phẩm</span>
            <ChevronDown 
              v-if="expanded" 
              class="chevron" 
              :class="{ rotate: showProductMenu }"
            />
          </div>

          <transition name="dropdown">
            <div v-show="expanded && showProductMenu" class="submenu">
              <router-link to="/admin/san-pham">Sản phẩm</router-link>
              <router-link to="/admin/danh-muc">Danh mục</router-link>
              <router-link to="/admin/loai">Loại áo</router-link>
              <router-link to="/admin/chat-lieu">Chất liệu</router-link>
              <router-link to="/admin/kich-thuoc">Kích thước</router-link>
              <router-link to="/admin/mau-sac">Màu sắc</router-link>
              <router-link to="/admin/ton-kho">Tồn kho</router-link>
            </div>
          </transition>
        </div>

        <!-- ================= PROMO DROPDOWN ================= -->
        <div class="menu-group">
          <div class="menu-title" @click="togglePromoMenu">
            <BadgePercent class="icon" />
            <span v-if="expanded">Khuyến mãi</span>
            <ChevronDown 
              v-if="expanded"
              class="chevron"
              :class="{ rotate: showPromoMenu }"
            />
          </div>

          <transition name="dropdown">
            <div v-show="expanded && showPromoMenu" class="submenu">
              <router-link to="/admin/phieu-giam-gia">Phiếu giảm giá</router-link>
              <router-link to="/admin/khuyen-mai">Đợt giảm giá</router-link>
            </div>
          </transition>
        </div>

        <!-- PTTT -->
        <router-link to="/admin/pttt" class="menu-item">
          <CreditCard class="icon" />
          <span v-if="expanded">PTTT</span>
        </router-link>

        <!-- ================= ACCOUNT DROPDOWN ================= -->
        <div class="menu-group">
          <div class="menu-title" @click="toggleAccountMenu">
            <UserCircle class="icon" />
            <span v-if="expanded">Tài khoản</span>
            <ChevronDown 
              v-if="expanded"
              class="chevron"
              :class="{ rotate: showAccountMenu }"
            />
          </div>

          <transition name="dropdown">
            <div v-show="expanded && showAccountMenu" class="submenu">
              <router-link to="/admin/khach-hang">Khách hàng</router-link>
              <router-link to="/admin/nhan-vien">Nhân viên</router-link>
              <router-link to="/admin/tai-khoan">Tài khoản hệ thống</router-link>
            </div>
          </transition>
        </div>

      </nav>
    </div>

    <!-- MAIN -->
    <div class="main">

      <div class="admin-header">
        <div class="left">
          <!-- NEW ICON -->
          <PanelLeft class="menu-icon" @click="toggleSidebar" />
        </div>

        <div class="right">
          <div class="notify">
            <Bell />
          </div>

          <div class="profile" @click="toggleDropdown">
            <img src="https://i.pravatar.cc/40" />
            <transition name="fade">
              <div v-if="showDropdown" class="dropdown">
                <div class="dropdown-item">Tài khoản của tôi</div>
                <div class="dropdown-item logout" @click.stop="logout">
                  Đăng xuất
                </div>
              </div>
            </transition>
          </div>
        </div>
      </div>

      <div class="content">
        <div class="content-card">
          <router-view v-slot="{ Component }">
            <transition name="page" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue"
import { useRouter } from "vue-router"
import logo from "@/assets/dirtywave.png"

import {
  Shirt,
  BadgePercent,
  FileText,
  CreditCard,
  UserCircle,
  Bell,
  BarChart3,
  ShoppingCart,
  RefreshCcw,
  ChevronDown,
  PanelLeft
} from "lucide-vue-next"

const router = useRouter()

const expanded = ref(false)
const showDropdown = ref(false)
const showProductMenu = ref(false)
const showPromoMenu = ref(false)
const showAccountMenu = ref(false)

const toggleSidebar = () => {
  expanded.value = !expanded.value
}

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
}

const toggleProductMenu = () => {
  showProductMenu.value = !showProductMenu.value
}

const togglePromoMenu = () => {
  showPromoMenu.value = !showPromoMenu.value
}

const toggleAccountMenu = () => {
  showAccountMenu.value = !showAccountMenu.value
}

const handleClickOutside = (event) => {
  const dropdown = document.querySelector(".profile")
  if (dropdown && !dropdown.contains(event.target)) {
    showDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener("click", handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener("click", handleClickOutside)
})

const logout = () => {
  localStorage.removeItem("token")
  showDropdown.value = false
  router.replace("/")
}
</script>

<style scoped>

/* ===== Layout ===== */

.admin-layout {
  display: flex;
  height: 100vh;
  background:
    radial-gradient(900px 700px at 20% -10%, rgba(94,234,212,.18), transparent 60%),
    radial-gradient(900px 700px at 90% 10%, rgba(255,204,0,.15), transparent 60%),
    #0b0f16;
}

/* ===== Sidebar ===== */

.sidebar {
  width: 80px;
  background: linear-gradient(180deg,#0f172a,#0b1220);
  color: #cbd5e1;
  transition: width .35s cubic-bezier(.4,0,.2,1);
  padding: 18px 12px;
  border-right: 1px solid rgba(255,255,255,.06);
  display: flex;
  flex-direction: column;
}

.sidebar.expanded {
  width: 260px;
}

.sidebar-scroll {
  overflow-y: auto;
  flex: 1;
  padding-right: 4px;
}

.sidebar-scroll::-webkit-scrollbar {
  width: 4px;
}

.sidebar-scroll::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,.15);
  border-radius: 4px;
}

/* ===== Logo ===== */

.admin-logo {
  height: 90px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-img {
  width: 95px;
  transition: .3s;
  filter: brightness(0) invert(1);
}

.sidebar.expanded .logo-img {
  width: 170px;
}

/* ===== Remove default link styles ===== */

a {
  text-decoration: none;
  color: inherit;
}

/* ===== Main Menu Items ===== */

.menu-item,
.menu-title {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-radius: 14px;
  margin-bottom: 8px;
  color: #94a3b8;
  cursor: pointer;
  transition: all .25s ease;
  position: relative;
}

.menu-item:hover,
.menu-title:hover {
  background: rgba(255,255,255,.06);
  color: white;
  transform: translateX(4px);
}

/* ===== ACTIVE MAIN MENU ===== */

.menu-item.router-link-active {
  background: linear-gradient(135deg,#ffcc00,#5eead4);
  color: #0b0f16 !important;
  font-weight: 600;
  box-shadow: 0 6px 18px rgba(255,204,0,.25);
}

.menu-item.router-link-active .icon {
  color: #0b0f16;
}

/* ===== Submenu ===== */

.submenu {
  padding-left: 50px;
  display: flex;
  flex-direction: column;
  margin-bottom: 6px;
}

.submenu a {
  padding: 8px 0;
  font-size: 14px;
  color: #8fa3bf;
  transition: all .2s ease;
  position: relative;
}

.submenu a:hover {
  color: white;
}

/* ===== ACTIVE SUBMENU ===== */

.submenu a.router-link-active {
  color: #ffffff;
  font-weight: 600;
}

.submenu a.router-link-active::before {
  content: "";
  position: absolute;
  left: -18px;
  top: 6px;
  bottom: 6px;
  width: 3px;
  border-radius: 3px;
  background: linear-gradient(180deg,#ffcc00,#5eead4);
  box-shadow: 0 0 8px rgba(94,234,212,.6);
}

/* ===== Dropdown Animation ===== */

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all .25s ease;
  overflow: hidden;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ===== Chevron ===== */

.chevron {
  margin-left: auto;
  transition: transform .3s ease;
}

.rotate {
  transform: rotate(180deg);
}

/* ===== Header ===== */

.admin-header {
  height: 70px;
  background: rgba(20,24,33,.95);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  border-bottom: 1px solid rgba(255,255,255,.06);
}

.menu-icon {
  cursor: pointer;
  color: white;
  transition: .2s;
}

.menu-icon:hover {
  transform: scale(1.1);
}

.right {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* ===== Profile ===== */

.profile {
  position: relative;
  cursor: pointer;
}

.profile img {
  width: 38px;
  height: 38px;
  border-radius: 50%;
}

/* ===== Profile Dropdown ===== */

.dropdown {
  position: absolute;
  right: 0;
  top: 55px;
  background: #1e293b;
  width: 200px;
  border-radius: 14px;
  box-shadow: 0 20px 40px rgba(0,0,0,.4);
  overflow: hidden;
}

.dropdown-item {
  padding: 14px 18px;
  color: white;
  transition: background .2s;
}

.dropdown-item:hover {
  background: rgba(255,255,255,.06);
}

.logout {
  color: #ef4444;
}

/* ===== Content ===== */

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  padding: 40px;
  overflow-y: auto;
}

.content-card {
  background: rgba(20,24,33,.95);
  border-radius: 26px;
  padding: 40px;
  min-height: 100%;
  box-shadow: 0 25px 80px rgba(0,0,0,.5);
  color: #e5e7eb;
}

/* ===== Page Animation ===== */

.page-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.page-enter-active {
  transition: .25s ease;
}

.page-leave-active {
  transition: .2s ease;
  opacity: 0;
  transform: translateY(-6px);
}

/* ===== Fade ===== */

.fade-enter-active,
.fade-leave-active {
  transition: opacity .2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.notify svg {
  color: white;
}
</style>