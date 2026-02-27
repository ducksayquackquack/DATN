<script setup>
</script>

<template>
  <div class="pos-wrapper">

    <!-- HEADER -->
    <div class="pos-header">
      <h2>Bán hàng</h2>
      <button class="btn-primary">
        + Tạo đơn hàng
      </button>
    </div>

    <!-- ORDER TABS -->
    <div class="order-tabs">
      <div class="tab active">
        <span>Đơn hàng 1 - HD10</span>
        <div class="tab-icons">
          <i class="icon cart"></i>
          <i class="icon close"></i>
        </div>
      </div>

      <div class="tab">
        <span>Đơn hàng 2 - HD11</span>
        <div class="tab-icons">
          <i class="icon cart"></i>
          <i class="icon close"></i>
        </div>
      </div>
    </div>

    <!-- MAIN CONTENT -->
    <div class="pos-grid">

      <!-- LEFT SIDE -->
      <div class="card product-section">

        <div class="section-header">
          <h3>Sản phẩm</h3>
          <div>
            <button class="btn-outline">Quét QR</button>
            <button class="btn-outline">Thêm sản phẩm</button>
          </div>
        </div>

        <!-- Demo product -->
        <div class="product-item">
          <img src="https://via.placeholder.com/80" />

          <div class="info">
            <strong>Converse Venom</strong>
            <p>712.500 VND</p>
            <small>Size: 41</small>
          </div>

          <div class="quantity">
            <button>-</button>
            <span>3</span>
            <button>+</button>
          </div>

          <div class="price">
            2.137.500 VND
          </div>

          <button class="btn-delete">
            <i class="icon trash"></i>
          </button>
        </div>

        <div class="total-line">
          Tổng tiền:
          <strong>2.377.500 VND</strong>
        </div>

      </div>

      <!-- RIGHT SIDE -->
      <div class="card customer-section">

        <div class="section-header">
          <h3>Khách hàng</h3>
          <button class="btn-outline">Chọn khách hàng</button>
        </div>

        <div class="customer-info">
          <p><strong>Tên khách hàng:</strong> Khách lẻ</p>
        </div>

        <div class="payment-summary">
          <div class="row">
            <span>Tiền hàng</span>
            <span>2.377.500 VND</span>
          </div>
          <div class="row">
            <span>Phí vận chuyển</span>
            <span>0 VND</span>
          </div>
          <div class="row discount">
            <span>Giảm giá</span>
            <span>250.000 VND</span>
          </div>
          <div class="row total">
            <span>Tổng số tiền</span>
            <span>2.127.500 VND</span>
          </div>
        </div>

        <button class="btn-confirm">
          XÁC NHẬN ĐẶT HÀNG
        </button>

      </div>

    </div>
  </div>
</template>

<style scoped>

/* ===== Wrapper ===== */
.pos-wrapper {
  padding: 28px;
}

/* ===== Header ===== */
.pos-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pos-header h2 {
  font-weight: 600;
  color: #ffffff;
}

/* ===== Primary Button (Blue Theme) ===== */
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

/* ===== Tabs ===== */
.order-tabs {
  display: flex;
  gap: 14px;
  margin: 20px 0;
}

.tab {
  background: #f1f5f9;
  padding: 10px 16px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  color: #334155;
}

.tab.active {
  background: white;
  border-bottom: 3px solid #2563eb;
  font-weight: 600;
}

.tab-icons {
  display: flex;
  gap: 8px;
}

/* ===== Grid Layout ===== */
.pos-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

/* ===== Cards ===== */
.card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.08);
}

/* ===== Section Header ===== */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  color: #1e293b;
}

/* ===== Outline Button ===== */
.btn-outline {
  border: 1.5px solid #2563eb;
  background: white;
  color: #2563eb;
  padding: 8px 14px;
  border-radius: 10px;
  cursor: pointer;
  margin-left: 6px;
  transition: 0.2s;
}

.btn-outline:hover {
  background: #eff6ff;
}

/* ===== Product Item ===== */
.product-item {
  display: grid;
  grid-template-columns: 80px 1fr auto auto auto;
  gap: 16px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #e2e8f0;
}

.product-item strong {
  color: #1e293b;
}

.product-item p,
.product-item small {
  color: #64748b;
}

.quantity {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity button {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: white;
}

.price {
  color: #dc2626;
  font-weight: 600;
}

/* ===== Delete Button ===== */
.btn-delete {
  background: #ef4444;
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  cursor: pointer;
}

/* ===== Totals ===== */
.total-line {
  text-align: right;
  margin-top: 16px;
  font-size: 15px;
  color: #1e293b;
}

/* ===== Payment ===== */
.payment-summary .row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #475569;
}

.payment-summary .discount {
  color: #2563eb;
}

.payment-summary .total {
  font-weight: 700;
  color: #dc2626;
  margin-top: 10px;
}

.btn-confirm {
  margin-top: 24px;
  width: 100%;
  background: linear-gradient(135deg, #2563eb, #1e40af);
  color: white;
  padding: 14px;
  border-radius: 14px;
  border: none;
  font-weight: 600;
  cursor: pointer;
  transition: 0.2s;
}

.btn-confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(37, 99, 235, 0.3);
}

/* ===== Icons (Clean CSS Icons Instead of Emoji) ===== */
.icon {
  width: 14px;
  height: 14px;
  display: inline-block;
}

.icon.cart {
  background: #2563eb;
  mask: url('https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/icons/cart.svg') no-repeat center;
  -webkit-mask: url('https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/icons/cart.svg') no-repeat center;
}

.icon.close {
  background: #64748b;
  mask: url('https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/icons/x.svg') no-repeat center;
  -webkit-mask: url('https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/icons/x.svg') no-repeat center;
}

.icon.trash {
  background: white;
  mask: url('https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/icons/trash.svg') no-repeat center;
  -webkit-mask: url('https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.5/icons/trash.svg') no-repeat center;
}

</style>