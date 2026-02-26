<template>
  <div class="dashboard">

    <div class="dashboard-header">
      <h2>Thống kê</h2>
    </div>

    <div class="stats-grid">

      <div class="stat-card today">
        <div class="card-top">
          <Calendar class="card-icon"/>
          <span>Hôm nay</span>
        </div>

        <div class="card-amount">
          {{ formatCurrency(data.today.revenue) }}
        </div>

        <div class="card-stats">
          <div>
            <label>Sản phẩm</label>
            <strong>{{ data.today.products }}</strong>
          </div>
          <div>
            <label>Đơn thành công</label>
            <strong>{{ data.today.success }}</strong>
          </div>
          <div>
            <label>Đơn hủy</label>
            <strong>{{ data.today.cancel }}</strong>
          </div>
          <div>
            <label>Đơn trả</label>
            <strong>{{ data.today.return }}</strong>
          </div>
        </div>
      </div>

      <div class="stat-card week">
        <div class="card-top">
          <Copy class="card-icon"/>
          <span>Tuần này</span>
        </div>

        <div class="card-amount">
          {{ formatCurrency(data.week.revenue) }}
        </div>

        <div class="card-stats">
          <div>
            <label>Sản phẩm</label>
            <strong>{{ data.week.products }}</strong>
          </div>
          <div>
            <label>Đơn thành công</label>
            <strong>{{ data.week.success }}</strong>
          </div>
          <div>
            <label>Đơn hủy</label>
            <strong>{{ data.week.cancel }}</strong>
          </div>
          <div>
            <label>Đơn trả</label>
            <strong>{{ data.week.return }}</strong>
          </div>
        </div>
      </div>

      <div class="stat-card month">
        <div class="card-top">
          <FileText class="card-icon"/>
          <span>Tháng này</span>
        </div>

        <div class="card-amount">
          {{ formatCurrency(data.month.revenue) }}
        </div>

        <div class="card-stats">
          <div>
            <label>Sản phẩm</label>
            <strong>{{ data.month.products }}</strong>
          </div>
          <div>
            <label>Đơn thành công</label>
            <strong>{{ data.month.success }}</strong>
          </div>
          <div>
            <label>Đơn hủy</label>
            <strong>{{ data.month.cancel }}</strong>
          </div>
          <div>
            <label>Đơn trả</label>
            <strong>{{ data.month.return }}</strong>
          </div>
        </div>
      </div>

      <div class="stat-card year">
        <div class="card-top">
          <BarChart3 class="card-icon"/>
          <span>Năm nay</span>
        </div>

        <div class="card-amount">
          {{ formatCurrency(data.year.revenue) }}
        </div>

        <div class="card-stats">
          <div>
            <label>Sản phẩm</label>
            <strong>{{ data.year.products }}</strong>
          </div>
          <div>
            <label>Đơn thành công</label>
            <strong>{{ data.year.success }}</strong>
          </div>
          <div>
            <label>Đơn hủy</label>
            <strong>{{ data.year.cancel }}</strong>
          </div>
          <div>
            <label>Đơn trả</label>
            <strong>{{ data.year.return }}</strong>
          </div>
        </div>
      </div>

    </div>

    <div class="filter-section">

      <div class="filter-left">
        <button
          v-for="item in filters"
          :key="item"
          :class="['filter-btn', activeFilter === item ? 'active' : '']"
          @click="activeFilter = item"
        >
          {{ item }}
        </button>
      </div>

      <div class="filter-right">
        <button class="export-btn">
          EXPORT TO EXCEL
        </button>
      </div>

    </div>

    <div class="content-grid">

      <div class="content-card">
        <h3>Danh sách sản phẩm bán chạy theo ngày</h3>

        <div class="mock-chart">
          <div class="bar" v-for="n in 10" :key="n" :style="{ height: (20 + n*8) + 'px' }"></div>
        </div>
      </div>

      <div class="content-card">
        <h3>Biểu đồ trạng thái ngày</h3>

        <div class="mock-chart-circle">
          <div class="circle"></div>
        </div>
      </div>

    </div>

  </div>
</template>

<script setup>
import { ref } from "vue"
import {
  Calendar,
  Copy,
  FileText,
  BarChart3
} from "lucide-vue-next"

const activeFilter = ref("NGÀY")

const filters = ["NGÀY", "TUẦN", "THÁNG", "NĂM", "TÙY CHỈNH"]

const data = {
  today: { revenue: 0, products: 0, success: 0, cancel: 0, return: 0 },
  week: { revenue: 2794651, products: 16, success: 4, cancel: 1, return: 1 },
  month: { revenue: 2794651, products: 16, success: 4, cancel: 1, return: 1 },
  year: { revenue: 2794651, products: 16, success: 4, cancel: 1, return: 1 }
}

const formatCurrency = (val) => {
  return val.toLocaleString("vi-VN") + " đ"
}
</script>

<style scoped>

.dashboard {
  padding: 10px 5px 30px 5px;
  color: #ffffff;
}

.dashboard-header {
  margin-bottom: 25px;
}

.dashboard-header h2 {
  font-weight: 600;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  border-radius: 14px;
  padding: 25px;
  color: white;
  transition: transform .25s ease, box-shadow .25s ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 15px 30px rgba(0,0,0,.2);
}

.card-top {
  display: flex;
  align-items: center;
  gap: 10px;
  opacity: .9;
}

.card-icon {
  width: 18px;
}

.card-amount {
  font-size: 26px;
  font-weight: 700;
  margin: 10px 0 15px 0;
}

.card-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  font-size: 13px;
}

.card-stats label {
  display: block;
  opacity: .8;
}

.card-stats strong {
  display: block;
  margin-top: 3px;
}

.today { background: #0e7490; }
.week { background: #fb923c; }
.month { background: #3b82f6; }
.year { background: #059669; }

.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f3f4f6;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 30px;
}

.filter-btn {
  border: 1px solid #d1d5db;
  padding: 8px 16px;
  border-radius: 8px;
  background: white;
  margin-right: 10px;
  cursor: pointer;
  font-weight: 500;
}

.filter-btn.active {
  background: #f97316;
  color: white;
  border-color: #f97316;
}

.export-btn {
  border: 1px solid #10b981;
  padding: 8px 16px;
  border-radius: 8px;
  background: white;
  color: #10b981;
  font-weight: 600;
  cursor: pointer;
}

.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.content-card {
  background: white;
  border-radius: 14px;
  padding: 20px;
}

.content-card h3 {
  margin-bottom: 20px;
}

.mock-chart {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  height: 200px;
}

.bar {
  width: 20px;
  background: #f97316;
  border-radius: 6px 6px 0 0;
}

.mock-chart-circle {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.circle {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: conic-gradient(#3b82f6 60%, #e5e7eb 40%);
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