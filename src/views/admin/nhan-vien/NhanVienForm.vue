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

const toBackendRole = (role) => role === "ADMIN" ? "ADMIN" : "EMPLOYEE"

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

  window.location.href = `mailto:${email}?subject=${subject}&body=${body}`
}

function goBack() {
  router.push("/admin/nhan-vien/list")
}

onMounted(async () => {
  if (id) {
    const res = await getNhanVienById(id)

    console.log("DETAIL:", res.data)

    const data = res.data

    form.code = data.maNhanVien || ""
    form.name = data.tenNhanVien || ""
    form.phone = data.soDienThoai || ""

    form.email =
      data.email ||
      data.taiKhoan?.email ||
      ""

    form.role =
      data.chucVu?.tenChucVu === "ADMIN"
        ? "ADMIN"
        : "NHAN_VIEN"

    form.taiKhoanId = data.taiKhoan?.id || null
    form.chucVuId = data.chucVu?.id || null

    form.note = data.ghiChu || ""

    form.status =
      data.trangThaiHoatDong === "Ngừng hoạt động"
        ? "Ngừng hoạt động"
        : "Hoạt động"
  }
})

async function save() {
  try {
    const action = id ? "cập nhật" : "tạo mới"
    const confirmed = await window.confirm(`Bạn có chắc muốn ${action} nhân viên này không?`)
    if (!confirmed) return

    const matKhauTam = form.password || taoMatKhauTam()

    const nhanVienPayload = {
      maNhanVien: form.code,
      tenNhanVien: form.name,
      soDienThoai: form.phone,
      chucVu: form.chucVuId
        ? { id: form.chucVuId }
        : { maChucVu: toBackendRole(form.role) },
      trangThaiHoatDong:
        form.status === "Ngừng hoạt động"
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
    } else {
      try {
        const accountRes = await taiKhoanService.create({
          email: form.email,
          matKhau: matKhauTam,
          vaiTro: toBackendRole(form.role),
          trangThaiHoatDong: form.status === "Ngừng hoạt động" ? "Ngừng hoạt động" : "Hoạt động",
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
    }

    router.push("/admin/nhan-vien/list")

  } catch (err) {
    console.error(err)
    window.toast.error("Lưu thất bại!")
  }
}
</script>

<template>
  <main class="wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>Form nhân viên</h1>
          <small class="muted">
            Tạo mới / chỉnh sửa nhân viên
          </small>
        </div>

        <div style="display:flex;gap:10px">
          <button class="btn" @click="goBack">
            ← Quay lại
          </button>

          <button class="btn primary" @click="save">
            Lưu
          </button>
        </div>
      </div>

      <div class="body">
        <div class="grid cols2">

          <div class="field">
            <label>Mã nhân viên</label>
            <input class="auto-code-input" readonly value="Mã tự sinh" />
          </div>

          <div class="field">
            <label>Họ tên</label>
            <input v-model="form.name" placeholder="VD: Nguyễn Văn C"/>
          </div>

          <div class="field">
            <label>Vai trò</label>
            <select v-model="form.role">
              <option>ADMIN</option>
              <option>NHAN_VIEN</option>
            </select>
          </div>

          <div class="field">
            <label>Email</label>
            <input type="email" v-model="form.email"/>
          </div>

          <div class="field">
            <label>SĐT</label>
            <input type="tel" v-model="form.phone"/>
          </div>

          <div class="field">
            <label>Trạng thái</label>
            <select v-model="form.status">
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
          </div>

          <div class="field">
            <label>Mật khẩu</label>
            <input type="text" v-model="form.password"
                   placeholder="Để trống để hệ thống tự tạo mật khẩu tạm"/>
          </div>

          <div class="field" style="grid-column:1/-1">
            <label>Ghi chú</label>
            <textarea v-model="form.note"></textarea>
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
}
select {
  padding-right: 34px;
}

input:focus,
select:focus,
textarea:focus {
  outline: none;
  border-color: #4f46e5;
}

textarea {
  min-height: 90px;
  resize: vertical;
}

.auto-code-input {
  background: #f1f5f9;
  color: #64748b;
  border-color: #cbd5e1;
  font-weight: 600;
}
</style>