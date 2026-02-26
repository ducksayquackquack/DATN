<script setup>
import { ref, onMounted, computed } from "vue";
import {
  getKhuyenMaiList,
  createKhuyenMai,
  updateKhuyenMai,
  deleteKhuyenMai
} from "@/services/khuyenMaiService";

const list = ref([]);
const search = ref("");
const show = ref(false);
const editing = ref(false);
const currentId = ref(null);

const form = ref({
  maKhuyenMai: "",
  tenKhuyenMai: "",
  giaTri: 0,
  ngayBatDau: "",
  ngayKetThuc: "",
  trangThai: "Hoạt động"
});

const fetchData = async () => {
  const { data } = await getKhuyenMaiList();
  list.value = data;
};

const filteredList = computed(() =>
  !search.value
    ? list.value
    : list.value.filter(i =>
        i.maKhuyenMai?.toLowerCase().includes(search.value.toLowerCase()) ||
        i.tenKhuyenMai?.toLowerCase().includes(search.value.toLowerCase()) ||
        i.id.toString().includes(search.value)
      )
);

const open = (item = null) => {
  editing.value = !!item;
  currentId.value = item?.id || null;
  form.value = item
    ? { ...item }
    : {
        maKhuyenMai: "",
        tenKhuyenMai: "",
        giaTri: 0,
        ngayBatDau: "",
        ngayKetThuc: "",
        trangThai: "Hoạt động"
      };
  show.value = true;
};

const save = async () => {
  editing.value
    ? await updateKhuyenMai(currentId.value, form.value)
    : await createKhuyenMai(form.value);
  show.value = false;
  fetchData();
};

const remove = async id => {
  if (confirm("Xoá khuyến mãi này?")) {
    await deleteKhuyenMai(id);
    fetchData();
  }
};

onMounted(fetchData);
</script>

<template>
  <div class="page">
    <h2 class="title">Quản lý Khuyến Mãi</h2>

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
          <th>Giá trị (%)</th>
          <th>Ngày bắt đầu</th>
          <th>Ngày kết thúc</th>
          <th>Trạng thái</th>
          <th>Hành động</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="item in filteredList" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.maKhuyenMai }}</td>
          <td>{{ item.tenKhuyenMai }}</td>
          <td>{{ item.giaTri }}</td>
          <td>{{ item.ngayBatDau?.substring(0,10) }}</td>
          <td>{{ item.ngayKetThuc?.substring(0,10) }}</td>
          <td>{{ item.trangThai }}</td>
          <td>
            <button class="btn-edit" @click="open(item)">Sửa</button>
            <button class="btn-delete" @click="remove(item.id)">Xoá</button>
          </td>
        </tr>

        <tr v-if="filteredList.length === 0">
          <td colspan="8">Không có dữ liệu</td>
        </tr>
      </tbody>
    </table>

    <div v-if="show" class="global-overlay">
      <div class="global-modal">
        <h3>{{ editing ? "Cập nhật Khuyến Mãi" : "Thêm Khuyến Mãi" }}</h3>

        <input v-model="form.maKhuyenMai" placeholder="Mã" />
        <input v-model="form.tenKhuyenMai" placeholder="Tên" />
        <input type="number" v-model="form.giaTri" placeholder="Giá trị (%)" />
        <input type="date" v-model="form.ngayBatDau" />
        <input type="date" v-model="form.ngayKetThuc" />

        <select v-model="form.trangThai">
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
.page { padding: 20px; }

.title { font-size: 22px; font-weight: 600; margin-bottom: 20px; }

.top-bar { display: flex; justify-content: space-between; margin-bottom: 15px; }

.top-bar input {
  padding: 8px;
  width: 280px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.table { width: 100%; border-collapse: collapse; }

.table th, .table td {
  border: 1px solid #ddd;
  padding: 10px;
  text-align: center;
}

.table th { background: #f5f5f5; }

button {
  border: none;
  border-radius: 4px;
  padding: 6px 10px;
  cursor: pointer;
}

.btn-primary { background: #2563eb; color: #fff; }
.btn-edit { background: #f39c12; color: #fff; margin-right: 5px; }
.btn-delete { background: #e74c3c; color: #fff; }
.btn-cancel { background: #ccc; }

.global-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.global-modal {
  width: 400px;
  background: #fff;
  padding: 25px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.global-modal input,
.global-modal select {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>