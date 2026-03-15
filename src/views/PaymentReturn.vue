<script setup>
import { computed, onMounted, ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import { createBackendCheckoutOrder } from "../services/checkoutOrderService"
import { useToast } from "../composables/useToast"
import { appendPaymentFlowTag, PAYMENT_FLOW_TAGS } from "../utils/paymentWorkflow"

const route = useRoute()
const router = useRouter()
const toast = useToast()
const confirming = ref(false)
const invoiceCode = ref("")
const localConfirmed = ref(false)

const responseCode = computed(() => String(route.query.vnp_ResponseCode || ""))
const transactionStatus = computed(() => String(route.query.vnp_TransactionStatus || ""))
const txnRef = computed(() => String(route.query.vnp_TxnRef || "").trim())
const isSuccess = computed(() => {
  if (responseCode.value !== "00") return false
  return !transactionStatus.value || transactionStatus.value === "00"
})

const orderFromStorage = computed(() => {
  try {
    return JSON.parse(localStorage.getItem("currentOrder") || "null")
  } catch {
    return null
  }
})

const hasOrderContext = computed(() => Boolean(orderFromStorage.value?.id))
const markKey = computed(() => {
  return orderFromStorage.value?.id ? `invoice_${orderFromStorage.value.id}` : ""
})
const txnMarkKey = computed(() => {
  const ref = txnRef.value
  return ref ? `invoice_txn_${ref}` : ""
})
const isAlreadyConfirmed = computed(() => {
  if (localConfirmed.value) return true
  const byOrder = markKey.value ? Boolean(localStorage.getItem(markKey.value)) : false
  const byTxn = txnMarkKey.value ? Boolean(localStorage.getItem(txnMarkKey.value)) : false
  return byOrder || byTxn
})

const extractErrorMessage = (error) => {
  const payload = error?.response?.data
  if (typeof payload === "string" && payload.trim()) return payload
  if (typeof payload?.detail === "string" && payload.detail.trim()) return payload.detail
  if (typeof payload?.title === "string" && payload.title.trim()) return payload.title
  if (typeof payload?.message === "string" && payload.message.trim()) return payload.message
  if (typeof payload?.error === "string" && payload.error.trim()) return payload.error
  if (typeof error?.message === "string" && error.message.trim()) return error.message
  return "Đã thanh toán nhưng chưa lưu được hóa đơn vào hệ thống"
}

if (responseCode.value) {
  localStorage.setItem(
    "vnpay:lastReturn",
    JSON.stringify({
      at: new Date().toISOString(),
      query: route.query,
      success: isSuccess.value
    })
  )
}

const confirmPayment = async () => {
  if (confirming.value) return

  if (!isSuccess.value) {
    toast.error("Giao dịch VNPay chưa thành công, không thể xác nhận")
    return
  }

  if (!hasOrderContext.value) {
    toast.error("Không còn dữ liệu đơn hàng để xác nhận")
    return
  }

  if (isAlreadyConfirmed.value) {
    toast.info("Đơn này đã được gửi xác nhận thanh toán trước đó")
    return
  }

  confirming.value = true
  try {
    const order = orderFromStorage.value
    const statusNote = appendPaymentFlowTag(
      order?.voucher?.code
        ? `Đơn hàng VNPay chờ nhân viên xác nhận - Voucher ${order.voucher.code}`
        : "Đơn hàng VNPay chờ nhân viên xác nhận",
      PAYMENT_FLOW_TAGS.VN_PAY_CUSTOMER_CONFIRMED,
      "Khach hang da bam Xac nhan thanh toan."
    )

    const created = await createBackendCheckoutOrder({
      cartItems: Array.isArray(order.items) ? order.items : [],
      delivery: order.delivery || {},
      shipping: Number(order.shipping || 0),
      discount: Number(order.discount || order.voucher?.value || 0),
      total: Number(order.total || 0),
      paymentMethod: "VNPAY",
      voucherCode: order?.voucher?.code || "",
      statusNote
    })

    const createdCode = created.order?.maHoaDon || String(created.orderId)
    invoiceCode.value = createdCode

    if (markKey.value) {
      localStorage.setItem(markKey.value, String(created.orderId))
    }
    if (txnMarkKey.value) {
      localStorage.setItem(txnMarkKey.value, String(created.orderId))
    }
    localConfirmed.value = true
    localStorage.setItem(
      "vnpay:lastCustomerConfirm",
      JSON.stringify({
        at: new Date().toISOString(),
        invoiceId: created.orderId,
        invoiceCode: createdCode
      })
    )

    localStorage.removeItem("cart")
    localStorage.removeItem("checkoutCart")

    toast.success(`Đã gửi yêu cầu xác nhận thanh toán. Mã hóa đơn: ${createdCode}`)
  } catch (error) {
    console.error("Persist VNPay order failed:", error)
    toast.error(extractErrorMessage(error))
  } finally {
    confirming.value = false
  }
}

const goHome = () => router.push("/home")
const goTest = () => router.push("/vnpay-test")

onMounted(() => {
  if (isSuccess.value && hasOrderContext.value && !isAlreadyConfirmed.value) {
    confirmPayment()
  }
})
</script>

<template>
  <div class="result-wrap">
    <div class="result-card">
      <h1 :class="isSuccess ? 'success' : 'fail'">
        {{ isSuccess ? "Thanh toán VNPay thành công" : "Thanh toán VNPay thất bại" }}
      </h1>

      <p class="meta">Mã phản hồi: <b>{{ responseCode || "N/A" }}</b></p>
      <p class="meta">Trạng thái giao dịch: <b>{{ transactionStatus || "N/A" }}</b></p>

      <p v-if="isSuccess && !isAlreadyConfirmed" class="hint">
        Giao dịch đã hoàn tất trên VNPay. Vui lòng bấm <b>Xác nhận thanh toán</b> để gửi yêu cầu cho nhân viên kiểm tra tiền vào tài khoản shop.
      </p>
      <p v-else-if="isSuccess && isAlreadyConfirmed" class="hint success-text">
        Bạn đã gửi xác nhận thanh toán thành công{{ invoiceCode ? ` cho hóa đơn ${invoiceCode}` : "" }}. Nhân viên sẽ xử lý và phản hồi trong mục lịch sử mua hàng.
      </p>
      <p v-else class="hint fail-text">
        Thanh toán chưa thành công, bạn có thể thử lại.
      </p>

      <div class="actions">
        <button
          v-if="isSuccess"
          class="btn success"
          :disabled="confirming || isAlreadyConfirmed || !hasOrderContext"
          @click="confirmPayment"
        >
          {{ confirming ? "Đang gửi xác nhận..." : (isAlreadyConfirmed ? "Đã gửi xác nhận" : "Xác nhận thanh toán") }}
        </button>
        <button class="btn primary" @click="goHome">Về trang chủ</button>
        <button class="btn" @click="goTest">Thử lại VNPay test</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.result-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: #f8fafc;
  padding: 24px;
}

.result-card {
  width: min(640px, 100%);
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 28px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}

.success {
  color: #15803d;
  margin: 0 0 14px;
}

.fail {
  color: #b91c1c;
  margin: 0 0 14px;
}

.meta {
  margin: 8px 0;
  color: #334155;
}

.hint {
  margin: 12px 0 0;
  color: #334155;
  line-height: 1.5;
}

.success-text {
  color: #166534;
}

.fail-text {
  color: #b91c1c;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.btn {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #0f172a;
  padding: 10px 14px;
  border-radius: 10px;
  cursor: pointer;
}

.btn.primary {
  border-color: #1d4ed8;
  background: #1d4ed8;
  color: #fff;
}

.btn.success {
  border-color: #15803d;
  background: #15803d;
  color: #fff;
}

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
</style>
