import { computed, ref } from 'vue'

import { fetchSyncTasks, fetchSyncTaskStatistics } from '@/api/lottery'
import type { LotterySyncTaskPage, LotterySyncTaskStatus, LotterySyncTaskStatistics } from '@/types/lottery'
import { getErrorMessage } from '@/utils/lotteryFormat'

export function useAdminSyncDashboard() {
  const statistics = ref<LotterySyncTaskStatistics | null>(null)
  const taskPage = ref<LotterySyncTaskPage>({
    pageNo: 1,
    pageSize: 10,
    total: 0,
    pages: 0,
    status: null,
    tasks: [],
  })
  const loading = ref(false)
  const refreshingTasks = ref(false)
  const pageNo = ref(1)
  const pageSize = ref(10)
  const statusFilter = ref<'' | LotterySyncTaskStatus>('')
  const errorMessage = ref('')
  const noticeMessage = ref('')
  let noticeTimer: ReturnType<typeof setTimeout> | null = null

  const activeTask = computed(() =>
    taskPage.value.tasks.find((task) => task.status === 'RUNNING' || task.status === 'PENDING') ?? null,
  )
  const hasActiveTask = computed(
    () => Boolean(activeTask.value) || Boolean(statistics.value?.runningCount || statistics.value?.pendingCount),
  )
  const totalPages = computed(() => Math.max(taskPage.value.pages || 0, 1))

  /**
   * 同步加载统计和任务列表，保持页面上方概览与日志表一致。
   */
  async function loadDashboard() {
    loading.value = true
    errorMessage.value = ''

    try {
      await refreshData()
    } catch (err) {
      errorMessage.value = getErrorMessage(err, '同步管理数据加载失败，请稍后重试')
    } finally {
      loading.value = false
    }
  }

  async function refreshData() {
    await Promise.all([loadStatistics(), loadTasks()])
  }

  async function loadStatistics() {
    statistics.value = await fetchSyncTaskStatistics()
  }

  async function loadTasks() {
    refreshingTasks.value = true
    try {
      taskPage.value = await fetchSyncTasks({
        pageNo: pageNo.value,
        pageSize: pageSize.value,
        status: statusFilter.value || undefined,
      })
    } finally {
      refreshingTasks.value = false
    }
  }

  async function refreshAfterAction(message: string) {
    showNotice(message)
    await refreshData()
  }

  function showNotice(message: string) {
    noticeMessage.value = message
    if (noticeTimer) {
      clearTimeout(noticeTimer)
    }
    noticeTimer = setTimeout(() => {
      noticeMessage.value = ''
      noticeTimer = null
    }, 4000)
  }

  function setErrorMessage(message: string) {
    errorMessage.value = message
  }

  function clearNoticeMessage() {
    noticeMessage.value = ''
  }

  async function changeStatusFilter() {
    pageNo.value = 1
    await loadDashboard()
  }

  async function changePage(nextPageNo: number) {
    pageNo.value = Math.min(Math.max(nextPageNo, 1), totalPages.value)
    await loadDashboard()
  }

  async function changePageSize() {
    pageNo.value = 1
    await loadDashboard()
  }

  function cleanupNoticeTimer() {
    if (noticeTimer) {
      clearTimeout(noticeTimer)
      noticeTimer = null
    }
  }

  return {
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
  }
}
