<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { useToast } from '../../composables/useToast'
import taiKhoanService from '../../services/taiKhoanService'
import { getAllKhachHang } from '../../services/KhachHangService'
import { resolveApiOrigin } from '../../utils/apiOrigin'
import logo from '../../assets/img/logo/new logo.png?url'

const router = useRouter()
const toast = useToast()

const identity = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)

const LOCAL_AUTH_USERS_KEY = 'localAuthUsers'

const normalizeEmail = (value) => String(value || '').trim().toLowerCase()
const normalizePhone = (value) => String(value || '').replace(/\D+/g, '')

const loadLocalAuthUsers = () => {
  try {
    const raw = localStorage.getItem(LOCAL_AUTH_USERS_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

const saveLocalAuthUsers = (value) => {
  try {
    localStorage.setItem(LOCAL_AUTH_USERS_KEY, JSON.stringify(value || {}))
  } catch {
    // Ignore storage errors.
  }
}

const extractAccounts = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const resolveEmailByIdentity = async (identityValue) => {
  const value = String(identityValue || '').trim()
  if (!value) return ''
  if (value.includes('@')) return normalizeEmail(value)

  const phone = normalizePhone(value)
  if (!phone) return ''

  try {
    const customerRes = await getAllKhachHang(0, 200)
    const customers = Array.isArray(customerRes?.data)
      ? customerRes.data
      : (Array.isArray(customerRes?.data?.content) ? customerRes.data.content : [])

    const matched = customers.find((customer) => normalizePhone(customer?.soDienThoai) === phone)
    return normalizeEmail(matched?.taiKhoan?.email || '')
  } catch {
    return ''
  }
}

const submit = async () => {
  const rawIdentity = String(identity.value || '').trim()
  if (!rawIdentity) {
    toast.warning('Vui lòng nhập email hoặc số điện thoại')
    return
  }

  if (String(newPassword.value || '').length < 6) {
    toast.warning('Mật khẩu mới cần ít nhất 6 ký tự')
    return
  }

  if (newPassword.value !== confirmPassword.value) {
    toast.warning('Mật khẩu xác nhận không khớp')
    return
  }

  loading.value = true
  try {
    const resolvedEmail = await resolveEmailByIdentity(rawIdentity)
    const targetEmail = normalizeEmail(resolvedEmail || rawIdentity)

    const allAccountsRes = await taiKhoanService.getAll({ page: 0, size: 500 })
    const accounts = extractAccounts(allAccountsRes?.data)
    const matchedAccount = accounts.find((account) => normalizeEmail(account?.email) === targetEmail)

    if (!matchedAccount?.id) {
      toast.error('Không tìm thấy tài khoản tương ứng')
      return
    }

    let backendUpdated = false
    try {
      await axios.put(`${resolveApiOrigin()}/api/tai-khoan/${matchedAccount.id}/dat-lai-mat-khau`, {
        newPassword: newPassword.value,
      })
      backendUpdated = true
    } catch {
      try {
        await taiKhoanService.update(matchedAccount.id, {
          ...matchedAccount,
          matKhau: newPassword.value,
          password: newPassword.value,
        })
        backendUpdated = true
      } catch {
        backendUpdated = false
      }
    }

    const localUsers = loadLocalAuthUsers()
    localUsers[targetEmail] = newPassword.value
    saveLocalAuthUsers(localUsers)

    if (backendUpdated) {
      toast.success('Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.')
    } else {
      toast.info('Đã lưu mật khẩu cục bộ cho môi trường dev. Nếu backend chặn cập nhật, vui lòng liên hệ quản trị viên.')
    }

    router.push('/auth/customer-login')
  } catch {
    toast.error('Không thể xử lý yêu cầu quên mật khẩu lúc này')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth forgot-page">
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

    <div class="card auth-card">
      <div class="head">
        <div>
          <h1>Quên mật khẩu</h1>
          <small class="muted">Nhập thông tin để đặt lại mật khẩu</small>
        </div>
      </div>

      <div class="body">
        <form class="grid" @submit.prevent="submit">
          <div class="field">
            <label>Email hoặc SĐT</label>
            <input v-model="identity" type="text" placeholder="vd: a@gmail.com / 0909xxxxxx" required />
          </div>

          <div class="field">
            <label>Mật khẩu mới</label>
            <input v-model="newPassword" type="password" placeholder="Nhập mật khẩu mới" minlength="6" required />
          </div>

          <div class="field">
            <label>Xác nhận mật khẩu mới</label>
            <input v-model="confirmPassword" type="password" placeholder="Nhập lại mật khẩu mới" minlength="6" required />
          </div>

          <button class="btn primary" type="submit" :disabled="loading">
            {{ loading ? 'Đang xử lý...' : 'Đặt lại mật khẩu' }}
          </button>

          <div class="footnote">
            <small class="muted">Đã nhớ mật khẩu?</small>
            <button type="button" class="link link-nav" @click="router.push('/auth/customer-login')">Đăng nhập</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import './login.css';

.forgot-page {
  position: relative;
  overflow: hidden;
  min-height: 100dvh;
  transition: opacity 0.2s ease, transform 0.2s ease;
  background:
    linear-gradient(0deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.16)),
    linear-gradient(145deg, rgba(60, 0, 0, 0.26) 0%, rgba(60, 0, 0, 0.1) 26%, transparent 26%, transparent 48%, rgba(60, 0, 0, 0.14) 48%, rgba(60, 0, 0, 0.08) 68%, transparent 68%),
    radial-gradient(circle at 50% 45%, rgba(255, 0, 0, 0.16) 0%, rgba(255, 0, 0, 0) 44%),
    linear-gradient(145deg, #5a0000 0%, #a80000 22%, #d30000 52%, #930000 78%, #4b0000 100%);
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

h1 {
  font-size: 26px;
}

label {
  font-size: 16px;
  font-weight: 600;
}

.field input {
  font-size: 16px;
  padding: 14px 18px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.15);
  background: rgba(248, 250, 252, 0.92);
  color: #111;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.field input:focus {
  outline: none;
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.14);
  transform: translateY(-1px);
}

.muted {
  font-size: 15px;
  color: rgba(0, 0, 0, 0.6);
}

.link {
  font-size: 15px;
  font-weight: 600;
  color: #c5162d;
  text-decoration: none;
}

.link-nav {
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
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

@media (max-width: 640px) {
  .forgot-page {
    padding: 12px 8px;
  }

  .forgot-page .auth-card {
    width: 100%;
    padding: 16px 12px 20px;
    border-radius: 18px;
  }

  .brand-wordmark {
    font-size: 38px;
  }

  h1 {
    font-size: 23px;
  }

  .field input {
    font-size: 15px;
    padding: 12px 14px;
  }
}
</style>
