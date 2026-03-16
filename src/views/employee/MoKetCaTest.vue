<template>
  <div class="mk-wrap">
    <section class="mk-head">
      <div>
        <h1>Mở ca làm việc</h1>
        <p>Nhân viên cần mở ca trước khi bán hàng và kết ca khi hết ca làm việc.</p>
      </div>
      <div class="mk-head-meta">
        <div><strong>Nhân viên:</strong> {{ currentUser?.tenNhanVien || currentUser?.hoTen || '---' }}</div>
        <div><strong>Thời gian:</strong> {{ formattedNow }}</div>
      </div>
    </section>

    <section v-if="loading" class="mk-card">Đang tải dữ liệu...</section>

    <section v-else-if="errorMessage && !activeShift" class="mk-card mk-warning">
      <h3>Chưa thể thao tác ca</h3>
      <p>{{ errorMessage }}</p>
    </section>

    <section v-else-if="!activeShift" class="mk-card mk-open-card">
      <div class="mk-open-grid">
        <div>
          <h3>Mở ca làm việc</h3>
          <p class="muted">Vui lòng mở ca để bắt đầu thao tác hóa đơn.</p>
          <p><strong>Ca làm việc:</strong> {{ currentSchedule?.tenCa || '---' }}</p>
          <p><strong>Thời gian làm việc:</strong> {{ currentSchedule?.gioBatDau || '--:--' }} - {{ currentSchedule?.gioKetThuc || '--:--' }}</p>
        </div>

        <div class="mk-form">
          <label>Tiền mặt đầu ca</label>
          <input type="text" :value="formatCurrency(openingCashInput)" @input="onMoneyInput($event, 'open')" placeholder="0" />

          <label>Ghi chú</label>
          <textarea v-model="openNote" rows="3" placeholder="Nhập ghi chú mở ca"></textarea>

          <button class="btn primary" type="button" @click="openShift" :disabled="submitting">{{ submitting ? 'Đang xử lý...' : 'Mở ca' }}</button>
        </div>
      </div>
    </section>

    <section v-else class="mk-content">
      <article class="mk-card">
        <div class="mk-topline">
          <h3>Đóng ca làm việc: {{ activeShift.id }}</h3>
          <span class="mk-badge">{{ currentSchedule?.tenCa || 'Ca làm việc' }}</span>
        </div>
        <p><strong>Giờ mở ca:</strong> {{ formatDateTime(activeShift.startedAt) }}</p>
      </article>

      <article class="mk-card">
        <h3>Đầu ca</h3>
        <div class="mk-summary-row">
          <span>Tiền mặt đầu ca</span>
          <strong>{{ formatCurrency(activeShift.tienDauCaNhap) }} ₫</strong>
        </div>
      </article>

      <article class="mk-card">
        <div class="mk-topline">
          <h3>Trong ca</h3>
          <button class="btn ghost" type="button" @click="refreshRevenue">Cập nhật dữ liệu</button>
        </div>
        <div class="mk-cols4">
          <div class="mk-mini">
            <h4>Bán hàng {{ revenue.orderCount }} hóa đơn</h4>
            <p>1. Tiền mặt <strong>{{ formatCurrency(revenue.cashRevenue) }}</strong></p>
            <p>2. Chuyển khoản <strong>{{ formatCurrency(revenue.transferRevenue) }}</strong></p>
            <p>3. Thẻ <strong>0</strong></p>
            <p class="mk-total">Tổng thu {{ formatCurrency(revenue.cashRevenue + revenue.transferRevenue) }}</p>
          </div>

          <div class="mk-mini">
            <h4>Phiếu thu 0 phiếu</h4>
            <p>1. Tiền mặt <strong>0</strong></p>
            <p>2. Chuyển khoản <strong>0</strong></p>
            <p>3. Thẻ <strong>0</strong></p>
            <p class="mk-total">Tổng thu 0</p>
          </div>

          <div class="mk-mini">
            <h4>Phiếu chi 0 phiếu</h4>
            <p>1. Tiền mặt <strong>0</strong></p>
            <p>2. Chuyển khoản <strong>0</strong></p>
            <p>3. Thẻ <strong>0</strong></p>
            <p class="mk-total">Tổng thu 0</p>
          </div>

          <div class="mk-mini">
            <h4>Trả hàng 0 hóa đơn</h4>
            <p>1. Tiền mặt <strong>0</strong></p>
            <p>2. Chuyển khoản <strong>0</strong></p>
            <p>3. Thẻ <strong>0</strong></p>
            <p class="mk-total">Tổng thu 0</p>
          </div>
        </div>
      </article>

      <article class="mk-card">
        <h3>Cuối ca</h3>
        <div class="mk-close-grid">
          <div>
            <label>Tiền mặt bàn giao thực tế</label>
            <input type="text" :value="formatCurrency(closeCashInput)" @input="onMoneyInput($event, 'close')" placeholder="0" />
          </div>
          <div>
            <label>Số tiền chênh lệch</label>
            <div class="mk-readonly">{{ signedDiff }} ₫</div>
          </div>
          <div>
            <label>Ghi chú kết ca (bắt buộc)</label>
            <textarea v-model="closeNote" rows="3" placeholder="Nhập lý do kết ca"></textarea>
          </div>
        </div>

        <div class="mk-actions">
          <button class="btn primary" type="button" @click="closeShift" :disabled="submitting">{{ submitting ? 'Đang xử lý...' : 'Kết ca' }}</button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { getAllLichLamViecFull } from '../../services/lichLamViecService'
import { createLichSuCa, getLichSuCaByNhanVien, updateLichSuCa } from '../../services/lichSuCaService'
import { getAllHoaDon } from '../../services/hoaDonService'
import { getAllNhanVien, getNhanVienByTaiKhoanId } from '../../services/nhanVienService'
import { getRevenueBucket, isRevenueCountableOrder, resolveInvoiceEmployeeId, resolveRevenueDateKey } from '../../utils/orderRevenue'

const ACTIVE_KEY_PREFIX = 'giao-ca-active'

const loading = ref(true)
const submitting = ref(false)
const errorMessage = ref('')

const now = ref(new Date())
let clockTimer = null
let revenueTimer = null

const idNhanVien = ref(null)
const currentUser = ref(null)
const currentSchedule = ref(null)
const activeShift = ref(null)
const existingHistory = ref(null)

const openingCashInput = ref(0)
const closeCashInput = ref(0)
const openNote = ref('')
const closeNote = ref('')

const revenue = ref({ cashRevenue: 0, transferRevenue: 0, orderCount: 0 })

const formattedNow = computed(() => {
  return now.value.toLocaleString('vi-VN', { hour12: false })
})

const expectedCash = computed(() => Number(activeShift.value?.tienDauCaNhap || 0) + Number(revenue.value?.cashRevenue || 0))
const diff = computed(() => Number(closeCashInput.value || 0) - Number(expectedCash.value || 0))
const signedDiff = computed(() => {
  const val = Number(diff.value || 0)
  const sign = val > 0 ? '+' : ''
  return `${sign}${formatCurrency(val)}`
})

const getTodayDateKey = () => {
  const d = now.value
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const normalizeDateKey = (value) => {
  if (!value) return ''
  if (Array.isArray(value) && value.length >= 3) {
    const [y, m, d] = value
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  }
  const raw = String(value).trim()
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw
  if (raw.includes('T')) return raw.split('T')[0]
  return ''
}

const normalizeTime = (value, fallback = '00:00') => {
  if (!value) return fallback
  const raw = String(value).trim()
  if (/^\d{2}:\d{2}$/.test(raw)) return raw
  if (/^\d{2}:\d{2}:\d{2}$/.test(raw)) return raw.slice(0, 5)
  return fallback
}

const toMinutes = (timeValue) => {
  const normalized = normalizeTime(timeValue)
  const [hour, minute] = normalized.split(':')
  return Number(hour) * 60 + Number(minute)
}

const extractList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const getStorageKey = (employeeId) => `${ACTIVE_KEY_PREFIX}:${employeeId}`

const loadActive = (employeeId) => {
  try {
    const raw = localStorage.getItem(getStorageKey(employeeId))
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

const saveActive = (employeeId, payload) => {
  localStorage.setItem(getStorageKey(employeeId), JSON.stringify(payload))
}

const clearActive = (employeeId) => {
  localStorage.removeItem(getStorageKey(employeeId))
}

const formatCurrency = (num) => {
  return new Intl.NumberFormat('vi-VN').format(Number(num || 0))
}

const formatDateTime = (value) => {
  if (!value) return '--/--/---- --:--'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '--/--/---- --:--'
  const date = `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`
  const time = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  return `${date} ${time}`
}

const onMoneyInput = (event, mode) => {
  const raw = String(event?.target?.value || '').replace(/\D/g, '')
  const numeric = Number(raw || 0)
  if (mode === 'open') openingCashInput.value = numeric
  if (mode === 'close') closeCashInput.value = numeric
}

const resolveLoggedEmployee = async () => {
  const taiKhoanId = Number(localStorage.getItem('userId') || 0)
  if (taiKhoanId > 0) {
    try {
      const byTaiKhoan = await getNhanVienByTaiKhoanId(taiKhoanId)
      const payload = byTaiKhoan?.data
      const resolved = Array.isArray(payload)
        ? payload[0]
        : (Array.isArray(payload?.content) ? payload.content[0] : payload)
      if (resolved?.id) {
        currentUser.value = resolved
        return Number(resolved.id)
      }
    } catch {
      // Fallback below.
    }

    try {
      const all = await getAllNhanVien()
      const list = extractList(all?.data)
      const mapped = list.find((item) => {
        const mappedTaiKhoanId = Number(item?.idTaiKhoan || item?.taiKhoan?.id || 0)
        return mappedTaiKhoanId === taiKhoanId
      })
      if (mapped?.id) {
        currentUser.value = mapped
        return Number(mapped.id)
      }
    } catch {
      // Continue fallback below.
    }
  }

  const raw = localStorage.getItem('user') || sessionStorage.getItem('user')
  if (raw) {
    try {
      const parsed = JSON.parse(raw)
      if (parsed?.idNhanVien) {
        currentUser.value = parsed
        return Number(parsed.idNhanVien)
      }
      if (parsed?.id && parsed?.tenNhanVien) {
        currentUser.value = parsed
        return Number(parsed.id)
      }
    } catch {
      // Ignore parse errors.
    }
  }

  return null
}

const findCurrentSchedule = (rows) => {
  const nowMinutes = now.value.getHours() * 60 + now.value.getMinutes()
  return rows
    .map((item) => ({
      ...item,
      gioBatDau: normalizeTime(item?.gioBatDau),
      gioKetThuc: normalizeTime(item?.gioKetThuc)
    }))
    .find((item) => {
      const start = toMinutes(item.gioBatDau)
      const end = toMinutes(item.gioKetThuc)
      return nowMinutes >= start && nowMinutes < end
    }) || null
}

const fetchRevenue = async () => {
  if (!idNhanVien.value) return
  const dateKey = normalizeDateKey(currentSchedule.value?.ngayLam) || getTodayDateKey()

  try {
    const res = await getAllHoaDon()
    const orders = extractList(res?.data)
    let cashRevenue = 0
    let transferRevenue = 0
    let orderCount = 0

    for (const order of orders) {
      if (resolveInvoiceEmployeeId(order) !== Number(idNhanVien.value)) continue
      if (!isRevenueCountableOrder(order)) continue

      const bucket = getRevenueBucket(order)
      if (bucket === 'unknown') continue
      if (resolveRevenueDateKey(order, bucket) !== dateKey) continue

      const amount = Number(order?.thanhTien || 0)
      if (amount <= 0) continue

      orderCount += 1
      if (bucket === 'cash') cashRevenue += amount
      else transferRevenue += amount
    }

    revenue.value = { cashRevenue, transferRevenue, orderCount }
  } catch {
    revenue.value = { cashRevenue: 0, transferRevenue: 0, orderCount: 0 }
  }
}

const loadData = async () => {
  loading.value = true
  errorMessage.value = ''

  try {
    if (!idNhanVien.value) {
      idNhanVien.value = await resolveLoggedEmployee()
      if (!idNhanVien.value) {
        errorMessage.value = 'Không xác định được nhân viên đăng nhập.'
        return
      }
    }

    const [lichRes, lichSuRes] = await Promise.all([
      getAllLichLamViecFull(),
      getLichSuCaByNhanVien(idNhanVien.value)
    ])

    const lichRows = extractList(lichRes?.data).filter((item) => Number(item?.idNhanVien) === Number(idNhanVien.value))
    const todayRows = lichRows.filter((item) => normalizeDateKey(item?.ngayLam) === getTodayDateKey())

    currentSchedule.value = findCurrentSchedule(todayRows)
    if (!currentSchedule.value) {
      clearActive(idNhanVien.value)
      activeShift.value = null
      errorMessage.value = 'Bạn chưa có ca đang diễn ra để mở ca.'
      return
    }

    const nowMinutes = now.value.getHours() * 60 + now.value.getMinutes()
    const endMinutes = toMinutes(currentSchedule.value.gioKetThuc)

    const historyRows = extractList(lichSuRes?.data)
    existingHistory.value = historyRows.find((item) => {
      return (
        normalizeDateKey(item?.ngay) === normalizeDateKey(currentSchedule.value?.ngayLam)
        && Number(item?.idCaLam) === Number(currentSchedule.value?.idCaLam)
      )
    }) || null

    const hasClosedAndEnded = existingHistory.value && nowMinutes >= endMinutes
    if (hasClosedAndEnded) {
      clearActive(idNhanVien.value)
      activeShift.value = null
      errorMessage.value = 'Ca đã kết thúc và đã đóng ca. Vui lòng chờ ca tiếp theo.'
      return
    }

    const stored = loadActive(idNhanVien.value)
    if (
      stored
      && Number(stored?.idLichLamViec) === Number(currentSchedule.value?.id)
      && normalizeDateKey(stored?.ngayLam) === normalizeDateKey(currentSchedule.value?.ngayLam)
    ) {
      activeShift.value = stored
      closeCashInput.value = Number(stored?.tienDauCaNhap || 0)
      await fetchRevenue()
      return
    }

    clearActive(idNhanVien.value)
    activeShift.value = null
  } catch (error) {
    console.error('Load open-close shift failed:', error)
    errorMessage.value = 'Không thể tải dữ liệu mở/kết ca làm việc.'
  } finally {
    loading.value = false
  }
}

const openShift = async () => {
  if (!currentSchedule.value?.id || !currentSchedule.value?.idCaLam) {
    errorMessage.value = 'Không có ca hợp lệ để mở.'
    return
  }

  submitting.value = true
  try {
    const payload = {
      id: `CA-${Date.now()}`,
      idNhanVien: idNhanVien.value,
      idCaLam: currentSchedule.value.idCaLam,
      idLichLamViec: currentSchedule.value.id,
      ngayLam: normalizeDateKey(currentSchedule.value.ngayLam),
      tienDauCaNhap: Number(openingCashInput.value || 0),
      ghiChuBanDau: String(openNote.value || '').trim(),
      startedAt: new Date().toISOString()
    }

    saveActive(idNhanVien.value, payload)
    activeShift.value = payload
    closeCashInput.value = Number(payload.tienDauCaNhap || 0)
    await fetchRevenue()
    window.toast?.success?.('Mở ca thành công')
  } catch (error) {
    console.error('Open shift failed:', error)
    window.toast?.error?.('Không thể mở ca lúc này')
  } finally {
    submitting.value = false
  }
}

const closeShift = async () => {
  if (!activeShift.value || !currentSchedule.value || !idNhanVien.value) return
  if (!String(closeNote.value || '').trim()) {
    window.alert('Vui lòng nhập lý do kết ca trước khi xác nhận.')
    return
  }

  submitting.value = true
  try {
    await fetchRevenue()

    const payload = {
      idNhanVien: idNhanVien.value,
      idCaLam: currentSchedule.value.idCaLam,
      ngay: normalizeDateKey(currentSchedule.value.ngayLam),
      tienCa: Number(closeCashInput.value || 0),
      tienDauCa: Number(activeShift.value?.tienDauCaNhap || 0),
      doanhThu: Number(revenue.value?.cashRevenue || 0),
      tienChuyenKhoan: Number(revenue.value?.transferRevenue || 0),
      ghiChu: [
        activeShift.value?.ghiChuBanDau ? `Đầu ca: ${activeShift.value.ghiChuBanDau}` : '',
        `Lý do kết ca: ${String(closeNote.value).trim()}`
      ].filter(Boolean).join(' | '),
      trangThai: 'Hoàn thành'
    }

    if (existingHistory.value?.id) {
      await updateLichSuCa(existingHistory.value.id, payload)
    } else {
      await createLichSuCa(payload)
    }

    clearActive(idNhanVien.value)
    activeShift.value = null
    openNote.value = ''
    closeNote.value = ''
    openingCashInput.value = 0
    closeCashInput.value = 0
    revenue.value = { cashRevenue: 0, transferRevenue: 0, orderCount: 0 }

    window.toast?.success?.('Kết ca thành công')
    await loadData()
  } catch (error) {
    console.error('Close shift failed:', error)
    window.toast?.error?.('Không thể kết ca lúc này')
  } finally {
    submitting.value = false
  }
}

const refreshRevenue = async () => {
  await fetchRevenue()
  window.toast?.success?.('Đã cập nhật doanh thu trong ca')
}

onMounted(async () => {
  await loadData()
  clockTimer = setInterval(() => {
    now.value = new Date()
  }, 1000)

  revenueTimer = setInterval(() => {
    if (activeShift.value) fetchRevenue()
  }, 15000)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (revenueTimer) clearInterval(revenueTimer)
})
</script>

<style scoped>
.mk-wrap {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  gap: 14px;
}

.mk-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(135deg, #ef4444, #7f1d1d);
  border: 1px solid #b91c1c;
  border-radius: 14px;
  padding: 14px 16px;
}

.mk-head h1 {
  margin: 0;
  font-size: 22px;
  color: #ffffff;
}

.mk-head p {
  margin: 6px 0 0;
  color: #fee2e2;
}

.mk-head-meta {
  text-align: right;
  font-size: 13px;
  color: #ffffff;
  display: grid;
  align-content: center;
  gap: 6px;
}

.mk-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 14px 16px;
}

.mk-warning {
  border-color: #fca5a5;
  background: #fff1f2;
}

.mk-open-card {
  border-color: #fca5a5;
  background: #fff5f5;
}

.mk-open-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.muted {
  color: #64748b;
}

.mk-form {
  display: grid;
  gap: 8px;
}

.mk-form label,
.mk-close-grid label {
  font-size: 13px;
  color: #334155;
  font-weight: 600;
}

.mk-form input,
.mk-form textarea,
.mk-close-grid input,
.mk-close-grid textarea {
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
}

.mk-content {
  display: grid;
  gap: 14px;
}

.mk-topline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.mk-badge {
  background: #fee2e2;
  color: #991b1b;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
}

.mk-summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
}

.mk-cols4 {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.mk-mini {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px;
}

.mk-mini h4 {
  margin: 0 0 8px;
  font-size: 14px;
}

.mk-mini p {
  margin: 4px 0;
  font-size: 13px;
  color: #334155;
}

.mk-total {
  margin-top: 8px !important;
  font-weight: 700;
}

.mk-close-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}

.mk-readonly {
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  padding: 10px 12px;
  color: #0f172a;
  font-weight: 700;
  min-height: 40px;
  display: flex;
  align-items: center;
}

.mk-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.btn {
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 14px;
  font-weight: 700;
  cursor: pointer;
}

.btn.primary {
  background: linear-gradient(135deg, #ef4444, #7f1d1d);
  color: #fff;
  border: 0;
}

.btn.ghost {
  background: #fff;
  color: #334155;
}

@media (max-width: 1100px) {
  .mk-cols4 {
    grid-template-columns: 1fr 1fr;
  }

  .mk-close-grid {
    grid-template-columns: 1fr;
  }

  .mk-open-grid {
    grid-template-columns: 1fr;
  }
}
</style>
