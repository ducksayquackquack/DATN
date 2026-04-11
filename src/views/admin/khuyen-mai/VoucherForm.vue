<template>
  <div class="form-page">
    <div class="page-header">
      <h1 class="page-title">{{ isEdit ? 'Cập nhật' : 'Tạo mới' }} phiếu giảm giá</h1>
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
        <h3 class="section-title">Thông tin phiếu giảm giá</h3>
        
        <div class="form-grid">
          <div class="form-group">
            <label class="form-label">
              Mã phiếu <span class="required">*</span>
            </label>
            <input 
              value="Mã tự sinh"
              class="form-input auto-code-input"
              readonly
            />
          </div>

          <div class="form-group">
            <label class="form-label">
              Tên phiếu <span class="required">*</span>
            </label>
            <input 
              v-model="form.tenPhieuGiamGia" 
              class="form-input" 
              placeholder="Nhập tên phiếu..."
            />
          </div>

          <div class="form-group full-width">
            <label class="form-label">Mô tả</label>
            <textarea 
              v-model="form.moTa" 
              class="form-textarea" 
              rows="3"
              placeholder="Nhập mô tả phiếu giảm giá..."
            ></textarea>
          </div>

          <div class="form-group">
            <label class="form-label">
              Hình thức giảm <span class="required">*</span>
            </label>
            <select v-model="form.hinhThucGiam" class="form-select">
              <option :value="true">Phần trăm (%)</option>
              <option :value="false">Số tiền cố định (VNĐ)</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">
              Giá trị giảm <span class="required">*</span>
            </label>
            <input 
              v-model.number="form.giaTriGiamGia" 
              class="form-input" 
              type="number"
              :placeholder="form.hinhThucGiam ? 'Nhập % (1-100)' : 'Nhập số tiền'"
              :min="form.hinhThucGiam ? 1 : 1000"
              :max="form.hinhThucGiam ? 100 : undefined"
            />
          </div>

          <div class="form-group">
            <label class="form-label">Giảm tối đa (VNĐ)</label>
            <input 
              v-model.number="form.soTienGiamToiDa" 
              class="form-input" 
              type="number"
              placeholder="0 = không giới hạn"
              :disabled="!form.hinhThucGiam"
            />
            <p class="form-hint">Voucher %: nhập mức trần | Voucher tiền cố định: tự lấy theo Đơn hàng tối thiểu</p>
          </div>

          <div class="form-group">
            <label class="form-label">
              Đơn hàng tối thiểu (VNĐ) <span class="required">*</span>
            </label>
            <input 
              v-model.number="form.hoaDonToiThieu" 
              class="form-input" 
              type="number"
              placeholder="Nhập giá trị đơn hàng tối thiểu"
            />
          </div>

          <div class="form-group">
            <label class="form-label">
              Số lượng sử dụng <span class="required">*</span>
            </label>
            <input 
              v-model.number="form.soLuongSuDung" 
              class="form-input" 
              type="number"
              placeholder="Nhập số lượng"
              min="1"
            />
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

          <div class="form-group">
            <label class="form-label">
              Trạng thái <span class="required">*</span>
            </label>
            <select 
              v-model="form.trangThai" 
              class="form-select"
              :class="form.trangThai ? 'status-active' : 'status-inactive'"
            >
              <option :value="true">Hoạt động</option>
              <option :value="false">Ngừng hoạt động</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted, computed } from "vue"
import { useRouter, useRoute } from "vue-router"
import axios from "axios"
import { normalizeVoucherData } from "../../../services/khuyenMaiService"

const router = useRouter()
const route = useRoute()
const id = route.params.id
const isEdit = computed(() => id && id !== 'new')

const API = "http://localhost:8080/api/phieu-giam-gia"

function goBack() {
  router.push("/admin/khuyen-mai/list?tab=vouchers")
}

const form = reactive({
  maPhieuGiamGia: "",
  tenPhieuGiamGia: "",
  moTa: "",
  hinhThucGiam: true, // true = percentage, false = fixed amount
  giaTriGiamGia: 0,
  soTienGiamToiDa: 0,
  hoaDonToiThieu: 0,
  soLuongSuDung: 1,
  ngayBatDau: new Date().toISOString().split('T')[0],
  ngayKetThuc: "",
  trangThai: true
})

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await axios.get(`${API}/${id}`)
      const data = normalizeVoucherData(res.data)
      
      Object.assign(form, {
        maPhieuGiamGia: data.maPhieuGiamGia || "",
        tenPhieuGiamGia: data.tenPhieuGiamGia || "",
        moTa: data.moTa || "",
        hinhThucGiam: data.hinhThucGiam ?? true,
        giaTriGiamGia: data.giaTriGiamGia ?? 0,
        soTienGiamToiDa: data.soTienGiamToiDa ?? 0,
        hoaDonToiThieu: data.hoaDonToiThieu ?? 0,
        soLuongSuDung: data.soLuongSuDung ?? 1,
        ngayBatDau: data.ngayBatDau ? data.ngayBatDau.substring(0, 10) : "",
        ngayKetThuc: data.ngayKetThuc ? data.ngayKetThuc.substring(0, 10) : "",
        trangThai: data.trangThai ?? true
      })
    } catch (error) {
      console.error('Error loading voucher:', error)
      window.toast.error('Không thể tải thông tin phiếu giảm giá')
    }
  } else {
    form.maPhieuGiamGia = await generateVoucherCode()
  }
})

async function generateVoucherCode() {
  try {
    const res = await axios.get(API)
    const rows = Array.isArray(res?.data) ? res.data : []
    const maxCode = rows.reduce((max, item) => {
      const code = String(item?.maPhieuGiamGia || "")
      const matched = code.match(/^PGG(\d+)$/i)
      if (!matched) return max
      return Math.max(max, Number(matched[1] || 0))
    }, 0)
    return `PGG${String(maxCode + 1).padStart(3, "0")}`
  } catch {
    return `PGG${String(Date.now()).slice(-6)}`
  }
}

async function save() {
  try {
    // Validation
    if (!form.maPhieuGiamGia || !form.tenPhieuGiamGia) {
      window.toast.warning('Vui lòng nhập đầy đủ thông tin bắt buộc')
      return
    }

    if (!/^PGG\d{3,}$/i.test(String(form.maPhieuGiamGia || '').trim())) {
      window.toast.warning('Mã phiếu phải theo định dạng PGG + số (ví dụ: PGG001)')
      return
    }

    if (form.hinhThucGiam === true && (form.giaTriGiamGia < 1 || form.giaTriGiamGia > 100)) {
      window.toast.warning('Giá trị giảm theo % phải từ 1-100')
      return
    }

    if (Number(form.giaTriGiamGia || 0) <= 0) {
      window.toast.warning('Giá trị giảm phải lớn hơn 0')
      return
    }

    if (Number(form.hoaDonToiThieu || 0) < 0) {
      window.toast.warning('Đơn hàng tối thiểu không hợp lệ')
      return
    }

    if (Number(form.soLuongSuDung || 0) <= 0) {
      window.toast.warning('Số lượng sử dụng phải lớn hơn 0')
      return
    }

    if (!form.ngayBatDau || !form.ngayKetThuc) {
      window.toast.warning('Vui lòng chọn ngày bắt đầu và kết thúc')
      return
    }

    if (new Date(form.ngayKetThuc) < new Date(form.ngayBatDau)) {
      window.toast.warning('Ngày kết thúc phải sau ngày bắt đầu')
      return
    }

    const minOrderValue = Number(form.hoaDonToiThieu || 0)
    const maxDiscountValue = form.hinhThucGiam
      ? Number(form.soTienGiamToiDa || 0)
      : minOrderValue

    const payload = {
      maPhieuGiamGia: form.maPhieuGiamGia,
      tenPhieuGiamGia: form.tenPhieuGiamGia,
      moTa: form.moTa,
      hinhThucGiam: form.hinhThucGiam,
      giaTriGiamGia: Number(form.giaTriGiamGia),
      soTienGiamToiDa: maxDiscountValue,
      hoaDonToiThieu: minOrderValue,
      soLuongSuDung: Number(form.soLuongSuDung),
      ngayBatDau: form.ngayBatDau + "T00:00:00",
      ngayKetThuc: form.ngayKetThuc + "T23:59:59",
      trangThai: form.trangThai
    }

    if (isEdit.value) {
      await axios.put(`${API}/${id}`, payload)
      window.toast.success('Cập nhật phiếu giảm giá thành công!')
    } else {
      await axios.post(API, payload)
      window.toast.success('Tạo phiếu giảm giá thành công!')
    }

    router.push("/admin/khuyen-mai/list?tab=vouchers")
  } catch (err) {
    console.error("SAVE ERROR:", err)
    window.toast.error('Lỗi: ' + (err.response?.data?.message || err.message))
  }
}
</script>

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

.form-group.full-width {
  grid-column: 1 / -1;
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
.form-select,
.form-textarea {
  padding: 10px 14px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 14px;
  color: #111827;
  outline: none;
  transition: all 0.2s;
  background-color: white;
  font-family: inherit;
}
.form-select {
  padding-right: 34px;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-input:disabled {
  background: #f3f4f6;
  color: #9ca3af;
  cursor: not-allowed;
}

.auto-code-input {
  background: #f1f5f9;
  color: #64748b;
  border-color: #cbd5e1;
  font-weight: 600;
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: #9ca3af;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
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
