<script setup>
import { ref, onMounted, computed } from "vue"
import { useRouter } from "vue-router"
import { getAllLoai, deleteLoai } from "../../../services/loaiService"
import { Pencil, Trash2 } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel } from "../../../utils/adminStatus"

const router = useRouter()
const list = ref([])

const searchText = ref("")
const filterStatus = ref("Tất cả trạng thái")
const sortOption = ref("Mới nhất")

const formatDateTime = (value) => {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString("vi-VN")
}

async function loadData() {
  const res = await getAllLoai()
  list.value = (res.data || []).map((item) => ({
    ...item,
    trangThai: normalizeAdminStatusLabel(item.trangThai)
  }))
}
onMounted(loadData)

async function remove(id) {
  await deleteLoai(id)
  loadData()
}

const filteredData = computed(() => {
  let data = [...list.value]

  if (searchText.value.trim()) {
    const keyword = searchText.value.toLowerCase()
    data = data.filter(l =>
      l.maLoai?.toLowerCase().includes(keyword) ||
      l.tenLoai?.toLowerCase().includes(keyword)
    )
  }

  if (filterStatus.value !== "Tất cả trạng thái") {
    data = data.filter(l => l.trangThai === filterStatus.value)
  }

  if (sortOption.value === "Tên A-Z") {
    data.sort((a, b) => a.tenLoai.localeCompare(b.tenLoai))
  }

  if (sortOption.value === "Mới nhất") {
    data.sort((a, b) => b.id - a.id)
  }

  return data
})
</script>

<template>
  <main class="wrap admin-page">
    <div class="card">

      <div class="head">
        <div>
          <h1 class="page-title">Loại</h1>
          <small class="muted page-subtitle">
            Ví dụ: Slim fit / Regular / Oversize...
          </small>
        </div>

        <button
          class="btn primary"
          @click="router.push('/admin/loai/form')"
        >
          + Thêm loại
        </button>
      </div>

      <div class="body">

        <div class="toolbar">
          <div class="filters">
            <input
              class="form-input"
              style="width:320px"
              type="text"
              placeholder="Tìm theo mã / tên..."
              v-model="searchText"
            />

            <select class="form-select" style="width:220px" v-model="filterStatus">
              <option>Tất cả trạng thái</option>
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
          </div>

          <div class="filters">
            <select class="form-select" style="width:220px" v-model="sortOption">
              <option>Mới nhất</option>
              <option>Tên A-Z</option>
            </select>
          </div>
        </div>

        <table class="table">
          <thead>
            <tr>
              <th style="width:100px">Mã</th>
              <th>Tên loại</th>
              <th style="width:170px" class="center">Ngày tạo</th>
              <th style="width:170px" class="center">Ngày sửa</th>
              <th style="width:180px" class="center">Trạng thái</th>
              <th style="width:180px" class="center">Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="l in filteredData" :key="l.id">
              <td>{{ l.maLoai }}</td>

              <td>
                <b>{{ l.tenLoai }}</b>
              </td>

              <td class="center">{{ formatDateTime(l.ngayTao) }}</td>

              <td class="center">{{ formatDateTime(l.ngaySua) }}</td>

              <td class="center">
                <span
                  class="pill"
                  :class="`status-${getAdminStatusTone(l.trangThai)}`"
                >
                  ● {{ l.trangThai }}
                </span>
              </td>

              <td class="center">
                <div class="actions">
                  <button
                    class="iconbtn"
                    @click="router.push(`/admin/loai/form/${l.id}`)"
                  >
                    <Pencil size="16" />
                  </button>

                  <button
                    class="iconbtn"
                    @click="remove(l.id)"
                  >
                    <Trash2 size="16" />
                  </button>
                </div>
              </td>
            </tr>

            <tr v-if="filteredData.length === 0">
              <td colspan="6" style="text-align:center">
                Không có dữ liệu
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination">
          <div>
            Hiển thị 1–{{ filteredData.length }} / {{ filteredData.length }}
          </div>

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

<style scoped>
.admin-page {
  font-family: 'Be Vietnam Pro', 'Segoe UI', Tahoma, sans-serif;
  background: #f6f8fb;
}

.page-title {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.1;
  letter-spacing: -0.03em;
  font-weight: 800;
}

.page-subtitle {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 15px;
  font-weight: 500;
}

.form-input,
.form-select {
  height: 40px;
}

.center {
  text-align: center;
  vertical-align: middle;
}

.actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.iconbtn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.pill.ok {
  background: #dcfce7;
  color: #166534;
}

.pill.danger {
  background: #fee2e2;
  color: #991b1b;
}

@media (max-width: 1024px) {
  .page-title {
    font-size: 20px;
  }
}

@media (max-width: 768px) {
  .page-title {
    font-size: 18px;
  }
}
</style>