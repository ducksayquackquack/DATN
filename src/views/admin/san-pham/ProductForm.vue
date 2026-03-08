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
  window.toast.info("Lưu nháp (demo)")
}

const form = reactive({
  sku: "",
  name: "",
  description: "",
  status: "Đang bán",

  giaBan: "",
  giaGoc: "",
  ton: 0,

  loai: "",
  loaiId: 1,

  tag: "",
  variantId: null
})

onMounted(async () => {
  if (!id) return

  const res = await getSanPhamById(id)
  const data = res.data

  const variants = data.sanPhamChiTiets || []
  const firstVariant = variants[0]

  form.variantId = firstVariant?.id || null
  form.ton = firstVariant?.soLuong ?? 0

  form.sku = data.maSanPham
  form.name = data.tenSanPham
  form.description = data.moTa
  form.giaBan =
    firstVariant?.giaBan ??
    data.giaBan ??
    ""

  form.giaGoc = data.giaGoc ?? ""

  // form.danhMuc =
  //   firstVariant?.danhMuc?.tenDanhMuc ||
  //   data.danhMuc ||
  //   ""

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
      trangThai:
        form.status === "Đang bán"
          ? "Hoạt động"
          : "Ngừng hoạt động",

      sanPhamChiTiets: [
        {
          id: form.variantId,

          ma: form.sku + "-01",
          giaNhap: Number(form.giaGoc) || 0,
          giaBan: Number(form.giaBan),
          soLuong: Number(form.ton),

          chatLieu: { id: 1 },
          mauSac: { id: 1 },
          kichThuoc: { id: 2 },
          hang: { id: 1 },
          xuatSu: { id: 1 },
          danhMuc: { id: 1 },
          loai: { id: form.loaiId },

          trangThai: "Hoạt động"
        }
      ]
    }

    if (id) {
      await updateSanPham(id, payload)
      window.toast.success("Cập nhật sản phẩm thành công")
    } else {
      await createSanPham(payload)
      window.toast.success("Tạo sản phẩm thành công")
    }

    setTimeout(() => {
      router.push("/admin/san-pham/list")
    }, 1000)
  } catch (err) {
    console.error("SAVE ERROR:", err)
    window.toast.error("Lưu thất bại: " + (err.response?.data?.message || err.message))
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

        <button class="btn primary" @click.prevent="saveProduct">
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
            :readonly="!!id"
            :placeholder="id ? '' : 'VD: SP001'"
          />
        </div>

        <!-- <div class="field">
          <label>Danh mục</label>
          <input
            v-model="form.danhMuc"
            type="text"
            placeholder="VD: Áo"
          />
        </div> -->

        <div class="field">
          <label>Loại / Form</label>
          <select v-model.number="form.loaiId">
            <option :value="1">Hoodie Jacket</option>
            <option :value="2">Bomber Jacket</option>
            <option :value="3">Coach Jacket</option>
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
        <label>Tồn kho</label>
        <input
          v-model="form.ton"
          type="number"
          placeholder="0"
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