<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from '@/composables/useToast'
import taiKhoanService from '../../../services/taiKhoanService'
import { getAllNhanVien, getNhanVienByTaiKhoanId, updateNhanVien } from '../../../services/nhanVienService'
import { getVietnameseNameByEmail, isGenericVietnameseName } from '../../../utils/vietnameseNames'
import { dispatchAuthContextChanged, resolveAccountByRole } from '../../../utils/authContext'

const router = useRouter()
const route = useRoute()
const toast = useToast()

const loading = ref(true)
const saving = ref(false)
const error = ref('')
const activeTab = ref(route.query.tab === 'security' ? 'security' : 'profile')

const account = ref(null)
const nhanVien = ref(null)
const avatarInputRef = ref(null)

const profileForm = reactive({
  tenNhanVien: '',
  maNhanVien: '',
  gioiTinh: 'Nam',
  ngaySinh: '',
  soDienThoai: '',
  diaChi: '',
  trangThaiHoatDong: 'Hoạt động',
  email: '',
  avatar: ''
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const getAvatarStorageKey = (accountId) => `avatar:${accountId}`

const extractList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

const normalizeNhanVien = (payload) => {
  if (!payload) return null
  if (Array.isArray(payload)) return payload[0] || null
  if (Array.isArray(payload?.content)) return payload.content[0] || null
  if (payload?.data) return normalizeNhanVien(payload.data)
  if (payload?.id || payload?.tenNhanVien || payload?.idTaiKhoan || payload?.taiKhoan) return payload
  return null
}

const displayName = computed(() => {
  if (profileForm.tenNhanVien.trim()) return profileForm.tenNhanVien.trim()
  const localPart = String(profileForm.email || '').split('@')[0]
  if (!localPart) return 'Quản trị viên'
  return localPart
    .replace(/[._-]+/g, ' ')
    .trim()
    .split(/\s+/)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
})

const avatarFallback = computed(() => {
  const name = String(displayName.value || 'AD').trim()
  const words = name.split(/\s+/).filter(Boolean)
  if (words.length >= 2) return `${words[0][0] || ''}${words[words.length - 1][0] || ''}`.toUpperCase()
  return name.slice(0, 2).toUpperCase()
})

const findNhanVienByTaiKhoanId = async (taiKhoanId) => {
  try {
    const byTaiKhoan = await getNhanVienByTaiKhoanId(taiKhoanId)
    const direct = normalizeNhanVien(byTaiKhoan)
    if (direct) return direct
  } catch {
    // Fallback to scanning all employees.
  }

  const allNhanVien = await getAllNhanVien()
  const employees = extractList(allNhanVien?.data)
  return employees.find((item) => {
    const inlineTaiKhoanId = item?.taiKhoan?.id
    return Number(item?.idTaiKhoan || inlineTaiKhoanId) === Number(taiKhoanId)
  }) || null
}

const resolveLoggedInAccount = async () => {
  const resolved = await resolveAccountByRole({
    service: taiKhoanService,
    expectedRoles: ['ADMIN']
  })

  if (resolved?.id) return resolved
  throw new Error('Không tìm thấy tài khoản ADMIN')
}

const fillForm = () => {
  profileForm.tenNhanVien = nhanVien.value?.tenNhanVien || ''
  profileForm.maNhanVien = nhanVien.value?.maNhanVien || ''
  profileForm.gioiTinh = nhanVien.value?.gioiTinh || 'Nam'
  profileForm.ngaySinh = nhanVien.value?.ngaySinh ? String(nhanVien.value.ngaySinh).slice(0, 10) : ''
  profileForm.soDienThoai = nhanVien.value?.soDienThoai || ''
  profileForm.diaChi = nhanVien.value?.diaChi || ''
  profileForm.trangThaiHoatDong = nhanVien.value?.trangThaiHoatDong || 'Hoạt động'
  profileForm.email = account.value?.email || ''
  const localAvatar = account.value?.id ? localStorage.getItem(getAvatarStorageKey(account.value.id)) : ''
  profileForm.avatar = localAvatar || account.value?.avatar || account.value?.hinhAnh || account.value?.anhDaiDien || ''
}

const ensureRealAdminName = async () => {
  if (!account.value?.email || !nhanVien.value?.id) return
  const mappedName = getVietnameseNameByEmail(account.value.email)
  if (!mappedName) return
  if (!isGenericVietnameseName(nhanVien.value?.tenNhanVien)) return

  try {
    await updateNhanVien(nhanVien.value.id, {
      ...nhanVien.value,
      tenNhanVien: mappedName,
      taiKhoan: { id: account.value.id },
      chucVu: nhanVien.value.chucVu || null
    })
    nhanVien.value = { ...nhanVien.value, tenNhanVien: mappedName }
  } catch {
    // Keep current name if API update fails.
  }
}

const loadProfile = async () => {
  loading.value = true
  error.value = ''

  try {
    account.value = await resolveLoggedInAccount()
    nhanVien.value = await findNhanVienByTaiKhoanId(account.value.id)

    if (!nhanVien.value?.id) {
      throw new Error('Khong tim thay nhan vien gan voi tai khoan ADMIN')
    }

    await ensureRealAdminName()
    fillForm()
    dispatchAuthContextChanged()
  } catch (e) {
    console.error('Load admin profile failed:', e)
    error.value = 'Khong the tai ho so admin. Vui long thu lai.'
  } finally {
    loading.value = false
  }
}

const saveProfileInfo = async () => {
  if (!account.value?.id || !nhanVien.value?.id) return

  let avatarToPersist = String(profileForm.avatar || '').trim()
  if (avatarToPersist.startsWith('data:')) {
    localStorage.setItem(getAvatarStorageKey(account.value.id), avatarToPersist)
    avatarToPersist = String(account.value?.avatar || '').trim()
  } else {
    localStorage.removeItem(getAvatarStorageKey(account.value.id))
  }

  saving.value = true
  try {
    await taiKhoanService.update(account.value.id, {
      ...account.value,
      email: String(profileForm.email || '').trim().toLowerCase(),
      avatar: avatarToPersist
    })

    await updateNhanVien(nhanVien.value.id, {
      ...nhanVien.value,
      tenNhanVien: profileForm.tenNhanVien,
      gioiTinh: profileForm.gioiTinh,
      ngaySinh: profileForm.ngaySinh || null,
      soDienThoai: profileForm.soDienThoai,
      diaChi: profileForm.diaChi,
      trangThaiHoatDong: profileForm.trangThaiHoatDong,
      taiKhoan: { id: account.value.id },
      chucVu: nhanVien.value.chucVu || null
    })

    localStorage.setItem('userEmail', String(profileForm.email || '').trim().toLowerCase())
    localStorage.setItem('role', 'ADMIN')
    if (String(profileForm.avatar || '').trim().startsWith('data:')) {
      toast.success('Đã lưu ảnh cục bộ cho trình duyệt hiện tại')
    }
    toast.success('Cập nhật thông tin admin thành công')
    await loadProfile()
    dispatchAuthContextChanged()
  } catch (e) {
    console.error('Save admin profile failed:', e)
    toast.error('Không thể cập nhật thông tin. Vui lòng kiểm tra dữ liệu.')
  } finally {
    saving.value = false
  }
}

const updateAvatarFromFile = (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = () => {
    profileForm.avatar = String(reader.result || '')
  }
  reader.readAsDataURL(file)
}

const triggerAvatarPicker = () => {
  avatarInputRef.value?.click()
}

const changePassword = async () => {
  if (!account.value?.id) return

  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    toast.error('Mật khẩu mới phải có ít nhất 6 ký tự')
    return
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    toast.error('Mật khẩu xác nhận không khớp')
    return
  }

  try {
    await taiKhoanService.updatePassword(
      account.value.id,
      passwordForm.currentPassword,
      passwordForm.newPassword
    )
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    toast.success('Đổi mật khẩu thành công')
  } catch (e) {
    console.error('Change admin password failed:', e)
    toast.error('Không thể đổi mật khẩu. Vui lòng kiểm tra mật khẩu hiện tại.')
  }
}

const toggleAccountStatus = async () => {
  if (!account.value?.id) return
  const nextStatus = String(account.value?.trangThaiTaiKhoan || '').toLowerCase() === 'kích hoạt' ? 'Khóa' : 'Kích hoạt'

  try {
    await taiKhoanService.update(account.value.id, {
      ...account.value,
      trangThaiTaiKhoan: nextStatus
    })
    toast.success(nextStatus === 'Khóa' ? 'Đã khóa tài khoản' : 'Đã kích hoạt tài khoản')
    await loadProfile()
  } catch (e) {
    console.error('Toggle account status failed:', e)
    toast.error('Không thể cập nhật trạng thái tài khoản')
  }
}

const logout = () => {
  localStorage.removeItem('role')
  localStorage.removeItem('userId')
  localStorage.removeItem('userEmail')
  router.push('/login')
}

onMounted(loadProfile)
</script>

<template>
  <main class="account-page">
    <div class="account-wrap">
      <header class="account-header">
        <div>
          <h1>Tài khoản Admin</h1>
          <p>Cập nhật thông tin, avatar và bảo mật tài khoản</p>
        </div>
      </header>

      <div class="tabs" v-if="!loading && !error">
        <button class="tab" :class="{ active: activeTab === 'profile' }" @click="activeTab = 'profile'">Thông tin</button>
        <button class="tab" :class="{ active: activeTab === 'security' }" @click="activeTab = 'security'">Bảo mật</button>
      </div>

      <section class="card" v-if="loading">Đang tải hồ sơ admin...</section>
      <section class="card error" v-if="!loading && error">{{ error }}</section>

      <template v-if="!loading && !error">
        <section class="card" v-show="activeTab === 'profile'">
          <h2>Thông tin cá nhân</h2>

          <div class="avatar-block">
            <img v-if="profileForm.avatar" :src="profileForm.avatar" class="avatar" alt="Avatar" />
            <div v-else class="avatar avatar-fallback">{{ avatarFallback }}</div>
            <div class="avatar-actions">
              <input
                ref="avatarInputRef"
                class="avatar-input"
                type="file"
                accept="image/*"
                @change="updateAvatarFromFile"
              />
              <button type="button" class="btn avatar-btn" @click="triggerAvatarPicker">Đổi ảnh đại diện</button>
              <p class="avatar-help">Khuyến nghị ảnh vuông, nền sáng, định dạng JPG/PNG.</p>
            </div>
          </div>

          <div class="form-grid">
            <label>
              Họ tên
              <input type="text" v-model="profileForm.tenNhanVien" />
            </label>
            <label>
              Mã nhân viên
              <input type="text" v-model="profileForm.maNhanVien" disabled />
            </label>
            <label>
              Email
              <input type="email" v-model="profileForm.email" />
            </label>
            <label>
              Số điện thoại
              <input type="text" v-model="profileForm.soDienThoai" />
            </label>
            <label>
              Giới tính
              <select v-model="profileForm.gioiTinh">
                <option value="Nam">Nam</option>
                <option value="Nữ">Nữ</option>
              </select>
            </label>
            <label>
              Ngày sinh
              <input type="date" v-model="profileForm.ngaySinh" />
            </label>
            <label>
              Địa chỉ
              <input type="text" v-model="profileForm.diaChi" />
            </label>
            <label>
              Trạng thái hoạt động
              <select v-model="profileForm.trangThaiHoatDong">
                <option value="Hoạt động">Hoạt động</option>
                <option value="Nghỉ phép">Nghỉ phép</option>
                <option value="Ngừng hoạt động">Ngừng hoạt động</option>
              </select>
            </label>
          </div>

          <div class="section-actions">
            <button class="btn primary" :disabled="saving" @click="saveProfileInfo">
              {{ saving ? 'Đang lưu...' : 'Lưu thông tin' }}
            </button>
          </div>
        </section>

        <section class="card" v-show="activeTab === 'security'">
          <h2>Bảo mật tài khoản</h2>

          <div class="form-grid">
            <label>
              Mật khẩu hiện tại
              <input type="password" v-model="passwordForm.currentPassword" />
            </label>
            <label>
              Mật khẩu mới
              <input type="password" v-model="passwordForm.newPassword" />
            </label>
            <label>
              Xác nhận mật khẩu mới
              <input type="password" v-model="passwordForm.confirmPassword" />
            </label>
          </div>

          <div class="section-actions">
            <button class="btn" @click="changePassword">Đổi mật khẩu</button>
          </div>

          <div class="status-row">
            <span class="muted">Trạng thái tài khoản:</span>
            <span class="status" :class="String(account?.trangThaiTaiKhoan || '').toLowerCase() === 'kich hoat' ? 'active' : 'inactive'">
              {{ account?.trangThaiTaiKhoan || '--' }}
            </span>
            <button class="btn" @click="toggleAccountStatus">Đổi trạng thái</button>
          </div>
        </section>

        <div class="page-bottom-actions">
          <button class="btn danger" @click="logout">Đăng xuất</button>
        </div>
      </template>
    </div>
  </main>
</template>

<style scoped>
.account-page {
  min-height: 0;
  padding: 12px 10px;
  background: transparent;
}

.account-wrap {
  max-width: 920px;
  margin: 0 auto;
  border-radius: 24px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.7);
}

.account-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.account-header h1 {
  margin: 0;
  color: #0f172a;
}

.account-header p {
  margin: 6px 0 0;
  color: #64748b;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.account-wrap .tab {
  border: 1px solid #d4dde9;
  border-radius: 999px;
  background: #fff;
  color: #334155;
  padding: 10px 16px !important;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px !important;
  appearance: none;
}

.account-wrap .tab.active {
  background: #111827;
  color: #fff;
  border-color: #111827;
}

.account-wrap .card {
  background: #fff;
  border: 1px solid #d9e1ec;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  padding: 16px;
  margin-bottom: 12px;
  background-color: #fff !important;
}

.card h2 {
  margin-top: 0;
}

.account-wrap .btn {
  border: 1px solid #d1dae6;
  border-radius: 12px;
  background: #fff;
  color: #0f172a;
  padding: 10px 14px;
  cursor: pointer;
  font-weight: 600;
}

.account-wrap .btn.primary {
  border-color: #dc2626;
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: #fff;
}

.account-wrap .btn.danger {
  border-color: #ef4444;
  color: #b91c1c;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #475569;
  font-weight: 600;
}

.account-wrap input,
.account-wrap select {
  border: 1px solid #d4dde9;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fff !important;
  min-height: 42px;
}

input:disabled {
  background: #f1f5f9;
}

.section-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.avatar-block {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #d4dde9;
}

.avatar-fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #111827;
  color: #fff;
  font-weight: 700;
}

.avatar-actions {
  flex: 1;
  display: grid;
  gap: 8px;
}

.avatar-input {
  display: none;
}

.avatar-btn {
  width: fit-content;
}

.avatar-help {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.page-bottom-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.status-row {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.status {
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
}

.status.active {
  background: #dcfce7;
  color: #166534;
}

.status.inactive {
  background: #fee2e2;
  color: #991b1b;
}

.muted {
  color: #64748b;
}

.error {
  color: #b91c1c;
}

@media (max-width: 768px) {
  .account-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .btn {
    flex: 1;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
