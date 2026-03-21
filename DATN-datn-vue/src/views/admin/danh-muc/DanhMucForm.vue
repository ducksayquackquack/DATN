<script setup>
import { ref, onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  createDanhMuc,
  updateDanhMuc,
  getDanhMucById
} from "../../../services/danhMucService"

const router = useRouter()
const route = useRoute()

const id = route.params.id

const form = ref({
  maDanhMuc: "",
  tenDanhMuc: "",
  trangThai: "Hoạt động",
  thuTuHienThi: "",
  moTa: "",
  metaTitle: "",
  slug: ""
})

const loading = ref(false)

const goBack = () => {
  router.push("/admin/danh-muc/list")
}

onMounted(async () => {
  if (id) {
    loading.value = true
    try {
      const res = await getDanhMucById(id)
      form.value.maDanhMuc = res.data.maDanhMuc
      form.value.tenDanhMuc = res.data.tenDanhMuc
      form.value.trangThai = res.data.trangThai
    } catch (e) {
      console.error(e)
    } finally {
      loading.value = false
    }
  }
})

const saveCategory = async () => {
  // Confirmation before saving
  const action = id ? 'cập nhật' : 'tạo mới'
  const confirmed = await window.confirmDialog(`Bạn có chắc chắn muốn ${action} danh mục "${form.value.tenDanhMuc}"?`)
  if (!confirmed) {
    return
  }

  const payload = {
    maDanhMuc: form.value.maDanhMuc,
    tenDanhMuc: form.value.tenDanhMuc,
    trangThai: form.value.trangThai
  }

  try {
    if (id) {
      await updateDanhMuc(id, payload)
      window.toast.success('Cập nhật danh mục thành công!')
    } else {
      await createDanhMuc(payload)
      window.toast.success('Tạo mới danh mục thành công!')
    }
    router.push("/admin/danh-muc/list")
  } catch (error) {
    window.toast.error('Có lỗi xảy ra: ' + (error.message || 'Vui lòng thử lại'))
  }
}
</script>

<template>
  <div class="card">
    <div class="head">
      <div>
        <h1>Form danh mục</h1>
        <small class="muted">
          Tạo mới / cập nhật danh mục sản phẩm
        </small>
      </div>

      <div style="display:flex;gap:10px;flex-wrap:wrap">
        <button class="btn" @click="goBack">
          ← Quay lại
        </button>

        <button class="btn primary" @click="saveCategory">
          Lưu
        </button>
      </div>
    </div>

    <div class="body" v-if="!loading">
      <div class="grid cols2">

        <div class="field">
          <label>Mã danh mục</label>
          <input
            type="text"
            placeholder="VD: DM010"
            v-model="form.maDanhMuc"
          />
          <small class="muted">Có thể để BE tự sinh</small>
        </div>

        <div class="field">
          <label>Tên danh mục</label>
          <input
            type="text"
            placeholder="VD: Áo / Quần / Phụ kiện"
            v-model="form.tenDanhMuc"
          />
        </div>

        <div class="field">
          <label>Trạng thái</label>
          <select v-model="form.trangThai">
            <option value="Hoạt động">Hoạt động</option>
            <option value="Ngừng hoạt động">Ngừng hoạt động</option>
          </select>
        </div>

        <div class="field">
          <label>Thứ tự hiển thị</label>
          <input
            type="number"
            placeholder="VD: 1"
            v-model="form.thuTuHienThi"
          />
          <small class="muted">Số nhỏ lên trước</small>
        </div>

        <div class="field" style="grid-column: 1 / -1">
          <label>Mô tả</label>
          <textarea
            placeholder="Mô tả ngắn: nhóm sản phẩm gồm..."
            v-model="form.moTa"
          ></textarea>
        </div>

        <div class="field" style="grid-column: 1 / -1">
          <label>SEO (tuỳ chọn)</label>

          <div class="grid cols2">
            <input
              type="text"
              placeholder="Meta title"
              v-model="form.metaTitle"
            />
            <input
              type="text"
              placeholder="Slug (VD: ao-nam)"
              v-model="form.slug"
            />
          </div>

          <small class="muted">
            Nếu anh có route theo slug thì lưu luôn.
          </small>
        </div>

      </div>
    </div>

    <div v-else style="padding:20px">
      Đang tải dữ liệu...
    </div>
  </div>
</template>