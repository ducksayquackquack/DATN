<script setup>
import { ref, computed, onMounted } from "vue"
import { useRoute, useRouter } from "vue-router"
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

const route = useRoute()
const router = useRouter()
const id = route.params.id

const allProducts = [
  {id:1,name:"Áo Bomber Da Lộn DirtyWave",cat:"Bomber",price:649000,old:799000,tag:"AIRFLEX",img:img1},
  {id:2,name:"Áo Bomber Dáng Lửng",cat:"Bomber",price:399000,old:null,tag:"New",img:img2},
  {id:3,name:"Áo Bomber Giả Da DirtyWave",cat:"Bomber",price:329000,old:389000,tag:"Best",img:img3},
  {id:4,name:"Áo Bomber Cotton DirtyWave",cat:"Bomber",price:459000,old:null,tag:"Office",img:img4},
  {id:5,name:"Hoodie Zip Dáng Hộp DirtyWave",cat:"Hoodie",price:499000,old:599000,tag:"Daily",img:img5},
  {id:6,name:"Hoodie Zip In Hình DirtyWave",cat:"Hoodie",price:699000,old:null,tag:"Layer",img:img6},
  {id:7,name:"Hoodie Zip Jacket DirtyWave",cat:"Hoodie",price:559000,old:null,tag:"New",img:img7},
  {id:8,name:"Coach Cách Nhiệt Timberland",cat:"Coach",price:799000,old:899000,tag:"Drop",img:img8},
  {id:9,name:"Coach Da ASOS DirtyWave",cat:"Coach",price:899000,old:null,tag:"Premium",img:img9},
  {id:10,name:"Coach Giả Da DirtyWave",cat:"Coach",price:699000,old:null,tag:"Trend",img:img10},
  {id:11,name:"Coach Lông Cừu DirtyWave",cat:"Coach",price:399000,old:559000,tag:"399K",img:img11},
]

const profileOpen = ref(false)
const mobileOpen = ref(false)

const getProduct = (productId) => {
  return allProducts.find(p => p.id === Number(productId))
}

const currentProduct = computed(() => {
  return getProduct(id)
})

// Sample product data - Replace with API call when ready
const product = ref({
  id: id,
  name: currentProduct.value?.name || "Sản phẩm",
  category: currentProduct.value?.cat || "Sản phẩm",
  price: currentProduct.value?.price || 0,
  originalPrice: currentProduct.value?.old || null,
  sku: "QJ2025-01",
  rating: 4.5,
  reviews: 128,
  images: [currentProduct.value?.img],
  description: "Sản phẩm chất lượng cao - chất liệu cotton 100%, form thoải mái, phù hợp để mặc hàng ngày hoặc mix với các item khác.",
  colors: ["Đen", "Xanh đậm", "Xanh nhạt"],
  selectedColor: "Đen",
  sizes: [28, 29, 30, 31, 32, 34, 36],
  selectedSize: 30,
  material: "Cotton 100%",
  form: "Straight",
  weight: "420gsm",
  features: [
    { label: "Chất liệu", value: "Cotton 100%" },
    { label: "Form dáng", value: "Straight" },
    { label: "Độ co giãn", value: "Moderate" },
    { label: "Chuẩn form", value: "Oversized" }
  ],
  vouchers: [
    { code: "VOUCHER-10K", discount: "10,000₫", condition: "từ 299K" },
    { code: "VOUCHER-20K", discount: "20,000₫", condition: "từ 699K" },
    { code: "VOUCHER-30K-MEMBER", discount: "30,000₫", condition: "từ 999K" },
  ],
  promos: [
    { badge: "HÀNG CHÍNH HÃNG", color: "bg-red" },
    { badge: "MIỄN PHÍ SHIP", color: "bg-blue" },
  ],
  relatedProducts: allProducts.filter(p => p.id !== Number(id)).slice(0, 5)
})

const mainImage = ref(product.value.images[0])
const quantity = ref(1)

const VND = n => new Intl.NumberFormat("vi-VN").format(n) + "₫"

const discount = computed(() => {
  if (!product.value.originalPrice) return 0
  return Math.round(((product.value.originalPrice - product.value.price) / product.value.originalPrice) * 100)
})

const selectImage = (img) => {
  mainImage.value = img
}

const decreaseQty = () => {
  if (quantity.value > 1) quantity.value--
}

const increaseQty = () => {
  quantity.value++
}

const addToCart = () => {
  window.toast.success(`Đã thêm ${quantity.value} sản phẩm vào giỏ hàng`)
  quantity.value = 1
}

const buyNow = () => {
  window.toast.info(`Mua ngay ${quantity.value} sản phẩm`)
}

const copyVoucher = (code) => {
  navigator.clipboard?.writeText(code)
  window.toast.success(`Đã sao chép mã: ${code}`)
}

const logout = () => {
  profileOpen.value = false
  router.push("/login")
}

const toggleMobileMenu = () => {
  mobileOpen.value = !mobileOpen.value
}

onMounted(() => {
  // TODO: Fetch product details from API based on id
  console.log("Loaded product:", id)
})
</script>

<template>
  <div class="product-detail">
    <!-- Breadcrumb -->
  <div class="container breadcrumb-section">
    <a href="#" @click.prevent="router.push('/home')">Trang chủ</a>    <span>/</span>
    <span>{{ product.name }}</span>
  </div>

    <!-- Main product section -->
    <div class="container product-main">
      <!-- Left: Gallery -->
      <div class="gallery-section">
        <div class="main-image">
          <img :src="mainImage" :alt="product.name" />
        </div>

        <!-- <div class="thumbnails">
          <div
            v-for="(img, idx) in product.images"
            :key="idx"
            class="thumbnail"
            :class="{ active: mainImage === img }"
            @click="selectImage(img)"
          >
            <img :src="img" :alt="`Thumbnail ${idx + 1}`" />
          </div>
        </div> -->
      </div>

      <!-- Right: Product Info -->
      <div class="info-section">
        <!-- Badges -->
        <div class="badges">
          <span v-for="promo in product.promos" :key="promo.badge" class="badge" :class="promo.color">
            {{ promo.badge }}
          </span>
        </div>

        <!-- Title & Price -->
        <h1 class="product-title">{{ product.name }}</h1>
        <div class="price-row">
          <span class="current-price">{{ VND(product.price) }}</span>
          <span v-if="product.originalPrice" class="original-price">{{ VND(product.originalPrice) }}</span>
          <span v-if="discount" class="discount-badge">-{{ discount }}%</span>
        </div>

        <!-- Rating -->
        <div class="rating">
          <span class="stars">★★★★★</span>
          <span class="review-count">({{ product.reviews }} đánh giá)</span>
        </div>

        <!-- Vouchers -->
        <div class="vouchers-section">
          <h4>Mã giảm giá</h4>
          <div class="voucher-list">
            <div v-for="voucher in product.vouchers" :key="voucher.code" class="voucher-item">
              <div class="voucher-info">
                <span class="voucher-code">{{ voucher.code }}</span>
                <span class="voucher-discount">Giảm {{ voucher.discount }}</span>
              </div>
              <button class="voucher-copy-btn" @click="copyVoucher(voucher.code)">Sao chép</button>
            </div>
          </div>
        </div>

        <!-- Color Selection -->
        <div class="option-group">
          <label>Màu sắc</label>
          <div class="color-options">
            <button
              v-for="color in product.colors"
              :key="color"
              class="color-btn"
              :class="{ active: product.selectedColor === color }"
              @click="product.selectedColor = color"
            >
              {{ color }}
            </button>
          </div>
        </div>

        <!-- Size Selection -->
        <div class="option-group">
          <label>Kích thước</label>
          <div class="size-options">
            <button
              v-for="size in product.sizes"
              :key="size"
              class="size-btn"
              :class="{ active: product.selectedSize === size }"
              @click="product.selectedSize = size"
            >
              {{ size }}
            </button>
          </div>
          <p class="size-help">
            <a href="#size-chart">Hướng dẫn chọn size</a>
          </p>
        </div>

        <!-- Quantity Selection -->
        <div class="option-group">
          <label>Số lượng</label>
          <div class="quantity-selector">
            <button @click="decreaseQty" class="qty-btn">-</button>
            <input v-model.number="quantity" type="number" min="1" class="qty-input" />
            <button @click="increaseQty" class="qty-btn">+</button>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="action-buttons">
          <button class="btn-cart" @click="addToCart">THÊM VÀO GIỎ</button>
          <button class="btn-buy" @click="buyNow">MUA NGAY</button>
        </div>

        <!-- Info Box -->
        <div class="info-box">
          <div class="info-item">
            <span class="info-icon">📦</span>
            <div>
              <strong>Giao hàng nhanh</strong>
              <p>Freeship từ 299K</p>
            </div>
          </div>
          <div class="info-item">
            <span class="info-icon">↩️</span>
            <div>
              <strong>Đổi/Trả dễ</strong>
              <p>Trong 30 ngày</p>
            </div>
          </div>
          <div class="info-item">
            <span class="info-icon">✓</span>
            <div>
              <strong>Hàng chính hãng</strong>
              <p>Bảo hành toàn quốc</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Divider -->
    <hr class="section-divider" />

    <!-- Product Description & Features -->
    <div class="container product-details-section">
      <div class="details-grid">
        <div class="details-col">
          <h3>Thông tin sản phẩm</h3>
          <div class="feature-list">
            <div v-for="feature in product.features" :key="feature.label" class="feature-item">
              <span class="label">{{ feature.label }}</span>
              <span class="value">{{ feature.value }}</span>
            </div>
          </div>
        </div>

        <div class="details-col">
          <h3>Mô tả chi tiết</h3>
          <p>{{ product.description }}</p>
          <p style="margin-top: 12px; color: #666; font-size: 14px;">
            Sản phẩm được kiểm tra chất lượng trước khi gửi. Bảo hành 30 ngày đổi nếu có lỗi sản xuất.
          </p>
        </div>
      </div>
    </div>

    <!-- Related Products -->
    <div class="container related-section">
      <h2>Sản phẩm tương tự</h2>
      <div class="related-products">
        <div v-for="relProduct in product.relatedProducts" :key="relProduct.id" class="related-card">
          <div class="related-img">
            <img :src="relProduct.img" :alt="relProduct.name" />
            <button class="view-btn" @click="$router.push('/product/' + relProduct.id)">Xem</button>
          </div>
          <div class="related-info">
            <h4>{{ relProduct.name }}</h4>
            <p class="related-price">{{ VND(relProduct.price) }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* Breadcrumb */
.breadcrumb-section {
  padding: 20px 0;
  font-size: 13px;
  color: #666;
  border-bottom: 1px solid #f0f0f0;
}

.breadcrumb-section a {
  color: #0066cc;
  text-decoration: none;
}

.breadcrumb-section a:hover {
  text-decoration: underline;
}

/* Main Product Section */
.product-main {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  padding: 40px 0;
}

/* Gallery */
.gallery-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.main-image {
  width: 100%;
  aspect-ratio: 5 / 6;
  background: #f9f9f9;
  border-radius: 8px;
  overflow: hidden;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnails {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.thumbnail {
  aspect-ratio: 1;
  background: #f5f5f5;
  border: 2px solid transparent;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail:hover {
  border-color: #ddd;
}

.thumbnail.active {
  border-color: #000;
}

/* Info Section */
.info-section {
  padding: 20px;
}

.badges {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.badge {
  display: inline-block;
  padding: 6px 12px;
  font-size: 11px;
  font-weight: 600;
  color: white;
  border-radius: 4px;
}

.badge.bg-red {
  background: #f44336;
}

.badge.bg-blue {
  background: #2196f3;
}

.product-title {
  font-size: 24px;
  line-height: 1.3;
  margin-bottom: 15px;
  font-weight: 600;
  color: #000;
}

/* Price */
.price-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
}

.current-price {
  font-size: 28px;
  font-weight: 700;
  color: #d32f2f;
}

.original-price {
  font-size: 16px;
  color: #999;
  text-decoration: line-through;
}

.discount-badge {
  padding: 4px 8px;
  background: #fff3cd;
  border: 1px solid #ffc107;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #f57f17;
}

/* Rating */
.rating {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 20px;
}

.stars {
  color: #ffc107;
  font-size: 14px;
}

.review-count {
  font-size: 13px;
  color: #666;
}

/* Vouchers */
.vouchers-section {
  margin-bottom: 25px;
}

.vouchers-section h4 {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 12px;
  text-transform: uppercase;
  color: #333;
}

.voucher-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.voucher-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 12px;
}

.voucher-code {
  font-family: monospace;
  font-weight: 600;
  color: #000;
}

.voucher-discount {
  display: block;
  color: #999;
  font-size: 11px;
  margin-top: 2px;
}

.voucher-copy-btn {
  padding: 4px 8px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s;
}

.voucher-copy-btn:hover {
  background: #f0f0f0;
  border-color: #999;
}

/* Options */
.option-group {
  margin-bottom: 20px;
}

.option-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
  color: #333;
  text-transform: uppercase;
}

.color-options,
.size-options {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.color-btn,
.size-btn {
  padding: 8px 12px;
  background: white;
  border: 2px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
}

.color-btn:hover,
.size-btn:hover {
  border-color: #999;
}

.color-btn.active,
.size-btn.active {
  background: #000;
  color: white;
  border-color: #000;
}

.size-help {
  margin-top: 8px;
  font-size: 12px;
}

.size-help a {
  color: #0066cc;
  text-decoration: none;
}

.size-help a:hover {
  text-decoration: underline;
}

/* Quantity */
.quantity-selector {
  display: flex;
  align-items: center;
  border: 1px solid #ddd;
  border-radius: 6px;
  width: fit-content;
}

.qty-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: white;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.2s;
}

.qty-btn:hover {
  background: #f5f5f5;
}

.qty-input {
  width: 50px;
  border: none;
  text-align: center;
  font-size: 14px;
  font-weight: 500;
}

.qty-input:focus {
  outline: none;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  gap: 12px;
  margin: 25px 0;
}

.btn-cart,
.btn-buy {
  flex: 1;
  padding: 14px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.btn-cart {
  background: white;
  color: #000;
  border: 2px solid #000;
}

.btn-cart:hover {
  background: #f5f5f5;
}

.btn-buy {
  background: #1a3a52;
  color: white;
}

.btn-buy:hover {
  background: #0d1f2d;
}

/* Info Box */
.info-box {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-top: 25px;
}

.info-item {
  display: flex;
  gap: 10px;
}

.info-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.info-item strong {
  display: block;
  font-size: 13px;
  margin-bottom: 4px;
  color: #000;
}

.info-item p {
  font-size: 12px;
  color: #666;
  margin: 0;
}

/* Divider */
.section-divider {
  border: none;
  border-top: 1px solid #f0f0f0;
  margin: 40px 0;
}

/* Details Section */
.product-details-section {
  padding: 40px 0;
}

.details-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
}

.details-col h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #000;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

.feature-item .label {
  color: #666;
  font-weight: 500;
}

.feature-item .value {
  color: #000;
  font-weight: 600;
}

.details-col p {
  color: #555;
  line-height: 1.6;
  font-size: 14px;
}

/* Related Products */
.related-section {
  padding: 40px 0;
  border-top: 1px solid #f0f0f0;
}

.related-section h2 {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 30px;
  color: #000;
}

.related-products {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
}

.related-card {
  cursor: pointer;
  transition: all 0.2s;
}

.related-card:hover {
  transform: translateY(-5px);
}

.related-img {
  position: relative;
  width: 100%;
  aspect-ratio: 3 / 4;
  background: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
}

.related-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.view-btn {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  padding: 6px 12px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}

.related-img:hover .view-btn {
  opacity: 1;
}

.related-info h4 {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.3;
  margin-bottom: 6px;
  color: #000;
}

.related-price {
  font-size: 16px;
  font-weight: 700;
  color: #d32f2f;
  margin: 0;
}

/* Responsive */
@media (max-width: 768px) {
  .product-main {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 20px 0;
  }

  .details-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .info-box {
    grid-template-columns: 1fr;
  }

  .related-products {
    grid-template-columns: repeat(2, 1fr);
  }

  .action-buttons {
    flex-direction: column;
  }
}
</style>