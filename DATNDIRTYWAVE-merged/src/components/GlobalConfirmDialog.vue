<script setup>
import { useConfirm } from '../composables/useConfirm'

const { confirmState, resolveConfirm, setPromptValue } = useConfirm()
</script>

<template>
  <div v-if="confirmState.visible" class="overlay" @click.self="resolveConfirm(false)">
    <div class="dialog">
      <h3 class="title">{{ confirmState.title }}</h3>
      <p class="message">{{ confirmState.message }}</p>

      <input
        v-if="confirmState.mode === 'prompt'"
        class="prompt-input"
        :value="confirmState.inputValue"
        :placeholder="confirmState.inputPlaceholder"
        @input="setPromptValue($event.target.value)"
        @keyup.enter="resolveConfirm(true)"
      />

      <div class="actions">
        <button
          v-if="confirmState.mode !== 'alert'"
          class="btn btn-secondary"
          @click="resolveConfirm(false)"
        >
          {{ confirmState.cancelText }}
        </button>
        <button class="btn btn-primary" @click="resolveConfirm(true)">{{ confirmState.confirmText }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 16px;
}

.dialog {
  width: min(460px, 100%);
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.25);
  padding: 18px;
}

.title {
  margin: 0;
  font-size: 18px;
  color: #111827;
}

.message {
  margin: 12px 0 18px;
  color: #374151;
  white-space: pre-line;
  line-height: 1.5;
}

.prompt-input {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  margin-bottom: 14px;
}

.prompt-input:focus {
  outline: none;
  border-color: #dc2626;
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.14);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn {
  border: none;
  border-radius: 10px;
  padding: 9px 14px;
  font-size: 14px;
  cursor: pointer;
}

.btn-secondary {
  background: #e5e7eb;
  color: #1f2937;
}

.btn-primary {
  background: #dc2626;
  color: #ffffff;
}
</style>
