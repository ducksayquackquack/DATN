<script setup>
import { computed, onMounted, ref, watch } from "vue"
import { useRouter } from "vue-router"
import logo from "../../assets/img/logo/new logo.png?url"
import CustomerFooter from "../../components/customer/CustomerFooter.vue"
import momo from "../../assets/img/payments/momo.png?url"
import SiteNav from '../../components/SiteNav.vue'
import visa from "../../assets/img/payments/visa.png?url"
import mastercard from "../../assets/img/payments/mastercard.png?url"
import vnpay from "../../assets/img/payments/vnpay.png?url"
import { createVnpayPayment } from "../../services/vnpayService"
import { createBackendCheckoutOrder, loadCheckoutContext } from "../../services/checkoutOrderService"
import { getAllSanPham } from "../../services/sanPhamService"
import {
  calculateVoucherDiscount,
  getAllVouchers,
  getActiveVouchers,
  isVoucherApplicable
} from "../../services/khuyenMaiService"
import { useToast } from "../../composables/useToast"

const router = useRouter()
const toast = useToast()
const year = new Date().getFullYear()

const submitting = ref(false)
const loadingProfile = ref(true)
const errorMsg = ref("")
const voucherCode = ref("")
const voucherHint = ref("")
const vouchers = ref([])
const selectedVoucher = ref(null)
const loadingVouchers = ref(false)
const paymentMethod = ref("COD")
const vnpayChannel = ref("AUTO")
const checkoutVoucherDrawerOpen = ref(false)

const restoreCheckoutPaymentState = () => {
  try {
    const savedOrder = JSON.parse(localStorage.getItem("currentOrder") || "null")
    const savedMethod = String(savedOrder?.paymentMethod || "").toUpperCase()
    const savedChannel = String(savedOrder?.vnpayChannel || "").toUpperCase()

    if (savedMethod === "COD" || savedMethod === "VNPAY") {
      paymentMethod.value = savedMethod
    }

    if (["VNPAYQR", "AUTO", "ATM", "INTCARD"].includes(savedChannel)) {
      vnpayChannel.value = savedChannel
    }
  } catch {
    // Ignore invalid localStorage payload.
  }
}

const cart = ref(JSON.parse(localStorage.getItem("checkoutCart") || "[]"))
const customerId = ref(null)
const variantCatalog = ref({})

const delivery = ref({
  name: "",
  phone: "",
  address: ""
})

const VND = (n) => new Intl.NumberFormat("vi-VN").format(Number(n || 0)) + "₫"
const persistResolvedCustomerContext = (context) => {
  const account = context?.account || null
  const customer = context?.customer || null
  const deliveryData = context?.delivery || {}

  if (account?.id) localStorage.setItem("userId", String(account.id))
  if (account?.email) localStorage.setItem("userEmail", String(account.email).trim().toLowerCase())
  localStorage.setItem("role", "CUSTOMER")

  if (customer?.tenKhachHang || deliveryData?.name) {
    localStorage.setItem("userName", String(customer?.tenKhachHang || deliveryData?.name || ""))
  }
  if (customer?.soDienThoai || deliveryData?.phone) {
    localStorage.setItem("userPhone", String(customer?.soDienThoai || deliveryData?.phone || ""))
  }
  if (deliveryData?.address) {
    localStorage.setItem("userAddress", String(deliveryData.address || ""))
  }
}

const subtotal = computed(() => {
  return cart.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0)
})

const shipping = computed(() => (subtotal.value >= 1000000 ? 0 : 30000))
const voucherDiscount = computed(() => {
  if (!selectedVoucher.value) return 0
  return calculateVoucherDiscount(selectedVoucher.value, subtotal.value)
})

const total = computed(() => Math.max(subtotal.value + shipping.value - voucherDiscount.value, 0))
const cartCount = computed(() => cart.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0))

const displayVouchers = computed(() => {
  // Show all loaded vouchers; applicability is shown via applyVoucherFromDrawer toast
  return vouchers.value.slice(0, 8)
})

const persistCheckoutCart = () => {
  localStorage.setItem("checkoutCart", JSON.stringify(cart.value))
}

const clearCartStorage = () => {
  cart.value = []
  localStorage.removeItem("cart")
  localStorage.removeItem("checkoutCart")
}

const normalizeCartItemQuantity = (item) => {
  const qty = Number(item?.quantity || 1)
  item.quantity = Number.isFinite(qty) && qty > 0 ? Math.floor(qty) : 1
}

const increaseItemQuantity = (item) => {
  normalizeCartItemQuantity(item)
  item.quantity += 1
  persistCheckoutCart()
}

const decreaseItemQuantity = (item) => {
  normalizeCartItemQuantity(item)
  if (item.quantity <= 1) return
  item.quantity -= 1
  persistCheckoutCart()
}

const removeCartItem = (item) => {
  cart.value = cart.value.filter((entry) => {
    return !(
      String(entry?.id) === String(item?.id)
      && String(entry?.size || "") === String(item?.size || "")
      && String(entry?.color || "") === String(item?.color || "")
    )
  })
  persistCheckoutCart()
}

const getVariantsForItem = (item) => {
  const key = String(item?.id || "")
  return Array.isArray(variantCatalog.value[key]) ? variantCatalog.value[key] : []
}

const getColorOptions = (item) => {
  return [...new Set(getVariantsForItem(item).map((variant) => variant.color).filter(Boolean))]
}

const getSizeOptions = (item) => {
  const variants = getVariantsForItem(item)
  const filtered = item?.color
    ? variants.filter((variant) => variant.color === item.color)
    : variants
  return [...new Set(filtered.map((variant) => variant.size).filter(Boolean))]
}

const syncItemVariant = (item) => {
  const variants = getVariantsForItem(item)
  if (!variants.length) return

  let matched = variants.find((variant) => variant.color === item.color && variant.size === item.size)
  if (!matched && item.color) {
    matched = variants.find((variant) => variant.color === item.color)
  }
  if (!matched && item.size) {
    matched = variants.find((variant) => variant.size === item.size)
  }
  if (!matched) {
    matched = variants[0]
  }

  item.color = matched.color || item.color || ""
  item.size = matched.size || item.size || ""
  item.price = Number(matched.price || item.price || 0)
  item.spctId = matched.spctId || item.spctId
  if (matched.image) item.image = matched.image
}

const updateItemColor = (item, color) => {
  item.color = color
  const sizeOptions = getSizeOptions(item)
  if (!sizeOptions.includes(item.size)) {
    item.size = sizeOptions[0] || ""
  }
  syncItemVariant(item)
  persistCheckoutCart()
}

const updateItemSize = (item, size) => {
  item.size = size
  syncItemVariant(item)
  persistCheckoutCart()
}

const loadVariantCatalog = async () => {
  try {
    const response = await getAllSanPham()
    const products = Array.isArray(response?.data) ? response.data : []
    const catalog = {}

    for (const product of products) {
      const productId = String(product?.id || "")
      const variants = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []
      catalog[productId] = variants.map((variant) => ({
        spctId: variant?.id,
        color: String(variant?.mauSac?.tenMau || "").trim(),
        size: String(variant?.kichThuoc?.tenKichThuoc || "").trim(),
        price: Number(variant?.giaBan || 0),
        image: product?.anh || product?.hinhAnh || product?.images?.[0] || ""
      }))
    }

    variantCatalog.value = catalog
    cart.value.forEach((item) => syncItemVariant(item))
    persistCheckoutCart()
  } catch (error) {
    console.error("Load variant catalog failed:", error)
  }
}

const bestVoucher = computed(() => {
  if (!displayVouchers.value.length) return null
  return displayVouchers.value.reduce((best, current) => {
    const bDisc = calculateVoucherDiscount(best, subtotal.value)
    const cDisc = calculateVoucherDiscount(current, subtotal.value)
    return cDisc > bDisc ? current : best
  }, displayVouchers.value[0])
})

const canSubmit = computed(() => {
  if (submitting.value) return false
  return validateCheckout() === ""
})

const goHome = () => {
  router.push("/home")
}

const checkoutVoucherChipLabel = (v) => {
  return String(v?.maPhieuGiamGia || "")
}

const openCheckoutVoucherDrawer = () => {
  checkoutVoucherDrawerOpen.value = true
  document.body.style.overflow = 'hidden'
}

const closeCheckoutVoucherDrawer = () => {
  checkoutVoucherDrawerOpen.value = false
  document.body.style.overflow = ''
}

const applyVoucherFromDrawer = (v) => {
  if (!isVoucherApplicable(v, subtotal.value, customerId.value)) {
    const needed = Number(v.hoaDonToiThieu || 0) - subtotal.value
    if (needed > 0) {
      toast.error(`Cần thêm ${VND(needed)} để áp dụng voucher này`)
    } else {
      toast.error('Voucher không khả dụng')
    }
    return
  }
  selectedVoucher.value = v
  voucherCode.value = v.maPhieuGiamGia
  voucherHint.value = `Đã áp dụng ${v.maPhieuGiamGia}`
  closeCheckoutVoucherDrawer()
}

const showDemoToast = (message) => {
  if (typeof toast?.info === "function") {
    toast.info(message)
    return
  }
  if (typeof toast?.success === "function") {
    toast.success(message)
  }
}

const loadVouchers = async () => {
  loadingVouchers.value = true
  try {
    const normalizeVoucher = (voucher) => ({
      ...voucher,
      id: voucher?.id ?? voucher?.phieuGiamGiaId ?? voucher?.maPhieuGiamGia ?? voucher?.maPhieu,
      maPhieuGiamGia: String(voucher?.maPhieuGiamGia || voucher?.maPhieu || voucher?.maKhuyenMai || voucher?.code || "").trim(),
      tenPhieuGiamGia: String(voucher?.tenPhieuGiamGia || voucher?.tenKhuyenMai || voucher?.tenPhieu || "").trim(),
      giaTriGiamGia: Number(voucher?.giaTriGiamGia ?? voucher?.giaTriGiam ?? 0),
      hoaDonToiThieu: Number(voucher?.hoaDonToiThieu ?? voucher?.donToiThieu ?? voucher?.dieuKienToiThieu ?? 0),
      soTienGiamToiDa: Number(voucher?.soTienGiamToiDa ?? 0)
    })

    let payload = []

    try {
      const response = await getActiveVouchers()
      const activeData = response?.data
      payload = Array.isArray(activeData)
        ? activeData
        : (Array.isArray(activeData?.content) ? activeData.content : [])
      if (!payload.length) {
        const fallbackResponse = await getAllVouchers()
        const fallbackData = fallbackResponse?.data
        payload = Array.isArray(fallbackData)
          ? fallbackData
          : (Array.isArray(fallbackData?.content) ? fallbackData.content : [])
      }
    } catch {
      const response = await getAllVouchers()
      const fullData = response?.data
      payload = Array.isArray(fullData)
        ? fullData
        : (Array.isArray(fullData?.content) ? fullData.content : [])
    }

    vouchers.value = payload.map(normalizeVoucher).filter((voucher) => voucher.maPhieuGiamGia)
  } catch (error) {
    vouchers.value = []
    console.error("Load vouchers failed:", error)
  } finally {
    loadingVouchers.value = false
  }
}

const applyVoucherCode = () => {
  voucherHint.value = ""

  if (!voucherCode.value.trim()) {
    selectedVoucher.value = null
    return
  }

  const normalizedCode = voucherCode.value.trim().toLowerCase()
  const found = vouchers.value.find(
    (voucher) => String(voucher?.maPhieuGiamGia || "").trim().toLowerCase() === normalizedCode
  )

  if (!found) {
    selectedVoucher.value = null
    voucherHint.value = "Không tìm thấy mã voucher hợp lệ"
    return
  }

  if (!isVoucherApplicable(found, subtotal.value, customerId.value)) {
    selectedVoucher.value = null
    voucherHint.value = "Voucher chưa đủ điều kiện áp dụng cho đơn hiện tại"
    return
  }

  selectedVoucher.value = found
  voucherHint.value = `Đã áp dụng ${found.maPhieuGiamGia}`
}

const removeVoucher = () => {
  selectedVoucher.value = null
  voucherCode.value = ""
  voucherHint.value = ""
}

const isValidPhone = (value) => {
  const phone = String(value || "").replace(/\s+/g, "")
  return /^(0|\+84)\d{9,10}$/.test(phone)
}

const validateCheckout = () => {
  if (!Array.isArray(cart.value) || cart.value.length === 0) {
    return "Giỏ hàng đang trống"
  }

  const hasInvalidItem = cart.value.some((item) => {
    return Number(item?.quantity || 0) <= 0 || Number(item?.price || 0) <= 0 || !item?.name
  })
  if (hasInvalidItem) {
    return "Giỏ hàng có dữ liệu không hợp lệ, vui lòng quay lại trang chủ"
  }

  if (!delivery.value.name.trim()) return "Vui lòng nhập họ tên người nhận"
  if (!isValidPhone(delivery.value.phone)) return "Số điện thoại không hợp lệ"
  if (delivery.value.address.trim().length < 10) return "Địa chỉ nhận hàng cần chi tiết hơn"

  return ""
}

const getVoucherDescription = (v) => {
  if (!v) return ""

  const discountValue = Number(v.giaTriGiamGia ?? v.giaTriGiam ?? 0)
  const minimumOrder = Number(v.hoaDonToiThieu ?? v.dieuKienToiThieu ?? 0)
  const maxDiscount = Number(v.soTienGiamToiDa || 0)
  const isPercent = Boolean(v.hinhThucGiam)
    || String(v.loaiKhuyenMai || "").toUpperCase().includes("PHAN_TRAM")
    || String(v.loaiKhuyenMai || "").toUpperCase().includes("PERCENT")

  if (isPercent) {
    if (maxDiscount > 0) {
      return `Giảm ${discountValue}% (tối đa ${Math.round(maxDiscount / 1000)}K) đơn từ ${Math.round(minimumOrder / 1000)}K`
    }
    return `Giảm ${discountValue}% đơn từ ${Math.round(minimumOrder / 1000)}K`
  }

  return `Giảm ${Math.round(discountValue / 1000)}K đơn từ ${Math.round(minimumOrder / 1000)}K`
}

const formatExpiry = (dateStr) => {
    if (!dateStr) return "N/A"
    try {
      const d = new Date(dateStr)
      return isNaN(d) ? String(dateStr) : `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}/${d.getFullYear()}`
    } catch {
      return String(dateStr)
    }
}

const selectVoucherCard = (v) => {
    if (selectedVoucher.value?.id === v.id) {
      removeVoucher()
      return
    }
    if (!isVoucherApplicable(v, subtotal.value, customerId.value)) {
      toast.error("Voucher chưa đủ điều kiện áp dụng cho đơn hiện tại")
      return
    }
    selectedVoucher.value = v
    voucherCode.value = v.maPhieuGiamGia
    voucherHint.value = `Đã áp dụng ${v.maPhieuGiamGia}`
}

const loadProfile = async () => {
  loadingProfile.value = true
  try {
    const context = await loadCheckoutContext()
    persistResolvedCustomerContext(context)
    customerId.value = context.customer?.id || null
    delivery.value.name = context.delivery.name || ""
    delivery.value.phone = context.delivery.phone || ""
    delivery.value.address = context.delivery.address || ""
  } catch (error) {
    console.error("Load checkout profile failed:", error)
  } finally {
    loadingProfile.value = false
  }
}

const resolveBankCode = () => {
  if (vnpayChannel.value === "VNPAYQR") return "VNPAYQR"
  if (vnpayChannel.value === "ATM") return "VNBANK"
  if (vnpayChannel.value === "INTCARD") return "INTCARD"
  // AUTO lets VNPay render available methods.
  return ""
}

const submitOrder = async () => {
  const validationError = validateCheckout()
  if (validationError) {
    errorMsg.value = validationError
    toast.error(validationError)
    return
  }

  errorMsg.value = ""
  submitting.value = true

  try {
    const orderId = `DH${Date.now()}`
    const normalizedPaymentMethod = paymentMethod.value === "COD" ? "COD" : "VNPAY"

    localStorage.setItem(
      "currentOrder",
      JSON.stringify({
        version: 2,
        id: orderId,
        customerId: customerId.value,
        delivery: {
          name: delivery.value.name.trim(),
          phone: delivery.value.phone.trim(),
          address: delivery.value.address.trim()
        },
        shipping: shipping.value,
        discount: voucherDiscount.value,
        voucher: selectedVoucher.value
          ? {
              id: selectedVoucher.value.id,
              code: selectedVoucher.value.maPhieuGiamGia,
              value: voucherDiscount.value
            }
          : null,
        items: cart.value,
        total: total.value,
        paymentMethod: normalizedPaymentMethod,
        vnpayChannel: vnpayChannel.value,
        createdAt: new Date().toISOString()
      })
    )

    if (normalizedPaymentMethod === "COD") {
      try {
        await createBackendCheckoutOrder({
          cartItems: cart.value,
          delivery: {
            name: delivery.value.name.trim(),
            phone: delivery.value.phone.trim(),
            address: delivery.value.address.trim()
          },
          shipping: shipping.value,
          discount: voucherDiscount.value,
          total: total.value,
          paymentMethod: "COD",
          voucherCode: selectedVoucher.value?.maPhieuGiamGia || ""
        })
      } catch (backendError) {
        console.warn("COD backend checkout fallback activated:", backendError)
        const pendingKey = "pendingOfflineOrders"
        const existing = JSON.parse(localStorage.getItem(pendingKey) || "[]")
        existing.unshift({
          id: orderId,
          createdAt: new Date().toISOString(),
          customerId: customerId.value,
          delivery: {
            name: delivery.value.name.trim(),
            phone: delivery.value.phone.trim(),
            address: delivery.value.address.trim()
          },
          shipping: shipping.value,
          discount: voucherDiscount.value,
          voucherCode: selectedVoucher.value?.maPhieuGiamGia || "",
          items: cart.value,
          total: total.value,
          paymentMethod: "COD",
          syncStatus: "pending"
        })
        localStorage.setItem(pendingKey, JSON.stringify(existing))
      }

      localStorage.removeItem("currentOrder")
      clearCartStorage()
      toast.success("Đặt hàng COD thành công")
      router.push("/home")
      return
    }

    const res = await createVnpayPayment({
      amount: total.value,
      orderId,
      bankCode: resolveBankCode()
    })

    const url = res?.data?.url
    if (!url) {
      throw new Error("Không tạo được link thanh toán VNPay")
    }

    window.location.href = url
  } catch (error) {
    console.error("Checkout failed:", error)
    errorMsg.value = error?.response?.data?.message || error.message || "Không thể đặt hàng. Vui lòng thử lại."
    toast.error(errorMsg.value)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  restoreCheckoutPaymentState()
  cart.value.forEach((item) => normalizeCartItemQuantity(item))
  persistCheckoutCart()
  await Promise.all([loadProfile(), loadVouchers()])
  loadVariantCatalog()

  // Pre-apply voucher selected from ProductDetail page
  const savedVoucher = localStorage.getItem('checkoutSelectedVoucher')
  if (savedVoucher) {
    try {
      localStorage.removeItem('checkoutSelectedVoucher')
      const preSelected = JSON.parse(savedVoucher)
      const found = vouchers.value.find(v =>
        v.id === preSelected.id || v.maPhieuGiamGia === preSelected.maPhieuGiamGia
      )
      if (found && isVoucherApplicable(found, subtotal.value, customerId.value)) {
        selectedVoucher.value = found
        voucherCode.value = found.maPhieuGiamGia
        voucherHint.value = `Đã áp dụng ${found.maPhieuGiamGia}`
      }
    } catch {}
  }
})

watch([subtotal, customerId], () => {
  if (selectedVoucher.value && !isVoucherApplicable(selectedVoucher.value, subtotal.value, customerId.value)) {
    selectedVoucher.value = null
    voucherCode.value = ""
    voucherHint.value = "Voucher không còn phù hợp với giá trị đơn hiện tại"
  }
})

</script>

<template>
  <div class="checkout-page page-shell">
    <SiteNav />

    <div class="checkout-breadcrumb-wrap">
      <div class="checkout-breadcrumb-inner">
        <div class="breadcrumbs breadcrumbs-standalone">
          <button class="crumb" @click="goHome">Trang chủ</button>
          <span>/</span>
          <span class="crumb-active">Checkout</span>
          <button class="crumb back-home" @click="goHome">Quay về trang chủ</button>
        </div>
      </div>
    </div>

    <main class="checkout-shell">
      <section class="checkout-left">
        <div class="section-card">
          <div class="section-head">
            <h2>Thông tin đơn hàng</h2>
          </div>

          <div class="form-grid single">
            <input v-model="delivery.name" type="text" placeholder="Họ và tên" />
            <input v-model="delivery.phone" type="text" placeholder="Số điện thoại" />
            <input v-model="delivery.address" type="text" placeholder="Địa chỉ" />
          </div>

          <div class="address-note" v-if="loadingProfile">Đang tải thông tin giao hàng...</div>
        </div>

        <div class="section-card">
          <h3>Phương thức vận chuyển</h3>
          <label class="option-card option-card-active">
            <div class="option-main">
              <span class="option-icon">🚚</span>
              <div>
                <strong>Freeship đơn hàng</strong>
                <p>{{ shipping === 0 ? 'Miễn phí cho đơn từ 1tr trở lên' : `Phí vận chuyển ${VND(shipping)}` }}</p>
              </div>
            </div>
          </label>
        </div>

        <div class="section-card">
          <h3>Hình thức thanh toán</h3>

          <label class="payment-card payment-card-cod" :class="{ active: paymentMethod === 'COD' }">
            <input type="radio" name="paymentMethod" value="COD" v-model="paymentMethod" />
            <div class="payment-badge payment-badge-cod">COD</div>
            <div class="payment-copy">
              <strong>Thanh toán khi giao hàng (COD)</strong>
              <p>- Khách hàng được kiểm tra hàng trước khi nhận hàng.</p>
            </div>
          </label>

          <label class="payment-card payment-card-vnpay" :class="{ active: paymentMethod === 'VNPAY' }">
            <input type="radio" name="paymentMethod" value="VNPAY" v-model="paymentMethod" />
            <div class="payment-badge payment-badge-vnpay">VNPAY</div>
            <div class="payment-copy">
              <strong>Ví điện tử VNPay</strong>
              <p>Thanh toán qua cổng VNPay (QR/ATM/Internet Banking).</p>
            </div>
          </label>

          <div class="payment-card payment-card-momo payment-disabled">
            <input type="radio" name="paymentMethod" disabled />
            <div class="payment-badge payment-badge-momo">MOMO</div>
            <div class="payment-copy">
              <strong>Thanh toán MoMo</strong>
              <p>Tính năng sắp ra mắt.</p>
            </div>
          </div>

          <div v-if="paymentMethod === 'VNPAY'" class="vnpay-channel-row">
            <label>
              <input type="radio" name="vnpayChannel" value="VNPAYQR" v-model="vnpayChannel" /> VNPay QR Code
            </label>
            <label>
              <input type="radio" name="vnpayChannel" value="ATM" v-model="vnpayChannel" /> ATM / Internet Banking
            </label>
            <label>
              <input type="radio" name="vnpayChannel" value="INTCARD" v-model="vnpayChannel" /> Thẻ quốc tế
            </label>
            <label>
              <input type="radio" name="vnpayChannel" value="AUTO" v-model="vnpayChannel" /> Tự chọn tại cổng VNPay
            </label>
          </div>
        </div>

        <div v-if="errorMsg" class="error-box">{{ errorMsg }}</div>
      </section>

      <aside class="checkout-right">
        <div class="cart-panel">
          <div class="cart-panel-head">
            <h2>Giỏ hàng</h2>
            <span>{{ cartCount }} sản phẩm</span>
          </div>

          <div class="cart-progress-line"><span></span></div>

          <div v-if="!cart.length" class="empty-cart">Giỏ hàng đang trống</div>

          <div v-for="item in cart" :key="item.id + (item.size || '') + (item.color || '')" class="cart-item-icd">
            <img v-if="item.image" :src="item.image" :alt="item.name" class="cart-thumb" />
            <div v-else class="cart-thumb cart-thumb-fallback">SP</div>

            <div class="cart-body-icd">
              <div class="cart-top-icd">
                <strong>{{ item.name }}</strong>
                <button class="remove-btn" type="button" @click="removeCartItem(item)">×</button>
              </div>

              <div class="delivery-tag">Đổi ý 15 ngày</div>

              <div class="variant-line">
                <select class="variant-select" :value="item.color || ''" @change="updateItemColor(item, $event.target.value)">
                  <option value="" disabled>Chọn màu</option>
                  <option v-for="color in getColorOptions(item)" :key="`${item.id}-color-${color}`" :value="color">
                    Màu: {{ color }}
                  </option>
                </select>
                <select class="variant-select" :value="item.size || ''" @change="updateItemSize(item, $event.target.value)">
                  <option value="" disabled>Chọn size</option>
                  <option v-for="size in getSizeOptions(item)" :key="`${item.id}-size-${size}`" :value="size">
                    Size: {{ size }}
                  </option>
                </select>
              </div>

              <div class="qty-price-row">
                <div class="qty-chip">
                  <button type="button" class="qty-btn" @click="decreaseItemQuantity(item)">−</button>
                  <b>{{ item.quantity }}</b>
                  <button type="button" class="qty-btn" @click="increaseItemQuantity(item)">+</button>
                </div>
                <div class="cart-price">{{ VND(item.price * item.quantity) }}</div>
              </div>
            </div>
          </div>

          <div class="promo-box">
            <div class="promo-title">Ưu Đãi Dành Cho Bạn</div>

            <!-- Voucher chips row: click any to open selector drawer -->
            <div v-if="vouchers.length && !loadingVouchers" class="promo-chips-row">
              <button
                v-for="v in vouchers"
                :key="v.id || v.maPhieuGiamGia"
                type="button"
                class="promo-chip"
                :class="{ 'is-selected': selectedVoucher?.maPhieuGiamGia === v.maPhieuGiamGia }"
                @click="openCheckoutVoucherDrawer"
              >
                {{ checkoutVoucherChipLabel(v) }}
              </button>
            </div>

            <div class="promo-select-row">
              <button type="button" class="promo-select-btn" :disabled="loadingVouchers || !vouchers.length" @click="openCheckoutVoucherDrawer">
                {{ selectedVoucher ? `Đổi voucher (${selectedVoucher.maPhieuGiamGia})` : 'Chọn voucher từ danh sách' }}
              </button>
            </div>
            <small v-if="voucherHint" :class="{ 'promo-ok': selectedVoucher, 'promo-error': !selectedVoucher }">{{ voucherHint }}</small>
            <small v-if="!loadingVouchers && !vouchers.length" class="promo-muted">Hiện chưa có voucher theo dữ liệu khuyến mãi.</small>
          </div>

          <div class="checkout-totals">
            <div class="checkout-total-row">
              <span>Tạm tính:</span>
              <span>
                {{ VND(subtotal) }}
                <small v-if="voucherDiscount > 0" class="saving-inline">Tiết kiệm: {{ VND(voucherDiscount) }}</small>
              </span>
            </div>
            <div class="checkout-total-row">
              <span>Phí vận chuyển:</span>
              <span class="free-label">{{ shipping === 0 ? 'Miễn phí' : VND(shipping) }}</span>
            </div>
            <div class="checkout-total-row muted">
              <span>Voucher giảm giá:</span>
              <span>-{{ VND(voucherDiscount) }}</span>
            </div>
            <div class="checkout-grand-total">
              <span>Tổng:</span>
              <div class="grand-right">
                <strong>{{ VND(total) }}</strong>
                <div class="savings-note">(Đã giảm {{ VND(voucherDiscount) }} trên giá gốc)</div>
              </div>
            </div>
          </div>

          <button class="submit-btn" :disabled="!canSubmit" @click="submitOrder">
            <span v-if="submitting">Đang xử lý...</span>
            <span v-else>Thanh toán</span>
          </button>
        </div>
      </aside>
    </main>

    <CustomerFooter />

    <!-- Voucher Selector Drawer -->
    <transition name="co-drawer">
      <div v-if="checkoutVoucherDrawerOpen" class="co-drawer-overlay" @click.self="closeCheckoutVoucherDrawer">
        <aside class="co-drawer">
          <div class="co-drawer__head">
            <div>
              <small>Voucher giảm giá - áp dụng online</small>
              <h3>Chọn voucher</h3>
            </div>
            <button type="button" class="co-drawer__close" @click="closeCheckoutVoucherDrawer">✕</button>
          </div>
          <div class="co-drawer__list">
            <article
              v-for="v in vouchers"
              :key="v.id || v.maPhieuGiamGia"
              class="co-drawer__voucher"
              :class="{ 'is-selected': selectedVoucher?.maPhieuGiamGia === v.maPhieuGiamGia }"
            >
              <div class="co-drawer__info">
                <span class="co-drawer__label">{{ v.tenPhieuGiamGia || 'Voucher' }}</span>
                <strong class="co-drawer__amount">{{ VND(calculateVoucherDiscount(v, subtotal)) }}</strong>
                <p class="co-drawer__min">Đơn từ {{ VND(v.hoaDonToiThieu || 0) }}</p>
                <p class="co-drawer__code">Mã: <b>{{ v.maPhieuGiamGia }}</b></p>
              </div>
              <button
                type="button"
                class="co-apply-btn"
                :class="{ 'is-applied': selectedVoucher?.maPhieuGiamGia === v.maPhieuGiamGia }"
                @click="applyVoucherFromDrawer(v)"
              >
                {{ selectedVoucher?.maPhieuGiamGia === v.maPhieuGiamGia ? '✓ Đã chọn' : 'Áp dụng' }}
              </button>
            </article>
            <p v-if="!vouchers.length" class="co-drawer__empty">Hiện không có phiếu giảm giá đang hoạt động.</p>
          </div>
        </aside>
      </div>
    </transition>

  </div>
</template>

<style scoped>
@import "./home.css";

.page-shell {
  --bg: #f7f8fc;
}

.checkout-page {
  min-height: 100vh;
  background: var(--bg);
  color: #111827;
  font-family: "Segoe UI", Tahoma, sans-serif;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: static;
}

.menu {
  gap: 12px;
}

.breadcrumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.crumb {
  border: none;
  background: none;
  color: inherit;
  cursor: pointer;
}

.crumb-active {
  color: #991b1b;
  font-weight: 700;
}

.back-home {
  margin-left: auto;
  color: #b91c1c;
  font-weight: 700;
}

.checkout-breadcrumb-wrap {
  max-width: 1280px;
  margin: 8px auto 0;
  padding: 0 24px;
}

.checkout-breadcrumb-inner {
  padding: 8px 2px 0;
}

.breadcrumbs-standalone {
  margin: 0;
}

.checkout-shell {
  max-width: 1280px;
  margin: 0 auto;
  padding: 18px 24px 40px;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(340px, 0.95fr);
  gap: 20px;
  align-items: start;
}

.section-card,
.cart-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.section-card + .section-card {
  margin-top: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-card h2,
.section-card h3,
.cart-panel h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.autofill-badge {
  background: #fff1f2;
  color: #b91c1c;
  border: 1px solid #fecdd3;
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 999px;
}

.form-grid.single {
  display: grid;
  gap: 10px;
}

.form-grid input {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  padding: 10px 12px;
  font-size: 14px;
}

.address-note {
  margin-top: 10px;
  color: #6b7280;
  font-size: 12px;
}

.option-card,
.payment-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  padding: 14px;
  background: #fff;
}

.option-card-active,
.payment-card.active {
  border-color: #dc2626;
  box-shadow: inset 0 0 0 1px #dc2626;
}

.payment-card + .payment-card {
  margin-top: 12px;
}

.payment-card-cod {
  border-color: #fecaca;
}

.payment-card-cod.active {
  border-color: #b91c1c;
  box-shadow: inset 0 0 0 1px #b91c1c;
}

.payment-card-vnpay.active {
  border-color: #dc2626;
  box-shadow: inset 0 0 0 1px #dc2626;
}

.payment-card-momo {
  margin-top: 12px;
}

.payment-disabled {
  opacity: 0.68;
}

.payment-card input,
.option-card input {
  margin-top: 4px;
}

.option-main {
  display: flex;
  gap: 12px;
}

.option-icon {
  font-size: 20px;
}

.option-main p,
.payment-copy p {
  margin: 4px 0 0;
  color: #475569;
  font-size: 13px;
}

.payment-badge {
  min-width: 44px;
  height: 24px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 800;
  color: #fff;
}

.payment-badge-cod {
  background: #b91c1c;
}

.payment-badge-vnpay {
  background: #dc2626;
}

.payment-badge-momo {
  background: #be185d;
}

.qr-tag {
  font-size: 11px;
  color: #dc2626;
}

.vnpay-channel-row {
  margin-top: 10px;
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #334155;
}

.error-box {
  margin-top: 16px;
  padding: 12px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 6px;
  color: #b91c1c;
  font-size: 14px;
}

.checkout-right {
  position: sticky;
  top: 16px;
}

.cart-panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.cart-panel-head span {
  color: #6b7280;
  font-size: 13px;
}

.cart-progress-line {
  height: 5px;
  background: #fee2e2;
  border-radius: 999px;
  margin-bottom: 8px;
}

.cart-progress-line span {
  display: block;
  width: 42%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #dc2626, #b91c1c);
}

.cart-offer {
  background: linear-gradient(90deg, #dc2626, #b91c1c);
  color: #fff;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 14px;
  line-height: 1.35;
}

.promo-box {
  border: 1px solid #dbe2ec;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
}

.promo-chips-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.promo-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  min-width: 86px;
  padding: 0 11px;
  border: 1px solid #ef4444;
  border-radius: 4px;
  color: #b91c1c;
  background: #ffffff;
  font-size: 11px;
  font-weight: 700;
  clip-path: polygon(0 0, 100% 0, 100% 44%, 98.5% 50%, 100% 56%, 100% 100%, 0 100%, 0 56%, 1.5% 50%, 0 44%);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.promo-chip.is-selected {
  background: #b91c1c;
  border-color: #b91c1c;
  color: white;
}

/* ── Checkout Voucher Drawer ── */
.co-drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  justify-content: flex-end;
  background: rgba(17, 24, 39, 0.32);
}

.co-drawer {
  width: min(360px, 100%);
  height: 100%;
  padding: 18px;
  background: white;
  box-shadow: -20px 0 40px rgba(0, 0, 0, 0.12);
  overflow-y: auto;
}

.co-drawer__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.co-drawer__head small {
  color: #b91c1c;
  font-size: 11px;
  text-transform: uppercase;
}

.co-drawer__head h3 {
  margin: 4px 0 0;
  font-size: 18px;
}

.co-drawer__close {
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  background: white;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
}

.co-drawer__list {
  display: grid;
  gap: 12px;
}

.co-drawer__voucher {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px;
  border: 1px solid #f0dde1;
  background: #fff7f8;
  border-radius: 6px;
}

.co-drawer__voucher.is-selected {
  border-color: #b91c1c;
  background: #fff1f2;
}

.co-drawer__info {
  flex: 1;
  min-width: 0;
}

.co-drawer__label {
  display: block;
  color: #b91c1c;
  font-size: 11px;
  text-transform: uppercase;
  font-weight: 700;
}

.co-drawer__amount {
  display: block;
  margin-top: 4px;
  color: #991b1b;
  font-size: 20px;
}

.co-drawer__min,
.co-drawer__code {
  margin: 2px 0 0;
  color: #4b5563;
  font-size: 12px;
}

.co-drawer__code b { color: #991b1b; }

.co-apply-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  background: #991b1b;
  color: white;
  font-size: 12px;
  font-weight: 600;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.15s;
}

.co-apply-btn.is-applied { background: #059669; }

.co-drawer__empty {
  color: #6b7280;
  font-size: 13px;
  text-align: center;
  padding: 16px;
}

.co-drawer-enter-active,
.co-drawer-leave-active {
  transition: opacity 0.22s ease;
}

.co-drawer-enter-active .co-drawer,
.co-drawer-leave-active .co-drawer {
  transition: transform 0.22s ease;
}

.co-drawer-enter-from,
.co-drawer-leave-to { opacity: 0; }

.co-drawer-enter-from .co-drawer,
.co-drawer-leave-to .co-drawer {
  transform: translateX(100%);
}

.promo-title {
  font-size: 18px;
  font-weight: 900;
  margin-bottom: 8px;
}

.promo-row {
  display: grid;
  grid-template-columns: 1fr 170px;
  gap: 10px;
}

.promo-select-row {
  margin-top: 8px;
}

.promo-select-btn {
  width: 100%;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  padding: 10px 12px;
  font-size: 14px;
  font-weight: 700;
  background: #fff;
  color: #0f172a;
  cursor: pointer;
}

.promo-select-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.promo-row input,
.promo-row button {
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  padding: 9px 12px;
  font-size: 14px;
}

.promo-row button {
  background: #b91c1c;
  color: #fff;
  font-weight: 700;
}

.promo-box small {
  display: block;
  margin-top: 8px;
  font-size: 12px;
}

.promo-ok {
  color: #047857;
}

.promo-error {
  color: #dc2626;
}

.promo-muted {
  color: #6b7280;
}

.voucher-picker {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding: 4px 0 6px;
}

.voucher-chip {
  min-width: 220px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 12px 14px;
  background: #fff5f5;
  cursor: pointer;
  position: relative;
}

.voucher-chip-selected {
  border-color: #dc2626;
  box-shadow: inset 0 0 0 1px #dc2626;
}

.vc-badge {
  position: absolute;
  top: 6px;
  right: 8px;
  font-size: 9px;
  font-weight: 800;
  color: #fff;
  background: #ef4444;
  border-radius: 4px;
  padding: 2px 6px;
  max-width: calc(100% - 16px);
  white-space: nowrap;
}

.vc-code {
  font-size: 30px;
  font-weight: 800;
  line-height: 1.1;
  margin: 20px 0 4px;
  color: #991b1b;
}

.vc-desc {
  font-size: 14px;
  color: #475569;
}

.vc-expiry {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.cart-item-icd {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #edf2f7;
}

.cart-body-icd {
  min-width: 0;
}

.cart-top-icd {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.cart-top-icd strong {
  font-size: 17px;
  line-height: 1.35;
}

.remove-btn {
  border: none;
  background: transparent;
  color: #111827;
  font-size: 24px;
  line-height: 1;
  font-weight: 700;
  cursor: pointer;
}

.delivery-tag {
  display: inline-flex;
  margin-top: 6px;
  border: 1px solid #fca5a5;
  color: #ef4444;
  background: #fff5f5;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  padding: 2px 8px;
}

.variant-line {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.variant-select {
  min-width: 120px;
  min-height: 32px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 0 10px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
}

.qty-price-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.qty-chip {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  min-height: 32px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #475569;
  font-size: 14px;
}

.qty-btn {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #111827;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-weight: 700;
  line-height: 1;
}

.empty-cart {
  color: #64748b;
  padding: 20px 0;
}

.cart-thumb {
  width: 128px;
  height: 160px;
  object-fit: cover;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.cart-thumb-fallback {
  display: grid;
  place-items: center;
  background: #f1f5f9;
  color: #94a3b8;
  font-weight: 700;
}

.cart-price {
  font-size: 24px;
  font-weight: 700;
}

.checkout-totals {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #e5e7eb;
}

.checkout-total-row,
.checkout-grand-total {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  font-size: 15px;
}

.checkout-total-row.muted {
  color: #4b5563;
}

.saving-inline {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: #ef4444;
}

.free-label {
  color: #0f172a;
  font-weight: 500;
}

.checkout-grand-total {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #e5e7eb;
  font-size: 18px;
}

.grand-right {
  text-align: right;
}

.checkout-grand-total strong {
  color: #111827;
}

.savings-note {
  font-size: 10px;
  color: #ef4444;
}

.payment-card-cod .payment-copy p:first-of-type {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #d1d5db;
}

.submit-btn {
  width: 100%;
  margin-top: 16px;
  border: none;
  border-radius: 4px;
  background: #dc2626;
  color: #fff;
  padding: 12px 16px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.profile-wrapper {
  position: relative;
}

.user-account-btn {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 12px;
  min-height: 42px;
  padding: 6px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.profile-avatar-chip {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
  background: #f1f5f9;
  overflow: hidden;
}

.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-identity {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.15;
}

.profile-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text);
}

.profile-role {
  font-size: 11px;
  color: var(--muted);
}

.profile-wrapper::after {
  content: "";
  position: absolute;
  top: 100%;
  right: 0;
  width: 220px;
  height: 14px;
}

.profile-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  padding: 8px;
  display: flex;
  flex-direction: column;
  min-width: 180px;
  z-index: 100;
  border: 1px solid var(--line);
  animation: fadeInMenu 0.16s ease;
}

@keyframes fadeInMenu {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dropdown-item {
  padding: 12px 14px;
  border: none;
  background: none;
  text-align: left;
  font-size: 14px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text);
  font-weight: 500;
  transition: all 0.2s;
}

.dropdown-item:hover {
  background: #f3f4f6;
}

.dropdown-item.danger {
  color: #dc2626;
}

.dropdown-item.danger:hover {
  background: #fee2e2;
}

@media (max-width: 1024px) {
  .checkout-shell {
    grid-template-columns: 1fr;
  }

  .checkout-right {
    position: static;
  }
}

@media (max-width: 768px) {
  header .container,
  .checkout-shell,
  .checkout-breadcrumb-wrap {
    padding-left: 16px;
    padding-right: 16px;
  }

  .actions .search {
    display: none;
  }

  .promo-title {
    font-size: 24px;
  }

  .promo-row {
    grid-template-columns: 1fr;
  }

  .vc-code {
    font-size: 18px;
  }

  .vc-desc,
  .vc-expiry {
    font-size: 13px;
  }

  .checkout-total-row {
    font-size: 13px;
  }

  .checkout-grand-total {
    font-size: 18px;
  }

  .cart-item-icd {
    grid-template-columns: 92px minmax(0, 1fr);
  }

  .cart-thumb {
    width: 92px;
    height: 116px;
  }

  .cart-top-icd strong,
  .cart-price,
  .qty-chip,
  .variant-select,
  .delivery-tag {
    font-size: 13px;
  }
}
</style>

