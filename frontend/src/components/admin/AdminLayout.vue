<template>
  <div class="admin-layout">
    <!-- SIDEBAR -->
    <div
      class="sidebar"
      :class="{ expanded }"
      @mouseenter="expanded = true"
      @mouseleave="expanded = false"
    >
      <div class="logo">
        <img v-if="!expanded" :src="logo" class="logo-img" />
        <span v-else class="logo-text">DirtyWave Admin</span>
      </div>

      <nav>
        <router-link to="/admin/san-pham">
          <component :is="Shirt" class="icon" />
          <span v-if="expanded">Sản Phẩm</span>
        </router-link>

        <router-link to="/admin/danh-muc">
          <component :is="LayoutList" class="icon" />
          <span v-if="expanded">Danh Mục</span>
        </router-link>

        <router-link to="/admin/loai">
          <component :is="Tag" class="icon" />
          <span v-if="expanded">Loại</span>
        </router-link>

        <router-link to="/admin/kich-thuoc">
          <component :is="Ruler" class="icon" />
          <span v-if="expanded">Kích Thước</span>
        </router-link>

        <router-link to="/admin/mau-sac">
          <component :is="Palette" class="icon" />
          <span v-if="expanded">Màu Sắc</span>
        </router-link>

        <router-link to="/admin/khuyen-mai">
          <component :is="BadgePercent" class="icon" />
          <span v-if="expanded">Khuyến Mãi</span>
        </router-link>

        <hr />

        <router-link to="/admin/hoa-don">
          <component :is="FileText" class="icon" />
          <span v-if="expanded">Hóa Đơn</span>
        </router-link>

        <router-link to="/admin/pttt">
          <component :is="CreditCard" class="icon" />
          <span v-if="expanded">PTTT</span>
        </router-link>

        <hr />

        <router-link to="/admin/khach-hang">
          <component :is="Users" class="icon" />
          <span v-if="expanded">Khách Hàng</span>
        </router-link>

        <router-link to="/admin/nhan-vien">
          <component :is="UserCog" class="icon" />
          <span v-if="expanded">Nhân Viên</span>
        </router-link>

        <router-link to="/admin/tai-khoan">
          <component :is="UserCircle" class="icon" />
          <span v-if="expanded">Tài Khoản</span>
        </router-link>
      </nav>
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
</template>

<script setup>
import { ref } from "vue"
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
  UserCircle
} from "lucide-vue-next"

const expanded = ref(false)
</script>

<style scoped>
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

.admin-layout {
  display: flex;
  height: 100vh;
  background: linear-gradient(135deg, #eef2ff, #f8fafc);
}

/* ===== SIDEBAR ===== */
.sidebar {
  width: 80px;
  background: linear-gradient(180deg, #0f172a, #111827);
  color: #cbd5e1;
  transition: width .3s ease;
  padding: 24px 12px;
  overflow: hidden;
  box-shadow: 6px 0 30px rgba(0,0,0,.15);
}

.sidebar.expanded {
  width: 240px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  margin-bottom: 30px;
}

.logo-img {
  width: 38px;
  height: auto;
  filter: brightness(0) invert(1);
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: white;
}

/* NAV LINKS */
.sidebar a {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border-radius: 12px;
  margin-bottom: 8px;
  text-decoration: none;
  color: #94a3b8;
  transition: all .2s ease;
  font-size: 14px;
}

.sidebar:not(.expanded) a {
  justify-content: center;
}

.sidebar.expanded a {
  justify-content: flex-start;
  padding-left: 18px;
}

.sidebar a:hover {
  background: rgba(255,255,255,.06);
  color: white;
  transform: translateX(3px);
}

.sidebar a.router-link-active {
  background: linear-gradient(90deg, #6366f1, #3b82f6);
  color: white;
  box-shadow: 0 6px 18px rgba(99,102,241,.35);
}

.icon {
  width: 20px;
  height: 20px;
  min-width: 20px;
}

/* Divider */
hr {
  border: none;
  border-top: 1px solid rgba(255,255,255,.08);
  margin: 14px 0;
}

/* ===== CONTENT ===== */
.content {
  flex: 1;
  padding: 40px;
  overflow-y: auto;
}

.content-card {
  background: white;
  padding: 32px;
  border-radius: 20px;
  box-shadow: 0 15px 40px rgba(0,0,0,.06);
  min-height: 100%;
  animation: fadeIn .3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>