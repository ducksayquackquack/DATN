<template>
  <div class="home">

    <h1>DirtyWave Store</h1>

    <hr />

    <!-- CATEGORIES -->
    <section>
      <h2>Categories</h2>

      <div class="category-list">
        <div 
          v-for="cat in categories" 
          :key="cat.id"
          class="category-box"
        >
          <div><strong>ID:</strong> {{ cat.id }}</div>
          <div><strong>Mã:</strong> {{ cat.maDanhMuc }}</div>
          <div><strong>Tên:</strong> {{ cat.tenDanhMuc }}</div>
        </div>
      </div>
    </section>

    <hr />

    <!-- PRODUCTS -->
    <section>
      <h2>Products</h2>

      <div class="product-list">
        <div 
          v-for="product in products" 
          :key="product.id"
          class="product-box"
        >
          <div><strong>ID:</strong> {{ product.id }}</div>
          <div><strong>Mã SP:</strong> {{ product.maSanPham }}</div>
          <div><strong>Tên:</strong> {{ product.tenSanPham }}</div>
          <div><strong>Mô tả:</strong> {{ product.moTa }}</div>
          <div><strong>Danh mục ID:</strong> {{ product.idDanhMuc }}</div>

          <div><strong>Giá:</strong> {{ formatPrice(product.giaBan) }}</div>

          <div v-if="product.giaTriKhuyenMai">
            <strong>Khuyến mãi:</strong> {{ product.giaTriKhuyenMai }}%
            <br />
            <strong>Giá sau KM:</strong>
            {{ formatPrice(discountPrice(product)) }}
          </div>
        </div>
      </div>
    </section>

  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "Home",

  data() {
    return {
      products: [],
      categories: []
    };
  },

  methods: {
    async loadHome() {
      try {
        const response = await axios.get(
          "http://localhost:8080/api/public/home"
        );

        this.products = response.data.products;
        this.categories = response.data.categories;

        console.log("HOME DATA:", response.data);

      } catch (error) {
        console.error("ERROR LOADING HOME:", error);
      }
    },

    formatPrice(price) {
      if (!price) return "0 đ";
      return new Intl.NumberFormat("vi-VN").format(price) + " đ";
    },

    discountPrice(product) {
      if (!product.giaTriKhuyenMai) return product.giaBan;

      return (
        product.giaBan *
        (1 - product.giaTriKhuyenMai / 100)
      );
    }
  },

  mounted() {
    this.loadHome();
  }
};
</script>

<style scoped>

.home {
  padding: 40px;
  font-family: Arial, sans-serif;
}

.category-list,
.product-list {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.category-box,
.product-box {
  border: 1px solid #ccc;
  padding: 15px;
  width: 250px;
  background: #f9f9f9;
}

hr {
  margin: 30px 0;
}

</style>