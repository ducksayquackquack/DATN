<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from "vue"
import { useRoute, useRouter } from "vue-router"
import { Eye, ShoppingCart } from "lucide-vue-next"
import { getAllSanPham } from '../../services/sanPhamService'
import logo from "../../assets/img/logo/new logo.png?url"
import SiteNav from '../../components/SiteNav.vue'
import CustomerFooter from '../../components/customer/CustomerFooter.vue'
import { useToast } from '../../composables/useToast'
import { resolveApiOrigin } from '../../utils/apiOrigin'
import { getProductImageOverride } from '../../utils/productImageOverrides'
import img1 from "../../assets/img/Jackets/Áo bomber da lộn DirtyWave.jpg?url"
import img2 from "../../assets/img/Jackets/Áo bomber dáng lửng.jpg?url"
import img3 from "../../assets/img/Jackets/Áo bomber giả da DirtyWave.jpg?url"
import img4 from "../../assets/img/Jackets/Áo bomber nhẹ vải cotton DirtyWave.jpg?url"
import img5 from "../../assets/img/Jackets/Áo hoodie kéo khoá dáng hộp DirtyWave.jpg?url"
import img6 from "../../assets/img/Jackets/Áo hoodie kéo khoá in hình DirtyWave.jpg?url"
import img7 from "../../assets/img/Jackets/Áo hoodie kéo khoá Jacket DirtyWave.jpg?url"
import img8 from "../../assets/img/Jackets/Áo khoác coach cách nhiệt vải Timberland.jpg?url"
import img9 from "../../assets/img/Jackets/Áo khoac coach da ASOS DirtyWave.jpg?url"
import img10 from "../../assets/img/Jackets/Áo khoác coach giả da DirtyWave.jpg?url"
import img11 from "../../assets/img/Jackets/Áo khoác coach lông cừu DirtyWave.jpg?url"
import momo from "../../assets/img/payments/momo.png?url"
import visa from "../../assets/img/payments/visa.png?url"
import mastercard from "../../assets/img/payments/mastercard.png?url"
import vnpay from "../../assets/img/payments/vnpay.png?url"
import { readCartObject, writeCartObject } from "../../utils/cartStorage"
const router = useRouter()
const route = useRoute()
const { success: toastSuccess } = useToast()
const BACKEND_ORIGIN = resolveApiOrigin().replace(/\/$/, '')
const VND = n => new Intl.NumberFormat("vi-VN").format(n) + "₫"
const CART_UPDATED_EVENT = "dirtywave:cart-updated"

const activeFilter = ref(null)
const selectedPriceTier = ref(null)
const activeMood = ref("Minimal")
const sectionPulse = ref("")
const toastMessage = ref("")
const toastVisible = ref(false)
let toastTimer = null
const email = ref("")
const year = new Date().getFullYear()

const getImage = (id) => {
  return new URL(`../../assets/img/product${id}.jpg`, import.meta.url).href
}

const allProducts = [
  // BOMBER
  {id:1,name:"Áo Bomber Da Lộn DirtyWave",cat:"Bomber",price:649000,old:799000,tag:"AIRFLEX",img:img1},
  {id:2,name:"Áo Bomber Dáng Lửng",cat:"Bomber",price:399000,old:null,tag:"New",img:img2},
  {id:3,name:"Áo Bomber Giả Da DirtyWave",cat:"Bomber",price:329000,old:389000,tag:"Best",img:img3},
  {id:4,name:"Áo Bomber Cotton DirtyWave",cat:"Bomber",price:459000,old:null,tag:"Office",img:img4},

  // HOODIE
  {id:5,name:"Hoodie Zip Dáng Hộp DirtyWave",cat:"Hoodie",price:499000,old:599000,tag:"Daily",img:img5},
  {id:6,name:"Hoodie Zip In Hình DirtyWave",cat:"Hoodie",price:699000,old:null,tag:"Layer",img:img6},
  {id:7,name:"Hoodie Zip Jacket DirtyWave",cat:"Hoodie",price:559000,old:null,tag:"New",img:img7},

  // COACH JACKET
  {id:8,name:"Coach Cách Nhiệt Timberland",cat:"Coach",price:799000,old:899000,tag:"Drop",img:img8},
  {id:9,name:"Coach Da ASOS DirtyWave",cat:"Coach",price:899000,old:null,tag:"Premium",img:img9},
  {id:10,name:"Coach Giả Da DirtyWave",cat:"Coach",price:699000,old:null,tag:"Trend",img:img10},
  {id:11,name:"Coach Lông Cừu DirtyWave",cat:"Coach",price:399000,old:559000,tag:"399K",img:img11},

  // BOMBER SALE
  {id:12,name:"Bomber Dáng Lửng DirtyWave",cat:"Bomber",price:299000,old:459000,tag:"299K",img:img2},
  {id:13,name:"Bomber Giả Da DirtyWave",cat:"Bomber",price:399000,old:559000,tag:"399K",img:img3},
  {id:14,name:"Bomber Vải Cotton DirtyWave",cat:"Bomber",price:299000,old:429000,tag:"299K",img:img4},

  // OTHER PRODUCTS
  {id:15,name:"Áo Thun Cotton 220gsm",cat:"Áo thun",price:329000,old:389000,tag:"Best",img:img5},
  {id:16,name:"Polo Basic Dệt Mịn",cat:"Polo",price:399000,old:null,tag:"New",img:img6},
  {id:17,name:"Jeans Slim Fit Co Giãn",cat:"Jeans",price:649000,old:799000,tag:"AIRFLEX",img:img8},
  {id:18,name:"Quần Kaki Form Straight",cat:"Kaki/Chino",price:499000,old:599000,tag:"Daily",img:img9},
  {id:19,name:"Quần Tây Minimal DirtyWave",cat:"Quần tây",price:559000,old:null,tag:"Office",img:img10},
  {id:20,name:"Quần Jogger Street",cat:"Jogger",price:399000,old:459000,tag:"Trend",img:img7},
]

const products = ref(allProducts)

const isAbsoluteUrl = (value = '') => /^https?:\/\//i.test(value) || /^data:image\//i.test(value)

const toImageUrl = (value) => {
  const raw = String(value || '').trim()
  if (!raw) return ''
  if (isAbsoluteUrl(raw)) return raw
  const normalized = raw.replace(/\\/g, '/')
  const uploadsMatch = normalized.match(/(?:^|\/)(uploads\/.*)$/i)
  if (uploadsMatch?.[1]) return `${BACKEND_ORIGIN}/${uploadsMatch[1]}`
  if (normalized.startsWith('/uploads/')) return `${BACKEND_ORIGIN}${normalized}`
  if (normalized.startsWith('uploads/')) return `${BACKEND_ORIGIN}/${normalized}`
  if (normalized.startsWith('assets/') || normalized.startsWith('img/')) return `/${normalized}`
  if (normalized.startsWith('/')) return normalized
  return normalized
}

const pickImageValue = (entry) => {
  if (!entry) return ''
  if (typeof entry === 'string') return toImageUrl(entry)
  if (Array.isArray(entry)) {
    for (const child of entry) {
      const found = pickImageValue(child)
      if (found) return found
    }
    return ''
  }
  if (typeof entry === 'object') {
    const keys = ['anh', 'hinhAnh', 'image', 'imageUrl', 'images', 'listAnh', 'anhChinh', 'url', 'path']
    for (const key of keys) {
      const found = pickImageValue(entry[key])
      if (found) return found
    }
  }
  return ''
}

const mapBackendProductToHomeCard = (item, fallbackIndex = 0) => {
  const variants = Array.isArray(item?.sanPhamChiTiets) ? item.sanPhamChiTiets : []
  const variantPrices = variants.map((variant) => Number(variant?.giaBan || 0)).filter((n) => n > 0)
  const variantOldPrices = variants.map((variant) => Number(variant?.giaNhap || 0)).filter((n) => n > 0)
  const id = Number(item?.id)
  const overrideImage = getProductImageOverride({ id, maSanPham: item?.maSanPham })[0]

  const variantStock = variants.reduce((sum, v) => sum + Number(v?.soLuong || 0), 0)
  const totalStock = variantStock > 0 ? variantStock : Number(item?.soLuong || 0)

  return {
    id,
    name: String(item?.tenSanPham || item?.name || `Sản phẩm ${id || fallbackIndex + 1}`),
    cat: String(item?.loai?.tenLoai || item?.danhMuc?.tenDanhMuc || 'Thời trang nam'),
    price: variantPrices.length ? Math.min(...variantPrices) : Number(item?.giaBan || item?.gia || 0),
    old: variantOldPrices.length ? Math.max(...variantOldPrices) : Number(item?.giaGoc || 0) || null,
    tag: totalStock > 0 ? 'New' : 'Sold out',
    img: overrideImage || pickImageValue([item, variants]) || allProducts[fallbackIndex % allProducts.length]?.img || img1,
  }
}

watch(() => route.query.category, async (cat) => {
  if (cat) {
    activeFilter.value = String(cat)
    await nextTick()
    scrollToId("best")
    pulseSection("best")
  }
})

onMounted(() => {

  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add("show")
      }
    })
  }, { threshold: 0.1 })

  document.querySelectorAll(".card").forEach(el => {
    el.classList.add("fade-in")
    observer.observe(el)
  })

  loadHomeBackendProducts()

  if (route.query.category) {
    activeFilter.value = String(route.query.category)
  }

  const savedCart = readCartObject()
  if (savedCart && typeof savedCart === "object") {
    cart.value = savedCart
  }

})

const handleNavAnchor = (anchor) => {
  scrollToId(anchor)
}

const scrollToId = id => {
  const el = document.getElementById(id)
  if (!el) return
  const headerOffset = 96
  const top = el.getBoundingClientRect().top + window.scrollY - headerOffset
  window.scrollTo({ top, behavior: "smooth" })
}

const pulseSection = (id) => {
  sectionPulse.value = id
  window.setTimeout(() => {
    if (sectionPulse.value === id) sectionPulse.value = ""
  }, 700)
}

const filterBy = cat => {
  activeFilter.value = cat
  scrollToId("best")
  pulseSection("best")
}

const clearFilter = () =>
  activeFilter.value = null

const openShop = () => {
  router.push("/san-pham")
}

const PRICE_TIERS = [
  { label: "199K", value: 199000 },
  { label: "299K", value: 299000 },
  { label: "399K", value: 399000 }
]

const STYLE_MOODS = {
  Minimal: { category: "Bomber", priceCap: 399000, desc: "Gọn, đứng phom, dễ phối hàng ngày." },
  Street: { category: "Hoodie", priceCap: 499000, desc: "Năng động, layer ổn, hợp dạo phố." },
  Office: { category: "Coach", priceCap: 799000, desc: "Lịch sự vừa đủ, vẫn giữ chất riêng." }
}

const outletBaseProducts = computed(() => products.value.filter(x => [11, 12, 13, 14].includes(x.id)))

const filteredOutletProducts = computed(() => {
  if (!selectedPriceTier.value) return outletBaseProducts.value
  return outletBaseProducts.value.filter(product => product.price <= selectedPriceTier.value)
})

const moodDescription = computed(() => STYLE_MOODS[activeMood.value]?.desc || "")

const applyPriceTier = (priceCap) => {
  selectedPriceTier.value = priceCap
  scrollToId("outlet")
  pulseSection("outlet")
}

const clearPriceTier = () => {
  selectedPriceTier.value = null
}

const applyMood = (mood) => {
  activeMood.value = mood
  const config = STYLE_MOODS[mood]
  if (!config) return
  activeFilter.value = config.category
  selectedPriceTier.value = config.priceCap
  scrollToId("best")
  pulseSection("best")
  toast(`Đang gợi ý phong cách ${mood}`)
}

const filteredBest = computed(() => {
  if(!activeFilter.value) return products.value
  return products.value.filter(p => p.cat === activeFilter.value)
})

const toast = msg => {
  toastMessage.value = msg
  toastVisible.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastVisible.value = false
  }, 5000)
}

// CART STATE
const cart = ref({})

const notifyCartUpdated = () => {
  window.dispatchEvent(new Event(CART_UPDATED_EVENT))
}

const addToCart = (id) => {
  if (!cart.value[id]) cart.value[id] = 0
  cart.value[id]++

  cart.value = { ...cart.value }   // force Vue update
  notifyCartUpdated()
  toastSuccess('Đã thêm vào giỏ hàng')
}

watch(
  cart,
  (nextCart) => {
    writeCartObject(nextCart)
  },
  { deep: true }
)

const copyVoucher = code => {
  navigator.clipboard?.writeText(code)
  toast("Đã sao chép mã: " + code)
}

const subscribe = () => {
  if(!email.value || !email.value.includes('@')) return toast('Nhập email hợp lệ')
  email.value = ''
  toast('Đã đăng ký nhận tin (demo)')
}

const selectedProduct = ref(null)
const quickQty = ref(1)
const quickSize = ref("S")
const quickColorIndex = ref(0)
const homeBackendProducts = ref([])

const normalizeKeyword = (value = '') =>
  String(value).normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim()

const isActiveStatus = (value = '') => {
  const normalized = normalizeKeyword(value)
  if (!normalized) return true
  if (normalized.includes('ngung')) return false
  if (normalized.includes('inactive')) return false
  if (normalized.includes('an')) return false
  return true
}

const isBackendProductActive = (item) => {
  if (!isActiveStatus(item?.trangThai || item?.status)) return false

  const variants = Array.isArray(item?.sanPhamChiTiets) ? item.sanPhamChiTiets : []
  if (!variants.length) return true

  return variants.some((variant) => isActiveStatus(variant?.trangThai || variant?.status))
}

const loadHomeBackendProducts = async () => {
  try {
    const response = await getAllSanPham()
    homeBackendProducts.value = Array.isArray(response?.data) ? response.data : []
    if (homeBackendProducts.value.length) {
      products.value = homeBackendProducts.value
        .filter((item) => isBackendProductActive(item))
        .map((item, index) => mapBackendProductToHomeCard(item, index))
        .filter((item) => Number.isFinite(item.id) && item.id > 0)
    }
  } catch {
    homeBackendProducts.value = []
    products.value = allProducts
  }
}

const getQuickProductCode = (product) => {
  if (!product) return ''
  const byId = homeBackendProducts.value.find(item => Number(item?.id) === Number(product.id))
  if (byId?.maSanPham) return byId.maSanPham
  const localName = normalizeKeyword(product.name)
  const byName = homeBackendProducts.value.find(item => normalizeKeyword(item?.tenSanPham) === localName)
  return byName?.maSanPham || String(product.id)
}

const QUICK_SIZES = ["S", "M", "L", "XL"]
const QUICK_PALETTES = [
  ["#a95d4e", "#111827", "#e6e0d4"],
  ["#7b3f2d", "#0f172a", "#d1c5ae"],
  ["#6f4f37", "#111111", "#d8d0c2"]
]

const getQuickPalette = (id) => QUICK_PALETTES[Number(id || 0) % QUICK_PALETTES.length]

const openModal = (product) => {
  selectedProduct.value = product
  quickQty.value = 1
  quickSize.value = "S"
  quickColorIndex.value = 0
}

const openProductDetail = (productId) => {
  router.push('/product/' + productId)
}
const cartCount = computed(() => {
  return Object.values(cart.value).reduce((a,b)=>a+b,0)
})

const closeModal = () => {
  selectedProduct.value = null
}

const quickDecrease = () => {
  quickQty.value = Math.max(1, quickQty.value - 1)
}

const quickIncrease = () => {
  quickQty.value += 1
}

const quickAddToCart = () => {
  if (!selectedProduct.value?.id) return
  addToCart(selectedProduct.value.id)
}

onUnmounted(() => {
  if (toastTimer) {
    clearTimeout(toastTimer)
    toastTimer = null
  }
})


</script>

<template>
<div class="home-root">

<SiteNav :cart-count="cartCount" @require-filter="filterBy" @require-anchor="handleNavAnchor" />

<!-- Hero -->
<main class="hero">
<div class="container">
<div class="hero-wrap">

<div class="hero-card">
<div class="hero-visual"></div>
<div class="content">
<button class="pill interactive-pill" @click="scrollToId('style-lab')">
Ưu đãi theo mốc đơn • Freeship
</button>

<h1>
Tủ đồ tối giản cho nam giới<br/>
ưu tiên phom, chất liệu và độ bền mặc
</h1>

<div class="row">
<button class="btn primary"
@click="scrollToId('best')">
Mua bán chạy
</button>

<button class="btn"
@click="scrollToId('new')">
Xem Hàng Mới Về
</button>

<button class="btn ghost"
@click="scrollToId('outlet')">
Vào Cửa Hàng
</button>
</div>

<div class="row" style="margin-top:12px">
<button class="pill interactive-pill" @click="toast('Hỗ trợ đổi size trong 7 ngày')">Đổi size nhanh</button>
<button class="pill interactive-pill" @click="toast('Hỗ trợ COD toàn quốc')">COD toàn quốc</button>
<button class="pill interactive-pill" @click="scrollToId('stores')">Hỗ trợ mỗi ngày</button>
</div>

</div>
</div>

<div class="hero-side">

<div class="mini-card reveal" @click="scrollToId('new')">
<span class="badge">NEW</span>
<div class="bg"></div>
<h3>Hàng Mới Về</h3>
<p>Drop mới theo mùa, giữ tinh thần gọn và dễ phối.</p>
<div style="margin-top:12px">
<button class="btn"
@click.stop="scrollToId('new')">
Xem ngay →
</button>
</div>
</div>

<div class="mini-card reveal" @click="scrollToId('outlet')">
<span class="badge">CỬA HÀNG</span>
<div class="bg"></div>
<h3>Giá tốt</h3>
<p>Khung giá rõ ràng cho các món nên có trong tủ đồ hằng ngày.</p>
<div style="margin-top:12px; display:flex; gap:10px; flex-wrap:wrap">
<button class="pill interactive-pill" @click.stop="applyPriceTier(199000)">199K</button>
<button class="pill interactive-pill" @click.stop="applyPriceTier(299000)">299K</button>
<button class="pill interactive-pill" @click.stop="applyPriceTier(399000)">399K</button>
</div>
<div style="margin-top:12px">
<button class="btn"
@click.stop="scrollToId('outlet')">
Vào Cửa Hàng →
</button>
</div>
</div>

</div>

</div>
</div>
</main>

<!-- Categories -->
<section id="category">
<div class="container">

<div class="section-head">
<div>
<h2>Danh mục nổi bật</h2>
</div>
<button class="btn" @click="openShop">
Xem tất cả →
</button>
</div>

<div class="tiles featured-cats">

<a class="tile" @click.prevent="filterBy('Hoodie')">
  <div class="tile-content">
    <div>
      <h3>Hoodie</h3>
      <span>Street • trẻ trung</span>
    </div>
    <img :src="img5" class="cat-img">
  </div>
</a>

<a class="tile" @click.prevent="filterBy('Bomber')">
  <div class="tile-content">
    <div>
      <h3>Bomber</h3>
      <span>Form rộng • cá tính</span>
    </div>
    <img :src="img1" class="cat-img">
  </div>
</a>

<a class="tile" @click.prevent="filterBy('Coach')">
  <div class="tile-content">
    <div>
      <h3>Coach Jacket</h3>
      <span>Minimal • dễ phối</span>
    </div>
    <img :src="img8" class="cat-img">
  </div>
</a>

</div>
</div>
</section>

<!-- Best sellers -->
<section id="best" :class="{ 'section-pulse': sectionPulse === 'best' }">
<div class="container">

<div class="section-head">
<div>
<h2>Sản phẩm bán chạy</h2>
</div>

<div style="display:flex; gap:10px; flex-wrap:wrap">

<button class="btn" @click="clearFilter()">
Tất cả
</button>

<button class="btn" @click="filterBy('Hoodie')">
Hoodie
</button>

<button class="btn" @click="filterBy('Bomber')">
Bomber
</button>

<button class="btn" @click="filterBy('Coach')">
Coach
</button>

</div>
</div>

<div class="cards">
<article
v-for="p in filteredBest.slice(0, 8)"
:key="p.id"
class="card">

<div class="thumb">
  <img :src="p.img" alt="" />
  <span class="thumb-badge">{{ p.tag }}</span>
  <div class="overlay">
    <button class="overlay-icon-btn" type="button" @click.stop="openModal(p)" aria-label="Xem nhanh">
      <Eye :size="16" />
    </button>
    <button class="overlay-icon-btn" type="button" @click.stop="openProductDetail(p.id)" aria-label="Mở chi tiết sản phẩm">
      <ShoppingCart :size="16" />
    </button>
  </div>
</div>

<div class="body">
<div class="title">{{ p.name }}</div>
<div class="meta">
<span>{{ p.cat }}</span>
<span class="price">
<b>{{ VND(p.price) }}</b>
<span v-if="p.old"
class="strike">
{{ VND(p.old) }}
</span>
</span>
</div>
</div>

<div class="footer">
<span class="chip">{{ p.tag }}</span>
<button class="btn"
@click="addToCart(p.id)">
Thêm giỏ
</button>
</div>

</article>
</div>

</div>
</section>

<!-- Style Lab -->
<section id="style-lab" class="style-lab">
  <div class="container">
    <div class="style-lab-wrap reveal">
      <div>
        <div class="pill">Style Lab</div>
        <h3>Chọn mood, nhận gợi ý outfit trong 1 chạm</h3>
        <p>{{ moodDescription }}</p>
      </div>

      <div class="style-moods">
        <button class="btn" :class="{ primary: activeMood === 'Minimal' }" @click="applyMood('Minimal')">Minimal</button>
        <button class="btn" :class="{ primary: activeMood === 'Street' }" @click="applyMood('Street')">Street</button>
        <button class="btn" :class="{ primary: activeMood === 'Office' }" @click="applyMood('Office')">Office</button>
      </div>

      <div class="style-lab-actions">
        <button class="btn primary" @click="copyVoucher('STYLELAB15')">Sao chép mã: STYLELAB15</button>
        <button class="btn" @click="openShop">Vào cửa hàng đầy đủ</button>
      </div>
    </div>
  </div>
</section>


<!-- New arrivals -->
<section id="new">
  <div class="container">
    <div class="section-head">
      <div>
        <h2>Hàng Mới Về</h2>
      </div>
      <button class="btn" @click="openShop">Xem thêm →</button>
    </div>

    <div class="cards">
      <article
        v-for="p in products.filter(x => [7,8,9,10].includes(x.id))"
        :key="p.id"
        class="card"
      >
        <div class="thumb">
          <img :src="p.img" alt="" />
          <span class="thumb-badge">{{ p.tag }}</span>
          <div class="overlay">
            <button class="overlay-icon-btn" type="button" @click.stop="openModal(p)" aria-label="Xem nhanh">
              <Eye :size="16" />
            </button>
            <button class="overlay-icon-btn" type="button" @click.stop="openProductDetail(p.id)" aria-label="Mở chi tiết sản phẩm">
              <ShoppingCart :size="16" />
            </button>
          </div>
        </div>
        <div class="body">
          <div class="title">{{ p.name }}</div>
          <div class="meta">
            <span>{{ p.cat }}</span>
            <span class="price">
              <b>{{ VND(p.price) }}</b>
              <span v-if="p.old" class="strike">
                {{ VND(p.old) }}
              </span>
            </span>
          </div>
        </div>
        <div class="footer">
          <span class="chip">{{ p.tag }}</span>
          <button class="btn" @click="addToCart(p.id)">
            Thêm giỏ
          </button>
        </div>
      </article>
    </div>
  </div>
</section>

<!-- Outlet -->
<section id="outlet" :class="{ 'section-pulse': sectionPulse === 'outlet' }">
  <div class="container">
    <div class="section-head">
      <div>
        <h2>Cửa Hàng</h2>
      </div>
      <div class="tier-row">
        <button
          v-for="tier in PRICE_TIERS"
          :key="tier.value"
          class="pill interactive-pill"
          :class="{ active: selectedPriceTier === tier.value }"
          @click="applyPriceTier(tier.value)"
        >
          {{ tier.label }}
        </button>
        <button v-if="selectedPriceTier" class="pill interactive-pill" @click="clearPriceTier">
          Bỏ lọc
        </button>
      </div>
    </div>

    <div class="cards">
      <article
        v-for="p in filteredOutletProducts"
        :key="p.id"
        class="card"
      >
        <div class="thumb">
          <img :src="p.img" alt="" />
          <span class="thumb-badge">{{ p.tag }}</span>
          <div class="overlay">
            <button class="overlay-icon-btn" type="button" @click.stop="openModal(p)" aria-label="Xem nhanh">
              <Eye :size="16" />
            </button>
            <button class="overlay-icon-btn" type="button" @click.stop="openProductDetail(p.id)" aria-label="Mở chi tiết sản phẩm">
              <ShoppingCart :size="16" />
            </button>
          </div>
        </div>
        <div class="body">
          <div class="title">{{ p.name }}</div>
          <div class="meta">
            <span>{{ p.cat }}</span>
            <span class="price">
              <b>{{ VND(p.price) }}</b>
              <span v-if="p.old" class="strike">
                {{ VND(p.old) }}
              </span>
            </span>
          </div>
        </div>
        <div class="footer">
          <span class="chip">{{ p.tag }}</span>
          <button class="btn" @click="addToCart(p.id)">
            Thêm giỏ
          </button>
        </div>
      </article>
    </div>
  </div>
</section>

<!-- Stores + Membership -->
<section id="stores">
  <div class="container">
    <div class="split">

      <div class="panel">
        <h3>Hệ thống cửa hàng</h3>
        <div style="margin-top:12px; display:grid; gap:10px">
          <div class="panel" style="padding:12px">
            <b>TP. HCM</b>
            <div style="color:var(--muted); font-size:13px">
              Quận 1 • Quận 3 • Quận 7…
            </div>
          </div>
          <div class="panel" style="padding:12px">
            <b>Hà Nội</b>
            <div style="color:var(--muted); font-size:13px">
              Cầu Giấy / Đống Đa…
            </div>
          </div>
          <div style="display:flex; gap:10px; flex-wrap:wrap">
            <button class="btn"
              @click="toast('Demo: trang store locator')">
              Xem tất cả cửa hàng
            </button>
          </div>
        </div>
      </div>

      <div class="panel" id="member">
        <h3>Membership & Newsletter</h3>

        <div class="form">
          <input v-model="email"
                 type="email"
                 placeholder="Email của anh..." />
          <button class="btn primary"
                  @click="subscribe">
            Đăng ký
          </button>
        </div>

        <div style="margin-top:12px; display:flex; gap:10px; flex-wrap:wrap">
          <span class="pill">Ưu đãi thành viên</span>
          <span class="pill">Thông báo drops</span>
          <span class="pill">Early access</span>
        </div>

        <div class="panel" style="margin-top:14px">
          <h3 style="margin:0 0 6px">Hỗ trợ nhanh</h3>
          <div style="margin-top:12px; display:flex; gap:10px; flex-wrap:wrap">
            <button class="btn"
              @click="toast('Demo: mở chat')">
              Chat ngay
            </button>
            <button class="btn ghost"
              @click="toast('Demo: hotline')">
              Gọi hotline : 0123.456.789
            </button>
          </div>
        </div>
      </div>

    </div>
  </div>
</section>

<CustomerFooter />

</div>

<!-- Toast -->

<div class="toast" :class="{ show: toastVisible }">
  {{ toastMessage }}
</div>

<div v-if="selectedProduct" class="quick-modal" @click.self="closeModal">
  <div class="quick-modal-content">
    <button class="quick-close" @click="closeModal">✕</button>

    <div class="quick-image-side">
      <img :src="selectedProduct.img" class="quick-image" />
      <button class="quick-nav quick-prev" type="button" aria-label="Ảnh trước">‹</button>
      <button class="quick-nav quick-next" type="button" aria-label="Ảnh sau">›</button>
    </div>

    <div class="quick-info-side">
      <h2>{{ selectedProduct.name }}</h2>
      <p class="quick-sku">Mã sản phẩm: {{ getQuickProductCode(selectedProduct) }}</p>

      <div class="quick-price">{{ VND(selectedProduct.price) }}</div>

      <div class="quick-row">
        <span>Màu sắc:</span>
        <b>Đỏ-{{ String(selectedProduct.id).padStart(3, '0') }}</b>
      </div>
      <div class="quick-swatches">
        <button
          v-for="(color, index) in getQuickPalette(selectedProduct.id)"
          :key="`${selectedProduct.id}-${color}`"
          class="quick-swatch"
          :class="{ active: quickColorIndex === index }"
          :style="{ background: color }"
          @click="quickColorIndex = index"
        ></button>
      </div>

      <div class="quick-row"><span>Kích thước:</span> <b>{{ quickSize }}</b></div>
      <div class="quick-sizes">
        <button
          v-for="size in QUICK_SIZES"
          :key="size"
          type="button"
          class="quick-size"
          :class="{ active: quickSize === size }"
          @click="quickSize = size"
        >
          {{ size }}
        </button>
      </div>

      <div class="quick-promo-box">
        <div class="quick-promo-title">ƯU ĐÃI ONLINE</div>
        <ul>
          <li>Nhập mã <b>PGG001</b> giảm 20K đơn từ 299K</li>
          <li>Nhập mã <b>PGG002</b> giảm 50K đơn từ 699K</li>
          <li>Nhập mã <b>PGG003</b> giảm 80K đơn từ 999K</li>
        </ul>
      </div>

      <div class="quick-action-row">
        <div class="quick-qty">
          <button type="button" @click="quickDecrease">−</button>
          <strong>{{ quickQty }}</strong>
          <button type="button" @click="quickIncrease">+</button>
        </div>
        <button class="btn" type="button" @click="quickAddToCart">THÊM VÀO GIỎ</button>
      </div>

      <button class="quick-detail-link" type="button" @click="openProductDetail(selectedProduct.id)">Xem chi tiết »</button>
    </div>
  </div>
</div>
</template>

<style scoped>
@import "./home.css";

/* Fix header overflow issues */

.home-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
header {
  overflow: visible !important;
}

header .container {
  overflow: visible !important;
}

.nav {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 20px;
  overflow: visible !important;
}

.dropdown-panel {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
}

.tile-content{
  display:flex;
  justify-content:space-between;
  align-items:center;
  width:100%;
}

.cat-img{
  width:70px;
  height:70px;
  object-fit:cover;
  border-radius:10px;
}

.category-photo{
  grid-column: span 1;
  border-radius:16px;
  overflow:hidden;
  height:120px;
}

.category-photo img{
  width:100%;
  height:100%;
  object-fit:cover;
}


.cart-foot{
  border-top:1px solid #eee;
  padding:18px;
  background:white;

  display:grid;
  grid-template-columns: 1fr auto;
  align-items:center;
  gap:20px;
}

.totals{
  display:flex;
  justify-content:space-between;
  align-items:center;
  font-size:15px;
}

.cart-actions{
  display:flex;
  gap:14px;
}

.checkout{
  min-width:120px;
  height:46px;
}

.continue{
  min-width:120px;
  height:46px;
}

.profile-wrapper {
  position: relative;
}

.user-account-btn {
  border: 1px solid var(--line);
  background: #fff;
  border-radius: 12px;
  min-height: 42px;
  padding: 6px 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.profile-avatar-chip {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
  background: #f1f5f9;
  overflow: hidden;
}

.profile-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-identity {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.15;
}

.profile-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text);
}

.profile-role {
  font-size: 11px;
  color: var(--muted);
}

.profile-wrapper::after {
  content: '';
  position: absolute;
  top: 100%;
  right: 0;
  width: 220px;
  height: 14px;
}

.profile-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 10px 30px rgba(0,0,0,.15);
  padding: 8px;
  display: flex;
  flex-direction: column;
  min-width: 180px;
  z-index: 100;
  border: 1px solid var(--line);
  animation: fadeInMenu 0.16s ease;
}

@keyframes fadeInMenu {
  from {
    opacity: 0;
    transform: translateY(-4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dropdown-item {
  padding: 12px 14px;
  border: none;
  background: none;
  text-align: left;
  font-size: 14px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text);
  font-weight: 500;
  transition: all 0.2s;
}

.dropdown-item:hover {
  background: #f3f4f6;
}

.dropdown-item.danger {
  color: #dc2626;
}

.dropdown-item.danger:hover {
  background: #fee2e2;
}
.img-row{
  display:flex;
  gap:12px;
  align-items:center;
  margin-top:10px;
}

.img-row img{
  height:28px;
  object-fit:contain;
}
.cart-img{
  width:60px;
  height:60px;
  object-fit:cover;
  border-radius:10px;
  background:#f5f5f5;
}
.dropdown{
  position:relative;
}

.dropdown-panel{
  position:absolute;
  top:100%;
  left:0;
  display:none;
  z-index:50;
}

.menu > *{
  white-space:nowrap;
}

.menu a{
  text-decoration:none;
  color:inherit;
  font-weight:500;
}
.menu > div:hover .dropdown-panel{
  display:block;
}
.cart-btn{
  position:relative;
}
.brand{
  flex:0 0 auto;
}

.menu{
  display:flex;
  align-items:center;
  gap:24px;
}
.actions{
  display:flex;
  align-items:center;
  gap:10px;
}
.cart-count{
  position:absolute;
  top:-6px;
  right:-6px;
  background:#ff3b30;
  color:white;
  font-size:11px;
  border-radius:50%;
  padding:2px 6px;
}
.actions{
  display:flex;
  align-items:center;
  gap:10px;
}

.interactive-pill {
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease;
}

.interactive-pill:hover {
  transform: translateY(-1px);
}

.interactive-pill.active {
  background: #111827 !important;
  color: #fff !important;
  border-color: #111827 !important;
}

.style-lab {
  padding: 22px 0;
}

.style-lab-wrap {
  border-radius: 24px;
  border: 1px solid #e7e7e7;
  background: linear-gradient(135deg, #fff8f1 0%, #f2f8ff 60%, #fff 100%);
  box-shadow: 0 18px 40px rgba(17, 24, 39, 0.08);
  padding: 22px;
  display: grid;
  gap: 14px;
}

.style-lab-wrap h3 {
  margin: 8px 0 6px;
  font-size: 30px;
  line-height: 1.1;
}

.style-lab-wrap p {
  margin: 0;
  color: #475569;
}

.style-moods,
.style-lab-actions,
.tier-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.section-pulse {
  animation: sectionPulse 0.65s ease;
}

@keyframes sectionPulse {
  0% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-6px);
  }
  100% {
    transform: translateY(0);
  }
}

.reveal {
  opacity: 0;
  transform: translateY(26px);
  animation: revealIn 0.7s ease forwards;
}

@keyframes revealIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 640px) {
  .style-lab-wrap h3 {
    font-size: 24px;
  }
}
</style>