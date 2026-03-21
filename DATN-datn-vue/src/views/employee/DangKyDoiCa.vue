<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useToast } from '../../composables/useToast'
import { getAllNhanVien } from '../../services/nhanVienService'
import { getAllCaLam } from '../../services/caLamService'
import { getAllLichLamViecFull } from '../../services/lichLamViecService'

const toast = useToast()

const loading = ref(true)
const currentEmployeeId = ref(null)
const currentEmployeeName = ref('Nhan vien')
const employees = ref([])
const shifts = ref([])
const schedules = ref([])
const requests = ref([])

const STORAGE_KEY = 'doi-ca-requests'

const form = reactive({
  scheduleId: '',
  targetEmployeeId: '',
  reason: ''
})

const todayKey = () => {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const extractList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const normalizeDate = (value) => {
  if (!value) return ''
  const raw = String(value).trim()
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw
  if (raw.includes('T')) return raw.split('T')[0]
  return ''
}

const normalizeTime = (value) => {
  if (!value) return '00:00'
  const raw = String(value).trim()
  if (/^\d{2}:\d{2}$/.test(raw)) return raw
  if (/^\d{2}:\d{2}:\d{2}$/.test(raw)) return raw.slice(0, 5)
  return '00:00'
}

const loadLocalRequests = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    requests.value = Array.isArray(parsed)
      ? parsed.filter((item) => Number(item?.requesterId) === Number(currentEmployeeId.value))
      : []
  } catch {
    requests.value = []
  }
}

const persistRequest = (requestItem) => {
  let all = []
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    all = Array.isArray(parsed) ? parsed : []
  } catch {
    all = []
  }

  all.unshift(requestItem)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(all))
  loadLocalRequests()
}

const resolveCurrentEmployee = async () => {
  const userDataRaw = localStorage.getItem('user') || sessionStorage.getItem('user')
  if (userDataRaw) {
    try {
      const userData = JSON.parse(userDataRaw)
      if (userData?.idNhanVien) {
        currentEmployeeId.value = Number(userData.idNhanVien)
        currentEmployeeName.value = userData?.tenNhanVien || `NV #${userData.idNhanVien}`
        return
      }
      if (userData?.id && userData?.tenNhanVien) {
        currentEmployeeId.value = Number(userData.id)
        currentEmployeeName.value = userData.tenNhanVien
        return
      }
    } catch {
      // Continue fallback.
    }
  }

  const userId = Number(localStorage.getItem('userId') || 0)
  const list = employees.value
  const found = list.find((item) => Number(item?.idTaiKhoan || item?.taiKhoan?.id) === userId) || list[0]
  if (found?.id) {
    currentEmployeeId.value = Number(found.id)
    currentEmployeeName.value = found.tenNhanVien || `NV #${found.id}`
  }
}

const employeeNameById = computed(() => {
  return new Map(employees.value.map((item) => [Number(item.id), item.tenNhanVien || `NV #${item.id}`]))
})

const shiftById = computed(() => {
  return new Map(shifts.value.map((item) => [Number(item.id), item]))
})

const ownFutureSchedules = computed(() => {
  return schedules.value
    .filter((item) => Number(item?.idNhanVien) === Number(currentEmployeeId.value))
    .filter((item) => normalizeDate(item?.ngayLam) >= todayKey())
    .map((item) => {
      const ca = shiftById.value.get(Number(item?.idCaLam))
      const tenCa = ca?.tenCa || `Ca #${item?.idCaLam}`
      const gioBatDau = normalizeTime(ca?.gioBatDau)
      const gioKetThuc = normalizeTime(ca?.gioKetThuc)
      return {
        id: item?.id,
        idCaLam: item?.idCaLam,
        ngayLam: normalizeDate(item?.ngayLam),
        tenCa,
        timeRange: `${gioBatDau} - ${gioKetThuc}`
      }
    })
    .sort((a, b) => String(a.ngayLam).localeCompare(String(b.ngayLam)))
})

const targetEmployees = computed(() => {
  return employees.value
    .filter((item) => Number(item?.id) !== Number(currentEmployeeId.value))
    .map((item) => ({ id: Number(item.id), name: item.tenNhanVien || `NV #${item.id}` }))
    .sort((a, b) => a.name.localeCompare(b.name, 'vi'))
})

const submitRequest = () => {
  if (!form.scheduleId || !form.targetEmployeeId || !String(form.reason || '').trim()) {
    toast.error('Vui lòng nhập đủ thông tin yêu cầu đổi ca.')
    return
  }

  const schedule = ownFutureSchedules.value.find((item) => Number(item.id) === Number(form.scheduleId))
  if (!schedule) {
    toast.error('Ca làm không hợp lệ.')
    return
  }

  const requestItem = {
    id: `DC-${Date.now()}`,
    requesterId: Number(currentEmployeeId.value),
    requesterName: currentEmployeeName.value,
    targetEmployeeId: Number(form.targetEmployeeId),
    targetEmployeeName: employeeNameById.value.get(Number(form.targetEmployeeId)) || `NV #${form.targetEmployeeId}`,
    scheduleId: Number(schedule.id),
    idCaLam: Number(schedule.idCaLam),
    ngayLam: schedule.ngayLam,
    tenCa: schedule.tenCa,
    reason: String(form.reason).trim(),
    status: 'Cho duyet',
    createdAt: new Date().toISOString(),
    source: 'local'
  }

  persistRequest(requestItem)

  form.scheduleId = ''
  form.targetEmployeeId = ''
  form.reason = ''

  toast.success('Đã gửi yêu cầu đổi ca. Chờ admin duyệt.')
}

const loadData = async () => {
  loading.value = true
  try {
    const [employeeRes, shiftRes, scheduleRes] = await Promise.all([
      getAllNhanVien(),
      getAllCaLam(),
      getAllLichLamViecFull()
    ])

    employees.value = extractList(employeeRes?.data)
    shifts.value = extractList(shiftRes?.data)
    schedules.value = extractList(scheduleRes?.data)

    await resolveCurrentEmployee()
    loadLocalRequests()
  } catch (error) {
    console.error('Load doi ca data failed:', error)
    toast.error('Không thể tải dữ liệu đăng ký đổi ca')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="doi-ca-page">
    <div class="head">
      <h1>Đăng ký đổi ca</h1>
      <p>Gửi yêu cầu đổi ca cho admin phê duyệt. Nhân viên chỉ gửi yêu cầu, không tự cập lịch.</p>
    </div>

    <div v-if="loading" class="card">Dang tai du lieu...</div>

    <template v-else>
      <div class="card">
        <h3>Tạo yêu cầu mới</h3>

        <div class="grid">
          <label>
            Ca của bạn
            <select v-model="form.scheduleId">
              <option value="">Chọn ca muốn đổi</option>
              <option v-for="item in ownFutureSchedules" :key="item.id" :value="item.id">
                {{ item.ngayLam }} | {{ item.tenCa }} ({{ item.timeRange }})
              </option>
            </select>
          </label>

          <label>
            Nhân viên muốn đổi
            <select v-model="form.targetEmployeeId">
              <option value="">Chọn nhân viên</option>
              <option v-for="emp in targetEmployees" :key="emp.id" :value="emp.id">
                {{ emp.name }}
              </option>
            </select>
          </label>

          <label class="full">
            Lý do
            <textarea v-model="form.reason" rows="3" placeholder="Nhập lý do đổi ca..."></textarea>
          </label>
        </div>

        <div class="actions">
          <button class="btn primary" @click="submitRequest">Gửi yêu cầu</button>
        </div>

        <small class="hint">Lưu ý: hiện tại trang này lưu request ở local để demo luồng nghiệp vụ. Khi backend có endpoint đổi ca, sẽ chuyển sang phê duyệt thực tế.</small>
      </div>

      <div class="card">
        <h3>Yêu cầu của tôi</h3>
        <table class="table" v-if="requests.length">
          <thead>
            <tr>
              <th>Ngày</th>
              <th>Ca</th>
              <th>Người đổi</th>
              <th>Ly do</th>
              <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="req in requests" :key="req.id">
              <td>{{ req.ngayLam }}</td>
              <td>{{ req.tenCa }}</td>
              <td>{{ req.targetEmployeeName }}</td>
              <td>{{ req.reason }}</td>
              <td><span class="badge">{{ req.status }}</span></td>
            </tr>
          </tbody>
        </table>
        <p v-else class="muted">Bạn chưa có yêu cầu đổi ca nào.</p>
      </div>
    </template>
  </div>
</template>

<style scoped>
.doi-ca-page {
  padding: 24px;
}

.head h1 {
  margin: 0;
  font-size: 24px;
  color: #111827;
}

.head p {
  margin: 8px 0 16px;
  color: #6b7280;
}

.card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 14px;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #374151;
  font-size: 14px;
}

.full {
  grid-column: 1 / -1;
}

select,
textarea {
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px;
  font-size: 14px;
}

.actions {
  margin-top: 10px;
}

.btn {
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 14px;
  cursor: pointer;
}

.btn.primary {
  border-color: #dc2626;
  background: #dc2626;
  color: #fff;
}

.hint {
  display: block;
  margin-top: 10px;
  color: #6b7280;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th,
.table td {
  border-bottom: 1px solid #f1f5f9;
  padding: 10px;
  text-align: left;
}

.badge {
  background: #fef3c7;
  color: #92400e;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 12px;
}

.muted {
  color: #6b7280;
}

@media (max-width: 768px) {
  .doi-ca-page {
    padding: 14px;
  }

  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
