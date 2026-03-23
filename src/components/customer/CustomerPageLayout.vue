<script setup>
import { computed } from "vue"
import SiteNav from "../SiteNav.vue"
import CustomerFooter from "./CustomerFooter.vue"

const props = defineProps({
  breadcrumb: { type: String, required: true },
  maxWidth: { type: String, default: "1100px" },
})

const containerStyle = computed(() => ({
  "--cp-max-width": props.maxWidth,
}))
</script>

<template>
  <div class="customer-page" :style="containerStyle">
    <SiteNav />

    <div class="cp-breadcrumb">
      <div class="cp-container">
        <RouterLink class="cp-home-link" to="/trang-chu">Trang chủ</RouterLink>
        <span>/</span>
        <strong>{{ breadcrumb }}</strong>
      </div>
    </div>

    <main class="cp-container cp-main">
      <slot />
    </main>

    <CustomerFooter />
  </div>
</template>

<style scoped>
.customer-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6f8;
}

.cp-container {
  width: min(var(--cp-max-width), calc(100% - 32px));
  margin: 0 auto;
}

.cp-breadcrumb {
  border-bottom: 1px solid #ead6da;
  background: #fff;
}

.cp-breadcrumb .cp-container {
  min-height: 52px;
  display: flex;
  gap: 8px;
  align-items: center;
  color: #6f6a6d;
  font-size: 14px;
}

.cp-home-link {
  text-decoration: none;
  color: #6f6a6d;
}

.cp-home-link:hover {
  color: #8f1121;
}

.cp-breadcrumb strong {
  color: #c5162d;
}

.cp-main {
  flex: 1;
  padding: 24px 0 40px;
}
</style>


