<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import taiKhoanService from '../../services/taiKhoanService'
import { getAllNhanVien } from '../../services/nhanVienService'
import { useToast } from '../../composables/useToast'
import { dispatchAuthContextChanged } from '../../utils/authContext'
import logo from '../../assets/img/logo/new logo.png?url'
import { resolveApiOrigin } from '../../utils/apiOrigin'

const router = useRouter()
const { warning: toastWarning, error: toastError } = useToast()

const identity = ref('')
const password = ref('')
const submitting = ref(false)
const error = ref('')

const clearAuth = () => {
  localStorage.removeItem('role')
  localStorage.removeItem('userId')
  localStorage.removeItem('userEmail')
  dispatchAuthContextChanged()
}

const normalizeRole = (role) => {
  const normalized = String(role || '').trim().toUpperCase().replace(/^ROLE_/, '')
  if (normalized === 'EMPLOYEE' || normalized === 'NHANVIEN' || normalized === 'NHAN_VIEN') return 'NHAN_VIEN'
  if (normalized === 'ADMIN') return 'ADMIN'
  if (normalized === 'CUSTOMER' || normalized === 'KHACHHANG' || normalized === 'KHACH_HANG' || normalized === 'USER') return 'KHACH_HANG'
  return normalized
}

const extractAccounts = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const resolveInternalUsername = async () => {
  const rawIdentity = String(identity.value || '').trim()
  const normalizedIdentity = rawIdentity.toLowerCase()

  if (!rawIdentity) return ''
  if (normalizedIdentity.includes('@')) return normalizedIdentity

  try {
    const response = await getAllNhanVien()
    const employees = Array.isArray(response?.data) ? response.data : []
    const matched = employees.find((item) => String(item?.soDienThoai || '').trim() === rawIdentity)
    const employeeEmail = String(matched?.taiKhoan?.email || matched?.email || '').trim().toLowerCase()
    return employeeEmail || normalizedIdentity
  } catch {
    return normalizedIdentity
  }
}

const syncUserId = async (email, role, accountIdFromResponse) => {
  if (accountIdFromResponse) {
    localStorage.setItem('userId', String(accountIdFromResponse))
    return
  }

  try {
    const allAccounts = await taiKhoanService.getAll({ page: 0, size: 500 })
    const accounts = extractAccounts(allAccounts?.data)
    const matched = accounts.find((item) => {
      const accountEmail = String(item?.email || '').trim().toLowerCase()
      const accountRole = normalizeRole(item?.vaiTro)
      return accountEmail === email && accountRole === role
    })

    if (matched?.id) {
      localStorage.setItem('userId', String(matched.id))
    }
  } catch {
    // Không chặn đăng nhập nếu không đồng bộ được userId.
  }
}

const routeByRole = (role) => {
  if (role === 'ADMIN') {
    router.push('/admin/trang-chu')
    return
  }

  if (role === 'NHAN_VIEN') {
    router.push('/employee/trang-chu')
    return
  }

  clearAuth()
  error.value = 'Không có quyền truy cập'
  toastError(error.value)
}

const login = async () => {
  error.value = ''

  if (!identity.value.trim()) {
    error.value = 'Vui lòng nhập tài khoản'
    toastWarning(error.value)
    return
  }

  if (!password.value) {
    error.value = 'Vui lòng nhập mật khẩu'
    toastWarning(error.value)
    return
  }

  submitting.value = true

  try {
    const username = await resolveInternalUsername()

    const authPayload = {
      username,
      password: password.value,
      email: username,
      matKhau: password.value,
      tenDangNhap: username
    }

    const authApiUrl = `${resolveApiOrigin()}/api/auth/login`
    const response = await axios.post(authApiUrl, authPayload, {
      withCredentials: true,
      headers: {
        'Content-Type': 'application/json'
      }
    })

    const normalizedRole = normalizeRole(response?.data?.role || response?.data?.vaiTro || response?.data?.authority)

    if (!['ADMIN', 'NHAN_VIEN'].includes(normalizedRole)) {
      clearAuth()
      error.value = 'Không có quyền truy cập'
      toastError(error.value)
      return
    }

    const normalizedEmail = String(username || identity.value || '').trim().toLowerCase()
    localStorage.setItem('role', normalizedRole)
    localStorage.setItem('userEmail', normalizedEmail)

    await syncUserId(normalizedEmail, normalizedRole, response?.data?.id || response?.data?.userId || response?.data?.taiKhoanId)

    dispatchAuthContextChanged()
    routeByRole(normalizedRole)
  } catch (e) {
    clearAuth()
    const status = Number(e?.response?.status || 0)
    const isNetwork = !e?.response
    const isServer = status >= 500
    error.value = (isNetwork || isServer)
      ? 'Không thể kết nối máy chủ. Vui lòng thử lại sau.'
      : 'Tài khoản hoặc mật khẩu không đúng'
    toastError(error.value)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-manager">
    <div class="bg-orb bg-orb-left" aria-hidden="true"></div>
    <div class="bg-orb bg-orb-right" aria-hidden="true"></div>

    <router-link to="/trang-chu" class="auth-brand" aria-label="Về trang chủ">
      <span class="brand-wordmark">
        <img :src="logo" alt="D" class="brand-d-icon" />
        <span class="brand-rest">irtyWave</span>
      </span>
    </router-link>

    <div class="card login-manager-card">
      <div class="head">
        <div>
          <h1>Đăng nhập nội bộ</h1>
          <small class="muted">Chỉ dành cho quản trị viên và nhân viên</small>
        </div>

        <!-- <span class="security-chip">Staff Portal</span> -->
      </div>

      <form class="body form-grid" @submit.prevent="login">
        <div class="field">
          <label>Tài khoản</label>
          <input
            v-model="identity"
            type="text"
            placeholder="Email hoặc số điện thoại"
            autocomplete="username"
          />
        </div>

        <div class="field">
          <label>Mật khẩu</label>
          <input
            v-model="password"
            type="password"
            placeholder="Nhập mật khẩu"
            autocomplete="current-password"
          />
        </div>

        <button class="btn primary" type="submit" :disabled="submitting">
          {{ submitting ? 'Đang đăng nhập...' : 'Đăng nhập' }}
        </button>

        <p v-if="error" class="error-text">{{ error }}</p>

        <div class="back-link-row">
          <router-link to="/auth/customer-login" class="back-customer-link">
            ← Đăng nhập với tư cách khách hàng
          </router-link>
        </div>

      </form>
    </div>
  </div>
</template>

<style scoped>
@import './login.css';

.login-manager {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px 16px;
  background:
    linear-gradient(0deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.16)),
    linear-gradient(145deg, rgba(60, 0, 0, 0.26) 0%, rgba(60, 0, 0, 0.1) 26%, transparent 26%, transparent 48%, rgba(60, 0, 0, 0.14) 48%, rgba(60, 0, 0, 0.08) 68%, transparent 68%),
    radial-gradient(circle at 50% 45%, rgba(255, 0, 0, 0.16) 0%, rgba(255, 0, 0, 0) 44%),
    linear-gradient(145deg, #5a0000 0%, #a80000 22%, #d30000 52%, #930000 78%, #4b0000 100%);
}

.bg-orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(18px);
  pointer-events: none;
}

.bg-orb-left {
  width: 280px;
  height: 280px;
  left: -110px;
  top: 8%;
  background: rgba(197, 22, 45, 0.18);
}

.bg-orb-right {
  width: 240px;
  height: 240px;
  right: -80px;
  bottom: 12%;
  background: rgba(252, 165, 165, 0.55);
}

.auth-brand {
  margin-bottom: 16px;
  z-index: 1;
}

.login-manager-card {
  width: min(460px, 94vw);
  border: 1px solid rgba(15, 23, 42, 0.1);
  box-shadow: 0 22px 58px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(8px);
  z-index: 1;
}

.form-grid {
  display: grid;
  gap: 16px;
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.security-chip {
  font-size: 12px;
  font-weight: 700;
  color: #9f1239;
  background: #ffe4e6;
  border: 1px solid #fecdd3;
  border-radius: 999px;
  padding: 6px 10px;
}

.login-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 0 20px;
  margin-top: 10px;
}

.switch-pill {
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  border-radius: 999px;
  padding: 8px 10px;
  text-decoration: none;
  color: #334155;
  border: 1px solid #dbe1ea;
  background: #f8fafc;
}

.switch-pill.active {
  border-color: #fda4af;
  color: #9f1239;
  background: #fff1f2;
}

.brand-wordmark {
  display: inline-flex;
  align-items: center;
  font-size: 48px;
  line-height: 1;
}

.brand-d-icon {
  width: 1.4em;
  height: 1.4em;
  margin-right: -0.04em;
  margin-top: -0.15em;
  object-fit: contain;
  filter:
    sepia(1)
    saturate(20)
    hue-rotate(326deg)
    brightness(1.3)
    contrast(1.1);
}

.brand-rest {
  font-size: 1em;
  font-weight: 800;
  letter-spacing: -0.035em;
  color: #ffffff;
}

.field {
  display: grid;
  gap: 8px;
}

label {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

input {
  width: 100%;
  border-radius: 12px;
  border: 1px solid #d1d5db;
  padding: 12px 14px;
  background: #fff;
  color: #111827;
  font-size: 15px;
}

input:focus {
  outline: none;
  border-color: #c5162d;
  box-shadow: 0 0 0 3px rgba(197, 22, 45, 0.14);
}

.error-text {
  margin: 0;
  color: #dc2626;
  font-weight: 600;
  text-align: center;
}

.alt-auth-links {
  display: grid;
  gap: 10px;
  padding-top: 4px;
}

.alt-auth-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.alt-auth-actions .btn {
  white-space: nowrap;
  font-size: 13px;
  padding: 10px 12px;
  justify-content: center;
}

@media (max-width: 540px) {
  .alt-auth-actions {
    grid-template-columns: 1fr;
  }
}

.back-link-row {
  display: flex;
  justify-content: center;
  padding-top: 4px;
}

.back-customer-link {
  font-size: 14px;
  font-weight: 600;
  color: #c5162d;
  text-decoration: none;
  opacity: 0.85;
  transition: opacity 0.2s;
}

.back-customer-link:hover {
  opacity: 1;
  text-decoration: none;
}
</style>


