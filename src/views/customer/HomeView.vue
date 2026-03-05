<script setup>
import { ref, computed, onMounted } from "vue"
import { useRouter } from "vue-router"
import logo from "../../assets/img/DirtyWaveLogo.png"
import img1 from "../../assets/img/Áo bomber da lộn DirtyWave.jpg?url"
import img2 from "../../assets/img/Áo bomber dáng lửng.jpg?url"
import img3 from "../../assets/img/Áo bomber giả da DirtyWave.jpg?url"
import img4 from "../../assets/img/Áo bomber nhẹ vải cotton DirtyWave.jpg?url"
import img5 from "../../assets/img/Áo hoodie kéo khoá dáng hộp DirtyWave.jpg?url"
import img6 from "../../assets/img/Áo hoodie kéo khoá in hình DirtyWave.jpg?url"
import img7 from "../../assets/img/Áo hoodie kéo khoá Jacket DirtyWave.jpg?url"
import img8 from "../../assets/img/Áo khoác coach cách nhiệt vải Timberland.jpg?url"
import img9 from "../../assets/img/Áo khoac coach da ASOS DirtyWave.jpg?url"
import img10 from "../../assets/img/Áo khoác coach giả da DirtyWave.jpg?url"
import img11 from "../../assets/img/Áo khoác coach lông cừu DirtyWave.jpg?url"
const router = useRouter()
const profileOpen = ref(false)

const logout = () => {
  profileOpen.value = false
  router.push("/login")
}
const VND = n => new Intl.NumberFormat("vi-VN").format(n) + "₫"

const searchQuery = ref("")
const mobileOpen = ref(false)
const activeFilter = ref(null)
const toastMessage = ref("")
const toastVisible = ref(false)
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

})
const toggleMobileMenu = () =>
  mobileOpen.value = !mobileOpen.value

const scrollToId = id => {
  const el = document.getElementById(id)
  if(el) el.scrollIntoView({behavior:"smooth"})
}

const filterBy = cat => {
  activeFilter.value = cat
  scrollToId("best")
}

const clearFilter = () =>
  activeFilter.value = null

const filteredBest = computed(() => {
  if(!activeFilter.value) return products.value
  return products.value.filter(p => p.cat === activeFilter.value)
})

const toast = msg => {
  toastMessage.value = msg
  toastVisible.value = true
  setTimeout(() => {
    toastVisible.value = false
  }, 3000)
}

// CART STATE
const cart = ref({})
const cartOpen = ref(false)

const openCart = () => cartOpen.value = true
const closeCart = () => cartOpen.value = false

const addToCart = (id) => {
  if (!cart.value[id]) cart.value[id] = 0
  cart.value[id]++

  cart.value = { ...cart.value }   // force Vue update
  toast("Đã thêm vào giỏ")
  openCart()
}

const dec = (id) => {
  if (!cart.value[id]) return

  cart.value[id]--

  if (cart.value[id] <= 0) delete cart.value[id]

  cart.value = { ...cart.value }
}

const inc = (id) => {
  if (!cart.value[id]) cart.value[id] = 0

  cart.value[id]++

  cart.value = { ...cart.value }
}

const subtotal = computed(() => {
  let sum = 0

  Object.entries(cart.value).forEach(([id, qty]) => {
    const p = products.value.find(x => x.id == id)
    if (p) sum += p.price * qty
  })

  return sum
})
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

const openModal = (product) => {
  selectedProduct.value = product
}
const cartCount = computed(() => {
  return Object.values(cart.value).reduce((a,b)=>a+b,0)
})

const cartEntries = computed(() => {
  return Object.entries(cart.value)
})

const getProduct = (id) => {
  return products.value.find(p => p.id === Number(id))
}

const closeModal = () => {
  selectedProduct.value = null
}


</script>

<template>
<div>

<!-- Topbar -->
<div class="topbar">
  <div class="row-full">
    <div class="marquee">
      <div class="marquee-track">
        Freeship theo mốc đơn • Đổi size dễ dàng • Hỗ trợ nhanh qua chat
      </div>
    </div>
  </div>
</div>

<!-- Header -->
<header>
<div class="container">
<div class="nav">

<a class="brand" href="#">
  <div class="logo">
    <img :src="logo">
  </div>
  <div>
    <strong>DirtyWave</strong><br/>
    <small style="color:var(--muted); font-size:12px">
      Shop Jacket
    </small>
  </div>
</a>

<div class="menu">

<div>
<a href="#">Áo nam ▾</a>
<div class="dropdown-panel" role="menu">
<div class="grid2">

<div class="panel-block">
<h4>Danh mục áo</h4>
<div class="panel-links">
<a @click.prevent="filterBy('Áo thun')">Áo thun</a>
<a @click.prevent="filterBy('Polo')">Polo</a>
<a @click.prevent="filterBy('Sơ mi')">Sơ mi</a>
<a @click.prevent="filterBy('Áo khoác')">Áo khoác</a>
<a @click.prevent="filterBy('Hoodie')">Hoodie</a>
<a @click.prevent="filterBy('Nỉ/Len')">Nỉ/Len</a>
</div>
</div>

<div class="panel-block panel-cta">
<div>
<h4 style="margin:0; color:rgba(238,242,255,.9)">
Bộ sưu tập mới
</h4>
<p>Mix đồ công sở & đi chơi, form dễ mặc.</p>
<div style="margin-top:10px">
<button class="btn primary" @click="scrollToId('new')">
Xem Hàng Mới Về
</button>
</div>
</div>
<span class="pill">New • 2026</span>
</div>

</div>
</div>
</div>

<div>
<a href="#">Quần nam ▾</a>
<div class="dropdown-panel" role="menu">
<div class="grid2">

<div class="panel-block">
<h4>Danh mục quần</h4>
<div class="panel-links">
<a @click.prevent="filterBy('Jeans')">Jeans</a>
<a @click.prevent="filterBy('Kaki/Chino')">Kaki/Chino</a>
<a @click.prevent="filterBy('Quần tây')">Quần tây</a>
<a @click.prevent="filterBy('Jogger')">Jogger</a>
<a @click.prevent="filterBy('Short')">Short</a>
<a @click.prevent="filterBy('Set đồ')">Set đồ</a>
</div>
</div>

<div class="panel-block panel-cta">
<div>
<h4 style="margin:0; color:rgba(238,242,255,.9)">
Jeans chủ lực
</h4>
<p>Co giãn, nhẹ, thoáng — hợp mặc cả ngày.</p>
<div style="margin-top:10px; display:flex; gap:8px; flex-wrap:wrap">
<span class="pill">AIRFLEX</span>
<span class="pill">Lightweight</span>
<span class="pill">ProCool</span>
</div>
</div>
<button class="btn" @click="scrollToId('best')">Bán chạy</button>
</div>

</div>
</div>
</div>

<a href="#outlet">Cửa Hàng</a>

</div>

<div class="actions">
<div class="search" role="search">
<span aria-hidden="true">🔎</span>
<input v-model="searchQuery"
placeholder="Tìm áo, quần, jeans, phụ kiện..." />
</div>

<button class="iconbtn hamburger"
aria-label="Mở menu"
@click="toggleMobileMenu()">☰</button>

<div 
  class="profile-wrapper"
  @mouseenter="profileOpen = true"
  @mouseleave="profileOpen = false"
>
  <button class="iconbtn" aria-label="Tài khoản">
    👤
  </button>

  <div v-show="profileOpen" class="profile-dropdown">
    <button class="dropdown-item" @click="toast('Demo: trang tài khoản')">
      Tài khoản
    </button>

    <button class="dropdown-item" @click="toast('Demo: đơn hàng')">
      Đơn hàng
    </button>

    <button class="dropdown-item danger" @click="logout">
      Đăng xuất
    </button>
  </div>
</div>

<button class="iconbtn cart-btn"
aria-label="Giỏ hàng"
@click="openCart">
🛒
<span v-if="cartCount" class="cart-count">
{{ cartCount }}
</span>
</button>
</div>

</div>

<!-- Mobile menu -->
<div v-show="mobileOpen"
style="padding: 0 0 14px;">
<div class="panel"
style="padding:12px; margin-bottom:10px">
<div class="form" style="margin:0">
<input placeholder="Tìm nhanh..." />
<button class="btn"
@click="toast('Demo: search')">Tìm</button>
</div>
</div>

<div class="panel" style="padding:12px">
<div style="display:grid; gap:6px">
<a class="btn ghost"
href="#category"
@click="toggleMobileMenu()">Áo nam</a>
<a class="btn ghost"
href="#category"
@click="toggleMobileMenu()">Quần nam</a>
<a class="btn ghost"
href="#outlet"
@click="toggleMobileMenu()">Cửa Hàng</a>
<a class="btn ghost"
href="#stores"
@click="toggleMobileMenu()">Cửa hàng</a>
<a class="btn ghost"
href="#member"
@click="toggleMobileMenu()">Membership</a>
</div>
</div>
</div>

</div>
</header>

<!-- Hero -->
<main class="hero">
<div class="container">
<div class="hero-wrap">

<div class="hero-card">
<div class="hero-visual"></div>
<div class="content">
<span class="pill">
Ưu đãi theo mốc đơn • Freeship
</span>

<h1>
Thời trang nam tối giản<br/>
tập trung vào form & chất liệu
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
<span class="pill">Đổi size dễ</span>
<span class="pill">Thanh toán COD</span>
<span class="pill">Hỗ trợ 7 ngày/tuần</span>
</div>

</div>
</div>

<div class="hero-side">

<div class="mini-card">
<span class="badge">NEW</span>
<div class="bg"></div>
<h3>Hàng Mới Về</h3>
<p>Áo polo, sơ mi, jeans nhẹ — cập nhật theo mùa.</p>
<div style="margin-top:12px">
<button class="btn"
@click="scrollToId('new')">
Xem ngay →
</button>
</div>
</div>

<div class="mini-card">
<span class="badge">CỬA HÀNG</span>
<div class="bg"></div>
<h3>Giá tốt</h3>
<p>Deal theo mốc giá — phù hợp xả kho / clearance.</p>
<div style="margin-top:12px; display:flex; gap:10px; flex-wrap:wrap">
<span class="pill">199K</span>
<span class="pill">299K</span>
<span class="pill">399K</span>
</div>
<div style="margin-top:12px">
<button class="btn"
@click="scrollToId('outlet')">
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
<a class="btn" href="#all">
Xem tất cả →
</a>
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
<section id="best">
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
v-for="p in filteredBest"
:key="p.id"
class="card">

<div class="thumb">
  <img :src="p.img" alt="" />
  <div class="overlay">
    <button class="overlay-btn" @click="$router.push('/product/' + p.id)">
      Xem chi tiết
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

<!-- Promo band -->
<section>
<div class="container">
<div class="promo">

<div>
<div class="pill">Ưu đãi hôm nay</div>

<p style="margin:8px 0 0">
Nhập mã <b>DIRTY10</b> giảm <b>10%</b>
(tối đa <b>50K</b>) •
<b>Freeship</b> đơn từ <b>399K</b>
</p>

<small style="display:block;margin-top:6px;color:var(--muted)">
Áp dụng đến 23:59 hôm nay •
Không áp dụng sản phẩm giảm sâu / Cửa Hàng
</small>
</div>

<div style="display:flex; gap:10px; flex-wrap:wrap">
<button class="btn primary"
@click="copyVoucher('DIRTY10')">
Sao chép mã: DIRTY10
</button>

<button class="btn"
@click="scrollToId('best')">
Mua ngay
</button>
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
      <a class="btn" href="#all">Xem thêm →</a>
    </div>

    <div class="cards">
      <article
        v-for="p in products.filter(x => [7,8,9,10].includes(x.id))"
        :key="p.id"
        class="card"
      >
        <div class="thumb">
          <img :src="p.img" alt="" />
          <div class="overlay">
            <button class="overlay-btn" @click="$router.push('/product/' + p.id)">
              Xem chi tiết
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
<section id="outlet">
  <div class="container">
    <div class="section-head">
      <div>
        <h2>Cửa Hàng</h2>
      </div>
      <div style="display:flex; gap:10px; flex-wrap:wrap">
        <span class="pill">199K</span>
        <span class="pill">299K</span>
        <span class="pill">399K</span>
      </div>
    </div>

    <div class="cards">
      <article
        v-for="p in products.filter(x => [11,12,13,14].includes(x.id))"
        :key="p.id"
        class="card"
      >
        <div class="thumb">
          <img :src="p.img" alt="" />
          <div class="overlay">
            <button class="overlay-btn" @click="$router.push('/product/' + p.id)">
              Xem chi tiết
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

<!-- Footer -->
<footer id="policy">
  <div class="container">
    <div class="foot">

      <div>
      <div style="display:flex; align-items:center; gap:10px">
        <img
          :src="logo"
          style="width:34px;height:34px;border-radius:12px"
        />
        <div>
          <strong>DirtyWave</strong><br/>
        </div>
      </div>

        <p style="margin:12px 0 0; font-size:11px; line-height:1.6;">
DirtyWave là shop áo khoác dành cho những ai thích phong cách tối giản nhưng vẫn có điểm nhấn. Bọn mình tập trung vào form dễ mặc, chất liệu bền và đường may chắc chắn để anh em có thể phối đi làm, đi chơi hay đi phượt đều ổn. Bộ sưu tập trải từ bomber, hoodie, varsity đến áo khoác gió, ưu tiên các tông màu dễ phối và thiết kế gọn gàng, không rườm rà.
        </p>
      </div>

      <div>
        <h4>Chính Sách</h4>
        <a href="#" @click.prevent="toast('Demo: giao hàng')">Chính sách Giao hàng</a>
        <a href="#" @click.prevent="toast('Demo: đổi trả')">Chính sách Đổi trả</a>
        <a href="#" @click.prevent="toast('Demo: kiểm hàng')">Chính sách Kiểm hàng</a>
        <a href="#" @click.prevent="toast('Demo: thanh toán')">Chính sách Thanh toán</a>
      </div>

      <div>
        <h4>Kết nối với chúng tôi</h4>
        <a href="#" @click.prevent="toast('Facebook')">Facebook</a>
        <a href="#" @click.prevent="toast('Instagram')">Instagram</a>
        <a href="#" @click.prevent="toast('TikTok')">TikTok</a>
        <a href="#" @click.prevent="toast('Zalo')">Zalo</a>
      </div>

      <div>
        <h4>Phương Thức Thanh Toán</h4>
<div class="img-row">
<img src="https://upload.wikimedia.org/wikipedia/commons/f/fe/MoMo_Logo.png" height="28"/>
<img src="https://upload.wikimedia.org/wikipedia/commons/2/2a/Visa_Logo.png" height="28"/>
<img src="https://upload.wikimedia.org/wikipedia/commons/0/04/Mastercard-logo.png" height="28"/>
<img src="https://upload.wikimedia.org/wikipedia/commons/6/6b/VNPAY-QR.png" height="28"/>
</div>
      </div>

    </div>

    <div class="copyright">
      © {{ year }} DirtyWave. Built as an original template (no 1:1 copying).
    </div>

  </div>
</footer>

</div>

<!-- CART DRAWER -->
<div
v-if="cartOpen"
:class="['drawer-backdrop', { open: cartOpen }]"
@click="closeCart">
</div>

<aside class="drawer" :class="{open:cartOpen}">
<div class="head">
<b>Giỏ hàng</b>
<button class="iconbtn" @click="closeCart">✕</button>
</div>

<div class="items">

<template v-if="cartEntries.length">
<div
v-for="[id, qty] in cartEntries"
:key="id"
class="lineitem"
>

<img
:src="getProduct(id)?.img"
class="cart-img"
/>

<div class="info">
<b>{{ getProduct(id)?.name }}</b>
<span>
{{ getProduct(id)?.cat }} •
{{ VND(getProduct(id)?.price) }}
</span>
</div>

<div style="display:grid; gap:6px; justify-items:end">

<div style="display:flex; gap:6px; align-items:center">
<button class="iconbtn" @click="dec(id)">−</button>

<b style="width:22px; text-align:center">
{{ qty }}
</b>

<button class="iconbtn" @click="inc(id)">+</button>
</div>

<span style="color:var(--muted); font-size:12px">
{{ VND((getProduct(id)?.price || 0) * qty) }}
</span>

</div>

</div>

</template>

<div v-else class="panel" style="padding:20px; text-align:center">
<b>Giỏ đang trống</b>
<p style="margin-top:6px; color:var(--muted)">Thêm vài món để checkout nhé.</p>
</div>

</div>

<div class="foot cart-foot">

<div class="totals">
<span>Tạm tính</span>
<b>{{ VND(subtotal) }}</b>
</div>

<div class="cart-actions">
<button class="btn primary checkout">
Thanh toán
</button>

<button class="btn continue" @click="closeCart">
Tiếp tục mua
</button>
</div>

</div>

</aside>

<!-- Toast -->

<div class="toast" :class="{ show: toastVisible }">
  {{ toastMessage }}
</div>

<div v-if="selectedProduct" class="modal">
  <div class="modal-content">
    <button class="close" @click="closeModal">✕</button>

    <img :src="selectedProduct.img" class="modal-img" />

    <h2>{{ selectedProduct.name }}</h2>
    <p>{{ selectedProduct.cat }}</p>

    <div class="price">
      {{ VND(selectedProduct.price) }}
    </div>

    <button class="btn primary">Thêm giỏ</button>
  </div>
</div>
</template>

<style scoped>
@import "./home.css";


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
.dropdown-panel{
  position:absolute;
  top:100%;
  left:50%;
  transform:translateX(40%);
}
.profile-dropdown {
  position: absolute;
  top: 42px;
  right: 0;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 10px 30px rgba(0,0,0,.15);
  padding: 8px;
  display: flex;
  flex-direction: column;
  min-width: 160px;
  z-index: 50;
}

.dropdown-item {
  padding: 10px 12px;
  border: none;
  background: none;
  text-align: left;
  font-size: 14px;
  border-radius: 8px;
  cursor: pointer;
}
.nav{
  display:grid;
  grid-template-columns: auto 1fr auto;
  align-items:center;
  gap:20px;
}
.dropdown-item:hover {
  background: #f3f4f6;
}

.dropdown-item.danger {
  color: #e53935;
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
</style>