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

        <!-- HEADER -->
        <div class="modal-header">
          <div>
            <h3>Chi tiết hóa đơn</h3>
            <p class="invoice-code">
              {{ selectedHoaDon?.maHoaDon }}
            </p>
          </div>

          <button class="btn-icon-close" @click="showModal = false">
            ✕
          </button>
        </div>

        <!-- INFO SUMMARY -->
        <div class="invoice-summary">
          <div>
            <span>Khách hàng</span>
            <strong>{{ selectedHoaDon?.tenKhachHang }}</strong>
          </div>
          <div>
            <span>Ngày tạo</span>
            <strong>{{ selectedHoaDon?.ngayTao }}</strong>
          </div>
          <div>
            <span>Tổng tiền</span>
            <strong class="money">
              {{ formatCurrency(selectedHoaDon?.thanhTien) }}
            </strong>
          </div>
        </div>

        <!-- DETAIL TABLE -->
        <div class="detail-wrapper">
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
                <td class="money">
                  {{ formatCurrency(ct.thanhTien) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- FOOTER -->
        <div class="modal-footer">
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
  font-size: 22px;
  color: #ffffff;
}

.card {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 15px 35px rgba(0,0,0,0.06);
  padding: 24px;
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
  color: #334155; /* darker */
  font-size: 14px;
}

.invoice-table td {
  padding: 14px;
  border-bottom: 1px solid #e2e8f0;
  font-size: 14px;
  color: #1e293b; /* darker body text */
}

.invoice-table tr:hover {
  background: #f8fafc;
}

.money {
  font-weight: 700;
  color: #0f172a;
}

/* ===== STATUS (NEUTRAL STYLE) ===== */
.status-badge {
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  background: #f1f5f9; /* light gray */
  color: #334155;       /* normal dark text */
}

/* Remove all colorful classes */
.success,
.info,
.warning,
.pending,
.danger {
  background: #f1f5f9;
  color: #334155;
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

/* ===== MODAL ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(6px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.modal-box {
  background: #ffffff;
  width: 720px;
  max-width: 90%;
  border-radius: 26px;
  padding: 32px;
  box-shadow: 0 30px 70px rgba(0, 0, 0, 0.18);
  animation: modalFade 0.25s ease;
}

/* HEADER */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  color: #ffffff; 
}

.modal-header h3 {
  font-weight: 700;
  font-size: 20px;
  margin-bottom: 4px;
  color: #0f172a; /* darker */
}

.invoice-code {
  color: #334155; /* darker */
  font-size: 14px;
  font-weight: 500;
}

/* CLOSE ICON BUTTON */
.btn-icon-close {
  background: #f1f5f9;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 16px;
  transition: 0.2s;
  color: #334155;
}

.btn-icon-close:hover {
  background: #e2e8f0;
}

/* SUMMARY */
.invoice-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 18px;
  background: #f8fafc;
  border-radius: 16px;
  margin-bottom: 24px;
}

.invoice-summary span {
  font-size: 12px;
  color: #475569; /* darker */
}

.invoice-summary strong {
  display: block;
  margin-top: 6px;
  font-size: 15px;
  color: #0f172a;
  font-weight: 700;
}

/* DETAIL TABLE */
.detail-wrapper {
  max-height: 250px;
  overflow-y: auto;
  margin-bottom: 20px;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
}

.detail-table th {
  text-align: left;
  padding: 12px;
  font-size: 13px;
  color: #334155; /* darker */
  background: #f1f5f9;
}

.detail-table td {
  padding: 12px;
  border-bottom: 1px solid #e2e8f0;
  font-size: 14px;
  color: #1e293b;
}

.detail-table tr:hover {
  background: #f8fafc;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 28px;
  padding-top: 18px;
  border-top: 1px solid #e2e8f0;
}

.btn-close {
  background: #2563eb;
  color: #ffffff;
  border: none;
  padding: 11px 22px; 
  border-radius: 10px; 
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 100px;
}

.btn-close:hover {
  background: #1e40af;
}
/* Animation */
@keyframes modalFade {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>