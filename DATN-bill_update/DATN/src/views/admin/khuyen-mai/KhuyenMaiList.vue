<script setup>
import { ref, onMounted } from "vue"
import { useRouter } from "vue-router"
import {
  getAllKhuyenMai,
  deleteKhuyenMai
} from "../../../services/khuyenMaiService"
import { Pencil, Trash2 } from "lucide-vue-next"

const router = useRouter()
const list = ref([])

async function loadData() {
  const res = await getAllKhuyenMai()
  list.value = res.data || []
}

onMounted(loadData)

const goToEdit = (id) => {
  router.push(`/admin/khuyen-mai/form/${id}`)
}

const remove = async (id) => {
  await deleteKhuyenMai(id)
  await loadData()
}

const isActive = (status) => {
  return status === "Đang chạy" || status === "Hoạt động"
}
</script>

<template>
  <main class="wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>Khuyến mãi</h1>
          <small class="muted">
            Mã giảm giá, giảm theo %, theo tiền...
          </small>
        </div>

        <button
          class="btn primary"
          @click="router.push('/admin/khuyen-mai/form')"
        >
          + Tạo khuyến mãi
        </button>
      </div>

      <div class="body">
        <table class="table">
          <thead>
            <tr>
              <th style="width:120px">Mã</th>
              <th>Tên</th>
              <th style="width:160px">Giảm</th>
              <th style="width:200px">Thời gian</th>
              <th style="width:140px">Trạng thái</th>
              <th class="action-col">Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="km in list" :key="km.id">
              <td><b>{{ km.maKhuyenMai }}</b></td>
              <td>{{ km.tenKhuyenMai }}</td>

              <td>
                <span class="pill">
                  {{ km.giaTri }}
                </span>
              </td>

              <td class="muted">
                {{ km.ngayBatDau?.substring(0,10) }} →
                {{ km.ngayKetThuc?.substring(0,10) }}
              </td>

              <td>
                <span
                  class="pill"
                  :class="isActive(km.trangThai) ? 'ok' : 'bad'"                >
                  ● {{ km.trangThai }}
                </span>
              </td>

              <td class="action-col">
                <div class="actions">
                  <button
                    class="iconbtn"
                    @click="goToEdit(km.id)"
                  >
                    <Pencil size="16"/>
                  </button>

                  <button
                    class="iconbtn"
                    @click="remove(km.id)"
                  >
                    <Trash2 size="16"/>
                  </button>
                </div>
              </td>
            </tr>

            <tr v-if="list.length === 0">
              <td colspan="6" style="text-align:center">
                Không có dữ liệu
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </main>
</template>

<style scoped>
.action-col {
  text-align: center;
  width: 150px;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}
.pill {
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
}

.pill.ok {
  background: #dcfce7;
  color: #166534;
}

.pill.bad {
  background: #fee2e2;
  color: #991b1b;
}
</style>