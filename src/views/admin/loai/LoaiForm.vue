<script setup>
import { reactive, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import { ArrowLeft, Save, Trash2 } from "lucide-vue-next"
import {
  createLoai,
  updateLoai,
  getLoaiById,
  getAllLoai
} from "../../../services/loaiService"

const router = useRouter()
const route = useRoute()
const id = route.params.id

const form = reactive({
  code: '',
  name: '',
  applyFor: 'Tất cả',
  status: 'Active',
  description: ''
})

onMounted(async () => {
  if (id) {
    const res = await getLoaiById(id)
    if (res.data) {
      form.code = res.data.maLoai || ''
      form.name = res.data.tenLoai || ''
      form.status =
        res.data.trangThai === 'Ngừng hoạt động'
          ? 'Ngừng hoạt động'
          : 'Hoạt động'
      form.description = res.data.moTa || ''
    }
  }
})

async function save() {
  const trimmedName = form.name.trim()

  if (!trimmedName) {
    window.toast?.error?.("Vui lòng nhập tên loại")
    return
  }

  // Chỉ cho phép chữ cái (Unicode), số và khoảng trắng
  if (/[^\p{L}\p{N}\s]/u.test(trimmedName)) {
    window.toast?.error?.("Tên loại không được chứa ký tự đặc biệt")
    return
  }

  // Kiểm tra trùng tên
  try {
    const res = await getAllLoai()
    const list = res.data || []
    const duplicate = list.find(
      (item) =>
        item.tenLoai?.trim().toLowerCase() === trimmedName.toLowerCase() &&
        String(item.id) !== String(id)
    )
    if (duplicate) {
      window.toast?.error?.("Tên loại đã tồn tại")
      return
    }
  } catch {
    // Nếu không kiểm tra được, vẫn cho phép lưu
  }

  const confirmed = await window.confirmDialog?.(id ? "Bạn có chắc muốn cập nhật loại này?" : "Bạn có chắc muốn tạo loại mới?") ?? confirm(id ? "Cập nhật loại?" : "Tạo loại mới?")
  if (!confirmed) return

  const payload = {
    maLoai: form.code,
    tenLoai: trimmedName,
    trangThai:
      form.status === 'Ngừng hoạt động'
        ? 'Ngừng hoạt động'
        : 'Hoạt động',
    moTa: form.description
  }

  if (id) {
    await updateLoai(id, payload)
  } else {
    await createLoai(payload)
  }

  router.push('/admin/loai/list')
}
</script>

<template>
  <main class="wrap">
    <div class="card">

      <!-- HEADER -->
      <div class="head">
        <div>
          <h1>Form loại</h1>
          <small class="muted">
            Tạo mới / cập nhật loại (form/fit/kiểu)
          </small>
        </div>

        <div style="display:flex;gap:12px;align-items:center">
          <button
            class="btn"
            @click="router.push('/admin/loai/list')"
          >
            <ArrowLeft size="16" style="margin-right:6px" />
            Quay lại
          </button>

          <button
            class="btn primary"
            @click="save"
          >
            <Save size="16" style="margin-right:6px" />
            Lưu
          </button>
        </div>
      </div>

      <div class="body">

        <!-- FORM GRID -->
        <div class="grid cols2">

          <div class="field">
            <label>Mã loại</label>
            <input
              type="text"
              readonly
              value="Mã tự sinh"
              class="auto-code-input"
            />
          </div>

          <div class="field">
            <label>Tên loại</label>
            <input
              v-model="form.name"
              type="text"
              placeholder="VD: Slim Fit / Regular / Oversize"
            />
          </div>

          <div class="field">
            <label>Áp dụng cho</label>
            <select v-model="form.applyFor">
              <option>Tất cả</option>
              <option>Áo</option>
              <option>Quần</option>
              <option>Phụ kiện</option>
            </select>
            <small class="muted">
              Tuỳ model BE, không cần thì bỏ.
            </small>
          </div>

          <div class="field">
            <label>Trạng thái</label>
            <select v-model="form.status">
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
          </div>

          <div class="field" style="grid-column: 1 / -1">
            <label>Mô tả</label>
            <textarea
              v-model="form.description"
              placeholder="Mô tả: đặc điểm form/fit, size gợi ý..."
            ></textarea>
          </div>

        </div>

        <hr class="sep"/>

        <!-- SIZE MAP CARD -->
        <div class="card" style="border-radius:14px">
          <div class="head">
            <h2>Quy đổi size (tuỳ chọn)</h2>
            <small class="muted">
              Nếu anh muốn map size theo chiều cao/cân nặng
            </small>
          </div>

          <div class="body">

            <table class="table">
              <thead>
                <tr>
                  <th style="width:140px">SIZE</th>
                  <th>GỢI Ý</th>
                  <th style="width:160px" class="right">THAO TÁC</th>
                </tr>
              </thead>

              <tbody>
                <tr>
                  <td><b>M</b></td>
                  <td class="muted">
                    1m65–1m72 • 55–65kg
                  </td>
                  <td class="right">
                    <div class="actions">
                      <button class="iconbtn" title="Xem chi tiết">
                        <span class="material-icons-outlined">visibility</span>
                      </button>

                      <button class="iconbtn">
                        <Trash2 size="16" />
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>

            <button class="btn">
              + Thêm quy đổi
            </button>

          </div>
        </div>

      </div>
    </div>
  </main>
</template>

<style scoped>
.actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
}

.iconbtn {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.auto-code-input {
  background: #f1f5f9;
  color: #64748b;
  border-color: #cbd5e1;
  font-weight: 600;
}
@media (max-width: 768px) {
  .head { flex-direction: column; align-items: flex-start; gap: 12px; }
  .body { overflow-x: auto; }
  table { min-width: 500px; }
}
</style>