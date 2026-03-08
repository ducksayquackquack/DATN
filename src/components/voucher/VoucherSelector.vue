<template>
  <div class="voucher-selector">
    <!-- Current Applied Voucher -->
    <div v-if="selectedVoucher" class="applied-voucher">
      <div class="voucher-card active">
        <div class="voucher-header">
          <span class="voucher-icon">🎫</span>
          <div class="voucher-info">
            <h4>{{ selectedVoucher.tenPhieuGiamGia }}</h4>
            <p class="voucher-code">{{ selectedVoucher.maPhieuGiamGia }}</p>
          </div>
          <button class="btn-remove" @click="removeVoucher" title="Bỏ áp dụng">
            ×
          </button>
        </div>
        <div class="voucher-body">
          <div class="discount-amount">
            Giảm: <strong>{{ formatCurrency(currentDiscount) }}</strong>
          </div>
          <div class="voucher-desc">{{ selectedVoucher.moTa }}</div>
        </div>
      </div>
    </div>

    <!-- No Voucher Applied -->
    <div v-else class="no-voucher">
      <button class="btn-select-voucher" @click="showVoucherModal = true">
        <span class="icon">🎁</span>
        <span>Chọn phiếu giảm giá</span>
      </button>
    </div>

    <!-- Voucher Suggestions -->
    <div v-if="suggestions.length > 0" class="suggestions">
      <h5 class="suggestions-title">💡 Gợi ý mua thêm để được giảm giá tốt hơn:</h5>
      <div class="suggestion-list">
        <div 
          v-for="(suggestion, idx) in suggestions" 
          :key="idx"
          class="suggestion-card"
          :class="{ 'worth-it': suggestion.worthIt }"
        >
          <div class="suggestion-header">
            <span class="badge">{{ suggestion.voucher.tenPhieuGiamGia }}</span>
            <span v-if="suggestion.worthIt" class="badge-hot">🔥 Đáng mua</span>
          </div>
          <div class="suggestion-body">
            <p>
              Mua thêm <strong>{{ formatCurrency(suggestion.amountNeeded) }}</strong>
              để được giảm <strong>{{ formatCurrency(suggestion.potentialDiscount) }}</strong>
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Voucher Selection Modal -->
    <div v-if="showVoucherModal" class="modal-overlay" @click.self="showVoucherModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Chọn phiếu giảm giá</h3>
          <button class="btn-close" @click="showVoucherModal = false">×</button>
        </div>
        
        <div class="modal-body">
          <div v-if="loading" class="loading">Đang tải...</div>
          
          <div v-else-if="applicableVouchers.length === 0" class="empty-state">
            <p>Không có phiếu giảm giá khả dụng cho đơn hàng này</p>
          </div>
          
          <div v-else class="voucher-list">
            <div 
              v-for="voucher in applicableVouchers" 
              :key="voucher.id"
              class="voucher-item"
              :class="{ 'selected': selectedVoucher?.id === voucher.id }"
              @click="selectVoucher(voucher)"
            >
              <div class="voucher-item-header">
                <h4>{{ voucher.tenPhieuGiamGia }}</h4>
                <span class="discount-badge">
                  -{{ formatCurrency(voucher.discountAmount) }}
                </span>
              </div>
              <div class="voucher-item-body">
                <p class="voucher-code">Mã: {{ voucher.maPhieuGiamGia }}</p>
                <p class="voucher-desc">{{ voucher.moTa }}</p>
                <div class="voucher-conditions">
                  <span v-if="voucher.hoaDonToiThieu > 0">
                    Đơn tối thiểu: {{ formatCurrency(voucher.hoaDonToiThieu) }}
                  </span>
                  <span v-if="voucher.soTienGiamToiDa > 0">
                    Giảm tối đa: {{ formatCurrency(voucher.soTienGiamToiDa) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showVoucherModal = false">
            Đóng
          </button>
        </div>
      </div>
    </div>

    <!-- Better Voucher Notification Modal -->
    <div v-if="showBetterVoucherModal" class="modal-overlay">
      <div class="modal-content notification-modal">
        <div class="modal-header">
          <h3>🎉 Có phiếu giảm giá tốt hơn!</h3>
        </div>
        
        <div class="modal-body">
          <div class="comparison">
            <div class="comparison-item current">
              <h5>Phiếu hiện tại</h5>
              <p class="voucher-name">{{ selectedVoucher?.tenPhieuGiamGia || 'Không có' }}</p>
              <p class="discount">Giảm: {{ formatCurrency(currentDiscount) }}</p>
            </div>
            
            <div class="comparison-arrow">→</div>
            
            <div class="comparison-item new">
              <h5>Phiếu mới</h5>
              <p class="voucher-name">{{ betterVoucher?.tenPhieuGiamGia }}</p>
              <p class="discount highlight">Giảm: {{ formatCurrency(betterVoucherDiscount) }}</p>
              <p class="difference">
                Tốt hơn: +{{ formatCurrency(betterVoucherDiscount - currentDiscount) }}
              </p>
            </div>
          </div>
          
          <p class="notification-message">
            Bạn có muốn áp dụng phiếu giảm giá mới này không?
          </p>
        </div>
        
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="rejectBetterVoucher">
            Giữ phiếu cũ
          </button>
          <button class="btn btn-primary" @click="acceptBetterVoucher">
            Áp dụng phiếu mới
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import {
  getActiveVouchers,
  calculateVoucherDiscount,
  isVoucherApplicable,
  findBestVoucher,
  getVoucherSuggestions,
  compareVouchers
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
    default: true
  }
})

const emit = defineEmits(['update:voucher', 'discount-changed'])

const vouchers = ref([])
const selectedVoucher = ref(null)
const showVoucherModal = ref(false)
const showBetterVoucherModal = ref(false)
const betterVoucher = ref(null)
const loading = ref(false)
const pollingInterval = ref(null)

// Computed
const currentDiscount = computed(() => {
  if (!selectedVoucher.value) return 0
  return calculateVoucherDiscount(selectedVoucher.value, props.subtotal)
})

const betterVoucherDiscount = computed(() => {
  if (!betterVoucher.value) return 0
  return calculateVoucherDiscount(betterVoucher.value, props.subtotal)
})

const applicableVouchers = computed(() => {
  return vouchers.value
    .filter(v => isVoucherApplicable(v, props.subtotal, props.customerId))
    .map(v => ({
      ...v,
      discountAmount: calculateVoucherDiscount(v, props.subtotal)
    }))
    .sort((a, b) => b.discountAmount - a.discountAmount)
})

const suggestions = computed(() => {
  return getVoucherSuggestions(vouchers.value, props.subtotal, props.customerId)
})

// Methods
const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(value || 0)
}

const loadVouchers = async () => {
  try {
    loading.value = true
    const response = await getActiveVouchers()
    const newVouchers = response.data || []
    
    // Check if there's a new better voucher
    if (selectedVoucher.value && newVouchers.length > 0) {
      checkForBetterVoucher(newVouchers)
    }
    
    vouchers.value = newVouchers
    
    // Auto-select best voucher if enabled and no voucher selected
    if (props.autoSelect && !selectedVoucher.value && applicableVouchers.value.length > 0) {
      const best = findBestVoucher(newVouchers, props.subtotal, props.customerId)
      if (best) {
        selectVoucher(best)
      }
    }
  } catch (error) {
    console.error('Error loading vouchers:', error)
  } finally {
    loading.value = false
  }
}

const checkForBetterVoucher = (newVouchers) => {
  const best = findBestVoucher(newVouchers, props.subtotal, props.customerId)
  
  if (!best || !selectedVoucher.value) return
  
  const comparison = compareVouchers(selectedVoucher.value, best, props.subtotal)
  
  // Show notification if new voucher is significantly better (>5% improvement)
  if (comparison.isBetter && comparison.difference > comparison.currentDiscount * 0.05) {
    betterVoucher.value = best
    showBetterVoucherModal.value = true
  }
}

const selectVoucher = (voucher) => {
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

const acceptBetterVoucher = () => {
  if (betterVoucher.value) {
    selectVoucher(betterVoucher.value)
  }
  showBetterVoucherModal.value = false
  betterVoucher.value = null
}

const rejectBetterVoucher = () => {
  showBetterVoucherModal.value = false
  betterVoucher.value = null
}

// Watch subtotal changes
watch(() => props.subtotal, (newSubtotal) => {
  if (selectedVoucher.value) {
    // Check if current voucher is still applicable
    if (!isVoucherApplicable(selectedVoucher.value, newSubtotal, props.customerId)) {
      removeVoucher()
    } else {
      emit('discount-changed', currentDiscount.value)
    }
  }
  
  // Auto-select best voucher when subtotal changes
  if (props.autoSelect && !selectedVoucher.value && applicableVouchers.value.length > 0) {
    const best = findBestVoucher(vouchers.value, newSubtotal, props.customerId)
    if (best) {
      selectVoucher(best)
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

.applied-voucher {
  margin-bottom: 16px;
}

.voucher-card {
  background: linear-gradient(135deg, #3fb68c 0%, #6fcf88 100%);
  border-radius: 12px;
  padding: 16px;
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.voucher-card.active {
  border: 2px solid #fbbf24;
}

.voucher-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.voucher-icon {
  font-size: 32px;
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
  opacity: 0.9;
}

.btn-remove {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
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
  background: rgba(255, 255, 255, 0.3);
}

.voucher-body {
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  padding-top: 12px;
}

.discount-amount {
  font-size: 18px;
  margin-bottom: 8px;
}

.discount-amount strong {
  color: #fbbf24;
  font-size: 20px;
}

.voucher-desc {
  font-size: 13px;
  opacity: 0.9;
}

.no-voucher {
  margin-bottom: 16px;
}

.btn-select-voucher {
  width: 100%;
  padding: 16px;
  background: #f3f4f6;
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
  background: #e5e7eb;
  border-color: #9ca3af;
  color: #374151;
}

.btn-select-voucher .icon {
  font-size: 24px;
}

.suggestions {
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 12px;
  padding: 16px;
  margin-top: 16px;
}

.suggestions-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #92400e;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.suggestion-card {
  background: white;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #fde68a;
}

.suggestion-card.worth-it {
  border-color: #f59e0b;
  background: #fef3c7;
}

.suggestion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.badge {
  background: #dbeafe;
  color: #1e40af;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.badge-hot {
  background: #fee2e2;
  color: #991b1b;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.suggestion-body p {
  margin: 0;
  font-size: 13px;
  color: #374151;
}

.suggestion-body strong {
  color: #dc2626;
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
  z-index: 1000;
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
  border-color: #667eea;
  background: #f9fafb;
}

.voucher-item.selected {
  border-color: #667eea;
  background: #eef2ff;
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  filter: brightness(1.1);
}

/* Notification Modal */
.notification-modal {
  max-width: 500px;
}

.comparison {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.comparison-item {
  flex: 1;
  padding: 16px;
  border-radius: 12px;
  text-align: center;
}

.comparison-item.current {
  background: #f3f4f6;
  border: 2px solid #d1d5db;
}

.comparison-item.new {
  background: #dcfce7;
  border: 2px solid #22c55e;
}

.comparison-item h5 {
  margin: 0 0 8px;
  font-size: 12px;
  text-transform: uppercase;
  color: #6b7280;
  font-weight: 600;
}

.comparison-item .voucher-name {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.comparison-item .discount {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #374151;
}

.comparison-item .discount.highlight {
  color: #16a34a;
  font-size: 18px;
}

.comparison-item .difference {
  margin: 8px 0 0;
  font-size: 13px;
  color: #16a34a;
  font-weight: 600;
}

.comparison-arrow {
  font-size: 24px;
  color: #9ca3af;
}

.notification-message {
  text-align: center;
  font-size: 15px;
  color: #374151;
  margin: 0;
}
</style>
