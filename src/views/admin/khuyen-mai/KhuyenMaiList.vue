<script setup>
import { ref, onMounted, computed } from "vue"
import { useRouter } from "vue-router"
import {
  getAllKhuyenMai,
  deleteKhuyenMai,
  getAllVouchers
} from "../../../services/khuyenMaiService"

const router = useRouter()
const list = ref([])
const voucherList = ref([])
const activeTab = ref('campaigns') // 'campaigns' or 'vouchers'
const searchKeyword = ref('')
const filterStatus = ref('')

async function loadData() {
  try {
    const [campaignsRes, vouchersRes] = await Promise.all([
      getAllKhuyenMai(),
      getAllVouchers()
    ])
    list.value = campaignsRes.data || []
    voucherList.value = vouchersRes.data || []
  } catch (error) {
    console.error('Error loading data:', error)
  }
}

onMounted(loadData)

const goToEdit = (id) => {
  router.push(`/admin/khuyen-mai/form/${id}`)
}

const goToVoucherForm = (id = null) => {
  if (id) {
    router.push(`/admin/khuyen-mai/voucher/${id}`)
  } else {
    router.push('/admin/khuyen-mai/voucher/new')
  }
}

const remove = async (id) => {
  if (!confirm('Bạn có chắc muốn xóa khuyến mãi này?')) return
  
  try {
    await deleteKhuyenMai(id)
    await loadData()
  } catch (error) {
    console.error('Error deleting:', error)
    alert('Không thể xóa khuyến mãi này')
  }
}

const isActive = (status) => {
  return status === "Đang chạy" || status === "Hoạt động"
}

const getVoucherStatus = (voucher) => {
  if (!voucher.trangThai) return { text: 'Đã kết thúc', class: 'ended' }
  
  const now = new Date()
  const start = new Date(voucher.ngayBatDau)
  const end = new Date(voucher.ngayKetThuc)
  
  if (now < start) return { text: 'Chưa bắt đầu', class: 'upcoming' }
  if (now > end) return { text: 'Đã kết thúc', class: 'ended' }
  return { text: 'Đang hoạt động', class: 'active' }
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
      return status.class === filterStatus.value
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
          @click="activeTab = 'campaigns'"
        >
          <span class="tab-icon">📢</span>
          Đợt khuyến mãi
        </button>
        <button 
          class="tab" 
          :class="{ active: activeTab === 'vouchers' }"
          @click="activeTab = 'vouchers'"
        >
          <span class="tab-icon">🎫</span>
          Phiếu giảm giá
        </button>
      </div>
    </div>

    <!-- Campaigns Tab -->
    <div v-if="activeTab === 'campaigns'" class="content-card">
      <div class="card-header">
        <h3>Danh sách đợt khuyến mãi</h3>
        <button class="btn-primary" @click="router.push('/admin/khuyen-mai/form')">
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
                  {{ km.giaTri }}
                </span>
              </td>
              <td class="text-muted">
                {{ km.ngayBatDau?.substring(0,10) }} → {{ km.ngayKetThuc?.substring(0,10) }}
              </td>
              <td>
                <span class="status-badge" :class="isActive(km.trangThai) ? 'active' : 'inactive'">
                  {{ km.trangThai }}
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
          <input 
            v-model="searchKeyword" 
            type="text" 
            class="search-input" 
            placeholder="Tìm theo tên hoặc mã phiếu..."
          >
          <select v-model="filterStatus" class="filter-select">
            <option value="">Tất cả trạng thái</option>
            <option value="active">Đang hoạt động</option>
            <option value="upcoming">Chưa bắt đầu</option>
            <option value="ended">Đã kết thúc</option>
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
              <td><strong>{{ voucher.maPhieuGiamGia }}</strong></td>
              <td>{{ voucher.tenPhieuGiamGia }}</td>
              <td>
                <span class="type-badge" :class="voucher.loaiPhieuGiamGia ? 'personal' : 'public'">
                  {{ voucher.loaiPhieuGiamGia ? 'Cá nhân' : 'Công khai' }}
                </span>
              </td>
              <td>
                <span class="value-badge">
                  {{ voucher.hinhThucGiam ? `${voucher.giaTriGiamGia}%` : formatCurrency(voucher.giaTriGiamGia) }}
                </span>
              </td>
              <td class="text-muted">{{ formatCurrency(voucher.hoaDonToiThieu) }}</td>
              <td class="text-center">{{ voucher.soLuongSuDung }}</td>
              <td class="text-muted">
                {{ formatDate(voucher.ngayBatDau) }} → {{ formatDate(voucher.ngayKetThuc) }}
              </td>
              <td>
                <span class="status-badge" :class="getVoucherStatus(voucher).class">
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
@import url('https://fonts.googleapis.com/icon?family=Material+Icons+Outlined');

.khuyen-mai-page {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 4px;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

/* Tabs */
.tabs-container {
  margin-bottom: 24px;
}

.tabs {
  display: flex;
  gap: 8px;
  background: white;
  padding: 8px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
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
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.tab:hover {
  background: #f3f4f6;
  color: #111827;
}

.tab.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.tab-icon {
  font-size: 20px;
}

/* Content Card */
.content-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
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
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.btn-primary .material-icons-outlined {
  font-size: 20px;
}

/* Filters */
.filters {
  padding: 16px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.filter-group {
  display: flex;
  gap: 12px;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
}

.search-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.filter-select {
  padding: 10px 16px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  cursor: pointer;
  background: white;
  min-width: 180px;
}

.filter-select:focus {
  border-color: #667eea;
}

/* Table */
.table-container {
  overflow-x: auto;
}

.modern-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.modern-table thead {
  background: #f9fafb;
}

.modern-table th {
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  color: #374151;
  border-bottom: 1px solid #e5e7eb;
  white-space: nowrap;
}

.modern-table td {
  padding: 16px;
  border-bottom: 1px solid #f3f4f6;
  color: #111827;
}

.modern-table tbody tr {
  transition: background 0.2s;
}

.modern-table tbody tr:hover {
  background: #f9fafb;
}

.text-center {
  text-align: center;
}

.text-muted {
  color: #6b7280;
  font-size: 13px;
}

/* Badges */
.value-badge {
  display: inline-block;
  padding: 4px 12px;
  background: #dbeafe;
  color: #1e40af;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
}

.type-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.type-badge.personal {
  background: #fef3c7;
  color: #92400e;
}

.type-badge.public {
  background: #dbeafe;
  color: #1e40af;
}

.status-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.active {
  background: #dcfce7;
  color: #166534;
}

.status-badge.inactive {
  background: #fee2e2;
  color: #991b1b;
}

.status-badge.upcoming {
  background: #e0e7ff;
  color: #3730a3;
}

.status-badge.ended {
  background: #f3f4f6;
  color: #6b7280;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.btn-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  color: #6b7280;
}

.btn-icon:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
  color: #111827;
}

.btn-icon.danger:hover {
  background: #fee2e2;
  border-color: #fca5a5;
  color: #dc2626;
}

.btn-icon .material-icons-outlined {
  font-size: 20px;
}

/* Empty State */
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
</style>