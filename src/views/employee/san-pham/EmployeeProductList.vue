<script setup>
import { computed, onMounted, ref } from "vue"
import { getAllSanPham } from "../../../services/sanPhamService"
import { getAdminStatusTone, normalizeAdminStatusLabel } from "../../../utils/adminStatus"

const search = ref("")
const list = ref([])
const loading = ref(false)

function formatCurrency(value) {
  return new Intl.NumberFormat("vi-VN").format(value || 0) + "₫"
}

function formatDateTime(value) {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString("vi-VN")
}

async function loadData() {
  loading.value = true
  try {
    const res = await getAllSanPham()
    const rows = Array.isArray(res?.data) ? res.data : []

    list.value = rows.map((item) => {
      const variants = Array.isArray(item?.sanPhamChiTiets) ? item.sanPhamChiTiets : []
      const firstVariant = variants[0] || null
      const totalTon = variants.reduce((sum, v) => sum + Number(v?.soLuong || 0), 0)
      const hasVariants = variants.length > 0

      const gia = firstVariant?.giaBan ?? item?.giaBan ?? item?.gia ?? null
      const ton = hasVariants
        ? totalTon
        : (Number.isFinite(Number(item?.soLuong ?? item?.ton)) ? Number(item?.soLuong ?? item?.ton) : null)

      return {
        id: item?.id,
        ma: item?.maSanPham,
        name: item?.tenSanPham,
        description: item?.moTa,
        loai: firstVariant?.loai?.tenLoai || item?.loai || "Khác",
        gia,
        ton,
        hasVariants,
        ngayTao: item?.ngayTao,
        ngaySua: item?.ngaySua,
        status: normalizeAdminStatusLabel(item?.trangThai)
      }
    })
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const filteredList = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  if (!keyword) return list.value

  return list.value.filter((item) => {
    return String(item?.ma || "").toLowerCase().includes(keyword)
      || String(item?.name || "").toLowerCase().includes(keyword)
      || String(item?.loai || "").toLowerCase().includes(keyword)
  })
})

const groupedProducts = computed(() => {
  const groups = {}
  filteredList.value.forEach((item) => {
    const key = item.loai || "Khác"
    if (!groups[key]) groups[key] = []
    groups[key].push(item)
  })
  return groups
})
</script>

<template>
  <section class="employee-product-page card">
    <div class="head page-header">
      <div>
        <h1 class="page-title">Sản phẩm</h1>
        <small class="muted page-subtitle">
          Danh sách sản phẩm và tồn kho để theo dõi bán hàng theo ca
        </small>
      </div>

      <span class="readonly-pill">Chế độ xem của nhân viên</span>
    </div>

    <div class="body">
      <div class="toolbar">
        <input
          v-model="search"
          class="form-input search-input"
          type="text"
          placeholder="Tìm theo mã / tên / loại..."
        />
      </div>

      <div v-if="loading" class="empty">
        Đang tải dữ liệu...
      </div>

      <div
        v-for="(items, type) in groupedProducts"
        :key="type"
        class="category-frame"
      >
        <div class="category-header">{{ type }}</div>

        <table class="data-table">
          <thead>
            <tr>
              <th style="width:120px">Mã</th>
              <th>Sản phẩm</th>
              <th style="width:140px" class="right">Giá</th>
              <th style="width:120px" class="right">Tồn</th>
              <th style="width:170px">Ngày tạo</th>
              <th style="width:170px">Ngày sửa</th>
              <th style="width:140px">Trạng thái</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.ma }}</td>
              <td>
                <b>{{ item.name }}</b>
                <div class="muted product-desc">{{ item.description }}</div>
                <span v-if="!item.hasVariants" class="warn-inline">Chưa có biến thể</span>
              </td>
              <td class="right">
                <span v-if="item.gia === null" class="muted">-</span>
                <template v-else>{{ formatCurrency(item.gia) }}</template>
              </td>
              <td class="right">
                <span v-if="item.ton === null" class="muted">-</span>
                <template v-else>{{ item.ton }}</template>
              </td>
              <td>{{ formatDateTime(item.ngayTao) }}</td>
              <td>{{ formatDateTime(item.ngaySua) }}</td>
              <td>
                <span class="pill" :class="`status-${getAdminStatusTone(item.status)}`">
                  ● {{ item.status }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="!loading && filteredList.length === 0" class="empty">
        Không có dữ liệu phù hợp
      </div>
    </div>
  </section>
</template>

<style scoped>
.employee-product-page {
  border: 1px solid #e5eaf4;
  border-radius: 20px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.04);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-title {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 800;
}

.page-subtitle {
  display: block;
  margin-top: 8px;
}

.readonly-pill {
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 700;
}

.toolbar {
  margin-bottom: 14px;
}

.search-input {
  width: 360px;
  max-width: 100%;
}

.form-input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #fff;
  font-size: 14px;
}

.form-input:focus {
  outline: none;
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.2);
}

.category-frame {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 14px;
  background: #fff;
}

.category-header {
  background: #f8fafc;
  padding: 14px 16px;
  font-weight: 700;
  font-size: 15px;
  color: #334155;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 13px 16px;
  border-bottom: 1px solid #eef2f7;
  text-align: left;
}

.data-table thead th {
  color: #94a3b8;
  background: #f8fafc;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
}

.data-table tbody td {
  color: #334155;
}

.product-desc {
  font-size: 12px;
  margin-top: 3px;
  color: #94a3b8;
}

.warn-inline {
  margin-top: 6px;
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  color: #a16207;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 999px;
  padding: 2px 8px;
}

.right {
  text-align: right;
}

.empty {
  text-align: center;
  padding: 40px 12px;
  color: #64748b;
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
