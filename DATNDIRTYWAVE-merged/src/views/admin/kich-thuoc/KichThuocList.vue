<script setup>
import { ref, onMounted, computed } from "vue"
import { useRouter } from "vue-router"
import {
  getAllKichThuoc,
  deleteKichThuoc
} from "../../../services/kichThuocService"
import { Trash2 } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel } from "../../../utils/adminStatus"
import { useConfirm } from "../../../composables/useConfirm"

const router = useRouter()
const { askConfirm } = useConfirm()
const list = ref([])

const searchText = ref("")
const filterStatus = ref("Tất cả")
const sortOption = ref("Mới nhất")

const formatDateTime = (value) => {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString("vi-VN")
}

const extractList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

async function loadData() {
  const res = await getAllKichThuoc()
  const items = extractList(res?.data)
  list.value = items.map(item => ({
    ...item,
    trangThai: normalizeAdminStatusLabel(item.trangThai),
    ghiChu: item.ghiChu || item.moTa || ""
  }))
}
onMounted(loadData)

async function remove(id) {
  const ok = await askConfirm("Xóa kích thước này?")
  if (!ok) return
  await deleteKichThuoc(id)
  await loadData()
}

const filteredData = computed(() => {
  let data = [...list.value]

  if (searchText.value.trim()) {
    const keyword = searchText.value.toLowerCase()
    data = data.filter(kt =>
      kt.maKichThuoc?.toLowerCase().includes(keyword) ||
      kt.tenKichThuoc?.toLowerCase().includes(keyword)
    )
  }

  if (filterStatus.value !== "Tất cả") {
    data = data.filter(kt => kt.trangThai === filterStatus.value)
  }

  return data
})
</script>

<template>
  <main class="wrap">
    <div class="card">

      <div class="head">
        <div>
          <h1>Kích thước</h1>
          <small class="muted">
            Quản lý size (S/M/L/XL...) hoặc số (28/29/30...)
          </small>
        </div>

        <button
          class="btn primary"
          @click="router.push('/admin/kich-thuoc/form')"
        >
          + Thêm size
        </button>
      </div>

      <div class="body">

        <div class="toolbar">
          <div class="filters">
            <input
              style="width:320px"
              type="text"
              placeholder="Tìm theo mã / size..."
              v-model="searchText"
            />

            <select style="width:220px" v-model="filterStatus">
              <option>Tất cả</option>
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
          </div>

          <div class="filters">
            <select style="width:220px">
              <option>Mới nhất</option>
            </select>
          </div>
        </div>

        <table class="table">
          <thead>
            <tr>
              <th style="width:120px">Mã</th>
              <th>Size</th>
              <th>Mô tả</th>
              <th style="width:170px" class="center">Ngày tạo</th>
              <th style="width:170px" class="center">Ngày sửa</th>
              <th style="width:180px" class="center">Trạng thái</th>
              <th style="width:140px" class="center">Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="kt in filteredData" :key="kt.id">
              <td>{{ kt.maKichThuoc }}</td>

              <td><b>{{ kt.tenKichThuoc }}</b></td>

              <td>
                <span class="muted" style="font-size:13px">{{ kt.ghiChu || '-' }}</span>
              </td>

              <td class="center">{{ formatDateTime(kt.ngayTao) }}</td>

              <td class="center">{{ formatDateTime(kt.ngaySua) }}</td>

              <td class="center">
                <span
                  class="pill"
                  :class="`status-${getAdminStatusTone(kt.trangThai)}`"
                >
                  ● {{ kt.trangThai }}
                </span>
              </td>

              <td class="center">
                <div class="actions">
                  <button
                    class="iconbtn"
                    @click="router.push(`/admin/kich-thuoc/form/${kt.id}`)"
                  >
                    <span class="material-icons-outlined">visibility</span>
                  </button>

                  <button
                    class="iconbtn"
                    @click="remove(kt.id)"
                  >
                    <Trash2 size="16" />
                  </button>
                </div>
              </td>
            </tr>

            <tr v-if="filteredData.length === 0">
              <td colspan="7" style="text-align:center">
                Không có dữ liệu
              </td>
            </tr>
          </tbody>
        </table>

      </div>
    </div>
  </main>
</template>

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

@media (max-width: 1024px) {
  .head { flex-direction: column; align-items: flex-start; gap: 12px; }
}
@media (max-width: 768px) {
  .body { overflow-x: auto; }
  table { min-width: 600px; }
}
</style>