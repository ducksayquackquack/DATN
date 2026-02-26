<template>
  <div class="admin-layout">

    <!-- SIDEBAR -->
    <div class="sidebar" :class="{ expanded }">

      <div class="admin-logo">
        <!-- ONLY LOGO NOW -->
        <img :src="logo" class="logo-img" />
      </div>

      <nav>
        <router-link to="/admin/dashboard">
          <BarChart3 class="icon" />
          <span v-if="expanded">Thống Kê</span>
        </router-link>

        <router-link to="/admin/san-pham">
          <Shirt class="icon" />
          <span v-if="expanded">Sản Phẩm</span>
        </router-link>

        <router-link to="/admin/danh-muc">
          <LayoutList class="icon" />
          <span v-if="expanded">Danh Mục</span>
        </router-link>

        <router-link to="/admin/loai">
          <Tag class="icon" />
          <span v-if="expanded">Loại</span>
        </router-link>

        <router-link to="/admin/kich-thuoc">
          <Ruler class="icon" />
          <span v-if="expanded">Kích Thước</span>
        </router-link>

        <router-link to="/admin/mau-sac">
          <Palette class="icon" />
          <span v-if="expanded">Màu Sắc</span>
        </router-link>

        <router-link to="/admin/khuyen-mai">
          <BadgePercent class="icon" />
          <span v-if="expanded">Khuyến Mãi</span>
        </router-link>

        <hr />

        <router-link to="/admin/hoa-don">
          <FileText class="icon" />
          <span v-if="expanded">Hóa Đơn</span>
        </router-link>

        <router-link to="/admin/pttt">
          <CreditCard class="icon" />
          <span v-if="expanded">PTTT</span>
        </router-link>

        <hr />

        <router-link to="/admin/khach-hang">
          <Users class="icon" />
          <span v-if="expanded">Khách Hàng</span>
        </router-link>

        <router-link to="/admin/nhan-vien">
          <UserCog class="icon" />
          <span v-if="expanded">Nhân Viên</span>
        </router-link>

        <router-link to="/admin/tai-khoan">
          <UserCircle class="icon" />
          <span v-if="expanded">Tài Khoản</span>
        </router-link>
      </nav>
    </div>

    <!-- MAIN -->
    <div class="main">

      <!-- HEADER -->
      <div class="admin-header">
        <div class="left">
          <Menu class="menu-icon" @click="toggleSidebar" />
        </div>

        <div class="right">

          <!-- Notification (badge removed) -->
          <div class="notify">
            <Bell />
          </div>

          <!-- Profile -->
          <div class="profile" @click="toggleDropdown">
            <img src="https://i.pravatar.cc/40" />
            <div v-if="showDropdown" class="dropdown">
              <div class="dropdown-item">
                Tài khoản của tôi
              </div>

              <div class="dropdown-item logout" @click.stop="logout">
                Đăng xuất
              </div>
            </div>
          </div>

        </div>
      </div>

      <!-- CONTENT -->
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
  LayoutList,
  Tag,
  Ruler,
  Palette,
  BadgePercent,
  FileText,
  CreditCard,
  Users,
  UserCog,
  UserCircle,
  Menu,
  Bell,
  BarChart3
} from "lucide-vue-next"

const router = useRouter()

const expanded = ref(false)
const showDropdown = ref(false)

const toggleSidebar = () => {
  expanded.value = !expanded.value
}

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
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
.admin-layout {
  display: flex;
  height: 100vh;
  background:
    radial-gradient(900px 700px at 20% -10%, rgba(94,234,212,.18), transparent 60%),
    radial-gradient(900px 700px at 90% 10%, rgba(255,204,0,.15), transparent 60%),
    #0b0f16;
}

.sidebar {
  width: 80px;
  background: rgba(15,18,25,.95);
  color: #cbd5e1;
  transition: width .3s ease;
  padding: 16px 12px;
  overflow: hidden;
  border-right: 1px solid rgba(255,255,255,.06);
  display: flex;
  flex-direction: column;
}

.sidebar.expanded {
  width: 240px;
}

.sidebar a {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-radius: 16px;
  margin-bottom: 8px;
  text-decoration: none;
  color: #94a3b8;
  transition: all .2s ease;
  font-size: 14px;
}

.sidebar:not(.expanded) a {
  justify-content: center;
  padding: 14px 0;
  width: 56px;
  margin: 0 auto 12px auto;
}

.sidebar a:hover {
  background: rgba(255,255,255,.06);
  color: white;
}

.sidebar a.router-link-active {
  background: linear-gradient(135deg, #ffcc00, #5eead4);
  color: #101114;
  font-weight: 600;
}

.admin-logo {
  height: 90px;
  margin-bottom: 25px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-img {
  width: 70px;
  transition: width .3s ease;
  filter: brightness(0) invert(1);
}

.sidebar.expanded .logo-img {
  width: 115px;
  transform: scale(1.53);
}

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
}

.right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notify {
  cursor: pointer;
  color: white;
}

.profile {
  position: relative;
  cursor: pointer;
}

.profile img {
  width: 38px;
  height: 38px;
  border-radius: 50%;
}

.dropdown {
  position: absolute;
  right: 0;
  top: 55px;
  background: #1e293b;
  width: 200px;
  border-radius: 14px;
  box-shadow: 0 20px 40px rgba(0,0,0,.4);
  overflow: hidden;
  animation: fadeIn .2s ease;
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

.page-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.page-enter-active {
  transition: all 0.25s ease;
}

.page-leave-active {
  transition: all 0.2s ease;
  opacity: 0;
  transform: translateY(-6px);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>