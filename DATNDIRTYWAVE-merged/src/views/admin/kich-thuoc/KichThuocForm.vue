<script setup>
import { reactive, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  createKichThuoc,
  updateKichThuoc,
  getKichThuocById,
  getAllKichThuoc
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
  ghiChu: ""
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

const generateNextKichThuocCode = async () => {
  form.maKichThuoc = 'KT001'
  try {
    const res = await getAllKichThuoc()
    const list = extractList(res?.data)
    const maxNumber = list.reduce((acc, item) => {
      const parsed = parseCodeNumber(item?.maKichThuoc, 'KT')
      return parsed && parsed > acc ? parsed : acc
    }, 0)
    form.maKichThuoc = `KT${String(maxNumber + 1).padStart(3, '0')}`
  } catch {}
}

onMounted(async () => {
  if (id) {
    const res = await getKichThuocById(id)

    form.maKichThuoc = res.data.maKichThuoc || ""
    form.tenKichThuoc = res.data.tenKichThuoc || ""
    form.trangThai = normalizeTrangThai(res.data.trangThai)
    form.ghiChu = res.data.ghiChu || res.data.moTa || ""
  } else {
    await generateNextKichThuocCode()
  }
})

async function save() {
  const trimmedSizeName = String(form.tenKichThuoc || '').trim()
  if (!trimmedSizeName) {
    window.toast?.error?.('Vui lòng nhập giá trị size')
    return
  }

  const confirmed = await window.confirmDialog?.(id ? "Bạn có chắc muốn cập nhật kích thước này?" : "Bạn có chắc muốn tạo kích thước mới?") ?? confirm(id ? "Cập nhật kích thước?" : "Tạo kích thước mới?")
  if (!confirmed) return

  const payload = {
    maKichThuoc: form.maKichThuoc,
    tenKichThuoc: trimmedSizeName,
    trangThai: mapToBE(form.trangThai),
    ghiChu: form.ghiChu
  }

  try {
    if (id) {
      await updateKichThuoc(id, payload)
      window.toast?.success?.('Cập nhật kích thước thành công')
    } else {
      await createKichThuoc(payload)
      window.toast?.success?.('Thêm kích thước thành công')
    }

    router.push("/admin/kich-thuoc/list")
  } catch (error) {
    window.toast?.error?.(error?.response?.data?.message || error?.message || 'Lưu kích thước thất bại')
  }
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
              class="input auto-code-input"
              readonly
              :value="form.maKichThuoc || 'Mã tự sinh'"
            />
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

          <div class="field" style="grid-column: 1 / -1">
            <label>Mô tả</label>
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
  background-color: #fff;
  font-size: 14px;
  transition: 0.2s ease;
}
select.input {
  padding-right: 34px;
}

.input:focus {
  outline: none;
  border-color: #6366f1;
}

.auto-code-input {
  background: #f1f5f9;
  color: #64748b;
  border-color: #cbd5e1;
  font-weight: 600;
}
</style>