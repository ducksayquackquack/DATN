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
              :value="form.code || 'Mã tự sinh'"
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
  getMauSacById,
  getAllMauSac
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

const extractList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const parseCodeNumber = (value = '', prefix = '') => {
  const raw = String(value || '').trim().toUpperCase()
  if (!raw.startsWith(prefix)) return null
  const suffix = raw.slice(prefix.length)
  if (!/^\d+$/.test(suffix)) return null
  return Number(suffix)
}

const generateNextMauCode = async () => {
  try {
    const res = await getAllMauSac()
    const list = extractList(res?.data)
    const maxNumber = list.reduce((acc, item) => {
      const parsed = parseCodeNumber(item?.maMau, 'MS')
      return parsed && parsed > acc ? parsed : acc
    }, 0)
    form.code = `MS${String(maxNumber + 1).padStart(3, '0')}`
  } catch {
    form.code = 'MS001'
  }
}

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
  } else {
    await generateNextMauCode()
  }
})

async function save() {
  const trimmedName = String(form.name || '').trim()
  if (!trimmedName) {
    window.toast?.error?.('Vui lòng nhập tên màu')
    return
  }

  const confirmed = await window.confirmDialog?.(id ? "Bạn có chắc muốn cập nhật màu sắc này?" : "Bạn có chắc muốn tạo màu sắc mới?") ?? confirm(id ? "Cập nhật màu sắc?" : "Tạo màu sắc mới?")
  if (!confirmed) return

  const payload = {
    maMau: form.code,
    tenMau: trimmedName,
    moTa: form.note,
    trangThai:
      form.status === "Ngừng hoạt động"
        ? "Ngừng hoạt động"
        : "Hoạt động"
  }

  try {
    if (id) {
      await updateMauSac(id, payload)
      window.toast?.success?.('Cập nhật màu sắc thành công')
    } else {
      await createMauSac(payload)
      window.toast?.success?.('Thêm màu sắc thành công')
    }

    router.push("/admin/mau-sac/list")
  } catch (error) {
    window.toast?.error?.(error?.response?.data?.message || error?.message || 'Lưu màu sắc thất bại')
  }
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