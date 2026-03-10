<script setup>
import { ref, onMounted, computed } from "vue"
import { useRoute, useRouter } from "vue-router"
import {
  getHoaDonById,
  getHoaDonItems,
  getHoaDonHistory,
  confirmHoaDon,
  shipHoaDon,
  completeHoaDon,
  cancelHoaDon,
  failDeliveryHoaDon,
  returnedHoaDon,
  searchSanPham,
  getSizesByProduct,
  getColorsByProductAndSize,
  addItemByVariant,
  updateItemQty,
  deleteItem
} from "../../../services/hoaDonService"

const route = useRoute()
const router = useRouter()

const id = route.params.id

const loading = ref(false)

const hoaDon = ref(null)
const items = ref([])
const history = ref([])

const searchText = ref("")
const suggestList = ref([])

const selectedProductId = ref("")
const selectedSizeId = ref("")
const selectedColorId = ref("")
const addQuantity = ref(1)

const sizeOptions = ref([])
const colorOptions = ref([])

const cancelReason = ref("")
const failNote = ref("")
const returnedNote = ref("")

const qtyMap = ref({})

const statusCode = computed(() => {
  return (
    hoaDon.value?.trangThai ||
    hoaDon.value?.statusCode ||
    hoaDon.value?.orderStatusCode ||
    ""
  )
})

const statusName = computed(() => {
  const fromApi =
    hoaDon.value?.trangThaiName ||
    hoaDon.value?.orderStatusName ||
    ""

  if (fromApi) return fromApi

  const map = {
    CHO_XAC_NHAN: "Chờ xác nhận",
    CHO_THANH_TOAN: "Chờ thanh toán",
    CHO_LAY_HANG: "Chờ lấy hàng",
    CHO_GIAO: "Chờ giao",
    DANG_GIAO: "Đang giao",
    HOAN_THANH: "Hoàn thành",
    DA_HOAN_THANH: "Hoàn thành",
    HUY: "Huỷ",
    DA_HUY: "Huỷ",
    HOAN_VE: "Hoàn về",
    GIAO_THAT_BAI: "Giao thất bại"
  }

  return map[statusCode.value] || statusCode.value
})

const finalOrder = computed(() => {
  return ["HOAN_THANH", "DA_HOAN_THANH", "HUY", "DA_HUY", "HOAN_VE"].includes(statusCode.value)
})

const formatCurrency = (value) => {
  return new Intl.NumberFormat("vi-VN").format(value || 0) + "₫"
}

const formatDate = (value) => {
  if (!value) return ""
  return new Date(value).toLocaleString("vi-VN")
}

const resetVariantSelect = () => {
  selectedSizeId.value = ""
  selectedColorId.value = ""
  sizeOptions.value = []
  colorOptions.value = []
}

const syncQtyMap = () => {
  const next = {}
  items.value.forEach((it) => {
    next[it.id] = it.soLuong
  })
  qtyMap.value = next
}

async function loadData() {
  try {
    loading.value = true

    const [hoaDonRes, itemRes, historyRes] = await Promise.all([
      getHoaDonById(id),
      getHoaDonItems(id),
      getHoaDonHistory(id)
    ])

    hoaDon.value = hoaDonRes?.data || null
    items.value = Array.isArray(itemRes?.data) ? itemRes.data : []
    history.value = Array.isArray(historyRes?.data) ? historyRes.data : []

    syncQtyMap()
  } catch (e) {
    console.error("Lỗi load chi tiết hóa đơn:", e)
    alert("Không tải được chi tiết hóa đơn")
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

let debounceTimer = null

const handleSearchProduct = () => {
  clearTimeout(debounceTimer)

  const q = searchText.value.trim()

  if (!q) {
    suggestList.value = []
    selectedProductId.value = ""
    resetVariantSelect()
    return
  }

  debounceTimer = setTimeout(async () => {
    try {
      const res = await searchSanPham(q)
      suggestList.value = Array.isArray(res?.data) ? res.data : []
    } catch (e) {
      console.error("Lỗi tìm sản phẩm:", e)
      suggestList.value = []
    }
  }, 200)
}

const selectProduct = async (p) => {
  searchText.value = p.name || p.tenSanPham || ""
  selectedProductId.value = p.id
  suggestList.value = []

  resetVariantSelect()

  try {
    const res = await getSizesByProduct(p.id)
    sizeOptions.value = Array.isArray(res?.data) ? res.data : []
  } catch (e) {
    console.error("Lỗi load size:", e)
    sizeOptions.value = []
  }
}

const handleChangeSize = async () => {
  selectedColorId.value = ""
  colorOptions.value = []

  if (!selectedProductId.value || !selectedSizeId.value) return

  try {
    const res = await getColorsByProductAndSize(selectedProductId.value, selectedSizeId.value)
    colorOptions.value = Array.isArray(res?.data) ? res.data : []
  } catch (e) {
    console.error("Lỗi load màu:", e)
    colorOptions.value = []
  }
}

const handleAddItem = async () => {
  try {
    if (!selectedProductId.value || !selectedSizeId.value || !selectedColorId.value) {
      alert("Chọn đủ Sản phẩm / Size / Màu trước khi thêm")
      return
    }

    await addItemByVariant(id, {
      productId: selectedProductId.value,
      sizeId: selectedSizeId.value,
      colorId: selectedColorId.value,
      soLuong: addQuantity.value
    })

    searchText.value = ""
    suggestList.value = []
    selectedProductId.value = ""
    selectedSizeId.value = ""
    selectedColorId.value = ""
    sizeOptions.value = []
    colorOptions.value = []
    addQuantity.value = 1

    await loadData()
  } catch (e) {
    console.error("Lỗi thêm sản phẩm:", e)
    alert("Thêm sản phẩm thất bại")
  }
}

const handleUpdateQty = async (itemId) => {
  try {
    await updateItemQty(id, itemId, qtyMap.value[itemId])
    await loadData()
  } catch (e) {
    console.error("Lỗi cập nhật số lượng:", e)
    alert("Cập nhật số lượng thất bại")
  }
}

const handleDeleteItem = async (itemId) => {
  const ok = window.confirm("Xóa sản phẩm này khỏi hóa đơn?")
  if (!ok) return

  try {
    await deleteItem(id, itemId)
    await loadData()
  } catch (e) {
    console.error("Lỗi xoá sản phẩm:", e)
    alert("Xóa sản phẩm thất bại")
  }
}

const handleConfirm = async () => {
  try {
    await confirmHoaDon(id)
    await loadData()
  } catch (e) {
    console.error("Lỗi xác nhận đơn:", e)
    alert("Xác nhận đơn thất bại")
  }
}

const handleShip = async () => {
  try {
    await shipHoaDon(id)
    await loadData()
  } catch (e) {
    console.error("Lỗi bắt đầu giao:", e)
    alert("Cập nhật trạng thái thất bại")
  }
}

const handleComplete = async () => {
  try {
    await completeHoaDon(id)
    await loadData()
  } catch (e) {
    console.error("Lỗi hoàn thành đơn:", e)
    alert("Cập nhật trạng thái thất bại")
  }
}

const handleCancel = async () => {
  if (!cancelReason.value.trim()) {
    alert("Vui lòng nhập lý do huỷ")
    return
  }

  try {
    await cancelHoaDon(id, cancelReason.value.trim())
    cancelReason.value = ""
    await loadData()
  } catch (e) {
    console.error("Lỗi huỷ đơn:", e)
    alert("Huỷ đơn thất bại")
  }
}

const handleFailDelivery = async () => {
  try {
    await failDeliveryHoaDon(id, failNote.value.trim())
    failNote.value = ""
    await loadData()
  } catch (e) {
    console.error("Lỗi giao thất bại:", e)
    alert("Cập nhật trạng thái thất bại")
  }
}

const handleReturned = async () => {
  try {
    await returnedHoaDon(id, returnedNote.value.trim())
    returnedNote.value = ""
    await loadData()
  } catch (e) {
    console.error("Lỗi hoàn về:", e)
    alert("Cập nhật trạng thái thất bại")
  }
}
</script>

<template>
  <main class="wrap" v-if="hoaDon">
    <div class="card">
      <div class="head">
        <h1>🧾 Chi tiết hóa đơn</h1>

        <button class="btn" @click="router.push('/admin/hoa-don/list')">
          ← Quay lại
        </button>
      </div>

      <div class="body">
        <div v-if="loading" class="note">
          Đang tải dữ liệu...
        </div>

        <template v-else>
          <div class="summary">
            <div><b>ID:</b> {{ hoaDon.id }}</div>
            <div><b>Mã HĐ:</b> {{ hoaDon.maHoaDon }}</div>
            <div><b>Trạng thái:</b> {{ statusName }}</div>
            <div><b>Phí ship:</b> {{ formatCurrency(hoaDon.phiShip) }}</div>
            <div><b>Thành tiền:</b> {{ formatCurrency(hoaDon.thanhTien) }}</div>
          </div>

          <div v-if="finalOrder" class="note danger">
            Hóa đơn đã kết thúc (final) nên không thể chỉnh sửa sản phẩm.
          </div>

          <hr class="sep" />

          <h2>⚙️ Cập nhật trạng thái</h2>

          <div v-if="finalOrder" class="note danger">
            Đơn đã kết thúc nên không được cập nhật trạng thái.
          </div>

          <div v-else class="status-actions">
            <button
              v-if="statusCode === 'CHO_XAC_NHAN'"
              class="btn"
              @click="handleConfirm"
            >
              ✅ Xác nhận
            </button>

            <button
              v-if="statusCode === 'CHO_LAY_HANG'"
              class="btn"
              @click="handleShip"
            >
              🚚 Bắt đầu giao
            </button>

            <button
              v-if="statusCode === 'DANG_GIAO'"
              class="btn"
              @click="handleComplete"
            >
              🏁 Hoàn thành
            </button>

            <div
              v-if="['CHO_XAC_NHAN', 'CHO_LAY_HANG'].includes(statusCode)"
              class="inline-form"
            >
              <input
                v-model="cancelReason"
                class="inline-input"
                type="text"
                placeholder="Lý do huỷ"
              />
              <button class="btn danger" @click="handleCancel">
                🛑 Huỷ
              </button>
            </div>

            <div v-if="statusCode === 'DANG_GIAO'" class="inline-form">
              <input
                v-model="failNote"
                class="inline-input"
                type="text"
                placeholder="Lý do giao thất bại"
              />
              <button class="btn danger" @click="handleFailDelivery">
                ❌ Giao thất bại
              </button>
            </div>

            <div v-if="statusCode === 'GIAO_THAT_BAI'" class="inline-form">
              <input
                v-model="returnedNote"
                class="inline-input"
                type="text"
                placeholder="Ghi chú hoàn về"
              />
              <button class="btn" @click="handleReturned">
                📦 Hoàn về
              </button>
            </div>
          </div>

          <div v-if="!finalOrder">
            <hr class="sep" />
            <h2>➕ Thêm sản phẩm</h2>

            <div class="search-box">
              <input
                v-model="searchText"
                class="search-input"
                type="text"
                placeholder="Nhập tên sản phẩm..."
                autocomplete="off"
                @input="handleSearchProduct"
              />

              <div v-if="suggestList.length" class="suggest-box">
                <div
                  v-for="sp in suggestList"
                  :key="sp.id"
                  class="suggest-item"
                  @click="selectProduct(sp)"
                >
                  {{ sp.name || sp.tenSanPham }}
                </div>
              </div>
            </div>

            <div class="variant-row">
              <div class="field">
                <label>Size</label>
                <select v-model="selectedSizeId" @change="handleChangeSize">
                  <option value="">-- Chọn size --</option>
                  <option v-for="s in sizeOptions" :key="s.id" :value="s.id">
                    {{ s.name || s.tenSize }}
                  </option>
                </select>
              </div>

              <div class="field">
                <label>Màu</label>
                <select v-model="selectedColorId">
                  <option value="">-- Chọn màu --</option>
                  <option v-for="c in colorOptions" :key="c.id" :value="c.id">
                    {{ c.name || c.tenMau }}
                  </option>
                </select>
              </div>

              <div class="field qty-field">
                <label>Số lượng</label>
                <input v-model.number="addQuantity" type="number" min="1" />
              </div>

              <div class="field action-field">
                <label>&nbsp;</label>
                <button class="btn primary" @click="handleAddItem">
                  Thêm
                </button>
              </div>
            </div>
          </div>

          <hr class="sep" />

          <h2>📦 Sản phẩm trong hóa đơn</h2>

          <table class="table">
            <thead>
              <tr>
                <th>ID SPCT</th>
                <th>Số lượng</th>
                <th>Thành tiền</th>
                <th>Trạng thái</th>
                <th style="width:170px">Hành động</th>
              </tr>
            </thead>

            <tbody>
              <tr v-if="items.length === 0">
                <td colspan="5" style="text-align:center">
                  Chưa có sản phẩm
                </td>
              </tr>

              <tr v-for="it in items" :key="it.id">
                <td>{{ it?.sanPhamChiTiet?.id || "N/A" }}</td>

                <td>
                  <span v-if="finalOrder">{{ it.soLuong }}</span>

                  <div v-else class="inline-form">
                    <input
                      v-model.number="qtyMap[it.id]"
                      class="small-input"
                      type="number"
                      min="1"
                    />
                    <button class="btn" @click="handleUpdateQty(it.id)">
                      Cập nhật
                    </button>
                  </div>
                </td>

                <td>{{ formatCurrency(it.thanhTien) }}</td>
                <td>{{ it.trangThai }}</td>

                <td>
                  <span v-if="finalOrder">-</span>

                  <button
                    v-else
                    class="btn danger"
                    @click="handleDeleteItem(it.id)"
                  >
                    🗑 Xóa
                  </button>
                </td>
              </tr>
            </tbody>
          </table>

          <hr class="sep" />

          <h2>🕒 Lịch sử trạng thái</h2>

          <table class="table">
            <thead>
              <tr>
                <th>Thời gian</th>
                <th>Từ</th>
                <th>Đến</th>
                <th>Ghi chú</th>
              </tr>
            </thead>

            <tbody>
              <tr v-if="history.length === 0">
                <td colspan="4" style="text-align:center">
                  Chưa có lịch sử
                </td>
              </tr>

              <tr v-for="h in history" :key="`${h.changedAt}-${h.toStatus}`">
                <td>{{ formatDate(h.changedAt) }}</td>
                <td>{{ h.fromStatus }}</td>
                <td>{{ h.toStatus }}</td>
                <td>{{ h.note }}</td>
              </tr>
            </tbody>
          </table>
        </template>
      </div>
    </div>
  </main>
</template>

<style scoped>
.summary{
  display:grid;
  gap:8px;
}

.sep{
  margin:18px 0;
  border:none;
  border-top:1px solid #e5e7eb;
}

.note{
  margin-top:12px;
  padding:10px 12px;
  border-radius:8px;
  background:#f3f4f6;
}

.note.danger{
  background:#fef2f2;
  color:#b91c1c;
}

.status-actions{
  display:flex;
  flex-wrap:wrap;
  gap:10px;
  align-items:center;
}

.inline-form{
  display:flex;
  gap:8px;
  align-items:center;
  flex-wrap:wrap;
}

.inline-input{
  width:260px;
  padding:8px 10px;
  border:1px solid #d8dee4;
  border-radius:8px;
}

.search-box{
  position:relative;
  width:360px;
  max-width:100%;
}

.search-input{
  width:100%;
  padding:10px 12px;
  border:1px solid #d8dee4;
  border-radius:8px;
}

.suggest-box{
  position:absolute;
  top:100%;
  left:0;
  right:0;
  background:white;
  border:1px solid #d8dee4;
  border-radius:8px;
  max-height:200px;
  overflow:auto;
  z-index:20;
  margin-top:4px;
}

.suggest-item{
  padding:8px 10px;
  cursor:pointer;
}

.suggest-item:hover{
  background:#f3f4f6;
}

.variant-row{
  display:flex;
  gap:12px;
  flex-wrap:wrap;
  margin-top:12px;
  align-items:flex-end;
}

.field{
  display:flex;
  flex-direction:column;
  gap:6px;
}

.field select,
.field input{
  min-width:180px;
  padding:10px 12px;
  border:1px solid #d8dee4;
  border-radius:8px;
}

.qty-field input{
  min-width:110px;
}

.action-field .btn{
  height:42px;
}

.small-input{
  width:110px;
  padding:8px 10px;
  border:1px solid #d8dee4;
  border-radius:8px;
}
</style>