<template>
  <div class="app">

    <aside class="sidebar">
      <div class="brand">
        <div class="brand-logo">
          <img :src="logo" alt="DirtyWave" />
        </div>
        <div class="brand-text">
          <div class="brand-name">EMPLOYEE</div>
          <div class="brand-sub">DirtyWave</div>
        </div>
      </div>

      <div class="nav">

        <div class="navgrp">
          <h4>Tổng quan</h4>

          <router-link to="/employee/dashboard" class="navlink">
            <span class="left">📊 Dashboard</span>
          </router-link>

          <router-link to="/employee/hoa-don/list" class="navlink">
            <span class="left">🧾 Hoá đơn</span>
          </router-link>

          <router-link to="/employee/san-pham/list" class="navlink">
            <span class="left">👕 Sản phẩm</span>
          </router-link>
        </div>

        <div class="navgrp">
          <h4>Nhanh</h4>

          <div class="navlink" @click="goHome">
            <span class="left">🛍️ Xem site khách</span>
          </div>

          <router-link to="/employee/khuyen-mai/list" class="navlink">
            <span class="left">🏷️ Khuyến mãi</span>
          </router-link>
        </div>

      </div>

      <div style="margin-top:14px" class="muted">
        <small>Ca làm: <b style="color:rgba(238,242,255,.9)">Sáng</b></small>
      </div>
    </aside>

    <div class="content">

      <div class="topbar">
        <div class="row">

          <div class="search">
            <span>🔎</span>
            <input placeholder="Tìm hoá đơn / khách / sản phẩm..." />
            <kbd>Ctrl K</kbd>
          </div>

          <div class="user-menu">
            <button class="btn">🔔</button>

            <div
              class="avatar-wrapper"
              @mouseenter="openUser = true"
              @mouseleave="openUser = false"
            >
              <div class="avatar">E</div>

              <div class="dropdown">
                <div class="dropdown-item logout" @click="logout">
                  🚪 Đăng xuất
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

<script setup>
import { ref } from "vue"
import { useRouter } from "vue-router"
import logo from '../../assets/img/DirtyWaveLogo.png'
import "./dashboard.css"

const router = useRouter()
const openUser = ref(false)

function logout() {
  localStorage.removeItem("role")
  router.push("/login")
}

function goHome() {
  router.push("/home")
}
</script>

<style scoped>
.brand {
  display:flex;
  align-items:center;
  gap:12px;
}

.brand-logo img {
  width:42px;
  height:42px;
  object-fit:contain;
}

.brand-text {
  display:flex;
  flex-direction:column;
}

.brand-name {
  font-weight:700;
  letter-spacing:1px;
}

.brand-sub {
  font-size:12px;
  opacity:.7;
}

.navlink {
  display:flex;
  align-items:center;
  padding:10px;
  border-radius:12px;
  cursor:pointer;
  transition: all .25s ease;
}

.navlink:hover {
  background:rgba(255,255,255,.05);
  transform: translateX(4px);
}

.router-link-active {
  background:linear-gradient(135deg, rgba(255,204,0,.14), rgba(94,234,212,.08));
}

.user-menu {
  display:flex;
  gap:10px;
  align-items:center;
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
  opacity: 0;
  visibility: hidden;
  transform: translateY(6px);
  transition: all .18s ease;
}

.dropdown-item {
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: .2s;
}

.dropdown-item:hover {
  background: #f2f2f2;
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