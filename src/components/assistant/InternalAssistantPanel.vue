<template>
  <div class="assistant-panel">
    <header class="assistant-header">
      <div class="assistant-header-main">
        <div class="assistant-avatar">AI</div>
        <div class="assistant-header-copy">
          <h3>Trợ lý AI nội bộ</h3>
          <div class="assistant-subtitle">
            Hỗ trợ tra cứu đơn hàng, sản phẩm, tồn kho, lịch làm và doanh thu
          </div>

          <div class="assistant-meta-row">
            <span class="assistant-badge role">{{ roleLabel }}</span>
            <span class="assistant-badge context">{{ contextLabel }}</span>
            <span v-if="sessionCode" class="assistant-badge muted">{{ sessionCode }}</span>
          </div>
        </div>
      </div>

      <div class="assistant-header-actions">
        <button class="secondary-btn" @click="clearHistory">Xóa lịch sử</button>
        <button class="close-btn" @click="$emit('close')">×</button>
      </div>
    </header>

    <section class="assistant-context-strip">
      <div>
        <div class="strip-label">Ngữ cảnh hiện tại</div>
        <div class="strip-value">{{ contextDescription }}</div>
      </div>

      <div class="status-pill" :class="{ live: !loading, busy: loading }">
        {{ loading ? "Đang xử lý" : "Sẵn sàng demo" }}
      </div>
    </section>

    <section v-if="showQuickActions" class="quick-actions-block">
      <div class="quick-actions-head">
        <div class="quick-actions-title">Gợi ý thao tác nhanh</div>
        <div class="quick-actions-subtitle">
          Chọn câu hỏi mẫu để demo nhanh và ổn định hơn
        </div>
      </div>

      <div class="quick-actions">
        <button
          v-for="item in displayedQuickActions"
          :key="item"
          class="quick-btn"
          @click="sendQuickAction(item)"
        >
          {{ item }}
        </button>
      </div>
    </section>

    <div ref="messagesContainer" class="messages">
      <div v-if="messages.length <= 1 && !loading" class="assistant-empty-state">
        <div class="assistant-empty-icon">AI</div>
        <div class="empty-title">{{ emptyStateTitle }}</div>
        <div class="empty-text">{{ emptyStateDescription }}</div>
      </div>

      <div
        v-for="(msg, index) in messages"
        :key="`${msg.sender}-${index}`"
        :class="['message-item', msg.sender]"
      >
        <div class="message-avatar">
          {{ msg.sender === "user" ? "U" : msg.sender === "assistant" ? "AI" : "SYS" }}
        </div>

        <div class="message-body">
          <div class="message-topline">
            <span class="message-sender">{{ senderLabel(msg.sender) }}</span>
            <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
          </div>

          <div class="message-bubble">
            <div class="message-text">{{ msg.text }}</div>

            <div
              v-if="msg.sender === 'assistant' && msg.toolCalls && msg.toolCalls.length"
              class="tool-actions-wrap"
            >
              <button class="inline-action" @click="toggleToolDetails(index)">
                {{ showToolDetailsMap[index] ? "Ẩn kỹ thuật" : "Xem kỹ thuật" }}
              </button>

              <div v-if="showToolDetailsMap[index]" class="tool-calls">
                <div class="tool-title">Tool đã dùng</div>
                <div class="tool-list">
                  <span
                    v-for="(tool, tIndex) in msg.toolCalls"
                    :key="`${index}-${tIndex}`"
                    class="tool-item"
                  >
                    {{ tool.tool || "tool" }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div
            v-if="msg.sender === 'assistant' && !loading && msg.text"
            class="message-actions"
          >
            <button class="inline-action" @click="copyMessage(msg.text)">Copy</button>
            <button
              v-if="isCustomerChatContext"
              class="inline-action primary"
              @click="insertIntoCustomerChat(msg.text)"
            >
              Chèn vào ô chat khách
            </button>
          </div>
        </div>
      </div>

      <div v-if="loading" class="message-item assistant typing-row">
        <div class="message-avatar">AI</div>
        <div class="message-body">
          <div class="message-topline">
            <span class="message-sender">Assistant</span>
          </div>

          <div class="message-bubble typing-bubble-wrap">
            <div class="typing-bubble">
              <span></span>
              <span></span>
              <span></span>
            </div>
            <div class="typing-text">{{ loadingText }}</div>
          </div>
        </div>
      </div>
    </div>

    <footer class="assistant-footer">
      <textarea
        v-model="inputMessage"
        class="assistant-input"
        rows="3"
        :placeholder="inputPlaceholder"
        @keydown.enter.exact.prevent="handleSend"
      />

      <div class="assistant-footer-actions">
        <div class="assistant-hint">
          {{
            isCustomerChatContext
              ? "Có thể chèn trực tiếp câu trả lời vào ô chat khách."
              : "Ưu tiên hỏi ngắn, rõ intent để demo đẹp hơn."
          }}
        </div>

        <button
          class="send-btn"
          :disabled="loading || !inputMessage.trim()"
          @click="handleSend"
        >
          {{ loading ? "Đang gửi..." : "Gửi" }}
        </button>
      </div>
    </footer>
  </div>
</template>

<script>
import { nextTick } from "vue";
import { assistantService } from "@/services/assistantService";

const PAGE_LABELS = {
  GENERAL_INTERNAL: "Trợ lý nội bộ",
  CUSTOMER_CHAT: "Hỗ trợ chat khách",
  REVENUE_DASHBOARD: "Dashboard doanh thu",
  POS: "Bán hàng tại quầy",
  ORDER_DETAIL: "Chi tiết đơn hàng",
};

export default {
  name: "InternalAssistantPanel",
  props: {
    role: {
      type: String,
      default: "EMPLOYEE",
    },
    source: {
      type: String,
      default: "EMPLOYEE_PANEL",
    },
    context: {
      type: Object,
      default: () => ({}),
    },
  },
  data() {
    return {
      loading: false,
      inputMessage: "",
      sessionCode: null,
      showToolDetailsMap: {},
      recommendedActions: [],
      messages: [this.buildGreetingMessage()],
    };
  },
  computed: {
    resolvedPageType() {
      const explicitPageType = this.context?.pageType;
      if (explicitPageType) return explicitPageType;

      if (
        this.context?.sessionId ||
        this.context?.lastCustomerMessage ||
        this.context?.currentReplyDraft
      ) {
        return "CUSTOMER_CHAT";
      }

      return "GENERAL_INTERNAL";
    },
    roleLabel() {
      return this.role === "ADMIN" ? "Chế độ quản trị" : "Chế độ nhân viên";
    },
    contextLabel() {
      return PAGE_LABELS[this.resolvedPageType] || "Trợ lý nội bộ";
    },
    contextDescription() {
      const route = this.context?.route || "Không xác định";
      const page = PAGE_LABELS[this.resolvedPageType] || "Màn hình nội bộ";
      return `${page} • ${route}`;
    },
    isCustomerChatContext() {
      return this.resolvedPageType === "CUSTOMER_CHAT";
    },
    showQuickActions() {
      return this.messages.length <= 1 && !this.loading;
    },
    inputPlaceholder() {
      const pageType = this.resolvedPageType;

      if (pageType === "CUSTOMER_CHAT") {
        return "Ví dụ: Viết giúp tôi câu trả lời cho khách về đơn HD202401001";
      }

      if (pageType === "REVENUE_DASHBOARD") {
        return "Ví dụ: Doanh thu tháng này, top 5 sản phẩm bán chạy, có bao nhiêu đơn hủy, có bao nhiêu đơn hoàn thành?";
      }

      if (pageType === "POS") {
        return "Ví dụ: SP001 còn hàng không, tìm sản phẩm hoodie, gợi ý 3 sản phẩm dễ chốt sale";
      }

      if (pageType === "ORDER_DETAIL") {
        return "Ví dụ: Đơn này đang ở trạng thái gì, tóm tắt nhanh đơn hàng này";
      }

      return "Ví dụ: SP001 còn hàng không, đơn HD202401001 đang ở trạng thái gì, hoặc doanh thu tháng này";
    },
    loadingText() {
      const pageType = this.resolvedPageType;

      if (pageType === "CUSTOMER_CHAT") {
        return "Assistant đang soạn gợi ý trả lời khách...";
      }

      if (pageType === "REVENUE_DASHBOARD") {
        return "Assistant đang tổng hợp doanh thu và số liệu...";
      }

      if (pageType === "POS") {
        return "Assistant đang kiểm tra sản phẩm và tồn kho...";
      }

      if (pageType === "ORDER_DETAIL") {
        return "Assistant đang tra cứu thông tin đơn hàng...";
      }

      return "Assistant đang đọc ngữ cảnh và tìm dữ liệu phù hợp...";
    },
    emptyStateTitle() {
      return "Trợ lý AI sẵn sàng hỗ trợ";
    },
    emptyStateDescription() {
      if (this.isCustomerChatContext) {
        return "Anh/chị có thể nhờ viết câu trả lời cho khách, tra đơn, kiểm tra tồn kho hoặc gợi ý sản phẩm dễ tư vấn.";
      }

      if (this.resolvedPageType === "REVENUE_DASHBOARD") {
        return "Anh/chị có thể hỏi về doanh thu, top sản phẩm bán chạy, số lượng đơn theo trạng thái hoặc lịch làm gần nhất.";
      }

      return "Anh/chị có thể hỏi về đơn hàng, sản phẩm, tồn kho, doanh thu, lịch làm hoặc nhờ assistant tóm tắt nhanh dữ liệu.";
    },
    defaultQuickActions() {
      const pageType = this.resolvedPageType;
      const hasCustomerMessage = !!this.context?.lastCustomerMessage;

      if (pageType === "CUSTOMER_CHAT") {
        return hasCustomerMessage
          ? [
              "Viết giúp tôi câu trả lời cho khách về đơn HD202401001",
              "Viết mềm hơn và lịch sự hơn",
              "Gợi ý 3 sản phẩm hoodie dễ chốt sale",
              "Rút gọn câu trả lời",
            ]
          : [
              "Tìm sản phẩm hoodie",
              "SP001 còn hàng không?",
              "Đơn HD202401001 đang ở trạng thái gì?",
              "Gợi ý 3 sản phẩm hoodie dễ chốt sale",
            ];
      }

      if (pageType === "REVENUE_DASHBOARD") {
        return [
          "Doanh thu tháng này",
          "Top 5 sản phẩm bán chạy",
          "Có bao nhiêu đơn hủy?",
          "Có bao nhiêu đơn chờ xử lý?",
          "Có bao nhiêu đơn hoàn thành?",
          "Sản phẩm nào có số lượt click nhiều nhất?",
        ];
      }

      if (pageType === "POS") {
        return [
          "SP001 còn hàng không?",
          "Tìm sản phẩm hoodie",
          "Gợi ý 3 sản phẩm hoodie dễ chốt sale",
          "Tìm sản phẩm áo khoác",
        ];
      }

      if (pageType === "ORDER_DETAIL") {
        return [
          "Đơn này đang ở trạng thái gì?",
          "Tóm tắt nhanh đơn hàng này",
          "Viết giúp tôi câu trả lời cho khách về đơn này",
          "Gợi ý bước xử lý tiếp theo",
        ];
      }

      if (this.role === "ADMIN") {
        return [
          "Doanh thu tháng này",
          "Top 5 sản phẩm bán chạy",
          "Có bao nhiêu đơn hủy?",
          "Có bao nhiêu đơn chờ xử lý?",
          "Có bao nhiêu đơn hoàn thành?",
          "Sản phẩm nào có số lượt click nhiều nhất?",
        ];
      }

      return [
        "Tìm sản phẩm hoodie",
        "SP001 còn hàng không?",
        "Đơn HD202401001 đang ở trạng thái gì?",
        "Gợi ý 3 sản phẩm hoodie dễ chốt sale",
      ];
    },
    displayedQuickActions() {
      const actions = this.recommendedActions?.length
        ? this.recommendedActions
        : this.defaultQuickActions;
      return actions.slice(0, 6);
    },
  },
  watch: {
    messages: {
      deep: true,
      handler() {
        this.saveHistoryToStorage();
      },
    },
    sessionCode() {
      this.saveHistoryToStorage();
    },
    context: {
      deep: true,
      handler() {
        this.loadHistoryFromStorage();
        this.recommendedActions = [];
      },
    },
    role() {
      this.loadHistoryFromStorage();
    },
  },
  mounted() {
    this.loadHistoryFromStorage();
    this.scrollToBottom();
  },
  methods: {
    buildGreetingMessage() {
      return {
        sender: "assistant",
        text:
          this.role === "ADMIN"
            ? "Xin chào anh/chị Admin. Em đã sẵn sàng hỗ trợ doanh thu, top sản phẩm, tồn kho thấp, lịch làm và tra cứu dữ liệu để demo."
            : "Xin chào anh/chị. Em có thể hỗ trợ tìm sản phẩm, kiểm tra tồn kho, tra cứu đơn hàng và gợi ý trả lời khách.",
        toolCalls: [],
        createdAt: new Date().toISOString(),
      };
    },
    senderLabel(sender) {
      if (sender === "user") return "Bạn";
      if (sender === "assistant") return "Assistant";
      return "Hệ thống";
    },
    formatTime(value) {
      if (!value) return "";
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) return "";
      return date.toLocaleTimeString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
      });
    },
    toggleToolDetails(index) {
      this.showToolDetailsMap = {
        ...this.showToolDetailsMap,
        [index]: !this.showToolDetailsMap[index],
      };
    },
    getStorageKey() {
      const pageType = this.resolvedPageType || "GENERAL_INTERNAL";
      const route = this.context?.route || "unknown-route";
      const role = this.role || "EMPLOYEE";
      return `dirtywave_ai_history:${role}:${pageType}:${route}`;
    },
    loadHistoryFromStorage() {
      try {
        const raw = localStorage.getItem(this.getStorageKey());
        if (!raw) {
          this.sessionCode = null;
          this.messages = [this.buildGreetingMessage()];
          return;
        }

        const parsed = JSON.parse(raw);
        const savedMessages = Array.isArray(parsed?.messages) ? parsed.messages : [];
        this.messages = savedMessages.length ? savedMessages : [this.buildGreetingMessage()];
        this.sessionCode = parsed?.sessionCode || null;
        this.scrollToBottom();
      } catch (error) {
        console.error("Không load được lịch sử assistant", error);
      }
    },
    saveHistoryToStorage() {
      try {
        localStorage.setItem(
          this.getStorageKey(),
          JSON.stringify({
            sessionCode: this.sessionCode || this.context?.sessionCode || null,
            messages: (this.messages || []).slice(-50),
            updatedAt: new Date().toISOString(),
          })
        );
      } catch (error) {
        console.error("Không lưu được lịch sử assistant", error);
      }
    },
    clearHistory() {
      localStorage.removeItem(this.getStorageKey());
      this.sessionCode = null;
      this.recommendedActions = [];
      this.messages = [this.buildGreetingMessage()];
      window.toast?.success?.("Đã xóa lịch sử trợ lý");
      this.scrollToBottom();
    },
    async copyMessage(text) {
      try {
        await navigator.clipboard.writeText(text);
        window.toast?.success?.("Đã copy câu trả lời");
      } catch (error) {
        window.toast?.error?.("Không copy được nội dung");
      }
    },
    insertIntoCustomerChat(text) {
      window.dispatchEvent(
        new CustomEvent("assistant:insert-customer-reply", {
          detail: {
            text,
            context: this.context || {},
          },
        })
      );
      window.toast?.success?.("Đã chèn nội dung vào ô chat khách");
    },
    async sendQuickAction(text) {
      this.inputMessage = text;
      await this.handleSend();
    },
    async handleSend() {
      const text = this.inputMessage.trim();
      if (!text || this.loading) return;

      this.messages.push({
        sender: "user",
        text,
        toolCalls: [],
        createdAt: new Date().toISOString(),
      });

      this.inputMessage = "";
      this.loading = true;
      this.scrollToBottom();

      try {
        const response = await assistantService.sendMessage({
          sessionCode: this.sessionCode || this.context?.sessionCode || null,
          message: text,
          role: this.role,
          source: this.source,
          context: this.context || {},
        });

        if (response?.sessionCode) {
          this.sessionCode = response.sessionCode;
        }

        if (Array.isArray(response?.quickReplies)) {
          this.recommendedActions = response.quickReplies.filter(Boolean);
        }

        this.messages.push({
          sender: "assistant",
          text: response?.message || "Không có phản hồi từ assistant.",
          toolCalls: response?.toolCalls || [],
          createdAt: new Date().toISOString(),
        });
      } catch (error) {
        const status = error?.response?.status;
        if (status === 401) {
          this.messages.push({
            sender: "system",
            text: "Phiên đăng nhập nội bộ đã hết hạn hoặc chưa được thiết lập. Anh/chị vui lòng đăng nhập lại bằng tài khoản nhân viên/quản trị viên rồi thử lại.",
            toolCalls: [],
            createdAt: new Date().toISOString(),
          });
          window.dispatchEvent(
            new CustomEvent("assistant:auth-required", {
              detail: {
                source: this.source,
                context: this.context || {},
              },
            })
          );
        } else if (status === 403) {
          this.messages.push({
            sender: "system",
            text: "Tài khoản hiện tại không có quyền dùng trợ lý nội bộ ở màn hình này.",
            toolCalls: [],
            createdAt: new Date().toISOString(),
          });
        } else {
          this.messages.push({
            sender: "assistant",
            text:
              error?.response?.data?.error ||
              error?.response?.data?.message ||
              "Không gọi được assistant. Vui lòng kiểm tra backend.",
            toolCalls: [],
            createdAt: new Date().toISOString(),
          });
        }
      } finally {
        this.loading = false;
        this.scrollToBottom();
      }
    },
    scrollToBottom() {
      nextTick(() => {
        const el = this.$refs.messagesContainer;
        if (el) el.scrollTop = el.scrollHeight;
      });
    },
  },
};
</script>

<style scoped>
.assistant-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.1), transparent 26%),
    linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.assistant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 18px 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
}

.assistant-header-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.assistant-header-copy {
  min-width: 0;
}

.assistant-avatar {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 14px;
  color: #fff;
  background: linear-gradient(135deg, #111827, #2563eb);
  box-shadow: 0 12px 25px rgba(37, 99, 235, 0.24);
}

.assistant-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.assistant-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.45;
}

.assistant-meta-row {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.assistant-badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 11px;
  font-weight: 700;
}

.assistant-badge.role {
  background: #dbeafe;
  color: #1d4ed8;
}

.assistant-badge.context {
  background: #e2e8f0;
  color: #334155;
}

.assistant-badge.muted {
  background: #f1f5f9;
  color: #64748b;
}

.assistant-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.secondary-btn,
.close-btn,
.inline-action,
.quick-btn,
.send-btn {
  border: none;
  cursor: pointer;
}

.secondary-btn {
  padding: 10px 12px;
  border-radius: 12px;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 700;
}

.close-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: #f8fafc;
  color: #0f172a;
  font-size: 24px;
}

.assistant-context-strip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
}

.strip-label {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 4px;
}

.strip-value {
  font-size: 13px;
  color: #0f172a;
  font-weight: 600;
}

.status-pill {
  flex-shrink: 0;
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  background: #e2e8f0;
  color: #334155;
}

.status-pill.live {
  background: #dcfce7;
  color: #166534;
}

.status-pill.busy {
  background: #dbeafe;
  color: #1d4ed8;
}

.quick-actions-block {
  padding: 14px 18px 8px;
}

.quick-actions-head {
  margin-bottom: 10px;
}

.quick-actions-title {
  font-size: 13px;
  font-weight: 800;
  color: #0f172a;
}

.quick-actions-subtitle {
  margin-top: 3px;
  font-size: 12px;
  color: #64748b;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-btn {
  padding: 9px 12px;
  border-radius: 999px;
  border: 1px solid #dbeafe;
  background: rgba(255, 255, 255, 0.92);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  transition: all 0.18s ease;
}

.quick-btn:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 18px 18px;
}

.assistant-empty-state {
  margin: 6px 0 18px;
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px dashed #cbd5e1;
  text-align: center;
}

.assistant-empty-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 18px;
  color: #fff;
  background: linear-gradient(135deg, #0f172a, #2563eb);
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.18);
}

.empty-title {
  font-size: 16px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 6px;
}

.empty-text {
  font-size: 13px;
  line-height: 1.65;
  color: #475569;
  max-width: 560px;
  margin: 0 auto;
}

.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 800;
  color: #0f172a;
  background: #e2e8f0;
}

.message-item.assistant .message-avatar {
  color: #fff;
  background: linear-gradient(135deg, #0f172a, #2563eb);
}

.message-item.user .message-avatar {
  color: #fff;
  background: linear-gradient(135deg, #1d4ed8, #3b82f6);
}

.message-body {
  min-width: 0;
  max-width: 86%;
}

.message-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.message-sender {
  font-size: 12px;
  font-weight: 800;
  color: #334155;
}

.message-time {
  font-size: 11px;
  color: #94a3b8;
}

.message-bubble {
  border-radius: 18px;
  padding: 14px 15px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.05);
}

.message-item.user .message-bubble {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  border-color: transparent;
}

.message-text {
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.65;
}

.message-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.inline-action {
  padding: 7px 10px;
  border-radius: 10px;
  background: #e2e8f0;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.inline-action.primary {
  background: #dbeafe;
  color: #1d4ed8;
}

.tool-actions-wrap {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed rgba(148, 163, 184, 0.5);
}

.tool-title {
  font-size: 12px;
  font-weight: 800;
  color: #334155;
  margin-bottom: 8px;
}

.tool-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tool-item {
  display: inline-flex;
  padding: 5px 9px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 700;
}

.typing-row {
  align-items: flex-start;
}

.typing-bubble-wrap {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.typing-bubble {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.typing-bubble span {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #94a3b8;
  animation: assistant-typing 1s infinite ease-in-out;
}

.typing-bubble span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-bubble span:nth-child(3) {
  animation-delay: 0.3s;
}

.typing-text {
  font-size: 13px;
  color: #64748b;
  line-height: 1.45;
}

.assistant-footer {
  padding: 16px 18px 18px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.9);
}

.assistant-input {
  width: 100%;
  resize: none;
  border: 1px solid #cbd5e1;
  border-radius: 16px;
  padding: 14px 15px;
  font-size: 14px;
  line-height: 1.5;
  outline: none;
  background: #fff;
}

.assistant-input:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.12);
}

.assistant-footer-actions {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.assistant-hint {
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
}

.send-btn {
  flex-shrink: 0;
  padding: 11px 18px;
  border-radius: 12px;
  background: linear-gradient(135deg, #0f172a, #2563eb);
  color: #fff;
  font-weight: 800;
  min-width: 110px;
}

.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@keyframes assistant-typing {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.35;
  }
  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

@media (max-width: 640px) {
  .assistant-header,
  .assistant-context-strip,
  .quick-actions-block,
  .messages,
  .assistant-footer {
    padding-left: 14px;
    padding-right: 14px;
  }

  .assistant-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .assistant-header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .assistant-context-strip {
    flex-direction: column;
    align-items: flex-start;
  }

  .assistant-footer-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .send-btn {
    width: 100%;
  }

  .typing-bubble-wrap {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
}
</style>