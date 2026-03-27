<script setup>
import { computed, onMounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import { createHoaDon, addHoaDonItem, updateHoaDon, updateHoaDonBySystemEvent } from "../../../services/hoaDonService"
import { getAllSanPham } from "../../../services/sanPhamService"
import { createKhachHang, getAllKhachHang } from "../../../services/KhachHangService"
import { getAllNhanVien, getNhanVienByTaiKhoanId } from "../../../services/nhanVienService"
import { getDiaChiByKhachHang } from "../../../services/diaChiService"
import { quoteShippingFeeAll } from "../../../services/shippingQuoteService"
import { Plus, Search, Trash2, X, Minus, ShoppingBag, MapPin, Truck } from "lucide-vue-next"
import { appendPaymentFlowTag, PAYMENT_FLOW_TAGS } from "../../../utils/paymentWorkflow"
import { buildOrderLookupTrackingUrl } from "../../../utils/publicTrackingUrl"
import VoucherSelector from "../../../components/voucher/VoucherSelector.vue"
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

const router = useRouter()
const route = useRoute()
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")

const panelBasePath = computed(() => route.path.startsWith('/employee/') ? '/employee' : '/admin')
const isEmployeePanel = computed(() => route.path.startsWith('/employee/'))

const loading = ref(false)
const saving = ref(false)
const currentEmployeeId = ref(null)
const currentSellerName = ref("")

const nhanVienList = ref([])
const khachHangList = ref([])
const variants = ref([])

const cashierId = ref(null)
const customerId = ref(null)
const taiQuay = ref(true)
const paymentMethod = ref("CASH")
const orderNote = ref("")
const discount = ref(0)
const selectedVoucher = ref(null)
const creatingCustomer = ref(false)
const showQuickCustomerForm = ref(false)
const quickCustomer = ref({ tenKhachHang: "", soDienThoai: "", email: "" })

const lines = ref([])
const showProductModal = ref(false)
const productSearchKeyword = ref("")

// Shipping address (for delivery mode)
const customerAddresses = ref([])
const selectedAddressId = ref(null)
const manualAddress = ref({ soDienThoai: "", diaChiCuThe: "", phuongXa: "", quanHuyen: "", tinhThanh: "" })

// Payment modal
const showPaymentModal = ref(false)
const paymentCash = ref(0)
const paymentBank = ref(0)

// Shipping fee (delivery mode)
const shippingFee = ref(0)
const shippingLoading = ref(false)
const shippingProvider = ref("GHN")

const fallbackImages = [img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11]

// ─── Helpers ─────────────────────────────────────────────────────────────────

const toList = (value) => {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.content)) return value.content
  if (Array.isArray(value?.data)) return value.data
  if (Array.isArray(value?.data?.content)) return value.data.content
  return []
}

const formatCurrency = (value) =>
  new Intl.NumberFormat("vi-VN").format(Number(value || 0)) + "₫"

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

function fallbackImageFor(id, code = "") {
  const normalizedId = Number(id)
  if (Number.isFinite(normalizedId) && normalizedId > 0) return fallbackImages[(normalizedId - 1) % fallbackImages.length]
  const digits = String(code || "").replace(/\D+/g, "")
  const codeNum = Number(digits)
  if (Number.isFinite(codeNum) && codeNum > 0) return fallbackImages[(codeNum - 1) % fallbackImages.length]
  return fallbackImages[0] || logoFallback
}

function pickImageValue(entry) {
  if (!entry) return ""
  if (typeof entry === "string") return isImageString(entry) ? toImageUrl(entry) : ""
  if (Array.isArray(entry)) {
    for (const child of entry) {
      const found = pickImageValue(child)
      if (found) return found
    }
    return ""
  }
  if (typeof entry === "object") {
    for (const key of ["anh", "hinhAnh", "image", "imageUrl", "images", "listAnh", "anhChinh", "duongDanAnh", "src", "thumbnail"]) {
      const found = pickImageValue(entry[key])
      if (found) return found
    }
  }
  return ""
}

function buildVariantLabel(product, variant) {
  const parts = [product?.tenSanPham]
  if (variant?.kichThuoc?.tenKichThuoc) parts.push(`Size ${variant.kichThuoc.tenKichThuoc}`)
  if (variant?.mauSac?.tenMauSac || variant?.mauSac?.tenMau) parts.push(variant.mauSac.tenMauSac || variant.mauSac.tenMau)
  return parts.filter(Boolean).join(" • ")
}

const flattenVariants = (products) =>
  products.flatMap((product) =>
    (product?.sanPhamChiTiets ?? []).map((v) => {
      const overrideImage = getProductImageOverride({ id: product?.id, maSanPham: product?.maSanPham })[0] || ""
      return {
        spctId: v.id,
        maSanPham: product.maSanPham || "",
        maSanPhamChiTiet: v.ma || "",
        tenSanPham: product.tenSanPham || "Sản phẩm",
        label: buildVariantLabel(product, v),
        tenMau: v?.mauSac?.tenMau || v?.mauSac?.tenMauSac || "",
        tenSize: v?.kichThuoc?.tenKichThuoc || "",
        giaBan: Number(v?.giaBan || 0),
        soLuongTon: Number(v?.soLuong || 0),
        image: overrideImage || pickImageValue([v, product]) || fallbackImageFor(product?.id, product?.maSanPham)
      }
    })
  )

// ─── Computed ────────────────────────────────────────────────────────────────

const filteredProductVariants = computed(() => {
  const kw = productSearchKeyword.value.trim().toLowerCase()
  if (!kw) return variants.value.slice(0, 60)
  return variants.value.filter((v) =>
    [v.maSanPham, v.maSanPhamChiTiet, v.tenSanPham, v.tenMau, v.tenSize, v.label]
      .join(" ").toLowerCase().includes(kw)
  ).slice(0, 60)
})

const subtotal = computed(() =>
  lines.value.reduce((sum, l) => sum + Number(l.giaBan || 0) * Number(l.soLuong || 0), 0)
)
const grandTotal = computed(() => Math.max(subtotal.value - Number(discount.value || 0) + (taiQuay.value ? 0 : Number(shippingFee.value || 0)), 0))

const paymentEntered = computed(() => Number(paymentCash.value || 0) + Number(paymentBank.value || 0))
const paymentRemaining = computed(() => Math.max(grandTotal.value - paymentEntered.value, 0))
const paymentChange = computed(() => Math.max(paymentEntered.value - grandTotal.value, 0))

const selectedCustomerLabel = computed(() => {
  const kh = khachHangList.value.find((k) => Number(k.id) === Number(customerId.value))
  return kh?.tenKhachHang || "Khách lẻ"
})

const defaultStatusCode = computed(() =>
  paymentMethod.value.toUpperCase() === "VNPAY" ? "CHO_LAY_HANG" : "HOAN_THANH"
)

const selectedAddress = computed(() => {
  if (selectedAddressId.value === "manual") return null
  return customerAddresses.value.find((a) => Number(a.id) === Number(selectedAddressId.value)) || null
})

const shippingAddressText = computed(() => {
  if (taiQuay.value) return "Mua tại quầy"
  if (selectedAddress.value) {
    return [selectedAddress.value.diaChiCuThe, selectedAddress.value.phuongXa, selectedAddress.value.quanHuyen, selectedAddress.value.tinhThanh].filter(Boolean).join(", ")
  }
  if (selectedAddressId.value === "manual") {
    return [manualAddress.value.diaChiCuThe, manualAddress.value.phuongXa, manualAddress.value.quanHuyen, manualAddress.value.tinhThanh].filter(Boolean).join(", ")
  }
  return ""
})

const shippingPhone = computed(() => {
  if (taiQuay.value) return ""
  if (selectedAddress.value) return selectedAddress.value.soDienThoai || ""
  if (selectedAddressId.value === "manual") return manualAddress.value.soDienThoai || ""
  const kh = khachHangList.value.find((k) => Number(k.id) === Number(customerId.value))
  return kh?.soDienThoai || ""
})

// Shipping address parts for GHN quote
const shippingProvince = computed(() => {
  if (taiQuay.value) return ""
  if (selectedAddress.value) return selectedAddress.value.tinhThanh || ""
  if (selectedAddressId.value === "manual") return manualAddress.value.tinhThanh || ""
  return ""
})
const shippingDistrict = computed(() => {
  if (taiQuay.value) return ""
  if (selectedAddress.value) return selectedAddress.value.quanHuyen || ""
  if (selectedAddressId.value === "manual") return manualAddress.value.quanHuyen || ""
  return ""
})
const shippingWard = computed(() => {
  if (taiQuay.value) return ""
  if (selectedAddress.value) return selectedAddress.value.phuongXa || ""
  if (selectedAddressId.value === "manual") return manualAddress.value.phuongXa || ""
  return ""
})

const fetchShippingQuote = async () => {
  if (taiQuay.value) { shippingFee.value = 0; return }
  const toProvince = shippingProvince.value
  const toDistrict = shippingDistrict.value
  if (!toProvince || !toDistrict) { shippingFee.value = 0; return }
  shippingLoading.value = true
  try {
    const totalQty = lines.value.reduce((s, l) => s + Number(l.soLuong || 0), 0)
    const payload = {
      provider: shippingProvider.value,
      toProvince,
      toDistrict,
      toWard: shippingWard.value,
      weightGram: Math.max(500, totalQty * 450),
      lengthCm: 30, widthCm: 20, heightCm: 12
    }
    const response = await quoteShippingFeeAll(payload)
    const quotes = Array.isArray(response?.data?.quotes) ? response.data.quotes : []
    const matched = quotes.find((q) => String(q?.provider || "").toUpperCase() === shippingProvider.value) || quotes[0]
    shippingFee.value = Number(matched?.fee || 0)
  } catch {
    shippingFee.value = 30000
  } finally {
    shippingLoading.value = false
  }
}

// Watch shipping-related changes to re-quote
watch([taiQuay, shippingProvider, shippingProvince, shippingDistrict, shippingWard, () => lines.value.length], () => {
  fetchShippingQuote()
})

// Watch customer ID changes to load addresses
watch(customerId, async (newId) => {
  customerAddresses.value = []
  selectedAddressId.value = null
  if (!newId) return
  try {
    const res = await getDiaChiByKhachHang(newId)
    customerAddresses.value = Array.isArray(res?.data) ? res.data : []
    if (customerAddresses.value.length) selectedAddressId.value = Number(customerAddresses.value[0].id)
  } catch { /* no addresses */ }
})

// ─── Cart actions ────────────────────────────────────────────────────────────

function addProductFromModal(variant) {
  const existed = lines.value.find((l) => Number(l.spctId) === Number(variant.spctId))
  const nextQty = (existed?.soLuong ?? 0) + 1
  if (nextQty > variant.soLuongTon) {
    window.toast?.warning?.("Số lượng vượt tồn kho")
    return
  }
  if (existed) {
    existed.soLuong = nextQty
  } else {
    lines.value.push({ ...variant, soLuong: 1 })
  }
  showProductModal.value = false
  productSearchKeyword.value = ""
}

function incQty(line) {
  if (line.soLuong >= line.soLuongTon) {
    window.toast?.warning?.("Đã đạt tồn kho tối đa")
    return
  }
  line.soLuong++
}

function decQty(line) {
  if (line.soLuong > 1) line.soLuong--
}

function removeLine(index) {
  lines.value.splice(index, 1)
}

const applyDiscount = (amount) => { discount.value = Number(amount || 0) }

const onImgError = (e) => { e.target.src = logoFallback }

function openPaymentModal() {
  if (!lines.value.length) { window.toast?.warning?.("Chưa có sản phẩm trong đơn"); return }
  if (!taiQuay.value && !shippingAddressText.value) { window.toast?.warning?.("Vui lòng chọn địa chỉ giao hàng"); return }
  paymentCash.value = 0
  paymentBank.value = 0
  showPaymentModal.value = true
}

function confirmPayment() {
  if (paymentRemaining.value > 0) { window.toast?.warning?.("Số tiền nhập chưa đủ"); return }
  showPaymentModal.value = false
  submitPosOrder()
}

// ─── Quick customer ──────────────────────────────────────────────────────────

const normalizedPhone = (value) => String(value || "").replace(/\s+/g, "")
const isValidVietnamPhone = (value) => /^(0|\+84)\d{9,10}$/.test(normalizedPhone(value))

const resolveCustomerIdFromResponse = (response) => {
  const candidates = [response?.data?.id, response?.data?.data?.id, response?.data?.khachHang?.id, response?.data?.content?.id]
  return Number(candidates.find((id) => Number(id) > 0)) || null
}

const resetQuickCustomerForm = () => {
  quickCustomer.value = { tenKhachHang: "", soDienThoai: "", email: "" }
}

const quickCreateCustomer = async () => {
  const name = String(quickCustomer.value.tenKhachHang || "").trim()
  const phone = normalizedPhone(quickCustomer.value.soDienThoai)
  const email = String(quickCustomer.value.email || "").trim()
  if (!name) { window.toast?.warning?.("Vui lòng nhập tên khách hàng"); return }
  if (!isValidVietnamPhone(phone)) { window.toast?.warning?.("Số điện thoại không hợp lệ"); return }

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
      } catch (error) { lastError = error }
    }
    const khRes = await getAllKhachHang(0, 200)
    khachHangList.value = toList(khRes?.data)
    if (!createdId) {
      const found = khachHangList.value.find((kh) => normalizedPhone(kh?.soDienThoai) === phone && String(kh?.tenKhachHang || "").trim() === name)
      createdId = found?.id ? Number(found.id) : null
    }
    if (!createdId) throw lastError || new Error("Không lấy được khách hàng vừa tạo")
    customerId.value = createdId
    showQuickCustomerForm.value = false
    resetQuickCustomerForm()
    window.toast?.success?.("Tạo nhanh khách hàng thành công")
  } catch (error) {
    window.toast?.error?.(error?.response?.data?.message || error?.message || "Không thể tạo nhanh khách hàng")
  } finally {
    creatingCustomer.value = false
  }
}

// ─── Employee context ────────────────────────────────────────────────────────

const resolveCurrentSellerContext = async () => {
  // Try to get nhanVien linked to the current taiKhoan
  try {
    const parsed = JSON.parse(localStorage.getItem("user") || sessionStorage.getItem("user") || "null")
    if (parsed?.idNhanVien) {
      const nv = nhanVienList.value.find((n) => Number(n.id) === Number(parsed.idNhanVien))
      if (nv) { currentSellerName.value = nv.tenNhanVien || ""; return Number(nv.id) }
    }
    if (parsed?.id && parsed?.tenNhanVien) {
      currentSellerName.value = parsed.tenNhanVien
      return Number(parsed.id)
    }
  } catch { /* fallback */ }

  const taiKhoanId = Number(localStorage.getItem("userId") || 0)
  if (taiKhoanId > 0) {
    // Try API lookup
    try {
      const res = await getNhanVienByTaiKhoanId(taiKhoanId)
      const first = Array.isArray(res?.data) ? res.data[0] : res?.data
      if (first?.id) {
        currentSellerName.value = first.tenNhanVien || ""
        return Number(first.id)
      }
    } catch { /* fallback */ }
    // Try matching from loaded NV list
    const mapped = nhanVienList.value.find((nv) => Number(nv?.idTaiKhoan || nv?.taiKhoan?.id || 0) === taiKhoanId)
    if (mapped?.id) {
      currentSellerName.value = mapped.tenNhanVien || ""
      return Number(mapped.id)
    }
  }

  // For admin: use stored name from login
  const storedName = localStorage.getItem("userName") || ""
  if (storedName) currentSellerName.value = storedName

  return null
}

// ─── Submit POS order ────────────────────────────────────────────────────────

const submitPosOrder = async () => {
  if (!cashierId.value) { window.toast?.warning?.("Vui lòng chọn nhân viên bán hàng"); return }
  if (!lines.value.length) { window.toast?.warning?.("Chưa có sản phẩm trong đơn"); return }
  if (!taiQuay.value && !shippingAddressText.value) { window.toast?.warning?.("Vui lòng chọn địa chỉ giao hàng"); return }

  saving.value = true
  try {
    const selectedCustomer = khachHangList.value.find((kh) => Number(kh.id) === Number(customerId.value)) ?? null
    const orderType = taiQuay.value ? "POS" : "DELIVERY"
    const addressText = shippingAddressText.value || "Mua tại quầy"
    const phone = shippingPhone.value || selectedCustomer?.soDienThoai || ""

    const shipCost = taiQuay.value ? 0 : Number(shippingFee.value || 0)

    const createRes = await createHoaDon({
      nhanVienId: Number(cashierId.value),
      khachHangId: selectedCustomer?.id ?? null,
      soDienThoaiNhanHang: phone,
      diaChiNhanHang: addressText,
      phiShip: shipCost,
      phuongThucThanhToan: paymentMethod.value,
      orderType,
      orderStatusCode: taiQuay.value ? "CHO_LAY_HANG" : "CHO_XAC_NHAN"
    })
    const orderId = createRes?.data?.hoaDon?.id ?? createRes?.data?.id
    if (!orderId) throw new Error("Không lấy được mã hóa đơn")

    for (const line of lines.value) {
      await addHoaDonItem(orderId, { spctId: line.spctId, soLuong: Number(line.soLuong), giaBan: Number(line.giaBan) })
    }

    const isVnpay = paymentMethod.value.toUpperCase() === "VNPAY"

    await updateHoaDon(orderId, {
      nhanVienId: Number(cashierId.value),
      khachHangId: selectedCustomer?.id ?? null,
      soDienThoaiNhanHang: phone,
      diaChiNhanHang: addressText,
      phiShip: shipCost,
      giaSauGiamGia: Number(discount.value || 0),
      thanhTien: Number(grandTotal.value || 0),
      phuongThucThanhToan: paymentMethod.value,
      orderType,
      statusNote: isVnpay
        ? appendPaymentFlowTag(`[${orderType}] ${orderNote.value || "Đơn bán hàng"}`, PAYMENT_FLOW_TAGS.VN_PAY_EMPLOYEE_CONFIRMED, "NV xác nhận VNPay")
        : `[${orderType}] ${orderNote.value || "Đơn bán hàng"}`
    })

    if (taiQuay.value && !isVnpay) {
      try {
        const trackingUrl = buildOrderLookupTrackingUrl({ maHoaDon: createRes?.data?.hoaDon?.maHoaDon || createRes?.data?.maHoaDon })
        await updateHoaDonBySystemEvent(orderId, "HOAN_TAT_POS", "Hoàn tất bán hàng tại quầy", trackingUrl)
        window.toast?.success?.("Tạo đơn thành công")
      } catch {
        window.toast?.warning?.("Đơn đã tạo nhưng chưa hoàn tất")
      }
    } else if (!taiQuay.value) {
      window.toast?.success?.("Tạo đơn giao hàng thành công")
    } else {
      window.toast?.success?.("Tạo đơn thành công — chờ khách xác nhận VNPay")
    }

    router.push(`${panelBasePath.value}/hoa-don/detail/${orderId}`)
  } catch (error) {
    window.toast?.error?.(error?.response?.data?.message || error.message || "Không thể tạo đơn")
  } finally {
    saving.value = false
  }
}

// ─── Load data ───────────────────────────────────────────────────────────────

const loadData = async () => {
  loading.value = true
  try {
    const [nvRes, khRes, spRes] = await Promise.all([
      getAllNhanVien(),
      getAllKhachHang(0, 200),
      getAllSanPham()
    ])
    nhanVienList.value = toList(nvRes?.data)
    khachHangList.value = toList(khRes?.data)
    variants.value = flattenVariants(toList(spRes?.data))

    // Resolve current seller for both admin and employee
    currentEmployeeId.value = await resolveCurrentSellerContext()
    if (currentEmployeeId.value) {
      cashierId.value = Number(currentEmployeeId.value)
    } else if (!cashierId.value && nhanVienList.value.length) {
      cashierId.value = Number(nhanVienList.value[0].id)
      const nv = nhanVienList.value[0]
      currentSellerName.value = nv?.tenNhanVien || ""
    }
  } catch (error) {
    window.toast?.error?.(error?.message || "Không thể tải dữ liệu bán hàng")
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <main class="bh-page">
    <!-- Header -->
    <header class="bh-header">
      <div>
        <h1 class="bh-title">Bán hàng</h1>
        <p class="bh-subtitle">Người bán: <strong>{{ currentSellerName || nhanVienList.find(nv => Number(nv.id) === Number(cashierId))?.tenNhanVien || '—' }}</strong></p>
      </div>
      <button class="bh-btn bh-btn-primary" type="button" @click="router.push(`${panelBasePath}/hoa-don/list`)">
        <span>+ Tạo đơn hàng</span>
      </button>
    </header>

    <div v-if="loading" class="bh-loading">Đang tải dữ liệu bán hàng...</div>

    <template v-else>
      <div class="bh-layout">
        <!-- ─── Left: Sản phẩm ─── -->
        <div class="bh-main">
          <div class="bh-card">
            <!-- Products header -->
            <div class="bh-card-head">
              <h2>Sản phẩm</h2>
              <div class="bh-card-actions">
                <button class="bh-btn bh-btn-primary" type="button" @click="showProductModal = true">
                  <Plus :size="15" /><span>THÊM SẢN PHẨM</span>
                </button>
              </div>
            </div>

            <!-- Cart items -->
            <div class="bh-cart">
              <div v-if="!lines.length" class="bh-cart-empty">
                <ShoppingBag :size="36" />
                <p>Chưa có sản phẩm — bấm "Thêm sản phẩm" để bắt đầu</p>
              </div>

              <div v-for="(line, idx) in lines" :key="line.spctId" class="bh-cart-item">
                <!-- Thumbnail -->
                <div class="bh-cart-thumb">
                  <img :src="line.image || logoFallback" :alt="line.tenSanPham" @error="onImgError" />
                </div>

                <!-- Info -->
                <div class="bh-cart-info">
                  <div class="bh-cart-name">{{ line.tenSanPham }}</div>
                  <div class="bh-cart-meta">
                    <span class="bh-cart-code">{{ line.maSanPhamChiTiet }}</span>
                    <span v-if="line.tenMau" class="bh-dot">·</span>
                    <span v-if="line.tenMau">{{ line.tenMau }}</span>
                    <span v-if="line.tenSize" class="bh-dot">·</span>
                    <span v-if="line.tenSize">{{ line.tenSize }}</span>
                  </div>
                </div>

                <!-- Price -->
                <div class="bh-cart-price">{{ formatCurrency(line.giaBan) }}</div>

                <!-- Qty controls -->
                <div class="bh-cart-qty">
                  <button class="bh-qty-btn" type="button" @click="decQty(line)" :disabled="line.soLuong <= 1">
                    <Minus :size="14" />
                  </button>
                  <span class="bh-qty-value">{{ line.soLuong }}</span>
                  <button class="bh-qty-btn" type="button" @click="incQty(line)" :disabled="line.soLuong >= line.soLuongTon">
                    <Plus :size="14" />
                  </button>
                </div>

                <!-- Delete -->
                <button class="bh-cart-delete" type="button" @click="removeLine(idx)">
                  <Trash2 :size="16" />
                </button>
              </div>

              <!-- Cart total -->
              <div v-if="lines.length" class="bh-cart-total">
                <span>Tổng tiền</span>
                <strong>{{ formatCurrency(subtotal) }}</strong>
              </div>
            </div>
          </div>
        </div>

        <!-- ─── Right: Customer + Payment ─── -->
        <div class="bh-sidebar">
          <!-- Customer panel -->
          <div class="bh-card">
            <div class="bh-card-head">
              <h2>Thông tin khách hàng</h2>
              <div class="bh-card-actions">
                <button class="bh-btn bh-btn-outline" type="button" @click="showQuickCustomerForm = !showQuickCustomerForm">
                  {{ showQuickCustomerForm ? 'Ẩn' : 'Chọn khách hàng' }}
                </button>
              </div>
            </div>
            <div class="bh-card-body">
              <div class="bh-kv">
                <span class="bh-k">Tên khách hàng</span>
                <span class="bh-v">{{ selectedCustomerLabel }}</span>
              </div>

              <select class="bh-select" v-model.number="customerId">
                <option :value="null">Khách lẻ</option>
                <option v-for="kh in khachHangList" :key="kh.id" :value="Number(kh.id)">
                  {{ kh.tenKhachHang || `KH #${kh.id}` }}
                </option>
              </select>

              <!-- Address section (for delivery mode) -->
              <template v-if="!taiQuay && customerId">
                <div class="bh-address-section">
                  <div class="bh-address-label"><MapPin :size="14" /> Địa chỉ giao hàng</div>
                  <select v-if="customerAddresses.length" class="bh-select" v-model="selectedAddressId">
                    <option v-for="addr in customerAddresses" :key="addr.id" :value="Number(addr.id)">
                      {{ [addr.diaChiCuThe, addr.phuongXa, addr.quanHuyen, addr.tinhThanh].filter(Boolean).join(', ') }}
                    </option>
                    <option value="manual">Nhập địa chỉ mới</option>
                  </select>
                  <div v-if="!customerAddresses.length || selectedAddressId === 'manual'" class="bh-address-form">
                    <input v-model="manualAddress.soDienThoai" type="text" placeholder="Số điện thoại nhận hàng" class="bh-input" />
                    <input v-model="manualAddress.diaChiCuThe" type="text" placeholder="Số nhà, tên đường..." class="bh-input" />
                    <input v-model="manualAddress.phuongXa" type="text" placeholder="Phường/Xã" class="bh-input" />
                    <input v-model="manualAddress.quanHuyen" type="text" placeholder="Quận/Huyện" class="bh-input" />
                    <input v-model="manualAddress.tinhThanh" type="text" placeholder="Tỉnh/Thành phố" class="bh-input" />
                  </div>
                </div>
              </template>
              <template v-if="!taiQuay && !customerId">
                <p class="bh-hint" style="color: #dc2626;">Giao hàng cần chọn khách hàng.</p>
              </template>

              <!-- Shipping fee (delivery mode) -->
              <div v-if="!taiQuay" class="bh-shipping-fee">
                <div class="bh-shipping-fee-row">
                  <span><Truck :size="14" /> Đơn vị vận chuyển</span>
                  <select class="bh-select bh-select-sm" v-model="shippingProvider">
                    <option value="GHN">GHN (Giao Hàng Nhanh)</option>
                    <option value="GHTK">GHTK (Giao Hàng Tiết Kiệm)</option>
                  </select>
                </div>
                <div v-if="shippingProvince" class="bh-shipping-fee-row" style="margin-top: 8px;">
                  <span>Phí vận chuyển</span>
                  <strong v-if="shippingLoading">Đang tính...</strong>
                  <strong v-else>{{ formatCurrency(shippingFee) }}</strong>
                </div>
              </div>

              <p v-if="taiQuay" class="bh-hint">Tại quầy: chỉ cần chọn sản phẩm và thanh toán.</p>

              <!-- Quick customer form -->
              <div v-if="showQuickCustomerForm" class="bh-quick-form">
                <input v-model="quickCustomer.tenKhachHang" type="text" placeholder="Tên khách hàng" :disabled="creatingCustomer" />
                <input v-model="quickCustomer.soDienThoai" type="text" placeholder="Số điện thoại" :disabled="creatingCustomer" />
                <input v-model="quickCustomer.email" type="email" placeholder="Email (tùy chọn)" :disabled="creatingCustomer" />
                <div class="bh-quick-actions">
                  <button class="bh-btn bh-btn-outline bh-btn-sm" type="button" @click="resetQuickCustomerForm" :disabled="creatingCustomer">Làm mới</button>
                  <button class="bh-btn bh-btn-primary bh-btn-sm" type="button" @click="quickCreateCustomer" :disabled="creatingCustomer">
                    {{ creatingCustomer ? 'Đang tạo...' : 'Tạo và chọn' }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Payment panel -->
          <div class="bh-card">
            <div class="bh-card-head bh-payment-head">
              <h2>Thông tin thanh toán</h2>
              <label class="bh-toggle">
                <span class="bh-toggle-label">{{ taiQuay ? 'Tại quầy' : 'Giao hàng' }}</span>
                <input type="checkbox" v-model="taiQuay" />
                <span class="bh-toggle-track"></span>
              </label>
            </div>
            <div class="bh-card-body">
              <div class="bh-field">
                <label>Nhân viên</label>
                <select class="bh-select" v-model.number="cashierId" :disabled="isEmployeePanel">
                  <option :value="null">Chọn nhân viên</option>
                  <option v-for="nv in nhanVienList" :key="nv.id" :value="Number(nv.id)">
                    {{ nv.tenNhanVien || `NV #${nv.id}` }}
                  </option>
                </select>
              </div>

              <div class="bh-field">
                <label>Thanh toán</label>
                <select class="bh-select" v-model="paymentMethod">
                  <option value="CASH">Tiền mặt</option>
                  <option value="BANK">Chuyển khoản</option>
                  <option value="VNPAY">VNPay</option>
                </select>
              </div>

              <div class="bh-field">
                <label>Giảm giá (voucher)</label>
                <VoucherSelector
                  :subtotal="subtotal"
                  :customer-id="customerId"
                  :auto-select="true"
                  @update:voucher="selectedVoucher = $event"
                  @discount-changed="applyDiscount"
                />
              </div>

              <div class="bh-field">
                <label>Ghi chú</label>
                <input class="bh-input" v-model="orderNote" type="text" placeholder="Ghi chú đơn hàng..." />
              </div>

              <!-- Summary -->
              <div class="bh-summary">
                <div class="bh-pay-row"><span>Tiền hàng</span><strong>{{ formatCurrency(subtotal) }}</strong></div>
                <div class="bh-pay-row bh-pay-discount"><span>Giảm giá</span><strong>- {{ formatCurrency(discount) }}</strong></div>
                <div v-if="!taiQuay" class="bh-pay-row"><span><Truck :size="14" /> Phí ship ({{ shippingProvider }})</span><strong>{{ shippingLoading ? '...' : formatCurrency(shippingFee) }}</strong></div>
                <div class="bh-pay-row bh-pay-total"><span>Tổng số tiền</span><strong>{{ formatCurrency(grandTotal) }}</strong></div>
              </div>

              <button
                class="bh-btn bh-btn-primary bh-btn-submit"
                type="button"
                :disabled="saving || !lines.length"
                @click="openPaymentModal"
              >
                {{ saving ? 'ĐANG TẠO ĐƠN...' : 'XÁC NHẬN ĐẶT HÀNG' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ─── Product selection modal ─── -->
    <Teleport to="body">
      <Transition name="bh-modal">
        <div v-if="showProductModal" class="bh-modal-overlay" @click.self="showProductModal = false">
          <div class="bh-modal-box">
            <div class="bh-modal-head">
              <h2>Thêm sản phẩm</h2>
              <button class="bh-modal-close" @click="showProductModal = false"><X :size="20" /></button>
            </div>
            <div class="bh-modal-search">
              <Search :size="18" class="bh-modal-search-icon" />
              <input v-model="productSearchKeyword" type="text" placeholder="Tìm tên, mã sản phẩm, màu, size..." />
            </div>
            <div class="bh-modal-list">
              <div v-if="!filteredProductVariants.length" class="bh-modal-empty">Không tìm thấy sản phẩm phù hợp</div>
              <div
                v-for="variant in filteredProductVariants"
                :key="variant.spctId"
                class="bh-modal-item"
                @click="addProductFromModal(variant)"
              >
                <div class="bh-modal-item-img">
                  <img :src="variant.image || logoFallback" :alt="variant.tenSanPham" @error="onImgError" />
                </div>
                <div class="bh-modal-item-info">
                  <div class="bh-modal-item-name">{{ variant.tenSanPham }}</div>
                  <div class="bh-modal-item-meta">
                    <span>{{ variant.maSanPhamChiTiet }}</span>
                    <span v-if="variant.tenMau">· {{ variant.tenMau }}</span>
                    <span v-if="variant.tenSize">· {{ variant.tenSize }}</span>
                    <span>· Tồn: {{ variant.soLuongTon }}</span>
                  </div>
                </div>
                <div class="bh-modal-item-price">{{ formatCurrency(variant.giaBan) }}</div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- ─── Payment modal ─── -->
    <Teleport to="body">
      <Transition name="bh-modal">
        <div v-if="showPaymentModal" class="bh-modal-overlay" @click.self="showPaymentModal = false">
          <div class="bh-modal-box bh-pay-modal">
            <div class="bh-modal-head">
              <h2>Thanh toán</h2>
              <button class="bh-modal-close" @click="showPaymentModal = false"><X :size="20" /></button>
            </div>
            <div class="bh-pay-modal-body">
              <div class="bh-pay-modal-total">
                <span>Tổng số tiền</span>
                <strong>{{ formatCurrency(grandTotal) }}</strong>
              </div>

              <div class="bh-pay-modal-inputs">
                <div class="bh-pay-modal-col">
                  <label>Tiền mặt</label>
                  <span class="bh-pay-modal-remaining">Còn lại</span>
                  <input type="number" class="bh-input" v-model.number="paymentCash" min="0" placeholder="Nhập tiền mặt..." />
                </div>
                <div class="bh-pay-modal-col">
                  <label>Chuyển khoản</label>
                  <span class="bh-pay-modal-remaining">Còn lại</span>
                  <input type="number" class="bh-input" v-model.number="paymentBank" min="0" placeholder="Nhập chuyển khoản..." />
                </div>
              </div>

              <div class="bh-pay-modal-summary">
                <div class="bh-pay-modal-row"><span>Đã nhập</span><strong>{{ formatCurrency(paymentEntered) }}</strong></div>
                <div class="bh-pay-modal-row"><span>Tiền thiếu</span><strong class="bh-text-danger">{{ formatCurrency(paymentRemaining) }}</strong></div>
                <div class="bh-pay-modal-row"><span>Tiền thừa</span><strong class="bh-text-success">{{ formatCurrency(paymentChange) }}</strong></div>
              </div>

              <div class="bh-pay-modal-actions">
                <button class="bh-btn bh-btn-outline" type="button" @click="showPaymentModal = false">Đóng</button>
                <button
                  class="bh-btn bh-btn-primary"
                  type="button"
                  :disabled="saving || paymentRemaining > 0"
                  @click="confirmPayment"
                >
                  {{ saving ? 'Đang xử lý...' : 'Xong' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </main>
</template>

<style scoped>
/* ── Page ── */
.bh-page {
  padding: 24px;
  min-height: 100vh;
  background: #f8f9fb;
}
.bh-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.bh-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: #111827;
}
.bh-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
}
.bh-loading {
  text-align: center;
  padding: 40px;
  color: #64748b;
  font-weight: 600;
}

/* ── Layout ── */
.bh-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(360px, 1fr);
  gap: 20px;
  align-items: start;
}
.bh-main {
  display: grid;
  gap: 20px;
}
.bh-sidebar {
  display: grid;
  gap: 16px;
  position: sticky;
  top: 80px;
}

/* ── Card ── */
.bh-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  overflow: hidden;
}
.bh-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eef2f7;
}
.bh-card-head h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: #111827;
}
.bh-card-actions {
  display: flex;
  gap: 8px;
}
.bh-card-body {
  padding: 18px 20px;
  display: grid;
  gap: 14px;
}

/* ── Buttons ── */
.bh-btn {
  border: none;
  border-radius: 10px;
  padding: 10px 16px;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.15s ease;
}
.bh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.bh-btn-primary {
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: #fff;
  box-shadow: 0 4px 12px rgba(185,28,28,0.25);
}
.bh-btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(185,28,28,0.3);
}
.bh-btn-outline {
  background: #fff;
  color: #374151;
  border: 1px solid #d1d5db;
}
.bh-btn-outline:hover:not(:disabled) {
  border-color: #9ca3af;
  background: #f9fafb;
}
.bh-btn-sm {
  padding: 7px 12px;
  font-size: 12px;
}
.bh-btn-submit {
  width: 100%;
  justify-content: center;
  padding: 14px;
  font-size: 15px;
  letter-spacing: 0.04em;
  border-radius: 12px;
  margin-top: 4px;
}

/* ── Cart ── */
.bh-cart {
  padding: 6px 0;
}
.bh-cart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 40px 20px;
  color: #94a3b8;
}
.bh-cart-empty p {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
}
.bh-cart-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 20px;
  border-bottom: 1px solid #f3f4f6;
  transition: background 0.15s;
}
.bh-cart-item:last-of-type {
  border-bottom: none;
}
.bh-cart-item:hover {
  background: #fafbfc;
}

/* Thumbnail */
.bh-cart-thumb {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  overflow: hidden;
  background: #f1f5f9;
  flex-shrink: 0;
  border: 1px solid #e5e7eb;
}
.bh-cart-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.bh-cart-thumb-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: #94a3b8;
}

/* Info */
.bh-cart-info {
  flex: 1;
  min-width: 0;
}
.bh-cart-name {
  font-weight: 700;
  font-size: 14px;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bh-cart-meta {
  display: flex;
  gap: 4px;
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}
.bh-cart-code {
  color: #9ca3af;
}
.bh-dot {
  color: #d1d5db;
}

/* Price */
.bh-cart-price {
  font-weight: 700;
  font-size: 14px;
  color: #111827;
  min-width: 100px;
  text-align: right;
  flex-shrink: 0;
}

/* Qty controls */
.bh-cart-qty {
  display: flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  overflow: hidden;
}
.bh-qty-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #f9fafb;
  color: #374151;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.15s;
}
.bh-qty-btn:hover:not(:disabled) {
  background: #f1f5f9;
}
.bh-qty-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.bh-qty-value {
  width: 36px;
  text-align: center;
  font-weight: 700;
  font-size: 14px;
  color: #111827;
  border-left: 1px solid #d1d5db;
  border-right: 1px solid #d1d5db;
  line-height: 32px;
}

/* Delete */
.bh-cart-delete {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: none;
  background: #fef2f2;
  color: #dc2626;
  display: grid;
  place-items: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s;
}
.bh-cart-delete:hover {
  background: #fee2e2;
}

/* Cart total */
.bh-cart-total {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  border-top: 1px solid #e5e7eb;
  font-size: 15px;
  color: #374151;
}
.bh-cart-total strong {
  font-size: 18px;
  color: #dc2626;
  font-weight: 800;
}

/* ── Customer panel ── */
.bh-kv {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}
.bh-k {
  color: #64748b;
  font-weight: 600;
}
.bh-v {
  color: #111827;
  font-weight: 700;
}
.bh-hint {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;
}
.bh-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  background: #fff;
}
.bh-select:focus {
  outline: none;
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220,38,38,0.1);
}
.bh-select:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.bh-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  background: #fff;
}
.bh-input:focus {
  outline: none;
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220,38,38,0.1);
}

.bh-quick-form {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px dashed #d1d5db;
  border-radius: 10px;
  background: #f9fafb;
}
.bh-quick-form input {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font: inherit;
  font-size: 13px;
  color: #111827;
}
.bh-quick-form input:focus {
  outline: none;
  border-color: #dc2626;
}
.bh-quick-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

/* ── Payment panel ── */
.bh-payment-head {
  gap: 12px;
}
.bh-field {
  display: grid;
  gap: 6px;
}
.bh-field label {
  font-size: 13px;
  font-weight: 700;
  color: #374151;
}
.bh-summary {
  display: grid;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}
.bh-pay-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #374151;
}
.bh-pay-row strong {
  font-weight: 700;
}
.bh-pay-discount strong {
  color: #dc2626;
}
.bh-pay-total {
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px dashed #d1d5db;
  font-size: 16px;
  font-weight: 800;
}
.bh-pay-total strong {
  font-size: 18px;
  color: #dc2626;
  font-weight: 800;
}

/* ── Modal ── */
.bh-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15,23,42,0.5);
  backdrop-filter: blur(4px);
  display: grid;
  place-items: center;
  z-index: 9999;
  padding: 20px;
}
.bh-modal-box {
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
.bh-modal-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px 14px;
  border-bottom: 1px solid #eef2f7;
}
.bh-modal-head h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #111827;
}
.bh-modal-close {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: #f1f5f9;
  color: #475569;
  display: grid;
  place-items: center;
  cursor: pointer;
}
.bh-modal-close:hover {
  background: #fee2e2;
  color: #dc2626;
}
.bh-modal-search {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 24px;
  border-bottom: 1px solid #eef2f7;
}
.bh-modal-search-icon {
  color: #94a3b8;
  flex-shrink: 0;
}
.bh-modal-search input {
  width: 100%;
  border: none;
  outline: none;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  background: transparent;
}
.bh-modal-search input::placeholder {
  color: #94a3b8;
}
.bh-modal-list {
  overflow-y: auto;
  padding: 8px;
}
.bh-modal-empty {
  text-align: center;
  padding: 32px;
  color: #94a3b8;
  font-weight: 600;
}
.bh-modal-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s;
}
.bh-modal-item:hover {
  background: #fef2f2;
}
.bh-modal-item-img {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  overflow: hidden;
  background: #f1f5f9;
  flex-shrink: 0;
  border: 1px solid #e5e7eb;
}
.bh-modal-item-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.bh-modal-item-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: #94a3b8;
}
.bh-modal-item-info {
  flex: 1;
  min-width: 0;
}
.bh-modal-item-name {
  font-weight: 700;
  font-size: 13px;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bh-modal-item-meta {
  display: flex;
  gap: 4px;
  font-size: 11px;
  color: #6b7280;
  margin-top: 2px;
}
.bh-modal-item-meta span {
  font-weight: 600;
}
.bh-modal-item-price {
  font-weight: 800;
  color: #b91c1c;
  font-size: 14px;
  flex-shrink: 0;
}

/* ── Modal transition ── */
.bh-modal-enter-active { transition: opacity 0.2s ease; }
.bh-modal-enter-active .bh-modal-box { transition: transform 0.25s cubic-bezier(.25,.8,.25,1), opacity 0.2s ease; }
.bh-modal-leave-active { transition: opacity 0.15s ease; }
.bh-modal-leave-active .bh-modal-box { transition: transform 0.15s ease, opacity 0.15s ease; }
.bh-modal-enter-from { opacity: 0; }
.bh-modal-enter-from .bh-modal-box { transform: translateY(20px) scale(0.97); opacity: 0; }
.bh-modal-leave-to { opacity: 0; }
.bh-modal-leave-to .bh-modal-box { transform: translateY(10px) scale(0.98); opacity: 0; }

/* ── Responsive ── */
@media (max-width: 900px) {
  .bh-layout {
    grid-template-columns: 1fr;
  }
  .bh-sidebar {
    position: static;
  }
  .bh-cart-item {
    flex-wrap: wrap;
    gap: 10px;
  }
  .bh-cart-price {
    min-width: auto;
  }
}

/* ── Toggle switch ── */
.bh-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}
.bh-toggle-label {
  font-size: 13px;
  font-weight: 700;
  color: #374151;
}
.bh-toggle input {
  display: none;
}
.bh-toggle-track {
  position: relative;
  width: 42px;
  height: 24px;
  background: #d1d5db;
  border-radius: 999px;
  transition: background 0.2s;
}
.bh-toggle-track::after {
  content: "";
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  background: #fff;
  border-radius: 50%;
  transition: transform 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
.bh-toggle input:checked + .bh-toggle-track {
  background: #dc2626;
}
.bh-toggle input:checked + .bh-toggle-track::after {
  transform: translateX(18px);
}

/* ── Address section ── */
.bh-address-section {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f9fafb;
}
.bh-address-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: #374151;
}
.bh-address-form {
  display: grid;
  gap: 6px;
}

/* ── Shipping fee ── */
.bh-shipping-fee {
  padding: 10px 12px;
  border: 1px solid #e0e7ff;
  border-radius: 10px;
  background: #eef2ff;
}
.bh-shipping-fee-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #374151;
}
.bh-shipping-fee-row span {
  display: flex;
  align-items: center;
  gap: 6px;
}
.bh-shipping-fee-row strong {
  color: #4338ca;
}
.bh-select-sm {
  padding: 4px 8px;
  font-size: 12px;
  border-radius: 6px;
  border: 1px solid #c7d2fe;
  background: #fff;
  color: #374151;
}

/* ── Payment modal ── */
.bh-pay-modal {
  max-width: 640px;
  width: 95vw;
}
.bh-pay-modal-body {
  padding: 28px 32px 32px;
  display: grid;
  gap: 22px;
}
.bh-pay-modal-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  padding-bottom: 18px;
  border-bottom: 1px solid #e5e7eb;
}
.bh-pay-modal-total strong {
  font-size: 24px;
  color: #dc2626;
  font-weight: 800;
}
.bh-pay-modal-inputs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.bh-pay-modal-col {
  display: grid;
  gap: 4px;
}
.bh-pay-modal-col label {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}
.bh-pay-modal-col .bh-input {
  font-size: 16px;
  padding: 10px 14px;
}
.bh-pay-modal-remaining {
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  text-align: right;
}
.bh-pay-modal-summary {
  display: grid;
  gap: 8px;
  padding-top: 14px;
  border-top: 1px solid #e5e7eb;
}
.bh-pay-modal-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
  color: #374151;
}
.bh-pay-modal-row strong {
  font-weight: 700;
}
.bh-text-danger {
  color: #dc2626;
}
.bh-text-success {
  color: #16a34a;
}
.bh-pay-modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 8px;
}
.bh-pay-modal-actions .bh-btn {
  min-width: 80px;
  justify-content: center;
}
</style>
