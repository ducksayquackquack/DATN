<script setup>
import { computed, onMounted, ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import { getAllVouchers, normalizeVoucherData } from "../../../services/khuyenMaiService"

const router = useRouter()
const route = useRoute()
const basePath = computed(() => (route.path.startsWith("/employee") ? "/employee" : "/admin"))

const list = ref([])
const loading = ref(false)
const keyword = ref("")
const statusFilter = ref("")

const toBoolean = (value) => {
  if (typeof value === "boolean") return value
  if (typeof value === "number") return value === 1
  if (typeof value === "string") {
    const normalized = value.trim().toLowerCase()
    return normalized === "true" || normalized === "1" || normalized === "yes"
  }
  return false
}

const toPggCode = (voucher, index = 0) => {
  const raw = String(voucher?.maPhieuGiamGia || "").trim().toUpperCase()
  if (/^PGG\d{3,}$/.test(raw)) return raw
  const idCandidate = Number(voucher?.id ?? voucher?.phieuGiamGiaId ?? voucher?.voucherId ?? (index + 1))
  if (Number.isFinite(idCandidate) && idCandidate > 0) {
    return `PGG${String(idCandidate).padStart(3, "0")}`
  }
  return raw || "-"
}

const parseDate = (value) => {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

const formatDate = (value) => {
  const date = parseDate(value)
  if (!date) return "-"
  const d = String(date.getDate()).padStart(2, "0")
  const m = String(date.getMonth() + 1).padStart(2, "0")
  const y = date.getFullYear()
  return `${d}/${m}/${y}`
}

const formatCurrency = (value) => {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(Number(value || 0))
}

const getStatus = (voucher) => {
  const now = new Date()
  const start = parseDate(voucher?.ngayBatDau)
  const end = parseDate(voucher?.ngayKetThuc)
  // Use date ranges as source of truth — backend trangThai is unreliable (returns false/0 for active vouchers)
  if (end && now > end) return { key: "ended", text: "Ngừng hoạt động" }
  if (start && now < start) return { key: "upcoming", text: "Đang xử lý" }
  return { key: "active", text: "Hoạt động" }
}

const normalizedVouchers = computed(() => {
  return list.value.map((voucher) => {
    const normalized = normalizeVoucherData(voucher)
    return {
      ...normalized,
      id: voucher?.id ?? voucher?.phieuGiamGiaId ?? voucher?.voucherId ?? voucher?.maPhieuGiamGia,
      loaiPhieuGiamGia: toBoolean(voucher?.loaiPhieuGiamGia ?? voucher?.caNhan ?? voucher?.isPersonal),
      hinhThucGiam: toBoolean(voucher?.hinhThucGiam ?? voucher?.giamTheoPhanTram ?? voucher?.isPercent)
    }
  })
})

const filtered = computed(() => {
  let rows = normalizedVouchers.value
  const kw = keyword.value.trim().toLowerCase()

  if (kw) {
    rows = rows.filter((voucher, index) => {
      return (
        String(voucher?.tenPhieuGiamGia || "").toLowerCase().includes(kw)
        || toPggCode(voucher, index).toLowerCase().includes(kw)
      )
    })
  }

  if (statusFilter.value) {
    rows = rows.filter((voucher) => getStatus(voucher).key === statusFilter.value)
  }

  return rows
})

const loadData = async () => {
  loading.value = true
  try {
    const response = await getAllVouchers()
    const payload = response?.data
    const rows = Array.isArray(payload) ? payload : (Array.isArray(payload?.content) ? payload.content : [])
    list.value = rows
  } catch (error) {
    console.error("Failed to load vouchers", error)
    list.value = []
    window?.toast?.error?.("Không thể tải danh sách phiếu giảm giá")
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="voucher-page">
    <div class="card table-section">
      <div class="table-header">
        <h3>Danh sách phiếu giảm giá</h3>
        <button class="btn-add" @click="router.push(`${basePath}/khuyen-mai/voucher/new`)">
          <i class="fa-solid fa-plus"></i>
          Thêm mới
        </button>
      </div>

      <div class="filter-row">
        <div class="input-wrapper search-col">
          <i class="fa-solid fa-magnifying-glass search-icon"></i>
          <input v-model="keyword" class="form-control" type="text" placeholder="Tìm theo tên hoặc mã phiếu...">
        </div>

        <select v-model="statusFilter" class="form-control status-select">
          <option value="">-- Tất cả trạng thái --</option>
          <option value="active">Hoạt động</option>
          <option value="upcoming">Đang xử lý</option>
          <option value="ended">Ngừng hoạt động</option>
        </select>
      </div>

      <div class="table-container">
        <table class="custom-table">
          <thead>
            <tr>
              <th class="text-center" width="60">STT</th>
              <th>Mã phiếu</th>
              <th>Tên phiếu</th>
              <th class="text-center">Giá trị</th>
              <th class="text-center">Đơn tối thiểu</th>
              <th class="text-center">Giảm tối đa</th>
              <th class="text-center">Số lượng</th>
              <th class="text-center">Trạng thái</th>
              <th class="text-center">Hành động</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="!loading && filtered.length === 0">
              <td colspan="9" class="empty-row">Không có phiếu giảm giá phù hợp</td>
            </tr>

            <tr v-for="(voucher, index) in filtered" :key="voucher.id || index">
              <td class="text-center">{{ index + 1 }}</td>
              <td><strong>{{ toPggCode(voucher, index) }}</strong></td>
              <td>{{ voucher.tenPhieuGiamGia || toPggCode(voucher, index) }}</td>
              <td class="text-center">
                {{ voucher.hinhThucGiam ? `${voucher.giaTriGiamGia}%` : formatCurrency(voucher.giaTriGiamGia) }}
              </td>
              <td class="text-center">{{ formatCurrency(voucher.hoaDonToiThieu || 0) }}</td>
              <td class="text-center">{{ Number(voucher.soTienGiamToiDa || 0) > 0 ? formatCurrency(voucher.soTienGiamToiDa) : "Không giới hạn" }}</td>
              <td class="text-center">{{ voucher.soLuongSuDung ?? 0 }}</td>
              <td class="text-center">
                <span class="status-badge" :class="`status-${getStatus(voucher).key}`">{{ getStatus(voucher).text }}</span>
              </td>
              <td class="text-center">
                <button class="btn-view" @click="router.push(`${basePath}/khuyen-mai/voucher/${voucher.id}`)">
                  <span class="material-icons-outlined">visibility</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.voucher-page {
  padding-bottom: 20px;
}

.card {
  background: #fff;
  border-radius: 18px;
  padding: 24px;
  border: 1px solid rgba(255, 77, 79, 0.18);
  box-shadow: 0 18px 50px rgba(17, 24, 39, 0.08);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.table-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: rgba(17, 24, 39, 0.9);
}

.btn-add {
  background: linear-gradient(90deg, #7c1021 0%, #a9162d 35%, #cf243a 68%, #e64555 100%);
  color: #fff;
  border: none;
  border-radius: 12px;
  height: 40px;
  padding: 0 16px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.filter-row {
  display: grid;
  grid-template-columns: 1fr 260px;
  gap: 12px;
  margin-bottom: 14px;
}

.input-wrapper {
  position: relative;
}

.search-icon {
  position: absolute;
  top: 50%;
  left: 12px;
  transform: translateY(-50%);
  color: rgba(17, 24, 39, 0.45);
}

.form-control {
  height: 42px;
  width: 100%;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 0 12px;
  font-size: 14px;
  color: #334155;
  outline: none;
}

.search-col .form-control {
  padding-left: 36px;
}

.table-container {
  overflow-x: auto;
  border: 1px solid rgba(255, 77, 79, 0.18);
  border-radius: 14px;
}

.custom-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 1100px;
}

.custom-table thead tr {
  background: linear-gradient(90deg, #7c1021 0%, #a9162d 35%, #cf243a 68%, #e64555 100%);
}

.custom-table th {
  background: transparent;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.3px;
  padding: 14px 16px;
  white-space: nowrap;
}
.custom-table thead th:first-child { border-top-left-radius: 10px; }
.custom-table thead th:last-child { border-top-right-radius: 10px; }

.custom-table td {
  padding: 13px 16px;
  border-bottom: 1px solid rgba(17, 24, 39, 0.06);
  color: rgba(17, 24, 39, 0.82);
}

.text-center {
  text-align: center;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 600;
}


.status-badge.status-active {
  background: rgba(34, 197, 94, 0.12);
  color: #166534;
}

.status-badge.status-upcoming {
  background: rgba(245, 158, 11, 0.14);
  color: #92400e;
}

.status-badge.status-ended {
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.btn-view {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  border: 1px solid rgba(17, 24, 39, 0.14);
  background: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.btn-view .material-icons-outlined {
  font-size: 18px;
}

.empty-row {
  text-align: center;
  color: rgba(17, 24, 39, 0.5);
  padding: 24px;
}

@media (max-width: 900px) {
  .filter-row {
    grid-template-columns: 1fr;
  }
}
</style>
