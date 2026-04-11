<template>
  <main class="wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>Form màu sắc</h1>
          <small class="muted">
            Tạo mới hoặc cập nhật màu
          </small>
        </div>

        <div style="display:flex;gap:10px;flex-wrap:wrap">
          <router-link
            class="btn"
            to="/admin/mau-sac/list"
          >
            ← Quay lại
          </router-link>

          <button
            class="btn primary"
            @click="save"
          >
            Lưu
          </button>
        </div>
      </div>

      <div class="body">
        <div class="grid cols2">

          <div class="field">
            <label>Mã màu</label>
            <input
              type="text"
              readonly
              value="Mã tự sinh"
              class="auto-code-input"
            />
          </div>

          <div class="field">
            <label>Tên màu</label>
            <input
              type="text"
              placeholder="VD: Xanh navy"
              v-model="form.name"
            />
          </div>

          <div class="field">
            <label>Trạng thái</label>
            <select v-model="form.status">
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
          </div>

          <div
            class="field"
            style="grid-column: 1 / -1"
          >
            <label>Mô tả</label>
            <textarea
              placeholder="Ghi chú..."
              v-model="form.note"
            ></textarea>
          </div>

        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { reactive, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  createMauSac,
  updateMauSac,
  getMauSacById
} from "../../../services/mauSacService"

const router = useRouter()
const route = useRoute()
const id = route.params.id

const form = reactive({
  code: "",
  name: "",
  status: "Hoạt động",
  note: ""
})

onMounted(async () => {
  if (id) {
    const res = await getMauSacById(id)

    if (res.data) {
      form.code = res.data.maMau || ""
      form.name = res.data.tenMau || ""
      form.note = res.data.moTa || res.data.ghiChu || ""
      form.status =
        res.data.trangThai === "Ngừng hoạt động"
          ? "Ngừng hoạt động"
          : "Hoạt động"
    }
  }
})

async function save() {
  const confirmed = await window.confirmDialog?.(id ? "Bạn có chắc muốn cập nhật màu sắc này?" : "Bạn có chắc muốn tạo màu sắc mới?") ?? confirm(id ? "Cập nhật màu sắc?" : "Tạo màu sắc mới?")
  if (!confirmed) return

  const payload = {
    maMau: form.code,
    tenMau: form.name,
    moTa: form.note,
    trangThai:
      form.status === "Ngừng hoạt động"
        ? "Ngừng hoạt động"
        : "Hoạt động"
  }

  if (id) {
    await updateMauSac(id, payload)
  } else {
    await createMauSac(payload)
  }

  router.push("/admin/mau-sac/list")
}
</script>

<style scoped>
.auto-code-input {
  background: #f1f5f9;
  color: #64748b;
  border-color: #cbd5e1;
  font-weight: 600;
}
</style>