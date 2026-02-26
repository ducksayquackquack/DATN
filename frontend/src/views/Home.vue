<template>
  <div class="home">
    <!-- TOPBAR -->
    <div class="topbar">
      <div class="container">
        <div class="row">
          <small>Freeship theo mốc đơn • Đổi size dễ dàng • Hỗ trợ nhanh</small>
          <div class="links">
            <a href="#" @click.prevent>Tra cứu đơn</a>
            <a href="#" @click.prevent>Hỗ trợ</a>
          </div>
        </div>
      </div>
    </div>

    <!-- HEADER / NAV -->
    <header>
      <div class="container">
        <div class="nav">
          <a class="brand" href="#" @click.prevent>
            <span class="logo" aria-hidden="true"></span>
            <strong>DENIM STORE</strong>
          </a>

          <nav class="menu" aria-label="Main navigation">
            <a href="#" @click.prevent>Trang chủ</a>

            <div class="dropdown" :class="{ open: dropdownOpen }">
              <a
                href="#"
                @click.prevent="toggleDropdown"
                aria-haspopup="true"
                :aria-expanded="dropdownOpen ? 'true' : 'false'"
              >
                Danh mục
              </a>

              <div class="dropdown-panel" role="menu">
                <div class="grid2">
                  <div class="panel-block">
                    <h4>Danh mục nổi bật</h4>

                    <div v-if="loading" class="pill">Đang tải danh mục…</div>
                    <div v-else-if="error" class="pill">Không tải được danh mục</div>

                    <div v-else class="panel-links">
                      <a
                        v-for="cat in categories"
                        :key="cat.id ?? cat.maDanhMuc ?? cat.tenDanhMuc"
                        href="#"
                        @click.prevent="selectCategory(cat)"
                      >
                        {{ cat.tenDanhMuc ?? cat.name ?? "Danh mục" }}
                      </a>
                    </div>
                  </div>

                  <div class="panel-block panel-cta">
                    <div>
                      <h4>Gợi ý</h4>
                      <strong>New arrivals</strong>
                      <p>Sản phẩm mới cập nhật từ hệ thống.</p>
                    </div>

                    <button class="btn primary" type="button" @click="scrollToProducts">
                      Xem sản phẩm
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <a href="#" @click.prevent>Khuyến mãi</a>
            <a href="#" @click.prevent>Liên hệ</a>
          </nav>

          <div class="actions">
            <div class="search" role="search">
              <span aria-hidden="true">⌕</span>
              <input
                v-model.trim="query"
                type="search"
                placeholder="Tìm sản phẩm…"
                aria-label="Search products"
              />
            </div>

            <button class="iconbtn" type="button" @click="toggleCart" aria-label="Open cart">
              🛒
            </button>

            <button class="iconbtn hamburger" type="button" @click="toggleDropdown" aria-label="Open menu">
              ☰
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- HERO -->
    <main class="hero">
      <div class="container">
        <div class="hero-wrap">
          <div class="hero-card">
            <div class="content">
              <div class="pill">Template Vue • API ready</div>
              <h1>DENIM STORE</h1>
              <p>Thời trang nam tối giản • Tối ưu trải nghiệm • Dễ mở rộng</p>

              <div class="row">
                <button class="btn primary" type="button" @click="scrollToProducts">Mua ngay</button>
                <button class="btn ghost" type="button" @click="loadHome">Tải lại dữ liệu</button>
              </div>

              <div v-if="error" style="margin-top: 12px;">
                <span class="pill">Lỗi: {{ error }}</span>
              </div>
            </div>

            <div class="hero-visual" aria-hidden="true"></div>
          </div>

          <div class="hero-side">
            <div class="mini-card">
              <div class="badge">HOT</div>
              <h3>Giá tốt mỗi ngày</h3>
              <p>Hiển thị giá theo API, tự format theo vi-VN.</p>
              <div class="bg" aria-hidden="true"></div>
            </div>

            <div class="mini-card">
              <div class="badge">FAST</div>
              <h3>Cart drawer</h3>
              <p>Thêm vào giỏ • Tính tổng • Toast thông báo.</p>
              <div class="bg" aria-hidden="true"></div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- CATEGORIES -->
    <section v-if="categories.length">
      <div class="container">
        <div class="section-head">
          <div>
            <h2>Danh mục</h2>
            <p>Chọn danh mục để lọc sản phẩm.</p>
          </div>

          <button v-if="selectedCategory" class="btn" type="button" @click="clearCategory">
            Bỏ lọc
          </button>
        </div>

        <div class="tiles">
          <a
            v-for="cat in categories"
            :key="cat.id ?? cat.maDanhMuc ?? cat.tenDanhMuc"
            class="tile"
            href="#"
            @click.prevent="selectCategory(cat)"
          >
            <div>
              <h3>
                {{ cat.tenDanhMuc ?? cat.name ?? "Danh mục" }}
                <span v-if="isSelectedCategory(cat)">• Đang chọn</span>
              </h3>
              <span>Nhấn để lọc</span>
            </div>
            <span class="dot" aria-hidden="true"></span>
          </a>
        </div>
      </div>
    </section>

    <!-- PRODUCTS -->
    <section ref="productsSection">
      <div class="container">
        <div class="section-head">
          <div>
            <h2>Sản phẩm</h2>
            <p v-if="selectedCategory">
              Đang lọc: <b>{{ selectedCategoryLabel }}</b>
            </p>
            <p v-else>
              Tất cả sản phẩm từ API.
            </p>
          </div>

          <div class="pill">
            <span v-if="loading">Đang tải…</span>
            <span v-else>{{ filteredProducts.length }} sản phẩm</span>
          </div>
        </div>

        <div v-if="loading" class="promo">
          <div>
            <b>Đang tải dữ liệu…</b>
            <p>Vui lòng chờ trong giây lát.</p>
          </div>
        </div>

        <div v-else-if="!filteredProducts.length" class="promo">
          <div>
            <b>Không có sản phẩm phù hợp</b>
            <p>Thử từ khóa khác hoặc bỏ lọc danh mục.</p>
          </div>
          <button class="btn" type="button" @click="resetFilters">Reset</button>
        </div>

        <div v-else class="cards">
          <article
            v-for="p in filteredProducts"
            :key="p.id"
            class="card"
          >
            <div
              class="thumb"
              :style="thumbStyle(p)"
              role="img"
              :aria-label="p.tenSanPham"
            ></div>

            <div class="body">
              <div class="title">{{ p.tenSanPham }}</div>

              <div class="meta">
                <div class="price">
                  <span>{{ formatPrice(finalPrice(p)) }}</span>
                  <span v-if="hasDiscount(p)" class="strike">
                    {{ formatPrice(p.giaBan) }}
                  </span>
                </div>

                <span v-if="hasDiscount(p)" class="chip">
                  -{{ p.giaTriKhuyenMai }}%
                </span>
                <span v-else class="chip">
                  New
                </span>
              </div>
            </div>

            <div class="footer">
              <span class="chip">{{ p.danhMucTen ?? "Denim" }}</span>
              <button class="btn primary" type="button" @click="addToCart(p)">
                Thêm giỏ
              </button>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- PROMO BAND -->
    <section>
      <div class="container">
        <div class="promo">
          <div>
            <b>Ưu đãi thành viên</b>
            <p>Đăng ký email để nhận thông tin giảm giá.</p>
          </div>
          <button class="btn" type="button" @click="scrollToNewsletter">Đăng ký</button>
        </div>
      </div>
    </section>

    <!-- STORE + NEWSLETTER -->
    <section>
      <div class="container">
        <div class="split">
          <div class="panel">
            <h3>Cửa hàng</h3>
            <p>Hệ thống demo: dữ liệu được tải từ API <code>/api/public/home</code>.</p>
          </div>

          <div ref="newsletterSection" class="panel">
            <h3>Newsletter</h3>
            <p>Nhập email để nhận cập nhật.</p>

            <form class="form" @submit.prevent="subscribe">
              <input v-model.trim="email" type="email" placeholder="you@example.com" required />
              <button class="btn primary" type="submit">Gửi</button>
            </form>
          </div>
        </div>
      </div>
    </section>

    <!-- FOOTER -->
    <footer>
      <div class="container">
        <div class="foot">
          <div>
            <h4>DENIM STORE</h4>
            <div style="color: var(--muted);">
              Template Vue • Clean UI • API ready
            </div>
          </div>

          <div>
            <h4>Hỗ trợ</h4>
            <a href="#" @click.prevent>Đổi trả</a>
            <a href="#" @click.prevent>Vận chuyển</a>
            <a href="#" @click.prevent>Liên hệ</a>
          </div>

          <div>
            <h4>Chính sách</h4>
            <a href="#" @click.prevent>Bảo mật</a>
            <a href="#" @click.prevent>Điều khoản</a>
          </div>

          <div>
            <h4>Tài khoản</h4>
            <a href="#" @click.prevent>Đăng nhập</a>
            <a href="#" @click.prevent>Đăng ký</a>
          </div>
        </div>

        <div class="copyright">
          © {{ new Date().getFullYear() }} Denim Store
        </div>
      </div>
    </footer>

    <!-- CART DRAWER -->
    <div
      class="drawer-backdrop"
      :class="{ open: cartOpen }"
      @click.self="cartOpen = false"
    ></div>

    <aside class="drawer" :class="{ open: cartOpen }" aria-label="Cart drawer">
      <div class="head">
        <strong>Giỏ hàng</strong>
        <button class="btn" type="button" @click="cartOpen = false">Đóng</button>
      </div>

      <div class="items">
        <div v-if="!cartItems.length" class="pill">Chưa có sản phẩm nào.</div>

        <div v-for="it in cartItems" :key="it.product.id" class="lineitem">
          <div class="ph" aria-hidden="true"></div>

          <div class="info">
            <b>{{ it.product.tenSanPham }}</b>
            <span>
              {{ it.qty }} × {{ formatPrice(finalPrice(it.product)) }}
            </span>
          </div>

          <div style="display:grid; gap:8px;">
            <button class="btn" type="button" @click="decreaseQty(it.product.id)">-</button>
            <button class="btn" type="button" @click="increaseQty(it.product.id)">+</button>
          </div>
        </div>
      </div>

      <div class="foot">
        <div class="totals">
          <span>Tạm tính</span>
          <strong>{{ formatPrice(cartTotal) }}</strong>
        </div>

        <button
          class="btn primary"
          type="button"
          :disabled="!cartItems.length"
          @click="checkout"
        >
          Thanh toán
        </button>
      </div>
    </aside>

    <!-- TOAST -->
    <div class="toast" :class="{ show: toastOpen }" role="status" aria-live="polite">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "Home",

  data() {
    return {
      // api data
      products: [],
      categories: [],

      // ui state used in template
      loading: false,
      error: "",
      query: "",
      dropdownOpen: false,
      selectedCategory: null,

      // cart
      cartOpen: false,
      cartItems: [], // [{ product, qty }]

      // toast
      toastOpen: false,
      toastMessage: "",

      // newsletter
      email: ""
    };
  },

  computed: {
    selectedCategoryLabel() {
      if (!this.selectedCategory) return "";
      return this.selectedCategory.tenDanhMuc || this.selectedCategory.name || "Danh mục";
    },

    filteredProducts() {
      let list = Array.isArray(this.products) ? this.products : [];

      // filter by category (best-effort depending on your API shape)
      if (this.selectedCategory) {
        const catId = this.selectedCategory.id ?? this.selectedCategory.maDanhMuc;
        const catName = this.selectedCategory.tenDanhMuc ?? this.selectedCategory.name;

        list = list.filter((p) => {
          // try common mappings
          if (catId != null) {
            return (
              p.categoryId === catId ||
              p.maDanhMuc === catId ||
              p.danhMucId === catId
            );
          }
          if (catName) {
            return (
              p.danhMucTen === catName ||
              p.categoryName === catName
            );
          }
          return true;
        });
      }

      // search query
      const q = (this.query || "").toLowerCase();
      if (q) {
        list = list.filter((p) => (p.tenSanPham || "").toLowerCase().includes(q));
      }

      return list;
    },

    cartTotal() {
      return this.cartItems.reduce((sum, it) => {
        return sum + this.finalPrice(it.product) * it.qty;
      }, 0);
    }
  },

  methods: {
    async loadHome() {
      this.loading = true;
      this.error = "";

      try {
        const response = await axios.get("http://localhost:8080/api/public/home");
        this.products = response.data?.products ?? [];
        this.categories = response.data?.categories ?? [];
      } catch (err) {
        console.error("ERROR LOADING HOME:", err);
        this.error =
          err?.response?.data?.message ||
          err?.message ||
          "Không thể tải dữ liệu.";
      } finally {
        this.loading = false;
      }
    },

    // --- dropdown / menu ---
    toggleDropdown() {
      this.dropdownOpen = !this.dropdownOpen;
    },

    // --- category filtering ---
    selectCategory(cat) {
      this.selectedCategory = cat;
      this.dropdownOpen = false;
      this.scrollToProducts();
    },

    clearCategory() {
      this.selectedCategory = null;
    },

    isSelectedCategory(cat) {
      if (!this.selectedCategory || !cat) return false;
      const a = this.selectedCategory.id ?? this.selectedCategory.maDanhMuc ?? this.selectedCategory.tenDanhMuc;
      const b = cat.id ?? cat.maDanhMuc ?? cat.tenDanhMuc;
      return a === b;
    },

    resetFilters() {
      this.query = "";
      this.selectedCategory = null;
    },

    // --- pricing ---
    formatPrice(price) {
      const val = Number(price || 0);
      return new Intl.NumberFormat("vi-VN").format(val) + " đ";
    },

    hasDiscount(product) {
      const v = Number(product?.giaTriKhuyenMai || 0);
      return v > 0;
    },

    finalPrice(product) {
      const giaBan = Number(product?.giaBan || 0);
      const km = Number(product?.giaTriKhuyenMai || 0);
      if (!km) return giaBan;
      return Math.round(giaBan * (1 - km / 100));
    },

    // --- product image thumb style (optional) ---
    thumbStyle(product) {
      // If API has image url, use it. Otherwise keep your gradient background.
      const url = product?.anhDaiDien || product?.image || "";
      if (!url) return {};
      return {
        backgroundImage: `url("${url}")`,
        backgroundSize: "cover",
        backgroundPosition: "center"
      };
    },

    // --- cart ---
    toggleCart() {
      this.cartOpen = !this.cartOpen;
    },

    addToCart(product) {
      if (!product?.id) return;

      const found = this.cartItems.find((x) => x.product.id === product.id);
      if (found) found.qty += 1;
      else this.cartItems.push({ product, qty: 1 });

      this.cartOpen = true;
      this.showToast("Đã thêm vào giỏ hàng");
    },

    increaseQty(productId) {
      const it = this.cartItems.find((x) => x.product.id === productId);
      if (it) it.qty += 1;
    },

    decreaseQty(productId) {
      const idx = this.cartItems.findIndex((x) => x.product.id === productId);
      if (idx === -1) return;

      this.cartItems[idx].qty -= 1;
      if (this.cartItems[idx].qty <= 0) this.cartItems.splice(idx, 1);
    },

    checkout() {
      this.showToast("Checkout demo (chưa tích hợp)");
    },

    // --- toast ---
    showToast(msg) {
      this.toastMessage = msg;
      this.toastOpen = true;
      clearTimeout(this._toastTimer);
      this._toastTimer = setTimeout(() => (this.toastOpen = false), 1800);
    },

    // --- scroll helpers ---
    scrollToProducts() {
      const el = this.$refs.productsSection;
      if (el?.scrollIntoView) el.scrollIntoView({ behavior: "smooth", block: "start" });
    },

    scrollToNewsletter() {
      const el = this.$refs.newsletterSection;
      if (el?.scrollIntoView) el.scrollIntoView({ behavior: "smooth", block: "start" });
    },

    // --- newsletter ---
    subscribe() {
      this.showToast(`Đã gửi email: ${this.email}`);
      this.email = "";
    }
  },

  mounted() {
    this.loadHome();
  },

  beforeUnmount() {
    clearTimeout(this._toastTimer);
  }
};
</script>
  <style>
    :root{
      --bg:#0b0c10;
      --card:#12141a;
      --muted:#9aa3b2;
      --text:#eef2ff;
      --line:#232735;
      --accent:#ffcc00;
      --accent2:#5eead4;
      --shadow: 0 10px 30px rgba(0,0,0,.35);
      --radius: 18px;
      --max: 1180px;
    }
    *{box-sizing:border-box}
    html,body{height:100%}
    body{
      margin:0;
      font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Arial, "Apple Color Emoji","Segoe UI Emoji";
      background: radial-gradient(900px 700px at 20% -10%, rgba(94,234,212,.12), transparent 55%),
                  radial-gradient(900px 700px at 90% 10%, rgba(255,204,0,.10), transparent 50%),
                  var(--bg);
      color: var(--text);
      line-height:1.5;
    }
    a{color:inherit; text-decoration:none}
    img{max-width:100%; display:block}
    .container{max-width:var(--max); margin:0 auto; padding:0 18px}
    .btn{
      display:inline-flex; align-items:center; justify-content:center; gap:10px;
      padding:12px 16px; border-radius:999px; border:1px solid var(--line);
      background: rgba(255,255,255,.04);
      color: var(--text);
      cursor:pointer;
      transition: .2s ease;
      user-select:none;
    }
    .btn:hover{transform: translateY(-1px); border-color: rgba(255,204,0,.35)}
    .btn.primary{background: linear-gradient(135deg, rgba(255,204,0,.95), rgba(94,234,212,.85)); color:#101114; border:0}
    .btn.ghost{background:transparent}
    .pill{
      display:inline-flex; gap:8px; align-items:center;
      padding:6px 10px; border:1px solid var(--line);
      border-radius:999px; color:var(--muted); font-size:12px;
      background: rgba(255,255,255,.03);
    }

    /* Topbar */
    .topbar{
      border-bottom:1px solid rgba(255,255,255,.06);
      background: rgba(0,0,0,.18);
      backdrop-filter: blur(10px);
    }
    .topbar .row{
      display:flex; align-items:center; justify-content:space-between;
      padding:10px 0;
      gap:12px;
    }
    .topbar small{color:var(--muted)}
    .topbar .links{display:flex; gap:12px; align-items:center}
    .topbar .links a{color:var(--muted); font-size:13px}
    .topbar .links a:hover{color:var(--text)}

    /* Header */
    header{
      position:sticky; top:0; z-index:50;
      background: rgba(11,12,16,.65);
      backdrop-filter: blur(12px);
      border-bottom:1px solid rgba(255,255,255,.06);
    }
    .nav{
      display:flex; align-items:center; justify-content:space-between;
      padding:14px 0;
      gap:12px;
    }
    .brand{display:flex; align-items:center; gap:10px}
    .logo{
      width:40px; height:40px; border-radius:12px;
      background: linear-gradient(135deg, rgba(255,204,0,.95), rgba(94,234,212,.85));
      box-shadow: var(--shadow);
    }
    .brand strong{letter-spacing:.5px}
    .menu{display:flex; align-items:center; gap:18px}
    .menu > a{
      color: rgba(238,242,255,.9);
      font-weight:600;
      font-size:14px;
      padding:10px 8px;
      border-radius:10px;
    }
    .menu > a:hover{background: rgba(255,255,255,.04)}
    .actions{display:flex; align-items:center; gap:10px}
    .iconbtn{
      width:40px; height:40px; border-radius:12px;
      border:1px solid var(--line);
      background: rgba(255,255,255,.03);
      display:grid; place-items:center;
      cursor:pointer;
      transition:.2s ease;
    }
    .iconbtn:hover{transform: translateY(-1px); border-color: rgba(255,204,0,.35)}
    .search{
      display:flex; align-items:center; gap:10px;
      padding:10px 12px;
      border:1px solid var(--line);
      background: rgba(255,255,255,.03);
      border-radius:999px;
      min-width: 280px;
    }
    .search input{
      width:100%; border:0; outline:0; background:transparent;
      color:var(--text);
      font-size:14px;
    }
    .hamburger{display:none}

    /* Mega menu */
    .dropdown{position:relative}
    .dropdown-panel{
      position:absolute; left:0; top:calc(100% + 10px);
      width:min(720px, 90vw);
      border-radius: var(--radius);
      border:1px solid rgba(255,255,255,.08);
      background: rgba(18,20,26,.95);
      backdrop-filter: blur(12px);
      box-shadow: var(--shadow);
      padding:16px;
      display:none;
    }
    .dropdown.open .dropdown-panel{display:block}
    .grid2{display:grid; grid-template-columns: 1.2fr 1fr; gap:14px}
    .panel-block{
      border:1px solid rgba(255,255,255,.06);
      background: rgba(255,255,255,.02);
      border-radius: 14px;
      padding:14px;
    }
    .panel-block h4{margin:0 0 8px 0; font-size:13px; color:var(--muted); letter-spacing:.3px; text-transform:uppercase}
    .panel-links{display:grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap:10px}
    .panel-links a{
      padding:10px 10px; border-radius:12px;
      border:1px solid rgba(255,255,255,.06);
      background: rgba(255,255,255,.02);
      font-weight:600; font-size:14px;
    }
    .panel-links a:hover{border-color: rgba(255,204,0,.35)}
    .panel-cta{
      display:flex; align-items:flex-end; justify-content:space-between; gap:10px;
      background: linear-gradient(135deg, rgba(255,204,0,.13), rgba(94,234,212,.10));
      border:1px solid rgba(255,255,255,.08);
    }
    .panel-cta p{margin:8px 0 0 0; color:var(--muted); font-size:13px}

    /* Hero */
    .hero{padding:28px 0 12px}
    .hero-wrap{
      display:grid; grid-template-columns: 1.1fr .9fr; gap:18px;
      align-items:stretch;
    }
    .hero-card{
      border-radius: calc(var(--radius) + 8px);
      border:1px solid rgba(255,255,255,.08);
      background: linear-gradient(135deg, rgba(255,255,255,.05), rgba(255,255,255,.02));
      box-shadow: var(--shadow);
      overflow:hidden;
      position:relative;
      min-height: 340px;
    }
    .hero-card .content{padding:26px}
    .hero h1{margin:12px 0 10px; font-size:42px; line-height:1.08}
    .hero p{margin:0; color: var(--muted); max-width: 52ch}
    .hero .row{display:flex; gap:10px; margin-top:16px; flex-wrap:wrap}

    .hero-visual{
      position:absolute; inset:auto -80px -90px auto;
      width:520px; height:520px;
      border-radius: 50%;
      background:
        radial-gradient(circle at 30% 30%, rgba(255,204,0,.9), rgba(255,204,0,.0) 55%),
        radial-gradient(circle at 60% 60%, rgba(94,234,212,.85), rgba(94,234,212,.0) 58%);
      filter: blur(0px);
      opacity: .9;
      transform: rotate(18deg);
    }

    .hero-side{
      display:grid; gap:18px;
    }
    .mini-card{
      border-radius: var(--radius);
      border:1px solid rgba(255,255,255,.08);
      background: rgba(18,20,26,.6);
      box-shadow: var(--shadow);
      padding:18px;
      min-height: 160px;
      position:relative;
      overflow:hidden;
    }
    .mini-card h3{margin:0 0 6px 0}
    .mini-card p{margin:0; color:var(--muted); font-size:14px}
    .mini-card .badge{
      position:absolute; top:14px; right:14px;
      font-weight:800; font-size:12px; letter-spacing:.4px;
      padding:6px 10px; border-radius:999px;
      background: rgba(255,204,0,.16);
      border:1px solid rgba(255,204,0,.25);
      color: #ffe9a1;
    }
    .mini-card .bg{
      position:absolute; inset:auto -30px -30px auto;
      width:240px; height:240px; border-radius:50%;
      background: radial-gradient(circle at 30% 30%, rgba(255,255,255,.08), transparent 60%);
    }

    /* Sections */
    section{padding:22px 0}
    .section-head{
      display:flex; align-items:flex-end; justify-content:space-between; gap:12px;
      margin-bottom:12px;
    }
    .section-head h2{margin:0; font-size:22px}
    .section-head p{margin:0; color:var(--muted); font-size:14px}
    .cards{
      display:grid;
      grid-template-columns: repeat(12, 1fr);
      gap:14px;
    }
    .card{
      grid-column: span 3;
      border-radius: var(--radius);
      border:1px solid rgba(255,255,255,.08);
      background: rgba(18,20,26,.55);
      box-shadow: var(--shadow);
      overflow:hidden;
      display:flex; flex-direction:column;
      min-height: 280px;
    }
    .card .thumb{
      height:150px;
      background:
        linear-gradient(135deg, rgba(255,204,0,.12), rgba(94,234,212,.10)),
        radial-gradient(300px 160px at 20% 30%, rgba(255,255,255,.10), transparent 60%),
        rgba(255,255,255,.03);
      border-bottom:1px solid rgba(255,255,255,.06);
      position:relative;
    }
    .card .thumb::after{
      content:"";
      position:absolute; inset:16px;
      border-radius: 14px;
      border:1px dashed rgba(255,255,255,.12);
    }
    .card .body{padding:14px}
    .card .title{font-weight:700}
    .card .meta{display:flex; justify-content:space-between; gap:10px; margin-top:8px; color:var(--muted); font-size:13px}
    .price{color:#eafff9}
    .strike{opacity:.55; text-decoration: line-through; margin-left:8px}
    .card .footer{
      margin-top:auto; padding:12px 14px; border-top:1px solid rgba(255,255,255,.06);
      display:flex; gap:10px; align-items:center; justify-content:space-between;
    }
    .chip{font-size:12px; color:var(--muted); border:1px solid rgba(255,255,255,.08); padding:6px 10px; border-radius:999px}

    /* Category tiles */
    .tiles{
      display:grid; grid-template-columns: repeat(12, 1fr); gap:14px;
    }
    .tile{
      grid-column: span 4;
      border-radius: var(--radius);
      border:1px solid rgba(255,255,255,.08);
      background: rgba(18,20,26,.55);
      box-shadow: var(--shadow);
      padding:16px;
      display:flex; align-items:center; justify-content:space-between; gap:12px;
      min-height: 96px;
    }
    .tile h3{margin:0; font-size:16px}
    .tile span{color:var(--muted); font-size:13px}
    .tile .dot{
      width:44px; height:44px; border-radius:14px;
      background: linear-gradient(135deg, rgba(255,204,0,.22), rgba(94,234,212,.14));
      border:1px solid rgba(255,255,255,.10);
    }

    /* Promo band */
    .promo{
      border-radius: calc(var(--radius) + 8px);
      border:1px solid rgba(255,255,255,.08);
      background: linear-gradient(135deg, rgba(255,204,0,.10), rgba(94,234,212,.08));
      box-shadow: var(--shadow);
      padding:18px;
      display:flex; align-items:center; justify-content:space-between; gap:12px;
      flex-wrap:wrap;
    }
    .promo b{color:#fff2b3}
    .promo p{margin:6px 0 0; color:var(--muted)}

    /* Store + newsletter */
    .split{
      display:grid; grid-template-columns: 1fr 1fr; gap:14px;
    }
    .panel{
      border-radius: var(--radius);
      border:1px solid rgba(255,255,255,.08);
      background: rgba(18,20,26,.55);
      box-shadow: var(--shadow);
      padding:18px;
    }
    .panel h3{margin:0 0 6px}
    .panel p{margin:0; color:var(--muted)}
    .form{
      margin-top:12px;
      display:flex; gap:10px; flex-wrap:wrap;
    }
    .form input{
      flex:1;
      min-width: 220px;
      padding:12px 14px;
      border-radius: 999px;
      border:1px solid rgba(255,255,255,.10);
      background: rgba(255,255,255,.03);
      color: var(--text);
      outline:none;
    }

    /* Footer */
    footer{
      margin-top: 18px;
      border-top:1px solid rgba(255,255,255,.06);
      padding:22px 0 40px;
      color: var(--muted);
    }
    .foot{
      display:grid; grid-template-columns: 2fr 1fr 1fr 1fr; gap:14px;
    }
    .foot h4{margin:0 0 10px; color: rgba(238,242,255,.9)}
    .foot a{display:block; padding:6px 0}
    .foot a:hover{color: var(--text)}
    .copyright{margin-top:18px; font-size:13px}

    /* Cart drawer */
    .drawer-backdrop{
      position:fixed; inset:0;
      background: rgba(0,0,0,.55);
      display:none;
      z-index: 100;
    }
    .drawer{
      position:fixed; top:0; right:0; height:100%; width:min(420px, 92vw);
      background: rgba(18,20,26,.96);
      border-left:1px solid rgba(255,255,255,.08);
      box-shadow: var(--shadow);
      transform: translateX(100%);
      transition: .25s ease;
      z-index: 110;
      display:flex; flex-direction:column;
    }
    .drawer.open{transform: translateX(0)}
    .drawer-backdrop.open{display:block}
    .drawer header{position:static; background:transparent; border:0}
    .drawer .head{
      display:flex; align-items:center; justify-content:space-between;
      padding:14px 16px;
      border-bottom:1px solid rgba(255,255,255,.06);
    }
    .drawer .items{padding:14px 16px; display:grid; gap:10px; overflow:auto}
    .lineitem{
      display:flex; gap:10px; align-items:center;
      border:1px solid rgba(255,255,255,.08);
      background: rgba(255,255,255,.02);
      border-radius: 14px;
      padding:10px;
    }
    .lineitem .ph{
      width:52px; height:52px; border-radius: 14px;
      background: linear-gradient(135deg, rgba(255,204,0,.10), rgba(94,234,212,.08));
      border:1px solid rgba(255,255,255,.08);
    }
    .lineitem .info{flex:1}
    .lineitem .info b{display:block; font-size:14px}
    .lineitem .info span{color:var(--muted); font-size:12px}
    .drawer .foot{
      margin-top:auto;
      padding:14px 16px;
      border-top:1px solid rgba(255,255,255,.06);
      display:grid; gap:10px;
    }
    .totals{display:flex; justify-content:space-between; color: rgba(238,242,255,.9)}
    .toast{
      position: fixed; left: 50%; bottom: 16px;
      transform: translateX(-50%);
      background: rgba(18,20,26,.95);
      border: 1px solid rgba(255,255,255,.10);
      padding: 10px 12px;
      border-radius: 999px;
      box-shadow: var(--shadow);
      display:none;
      z-index: 200;
      color: rgba(238,242,255,.95);
      font-size: 13px;
    }
    .toast.show{display:block}

    /* Responsive */
    @media (max-width: 980px){
      .hero-wrap{grid-template-columns: 1fr; }
      .search{display:none}
      .cards .card{grid-column: span 6}
      .tiles .tile{grid-column: span 6}
      .foot{grid-template-columns: 1fr 1fr}
    }
    @media (max-width: 640px){
      .menu{display:none}
      .hamburger{display:grid}
      .cards .card{grid-column: span 12}
      .tiles .tile{grid-column: span 12}
      .split{grid-template-columns: 1fr}
      .hero h1{font-size:34px}
    }
  </style>