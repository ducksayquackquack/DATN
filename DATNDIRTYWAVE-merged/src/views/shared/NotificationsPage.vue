<script setup>
import { computed } from "vue"
import { useRoute, useRouter } from "vue-router"
import { Bell, CheckCheck, RefreshCw } from "lucide-vue-next"
import { getScopeFromPath, useNotifications } from "../../composables/useNotifications"
import SiteNav from "../../components/SiteNav.vue"

const route = useRoute()
const router = useRouter()
const scope = computed(() => getScopeFromPath(route.path))
const { notifications, loading, unreadCount, refresh, markAllAsRead, markOneAsRead } = useNotifications(scope.value)

const title = computed(() => {
  if (scope.value === "admin") return "Thông báo quản trị"
  if (scope.value === "employee") return "Thông báo nhân viên"
  return "Thông báo của bạn"
})

const goToLink = (item) => {
  if (!item?.read) {
    markOneAsRead(item.id)
  }
  if (!item?.link) return
  router.push(item.link)
}

const goToOrderDetail = (item) => {
  markOneAsRead(item.id)
  router.push(item.link)
}

const formatDateTime = (value) => {
  if (!value) return "-"
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return "-"
  return `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}/${d.getFullYear()} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`
}
</script>

<template>
  <div class="notify-page-root" :class="{ 'notify-page-root--customer': scope === 'customer' }">
    <SiteNav v-if="scope === 'customer'" />

    <main class="notify-wrap" :class="{ 'notify-wrap--customer': scope === 'customer' }">
      <section class="notify-card">
        <header class="notify-head">
          <div class="notify-title-wrap">
            <Bell :size="18" />
            <div>
              <h1>{{ title }}</h1>
              <small>Có {{ unreadCount }} thông báo chưa đọc</small>
            </div>
          </div>

          <div class="notify-actions">
            <button class="btn ghost" type="button" @click="refresh">
              <RefreshCw :size="14" />
              <span>Làm mới</span>
            </button>
            <button class="btn" type="button" @click="markAllAsRead" :disabled="!notifications.length">
              <CheckCheck :size="14" />
              <span>Đánh dấu đã đọc</span>
            </button>
          </div>
        </header>

        <p v-if="loading" class="notify-muted">Đang tải thông báo...</p>
        <p v-else-if="!notifications.length" class="notify-muted">Chưa có thông báo mới.</p>

        <article
          v-for="item in notifications"
          :key="item.id"
          class="notify-item"
          :class="{ unread: !item.read }"
          @click="goToLink(item)"
        >
          <div class="notify-item-content">
            <div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
            </div>
            <button
              v-if="item.showDetail"
              class="notify-detail-btn"
              type="button"
              @click.stop="goToOrderDetail(item)"
            >Xem chi tiết đơn hàng</button>
          </div>
          <small>{{ formatDateTime(item.createdAt) }}</small>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.notify-page-root {
  min-height: 100%;
}

.notify-page-root--customer {
  background: #f4f5f7;
}

.notify-wrap {
  padding: 20px;
}

.notify-wrap--customer {
  min-height: calc(100vh - 140px);
  background: #f4f5f7;
}

.notify-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 18px;
}

.notify-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}

.notify-title-wrap {
  display: flex;
  gap: 10px;
  align-items: center;
}

.notify-title-wrap h1 {
  margin: 0;
  font-size: 20px;
}

.notify-title-wrap small {
  color: #6b7280;
}

.notify-actions {
  display: flex;
  gap: 8px;
}

.btn {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #111827;
  border-radius: 10px;
  padding: 8px 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.btn.ghost {
  background: #f9fafb;
}

.notify-muted {
  color: #6b7280;
}

.notify-item {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px;
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  cursor: pointer;
}

.notify-item.unread {
  border-color: #f87171;
  background: #fff7f7;
}

.notify-item h3 {
  margin: 0 0 4px;
  font-size: 15px;
}

.notify-item p {
  margin: 0;
  color: #374151;
}

.notify-item small {
  color: #6b7280;
  white-space: nowrap;
}

.notify-item-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.notify-detail-btn {
  align-self: flex-start;
  background: #111827;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.notify-detail-btn:hover {
  background: #dc2626;
}
</style>
