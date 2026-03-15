<script setup>
import { ref, onMounted, computed, watch } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  getAllKhuyenMai,
  deleteKhuyenMai,
  getAllVouchers
} from "../../../services/khuyenMaiService"
import { getAdminStatusTone, normalizeAdminStatusLabel } from "../../../utils/adminStatus"

const router = useRouter()
const route = useRoute()
const basePath = computed(() => (route.path.startsWith('/employee') ? '/employee' : '/admin'))
const list = ref([])
const voucherList = ref([])
const activeTab = ref('campaigns') // 'campaigns' or 'vouchers'
const searchKeyword = ref('')
const filterStatus = ref('')

async function loadData() {
  try {
    const [campaignsRes, vouchersRes] = await Promise.allSettled([
      getAllKhuyenMai(),
      getAllVouchers()
    ])

    if (campaignsRes.status === 'fulfilled') {
      const data = campaignsRes.value?.data
      list.value = Array.isArray(data)
        ? data
        : (Array.isArray(data?.content) ? data.content : [])
    } else {
      list.value = []
      console.error('Error loading campaigns:', campaignsRes.reason)
    }

    if (vouchersRes.status === 'fulfilled') {
      const data = vouchersRes.value?.data
      voucherList.value = Array.isArray(data)
        ? data
        : (Array.isArray(data?.content) ? data.content : [])
    } else {
      voucherList.value = []
      console.error('Error loading vouchers:', vouchersRes.reason)
    }

  } catch (error) {
    console.error('Error loading data:', error)
    list.value = []
    voucherList.value = []
  }
}

onMounted(() => {
  if (route.query.tab === 'vouchers') {
    activeTab.value = 'vouchers'
  }
  loadData()
})

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'vouchers' || tab === 'campaigns') {
      activeTab.value = tab
    }
  }
)

const goToEdit = (id) => {
  router.push(`${basePath.value}/khuyen-mai/form/${id}`)
}

const goToVoucherForm = (id = null) => {
  if (id) {
    router.push(`${basePath.value}/khuyen-mai/voucher/${id}`)
  } else {
    router.push(`${basePath.value}/khuyen-mai/voucher/new`)
  }
}

const remove = async (id) => {
  const confirmed = await window.confirmDialog('Bạn có chắc muốn xóa khuyến mãi này?')
  if (!confirmed) return
  
  try {
    await deleteKhuyenMai(id)
    window.toast.success('Xóa khuyến mãi thành công')
    await loadData()
  } catch (error) {
    console.error('Error deleting:', error)
    window.toast.error('Không thể xóa khuyến mãi này')
  }
}

const toDate = (value) => {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

const getCampaignStatus = (campaign) => {
  const normalized = normalizeAdminStatusLabel(campaign?.trangThai)
  const now = new Date()
  const start = toDate(campaign?.ngayBatDau)
  const end = toDate(campaign?.ngayKetThuc)

  if (normalized === 'Ngừng hoạt động' || (end && now > end)) {
    return { text: 'Ngừng hoạt động', tone: 'danger' }
  }

  if (normalized === 'Đang xử lý' || (start && now < start)) {
    return { text: 'Đang xử lý', tone: 'warning' }
  }

  return {
    text: normalized === 'Không xác định' ? 'Hoạt động' : normalized,
    tone: getAdminStatusTone(normalized)
  }
}

const getVoucherStatus = (voucher) => {
  const normalized = normalizeAdminStatusLabel(voucher?.trangThai)
  const now = new Date()
  const start = toDate(voucher?.ngayBatDau)
  const end = toDate(voucher?.ngayKetThuc)

  if (normalized === 'Ngừng hoạt động' || (end && now > end)) {
    return { text: 'Ngừng hoạt động', tone: 'danger' }
  }

  if (normalized === 'Đang xử lý' || (start && now < start)) {
    return { text: 'Đang xử lý', tone: 'warning' }
  }

  return { text: 'Hoạt động', tone: 'success' }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('vi-VN')
}

const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(value || 0)
}

const formatCampaignValue = (campaign) => {
  const unit = String(campaign?.donViGiam || 'PERCENT').toUpperCase()
  const value = Number(campaign?.giaTri || 0)
  return unit === 'VND' ? formatCurrency(value) : `${value}%`
}

const filteredVouchers = computed(() => {
  let filtered = voucherList.value
  
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(v => 
      v.tenPhieuGiamGia?.toLowerCase().includes(keyword) ||
      v.maPhieuGiamGia?.toLowerCase().includes(keyword)
    )
  }
  
  if (filterStatus.value) {
    filtered = filtered.filter(v => {
      const status = getVoucherStatus(v)
      return status.tone === filterStatus.value
    })
  }
  
  return filtered
})
</script>

<template>
  <div class="khuyen-mai-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Quản lý giảm giá/ Khuyến mãi</h1>
        <p class="page-subtitle">Mã giảm giá, giảm theo %, theo tiền...</p>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tabs-container">
      <div class="tabs">
        <button 
          class="tab" 
          :class="{ active: activeTab === 'campaigns' }"
          @click="router.push(`${basePath}/khuyen-mai/list?tab=campaigns`)"
        >
          <span class="material-icons-outlined tab-icon">campaign</span>
          Đợt khuyến mãi
        </button>
        <button 
          class="tab" 
          :class="{ active: activeTab === 'vouchers' }"
          @click="router.push(`${basePath}/khuyen-mai/list?tab=vouchers`)"
        >
          <span class="material-icons-outlined tab-icon">confirmation_number</span>
          Phiếu giảm giá
        </button>
      </div>
    </div>

    <!-- Campaigns Tab -->
    <div v-if="activeTab === 'campaigns'" class="content-card">
      <div class="card-header">
        <h3>Danh sách đợt khuyến mãi</h3>
        <button class="btn-primary" @click="router.push(`${basePath}/khuyen-mai/form`)">
          <span class="material-icons-outlined">add</span>
          Tạo khuyến mãi
        </button>
      </div>

      <div class="table-container">
        <table class="modern-table">
          <thead>
            <tr>
              <th style="width:50px">STT</th>
              <th style="width:120px">Mã</th>
              <th>Tên chương trình</th>
              <th style="width:120px">Giá trị</th>
              <th style="width:200px">Thời gian</th>
              <th style="width:140px">Trạng thái</th>
              <th style="width:120px" class="text-center">Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="list.length === 0">
              <td colspan="7" class="empty-state">
                <div class="empty-content">
                  <span class="material-icons-outlined">inbox</span>
                  <p>Chưa có đợt khuyến mãi nào</p>
                </div>
              </td>
            </tr>
            
            <tr v-for="(km, idx) in list" :key="km.id">
              <td class="text-center">{{ idx + 1 }}</td>
              <td><strong>{{ km.maKhuyenMai }}</strong></td>
              <td>{{ km.tenKhuyenMai }}</td>
              <td>
                <span class="value-badge">
                  {{ formatCampaignValue(km) }}
                </span>
              </td>
              <td class="text-muted">
                {{ km.ngayBatDau?.substring(0,10) }} → {{ km.ngayKetThuc?.substring(0,10) }}
              </td>
              <td>
                <span class="status-badge" :class="`status-${getCampaignStatus(km).tone}`">
                  {{ getCampaignStatus(km).text }}
                </span>
              </td>
              <td class="text-center">
                <div class="action-buttons">
                  <button class="btn-icon" @click="goToEdit(km.id)" title="Chỉnh sửa">
                    <span class="material-icons-outlined">edit</span>
                  </button>
                  <button class="btn-icon danger" @click="remove(km.id)" title="Xóa">
                    <span class="material-icons-outlined">delete</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Vouchers Tab -->
    <div v-if="activeTab === 'vouchers'" class="content-card">
      <div class="card-header">
        <h3>Danh sách phiếu giảm giá</h3>
        <button class="btn-primary" @click="goToVoucherForm()">
          <span class="material-icons-outlined">add</span>
          Tạo phiếu giảm giá
        </button>
      </div>

      <!-- Filters -->
      <div class="filters">
        <div class="filter-group">
          <div class="search-wrap">
            <span class="material-icons-outlined">search</span>
            <input 
              v-model="searchKeyword" 
              type="text" 
              class="search-input" 
              placeholder="Tìm theo tên hoặc mã phiếu..."
            >
          </div>
          <select v-model="filterStatus" class="filter-select">
            <option value="">Tất cả trạng thái</option>
            <option value="success">Hoạt động</option>
            <option value="warning">Đang xử lý</option>
            <option value="danger">Ngừng hoạt động</option>
          </select>
        </div>
      </div>

      <div class="table-container">
        <table class="modern-table">
          <thead>
            <tr>
              <th style="width:50px">STT</th>
              <th style="width:120px">Mã phiếu</th>
              <th>Tên phiếu</th>
              <th style="width:100px">Loại</th>
              <th style="width:120px">Giá trị</th>
              <th style="width:120px">Đơn tối thiểu</th>
              <th style="width:100px">Số lượng</th>
              <th style="width:200px">Thời gian</th>
              <th style="width:130px">Trạng thái</th>
              <th style="width:120px" class="text-center">Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="filteredVouchers.length === 0">
              <td colspan="10" class="empty-state">
                <div class="empty-content">
                  <span class="material-icons-outlined">receipt_long</span>
                  <p>Không tìm thấy phiếu giảm giá</p>
                </div>
              </td>
            </tr>
            
            <tr v-for="(voucher, idx) in filteredVouchers" :key="voucher.id">
              <td class="text-center">{{ idx + 1 }}</td>
              <td><strong>{{ voucher.maPhieuGiamGia || '-' }}</strong></td>
              <td>{{ voucher.tenPhieuGiamGia || '-' }}</td>
              <td>
                <span class="type-badge" :class="voucher.loaiPhieuGiamGia ? 'personal' : 'public'">
                  {{ voucher.loaiPhieuGiamGia ? 'Cá nhân' : 'Công khai' }}
                </span>
              </td>
              <td>
                <span class="value-badge" v-if="voucher.giaTriGiamGia">
                  {{ voucher.hinhThucGiam ? `${voucher.giaTriGiamGia}%` : formatCurrency(voucher.giaTriGiamGia) }}
                </span>
                <span v-else class="text-muted">-</span>
              </td>
              <td class="text-muted">{{ voucher.hoaDonToiThieu ? formatCurrency(voucher.hoaDonToiThieu) : '-' }}</td>
              <td class="text-center">{{ voucher.soLuongSuDung ?? '-' }}</td>
              <td class="text-muted">
                {{ formatDate(voucher.ngayBatDau) }} → {{ formatDate(voucher.ngayKetThuc) }}
              </td>
              <td>
                <span class="status-badge" :class="`status-${getVoucherStatus(voucher).tone}`">
                  {{ getVoucherStatus(voucher).text }}
                </span>
              </td>
              <td class="text-center">
                <div class="action-buttons">
                  <button class="btn-icon" @click="goToVoucherForm(voucher.id)" title="Xem chi tiết">
                    <span class="material-icons-outlined">visibility</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap');
@import url('https://fonts.googleapis.com/icon?family=Material+Icons+Outlined');

.khuyen-mai-page {
  font-family: 'Be Vietnam Pro', 'Segoe UI', Tahoma, sans-serif;
  padding: 28px 30px;
  background: #f6f8fb;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 18px;
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
  margin: 8px 0 0;
  color: #64748b;
  font-size: 15px;
  font-weight: 500;
}

.tabs-container {
  margin-bottom: 18px;
}

.tabs {
  display: flex;
  gap: 8px;
  background: #fff;
  padding: 8px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  width: fit-content;
}

.tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
}

.tab:hover {
  background: #f3f4f6;
  color: #111827;
}

.tab.active {
  background: #ef2d2d;
  color: #fff;
}

.tab-icon {
  font-size: 19px;
  color: inherit;
}

.content-card {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.card-header h3 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 16px;
  background: #ef2d2d;
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.btn-primary:hover {
  background: #dc2626;
}

.btn-primary .material-icons-outlined {
  font-size: 19px;
}

.filters {
  padding: 16px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.filter-group {
  display: flex;
  gap: 12px;
  width: 100%;
}

.search-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 280px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #fff;
}

.search-wrap .material-icons-outlined {
  font-size: 18px;
  color: #6b7280;
}

.search-input {
  flex: 1;
  width: auto;
  min-width: 120px;
  height: 40px;
  padding: 0;
  border: none;
  font-size: 14px;
  outline: none;
  background: transparent;
}

.search-wrap:focus-within {
  border-color: #93c5fd;
  box-shadow: 0 0 0 3px rgba(147, 197, 253, 0.26);
}

.filter-select {
  height: 40px;
  padding: 0 16px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  cursor: pointer;
  background: #fff;
  width: 220px;
  min-width: 220px;
  flex: 0 0 220px;
}

.filter-select:focus {
  border-color: #93c5fd;
  box-shadow: 0 0 0 3px rgba(147, 197, 253, 0.26);
}

.table-container {
  overflow-x: auto;
}

.modern-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.modern-table thead {
  background: #f8fafc;
}

.modern-table th {
  padding: 12px 16px;
  text-align: left;
  color: #94a3b8;
  border-bottom: 1px solid #e5e7eb;
  white-space: nowrap;
  text-transform: uppercase;
  font-size: 13px;
  font-weight: 700;
}

.modern-table td {
  padding: 16px;
  border-bottom: 1px solid #f3f4f6;
  color: #334155;
}

.modern-table tbody tr:hover {
  background: #f9fafb;
}

.text-center {
  text-align: center;
}

.text-muted {
  color: #64748b;
  font-size: 13px;
}

.value-badge {
  display: inline-block;
  padding: 4px 12px;
  background: #f3f4f6;
  color: #374151;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
}

.type-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  background: #f3f4f6;
  color: #475569;
}

.status-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-badge.active {
  background: #dcfce7;
  color: #166534;
}

.status-badge.inactive,
.status-badge.upcoming,
.status-badge.ended {
  background: #fee2e2;
  color: #991b1b;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid #d1d5db;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  color: #64748b;
}

.btn-icon:hover {
  background: #f1f5f9;
}

.btn-icon.danger:hover {
  background: #fee2e2;
  border-color: #fca5a5;
  color: #dc2626;
}

.btn-icon .material-icons-outlined {
  font-size: 18px;
}

.empty-state {
  padding: 60px 20px !important;
  text-align: center;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #9ca3af;
}

.empty-content .material-icons-outlined {
  font-size: 48px;
  opacity: 0.5;
}

.empty-content p {
  margin: 0;
  font-size: 14px;
}

@media (max-width: 1024px) {
  .page-title {
    font-size: 20px;
  }

  .card-header h3 {
    font-size: 20px;
  }
}

@media (max-width: 900px) {
  .filter-group {
    flex-direction: column;
  }

  .search-wrap,
  .filter-select {
    width: 100%;
    min-width: 0;
    flex: 1;
  }
}

@media (max-width: 768px) {
  .khuyen-mai-page {
    padding: 18px 14px;
  }

  .page-title {
    font-size: 18px;
  }

  .card-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .btn-primary {
    justify-content: center;
  }
}
</style>