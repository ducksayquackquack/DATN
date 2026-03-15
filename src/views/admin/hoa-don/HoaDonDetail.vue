<script setup>
import { computed, onMounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import {
  addHoaDonItem,
  createHoaDon,
  deleteHoaDonItem,
  getHoaDonById,
  updateHoaDon,
  updateHoaDonBySystemEvent,
  updateHoaDonItemQty
} from "../../../services/hoaDonService"
import { getAllKhachHang } from "../../../services/KhachHangService"
import { getAllNhanVien } from "../../../services/nhanVienService"
import { getAllSanPham } from "../../../services/sanPhamService"
import VoucherSelector from "../../../components/voucher/VoucherSelector.vue"
import { useConfirm } from "../../../composables/useConfirm"
import { useToast } from "../../../composables/useToast"
import { CheckCircle2, Loader2, Package2, Plus, Save, ShoppingBag, Ticket, Trash2, UserRound } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel, normalizeOrderStatusCode } from "../../../utils/adminStatus"
import { describePaymentFlowState } from "../../../utils/paymentWorkflow"

const router = useRouter()
const route = useRoute()
const { askConfirm } = useConfirm()
const { showToast } = useToast()
const panelBasePath = computed(() => (route.path.startsWith('/employee/') ? '/employee' : '/admin'))

const isCreate = computed(() => {
  return route.path.endsWith("/hoa-don/detail/create") || route.params.id === "create" || !route.params.id
})

const mode = computed(() => (isCreate.value ? "create" : "edit"))

const pageTitle = computed(() => {
  if (isCreate.value) return "Tạo hoá đơn mới"
  if (hoaDon.value.maHoaDon) return hoaDon.value.maHoaDon
  return hoaDon.value.id ? `Hoá đơn #${hoaDon.value.id}` : "Chi tiết hoá đơn"
})

const isLoading = ref(false)
const isSaving = ref(false)
const apiWarning = ref("")

const nhanVienList = ref([])
const khachHangList = ref([])
const sanPhamVariants = ref([])
const items = ref([])
const history = ref([])
const selectedVoucher = ref(null)
const manualStatusNote = ref("")


const hoaDon = ref({
  id: null,
  maHoaDon: "",
  nhanVienId: null,
  khachHangId: null,
  soDienThoaiNhanHang: "",
  diaChiNhanHang: "",
  ngayNhanHangDuKien: "",
  ngayNhanHangMongMuon: "",
  phiShip: 0,
  giaSauGiamGia: 0,
  thanhTien: 0,
  orderStatusCode: "CHO_XAC_NHAN",
  orderStatusName: "Chờ xác nhận",
  orderType: "ONLINE",
  phuongThucThanhToan: "COD",
  statusNote: "",
  fulfillmentStatusCode: "PENDING",
  fulfillmentStatusName: "Chờ xác nhận",
  businessClosureStatus: "OPEN",
  businessClosureStatusName: "Đang mở",
  finalOrder: false
})

const newItem = ref({
  spctId: null,
  soLuong: 1
})

const statusNameMap = {
  CHO_XAC_NHAN: "Chờ xác nhận",
  CHO_LAY_HANG: "Chờ lấy hàng",
  DANG_GIAO: "Đang giao",
  GIAO_THAT_BAI: "Giao thất bại",
  HOAN_VE: "Hoàn về",
  HOAN_THANH: "Hoàn thành",
  HUY: "Đã hủy"
}

const variantMap = computed(() => {
  return sanPhamVariants.value.reduce((accumulator, variant) => {
    accumulator.set(variant.spctId, variant)
    return accumulator
  }, new Map())
})

const subtotal = computed(() => {
  return items.value.reduce((sum, item) => sum + Number(item.thanhTien || 0), 0)
})

const grandTotal = computed(() => {
  const shipFee = String(hoaDon.value.orderType || "ONLINE").toUpperCase() === "POS"
    ? 0
    : Number(hoaDon.value.phiShip || 0)
  const total = subtotal.value + shipFee - Number(hoaDon.value.giaSauGiamGia || 0)
  return total > 0 ? total : 0
})

const isPosOrder = computed(() => String(hoaDon.value.orderType || "ONLINE").toUpperCase() === "POS")

const paymentMethodLabel = computed(() => {
  const code = String(hoaDon.value.phuongThucThanhToan || "").toUpperCase()
  if (code === "CASH") return "Tiền mặt tại quầy"
  if (code === "BANK") return "Chuyển khoản"
  if (code === "VNPAY") return "VNPay"
  if (code === "COD") return "COD"
  return hoaDon.value.phuongThucThanhToan || "COD"
})

const currentStatusName = computed(() => {
  const normalizedCode = normalizeOrderStatusCode(hoaDon.value.orderStatusCode, hoaDon.value.orderStatusName)
  const fallback = statusNameMap[normalizedCode] || "Chờ xác nhận"
  return normalizeAdminStatusLabel(hoaDon.value.orderStatusName || fallback)
})

const currentStatusTone = computed(() => getAdminStatusTone(currentStatusName.value))

const fulfillmentLabel = computed(() => {
  const explicit = String(hoaDon.value.fulfillmentStatusName || "").trim()
  if (explicit) return explicit
  return "Chờ xác nhận"
})

const businessClosureLabel = computed(() => {
  const explicit = String(hoaDon.value.businessClosureStatusName || "").trim()
  if (explicit) return explicit
  const code = String(hoaDon.value.businessClosureStatus || "OPEN").toUpperCase()
  return code === "CLOSED" ? "Đã chốt" : "Đang mở"
})

const selectedCustomer = computed(() => {
  return khachHangList.value.find((item) => Number(item?.id) === Number(hoaDon.value.khachHangId)) || null
})

const selectedEmployee = computed(() => {
  return nhanVienList.value.find((item) => Number(item?.id) === Number(hoaDon.value.nhanVienId)) || null
})

const selectedCustomerLabel = computed(() => {
  if (selectedCustomer.value?.tenKhachHang) return selectedCustomer.value.tenKhachHang
  if (hoaDon.value.khachHangId != null) return `KH #${hoaDon.value.khachHangId}`
  return "Chưa chọn"
})

const orderTypeLabel = computed(() => {
  const explicit = String(hoaDon.value.orderType || "").toUpperCase()
  if (explicit === "POS") return "Tại quầy"
  if (explicit === "ONLINE") return "Trực tuyến"

  const note = String(hoaDon.value.statusNote || "").toUpperCase()
  if (note.includes("[POS]")) return "Tại quầy"

  const deliveryAddress = String(hoaDon.value.diaChiNhanHang || "").trim().toLowerCase()
  if (deliveryAddress.includes("mua tại quầy") || deliveryAddress.includes("mua tai quay")) {
    return "Tại quầy"
  }

  return "Trực tuyến"
})

function backToList(refresh = false) {
  const query = {}
  if (refresh) query.refresh = "true"
  if (route.query.page) query.page = String(route.query.page)
  router.push({ path: `${panelBasePath.value}/hoa-don/list`, query })
}

const canEdit = computed(() => !hoaDon.value.finalOrder)

const paymentFlowState = computed(() => {
  if (isCreate.value) return null
  if (hoaDon.value.paymentFlowCode) {
    return {
      code: hoaDon.value.paymentFlowCode,
      label: hoaDon.value.paymentFlowLabel || "",
      tone: hoaDon.value.paymentFlowTone || "neutral"
    }
  }
  const note = hoaDon.value.statusNote || ""
  const pttt = String(hoaDon.value.phuongThucThanhToan || "").toUpperCase()
  if (!pttt.includes("VNPAY") && !note.toUpperCase().includes("VNPAY")) return null
  return describePaymentFlowState({ statusNote: note })
})

const canEmployeeConfirmPayment = computed(() =>
  paymentFlowState.value?.code === "WAIT_EMPLOYEE" && !hoaDon.value.finalOrder
)

const currentOrderCode = computed(() =>
  normalizeOrderStatusCode(hoaDon.value.orderStatusCode, hoaDon.value.orderStatusName)
)

const canConfirmOnlineOrder = computed(() =>
  !isCreate.value && !isPosOrder.value && currentOrderCode.value === "CHO_XAC_NHAN" && !hoaDon.value.finalOrder
)

const canStartShipping = computed(() =>
  !isCreate.value && !isPosOrder.value && currentOrderCode.value === "CHO_LAY_HANG" && !hoaDon.value.finalOrder
)

const canMarkReturned = computed(() =>
  !isCreate.value && currentOrderCode.value === "GIAO_THAT_BAI" && !hoaDon.value.finalOrder
)

const canQuickComplete = computed(() =>
  !isCreate.value
  && !hoaDon.value.finalOrder
  && (
    (isPosOrder.value && currentOrderCode.value === "CHO_LAY_HANG")
    || (!isPosOrder.value && currentOrderCode.value === "DANG_GIAO")
  )
)

function isHtmlPayload(payload) {
  return typeof payload === "string" && /<html|<!doctype/i.test(payload)
}

function extractApiErrorMessage(error, fallback = "Không thể cập nhật trạng thái") {
  const payload = error?.response?.data
  if (typeof payload === "string" && payload.trim()) return payload
  if (typeof payload?.message === "string" && payload.message.trim()) return payload.message
  if (typeof payload?.detail === "string" && payload.detail.trim()) return payload.detail
  if (typeof payload?.title === "string" && payload.title.trim()) return payload.title
  if (typeof error?.message === "string" && error.message.trim()) return error.message
  return fallback
}

function formatCurrency(value) {
  return new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + "₫"
}

function formatDateInput(value) {
  if (!value) return ""
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ""
  return date.toISOString().split("T")[0]
}

function formatDateTime(value) {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return "-"
  return `${String(date.getDate()).padStart(2, "0")}/${String(date.getMonth() + 1).padStart(2, "0")}/${date.getFullYear()} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`
}

function extractList(response) {
  if (isHtmlPayload(response?.data)) {
    throw new Error("API trả về trang đăng nhập thay vì JSON. Hãy khởi động lại backend sau khi áp dụng SecurityConfig.")
  }

  if (Array.isArray(response?.data)) return response.data
  if (Array.isArray(response?.data?.content)) return response.data.content
  return []
}

function buildVariantLabel(product, variant) {
  const parts = [product?.tenSanPham]
  if (variant?.kichThuoc?.tenKichThuoc) parts.push(`Size ${variant.kichThuoc.tenKichThuoc}`)
  if (variant?.mauSac?.tenMauSac) parts.push(variant.mauSac.tenMauSac)
  if (variant?.tenSanPhamChiTiet) parts.push(variant.tenSanPhamChiTiet)
  return parts.filter(Boolean).join(" • ")
}

function flattenVariants(products) {
  return products.flatMap((product) => {
    const variants = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []

    if (!variants.length) {
      return []
    }

    return variants.map((variant) => ({
      spctId: variant.id,
      productId: product.id,
      maSanPham: product.maSanPham || "",
      maSanPhamChiTiet: variant.ma || "",
      tenSanPhamChiTiet: buildVariantLabel(product, variant),
      giaBan: Number(variant.giaBan || 0),
      soLuongTon: Number(variant.soLuong || 0),
      kichThuoc: variant?.kichThuoc?.tenKichThuoc || "",
      mauSac: variant?.mauSac?.tenMauSac || ""
    }))
  })
}

function syncTotals() {
  hoaDon.value.thanhTien = grandTotal.value
}

function getVariant(spctId) {
  return variantMap.value.get(spctId) || null
}

function getItemName(item) {
  return item.tenSanPhamChiTiet || getVariant(item.spctId)?.tenSanPhamChiTiet || `SPCT #${item.spctId}`
}

function getItemCodeMeta(item) {
  const variant = getVariant(item?.spctId)
  const invoiceProductCode = String(item?.maSanPham || "").trim()
  const invoiceVariantCode = String(item?.maSanPhamChiTiet || "").trim()
  const catalogProductCode = String(variant?.maSanPham || "").trim()
  const catalogVariantCode = String(variant?.maSanPhamChiTiet || "").trim()

  const productCode = invoiceProductCode || catalogProductCode || "SP?"
  const variantCode = invoiceVariantCode || catalogVariantCode || `SPCT#${item?.spctId}`

  const isProductCodeMismatch = Boolean(invoiceProductCode && catalogProductCode)
    && invoiceProductCode.toUpperCase() !== catalogProductCode.toUpperCase()

  const isVariantCodeMismatch = Boolean(invoiceVariantCode && catalogVariantCode)
    && invoiceVariantCode.toUpperCase() !== catalogVariantCode.toUpperCase()

  return {
    productCode,
    variantCode,
    mismatch: isProductCodeMismatch || isVariantCodeMismatch,
    catalogProductCode,
    catalogVariantCode
  }
}

function applyDiscount(discount) {
  hoaDon.value.giaSauGiamGia = Number(discount || 0)
  syncTotals()
}

async function dispatchSystemEvent(eventCode, note, successMessage) {
  isSaving.value = true
  try {
    const response = await updateHoaDonBySystemEvent(hoaDon.value.id, eventCode, note)
    applyInvoiceDetail(response?.data)
    showToast(successMessage || "Đã cập nhật trạng thái đơn hàng", "success")
  } catch (error) {
    console.error("Dispatch system event failed:", error)
    showToast(extractApiErrorMessage(error), "error")
  } finally {
    isSaving.value = false
  }
}

async function confirmOnlineOrder() {
  if (!canConfirmOnlineOrder.value) return
  await dispatchSystemEvent(
    "XAC_NHAN_DON_HANG",
    "Nhân viên xác nhận đơn hàng",
    "Đã xác nhận đơn hàng"
  )
}

async function startShipping() {
  if (!canStartShipping.value) return
  await dispatchSystemEvent(
    "GIAO_HANG_BAT_DAU",
    "Đơn hàng đang được giao",
    "Đã chuyển đơn sang trạng thái đang giao"
  )
}

async function markReturned() {
  if (!canMarkReturned.value) return
  await dispatchSystemEvent(
    "GIAO_HANG_HOAN_VE",
    "Đơn hàng hoàn về sau khi giao thất bại",
    "Đã xác nhận đơn hoàn về"
  )
}

async function quickCompleteOrder() {
  if (!canQuickComplete.value) return
  await dispatchSystemEvent(
    isPosOrder.value ? "HOAN_TAT_POS" : "GIAO_HANG_THANH_CONG",
    isPosOrder.value
      ? "Đã hoàn tất bán hàng tại quầy"
      : "Giao hàng thành công",
    "Đã xác nhận hoàn thành đơn hàng"
  )
}

async function confirmPaymentByEmployee() {
  if (!canEmployeeConfirmPayment.value) return
  await dispatchSystemEvent(
    "THANH_TOAN_NHAN_VIEN_XAC_NHAN",
    "Nhân viên đã xác nhận thanh toán VNPay",
    "Đã xác nhận thanh toán VNPay"
  )
}

function normalizeHistoryStatusLabel(value, fallback) {
  const normalized = normalizeAdminStatusLabel(value)
  return normalized || fallback
}

function deriveFulfillmentStatus(statusCode) {
  const code = normalizeOrderStatusCode(statusCode)
  if (code === "CHO_XAC_NHAN") return { code: "PENDING", label: "Chờ xác nhận" }
  if (code === "CHO_LAY_HANG") return { code: "PACKED", label: "Chờ lấy hàng" }
  if (code === "DANG_GIAO") return { code: "SHIPPING", label: "Đang giao" }
  if (code === "DA_GIAO") return { code: "DELIVERED", label: "Đã giao" }
  if (code === "HOAN_THANH") return { code: "DELIVERED", label: "Hoàn thành" }
  if (code === "GIAO_THAT_BAI") return { code: "FAILED", label: "Giao thất bại" }
  if (code === "HOAN_VE") return { code: "RETURNED", label: "Hoàn về" }
  if (code === "HUY") return { code: "FAILED", label: "Đã hủy" }
  return { code: "PENDING", label: "Chờ xác nhận" }
}

function deriveBusinessClosure(statusCode) {
  const code = normalizeOrderStatusCode(statusCode)
  if (["HOAN_THANH", "HUY", "HOAN_VE"].includes(code)) return "CLOSED"
  return "OPEN"
}

function deriveBusinessClosureName(statusCode) {
  return deriveBusinessClosure(statusCode) === "CLOSED" ? "Đã chốt" : "Đang mở"
}

function buildFallbackHistory(row) {
  const normalizedCode = normalizeOrderStatusCode(row?.orderStatusCode, row?.orderStatusName)
  const currentStatus = normalizeHistoryStatusLabel(
    row?.orderStatusName || statusNameMap[normalizedCode] || hoaDon.value.orderStatusName,
    "Chờ xác nhận"
  )
  const changedAt = row?.ngayHuy || row?.ngayTao || new Date().toISOString()
  const note = row?.ngayHuy ? "Hóa đơn đã kết thúc ở trạng thái hiện tại." : "Tạo hóa đơn"

  return [{
    changedAt,
    fromStatus: "Khởi tạo",
    toStatus: currentStatus,
    note
  }]
}

function normalizeHistoryEntries(rawHistory, row) {
  const normalizedCode = normalizeOrderStatusCode(row?.orderStatusCode, row?.orderStatusName)
  const entries = Array.isArray(rawHistory)
    ? rawHistory
      .map((entry) => ({
        changedAt: entry?.changedAt || entry?.createdAt || row?.ngayHuy || row?.ngayTao || null,
        fromStatus: normalizeHistoryStatusLabel(entry?.fromStatus, "Khởi tạo"),
        toStatus: normalizeHistoryStatusLabel(
          entry?.toStatus,
          normalizeHistoryStatusLabel(row?.orderStatusName || statusNameMap[normalizedCode], "Chờ xác nhận")
        ),
        note: String(entry?.note || "").trim() || "Không có ghi chú"
      }))
      .filter((entry) => entry.changedAt || entry.toStatus)
    : []

  if (entries.length) return entries
  return buildFallbackHistory(row)
}

async function loadReferenceData() {
  const [nhanVienRes, khachHangRes, sanPhamRes] = await Promise.all([
    getAllNhanVien(),
    getAllKhachHang(0, 100),
    getAllSanPham()
  ])

  nhanVienList.value = extractList(nhanVienRes)
  khachHangList.value = extractList(khachHangRes)
  sanPhamVariants.value = flattenVariants(extractList(sanPhamRes))
}

function normalizeLoadedItems(loadedItems) {
  return (loadedItems || []).map((item) => {
    const variant = getVariant(item.spctId)
    return {
      id: item.id,
      spctId: item.spctId,
      maSanPham: item.maSanPham || variant?.maSanPham || "",
      maSanPhamChiTiet: item.maSanPhamChiTiet || variant?.maSanPhamChiTiet || "",
      soLuong: Number(item.soLuong || 0),
      giaBan: Number(item.giaBan || variant?.giaBan || 0),
      thanhTien: Number(item.thanhTien || 0),
      trangThai: item.trangThai || "ACTIVE",
      tenSanPhamChiTiet: variant?.tenSanPhamChiTiet || item.tenSanPhamChiTiet || ""
    }
  })
}

function applyInvoiceDetail(detail) {
  const safeDetail = detail || {}
  const row = safeDetail?.hoaDon || {}

  const normalizedHistory = normalizeHistoryEntries(safeDetail?.history, row)
  const latestHistoryNote = [...normalizedHistory]
    .reverse()
    .find((entry) => entry?.note && entry.note !== "Không có ghi chú")?.note || ""

  hoaDon.value = {
    id: row.id,
    maHoaDon: row.maHoaDon || "",
    nhanVienId: row.nhanVienId != null ? Number(row.nhanVienId) : null,
    khachHangId: row.khachHangId != null ? Number(row.khachHangId) : null,
    soDienThoaiNhanHang: row.soDienThoaiNhanHang || "",
    diaChiNhanHang: row.diaChiNhanHang || "",
    ngayNhanHangDuKien: formatDateInput(row.ngayNhanHangDuKien),
    ngayNhanHangMongMuon: formatDateInput(row.ngayNhanHangMongMuon),
    phiShip: Number(row.phiShip || 0),
    giaSauGiamGia: Number(row.giaSauGiamGia || 0),
    thanhTien: Number(row.thanhTien || 0),
    orderStatusCode: normalizeOrderStatusCode(row.orderStatusCode, row.orderStatusName, row.statusNote),
    orderStatusName: row.orderStatusName || "Chờ xác nhận",
    orderType: String(
      row.orderType
      || (String(row.statusNote || "").toUpperCase().includes("[POS]")
        || String(row.diaChiNhanHang || "").toLowerCase().includes("mua tại quầy")
        || String(row.diaChiNhanHang || "").toLowerCase().includes("mua tai quay")
        ? "POS"
        : "ONLINE")
    ).toUpperCase(),
    statusNote: row.statusNote || latestHistoryNote,
    phuongThucThanhToan: row.phuongThucThanhToan || "COD",
    fulfillmentStatusCode: row.fulfillmentStatusCode || deriveFulfillmentStatus(row.orderStatusCode).code,
    fulfillmentStatusName: row.fulfillmentStatusName || deriveFulfillmentStatus(row.orderStatusCode).label,
    businessClosureStatus: row.businessClosureStatus || deriveBusinessClosure(row.orderStatusCode),
    businessClosureStatusName: row.businessClosureStatusName || deriveBusinessClosureName(row.orderStatusCode),
    finalOrder: Boolean(safeDetail?.finalOrder)
  }

  manualStatusNote.value = hoaDon.value.statusNote || ""
  items.value = normalizeLoadedItems(safeDetail?.items)
  history.value = normalizedHistory

  if (!hoaDon.value.soDienThoaiNhanHang && selectedCustomer.value?.soDienThoai) {
    hoaDon.value.soDienThoaiNhanHang = selectedCustomer.value.soDienThoai
  }

  syncTotals()
}

async function loadInvoice() {
  if (isCreate.value) {
    const today = new Date().toISOString().split("T")[0]
    hoaDon.value.ngayNhanHangDuKien = today
    hoaDon.value.ngayNhanHangMongMuon = today
    return
  }

  const response = await getHoaDonById(route.params.id)
  if (isHtmlPayload(response?.data)) {
    throw new Error("API hoá đơn đang bị chuyển hướng tới /login. Hãy restart backend sau khi cập nhật SecurityConfig.")
  }

  const detail = response.data
  applyInvoiceDetail(detail)
}

async function loadData() {
  isLoading.value = true
  apiWarning.value = ""

  try {
    await loadReferenceData()
    await loadInvoice()
  } catch (error) {
    console.error("HoaDon load error:", error)
    apiWarning.value = error.message || "Không thể tải dữ liệu hoá đơn"
    showToast(apiWarning.value, "error")
  } finally {
    isLoading.value = false
  }
}

function addProduct() {
  if (!newItem.value.spctId || Number(newItem.value.soLuong) <= 0) {
    showToast("Chọn sản phẩm và số lượng hợp lệ", "warning")
    return
  }

  const variant = getVariant(newItem.value.spctId)
  if (!variant) {
    showToast("Không tìm thấy biến thể sản phẩm", "error")
    return
  }

  const quantity = Number(newItem.value.soLuong)
  const existing = items.value.find((item) => item.spctId === newItem.value.spctId)

  if (existing) {
    existing.soLuong += quantity
    existing.thanhTien = existing.soLuong * existing.giaBan
  } else {
    items.value.push({
      id: null,
      spctId: variant.spctId,
      soLuong: quantity,
      giaBan: variant.giaBan,
      thanhTien: quantity * variant.giaBan,
      trangThai: "ACTIVE",
      tenSanPhamChiTiet: variant.tenSanPhamChiTiet
    })
  }

  newItem.value = { spctId: null, soLuong: 1 }
  syncTotals()
}

async function removeItem(index) {
  const item = items.value[index]
  if (!item) return

  const accepted = await askConfirm("Xóa sản phẩm khỏi hoá đơn?", {
    title: "Xác nhận xóa",
    confirmText: "Xóa",
    cancelText: "Giữ lại"
  })

  if (!accepted) return

  try {
    if (!isCreate.value && item.id) {
      await deleteHoaDonItem(hoaDon.value.id, item.id)
    }

    items.value.splice(index, 1)
    syncTotals()
    showToast("Đã xóa sản phẩm", "success")
  } catch (error) {
    console.error("Remove item failed:", error)
    showToast("Không thể xóa sản phẩm", "error")
  }
}

async function changeItemQuantity(index, rawValue) {
  const item = items.value[index]
  if (!item) return

  const quantity = Number(rawValue || 0)
  if (quantity <= 0) {
    await removeItem(index)
    return
  }

  item.soLuong = quantity
  item.thanhTien = quantity * Number(item.giaBan || 0)
  syncTotals()

  if (!isCreate.value && item.id) {
    try {
      await updateHoaDonItemQty(hoaDon.value.id, item.id, { soLuong: quantity })
    } catch (error) {
      console.error("Update qty failed:", error)
      showToast("Không thể cập nhật số lượng", "error")
    }
  }
}

function buildUpdatePayload() {
  const normalizedOrderType = String(hoaDon.value.orderType || "ONLINE").toUpperCase()
  const isPos = normalizedOrderType === "POS"
  const normalizedPayment = String(hoaDon.value.phuongThucThanhToan || "COD").toUpperCase()
  const currentStatusCode = normalizeOrderStatusCode(hoaDon.value.orderStatusCode, hoaDon.value.orderStatusName, hoaDon.value.statusNote)
  const normalizedStatusCode = isPos
    ? (currentStatusCode === "CHO_XAC_NHAN" ? "CHO_LAY_HANG" : currentStatusCode)
    : currentStatusCode

  const noteParts = []
  const orderTag = isPos ? "[POS]" : "[ONLINE]"
  noteParts.push(orderTag)
  if (manualStatusNote.value) noteParts.push(manualStatusNote.value)
  if (selectedVoucher.value) {
    noteParts.push(`Áp dụng voucher ${selectedVoucher.value.maPhieuGiamGia || selectedVoucher.value.tenPhieuGiamGia}`)
  }

  const dedupedNote = Array.from(new Set(noteParts)).join(" | ")

  return {
    nhanVienId: hoaDon.value.nhanVienId,
    khachHangId: hoaDon.value.khachHangId || null,
    soDienThoaiNhanHang: isPos
      ? (selectedCustomer.value?.soDienThoai || hoaDon.value.soDienThoaiNhanHang || "")
      : hoaDon.value.soDienThoaiNhanHang,
    diaChiNhanHang: isPos ? "Mua tại quầy" : hoaDon.value.diaChiNhanHang,
    ngayNhanHangDuKien: isPos ? null : (hoaDon.value.ngayNhanHangDuKien || null),
    ngayNhanHangMongMuon: isPos ? null : (hoaDon.value.ngayNhanHangMongMuon || null),
    phiShip: isPos ? 0 : Number(hoaDon.value.phiShip || 0),
    giaSauGiamGia: isPos ? 0 : Number(hoaDon.value.giaSauGiamGia || 0),
    phuongThucThanhToan: isPos && normalizedPayment === "COD" ? "CASH" : normalizedPayment,
    orderType: normalizedOrderType,
    orderStatusCode: normalizedStatusCode,
    statusNote: dedupedNote || undefined
  }
}

async function saveInvoice() {
  if (!hoaDon.value.nhanVienId) {
    showToast("Vui lòng chọn nhân viên", "warning")
    return
  }

  if (!isPosOrder.value && !hoaDon.value.khachHangId) {
    showToast("Vui lòng chọn khách hàng", "warning")
    return
  }

  if (items.value.length === 0) {
    showToast("Hoá đơn cần ít nhất một sản phẩm", "warning")
    return
  }

  isSaving.value = true

  try {
    if (isCreate.value) {
      const isPos = isPosOrder.value
      const normalizedPayment = String(hoaDon.value.phuongThucThanhToan || "COD").toUpperCase()
      const createPayload = {
        nhanVienId: hoaDon.value.nhanVienId,
        khachHangId: hoaDon.value.khachHangId || null,
        soDienThoaiNhanHang: isPos
          ? (selectedCustomer.value?.soDienThoai || hoaDon.value.soDienThoaiNhanHang || "")
          : hoaDon.value.soDienThoaiNhanHang,
        diaChiNhanHang: isPos ? "Mua tại quầy" : hoaDon.value.diaChiNhanHang,
        ngayNhanHangDuKien: isPos ? null : (hoaDon.value.ngayNhanHangDuKien || null),
        ngayNhanHangMongMuon: isPos ? null : (hoaDon.value.ngayNhanHangMongMuon || null),
        phiShip: isPos ? 0 : Number(hoaDon.value.phiShip || 0),
        phuongThucThanhToan: isPos && normalizedPayment === "COD" ? "CASH" : normalizedPayment,
        orderType: isPos ? "POS" : "ONLINE",
        orderStatusCode: isPos
          ? "CHO_LAY_HANG"
          : "CHO_XAC_NHAN"
      }

      const createResponse = await createHoaDon(createPayload)
      if (isHtmlPayload(createResponse?.data)) {
        throw new Error("Backend đang chuyển hướng POST /api/hoa-don sang /login. Hãy restart DATN-API sau khi áp dụng SecurityConfig.")
      }

      const newInvoiceId = createResponse?.data?.hoaDon?.id || createResponse?.data?.id
      if (!newInvoiceId) {
        throw new Error("Không lấy được ID hoá đơn mới từ API")
      }

      for (const item of items.value) {
        await addHoaDonItem(newInvoiceId, {
          spctId: item.spctId,
          soLuong: item.soLuong,
          giaBan: item.giaBan
        })
      }

      await updateHoaDon(newInvoiceId, buildUpdatePayload())
      showToast("Tạo hoá đơn thành công", "success")
      router.push(`${panelBasePath.value}/hoa-don/detail/${newInvoiceId}?refresh=true`)
      return
    }

    await updateHoaDon(hoaDon.value.id, buildUpdatePayload())
    showToast("Cập nhật hoá đơn thành công", "success")
    backToList(true)
  } catch (error) {
    console.error("Save invoice failed:", error)
    const message = error?.response?.data?.message || error.message || "Không thể lưu hoá đơn"
    showToast(message, "error")
  } finally {
    isSaving.value = false
  }
}

onMounted(loadData)

watch(
  () => hoaDon.value.orderType,
  (next) => {
    const normalized = String(next || "ONLINE").toUpperCase()
    if (normalized === "POS") {
      hoaDon.value.phiShip = 0
      hoaDon.value.ngayNhanHangDuKien = ""
      hoaDon.value.ngayNhanHangMongMuon = ""
      hoaDon.value.diaChiNhanHang = "Mua tại quầy"
      hoaDon.value.soDienThoaiNhanHang = selectedCustomer.value?.soDienThoai || ""
      if (String(hoaDon.value.phuongThucThanhToan || "").toUpperCase() === "COD") {
        hoaDon.value.phuongThucThanhToan = "CASH"
      }
      if (normalizeOrderStatusCode(hoaDon.value.orderStatusCode, hoaDon.value.orderStatusName) === "CHO_XAC_NHAN") {
        hoaDon.value.orderStatusCode = "CHO_LAY_HANG"
      }
    }

    if (normalized !== "POS" && String(hoaDon.value.phuongThucThanhToan || "").toUpperCase() === "CASH") {
      hoaDon.value.phuongThucThanhToan = "COD"
    }

    syncTotals()
  }
)

watch(
  () => hoaDon.value.khachHangId,
  () => {
    if (isPosOrder.value) {
      hoaDon.value.soDienThoaiNhanHang = selectedCustomer.value?.soDienThoai || ""
    }
  }
)

watch(
  () => hoaDon.value.orderType,
  () => {
    if (isPosOrder.value) {
      hoaDon.value.giaSauGiamGia = 0
      selectedVoucher.value = null
    }
    syncTotals()
  }
)
</script>

<template>
  <main class="hoa-don-page">
    <div class="hoa-don-shell">
      <header class="hero-card">
        <div>
          <p class="eyebrow">Quản lý hoá đơn</p>
          <h1>{{ pageTitle }}</h1>
          <p class="hero-subtitle">
            {{ isCreate ? "Tạo đơn hàng từ dữ liệu nhân viên, khách hàng, sản phẩm và voucher hiện có." : "Theo dõi đơn hàng, chỉnh sửa thông tin giao nhận và đồng bộ trạng thái." }}
          </p>
        </div>

        <div class="hero-actions">
          <button class="btn ghost" @click="backToList()">Quay lại</button>
          <button class="btn primary" @click="saveInvoice" :disabled="isSaving">
            <Loader2 v-if="isSaving" :size="16" class="spin" />
            <Save v-else :size="16" />
            <span>{{ isSaving ? "Đang lưu" : "Lưu hoá đơn" }}</span>
          </button>
        </div>
      </header>

      <div v-if="apiWarning" class="warning-banner">
        <strong>API cảnh báo:</strong> {{ apiWarning }}
      </div>

      <div v-if="isLoading" class="loading-card">
        <Loader2 :size="20" class="spin" />
        <span>Đang tải dữ liệu hoá đơn...</span>
      </div>

      <template v-else>
        <section class="overview-grid">
          <article class="metric-card">
            <div class="metric-icon accent"><ShoppingBag :size="18" /></div>
            <div>
              <p class="metric-label">Trạng thái hiện tại</p>
              <strong>{{ currentStatusName }}</strong>
            </div>
          </article>

          <article class="metric-card">
            <div class="metric-icon gold"><Ticket :size="18" /></div>
            <div>
              <p class="metric-label">Giảm giá đang áp dụng</p>
              <strong>{{ formatCurrency(hoaDon.giaSauGiamGia) }}</strong>
            </div>
          </article>

          <article class="metric-card">
            <div class="metric-icon teal"><Package2 :size="18" /></div>
            <div>
              <p class="metric-label">Sản phẩm trong đơn</p>
              <strong>{{ items.length }}</strong>
            </div>
          </article>

          <article class="metric-card">
            <div class="metric-icon slate"><UserRound :size="18" /></div>
            <div>
              <p class="metric-label">Khách hàng</p>
              <strong>{{ selectedCustomerLabel }}</strong>
            </div>
          </article>
        </section>

        <div class="detail-grid">
          <section class="panel soft">
            <div class="panel-head">
              <div>
                <h2>{{ isPosOrder ? 'Thông tin bán tại quầy' : 'Thông tin giao nhận' }}</h2>
              </div>
            </div>

            <div class="form-grid">
              <label class="field">
                <span>Nhân viên bán hàng</span>
                <select class="strong-select" v-model.number="hoaDon.nhanVienId" :disabled="!canEdit">
                  <option :value="null">Chọn nhân viên</option>
                  <option v-for="nhanVien in nhanVienList" :key="nhanVien.id" :value="Number(nhanVien.id)">
                    {{ nhanVien.tenNhanVien || nhanVien.hoTen || `NV #${nhanVien.id}` }}
                  </option>
                </select>
              </label>

              <label class="field">
                <span>Khách hàng</span>
                <select class="strong-select" v-model.number="hoaDon.khachHangId" :disabled="!canEdit">
                  <option :value="null">Chọn khách hàng</option>
                  <option v-for="khachHang in khachHangList" :key="khachHang.id" :value="Number(khachHang.id)">
                    {{ khachHang.tenKhachHang || `KH #${khachHang.id}` }}
                  </option>
                </select>
              </label>

              <label class="field full" v-if="!isPosOrder">
                <span>Số điện thoại nhận hàng</span>
                <input v-model="hoaDon.soDienThoaiNhanHang" type="text" placeholder="Nhập số điện thoại nhận hàng" :disabled="!canEdit" />
              </label>

              <label class="field full" v-if="!isPosOrder">
                <span>Địa chỉ nhận hàng</span>
                <textarea v-model="hoaDon.diaChiNhanHang" rows="3" placeholder="Nhập địa chỉ giao hàng" :disabled="!canEdit"></textarea>
              </label>

              <label class="field full" v-else>
                <span>Giao nhận</span>
                <input value="Mua tại quầy" type="text" disabled />
              </label>

              <label class="field" v-if="!isPosOrder">
                <span>Ngày giao dự kiến</span>
                <input v-model="hoaDon.ngayNhanHangDuKien" type="date" :disabled="!canEdit" />
              </label>

              <label class="field" v-if="!isPosOrder">
                <span>Ngày khách mong muốn</span>
                <input v-model="hoaDon.ngayNhanHangMongMuon" type="date" :disabled="!canEdit" />
              </label>

              <label class="field" v-if="!isPosOrder">
                <span>Phí ship</span>
                <input v-model.number="hoaDon.phiShip" type="number" min="0" :disabled="!canEdit" @input="syncTotals" />
              </label>

              <label class="field">
                <span>Loại đơn</span>
                <select class="strong-select" v-model="hoaDon.orderType" :disabled="!canEdit">
                  <option value="ONLINE">Online</option>
                  <option value="POS">Tại quầy (POS)</option>
                </select>
              </label>

              <label class="field">
                <span>Thanh toán</span>
                <select class="strong-select" v-model="hoaDon.phuongThucThanhToan" :disabled="!canEdit">
                  <template v-if="isPosOrder">
                    <option value="CASH">Tiền mặt tại quầy</option>
                    <option value="BANK">Chuyển khoản</option>
                    <option value="VNPAY">VNPay</option>
                  </template>
                  <template v-else>
                    <option value="COD">COD</option>
                    <option value="VNPAY">VNPay</option>
                    <option value="BANK">Chuyển khoản</option>
                  </template>
                </select>
              </label>

              <div class="field voucher-field full">
                <span>Giảm giá</span>
                <VoucherSelector
                  :subtotal="subtotal"
                  :customer-id="hoaDon.khachHangId"
                  @update:voucher="selectedVoucher = $event"
                  @discount-changed="applyDiscount"
                />
              </div>
            </div>
          </section>

          <section class="panel contrast">
            <div class="panel-head space-between">
              <div>
                <h2>Trạng thái và tổng quan</h2>
              </div>

              <span class="status-badge" :class="`status-${currentStatusTone}`">{{ currentStatusName }}</span>
            </div>

            <div class="status-actions" v-if="!isCreate">
              <p class="status-hint">Trạng thái đơn hàng chỉ được cập nhật qua sự kiện hệ thống giao hàng/thanh toán.</p>
              <p v-if="hoaDon.finalOrder" class="status-hint">Hóa đơn đã kết thúc nên không thể nhận thêm sự kiện.</p>
              <button
                v-if="canConfirmOnlineOrder"
                class="btn primary"
                type="button"
                :disabled="isSaving"
                @click="confirmOnlineOrder"
              >
                <CheckCircle2 :size="16" />
                <span>Xác nhận đơn hàng</span>
              </button>
              <button
                v-if="canStartShipping"
                class="btn primary"
                type="button"
                :disabled="isSaving"
                @click="startShipping"
              >
                <CheckCircle2 :size="16" />
                <span>Bắt đầu giao hàng</span>
              </button>
              <button
                v-if="canQuickComplete"
                class="btn primary"
                type="button"
                :disabled="isSaving"
                @click="quickCompleteOrder"
              >
                <CheckCircle2 :size="16" />
                <span>Xác nhận hoàn thành</span>
              </button>
              <button
                v-if="canMarkReturned"
                class="btn primary"
                type="button"
                :disabled="isSaving"
                @click="markReturned"
              >
                <CheckCircle2 :size="16" />
                <span>Xác nhận hoàn về</span>
              </button>
            </div>

            <div class="payment-verify-card" v-if="paymentFlowState">
              <div class="payment-verify-header">
                <span class="payment-tag vnpay">VNPay</span>
                <span class="payment-flow-label" :class="`tone-${paymentFlowState.tone}`">{{ paymentFlowState.label }}</span>
              </div>
              <div v-if="canEmployeeConfirmPayment" class="payment-verify-actions">
                <p class="payment-verify-hint">Khách hàng đã bấm xác nhận chuyển khoản VNPay. Kiểm tra tài khoản ngân hàng trước khi xác nhận.</p>
                <button class="btn btn-success" type="button" :disabled="isSaving" @click="confirmPaymentByEmployee">
                  <CheckCircle2 :size="16" />
                  <span>Xác nhận thanh toán VNPay</span>
                </button>
              </div>
            </div>

            <div class="summary-box">
              <div class="summary-row">
                <span>Loại đơn</span>
                <strong>{{ orderTypeLabel }}</strong>
              </div>
              <div class="summary-row">
                <span>Thanh toán</span>
                <strong>{{ paymentMethodLabel }}</strong>
              </div>
              <div class="summary-row">
                <span>Tạm tính</span>
                <strong>{{ formatCurrency(subtotal) }}</strong>
              </div>
              <div class="summary-row" v-if="!isPosOrder">
                <span>Phí ship</span>
                <strong>{{ formatCurrency(hoaDon.phiShip) }}</strong>
              </div>
              <div class="summary-row danger-text">
                <span>Giảm giá</span>
                <strong>-{{ formatCurrency(hoaDon.giaSauGiamGia) }}</strong>
              </div>
              <div class="summary-row total">
                <span>Tổng cộng</span>
                <strong>{{ formatCurrency(grandTotal) }}</strong>
              </div>
            </div>

            <div class="history-box">
              <h3>Lịch sử trạng thái</h3>
              <div class="table-wrap compact">
                <table class="history-table">
                  <thead>
                    <tr>
                      <th>Thời gian</th>
                      <th>Từ</th>
                      <th>Đến</th>
                      <th>Ghi chú</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="!history.length">
                      <td colspan="4" class="empty-cell">Chưa có lịch sử trạng thái.</td>
                    </tr>
                    <tr v-for="(entry, index) in history" :key="`${entry.changedAt || 'history'}-${index}`">
                      <td>{{ formatDateTime(entry.changedAt) }}</td>
                      <td>{{ entry.fromStatus }}</td>
                      <td>{{ entry.toStatus }}</td>
                      <td>{{ entry.note }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </div>

        <section class="panel wide-panel">
          <div class="panel-head space-between">
            <div>
              <h2>Sản phẩm trong hoá đơn</h2>
              <p>Dữ liệu lấy từ danh sách sản phẩm và biến thể hiện có.</p>
            </div>
          </div>

          <div class="product-builder">
            <select class="strong-select" v-model.number="newItem.spctId" :disabled="!canEdit">
              <option :value="null">Chọn biến thể sản phẩm</option>
              <option v-for="variant in sanPhamVariants" :key="variant.spctId" :value="variant.spctId">
                {{ variant.tenSanPhamChiTiet }} · {{ formatCurrency(variant.giaBan) }} · Tồn {{ variant.soLuongTon }}
              </option>
            </select>

            <input v-model.number="newItem.soLuong" type="number" min="1" :disabled="!canEdit" />

            <button class="btn primary" type="button" @click="addProduct" :disabled="!canEdit">
              <Plus :size="16" />
              <span>Thêm sản phẩm</span>
            </button>
          </div>

          <div class="table-wrap">
            <table class="order-table">
              <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th>Giá bán</th>
                  <th>Số lượng</th>
                  <th>Thành tiền</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!items.length">
                  <td colspan="5" class="empty-cell">Chưa có sản phẩm nào trong hoá đơn.</td>
                </tr>
                <tr v-for="(item, index) in items" :key="`${item.spctId}-${index}`">
                  <td>
                    <div class="product-name">{{ getItemName(item) }}</div>
                    <small v-if="item.maSanPham || item.maSanPhamChiTiet || getVariant(item.spctId)">
                      {{ getItemCodeMeta(item).productCode }} / {{ getItemCodeMeta(item).variantCode }}
                    </small>
                    <small v-else>SPCT #{{ item.spctId }}</small>
                    <small
                      v-if="getItemCodeMeta(item).mismatch"
                      class="code-warning"
                    >
                      Cảnh báo lệch mã với catalog
                    </small>
                  </td>
                  <td>{{ formatCurrency(item.giaBan) }}</td>
                  <td>
                    <input
                      class="qty-input"
                      type="number"
                      min="1"
                      :value="item.soLuong"
                      :disabled="!canEdit"
                      @change="changeItemQuantity(index, $event.target.value)"
                    />
                  </td>
                  <td>{{ formatCurrency(item.thanhTien) }}</td>
                  <td class="action-cell">
                    <button class="icon-btn" type="button" @click="removeItem(index)" :disabled="!canEdit">
                      <Trash2 :size="16" />
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>
    </div>
  </main>
</template>

<style scoped>
.hoa-don-page {
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(255, 224, 230, 0.9), transparent 35%),
    linear-gradient(180deg, #fff8f6 0%, #f8fafc 100%);
  min-height: 100vh;
}

.hoa-don-shell {
  display: grid;
  gap: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.hoa-don-shell > * {
  animation: riseIn 0.5s ease both;
}

.hoa-don-shell > *:nth-child(2) { animation-delay: 0.05s; }
.hoa-don-shell > *:nth-child(3) { animation-delay: 0.1s; }
.hoa-don-shell > *:nth-child(4) { animation-delay: 0.15s; }
.hoa-don-shell > *:nth-child(5) { animation-delay: 0.2s; }

.hero-card,
.panel,
.metric-card,
.loading-card,
.warning-banner {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(12px);
}

.hero-card {
  padding: 28px 30px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
}

.hero-card > div:first-child {
  max-width: 720px;
}

.eyebrow {
  margin: 0 0 8px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 12px;
  font-weight: 700;
  color: #b91c1c;
}

.hero-card h1,
.panel h2,
.panel h3 {
  margin: 0;
  color: #111827;
}

.hero-subtitle,
.panel-head p,
.metric-label {
  color: #64748b;
  margin: 0;
}

.hero-subtitle {
  margin-top: 10px;
  font-size: 15px;
  line-height: 1.6;
}

.code-warning {
  margin-top: 4px;
  display: inline-block;
  color: #b45309;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 999px;
  padding: 2px 8px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.btn {
  border: none;
  border-radius: 14px;
  padding: 12px 16px;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn.primary {
  background: linear-gradient(135deg, #ef4444, #991b1b);
  color: white;
  box-shadow: 0 10px 22px rgba(153, 27, 27, 0.28);
}

.btn.ghost {
  background: #f8fafc;
  color: #0f172a;
  border: 1px solid #dbe2ea;
}

.btn.danger {
  background: #fff1f2;
  color: #be123c;
  border: 1px solid #fecdd3;
}

.btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.btn:disabled,
.icon-btn:disabled,
.product-builder select:disabled,
.product-builder input:disabled,
.field input:disabled,
.field select:disabled,
.field textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.warning-banner,
.loading-card {
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.warning-banner {
  color: #92400e;
  background: #fff7ed;
  border-color: #fdba74;
  font-weight: 600;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  padding: 18px;
  display: flex;
  gap: 14px;
  align-items: center;
  min-height: 86px;
}

.metric-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
}

.metric-icon.accent { background: #fee2e2; color: #b91c1c; }
.metric-icon.gold { background: #fef3c7; color: #b45309; }
.metric-icon.teal { background: #ccfbf1; color: #0f766e; }
.metric-icon.slate { background: #e2e8f0; color: #334155; }

.detail-grid {
  display: grid;
  grid-template-columns: 1.3fr 0.9fr;
  gap: 20px;
}

.panel {
  padding: 24px;
}

.panel h2 {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.panel h3 {
  font-size: 16px;
  font-weight: 700;
}

.panel.soft {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(255, 247, 247, 0.95));
}

.panel.contrast {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.98));
}

.wide-panel {
  padding-bottom: 18px;
}

.panel-head {
  margin-bottom: 18px;
}

.space-between {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field {
  display: grid;
  gap: 8px;
}

.field-hint {
  min-height: 20px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.field.full,
.voucher-field {
  grid-column: 1 / -1;
}

.field span {
  font-size: 14px;
  font-weight: 800;
  color: #1e293b;
}

.field input,
.field select,
.field textarea,
.product-builder select,
.product-builder input,
.qty-input {
  width: 100%;
  border-radius: 14px;
  border: 1px solid #dbe2ea;
  background: white;
  padding: 12px 14px;
  font: inherit;
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.strong-select {
  height: 46px !important;
  min-height: 46px !important;
  padding: 0 14px !important;
  line-height: 46px !important;
  font-size: 14px !important;
  font-weight: 700 !important;
  letter-spacing: 0 !important;
  -webkit-text-fill-color: #0f172a !important;
  color: #0f172a !important;
  background-color: #ffffff !important;
  border: 1px solid #94a3b8 !important;
  text-shadow: none !important;
  opacity: 1 !important;
}

.strong-select:focus {
  border-color: #ef4444 !important;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.14) !important;
}

.strong-select option {
  color: #0f172a !important;
  background: #ffffff !important;
  font-weight: 700 !important;
  font-size: 14px !important;
}

.strong-select option:first-child {
  color: #334155 !important;
}

.field input:focus,
.field select:focus,
.field textarea:focus,
.product-builder select:focus,
.product-builder input:focus,
.qty-input:focus {
  outline: none;
  border-color: #ef4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.14);
}

.field textarea {
  resize: vertical;
}

.status-badge {
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  border: 1px solid;
}

.status-success {
  background: #dcfce7;
  color: #166534;
  border-color: #86efac;
}

.status-warning {
  background: #fef3c7;
  color: #92400e;
  border-color: #fcd34d;
}

.status-danger {
  background: #fee2e2;
  color: #991b1b;
  border-color: #fca5a5;
}

.status-actions {
  margin-bottom: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.status-action {
  border-width: 1px;
  border-style: solid;
  box-shadow: none;
}

.status-action-success {
  background: #ecfdf3;
  border-color: #86efac;
  color: #166534;
}

.status-action-warning {
  background: #fffbeb;
  border-color: #fcd34d;
  color: #92400e;
}

.status-action-danger {
  background: #fff1f2;
  border-color: #fda4af;
  color: #be123c;
}

.status-hint {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.status-note-box {
  margin-bottom: 14px;
}

.summary-box {
  display: grid;
  gap: 10px;
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, #fff7ed, #ffffff);
  border: 1px solid #fed7aa;
}

.summary-row strong {
  font-size: 15px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #334155;
}

.summary-row.total {
  margin-top: 6px;
  padding-top: 12px;
  border-top: 1px dashed #fdba74;
  color: #b91c1c;
  font-size: 18px;
}

.danger-text {
  color: #dc2626;
}

.payment-verify-card {
  margin-top: 14px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  display: grid;
  gap: 8px;
}

.payment-state {
  margin: 0;
  font-weight: 700;
}

.payment-success {
  color: #166534;
}

.payment-warning {
  color: #b45309;
}

.payment-neutral {
  color: #475569;
}

.payment-note {
  margin: 0;
  color: #334155;
  font-size: 13px;
  line-height: 1.5;
}

.history-box {
  margin-top: 18px;
  display: grid;
  gap: 14px;
}

.table-wrap.compact {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
}

.history-table {
  width: 100%;
  border-collapse: collapse;
}

.history-table th,
.history-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #eef2f7;
  text-align: left;
}

.history-table th {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #64748b;
  background: #f8fafc;
}

.product-builder {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px auto;
  gap: 12px;
  margin-bottom: 18px;
  align-items: center;
}

.table-wrap {
  overflow-x: auto;
}

.order-table {
  width: 100%;
  border-collapse: collapse;
  border-radius: 14px;
  overflow: hidden;
}

.order-table th,
.order-table td {
  padding: 14px 12px;
  border-bottom: 1px solid #edf2f7;
  text-align: left;
}

.order-table th {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #64748b;
  background: #f8fafc;
}

.product-name {
  font-weight: 700;
  color: #111827;
}

.qty-input {
  width: 92px;
}

.action-cell {
  width: 64px;
}

.icon-btn {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  border: none;
  background: #fff1f2;
  color: #dc2626;
  display: inline-grid;
  place-items: center;
  cursor: pointer;
}

.icon-btn:hover:not(:disabled) {
  background: #ffe4e6;
}

.empty-cell {
  text-align: center;
  color: #94a3b8;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes riseIn {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1100px) {
  .overview-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hoa-don-page {
    padding: 16px;
  }

  .hero-card,
  .space-between,
  .hero-actions,
  .product-builder,
  .form-grid {
    display: grid;
    grid-template-columns: 1fr;
  }

  .status-actions .btn {
    width: 100%;
  }
}
</style>
