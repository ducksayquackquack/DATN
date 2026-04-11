<template>
  <div class="assistant-demo-page">
    <div class="demo-left">
      <h1>Internal AI Assistant Demo</h1>
      <p>Đây là khung demo trợ lý nội bộ cho DirtyWave.</p>

      <div class="role-switch">
        <button
          :class="['role-btn', role === 'EMPLOYEE' ? 'active' : '']"
          @click="role = 'EMPLOYEE'"
        >
          EMPLOYEE
        </button>
        <button
          :class="['role-btn', role === 'ADMIN' ? 'active' : '']"
          @click="role = 'ADMIN'"
        >
          ADMIN
        </button>
      </div>

      <div class="demo-note">
        <h3>Demo gợi ý</h3>
        <ul>
          <li>Tìm sản phẩm hoodie</li>
          <li>SP001 còn hàng không?</li>
          <li>Đơn HD202401001 đang ở trạng thái gì?</li>
          <li>Doanh thu tháng này là bao nhiêu?</li>
          <li>Top 5 sản phẩm bán chạy</li>
          <li>Nhân viên nào có lịch làm gần nhất?</li>
        </ul>
      </div>

      <div class="demo-tip">
        Kéo nút <strong>AI Demo</strong> lên xuống, hoặc rê sang nửa trái / nửa phải màn hình để nó bám vào lề gần nhất.
      </div>
    </div>

    <div class="demo-right">
      <div class="demo-right-card">
        <div class="demo-right-title">Chế độ hiển thị</div>
        <div class="demo-right-text">
          Assistant hiện ở dạng nút nổi draggable. Bấm vào nút để mở / đóng panel.
        </div>
      </div>
    </div>

    <FloatingAssistantWidget
      :role="role"
      :source="role === 'ADMIN' ? 'ADMIN_PANEL' : 'EMPLOYEE_PANEL'"
      :context="assistantContext"
    />
  </div>
</template>

<script>
import FloatingAssistantWidget from "@/components/assistant/FloatingAssistantWidget.vue";

export default {
  name: "InternalAssistantDemo",
  components: {
    FloatingAssistantWidget,
  },
  data() {
    return {
      role: "EMPLOYEE",
    };
  },
  computed: {
    assistantContext() {
      if (this.role === "ADMIN") {
        return {
          pageType: "REVENUE_DASHBOARD",
          route: "/assistant/demo-admin",
        };
      }

      return {
        pageType: "CUSTOMER_CHAT",
        route: "/assistant/demo-employee",
      };
    },
  },
};
</script>

<style scoped>
.assistant-demo-page {
  min-height: 100vh;
  display: flex;
  gap: 24px;
  padding: 24px;
  background: #f8fafc;
}

.demo-left {
  flex: 1;
  max-width: 760px;
}

.demo-right {
  width: 320px;
}

.demo-left h1 {
  margin-bottom: 10px;
  font-size: 28px;
  color: #0f172a;
}

.demo-left p {
  margin-top: 0;
  color: #475569;
}

.role-switch {
  display: flex;
  gap: 10px;
  margin: 20px 0;
}

.role-btn {
  border: 1px solid #d1d5db;
  background: white;
  padding: 10px 18px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
}

.role-btn.active {
  background: #111827;
  color: white;
  border-color: #111827;
}

.demo-note,
.demo-right-card,
.demo-tip {
  background: white;
  border-radius: 14px;
  padding: 18px;
  border: 1px solid #e5e7eb;
}

.demo-note h3,
.demo-right-title {
  margin-top: 0;
  color: #0f172a;
}

.demo-note ul {
  padding-left: 18px;
  line-height: 1.8;
  color: #334155;
}

.demo-right-title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 8px;
}

.demo-right-text {
  color: #475569;
  line-height: 1.6;
}

.demo-tip {
  margin-top: 18px;
  color: #334155;
  line-height: 1.6;
  background: #eff6ff;
  border-color: #bfdbfe;
}

@media (max-width: 900px) {
  .assistant-demo-page {
    flex-direction: column;
  }

  .demo-right {
    width: 100%;
  }
}
</style>