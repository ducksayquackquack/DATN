<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { FileText, Shirt, Tag, Calendar, ChevronLeft, ChevronRight } from 'lucide-vue-next'

import { getAllHoaDon } from '../../services/hoaDonService'
import { getAllSanPham } from '../../services/sanPhamService'
import { getAllKhuyenMai } from '../../services/khuyenMaiService'

import logo from '../../assets/img/logo/new logo.png?url'

// HD jacket product images (no people) from Unsplash
const jacket1 = 'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=1920&q=90&fit=crop&auto=format'
const jacket2 = 'https://images.unsplash.com/photo-1542272604-787c3835535d?w=1920&q=90&fit=crop&auto=format'
const jacket3 = 'https://images.unsplash.com/photo-1548126032-079a0fb0099d?w=1920&q=90&fit=crop&auto=format'

const router = useRouter()
const go = (path) => router.push(path)

const now = ref(new Date())
const loading = ref(false)
const activeSlide = ref(0)

const invoices = ref([])
const products = ref([])
const campaigns = ref([])

const slides = [
  {
    img: jacket1,
    kicker: 'Bộ sưu tập',
    title: 'Bomber Da Lộn DirtyWave',
    desc: 'Chất liệu da lộn cao cấp, form dáng chuẩn — sản phẩm chủ lực mùa này.'
  },
  {
    img: jacket2,
    kicker: 'Xu hướng',
    title: 'Hoodie Kéo Khoá In Hình',
    desc: 'Thiết kế trẻ trung, dễ phối — bán chạy nhất nhóm sản phẩm hoodie.'
  },
  {
    img: jacket3,
    kicker: 'Mới về',
    title: 'Coach Jacket Giả Da',
    desc: 'Khoác nhẹ, phong cách city — theo dõi đơn hàng nhanh ngay tại đây.'
  }
]

let slideTimer = null

const resetSlideTimer = () => {
  if (slideTimer) clearInterval(slideTimer)
  slideTimer = setInterval(() => {
    activeSlide.value = (activeSlide.value + 1) % slides.length
  }, 4500)
}

const nextSlide = () => {
  activeSlide.value = (activeSlide.value + 1) % slides.length
  resetSlideTimer()
}

const prevSlide = () => {
  activeSlide.value = (activeSlide.value - 1 + slides.length) % slides.length
  resetSlideTimer()
}

const goToSlide = (i) => {
  activeSlide.value = i
  resetSlideTimer()
}

const normalizeArray = (res) => {
  const payload = res?.data
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.data)) return payload.data
  if (Array.isArray(payload?.content)) return payload.content
  if (Array.isArray(payload?.data?.content)) return payload.data.content
  return []
}

const toNumber = (value) => {
  const n = Number(value)
  return Number.isFinite(n) ? n : 0
}

const parseDate = (value) => {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

const formatDate = (value) => {
  const date = parseDate(value)
  return date ? date.toLocaleString('vi-VN') : '-'
}

const formatCurrency = (value) => `${new Intl.NumberFormat('vi-VN').format(toNumber(value))}₫`

const invoiceTotal = (item) => {
  return toNumber(item?.tongTien) || toNumber(item?.thanhTien) || toNumber(item?.tongTienThanhToan) || 0
}

const invoiceStatus = (item) => {
  const raw = String(item?.orderStatusName || item?.trangThai || item?.orderStatusCode || '').toUpperCase()
  if (raw.includes('HOAN_THANH') || raw.includes('HOÀN THÀNH')) return { label: 'Hoàn thành', tone: 'success' }
  if (raw.includes('HUY') || raw.includes('HỦY')) return { label: 'Đã hủy', tone: 'danger' }
  if (raw.includes('DANG_GIAO') || raw.includes('ĐANG GIAO')) return { label: 'Đang giao', tone: 'processing' }
  if (raw.includes('CHO') || raw.includes('CHỜ')) return { label: 'Chờ xử lý', tone: 'pending' }
  return { label: item?.orderStatusName || item?.trangThai || 'Không xác định', tone: 'pending' }
}

const productStock = (item) => {
  const details = Array.isArray(item?.sanPhamChiTiets) ? item.sanPhamChiTiets : []
  if (details.length) {
    return details.reduce((sum, row) => sum + toNumber(row?.soLuong), 0)
  }
  return toNumber(item?.soLuong || item?.ton || 0)
}

const isActivePromotion = (item) => {
  const status = String(item?.trangThai || '').toLowerCase()
  if (status.includes('ngừng') || status.includes('hủy') || status.includes('inactive')) return false
  const start = parseDate(item?.ngayBatDau)
  const end = parseDate(item?.ngayKetThuc)
  const n = now.value
  if (start && n < start) return false
  if (end && n > end) return false
  return true
}

const quickLinks = [
  { title: 'Hóa đơn', desc: 'Xử lý đơn theo trạng thái', path: '/employee/hoa-don/list', icon: FileText },
  { title: 'Sản phẩm', desc: 'Xem tồn kho theo nhóm', path: '/employee/san-pham/list', icon: Shirt },
  { title: 'Mở/Kết ca', desc: 'Thao tác ca làm việc', path: '/employee/giao-ca', icon: Calendar },
  { title: 'Khuyến mãi', desc: 'Theo dõi ưu đãi đang chạy', path: '/employee/khuyen-mai/list', icon: Tag }
]

const pendingOrders = computed(() => {
  return invoices.value.filter((item) => {
    const label = invoiceStatus(item).label.toLowerCase()
    return label.includes('chờ') || label.includes('đang giao')
  })
})

const lowStockProducts = computed(() => {
  return products.value
    .map((item) => ({
      id: item?.id,
      maSanPham: item?.maSanPham || `SP-${item?.id || 'N/A'}`,
      tenSanPham: item?.tenSanPham || 'Sản phẩm',
      ton: productStock(item)
    }))
    .filter((item) => item.ton <= 12)
    .sort((a, b) => a.ton - b.ton)
    .slice(0, 6)
})

const activePromos = computed(() => {
  return campaigns.value
    .filter(isActivePromotion)
    .slice(0, 5)
    .map((item) => ({
      id: item?.id,
      name: item?.tenKhuyenMai || item?.maKhuyenMai || 'Khuyến mãi',
      end: item?.ngayKetThuc
    }))
})

const pendingOrdersList = computed(() => {
  return [...pendingOrders.value]
    .sort((a, b) => {
      const aTime = parseDate(a?.ngayTao || a?.createdAt)?.getTime() || 0
      const bTime = parseDate(b?.ngayTao || b?.createdAt)?.getTime() || 0
      return bTime - aTime
    })
    .slice(0, 8)
    .map((item) => {
      const status = invoiceStatus(item)
      return {
        id: item?.id,
        maHoaDon: item?.maHoaDon || `HD-${item?.id || 'N/A'}`,
        customer: item?.tenKhachHang || item?.hoTenKhachHang || 'Khách lẻ',
        total: invoiceTotal(item),
        status: status.label,
        tone: status.tone,
        createdAt: item?.ngayTao || item?.createdAt || item?.ngayDat
      }
    })
})

const syncData = async () => {
  loading.value = true

  const [hoaDonRes, sanPhamRes, khuyenMaiRes] = await Promise.allSettled([
    getAllHoaDon(),
    getAllSanPham(),
    getAllKhuyenMai()
  ])

  if (hoaDonRes.status === 'fulfilled') invoices.value = normalizeArray(hoaDonRes.value)
  if (sanPhamRes.status === 'fulfilled') products.value = normalizeArray(sanPhamRes.value)
  if (khuyenMaiRes.status === 'fulfilled') campaigns.value = normalizeArray(khuyenMaiRes.value)

  loading.value = false
}

onMounted(async () => {
  await syncData()
  resetSlideTimer()
})

onUnmounted(() => {
  if (slideTimer) clearInterval(slideTimer)
})
</script>

<template>
  <div class="ew-page">
    <section class="ew-hero">
      <div class="ew-hero-inner">
        <div class="ew-hero-top-row">
          <div class="ew-brand">
            <img class="ew-logo" :src="logo" alt="DirtyWave" />
            <div>
              <div class="ew-brand-name">Trang chủ ca làm việc</div>
            </div>
          </div>
        </div>

        <div class="ew-hero-divider"></div>

        <h1 class="ew-hero-title">Xử lý đơn nhanh, giữ nhịp vận hành trong ca.</h1>
        <p class="ew-hero-desc">
          Theo dõi đơn hàng, tồn kho và khuyến mãi theo thời gian thực.
          Tất cả thao tác ca làm việc trong một màn hình.
        </p>

        <div class="ew-hero-actions">
          <button class="ew-btn ew-btn-primary" type="button" @click="go('/employee/hoa-don/list')">
            <FileText :size="14" :stroke-width="2.3" /> Xử lý hóa đơn
          </button>
          <button class="ew-btn ew-btn-outline" type="button" @click="go('/employee/giao-ca')">
            <Calendar :size="14" :stroke-width="2.3" /> Mở/Kết ca
          </button>
          <button class="ew-btn ew-btn-soft" type="button" @click="syncData" :disabled="loading">
            {{ loading ? 'Đang tải...' : 'Làm mới' }}
          </button>
        </div>

        <div class="ew-meta-row">
          <span>{{ now.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' }) }}</span>
        </div>
      </div>

      <div class="ew-banner-wrap">
        <img
          v-for="(slide, i) in slides"
          :key="i"
          :src="slide.img"
          :alt="slide.title"
          class="ew-banner-img"
          :class="{ 'ew-banner-img--active': i === activeSlide }"
        />
        <div class="ew-banner-overlay"></div>
        <div class="ew-slide-glass">
          <div class="ew-slide-kicker">{{ slides[activeSlide].kicker }}</div>
          <div class="ew-slide-title">{{ slides[activeSlide].title }}</div>
          <div class="ew-slide-desc">{{ slides[activeSlide].desc }}</div>
          <div class="ew-slide-dots">
            <button
              v-for="(_, i) in slides"
              :key="i"
              class="ew-slide-dot"
              :class="{ 'ew-slide-dot--active': i === activeSlide }"
              type="button"
              @click="goToSlide(i)"
              :aria-label="`Ảnh ${i + 1}`"
            />
          </div>
        </div>
        <button class="ew-banner-arrow ew-banner-left" type="button" @click="prevSlide" aria-label="Ảnh trước">
          <ChevronLeft :size="18" :stroke-width="2.4" />
        </button>
        <button class="ew-banner-arrow ew-banner-right" type="button" @click="nextSlide" aria-label="Ảnh sau">
          <ChevronRight :size="18" :stroke-width="2.4" />
        </button>
      </div>
    </section>

    <section class="ew-section ew-kpi-grid">
      <article class="ew-kpi-card">
        <div class="ew-kpi-label">Đơn chờ xử lý</div>
        <div class="ew-kpi-value">{{ pendingOrders.length }}</div>
        <div class="ew-kpi-sub">Cần xác nhận hoặc chuẩn bị</div>
      </article>
      <article class="ew-kpi-card">
        <div class="ew-kpi-label">Sản phẩm cận tồn</div>
        <div class="ew-kpi-value">{{ lowStockProducts.length }}</div>
        <div class="ew-kpi-sub">Ngưỡng cảnh báo ≤ 12</div>
      </article>
      <article class="ew-kpi-card">
        <div class="ew-kpi-label">Khuyến mãi đang chạy</div>
        <div class="ew-kpi-value">{{ activePromos.length }}</div>
        <div class="ew-kpi-sub">Campaign đang kích hoạt</div>
      </article>
      <article class="ew-kpi-card">
        <div class="ew-kpi-label">Tổng sản phẩm</div>
        <div class="ew-kpi-value">{{ products.length }}</div>
        <div class="ew-kpi-sub">Trong danh mục hiện tại</div>
      </article>
    </section>

    <section class="ew-section">
      <div class="ew-section-title">Lối tắt thao tác</div>
      <div class="ew-quick-grid">
        <button v-for="item in quickLinks" :key="item.path" class="ew-quick" type="button" @click="go(item.path)">
          <div class="ew-quick-icon">
            <component :is="item.icon" :size="16" :stroke-width="2.1" />
          </div>
          <div class="ew-quick-main">
            <div class="ew-quick-name">{{ item.title }}</div>
            <div class="ew-quick-desc">{{ item.desc }}</div>
          </div>
          <span class="ew-quick-arrow">›</span>
        </button>
      </div>
    </section>

    <section class="ew-section ew-main-grid">
      <article class="ew-card ew-card-lg">
        <div class="ew-card-head">
          <h2>Đơn hàng cần xử lý</h2>
          <button class="ew-link-btn" type="button" @click="go('/employee/hoa-don/list')">Mở danh sách</button>
        </div>

        <div class="ew-table-wrap">
          <table class="ew-table">
            <thead>
              <tr>
                <th>Mã đơn</th>
                <th>Khách hàng</th>
                <th>Tổng tiền</th>
                <th>Trạng thái</th>
                <th>Thời gian</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in pendingOrdersList" :key="row.id || row.maHoaDon">
                <td>{{ row.maHoaDon }}</td>
                <td>{{ row.customer }}</td>
                <td>{{ formatCurrency(row.total) }}</td>
                <td><span class="ew-status" :class="`tone-${row.tone}`">{{ row.status }}</span></td>
                <td>{{ formatDate(row.createdAt) }}</td>
              </tr>
              <tr v-if="!pendingOrdersList.length">
                <td colspan="5" class="ew-empty">Không có đơn nào cần xử lý.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <div class="ew-side-col">
        <article class="ew-card">
          <div class="ew-card-head">
            <h2>Tồn kho cảnh báo</h2>
            <button class="ew-link-btn" type="button" @click="go('/employee/san-pham/list')">Xem kho</button>
          </div>
          <div class="ew-list">
            <div v-for="item in lowStockProducts" :key="item.id" class="ew-list-item">
              <div>
                <div class="ew-list-title">{{ item.tenSanPham }}</div>
                <div class="ew-list-sub">{{ item.maSanPham }}</div>
              </div>
              <div class="ew-list-tail">{{ item.ton }}</div>
            </div>
            <div v-if="!lowStockProducts.length" class="ew-empty">Không có sản phẩm cận tồn.</div>
          </div>
        </article>

        <article class="ew-card">
          <div class="ew-card-head">
            <h2>Khuyến mãi đang chạy</h2>
            <button class="ew-link-btn" type="button" @click="go('/employee/khuyen-mai/list')">Xem tất cả</button>
          </div>
          <div class="ew-list">
            <div v-for="promo in activePromos" :key="promo.id" class="ew-list-item">
              <div>
                <div class="ew-list-title">{{ promo.name }}</div>
                <div class="ew-list-sub" v-if="promo.end">Đến {{ formatDate(promo.end) }}</div>
              </div>
            </div>
            <div v-if="!activePromos.length" class="ew-empty">Chưa có khuyến mãi nào đang chạy.</div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.ew-page {
  padding: 14px;
  min-height: calc(100vh - 70px);
  background: transparent;
  color: #101014;
}

.ew-hero {
  border-radius: 18px;
  border: 1px solid rgba(220, 24, 24, 0.18);
  background: linear-gradient(120deg, rgba(255, 255, 255, 0.97) 0%, rgba(255, 245, 246, 0.96) 50%, rgba(255, 239, 241, 0.95) 100%);
  box-shadow: 0 14px 36px rgba(5, 5, 5, 0.1);
  overflow: hidden;
}

.ew-banner-wrap {
  position: relative;
  height: 420px;
  overflow: hidden;
  border-top: 1px solid rgba(185, 28, 28, 0.2);
}

@media (min-width: 900px) {
  .ew-banner-wrap {
    height: 520px;
  }
}

@media (min-width: 1280px) {
  .ew-banner-wrap {
    height: 620px;
  }
}

.ew-banner-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center center;
  opacity: 0;
  transition: opacity 0.75s ease;
  image-rendering: auto;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
}

.ew-banner-img--active {
  opacity: 1;
}

.ew-banner-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(5, 7, 12, 0.14), rgba(5, 7, 12, 0.44));
}

.ew-banner-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  width: 36px;
  height: 36px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  background: rgba(10, 13, 20, 0.54);
  color: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
}

.ew-banner-left {
  left: 12px;
}

.ew-banner-right {
  right: 12px;
}

.ew-slide-glass {
  position: absolute;
  z-index: 2;
  left: 12px;
  right: 12px;
  bottom: 12px;
  color: #f3f4f6;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(12, 14, 22, 0.52);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding: 12px 14px;
}

.ew-slide-kicker {
  text-transform: uppercase;
  letter-spacing: 1.1px;
  font-size: 10px;
  font-weight: 700;
  color: #fca5a5;
}

.ew-slide-title {
  margin-top: 3px;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.2;
}

.ew-slide-desc {
  margin-top: 3px;
  font-size: 12px;
  color: #d1d5db;
  line-height: 1.45;
}

.ew-slide-dots {
  margin-top: 8px;
  display: flex;
  gap: 6px;
}

.ew-slide-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  border: none;
  background: rgba(255, 255, 255, 0.3);
  cursor: pointer;
  padding: 0;
  transition: all 0.35s ease;
}

.ew-slide-dot--active {
  width: 22px;
  background: #ffffff;
}

.ew-hero-inner {
  padding: 20px 24px 18px;
}

.ew-hero-top-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.ew-live-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  font-size: 11px;
  font-weight: 700;
  color: #c81f35;
  letter-spacing: 0.5px;
  white-space: nowrap;
  margin-top: 4px;
}

.ew-live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ef4444;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.2);
  animation: ew-pulse 1.8s ease-in-out infinite;
}

@keyframes ew-pulse {
  0%, 100% { box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.2); }
  50% { box-shadow: 0 0 0 7px rgba(239, 68, 68, 0.08); }
}

.ew-hero-divider {
  margin: 14px 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(185, 28, 28, 0.25), rgba(185, 28, 28, 0.06) 60%, transparent);
}

.ew-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ew-brand-eyebrow {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: #9ca3af;
  line-height: 1;
  margin-bottom: 2px;
}

.ew-logo {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  object-fit: contain;
  background: linear-gradient(135deg, #8f1123 0%, #c81f35 100%);
  padding: 6px;
  border: 1px solid rgba(185, 28, 28, 0.5);
  box-shadow: 0 4px 12px rgba(185, 28, 28, 0.35);
}

.ew-brand-kicker {
  font-size: 11px;
  letter-spacing: 1.2px;
  text-transform: uppercase;
  color: #ef4444;
  font-weight: 700;
}

.ew-brand-name {
  font-size: 26px;
  line-height: 1.1;
  font-weight: 800;
  color: #0f0f12;
}

.ew-hero-title {
  margin: 0 0 0;
  font-size: clamp(22px, 2.8vw, 36px);
  line-height: 1.18;
  font-weight: 800;
  max-width: 800px;
  color: #0a0a0f;
  letter-spacing: -0.3px;
}

.ew-hero-desc {
  margin: 10px 0 0;
  font-size: 14px;
  line-height: 1.65;
  color: #4b5563;
  max-width: 700px;
}

.ew-hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 16px;
}

.ew-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 12px;
  border: 1px solid transparent;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.ew-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.ew-btn-primary {
  color: #fff;
  background: linear-gradient(130deg, #7f1020 0%, #b7182d 45%, #ef3348 100%);
  box-shadow: 0 8px 20px rgba(176, 22, 43, 0.34);
}

.ew-btn-primary:hover:not(:disabled) {
  box-shadow: 0 12px 28px rgba(176, 22, 43, 0.44);
}

.ew-btn-outline {
  color: #7f1020;
  border-color: rgba(185, 28, 28, 0.35);
  background: linear-gradient(145deg, #fff, #fff5f6);
}

.ew-btn-soft {
  color: #9f1239;
  border-color: rgba(226, 30, 65, 0.24);
  background: rgba(255, 255, 255, 0.76);
}

.ew-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ew-meta-row {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #6b7280;
  flex-wrap: wrap;
}

.ew-meta-sep {
  color: #d1d5db;
}

.ew-section {
  margin-top: 14px;
}

.ew-section-title {
  margin: 4px 2px 10px;
  font-size: 15px;
  letter-spacing: 0.8px;
  text-transform: uppercase;
  color: #13131b;
  font-weight: 800;
}

.ew-kpi-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

@media (min-width: 760px) {
  .ew-kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1200px) {
  .ew-kpi-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.ew-kpi-card {
  border-radius: 14px;
  padding: 14px;
  border: 1px solid rgba(157, 20, 39, 0.28);
  background: linear-gradient(145deg, #ffffff 0%, #fff1f3 45%, #ffdfe4 100%);
  box-shadow: 0 10px 22px rgba(117, 18, 35, 0.13);
}

.ew-kpi-label {
  font-size: 12px;
  color: #6b7280;
}

.ew-kpi-value {
  margin-top: 4px;
  font-size: 28px;
  line-height: 1.1;
  color: #0f0f14;
  font-weight: 800;
}

.ew-kpi-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #9ca3af;
}

.ew-quick-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

@media (min-width: 900px) {
  .ew-quick-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (min-width: 1280px) {
  .ew-quick-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.ew-quick {
  border: 1px solid rgba(19, 19, 27, 0.08);
  background: #ffffff;
  border-radius: 14px;
  padding: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(16, 16, 24, 0.06);
  transition: transform 0.14s ease, box-shadow 0.14s ease, border-color 0.14s ease;
}

.ew-quick:hover {
  transform: translateY(-1px);
  border-color: rgba(220, 38, 38, 0.3);
  box-shadow: 0 12px 24px rgba(16, 16, 24, 0.1);
}

.ew-quick-icon {
  width: 42px;
  height: 42px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  color: #b91c1c;
  background: linear-gradient(145deg, rgba(185, 28, 28, 0.11), rgba(255, 241, 242, 0.92));
  border: 1px solid rgba(220, 38, 38, 0.2);
  flex: 0 0 42px;
}

.ew-quick-icon :deep(svg) {
  width: 16px;
  height: 16px;
}

.ew-quick-main {
  flex: 1;
}

.ew-quick-name {
  font-size: 14px;
  font-weight: 700;
  color: #12131a;
}

.ew-quick-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #6b7280;
}

.ew-quick-arrow {
  color: #b91c1c;
  font-size: 22px;
  line-height: 1;
}

.ew-main-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

@media (min-width: 1240px) {
  .ew-main-grid {
    grid-template-columns: 1.45fr 0.55fr;
  }
}

.ew-side-col {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.ew-card {
  border-radius: 14px;
  border: 1px solid rgba(145, 23, 41, 0.22);
  background: linear-gradient(170deg, #ffffff 0%, #fff6f7 100%);
  box-shadow: 0 12px 26px rgba(103, 20, 35, 0.12);
  padding: 12px;
}

.ew-card-lg {
  padding: 12px;
}

.ew-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.ew-card-head h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: #18181f;
}

.ew-link-btn {
  border: 1px solid rgba(146, 19, 36, 0.36);
  border-radius: 10px;
  background: linear-gradient(130deg, #8f1123, #c81f35 60%, #e13a4d);
  color: #fff;
  font-size: 12px;
  padding: 6px 10px;
  cursor: pointer;
  box-shadow: 0 8px 15px rgba(140, 20, 38, 0.22);
}

.ew-table-wrap {
  overflow-x: auto;
}

.ew-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  min-width: 640px;
}

.ew-table thead tr {
  background: linear-gradient(90deg, #7c1021 0%, #a9162d 35%, #cf243a 68%, #e64555 100%);
}

.ew-table thead th {
  text-align: left;
  font-size: 12px;
  color: #fff;
  padding: 11px 10px;
  background: transparent;
  border-bottom: 2px solid rgba(93, 14, 28, 0.5);
}

.ew-table tbody td {
  padding: 10px;
  font-size: 13px;
  color: #1a1f2a;
  border-bottom: 1px solid rgba(181, 34, 53, 0.1);
  background: #fff;
}

.ew-table tbody tr:hover td {
  background: #fff6f7;
}

.ew-status {
  display: inline-flex;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  padding: 4px 9px;
}

.tone-success {
  color: #0f766e;
  background: rgba(20, 184, 166, 0.14);
}

.tone-danger {
  color: #b91c1c;
  background: rgba(248, 113, 113, 0.2);
}

.tone-processing {
  color: #1d4ed8;
  background: rgba(96, 165, 250, 0.2);
}

.tone-pending {
  color: #92400e;
  background: rgba(251, 191, 36, 0.2);
}

.ew-list {
  display: grid;
  gap: 8px;
}

.ew-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-radius: 10px;
  padding: 9px 10px;
  border: 1px solid rgba(161, 24, 43, 0.18);
  background: linear-gradient(140deg, #fff, #fff4f6 60%, #ffedf0);
}

.ew-list-title {
  font-size: 13px;
  font-weight: 700;
  color: #171720;
}

.ew-list-sub {
  margin-top: 2px;
  font-size: 11px;
  color: #6b7280;
}

.ew-list-tail {
  font-size: 18px;
  font-weight: 800;
  color: #b91c1c;
  min-width: 22px;
  text-align: right;
}

.ew-empty {
  padding: 10px;
  text-align: center;
  font-size: 12px;
  color: #6b7280;
}

@media (max-width: 760px) {
  .ew-page {
    padding: 10px;
  }

  .ew-banner-wrap {
    height: 170px;
  }

  .ew-banner-arrow {
    width: 32px;
    height: 32px;
  }

  .ew-hero-inner {
    padding: 14px;
  }

  .ew-brand-name {
    font-size: 20px;
  }
}
</style>
