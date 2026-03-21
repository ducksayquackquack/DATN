<template>
  <div class="toast-container">
    <transition-group name="toast">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="toast-card"
        :class="toast.type"
      >
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
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { useToast } from '../composables/useToast'

const { toasts } = useToast()

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
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 90px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 12px;
  pointer-events: none;
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
  background: #10b981;
  transform-origin: left;
  animation-name: toast-progress;
  animation-timing-function: linear;
  animation-fill-mode: forwards;
}

.toast-card.success {
  border-left-color: #10b981;
}

.toast-card.success .toast-icon {
  color: #10b981;
  background: rgba(16, 185, 129, 0.14);
}

.toast-card.success .toast-progress {
  background: #10b981;
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

@keyframes toast-progress {
  from { transform: scaleX(1); }
  to { transform: scaleX(0); }
}
</style>
