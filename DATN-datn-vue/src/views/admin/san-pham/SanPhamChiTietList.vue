<script setup>
import { ref, computed, onMounted } from "vue"
import { useRouter } from "vue-router"
import { getAllSanPham, deleteSanPhamChiTiet } from "../../../services/sanPhamService"
import { getImageUrl } from "../../../services/uploadService"

const router = useRouter()
const loading = ref(false)
const allVariants = ref([])

// Bộ lọc
const filterKeyword = ref("")
const filterTrangThai = ref("Tất cả")
const filterMauSac = ref("Tất cả")
const filterKichThuoc = ref("Tất cả")
const filterLoai = ref("Tất cả")
const filterGiaMin = ref(0)
const filterGiaMax = ref(10000000)

async function loadData() {
  loading.value = true
  try {
    const res = await getAllSanPham()
    const products = Array.isArray(res?.data) ? res.data : []
    const list = []
    for (const product of products) {
      const variants = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []
      for (const v of variants) {
        list.push({
          id: v.id,
          sanPhamId: product.id,
          maSP: product.maSanPham || "-",
          maCTSP: v.ma || "-",
          tenSanPham: product.tenSanPham || "-",
          mauSac: v.mauSac?.tenMau || "-",
          kichThuoc: v.kichThuoc?.tenKichThuoc || "-",
          loaiSan: v.loai?.tenLoai || "-",
          soLuong: Number(v.soLuong || 0),
          giaBan: Number(v.giaBan || 0),
          trangThai: v.trangThai || "Hoạt động",
          hinhAnh: v.hinhAnh || null
        })
      }
    }
    allVariants.value = list
  } catch (err) {
    console.error(err)
    window.toast?.error("Lỗi tải dữ liệu")
  } finally {
    loading.value = false
  }
}

// Danh sách unique cho dropdown
const mauSacOptions = computed(() => ["Tất cả", ...new Set(allVariants.value.map(v => v.mauSac).filter(v => v !== "-"))])
const kichThuocOptions = computed(() => ["Tất cả", ...new Set(allVariants.value.map(v => v.kichThuoc).filter(v => v !== "-"))])
const loaiOptions = computed(() => ["Tất cả", ...new Set(allVariants.value.map(v => v.loaiSan).filter(v => v !== "-"))])
const maxGia = computed(() => Math.max(10000000, ...allVariants.value.map(v => v.giaBan)))

const filteredVariants = computed(() => {
  const kw = filterKeyword.value.trim().toLowerCase()
  return allVariants.value.filter(v => {
    if (kw && !v.maSP.toLowerCase().includes(kw) && !v.maCTSP.toLowerCase().includes(kw) && !v.tenSanPham.toLowerCase().includes(kw)) return false
    if (filterTrangThai.value !== "Tất cả" && v.trangThai !== filterTrangThai.value) return false
    if (filterMauSac.value !== "Tất cả" && v.mauSac !== filterMauSac.value) return false
    if (filterKichThuoc.value !== "Tất cả" && v.kichThuoc !== filterKichThuoc.value) return false
    if (filterLoai.value !== "Tất cả" && v.loaiSan !== filterLoai.value) return false
    if (v.giaBan < filterGiaMin.value || v.giaBan > filterGiaMax.value) return false
    return true
  })
})

function resetFilter() {
  filterKeyword.value = ""
  filterTrangThai.value = "Tất cả"
  filterMauSac.value = "Tất cả"
  filterKichThuoc.value = "Tất cả"
  filterLoai.value = "Tất cả"
  filterGiaMin.value = 0
  filterGiaMax.value = maxGia.value
}

function editVariant(v) {
  router.push(`/admin/san-pham/form/${v.sanPhamId}?color=${encodeURIComponent(v.mauSac)}`)
}

async function deleteVariant(v) {
  const ok = await window.confirmDialog?.(`Xóa biến thể ${v.maCTSP}?`)
  if (!ok) return
  try {
    await deleteSanPhamChiTiet(v.id)
    allVariants.value = allVariants.value.filter(item => item.id !== v.id)
    window.toast?.success(`Đã xóa biến thể ${v.maCTSP}`)
  } catch (err) {
    window.toast?.error("Xóa thất bại: " + (err.response?.data?.message || err.message))
  }
}

function goBack() {
  router.push("/admin/san-pham/list")
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h1>Quản lý Biến thể Sản phẩm</h1>
        <p class="subtitle">Xem và quản lý tất cả biến thể sản phẩm (size × màu)</p>
      </div>
      <div class="header-actions">
        <button class="btn outline" @click="goBack">← Quay lại</button>
        <button class="btn primary" @click="loadData">⚡ Làm mới</button>
      </div>
    </div>

    <!-- Bộ lọc -->
    <div class="filter-box">
      <div class="filter-title">🔍 Bộ lọc tìm kiếm</div>
      <div class="filter-row">
        <div class="filter-field wide">
          <label>Tìm kiếm</label>
          <div class="search-input">
            <span>🔍</span>
            <input v-model="filterKeyword" type="text" placeholder="Tìm theo mã SP / mã CTSP / tên sản phẩm..." />
          </div>
        </div>
        <div class="filter-field">
          <label>Trạng thái</label>
          <select v-model="filterTrangThai">
            <option>Tất cả</option>
            <option>Hoạt động</option>
            <option>Ngừng hoạt động</option>
          </select>
        </div>
      </div>
      <div class="filter-row">
        <div class="filter-field">
          <label>Màu sắc</label>
          <select v-model="filterMauSac">
            <option v-for="o in mauSacOptions" :key="o">{{ o }}</option>
          </select>
        </div>
        <div class="filter-field">
          <label>Kích thước</label>
          <select v-model="filterKichThuoc">
            <option v-for="o in kichThuocOptions" :key="o">{{ o }}</option>
          </select>
        </div>
        <div class="filter-field">
          <label>Loại sản phẩm</label>
          <select v-model="filterLoai">
            <option v-for="o in loaiOptions" :key="o">{{ o }}</option>
          </select>
        </div>
      </div>
      <div class="filter-row">
        <div class="filter-field wide">
          <label>Khoảng giá: {{ filterGiaMin.toLocaleString('vi-VN') }}₫ – {{ filterGiaMax.toLocaleString('vi-VN') }}₫</label>
          <div class="range-row">
            <input type="number" v-model.number="filterGiaMin" placeholder="Giá từ" min="0" />
            <span>–</span>
            <input type="number" v-model.number="filterGiaMax" placeholder="Giá đến" min="0" />
          </div>
        </div>
        <div class="filter-field align-end">
          <button class="btn danger" @click="resetFilter">✕ Đặt lại bộ lọc</button>
        </div>
      </div>
    </div>

    <!-- Bảng -->
    <div class="table-label">Danh sách chi tiết sản phẩm</div>

    <div v-if="loading" class="loading">Đang tải...</div>
    <div v-else-if="!filteredVariants.length" class="empty">Không có biến thể nào phù hợp</div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>STT</th>
          <th>Mã SP</th>
          <th>Mã CTSP</th>
          <th>Tên sản phẩm</th>
          <th>Màu sắc</th>
          <th>Kích thước</th>
          <th>Loại sản phẩm</th>
          <th>SL tồn</th>
          <th>Giá bán</th>
          <th>Trạng thái</th>
          <th>Hành động</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(v, idx) in filteredVariants" :key="v.id">
          <td>{{ idx + 1 }}</td>
          <td>{{ v.maSP }}</td>
          <td>{{ v.maCTSP }}</td>
          <td>{{ v.tenSanPham }}</td>
          <td>{{ v.mauSac }}</td>
          <td>{{ v.kichThuoc }}</td>
          <td>{{ v.loaiSan }}</td>
          <td>{{ v.soLuong }}</td>
          <td>{{ v.giaBan.toLocaleString('vi-VN') }} ₫</td>
          <td>
            <span class="badge" :class="v.trangThai === 'Hoạt động' ? 'badge-active' : 'badge-inactive'">
              {{ v.trangThai }}
            </span>
          </td>
          <td class="actions">
            <button class="btn-icon" @click="editVariant(v)" title="Chỉnh sửa">✏️</button>
            <button class="btn-icon" @click="deleteVariant(v)" title="Xóa">🗑️</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.page {
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-header h1 { margin: 0 0 4px; font-size: 20px; font-weight: 700; color: #1e293b; }
.subtitle { margin: 0; font-size: 13px; color: #94a3b8; }

.header-actions { display: flex; gap: 10px; }

.btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid transparent;
  font-weight: 500;
  transition: all 0.2s;
}
.btn.outline { background: #fff; border-color: #e2e8f0; color: #475569; }
.btn.outline:hover { border-color: #94a3b8; }
.btn.primary { background: #dc2626; color: #fff; border-color: #dc2626; }
.btn.primary:hover { background: #b91c1c; }
.btn.danger { background: #1e293b; color: #fff; border-color: #1e293b; }
.btn.danger:hover { background: #0f172a; }

/* Filter box */
.filter-box {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 20px;
  background: #f8fafc;
}
.filter-title { font-weight: 600; font-size: 14px; color: #475569; margin-bottom: 12px; }
.filter-row { display: flex; gap: 16px; margin-bottom: 12px; flex-wrap: wrap; }
.filter-field { display: flex; flex-direction: column; gap: 6px; min-width: 160px; flex: 1; }
.filter-field.wide { flex: 2; }
.filter-field.align-end { justify-content: flex-end; }
.filter-field label { font-size: 13px; color: #64748b; font-weight: 500; }
.filter-field select,
.filter-field input[type="number"] {
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  background: #fff;
  color: #334155;
}
.search-input {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 12px;
  background: #fff;
}
.search-input input { border: none; outline: none; font-size: 13px; color: #334155; width: 100%; background: transparent; }
.range-row { display: flex; align-items: center; gap: 8px; }
.range-row input { flex: 1; }
.range-row span { color: #94a3b8; }

.table-label { font-weight: 600; font-size: 14px; color: #1e293b; margin-bottom: 12px; }

.table { width: 100%; border-collapse: collapse; font-size: 14px; }
.table th {
  padding: 12px 14px;
  text-align: left;
  font-weight: 600;
  color: #64748b;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 2px solid #f1f5f9;
}
.table td { padding: 13px 14px; color: #334155; border-bottom: 1px solid #f8fafc; }
.table tbody tr:hover { background: #f8fafc; }

.badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}
.badge-active { background: #dcfce7; color: #16a34a; }
.badge-inactive { background: #fee2e2; color: #dc2626; }

.actions { display: flex; gap: 6px; }
.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 15px;
  padding: 4px 6px;
  border-radius: 4px;
  transition: background 0.15s;
}
.btn-icon:hover { background: #f1f5f9; }

.loading, .empty { text-align: center; padding: 48px; color: #94a3b8; font-size: 14px; }

.thumb {
  width: 44px;
  height: 44px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.no-img { color: #cbd5e1; font-size: 18px; }
</style>
