<script setup>
import { ref } from "vue"
import MauSacList from "../mau-sac/MauSacList.vue"
import KichThuocList from "../kich-thuoc/KichThuocList.vue"

const activeTab = ref("mau-sac")
</script>

<template>
  <main class="wrap thuoc-tinh-page">
    <div class="tabs-shell">
      <div class="tabs-bar">
        <button
          type="button"
          class="tab-btn"
          :class="{ active: activeTab === 'mau-sac' }"
          @click="activeTab = 'mau-sac'"
        >
          Màu sắc
        </button>
        <button
          type="button"
          class="tab-btn"
          :class="{ active: activeTab === 'kich-thuoc' }"
          @click="activeTab = 'kich-thuoc'"
        >
          Kích thước
        </button>
      </div>
    </div>

    <div class="tt-content">
      <MauSacList v-if="activeTab === 'mau-sac'" />
      <KichThuocList v-else />
    </div>
  </main>
</template>

<style scoped>
.thuoc-tinh-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tabs-shell {
  padding: 0;
}

.tabs-bar {
  display: flex;
  gap: 8px;
  background: #fff;
  border: 1px solid #f1d5d9;
  border-radius: 14px;
  padding: 6px;
  width: fit-content;
}

.tab-btn {
  height: 40px;
  min-width: 120px;
  padding: 0 18px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: #fff;
  color: rgba(17, 24, 39, 0.75);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.tab-btn.active {
  border-color: rgba(147, 19, 36, 0.4);
  background: linear-gradient(90deg, #7c1021 0%, #a9162d 35%, #cf243a 68%, #e64555 100%);
  color: #fff;
}

.tt-content {
  margin-top: 0;
}
</style>
