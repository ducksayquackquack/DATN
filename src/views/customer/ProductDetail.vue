<script setup>
import { useRoute } from "vue-router"

const route = useRoute()
const id = route.params.id
</script>

<template>
  <div style="padding:40px">
    <h1>Product ID: {{ id }}</h1>
  </div>
</template>