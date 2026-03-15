<script setup>
import { ref, computed, onMounted } from "vue"
import { useRouter } from "vue-router"
import { getAllDanhMuc, deleteDanhMuc } from "../../../services/danhMucService"
import { Pencil, Trash2 } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel } from "../../../utils/adminStatus"

const router = useRouter()

const danhMucs = ref([])
const loading = ref(false)

const filterStatus = ref("Tất cả")
const searchText = ref("")
const sortOption = ref("Mới nhất")

const currentPage = ref(1)
const pageSize = 5

const formatDateTime = (value) => {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString("vi-VN")
}

const loadData = async () => {
  loading.value = true
  const res = await getAllDanhMuc()
  danhMucs.value = res.data
  loading.value = false
}

onMounted(loadData)

const filteredData = computed(() => {
  let data = [...danhMucs.value]

  if (filterStatus.value === "Hoạt động") {
    data = data.filter(d => normalizeAdminStatusLabel(d.trangThai) === "Hoạt động")
  }

  if (filterStatus.value === "Ngừng hoạt động") {
    data = data.filter(d => normalizeAdminStatusLabel(d.trangThai) === "Ngừng hoạt động")
  }

  if (searchText.value.trim() !== "") {
    const keyword = searchText.value.toLowerCase()
    data = data.filter(d =>
      d.maDanhMuc.toLowerCase().includes(keyword) ||
      d.tenDanhMuc.toLowerCase().includes(keyword)
    )
  }

  if (sortOption.value === "Tên A-Z") {
    data.sort((a, b) => a.tenDanhMuc.localeCompare(b.tenDanhMuc))
  }

  if (sortOption.value === "Tên Z-A") {
    data.sort((a, b) => b.tenDanhMuc.localeCompare(a.tenDanhMuc))
  }

  if (sortOption.value === "Mới nhất") {
    data.sort((a, b) => new Date(b.ngayTao) - new Date(a.ngayTao))
  }

  return data
})

const totalPages = computed(() =>
  Math.ceil(filteredData.value.length / pageSize)
)

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredData.value.slice(start, start + pageSize)
})

const goToCreate = () => {
  router.push("/admin/danh-muc/form")
}

const goToEdit = (id) => {
  router.push(`/admin/danh-muc/form/${id}`)
}

const remove = async (id) => {
  const confirmed = await window.confirmDialog("Bạn chắc chắn muốn xoá?")
  if (!confirmed) return
  await deleteDanhMuc(id)
  window.toast.success("Xóa danh mục thành công")
  await loadData()
}
</script>

<template>
  <main class="wrap admin-page">
    <div class="card">
      <div class="head">
        <div>
          <h1 class="page-title">Danh mục</h1>
          <small class="muted page-subtitle">
            Quản lý nhóm sản phẩm (VD: Áo, Quần, Phụ kiện...)
          </small>
        </div>

        <div style="display:flex;gap:10px;flex-wrap:wrap">
          <button class="btn primary" @click="goToCreate">
            + Thêm danh mục
          </button>
        </div>
      </div>

      <div class="body">

        <div class="toolbar">
          <div class="filters">
            <span class="chip">Trạng thái</span>

            <select class="form-select" style="width:220px" v-model="filterStatus">
              <option>Tất cả</option>
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>

            <input
              class="form-input"
              style="width:280px"
              type="text"
              placeholder="Tìm theo mã / tên..."
              v-model="searchText"
            />
          </div>

          <div class="filters">
            <span class="chip">Sắp xếp</span>

            <select class="form-select" style="width:220px" v-model="sortOption">
              <option>Mới nhất</option>
              <option>Tên A-Z</option>
              <option>Tên Z-A</option>
            </select>
          </div>
        </div>

        <table class="table">
          <thead>
            <tr>
              <th style="width:90px">Mã</th>
              <th>Tên danh mục</th>
              <th style="width:140px">Trạng thái</th>
              <th style="width:160px" class="right">Ngày tạo</th>
              <th style="width:160px" class="right">Ngày sửa</th>
              <th style="width:120px; text-align:center">Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="dm in paginatedData" :key="dm.id">
              <td>{{ dm.maDanhMuc }}</td>

              <td>
                <b>{{ dm.tenDanhMuc }}</b>
              </td>

              <td>
                <span
                  class="pill"
                  :class="`status-${getAdminStatusTone(dm.trangThai)}`"
                >
                  ● {{ normalizeAdminStatusLabel(dm.trangThai) }}
                </span>
              </td>

              <td class="right">
                {{ formatDateTime(dm.ngayTao) }}
              </td>

              <td class="right">
                {{ formatDateTime(dm.ngaySua) }}
              </td>

            <td class="action-cell">
              <div class="actions">
                <button class="iconbtn" @click="goToEdit(dm.id)">
                  <Pencil size="16"/>
                </button>

                <button class="iconbtn" @click="remove(dm.id)">
                  <Trash2 size="16"/>
                </button>
              </div>
            </td>
            </tr>

            <tr v-if="paginatedData.length === 0">
              <td colspan="6" style="text-align:center">
                Không có dữ liệu
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination">
          <div>
            Hiển thị
            {{ (currentPage - 1) * pageSize + 1 }}
            –
            {{ Math.min(currentPage * pageSize, filteredData.length) }}
            /
            {{ filteredData.length }}
          </div>

          <div class="pager">
            <button
              class="btn"
              :disabled="currentPage === 1"
              @click="currentPage--"
            >
              ← Trước
            </button>

            <span class="chip">
              {{ currentPage }}
            </span>

            <button
              class="btn"
              :disabled="currentPage === totalPages"
              @click="currentPage++"
            >
              Sau →
            </button>
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

.actions{
  display:flex;
  justify-content:center;
  align-items:center;
  gap:8px;
} 
.action-cell{
  text-align:center;
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