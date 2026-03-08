<script setup>
import { useToast } from '../composables/useToast'

const { toasts } = useToast()
</script>

<template>
  <div class="toast-container">
    <transition-group name="toast">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="toast"
        :class="toast.type"
      >
        <div class="toast-icon">
          <span v-if="toast.type === 'success'">✓</span>
          <span v-else-if="toast.type === 'error'">✕</span>
          <span v-else-if="toast.type === 'warning'">⚠</span>
          <span v-else>ℹ</span>
        </div>
        <div class="toast-message">{{ toast.message }}</div>
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-radius: 10px;
  color: white;
  font-weight: 500;
  font-size: 14px;
  min-width: 300px;
  max-width: 500px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  pointer-events: auto;
  backdrop-filter: blur(10px);
}

.toast-icon {
  font-size: 18px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
}

.toast-message {
  flex: 1;
  line-height: 1.4;
}

.success {
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
}

.error {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.warning {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.info {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
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
  transition: transform 0.3s ease;
}
</style>
