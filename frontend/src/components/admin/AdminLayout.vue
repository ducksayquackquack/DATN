<template>
  <div class="admin-layout">
    <div
      class="sidebar"
      :class="{ expanded }"
      @mouseenter="expanded = true"
      @mouseleave="expanded = false"
    >
      <div class="admin-logo">
        <img :src="logo" class="logo-img" :class="{ hidden: expanded }" />
        <span class="logo-text" :class="{ show: expanded }">
          DirtyWave Admin
        </span>
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
.admin-layout {
  display: flex;
  height: 100vh;
  background:
    radial-gradient(900px 700px at 20% -10%, rgba(94,234,212,.18), transparent 60%),
    radial-gradient(900px 700px at 90% 10%, rgba(255,204,0,.15), transparent 60%),
    #0b0f16;
}

/* SIDEBAR */
.sidebar {
  width: 80px;
  background: rgba(15,18,25,.95);
  color: #cbd5e1;
  transition: width .3s ease;
  padding: 24px 12px;
  overflow: hidden;
  border-right: 1px solid rgba(255,255,255,.06);
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

/* CONTENT */
.content {
  flex: 1;
  padding: 40px;
  overflow-y: auto;
}

/* GLASS CARD */
.content-card {
  background: rgba(20,24,33,.95);
  border-radius: 26px;
  padding: 40px;
  min-height: 100%;
  box-shadow: 0 25px 80px rgba(0,0,0,.5);
  color: #e5e7eb;
}

/* 🔥 ONLY FIX PAGE TITLES */
.content-card > h1 {
  color: #ffffff !important;
  font-weight: 600;
}

/* Small text */
.content-card small {
  color: rgba(255,255,255,.6);
}

/* INPUTS */
.content-card input,
.content-card select {
  background: rgba(255,255,255,.08);
  border: 1px solid rgba(255,255,255,.15);
  color: white;
}

.content-card input::placeholder {
  color: rgba(255,255,255,.5);
}

/* TABLE FIX */
.content-card table {
  background: transparent;
  color: #e5e7eb;
}

.content-card thead {
  background: rgba(255,255,255,.08);
}

.content-card th {
  color: #ffffff;
  font-weight: 600;
}

.content-card tbody tr {
  background: rgba(255,255,255,.04);
  transition: background .2s ease;
}

.content-card tbody tr:hover {
  background: rgba(255,255,255,.08);
}

.content-card td {
  border-top: 1px solid rgba(255,255,255,.08);
}

.content-card .btn,
.content-card button {
  color: white;
}

/* TRANSITION */
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

/* LOGO */
.admin-logo {
  position: relative;
  height: 90px;
  margin-bottom: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-img {
  width: 52px;
  transition: opacity .25s ease, transform .25s ease;
  filter: brightness(0) invert(1);
}

.logo-img.hidden {
  opacity: 0;
  transform: scale(.9);
}

.logo-text {
  position: absolute;
  white-space: nowrap;
  opacity: 0;
  transition: opacity .25s ease, transform .25s ease;
  transform: translateY(4px);
  font-size: 20px;
  font-weight: 600;
  letter-spacing: .5px;
  color: white;
}

.logo-text.show {
  opacity: 1;
  transform: translateY(0);
}

</style>