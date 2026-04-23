<script setup>
import { ref, computed, onMounted, watch } from "vue"
import { useRouter } from "vue-router"
import {
  getAllKhachHang,
  deleteKhachHang,
  getHoaDonByKhachHang
} from "../../../services/khachHangService"
import { getDiaChiByKhachHang } from "../../../services/diaChiService"
import { Trash2 } from "lucide-vue-next"
import { getAdminStatusTone, normalizeAdminStatusLabel } from "../../../utils/adminStatus"

const router = useRouter()

const list = ref([])
const loading = ref(false)

const searchText = ref("")
const filterStatus = ref("Tất cả")

const currentPage = ref(1)
const pageSize = 5
const totalPages = ref(1)

const showConfirm = ref(false)
const selectedId = ref(null)

const toList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const formatAddress = (addr) => {
  if (!addr) return ""
  return [addr.diaChiCuThe, addr.phuongXa, addr.quanHuyen, addr.tinhThanh]
    .map((part) => String(part || "").trim())
    .filter(Boolean)
    .join(", ")
}

const normalizeAddressText = (k) => {
  const direct = [k?.diaChiNhanHang, k?.diaChi]
    .map((v) => String(v || "").trim())
    .find(Boolean)
  if (direct) return direct

  const addresses = toList(k?.diaChiList || k?.danhSachDiaChi || k?.diaChis)
  const preferred = addresses.find((item) => item?.macDinh === true || item?.laMacDinh === true) || addresses[0]
  return formatAddress(preferred)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getAllKhachHang(currentPage.value - 1, pageSize)

    list.value = res.data.content || []
    totalPages.value = res.data.totalPages || 1

    for (let k of list.value) {
      try {
        const resOrder = await getHoaDonByKhachHang(k.id)
        k.orderCount = resOrder.data.length
      } catch {
        k.orderCount = 0
      }

      try {
        const resAddress = await getDiaChiByKhachHang(k.id)
        const addressRows = toList(resAddress?.data)
        const preferred = addressRows.find((item) => item?.macDinh === true || item?.laMacDinh === true) || addressRows[0]
        const fullAddress = formatAddress(preferred)
        if (fullAddress) {
          k.diaChiNhanHang = fullAddress
        }
      } catch {
        // Keep fallback on customer payload fields.
      }
    }

  } catch (err) {
    console.error("Load khách hàng failed:", err)
    list.value = []
  }
  loading.value = false
}

onMounted(loadData)

watch(currentPage, () => {
  loadData()
})

const filteredData = computed(() => {
  let data = [...list.value]

  if (searchText.value.trim()) {
    const key = searchText.value.toLowerCase()
    data = data.filter(k =>
      k.tenKhachHang?.toLowerCase().includes(key) ||
      k.soDienThoai?.includes(key)
    )
  }

  if (filterStatus.value !== "Tất cả") {
    data = data.filter(k =>
      normalizeAdminStatusLabel(k.trangThai) === filterStatus.value
    )
  }

  return data
})

const goToEdit = (id) => {
  router.push(`/admin/khach-hang/form/${id}`)
}

const confirmDelete = (id) => {
  selectedId.value = id
  showConfirm.value = true
}

const remove = async () => {
  await deleteKhachHang(selectedId.value)
  showConfirm.value = false
  await loadData()
}
</script>

<template>
  <div class="card">
    <div class="head">
      <div>
        <h1>Khách hàng</h1>
        <small class="muted">
          Quản lý hồ sơ khách
        </small>
      </div>

      <button 
        class="btn primary"
        @click="router.push('/admin/khach-hang/form/new')"
      >
        + Thêm khách hàng
      </button>
    </div>

    <div class="body">

      <div class="toolbar">
        <div class="filters">
          <input
            style="width:320px"
            type="text"
            placeholder="Tìm theo tên / SĐT..."
            v-model="searchText"
          />

          <select
            style="width:220px"
            v-model="filterStatus"
          >
            <option>Tất cả</option>
            <option>Hoạt động</option>
            <option>Đang xử lý</option>
            <option>Ngừng hoạt động</option>
          </select>
        </div>
      </div>

      <table class="table">
        <thead>
          <tr>
            <th style="width:90px">Mã</th>
            <th style="width:220px">Khách hàng</th>
            <th style="width:140px">SĐT</th>
            <th>Địa chỉ</th>
            <th style="width:150px;text-align:center">Trạng thái</th>
            <th style="width:120px;text-align:center">Thao tác</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="kh in filteredData" :key="kh.id">
            <td>{{ kh.maKhachHang }}</td>
            <td><b>{{ kh.tenKhachHang }}</b></td>
            <td>{{ kh.soDienThoai }}</td>
            <td>{{ normalizeAddressText(kh) || 'Chưa cập nhật' }}</td>

            <td style="text-align:center">
              <span
                class="pill"
                :class="`status-${getAdminStatusTone(kh.trangThai)}`"
              >
                ● {{ normalizeAdminStatusLabel(kh.trangThai) }}
              </span>
            </td>

            <td style="text-align:center">
              <div class="actions" style="justify-content:center">
                <button
                  class="iconbtn"
                  @click="goToEdit(kh.id)"
                >
                  <span class="material-icons-outlined">visibility</span>
                </button>

              <button
                class="iconbtn"
                @click="confirmDelete(kh.id)"
              >
                <Trash2 size="16"/>
              </button>
              </div>
            </td>
          </tr>

          <tr v-if="filteredData.length === 0">
            <td colspan="6" style="text-align:center">
              Không có dữ liệu
            </td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <div>
          Trang {{ currentPage }} / {{ totalPages }}
        </div>

        <div class="pager">
          <button
            class="btn"
            :disabled="currentPage === 1"
            @click="currentPage--"
          >
            ←
          </button>

          <span class="chip">
            {{ currentPage }}
          </span>

          <button
            class="btn"
            :disabled="currentPage === totalPages"
            @click="currentPage++"
          >
            →
          </button>
        </div>
      </div>

    </div>

    <div v-if="showConfirm" class="modal-overlay">
      <div class="modal">
        <h3>Xác nhận xoá?</h3>
        <div style="display:flex;gap:10px;margin-top:15px">
          <button class="btn" @click="showConfirm=false">
            Huỷ
          </button>
          <button class="btn danger" @click="remove">
            Xác nhận
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.action-col {
  text-align: center;
  width: 140px;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.iconbtn {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: 0.2s;
}

.iconbtn:hover {
  background: #f3f4f6;
}

.iconbtn:disabled {
  opacity: 1;
  color: #111827;
  cursor: not-allowed;
}

.modal-overlay{
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.35);

  display: flex;
  align-items: center;
  justify-content: center;

  z-index: 1000;
}

.modal{
  background: white;
  padding: 20px 24px;
  border-radius: 12px;
  min-width: 320px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2);
}

@media (max-width: 1024px) {
  .head { flex-direction: column; align-items: flex-start; gap: 12px; }
}
@media (max-width: 768px) {
  .body { overflow-x: auto; }
  table { min-width: 700px; }
  .modal { min-width: auto; width: 90vw; }
}
</style>