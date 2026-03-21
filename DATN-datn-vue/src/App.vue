<script setup>
import { onBeforeUnmount, watchEffect } from "vue"
import { useRoute } from "vue-router"
import GlobalToast from "./components/GlobalToast.vue"
import GlobalConfirmDialog from "./components/GlobalConfirmDialog.vue"
import { useToast } from "./composables/useToast"
import { useConfirm } from "./composables/useConfirm"

const route = useRoute()
const OPS_THEME_CLASS = "ops-theme"

const { showToast, success, error, warning, info } = useToast()
const { askAlert, askConfirm, askPrompt } = useConfirm()

const nativeAlert = window.alert
const nativeConfirm = window.confirm
const nativePrompt = window.prompt

// Keep real browser dialogs available for debugging/fallback.
window.browserNativeAlert = nativeAlert
window.browserNativeConfirm = nativeConfirm
window.browserNativePrompt = nativePrompt

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
window.confirmDialog = askConfirm
window.alertDialog = askAlert
window.promptDialog = askPrompt

// Replace browser dialogs with app dialogs.
window.alert = (message) => {
  askAlert(message)
}

window.confirm = (message) => {
  return askConfirm(message)
}

window.prompt = (message, defaultValue = "") => {
  return askPrompt(message, defaultValue)
}

// Compatibility aliases requested by existing code paths.
window.nativeAlert = window.alert
window.nativeConfirm = window.confirm
window.nativePrompt = window.prompt

watchEffect(() => {
  const path = route.path || ""
  const useOpsTheme = path.startsWith("/admin") || path.startsWith("/employee")
  document.body.classList.toggle(OPS_THEME_CLASS, useOpsTheme)
})

onBeforeUnmount(() => {
  document.body.classList.remove(OPS_THEME_CLASS)
})
</script>

<template>
  <router-view />
  <GlobalToast />
  <GlobalConfirmDialog />
</template>