<script setup>
import { ref } from "vue"

const visible = ref(false)
const message = ref("")
const type = ref("success")

function showToast(msg, t = "success") {
  message.value = msg
  type.value = t
  visible.value = true

  setTimeout(() => {
    visible.value = false
  }, 2500)
}

defineExpose({ showToast })
</script>

<template>
  <transition name="toast">
    <div v-if="visible" class="toast" :class="type">
      {{ message }}
    </div>
  </transition>
</template>

<style scoped>
.toast {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 14px 22px;
  border-radius: 10px;
  color: white;
  font-weight: 600;
  z-index: 9999;
}

.success { background: #22c55e; }
.error { background: #ef4444; }

.toast-enter-active,
.toast-leave-active {
  transition: all .3s ease;
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
</style>