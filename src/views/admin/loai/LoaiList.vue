<script setup>
import { ref, onMounted, computed } from "vue"
import { useRouter } from "vue-router"
import { getAllLoai, deleteLoai } from "../../../services/loaiService"
import { Pencil, Trash2 } from "lucide-vue-next"

const router = useRouter()
const list = ref([])

const searchText = ref("")
const filterStatus = ref("Tất cả trạng thái")
const sortOption = ref("Mới nhất")

async function loadData() {
  const res = await getAllLoai()
  list.value = res.data || []
}
onMounted(loadData)

async function remove(id) {
  await deleteLoai(id)
  loadData()
}

const filteredData = computed(() => {
  let data = [...list.value]

  if (searchText.value.trim()) {
    const keyword = searchText.value.toLowerCase()
    data = data.filter(l =>
      l.maLoai?.toLowerCase().includes(keyword) ||
      l.tenLoai?.toLowerCase().includes(keyword)
    )
  }

  if (filterStatus.value !== "Tất cả trạng thái") {
    data = data.filter(l => l.trangThai === filterStatus.value)
  }

  if (sortOption.value === "Tên A-Z") {
    data.sort((a, b) => a.tenLoai.localeCompare(b.tenLoai))
  }

  if (sortOption.value === "Mới nhất") {
    data.sort((a, b) => b.id - a.id)
  }

  return data
})
</script>

<template>
  <main class="wrap">
    <div class="card">

      <div class="head">
        <div>
          <h1>Loại</h1>
          <small class="muted">
            Ví dụ: Slim fit / Regular / Oversize...
          </small>
        </div>

        <button
          class="btn primary"
          @click="router.push('/admin/loai/form')"
        >
          + Thêm loại
        </button>
      </div>

      <div class="body">

        <div class="toolbar">
          <div class="filters">
            <input
              style="width:320px"
              type="text"
              placeholder="Tìm theo mã / tên..."
              v-model="searchText"
            />

            <select style="width:220px" v-model="filterStatus">
              <option>Tất cả trạng thái</option>
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
          </div>

          <div class="filters">
            <select style="width:220px" v-model="sortOption">
              <option>Mới nhất</option>
              <option>Tên A-Z</option>
            </select>
          </div>
        </div>

        <table class="table">
          <thead>
            <tr>
              <th style="width:100px">Mã</th>
              <th>Tên loại</th>
              <th style="width:180px" class="center">Trạng thái</th>
              <th style="width:180px" class="center">Thao tác</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="l in filteredData" :key="l.id">
              <td>{{ l.maLoai }}</td>

              <td>
                <b>{{ l.tenLoai }}</b>
              </td>

              <td class="center">
                <span
                  class="pill"
                  :class="{
                    ok: l.trangThai === 'Hoạt động',
                    danger: l.trangThai === 'Ngừng hoạt động'
                  }"
                >
                  ● {{ l.trangThai }}
                </span>
              </td>

              <td class="center">
                <div class="actions">
                  <button
                    class="iconbtn"
                    @click="router.push(`/admin/loai/form/${l.id}`)"
                  >
                    <Pencil size="16" />
                  </button>

                  <button
                    class="iconbtn"
                    @click="remove(l.id)"
                  >
                    <Trash2 size="16" />
                  </button>
                </div>
              </td>
            </tr>

            <tr v-if="filteredData.length === 0">
              <td colspan="4" style="text-align:center">
                Không có dữ liệu
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination">
          <div>
            Hiển thị 1–{{ filteredData.length }} / {{ filteredData.length }}
          </div>

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

<style scoped>
.center {
  text-align: center;
  vertical-align: middle;
}

.actions {
  display: flex;
  justify-content: center;
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

.pill.ok {
  background: #dcfce7;
  color: #166534;
}

.pill.danger {
  background: #fee2e2;
  color: #991b1b;
}
</style>