<template>
  <div>
    <h2 class="mb-4">Quản lý Hóa Đơn</h2>

    <table class="table table-bordered table-hover">
      <thead class="table-light">
        <tr>
          <th>ID</th>
          <th>Mã</th>
          <th>Khách Hàng</th>
          <th>Ngày Tạo</th>
          <th>Tổng Tiền</th>
          <th>Trạng Thái</th>
          <th width="180">Hành động</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in hoaDonList" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.maHoaDon }}</td>
          <td>{{ item.tenKhachHang }}</td>
          <td>{{ item.ngayTao }}</td>
          <td>{{ formatCurrency(item.thanhTien) }}</td>
            <td>
              {{ item.trangThai }}
            </td>
          <td>
            <button
              class="btn btn-sm btn-primary me-2"
              @click="viewDetail(item)"
            >
              Xem
            </button>
            <button
              class="btn btn-sm btn-danger"
              @click="remove(item.id)"
            >
              Xóa
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- MODAL -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-box">
        <h5 class="mb-3">
          Chi tiết hóa đơn {{ selectedHoaDon.maHoaDon }}
        </h5>

        <table class="table table-bordered">
          <thead>
            <tr>
              <th>Sản phẩm</th>
              <th>Số lượng</th>
              <th>Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ct in chiTietList" :key="ct.id">
              <td>{{ ct.sanPhamChiTiet?.ma }}</td>
              <td>{{ ct.soLuong }}</td>
              <td>{{ formatCurrency(ct.thanhTien) }}</td>
            </tr>
          </tbody>
        </table>

        <button class="btn btn-secondary mt-3" @click="showModal = false">
          Đóng
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import {
  getHoaDonList,
  deleteHoaDon,
  getHoaDonChiTiet,
} from "@/services/HoaDonService";

const hoaDonList = ref([]);
const chiTietList = ref([]);
const selectedHoaDon = ref(null);
const showModal = ref(false);

const fetchData = async () => {
  const { data } = await getHoaDonList();
  hoaDonList.value = data;
};

const remove = async (id) => {
  if (confirm("Bạn có chắc muốn xóa hóa đơn này?")) {
    await deleteHoaDon(id);
    fetchData();
  }
};

const viewDetail = async (item) => {
  selectedHoaDon.value = item;
  const { data } = await getHoaDonChiTiet(item.id);
  chiTietList.value = data;
  showModal.value = true;
};

const formatCurrency = (value) => {
  if (!value) return "0 đ";
  return value.toLocaleString("vi-VN") + " đ";
};

onMounted(fetchData);
</script>
<style>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);

  display: flex;
  justify-content: center;
  align-items: center;

  z-index: 9999;
}

.modal-box {
  background: #ffffff;
  width: 600px;
  padding: 30px;
  border-radius: 16px;

  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.2);
  animation: fadeIn 0.2s ease;
}

/* Table header */
.table thead th {
  background-color: #f8fafc;
  font-weight: 600;
  color: #475569;
  padding: 14px;
}

/* Optional: smooth entrance */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>