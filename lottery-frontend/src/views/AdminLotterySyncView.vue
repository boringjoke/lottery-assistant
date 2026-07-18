<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'

import '@/assets/adminLotterySync.css'
import AdminActiveTaskBand from '@/components/admin/AdminActiveTaskBand.vue'
import AdminPermissionState from '@/components/admin/AdminPermissionState.vue'
import AdminSyncOperationPanel from '@/components/admin/AdminSyncOperationPanel.vue'
import AdminSyncStatsGrid from '@/components/admin/AdminSyncStatsGrid.vue'
import AdminSyncTaskDrawer from '@/components/admin/AdminSyncTaskDrawer.vue'
import AdminSyncTaskTable from '@/components/admin/AdminSyncTaskTable.vue'
import { useAdminSyncActions } from '@/composables/useAdminSyncActions'
import { useAdminSyncAuth } from '@/composables/useAdminSyncAuth'
import { useAdminSyncDashboard } from '@/composables/useAdminSyncDashboard'
import { useAdminSyncTaskDetail } from '@/composables/useAdminSyncTaskDetail'

const {
  statistics,
  taskPage,
  loading,
  refreshingTasks,
  pageNo,
  pageSize,
  statusFilter,
  errorMessage,
  noticeMessage,
  activeTask,
  hasActiveTask,
  totalPages,
  loadDashboard,
  refreshAfterAction,
  setErrorMessage,
  clearNoticeMessage,
  changeStatusFilter,
  changePage,
  changePageSize,
  cleanupNoticeTimer,
} = useAdminSyncDashboard()

const {
  submittingAction,
  retryingTaskNo,
  historyPageForm,
  historyForm,
  issueRangeForm,
  dateRangeForm,
  submitLatest,
  submitHistoryPage,
  submitHistory,
  submitIssueRange,
  submitDateRange,
  retryTask,
} = useAdminSyncActions({
  refreshAfterAction,
  setErrorMessage,
  clearNoticeMessage,
})

const {
  detailDrawerOpen,
  detailLoading,
  detailError,
  selectedTask,
  openTaskDetail,
  closeTaskDetail,
} = useAdminSyncTaskDetail()

const { authChecking, permissionDenied, initializeAdminPage } = useAdminSyncAuth(loadDashboard)

onMounted(initializeAdminPage)
onBeforeUnmount(cleanupNoticeTimer)
</script>

<template>
  <div class="admin-sync-page">
    <header class="admin-topbar">
      <RouterLink class="admin-brand" to="/lottery-assistant?tab=overview">
        <span class="brand-mark">≋</span>
        <span>彩票助手</span>
      </RouterLink>
      <div class="admin-badge">同步管理</div>
    </header>

    <main class="admin-main">
      <AdminPermissionState v-if="authChecking" />
      <AdminPermissionState v-else-if="permissionDenied" denied />

      <template v-else>
        <section class="page-title-row">
          <div>
            <h1>开奖数据同步管理</h1>
            <p>管理大乐透开奖同步任务、执行进度和失败重试。</p>
          </div>
          <button class="refresh-button" type="button" :disabled="loading" @click="loadDashboard">
            <svg
              class="refresh-button__icon"
              :class="{ spinning: loading }"
              viewBox="0 0 24 24"
              fill="none"
              aria-hidden="true"
            >
              <path
                d="M20 6v5h-5"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M19 11a7 7 0 0 0-12.1-4.8L5 8"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M4 18v-5h5"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <path
                d="M5 13a7 7 0 0 0 12.1 4.8L19 16"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <span>{{ loading ? '刷新中' : '刷新数据' }}</span>
          </button>
        </section>

        <div v-if="errorMessage" class="message message--error">{{ errorMessage }}</div>
        <div v-if="noticeMessage" class="message message--success">{{ noticeMessage }}</div>

        <AdminSyncStatsGrid :statistics="statistics" />
        <AdminActiveTaskBand :active-task="activeTask" />

        <AdminSyncOperationPanel
          :has-active-task="hasActiveTask"
          :submitting-action="submittingAction"
          :history-page-form="historyPageForm"
          :history-form="historyForm"
          :issue-range-form="issueRangeForm"
          :date-range-form="dateRangeForm"
          @submit-latest="submitLatest"
          @submit-history-page="submitHistoryPage"
          @submit-history="submitHistory"
          @submit-issue-range="submitIssueRange"
          @submit-date-range="submitDateRange"
        />

        <AdminSyncTaskTable
          v-model:page-size="pageSize"
          v-model:status-filter="statusFilter"
          :task-page="taskPage"
          :refreshing-tasks="refreshingTasks"
          :page-no="pageNo"
          :total-pages="totalPages"
          :has-active-task="hasActiveTask"
          :retrying-task-no="retryingTaskNo"
          @change-status-filter="changeStatusFilter"
          @change-page-size="changePageSize"
          @change-page="changePage"
          @open-detail="openTaskDetail"
          @retry-task="retryTask"
        />

        <AdminSyncTaskDrawer
          :open="detailDrawerOpen"
          :loading="detailLoading"
          :error="detailError"
          :selected-task="selectedTask"
          :has-active-task="hasActiveTask"
          :retrying-task-no="retryingTaskNo"
          @close="closeTaskDetail"
          @refresh="openTaskDetail"
          @retry-task="retryTask"
        />
      </template>
    </main>
  </div>
</template>
