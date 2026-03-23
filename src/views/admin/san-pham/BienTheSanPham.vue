<script setup>
import { computed, onMounted, ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import { getAllSanPham } from "../../../services/sanPhamService"
import { resolveApiOrigin } from "../../../utils/apiOrigin"
import { getProductImageConfig } from "../../../utils/productImageOverrides"
import { getAdminStatusTone, normalizeAdminStatusLabel } from "../../../utils/adminStatus"
import logo from "../../../assets/img/logo/new logo.png?url"
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
const route = useRoute()
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")
const routeBase = computed(() => (route.path.startsWith("/employee/") ? "/employee" : "/admin"))
const fallbackImages = [img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11]

// ── State ───────────────────────────────────────────────────────────
const allVariants = ref([])
const loading = ref(false)

// Filters
const searchQuery = ref("")
const filterStatus = ref("")
const filterColorId = ref("")
const filterSizeId = ref("")
const priceMin = ref("")
const priceMax = ref("")

// Derived filter options
const colorOptions = computed(() => {
  const map = {}
  for (const v of allVariants.value) {
    if (v.mauSacId) map[v.mauSacId] = { id: v.mauSacId, name: v.mauSac, hex: v.mauSacHex }
  }
  return Object.values(map)
})

const sizeOptions = computed(() => {
  const map = {}
  for (const v of allVariants.value) {
    if (v.kichThuocId) map[v.kichThuocId] = { id: v.kichThuocId, name: v.kichThuoc }
  }
  return Object.values(map)
})

// ── Computed filtered list ─────────────────────────────────────────
const filteredVariants = computed(() => {
  let list = allVariants.value

  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(
      (v) =>
        v.tenSanPham.toLowerCase().includes(q) ||
        v.maSanPham.toLowerCase().includes(q) ||
        (v.ma || "").toLowerCase().includes(q)
    )
  }

  if (filterStatus.value) list = list.filter((v) => v.trangThai === filterStatus.value)
  if (filterColorId.value) list = list.filter((v) => String(v.mauSacId || "") === filterColorId.value)
  if (filterSizeId.value) list = list.filter((v) => String(v.kichThuocId || "") === filterSizeId.value)
  if (priceMin.value !== "") list = list.filter((v) => v.giaBan >= Number(priceMin.value))
  if (priceMax.value !== "") list = list.filter((v) => v.giaBan <= Number(priceMax.value))

  return list
})

// ── Utils ────────────────────────────────────────────────────────────
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
  return normalized
}

function colorHexByName(name = "") {
  if (/^#([0-9a-f]{3}|[0-9a-f]{6})$/i.test(String(name || "").trim())) return String(name).trim()
  const n = String(name || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .toLowerCase()
  if (n.includes("black") || n.includes("den")) return "#111827"
  if (n.includes("trang") || n.includes("kem")) return "#e5dfd0"
  if (n.includes("gray") || n.includes("grey") || n.includes("xam") || n.includes("ghi")) return "#8c95a3"
  if (n.includes("tim") || n.includes("purple")) return "#7c3aed"
  if (n.includes("xanh") && n.includes("reu")) return "#4f6f52"
  if (n.includes("navy")) return "#1e3a8a"
  if (n.includes("xanh")) return "#2f4f75"
  if (n.includes("red") || n.includes("do") || n.includes("ruou")) return "#dc2626"
  if (n.includes("yellow") || n.includes("vang")) return "#d4a017"
  if (n.includes("nau")) return "#7a4b2f"
  if (n.includes("pink") || n.includes("hong")) return "#d684a1"
  return "#9ca3af"
}

function fallbackImageFor(id, code = "") {
  const numericId = Number(id)
  if (Number.isFinite(numericId) && numericId > 0) {
    return fallbackImages[(numericId - 1) % fallbackImages.length]
  }

  const digits = String(code || "").replace(/\D+/g, "")
  const codeNum = Number(digits)
  if (Number.isFinite(codeNum) && codeNum > 0) {
    return fallbackImages[(codeNum - 1) % fallbackImages.length]
  }

  return fallbackImages[0] || logo
}

function pickProductImage(product, colorId = null) {
  const imageConfig = getProductImageConfig({ id: product.id, maSanPham: product.maSanPham })
  const colorImage = imageConfig.colorImages.find((entry) => Number(entry.colorId) === Number(colorId))
  if (colorImage?.image) return toImageUrl(colorImage.image)
  if (imageConfig.images?.length) return toImageUrl(imageConfig.images[0])
  const direct =
    product.anh ||
    product.anhChinh ||
    product.hinhAnh ||
    (Array.isArray(product.listAnh) ? product.listAnh[0] : "") ||
    (Array.isArray(product.images) ? product.images[0] : "") ||
    ""
  return direct ? toImageUrl(direct) : fallbackImageFor(product.id, product.maSanPham)
}

const formatGia = (n) => Number(n || 0).toLocaleString("vi-VN") + "đ"

function clearFilters() {
  searchQuery.value = ""
  filterStatus.value = ""
  filterColorId.value = ""
  filterSizeId.value = ""
  priceMin.value = ""
  priceMax.value = ""
}

// ── Load data ────────────────────────────────────────────────────────
async function loadData() {
  loading.value = true
  try {
    const res = await getAllSanPham()
    const products = Array.isArray(res?.data) ? res.data : []
    const rows = []
    for (const p of products) {
      const variants = Array.isArray(p.sanPhamChiTiets) ? p.sanPhamChiTiets : []
      for (const v of variants) {
        const mauTen = v.mauSac?.tenMau || v.mauSac?.tenMauSac || v.mauSac?.name || "—"
        rows.push({
          productId: p.id,
          maSanPham: p.maSanPham || "",
          tenSanPham: p.tenSanPham || "",
          image: pickProductImage(p, v.mauSac?.id || null),
          id: v.id,
          ma: v.ma || "",
          mauSac: mauTen,
          mauSacId: v.mauSac?.id || null,
          mauSacHex: colorHexByName(v.mauSac?.hex || mauTen || v.mauSac?.maMau || v.mauSac?.ma || ""),
          kichThuoc: v.kichThuoc?.tenKichThuoc || v.kichThuoc?.name || "—",
          kichThuocId: v.kichThuoc?.id || null,
          loai: v.loai?.tenLoai || v.loai?.name || "—",
          giaBan: Number(v.giaBan || 0),
          giaNhap: Number(v.giaNhap || 0),
          soLuong: Number(v.soLuong || 0),
          trangThai: normalizeAdminStatusLabel(v.trangThai || "Hoạt động"),
          statusTone: getAdminStatusTone(v.trangThai || "Hoạt động")
        })
      }
    }
    allVariants.value = rows
  } catch (err) {
    console.error("Lỗi tải biến thể:", err)
    window.toast?.error("Không tải được dữ liệu biến thể")
  } finally {
    loading.value = false
  }
}

function goEditProduct(productId) {
  router.push({
    path: `${routeBase.value}/san-pham/form/${productId}`,
    query: { from: "bien-the" }
  })
}

function goCreateProduct() {
  router.push({
    path: `${routeBase.value}/san-pham/form`,
    query: { from: "bien-the" }
  })
}

onMounted(loadData)
</script>

<template>
  <div class="card bts-page">
    <!-- ── Header ──────────────────────────────────────────────────── -->
    <div class="head">
      <div>
        <h1>Biến thể sản phẩm</h1>
        <small class="muted">{{ filteredVariants.length }} / {{ allVariants.length }} biến thể</small>
      </div>
      <div class="header-actions">
        <button class="btn with-icon" @click="loadData" :disabled="loading">
          <span class="material-icons-outlined btn-icon">autorenew</span>
          {{ loading ? "Đang tải..." : "Làm mới" }}
        </button>
        <button class="btn primary with-icon" @click="goCreateProduct">
          <span class="material-icons-outlined btn-icon">add</span>
          Thêm sản phẩm
        </button>
      </div>
    </div>

    <!-- ── Filter bar ──────────────────────────────────────────────── -->
    <div class="body">
      <div class="filter-bar">
        <!-- Search -->
        <div class="filter-field search-field">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Tìm tên SP, mã SP, mã biến thể..."
            class="filter-input"
          />
        </div>

        <!-- Status -->
        <div class="filter-field">
          <select v-model="filterStatus" class="filter-input">
            <option value="">Tất cả trạng thái</option>
            <option value="Hoạt động">Hoạt động</option>
            <option value="Ngừng hoạt động">Ngừng hoạt động</option>
          </select>
        </div>

        <!-- Size -->
        <div class="filter-field">
          <select v-model="filterSizeId" class="filter-input">
            <option value="">Tất cả kích cỡ</option>
            <option v-for="s in sizeOptions" :key="s.id" :value="String(s.id)">{{ s.name }}</option>
          </select>
        </div>

        <!-- Color -->
        <div class="filter-field">
          <select v-model="filterColorId" class="filter-input">
            <option value="">Tất cả màu sắc</option>
            <option v-for="c in colorOptions" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
          </select>
        </div>

        <!-- Price range -->
        <div class="filter-field price-range">
          <input
            v-model.number="priceMin"
            type="number"
            placeholder="Giá từ"
            min="0"
            class="filter-input price-input"
          />
          <span class="price-sep">—</span>
          <input
            v-model.number="priceMax"
            type="number"
            placeholder="Đến"
            min="0"
            class="filter-input price-input"
          />
        </div>

        <!-- Clear -->
        <button class="btn btn-clear" @click="clearFilters">Xóa lọc</button>
      </div>

      <!-- Color chip quick-filter -->
      <div class="color-filter-chips" v-if="colorOptions.length">
        <span class="chip-label">Màu:</span>
        <button
          v-for="c in colorOptions"
          :key="c.id"
          type="button"
          class="color-filter-chip"
          :class="{ active: filterColorId === String(c.id) }"
          @click="filterColorId = filterColorId === String(c.id) ? '' : String(c.id)"
        >
          <span class="cdot" :style="{ background: c.hex }"></span>
          {{ c.name }}
        </button>
      </div>

      <!-- ── Table ───────────────────────────────────────────────────── -->
      <div class="table-wrap">
        <div v-if="loading" class="loading-state">Đang tải dữ liệu...</div>

        <template v-else>
          <table class="bts-table" v-if="filteredVariants.length">
            <thead>
              <tr>
                <th class="col-stt">STT</th>
                <th class="col-img">Ảnh</th>
                <th>Tên sản phẩm</th>
                <th>Mã SP</th>
                <th>Mã biến thể</th>
                <th>Màu</th>
                <th>Kích cỡ</th>
                <th>Loại</th>
                <th class="col-num">Tồn kho</th>
                <th class="col-num">Giá bán</th>
                <th>Trạng thái</th>
                <th class="col-act">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(v, idx) in filteredVariants" :key="`${v.id}-${idx}`">
                <td class="col-stt muted">{{ idx + 1 }}</td>
                <td class="col-img">
                  <div class="product-thumb">
                    <img v-if="v.image" :src="v.image" :alt="v.tenSanPham" />
                    <span v-else class="thumb-placeholder">?</span>
                  </div>
                </td>
                <td>
                  <button class="link-btn" @click="goEditProduct(v.productId)">
                    {{ v.tenSanPham }}
                  </button>
                </td>
                <td>
                  <span class="code-badge">{{ v.maSanPham }}</span>
                </td>
                <td>
                  <span class="code-badge secondary">{{ v.ma || "—" }}</span>
                </td>
                <td>
                  <div class="color-cell">
                    <span class="cdot" :style="{ background: v.mauSacHex }"></span>
                    {{ v.mauSac }}
                  </div>
                </td>
                <td>
                  <span class="size-pill">{{ v.kichThuoc }}</span>
                </td>
                <td class="muted">{{ v.loai }}</td>
                <td class="col-num">
                  <span :class="['stock-badge', v.soLuong === 0 ? 'out' : v.soLuong < 5 ? 'low' : 'ok']">
                    {{ v.soLuong }}
                  </span>
                </td>
                <td class="col-num price">{{ formatGia(v.giaBan) }}</td>
                <td>
                  <span class="status-pill" :class="`tone-${v.statusTone}`">
                    {{ v.trangThai }}
                  </span>
                </td>
                <td class="col-act">
                  <button class="btn btn-small btn-edit" @click="goEditProduct(v.productId)">
                    Sửa SP
                  </button>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-else class="empty-state">
            <p>Không có biến thể nào phù hợp với bộ lọc.</p>
            <button class="btn" @click="clearFilters">Xóa bộ lọc</button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── Layout ─────────────────────────────────────────────────────────── */
.bts-page .head {
  flex-wrap: wrap;
  gap: 12px;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.with-icon {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-icon {
  font-size: 16px;
  line-height: 1;
  color: currentColor;
}

/* ── Filter bar ──────────────────────────────────────────────────────── */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  padding: 14px 0 10px;
}

.filter-field {
  display: flex;
  align-items: center;
  gap: 4px;
}

.search-field {
  flex: 1 1 220px;
}

.filter-input {
  padding: 9px 12px;
  border: 1px solid #d8dee9;
  border-radius: 10px;
  font-size: 14px;
  background: #fff;
  width: 100%;
  box-sizing: border-box;
}

.filter-input:focus {
  outline: none;
  border-color: #c5162d;
}

.price-range {
  gap: 6px;
}

.price-input {
  width: 110px !important;
}

.price-sep {
  color: #94a3b8;
  font-size: 12px;
}

.btn-clear {
  white-space: nowrap;
}

/* ── Color chip filter ────────────────────────────────────────────────── */
.color-filter-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.chip-label {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  margin-right: 2px;
}

.color-filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
  color: #374151;
  transition: border-color 0.15s, background 0.15s;
}

.color-filter-chip:hover {
  border-color: #c5162d;
}

.color-filter-chip.active {
  border-color: #c5162d;
  background: #fff1f2;
  color: #c5162d;
  font-weight: 600;
}

/* ── Table ───────────────────────────────────────────────────────────── */
.table-wrap {
  overflow-x: auto;
  margin-top: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
}

.bts-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 860px;
}

.bts-table th {
  padding: 10px 12px;
  background: #f8fafc;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.bts-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 14px;
  vertical-align: middle;
}

.bts-table tbody tr:last-child td {
  border-bottom: none;
}

.bts-table tbody tr:hover td {
  background: #fafafa;
}

.col-stt,
.col-num {
  text-align: center;
  width: 60px;
}

.col-img {
  width: 60px;
}

.col-act {
  width: 80px;
  text-align: center;
}

/* ── Cells ────────────────────────────────────────────────────────────── */
.product-thumb {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  overflow: hidden;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-placeholder {
  color: #94a3b8;
  font-size: 18px;
}

.link-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #0f172a;
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  padding: 0;
  text-decoration: underline;
  text-decoration-color: transparent;
  transition: color 0.15s, text-decoration-color 0.15s;
}

.link-btn:hover {
  color: #c5162d;
  text-decoration-color: #c5162d;
}

.code-badge {
  display: inline-block;
  padding: 2px 8px;
  background: #f1f5f9;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.code-badge.secondary {
  background: #f8fafc;
  color: #334155;
  border-color: #cbd5e1;
}

.color-cell {
  display: flex;
  align-items: center;
  gap: 7px;
}

.cdot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 1px solid rgba(15, 23, 42, 0.15);
}

.size-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.price {
  font-weight: 600;
  color: #c5162d;
}

/* Stock badge */
.stock-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.stock-badge.ok {
  background: #f1f5f9;
  color: #334155;
}

.stock-badge.low {
  background: #e2e8f0;
  color: #334155;
}

.stock-badge.out {
  background: #fee2e2;
  color: #dc2626;
}

/* Status pill */
.status-pill {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.status-pill.tone-success {
  background: #f1f5f9;
  color: #334155;
}

.status-pill.tone-danger {
  background: #fee2e2;
  color: #dc2626;
}

.status-pill.tone-warning {
  background: #f1f5f9;
  color: #334155;
}

.status-pill.tone-info {
  background: #f1f5f9;
  color: #334155;
}

.status-pill.tone-muted,
.status-pill.tone-default {
  background: #f1f5f9;
  color: #64748b;
}

/* Action button */
.btn-edit {
  border-color: #cbd5e1;
  color: #334155;
  background: #ffffff;
}

.btn-edit:hover {
  border-color: #c5162d;
  color: #c5162d;
  background: #fff1f2;
}

.btn-small {
  padding: 6px 12px;
  font-size: 13px;
}

/* ── Empty / loading ──────────────────────────────────────────────────── */
.loading-state {
  text-align: center;
  padding: 40px;
  color: #64748b;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #64748b;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.muted {
  color: #64748b;
}
</style>
