import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchCurrentUser, logout } from '@/api/auth'
import {
  fetchSyncTask,
  fetchSyncTasks,
  fetchSyncTaskStatistics,
  retrySyncTask,
  startIssueRangeSync,
} from '@/api/lottery'
import type { LotterySyncTask, LotterySyncTaskPage, LotterySyncTaskStatistics } from '@/types/lottery'
import AdminLotterySyncView from '../AdminLotterySyncView.vue'

const routerMocks = vi.hoisted(() => ({
  push: vi.fn(),
  route: { fullPath: '/admin/lottery-sync' },
}))

vi.mock('@/api/lottery', () => ({
  fetchSyncTasks: vi.fn(),
  fetchSyncTask: vi.fn(),
  fetchSyncTaskStatistics: vi.fn(),
  retrySyncTask: vi.fn(),
  startIssueRangeSync: vi.fn(),
  startDateRangeSync: vi.fn(),
  startHistorySync: vi.fn(),
  syncHistoryPage: vi.fn(),
  syncLatestDraw: vi.fn(),
}))

vi.mock('@/api/auth', () => ({
  fetchCurrentUser: vi.fn(),
  logout: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => routerMocks.route,
  useRouter: () => ({ push: routerMocks.push }),
}))

const statistics: LotterySyncTaskStatistics = {
  runningCount: 1,
  pendingCount: 0,
  failedCount: 2,
  successCountToday: 3,
  latestSuccessTime: '2026-07-16T10:00:00',
  latestFailureTime: '2026-07-16T11:00:00',
  latestFailureMessage: 'crawler timeout',
}

const runningTask: LotterySyncTask = {
  taskNo: 'DLT-HISTORY-RUNNING-001',
  lotteryType: 'DLT',
  syncType: 'HISTORY',
  triggerSource: 'ADMIN',
  status: 'RUNNING',
  requestParams: '{"startPage":1,"pageSize":20}',
  requestParamMap: { startPage: '1', pageSize: '20' },
  startPage: 1,
  currentPage: 2,
  lastSuccessPage: 1,
  failedPage: null,
  pageSize: 20,
  maxPages: 5,
  pageDelayMillis: 1000,
  stopWhenLastPage: true,
  successCount: 20,
  skippedCount: 0,
  failedCount: 0,
  failureReason: null,
  startTime: '2026-07-16T10:00:00',
  finishTime: null,
}

const failedTask: LotterySyncTask = {
  ...runningTask,
  taskNo: 'DLT-HISTORY-FAILED-001',
  status: 'FAILED',
  failedPage: 3,
  failedCount: 1,
  failureReason: 'crawler timeout',
  finishTime: '2026-07-16T10:05:00',
}

const failedIssueRangeTask: LotterySyncTask = {
  ...failedTask,
  taskNo: 'DLT-ISSUE-RANGE-FAILED-001',
  syncType: 'ISSUE_RANGE',
  requestParams: '{"startIssueNo":"26070","endIssueNo":"26076","startPage":1,"pageSize":20,"maxPages":10}',
  requestParamMap: {
    startIssueNo: '26070',
    endIssueNo: '26076',
    startPage: '1',
    pageSize: '20',
    maxPages: '10',
  },
}

const scheduledLatestTask: LotterySyncTask = {
  ...runningTask,
  taskNo: 'DLT-LATEST-SCHEDULED-001',
  syncType: 'LATEST',
  triggerSource: 'SCHEDULED',
  status: 'SUCCESS',
  requestParams: '{"source":"crawler.latest"}',
  requestParamMap: { source: 'crawler.latest' },
  startPage: null,
  currentPage: null,
  lastSuccessPage: null,
  failedPage: null,
  pageSize: null,
  maxPages: null,
  pageDelayMillis: null,
  stopWhenLastPage: null,
  successCount: 1,
  skippedCount: 0,
  failedCount: 0,
  startTime: '2026-07-20T21:45:00',
  finishTime: '2026-07-20T21:45:03',
}

const taskPage: LotterySyncTaskPage = {
  pageNo: 1,
  pageSize: 10,
  total: 2,
  pages: 1,
  status: null,
  tasks: [runningTask, failedTask],
}

function mountView() {
  return mount(AdminLotterySyncView, {
    global: {
      stubs: {
        RouterLink: {
          template: '<a><slot /></a>',
        },
      },
    },
  })
}

describe('AdminLotterySyncView', () => {
  beforeEach(() => {
    routerMocks.push.mockReset()
    vi.mocked(fetchCurrentUser).mockReset()
    vi.mocked(logout).mockReset()
    vi.mocked(fetchSyncTask).mockReset()
    vi.mocked(fetchSyncTaskStatistics).mockReset()
    vi.mocked(fetchSyncTasks).mockReset()
    vi.mocked(startIssueRangeSync).mockReset()
    vi.mocked(retrySyncTask).mockReset()
    vi.mocked(fetchSyncTaskStatistics).mockResolvedValue(statistics)
    vi.mocked(fetchSyncTasks).mockResolvedValue(taskPage)
    vi.mocked(fetchSyncTask).mockResolvedValue(failedTask)
    vi.mocked(fetchCurrentUser).mockResolvedValue({
      userId: 1,
      nickname: '管理员',
      avatarUrl: null,
      roles: ['USER', 'ADMIN'],
    })
    vi.mocked(logout).mockResolvedValue(undefined)
  })

  it('redirects anonymous visitor to login page before loading admin data', async () => {
    vi.mocked(fetchCurrentUser).mockRejectedValue(new Error('请先登录'))
    mountView()
    await flushPromises()

    expect(routerMocks.push).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/admin/lottery-sync' },
    })
    expect(fetchSyncTaskStatistics).not.toHaveBeenCalled()
    expect(fetchSyncTasks).not.toHaveBeenCalled()
  })

  it('shows no permission page for non-admin user', async () => {
    vi.mocked(fetchCurrentUser).mockResolvedValue({
      userId: 2,
      nickname: '普通用户',
      avatarUrl: null,
      roles: ['USER'],
    })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('您无权操作页面')
    expect(wrapper.text()).toContain('返回彩票助手')
    expect(fetchSyncTaskStatistics).not.toHaveBeenCalled()
    expect(fetchSyncTasks).not.toHaveBeenCalled()
  })

  it('loads statistics and task list on mount', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(fetchSyncTaskStatistics).toHaveBeenCalledOnce()
    expect(fetchSyncTasks).toHaveBeenCalledWith({ pageNo: 1, pageSize: 10, status: undefined })
    expect(wrapper.find('.account-trigger').text()).toContain('管理员')
    expect(wrapper.text()).toContain('运行中')
    expect(wrapper.text()).toContain('DLT-HISTORY-RUNNING-001')
  })

  it('shows admin account dropdown and supports logout', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('.account-trigger').trigger('click')

    expect(wrapper.text()).toContain('个人中心')
    expect(wrapper.text()).toContain('数据同步管理')
    expect(wrapper.text()).toContain('退出登录')

    const logoutButton = wrapper
      .findAll('.account-menu button')
      .find((button) => button.text().includes('退出登录'))
    expect(logoutButton).toBeTruthy()

    await logoutButton?.trigger('click')
    await flushPromises()

    expect(logout).toHaveBeenCalledOnce()
    expect(routerMocks.push).toHaveBeenCalledWith('/login')
  })

  it('keeps task list columns compact for scanning', async () => {
    const wrapper = mountView()
    await flushPromises()

    const headers = wrapper.findAll('th').map((header) => header.text())
    expect(headers).toEqual(['任务编号', '票种', '同步类型', '状态', '范围', '时间', '操作'])
  })

  it('keeps maxPages only in the history batch form', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="history-sync-form"]').text()).toContain('最大扫描页数')
    expect(wrapper.find('[data-testid="issue-range-form"]').text()).not.toContain('最大扫描页数')
    expect(wrapper.find('[data-testid="date-range-form"]').text()).not.toContain('最大扫描页数')
  })

  it('collapses and expands sync operation cards', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="history-sync-form"]').exists()).toBe(true)

    await wrapper.find('.collapse-button').trigger('click')

    expect(wrapper.find('[data-testid="history-sync-form"]').exists()).toBe(false)
    expect(wrapper.find('.collapse-button').text()).toContain('展开操作')

    await wrapper.find('.collapse-button').trigger('click')

    expect(wrapper.find('[data-testid="history-sync-form"]').exists()).toBe(true)
    expect(wrapper.find('.collapse-button').text()).toContain('收起操作')
  })

  it('shows lottery type as a dropdown like the public assistant page', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('.ticket-button').trigger('click')

    expect(wrapper.find('.ticket-menu').exists()).toBe(true)
    expect(wrapper.find('.ticket-menu').text()).toContain('大乐透')
    expect(wrapper.find('.ticket-menu').text()).toContain('敬请期待')
  })

  it('opens task detail drawer from the task list', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="detail-DLT-HISTORY-FAILED-001"]').trigger('click')
    await flushPromises()

    expect(fetchSyncTask).toHaveBeenCalledWith('DLT-HISTORY-FAILED-001')
    expect(wrapper.find('.task-drawer').exists()).toBe(true)
    expect(wrapper.find('#taskDetailTitle').text()).toBe('同步任务详情')
    expect(wrapper.find('.task-drawer').text()).toContain('DLT-HISTORY-FAILED-001')
    expect(wrapper.find('.task-drawer').text()).toContain('请求范围')
    expect(wrapper.find('.task-drawer').text()).toContain('crawler timeout')
    expect(wrapper.find('.task-drawer').text()).toMatch(/失败原因[\s\S]*基本信息[\s\S]*请求范围/)
  })

  it('shows request range fields in business order with Chinese labels', async () => {
    vi.mocked(fetchSyncTask).mockResolvedValue(failedIssueRangeTask)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="detail-DLT-HISTORY-FAILED-001"]').trigger('click')
    await flushPromises()

    const requestSectionText =
      wrapper.findAll('.detail-section').find((section) => section.text().includes('请求范围'))?.text() ?? ''
    expect(requestSectionText).toMatch(/范围摘要[\s\S]*起始期号[\s\S]*结束期号[\s\S]*起始页[\s\S]*每页数量[\s\S]*最大扫描页数/)
  })

  it('shows scheduled trigger source with Chinese label in task detail drawer', async () => {
    vi.mocked(fetchSyncTask).mockResolvedValue(scheduledLatestTask)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="detail-DLT-HISTORY-FAILED-001"]').trigger('click')
    await flushPromises()

    const triggerSourceField = wrapper
      .findAll('.detail-grid div')
      .find((field) => field.text().includes('触发来源'))
    expect(triggerSourceField?.text()).toContain('定时任务')
    expect(triggerSourceField?.text()).not.toContain('SCHEDULED')
  })

  it('creates issue range task without maxPages in request body', async () => {
    vi.useFakeTimers()
    vi.mocked(fetchSyncTaskStatistics).mockResolvedValue({ ...statistics, runningCount: 0 })
    vi.mocked(fetchSyncTasks).mockResolvedValue({ ...taskPage, tasks: [failedTask] })
    vi.mocked(startIssueRangeSync).mockResolvedValue({
      taskNo: 'DLT-ISSUE-RANGE-001',
      lotteryType: 'DLT',
      issueNo: null,
      status: 'PENDING',
      successCount: 0,
      skippedCount: 0,
      failedCount: 0,
    })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('#issueStartIssueNo').setValue('26070')
    await wrapper.find('#issueEndIssueNo').setValue('26076')
    await wrapper.find('[data-testid="issue-range-form"]').trigger('submit')
    await flushPromises()

    expect(startIssueRangeSync).toHaveBeenCalledWith({
      startIssueNo: '26070',
      endIssueNo: '26076',
      startPage: 1,
      pageSize: 20,
      pageDelayMillis: 1000,
      stopWhenLastPage: true,
    })
    expect(wrapper.text()).toContain('已创建同步任务 DLT-ISSUE-RANGE-001')

    vi.advanceTimersByTime(4000)
    await flushPromises()

    expect(wrapper.text()).not.toContain('已创建同步任务 DLT-ISSUE-RANGE-001')
    vi.useRealTimers()
  })

  it('retries failed task and refreshes task data', async () => {
    vi.mocked(fetchSyncTaskStatistics).mockResolvedValue({ ...statistics, runningCount: 0 })
    vi.mocked(fetchSyncTasks).mockResolvedValue({ ...taskPage, tasks: [failedTask] })
    vi.mocked(retrySyncTask).mockResolvedValue({
      taskNo: 'DLT-HISTORY-RETRY-001',
      lotteryType: 'DLT',
      issueNo: null,
      status: 'PENDING',
      successCount: 0,
      skippedCount: 0,
      failedCount: 0,
    })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('[data-testid="retry-DLT-HISTORY-FAILED-001"]').trigger('click')
    await flushPromises()

    expect(retrySyncTask).toHaveBeenCalledWith('DLT-HISTORY-FAILED-001')
    expect(fetchSyncTasks).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('已创建重试任务 DLT-HISTORY-RETRY-001')
  })
})
