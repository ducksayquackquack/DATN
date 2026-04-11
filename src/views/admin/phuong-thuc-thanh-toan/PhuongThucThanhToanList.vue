<template>
  <main class="wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>Phương thức thanh toán</h1>
          <small class="muted">
            Quản lý các phương thức dùng trong checkout
          </small>
        </div>

        <router-link
          class="btn primary"
          to="/admin/phuong-thuc-thanh-toan/form"
        >
          + Thêm phương thức
        </router-link>
      </div>

      <div class="body">

        <div class="toolbar">
          <div class="filters">
            <input
              style="width:320px"
              type="text"
              placeholder="Tìm theo mã / tên..."
              v-model="search"
            />

            <select
              style="width:220px"
              v-model="statusFilter"
            >
              <option value="">Tất cả</option>
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
          </div>
        </div>

        <table class="table">
          <thead>
            <tr>
              <th style="width:100px">Mã</th>
              <th>Tên phương thức</th>
              <th style="width:170px">Ngày tạo</th>
              <th style="width:170px">Ngày sửa</th>
              <th style="width:140px">Trạng thái</th>
              <th style="width:140px" class="center">Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="item in filteredList"
              :key="item.code"
            >
              <td>{{ item.code }}</td>

              <td>
                <b>{{ item.name }}</b>
                <div class="muted" style="font-size:12px">
                  {{ item.description }}
                </div>
              </td>

              <td>{{ formatDateTime(item.createdAt) }}</td>

              <td>{{ formatDateTime(item.updatedAt) }}</td>

              <td>
                <span
                  class="pill"
                  :class="`status-${getAdminStatusTone(item.status)}`"
                >
                  ● {{ item.status }}
                </span>
              </td>

            <td class="center">
              <div class="actions">
                <router-link
                  class="iconbtn"
                  :to="`/admin/phuong-thuc-thanh-toan/form/${item.id}`"
                >
                  <span class="material-icons-outlined">visibility</span>
                </router-link>

                <button
                  class="iconbtn"
                  @click="remove(item.id)"
                >
                  <Trash2 size="16" />
                </button>
              </div>
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
import {
  getAllPTTT,
  deletePTTT
} from "../../../services/ptttService"
import { Trash2 } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel } from "../../../utils/adminStatus"

const search = ref("")
const statusFilter = ref("")
const list = ref([])

const formatDateTime = (value) => {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString("vi-VN")
}

async function loadData() {
  const res = await getAllPTTT()

  list.value = res.data.map(item => ({
    id: item.id,
    code: item.ma,
    name: item.ten,
    description: item.moTa,
    createdAt: item.ngayTao,
    updatedAt: item.ngaySua,
    status: normalizeAdminStatusLabel(item.trangThai)
  }))
}

onMounted(loadData)

const filteredList = computed(() => {
  return list.value.filter(item => {

    const matchSearch =
      item.code.toLowerCase().includes(search.value.toLowerCase()) ||
      item.name.toLowerCase().includes(search.value.toLowerCase())

    const matchStatus =
      !statusFilter.value ||
      item.status === statusFilter.value

    return matchSearch && matchStatus
  })
})

async function remove(id) {
  const ok = await window.confirmDialog?.("Xóa phương thức thanh toán này?") ?? confirm("Xóa phương thức thanh toán này?")
  if (!ok) return
  await deletePTTT(id)
  loadData()
}
</script>

<style scoped>

.center{
  text-align:center;
}

.actions{
  display:flex;
  justify-content:center;
  align-items:center;
  gap:8px;
}

</style>