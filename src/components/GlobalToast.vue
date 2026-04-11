<template>
  <div class="toast-container">
    <transition-group name="toast">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="toast-card"
        :class="[toast.type, { 'toast-card--cart': toast.variant === 'cart-add' }]"
      >
        <template v-if="toast.variant === 'cart-add'">
          <div class="toast-cart-head">
            <div class="toast-cart-success">✓</div>
            <strong>{{ toast.message || 'Thêm vào giỏ hàng thành công' }}</strong>
            <button type="button" class="toast-cart-close" @click="dismissToast(toast.id)">×</button>
          </div>

          <div class="toast-cart-body">
            <img
              :src="toast.payload?.image || fallbackImage"
              alt="Sản phẩm"
              class="toast-cart-image"
            />
            <div class="toast-cart-content">
              <h4 class="toast-cart-name">{{ toast.payload?.name || 'Sản phẩm' }}</h4>
              <p class="toast-cart-variant">
                {{ formatVariant(toast.payload) }}
              </p>
              <p class="toast-cart-price">{{ formatVnd(toast.payload?.price) }}</p>
            </div>
          </div>

          <button
            type="button"
            class="toast-cart-action"
            @click="openCart(toast.id)"
          >
            {{ toast.payload?.actionLabel || 'Xem giỏ hàng' }}
          </button>
        </template>

        <template v-else>
          <div class="toast-icon">
            <span v-if="toast.type === 'success'">✓</span>
            <span v-else-if="toast.type === 'error'">!</span>
            <span v-else-if="toast.type === 'warning'">!</span>
            <span v-else>i</span>
          </div>
          <div class="toast-body">
            <h4 class="toast-title">{{ toastTitle(toast.type) }}</h4>
            <p class="toast-message">{{ toast.message }}</p>
          </div>
          <div class="toast-progress" :style="toastProgressStyle(toast)" />
        </template>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useToast } from '../composables/useToast'

const router = useRouter()
const { toasts, dismissToast } = useToast()
const fallbackImage = 'https://via.placeholder.com/72x72?text=SP'

const toastTitle = (type) => {
  if (type === 'success') return 'Thành công'
  if (type === 'error') return 'Lỗi'
  if (type === 'warning') return 'Cảnh báo'
  return 'Thông báo'
}

const toastProgressStyle = (toast) => {
  const duration = Number(toast?.duration || 5000)
  return {
    animationDuration: `${Math.max(duration, 200)}ms`
  }
}

const formatVariant = (payload = {}) => {
  const color = String(payload?.color || '').trim()
  const size = String(payload?.size || '').trim()
  if (color && size) return `${color} / ${size}`
  if (color) return color
  if (size) return `Size ${size}`
  return 'Phiên bản mặc định'
}

const formatVnd = (value) => {
  const amount = Number(value || 0)
  if (!Number.isFinite(amount) || amount <= 0) return ''
  return `${new Intl.NumberFormat('vi-VN').format(amount)}₫`
}

const openCart = (toastId) => {
  dismissToast(toastId)
  router.push('/gio-hang')
}
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 90px;
  right: 20px;
  z-index: 12000;
  display: flex;
  flex-direction: column;
  gap: 12px;
  pointer-events: none;
}

:global(body.ops-theme .toast-container) {
  top: var(--ops-toast-top, 92px);
  right: 24px;
  transition: top 0.24s cubic-bezier(0.22, 1, 0.36, 1);
}

.toast-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 16px 16px;
  border-radius: 10px;
  background: #ffffff;
  border-left: 4px solid #10b981;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.18);
  min-width: 300px;
  max-width: 420px;
  pointer-events: auto;
  overflow: hidden;
}

.toast-card--cart {
  display: block;
  padding: 12px;
  border-left: 0;
  border: 1px solid rgba(239, 68, 68, 0.35);
  background:
    radial-gradient(circle at 16% 12%, rgba(239, 68, 68, 0.2), transparent 42%),
    linear-gradient(145deg, #fff5f5 0%, #fff0f1 100%);
}

.toast-cart-head {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #991b1b;
  margin-bottom: 10px;
}

.toast-cart-success {
  width: 18px;
  height: 18px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: #dc2626;
}

.toast-cart-close {
  margin-left: auto;
  border: 0;
  background: transparent;
  color: #dc2626;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.toast-cart-body {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  align-items: center;
}

.toast-cart-image {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.toast-cart-content {
  min-width: 0;
  flex: 1;
}

.toast-cart-name {
  margin: 0;
  font-size: 15px;
  line-height: 1.3;
  color: #111827;
}

.toast-cart-variant {
  margin: 3px 0 0;
  font-size: 13px;
  color: #4b5563;
}

.toast-cart-price {
  margin: 6px 0 0;
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
}

.toast-cart-action {
  width: 100%;
  border: 0;
  border-radius: 12px;
  height: 40px;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ef4444 0%, #b91c1c 100%);
  cursor: pointer;
}

.toast-icon {
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  margin-top: 2px;
}

.toast-body {
  flex: 1;
  min-width: 0;
}

.toast-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.1;
  color: #111827;
}

.toast-message {
  margin: 4px 0 0;
  font-size: 14px;
  line-height: 1.35;
  color: #4b5563;
  word-break: break-word;
}

.toast-progress {
  position: absolute;
  left: 0;
  bottom: 0;
  height: 3px;
  width: 100%;
  background: #dc2626;
  transform-origin: left;
  animation-name: toast-progress;
  animation-timing-function: linear;
  animation-fill-mode: forwards;
}

.toast-card.success {
  border-left-color: #dc2626;
}

.toast-card.success .toast-icon {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.14);
}

.toast-card.success .toast-progress {
  background: #dc2626;
}

.toast-card.error {
  border-left-color: #ef4444;
}

.toast-card.error .toast-icon {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.14);
}

.toast-card.error .toast-progress {
  background: #ef4444;
}

.toast-card.warning {
  border-left-color: #f59e0b;
}

.toast-card.warning .toast-icon {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.14);
}

.toast-card.warning .toast-progress {
  background: #f59e0b;
}

.toast-card.info {
  border-left-color: #3b82f6;
}

.toast-card.info .toast-icon {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.14);
}

.toast-card.info .toast-progress {
  background: #3b82f6;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.35s ease;
}

.toast-enter-from {
  transform: translateX(120%);
  opacity: 0;
}

.toast-enter-to {
  transform: translateX(0);
  opacity: 1;
}

.toast-leave-to {
  transform: translateX(120%);
  opacity: 0;
}

.toast-move {
  transition: transform 0.35s ease;
}

@media (max-width: 768px) {
  .toast-container,
  :global(body.ops-theme .toast-container) {
    top: var(--ops-toast-top, 74px);
    right: 12px;
    left: 12px;
  }

  .toast-card {
    min-width: 0;
    max-width: none;
  }
}

@keyframes toast-progress {
  from { transform: scaleX(1); }
  to { transform: scaleX(0); }
}
</style>
