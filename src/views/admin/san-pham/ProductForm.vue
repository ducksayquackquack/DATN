<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  createSanPham,
  updateSanPham,
  getSanPhamById,
  getAllSanPham,
  cleanupDeletedVariants
} from "../../../services/sanPhamService"
import { getAllLoai } from "../../../services/loaiService"
import { getAllKichThuoc } from "../../../services/kichThuocService"
import { getAllMauSac } from "../../../services/mauSacService"
import { resolveApiOrigin } from "../../../utils/apiOrigin"
import { clearProductImageOverride, getProductImageConfig, setProductImageConfig } from "../../../utils/productImageOverrides"
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
const id = route.params.id

const routeBase = computed(() => {
  if (route.path.startsWith("/employee/")) return "/employee"
  return "/admin"
})

const backTarget = computed(() => {
  if (String(route.query?.from || "").toLowerCase() === "bien-the") {
    return `${routeBase.value}/san-pham/bien-the`
  }
  return `${routeBase.value}/san-pham/list`
})

const form = reactive({
  sku: "",
  name: "",
  description: "",
  status: "Đang bán",
  giaBan: 0,
  giaGoc: 0,
  ton: 0,
  loaiId: null
})

const loaiOptions = ref([])
const sizeOptions = ref([])
const colorOptions = ref([])
const variantRows = ref([])
const imageCards = ref([])
const originalVariantIds = ref([])

const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, "")
const fallbackImages = [img1, img2, img3, img4, img5, img6, img7, img8, img9, img10, img11]
const fallbackImageSet = new Set(fallbackImages)

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

const createImageCard = (src = "") => ({
  key: `${Date.now()}-${Math.random()}`,
  src: String(src || "").trim()
})

const isImageString = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return false
  if (/^data:image\//i.test(raw)) return true

  const normalized = raw.replace(/\\/g, "/").split(/[?#]/)[0]

  if (/^https?:\/\//i.test(raw)) {
    return /\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i.test(normalized)
  }

  if (normalized.startsWith("/uploads/") || normalized.startsWith("uploads/")) return true
  if (/\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i.test(normalized)) return true

  return false
}

const toImageUrl = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) return ""
  if (/^data:image\//i.test(raw)) return raw

  const normalized = raw.replace(/\\/g, "/")
  const uploadsMatch = normalized.match(/^.*?\/?(uploads\/.*)$/i)

  if (uploadsMatch?.[1]) {
    return `${BACKEND_ORIGIN}/${uploadsMatch[1]}`
  }

  if (/^https?:\/\//i.test(normalized)) return normalized
  if (normalized.startsWith("/")) return normalized
  if (normalized.startsWith("uploads/")) return `${BACKEND_ORIGIN}/${normalized}`
  return normalized.includes("/") ? `/${normalized.replace(/^\/+/, "")}` : normalized
}

const extractImageList = (data) => {
  const bucket = []

  const walk = (entry) => {
    if (!entry) return

    if (typeof entry === "string") {
      const trimmed = entry.trim()
      if (trimmed && isImageString(trimmed)) {
        bucket.push(toImageUrl(trimmed))
      }
      return
    }

    if (Array.isArray(entry)) {
      entry.forEach(walk)
      return
    }

    if (typeof entry === "object") {
      const keys = ["anh", "hinhAnh", "image", "imageUrl", "images", "listAnh", "anhChinh", "duongDanAnh"]
      keys.forEach((key) => walk(entry[key]))
    }
  }

  walk(data)
  return [...new Set(bucket)]
}

const allImageValues = computed(() => {
  const parsed = selectedColors.value
    .map((color) => {
      const key = String(color?.id || "")
      const mappedCardKey = colorImageToCardKey.value[key]
      const mappedCard = imageCards.value.find((item) => item.key === mappedCardKey)
      return String(colorImages.value[key]?.previewUrl || mappedCard?.src || "").trim()
    })
    .filter(Boolean)

  if (parsed.length) return parsed
  if (id) {
    return [fallbackImageFor(Number(route.params.id), form.sku || route.params.id)]
  }

  return []
})

const resolveImageCardSrc = (imageCard, index) => {
  const raw = String(imageCard?.src || "").trim()
  if (raw) return raw
  if (id && index === 0) {
    return fallbackImageFor(Number(route.params.id), form.sku || route.params.id)
  }
  return ""
}

const removeImageAt = (index) => {
  imageCards.value.splice(index, 1)

  if (!imageCards.value.length) {
    imageCards.value.push(createImageCard())
  }
}

const readImageFile = async (file) => {
  if (!file?.type?.startsWith("image/")) return ""

  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ""))
    reader.onerror = () => reject(new Error("Không đọc được ảnh"))
    reader.readAsDataURL(file)
  }).catch(() => "")
}

const appendEmptyImageCard = () => {
  imageCards.value.push(createImageCard())
}

const prioritizeImagesForSave = (images = []) => {
  const normalized = images.map((item) => String(item || "").trim()).filter(Boolean)
  if (!normalized.length) return []

  const nonFallback = normalized.filter((item) => !fallbackImageSet.has(item))
  const fallbackOnly = normalized.filter((item) => fallbackImageSet.has(item))
  return [...nonFallback, ...fallbackOnly]
}

const handleImageCardFileChange = async (index, event) => {
  const files = Array.from(event?.target?.files || [])
  if (!files.length) return

  const next = []
  for (const file of files) {
    const dataUrl = await readImageFile(file)
    if (dataUrl) next.push(dataUrl)
  }

  if (!next.length) {
    window.toast.warning("Không có tệp ảnh hợp lệ")
    return
  }

  const normalizedCurrent = imageCards.value.map((item) => ({ ...item }))

  if (!normalizedCurrent[index]) {
    normalizedCurrent[index] = createImageCard()
  }

  // Chon anh luon thay the card hien tai, khong tao card moi khi chi co 1 file.
  normalizedCurrent[index].src = next[0]

  // Neu nguoi dung chon nhieu anh cung luc, cac anh con lai moi them sau card hien tai.
  if (next.length > 1) {
    const extras = next.slice(1).map((src) => createImageCard(src))
    normalizedCurrent.splice(index + 1, 0, ...extras)
  }

  imageCards.value = normalizedCurrent

  event.target.value = ""
  window.toast.success(`Đã thêm ${next.length} ảnh`)
}

function goBack() {
  router.push(backTarget.value)
}

const splitCsv = (value = "") =>
  String(value)
    .split(",")
    .map((v) => v.trim())
    .filter(Boolean)

const normalizeName = (value = "") =>
  String(value)
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .replace(/đ/g, "d")
    .replace(/Đ/g, "D")
    .trim()
    .toLowerCase()

const getSizeLabel = (sizeId) => {
  return sizeOptions.value.find((item) => Number(item.id) === Number(sizeId))?.name || ""
}

const getColorLabel = (colorId) => {
  return colorOptions.value.find((item) => Number(item.id) === Number(colorId))?.name || ""
}

const colorHexByName = (name = "") => {
  if (/^#([0-9a-f]{3}|[0-9a-f]{6})$/i.test(String(name || "").trim())) return String(name).trim()
  const normalized = normalizeName(name)
  if (normalized.includes("black") || normalized.includes("den")) return "#111827"
  if (normalized.includes("trang") || normalized.includes("kem")) return "#e5dfd0"
  if (normalized.includes("gray") || normalized.includes("grey") || normalized.includes("xam") || normalized.includes("ghi")) return "#8c95a3"
  if (normalized.includes("tim") || normalized.includes("purple")) return "#7c3aed"
  if (normalized.includes("xanh") && normalized.includes("reu")) return "#4f6f52"
  if (normalized.includes("navy")) return "#1e3a8a"
  if (normalized.includes("xanh")) return "#2f4f75"
  if (normalized.includes("red") || normalized.includes("do") || normalized.includes("ruou")) return "#dc2626"
  if (normalized.includes("yellow") || normalized.includes("vang")) return "#d4a017"
  if (normalized.includes("nau")) return "#7a4b2f"
  if (normalized.includes("pink") || normalized.includes("hong")) return "#d684a1"
  return "#9ca3af"
}

const getColorMeta = (colorId) => {
  return colorOptions.value.find((item) => Number(item.id) === Number(colorId)) || null
}

const getSizeMeta = (sizeId) => {
  return sizeOptions.value.find((item) => Number(item.id) === Number(sizeId)) || null
}

const createEmptyVariantRow = () => ({
  key: `${Date.now()}-${Math.random()}`,
  id: null,
  ma: "",
  sizeId: null,
  colorId: null,
  giaNhap: Number(form.giaGoc || 0),
  giaBan: Number(form.giaBan || 0),
  soLuong: Number(form.ton || 0),
  trangThai: "Hoạt động"
})

function addVariantRow(payload = {}) {
  variantRows.value.push({
    ...createEmptyVariantRow(),
    ...payload
  })
}

function removeVariantRow(index) {
  variantRows.value.splice(index, 1)
}

function findOptionByName(options, candidateName) {
  const normalized = normalizeName(candidateName)
  return options.find((item) => normalizeName(item.name) === normalized) || null
}

// ─── Variant Builder: multi-select chip state ────────────────────────────────
const selectedColors = ref([])
const selectedSizes = ref([])
const colorImages = ref({})
const colorImageToCardKey = ref({})
const draggedColorIndex = ref(null)
const dragOverColorIndex = ref(null)
const bulkPanel = ref({ colorId: null, soLuong: '', giaBan: '' })

const groupedVariants = computed(() => {
  const map = {}
  for (const row of variantRows.value) {
    const cid = String(row.colorId ?? '__none__')
    if (!map[cid]) {
      const meta = cid === '__none__' ? null : getColorMeta(row.colorId)
      map[cid] = {
        colorId: row.colorId,
        colorName: meta?.name || '— Chưa phân màu —',
        colorHex: meta?.hex || '#94a3b8',
        items: []
      }
    }
    map[cid].items.push(row)
  }
  return Object.values(map)
})

function toggleColorSelect(color) {
  const idx = selectedColors.value.findIndex((c) => c.id === color.id)
  const hasVariant = variantRows.value.some((row) => Number(row.colorId) === Number(color.id))
  if (idx >= 0 && hasVariant) {
    window.toast.info(`Màu ${color.name} đang được dùng trong biến thể và không thể bỏ chọn`)
    return
  }
  if (idx >= 0) selectedColors.value.splice(idx, 1)
  else selectedColors.value.push(color)
}

function toggleSizeSelect(size) {
  const idx = selectedSizes.value.findIndex((s) => s.id === size.id)
  const hasVariant = variantRows.value.some((row) => Number(row.sizeId) === Number(size.id))
  if (idx >= 0 && hasVariant) {
    window.toast.info(`Kích cỡ ${size.name} đang được dùng trong biến thể và không thể bỏ chọn`)
    return
  }
  if (idx >= 0) selectedSizes.value.splice(idx, 1)
  else selectedSizes.value.push(size)
}

function generateVariantsAuto() {
  if (!selectedColors.value.length || !selectedSizes.value.length) {
    window.toast.warning('Vui lòng chọn ít nhất 1 màu và 1 kích cỡ trước khi tạo biến thể')
    return
  }
  const existingPairs = new Set(
    variantRows.value.map((row) => `${Number(row.sizeId)}-${Number(row.colorId)}`)
  )
  let created = 0
  for (const color of selectedColors.value) {
    for (const size of selectedSizes.value) {
      const key = `${Number(size.id)}-${Number(color.id)}`
      if (existingPairs.has(key)) continue
      existingPairs.add(key)
      addVariantRow({ sizeId: Number(size.id), colorId: Number(color.id) })
      created++
    }
  }
  if (created) window.toast.success(`Đã tạo ${created} biến thể mới`)
  else window.toast.info('Không có biến thể mới (các tổ hợp đã tồn tại)')
}

function addVariantRowForColor(colorId) {
  addVariantRow({ colorId: Number(colorId) })
}

function openBulkPanel(colorId) {
  bulkPanel.value = { colorId, soLuong: '', giaBan: '' }
}

function closeBulkPanel() {
  bulkPanel.value.colorId = null
}

function applyBulkToGroup() {
  const { colorId, soLuong, giaBan } = bulkPanel.value
  let applied = 0
  for (const row of variantRows.value) {
    if (String(row.colorId) === String(colorId)) {
      if (String(soLuong).trim() !== '') row.soLuong = Number(soLuong)
      if (String(giaBan).trim() !== '') row.giaBan = Number(giaBan)
      applied++
    }
  }
  if (applied) window.toast.success(`Đã áp dụng cho ${applied} biến thể`)
  closeBulkPanel()
}

const handleColorImageChange = async (colorId, event) => {
  const file = event?.target?.files?.[0]
  if (!file?.type?.startsWith('image/')) return
  const dataUrl = await readImageFile(file)
  if (dataUrl) {
    const key = String(colorId)
    colorImages.value = { ...colorImages.value, [key]: { previewUrl: dataUrl } }

    const mappedKey = colorImageToCardKey.value[key]
    const foundIndex = imageCards.value.findIndex((item) => item.key === mappedKey)
    if (foundIndex >= 0) {
      imageCards.value[foundIndex].src = dataUrl
    } else {
      const card = createImageCard(dataUrl)
      imageCards.value.push(card)
      colorImageToCardKey.value = { ...colorImageToCardKey.value, [key]: card.key }
    }

    window.toast.success(`Đã thêm ảnh cho màu ${getColorLabel(colorId)}`)
  }
  event.target.value = ''
}

const handleGroupColorImageChange = async (colorId, event) => {
  await handleColorImageChange(colorId, event)
}

const handleColorCardDragStart = (index, event) => {
  draggedColorIndex.value = index
  dragOverColorIndex.value = index
  if (event?.dataTransfer) {
    event.dataTransfer.effectAllowed = "move"
    event.dataTransfer.setData("text/plain", String(index))
  }
}

const handleColorCardDragOver = (index, event) => {
  if (event) event.preventDefault()
  dragOverColorIndex.value = index
}

const handleColorCardDrop = (dropIndex, event) => {
  if (event) event.preventDefault()
  const from = Number(draggedColorIndex.value)
  const to = Number(dropIndex)
  if (!Number.isFinite(from) || !Number.isFinite(to) || from === to) {
    draggedColorIndex.value = null
    dragOverColorIndex.value = null
    return
  }
  const rows = [...selectedColors.value]
  const [picked] = rows.splice(from, 1)
  rows.splice(to, 0, picked)
  selectedColors.value = rows
  draggedColorIndex.value = null
  dragOverColorIndex.value = null
}

const handleColorCardDragEnd = () => {
  draggedColorIndex.value = null
  dragOverColorIndex.value = null
}

const removeColorImage = (colorId) => {
  const key = String(colorId)
  const mappedCardKey = colorImageToCardKey.value[key]
  colorImages.value = { ...colorImages.value, [key]: { previewUrl: "" } }
  if (mappedCardKey) {
    imageCards.value = imageCards.value.filter((item) => item.key !== mappedCardKey)
  }
  const nextMapping = { ...colorImageToCardKey.value }
  delete nextMapping[key]
  colorImageToCardKey.value = nextMapping
}

const hydrateColorImagesFromGallery = (images = []) => {
  const nextCards = []
  const nextMapping = {}
  const nextColorImages = {}

  selectedColors.value.forEach((color, index) => {
    const src = String(images[index] || "").trim()
    if (!src) return
    const card = createImageCard(src)
    nextCards.push(card)
    nextMapping[String(color.id)] = card.key
    nextColorImages[String(color.id)] = { previewUrl: src }
  })

  imageCards.value = nextCards.length ? nextCards : [createImageCard()]
  colorImageToCardKey.value = nextMapping
  colorImages.value = nextColorImages
}

const hydrateColorImagesFromConfig = (entries = [], fallbackImages = []) => {
  const normalizedEntries = (Array.isArray(entries) ? entries : [])
    .map((entry) => ({
      colorId: Number(entry?.colorId),
      image: String(entry?.image || "").trim(),
      order: Number.isFinite(Number(entry?.order)) ? Number(entry.order) : 0
    }))
    .filter((entry) => Number.isFinite(entry.colorId) && entry.colorId > 0 && entry.image)
    .sort((left, right) => left.order - right.order)

  if (!normalizedEntries.length) {
    hydrateColorImagesFromGallery(fallbackImages)
    return
  }

  const nextCards = []
  const nextMapping = {}
  const nextColorImages = {}

  normalizedEntries.forEach((entry) => {
    const card = createImageCard(entry.image)
    nextCards.push(card)
    nextMapping[String(entry.colorId)] = card.key
    nextColorImages[String(entry.colorId)] = { previewUrl: entry.image }
  })

  imageCards.value = nextCards.length ? nextCards : [createImageCard()]
  colorImageToCardKey.value = nextMapping
  colorImages.value = nextColorImages
}

const syncSelectedColorsFromVariants = () => {
  const colorIds = [...new Set(
    variantRows.value
      .map((row) => Number(row.colorId))
      .filter((cid) => Number.isFinite(cid) && cid > 0)
  )]

  selectedColors.value = colorOptions.value.filter((color) => colorIds.includes(Number(color.id)))
}

const syncSelectedSizesFromVariants = () => {
  const sizeIds = [...new Set(
    variantRows.value
      .map((row) => Number(row.sizeId))
      .filter((sid) => Number.isFinite(sid) && sid > 0)
  )]

  selectedSizes.value = sizeOptions.value.filter((size) => sizeIds.includes(Number(size.id)))
}

const clearAllVariants = () => {
  variantRows.value = []
  selectedColors.value = []
  selectedSizes.value = []
  colorImages.value = {}
  colorImageToCardKey.value = {}
  imageCards.value = [createImageCard()]
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

function buildStableVariantCode(row) {
  if (String(row?.ma || "").trim()) return String(row.ma).trim()
  const colorId = Number(row?.colorId || 0)
  const sizeId = Number(row?.sizeId || 0)
  if (!Number.isFinite(colorId) || colorId <= 0 || !Number.isFinite(sizeId) || sizeId <= 0) return ""
  return `${form.sku}-MS${String(colorId).padStart(2, "0")}-KT${String(sizeId).padStart(2, "0")}`
}

const colorImagePayloads = computed(() => {
  return selectedColors.value
    .map((color, index) => ({
      colorId: Number(color.id),
      image: String(colorImages.value[String(color.id)]?.previewUrl || "").trim(),
      order: index
    }))
    .filter((entry) => Number.isFinite(entry.colorId) && entry.colorId > 0 && entry.image)
})

function createVariantPayloads() {
  const parsedLoaiId = Number(form.loaiId)
  return variantRows.value.map((row) => ({
    id: row.id,
    ma: buildStableVariantCode(row),
    giaNhap: Number(row.giaNhap || 0),
    giaBan: Number(row.giaBan || 0),
    soLuong: Number(row.soLuong || 0),
    chatLieu: { id: 1 },
    mauSac: { id: Number(row.colorId) },
    kichThuoc: { id: Number(row.sizeId) },
    hang: { id: 1 },
    xuatSu: { id: 1 },
    danhMuc: { id: 1 },
    loai: { id: parsedLoaiId },
    trangThai: row.trangThai || "Hoạt động"
  }))
}

function validateVariants() {
  if (!variantRows.value.length) return "Sản phẩm phải có ít nhất 1 biến thể"

  const pairSet = new Set()
  for (const row of variantRows.value) {
    const sizeId = Number(row.sizeId)
    const colorId = Number(row.colorId)
    const giaBan = Number(row.giaBan)
    const soLuong = Number(row.soLuong)

    if (!Number.isFinite(sizeId) || sizeId <= 0) return "Vui lòng chọn size cho tất cả biến thể"
    if (!Number.isFinite(colorId) || colorId <= 0) return "Vui lòng chọn màu cho tất cả biến thể"
    if (!Number.isFinite(giaBan) || giaBan <= 0) return "Giá bán biến thể phải lớn hơn 0"
    if (!Number.isFinite(soLuong) || soLuong < 0) return "Tồn kho biến thể không hợp lệ"

    const pairKey = `${sizeId}-${colorId}`
    if (pairSet.has(pairKey)) {
      const sizeName = getSizeLabel(sizeId) || sizeId
      const colorName = getColorLabel(colorId) || colorId
      return `Biến thể size ${sizeName} / màu ${colorName} đang bị trùng`
    }
    pairSet.add(pairKey)
  }

  return ""
}

function validateProduct() {
  if (!String(form.name || "").trim()) return "Tên sản phẩm không được để trống"
  if (!String(form.sku || "").trim()) return "Mã sản phẩm không được để trống"
  if (!/^SP\d+/i.test(String(form.sku || "").trim())) return "Mã sản phẩm phải bắt đầu bằng SP + số (ví dụ: SP001)"
  if (!Number.isFinite(Number(form.loaiId)) || Number(form.loaiId) <= 0) return "Vui lòng chọn loại sản phẩm hợp lệ"

  const variantError = validateVariants()
  if (variantError) return variantError

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

async function loadVariantMasterData() {
  try {
    const [sizesRes, colorsRes] = await Promise.all([getAllKichThuoc(), getAllMauSac()])

    sizeOptions.value = (Array.isArray(sizesRes?.data) ? sizesRes.data : [])
      .map((item) => ({
        id: Number(item?.id),
        name: String(item?.tenKichThuoc || item?.name || "").trim()
      }))
      .filter((item) => Number.isFinite(item.id) && item.id > 0 && item.name)

    colorOptions.value = (Array.isArray(colorsRes?.data) ? colorsRes.data : [])
      .map((item) => ({
        id: Number(item?.id),
        name: String(item?.tenMau || item?.tenMauSac || item?.name || "").trim(),
        hex: colorHexByName(item?.hex || item?.tenMau || item?.tenMauSac || item?.name || item?.maMau || item?.ma || "")
      }))
      .filter((item) => Number.isFinite(item.id) && item.id > 0 && item.name)
  } catch {
    sizeOptions.value = []
    colorOptions.value = []
  }
}

function mapExistingVariants(variants) {
  originalVariantIds.value = variants
    .map((row) => Number(row?.id))
    .filter((id) => Number.isFinite(id) && id > 0)

  variantRows.value = variants.map((row) => ({
    key: `${row.id || Date.now()}-${Math.random()}`,
    id: row.id || null,
    ma: String(row?.ma || "").trim(),
    sizeId: Number(row?.kichThuoc?.id || 0) || null,
    colorId: Number(row?.mauSac?.id || 0) || null,
    giaNhap: Number(row?.giaNhap || 0),
    giaBan: Number(row?.giaBan || 0),
    soLuong: Number(row?.soLuong || 0),
    trangThai: row?.trangThai || "Hoạt động"
  }))
}

watch(
  () => [form.giaBan, form.giaGoc, form.ton],
  ([giaBan, giaGoc, ton]) => {
    if (variantRows.value.length) return
    form.giaBan = Number(giaBan || 0)
    form.giaGoc = Number(giaGoc || 0)
    form.ton = Number(ton || 0)
  }
)

onMounted(async () => {
  await Promise.all([loadLoaiOptions(), loadVariantMasterData()])
  imageCards.value = [createImageCard()]

  if (!id) {
    form.sku = await generateProductCode()
    return
  }

  const res = await getSanPhamById(id)
  const data = res.data || {}
  const variants = Array.isArray(data.sanPhamChiTiets) ? data.sanPhamChiTiets : []
  const firstVariant = variants[0] || null

  form.sku = data.maSanPham || ""
  form.name = data.tenSanPham || ""
  form.description = data.moTa || ""
  form.giaBan = Number(firstVariant?.giaBan || 0)
  form.giaGoc = Number(firstVariant?.giaNhap || 0)
  form.ton = Number(firstVariant?.soLuong || 0)
  form.loaiId = Number(firstVariant?.loai?.id || loaiOptions.value[0]?.id || 1)
  form.status = data.trangThai === "Hoạt động" ? "Đang bán" : "Ẩn"
  const extractedImages = extractImageList([data, variants])
  const imageConfig = getProductImageConfig({ id: data.id, maSanPham: data.maSanPham })
  const overrideImages = imageConfig.images
  const resolvedImages = overrideImages.length ? overrideImages : extractedImages

  if (variants.length) {
    mapExistingVariants(variants)
    syncSelectedColorsFromVariants()
    syncSelectedSizesFromVariants()
    hydrateColorImagesFromConfig(
      imageConfig.colorImages,
      resolvedImages.length ? resolvedImages : [fallbackImageFor(data.id, data.maSanPham)]
    )
  } else {
    imageCards.value = [createImageCard(fallbackImageFor(data.id, data.maSanPham))]
  }
})

async function saveProduct() {
  if (!id && !String(form.status || "").trim()) {
    form.status = "Đang bán"
  }

  const error = validateProduct()
  if (error) {
    window.toast.warning(error)
    return
  }

  const action = id ? "cập nhật" : "tạo mới"
  const confirmed = await window.confirmDialog(`Bạn có chắc chắn muốn ${action} sản phẩm "${form.name}"?`)
  if (!confirmed) return

  try {
    const orderedImages = prioritizeImagesForSave(allImageValues.value)
    const overrideImages = orderedImages.filter((item) => !fallbackImageSet.has(item))
    const hasImageConfig = overrideImages.length > 0 || colorImagePayloads.value.length > 0
    const currentVariantIds = variantRows.value
      .map((row) => Number(row?.id))
      .filter((rowId) => Number.isFinite(rowId) && rowId > 0)
    const deletedVariantIds = originalVariantIds.value.filter((rowId) => !currentVariantIds.includes(rowId))

    let unresolvedDeletedVariantIds = []
    if (id && deletedVariantIds.length) {
      unresolvedDeletedVariantIds = await cleanupDeletedVariants(deletedVariantIds, id)
    }

    const payload = {
      maSanPham: form.sku,
      tenSanPham: form.name,
      moTa: form.description,
      trangThai: form.status === "Đang bán" ? "Hoạt động" : "Ngừng hoạt động",
      anh: orderedImages[0] || "",
      anhChinh: orderedImages[0] || "",
      hinhAnh: orderedImages[0] || "",
      images: orderedImages,
      listAnh: orderedImages,
      mauSacHinhAnhs: colorImagePayloads.value,
      anhTheoMauSac: colorImagePayloads.value,
      colorImages: colorImagePayloads.value,
      sanPhamChiTiets: createVariantPayloads(),
      deletedVariantIds,
      sanPhamChiTietIdsXoa: unresolvedDeletedVariantIds,
      xoaSanPhamChiTietIds: unresolvedDeletedVariantIds
    }

    if (id) {
      await updateSanPham(id, payload)
      if (hasImageConfig) {
        setProductImageConfig({ id, maSanPham: form.sku }, { images: overrideImages, colorImages: colorImagePayloads.value })
      } else {
        clearProductImageOverride({ id, maSanPham: form.sku })
      }
      window.toast.success("Cập nhật sản phẩm thành công")
    } else {
      const response = await createSanPham(payload)
      const savedId = response?.data?.id || response?.data?.data?.id || null
      if (hasImageConfig) {
        setProductImageConfig({ id: savedId, maSanPham: form.sku }, { images: overrideImages, colorImages: colorImagePayloads.value })
      } else {
        clearProductImageOverride({ id: savedId, maSanPham: form.sku })
      }
      window.toast.success("Tạo sản phẩm thành công")
    }

    setTimeout(() => {
      router.push(backTarget.value)
    }, 900)
  } catch (err) {
    console.error("SAVE ERROR:", err)
    window.toast.error("Lưu thất bại: " + (err.response?.data?.message || err.message))
  }
}
</script>

<template>
  <div class="card product-form-page">
    <!-- ── Tiêu đề trang ───────────────────────────────────────────── -->
    <div class="head product-form-header">
      <div>
        <h1>{{ id ? "Sửa sản phẩm" : "Thêm sản phẩm" }}</h1>
        <small class="muted">{{ id ? `Mã: ${form.sku}` : "Mã tự sinh khi lưu" }}</small>
      </div>
      <div class="header-actions">
        <button type="button" class="btn with-icon" @click="goBack">
          <span class="material-icons-outlined btn-icon">arrow_back</span>
          Quay lại
        </button>
        <button class="btn primary with-icon" @click.prevent="saveProduct">
          <span class="material-icons-outlined btn-icon">save</span>
          Lưu sản phẩm
        </button>
      </div>
    </div>

    <div class="body product-form-body">

      <!-- ── TOP: 2 cột (Thông tin | Cấu hình biến thể) ────────────── -->
      <div class="top-grid">

        <!-- Cột trái: Thông tin sản phẩm -->
        <section class="product-panel">
          <div class="panel-head">
            <h2>Thông tin sản phẩm</h2>
            <p>Điền thông tin cơ bản. Mã sản phẩm được tự sinh.</p>
          </div>

          <div class="info-fields">
            <div class="field">
              <label>Mã sản phẩm</label>
              <input type="text" readonly :value="form.sku || 'Mã tự sinh'" class="auto-code-input" />
            </div>

            <div class="field">
              <label>Tên sản phẩm</label>
              <input v-model="form.name" type="text" placeholder="VD: Áo hoodie kéo khoá DirtyWave" />
            </div>

            <div class="field">
              <label>Loại sản phẩm</label>
              <select v-model.number="form.loaiId">
                <option v-for="loaiOption in loaiOptions" :key="loaiOption.id" :value="loaiOption.id">
                  {{ loaiOption.name }}
                </option>
              </select>
            </div>

            <div class="field">
              <label>Trạng thái</label>
              <select v-model="form.status">
                <option>Đang bán</option>
                <option>Ẩn</option>
              </select>
            </div>

            <div class="field">
              <label>Giá bán gợi ý cho biến thể mới</label>
              <input v-model.number="form.giaBan" type="number" min="0" placeholder="329000" :disabled="variantRows.length > 0" />
            </div>

            <div class="field">
              <label>Giá nhập gợi ý cho biến thể mới</label>
              <input v-model.number="form.giaGoc" type="number" min="0" placeholder="289000" :disabled="variantRows.length > 0" />
            </div>

            <div class="field">
              <label>Tồn kho gợi ý cho biến thể mới</label>
              <input v-model.number="form.ton" type="number" min="0" placeholder="0" :disabled="variantRows.length > 0" />
            </div>

            <div class="field field-mota">
              <label>Mô tả</label>
              <textarea v-model="form.description" placeholder="Mô tả ngắn gọn về chất liệu, form, cách sử dụng..."></textarea>
            </div>
          </div>
        </section>

        <!-- Cột phải: Cấu hình biến thể -->
        <section class="product-panel variant-builder-panel">
          <div class="panel-head">
            <h2>Cấu hình biến thể</h2>
            <p>Chọn màu và cỡ, tạo biến thể tự động theo tổ hợp.</p>
          </div>

          <!-- Màu sắc -->
          <div class="builder-section">
            <div class="builder-label-row">
              <span class="builder-label">Màu sắc</span>
              <span class="builder-count">{{ selectedColors.length }} đã chọn</span>
            </div>
            <div class="color-chips-wrap">
              <button
                v-for="color in colorOptions"
                :key="color.id"
                type="button"
                class="color-chip-btn"
                :class="{ active: selectedColors.some((c) => c.id === color.id) }"
                @click="toggleColorSelect(color)"
              >
                <span class="color-dot" :style="{ background: color.hex }"></span>
                <span>{{ color.name }}</span>
              </button>
            </div>
          </div>

          <!-- Kích cỡ -->
          <div class="builder-section">
            <div class="builder-label-row">
              <span class="builder-label">Kích cỡ</span>
              <span class="builder-count">{{ selectedSizes.length }} đã chọn</span>
            </div>
            <div class="size-chips-wrap">
              <button
                v-for="size in sizeOptions"
                :key="size.id"
                type="button"
                class="size-chip-btn"
                :class="{ active: selectedSizes.some((s) => s.id === size.id) }"
                @click="toggleSizeSelect(size)"
              >
                {{ size.name }}
              </button>
            </div>
          </div>

          <!-- Nút tạo biến thể -->
          <div class="builder-section builder-footer">
            <button
              type="button"
              class="btn primary btn-generate with-icon"
              :disabled="!selectedColors.length || !selectedSizes.length"
              @click="generateVariantsAuto"
            >
              <span class="material-icons-outlined btn-icon">auto_awesome</span>
              Tạo biến thể tự động
              <span v-if="selectedColors.length && selectedSizes.length" class="combo-count">
                ({{ selectedColors.length * selectedSizes.length }} tổ hợp)
              </span>
            </button>
            <button type="button" class="btn btn-generate with-icon" @click="addVariantRow()">
              <span class="material-icons-outlined btn-icon">add</span>
              Thêm thủ công
            </button>
          </div>
        </section>
      </div>
      <!-- end top-grid -->

      <!-- ── GIỮA: Danh sách biến thể (nhóm theo màu) ───────────────── -->
      <section class="product-panel variant-section">
        <div class="panel-head panel-head-inline">
          <div>
            <h2>Danh sách biến thể</h2>
            <p class="muted">{{ variantRows.length }} biến thể</p>
          </div>
          <button
            v-if="variantRows.length"
            type="button"
            class="btn danger btn-small"
            @click="clearAllVariants()"
          >
            Xóa tất cả
          </button>
        </div>

        <!-- Empty state -->
        <div v-if="!variantRows.length" class="variant-empty-state">
          <p>Chưa có biến thể. Chọn màu + cỡ bên trên rồi nhấn <strong>Tạo biến thể tự động</strong>.</p>
        </div>

        <!-- Grouped by color -->
        <div v-else class="variant-groups">
          <div v-for="group in groupedVariants" :key="String(group.colorId)" class="variant-group">
            <!-- Group header -->
            <div class="group-header">
              <div class="group-header-left">
                <span class="color-dot lg" :style="{ background: group.colorHex }"></span>
                <strong>{{ group.colorName }}</strong>
                <span class="group-count">{{ group.items.length }} cỡ</span>
              </div>
              <div class="group-header-right">
                <template v-if="bulkPanel.colorId === group.colorId">
                  <input
                    v-model.number="bulkPanel.soLuong"
                    type="number"
                    placeholder="Số lượng"
                    min="0"
                    class="bulk-input"
                  />
                  <input
                    v-model.number="bulkPanel.giaBan"
                    type="number"
                    placeholder="Giá bán"
                    min="0"
                    class="bulk-input"
                  />
                  <button type="button" class="btn primary btn-small" @click="applyBulkToGroup">Áp dụng</button>
                  <button type="button" class="btn btn-small" @click="closeBulkPanel">Bỏ</button>
                </template>
                <template v-else>
                  <template v-if="group.colorId">
                    <input
                      :id="`group-color-img-${group.colorId}`"
                      class="image-file-input"
                      type="file"
                      accept="image/*"
                      @change="handleGroupColorImageChange(group.colorId, $event)"
                    />
                    <label class="btn btn-small group-action-btn" :for="`group-color-img-${group.colorId}`">Ảnh màu</label>
                  </template>
                    <button type="button" class="btn btn-small with-icon group-action-btn" @click="openBulkPanel(group.colorId)">
                      <span class="material-icons-outlined btn-icon">bolt</span>
                      Thêm nhanh
                  </button>
                    <button type="button" class="btn btn-small with-icon group-action-btn" @click="addVariantRowForColor(group.colorId)">
                      <span class="material-icons-outlined btn-icon">add</span>
                      Thêm cỡ
                  </button>
                </template>
              </div>
            </div>

            <!-- Sub-table -->
            <div class="sub-table-wrap">
              <table class="variant-table">
                <thead>
                  <tr>
                    <th>Kích cỡ</th>
                    <th>Giá nhập</th>
                    <th>Giá bán</th>
                    <th>Tồn kho</th>
                    <th>Trạng thái</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="variant in group.items" :key="variant.key">
                    <td>
                      <select v-model.number="variant.sizeId">
                        <option :value="null">Chọn size</option>
                        <option v-for="size in sizeOptions" :key="size.id" :value="size.id">
                          {{ size.name }}
                        </option>
                      </select>
                    </td>
                    <td><input v-model.number="variant.giaNhap" type="number" min="0" /></td>
                    <td><input v-model.number="variant.giaBan" type="number" min="0" /></td>
                    <td><input v-model.number="variant.soLuong" type="number" min="0" /></td>
                    <td>
                      <select v-model="variant.trangThai">
                        <option>Hoạt động</option>
                        <option>Ngừng hoạt động</option>
                      </select>
                    </td>
                    <td>
                      <button
                        type="button"
                        class="btn danger btn-small"
                        @click="removeVariantRow(variantRows.indexOf(variant))"
                      >
                        Xóa
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </section>

      <!-- ── DƯỚI: Ảnh theo màu sắc ──────────────────────────────────── -->
      <section v-if="selectedColors.length" class="product-panel color-images-section standalone">
          <transition-group name="color-image" tag="div" class="color-images-grid">
            <div
              v-for="(color, index) in selectedColors"
              :key="color.id"
              class="color-image-card"
              :class="{ 'is-drag-over': dragOverColorIndex === index }"
              draggable="true"
              @dragstart="handleColorCardDragStart(index, $event)"
              @dragover="handleColorCardDragOver(index, $event)"
              @drop="handleColorCardDrop(index, $event)"
              @dragend="handleColorCardDragEnd"
            >
              <div class="color-image-header">
                <span class="color-dot" :style="{ background: color.hex }"></span>
                <span>{{ color.name }}</span>
              </div>
              <button
                v-if="colorImages[String(color.id)]?.previewUrl"
                type="button"
                class="image-remove-icon"
                aria-label="Xóa ảnh màu"
                @click="removeColorImage(color.id)"
              >
                ×
              </button>
              <div class="image-card-media">
                <img
                  v-if="colorImages[String(color.id)]?.previewUrl"
                  :src="colorImages[String(color.id)].previewUrl"
                  alt=""
                />
                <div v-else class="image-placeholder">Chưa có ảnh</div>
              </div>
              <input
                :id="`color-img-${color.id}`"
                class="image-file-input"
                type="file"
                accept="image/*"
                @change="handleColorImageChange(color.id, $event)"
              />
              <div class="image-card-actions">
                <span class="image-order">#{{ index + 1 }}</span>
                <span class="image-drag-hint">Kéo thả để đổi thứ tự</span>
                <label class="btn image-select-btn" :for="`color-img-${color.id}`">Chọn ảnh</label>
              </div>
            </div>
          </transition-group>
      </section>

    </div>
  </div>
</template>

<style scoped>
/* ── Layout ────────────────────────────────────────────────────────── */
.product-form-page {
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.product-form-header {
  align-items: flex-start;
  gap: 16px;
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
  display: block;
}

.product-form-body {
  display: grid;
  gap: 20px;
}

/* TOP 2-column grid */
.top-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
  align-items: start;
}

/* ── Card panel ────────────────────────────────────────────────────── */
.product-panel {
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  padding: 20px;
  background: #ffffff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.panel-head {
  margin-bottom: 18px;
}

.panel-head h2 {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
}

.panel-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 14px;
}

.panel-head-inline {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

/* ── Thông tin sản phẩm ────────────────────────────────────────────── */
.info-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.field-mota {
  grid-column: 1 / -1;
}

/* ── Inputs ────────────────────────────────────────────────────────── */
input,
select,
textarea {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d8dee9;
  background: #fff;
  font-size: 14px;
  box-sizing: border-box;
}

input:focus,
select:focus,
textarea:focus {
  outline: none;
  border-color: #c5162d;
}

textarea {
  min-height: 100px;
  resize: vertical;
}

.auto-code-input {
  background: #f1f5f9;
  color: #64748b;
  border-color: #cbd5e1;
  font-weight: 600;
}

/* ── Variant builder (right panel) ─────────────────────────────────── */
.variant-builder-panel {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.builder-section {
  padding: 14px 0;
  border-bottom: 1px solid #f1f5f9;
}

.builder-section:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.builder-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.builder-label {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.builder-count {
  font-size: 12px;
  color: #64748b;
}

/* Color chip multi-select */
.color-chips-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.color-chip-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
  color: #374151;
  transition: border-color 0.2s, background 0.2s, color 0.2s;
}

.color-chip-btn:hover {
  border-color: #c5162d;
}

.color-chip-btn.active {
  border-color: #c5162d;
  background: #fff1f2;
  color: #c5162d;
  font-weight: 600;
}

.color-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid rgba(15, 23, 42, 0.15);
  flex-shrink: 0;
}

.color-dot.lg {
  width: 18px;
  height: 18px;
}

/* Size chip selector */
.size-chips-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.size-chip-btn {
  padding: 6px 14px;
  border-radius: 8px;
  border: 1.5px solid #e2e8f0;
  background: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  color: #374151;
  transition: border-color 0.2s, background 0.2s, color 0.2s;
  min-width: 42px;
  text-align: center;
}

.size-chip-btn:hover {
  border-color: #c5162d;
}

.size-chip-btn.active {
  border-color: #c5162d;
  background: #c5162d;
  color: #fff;
}

/* Generate buttons */
.builder-footer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 14px;
}

.btn-generate {
  width: 100%;
  justify-content: center;
}

.combo-count {
  font-size: 12px;
  opacity: 0.85;
  margin-left: 4px;
}

/* ── Variant section ────────────────────────────────────────────────── */
.variant-section .muted {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.variant-empty-state {
  text-align: center;
  padding: 32px 16px;
  color: #64748b;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
}

.variant-groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.variant-group {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  gap: 12px;
  flex-wrap: wrap;
}

.group-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.group-header-right {
  display: flex;
  align-items: stretch;
  gap: 8px;
  flex-wrap: wrap;
}

.group-action-btn {
  min-height: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  line-height: 1;
}

.group-count {
  font-size: 12px;
  color: #64748b;
  background: #e2e8f0;
  padding: 2px 8px;
  border-radius: 999px;
}

.bulk-input {
  width: 110px !important;
  padding: 6px 10px !important;
  font-size: 13px !important;
}

.sub-table-wrap {
  overflow-x: auto;
}

.variant-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 600px;
}

.variant-table th,
.variant-table td {
  border-bottom: 1px solid #f1f5f9;
  padding: 9px 10px;
  text-align: left;
}

.variant-table thead th {
  background: #fafafa;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.variant-table tbody tr:last-child td {
  border-bottom: none;
}

.variant-table td input,
.variant-table td select {
  padding: 7px 10px;
  font-size: 13px;
}

/* ── Images ─────────────────────────────────────────────────────────── */
.image-file-input {
  display: none;
}

.image-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}

.image-preview-grid.large {
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.image-preview-card {
  border: 1px solid #dbe4f0;
  border-radius: 16px;
  padding: 12px;
  background: #fff;
  display: grid;
  gap: 12px;
  position: relative;
  transition: border-color 0.2s ease;
}

.image-preview-card.is-empty {
  border-style: dashed;
}

.image-preview-card.is-drag-over {
  border-color: #c5162d;
  box-shadow: 0 0 0 2px rgba(197, 22, 45, 0.18);
}

.image-card-media {
  width: 100%;
  aspect-ratio: 3 / 4;
  border-radius: 12px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
}

.image-preview-card img,
.color-image-card .image-card-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #94a3b8;
  padding: 16px;
  font-size: 13px;
}

.image-card-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.image-order {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  padding: 4px 10px;
}

.image-drag-hint {
  font-size: 12px;
  color: #64748b;
}

.image-select-btn {
  min-width: 110px;
  text-align: center;
}

.image-remove-icon {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.56);
  color: #fff;
  font-size: 20px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s, background 0.2s;
}

.image-preview-card:hover .image-remove-icon,
.color-image-card:hover .image-remove-icon {
  opacity: 1;
}

.image-remove-icon:hover {
  background: #dc2626;
}

/* Per-color images */
.color-images-section {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid #f1f5f9;
}

.color-images-section.standalone {
  margin-top: 0;
  padding-top: 20px;
  border-top: 0;
}

.color-images-heading {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.color-images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.color-image-card {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 14px;
  display: grid;
  gap: 12px;
  background: #fff;
  position: relative;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.24s ease;
}

.color-image-card.is-drag-over {
  border-color: #c5162d;
  box-shadow: 0 0 0 2px rgba(197, 22, 45, 0.18);
}

.color-image-card .image-card-media {
  aspect-ratio: 3 / 4;
}

.color-image-card .image-card-media img {
  object-fit: contain;
  background: #f8fafc;
}

.color-image-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.color-image-move {
  transition: transform 0.24s ease, opacity 0.24s ease;
}

.color-image-enter-active,
.color-image-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.color-image-enter-from,
.color-image-leave-to {
  opacity: 0;
  transform: scale(0.98);
}

/* ── Utility ─────────────────────────────────────────────────────────── */
.btn.danger {
  border-color: #fecaca;
  color: #b91c1c;
  background: #fff1f2;
}

.btn-small {
  padding: 7px 12px;
  font-size: 13px;
}

/* ── Responsive ──────────────────────────────────────────────────────── */
@media (max-width: 900px) {
  .top-grid {
    grid-template-columns: 1fr;
  }
  .info-fields {
    grid-template-columns: 1fr;
  }
  .field-mota {
    grid-column: 1;
  }
}

@media (max-width: 600px) {
  .panel-head-inline {
    flex-direction: column;
    align-items: stretch;
  }
  .group-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>