<script setup>
import { computed, onMounted, ref, watch } from "vue"
import { useRoute, useRouter } from "vue-router"
import CustomerPageLayout from "../../../components/customer/CustomerPageLayout.vue"
import { getAllSanPham } from "../../../services/sanPhamService"
import { getAllDanhMuc } from "../../../services/danhMucService"
import { resolveApiOrigin } from "../../../utils/apiOrigin"
import { getProductImageOverride } from "../../../utils/productImageOverrides"
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
import img12 from "../../../assets/img/Jackets/bomber/bomber-astronaut/bomber-astronaut-black.PNG?url"
import img13 from "../../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-black.PNG?url"
import img14 from "../../../assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-black.PNG?url"
import img15 from "../../../assets/img/Jackets/coach/coach-leopard/coach-leopard.PNG?url"
import img16 from "../../../assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-black.PNG?url"
import img17 from "../../../assets/img/Jackets/coach/coach-tiger-stripe/coach-tiger-stripe.PNG?url"
import img18 from "../../../assets/img/Jackets/hoodie/hoodie-camo/hoodie-camo-black.PNG?url"
import img19 from "../../../assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-blue.PNG?url"
import img20 from "../../../assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-black.PNG?url"

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
const currentPage = ref(1)
const pageSize = ref(12)

const fallbackImages = [img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11, img12, img13, img14, img15, img16, img17, img18, img19, img20]

const mappedFallbackByCodeNum = {
  1: img1,
  2: img2,
  3: img3,
  4: img4,
  5: img5,
  6: img6,
  7: img7,
  8: img8,
  9: img9,
  10: img10,
  11: img11,
  12: img12,
  13: img13,
  14: img14,
  15: img15,
  16: img16,
  17: img17,
  18: img18,
  19: img19,
  20: img20
}

const mappedFallbackByCode = {
  SP001: img1,   // Bomber da lộn
  SP002: img2,   // Bomber dáng lửng
  SP003: img3,   // Bomber da có túi
  SP004: img4,   // Bomber cotton nhẹ
  SP005: img5,   // Hoodie dáng hộp
  SP006: img6,   // Hoodie in hình
  SP007: img7,   // Hoodie kéo khoá
  SP008: img8,   // Coach cách nhiệt
  SP009: img9,   // Coach da trơn
  SP010: img10,  // Coach giả da
  SP011: img11,  // Coach lông cừu
  SP012: img12,  // Bomber Astronaut
  SP013: img13,  // Bomber Embroidered Fuzzy
  SP014: img14,  // Bomber Windbreaker
  SP015: img15,  // Coach Leopard
  SP016: img16,  // Coach Longsleeve
  SP017: img17,  // Coach Tiger Stripe
  SP018: img18,  // Hoodie Camo
  SP019: img19,  // Hoodie Zip Boxy
  SP020: img20,  // Hoodie Zip Silk
}

const mappedFallbackByName = [
  { keywords: ["bomber", "da", "lon"], image: img1 },
  { keywords: ["bomber", "dang", "lung"], image: img2 },
  { keywords: ["bomber", "gia", "da"], image: img3 },
  { keywords: ["bomber", "cotton"], image: img4 },
  { keywords: ["hoodie", "dang", "hop"], image: img5 },
  { keywords: ["hoodie", "in", "hinh"], image: img6 },
  { keywords: ["hoodie", "keo", "khoa"], image: img7 },
  { keywords: ["coach", "cach", "nhiet"], image: img8 },
  { keywords: ["coach", "da", "asos"], image: img9 },
  { keywords: ["coach", "gia", "da"], image: img10 },
  { keywords: ["coach", "long", "cuu"], image: img11 },
  { keywords: ["astronaut"], image: img12 },
  { keywords: ["embroidered", "fuzzy"], image: img13 },
  { keywords: ["windbreaker"], image: img14 },
  { keywords: ["leopard"], image: img15 },
  { keywords: ["longsleeve"], image: img16 },
  { keywords: ["tiger", "stripe"], image: img17 },
  { keywords: ["camo"], image: img18 },
  { keywords: ["zip", "boxy"], image: img19 },
  { keywords: ["zip", "silk"], image: img20 }
]

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

const normalizeProductKey = (value = "") =>
  String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()

const normalizeSearchText = (value = "") =>
  normalizeProductKey(value)
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim()

const stripTrailingBrandTokenForImage = (value = "") =>
  String(value || "")
    .trim()
    .replace(/[\s_-]*(dirty\s*wave|dirtywave)$/i, "")
    .trim()

const getMappedFallbackByName = (name = "") => {
  const normalized = normalizeProductKey(name)
  if (!normalized) return ""
  const found = mappedFallbackByName.find((rule) =>
    rule.keywords.every((keyword) => normalized.includes(keyword))
  )
  return found?.image || ""
}

const fallbackIndexByName = (name = "") => {
  const normalized = normalizeSearchText(stripTrailingBrandTokenForImage(name))
  if (!normalized) return -1
  let hash = 0
  for (let i = 0; i < normalized.length; i += 1) {
    hash = ((hash << 5) - hash + normalized.charCodeAt(i)) | 0
  }
  return Math.abs(hash) % fallbackImages.length
}

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

const fallbackImageFor = (id, code = "", name = "") => {
  const normalizedCode = String(code || "").trim().toUpperCase()
  const allowCuratedCodeMap = /^ATID070-\d+$/i.test(normalizedCode) || /^SP\d+$/i.test(normalizedCode)

  if (mappedFallbackByCode[normalizedCode]) {
    return mappedFallbackByCode[normalizedCode]
  }

  if (allowCuratedCodeMap) {
    const codeDigits = String(normalizedCode).replace(/\D+/g, "")
    const codeNum = Number(codeDigits)
    if (Number.isFinite(codeNum) && codeNum > 0 && mappedFallbackByCodeNum[codeNum]) {
      return mappedFallbackByCodeNum[codeNum]
    }
  }

  const mappedByName = getMappedFallbackByName(name)
  if (mappedByName) return mappedByName

  const normalizedId = Number(id)
  if (Number.isFinite(normalizedId) && normalizedId > 0) {
    if (allowCuratedCodeMap && mappedFallbackByCodeNum[normalizedId]) return mappedFallbackByCodeNum[normalizedId]
    const nameIndex = fallbackIndexByName(name)
    if (nameIndex >= 0) return fallbackImages[nameIndex] || fallbackImages[0]
    return fallbackImages[(normalizedId - 1) % fallbackImages.length]
  }

  const digits = String(code || "").replace(/\D+/g, "")
  const codeNum = Number(digits)
  if (Number.isFinite(codeNum) && codeNum > 0) {
    if (allowCuratedCodeMap && mappedFallbackByCodeNum[codeNum]) return mappedFallbackByCodeNum[codeNum]
    const nameIndex = fallbackIndexByName(name)
    if (nameIndex >= 0) return fallbackImages[nameIndex] || fallbackImages[0]
    return fallbackImages[(codeNum - 1) % fallbackImages.length]
  }

  const nameIndex = fallbackIndexByName(name)
  if (nameIndex >= 0) return fallbackImages[nameIndex] || fallbackImages[0]
  return fallbackImages[0]
}

const isActiveStatus = (value = "") => {
  const normalized = String(value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
  return !normalized.includes("ngung") && !normalized.includes("inactive")
}

const normalizeProduct = (item) => {
  const allVariants = Array.isArray(item?.sanPhamChiTiets) ? item.sanPhamChiTiets : []
  const variants = allVariants.filter((variant) => isActiveStatus(variant?.trangThai || variant?.status))
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
  const active = isActiveStatus(item?.trangThai || item?.status) && variants.length > 0

  return {
    id,
    name: String(item?.tenSanPham || item?.name || "Sản phẩm"),
    code,
    image: overrideImage || imageCandidate || fallbackImageFor(id, code, item?.tenSanPham || item?.name),
    category,
    categoryKey: normalizeText(category),
    price,
    stock,
    sold: toNumber(item?.daBan || item?.luotBan || item?.soLuongBan || 0),
    active,
  }
}

const formatVND = (value) => new Intl.NumberFormat("vi-VN").format(toNumber(value)) + "₫"

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
  let list = products.value.filter((item) => item.active)

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

const totalPages = computed(() => Math.max(1, Math.ceil(filteredProducts.value.length / pageSize.value)))

const paginatedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredProducts.value.slice(start, start + pageSize.value)
})

const goToPage = (page) => {
  currentPage.value = Math.max(1, Math.min(page, totalPages.value))
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch([search, sortBy, selectedCategories, inStockOnly, minPrice, maxPrice], () => {
  currentPage.value = 1
}, { deep: true })

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
          <span v-else>Hiển thị {{ (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, filteredProducts.length) }} / {{ filteredProducts.length }} sản phẩm</span>
        </div>

        <div v-if="errorMessage" class="cp-alert">{{ errorMessage }}</div>

        <section v-if="!loading" class="cp-grid">
          <article v-for="item in paginatedProducts" :key="item.id" class="cp-card">
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

        <nav v-if="totalPages > 1" class="cp-pagination">
          <button type="button" :disabled="currentPage <= 1" @click="goToPage(currentPage - 1)">‹</button>
          <button
            v-for="page in totalPages"
            :key="page"
            type="button"
            :class="{ active: page === currentPage }"
            @click="goToPage(page)"
          >{{ page }}</button>
          <button type="button" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">›</button>
        </nav>
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
  background-color: #fff;
}
.cp-toolbar select {
  padding-right: 34px;
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
  height: 300px;
  background: #f8f1f3;
}

.cp-card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
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

.cp-pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 6px;
}

.cp-pagination button {
  min-width: 38px;
  height: 38px;
  border: 1px solid #e8d8db;
  border-radius: 10px;
  background: #fff;
  color: #3a3437;
  font-weight: 600;
  cursor: pointer;
  transition: all .15s;
}

.cp-pagination button.active {
  background: linear-gradient(135deg, #c5162d 0%, #8f1121 100%);
  color: #fff;
  border-color: transparent;
}

.cp-pagination button:disabled {
  opacity: 0.4;
  cursor: default;
}

@media (max-width: 640px) {
  .cp-grid {
    grid-template-columns: 1fr;
  }
}
</style>
