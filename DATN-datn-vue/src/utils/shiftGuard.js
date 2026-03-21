import { getAllLichLamViecFull } from '../services/lichLamViecService'
import { getLichSuCaByNhanVien } from '../services/lichSuCaService'

const extractList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
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

  const match = raw.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/)
  if (!match) return ''
  const day = String(match[1]).padStart(2, '0')
  const month = String(match[2]).padStart(2, '0')
  return `${match[3]}-${month}-${day}`
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

const getDateKey = (date) => {
  const d = date instanceof Date ? date : new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getMinutesOfDate = (date) => {
  const d = date instanceof Date ? date : new Date(date)
  return d.getHours() * 60 + d.getMinutes()
}

const isCompletedHistoryRow = (row) => {
  const status = String(row?.trangThai || '').trim().toLowerCase()
  return status.includes('hoàn thành') || status.includes('hoan thanh')
}

export const validateEmployeeActiveShift = async (employeeId, atDate = new Date()) => {
  const id = Number(employeeId)
  if (!Number.isFinite(id) || id <= 0) {
    return { allowed: false, reason: 'Nhân viên không hợp lệ.' }
  }

  try {
    const [scheduleRes, historyRes] = await Promise.all([
      getAllLichLamViecFull(),
      getLichSuCaByNhanVien(id)
    ])

    const allSchedules = extractList(scheduleRes?.data)
    const histories = extractList(historyRes?.data)

    const todayKey = getDateKey(atDate)
    const nowMinutes = getMinutesOfDate(atDate)

    const inProgressSchedule = allSchedules
      .filter((item) => Number(item?.idNhanVien) === id)
      .filter((item) => normalizeDateKey(item?.ngayLam) === todayKey)
      .map((item) => ({
        ...item,
        gioBatDau: normalizeTime(item?.gioBatDau),
        gioKetThuc: normalizeTime(item?.gioKetThuc)
      }))
      .find((item) => {
        const start = toMinutes(item.gioBatDau)
        const end = toMinutes(item.gioKetThuc)
        return nowMinutes >= start && nowMinutes < end
      })

    if (!inProgressSchedule) {
      return {
        allowed: false,
        reason: 'Nhân viên chưa có ca đang trực tại thời điểm tạo hóa đơn.'
      }
    }

    const wasClosed = histories.some((row) => {
      return (
        normalizeDateKey(row?.ngay) === normalizeDateKey(inProgressSchedule?.ngayLam)
        && Number(row?.idCaLam) === Number(inProgressSchedule?.idCaLam)
        && isCompletedHistoryRow(row)
      )
    })

    if (wasClosed) {
      return {
        allowed: false,
        reason: 'Ca hiện tại đã được đóng. Không thể ghi nhận thêm hóa đơn cho ca này.'
      }
    }

    return {
      allowed: true,
      schedule: inProgressSchedule
    }
  } catch {
    return {
      allowed: false,
      reason: 'Không kiểm tra được thông tin ca trực. Vui lòng thử lại.'
    }
  }
}
