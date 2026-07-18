import { reactive, ref } from 'vue'

import {
  retrySyncTask,
  startDateRangeSync,
  startHistorySync,
  startIssueRangeSync,
  syncHistoryPage,
  syncLatestDraw,
} from '@/api/lottery'
import type {
  LotteryDateRangeSyncRequest,
  LotteryHistorySyncRequest,
  LotteryIssueRangeSyncRequest,
} from '@/types/lottery'
import { getErrorMessage } from '@/utils/lotteryFormat'

interface AdminSyncActionOptions {
  refreshAfterAction: (message: string) => Promise<void>
  setErrorMessage: (message: string) => void
  clearNoticeMessage: () => void
}

export function useAdminSyncActions(options: AdminSyncActionOptions) {
  const submittingAction = ref('')
  const retryingTaskNo = ref('')

  const historyPageForm = reactive({
    pageNo: 1,
    pageSize: 20,
  })

  const historyForm = reactive<LotteryHistorySyncRequest>({
    startPage: 1,
    pageSize: 20,
    maxPages: 5,
    pageDelayMillis: 1000,
    stopWhenLastPage: true,
  })

  const issueRangeForm = reactive<LotteryIssueRangeSyncRequest>({
    startIssueNo: '',
    endIssueNo: '',
    startPage: 1,
    pageSize: 20,
    pageDelayMillis: 1000,
    stopWhenLastPage: true,
  })

  const dateRangeForm = reactive<LotteryDateRangeSyncRequest>({
    startDate: '',
    endDate: '',
    startPage: 1,
    pageSize: 20,
    pageDelayMillis: 1000,
    stopWhenLastPage: true,
  })

  async function runAction(actionKey: string, action: () => Promise<{ taskNo: string }>) {
    submittingAction.value = actionKey
    options.setErrorMessage('')
    options.clearNoticeMessage()

    try {
      const result = await action()
      await options.refreshAfterAction(`已创建同步任务 ${result.taskNo}`)
    } catch (err) {
      options.setErrorMessage(getErrorMessage(err, '同步任务创建失败，请检查参数后重试'))
    } finally {
      submittingAction.value = ''
    }
  }

  async function submitLatest() {
    await runAction('latest', syncLatestDraw)
  }

  async function submitHistoryPage() {
    await runAction('historyPage', () =>
      syncHistoryPage(Number(historyPageForm.pageNo), Number(historyPageForm.pageSize)),
    )
  }

  async function submitHistory() {
    await runAction('history', () =>
      startHistorySync({
        startPage: Number(historyForm.startPage),
        pageSize: Number(historyForm.pageSize),
        maxPages: Number(historyForm.maxPages),
        pageDelayMillis: Number(historyForm.pageDelayMillis),
        stopWhenLastPage: Boolean(historyForm.stopWhenLastPage),
      }),
    )
  }

  async function submitIssueRange() {
    await runAction('issueRange', () =>
      startIssueRangeSync({
        startIssueNo: issueRangeForm.startIssueNo.trim(),
        endIssueNo: issueRangeForm.endIssueNo.trim(),
        startPage: Number(issueRangeForm.startPage),
        pageSize: Number(issueRangeForm.pageSize),
        pageDelayMillis: Number(issueRangeForm.pageDelayMillis),
        stopWhenLastPage: Boolean(issueRangeForm.stopWhenLastPage),
      }),
    )
  }

  async function submitDateRange() {
    await runAction('dateRange', () =>
      startDateRangeSync({
        startDate: dateRangeForm.startDate,
        endDate: dateRangeForm.endDate,
        startPage: Number(dateRangeForm.startPage),
        pageSize: Number(dateRangeForm.pageSize),
        pageDelayMillis: Number(dateRangeForm.pageDelayMillis),
        stopWhenLastPage: Boolean(dateRangeForm.stopWhenLastPage),
      }),
    )
  }

  async function retryTask(taskNo: string) {
    retryingTaskNo.value = taskNo
    options.setErrorMessage('')
    options.clearNoticeMessage()

    try {
      const result = await retrySyncTask(taskNo)
      await options.refreshAfterAction(`已创建重试任务 ${result.taskNo}`)
    } catch (err) {
      options.setErrorMessage(getErrorMessage(err, '重试任务创建失败，请稍后再试'))
    } finally {
      retryingTaskNo.value = ''
    }
  }

  return {
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
  }
}
