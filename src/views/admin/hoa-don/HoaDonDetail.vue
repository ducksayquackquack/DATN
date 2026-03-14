<script setup>
import { ref, onMounted, computed } from "vue"
import { useRoute, useRouter } from "vue-router"
import axios from "axios"
import QRCode from "qrcode"

const route = useRoute()
const router = useRouter()

const id = route.params.id

const hoaDon = ref(null)
const chiTietList = ref([])

const selectedTrangThai = ref("")
const selectedPTTT = ref("")
const loading = ref(false)

const qrCode = ref("")
const showQR = ref(false)

const API = "http://localhost:8080/api/hoa-don"
const PAYMENT_API = "http://localhost:8080/api/payment/create"

onMounted(loadData)

async function loadData() {
  loading.value = true

  // Luôn đọc từ localStorage trước
  const localData = JSON.parse(localStorage.getItem("hoaDonList")) || []
  const localHoaDon = localData.find(item => 
    String(item.id) === String(id) || 
    String(item.maHoaDon) === String(id)
  )

  if (localHoaDon) {
    console.log("Found hóa đơn in localStorage:", localHoaDon)
    
    // Có dữ liệu trong localStorage - dùng luôn
    hoaDon.value = {
      id: localHoaDon.id,
      maHoaDon: localHoaDon.maHoaDon || localHoaDon.id,
      tenKhachHang: localHoaDon.tenKhachHang || localHoaDon.customer || "Khách hàng",
      soDienThoaiNhanHang: localHoaDon.soDienThoaiNhanHang || localHoaDon.phone || "",
      diaChiNhanHang: localHoaDon.diaChiNhanHang || localHoaDon.address || "",
      thanhTien: Number(localHoaDon.thanhTien || localHoaDon.total || 0),
      phuongThucThanhToan: localHoaDon.phuongThucThanhToan || localHoaDon.payment || "TIEN_MAT",
      trangThai: localHoaDon.trangThai || localHoaDon.status || "CHUA_XAC_NHAN",
      ngayTao: localHoaDon.ngayTao || localHoaDon.date || new Date().toISOString(),
      phiShip: Number(localHoaDon.phiShip || 0)
    }
    
    selectedTrangThai.value = hoaDon.value.trangThai
    selectedPTTT.value = hoaDon.value.phuongThucThanhToan
    
    // Chuyển đổi items - hỗ trợ cả 2 format
    const items = localHoaDon.chiTietHoaDon || localHoaDon.items || []
    console.log("Raw items:", items)
    
    chiTietList.value = items.map(item => {
      // Lấy giá trị từ cả 2 format
      const price = Number(item.price || item.donGia || 0)
      const quantity = Number(item.quantity || item.soLuong || 1)
      const name = item.name || item.tenSanPham || "Sản phẩm"
      
      console.log(`Item: ${name}, Price: ${price}, Qty: ${quantity}`)
      
      return {
        id: item.id || Math.random(),
        soLuong: quantity,
        donGia: price,
        sanPhamChiTiet: {
          sanPham: {
            tenSanPham: name
          },
          giaBan: price
        }
      }
    })
    
    console.log("Converted chiTietList:", chiTietList.value)
    
    loading.value = false
    return
  }

  // Không có trong localStorage - thử backend
  try {
    const res = await axios.get(`${API}/${id}`)
    hoaDon.value = res.data
    selectedTrangThai.value = res.data?.trangThai
    selectedPTTT.value = res.data?.phuongThucThanhToan

    const ct = await axios.get(`${API}/${id}/chi-tiet`)
    chiTietList.value = ct.data || []
  } catch (e) {
    console.error("Không tìm thấy hóa đơn:", e)
    alert("Không tìm thấy hóa đơn!")
  }

  loading.value = false
}

const formatCurrency = (v) =>
  new Intl.NumberFormat("vi-VN").format(v || 0) + "₫"

const subtotal = computed(() => {
  if (!Array.isArray(chiTietList.value) || chiTietList.value.length === 0) {
    return 0
  }
  
  const total = chiTietList.value.reduce((sum, item) => {
    const price = Number(item?.donGia ?? item?.sanPhamChiTiet?.giaBan ?? 0)
    const qty = Number(item?.soLuong ?? 0)
    const itemTotal = price * qty
    console.log(`Subtotal calc - Price: ${price}, Qty: ${qty}, Item Total: ${itemTotal}`)
    return sum + itemTotal
  }, 0)
  
  console.log("Final subtotal:", total)
  return total
})

async function taoQRThanhToan() {
  try {
    const amount = subtotal.value + (hoaDon.value?.phiShip || 0)

    const res = await axios.get(`${PAYMENT_API}?amount=${amount}`)

    const paymentUrl = res.data.paymentUrl

    qrCode.value = await QRCode.toDataURL(paymentUrl)

    showQR.value = true
  } catch (e) {
    alert("Không tạo được QR VNPay")
  }
}

async function handleUpdate() {
  if (!confirm("Bạn có chắc muốn cập nhật hóa đơn này không?")) {
    return
  }

  try {
    // Kiểm tra xem hóa đơn có trong localStorage không
    const localData = JSON.parse(localStorage.getItem("hoaDonList")) || []
    const index = localData.findIndex(item => 
      String(item.id) === String(id) || 
      String(item.maHoaDon) === String(id)
    )

    if (index !== -1) {
      // Cập nhật localStorage
      localData[index] = {
        ...localData[index],
        trangThai: selectedTrangThai.value,
        phuongThucThanhToan: selectedPTTT.value
      }
      localStorage.setItem("hoaDonList", JSON.stringify(localData))
      
      alert("Cập nhật thành công!")
      await loadData()
      return
    }

    
    const payload = {
      ...hoaDon.value,
      trangThai: selectedTrangThai.value,
      phuongThucThanhToan: selectedPTTT.value
    }

    await axios.put(`${API}/${id}`, payload)

    if (selectedPTTT.value === "VNPAY") {
      await taoQRThanhToan()
      return
    }

    alert("Cập nhật thành công")
    await loadData()
    
  } catch (e) {
    console.error("Lỗi cập nhật:", e)
    alert("Cập nhật thất bại: " + (e.response?.data?.message || e.message))
  }
}

async function handleDelete() {
  if (!confirm("Bạn chắc muốn huỷ đơn?")) return

  try {
    // Kiểm tra xem hóa đơn có trong localStorage không
    const localData = JSON.parse(localStorage.getItem("hoaDonList")) || []
    const index = localData.findIndex(item => 
      String(item.id) === String(id) || 
      String(item.maHoaDon) === String(id)
    )

    if (index !== -1) {
      // Cập nhật trạng thái thành ĐÃ_HUY thay vì xóa
      localData[index] = {
        ...localData[index],
        trangThai: "DA_HUY"
      }
      localStorage.setItem("hoaDonList", JSON.stringify(localData))
      alert("Đã huỷ đơn!")
      
      // Cập nhật lại giao diện
      selectedTrangThai.value = "DA_HUY"
      await loadData()
      return
    }

    // Nếu không có trong localStorage, thử cập nhật backend
    const payload = {
      ...hoaDon.value,
      trangThai: "DA_HUY"
    }
    await axios.put(`${API}/${id}`, payload)
    alert("Đã huỷ đơn!")
    await loadData()
    
  } catch (e) {
    console.error("Lỗi huỷ đơn:", e)
    alert("Không thể huỷ: " + (e.response?.data?.message || e.message))
  }
}

function handlePrint() {
  window.print()
}
</script>

<template>
<main class="wrap" v-if="hoaDon">

<div class="grid cols2">

<!-- LEFT -->
<div class="card">

<div class="head">
<div>
<h1>Hoá đơn #{{ hoaDon.maHoaDon }}</h1>
<small class="muted">
Tạo: {{ hoaDon.ngayTao }}
</small>
</div>

<div class="actions">
<button class="btn" @click="router.push('/admin/hoa-don/list')">
← Quay lại
</button>

<button class="btn" @click="handlePrint">
In
</button>

<button class="btn primary" @click="handleUpdate">
Lưu
</button>
</div>
</div>

<div class="body">

<!-- KHÁCH HÀNG -->
<div class="card inner">
<div class="head">
<h2>Khách hàng</h2>
</div>

<div class="body">
<b>{{ hoaDon.tenKhachHang }}</b>

<div class="muted">
{{ hoaDon.soDienThoaiNhanHang }}
</div>

<div class="muted">
{{ hoaDon.diaChiNhanHang }}
</div>
</div>
</div>

<hr class="sep">

<!-- TRẠNG THÁI -->
<div class="card inner">
<div class="head">
<h2>Trạng thái</h2>
</div>

<div class="body">
<select v-model="selectedTrangThai">

<option value="CHUA_XAC_NHAN">Chưa xác nhận</option>
<option value="CHO_THANH_TOAN">Chờ thanh toán</option>
<option value="CHO_GIAO">Chờ giao</option>
<option value="DANG_GIAO">Đang giao</option>
<option value="DA_HOAN_THANH">Hoàn thành</option>
<option value="DA_HUY">Huỷ</option>

</select>
</div>
</div>

<hr class="sep">

<!-- SẢN PHẨM -->
<div class="card inner">

<div class="head">
<h2>Sản phẩm</h2>
</div>

<div class="body">

<table class="table">

<thead>
<tr>
<th>Sản phẩm</th>
<th>SL</th>
<th>Đơn giá</th>
<th>Thành tiền</th>
</tr>
</thead>

<tbody>

<tr v-for="item in chiTietList" :key="item.id">

<td>
{{ item?.sanPhamChiTiet?.sanPham?.tenSanPham }}
</td>

<td>
{{ item?.soLuong }}
</td>

<td>
{{ formatCurrency(item?.donGia ?? item?.sanPhamChiTiet?.giaBan) }}
</td>

<td>
{{
formatCurrency(
(item?.donGia ?? item?.sanPhamChiTiet?.giaBan) * item?.soLuong
)
}}
</td>

</tr>

</tbody>
</table>

<div class="card total-box mt">

<div class="body">

<div class="row-between">
<span>Tạm tính</span>
<span>{{ formatCurrency(subtotal) }}</span>
</div>

<div class="row-between">
<span>Phí ship</span>
<span>{{ formatCurrency(hoaDon.phiShip) }}</span>
</div>

<hr>

<div class="row-between bold">

<span>Tổng</span>

<span>
{{ formatCurrency(subtotal + (hoaDon.phiShip || 0)) }}
</span>

</div>

<div class="mt">

<label>Thanh toán</label>

<select v-model="selectedPTTT">

<option value="TIEN_MAT">Tiền mặt</option>
<option value="VNPAY">VNPAY</option>

</select>

</div>

</div>
</div>

<div v-if="showQR" class="qr-box">

<h3>Quét QR để thanh toán</h3>

<img :src="qrCode">

<p class="muted">
Mở app ngân hàng để quét
</p>

<button class="btn" @click="showQR=false">
Đóng
</button>

</div>

</div>
</div>

</div>
</div>

<!-- RIGHT -->
<div class="card">

<div class="head">
<h2>Timeline</h2>
</div>

<div class="body">

<div class="card inner">
<div class="body">
<b>Đơn được tạo</b>

<div class="muted">
{{ hoaDon.ngayTao }}
</div>
</div>
</div>

<div class="card inner">
<div class="body">
<b>{{ selectedTrangThai }}</b>

<div class="muted">
{{ hoaDon.ngayTao }}
</div>
</div>
</div>

<hr>

<button class="btn danger" @click="handleDelete">
Huỷ đơn
</button>

</div>
</div>

</div>

</main>
</template>

<style scoped>

.grid{
display:grid;
gap:20px;
}

.cols2{
grid-template-columns:1fr 350px;
}

.card{
background:white;
border-radius:12px;
padding:16px;
box-shadow:0 2px 8px rgba(0,0,0,0.05);
}

.inner{
background:#fafafa;
}

.head{
display:flex;
justify-content:space-between;
align-items:center;
margin-bottom:10px;
}

.actions{
display:flex;
gap:10px;
}

.table{
width:100%;
border-collapse:collapse;
}

.table th,
.table td{
border-bottom:1px solid #eee;
padding:8px;
}

.row-between{
display:flex;
justify-content:space-between;
margin-bottom:6px;
}

.bold{
font-weight:600;
}

.muted{
color:#777;
font-size:13px;
}

.mt{
margin-top:10px;
}

.qr-box{
margin-top:20px;
text-align:center;
border:1px dashed #ccc;
padding:20px;
border-radius:10px;
}

.btn{
padding:6px 12px;
border:none;
background:#eee;
cursor:pointer;
border-radius:6px;
}

.primary{
background:#2563eb;
color:white;
}

.danger{
background:#ef4444;
color:white;
}

</style>