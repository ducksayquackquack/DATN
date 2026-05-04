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
import { resolveApiOrigin } from "../../../utils/apiOrigin"

const router = useRouter()
const route = useRoute()
const id = route.params.id
const originalTaiKhoanEmail = ref("")
const sendingMail = ref(false)
const deletingEmployee = ref(false)
const verifyingAccountLogin = ref(false)
const quickResettingPassword = ref(false)
const currentAccountPassword = ref("")
const showCurrentPassword = ref(false)
const accountPasswordUnavailable = ref(false)
const EMPLOYEE_LAST_KNOWN_PASSWORDS_KEY = "employeeLastKnownPasswords"

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

const buildNhanVienPayload = (taiKhoanId = form.taiKhoanId) => ({
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
  taiKhoan: taiKhoanId ? { id: Number(taiKhoanId) } : null
})

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

const buildPasswordCandidates = (explicitCurrent = "") => {
  const candidates = [
    String(explicitCurrent || "").trim(),
    String(currentAccountPassword.value || "").trim(),
    getLastKnownPassword(form.taiKhoanId, form.email)
  ].filter(Boolean)

  return Array.from(new Set(candidates))
}

const updateAccountPasswordCompat = async (taiKhoanId, nextPassword, currentPassword = "") => {
  const accountId = Number(taiKhoanId || 0)
  const safeNextPassword = String(nextPassword || "").trim()
  if (!accountId || !safeNextPassword) return

  const fallbackCurrentPasswords = buildPasswordCandidates(currentPassword)
  const attempts = [
    () => taiKhoanService.update(accountId, {
      matKhau: safeNextPassword,
      password: safeNextPassword,
      newPassword: safeNextPassword,
      currentPassword: fallbackCurrentPasswords[0] || undefined,
    }),
    ...fallbackCurrentPasswords.map((currentValue) => () => taiKhoanService.updatePassword(accountId, currentValue, safeNextPassword)),
    () => taiKhoanService.update(accountId, {
      newPassword: safeNextPassword,
      matKhau: safeNextPassword,
      password: safeNextPassword,
    })
  ]

  let lastError = null
  for (const execute of attempts) {
    try {
      await execute()
      return
    } catch (error) {
      lastError = error
    }
  }

  throw lastError || new Error("Không cập nhật được mật khẩu tài khoản")
}

const loadLastKnownPasswords = () => {
  try {
    const raw = localStorage.getItem(EMPLOYEE_LAST_KNOWN_PASSWORDS_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === "object" ? parsed : {}
  } catch {
    return {}
  }
}

const saveLastKnownPasswords = (mapObj) => {
  try {
    localStorage.setItem(EMPLOYEE_LAST_KNOWN_PASSWORDS_KEY, JSON.stringify(mapObj || {}))
  } catch {
    // Ignore storage failures to avoid blocking employee operations.
  }
}

const buildPasswordStoreKey = (taiKhoanId, email = "") => {
  const idPart = Number(taiKhoanId || 0)
  if (idPart > 0) return `id:${idPart}`
  const normalizedEmail = normalizeEmail(email)
  if (normalizedEmail) return `email:${normalizedEmail}`
  return ""
}

const setLastKnownPassword = (taiKhoanId, email, password) => {
  const safePassword = String(password || "").trim()
  if (!safePassword) return

  const store = loadLastKnownPasswords()
  const keyById = buildPasswordStoreKey(taiKhoanId)
  const keyByEmail = buildPasswordStoreKey(null, email)
  const payload = {
    password: safePassword,
    updatedAt: Date.now()
  }

  if (keyById) store[keyById] = payload
  if (keyByEmail) store[keyByEmail] = payload
  saveLastKnownPasswords(store)
}

const getLastKnownPassword = (taiKhoanId, email = "") => {
  const store = loadLastKnownPasswords()
  const keyById = buildPasswordStoreKey(taiKhoanId)
  const keyByEmail = buildPasswordStoreKey(null, email)
  const byId = keyById ? store[keyById]?.password : ""
  const byEmail = keyByEmail ? store[keyByEmail]?.password : ""
  return String(byId || byEmail || "").trim()
}

const extractPasswordFromAccount = (account) => {
  const raw = String(account?.matKhau || account?.password || "").trim()
  if (!raw) return ""

  // Ignore masked/hash values from backend because they are not usable login passwords.
  const masked = /^([*•.]){6,}$/.test(raw)
  const hashed = raw.startsWith("$2a$") || raw.startsWith("$2b$") || raw.startsWith("$2y$")
  if (masked || hashed) return ""

  return raw
}

const syncCurrentPasswordFromAccount = async (taiKhoanId, fallbackAccount = null, fallbackEmail = "") => {
  const parsedId = Number(taiKhoanId || 0)
  if (!parsedId) {
    const localPassword = getLastKnownPassword(0, fallbackEmail || form.email)
    currentAccountPassword.value = localPassword
    accountPasswordUnavailable.value = !localPassword
    return
  }

  const fromFallback = extractPasswordFromAccount(fallbackAccount)
  if (fromFallback) {
    currentAccountPassword.value = fromFallback
    accountPasswordUnavailable.value = false
    setLastKnownPassword(parsedId, fallbackAccount?.email || fallbackEmail || form.email, fromFallback)
    return
  }

  try {
    const detailRes = await taiKhoanService.getById(parsedId)
    const detail = detailRes?.data
    const password = extractPasswordFromAccount(detail)
    const localPassword = getLastKnownPassword(parsedId, detail?.email || fallbackEmail || form.email)
    const resolvedPassword = password || localPassword

    currentAccountPassword.value = resolvedPassword
    accountPasswordUnavailable.value = !resolvedPassword

    if (password) {
      setLastKnownPassword(parsedId, detail?.email || fallbackEmail || form.email, password)
    }
  } catch {
    const localPassword = getLastKnownPassword(parsedId, fallbackEmail || fallbackAccount?.email || form.email)
    currentAccountPassword.value = localPassword
    accountPasswordUnavailable.value = !localPassword
  }
}

const notifyMailSenderFailure = (email, password, reason = "") => {
  const reasonText = String(reason || "").trim()
  const fallbackMsg = `SMTP chưa cấu hình. Dùng thủ công: ${email} / ${password}`

  window.toast?.warning?.(
    reasonText
      ? `Gửi email thất bại (${reasonText}). ${fallbackMsg}`
      : `Gửi email thất bại. ${fallbackMsg}`
  )

  if (navigator?.clipboard?.writeText) {
    navigator.clipboard.writeText(`Email: ${email}\nMật khẩu tạm: ${password}`).catch(() => {})
  }
}

const taoTaiKhoanNhanVien = async (email, matKhau, role = form.role) => {
  const normalizedEmail = normalizeEmail(email)
  const backendRole = toBackendRole(role)
  const roleCandidates = backendRole === "ADMIN"
    ? ["ADMIN", "ROLE_ADMIN"]
    : ["EMPLOYEE", "ROLE_EMPLOYEE", "NHAN_VIEN", "ROLE_NHAN_VIEN", "STAFF", "ROLE_STAFF"]

  const payloadCandidates = roleCandidates.flatMap((vaiTro) => ([
    {
      email: normalizedEmail,
      username: normalizedEmail,
      matKhau,
      vaiTro,
      trangThaiHoatDong: "Hoạt động",
      trangThaiTaiKhoan: "Kích hoạt"
    },
    {
      email: normalizedEmail,
      tenDangNhap: normalizedEmail,
      matKhau,
      vaiTro,
      trangThaiHoatDong: "Hoạt động",
      trangThaiTaiKhoan: "Kích hoạt"
    },
    {
      email: normalizedEmail,
      username: normalizedEmail,
      password: matKhau,
      vaiTro,
      trangThaiHoatDong: "Hoạt động",
      trangThaiTaiKhoan: "Kích hoạt"
    }
  ]))

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

  throw lastError || new Error("Không tạo được tài khoản nhân viên")
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
        return { success: true, message: "Đã gửi email tài khoản", endpoint, previewUrl: payload?.previewUrl }
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

const normalizeRole = (role) => String(role || "")
  .normalize("NFD")
  .replace(/[\u0300-\u036f]/g, "")
  .replace(/đ/g, "d")
  .replace(/Đ/g, "D")
  .trim()
  .toUpperCase()
  .replace(/^ROLE_/, "")
  .replace(/\s+/g, "_")
const isEmployeeRole = (role) => {
  const normalized = normalizeRole(role)
  return normalized === "EMPLOYEE" || normalized === "NHAN_VIEN" || normalized === "NHANVIEN" || normalized === "STAFF"
}

async function verifyDisplayedPasswordLogin() {
  const accountEmail = normalizeEmail(form.email)
  const shownPassword = String(currentAccountPassword.value || "").trim()

  if (!accountEmail) {
    window.toast?.warning?.("Nhân viên chưa có email tài khoản để kiểm tra đăng nhập")
    return
  }

  if (!shownPassword) {
    window.toast?.warning?.("Không có mật khẩu hiển thị để kiểm tra")
    return
  }

  verifyingAccountLogin.value = true
  try {
    const authPayload = {
      username: accountEmail,
      email: accountEmail,
      tenDangNhap: accountEmail,
      password: shownPassword,
      matKhau: shownPassword,
    }

    const response = await fetch(`${resolveApiOrigin()}/api/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      credentials: "include",
      body: JSON.stringify(authPayload)
    })

    if (!response.ok) {
      throw new Error(`AUTH_HTTP_${response.status}`)
    }

    const payload = await response.json().catch(() => ({}))
    const loginRole = normalizeRole(payload?.role || payload?.vaiTro || payload?.authority)
    const isAllowed = loginRole === "ADMIN" || isEmployeeRole(loginRole)

    if (!isAllowed) {
      throw new Error("AUTH_ROLE_NOT_ALLOWED")
    }

    window.toast?.success?.("Mật khẩu đang hiển thị đăng nhập được")
  } catch {
    window.toast?.error?.("Mật khẩu đang hiển thị KHÔNG đăng nhập được. Hãy nhập mật khẩu mới rồi bấm Lưu.")
  } finally {
    verifyingAccountLogin.value = false
  }
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

async function resolveReusableAccountByEmailForEmployee(targetEmail) {
  const normalizedEmail = normalizeEmail(targetEmail)
  if (!normalizedEmail) {
    return { canReuse: false, account: null, reason: "EMAIL_EMPTY" }
  }

  const accounts = await listAllTaiKhoan()
  const matched = accounts.find((acc) => normalizeEmail(acc?.email) === normalizedEmail)
  if (!matched?.id) {
    return { canReuse: false, account: null, reason: "NOT_FOUND" }
  }

  const employeeRes = await getAllNhanVien()
  const employees = Array.isArray(employeeRes?.data) ? employeeRes.data : []
  const isLinked = employees.some((emp) => Number(emp?.idTaiKhoan || emp?.taiKhoan?.id || 0) === Number(matched.id))

  if (isLinked) {
    return { canReuse: false, account: matched, reason: "LINKED_EMPLOYEE" }
  }

  const normalizedRole = normalizeRole(matched?.vaiTro)
  if (normalizedRole === "ADMIN") {
    return { canReuse: false, account: matched, reason: "ADMIN_ACCOUNT" }
  }

  return { canReuse: true, account: matched, reason: "OK" }
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

    let ensuredTaiKhoanId = form.taiKhoanId ? Number(form.taiKhoanId) : null

    if (ensuredTaiKhoanId) {
      const normalizedCurrentEmail = String(form.email || "").trim().toLowerCase()
      const emailChanged = normalizedCurrentEmail && normalizedCurrentEmail !== originalTaiKhoanEmail.value

      if (emailChanged) {
        const usedByAnother = await isEmailUsedByAnotherAccount(normalizedCurrentEmail, ensuredTaiKhoanId)
        if (usedByAnother) {
          const reclaimed = await reclaimOrphanEmployeeEmail(normalizedCurrentEmail, ensuredTaiKhoanId)
          if (!reclaimed) {
            errors.email = "Email đã tồn tại trong hệ thống tài khoản"
            window.toast?.error?.("Email đã tồn tại trong hệ thống tài khoản")
            return
          }
        }
      }

      const accountUpdatePayload = {
        vaiTro: toBackendRole(form.role),
        trangThaiHoatDong: form.status === "Ngừng hoạt động" ? "Ngừng hoạt động" : "Hoạt động"
      }

      if (emailChanged) {
        accountUpdatePayload.email = normalizedCurrentEmail
      }

      try {
        await taiKhoanService.update(ensuredTaiKhoanId, accountUpdatePayload)
      } catch (error) {
        const message = extractErrorMessage(error)
        if (emailChanged && isDuplicateEmailError(message)) {
          const reclaimed = await reclaimOrphanEmployeeEmail(normalizedCurrentEmail, ensuredTaiKhoanId)
          if (reclaimed) {
            await taiKhoanService.update(ensuredTaiKhoanId, accountUpdatePayload)
          } else {
            window.toast?.error?.("Email đã tồn tại trong hệ thống tài khoản")
            throw error
          }
        } else {
          throw error
        }
      }

      await updateAccountPasswordCompat(ensuredTaiKhoanId, generatedPassword)

      if (emailChanged) {
        originalTaiKhoanEmail.value = normalizedCurrentEmail
      }
    } else {
      const normalizedCurrentEmail = String(form.email || "").trim().toLowerCase()
      ensuredTaiKhoanId = await taoTaiKhoanNhanVien(normalizedCurrentEmail, generatedPassword, form.role)
      form.taiKhoanId = ensuredTaiKhoanId
      await updateNhanVien(id, buildNhanVienPayload(ensuredTaiKhoanId))
      originalTaiKhoanEmail.value = normalizedCurrentEmail
    }

    const mailResult = await guiMailThongTinTaiKhoan(normalizedEmail, generatedPassword, form.role, form.name.trim())
    if (!mailResult.success) {
      setLastKnownPassword(ensuredTaiKhoanId, normalizedEmail, generatedPassword)
      currentAccountPassword.value = generatedPassword
      accountPasswordUnavailable.value = false
      notifyMailSenderFailure(normalizedEmail, generatedPassword, mailResult.message)
      return
    }

    setLastKnownPassword(ensuredTaiKhoanId, normalizedEmail, generatedPassword)
    currentAccountPassword.value = generatedPassword
    accountPasswordUnavailable.value = false
    form.password = ""
    window.toast?.success?.("Đã gửi email tài khoản cho nhân viên")
    if (mailResult.previewUrl) {
      window.toast?.info?.(`Xem email test tại: ${mailResult.previewUrl}`)
    }
  } catch (err) {
    window.toast?.error?.("Không thể gửi email: " + (err?.response?.data?.message || err?.message || "Lỗi không xác định"))
  } finally {
    sendingMail.value = false
  }
}

async function quickResetPasswordForNV015() {
  if (!id || String(form.code || '').trim().toUpperCase() !== 'NV015') return

  const accountId = Number(form.taiKhoanId || 0)
  if (!accountId) {
    window.toast?.error?.('Không tìm thấy tài khoản liên kết để đặt lại mật khẩu')
    return
  }

  const accountEmail = normalizeEmail(form.email)
  if (!accountEmail) {
    window.toast?.error?.('NV015 chưa có email tài khoản hợp lệ')
    return
  }

  const confirmed = await (window.confirmDialog?.('Đặt lại mật khẩu nhanh cho NV015? Hệ thống sẽ sinh mật khẩu mới và copy vào clipboard.')
    ?? confirm('Đặt lại mật khẩu nhanh cho NV015?'))
  if (!confirmed) return

  quickResettingPassword.value = true
  try {
    const generatedPassword = taoMatKhauTam()
    await updateAccountPasswordCompat(accountId, generatedPassword, currentAccountPassword.value)

    setLastKnownPassword(accountId, accountEmail, generatedPassword)
    currentAccountPassword.value = generatedPassword
    accountPasswordUnavailable.value = false
    showCurrentPassword.value = true
    form.password = ''

    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(`Email: ${accountEmail}\nMật khẩu mới: ${generatedPassword}`)
    }

    window.toast?.success?.('Đã đặt lại mật khẩu nhanh cho NV015 và copy thông tin đăng nhập')
  } catch (error) {
    window.toast?.error?.(error?.response?.data?.message || error?.message || 'Đặt lại mật khẩu nhanh thất bại')
  } finally {
    quickResettingPassword.value = false
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
    const resolvedRole = normalizeRole(data.chucVu?.maChucVu || data.chucVu?.tenChucVu || data.taiKhoan?.vaiTro)
    form.role = resolvedRole === "ADMIN" ? "ADMIN" : "NHAN_VIEN"
    form.taiKhoanId = data.taiKhoan?.id || null
    form.chucVuId = data.chucVu?.id || null
    form.note = data.ghiChu || ""
    form.status = data.trangThaiHoatDong === "Ngừng hoạt động"
      ? "Ngừng hoạt động"
      : "Hoạt động"

    await syncCurrentPasswordFromAccount(form.taiKhoanId, data.taiKhoan, form.email)
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

    const nhanVienPayload = buildNhanVienPayload()

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
          trangThaiHoatDong: form.status === "Ngừng hoạt động" ? "Ngừng hoạt động" : "Hoạt động"
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

        if (form.password) {
          await updateAccountPasswordCompat(form.taiKhoanId, form.password)
          setLastKnownPassword(form.taiKhoanId, form.email, form.password)
          currentAccountPassword.value = form.password
          accountPasswordUnavailable.value = false
        }
      }
      await updateNhanVien(id, nhanVienPayload)
      window.toast?.success?.("Cập nhật nhân viên thành công!")
    } else {
      const reusable = await resolveReusableAccountByEmailForEmployee(form.email.trim())
      let createdTaiKhoanId = null

      if (reusable.canReuse && reusable.account?.id) {
        const normalizedRole = toBackendRole(form.role)
        await taiKhoanService.update(reusable.account.id, {
          vaiTro: normalizedRole,
          trangThaiHoatDong: "Hoạt động",
          trangThaiTaiKhoan: "Kích hoạt"
        })
        await updateAccountPasswordCompat(reusable.account.id, matKhauTam)
        createdTaiKhoanId = Number(reusable.account.id)
      } else if (reusable.reason === "LINKED_EMPLOYEE") {
        errors.email = "Email đã được dùng cho nhân viên khác"
        window.toast?.error?.("Email đã được dùng cho nhân viên khác")
        return
      } else if (reusable.reason === "ADMIN_ACCOUNT") {
        errors.email = "Email đang thuộc tài khoản quản trị viên"
        window.toast?.error?.("Email đang thuộc tài khoản quản trị viên, vui lòng dùng email khác")
        return
      } else {
        createdTaiKhoanId = await taoTaiKhoanNhanVien(form.email.trim(), matKhauTam, form.role)
      }

      await createNhanVien(buildNhanVienPayload(createdTaiKhoanId))
      setLastKnownPassword(createdTaiKhoanId, form.email.trim(), matKhauTam)
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
          <button
            v-if="id && String(form.code || '').trim().toUpperCase() === 'NV015'"
            class="btn"
            :disabled="quickResettingPassword"
            @click="quickResetPasswordForNV015"
          >
            {{ quickResettingPassword ? 'Đang reset...' : 'Reset mật khẩu nhanh NV015' }}
          </button>
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
          </div>

          <!-- Họ tên (bắt buộc) -->
          <div class="field" :class="{ 'has-error': errors.name }">
            <label>Họ tên <span class="req">*</span></label>
            <input v-model="form.name" placeholder="VD: Nguyễn Văn C" @blur="validate" />
            <small v-if="errors.name" class="err-msg">{{ errors.name }}</small>
          </div>

          <!-- Vai trò -->
          <div class="field">
            <label>Vai trò <span class="req">*</span></label>
            <select v-model="form.role">
              <option value="ADMIN">Quản trị viên</option>
              <option value="NHAN_VIEN">Nhân viên bán hàng</option>
            </select>
          </div>

          <!-- Email -->
          <div class="field" :class="{ 'has-error': errors.email }">
            <label>Email {{ !id ? '' : '' }} <span v-if="!id" class="req">*</span></label>
            <input type="email" v-model="form.email"
                   :placeholder="id ? 'Để trống nếu không đổi email' : 'VD: nhanvien@dirtywave.com'"
                   maxlength="100"
                   @blur="validate" />
            <small v-if="errors.email" class="err-msg">{{ errors.email }}</small>
          </div>

          <!-- SĐT (bắt buộc) -->
          <div class="field" :class="{ 'has-error': errors.phone }">
            <label>Số điện thoại <span class="req">*</span></label>
            <input type="tel" v-model="form.phone" placeholder="VD: 0912345678" maxlength="11" @blur="validate" />
            <small v-if="errors.phone" class="err-msg">{{ errors.phone }}</small>
          </div>

          <!-- Trạng thái (chỉ sửa khi đã có nhân viên) -->
          <div class="field" v-if="id">
            <label>Trạng thái hoạt động</label>
            <select v-model="form.status">
              <option>Hoạt động</option>
              <option>Ngừng hoạt động</option>
            </select>
          </div>

          <!-- Mật khẩu -->
          <div v-if="!id" class="field" style="grid-column:1/-1">
            <label>Mật khẩu</label>
            <input type="text" v-model="form.password"
                   placeholder="Để trống để hệ thống tự tạo mật khẩu tạm (VD: DW@123456)"/>
          </div>

          <div v-if="id && !accountPasswordUnavailable" class="field" style="grid-column:1/-1">
            <label>Mật khẩu hiện tại (đã biết)</label>
            <div class="password-preview-row">
              <input
                :type="showCurrentPassword ? 'text' : 'password'"
                :value="currentAccountPassword"
                readonly
              />
              <button
                type="button"
                class="btn"
                @click="showCurrentPassword = !showCurrentPassword"
              >
                {{ showCurrentPassword ? 'Ẩn' : 'Hiện' }}
              </button>
              <button
                type="button"
                class="btn"
                :disabled="verifyingAccountLogin || !currentAccountPassword"
                @click="verifyDisplayedPasswordLogin"
              >
                {{ verifyingAccountLogin ? 'Đang kiểm tra...' : 'Kiểm tra đăng nhập' }}
              </button>
            </div>
            <small class="hint">
              Nhấn "Kiểm tra đăng nhập" để xác thực mật khẩu này có dùng được thật trên hệ thống.
            </small>
          </div>

          <!-- Ghi chú -->
          <div class="field" style="grid-column:1/-1">
            <label>Ghi chú nội bộ</label>
            <textarea v-model="form.note" placeholder="VD: Nhân viên ca sáng, phụ trách khu vực bán hàng 1..."></textarea>
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

.password-preview-row {
  display: flex;
  gap: 10px;
}

.password-preview-row .btn {
  white-space: nowrap;
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