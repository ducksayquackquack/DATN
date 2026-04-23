<script setup>
import { reactive, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  createNhanVien,
  updateNhanVien,
  getNhanVienById
} from "../../../services/nhanVienService"
import taiKhoanService from "../../../services/taiKhoanService"

const router = useRouter()
const route = useRoute()
const id = route.params.id

const form = reactive({
  code: "",
  name: "",
  email: "",
  phone: "",
  role: "NHAN_VIEN",
  status: "Hoạt động",
  password: "",
  note: "",
  taiKhoanId: null,
  chucVuId: null
})

// Validation errors
const errors = reactive({
  name: "",
  email: "",
  phone: ""
})

const toBackendRole = (role) => role === "ADMIN" ? "ADMIN" : "EMPLOYEE"

const isValidEmail = (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value || "").trim())

const checkEmailExists = async (email) => {
  const normalized = String(email || "").trim().toLowerCase()
  if (!normalized) return false
  try {
    const res = await taiKhoanService.getAll({ page: 0, size: 500 })
    const payload = res?.data
    const accounts = Array.isArray(payload)
      ? payload
      : (Array.isArray(payload?.content) ? payload.content : [])
    return accounts.some((acc) => String(acc?.email || "").trim().toLowerCase() === normalized)
  } catch {
    return false
  }
}

const taoMatKhauTam = () => {
  const part = String(Math.floor(100000 + Math.random() * 900000))
  return `DW@${part}`
}

const guiMailThongTinTaiKhoan = (email, password, role) => {
  if (!email || !password) return
  const vaiTroHienThi = role === 'ADMIN' ? 'Quản trị viên' : 'Nhân viên'
  const subject = encodeURIComponent('Thông tin tài khoản đăng nhập nội bộ')
  const body = encodeURIComponent(
    `Chào bạn,\n\n` +
    `Bạn đã được cấp tài khoản nội bộ DirtyWave.\n` +
    `Tài khoản: ${email}\n` +
    `Mật khẩu tạm thời: ${password}\n` +
    `Vai trò: ${vaiTroHienThi}\n` +
    `Trang đăng nhập nội bộ: http://localhost:5173/auth/staff-login\n\n` +
    `Vui lòng đổi mật khẩu ngay sau khi đăng nhập.`
  )
  window.location.href = `mailto:${encodeURIComponent(email)}?subject=${subject}&body=${body}`
}

function validate() {
  let valid = true

  // Họ tên: bắt buộc, tối thiểu 2 ký tự
  const trimmedName = (form.name || "").trim()
  if (!trimmedName) {
    errors.name = "Họ tên không được để trống"
    valid = false
  } else if (trimmedName.length < 2) {
    errors.name = "Họ tên phải có ít nhất 2 ký tự"
    valid = false
  } else {
    errors.name = ""
  }

  // Email: bắt buộc khi tạo mới
  const trimmedEmail = (form.email || "").trim()
  if (!id) {
    if (!trimmedEmail) {
      errors.email = "Email không được để trống"
      valid = false
    } else if (!isValidEmail(trimmedEmail)) {
      errors.email = "Email không đúng định dạng"
      valid = false
    } else {
      errors.email = ""
    }
  } else {
    if (trimmedEmail && !isValidEmail(trimmedEmail)) {
      errors.email = "Email không đúng định dạng"
      valid = false
    } else {
      errors.email = ""
    }
  }

  // SĐT: bắt buộc, 10–11 chữ số
  const trimmedPhone = (form.phone || "").trim()
  if (!trimmedPhone) {
    errors.phone = "Số điện thoại không được để trống"
    valid = false
  } else if (!/^\d{10,11}$/.test(trimmedPhone)) {
    errors.phone = "SĐT phải gồm 10–11 chữ số"
    valid = false
  } else {
    errors.phone = ""
  }

  return valid
}

function goBack() {
  router.push("/admin/nhan-vien/list")
}

onMounted(async () => {
  if (id) {
    const res = await getNhanVienById(id)
    const data = res.data

    form.code = data.maNhanVien || ""
    form.name = data.tenNhanVien || ""
    form.phone = data.soDienThoai || ""
    form.email = data.email || data.taiKhoan?.email || ""
    form.role = data.chucVu?.tenChucVu === "ADMIN" ? "ADMIN" : "NHAN_VIEN"
    form.taiKhoanId = data.taiKhoan?.id || null
    form.chucVuId = data.chucVu?.id || null
    form.note = data.ghiChu || ""
    form.status = data.trangThaiHoatDong === "Ngừng hoạt động"
      ? "Ngừng hoạt động"
      : "Hoạt động"
  }
})

async function save() {
  if (!validate()) return

  try {
    const action = id ? "cập nhật" : "tạo mới"
    const confirmed = await window.confirmDialog?.(`Bạn có chắc muốn ${action} nhân viên này không?`) ?? confirm(`Bạn có chắc muốn ${action} nhân viên này không?`)
    if (!confirmed) return

    if (!id) {
      const normalizedEmail = String(form.email || "").trim().toLowerCase()
      if (!isValidEmail(normalizedEmail)) {
        errors.email = "Email không đúng định dạng"
        window.toast?.error?.("Email không đúng định dạng")
        return
      }
      const emailExists = await checkEmailExists(normalizedEmail)
      if (emailExists) {
        errors.email = "Email đã tồn tại"
        window.toast?.error?.("Email đã tồn tại, vui lòng dùng email khác")
        return
      }
      form.email = normalizedEmail
    }

    const matKhauTam = form.password || taoMatKhauTam()

    const nhanVienPayload = {
      maNhanVien: form.code,
      tenNhanVien: form.name.trim(),
      soDienThoai: form.phone.trim(),
      ghiChu: form.note,
      chucVu: form.chucVuId
        ? { id: form.chucVuId }
        : { maChucVu: toBackendRole(form.role) },
      trangThaiHoatDong: form.status === "Ngừng hoạt động"
        ? "Ngừng hoạt động"
        : "Hoạt động",
      taiKhoan: form.taiKhoanId ? { id: form.taiKhoanId } : null
    }

    if (id) {
      if (form.taiKhoanId) {
        await taiKhoanService.update(form.taiKhoanId, {
          email: form.email,
          vaiTro: toBackendRole(form.role),
          trangThaiHoatDong: form.status === "Ngừng hoạt động" ? "Ngừng hoạt động" : "Hoạt động",
          matKhau: form.password || undefined
        })
      }
      await updateNhanVien(id, nhanVienPayload)
      window.toast?.success?.("Cập nhật nhân viên thành công!")
    } else {
      try {
        const accountRes = await taiKhoanService.create({
          email: form.email.trim(),
          matKhau: matKhauTam,
          vaiTro: toBackendRole(form.role),
          trangThaiHoatDong: "Hoạt động",
          trangThaiTaiKhoan: "Kích hoạt"
        })

        const createdTaiKhoanId = accountRes?.data?.id
        if (!createdTaiKhoanId) {
          throw new Error("Không tạo được tài khoản cho nhân viên")
        }

        await createNhanVien({
          ...nhanVienPayload,
          taiKhoan: { id: createdTaiKhoanId }
        })

        guiMailThongTinTaiKhoan(form.email, matKhauTam, form.role)
      } catch {
        await createNhanVien({
          ...nhanVienPayload,
          taiKhoan: null,
          email: form.email,
          matKhau: matKhauTam,
          vaiTro: toBackendRole(form.role)
        })
        guiMailThongTinTaiKhoan(form.email, matKhauTam, form.role)
      }
      window.toast?.success?.("Thêm nhân viên thành công!")
    }

    router.push("/admin/nhan-vien/list")

  } catch (err) {
    console.error(err)
    window.toast?.error?.("Lưu thất bại: " + (err?.response?.data?.message || err?.message || "Lỗi không xác định"))
  }
}
</script>

<template>
  <main class="wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>{{ id ? 'Chỉnh sửa nhân viên' : 'Thêm nhân viên mới' }}</h1>
          <small class="muted">
            {{ id ? 'Cập nhật thông tin hồ sơ nhân viên' : 'Điền đầy đủ thông tin để tạo tài khoản nhân viên' }}
          </small>
        </div>

        <div style="display:flex;gap:10px">
          <button class="btn" @click="goBack">← Quay lại</button>
          <button class="btn primary" @click="save">Lưu</button>
        </div>
      </div>

      <div class="body">
        <div class="grid cols2">

          <!-- Mã nhân viên -->
          <div class="field">
            <label>Mã nhân viên</label>
            <input class="auto-code-input" readonly :value="id ? form.code : 'Mã tự sinh'" />
            <small class="hint">Mã được hệ thống tự động sinh khi tạo mới</small>
          </div>

          <!-- Họ tên (bắt buộc) -->
          <div class="field" :class="{ 'has-error': errors.name }">
            <label>Họ tên <span class="req">*</span></label>
            <input v-model="form.name" placeholder="VD: Nguyễn Văn C" @blur="validate" />
            <small v-if="errors.name" class="err-msg">{{ errors.name }}</small>
            <small v-else class="hint">Tối thiểu 2 ký tự, không để trống</small>
          </div>

          <!-- Vai trò -->
          <div class="field">
            <label>Vai trò <span class="req">*</span></label>
            <select v-model="form.role">
              <option value="ADMIN">Quản trị viên</option>
              <option value="NHAN_VIEN">Nhân viên bán hàng</option>
            </select>
            <small class="hint">Phân quyền truy cập vào hệ thống quản trị</small>
          </div>

          <!-- Email -->
          <div class="field" :class="{ 'has-error': errors.email }">
            <label>Email {{ !id ? '' : '' }} <span v-if="!id" class="req">*</span></label>
            <input type="email" v-model="form.email"
                   :placeholder="id ? 'Để trống nếu không đổi email' : 'VD: nhanvien@dirtywave.com'"
                   @blur="validate" />
            <small v-if="errors.email" class="err-msg">{{ errors.email }}</small>
            <small v-else class="hint">
              {{ id ? 'Dùng để đăng nhập. Cập nhật cẩn thận.' : 'Bắt buộc – dùng để đăng nhập hệ thống' }}
            </small>
          </div>

          <!-- SĐT (bắt buộc) -->
          <div class="field" :class="{ 'has-error': errors.phone }">
            <label>Số điện thoại <span class="req">*</span></label>
            <input type="tel" v-model="form.phone" placeholder="VD: 0912345678" maxlength="11" @blur="validate" />
            <small v-if="errors.phone" class="err-msg">{{ errors.phone }}</small>
            <small v-else class="hint">10–11 chữ số, không có dấu cách hoặc dấu gạch</small>
          </div>

          <!-- Trạng thái (chỉ sửa khi đã có nhân viên) -->
          <div class="field" v-if="id">
            <label>Trạng thái hoạt động</label>
            <select v-model="form.status">
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
            <small class="hint">Nhân viên ngừng hoạt động không thể đăng nhập</small>
          </div>

          <!-- Mật khẩu -->
          <div class="field" style="grid-column:1/-1">
            <label>Mật khẩu</label>
            <input type="text" v-model="form.password"
                   :placeholder="id ? 'Để trống để giữ nguyên mật khẩu hiện tại' : 'Để trống để hệ thống tự tạo mật khẩu tạm (VD: DW@123456)'"/>
            <small class="hint">
              {{ id
                  ? 'Chỉ điền nếu muốn đặt lại mật khẩu. Hệ thống sẽ gửi email thông báo.'
                  : 'Nếu để trống, hệ thống tự sinh mật khẩu và gửi email cho nhân viên.' }}
            </small>
          </div>

          <!-- Ghi chú -->
          <div class="field" style="grid-column:1/-1">
            <label>Ghi chú nội bộ</label>
            <textarea v-model="form.note" placeholder="VD: Nhân viên ca sáng, phụ trách khu vực bán hàng 1..."></textarea>
            <small class="hint">Ghi chú chỉ admin thấy, không hiển thị cho nhân viên</small>
          </div>

        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
input, select, textarea {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d8dee9;
  background-color: #fff;
  font-size: 14px;
  transition: border-color 0.15s;
}
select { padding-right: 34px; }

input:focus, select:focus, textarea:focus {
  outline: none;
  border-color: #4f46e5;
}

textarea { min-height: 90px; resize: vertical; }

.auto-code-input {
  background: #f1f5f9;
  color: #64748b;
  border-color: #cbd5e1;
  font-weight: 600;
}

/* Required star */
.req { color: #e53e3e; margin-left: 3px; }

/* Hint text */
.hint {
  display: block;
  margin-top: 4px;
  font-size: 11.5px;
  color: #94a3b8;
  line-height: 1.4;
}

/* Validation error */
.has-error input, .has-error select, .has-error textarea {
  border-color: #e53e3e;
}
.err-msg {
  display: block;
  margin-top: 4px;
  font-size: 11.5px;
  color: #e53e3e;
  font-weight: 500;
}

@media (max-width: 768px) {
  .head { flex-direction: column; align-items: flex-start; gap: 12px; }
}
</style>