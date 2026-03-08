<script setup>
import { ref, computed, onMounted, watch } from "vue"
import { useRouter, useRoute } from "vue-router"
import { getAllHoaDon } from "../../../services/hoaDonService"
import { Eye, Printer } from "lucide-vue-next"

const router = useRouter()
const route = useRoute()

const hoaDons = ref([])
const loading = ref(false)

const searchText = ref("")
const filterStatus = ref("Tất cả trạng thái")
const fromDate = ref("")
const toDate = ref("")
const sortOption = ref("Mới nhất")

const currentPage = ref(1)
const pageSize = 10

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAllHoaDon()

    if (Array.isArray(res.data)) {
      hoaDons.value = res.data
    } else if (res.data.content) {
      hoaDons.value = res.data.content
    } else {
      hoaDons.value = []
    }
  } catch (error) {
    console.error("Error loading invoices:", error)
    hoaDons.value = []
  }
  finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()

  if (route.query.refresh) {
    router.replace('/admin/hoa-don/list')
  }

  window.addEventListener('focus', loadData)
})

watch(
  () => route.query.refresh,
  () => {
    currentPage.value = 1
    loadData()
  }
)
const formatCurrency = (value) => {
  if (!value) return "0₫"
  return new Intl.NumberFormat("vi-VN").format(value) + "₫"
}

const filteredData = computed(() => {
  let data = Array.isArray(hoaDons.value) ? [...hoaDons.value] : []

  if (searchText.value.trim()) {
    const keyword = searchText.value.toLowerCase()
    data = data.filter(d =>
      d.maHoaDon?.toLowerCase().includes(keyword) ||
      d.tenKhachHang?.toLowerCase().includes(keyword) ||
      d.soDienThoaiNhanHang?.includes(keyword)
    )
  }

  if (filterStatus.value !== "Tất cả trạng thái") {
    data = data.filter(d => d.trangThai === filterStatus.value)
  }

  if (fromDate.value) {
    data = data.filter(d => new Date(d.ngayTao) >= new Date(fromDate.value))
  }

  if (toDate.value) {
    data = data.filter(d => new Date(d.ngayTao) <= new Date(toDate.value))
  }

  if (sortOption.value === "Tổng cao nhất")
    data.sort((a, b) => (b.thanhTien || 0) - (a.thanhTien || 0))

  if (sortOption.value === "Tổng thấp nhất")
    data.sort((a, b) => (a.thanhTien || 0) - (b.thanhTien || 0))

  if (sortOption.value === "Mới nhất")
    data.sort((a, b) => new Date(b.ngayTao || 0) - new Date(a.ngayTao || 0))

  return data
})

const totalPages = computed(() =>
  Math.ceil(filteredData.value.length / pageSize)
)

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredData.value.slice(start, start + pageSize)
})

const viewDetail = (id) => {
  router.push(`/admin/hoa-don/detail/${id}`)
}

const printInvoice = () => {
  window.toast.info("In hoá đơn (demo)")
}
</script>

<template>
  <main class="wrap">
    <div class="card">

      <!-- HEADER SAME AS IMAGE -->
      <div class="head">
        <div>
          <h1>Hoá đơn</h1>
          <small class="muted">
            Theo dõi trạng thái đơn, thanh toán, giao hàng
          </small>
        </div>

      <router-link
        class="btn primary"
        to="/admin/hoa-don/detail/create"
      >
        + Tạo hoá đơn
      </router-link>
      </div>

      <div class="body">

        <!-- TOOLBAR EXACT SAME STRUCTURE -->
        <div class="toolbar">
          <div class="filters">
            <input
              style="width:320px"
              type="text"
              placeholder="Tìm theo mã HD / khách / SĐT..."
              v-model="searchText"
            />

            <select style="width:220px" v-model="filterStatus">
              <option>Tất cả trạng thái</option>
              <option>Chờ xác nhận</option>
              <option>Đang chuẩn bị</option>
              <option>Đang giao</option>
              <option>Hoàn thành</option>
              <option>Đã hủy</option>
            </select>
          </div>

          <div class="filters">
            <select style="width:220px" v-model="sortOption">
              <option>Mới nhất</option>
              <option>Tổng cao nhất</option>
              <option>Tổng thấp nhất</option>
            </select>
          </div>
        </div>

        <!-- TABLE STRUCTURE -->
        <table class="table">
          <thead>
            <tr>
              <th style="width:120px">Mã</th>
              <th>Khách hàng</th>
              <th style="width:160px">Thanh toán</th>
              <th style="width:140px">Trạng thái</th>
              <th style="width:140px" class="right">Tổng</th>
              <th style="width:120px; text-align:center">Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="hd in paginatedData" :key="hd.id">
              <td>
                {{ hd.maHoaDon }}
                <div class="muted small">
                  {{ hd.ngayTao }}
                </div>
              </td>

              <td>
                <b>{{ hd.tenKhachHang }}</b>
                <div class="muted small">
                  {{ hd.soDienThoaiNhanHang }}
                </div>
              </td>

              <td>
                <span class="pill">
                  {{ hd.phuongThucThanhToan || "COD" }}
                </span>
              </td>

              <td>
                <span
                  class="pill"
                  :class="{
                    ok: hd.trangThai === 'Hoàn thành',
                    warn: ['Chờ xác nhận','Đang chuẩn bị','Đang giao'].includes(hd.trangThai),
                    bad: hd.trangThai === 'Đã hủy'
                  }"
                >
                  ● {{ hd.trangThai }}
                </span>
              </td>

              <td class="right">
                {{ formatCurrency(hd.thanhTien) }}
              </td>

            <td>
              <div class="actions">
                <button class="iconbtn" @click="viewDetail(hd.id)">
                  <Eye size="16" />
                </button>

                <button class="iconbtn" @click="printInvoice">
                  <Printer size="16" />
                </button>
              </div>
            </td>
            </tr>

            <tr v-if="paginatedData.length === 0">
              <td colspan="6" style="text-align:center">
                Không có dữ liệu
              </td>
            </tr>
          </tbody>
        </table>

        <!-- PAGINATION EXACT SAME STRUCTURE -->
        <div class="pagination">
          <div>
Hiển thị 1–{{ paginatedData.length }} / {{ filteredData.length }}          </div>

          <div class="pager">
            <button
              class="btn"
              @click="currentPage--"
              :disabled="currentPage === 1"
            >
              ← Trước
            </button>

            <span class="chip">
              {{ currentPage }}
            </span>

            <button
              class="btn"
              @click="currentPage++"
              :disabled="currentPage >= totalPages"
            >
              Sau →
            </button>
          </div>
        </div>

      </div>
    </div>
  </main>
</template>


<style scoped>
.actions{
  display:flex;
  justify-content:center;
  align-items:center;
  gap:8px;
}

.action-cell{
  text-align:center;
}

.small{
  font-size: 12px;
}

.pill.bad{
  border-color: rgba(255, 92, 122, 0.35);
  background: rgba(255, 92, 122, 0.08);
  color: #ffd1da;
}
</style>