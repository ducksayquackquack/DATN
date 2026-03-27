<script setup>
import { computed, onMounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import {
  addHoaDonItem,
  createHoaDon,
  deleteHoaDonItem,
  getHoaDonById,
  sendOrderLookupMail,
  updateHoaDon,
  updateHoaDonBySystemEvent,
  updateHoaDonItemQty
} from "../../../services/hoaDonService"
import { createKhachHang, getAllKhachHang } from "../../../services/KhachHangService"
import { getAllNhanVien, getNhanVienByTaiKhoanId } from "../../../services/nhanVienService"
import { getAllSanPham } from "../../../services/sanPhamService"
import VoucherSelector from "../../../components/voucher/VoucherSelector.vue"
import { useConfirm } from "../../../composables/useConfirm"
import { useToast } from "../../../composables/useToast"
import { CheckCircle2, ClipboardList, Loader2, Minus, Package2, PackageCheck, PackageSearch, Plus, RotateCcw, Save, Search, ShoppingBag, Ticket, Trash2, Truck, UserRound, X, OctagonX } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel, normalizeOrderStatusCode } from "../../../utils/adminStatus"
import { describePaymentFlowState } from "../../../utils/paymentWorkflow"
import { buildOrderLookupTrackingUrl } from "../../../utils/publicTrackingUrl"
import { validateEmployeeActiveShift } from "../../../utils/shiftGuard"
import { resolveApiOrigin } from "../../../utils/apiOrigin"
import { getProductImageOverride } from "../../../utils/productImageOverrides"
import logoFallback from "../../../assets/img/logo/new logo.png?url"
import img1 from "../../../assets/img/Jackets/bomber/bomber-da-lon.jpg?url"
import img2 from "../../../assets/img/Jackets/bomber/bomber-dang-lung.jpg?url"
import img3 from "../../../assets/img/Jackets/bomber/bomber-gia-da.jpg?url"
import img4 from "../../../assets/img/Jackets/bomber/bomber-nhe-cotton.jpg?url"
import img5 from "../../../assets/img/Jackets/hoodie/hoodie-dang-hop.jpg?url"
import img6 from "../../../assets/img/Jackets/hoodie/hoodie-in-hinh.jpg?url"
import img7 from "../../../assets/img/Jackets/hoodie/hoodie-keo-khoa.jpg?url"
import img8 from "../../../assets/img/Jackets/coach/coach-cach-nhiet.jpg?url"
import img9 from "../../../assets/img/Jackets/coach/coach-da-asos.jpg?url"
import img10 from "../../../assets/img/Jackets/coach/coach-gia-da.jpg?url"
import img11 from "../../../assets/img/Jackets/coach/coach-long-cuu.jpg?url"
// New products
import img12 from "../../../assets/img/Jackets/bomber/bomber-astronaut/IMG_4435.PNG?url"
import img13 from "../../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/IMG_4437.PNG?url"
import img14 from "../../../assets/img/Jackets/bomber/bomber-windbreaker/IMG_4432.PNG?url"
import img15 from "../../../assets/img/Jackets/coach/coach-leopard/IMG_4445.PNG?url"
import img16 from "../../../assets/img/Jackets/coach/coach-longsleeve/IMG_4442.PNG?url"
import img17 from "../../../assets/img/Jackets/coach/coach-tiger-stripe/IMG_4446.PNG?url"
import img18 from "../../../assets/img/Jackets/hoodie/hoodie-camo/IMG_4450.PNG?url"
import img19 from "../../../assets/img/Jackets/hoodie/hoodie-zip-boxy/IMG_4452.PNG?url"
import img20 from "../../../assets/img/Jackets/hoodie/hoodie-zip-silk/IMG_4447.PNG?url"

const fallbackImages = [img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11]
function fallbackImageFor(id, code = "") {
  const n = Number(id)
  if (Number.isFinite(n) && n > 0) return fallbackImages[(n - 1) % fallbackImages.length]
  const d = Number(String(code || "").replace(/\D+/g, ""))
  if (Number.isFinite(d) && d > 0) return fallbackImages[(d - 1) % fallbackImages.length]
  return fallbackImages[0] || logoFallback
}

const router = useRouter()
const route = useRoute()
const { askConfirm } = useConfirm()
const { showToast } = useToast()
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")
const panelBasePath = computed(() => (route.path.startsWith('/employee/') ? '/employee' : '/admin'))
const isEmployeePanel = computed(() => route.path.startsWith('/employee/'))

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
const isSendingLookupMail = ref(false)
const apiWarning = ref("")
const currentEmployeeId = ref(null)
const employeeOwnershipMismatch = ref(false)

const nhanVienList = ref([])
const khachHangList = ref([])
const sanPhamVariants = ref([])
const items = ref([])
const history = ref([])
const selectedVoucher = ref(null)
const persistedVoucherCode = ref("")
const persistedVoucherName = ref("")
const manualStatusNote = ref("")
const showQuickCustomerForm = ref(false)
const creatingCustomer = ref(false)
const quickCustomer = ref({
  tenKhachHang: "",
  soDienThoai: "",
  email: ""
})

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

const ONLINE_TIMELINE_STEPS = [
  { code: "CHO_XAC_NHAN", label: "Chờ xác nhận", icon: "ClipboardList" },
  { code: "CHO_LAY_HANG", label: "Chờ lấy hàng", icon: "Package2" },
  { code: "DANG_GIAO", label: "Đang giao", icon: "Truck" },
  { code: "HOAN_THANH", label: "Hoàn thành", icon: "CheckCircle2" }
]

const POS_TIMELINE_STEPS = [
  { code: "CHO_LAY_HANG", label: "Chờ xử lý", icon: "ClipboardList" },
  { code: "HOAN_THANH", label: "Hoàn thành", icon: "CheckCircle2" }
]

const ONLINE_TIMELINE_CODE_ORDER = { CHO_XAC_NHAN: 0, CHO_LAY_HANG: 1, DANG_GIAO: 2, HOAN_THANH: 3 }
const POS_TIMELINE_CODE_ORDER = { CHO_XAC_NHAN: 0, CHO_LAY_HANG: 0, HOAN_THANH: 1 }
const TERMINAL_CODES = ["GIAO_THAT_BAI", "HOAN_VE", "HUY"]

const timelineSteps = computed(() => (isPosOrder.value ? POS_TIMELINE_STEPS : ONLINE_TIMELINE_STEPS))
const timelineCodeOrder = computed(() => (isPosOrder.value ? POS_TIMELINE_CODE_ORDER : ONLINE_TIMELINE_CODE_ORDER))

const timelineCurrentIndex = computed(() => {
  const code = currentOrderCode.value
  if (TERMINAL_CODES.includes(code)) return -1
  return timelineCodeOrder.value[code] ?? 0
})

const timelineProgressPercent = computed(() => {
  const idx = timelineCurrentIndex.value
  if (idx < 0) return 0
  const maxIndex = Math.max(1, timelineSteps.value.length - 1)
  return (idx / maxIndex) * 100
})

const isTerminalStatus = computed(() => TERMINAL_CODES.includes(currentOrderCode.value))

const showAddProductModal = ref(false)
const productSearchKeyword = ref("")
const filteredProductVariants = computed(() => {
  const kw = productSearchKeyword.value.trim().toLowerCase()
  if (!kw) return sanPhamVariants.value.slice(0, 50)
  return sanPhamVariants.value.filter((v) =>
    [v.maSanPham, v.maSanPhamChiTiet, v.tenSanPhamChiTiet, v.kichThuoc, v.mauSac]
      .join(" ").toLowerCase().includes(kw)
  ).slice(0, 50)
})

function addProductFromModal(variant) {
  newItem.value.spctId = variant.spctId
  newItem.value.soLuong = 1
  addProduct()
  showAddProductModal.value = false
  productSearchKeyword.value = ""
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

const firstNonEmptyValue = (...values) => {
  for (const value of values) {
    const normalized = String(value || "").trim()
    if (normalized) return normalized
  }
  return ""
}

const selectedCustomerEmail = computed(() => {
  return firstNonEmptyValue(
    hoaDon.value.customerEmail,
    selectedCustomer.value?.email,
    selectedCustomer.value?.taiKhoan?.email,
    selectedCustomer.value?.taiKhoanEmail,
    selectedCustomer.value?.tenDangNhap,
    selectedCustomer.value?.taiKhoan?.tenDangNhap
  )
})

const selectedCustomerLabel = computed(() => {
  if (selectedCustomer.value?.tenKhachHang) return selectedCustomer.value.tenKhachHang
  if (hoaDon.value.khachHangId != null) return `KH #${hoaDon.value.khachHangId}`
  return "Chưa chọn"
})

const orderTypeLabel = computed(() => {
  const explicit = String(hoaDon.value.orderType || "").toUpperCase()
  if (explicit === "POS") return "Tại quầy"
  if (explicit === "DELIVERY") return "Trực tuyến"
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
const selectedVariantPreview = computed(() => getVariant(newItem.value.spctId))

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

// GHN checkpoint availability (chỉ áp dụng cho đơn online, không phải POS)
const canGhnLayHang = computed(() =>
  !isCreate.value && !isPosOrder.value && !hoaDon.value.finalOrder
  && ["CHO_LAY_HANG", "DANG_GIAO"].includes(currentOrderCode.value)
)
const canGhnTrungChuyen = computed(() =>
  !isCreate.value && !isPosOrder.value && !hoaDon.value.finalOrder
  && ["CHO_LAY_HANG", "DANG_GIAO"].includes(currentOrderCode.value)
)
const canGhnGiaoThatBai = computed(() =>
  !isCreate.value && !isPosOrder.value && !hoaDon.value.finalOrder
  && ["DANG_GIAO", "GIAO_THAT_BAI"].includes(currentOrderCode.value)
)
const canGhnDangHoanVe = computed(() =>
  !isCreate.value && !isPosOrder.value && !hoaDon.value.finalOrder
  && ["GIAO_THAT_BAI", "HOAN_VE"].includes(currentOrderCode.value)
)
const hasAnyGhnCheckpoint = computed(() =>
  canGhnLayHang.value || canGhnTrungChuyen.value
  || canGhnGiaoThatBai.value || canGhnDangHoanVe.value
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

function normalizePhone(value) {
  return String(value || "").replace(/\s+/g, "")
}

function isValidVietnamPhone(value) {
  return /^(0|\+84)\d{9,10}$/.test(normalizePhone(value))
}

function resolveCustomerIdFromResponse(response) {
  const candidates = [
    response?.data?.id,
    response?.data?.data?.id,
    response?.data?.khachHang?.id,
    response?.data?.content?.id
  ]
  const found = candidates.find((id) => Number(id) > 0)
  return found ? Number(found) : null
}

function resetQuickCustomerForm() {
  quickCustomer.value = { tenKhachHang: "", soDienThoai: "", email: "" }
}

function extractVoucherSnapshotFromNote(note = "") {
  const text = String(note || "")
  const voucherMatch = text.match(/áp dụng voucher\s*([^|\n\r]+)/i)
  const phrase = voucherMatch?.[1] ? voucherMatch[1].trim() : ""

  if (!phrase) {
    return { code: "", name: "" }
  }

  const cleanPhrase = phrase.replace(/[\s:;,.-]+$/, "").trim()
  const codeMatch = cleanPhrase.match(/\b[A-Z0-9_-]{4,}\b/i)

  if (codeMatch?.[0]) {
    const code = String(codeMatch[0]).toUpperCase()
    const name = cleanPhrase.toUpperCase() === code ? "" : cleanPhrase
    return { code, name }
  }

  return { code: "", name: cleanPhrase }
}

async function quickCreateCustomer() {
  if (!canEdit.value) return

  const name = String(quickCustomer.value.tenKhachHang || "").trim()
  const phone = normalizePhone(quickCustomer.value.soDienThoai)
  const email = String(quickCustomer.value.email || "").trim()

  if (!name) {
    showToast("Vui lòng nhập tên khách hàng", "warning")
    return
  }
  if (!isValidVietnamPhone(phone)) {
    showToast("Số điện thoại khách hàng không hợp lệ", "warning")
    return
  }

  creatingCustomer.value = true
  try {
    const payloadCandidates = [
      { tenKhachHang: name, soDienThoai: phone, email, trangThai: "Hoạt động" },
      { tenKhachHang: name, soDienThoai: phone, taiKhoanEmail: email, trangThai: "Hoạt động" },
      { tenKhachHang: name, soDienThoai: phone, trangThai: "Hoạt động" }
    ]

    let createdId = null
    let lastError = null
    for (const payload of payloadCandidates) {
      try {
        const createRes = await createKhachHang(payload)
        createdId = resolveCustomerIdFromResponse(createRes)
        if (createdId) break
      } catch (error) {
        lastError = error
      }
    }

    const khachHangRes = await getAllKhachHang(0, 100)
    khachHangList.value = extractList(khachHangRes)

    if (!createdId) {
      const found = khachHangList.value.find((kh) =>
        normalizePhone(kh?.soDienThoai) === phone && String(kh?.tenKhachHang || "").trim() === name
      )
      createdId = found?.id ? Number(found.id) : null
    }

    if (!createdId) {
      throw lastError || new Error("Không lấy được khách hàng vừa tạo")
    }

    hoaDon.value.khachHangId = createdId
    if (isPosOrder.value) {
      hoaDon.value.soDienThoaiNhanHang = phone
    }

    showQuickCustomerForm.value = false
    resetQuickCustomerForm()
    showToast("Đã tạo và chọn khách hàng mới", "success")
  } catch (error) {
    showToast(extractApiErrorMessage(error, "Không thể tạo nhanh khách hàng"), "error")
  } finally {
    creatingCustomer.value = false
  }
}

function extractList(response) {
  if (isHtmlPayload(response?.data)) {
    throw new Error("API trả về trang đăng nhập thay vì JSON. Hãy khởi động lại backend sau khi áp dụng SecurityConfig.")
  }

  if (Array.isArray(response?.data)) return response.data
  if (Array.isArray(response?.data?.content)) return response.data.content
  return []
}

const resolveCurrentEmployeeContext = async () => {
  const storedUserRaw = localStorage.getItem("user") || sessionStorage.getItem("user")
  if (storedUserRaw) {
    try {
      const parsed = JSON.parse(storedUserRaw)
      if (parsed?.idNhanVien) return Number(parsed.idNhanVien)
      if (parsed?.id && parsed?.tenNhanVien) return Number(parsed.id)
    } catch {
      // Continue with fallback checks.
    }
  }

  const taiKhoanId = Number(localStorage.getItem("userId") || 0)
  if (taiKhoanId > 0) {
    try {
      const byTaiKhoan = await getNhanVienByTaiKhoanId(taiKhoanId)
      const rows = extractList(byTaiKhoan)
      const first = rows[0] || byTaiKhoan?.data || null
      if (first?.id) return Number(first.id)
    } catch {
      // Continue fallback checks.
    }

    const mapped = nhanVienList.value.find((item) => {
      const mappedTaiKhoanId = Number(item?.idTaiKhoan || item?.taiKhoan?.id || 0)
      return mappedTaiKhoanId === taiKhoanId
    })
    if (mapped?.id) return Number(mapped.id)
  }

  return null
}

const enforceEmployeeOwnership = () => {
  if (!isEmployeePanel.value || !currentEmployeeId.value) return
  hoaDon.value.nhanVienId = Number(currentEmployeeId.value)
}

const canOperateForEmployeeShift = async (employeeId) => {
  // TEMP DEMO MODE: turn off shift validation so all employees can operate.
  // const check = await validateEmployeeActiveShift(employeeId)
  // if (!check.allowed) {
  //   showToast(check.reason || "Nhân viên chưa trong ca trực hợp lệ", "warning")
  //   return false
  // }
  return true
}

function buildVariantLabel(product, variant) {
  const parts = [product?.tenSanPham]
  if (variant?.kichThuoc?.tenKichThuoc) parts.push(`Size ${variant.kichThuoc.tenKichThuoc}`)
  if (variant?.mauSac?.tenMauSac) parts.push(variant.mauSac.tenMauSac)
  if (variant?.tenSanPhamChiTiet) parts.push(variant.tenSanPhamChiTiet)
  return parts.filter(Boolean).join(" • ")
}

function isImageString(value = "") {
  const raw = String(value || "").trim()
  if (!raw) return false
  if (/^data:image\//i.test(raw)) return true

  const normalized = raw.replace(/\\/g, "/").split(/[?#]/)[0]
  if (/\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i.test(normalized)) return true
  if (normalized.startsWith("/uploads/") || normalized.startsWith("uploads/")) return true
  return /^https?:\/\//i.test(raw)
}

function toImageUrl(value = "") {
  const raw = String(value || "").trim()
  if (!raw) return ""
  if (/^data:image\//i.test(raw)) return raw

  const normalized = raw.replace(/\\/g, "/")
  const uploadsMatch = normalized.match(/^.*?\/?(uploads\/.*)$/i)

  if (uploadsMatch?.[1]) return `${BACKEND_ORIGIN}/${uploadsMatch[1]}`
  if (/^https?:\/\//i.test(normalized)) return normalized
  if (normalized.startsWith("/")) return normalized
  if (normalized.startsWith("uploads/")) return `${BACKEND_ORIGIN}/${normalized}`
  return normalized.includes("/") ? `/${normalized.replace(/^\/+/, "")}` : normalized
}

function pickImageValue(entry) {
  if (!entry) return ""

  if (typeof entry === "string") {
    return isImageString(entry) ? toImageUrl(entry) : ""
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

function flattenVariants(products) {
  return products.flatMap((product) => {
    const variants = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []

    if (!variants.length) {
      return []
    }

    const overrideImage = getProductImageOverride({ id: product?.id, maSanPham: product?.maSanPham })[0] || ""

    return variants.map((variant) => ({
      spctId: variant.id,
      productId: product.id,
      maSanPham: product.maSanPham || "",
      maSanPhamChiTiet: variant.ma || "",
      tenSanPhamChiTiet: buildVariantLabel(product, variant),
      giaBan: Number(variant.giaBan || 0),
      soLuongTon: Number(variant.soLuong || 0),
      kichThuoc: variant?.kichThuoc?.tenKichThuoc || "",
      mauSac: variant?.mauSac?.tenMauSac || "",
      image: overrideImage || pickImageValue([variant, product, variants]) || fallbackImageFor(product?.id, product?.maSanPham)
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

function getItemImage(item) {
  const raw = String(item?.image || getVariant(item?.spctId)?.image || "").trim()
  if (raw) {
    const resolved = toImageUrl(raw)
    if (resolved) return resolved
  }
  return fallbackImageFor(item?.spctId, item?.maSanPhamChiTiet || item?.maSanPham)
}

const onImgError = (e) => { e.target.src = logoFallback }

function incrementItemQty(index) {
  const item = items.value[index]
  if (!item || !canEdit.value) return
  changeItemQuantity(index, Number(item.soLuong || 0) + 1)
}

function decrementItemQty(index) {
  const item = items.value[index]
  if (!item || !canEdit.value) return
  const next = Number(item.soLuong || 0) - 1
  if (next >= 1) changeItemQuantity(index, next)
}

function applyDiscount(discount) {
  hoaDon.value.giaSauGiamGia = Number(discount || 0)
  syncTotals()
}

async function dispatchSystemEvent(eventCode, note, successMessage) {
  // TEMP DEMO MODE: disable employee ownership + shift constraints.
  // if (isEmployeePanel.value) {
  //   if (!currentEmployeeId.value || Number(hoaDon.value.nhanVienId) !== Number(currentEmployeeId.value)) {
  //     showToast("Bạn không có quyền thao tác hóa đơn của nhân viên khác", "error")
  //     return
  //   }
  //
  //   const canOperate = await canOperateForEmployeeShift(hoaDon.value.nhanVienId)
  //   if (!canOperate) return
  // }

  isSaving.value = true
  try {
    const maHoaDon = String(hoaDon.value.maHoaDon || "").trim()
    const trackingUrl = maHoaDon ? buildOrderLookupTrackingUrl({ maHoaDon }) : ""

    const response = await updateHoaDonBySystemEvent(hoaDon.value.id, eventCode, note, trackingUrl)
    applyInvoiceDetail(response?.data)
    showToast(successMessage || "Đã cập nhật trạng thái đơn hàng", "success")

    // Auto-send tracking mail so backend has the current public URL
    const email = String(selectedCustomerEmail.value || "").trim()
    if (maHoaDon && email) {
      sendOrderLookupMail({
        maHoaDon,
        soDienThoai: String(hoaDon.value.soDienThoaiNhanHang || "").trim(),
        email,
        trackingUrl
      }).catch(() => { /* silent */ })
    }
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

async function dispatchGhnCheckpoint(eventCode, label) {
  const confirmed = await askConfirm(`Ghi checkpoint GHN: "${label}"?`)
  if (!confirmed) return
  await dispatchSystemEvent(eventCode, label, `Đã ghi checkpoint: ${label}`)
}

async function sendLookupMailNow() {
  if (isCreate.value || !hoaDon.value.id) {
    showToast("Chỉ gửi mail tra cứu sau khi hóa đơn đã được tạo", "warning")
    return
  }

  const maHoaDon = String(hoaDon.value.maHoaDon || "").trim()
  const soDienThoai = String(hoaDon.value.soDienThoaiNhanHang || "").trim()
  const email = String(selectedCustomerEmail.value || "").trim()

  if (!maHoaDon || !soDienThoai) {
    showToast("Thiếu mã đơn hoặc số điện thoại nhận hàng", "warning")
    return
  }

  if (!email) {
    showToast("Không tìm thấy email khách hàng để gửi link tra cứu", "warning")
    return
  }

  const trackingUrl = buildOrderLookupTrackingUrl({ maHoaDon })

  isSendingLookupMail.value = true
  try {
    const payload = {
      maHoaDon,
      soDienThoai,
      email
    }
    if (trackingUrl) payload.trackingUrl = trackingUrl

    const response = await sendOrderLookupMail(payload)
    showToast(response?.data?.message || "Đã gửi mail tra cứu cho khách hàng", "success")
  } catch (error) {
    showToast(extractApiErrorMessage(error, "Không thể gửi mail tra cứu"), "error")
  } finally {
    isSendingLookupMail.value = false
  }
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
      tenSanPhamChiTiet: variant?.tenSanPhamChiTiet || item.tenSanPhamChiTiet || "",
      image: item.image || variant?.image || ""
    }
  })
}

function resolvePersistedDiscount(row, normalizedItems, orderTypeCode) {
  const explicitDiscount = Number(row?.giaSauGiamGia || 0)
  if (explicitDiscount > 0) return explicitDiscount

  const subtotalFromItems = normalizedItems.reduce((sum, item) => sum + Number(item?.thanhTien || 0), 0)
  const shipFee = String(orderTypeCode || "").toUpperCase() === "POS"
    ? 0
    : Number(row?.phiShip || 0)
  const savedGrandTotal = Number(row?.thanhTien || 0)
  const inferredDiscount = subtotalFromItems + shipFee - savedGrandTotal

  return inferredDiscount > 0 ? inferredDiscount : 0
}

function resolvePersistedShipping(row, safeDetail, normalizedItems, orderTypeCode) {
  if (String(orderTypeCode || "").toUpperCase() === "POS") return 0

  const explicitCandidates = [
    row?.phiShip,
    row?.shippingFee,
    row?.phiVanChuyen,
    safeDetail?.totals?.shippingFee,
    safeDetail?.totals?.shipFee,
    safeDetail?.totals?.phiShip
  ]

  for (const candidate of explicitCandidates) {
    const value = Number(candidate)
    if (Number.isFinite(value) && value > 0) return value
  }

  const subtotalFromItems = normalizedItems.reduce((sum, item) => sum + Number(item?.thanhTien || 0), 0)
  const discount = Math.max(Number(row?.giaSauGiamGia ?? safeDetail?.totals?.discount ?? 0), 0)
  const grandTotal = Number(row?.thanhTien ?? safeDetail?.totals?.grandTotal ?? 0)
  const inferredShipping = grandTotal - subtotalFromItems + discount

  return inferredShipping > 0 ? inferredShipping : 0
}

function applyInvoiceDetail(detail) {
  const safeDetail = detail || {}
  const row = safeDetail?.hoaDon || {}
  const customerRow = safeDetail?.customer || row?.khachHang || {}

  const normalizedHistory = normalizeHistoryEntries(safeDetail?.history, row)
  const latestHistoryNote = [...normalizedHistory]
    .reverse()
    .find((entry) => entry?.note && entry.note !== "Không có ghi chú")?.note || ""

  const normalizedOrderType = String(
    row.orderType
    || (String(row.statusNote || "").toUpperCase().includes("[POS]")
      || String(row.diaChiNhanHang || "").toLowerCase().includes("mua tại quầy")
      || String(row.diaChiNhanHang || "").toLowerCase().includes("mua tai quay")
      ? "POS"
      : "ONLINE")
  ).toUpperCase()

  const normalizedItems = normalizeLoadedItems(safeDetail?.items)
  const persistedShipping = resolvePersistedShipping(row, safeDetail, normalizedItems, normalizedOrderType)
  const persistedDiscount = resolvePersistedDiscount(row, normalizedItems, normalizedOrderType)
  const voucherSnapshot = extractVoucherSnapshotFromNote(row.statusNote || latestHistoryNote)

  hoaDon.value = {
    id: row.id,
    maHoaDon: row.maHoaDon || "",
    nhanVienId: row.nhanVienId != null ? Number(row.nhanVienId) : null,
    khachHangId: row.khachHangId != null ? Number(row.khachHangId) : null,
    soDienThoaiNhanHang: row.soDienThoaiNhanHang || "",
    diaChiNhanHang: row.diaChiNhanHang || "",
    ngayNhanHangDuKien: formatDateInput(row.ngayNhanHangDuKien),
    ngayNhanHangMongMuon: formatDateInput(row.ngayNhanHangMongMuon),
    phiShip: persistedShipping,
    giaSauGiamGia: persistedDiscount,
    thanhTien: Number(row.thanhTien || 0),
    orderStatusCode: normalizeOrderStatusCode(row.orderStatusCode, row.orderStatusName, row.statusNote),
    orderStatusName: row.orderStatusName || "Chờ xác nhận",
    orderType: normalizedOrderType,
    statusNote: row.statusNote || latestHistoryNote,
    phuongThucThanhToan: row.phuongThucThanhToan || "COD",
    customerEmail: firstNonEmptyValue(
      row.customerEmail,
      row.emailKhachHang,
      row.emailNguoiNhan,
      customerRow?.email,
      customerRow?.taiKhoan?.email
    ),
    fulfillmentStatusCode: row.fulfillmentStatusCode || deriveFulfillmentStatus(row.orderStatusCode).code,
    fulfillmentStatusName: row.fulfillmentStatusName || deriveFulfillmentStatus(row.orderStatusCode).label,
    businessClosureStatus: row.businessClosureStatus || deriveBusinessClosure(row.orderStatusCode),
    businessClosureStatusName: row.businessClosureStatusName || deriveBusinessClosureName(row.orderStatusCode),
    finalOrder: Boolean(safeDetail?.finalOrder)
  }

  manualStatusNote.value = hoaDon.value.statusNote || ""
  persistedVoucherCode.value = persistedDiscount > 0 ? voucherSnapshot.code : ""
  persistedVoucherName.value = persistedDiscount > 0 ? voucherSnapshot.name : ""
  items.value = normalizedItems
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
  employeeOwnershipMismatch.value = false

  try {
    await loadReferenceData()
    if (isEmployeePanel.value) {
      currentEmployeeId.value = await resolveCurrentEmployeeContext()
      if (!currentEmployeeId.value) {
        throw new Error("Không xác định được nhân viên đăng nhập. Vui lòng đăng nhập lại.")
      }
    }
    await loadInvoice()

    // TEMP DEMO MODE: disable cross-employee ownership restriction.
    // if (
    //   isEmployeePanel.value
    //   && !isCreate.value
    //   && Number(hoaDon.value.nhanVienId || 0) > 0
    //   && Number(hoaDon.value.nhanVienId) !== Number(currentEmployeeId.value)
    // ) {
    //   employeeOwnershipMismatch.value = true
    //   apiWarning.value = "Bạn không có quyền chỉnh sửa hóa đơn của nhân viên khác."
    // }

    enforceEmployeeOwnership()
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
      tenSanPhamChiTiet: variant.tenSanPhamChiTiet,
      image: variant.image || ""
    })
  }

  newItem.value = { spctId: Number(variant.spctId), soLuong: 1 }
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
  // TEMP DEMO MODE: allow saving regardless of employee ownership.
  // if (employeeOwnershipMismatch.value) {
  //   showToast("Bạn không có quyền lưu hóa đơn của nhân viên khác", "error")
  //   return
  // }

  // TEMP DEMO MODE: keep employee editable and do not force owner.
  // if (isEmployeePanel.value) {
  //   if (!currentEmployeeId.value) {
  //     showToast("Không xác định được nhân viên đăng nhập", "error")
  //     return
  //   }
  //   hoaDon.value.nhanVienId = Number(currentEmployeeId.value)
  // }

  if (!hoaDon.value.nhanVienId) {
    showToast("Vui lòng chọn nhân viên", "warning")
    return
  }

  // TEMP DEMO MODE: disable shift guard.
  // const canOperate = await canOperateForEmployeeShift(hoaDon.value.nhanVienId)
  // if (isEmployeePanel.value && !canOperate) return

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
  (next, prev) => {
    const normalized = String(next || "ONLINE").toUpperCase()
    const prevNormalized = String(prev || "ONLINE").toUpperCase()

    if (normalized === "POS") {
      hoaDon.value.phiShip = 0
      hoaDon.value.ngayNhanHangDuKien = ""
      hoaDon.value.ngayNhanHangMongMuon = ""
      hoaDon.value.diaChiNhanHang = "Mua tại quầy"
      hoaDon.value.soDienThoaiNhanHang = selectedCustomer.value?.soDienThoai || ""

      if (isCreate.value && prevNormalized !== "POS") {
        hoaDon.value.giaSauGiamGia = 0
        selectedVoucher.value = null
      }

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
          <button
            v-if="!isCreate"
            class="btn lookup-mail"
            type="button"
            :disabled="isSendingLookupMail"
            @click="sendLookupMailNow"
          >
            <Loader2 v-if="isSendingLookupMail" :size="16" class="spin" />
            <span>{{ isSendingLookupMail ? "Đang gửi mail" : "Gửi mail tra cứu" }}</span>
          </button>
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
                <select class="strong-select" v-model.number="hoaDon.nhanVienId" :disabled="!canEdit || isEmployeePanel">
                  <option :value="null">Chọn nhân viên</option>
                  <option v-for="nhanVien in nhanVienList" :key="nhanVien.id" :value="Number(nhanVien.id)">
                    {{ nhanVien.tenNhanVien || nhanVien.hoTen || `NV #${nhanVien.id}` }}
                  </option>
                </select>
              </label>

              <div class="field field-stack">
                <span>Khách hàng</span>
                <select class="strong-select" v-model.number="hoaDon.khachHangId" :disabled="!canEdit">
                  <option :value="null">Chọn khách hàng</option>
                  <option v-for="khachHang in khachHangList" :key="khachHang.id" :value="Number(khachHang.id)">
                    {{ khachHang.tenKhachHang || `KH #${khachHang.id}` }}
                  </option>
                </select>
                <button
                  class="btn ghost quick-customer-toggle"
                  type="button"
                  :disabled="!canEdit || creatingCustomer"
                  @click="showQuickCustomerForm = !showQuickCustomerForm"
                >
                  {{ showQuickCustomerForm ? "Ẩn tạo nhanh" : "Tạo nhanh khách hàng" }}
                </button>
                <div v-if="showQuickCustomerForm" class="quick-customer-form">
                  <input
                    v-model="quickCustomer.tenKhachHang"
                    type="text"
                    placeholder="Tên khách hàng"
                    :disabled="!canEdit || creatingCustomer"
                  />
                  <input
                    v-model="quickCustomer.soDienThoai"
                    type="text"
                    placeholder="Số điện thoại"
                    :disabled="!canEdit || creatingCustomer"
                  />
                  <input
                    v-model="quickCustomer.email"
                    type="email"
                    placeholder="Email (tùy chọn)"
                    :disabled="!canEdit || creatingCustomer"
                  />
                  <div class="quick-customer-actions">
                    <button
                      class="btn ghost"
                      type="button"
                      :disabled="!canEdit || creatingCustomer"
                      @click="resetQuickCustomerForm"
                    >
                      Làm mới
                    </button>
                    <button
                      class="btn primary"
                      type="button"
                      :disabled="!canEdit || creatingCustomer"
                      @click="quickCreateCustomer"
                    >
                      {{ creatingCustomer ? "Đang tạo" : "Tạo và chọn" }}
                    </button>
                  </div>
                </div>
              </div>

              <label class="field full">
                <span>Email khách hàng</span>
                <input :value="selectedCustomerEmail || 'Chưa có email khách hàng'" type="text" disabled />
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
                  <option value="DELIVERY">Giao hàng</option>
                  <option value="POS">Bán hàng tại quầy</option>
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
                    <option value="CASH">Tiền mặt</option>
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
                  :auto-select="isCreate"
                  :initial-voucher-code="persistedVoucherCode"
                  :initial-voucher-name="persistedVoucherName"
                  :initial-discount="hoaDon.giaSauGiamGia"
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
            </div>

            <!-- ── Order Timeline ── -->
            <div v-if="!isCreate" class="order-timeline">
              <div class="timeline-track">
                <div class="timeline-bg-line"></div>
                <div class="timeline-progress-line" :style="{ width: timelineProgressPercent + '%' }"></div>
                <div
                  v-for="(step, idx) in timelineSteps"
                  :key="step.code"
                  class="timeline-step"
                  :class="{
                    done: timelineCurrentIndex >= idx && !isTerminalStatus,
                    active: timelineCurrentIndex === idx && !isTerminalStatus,
                    dimmed: isTerminalStatus
                  }"
                >
                  <div class="timeline-dot">
                    <CheckCircle2 v-if="timelineCurrentIndex > idx && !isTerminalStatus" :size="16" />
                    <component v-else :is="{ ClipboardList, Package2, Truck, CheckCircle2 }[step.icon]" :size="16" />
                  </div>
                  <span class="timeline-label">{{ step.label }}</span>
                </div>
              </div>
              <div v-if="isTerminalStatus" class="timeline-terminal-badge" :class="`status-${currentStatusTone}`">
                <span class="material-icons-outlined" style="font-size:16px">{{ currentOrderCode === 'HUY' ? 'cancel' : 'replay' }}</span>
                {{ currentStatusName }}
              </div>
            </div>

            <div class="status-actions" v-if="!isCreate">
              <p class="status-hint">Trạng thái đơn hàng chỉ được cập nhật qua sự kiện hệ thống giao hàng/thanh toán.</p>
              <p v-if="hoaDon.finalOrder" class="status-hint">Hóa đơn đã kết thúc nên không thể nhận thêm sự kiện.</p>
              <div class="status-btn-row">
                <button v-if="canConfirmOnlineOrder" class="btn primary" type="button" :disabled="isSaving" @click="confirmOnlineOrder">
                  <CheckCircle2 :size="16" /><span>Xác nhận đơn hàng</span>
                </button>
                <button v-if="canStartShipping" class="btn primary" type="button" :disabled="isSaving" @click="startShipping">
                  <Truck :size="16" /><span>Bắt đầu giao hàng</span>
                </button>
                <button v-if="canQuickComplete" class="btn primary" type="button" :disabled="isSaving" @click="quickCompleteOrder">
                  <CheckCircle2 :size="16" /><span>Xác nhận hoàn thành</span>
                </button>
                <button v-if="canMarkReturned" class="btn danger" type="button" :disabled="isSaving" @click="markReturned">
                  <span>Xác nhận hoàn về</span>
                </button>
              </div>
            </div>

            <!-- GHN Checkpoint sub-events: ghi milestone vận chuyển chi tiết -->
            <div class="ghn-checkpoint-card" v-if="hasAnyGhnCheckpoint">
              <p class="ghn-checkpoint-title">Cập nhật checkpoint GHN</p>
              <p class="ghn-checkpoint-hint">Ghi mốc vận chuyển chi tiết theo GHN — không thay đổi trạng thái chính, chỉ cập nhật timeline theo dõi.</p>
              <div class="ghn-checkpoint-actions">
                <button
                  v-if="canGhnLayHang"
                  class="btn ghost small"
                  type="button"
                  :disabled="isSaving"
                  @click="dispatchGhnCheckpoint('GHN_LAY_HANG', 'Shipper đã lấy hàng từ shop')"
                ><PackageCheck :size="14" /><span>Đã lấy hàng</span></button>
                <button
                  v-if="canGhnTrungChuyen"
                  class="btn ghost small"
                  type="button"
                  :disabled="isSaving"
                  @click="dispatchGhnCheckpoint('GHN_TRUNG_CHUYEN', 'Hàng đang trung chuyển / tại kho chia')"
                ><PackageSearch :size="14" /><span>Đang trung chuyển</span></button>
                <button
                  v-if="canGhnGiaoThatBai"
                  class="btn ghost small"
                  type="button"
                  :disabled="isSaving"
                  @click="dispatchGhnCheckpoint('GHN_GIAO_THAT_BAI', 'Giao hàng không thành công')"
                ><OctagonX :size="14" /><span>Giao thất bại (GHN)</span></button>
                <button
                  v-if="canGhnDangHoanVe"
                  class="btn ghost small"
                  type="button"
                  :disabled="isSaving"
                  @click="dispatchGhnCheckpoint('GHN_DANG_HOAN_VE', 'Shipper đang trên đường hoàn hàng về shop')"
                ><RotateCcw :size="14" /><span>Đang hoàn về shop</span></button>
              </div>
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
              <div v-if="!history.length" class="history-empty">Chưa có lịch sử trạng thái.</div>
              <div v-else class="history-timeline">
                <div v-for="(entry, index) in history" :key="`${entry.changedAt || 'history'}-${index}`" class="history-entry">
                  <div class="history-dot" :class="{ first: index === 0 }"></div>
                  <div class="history-content">
                    <div class="history-status-row">
                      <span class="history-from">{{ entry.fromStatus }}</span>
                      <span class="history-arrow">→</span>
                      <span class="history-to">{{ entry.toStatus }}</span>
                    </div>
                    <p v-if="entry.note" class="history-note">{{ entry.note }}</p>
                    <time class="history-time">{{ formatDateTime(entry.changedAt) }}</time>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>

        <section class="panel wide-panel">
          <div class="panel-head space-between">
            <div>
              <h2>Sản phẩm trong hoá đơn</h2>
              <p>{{ items.length }} sản phẩm · Tạm tính {{ formatCurrency(subtotal) }}</p>
            </div>
            <button v-if="canEdit" class="btn primary" type="button" @click="showAddProductModal = true">
              <Plus :size="16" /><span>Thêm sản phẩm</span>
            </button>
          </div>

          <!-- Product cards grid -->
          <div v-if="!items.length" class="product-empty-state">
            <ShoppingBag :size="40" />
            <p>Chưa có sản phẩm nào trong hoá đơn</p>
            <button v-if="canEdit" class="btn ghost" type="button" @click="showAddProductModal = true">
              <Plus :size="16" /><span>Thêm sản phẩm đầu tiên</span>
            </button>
          </div>

          <div v-else class="product-cards">
            <div v-for="(item, index) in items" :key="`${item.spctId}-${index}`" class="product-card">
              <div class="product-card-img">
                <img :src="getItemImage(item)" :alt="getItemName(item)" @error="onImgError" />
              </div>
              <div class="product-card-body">
                <div class="product-card-name">{{ getItemName(item) }}</div>
                <div class="product-card-meta">
                  <small v-if="item.maSanPham || item.maSanPhamChiTiet || getVariant(item.spctId)">
                    {{ getItemCodeMeta(item).productCode }} / {{ getItemCodeMeta(item).variantCode }}
                  </small>
                  <small v-else>SPCT #{{ item.spctId }}</small>
                </div>
                <div class="product-card-price">{{ formatCurrency(item.giaBan) }}</div>
              </div>
              <div class="product-card-actions">
                <div class="product-card-qty">
                  <button v-if="canEdit" class="qty-btn" type="button" @click="decrementItemQty(index)" :disabled="item.soLuong <= 1">
                    <Minus :size="14" />
                  </button>
                  <span class="qty-value">{{ item.soLuong }}</span>
                  <button v-if="canEdit" class="qty-btn" type="button" @click="incrementItemQty(index)">
                    <Plus :size="14" />
                  </button>
                </div>
                <div class="product-card-total">{{ formatCurrency(item.thanhTien) }}</div>
                <button v-if="canEdit" class="icon-btn" type="button" @click="removeItem(index)">
                  <Trash2 :size="16" />
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- ── Add Product Modal ── -->
        <Teleport to="body">
          <Transition name="modal-fade">
            <div v-if="showAddProductModal" class="modal-overlay" @click.self="showAddProductModal = false">
              <div class="modal-content">
                <div class="modal-header">
                  <h2>Thêm sản phẩm</h2>
                  <button class="modal-close" @click="showAddProductModal = false"><X :size="20" /></button>
                </div>
                <div class="modal-search">
                  <Search :size="18" class="modal-search-icon" />
                  <input
                    v-model="productSearchKeyword"
                    type="text"
                    placeholder="Tìm theo tên, mã sản phẩm, màu, size..."
                    class="modal-search-input"
                  />
                </div>
                <div class="modal-product-list">
                  <div v-if="!filteredProductVariants.length" class="modal-empty">Không tìm thấy sản phẩm phù hợp</div>
                  <div
                    v-for="variant in filteredProductVariants"
                    :key="variant.spctId"
                    class="modal-product-item"
                    @click="addProductFromModal(variant)"
                  >
                    <div class="modal-product-img">
                      <img :src="variant.image || logoFallback" :alt="variant.tenSanPhamChiTiet" @error="onImgError" />
                    </div>
                    <div class="modal-product-info">
                      <div class="modal-product-name">{{ variant.tenSanPhamChiTiet }}</div>
                      <div class="modal-product-meta">
                        <span v-if="variant.mauSac">{{ variant.mauSac }}</span>
                        <span v-if="variant.kichThuoc">{{ variant.kichThuoc }}</span>
                        <span>Tồn: {{ variant.soLuongTon }}</span>
                      </div>
                    </div>
                    <div class="modal-product-price">{{ formatCurrency(variant.giaBan) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </Transition>
        </Teleport>
      </template>
    </div>
  </main>
</template>

<style scoped>
.hoa-don-page {
  padding: 24px;
  background: transparent;
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
  background: #ffffff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
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
  color: #475569;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
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

.btn.lookup-mail {
  background: #fff;
  color: #0f172a;
  border: 1px solid #dbe2ea;
}

.btn.lookup-mail:hover:not(:disabled) {
  border-color: #f1b3be;
  background: #fff7f8;
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
  color: #b91c1c;
  background: #fff5f5;
  border-color: #fecaca;
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
.metric-icon.gold { background: #f3f4f6; color: #374151; }
.metric-icon.teal { background: #f3f4f6; color: #374151; }
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
  background: #ffffff;
}

.panel.contrast {
  background: #ffffff;
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
  align-content: start;
}

.field-stack {
  align-content: start;
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

.quick-customer-toggle {
  justify-content: flex-start;
  min-height: auto;
  width: auto;
  padding: 5px 10px;
  font-size: 12px;
}

.quick-customer-form {
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
  padding: 12px;
  display: grid;
  gap: 8px;
  background: #f8fafc;
}

.quick-customer-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.status-badge {
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  border: 1px solid;
}

.status-success {
  background: #f3f4f6;
  color: #111827;
  border-color: #d1d5db;
}

.status-warning {
  background: #fff1f2;
  color: #b91c1c;
  border-color: #fecdd3;
}

.status-danger {
  background: #fee2e2;
  color: #991b1b;
  border-color: #fecaca;
}

.status-actions {
  margin-bottom: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.ghn-checkpoint-card {
  margin-bottom: 14px;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.ghn-checkpoint-title {
  font-weight: 600;
  font-size: 13px;
  color: #0f172a;
  margin: 0 0 4px 0;
}

.ghn-checkpoint-hint {
  font-size: 12px;
  color: #64748b;
  margin: 0 0 8px 0;
}

.ghn-checkpoint-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ghn-checkpoint-actions .btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn.small {
  font-size: 12px;
  padding: 4px 10px;
  height: auto;
}

.status-action {
  border-width: 1px;
  border-style: solid;
  box-shadow: none;
}

.status-action-success {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #334155;
}

.status-action-warning {
  background: #fff5f5;
  border-color: #fecaca;
  color: #b91c1c;
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
  background: #fff;
  border: 1px solid #e5e7eb;
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
  border-top: 1px dashed #d1d5db;
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
  color: #111827;
}

.payment-warning {
  color: #b91c1c;
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

.selected-variant-preview {
  margin: -6px 0 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.selected-variant-meta {
  display: grid;
  gap: 2px;
}

.selected-variant-meta strong {
  font-size: 14px;
  color: #0f172a;
}

.selected-variant-meta small {
  font-size: 12px;
  color: #64748b;
}

.variant-thumb {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #e2e8f0;
  background: #fff;
  flex-shrink: 0;
}

.item-main {
  display: flex;
  align-items: flex-start;
  gap: 10px;
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

/* ── Order Timeline ── */
.order-timeline {
  margin-bottom: 20px;
  padding: 20px 16px 12px;
  background: linear-gradient(135deg, #fafafa, #f8f9fb);
  border-radius: 16px;
  border: 1px solid #e5e7eb;
}
.timeline-track {
  display: flex;
  justify-content: space-between;
  position: relative;
  padding: 0 8px;
}
.timeline-bg-line {
  position: absolute;
  top: 18px;
  left: 28px;
  right: 28px;
  height: 3px;
  background: #e5e7eb;
  border-radius: 2px;
  z-index: 0;
}
.timeline-progress-line {
  position: absolute;
  top: 18px;
  left: 28px;
  height: 3px;
  background: linear-gradient(90deg, #dc2626, #ef4444);
  border-radius: 2px;
  z-index: 1;
  transition: width 0.6s cubic-bezier(.25,.8,.25,1);
}
.timeline-step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  z-index: 2;
}
.timeline-dot {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #fff;
  border: 2px solid #d1d5db;
  color: #9ca3af;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.timeline-step.done .timeline-dot {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  border-color: #dc2626;
  color: #fff;
  box-shadow: 0 2px 8px rgba(220,38,38,0.25);
}
.timeline-step.active .timeline-dot {
  background: #fff;
  border-color: #dc2626;
  color: #dc2626;
  box-shadow: 0 0 0 4px rgba(220,38,38,0.12), 0 2px 8px rgba(220,38,38,0.15);
  animation: pulse-ring 2s ease infinite;
}
.timeline-step.dimmed .timeline-dot {
  opacity: 0.4;
}
.timeline-label {
  margin-top: 8px;
  font-size: 11px;
  font-weight: 700;
  color: #9ca3af;
  text-align: center;
  line-height: 1.3;
}
.timeline-step.done .timeline-label,
.timeline-step.active .timeline-label {
  color: #dc2626;
}
.timeline-step.dimmed .timeline-label {
  opacity: 0.4;
}
.timeline-terminal-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
  margin-top: 12px;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  width: fit-content;
  margin-left: auto;
  margin-right: auto;
}
@keyframes pulse-ring {
  0%, 100% { box-shadow: 0 0 0 4px rgba(220,38,38,0.12), 0 2px 8px rgba(220,38,38,0.15); }
  50% { box-shadow: 0 0 0 8px rgba(220,38,38,0.06), 0 2px 8px rgba(220,38,38,0.15); }
}

/* ── Status button row ── */
.status-btn-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

/* ── History vertical timeline ── */
.history-empty {
  text-align: center;
  color: #94a3b8;
  padding: 16px;
  font-size: 13px;
}
.history-timeline {
  position: relative;
  padding-left: 24px;
}
.history-timeline::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 6px;
  bottom: 6px;
  width: 2px;
  background: #e5e7eb;
  border-radius: 1px;
}
.history-entry {
  position: relative;
  padding: 0 0 16px 0;
}
.history-entry:last-child {
  padding-bottom: 0;
}
.history-dot {
  position: absolute;
  left: -20px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d1d5db;
  border: 2px solid #fff;
}
.history-dot.first {
  background: #dc2626;
  box-shadow: 0 0 0 3px rgba(220,38,38,0.15);
}
.history-content {
  display: grid;
  gap: 2px;
}
.history-status-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
}
.history-from { color: #64748b; }
.history-arrow { color: #9ca3af; font-size: 11px; }
.history-to { color: #111827; }
.history-note { margin: 0; font-size: 12px; color: #64748b; }
.history-time { font-size: 11px; color: #94a3b8; }

/* ── Product cards ── */
.product-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 20px;
  color: #94a3b8;
}
.product-empty-state p {
  margin: 0;
  font-weight: 600;
}
.product-cards {
  display: grid;
  gap: 10px;
}
.product-card {
  display: grid;
  grid-template-columns: 64px 1fr auto;
  gap: 14px;
  align-items: center;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid #eef2f7;
  background: #fafbfc;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.product-card:hover {
  border-color: #dbe2ea;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.product-card-img {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  overflow: hidden;
  background: #f1f5f9;
  flex-shrink: 0;
}
.product-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.product-card-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: #94a3b8;
}
.product-card-body {
  display: grid;
  gap: 2px;
  min-width: 0;
}
.product-card-name {
  font-weight: 700;
  color: #111827;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-card-meta small {
  color: #64748b;
  font-size: 12px;
}
.product-card-price {
  font-weight: 800;
  color: #b91c1c;
  font-size: 14px;
}
.product-card-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.product-card-qty {
  display: flex;
  align-items: center;
  gap: 0;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}
.qty-btn {
  width: 30px;
  height: 30px;
  border: none;
  background: #f9fafb;
  color: #374151;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.15s;
}
.qty-btn:hover:not(:disabled) {
  background: #f1f5f9;
}
.qty-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.qty-value {
  width: 36px;
  text-align: center;
  font-weight: 700;
  font-size: 14px;
  color: #111827;
  border-left: 1px solid #d1d5db;
  border-right: 1px solid #d1d5db;
  line-height: 30px;
}
.product-card-total {
  font-weight: 800;
  color: #111827;
  font-size: 14px;
  min-width: 90px;
  text-align: right;
}

/* ── Modal ── */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15,23,42,0.5);
  backdrop-filter: blur(4px);
  display: grid;
  place-items: center;
  z-index: 9999;
  padding: 20px;
}
.modal-content {
  background: #fff;
  border-radius: 20px;
  width: 100%;
  max-width: 640px;
  max-height: 80vh;
  display: grid;
  grid-template-rows: auto auto 1fr;
  box-shadow: 0 24px 60px rgba(15,23,42,0.2);
  overflow: hidden;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}
.modal-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}
.modal-close {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: #f1f5f9;
  color: #475569;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.2s;
}
.modal-close:hover {
  background: #fee2e2;
  color: #dc2626;
}
.modal-search {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 24px;
  border-bottom: 1px solid #eef2f7;
}
.modal-search-icon {
  color: #94a3b8;
  flex-shrink: 0;
}
.modal-search-input {
  width: 100%;
  border: none;
  outline: none;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  background: transparent;
}
.modal-search-input::placeholder {
  color: #94a3b8;
}
.modal-product-list {
  overflow-y: auto;
  padding: 8px;
}
.modal-empty {
  text-align: center;
  padding: 32px;
  color: #94a3b8;
  font-weight: 600;
}
.modal-product-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s;
}
.modal-product-item:hover {
  background: #fef2f2;
}
.modal-product-img {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  overflow: hidden;
  background: #f1f5f9;
  flex-shrink: 0;
}
.modal-product-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.modal-product-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: #94a3b8;
}
.modal-product-info {
  flex: 1;
  min-width: 0;
}
.modal-product-name {
  font-weight: 700;
  color: #111827;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.modal-product-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}
.modal-product-meta span {
  font-weight: 600;
}
.modal-product-price {
  font-weight: 800;
  color: #b91c1c;
  font-size: 14px;
  flex-shrink: 0;
}

/* ── Modal transition ── */
.modal-fade-enter-active {
  transition: opacity 0.2s ease;
}
.modal-fade-enter-active .modal-content {
  transition: transform 0.25s cubic-bezier(.25,.8,.25,1), opacity 0.2s ease;
}
.modal-fade-leave-active {
  transition: opacity 0.15s ease;
}
.modal-fade-leave-active .modal-content {
  transition: transform 0.15s ease, opacity 0.15s ease;
}
.modal-fade-enter-from {
  opacity: 0;
}
.modal-fade-enter-from .modal-content {
  transform: translateY(20px) scale(0.97);
  opacity: 0;
}
.modal-fade-leave-to {
  opacity: 0;
}
.modal-fade-leave-to .modal-content {
  transform: translateY(10px) scale(0.98);
  opacity: 0;
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
  .form-grid {
    display: grid;
    grid-template-columns: 1fr;
  }

  .status-actions .btn,
  .status-btn-row .btn {
    width: 100%;
  }

  .product-card {
    grid-template-columns: 48px 1fr;
    gap: 10px;
  }
  .product-card-actions {
    grid-column: 1 / -1;
    justify-content: space-between;
  }
  .timeline-label {
    font-size: 10px;
  }
  .timeline-dot {
    width: 28px;
    height: 28px;
  }
  .modal-content {
    max-height: 90vh;
    border-radius: 16px;
  }
}
</style>
