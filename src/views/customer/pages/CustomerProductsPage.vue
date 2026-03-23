<script setup>
import { computed, onMounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import CustomerPageLayout from "../../../components/customer/CustomerPageLayout.vue"
import { getAllSanPham } from "../../../services/sanPhamService"
import { getAllDanhMuc } from "../../../services/danhMucService"
import { resolveApiOrigin } from "../../../utils/apiOrigin"
import { getProductImageOverride } from "../../../utils/productImageOverrides"
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

const route = useRoute()
const router = useRouter()
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")

const loading = ref(false)
const errorMessage = ref("")
const products = ref([])
const danhMucOptions = ref([])
const search = ref(String(route.query.q || ""))
const sortBy = ref("newest")
const selectedCategories = ref([])
const inStockOnly = ref(false)
const minPrice = ref("")
const maxPrice = ref("")

const fallbackImages = [img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11]

const sortOptions = [
  { value: "newest", label: "Mới nhất trước" },
  { value: "price-asc", label: "Giá thấp - cao" },
  { value: "price-desc", label: "Giá cao - thấp" },
  { value: "popular", label: "Bán chạy nhất" },
]

const toNumber = (value) => {
  const n = Number(value)
  return Number.isFinite(n) ? n : 0
}

const normalizeText = (value) => String(value || "").trim().toLowerCase()

const resolveProductId = (item) => {
  const id = Number(item?.id ?? item?.idSanPham ?? item?.sanPhamId ?? item?.productId)
  return Number.isFinite(id) && id > 0 ? id : 0
}

const isAbsoluteUrl = (value) => /^https?:\/\//i.test(value) || /^data:image\//i.test(value)

const isImageString = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return false
  if (isAbsoluteUrl(raw)) return true
  const normalized = raw.replace(/\\/g, "/").split(/[?#]/)[0]
  if (normalized.startsWith("/uploads/") || normalized.startsWith("uploads/")) return true
  return /\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i.test(normalized)
}

const toImageUrl = (value) => {
  if (!value) return ""
  const raw = String(value).trim()
  if (!raw) return ""

  if (isAbsoluteUrl(raw)) return raw

  const normalizedSlash = raw.replace(/\\/g, "/")
  const uploadsMatch = normalizedSlash.match(/(?:^|\/)(uploads\/.*)$/i)
  if (uploadsMatch?.[1]) return `${BACKEND_ORIGIN}/${uploadsMatch[1]}`

  if (normalizedSlash.startsWith("/uploads/")) return `${BACKEND_ORIGIN}${normalizedSlash}`
  if (normalizedSlash.startsWith("uploads/")) return `${BACKEND_ORIGIN}/${normalizedSlash}`
  if (normalizedSlash.startsWith("assets/") || normalizedSlash.startsWith("img/")) return `/${normalizedSlash}`
  if (normalizedSlash.startsWith("/")) return normalizedSlash

  return normalizedSlash
}

const pickImageValue = (entry) => {
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
    const keys = [
      "anh",
      "hinhAnh",
      "image",
      "imageUrl",
      "url",
      "duongDan",
      "duongDanAnh",
      "link",
      "path",
    ]
    for (const key of keys) {
      const found = pickImageValue(entry[key])
      if (found) return found
    }
  }
  return ""
}

const fallbackImageFor = (id) => {
  const normalizedId = Number(id)
  const safeIndex = Number.isFinite(normalizedId) && normalizedId > 0
    ? (normalizedId - 1) % fallbackImages.length
    : 0
  return fallbackImages[safeIndex]
}

const normalizeProduct = (item) => {
  const variants = Array.isArray(item?.sanPhamChiTiets) ? item.sanPhamChiTiets : []
  const variantPrices = variants.map((v) => toNumber(v?.giaBan)).filter((v) => v > 0)

  let price = toNumber(item?.giaBan || item?.gia || 0)
  if (variantPrices.length) {
    price = Math.min(...variantPrices)
  }

  const stock = variants.length
    ? variants.reduce((sum, v) => sum + toNumber(v?.soLuong), 0)
    : toNumber(item?.soLuong || 0)

  const imageCandidate = pickImageValue([
    item?.anh,
    item?.hinhAnh,
    item?.images,
    item?.image,
    item?.listAnh,
    item?.anhChinh,
    variants,
  ])

  const id = resolveProductId(item)
  const code = String(item?.maSanPham || item?.ma || "")
  const category = String(item?.danhMuc?.tenDanhMuc || item?.loai?.tenLoai || "Thời trang nam")
  const overrideImage = getProductImageOverride({ id, maSanPham: code })[0]

  return {
    id,
    name: String(item?.tenSanPham || item?.name || "Sản phẩm"),
    code,
    image: overrideImage || imageCandidate || fallbackImageFor(id),
    category,
    categoryKey: normalizeText(category),
    price,
    stock,
    sold: toNumber(item?.daBan || item?.luotBan || item?.soLuongBan || 0),
  }
}

const formatVND = (value) => new Intl.NumberFormat("vi-VN").format(toNumber(value)) + "đ"

const categoryOptions = computed(() => {
  const set = new Set()

  for (const item of danhMucOptions.value) {
    const name = String(item || '').trim()
    if (name) set.add(name)
  }

  for (const item of products.value) {
    if (item.category) set.add(item.category)
  }
  return [...set]
})

const priceBounds = computed(() => {
  if (!products.value.length) {
    return { min: 0, max: 0 }
  }
  const prices = products.value.map((item) => toNumber(item.price)).filter((v) => v > 0)
  if (!prices.length) {
    return { min: 0, max: 0 }
  }
  return { min: Math.min(...prices), max: Math.max(...prices) }
})

const minPriceValue = computed(() => {
  const val = toNumber(minPrice.value)
  return val > 0 ? val : 0
})

const maxPriceValue = computed(() => {
  const val = toNumber(maxPrice.value)
  return val > 0 ? val : 0
})

const filteredProducts = computed(() => {
  const keyword = normalizeText(search.value)
  let list = [...products.value]

  if (keyword) {
    list = list.filter((item) => {
      const name = normalizeText(item.name)
      const code = normalizeText(item.code)
      const category = item.categoryKey
      return name.includes(keyword) || code.includes(keyword) || category.includes(keyword)
    })
  }

  if (selectedCategories.value.length) {
    const chosen = new Set(selectedCategories.value.map(normalizeText))
    list = list.filter((item) => chosen.has(item.categoryKey))
  }

  if (inStockOnly.value) {
    list = list.filter((item) => toNumber(item.stock) > 0)
  }

  if (minPriceValue.value > 0) {
    list = list.filter((item) => toNumber(item.price) >= minPriceValue.value)
  }

  if (maxPriceValue.value > 0) {
    list = list.filter((item) => toNumber(item.price) <= maxPriceValue.value)
  }

  if (sortBy.value === "price-asc") list.sort((a, b) => a.price - b.price)
  if (sortBy.value === "price-desc") list.sort((a, b) => b.price - a.price)
  if (sortBy.value === "popular") list.sort((a, b) => b.sold - a.sold || b.id - a.id)
  if (sortBy.value === "newest") list.sort((a, b) => b.id - a.id)

  return list
})

const toggleCategory = (category) => {
  const key = normalizeText(category)
  const next = selectedCategories.value.map(normalizeText)
  const index = next.indexOf(key)
  if (index >= 0) {
    selectedCategories.value = selectedCategories.value.filter((item) => normalizeText(item) !== key)
    return
  }
  selectedCategories.value = [...selectedCategories.value, category]
}

const resetFilters = () => {
  search.value = ""
  sortBy.value = "newest"
  selectedCategories.value = []
  inStockOnly.value = false
  minPrice.value = ""
  maxPrice.value = ""
}

const loadProducts = async () => {
  loading.value = true
  errorMessage.value = ""

  try {
    const [productRes, danhMucRes] = await Promise.all([
      getAllSanPham(),
      getAllDanhMuc().catch(() => ({ data: [] }))
    ])

    const source = Array.isArray(productRes?.data) ? productRes.data : []
    const danhMucRaw = Array.isArray(danhMucRes?.data) ? danhMucRes.data : []

    danhMucOptions.value = danhMucRaw
      .map((item) => String(item?.tenDanhMuc || item?.name || '').trim())
      .filter(Boolean)

    products.value = source.map(normalizeProduct)
  } catch {
    products.value = []
    danhMucOptions.value = []
    errorMessage.value = "Không thể tải danh sách sản phẩm."
  } finally {
    loading.value = false
  }
}

const openProductDetail = (id) => {
  if (!Number.isFinite(Number(id)) || Number(id) <= 0) {
    window.toast?.warning("Không xác định được sản phẩm để mở chi tiết")
    return
  }
  router.push(`/product/${id}`)
}

watch(
  () => route.query.q,
  (value) => {
    search.value = String(value || "")
  }
)

watch(search, (value) => {
  const q = String(value || "").trim()
  const query = { ...route.query }

  if (!q) {
    if (query.q) {
      delete query.q
      router.replace({ path: "/san-pham", query })
    }
    return
  }

  if (q !== String(route.query.q || "")) {
    router.replace({ path: "/san-pham", query: { ...query, q } })
  }
})

onMounted(loadProducts)
</script>

<template>
  <CustomerPageLayout breadcrumb="Sản phẩm" max-width="1320px">
    <div class="cp-shell">
      <aside class="cp-sidebar">
        <section class="cp-side-block">
          <h3>Bộ lọc & Sắp xếp</h3>
          <small>Áp dụng ngay khi thay đổi.</small>
        </section>

        <section class="cp-side-block">
          <h4>Sắp xếp theo</h4>
          <div class="cp-side-list">
            <button
              v-for="option in sortOptions"
              :key="option.value"
              type="button"
              class="cp-side-item"
              :class="{ 'is-active': sortBy === option.value }"
              @click="sortBy = option.value"
            >
              <span>{{ option.label }}</span>
              <span v-if="sortBy === option.value">✓</span>
            </button>
          </div>
        </section>

        <section class="cp-side-block">
          <h4>Danh mục</h4>
          <div class="cp-side-checks">
            <label v-for="category in categoryOptions" :key="category" class="cp-check-row">
              <input
                type="checkbox"
                :checked="selectedCategories.some((item) => normalizeText(item) === normalizeText(category))"
                @change="toggleCategory(category)"
              />
              <span>{{ category }}</span>
            </label>
          </div>
        </section>

        <section class="cp-side-block">
          <h4>Khoảng giá</h4>
          <div class="cp-price-range">
            <label>
              Từ
              <input v-model="minPrice" type="number" min="0" placeholder="0" />
            </label>
            <label>
              Đến
              <input v-model="maxPrice" type="number" min="0" :placeholder="String(priceBounds.max || 0)" />
            </label>
          </div>
        </section>

        <section class="cp-side-block">
          <label class="cp-check-row">
            <input v-model="inStockOnly" type="checkbox" />
            <span>Chỉ hiển thị còn hàng</span>
          </label>
        </section>

        <button class="cp-reset-btn" type="button" @click="resetFilters">Xóa bộ lọc</button>
      </aside>

      <section class="cp-content">
        <header class="cp-header">
        <h1>Sản phẩm</h1>
        <p>Tìm nhanh sản phẩm theo tên hoặc mã, lấy dữ liệu trực tiếp từ hệ thống hiện tại.</p>
        </header>

        <section class="cp-toolbar">
          <input
            v-model="search"
            type="text"
            placeholder="Tìm sản phẩm..."
            aria-label="Tìm sản phẩm"
          />

          <select v-model="sortBy" aria-label="Sắp xếp sản phẩm">
            <option v-for="option in sortOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </section>

        <div class="cp-meta">
          <span v-if="loading">Đang tải sản phẩm...</span>
          <span v-else>Hiển thị {{ filteredProducts.length }} / {{ products.length }} sản phẩm</span>
        </div>

        <div v-if="errorMessage" class="cp-alert">{{ errorMessage }}</div>

        <section v-if="!loading" class="cp-grid">
          <article v-for="item in filteredProducts" :key="item.id" class="cp-card">
            <div class="cp-card-image">
              <img v-if="item.image" :src="item.image" :alt="item.name" />
              <div v-else class="cp-image-fallback">DirtyWave</div>
            </div>

            <div class="cp-card-body">
              <p class="cp-category">{{ item.category }}</p>
              <h2>{{ item.name }}</h2>
              <p class="cp-code">{{ item.code || `SP-${item.id}` }}</p>
              <div class="cp-row">
                <strong>{{ formatVND(item.price) }}</strong>
                <span>Còn {{ item.stock }}</span>
              </div>
            </div>

            <button class="cp-btn" type="button" @click="openProductDetail(item.id)">Xem chi tiết</button>
          </article>

          <article v-if="filteredProducts.length === 0" class="cp-empty">
            <h3>Không có sản phẩm phù hợp</h3>
            <p>Hãy thử nới điều kiện lọc hoặc xóa bộ lọc hiện tại.</p>
            <button class="cp-btn cp-btn-outline" type="button" @click="resetFilters">Xóa bộ lọc</button>
          </article>
        </section>
      </section>
    </div>
  </CustomerPageLayout>
</template>

<style scoped>
.cp-shell {
  display: grid;
  grid-template-columns: 270px minmax(0, 1fr);
  gap: 20px;
}

.cp-sidebar {
  align-self: start;
  position: sticky;
  top: 88px;
  display: grid;
  gap: 12px;
}

.cp-side-block {
  background: #fff;
  border: 1px solid #e8d8db;
  border-radius: 12px;
  padding: 14px;
}

.cp-side-block h3,
.cp-side-block h4 {
  margin: 0;
  color: #161214;
}

.cp-side-block small {
  margin-top: 6px;
  display: block;
  color: #6f6a6d;
}

.cp-side-list {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.cp-side-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #ecdde0;
  border-radius: 10px;
  background: #fff;
  color: #3a3437;
  padding: 9px 11px;
  cursor: pointer;
}

.cp-side-item.is-active {
  border-color: #c5162d;
  color: #8f1121;
  background: #fff4f6;
}

.cp-side-checks {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.cp-check-row {
  display: flex;
  gap: 10px;
  align-items: center;
  color: #3a3437;
  font-size: 14px;
}

.cp-price-range {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.cp-price-range label {
  display: grid;
  gap: 5px;
  color: #6f6a6d;
  font-size: 13px;
}

.cp-price-range input {
  height: 38px;
  border: 1px solid #e8d8db;
  border-radius: 10px;
  padding: 0 10px;
}

.cp-reset-btn {
  height: 40px;
  border: 1px solid #c5162d;
  border-radius: 10px;
  background: #fff;
  color: #8f1121;
  font-weight: 700;
  cursor: pointer;
}

.cp-content {
  min-width: 0;
}

.cp-header h1 {
  margin: 0;
  color: #161214;
  font-size: 30px;
}

.cp-header p {
  margin: 8px 0 0;
  color: #6f6a6d;
}

.cp-toolbar {
  margin-top: 18px;
  display: grid;
  grid-template-columns: 1fr 220px;
  gap: 12px;
}

.cp-toolbar input,
.cp-toolbar select {
  height: 44px;
  border: 1px solid #e8d8db;
  border-radius: 12px;
  padding: 0 14px;
  font: inherit;
  background: #fff;
}

.cp-meta {
  margin-top: 12px;
  color: #6f6a6d;
  font-size: 14px;
}

.cp-alert {
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid #f3b9c2;
  border-radius: 10px;
  background: #fdecef;
  color: #8f1121;
}

.cp-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.cp-card {
  display: flex;
  flex-direction: column;
  border: 1px solid #e8d8db;
  border-radius: 16px;
  background: #fff;
  overflow: hidden;
}

.cp-card-image {
  height: 210px;
  background: #f8f1f3;
}

.cp-card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cp-image-fallback {
  height: 100%;
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #8f1121;
  letter-spacing: 0.06em;
}

.cp-card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 12px;
}

.cp-category {
  margin: 0;
  font-size: 12px;
  color: #8f1121;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.cp-card h2 {
  margin: 6px 0 4px;
  font-size: 16px;
  line-height: 1.35;
  min-height: calc(1.35em * 2);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  color: #161214;
}

.cp-code {
  margin: 0;
  color: #6f6a6d;
  font-size: 13px;
}

.cp-row {
  margin-top: auto;
  padding-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cp-row strong {
  color: #c5162d;
}

.cp-row span {
  color: #6f6a6d;
  font-size: 13px;
}

.cp-btn {
  margin: 0 12px 12px;
  height: 40px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #c5162d 0%, #8f1121 100%);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.cp-empty {
  grid-column: 1 / -1;
  border: 1px dashed #e8d8db;
  border-radius: 16px;
  background: #fff;
  padding: 24px;
  text-align: center;
}

.cp-empty h3 {
  margin: 0;
  color: #161214;
}

.cp-empty p {
  margin: 8px 0 16px;
  color: #6f6a6d;
}

.cp-btn-outline {
  width: 180px;
  margin: 0 auto;
  background: #fff;
  border: 1px solid #c5162d;
  color: #8f1121;
}

@media (max-width: 1100px) {
  .cp-shell {
    grid-template-columns: 1fr;
  }

  .cp-sidebar {
    position: static;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .cp-reset-btn {
    grid-column: 1 / -1;
  }

  .cp-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .cp-sidebar {
    grid-template-columns: 1fr;
  }

  .cp-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .cp-toolbar {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .cp-grid {
    grid-template-columns: 1fr;
  }
}
</style>
