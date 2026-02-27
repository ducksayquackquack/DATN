<script setup>
</script>

<template>
  <div class="return-wrapper">

    <!-- HEADER -->
    <div class="return-header">
      <h2>Trả hàng</h2>
    </div>

    <!-- MAIN CARD -->
    <div class="return-card">

      <div class="search-section">

        <label>Mã hóa đơn:</label>

        <div class="search-box">
          <input type="text" placeholder="Nhập mã hóa đơn (VD: HD13)" />
          <button class="btn-primary">Tìm kiếm</button>
          <button class="btn-outline">Quét mã</button>
        </div>

      </div>

      <!-- EMPTY STATE -->
      <div class="empty-state">
        <div class="illustration">
          🚚
        </div>
        <h3>TRẢ HÀNG</h3>
        <p>Nhập mã hóa đơn để bắt đầu xử lý trả hàng</p>
      </div>

    </div>
  </div>
</template>

<style scoped>

/* ===== Wrapper ===== */
.return-wrapper {
  padding: 28px;
}

/* ===== Header ===== */
.return-header h2 {
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 20px;
}

/* ===== Card ===== */
.return-card {
  background: white;
  border-radius: 22px;
  padding: 40px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.08);
  min-height: 450px;
}

/* ===== Search Section ===== */
.search-section {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 40px;
}

.search-section label {
  font-weight: 500;
  color: #334155;
}

.search-box {
  display: flex;
  gap: 12px;
  flex: 1;
}

.search-box input {
  flex: 1;
  padding: 10px 16px;
  border-radius: 12px;
  border: 1px solid #cbd5e1;
  outline: none;
  transition: 0.2s;
}

.search-box input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
}

/* ===== Buttons ===== */
.btn-primary {
  background: linear-gradient(135deg, #2563eb, #1e40af);
  color: white;
  border: none;
  padding: 10px 18px;
  border-radius: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: 0.2s;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.3);
}

.btn-outline {
  border: 1.5px solid #2563eb;
  background: white;
  color: #2563eb;
  padding: 10px 18px;
  border-radius: 12px;
  cursor: pointer;
  transition: 0.2s;
}

.btn-outline:hover {
  background: #eff6ff;
}

/* ===== Empty State ===== */
.empty-state {
  text-align: center;
  margin-top: 60px;
}

.empty-state .illustration {
  font-size: 80px;
  margin-bottom: 20px;
}

.empty-state h3 {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 2px;
}

.empty-state p {
  margin-top: 8px;
  color: #64748b;
}

</style>