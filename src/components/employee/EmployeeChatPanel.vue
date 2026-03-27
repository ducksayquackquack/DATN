<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue"
import {
  getEmployeeChatMessages,
  getEmployeeChatSessions,
  replyEmployeeChat,
  acceptEmployeeChatSession,
  closeEmployeeChatSession
} from "../../services/employeeChatService"

const sessions = ref([])
const selectedSession = ref(null)
const messages = ref([])
const replyText = ref("")
const loadingSessions = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)

let pollingTimer = null

const currentEmployee = {
  id: String(
    localStorage.getItem("employeeId") ||
    localStorage.getItem("nhanVienId") ||
    localStorage.getItem("userId") ||
    "1"
  ).trim(),
  name: String(
    localStorage.getItem("employeeName") ||
    localStorage.getItem("userName") ||
    localStorage.getItem("displayName") ||
    "Nhân viên DirtyWave"
  ).trim()
}

const normalizeStatus = (value) => String(value || "").trim().toUpperCase()
const normalizeMode = (value) => String(value || "").trim().toUpperCase()

const statusLabel = (value) => {
  const map = {
    OPEN: "Mở",
    WAITING_EMPLOYEE: "Chờ nhân viên",
    IN_PROGRESS: "Đang xử lý",
    CLOSED: "Đã đóng"
  }
  return map[String(value || "").trim().toUpperCase()] || String(value || "")
}

const modeLabel = (value) => {
  const map = {
    HUMAN: "Nhân viên",
    AUTO: "Tự động",
    BOT: "Bot"
  }
  return map[String(value || "").trim().toUpperCase()] || String(value || "")
}

const groupedSessions = computed(() => {
  const map = new Map()

  for (const session of sessions.value) {
    const key = getSessionCustomerKey(session) || `session-${session.id}`
    const prev = map.get(key)

    if (!prev) {
      map.set(key, session)
      continue
    }

    const currentTime = new Date(session?.lastMessageAt || session?.updatedAt || 0).getTime()
    const prevTime = new Date(prev?.lastMessageAt || prev?.updatedAt || 0).getTime()

    if (currentTime >= prevTime) {
      map.set(key, session)
    }
  }

  return Array.from(map.values()).sort((a, b) => {
    const timeA = new Date(a?.lastMessageAt || a?.updatedAt || 0).getTime()
    const timeB = new Date(b?.lastMessageAt || b?.updatedAt || 0).getTime()
    return timeB - timeA
  })
})

const getCustomerDisplayName = (session) => {
  if (!session) return "Khách hàng"

  const candidates = [
    session.customerName,
    session.customerFullName,
    session.customerDisplayName,
    session.tenKhachHang,
    session.khachHang?.tenKhachHang,
    session.customer?.tenKhachHang,
    session.khachHang?.hoTen,
    session.customer?.hoTen,
    session.khachHang?.taiKhoan?.email,
    session.customer?.taiKhoan?.email,
    session.customerEmail,
    session.email,
    session.customerIdentifier,
    session.customerPhone,
    session.phone
  ]

  const matched = candidates.find((value) => String(value || "").trim())
  return matched ? String(matched).trim() : `Phiên #${session.id}`
}

async function acceptSession() {
  if (!selectedSession.value?.id) return

  try {
    await acceptEmployeeChatSession(
      selectedSession.value.id,
      currentEmployee.id,
      currentEmployee.name
    )

    await loadSessions()
    const matched = sessions.value.find((s) => s.id === selectedSession.value?.id)
    if (matched) {
      selectedSession.value = matched
      await loadMessages(matched.id)
    }
  } catch (error) {
    console.error("Không thể tiếp nhận phiên chat", error)
    alert("Tiếp nhận phiên chat thất bại")
  }
}

async function closeSession() {
  if (!selectedSession.value?.id) return

  try {
    await closeEmployeeChatSession(selectedSession.value.id)
    await loadSessions()
  } catch (error) {
    console.error("Không thể kết thúc phiên chat", error)
    alert("Kết thúc phiên chat thất bại")
  }
}

const normalizeSession = (session) => ({
  ...session,
  status: normalizeStatus(session?.status),
  chatMode: normalizeMode(session?.chatMode),
  customerDisplayName: getCustomerDisplayName(session)
})

const selectedSessionTitle = computed(() => {
  if (!selectedSession.value) return "Chưa chọn cuộc trò chuyện"
  return getCustomerDisplayName(selectedSession.value)
})

const getSessionCustomerKey = (session) => {
  return String(
    session?.customerEmail ||
    session?.email ||
    session?.customerIdentifier ||
    session?.customerName ||
    session?.customerDisplayName ||
    session?.tenKhachHang ||
    session?.customerPhone ||
    session?.phone ||
    ""
  ).trim().toLowerCase()
}

const getSessionSortTime = (session) => {
  return new Date(
    session?.lastMessageAt ||
    session?.updatedAt ||
    session?.startedAt ||
    0
  ).getTime()
}

async function loadSessions(showLoading = false) {
  try {
    if (showLoading) loadingSessions.value = true

    const { data } = await getEmployeeChatSessions()
    const nextSessions = (Array.isArray(data) ? data : []).map(normalizeSession)

    sessions.value = nextSessions

    if (!nextSessions.length) {
      selectedSession.value = null
      messages.value = []
      return
    }

    if (!selectedSession.value) {
      await selectSession(nextSessions[0], true)
      return
    }

    const matched = nextSessions.find((s) => s.id === selectedSession.value.id)

    if (matched) {
      selectedSession.value = matched
    } else {
      selectedSession.value = nextSessions[0]
      await loadMessages(nextSessions[0].id, false)
    }
  } catch (error) {
    console.error("Không tải được danh sách chat", error)
  } finally {
    if (showLoading) loadingSessions.value = false
  }
}

async function selectSession(session, silent = false) {
  selectedSession.value = session
  await loadMessages(session.id, !silent)
}

async function loadMessages(sessionId, showLoading = false) {
  if (!sessionId) return

  try {
    if (showLoading) loadingMessages.value = true

    const { data } = await getEmployeeChatMessages(sessionId)
    const nextMessages = Array.isArray(data) ? data : []

    if (JSON.stringify(nextMessages) !== JSON.stringify(messages.value)) {
      messages.value = nextMessages
    }
  } catch (error) {
    console.error("Không tải được tin nhắn", error)
  } finally {
    if (showLoading) loadingMessages.value = false
  }
}

function senderLabel(message) {
  const type = String(message?.senderType || "").toUpperCase()

  if (type === "CUSTOMER") {
    return (
      message?.senderName ||
      selectedSession.value?.customerDisplayName ||
      getCustomerDisplayName(selectedSession.value) ||
      "Khách hàng"
    )
  }

  if (type === "BOT") return "Bot"
  if (type === "EMPLOYEE") return message.senderName || "Nhân viên"
  if (type === "SYSTEM") return "Hệ thống"
  return "Không rõ"
}

function senderClass(message) {
  const type = String(message?.senderType || "").toUpperCase()
  if (type === "CUSTOMER") return "is-customer"
  if (type === "BOT") return "is-bot"
  if (type === "EMPLOYEE") return "is-employee"
  return "is-system"
}

function formatTime(value) {
  if (!value) return ""
  const d = new Date(value)
  return new Intl.DateTimeFormat("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit"
  }).format(d)
}

async function sendReply() {
  const content = String(replyText.value || "").trim()
  if (!selectedSession.value?.id || !content || sending.value) return

  try {
    sending.value = true

    const status = String(selectedSession.value?.status || "").toUpperCase()
    if (status === "OPEN" || status === "WAITING_EMPLOYEE") {
      await acceptEmployeeChatSession(
        selectedSession.value.id,
        currentEmployee.id,
        currentEmployee.name
      )
    }

    await replyEmployeeChat(selectedSession.value.id, {
      senderId: currentEmployee.id,
      senderName: currentEmployee.name,
      content
    })

    replyText.value = ""
    await loadSessions()
    await loadMessages(selectedSession.value.id)
  } catch (error) {
    console.error("Không gửi được phản hồi", error)
    alert("Gửi phản hồi thất bại")
  } finally {
    sending.value = false
  }
}

function handleEnter(event) {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault()
    sendReply()
  }
}

function startPolling() {
  stopPolling()
  pollingTimer = setInterval(async () => {
    await loadSessions(false)
    if (selectedSession.value?.id) {
      await loadMessages(selectedSession.value.id, false)
    }
  }, 3000)
}

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

const canAcceptSelectedSession = computed(() => {
  const status = String(selectedSession.value?.status || "").toUpperCase()
  return status === "OPEN" || status === "WAITING_EMPLOYEE"
})

const canReplySelectedSession = computed(() => {
  const status = String(selectedSession.value?.status || "").toUpperCase()
  return status === "IN_PROGRESS"
})

onMounted(async () => {
  await loadSessions(true)
  startPolling()
})

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<template>
<div class="emp-chat-wrap">
  <aside class="emp-chat-sidebar">
    <div class="emp-chat-sidebar__head">
      <h3>Hội thoại khách hàng</h3>
      <span>{{ groupedSessions.length }} cuộc chat</span>
    </div>

    <div v-if="loadingSessions" class="emp-chat-empty">
      Đang tải danh sách...
    </div>

    <button
      v-for="session in groupedSessions"
      :key="session.id"
      type="button"
      class="emp-session-item"
      :class="{ active: selectedSession?.id === session.id }"
      @click="selectSession(session)"
    >
      <div class="emp-session-item__top">
        <strong>{{ getCustomerDisplayName(session) }}</strong>
        <span>{{ modeLabel(session.chatMode) }}</span>
      </div>

      <div class="emp-session-item__bottom">
        <small>{{ statusLabel(session.status) }}</small>
        <small>{{ formatTime(session.lastMessageAt) }}</small>
      </div>
    </button>

    <div v-if="!loadingSessions && !groupedSessions.length" class="emp-chat-empty">
      Chưa có hội thoại nào
    </div>
  </aside>

  <section class="emp-chat-main">
    <header class="emp-chat-main__head">
      <div>
        <h3>{{ selectedSessionTitle }}</h3>
        <small v-if="selectedSession">
          Chế độ: {{ modeLabel(selectedSession.chatMode) }} • Trạng thái: {{ statusLabel(selectedSession.status) }}
        </small>
        <br v-if="selectedSession?.assignedEmployeeName" />
        <small v-if="selectedSession?.assignedEmployeeName">
          Nhân viên phụ trách: {{ selectedSession.assignedEmployeeName }}
        </small>
      </div>

      <div v-if="selectedSession" class="emp-chat-actions">
  <button
    v-if="canAcceptSelectedSession"
    type="button"
    class="emp-action-btn emp-action-btn--accept"
    @click="acceptSession"
  >
    Tiếp nhận
  </button>

  <button
    v-if="String(selectedSession?.status || '').toUpperCase() === 'IN_PROGRESS'"
    type="button"
    class="emp-action-btn emp-action-btn--close"
    @click="closeSession"
  >
    Kết thúc phiên
  </button>
</div>
    </header>

    <div class="emp-chat-main__body">
      <div v-if="!selectedSession" class="emp-chat-empty">
        Chọn một cuộc trò chuyện để bắt đầu
      </div>

      <div v-else-if="loadingMessages" class="emp-chat-empty">
        Đang tải tin nhắn...
      </div>

      <template v-else>
        <article
          v-for="message in messages"
          :key="message.id"
          class="emp-msg"
          :class="senderClass(message)"
        >
          <div class="emp-msg__meta">
            <strong>{{ senderLabel(message) }}</strong>
            <span>{{ formatTime(message.createdAt) }}</span>
          </div>

          <div class="emp-msg__bubble">
            {{ message.content }}
          </div>
        </article>

        <div v-if="!messages.length" class="emp-chat-empty">
          Chưa có tin nhắn nào trong hội thoại này
        </div>
      </template>
    </div>

    <footer class="emp-chat-main__footer">
      <textarea
  v-model="replyText"
  rows="3"
  :disabled="!canReplySelectedSession"
  :placeholder="
    canReplySelectedSession
      ? 'Nhập phản hồi cho khách hàng...'
      : 'Tiếp nhận phiên chat để bắt đầu phản hồi...'
  "
  @keydown="handleEnter"
/>

<button
  type="button"
  :disabled="!canReplySelectedSession || !replyText.trim() || sending"
  @click="sendReply"
>
  {{ sending ? "Đang gửi..." : "Gửi phản hồi" }}
</button>
    </footer>
  </section>
</div>
</template>

<style scoped>
.emp-chat-wrap {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  min-height: 720px;
}

.emp-chat-sidebar,
.emp-chat-main {
  background: #fff;
  border: 1px solid #e9e9ef;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.06);
}

.emp-chat-sidebar {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
}

.emp-chat-sidebar__head,
.emp-chat-main__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #efeff5;
}

.emp-chat-sidebar__head h3,
.emp-chat-main__head h3 {
  margin: 0;
  font-size: 18px;
}

.emp-session-item {
  text-align: left;
  border: 1px solid #ececf3;
  background: #fafafe;
  border-radius: 16px;
  padding: 12px;
  cursor: pointer;
  transition: 0.2s ease;
}

.emp-session-item:hover,
.emp-session-item.active {
  border-color: #c5162d;
  background: #fff4f6;
}

.emp-session-item__top,
.emp-session-item__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.emp-session-item__top strong {
  font-size: 14px;
}

.emp-session-item__top span {
  font-size: 12px;
  font-weight: 700;
  color: #c5162d;
}

.emp-session-item__bottom {
  margin-top: 8px;
  color: #777;
}

.emp-chat-main {
  display: grid;
  grid-template-rows: auto 1fr auto;
  overflow: hidden;
}

.emp-chat-main__head {
  padding: 16px 18px;
}

.emp-chat-main__body {
  padding: 16px 18px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #fcfcff;
}

.emp-msg {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.emp-msg__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #666;
}

.emp-msg__bubble {
  max-width: 72%;
  padding: 12px 14px;
  border-radius: 16px;
  line-height: 1.45;
  white-space: pre-wrap;
  border: 1px solid #ececf2;
  background: #fff;
}

.emp-msg.is-customer .emp-msg__bubble {
  background: #eef6ff;
  border-color: #d6e8ff;
}

.emp-msg.is-bot .emp-msg__bubble {
  background: #fff4f6;
  border-color: #f3d4da;
}

.emp-msg.is-employee {
  align-items: flex-end;
}

.emp-msg.is-employee .emp-msg__meta {
  justify-content: flex-end;
}

.emp-msg.is-employee .emp-msg__bubble {
  background: linear-gradient(135deg, #7b0f1e, #c5162d);
  color: #fff;
  border-color: transparent;
}

.emp-msg.is-system .emp-msg__bubble {
  background: #f6f6f8;
  border-color: #e6e6ea;
}

.emp-chat-main__footer {
  padding: 16px 18px;
  border-top: 1px solid #efeff5;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  background: #fff;
}

.emp-chat-main__footer textarea {
  resize: none;
  border: 1px solid #dddfea;
  border-radius: 14px;
  padding: 12px 14px;
  font: inherit;
  outline: none;
}

.emp-chat-main__footer button {
  border: none;
  border-radius: 14px;
  padding: 0 18px;
  min-width: 140px;
  background: #c5162d;
  color: #fff;
  cursor: pointer;
  font-weight: 700;
}

.emp-chat-main__footer button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.emp-chat-empty {
  padding: 20px;
  text-align: center;
  color: #777;
}

@media (max-width: 1100px) {
  .emp-chat-wrap {
    grid-template-columns: 1fr;
  }

  .emp-msg__bubble {
    max-width: 100%;
  }

  .emp-chat-main__footer {
    grid-template-columns: 1fr;
  }
}

.emp-chat-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.emp-action-btn {
  appearance: none;
  border: 1px solid rgba(255,255,255,0.15);
  outline: none;
  border-radius: 999px;
  padding: 11px 20px;
  min-height: 44px;
  font-size:12px;
  font-weight: 600;
  letter-spacing: 0.2px;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    filter 0.18s ease,
    opacity 0.18s ease;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.14);
}

.emp-action-btn:hover {
  transform: translateY(-1px);
  filter: brightness(1.03);
}

.emp-action-btn:active {
  transform: translateY(0);
}

.emp-action-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.emp-action-btn--accept {
  color: #ffffff;
  background: linear-gradient(135deg, #b91c1c, #ef4444);
}

.emp-action-btn--accept:hover {
  box-shadow: 0 12px 26px rgba(239, 68, 68, 0.28);
}

.emp-action-btn--close {
  color: #ffffff;
  background: linear-gradient(135deg, #b91c1c, #ef4444);
}

.emp-action-btn--close:hover {
  box-shadow: 0 12px 26px rgba(239, 68, 68, 0.28);
}
</style>