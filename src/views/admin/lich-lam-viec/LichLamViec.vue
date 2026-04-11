<template>
  <div class="lich-lam-viec-page">
    <header class="hero">
      <div>
        <h1 class="page-title">Quản Lý Lịch Làm Việc</h1>
        <p class="page-subtitle">Theo dõi ca làm, lọc theo nhân viên và xem lịch theo ngày, tuần hoặc tháng.</p>
      </div>
      <div class="hero-actions">
        <button class="btn btn-ghost" @click="loadAllData">
          <span class="material-icons-outlined">refresh</span>
          Tải lại
        </button>
        <button class="btn btn-primary" @click="addSchedule">
          <span class="material-icons-outlined">add</span>
          Thêm mới lịch làm việc
        </button>
      </div>
    </header>

    <section class="summary-grid">
      <article class="summary-card">
        <div class="summary-label">Tổng ca làm</div>
        <div class="summary-value">{{ filteredSchedules.length }}</div>
      </article>
      <article class="summary-card">
        <div class="summary-label">Đang hoạt động</div>
        <div class="summary-value">{{ activeCount }}</div>
      </article>
      <article class="summary-card">
        <div class="summary-label">Ngưng hoạt động</div>
        <div class="summary-value">{{ inactiveCount }}</div>
      </article>
      <article class="summary-card">
        <div class="summary-label">Nhân viên khả dụng</div>
        <div class="summary-value">{{ employees.length }}</div>
      </article>
    </section>

    <section class="panel options-panel">
      <div class="panel-title">
        <span class="material-icons-outlined">tune</span>
        Tùy chọn
      </div>

      <div class="panel-controls">
        <div class="field employee-field">
          <label>Nhân viên</label>
          <input
            v-model="filters.employeeKeyword"
            type="text"
            class="form-input"
            list="employee-options"
            placeholder="Tìm kiếm nhân viên..."
          />
          <datalist id="employee-options">
            <option v-for="employee in employeeSuggestions" :key="employee.id" :value="employee.display" />
          </datalist>
          <small class="field-hint">
            {{ selectedEmployee ? `Đang chọn: ${selectedEmployee.display}` : 'Đang xem tất cả nhân viên' }}
          </small>
        </div>

        <div class="field">
          <label>Tìm kiếm ca</label>
          <input
            v-model="filters.search"
            type="text"
            class="form-input"
            placeholder="Nhập tên ca, mô tả hoặc mã ca..."
          />
        </div>

        <div class="field">
          <label>Giờ bắt đầu</label>
          <select v-model="filters.startTime" class="form-select">
            <option value="">Tất cả</option>
            <option value="morning">Sáng (7h-12h)</option>
            <option value="afternoon">Chiều (12h-17h)</option>
            <option value="evening">Tối (17h-22h)</option>
          </select>
        </div>

        <div class="field">
          <label>Giờ kết thúc</label>
          <select v-model="filters.endTime" class="form-select">
            <option value="">Tất cả</option>
            <option value="morning">12h</option>
            <option value="afternoon">17h</option>
            <option value="evening">22h</option>
          </select>
        </div>

        <div class="field">
          <label>Trạng thái</label>
          <select v-model="filters.status" class="form-select">
            <option value="">Tất cả</option>
            <option value="active">Hoạt động</option>
            <option value="inactive">Ngưng</option>
          </select>
        </div>
      </div>
    </section>

    <section class="panel import-panel">
      <div class="panel-title">
        <span class="material-icons-outlined">upload_file</span>
        Nhập dữ liệu từ Excel/CSV
      </div>

      <div class="import-row">
        <p class="import-desc">
          Hỗ trợ CSV mẫu: <code>date, employee, shift, start, end, status</code>. Dữ liệu nhập sẽ hiển thị trực tiếp trên lịch.
        </p>
        <button class="btn btn-import" @click="triggerImport" :disabled="importing">
          <span class="material-icons-outlined">file_upload</span>
          {{ importing ? 'Đang nhập...' : 'Import File' }}
        </button>
        <input
          ref="importInput"
          type="file"
          accept=".csv,.xlsx,.xls"
          class="hidden-input"
          @change="handleImportFile"
        />
      </div>
      <p v-if="lastImportedFile" class="import-file">
        File gần nhất: <strong>{{ lastImportedFile }}</strong> ({{ importedAssignments.length }} bản ghi)
      </p>
    </section>

    <section class="panel calendar-panel">
      <div class="calendar-toolbar">
        <div class="left-tools">
          <div class="segment">
            <button class="segment-btn" @click="goPrevious"><span class="material-icons-outlined">chevron_left</span></button>
            <button class="segment-btn" @click="goNext"><span class="material-icons-outlined">chevron_right</span></button>
          </div>
          <button class="btn btn-ghost small" @click="goToday">Hôm nay</button>
          <div class="range-label">{{ rangeLabel }}</div>
        </div>

        <div class="right-tools">
          <div class="segment">
            <button class="segment-btn" :class="{ active: viewMode === 'table' }" @click="viewMode = 'table'">Bảng</button>
            <button class="segment-btn" :class="{ active: viewMode === 'calendar' }" @click="viewMode = 'calendar'">Lịch</button>
          </div>

          <div class="segment" v-if="viewMode === 'calendar'">
            <button class="segment-btn" :class="{ active: calendarScope === 'day' }" @click="calendarScope = 'day'">Ngày</button>
            <button class="segment-btn" :class="{ active: calendarScope === 'week' }" @click="calendarScope = 'week'">Tuần</button>
            <button class="segment-btn" :class="{ active: calendarScope === 'month' }" @click="calendarScope = 'month'">Tháng</button>
          </div>
        </div>
      </div>

      <div v-if="viewMode === 'calendar'" class="calendar-grid" :class="`scope-${calendarScope}`">
        <div class="calendar-head" v-for="head in visibleWeekHeaders" :key="head.key">{{ head.label }}</div>

        <article
          v-for="day in visibleCalendarDays"
          :key="day.key"
          class="calendar-cell"
          :class="{ muted: !day.isCurrentMonth, today: day.isToday }"
        >
          <header class="cell-head">
            <strong>{{ day.day }}</strong>
            <small>{{ day.monthLabel }}</small>
          </header>

          <div class="cell-events">
            <template v-if="getEntriesByDate(day.dateKey).length">
              <div
                v-for="entry in getEntriesByDate(day.dateKey).slice(0, 3)"
                :key="entry.key"
                class="event-pill"
                :class="entry.status"
              >
                <span class="event-time">{{ entry.startTime }}-{{ entry.endTime }}</span>
                <span class="event-name">{{ entry.shiftName }}</span>
                <span v-if="entry.employeeName" class="event-employee">{{ entry.employeeName }}</span>
              </div>
              <button
                v-if="getEntriesByDate(day.dateKey).length > 3"
                class="more-btn"
                @click="showDayDetail(day.dateKey)"
              >
                +{{ getEntriesByDate(day.dateKey).length - 3 }} ca
              </button>
            </template>
            <p v-else class="no-event">Trống</p>
          </div>
        </article>
      </div>

      <div v-else class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>STT</th>
              <th>Thông tin ca</th>
              <th>Bắt đầu</th>
              <th>Kết thúc</th>
              <th>Trạng thái</th>
              <th class="text-center">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="filteredSchedules.length === 0">
              <td colspan="6" class="empty-state">
                <span class="material-icons-outlined">event_busy</span>
                <p>Không có dữ liệu</p>
              </td>
            </tr>
            <tr v-for="(schedule, index) in filteredSchedules" :key="schedule.id">
              <td>{{ index + 1 }}</td>
              <td>
                <div class="schedule-info">
                  <div class="schedule-icon" :class="schedule.type">
                    <span class="material-icons-outlined">{{ schedule.icon }}</span>
                  </div>
                  <div>
                    <div class="schedule-name">{{ schedule.name }}</div>
                    <div class="schedule-desc">{{ schedule.description || 'Không có mô tả' }}</div>
                  </div>
                </div>
              </td>
              <td>{{ schedule.startTime }}</td>
              <td>{{ schedule.endTime }}</td>
              <td>
                <span class="status-badge" :class="schedule.status">{{ getStatusText(schedule.status) }}</span>
              </td>
              <td class="text-center">
                <div class="action-buttons">
                  <button class="btn-icon" @click="editSchedule(schedule)" title="Chỉnh sửa">
                    <span class="material-icons-outlined">visibility</span>
                  </button>
                  <button class="btn-icon danger" @click="deleteSchedule(schedule)" title="Xóa">
                    <span class="material-icons-outlined">delete</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="scheduleModal.open" class="modal-overlay" @click.self="closeScheduleModal">
      <div class="modal-card">
        <h3 class="modal-title">{{ scheduleModal.mode === 'create' ? 'Thêm lịch làm việc' : 'Cập nhật lịch làm việc' }}</h3>

        <div class="modal-grid">
          <div class="modal-field">
            <label>Nhân viên</label>
            <select v-model="scheduleForm.idNhanVien" class="form-select">
              <option v-for="employee in employees" :key="employee.id" :value="employee.id">{{ employee.display }}</option>
            </select>
          </div>

          <div class="modal-field">
            <label>Ca làm</label>
            <select v-model="scheduleForm.idCaLam" class="form-select">
              <option v-for="shift in shiftsCatalog" :key="shift.id" :value="shift.id">
                {{ shift.name }} ({{ shift.startTime }}-{{ shift.endTime }})
              </option>
            </select>
          </div>

          <div class="modal-field">
            <label>Ngày làm</label>
            <input v-model="scheduleForm.ngayLam" class="form-input" type="date" />
          </div>

          <div class="modal-field">
            <label>Trạng thái</label>
            <select v-model="scheduleForm.trangThai" class="form-select">
              <option value="Đã phân công">Đã phân công</option>
              <option value="Hoạt động">Hoạt động</option>
              <option value="Ngưng">Ngưng</option>
            </select>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn-modal" @click="closeScheduleModal">Hủy</button>
          <button class="btn-modal btn-modal-primary" @click="submitScheduleForm">{{ scheduleModal.mode === 'create' ? 'Thêm' : 'Lưu' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useToast } from '../../../composables/useToast'
import { createLichLamViec, deleteLichLamViec, getAllLichLamViec, getAllLichLamViecFull } from '../../../services/lichLamViecService'
import { getAllNhanVien } from '../../../services/nhanVienService'
import { getAllCaLam } from '../../../services/caLamService'
import taiKhoanService from '../../../services/taiKhoanService'

const toast = useToast()

const filters = reactive({
  employeeKeyword: '',
  search: '',
  startTime: '',
  endTime: '',
  status: ''
})

const schedules = ref([])
const employees = ref([])
const shiftsCatalog = ref([])
const scheduleModal = reactive({
  open: false,
  mode: 'create',
  editingId: null
})
const scheduleForm = reactive({
  idNhanVien: null,
  idCaLam: null,
  ngayLam: new Date().toISOString().slice(0, 10),
  trangThai: 'Đã phân công'
})
const importedAssignments = ref([])
const importing = ref(false)
const lastImportedFile = ref('')
const importInput = ref(null)
const viewMode = ref('calendar')
const calendarScope = ref('week')
const cursorDate = ref(new Date())

const weekLabels = ['CN', 'Th 2', 'Th 3', 'Th 4', 'Th 5', 'Th 6', 'Th 7']

const normalizeTime = (timeValue) => {
  if (!timeValue) return ''
  const raw = String(timeValue).trim()
  if (/^\d{2}:\d{2}$/.test(raw)) return raw
  if (/^\d{2}:\d{2}:\d{2}$/.test(raw)) return raw.slice(0, 5)
  return raw
}

const normalizeDateKey = (dateValue) => {
  if (!dateValue) return ''
  const raw = String(dateValue).trim()
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw

  const match = raw.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/)
  if (!match) return ''

  const day = match[1].padStart(2, '0')
  const month = match[2].padStart(2, '0')
  return `${match[3]}-${month}-${day}`
}

const toDateKey = (date) => {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

const mapScheduleType = (startTime) => {
  const hour = Number(String(startTime || '').split(':')[0])
  if (Number.isNaN(hour)) return { type: 'morning', icon: 'schedule' }
  if (hour < 12) return { type: 'morning', icon: 'wb_sunny' }
  if (hour < 17) return { type: 'afternoon', icon: 'wb_twilight' }
  return { type: 'evening', icon: 'nights_stay' }
}

const mapApiSchedule = (item) => {
  const startTime = normalizeTime(item?.gioBatDau)
  const endTime = normalizeTime(item?.gioKetThuc)
  const employeeName = item?.tenNhanVien || ''
  const dateKey = normalizeDateKey(item?.ngayLam)
  const statusText = String(item?.trangThai || '').toLowerCase()
  const status = statusText.includes('hoạt động')
    || statusText.includes('hoat dong')
    || statusText.includes('đã phân công')
    || statusText.includes('da phan cong')
    ? 'active'
    : 'inactive'
  const visual = mapScheduleType(startTime)

  const descriptionParts = []
  if (employeeName) descriptionParts.push(`NV: ${employeeName}`)
  if (dateKey) {
    const [year, month, day] = dateKey.split('-')
    descriptionParts.push(`Ngày: ${day}/${month}/${year}`)
  }
  if (item?.moTa) descriptionParts.push(item.moTa)

  return {
    id: item?.id,
    employeeId: item?.idNhanVien || null,
    shiftId: item?.idCaLam || null,
    employeeName,
    dateKey,
    type: visual.type,
    icon: visual.icon,
    name: item?.tenCa || item?.tenLich || 'Chưa đặt tên',
    description: descriptionParts.join(' | '),
    startTime,
    endTime,
    status
  }
}

const mapEmployee = (item) => {
  const id = item?.id
  const code = item?.maNhanVien || `NV-${id || ''}`
  const name = item?.tenNhanVien || 'Không rõ tên'
  return {
    id,
    code,
    name,
    display: `${code} - ${name}`
  }
}

const extractList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const normalizeRole = (role) => String(role || '').trim().toUpperCase().replace(/^ROLE_/, '')

const isEmployeeRole = (role) => {
  const normalized = normalizeRole(role)
  return normalized === 'EMPLOYEE' || normalized === 'NHAN_VIEN' || normalized === 'NHANVIEN'
}

const selectedEmployee = computed(() => {
  const keyword = filters.employeeKeyword.trim().toLowerCase()
  if (!keyword) return null
  return employees.value.find((employee) => employee.display.toLowerCase() === keyword) || null
})

const employeeSuggestions = computed(() => {
  const keyword = filters.employeeKeyword.trim().toLowerCase()
  if (!keyword) return employees.value.slice(0, 20)
  return employees.value
    .filter((employee) => employee.display.toLowerCase().includes(keyword))
    .slice(0, 20)
})

const filteredSchedules = computed(() => {
  return schedules.value.filter((schedule) => {
    const matchEmployee = !selectedEmployee.value
      || schedule.employeeId === selectedEmployee.value.id
      || schedule.employeeName === selectedEmployee.value.name

    const keyword = filters.search.trim().toLowerCase()
    const matchKeyword = !keyword
      || schedule.name.toLowerCase().includes(keyword)
      || schedule.description.toLowerCase().includes(keyword)
      || String(schedule.id || '').includes(keyword)

    const mapStart = {
      morning: schedule.startTime >= '07:00' && schedule.startTime < '12:00',
      afternoon: schedule.startTime >= '12:00' && schedule.startTime < '17:00',
      evening: schedule.startTime >= '17:00'
    }

    const mapEnd = {
      morning: schedule.endTime === '12:00',
      afternoon: schedule.endTime === '17:00',
      evening: schedule.endTime === '22:00'
    }

    const matchStart = !filters.startTime || !!mapStart[filters.startTime]
    const matchEnd = !filters.endTime || !!mapEnd[filters.endTime]
    const matchStatus = !filters.status || schedule.status === filters.status

    return matchEmployee && matchKeyword && matchStart && matchEnd && matchStatus
  })
})

const activeCount = computed(() => filteredSchedules.value.filter((item) => item.status === 'active').length)
const inactiveCount = computed(() => filteredSchedules.value.filter((item) => item.status === 'inactive').length)

const startOfWeek = (dateValue) => {
  const date = new Date(dateValue)
  date.setHours(0, 0, 0, 0)
  date.setDate(date.getDate() - date.getDay())
  return date
}

const visibleCalendarDays = computed(() => {
  const current = new Date(cursorDate.value)
  current.setHours(0, 0, 0, 0)

  if (calendarScope.value === 'day') {
    return [buildDayInfo(current, true)]
  }

  if (calendarScope.value === 'week') {
    const first = startOfWeek(current)
    return Array.from({ length: 7 }, (_, index) => {
      const day = new Date(first)
      day.setDate(first.getDate() + index)
      return buildDayInfo(day, true)
    })
  }

  const firstOfMonth = new Date(current.getFullYear(), current.getMonth(), 1)
  const firstGrid = startOfWeek(firstOfMonth)
  return Array.from({ length: 42 }, (_, index) => {
    const day = new Date(firstGrid)
    day.setDate(firstGrid.getDate() + index)
    return buildDayInfo(day, day.getMonth() === current.getMonth())
  })
})

const visibleWeekHeaders = computed(() => {
  if (calendarScope.value === 'day') {
    const day = visibleCalendarDays.value[0]
    return [{ key: day.key, label: `${weekLabels[new Date(day.dateKey).getDay()]} (${day.day}/${new Date(day.dateKey).getMonth() + 1})` }]
  }
  return weekLabels.map((label, index) => ({ key: index, label }))
})

const rangeLabel = computed(() => {
  const current = new Date(cursorDate.value)
  if (calendarScope.value === 'day') {
    return `Ngày ${current.getDate()} Tháng ${current.getMonth() + 1}, ${current.getFullYear()}`
  }

  if (calendarScope.value === 'week') {
    const first = startOfWeek(current)
    const last = new Date(first)
    last.setDate(first.getDate() + 6)
    return `Tuần ${toDateKey(first).slice(8, 10)}/${toDateKey(first).slice(5, 7)} - ${toDateKey(last).slice(8, 10)}/${toDateKey(last).slice(5, 7)}`
  }

  return `Tháng ${current.getMonth() + 1} Năm ${current.getFullYear()}`
})

const getStatusText = (status) => (status === 'active' ? 'Hoạt động' : 'Ngưng')

const buildDayInfo = (date, isCurrentMonth) => {
  const todayKey = toDateKey(new Date())
  return {
    key: `${toDateKey(date)}-${isCurrentMonth ? 'current' : 'outside'}`,
    dateKey: toDateKey(date),
    day: date.getDate(),
    monthLabel: `Th${date.getMonth() + 1}`,
    isCurrentMonth,
    isToday: toDateKey(date) === todayKey
  }
}

const getEntriesByDate = (dateKey) => {
  if (importedAssignments.value.length > 0) {
    return importedAssignments.value
      .filter((entry) => {
        const employeeMatch = !selectedEmployee.value || selectedEmployee.value.name === entry.employeeName
        return entry.dateKey === dateKey && employeeMatch
      })
      .map((entry, index) => ({ ...entry, key: `${dateKey}-${entry.shiftName}-${index}` }))
  }

  return filteredSchedules.value.map((schedule, index) => ({
    ...schedule,
    key: `${dateKey}-${schedule.id || index}`,
    shiftName: schedule.name,
    startTime: schedule.startTime,
    endTime: schedule.endTime,
    status: schedule.status
  })).filter((entry) => !entry.dateKey || entry.dateKey === dateKey)
}

const goToday = () => {
  cursorDate.value = new Date()
}

const goPrevious = () => {
  const date = new Date(cursorDate.value)
  if (calendarScope.value === 'day') date.setDate(date.getDate() - 1)
  if (calendarScope.value === 'week') date.setDate(date.getDate() - 7)
  if (calendarScope.value === 'month') date.setMonth(date.getMonth() - 1)
  cursorDate.value = date
}

const goNext = () => {
  const date = new Date(cursorDate.value)
  if (calendarScope.value === 'day') date.setDate(date.getDate() + 1)
  if (calendarScope.value === 'week') date.setDate(date.getDate() + 7)
  if (calendarScope.value === 'month') date.setMonth(date.getMonth() + 1)
  cursorDate.value = date
}

const loadSchedules = async () => {
  try {
    let fullRes = null
    try {
      fullRes = await getAllLichLamViecFull()
    } catch (error) {
      fullRes = null
    }

    let data = extractList(fullRes?.data)

    // Some environments expose /full but still return an empty list.
    if (data.length === 0) {
      const baseRes = await getAllLichLamViec()
      data = extractList(baseRes?.data)
    }

    schedules.value = data.map(mapApiSchedule)
  } catch (err) {
    console.error('Load schedules failed:', err)
    schedules.value = []
    toast.error('Không tải được dữ liệu ca làm việc')
  }
}

const loadEmployees = async () => {
  try {
    const [nhanVienRes, taiKhoanRes] = await Promise.all([
      getAllNhanVien(),
      taiKhoanService.getAll().catch(() => ({ data: [] }))
    ])

    const nhanVienData = extractList(nhanVienRes?.data)
    const taiKhoanData = extractList(taiKhoanRes?.data)
    const taiKhoanRoleById = new Map(taiKhoanData.map((item) => [item?.id, item?.vaiTro]))

    const employeeOnly = nhanVienData.filter((item) => {
      const inlineRole = item?.taiKhoan?.vaiTro
      const accountId = item?.taiKhoan?.id
      const mappedRole = taiKhoanRoleById.get(accountId)
      const role = inlineRole || mappedRole
      return isEmployeeRole(role)
    })

    const fallbackEmployees = employeeOnly.length > 0 ? employeeOnly : nhanVienData
    employees.value = fallbackEmployees.map(mapEmployee)
  } catch (err) {
    console.error('Load employees failed:', err)
    employees.value = []
    toast.error('Không tải được danh sách nhân viên')
  }
}

const loadShiftsCatalog = async () => {
  try {
    const res = await getAllCaLam()
    const data = extractList(res?.data)
    shiftsCatalog.value = data.map((item) => ({
      id: item?.id,
      name: item?.tenCa || `Ca #${item?.id}`,
      startTime: normalizeTime(item?.gioBatDau),
      endTime: normalizeTime(item?.gioKetThuc)
    }))
  } catch (error) {
    console.error('Load shifts catalog failed:', error)
    shiftsCatalog.value = []
  }
}

const seedDailySchedules = async (dates) => {
  if (!employees.value.length || !shiftsCatalog.value.length) return

  // Use up to 3 distinct shifts (sáng/chiều/tối)
  const shifts3 = shiftsCatalog.value.slice(0, 3)
  let seeded = 0

  for (const dateKey of dates) {
    const existingForDate = schedules.value.filter((s) => s.dateKey === dateKey)
    const needed = 3 - existingForDate.length
    if (needed <= 0) continue

    const alreadyAssignedIds = new Set(existingForDate.map((s) => Number(s.employeeId)).filter(Boolean))
    const available = employees.value.filter((e) => !alreadyAssignedIds.has(Number(e.id)))

    // Shuffle randomly and pick `needed` employees
    const shuffled = [...available].sort(() => Math.random() - 0.5)
    const selected = shuffled.slice(0, needed)

    for (let i = 0; i < selected.length; i++) {
      const employee = selected[i]
      const shift = shifts3[i % shifts3.length]
      if (!employee?.id || !shift?.id) continue

      try {
        await createLichLamViec({
          idNhanVien: Number(employee.id),
          idCaLam: Number(shift.id),
          ngayLam: dateKey,
          trangThai: 'Đã phân công'
        })
        seeded++
      } catch {
        // Skip duplicates / API errors silently
      }
    }
  }

  if (seeded > 0) {
    toast.success(`Đã tự động tạo ${seeded} lịch làm việc cho hôm nay / ngày mai`)
    await loadSchedules()
  }
}

const loadAllData = async () => {
  await Promise.all([loadSchedules(), loadEmployees(), loadShiftsCatalog()])

  if (!schedules.value.length && employees.value.length && shiftsCatalog.value.length) {
    await seedSchedulesFromMasterData()
    await loadSchedules()
  }

  // Auto-generate at least 3 shifts for today and tomorrow
  if (employees.value.length && shiftsCatalog.value.length) {
    const today = toDateKey(new Date())
    const tomorrowDate = new Date()
    tomorrowDate.setDate(tomorrowDate.getDate() + 1)
    const tomorrow = toDateKey(tomorrowDate)
    await seedDailySchedules([today, tomorrow])
  }
}

const seedSchedulesFromMasterData = async () => {
  const start = startOfWeek(new Date())
  const maxEmployees = employees.value.slice(0, 10)
  const daySlots = [0, 1, 2, 3, 4, 5, 6]
  const maxSeedRows = 20
  const payloads = []

  maxEmployees.forEach((employee, employeeIndex) => {
    daySlots.forEach((dayOffset) => {
      const date = new Date(start)
      date.setDate(start.getDate() + dayOffset)
      const shift = shiftsCatalog.value[(employeeIndex + dayOffset) % shiftsCatalog.value.length]

      if (!employee?.id || !shift?.id) return
      payloads.push({
        idNhanVien: Number(employee.id),
        idCaLam: Number(shift.id),
        ngayLam: toDateKey(date),
        trangThai: 'Đã phân công'
      })

      if (payloads.length >= maxSeedRows) return
    })

    if (payloads.length >= maxSeedRows) return
  })

  const results = await Promise.allSettled(payloads.map((payload) => createLichLamViec(payload)))
  const successCount = results.filter((result) => result.status === 'fulfilled').length
  if (successCount > 0) {
    toast.success(`Đã khởi tạo ${successCount} lịch làm việc từ dữ liệu nhân viên/ca`) 
  }
}

const resetScheduleForm = () => {
  scheduleForm.idNhanVien = employees.value[0]?.id || null
  scheduleForm.idCaLam = shiftsCatalog.value[0]?.id || null
  scheduleForm.ngayLam = new Date().toISOString().slice(0, 10)
  scheduleForm.trangThai = 'Đã phân công'
}

const openCreateScheduleModal = () => {
  resetScheduleForm()
  scheduleModal.mode = 'create'
  scheduleModal.editingId = null
  scheduleModal.open = true
}

const openEditScheduleModal = (schedule) => {
  scheduleModal.mode = 'edit'
  scheduleModal.editingId = schedule?.id || null
  scheduleForm.idNhanVien = schedule?.employeeId || employees.value[0]?.id || null
  scheduleForm.idCaLam = schedule?.shiftId || shiftsCatalog.value[0]?.id || null
  scheduleForm.ngayLam = schedule?.dateKey || new Date().toISOString().slice(0, 10)
  scheduleForm.trangThai = schedule?.status === 'inactive' ? 'Ngưng' : 'Đã phân công'
  scheduleModal.open = true
}

const closeScheduleModal = () => {
  scheduleModal.open = false
  scheduleModal.editingId = null
}

const createScheduleFromInput = async () => {
  if (!employees.value.length) {
    toast.error('Không có nhân viên khả dụng để tạo lịch')
    return false
  }

  if (!shiftsCatalog.value.length) {
    toast.error('Không có ca làm khả dụng để tạo lịch')
    return false
  }

  const idNhanVien = Number(scheduleForm.idNhanVien)
  if (!employees.value.some((item) => item.id === idNhanVien)) {
    toast.error('ID nhân viên không hợp lệ')
    return false
  }

  const idCaLam = Number(scheduleForm.idCaLam)
  if (!shiftsCatalog.value.some((item) => item.id === idCaLam)) {
    toast.error('ID ca làm không hợp lệ')
    return false
  }

  const ngayLam = String(scheduleForm.ngayLam || '').trim()
  if (!/^\d{4}-\d{2}-\d{2}$/.test(ngayLam)) {
    toast.error('Ngày làm không hợp lệ, dùng định dạng yyyy-mm-dd')
    return false
  }

  const payload = {
    idNhanVien,
    idCaLam,
    ngayLam,
    trangThai: String(scheduleForm.trangThai || '').trim() || 'Đã phân công'
  }

  try {
    await createLichLamViec(payload)
    return true
  } catch (error) {
    console.error('Create schedule failed:', error)
    toast.error('Tạo lịch làm việc thất bại')
    return false
  }
}

const addSchedule = async () => {
  if (!employees.value.length || !shiftsCatalog.value.length) {
    toast.error('Không đủ dữ liệu nhân viên/ca để tạo lịch')
    return
  }

  openCreateScheduleModal()
}

const submitScheduleForm = async () => {
  const created = await createScheduleFromInput()
  if (!created) return

  if (scheduleModal.mode === 'edit' && scheduleModal.editingId) {
    try {
      await deleteLichLamViec(scheduleModal.editingId)
      toast.success('Cập nhật lịch làm việc thành công')
    } catch (error) {
      console.error('Delete old schedule failed:', error)
      toast.warning('Đã tạo lịch mới nhưng chưa xóa được lịch cũ')
    }
  } else {
    toast.success('Thêm lịch làm việc thành công')
  }

  closeScheduleModal()
  loadSchedules()
}

const editSchedule = async (schedule) => {
  if (!schedule?.id) return
  openEditScheduleModal(schedule)
}

const deleteSchedule = async (schedule) => {
  if (!schedule?.id) return
  const confirmed = await window.confirmDialog(`Bạn có chắc muốn xóa lịch "${schedule.name}"?`)
  if (!confirmed) return

  deleteLichLamViec(schedule.id)
    .then(() => {
      toast.success('Xóa lịch thành công')
      loadSchedules()
    })
    .catch((err) => {
      console.error('Delete schedule failed:', err)
      toast.error('Xóa ca thất bại')
    })
}

const triggerImport = () => {
  importInput.value?.click()
}

const handleImportFile = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  lastImportedFile.value = file.name
  const extension = file.name.split('.').pop()?.toLowerCase()

  if (extension !== 'csv') {
    toast.info('Tạm thời hỗ trợ đọc CSV trực tiếp. File Excel sẽ được hỗ trợ ở bước tiếp theo.')
    event.target.value = ''
    return
  }

  importing.value = true
  try {
    const text = await file.text()
    const lines = text.split(/\r?\n/).filter(Boolean)
    if (lines.length < 2) {
      toast.error('File CSV không có dữ liệu hợp lệ')
      importing.value = false
      return
    }

    const rows = lines.slice(1).map((line) => line.split(/[,;\t]/).map((part) => part.trim()))
    const mapped = rows
      .map((row) => {
        const dateKey = normalizeDateKey(row[0])
        if (!dateKey) return null
        return {
          dateKey,
          employeeName: row[1] || 'Không rõ nhân viên',
          shiftName: row[2] || 'Ca nhập từ file',
          startTime: normalizeTime(row[3] || '08:00'),
          endTime: normalizeTime(row[4] || '17:00'),
          status: String(row[5] || 'active').toLowerCase().includes('ngưng') ? 'inactive' : 'active'
        }
      })
      .filter(Boolean)

    importedAssignments.value = mapped
    toast.success(`Đã nhập ${mapped.length} bản ghi lịch làm việc`)
  } catch (error) {
    console.error('Import file failed:', error)
    toast.error('Không thể đọc file CSV')
  } finally {
    importing.value = false
    event.target.value = ''
  }
}

const showDayDetail = (dateKey) => {
  const count = getEntriesByDate(dateKey).length
  toast.info(`${dateKey}: ${count} ca làm việc`) 
}

const autoSeedTodayIfNeeded = async () => {
  const todayKey = toDateKey(new Date())
  const storageKey = `llv:auto:${todayKey}`
  if (localStorage.getItem(storageKey)) return

  if (!employees.value.length || !shiftsCatalog.value.length) return

  // Shuffle employees and pick 3 at random
  const shuffled = [...employees.value].sort(() => Math.random() - 0.5)
  const selected = shuffled.slice(0, Math.min(3, shuffled.length))

  // Assign each selected employee to a different shift (round-robin across catalog)
  let seeded = 0
  for (let i = 0; i < selected.length; i++) {
    const shift = shiftsCatalog.value[i % shiftsCatalog.value.length]
    try {
      await createLichLamViec({
        idNhanVien: selected[i].id,
        idCaLam: shift.id,
        ngayLam: todayKey,
        trangThai: 'Đã phân công'
      })
      seeded++
    } catch {
      // silent per-entry failure
    }
  }

  if (seeded > 0) {
    localStorage.setItem(storageKey, '1')
    await loadSchedules()
  }
}

onMounted(async () => {
  await loadAllData()
  await autoSeedTodayIfNeeded()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap');
@import url('https://fonts.googleapis.com/icon?family=Material+Icons+Outlined');

.lich-lam-viec-page {
  --bg: #f4f6fb;
  --card: #ffffff;
  --line: #e6e9f2;
  --text: #111827;
  --muted: #6b7280;
  --brand: #ef4444;
  --brand-deep: #0f172a;
  --accent: #fef2f2;

  font-family: 'Be Vietnam Pro', 'Segoe UI', Tahoma, sans-serif;
  background: transparent;
  min-height: calc(100vh - 76px);
  padding: 24px;
  color: var(--text);
  border: 1px solid #e5eaf4;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.04);
  animation: reveal 0.35s ease;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  line-height: 1.15;
  letter-spacing: -0.03em;
  font-weight: 800;
}

.page-subtitle {
  margin: 8px 0 0;
  max-width: 760px;
  color: var(--muted);
  font-size: 16px;
  font-weight: 500;
}

.hero-actions {
  display: flex;
  gap: 10px;
}

.btn {
  height: 42px;
  border-radius: 12px;
  border: 1px solid transparent;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 700;
}

.btn.small {
  height: 34px;
  border-radius: 10px;
  padding: 0 12px;
}

.btn-primary {
  background: linear-gradient(120deg, #ef4444, #7f1d1d);
  color: #fff;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(127, 29, 29, 0.25);
}

.btn-ghost {
  border-color: var(--line);
  background: #fff;
  color: #334155;
}

.btn-ghost:hover,
.segment-btn:hover,
.btn-icon:hover {
  background: #f8fafc;
}

.btn-import {
  background: linear-gradient(120deg, #ef4444, #0f172a);
  color: #fff;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.summary-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 14px;
}

.summary-label {
  color: var(--muted);
  font-size: 13px;
  font-weight: 600;
}

.summary-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 800;
  color: var(--brand-deep);
}

.panel {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 14px;
}

.panel-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #1f2937;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 14px;
}

.panel-controls {
  display: grid;
  grid-template-columns: 1.2fr 1fr 0.8fr 0.8fr 0.8fr;
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field label {
  font-size: 13px;
  font-weight: 700;
  color: #475569;
}

.field-hint {
  color: #64748b;
  font-size: 12px;
}

.form-input,
.form-select {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid #d2d8e2;
  background-color: #fff;
  color: #0f172a;
  font-size: 14px;
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

.import-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  background: #f8fafc;
  border: 1px solid #edf1f7;
  border-radius: 12px;
  padding: 14px;
}

.import-desc {
  margin: 0;
  color: #475569;
  line-height: 1.35;
}

.import-desc code {
  color: #991b1b;
  font-weight: 700;
}

.hidden-input {
  display: none;
}

.import-file {
  margin: 10px 0 0;
  color: #475569;
}

.calendar-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  align-items: center;
}

.left-tools,
.right-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}

.range-label {
  font-weight: 700;
  color: #1e293b;
}

.segment {
  border: 1px solid #d7dde8;
  border-radius: 11px;
  overflow: hidden;
  display: inline-flex;
}

.segment-btn {
  border: 0;
  background: #fff;
  color: #334155;
  min-width: 38px;
  height: 36px;
  padding: 0 11px;
  font-weight: 700;
  cursor: pointer;
}

.segment-btn.active {
  background: #111827;
  color: #fff;
}

.calendar-grid {
  border: 1px solid #e5eaf3;
  border-radius: 14px;
  overflow: hidden;
  display: grid;
  background: #fff;
}

.calendar-grid.scope-week,
.calendar-grid.scope-month {
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

.calendar-grid.scope-day {
  grid-template-columns: 1fr;
}

.calendar-head {
  background: #f8fafc;
  padding: 10px;
  border-right: 1px solid #edf1f7;
  font-size: 13px;
  font-weight: 800;
  color: #991b1b;
  text-align: center;
}

.calendar-cell {
  min-height: 150px;
  border-top: 1px solid #edf1f7;
  border-right: 1px solid #edf1f7;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.calendar-cell.muted {
  background: #fafbfd;
}

.calendar-cell.today {
  background: var(--accent);
}

.cell-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cell-head strong {
  font-size: 16px;
}

.cell-head small {
  color: #64748b;
}

.cell-events {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.event-pill {
  border-radius: 9px;
  padding: 6px 8px;
  font-size: 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.event-pill.active {
  background: #dcfce7;
  color: #166534;
}

.event-pill.inactive {
  background: #fee2e2;
  color: #b91c1c;
}

.event-time {
  font-weight: 700;
}

.event-employee {
  font-weight: 600;
  opacity: 0.95;
}

.more-btn {
  border: 0;
  border-radius: 8px;
  padding: 5px 8px;
  background: #e2e8f0;
  color: #334155;
  font-size: 12px;
  cursor: pointer;
}

.no-event {
  margin: 0;
  color: #94a3b8;
  font-size: 12px;
  font-style: italic;
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
  padding: 12px 10px;
  border-bottom: 1px solid #eef2f7;
  text-align: left;
  white-space: nowrap;
}

.data-table thead th {
  color: #475569;
  background: #f8fafc;
  font-size: 12px;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.text-center {
  text-align: center !important;
}

.schedule-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.schedule-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.schedule-icon.morning {
  background: #ffedd5;
  color: #9a3412;
}

.schedule-icon.afternoon {
  background: #fee2e2;
  color: #991b1b;
}

.schedule-icon.evening {
  background: #ede9fe;
  color: #5b21b6;
}

.schedule-name {
  color: #0f172a;
  font-weight: 700;
}

.schedule-desc {
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
  color: #166534;
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
  width: 34px;
  height: 34px;
  border-radius: 9px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-icon.danger {
  border-color: #fecaca;
  color: #ef4444;
}

.empty-state {
  padding: 40px 10px;
  text-align: center;
  color: #64748b;
}

.empty-state .material-icons-outlined {
  display: block;
  font-size: 36px;
  margin-bottom: 8px;
  color: #9ca3af;
}

.empty-state p {
  margin: 0;
  color: #334155;
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
  width: min(580px, 100%);
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
  border-color: #ef4444;
  background: #ef4444;
  color: #fff;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel-controls {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .import-row {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 768px) {
  .lich-lam-viec-page {
    padding: 14px;
  }

  .hero {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-actions,
  .left-tools,
  .right-tools,
  .calendar-toolbar {
    flex-wrap: wrap;
  }

  .summary-grid,
  .panel-controls {
    grid-template-columns: 1fr;
  }

  .page-title {
    font-size: 20px;
  }

  .calendar-cell {
    min-height: 130px;
  }

  .modal-grid {
    grid-template-columns: 1fr;
  }
}

@keyframes reveal {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
