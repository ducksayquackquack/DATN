<template>
  <div class="su-hoat-dong-page">
    <div class="page-header">
      <h1 class="page-title">Lịch sử giao ca & kế toán</h1>
      <p class="page-subtitle">Theo dõi lịch sử giao ca và kết toán của nhân viên</p>
    </div>

    <div class="filter-section">
      <div class="filter-grid">
        <div class="filter-item filter-search">
          <label>Tìm kiếm</label>
          <input
            v-model="filters.search"
            class="form-input"
            type="text"
            placeholder="Tìm theo nhân viên / tên"
          />
        </div>

        <div class="filter-item">
          <label>Từ ngày</label>
          <input v-model="filters.fromDate" class="form-input" type="date" />
        </div>

        <div class="filter-item">
          <label>Đến ngày</label>
          <input v-model="filters.toDate" class="form-input" type="date" />
        </div>
      </div>

      <button class="btn-refresh" @click="loadActivities">
        <span class="material-icons-outlined">refresh</span>
      </button>
    </div>

    <div class="table-section">
      <div class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th class="text-center">#</th>
              <th>Nhân viên</th>
              <th>Ngày</th>
              <th>Ca</th>
              <th>Mở</th>
              <th>Đóng</th>
              <th class="text-right">Đầu ca</th>
              <th class="text-right">DT tiền mặt</th>
              <th class="text-right">DT chuyển khoản</th>
              <th class="text-right">Chênh lệch</th>
              <th>Trạng thái</th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="filteredActivities.length === 0">
              <td colspan="11" class="empty-state">
                <span class="material-icons-outlined">history</span>
                <p>Không có dữ liệu</p>
              </td>
            </tr>

        <tr v-for="(activity, index) in filteredActivities" :key="activity.id || index">
              <td class="text-center">{{ index + 1 }}</td>
              <td>{{ activity.employee }}</td>
              <td>{{ activity.displayDate }}</td>
              <td>{{ activity.shift }}</td>
              <td>{{ activity.openTime || '--:--' }}</td>
              <td>{{ activity.closeTime || '--:--' }}</td>
              <td class="text-right">{{ activity.openingCash }}</td>
              <td class="text-right">{{ activity.cashRevenue }}</td>
              <td class="text-right">{{ activity.transferRevenue }}</td>
              <td class="text-right" :style="activity.differenceValue !== null && activity.differenceValue < 0 ? 'color:#dc2626;font-weight:600' : activity.differenceValue > 0 ? 'color:#16a34a;font-weight:600' : ''">
                {{ activity.difference }}
              </td>
              <td>
                <span class="status-badge" :class="activity.status">
                  {{ activity.status === 'completed' ? 'Đã kết toán' : 'Chờ kết toán' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useToast } from '../../../composables/useToast'
import { createLichSuCa, getAllLichSuCa, updateLichSuCa } from '../../../services/lichSuCaService'
import { getAllHoaDon } from '../../../services/hoaDonService'
import { getAllNhanVien } from '../../../services/nhanVienService'
import { getAllCaLam } from '../../../services/caLamService'
import { getAllLichLamViec, getAllLichLamViecFull } from '../../../services/lichLamViecService'
import taiKhoanService from '../../../services/taiKhoanService'
import {
  getRevenueBucket,
  isRevenueCountableOrder,
  resolveInvoiceEmployeeId,
  resolveRevenueDateKey
} from '../../../utils/orderRevenue'

const toast = useToast()

const today = new Date()
const firstDay = new Date(today.getFullYear(), today.getMonth(), 1)

const toDateInput = (dateValue) => {
  const pad = (n) => String(n).padStart(2, '0')
  return `${dateValue.getFullYear()}-${pad(dateValue.getMonth() + 1)}-${pad(dateValue.getDate())}`
}

const filters = reactive({
  search: '',
  fromDate: toDateInput(firstDay),
  toDate: toDateInput(today)
})

const activities = ref([])

const MAX_SEEDED_ROWS = 20

const formatCurrency = (value) => {
  const numeric = Number(value || 0)
  return `${new Intl.NumberFormat('vi-VN').format(numeric)} ₫`
}
// Extract “Tiền đầu ca” from ghiChu strings saved by GiaoCa.vue
// Format: "Tiền đầu ca: 1.500.000đ"
const parseTienDauCaFromGhiChu = (ghiChu) => {
  if (!ghiChu) return 0
  const match = String(ghiChu).match(/Tiền đầu ca:\s*([\d.,]+)đ/)
  if (!match) return 0
  const cleaned = match[1].replace(/\./g, '').replace(',', '.')
  const val = parseFloat(cleaned)
  return Number.isFinite(val) ? val : 0
}
const parseNumber = (value) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : 0
}

const readTransferAmount = (item) => {
  const candidates = [
    item?.tienChuyenKhoan,
    item?.tienCK,
    item?.tienChuyenKhoanThucTe,
    item?.soTienChuyenKhoan,
    item?.chuyenKhoan
  ]
  const found = candidates.find((value) => value !== undefined && value !== null && String(value) !== '')
  return parseNumber(found)
}

const extractList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const resolveRevenueAmount = (byEmployeeDate, byEmployee, employeeId, dateKey) => {
  if (!employeeId) return 0
  if (dateKey) return parseNumber(byEmployeeDate.get(`${employeeId}-${dateKey}`))
  return parseNumber(byEmployee.get(employeeId))
}

const normalizeRole = (role) => String(role || '').trim().toUpperCase().replace(/^ROLE_/, '')

const isEmployeeRole = (role) => {
  const normalized = normalizeRole(role)
  return normalized === 'EMPLOYEE' || normalized === 'NHAN_VIEN' || normalized === 'NHANVIEN'
}

const normalizeDateKey = (dateValue) => {
  if (!dateValue) return ''
  const raw = String(dateValue).trim()
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw
  if (/^\d{4}-\d{2}-\d{2}T/.test(raw)) return raw.slice(0, 10)
  const match = raw.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/)
  if (match) {
    const day = match[1].padStart(2, '0')
    const month = match[2].padStart(2, '0')
    return `${match[3]}-${month}-${day}`
  }
  return ''
}

const normalizeTime = (timeValue) => {
  if (!timeValue) return '--:--'
  const raw = String(timeValue).trim()
  if (/^\d{2}:\d{2}$/.test(raw)) return raw
  if (/^\d{2}:\d{2}:\d{2}$/.test(raw)) return raw.slice(0, 5)
  return raw
}

const mapStatus = (statusValue) => {
  const statusText = String(statusValue || '').toLowerCase()
  if (statusText.includes('hoàn thành') || statusText.includes('hoan thanh')) return 'completed'
  return 'pending'
}

const formatDisplayDate = (dateKey) => {
  const raw = normalizeDateKey(dateKey)
  if (!raw) return '--/--/----'
  const [year, month, day] = raw.split('-')
  if (!year || !month || !day) return '--/--/----'
  return `${day}/${month}/${year}`
}

const seedHistoryFromSchedules = async (
  scheduleRows,
  posCashRevenueByEmployeeDate = new Map(),
  posCashRevenueByEmployee = new Map(),
  codRevenueByEmployeeDate = new Map(),
  codRevenueByEmployee = new Map(),
  transferRevenueByEmployeeDate = new Map(),
  transferRevenueByEmployee = new Map()
) => {
  const uniqueRows = []
  const seen = new Set()

  scheduleRows.forEach((item) => {
    if (uniqueRows.length >= MAX_SEEDED_ROWS) return
    const idNhanVien = Number(item?.idNhanVien)
    const idCaLam = Number(item?.idCaLam)
    const ngay = normalizeDateKey(item?.ngayLam)
    if (!idNhanVien || !idCaLam || !ngay) return

    const key = `${idNhanVien}-${idCaLam}-${ngay}`
    if (seen.has(key)) return
    seen.add(key)

    const posRevenueValue = resolveRevenueAmount(posCashRevenueByEmployeeDate, posCashRevenueByEmployee, idNhanVien, ngay)
    const codRevenueValue = resolveRevenueAmount(codRevenueByEmployeeDate, codRevenueByEmployee, idNhanVien, ngay)
    const cashRevenueValue = posRevenueValue + codRevenueValue
    const transferRevenueValue = resolveRevenueAmount(transferRevenueByEmployeeDate, transferRevenueByEmployee, idNhanVien, ngay)

    uniqueRows.push({
      idNhanVien,
      idCaLam,
      ngay,
      tienDauCa: 0,
      tienCa: cashRevenueValue,
      tienChuyenKhoan: transferRevenueValue,
      doanhThu: cashRevenueValue,
      ghiChu: 'Khởi tạo từ lịch làm việc',
      trangThai: 'Chờ kết toán'
    })
  })

  if (!uniqueRows.length) return 0

  const results = await Promise.allSettled(uniqueRows.map((payload) => createLichSuCa(payload)))
  return results.filter((result) => result.status === 'fulfilled').length
}

const filteredActivities = computed(() => {
  return activities.value.filter((item) => {
    const keyword = filters.search.trim().toLowerCase()
    const keywordMatch = !keyword
      || item.employee.toLowerCase().includes(keyword)
      || item.shift.toLowerCase().includes(keyword)

    const fromMatch = !filters.fromDate || !item.dateKey || item.dateKey >= filters.fromDate
    const toMatch = !filters.toDate || !item.dateKey || item.dateKey <= filters.toDate

    return keywordMatch && fromMatch && toMatch
  })
})

const countRowsInCurrentRange = (rows) => {
  return rows.filter((item) => {
    const fromMatch = !filters.fromDate || !item.dateKey || item.dateKey >= filters.fromDate
    const toMatch = !filters.toDate || !item.dateKey || item.dateKey <= filters.toDate
    return fromMatch && toMatch
  }).length
}

const adjustFilterRangeToData = (rows) => {
  const dateKeys = rows
    .map((item) => normalizeDateKey(item?.dateKey))
    .filter(Boolean)

  if (!dateKeys.length) return

  const minDate = [...dateKeys].sort()[0]
  const maxDate = [...dateKeys].sort().at(-1)

  if (!minDate || !maxDate) return

  filters.fromDate = minDate
  filters.toDate = maxDate
}

const loadActivities = async () => {
  try {
    let hoaDonLoadFailed = false

    const [historyRes, hoaDonRes, employeeRes, shiftRes, scheduleRes, taiKhoanRes] = await Promise.all([
      getAllLichSuCa(),
      getAllHoaDon().catch(() => {
        hoaDonLoadFailed = true
        return { data: [] }
      }),
      getAllNhanVien(),
      getAllCaLam(),
      getAllLichLamViecFull().catch(() => ({ data: [] })),
      taiKhoanService.getAll().catch(() => ({ data: [] }))
    ])

    if (hoaDonLoadFailed) {
      toast.error('Không tải được dữ liệu hóa đơn nên doanh thu có thể hiển thị thiếu')
    }

    let historyData = extractList(historyRes?.data)
    const hoaDonData = extractList(hoaDonRes?.data)
    const employeeDataRaw = extractList(employeeRes?.data)
    const shiftData = extractList(shiftRes?.data)
    let scheduleData = extractList(scheduleRes?.data)
    if (scheduleData.length === 0) {
      const scheduleBaseRes = await getAllLichLamViec().catch(() => ({ data: [] }))
      scheduleData = extractList(scheduleBaseRes?.data)
    }

    const cashRevenueByEmployeeDate = new Map()
    const cashRevenueByEmployee = new Map()
    const codRevenueByEmployeeDate = new Map()
    const codRevenueByEmployee = new Map()
    const transferRevenueByEmployeeDate = new Map()
    const transferRevenueByEmployee = new Map()
    hoaDonData.forEach((invoice) => {
      const employeeId = resolveInvoiceEmployeeId(invoice)
      const revenueBucket = getRevenueBucket(invoice)
      const dateKey = resolveRevenueDateKey(invoice, revenueBucket)
      const amount = Math.max(0, parseNumber(invoice?.thanhTien) - parseNumber(invoice?.phiShip))
      if (!employeeId || !dateKey || !isRevenueCountableOrder(invoice)) return

      const key = `${employeeId}-${dateKey}`

      if (revenueBucket === 'cash') {
        cashRevenueByEmployeeDate.set(key, parseNumber(cashRevenueByEmployeeDate.get(key)) + amount)
        cashRevenueByEmployee.set(employeeId, parseNumber(cashRevenueByEmployee.get(employeeId)) + amount)
      } else if (revenueBucket === 'cod') {
        codRevenueByEmployeeDate.set(key, parseNumber(codRevenueByEmployeeDate.get(key)) + amount)
        codRevenueByEmployee.set(employeeId, parseNumber(codRevenueByEmployee.get(employeeId)) + amount)
      } else if (revenueBucket === 'transfer') {
        transferRevenueByEmployeeDate.set(key, parseNumber(transferRevenueByEmployeeDate.get(key)) + amount)
        transferRevenueByEmployee.set(employeeId, parseNumber(transferRevenueByEmployee.get(employeeId)) + amount)
      }
    })

    const backfillHistoryFromHoaDon = async (rows) => {
      const updates = rows
        .filter((item) => item?.id)
        .map((item) => {
          const employeeId = Number(item?.idNhanVien)
          const dateKey = normalizeDateKey(item?.ngay)
          const currentCash = parseNumber(item?.tienCa)
          const currentRevenue = parseNumber(item?.doanhThu)
          const currentTransfer = readTransferAmount(item)

          const posRevenueValue = resolveRevenueAmount(cashRevenueByEmployeeDate, cashRevenueByEmployee, employeeId, dateKey)
          const codRevenueValue = resolveRevenueAmount(codRevenueByEmployeeDate, codRevenueByEmployee, employeeId, dateKey)
          const computedCashRevenue = posRevenueValue + codRevenueValue
          const computedTransferRevenue = resolveRevenueAmount(transferRevenueByEmployeeDate, transferRevenueByEmployee, employeeId, dateKey)

          const shouldBackfillCashRevenue = currentRevenue <= 0 && computedCashRevenue > 0
          const shouldBackfillTransferRevenue = currentTransfer <= 0 && computedTransferRevenue > 0
          if (!shouldBackfillCashRevenue && !shouldBackfillTransferRevenue) return null

          const nextRevenue = shouldBackfillCashRevenue ? computedCashRevenue : currentRevenue
          const nextTransfer = shouldBackfillTransferRevenue ? computedTransferRevenue : currentTransfer

          const openingCash = parseNumber(item?.tienDauCa) || parseTienDauCaFromGhiChu(item?.ghiChu)
          const tienCa = currentCash > 0 ? currentCash : (openingCash + nextRevenue)

          return updateLichSuCa(item.id, {
            idNhanVien: employeeId,
            idCaLam: Number(item?.idCaLam),
            ngay: dateKey,
            tienCa,
            tienChuyenKhoan: nextTransfer,
            doanhThu: nextRevenue,
            ghiChu: item?.ghiChu || 'Đồng bộ từ hóa đơn',
            trangThai: item?.trangThai || 'Chờ kết toán'
          })
        })
        .filter(Boolean)

      if (!updates.length) return 0
      const results = await Promise.allSettled(updates)
      return results.filter((result) => result.status === 'fulfilled').length
    }

    if (historyData.length === 0 && scheduleData.length > 0) {
      const createdCount = await seedHistoryFromSchedules(
        scheduleData,
        cashRevenueByEmployeeDate,
        cashRevenueByEmployee,
        codRevenueByEmployeeDate,
        codRevenueByEmployee,
        transferRevenueByEmployeeDate,
        transferRevenueByEmployee
      )
      if (createdCount > 0) {
        const reloadedHistory = await getAllLichSuCa().catch(() => ({ data: [] }))
        historyData = extractList(reloadedHistory?.data)
        toast.success(`Đã khởi tạo ${createdCount} dòng lịch sử giao ca từ lịch làm việc`)
      }
    }

    if (historyData.length > 0) {
      const backfilledCount = await backfillHistoryFromHoaDon(historyData)
      if (backfilledCount > 0) {
        const reloadedHistory = await getAllLichSuCa().catch(() => ({ data: [] }))
        historyData = extractList(reloadedHistory?.data)
      }

    }

    const taiKhoanData = extractList(taiKhoanRes?.data)

    const taiKhoanRoleById = new Map(taiKhoanData.map((item) => [item?.id, item?.vaiTro]))
    const employeeData = employeeDataRaw.filter((item) => {
      const inlineRole = item?.taiKhoan?.vaiTro
      const accountId = item?.taiKhoan?.id
      const mappedRole = taiKhoanRoleById.get(accountId)
      const role = inlineRole || mappedRole
      return isEmployeeRole(role)
    })

    const employeesForView = employeeData.length > 0 ? employeeData : employeeDataRaw
    const employeeIdSet = new Set(employeesForView.map((item) => item?.id))

    const employeeMap = new Map(employeesForView.map((item) => [item.id, item.tenNhanVien]))
    const shiftMap = new Map(shiftData.map((item) => [item.id, item]))

    const scheduleByEmployeeDate = new Map(
      scheduleData.map((item) => [`${item?.idNhanVien}-${normalizeDateKey(item?.ngayLam)}`, item])
    )

    const historyRows = historyData
      .filter((item) => employeeIdSet.size === 0 || employeeIdSet.has(item?.idNhanVien))
      .map((item) => {
      const employeeId = item?.idNhanVien
      const dateKey = normalizeDateKey(item?.ngay)
      const schedule = scheduleByEmployeeDate.get(`${employeeId}-${dateKey}`)
      const shiftInfo = shiftMap.get(item?.idCaLam) || shiftMap.get(schedule?.idCaLam)

      const closingCash = parseNumber(item?.tienCa)
      const openingCash = parseNumber(item?.tienDauCa) || parseTienDauCaFromGhiChu(item?.ghiChu)
      const posCashRevenue = resolveRevenueAmount(cashRevenueByEmployeeDate, cashRevenueByEmployee, employeeId, dateKey)
      const codCollectedRevenue = resolveRevenueAmount(codRevenueByEmployeeDate, codRevenueByEmployee, employeeId, dateKey)
      const inferredCashRevenue = posCashRevenue + codCollectedRevenue
      const cashRevenue = parseNumber(item?.doanhThu) || inferredCashRevenue
      const transferRevenue = readTransferAmount(item)
        || resolveRevenueAmount(transferRevenueByEmployeeDate, transferRevenueByEmployee, employeeId, dateKey)
      const totalRevenue = cashRevenue + transferRevenue
      // correct formula: actual closing cash − (opening + cash_sales)
      const difference = openingCash > 0
        ? closingCash - (openingCash + cashRevenue)
        : null  // seeded legacy rows have no opening — can't compute

      return {
        id: item?.id,
        employeeId,
        dateKey,
        displayDate: formatDisplayDate(dateKey),
        employee: employeeMap.get(employeeId) || `NV #${employeeId ?? '--'}`,
        shift: shiftInfo?.tenCa || schedule?.tenCa || `Ca #${item?.idCaLam ?? '--'}`,
        openTime: normalizeTime(shiftInfo?.gioBatDau || schedule?.gioBatDau),
        closeTime: normalizeTime(shiftInfo?.gioKetThuc || schedule?.gioKetThuc),
        openingCashValue: openingCash,
        closingCashValue: closingCash,
        cashRevenueValue: cashRevenue,
        transferRevenueValue: transferRevenue,
        openingCash: formatCurrency(openingCash),
        cashRevenue: formatCurrency(cashRevenue),
        transferRevenue: formatCurrency(transferRevenue),
        totalRevenue: formatCurrency(totalRevenue),
        closingCash: formatCurrency(closingCash),
        difference: difference !== null ? formatCurrency(difference) : '--',
        differenceValue: difference,
        status: mapStatus(item?.trangThai)
      }
    })

    const nonEmptyHistoryRows = historyRows.filter((row) => {
      return row.openingCashValue > 0
        || row.closingCashValue > 0
        || row.cashRevenueValue > 0
        || row.transferRevenueValue > 0
    })

    const historyKeys = new Set(nonEmptyHistoryRows.map((row) => `${row.employeeId}-${row.dateKey}`))
    const invoiceKeys = new Set([
      ...transferRevenueByEmployeeDate.keys()
    ])

    const invoiceDerivedRows = [...invoiceKeys]
      .map((key) => {
        if (historyKeys.has(key)) return null

        const splitAt = key.indexOf('-')
        if (splitAt <= 0) return null

        const employeeId = Number(key.slice(0, splitAt))
        const dateKey = key.slice(splitAt + 1)
        if (!employeeId || !dateKey) return null
        if (employeeIdSet.size > 0 && !employeeIdSet.has(employeeId)) return null

        if (filters.fromDate && dateKey < filters.fromDate) return null
        if (filters.toDate && dateKey > filters.toDate) return null

        const posCashRevenue = resolveRevenueAmount(cashRevenueByEmployeeDate, cashRevenueByEmployee, employeeId, dateKey)
        const codCollectedRevenue = resolveRevenueAmount(codRevenueByEmployeeDate, codRevenueByEmployee, employeeId, dateKey)
        const cashRevenue = posCashRevenue + codCollectedRevenue
        const transferRevenue = resolveRevenueAmount(transferRevenueByEmployeeDate, transferRevenueByEmployee, employeeId, dateKey)
        if (transferRevenue <= 0) return null

        const schedule = scheduleByEmployeeDate.get(`${employeeId}-${dateKey}`)
        const shiftInfo = shiftMap.get(schedule?.idCaLam)

        return {
          id: `invoice-${employeeId}-${dateKey}`,
          employeeId,
          dateKey,
          displayDate: formatDisplayDate(dateKey),
          employee: employeeMap.get(employeeId) || `NV #${employeeId ?? '--'}`,
          shift: shiftInfo?.tenCa || schedule?.tenCa || 'Không có ca (theo hóa đơn)',
          openTime: normalizeTime(shiftInfo?.gioBatDau || schedule?.gioBatDau),
          closeTime: normalizeTime(shiftInfo?.gioKetThuc || schedule?.gioKetThuc),
          openingCashValue: 0,
          closingCashValue: 0,
          cashRevenueValue: cashRevenue,
          transferRevenueValue: transferRevenue,
          openingCash: formatCurrency(0),
          cashRevenue: formatCurrency(cashRevenue),
          transferRevenue: formatCurrency(transferRevenue),
          totalRevenue: formatCurrency(cashRevenue + transferRevenue),
          closingCash: formatCurrency(0),
          difference: '--',
          differenceValue: null,
          status: 'pending'
        }
      })
      .filter(Boolean)

    const rows = nonEmptyHistoryRows.length > 0
      ? [...nonEmptyHistoryRows, ...invoiceDerivedRows]
      : invoiceDerivedRows

    const sortedRows = rows.sort((a, b) => {
      const dateCompare = String(b?.dateKey || '').localeCompare(String(a?.dateKey || ''))
      if (dateCompare !== 0) return dateCompare
      return String(a?.employee || '').localeCompare(String(b?.employee || ''), 'vi')
    })

    activities.value = sortedRows

    // Prevent false-empty UI when DB rows exist but initial date filter does not cover their dates.
    if (sortedRows.length > 0 && countRowsInCurrentRange(sortedRows) === 0) {
      adjustFilterRangeToData(sortedRows)
    }
  } catch (error) {
    console.error('Load activity history failed:', error)
    activities.value = []
    toast.error('Không tải được lịch sử hoạt động')
  }
}

onMounted(loadActivities)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap');
@import url('https://fonts.googleapis.com/icon?family=Material+Icons+Outlined');

.su-hoat-dong-page {
  font-family: 'Be Vietnam Pro', 'Segoe UI', Tahoma, sans-serif;
  padding: 28px 30px;
  background: #f6f8fb;
  min-height: calc(100vh - 76px);
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

.filter-grid {
  flex: 1;
  display: grid;
  gap: 12px;
  grid-template-columns: 1.4fr 1fr 1fr;
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

.btn-refresh {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: #f8fafc;
  color: #475569;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.btn-refresh:hover {
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

.text-right {
  text-align: right !important;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 96px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-badge.completed {
  background: #dcfce7;
  color: #15803d;
}

.status-badge.pending {
  background: #fef3c7;
  color: #92400e;
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

@media (max-width: 1024px) {
  .page-title {
    font-size: 20px;
  }

  .filter-grid {
    grid-template-columns: repeat(2, minmax(170px, 1fr));
  }
}

@media (max-width: 768px) {
  .su-hoat-dong-page {
    padding: 18px 14px;
  }

  .page-title {
    font-size: 18px;
  }

  .filter-section {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .btn-refresh {
    width: 100%;
  }
}
</style>
