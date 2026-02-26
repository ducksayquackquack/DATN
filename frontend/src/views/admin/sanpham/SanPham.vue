<script setup>
import { ref, onMounted, computed } from "vue";
import {
  getSanPhamList,
  createSanPham,
  updateSanPham,
  deleteSanPham
} from "@/services/sanPhamService";

const list = ref([]);
const search = ref("");
const show = ref(false);
const editing = ref(false);
const currentId = ref(null);

const form = ref({
  maSanPham: "",
  tenSanPham: "",
  moTa: "",
  trangThai: "Hoạt động"
});

const fetchData = async () => {
  const res = await getSanPhamList();
  list.value = res.data;
};

const filteredList = computed(() => {
  if (!search.value) return list.value;

  return list.value.filter(item =>
    item.maSanPham.toLowerCase().includes(search.value.toLowerCase()) ||
    item.tenSanPham.toLowerCase().includes(search.value.toLowerCase()) ||
    item.id.toString().includes(search.value)
  );
});

/* ================= OPEN MODAL ================= */
const open = (item = null) => {
  editing.value = !!item;
  currentId.value = item?.id || null;

  form.value = item
    ? { ...item }
    : {
        maSanPham: "",
        tenSanPham: "",
        moTa: "",
        trangThai: "Hoạt động"
      };

  show.value = true;
};

/* ================= SAVE ================= */
const save = async () => {
  if (editing.value) {
    await updateSanPham(currentId.value, form.value);
  } else {
    await createSanPham(form.value);
  }

  show.value = false;
  fetchData();
};

const handleDelete = async (id) => {
  if (confirm("Bạn chắc chắn muốn xoá sản phẩm này?")) {
    await deleteSanPham(id);
    fetchData();
  }
};

onMounted(fetchData);
</script>

<template>
  <div class="page">
    <h2 class="title">Quản lý Sản Phẩm</h2>

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
          <th>Ngày tạo</th>
          <th>Trạng thái</th>
          <th>Hành động</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="item in filteredList" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.maSanPham }}</td>
          <td>{{ item.tenSanPham }}</td>
          <td>{{ item.moTa }}</td>
          <td>{{ item.ngayTao?.substring(0,10) }}</td>
          <td>{{ item.trangThai }}</td>
          <td>
            <button class="btn-edit" @click="open(item)">Sửa</button>
            <button class="btn-delete" @click="handleDelete(item.id)">
              Xoá
            </button>
          </td>
        </tr>

        <tr v-if="filteredList.length === 0">
          <td colspan="7">Không có dữ liệu</td>
        </tr>
      </tbody>
    </table>

    <!-- MODAL -->
    <div v-if="show" class="global-overlay">
      <div class="global-modal">
        <h3>
          {{ editing ? "Cập nhật Sản Phẩm" : "Thêm Sản Phẩm" }}
        </h3>

        <input v-model="form.maSanPham" placeholder="Mã sản phẩm" />
        <input v-model="form.tenSanPham" placeholder="Tên sản phẩm" />
        <input v-model="form.moTa" placeholder="Mô tả" />

        <select v-model="form.trangThai">
          <option value="Hoạt động">Hoạt động</option>
          <option value="Ngừng hoạt động">Ngừng hoạt động</option>
        </select>

        <div class="modal-actions">
          <button class="btn-cancel" @click="show = false">Huỷ</button>
          <button class="btn-primary" @click="save">Lưu</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  padding: 30px;
}

/* Title */
.title {
  font-size: 26px;
  font-weight: 600;
  margin-bottom: 24px;
  color: #ffffff;
}

/* Top bar */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.top-bar input {
  padding: 10px 14px;
  width: 300px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  transition: 0.2s;
  font-size: 14px;
}

.top-bar input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

/* Table */
.table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  background: white;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0,0,0,0.04);
}

.table th,
.table td {
  padding: 14px;
  text-align: center;
}

.table th {
  background: #f8fafc;
  font-weight: 600;
  color: #475569;
}

.table td {
  border-top: 1px solid #f1f5f9;
}

.table tbody tr {
  transition: background-color 0.2s ease;
}

.table tbody tr:hover {
  background: #f9fafb;
}

/* Buttons */
button {
  border: none;
  border-radius: 8px;
  padding: 7px 14px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s ease;
}

.btn-primary {
  background: #2563eb;
  color: white;
}

.btn-primary:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
}

.btn-edit {
  background: #f39c12;
  color: white;
  margin-right: 6px;
}

.btn-edit:hover {
  background: #d97706;
  transform: translateY(-1px);
}

.btn-delete {
  background: #e74c3c;
  color: white;
}

.btn-delete:hover {
  background: #dc2626;
  transform: translateY(-1px);
}

.btn-cancel {
  background: #ccc;
}

.btn-cancel:hover {
  background: #b5b5b5;
}

/* Modal overlay */
.global-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);

  display: flex;
  justify-content: center;
  align-items: center;

  backdrop-filter: blur(4px);
  z-index: 9999;
}

/* Modal */
.global-modal {
  width: 420px;
  background: #ffffff;
  padding: 28px;
  border-radius: 16px;

  display: flex;
  flex-direction: column;
  gap: 14px;

  box-shadow: 0 20px 50px rgba(0,0,0,0.15);
  animation: fadeIn 0.2s ease;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

/* Animation */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>