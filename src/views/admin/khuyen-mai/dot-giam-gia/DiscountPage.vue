<template>
  <div class="discount-page">
    <h2 class="page-title">Quản lý giảm giá / Đợt giảm giá</h2>

    <div class="card filter-section">
      <div class="filter-header">
        <div class="filter-title">
          <i class="fa-solid fa-filter"></i> Bộ lọc tìm kiếm
        </div>
      </div>

      <div class="filter-grid">
        <div class="filter-item search-col">
          <label class="label">Từ khóa</label>
          <div class="input-wrapper">
            <i class="fa-solid fa-magnifying-glass search-icon"></i>
            <input v-model="filters.keyword" type="text" class="form-control" placeholder="Tìm theo tên hoặc mã đợt..." />
          </div>
        </div>

        <div class="filter-item">
          <label class="label">Ngày bắt đầu</label>
          <div class="date-wrap">
            <input v-model="filters.startDate" type="date" class="form-control date-native" @click="$event.target.showPicker()" />
            <span class="date-overlay" v-if="filters.startDate">{{ fmtDateDisplay(filters.startDate) }}</span>
            <span class="date-overlay date-placeholder" v-else>dd/mm/yyyy</span>
          </div>
        </div>

        <div class="filter-item">
          <label class="label">Ngày kết thúc</label>
          <div class="date-wrap">
            <input v-model="filters.endDate" type="date" class="form-control date-native" @click="$event.target.showPicker()" />
            <span class="date-overlay" v-if="filters.endDate">{{ fmtDateDisplay(filters.endDate) }}</span>
            <span class="date-overlay date-placeholder" v-else>dd/mm/yyyy</span>
          </div>
        </div>

        <div class="filter-item">
          <label class="label">Trạng thái</label>
          <select v-model="filters.status" class="form-control">
            <option value="">-- Tất cả --</option>
            <option value="active">Đang diễn ra</option>
            <option value="paused">Tạm dừng</option>
            <option value="upcoming">Sắp diễn ra</option>
            <option value="ended">Đã kết thúc</option>
          </select>
        </div>

        <div class="filter-item action-col">
          <div class="btn-group-filter">
            <button class="btn-filter search" @click="fetchDiscounts">
              <i class="fa-solid fa-magnifying-glass"></i>
              <span>Tìm kiếm</span>
            </button>
            <button class="btn-filter reset" @click="resetFilters" title="Làm mới bộ lọc">
              <span class="material-icons-outlined" style="font-size:18px">filter_list_off</span>
              <span>Làm mới bộ lọc</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="card table-section">
      <div class="table-header">
        <div class="header-left">
          <h3>Danh sách đợt giảm giá</h3>
        </div>
        <button class="btn-add" @click="goToAddPage">
          <i class="fa-solid fa-plus"></i> Thêm mới
        </button>
      </div>

      <div class="table-container">
        <table class="custom-table">
          <thead>
            <tr>
              <th class="text-center" width="50">STT</th>
              <th>Mã đợt</th>
              <th>Tên đợt</th>
              <th class="text-center">Giá trị</th>
              <th class="text-center">Loại giảm</th>
              <th class="text-center">Áp dụng</th>
              <th class="text-center">Ngày bắt đầu</th>
              <th class="text-center">Ngày kết thúc</th>
              <th class="text-center">Trạng thái</th>
              <th class="text-center">Hành động</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="filteredList.length === 0">
              <td colspan="10" class="empty-row">
                <img src="https://cdn-icons-png.flaticon.com/512/7486/7486754.png" alt="Empty" width="60" />
                <p>Không tìm thấy dữ liệu phù hợp</p>
              </td>
            </tr>

            <tr v-for="(item, index) in paginatedDiscounts" :key="item.id">
              <td class="text-center">{{ (currentPage - 1) * itemsPerPage + index + 1 }}</td>
              <td>{{ item.maDotGiamGia }}</td>
              <td>{{ item.tenDotGiamGia }}</td>
              <td class="text-center highlight-text">{{ item.giaTriGiamGia + "%" }}</td>
              <td class="text-center"><span class="ss-tag ss-tag-percent">%</span></td>
              <td class="text-center"><span class="applied-badge">{{ appliedLabel(item) }}</span></td>
              <td class="text-center">{{ formatDate(item.ngayBatDau) }}</td>
              <td class="text-center">{{ formatDate(item.ngayKetThuc) }}</td>
              <td class="text-center">
                <span class="ss-badge" :class="item.statusClass">{{ item.statusText }}</span>
              </td>
              <td class="text-center action-cell">
                <div class="d-flex align-items-center justify-content-center action-icons">
                  <button
                    v-if="item.statusKey !== 'ended'"
                    type="button"
                    class="ss-switch"
                    :class="{ 'is-on': item.trangThai }"
                    :title="item.trangThai ? 'Tắt đợt giảm giá' : 'Bật đợt giảm giá'"
                    :aria-checked="item.trangThai ? 'true' : 'false'"
                    role="switch"
                    @click="toggleStatus(item)"
                  >
                    <span class="ss-switch__thumb"></span>
                  </button>
                  <button class="ss-icon-btn-view" type="button" @click="viewDetail(item.id)" title="Xem chi tiết">
                    <span class="material-icons-outlined">visibility</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination" v-if="totalPages > 0">
        <button class="page-btn" @click="changePage(currentPage - 1)" :disabled="currentPage === 1" aria-label="Trang trước">
          <span class="page-btn__arrow">&lt;</span>
        </button>
        <button class="page-btn active">{{ currentPage }}</button>
        <button class="page-btn" @click="changePage(currentPage + 1)" :disabled="currentPage === totalPages" aria-label="Trang sau">
          <span class="page-btn__arrow">&gt;</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { discountService, normalizeDiscountData } from "@/services/dotGiamGiaService"

const router = useRouter()
const route = useRoute()
const basePath = computed(() => (route.path.startsWith("/employee") ? "/employee" : "/admin"))

const rawDiscounts = ref([])
const currentPage = ref(1)
const itemsPerPage = 5

const filters = reactive({ keyword: "", startDate: "", endDate: "", status: "" })

const confirmAction = async (message) => {
  if (window?.confirmDialog) return window.confirmDialog(message)
  return window.confirm(message)
}

const notifySuccess = (message) => { if (window?.toast?.success) window.toast.success(message) }
const notifyError = (message) => { if (window?.toast?.error) window.toast.error(message); else window.alert(message) }

const parseDateAny = (input) => {
  if (!input) return null
  if (Array.isArray(input)) { const [y, m, d] = input; return new Date(y, (m ?? 1) - 1, d ?? 1) }
  if (typeof input === "string" && /^\d{4}-\d{2}-\d{2}$/.test(input)) {
    const [y, m, d] = input.split("-").map(Number); return new Date(y, m - 1, d)
  }
  const date = new Date(input)
  return Number.isNaN(date.getTime()) ? null : date
}

const formatDate = (value) => {
  const date = parseDateAny(value)
  if (!date) return "-"
  const d = String(date.getDate()).padStart(2, "0")
  const m = String(date.getMonth() + 1).padStart(2, "0")
  const y = date.getFullYear()
  return `${d}/${m}/${y}`
}

const fmtDateDisplay = (val) => {
  if (!val) return ""
  const parts = val.split("-")
  if (parts.length !== 3) return val
  return `${parts[2]}/${parts[1]}/${parts[0]}`
}

const appliedLabel = (item) => String(item?.apDungLabel || "Chưa áp dụng")

const applyTargetLabel = (value) => {
  const key = String(value || "ALL").toUpperCase()
  if (key === "HOODIE") return "Hoodie"
  if (key === "BOMBER") return "Bomber"
  if (key === "COACH") return "Coach"
  return "Tất cả sản phẩm"
}

const codeNumber = (value) => {
  const digits = String(value || "").replace(/\D+/g, "")
  const parsed = Number(digits)
  return Number.isFinite(parsed) ? parsed : Number.MAX_SAFE_INTEGER
}

const sortDiscountsForList = (rows) => {
  return [...rows].sort((a, b) => {
    const byCode = codeNumber(a?.maDotGiamGia || a?.maKhuyenMai) - codeNumber(b?.maDotGiamGia || b?.maKhuyenMai)
    if (byCode !== 0) return byCode
    return String(a?.maDotGiamGia || a?.maKhuyenMai || "").localeCompare(String(b?.maDotGiamGia || b?.maKhuyenMai || ""), undefined, { numeric: true, sensitivity: "base" })
  })
}

const fetchDiscounts = async () => {
  try {
    const data = await discountService.getAll()
    let allVariants = []
    try {
      allVariants = await discountService.getAllProductDetails()
    } catch {
      allVariants = []
    }

    const totalProducts = new Set(
      (Array.isArray(allVariants) ? allVariants : [])
        .map((item) => Number(item?.idSanPham || 0))
        .filter((id) => Number.isFinite(id) && id > 0)
    ).size

    const normalizedItems = (Array.isArray(data) ? data : []).map((item) => normalizeDiscountData(item))
    const associatedProductsByCampaign = await Promise.all(
      normalizedItems.map(async (item) => {
        try {
          const products = await discountService.getAssociatedProducts(item.id)
          return {
            id: item.id,
            count: Array.isArray(products) ? products.length : 0
          }
        } catch {
          return {
            id: item.id,
            count: Number(item?.soLuongSanPhamApDung || 0)
          }
        }
      })
    )

    const appliedCountMap = new Map(associatedProductsByCampaign.map((entry) => [entry.id, Number(entry.count || 0)]))
    const now = new Date()
    const parseDateBoundary = (value, isEnd) => {
      const d = parseDateAny(value) || new Date()
      d.setHours(isEnd ? 23 : 0, isEnd ? 59 : 0, isEnd ? 59 : 0, isEnd ? 999 : 0)
      return d
    }
    rawDiscounts.value = normalizedItems
      .map((item) => {
        const appliedCount = Number(appliedCountMap.get(item.id) ?? item?.soLuongSanPhamApDung ?? 0)
        const start = parseDateBoundary(item.ngayBatDau, false)
        const end = parseDateBoundary(item.ngayKetThuc, true)
        let statusKey = "active", statusText = "Đang diễn ra", statusClass = "status-active"
        if (now > end) {
          statusKey = "ended"; statusText = "Đã kết thúc"; statusClass = "status-ended"
        } else if (!item.trangThai) {
          statusKey = "paused"; statusText = "Tạm dừng"; statusClass = "status-ended"
        } else if (now < start) {
          statusKey = "upcoming"; statusText = "Sắp diễn ra"; statusClass = "status-upcoming"
        }

        const apDungLabel = applyTargetLabel(item?.doiTuongApDung)

        return {
          ...item,
          soLuongSanPhamApDung: appliedCount,
          apDungLabel,
          statusKey,
          statusText,
          statusClass
        }
      })
  } catch (error) {
    console.error("Load campaign failed", error)
    notifyError("Không thể tải danh sách đợt giảm giá")
  }
}

const filteredList = computed(() => {
  const kw = String(filters.keyword || "").trim().toLowerCase()
  const rows = rawDiscounts.value.filter((item) => {
    const keywordMatch = !kw || String(item.tenDotGiamGia || "").toLowerCase().includes(kw) || String(item.maDotGiamGia || "").toLowerCase().includes(kw)
    const startFilter = filters.startDate ? parseDateAny(filters.startDate) : null
    const endFilter = filters.endDate ? parseDateAny(filters.endDate) : null
    const start = parseDateAny(item.ngayBatDau), end = parseDateAny(item.ngayKetThuc)
    const startMatch = !startFilter || (start && start >= startFilter)
    const endMatch = !endFilter || (end && end <= endFilter)
    const statusMatch = !filters.status || item.statusKey === filters.status
    return keywordMatch && startMatch && endMatch && statusMatch
  })
  return sortDiscountsForList(rows)
})

const totalPages = computed(() => Math.ceil(filteredList.value.length / itemsPerPage))
const paginatedDiscounts = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return filteredList.value.slice(start, start + itemsPerPage)
})
const changePage = (page) => { if (page >= 1 && page <= totalPages.value) currentPage.value = page }

watch(filters, () => { currentPage.value = 1 }, { deep: true })

const resetFilters = () => {
  filters.keyword = ""; filters.startDate = ""; filters.endDate = ""; filters.status = ""
  fetchDiscounts()
}
const goToAddPage = () => { router.push(`${basePath.value}/khuyen-mai/form`) }
const viewDetail = (id) => { router.push(`${basePath.value}/khuyen-mai/form/${id}`) }

const toggleStatus = async (item) => {
  const newStatus = !item.trangThai
  const action = newStatus ? "kích hoạt" : "ngừng kích hoạt"
  const confirmed = await confirmAction(`Bạn có chắc muốn ${action} đợt giảm giá "${item.tenDotGiamGia}"?`)
  if (!confirmed) return
  try {
    await discountService.update(item.id, {
      maDotGiamGia: item.maDotGiamGia,
      tenDotGiamGia: item.tenDotGiamGia,
      giaTriGiamGia: item.giaTriGiamGia,
      ngayBatDau: item.ngayBatDau,
      ngayKetThuc: item.ngayKetThuc,
      doiTuongApDung: item.doiTuongApDung,
      trangThai: newStatus
    })
    notifySuccess("Cập nhật trạng thái đợt giảm giá thành công")
    item.trangThai = newStatus
  } catch (error) {
    console.error(error)
    notifyError("Lỗi cập nhật trạng thái đợt giảm giá")
  }
}

onMounted(fetchDiscounts)
</script>

<style scoped>
* { box-sizing: border-box; }
.discount-page { padding-bottom: 30px; }

.page-title {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 30px;
  margin-top: 10px;
  color: rgba(17, 24, 39, 0.92);
}

.card {
  background: #fff;
  border-radius: 18px;
  padding: 24px;
  margin-bottom: 24px;
  border: 1px solid rgba(17, 24, 39, 0.08);
  box-shadow: 0 1px 3px rgba(17, 24, 39, 0.08), 0 8px 24px rgba(17, 24, 39, 0.04);
}

.filter-header {
  margin-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 12px;
}
.filter-title {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr) auto;
  gap: 20px;
  align-items: end;
}

.label {
  font-size: 13px;
  font-weight: 500;
  color: rgba(17, 24, 39, 0.65);
  margin-bottom: 8px;
  display: block;
}

.form-control,
.input-wrapper input {
  height: 42px;
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 0 12px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
  background-color: #fff;
  color: #334155;
}
.form-control:focus,
.input-wrapper input:focus {
  border-color: #ff4d4f;
  box-shadow: 0 0 0 3px rgba(255, 77, 79, 0.12);
}

/* Date DD/MM/YYYY overlay */
.date-wrap { position: relative; }
.date-native { color: transparent !important; }
.date-native::-webkit-datetime-edit { color: transparent; }
.date-native::-webkit-calendar-picker-indicator { position: relative; z-index: 2; cursor: pointer; }
.date-overlay { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); pointer-events: none; font-size: 14px; color: #334155; }
.date-placeholder { color: #94a3b8; }

.input-wrapper { position: relative; width: 100%; }
.input-wrapper input { padding-left: 36px; }
.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(17, 24, 39, 0.45);
  font-size: 14px;
  pointer-events: none;
}

.btn-group-filter { display: flex; gap: 10px; }
.btn-filter {
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 10px;
  font-weight: 500;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid transparent;
  padding: 0 20px;
  transition: 0.2s;
}
.btn-filter.reset { background: #f1f5f9; color: #64748b; padding: 0 14px; }
.btn-filter.reset:hover { background: #e2e8f0; color: #ef4444; }
.btn-filter.search {
  background: linear-gradient(90deg, #7c1021 0%, #a9162d 35%, #cf243a 68%, #e64555 100%);
  color: #fff;
  box-shadow: 0 10px 18px rgba(185, 24, 45, 0.22);
  border: none;
}
.btn-filter.search:hover { filter: brightness(0.97); }

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.header-left h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: rgba(17, 24, 39, 0.92);
}

.btn-add {
  background: linear-gradient(90deg, #7c1021 0%, #a9162d 35%, #cf243a 68%, #e64555 100%);
  color: white;
  border: none;
  padding: 0 18px;
  height: 40px;
  border-radius: 12px;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: 0.2s;
  box-shadow: 0 10px 18px rgba(185, 24, 45, 0.22);
}
.btn-add:hover { filter: brightness(0.97); }

.table-container {
  overflow-x: auto;
  border: 1px solid rgba(17, 24, 39, 0.08);
  border-radius: 14px;
  background: #fff;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  min-width: 900px;
}
.custom-table thead tr {
  background: linear-gradient(90deg, #7c1021 0%, #a9162d 35%, #cf243a 68%, #e64555 100%);
}
.custom-table th {
  background: transparent;
  color: #fff;
  font-weight: 500;
  font-size: 13px;
  letter-spacing: 0.3px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.06);
  white-space: nowrap;
}
.custom-table thead th:first-child { border-top-left-radius: 10px; }
.custom-table thead th:last-child { border-top-right-radius: 10px; }
.custom-table td {
  padding: 14px 16px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.05);
  vertical-align: middle;
  color: rgba(17, 24, 39, 0.82);
}
.custom-table tbody tr:hover td { background: rgba(17, 24, 39, 0.015); }
.custom-table tbody tr:last-child td { border-bottom: none; }

.text-center { text-align: center; }
.highlight-text { font-weight: 500; color: rgba(17, 24, 39, 0.92); }

.ss-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 400;
  border: 1px solid rgba(17, 24, 39, 0.14);
  background: rgba(17, 24, 39, 0.06);
  color: rgba(17, 24, 39, 0.88);
}

.applied-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  background: rgba(255, 77, 79, 0.08);
  color: #b42324;
  border: 1px solid rgba(255, 77, 79, 0.24);
}

.ss-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
  border: 1px solid transparent;
}
.status-active { background: rgba(255, 77, 79, 0.10); color: #b42324; border-color: rgba(255, 77, 79, 0.28); }
.status-upcoming { background: #fff; color: rgba(17, 24, 39, 0.86); border-color: rgba(255, 77, 79, 0.28); }
.status-ended { background: rgba(17, 24, 39, 0.06); color: rgba(17, 24, 39, 0.72); border-color: rgba(17, 24, 39, 0.12); }

.empty-row {
  text-align: center;
  padding: 40px !important;
  color: rgba(17, 24, 39, 0.55);
}
.empty-row img { margin-bottom: 10px; opacity: 0.5; display: block; margin-left: auto; margin-right: auto; }

.action-cell { white-space: nowrap; }

.action-icons {
  gap: 14px;
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
}
.page-btn {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(17, 24, 39, 0.14);
  background: white;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: 0.2s;
  color: rgba(17, 24, 39, 0.80);
}
.page-btn:hover { background: rgba(17, 24, 39, 0.04); border-color: rgba(17, 24, 39, 0.18); }
.page-btn.active { background: #c81f35; color: white; border-color: #c81f35; }
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.page-btn__arrow {
  font-size: 16px;
  line-height: 1;
  font-weight: 700;
}

.ss-switch {
  position: relative;
  width: 44px;
  height: 24px;
  border-radius: 999px;
  border: 1px solid rgba(17, 24, 39, 0.18);
  background: rgba(17, 24, 39, 0.12);
  transition: all 0.2s ease;
  cursor: pointer;
  padding: 0;
}

.ss-switch__thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(17, 24, 39, 0.28);
  transition: transform 0.2s ease;
}

.ss-switch.is-on {
  background: linear-gradient(90deg, #ff4d4f 0%, #d42525 100%);
  border-color: rgba(212, 37, 37, 0.75);
}

.ss-switch.is-on .ss-switch__thumb {
  transform: translateX(20px);
}

.ss-icon-btn-view {
  width: 34px;
  height: 34px;
  border: 1px solid rgba(17, 24, 39, 0.12);
  background: #fff;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: 0.2s;
  color: rgba(17, 24, 39, 0.72);
}
.ss-icon-btn-view:hover {
  background: rgba(255, 77, 79, 0.06);
  border-color: rgba(255, 77, 79, 0.3);
  color: #ff4d4f;
}
.ss-icon-btn-view .material-icons-outlined { font-size: 18px; }

@media (max-width: 1200px) {
  .filter-grid { grid-template-columns: repeat(2, 1fr); }
  .search-col { grid-column: span 2; }
  .action-col { grid-column: span 2; justify-self: end; }
}
@media (max-width: 768px) {
  .filter-grid { grid-template-columns: 1fr; }
  .search-col, .action-col { grid-column: span 1; }
  .action-col { justify-self: stretch; }
  .action-col .filter-actions { flex-direction: column; }
  .discount-page .ss-table { min-width: 800px; }
  .discount-page .head { flex-direction: column; align-items: flex-start; gap: 12px; }
}
</style>