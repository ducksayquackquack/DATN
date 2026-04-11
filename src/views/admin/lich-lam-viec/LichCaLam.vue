<template>
  <div class="ca-lam-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Danh sách ca làm việc</h1>
        <p class="page-subtitle">Bộ lọc tìm kiếm chung</p>
      </div>
    </div>

    <div class="filter-section">
      <div class="filter-group">
        <div class="filter-item">
          <label>Tìm kiếm chung</label>
          <input
            v-model="filters.search"
            type="text"
            class="form-input"
            placeholder="Nhập tên ca, mã ca..."
          />
        </div>
        <div class="filter-item">
          <label>Thời gian bắt đầu</label>
          <select v-model="filters.startTime" class="form-select">
            <option value="">--- --</option>
            <option value="morning">Sáng (7h-12h)</option>
            <option value="afternoon">Chiều (12h-17h)</option>
            <option value="evening">Tối (17h-22h)</option>
          </select>
        </div>
        <div class="filter-item">
          <label>Thời gian kết thúc</label>
          <select v-model="filters.endTime" class="form-select">
            <option value="">--- --</option>
            <option value="morning">12h</option>
            <option value="afternoon">17h</option>
            <option value="evening">22h</option>
          </select>
        </div>
        <div class="filter-item">
          <label>Trạng thái</label>
          <select v-model="filters.status" class="form-select">
            <option value="">Tất cả</option>
            <option value="active">Hoạt động</option>
            <option value="inactive">Ngưng</option>
          </select>
        </div>
      </div>
      <button class="btn-add" @click="addShift">
        <span class="material-icons-outlined">add</span>
      </button>
      <button class="btn-refresh" @click="loadShifts">
        <span class="material-icons-outlined">refresh</span>
      </button>
    </div>

    <div class="table-section">
      <div class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>STT</th>
              <th>Thông tin ca</th>
              <th>Giờ bắt đầu</th>
              <th>Giờ kết thúc</th>
              <th>Trạng thái</th>
              <th class="text-center">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredShifts.length === 0">
              <td colspan="6" class="empty-state">
                <span class="material-icons-outlined">event_busy</span>
                <p>Không có dữ liệu</p>
              </td>
            </tr>
            <tr v-for="(shift, index) in filteredShifts" :key="shift.id">
              <td>{{ index + 1 }}</td>
              <td>
                <div class="shift-info">
                  <div class="shift-icon" :class="shift.type">
                    <span class="material-icons-outlined">{{ shift.icon }}</span>
                  </div>
                  <div>
                    <div class="shift-name">{{ shift.name }}</div>
                    <div class="shift-desc">{{ shift.description }}</div>
                  </div>
                </div>
              </td>
              <td>{{ shift.startTime }}</td>
              <td>{{ shift.endTime }}</td>
              <td>
                <span class="status-badge" :class="shift.status">
                  {{ getStatusText(shift.status) }}
                </span>
              </td>
              <td class="text-center">
                <div class="action-buttons">
                  <button class="btn-icon" @click="editShift(shift)" title="Chỉnh sửa">
                    <span class="material-icons-outlined">visibility</span>
                  </button>
                  <button class="btn-icon danger" @click="deleteShift(shift)" title="Xóa">
                    <span class="material-icons-outlined">delete</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="formModal.open" class="modal-overlay" @click.self="closeFormModal">
      <div class="modal-card">
        <h3 class="modal-title">{{ formModal.mode === 'create' ? 'Thêm ca làm' : 'Cập nhật ca làm' }}</h3>

        <div class="modal-grid">
          <div class="modal-field">
            <label>Tên ca làm</label>
            <input v-model="formModel.tenCa" class="form-input" type="text" placeholder="Ví dụ: Ca sáng" />
          </div>

          <div class="modal-field">
            <label>Mô tả</label>
            <input v-model="formModel.moTa" class="form-input" type="text" placeholder="Ví dụ: Ca làm buổi sáng" />
          </div>

          <div class="modal-field">
            <label>Giờ bắt đầu</label>
            <input v-model="formModel.gioBatDau" class="form-input" type="time" />
          </div>

          <div class="modal-field">
            <label>Giờ kết thúc</label>
            <input v-model="formModel.gioKetThuc" class="form-input" type="time" />
          </div>

          <div class="modal-field modal-field-full">
            <label>Trạng thái</label>
            <select v-model="formModel.trangThai" class="form-select">
              <option value="Hoạt động">Hoạt động</option>
              <option value="Ngưng">Ngưng</option>
            </select>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn-modal" @click="closeFormModal">Hủy</button>
          <button class="btn-modal btn-modal-primary" @click="submitShiftForm">{{ formModal.mode === 'create' ? 'Thêm' : 'Lưu' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useToast } from '../../../composables/useToast'
import { createCaLam, getAllCaLam, deleteCaLam, updateCaLam } from '../../../services/caLamService'

const toast = useToast()

const filters = reactive({
  search: '',
  startTime: '',
  endTime: '',
  status: ''
})

const shifts = ref([])
const formModal = reactive({
  open: false,
  mode: 'create',
  editingId: null
})

const formModel = reactive({
  tenCa: '',
  moTa: '',
  gioBatDau: '07:00',
  gioKetThuc: '12:00',
  trangThai: 'Hoạt động'
})

const normalizeTime = (timeValue) => {
  if (!timeValue) return ''
  const raw = String(timeValue).trim()
  if (/^\d{2}:\d{2}$/.test(raw)) return raw
  if (/^\d{2}:\d{2}:\d{2}$/.test(raw)) return raw.slice(0, 5)
  return raw
}

const mapShiftType = (startTime) => {
  const hour = Number(String(startTime || '').split(':')[0])
  if (Number.isNaN(hour)) return { type: 'morning', icon: 'schedule' }
  if (hour < 12) return { type: 'morning', icon: 'wb_sunny' }
  if (hour < 17) return { type: 'afternoon', icon: 'wb_twilight' }
  return { type: 'evening', icon: 'nights_stay' }
}

const mapApiShift = (item) => {
  const startTime = normalizeTime(item?.gioBatDau)
  const endTime = normalizeTime(item?.gioKetThuc)
  const statusText = String(item?.trangThai || '').toLowerCase()
  const status = statusText.includes('hoạt động') || statusText.includes('hoat dong') ? 'active' : 'inactive'
  const visual = mapShiftType(startTime)

  return {
    id: item?.id,
    name: item?.tenCa || 'Chưa đặt tên',
    description: item?.moTa || '',
    startTime,
    endTime,
    status,
    type: visual.type,
    icon: visual.icon
  }
}

const filteredShifts = computed(() => {
  return shifts.value.filter((shift) => {
    const keyword = filters.search.trim().toLowerCase()
    const matchKeyword = !keyword
      || shift.name.toLowerCase().includes(keyword)
      || shift.description.toLowerCase().includes(keyword)
      || String(shift.id || '').includes(keyword)

    const mapStart = {
      morning: shift.startTime >= '07:00' && shift.startTime < '12:00',
      afternoon: shift.startTime >= '12:00' && shift.startTime < '17:00',
      evening: shift.startTime >= '17:00'
    }

    const mapEnd = {
      morning: shift.endTime === '12:00',
      afternoon: shift.endTime === '17:00',
      evening: shift.endTime === '22:00'
    }

    const matchStart = !filters.startTime || !!mapStart[filters.startTime]
    const matchEnd = !filters.endTime || !!mapEnd[filters.endTime]
    const matchStatus = !filters.status || shift.status === filters.status

    return matchKeyword && matchStart && matchEnd && matchStatus
  })
})

const getStatusText = (status) => {
  return status === 'active' ? 'Hoạt động' : 'Ngưng'
}

const loadShifts = async () => {
  try {
    const res = await getAllCaLam()
    const data = Array.isArray(res?.data)
      ? res.data
      : (Array.isArray(res?.data?.content) ? res.data.content : [])
    shifts.value = data.map(mapApiShift)
  } catch (err) {
    console.error('Load shifts failed:', err)
    shifts.value = []
    toast.error('Không tải được ca làm')
  }
}

const validateTimeValue = (timeValue) => /^\d{2}:\d{2}$/.test(timeValue)

const resetFormModel = () => {
  formModel.tenCa = ''
  formModel.moTa = ''
  formModel.gioBatDau = '07:00'
  formModel.gioKetThuc = '12:00'
  formModel.trangThai = 'Hoạt động'
}

const openCreateModal = () => {
  formModal.open = true
  formModal.mode = 'create'
  formModal.editingId = null
  resetFormModel()
}

const openEditModal = (shift) => {
  if (!shift?.id) return
  formModal.open = true
  formModal.mode = 'edit'
  formModal.editingId = shift.id
  formModel.tenCa = shift.name || ''
  formModel.moTa = shift.description || ''
  formModel.gioBatDau = shift.startTime || '07:00'
  formModel.gioKetThuc = shift.endTime || '12:00'
  formModel.trangThai = shift.status === 'inactive' ? 'Ngưng' : 'Hoạt động'
}

const closeFormModal = () => {
  formModal.open = false
  formModal.editingId = null
}

const buildShiftPayload = () => {
  if (!String(formModel.tenCa).trim()) {
    toast.error('Tên ca không được để trống')
    return null
  }

  if (!validateTimeValue(String(formModel.gioBatDau).trim())) {
    toast.error('Giờ bắt đầu không hợp lệ, dùng định dạng HH:mm')
    return null
  }

  if (!validateTimeValue(String(formModel.gioKetThuc).trim())) {
    toast.error('Giờ kết thúc không hợp lệ, dùng định dạng HH:mm')
    return null
  }

  if (String(formModel.gioBatDau).trim() >= String(formModel.gioKetThuc).trim()) {
    toast.error('Giờ kết thúc phải lớn hơn giờ bắt đầu')
    return null
  }

  return {
    tenCa: String(formModel.tenCa).trim(),
    moTa: String(formModel.moTa || '').trim(),
    gioBatDau: String(formModel.gioBatDau).trim(),
    gioKetThuc: String(formModel.gioKetThuc).trim(),
    trangThai: String(formModel.trangThai).trim() || 'Hoạt động'
  }
}

const addShift = async () => {
  openCreateModal()
}

const editShift = async (shift) => {
  openEditModal(shift)
}

const submitShiftForm = async () => {
  const payload = buildShiftPayload()
  if (!payload) return

  try {
    if (formModal.mode === 'create') {
      await createCaLam(payload)
      toast.success('Thêm ca làm thành công')
    } else {
      await updateCaLam(formModal.editingId, payload)
      toast.success('Cập nhật ca làm thành công')
    }

    closeFormModal()
    loadShifts()
  } catch (error) {
    console.error('Submit shift form failed:', error)
    toast.error(formModal.mode === 'create' ? 'Thêm ca làm thất bại' : 'Cập nhật ca làm thất bại')
  }
}

const deleteShift = async (shift) => {
  if (!shift?.id) return
  const confirmed = await window.confirmDialog(`Bạn có chắc muốn xóa ca "${shift.name}"?`)
  if (!confirmed) return

  deleteCaLam(shift.id)
    .then(() => {
      toast.success('Xóa thành công!')
      loadShifts()
    })
    .catch((err) => {
      console.error('Delete shift failed:', err)
      toast.error('Xóa ca thất bại')
    })
  }

onMounted(loadShifts)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap');
@import url('https://fonts.googleapis.com/icon?family=Material+Icons+Outlined');

.ca-lam-page {
  font-family: 'Be Vietnam Pro', 'Segoe UI', Tahoma, sans-serif;
  padding: 28px 30px;
  background: #f6f8fb;
  min-height: calc(100vh - 76px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

.filter-section {
  margin-bottom: 18px;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 16px;
}

.filter-group {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(4, minmax(130px, 1fr));
  gap: 12px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-item label {
  color: #374151;
  font-size: 13px;
  font-weight: 600;
}

.form-input,
.form-select {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background-color: #fff;
  font-size: 14px;
  color: #111827;
}
.form-select {
  padding-right: 34px;
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: #f87171;
  box-shadow: 0 0 0 3px rgba(248, 113, 113, 0.2);
}

.btn-add,
.btn-refresh {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.btn-add {
  background: linear-gradient(120deg, #ef4444, #0f172a);
  border-color: transparent;
  color: #fff;
}

.btn-add:hover {
  background: linear-gradient(120deg, #dc2626, #0f172a);
}

.btn-refresh {
  background: #f8fafc;
  color: #475569;
}

.btn-refresh:hover,
.btn-icon:hover {
  background: #f1f5f9;
}

.table-section {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 16px;
}

.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 13px 10px;
  border-bottom: 1px solid #eef2f7;
  text-align: left;
  white-space: nowrap;
}

.data-table thead th {
  color: #94a3b8;
  background: #f8fafc;
  font-size: 13px;
  font-weight: 700;
}

.text-center {
  text-align: center !important;
}

.shift-info {
  display: flex;
  gap: 10px;
  align-items: center;
}

.shift-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.shift-icon .material-icons-outlined {
  font-size: 17px;
}

.shift-icon.morning {
  background: linear-gradient(135deg, #fb923c, #ef4444);
  color: #fff;
}

.shift-icon.afternoon {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
}

.shift-icon.evening {
  background: linear-gradient(135deg, #f43f5e, #b91c1c);
  color: #fff;
}

.shift-name {
  color: #111827;
  font-weight: 700;
}

.shift-desc {
  color: #94a3b8;
  font-size: 12px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-badge.active {
  background: #dcfce7;
  color: #15803d;
}

.status-badge.inactive {
  background: #fee2e2;
  color: #b91c1c;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.btn-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-icon .material-icons-outlined {
  font-size: 17px;
}

.btn-icon.danger {
  border-color: #fca5a5;
  color: #ef4444;
}

.empty-state {
  padding: 52px 12px;
  text-align: center;
  color: #64748b;
}

.empty-state .material-icons-outlined {
  display: block;
  font-size: 44px;
  margin-bottom: 8px;
  color: #9ca3af;
}

.empty-state p {
  margin: 0;
  color: #334155;
  font-size: 24px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.modal-card {
  width: min(560px, 100%);
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  padding: 16px;
}

.modal-title {
  margin: 0 0 12px;
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
}

.modal-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.modal-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modal-field label {
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.modal-field-full {
  grid-column: 1 / -1;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
}

.btn-modal {
  border: 1px solid #d1d5db;
  background: #f8fafc;
  color: #334155;
  border-radius: 10px;
  padding: 8px 14px;
  cursor: pointer;
}

.btn-modal-primary {
  border-color: transparent;
  background: linear-gradient(120deg, #ef4444, #0f172a);
  color: #fff;
}

@media (max-width: 1024px) {
  .page-title {
    font-size: 20px;
  }

  .filter-group {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }
}

@media (max-width: 768px) {
  .ca-lam-page {
    padding: 18px 14px;
  }

  .page-title {
    font-size: 18px;
  }

  .filter-section {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-group {
    grid-template-columns: 1fr;
  }

  .btn-add,
  .btn-refresh {
    width: 100%;
  }

  .modal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
