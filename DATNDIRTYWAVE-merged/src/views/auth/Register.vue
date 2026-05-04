<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '../../composables/useToast'
import taiKhoanService from '../../services/taiKhoanService'
import { createKhachHang, getKhachHangByTaiKhoanId, listAllKhachHang } from '../../services/KhachHangService'
import { createDiaChi } from '../../services/diaChiService'
import logo from '../../assets/img/logo/new logo.png?url'

const router = useRouter()
const { success: toastSuccess, error: toastError, warning: toastWarning } = useToast()

const form = ref({
  fullName: '',
  email: '',
  phone: '',
  gender: '',
  birthDate: '',
  tinhThanh: '',
  quanHuyen: '',
  phuongXa: '',
  diaChiCuThe: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)
const error = ref('')
const success = ref('')
const isRouteTransitioning = ref(false)

const LOCAL_AUTH_USERS_KEY = 'localAuthUsers'
const LOCAL_REGISTERED_PROFILES_KEY = 'localRegisteredProfiles'
const PHONE_REGEX = /^(0|\+84)\d{9,10}$/
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const normalizePhone = (value = '') => String(value || '').replace(/\s+/g, '').trim()

const persistLocalRegisteredUser = (email, rawPassword) => {
  try {
    const existing = JSON.parse(localStorage.getItem(LOCAL_AUTH_USERS_KEY) || '{}')
    const safeObject = existing && typeof existing === 'object' ? existing : {}
    safeObject[String(email || '').trim().toLowerCase()] = String(rawPassword || '')
    localStorage.setItem(LOCAL_AUTH_USERS_KEY, JSON.stringify(safeObject))
  } catch {
    // Ignore local storage issues; registration result should still be shown.
  }
}

const persistLocalRegisteredProfile = (email) => {
  try {
    const existing = JSON.parse(localStorage.getItem(LOCAL_REGISTERED_PROFILES_KEY) || '{}')
    const safeObject = existing && typeof existing === 'object' ? existing : {}
    const normalizedEmail = String(email || '').trim().toLowerCase()
    const profile = {
      fullName: String(form.value.fullName || '').trim(),
      phone: String(form.value.phone || '').trim(),
      gender: normalizeGender(form.value.gender),
      birthDate: String(form.value.birthDate || '').trim(),
      diaChiCuThe: String(form.value.diaChiCuThe || '').trim(),
      tinhThanh: String(form.value.tinhThanh || '').trim(),
      quanHuyen: String(form.value.quanHuyen || '').trim(),
      phuongXa: String(form.value.phuongXa || '').trim()
    }

    safeObject[normalizedEmail] = profile
    localStorage.setItem(LOCAL_REGISTERED_PROFILES_KEY, JSON.stringify(safeObject))
    localStorage.setItem('userName', profile.fullName)
    localStorage.setItem('userPhone', profile.phone)
    localStorage.setItem('userDiaChiCuThe', profile.diaChiCuThe)
    localStorage.setItem('userTinhThanh', profile.tinhThanh)
    localStorage.setItem('userQuanHuyen', profile.quanHuyen)
    localStorage.setItem('userPhuongXa', profile.phuongXa)
    localStorage.setItem('userGender', profile.gender)
    localStorage.setItem('userBirthDate', profile.birthDate)
    localStorage.setItem('userAddress', [profile.diaChiCuThe, profile.phuongXa, profile.quanHuyen, profile.tinhThanh].filter(Boolean).join(', '))
  } catch {
    // Ignore storage issues; backend persistence remains primary.
  }
}

const normalizeGender = (value) => {
  const normalized = String(value || '').trim().toLowerCase()
  if (normalized === 'nam' || normalized === 'male') return 'Nam'
  if (normalized === 'nữ' || normalized === 'nu' || normalized === 'female') return 'Nữ'
  return 'Khác'
}

const fetchAccountByEmail = async (email) => {
  const normalized = String(email || '').trim().toLowerCase()
  if (!normalized) return null

  const pageSize = 50
  let page = 0
  let totalPages = 1

  while (page < totalPages) {
    const response = await taiKhoanService.getAll({ page, size: pageSize })
    const payload = response?.data
    const accounts = Array.isArray(payload)
      ? payload
      : (Array.isArray(payload?.content) ? payload.content : [])

    const matched = accounts.find((item) => String(item?.email || '').trim().toLowerCase() === normalized) || null
    if (matched) return matched

    const detectedTotalPages = Number(payload?.totalPages)
    if (Number.isFinite(detectedTotalPages) && detectedTotalPages > 0) {
      totalPages = detectedTotalPages
    } else if (accounts.length < pageSize) {
      break
    } else {
      totalPages = page + 2
    }

    page += 1
  }

  return null
}

const fetchCustomerByPhone = async (phone) => {
  const normalizedPhone = normalizePhone(phone)
  if (!normalizedPhone) return null

  const customers = await listAllKhachHang(100)
  return customers.find((item) => normalizePhone(item?.soDienThoai) === normalizedPhone) || null
}

const extractApiErrorMessage = (err) => {
  const data = err?.response?.data
  if (typeof data === 'string' && data.trim()) return data.trim()
  if (typeof data?.message === 'string' && data.message.trim()) return data.message.trim()
  if (typeof data?.error === 'string' && data.error.trim()) return data.error.trim()
  if (Array.isArray(data?.errors) && data.errors.length) {
    const first = data.errors.find((item) => typeof item === 'string' && item.trim())
    if (first) return first.trim()
  }
  if (data?.errors && typeof data.errors === 'object') {
    const firstEntry = Object.values(data.errors)
      .flatMap((value) => (Array.isArray(value) ? value : [value]))
      .find((value) => typeof value === 'string' && value.trim())
    if (firstEntry) return String(firstEntry).trim()
  }
  return ''
}

const isDuplicateEmailError = (message = '') => {
  const normalized = String(message || '').trim().toLowerCase()
  return normalized.includes('email') && (
    normalized.includes('tồn tại') ||
    normalized.includes('ton tai') ||
    normalized.includes('đã có') ||
    normalized.includes('da co') ||
    normalized.includes('already') ||
    normalized.includes('duplicate')
  )
}

const isDuplicatePhoneError = (message = '') => {
  const normalized = String(message || '').trim().toLowerCase()
  return (normalized.includes('so dien thoai') || normalized.includes('số điện thoại') || normalized.includes('phone')) && (
    normalized.includes('tồn tại') ||
    normalized.includes('ton tai') ||
    normalized.includes('đã có') ||
    normalized.includes('da co') ||
    normalized.includes('already') ||
    normalized.includes('duplicate')
  )
}

const isFutureDate = (dateString) => {
  if (!dateString) return false
  const selectedDate = new Date(dateString)
  if (Number.isNaN(selectedDate.getTime())) return false

  const today = new Date()
  selectedDate.setHours(0, 0, 0, 0)
  today.setHours(0, 0, 0, 0)
  return selectedDate > today
}

const createCustomerProfile = async (accountId) => {
  const payloadCandidates = [
    {
      tenKhachHang: form.value.fullName.trim(),
      soDienThoai: form.value.phone.trim(),
      gioiTinh: normalizeGender(form.value.gender),
      ngaySinh: form.value.birthDate,
      idTaiKhoan: Number(accountId),
      trangThai: 'Hoạt động'
    },
    {
      tenKhachHang: form.value.fullName.trim(),
      soDienThoai: form.value.phone.trim(),
      gioiTinh: normalizeGender(form.value.gender),
      ngaySinh: form.value.birthDate,
      taiKhoan: { id: Number(accountId) },
      trangThai: 'Hoạt động'
    },
    {
      tenKhachHang: form.value.fullName.trim(),
      soDienThoai: form.value.phone.trim(),
      gioiTinh: normalizeGender(form.value.gender),
      ngaySinh: form.value.birthDate,
      taiKhoanId: Number(accountId),
      trangThai: 'Hoạt động'
    }
  ]

  let lastError = null
  for (const payload of payloadCandidates) {
    try {
      await createKhachHang(payload)
      const customerResponse = await getKhachHangByTaiKhoanId(accountId)
      if (customerResponse?.data?.id) return customerResponse.data
    } catch (err) {
      lastError = err
    }
  }

  throw lastError || new Error('Create customer failed')
}

const createCustomerAddress = async (customerId) => {
  const payloadCandidates = [
    {
      idKhachHang: Number(customerId),
      tinhThanh: form.value.tinhThanh.trim(),
      quanHuyen: form.value.quanHuyen.trim(),
      phuongXa: form.value.phuongXa.trim(),
      diaChiCuThe: form.value.diaChiCuThe.trim(),
      trangThai: 'Hoạt động'
    },
    {
      khachHang: { id: Number(customerId) },
      tinhThanh: form.value.tinhThanh.trim(),
      quanHuyen: form.value.quanHuyen.trim(),
      phuongXa: form.value.phuongXa.trim(),
      diaChiCuThe: form.value.diaChiCuThe.trim(),
      trangThai: 'Hoạt động'
    },
    {
      khachHangId: Number(customerId),
      tinhThanh: form.value.tinhThanh.trim(),
      quanHuyen: form.value.quanHuyen.trim(),
      phuongXa: form.value.phuongXa.trim(),
      diaChiCuThe: form.value.diaChiCuThe.trim(),
      trangThai: 'Hoạt động'
    }
  ]

  let lastError = null
  for (const payload of payloadCandidates) {
    try {
      await createDiaChi(payload)
      return
    } catch (err) {
      lastError = err
    }
  }

  throw lastError || new Error('Create address failed')
}

const validate = () => {
  error.value = ''
  success.value = ''

  const fail = (message) => {
    error.value = message
    toastWarning(message)
    return false
  }

  if (!form.value.fullName.trim()) {
    return fail('Vui lòng nhập họ tên.')
  }

  if (!EMAIL_REGEX.test(String(form.value.email || '').trim())) {
    return fail('Email không hợp lệ.')
  }

  if (!PHONE_REGEX.test(normalizePhone(form.value.phone))) {
    return fail('Số điện thoại không hợp lệ.')
  }

  if (!form.value.gender) {
    return fail('Vui lòng chọn giới tính.')
  }

  if (!form.value.birthDate) {
    return fail('Vui lòng chọn ngày sinh.')
  }

  if (isFutureDate(form.value.birthDate)) {
    return fail('Ngày sinh không được ở tương lai.')
  }

  if (!form.value.tinhThanh.trim() || !form.value.quanHuyen.trim() || !form.value.phuongXa.trim()) {
    return fail('Địa chỉ cần có đủ Tỉnh/Thành, Quận/Huyện và Phường/Xã.')
  }

  if (!form.value.diaChiCuThe.trim()) {
    return fail('Vui lòng nhập địa chỉ cụ thể.')
  }

  if (form.value.password.length < 6) {
    return fail('Mật khẩu cần ít nhất 6 ký tự.')
  }

  if (form.value.password !== form.value.confirmPassword) {
    return fail('Mật khẩu xác nhận không khớp.')
  }

  return true
}

const submit = async () => {
  if (!validate()) return

  loading.value = true
  error.value = ''
  success.value = ''

  const normalizedEmail = form.value.email.trim().toLowerCase()
  const normalizedPhone = normalizePhone(form.value.phone)
  const basePayload = {
    email: normalizedEmail,
    tenKhachHang: form.value.fullName.trim(),
    soDienThoai: normalizedPhone,
    gioiTinh: normalizeGender(form.value.gender),
    ngaySinh: form.value.birthDate,
    diaChiCuThe: form.value.diaChiCuThe.trim(),
    tinhThanh: form.value.tinhThanh.trim(),
    quanHuyen: form.value.quanHuyen.trim(),
    phuongXa: form.value.phuongXa.trim(),
    trangThaiTaiKhoan: 'Kích hoạt',
    trangThaiHoatDong: 'Hoạt động'
  }

  try {
    const existingAccount = await fetchAccountByEmail(normalizedEmail)
    if (existingAccount?.id) {
      error.value = 'Email đã tồn tại. Vui lòng dùng email khác.'
      toastError(error.value)
      return
    }

    const existingCustomerByPhone = await fetchCustomerByPhone(normalizedPhone)
    if (existingCustomerByPhone?.id) {
      error.value = 'Số điện thoại đã tồn tại. Vui lòng dùng số khác.'
      toastError(error.value)
      return
    }

    let accountId = null

    // Try strict username/password contract first because /api/auth/login accepts this shape.
    const payloadCandidates = [
      { ...basePayload, vaiTro: 'CUSTOMER', username: normalizedEmail, matKhau: form.value.password },
      { ...basePayload, vaiTro: 'ROLE_CUSTOMER', tenDangNhap: normalizedEmail, matKhau: form.value.password },
      { ...basePayload, vaiTro: 'CUSTOMER', username: normalizedEmail, password: form.value.password },
      { ...basePayload, vaiTro: 'ROLE_CUSTOMER', username: normalizedEmail, password: form.value.password },
      { ...basePayload, vaiTro: 'KHACH_HANG', username: normalizedEmail, password: form.value.password },
      { ...basePayload, vaiTro: 'CUSTOMER', tenDangNhap: normalizedEmail, password: form.value.password }
    ]

    let created = false
    let createdAccountId = null
    let lastError = null
    for (const payload of payloadCandidates) {
      try {
        const response = await taiKhoanService.create(payload)
        const responseAccountId = Number(response?.data?.id || response?.data?.data?.id)
        if (Number.isFinite(responseAccountId) && responseAccountId > 0) {
          createdAccountId = responseAccountId
        }
        created = true
        break
      } catch (err) {
        lastError = err
      }
    }

    if (!created) {
      throw lastError || new Error('Create account failed')
    }

    const resolvedAccount = createdAccountId
      ? { id: createdAccountId }
      : await fetchAccountByEmail(normalizedEmail)

    accountId = Number(resolvedAccount?.id) || null

    if (!accountId) {
      throw new Error('Account lookup failed')
    }

    let customer = null
    let profileWarning = ''

    try {
      const existingCustomerResponse = await getKhachHangByTaiKhoanId(accountId)
      customer = existingCustomerResponse?.data || null
    } catch {
      customer = null
    }

    if (!customer?.id) {
      try {
        customer = await createCustomerProfile(accountId)
      } catch {
        profileWarning = 'Tài khoản đã được tạo nhưng chưa thể tạo hồ sơ khách hàng. Bạn vẫn có thể đăng nhập.'
      }
    }

    if (customer?.id) {
      try {
        await createCustomerAddress(customer.id)
      } catch {
        profileWarning = profileWarning || 'Tài khoản đã được tạo, nhưng địa chỉ mặc định chưa lưu được. Bạn có thể cập nhật sau.'
      }
    }

    persistLocalRegisteredUser(normalizedEmail, form.value.password)
    persistLocalRegisteredProfile(normalizedEmail)

    success.value = 'Tạo tài khoản thành công. Đang chuyển sang đăng nhập...'

    if (profileWarning) {
      toastWarning(profileWarning)
    }

    toastSuccess(success.value)
    setTimeout(() => {
      router.push('/auth/customer-login')
    }, 900)
  } catch (e) {
    const apiMessage = extractApiErrorMessage(e)

    if (isDuplicateEmailError(apiMessage)) {
      error.value = 'Email đã tồn tại. Vui lòng dùng email khác.'
    } else if (isDuplicatePhoneError(apiMessage)) {
      error.value = 'Số điện thoại đã tồn tại. Vui lòng dùng số khác.'
    } else if (apiMessage) {
      error.value = apiMessage
    } else {
      const fallbackStatus = Number(e?.response?.status || 0)
      if (fallbackStatus === 400) {
        error.value = 'Thông tin đăng ký chưa hợp lệ. Vui lòng kiểm tra lại các trường bắt buộc.'
      } else if (fallbackStatus === 409) {
        error.value = 'Thông tin tài khoản bị trùng (email hoặc số điện thoại).'
      } else {
        error.value = 'Không thể tạo tài khoản lúc này. Vui lòng thử lại sau.'
      }
    }

    toastError(error.value)
  } finally {
    loading.value = false
  }
}

const navigateWithTransition = (path) => {
  if (!path || isRouteTransitioning.value) return

  const safePush = () => {
    router.push(path).finally(() => {
      isRouteTransitioning.value = false
    })
  }

  const canUseViewTransition = typeof document !== 'undefined' && typeof document.startViewTransition === 'function'
  if (canUseViewTransition) {
    isRouteTransitioning.value = true
    document.startViewTransition(() => safePush())
    return
  }

  isRouteTransitioning.value = true
  window.setTimeout(safePush, 170)
}
</script>

<template>
  <div class="auth register-page" :class="{ 'auth-route-transitioning': isRouteTransitioning }">
    <div class="auth-ambient" aria-hidden="true">
      <span class="orb orb-a"></span>
      <span class="orb orb-b"></span>
    </div>

    <router-link to="/trang-chu" class="auth-brand">
      <div class="brand">
        <span class="brand-wordmark">
          <img :src="logo" alt="D" class="brand-d-icon" />
          <span class="brand-rest">irtyWave</span>
        </span>
      </div>
    </router-link>

    <div class="card auth-card register-card">
      <div class="head">
        <div>
          <h1>Tạo tài khoản</h1>
          <small class="muted">Tạo tài khoản khách hàng để mua sắm</small>
        </div>
      </div>

      <div class="body">
        <form class="grid" @submit.prevent="submit">
          <div class="field col-2">
            <label>Họ và tên</label>
            <input v-model="form.fullName" type="text" placeholder="Nguyễn Văn A" required />
          </div>

          <div class="field">
            <label>Email</label>
            <input v-model="form.email" type="email" placeholder="you@example.com" required />
          </div>

          <div class="field">
            <label>Số điện thoại</label>
            <input v-model="form.phone" type="tel" placeholder="09xxxxxxxx" required />
          </div>

          <div class="field">
            <label>Giới tính</label>
            <select v-model="form.gender" :class="{ 'is-empty': !form.gender }" required>
              <option value="" disabled>Chọn giới tính</option>
              <option value="Nam">Nam</option>
              <option value="Nữ">Nữ</option>
              <option value="Khác">Khác</option>
            </select>
          </div>

          <div class="field">
            <label>Ngày sinh</label>
            <input v-model="form.birthDate" type="date" :max="new Date().toISOString().slice(0, 10)" required />
          </div>

          <div class="field">
            <label>Tỉnh/Thành</label>
            <input v-model="form.tinhThanh" type="text" placeholder="VD: TP. Hà Nội" required />
          </div>

          <div class="field">
            <label>Quận/Huyện</label>
            <input v-model="form.quanHuyen" type="text" placeholder="VD: Cầu Giấy" required />
          </div>

          <div class="field">
            <label>Phường/Xã</label>
            <input v-model="form.phuongXa" type="text" placeholder="VD: Dịch Vọng" required />
          </div>

          <div class="field col-2">
            <label>Địa chỉ cụ thể</label>
            <input v-model="form.diaChiCuThe" type="text" placeholder="Số nhà, tên đường..." required />
          </div>

          <div class="field">
            <label>Mật khẩu</label>
            <input v-model="form.password" type="password" placeholder="Tối thiểu 6 ký tự" required />
          </div>

          <div class="field">
            <label>Xác nhận mật khẩu</label>
            <input v-model="form.confirmPassword" type="password" placeholder="Nhập lại mật khẩu" required />
          </div>

          <button class="btn primary" type="submit" :disabled="loading">
            {{ loading ? 'Đang tạo...' : 'Tạo tài khoản' }}
          </button>

          <div v-if="error" class="error">{{ error }}</div>
          <div v-if="success" class="success">{{ success }}</div>

          <div class="footnote">
            <small class="muted">Đã có tài khoản?</small>
            <button
              type="button"
              class="link register-link"
              :disabled="isRouteTransitioning"
              @click="navigateWithTransition('/auth/customer-login')"
            >
              Đăng nhập
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import './login.css';

.auth {
  position: relative;
  overflow: hidden;
  transition: opacity 0.2s ease, transform 0.2s ease;
  background:
    linear-gradient(0deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.16)),
    linear-gradient(145deg, rgba(60, 0, 0, 0.26) 0%, rgba(60, 0, 0, 0.1) 26%, transparent 26%, transparent 48%, rgba(60, 0, 0, 0.14) 48%, rgba(60, 0, 0, 0.08) 68%, transparent 68%),
    radial-gradient(circle at 50% 45%, rgba(255, 0, 0, 0.16) 0%, rgba(255, 0, 0, 0) 44%),
    linear-gradient(145deg, #5a0000 0%, #a80000 22%, #d30000 52%, #930000 78%, #4b0000 100%);
}

.auth.auth-route-transitioning {
  opacity: 0.84;
  transform: translateY(6px);
}

.auth-ambient {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(28px);
  opacity: 0.34;
  animation: orbFloat 7s ease-in-out infinite;
}

.orb-a {
  width: 240px;
  height: 240px;
  background: #ef4444;
  top: -80px;
  right: -40px;
}

.orb-b {
  width: 220px;
  height: 220px;
  background: #fca5a5;
  bottom: -90px;
  left: -60px;
  animation-delay: 1.2s;
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.pill {
  font-size: 12px;
  font-weight: 700;
  color: #111827;
  background: #e5e7eb;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  padding: 6px 10px;
}

.register-card {
  width: min(720px, 96vw);
  padding: 24px 18px 30px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(10px);
  box-shadow: 0 24px 65px rgba(15, 23, 42, 0.14);
  animation: authCardIn 0.55s ease;
}

.brand-wordmark {
  display: inline-flex;
  align-items: center;
  line-height: 1;
  user-select: none;
  font-size: 52px;
}

.brand-d-icon {
  width: 1.4em;
  height: 1.4em;
  object-fit: contain;
  margin-right: -0.04em;
  margin-top: -0.15em;
  filter:
    sepia(1)
    saturate(20)
    hue-rotate(326deg)
    brightness(1.3)
    contrast(1.1);
  transition: transform 0.25s ease, filter 0.25s ease;
}

.brand:hover .brand-d-icon {
  transform: scale(1.1) rotate(-3deg);
  filter:
    sepia(1)
    saturate(22)
    hue-rotate(326deg)
    brightness(1.4)
    contrast(1.12);
}

.brand-rest {
  font-size: 1em;
  font-weight: 800;
  letter-spacing: -0.035em;
  color: #ffffff;
  transition: color 0.25s ease;
}

.brand:hover .brand-rest {
  color: #ffe4e6;
}

.grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.col-2 {
  grid-column: 1 / -1;
}

.field input,
.field select {
  font-size: 16px;
  padding: 14px 18px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.15);
  background: rgba(248, 250, 252, 0.92);
  color: #111;
  width: 100%;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.field select {
  -webkit-appearance: none;
  -moz-appearance: none;
  appearance: none;
  min-height: 52px;
  line-height: 1.35;
  padding-top: 0;
  padding-bottom: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236b7280' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 14px center;
  background-size: 12px 12px;
  padding-right: 38px;
}

.field select option {
  color: #111827;
}

.field select.is-empty {
  color: #9ca3af;
}

.field select:not(.is-empty) {
  color: #111827;
}

.field input:focus,
.field select:focus {
  outline: none;
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.14);
  transform: translateY(-1px);
}

.btn.primary {
  grid-column: 1 / -1;
}

.footnote {
  grid-column: 1 / -1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  margin-top: 6px;
}

.error {
  grid-column: 1 / -1;
  width: 100%;
  margin-top: 12px;
  color: #dc2626;
  text-align: center;
  font-size: 15px;
}

.success {
  grid-column: 1 / -1;
  width: 100%;
  margin-top: 12px;
  color: #0f766e;
  text-align: center;
  font-size: 15px;
}

.footnote .muted {
  color: #475569;
  font-size: 15px;
}

.register-link {
  color: #c5162d;
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
  font-weight: 700;
}

.register-link:hover {
  color: #dc2626;
}

.register-link:disabled {
  opacity: 0.55;
  cursor: default;
}

@keyframes authCardIn {
  0% {
    opacity: 0;
    transform: translateY(18px) scale(0.98);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes orbFloat {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-14px);
  }
}

@media (max-width: 760px) {
  .brand-wordmark {
    font-size: 40px;
  }

  .grid {
    grid-template-columns: 1fr;
  }

  .col-2 {
    grid-column: auto;
  }
}
</style>

