<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { fetchCurrentUser } from '@/api/auth'
import {
  fetchSyncTask,
  fetchSyncTasks,
  fetchSyncTaskStatistics,
  retrySyncTask,
  startDateRangeSync,
  startHistorySync,
  startIssueRangeSync,
  syncHistoryPage,
  syncLatestDraw,
} from '@/api/lottery'
import type { CurrentUser } from '@/types/auth'
import type {
  LotteryDateRangeSyncRequest,
  LotteryHistorySyncRequest,
  LotteryIssueRangeSyncRequest,
  LotterySyncTask,
  LotterySyncTaskPage,
  LotterySyncTaskStatus,
  LotterySyncTaskStatistics,
  LotterySyncType,
} from '@/types/lottery'
import { getErrorMessage } from '@/utils/lotteryFormat'

const route = useRoute()
const router = useRouter()

const statusOptions: Array<{ value: '' | LotterySyncTaskStatus; label: string }> = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待执行' },
  { value: 'RUNNING', label: '进行中' },
  { value: 'SUCCESS', label: '成功' },
  { value: 'PARTIAL_SUCCESS', label: '部分成功' },
  { value: 'FAILED', label: '失败' },
  { value: 'RETRIED', label: '已重试' },
]

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
const submittingAction = ref('')
const retryingTaskNo = ref('')
const ticketDropdownOpen = ref(false)
const operationsCollapsed = ref(false)
const pageNo = ref(1)
const pageSize = ref(10)
const statusFilter = ref<'' | LotterySyncTaskStatus>('')
const errorMessage = ref('')
const noticeMessage = ref('')
let noticeTimer: ReturnType<typeof setTimeout> | null = null
const detailDrawerOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const selectedTask = ref<LotterySyncTask | null>(null)
const authChecking = ref(true)
const permissionDenied = ref(false)
const currentUser = ref<CurrentUser | null>(null)

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
    await Promise.all([loadStatistics(), loadTasks()])
  } catch (err) {
    errorMessage.value = getErrorMessage(err, '同步管理数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
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
  await Promise.all([loadStatistics(), loadTasks()])
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

async function runAction(actionKey: string, action: () => Promise<{ taskNo: string }>) {
  submittingAction.value = actionKey
  errorMessage.value = ''
  noticeMessage.value = ''

  try {
    const result = await action()
    await refreshAfterAction(`已创建同步任务 ${result.taskNo}`)
  } catch (err) {
    errorMessage.value = getErrorMessage(err, '同步任务创建失败，请检查参数后重试')
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
  errorMessage.value = ''
  noticeMessage.value = ''

  try {
    const result = await retrySyncTask(taskNo)
    await refreshAfterAction(`已创建重试任务 ${result.taskNo}`)
  } catch (err) {
    errorMessage.value = getErrorMessage(err, '重试任务创建失败，请稍后再试')
  } finally {
    retryingTaskNo.value = ''
  }
}

async function openTaskDetail(taskNo: string) {
  detailDrawerOpen.value = true
  detailLoading.value = true
  detailError.value = ''
  selectedTask.value = null

  try {
    selectedTask.value = await fetchSyncTask(taskNo)
  } catch (err) {
    detailError.value = getErrorMessage(err, '同步任务详情加载失败，请稍后重试')
  } finally {
    detailLoading.value = false
  }
}

function closeTaskDetail() {
  detailDrawerOpen.value = false
  detailLoading.value = false
  detailError.value = ''
  selectedTask.value = null
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

function statusLabel(status: LotterySyncTaskStatus | null | undefined): string {
  return statusOptions.find((item) => item.value === status)?.label ?? status ?? '-'
}

function syncTypeLabel(syncType: LotterySyncType | null | undefined): string {
  const labels: Record<LotterySyncType, string> = {
    LATEST: '同步最新开奖',
    HISTORY_PAGE: '同步历史分页',
    HISTORY: '历史分页批量同步',
    ISSUE_RANGE: '按期号范围同步',
    DATE_RANGE: '按日期范围同步',
  }

  return syncType ? labels[syncType] : '-'
}

function formatLotteryType(lotteryType: string | null | undefined): string {
  return lotteryType === 'DLT' ? '大乐透' : lotteryType || '-'
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '-'
  }

  return value.replace('T', ' ').slice(0, 19)
}

function taskTime(task: LotterySyncTask): string {
  return formatDateTime(task.finishTime ?? task.startTime)
}

function progressText(task: LotterySyncTask): string {
  const current = task.currentPage ?? task.startPage
  const max = task.maxPages
  if (!current && !max) {
    return '-'
  }

  return max ? `第 ${current ?? '-'} / ${max} 页` : `第 ${current ?? '-'} 页`
}

function taskScopeText(task: LotterySyncTask): string {
  const params = task.requestParamMap ?? {}
  if (task.syncType === 'ISSUE_RANGE') {
    return `${params.startIssueNo ?? '-'} 至 ${params.endIssueNo ?? '-'}`
  }
  if (task.syncType === 'DATE_RANGE') {
    return `${params.startDate ?? '-'} 至 ${params.endDate ?? '-'}`
  }
  if (task.syncType === 'HISTORY_PAGE') {
    return `第 ${params.pageNo ?? task.startPage ?? '-'} 页`
  }
  if (task.syncType === 'HISTORY') {
    return `从第 ${params.startPage ?? task.startPage ?? '-'} 页开始`
  }
  return params.source === 'crawler.latest' ? '最新一期' : '-'
}

function formatBoolean(value: boolean | null | undefined): string {
  if (value === null || value === undefined) {
    return '-'
  }

  return value ? '是' : '否'
}

function formatValue(value: string | number | boolean | null | undefined): string {
  if (value === null || value === undefined || value === '') {
    return '-'
  }

  return String(value)
}

function requestFields(task: LotterySyncTask): Array<{ label: string; value: string }> {
  const params = task.requestParamMap ?? {}
  const fieldMap: Record<LotterySyncType, Array<[string, string]>> = {
    LATEST: [['数据来源', 'source']],
    HISTORY_PAGE: [
      ['页码', 'pageNo'],
      ['每页数量', 'pageSize'],
    ],
    HISTORY: [
      ['起始页', 'startPage'],
      ['每页数量', 'pageSize'],
      ['最大扫描页数', 'maxPages'],
      ['页间隔毫秒', 'pageDelayMillis'],
      ['最后一页停止', 'stopWhenLastPage'],
    ],
    ISSUE_RANGE: [
      ['起始期号', 'startIssueNo'],
      ['结束期号', 'endIssueNo'],
      ['起始页', 'startPage'],
      ['每页数量', 'pageSize'],
      ['最大扫描页数', 'maxPages'],
      ['页间隔毫秒', 'pageDelayMillis'],
      ['最后一页停止', 'stopWhenLastPage'],
    ],
    DATE_RANGE: [
      ['开始日期', 'startDate'],
      ['结束日期', 'endDate'],
      ['起始页', 'startPage'],
      ['每页数量', 'pageSize'],
      ['最大扫描页数', 'maxPages'],
      ['页间隔毫秒', 'pageDelayMillis'],
      ['最后一页停止', 'stopWhenLastPage'],
    ],
  }

  return fieldMap[task.syncType]
    .filter(([, key]) => params[key] !== undefined)
    .map(([label, key]) => ({
      label,
      value: key === 'stopWhenLastPage' ? (params[key] === 'true' ? '是' : '否') : String(params[key] ?? '-'),
    }))
}

/**
 * 管理页进入时先恢复登录态；只有 ADMIN 角色才加载同步管理数据。
 */
async function initializeAdminPage() {
  authChecking.value = true
  permissionDenied.value = false

  try {
    currentUser.value = await fetchCurrentUser()
    if (!currentUser.value.roles.includes('ADMIN')) {
      permissionDenied.value = true
      return
    }

    await loadDashboard()
  } catch {
    await router.push({
      path: '/login',
      query: { redirect: route.fullPath },
    })
  } finally {
    authChecking.value = false
  }
}

onMounted(initializeAdminPage)

onBeforeUnmount(() => {
  if (noticeTimer) {
    clearTimeout(noticeTimer)
    noticeTimer = null
  }
})
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
      <section v-if="authChecking" class="permission-state" aria-live="polite">
        <div class="permission-state__mark">≋</div>
        <h1>正在验证管理权限</h1>
        <p>请稍候，系统正在确认当前登录状态。</p>
      </section>

      <section v-else-if="permissionDenied" class="permission-state permission-state--denied">
        <div class="permission-state__mark">!</div>
        <h1>您无权操作页面</h1>
        <p>当前账号没有同步管理权限，请返回公共彩票助手页面继续使用开奖查询和号码分析功能。</p>
        <RouterLink class="permission-state__button" to="/lottery-assistant?tab=overview">
          返回彩票助手
        </RouterLink>
      </section>

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

      <section class="operation-panel">
        <div class="operation-panel__header">
          <div class="section-heading">
            <span></span>
            <div>
              <h2>同步操作</h2>
              <p>当前票种固定为大乐透；范围同步请保持低频、串行执行。</p>
            </div>
          </div>
          <button
            class="collapse-button"
            type="button"
            :aria-expanded="!operationsCollapsed"
            @click="operationsCollapsed = !operationsCollapsed"
          >
            <span>{{ operationsCollapsed ? '展开操作' : '收起操作' }}</span>
            <span
              class="collapse-button__arrow"
              :class="{ collapsed: operationsCollapsed }"
              aria-hidden="true"
            ></span>
          </button>
        </div>

        <div v-if="!operationsCollapsed" class="operation-panel__body">
          <div class="ticket-line">
            <label>票种</label>
            <div class="ticket-selector">
              <button
                type="button"
                class="ticket-button"
                @click="ticketDropdownOpen = !ticketDropdownOpen"
              >
                <span>大乐透</span>
                <span
                  class="ticket-button__arrow"
                  :class="{ rotated: ticketDropdownOpen }"
                  aria-hidden="true"
                ></span>
              </button>
              <div
                v-if="ticketDropdownOpen"
                class="dropdown-mask"
                @click="ticketDropdownOpen = false"
              ></div>
              <div v-if="ticketDropdownOpen" class="ticket-menu">
                <button type="button" class="ticket-menu__active" @click="ticketDropdownOpen = false">
                  大乐透 <span>✓</span>
                </button>
                <button type="button" disabled>双色球 <span>敬请期待</span></button>
                <button type="button" disabled>福彩3D <span>敬请期待</span></button>
              </div>
            </div>
            <span>首版仅开放大乐透，接口保留多票种扩展空间。</span>
          </div>

          <div class="operation-grid">
          <form class="operation-card operation-card--latest" @submit.prevent="submitLatest">
            <div>
              <h3>同步最新开奖</h3>
              <p>抓取并同步当前票种最新一期开奖数据。</p>
            </div>
            <button class="primary-button" type="submit" :disabled="hasActiveTask || submittingAction === 'latest'">
              {{ submittingAction === 'latest' ? '创建中' : '同步最新开奖' }}
            </button>
          </form>

          <form class="operation-card" @submit.prevent="submitHistoryPage">
            <div>
              <h3>同步历史分页</h3>
              <p>用于快速补一页历史开奖数据，任务会立即执行。</p>
            </div>
            <div class="field-row">
              <label for="historyPageNo">页码</label>
              <input id="historyPageNo" v-model.number="historyPageForm.pageNo" min="1" type="number" />
            </div>
            <div class="field-row">
              <label for="historyPageSize">每页数量</label>
              <input id="historyPageSize" v-model.number="historyPageForm.pageSize" min="1" max="50" type="number" />
            </div>
            <button
              class="primary-button"
              type="submit"
              :disabled="hasActiveTask || submittingAction === 'historyPage'"
            >
              {{ submittingAction === 'historyPage' ? '创建中' : '同步这一页' }}
            </button>
          </form>

          <form
            data-testid="history-sync-form"
            class="operation-card"
            @submit.prevent="submitHistory"
          >
            <div>
              <h3>历史分页批量同步</h3>
              <p>从指定页开始扫描多页，适合批量补录历史开奖。</p>
            </div>
            <div class="field-row">
              <label for="historyStartPage">起始页</label>
              <input id="historyStartPage" v-model.number="historyForm.startPage" min="1" type="number" />
            </div>
            <div class="field-row">
              <label for="historyPageSize">每页数量</label>
              <input id="historyPageSize" v-model.number="historyForm.pageSize" min="1" max="50" type="number" />
            </div>
            <div class="field-row">
              <label for="historyMaxPages">最大扫描页数</label>
              <input id="historyMaxPages" v-model.number="historyForm.maxPages" min="1" type="number" />
            </div>
            <div class="field-row">
              <label for="historyPageDelay">页间隔毫秒</label>
              <input id="historyPageDelay" v-model.number="historyForm.pageDelayMillis" min="0" type="number" />
            </div>
            <label class="check-row">
              <input v-model="historyForm.stopWhenLastPage" type="checkbox" />
              到达最后一页时停止
            </label>
            <button class="primary-button" type="submit" :disabled="hasActiveTask || submittingAction === 'history'">
              {{ submittingAction === 'history' ? '创建中' : '创建批量任务' }}
            </button>
          </form>

          <form
            data-testid="issue-range-form"
            class="operation-card"
            @submit.prevent="submitIssueRange"
          >
            <div>
              <h3>按期号范围同步</h3>
              <p>输入起止期号；单期同步时两个期号填写一致。</p>
            </div>
            <div class="field-row">
              <label for="issueStartIssueNo">起始期号</label>
              <input id="issueStartIssueNo" v-model="issueRangeForm.startIssueNo" placeholder="26070" type="text" />
            </div>
            <div class="field-row">
              <label for="issueEndIssueNo">结束期号</label>
              <input id="issueEndIssueNo" v-model="issueRangeForm.endIssueNo" placeholder="26076" type="text" />
            </div>
            <div class="field-row">
              <label for="issueStartPage">起始页</label>
              <input id="issueStartPage" v-model.number="issueRangeForm.startPage" min="1" type="number" />
            </div>
            <div class="field-row">
              <label for="issuePageSize">每页数量</label>
              <input id="issuePageSize" v-model.number="issueRangeForm.pageSize" min="1" max="50" type="number" />
            </div>
            <div class="field-row">
              <label for="issuePageDelay">页间隔毫秒</label>
              <input id="issuePageDelay" v-model.number="issueRangeForm.pageDelayMillis" min="0" type="number" />
            </div>
            <label class="check-row">
              <input v-model="issueRangeForm.stopWhenLastPage" type="checkbox" />
              到达最后一页时停止
            </label>
            <button
              class="primary-button"
              type="submit"
              :disabled="hasActiveTask || submittingAction === 'issueRange'"
            >
              {{ submittingAction === 'issueRange' ? '创建中' : '按期号同步' }}
            </button>
          </form>

          <form
            data-testid="date-range-form"
            class="operation-card"
            @submit.prevent="submitDateRange"
          >
            <div>
              <h3>按日期范围同步</h3>
              <p>按开奖日期筛选历史分页结果，扫描上限由后端配置控制。</p>
            </div>
            <div class="field-row">
              <label for="dateStartDate">开始日期</label>
              <input id="dateStartDate" v-model="dateRangeForm.startDate" type="date" />
            </div>
            <div class="field-row">
              <label for="dateEndDate">结束日期</label>
              <input id="dateEndDate" v-model="dateRangeForm.endDate" type="date" />
            </div>
            <div class="field-row">
              <label for="dateStartPage">起始页</label>
              <input id="dateStartPage" v-model.number="dateRangeForm.startPage" min="1" type="number" />
            </div>
            <div class="field-row">
              <label for="datePageSize">每页数量</label>
              <input id="datePageSize" v-model.number="dateRangeForm.pageSize" min="1" max="50" type="number" />
            </div>
            <div class="field-row">
              <label for="datePageDelay">页间隔毫秒</label>
              <input id="datePageDelay" v-model.number="dateRangeForm.pageDelayMillis" min="0" type="number" />
            </div>
            <label class="check-row">
              <input v-model="dateRangeForm.stopWhenLastPage" type="checkbox" />
              到达最后一页时停止
            </label>
            <button
              class="primary-button"
              type="submit"
              :disabled="hasActiveTask || submittingAction === 'dateRange'"
            >
              {{ submittingAction === 'dateRange' ? '创建中' : '按日期同步' }}
            </button>
          </form>
          </div>
        </div>
      </section>

      <section class="task-panel">
        <div class="task-toolbar">
          <div class="section-heading">
            <span></span>
            <div>
              <h2>同步任务日志</h2>
              <p>展示任务状态、范围和时间；进度、统计与失败原因可在详情中查看。</p>
            </div>
          </div>
          <div class="toolbar-controls">
            <select v-model="statusFilter" @change="changeStatusFilter">
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <select v-model.number="pageSize" @change="changePageSize">
              <option :value="10">10 条/页</option>
              <option :value="20">20 条/页</option>
              <option :value="50">50 条/页</option>
            </select>
          </div>
        </div>

        <div v-if="refreshingTasks && taskPage.tasks.length" class="inline-loading">正在刷新同步任务</div>

        <div class="table-wrap">
          <table>
            <colgroup>
              <col class="task-no-column" />
              <col class="lottery-column" />
              <col class="type-column" />
              <col class="status-column" />
              <col class="scope-column" />
              <col class="time-column" />
              <col class="action-column" />
            </colgroup>
            <thead>
              <tr>
                <th>任务编号</th>
                <th>票种</th>
                <th>同步类型</th>
                <th>状态</th>
                <th>范围</th>
                <th>时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!taskPage.tasks.length">
                <td class="empty-cell" colspan="7">暂无同步任务记录</td>
              </tr>
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
                      @click="openTaskDetail(task.taskNo)"
                    >
                      详情
                    </button>
                    <button
                      v-if="task.status === 'FAILED'"
                      class="small-button"
                      type="button"
                      :data-testid="`retry-${task.taskNo}`"
                      :disabled="hasActiveTask || retryingTaskNo === task.taskNo"
                      @click="retryTask(task.taskNo)"
                    >
                      {{ retryingTaskNo === task.taskNo ? '重试中' : '重试' }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pagination-row">
          <span>共 {{ taskPage.total }} 条</span>
          <div class="pagination-actions">
            <button type="button" :disabled="pageNo <= 1" @click="changePage(pageNo - 1)">上一页</button>
            <span>第 {{ pageNo }} / {{ totalPages }} 页</span>
            <button type="button" :disabled="pageNo >= totalPages" @click="changePage(pageNo + 1)">下一页</button>
          </div>
        </div>
      </section>

      <div v-if="detailDrawerOpen" class="drawer-mask" @click="closeTaskDetail"></div>
      <aside
        v-if="detailDrawerOpen"
        class="task-drawer"
        role="dialog"
        aria-modal="true"
        aria-labelledby="taskDetailTitle"
      >
        <header class="task-drawer__header">
          <div>
            <h2 id="taskDetailTitle">同步任务详情</h2>
          </div>
          <button class="drawer-close-button" type="button" aria-label="关闭详情" @click="closeTaskDetail">
            ×
          </button>
        </header>

        <div v-if="detailLoading" class="drawer-state">正在加载任务详情</div>
        <div v-else-if="detailError" class="drawer-state drawer-state--error">
          {{ detailError }}
        </div>
        <div v-else-if="selectedTask" class="task-drawer__body">
          <div class="drawer-status-row">
            <span class="status-pill" :class="`status-pill--${selectedTask.status}`">
              {{ statusLabel(selectedTask.status) }}
            </span>
            <span>{{ syncTypeLabel(selectedTask.syncType) }}</span>
          </div>

          <section class="detail-section">
            <h3>失败原因</h3>
            <p class="failure-text">{{ selectedTask.failureReason || '无' }}</p>
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
                <dd>{{ selectedTask.triggerSource }}</dd>
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
            <button class="ghost-action-button" type="button" @click="openTaskDetail(selectedTask.taskNo)">
              刷新详情
            </button>
            <button
              v-if="selectedTask.status === 'FAILED'"
              class="primary-button"
              type="button"
              :disabled="hasActiveTask || retryingTaskNo === selectedTask.taskNo"
              @click="retryTask(selectedTask.taskNo)"
            >
              {{ retryingTaskNo === selectedTask.taskNo ? '重试中' : '重试任务' }}
            </button>
          </div>
        </div>
      </aside>
      </template>
    </main>
  </div>
</template>

<style scoped>
.admin-sync-page {
  min-height: 100vh;
  background: #f8fafc;
  color: #0f172a;
}

.admin-topbar {
  display: flex;
  height: 64px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0;
  background: #ffffff;
  padding: 0 24px;
}

.admin-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #1e3a8a;
  font-size: 20px;
  font-weight: 900;
  text-decoration: none;
}

.brand-mark {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: #ffffff;
}

.admin-badge {
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 800;
}

.permission-state {
  display: flex;
  min-height: calc(100vh - 160px);
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.permission-state__mark {
  display: inline-flex;
  width: 44px;
  height: 44px;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  border-radius: 12px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 24px;
  font-weight: 900;
}

.permission-state--denied .permission-state__mark {
  background: #fef2f2;
  color: #b91c1c;
}

.permission-state h1 {
  margin-bottom: 10px;
  color: #0f172a;
  font-size: 24px;
  font-weight: 900;
}

.permission-state p {
  max-width: 430px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.permission-state__button {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  margin-top: 22px;
  border-radius: 9px;
  background: #2563eb;
  color: #ffffff;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 900;
  text-decoration: none;
  box-shadow: 0 10px 24px rgb(37 99 235 / 0.2);
}

.permission-state__button:hover {
  background: #1d4ed8;
}

.admin-main {
  max-width: 1280px;
  margin: 0 auto;
  padding: 28px 24px 48px;
}

.page-title-row,
.task-toolbar,
.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-title-row {
  margin-bottom: 22px;
}

h1,
h2,
h3,
p {
  margin: 0;
}

.page-title-row h1 {
  font-size: 26px;
  font-weight: 900;
}

.page-title-row p,
.section-heading p,
.operation-card p,
.active-task-band p,
.ticket-line span,
.metric-card small {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.primary-button,
.small-button,
.collapse-button,
.pagination-actions button,
.ticket-button,
.ticket-menu button {
  border: 0;
  border-radius: 8px;
  font-weight: 800;
  cursor: pointer;
}

.primary-button {
  width: 100%;
  min-height: 42px;
  background: #2563eb;
  color: #ffffff;
  padding: 0 14px;
  box-shadow: 0 8px 20px rgb(37 99 235 / 0.16);
}

.primary-button:disabled,
.small-button:disabled,
.refresh-button:disabled,
.pagination-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.refresh-button {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  gap: 9px;
  border: 1px solid #2563eb;
  border-radius: 9px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: #ffffff;
  padding: 0 15px 0 12px;
  font-size: 14px;
  font-weight: 900;
  box-shadow: 0 10px 24px rgb(37 99 235 / 0.22);
  cursor: pointer;
}

.refresh-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #1d4ed8 0%, #1e40af 100%);
  box-shadow: 0 12px 28px rgb(37 99 235 / 0.28);
}

.refresh-button:focus-visible {
  outline: 3px solid #bfdbfe;
  outline-offset: 2px;
}

.refresh-button__icon {
  width: 18px;
  height: 18px;
  color: #ffffff;
}

.refresh-button__icon.spinning {
  animation: refresh-spin 0.8s linear infinite;
}

@keyframes refresh-spin {
  to {
    transform: rotate(360deg);
  }
}

.small-button {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 7px 10px;
  white-space: nowrap;
}

.small-button--plain {
  border-color: #e2e8f0;
  background: #ffffff;
  color: #334155;
}

.small-button--plain:hover {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.message {
  margin-bottom: 16px;
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 14px;
  font-weight: 700;
}

.message--error {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.message--success {
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.metric-card,
.active-task-band,
.operation-panel,
.task-panel {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 10px 30px rgb(15 23 42 / 0.04);
}

.metric-card {
  padding: 16px;
}

.metric-card span {
  display: block;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.metric-card strong {
  display: block;
  margin: 8px 0 4px;
  color: #0f172a;
  font-size: 30px;
  font-weight: 900;
}

.metric-card--running {
  border-color: #bfdbfe;
  background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
}

.metric-card--running span,
.metric-card--running strong {
  color: #1d4ed8;
}

.metric-card--pending {
  border-color: #fed7aa;
  background: linear-gradient(180deg, #fff7ed 0%, #ffffff 100%);
}

.metric-card--pending span,
.metric-card--pending strong {
  color: #c2410c;
}

.metric-card--failed {
  border-color: #fecaca;
  background: linear-gradient(180deg, #fef2f2 0%, #ffffff 100%);
}

.metric-card--failed span,
.metric-card--failed strong {
  color: #b91c1c;
}

.metric-card--success {
  border-color: #bbf7d0;
  background: linear-gradient(180deg, #f0fdf4 0%, #ffffff 100%);
}

.metric-card--success span,
.metric-card--success strong {
  color: #15803d;
}

.active-task-band {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  padding: 18px;
}

.section-kicker {
  display: block;
  margin-bottom: 6px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 900;
}

.active-task-band h2 {
  font-size: 18px;
  font-weight: 900;
}

.operation-panel,
.task-panel {
  padding: 20px;
}

.operation-panel {
  margin-bottom: 20px;
}

.operation-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.operation-panel__body {
  margin-top: 18px;
  border-top: 1px solid #f1f5f9;
  padding-top: 16px;
}

.collapse-button {
  display: inline-flex;
  min-height: 36px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  padding: 0 12px;
  font-size: 13px;
}

.collapse-button:hover {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
}

.collapse-button:focus-visible {
  outline: 3px solid #bfdbfe;
  outline-offset: 2px;
}

.collapse-button__arrow {
  width: 8px;
  height: 8px;
  border-right: 2px solid currentColor;
  border-bottom: 2px solid currentColor;
  transform: translateY(-2px) rotate(45deg);
  transition: transform 0.2s;
}

.collapse-button__arrow.collapsed {
  transform: translateY(2px) rotate(-135deg);
}

.section-heading {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.section-heading > span {
  flex: 0 0 auto;
  width: 6px;
  height: 20px;
  margin-top: 3px;
  border-radius: 999px;
  background: #2563eb;
}

.section-heading h2 {
  font-size: 18px;
  font-weight: 900;
}

.ticket-line {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 18px;
}

.ticket-line label {
  color: #334155;
  font-size: 14px;
  font-weight: 900;
}

.ticket-selector {
  position: relative;
  z-index: 20;
}

.ticket-button {
  display: inline-flex;
  min-width: 148px;
  height: 38px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #0f172a;
  padding: 0 12px;
  text-align: left;
}

.ticket-button:hover {
  background: #f8fafc;
}

.ticket-button__arrow {
  display: inline-flex;
  position: relative;
  flex: 0 0 20px;
  width: 20px;
  height: 20px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: #f1f5f9;
  transition: transform 0.2s;
}

.ticket-button__arrow::before {
  content: "";
  width: 6px;
  height: 6px;
  border-right: 2px solid #94a3b8;
  border-bottom: 2px solid #94a3b8;
  transform: translateY(-1px) rotate(45deg);
}

.ticket-button__arrow.rotated {
  transform: rotate(180deg);
}

.dropdown-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
}

.ticket-menu {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 40;
  width: 168px;
  overflow: hidden;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 16px 40px rgb(15 23 42 / 0.12);
}

.ticket-menu button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  border-radius: 0;
  background: #ffffff;
  color: #64748b;
  padding: 10px 12px;
  font-size: 14px;
  text-align: left;
}

.ticket-menu button:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.ticket-menu button span {
  font-size: 11px;
}

.ticket-menu__active {
  background: #eff6ff !important;
  color: #2563eb !important;
}

.operation-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.operation-card {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  gap: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
  padding: 16px;
}

.operation-card--latest {
  background: #f1f5f9;
}

.operation-card h3 {
  margin-bottom: 6px;
  font-size: 16px;
  font-weight: 900;
}

.field-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}

.field-row label,
.check-row {
  color: #334155;
  font-size: 13px;
  font-weight: 800;
}

.field-row input,
.toolbar-controls select {
  min-width: 0;
  height: 38px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #ffffff;
  color: #0f172a;
  padding: 0 10px;
  font-size: 13px;
}

.field-row input:focus,
.toolbar-controls select:focus {
  border-color: #2563eb;
  outline: 2px solid #bfdbfe;
}

.check-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.operation-card .primary-button {
  margin-top: auto;
}

.toolbar-controls {
  display: flex;
  gap: 10px;
}

.toolbar-controls select {
  min-width: 120px;
}

.inline-loading {
  margin: 16px 0 -4px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
}

.table-wrap {
  margin-top: 18px;
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 1040px;
  border-collapse: collapse;
  text-align: left;
}

.task-no-column {
  width: 300px;
}

.lottery-column {
  width: 92px;
}

.type-column {
  width: 150px;
}

.status-column {
  width: 104px;
}

.scope-column {
  width: auto;
}

.time-column {
  width: 176px;
}

.action-column {
  width: 130px;
}

th {
  border-top: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #64748b;
  padding: 11px 12px;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

td {
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
  padding: 13px 12px;
  font-size: 13px;
  vertical-align: middle;
}

tbody tr:hover {
  background: #eff6ff66;
}

.task-no,
.count-cell,
.time-cell {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.task-no {
  color: #0f172a;
  font-weight: 800;
  white-space: nowrap;
}

.time-cell {
  color: #64748b;
  white-space: nowrap;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.empty-cell {
  height: 120px;
  color: #94a3b8;
  text-align: center;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
  background: #f8fafc;
  color: #475569;
  padding: 0 9px;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.status-pill--PENDING {
  border-color: #fed7aa;
  background: #fff7ed;
  color: #c2410c;
}

.status-pill--RUNNING {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.status-pill--SUCCESS {
  border-color: #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.status-pill--PARTIAL_SUCCESS {
  border-color: #fde68a;
  background: #fffbeb;
  color: #b45309;
}

.status-pill--FAILED {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.status-pill--RETRIED {
  border-color: #ddd6fe;
  background: #f5f3ff;
  color: #6d28d9;
}

.muted {
  color: #94a3b8;
}

.pagination-row {
  margin-top: 18px;
  border-top: 1px solid #f1f5f9;
  padding-top: 16px;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.pagination-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-actions button {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  padding: 8px 11px;
}

.drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  background: rgb(15 23 42 / 0.32);
}

.task-drawer {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 90;
  display: flex;
  width: min(560px, 100vw);
  height: 100vh;
  flex-direction: column;
  border-left: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: -24px 0 60px rgb(15 23 42 / 0.2);
}

.task-drawer__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e2e8f0;
  padding: 22px 22px 18px;
}

.task-drawer__header h2 {
  max-width: 420px;
  overflow: hidden;
  font-size: 18px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-close-button {
  display: inline-flex;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  color: #64748b;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
}

.drawer-close-button:hover {
  background: #f8fafc;
  color: #0f172a;
}

.drawer-state {
  margin: 22px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  color: #475569;
  padding: 18px;
  font-size: 14px;
  font-weight: 800;
}

.drawer-state--error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.task-drawer__body {
  overflow-y: auto;
  padding: 20px 22px 28px;
}

.drawer-status-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  color: #334155;
  font-size: 14px;
  font-weight: 900;
}

.detail-section {
  border-top: 1px solid #f1f5f9;
  padding: 18px 0;
}

.detail-section:first-of-type {
  border-top: 0;
  padding-top: 0;
}

.detail-section h3 {
  margin-bottom: 12px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 900;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
}

.detail-grid--compact {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.detail-grid div {
  min-width: 0;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  background: #f8fafc;
  padding: 10px;
}

.detail-grid dt {
  margin-bottom: 5px;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.detail-grid dd {
  margin: 0;
  overflow-wrap: anywhere;
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.5;
}

.request-json {
  overflow-x: hidden;
  margin: 12px 0 0;
  border: 1px solid #dbeafe;
  border-radius: 9px;
  background: #eff6ff;
  color: #1e3a8a;
  padding: 10px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-counts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.detail-counts div {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  padding: 12px;
}

.detail-counts span {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.detail-counts strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 24px;
  font-weight: 900;
}

.failure-text {
  min-height: 46px;
  border: 1px solid #fecaca;
  border-radius: 9px;
  background: #fef2f2;
  color: #991b1b;
  padding: 12px;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.drawer-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  position: sticky;
  bottom: 0;
  margin-top: 4px;
  border-top: 1px solid #e2e8f0;
  background: #ffffff;
  padding-top: 16px;
}

.ghost-action-button {
  min-height: 42px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #ffffff;
  color: #334155;
  font-weight: 900;
  cursor: pointer;
}

.ghost-action-button:hover {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
}

@media (max-width: 1050px) {
  .stats-grid,
  .operation-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .admin-topbar,
  .page-title-row,
  .active-task-band,
  .operation-panel__header,
  .task-toolbar,
  .ticket-line,
  .pagination-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .admin-main {
    padding: 20px 16px 36px;
  }

  .stats-grid,
  .operation-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-controls {
    width: 100%;
    flex-direction: column;
  }

  .toolbar-controls select {
    width: 100%;
  }

  .detail-grid,
  .detail-grid--compact,
  .detail-counts,
  .drawer-actions {
    grid-template-columns: 1fr;
  }
}
</style>



