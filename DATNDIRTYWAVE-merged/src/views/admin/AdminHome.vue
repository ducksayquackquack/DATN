<template>
  <div class="tc-page">
    <section class="tc-hero">
      <div class="tc-hero-inner">
        <div class="tc-hero-top-row">
          <div class="tc-brand">
            <img class="tc-logo" :src="logo" alt="DirtyWave" />
            <div class="tc-brand-text">
              <div class="tc-brand-name">Trang chủ quản trị</div>
            </div>
          </div>
        </div>

        <div class="tc-hero-divider"></div>

        <h1 class="tc-hero-title">Chọn đúng sản phẩm, xử lý đơn nhanh, giữ nhịp vận hành mỗi ngày.</h1>
        <p class="tc-hero-desc">
          Giao diện được tối ưu cho DirtyWave, toàn bộ số liệu và hành động dùng API + route thật của DATN.
          Mọi thẻ dưới đây đều phục vụ trực tiếp cho admin trong ca làm việc.
        </p>

        <div class="tc-hero-actions">
          <button class="tc-btn tc-btn-primary" type="button" @click="go('/admin/san-pham/list')">
            <Shirt :size="14" :stroke-width="2.3" /> Quản lý sản phẩm
          </button>
          <button class="tc-btn tc-btn-outline" type="button" @click="go('/admin/ban-hang')">
            <CreditCard :size="14" :stroke-width="2.3" /> Vào màn bán hàng
          </button>
          <button class="tc-btn tc-btn-soft" type="button" @click="syncDashboard" :disabled="loading">
            {{ loading ? 'Đang tải...' : 'Làm mới dữ liệu' }}
          </button>
        </div>

        <div class="tc-meta-row">
          <span>{{ todayLabel }}</span>
        </div>
        <div v-if="apiError" class="tc-error">{{ apiError }}</div>
      </div>

      <div class="tc-banner-wrap">
        <img
          v-for="(slide, idx) in slides"
          :key="idx"
          :src="slide.img"
          :alt="slide.title"
          class="tc-banner-img"
          :class="{ 'tc-banner-img--active': idx === activeSlide }"
        />
        <div class="tc-banner-overlay"></div>
        <div class="tc-slide-glass">
          <div class="tc-slide-kicker">{{ slides[activeSlide].kicker }}</div>
          <div class="tc-slide-title">{{ slides[activeSlide].title }}</div>
          <div class="tc-slide-desc">{{ slides[activeSlide].desc }}</div>
          <div class="tc-slide-dots">
            <button
              v-for="(slide, idx) in slides"
              :key="idx"
              class="tc-slide-dot"
              :class="{ 'tc-slide-dot--active': idx === activeSlide }"
              type="button"
              @click="activeSlide = idx"
              :aria-label="`Ảnh ${idx + 1}`"
            />
          </div>
        </div>
        <button class="tc-banner-arrow tc-banner-left" type="button" @click="activeSlide = (activeSlide - 1 + slides.length) % slides.length" aria-label="Ảnh trước">
          <ChevronLeft :size="18" :stroke-width="2.4" />
        </button>
        <button class="tc-banner-arrow tc-banner-right" type="button" @click="activeSlide = (activeSlide + 1) % slides.length" aria-label="Ảnh sau">
          <ChevronRight :size="18" :stroke-width="2.4" />
        </button>
      </div>
    </section>

    <section class="tc-section tc-kpi-grid">
      <article class="tc-kpi-card">
        <div class="tc-kpi-label">Tổng doanh thu</div>
        <div class="tc-kpi-value">{{ formatCurrency(totalRevenue) }}</div>
        <div class="tc-kpi-sub">{{ completedOrders }} đơn hoàn thành</div>
      </article>

      <article class="tc-kpi-card">
        <div class="tc-kpi-label">Đơn đang xử lý</div>
        <div class="tc-kpi-value">{{ pendingOrders }}</div>
        <div class="tc-kpi-sub">Theo hóa đơn hiện có</div>
      </article>

      <article v-if="false" class="tc-kpi-card">
        <div class="tc-kpi-label">Sản phẩm cận tồn</div>
        <div class="tc-kpi-value">{{ lowStockProducts.length }}</div>
        <div class="tc-kpi-sub">Ngưỡng cảnh báo &lt; 12</div>
      </article>

      <article class="tc-kpi-card">
        <div class="tc-kpi-label">Nhân sự hoạt động</div>
        <div class="tc-kpi-value">{{ activeEmployees }}/{{ employees.length }}</div>
        <div class="tc-kpi-sub">Khách hàng: {{ customers.length }}</div>
      </article>
    </section>

    <section class="tc-section">
      <div class="tc-section-title">Lối tắt nhanh</div>
      <div class="tc-quick-grid">
        <button v-for="item in quickLinks" :key="item.path" class="tc-quick" type="button" @click="go(item.path)">
          <div class="tc-quick-icon">
            <component :is="item.icon" :size="16" :stroke-width="2.1" />
          </div>
          <div class="tc-quick-main">
            <div class="tc-quick-name">{{ item.title }}</div>
            <div class="tc-quick-desc">{{ item.desc }}</div>
          </div>
          <span class="tc-quick-arrow">›</span>
        </button>
      </div>
    </section>

    <section class="tc-section">
      <div class="tc-section-head-row">
        <div class="tc-section-title">Sản phẩm nổi bật</div>
        <button class="tc-link-btn" type="button" @click="go('/admin/san-pham/list')">Xem tất cả</button>
      </div>
      <div class="tc-feature-grid">
        <article v-for="item in featuredProducts" :key="item.id" class="tc-feature-card">
          <div class="tc-feature-image-wrap">
            <img :src="item.image" :alt="item.name" class="tc-feature-image" loading="lazy" />
            <span class="tc-feature-chip">{{ item.chip }}</span>
          </div>
          <div class="tc-feature-body">
            <h3>{{ item.name }}</h3>
            <p>{{ item.meta }}</p>
          </div>
        </article>
      </div>
    </section>

    <section class="tc-section">
      <div class="tc-about">
        <article class="tc-about-main">
          <div class="tc-section-title">Về trung tâm vận hành</div>
          <p>
            Trang chủ này được thiết kế riêng cho đội admin DirtyWave:
            ưu tiên dữ liệu live, thao tác nhanh, và cảnh báo tồn kho theo thời gian thực.
          </p>
          <div class="tc-about-actions">
            <button class="tc-btn tc-btn-primary" type="button" @click="go('/admin/hoa-don/pos')">Tạo đơn ngay</button>
            <button class="tc-btn tc-btn-outline" type="button" @click="go('/admin/khuyen-mai/list')">Tạo khuyến mãi</button>
          </div>
        </article>
        <article class="tc-about-steps">
          <h3>3 bước xử lý nhanh</h3>
          <div class="tc-step" v-for="(step, index) in aboutSteps" :key="step.title">
            <span class="tc-step-no">{{ String(index + 1).padStart(2, '0') }}</span>
            <div>
              <div class="tc-step-title">{{ step.title }}</div>
              <div class="tc-step-desc">{{ step.desc }}</div>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section class="tc-section tc-main-grid">
      <article class="tc-card tc-card-lg">
        <div class="tc-card-head">
          <h2>Hóa đơn gần nhất</h2>
          <button class="tc-link-btn" type="button" @click="go('/admin/hoa-don/list')">Mở danh sách</button>
        </div>

        <div class="tc-table-wrap">
          <table class="tc-table">
            <thead>
              <tr>
                <th style="width:140px">Mã đơn</th>
                <th style="width:200px">Khách hàng</th>
                <th style="width:160px;text-align:right">Tổng tiền</th>
                <th style="width:150px;text-align:center">Trạng thái</th>
                <th style="width:160px;text-align:center">Thời gian</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in recentInvoices" :key="row.id || row.maHoaDon">
                <td>{{ row.maHoaDon }}</td>
                <td>{{ row.customer }}</td>
                <td style="text-align:right;font-weight:600">{{ formatCurrency(row.total) }}</td>
                <td style="text-align:center">
                  <span class="tc-status" :class="`tone-${row.tone}`">{{ row.status }}</span>
                </td>
                <td style="text-align:center">{{ formatDate(row.createdAt) }}</td>
              </tr>
              <tr v-if="!recentInvoices.length">
                <td colspan="5" class="tc-empty">Chưa có dữ liệu hóa đơn.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <div class="tc-side-col">
        <article v-if="false" class="tc-card">
          <div class="tc-card-head">
            <h2>Tồn kho cảnh báo</h2>
            <button class="tc-link-btn" type="button" @click="go('/admin/san-pham/list')">Mở kho</button>
          </div>

          <div class="tc-list">
            <div v-for="item in lowStockProducts" :key="item.id" class="tc-list-item">
              <div>
                <div class="tc-list-title">{{ item.tenSanPham }}</div>
                <div class="tc-list-sub">{{ item.maSanPham }}</div>
              </div>
              <span class="tc-stock-badge" :class="`tc-stock-${item.urgency}`">
                {{ item.ton === 0 ? 'Hết hàng' : `Còn ${item.ton}` }}
              </span>
            </div>
            <div v-if="!lowStockProducts.length" class="tc-empty">Tất cả sản phẩm đều đủ hàng.</div>
          </div>
        </article>

        <article class="tc-card">
          <div class="tc-card-head">
            <h2>Khuyến mãi đang chạy</h2>
            <button class="tc-link-btn" type="button" @click="go('/admin/khuyen-mai/list')">Mở khuyến mãi</button>
          </div>

          <div class="tc-list">
            <div v-for="promo in activePromos" :key="promo.id" class="tc-list-item">
              <div>
                <div class="tc-list-title">{{ promo.name }}</div>
                <div class="tc-list-sub">{{ promo.type }}</div>
              </div>
              <button class="tc-mini-btn" type="button" @click="go('/admin/khuyen-mai/list')">Xem</button>
            </div>
            <div v-if="!activePromos.length" class="tc-empty">Chưa có chương trình nào đang chạy.</div>
          </div>
        </article>

        <article class="tc-card">
          <div class="tc-card-head">
            <h2>Phương thức thanh toán</h2>
            <button class="tc-link-btn" type="button" @click="go('/admin/phuong-thuc-thanh-toan/list')">Cấu hình</button>
          </div>

          <div class="tc-payment-grid">
            <div v-for="item in paymentMix" :key="item.code" class="tc-pay-item">
              <img v-if="item.logo" :src="item.logo" :alt="item.label" class="tc-pay-logo" />
              <div v-else class="tc-pay-fallback">
                <component v-if="item.icon" :is="item.icon" :size="15" :stroke-width="2" />
                <span v-else>{{ item.label.slice(0, 1) }}</span>
              </div>
              <div>
                <div class="tc-pay-label">{{ item.label }}</div>
                <div class="tc-pay-sub">{{ item.count }} đơn</div>
              </div>
            </div>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Shirt,
  FileText,
  Tag,
  Users,
  UserCog,
  BarChart3,
  House,
  CreditCard,
  Banknote,
  Package,
  ChevronLeft,
  ChevronRight
} from 'lucide-vue-next'

import { getAllHoaDon } from '../../services/hoaDonService'
import { getAllSanPham } from '../../services/sanPhamService'
import { getAllKhuyenMai, getAllVouchers } from '../../services/khuyenMaiService'
import { getAllNhanVien } from '../../services/nhanVienService'
import { getAllKhachHang } from '../../services/KhachHangService'
import { resolveApiOrigin } from '../../utils/apiOrigin'
import { getProductImageOverride } from '../../utils/productImageOverrides'

import logo from '../../assets/img/logo/new logo.png?url'
import img1 from '../../assets/img/Jackets/bomber/bomber-da-lon.jpg?url'
import img2 from '../../assets/img/Jackets/bomber/bomber-dang-lung.jpg?url'
import img3 from '../../assets/img/Jackets/bomber/bomber-gia-da.jpg?url'
import img4 from '../../assets/img/Jackets/bomber/bomber-nhe-cotton.jpg?url'
import img5 from '../../assets/img/Jackets/hoodie/hoodie-dang-hop.jpg?url'
import img6 from '../../assets/img/Jackets/hoodie/hoodie-in-hinh.jpg?url'
import img7 from '../../assets/img/Jackets/hoodie/hoodie-keo-khoa.jpg?url'
import img8 from '../../assets/img/Jackets/coach/coach-cach-nhiet.jpg?url'
import img9 from '../../assets/img/Jackets/coach/coach-da-asos.jpg?url'
import img10 from '../../assets/img/Jackets/coach/coach-gia-da.jpg?url'
import img11 from '../../assets/img/Jackets/coach/coach-long-cuu.jpg?url'
import img12 from '../../assets/img/Jackets/bomber/bomber-astronaut/bomber-astronaut-black.PNG?url'
import img13 from '../../assets/img/Jackets/bomber/bomber-embroidered-fuzzy/bomer-embroidered-black.PNG?url'
import img14 from '../../assets/img/Jackets/bomber/bomber-windbreaker/bomer-windbreaker-black.PNG?url'
import img15 from '../../assets/img/Jackets/coach/coach-leopard/coach-leopard.PNG?url'
import img16 from '../../assets/img/Jackets/coach/coach-longsleeve/coach-longsleeve-black.PNG?url'
import img17 from '../../assets/img/Jackets/coach/coach-tiger-stripe/coach-tiger-stripe.PNG?url'
import img18 from '../../assets/img/Jackets/hoodie/hoodie-camo/hoodie-camo-black.PNG?url'
import img19 from '../../assets/img/Jackets/hoodie/hoodie-zip-boxy/hoodie-zip-boxy-blue.PNG?url'
import img20 from '../../assets/img/Jackets/hoodie/hoodie-zip-silk/hoodie-zip-silk-black.PNG?url'
import vnpayLogo from '../../assets/img/payments/vnpay.png?url'
import visaLogo from '../../assets/img/payments/visa.png?url'
import momoLogo from '../../assets/img/payments/momo.png?url'
import masterLogo from '../../assets/img/payments/mastercard.png?url'

const adminHeroImages = [
  'https://images.unsplash.com/photo-1551028719-00167b16eac5?w=1920&q=90&fit=crop&auto=format',
  'https://images.unsplash.com/photo-1542272604-787c3835535d?w=1920&q=90&fit=crop&auto=format',
  'https://images.unsplash.com/photo-1548126032-079a0fb0099d?w=1920&q=90&fit=crop&auto=format'
]

const router = useRouter()
const go = (path) => router.push(path)

const now = ref(new Date())
const loading = ref(false)
const apiError = ref('')

const invoices = ref([])
const products = ref([])
const campaigns = ref([])
const vouchers = ref([])
const employees = ref([])
const customers = ref([])
const soldBySpct = ref(new Map())
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, '')
const mappedFallbackByCode = {
  SP001: img1,
  SP002: img2,
  SP003: img3,
  SP004: img4,
  SP005: img5,
  SP006: img6,
  SP007: img7,
  SP008: img8,
  SP009: img9,
  SP010: img10,
  SP011: img11,
  SP012: img12,
  SP013: img13,
  SP014: img14,
  SP015: img15,
  SP016: img16,
  SP017: img17,
  SP018: img18,
  SP019: img19,
  SP020: img20
}

const mappedFallbackByName = {
  'coach longsleeve dirtywave': img16,
  'bomber windbreaker dirtywave': img14,
  'hoodie camo dirtywave': img18,
  'bomber astronaut dirtywave': img12,
  'hoodie zip silk dirtywave': img20,
  'coach tiger stripe dirtywave': img17,
  'hoodie zip boxy dirtywave': img19,
  'bomber embroidered fuzzy dirtywave': img13
}

const featuredPriorityNames = [
  'coach longsleeve dirtywave',
  'bomber windbreaker dirtywave',
  'hoodie camo dirtywave',
  'bomber astronaut dirtywave',
  'hoodie zip silk dirtywave',
  'coach tiger stripe dirtywave',
  'hoodie zip boxy dirtywave',
  'bomber embroidered fuzzy dirtywave'
]

const activeSlide = ref(0)

const quickLinks = [
  { title: 'Sản phẩm', desc: 'Danh sách, form thêm/sửa', path: '/admin/san-pham/list', icon: Shirt },
  { title: 'Hóa đơn', desc: 'Theo dõi trạng thái đơn', path: '/admin/hoa-don/list', icon: FileText },
  { title: 'Bán hàng', desc: 'POS tại quầy', path: '/admin/hoa-don/pos', icon: CreditCard },
  { title: 'Khuyến mãi', desc: 'Campaign và voucher', path: '/admin/khuyen-mai/list', icon: Tag },
  { title: 'Khách hàng', desc: 'Quản lý hồ sơ khách', path: '/admin/khach-hang/list', icon: Users },
  { title: 'Nhân viên', desc: 'Tài khoản vận hành', path: '/admin/nhan-vien/list', icon: UserCog },
  { title: 'Lịch làm việc', desc: 'Điều phối ca trực', path: '/admin/lich-lam-viec/lich-lam-viec', icon: House },
  { title: 'Thống kê', desc: 'Doanh thu và hiệu quả', path: '/admin/thong-ke/doanh-thu', icon: BarChart3 }
]

const aboutSteps = [
  { title: 'Xác định nhu cầu', desc: 'Nhìn lối chơi + mặt sân để lọc nhanh nhóm sản phẩm phù hợp.' },
  { title: 'Đối chiếu tồn kho', desc: 'Ưu tiên SKU còn hàng và cảnh báo mã gần hết trước khi tư vấn.' },
  { title: 'Chốt đơn tại quầy', desc: 'Đẩy nhanh thanh toán và đồng bộ trạng thái đơn theo thời gian thực.' }
]

const paymentMeta = {
  VNPAY: { label: 'VNPay', logo: vnpayLogo },
  MOMO: { label: 'MoMo', logo: momoLogo },
  VISA: { label: 'Visa', logo: visaLogo },
  MASTERCARD: { label: 'MasterCard', logo: masterLogo },
  CASH: { label: 'Tiền mặt', logo: null, icon: Banknote },
  COD: { label: 'COD', logo: null, icon: Package },
  OTHER: { label: 'Khác', logo: null, icon: null }
}

let clockTimer = null
let slideTimer = null

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

const normalizeName = (value = '') => String(value || '').trim().toLowerCase().replace(/\s+/g, ' ')

const parseDate = (value) => {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

const isImageString = (value = '') => {
  const raw = String(value || '').trim()
  if (!raw) return false
  if (/^data:image\//i.test(raw)) return true

  const normalized = raw.replace(/\\/g, '/').split(/[?#]/)[0]
  if (normalized.startsWith('/uploads/') || normalized.startsWith('uploads/')) return true
  if (/\.(png|jpe?g|gif|webp|svg|bmp|avif)$/i.test(normalized)) return true
  return /^https?:\/\//i.test(raw)
}

const toImageUrl = (value = '') => {
  const raw = String(value || '').trim()
  if (!raw) return ''
  if (/^data:image\//i.test(raw)) return raw

  const normalized = raw.replace(/\\/g, '/')
  const uploadsMatch = normalized.match(/^.*?\/?(uploads\/.*)$/i)
  if (uploadsMatch?.[1]) return `${BACKEND_ORIGIN}/${uploadsMatch[1]}`
  if (/^https?:\/\//i.test(normalized)) return normalized
  if (normalized.startsWith('/')) return normalized
  if (normalized.startsWith('uploads/')) return `${BACKEND_ORIGIN}/${normalized}`
  return normalized.includes('/') ? `/${normalized.replace(/^\/+/, '')}` : normalized
}

const pickImageValue = (entry) => {
  if (!entry) return ''

  if (typeof entry === 'string') {
    return isImageString(entry) ? toImageUrl(entry) : ''
  }

  if (Array.isArray(entry)) {
    for (const child of entry) {
      const found = pickImageValue(child)
      if (found) return found
    }
    return ''
  }

  if (typeof entry === 'object') {
    const keys = ['anh', 'hinhAnh', 'image', 'imageUrl', 'images', 'listAnh', 'anhChinh', 'duongDanAnh', 'src', 'thumbnail']
    for (const key of keys) {
      const found = pickImageValue(entry[key])
      if (found) return found
    }
  }

  return ''
}

const resolveProductImage = (product = {}) => {
  const override = getProductImageOverride({ id: product?.id, maSanPham: product?.maSanPham })[0]
  const overrideUrl = toImageUrl(override)
  if (overrideUrl) return overrideUrl

  const byName = mappedFallbackByName[normalizeName(product?.tenSanPham)]
  if (byName) return byName

  const directImage = pickImageValue([product, product?.sanPhamChiTiets])
  if (directImage) return directImage

  const code = String(product?.maSanPham || '').trim().toUpperCase()
  if (mappedFallbackByCode[code]) return mappedFallbackByCode[code]

  return logo
}

const formatCurrency = (value) => `${new Intl.NumberFormat('vi-VN').format(toNumber(value))}₫`

const formatDate = (value) => {
  const date = parseDate(value)
  return date ? date.toLocaleString('vi-VN') : '-'
}

const invoiceDate = (item) => parseDate(item?.ngayTao || item?.createdAt || item?.ngayDat)

const invoiceTotal = (item) => {
  const gross = (
    toNumber(item?.tongTien) ||
    toNumber(item?.thanhTien) ||
    toNumber(item?.tongTienThanhToan) ||
    toNumber(item?.totalAmount) ||
    0
  )
  const shipping = toNumber(item?.phiVanChuyen || item?.phiShip || item?.shippingFee || 0)
  return Math.max(0, gross - shipping)
}

const invoiceStatus = (item) => {
  const raw = String(item?.orderStatusName || item?.trangThai || item?.orderStatusCode || '').toUpperCase()
  if (raw.includes('HOAN_THANH') || raw.includes('HOÀN THÀNH')) return { label: 'Hoàn thành', tone: 'success' }
  if (raw.includes('HUY') || raw.includes('HỦY')) return { label: 'Đã hủy', tone: 'danger' }
  if (raw.includes('DANG_GIAO') || raw.includes('ĐANG GIAO')) return { label: 'Đang giao', tone: 'processing' }
  if (raw.includes('CHO') || raw.includes('CHỜ')) return { label: 'Chờ xử lý', tone: 'pending' }
  return { label: item?.orderStatusName || item?.trangThai || 'Không xác định', tone: 'pending' }
}

const invoicePaymentCode = (item) => {
  const raw = String(item?.phuongThucThanhToan || item?.paymentMethod || item?.tenPhuongThucThanhToan || '').toUpperCase()
  if (!raw) return 'COD'
  if (raw.includes('VNPAY')) return 'VNPAY'
  if (raw.includes('MOMO')) return 'MOMO'
  if (raw.includes('VISA')) return 'VISA'
  if (raw.includes('MASTER')) return 'MASTERCARD'
  if (raw.includes('CASH') || raw.includes('TIỀN MẶT')) return 'CASH'
  if (raw.includes('COD')) return 'COD'
  return 'OTHER'
}

const productStock = (item) => {
  const topLevelStock = Number(item?.soLuong ?? item?.soLuongTon ?? item?.tonKho ?? item?.ton)
  if (Number.isFinite(topLevelStock)) {
    return Math.max(0, topLevelStock)
  }

  const details =
    (Array.isArray(item?.sanPhamChiTiets) && item.sanPhamChiTiets)
    || (Array.isArray(item?.variants) && item.variants)
    || (Array.isArray(item?.chiTiets) && item.chiTiets)
    || (Array.isArray(item?.danhSachBienThe) && item.danhSachBienThe)
    || (Array.isArray(item?.sanPhamChiTietList) && item.sanPhamChiTietList)
    || []
  if (details.length) {
    return details.reduce((sum, row) => {
      const base = toNumber(row?.soLuong ?? row?.soLuongTon ?? row?.tonKho ?? row?.ton)
      const spctId = Number(row?.id || 0)
      const sold = spctId > 0 ? toNumber(soldBySpct.value.get(spctId)) : 0
      return sum + Math.max(0, base - sold)
    }, 0)
  }
  return 0
}

const productMinPrice = (item) => {
  const details =
    (Array.isArray(item?.sanPhamChiTiets) && item.sanPhamChiTiets)
    || (Array.isArray(item?.variants) && item.variants)
    || (Array.isArray(item?.chiTiets) && item.chiTiets)
    || (Array.isArray(item?.danhSachBienThe) && item.danhSachBienThe)
    || (Array.isArray(item?.sanPhamChiTietList) && item.sanPhamChiTietList)
    || []

  const activeDetails = details.filter((row) => isActiveStatus(row?.trangThai || row?.status))
  const source = activeDetails.length ? activeDetails : details
  const variantPrices = source
    .map((row) => toNumber(
      row?.giaBanSauDotGiamGia
      ?? row?.giaSauGiam
      ?? row?.giaBan
      ?? row?.price
    ))
    .filter((price) => price > 0)

  if (variantPrices.length) return Math.min(...variantPrices)

  return toNumber(item?.giaBan || item?.gia || item?.price || 0)
}

const productLowStockValue = (item) => {
  const details =
    (Array.isArray(item?.sanPhamChiTiets) && item.sanPhamChiTiets)
    || (Array.isArray(item?.variants) && item.variants)
    || (Array.isArray(item?.chiTiets) && item.chiTiets)
    || (Array.isArray(item?.danhSachBienThe) && item.danhSachBienThe)
    || (Array.isArray(item?.sanPhamChiTietList) && item.sanPhamChiTietList)
    || []

  if (details.length) {
    const minVariantStock = details.reduce((minStock, row) => {
      const base = toNumber(row?.soLuong ?? row?.soLuongTon ?? row?.tonKho ?? row?.ton)
      const spctId = Number(row?.id || 0)
      const sold = spctId > 0 ? toNumber(soldBySpct.value.get(spctId)) : 0
      const available = Math.max(0, base - sold)
      return Math.min(minStock, available)
    }, Number.POSITIVE_INFINITY)
    return Number.isFinite(minVariantStock) ? minVariantStock : 0
  }

  return toNumber(item?.soLuong ?? item?.soLuongTon ?? item?.tonKho ?? item?.ton ?? 0)
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

const syncDashboard = async () => {
  loading.value = true
  apiError.value = ''

  const [hoaDonRes, sanPhamRes, khuyenMaiRes, voucherRes, nhanVienRes, khachHangRes] = await Promise.allSettled([
    getAllHoaDon(),
    getAllSanPham(),
    getAllKhuyenMai(),
    getAllVouchers(),
    getAllNhanVien(),
    getAllKhachHang(0, 200)
  ])

  const failed = []

  if (hoaDonRes.status === 'fulfilled') invoices.value = normalizeArray(hoaDonRes.value)
  else {
    invoices.value = []
    failed.push('hóa đơn')
  }

  if (sanPhamRes.status === 'fulfilled') products.value = normalizeArray(sanPhamRes.value)
  else {
    products.value = []
    failed.push('sản phẩm')
  }

  if (khuyenMaiRes.status === 'fulfilled') campaigns.value = normalizeArray(khuyenMaiRes.value)
  else {
    campaigns.value = []
    failed.push('khuyến mãi')
  }

  if (voucherRes.status === 'fulfilled') vouchers.value = normalizeArray(voucherRes.value)
  else {
    vouchers.value = []
    failed.push('voucher')
  }

  if (nhanVienRes.status === 'fulfilled') employees.value = normalizeArray(nhanVienRes.value)
  else {
    employees.value = []
    failed.push('nhân viên')
  }

  if (khachHangRes.status === 'fulfilled') customers.value = normalizeArray(khachHangRes.value)
  else {
    customers.value = []
    failed.push('khách hàng')
  }

  if (failed.length) apiError.value = `Chưa tải được: ${failed.join(', ')}`

  // Compute sold quantities per variant from completed invoices
  soldBySpct.value = await computeSoldBySpctFromInvoices(invoices.value)

  loading.value = false
}

import { shouldCountOrderForStock, computeSoldBySpctFromInvoices } from "@/utils/stockCalculation"

const shouldCountForStock = (order) => shouldCountOrderForStock(order)

const totalRevenue = computed(() => {
  return invoices.value.reduce((sum, item) => {
    const status = invoiceStatus(item)
    // Bỏ qua đơn đã hủy
    if (status.label === 'Đã hủy') return sum
    const orderType = String(item?.loaiHoaDon || item?.orderType || '').toUpperCase()
    const isOnline = orderType.includes('DELIVERY') || orderType.includes('TRUC_TUYEN') || orderType.includes('ONLINE')
    if (isOnline) {
      // Đơn trực tuyến: chỉ tính khi đã hoàn thành hoặc thanh toán chuyển khoản
      const payCode = invoicePaymentCode(item)
      const isCompleted = status.label === 'Hoàn thành'
      const isBankPaid = payCode === 'OTHER' || payCode === 'VNPAY' || String(item?.phuongThucThanhToan || '').toUpperCase().includes('BANK')
      if (!isCompleted && !isBankPaid) return sum
    }
    return sum + invoiceTotal(item)
  }, 0)
})

const completedOrders = computed(() => invoices.value.filter((item) => invoiceStatus(item).label === 'Hoàn thành').length)

const pendingOrders = computed(() => {
  return invoices.value.filter((item) => {
    const label = invoiceStatus(item).label.toLowerCase()
    return label.includes('chờ') || label.includes('đang giao')
  }).length
})

const activeEmployees = computed(() => {
  return employees.value.filter((item) => {
    const status = String(item?.trangThaiHoatDong || item?.trangThai || '').toLowerCase()
    return !status || status.includes('hoạt') || status.includes('active') || status === '1' || status === 'true'
  }).length
})

const recentInvoices = computed(() => {
  return [...invoices.value]
    .sort((a, b) => {
      const bTime = invoiceDate(b)?.getTime() || 0
      const aTime = invoiceDate(a)?.getTime() || 0
      return bTime - aTime
    })
    .slice(0, 6)
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

const lowStockProducts = computed(() => {
  const rows = []
  for (const item of products.value) {
    const stock = productStock(item)
    if (stock < 12) {
      rows.push({
        id: item?.id,
        maSanPham: item?.maSanPham || `SP-${item?.id || 'N/A'}`,
        tenSanPham: item?.tenSanPham || 'Sản phẩm chưa đặt tên',
        ton: stock,
        urgency: stock === 0 ? 'out' : stock <= 5 ? 'critical' : 'low'
      })
    }
  }
  return rows.sort((a, b) => a.ton - b.ton).slice(0, 8)
})

const activePromos = computed(() => {
  const km = campaigns.value
    .filter(isActivePromotion)
    .slice(0, 3)
    .map((item) => ({
      id: `km-${item?.id}`,
      name: item?.tenKhuyenMai || item?.maKhuyenMai || 'Khuyến mãi',
      type: 'Campaign'
    }))

  const vc = vouchers.value
    .filter(isActivePromotion)
    .slice(0, 2)
    .map((item) => ({
      id: `vc-${item?.id}`,
      name: item?.tenPhieuGiamGia || item?.maPhieuGiamGia || 'Voucher',
      type: 'Voucher'
    }))

  return [...km, ...vc]
})

const featuredProducts = computed(() => {
  const normalizedPriority = featuredPriorityNames
  const selected = []
  const used = new Set()

  for (const targetName of normalizedPriority) {
    const index = products.value.findIndex((item, idx) => {
      if (used.has(idx)) return false
      const name = normalizeName(item?.tenSanPham)
      return name === targetName || name.includes(targetName) || targetName.includes(name)
    })

    if (index !== -1) {
      selected.push(products.value[index])
      used.add(index)
    }

    if (selected.length >= 4) break
  }

  if (selected.length < 4) {
    products.value.forEach((item, idx) => {
      if (selected.length >= 4) return
      if (used.has(idx)) return
      selected.push(item)
      used.add(idx)
    })
  }

  return selected
    .map((item, idx) => ({
      id: item?.id || `fallback-${idx}`,
      name: item?.tenSanPham || `Sản phẩm ${idx + 1}`,
      image: resolveProductImage(item),
      chip: idx % 2 === 0 ? 'Top bán' : 'Khuyến nghị',
      meta: `Tồn kho hiện tại: ${productStock(item)} • Giá từ ${formatCurrency(productMinPrice(item))}`
    }))
})

const slides = computed(() => {
  const source = featuredProducts.value.slice(0, 3)
  if (!source.length) {
    return [
      {
        kicker: 'Bộ sưu tập',
        title: 'DirtyWave',
        desc: 'Dữ liệu sản phẩm sẽ hiển thị sau khi đồng bộ.',
        img: adminHeroImages[0]
      }
    ]
  }

  return source.map((item, index) => ({
    kicker: index === 0 ? 'Bộ sưu tập' : index === 1 ? 'Vận hành' : 'Hiệu suất',
    title: item.name,
    desc: item.meta,
    img: adminHeroImages[index % adminHeroImages.length]
  }))
})

const paymentMix = computed(() => {
  const bucket = { VNPAY: 0, MOMO: 0, VISA: 0, MASTERCARD: 0, CASH: 0, COD: 0, OTHER: 0 }
  for (const inv of invoices.value) {
    const code = invoicePaymentCode(inv)
    bucket[code] = (bucket[code] || 0) + 1
  }

  return Object.entries(bucket)
    .map(([code, count]) => ({ code, count, ...paymentMeta[code] }))
    .filter((item) => item.count > 0)
    .sort((a, b) => b.count - a.count)
    .slice(0, 5)
})

const todayLabel = computed(() => now.value.toLocaleDateString('vi-VN', {
  weekday: 'long',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric'
}))

onMounted(async () => {
  clockTimer = setInterval(() => {
    now.value = new Date()
  }, 1000)

  slideTimer = setInterval(() => {
    activeSlide.value = (activeSlide.value + 1) % slides.value.length
  }, 4500)

  await syncDashboard()
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  if (slideTimer) clearInterval(slideTimer)
})
</script>

<style scoped>
.tc-page {
  padding: 14px;
  min-height: calc(100vh - 70px);
  background: transparent;
  color: #101014;
}

.tc-hero {
  border-radius: 18px;
  border: 1px solid rgba(220, 24, 24, 0.18);
  background: linear-gradient(120deg, rgba(255, 255, 255, 0.97) 0%, rgba(255, 245, 246, 0.96) 50%, rgba(255, 239, 241, 0.95) 100%);
  box-shadow: 0 14px 36px rgba(5, 5, 5, 0.1);
  overflow: hidden;
}

.tc-hero-inner {
  padding: 20px 24px 18px;
}

.tc-hero-top-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}

.tc-live-pill {
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

.tc-live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ef4444;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.2);
  animation: tc-pulse 1.8s ease-in-out infinite;
}

@keyframes tc-pulse {
  0%, 100% { box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.2); }
  50% { box-shadow: 0 0 0 7px rgba(239, 68, 68, 0.08); }
}

.tc-hero-divider {
  margin: 14px 0;
  height: 1px;
  background: linear-gradient(90deg, rgba(185, 28, 28, 0.25), rgba(185, 28, 28, 0.06) 60%, transparent);
}

.tc-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tc-brand-eyebrow {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: #9ca3af;
  line-height: 1;
  margin-bottom: 2px;
}

.tc-logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  object-fit: contain;
  background: linear-gradient(135deg, #b91c1c 0%, #ef4444 100%);
  padding: 6px;
  border: 1px solid rgba(239, 68, 68, 0.4);
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.3);
}

.tc-brand-kicker {
  font-size: 11px;
  letter-spacing: 1.2px;
  text-transform: uppercase;
  color: #ef4444;
  font-weight: 700;
}

.tc-brand-name {
  font-size: 26px;
  line-height: 1.1;
  font-weight: 800;
  color: #0f0f12;
}

.tc-hero-title {
  margin: 0;
  font-size: clamp(22px, 2.8vw, 36px);
  line-height: 1.18;
  font-weight: 800;
  max-width: 800px;
  color: #0a0a0f;
  letter-spacing: -0.3px;
}

.tc-hero-desc {
  margin: 10px 0 0;
  font-size: 14px;
  line-height: 1.65;
  color: #4b5563;
  max-width: 700px;
}

.tc-hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 16px;
}

.tc-btn {
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

.tc-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.tc-btn-primary {
  color: #fff;
  background: linear-gradient(130deg, #7f1020 0%, #b7182d 45%, #ef3348 100%);
  box-shadow: 0 8px 20px rgba(176, 22, 43, 0.34);
}

.tc-btn-primary:hover:not(:disabled) {
  box-shadow: 0 12px 28px rgba(176, 22, 43, 0.44);
}

.tc-btn-outline {
  color: #7f1020;
  border-color: rgba(185, 28, 28, 0.35);
  background: linear-gradient(145deg, #fff, #fff5f6);
}

.tc-btn-soft {
  color: #9f1239;
  border-color: rgba(226, 30, 65, 0.24);
  background: rgba(255, 255, 255, 0.76);
}

.tc-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.tc-banner-wrap {
  position: relative;
  height: 420px;
  overflow: hidden;
  border-top: 1px solid rgba(185, 28, 28, 0.2);
}

@media (min-width: 900px) {
  .tc-banner-wrap {
    height: 520px;
  }
}

@media (min-width: 1280px) {
  .tc-banner-wrap {
    height: 620px;
  }
}

.tc-banner-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center center;
  opacity: 0;
  transition: opacity 0.75s ease;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
}

.tc-banner-img--active {
  opacity: 1;
}

.tc-banner-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(5, 7, 12, 0.14), rgba(5, 7, 12, 0.44));
}

.tc-banner-arrow {
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

.tc-banner-left {
  left: 12px;
}

.tc-banner-right {
  right: 12px;
}

.tc-slide-glass {
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

.tc-slide-kicker {
  text-transform: uppercase;
  letter-spacing: 1.1px;
  font-size: 10px;
  font-weight: 700;
  color: #fca5a5;
}

.tc-slide-title {
  margin-top: 3px;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.2;
}

.tc-slide-desc {
  margin-top: 3px;
  font-size: 12px;
  color: #d1d5db;
  line-height: 1.45;
}

.tc-slide-dots {
  margin-top: 8px;
  display: flex;
  gap: 6px;
}

.tc-slide-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  border: none;
  background: rgba(255, 255, 255, 0.3);
  cursor: pointer;
  padding: 0;
  transition: all 0.35s ease;
}

.tc-slide-dot--active {
  width: 22px;
  background: #ffffff;
}

.tc-meta-sep {
  color: #d1d5db;
}

.tc-hero-badges {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tc-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  padding: 7px 10px;
  border: 1px solid rgba(17, 24, 39, 0.12);
  background: rgba(255, 255, 255, 0.76);
  font-size: 12px;
}

.tc-badge-logo {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.tc-dot-live {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  box-shadow: 0 0 0 5px rgba(239, 68, 68, 0.22);
}

.tc-meta-row {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #6b7280;
}

.tc-error {
  margin-top: 10px;
  font-size: 12px;
  color: #b91c1c;
}

.tc-section {
  margin-top: 14px;
}

.tc-section-title {
  margin: 4px 2px 10px;
  font-size: 15px;
  letter-spacing: 0.8px;
  text-transform: uppercase;
  color: #13131b;
  font-weight: 800;
}

.tc-kpi-grid {
  display: grid;
  grid-template-columns: repeat(1, minmax(0, 1fr));
  gap: 10px;
}

@media (min-width: 760px) {
  .tc-kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1200px) {
  .tc-kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

.tc-kpi-card {
  border-radius: 14px;
  padding: 14px;
  border: 1px solid rgba(157, 20, 39, 0.28);
  background: linear-gradient(145deg, #ffffff 0%, #fff1f3 45%, #ffdfe4 100%);
  box-shadow: 0 10px 22px rgba(117, 18, 35, 0.13);
}

.tc-kpi-label {
  font-size: 12px;
  color: #6b7280;
}

.tc-kpi-value {
  margin-top: 4px;
  font-size: 28px;
  line-height: 1.1;
  color: #0f0f14;
  font-weight: 800;
}

.tc-kpi-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #9ca3af;
}

.tc-quick-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

@media (min-width: 900px) {
  .tc-quick-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (min-width: 1280px) {
  .tc-quick-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

.tc-quick {
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

.tc-quick-icon {
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

.tc-quick-icon :deep(svg) {
  width: 16px;
  height: 16px;
}

.tc-quick-main {
  flex: 1;
}

.tc-quick:hover {
  transform: translateY(-1px);
  border-color: rgba(220, 38, 38, 0.3);
  box-shadow: 0 12px 24px rgba(16, 16, 24, 0.1);
}

.tc-quick-name {
  font-size: 14px;
  font-weight: 700;
  color: #12131a;
}

.tc-quick-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #6b7280;
}

.tc-quick-arrow {
  color: #b91c1c;
  font-size: 22px;
  line-height: 1;
}

.tc-main-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.tc-section-head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.tc-cat-grid {
  display: grid;
  grid-template-columns: repeat(1, minmax(0, 1fr));
  gap: 10px;
}

@media (min-width: 860px) {
  .tc-cat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1320px) {
  .tc-cat-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

.tc-cat {
  border-radius: 14px;
  border: 1px solid rgba(16, 16, 26, 0.08);
  background: linear-gradient(170deg, #ffffff, #fff7f8 70%, #fff0f2);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.06);
  padding: 12px;
}

.tc-cat-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tc-cat-head h3 {
  margin: 0;
  font-size: 15px;
}

.tc-cat-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: rgba(220, 38, 38, 0.12);
  color: #b91c1c;
}

.tc-cat p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #4b5563;
  line-height: 1.56;
}

.tc-cat-tags {
  margin-top: 10px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tc-tag {
  border-radius: 999px;
  font-size: 11px;
  padding: 4px 8px;
  color: #991b1b;
  border: 1px solid rgba(239, 68, 68, 0.25);
  background: #fff;
}

.tc-feature-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

@media (min-width: 860px) {
  .tc-feature-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (min-width: 1320px) {
  .tc-feature-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

.tc-feature-card {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(16, 16, 26, 0.08);
  background: #fff;
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.06);
}

.tc-feature-image-wrap {
  position: relative;
  height: 170px;
  background: #0f0f13;
}

.tc-feature-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.tc-feature-chip {
  position: absolute;
  left: 10px;
  bottom: 10px;
  border-radius: 999px;
  padding: 4px 9px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: rgba(185, 28, 28, 0.92);
}

.tc-feature-body {
  padding: 11px;
}

.tc-feature-body h3 {
  margin: 0;
  font-size: 14px;
  color: #111827;
}

.tc-feature-body p {
  margin: 6px 0 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
}

.tc-about {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

@media (min-width: 1120px) {
  .tc-about {
    grid-template-columns: 1.15fr 0.85fr;
  }
}

.tc-about-main,
.tc-about-steps {
  border-radius: 14px;
  border: 1px solid rgba(16, 16, 26, 0.08);
  background: linear-gradient(180deg, #ffffff, #fff4f5);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.06);
  padding: 12px;
}

.tc-about-main p {
  margin: 8px 0 0;
  font-size: 13px;
  color: #4b5563;
  line-height: 1.6;
}

.tc-about-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tc-about-steps h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
}

.tc-step {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  align-items: flex-start;
  border-radius: 10px;
  border: 1px solid rgba(16, 16, 26, 0.08);
  background: #fff;
  padding: 9px;
}

.tc-step-no {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: rgba(220, 38, 38, 0.12);
  color: #b91c1c;
  font-weight: 800;
  flex: 0 0 34px;
}

.tc-step-title {
  font-size: 13px;
  font-weight: 700;
  color: #111827;
}

.tc-step-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #6b7280;
}

@media (min-width: 1240px) {
  .tc-main-grid {
    grid-template-columns: 1.35fr 0.65fr;
  }
}

.tc-side-col {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.tc-card {
  border-radius: 14px;
  border: 1px solid rgba(145, 23, 41, 0.22);
  background: linear-gradient(170deg, #ffffff 0%, #fff6f7 100%);
  box-shadow: 0 12px 26px rgba(103, 20, 35, 0.12);
  padding: 12px;
}

.tc-card-lg {
  padding: 12px;
}

.tc-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.tc-card-head h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: #18181f;
}

.tc-link-btn {
  border: 1px solid rgba(146, 19, 36, 0.36);
  border-radius: 10px;
  background: linear-gradient(130deg, #8f1123, #c81f35 60%, #e13a4d);
  color: #fff;
  font-size: 12px;
  padding: 6px 10px;
  cursor: pointer;
  box-shadow: 0 8px 15px rgba(140, 20, 38, 0.22);
}

.tc-table-wrap {
  overflow-x: auto;
}

.tc-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  min-width: 680px;
}

.tc-table thead tr {
  background: linear-gradient(90deg, #7c1021 0%, #a9162d 35%, #cf243a 68%, #e64555 100%);
}

.tc-table thead th {
  text-align: left;
  font-size: 12px;
  color: #fff;
  padding: 11px 10px;
  background: transparent;
  border-bottom: 2px solid rgba(93, 14, 28, 0.5);
}

.tc-table tbody td {
  padding: 10px;
  font-size: 13px;
  color: #1a1f2a;
  border-bottom: 1px solid rgba(181, 34, 53, 0.1);
  background: #fff;
}

.tc-table tbody tr:hover td {
  background: #fff6f7;
}

.tc-status {
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

.tc-list {
  display: grid;
  gap: 8px;
}

.tc-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-radius: 10px;
  padding: 9px 10px;
  border: 1px solid rgba(161, 24, 43, 0.18);
  background: linear-gradient(140deg, #fff, #fff4f6 60%, #ffedf0);
}

.tc-list-title {
  font-size: 13px;
  font-weight: 700;
  color: #171720;
}

.tc-list-sub {
  margin-top: 2px;
  font-size: 11px;
  color: #6b7280;
}

.tc-list-tail {
  font-size: 18px;
  font-weight: 800;
  color: #b91c1c;
  min-width: 22px;
  text-align: right;
}

.tc-stock-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  white-space: nowrap;
  flex-shrink: 0;
}

.tc-stock-out {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fca5a5;
}

.tc-stock-critical {
  background: #fff7ed;
  color: #9a3412;
  border: 1px solid #fdba74;
}

.tc-stock-low {
  background: #fefce8;
  color: #854d0e;
  border: 1px solid #fde047;
}

.tc-mini-btn {
  border: 1px solid rgba(17, 24, 39, 0.16);
  border-radius: 8px;
  font-size: 11px;
  padding: 5px 8px;
  background: #ffffff;
  color: #111827;
  cursor: pointer;
}

.tc-payment-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.tc-pay-item {
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 10px;
  padding: 8px;
  border: 1px solid rgba(155, 22, 40, 0.2);
  background: linear-gradient(140deg, #fff, #fff5f7 72%);
}

.tc-pay-logo,
.tc-pay-fallback {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  object-fit: contain;
  border: 1px solid rgba(17, 24, 39, 0.1);
  background: #fff;
}

.tc-pay-fallback {
  display: grid;
  place-items: center;
  color: #b91c1c;
  font-weight: 800;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: 1px solid rgba(17, 24, 39, 0.1);
  background: rgba(185, 28, 28, 0.07);
  flex: 0 0 28px;
}

.tc-pay-label {
  font-size: 12px;
  font-weight: 700;
}

.tc-pay-sub {
  font-size: 11px;
  color: #6b7280;
}

.tc-empty {
  padding: 10px;
  text-align: center;
  font-size: 12px;
  color: #6b7280;
}

@media (max-width: 760px) {
  .tc-page {
    padding: 10px;
  }

  .tc-hero-left {
    padding: 14px;
  }

  .tc-slider {
    height: 260px;
  }

  .tc-brand-name {
    font-size: 24px;
  }
}
</style>
