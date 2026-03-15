<script setup>
import { reactive, ref, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  createSanPham,
  deleteSanPham,
  updateSanPham,
  getSanPhamById,
  getAllSanPham
} from "../../../services/sanPhamService"
import { getAllLoai } from "../../../services/loaiService"

const router = useRouter()
const route = useRoute()
const id = route.params.id

function goBack() {
  router.push("/admin/san-pham/list")
}

function saveDraft() {
  window.toast.info("Lưu nháp (demo)")
}

const form = reactive({
  sku: "",
  name: "",
  description: "",
  status: "Đang bán",

  giaBan: "",
  giaGoc: "",
  ton: 0,

  loai: "",
  loaiId: null,

  quickSizes: "M,L",
  quickColors: "Đen",

  tag: "",
  variantId: null
})

const loaiOptions = ref([])

const splitCsv = (value = "") => {
  return String(value)
    .split(",")
    .map((v) => v.trim())
    .filter(Boolean)
}

async function generateProductCode() {
  try {
    const res = await getAllSanPham()
    const rows = Array.isArray(res?.data) ? res.data : []
    const maxCode = rows.reduce((max, item) => {
      const code = String(item?.maSanPham || "")
      const matched = code.match(/^SP(\d+)$/i)
      if (!matched) return max
      return Math.max(max, Number(matched[1] || 0))
    }, 0)

    return `SP${String(maxCode + 1).padStart(3, "0")}`
  } catch {
    return `SP${String(Date.now()).slice(-6)}`
  }
}

function extractSkuNumber(value = "") {
  const matched = String(value).trim().match(/^SP(\d+)$/i)
  if (!matched) return null
  const numeric = Number(matched[1])
  return Number.isFinite(numeric) ? numeric : null
}

function formatSku(num) {
  return `SP${String(num).padStart(3, "0")}`
}

async function handleGenerateSku() {
  const generated = await generateProductCode()
  const generatedNum = extractSkuNumber(generated)
  const currentNum = extractSkuNumber(form.sku)

  let finalSku = generated
  if (generatedNum !== null && currentNum !== null && generatedNum <= currentNum) {
    finalSku = formatSku(currentNum + 1)
  }

  form.sku = finalSku
  window.toast?.info(`Đã sinh mã: ${finalSku}`)
}

function createVariantPayloads() {
  const parsedGiaBan = Number(form.giaBan)
  const parsedTon = Number(form.ton)
  const parsedGiaNhap = Number(form.giaGoc || 0)
  const parsedLoaiId = Number(form.loaiId)

  const sizes = splitCsv(form.quickSizes)
  const colors = splitCsv(form.quickColors)
  const finalSizes = sizes.length ? sizes : ["M"]
  const finalColors = colors.length ? colors : ["Đen"]

  const variants = []
  let idx = 1

  for (const size of finalSizes) {
    for (const color of finalColors) {
      variants.push({
        id: form.variantId,
        ma: `${form.sku}-${String(idx).padStart(2, "0")}`,
        giaNhap: parsedGiaNhap,
        giaBan: parsedGiaBan,
        soLuong: parsedTon,
        chatLieu: { id: 1 },
        mauSac: { id: 1, tenMau: color },
        kichThuoc: { id: 2, tenKichThuoc: size },
        hang: { id: 1 },
        xuatSu: { id: 1 },
        danhMuc: { id: 1 },
        loai: { id: parsedLoaiId },
        trangThai: "Hoạt động"
      })
      idx += 1
    }
  }

  return variants
}

function validateProduct() {
  if (!String(form.name || "").trim()) return "Tên sản phẩm không được để trống"
  if (!String(form.sku || "").trim()) return "Mã sản phẩm không được để trống"
  // Accept any mã sản phẩm that starts with SP and has at least 1 digit
  if (!/^SP\d+/i.test(String(form.sku || "").trim())) return "Mã sản phẩm phải bắt đầu bằng SP + số (ví dụ: SP001)"
  if (!Number.isFinite(Number(form.loaiId)) || Number(form.loaiId) <= 0) return "Vui lòng chọn loại sản phẩm hợp lệ"
  if (!Number.isFinite(Number(form.giaBan)) || Number(form.giaBan) <= 0) return "Giá bán phải lớn hơn 0"
  if (!Number.isFinite(Number(form.ton)) || Number(form.ton) < 0) return "Tồn kho không hợp lệ"
  return ""
}

async function loadLoaiOptions() {
  try {
    const res = await getAllLoai()
    const rows = Array.isArray(res?.data) ? res.data : []
    loaiOptions.value = rows
      .map((item) => ({
        id: Number(item?.id),
        name: String(item?.tenLoai || item?.name || "").trim()
      }))
      .filter((item) => Number.isFinite(item.id) && item.id > 0 && item.name)

    if (!loaiOptions.value.length) {
      loaiOptions.value = [
        { id: 1, name: "Hoodie Jacket" },
        { id: 2, name: "Bomber Jacket" },
        { id: 3, name: "Coach Jacket" }
      ]
    }

    if (!Number.isFinite(Number(form.loaiId)) || Number(form.loaiId) <= 0) {
      form.loaiId = loaiOptions.value[0].id
    }
  } catch {
    loaiOptions.value = [
      { id: 1, name: "Hoodie Jacket" },
      { id: 2, name: "Bomber Jacket" },
      { id: 3, name: "Coach Jacket" }
    ]

    if (!Number.isFinite(Number(form.loaiId)) || Number(form.loaiId) <= 0) {
      form.loaiId = 1
    }
  }
}

function getLoaiNameById(loaiId) {
  return loaiOptions.value.find((item) => Number(item.id) === Number(loaiId))?.name || ""
}

function buildPersistenceError(savedProduct) {
  const variants = Array.isArray(savedProduct?.sanPhamChiTiets) ? savedProduct.sanPhamChiTiets : []
  if (!variants.length) {
    return "Backend chưa lưu được biến thể sản phẩm (loại/giá/tồn). Vui lòng kiểm tra API sản phẩm chi tiết."
  }

  const firstVariant = variants[0]
  const savedLoaiId = Number(firstVariant?.loai?.id || 0)
  const savedGiaBan = Number(firstVariant?.giaBan)
  const savedSoLuong = Number(firstVariant?.soLuong)

  if (savedLoaiId !== Number(form.loaiId)) {
    return `Loại đã lưu không khớp (đã chọn: ${getLoaiNameById(form.loaiId) || form.loaiId}, backend lưu: ${firstVariant?.loai?.tenLoai || savedLoaiId || "không xác định"}).`
  }

  if (!Number.isFinite(savedGiaBan) || savedGiaBan <= 0 || savedGiaBan !== Number(form.giaBan)) {
    return "Giá bán đã lưu không khớp với dữ liệu đã nhập."
  }

  if (!Number.isFinite(savedSoLuong) || savedSoLuong < 0 || savedSoLuong !== Number(form.ton)) {
    return "Tồn kho đã lưu không khớp với dữ liệu đã nhập."
  }

  return ""
}

async function resolveSavedProductId(createdResponse) {
  const candidateId = Number(createdResponse?.data?.id || createdResponse?.id)
  if (Number.isFinite(candidateId) && candidateId > 0) return candidateId

  const listRes = await getAllSanPham()
  const rows = Array.isArray(listRes?.data) ? listRes.data : []
  const matched = rows.find((item) => String(item?.maSanPham || "").trim() === String(form.sku || "").trim())
  const fallbackId = Number(matched?.id)

  if (Number.isFinite(fallbackId) && fallbackId > 0) return fallbackId
  throw new Error("Không tìm thấy sản phẩm vừa tạo để kiểm tra dữ liệu lưu")
}

onMounted(async () => {
  await loadLoaiOptions()

  if (!id) {
    form.sku = await generateProductCode()
  }

  if (!id) return

  const res = await getSanPhamById(id)
  const data = res.data

  const variants = data.sanPhamChiTiets || []
  const firstVariant = variants[0]

  form.variantId = firstVariant?.id || null
  form.ton = firstVariant?.soLuong ?? 0

  form.sku = data.maSanPham
  form.name = data.tenSanPham
  form.description = data.moTa
  form.giaBan =
    firstVariant?.giaBan ??
    data.giaBan ??
    ""

  form.giaGoc = data.giaGoc ?? ""

  // form.danhMuc =
  //   firstVariant?.danhMuc?.tenDanhMuc ||
  //   data.danhMuc ||
  //   ""

  form.loai =
    firstVariant?.loai?.tenLoai ||
    data.loai ||
    ""
  form.loaiId = Number(firstVariant?.loai?.id) || loaiOptions.value[0]?.id || 1

  form.tag = data.tag ?? ""

  form.status =
    data.trangThai === "Hoạt động"
      ? "Đang bán"
      : "Ẩn"
})

async function saveProduct() {
  const error = validateProduct()
  if (error) {
    window.toast.warning(error)
    return
  }

  const action = id ? 'cập nhật' : 'tạo mới'
  const confirmed = await window.confirmDialog(`Bạn có chắc chắn muốn ${action} sản phẩm "${form.name}"?`)
  if (!confirmed) {
    return
  }

  try {
    const variantsPayload = createVariantPayloads()
    const payload = {
      maSanPham: form.sku,
      tenSanPham: form.name,
      moTa: form.description,
      trangThai:
        form.status === "Đang bán"
          ? "Hoạt động"
          : "Ngừng hoạt động",
      sanPhamChiTiets: variantsPayload
    }

    if (id) {
      await updateSanPham(id, payload)
      window.toast.success("Cập nhật sản phẩm thành công")
    } else {
      await createSanPham(payload)
      window.toast.success("Tạo sản phẩm thành công")
    }

    setTimeout(() => {
      router.push("/admin/san-pham/list")
    }, 1000)
  } catch (err) {
    console.error("SAVE ERROR:", err)
    window.toast.error("Lưu thất bại: " + (err.response?.data?.message || err.message))
  }
}
</script>

<template>
  <div class="card">
    <div class="head">
      <div>
        <h1>Form sản phẩm</h1>
        <small class="muted">
          Tạo mới / cập nhật sản phẩm + biến thể
        </small>
      </div>

      <div style="display:flex;gap:10px;flex-wrap:wrap">
        <button type="button" class="btn" @click="goBack">
          ← Quay lại
        </button>

        <button type="button" class="btn" @click="saveDraft">
          Lưu nháp
        </button>

        <button class="btn primary" @click.prevent="saveProduct">
          Lưu
        </button>
      </div>
    </div>

    <div class="body">

      <!-- BASIC INFO -->
      <div class="grid cols2">

        <div class="field">
          <label>Tên sản phẩm</label>
          <input
            v-model="form.name"
            type="text"
            placeholder="VD: Áo thun cotton 220gsm"
          />
        </div>

        <div class="field">
          <label>Mã Sản Phẩm</label>
          <div style="display:flex; gap:8px; align-items:center;">
            <input
              v-model="form.sku"
              type="text"
              :readonly="!!id"
              :placeholder="id ? '' : 'VD: SP001'"
            />
            <button v-if="!id" type="button" class="btn" @click="handleGenerateSku">
              Sinh mã
            </button>
          </div>
        </div>

        <!-- <div class="field">
          <label>Danh mục</label>
          <input
            v-model="form.danhMuc"
            type="text"
            placeholder="VD: Áo"
          />
        </div> -->

        <div class="field">
          <label>Loại / Form</label>
          <select v-model.number="form.loaiId">
            <option v-for="loaiOption in loaiOptions" :key="loaiOption.id" :value="loaiOption.id">
              {{ loaiOption.name }}
            </option>
          </select>
        </div>

        <div class="field">
          <label>Giá bán</label>
          <input
            v-model.number="form.giaBan"
            type="number"
            placeholder="329000"
          />
        </div>

        <div class="field">
        <label>Tồn kho</label>
        <input
          v-model.number="form.ton"
          type="number"
          placeholder="0"
        />
        </div>

        <div class="field">
          <label>Giá gốc (tuỳ chọn)</label>
          <input
            v-model.number="form.giaGoc"
            type="number"
            placeholder="389000"
          />
        </div>

        <div class="field">
          <label>Trạng thái</label>
          <select v-model="form.status">
            <option>Đang bán</option>
            <option>Ẩn</option>
          </select>
        </div>

        <div class="field">
          <label>Tag</label>
          <input
            v-model="form.tag"
            type="text"
            placeholder="VD: New, Best, Sale"
          />
        </div>

        <div class="field">
          <label>Thêm nhanh size (phân tách dấu phẩy)</label>
          <input
            v-model="form.quickSizes"
            type="text"
            placeholder="VD: S,M,L,XL"
          />
        </div>

        <div class="field">
          <label>Thêm nhanh màu (phân tách dấu phẩy)</label>
          <input
            v-model="form.quickColors"
            type="text"
            placeholder="VD: Đen,Trắng,Xám"
          />
        </div>

        <div class="field" style="grid-column: 1 / -1">
          <label>Mô tả</label>
          <textarea
            v-model="form.description"
            placeholder="Chất liệu, form, hướng dẫn bảo quản..."
          ></textarea>
        </div>

      </div>

    </div>
  </div>
</template>

<style scoped>
input,
select,
textarea {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d8dee9;
  background: #fff;
  font-size: 14px;
}

input:focus,
select:focus,
textarea:focus {
  outline: none;
  border-color: #4f46e5;
}

textarea {
  min-height: 90px;
  resize: vertical;
}
</style>