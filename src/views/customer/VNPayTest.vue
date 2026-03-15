<script setup>
import { ref } from "vue"
import { createVnpayPayment } from "../../services/vnpayService"

const amount = ref(100000)
const orderId = ref(`TEST-${Date.now()}`)
const loading = ref(false)
const error = ref("")
const lastUrl = ref("")

const startPayment = async () => {
  error.value = ""
  lastUrl.value = ""

  if (!Number(amount.value) || Number(amount.value) <= 0) {
    error.value = "So tien phai lon hon 0"
    return
  }

  try {
    loading.value = true
    const res = await createVnpayPayment({
      amount: Number(amount.value),
      orderId: orderId.value
    })

    const url = res?.data?.url
    if (!url) {
      error.value = "Khong tao duoc URL VNPay"
      return
    }

    lastUrl.value = url
    window.location.href = url
  } catch (e) {
    error.value = e?.response?.data?.message || "Loi tao thanh toan VNPay"
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="test-wrap">
    <div class="test-card">
      <h1>VNPay Test</h1>
      <p>Trang nay de test rieng VNPay truoc khi dung checkout that.</p>

      <label>Order ID</label>
      <input v-model="orderId" type="text" />

      <label>So tien (VND)</label>
      <input v-model.number="amount" type="number" min="1000" step="1000" />

      <button class="btn" :disabled="loading" @click="startPayment">
        {{ loading ? "Dang tao URL..." : "Test thanh toan VNPay" }}
      </button>

      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="lastUrl" class="url">{{ lastUrl }}</p>
    </div>
  </section>
</template>

<style scoped>
.test-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #f8fafc;
}

.test-card {
  width: min(560px, 100%);
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 24px;
  display: grid;
  gap: 10px;
}

label {
  font-weight: 600;
  color: #1e293b;
}

input {
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 10px 12px;
}

.btn {
  margin-top: 8px;
  border: 1px solid #1d4ed8;
  background: #1d4ed8;
  color: #fff;
  border-radius: 10px;
  padding: 11px 14px;
  cursor: pointer;
}

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.error {
  color: #b91c1c;
  margin: 4px 0 0;
}

.url {
  color: #0f172a;
  font-size: 12px;
  word-break: break-all;
}
</style>
