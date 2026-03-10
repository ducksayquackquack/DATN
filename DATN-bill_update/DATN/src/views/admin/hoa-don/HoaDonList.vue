<script setup>
import { ref, computed, onMounted } from "vue"
import { useRoute, useRouter } from "vue-router"
import {
  getHoaDonById,
  createHoaDon,
  updateHoaDon,
  getNhanVienOptions,
  getKhachHangOptions
} from "../../../services/hoaDonService"

const route = useRoute()
const router = useRouter()

const id = route.params.id
const isEdit = computed(() => !!id)

const loading = ref(false)
const saving = ref(false)
const error = ref("")

const listNV = ref([])
const listKH = ref([])

const form = ref({
  id: null,
  maHoaDon: "",
  nhanVienId: "",
  khachHangId: "",
  soDienThoaiNhanHang: "",
  diaChiNhanHang: "",
  phiShipText: "0",
  ngayNhanHangDuKien: "",
  ngayNhanHangMongMuon: "",
  ngayTao: "",
  trangThai: "",
  trangThaiName: ""
})

const normalizeDateInput = (value) => {
  if (!value) return ""
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ""
  return d.toISOString().slice(0, 10)
}

const formatDateTime = (value) => {
  if (!value) return ""
  return new Date(value).toLocaleString("vi-VN")
}

const parsePhiShip = (value) => {
  if (!value) return 0
  const raw = String(value).trim().replace(/\s/g, "").replace(/\./g, "").replace(/,/g, "")
  return Number(raw || 0)
}

const mapDetailToForm = (data) => {
  form.value.id = data?.id ?? null
  form.value.maHoaDon = data?.maHoaDon ?? ""
  form.value.nhanVienId = data?.nhanVien?.id ?? data?.nhanVienId ?? ""
  form.value.khachHangId = data?.khachHang?.id ?? data?.khachHangId ?? ""
  form.value.soDienThoaiNhanHang = data?.soDienThoaiNhanHang ?? ""
  form.value.diaChiNhanHang = data?.diaChiNhanHang ?? ""
  form.value.phiShipText = String(data?.phiShip ?? 0)
  form.value.ngayNhanHangDuKien = normalizeDateInput(data?.ngayNhanHangDuKien)
  form.value.ngayNhanHangMongMuon = normalizeDateInput(data?.ngayNhanHangMongMuon)
  form.value.ngayTao = data?.ngayTao ?? ""
  form.value.trangThai = data?.trangThai ?? data?.statusCode ?? ""
  form.value.trangThaiName = data?.trangThaiName ?? data?.orderStatusName ?? ""
}

const loadOptions = async () => {
  const [nvRes, khRes] = await Promise.all([
    getNhanVienOptions(),
    getKhachHangOptions()
  ])

  listNV.value = Array.isArray(nvRes?.data) ? nvRes.data : []
  listKH.value = Array.isArray(khRes?.data) ? khRes.data : []
}

const loadDetail = async () => {
  if (!isEdit.value) return
  const res = await getHoaDonById(id)
  mapDetailToForm(res?.data || {})
}

const loadData = async () => {
  try {
    loading.value = true
    error.value = ""

    await loadOptions()
    await loadDetail()
  } catch (e) {
    console.error("Lỗi load form hóa đơn:", e)
    error.value = "Không tải được dữ liệu form hóa đơn"
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

const submitPayload = computed(() => ({
  id: form.value.id,
  maHoaDon: form.value.maHoaDon,
  nhanVienId: form.value.nhanVienId || null,
  khachHangId: form.value.khachHangId || null,
  soDienThoaiNhanHang: form.value.soDienThoaiNhanHang,
  diaChiNhanHang: form.value.diaChiNhanHang,
  phiShip: parsePhiShip(form.value.phiShipText),
  ngayNhanHangDuKien: form.value.ngayNhanHangDuKien || null,
  ngayNhanHangMongMuon: form.value.ngayNhanHangMongMuon || null,
  ngayTao: form.value.ngayTao || null,
  trangThai: form.value.trangThai || null
}))

const handleSubmit = async () => {
  try {
    saving.value = true
    error.value = ""

    if (!form.value.nhanVienId || !form.value.khachHangId) {
      error.value = "Vui lòng chọn nhân viên và khách hàng"
      return
    }

    if (isEdit.value) {
      await updateHoaDon(id, submitPayload.value)
      alert("Cập nhật thông tin thành công")
      router.push(`/admin/hoa-don/detail/${id}`)
      return
    }

    const res = await createHoaDon(submitPayload.value)
    const createdId = res?.data?.id || res?.data?.data?.id

    alert("Tạo hóa đơn thành công")

    if (createdId) {
      router.push(`/admin/hoa-don/detail/${createdId}`)
    } else {
      router.push("/admin/hoa-don/list")
    }
  } catch (e) {
    console.error("Lỗi lưu hóa đơn:", e)
    error.value = e?.response?.data?.message || "Lưu hóa đơn thất bại"
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <main class="wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>{{ isEdit ? "Cập nhật / Chi tiết hóa đơn" : "Thêm hóa đơn" }}</h1>
          <small class="muted">
            {{ isEdit ? "Cập nhật thông tin đơn hàng" : "Tạo hóa đơn mới" }}
          </small>
        </div>

        <button class="btn" @click="router.push('/admin/hoa-don/list')">
          ← Quay lại
        </button>
      </div>

      <div class="body">
        <div v-if="error" class="note danger">
          {{ error }}
        </div>

        <div v-if="loading" class="note">
          Đang tải dữ liệu...
        </div>

        <template v-else>
          <div v-if="isEdit" class="card inner summary">
            <div><b>ID:</b> {{ form.id }}</div>
            <div><b>Mã hóa đơn:</b> {{ form.maHoaDon }}</div>
            <div>
              <b>Trạng thái:</b>
              {{ form.trangThaiName || form.trangThai }}
            </div>
            <div>
              <b>Ngày tạo:</b>
              {{ formatDateTime(form.ngayTao) }}
            </div>
          </div>

          <div class="form-grid">
            <div class="field">
              <label>Mã hóa đơn</label>
              <input
                v-model="form.maHoaDon"
                type="text"
                placeholder="(Có thể để trống nếu hệ thống tự sinh)"
              />
            </div>

            <div class="field">
              <label>Nhân viên</label>
              <select v-model="form.nhanVienId">
                <option value="">-- Chọn nhân viên --</option>
                <option v-for="nv in listNV" :key="nv.id" :value="nv.id">
                  {{ nv.tenNhanVien }}
                </option>
              </select>
            </div>

            <div class="field">
              <label>Khách hàng</label>
              <select v-model="form.khachHangId">
                <option value="">-- Chọn khách hàng --</option>
                <option v-for="kh in listKH" :key="kh.id" :value="kh.id">
                  {{ kh.tenKhachHang }}
                </option>
              </select>
            </div>

            <div class="field">
              <label>SĐT nhận hàng</label>
              <input v-model="form.soDienThoaiNhanHang" type="text" />
            </div>

            <div class="field full">
              <label>Địa chỉ nhận hàng</label>
              <input v-model="form.diaChiNhanHang" type="text" />
            </div>

            <div class="field">
              <label>Phí ship</label>
              <input
                v-model="form.phiShipText"
                type="text"
                placeholder="VD: 50000 hoặc 50.000"
              />
            </div>

            <div class="field">
              <label>Ngày nhận hàng dự kiến</label>
              <input v-model="form.ngayNhanHangDuKien" type="date" />
            </div>

            <div class="field">
              <label>Ngày nhận hàng mong muốn</label>
              <input v-model="form.ngayNhanHangMongMuon" type="date" />
            </div>

            <div class="field">
              <label>Trạng thái</label>
              <div class="readonly-box">
                <b v-if="!isEdit">Chờ xác nhận</b>
                <span v-else>{{ form.trangThaiName || form.trangThai }}</span>
              </div>
            </div>
          </div>

          <div class="footer-actions">
            <button class="btn" @click="router.push('/admin/hoa-don/list')">
              Huỷ
            </button>

            <button class="btn primary" @click="handleSubmit" :disabled="saving">
              {{ saving ? "Đang lưu..." : (isEdit ? "Cập nhật thông tin" : "Lưu") }}
            </button>
          </div>
        </template>
      </div>
    </div>
  </main>
</template>

<style scoped>
.summary{
  margin-bottom:16px;
  border:1px dashed #cfd8df;
  background:#fbfdff;
}

.form-grid{
  display:grid;
  grid-template-columns:repeat(2, minmax(0, 1fr));
  gap:14px;
}

.field{
  display:flex;
  flex-direction:column;
  gap:6px;
}

.field.full{
  grid-column:1 / -1;
}

.field input,
.field select{
  width:100%;
  padding:10px 12px;
  border:1px solid #d8dee4;
  border-radius:8px;
  outline:none;
  background:white;
}

.readonly-box{
  min-height:42px;
  display:flex;
  align-items:center;
  padding:10px 12px;
  border:1px solid #d8dee4;
  border-radius:8px;
  background:#fafafa;
}

.note{
  margin-bottom:12px;
  padding:10px 12px;
  border-radius:8px;
  background:#f3f4f6;
}

.note.danger{
  background:#fef2f2;
  color:#b91c1c;
}

.footer-actions{
  display:flex;
  justify-content:flex-end;
  gap:10px;
  margin-top:16px;
}
</style>