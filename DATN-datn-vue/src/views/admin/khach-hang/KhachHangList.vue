<script setup>
import { ref, computed, onMounted, watch } from "vue"
import { useRouter } from "vue-router"
import {
  getAllKhachHang,
  deleteKhachHang,
  getHoaDonByKhachHang
} from "../../../services/khachHangService"
import { Pencil, Trash2 } from "lucide-vue-next"
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
        @click="router.push('/admin/khach-hang/form')"
      >
        + Thêm khách
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
            <th>Mã</th>
            <th>Khách hàng</th>
            <th>SĐT</th>
            <th>Trạng thái</th>
            <th class="action-col">Thao tác</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="kh in filteredData" :key="kh.id">
            <td>{{ kh.maKhachHang }}</td>
            <td><b>{{ kh.tenKhachHang }}</b></td>
            <td>{{ kh.soDienThoai }}</td>

            <td>
              <span
                class="pill"
                :class="`status-${getAdminStatusTone(kh.trangThai)}`"
              >
                ● {{ normalizeAdminStatusLabel(kh.trangThai) }}
              </span>
            </td>

            <td class="action-col">
              <div class="actions">
                <button
                  class="iconbtn"
                  @click="goToEdit(kh.id)"
                >
                  <Pencil size="16"/>
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
            <td colspan="5" style="text-align:center">
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
</style>