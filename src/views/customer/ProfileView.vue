<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import taiKhoanService from '../../services/taiKhoanService'
import {
  getAllKhachHang,
  getKhachHangByTaiKhoanId,
  updateKhachHang
} from '../../services/KhachHangService'
import { getAllHoaDon, updateHoaDonBySystemEvent } from '../../services/hoaDonService'
import {
  createDiaChi,
  deleteDiaChi,
  getDiaChiByKhachHang,
  updateDiaChi
} from '../../services/diaChiService'
import { useToast } from '../../composables/useToast'
import { getVietnameseNameByEmail, isGenericVietnameseName } from '../../utils/vietnameseNames'
import { normalizeAdminStatusLabel } from '../../utils/adminStatus'
import { describePaymentFlowState } from '../../utils/paymentWorkflow'
import { dispatchAuthContextChanged, resolveAccountByRole } from '../../utils/authContext'
import SiteNav from '../../components/SiteNav.vue'

const router = useRouter()
const route = useRoute()
const toast = useToast()

const loading = ref(true)
const saving = ref(false)
const confirmingOrderId = ref(null)
const error = ref('')
const activeTab = ref(route.query.tab === 'orders' ? 'orders' : 'profile')

const account = ref(null)
const customer = ref(null)
const orders = ref([])
const addresses = ref([])
const avatarInputRef = ref(null)
const OFFLINE_ORDER_STORAGE_KEY = 'pendingOfflineOrders'
const LOCAL_REGISTERED_PROFILES_KEY = 'localRegisteredProfiles'
const MISSING_PROFILE_TOAST_PREFIX = 'customer:missing-profile-warning:'

const profileForm = reactive({
  tenKhachHang: '',
  gioiTinh: 'Nam',
  ngaySinh: '',
  soDienThoai: '',
  email: '',
  avatar: ''
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const addressForm = reactive({
  id: null,
  diaChiCuThe: '',
  tinhThanh: '',
  quanHuyen: '',
  phuongXa: '',
  trangThai: 'Hoạt động'
})

const MAX_AVATAR_LENGTH = 4000

const getAvatarStorageKey = (accountId) => `avatar:${accountId}`

const loadLocalRegisteredProfile = (email) => {
  try {
    const raw = localStorage.getItem(LOCAL_REGISTERED_PROFILES_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    const safeObject = parsed && typeof parsed === 'object' ? parsed : {}
    const normalizedEmail = String(email || '').trim().toLowerCase()
    return safeObject[normalizedEmail] || null
  } catch {
    return null
  }
}

const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value || 0))
}

const hasCustomerProfile = computed(() => Boolean(customer.value?.id))

const hasAnyCustomerInput = () => {
  return Boolean(
    String(profileForm.tenKhachHang || '').trim()
    || String(profileForm.soDienThoai || '').trim()
    || String(profileForm.ngaySinh || '').trim()
    || String(addressForm.diaChiCuThe || '').trim()
    || String(addressForm.tinhThanh || '').trim()
    || String(addressForm.quanHuyen || '').trim()
    || String(addressForm.phuongXa || '').trim()
  )
}

const hasLocalRegisteredProfileSnapshot = () => {
  const localProfile = loadLocalRegisteredProfile(account.value?.email)
  if (!localProfile || typeof localProfile !== 'object') return false
  return Boolean(
    String(localProfile.fullName || '').trim()
    || String(localProfile.phone || '').trim()
    || String(localProfile.birthDate || '').trim()
    || String(localProfile.diaChiCuThe || '').trim()
    || String(localProfile.tinhThanh || '').trim()
    || String(localProfile.quanHuyen || '').trim()
    || String(localProfile.phuongXa || '').trim()
  )
}

const shouldWarnMissingCustomerProfile = () => {
  if (hasCustomerProfile.value) return false
  return !hasAnyCustomerInput() && !hasLocalRegisteredProfileSnapshot()
}

const formatDateTime = (value) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleString('vi-VN')
}

const getOrderTotal = (order) => {
  const directTotal = Number(
    order?.displayTotal
    ?? order?.thanhTien
    ?? order?.tongTien
    ?? order?.tongThanhToan
    ?? order?.total
    ?? 0
  )
  if (directTotal > 0) return directTotal

  const details = Array.isArray(order?.hoaDonChiTiets)
    ? order.hoaDonChiTiets
    : (Array.isArray(order?.items) ? order.items : [])

  const subtotal = details.reduce((sum, item) => {
    const lineTotal = Number(item?.thanhTien || 0)
    if (lineTotal > 0) return sum + lineTotal
    return sum + Number(item?.giaBan || item?.price || 0) * Number(item?.soLuong || item?.quantity || 0)
  }, 0)

  return Math.max(subtotal + Number(order?.phiShip || order?.shipping || 0) - Number(order?.giaSauGiamGia || order?.discount || 0), 0)
}

const isVnpayOrder = (order) => {
  const paymentText = String(order?.phuongThucThanhToan || order?.paymentMethod || '').toUpperCase()
  const statusText = String(order?.statusNote || '').toUpperCase()
  return paymentText.includes('VNPAY') || statusText.includes('VNPAY')
}

const shouldShowPaymentFlow = (order) => {
  return Boolean(order?.isOfflineFallback || isVnpayOrder(order))
}

const mapOfflineOrders = (customerId) => {
  const stored = JSON.parse(localStorage.getItem(OFFLINE_ORDER_STORAGE_KEY) || '[]')
  if (!Array.isArray(stored)) return []

  return stored
    .filter((item) => Number(item?.customerId) === Number(customerId))
    .map((item) => ({
      ...item,
      id: `offline-${item.id}`,
      maHoaDon: item.id,
      ngayTao: item.createdAt,
      thanhTien: Number(item.total || 0),
      phuongThucThanhToan: item.paymentMethod || 'COD',
      trangThai: item.syncStatus === 'pending' ? 'Chờ đồng bộ' : 'Đã ghi nhận',
      statusNote: item.syncStatus === 'pending' ? 'Đơn COD đã lưu tạm trên trình duyệt, chờ đồng bộ vào hệ thống.' : '',
      isOfflineFallback: true
    }))
}

const getCurrentAccount = async () => {
  const matched = await resolveAccountByRole({
    service: taiKhoanService,
    expectedRoles: ['CUSTOMER', 'KHACH_HANG', 'KHACHHANG', 'USER'],
    allowFallback: false
  })

  if (!matched?.id) {
    throw new Error('Không tìm thấy tài khoản CUSTOMER hiện tại')
  }

  return matched
}

const extractCustomerList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const getTaiKhoanIdFromKhachHang = (item) => {
  return Number(item?.idTaiKhoan || item?.taiKhoan?.id || 0)
}

const findCustomerFromPages = async (accountId) => {
  const pageSize = 50
  let page = 0
  let totalPages = 1

  while (page < totalPages) {
    const listRes = await getAllKhachHang(page, pageSize)
    const pageData = listRes?.data
    const list = extractCustomerList(pageData)
    const matched = list.find((item) => getTaiKhoanIdFromKhachHang(item) === Number(accountId))
    if (matched) return matched

    const detectedTotalPages = Number(pageData?.totalPages)
    if (Number.isFinite(detectedTotalPages) && detectedTotalPages > 0) {
      totalPages = detectedTotalPages
    } else if (list.length < pageSize) {
      break
    } else {
      totalPages = page + 2
    }
    page += 1
  }

  return null
}

const getCurrentCustomer = async (accountId, accountEmail = '') => {
  try {
    const byTaiKhoan = await getKhachHangByTaiKhoanId(accountId)
    if (byTaiKhoan?.data?.id) return byTaiKhoan.data
  } catch {
    // Fallback below.
  }

  const byPages = await findCustomerFromPages(accountId)
  if (byPages?.id) return byPages

  const listRes = await getAllKhachHang(0, 500)
  const list = extractCustomerList(listRes?.data)
  const normalizedEmail = String(accountEmail || '').trim().toLowerCase()
  if (!normalizedEmail) return null

  return list.find((item) => {
    const email = String(item?.taiKhoan?.email || '').trim().toLowerCase()
    return email && email === normalizedEmail
  }) || null
}

const loadOrders = async (customerId) => {
  const offlineOrders = mapOfflineOrders(customerId)

  try {
    const res = await getAllHoaDon()
    const list = Array.isArray(res?.data) ? res.data : (Array.isArray(res?.data?.content) ? res.data.content : [])
    const backendOrders = list
      .filter((item) => Number(item?.khachHang?.id || item?.khachHangId) === Number(customerId))
      .map((item) => ({
        ...item,
        displayTotal: getOrderTotal(item)
      }))

    orders.value = [...offlineOrders, ...backendOrders]
      .sort((a, b) => String(b?.ngayTao || '').localeCompare(String(a?.ngayTao || '')))

    const newlyConfirmed = orders.value.filter((item) => {
      const state = describePaymentFlowState(item)
      if (state.code !== 'EMPLOYEE_CONFIRMED') return false
      const noticeKey = `vnpay_notice_${item.id}`
      if (localStorage.getItem(noticeKey)) return false
      localStorage.setItem(noticeKey, new Date().toISOString())
      return true
    })

    if (newlyConfirmed.length) {
      toast.success(`Có ${newlyConfirmed.length} đơn VNPay đã được xác nhận thanh toán thành công`)
    }
  } catch {
    orders.value = offlineOrders.sort((a, b) => String(b?.ngayTao || '').localeCompare(String(a?.ngayTao || '')))
  }
}

const getOrderStatusLabel = (order) => {
  return normalizeAdminStatusLabel(order?.orderStatusName || order?.trangThai || '')
}

const getOrderPaymentFlow = (order) => {
  if (order?.isOfflineFallback) {
    return {
      code: 'OFFLINE_PENDING',
      tone: 'pending',
      label: order?.statusNote || 'Đơn hàng đã được ghi nhận cục bộ và đang chờ đồng bộ lên hệ thống.'
    }
  }
  if (order?.paymentFlowCode) {
    return {
      code: String(order.paymentFlowCode || ''),
      tone: String(order.paymentFlowTone || 'neutral'),
      label: String(order.paymentFlowLabel || '')
    }
  }
  return describePaymentFlowState(order)
}

const canConfirmPayment = (order) => {
  if (order?.isOfflineFallback) return false
  const paymentText = String(order?.phuongThucThanhToan || order?.paymentMethod || '').toUpperCase()
  const isVnpay = paymentText.includes('VNPAY') || String(order?.statusNote || '').toUpperCase().includes('VNPAY')
  if (!isVnpay) return false
  return getOrderPaymentFlow(order).code === 'WAIT_CUSTOMER'
}

const confirmPayment = async (order) => {
  if (!order?.id || !canConfirmPayment(order)) return

  confirmingOrderId.value = order.id
  try {
    await updateHoaDonBySystemEvent(
      order.id,
      'THANH_TOAN_KHACH_XAC_NHAN',
      'Khach hang da bam xac nhan thanh toan'
    )
    toast.success('Đã gửi yêu cầu xác nhận thanh toán tới nhân viên')
    await loadOrders(customer.value?.id)
  } catch (e) {
    console.error('Customer confirm payment failed:', e)
    toast.error(e?.response?.data?.message || 'Không thể gửi xác nhận thanh toán')
  } finally {
    confirmingOrderId.value = null
  }
}

const loadAddresses = async (customerId) => {
  try {
    const res = await getDiaChiByKhachHang(customerId)
    addresses.value = Array.isArray(res?.data) ? res.data : []
  } catch {
    addresses.value = []
  }
}

const fillProfileForm = () => {
  const localProfile = loadLocalRegisteredProfile(account.value?.email)
  profileForm.tenKhachHang = customer.value?.tenKhachHang || localProfile?.fullName || localStorage.getItem('userName') || ''
  profileForm.gioiTinh = customer.value?.gioiTinh || localProfile?.gender || localStorage.getItem('userGender') || 'Nam'
  profileForm.ngaySinh = customer.value?.ngaySinh
    ? String(customer.value.ngaySinh).slice(0, 10)
    : (localProfile?.birthDate || localStorage.getItem('userBirthDate') || '')
  profileForm.soDienThoai = customer.value?.soDienThoai || localProfile?.phone || localStorage.getItem('userPhone') || ''
  profileForm.email = account.value?.email || ''
  const localAvatar = account.value?.id ? localStorage.getItem(getAvatarStorageKey(account.value.id)) : ''
  profileForm.avatar = localAvatar || account.value?.avatar || account.value?.hinhAnh || account.value?.anhDaiDien || ''

  if (!addresses.value.length) {
    addressForm.diaChiCuThe = localProfile?.diaChiCuThe || localStorage.getItem('userDiaChiCuThe') || ''
    addressForm.tinhThanh = localProfile?.tinhThanh || localStorage.getItem('userTinhThanh') || ''
    addressForm.quanHuyen = localProfile?.quanHuyen || localStorage.getItem('userQuanHuyen') || ''
    addressForm.phuongXa = localProfile?.phuongXa || localStorage.getItem('userPhuongXa') || ''
  }
}

const ensureRealCustomerName = async () => {
  if (!account.value?.email || !customer.value?.id) return
  const mappedName = getVietnameseNameByEmail(account.value.email)
  if (!mappedName) return
  if (!isGenericVietnameseName(customer.value?.tenKhachHang)) return

  try {
    await updateKhachHang(customer.value.id, {
      ...customer.value,
      tenKhachHang: mappedName
    })
    customer.value = { ...customer.value, tenKhachHang: mappedName }
  } catch {
    // Keep current name if API update fails.
  }
}

const loadAccountCenter = async () => {
  loading.value = true
  error.value = ''
  orders.value = []
  addresses.value = []

  try {
    account.value = await getCurrentAccount()
    customer.value = await getCurrentCustomer(account.value.id, account.value?.email)
    fillProfileForm()

    if (customer.value?.id) {
      await ensureRealCustomerName()
      await Promise.all([
        loadOrders(customer.value.id),
        loadAddresses(customer.value.id)
      ])
      fillProfileForm()
    } else {
      activeTab.value = 'profile'
      fillProfileForm()
      if (shouldWarnMissingCustomerProfile()) {
        const accountKey = String(account.value?.id || account.value?.email || '').trim().toLowerCase()
        if (accountKey) {
          toast.showToast(
            'Tài khoản đã đăng nhập nhưng chưa có hồ sơ khách hàng. Bạn vẫn có thể cập nhật email, ảnh đại diện và mật khẩu.',
            'warning',
            {
              onceKey: `${MISSING_PROFILE_TOAST_PREFIX}${accountKey}`
            }
          )
        }
      }
    }
  } catch (e) {
    console.error('Load account center failed:', e)
    error.value = 'Không thể tải trung tâm tài khoản. Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}

const saveProfileInfo = async () => {
  if (!account.value?.id) return

  let avatarToPersist = String(profileForm.avatar || '').trim()
  if (avatarToPersist.startsWith('data:')) {
    localStorage.setItem(getAvatarStorageKey(account.value.id), avatarToPersist)
    // Data URL usually cannot fit DB avatar column; keep backend avatar unchanged.
    avatarToPersist = String(account.value?.avatar || '').trim()
    if (!avatarToPersist && profileForm.avatar.length > MAX_AVATAR_LENGTH) {
      toast.success('Đã lưu ảnh cục bộ cho trình duyệt hiện tại. Để đồng bộ mọi thiết bị, vui lòng dùng URL ảnh.')
    }
  } else if (account.value?.id) {
    localStorage.removeItem(getAvatarStorageKey(account.value.id))
  }

  saving.value = true
  try {
    await taiKhoanService.update(account.value.id, {
      ...account.value,
      email: String(profileForm.email || '').trim().toLowerCase(),
      avatar: avatarToPersist
    })

    if (customer.value?.id) {
      await updateKhachHang(customer.value.id, {
        ...customer.value,
        tenKhachHang: profileForm.tenKhachHang,
        gioiTinh: profileForm.gioiTinh,
        ngaySinh: profileForm.ngaySinh || null,
        soDienThoai: profileForm.soDienThoai
      })
    }

    localStorage.setItem('userEmail', String(profileForm.email || '').trim().toLowerCase())
    localStorage.setItem('userName', String(profileForm.tenKhachHang || '').trim())
    localStorage.setItem('userPhone', String(profileForm.soDienThoai || '').trim())
    dispatchAuthContextChanged()

    if (customer.value?.id) {
      toast.success('Cập nhật thông tin tài khoản thành công')
    } else {
      toast.success('Đã lưu thông tin tài khoản cơ bản. Hồ sơ khách hàng sẽ cập nhật sau khi backend sẵn sàng.')
    }

    await loadAccountCenter()
  } catch (e) {
    console.error('Save profile failed:', e)
    toast.error('Không thể cập nhật thông tin. Vui lòng kiểm tra lại dữ liệu.')
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
    console.error('Change password failed:', e)
    toast.error('Không thể đổi mật khẩu. Vui lòng kiểm tra mật khẩu hiện tại.')
  }
}

const startEditAddress = (item) => {
  addressForm.id = item?.id || null
  addressForm.diaChiCuThe = item?.diaChiCuThe || ''
  addressForm.tinhThanh = item?.tinhThanh || ''
  addressForm.quanHuyen = item?.quanHuyen || ''
  addressForm.phuongXa = item?.phuongXa || ''
  addressForm.trangThai = item?.trangThai || 'Hoạt động'
}

const resetAddressForm = () => {
  addressForm.id = null
  addressForm.diaChiCuThe = ''
  addressForm.tinhThanh = ''
  addressForm.quanHuyen = ''
  addressForm.phuongXa = ''
  addressForm.trangThai = 'Hoạt động'
}

const saveAddress = async () => {
  if (!customer.value?.id) {
    toast.warning('Tài khoản này chưa có hồ sơ khách hàng, chưa thể lưu địa chỉ.')
    return
  }
  if (!addressForm.diaChiCuThe.trim()) {
    toast.error('Vui lòng nhập địa chỉ cụ thể')
    return
  }

  const payload = {
    khachHang: { id: customer.value.id },
    diaChiCuThe: addressForm.diaChiCuThe,
    tinhThanh: addressForm.tinhThanh,
    quanHuyen: addressForm.quanHuyen,
    phuongXa: addressForm.phuongXa,
    trangThai: addressForm.trangThai || 'Hoạt động'
  }

  try {
    if (addressForm.id) {
      await updateDiaChi(addressForm.id, payload)
      toast.success('Cập nhật địa chỉ thành công')
    } else {
      await createDiaChi(payload)
      toast.success('Thêm địa chỉ thành công')
    }
    resetAddressForm()
    await loadAddresses(customer.value.id)
  } catch (e) {
    console.error('Save address failed:', e)
    toast.error('Không thể lưu địa chỉ')
  }
}

const removeAddress = async (id) => {
  if (!id || !customer.value?.id) {
    toast.warning('Tài khoản này chưa có hồ sơ khách hàng, chưa thể xóa địa chỉ.')
    return
  }
  try {
    await deleteDiaChi(id)
    toast.success('Đã xóa địa chỉ')
    await loadAddresses(customer.value.id)
  } catch (e) {
    console.error('Delete address failed:', e)
    toast.error('Không thể xóa địa chỉ')
  }
}

const deliveryOrders = computed(() => {
  if (!hasCustomerProfile.value) return []
  return orders.value.filter((item) => {
    const statusCode = String(item?.orderStatusCode || '').trim().toUpperCase()
    return statusCode === 'DANG_GIAO' || statusCode === 'CHO_LAY_HANG'
  })
})

const goBackHome = () => {
  router.push('/home')
}

const logout = () => {
  localStorage.removeItem('role')
  localStorage.removeItem('userId')
  localStorage.removeItem('userEmail')
  router.push('/login')
}

onMounted(loadAccountCenter)
</script>

<template>
  <div>
  <SiteNav />
  <main class="account-page">
    <div class="account-wrap">
      <header class="account-header">
        <div>
          <h1>Tài khoản của tôi</h1>
          <p>Thông tin người dùng, địa chỉ, mật khẩu và đơn hàng</p>
        </div>
      </header>

      <div class="tabs" v-if="!loading && !error">
        <button class="tab" :class="{ active: activeTab === 'profile' }" @click="activeTab = 'profile'">Thông tin người dùng</button>
        <button class="tab" :class="{ active: activeTab === 'orders' }" :disabled="!hasCustomerProfile" @click="activeTab = 'orders'">Lịch sử mua hàng</button>
        <button class="tab" :class="{ active: activeTab === 'delivery' }" :disabled="!hasCustomerProfile" @click="activeTab = 'delivery'">Đang giao</button>
      </div>

      <section class="card" v-if="loading">Đang tải trung tâm tài khoản...</section>
      <section class="card error" v-if="!loading && error">{{ error }}</section>

      <template v-if="!loading && !error">
        <section class="card" v-show="activeTab === 'profile'">
          <h2>Thông tin người dùng</h2>

          <div class="avatar-block">
            <img v-if="profileForm.avatar" :src="profileForm.avatar" class="avatar" alt="Avatar" />
            <div v-else class="avatar avatar-fallback">{{ (profileForm.tenKhachHang || 'U').slice(0, 1) }}</div>
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
              <input type="text" v-model="profileForm.tenKhachHang" />
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
                <option value="Khác">Khác</option>
              </select>
            </label>
            <label>
              Ngày sinh
              <input type="date" v-model="profileForm.ngaySinh" />
            </label>
          </div>

          <div class="section-actions">
            <button class="btn primary" :disabled="saving" @click="saveProfileInfo">
              {{ saving ? 'Đang lưu...' : 'Lưu thông tin' }}
            </button>
          </div>

          <h3>Đổi mật khẩu</h3>
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

          <h3>Địa chỉ nhận hàng</h3>
          <div class="address-form">
            <input type="text" v-model="addressForm.diaChiCuThe" placeholder="Số nhà, tên đường" />
            <input type="text" v-model="addressForm.phuongXa" placeholder="Phường/Xã" />
            <input type="text" v-model="addressForm.quanHuyen" placeholder="Quận/Huyện" />
            <input type="text" v-model="addressForm.tinhThanh" placeholder="Tỉnh/Thành phố" />
            <div class="section-actions">
              <button class="btn" @click="resetAddressForm">Làm mới</button>
              <button class="btn primary" @click="saveAddress">{{ addressForm.id ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ' }}</button>
            </div>
          </div>

          <div class="address-list" v-if="addresses.length">
            <article class="address-item" v-for="addr in addresses" :key="addr.id">
              <div>
                <strong>{{ addr.diaChiCuThe }}</strong>
                <p>{{ addr.phuongXa }}, {{ addr.quanHuyen }}, {{ addr.tinhThanh }}</p>
              </div>
              <div class="item-actions">
                <button class="btn" @click="startEditAddress(addr)">Sửa</button>
                <button class="btn danger" @click="removeAddress(addr.id)">Xóa</button>
              </div>
            </article>
          </div>
        </section>

        <section class="card" v-show="activeTab === 'orders'">
          <h2>Lịch sử mua hàng</h2>
          <div class="order-list" v-if="orders.length">
            <article class="order-item" v-for="order in orders" :key="order.id">
              <div class="order-head">
                <strong>#{{ order.maHoaDon || `HD-${order.id}` }}</strong>
                <span class="status">{{ getOrderStatusLabel(order) }}</span>
              </div>
              <div class="order-meta">
                <span>Ngày tạo: {{ formatDateTime(order.ngayTao) }}</span>
                <span>Tổng tiền: {{ formatCurrency(getOrderTotal(order)) }}</span>
                <span>Phương thức: {{ order.phuongThucThanhToan || 'COD' }}</span>
              </div>
              <div
                v-if="shouldShowPaymentFlow(order)"
                class="payment-flow"
                :class="`payment-${getOrderPaymentFlow(order).tone}`"
              >
                {{ getOrderPaymentFlow(order).label }}
              </div>
              <div class="order-actions" v-if="canConfirmPayment(order)">
                <button
                  class="btn primary"
                  :disabled="confirmingOrderId === order.id"
                  @click="confirmPayment(order)"
                >
                  {{ confirmingOrderId === order.id ? 'Đang gửi...' : 'Xác nhận thanh toán' }}
                </button>
              </div>
            </article>
          </div>
          <p class="muted" v-else>Bạn chưa có đơn hàng nào.</p>
        </section>

        <section class="card" v-show="activeTab === 'delivery'">
          <h2>Theo dõi đơn hàng đang giao</h2>
          <div class="order-list" v-if="deliveryOrders.length">
            <article class="order-item" v-for="order in deliveryOrders" :key="order.id">
              <div class="order-head">
                <strong>#{{ order.maHoaDon || `HD-${order.id}` }}</strong>
                <span class="status shipping">{{ getOrderStatusLabel(order) }}</span>
              </div>
              <div class="order-meta">
                <span>Ngày tạo: {{ formatDateTime(order.ngayTao) }}</span>
                <span>Địa chỉ nhận: {{ order.diaChiNhanHang || 'Đang cập nhật' }}</span>
              </div>
            </article>
          </div>
          <p class="muted" v-else>Hiện không có đơn hàng đang giao.</p>
        </section>

        <div class="page-bottom-actions">
          <button class="btn danger" @click="logout">Đăng xuất</button>
        </div>
      </template>
    </div>
  </main>
  </div>
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

.tab {
  border: 1px solid #d4dde9;
  border-radius: 999px;
  background: #fff;
  color: #334155;
  padding: 8px 14px;
  cursor: pointer;
  font-weight: 600;
}

.tab.active {
  background: #111827;
  color: #fff;
  border-color: #111827;
}

.tab:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.card {
  background: #fff;
  border: 1px solid #d9e1ec;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
  padding: 16px;
  margin-bottom: 12px;
}

.card h2 {
  margin-top: 0;
}

.card h3 {
  margin: 18px 0 10px;
}

.btn {
  border: 1px solid #d1dae6;
  border-radius: 12px;
  background: #fff;
  color: #0f172a;
  padding: 10px 14px;
  cursor: pointer;
  font-weight: 600;
}

.btn.primary {
  border-color: #dc2626;
  background: linear-gradient(135deg, #dc2626, #b91c1c);
  color: #fff;
}

.btn.danger {
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

input,
select {
  border: 1px solid #d4dde9;
  border-radius: 10px;
  padding: 10px 12px;
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

.address-form {
  display: grid;
  gap: 8px;
}

.address-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.address-item {
  border: 1px solid #e5ebf4;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.address-item p {
  margin: 6px 0 0;
  color: #64748b;
}

.item-actions {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.order-list {
  display: grid;
  gap: 10px;
}

.order-item {
  border: 1px solid #e5ebf4;
  border-radius: 12px;
  padding: 12px;
}

.order-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.status {
  background: #f1f5f9;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.status.shipping {
  background: #dbeafe;
  color: #1d4ed8;
}

.order-meta {
  margin-top: 8px;
  display: grid;
  gap: 4px;
  color: #64748b;
  font-size: 14px;
}

.payment-flow {
  margin-top: 10px;
  font-size: 13px;
  font-weight: 700;
}

.payment-success {
  color: #166534;
}

.payment-warning {
  color: #b45309;
}

.payment-neutral {
  color: #475569;
}

.order-actions {
  margin-top: 10px;
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

  .address-item {
    flex-direction: column;
  }
}
</style>
