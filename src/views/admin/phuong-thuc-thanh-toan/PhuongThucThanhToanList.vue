<template>
  <main class="wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>Phương thức thanh toán</h1>
          <small class="muted">
            Quản lý các phương thức dùng trong checkout
          </small>
        </div>

        <router-link
          class="btn primary"
          to="/admin/phuong-thuc-thanh-toan/form"
        >
          + Thêm phương thức
        </router-link>
      </div>

      <div class="body">

        <div class="toolbar">
          <div class="filters">
            <input
              style="width:320px"
              type="text"
              placeholder="Tìm theo mã / tên..."
              v-model="search"
            />

            <select
              style="width:220px"
              v-model="statusFilter"
            >
              <option value="">Tất cả</option>
              <option>Active</option>
              <option>Inactive</option>
            </select>
          </div>
        </div>

        <table class="table">
          <thead>
            <tr>
              <th style="width:100px">Mã</th>
              <th>Tên phương thức</th>
              <th style="width:140px">Trạng thái</th>
              <th style="width:140px" class="center">Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="item in filteredList"
              :key="item.code"
            >
              <td>{{ item.code }}</td>

              <td>
                <b>{{ item.name }}</b>
                <div class="muted" style="font-size:12px">
                  {{ item.description }}
                </div>
              </td>

              <td>
                <span
                  class="pill"
                  :class="item.status === 'Active' ? 'ok' : 'warn'"
                >
                  ● {{ item.status }}
                </span>
              </td>

            <td class="center">
              <div class="actions">
                <router-link
                  class="iconbtn"
                  :to="`/admin/phuong-thuc-thanh-toan/form/${item.id}`"
                >
                  <Pencil size="16" />
                </router-link>

                <button
                  class="iconbtn"
                  @click="remove(item.id)"
                >
                  <Trash2 size="16" />
                </button>
              </div>
            </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination">
          <div>Hiển thị 1–10 / {{ list.length }}</div>
          <div class="pager">
            <button class="btn">← Trước</button>
            <span class="chip">1</span>
            <button class="btn">Sau →</button>
          </div>
        </div>

      </div>
    </div>
  </main>
</template>

<script setup>
import { ref, computed, onMounted } from "vue"
import {
  getAllPTTT,
  deletePTTT
} from "../../../services/ptttService"
import { Pencil, Trash2 } from "lucide-vue-next"

const search = ref("")
const statusFilter = ref("")
const list = ref([])

async function loadData() {
  const res = await getAllPTTT()

  list.value = res.data.map(item => ({
    id: item.id,
    code: item.ma,
    name: item.ten,
    description: item.moTa,
    status:
      item.trangThai === "Ngừng hoạt động"
        ? "Inactive"
        : "Active"
  }))
}

onMounted(loadData)

const filteredList = computed(() => {
  return list.value.filter(item => {

    const matchSearch =
      item.code.toLowerCase().includes(search.value.toLowerCase()) ||
      item.name.toLowerCase().includes(search.value.toLowerCase())

    const matchStatus =
      !statusFilter.value ||
      item.status === statusFilter.value

    return matchSearch && matchStatus
  })
})

async function remove(id) {
  await deletePTTT(id)
  loadData()
}
</script>

<style scoped>

.center{
  text-align:center;
}

.actions{
  display:flex;
  justify-content:center;
  align-items:center;
  gap:8px;
}

</style>