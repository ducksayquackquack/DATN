<script setup>
import { computed, onBeforeUnmount, onMounted, watchEffect } from "vue"
import { useRoute, useRouter } from "vue-router"
import GlobalToast from "./components/GlobalToast.vue"
import GlobalConfirmDialog from "./components/GlobalConfirmDialog.vue"
import DirtyWaveChatbot from "./components/chat/DirtyWaveChatbot.vue"
import InternalAssistantDock from "./components/assistant/InternalAssistantDock.vue"
import { useToast } from "./composables/useToast"
import { useConfirm } from "./composables/useConfirm"
import { useInternalAssistant } from "./composables/useInternalAssistant"

const route = useRoute()
const router = useRouter()
const OPS_THEME_CLASS = "ops-theme"

const { showToast, success, error, warning, info } = useToast()
const { askAlert, askConfirm, askPrompt } = useConfirm()
const {
  context: sharedAssistantContext,
  role: sharedAssistantRole
} = useInternalAssistant()

const nativeAlert = window.alert
const nativeConfirm = window.confirm
const nativePrompt = window.prompt

window.browserNativeAlert = nativeAlert
window.browserNativeConfirm = nativeConfirm
window.browserNativePrompt = nativePrompt

window.toast = {
  show: showToast,
  success,
  error,
  warning,
  info
}

window.showToast = showToast
window.confirmDialog = askConfirm
window.alertDialog = askAlert
window.promptDialog = askPrompt

window.alert = (message) => {
  askAlert(message)
}

window.confirm = (message) => {
  return askConfirm(message)
}

window.prompt = (message, defaultValue = "") => {
  return askPrompt(message, defaultValue)
}

window.nativeAlert = window.alert
window.nativeConfirm = window.confirm
window.nativePrompt = window.prompt

watchEffect(() => {
  const path = route.path || ""
  const useOpsTheme = path.startsWith("/admin") || path.startsWith("/employee")
  document.body.classList.toggle(OPS_THEME_CLASS, useOpsTheme)
})

const isInternalArea = computed(() => {
  const path = route.path || ""
  return path.startsWith("/admin") || path.startsWith("/employee")
})

const showCustomerChatbot = computed(() => {
  const path = route.path || ""

  if (path.startsWith("/admin")) return false
  if (path.startsWith("/employee")) return false
  if (path === "/internal-assistant-demo") return false

  return true
})

const internalAssistantRole = computed(() => {
  if (sharedAssistantRole?.value) {
    return String(sharedAssistantRole.value).toUpperCase().includes("ADMIN")
      ? "ADMIN"
      : "EMPLOYEE"
  }

  const path = route.path || ""
  if (path.startsWith("/admin")) return "ADMIN"
  return "EMPLOYEE"
})

const internalAssistantSource = computed(() => {
  return internalAssistantRole.value === "ADMIN" ? "ADMIN_PANEL" : "EMPLOYEE_PANEL"
})

const handleAssistantAuthRequired = () => {
  const currentPath = route.fullPath || route.path || ""
  if (currentPath.startsWith("/auth/staff-login")) return

  warning("Phiên đăng nhập nội bộ đã hết hạn. Vui lòng đăng nhập lại.")
  router.push("/auth/staff-login")
}

onMounted(() => {
  window.addEventListener("assistant:auth-required", handleAssistantAuthRequired)
})

const internalAssistantContext = computed(() => {
  const path = route.path || ""
  const shared = sharedAssistantContext?.value || {}

  const hasSharedChatContext =
    !!shared?.pageType ||
    !!shared?.sessionId ||
    !!shared?.lastCustomerMessage ||
    !!shared?.currentReplyDraft

  if (hasSharedChatContext) {
    return {
      route: path,
      ...shared,
      route: shared.route || path,
      pageType:
        shared.pageType ||
        (shared.sessionId || shared.lastCustomerMessage || shared.currentReplyDraft
          ? "CUSTOMER_CHAT"
          : "GENERAL_INTERNAL")
    }
  }

  if (path.startsWith("/employee/chat")) {
    return {
      pageType: "CUSTOMER_CHAT",
      route: path
    }
  }

  if (path.startsWith("/employee/dashboard") || path.startsWith("/admin/thong-ke")) {
    return {
      pageType: "REVENUE_DASHBOARD",
      route: path
    }
  }

  if (path.includes("/ban-hang")) {
    return {
      pageType: "POS",
      route: path
    }
  }

  if (path.includes("/san-pham")) {
  return {
    pageType: "PRODUCT_LIST",
    route: path
  }
}

  if (path.includes("/hoa-don/detail")) {
    return {
      pageType: "ORDER_DETAIL",
      route: path
    }
  }

  return {
    pageType: "GENERAL_INTERNAL",
    route: path
  }
})

onBeforeUnmount(() => {
  document.body.classList.remove(OPS_THEME_CLASS)
  window.removeEventListener("assistant:auth-required", handleAssistantAuthRequired)
})
</script>

<template>
  <router-view v-slot="{ Component, route: currentRoute }">
    <div class="app-route-stage">
      <transition name="app-route">
        <component
          :is="Component"
          :key="currentRoute.matched?.[0]?.path || currentRoute.path"
          class="app-route-view"
        />
      </transition>
    </div>
  </router-view>

  <GlobalToast />
  <GlobalConfirmDialog />

  <DirtyWaveChatbot v-if="showCustomerChatbot" />

  <InternalAssistantDock
    v-if="isInternalArea"
    :role="internalAssistantRole"
    :source="internalAssistantSource"
    :context="internalAssistantContext"
  />
</template>

<style>
.app-route-stage {
  position: relative;
  min-height: 100vh;
  isolation: isolate;
}

.app-route-enter-active,
.app-route-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.app-route-leave-active {
  position: absolute;
  inset: 0;
  width: 100%;
  pointer-events: none;
}

.app-route-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.app-route-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (prefers-reduced-motion: reduce) {
  .app-route-enter-active,
  .app-route-leave-active {
    transition: none;
  }
}
</style>