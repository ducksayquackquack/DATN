<template>
  <main class="wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>Màu sắc</h1>
          <small class="muted">
            Quản lý màu + mã màu (hex) để render swatch
          </small>
        </div>

        <router-link
          class="btn primary"
          to="/admin/mau-sac/form"
        >
          + Thêm màu
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
              <th style="width:90px">Mã</th>
              <th>Tên màu</th>
              <th style="width:170px">Ngày tạo</th>
              <th style="width:170px">Ngày sửa</th>
              <th style="width:140px">Trạng thái</th>
              <th class="center" style="width:140px">Thao tác</th>            </tr>
          </thead>

          <tbody>
            <tr
              v-for="item in filteredList"
              :key="item.code"
            >
              <td>{{ item.code }}</td>

              <td>
                <b>{{ item.name }}</b>
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
                  <button
                    class="iconbtn"
                    @click="router.push(`/admin/mau-sac/form/${item.id}`)"
                  >
                    <Pencil size="16" />
                  </button>

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
import { useRouter } from "vue-router"
import {
  getAllMauSac,
  deleteMauSac
} from "../../../services/mauSacService"
import { Pencil, Trash2 } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel } from "../../../utils/adminStatus"

const router = useRouter()
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
  const res = await getAllMauSac()

  list.value = res.data.map(item => ({
    id: item.id,
    code: item.maMau,
    name: item.tenMau,
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
  await deleteMauSac(id)
  loadData()
}
</script>

<style scoped>
.center {
  text-align: center;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.iconbtn {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>