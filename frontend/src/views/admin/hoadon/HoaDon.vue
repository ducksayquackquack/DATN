<template>
  <div class="invoice-page">
    <h2 class="page-title">Quản lý Hóa Đơn</h2>

    <div class="card">
      <table class="invoice-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Mã</th>
            <th>Khách Hàng</th>
            <th>Ngày Tạo</th>
            <th>Tổng Tiền</th>
            <th>Trạng Thái</th>
            <th width="200">Hành động</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="item in hoaDonList" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.maHoaDon }}</td>
            <td>{{ item.tenKhachHang }}</td>
            <td>{{ item.ngayTao }}</td>
            <td class="money">
              {{ formatCurrency(item.thanhTien) }}
            </td>

            <td>
              <span class="status-badge" :class="statusClass(item.trangThai)">
                {{ item.trangThai }}
              </span>
            </td>

            <td class="actions">
              <button class="btn-view" @click="viewDetail(item)">
                Xem
              </button>

              <button class="btn-delete" @click="remove(item.id)">
                Xóa
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- MODAL -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-box">
        <h4 class="modal-title">
          Chi tiết hóa đơn {{ selectedHoaDon?.maHoaDon }}
        </h4>

        <table class="detail-table">
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

        <div class="modal-actions">
          <button class="btn-close" @click="showModal = false">
            Đóng
          </button>
        </div>
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

const statusClass = (status) => {
  switch (status) {
    case "Đã giao":
      return "success";
    case "Đang giao":
      return "info";
    case "Chờ xử lý":
      return "warning";
    case "Chờ xác nhận":
      return "pending";
    case "Đã hủy":
      return "danger";
    default:
      return "";
  }
};

onMounted(fetchData);
</script>

<style scoped>

.invoice-page {
  padding: 24px;
}

.page-title {
  font-weight: 700;
  margin-bottom: 20px;
}

.card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.06);
  padding: 20px;
}

.invoice-table {
  width: 100%;
  border-collapse: collapse;
}

.invoice-table th {
  text-align: left;
  padding: 14px;
  background: #f1f5f9;
  font-weight: 600;
  color: #475569;
}

.invoice-table td {
  padding: 14px;
  border-bottom: 1px solid #e2e8f0;
}

.invoice-table tr:hover {
  background: #f8fafc;
}

.money {
  font-weight: 600;
  color: #0f172a;
}

/* STATUS BADGE */
.status-badge {
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}



/* ACTION BUTTONS */
.actions {
  display: flex;
  gap: 10px;
}

.btn-view {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: none;
  color: white;
  padding: 8px 16px;
  border-radius: 10px;
  font-weight: 500;
  cursor: pointer;
  transition: 0.2s;
}

.btn-view:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 15px rgba(37, 99, 235, 0.3);
}

.btn-delete {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  border: none;
  color: white;
  padding: 8px 16px;
  border-radius: 10px;
  font-weight: 500;
  cursor: pointer;
  transition: 0.2s;
}

.btn-delete:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 15px rgba(220, 38, 38, 0.3);
}

/* MODAL */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.modal-box {
  background: white;
  width: 600px;
  padding: 28px;
  border-radius: 18px;
  animation: fadeIn 0.2s ease;
}

.modal-title {
  margin-bottom: 16px;
  font-weight: 600;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
}

.detail-table th,
.detail-table td {
  padding: 10px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-actions {
  text-align: right;
  margin-top: 20px;
}

.btn-close {
  background: #334155;
  color: white;
  border: none;
  padding: 8px 18px;
  border-radius: 10px;
  cursor: pointer;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

</style>