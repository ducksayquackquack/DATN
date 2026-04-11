<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import CustomerPageLayout from '../../../components/customer/CustomerPageLayout.vue'
import { lookupHoaDon } from '../../../services/hoaDonService'
import { CheckCheck, ClipboardList, Truck, CircleCheckBig } from 'lucide-vue-next'

const route = useRoute()

const maDonHang = ref('')
const dangTraCuu = ref(false)
const loi = ref('')
const ketQua = ref(null)

const formatDate = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const d = String(date.getDate()).padStart(2, '0')
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const y = date.getFullYear()
  return `${d}/${m}/${y}`
}

const formatDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const d = String(date.getDate()).padStart(2, '0')
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const y = date.getFullYear()
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  return `${d}/${m}/${y} ${hh}:${mm}`
}

const formatMoney = (value) => {
  const amount = Number(value)
  if (!Number.isFinite(amount)) return '0₫'
  return `${new Intl.NumberFormat('vi-VN').format(amount)}₫`
}

const normalizeMetaText = (value = '') => {
  const normalized = String(value || '').trim()
  if (!normalized) return ''
  const lowered = normalized.toLowerCase()
  if (['null', 'undefined', 'n/a', 'na', 'khong ro', 'không rõ', 'chua ro', 'chưa rõ'].includes(lowered)) {
    return ''
  }
  return normalized
}

const getLookupItemColor = (item = {}) => {
  return normalizeMetaText(item?.mauSac?.tenMauSac || item?.mauSac?.tenMau || item?.tenMauSac || item?.tenMau || item?.mauSac)
}

const getLookupItemSize = (item = {}) => {
  return normalizeMetaText(item?.kichThuoc?.tenKichThuoc || item?.kichThuoc?.tenSize || item?.tenKichThuoc || item?.tenSize || item?.kichThuoc)
}

const getLookupItemVoucher = (item = {}) => {
  const code = String(item?.maPhieuGiamGia || item?.voucherCode || item?.maVoucher || item?.phieuGiamGia || item?.voucher?.code || '').trim()
  const discount = Number(item?.voucherDiscount || item?.giamGiaVoucher || item?.giaTriGiamGia || item?.discount || item?.voucher?.discount || 0)
  if (!code && !(discount > 0)) return null
  return { code, discount }
}

/* ── Main status steps (always shown) ── */
const MAIN_STEPS = [
  { code: 'DON_MOI', label: 'Đơn mới tạo', icon: 'ClipboardList' },
  { code: 'DA_XAC_NHAN', label: 'Shop đã xác nhận', icon: 'CheckCheck' },
  { code: 'DANG_GIAO', label: 'Đang giao hàng', icon: 'Truck' },
  { code: 'HOAN_THANH', label: 'Giao hàng thành công', icon: 'CircleCheckBig' }
]

const STEP_ICON_MAP = {
  ClipboardList,
  CheckCheck,
  Truck,
  CircleCheckBig
}

const MAIN_CODE_MAP = {
  DON_MOI: 0,
  DA_XAC_NHAN: 1,
  CHO_LAY_HANG: 1,
  DANG_GIAO: 2,
  DA_GIAO: 3,
  HOAN_THANH: 3
}

const TERMINAL_CODES = ['GIAO_THAT_BAI', 'HOAN_VE', 'HUY']

const normalizeText = (value = '') => {
  return String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

const findStepInfo = (timeline, matchers = []) => {
  const found = (timeline || []).find((step) => {
    const code = normalizeText(step?.code)
    const label = normalizeText(step?.label)
    const note = normalizeText(step?.note)
    return matchers.some((matcher) => code.includes(matcher) || label.includes(matcher) || note.includes(matcher))
  })

  return {
    changedAt: found?.changedAt || '',
    note: String(found?.note || '').trim()
  }
}

const mainTimeline = computed(() => {
  const raw = Array.isArray(ketQua.value?.tracking?.timeline) ? ketQua.value.tracking.timeline : []

  const donMoi = findStepInfo(raw, ['don moi', 'khoi tao', 'created'])
  const daXacNhan = findStepInfo(raw, ['xac nhan', 'cho lay hang', 'duoc tiep nhan'])
  const dangGiao = findStepInfo(raw, ['dang giao', 'trung chuyen', 'out for delivery'])
  const hoanThanh = findStepInfo(raw, ['hoan thanh', 'thanh cong', 'da giao', 'delivered'])

  const mapped = [
    { ...MAIN_STEPS[0], time: donMoi.changedAt || ketQua.value?.order?.ngayTao || '', note: donMoi.note || '' },
    { ...MAIN_STEPS[1], time: daXacNhan.changedAt || '', note: daXacNhan.note || '' },
    { ...MAIN_STEPS[2], time: dangGiao.changedAt || '', note: dangGiao.note || '' },
    { ...MAIN_STEPS[3], time: hoanThanh.changedAt || '', note: hoanThanh.note || '' }
  ]

  return mapped
})

const currentTrackingStepCode = computed(() => {
  const raw = Array.isArray(ketQua.value?.tracking?.timeline) ? ketQua.value.tracking.timeline : []
  const currentRow = raw.find((step) => String(step?.state || '').toLowerCase() === 'current')
  const code = String(currentRow?.code || '').toUpperCase()

  if (code === 'CREATED') return 'DON_MOI'
  if (code === 'CONFIRMED' || code === 'PICKED_UP' || code === 'IN_TRANSIT') return 'DA_XAC_NHAN'
  if (code === 'OUT_FOR_DELIVERY') return 'DANG_GIAO'
  if (code === 'DELIVERED') return 'HOAN_THANH'
  return ''
})

const currentStepIndex = computed(() => {
  const statusName = String(ketQua.value?.order?.orderStatusName || '').trim()
  const code = String(currentTrackingStepCode.value || ketQua.value?.tracking?.currentCode || '').toUpperCase()

  if (TERMINAL_CODES.includes(code)) return -1

  // Try code mapping first
  if (MAIN_CODE_MAP[code] !== undefined) return MAIN_CODE_MAP[code]

  // Fallback text matching
  if (/hoàn thành|thành công|da giao/i.test(statusName)) return 3
  if (/đang giao/i.test(statusName)) return 2
  if (/chờ lấy|xác nhận/i.test(statusName)) return 1
  return 0
})

const isTerminal = computed(() => {
  const code = String(ketQua.value?.tracking?.currentCode || '').toUpperCase()
  return TERMINAL_CODES.includes(code)
})

const terminalLabel = computed(() => {
  const code = String(ketQua.value?.tracking?.currentCode || '').toUpperCase()
  if (code === 'GIAO_THAT_BAI') return 'Giao thất bại'
  if (code === 'HOAN_VE') return 'Hoàn về'
  if (code === 'HUY') return 'Đã hủy'
  return ketQua.value?.order?.orderStatusName || ''
})

/* ── GHN sub-events (only shown when they exist) ── */
const ghnEvents = computed(() => {
  const raw = ketQua.value?.tracking?.timeline || []
  return raw.filter((step) => {
    const code = String(step.code || '').toUpperCase()
    return code.startsWith('GHN_') && step.changedAt
  })
})

const statusTone = computed(() => {
  const name = String(ketQua.value?.order?.orderStatusName || '').toLowerCase()
  if (/hoàn thành|thành công/.test(name)) return 'green'
  if (/đang giao|chờ lấy/.test(name)) return 'blue'
  if (/hủy|thất bại|hoàn về/.test(name)) return 'red'
  return 'amber'
})

const progressPercent = computed(() => {
  if (isTerminal.value) return 100
  const idx = currentStepIndex.value
  if (idx < 0) return 0
  return Math.round((idx / (MAIN_STEPS.length - 1)) * 100)
})

const traCuu = async () => {
  loi.value = ''
  ketQua.value = null

  const ma = String(maDonHang.value || '').trim().replace(/^#+/, '')
  if (!ma) {
    loi.value = 'Vui lòng nhập mã đơn hàng.'
    return
  }

  dangTraCuu.value = true

  try {
    const response = await lookupHoaDon(ma)
    ketQua.value = response?.data || null

    if (!ketQua.value?.order?.maHoaDon) {
      loi.value = 'Không tìm thấy đơn hàng phù hợp. Vui lòng kiểm tra lại mã đơn.'
      ketQua.value = null
    }
  } catch (error) {
    const status = error?.response?.status
    const backendMsg = error?.response?.data?.message || error?.response?.data?.error || ''
    if (status === 404 || /khong tim thay|not found/i.test(backendMsg)) {
      loi.value = 'Không tìm thấy đơn hàng phù hợp. Vui lòng kiểm tra lại mã đơn.'
    } else if (backendMsg) {
      loi.value = backendMsg
    } else {
      loi.value = 'Không thể tra cứu đơn hàng lúc này. Vui lòng thử lại sau.'
    }
  } finally {
    dangTraCuu.value = false
  }
}

onMounted(() => {
  const queryMa = String(route.query.ma || '').trim()
  if (queryMa) maDonHang.value = queryMa
  if (queryMa) traCuu()
})
</script>

<template>
  <CustomerPageLayout breadcrumb="Tra cứu đơn hàng" max-width="900px">
    <section class="lookup-shell">
      <article class="lookup-card">
        <header class="lookup-header">
          <h1>Tra cứu đơn hàng</h1>
          <p>Nhập mã đơn để theo dõi tiến trình giao hàng — hoặc mở từ link email để tra cứu tự động.</p>
        </header>

        <form class="lookup-form" @submit.prevent="traCuu">
          <div class="input-row">
            <input v-model="maDonHang" type="text" placeholder="Nhập mã đơn hàng (VD: HD1024)" class="lookup-input" />
            <button class="btn-lookup" type="submit" :disabled="dangTraCuu">
              {{ dangTraCuu ? 'Đang tra cứu...' : 'Tra cứu đơn hàng' }}
            </button>
          </div>
        </form>

        <p v-if="loi" class="lookup-error">{{ loi }}</p>

        <div v-if="ketQua" class="lookup-result">
          <!-- ── Status badge ── -->
          <div class="result-header">
            <h2>Kết quả tra cứu</h2>
            <span class="status-pill" :class="`tone-${statusTone}`">{{ ketQua.order?.orderStatusName || 'Đang xử lý' }}</span>
          </div>

          <!-- ── Main progress stepper ── -->
          <section class="stepper-card">
            <div class="stepper-bar">
              <div class="stepper-bar-bg"></div>
              <div class="stepper-bar-fill" :class="{ terminal: isTerminal }" :style="{ width: progressPercent + '%' }"></div>
            </div>
            <div class="stepper-steps">
              <div
                v-for="(step, idx) in mainTimeline"
                :key="step.code"
                class="stepper-step"
                :class="{
                  done: !isTerminal && currentStepIndex >= idx,
                  active: !isTerminal && currentStepIndex === idx,
                  future: !isTerminal && currentStepIndex < idx,
                  dimmed: isTerminal
                }"
              >
                <div class="stepper-icon">
                  <component :is="STEP_ICON_MAP[step.icon]" :size="16" />
                </div>
                <span class="stepper-label">{{ step.label }}</span>
                <small class="stepper-time">{{ formatDateTime(step.time) }}</small>
              </div>
            </div>

            <div v-if="isTerminal" class="terminal-badge" :class="`tone-${statusTone}`">
              {{ terminalLabel }}
            </div>
          </section>

          <!-- ── GHN sub-events ── -->
          <section v-if="ghnEvents.length" class="ghn-card">
            <h3>Chi tiết vận chuyển (GHN)</h3>
            <div class="ghn-timeline">
              <div v-for="(ev, idx) in ghnEvents" :key="idx" class="ghn-event">
                <div class="ghn-dot"></div>
                <div>
                  <p class="ghn-label">{{ ev.label || ev.code }}</p>
                  <small>{{ formatDateTime(ev.changedAt) }}</small>
                  <p v-if="ev.note" class="ghn-note">{{ ev.note }}</p>
                </div>
              </div>
            </div>
          </section>

          <!-- ── Order details grid ── -->
          <section class="info-grid">
            <div class="info-item">
              <span class="info-label">Mã đơn</span>
              <strong>{{ ketQua.order?.maHoaDon }}</strong>
            </div>
            <div class="info-item">
              <span class="info-label">Trạng thái</span>
              <strong>{{ ketQua.order?.orderStatusName }}</strong>
            </div>
            <div class="info-item">
              <span class="info-label">Người nhận</span>
              <strong>{{ ketQua.customer?.tenKhachHang || '-' }}</strong>
            </div>
            <div class="info-item">
              <span class="info-label">Ngày tạo</span>
              <strong>{{ formatDate(ketQua.order?.ngayTao) }}</strong>
            </div>
            <div class="info-item">
              <span class="info-label">Tạm tính</span>
              <strong>{{ formatMoney(ketQua.totals?.subtotal) }}</strong>
            </div>
            <div class="info-item">
              <span class="info-label">Phí vận chuyển</span>
              <strong>{{ formatMoney(ketQua.totals?.shippingFee) }}</strong>
            </div>
            <div class="info-item">
              <span class="info-label">Giảm giá</span>
              <strong class="discount">-{{ formatMoney(ketQua.totals?.discount) }}</strong>
            </div>
            <div class="info-item total">
              <span class="info-label">Tổng cộng</span>
              <strong>{{ formatMoney(ketQua.totals?.grandTotal) }}</strong>
            </div>
          </section>

          <!-- ── Items table ── -->
          <section v-if="Array.isArray(ketQua.items) && ketQua.items.length" class="items-card">
            <h3>Chi tiết sản phẩm</h3>
            <div class="items-scroll">
              <table class="items-table">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th>SL</th>
                    <th>Đơn giá</th>
                    <th>Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="it in ketQua.items" :key="it.id">
                    <td class="item-name">
                      <div class="item-name-main">{{ it.tenSanPham || it.tenSanPhamChiTiet || it.maSanPham || it.maSanPhamChiTiet || `SPCT#${it.spctId}` }}</div>
                      <div v-if="getLookupItemColor(it) || getLookupItemSize(it)" class="item-meta-inline">
                        <template v-if="getLookupItemColor(it)">Màu: {{ getLookupItemColor(it) }}</template>
                        <template v-if="getLookupItemColor(it) && getLookupItemSize(it)"> • </template>
                        <template v-if="getLookupItemSize(it)">Size: {{ getLookupItemSize(it) }}</template>
                      </div>
                      <div v-if="getLookupItemVoucher(it)" class="item-voucher-inline">
                        Voucher {{ getLookupItemVoucher(it).code || 'đã áp dụng' }}
                        <template v-if="getLookupItemVoucher(it).discount > 0"> • -{{ formatMoney(getLookupItemVoucher(it).discount) }}</template>
                      </div>
                    </td>
                    <td>{{ it.soLuong }}</td>
                    <td>{{ formatMoney(it.giaBan) }}</td>
                    <td class="item-total">{{ formatMoney(it.thanhTien) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </article>
    </section>
  </CustomerPageLayout>
</template>

<style scoped>
.lookup-shell {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 0 12px;
}

.lookup-card {
  width: 100%;
  max-width: 860px;
  margin: 0 auto;
  border-radius: 20px;
  border: 1px solid #f3d6da;
  background: #fff;
  padding: 28px 32px;
  box-shadow: 0 20px 50px rgba(143, 17, 33, 0.07);
}

/* ── Header ── */
.lookup-header h1 {
  margin: 0;
  font-size: 22px;
  color: #111827;
}

.lookup-header p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 14px;
}

/* ── Search form ── */
.lookup-form {
  margin-top: 20px;
}

.input-row {
  display: flex;
  gap: 10px;
}

.lookup-input {
  flex: 1;
  border-radius: 12px;
  border: 1.5px solid #d1d5db;
  padding: 12px 16px;
  font-size: 15px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.lookup-input:focus {
  outline: none;
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.12);
}

.btn-lookup {
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #dc2626 0%, #991b1b 100%);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  padding: 12px 24px;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.2s;
}

.btn-lookup:hover { opacity: 0.92; }
.btn-lookup:disabled { opacity: 0.6; cursor: not-allowed; }

.lookup-error {
  margin: 14px 0 0;
  color: #dc2626;
  font-weight: 600;
  font-size: 14px;
}

/* ── Result ── */
.lookup-result {
  margin-top: 24px;
  display: grid;
  gap: 20px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.result-header h2 {
  margin: 0;
  font-size: 18px;
  color: #1f2937;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 5px 14px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 13px;
}

.status-pill.tone-green { background: #dcfce7; color: #166534; }
.status-pill.tone-blue { background: #dbeafe; color: #1e40af; }
.status-pill.tone-amber { background: #fef3c7; color: #92400e; }
.status-pill.tone-red { background: #fee2e2; color: #991b1b; }

/* ── Stepper ── */
.stepper-card {
  border: 1px solid #f3d6da;
  border-radius: 16px;
  background: linear-gradient(180deg, #fff 0%, #fff8f8 100%);
  padding: 20px 16px 16px;
  position: relative;
}

.stepper-bar {
  position: relative;
  height: 6px;
  border-radius: 999px;
  background: #fecdd3;
  overflow: hidden;
  margin-bottom: 20px;
}

.stepper-bar-fill {
  position: absolute;
  top: 0; left: 0;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #ef4444 0%, #b91c1c 100%);
  transition: width 0.4s ease;
}

.stepper-bar-fill.terminal {
  background: linear-gradient(90deg, #9ca3af 0%, #6b7280 100%);
}

.stepper-steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
  text-align: center;
}

.stepper-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stepper-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4b5563;
  background: #ffffff;
  border: 1.5px solid #9ca3af;
  transition: all 0.3s;
}

.stepper-step.done .stepper-icon,
.stepper-step.active .stepper-icon {
  color: #111827;
  background: #ffffff;
  border-color: #111827;
}

.stepper-step.dimmed .stepper-icon {
  opacity: 0.4;
}

.stepper-label {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  line-height: 1.3;
}

.stepper-step.done .stepper-label,
.stepper-step.active .stepper-label {
  color: #111827;
}

.stepper-time {
  font-size: 10px;
  color: #6b7280;
  min-height: 14px;
}

.terminal-badge {
  margin-top: 12px;
  text-align: center;
  padding: 6px 16px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 13px;
}

.terminal-badge.tone-red { background: #fee2e2; color: #991b1b; }
.terminal-badge.tone-amber { background: #fef3c7; color: #92400e; }

/* ── GHN sub-events ── */
.ghn-card {
  border: 1px solid #e0e7ff;
  border-radius: 14px;
  background: #f8faff;
  padding: 16px;
}

.ghn-card h3 {
  margin: 0 0 12px;
  font-size: 15px;
  color: #1e40af;
}

.ghn-timeline {
  display: grid;
  gap: 10px;
}

.ghn-event {
  display: grid;
  grid-template-columns: 12px 1fr;
  gap: 10px;
  align-items: flex-start;
}

.ghn-dot {
  margin-top: 6px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #3b82f6;
}

.ghn-label {
  margin: 0;
  font-weight: 600;
  font-size: 14px;
  color: #1f2937;
}

.ghn-note {
  margin: 2px 0 0;
  font-size: 13px;
  color: #6b7280;
}

/* ── Info grid ── */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 14px;
  border-radius: 12px;
  background: #f9fafb;
  border: 1px solid #f1f5f9;
}

.info-item.total {
  border-color: #fee2e2;
  background: #fff5f5;
}

.info-label {
  font-size: 12px;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.info-item strong {
  font-size: 15px;
  color: #1f2937;
}

.info-item .discount {
  color: #dc2626;
}

.info-item.total strong {
  color: #991b1b;
  font-size: 16px;
}

/* ── Items ── */
.items-card {
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.items-card h3 {
  margin: 0;
  padding: 14px 16px;
  font-size: 15px;
  background: #fef2f2;
  border-bottom: 1px solid #fecdd3;
}

.items-scroll {
  overflow-x: auto;
}

.items-table {
  width: 100%;
  min-width: 480px;
  border-collapse: collapse;
}

.items-table th,
.items-table td {
  padding: 10px 14px;
  border-bottom: 1px solid #f1f5f9;
  text-align: left;
  font-size: 14px;
}

.items-table thead {
  background: #f9fafb;
}

.items-table th {
  font-weight: 600;
  color: #6b7280;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.item-name {
  font-weight: 600;
  color: #1f2937;
}

.item-name-main {
  font-weight: 700;
}

.item-meta-inline {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.item-voucher-inline {
  margin-top: 4px;
  color: #b91c1c;
  font-size: 12px;
  font-weight: 700;
}

.item-total {
  font-weight: 700;
  color: #991b1b;
}

/* ── Mobile ── */
@media (max-width: 640px) {
  .lookup-card {
    padding: 14px 12px;
    border-radius: 12px;
  }

  .lookup-result {
    gap: 14px;
  }

  .lookup-header h1 {
    font-size: 18px;
  }

  .lookup-header p {
    font-size: 13px;
  }

  .input-row {
    flex-direction: column;
  }

  .btn-lookup {
    width: 100%;
  }

  .stepper-card {
    padding: 14px 12px;
  }

  .stepper-bar {
    display: none;
  }

  .stepper-steps {
    display: grid;
    grid-template-columns: 1fr;
    gap: 12px;
    text-align: left;
    position: relative;
  }

  .stepper-step {
    display: grid;
    grid-template-columns: 28px 1fr;
    column-gap: 10px;
    row-gap: 2px;
    align-items: flex-start;
    position: relative;
  }

  .stepper-step:not(:last-child)::after {
    content: "";
    position: absolute;
    left: 13px;
    top: 30px;
    bottom: -14px;
    width: 1.5px;
    background: #d1d5db;
  }

  .stepper-step.done:not(:last-child)::after,
  .stepper-step.active:not(:last-child)::after {
    background: #111827;
  }

  .stepper-icon {
    width: 28px;
    height: 28px;
    grid-column: 1;
    grid-row: 1 / span 2;
  }

  .stepper-label {
    font-size: 12px;
    grid-column: 2;
    margin-top: 2px;
  }

  .stepper-time {
    font-size: 10px;
    min-height: 0;
    grid-column: 2;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .items-table {
    min-width: 420px;
  }
}
</style>
