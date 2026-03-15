<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToast } from '../../../composables/useToast'
import { getAllHoaDon, getHoaDonChiTiet, getHoaDonPage } from '../../../services/hoaDonService'
import { getAllSanPham, getSanPhamPage } from '../../../services/sanPhamService'

const toast = useToast()
const loading = ref(false)
const invoices = ref([])
const statisticsError = ref('')
const BACKEND_URL = 'http://localhost:8080'

const toInputDate = (date) => {
  const d = new Date(date)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

const initialStartDate = (() => {
  const d = new Date()
  return toInputDate(new Date(d.getFullYear(), 0, 1))
})()

const initialEndDate = (() => {
  const d = new Date()
  return toInputDate(new Date(d.getFullYear(), 11, 31))
})()

const filters = ref({
  startDate: initialStartDate,
  endDate: initialEndDate,
  period: 'month'
})

const appliedFilters = ref({ ...filters.value })
const products = ref([])
const productNameByVariantId = ref(new Map())
const productNameByProductId = ref(new Map())
const productNameByMaSanPham = ref(new Map())
const hideTopProductRevenue = ref(false)
const searchMeta = ref({
  hasSearched: false,
  total: 0
})

const applyFilters = () => {
  if (filters.value.startDate && filters.value.endDate && filters.value.startDate > filters.value.endDate) {
    toast.warning('Từ ngày không được lớn hơn Đến ngày')
    return false
  }

  appliedFilters.value = { ...filters.value }
  return true
}

const completedStatuses = ['Đã giao', 'Hoàn thành']
const cancelledStatuses = ['Đã hủy']

const getInvoiceDetails = (invoice) => {
  if (!invoice || typeof invoice !== 'object') return []

  const detailCollections = [
    invoice.chiTietHoaDons,
    invoice.hoaDonChiTiets,
    invoice.chiTietDonHangs,
    invoice.chiTiets,
    invoice._chiTietHoaDons
  ]

  const found = detailCollections.find((items) => Array.isArray(items) && items.length > 0)
  return found || []
}

const parseDate = (value) => {
  if (!value) return null
  const date = value instanceof Date ? value : new Date(value)
  if (!Number.isNaN(date.getTime())) return date

  if (typeof value === 'string') {
    const match = value
      .trim()
      .match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})(?:\s+(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?)?$/)

    if (match) {
      const day = Number(match[1])
      const month = Number(match[2])
      const year = Number(match[3])
      const hour = Number(match[4] || 0)
      const minute = Number(match[5] || 0)
      const second = Number(match[6] || 0)

      const fallback = new Date(year, month - 1, day, hour, minute, second)
      if (!Number.isNaN(fallback.getTime())) return fallback
    }
  }

  return null
}

const inRangeInvoices = computed(() => {
  const start = parseDate(appliedFilters.value.startDate)
  const end = parseDate(appliedFilters.value.endDate)

  return invoices.value.filter((item) => {
    const d = parseDate(item.ngayTao)
    if (!d) return false
    if (start && d < start) return false
    if (end) {
      const endOfDay = new Date(end)
      endOfDay.setHours(23, 59, 59, 999)
      if (d > endOfDay) return false
    }
    return true
  })
})

const stats = computed(() => {
  const source = inRangeInvoices.value
  const totalRevenue = source.reduce((sum, i) => sum + (Number(i.thanhTien) || 0), 0)
  const totalOrders = source.length
  const completedOrders = source.filter((i) => completedStatuses.includes(i.trangThai)).length
  const cancelledOrders = source.filter((i) => cancelledStatuses.includes(i.trangThai)).length
  const pendingOrders = totalOrders - completedOrders - cancelledOrders

  return {
    totalRevenue,
    totalOrders,
    avgOrderValue: totalOrders > 0 ? Math.round(totalRevenue / totalOrders) : 0,
    completedOrders,
    pendingOrders: Math.max(pendingOrders, 0),
    cancelledOrders
  }
})

const revenueByMonth = computed(() => {
  const bucket = new Map()

  inRangeInvoices.value.forEach((item) => {
    const d = parseDate(item.ngayTao)
    if (!d) return
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    const current = bucket.get(key) || 0
    bucket.set(key, current + (Number(item.thanhTien) || 0))
  })

  // Fill missing months in selected range so the chart timeline is consistent.
  const start = parseDate(appliedFilters.value.startDate)
  const end = parseDate(appliedFilters.value.endDate)

  if (start && end && appliedFilters.value.period === 'month') {
    const cursor = new Date(start.getFullYear(), start.getMonth(), 1)
    const endMonth = new Date(end.getFullYear(), end.getMonth(), 1)

    while (cursor <= endMonth) {
      const key = `${cursor.getFullYear()}-${String(cursor.getMonth() + 1).padStart(2, '0')}`
      if (!bucket.has(key)) {
        bucket.set(key, 0)
      }
      cursor.setMonth(cursor.getMonth() + 1)
    }
  }

  const rows = [...bucket.entries()]
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([key, revenue]) => {
      const [, month] = key.split('-')
      return { month: `T${Number(month)}`, revenue }
    })

  return rows
})

const maxRevenue = computed(() => {
  return Math.max(...revenueByMonth.value.map((item) => item.revenue), 1)
})

const hasRevenueData = computed(() => {
  return revenueByMonth.value.length > 0 && revenueByMonth.value.some((item) => item.revenue > 0)
})

const topProducts = computed(() => {
  const productMap = new Map()

  const getProductStock = (product) => {
    const variants = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []
    if (variants.length > 0) {
      return variants.reduce((sum, v) => sum + (Number(v?.soLuong) || 0), 0)
    }
    return Number(product?.soLuong ?? product?.ton ?? 0) || 0
  }

  // Seed full catalog so admin can see all current products.
  products.value.forEach((product) => {
    const name = product?.tenSanPham || product?.maSanPham || `SP #${product?.id || 'N/A'}`
    if (!productMap.has(name)) {
      productMap.set(name, {
        name,
        sold: 0,
        stock: getProductStock(product),
        revenue: 0
      })
    }
  })

  inRangeInvoices.value.forEach((invoice) => {
    const details = getInvoiceDetails(invoice)
    details.forEach((d) => {
      // Try to get product name from various possible fields
      let name = null
      
      // Check nested sanPhamChiTiet.sanPham structure
      if (d?.sanPhamChiTiet?.sanPham?.tenSanPham) {
        name = d.sanPhamChiTiet.sanPham.tenSanPham
      }
      // Check direct sanPham reference
      else if (d?.sanPham?.tenSanPham) {
        name = d.sanPham.tenSanPham
      }
      // Check direct tenSanPham field
      else if (d?.tenSanPham) {
        name = d.tenSanPham
      }
      // Fallback to product code if available
      else if (d?.sanPhamChiTiet?.sanPham?.maSanPham) {
        name = d.sanPhamChiTiet.sanPham.maSanPham
      }
      else if (d?.sanPham?.maSanPham) {
        name = d.sanPham.maSanPham
      }
      else if (d?.maSanPham) {
        name = productNameByMaSanPham.value.get(d.maSanPham) || d.maSanPham
      }
      
      // If still no name, use ID as last resort
      if (!name) {
        const variantId = d?.sanPhamChiTietId ?? d?.idSanPhamChiTiet ?? d?.spctId ?? d?.sanPhamChiTiet?.id
        const productId = d?.sanPhamId ?? d?.sanPham?.id ?? d?.sanPhamChiTiet?.sanPham?.id

        if (variantId && productNameByVariantId.value.has(variantId)) {
          name = productNameByVariantId.value.get(variantId)
        } else if (productId && productNameByProductId.value.has(productId)) {
          name = productNameByProductId.value.get(productId)
        }
      }

      if (!name) {
        const fallbackId = d?.sanPhamChiTietId || d?.sanPhamId || d?.id
        name = fallbackId ? `Sản phẩm #${fallbackId}` : 'Sản phẩm không xác định'
      }

      const sold = Number(d?.soLuong ?? d?.quantity ?? 0) || 0
      const lineRevenue = Number(d?.thanhTien ?? d?.tongTien) || 0
      const unitPrice = Number(d?.donGia ?? d?.giaBan ?? d?.sanPhamChiTiet?.giaBan) || 0
      const revenue = lineRevenue > 0 ? lineRevenue : sold * unitPrice
      
      const prev = productMap.get(name) || { name, sold: 0, stock: 0, revenue: 0 }

      productMap.set(name, {
        name,
        sold: prev.sold + sold,
        stock: prev.stock || 0,
        revenue: prev.revenue + revenue
      })
    })
  })

  // Show all products — including those with zero sales
  const rows = [...productMap.values()]
    .sort((a, b) => {
      if (b.sold !== a.sold) return b.sold - a.sold
      return a.name.localeCompare(b.name, 'vi')
    })

  return rows.length > 0 ? rows : [
    { name: 'Chưa có dữ liệu sản phẩm', sold: 0, stock: 0, revenue: 0 }
  ]
})

const yAxisTicks = computed(() => {
  const max = maxRevenue.value || 1
  const steps = 4
  return Array.from({ length: steps + 1 }, (_, idx) => {
    const value = Math.round((max * (steps - idx)) / steps)
    return {
      label: formatCurrency(value)
    }
  })
})

const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(value || 0)
}

const isBackendUnavailableError = (error) => {
  if (!error) return false
  if (!error?.response) return true
  const status = Number(error?.response?.status || 0)
  return !status || status >= 500
}

const getStatisticsErrorMessage = (error) => {
  if (isBackendUnavailableError(error)) {
    return `Không kết nối được backend tại ${BACKEND_URL}. Hãy khởi động API rồi tải lại trang.`
  }
  return error?.response?.data?.message || 'Không tải được dữ liệu thống kê doanh thu.'
}

const applyStatisticsFilters = async () => {
  if (!applyFilters()) return
  searchMeta.value = {
    hasSearched: true,
    total: searchMeta.value.total
  }
  await fetchStatistics(false)
}

const loadStatistics = () => {
  fetchStatistics(true)
}

const exportReport = () => {
  toast.info('Đang xuất báo cáo...')
}

const extractList = (payload) => {
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.content)) return payload.content
  return []
}

const fetchAllPages = async (fetchPageFn, fallbackFn) => {
  const firstRes = await fallbackFn()
  const firstPayload = firstRes?.data
  const firstItems = extractList(firstPayload)

  if (!firstPayload || typeof firstPayload !== 'object' || !Array.isArray(firstPayload.content)) {
    return firstItems
  }

  const totalPages = Number(firstPayload.totalPages) || 1
  if (totalPages <= 1) return firstItems

  const pageCalls = []
  for (let page = 1; page < totalPages; page += 1) {
    pageCalls.push(fetchPageFn(page))
  }

  const pageResponses = await Promise.all(pageCalls)
  const restItems = pageResponses.flatMap((res) => extractList(res?.data))
  return [...firstItems, ...restItems]
}

const fetchStatistics = async (showRefreshToast = false) => {
  loading.value = true
  statisticsError.value = ''
  try {
    const source = await fetchAllPages(
      (page) => getHoaDonPage({ page, size: 50 }),
      () => getAllHoaDon()
    )

    const enrichedInvoices = await Promise.all(
      source.map(async (invoice) => {
        const normalized = { ...invoice }

        if (getInvoiceDetails(normalized).length > 0 || !normalized?.id) {
          return normalized
        }

        try {
          const detailRes = await getHoaDonChiTiet(normalized.id)
          if (Array.isArray(detailRes.data)) {
            normalized._chiTietHoaDons = detailRes.data
          } else if (Array.isArray(detailRes.data?.content)) {
            normalized._chiTietHoaDons = detailRes.data.content
          } else {
            normalized._chiTietHoaDons = []
          }
        } catch {
          normalized._chiTietHoaDons = []
        }

        return normalized
      })
    )

    invoices.value = enrichedInvoices
    if (searchMeta.value.hasSearched) {
      searchMeta.value = {
        hasSearched: true,
        total: inRangeInvoices.value.length
      }
    }
    if (showRefreshToast) {
      toast.success('Đã cập nhật số liệu doanh thu')
    }
  } catch (err) {
    invoices.value = []
    statisticsError.value = getStatisticsErrorMessage(err)
    if (showRefreshToast) {
      toast.error(statisticsError.value)
    }
    console.error('Load statistics failed:', err)
  } finally {
    loading.value = false
  }
}

const fetchProducts = async () => {
  try {
    const source = await fetchAllPages(
      (page) => getSanPhamPage({ page, size: 100 }),
      () => getAllSanPham()
    )

    products.value = source

    const byVariant = new Map()
    const byProduct = new Map()

    source.forEach((product) => {
      const productName = product?.tenSanPham || product?.maSanPham || `SP #${product?.id || 'N/A'}`
      if (product?.id != null) {
        byProduct.set(product.id, productName)
      }

      const variants = Array.isArray(product?.sanPhamChiTiets) ? product.sanPhamChiTiets : []
      variants.forEach((variant) => {
        if (variant?.id != null) {
          byVariant.set(variant.id, productName)
        }
      })
    })

    productNameByVariantId.value = byVariant
    productNameByProductId.value = byProduct

    const byMaSanPham = new Map()
    source.forEach((product) => {
      const productName = product?.tenSanPham || product?.maSanPham || `SP #${product?.id || 'N/A'}`
      if (product?.maSanPham) {
        byMaSanPham.set(product.maSanPham, productName)
      }
    })
    productNameByMaSanPham.value = byMaSanPham
  } catch (err) {
    if (!statisticsError.value) {
      statisticsError.value = getStatisticsErrorMessage(err)
    }
    console.error('Load products failed:', err)
  }
}

onMounted(() => {
  applyFilters()
  fetchProducts()
  fetchStatistics(false)
  searchMeta.value = {
    hasSearched: true,
    total: 0
  }
})
</script>

<template>
  <main class="wrap">
    <div class="card thong-ke-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Thống kê doanh thu</h1>
        <p class="page-subtitle">Báo cáo doanh thu và đơn hàng</p>
      </div>
      <div class="header-actions">
        <button class="btn-secondary" @click="loadStatistics">
          <span class="material-icons-outlined">refresh</span>
          Làm mới
        </button>
        <button class="btn-primary" @click="exportReport">
          <span class="material-icons-outlined">download</span>
          Xuất báo cáo
        </button>
      </div>
    </div>
    <div v-if="statisticsError" class="stats-alert">
      {{ statisticsError }}
    </div>

    <!-- Filters -->
    <div class="filter-section">
      <div class="filter-group">
        <div class="filter-item">
          <label>Từ ngày</label>
          <input 
            v-model="filters.startDate" 
            type="date" 
            class="form-input"
          />
        </div>
        <div class="filter-item">
          <label>Đến ngày</label>
          <input 
            v-model="filters.endDate" 
            type="date" 
            class="form-input"
          />
        </div>
        <div class="filter-item">
          <label>Khoảng thời gian</label>
          <select v-model="filters.period" class="form-select">
            <option value="day">Theo ngày</option>
            <option value="week">Theo tuần</option>
            <option value="month">Theo tháng</option>
            <option value="year">Theo năm</option>
          </select>
        </div>
      </div>
      <button class="btn-search" :disabled="loading" @click="applyStatisticsFilters">
        <span class="material-icons-outlined">search</span>
        Tìm kiếm
      </button>
    </div>
    <p class="search-meta" v-if="searchMeta.hasSearched">
      Hiển thị {{ searchMeta.total }} đơn hàng trong khoảng thời gian đã chọn.
    </p>

    <!-- Main Stats -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">Tổng doanh thu</span>
          <div class="stat-icon revenue">
            <span class="material-icons-outlined">payments</span>
          </div>
        </div>
        <div class="stat-value">{{ formatCurrency(stats.totalRevenue) }}</div>
        <div class="stat-change positive">
          <span class="material-icons-outlined">trending_up</span>
          +12.5% so với tháng trước
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">Tổng đơn hàng</span>
          <div class="stat-icon orders">
            <span class="material-icons-outlined">receipt_long</span>
          </div>
        </div>
        <div class="stat-value">{{ stats.totalOrders }}</div>
        <div class="stat-change positive">
          <span class="material-icons-outlined">trending_up</span>
          +8.3% so với tháng trước
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-header">
          <span class="stat-label">Giá trị TB/đơn</span>
          <div class="stat-icon avg">
            <span class="material-icons-outlined">calculate</span>
          </div>
        </div>
        <div class="stat-value">{{ formatCurrency(stats.avgOrderValue) }}</div>
        <div class="stat-change positive">
          <span class="material-icons-outlined">trending_up</span>
          +3.8% so với tháng trước
        </div>
      </div>
    </div>

    <!-- Order Status Stats -->
    <div class="stats-grid secondary">
      <div class="stat-card small">
        <div class="stat-icon-small success">
          <span class="material-icons-outlined">check_circle</span>
        </div>
        <div class="stat-content">
          <div class="stat-label">Đơn hoàn thành</div>
          <div class="stat-value-small">{{ stats.completedOrders }}</div>
        </div>
      </div>

      <div class="stat-card small">
        <div class="stat-icon-small warning">
          <span class="material-icons-outlined">pending</span>
        </div>
        <div class="stat-content">
          <div class="stat-label">Đơn chờ xử lý</div>
          <div class="stat-value-small">{{ stats.pendingOrders }}</div>
        </div>
      </div>

      <div class="stat-card small">
        <div class="stat-icon-small danger">
          <span class="material-icons-outlined">cancel</span>
        </div>
        <div class="stat-content">
          <div class="stat-label">Đơn đã hủy</div>
          <div class="stat-value-small">{{ stats.cancelledOrders }}</div>
        </div>
      </div>
    </div>

    <!-- Revenue Chart -->
    <div class="chart-section">
      <div class="section-header">
        <h3>Doanh thu theo tháng</h3>
        <p class="section-subtitle">Biểu đồ doanh thu theo khoảng thời gian đã chọn</p>
      </div>
      <div v-if="hasRevenueData" class="chart-container">
        <div class="chart-layout">
          <div class="chart-y-axis">
            <div v-for="(tick, idx) in yAxisTicks" :key="idx" class="y-tick">{{ tick.label }}</div>
          </div>
          <div class="chart-bars" :style="{ gridTemplateColumns: `repeat(${Math.max(revenueByMonth.length, 1)}, minmax(0, 1fr))` }">
            <div 
              v-for="item in revenueByMonth" 
              :key="item.month"
              class="chart-bar-wrapper"
            >
              <div class="bar-track">
                <div class="chart-bar" :style="{ height: (item.revenue / maxRevenue * 100) + '%' }">
                  <div class="bar-value">{{ formatCurrency(item.revenue) }}</div>
                </div>
              </div>
              <div class="bar-label">{{ item.month }}</div>
              <div class="bar-amount">{{ formatCurrency(item.revenue) }}</div>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="chart-empty">Chưa có dữ liệu doanh thu theo khoảng thời gian đã chọn.</div>
    </div>

    <!-- Top Products -->
    <div class="table-section">
      <div class="section-header">
        <h3>Sản phẩm bán chạy</h3>
      </div>
      <div class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th class="text-center">STT</th>
              <th>Tên sản phẩm</th>
              <th class="text-center">Số lượng bán</th>
              <th class="text-center">Doanh thu</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(product, index) in topProducts" :key="index">
              <td class="text-center">{{ index + 1 }}</td>
              <td>{{ product.name }}</td>
              <td class="text-center">{{ product.sold > 0 ? product.sold : product.stock }}</td>
              <td class="text-center">{{ hideTopProductRevenue ? 'Đang cập nhật' : formatCurrency(product.revenue) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap');
@import url('https://fonts.googleapis.com/icon?family=Material+Icons+Outlined');

.thong-ke-page {
  font-family: 'Be Vietnam Pro', 'Segoe UI', Tahoma, sans-serif;
  padding: 24px 28px;
  background: transparent;
  min-height: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.03em;
  margin: 0 0 6px;
}

.page-subtitle {
  font-size: 15px;
  color: #64748b;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn-primary,
.btn-secondary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: #dc2626;
  color: white;
}

.btn-primary:hover {
  background: #b91c1c;
  transform: translateY(-1px);
}

.btn-secondary {
  background: white;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}

.filter-section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  border: 1px solid #e5e7eb;
  display: flex;
  gap: 16px;
  align-items: flex-end;
}

.search-meta {
  margin: -12px 0 18px;
  font-size: 13px;
  color: #4b5563;
}

.stats-alert {
  margin-bottom: 20px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #fecaca;
  background: #fff1f2;
  color: #991b1b;
  font-size: 14px;
  font-weight: 600;
}

.filter-group {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  flex: 1;
}

.filter-item {
  display: flex;
  flex-direction: column;
}

.filter-item label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
  white-space: nowrap;
}

.form-input,
.form-select {
  width: 100%;
  padding: 11px 14px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
}

.form-input:focus,
.form-select:focus {
  border-color: #111827;
}

.btn-search {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 11px 20px;
  background: #dc2626;
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 160px;
  width: auto;
  height: 44px;
  flex-shrink: 0;
}

.btn-search:hover {
  background: #b91c1c;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stats-grid.secondary {
  grid-template-columns: repeat(3, 1fr);
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stat-card.small {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.revenue {
  background: #dcfce7;
  color: #16a34a;
}

.stat-icon.orders {
  background: #dbeafe;
  color: #2563eb;
}

.stat-icon.avg {
  background: #fef3c7;
  color: #f59e0b;
}

.stat-icon .material-icons-outlined {
  font-size: 24px;
}

.stat-icon-small {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-small.success {
  background: #dcfce7;
  color: #16a34a;
}

.stat-icon-small.warning {
  background: #fef3c7;
  color: #f59e0b;
}

.stat-icon-small.danger {
  background: #fee2e2;
  color: #ef4444;
}

.stat-icon-small .material-icons-outlined {
  font-size: 28px;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #111827;
  margin-bottom: 8px;
}

.stat-value-small {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin-top: 4px;
}

.stat-change {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
}

.stat-change.positive {
  color: #16a34a;
}

.stat-change .material-icons-outlined {
  font-size: 18px;
}

.chart-section,
.table-section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #e5e7eb;
  margin-bottom: 24px;
}

.section-header {
  margin-bottom: 24px;
}

.section-header h3 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.section-subtitle {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.chart-container {
  min-height: 380px;
  padding: 12px 0 4px;
  position: relative;
}

.chart-layout {
  display: grid;
  grid-template-columns: 112px 1fr;
  gap: 16px;
  align-items: stretch;
  height: 100%;
}

.chart-y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 4px 0 62px;
}

.y-tick {
  font-size: 11px;
  color: #6b7280;
}

.chart-bars {
  display: grid;
  align-items: stretch;
  height: 100%;
  gap: 12px;
  padding: 0 12px 18px;
  overflow: hidden;
  border-left: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.chart-bar-wrapper {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  position: relative;
}

.bar-track {
  height: 240px;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.chart-bar {
  width: min(72px, 100%);
  background: linear-gradient(180deg, #dc2626 0%, #b91c1c 100%);
  border-radius: 8px 8px 0 0;
  position: relative;
  min-height: 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 6px;
  transition: all 0.3s;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(220, 38, 38, 0.2);
}

.chart-bar:hover {
  opacity: 0.9;
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(220, 38, 38, 0.3);
}

.bar-value {
  font-size: 10px;
  font-weight: 600;
  color: white;
  white-space: normal;
  max-width: 100%;
  padding: 0 4px;
  word-break: break-word;
  text-align: center;
  line-height: 1.1;
}

.bar-label {
  margin-top: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #111827;
}

.bar-amount {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
}

.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  background: #f9fafb;
  padding: 12px 16px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  border-bottom: 1px solid #e5e7eb;
}

.data-table td {
  padding: 16px;
  border-bottom: 1px solid #f3f4f6;
  font-size: 14px;
  color: #374151;
}

.data-table tbody tr:hover {
  background: #f9fafb;
}

.text-center {
  text-align: center;
}

.text-right {
  text-align: right;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .stats-grid.secondary {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .thong-ke-page {
    padding: 18px 14px;
  }

  .page-title {
    font-size: 18px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 16px;
  }
  
  .header-actions {
    width: 100%;
  }
  
  .btn-primary,
  .btn-secondary {
    flex: 1;
  }
  
  .filter-section {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-group {
    grid-template-columns: 1fr;
  }

  .btn-search {
    width: 100%;
  }
  
  .chart-container {
    min-height: 280px;
  }

  .chart-layout {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .chart-y-axis {
    display: none;
  }
  
  .chart-bars {
    gap: 10px;
    padding: 0 8px 14px;
  }

  .bar-track {
    height: 180px;
  }
  
  .bar-amount {
    font-size: 10px;
  }
}

.chart-empty {
  padding: 60px 20px;
  text-align: center;
  color: #9ca3af;
  font-size: 15px;
  background: #f9fafb;
  border-radius: 12px;
}
</style>
