<script setup>
import { ref, computed, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  getAllSanPham,
  deleteSanPham,
  updateSanPham
} from "../../../services/sanPhamService"
import { Pencil, Trash2 } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel } from "../../../utils/adminStatus"
import { resolveApiOrigin } from "../../../utils/apiOrigin"
import { getProductImageOverride } from "../../../utils/productImageOverrides"
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

const search = ref("")
const list = ref([])
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")
const fallbackImages = [img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11]

const fallbackImageFor = (id, code = "") => {
  const normalizedId = Number(id)
  if (Number.isFinite(normalizedId) && normalizedId > 0) {
    return fallbackImages[(normalizedId - 1) % fallbackImages.length]
  }

  const digits = String(code || "").replace(/\D+/g, "")
  const codeNum = Number(digits)
  if (Number.isFinite(codeNum) && codeNum > 0) {
    return fallbackImages[(codeNum - 1) % fallbackImages.length]
  }

  return fallbackImages[0] || logo
}

const isImageString = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return false
  if (/^data:image\//i.test(raw)) return true

  const normalized = raw.replace(/\\/g, "/").split(/[?#]/)[0]
  if (/\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i.test(normalized)) return true
  if (normalized.startsWith("/uploads/") || normalized.startsWith("uploads/")) return true
  return /^https?:\/\//i.test(raw)
}

const toImageUrl = (value = "") => {
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

const pickImageValue = (entry) => {
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

const routeBase = computed(() => {
  if (route.path.startsWith("/employee/")) return "/employee"
  return "/admin"
})

function goToCreate() {
  router.push(`${routeBase.value}/san-pham/form`)
}

function goToEdit(id) {
  router.push(`${routeBase.value}/san-pham/form/${id}`)
}

function formatCurrency(value) {
  return new Intl.NumberFormat("vi-VN").format(value || 0) + "₫"
}

function formatDateTime(value) {
  if (!value) return "-"
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString("vi-VN")
}

async function loadData() {
  const res = await getAllSanPham()

  console.log(res.data)

  list.value = res.data.map(item => {
    const variants = item.sanPhamChiTiets || []
    const hasVariants = variants.length > 0

const totalTon =
  hasVariants
    ? variants.reduce((sum, v) => sum + (v.soLuong || 0), 0)
    : item.soLuong ?? 0
    const firstVariant = variants.length > 0 ? variants[0] : null

    const loaiName =
      firstVariant?.loai?.tenLoai ||
      item.loai ||
      "Khác"

    const normalizedGia =
      firstVariant?.giaBan ??
      item.giaBan ??
      item.gia ??
      null

    const normalizedTon =
      hasVariants
        ? (totalTon ?? 0)
        : (Number.isFinite(Number(item.soLuong ?? item.ton)) ? Number(item.soLuong ?? item.ton) : null)

    return {
      id: item.id,
      raw: item,
      ma: item.maSanPham,
      name: item.tenSanPham,
      description: item.moTa,
      image: getProductImageOverride({ id: item.id, maSanPham: item.maSanPham })[0] || pickImageValue([item, variants]) || fallbackImageFor(item.id, item.maSanPham),

      // gia:
      //   firstVariant?.giaBan ??
      //   item.giaBan ??
      //   item.gia ??
      //   0,

      gia: normalizedGia,

      ton: normalizedTon,

      loai: loaiName,
      type: loaiName,
      hasVariants,
      missingVariantData: !hasVariants,
      ngayTao: item.ngayTao,
      ngaySua: item.ngaySua,

      status: normalizeAdminStatusLabel(item.trangThai)
    }
  })
}

onMounted(loadData)

const filteredList = computed(() =>
  list.value.filter(item =>
    item.ma?.toLowerCase().includes(search.value.toLowerCase()) ||
    item.name?.toLowerCase().includes(search.value.toLowerCase())
  )
)

const groupedProducts = computed(() => {
  const groups = {}

  filteredList.value.forEach(item => {
    const key = item.type || "Khác"

    if (!groups[key]) {
      groups[key] = []
    }

    groups[key].push(item)
  })

  return groups
})

async function remove(id) {
  const target = list.value.find((item) => Number(item.id) === Number(id))
  if (!target) return

  const hideConfirmed = await window.confirmDialog("Ẩn sản phẩm này khỏi danh sách bán? (Khuyến nghị)")
  if (hideConfirmed) {
    try {
      await updateSanPham(id, {
        tenSanPham: target.name,
        moTa: target.description,
        trangThai: "Ngừng hoạt động"
      })
      window.toast.success("Đã ẩn sản phẩm")
      await loadData()
    } catch (err) {
      console.error("Hide failed:", err)
      window.toast.error("Không thể ẩn sản phẩm")
    }
    return
  }

  const deleteConfirmed = await window.confirmDialog("Bạn muốn xóa vĩnh viễn sản phẩm này?")
  if (!deleteConfirmed) return

  try {
    await deleteSanPham(id)
    window.toast.success("Đã xóa vĩnh viễn sản phẩm")
    await loadData()
  } catch (err) {
    console.error("Delete failed:", err)
    window.toast.error("Không thể xoá sản phẩm")
  }
}
</script>

<template>
  <div class="card admin-product-page">

    <div class="head page-header">
      <div>
        <h1 class="page-title">Sản phẩm</h1>
        <small class="muted page-subtitle">
          Danh sách sản phẩm + tồn kho, trạng thái bán
        </small>
      </div>

      <button class="btn-create" @click="goToCreate">
        + Thêm sản phẩm
      </button>
    </div>

    <div class="body">

      <div class="toolbar">
        <div class="filters">
          <input
            v-model="search"
            class="form-input search-input"
            type="text"
            placeholder="Tìm theo mã / tên..."
          />
        </div>
      </div>

      <!-- CATEGORY FRAMES -->

      <div
        v-for="(items, type) in groupedProducts"
        :key="type"
        class="category-frame"
      >

        <div class="category-header">
          {{ type }}
        </div>

        <table class="data-table">
          <thead>
            <tr>
              <th style="width:96px">Ảnh</th>
              <th style="width:120px">Mã</th>
              <th>Sản phẩm</th>
              <th style="width:140px">Loại</th>
              <th style="width:140px" class="right">Giá</th>
              <th style="width:120px" class="right">Tồn</th>
              <th style="width:170px">Ngày tạo</th>
              <th style="width:170px">Ngày sửa</th>
              <th style="width:140px">Trạng thái</th>
              <th style="width:160px" class="col-actions">Thao tác</th>
            </tr>
          </thead>

          <tbody>

            <tr
              v-for="item in items"
              :key="item.id"
            >
              <td>
                <img class="product-thumb" :src="item.image" :alt="item.name" />
              </td>
              <td>{{ item.ma }}</td>

              <td>
                <b>{{ item.name }}</b>

                <div class="muted product-desc">
                  {{ item.description }}
                </div>

                <div v-if="item.missingVariantData" class="warn-inline">
                  Chưa cấu hình biến thể
                </div>
              </td>

              <td>{{ item.loai }}</td>

              <td class="col-actions">
                <template v-if="item.gia === null">
                  <span class="muted">-</span>
                </template>
                <template v-else>
                  {{ formatCurrency(item.gia) }}
                </template>
              </td>

              <td class="right">
                <template v-if="item.ton === null">
                  <span class="muted">-</span>
                </template>
                <template v-else>
                  {{ item.ton }}
                </template>
              </td>

              <td>{{ formatDateTime(item.ngayTao) }}</td>

              <td>{{ formatDateTime(item.ngaySua) }}</td>

              <td>
                <span
                  class="pill"
                  :class="`status-${getAdminStatusTone(item.status)}`"
                >
                  ● {{ item.status }}
                </span>
              </td>

              <td class="right">

                <div class="actions">

                  <button
                    class="iconbtn"
                    @click="goToEdit(item.id)"
                  >
                    <Pencil size="16" />
                  </button>
                <button
                  class="iconbtn"
                  @click="remove(item.id)"
                  title="Xoá sản phẩm"
                >
                  <Trash2 size="16" />
                </button>
                </div>

              </td>
            </tr>

          </tbody>
        </table>

      </div>

      <div
        v-if="filteredList.length === 0"
        class="empty"
      >
        Không có dữ liệu
      </div>

      <div class="pagination">
        <div>
          Hiển thị {{ filteredList.length }} sản phẩm
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap');

.admin-product-page {
  font-family: 'Be Vietnam Pro', 'Segoe UI', Tahoma, sans-serif;
  padding: 28px 30px;
  background: transparent;
  min-height: calc(100vh - 76px);
  border: 1px solid #e5eaf4;
  border-radius: 20px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.04);
  overflow: hidden;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.page-title {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.1;
  letter-spacing: -0.03em;
  font-weight: 800;
}

.page-subtitle {
  display: block;
  margin-top: 8px;
  color: #64748b;
  font-size: 15px;
  font-weight: 500;
}

.btn-create {
  height: 44px;
  border: 0;
  border-radius: 12px;
  background: #ef2d2d;
  color: #fff;
  cursor: pointer;
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.btn-create:hover {
  background: #dc2626;
  transform: translateY(-1px);
}

.body {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 16px;
}

.toolbar {
  margin-bottom: 14px;
}

.search-input {
  width: 320px;
  max-width: 100%;
}

.form-input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #fff;
  font-size: 14px;
  color: #111827;
}

.form-input:focus {
  outline: none;
  border-color: #93c5fd;
  box-shadow: 0 0 0 3px rgba(147, 197, 253, 0.26);
}

.category-frame{
  border:1px solid #e5e7eb;
  border-radius:12px;
  overflow:hidden;
  margin-bottom:14px;
  background:#fff;
}

.category-header{
  background:#f8fafc;
  padding:14px 16px;
  font-weight:700;
  font-size:15px;
  color: #334155;
}

.product-desc{
  font-size:12px;
  margin-top:3px;
  color: #94a3b8;
}

.product-thumb {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #dbe4f0;
  background: #f8fafc;
}

.warn-inline {
  margin-top: 6px;
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  color: #a16207;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 999px;
  padding: 2px 8px;
}

.actions{
  display:flex;
  justify-content:center;
  align-items:center;
  gap:8px;
}

.iconbtn{
  display:flex;
  align-items:center;
  justify-content:center;
  width:32px;
  height:32px;
  border-radius:8px;
  border:1px solid #d1d5db;
  background:#fff;
  cursor:pointer;
  color: #64748b;
}

.iconbtn:hover{
  background:#f1f5f9;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 13px 16px;
  border-bottom: 1px solid #eef2f7;
  text-align: left;
}

.data-table thead th {
  color: #94a3b8;
  background: #f8fafc;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
}

.data-table tbody td {
  color: #334155;
  vertical-align: middle;
}

.col-actions {
  text-align: center !important;
}

.pill.ok {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #15803d;
  background: #dcfce7;
}

.pill.warn {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #b91c1c;
  background: #fee2e2;
}

.right{
  text-align:right;
}

.empty{
  text-align:center;
  padding:52px 12px;
  color:#64748b;
}

.pagination {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 1024px) {
  .page-title {
    font-size: 20px;
  }
}

@media (max-width: 768px) {
  .admin-product-page {
    padding: 18px 14px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-title {
    font-size: 18px;
  }

  .btn-create {
    justify-content: center;
  }

  .search-input {
    width: 100%;
  }
}

</style>