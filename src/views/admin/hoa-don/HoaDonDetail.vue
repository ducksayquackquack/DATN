<script setup>
import { ref, onMounted, computed } from "vue"
import { useRoute, useRouter } from "vue-router"
import axios from "axios"

const route = useRoute()
const router = useRouter()
const id = route.params.id

const hoaDon = ref(null)
const chiTietList = ref([])
const selectedTrangThai = ref("")
const selectedPTTT = ref("")
const ghiChu = ref("")
const loading = ref(false)

const API = "http://localhost:8080/api/hoa-don"

const loadData = async () => {
  loading.value = true
  try {
    const hdRes = await axios.get(`${API}/${id}`)
    hoaDon.value = hdRes.data

    selectedTrangThai.value = hdRes.data?.trangThai || ""
    const rawPTTT = hdRes.data?.phuongThucThanhToan

    if (!rawPTTT) {
      selectedPTTT.value = ""
    } else if (rawPTTT.toUpperCase() === "COD") {
      selectedPTTT.value = "COD"
    } else {
      selectedPTTT.value = "Chuyển khoản"
    }
    ghiChu.value = hdRes.data?.ghiChu || ""

    try {
      const ctRes = await axios.get(`${API}/${id}/chi-tiet`)
      chiTietList.value = Array.isArray(ctRes.data) ? ctRes.data : []
    } catch {
      chiTietList.value = []
    }

  } catch {
    hoaDon.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const formatCurrency = (value) =>
  new Intl.NumberFormat("vi-VN").format(value || 0) + "₫"

const subtotal = computed(() => {
  if (!Array.isArray(chiTietList.value)) return 0
  return chiTietList.value.reduce((sum, item) => {
    const price =
      item?.donGia ??
      item?.sanPhamChiTiet?.giaBan ??
      item?.thanhTien ??
      0
    const qty = item?.soLuong ?? 0
    return sum + price * qty
  }, 0)
})

const handleUpdate = async () => {
  if (!hoaDon.value) return
  loading.value = true
  try {
    const payload = {
      ...hoaDon.value,
      trangThai: selectedTrangThai.value,
      phuongThucThanhToan: selectedPTTT.value
    }

    await axios.put(`${API}/${id}`, payload)
    await loadData()
    alert("Đã cập nhật thành công")
  } catch {
    alert("Cập nhật thất bại")
  } finally {
    loading.value = false
  }
}

const handleDelete = async () => {
  if (!hoaDon.value) return
  loading.value = true
  try {
    await axios.delete(`${API}/${id}`)
    router.push("/admin/hoa-don/list")
  } finally {
    loading.value = false
  }
}

const handlePrint = () => {
  window.print()
}
</script>

<template>
  <main class="wrap" v-if="hoaDon">
    <div class="grid cols2">

      <div class="card">
        <div class="head">
          <div>
            <h1>Hoá đơn #{{ hoaDon.maHoaDon }}</h1>
            <small class="muted">
              Tạo: {{ hoaDon.ngayTao }}
            </small>
          </div>

          <div style="display:flex;gap:10px;flex-wrap:wrap">
            <button class="btn" @click="router.push('/admin/hoa-don/list')">
              ← Quay lại
            </button>
            <button class="btn" @click="handlePrint">
              🖨️ In
            </button>
            <button class="btn primary" :disabled="loading" @click="handleUpdate">
              Lưu
            </button>
          </div>
        </div>

        <div class="body">

          <div class="grid cols2">
            <div class="card inner">
              <div class="head">
                <h2>Khách hàng</h2>
                <small class="muted">Thông tin nhận hàng</small>
              </div>
              <div class="body">
                <b>{{ hoaDon.tenKhachHang }}</b>
                <div class="muted">
                  {{ hoaDon.soDienThoaiNhanHang }}
                </div>
                <div class="muted" style="margin-top:8px">
                  {{ hoaDon.diaChiNhanHang }}
                </div>
              </div>
            </div>

            <div class="card inner">
              <div class="head">
                <h2>Trạng thái</h2>
                <small class="muted">Workflow</small>
              </div>
              <div class="body">
                <label>Cập nhật trạng thái</label>
                <select v-model="selectedTrangThai">
                  <option>Chờ xác nhận</option>
                  <option>Đang chuẩn bị</option>
                  <option>Đang giao</option>
                  <option>Hoàn thành</option>
                  <option>Huỷ</option>
                </select>
              </div>
            </div>
          </div>

          <hr class="sep"/>

          <div class="card inner">
            <div class="head">
              <h2>Sản phẩm</h2>
              <small class="muted">Line items</small>
            </div>
            <div class="body">
              <table class="table">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th class="center">SL</th>
                    <th class="right">Đơn giá</th>
                    <th class="right">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="chiTietList.length === 0">
                    <td colspan="4" style="text-align:center">
                      Không có sản phẩm
                    </td>
                  </tr>

                  <tr v-for="item in chiTietList" :key="item.id">
                    <td>
                      <b>
                        {{ item?.sanPhamChiTiet?.sanPham?.tenSanPham || 'SP ID: ' + item?.sanPhamChiTiet?.id }}
                      </b>
                    </td>
                    <td class="center">
                      {{ item?.soLuong || 0 }}
                    </td>
                    <td class="right">
                      {{ formatCurrency(item?.donGia ?? item?.sanPhamChiTiet?.giaBan ?? 0) }}
                    </td>
                    <td class="right">
                      {{
                        formatCurrency(
                          (item?.donGia ?? item?.sanPhamChiTiet?.giaBan ?? 0) *
                          (item?.soLuong || 0)
                        )
                      }}
                    </td>
                  </tr>
                </tbody>
              </table>

              <div class="grid cols2" style="margin-top:12px">
                <div>
                  <label>Ghi chú đơn</label>
                  <textarea v-model="ghiChu"></textarea>
                </div>

                <div class="card total-box">
                  <div class="body">
                    <div class="row-between muted">
                      <span>Tạm tính</span>
                      <span>{{ formatCurrency(subtotal) }}</span>
                    </div>

                    <div class="row-between muted">
                      <span>Phí ship</span>
                      <span>{{ formatCurrency(hoaDon.phiShip) }}</span>
                    </div>

                    <hr class="sep"/>

                    <div class="row-between bold">
                      <span>Tổng</span>
                      <span>{{ formatCurrency(subtotal + (hoaDon.phiShip || 0)) }}</span>                    </div>

                    <div style="margin-top:10px">
                      <label>Phương thức thanh toán</label>
                      <select v-model="selectedPTTT">
                        <option value="COD">COD</option>
                        <option value="Chuyển khoản">Chuyển khoản</option>
                      </select>
                    </div>
                  </div>
                </div>
              </div>

            </div>
          </div>

        </div>
      </div>

      <div class="card">
        <div class="head">
          <h2>Timeline</h2>
          <small class="muted">Log thao tác</small>
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

          <hr class="sep"/>

          <button class="btn danger" @click="handleDelete">
            Huỷ đơn
          </button>
        </div>
      </div>

    </div>
  </main>
</template>

<style scoped>
.row-between {
  display:flex;
  justify-content:space-between;
  margin-bottom:8px;
}
.bold {
  font-weight:600;
}
.center {
  text-align:center;
}
.muted {
  color:#6b7280;
  font-size:13px;
}
.inner {
  border-radius:14px;
}
.total-box {
  border-radius:14px;
}
</style>