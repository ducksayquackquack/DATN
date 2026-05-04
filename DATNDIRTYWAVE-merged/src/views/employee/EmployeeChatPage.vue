<script setup>
import { computed, watchEffect } from "vue"
import { useRoute } from "vue-router"
import EmployeeChatPanel from "../../components/employee/EmployeeChatPanel.vue"
import { useInternalAssistant } from "../../composables/useInternalAssistant"

const route = useRoute()
const { context, role, open } = useInternalAssistant()

const currentRole = computed(() => {
  const savedRole = String(localStorage.getItem("role") || "").toUpperCase()
  return savedRole.includes("ADMIN") ? "ADMIN" : "EMPLOYEE"
})

watchEffect(() => {
  role.value = currentRole.value

  context.value = {
    pageType: "CUSTOMER_CHAT",
    route: route.path,
    title: "Chat khách hàng",
    helperPrompts: [
      "Viết giúp tôi câu tư vấn mềm hơn",
      "Gợi ý câu trả lời chốt sale",
      "Tóm tắt ý chính để trả lời khách",
      "Gợi ý upsell cho khách"
    ]
  }

  // Tự mở assistant khi vào trang chat nếu muốn:
  // open.value = true
})
</script>

<template>
  <EmployeeChatPanel />
</template>