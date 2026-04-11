<script setup>
import { computed } from "vue"
import { useRoute } from "vue-router"
import AddDiscountPage from "./dot-giam-gia/AddDiscountPage.vue"
import DetailDiscountPage from "./dot-giam-gia/DetailDiscountPage.vue"

const route = useRoute()
const hasId = computed(() => Boolean(route.params.id))
const activeComponent = computed(() => (hasId.value ? DetailDiscountPage : AddDiscountPage))
</script>

<template>
  <component :is="activeComponent" />
</template>