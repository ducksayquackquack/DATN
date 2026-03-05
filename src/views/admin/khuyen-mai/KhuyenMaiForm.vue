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
  <main class="wrap">
    <div class="card">
      <div class="head">
        <h1>Form khuyến mãi</h1>

        <div style="display:flex;gap:10px">
          <button type="button" class="btn" @click="goBack">
            ← Quay lại
          </button>

          <button type="button" class="btn primary" @click="save">
            Lưu
          </button>
        </div>
      </div>

      <div class="body">
        <div class="grid cols2">

          <div class="field">
            <label>Mã khuyến mãi</label>
            <input class="input" v-model="form.maKhuyenMai" />
          </div>

          <div class="field">
            <label>Tên chương trình</label>
            <input class="input" v-model="form.tenKhuyenMai" />
          </div>

          <div class="field">
            <label>Giá trị giảm</label>
            <input class="input" v-model="form.giaTri" type="number" />
          </div>

          <div class="field">
            <label>Ngày bắt đầu</label>
            <input class="input" v-model="form.ngayBatDau" type="date" />
          </div>

          <div class="field">
            <label>Ngày kết thúc</label>
            <input class="input" v-model="form.ngayKetThuc" type="date" />
          </div>

          <div class="field">
            <label>Trạng thái</label>
            <select
              class="input"
              :class="form.trangThai === 'Hoạt động' ? 'status-ok' : 'status-bad'"
              v-model="form.trangThai"
            >
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
          </div>

        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.input {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d8dee9;
  background: #fff;
  font-size: 14px;
}
.input:focus {
  outline: none;
  border-color: #4f46e5;
}
.status-ok {
  background: #dcfce7;
  color: #166534;
  border-color: #86efac;
  font-weight: 500;
}

.status-bad {
  background: #fee2e2;
  color: #991b1b;
  border-color: #fca5a5;
  font-weight: 500;
}
</style>