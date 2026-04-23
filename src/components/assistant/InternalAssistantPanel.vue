<template>
  <div class="assistant-panel">
    <header class="assistant-header">
      <div class="assistant-header-main">
        <div class="assistant-avatar">AI</div>
        <div class="assistant-header-copy">
          <h3>Trợ lý bán hàng</h3>
          <div class="assistant-subtitle">
            {{ assistantSubtitle }}
          </div>

          <div class="assistant-meta-row">
            <span class="assistant-badge role">{{ roleLabel }}</span>
            <span class="assistant-badge context">{{ contextLabel }}</span>
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
        <div class="strip-label">Khu vực đang hỗ trợ</div>
        <div class="strip-value">{{ contextDescription }}</div>
      </div>

      <div class="status-pill" :class="{ live: !loading, busy: loading }">
        {{ loading ? "Đang xử lý" : "Sẵn sàng hỗ trợ" }}
      </div>
    </section>

    <section v-if="apiIssue" class="assistant-issue-banner">
      <span>{{ apiIssue }}</span>
    </section>

    <section v-if="showQuickActions" class="quick-actions-block">
      <div class="quick-actions-head">
        <div class="quick-actions-title">Gợi ý nhanh</div>
        <div class="quick-actions-subtitle">
          Chọn một thao tác thường dùng để bắt đầu nhanh hơn
        </div>
      </div>

      <div class="quick-actions">
        <button
          v-for="item in displayedQuickActions"
          :key="suggestedActionKey(item)"
          class="quick-btn"
          @click="sendQuickAction(item)"
        >
          {{ suggestedActionLabel(item) }}
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
        :key="msg.id || `${msg.sender}-${index}`"
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
              v-if="msg.sender === 'assistant' && msg.requiresConfirmation"
              class="assistant-card action-card"
            >
              <div class="card-title">Xác nhận thao tác</div>
              <div class="card-text">
                {{ msg.confirmationMessage || "Vui lòng xác nhận để tiếp tục xử lý." }}
              </div>
              <div class="card-meta-row">
                <span v-if="msg.actionStatus" class="tiny-badge status">{{ displayActionStatus(msg.actionStatus) }}</span>
              </div>
              <div class="action-buttons">
                <button
                  class="inline-action primary"
                  :disabled="loading || !msg.pendingActionToken"
                  @click="confirmPendingAction(msg)"
                >
                  Xác nhận
                </button>
                <button
                  class="inline-action"
                  :disabled="loading || !msg.pendingActionToken"
                  @click="cancelPendingAction(msg)"
                >
                  Hủy
                </button>
              </div>
            </div>

            <div
              v-if="msg.sender === 'assistant' && msg.missingSlots && msg.missingSlots.length"
              class="assistant-card slot-card"
            >
              <div class="card-title">Cần bổ sung thông tin</div>
              <div class="card-text">Điền nhanh các trường còn thiếu để assistant tiếp tục xử lý.</div>
              <div class="missing-slot-list">
                <div v-for="slot in msg.missingSlots" :key="`${msg.id || index}-${slot}`" class="slot-field">
                  <label :for="`slot-${index}-${slot}`">{{ slotLabel(slot) }}</label>
                  <input
                    v-if="slotInputType(slot) !== 'textarea'"
                    :id="`slot-${index}-${slot}`"
                    v-model="slotDrafts[msg.id || index][slot]"
                    :type="slotInputType(slot)"
                    :placeholder="slotPlaceholder(slot)"
                    class="slot-input"
                  />
                  <textarea
                    v-else
                    :id="`slot-${index}-${slot}`"
                    v-model="slotDrafts[msg.id || index][slot]"
                    rows="2"
                    :placeholder="slotPlaceholder(slot)"
                    class="slot-input slot-textarea"
                  ></textarea>
                </div>
              </div>
              <div class="action-buttons">
                <button class="inline-action primary" :disabled="loading" @click="submitMissingSlots(msg, index)">
                  Gửi thông tin
                </button>
              </div>
            </div>

            <div
              v-if="msg.sender === 'assistant' && msg.warnings && msg.warnings.length"
              class="assistant-card warning-card"
            >
              <div class="card-title">Lưu ý</div>
              <ul class="warning-list">
                <li v-for="warning in msg.warnings" :key="warning">{{ warning }}</li>
              </ul>
            </div>

            <div
              v-if="msg.sender === 'assistant' && msg.suggestedActions && msg.suggestedActions.length"
              class="assistant-card suggested-card"
            >
              <div class="card-title">Thao tác đề xuất</div>
              <div class="suggested-chip-list">
                <button
                  v-for="action in msg.suggestedActions.slice(0, 6)"
                  :key="`${msg.id || index}-${suggestedActionKey(action)}`"
                  class="chip-btn"
                  @click="useSuggestedAction(action, msg)"
                >
                  {{ suggestedActionLabel(action) }}
                </button>
              </div>
            </div>
          </div>

          <div v-if="msg.sender === 'assistant' && !loading && msg.text" class="message-actions">
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
          {{ assistantHint }}
        </div>

        <button
          class="send-btn"
          :disabled="loading || !inputMessage.trim()"
          @click="handleSend"
        >
          {{ loading ? "Đang gửi..." : pendingActionState ? "Gửi / xác nhận" : "Gửi" }}
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
      pendingActionState: null,
      slotDrafts: {},
      showDebug: false,
      apiIssue: "",
      lastAuthEventAt: 0,
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
      return this.role === "ADMIN" ? "Quản trị viên" : "Nhân viên bán hàng";
    },
    contextLabel() {
      return PAGE_LABELS[this.resolvedPageType] || "Trợ lý bán hàng";
    },
    contextDescription() {
      return PAGE_LABELS[this.resolvedPageType] || "Trợ lý bán hàng";
    },
    assistantSubtitle() {
      if (this.role === "ADMIN") {
        return "Hỗ trợ tra cứu đơn hàng, khách hàng, tồn kho và thao tác bán hàng hằng ngày.";
      }
      return "Hỗ trợ tra cứu đơn hàng, khách hàng, tồn kho và tạo đơn nhanh hơn.";
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
        return "Ví dụ: Doanh thu hôm nay thế nào, có đơn trễ hay tồn thấp nào cần xử lý?";
      }
      if (pageType === "POS") {
        return "Ví dụ: SP001 còn hàng không, tạo đơn mới cho khách 0912345678, gợi ý 3 sản phẩm dễ chốt sale";
      }
      if (pageType === "ORDER_DETAIL") {
        return "Ví dụ: Đơn này đang ở trạng thái gì, cập nhật trạng thái sang đang giao";
      }
      return "Ví dụ: Đơn HD202401001 đang ở trạng thái gì, doanh thu hôm nay là bao nhiêu, hoặc cho tôi xem đơn chưa giao ở Hà Nội";
    },
    loadingText() {
      const pageType = this.resolvedPageType;
      if (pageType === "CUSTOMER_CHAT") return "Assistant đang soạn gợi ý trả lời khách...";
      if (pageType === "REVENUE_DASHBOARD") return "Assistant đang tổng hợp doanh thu, đơn trễ và cảnh báo...";
      if (pageType === "POS") return "Assistant đang kiểm tra sản phẩm, tồn kho và thao tác bán hàng...";
      if (pageType === "ORDER_DETAIL") return "Assistant đang tra cứu và chuẩn bị thao tác với đơn hàng...";
      return "Assistant đang đọc ngữ cảnh và tìm dữ liệu phù hợp...";
    },
    emptyStateTitle() {
      return "Trợ lý AI sẵn sàng hỗ trợ";
    },
    emptyStateDescription() {
      if (this.isCustomerChatContext) {
        return "Anh/chị có thể nhờ viết câu trả lời cho khách, tra lịch sử mua hàng, tìm voucher phù hợp hoặc kiểm tra nhanh trạng thái đơn.";
      }
      if (this.role === "ADMIN") {
        return "Anh/chị có thể hỏi về doanh thu, đơn trễ, tồn thấp, hồ sơ khách hàng, sản phẩm hoặc trạng thái vận hành.";
      }
      return "Anh/chị có thể hỏi về đơn hàng, sản phẩm, tồn kho, khách hàng, tạo đơn mới hoặc nhờ assistant soạn câu trả lời gửi khách.";
    },
    assistantHint() {
      if (this.pendingActionState?.token) {
        return "Có thao tác đang chờ xác nhận. Anh/chị có thể bấm nút Xác nhận/Hủy hoặc trả lời tiếp trong chat.";
      }
      if (this.isCustomerChatContext) {
        return "Có thể chèn trực tiếp câu trả lời vào ô chat khách, hoặc dùng các chip gợi ý để thao tác nhanh.";
      }
      if (this.role === "ADMIN") {
        return "Anh/chị có thể dùng trợ lý để tra cứu nhanh, chuẩn bị đơn nháp và chuyển sang đúng màn hình nghiệp vụ.";
      }
      return "Nhân viên nên hỏi ngắn, rõ intent: đơn hàng, khách hàng, tồn kho, tạo đơn hoặc câu trả lời cho khách.";
    },
    defaultQuickActions() {
      const pageType = this.resolvedPageType;
      const hasCustomerMessage = !!this.context?.lastCustomerMessage;

      if (pageType === "CUSTOMER_CHAT") {
        return hasCustomerMessage
          ? [
              "Khách này từng mua chưa?",
              "Viết giúp tôi câu trả lời cho khách",
              "Viết mềm hơn và lịch sự hơn",
              "Rút gọn câu trả lời",
              "Có voucher nào áp được không?",
              "Gợi ý 3 sản phẩm hoodie dễ chốt sale",
            ]
          : [
              "Khách này từng mua chưa?",
              "Tìm sản phẩm hoodie",
              "SP001 còn hàng không?",
              "Đơn này đang ở trạng thái gì?",
              "Gợi ý 3 sản phẩm hoodie dễ chốt sale",
            ];
      }

      if (pageType === "REVENUE_DASHBOARD") {
        return this.role === "ADMIN"
          ? [
              "Doanh thu hôm nay là bao nhiêu?",
              "Đơn trễ cần xử lý",
              "Tồn thấp",
              "Có thanh toán thất bại nào bất thường không?",
            ]
          : [
              "Đơn HD202401001 đang ở trạng thái gì?",
              "Khách này từng mua chưa?",
              "SP001 còn hàng không?",
              "Viết giúp tôi câu trả lời cho khách",
            ];
      }

      if (pageType === "POS") {
        return [
          "SP001 còn hàng không?",
          "Tìm sản phẩm hoodie",
          "Tạo đơn mới cho khách 0912345678",
          "Gợi ý 3 sản phẩm hoodie dễ chốt sale",
        ];
      }

      if (pageType === "ORDER_DETAIL") {
        return [
          "Đơn này đang ở trạng thái gì?",
          "Tóm tắt nhanh đơn hàng này",
          "Cập nhật trạng thái sang đang giao",
          "Viết giúp tôi câu trả lời cho khách về đơn này",
        ];
      }

      if (this.role === "ADMIN") {
        return [
          "Doanh thu hôm nay là bao nhiêu?",
          "Đơn trễ cần xử lý",
          "Tồn thấp",
        ];
      }

      return [
        "Khách này từng mua chưa?",
        "SP001 còn hàng không?",
        "Đơn HD202401001 đang ở trạng thái gì?",
        "Cho tôi xem các đơn chưa giao ở Hà Nội",
      ];
    },
    displayedQuickActions() {
      const actions = this.recommendedActions?.length ? this.recommendedActions : this.defaultQuickActions;
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
        id: this.newMessageId(),
        sender: "assistant",
        text:
          this.role === "ADMIN"
            ? "Xin chào anh/chị Admin. Em sẵn sàng hỗ trợ doanh thu, cảnh báo vận hành, voucher, sản phẩm và các thao tác cần xác nhận."
            : "Xin chào anh/chị. Em có thể hỗ trợ tìm sản phẩm, kiểm tra tồn kho, tra cứu đơn hàng, tạo đơn nháp và gợi ý trả lời khách.",
        toolCalls: [],
        createdAt: new Date().toISOString(),
        suggestedActions: this.defaultQuickActions,
      };
    },
    newMessageId() {
      return `msg_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
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
    formatDebug(debug) {
      if (!debug) return "";
      try {
        return typeof debug === "string" ? debug : JSON.stringify(debug, null, 2);
      } catch (error) {
        return String(debug);
      }
    },
    showDebugFor(msg) {
      return false;
    },
    toggleToolDetails(index) {
      this.showToolDetailsMap = {
        ...this.showToolDetailsMap,
        [index]: !this.showToolDetailsMap[index],
      };
    },
    slotLabel(slot) {
      const map = {
        customerPhone: "Số điện thoại khách",
        shippingAddress: "Địa chỉ giao hàng",
        quantity: "Số lượng",
        targetStatus: "Trạng thái cần cập nhật",
        productHint: "Mã hoặc gợi ý sản phẩm",
        productName: "Tên sản phẩm",
        orderCode: "Mã đơn hàng",
      };
      return map[slot] || slot;
    },
    slotPlaceholder(slot) {
      const map = {
        customerPhone: "Ví dụ: 0912345678",
        shippingAddress: "Ví dụ: 12 Nguyễn Trãi, Hà Nội",
        quantity: "Ví dụ: 2",
        targetStatus: "Ví dụ: dang giao",
        productHint: "Ví dụ: SP001 hoặc áo sơ mi trắng size M",
        productName: "Ví dụ: Áo sơ mi trắng",
        orderCode: "Ví dụ: HD202401001",
      };
      return map[slot] || `Nhập ${this.slotLabel(slot).toLowerCase()}`;
    },
    slotInputType(slot) {
      if (slot === "quantity") return "number";
      if (slot === "shippingAddress") return "textarea";
      return "text";
    },
    ensureSlotDraft(messageKey, slots = []) {
      const draft = { ...(this.slotDrafts[messageKey] || {}) };
      slots.forEach((slot) => {
        if (draft[slot] == null) {
          draft[slot] = "";
        }
      });
      this.slotDrafts = {
        ...this.slotDrafts,
        [messageKey]: draft,
      };
    },
    getStorageKey() {
      const pageType = this.resolvedPageType || "GENERAL_INTERNAL";
      const route = this.context?.route || "unknown-route";
      const role = this.role || "EMPLOYEE";
      const sessionScopedKey =
        this.context?.sessionCode || this.context?.sessionId || this.context?.orderCode || "global";
      return `dirtywave_ai_history:${role}:${pageType}:${route}:${sessionScopedKey}`;
    },
    loadHistoryFromStorage() {
      try {
        const raw = localStorage.getItem(this.getStorageKey());
        if (!raw) {
          this.sessionCode = null;
          this.pendingActionState = null;
          this.messages = [this.buildGreetingMessage()];
          return;
        }

        const parsed = JSON.parse(raw);
        const savedMessages = Array.isArray(parsed?.messages) ? parsed.messages : [];
        this.messages = savedMessages.length ? savedMessages : [this.buildGreetingMessage()];
        this.sessionCode = parsed?.sessionCode || null;
        this.pendingActionState = parsed?.pendingActionState || null;
        this.recommendedActions = Array.isArray(parsed?.recommendedActions) ? parsed.recommendedActions : [];
        this.messages.forEach((msg, index) => {
          if (msg?.missingSlots?.length) {
            this.ensureSlotDraft(msg.id || index, msg.missingSlots);
          }
        });
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
            pendingActionState: this.pendingActionState || null,
            recommendedActions: this.recommendedActions || [],
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
      this.pendingActionState = null;
      this.slotDrafts = {};
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
      this.inputMessage = typeof text === "string" ? text : this.suggestedActionLabel(text);
      await this.handleSend();
    },
    suggestedActionLabel(action) {
      if (typeof action === "string") return action;
      return this.formatSuggestedActionLabel(action);
    },
    displayActionStatus(status) {
      const code = String(status || "").toUpperCase();
      const map = {
        HANDOFF_REQUIRED: "Cần mở màn hình xử lý",
        CONFIRMED: "Đã xác nhận",
        CANCELLED: "Đã hủy",
        EXECUTED: "Đã xử lý",
        DONE: "Hoàn tất",
        FAILED: "Chưa thể xử lý"
      };
      return map[code] || "Đang xử lý";
    },
    suggestedActionKey(action) {
      if (typeof action === "string") return action;
      return `${action?.type || "action"}-${action?.label || "label"}`;
    },
    formatSuggestedActionLabel(action) {
      if (typeof action === "string") return action;
      const map = {
        open_order_workflow: "Mở workflow tạo đơn",
        cancel_draft: "Hủy đơn nháp",
        open_customer: "Mở hồ sơ khách",
        recent_orders: "Xem đơn gần đây",
        upsell: "Gợi ý mua thêm",
        cross_sell: "Gợi ý sản phẩm đi kèm",
        stock_guard: "Kiểm tra tồn kho",
        campaign: "Gợi ý đẩy bán nhanh",
        drill_down: "Xem chi tiết",
        export: "Xuất dữ liệu tổng hợp",
      };
      return map[action?.type] || action?.label || action?.type || "Gợi ý";
    },
    sanitizeSuggestedActions(actions, message = null) {
      const input = Array.isArray(actions) ? actions : [];
      const seen = new Set();
      const result = [];
      for (const action of input) {
        const label = this.suggestedActionLabel(action);
        const type = typeof action === "string" ? action : String(action?.type || "");
        if (!label) continue;
        if (["voucher_fit", "continue_previous", "export", "drill_down"].includes(type)) {
          continue;
        }
        const key = type || label;
        if (seen.has(key)) continue;
        seen.add(key);
        result.push(action);
      }
      return result.slice(0, 4);
    },
    extractDraftOrder(message) {
      if (!message) return null;
      if (message?.data?.draftOrder) return message.data.draftOrder;
      const text = String(message?.text || "");
      if (!/đơn nháp/i.test(text)) return null;

      const phoneMatch = text.match(/khách\s+(\d{9,11})/i);
      const itemMatch = text.match(/:\s*([a-z0-9_-]+)\s*x\s*(\d+)/i);
      const addressMatch = text.match(/giao tới\s+(.+?)(?:\.|$)/i);
      const sizeMatch = text.match(/size\s+([A-Za-z0-9]+)/i);

      return {
        customerPhone: phoneMatch?.[1] || "",
        shippingAddress: addressMatch?.[1]?.trim() || "",
        items: itemMatch ? [{
          productHint: itemMatch[1].toUpperCase(),
          quantity: Number(itemMatch[2] || 1),
          size: sizeMatch?.[1] || "",
        }] : [],
        sourceMessage: text,
      };
    },
    useSuggestedAction(action, msg = null) {
      const type = typeof action === "string" ? "" : (action?.type || "");
      if (type === "open_order_workflow") {
        const targetMsg = msg || [...this.messages].reverse().find((m) => m?.sender === "assistant" && this.extractDraftOrder(m));
        const draftOrder = this.extractDraftOrder(targetMsg);
        if (!draftOrder) {
          window.toast?.warning?.("Chưa tìm thấy dữ liệu đơn nháp để mở workflow.");
          return;
        }
        localStorage.setItem("assistantDraftOrder", JSON.stringify(draftOrder));
        const basePath = this.role === "ADMIN" ? "/admin" : "/employee";
        this.$router.push(`${basePath}/hoa-don/pos`);
        window.toast?.success?.("Đã mở workflow tạo đơn và nạp dữ liệu nháp.");
        return;
      }
      this.inputMessage = this.formatSuggestedActionLabel(action);
      this.scrollToBottom();
    },
    normalizeAssistantMessage(response) {
      const message = {
        id: this.newMessageId(),
        sender: "assistant",
        text: response?.message || "Không có phản hồi từ assistant.",
        toolCalls: response?.toolCalls || [],
        createdAt: new Date().toISOString(),
        requiresConfirmation: !!response?.requiresConfirmation,
        confirmationMessage: response?.confirmationMessage || "",
        pendingActionToken: response?.pendingActionToken || null,
        actionStatus: response?.actionStatus || null,
        missingSlots: Array.isArray(response?.missingSlots) ? response.missingSlots.filter(Boolean) : [],
        suggestedActions: [],
        warnings: Array.isArray(response?.warnings) ? response.warnings.filter(Boolean) : [],
        auditId: response?.auditId || null,
        debug: response?.debug || null,
        data: response?.data || null,
      };

      if (message.missingSlots.length) {
        this.ensureSlotDraft(message.id, message.missingSlots);
      }
      message.suggestedActions = this.sanitizeSuggestedActions(
        Array.isArray(response?.suggestedActions)
          ? response.suggestedActions.filter(Boolean)
          : Array.isArray(response?.quickReplies)
            ? response.quickReplies.filter(Boolean)
            : [],
        message
      );

      const draftOrder = this.extractDraftOrder(message);
      if (draftOrder && !message.data?.draftOrder) {
        message.data = {
          ...(message.data || {}),
          draftOrder,
        };
      }
      if (draftOrder) {
        const hasOpenWorkflow = (message.suggestedActions || []).some((a) => typeof a === "object" && a?.type === "open_order_workflow");
        if (!hasOpenWorkflow) {
          message.suggestedActions = this.sanitizeSuggestedActions([
            { type: "open_order_workflow", label: "Mở workflow tạo đơn", priority: "high" },
            ...(message.suggestedActions || []),
          ], message);
        }
      }
      return message;
    },
    async submitMissingSlots(msg, index) {
  const key = msg.id || index;
  const draft = this.slotDrafts[key] || {};

  const slotValues = {};
  (msg.missingSlots || []).forEach((slot) => {
    const rawValue = draft[slot];
    const value = typeof rawValue === "string" ? rawValue.trim() : rawValue;

    if (value !== undefined && value !== null && String(value).trim() !== "") {
      slotValues[slot] = slot === "quantity" ? Number(value) : value;
    }
  });

  if (!Object.keys(slotValues).length) {
    window.toast?.error?.("Anh/chị hãy nhập ít nhất một thông tin còn thiếu.");
    return;
  }

  this.messages.push({
    id: this.newMessageId(),
    sender: "user",
    text: Object.entries(slotValues)
      .map(([slot, value]) => `${this.slotLabel(slot)}: ${value}`)
      .join(", "),
    toolCalls: [],
    createdAt: new Date().toISOString(),
  });

  this.inputMessage = "";
  await this.handleSend({
    forcedMessage: "continue_pending_action",
    pendingActionToken: msg?.pendingActionToken || this.pendingActionState?.token || null,
    slotValues,
    skipUserEcho: true,
  });
},
async confirmPendingAction(msg) {
      if (!msg?.pendingActionToken) return;
      this.inputMessage = "xác nhận";
      await this.handleSend({
        forcedMessage: "xác nhận",
        pendingActionToken: msg.pendingActionToken,
      });
    },
    async cancelPendingAction(msg) {
      if (!msg?.pendingActionToken) return;
      this.inputMessage = "hủy";
      await this.handleSend({
        forcedMessage: "hủy",
        pendingActionToken: msg.pendingActionToken,
      });
    },
    async handleSend(options = {}) {
  const text = (options?.forcedMessage ?? this.inputMessage).trim();
  if (!text || this.loading) return;
  if (!options?.skipUserEcho) {
    this.messages.push({
      id: this.newMessageId(),
      sender: "user",
      text,
      toolCalls: [],
      createdAt: new Date().toISOString(),
    });
  }

  this.inputMessage = "";
  this.loading = true;
  this.scrollToBottom();

  try {
    this.apiIssue = "";

    const response = await assistantService.sendMessage({
      sessionCode: this.sessionCode || this.context?.sessionCode || null,
      pendingActionToken: options?.pendingActionToken || this.pendingActionState?.token || null,
      message: text,
      role: this.role,
      source: this.source,
      context: {
        ...(this.context || {}),
        slotValues: options?.slotValues || null,
      },
    });

    if (response?.sessionCode) {
      this.sessionCode = response.sessionCode;
    }

    const suggested = Array.isArray(response?.suggestedActions)
      ? response.suggestedActions.filter(Boolean)
      : Array.isArray(response?.quickReplies)
        ? response.quickReplies.filter(Boolean)
        : [];
    if (suggested.length) {
      this.recommendedActions = this.sanitizeSuggestedActions(suggested);
    }

    const assistantMsg = this.normalizeAssistantMessage(response);
    this.messages.push(assistantMsg);

    if (assistantMsg.requiresConfirmation && assistantMsg.pendingActionToken) {
      this.pendingActionState = {
        token: assistantMsg.pendingActionToken,
        confirmationMessage: assistantMsg.confirmationMessage,
        actionStatus: assistantMsg.actionStatus,
        auditId: assistantMsg.auditId,
      };
    } else if (["CONFIRMED", "CANCELLED", "EXECUTED", "FAILED", "DONE"].includes((assistantMsg.actionStatus || "").toUpperCase())) {
      this.pendingActionState = null;
    } else if (!assistantMsg.pendingActionToken && !assistantMsg.requiresConfirmation) {
      this.pendingActionState = null;
    }
  } catch (error) {
    const status = error?.response?.status;
    if (status === 401) {
      this.apiIssue = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để dùng trợ lý.";
      this.messages.push({
        id: this.newMessageId(),
        sender: "system",
        text: "Phiên đăng nhập nội bộ đã hết hạn hoặc chưa được thiết lập. Anh/chị vui lòng đăng nhập lại bằng tài khoản nhân viên/quản trị viên rồi thử lại.",
        toolCalls: [],
        createdAt: new Date().toISOString(),
      });
      const now = Date.now();
      if (now - this.lastAuthEventAt > 8000) {
        this.lastAuthEventAt = now;
        window.dispatchEvent(
          new CustomEvent("assistant:auth-required", {
            detail: {
              source: this.source,
              context: this.context || {},
            },
          })
        );
      }
    } else if (status === 403) {
      this.apiIssue = "Tài khoản hiện tại chưa có quyền dùng trợ lý ở màn hình này.";
      this.messages.push({
        id: this.newMessageId(),
        sender: "system",
        text: "Tài khoản hiện tại không có quyền dùng trợ lý nội bộ ở màn hình này.",
        toolCalls: [],
        createdAt: new Date().toISOString(),
      });
    } else {
      this.apiIssue = "Không thể kết nối trợ lý. Kiểm tra backend DATNAPI3 và thử lại.";
      this.messages.push({
        id: this.newMessageId(),
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
  min-height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  overflow: hidden;
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
.send-btn,
.chip-btn,
.footer-suggestion-btn {
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

.assistant-issue-banner {
  margin: 10px 16px 0;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid #fecaca;
  background: #fff1f2;
  color: #9f1239;
  font-size: 12px;
  font-weight: 600;
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

.quick-btn,
.chip-btn {
  padding: 9px 12px;
  border-radius: 999px;
  border: 1px solid #dbeafe;
  background: rgba(255, 255, 255, 0.92);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  transition: all 0.18s ease;
}

.quick-btn:hover,
.chip-btn:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 20px 20px;
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

.inline-action:disabled,
.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.assistant-card {
  margin-top: 12px;
  border-radius: 14px;
  padding: 12px;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.action-card {
  background: #eff6ff;
}

.slot-card {
  background: #f8fafc;
}

.warning-card {
  background: #fff7ed;
  border-color: rgba(251, 191, 36, 0.45);
}

.suggested-card {
  background: #f8fafc;
}

.debug-card {
  background: #0f172a;
  color: #e2e8f0;
  border-color: rgba(148, 163, 184, 0.25);
}

.card-title {
  font-size: 12px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 6px;
}

.debug-card .card-title {
  color: #f8fafc;
}

.card-text {
  font-size: 13px;
  line-height: 1.55;
  color: #334155;
}

.card-meta-row,
.action-buttons,
.suggested-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.tiny-badge {
  display: inline-flex;
  align-items: center;
  padding: 5px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #475569;
  font-size: 11px;
  font-weight: 700;
}

.tiny-badge.status {
  background: #dbeafe;
  color: #1d4ed8;
}

.missing-slot-list {
  display: grid;
  gap: 10px;
  margin-top: 10px;
}

.slot-field {
  display: grid;
  gap: 6px;
}

.slot-field label {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.slot-input {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 10px 12px;
  font-size: 13px;
  background: #fff;
  outline: none;
}

.slot-input:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.slot-textarea {
  resize: vertical;
}

.warning-list {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #9a3412;
  font-size: 13px;
  line-height: 1.55;
}

.debug-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.5;
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

.footer-suggestions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 0 18px 12px;
}

.footer-suggestion-btn {
  border: 1px solid rgba(59, 130, 246, 0.2);
  background: rgba(59, 130, 246, 0.08);
  color: #1d4ed8;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.18s ease;
}

.footer-suggestion-btn:hover {
  background: rgba(59, 130, 246, 0.14);
  border-color: rgba(59, 130, 246, 0.35);
}

.assistant-footer {
  flex-shrink: 0;
  padding: 14px 18px 16px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.94);
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

  .assistant-footer-actions,
  .typing-bubble-wrap {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .send-btn,
  .inline-action,
  .chip-btn {
    width: 100%;
  }
}
</style>
