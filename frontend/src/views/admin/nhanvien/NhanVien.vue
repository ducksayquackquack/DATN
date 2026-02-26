<script setup>
import { ref, onMounted, computed } from "vue";
import {
  getNhanVienList,
  createNhanVien,
  updateNhanVien,
  deleteNhanVien
} from "@/services/nhanVienService";

const list = ref([]);
const search = ref("");
const show = ref(false);
const editing = ref(false);
const currentId = ref(null);

const form = ref({
  maNhanVien: "",
  tenNhanVien: "",
  gioiTinh: "",
  ngaySinh: "",
  soDienThoai: "",
  diaChi: "",
  trangThaiHoatDong: "Hoạt động"
});

const fetchData = async () => {
  const { data } = await getNhanVienList();
  list.value = data;
};

const filteredList = computed(() =>
  !search.value
    ? list.value
    : list.value.filter(i =>
        i.maNhanVien?.toLowerCase().includes(search.value.toLowerCase()) ||
        i.tenNhanVien?.toLowerCase().includes(search.value.toLowerCase()) ||
        i.id.toString().includes(search.value)
      )
);

const open = (item = null) => {
  editing.value = !!item;
  currentId.value = item?.id || null;
  form.value = item
    ? { ...item }
    : {
        maNhanVien: "",
        tenNhanVien: "",
        gioiTinh: "",
        ngaySinh: "",
        soDienThoai: "",
        diaChi: "",
        trangThaiHoatDong: "Hoạt động"
      };
  show.value = true;
};

const save = async () => {
  editing.value
    ? await updateNhanVien(currentId.value, form.value)
    : await createNhanVien(form.value);
  show.value = false;
  fetchData();
};

const remove = async id => {
  if (confirm("Xoá nhân viên này?")) {
    await deleteNhanVien(id);
    fetchData();
  }
};

onMounted(fetchData);
</script>

<template>
  <div class="page">
    <h2 class="title">Quản lý Nhân Viên</h2>

    <div class="top-bar">
      <input v-model="search" placeholder="Tìm theo ID, Mã, Tên..." />
      <button class="btn-primary" @click="open()">Thêm mới</button>
    </div>

    <table class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Mã</th>
          <th>Tên</th>
          <th>Giới tính</th>
          <th>Ngày sinh</th>
          <th>SĐT</th>
          <th>Địa chỉ</th>
          <th>Trạng thái</th>
          <th>Hành động</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="item in filteredList" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.maNhanVien }}</td>
          <td>{{ item.tenNhanVien }}</td>
          <td>{{ item.gioiTinh }}</td>
          <td>{{ item.ngaySinh?.substring(0,10) }}</td>
          <td>{{ item.soDienThoai }}</td>
          <td>{{ item.diaChi }}</td>
          <td>{{ item.trangThaiHoatDong }}</td>
          <td>
            <button class="btn-edit" @click="open(item)">Sửa</button>
            <button class="btn-delete" @click="remove(item.id)">Xoá</button>
          </td>
        </tr>

        <tr v-if="filteredList.length === 0">
          <td colspan="9">Không có dữ liệu</td>
        </tr>
      </tbody>
    </table>

    <div v-if="show" class="global-overlay">
      <div class="global-modal">
        <h3>{{ editing ? "Cập nhật Nhân Viên" : "Thêm Nhân Viên" }}</h3>

        <input v-model="form.maNhanVien" placeholder="Mã nhân viên" />
        <input v-model="form.tenNhanVien" placeholder="Tên nhân viên" />
        <input v-model="form.gioiTinh" placeholder="Giới tính" />
        <input type="date" v-model="form.ngaySinh" />
        <input v-model="form.soDienThoai" placeholder="SĐT" />
        <input v-model="form.diaChi" placeholder="Địa chỉ" />

        <select v-model="form.trangThaiHoatDong">
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
/* Layout already provides card + background */
.page {
  padding: 0;
}

/* Title */
.title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 24px;
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
  color: #fff;
}

.btn-primary:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
}

.btn-edit {
  background: #f39c12;
  color: #fff;
  margin-right: 6px;
}

.btn-edit:hover {
  background: #d97706;
  transform: translateY(-1px);
}

.btn-delete {
  background: #e74c3c;
  color: #fff;
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

/* OVERLAY */
.global-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.5);
  backdrop-filter: blur(4px);

  display: flex;
  justify-content: center;
  align-items: center;

  z-index: 9999;
}

/* MODAL */
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

.global-modal input,
.global-modal select {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
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

/* Smooth modal animation */
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