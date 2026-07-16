<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    number: string | number
    color: 'red' | 'blue'
    size?: 'sm' | 'md' | 'lg'
  }>(),
  {
    size: 'md',
  },
)

const displayNumber = computed(() => String(props.number).padStart(2, '0'))
</script>

<template>
  <span class="number-ball" :class="[`number-ball--${color}`, `number-ball--${size}`]">
    {{ displayNumber }}
  </span>
</template>

<style scoped>
.number-ball {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  color: #ffffff;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-weight: 800;
  box-shadow: 0 1px 3px rgb(15 23 42 / 0.12);
}

.number-ball--sm {
  width: 32px;
  height: 32px;
  font-size: 14px;
}

.number-ball--md {
  width: 40px;
  height: 40px;
  font-size: 16px;
}

.number-ball--lg {
  width: 48px;
  height: 48px;
  font-size: 18px;
}

.number-ball--red {
  background: linear-gradient(180deg, #f87171 0%, #ef4444 100%);
  box-shadow: 0 3px 8px rgb(239 68 68 / 0.22);
}

.number-ball--blue {
  background: linear-gradient(180deg, #60a5fa 0%, #3b82f6 100%);
  box-shadow: 0 3px 8px rgb(59 130 246 / 0.22);
}
</style>
