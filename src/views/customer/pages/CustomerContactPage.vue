<script setup>
import { reactive } from "vue"
import CustomerPageLayout from "../../../components/customer/CustomerPageLayout.vue"

const form = reactive({
  name: "",
  phone: "",
  email: "",
  message: ""
})

const submitForm = () => {
  const payload = { ...form }
  window.toast?.success?.("Đã gửi liên hệ. Cảm ơn bạn!")
  form.name = ""
  form.phone = ""
  form.email = ""
  form.message = ""
  return payload
}
</script>

<template>
  <CustomerPageLayout breadcrumb="Liên hệ">
      <section class="cp-grid">
        <article class="cp-card">
          <h1>Liên hệ với DirtyWave</h1>
          <p>Đội ngũ hỗ trợ luôn sẵn sàng tư vấn size, form và tình trạng đơn hàng.</p>

          <div class="cp-info">
            <div>
              <strong>Hotline</strong>
              <span>1900 6789</span>
            </div>
            <div>
              <strong>Email</strong>
              <span>support@dirtywave.vn</span>
            </div>
            <div>
              <strong>Địa chỉ</strong>
              <span>Nam Từ Liêm, Hà Nội</span>
            </div>
          </div>
        </article>

        <form class="cp-form" @submit.prevent="submitForm">
          <label>
            Họ tên
            <input v-model="form.name" type="text" required />
          </label>

          <label>
            Số điện thoại
            <input v-model="form.phone" type="text" required />
          </label>

          <label>
            Email
            <input v-model="form.email" type="email" required />
          </label>

          <label>
            Nội dung
            <textarea v-model="form.message" rows="5" required></textarea>
          </label>

          <button type="submit">Gửi liên hệ</button>
        </form>
      </section>
  </CustomerPageLayout>
</template>

<style scoped>
.cp-grid {
  display: grid;
  grid-template-columns: 0.95fr 1.05fr;
  gap: 14px;
}

.cp-card,
.cp-form {
  border: 1px solid #e8d8db;
  border-radius: 16px;
  background: #fff;
  padding: 18px;
}

.cp-card h1 {
  margin: 0;
  color: #161214;
  font-size: 30px;
}

.cp-card p {
  margin: 10px 0 0;
  color: #5d585c;
}

.cp-info {
  margin-top: 16px;
  display: grid;
  gap: 10px;
}

.cp-info div {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff5f7;
}

.cp-info strong {
  color: #8f1121;
}

.cp-info span {
  color: #161214;
}

.cp-form {
  display: grid;
  gap: 10px;
}

.cp-form label {
  display: grid;
  gap: 6px;
  color: #4a4549;
  font-size: 14px;
}

.cp-form input,
.cp-form textarea {
  border: 1px solid #e8d8db;
  border-radius: 10px;
  padding: 10px 12px;
  font: inherit;
}

.cp-form button {
  margin-top: 4px;
  height: 42px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #c5162d 0%, #8f1121 100%);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 900px) {
  .cp-grid {
    grid-template-columns: 1fr;
  }
}
</style>
