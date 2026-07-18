<script setup lang="ts">
import type { LotterySyncTaskPage, LotterySyncTaskStatus } from '@/types/lottery'
import {
  formatLotteryType,
  statusLabel,
  statusOptions,
  syncTypeLabel,
  taskScopeText,
  taskTime,
} from '@/utils/lotterySyncFormat'

defineProps<{
  taskPage: LotterySyncTaskPage
  refreshingTasks: boolean
  pageNo: number
  pageSize: number
  statusFilter: '' | LotterySyncTaskStatus
  totalPages: number
  hasActiveTask: boolean
  retryingTaskNo: string
}>()

const emit = defineEmits<{
  'update:pageSize': [value: number]
  'update:statusFilter': [value: '' | LotterySyncTaskStatus]
  changeStatusFilter: []
  changePageSize: []
  changePage: [pageNo: number]
  openDetail: [taskNo: string]
  retryTask: [taskNo: string]
}>()

function updateStatusFilter(event: Event) {
  emit('update:statusFilter', (event.target as HTMLSelectElement).value as '' | LotterySyncTaskStatus)
  emit('changeStatusFilter')
}

function updatePageSize(event: Event) {
  emit('update:pageSize', Number((event.target as HTMLSelectElement).value))
  emit('changePageSize')
}
</script>

<template>
  <section class="task-panel">
    <div class="task-toolbar">
      <div class="section-heading">
        <span></span>
        <div>
          <h2>任务日志</h2>
          <p>查看同步任务状态、范围和失败重试入口。</p>
        </div>
      </div>
      <div class="toolbar-controls">
        <select :value="statusFilter" @change="updateStatusFilter">
          <option v-for="option in statusOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
        <select :value="pageSize" @change="updatePageSize">
          <option :value="10">10 条/页</option>
          <option :value="20">20 条/页</option>
          <option :value="50">50 条/页</option>
        </select>
      </div>
    </div>

    <div v-if="refreshingTasks" class="inline-loading">任务列表刷新中...</div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th class="task-no-column">任务编号</th>
            <th class="lottery-column">票种</th>
            <th class="type-column">同步类型</th>
            <th class="status-column">状态</th>
            <th class="scope-column">范围</th>
            <th class="time-column">时间</th>
            <th class="action-column">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="task in taskPage.tasks" :key="task.taskNo">
            <td class="task-no">{{ task.taskNo }}</td>
            <td>{{ formatLotteryType(task.lotteryType) }}</td>
            <td>{{ syncTypeLabel(task.syncType) }}</td>
            <td>
              <span class="status-pill" :class="`status-pill--${task.status}`">
                {{ statusLabel(task.status) }}
              </span>
            </td>
            <td>{{ taskScopeText(task) }}</td>
            <td class="time-cell">{{ taskTime(task) }}</td>
            <td>
              <div class="row-actions">
                <button
                  class="small-button small-button--plain"
                  type="button"
                  :data-testid="`detail-${task.taskNo}`"
                  @click="emit('openDetail', task.taskNo)"
                >
                  详情
                </button>
                <button
                  v-if="task.status === 'FAILED'"
                  class="small-button"
                  type="button"
                  :disabled="hasActiveTask || retryingTaskNo === task.taskNo"
                  :data-testid="`retry-${task.taskNo}`"
                  @click="emit('retryTask', task.taskNo)"
                >
                  {{ retryingTaskNo === task.taskNo ? '重试中' : '重试' }}
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!taskPage.tasks.length">
            <td class="empty-cell" colspan="7">暂无同步任务</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination-row">
      <span>共 {{ taskPage.total }} 条，第 {{ pageNo }} / {{ totalPages }} 页</span>
      <div class="pagination-actions">
        <button type="button" :disabled="pageNo <= 1" @click="emit('changePage', pageNo - 1)">
          上一页
        </button>
        <button type="button" :disabled="pageNo >= totalPages" @click="emit('changePage', pageNo + 1)">
          下一页
        </button>
      </div>
    </div>
  </section>
</template>
