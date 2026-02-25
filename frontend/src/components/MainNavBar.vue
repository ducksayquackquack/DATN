<template>
  <div class="navbar-wrapper">

    <!-- NAVBAR -->
    <div class="navbar">

      <div class="logo">DIRTYWAVE</div>

      <div class="menu">

        <!-- SAN PHAM -->
        <div class="menu-item"
             @mouseenter="active = 'sanpham'"
             @mouseleave="active = null">
          Sản phẩm
          <span class="underline"></span>
        </div>

        <!-- DENIM -->
        <div class="menu-item"
             @mouseenter="active = 'denim'"
             @mouseleave="active = null">
          DENIM
          <span class="underline"></span>
        </div>

        <div class="menu-item">
          OUTLET
          <span class="underline"></span>
        </div>

      </div>

      <!-- ICONS -->
      <div class="icons">
        <i class="icon">🔍</i>
        <i class="icon">👤</i>
        <i class="icon">🛒</i>
      </div>
    </div>

    <!-- MEGA MENU SAN PHAM -->
    <div v-if="active === 'sanpham'" class="mega">
      <div class="mega-left">
        <h4>TẤT CẢ SẢN PHẨM</h4>
        <p>Sản phẩm mới</p>
        <p>Bán chạy nhất</p>
        <p>OUTLET - Sale up to 50%</p>
      </div>

      <div class="mega-right">
        <div class="image-card">
          <img src="https://images.unsplash.com/photo-1520975693415-3e36d53fbd13?q=80&w=600"/>
          <span>Đồ Thu Đông</span>
        </div>
        <div class="image-card">
          <img src="https://images.unsplash.com/photo-1516822003754-cca485356ecb?q=80&w=600"/>
          <span>Retro Sports</span>
        </div>
      </div>
    </div>

    <!-- MEGA MENU DENIM -->
    <div v-if="active === 'denim'" class="mega">
      <div class="mega-left">
        <h4>JEANS</h4>
        <p>Quần Jeans</p>
        <p>Quần Short Jeans</p>
        <p>Áo Khoác Jeans</p>
      </div>

      <div class="mega-right">
        <div class="image-card">
          <img src="https://images.unsplash.com/photo-1520974735194-4c6c8d49f1b8?q=80&w=600"/>
          <span>AirFlex</span>
        </div>
        <div class="image-card">
          <img src="https://images.unsplash.com/photo-1520975922284-7b96b567a14a?q=80&w=600"/>
          <span>Smart Jeans</span>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
export default {
  data() {
    return {
      active: null
    };
  }
};
</script>

<style scoped>

.navbar-wrapper {
  position: relative;
}

.navbar {
  height: 70px;
  background: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
  border-bottom: 1px solid #eee;
}

.logo {
  font-weight: bold;
  font-size: 22px;
}

.menu {
  display: flex;
  gap: 40px;
}

.menu-item {
  position: relative;
  cursor: pointer;
  font-weight: 500;
}

.underline {
  position: absolute;
  bottom: -5px;
  left: 0;
  width: 0%;
  height: 2px;
  background: black;
  transition: 0.3s ease;
}

.menu-item:hover .underline {
  width: 100%;
}

/* ICONS BLACK WHITE */
.icons {
  display: flex;
  gap: 20px;
}

.icon {
  font-size: 18px;
  cursor: pointer;
  filter: grayscale(100%);
  transition: 0.2s;
}

.icon:hover {
  filter: grayscale(0%);
}

/* MEGA MENU */

.mega {
  position: absolute;
  top: 70px;
  left: 0;
  width: 100%;
  background: white;
  display: flex;
  justify-content: space-between;
  padding: 40px 80px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
  z-index: 10;
}

.mega-left p {
  margin: 10px 0;
  cursor: pointer;
}

.mega-right {
  display: flex;
  gap: 20px;
}

.image-card {
  position: relative;
  width: 200px;
  height: 120px;
  overflow: hidden;
  border-radius: 10px;
}

.image-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-card span {
  position: absolute;
  bottom: 10px;
  left: 10px;
  color: white;
  font-weight: bold;
}

</style>