<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from "vue"
import { useRouter, useRoute } from "vue-router"
import { getAllHoaDon, updateHoaDon, updateHoaDonBySystemEvent, cancelHoaDon, getHoaDonById, sendOrderLookupMail } from "../../../services/hoaDonService"
import { getAllKhachHang, getKhachHangById } from "../../../services/KhachHangService"
import { getAllNhanVien } from "../../../services/nhanVienService"
import { getAllLichLamViecFull } from "../../../services/lichLamViecService"
import { getAllSanPham } from "../../../services/sanPhamService"
import { Eye, Plus, Search, ChevronDown, Loader2, Mail, X, UserRound, Phone, MapPin, CalendarDays, CreditCard, Package2, Truck, CheckCircle2, ClipboardList, ReceiptText } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel, normalizeOrderStatusCode } from "../../../utils/adminStatus"
import { hasPaymentFlowTag, PAYMENT_FLOW_TAGS } from "../../../utils/paymentWorkflow"
import { buildOrderLookupTrackingUrl } from "../../../utils/publicTrackingUrl"
import { resolveApiOrigin } from "../../../utils/apiOrigin"
import { fallbackImageFor, fallbackImageForVariant } from "../../../utils/productImageFallback"
import { getProductImageConfig, getProductImageOverride } from "../../../utils/productImageOverrides"
import { useToast } from "../../../composables/useToast"
import { useConfirm } from "../../../composables/useConfirm"
import { createHoaDonRealtimeChannel } from "../../../utils/hoaDonRealtime"

const router = useRouter()
const route = useRoute()
const { showToast } = useToast()
const { askConfirm } = useConfirm()
const panelBasePath = computed(() => (route.path.startsWith('/employee/') ? '/employee' : '/admin'))
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")

const hoaDons = ref([])
const loading = ref(false)
const apiWarning = ref("")
const searchText = ref("")
const filterStatus = ref("Tất cả trạng thái")
const filterType = ref("Tất cả loại")
const filterDateFrom = ref("")
const filterDateTo = ref("")
const activeDatePreset = ref("")
const currentPage = ref(1)
const pageSize = ref(10)
const cancelReasonById = ref({})
const rowSavingById = ref({})
const transferTargetById = ref({})
const activeShiftEmployees = ref([])
const allNhanVien = ref([])
const openMoreMenuId = ref(null)
const NOTIFICATION_STORAGE_KEY = "notifications:seen"
const realtimeConnectionLabel = ref("Polling")
const realtimeConnectionTone = ref("neutral")
const queuedReload = ref(false)
const correctedRowTotalsById = ref({})
const hydratingRowTotalsById = ref({})
let realtimeStop = null
let realtimeDebounceTimer = null
const loadFailStreak = ref(0)

const readPageFromQuery = (value) => {
  const page = Number(value)
  return Number.isInteger(page) && page > 0 ? page : 1
}

const loadData = async () => {
  if (loading.value) {
    queuedReload.value = true
    return
  }

  loading.value = true
  apiWarning.value = ""
  try {
    correctedRowTotalsById.value = {}
    hydratingRowTotalsById.value = {}
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
    const nvList = extractList(nhanVienRes?.data)
    allNhanVien.value = nvList
    activeShiftEmployees.value = buildActiveShiftEmployees(
      nvList,
      extractList(lichRes?.data)
    )
    loadFailStreak.value = 0
  } catch (error) {
    console.error("Error loading invoices:", error)
    hoaDons.value = []
    allNhanVien.value = []
    activeShiftEmployees.value = []
    apiWarning.value = error.message || "Không thể tải danh sách hoá đơn"
    loadFailStreak.value += 1
    // Prevent endless polling storms while backend is unavailable.
    if (loadFailStreak.value >= 3 && realtimeStop) {
      realtimeStop()
      realtimeStop = null
      realtimeConnectionLabel.value = "Tạm dừng polling"
      realtimeConnectionTone.value = "neutral"
    }
  } finally {
    loading.value = false
    if (queuedReload.value) {
      queuedReload.value = false
      setTimeout(() => loadData(), 180)
    }
  }
}

const queueRealtimeReload = () => {
  if (realtimeDebounceTimer) clearTimeout(realtimeDebounceTimer)
  realtimeDebounceTimer = setTimeout(() => {
    loadData()
  }, 450)
}

const updateRealtimeBadge = (state = {}) => {
  if (state.connected) {
    realtimeConnectionLabel.value = "WebSocket"
    realtimeConnectionTone.value = "online"
    return
  }

  realtimeConnectionLabel.value = "Polling"
  realtimeConnectionTone.value = "neutral"
}

const startRealtimeOrders = () => {
  if (realtimeStop) return
  const channel = createHoaDonRealtimeChannel({
    pollIntervalMs: 15000,
    reconnectDelayMs: 3500,
    onMessage: () => {
      queueRealtimeReload()
    },
    onState: (state) => {
      updateRealtimeBadge(state)
    },
  })

  channel.start()
  realtimeStop = () => channel.stop()
}

const handleWindowFocus = () => {
  // Try to recover realtime once user returns and backend may be up again.
  if (!realtimeStop) {
    startRealtimeOrders()
  }
  loadData()
}

onMounted(() => {
  currentPage.value = readPageFromQuery(route.query.page)
  loadData()
  startRealtimeOrders()
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
  if (realtimeStop) realtimeStop()
  if (realtimeDebounceTimer) clearTimeout(realtimeDebounceTimer)
  window.removeEventListener('focus', handleWindowFocus)
  document.removeEventListener('click', handleDocumentClick)
})

const toDayStartTimestamp = (value = "") => {
  if (!value) return null
  const date = new Date(`${value}T00:00:00`)
  if (Number.isNaN(date.getTime())) return null
  return date.getTime()
}

const toDayEndTimestamp = (value = "") => {
  if (!value) return null
  const date = new Date(`${value}T23:59:59.999`)
  if (Number.isNaN(date.getTime())) return null
  return date.getTime()
}

const clearDateFilters = () => {
  filterDateFrom.value = ""
  filterDateTo.value = ""
  activeDatePreset.value = ""
}

const toInputDate = (date) => {
  const d = new Date(date)
  if (Number.isNaN(d.getTime())) return ""
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, "0")
  const day = String(d.getDate()).padStart(2, "0")
  return `${y}-${m}-${day}`
}

const applyDatePreset = (preset) => {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  let from = ""
  let to = ""

  if (preset === "today") {
    from = toInputDate(today)
    to = toInputDate(today)
  } else if (preset === "last7") {
    const start = new Date(today)
    start.setDate(start.getDate() - 6)
    from = toInputDate(start)
    to = toInputDate(today)
  } else if (preset === "last30") {
    const start = new Date(today)
    start.setDate(start.getDate() - 29)
    from = toInputDate(start)
    to = toInputDate(today)
  } else if (preset === "thisMonth") {
    const start = new Date(today.getFullYear(), today.getMonth(), 1)
    from = toInputDate(start)
    to = toInputDate(today)
  } else if (preset === "lastMonth") {
    const start = new Date(today.getFullYear(), today.getMonth() - 1, 1)
    const end = new Date(today.getFullYear(), today.getMonth(), 0)
    from = toInputDate(start)
    to = toInputDate(end)
  }

  filterDateFrom.value = from
  filterDateTo.value = to
  activeDatePreset.value = preset
}

const isDateRangeInvalid = computed(() => {
  if (!filterDateFrom.value || !filterDateTo.value) return false
  return toDayStartTimestamp(filterDateFrom.value) > toDayEndTimestamp(filterDateTo.value)
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

const firstNonEmptyValue = (...values) => {
  for (const value of values) {
    const normalized = String(value || "").trim()
    if (normalized) return normalized
  }
  return ""
}

const normalizeEmailValue = (value) => {
  const normalized = String(value || "").trim()
  if (!normalized) return ""
  const lowered = normalized.toLowerCase()
  if (["null", "undefined", "nan", "n/a"].includes(lowered)) return ""
  if ((/^pos\.guest.*@dirtywave\.local$/i).test(normalized) || (/^guest.*@dirtywave\.local$/i).test(normalized)) return ""
  return normalized
}

const firstValidEmailValue = (...values) => {
  for (const value of values) {
    const normalized = normalizeEmailValue(value)
    if (normalized && normalized.includes("@")) return normalized
  }
  return ""
}

const formatDateTime = (date) => {
  if (!date) return "-"
  const d = new Date(date)
  if (Number.isNaN(d.getTime())) return "-"
  const day = d.getDate().toString().padStart(2, "0")
  const month = (d.getMonth() + 1).toString().padStart(2, "0")
  const year = d.getFullYear()
  const hour = d.getHours().toString().padStart(2, "0")
  const minute = d.getMinutes().toString().padStart(2, "0")
  return `${day}/${month}/${year} ${hour}:${minute}`
}

const normalizeImageUrl = (raw) => {
  const value = String(raw || "").trim()
  if (!value) return ""
  if (/^(https?:)?\/\//i.test(value) || value.startsWith("data:") || value.startsWith("blob:")) return value
  const cleaned = value.replace(/\\/g, "/")
  if (!BACKEND_ORIGIN) return cleaned
  if (cleaned.startsWith("/")) return `${BACKEND_ORIGIN}${cleaned}`
  return `${BACKEND_ORIGIN}/${cleaned.replace(/^\/+/, "")}`
}

const pickImageValue = (entry) => {
  if (!entry) return ""

  if (typeof entry === "string") {
    return normalizeImageUrl(entry)
  }

  if (Array.isArray(entry)) {
    for (const child of entry) {
      const found = pickImageValue(child)
      if (found) return found
    }
    return ""
  }

  if (typeof entry === "object") {
    const keys = ["anh", "hinhAnh", "image", "imageUrl", "images", "listAnh", "anhChinh", "duongDanAnh", "src", "thumbnail"]
    for (const key of keys) {
      const found = pickImageValue(entry[key])
      if (found) return found
    }
  }

  return ""
}

const getPaymentMethodLabel = (hoaDon) => {
  const code = String(
    hoaDon?.phuongThucThanhToan
    || hoaDon?.paymentMethod
    || hoaDon?.paymentMethodCode
    || ""
  ).toUpperCase()
  if (code.includes("VNPAY")) return "VNPay"
  if (code === "CASH" || code === "TIEN_MAT") return "Tiền mặt"
  if (code === "BANK" || code.includes("CHUYEN") || code.includes("TRANSFER")) return "Chuyển khoản"
  if (code === "COD") return "Thanh toán khi nhận hàng"
  if (!code) return "-"
  return hoaDon?.phuongThucThanhToan || hoaDon?.paymentMethod || "-"
}

const getSummaryImageKey = (item, idx) => String(item?.spctId || item?.sanPhamChiTietId || item?.id || idx)

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

  // Filter out broken/empty invoices (0₫ total)
  data = data.filter(d => Number(d?.thanhTien || 0) > 0)

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

  let fromTs = toDayStartTimestamp(filterDateFrom.value)
  let toTs = toDayEndTimestamp(filterDateTo.value)

  if (fromTs != null && toTs != null && fromTs > toTs) {
    const temp = fromTs
    fromTs = toTs
    toTs = temp
  }

  if (fromTs != null || toTs != null) {
    data = data.filter((d) => {
      const created = d?.ngayTao || d?.createdAt || d?.ngaySua
      if (!created) return false
      const createdTs = new Date(created).getTime()
      if (Number.isNaN(createdTs)) return false
      if (fromTs != null && createdTs < fromTs) return false
      if (toTs != null && createdTs > toTs) return false
      return true
    })
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

const getRowDisplayTotal = (hd) => {
  const corrected = Number(correctedRowTotalsById.value?.[Number(hd?.id || 0)] || 0)
  if (Number.isFinite(corrected) && corrected > 0) return corrected

  const savedTotal = Number(hd?.thanhTien || hd?.tongTienSauGiam || hd?.tongThanhToan || 0)
  const subtotal = Number(hd?.tongTien || hd?.tamTinh || 0)
  const shipping = Number(hd?.phiShip || hd?.phiVanChuyen || hd?.shippingFee || 0)
  const discount = Number(hd?.giamGia || hd?.giaSauGiamGia || hd?.discountAmount || 0)

  const likelySwappedLegacyTotals = (
    savedTotal > 0
    && discount > 0
    && subtotal > discount
    && Math.abs(savedTotal - discount) <= 1
  )

  if (Number.isFinite(savedTotal) && savedTotal > 0 && !likelySwappedLegacyTotals) return savedTotal

  const computed = subtotal + shipping - discount
  return computed > 0 ? computed : 0
}

const viewDetail = (id) => {
  openMoreMenuId.value = null
  router.push({
    path: `${panelBasePath.value}/hoa-don/detail/${id}`,
    query: currentPage.value > 1 ? { page: String(currentPage.value) } : {}
  })
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
  if (!["CHO_XAC_NHAN", "CHO_LAY_HANG", "DANG_GIAO"].includes(code)) return false
  return getTransferCandidates(hoaDon).length > 0
}

const getTransferCandidates = (hoaDon) => {
  const currentEmployeeId = resolveOrderEmployeeId(hoaDon)
  const pool = activeShiftEmployees.value.length > 0 ? activeShiftEmployees.value : allNhanVien.value
  return pool.filter((item) => Number(item?.id) !== Number(currentEmployeeId))
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

// ═══ Summary modal ═══
const summaryModal = ref(null)
const summaryLoading = ref(false)
const summaryImageErrors = ref({})

const variantCatalog = ref(new Map())
const variantCatalogByCode = ref(new Map())
const customerCatalog = ref(new Map())

const SUMMARY_ONLINE_TIMELINE = [
  { code: "CHO_XAC_NHAN", label: "Chờ xác nhận", icon: ClipboardList },
  { code: "CHO_LAY_HANG", label: "Chờ lấy hàng", icon: Package2 },
  { code: "DANG_GIAO", label: "Đang giao", icon: Truck },
  { code: "HOAN_THANH", label: "Hoàn thành", icon: CheckCircle2 },
]

const SUMMARY_POS_TIMELINE = [
  { code: "CHO_XAC_NHAN", label: "Chờ xử lý", icon: ClipboardList },
  { code: "CHO_LAY_HANG", label: "Đang phục vụ", icon: Package2 },
  { code: "HOAN_THANH", label: "Hoàn thành", icon: CheckCircle2 },
]

const getSummaryTimelineSteps = (hoaDon) => {
  return getOrderTypeLabel(hoaDon) === "Tại quầy" ? SUMMARY_POS_TIMELINE : SUMMARY_ONLINE_TIMELINE
}

const getSummaryTimelineState = (hoaDon, stepCode) => {
  const currentCode = normalizeOrderStatusCode(hoaDon?.orderStatusCode, hoaDon?.orderStatusName, hoaDon?.statusNote)
  const steps = getSummaryTimelineSteps(hoaDon)
  const currentIndex = steps.findIndex((step) => step.code === currentCode)
  const stepIndex = steps.findIndex((step) => step.code === stepCode)
  if (currentIndex === -1 || stepIndex === -1) return "upcoming"
  if (stepIndex < currentIndex) return "done"
  if (stepIndex === currentIndex) return "active"
  return "upcoming"
}

const loadCustomerCatalog = async () => {
  if (customerCatalog.value.size > 0) return
  try {
    const res = await getAllKhachHang()
    const list = Array.isArray(res?.data?.content) ? res.data.content
      : Array.isArray(res?.data) ? res.data : []
    const map = new Map()
    for (const customer of list) {
      map.set(Number(customer?.id), customer)
    }
    customerCatalog.value = map
  } catch {
    customerCatalog.value = new Map()
  }
}

const loadVariantCatalog = async () => {
  if (variantCatalog.value.size > 0 || variantCatalogByCode.value.size > 0) return
  try {
    const res = await getAllSanPham()
    const products = Array.isArray(res?.data?.content) ? res.data.content
      : Array.isArray(res?.data) ? res.data : []

    const byIdMap = new Map()
    const byCodeMap = new Map()

    for (const sp of products) {
      const spName = sp.tenSanPham || sp.ten || ''
      const imageConfig = getProductImageConfig({ id: sp?.id, maSanPham: sp?.maSanPham })
      const overrideImage = String(getProductImageOverride({ id: sp?.id, maSanPham: sp?.maSanPham })?.[0] || "").trim()
      const productDirectImage = pickImageValue(sp)
      const variants = Array.isArray(sp.sanPhamChiTiets) ? sp.sanPhamChiTiets
        : Array.isArray(sp.chiTietSanPhams) ? sp.chiTietSanPhams
        : Array.isArray(sp.variants) ? sp.variants : []
      for (const v of variants) {
        const colorName = v.mauSac?.tenMauSac || v.mauSac?.tenMau || v.tenMauSac || ''
        const sizeName = v.kichThuoc?.tenKichThuoc || v.kichThuoc?.tenSize || v.tenKichThuoc || ''
        const parts = [spName, colorName, sizeName].filter(Boolean)
        const colorId = Number(v?.mauSac?.id || v?.mauSacId || v?.idMauSac || 0)
        const colorImage = (imageConfig?.colorImages || []).find((entry) => Number(entry?.colorId) === colorId)?.image || ""
        const variantDirectImage = pickImageValue(v)
        const byVariantFallback = fallbackImageForVariant({
          id: Number(v?.id || sp?.id || 0),
          maSanPham: sp?.maSanPham,
          tenSanPham: spName,
          tenMauSac: colorName,
          maChiTietSanPham: String(v?.ma || v?.maSanPhamChiTiet || ""),
        })
        const resolvedImage = normalizeImageUrl(
          variantDirectImage
          || colorImage
          || productDirectImage
          || overrideImage
          || byVariantFallback
          || fallbackImageFor(Number(v?.id || sp?.id || 0), sp?.maSanPham, spName)
        )

        const entry = {
          ten: parts.join(' - '),
          anh: resolvedImage,
          mauSac: colorName,
          kichThuoc: sizeName,
          giaBan: Number(v.giaBan || 0),
        }

        const variantId = Number(v.id || v.spctId || v.sanPhamChiTietId || 0)
        if (Number.isFinite(variantId) && variantId > 0) {
          byIdMap.set(variantId, entry)
        }
        const variantCode = String(v.ma || v.maSanPhamChiTiet || "").trim().toUpperCase()
        if (variantCode) {
          byCodeMap.set(variantCode, entry)
        }
      }
    }

    variantCatalog.value = byIdMap
    variantCatalogByCode.value = byCodeMap
  } catch { /* catalog load failed, items will show fallback names */ }
}

const enrichItems = (items) => {
  const collectItemLabels = (item, fields = []) => {
    const labels = new Set()
    for (const field of fields) {
      const value = String(field || "").trim()
      if (value) labels.add(value)
    }
    return Array.from(labels)
  }

  return items.map(item => {
    const spctId = Number(item.spctId || item.sanPhamChiTietId || item.idSanPhamChiTiet || item.chiTietSanPhamId || item.sanPhamChiTiet?.id || 0)
    const maSanPhamChiTiet = String(item.maSanPhamChiTiet || item.maSpct || item.sanPhamChiTiet?.maSanPhamChiTiet || '').trim().toUpperCase()
    const cat = variantCatalog.value.get(spctId) || (maSanPhamChiTiet ? variantCatalogByCode.value.get(maSanPhamChiTiet) : undefined)
    const mauSac = item.sanPhamChiTiet?.mauSac?.tenMauSac || item.tenMauSac || cat?.mauSac || ''
    const kichThuoc = item.sanPhamChiTiet?.kichThuoc?.tenKichThuoc || item.tenKichThuoc || cat?.kichThuoc || ''
    const maSanPham = item.maSanPham || item.sanPhamChiTiet?.maSanPham || item.sanPham?.maSanPham || ""
    // Extract base product name without variant suffix
    const rawName = item.tenSanPhamChiTiet || cat?.ten || item.tenSanPham || `SP #${spctId}`
    const suffixParts = [kichThuoc, mauSac].filter(Boolean)
    let baseName = rawName
    for (const part of suffixParts) {
      const escaped = part.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      baseName = baseName.replace(new RegExp('\\s*-\\s*' + escaped + '\\s*$'), '')
      baseName = baseName.replace(new RegExp('\\s*-\\s*' + escaped + '\\s*-'), ' -')
    }
    const itemVoucherLabels = collectItemLabels(item, [
      item?.maPhieuGiamGia,
      item?.tenPhieuGiamGia,
      item?.voucherCode,
      item?.voucherName,
      item?.phieuGiamGia?.maPhieuGiamGia,
      item?.phieuGiamGia?.tenPhieuGiamGia,
    ])
    const itemCampaignLabels = collectItemLabels(item, [
      item?.tenKhuyenMai,
      item?.tenDotGiamGia,
      item?.dotGiamGia?.tenDotGiamGia,
      item?.khuyenMai?.tenKhuyenMai,
      item?.maKhuyenMai,
      item?.maDotGiamGia,
      item?.dotGiamGia?.maDotGiamGia,
      item?.khuyenMai?.maKhuyenMai,
    ])
    const rawQty = Number(item.soLuong || item.quantity || 0)
    const resolvedQty = Number.isFinite(rawQty) && rawQty > 0 ? rawQty : 0
    const discountedUnitPrice = Number(item.giaSauGiam || item.giaBanSauDotGiamGia || 0)
    const fallbackUnitPrice = Number(item.donGia || item.giaBan || cat?.giaBan || 0)
    const resolvedUnitPrice = discountedUnitPrice > 0 ? discountedUnitPrice : fallbackUnitPrice
    const rawLineTotal = Number(item.thanhTien || item.lineTotal || 0)
    const computedLineTotal = resolvedUnitPrice * resolvedQty
    const resolvedLineTotal = discountedUnitPrice > 0
      ? computedLineTotal
      : (rawLineTotal > 0 ? rawLineTotal : computedLineTotal)

    return {
      ...item,
      _tenHienThi: baseName.trim(),
      _maSanPham: maSanPham,
      _anh: normalizeImageUrl(
        cat?.anh
        || item.sanPhamChiTiet?.anh
        || item.sanPhamChiTiet?.hinhAnh
        || item.anh
        || item.hinhAnh
        || item.hinhAnhUrl
        || item.image
        || item.urlAnh
        || ''
      ),
      _mauSac: mauSac,
      _kichThuoc: kichThuoc,
      _maSanPhamChiTiet: maSanPhamChiTiet,
      _fallbackAnh:
        fallbackImageForVariant({
          id: spctId,
          maSanPham: maSanPham,
          tenSanPham: baseName.trim(),
          tenMauSac: mauSac,
          maChiTietSanPham: maSanPhamChiTiet,
        }) || fallbackImageFor(spctId, maSanPham || maSanPhamChiTiet, baseName.trim()),
      _giaBan: resolvedUnitPrice,
      _thanhTien: resolvedLineTotal,
      _itemVoucherLabels: itemVoucherLabels,
      _itemCampaignLabels: itemCampaignLabels,
    }
  })
}

const markSummaryImageError = (item, idx) => {
  const key = getSummaryImageKey(item, idx)
  summaryImageErrors.value[key] = Number(summaryImageErrors.value[key] || 0) + 1
}

const getSummaryItemImageSrc = (item, idx) => {
  // Prefer the actual variant image from order/catalog first.
  if (item?._anh) {
    const errorCount = Number(summaryImageErrors.value[getSummaryImageKey(item, idx)] || 0)
    if (errorCount === 0) return item._anh
  }
  // Use mapped fallback only when real image is missing/failed.
  if (item?._fallbackAnh) return item._fallbackAnh
  // Show initials as last resort
  return ""
}

const summaryItemsSubtotal = computed(() => {
  const items = summaryModal.value?.items || []
  return items.reduce((sum, item) => sum + Number(item?._thanhTien || 0), 0)
})

const extractVoucherSnapshotFromNote = (note = "") => {
  const text = String(note || "")
  const voucherMatch = text.match(/áp dụng voucher\s*([^|\n\r]+)/i)
  const phrase = voucherMatch?.[1] ? voucherMatch[1].trim() : ""

  if (!phrase) return ""

  const cleaned = phrase.replace(/[\s:;,.-]+$/, "").trim()
  if (!cleaned) return ""
  return cleaned
}

const collectSummaryAppliedVouchers = (detail = {}, hd = {}, items = []) => {
  const labels = new Set()

  const directCandidates = [
    detail?.maPhieuGiamGia,
    detail?.tenPhieuGiamGia,
    detail?.voucherCode,
    detail?.voucherName,
    detail?.phieuGiamGia?.maPhieuGiamGia,
    detail?.phieuGiamGia?.tenPhieuGiamGia,
    hd?.maPhieuGiamGia,
    hd?.tenPhieuGiamGia,
    hd?.voucherCode,
    hd?.voucherName,
  ].map((entry) => String(entry || "").trim()).filter(Boolean)

  for (const value of directCandidates) labels.add(value)

  const noteLabel = extractVoucherSnapshotFromNote(detail?.statusNote || hd?.statusNote || "")
  if (noteLabel) labels.add(noteLabel)

  for (const item of items) {
    const itemCandidates = [
      item?.maPhieuGiamGia,
      item?.tenPhieuGiamGia,
      item?.voucherCode,
      item?.voucherName,
      item?.phieuGiamGia?.maPhieuGiamGia,
      item?.phieuGiamGia?.tenPhieuGiamGia,
    ].map((entry) => String(entry || "").trim()).filter(Boolean)
    for (const value of itemCandidates) labels.add(value)
  }

  return Array.from(labels)
}

const collectSummaryAppliedCampaigns = (items = []) => {
  const labels = new Set()

  for (const item of items) {
    const name = String(
      item?.tenKhuyenMai
      || item?.tenDotGiamGia
      || item?.dotGiamGia?.tenDotGiamGia
      || item?.khuyenMai?.tenKhuyenMai
      || ""
    ).trim()

    const code = String(
      item?.maKhuyenMai
      || item?.maDotGiamGia
      || item?.dotGiamGia?.maDotGiamGia
      || item?.khuyenMai?.maKhuyenMai
      || ""
    ).trim()

    const label = [code, name].filter(Boolean).join(" - ") || name || code
    if (label) labels.add(label)
  }

  return Array.from(labels)
}

const resolveSummaryDiscountAmount = ({ detail = {}, hd = {}, items = [] } = {}) => {
  const explicitDiscountCandidates = [
    detail?.giamGia,
    detail?.discountAmount,
    detail?.giaSauGiamGia,
    detail?.tongGiamGia,
    hd?.giamGia,
    hd?.discountAmount,
    hd?.giaSauGiamGia,
    hd?.tongGiamGia,
  ]

  const explicitDiscount = explicitDiscountCandidates
    .map((value) => Number(value || 0))
    .find((value) => Number.isFinite(value) && value > 0) || 0

  const orderTypeRaw = String(detail?.orderType || hd?.orderType || "").toUpperCase()
  const orderType = orderTypeRaw || (String(detail?.statusNote || hd?.statusNote || "").toUpperCase().includes("[POS]") ? "POS" : "ONLINE")
  const itemsSubtotal = items.reduce((sum, item) => sum + Number(item?._thanhTien || 0), 0)
  const shipFee = orderType === "POS" ? 0 : Number(detail?.phiVanChuyen || detail?.shippingFee || hd?.phiVanChuyen || hd?.shippingFee || 0)
  const finalTotal = Number(detail?.thanhTien || hd?.thanhTien || detail?.tongTienSauGiam || detail?.tongThanhToan || 0)
  const inferredDiscount = itemsSubtotal + shipFee - finalTotal

  const likelySwappedLegacyTotals = (
    explicitDiscount > 0
    && finalTotal > 0
    && itemsSubtotal > explicitDiscount
    && Math.abs(finalTotal - explicitDiscount) <= 1
  )

  const likelyReverseSwappedLegacyTotals = (
    explicitDiscount > 0
    && finalTotal > 0
    && itemsSubtotal > 0
    && explicitDiscount > finalTotal
    && (explicitDiscount / Math.max(itemsSubtotal, 1)) >= 0.6
    && (finalTotal / Math.max(itemsSubtotal, 1)) <= 0.5
    && Math.abs((explicitDiscount + finalTotal) - (itemsSubtotal + shipFee)) <= 2
  )

  if (likelySwappedLegacyTotals) return explicitDiscount
  if (likelyReverseSwappedLegacyTotals) return finalTotal

  if (finalTotal > 0 && itemsSubtotal > 0 && inferredDiscount >= 0) {
    if (explicitDiscount <= 0) return inferredDiscount

    const explicitFinalTotal = itemsSubtotal + shipFee - explicitDiscount
    const explicitMismatchesSavedTotal = Math.abs(explicitFinalTotal - finalTotal) > 1
    if (explicitMismatchesSavedTotal) return inferredDiscount
  }

  if (explicitDiscount > 0) return explicitDiscount
  return inferredDiscount > 0 ? inferredDiscount : 0
}

const computeCorrectedTotalsFromDetail = (detail = {}, hd = {}) => {
  const rawItems = Array.isArray(detail?.items) ? detail.items
    : Array.isArray(detail?.hoaDonChiTiets) ? detail.hoaDonChiTiets
    : Array.isArray(detail?.chiTietHoaDons) ? detail.chiTietHoaDons
    : []

  const items = enrichItems(rawItems)
  const itemsSubtotal = items.reduce((sum, item) => sum + Number(item?._thanhTien || 0), 0)
  const orderTypeRaw = String(detail?.orderType || hd?.orderType || '').toUpperCase()
  const orderType = orderTypeRaw || (String(detail?.statusNote || hd?.statusNote || '').toUpperCase().includes('[POS]') ? 'POS' : 'ONLINE')
  const shippingFee = orderType === 'POS'
    ? 0
    : Number(detail?.phiVanChuyen || detail?.shippingFee || detail?.phiShip || hd?.phiShip || 0)

  const fallbackDiscount = resolveSummaryDiscountAmount({ detail, hd, items })
  const savedFinalCandidates = [
    detail?.thanhTien,
    hd?.thanhTien,
    detail?.tongTienSauGiam,
    detail?.tongThanhToan,
  ]
  const savedFinalTotal = savedFinalCandidates
    .map((value) => Number(value || 0))
    .find((value) => Number.isFinite(value) && value > 0) || 0

  const likelySwappedLegacyTotals = (
    fallbackDiscount > 0
    && savedFinalTotal > 0
    && itemsSubtotal > fallbackDiscount
    && Math.abs(savedFinalTotal - fallbackDiscount) <= 1
  )

  const likelyReverseSwappedLegacyTotals = (
    fallbackDiscount > 0
    && savedFinalTotal > 0
    && itemsSubtotal > 0
    && fallbackDiscount > savedFinalTotal
    && (fallbackDiscount / Math.max(itemsSubtotal, 1)) >= 0.6
    && (savedFinalTotal / Math.max(itemsSubtotal, 1)) <= 0.5
    && Math.abs((fallbackDiscount + savedFinalTotal) - (itemsSubtotal + shippingFee)) <= 2
  )

  const finalTotal = savedFinalTotal > 0 && !likelySwappedLegacyTotals && !likelyReverseSwappedLegacyTotals
    ? savedFinalTotal
    : (likelyReverseSwappedLegacyTotals
      ? fallbackDiscount
      : Math.max(itemsSubtotal + shippingFee - fallbackDiscount, 0))

  const inferredDiscount = itemsSubtotal + shippingFee - finalTotal
  const discountAmount = inferredDiscount >= 0
    ? inferredDiscount
    : Math.max(fallbackDiscount, 0)

  return {
    items,
    itemsSubtotal,
    shippingFee,
    discountAmount,
    finalTotal,
  }
}

// Keep list row totals stable and avoid post-click value jumps.

const openSummary = async (hd) => {
  openMoreMenuId.value = null
  summaryLoading.value = true
  summaryImageErrors.value = {}
  summaryModal.value = {
    ...hd,
    items: [],
    phiVanChuyen: 0,
    tongTien: 0,
    finalTotal: Number(hd?.thanhTien || 0),
    giamGia: 0,
    voucherLabels: [],
    campaignLabels: []
  }
  try {
    const [res] = await Promise.all([getHoaDonById(hd.id), loadVariantCatalog(), loadCustomerCatalog()])
    const detail = res?.data || {}
    const { items, itemsSubtotal, shippingFee, discountAmount, finalTotal } = computeCorrectedTotalsFromDetail(detail, hd)
    const rawItems = Array.isArray(detail?.items) ? detail.items
      : Array.isArray(detail?.hoaDonChiTiets) ? detail.hoaDonChiTiets
      : Array.isArray(detail?.chiTietHoaDons) ? detail.chiTietHoaDons
      : []
    const voucherLabels = collectSummaryAppliedVouchers(detail, hd, rawItems)
    const campaignLabels = collectSummaryAppliedCampaigns(rawItems)
    const customerId = Number(detail?.khachHangId || detail?.customerId || hd?.khachHangId || 0)
    const customer = customerCatalog.value.get(customerId)
    let resolvedEmail = firstValidEmailValue(
      detail?.emailNhanHang,
      detail?.email,
      detail?.customerEmail,
      detail?.khachHang?.email,
      detail?.khachHang?.taiKhoan?.email,
      detail?.khachHang?.taiKhoanEmail,
      hd?.emailNhanHang,
      hd?.email,
      customer?.email,
      customer?.taiKhoan?.email,
      customer?.taiKhoanEmail,
      customer?.tenDangNhap,
      customer?.taiKhoan?.tenDangNhap,
    )

    if (!resolvedEmail && customerId > 0) {
      try {
        const customerRes = await getKhachHangById(customerId)
        const customerDetail = customerRes?.data || {}
        resolvedEmail = firstValidEmailValue(
          customerDetail?.email,
          customerDetail?.taiKhoan?.email,
          customerDetail?.taiKhoanEmail,
          customerDetail?.tenDangNhap,
          customerDetail?.taiKhoan?.tenDangNhap,
        )
      } catch {
        // Keep summary usable even if customer detail API fails.
      }
    }
    summaryModal.value = {
      ...hd,
      ...detail,
      items,
      resolvedEmail,
      phiVanChuyen: shippingFee,
      tongTien: itemsSubtotal,
      finalTotal,
      giamGia: discountAmount,
      voucherLabels,
      campaignLabels,
    }
  } catch (e) {
    console.error("Failed to load order detail for summary:", e)
  } finally {
    summaryLoading.value = false
  }
}

const closeSummary = () => { summaryModal.value = null }

const hydrateCorrectedTotalForRow = async (hd) => {
  const id = Number(hd?.id || 0)
  if (!id) return
  if (Number(correctedRowTotalsById.value?.[id] || 0) > 0) return
  if (hydratingRowTotalsById.value[id]) return

  hydratingRowTotalsById.value = { ...hydratingRowTotalsById.value, [id]: true }
  try {
    const res = await getHoaDonById(id)
    const detail = res?.data || {}
    const { finalTotal } = computeCorrectedTotalsFromDetail(detail, hd)
    if (Number(finalTotal || 0) > 0) {
      correctedRowTotalsById.value = {
        ...correctedRowTotalsById.value,
        [id]: Number(finalTotal)
      }
    }
  } catch {
    // ignore per-row correction errors
  } finally {
    const next = { ...hydratingRowTotalsById.value }
    delete next[id]
    hydratingRowTotalsById.value = next
  }
}

watch(
  paginatedData,
  (rows) => {
    ;(rows || []).forEach((row) => {
      hydrateCorrectedTotalForRow(row)
    })
  },
  { immediate: true }
)

const goToFullDetail = (id) => {
  summaryModal.value = null
  router.push({
    path: `${panelBasePath.value}/hoa-don/detail/${id}`,
    query: currentPage.value > 1 ? { page: String(currentPage.value) } : {}
  })
}

// ═══ Send lookup mail from list ═══
const sendingMailForId = ref(null)

const resolveEmailForLookupMail = async (hd) => {
  await loadCustomerCatalog()
  const customerId = Number(hd?.khachHangId || hd?.customerId || 0)
  const customer = customerCatalog.value.get(customerId)

  const emailFromRow = firstValidEmailValue(
    hd?.emailNhanHang,
    hd?.email,
    hd?.customerEmail,
    hd?.emailKhachHang,
    hd?.emailNguoiNhan,
    customer?.email,
    customer?.taiKhoan?.email,
    customer?.taiKhoanEmail,
  )

  if (emailFromRow) return emailFromRow

  if (!hd?.id) return ""

  try {
    const detailRes = await getHoaDonById(hd.id)
    const detail = detailRes?.data || {}
    const row = detail?.hoaDon || detail || {}
    const detailCustomer = detail?.customer || row?.khachHang || customer || {}

    return firstValidEmailValue(
      row?.emailNhanHang,
      row?.email,
      row?.customerEmail,
      row?.emailKhachHang,
      row?.emailNguoiNhan,
      detailCustomer?.email,
      detailCustomer?.taiKhoan?.email,
      detailCustomer?.taiKhoanEmail,
      detail?.khachHang?.email,
      detail?.khachHang?.taiKhoan?.email,
      detail?.khachHang?.taiKhoanEmail,
    )
  } catch {
    return ""
  }
}

const sendLookupMailFromList = async (hd) => {
  openMoreMenuId.value = null
  const maHoaDon = String(hd?.maHoaDon || "").trim()
  const soDienThoai = String(hd?.soDienThoaiNhanHang || "").trim()

  if (!maHoaDon) { showToast("Thiếu mã đơn hàng", "warning"); return }
  const email = await resolveEmailForLookupMail(hd)
  if (!email) { showToast("Không tìm thấy email khách hàng để gửi", "warning"); return }

  sendingMailForId.value = hd.id
  try {
    const trackingUrl = buildOrderLookupTrackingUrl({ maHoaDon })
    const payload = { maHoaDon, email }
    if (soDienThoai) payload.soDienThoai = soDienThoai
    if (trackingUrl) payload.trackingUrl = trackingUrl
    const response = await sendOrderLookupMail(payload)
    showToast(response?.data?.message || "Đã gửi mail tra cứu", "success")
  } catch (error) {
    showToast(error?.response?.data?.message || "Không thể gửi mail tra cứu", "error")
  } finally {
    sendingMailForId.value = null
  }
}
</script>

<template>
  <main class="invoice-page">
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
          <small class="realtime-badge" :class="`is-${realtimeConnectionTone}`">Dữ liệu: {{ realtimeConnectionLabel }}</small>
        </div>
        <div style="display:flex; gap:8px;">
        </div>
      </div>

      <div class="body">
        <!-- FILTERS & SEARCH -->
        <div class="invoice-filters-container">
          <!-- TOP ROW: SEARCH + QUICK FILTERS -->
          <div class="invoice-filters-top">
            <div class="search">
              <Search :size="18" style="color: #9ca3af;" />
              <input
                v-model="searchText"
                type="text"
                placeholder="Tìm theo mã hóa đơn, khách hàng, SĐT, nhân viên..."
                style="flex: 1; border: none; outline: none; background: transparent;"
              />
            </div>

            <div class="quick-filters">
              <select v-model="filterStatus" class="invoice-filter-select">
                <option v-for="status in statusOptions" :key="status">{{ status }}</option>
              </select>

              <select v-model="filterType" class="invoice-filter-select">
                <option>Tất cả loại</option>
                <option>Tại quầy</option>
                <option>Trực tuyến</option>
              </select>
            </div>
          </div>

          <!-- DATE FILTER SECTION (EXPANDED) -->
          <div class="date-filter-expanded">
            <div class="date-filter-header"></div>

            <div class="date-filter-body">
              <div class="date-filter-inputs-wrapper">
                <div class="date-filter-inputs">
                  <label class="date-filter-field date-filter-field--with-icon">
                    <span>Từ ngày</span>
                    <input v-model="filterDateFrom" type="date" />
                  </label>
                  <label class="date-filter-field">
                    <span>Đến ngày</span>
                    <input v-model="filterDateTo" type="date" />
                  </label>
                </div>
              </div>

              <div class="date-filter-presets-wrapper">
                <span class="presets-label">Lọc nhanh</span>
                <div class="date-filter-presets">
                  <button type="button" :class="{ active: activeDatePreset === 'today' }" @click="applyDatePreset('today')">Hôm nay</button>
                  <button type="button" :class="{ active: activeDatePreset === 'last7' }" @click="applyDatePreset('last7')">7 ngày</button>
                  <button type="button" :class="{ active: activeDatePreset === 'last30' }" @click="applyDatePreset('last30')">30 ngày</button>
                  <button type="button" :class="{ active: activeDatePreset === 'thisMonth' }" @click="applyDatePreset('thisMonth')">Tháng này</button>
                  <button type="button" :class="{ active: activeDatePreset === 'lastMonth' }" @click="applyDatePreset('lastMonth')">Tháng trước</button>
                  <button class="date-filter-clear date-filter-clear-inline" type="button" @click="clearDateFilters" :disabled="!filterDateFrom && !filterDateTo">
                    Xóa lọc
                  </button>
                </div>
              </div>

              <div v-if="isDateRangeInvalid" class="date-filter-warning">
                ⚠️ Khoảng ngày chưa đúng, hệ thống sẽ tự đổi thứ tự khi lọc.
              </div>

            </div>
          </div>
        </div>

        <!-- TABLE -->
        <div class="table-responsive-wrapper">
          <table class="table" style="width: 100%;">
            <thead>
              <tr>
                <th style="min-width: 270px; text-align: left;">Đơn hàng</th>
                <th style="min-width: 240px;">Khách hàng</th>
                <th class="value-head" style="min-width: 200px;">Giá trị</th>
                <th class="status-head" style="min-width: 220px;">Trạng thái</th>
                <th style="min-width: 200px; text-align: right;">Thao tác</th>
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
                    <div class="money-total">{{ formatCurrency(getRowDisplayTotal(hd)) }}</div>
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
                    <button class="action-btn" @click="openSummary(hd)" title="Xem chi tiết">
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

                    <div class="row-more-wrap">
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

                        <button
                          class="menu-action mail-action"
                          type="button"
                          :disabled="sendingMailForId === hd.id"
                          @click="sendLookupMailFromList(hd)"
                        >
                          <Mail :size="14" />
                          <span>{{ sendingMailForId === hd.id ? "Đang gửi..." : "Gửi mail tra cứu" }}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- PAGINATION -->
        <div class="pagination-container">
          <div class="pagination-info">
            Hiển thị {{ paginatedData.length === 0 ? 0 : (currentPage - 1) * pageSize + 1 }} – {{ Math.min(currentPage * pageSize, filteredData.length) }} / {{ filteredData.length }}
          </div>
          <div class="pagination-controls">
            <button
              class="pagination-btn"
              @click="currentPage--"
              :disabled="currentPage === 1"
            >
              ← Trước
            </button>
            <div class="pagination-page-indicator">
              {{ currentPage }}
            </div>
            <button
              class="pagination-btn"
              @click="currentPage++"
              :disabled="currentPage >= totalPages"
            >
              Sau →
            </button>
          </div>
        </div>
      </div>
    </div>
  </main>

  <!-- Summary Modal -->
  <Teleport to="body">
    <Transition name="summary-fade">
      <div v-if="summaryModal" class="summary-overlay" @click.self="closeSummary">
        <div class="summary-box">
          <div class="summary-head">
            <div>
              <h2>{{ formatMaHoaDon(summaryModal.maHoaDon) || `#${summaryModal.id}` }}</h2>
              <p class="summary-subhead">Tóm tắt nhanh đơn hàng theo phong cách dashboard</p>
            </div>
            <button class="summary-close" @click="closeSummary"><X :size="18" /></button>
          </div>

          <div v-if="summaryLoading" class="summary-loading">
            <div class="summary-skeleton summary-skeleton-title"></div>
            <div class="summary-skeleton-pills">
              <div class="summary-skeleton summary-skeleton-pill"></div>
              <div class="summary-skeleton summary-skeleton-pill"></div>
              <div class="summary-skeleton summary-skeleton-pill"></div>
            </div>
            <div class="summary-skeleton-grid">
              <div class="summary-skeleton summary-skeleton-card" v-for="n in 6" :key="n"></div>
            </div>
            <div class="summary-skeleton-list">
              <div class="summary-skeleton-row" v-for="n in 3" :key="`row-${n}`">
                <div class="summary-skeleton summary-skeleton-thumb"></div>
                <div class="summary-skeleton-col">
                  <div class="summary-skeleton summary-skeleton-line lg"></div>
                  <div class="summary-skeleton summary-skeleton-line md"></div>
                </div>
                <div class="summary-skeleton summary-skeleton-line price"></div>
              </div>
            </div>
          </div>
          <div v-else class="summary-body">
            <div class="summary-top-meta">
              <span class="summary-meta-pill">Mã đơn: {{ summaryModal.maHoaDon || `#${summaryModal.id}` }}</span>
              <span class="summary-meta-pill">Nhân viên: {{ summaryModal.tenNhanVien || 'Chưa gán' }}</span>
              <span class="summary-meta-pill">Thanh toán: {{ getPaymentMethodLabel(summaryModal) }}</span>
            </div>

            <div v-if="summaryModal.voucherLabels?.length || summaryModal.campaignLabels?.length" class="summary-applied-wrap">
              <div v-if="summaryModal.voucherLabels?.length" class="summary-applied-row">
                <span class="summary-applied-label">Voucher áp dụng</span>
                <div class="summary-applied-tags">
                  <span v-for="voucher in summaryModal.voucherLabels" :key="voucher" class="summary-applied-tag">{{ voucher }}</span>
                </div>
              </div>
              <div v-if="summaryModal.campaignLabels?.length" class="summary-applied-row">
                <span class="summary-applied-label">Đợt giảm giá sản phẩm</span>
                <div class="summary-applied-tags">
                  <span v-for="campaign in summaryModal.campaignLabels" :key="campaign" class="summary-applied-tag summary-applied-tag--campaign">{{ campaign }}</span>
                </div>
              </div>
            </div>

            <div class="summary-info-grid">
              <div class="summary-field">
                <span class="summary-label"><ReceiptText :size="11" /> Trạng thái</span>
                <span class="status-badge" :class="`status-${getStatusColor(summaryModal.orderStatusName)}`">
                  {{ normalizeAdminStatusLabel(summaryModal.orderStatusName) }}
                </span>
              </div>
              <div class="summary-field">
                <span class="summary-label"><Package2 :size="11" /> Loại đơn</span>
                <span>{{ getOrderTypeLabel(summaryModal) }}</span>
              </div>
              <div class="summary-field">
                <span class="summary-label"><CalendarDays :size="11" /> Ngày tạo</span>
                <span>{{ formatDateTime(summaryModal.ngayTao) }}</span>
              </div>
              <div class="summary-field">
                <span class="summary-label"><ReceiptText :size="11" /> Mã hóa đơn hiển thị</span>
                <span>{{ formatMaHoaDon(summaryModal.maHoaDon) || `#${summaryModal.id}` }}</span>
              </div>
              <div class="summary-field">
                <span class="summary-label"><UserRound :size="11" /> Khách hàng</span>
                <span>{{ summaryModal.tenKhachHang || 'Khách lẻ' }}</span>
              </div>
              <div class="summary-field">
                <span class="summary-label"><Phone :size="11" /> Điện thoại</span>
                <span>{{ summaryModal.soDienThoaiNhanHang || '—' }}</span>
              </div>
              <div class="summary-field">
                <span class="summary-label"><Mail :size="11" /> Email</span>
                <span>{{ summaryModal.resolvedEmail || summaryModal.emailNhanHang || summaryModal.email || '—' }}</span>
              </div>
              <div class="summary-field">
                <span class="summary-label"><MapPin :size="11" /> Địa chỉ</span>
                <span class="summary-value multiline">{{ summaryModal.diaChiNhanHang || '—' }}</span>
              </div>
            </div>

            <div v-if="summaryModal.items?.length" class="summary-items">
              <div class="summary-section-title">Sản phẩm</div>
              <div class="summary-item" v-for="(item, idx) in summaryModal.items" :key="idx">
                <div class="summary-item-thumb">
                  <img
                    v-if="getSummaryItemImageSrc(item, idx)"
                    class="summary-item-img"
                    :src="getSummaryItemImageSrc(item, idx)"
                    @error="markSummaryImageError(item, idx)"
                  />
                  <span v-else class="summary-item-initials">{{ (item._tenHienThi || '?')[0] }}</span>
                </div>
                <div class="summary-item-body">
                  <div class="summary-item-name">
                    {{ item._tenHienThi }}
                    <span v-if="item._mauSac || item._kichThuoc" class="summary-item-variant">
                      {{ [item._mauSac, item._kichThuoc].filter(Boolean).join(' / ') }}
                    </span>
                  </div>
                  <div v-if="item._maSanPhamChiTiet" class="summary-item-code">{{ item._maSanPhamChiTiet }}</div>
                  <div class="summary-item-meta">x{{ item.soLuong || 0 }} × {{ formatCurrency(item._giaBan) }}</div>
                  <div v-if="item._itemVoucherLabels?.length || item._itemCampaignLabels?.length" class="summary-item-promos">
                    <span
                      v-for="(label, promoIdx) in item._itemVoucherLabels || []"
                      :key="`voucher-${idx}-${promoIdx}-${label}`"
                      class="summary-item-promo summary-item-promo--voucher"
                    >
                      Voucher: {{ label }}
                    </span>
                    <span
                      v-for="(label, promoIdx) in item._itemCampaignLabels || []"
                      :key="`campaign-${idx}-${promoIdx}-${label}`"
                      class="summary-item-promo summary-item-promo--campaign"
                    >
                      Campaign: {{ label }}
                    </span>
                  </div>
                </div>
                <div class="summary-item-price">{{ formatCurrency(item._thanhTien) }}</div>
              </div>
            </div>

            <div class="summary-totals">
              <div class="summary-total-row" v-if="(summaryModal.phiVanChuyen > 0) || (summaryModal.giamGia > 0)">
                <span>Tạm tính</span>
                <span>{{ formatCurrency(summaryItemsSubtotal) }}</span>
              </div>
              <div class="summary-total-row" v-if="summaryModal.phiVanChuyen > 0">
                <span>Phí vận chuyển</span>
                <span>{{ formatCurrency(summaryModal.phiVanChuyen) }}</span>
              </div>
              <div class="summary-total-row" v-if="summaryModal.giamGia > 0">
                <span>Giảm giá</span>
                <span class="discount-amount">-{{ formatCurrency(summaryModal.giamGia) }}</span>
              </div>
              <div class="summary-total-row summary-grand-total">
                <span>Tổng cộng</span>
                <span>{{ formatCurrency(summaryModal.finalTotal || summaryModal.tongTien) }}</span>
              </div>
            </div>
          </div>

          <div class="summary-footer">
            <button class="summary-detail-btn" @click="goToFullDetail(summaryModal.id)">
              Xem chi tiết đầy đủ →
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.invoice-page {
  width: 100%;
}

/* FILTERS CONTAINER */
.invoice-filters-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #f8f9fb 0%, #f3f4f7 100%);
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
}

.invoice-filters-top {
  display: flex;
  gap: 12px;
  align-items: stretch;
  flex-wrap: wrap;
}

.search {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 15px;
  border: 1px solid #cbd5e1;
  background: white;
  border-radius: 10px;
  flex: 1;
  min-width: 300px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.2s;
}

.search:focus-within {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.12);
}

.quick-filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.invoice-filter-select {
  padding: 11px 36px 11px 13px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  min-width: 180px;
  font-size: 14px;
  background: white url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23475569' d='M10.293 3.293L6 7.586 1.707 3.293A1 1 0 00.293 4.707l5 5a1 1 0 001.414 0l5-5a1 1 0 10-1.414-1.414z'/%3E%3C/svg%3E") no-repeat right 10px center;
  cursor: pointer;
  font-weight: 500;
  color: #334155;
  appearance: none;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.invoice-filter-select:hover {
  border-color: #94a3b8;
  background-color: #f8fafc;
}

.invoice-filter-select:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.12);
}

/* DATE FILTER EXPANDED */
.date-filter-expanded {
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-top: 1px solid #e2e8f0;
  padding-top: 8px;
}

.date-filter-header {
  display: none;
}

.date-filter-clear {
  border: 1px solid #dc2626;
  background: #fff1f2;
  color: #991b1b;
  font-size: 13px;
  border-radius: 8px;
  padding: 8px 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
}

.date-filter-clear:hover:not(:disabled) {
  border-color: #991b1b;
  background: #fee2e2;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(153, 27, 27, 0.15);
}

.date-filter-clear:disabled {
  color: #dc2626;
  background: #fff5f5;
  border-color: #fca5a5;
  opacity: 1;
  cursor: not-allowed;
}

.date-filter-body {
  display: grid;
  grid-template-columns: minmax(280px, 450px) 1fr;
  gap: 12px;
  align-items: stretch;
}

.date-filter-inputs-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.date-filter-presets-wrapper {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.presets-label {
  font-size: 13px;
  color: #475569;
  font-weight: 600;
  line-height: 1.1;
  min-height: 18px;
}

.date-filter-inputs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.date-filter-field--with-icon span::before {
  content: "";
  display: inline-block;
  width: 18px;
  height: 18px;
  margin-right: 8px;
  vertical-align: -3px;
  background: center / contain no-repeat url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='%23dc2626' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Crect x='3' y='4' width='18' height='18' rx='2' ry='2'/%3E%3Cline x1='16' y1='2' x2='16' y2='6'/%3E%3Cline x1='8' y1='2' x2='8' y2='6'/%3E%3Cline x1='3' y1='10' x2='21' y2='10'/%3E%3C/svg%3E");
}

.date-filter-field {
  display: grid;
  gap: 6px;
}

.date-filter-field span {
  font-size: 14px;
  color: #475569;
  font-weight: 600;
}

.date-filter-field input {
  padding: 11px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 9px;
  font-size: 15px;
  background: white;
  color: #1e293b;
  font-weight: 500;
  transition: all 0.2s;
  cursor: pointer;
}

.date-filter-field input:hover {
  border-color: #94a3b8;
  background: #f8fafc;
}

.date-filter-field input:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.12);
}

.date-filter-presets {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  gap: 8px;
}

.date-filter-presets button {
  border: 1.5px solid #cbd5e1;
  background: white;
  color: #475569;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  padding: 11px 14px;
  line-height: 1.3;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: center;
  white-space: nowrap;
  min-width: fit-content;
}

.date-filter-presets button:hover {
  border-color: #6366f1;
  background: #eef2ff;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.12);
}

.date-filter-presets button.active {
  border-color: #dc2626;
  background: #fff1f2;
  color: #991b1b;
  box-shadow: 0 2px 8px rgba(153, 27, 27, 0.15);
  font-weight: 800;
}

.date-filter-warning {
  grid-column: 1 / -1;
  background: #fef3c7;
  border: 1px solid #fcd34d;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 12px;
  color: #92400e;
  font-weight: 500;
  margin-top: 2px;
}

.date-filter-clear-inline {
  margin-left: 12px;
}

@media (max-width: 1080px) {
  .date-filter-body {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}


.date-filter-warning {
  color: #b45309;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 8px;
}

.realtime-badge {
  display: inline-flex;
  align-items: center;
  margin-top: 8px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid #e2e8f0;
  color: #475569;
  background: #f8fafc;
}

.realtime-badge.is-online {
  border-color: #86efac;
  background: #dcfce7;
  color: #166534;
}

.table {
  width: 100%;
  display: table;
  overflow-x: visible;
  max-width: none;
  border-collapse: collapse;
  font-size: 15px;
}

.table thead {
  background: linear-gradient(135deg, #f8f9fb 0%, #f0f1f4 100%);
  border-bottom: 2px solid #e2e8f0;
}

.table th {
  position: sticky;
  top: 0;
  z-index: 3;
  padding: 14px 16px;
  text-align: left;
  font-weight: 700;
  color: #334155;
  font-size: 12px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  background: linear-gradient(135deg, #f8f9fb 0%, #f0f1f4 100%);
  border-bottom: 2px solid #e2e8f0;
}

.table td {
  padding: 16px 16px;
  color: var(--text);
  vertical-align: top;
  border-bottom: 1px solid #f1f5f9;
}

.table tbody tr {
  transition: background-color 0.2s ease;
}

.table tbody tr:nth-child(even) td {
  background: #f8fafc;
}

.table tbody tr:hover td {
  background: #eff6ff;
}

.table tbody tr:last-child td {
  border-bottom: none;
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
  padding: 6px 11px;
  border-radius: 18px;
  font-size: 12.5px;
  font-weight: 700;
  border: 0.75px solid;
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

.table-responsive-wrapper {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  margin: 0;
  padding: 0;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 28px;
  padding: 18px;
  border-top: 1px solid #e2e8f0;
  border-bottom-left-radius: 12px;
  border-bottom-right-radius: 12px;
  background: linear-gradient(135deg, #f8f9fb 0%, #f0f1f4 100%);
  flex-wrap: wrap;
  gap: 16px;
}

.pagination-info {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-btn {
  border: 1px solid #cbd5e1;
  background: white;
  color: #334155;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 700;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.pagination-btn:hover:not(:disabled) {
  border-color: #6366f1;
  background: white;
  color: #6366f1;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.12);
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-page-indicator {
  padding: 8px 16px;
  background: white;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  min-width: 48px;
  text-align: center;
  font-weight: 700;
  font-size: 13px;
  color: #334155;
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

/* ═══ Mail action in dropdown ═══ */
.mail-action {
  display: flex;
  align-items: center;
  gap: 6px;
  border-top: 1px solid #f0f0f0;
  margin-top: 4px;
  padding-top: 8px;
}

/* ═══ Summary Modal ═══ */
.summary-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15,23,42,.5);
  backdrop-filter: blur(4px);
  display: grid;
  place-items: center;
  z-index: 9999;
  padding: 20px;
}
.summary-box {
  background: #fff;
  border-radius: 18px;
  max-width: 720px;
  width: 100%;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 50px rgba(0,0,0,.18);
  overflow: hidden;
  --sidebar-surface:
    radial-gradient(circle at 14% 10%, rgba(239, 68, 68, 0.2), transparent 44%),
    linear-gradient(165deg, #0a0a0f 0%, #17161e 46%, #07070b 100%);
}
.summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 22px;
  border-bottom: 1px solid rgba(255,255,255,0.12);
  background: var(--sidebar-surface);
  position: relative;
  isolation: isolate;
}
.summary-head::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.25;
  z-index: 0;
  background-image:
    repeating-linear-gradient(45deg, rgba(255, 255, 255, 0.07) 0 1px, transparent 1px 18px),
    repeating-linear-gradient(-45deg, rgba(255, 255, 255, 0.05) 0 1px, transparent 1px 18px);
}
.summary-head > * {
  position: relative;
  z-index: 1;
}
.summary-head h2 {
  margin: 0;
  font-size: 30px;
  letter-spacing: -0.02em;
  font-weight: 900;
  color: #f8fafc;
}
.summary-subhead {
  margin: 4px 0 0;
  font-size: 12px;
  font-weight: 600;
  color: rgba(241, 245, 249, 0.72);
}
.summary-close {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
  color: #f8fafc;
  display: grid;
  place-items: center;
  cursor: pointer;
}
.summary-close:hover { background: rgba(255, 255, 255, 0.24); color: #ffffff; }

.summary-loading {
  padding: 20px 22px 24px;
  display: grid;
  gap: 14px;
}
.summary-skeleton {
  border-radius: 10px;
  background: linear-gradient(90deg, #f1f5f9 0%, #ffffff 50%, #f1f5f9 100%);
  background-size: 220% 100%;
  animation: summaryShimmer 1.3s linear infinite;
}
.summary-skeleton-title { height: 34px; width: 42%; }
.summary-skeleton-pills { display: flex; gap: 8px; flex-wrap: wrap; }
.summary-skeleton-pill { height: 34px; width: 180px; border-radius: 999px; }
.summary-skeleton-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.summary-skeleton-card { height: 74px; }
.summary-skeleton-list { display: grid; gap: 12px; }
.summary-skeleton-row { display: grid; grid-template-columns: 56px minmax(0, 1fr) 110px; gap: 12px; align-items: center; }
.summary-skeleton-thumb { width: 56px; height: 56px; }
.summary-skeleton-col { display: grid; gap: 8px; }
.summary-skeleton-line { height: 12px; }
.summary-skeleton-line.lg { width: 82%; }
.summary-skeleton-line.md { width: 56%; }
.summary-skeleton-line.price { width: 100px; justify-self: end; height: 22px; }

@keyframes summaryShimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -20% 0; }
}

.summary-body {
  overflow-y: auto;
  padding: 16px 22px;
}

.summary-top-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.summary-applied-wrap {
  margin-top: 12px;
  margin-bottom: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 10px;
  background: #f8fafc;
  display: grid;
  gap: 8px;
}

.summary-applied-row {
  display: grid;
  gap: 6px;
}

.summary-applied-label {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.summary-applied-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.summary-applied-tag {
  border: 1px solid #fecaca;
  background: #fff1f2;
  color: #b91c1c;
  border-radius: 999px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 700;
}

.summary-applied-tag--campaign {
  border-color: #fed7aa;
  background: #fff7ed;
  color: #c2410c;
}

.summary-meta-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 11px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.summary-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 20px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fcfdff;
}
.summary-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
  min-width: 0;
}
.summary-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10px;
  font-weight: 700;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: .035em;
}
.summary-field .status-badge {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  margin-top: 2px;
}
.summary-field > span:last-child {
  font-size: 14px;
  font-weight: 600;
  color: #111;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.summary-value.multiline {
  white-space: normal !important;
  overflow: visible !important;
  text-overflow: clip !important;
  overflow-wrap: anywhere;
  word-break: break-word;
  line-height: 1.35;
}

.summary-section-title {
  font-size: 12px;
  font-weight: 800;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: .04em;
  margin-top: 18px;
  margin-bottom: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.summary-items {
  margin-bottom: 4px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}
.summary-item-img {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  object-fit: contain;
  flex-shrink: 0;
  background: #f3f4f6;
}
.summary-item-thumb {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  flex-shrink: 0;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.summary-item-initials {
  font-size: 18px;
  font-weight: 800;
  color: #9ca3af;
  text-transform: uppercase;
}
.summary-item-body {
  flex: 1;
  min-width: 0;
}
.summary-item-name {
  flex: 1;
  font-size: 14px;
  font-weight: 700;
  color: #111;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.summary-item-code {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 700;
  color: #94a3b8;
}
.summary-item-variant {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 500;
  margin-left: 4px;
}
.summary-item-meta {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}
.summary-item-promos {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}
.summary-item-promo {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 700;
  line-height: 1.2;
}
.summary-item-promo--voucher {
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #9a3412;
}
.summary-item-promo--campaign {
  background: #ecfeff;
  border: 1px solid #a5f3fc;
  color: #155e75;
}
.summary-item-price {
  font-size: 22px;
  font-weight: 900;
  letter-spacing: -0.02em;
  color: #dc2626;
  flex-shrink: 0;
}

.summary-totals {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: linear-gradient(180deg, #fff, #fcfcfd);
}
.summary-total-row {
  display: flex;
  justify-content: space-between;
  font-size: 13.5px;
  color: #374151;
  padding: 4px 0;
}
.discount-amount { color: #dc2626; }
.summary-grand-total {
  display: flex;
  align-items: center;
  font-size: 17px;
  font-weight: 800;
  color: #991b1b;
  padding: 9px 10px;
  margin-top: 6px;
  border-radius: 10px;
  background: linear-gradient(135deg, #fff1f2, #fee2e2);
  border: 1px solid #fca5a5;
}
.summary-grand-total span:last-child {
  color: #dc2626;
  font-size: 22px;
  font-weight: 900;
}

.summary-footer {
  padding: 16px 22px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: center;
  background: #fff;
  border-radius: 0 0 20px 20px;
}
.summary-detail-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 13px 0;
  width: 100%;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: #fff;
  font: inherit;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all .15s;
  box-shadow: 0 2px 8px rgba(220,38,38,.22);
}
.summary-detail-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(220,38,38,.35);
}

@media (max-width: 760px) {
  .summary-skeleton-grid,
  .summary-info-grid {
    grid-template-columns: 1fr;

  .invoice-filter-select,
  .invoice-filter-select--type,
  .date-filter-card {
    min-width: 100%;
    width: 100%;
  }

  .date-filter-inputs {
    grid-template-columns: 1fr;
  }
  }
  .summary-head h2 {
    font-size: 24px;
  }
  .summary-item-price {
    font-size: 18px;
  }
}

/* Summary modal transitions */
.summary-fade-enter-active { transition: opacity .2s ease; }
.summary-fade-leave-active { transition: opacity .15s ease; }
.summary-fade-enter-from, .summary-fade-leave-to { opacity: 0; }
</style>
