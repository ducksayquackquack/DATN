<template>
  <div class="voucher-selector">
    <div class="voucher-actions">
      <button class="btn-select-voucher" @click="openVoucherModal">
        <Gift :size="20" :stroke-width="2" class="icon" />
        <span>{{ selectedVoucher ? 'Đổi phiếu giảm giá' : 'Chọn phiếu giảm giá' }}</span>
      </button>
    </div>

    <div v-if="selectedVoucher" class="applied-voucher">
      <div class="voucher-card active">
        <div class="voucher-header">
          <Ticket :size="22" :stroke-width="2" class="voucher-icon" />
          <div class="voucher-info">
            <h4>{{ selectedVoucher.tenPhieuGiamGia }}</h4>
            <p class="voucher-code">{{ selectedVoucher.maPhieuGiamGia }}</p>
          </div>
          <button class="btn-remove" @click="removeVoucher" title="Bỏ áp dụng">×</button>
        </div>
        <div class="voucher-body">
          <div class="discount-amount">
            Giảm: <strong>{{ formatCurrency(currentDiscount) }}</strong>
          </div>
          <div class="voucher-desc">{{ selectedVoucher.moTa || 'Không có mô tả' }}</div>
        </div>
      </div>
    </div>

    <div v-if="showVoucherModal" class="modal-overlay" @click.self="showVoucherModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Chọn phiếu giảm giá</h3>
          <button class="btn-close" @click="showVoucherModal = false">×</button>
        </div>

        <div class="modal-body">
          <div v-if="loading" class="loading">Đang tải...</div>

          <div v-else-if="modalVouchers.length === 0" class="empty-state">
            <p>Không có phiếu giảm giá đang hoạt động</p>
          </div>

          <div v-else class="voucher-list">
            <div
              v-for="voucher in modalVouchers"
              :key="voucher.id"
              class="voucher-item"
              :class="{
                selected: selectedVoucher?.id === voucher.id,
                disabled: !voucher.isApplicable
              }"
              @click="selectVoucher(voucher)"
            >
              <div class="voucher-item-header">
                <h4>{{ voucher.tenPhieuGiamGia }}</h4>
                <span class="discount-badge" :class="{ muted: !voucher.isApplicable }">
                  {{ voucher.isApplicable ? `-${formatCurrency(voucher.discountAmount)}` : 'Chưa đủ điều kiện' }}
                </span>
              </div>
              <div class="voucher-item-body">
                <p class="voucher-code">Mã: {{ voucher.maPhieuGiamGia }}</p>
                <p class="voucher-desc">{{ voucher.moTa || 'Không có mô tả' }}</p>
                <div class="voucher-conditions">
                  <span v-if="voucher.hoaDonToiThieu > 0">Đơn tối thiểu: {{ formatCurrency(voucher.hoaDonToiThieu) }}</span>
                  <span v-if="voucher.soTienGiamToiDa > 0">Giảm tối đa: {{ formatCurrency(voucher.soTienGiamToiDa) }}</span>
                </div>
                <p v-if="!voucher.isApplicable && voucher.amountNeeded > 0" class="voucher-note">
                  Cần mua thêm {{ formatCurrency(voucher.amountNeeded) }} để áp dụng
                </p>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showVoucherModal = false">Đóng</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { Gift, Ticket } from 'lucide-vue-next'
import {
  getActiveVouchers,
  getAllVouchers,
  calculateVoucherDiscount,
  isVoucherApplicable
} from '@/services/khuyenMaiService'

const props = defineProps({
  subtotal: {
    type: Number,
    required: true
  },
  customerId: {
    type: Number,
    default: null
  },
  autoSelect: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:voucher', 'discount-changed'])

const vouchers = ref([])
const selectedVoucher = ref(null)
const showVoucherModal = ref(false)
const loading = ref(false)
const pollingInterval = ref(null)

// Computed
const currentDiscount = computed(() => {
  if (!selectedVoucher.value) return 0
  return calculateVoucherDiscount(selectedVoucher.value, props.subtotal)
})

const isVoucherActiveNow = (voucher) => {
  if (!voucher) return false

  const now = new Date()
  const start = voucher.ngayBatDau ? new Date(voucher.ngayBatDau) : null
  const end = voucher.ngayKetThuc ? new Date(voucher.ngayKetThuc) : null

  if (start && !Number.isNaN(start.getTime()) && now < start) return false
  if (end && !Number.isNaN(end.getTime()) && now > end) return false

  const remaining = voucher.soLuongSuDung
  if (remaining !== null && remaining !== undefined && Number(remaining) <= 0) return false

  return true
}

const modalVouchers = computed(() => {
  return vouchers.value
    .filter(isVoucherActiveNow)
    .map(v => {
      const minOrder = Number(v.hoaDonToiThieu) || 0
      const amountNeeded = Math.max(minOrder - (Number(props.subtotal) || 0), 0)
      const applicable = isVoucherApplicable(v, props.subtotal, props.customerId)
      return {
        ...v,
        isApplicable: applicable,
        amountNeeded,
        discountAmount: calculateVoucherDiscount(v, props.subtotal)
      }
    })
    .sort((a, b) => {
      if (a.isApplicable !== b.isApplicable) return Number(b.isApplicable) - Number(a.isApplicable)
      return b.discountAmount - a.discountAmount
    })
})

// Methods
const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(value || 0)
}

const extractVoucherList = (response) => {
  const payload = response?.data
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

const loadVouchers = async () => {
  try {
    loading.value = true
    let newVouchers = []

    try {
      const activeRes = await getActiveVouchers()
      newVouchers = extractVoucherList(activeRes)
    } catch (activeError) {
      console.warn('[VoucherSelector] Failed to load /active vouchers, falling back to /api/phieu-giam-gia', activeError)
    }

    if (newVouchers.length === 0) {
      const fallbackRes = await getAllVouchers()
      newVouchers = extractVoucherList(fallbackRes)
    }
    
    vouchers.value = newVouchers

    if (selectedVoucher.value) {
      const refreshed = newVouchers.find(v => v.id === selectedVoucher.value.id)
      if (refreshed) {
        selectedVoucher.value = refreshed
      }

      if (!isVoucherApplicable(selectedVoucher.value, props.subtotal, props.customerId)) {
        removeVoucher()
      } else {
        emit('discount-changed', currentDiscount.value)
      }
    }
  } catch (error) {
    console.error('Error loading vouchers:', error)
    vouchers.value = []
  } finally {
    loading.value = false
  }
}

const openVoucherModal = async () => {
  showVoucherModal.value = true
  if (!loading.value && vouchers.value.length === 0) {
    await loadVouchers()
  }
}

const selectVoucher = (voucher) => {
  if (!voucher || !voucher.isApplicable) return
  selectedVoucher.value = voucher
  showVoucherModal.value = false
  emit('update:voucher', voucher)
  emit('discount-changed', currentDiscount.value)
}

const removeVoucher = () => {
  selectedVoucher.value = null
  emit('update:voucher', null)
  emit('discount-changed', 0)
}

// Watch subtotal changes
watch(() => props.subtotal, (newSubtotal) => {
  if (selectedVoucher.value) {
    if (!isVoucherApplicable(selectedVoucher.value, newSubtotal, props.customerId)) {
      removeVoucher()
    } else {
      emit('discount-changed', currentDiscount.value)
    }
  }
})

// Polling for new vouchers (every 30 seconds)
const startPolling = () => {
  pollingInterval.value = setInterval(() => {
    loadVouchers()
  }, 30000) // 30 seconds
}

const stopPolling = () => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
    pollingInterval.value = null
  }
}

// Lifecycle
onMounted(() => {
  loadVouchers()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.voucher-selector {
  margin: 16px 0;
}

.voucher-actions {
  margin-bottom: 12px;
}

.applied-voucher {
  margin-bottom: 16px;
}

.voucher-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  color: #111111;
  border: 1px solid #e5e5e5;
}

.voucher-card.active {
  border: 2px solid #dc2626;
}

.voucher-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.voucher-icon {
  color: #dc2626;
  flex-shrink: 0;
}

.voucher-info {
  flex: 1;
}

.voucher-info h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.voucher-code {
  margin: 4px 0 0;
  font-size: 12px;
  color: #525252;
}

.btn-remove {
  background: #ffffff;
  border: 1px solid #dc2626;
  color: #dc2626;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: 0.2s;
}

.btn-remove:hover {
  background: #dc2626;
  color: #ffffff;
}

.voucher-body {
  border-top: 1px solid #efefef;
  padding-top: 12px;
}

.discount-amount {
  font-size: 18px;
  margin-bottom: 8px;
}

.discount-amount strong {
  color: #dc2626;
  font-size: 20px;
}

.voucher-desc {
  font-size: 13px;
  color: #404040;
}

.btn-select-voucher {
  width: 100%;
  padding: 12px 16px;
  background: #ffffff;
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: 0.2s;
}

.btn-select-voucher:hover {
  background: #fff5f5;
  border-color: #dc2626;
  color: #dc2626;
}

.btn-select-voucher .icon {
  color: #dc2626;
  flex-shrink: 0;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.btn-close {
  background: none;
  border: none;
  font-size: 32px;
  color: #9ca3af;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: 0.2s;
}

.btn-close:hover {
  background: #f3f4f6;
  color: #374151;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.loading,
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
}

.voucher-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.voucher-item {
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: 0.2s;
}

.voucher-item:hover {
  border-color: #dc2626;
  background: #fff5f5;
}

.voucher-item.selected {
  border-color: #dc2626;
  background: #fff1f2;
}

.voucher-item.disabled {
  opacity: 0.75;
  cursor: not-allowed;
}

.voucher-item.disabled:hover {
  border-color: #e5e7eb;
  background: #ffffff;
}

.voucher-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.voucher-item-header h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.discount-badge {
  background: #dc2626;
  color: white;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.discount-badge.muted {
  background: #f5f5f5;
  color: #525252;
}

.voucher-item-body {
  font-size: 13px;
  color: #6b7280;
}

.voucher-item-body .voucher-code {
  margin: 4px 0;
  font-weight: 500;
  color: #374151;
}

.voucher-item-body .voucher-desc {
  margin: 8px 0;
}

.voucher-note {
  margin: 8px 0 0;
  color: #dc2626;
  font-size: 12px;
  font-weight: 600;
}

.voucher-conditions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  font-size: 12px;
}

.voucher-conditions span {
  background: #f3f4f6;
  padding: 4px 8px;
  border-radius: 4px;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: 0.2s;
  border: none;
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

.btn-primary {
  background: #dc2626;
  color: white;
}
.btn-primary:hover {
  filter: brightness(1.1);
}
</style>
