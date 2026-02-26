<script setup>
import { ref, onMounted, computed } from "vue";
import {
  getPTTT,
  createPTTT,
  updatePTTT,
  deletePTTT
} from "@/services/phuongThucThanhToanService";

/* ================= STATE ================= */
const ptttList = ref([]);
const search = ref("");
const show = ref(false);
const editing = ref(false);
const currentId = ref(null);

const ptttForm = ref({
  ma: "",
  ten: "",
  moTa: "",
  trangThai: "Hoạt động"
});

/* ================= FETCH ================= */
const fetchData = async () => {
  const res = await getPTTT();
  ptttList.value = res.data;
};

/* ================= SEARCH ================= */
const filteredList = computed(() => {
  if (!search.value) return ptttList.value;

  return ptttList.value.filter(item =>
    item.ma.toLowerCase().includes(search.value.toLowerCase()) ||
    item.ten.toLowerCase().includes(search.value.toLowerCase()) ||
    item.id.toString().includes(search.value)
  );
});

/* ================= OPEN MODAL ================= */
const open = (item = null) => {
  editing.value = !!item;
  currentId.value = item?.id || null;

  ptttForm.value = item
    ? { ...item }
    : { ma: "", ten: "", moTa: "", trangThai: "Hoạt động" };

  show.value = true;
};

/* ================= SAVE ================= */
const save = async () => {
  if (editing.value) {
    await updatePTTT(currentId.value, ptttForm.value);
  } else {
    await createPTTT(ptttForm.value);
  }
  show.value = false;
  fetchData();
};

/* ================= DELETE ================= */
const remove = async (id) => {
  if (confirm("Bạn chắc chắn muốn xoá phương thức này?")) {
    await deletePTTT(id);
    fetchData();
  }
};

onMounted(fetchData);
</script>

<template>
  <div class="admin-container">
    <h2 class="title">Quản lý Phương Thức Thanh Toán</h2>

    <!-- SEARCH + ADD -->
    <div class="top-bar">
      <input
        v-model="search"
        type="text"
        placeholder="Tìm theo ID, Mã hoặc Tên..."
      />
      <button class="btn-primary" @click="open()">Thêm mới</button>
    </div>

    <table class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Mã</th>
          <th>Tên</th>
          <th>Mô tả</th>
          <th>Trạng thái</th>
          <th>Hành động</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="item in filteredList" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.ma }}</td>
          <td>{{ item.ten }}</td>
          <td>{{ item.moTa }}</td>
          <td>{{ item.trangThai }}</td>
          <td>
            <button class="btn-edit" @click="open(item)">Sửa</button>
            <button class="btn-delete" @click="remove(item.id)">Xoá</button>
          </td>
        </tr>

        <tr v-if="filteredList.length === 0">
          <td colspan="6">Không có dữ liệu</td>
        </tr>
      </tbody>
    </table>

    <!-- CENTERED MODAL -->
    <div v-if="show" class="global-overlay">
      <div class="global-modal">
        <h3>
          {{ editing ? "Cập nhật PTTT" : "Thêm PTTT" }}
        </h3>

        <input v-model="ptttForm.ma" placeholder="Mã" />
        <input v-model="ptttForm.ten" placeholder="Tên" />
        <input v-model="ptttForm.moTa" placeholder="Mô tả" />

        <select v-model="ptttForm.trangThai">
          <option value="Hoạt động">Hoạt động</option>
          <option value="Ngừng hoạt động">Ngừng hoạt động</option>
        </select>

        <div class="modal-actions">
          <button class="btn-cancel" @click="show=false">Hủy</button>
          <button class="btn-primary" @click="save">Lưu</button>
        </div>
      </div>
    </div>
  </div>
</template>
<style scoped>
.admin-container {
  padding: 0;
}

/* Title */
.title {
  font-size: 24px;
  margin-bottom: 24px;
  font-weight: 600;
  color: #ffffff;
}

/* TOP BAR */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.top-bar input {
  padding: 10px 14px;
  width: 280px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.top-bar input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

/* TABLE */
.table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.table th {
  padding: 14px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  background: #f8fafc;
  color: #475569;
}

.table td {
  padding: 14px;
  text-align: center;
  font-size: 14px;
  border-top: 1px solid #f1f5f9;
}

.table tbody tr {
  transition: background-color 0.2s ease;
}

.table tbody tr:hover {
  background-color: #f9fafb;
}

/* BUTTONS */
button {
  border: none;
  border-radius: 8px;
  padding: 7px 14px;
  cursor: pointer;
  font-size: 13px;
  transition: transform 0.15s ease, background-color 0.2s ease;
}

.btn-primary {
  background: #2563eb;
  color: #ffffff;
}

.btn-primary:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
}

.btn-edit {
  background: #f39c12;
  color: #ffffff;
  margin-right: 6px;
}

.btn-edit:hover {
  background: #d97706;
  transform: translateY(-1px);
}

.btn-delete {
  background: #e74c3c;
  color: #ffffff;
}

.btn-delete:hover {
  background: #dc2626;
  transform: translateY(-1px);
}

.btn-cancel {
  background: #cccccc;
}

.btn-cancel:hover {
  background: #b5b5b5;
}

/* GLOBAL CENTER MODAL */
.global-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);

  display: flex;
  justify-content: center;
  align-items: center;

  z-index: 9999;
}

.global-modal {
  width: 420px;
  background: #ffffff;
  padding: 28px;
  border-radius: 16px;

  display: flex;
  flex-direction: column;
  gap: 14px;

  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.15);
  animation: fadeIn 0.2s ease;
}

.global-modal h3 {
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.global-modal input,
.global-modal select {
  padding: 10px 12px;
  font-size: 14px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  width: 100%;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.global-modal input:focus,
.global-modal select:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}

/* Animation */
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