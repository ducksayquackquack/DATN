<script setup>
import GlobalToast from "./components/GlobalToast.vue"
import { useToast } from "./composables/useToast"

const { showToast, success, error, warning, info } = useToast()

// Make toast available globally
window.toast = {
  show: showToast,
  success,
  error,
  warning,
  info
}

// Backward compatibility
window.showToast = showToast
</script>

<template>
  <router-view />
  <GlobalToast />
</template>