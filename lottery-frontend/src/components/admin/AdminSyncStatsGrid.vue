<script setup lang="ts">
import type { LotterySyncTaskStatistics } from '@/types/lottery'
import { formatDateTime } from '@/utils/lotterySyncFormat'

defineProps<{
  statistics: LotterySyncTaskStatistics | null
}>()
</script>

<template>
  <section class="stats-grid" aria-label="同步任务状态统计">
    <article class="metric-card metric-card--running">
      <span>运行中</span>
      <strong>{{ statistics?.runningCount ?? 0 }}</strong>
      <small>正在运行同步任务</small>
    </article>
    <article class="metric-card metric-card--pending">
      <span>待执行</span>
      <strong>{{ statistics?.pendingCount ?? 0 }}</strong>
      <small>等待后台线程池处理</small>
    </article>
    <article class="metric-card metric-card--failed">
      <span>失败任务</span>
      <strong>{{ statistics?.failedCount ?? 0 }}</strong>
      <small>可从日志中重试</small>
    </article>
    <article class="metric-card metric-card--success">
      <span>今日成功</span>
      <strong>{{ statistics?.successCountToday ?? 0 }}</strong>
      <small>最近成功：{{ formatDateTime(statistics?.latestSuccessTime) }}</small>
    </article>
  </section>
</template>
