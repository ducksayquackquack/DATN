<script setup>
import { ref, onMounted, computed } from "vue";
import {
  getTaiKhoanList,
  createTaiKhoan,
  updateTaiKhoan,
  deleteTaiKhoan
} from "@/services/taiKhoanService";

const list = ref([]);
const search = ref("");
const show = ref(false);
const editing = ref(false);
const currentId = ref(null);

const roles = ["ADMIN", "EMPLOYEE", "CUSTOMER"];

const form = ref({
  username: "",
  password: "",
  vaiTro: "EMPLOYEE",
  trangThaiHoatDong: "Hoạt động"
});

const fetchData = async () => {
  const { data } = await getTaiKhoanList();
  list.value = data;
};

const filteredList = computed(() =>
  !search.value
    ? list.value
    : list.value.filter(i =>
        i.email?.toLowerCase().includes(search.value.toLowerCase()) ||
        i.vaiTro?.toLowerCase().includes(search.value.toLowerCase()) ||
        i.id.toString().includes(search.value)
      )
);

const open = (item = null) => {
  editing.value = !!item;
  currentId.value = item?.id || null;

  form.value = item
    ? {
        username: item.email,
        vaiTro: item.vaiTro,
        trangThaiHoatDong: item.trangThaiHoatDong
      }
    : {
        username: "",
        password: "",
        vaiTro: "EMPLOYEE",
        trangThaiHoatDong: "Hoạt động"
      };

  show.value = true;
};

const save = async () => {
  if (editing.value) {
    await updateTaiKhoan(currentId.value, {
      email: form.value.username,
      vaiTro: form.value.vaiTro,
      trangThaiHoatDong: form.value.trangThaiHoatDong
    });
  } else {
    await createTaiKhoan({
      email: form.value.username,
      matKhau: form.value.password,
      vaiTro: form.value.vaiTro,
      trangThaiHoatDong: form.value.trangThaiHoatDong
    });
  }

  show.value = false;
  fetchData();
};

const remove = async id => {
  if (confirm("Xoá tài khoản này?")) {
    await deleteTaiKhoan(id);
    fetchData();
  }
};

onMounted(fetchData);
</script>

<template>
  <div class="admin-container">
    <h2 class="title">Quản lý Tài Khoản</h2>

    <div class="top-bar">
      <input v-model="search" placeholder="Tìm theo ID, Username, Role..." />
      <button class="btn-primary" @click="open()">Thêm mới</button>
    </div>

    <table class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Username</th>
          <th>Role</th>
          <th>Trạng thái</th>
          <th>Hành động</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="item in filteredList" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.email }}</td>
          <td>{{ item.vaiTro }}</td>
          <td>{{ item.trangThaiHoatDong }}</td>
          <td>
            <button class="btn-edit" @click="open(item)">Sửa</button>
            <button class="btn-delete" @click="remove(item.id)">Xoá</button>
          </td>
        </tr>

        <tr v-if="filteredList.length === 0">
          <td colspan="5">Không có dữ liệu</td>
        </tr>
      </tbody>
    </table>

    <div v-if="show" class="global-overlay">
      <div class="global-modal">
        <h3>{{ editing ? "Cập nhật Tài Khoản" : "Thêm Tài Khoản" }}</h3>

        <input v-model="form.username" placeholder="Username" />

        <input
          v-if="!editing"
          type="password"
          v-model="form.password"
          placeholder="Password"
        />

        <select v-model="form.vaiTro">
          <option v-for="r in roles" :key="r" :value="r">
            {{ r }}
          </option>
        </select>

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
/* Layout already provides background + card */
.admin-container {
  padding: 0;
}

/* Title */
.title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 24px;
  color: #1e293b;
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