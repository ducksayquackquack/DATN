<script setup>
import { ref, computed, onMounted } from "vue"
import { useRouter } from "vue-router"
import {
  getAllSanPham,
  deleteSanPham
} from "../../../services/sanPhamService"
import { Pencil, Trash2 } from "lucide-vue-next"

const router = useRouter()

const search = ref("")
const list = ref([])

function goToCreate() {
  router.push("/admin/san-pham/form")
}

function formatCurrency(value) {
  return new Intl.NumberFormat("vi-VN").format(value || 0) + "₫"
}

async function loadData() {
  const res = await getAllSanPham()

  console.log(res.data)

  list.value = res.data.map(item => {
    const variants = item.sanPhamChiTiets || []

const totalTon =
  variants.length > 0
    ? variants.reduce((sum, v) => sum + (v.soLuong || 0), 0)
    : item.soLuong ?? 0
    const firstVariant = variants.length > 0 ? variants[0] : null

    const loaiName =
      firstVariant?.loai?.tenLoai ||
      item.loai ||
      "Khác"

    return {
      id: item.id,
      ma: item.maSanPham,
      name: item.tenSanPham,
      description: item.moTa,

      // gia:
      //   firstVariant?.giaBan ??
      //   item.giaBan ??
      //   item.gia ??
      //   0,

gia:
  firstVariant?.giaBan ??
  0,

      ton: totalTon ?? 0,

      loai: loaiName,
      type: loaiName,

      status:
        item.trangThai === "Hoạt động"
          ? "Đang bán"
          : "Ẩn"
    }
  })
}

onMounted(loadData)

const filteredList = computed(() =>
  list.value.filter(item =>
    item.ma?.toLowerCase().includes(search.value.toLowerCase()) ||
    item.name?.toLowerCase().includes(search.value.toLowerCase())
  )
)

const groupedProducts = computed(() => {
  const groups = {}

  filteredList.value.forEach(item => {
    const key = item.type || "Khác"

    if (!groups[key]) {
      groups[key] = []
    }

    groups[key].push(item)
  })

  return groups
})

async function remove(id) {
  if (!confirm("Bạn chắc chắn muốn xoá sản phẩm này?")) return

  try {
    await deleteSanPham(id)
    await loadData()
  } catch (err) {
    console.error("Delete failed:", err)
    window.toast.error("Không thể xoá sản phẩm")
  }
}
</script>

<template>
  <div class="card">

    <div class="head">
      <div>
        <h1>Sản phẩm</h1>
        <small class="muted">
          Danh sách sản phẩm + tồn kho, trạng thái bán
        </small>
      </div>

      <button class="btn primary" @click="goToCreate">
        + Thêm sản phẩm
      </button>
    </div>

    <div class="body">

      <div class="toolbar">
        <div class="filters">
          <input
            v-model="search"
            style="width:320px"
            type="text"
            placeholder="Tìm theo mã / tên..."
          />
        </div>
      </div>

      <!-- CATEGORY FRAMES -->

      <div
        v-for="(items, type) in groupedProducts"
        :key="type"
        class="category-frame"
      >

        <div class="category-header">
          {{ type }}
        </div>

        <table class="table">
          <thead>
            <tr>
              <th style="width:120px">Mã</th>
              <th>Sản phẩm</th>
              <th style="width:140px">Loại</th>
              <th style="width:140px" class="right">Giá</th>
              <th style="width:120px" class="right">Tồn</th>
              <th style="width:140px">Trạng thái</th>
              <th style="width:160px" class="right">Thao tác</th>
            </tr>
          </thead>

          <tbody>

            <tr
              v-for="item in items"
              :key="item.id"
            >
              <td>{{ item.ma }}</td>

              <td>
                <b>{{ item.name }}</b>

                <div class="muted product-desc">
                  {{ item.description }}
                </div>
              </td>

              <td>{{ item.loai }}</td>

              <td class="right">
                {{ formatCurrency(item.gia) }}
              </td>

              <td class="right">
                {{ item.ton }}
              </td>

              <td>
                <span
                  class="pill"
                  :class="item.status === 'Đang bán' ? 'ok' : 'warn'"
                >
                  ● {{ item.status }}
                </span>
              </td>

              <td class="right">

                <div class="actions">

                  <button
                    class="iconbtn"
                    @click="router.push(`/admin/san-pham/form/${item.id}`)"
                  >
                    <Pencil size="16" />
                  </button>
                <button
                  class="iconbtn"
                  @click="remove(item.id)"
                  title="Xoá sản phẩm"
                >
                  <Trash2 size="16" />
                </button>
                </div>

              </td>
            </tr>

          </tbody>
        </table>

      </div>

      <div
        v-if="filteredList.length === 0"
        class="empty"
      >
        Không có dữ liệu
      </div>

      <div class="pagination">
        <div>
          Hiển thị {{ filteredList.length }} sản phẩm
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>

.category-frame{
  border:1px solid #e5e7eb;
  border-radius:12px;
  overflow:hidden;
  margin-bottom:24px;
  background:#fff;
}

.category-header{
  background:#f3f4f6;
  padding:14px 16px;
  font-weight:600;
  font-size:15px;
}

.product-desc{
  font-size:12px;
  margin-top:3px;
}

.actions{
  display:flex;
  justify-content:flex-end;
  align-items:center;
  gap:8px;
}

.iconbtn{
  display:flex;
  align-items:center;
  justify-content:center;
  width:36px;
  height:36px;
  border-radius:10px;
  border:1px solid #e5e7eb;
  background:#fff;
  cursor:pointer;
}

.iconbtn:hover{
  background:#f1f5f9;
}

.pill.ok{
  background:#dcfce7;
  color:#166534;
}

.pill.warn{
  background:#fff1e6;
  color:#f97316;
}

.right{
  text-align:right;
}

.empty{
  text-align:center;
  padding:40px;
  color:#6b7280;
}

</style>