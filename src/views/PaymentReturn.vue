<script setup>
import { useRoute, useRouter } from "vue-router"
import { onMounted, ref } from "vue"

const route = useRoute()
const router = useRouter()

const responseCode = ref("")

onMounted(async () => {

  responseCode.value = route.query.vnp_ResponseCode || ""

  if (responseCode.value === "00") {

    const order = JSON.parse(localStorage.getItem("currentOrder"))

    if (!order) return

    // Tránh tạo hóa đơn nhiều lần khi refresh - dùng orderId làm key
    const invoiceKey = `invoice_${order.id}`
    const created = localStorage.getItem(invoiceKey)

    if (created) return

    try {
      // Lưu hóa đơn vào localStorage (giống như thanh toán tiền mặt)
      const invoices = JSON.parse(localStorage.getItem("hoaDonList")) || []
      
      const invoice = {
        id: "HD" + Date.now(),
        maHoaDon: "HD" + Date.now(),
        tenKhachHang: order.customer.name,
        soDienThoaiNhanHang: order.customer.phone,
        diaChiNhanHang: order.customer.address,
        thanhTien: order.total,
        phuongThucThanhToan: "VNPAY",
        trangThai: "CHUA_XAC_NHAN",
        ngayTao: new Date().toISOString(),
        chiTietHoaDon: order.items
      }
      
      invoices.unshift(invoice)
      localStorage.setItem("hoaDonList", JSON.stringify(invoices))

      console.log("Hóa đơn VNPay đã lưu vào localStorage:", invoice)

      // Đánh dấu đơn hàng này đã tạo hóa đơn
      localStorage.setItem(invoiceKey, "true")

      localStorage.removeItem("cart")
      localStorage.removeItem("currentOrder")
      
    } catch (error) {
      console.error("Lỗi tạo hóa đơn:", error)
      // Vẫn xóa cart và order để tránh tạo lại
      localStorage.removeItem("cart")
      localStorage.removeItem("currentOrder")
      localStorage.setItem(invoiceKey, "true")
    }
  }

})
</script>

<template>

<div class="result">

<h1 v-if="responseCode === '00'" class="success">
Thanh toán thành công 🎉
</h1>

<h1 v-else class="fail">
Thanh toán thất bại ❌
</h1>

<button @click="router.push('/home')" class="btn">
Về trang chủ
</button>

</div>

</template>

<style scoped>

.result{
text-align:center;
margin-top:120px;
}

.success{
color:#16a34a;
}

.fail{
color:#dc2626;
}

.btn{
margin-top:20px;
padding:12px 30px;
background:#1a3a52;
color:white;
border:none;
border-radius:6px;
cursor:pointer;
}

</style>