<script setup>
import { reactive, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  createKichThuoc,
  updateKichThuoc,
  getKichThuocById
} from "../../../services/kichThuocService"

const router = useRouter()
const route = useRoute()
const id = route.params.id

function goBack() {
  router.push("/admin/kich-thuoc/list")
}

function normalizeTrangThai(value) {
  if (!value) return "Ngừng hoạt động"
  if (value === "Active") return "Hoạt động"
  if (value === "Inactive") return "Ngừng hoạt động"
  return value
}

function mapToBE(value) {
  if (value === "Hoạt động") return "Active"
  if (value === "Ngừng hoạt động") return "Inactive"
  return value
}

const form = reactive({
  maKichThuoc: "",
  tenKichThuoc: "",
  trangThai: "Hoạt động",
  thuTu: "",
  ghiChu: ""
})

onMounted(async () => {
  if (id) {
    const res = await getKichThuocById(id)

    form.maKichThuoc = res.data.maKichThuoc || ""
    form.tenKichThuoc = res.data.tenKichThuoc || ""
    form.trangThai = normalizeTrangThai(res.data.trangThai)
    form.thuTu = res.data.thuTu || ""
    form.ghiChu = res.data.ghiChu || res.data.moTa || ""
  }
})

async function save() {
  const payload = {
    maKichThuoc: form.maKichThuoc,
    tenKichThuoc: form.tenKichThuoc,
    trangThai: mapToBE(form.trangThai),
    thuTu: form.thuTu,
    ghiChu: form.ghiChu
  }

  if (id) {
    await updateKichThuoc(id, payload)
  } else {
    await createKichThuoc(payload)
  }

  router.push("/admin/kich-thuoc/list")
}
</script>

<template>
  <main class="wrap">
    <div class="card">

      <div class="head">
        <div>
          <h1>Form kích thước</h1>
          <small class="muted">
            Tạo mới hoặc cập nhật size
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
            <label>Mã size</label>
            <input
              class="input"
              v-model="form.maKichThuoc"
              placeholder="VD: KT010"
            />
            <small class="muted">
              Có thể để BE tự sinh
            </small>
          </div>

          <div class="field">
            <label>Giá trị size</label>
            <input
              class="input"
              v-model="form.tenKichThuoc"
              placeholder="VD: S / M / L"
            />
          </div>

          <div class="field">
            <label>Trạng thái</label>
            <select class="input" v-model="form.trangThai">
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
          </div>

          <div class="field">
            <label>Thứ tự hiển thị</label>
            <input
              class="input"
              type="number"
              v-model="form.thuTu"
              placeholder="VD: 1"
            />
          </div>

          <div class="field" style="grid-column: 1 / -1">
            <label>Ghi chú</label>
            <textarea
              class="input"
              v-model="form.ghiChu"
              placeholder="Mô tả: áp dụng cho áo/quần..."
            ></textarea>
          </div>

        </div>
      </div>

    </div>
  </main>
</template>

<style scoped>
.input {
  width: 100%;
  padding: 10px 14px;
  border-radius: 12px;
  border: 1px solid #d8dee9;
  background: #fff;
  font-size: 14px;
  transition: 0.2s ease;
}

.input:focus {
  outline: none;
  border-color: #6366f1;
}
</style>