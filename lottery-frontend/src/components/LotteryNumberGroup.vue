<script setup lang="ts">
import LotteryNumberBall from '@/components/LotteryNumberBall.vue'
import { parseNumberText } from '@/utils/lotteryFormat'

withDefaults(
  defineProps<{
    frontNumbers: string | Array<string | number>
    backNumbers: string | Array<string | number>
    size?: 'sm' | 'md' | 'lg'
  }>(),
  {
    size: 'sm',
  },
)
</script>

<template>
  <div class="number-group">
    <div class="number-group__area">
      <LotteryNumberBall
        v-for="number in parseNumberText(frontNumbers)"
        :key="`front-${number}`"
        :number="number"
        color="red"
        :size="size"
      />
    </div>
    <span class="number-group__divider">+</span>
    <div class="number-group__area">
      <LotteryNumberBall
        v-for="number in parseNumberText(backNumbers)"
        :key="`back-${number}`"
        :number="number"
        color="blue"
        :size="size"
      />
    </div>
  </div>
</template>

<style scoped>
.number-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.number-group__area {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.number-group__divider {
  color: #cbd5e1;
  font-weight: 700;
}
</style>
