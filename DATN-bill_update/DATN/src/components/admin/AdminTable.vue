<script setup>
defineProps({
  columns: Array,
  rows: Array
});
</script>

<template>
  <table class="table">
    <thead>
      <tr>
        <th v-for="col in columns" :key="col.key" :style="col.style">
          {{ col.label }}
        </th>
      </tr>
    </thead>

    <tbody>
      <tr v-for="row in rows" :key="row.id">
        <td v-for="col in columns" :key="col.key">
          <slot :name="col.key" :row="row">
            {{ row[col.key] }}
          </slot>
        </td>
      </tr>
    </tbody>
  </table>
</template>