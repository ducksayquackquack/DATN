<template>
  <div class="promo-bar">
    <div class="promo-track">
      <div class="promo-item" v-for="(item, index) in promos" :key="index">
        {{ item }}
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "PromoBar",
  data() {
    return {
      promos: [
        "🎉 RINH LỘC ĐẦU NĂM – KHAI BILL ĐÓN VẬN",
        "🔥 VOUCHER 10% TỐI ĐA 10K",
        "💰 VOUCHER 20K ĐƠN TỪ 499K",
        "🚚 Freeship đơn từ 399K"
      ]
    };
  }
};
</script>

<style scoped>
.promo-bar {
  background: #c9141c;
  color: white;
  overflow: hidden;
  white-space: nowrap;
  height: 40px;
  display: flex;
  align-items: center;
}

.promo-track {
  display: inline-block;
  animation: scroll 20s linear infinite;
}

.promo-item {
  display: inline-block;
  padding: 0 60px;
  font-weight: 500;
  font-size: 14px;
}

@keyframes scroll {
  0% {
    transform: translateX(100%);
  }
  100% {
    transform: translateX(-100%);
  }
}
</style>