<template>
  <div class="dashboard-page">

    <div class="page-header">
      <h2>Thống kê</h2>
    </div>

    <div class="stats-grid">
      <div class="stat-card today">
        <div class="card-title">Hôm nay</div>
        <div class="card-amount">0 đ</div>
        <div class="card-meta">
          <span>Sản phẩm: 0</span>
          <span>Đơn thành công: 0</span>
          <span>Đơn hủy: 0</span>
          <span>Đơn trả: 0</span>
        </div>
      </div>

      <div class="stat-card week">
        <div class="card-title">Tuần này</div>
        <div class="card-amount">2,794,651 đ</div>
        <div class="card-meta">
          <span>Sản phẩm: 16</span>
          <span>Đơn thành công: 4</span>
          <span>Đơn hủy: 1</span>
          <span>Đơn trả: 1</span>
        </div>
      </div>

      <div class="stat-card month">
        <div class="card-title">Tháng này</div>
        <div class="card-amount">2,794,651 đ</div>
        <div class="card-meta">
          <span>Sản phẩm: 16</span>
          <span>Đơn thành công: 4</span>
          <span>Đơn hủy: 1</span>
          <span>Đơn trả: 1</span>
        </div>
      </div>

      <div class="stat-card year">
        <div class="card-title">Năm nay</div>
        <div class="card-amount">2,794,651 đ</div>
        <div class="card-meta">
          <span>Sản phẩm: 16</span>
          <span>Đơn thành công: 4</span>
          <span>Đơn hủy: 1</span>
          <span>Đơn trả: 1</span>
        </div>
      </div>
    </div>

    <div class="filter-section">
      <div class="filter-left">
        <button class="filter-btn active">NGÀY</button>
        <button class="filter-btn">TUẦN</button>
        <button class="filter-btn">THÁNG</button>
        <button class="filter-btn">NĂM</button>
        <button class="filter-btn">TÙY CHỈNH</button>
      </div>

      <button class="export-btn">
        EXPORT TO EXCEL
      </button>
    </div>

    <div class="content-grid">
      <div class="content-card">
        <h3>Danh sách sản phẩm bán chạy</h3>
        <div class="mock-chart">
          <div v-for="n in 8" :key="n" class="bar"></div>
        </div>
      </div>

      <div class="content-card">
        <h3>Biểu đồ trạng thái</h3>
        <div class="mock-chart-circle">
          <div class="circle"></div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
</script>

<style scoped>

.dashboard-page {
  padding: 30px;
  color: #e5e7eb;
}

.page-header {
  margin-bottom: 25px;
}

.page-header h2 {
  font-size: 26px;
  font-weight: 600;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  border-radius: 16px;
  padding: 24px;
  color: white;
  transition: transform .25s ease, box-shadow .25s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(0,0,0,.35);
}

.card-title {
  font-size: 15px;
  opacity: .9;
}

.card-amount {
  font-size: 26px;
  font-weight: 700;
  margin: 10px 0 15px 0;
}

.card-meta {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
  font-size: 13px;
  opacity: .9;
}

.today { background: linear-gradient(135deg,#0ea5e9,#06b6d4); }
.week { background: linear-gradient(135deg,#fb923c,#f97316); }
.month { background: linear-gradient(135deg,#3b82f6,#2563eb); }
.year { background: linear-gradient(135deg,#10b981,#059669); }

.filter-section {
  background: #111827;
  padding: 20px;
  border-radius: 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.filter-btn {
  border: 1px solid #374151;
  padding: 8px 16px;
  border-radius: 8px;
  background: transparent;
  color: #e5e7eb;
  margin-right: 10px;
  cursor: pointer;
  transition: .2s;
}

.filter-btn.active {
  background: #f97316;
  border-color: #f97316;
}

.export-btn {
  border: 1px solid #10b981;
  padding: 8px 16px;
  border-radius: 8px;
  background: transparent;
  color: #10b981;
  cursor: pointer;
  transition: .2s;
}

.export-btn:hover {
  background: #10b981;
  color: white;
}

.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.content-card {
  background: #111827;
  border-radius: 16px;
  padding: 20px;
}

.content-card h3 {
  margin-bottom: 20px;
}

.mock-chart {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  height: 180px;
}

.bar {
  width: 22px;
  background: #f97316;
  border-radius: 6px 6px 0 0;
  height: 60%;
}

.mock-chart-circle {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 180px;
}

.circle {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: conic-gradient(#10b981 45%, #f97316 25%, #3b82f6 30%);
}

@media (max-width: 1000px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .content-grid {
    grid-template-columns: 1fr;
  }
}

</style>

