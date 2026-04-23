<template>
  <main class="wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>Nhân viên</h1>
          <small class="muted">
            Quản lý tài khoản vận hành, phân quyền
          </small>
        </div>

        <router-link
          class="btn primary"
          to="/admin/nhan-vien/form"
        >
          + Thêm nhân viên
        </router-link>
      </div>

      <div class="body">

<div class="toolbar">
  <div class="filters">
    <input
      class="filter-input"
      type="text"
      placeholder="Tìm tên / email / SĐT..."
      v-model="search"
    />

    <select class="filter-select" v-model="roleFilter">
      <option value="">Tất cả vai trò</option>
      <option>Quản trị hệ thống</option>
      <option>Nhân viên bán hàng</option>
    </select>

    <select class="filter-select" v-model="statusFilter">
      <option value="">Tất cả trạng thái</option>
      <option>Hoạt động</option>
      <option>Ngừng hoạt động</option>
    </select>
  </div>
</div>
        <table class="table">
          <thead>
            <tr>
              <th style="width:80px">Mã</th>
              <th style="width:220px">Nhân viên</th>
              <th>Email</th>
              <th style="width:140px">SĐT</th>
              <th style="width:110px;text-align:center">Vai trò</th>
              <th style="width:140px;text-align:center">Trạng thái</th>
              <th style="width:90px;text-align:center">Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="item in filteredList" :key="item.id">
              <td>{{ item.code }}</td>

              <td>
                <b>{{ item.name }}</b>
                <div class="muted" style="font-size:12px">
                  {{ item.role }}
                </div>
              </td>

              <td class="muted">
                {{ item.email }}
              </td>

              <td>{{ item.phone }}</td>

              <td style="text-align:center">
                <span class="pill">
                  {{ item.role === "Quản trị hệ thống" ? "ADMIN" : "STAFF" }}
                </span>
              </td>

              <td style="text-align:center">
                <span
                  class="pill"
                  :class="`status-${getAdminStatusTone(item.status)}`"
                >
                  ● {{ item.status }}
                </span>
              </td>

              <td style="text-align:center">
                <router-link
                  class="iconbtn"
                  :to="`/admin/nhan-vien/form/${item.id}`"
                  style="margin:0 auto"
                >
                  <span class="material-icons-outlined">visibility</span>
                </router-link>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination">
          <div>Hiển thị 1–10 / {{ list.length }}</div>

          <div class="pager">
            <button class="btn">← Trước</button>
            <span class="chip">1</span>
            <button class="btn">Sau →</button>
          </div>
        </div>

      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from "vue"
import { useRouter } from "vue-router"
import {
  getAllNhanVien,
  updateNhanVien
} from "../../../services/nhanVienService"
import { Check } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel } from "../../../utils/adminStatus"

const router = useRouter()

const search = ref("")
const statusFilter = ref("")
const roleFilter = ref("")
const list = ref([])

async function loadData() {
  const res = await getAllNhanVien()

  list.value = res.data.map(item => ({
    id: item.id,
    code: item.maNhanVien,
    name: item.tenNhanVien,
    phone: item.soDienThoai,
    email: item.email || item.taiKhoan?.email || "", 
    role:
      item.chucVu?.tenChucVu === "ADMIN"
        ? "Quản trị hệ thống"
        : "Nhân viên bán hàng",
    status: normalizeAdminStatusLabel(item.trangThaiHoatDong)
  }))
}

onMounted(loadData)

const filteredList = computed(() => {
  return list.value.filter(item => {

    const matchSearch =
      item.name.toLowerCase().includes(search.value.toLowerCase()) ||
      item.phone?.includes(search.value)

    const matchRole =
      !roleFilter.value ||
      item.role === roleFilter.value

    const matchStatus =
      !statusFilter.value ||
      item.status === statusFilter.value

    return matchSearch && matchRole && matchStatus
  })
})

async function toggleStatus(item) {

  const payload = {
    maNhanVien: item.code,
    tenNhanVien: item.name,
    soDienThoai: item.phone,
    trangThaiHoatDong:
      item.status === "Hoạt động"
        ? "Ngừng hoạt động"
        : "Hoạt động"
  }

  await updateNhanVien(item.id, payload)

  loadData()
}
</script>

<style scoped>
.toolbar {
  margin-bottom: 20px;
}

.filters {
  display: flex;             
  align-items: center;
  gap: 16px;                
  flex-wrap: wrap;           
}

.filter-input {
  width: 320px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d8dee9;
}

.filter-select {
  width: 220px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d8dee9;
}

@media (max-width: 1024px) {
  .head { flex-direction: column; align-items: flex-start; gap: 12px; }
  .filter-input, .filter-select { width: 100%; }
}
@media (max-width: 768px) {
  .body { overflow-x: auto; }
  table { min-width: 700px; }
}
</style>