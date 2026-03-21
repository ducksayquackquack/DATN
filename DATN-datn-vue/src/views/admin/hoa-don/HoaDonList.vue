<script setup>
import { ref, computed, onMounted, watch } from "vue"
import { useRouter, useRoute } from "vue-router"
import { getAllHoaDon, updateHoaDon, updateHoaDonBySystemEvent } from "../../../services/hoaDonService"
import { getAllNhanVien } from "../../../services/nhanVienService"
import { getAllLichLamViecFull } from "../../../services/lichLamViecService"
import { Eye, Plus, Search } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel, normalizeOrderStatusCode } from "../../../utils/adminStatus"
import { hasPaymentFlowTag, PAYMENT_FLOW_TAGS } from "../../../utils/paymentWorkflow"
import { useToast } from "../../../composables/useToast"

const router = useRouter()
const route = useRoute()
const { showToast } = useToast()
const panelBasePath = computed(() => (route.path.startsWith('/employee/') ? '/employee' : '/admin'))

const hoaDons = ref([])
const loading = ref(false)
const apiWarning = ref("")
const searchText = ref("")
const filterStatus = ref("Tất cả trạng thái")
const currentPage = ref(1)
const pageSize = ref(10)
const cancelReasonById = ref({})
const rowSavingById = ref({})
const transferTargetById = ref({})
const activeShiftEmployees = ref([])
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
  window.addEventListener('focus', loadData)
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

  data.sort((a, b) => {
    const dateA = a?.ngayTao ? new Date(a.ngayTao).getTime() : (a?.id || 0)
    const dateB = b?.ngayTao ? new Date(b.ngayTao).getTime() : (b?.id || 0)
    return dateB - dateA
  })

  return data
})

const totalPages = computed(() =>
  Math.ceil(filteredData.value.length / pageSize.value)
)

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

const viewDetail = (id) => {
  router.push({
    path: `${panelBasePath.value}/hoa-don/detail/${id}`,
    query: currentPage.value > 1 ? { page: String(currentPage.value) } : {}
  })
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

const canCancelFromList = (hoaDon) => {
  const code = normalizeOrderStatusCode(hoaDon?.orderStatusCode, hoaDon?.orderStatusName, hoaDon?.statusNote)
  return code === "CHO_XAC_NHAN" || code === "CHO_LAY_HANG"
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

  rowSavingById.value[hoaDon.id] = true
  try {
    await updateHoaDon(hoaDon.id, {
      orderStatusCode: "HUY",
      statusNote: `Huỷ đơn: ${reason}`
    })

    cancelReasonById.value[hoaDon.id] = ""
    await loadData()
    showToast(`Đã hủy đơn ${hoaDon.maHoaDon || `#${hoaDon.id}`}`, "success")
  } catch (error) {
    console.error("Cancel from list failed:", error)
    apiWarning.value = error?.response?.data?.message || "Không thể hủy hóa đơn"
    showToast(apiWarning.value, "error")
  } finally {
    rowSavingById.value[hoaDon.id] = false
  }
}

const completeFromList = async (hoaDon) => {
  if (!hoaDon?.id || !canCompleteFromList(hoaDon)) return

  rowSavingById.value[hoaDon.id] = true
  try {
    const isPos = getOrderTypeLabel(hoaDon) === "Tại quầy"
    await updateHoaDonBySystemEvent(
      hoaDon.id,
      isPos ? "HOAN_TAT_POS" : "GIAO_HANG_THANH_CONG",
      isPos
        ? "Nhân viên xác nhận hoàn tất bán hàng tại quầy"
        : "Giao hàng thành công"
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
          <router-link class="btn" :to="{ path: `${panelBasePath}/hoa-don/pos` }">
            <span>Bán hàng tại quầy</span>
          </router-link>
          <router-link class="btn primary" :to="{ path: `${panelBasePath}/hoa-don/detail/create` }">
            <Plus :size="18" />
            <span>Thêm hoá đơn</span>
          </router-link>
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

          <select v-model="filterStatus" style="padding: 10px 12px; border: 1px solid var(--line); border-radius: 8px; min-width: 200px; font-size: 14px;">
            <option v-for="status in statusOptions" :key="status">{{ status }}</option>
          </select>
        </div>

        <!-- TABLE -->
        <div style="overflow-x: auto;">
          <table class="table" style="width: 100%;">
            <thead>
              <tr>
                <th style="width: 80px; text-align: left;">ID</th>
                <th style="width: 140px; text-transform: none;">Mã hóa đơn</th>
                <th>Nhân viên</th>
                <th>Khách hàng</th>
                <th style="width: 100px;">Phí ship</th>
                <th style="width: 130px; text-align: right;">Thành tiền</th>
                <th style="width: 100px;">Ngày tạo</th>
                <th style="width: 120px;">Loại đơn</th>
                <th style="width: 140px;">Trạng thái</th>
                <th style="width: 240px; text-align: left;">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="paginatedData.length === 0">
                <td colspan="10" style="text-align: center; padding: 32px;">
                  <div style="color: #9ca3af;">Chưa có dữ liệu</div>
                </td>
              </tr>
              <tr
                v-for="hd in paginatedData"
                :key="hd.id"
                :class="{ 'order-row-attention': shouldHighlightRow(hd) }"
                class="clickable-row"
                style="border-bottom: 1px solid var(--line); cursor: pointer;"
                @click.self="viewDetail(hd.id)"
              >
                <td style="font-weight: 600;" @click="viewDetail(hd.id)">{{ hd.id }}</td>
                <td style="font-weight: 600; color: var(--text);" @click="viewDetail(hd.id)">
                  <div class="invoice-code-cell">
                    <span class="invoice-code-link">{{ hd.maHoaDon }}</span>
                    <span v-if="shouldHighlightRow(hd)" class="row-attention-badge">Mới</span>
                  </div>
                </td>
                <td @click="viewDetail(hd.id)">{{ hd.tenNhanVien || "-" }}</td>
                <td @click="viewDetail(hd.id)">
                  <div>
                    <b>{{ hd.tenKhachHang || "Khách lẻ" }}</b>
                    <div style="font-size: 13px; color: #9ca3af;">{{ hd.soDienThoaiNhanHang }}</div>
                  </div>
                </td>
                <td @click="viewDetail(hd.id)">{{ formatCurrency(hd.phiShip) }}</td>
                <td style="text-align: right; font-weight: 600;" @click="viewDetail(hd.id)">{{ formatCurrency(hd.thanhTien) }}</td>
                <td style="color: #9ca3af; font-size: 13px;" @click="viewDetail(hd.id)">{{ formatDate(hd.ngayTao) }}</td>
                <td @click="viewDetail(hd.id)">{{ getOrderTypeLabel(hd) }}</td>
                <td @click="viewDetail(hd.id)">
                  <span
                    class="status-badge"
                    :class="`status-${getStatusColor(hd.orderStatusName)}`"
                  >
                    {{ normalizeAdminStatusLabel(hd.orderStatusName) }}
                  </span>
                </td>
                <td>
                  <div class="action-stack">
                    <button class="action-btn" @click="viewDetail(hd.id)" title="Xem chi tiết">
                      <Eye :size="18" />
                      <span>Chi tiết</span>
                    </button>

                    <button
                      v-if="canCompleteFromList(hd)"
                      class="action-btn complete"
                      type="button"
                      :disabled="rowSavingById[hd.id]"
                      @click="completeFromList(hd)"
                    >
                      <span>{{ rowSavingById[hd.id] ? "Đang cập nhật" : getCompleteButtonLabel(hd) }}</span>
                    </button>

                    <div v-if="canTransferFromList(hd)" class="transfer-inline">
                      <select
                        v-model="transferTargetById[hd.id]"
                        class="transfer-select"
                        :disabled="rowSavingById[hd.id]"
                      >
                        <option value="">Chọn NV đang trực...</option>
                        <option
                          v-for="emp in getTransferCandidates(hd)"
                          :key="`transfer-${hd.id}-${emp.id}`"
                          :value="emp.id"
                        >
                          {{ emp.tenNhanVien }} ({{ emp.gioBatDau }}-{{ emp.gioKetThuc }})
                        </option>
                      </select>
                      <button
                        class="transfer-btn"
                        type="button"
                        :disabled="rowSavingById[hd.id]"
                        :title="getTransferTargetName(hd) ? `Chuyển cho ${getTransferTargetName(hd)}` : 'Chọn nhân viên nhận đơn'"
                        @click="transferFromList(hd)"
                      >
                        {{ rowSavingById[hd.id] ? "Đang chuyển" : "Chuyển xử lý" }}
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
  font-size: 14px;
}

.table thead {
  background: var(--line-light);
  border-bottom: 2px solid var(--line);
}

.table th {
  padding: 12px 14px;
  text-align: left;
  font-weight: 600;
  color: var(--text);
  font-size: 12px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.table td {
  padding: 14px;
  color: var(--text);
}

.order-row-attention td {
  background: linear-gradient(90deg, #fff3f3 0%, #fff8f8 100%);
}

.order-row-attention td:first-child {
  box-shadow: inset 4px 0 0 #f87171;
}

.clickable-row:hover td {
  background: #f9f5f6;
}

.invoice-code-link {
  color: #c5162d;
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 3px;
}

.invoice-code-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
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
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid;
  white-space: nowrap;
}

.status-success {
  background: #dcfce7;
  border-color: #86efac;
  color: #166534;
}

.status-danger {
  background: #fee2e2;
  border-color: #fca5a5;
  color: #991b1b;
}

.status-warning {
  background: #fef3c7;
  border-color: #fcd34d;
  color: #92400e;
}

.status-neutral {
  background: #f3f4f6;
  border-color: #d1d5db;
  color: #374151;
}

.action-btn {
  background: transparent;
  border: 1px solid var(--line);
  padding: 6px 10px;
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

.action-btn.complete {
  border-color: #22c55e;
  background: #f0fdf4;
  color: #166534;
}

.action-btn.complete:hover {
  background: #dcfce7;
  color: #166534;
}

.action-stack {
  display: grid;
  gap: 8px;
}

.cancel-inline {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 6px;
}

.transfer-inline {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 6px;
}

.transfer-select {
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 6px 8px;
  font-size: 12px;
  background: white;
}

.transfer-btn {
  border: 1px solid #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: 6px;
  font-size: 12px;
  padding: 6px 8px;
  cursor: pointer;
  white-space: nowrap;
}

.transfer-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-input {
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 6px 8px;
  font-size: 12px;
}

.cancel-btn {
  border: 1px solid #fca5a5;
  background: #fff1f2;
  color: #b91c1c;
  border-radius: 6px;
  font-size: 12px;
  padding: 6px 8px;
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
  border: 1px solid #fdba74;
  background: #fff7ed;
  color: #92400e;
}

.payment-warning {
  margin: 14px 20px 0;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #f59e0b;
  background: #fffbeb;
  color: #92400e;
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
  background: #fef3c7;
  border: 1px solid #fcd34d;
  border-radius: 8px;
  text-decoration: none;
  color: #78350f;
  transition: background 0.15s ease;
}

.payment-warning-link:hover {
  background: #fde68a;
  border-color: #f59e0b;
}

.payment-warning-code {
  font-weight: 700;
  font-size: 13px;
  color: #b45309;
  font-family: monospace;
}

.payment-warning-customer {
  font-weight: 600;
  font-size: 13px;
}

.payment-warning-phone {
  font-size: 13px;
  color: #92400e;
}

.payment-warning-amount {
  font-size: 13px;
  font-weight: 600;
  color: #c2410c;
}

.payment-warning-sep {
  color: #d97706;
  font-size: 12px;
}

.payment-warning-arrow {
  margin-left: auto;
  font-size: 12px;
  font-weight: 600;
  color: #b45309;
  white-space: nowrap;
  text-decoration: underline;
}

@media (max-width: 768px) {
  .search {
    min-width: 100%;
  }
  
  .table th,
  .table td {
    padding: 10px 8px;
    font-size: 12px;
  }
}
</style>
