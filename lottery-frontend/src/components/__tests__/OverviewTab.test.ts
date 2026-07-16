import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/api/http'
import { fetchDltDrawPage, fetchLatestDltDraw } from '@/api/lottery'
import type { LotteryDrawPage } from '@/types/lottery'
import OverviewTab from '../OverviewTab.vue'

vi.mock('@/api/lottery', () => ({
  fetchDltDrawPage: vi.fn(),
  fetchLatestDltDraw: vi.fn(),
}))

const emptyPage: LotteryDrawPage = {
  pageNo: 1,
  pageSize: 5,
  total: 0,
  pages: 0,
  draws: [],
}

describe('OverviewTab', () => {
  beforeEach(() => {
    vi.mocked(fetchLatestDltDraw).mockReset()
    vi.mocked(fetchDltDrawPage).mockReset()
    vi.mocked(fetchDltDrawPage).mockResolvedValue(emptyPage)
  })

  it('shows empty state when latest draw has not been synced yet', async () => {
    vi.mocked(fetchLatestDltDraw).mockRejectedValue(new ApiError('请求失败：404', 'HTTP_STATUS_ERROR', 404))

    const wrapper = mount(OverviewTab)
    await flushPromises()

    expect(wrapper.text()).toContain('暂无最新开奖数据')
    expect(wrapper.text()).not.toContain('开奖概览加载失败')

    wrapper.unmount()
  })

  it('keeps non-404 latest draw errors as overview failures', async () => {
    vi.mocked(fetchLatestDltDraw).mockRejectedValue(new ApiError('请求失败：500', 'HTTP_STATUS_ERROR', 500))

    const wrapper = mount(OverviewTab)
    await flushPromises()

    expect(wrapper.text()).toContain('开奖概览加载失败')

    wrapper.unmount()
  })
})
