<script setup>
import { reactive, ref, onMounted, computed } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  createSanPham,
  deleteSanPham,
  updateSanPham,
  getSanPhamById,
  getAllSanPham
} from "../../../services/sanPhamService"
import { getAllLoai } from "../../../services/loaiService"
import { getAllMauSac } from "../../../services/mauSacService"
import { getAllKichThuoc } from "../../../services/kichThuocService"
import { uploadImage, getImageUrl } from "../../../services/uploadService"

const router = useRouter()
const route = useRoute()
const id = route.params.id
const filterColor = route.query.color ? decodeURIComponent(String(route.query.color)) : null

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

  quickSizes: "",
  quickColors: "Đen",

  tag: "",
  variantId: null
})

const loaiOptions = ref([])
const variants = ref([])
const editingVariant = ref(null)
const showVariantEditor = ref(false)

// Dữ liệu màu sắc và kích thước từ backend
const mauSacList = ref([])
const kichThuocList = ref([])

// Ảnh theo màu sắc
const colorImages = ref({})

function onColorImageChange(mauSac, event) {
  const file = event.target.files?.[0]
  if (!file) return
  const previewUrl = URL.createObjectURL(file)
  colorImages.value = { ...colorImages.value, [mauSac]: { file, previewUrl } }
  // Cập nhật hinhAnh preview cho tất cả variants của màu này
  variants.value.forEach(v => {
    if (v.mauSac === mauSac) v.hinhAnh = previewUrl
  })
}

function onColorImageDrop(mauSac, event) {
  const file = event.dataTransfer.files?.[0]
  if (!file || !file.type.startsWith('image/')) return
  colorImages.value = { ...colorImages.value, [mauSac]: { file, previewUrl: URL.createObjectURL(file) } }
}

function removeColorImage(mauSac) {
  const copy = { ...colorImages.value }
  delete copy[mauSac]
  colorImages.value = copy
}

const availableColors = ref(["Đen", "Trắng", "Xanh dương", "Đỏ", "Vàng", "Xám", "Xanh lá"])
const availableSizes = ref(["XS", "S", "M", "L", "XL", "XXL", "2XL", "3XL"])
const selectedColors = ref([])
const selectedSizes = ref([])
const customColor = ref("")
const customSize = ref("")
const showColorDropdown = ref(false)
const showSizeDropdown = ref(false)

// Color dot map
const colorDotMap = {
  "Đen": "#222",
  "Trắng": "#fff",
  "Xanh dương": "#3b82f6",
  "Đỏ": "#ef4444",
  "Vàng": "#eab308",
  "Xám": "#9ca3af",
  "Xanh lá": "#22c55e",
}

function getColorDot(color) {
  return colorDotMap[color] || "#aaa"
}

function updateSelectedFromForm() {
  selectedColors.value = splitCsv(form.quickColors)
  selectedSizes.value = splitCsv(form.quickSizes)
}

function syncQuickFromSelected() {
  form.quickColors = selectedColors.value.join(",")
  form.quickSizes = selectedSizes.value.join(",")
}

function addColor(color) {
  const c = String(color || customColor.value || "").trim()
  if (!c) return
  if (!selectedColors.value.includes(c)) {
    selectedColors.value.push(c)
    if (!availableColors.value.includes(c)) availableColors.value.push(c)
    syncQuickFromSelected()
    window.toast?.success(`Đã thêm màu: ${c}`)
  }
  customColor.value = ""
  showColorDropdown.value = false
}

function removeColor(color) {
  selectedColors.value = selectedColors.value.filter((c) => c !== color)
  syncQuickFromSelected()
}

function addSize(size) {
  const s = String(size || customSize.value || "").trim()
  if (!s) return
  if (!selectedSizes.value.includes(s)) {
    selectedSizes.value.push(s)
    if (!availableSizes.value.includes(s)) availableSizes.value.push(s)
    syncQuickFromSelected()
    window.toast?.success(`Đã thêm kích cỡ: ${s}`)
  }
  customSize.value = ""
  showSizeDropdown.value = false
}

function removeSize(value) {
  selectedSizes.value = selectedSizes.value.filter((s) => s !== value)
  syncQuickFromSelected()
}

function onColorSelectionChange() {
  syncQuickFromSelected()
}

function onSizeSelectionChange() {
  syncQuickFromSelected()
}

const splitCsv = (value = "") => {
  return String(value)
    .split(",")
    .map((v) => v.trim())
    .filter(Boolean)
}

function generateVariantsPreview() {
  const sizes = splitCsv(form.quickSizes)
  const colors = splitCsv(form.quickColors)
  const finalSizes = sizes.length ? sizes : ["M"]
  const finalColors = colors.length ? colors : ["Đen"]

  const preview = []
  let idx = 1

  for (const size of finalSizes) {
    for (const color of finalColors) {
      preview.push({
        id: idx,
        ma: `${form.sku}-${String(idx).padStart(2, "0")}`,
        kichThuoc: size,
        mauSac: color,
        giaBan: form.giaBan || 0,
        giaGoc: form.giaGoc || 0,
        soLuong: form.ton || 0,
        status: true
      })
      idx += 1
    }
  }

  return preview
}

// Chỉ thêm biến thể mới, giữ nguyên các biến thể đã có
function refreshVariantsPreview() {
  const sizes = splitCsv(form.quickSizes)
  const colors = splitCsv(form.quickColors)
  const finalSizes = sizes.length ? sizes : ["M"]
  const finalColors = colors.length ? colors : ["Đen"]

  const maxId = variants.value.reduce((m, v) => Math.max(m, v.id || 0), 0)
  let nextId = maxId + 1

  for (const size of finalSizes) {
    for (const color of finalColors) {
      const exists = variants.value.some(
        (v) => v.kichThuoc === size && v.mauSac === color
      )
      if (!exists) {
        variants.value.push({
          id: nextId,       // id tạm, chỉ dùng cho UI
          dbId: null,       // null = chưa có trong DB
          ma: `${form.sku}-${String(nextId).padStart(2, "0")}`,
          kichThuoc: size,
          mauSac: color,
          giaBan: form.giaBan || 0,
          giaGoc: form.giaGoc || 0,
          soLuong: form.ton || 0,
          status: true
        })
        nextId++
      }
    }
  }
}

function openVariantEditor(variant) {
  editingVariant.value = { ...variant }
  showVariantEditor.value = true
}

function closeVariantEditor() {
  showVariantEditor.value = false
  editingVariant.value = null
}

function saveVariantEdit() {
  if (!editingVariant.value) return
  
  const idx = variants.value.findIndex(v => v.id === editingVariant.value.id)
  if (idx >= 0) {
    variants.value[idx] = { ...editingVariant.value }
  }
  closeVariantEditor()
  window.toast?.success("Cập nhật biến thể thành công")
}

function deleteVariant(id) {
  variants.value = variants.value.filter(v => v.id !== id)
  window.toast?.info("Đã xóa biến thể")
}

function clearAllVariants() {
  variants.value = []
  window.toast?.info("Đã xóa tất cả biến thể")
}

function addVariantToGroup(mauSac) {
  const size = prompt("Nhập kích cỡ mới:")
  if (!size || !size.trim()) return
  const maxId = variants.value.reduce((m, v) => Math.max(m, v.id || 0), 0)
  const nextId = maxId + 1
  variants.value.push({
    id: nextId,
    dbId: null,
    ma: `${form.sku}-${String(nextId).padStart(2, "0")}`,
    kichThuoc: size.trim(),
    mauSac,
    giaBan: form.giaBan || 0,
    giaGoc: form.giaGoc || 0,
    soLuong: form.ton || 0,
    status: true
  })
}

function generateAllVariants() {
  refreshVariantsPreview()
  window.toast?.success("Đã tạo nhanh tất cả biến thể")
}

const groupedVariants = computed(() => {
  const map = {}
  variants.value.forEach((variant) => {
    const key = variant.mauSac || "Không rõ"
    if (!map[key]) map[key] = []
    map[key].push(variant)
  })
  return Object.entries(map).map(([mauSac, items]) => ({ mauSac, items }))
})

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

async function handleVariantImageChange(variant, event) {
  const file = event.target.files?.[0]
  if (!file) return
  try {
    const res = await uploadImage(file)
    variant.hinhAnh = res.data.url
    window.toast?.success("Đã tải ảnh lên")
  } catch {
    window.toast?.error("Lỗi tải ảnh")
  }
}

function createVariantPayloads() {
  const parsedLoaiId = Number(form.loaiId)

  return variants.value.map((variant) => {
    const mauSacObj = mauSacList.value.find(
      m => m.tenMau?.toLowerCase() === variant.mauSac?.toLowerCase()
    )
    const kichThuocObj = kichThuocList.value.find(
      k => k.tenKichThuoc?.toLowerCase() === variant.kichThuoc?.toLowerCase()
    )

    // Chỉ gửi id nếu là biến thể đã tồn tại trong DB (id từ backend, không phải id tạm)
    const realId = variant.dbId ? variant.dbId : null

    return {
      ...(realId ? { id: realId } : {}),
      ma: variant.ma,
      giaNhap: Number(variant.giaGoc || 0),
      giaBan: Number(variant.giaBan || 0),
      soLuong: Number(variant.soLuong || 0),
      chatLieu: { id: 1 },
      mauSac: mauSacObj ? { id: mauSacObj.id } : { tenMau: variant.mauSac },
      kichThuoc: kichThuocObj ? { id: kichThuocObj.id } : { tenKichThuoc: variant.kichThuoc },
      hang: { id: 1 },
      xuatSu: { id: 1 },
      danhMuc: { id: 1 },
      loai: { id: parsedLoaiId },
      trangThai: variant.status ? "Hoạt động" : "Ngừng hoạt động",
      hinhAnh: variant.hinhAnh || null
    }
  })
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
  // Close dropdowns on outside click
  document.addEventListener('click', () => {
    showColorDropdown.value = false
    showSizeDropdown.value = false
  })

  await loadLoaiOptions()

  // Load màu sắc và kích thước từ backend
  try {
    const [mauRes, kichRes] = await Promise.all([getAllMauSac(), getAllKichThuoc()])
    mauSacList.value = Array.isArray(mauRes?.data) ? mauRes.data : []
    kichThuocList.value = Array.isArray(kichRes?.data) ? kichRes.data : []
  } catch {
    // fallback: dùng tên thay id
  }
  updateSelectedFromForm()

  if (!id) {
    form.sku = await generateProductCode()
  }

  if (!id) {
    syncQuickFromSelected()
    refreshVariantsPreview()
    return
  }

  const res = await getSanPhamById(id)
  const data = res.data

  const variantsList = data.sanPhamChiTiets || []
  const firstVariant = variantsList[0]

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

  form.loai =
    firstVariant?.loai?.tenLoai ||
    data.loai ||
    ""
  form.loaiId = Number(firstVariant?.loai?.id) || loaiOptions.value[0]?.id || 1

  form.tag = data.tag ?? ""

  // Nếu tất cả biến thể đều ngừng hoạt động → hiển thị "Ẩn"
  const allVariantsStopped = variantsList.length > 0 &&
    variantsList.every(v => v.trangThai === "Ngừng hoạt động")

  form.status =
    (data.trangThai === "Hoạt động" && !allVariantsStopped)
      ? "Đang bán"
      : "Ẩn"

  // Load tất cả biến thể từ backend vào variants.value
  if (variantsList.length) {
    variants.value = variantsList.map((v, idx) => ({
      id: idx + 1,
      dbId: v.id,
      ma: v.ma || `${data.maSanPham}-${String(idx + 1).padStart(2, "0")}`,
      kichThuoc: v.kichThuoc?.tenKichThuoc || "",
      mauSac: v.mauSac?.tenMau || "",
      giaBan: Number(v.giaBan || 0),
      giaGoc: Number(v.giaNhap || 0),
      soLuong: Number(v.soLuong || 0),
      status: v.trangThai !== "Ngừng hoạt động"
    }))

    // Nếu có filterColor (từ trang biến thể), chỉ hiển thị biến thể của màu đó
    if (filterColor) {
      variants.value = variants.value.filter(v => v.mauSac === filterColor)
    }

    // Sync selectedColors và selectedSizes từ danh sách biến thể hiện tại
    const allColors = [...new Set(variants.value.map(v => v.mauSac).filter(Boolean))]
    const allSizes = [...new Set(variants.value.map(v => v.kichThuoc).filter(Boolean))]

    selectedColors.value = allColors
    selectedSizes.value = allSizes
    form.quickColors = allColors.join(",")
    form.quickSizes = allSizes.join(",")

    for (const c of allColors) {
      if (!availableColors.value.includes(c)) availableColors.value.push(c)
    }
    for (const s of allSizes) {
      if (!availableSizes.value.includes(s)) availableSizes.value.push(s)
    }
  } else {
    refreshVariantsPreview()
  }
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
    const payload = {
      maSanPham: form.sku,
      tenSanPham: form.name,
      moTa: form.description,
      trangThai: form.status === "Đang bán" ? "Hoạt động" : "Ngừng hoạt động",
    }

    const variantsPayload = createVariantPayloads()
    if (!variantsPayload.length) {
      window.toast.warning("Vui lòng tạo ít nhất một biến thể trước khi lưu")
      return
    }

    // Validate giá bán của từng biến thể
    const invalidVariant = variantsPayload.find(v => !v.giaBan || v.giaBan <= 0)
    if (invalidVariant) {
      window.toast.warning("Giá bán của tất cả biến thể phải lớn hơn 0")
      return
    }

    // Upload ảnh cho từng màu nếu có file mới
    for (const [mauSac, imgData] of Object.entries(colorImages.value)) {
      if (!imgData?.file) continue
      try {
        const res = await uploadImage(imgData.file)
        const uploadedUrl = res.data?.url || res.data
        variants.value.forEach(v => {
          if (v.mauSac === mauSac) v.hinhAnh = uploadedUrl
        })
      } catch {
        window.toast.warning(`Không thể upload ảnh cho màu ${mauSac}, bỏ qua`)
      }
    }

    // Nếu sản phẩm đang bán, đảm bảo tất cả biến thể đều active
    if (form.status === "Đang bán") {
      variants.value.forEach(v => { v.status = true })
    }

    // Gọi lại SAU khi upload để hinhAnh đã được cập nhật
    payload.sanPhamChiTiets = createVariantPayloads()

    if (id) {
      await updateSanPham(id, payload)
      window.toast.success("Cập nhật sản phẩm thành công")
    } else {
      await createSanPham(payload)
      window.toast.success("Tạo sản phẩm thành công")
    }

    setTimeout(() => {
      router.push("/admin/san-pham/chi-tiet/list")
    }, 1000)
  } catch (err) {
    console.error("SAVE ERROR:", err)
    const errData = err.response?.data
    console.error("Backend error detail:", JSON.stringify(errData, null, 2))
    console.error("Payload sent:", JSON.stringify(createVariantPayloads(), null, 2))
    const msg = errData?.message || errData?.error || (typeof errData === 'string' ? errData : err.message)
    window.toast.error("Lưu thất bại: " + msg)
  }
}
</script>

<template>
  <div class="card">
    <div class="head">
      <div>
        <h1>Form sản phẩm</h1>
        <small class="muted">Tạo mới / cập nhật sản phẩm + biến thể</small>
      </div>

      <div class="action-buttons">
        <button type="button" class="btn" @click="goBack">← Quay lại</button>
        <button type="button" class="btn" @click="saveDraft">Lưu nháp</button>
        <button type="button" class="btn primary" @click.prevent="saveProduct">Lưu</button>
      </div>
    </div>

    <div class="body">
      <div class="main-layout">
        <div class="form-section">
          <h2>Thêm thông tin sản phẩm</h2>
          <div class="grid cols2">
            <div class="field">
              <label>Tên sản phẩm</label>
              <input v-model="form.name" type="text" placeholder="VD: Áo thun cotton 220gsm" />
            </div>

            <div class="field">
              <label>Mã Sản Phẩm</label>
              <div class="inline-control">
                <input v-model="form.sku" type="text" :readonly="!!id" :placeholder="id ? '' : 'VD: SP001'" />
                <button v-if="!id" type="button" class="btn" @click="handleGenerateSku">Sinh mã</button>
              </div>
            </div>

            <div class="field">
              <label>Loại / Form</label>
              <select v-model.number="form.loaiId">
                <option v-for="loaiOption in loaiOptions" :key="loaiOption.id" :value="loaiOption.id">{{ loaiOption.name }}</option>
              </select>
            </div>

            <div class="field">
              <label>Giá bán</label>
              <input v-model.number="form.giaBan" type="number" placeholder="329000" />
            </div>

            <div class="field">
              <label>Tồn kho</label>
              <input v-model.number="form.ton" type="number" placeholder="0" />
            </div>

            <div class="field">
              <label>Giá gốc (tuỳ chọn)</label>
              <input v-model.number="form.giaGoc" type="number" placeholder="389000" />
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
              <input v-model="form.tag" type="text" placeholder="VD: New, Best, Sale" />
            </div>

            <div class="field">
              <label>Thêm nhanh size (phân tách dấu phẩy)</label>
              <input v-model="form.quickSizes" type="text" placeholder="VD: S,M,L,XL" />
            </div>

            <div class="field">
              <label>Thêm nhanh màu (phân tách dấu phẩy)</label>
              <input v-model="form.quickColors" type="text" placeholder="VD: Đen,Trắng,Xám" />
            </div>

            <div class="field full-width">
              <label>Mô tả</label>
              <textarea v-model="form.description" placeholder="Chất liệu, form, hướng dẫn bảo quản..."></textarea>
            </div>
          </div>
        </div>

        <div class="variant-panel">
          <h2>Biến thể sản phẩm</h2>

          <!-- Màu sắc -->
          <div class="field">
            <div class="variant-field-header">
              <label>Màu sắc <span class="required">*</span></label>
              <button type="button" class="btn btn-sm" @click="showColorDropdown = !showColorDropdown">Thêm màu</button>
            </div>
            <div class="tag-input-box" @click.stop>
              <div class="tag-list">
                <span v-for="color in selectedColors" :key="color" class="color-tag">
                  <span class="color-dot" :style="{ background: getColorDot(color) }"></span>
                  {{ color }}
                  <button type="button" class="tag-remove" @click="removeColor(color)">×</button>
                </span>
              </div>
              <div class="tag-actions">
                <button type="button" class="tag-clear" @click="selectedColors = []; syncQuickFromSelected()" title="Xóa tất cả">×</button>
                <button type="button" class="tag-toggle" @click="showColorDropdown = !showColorDropdown">{{ showColorDropdown ? '▲' : '▼' }}</button>
              </div>
            </div>
            <div v-if="showColorDropdown" class="tag-dropdown" @click.stop>
              <div v-for="c in availableColors" :key="c"
                class="tag-option"
                :class="{ selected: selectedColors.includes(c) }"
                @click="selectedColors.includes(c) ? removeColor(c) : addColor(c)">
                <span class="color-dot" :style="{ background: getColorDot(c) }"></span>
                {{ c }}
              </div>
              <div class="tag-option-input">
                <input v-model="customColor" placeholder="Nhập màu mới..." @keyup.enter="addColor()" />
                <button type="button" class="btn btn-sm primary" @click="addColor()">Thêm</button>
              </div>
            </div>
          </div>

          <!-- Kích cỡ -->
          <div class="field">
            <label>Kích cỡ <span class="required">*</span></label>
            <div class="size-tag-row">
              <button type="button" class="btn btn-sm" @click.stop="showSizeDropdown = !showSizeDropdown">Chọn kích cỡ</button>
              <span v-for="size in selectedSizes" :key="size" class="size-tag">
                {{ size }}
                <button type="button" class="tag-remove" @click="removeSize(size)">×</button>
              </span>
            </div>
            <div v-if="showSizeDropdown" class="tag-dropdown" @click.stop>
              <div class="tag-options-grid">
                <div v-for="s in availableSizes" :key="s"
                  class="tag-option"
                  :class="{ selected: selectedSizes.includes(s) }"
                  @click="selectedSizes.includes(s) ? removeSize(s) : addSize(s)">
                  {{ s }}
                </div>
              </div>
              <div class="tag-option-input">
                <input v-model="customSize" placeholder="Nhập size mới..." @keyup.enter="addSize()" />
                <button type="button" class="btn btn-sm primary" @click="addSize()">Thêm</button>
              </div>
            </div>
          </div>

          <!-- Loại sản phẩm -->
          <div class="field">
            <label>Loại sản phẩm <span class="required">*</span></label>
            <div class="tag-input-box">
              <div class="tag-list">
                <span class="color-tag">
                  {{ loaiOptions.find(l => l.id === form.loaiId)?.name || 'Chưa chọn' }}
                  <button type="button" class="tag-remove" @click="form.loaiId = null">×</button>
                </span>
              </div>
              <div class="tag-actions">
                <button type="button" class="tag-toggle">▼</button>
              </div>
            </div>
            <select v-model.number="form.loaiId" class="mt-4">
              <option v-for="lo in loaiOptions" :key="lo.id" :value="lo.id">{{ lo.name }}</option>
            </select>
          </div>

          <button type="button" class="btn primary full-width" @click="refreshVariantsPreview">Tạo biến thể</button>
        </div>
      </div>

      <div class="variant-list">
        <div class="variant-list-header">
          <div class="variant-list-title">
            <span class="variant-list-icon">⊞</span>
            Danh sách biến thể
          </div>
          <button type="button" class="btn btn-danger" @click="clearAllVariants">🗑 Xóa tất cả</button>
        </div>

        <div v-if="variants.length === 0" class="empty-variants">
          Chưa có biến thể nào. Vui lòng tạo biến thể.
        </div>

        <div v-for="group in groupedVariants" :key="group.mauSac" class="variant-group">
          <div class="variant-group-header">
            <div class="variant-group-title">
              <span class="color-dot" :style="{ background: getColorDot(group.mauSac) }"></span>
              <span class="group-color-name">{{ group.mauSac }}</span>
              <span class="group-count">({{ group.items.length }} biến thể)</span>
            </div>
            <button type="button" class="btn btn-dark btn-sm" @click="addVariantToGroup(group.mauSac)">⚡ Thêm nhanh</button>
          </div>

          <table class="group-table">
            <thead>
              <tr>
                <th>Kích cỡ</th>
                <th>Số lượng</th>
                <th>Giá bán (VNĐ)</th>
                <th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="variant in group.items" :key="variant.id">
                <td class="cell-size-info">
                  <span class="size-badge">{{ variant.kichThuoc }}</span>
                  <span class="size-meta">• {{ loaiOptions.find(l => l.id === form.loaiId)?.name || '' }}</span>
                </td>
                <td>
                  <input class="inline-input" v-model.number="variant.soLuong" type="number" placeholder="Nhập số lượng" min="0" />
                </td>
                <td>
                  <input class="inline-input" v-model.number="variant.giaBan" type="number" placeholder="Nhập giá bán" min="0" />
                </td>
                <td class="cell-action">
                  <button type="button" class="btn-delete-variant" @click="deleteVariant(variant.id)">🗑</button>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- Ảnh cho màu này -->
          <div class="group-color-image">
            <div class="group-color-image-box" @click="$refs['img_' + group.mauSac][0].click()" @dragover.prevent @drop.prevent="onColorImageDrop(group.mauSac, $event)">
              <img v-if="colorImages[group.mauSac]?.previewUrl" :src="colorImages[group.mauSac].previewUrl" class="group-color-image-preview" />
              <div v-else class="group-color-image-placeholder">
                <span style="font-size:22px;opacity:0.3">🖼</span>
                <span>Chưa có ảnh</span>
              </div>
            </div>
            <input type="file" accept="image/*" :ref="'img_' + group.mauSac" style="display:none" @change="onColorImageChange(group.mauSac, $event)" />
            <button type="button" class="btn btn-sm" @click="$refs['img_' + group.mauSac][0].click()">
              🖼 Thêm ảnh {{ group.mauSac }}
            </button>
          </div>
        </div>
      </div>

      <div v-if="showVariantEditor" class="modal-overlay" @click.self="closeVariantEditor">
        <div class="modal-content">
          <div class="modal-header">
            <h3>Chỉnh sửa biến thể</h3>
            <button type="button" class="btn-close" @click="closeVariantEditor">✕</button>
          </div>

          <div class="modal-body" v-if="editingVariant">
            <div class="field"><label>Mã biến thể</label><input v-model="editingVariant.ma" readonly /></div>
            <div class="field"><label>Size</label><input v-model="editingVariant.kichThuoc" /></div>
            <div class="field"><label>Màu</label><input v-model="editingVariant.mauSac" /></div>
            <div class="field"><label>Giá bán</label><input v-model.number="editingVariant.giaBan" type="number" /></div>
            <div class="field"><label>Giá gốc</label><input v-model.number="editingVariant.giaGoc" type="number" /></div>
            <div class="field"><label>Tồn kho</label><input v-model.number="editingVariant.soLuong" type="number" /></div>
            <div class="field"><label><input type="checkbox" v-model="editingVariant.status" /> Hoạt động</label></div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn" @click="closeVariantEditor">Hủy</button>
            <button type="button" class="btn primary" @click="saveVariantEdit">Lưu</button>
          </div>
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

/* ===== GRID LAYOUT ===== */
.grid {
  display: grid;
  gap: 16px;
}

.grid.cols2 {
  grid-template-columns: repeat(2, 1fr);
}

.field {
  display: flex;
  flex-direction: column;
}

.field label {
  margin-bottom: 8px;
  font-weight: 500;
  color: #2d3748;
  font-size: 14px;
}

.btn {
  padding: 8px 12px;
  border: 1px solid #d8dee9;
  border-radius: 6px;
  background: white;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  color: #2d3748;
}

.btn:hover {
  border-color: #4f46e5;
  color: #4f46e5;
}

.btn.primary {
  background: #4f46e5;
  color: white;
  border-color: #4f46e5;
}

.btn.primary:hover {
  background: #4338ca;
  border-color: #4338ca;
}

/* ===== MAIN LAYOUT ===== */
.main-layout {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
  align-items: start;
}

.form-section {
  min-width: 0;
}

/* ===== VARIANTS PREVIEW ===== */
.variants-preview {
  background: #f9fafb;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  position: sticky;
  top: 20px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

.variants-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 10px;
}

.variants-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
}

.btn-update {
  background: #4f46e5;
  color: white;
  border: none;
  border-radius: 6px;
  width: 32px;
  height: 32px;
  cursor: pointer;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.btn-update:hover {
  background: #4338ca;
}

/* ===== COMPACT TABLE ===== */
.variants-compact-table {
  overflow: hidden;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.variants-compact-table table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  font-size: 12px;
}

.variants-compact-table thead {
  background: #f0f4f8;
  border-bottom: 1px solid #e2e8f0;
}

.variants-compact-table th {
  padding: 8px 6px;
  text-align: left;
  font-weight: 600;
  color: #4a5568;
  white-space: nowrap;
}

.variants-compact-table td {
  padding: 8px 6px;
  border-bottom: 1px solid #e2e8f0;
  color: #2d3748;
  word-break: break-word;
}

.variants-compact-table tbody tr:hover {
  background: #f7fafc;
}

.cell-code {
  font-weight: 500;
  font-size: 11px;
}

.cell-size,
.cell-color {
  font-size: 11px;
}

.cell-price {
  font-weight: 500;
  color: #4f46e5;
  font-size: 11px;
}

.cell-actions {
  text-align: center;
  padding: 4px 2px !important;
}

.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  padding: 2px 4px;
  color: #4f46e5;
  transition: color 0.2s;
}

.btn-icon:hover {
  color: #4338ca;
}

/* ===== EMPTY STATE ===== */
.empty-variants {
  padding: 20px 16px;
  text-align: center;
  color: #a0aec0;
  background: white;
  border-radius: 6px;
  border: 1px dashed #cbd5e0;
}

.empty-variants p {
  margin: 0 0 4px 0;
  font-size: 13px;
  font-weight: 500;
}

.empty-variants small {
  display: block;
  font-size: 11px;
  color: #cbd5e0;
}

/* ===== MODAL ===== */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 90%;
  max-width: 500px;
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(30px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.btn-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #a0aec0;
  transition: color 0.2s;
}

.btn-close:hover {
  color: #4a5568;
}

.modal-body {
  padding: 20px;
  max-height: 70vh;
  overflow-y: auto;
}

.modal-body .field {
  margin-bottom: 16px;
}

.modal-body .field:last-child {
  margin-bottom: 0;
}

.modal-body label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-weight: 500;
  color: #2d3748;
}

.modal-body input[type="checkbox"] {
  width: auto;
}

.modal-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 20px;
  border-top: 1px solid #e2e8f0;
  background: #f9fafb;
}

.modal-footer .btn {
  padding: 8px 16px;
}

/* ===== RESPONSIVE ===== */
@media (max-width: 1200px) {
  .main-layout {
    grid-template-columns: 1fr;
  }

  .variants-preview {
    position: relative;
    top: 0;
    max-height: none;
  }
}

/* ===== VARIANT PANEL ===== */
.variant-panel {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.variant-panel h2 {
  margin: 0 0 4px 0;
  font-size: 15px;
  font-weight: 600;
}

.variant-field-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.variant-field-header label {
  margin-bottom: 0;
}

.required {
  color: #ef4444;
}

/* Tag input box (màu sắc, loại) */
.tag-input-box {
  display: flex;
  align-items: center;
  border: 1px solid #d8dee9;
  border-radius: 10px;
  padding: 6px 10px;
  background: #fff;
  min-height: 42px;
  cursor: pointer;
  gap: 6px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
}

.tag-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.tag-clear,
.tag-toggle {
  background: none;
  border: none;
  cursor: pointer;
  color: #9ca3af;
  font-size: 14px;
  padding: 2px 4px;
  line-height: 1;
}

.tag-clear:hover,
.tag-toggle:hover {
  color: #4a5568;
}

/* Color tag chip */
.color-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 3px 8px 3px 6px;
  font-size: 13px;
  color: #2d3748;
}

.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid rgba(0,0,0,0.15);
  flex-shrink: 0;
  display: inline-block;
}

.tag-remove {
  background: none;
  border: none;
  cursor: pointer;
  color: #9ca3af;
  font-size: 16px;
  line-height: 1;
  padding: 0 0 0 2px;
}

.tag-remove:hover {
  color: #ef4444;
}

/* Size tag row */
.size-tag-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-height: 42px;
}

.size-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid #d8dee9;
  border-radius: 8px;
  padding: 4px 10px;
  font-size: 13px;
  font-weight: 500;
  background: #fff;
  color: #2d3748;
}

/* Dropdown */
.tag-dropdown {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  margin-top: 4px;
  overflow: hidden;
  z-index: 100;
  position: relative;
}

.tag-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  font-size: 13px;
  color: #2d3748;
  transition: background 0.15s;
}

.tag-option:hover {
  background: #f1f5f9;
}

.tag-option.selected {
  background: #3b82f6;
  color: #fff;
}

.tag-option.selected .color-dot {
  border-color: rgba(255,255,255,0.4);
}

.tag-option-input {
  display: flex;
  gap: 8px;
  padding: 8px 10px;
  border-top: 1px solid #e2e8f0;
  background: #f9fafb;
}

.tag-option-input input {
  flex: 1;
  padding: 6px 10px;
  font-size: 13px;
  border-radius: 6px;
  border: 1px solid #d8dee9;
}

.btn-sm {
  padding: 5px 10px;
  font-size: 12px;
}

.full-width {
  width: 100%;
}

.tag-options-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
  padding: 8px;
}

.tag-options-grid .tag-option {
  justify-content: center;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 6px 4px;
  text-align: center;
}

.tag-options-grid .tag-option.selected {
  background: #3b82f6;
  color: #fff;
  border-color: #3b82f6;
}

.mt-4 {
  margin-top: 8px;
}

/* ===== VARIANT LIST GROUPED ===== */
.variant-list {
  margin-top: 24px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.variant-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: linear-gradient(135deg, #7f1d1d, #1a1a2e);
  color: #fff;
}

.variant-list-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

.variant-list-icon {
  font-size: 18px;
}

.btn-danger {
  background: #dc2626;
  color: #fff;
  border-color: #dc2626;
}

.btn-danger:hover {
  background: #b91c1c;
  border-color: #b91c1c;
  color: #fff;
}

.btn-dark {
  background: #1e293b;
  color: #fff;
  border-color: #1e293b;
}

.btn-dark:hover {
  background: #0f172a;
  border-color: #0f172a;
  color: #fff;
}

.variant-group {
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.variant-group:last-child {
  border-bottom: none;
}

.variant-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.variant-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-color-name {
  font-weight: 600;
  font-size: 14px;
  color: #1e293b;
}

.group-count {
  font-size: 13px;
  color: #64748b;
}

.group-table {
  width: 100%;
  border-collapse: collapse;
}

.group-table thead tr {
  background: #f1f5f9;
}

.group-table th {
  padding: 10px 20px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  border-bottom: 1px solid #e2e8f0;
}

.group-table td {
  padding: 10px 20px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
  color: #334155;
}

.group-table tbody tr:last-child td {
  border-bottom: none;
}

.group-table tbody tr:hover {
  background: #f8fafc;
}

.cell-size-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.size-badge {
  font-weight: 600;
  color: #1e293b;
}

.size-meta {
  color: #94a3b8;
  font-size: 12px;
}

.inline-input {
  width: 100%;
  padding: 7px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  background: #fff;
  color: #334155;
  transition: border-color 0.2s;
}

.inline-input:focus {
  outline: none;
  border-color: #3b82f6;
}

.cell-action {
  text-align: center;
  width: 60px;
}

.btn-delete-variant {
  background: #dc2626;
  color: #fff;
  border: none;
  border-radius: 8px;
  width: 34px;
  height: 34px;
  cursor: pointer;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.btn-delete-variant:hover {
  background: #b91c1c;
}

/* ===== COLOR IMAGES ===== */
.group-color-image {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.group-color-image-box {
  width: 80px;
  height: 80px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  background: #fff;
  flex-shrink: 0;
}

.group-color-image-box:hover {
  border-color: #94a3b8;
}

.group-color-image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.group-color-image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #94a3b8;
  font-size: 11px;
}

.color-images-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  font-weight: 600;
  font-size: 14px;
  color: #1e293b;
}

.color-images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  border-top: none;
}

.color-image-card {
  padding: 16px;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.color-image-card:last-child {
  border-right: none;
}

.color-image-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.color-radio-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid rgba(0,0,0,0.15);
  display: inline-block;
  flex-shrink: 0;
}

.color-image-box {
  width: 100%;
  aspect-ratio: 4/3;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  background: #f8fafc;
  transition: border-color 0.2s;
}

.color-image-box:hover {
  border-color: #94a3b8;
}

.color-image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.color-image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.btn-add-image {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
  color: #475569;
  text-align: center;
  transition: all 0.2s;
}

.btn-add-image:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
}

/* ===== VARIANT IMAGE ===== */
.cell-image { text-align: center; width: 60px; }

.img-upload-label {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.variant-thumb {
  width: 44px;
  height: 44px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.img-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  font-size: 18px;
  background: #f8fafc;
}
</style>