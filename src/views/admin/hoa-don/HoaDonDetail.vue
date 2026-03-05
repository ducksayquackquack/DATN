<script setup>
import { ref, onMounted, computed } from "vue"
import { useRoute, useRouter } from "vue-router"
import axios from "axios"

const route = useRoute()
const router = useRouter()

const id = route.params.id
const isCreate = !id

const hoaDon = ref({
  maHoaDon: "",
  tenKhachHang: "",
  soDienThoaiNhanHang: "",
  diaChiNhanHang: "",
  ngayTao: new Date().toLocaleString(),
  trangThai: "Chờ xác nhận",
  phuongThucThanhToan: "COD",
  phiShip: 0,
  ghiChu: ""
})

const chiTietList = ref([])
const sanPhamList = ref([])

const selectedTrangThai = ref("Chờ xác nhận")
const selectedPTTT = ref("COD")
const ghiChu = ref("")
const selectedProduct = ref("")
const selectedQuantity = ref(1)

const loading = ref(false)
const showAddProduct = ref(false)

const API = "http://localhost:8080/api/hoa-don"
const PRODUCT_API = "http://localhost:8080/api/san-pham"



const loadData = async () => {
  loading.value = true

  try {
    // Always load products
    const productsRes = await axios.get(PRODUCT_API)
    sanPhamList.value = Array.isArray(productsRes.data) 
      ? productsRes.data 
      : (productsRes.data.content || [])

    if (!id) return

    const hdRes = await axios.get(`${API}/${id}`)
    hoaDon.value = hdRes.data

    selectedTrangThai.value = hdRes.data?.trangThai || "Chờ xác nhận"

    const rawPTTT = hdRes.data?.phuongThucThanhToan

    if (!rawPTTT) {
      selectedPTTT.value = "COD"
    }
    else if (rawPTTT.toUpperCase() === "COD") {
      selectedPTTT.value = "COD"
    }
    else {
      selectedPTTT.value = "Chuyển khoản"
    }

    ghiChu.value = hdRes.data?.ghiChu || ""

    try {
      const ctRes = await axios.get(`${API}/${id}/chi-tiet`)
      chiTietList.value = Array.isArray(ctRes.data) ? ctRes.data : []
    }
    catch {
      chiTietList.value = []
    }
  }
  catch {
    console.error("Không load được hóa đơn")
  }
  finally {
    loading.value = false
  }
}

const addProduct = async () => {
  if (!selectedProduct.value || !selectedQuantity.value) {
    alert("Vui lòng chọn sản phẩm và số lượng")
    return
  }

  const product = sanPhamList.value.find(p => p.id == selectedProduct.value)
  if (!product) return

  // Get price from variants first, then fallback to product price
  const variants = product.sanPhamChiTiets || []
  const firstVariant = variants.length > 0 ? variants[0] : null
  const giaBan = firstVariant?.giaBan ?? product.giaBan ?? 0

  const existingItem = chiTietList.value.find(
    item => (item.sanPhamChiTietId == selectedProduct.value || 
             item.sanPhamChiTiet?.id == selectedProduct.value)
  )

  if (existingItem) {
    existingItem.soLuong += parseInt(selectedQuantity.value)
  } else {
    chiTietList.value.push({
      sanPhamChiTiet: {
        id: product.id,
        giaBan: giaBan,
        sanPham: {
          tenSanPham: product.tenSanPham
        }
      },
      sanPhamChiTietId: product.id,
      soLuong: parseInt(selectedQuantity.value),
      donGia: giaBan
    })
  }

  selectedProduct.value = ""
  selectedQuantity.value = 1
  showAddProduct.value = false
}

const removeProduct = (index) => {
  chiTietList.value.splice(index, 1)
}

onMounted(loadData)



const formatCurrency = (value) =>
  new Intl.NumberFormat("vi-VN").format(value || 0) + "₫"

const getProductPrice = (product) => {
  if (!product) return 0
  const variants = product.sanPhamChiTiets || []
  const firstVariant = variants.length > 0 ? variants[0] : null
  return firstVariant?.giaBan ?? product.giaBan ?? 0
}


const editable = computed(() =>
  ["Chờ xác nhận", "Đang giao"].includes(selectedTrangThai.value)
)

const subtotal = computed(() => {

  if (!Array.isArray(chiTietList.value)) return 0

  return chiTietList.value.reduce((sum, item) => {

    const price =
      item?.donGia ??
      item?.sanPhamChiTiet?.giaBan ??
      item?.thanhTien ??
      0

    const qty = item?.soLuong ?? 0

    return sum + price * qty

  }, 0)

})



const handleSave = async () => {

  if (loading.value) return

  loading.value = true

  try {

    const payload = {
      ...hoaDon.value,
      trangThai: selectedTrangThai.value,
      phuongThucThanhToan: selectedPTTT.value,
      ghiChu: ghiChu.value || hoaDon.value.ghiChu
    }

if (isCreate) {

  const res = await axios.post(API, payload)
  const newHoaDonId = res.data.id

  // Add products to the new invoice
  if (chiTietList.value.length > 0) {
    for (const item of chiTietList.value) {
      await axios.post(`${API}/${newHoaDonId}/chi-tiet`, {
        sanPhamChiTietId: item.sanPhamChiTiet?.id || item.sanPhamChiTietId,
        soLuong: item.soLuong,
        donGia: item.donGia || item.sanPhamChiTiet?.giaBan
      })
    }
  }

  alert("Tạo hóa đơn thành công")

  router.push('/admin/hoa-don/list?refresh=' + Date.now())

}
    else {
      await axios.put(`${API}/${id}`, payload)
      alert("Cập nhật thành công")
      router.push('/admin/hoa-don/list?refresh=' + Date.now())
    }

  }
  catch (error) {
    console.error(error)
    alert("Lưu hóa đơn thất bại: " + (error.response?.data?.message || error.message))
  }
  finally {
    loading.value = false
  }

}



const handleDelete = async () => {

  if (!hoaDon.value) return

  loading.value = true

  try {

    await axios.delete(`${API}/${id}`)

    router.push("/admin/hoa-don/list")

  }
  finally {

    loading.value = false

  }

}



const handlePrint = () => {

  window.print()

}
</script>



<template>
  <main class="wrap" v-if="hoaDon">

    <div class="grid cols2">

      <div class="card">

        <div class="head">

          <div>

            <h1 v-if="isCreate">Tạo hoá đơn</h1>

            <h1 v-else>
              Hoá đơn #{{ hoaDon.maHoaDon }}
            </h1>

            <small class="muted">
              Tạo: {{ hoaDon.ngayTao }}
            </small>

          </div>



          <div style="display:flex;gap:10px;flex-wrap:wrap">

            <button class="btn" @click="router.push('/admin/hoa-don/list')">
              ← Quay lại
            </button>

            <button class="btn" @click="handlePrint">
              🖨️ In
            </button>

            <button
              class="btn primary"
              :disabled="loading"
              @click="handleSave"
            >
              Lưu
            </button>

          </div>

        </div>



        <div class="body">

          <div class="grid cols2">

            <div class="card inner">

              <div class="head">

                <h2>Khách hàng</h2>

                <small class="muted">
                  Thông tin nhận hàng
                </small>

              </div>

              <div class="body">

                <div class="field">
                  <label>Tên khách hàng</label>
                  <input
                    v-if="isCreate"
                    v-model="hoaDon.tenKhachHang"
                    type="text"
                    placeholder="Tên khách hàng"
                  />

                  <b v-else>
                    {{ hoaDon.tenKhachHang }}
                  </b>
                </div>

                <div class="field">
                  <label>Số điện thoại</label>
                  <input
                    v-if="isCreate"
                    v-model="hoaDon.soDienThoaiNhanHang"
                    type="tel"
                    placeholder="Số điện thoại"
                  />

                  <div v-else class="muted">
                    {{ hoaDon.soDienThoaiNhanHang }}
                  </div>
                </div>

                <div class="field">
                  <label>Địa chỉ</label>
                  <input
                    v-if="isCreate"
                    v-model="hoaDon.diaChiNhanHang"
                    type="text"
                    placeholder="Địa chỉ"
                  />

                  <div v-else class="muted">
                    {{ hoaDon.diaChiNhanHang }}
                  </div>
                </div>

              </div>

            </div>



            <div class="card inner">

              <div class="head">

                <h2>Trạng thái</h2>

                <small class="muted">
                  Workflow
                </small>

              </div>

              <div class="body">

                <label>Cập nhật trạng thái</label>

<select v-model="selectedTrangThai" :disabled="!editable">                  <option>Chờ xác nhận</option>
                  <option>Chờ xử lý</option>
                  <option>Đang chuẩn bị</option>
                  <option>Đang giao</option>
                  <option>Đã giao</option>
                  <option>Hoàn thành</option>
                  <option>Đã hủy</option>
                </select>

              </div>

            </div>

          </div>



          <hr class="sep"/>



          <div class="card inner">

            <div class="head">

              <h2>Sản phẩm</h2>

              <small class="muted">
                Line items
              </small>

            </div>



            <div class="body">

              <table class="table">

                <thead>

                  <tr>

                    <th>Sản phẩm</th>

                    <th class="center">SL</th>

                    <th class="right">Đơn giá</th>

                    <th class="right">Thành tiền</th>

                    <th v-if="isCreate" class="center">Thao tác</th>

                  </tr>

                </thead>

                <tbody>

                  <tr v-if="chiTietList.length === 0">

                    <td :colspan="isCreate ? 5 : 4" style="text-align:center">
                      Không có sản phẩm
                    </td>

                  </tr>

                  <tr
                    v-for="(item, idx) in chiTietList"
                    :key="idx"
                  >

                    <td>

                      <b>

                      {{
                        item?.sanPhamChiTiet?.sanPham?.tenSanPham
                        || item?.tenSanPham
                        || item?.sanPham?.tenSanPham
                        || 'SP ID: ' + (item?.sanPhamChiTiet?.id || item?.sanPhamChiTietId)
                      }}

                      </b>

                    </td>

                    <td class="center">

                      <input 
                        v-if="isCreate"
                        v-model.number="item.soLuong" 
                        type="number" 
                        min="1"
                        style="width:60px"
                      />
                      <span v-else>{{ item?.soLuong || 0 }}</span>

                    </td>

                    <td class="right">

                      {{
                        formatCurrency(
                          item?.donGia ??
                          item?.sanPhamChiTiet?.giaBan ??
                          0
                        )
                      }}

                    </td>

                    <td class="right">

                      {{

                        formatCurrency(

                          (item?.donGia ??
                          item?.sanPhamChiTiet?.giaBan ??
                          0)

                          *

                          (item?.soLuong || 0)

                        )

                      }}

                    </td>

                    <td v-if="isCreate" class="center">
                      <button 
                        class="btn danger"
                        style="padding: 5px 10px; font-size: 12px;"
                        @click="removeProduct(idx)"
                      >
                        Xóa
                      </button>
                    </td>

                  </tr>

                </tbody>

              </table>

              <div v-if="isCreate" style="margin-top: 12px">
                <button 
                  v-if="!showAddProduct"
                  class="btn primary"
                  @click="showAddProduct = true"
                >
                  + Thêm sản phẩm
                </button>

                <div v-else class="card inner" style="margin-top: 10px">
                  <div class="body">
                    <div class="grid cols2" style="gap: 10px;">
                      <div>
                        <label>Chọn sản phẩm</label>
                        <select v-model="selectedProduct">
                          <option value="">-- Chọn sản phẩm --</option>
                          <option 
                            v-for="p in sanPhamList" 
                            :key="p.id"
                            :value="p.id"
                          >
                            {{ p.tenSanPham }} ({{ formatCurrency(getProductPrice(p)) }})
                          </option>
                        </select>
                      </div>
                      <div>
                        <label>Số lượng</label>
                        <input 
                          v-model.number="selectedQuantity" 
                          type="number" 
                          min="1"
                          placeholder="1"
                        />
                      </div>
                    </div>
                    <div style="display: flex; gap: 8px; margin-top: 10px;">
                      <button class="btn primary" @click="addProduct">Thêm</button>
                      <button class="btn" @click="showAddProduct = false">Hủy</button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="grid cols2" style="margin-top:12px">

                <div>

                  <label>Ghi chú đơn</label>

                  <textarea v-model="ghiChu"></textarea>

                </div>



                <div class="card total-box">

                  <div class="body">

                    <div class="row-between muted">

                      <span>Tạm tính</span>

                      <span>
                        {{ formatCurrency(subtotal) }}
                      </span>

                    </div>



                    <div class="row-between muted">

                      <span>Phí ship</span>

                      <span>
                        {{ formatCurrency(hoaDon.phiShip) }}
                      </span>

                    </div>



                    <hr class="sep"/>



                    <div class="row-between bold">

                      <span>Tổng</span>

                      <span>

                        {{
                          formatCurrency(
                            subtotal + (hoaDon.phiShip || 0)
                          )
                        }}

                      </span>

                    </div>



                    <div style="margin-top:10px">

                      <label>Phương thức thanh toán</label>

                      <select v-model="selectedPTTT">

                        <option value="COD">
                          COD
                        </option>

                        <option value="Chuyển khoản">
                          Chuyển khoản
                        </option>

                      </select>

                    </div>

                  </div>

                </div>

              </div>

            </div>

          </div>

        </div>

      </div>



      <div class="card">

        <div class="head">

          <h2>Timeline</h2>

          <small class="muted">
            Log thao tác
          </small>

        </div>



        <div class="body">

          <div class="card inner">

            <div class="body">

              <b>✓ Đơn được tạo</b>

              <div class="muted" style="font-size: 12px;">

                {{ hoaDon.ngayTao }}

              </div>

              <div style="margin-top: 8px; font-size: 13px;">
                <div><strong>Khách:</strong> {{ hoaDon.tenKhachHang }}</div>
                <div><strong>SĐT:</strong> {{ hoaDon.soDienThoaiNhanHang }}</div>
                <div><strong>K/hoàn:</strong> {{ formatCurrency(subtotal + (hoaDon.phiShip || 0)) }}</div>
              </div>

            </div>

          </div>

          <div class="card inner">

            <div class="body">

              <b>{{ selectedTrangThai === 'Hoàn thành' ? '✓' : '◉' }} {{ selectedTrangThai }}</b>

              <div class="muted" style="font-size: 12px;">

                {{ hoaDon.ngayTao }}

              </div>

            </div>

          </div>

          <hr class="sep"/>

          <div style="font-size: 12px; color: var(--muted); margin-bottom: 12px;">
            <strong>Tổng sản phẩm:</strong> {{ chiTietList.length }}<br/>
            <strong>Tính theo:</strong> {{ selectedPTTT }}
          </div>

          <button
            v-if="!isCreate"
            class="btn danger"
            @click="handleDelete"
          >
            Huỷ đơn
          </button>

        </div>

      </div>

    </div>

  </main>
</template>



<style scoped>

label{
  display: block;
  font-size: 13px;
  color: rgba(238, 242, 255, 0.9);
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 0.3px;
}

.row-between{
  display:flex;
  justify-content:space-between;
  margin-bottom:8px;
}

.bold{
  font-weight:600;
}

.center{
  text-align:center;
}

.muted{
  color:#6b7280;
  font-size:13px;
}

.inner{
  border-radius:14px;
}

.total-box{
  border-radius:14px;
}

input[type="number"]{
  padding: 8px 10px;
}

button.danger:hover{
  background: rgba(255, 92, 122, 0.15);
}

</style>