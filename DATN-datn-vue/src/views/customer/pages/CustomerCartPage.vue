<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue"
import { useRouter } from "vue-router"
import SiteNav from "../../../components/SiteNav.vue"
import CustomerFooter from "../../../components/customer/CustomerFooter.vue"
import { getAllSanPham } from "../../../services/sanPhamService"
import img1 from "../../../assets/img/Jackets/Áo bomber da lộn DirtyWave.jpg?url"
import img2 from "../../../assets/img/Jackets/Áo bomber dáng lửng.jpg?url"
import img3 from "../../../assets/img/Jackets/Áo bomber giả da DirtyWave.jpg?url"
import img4 from "../../../assets/img/Jackets/Áo bomber nhẹ vải cotton DirtyWave.jpg?url"
import img5 from "../../../assets/img/Jackets/Áo hoodie kéo khoá dáng hộp DirtyWave.jpg?url"
import img6 from "../../../assets/img/Jackets/Áo hoodie kéo khoá in hình DirtyWave.jpg?url"
import img7 from "../../../assets/img/Jackets/Áo hoodie kéo khoá Jacket DirtyWave.jpg?url"
import img8 from "../../../assets/img/Jackets/Áo khoác coach cách nhiệt vải Timberland.jpg?url"
import img9 from "../../../assets/img/Jackets/Áo khoac coach da ASOS DirtyWave.jpg?url"
import img10 from "../../../assets/img/Jackets/Áo khoác coach giả da DirtyWave.jpg?url"
import img11 from "../../../assets/img/Jackets/Áo khoác coach lông cừu DirtyWave.jpg?url"

const router = useRouter()

const CART_STORAGE_KEY = "cart"
const CHECKOUT_CART_STORAGE_KEY = "checkoutCart"
const CART_UPDATED_EVENT = "dirtywave:cart-updated"
const year = new Date().getFullYear()

const cart = ref({})
const cartVariants = ref({})
const selectedLineKeys = ref([])
const loadingProducts = ref(false)
const backendProducts = ref([])

const fallbackProducts = [
  { id: 1, name: "Áo Bomber Da Lộn DirtyWave", cat: "Bomber", price: 649000, img: img1 },
  { id: 2, name: "Áo Bomber Dáng Lửng", cat: "Bomber", price: 399000, img: img2 },
  { id: 3, name: "Áo Bomber Giả Da DirtyWave", cat: "Bomber", price: 329000, img: img3 },
  { id: 4, name: "Áo Bomber Cotton DirtyWave", cat: "Bomber", price: 459000, img: img4 },
  { id: 5, name: "Hoodie Zip Dáng Hộp DirtyWave", cat: "Hoodie", price: 499000, img: img5 },
  { id: 6, name: "Hoodie Zip In Hình DirtyWave", cat: "Hoodie", price: 699000, img: img6 },
  { id: 7, name: "Hoodie Zip Jacket DirtyWave", cat: "Hoodie", price: 559000, img: img7 },
  { id: 8, name: "Coach Cách Nhiệt Timberland", cat: "Coach", price: 799000, img: img8 },
  { id: 9, name: "Coach Da ASOS DirtyWave", cat: "Coach", price: 899000, img: img9 },
  { id: 10, name: "Coach Giả Da DirtyWave", cat: "Coach", price: 699000, img: img10 },
  { id: 11, name: "Coach Lông Cừu DirtyWave", cat: "Coach", price: 399000, img: img11 },
  { id: 12, name: "Bomber Dáng Lửng DirtyWave", cat: "Bomber", price: 299000, img: img2 },
  { id: 13, name: "Bomber Giả Da DirtyWave", cat: "Bomber", price: 399000, img: img3 },
  { id: 14, name: "Bomber Vải Cotton DirtyWave", cat: "Bomber", price: 299000, img: img4 },
  { id: 15, name: "Áo Thun Cotton 220gsm", cat: "Áo thun", price: 329000, img: img5 },
  { id: 16, name: "Polo Basic Dệt Mịn", cat: "Polo", price: 399000, img: img6 },
  { id: 17, name: "Jeans Slim Fit Co Giãn", cat: "Jeans", price: 649000, img: img8 },
  { id: 18, name: "Quần Kaki Form Straight", cat: "Kaki/Chino", price: 499000, img: img9 },
  { id: 19, name: "Quần Tây Minimal DirtyWave", cat: "Quần tây", price: 559000, img: img10 },
  { id: 20, name: "Quần Jogger Street", cat: "Jogger", price: 399000, img: img7 },
]

const fallbackMap = computed(() => {
  const map = new Map()
  for (const item of fallbackProducts) map.set(item.id, item)
  return map
})

const toNumber = (value) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const formatVND = (value) => new Intl.NumberFormat("vi-VN").format(toNumber(value)) + "₫"

const pickImageValue = (entry) => {
  if (!entry) return ""
  if (typeof entry === "string") return entry
  if (Array.isArray(entry)) {
    for (const child of entry) {
      const found = pickImageValue(child)
      if (found) return found
    }
    return ""
  }
  if (typeof entry === "object") {
    const keys = ["anh", "hinhAnh", "image", "imageUrl", "url", "duongDan", "duongDanAnh", "link", "path"]
    for (const key of keys) {
      const found = pickImageValue(entry[key])
      if (found) return found
    }
  }
  return ""
}

const normalizeImage = (value) => {
  const raw = String(value || "").trim()
  if (!raw) return ""
  if (/^https?:\/\//i.test(raw) || /^data:image\//i.test(raw)) return raw
  if (raw.startsWith("/")) return raw
  if (raw.startsWith("assets/") || raw.startsWith("img/") || raw.startsWith("uploads/")) return `/${raw}`
  if (raw.includes("\\")) return raw.replace(/\\/g, "/")
  return raw
}

const normalizeBackendProduct = (item) => {
  const variants = Array.isArray(item?.sanPhamChiTiets) ? item.sanPhamChiTiets : []
  const variantPrices = variants.map((v) => toNumber(v?.giaBan)).filter((v) => v > 0)
  const price = variantPrices.length ? Math.min(...variantPrices) : toNumber(item?.giaBan || item?.gia || 0)
  const imageCandidate = pickImageValue([
    item?.anh,
    item?.hinhAnh,
    item?.images,
    item?.image,
    item?.listAnh,
    item?.anhChinh,
    variants,
  ])

  return {
    id: Number(item?.id),
    name: String(item?.tenSanPham || item?.name || "Sản phẩm"),
    cat: String(item?.danhMuc?.tenDanhMuc || item?.loai?.tenLoai || "Thời trang nam"),
    price,
    img: normalizeImage(imageCandidate),
    variants: variants.map((v) => ({
      spctId: v?.id,
      color: String(v?.mauSac?.tenMau || "").trim(),
      size: String(v?.kichThuoc?.tenKichThuoc || "").trim(),
      price: Number(v?.giaBan || 0),
      stock: Number(v?.soLuong || 0),
    })),
  }
}

const backendMap = computed(() => {
  const map = new Map()
  for (const product of backendProducts.value) {
    map.set(product.id, product)
  }
  return map
})

const readCart = () => {
  try {
    const stored = JSON.parse(localStorage.getItem(CART_STORAGE_KEY) || "{}")
    cart.value = stored && typeof stored === "object" ? stored : {}
  } catch {
    cart.value = {}
  }
}

const readCartVariants = () => {
  try {
    const stored = JSON.parse(localStorage.getItem("cartVariants") || "{}")
    cartVariants.value = stored && typeof stored === "object" ? stored : {}
  } catch {
    cartVariants.value = {}
  }
}

const writeCartVariants = () => {
  localStorage.setItem("cartVariants", JSON.stringify(cartVariants.value))
}

const getColorOptions = (id) => {
  const product = backendMap.value.get(Number(id))
  if (!product?.variants?.length) return []
  return [...new Set(product.variants.map((v) => v.color).filter(Boolean))]
}

const getSizeOptionsForModal = (id, color) => {
  const product = backendMap.value.get(Number(id))
  if (!product?.variants?.length) return []
  const filtered = color ? product.variants.filter((v) => v.color === color) : product.variants
  return [...new Set(filtered.map((v) => v.size).filter(Boolean))]
}

// ── Detail Modal ──
const detailModal = ref(null)
const modalColor = ref("")
const modalSize = ref("")
const modalQty = ref(1)

const openDetailModal = (line) => {
  detailModal.value = line
  modalColor.value = line.color || ""
  modalSize.value = line.size || ""
  modalQty.value = line.quantity
  document.body.style.overflow = "hidden"
}

const closeDetailModal = () => {
  detailModal.value = null
  document.body.style.overflow = ""
}

const selectModalColor = (color) => {
  modalColor.value = color
  const sizes = getSizeOptionsForModal(detailModal.value?.id, color)
  if (sizes.length && !sizes.includes(modalSize.value)) {
    modalSize.value = sizes[0]
  }
}

const confirmDetail = () => {
  if (!detailModal.value) return
  const id = String(detailModal.value.id)

  if (modalQty.value > 0) {
    cart.value[id] = modalQty.value
    cart.value = { ...cart.value }
    writeCart()
  }

  const product = backendMap.value.get(Number(id))
  let spctId = null
  if (product?.variants?.length && (modalColor.value || modalSize.value)) {
    const matched =
      product.variants.find((v) => v.color === modalColor.value && v.size === modalSize.value) ||
      product.variants.find((v) => v.color === modalColor.value) ||
      product.variants.find((v) => v.size === modalSize.value)
    spctId = matched?.spctId || null
  }

  cartVariants.value = {
    ...cartVariants.value,
    [id]: { color: modalColor.value, size: modalSize.value, spctId },
  }
  writeCartVariants()
  closeDetailModal()
}

const notifyCartUpdated = () => {
  window.dispatchEvent(new Event(CART_UPDATED_EVENT))
}

const writeCart = () => {
  localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cart.value))
  notifyCartUpdated()
}

const resolveProduct = (id) => {
  const numericId = Number(id)
  const backendProduct = backendMap.value.get(numericId)
  const fallbackProduct = fallbackMap.value.get(numericId)
  // Prefer backend data but fall back to static image when backend has none
  if (backendProduct?.img) return backendProduct
  if (fallbackProduct) return { ...(backendProduct || {}), ...fallbackProduct, img: fallbackProduct.img }
  if (backendProduct) return backendProduct
  return { id: numericId, name: `Sản phẩm #${numericId}`, cat: "Sản phẩm", price: 0, img: img11, variants: [] }
}

const cartLines = computed(() => {
  return Object.entries(cart.value)
    .map(([id, quantity]) => {
      const qty = toNumber(quantity)
      if (qty <= 0) return null

      const product = resolveProduct(id)
      const variantChoice = cartVariants.value[String(id)] || {}
      return {
        id: Number(id),
        lineKey: `${id}__${variantChoice.color || ""}__${variantChoice.size || ""}`,
        quantity: qty,
        name: product.name,
        category: product.cat,
        image: product.img,
        price: toNumber(product.price),
        lineTotal: toNumber(product.price) * qty,
        color: variantChoice.color || "",
        size: variantChoice.size || "",
        spctId: variantChoice.spctId || null,
        variants: product.variants || [],
      }
    })
    .filter(Boolean)
})

const syncSelectedLines = () => {
  const availableKeys = cartLines.value.map((line) => line.lineKey)
  if (!availableKeys.length) {
    selectedLineKeys.value = []
    return
  }

  if (!selectedLineKeys.value.length) {
    selectedLineKeys.value = [...availableKeys]
    return
  }

  selectedLineKeys.value = selectedLineKeys.value.filter((key) => availableKeys.includes(key))
  if (!selectedLineKeys.value.length) {
    selectedLineKeys.value = [...availableKeys]
  }
}

const allSelected = computed(() => {
  if (!cartLines.value.length) return false
  return cartLines.value.every((line) => selectedLineKeys.value.includes(line.lineKey))
})

const selectedCartLines = computed(() => {
  const keys = new Set(selectedLineKeys.value)
  return cartLines.value.filter((line) => keys.has(line.lineKey))
})

const selectedItemsCount = computed(() => selectedCartLines.value.reduce((sum, line) => sum + line.quantity, 0))

const selectedSubtotal = computed(() => selectedCartLines.value.reduce((sum, line) => sum + line.lineTotal, 0))

const toggleLineSelection = (lineKey) => {
  const exists = selectedLineKeys.value.includes(lineKey)
  if (exists) {
    selectedLineKeys.value = selectedLineKeys.value.filter((key) => key !== lineKey)
    return
  }
  selectedLineKeys.value = [...selectedLineKeys.value, lineKey]
}

const toggleSelectAll = () => {
  if (allSelected.value) {
    selectedLineKeys.value = []
    return
  }
  selectedLineKeys.value = cartLines.value.map((line) => line.lineKey)
}

const subtotal = computed(() => cartLines.value.reduce((sum, line) => sum + line.lineTotal, 0))
const totalItems = computed(() => cartLines.value.reduce((sum, line) => sum + line.quantity, 0))

const resolveLineStock = (line) => {
  if (!line?.variants?.length) return Infinity

  const matchedBySpct = line.variants.find((variant) => Number(variant?.spctId) === Number(line.spctId))
  if (matchedBySpct) return Number(matchedBySpct?.stock || 0)

  const matchedByColorSize = line.variants.find((variant) => {
    return String(variant?.color || "") === String(line?.color || "")
      && String(variant?.size || "") === String(line?.size || "")
  })
  if (matchedByColorSize) return Number(matchedByColorSize?.stock || 0)

  return Infinity
}

const increase = (id) => {
  const key = String(id)
  const line = cartLines.value.find((entry) => String(entry.id) === key)
  const current = toNumber(cart.value[key])
  const next = current + 1
  const maxStock = resolveLineStock(line)

  if (Number.isFinite(maxStock) && maxStock >= 0 && next > maxStock) {
    window.toast?.warning?.("Số lượng đã đạt mức tồn kho tối đa")
    return
  }

  cart.value[key] = next
  cart.value = { ...cart.value }
  writeCart()
}

const decrease = (id) => {
  const key = String(id)
  const current = toNumber(cart.value[key])
  if (current <= 1) {
    delete cart.value[key]
  } else {
    cart.value[key] = current - 1
  }
  cart.value = { ...cart.value }
  writeCart()
}

const removeItem = (id) => {
  const key = String(id)
  if (!(key in cart.value)) return
  delete cart.value[key]
  cart.value = { ...cart.value }
  writeCart()
}

const clearCart = () => {
  cart.value = {}
  writeCart()
}

const continueShopping = () => {
  router.push("/san-pham")
}

const goProduct = (id) => {
  router.push(`/product/${id}`)
}

const beginCheckout = () => {
  if (!selectedCartLines.value.length) {
    window.toast?.warning?.("Vui lòng chọn ít nhất 1 sản phẩm để thanh toán")
    return
  }

  const outOfStockLines = selectedCartLines.value.filter((line) => {
    const maxStock = resolveLineStock(line)
    return Number.isFinite(maxStock) && maxStock >= 0 && Number(line.quantity || 0) > maxStock
  })
  if (outOfStockLines.length) {
    window.toast?.error?.("Một số sản phẩm vượt quá tồn kho, vui lòng kiểm tra lại giỏ hàng")
    return
  }

  const payload = selectedCartLines.value.map((line) => ({
    id: line.id,
    name: line.name,
    price: line.price,
    quantity: line.quantity,
    image: line.image,
    category: line.category,
    color: line.color || "",
    size: line.size || "",
    spctId: line.spctId || null,
  }))

  localStorage.setItem(CHECKOUT_CART_STORAGE_KEY, JSON.stringify(payload))
  router.push("/checkout")
}

const loadProducts = async () => {
  loadingProducts.value = true
  try {
    const response = await getAllSanPham()
    const rows = Array.isArray(response?.data) ? response.data : []
    backendProducts.value = rows.map(normalizeBackendProduct).filter((item) => Number.isFinite(item.id))
  } catch {
    backendProducts.value = []
  } finally {
    loadingProducts.value = false
  }
}

onMounted(() => {
  readCart()
  readCartVariants()
  loadProducts()
  window.addEventListener("storage", readCart)
})

watch(cartLines, syncSelectedLines, { immediate: true })

onUnmounted(() => {
  window.removeEventListener("storage", readCart)
})
</script>

<template>
  <div class="cart-page">
    <SiteNav :cart-count="totalItems" />

    <section class="cart-hero">
      <div class="cart-shell">
        <p class="cart-kicker">DirtyWave Checkout Lane</p>
        <h1>Giỏ hàng của bạn</h1>
        <p>
          Giao diện mới ưu tiên rõ ràng giá trị đơn, thao tác nhanh trên mobile và giữ nhịp mua sắm liền mạch.
        </p>
      </div>
    </section>

    <main class="cart-shell cart-main">
      <section class="cart-lines-card">
        <header class="cart-lines-head">
          <div>
            <h2>Mặt hàng đã chọn</h2>
            <p>{{ selectedItemsCount }} / {{ totalItems }} sản phẩm được chọn</p>
          </div>
          <div style="display:flex; gap:10px; align-items:center;">
            <label v-if="cartLines.length" style="display:flex; gap:6px; align-items:center; font-size:13px; color:#334155;">
              <input type="checkbox" :checked="allSelected" @change="toggleSelectAll" />
              Chọn tất cả
            </label>
            <button v-if="cartLines.length" class="ghost-btn" type="button" @click="clearCart">Xóa toàn bộ</button>
          </div>
        </header>

        <div v-if="!cartLines.length" class="cart-empty">
          <strong>Giỏ hàng hiện đang trống</strong>
          <p>Thêm vài món bomber, hoodie hoặc coach để mở ưu đãi theo mốc đơn.</p>
          <button class="primary-btn" type="button" @click="continueShopping">Khám phá sản phẩm</button>
        </div>

        <div v-else class="cart-lines-list">
          <article v-for="line in cartLines" :key="line.id" class="cart-line">
            <label style="display:flex; align-items:flex-start; padding-top:6px;">
              <input
                type="checkbox"
                :checked="selectedLineKeys.includes(line.lineKey)"
                @change="toggleLineSelection(line.lineKey)"
              />
            </label>
            <button class="line-image" type="button" @click="openDetailModal(line)">
              <img :src="line.image" :alt="line.name" />
            </button>

            <div class="line-copy">
              <p class="line-cat">{{ line.category }}</p>
              <h3 class="line-name-link" @click="goProduct(line.id)">{{ line.name }}</h3>
              <p class="line-price">{{ formatVND(line.price) }}</p>
              <div v-if="line.color || line.size" class="line-variant-tags">
                <span v-if="line.color" class="line-tag">{{ line.color }}</span>
                <span v-if="line.size" class="line-tag">Size {{ line.size }}</span>
              </div>
              <button class="line-edit-btn" type="button" @click="openDetailModal(line)">✏ Chỉnh sửa</button>
            </div>

            <div class="line-controls">
              <div class="qty-box">
                <button type="button" aria-label="Giảm" @click="decrease(line.id)">−</button>
                <strong>{{ line.quantity }}</strong>
                <button type="button" aria-label="Tăng" @click="increase(line.id)">+</button>
              </div>
              <strong class="line-total">{{ formatVND(line.lineTotal) }}</strong>
              <button class="line-remove" type="button" @click="removeItem(line.id)">Xóa</button>
            </div>
          </article>
        </div>
      </section>

      <aside class="cart-summary-card">
        <h2>Tóm tắt đơn</h2>
        <div class="summary-grid">
          <span>Số lượng</span>
          <strong>{{ selectedItemsCount }} món</strong>
          <span>Tạm tính</span>
          <strong>{{ formatVND(selectedSubtotal) }}</strong>
          <span>Vận chuyển</span>
          <strong>Tính tại checkout</strong>
        </div>

        <div class="summary-total">
          <span>Thành tiền dự kiến</span>
          <strong>{{ formatVND(selectedSubtotal) }}</strong>
        </div>

        <button class="primary-btn" type="button" :disabled="!selectedCartLines.length" @click="beginCheckout">
          Thanh toán ngay
        </button>
        <button class="secondary-btn" type="button" @click="continueShopping">Tiếp tục mua</button>

        <p class="summary-note" v-if="loadingProducts">Đang đồng bộ dữ liệu sản phẩm mới nhất...</p>
      </aside>
    </main>

    <!-- Product Detail Modal -->
    <transition name="pd-fade">
      <div v-if="detailModal" class="pd-overlay" @click.self="closeDetailModal">
        <div class="pd-card">
          <div class="pd-card-head">
            <p class="pd-cat-label">{{ detailModal.category }}</p>
            <button class="pd-close" type="button" @click="closeDetailModal">×</button>
          </div>

          <div class="pd-image-wrap">
            <img :src="detailModal.image" :alt="detailModal.name" class="pd-main-img" />
          </div>

          <div class="pd-info">
            <h3 class="pd-name">{{ detailModal.name }}</h3>
            <p class="pd-price">{{ formatVND(detailModal.price) }}</p>
          </div>

          <div v-if="getColorOptions(detailModal.id).length" class="pd-section">
            <p class="pd-label">Màu sắc</p>
            <div class="pd-option-grid">
              <button
                v-for="color in getColorOptions(detailModal.id)"
                :key="color"
                type="button"
                class="pd-opt-btn"
                :class="{ active: modalColor === color }"
                @click="selectModalColor(color)"
              >
                {{ color }}
              </button>
            </div>
          </div>

          <div v-if="getSizeOptionsForModal(detailModal.id, modalColor).length" class="pd-section">
            <p class="pd-label">Size</p>
            <div class="pd-option-grid">
              <button
                v-for="size in getSizeOptionsForModal(detailModal.id, modalColor)"
                :key="size"
                type="button"
                class="pd-opt-btn"
                :class="{ active: modalSize === size }"
                @click="modalSize = size"
              >
                {{ size }}
              </button>
            </div>
          </div>

          <div class="pd-section">
            <p class="pd-label">Số lượng</p>
            <div class="qty-box">
              <button type="button" @click="modalQty = Math.max(1, modalQty - 1)">−</button>
              <strong>{{ modalQty }}</strong>
              <button type="button" @click="modalQty++">+</button>
            </div>
          </div>

          <button class="pd-confirm-btn" type="button" @click="confirmDetail">Xác nhận</button>
        </div>
      </div>
    </transition>

    <CustomerFooter />
  </div>
</template>

<style scoped>
.cart-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at 15% 20%, rgba(255, 219, 192, 0.5) 0%, rgba(255, 219, 192, 0) 44%),
    radial-gradient(circle at 90% 8%, rgba(204, 223, 255, 0.5) 0%, rgba(204, 223, 255, 0) 46%),
    #f7f8fb;
}

.cart-shell {
  width: min(1220px, calc(100% - 32px));
  margin: 0 auto;
}

.cart-hero {
  padding: 26px 0 12px;
}

.cart-kicker {
  margin: 0;
  color: #8f1121;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.cart-hero h1 {
  margin: 8px 0 6px;
  font-size: clamp(28px, 4vw, 44px);
  line-height: 1.08;
}

.cart-hero p {
  margin: 0;
  max-width: 760px;
  color: #5f6171;
  line-height: 1.6;
}

.cart-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  padding: 14px 0 36px;
}

.cart-lines-card,
.cart-summary-card {
  border-radius: 24px;
  border: 1px solid rgba(22, 22, 28, 0.08);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 38px rgba(18, 18, 23, 0.08);
}

.cart-lines-card {
  padding: 18px;
}

.cart-lines-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.cart-lines-head h2,
.cart-summary-card h2 {
  margin: 0;
  font-size: 21px;
  color: #1b1b22;
}

.cart-lines-head p {
  margin: 4px 0 0;
  color: #727686;
  font-size: 14px;
}

.ghost-btn {
  border: 1px solid #d7dae3;
  background: #fff;
  height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  font-weight: 700;
  cursor: pointer;
}

.cart-empty {
  border: 1px dashed #d8dbe5;
  border-radius: 18px;
  padding: 28px 20px;
  text-align: center;
}

.cart-empty strong {
  font-size: 19px;
}

.cart-empty p {
  margin: 8px auto 18px;
  max-width: 440px;
  color: #6f7282;
}

.cart-lines-list {
  display: grid;
  gap: 12px;
}

.cart-line {
  border: 1px solid #e3e6ef;
  border-radius: 18px;
  background: linear-gradient(130deg, #ffffff 0%, #f9fbff 100%);
  padding: 12px;
  display: grid;
  grid-template-columns: 24px 84px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
}

.cart-line input[type="checkbox"] {
  width: 14px;
  height: 14px;
  accent-color: #0f9d58;
}

.line-image {
  width: 84px;
  height: 84px;
  border: 0;
  border-radius: 16px;
  overflow: hidden;
  padding: 0;
  background: #f3f4f8;
  cursor: pointer;
}

.line-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.line-copy h3 {
  margin: 2px 0 4px;
  font-size: 17px;
  line-height: 1.3;
  color: #18181f;
}

.line-cat {
  margin: 0;
  color: #75798b;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-weight: 700;
}

.line-price {
  margin: 0;
  color: #4d5162;
  font-weight: 600;
}

.line-controls {
  display: grid;
  gap: 8px;
  justify-items: end;
}

.qty-box {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #d7dbe5;
  border-radius: 999px;
  padding: 4px;
  background: #fff;
}

.qty-box button {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 999px;
  background: #f2f4f9;
  font-size: 18px;
  cursor: pointer;
}

.qty-box strong {
  min-width: 24px;
  text-align: center;
}

.line-total {
  color: #8f1121;
  font-size: 17px;
}

.line-remove {
  border: 0;
  background: transparent;
  color: #8a4a52;
  font-weight: 700;
  cursor: pointer;
}

.line-name-link {
  cursor: pointer;
  transition: color 0.15s;
}
.line-name-link:hover { color: #8f1121; }

.line-variant-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.line-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 9px;
  border: 1px solid #e2c4ca;
  border-radius: 999px;
  background: #fff5f6;
  color: #8f1121;
  font-size: 12px;
  font-weight: 600;
}

.line-edit-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  border: 1px solid #d7dae3;
  background: #fff;
  border-radius: 999px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 700;
  color: #374151;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}
.line-edit-btn:hover { border-color: #8f1121; color: #8f1121; }

/* ── Product Detail Modal ── */
.pd-overlay {
  position: fixed;
  inset: 0;
  z-index: 80;
  background: rgba(17, 24, 39, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.pd-card {
  width: min(420px, 100%);
  background: #fff;
  border-radius: 24px;
  padding: 20px;
  box-shadow: 0 24px 60px rgba(18, 18, 23, 0.18);
  max-height: 90vh;
  overflow-y: auto;
}

.pd-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.pd-cat-label {
  margin: 0;
  color: #8f1121;
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.07em;
}

.pd-close {
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 999px;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #374151;
  transition: background 0.15s;
}
.pd-close:hover { background: #f3f4f8; }

.pd-image-wrap {
  width: 100%;
  aspect-ratio: 4/3;
  border-radius: 16px;
  overflow: hidden;
  background: #f3f4f8;
  margin-bottom: 14px;
}

.pd-main-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pd-info {
  margin-bottom: 12px;
}

.pd-name {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
  color: #18181f;
  line-height: 1.3;
}

.pd-price {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: #8f1121;
}

.pd-section {
  margin-bottom: 14px;
}

.pd-label {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 700;
  color: #374151;
}

.pd-option-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pd-opt-btn {
  min-width: 60px;
  padding: 7px 14px;
  border: 1px solid #d7dae3;
  border-radius: 12px;
  background: #f9fafb;
  color: #374151;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, color 0.15s;
}
.pd-opt-btn:hover { border-color: #8f1121; color: #8f1121; }
.pd-opt-btn.active {
  border-color: #8f1121;
  background: #fff1f2;
  color: #8f1121;
  box-shadow: inset 0 0 0 1px #8f1121;
}

.pd-confirm-btn {
  width: 100%;
  min-height: 48px;
  margin-top: 8px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #c5162d 0%, #8f1121 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  transition: opacity 0.15s;
}
.pd-confirm-btn:hover { opacity: 0.9; }

.pd-fade-enter-active,
.pd-fade-leave-active { transition: opacity 0.2s ease; }
.pd-fade-enter-from,
.pd-fade-leave-to { opacity: 0; }

.cart-summary-card {
  padding: 18px;
  align-self: start;
  position: sticky;
  top: 112px;
}

.summary-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px 8px;
  color: #4a4f61;
}

.summary-grid strong {
  color: #191b25;
}

.summary-total {
  margin-top: 16px;
  border-radius: 14px;
  border: 1px solid #ecd3d7;
  background: linear-gradient(135deg, #fff5f6 0%, #fff 100%);
  padding: 12px;
  display: grid;
  gap: 6px;
}

.summary-total span {
  color: #6f7282;
  font-size: 13px;
}

.summary-total strong {
  font-size: 24px;
  color: #8f1121;
}

.primary-btn,
.secondary-btn {
  width: 100%;
  min-height: 46px;
  border-radius: 999px;
  border: 0;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}

.primary-btn {
  margin-top: 14px;
  background: linear-gradient(135deg, #c5162d 0%, #8f1121 100%);
  color: #fff;
}

.primary-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.secondary-btn {
  margin-top: 10px;
  border: 1px solid #dadde6;
  background: #fff;
  color: #22232d;
}

.summary-note {
  margin: 12px 0 0;
  color: #6d7180;
  font-size: 13px;
}

@media (max-width: 1024px) {
  .cart-main {
    grid-template-columns: 1fr;
  }

  .cart-summary-card {
    position: static;
  }
}

@media (max-width: 680px) {
  .cart-shell {
    width: min(1220px, calc(100% - 22px));
  }

  .cart-line {
    grid-template-columns: 24px 70px minmax(0, 1fr);
  }

  .line-image {
    width: 70px;
    height: 70px;
  }

  .line-controls {
    grid-column: 1 / -1;
    justify-items: stretch;
    grid-template-columns: 1fr auto auto;
    align-items: center;
  }

  .line-total {
    text-align: right;
  }

  .line-remove {
    text-align: right;
  }
}
</style>
