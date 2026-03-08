<script setup>
import { reactive, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import axios from "axios"

const router = useRouter()
const route = useRoute()
const id = route.params.id

const API = "http://localhost:8080/api/khuyen-mai"

function goBack() {
  router.push("/admin/khuyen-mai/list")
}

const form = reactive({
  maKhuyenMai: "",
  tenKhuyenMai: "",
  giaTri: "",
  ngayBatDau: "",
  ngayKetThuc: "",
  trangThai: "Hoạt động"
})

onMounted(async () => {
  if (id) {
    const res = await axios.get(`${API}/${id}`)

    form.maKhuyenMai = res.data.maKhuyenMai
    form.tenKhuyenMai = res.data.tenKhuyenMai
    form.giaTri = res.data.giaTri
    form.ngayBatDau = res.data.ngayBatDau
      ? res.data.ngayBatDau.substring(0, 10)
      : ""
    form.ngayKetThuc = res.data.ngayKetThuc
      ? res.data.ngayKetThuc.substring(0, 10)
      : ""
    form.trangThai = res.data.trangThai
  }
})

async function save() {
  try {
    const payload = {
      maKhuyenMai: form.maKhuyenMai,
      tenKhuyenMai: form.tenKhuyenMai,
      giaTri: Number(form.giaTri),
      ngayBatDau: form.ngayBatDau + "T00:00:00",
      ngayKetThuc: form.ngayKetThuc + "T00:00:00",
      trangThai: form.trangThai
    }

    if (id) {
      await axios.put(`${API}/${id}`, payload)
    } else {
      await axios.post(API, payload)
    }

    router.push("/admin/khuyen-mai/list")
  } catch (err) {
    console.error("SAVE ERROR:", err)
  }
}
</script>

<template>
  <div class="form-page">
    <div class="page-header">
      <h1 class="page-title">{{ id ? 'Cập nhật' : 'Tạo mới' }} khuyến mãi</h1>
      <div class="header-actions">
        <button class="btn-secondary" @click="goBack">
          <span class="material-icons-outlined">arrow_back</span>
          Quay lại
        </button>
        <button class="btn-primary" @click="save">
          <span class="material-icons-outlined">save</span>
          Lưu
        </button>
      </div>
    </div>

    <div class="form-card">
      <div class="form-section">
        <h3 class="section-title">Thông tin khuyến mãi</h3>
        
        <div class="form-grid">
          <div class="form-group">
            <label class="form-label">
              Mã khuyến mãi <span class="required">*</span>
            </label>
            <input 
              v-model="form.maKhuyenMai" 
              class="form-input" 
              placeholder="Nhập mã khuyến mãi..."
            />
          </div>

          <div class="form-group">
            <label class="form-label">
              Tên chương trình <span class="required">*</span>
            </label>
            <input 
              v-model="form.tenKhuyenMai" 
              class="form-input" 
              placeholder="Nhập tên chương trình..."
            />
          </div>

          <div class="form-group">
            <label class="form-label">
              Giá trị giảm <span class="required">*</span>
            </label>
            <input 
              v-model="form.giaTri" 
              class="form-input" 
              type="number" 
              placeholder="Nhập giá trị..."
            />
            <p class="form-hint">Ví dụ: 20 (cho 20%), 50000 (cho 50,000đ)</p>
          </div>

          <div class="form-group">
            <label class="form-label">
              Trạng thái <span class="required">*</span>
            </label>
            <select 
              v-model="form.trangThai" 
              class="form-select"
              :class="form.trangThai === 'Hoạt động' ? 'status-active' : 'status-inactive'"
            >
              <option value="Hoạt động">Hoạt động</option>
              <option value="Ngừng hoạt động">Ngừng hoạt động</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">
              Ngày bắt đầu <span class="required">*</span>
            </label>
            <input 
              v-model="form.ngayBatDau" 
              class="form-input" 
              type="date"
              @click="$event.target.showPicker()"
            />
          </div>

          <div class="form-group">
            <label class="form-label">
              Ngày kết thúc <span class="required">*</span>
            </label>
            <input 
              v-model="form.ngayKetThuc" 
              class="form-input" 
              type="date"
              @click="$event.target.showPicker()"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/icon?family=Material+Icons+Outlined');

.form-page {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn-secondary,
.btn-primary {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary {
  background: white;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover {
  background: #f3f4f6;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.btn-secondary .material-icons-outlined,
.btn-primary .material-icons-outlined {
  font-size: 20px;
}

.form-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 24px;
}

.form-section {
  margin-bottom: 32px;
}

.form-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f3f4f6;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
}

.required {
  color: #dc2626;
}

.form-input,
.form-select {
  padding: 10px 14px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 14px;
  color: #111827;
  outline: none;
  transition: all 0.2s;
  background: white;
}

.form-input:focus,
.form-select:focus {
  border-color: #1f9c5b;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-input::placeholder {
  color: #9ca3af;
}

.form-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.form-select {
  cursor: pointer;
}

.status-active {
  background: #dcfce7;
  color: #166534;
  border-color: #86efac;
  font-weight: 600;
}

.status-inactive {
  background: #fee2e2;
  color: #991b1b;
  border-color: #fca5a5;
  font-weight: 600;
}
</style>