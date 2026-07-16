<script setup lang="ts">
defineProps<{
  title: string
  message?: string
  actionLabel?: string
}>()

defineEmits<{
  action: []
}>()
</script>

<template>
  <div class="state-message">
    <div class="state-message__title">{{ title }}</div>
    <div v-if="message" class="state-message__body">{{ message }}</div>
    <button v-if="actionLabel" class="secondary-button" type="button" @click="$emit('action')">
      {{ actionLabel }}
    </button>
  </div>
</template>

<style scoped>
.state-message {
  display: flex;
  min-height: 180px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 32px;
  color: #64748b;
  text-align: center;
}

.state-message__title {
  color: #334155;
  font-size: 16px;
  font-weight: 800;
}

.state-message__body {
  max-width: 420px;
  font-size: 14px;
}
</style>
