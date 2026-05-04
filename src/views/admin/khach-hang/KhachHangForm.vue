<script setup>
import { ref, onMounted, computed } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  createKhachHang,
  updateKhachHang,
  getKhachHangById,
  listAllKhachHang
} from "../../../services/KhachHangService"
import {
  getHoaDonByKhachHang
} from "../../../services/KhachHangService"
import { getDiaChiByKhachHang } from "../../../services/diaChiService"
import taiKhoanService from "../../../services/taiKhoanService"

const router = useRouter()
const route = useRoute()
const id = route.params.id
const isCreateMode = !id || String(id) === "new"

const form = ref({
  maKhachHang: "",
  tenKhachHang: "",
  gioiTinh: "Nam",
  ngaySinh: "",
  soDienThoai: "",
  email: "",
  diaChiNhanHang: "",
  trangThai: "Hoạt động"
})

const errors = ref({
  tenKhachHang: "",
  ngaySinh: "",
  soDienThoai: "",
  email: ""
})

const generatedPassword = ref("")

const hoaDonList = ref([])

const toList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const formatAddress = (addr) => {
  if (!addr) return ""
  return [addr.diaChiCuThe, addr.phuongXa, addr.quanHuyen, addr.tinhThanh]
    .map((part) => String(part || "").trim())
    .filter(Boolean)
    .join(", ")
}

onMounted(async () => {
  if (!isCreateMode) {
    const res = await getKhachHangById(id)

    form.value = {
      ...res.data,
      ngaySinh: res.data.ngaySinh
        ? res.data.ngaySinh.substring(0,10)
        : "",
      email: res.data.email || res.data.taiKhoan?.email || "",
      trangThai: res.data.trangThai || "Hoạt động"
    }

    try {
      const addressRes = await getDiaChiByKhachHang(id)
      const addressRows = toList(addressRes?.data)
      const preferred = addressRows.find((item) => item?.macDinh === true || item?.laMacDinh === true) || addressRows[0]
      const fullAddress = formatAddress(preferred)
      if (fullAddress) {
        form.value.diaChiNhanHang = fullAddress
      }
    } catch {
      // Keep existing customer payload address if address service is unavailable.
    }

    const hdRes = await getHoaDonByKhachHang(id)
    hoaDonList.value = hdRes.data
  }
})

const totalOrders = computed(() => hoaDonList.value.length)

const totalSpending = computed(() =>
  hoaDonList.value.reduce((sum, hd) =>
    sum + (hd.thanhTien || 0), 0)
)

const formatCurrency = (v) =>
  new Intl.NumberFormat("vi-VN").format(v) + "₫"

const taoMatKhauTam = () => {
  const part = String(Math.floor(100000 + Math.random() * 900000))
  return `DW@${part}`
}

const normalizeEmail = (value = "") => String(value || "").trim().toLowerCase()
const normalizePhone = (value = "") => String(value || "").replace(/\s+/g, "").trim()

const listAllTaiKhoan = async () => {
  const size = 200
  const collected = []
  const visited = new Set()
  let page = 0
  let totalPages = 1

  while (page < totalPages) {
    const res = await taiKhoanService.getAll({ page, size })
    const payload = res?.data
    const accounts = Array.isArray(payload)
      ? payload
      : (Array.isArray(payload?.content) ? payload.content : [])

    for (const item of accounts) {
      const key = String(item?.id || "")
      if (!key || visited.has(key)) continue
      visited.add(key)
      collected.push(item)
    }

    const detectedTotalPages = Number(payload?.totalPages)
    if (Number.isFinite(detectedTotalPages) && detectedTotalPages > 0) {
      totalPages = detectedTotalPages
    } else if (accounts.length < size) {
      break
    } else {
      totalPages = page + 2
    }

    page += 1
  }

  return collected
}

const findDuplicateCustomerPhone = async (phone, excludeId = null) => {
  const normalizedPhone = normalizePhone(phone)
  if (!normalizedPhone) return null

  const customers = await listAllKhachHang(100)
  return customers.find((item) => {
    if (excludeId && Number(item?.id) === Number(excludeId)) return false
    return normalizePhone(item?.soDienThoai) === normalizedPhone
  }) || null
}

const checkEmailExists = async (email) => {
  const normalized = normalizeEmail(email)
  if (!normalized) return false
  const accounts = await listAllTaiKhoan()
  return accounts.some((item) => normalizeEmail(item?.email) === normalized)
}

const taoTaiKhoanKhachHang = async (email, matKhau) => {
  const normalizedEmail = String(email || "").trim().toLowerCase()
  const payloadCandidates = [
    {
      email: normalizedEmail,
      username: normalizedEmail,
      matKhau,
      vaiTro: "CUSTOMER",
      trangThaiHoatDong: "Hoạt động",
      trangThaiTaiKhoan: "Kích hoạt"
    },
    {
      email: normalizedEmail,
      tenDangNhap: normalizedEmail,
      matKhau,
      vaiTro: "ROLE_CUSTOMER",
      trangThaiHoatDong: "Hoạt động",
      trangThaiTaiKhoan: "Kích hoạt"
    },
    {
      email: normalizedEmail,
      username: normalizedEmail,
      password: matKhau,
      vaiTro: "CUSTOMER",
      trangThaiHoatDong: "Hoạt động",
      trangThaiTaiKhoan: "Kích hoạt"
    }
  ]

  let lastError = null
  for (const payload of payloadCandidates) {
    try {
      const res = await taiKhoanService.create(payload)
      const accountId = Number(res?.data?.id || res?.data?.data?.id)
      if (accountId > 0) return accountId
    } catch (error) {
      lastError = error
    }
  }

  throw lastError || new Error("Không tạo được tài khoản khách hàng")
}

// Validation helpers
const isValidName = (name) => {
  const trimmed = String(name || "").trim()
  if (!trimmed) return false
  // Only Vietnamese letters and spaces
  return /^[a-zA-ZÀ-ỳ\s]+$/.test(trimmed) && trimmed.length <= 100
}

const isValidPhone = (phone) => {
  const trimmed = String(phone || "").trim()
  if (!trimmed) return false
  // 10 digits, starts with 0
  return /^0\d{9}$/.test(trimmed)
}

const formatPhoneDisplay = (phone) => {
  const str = String(phone || "").trim()
  if (str.length > 50) return str.substring(0, 47) + "..."
  return str
}

const save = async () => {
  // Clear errors
  errors.value = { tenKhachHang: "", ngaySinh: "", soDienThoai: "", email: "" }

  // Validate name
  if (!isValidName(form.value.tenKhachHang)) {
    errors.value.tenKhachHang = "Họ tên chỉ được chứa chữ cái và không quá 100 ký tự"
    window.toast.error(errors.value.tenKhachHang)
    return
  }

  if (!normalizePhone(form.value.soDienThoai)) {
    errors.value.soDienThoai = "Vui lòng nhập số điện thoại"
    window.toast.error(errors.value.soDienThoai)
    return
  }

  if (!isValidPhone(form.value.soDienThoai)) {
    errors.value.soDienThoai = "Số điện thoại phải có 10 chữ số và bắt đầu bằng 0"
    window.toast.error(errors.value.soDienThoai)
    return
  }

  // Validate birth date if provided - must not be empty on create
  if (isCreateMode && !form.value.ngaySinh) {
    errors.value.ngaySinh = "Vui lòng nhập ngày sinh"
    window.toast.error(errors.value.ngaySinh)
    return
  }

  // Confirmation before saving
  const action = isCreateMode ? 'tạo mới' : 'cập nhật'
  const confirmed = await window.confirmDialog(`Bạn có chắc chắn muốn ${action} thông tin khách hàng "${form.value.tenKhachHang}"?`)
  if (!confirmed) {
    return
  }

  try {
    if (isCreateMode) {
      const normalizedEmail = normalizeEmail(form.value.email)
      const normalizedPhone = normalizePhone(form.value.soDienThoai)
      if (!normalizedEmail) {
        window.toast.error("Vui lòng nhập email để tạo tài khoản khách hàng")
        return
      }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
        window.toast.error("Email không đúng định dạng")
        return
      }

      if (await checkEmailExists(normalizedEmail)) {
        errors.value.email = "Email đã tồn tại trong hệ thống"
        window.toast.error(errors.value.email)
        return
      }

      if (await findDuplicateCustomerPhone(normalizedPhone)) {
        errors.value.soDienThoai = "Số điện thoại đã tồn tại trong hệ thống"
        window.toast.error(errors.value.soDienThoai)
        return
      }

      const matKhauTam = taoMatKhauTam()
      let taiKhoanId = null
      try {
        taiKhoanId = await taoTaiKhoanKhachHang(normalizedEmail, matKhauTam)

        await createKhachHang({
          ...form.value,
          email: normalizedEmail,
          soDienThoai: normalizedPhone,
          idTaiKhoan: taiKhoanId,
          taiKhoan: { id: taiKhoanId },
          trangThai: form.value.trangThai || "Hoạt động"
        })
      } catch (error) {
        if (taiKhoanId) {
          await taiKhoanService.delete(taiKhoanId).catch(() => {})
        }
        throw error
      }

      generatedPassword.value = matKhauTam
      window.toast.success(`Tạo mới khách hàng thành công! Mật khẩu tạm: ${matKhauTam}`)
      router.push("/admin/khach-hang/list")
      return
    }

    await updateKhachHang(id, form.value)
    window.toast.success('Cập nhật khách hàng thành công!')
    router.push("/admin/khach-hang/list")
  } catch (error) {
    window.toast.error('Có lỗi xảy ra: ' + (error.message || 'Vui lòng thử lại'))
  }
}
</script>

<template>
  <main class="wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>{{ isCreateMode ? 'Thêm khách hàng' : 'Form khách hàng' }}</h1>
          <small class="muted">
            Tạo mới / cập nhật thông tin khách
          </small>
        </div>

        <div style="display:flex;gap:10px">
          <button
            class="btn"
            @click="router.push('/admin/khach-hang/list')"
          >
            ← Quay lại
          </button>

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
            <label>Mã khách</label>
            <input
              class="input auto-code-input"
              readonly
              value="Mã tự sinh"
            />
          </div>

          <div class="field">
            <label>Họ tên <span class="req">*</span></label>
            <input
              class="input"
              :class="errors.tenKhachHang ? 'error' : ''"
              v-model="form.tenKhachHang"
              placeholder="VD: Nguyễn Văn A (tối đa 100 ký tự)"
              maxlength="100"
            />
            <small v-if="errors.tenKhachHang" class="error-text">{{ errors.tenKhachHang }}</small>
          </div>

          <div class="field">
            <label>Giới tính</label>
            <select
              class="input"
              v-model="form.gioiTinh"
            >
              <option>Nam</option>
              <option>Nữ</option>
            </select>
          </div>

          <div class="field">
            <label>Ngày sinh <span class="req" v-if="isCreateMode">*</span></label>
            <input
              class="input"
              :class="errors.ngaySinh ? 'error' : ''"
              type="date"
              v-model="form.ngaySinh"
            />
            <small v-if="errors.ngaySinh" class="error-text">{{ errors.ngaySinh }}</small>
          </div>

          <div class="field">
            <label>SĐT (10 chữ số)</label>
            <input
              class="input"
              :class="errors.soDienThoai ? 'error' : ''"
              v-model="form.soDienThoai"
              placeholder="VD: 0912xxxxxx"
              maxlength="10"
              pattern="0\d{9}"
            />
            <small v-if="errors.soDienThoai" class="error-text">{{ errors.soDienThoai }}</small>
          </div>

          <div class="field" v-if="isCreateMode">
            <label>Email đăng nhập <span class="req">*</span></label>
            <input
              class="input"
              v-model="form.email"
              placeholder="VD: khachhang@dirtywave.com"
            />
          </div>

          <div class="field">
            <label>Địa chỉ</label>
            <input
              class="input"
              v-model="form.diaChiNhanHang"
              placeholder="VD: 123 Đường ABC, Quận XYZ, TP. HCM"
            />
          </div>

          <div class="field">
            <label>Trạng thái</label>
            <select
              class="input"
              :class="form.trangThai === 'Hoạt động' ? 'status-ok' : 'status-bad'"
              v-model="form.trangThai"
            >
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
          </div>

        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.input {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d8dee9;
  background-color: #fff;
  font-size: 14px;
  transition: 0.2s ease;
}
select.input {
  padding-right: 34px;
}

.input:focus {
  outline: none;
  border-color: #4f46e5;
}

.input.error {
  border-color: #dc2626;
  background-color: #fef2f2;
}

.error-text {
  color: #dc2626;
  font-size: 12px;
  margin-top: 4px;
  display: block;
}

.req {
  color: #dc2626;
}

.status-ok {
  background-color: #dcfce7;
  color: #166534;
  border-color: #86efac;
  font-weight: 500;
}

.status-bad {
  background-color: #fee2e2;
  color: #991b1b;
  border-color: #fca5a5;
  font-weight: 500;
}

.auto-code-input {
  background: #f1f5f9;
  color: #64748b;
  border-color: #cbd5e1;
  font-weight: 600;
}

.req {
  color: #dc2626;
}
@media (max-width: 768px) {
  .head { flex-direction: column; align-items: flex-start; gap: 12px; }
}
</style>