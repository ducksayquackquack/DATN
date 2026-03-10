<script setup>
import { ref, computed } from "vue"
import { useRouter } from "vue-router"
import { createHoaDon } from "../../services/hoaDonService"
import axios from "axios"

const router = useRouter()

/* =============================
   LẤY DỮ LIỆU GIỎ HÀNG
============================= */

const rawCart = JSON.parse(localStorage.getItem("cart")) || {}
const products = JSON.parse(localStorage.getItem("products")) || []

const cart = ref(
  Object.entries(rawCart).map(([id, qty]) => {
    const p = products.find(x => x.id == id)

    return {
      id,
      name: p?.name || "Sản phẩm",
      price: p?.price || 0,
      quantity: qty,
      image: p?.img || ""
    }
  })
)

/* =============================
   THÔNG TIN KHÁCH HÀNG
============================= */

const customer = ref({
  name: "",
  phone: "",
  address: ""
})

/* =============================
   PHƯƠNG THỨC THANH TOÁN
============================= */

const paymentMethod = ref("TIEN_MAT")

/* =============================
   FORMAT TIỀN
============================= */

const VND = (n) => {
  return new Intl.NumberFormat("vi-VN").format(n) + "₫"
}

/* =============================
   TỔNG TIỀN
============================= */

const total = computed(() => {
  return cart.value.reduce((sum, item) => {
    return sum + item.price * item.quantity
  }, 0)
})

/* =============================
   TẠO HÓA ĐƠN
============================= */

const createInvoice = async (trangThai = "CHUA_XAC_NHAN") => {

  try {
    // TẠM THỜI: Chỉ lưu localStorage để test, bỏ qua backend
    const invoices = JSON.parse(localStorage.getItem("hoaDonList")) || []
    
    const invoice = {
      id: "HD" + Date.now(),
      maHoaDon: "HD" + Date.now(),
      tenKhachHang: customer.value.name,
      soDienThoaiNhanHang: customer.value.phone,
      diaChiNhanHang: customer.value.address,
      thanhTien: total.value,
      phuongThucThanhToan: paymentMethod.value,
      trangThai: trangThai,
      ngayTao: new Date().toISOString(),
      chiTietHoaDon: cart.value
    }
    
    invoices.unshift(invoice)
    localStorage.setItem("hoaDonList", JSON.stringify(invoices))
    
    console.log("Hóa đơn đã lưu vào localStorage:", invoice)
    
    return invoice

  } catch (error) {
    console.error("Lỗi:", error)
    throw error
  }
}

/* =============================
   ĐẶT HÀNG
============================= */

const submitOrder = async () => {

  if (!customer.value.name || !customer.value.phone || !customer.value.address) {
    alert("Vui lòng nhập đầy đủ thông tin")
    return
  }

  if (!cart.value.length) {
    alert("Giỏ hàng trống")
    return
  }

  const orderId = "DH" + Date.now()

  /* =============================
     THANH TOÁN VNPAY
  ============================= */

  if (paymentMethod.value === "VNPAY") {

    const order = {
      id: orderId,
      customer: customer.value,
      items: cart.value,
      total: total.value
    }

    localStorage.setItem("currentOrder", JSON.stringify(order))

    const res = await fetch("http://localhost:3000/create-payment", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        amount: total.value,
        orderId: orderId
      })
    })

    const data = await res.json()

    if (data.url) {
      window.location.href = data.url
    } else {
      alert("Không tạo được link VNPay")
    }

    return
  }

  /* =============================
     TIỀN MẶT
  ============================= */

  try {

    await createInvoice("CHUA_XAC_NHAN")

    localStorage.removeItem("cart")

    alert("Đặt hàng thành công!")

    router.push("/home")

  } catch (error) {

    console.error(error)

    alert("Có lỗi xảy ra khi tạo hóa đơn")

  }

}



const backHome = () => {
  router.push("/home")
}
</script>

<template>

<div class="checkout">

<h1>Thanh toán</h1>

<div class="checkout-container">

<!-- GIỎ HÀNG -->

<div class="cart">

<h2>Giỏ hàng</h2>

<div v-if="cart.length">

<div
v-for="item in cart"
:key="item.id"
class="cart-item"
>

<img :src="item.image" />

<div class="info">

<h3>{{ item.name }}</h3>

<p>Số lượng: {{ item.quantity }}</p>

<p>{{ VND(item.price) }}</p>

</div>

</div>

</div>

<div v-else class="empty">
Giỏ hàng trống
</div>

</div>

<!-- THANH TOÁN -->

<div class="payment">

<h2>Thông tin khách hàng</h2>

<input v-model="customer.name" placeholder="Họ và tên" />

<input v-model="customer.phone" placeholder="Số điện thoại" />

<input v-model="customer.address" placeholder="Địa chỉ" />

<h2>Phương thức thanh toán</h2>

<label class="method">
<input type="radio" value="TIEN_MAT" v-model="paymentMethod" />
<span>Tiền mặt khi nhận hàng</span>
</label>

<label class="method">
<input type="radio" value="VNPAY" v-model="paymentMethod" />
<span>Thanh toán qua VNPay</span>
</label>

<div class="total">
Tổng tiền:
<strong>{{ VND(total) }}</strong>
</div>

<button class="btn-order" @click="submitOrder">
Xác nhận đặt hàng
</button>

<button class="btn-back" @click="backHome">
Quay lại
</button>

</div>

</div>

</div>

</template>

<style scoped>

.checkout{
max-width:1100px;
margin:auto;
padding:40px 20px;
}

.checkout-container{
display:flex;
gap:40px;
margin-top:30px;
}

.cart{
flex:2;
}

.cart-item{
display:flex;
gap:15px;
border-bottom:1px solid #eee;
padding:10px 0;
}

.cart-item img{
width:80px;
height:80px;
object-fit:cover;
border-radius:6px;
}

.payment{
flex:1;
background:#fafafa;
padding:20px;
border-radius:10px;
}

input{
width:100%;
padding:12px;
margin-bottom:10px;
border:1px solid #ddd;
border-radius:6px;
}

.method{
display:flex;
gap:10px;
margin:10px 0;
}

.total{
font-size:20px;
margin:20px 0;
}

.btn-order{
width:100%;
padding:14px;
background:#1a3a52;
color:white;
border:none;
border-radius:8px;
font-weight:600;
cursor:pointer;
}

.btn-back{
width:100%;
padding:14px;
margin-top:10px;
background:#eee;
border:none;
border-radius:8px;
cursor:pointer;
}

.empty{
padding:40px;
text-align:center;
color:#666;
}

@media(max-width:768px){
.checkout-container{
flex-direction:column;
}
}

</style>