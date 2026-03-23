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
  donViGiam: "PERCENT",
  giaTri: "",
  ngayBatDau: "",
  ngayKetThuc: "",
  trangThai: "Hoạt động"
})

async function generateCampaignCode() {
  try {
    const res = await axios.get(API)
    const rows = Array.isArray(res?.data) ? res.data : []
    const maxCode = rows.reduce((max, item) => {
      const code = String(item?.maKhuyenMai || "")
      const matched = code.match(/^KM(\d+)$/i)
      if (!matched) return max
      return Math.max(max, Number(matched[1] || 0))
    }, 0)
    return `KM${String(maxCode + 1).padStart(3, "0")}`
  } catch {
    return `KM${String(Date.now()).slice(-6)}`
  }
}

function validateForm() {
  if (!String(form.maKhuyenMai || "").trim()) return "Mã khuyến mãi không được để trống"
  if (!/^KM\d{3,}$/i.test(String(form.maKhuyenMai || "").trim())) return "Mã khuyến mãi phải theo định dạng KM + số (ví dụ: KM001)"
  if (!String(form.tenKhuyenMai || "").trim()) return "Tên chương trình không được để trống"

  const discountValue = Number(form.giaTri || 0)
  if (!Number.isFinite(discountValue) || discountValue <= 0) return "Giá trị giảm phải lớn hơn 0"

  if (form.donViGiam === "PERCENT" && discountValue > 100) {
    return "Giảm theo % không được vượt quá 100"
  }

  if (!form.ngayBatDau || !form.ngayKetThuc) return "Ngày bắt đầu và kết thúc không được để trống"
  if (new Date(form.ngayKetThuc) < new Date(form.ngayBatDau)) return "Ngày kết thúc phải sau ngày bắt đầu"
  return ""
}

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
    form.donViGiam = String(res.data?.donViGiam || "PERCENT").toUpperCase() === "VND" ? "VND" : "PERCENT"
  } else {
    form.maKhuyenMai = await generateCampaignCode()
  }
})

async function save() {
  const validateError = validateForm()
  if (validateError) {
    window.toast.warning(validateError)
    return
  }

  const action = id ? 'cập nhật' : 'tạo mới'
  const confirmed = await window.confirmDialog(`Bạn có chắc chắn muốn ${action} khuyến mãi "${form.tenKhuyenMai}"?`)
  if (!confirmed) {
    return
  }

  try {
    const payload = {
      maKhuyenMai: form.maKhuyenMai,
      tenKhuyenMai: form.tenKhuyenMai,
      giaTri: Number(form.giaTri),
      donViGiam: form.donViGiam,
      ngayBatDau: form.ngayBatDau + "T00:00:00",
      ngayKetThuc: form.ngayKetThuc + "T00:00:00",
      trangThai: form.trangThai
    }

    if (id) {
      await axios.put(`${API}/${id}`, payload)
      window.toast.success('Cập nhật khuyến mãi thành công')
    } else {
      await axios.post(API, payload)
      window.toast.success('Tạo khuyến mãi thành công')
    }

    router.push("/admin/khuyen-mai/list")
  } catch (err) {
    console.error("SAVE ERROR:", err)
    window.toast.error('Lưu khuyến mãi thất bại')
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
              value="Mã tự sinh"
              class="form-input auto-code-input"
              readonly
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
              Đơn vị giảm <span class="required">*</span>
            </label>
            <select v-model="form.donViGiam" class="form-select">
              <option value="PERCENT">Phần trăm (%)</option>
              <option value="VND">Tiền (VND)</option>
            </select>
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
            <p class="form-hint" v-if="form.donViGiam === 'PERCENT'">Đơn vị hiện tại: %. Ví dụ: 20 nghĩa là giảm 20%</p>
            <p class="form-hint" v-else>Đơn vị hiện tại: VND. Ví dụ: 50000 nghĩa là giảm 50.000đ</p>
          </div>

          <div class="form-group">
            <label class="form-label">

            .auto-code-input {
              background: #f1f5f9;
              color: #64748b;
              border-color: #cbd5e1;
              font-weight: 600;
            }
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
  background: #dc2626;
  color: white;
}
.btn-primary:hover {
  background: #b91c1c;
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