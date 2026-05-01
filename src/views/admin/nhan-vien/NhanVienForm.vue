<script setup>
import { reactive, onMounted, ref } from "vue"
import { useRouter, useRoute } from "vue-router"
import {
  getAllNhanVien,
  createNhanVien,
  updateNhanVien,
  getNhanVienById,
  deleteNhanVien
} from "../../../services/nhanVienService"
import {
  getLichLamViecByNhanVien
} from "../../../services/lichLamViecService"
import taiKhoanService from "../../../services/taiKhoanService"

const router = useRouter()
const route = useRoute()
const id = route.params.id
const originalTaiKhoanEmail = ref("")
const sendingMail = ref(false)
const deletingEmployee = ref(false)

const form = reactive({
  code: "",
  name: "",
  email: "",
  phone: "",
  role: "NHAN_VIEN",
  status: "Hoạt động",
  password: "",
  note: "",
  taiKhoanId: null,
  chucVuId: null
})

// Validation errors
const errors = reactive({
  name: "",
  email: "",
  phone: ""
})

const toBackendRole = (role) => role === "ADMIN" ? "ADMIN" : "EMPLOYEE"

const isValidEmail = (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value || "").trim())

const isDuplicateEmailError = (message = "") => {
  const normalized = String(message || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
  return normalized.includes("email") && (
    normalized.includes("ton tai") ||
    normalized.includes("already") ||
    normalized.includes("duplicate")
  )
}

const normalizeEmail = (value = "") => String(value || "").trim().toLowerCase()

const roleLabel = (role = "") => {
  const normalized = normalizeRole(role)
  if (normalized === "ADMIN") return "ADMIN"
  if (isEmployeeRole(normalized)) return "EMPLOYEE"
  return normalized || "UNKNOWN"
}

const listAllTaiKhoan = async () => {
  const size = 500
  let page = 0
  const collected = []
  const visited = new Set()

  while (page < 50) {
    const res = await taiKhoanService.getAll({ page, size })
    const payload = res?.data

    if (Array.isArray(payload)) {
      return payload
    }

    const content = Array.isArray(payload?.content) ? payload.content : []
    for (const item of content) {
      const key = String(item?.id || "")
      if (!key || visited.has(key)) continue
      visited.add(key)
      collected.push(item)
    }

    const totalPages = Number(payload?.totalPages || 0)
    if (!content.length) break
    if (Number.isFinite(totalPages) && totalPages > 0 && page >= totalPages - 1) break
    if (content.length < size && (!Number.isFinite(totalPages) || totalPages <= 0)) break
    page += 1
  }

  return collected
}

const checkEmailExists = async (email) => {
  const normalized = normalizeEmail(email)
  if (!normalized) return false
  try {
    const accounts = await listAllTaiKhoan()
    return accounts.some((acc) => String(acc?.email || "").trim().toLowerCase() === normalized)
  } catch {
    return false
  }
}

const isEmailUsedByAnotherAccount = async (email, currentTaiKhoanId = null) => {
  const normalized = normalizeEmail(email)
  if (!normalized) return false
  try {
    const accounts = await listAllTaiKhoan()

    return accounts.some((acc) => {
      const accEmail = normalizeEmail(acc?.email)
      const accId = Number(acc?.id || 0)
      if (accEmail !== normalized) return false
      if (currentTaiKhoanId && accId === Number(currentTaiKhoanId)) return false
      return true
    })
  } catch {
    return false
  }
}

const taoMatKhauTam = () => {
  const part = String(Math.floor(100000 + Math.random() * 900000))
  return `DW@${part}`
}

const guiMailThongTinTaiKhoan = async (email, password, role, tenNhanVien) => {
  if (!email || !password) {
    return { success: false, message: "Thiếu email hoặc mật khẩu tạm" }
  }

  const NODE = String(import.meta.env.VITE_NODE_BACKEND_URL || 'http://localhost:3000').trim().replace(/\/$/, '')

  const endpoints = [
    NODE ? `${NODE}/api/mail/send-nhanvien-account` : "",
    "http://localhost:3000/api/mail/send-nhanvien-account",
  ].filter(Boolean)

  let lastMessage = "Không kết nối được dịch vụ gửi mail"
  let hasUsefulHttpMessage = false

  for (const endpoint of endpoints) {
    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, matKhau: password, vaiTro: role, tenNhanVien: tenNhanVien || email })
      })

      let payload = null
      try {
        payload = await response.json()
      } catch {
        payload = null
      }

      if (response.ok) {
        return { success: true, message: "Đã gửi email tài khoản", endpoint }
      }

      const httpMessage = payload?.message || `Gửi mail thất bại (${response.status})`
      lastMessage = httpMessage
      hasUsefulHttpMessage = true
    } catch (e) {
      if (!hasUsefulHttpMessage) {
        lastMessage = e?.message || "Lỗi mạng khi gửi mail"
      }
    }
  }

  console.warn('[NhanVien] Gửi email tài khoản thất bại:', lastMessage)
  return { success: false, message: lastMessage }
}

function validate() {
  let valid = true

  // Họ tên: bắt buộc, tối thiểu 2 ký tự
  const trimmedName = (form.name || "").trim()
  if (!trimmedName) {
    errors.name = "Họ tên không được để trống"
    valid = false
  } else if (trimmedName.length < 2) {
    errors.name = "Họ tên phải có ít nhất 2 ký tự"
    valid = false
  } else {
    errors.name = ""
  }

  // Email: bắt buộc khi tạo mới
  const trimmedEmail = (form.email || "").trim()
  if (!id) {
    if (!trimmedEmail) {
      errors.email = "Email không được để trống"
      valid = false
    } else if (trimmedEmail.length > 100) {
      errors.email = "Email không được vượt quá 100 ký tự"
      valid = false
    } else if (!isValidEmail(trimmedEmail)) {
      errors.email = "Email không đúng định dạng"
      valid = false
    } else {
      errors.email = ""
    }
  } else {
    if (trimmedEmail) {
      if (trimmedEmail.length > 100) {
        errors.email = "Email không được vượt quá 100 ký tự"
        valid = false
      } else if (!isValidEmail(trimmedEmail)) {
        errors.email = "Email không đúng định dạng"
        valid = false
      } else {
        errors.email = ""
      }
    } else {
      errors.email = ""
    }
  }

  // SĐT: bắt buộc, 10–11 chữ số
  const trimmedPhone = (form.phone || "").trim()
  if (!trimmedPhone) {
    errors.phone = "Số điện thoại không được để trống"
    valid = false
  } else if (!/^\d{10,11}$/.test(trimmedPhone)) {
    errors.phone = "SĐT phải gồm 10–11 chữ số"
    valid = false
  } else {
    errors.phone = ""
  }

  return valid
}

function goBack() {
  router.push("/admin/nhan-vien/list")
}

const extractErrorMessage = (err) => String(
  err?.response?.data?.message
  || err?.response?.data?.error
  || err?.message
  || ""
)

const isScheduleFkConflict = (message) => /FK_LichLamViec_NhanVien|conflicted with the REFERENCE constraint/i.test(message)

const normalizeRole = (role) => String(role || "").trim().toUpperCase().replace(/^ROLE_/, "")
const isEmployeeRole = (role) => {
  const normalized = normalizeRole(role)
  return normalized === "EMPLOYEE" || normalized === "NHAN_VIEN" || normalized === "NHANVIEN"
}

async function resolveTaiKhoanIdForDelete() {
  if (form.taiKhoanId) return Number(form.taiKhoanId)

  const normalizedEmail = String(form.email || "").trim().toLowerCase()
  if (!normalizedEmail) return null

  try {
    const res = await taiKhoanService.getAll({ page: 0, size: 500 })
    const payload = res?.data
    const accounts = Array.isArray(payload)
      ? payload
      : (Array.isArray(payload?.content) ? payload.content : [])

    const matched = accounts.find((acc) => {
      const sameEmail = String(acc?.email || "").trim().toLowerCase() === normalizedEmail
      return sameEmail && isEmployeeRole(acc?.vaiTro)
    })
    return matched?.id ? Number(matched.id) : null
  } catch {
    return null
  }
}

async function cleanupOrphanEmployeeAccountByEmail(email) {
  const normalizedEmail = normalizeEmail(email)
  if (!normalizedEmail) return false

  try {
    const accounts = await listAllTaiKhoan()

    const matchedAccount = accounts.find((acc) => {
      const sameEmail = normalizeEmail(acc?.email) === normalizedEmail
      return sameEmail && isEmployeeRole(acc?.vaiTro)
    })

    if (!matchedAccount?.id) return false

    const employeeRes = await getAllNhanVien()
    const employees = Array.isArray(employeeRes?.data) ? employeeRes.data : []

    const isLinked = employees.some((emp) => {
      const linkedTaiKhoanId = Number(emp?.idTaiKhoan || emp?.taiKhoan?.id || 0)
      return linkedTaiKhoanId && linkedTaiKhoanId === Number(matchedAccount.id)
    })

    if (isLinked) return false

    await taiKhoanService.delete(matchedAccount.id)
    return true
  } catch {
    return false
  }
}

async function reclaimOrphanEmployeeEmail(targetEmail, currentTaiKhoanId = null) {
  const normalizedEmail = normalizeEmail(targetEmail)
  if (!normalizedEmail) return false

  try {
    const accounts = await listAllTaiKhoan()
    const conflicted = accounts.find((acc) => {
      const sameEmail = normalizeEmail(acc?.email) === normalizedEmail
      if (!sameEmail) return false
      if (currentTaiKhoanId && Number(acc?.id || 0) === Number(currentTaiKhoanId)) return false
      return true
    })

    if (!conflicted?.id) return false
    if (!isEmployeeRole(conflicted?.vaiTro)) return false

    const employeeRes = await getAllNhanVien()
    const employees = Array.isArray(employeeRes?.data) ? employeeRes.data : []
    const linkedEmployee = employees.find((emp) => {
      const linkedTaiKhoanId = Number(emp?.idTaiKhoan || emp?.taiKhoan?.id || 0)
      return linkedTaiKhoanId && linkedTaiKhoanId === Number(conflicted.id)
    })

    // Do not auto-reclaim email if that account is still attached to another employee record.
    if (linkedEmployee && Number(linkedEmployee?.id || 0) !== Number(id || 0)) {
      return false
    }

    const fallbackEmail = `migrated+reclaim-${conflicted.id}-${Date.now()}@dirtywave.local`

    const detailRes = await taiKhoanService.getById(conflicted.id).catch(() => null)
    const detail = detailRes?.data || conflicted || {}
    const reclaimPayload = {
      ...detail,
      email: fallbackEmail,
      id: undefined,
      matKhau: undefined,
      password: undefined,
      currentPassword: undefined,
      newPassword: undefined,
    }

    await taiKhoanService.update(conflicted.id, reclaimPayload)
    return true
  } catch {
    return false
  }
}

async function findConflictingAccountByEmail(targetEmail, currentTaiKhoanId = null) {
  const normalizedEmail = normalizeEmail(targetEmail)
  if (!normalizedEmail) return null

  try {
    const accounts = await listAllTaiKhoan()
    return accounts.find((acc) => {
      const sameEmail = normalizeEmail(acc?.email) === normalizedEmail
      if (!sameEmail) return false
      if (currentTaiKhoanId && Number(acc?.id || 0) === Number(currentTaiKhoanId)) return false
      return true
    }) || null
  } catch {
    return null
  }
}

const normalizeDateKey = (value) => {
  const raw = String(value || "").trim()
  if (!raw) return ""
  const direct = raw.slice(0, 10)
  if (/^\d{4}-\d{2}-\d{2}$/.test(direct)) return direct

  const parsed = new Date(raw)
  if (Number.isNaN(parsed.getTime())) return ""
  return parsed.toISOString().slice(0, 10)
}

const isInactiveScheduleStatus = (value) => {
  const normalized = String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
  return normalized.includes("ngung") || normalized.includes("huy") || normalized.includes("inactive")
}

async function getBlockingSchedulesByEmployeeId(employeeId) {
  try {
    const res = await getLichLamViecByNhanVien(employeeId)
    const payload = res?.data
    const schedules = Array.isArray(payload)
      ? payload
      : (Array.isArray(payload?.content) ? payload.content : [])

    if (!schedules.length) return []

    const todayKey = new Date().toISOString().slice(0, 10)
    return schedules.filter((item) => {
      const status = String(item?.trangThai || item?.status || "")
      const dateKey = normalizeDateKey(item?.ngayLam || item?.dateKey || item?.ngay)

      if (isInactiveScheduleStatus(status)) return false
      if (!dateKey) return true

      // Block delete for current/future assigned shifts.
      return dateKey >= todayKey
    })
  } catch {
    return []
  }
}

async function sendAccountMailManually() {
  if (!id) {
    window.toast?.warning?.("Vui lòng lưu nhân viên trước khi gửi email")
    return
  }

  const normalizedEmail = String(form.email || "").trim().toLowerCase()
  if (!isValidEmail(normalizedEmail)) {
    errors.email = "Email không đúng định dạng"
    window.toast?.error?.("Email không đúng định dạng")
    return
  }

  const confirmed = await window.confirmDialog?.("Gửi email tài khoản cho nhân viên này?") ?? confirm("Gửi email tài khoản cho nhân viên này?")
  if (!confirmed) return

  sendingMail.value = true
  try {
    const generatedPassword = form.password || taoMatKhauTam()

    if (form.taiKhoanId) {
      const normalizedCurrentEmail = String(form.email || "").trim().toLowerCase()
      const emailChanged = normalizedCurrentEmail && normalizedCurrentEmail !== originalTaiKhoanEmail.value

      if (emailChanged) {
        const usedByAnother = await isEmailUsedByAnotherAccount(normalizedCurrentEmail, form.taiKhoanId)
        if (usedByAnother) {
          const reclaimed = await reclaimOrphanEmployeeEmail(normalizedCurrentEmail, form.taiKhoanId)
          if (!reclaimed) {
            const conflict = await findConflictingAccountByEmail(normalizedCurrentEmail, form.taiKhoanId)
            errors.email = "Email đã tồn tại trong hệ thống tài khoản"
            window.toast?.error?.("Email đã tồn tại trong hệ thống tài khoản")
            return
          }
        }
      }

      const accountUpdatePayload = {
        vaiTro: toBackendRole(form.role),
        trangThaiHoatDong: form.status === "Ngừng hoạt động" ? "Ngừng hoạt động" : "Hoạt động",
        matKhau: generatedPassword
      }

      if (emailChanged) {
        accountUpdatePayload.email = normalizedCurrentEmail
      }

      try {
        await taiKhoanService.update(form.taiKhoanId, accountUpdatePayload)
      } catch (error) {
        const message = extractErrorMessage(error)
        if (emailChanged && isDuplicateEmailError(message)) {
          const reclaimed = await reclaimOrphanEmployeeEmail(normalizedCurrentEmail, form.taiKhoanId)
          if (reclaimed) {
            await taiKhoanService.update(form.taiKhoanId, accountUpdatePayload)
          } else {
            window.toast?.error?.("Email đã tồn tại trong hệ thống tài khoản")
            throw error
          }
        } else {
          throw error
        }
      }
      if (emailChanged) {
        originalTaiKhoanEmail.value = normalizedCurrentEmail
      }
    }

    const mailResult = await guiMailThongTinTaiKhoan(normalizedEmail, generatedPassword, form.role, form.name.trim())
    if (!mailResult.success) {
      window.toast?.warning?.(`Gửi email thất bại: ${mailResult.message}`)
      return
    }

    form.password = ""
    window.toast?.success?.("Đã gửi email tài khoản cho nhân viên")
  } catch (err) {
    window.toast?.error?.("Không thể gửi email: " + (err?.response?.data?.message || err?.message || "Lỗi không xác định"))
  } finally {
    sendingMail.value = false
  }
}

async function removeEmployee() {
  if (!id) return
  const confirmed = await window.confirmDialog?.("Xóa vĩnh viễn nhân viên này? Hành động không thể hoàn tác.") ?? confirm("Xóa vĩnh viễn nhân viên này? Hành động không thể hoàn tác.")
  if (!confirmed) return

  deletingEmployee.value = true
  try {
    const blockingSchedules = await getBlockingSchedulesByEmployeeId(id)
    if (blockingSchedules.length > 0) {
      window.toast?.error?.("Không thể xóa nhân viên vì đang có ca làm việc hiện tại/sắp tới. Vui lòng chuyển các ca này sang nhân viên khác trước khi xóa.")
      return
    }

    await deleteNhanVien(id)

    const taiKhoanIdToDelete = await resolveTaiKhoanIdForDelete()
    let accountDeleteFailed = false

    if (taiKhoanIdToDelete) {
      try {
        await taiKhoanService.delete(taiKhoanIdToDelete)
      } catch {
        accountDeleteFailed = true
      }
    }

    if (accountDeleteFailed) {
      window.toast?.warning?.("Đã xóa nhân viên nhưng chưa xóa được tài khoản liên kết. Vui lòng thử lại thao tác xóa tài khoản.")
    } else {
      window.toast?.success?.("Đã xóa vĩnh viễn nhân viên và dữ liệu liên quan")
    }

    router.push("/admin/nhan-vien/list")
  } catch (err) {
    const message = extractErrorMessage(err)

    if (isScheduleFkConflict(message)) {
      window.toast?.error?.("Không thể xóa nhân viên vì vẫn còn lịch/ca làm việc đang gán. Vui lòng chuyển lịch đó sang nhân viên khác rồi thử lại.")
      return
    }

    window.toast?.error?.("Không thể xóa nhân viên. Vui lòng thử lại.")
  } finally {
    deletingEmployee.value = false
  }
}

onMounted(async () => {
  if (id) {
    const res = await getNhanVienById(id)
    const data = res.data

    form.code = data.maNhanVien || ""
    form.name = data.tenNhanVien || ""
    form.phone = data.soDienThoai || ""
    form.email = data.email || data.taiKhoan?.email || ""
    originalTaiKhoanEmail.value = String(form.email || "").trim().toLowerCase()
    form.role = data.chucVu?.tenChucVu === "ADMIN" ? "ADMIN" : "NHAN_VIEN"
    form.taiKhoanId = data.taiKhoan?.id || null
    form.chucVuId = data.chucVu?.id || null
    form.note = data.ghiChu || ""
    form.status = data.trangThaiHoatDong === "Ngừng hoạt động"
      ? "Ngừng hoạt động"
      : "Hoạt động"
  }
})

async function save() {
  if (!validate()) return

  try {
    const action = id ? "cập nhật" : "tạo mới"
    const confirmed = await window.confirmDialog?.(`Bạn có chắc muốn ${action} nhân viên này không?`) ?? confirm(`Bạn có chắc muốn ${action} nhân viên này không?`)
    if (!confirmed) return

    if (!id) {
      const normalizedEmail = String(form.email || "").trim().toLowerCase()
      if (!isValidEmail(normalizedEmail)) {
        errors.email = "Email không đúng định dạng"
        window.toast?.error?.("Email không đúng định dạng")
        return
      }
      const emailExists = await checkEmailExists(normalizedEmail)
      if (emailExists) {
        const cleaned = await cleanupOrphanEmployeeAccountByEmail(normalizedEmail)
        if (!cleaned) {
          errors.email = "Email đã tồn tại"
          window.toast?.error?.("Email đã tồn tại, vui lòng dùng email khác")
          return
        }
      }
      form.email = normalizedEmail
    }

    const matKhauTam = form.password || taoMatKhauTam()

    const nhanVienPayload = {
      maNhanVien: form.code,
      tenNhanVien: form.name.trim(),
      soDienThoai: form.phone.trim(),
      ghiChu: form.note,
      chucVu: form.chucVuId
        ? { id: form.chucVuId }
        : { maChucVu: toBackendRole(form.role) },
      trangThaiHoatDong: form.status === "Ngừng hoạt động"
        ? "Ngừng hoạt động"
        : "Hoạt động",
      taiKhoan: form.taiKhoanId ? { id: form.taiKhoanId } : null
    }

    if (id) {
      if (form.taiKhoanId) {
        const normalizedCurrentEmail = String(form.email || "").trim().toLowerCase()
        const emailChanged = normalizedCurrentEmail && normalizedCurrentEmail !== originalTaiKhoanEmail.value

        if (emailChanged) {
          const usedByAnother = await isEmailUsedByAnotherAccount(normalizedCurrentEmail, form.taiKhoanId)
          if (usedByAnother) {
            const reclaimed = await reclaimOrphanEmployeeEmail(normalizedCurrentEmail, form.taiKhoanId)
            if (!reclaimed) {
              errors.email = "Email đã tồn tại trong hệ thống tài khoản"
              window.toast?.error?.("Email đã tồn tại trong hệ thống tài khoản")
              return
            }
          }
        }

        const accountUpdatePayload = {
          vaiTro: toBackendRole(form.role),
          trangThaiHoatDong: form.status === "Ngừng hoạt động" ? "Ngừng hoạt động" : "Hoạt động",
          matKhau: form.password || undefined
        }

        // Backend currently validates duplicate email strictly on update.
        // Only send email field when user actually changes it.
        if (emailChanged) {
          accountUpdatePayload.email = normalizedCurrentEmail
        }

        try {
          await taiKhoanService.update(form.taiKhoanId, accountUpdatePayload)
        } catch (error) {
          const message = extractErrorMessage(error)
          if (emailChanged && isDuplicateEmailError(message)) {
            const reclaimed = await reclaimOrphanEmployeeEmail(normalizedCurrentEmail, form.taiKhoanId)
            if (reclaimed) {
              await taiKhoanService.update(form.taiKhoanId, accountUpdatePayload)
            } else {
              window.toast?.error?.("Email đã tồn tại trong hệ thống tài khoản")
              throw error
            }
          } else {
            throw error
          }
        }
        if (emailChanged) {
          originalTaiKhoanEmail.value = normalizedCurrentEmail
        }
      }
      await updateNhanVien(id, nhanVienPayload)
      window.toast?.success?.("Cập nhật nhân viên thành công!")
    } else {
      try {
        const accountRes = await taiKhoanService.create({
          email: form.email.trim(),
          matKhau: matKhauTam,
          vaiTro: toBackendRole(form.role),
          trangThaiHoatDong: "Hoạt động",
          trangThaiTaiKhoan: "Kích hoạt"
        })

        const createdTaiKhoanId = accountRes?.data?.id
        if (!createdTaiKhoanId) {
          throw new Error("Không tạo được tài khoản cho nhân viên")
        }

        await createNhanVien({
          ...nhanVienPayload,
          taiKhoan: { id: createdTaiKhoanId }
        })
      } catch {
        await createNhanVien({
          ...nhanVienPayload,
          taiKhoan: null,
          email: form.email,
          matKhau: matKhauTam,
          vaiTro: toBackendRole(form.role)
        })
      }
      window.toast?.success?.("Thêm nhân viên thành công! Vào chi tiết nhân viên để gửi email tài khoản.")
    }

    router.push("/admin/nhan-vien/list")

  } catch (err) {
    console.error(err)
    window.toast?.error?.("Lưu thất bại: " + (err?.response?.data?.message || err?.message || "Lỗi không xác định"))
  }
}
</script>

<template>
  <main class="wrap">
    <div class="card">
      <div class="head">
        <div>
          <h1>{{ id ? 'Chỉnh sửa nhân viên' : 'Thêm nhân viên mới' }}</h1>
          <small class="muted">
            {{ id ? 'Cập nhật thông tin hồ sơ nhân viên' : 'Điền đầy đủ thông tin để tạo tài khoản nhân viên' }}
          </small>
        </div>

        <div style="display:flex;gap:10px">
          <button class="btn" @click="goBack">← Quay lại</button>
          <button v-if="id" class="btn" :disabled="sendingMail" @click="sendAccountMailManually">
            {{ sendingMail ? 'Đang gửi...' : 'Gửi email tài khoản' }}
          </button>
          <button v-if="id" class="btn danger" :disabled="deletingEmployee" @click="removeEmployee">
            {{ deletingEmployee ? 'Đang xóa...' : 'Xóa nhân viên' }}
          </button>
          <button class="btn primary" @click="save">Lưu</button>
        </div>
      </div>

      <div class="body">
        <div class="grid cols2">

          <!-- Mã nhân viên -->
          <div class="field">
            <label>Mã nhân viên</label>
            <input class="auto-code-input" readonly :value="id ? form.code : 'Mã tự sinh'" />
            <small class="hint">Mã được hệ thống tự động sinh khi tạo mới</small>
          </div>

          <!-- Họ tên (bắt buộc) -->
          <div class="field" :class="{ 'has-error': errors.name }">
            <label>Họ tên <span class="req">*</span></label>
            <input v-model="form.name" placeholder="VD: Nguyễn Văn C" @blur="validate" />
            <small v-if="errors.name" class="err-msg">{{ errors.name }}</small>
            <small v-else class="hint">Tối thiểu 2 ký tự, không để trống</small>
          </div>

          <!-- Vai trò -->
          <div class="field">
            <label>Vai trò <span class="req">*</span></label>
            <select v-model="form.role">
              <option value="ADMIN">Quản trị viên</option>
              <option value="NHAN_VIEN">Nhân viên bán hàng</option>
            </select>
            <small class="hint">Phân quyền truy cập vào hệ thống quản trị</small>
          </div>

          <!-- Email -->
          <div class="field" :class="{ 'has-error': errors.email }">
            <label>Email {{ !id ? '' : '' }} <span v-if="!id" class="req">*</span></label>
            <input type="email" v-model="form.email"
                   :placeholder="id ? 'Để trống nếu không đổi email' : 'VD: nhanvien@dirtywave.com'"
                   maxlength="100"
                   @blur="validate" />
            <small v-if="errors.email" class="err-msg">{{ errors.email }}</small>
            <small v-else class="hint">
              {{ id ? 'Dùng để đăng nhập. Cập nhật cẩn thận.' : 'Bắt buộc – dùng để đăng nhập hệ thống' }}
            </small>
          </div>

          <!-- SĐT (bắt buộc) -->
          <div class="field" :class="{ 'has-error': errors.phone }">
            <label>Số điện thoại <span class="req">*</span></label>
            <input type="tel" v-model="form.phone" placeholder="VD: 0912345678" maxlength="11" @blur="validate" />
            <small v-if="errors.phone" class="err-msg">{{ errors.phone }}</small>
            <small v-else class="hint">10–11 chữ số, không có dấu cách hoặc dấu gạch</small>
          </div>

          <!-- Trạng thái (chỉ sửa khi đã có nhân viên) -->
          <div class="field" v-if="id">
            <label>Trạng thái hoạt động</label>
            <select v-model="form.status">
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
            <small class="hint">Nhân viên ngừng hoạt động không thể đăng nhập</small>
          </div>

          <!-- Mật khẩu -->
          <div class="field" style="grid-column:1/-1">
            <label>Mật khẩu</label>
            <input type="text" v-model="form.password"
                   :placeholder="id ? 'Để trống để giữ nguyên mật khẩu hiện tại' : 'Để trống để hệ thống tự tạo mật khẩu tạm (VD: DW@123456)'"/>
            <small class="hint">
              {{ id
                  ? 'Điền để đặt lại mật khẩu khi cần. Dùng nút Gửi email tài khoản để gửi thủ công.'
                  : 'Nếu để trống, hệ thống tự sinh mật khẩu tạm. Email không gửi tự động nữa.' }}
            </small>
          </div>

          <!-- Ghi chú -->
          <div class="field" style="grid-column:1/-1">
            <label>Ghi chú nội bộ</label>
            <textarea v-model="form.note" placeholder="VD: Nhân viên ca sáng, phụ trách khu vực bán hàng 1..."></textarea>
            <small class="hint">Ghi chú chỉ admin thấy, không hiển thị cho nhân viên</small>
          </div>

        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
input, select, textarea {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d8dee9;
  background-color: #fff;
  font-size: 14px;
  transition: border-color 0.15s;
}
select { padding-right: 34px; }

input:focus, select:focus, textarea:focus {
  outline: none;
  border-color: #4f46e5;
}

textarea { min-height: 90px; resize: vertical; }

.auto-code-input {
  background: #f1f5f9;
  color: #64748b;
  border-color: #cbd5e1;
  font-weight: 600;
}

.btn.danger {
  background: #fff1f2;
  border-color: #fecdd3;
  color: #b91c1c;
}

.btn.danger:hover {
  background: #ffe4e6;
}

/* Required star */
.req { color: #e53e3e; margin-left: 3px; }

/* Hint text */
.hint {
  display: block;
  margin-top: 4px;
  font-size: 11.5px;
  color: #94a3b8;
  line-height: 1.4;
}

/* Validation error */
.has-error input, .has-error select, .has-error textarea {
  border-color: #e53e3e;
}
.err-msg {
  display: block;
  margin-top: 4px;
  font-size: 11.5px;
  color: #e53e3e;
  font-weight: 500;
}

@media (max-width: 768px) {
  .head { flex-direction: column; align-items: flex-start; gap: 12px; }
}
</style>