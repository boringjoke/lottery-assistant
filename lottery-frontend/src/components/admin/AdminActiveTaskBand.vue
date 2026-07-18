<script setup lang="ts">
import type { LotterySyncTask } from '@/types/lottery'
import { progressText, statusLabel, syncTypeLabel } from '@/utils/lotterySyncFormat'

defineProps<{
  activeTask: LotterySyncTask | null
}>()
</script>

<template>
  <section class="active-task-band">
    <div>
      <span class="section-kicker">当前活跃任务</span>
      <h2>{{ activeTask ? activeTask.taskNo : '暂无活跃任务' }}</h2>
      <p v-if="activeTask">
        {{ syncTypeLabel(activeTask.syncType) }} · {{ progressText(activeTask) }} ·
        成功 {{ activeTask.successCount }} / 跳过 {{ activeTask.skippedCount }}
      </p>
      <p v-else>当前可以创建新的同步任务；同一时间仅允许一个待执行或运行中的同步任务。</p>
    </div>
    <span class="status-pill" :class="activeTask ? 'status-pill--RUNNING' : 'status-pill--SUCCESS'">
      {{ activeTask ? statusLabel(activeTask.status) : '空闲' }}
    </span>
  </section>
</template>
