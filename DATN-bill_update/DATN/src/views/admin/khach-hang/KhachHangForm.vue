<script setup>
import { ref, onMounted, computed } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  createKhachHang,
  updateKhachHang,
  getKhachHangById
} from "../../../services/khachHangService"
import {
  getHoaDonByKhachHang
} from "../../../services/khachHangService"

const router = useRouter()
const route = useRoute()
const id = route.params.id

const form = ref({
  maKhachHang: "",
  tenKhachHang: "",
  gioiTinh: "Nam",
  ngaySinh: "",
  soDienThoai: "",
  trangThai: "Hoạt động"
})

const hoaDonList = ref([])

onMounted(async () => {
  if (id) {
    const res = await getKhachHangById(id)

    form.value = {
      ...res.data,
      ngaySinh: res.data.ngaySinh
        ? res.data.ngaySinh.substring(0,10)
        : "",
      trangThai: res.data.trangThai || "Hoạt động"
    }

    const hdRes = await getHoaDonByKhachHang(id)
    hoaDonList.value = hdRes.data
  }
})

const totalOrders = computed(() => hoaDonList.value.length)

const totalSpending = computed(() =>
  hoaDonList.value.reduce((sum, hd) =>
    sum + (hd.thanhTien || 0), 0)
)

const formatCurrency = (v) =>
  new Intl.NumberFormat("vi-VN").format(v) + "₫"

const save = async () => {
  if (id) {
    await updateKhachHang(id, form.value)
  } else {
    await createKhachHang(form.value)
  }

  router.push("/admin/khach-hang/list")
}
</script>

<template>
  <main class="wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>Form khách hàng</h1>
          <small class="muted">
            Tạo mới / cập nhật thông tin khách
          </small>
        </div>

        <div style="display:flex;gap:10px">
          <button
            class="btn"
            @click="router.push('/admin/khach-hang/list')"
          >
            ← Quay lại
          </button>

          <button
            class="btn primary"
            @click="save"
          >
            Lưu
          </button>
        </div>
      </div>

      <div class="body">
        <div class="grid cols2">

          <div class="field">
            <label>Mã khách</label>
            <input
              class="input"
              v-model="form.maKhachHang"
              placeholder="VD: KH001"
            />
          </div>

          <div class="field">
            <label>Họ tên</label>
            <input
              class="input"
              v-model="form.tenKhachHang"
              placeholder="VD: Nguyễn Văn A"
            />
          </div>

          <div class="field">
            <label>Giới tính</label>
            <select
              class="input"
              v-model="form.gioiTinh"
            >
              <option>Nam</option>
              <option>Nữ</option>
            </select>
          </div>

          <div class="field">
            <label>Ngày sinh</label>
            <input
              class="input"
              type="date"
              v-model="form.ngaySinh"
            />
          </div>

          <div class="field">
            <label>SĐT</label>
            <input
              class="input"
              v-model="form.soDienThoai"
              placeholder="VD: 0912xxxxxx"
            />
          </div>

          <div class="field">
            <label>Trạng thái</label>
            <select
              class="input"
              :class="form.trangThai === 'Hoạt động' ? 'status-ok' : 'status-bad'"
              v-model="form.trangThai"
            >
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
          </div>

        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.input {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d8dee9;
  background: #fff;
  font-size: 14px;
  transition: 0.2s ease;
}

.input:focus {
  outline: none;
  border-color: #4f46e5;
}
.status-ok {
  background: #dcfce7;
  color: #166534;
  border-color: #86efac;
  font-weight: 500;
}

.status-bad {
  background: #fee2e2;
  color: #991b1b;
  border-color: #fca5a5;
  font-weight: 500;
}
</style>