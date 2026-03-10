<template>
  <main class="wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>Form phương thức thanh toán</h1>
          <small class="muted">
            Thiết lập các phương thức cho checkout
          </small>
        </div>

        <div style="display:flex;gap:10px;flex-wrap:wrap">
          <router-link
            class="btn"
            to="/admin/phuong-thuc-thanh-toan/list"
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
            <label>Mã</label>
            <input
              type="text"
              placeholder="VD: PTTT03"
              v-model="form.code"
            />
          </div>

          <div class="field">
            <label>Tên phương thức</label>
            <input
              type="text"
              placeholder="VD: Ví MoMo"
              v-model="form.name"
            />
          </div>

          <div class="field">
            <label>Trạng thái</label>
            <select v-model="form.status">
              <option>Active</option>
              <option>Inactive</option>
            </select>
          </div>

          <div
            class="field"
            style="grid-column: 1 / -1"
          >
            <label>Mô tả / Hướng dẫn</label>
            <textarea
              placeholder="Thông tin tài khoản, QR, lưu ý khi thanh toán..."
              v-model="form.description"
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
  createPTTT,
  updatePTTT,
  getPTTTById
} from "../../../services/ptttService"

const router = useRouter()
const route = useRoute()
const id = route.params.id

const form = reactive({
  code: "",
  name: "",
  description: "",
  status: "Active"
})

onMounted(async () => {
  if (id) {
    const res = await getPTTTById(id)

    if (res.data) {
      form.code = res.data.ma || ""
      form.name = res.data.ten || ""
      form.description = res.data.moTa || ""
      form.status =
        res.data.trangThai === "Ngừng hoạt động"
          ? "Inactive"
          : "Active"
    }
  }
})

async function save() {

  const payload = {
    ma: form.code,
    ten: form.name,
    moTa: form.description,
    trangThai:
      form.status === "Inactive"
        ? "Ngừng hoạt động"
        : "Hoạt động"
  }

  if (id) {
    await updatePTTT(id, payload)
  } else {
    await createPTTT(payload)
  }

  router.push("/admin/phuong-thuc-thanh-toan/list")
}
</script>