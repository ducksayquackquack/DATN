<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { Eye, EyeOff } from 'lucide-vue-next'
import { useToast } from '../../composables/useToast'
import taiKhoanService from '../../services/taiKhoanService'
import { getAllKhachHang } from '../../services/KhachHangService'
import logo from '../../assets/img/logo/new logo.png?url'

const router = useRouter()
const { error: toastError, warning: toastWarning } = useToast()

const identity = ref('')
const password = ref('')
const error = ref(null)
const showPassword = ref(false)
const remember = ref(false)

const LOCAL_AUTH_USERS_KEY = 'localAuthUsers'
const LOCAL_REGISTERED_PROFILES_KEY = 'localRegisteredProfiles'

const loadLocalAuthUsers = () => {
  try {
    const raw = localStorage.getItem(LOCAL_AUTH_USERS_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

const saveLocalAuthUsers = (users) => {
  try {
    localStorage.setItem(LOCAL_AUTH_USERS_KEY, JSON.stringify(users || {}))
  } catch {
    // Ignore storage failures to avoid blocking login.
  }
}

const loadLocalRegisteredProfiles = () => {
  try {
    const raw = localStorage.getItem(LOCAL_REGISTERED_PROFILES_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

const applyLocalProfileSnapshot = (email) => {
  const normalizedEmail = String(email || '').trim().toLowerCase()
  if (!normalizedEmail) return

  const profiles = loadLocalRegisteredProfiles()
  const profile = profiles[normalizedEmail]
  if (!profile || typeof profile !== 'object') return

  localStorage.setItem('userName', String(profile.fullName || '').trim())
  localStorage.setItem('userPhone', String(profile.phone || '').trim())
  localStorage.setItem('userDiaChiCuThe', String(profile.diaChiCuThe || '').trim())
  localStorage.setItem('userTinhThanh', String(profile.tinhThanh || '').trim())
  localStorage.setItem('userQuanHuyen', String(profile.quanHuyen || '').trim())
  localStorage.setItem('userPhuongXa', String(profile.phuongXa || '').trim())
  localStorage.setItem('userGender', String(profile.gender || '').trim())
  localStorage.setItem('userBirthDate', String(profile.birthDate || '').trim())
  localStorage.setItem('userAddress', [profile.diaChiCuThe, profile.phuongXa, profile.quanHuyen, profile.tinhThanh].filter(Boolean).join(', '))
}

const resolveUsername = async () => {
  const rawIdentity = identity.value.trim()
  const normalizedIdentity = rawIdentity.toLowerCase()

  if (!rawIdentity) return ''
  if (normalizedIdentity.includes('@')) return normalizedIdentity

  try {
    const customerRes = await getAllKhachHang(0, 100)
    const customers = Array.isArray(customerRes?.data)
      ? customerRes.data
      : (Array.isArray(customerRes?.data?.content) ? customerRes.data.content : [])

    const matchedCustomer = customers.find((customer) => {
      return String(customer?.soDienThoai || '').trim() === rawIdentity
    })

    const matchedEmail = String(matchedCustomer?.taiKhoan?.email || '').trim().toLowerCase()
    return matchedEmail || normalizedIdentity
  } catch {
    return normalizedIdentity
  }
}

const togglePassword = () => {
  showPassword.value = !showPassword.value
}

const normalizeRole = (role) => String(role || '').replace('ROLE_', '').trim().toUpperCase()

const extractAccounts = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const routeByRole = (role) => {
  if (role === 'ADMIN') router.push('/admin')
  else if (role === 'EMPLOYEE') router.push('/employee/dashboard')
  else if (role === 'CUSTOMER' || role === 'KHACH_HANG' || role === 'KHACHHANG') router.push('/home')
  else router.push('/')
}

const login = async () => {
  error.value = null

  if (!identity.value.trim()) {
    error.value = 'Vui lòng nhập email hoặc số điện thoại'
    toastWarning(error.value)
    return
  }

  if (!password.value) {
    error.value = 'Vui lòng nhập mật khẩu'
    toastWarning(error.value)
    return
  }

  const username = await resolveUsername()
  const normalizedIdentity = username || identity.value.trim().toLowerCase()
  try {
    const authPayload = {
      username,
      password: password.value,
      email: username,
      matKhau: password.value,
      tenDangNhap: username
    }

    const response = await axios.post(
      'http://localhost:8080/api/auth/login',
      authPayload,
      {
        withCredentials: true,
        headers: {
          'Content-Type': 'application/json'
        }
      }
    )

    const roleFromResponse = response?.data?.role || response?.data?.vaiTro || response?.data?.authority
    const normalizedRole = normalizeRole(roleFromResponse)

    localStorage.setItem('role', normalizedRole)
    localStorage.setItem('userEmail', normalizedIdentity)
    applyLocalProfileSnapshot(normalizedIdentity)

    // Try to keep userId in sync with the login identity so profile pages load correct data.
    const accountIdFromResponse = response?.data?.id || response?.data?.userId || response?.data?.taiKhoanId
    if (accountIdFromResponse) {
      localStorage.setItem('userId', String(accountIdFromResponse))
    } else {
      try {
        const allAccounts = await taiKhoanService.getAll()
        const accounts = Array.isArray(allAccounts?.data)
          ? allAccounts.data
          : (Array.isArray(allAccounts?.data?.content) ? allAccounts.data.content : [])
        const matched = accounts.find((acc) => {
          const email = String(acc?.email || '').trim().toLowerCase()
          const accountRole = String(acc?.vaiTro || '').trim().toUpperCase()
          return email === normalizedIdentity && accountRole === normalizedRole.toUpperCase()
        })
        if (matched?.id) {
          localStorage.setItem('userId', String(matched.id))
        }
      } catch {
        // Keep login successful even if account lookup fails.
      }
    }

    routeByRole(normalizedRole)
  } catch (e) {
    // Fallback path: allow login when auth endpoint format differs but account exists.
    try {
      const allAccounts = await taiKhoanService.getAll({ page: 0, size: 500 })
      const accounts = extractAccounts(allAccounts?.data)
      const localAuthUsers = loadLocalAuthUsers()

      const matched = accounts.find((acc) => {
        const email = String(acc?.email || '').trim().toLowerCase()
        const role = normalizeRole(acc?.vaiTro)
        const rawPassword = String(acc?.matKhau || acc?.password || '')
        const localPassword = String(localAuthUsers[email] || '')
        const isKnownRole = ['ADMIN', 'EMPLOYEE', 'CUSTOMER', 'KHACH_HANG', 'KHACHHANG'].includes(role)

        if (email !== normalizedIdentity || !isKnownRole) return false

        if (rawPassword) {
          return rawPassword === password.value
        }

        // Compatibility mode: some environments return account with null password
        // even after successful registration. Keep this fallback customer-only
        // and require a locally known password.
        const isCustomer = ['CUSTOMER', 'KHACH_HANG', 'KHACHHANG'].includes(role)
        if (!isCustomer) return false

        if (localPassword) {
          return localPassword === password.value
        }

        return false
      })

      if (matched?.id) {
        const role = normalizeRole(matched?.vaiTro)
        const email = String(matched?.email || normalizedIdentity).trim().toLowerCase()

        if (['CUSTOMER', 'KHACH_HANG', 'KHACHHANG'].includes(role)) {
          const localAuthUsers = loadLocalAuthUsers()
          localAuthUsers[email] = password.value
          saveLocalAuthUsers(localAuthUsers)
        }

        localStorage.setItem('userId', String(matched.id))
        localStorage.setItem('userEmail', email)
        localStorage.setItem('role', role)
        applyLocalProfileSnapshot(email)
        routeByRole(role)
        return
      }
    } catch {
      // Keep user-facing error below.
    }

    error.value = 'Email hoặc mật khẩu không đúng'
    toastError(error.value)
  }
}
</script>

<template>
  <div class="auth">
    <div class="auth-ambient" aria-hidden="true">
      <span class="orb orb-a"></span>
      <span class="orb orb-b"></span>
    </div>

    <router-link to="/home" class="auth-brand">
      <div class="brand">
        <span class="brand-wordmark">
          <img :src="logo" alt="D" class="brand-d-icon" />
          <span class="brand-rest">irtyWave</span>
        </span>
      </div>
    </router-link>

    <div class="card auth-card">
      <div class="head">
        <div>
          <h1>Đăng nhập</h1>
          <small class="muted">Nhập thông tin để tiếp tục</small>
        </div>
        <span class="pill">Secure</span>
      </div>

      <div class="body">
        <form class="grid" autocomplete="on" @submit.prevent="login">

          <div class="field">
            <label>Email hoặc SĐT</label>
            <input
              v-model="identity"
              type="text"
              placeholder="vd: a@gmail.com / 0909xxxxxx"
              required
            />
            <small class="muted">
              Anh có thể dùng email hoặc số điện thoại.
            </small>
          </div>

          <div class="field">
            <label>Mật khẩu</label>

            <div class="pw">
              <input
                :type="showPassword ? 'text' : 'password'"
                v-model="password"
                placeholder="••••••••"
                required
              />
              <button
                type="button"
                class="iconbtn"
                @click="togglePassword"
                :aria-label="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
              >
                <EyeOff v-if="showPassword" :size="16" />
                <Eye v-else :size="16" />
              </button>
            </div>

            <div class="row">
              <label class="remember">
                <input type="checkbox" v-model="remember" />
                <span>Ghi nhớ đăng nhập</span>
              </label>

              <a class="link" href="#" @click.prevent>
                Quên mật khẩu?
              </a>
            </div>
          </div>

          <button class="btn primary" type="submit">
            Đăng nhập
          </button>

          <div v-if="error" class="error">
            {{ error }}
          </div>

          <div class="or">
            <span></span>
            <small class="muted">hoặc</small>
            <span></span>
          </div>

          <div class="grid cols2">
            <button class="btn" type="button">
              G • Google
            </button>
            <button class="btn" type="button">
              f • Facebook
            </button>
          </div>

          <div class="footnote">
            <small class="muted">Chưa có tài khoản?</small>
            <router-link to="/register" class="link">
              Tạo tài khoản
            </router-link>
          </div>

        </form>
      </div>
    </div>

    <div class="footer">
      © {{ new Date().getFullYear() }} DirtyWave
    </div>

  </div>
</template>

<style scoped>
@import './login.css';

.auth {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 12% 18%, rgba(239, 68, 68, 0.2), transparent 44%),
    radial-gradient(circle at 88% 82%, rgba(14, 116, 144, 0.16), transparent 42%),
    linear-gradient(145deg, #f8fafc 0%, #eef2ff 45%, #fef2f2 100%);
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
  background: #0ea5e9;
  bottom: -90px;
  left: -60px;
  animation-delay: 1.1s;
}

.auth-card {
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
  width: 1.06em;
  height: 1.06em;
  object-fit: contain;
  margin-right: -0.04em;
  filter:
    sepia(1)
    saturate(14)
    hue-rotate(326deg)
    brightness(0.98)
    contrast(1.16)
    drop-shadow(0 3px 8px rgba(197, 22, 45, 0.22));
  transition: transform 0.25s ease, filter 0.25s ease;
}

.brand:hover .brand-d-icon {
  transform: scale(1.08) rotate(-3deg);
  filter:
    sepia(1)
    saturate(15)
    hue-rotate(326deg)
    brightness(1.03)
    contrast(1.18)
    drop-shadow(0 4px 12px rgba(197, 22, 45, 0.3));
}

.brand-rest {
  font-size: 1em;
  font-weight: 800;
  letter-spacing: -0.035em;
  color: #111827;
  transition: color 0.25s ease;
}

.brand:hover .brand-rest {
  color: #c5162d;
}

h1 {
  font-size: 26px;
}

label {
  font-size: 16px;
  font-weight: 600;
}

.field input,
.pw input {
  font-size: 16px;
  padding: 14px 18px;
  border-radius: 999px;
  border: 1px solid rgba(0,0,0,.15);
  background: rgba(248, 250, 252, 0.92);
  color: #111;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.pw {
  position: relative;
}

.field input:focus,
.pw input:focus {
  outline: none;
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.14);
  transform: translateY(-1px);
}

.pw input {
  padding-right: 50px;
}

.iconbtn {
  position: absolute;
  right: 14px;
  background: none;
  border: none;
  cursor: pointer;
  color: #111;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.remember {
  display: flex;
  gap: 8px;
  align-items: center;
  font-size: 15px;
}

.link {
  font-size: 15px;
  font-weight: 600;
  color: #111;
}

.muted {
  font-size: 15px;
  color: rgba(0,0,0,.6);
}

.btn {
  font-size: 16px;
}

.footer {
  text-align: center;
  font-size: 15px;
  margin-top: 24px;
  color: rgba(0,0,0,.6);
}

.error {
  margin-top: 12px;
  color: #e53935;
  text-align: center;
  font-size: 15px;
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
}
</style>