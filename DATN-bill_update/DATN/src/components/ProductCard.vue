<script setup>
defineProps({
  product: Object
})

defineEmits(["add"])

const formatVND = (n) =>
  new Intl.NumberFormat("vi-VN").format(n) + "₫"
</script>

<template>
  <article class="p-card">
    <div class="thumb"></div>

    <div class="body">
      <div class="title">{{ product.name }}</div>

      <div class="meta">
        <span>{{ product.cat }}</span>
        <span class="price">
          <b>{{ formatVND(product.price) }}</b>
          <span v-if="product.old" class="strike">
            {{ formatVND(product.old) }}
          </span>
        </span>
      </div>
    </div>

    <div class="footer">
      <span class="pill">{{ product.tag }}</span>
      <button class="btn" @click="$emit('add', product)">
        Thêm giỏ
      </button>
    </div>
  </article>
</template>