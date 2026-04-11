<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from "vue"
import { useRouter, useRoute } from "vue-router"
import { getAllHoaDon, updateHoaDon, updateHoaDonBySystemEvent, cancelHoaDon } from "../../../services/hoaDonService"
import { getAllNhanVien } from "../../../services/nhanVienService"
import { getAllLichLamViecFull } from "../../../services/lichLamViecService"
import { Eye, Plus, Search, ChevronDown } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel, normalizeOrderStatusCode } from "../../../utils/adminStatus"
import { hasPaymentFlowTag, PAYMENT_FLOW_TAGS } from "../../../utils/paymentWorkflow"
import { buildOrderLookupTrackingUrl } from "../../../utils/publicTrackingUrl"
import { useToast } from "../../../composables/useToast"
import { useConfirm } from "../../../composables/useConfirm"

const router = useRouter()
const route = useRoute()
const { showToast } = useToast()
const { askConfirm } = useConfirm()
const panelBasePath = computed(() => (route.path.startsWith('/employee/') ? '/employee' : '/admin'))

const hoaDons = ref([])
const loading = ref(false)
const apiWarning = ref("")
const searchText = ref("")
const filterStatus = ref("Tất cả trạng thái")
const filterType = ref("Tất cả loại")
const currentPage = ref(1)
const pageSize = ref(10)
const cancelReasonById = ref({})
const rowSavingById = ref({})
const transferTargetById = ref({})
const activeShiftEmployees = ref([])
const openMoreMenuId = ref(null)
const NOTIFICATION_STORAGE_KEY = "notifications:seen"

const readPageFromQuery = (value) => {
  const page = Number(value)
  return Number.isInteger(page) && page > 0 ? page : 1
}

const loadData = async () => {
  loading.value = true
  apiWarning.value = ""
  try {
    const [invoiceRes, nhanVienRes, lichRes] = await Promise.all([
      getAllHoaDon(),
      getAllNhanVien(),
      getAllLichLamViecFull()
    ])

    const res = invoiceRes
    if (typeof res.data === "string" && /<html|<!doctype/i.test(res.data)) {
      throw new Error("API hoá đơn đang trả về trang đăng nhập thay vì JSON. Hãy restart DATN-API sau khi áp dụng SecurityConfig.")
    }

    hoaDons.value = Array.isArray(res.data) ? res.data : (res.data?.content || [])
    activeShiftEmployees.value = buildActiveShiftEmployees(
      extractList(nhanVienRes?.data),
      extractList(lichRes?.data)
    )
  } catch (error) {
    console.error("Error loading invoices:", error)
    hoaDons.value = []
    activeShiftEmployees.value = []
    apiWarning.value = error.message || "Không thể tải danh sách hoá đơn"
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  currentPage.value = readPageFromQuery(route.query.page)
  loadData()
  if (route.query.refresh) {
    router.replace({
      path: `${panelBasePath.value}/hoa-don/list`,
      query: currentPage.value > 1 ? { page: String(currentPage.value) } : {}
    })
  }
  window.addEventListener('focus', handleWindowFocus)
  document.addEventListener('click', handleDocumentClick)
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', handleWindowFocus)
  document.removeEventListener('click', handleDocumentClick)
})

watch(
  () => [route.query.refresh, route.query.page],
  ([, page]) => {
    currentPage.value = readPageFromQuery(page)
    loadData()
  }
)

const formatCurrency = (value) => {
  if (!value) return "0₫"
  return new Intl.NumberFormat("vi-VN").format(value) + "₫"
}

const formatDate = (date) => {
  if (!date) return "-"
  const d = new Date(date)
  return `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getFullYear()}`
}

const statusOptions = computed(() => {
  const names = new Set(["Tất cả trạng thái"])
  for (const item of hoaDons.value || []) {
    const normalizedCode = normalizeOrderStatusCode(item?.orderStatusCode, item?.orderStatusName, item?.statusNote)
    const normalizedLabel = normalizeAdminStatusLabel(item?.orderStatusName || normalizedCode)
    if (normalizedLabel) names.add(normalizedLabel)
  }
  return Array.from(names)
})

const pendingPaymentConfirmations = computed(() => {
  return (hoaDons.value || []).filter((item) => {
    const note = item?.statusNote || ""
    const hasCustomerConfirm = hasPaymentFlowTag(note, PAYMENT_FLOW_TAGS.VN_PAY_CUSTOMER_CONFIRMED)
    const hasEmployeeConfirm = hasPaymentFlowTag(note, PAYMENT_FLOW_TAGS.VN_PAY_EMPLOYEE_CONFIRMED)
    return hasCustomerConfirm && !hasEmployeeConfirm
  })
})

const readSeenNotifications = () => {
  try {
    return JSON.parse(localStorage.getItem(NOTIFICATION_STORAGE_KEY) || "{}")
  } catch {
    return {}
  }
}

const notificationScope = computed(() => (panelBasePath.value === "/admin" ? "admin" : "employee"))

const isNotificationSeen = (id) => {
  if (!id) return false
  const seenMap = readSeenNotifications()
  return Boolean(seenMap?.[notificationScope.value]?.[id])
}

const getOrderNotificationIds = (hoaDon) => {
  if (!hoaDon?.id) return []

  const ids = []
  const note = String(hoaDon?.statusNote || "")
  const hasCustomerConfirm = hasPaymentFlowTag(note, PAYMENT_FLOW_TAGS.VN_PAY_CUSTOMER_CONFIRMED)
  const hasEmployeeConfirm = hasPaymentFlowTag(note, PAYMENT_FLOW_TAGS.VN_PAY_EMPLOYEE_CONFIRMED)

  if (hasCustomerConfirm && !hasEmployeeConfirm) {
    ids.push(`invoice-${hoaDon.id}`)
  }

  const code = normalizeOrderStatusCode(hoaDon?.orderStatusCode, hoaDon?.orderStatusName, hoaDon?.statusNote)
  if (["CHO_XAC_NHAN", "CHO_LAY_HANG", "GIAO_THAT_BAI"].includes(code)) {
    ids.push(`invoice-queue-${hoaDon.id}-${code}`)
  }

  return ids
}

const shouldHighlightRow = (hoaDon) => {
  return getOrderNotificationIds(hoaDon).some((id) => !isNotificationSeen(id))
}

const filteredData = computed(() => {
  let data = Array.isArray(hoaDons.value) ? [...hoaDons.value] : []

  if (searchText.value.trim()) {
    const keyword = searchText.value.toLowerCase()
    data = data.filter(d =>
      (d.maHoaDon || "").toLowerCase().includes(keyword) ||
      (d.tenNhanVien || "").toLowerCase().includes(keyword) ||
      (d.tenKhachHang || "").toLowerCase().includes(keyword) ||
      (d.soDienThoaiNhanHang || "").includes(keyword)
    )
  }

  if (filterStatus.value !== "Tất cả trạng thái") {
    data = data.filter((d) => {
      const normalizedCode = normalizeOrderStatusCode(d?.orderStatusCode, d?.orderStatusName, d?.statusNote)
      return normalizeAdminStatusLabel(d?.orderStatusName || normalizedCode) === filterStatus.value
    })
  }

  if (filterType.value !== "Tất cả loại") {
    data = data.filter((d) => getOrderTypeLabel(d) === filterType.value)
  }

  data.sort((a, b) => {
    const dateA = a?.ngayTao ? new Date(a.ngayTao).getTime() : (a?.id || 0)
    const dateB = b?.ngayTao ? new Date(b.ngayTao).getTime() : (b?.id || 0)
    return dateB - dateA
  })

  return data
})

// Format maHoaDon: HD20260408XXXXXXXXX → HD20260408001
const invoiceCodeMap = computed(() => {
  const map = new Map()
  const dateGroups = new Map()
  hoaDons.value.forEach((hd) => {
    const raw = String(hd.maHoaDon || '')
    const m = raw.match(/^HD(\d{8})/)
    if (m) {
      const dateKey = m[1]
      if (!dateGroups.has(dateKey)) dateGroups.set(dateKey, [])
      dateGroups.get(dateKey).push(raw)
    }
  })
  dateGroups.forEach((codes, dateKey) => {
    codes.sort()
    codes.forEach((code, idx) => {
      map.set(code, `HD${dateKey}${String(idx + 1).padStart(3, '0')}`)
    })
  })
  return map
})

const formatMaHoaDon = (code) => invoiceCodeMap.value.get(code) || code

const totalPages = computed(() =>
  Math.ceil(filteredData.value.length / pageSize.value)
)

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

const viewDetail = (id) => {
  openMoreMenuId.value = null
  router.push({
    path: `${panelBasePath.value}/hoa-don/detail/${id}`,
    query: currentPage.value > 1 ? { page: String(currentPage.value) } : {}
  })
}

const handleWindowFocus = () => {
  loadData()
}

const handleDocumentClick = (event) => {
  if (!event?.target?.closest?.('.row-more-wrap')) {
    openMoreMenuId.value = null
  }
}

const toggleMoreMenu = (id) => {
  if (openMoreMenuId.value === id) {
    openMoreMenuId.value = null
    return
  }

  openMoreMenuId.value = id
  const row = paginatedData.value.find((item) => item?.id === id)
  if (!row || !canTransferFromList(row)) return

  const currentValue = Number(transferTargetById.value[id] || 0)
  const candidates = getTransferCandidates(row)
  const hasCurrent = candidates.some((item) => Number(item?.id) === currentValue)
  if (!hasCurrent) {
    transferTargetById.value[id] = String(candidates[0]?.id || "")
  }
}

const getPrimaryActionType = (hoaDon) => {
  if (canConfirmFromList(hoaDon)) return "confirm"
  if (canStartShippingFromList(hoaDon)) return "shipping"
  if (canCompleteFromList(hoaDon)) return "complete"
  return ""
}

const getPrimaryActionLabel = (hoaDon) => {
  const type = getPrimaryActionType(hoaDon)
  if (!type) return ""
  if (rowSavingById.value[hoaDon.id]) return "Đang xử lý"
  if (type === "confirm") return "Xác nhận đơn"
  if (type === "shipping") return "Bắt đầu giao hàng"
  return getCompleteButtonLabel(hoaDon)
}

const runPrimaryAction = (hoaDon) => {
  const type = getPrimaryActionType(hoaDon)
  if (type === "confirm") return confirmFromList(hoaDon)
  if (type === "shipping") return startShippingFromList(hoaDon)
  if (type === "complete") return completeFromList(hoaDon)
}

const hasExtraActions = (hoaDon) => {
  const primary = getPrimaryActionType(hoaDon)
  const hasConfirm = canConfirmFromList(hoaDon) && primary !== "confirm"
  const hasShipping = canStartShippingFromList(hoaDon) && primary !== "shipping"
  const hasComplete = canCompleteFromList(hoaDon) && primary !== "complete"
  return hasConfirm || hasShipping || hasComplete || canTransferFromList(hoaDon) || canCancelFromList(hoaDon)
}

const getStatusColor = (status) => getAdminStatusTone(status)

const extractList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const normalizeDateKey = (value) => {
  if (!value) return ""
  if (Array.isArray(value) && value.length >= 3) {
    const [y, m, d] = value
    return `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`
  }

  const raw = String(value).trim()
  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw
  if (raw.includes("T")) return raw.split("T")[0]

  const match = raw.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/)
  if (!match) return ""
  const day = String(match[1]).padStart(2, "0")
  const month = String(match[2]).padStart(2, "0")
  return `${match[3]}-${month}-${day}`
}

const normalizeTime = (value, fallback = "00:00") => {
  if (!value) return fallback
  const raw = String(value).trim()
  if (/^\d{2}:\d{2}$/.test(raw)) return raw
  if (/^\d{2}:\d{2}:\d{2}$/.test(raw)) return raw.slice(0, 5)
  return fallback
}

const toMinutes = (timeValue) => {
  const normalized = normalizeTime(timeValue)
  const [hour, minute] = normalized.split(":")
  return Number(hour) * 60 + Number(minute)
}

const getDateKey = (date) => {
  const d = date instanceof Date ? date : new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, "0")
  const day = String(d.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

const getMinutesOfDate = (date) => {
  const d = date instanceof Date ? date : new Date(date)
  return d.getHours() * 60 + d.getMinutes()
}

const resolveOrderEmployeeId = (hoaDon) => {
  const candidate = hoaDon?.nhanVienId ?? hoaDon?.idNhanVien
  const numeric = Number(candidate)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : null
}

const buildActiveShiftEmployees = (nhanVienRows, lichRows, atDate = new Date()) => {
  const todayKey = getDateKey(atDate)
  const nowMinutes = getMinutesOfDate(atDate)

  const inProgress = lichRows
    .filter((item) => normalizeDateKey(item?.ngayLam) === todayKey)
    .map((item) => ({
      ...item,
      gioBatDau: normalizeTime(item?.gioBatDau),
      gioKetThuc: normalizeTime(item?.gioKetThuc)
    }))
    .filter((item) => {
      const start = toMinutes(item?.gioBatDau)
      const end = toMinutes(item?.gioKetThuc)
      return nowMinutes >= start && nowMinutes < end
    })

  const mapByEmployee = new Map()
  for (const item of inProgress) {
    const employeeId = Number(item?.idNhanVien)
    if (!Number.isFinite(employeeId) || employeeId <= 0) continue
    if (!mapByEmployee.has(employeeId)) {
      mapByEmployee.set(employeeId, item)
    }
  }

  return Array.from(mapByEmployee.entries())
    .map(([employeeId, schedule]) => {
      const employee = nhanVienRows.find((row) => Number(row?.id) === employeeId) || {}
      const displayName = employee?.tenNhanVien || schedule?.tenNhanVien || `NV #${employeeId}`
      return {
        id: employeeId,
        tenNhanVien: displayName,
        idCaLam: Number(schedule?.idCaLam || 0),
        tenCa: schedule?.tenCa || "",
        gioBatDau: schedule?.gioBatDau || "",
        gioKetThuc: schedule?.gioKetThuc || ""
      }
    })
    .sort((a, b) => a.tenNhanVien.localeCompare(b.tenNhanVien, "vi"))
}

const getOrderTypeLabel = (hoaDon) => {
  const explicitCandidates = [
    hoaDon?.orderType,
    hoaDon?.loaiDon,
    hoaDon?.orderTypeCode,
    hoaDon?.kieuDon
  ]

  for (const candidate of explicitCandidates) {
    const normalized = String(candidate || "").trim().toUpperCase()
    if (!normalized) continue
    if (normalized.includes("POS") || normalized.includes("TAI_QUAY")) return "Tại quầy"
    if (normalized.includes("ONLINE")) return "Trực tuyến"
  }

  const note = String(hoaDon?.statusNote || "").toUpperCase()
  if (note.includes("[POS]")) return "Tại quầy"

  const deliveryAddress = String(hoaDon?.diaChiNhanHang || "").trim().toLowerCase()
  if (deliveryAddress.includes("mua tại quầy") || deliveryAddress.includes("mua tai quay")) {
    return "Tại quầy"
  }

  return "Trực tuyến"
}

const deriveFulfillmentStatusLabel = (hoaDon) => {
  const explicit = String(hoaDon?.fulfillmentStatusName || "").trim()
  if (explicit) return explicit

  const code = normalizeOrderStatusCode(hoaDon?.orderStatusCode, hoaDon?.orderStatusName, hoaDon?.statusNote)
  if (code === "CHO_XAC_NHAN") return "Chờ xác nhận"
  if (code === "CHO_LAY_HANG") return "Chờ lấy hàng"
  if (code === "DANG_GIAO") return "Đang giao"
  if (code === "DA_GIAO") return "Đã giao"
  if (code === "HOAN_THANH") return "Hoàn thành"
  if (code === "GIAO_THAT_BAI") return "Giao thất bại"
  if (code === "HOAN_VE") return "Hoàn về"
  if (code === "HUY") return "Đã hủy"
  return "Chờ xác nhận"
}

const deriveBusinessClosureLabel = (hoaDon) => {
  const explicitName = String(hoaDon?.businessClosureStatusName || "").trim()
  if (explicitName) return explicitName

  const explicit = String(hoaDon?.businessClosureStatus || "").trim().toUpperCase()
  if (explicit === "CLOSED") return "Đã chốt"
  if (explicit === "OPEN") return "Đang mở"

  const code = normalizeOrderStatusCode(hoaDon?.orderStatusCode, hoaDon?.orderStatusName, hoaDon?.statusNote)
  if (["HOAN_THANH", "HUY", "HOAN_VE"].includes(code)) return "Đã chốt"
  return "Đang mở"
}

const canConfirmFromList = (hoaDon) => {
  const code = normalizeOrderStatusCode(hoaDon?.orderStatusCode, hoaDon?.orderStatusName, hoaDon?.statusNote)
  const isPos = getOrderTypeLabel(hoaDon) === "Tại quầy"
  return !isPos && code === "CHO_XAC_NHAN"
}

const canStartShippingFromList = (hoaDon) => {
  const code = normalizeOrderStatusCode(hoaDon?.orderStatusCode, hoaDon?.orderStatusName, hoaDon?.statusNote)
  const isPos = getOrderTypeLabel(hoaDon) === "Tại quầy"
  return !isPos && code === "CHO_LAY_HANG"
}

const canCancelFromList = (hoaDon) => {
  const code = normalizeOrderStatusCode(hoaDon?.orderStatusCode, hoaDon?.orderStatusName, hoaDon?.statusNote)
  return code === "CHO_XAC_NHAN" || code === "CHO_LAY_HANG"
}

const isCanceledOrder = (hoaDon) => {
  if (!hoaDon) return false
  const code = normalizeOrderStatusCode(hoaDon?.orderStatusCode, hoaDon?.orderStatusName, hoaDon?.statusNote)
  return code === "HUY"
}

const canCompleteFromList = (hoaDon) => {
  const code = normalizeOrderStatusCode(hoaDon?.orderStatusCode, hoaDon?.orderStatusName, hoaDon?.statusNote)
  const isPos = getOrderTypeLabel(hoaDon) === "Tại quầy"
  if (isPos) return code === "CHO_LAY_HANG"
  return code === "DANG_GIAO"
}

const canTransferFromList = (hoaDon) => {
  const code = normalizeOrderStatusCode(hoaDon?.orderStatusCode, hoaDon?.orderStatusName, hoaDon?.statusNote)
  const isOnline = getOrderTypeLabel(hoaDon) === "Trực tuyến"
  if (!isOnline) return false
  if (!["CHO_XAC_NHAN", "CHO_LAY_HANG", "DANG_GIAO"].includes(code)) return false
  return getTransferCandidates(hoaDon).length > 0
}

const getTransferCandidates = (hoaDon) => {
  const currentEmployeeId = resolveOrderEmployeeId(hoaDon)
  return activeShiftEmployees.value.filter((item) => Number(item?.id) !== Number(currentEmployeeId))
}

const getTransferTargetName = (hoaDon) => {
  const selectedId = Number(transferTargetById.value[hoaDon?.id] || 0)
  const selected = getTransferCandidates(hoaDon).find((item) => Number(item?.id) === selectedId)
  return selected?.tenNhanVien || ""
}

const transferFromList = async (hoaDon) => {
  if (!hoaDon?.id || !canTransferFromList(hoaDon)) return

  const candidates = getTransferCandidates(hoaDon)
  const selectedId = Number(transferTargetById.value[hoaDon.id] || candidates[0]?.id || 0)
  if (!selectedId) {
    apiWarning.value = `Không tìm thấy nhân viên đang trực để chuyển đơn ${hoaDon.maHoaDon || `#${hoaDon.id}`}`
    showToast(apiWarning.value, "error")
    return
  }

  const target = candidates.find((item) => Number(item?.id) === selectedId)
  if (!target) {
    apiWarning.value = "Nhân viên nhận đơn không hợp lệ hoặc đã hết ca."
    showToast(apiWarning.value, "error")
    return
  }

  const fromName = hoaDon?.tenNhanVien || `NV #${resolveOrderEmployeeId(hoaDon) || "?"}`
  const note = `Chuyển xử lý: ${fromName} -> ${target.tenNhanVien} (${new Date().toLocaleString("vi-VN")})`

  rowSavingById.value[hoaDon.id] = true
  try {
    await updateHoaDon(hoaDon.id, {
      idNhanVien: selectedId,
      statusNote: note
    })

    transferTargetById.value[hoaDon.id] = ""
    await loadData()
    showToast(`Đã chuyển đơn ${hoaDon.maHoaDon || `#${hoaDon.id}`} cho ${target.tenNhanVien}`, "success")
  } catch (error) {
    console.error("Transfer from list failed:", error)
    apiWarning.value = error?.response?.data?.message || "Không thể chuyển người xử lý hóa đơn"
    showToast(apiWarning.value, "error")
  } finally {
    rowSavingById.value[hoaDon.id] = false
  }
}

const getCompleteButtonLabel = (hoaDon) => {
  return getOrderTypeLabel(hoaDon) === "Tại quầy" ? "Xác nhận hoàn thành" : "Xác nhận giao thành công"
}

const cancelFromList = async (hoaDon) => {
  if (!hoaDon?.id || !canCancelFromList(hoaDon)) return

  const reason = String(cancelReasonById.value[hoaDon.id] || "").trim()
  if (!reason) {
    apiWarning.value = `Vui lòng nhập lý do hủy cho hóa đơn ${hoaDon.maHoaDon || `#${hoaDon.id}`}`
    return
  }

  const ok = await askConfirm(`Hủy đơn ${hoaDon.maHoaDon || `#${hoaDon.id}`}?`)
  if (!ok) return

  rowSavingById.value[hoaDon.id] = true
  try {
    let lastError = null

    try {
      await cancelHoaDon(hoaDon.id, reason)
    } catch (error) {
      lastError = error
      try {
        await updateHoaDonBySystemEvent(
          hoaDon.id,
          "HUY_DON_HANG",
          `Huỷ đơn: ${reason}`
        )
        lastError = null
      } catch (fallbackError) {
        lastError = fallbackError
      }
    }

    await loadData()

    const refreshed = (hoaDons.value || []).find((item) => Number(item?.id) === Number(hoaDon.id))
    if (lastError && !isCanceledOrder(refreshed)) {
      throw lastError
    }

    cancelReasonById.value[hoaDon.id] = ""
    showToast(`Đã hủy đơn ${hoaDon.maHoaDon || `#${hoaDon.id}`}`, "success")
  } catch (error) {
    console.error("Cancel from list failed:", error)
    apiWarning.value = error?.response?.data?.message || "Không thể hủy hóa đơn"
    showToast(apiWarning.value, "error")
  } finally {
    rowSavingById.value[hoaDon.id] = false
  }
}

const confirmFromList = async (hoaDon) => {
  if (!hoaDon?.id || !canConfirmFromList(hoaDon)) return

  const ok = await askConfirm(`Xác nhận đơn ${hoaDon.maHoaDon || `#${hoaDon.id}`}?`)
  if (!ok) return

  rowSavingById.value[hoaDon.id] = true
  try {
    const trackingUrl = buildOrderLookupTrackingUrl({ maHoaDon: hoaDon.maHoaDon })
    await updateHoaDonBySystemEvent(
      hoaDon.id,
      "XAC_NHAN_DON_HANG",
      "Nhân viên xác nhận đơn hàng",
      trackingUrl
    )
    await loadData()
    showToast(`Đã xác nhận đơn ${hoaDon.maHoaDon || `#${hoaDon.id}`}`, "success")
  } catch (error) {
    console.error("Confirm from list failed:", error)
    apiWarning.value = error?.response?.data?.message || "Không thể xác nhận đơn hàng"
    showToast(apiWarning.value, "error")
  } finally {
    rowSavingById.value[hoaDon.id] = false
  }
}

const startShippingFromList = async (hoaDon) => {
  if (!hoaDon?.id || !canStartShippingFromList(hoaDon)) return

  const ok = await askConfirm(`Chuyển đơn ${hoaDon.maHoaDon || `#${hoaDon.id}`} sang đang giao?`)
  if (!ok) return

  rowSavingById.value[hoaDon.id] = true
  try {
    const trackingUrl = buildOrderLookupTrackingUrl({ maHoaDon: hoaDon.maHoaDon })
    await updateHoaDonBySystemEvent(
      hoaDon.id,
      "GIAO_HANG_BAT_DAU",
      "Đơn hàng đang được giao",
      trackingUrl
    )
    await loadData()
    showToast(`Đã chuyển đơn ${hoaDon.maHoaDon || `#${hoaDon.id}`} sang đang giao`, "success")
  } catch (error) {
    console.error("Start shipping from list failed:", error)
    apiWarning.value = error?.response?.data?.message || "Không thể chuyển trạng thái sang đang giao"
    showToast(apiWarning.value, "error")
  } finally {
    rowSavingById.value[hoaDon.id] = false
  }
}

const completeFromList = async (hoaDon) => {
  if (!hoaDon?.id || !canCompleteFromList(hoaDon)) return

  const ok = await askConfirm(`Hoàn thành đơn ${hoaDon.maHoaDon || `#${hoaDon.id}`}?`)
  if (!ok) return

  rowSavingById.value[hoaDon.id] = true
  try {
    const isPos = getOrderTypeLabel(hoaDon) === "Tại quầy"
    const trackingUrl = buildOrderLookupTrackingUrl({ maHoaDon: hoaDon.maHoaDon })
    await updateHoaDonBySystemEvent(
      hoaDon.id,
      isPos ? "HOAN_TAT_POS" : "GIAO_HANG_THANH_CONG",
      isPos
        ? "Nhân viên xác nhận hoàn tất bán hàng tại quầy"
        : "Giao hàng thành công",
      trackingUrl
    )
    await loadData()
    showToast(`Đã cập nhật đơn ${hoaDon.maHoaDon || `#${hoaDon.id}`}`, "success")
  } catch (error) {
    console.error("Complete from list failed:", error)
    apiWarning.value = error?.response?.data?.message || "Không thể chuyển trạng thái sang Hoàn thành"
    showToast(apiWarning.value, "error")
  } finally {
    rowSavingById.value[hoaDon.id] = false
  }
}
</script>

<template>
  <main class="wrap">
    <div class="card">
      <div v-if="apiWarning" class="list-warning">
        <strong>API cảnh báo:</strong> {{ apiWarning }}
      </div>

      <div v-if="pendingPaymentConfirmations.length" class="payment-warning">
        <div class="payment-warning-header">
          <span class="payment-warning-icon">⚠</span>
          <strong>Cần kiểm tra VNPay ({{ pendingPaymentConfirmations.length }} đơn chờ xác nhận tiền vào tài khoản shop):</strong>
        </div>
        <ul class="payment-warning-list">
          <li v-for="item in pendingPaymentConfirmations" :key="item.id" class="payment-warning-item">
            <router-link :to="`${panelBasePath}/hoa-don/detail/${item.id}`" class="payment-warning-link">
              <span class="payment-warning-code">{{ item.maHoaDon }}</span>
              <span class="payment-warning-sep">·</span>
              <span class="payment-warning-customer">{{ item.tenKhachHang || 'Khách lẻ' }}</span>
              <template v-if="item.soDienThoaiNhanHang">
                <span class="payment-warning-sep">·</span>
                <span class="payment-warning-phone">{{ item.soDienThoaiNhanHang }}</span>
              </template>
              <span class="payment-warning-sep">·</span>
              <span class="payment-warning-amount">{{ formatCurrency(item.thanhTien) }}</span>
              <span class="payment-warning-arrow">→ Xem đơn</span>
            </router-link>
          </li>
        </ul>
      </div>

      <!-- HEADER -->
      <div class="head" style="display: flex; align-items: center; justify-content: space-between;">
        <div>
          <h1>Quản lý Hoá Đơn</h1>
          <small class="muted">Theo dõi và quản lý tất cả đơn hàng</small>
        </div>
        <div style="display:flex; gap:8px;">
        </div>
      </div>

      <div class="body">
        <!-- FILTERS & SEARCH -->
        <div style="display: flex; gap: 12px; margin-bottom: 24px; flex-wrap: wrap;">
          <div class="search">
            <Search :size="18" style="color: #9ca3af;" />
            <input
              v-model="searchText"
              type="text"
              placeholder="Tìm theo mã hóa đơn, khách hàng, SĐT, nhân viên..."
              style="flex: 1; border: none; outline: none; background: transparent;"
            />
          </div>

          <select v-model="filterStatus" style="padding: 10px 34px 10px 12px; border: 1px solid var(--line); border-radius: 8px; min-width: 200px; font-size: 14px;">
            <option v-for="status in statusOptions" :key="status">{{ status }}</option>
          </select>

          <select v-model="filterType" style="padding: 10px 34px 10px 12px; border: 1px solid var(--line); border-radius: 8px; min-width: 160px; font-size: 14px;">
            <option>Tất cả loại</option>
            <option>Tại quầy</option>
            <option>Trực tuyến</option>
          </select>
        </div>

        <!-- TABLE -->
        <div style="overflow-x: auto;">
          <table class="table" style="width: 100%;">
            <thead>
              <tr>
                <th style="width: 270px; text-align: left;">Đơn hàng</th>
                <th style="width: 240px;">Khách hàng</th>
                <th class="value-head" style="width: 240px;">Giá trị</th>
                <th class="status-head" style="width: 250px;">Trạng thái</th>
                <th style="width: 280px; text-align: right;">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="paginatedData.length === 0">
                <td colspan="5" style="text-align: center; padding: 32px;">
                  <div style="color: #9ca3af;">Chưa có dữ liệu</div>
                </td>
              </tr>
              <tr
                v-for="(hd, rowIndex) in paginatedData"
                :key="hd.id"
                style="border-bottom: 1px solid var(--line);"
              >
                <td>
                  <div class="invoice-main-cell">
                    <div class="invoice-main-top">
                      <span class="invoice-id-chip">#{{ hd.id }}</span>
                      <span class="invoice-code">{{ formatMaHoaDon(hd.maHoaDon) }}</span>
                    </div>
                    <div class="invoice-main-sub">
                      <span>NV: {{ hd.tenNhanVien || "-" }}</span>
                      <span class="dot-sep">•</span>
                      <span>{{ formatDate(hd.ngayTao) }}</span>
                    </div>
                    <span v-if="shouldHighlightRow(hd)" class="row-attention-badge">Mới</span>
                  </div>
                </td>
                <td>
                  <div class="customer-cell">
                    <div class="customer-name">{{ hd.tenKhachHang || "Khách lẻ" }}</div>
                    <div class="customer-phone">{{ hd.soDienThoaiNhanHang || 'Không có SĐT' }}</div>
                  </div>
                </td>
                <td class="value-col">
                  <div class="money-cell">
                    <div class="money-total">{{ formatCurrency(hd.thanhTien) }}</div>
                    <div class="money-sub">Ship: {{ formatCurrency(hd.phiShip) }}</div>
                  </div>
                </td>
                <td class="status-col">
                  <div class="status-cell">
                    <span class="order-type-chip">{{ getOrderTypeLabel(hd) }}</span>
                    <span
                      class="status-badge"
                      :class="`status-${getStatusColor(hd.orderStatusName)}`"
                    >
                      {{ normalizeAdminStatusLabel(hd.orderStatusName) }}
                    </span>
                  </div>
                </td>
                <td class="action-col">
                  <div class="action-stack">
                    <button class="action-btn" @click="viewDetail(hd.id)" title="Xem chi tiết">
                      <Eye :size="18" />
                      <span>Chi tiết</span>
                    </button>

                    <button
                      v-if="getPrimaryActionType(hd)"
                      class="action-btn action-btn-primary"
                      type="button"
                      :disabled="rowSavingById[hd.id]"
                      @click="runPrimaryAction(hd)"
                    >
                      <span>{{ getPrimaryActionLabel(hd) }}</span>
                    </button>

                    <div v-if="hasExtraActions(hd)" class="row-more-wrap">
                      <button
                        class="action-btn more-toggle"
                        type="button"
                        @click.stop="toggleMoreMenu(hd.id)"
                      >
                        <span>Thêm</span>
                        <ChevronDown :size="16" />
                      </button>

                      <div
                        v-if="openMoreMenuId === hd.id"
                        class="row-more-menu"
                        :class="{ 'row-more-menu--up': rowIndex >= Math.max(paginatedData.length - 2, 0) }"
                        @click.stop
                      >
                        <button
                          v-if="canConfirmFromList(hd) && getPrimaryActionType(hd) !== 'confirm'"
                          class="menu-action"
                          type="button"
                          :disabled="rowSavingById[hd.id]"
                          @click="confirmFromList(hd)"
                        >
                          Xác nhận đơn
                        </button>

                        <button
                          v-if="canStartShippingFromList(hd) && getPrimaryActionType(hd) !== 'shipping'"
                          class="menu-action"
                          type="button"
                          :disabled="rowSavingById[hd.id]"
                          @click="startShippingFromList(hd)"
                        >
                          Bắt đầu giao hàng
                        </button>

                        <button
                          v-if="canCompleteFromList(hd) && getPrimaryActionType(hd) !== 'complete'"
                          class="menu-action"
                          type="button"
                          :disabled="rowSavingById[hd.id]"
                          @click="completeFromList(hd)"
                        >
                          {{ getCompleteButtonLabel(hd) }}
                        </button>

                        <div v-if="canTransferFromList(hd)" class="transfer-inline">
                          <select
                            v-model="transferTargetById[hd.id]"
                            class="transfer-select"
                          >
                            <option
                              v-for="emp in getTransferCandidates(hd)"
                              :key="emp.id"
                              :value="emp.id"
                            >
                              {{ emp.tenNhanVien }} ({{ emp.tenCa || 'Đang trực' }})
                            </option>
                          </select>
                          <button
                            class="transfer-btn"
                            type="button"
                            :disabled="rowSavingById[hd.id]"
                            @click="transferFromList(hd)"
                          >
                            {{ rowSavingById[hd.id] ? 'Đang chuyển' : 'Chuyển người xử lý' }}
                          </button>
                        </div>

                        <div v-if="canCancelFromList(hd)" class="cancel-inline">
                          <input
                            v-model="cancelReasonById[hd.id]"
                            type="text"
                            class="cancel-input"
                            placeholder="Lý do hủy..."
                          />
                          <button
                            class="cancel-btn"
                            type="button"
                            :disabled="rowSavingById[hd.id]"
                            @click="cancelFromList(hd)"
                          >
                            {{ rowSavingById[hd.id] ? "Đang hủy" : "Hủy" }}
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- PAGINATION -->
        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--line); flex-wrap: wrap; gap: 12px;">
          <div style="font-size: 13px; color: #9ca3af;">
            Hiển thị {{ paginatedData.length === 0 ? 0 : (currentPage - 1) * pageSize + 1 }} – {{ Math.min(currentPage * pageSize, filteredData.length) }} / {{ filteredData.length }}
          </div>
          <div style="display: flex; gap: 8px;">
            <button
              class="btn"
              @click="currentPage--"
              :disabled="currentPage === 1"
              style="padding: 8px 12px; font-size: 13px;"
            >
              ← Trước
            </button>
            <div style="padding: 8px 14px; background: var(--line-light); border-radius: 8px; min-width: 40px; text-align: center; font-weight: 600; font-size: 13px;">
              {{ currentPage }}
            </div>
            <button
              class="btn"
              @click="currentPage++"
              :disabled="currentPage >= totalPages"
              style="padding: 8px 12px; font-size: 13px;"
            >
              Sau →
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.search {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid var(--line);
  background: white;
  border-radius: 10px;
  flex: 1;
  min-width: 280px;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 15px;
}

.table thead {
  background: var(--line-light);
  border-bottom: 2px solid var(--line);
}

.table th {
  position: sticky;
  top: 0;
  z-index: 3;
  padding: 14px 14px;
  text-align: left;
  font-weight: 700;
  color: var(--text);
  font-size: 13px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  background: linear-gradient(90deg, #8f0f22 0%, #bd1730 42%, #df3a4e 100%);
  color: #fff;
}

.table td {
  padding: 16px 14px;
  color: var(--text);
  vertical-align: top;
}

.table tbody tr:nth-child(even) td {
  background: #fffafa;
}

.table tbody tr:hover td {
  background: #fff1f3;
}

.invoice-code-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.invoice-main-cell {
  display: grid;
  gap: 6px;
}

.invoice-main-top {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.invoice-id-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #fff1f2;
  border: 1px solid #fecdd3;
  color: #9f1239;
  font-size: 11px;
  font-weight: 700;
}

.invoice-code {
  font-weight: 700;
  color: #1f2937;
  font-size: 13px;
  letter-spacing: 0.01em;
}

.invoice-main-sub {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #6b7280;
  flex-wrap: wrap;
}

.dot-sep {
  color: #c4c9d4;
}

.customer-cell {
  display: grid;
  gap: 4px;
}

.customer-name {
  font-weight: 700;
  color: #1f2937;
  line-height: 1.3;
}

.customer-phone {
  font-size: 13px;
  color: #6b7280;
}

.money-cell {
  display: grid;
  justify-items: start;
  gap: 4px;
}

.value-col {
  padding-right: 68px;
  padding-left: 18px;
}

.status-col {
  padding-left: 18px;
  text-align: left;
}

.value-head {
  text-align: left !important;
  padding-right: 52px !important;
  padding-left: 18px !important;
}

.status-head {
  padding-left: 18px !important;
  text-align: left !important;
}

.money-total {
  font-weight: 800;
  color: #111827;
  letter-spacing: 0.01em;
}

.money-sub {
  font-size: 13px;
  color: #6b7280;
}

.status-cell {
  display: grid;
  gap: 8px;
  justify-items: start;
  align-items: start;
}

.order-type-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.03em;
  color: #7f1020;
  background: #fff5f6;
  border: 1px solid #fecdd3;
  text-transform: uppercase;
}

.row-attention-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #fee2e2;
  border: 1px solid #fca5a5;
  color: #b91c1c;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.status-badge {
  display: inline-block;
  padding: 7px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
  border: 1px solid;
  white-space: nowrap;
}

.status-success {
  background: #fff1f2;
  border-color: #fda4af;
  color: #b91c1c;
}

.status-danger {
  background: #fee2e2;
  border-color: #fca5a5;
  color: #b91c1c;
}

.status-warning {
  background: #fff1f2;
  border-color: #fda4af;
  color: #b91c1c;
}

.status-neutral {
  background: #fff5f5;
  border-color: #fecdd3;
  color: #9f1239;
}

.action-btn {
  background: transparent;
  border: 1px solid var(--line);
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  color: #6b7280;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: var(--line-light);
  color: var(--accent);
}

.action-btn-primary {
  border-color: #fca5a5;
  background: #fff1f2;
  color: #b91c1c;
  font-weight: 700;
}

.action-btn-primary:hover {
  background: #ffe4e6;
  color: #991b1b;
}

.action-col {
  padding-left: 24px;
}

.action-stack {
  display: grid;
  gap: 8px;
  max-width: 260px;
  justify-items: end;
  margin-left: auto;
}

.more-toggle {
  min-width: 96px;
  justify-content: center;
}

.row-more-wrap {
  position: relative;
}

.row-more-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 280px;
  border: 1px solid #fecdd3;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.14);
  padding: 10px;
  display: grid;
  gap: 8px;
  z-index: 12;
}

.row-more-menu--up {
  top: auto;
  bottom: calc(100% + 8px);
}

.menu-action {
  width: 100%;
  border: 1px solid #fda4af;
  background: #fff1f2;
  color: #b91c1c;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
}

.menu-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-inline {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 6px;
}

.transfer-inline {
  display: grid;
  grid-template-columns: 1fr;
  gap: 6px;
}

.transfer-select {
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 8px 34px 8px 10px;
  font-size: 13px;
  background-color: white;
  min-width: 150px;
  color: #111827;
  min-height: 38px;
  width: 100%;
}

.transfer-select.is-empty {
  color: #6b7280;
}

.transfer-btn {
  border: 1px solid #fca5a5;
  background: #fff1f2;
  color: #b91c1c;
  border-radius: 6px;
  font-size: 13px;
  padding: 8px 10px;
  cursor: pointer;
  white-space: nowrap;
  width: 100%;
}

.transfer-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-input {
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
}

.cancel-btn {
  border: 1px solid #fca5a5;
  background: #fff1f2;
  color: #b91c1c;
  border-radius: 6px;
  font-size: 13px;
  padding: 8px 10px;
  cursor: pointer;
}

.cancel-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.list-warning {
  margin: 20px 20px 0;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #fca5a5;
  background: #fff1f2;
  color: #b91c1c;
}

.payment-warning {
  margin: 14px 20px 0;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #fca5a5;
  background: #fff1f2;
  color: #b91c1c;
}

.payment-warning-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.payment-warning-icon {
  font-size: 16px;
}

.payment-warning-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.payment-warning-item {
  border-radius: 8px;
  overflow: hidden;
}

.payment-warning-link {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 12px;
  background: #fff5f5;
  border: 1px solid #fecdd3;
  border-radius: 8px;
  text-decoration: none;
  color: #9f1239;
  transition: background 0.15s ease;
}

.payment-warning-link:hover {
  background: #ffe4e6;
  border-color: #fca5a5;
}

.payment-warning-code {
  font-weight: 700;
  font-size: 13px;
  color: #b91c1c;
  font-family: monospace;
}

.payment-warning-customer {
  font-weight: 600;
  font-size: 13px;
}

.payment-warning-phone {
  font-size: 13px;
  color: #9f1239;
}

.payment-warning-amount {
  font-size: 13px;
  font-weight: 600;
  color: #b91c1c;
}

.payment-warning-sep {
  color: #f43f5e;
  font-size: 12px;
}

.payment-warning-arrow {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  color: #b91c1c;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .search {
    min-width: 100%;
  }

  .table {
    font-size: 14px;
  }

  .invoice-main-sub {
    gap: 4px;
  }

  .money-cell {
    justify-items: start;
  }

  .value-col,
  .status-col {
    padding-left: 8px;
    padding-right: 8px;
  }

  .value-head,
  .status-head {
    padding-left: 8px !important;
    padding-right: 8px !important;
  }

  .action-stack {
    max-width: 100%;
  }

  .row-more-menu {
    width: 240px;
  }
  
  .table th,
  .table td {
    padding: 10px 8px;
    font-size: 13px;
  }
}
</style>
