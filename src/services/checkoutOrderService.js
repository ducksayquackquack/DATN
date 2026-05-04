import taiKhoanService from "./taiKhoanService"
import { createKhachHang, getAllKhachHang, getKhachHangByTaiKhoanId } from "./KhachHangService"
import { getDiaChiByKhachHang } from "./diaChiService"
import { addHoaDonItem, createHoaDon } from "./hoaDonService"
import { getAllSanPham } from "./sanPhamService"
import { getAllNhanVien } from "./nhanVienService"
import { applyCampaignPriceToVariants } from "./campaignPricingService"

const COMMON_WORDS = new Set(["ao", "quan", "dirtywave", "nam"])

const normalizeText = (value = "") => {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .replace(/�/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim()
}

const tokenizeName = (value = "") => {
  return normalizeText(value)
    .split(" ")
    .filter((token) => token && !COMMON_WORDS.has(token))
}

const extractAccounts = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const extractAddresses = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const toDisplayNameFromEmail = (email = "") => {
  const localPart = String(email || "").split("@")[0].replace(/[._-]+/g, " ").trim()
  if (!localPart) return ""
  return localPart
    .split(/\s+/)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ")
}

const formatAddress = (address) => {
  if (!address) return ""
  const parts = [address.diaChiCuThe, address.phuongXa, address.quanHuyen, address.tinhThanh]
    .map((part) => String(part || "").trim())
    .filter(Boolean)
  return parts.join(", ")
}

const getAddressCompletenessScore = (address = {}) => {
  const detail = String(address?.diaChiCuThe || "").trim()
  const ward = String(address?.phuongXa || "").trim()
  const district = String(address?.quanHuyen || "").trim()
  const province = String(address?.tinhThanh || "").trim()

  let score = 0
  if (detail) score += 10
  if (ward) score += 20
  if (district) score += 25
  if (province) score += 30

  const defaultFlag = Boolean(
    address?.macDinh
    || address?.laMacDinh
    || address?.isDefault
    || address?.defaultAddress
    || address?.diaChiMacDinh
  )
  if (defaultFlag) score += 100

  const statusText = String(address?.trangThai || "").toLowerCase()
  if (statusText.includes("mặc định") || statusText.includes("mac dinh")) {
    score += 100
  }

  return score
}

const pickPreferredAddress = (addresses = []) => {
  if (!Array.isArray(addresses) || !addresses.length) return null
  return addresses
    .slice()
    .sort((a, b) => getAddressCompletenessScore(b) - getAddressCompletenessScore(a))[0] || null
}

const scoreVariantMatch = (item, variant) => {
  const itemTokens = tokenizeName(item.name)
  const productTokens = tokenizeName(variant.productName)
  let score = 0

  for (const token of itemTokens) {
    if (productTokens.includes(token)) score += 3
  }

  const itemName = normalizeText(item.name)
  const productName = normalizeText(variant.productName)
  if (itemName && productName) {
    if (itemName.includes(productName) || productName.includes(itemName)) score += 4
  }

  const itemColor = normalizeText(item.color)
  const itemSize = normalizeText(item.size)
  const variantColor = normalizeText(variant.colorName)
  const variantSize = normalizeText(variant.sizeName)

  if (itemColor && itemColor === variantColor) score += 5
  if (itemSize && itemSize === variantSize) score += 5

  const itemPrice = Number(item.price || 0)
  const variantPrice = Number(variant.price || 0)
  if (itemPrice > 0 && variantPrice > 0) {
    const diff = Math.abs(itemPrice - variantPrice)
    if (diff === 0) score += 3
    else if (diff <= 30000) score += 1
  }

  return score
}

const isActiveVariantStatus = (value = "") => {
  const normalized = String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
  return !normalized.includes("ngung") && !normalized.includes("inactive")
}

const flattenVariants = (products = []) => {
  return products.flatMap((product) => {
    const variants = Array.isArray(product?.sanPhamChiTiets)
      ? product.sanPhamChiTiets.filter((variant) => isActiveVariantStatus(variant?.trangThai || variant?.status))
      : []
    return variants.map((variant) => ({
      spctId: variant.id,
      productId: product.id,
      productName: product.tenSanPham,
      colorName: variant?.mauSac?.tenMau || "",
      sizeName: variant?.kichThuoc?.tenKichThuoc || "",
      price: Number(variant?.giaBan || 0),
      raw: variant
    }))
  })
}

const resolveVariantForItem = (item, variants) => {
  if (item?.spctId) {
    const exact = variants.find((variant) => Number(variant.spctId) === Number(item.spctId))
    if (exact) return exact
  }

  const ranked = variants
    .map((variant) => ({ variant, score: scoreVariantMatch(item, variant) }))
    .filter((entry) => entry.score > 0)
    .sort((left, right) => right.score - left.score)

  return ranked[0]?.variant || null
}

const normalizeRole = (role) => {
  return String(role || "")
    .trim()
    .toUpperCase()
    .replace(/^ROLE_/, "")
}

const isCustomerRole = (role) => {
  const normalized = normalizeRole(role)
  return normalized === "CUSTOMER" || normalized === "KHACH_HANG" || normalized === "KHACHHANG"
}

const isEmployeeRole = (role) => {
  const normalized = normalizeRole(role)
  return normalized === "EMPLOYEE" || normalized === "NHAN_VIEN" || normalized === "NHANVIEN"
}

const normalizeStatusText = (value = "") => {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
    .trim()
}

export const isAccountActiveForCheckout = (account = {}) => {
  const normalized = normalizeStatusText(
    account?.trangThaiTaiKhoan
    || account?.trangThai
    || account?.status
    || ""
  )

  if (!normalized) return true
  if (normalized.includes("ngung") || normalized.includes("inactive") || normalized.includes("disabled") || normalized.includes("block") || normalized.includes("khoa")) {
    return false
  }

  return true
}

async function resolveDefaultEmployeeId() {
  try {
    const response = await getAllNhanVien()
    const employees = extractAccounts(response?.data)
    const firstEmployee = employees.find((item) => {
      const role = item?.taiKhoan?.vaiTro || item?.vaiTro
      return isEmployeeRole(role)
    }) || employees[0]
    return Number(firstEmployee?.id || 0) || null
  } catch {
    return 1
  }
}

async function getCurrentAccount() {
  const userId = localStorage.getItem("userId")
  const userEmail = String(localStorage.getItem("userEmail") || "").trim().toLowerCase()

  if (userId) {
    try {
      const byId = await taiKhoanService.getById(userId)
      const accountById = byId?.data || null
      if (accountById?.id && isCustomerRole(accountById?.vaiTro)) {
        return accountById
      }
    } catch {
      // Fallback below.
    }
  }

  if (!userEmail) return null

  try {
    const allRes = await taiKhoanService.getAll()
    const accounts = extractAccounts(allRes?.data)
    return accounts.find((item) => {
      const sameEmail = String(item?.email || "").trim().toLowerCase() === userEmail
      return sameEmail && isCustomerRole(item?.vaiTro)
    }) || null
  } catch {
    return null
  }
}

async function getCurrentCustomer(account) {
  if (!account?.id) return null

  try {
    const byTaiKhoan = await getKhachHangByTaiKhoanId(account.id)
    if (byTaiKhoan?.data?.id) return byTaiKhoan.data
  } catch {
    // Fallback below.
  }

  try {
    const listRes = await getAllKhachHang(0, 500)
    const customers = extractAccounts(listRes?.data)
    return customers.find((item) => Number(item?.idTaiKhoan || item?.taiKhoan?.id) === Number(account.id)) || null
  } catch {
    return null
  }
}

async function ensureCustomerProfile(account, existingCustomer, delivery = {}) {
  if (existingCustomer?.id) return existingCustomer
  if (!account?.id) return null

  const normalizedName = String(delivery?.name || "").trim()
  const normalizedPhone = String(delivery?.phone || "").trim()

  const payloadCandidates = [
    {
      tenKhachHang: normalizedName || toDisplayNameFromEmail(account?.email),
      soDienThoai: normalizedPhone,
      idTaiKhoan: Number(account.id),
      trangThai: "Hoạt động"
    },
    {
      tenKhachHang: normalizedName || toDisplayNameFromEmail(account?.email),
      soDienThoai: normalizedPhone,
      taiKhoan: { id: Number(account.id) },
      trangThai: "Hoạt động"
    },
    {
      tenKhachHang: normalizedName || toDisplayNameFromEmail(account?.email),
      soDienThoai: normalizedPhone,
      taiKhoanId: Number(account.id),
      trangThai: "Hoạt động"
    }
  ]

  for (const payload of payloadCandidates) {
    try {
      await createKhachHang(payload)
      const byTaiKhoan = await getKhachHangByTaiKhoanId(account.id)
      if (byTaiKhoan?.data?.id) return byTaiKhoan.data
    } catch {
      // Try next payload shape.
    }
  }

  return null
}

export async function loadCheckoutContext() {
  const account = await getCurrentAccount()
  const customer = await getCurrentCustomer(account)
  const accountActive = isAccountActiveForCheckout(account)
  let addresses = []

  if (customer?.id) {
    try {
      const addressRes = await getDiaChiByKhachHang(customer.id)
      addresses = extractAddresses(addressRes?.data)
    } catch {
      addresses = []
    }
  }

  const primaryAddress = pickPreferredAddress(addresses)
  const localDetail = String(localStorage.getItem("userDiaChiCuThe") || "").trim()
  const localWard = String(localStorage.getItem("userPhuongXa") || "").trim()
  const localDistrict = String(localStorage.getItem("userQuanHuyen") || "").trim()
  const localProvince = String(localStorage.getItem("userTinhThanh") || "").trim()
  const localAddress = String(localStorage.getItem("userAddress") || "").trim()
  const mergedLocalAddress = [localDetail, localWard, localDistrict, localProvince]
    .filter(Boolean)
    .join(", ")

  return {
    account,
    accountActive,
    customer,
    addresses,
    delivery: {
      name: customer?.tenKhachHang || localStorage.getItem("userName") || account?.tenTaiKhoan || toDisplayNameFromEmail(account?.email) || "",
      phone: customer?.soDienThoai || localStorage.getItem("userPhone") || "",
      address: formatAddress(primaryAddress) || localAddress || mergedLocalAddress || "",
      diaChiCuThe: String(primaryAddress?.diaChiCuThe || localDetail || "").trim(),
      phuongXa: String(primaryAddress?.phuongXa || localWard || "").trim(),
      quanHuyen: String(primaryAddress?.quanHuyen || localDistrict || "").trim(),
      tinhThanh: String(primaryAddress?.tinhThanh || localProvince || "").trim()
    }
  }
}

const isValidPhone = (value) => {
  const phone = String(value || "").replace(/\s+/g, "")
  return /^(0|\+84)\d{9,10}$/.test(phone)
}

const normalizeDelivery = (delivery = {}, contextDelivery = {}) => {
  const province = String(delivery?.province || "").trim()
  const district = String(delivery?.district || "").trim()
  const ward = String(delivery?.ward || "").trim()
  const addressDetail = String(delivery?.addressDetail || "").trim()

  const mergedAddress = [addressDetail, ward, district, province]
    .filter(Boolean)
    .join(", ")

  return {
    name: String(delivery?.name || contextDelivery?.name || "").trim(),
    phone: String(delivery?.phone || contextDelivery?.phone || "").trim(),
    address: String(mergedAddress || delivery?.address || contextDelivery?.address || "").trim()
  }
}

export async function createBackendCheckoutOrder({ cartItems, delivery, shipping = 0, discount = 0, total = null, paymentMethod = "VNPAY", statusNote = "", voucherCode = "", orderType = "ONLINE", orderStatusCode = "" }) {
  const safeItems = Array.isArray(cartItems) ? cartItems : []
  if (!safeItems.length) {
    throw new Error("Giỏ hàng đang trống")
  }

  const hasInvalidItem = safeItems.some((item) => {
    return Number(item?.quantity || 0) <= 0 || Number(item?.price || 0) <= 0 || !item?.name
  })
  if (hasInvalidItem) {
    throw new Error("Giỏ hàng có dữ liệu không hợp lệ")
  }

  const context = await loadCheckoutContext()
  if (context.account && context.accountActive === false) {
    throw new Error("Tài khoản đang ngừng hoạt động, không thể thanh toán")
  }
  const finalDelivery = normalizeDelivery(delivery, context.delivery)
  const ensuredCustomer = await ensureCustomerProfile(context.account, context.customer, finalDelivery)
  const nhanVienId = await resolveDefaultEmployeeId()

  if (!finalDelivery.name) {
    throw new Error("Thiếu tên người nhận")
  }
  if (!isValidPhone(finalDelivery.phone)) {
    throw new Error("Số điện thoại nhận hàng không hợp lệ")
  }
  if (finalDelivery.address.length < 10) {
    throw new Error("Địa chỉ nhận hàng cần chi tiết hơn")
  }
  if (!ensuredCustomer?.id) {
    throw new Error("Không tìm thấy hồ sơ khách hàng. Vui lòng cập nhật hồ sơ tài khoản trước khi thanh toán.")
  }
  if (!nhanVienId) {
    throw new Error("Không tìm thấy nhân viên xử lý đơn hàng. Vui lòng liên hệ quản trị.")
  }

  const productRes = await getAllSanPham()
  const rawProducts = Array.isArray(productRes?.data) ? productRes.data : []
  const products = await Promise.all(rawProducts.map(async (product) => {
    const variants = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []
    if (!variants.length) return product
    return {
      ...product,
      sanPhamChiTiets: await applyCampaignPriceToVariants(variants, product.id)
    }
  }))
  const variants = flattenVariants(products)
  const resolvedItems = safeItems.map((item) => ({
    ...item,
    variant: resolveVariantForItem(item, variants)
  }))
  const subtotal = safeItems.reduce((sum, item) => {
    return sum + Number(item?.price || 0) * Number(item?.quantity || 0)
  }, 0)
  const normalizedDiscount = Math.max(Number(discount || 0), 0)
  const normalizedTotal = Math.max(Number(total ?? (subtotal + Number(shipping || 0) - normalizedDiscount)), 0)

  const unresolvedItems = resolvedItems.filter((item) => !item.variant)
  if (unresolvedItems.length) {
    throw new Error(`Không map được sản phẩm sang dữ liệu backend: ${unresolvedItems.map((item) => item.name).join(", ")}`)
  }

  const invalidStockItems = resolvedItems.filter((item) => {
    const variantRaw = item?.variant?.raw || {}
    const stockCandidates = [
      variantRaw?.soLuong,
      variantRaw?.soLuongTon,
      variantRaw?.tonKho,
      variantRaw?.available,
    ]
    const inStock = stockCandidates
      .map((value) => Number(value))
      .find((value) => Number.isFinite(value) && value >= 0)
    if (!Number.isFinite(inStock)) return false

    const quantity = Number(item?.quantity || 0)
    return quantity > inStock
  })
  if (invalidStockItems.length) {
    throw new Error(`Số lượng vượt tồn kho: ${invalidStockItems.map((item) => item.name).join(", ")}`)
  }

  const normalizedOrderType = String(orderType || "ONLINE").trim().toUpperCase() === "POS" ? "POS" : "ONLINE"
  const rawPaymentMethod = String(paymentMethod || "").trim().toUpperCase()
  const normalizedPaymentMethod = ["COD", "VNPAY", "BANK", "CASH"].includes(rawPaymentMethod)
    ? rawPaymentMethod
    : (normalizedOrderType === "POS" ? "CASH" : "COD")

  let resolvedOrderStatusCode = String(orderStatusCode || "").trim().toUpperCase()
  if (!resolvedOrderStatusCode) {
    if (normalizedOrderType === "POS") {
      resolvedOrderStatusCode = normalizedPaymentMethod === "VNPAY" ? "CHO_LAY_HANG" : "HOAN_THANH"
    } else {
      resolvedOrderStatusCode = normalizedPaymentMethod === "VNPAY" ? "CHO_LAY_HANG" : "CHO_XAC_NHAN"
    }
  }

  const allowedStatusByOrderType = {
    POS: ["CHO_LAY_HANG", "HOAN_THANH", "HUY"],
    ONLINE: ["CHO_XAC_NHAN", "CHO_LAY_HANG", "DANG_GIAO", "GIAO_THAT_BAI", "HOAN_VE", "HOAN_THANH", "HUY"]
  }

  if (!allowedStatusByOrderType[normalizedOrderType].includes(resolvedOrderStatusCode)) {
    throw new Error(`Trạng thái ${resolvedOrderStatusCode} không hợp lệ cho đơn ${normalizedOrderType}`)
  }

  const createPayload = {
    nhanVienId,
    khachHangId: ensuredCustomer.id,
    soDienThoaiNhanHang: finalDelivery.phone,
    diaChiNhanHang: finalDelivery.address,
    phiShip: Number(shipping || 0),
    giaSauGiamGia: normalizedDiscount,
    giamGia: normalizedDiscount,
    discountAmount: normalizedDiscount,
    thanhTien: normalizedTotal,
    phuongThucThanhToan: normalizedPaymentMethod,
    orderStatusCode: resolvedOrderStatusCode,
    orderType: normalizedOrderType,
    statusNote: statusNote || `[${normalizedOrderType}] Đơn hàng checkout khách hàng - ${normalizedPaymentMethod}${voucherCode ? ` - Voucher ${voucherCode}` : ""}`
  }

  const createRes = await createHoaDon(createPayload)
  const orderId = createRes?.data?.hoaDon?.id || createRes?.data?.id || createRes?.data?.hoaDonId
  if (!orderId) {
    throw new Error("Không lấy được mã hóa đơn từ API")
  }

  for (const item of resolvedItems) {
    await addHoaDonItem(orderId, {
      spctId: item.variant.spctId,
      soLuong: Number(item.quantity || 1),
      giaBan: Number(item.variant.price || item.price || 0)
    })
  }

  return {
    context: {
      ...context,
      customer: ensuredCustomer
    },
    orderId,
    order: createRes?.data?.hoaDon || createRes?.data || null,
    items: resolvedItems.map((item) => ({
      ...item,
      spctId: item.variant.spctId
    }))
  }
}
