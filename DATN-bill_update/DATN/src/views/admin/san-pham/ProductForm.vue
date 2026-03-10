<script setup>
import { reactive, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  createSanPham,
  updateSanPham,
  getSanPhamById
} from "../../../services/sanPhamService"

const router = useRouter()
const route = useRoute()
const id = route.params.id

function goBack() {
  router.push("/admin/san-pham/list")
}

function saveDraft() {
  alert("Lưu nháp (demo)")
}

const form = reactive({
  sku: "",
  name: "",
  description: "",
  status: "Đang bán",
  giaBan: "",
  giaGoc: "",
  danhMuc: "",
  loai: "",
  tag: ""
})

onMounted(async () => {
  if (!id) return

  const res = await getSanPhamById(id)
  const data = res.data

  const variants = data.sanPhamChiTiets || []
  const firstVariant = variants[0]

  form.sku = data.maSanPham
  form.name = data.tenSanPham
  form.description = data.moTa
  form.giaBan =
    firstVariant?.giaBan ??
    data.giaBan ??
    ""

  form.giaGoc = data.giaGoc ?? ""

  form.danhMuc =
    firstVariant?.danhMuc?.tenDanhMuc ||
    data.danhMuc ||
    ""

  form.loai =
    firstVariant?.loai?.tenLoai ||
    data.loai ||
    ""

  form.tag = data.tag ?? ""

  form.status =
    data.trangThai === "Hoạt động"
      ? "Đang bán"
      : "Ẩn"
})

async function saveProduct() {
  try {
    const payload = {
      maSanPham: form.sku,
      tenSanPham: form.name,
      moTa: form.description,
      giaBan: Number(form.giaBan),
      giaGoc: form.giaGoc ? Number(form.giaGoc) : null,
      danhMuc: form.danhMuc,
      loai: form.loai,
      tag: form.tag,
      trangThai:
        form.status === "Đang bán"
          ? "Hoạt động"
          : "Ngừng hoạt động"
    }

    if (id) {
      await updateSanPham(id, payload)
    } else {
      await createSanPham(payload)
    }

    router.push("/admin/san-pham/list")
  } catch (err) {
    console.error("SAVE ERROR:", err)
    alert("Lưu thất bại!")
  }
}
</script>

<template>
  <div class="card">
    <div class="head">
      <div>
        <h1>Form sản phẩm</h1>
        <small class="muted">
          Tạo mới / cập nhật sản phẩm + biến thể
        </small>
      </div>

      <div style="display:flex;gap:10px;flex-wrap:wrap">
        <button type="button" class="btn" @click="goBack">
          ← Quay lại
        </button>

        <button type="button" class="btn" @click="saveDraft">
          Lưu nháp
        </button>

        <button type="button" class="btn primary" @click="saveProduct">
          Lưu
        </button>
      </div>
    </div>

    <div class="body">

      <!-- BASIC INFO -->
      <div class="grid cols2">

        <div class="field">
          <label>Tên sản phẩm</label>
          <input
            v-model="form.name"
            type="text"
            placeholder="VD: Áo thun cotton 220gsm"
          />
        </div>

        <div class="field">
          <label>Mã Sản Phẩm</label>
          <input
            v-model="form.sku"
            type="text"
            placeholder="VD: SP001"
          />
        </div>

        <div class="field">
          <label>Danh mục</label>
          <input
            v-model="form.danhMuc"
            type="text"
            placeholder="VD: Áo"
          />
        </div>

        <div class="field">
          <label>Loại / Form</label>
          <select v-model="form.loai">
            <option value="">-- Chọn loại --</option>
            <option value="Hoodie Jacket">Hoodie Jacket</option>
            <option value="Bomber Jacket">Bomber Jacket</option>
            <option value="Coach Jacket">Coach Jacket</option>
          </select>
        </div>

        <div class="field">
          <label>Giá bán</label>
          <input
            v-model="form.giaBan"
            type="number"
            placeholder="329000"
          />
        </div>

        <div class="field">
          <label>Giá gốc (tuỳ chọn)</label>
          <input
            v-model="form.giaGoc"
            type="number"
            placeholder="389000"
          />
        </div>

        <div class="field">
          <label>Trạng thái</label>
          <select v-model="form.status">
            <option>Đang bán</option>
            <option>Ẩn</option>
          </select>
        </div>

        <div class="field">
          <label>Tag</label>
          <input
            v-model="form.tag"
            type="text"
            placeholder="VD: New, Best, Sale"
          />
        </div>

        <div class="field" style="grid-column: 1 / -1">
          <label>Mô tả</label>
          <textarea
            v-model="form.description"
            placeholder="Chất liệu, form, hướng dẫn bảo quản..."
          ></textarea>
        </div>

      </div>

    </div>
  </div>
</template>

<style scoped>
input,
select,
textarea {
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