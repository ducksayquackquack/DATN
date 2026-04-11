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
  // Confirmation before saving
  const action = id ? 'cập nhật' : 'tạo mới'
  const confirmed = await window.confirmDialog(`Bạn có chắc chắn muốn ${action} thông tin khách hàng "${form.value.tenKhachHang}"?`)
  if (!confirmed) {
    return
  }

  try {
    if (id) {
      await updateKhachHang(id, form.value)
      window.toast.success('Cập nhật khách hàng thành công!')
    } else {
      await createKhachHang(form.value)
      window.toast.success('Tạo mới khách hàng thành công!')
    }
    router.push("/admin/khach-hang/list")
  } catch (error) {
    window.toast.error('Có lỗi xảy ra: ' + (error.message || 'Vui lòng thử lại'))
  }
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
              class="input auto-code-input"
              readonly
              value="Mã tự sinh"
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
  background-color: #fff;
  font-size: 14px;
  transition: 0.2s ease;
}
select.input {
  padding-right: 34px;
}

.input:focus {
  outline: none;
  border-color: #4f46e5;
}
.status-ok {
  background-color: #dcfce7;
  color: #166534;
  border-color: #86efac;
  font-weight: 500;
}

.status-bad {
  background-color: #fee2e2;
  color: #991b1b;
  border-color: #fca5a5;
  font-weight: 500;
}

.auto-code-input {
  background: #f1f5f9;
  color: #64748b;
  border-color: #cbd5e1;
  font-weight: 600;
}
</style>