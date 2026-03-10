<script setup>
defineProps({
  title: String,
  subtitle: String
});
</script>

<template>
  <div class="card">
    <div class="head">
      <div>
        <h1>{{ title }}</h1>
        <small class="muted">{{ subtitle }}</small>
      </div>
      <div style="display:flex;gap:10px;flex-wrap:wrap">
        <slot name="actions" />
      </div>
    </div>

    <div class="body">
      <slot />
    </div>
  </div>
</template>