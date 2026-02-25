<template>
  <div>
    <h1>Product Detail Page</h1>
    <p>Product ID: {{ $route.params.productId }}</p>
  </div>
</template>

<script>
export default {
  name: 'ProductDetail',
}
</script>