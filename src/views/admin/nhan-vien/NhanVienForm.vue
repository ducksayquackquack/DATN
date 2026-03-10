<script setup>
import { reactive, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  createNhanVien,
  updateNhanVien,
  getNhanVienById
} from "../../../services/nhanVienService"

const router = useRouter()
const route = useRoute()
const id = route.params.id

const form = reactive({
  code: "",
  name: "",
  email: "",
  phone: "",
  role: "STAFF",
  status: "Active",
  password: "",
  note: ""
})

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
        : "STAFF"

    form.note = data.ghiChu || ""

    form.status =
      data.trangThaiHoatDong === "Ngừng hoạt động"
        ? "Inactive"
        : "Active"
  }
})

async function save() {
  try {
    const payload = {
      maNhanVien: form.code,
      tenNhanVien: form.name,
      soDienThoai: form.phone,
      email: form.email,
      ghiChu: form.note,
      matKhau: form.password || null,
      chucVu: { tenChucVu: form.role },
      trangThaiHoatDong:
        form.status === "Inactive"
          ? "Ngừng hoạt động"
          : "Hoạt động"
    }

    if (id) {
      await updateNhanVien(id, payload)
    } else {
      await createNhanVien(payload)
    }

    router.push("/admin/nhan-vien/list")

  } catch (err) {
    console.error(err)
    alert("Lưu thất bại!")
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
            <label>Họ tên</label>
            <input v-model="form.name" placeholder="VD: Nguyễn Văn C"/>
          </div>

          <div class="field">
            <label>Vai trò</label>
            <select v-model="form.role">
              <option>ADMIN</option>
              <option>STAFF</option>
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
              <option>Active</option>
              <option>Inactive</option>
            </select>
          </div>

          <div class="field">
            <label>Mật khẩu</label>
            <input type="text" v-model="form.password"
                   placeholder="Để trống nếu không đổi"/>
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
  background: #fff;
  font-size: 14px;
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
</style>