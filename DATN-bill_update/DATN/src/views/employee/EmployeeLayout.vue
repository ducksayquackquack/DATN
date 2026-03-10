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
            <!-- Notification Bell -->
            <div class="notification-wrapper">
              <button class="btn notification-btn" @click="toggleNotifications">
                🔔
                <span v-if="unreadCount > 0" class="notification-badge">{{ unreadCount }}</span>
              </button>
              
              <!-- Notification Dropdown -->
              <div v-show="showNotifications" class="notification-dropdown">
                <div class="notification-header">
                  <h4>Thông báo</h4>
                  <button v-if="notifications.length > 0" class="clear-btn" @click="clearAllNotifications">Xóa tất cả</button>
                </div>
                <div class="notification-list">
                  <div v-if="notifications.length === 0" class="no-notifications">
                    Không có thông báo nào
                  </div>
                  <div 
                    v-for="notif in notifications" 
                    :key="notif.id" 
                    class="notification-item"
                    :class="{ unread: !notif.daDoc }"
                    @click="viewNotification(notif)"
                  >
                    <div class="notif-icon">
                      {{ getNotifIcon(notif.type) }}
                    </div>
                    <div class="notif-content">
                      <p class="notif-message">{{ notif.message }}</p>
                      <small class="notif-time">{{ formatTime(notif.ngayTao) }}</small>
                    </div>
                  </div>
                </div>
              </div>
            </div>

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
import { ref, computed, onMounted, onUnmounted } from "vue"
import { useRouter } from "vue-router"
import logo from '../../assets/img/DirtyWaveLogo.png'
import "./dashboard.css"

const router = useRouter()
const openUser = ref(false)

// Notification state
const notifications = ref([])
const showNotifications = ref(false)
let notificationInterval = null

// Load notifications from localStorage
const loadNotifications = () => {
  const stored = JSON.parse(localStorage.getItem("employeeNotifications")) || []
  notifications.value = stored.sort((a, b) => new Date(b.ngayTao) - new Date(a.ngayTao))
}

// Computed: unread count
const unreadCount = computed(() => {
  return notifications.value.filter(n => !n.daDoc).length
})

// Toggle notification dropdown
const toggleNotifications = () => {
  showNotifications.value = !showNotifications.value
  if (showNotifications.value) {
    loadNotifications()
  }
}

// Get notification icon based on type
const getNotifIcon = (type) => {
  switch (type) {
    case 'NEW_ORDER': return '🆕'
    case 'PAYMENT_CONFIRMED': return '💳'
    case 'ORDER_CONFIRMED': return '✅'
    case 'ORDER_CANCELLED': return '❌'
    default: return '📢'
  }
}

// Format time
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return 'Vừa xong'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} phút trước`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} giờ trước`
  return date.toLocaleDateString('vi-VN')
}

// View notification - mark as read and navigate
const viewNotification = (notif) => {
  // Mark as read
  notif.daDoc = true
  localStorage.setItem("employeeNotifications", JSON.stringify(notifications.value))
  
  // Navigate to order detail if exists
  if (notif.orderId) {
    router.push(`/employee/hoa-don/detail/${notif.orderId}`)
  }
  
  showNotifications.value = false
}

// Clear all notifications
const clearAllNotifications = () => {
  notifications.value = []
  localStorage.removeItem("employeeNotifications")
}

// Auto-refresh notifications every 5 seconds
onMounted(() => {
  loadNotifications()
  notificationInterval = setInterval(loadNotifications, 5000)
})

onUnmounted(() => {
  if (notificationInterval) {
    clearInterval(notificationInterval)
  }
})

function logout() {
  localStorage.removeItem("role")
  router.push("/login")
}

function goHome() {
  router.push("/home")
}
</script>

<style scoped>
.notification-wrapper {
  position: relative;
}

.notification-btn {
  position: relative;
  font-size: 18px;
}

.notification-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ff3b30;
  color: white;
  font-size: 10px;
  border-radius: 50%;
  padding: 2px 5px;
  min-width: 16px;
  text-align: center;
}

.notification-dropdown {
  position: absolute;
  top: 48px;
  right: 0;
  background: #ffffff;
  border-radius: 12px;
  padding: 0;
  width: 320px;
  max-height: 400px;
  box-shadow: 0 10px 25px rgba(0,0,0,.25);
  z-index: 100;
  overflow: hidden;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
}

.notification-header h4 {
  margin: 0;
  font-size: 14px;
  color: #333;
}

.clear-btn {
  background: none;
  border: none;
  color: #2563eb;
  font-size: 12px;
  cursor: pointer;
}

.notification-list {
  max-height: 340px;
  overflow-y: auto;
}

.no-notifications {
  padding: 20px;
  text-align: center;
  color: #777;
}

.notification-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.notification-item:hover {
  background: #f8f9fa;
}

.notification-item.unread {
  background: #f0f5ff;
}

.notification-item.unread:hover {
  background: #e0ebff;
}

.notif-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.notif-content {
  flex: 1;
  min-width: 0;
}

.notif-message {
  margin: 0;
  font-size: 13px;
  color: #333;
  line-height: 1.4;
}

.notif-time {
  color: #777;
  font-size: 11px;
}

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
