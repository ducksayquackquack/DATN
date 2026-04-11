<script setup>
import { computed, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import DiscountPage from "./dot-giam-gia/DiscountPage.vue"
import VoucherListPanel from "./VoucherListPanel.vue"

const router = useRouter()
const route = useRoute()

const basePath = computed(() => (route.path.startsWith("/employee") ? "/employee" : "/admin"))
const activeTab = computed(() => (route.query.tab === "vouchers" ? "vouchers" : "campaigns"))

const setTab = (tab) => {
  router.push(`${basePath.value}/khuyen-mai/list?tab=${tab}`)
}

watch(
  () => route.query.tab,
  (tab) => {
    if (tab !== "vouchers" && tab !== "campaigns") {
      router.replace(`${basePath.value}/khuyen-mai/list?tab=campaigns`)
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="km-list-page">
    <div class="tabs">
      <button class="tab-btn" :class="{ active: activeTab === 'campaigns' }" @click="setTab('campaigns')">
        Đợt giảm giá
      </button>
      <button class="tab-btn" :class="{ active: activeTab === 'vouchers' }" @click="setTab('vouchers')">
        Phiếu giảm giá
      </button>
    </div>

    <DiscountPage v-if="activeTab === 'campaigns'" />
    <VoucherListPanel v-else />
  </div>
</template>

<style scoped>
.km-list-page {
  padding-top: 4px;
}

.tabs {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border: 1px solid rgba(255, 77, 79, 0.18);
  border-radius: 12px;
  padding: 8px;
  margin-bottom: 14px;
}

.tab-btn {
  border: none;
  background: transparent;
  color: rgba(17, 24, 39, 0.75);
  border-radius: 8px;
  padding: 9px 14px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.tab-btn.active {
  background: linear-gradient(90deg, #ff4d4f 0%, #111827 100%);
  color: #fff;
}
</style>
