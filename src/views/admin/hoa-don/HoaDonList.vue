<script setup>
import { ref, computed, onMounted } from "vue"
import { useRouter } from "vue-router"
import { getAllHoaDon } from "../../../services/hoaDonService"
import { Eye, Printer } from "lucide-vue-next"

const router = useRouter()

const hoaDons = ref([])
const loading = ref(false)

const searchText = ref("")
const filterStatus = ref("")
const sortOption = ref("Mới nhất")

const currentPage = ref(1)
const pageSize = 5

/* ================= LOAD DATA ================= */
const loadData = async () => {
  try {
    loading.value = true
    const res = await getAllHoaDon()

    let backendData = []
    if (Array.isArray(res.data)) {
      backendData = res.data
    } else if (res.data?.content) {
      backendData = res.data.content
    }

    // Đọc thêm từ localStorage (hóa đơn tạo từ frontend)
    const localData = JSON.parse(localStorage.getItem("hoaDonList")) || []
    
    console.log("LocalStorage data:", localData)
    
    // Chuyển đổi format localStorage về format backend
    const convertedLocalData = localData.map(item => {
      const converted = {
        id: item.id,
        maHoaDon: item.maHoaDon || item.id,
        tenKhachHang: item.tenKhachHang || item.customer || "Khách hàng",
        soDienThoaiNhanHang: item.soDienThoaiNhanHang || item.phone || "",
        diaChiNhanHang: item.diaChiNhanHang || item.address || "",
        thanhTien: item.thanhTien || item.total || 0,
        phuongThucThanhToan: item.phuongThucThanhToan || item.payment || "TIEN_MAT",
        trangThai: item.trangThai || item.status || "CHUA_XAC_NHAN",
        ngayTao: item.ngayTao || item.date || new Date().toISOString()
      }
      console.log("Converted item:", converted)
      return converted
    })

    // Gộp dữ liệu backend + localStorage
    hoaDons.value = [...convertedLocalData, ...backendData]
    
    console.log("Final hoaDons:", hoaDons.value)
    
  } catch (err) {
    console.error("Lỗi load hóa đơn:", err)
    
    // Nếu backend lỗi, chỉ dùng localStorage
    const localData = JSON.parse(localStorage.getItem("hoaDonList")) || []
    const convertedLocalData = localData.map(item => ({
      id: item.id,
      maHoaDon: item.maHoaDon || item.id,
      tenKhachHang: item.tenKhachHang || item.customer,
      soDienThoaiNhanHang: item.soDienThoaiNhanHang || item.phone,
      diaChiNhanHang: item.diaChiNhanHang || item.address,
      thanhTien: item.thanhTien || item.total,
      phuongThucThanhToan: item.phuongThucThanhToan || item.payment,
      trangThai: item.trangThai || item.status,
      ngayTao: item.ngayTao || item.date
    }))
    
    hoaDons.value = convertedLocalData
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

/* ================= FORMAT ================= */

const formatCurrency = (value) => {
  if (!value) return "0₫"
  return new Intl.NumberFormat("vi-VN").format(value) + "₫"
}

const formatDate = (date) => {
  if (!date) return ""
  return new Date(date).toLocaleString("vi-VN")
}

const formatStatus = (status) => {
  const map = {
    CHUA_XAC_NHAN: "Chưa xác nhận",
    CHO_THANH_TOAN: "Chờ thanh toán",
    CHO_GIAO: "Chờ giao",
    DANG_GIAO: "Đang giao",
    DA_HOAN_THANH: "Hoàn thành",
    DA_HUY: "Đã huỷ"
  }
  return map[status] || status
}

const formatPayment = (method) => {
  const map = {
    TIEN_MAT: "Tiền mặt",
    VNPAY: "Thanh toán VNPAY"
  }
  return map[method] || "COD"
}

/* ================= FILTER ================= */

const filteredData = computed(() => {
  let data = [...hoaDons.value]

  if (searchText.value.trim()) {
    const keyword = searchText.value.toLowerCase()

    data = data.filter(d =>
      d.maHoaDon?.toLowerCase().includes(keyword) ||
      d.tenKhachHang?.toLowerCase().includes(keyword) ||
      d.soDienThoaiNhanHang?.includes(keyword)
    )
  }

  if (filterStatus.value) {
    data = data.filter(d => d.trangThai === filterStatus.value)
  }

  if (sortOption.value === "Tổng cao nhất")
    data.sort((a, b) => b.thanhTien - a.thanhTien)

  if (sortOption.value === "Tổng thấp nhất")
    data.sort((a, b) => a.thanhTien - b.thanhTien)

  if (sortOption.value === "Mới nhất")
    data.sort((a, b) => new Date(b.ngayTao) - new Date(a.ngayTao))

  return data
})

/* ================= PAGINATION ================= */

const totalPages = computed(() =>
  Math.ceil(filteredData.value.length / pageSize)
)

const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredData.value.slice(start, start + pageSize)
})

/* ================= ACTION ================= */

const viewDetail = (id) => {
  router.push(`/admin/hoa-don/detail/${id}`)
}

const printInvoice = () => {
  window.print()
}
</script>

<template>
  <main class="wrap">
    <div class="card">

      <!-- HEADER -->
      <div class="head">
        <div>
          <h1>Hoá đơn</h1>
          <small class="muted">
            Theo dõi trạng thái đơn hàng
          </small>
        </div>

        <router-link class="btn primary" to="/admin/hoa-don/detail">
          + Tạo hoá đơn
        </router-link>
      </div>

      <div class="body">

        <!-- FILTER -->
        <div class="toolbar">
          <div class="filters">

            <input
              style="width:320px"
              type="text"
              placeholder="Tìm theo mã HD / khách / SĐT..."
              v-model="searchText"
            />

            <select style="width:220px" v-model="filterStatus">
              <option value="">Tất cả</option>
              <option value="CHUA_XAC_NHAN">Chưa xác nhận</option>
              <option value="CHO_THANH_TOAN">Chờ thanh toán</option>
              <option value="CHO_GIAO">Chờ giao</option>
              <option value="DANG_GIAO">Đang giao</option>
              <option value="DA_HOAN_THANH">Hoàn thành</option>
              <option value="DA_HUY">Huỷ</option>
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
                  {{ formatDate(hd.ngayTao) }}
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
                  {{ formatPayment(hd.phuongThucThanhToan) }}
                </span>
              </td>

              <td>
                <span
                  class="pill"
                  :class="{
                    ok: hd.trangThai === 'DA_HOAN_THANH',
                    warn: ['CHUA_XAC_NHAN','CHO_GIAO','DANG_GIAO','CHO_THANH_TOAN'].includes(hd.trangThai),
                    danger: hd.trangThai === 'DA_HUY'
                  }"
                >
                  ● {{ formatStatus(hd.trangThai) }}
                </span>
              </td>

              <td class="right">
                {{ formatCurrency(hd.thanhTien) }}
              </td>

              <td>
                <div class="actions">

                  <button class="iconbtn" @click="viewDetail(hd.id)">
                    <Eye size="16"/>
                  </button>

                  <button class="iconbtn" @click="printInvoice">
                    <Printer size="16"/>
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

        <!-- PAGINATION -->

        <div class="pagination">

          <div>
            Hiển thị {{ paginatedData.length }} / {{ filteredData.length }}
          </div>

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
  gap:8px;
}
</style>