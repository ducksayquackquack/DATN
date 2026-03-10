t<script setup>
import { useRouter } from "vue-router"
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
  Bell,
  Search
} from "lucide-vue-next"

import logo from '../../../assets/img/DirtyWaveLogo.png'

const router = useRouter()

function logout() {
  localStorage.removeItem("role")
  router.push("/login")
}
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-logo">
          <img :src="logo" alt="DirtyWave" />
        </div>
        <div class="brand-text">
          <div class="brand-name">DirtyWave</div>
          <div class="brand-sub">Shop Jacket</div>
        </div>
      </div>

      <div class="nav">
        <div class="navgrp">
          <h4>Vận hành</h4>

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

          <RouterLink to="/admin/khuyen-mai/list">
            <span class="left">
              <Tag class="icon" /> Khuyến mãi
            </span>
          </RouterLink>
        </div>

        <div class="navgrp">
          <h4>Danh mục</h4>

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

        <div class="navgrp">
          <h4>Tài khoản</h4>

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
      </div>
    </aside>

    <div class="content">
      <div class="topbar">
        <div class="row">
          <div class="search">
            <Search size="18" />
            <input placeholder="Tìm mã hoá đơn, tên sản phẩm, khách hàng..." />
            <!-- <kbd></kbd> -->
          </div>

          <div class="userbox">
            <button class="btn">
              <Bell size="18" />
            </button>

            <div class="avatar-wrapper">
              <div class="avatar">A</div>
              <div class="dropdown">
                <div class="dropdown-item logout" @click="logout">
                  Đăng xuất
                </div>
              </div>
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

.icon {
  width: 18px;
  height: 18px;
  margin-right: 10px;
  transition: all 0.25s ease;
}

.left {
  display: flex;
  align-items: center;
}

.nav a {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 10px;
  transition: all 0.25s ease;
}

.nav a:hover {
  background: rgba(255,255,255,0.06);
  transform: translateX(4px);
}

.nav a:hover .icon {
  transform: scale(1.15);
}

.nav a.router-link-active {
  background: rgba(255,255,255,0.1);
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
}

.avatar-wrapper:hover .dropdown {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.dropdown {
  position: absolute;
  top: 48px;
  right: 0;
  background: #ffffff;
  border-radius: 12px;
  padding: 8px;
  width: 180px;
  box-shadow: 0 10px 25px rgba(0,0,0,.25);
  z-index: 1000;
  opacity: 0;
  visibility: hidden;
  transform: translateY(6px);
  transition: all .18s ease;
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
  color: #ff6b6b;
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
</style>