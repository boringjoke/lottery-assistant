import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import HistoryTab from '../HistoryTab.vue'
import { fetchDltDrawPage } from '@/api/lottery'
import type { LotteryDrawPage } from '@/types/lottery'

vi.mock('@/api/lottery', () => ({
  fetchDltDrawPage: vi.fn(),
}))

const emptyPage: LotteryDrawPage = {
  pageNo: 1,
  pageSize: 20,
  total: 0,
  pages: 0,
  draws: [],
}

describe('HistoryTab', () => {
  beforeEach(() => {
    vi.mocked(fetchDltDrawPage).mockReset()
    vi.mocked(fetchDltDrawPage).mockResolvedValue(emptyPage)
  })

  it('queries list with issue number and date range', async () => {
    const wrapper = mount(HistoryTab)
    await flushPromises()

    await wrapper.find('#issueNo').setValue('26076')
    await wrapper.find('#startDate').setValue('2026-07-01')
    await wrapper.find('#endDate').setValue('2026-07-31')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(fetchDltDrawPage).toHaveBeenLastCalledWith({
      pageNo: 1,
      pageSize: 20,
      issueNo: '26076',
      startDate: '2026-07-01',
      endDate: '2026-07-31',
    })
  })

  it('resets filters and reloads the default list', async () => {
    const wrapper = mount(HistoryTab)
    await flushPromises()

    await wrapper.find('#issueNo').setValue('26076')
    await wrapper.find('#startDate').setValue('2026-07-01')
    await wrapper.find('#endDate').setValue('2026-07-31')
    await wrapper.find('button.secondary-button').trigger('click')
    await flushPromises()

    expect(fetchDltDrawPage).toHaveBeenLastCalledWith({
      pageNo: 1,
      pageSize: 20,
      issueNo: undefined,
      startDate: undefined,
      endDate: undefined,
    })
  })
})
