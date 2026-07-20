<script setup lang="ts">
import type { LotterySyncTask } from '@/types/lottery'
import {
  formatBoolean,
  formatDateTime,
  formatLotteryType,
  formatValue,
  progressText,
  requestFields,
  statusLabel,
  syncTypeLabel,
  taskScopeText,
  triggerSourceLabel,
} from '@/utils/lotterySyncFormat'

defineProps<{
  open: boolean
  loading: boolean
  error: string
  selectedTask: LotterySyncTask | null
  hasActiveTask: boolean
  retryingTaskNo: string
}>()

const emit = defineEmits<{
  close: []
  refresh: [taskNo: string]
  retryTask: [taskNo: string]
}>()
</script>

<template>
  <div v-if="open" class="drawer-mask" @click="emit('close')"></div>
  <aside
    v-if="open"
    class="task-drawer"
    aria-labelledby="taskDetailTitle"
    role="dialog"
    aria-modal="true"
  >
    <header class="task-drawer__header">
      <div>
        <h2 id="taskDetailTitle">同步任务详情</h2>
        <p v-if="selectedTask" class="task-no">{{ selectedTask.taskNo }}</p>
      </div>
      <button class="drawer-close-button" type="button" aria-label="关闭详情" @click="emit('close')">
        ×
      </button>
    </header>

    <div v-if="loading" class="drawer-state">详情加载中...</div>
    <div v-else-if="error" class="drawer-state drawer-state--error">{{ error }}</div>

    <div v-else-if="selectedTask" class="task-drawer__body">
      <div class="drawer-status-row">
        <span class="status-pill" :class="`status-pill--${selectedTask.status}`">
          {{ statusLabel(selectedTask.status) }}
        </span>
        <span>{{ syncTypeLabel(selectedTask.syncType) }}</span>
      </div>

      <section class="detail-section" v-if="selectedTask.failureReason">
        <h3>失败原因</h3>
        <p class="failure-text">{{ selectedTask.failureReason }}</p>
      </section>

      <section class="detail-section">
        <h3>基本信息</h3>
        <dl class="detail-grid">
          <div>
            <dt>任务编号</dt>
            <dd>{{ selectedTask.taskNo }}</dd>
          </div>
          <div>
            <dt>票种</dt>
            <dd>{{ formatLotteryType(selectedTask.lotteryType) }}</dd>
          </div>
          <div>
            <dt>同步类型</dt>
            <dd>{{ syncTypeLabel(selectedTask.syncType) }}</dd>
          </div>
          <div>
            <dt>触发来源</dt>
            <dd>{{ triggerSourceLabel(selectedTask.triggerSource) }}</dd>
          </div>
        </dl>
      </section>

      <section class="detail-section">
        <h3>请求范围</h3>
        <dl class="detail-grid">
          <div>
            <dt>范围摘要</dt>
            <dd>{{ taskScopeText(selectedTask) }}</dd>
          </div>
          <div v-for="field in requestFields(selectedTask)" :key="field.label">
            <dt>{{ field.label }}</dt>
            <dd>{{ field.value }}</dd>
          </div>
        </dl>
        <pre class="request-json">{{ selectedTask.requestParams }}</pre>
      </section>

      <section class="detail-section">
        <h3>执行进度</h3>
        <dl class="detail-grid detail-grid--compact">
          <div>
            <dt>进度</dt>
            <dd>{{ progressText(selectedTask) }}</dd>
          </div>
          <div>
            <dt>起始页</dt>
            <dd>{{ formatValue(selectedTask.startPage) }}</dd>
          </div>
          <div>
            <dt>当前页</dt>
            <dd>{{ formatValue(selectedTask.currentPage) }}</dd>
          </div>
          <div>
            <dt>最后成功页</dt>
            <dd>{{ formatValue(selectedTask.lastSuccessPage) }}</dd>
          </div>
          <div>
            <dt>失败页</dt>
            <dd>{{ formatValue(selectedTask.failedPage) }}</dd>
          </div>
          <div>
            <dt>每页数量</dt>
            <dd>{{ formatValue(selectedTask.pageSize) }}</dd>
          </div>
          <div>
            <dt>最大扫描页数</dt>
            <dd>{{ formatValue(selectedTask.maxPages) }}</dd>
          </div>
          <div>
            <dt>页间隔毫秒</dt>
            <dd>{{ formatValue(selectedTask.pageDelayMillis) }}</dd>
          </div>
          <div>
            <dt>最后一页停止</dt>
            <dd>{{ formatBoolean(selectedTask.stopWhenLastPage) }}</dd>
          </div>
        </dl>
      </section>

      <section class="detail-section">
        <h3>处理统计</h3>
        <div class="detail-counts">
          <div>
            <span>成功</span>
            <strong>{{ selectedTask.successCount }}</strong>
          </div>
          <div>
            <span>跳过</span>
            <strong>{{ selectedTask.skippedCount }}</strong>
          </div>
          <div>
            <span>失败</span>
            <strong>{{ selectedTask.failedCount }}</strong>
          </div>
        </div>
      </section>

      <section class="detail-section">
        <h3>时间信息</h3>
        <dl class="detail-grid">
          <div>
            <dt>开始时间</dt>
            <dd>{{ formatDateTime(selectedTask.startTime) }}</dd>
          </div>
          <div>
            <dt>结束时间</dt>
            <dd>{{ formatDateTime(selectedTask.finishTime) }}</dd>
          </div>
        </dl>
      </section>

      <div class="drawer-actions">
        <button class="ghost-action-button" type="button" @click="emit('refresh', selectedTask.taskNo)">
          刷新详情
        </button>
        <button
          v-if="selectedTask.status === 'FAILED'"
          class="primary-button"
          type="button"
          :disabled="hasActiveTask || retryingTaskNo === selectedTask.taskNo"
          @click="emit('retryTask', selectedTask.taskNo)"
        >
          {{ retryingTaskNo === selectedTask.taskNo ? '重试中' : '重试任务' }}
        </button>
      </div>
    </div>
  </aside>
</template>
