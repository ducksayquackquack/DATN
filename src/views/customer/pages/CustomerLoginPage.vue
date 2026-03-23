<script setup>
import CustomerPageLayout from '../../../components/customer/CustomerPageLayout.vue'
import Login from '../../auth/Login.vue'
</script>

<template>
  <CustomerPageLayout breadcrumb="Đăng nhập" max-width="860px">
    <section class="customer-login-shell">
      <Login embedded />
    </section>
  </CustomerPageLayout>
</template>

<style scoped>
.customer-login-shell {
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
  display: flex;
  justify-content: center;
}

.customer-login-shell :deep(.auth-card) {
  width: min(560px, 100%);
}
</style>
