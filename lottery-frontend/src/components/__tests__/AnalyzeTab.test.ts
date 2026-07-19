import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import AnalyzeTab from '../AnalyzeTab.vue'
import { createFavorite } from '@/api/favorites'
import { analyzeDltNumbers } from '@/api/lottery'
import type { LotteryDltAnalyzeResponse } from '@/types/lottery'

const push = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ fullPath: '/lottery-assistant?tab=analyze' }),
  useRouter: () => ({ push }),
}))

vi.mock('@/api/lottery', () => ({
  analyzeDltNumbers: vi.fn(),
}))

vi.mock('@/api/favorites', () => ({
  createFavorite: vi.fn(),
}))

const analyzeResponse: LotteryDltAnalyzeResponse = {
  totalNumberCount: 1,
  analyzedDrawCount: 100,
  winningNumberCount: 0,
  winningHitCount: 0,
  bestPrizeLevel: null,
  bestPrizeName: '未中奖',
  results: [
    {
      lineNo: 1,
      inputText: '01 05 12 23 35 + 03 11',
      displayText: '01 05 12 23 35 + 03 11',
      frontNumbers: [1, 5, 12, 23, 35],
      backNumbers: [3, 11],
      winning: false,
      winningHitCount: 0,
      bestPrizeLevel: null,
      bestPrizeName: '未中奖',
      hitDetails: [],
    },
  ],
}

const analyzeResponseWithHitDetails: LotteryDltAnalyzeResponse = {
  ...analyzeResponse,
  winningNumberCount: 1,
  winningHitCount: 1,
  bestPrizeLevel: 9,
  bestPrizeName: '九等奖',
  results: [
    {
      ...analyzeResponse.results[0],
      winning: true,
      winningHitCount: 1,
      bestPrizeLevel: 9,
      bestPrizeName: '九等奖',
      hitDetails: [
        {
          issueNo: '25001',
          drawDate: '2025-01-01',
          drawFrontNumbers: [1, 5, 12, 23, 35],
          drawBackNumbers: [3, 11],
          frontHitCount: 5,
          backHitCount: 2,
          prizeLevel: 9,
          prizeName: '九等奖',
        },
      ],
    },
  ],
}

const analyzeResponseWithManyHitDetails: LotteryDltAnalyzeResponse = {
  ...analyzeResponseWithHitDetails,
  winningHitCount: 12,
  results: [
    {
      ...analyzeResponseWithHitDetails.results[0],
      winningHitCount: 12,
      hitDetails: Array.from({ length: 12 }, (_, index) => ({
        issueNo: `250${String(index + 1).padStart(2, '0')}`,
        drawDate: `2025-01-${String(index + 1).padStart(2, '0')}`,
        drawFrontNumbers: [1, 5, 12, 23, 35],
        drawBackNumbers: [3, 11],
        frontHitCount: 5,
        backHitCount: 2,
        prizeLevel: 9,
        prizeName: '九等奖',
      })),
    },
  ],
}

describe('AnalyzeTab', () => {
  beforeEach(() => {
    push.mockReset()
    vi.mocked(analyzeDltNumbers).mockReset()
    vi.mocked(createFavorite).mockReset()
    vi.mocked(analyzeDltNumbers).mockResolvedValue(analyzeResponse)
    vi.mocked(createFavorite).mockResolvedValue({
      id: 101,
      lotteryType: 'DLT',
      frontNumbers: '01,05,12,23,35',
      backNumbers: '03,11',
      displayText: '01 05 12 23 35 + 03 11',
      favoriteName: '01 05 12 23 35 + 03 11',
      remark: '来自号码分析结果',
      status: 'ACTIVE',
      favoriteTime: '2026-07-19T10:00:00',
      effectiveTime: '2026-07-19T10:00:00',
      cancelTime: null,
    })
  })

  it('submits single input as backend numbers array', async () => {
    const wrapper = mount(AnalyzeTab)
    const inputs = wrapper.findAll('input.number-input')
    const values = ['01', '05', '12', '23', '35', '03', '11']

    for (const [index, value] of values.entries()) {
      await inputs[index]?.setValue(value)
    }

    await wrapper.find('button.primary-button').trigger('click')

    expect(analyzeDltNumbers).toHaveBeenCalledWith({
      numbers: ['01 05 12 23 35 + 03 11'],
    })
  })

  it('removes letters and symbols from single number inputs', async () => {
    const wrapper = mount(AnalyzeTab)
    const firstInput = wrapper.find('input.number-input')

    await firstInput.setValue('a1#2b')

    expect((firstInput.element as HTMLInputElement).value).toBe('12')
  })

  it('rejects front numbers greater than 35', async () => {
    const wrapper = mount(AnalyzeTab)
    const inputs = wrapper.findAll('input.number-input')
    const values = ['36', '05', '12', '23', '35', '03', '11']

    for (const [index, value] of values.entries()) {
      await inputs[index]?.setValue(value)
    }
    await wrapper.find('button.primary-button').trigger('click')

    expect(analyzeDltNumbers).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('前区号码范围必须为 01-35')
  })

  it('rejects incomplete single input', async () => {
    const wrapper = mount(AnalyzeTab)
    const inputs = wrapper.findAll('input.number-input')
    const values = ['01', '05', '12', '23', '', '03', '11']

    for (const [index, value] of values.entries()) {
      await inputs[index]?.setValue(value)
    }
    await wrapper.find('button.primary-button').trigger('click')

    expect(analyzeDltNumbers).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请输入完整号码：前区 5 个号码，后区 2 个号码')
  })

  it('rejects duplicate single input numbers', async () => {
    const wrapper = mount(AnalyzeTab)
    const inputs = wrapper.findAll('input.number-input')
    const values = ['01', '01', '12', '23', '35', '03', '11']

    for (const [index, value] of values.entries()) {
      await inputs[index]?.setValue(value)
    }
    await wrapper.find('button.primary-button').trigger('click')

    expect(analyzeDltNumbers).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('前区号码不能重复')
  })

  it('clears validation error when switching analyze mode', async () => {
    const wrapper = mount(AnalyzeTab)
    await wrapper.find('button.primary-button').trigger('click')
    expect(wrapper.text()).toContain('请输入完整号码')

    await wrapper.findAll('.mode-tabs button')[1]?.trigger('click')

    expect(wrapper.text()).not.toContain('请输入完整号码')
  })

  it('rejects back numbers greater than 12', async () => {
    const wrapper = mount(AnalyzeTab)
    const inputs = wrapper.findAll('input.number-input')
    const values = ['01', '05', '12', '23', '35', '13', '11']

    for (const [index, value] of values.entries()) {
      await inputs[index]?.setValue(value)
    }
    await wrapper.find('button.primary-button').trigger('click')

    expect(analyzeDltNumbers).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('后区号码范围必须为 01-12')
  })

  it('submits batch input without blank lines', async () => {
    const wrapper = mount(AnalyzeTab)
    await wrapper.findAll('.mode-tabs button')[1]?.trigger('click')
    await wrapper.find('textarea').setValue('01 05 12 23 35 + 03 11\n\n02 09 16 22 33 + 04 09')

    await wrapper.find('button.primary-button').trigger('click')

    expect(analyzeDltNumbers).toHaveBeenCalledWith({
      numbers: ['01 05 12 23 35 + 03 11', '02 09 16 22 33 + 04 09'],
    })
  })

  it('rejects invalid batch input with line number', async () => {
    const wrapper = mount(AnalyzeTab)
    await wrapper.findAll('.mode-tabs button')[1]?.trigger('click')
    await wrapper.find('textarea').setValue('01 05 12 23 36 + 03 11')

    await wrapper.find('button.primary-button').trigger('click')

    expect(analyzeDltNumbers).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('第1行：前区号码范围必须为 01-35')
  })

  it('uses dedicated table style for hit detail list', async () => {
    vi.mocked(analyzeDltNumbers).mockResolvedValue(analyzeResponseWithHitDetails)
    const wrapper = mount(AnalyzeTab)
    const inputs = wrapper.findAll('input.number-input')
    const values = ['01', '05', '12', '23', '35', '03', '11']

    for (const [index, value] of values.entries()) {
      await inputs[index]?.setValue(value)
    }
    await wrapper.find('button.primary-button').trigger('click')
    await flushPromises()

    expect(wrapper.find('table.hit-detail-table').exists()).toBe(true)
    expect(wrapper.findAll('.hit-detail-table th').map((header) => header.text())).toEqual([
      '输入号码',
      '命中期号',
      '命中日期',
      '开奖号码',
      '前区命中',
      '后区命中',
      '奖级',
    ])
  })

  it('paginates hit detail list locally', async () => {
    vi.mocked(analyzeDltNumbers).mockResolvedValue(analyzeResponseWithManyHitDetails)
    const wrapper = mount(AnalyzeTab)
    const inputs = wrapper.findAll('input.number-input')
    const values = ['01', '05', '12', '23', '35', '03', '11']

    for (const [index, value] of values.entries()) {
      await inputs[index]?.setValue(value)
    }
    await wrapper.find('button.primary-button').trigger('click')
    await flushPromises()

    expect(wrapper.findAll('.hit-detail-table tbody tr')).toHaveLength(10)
    expect(wrapper.text()).toContain('共 12 条，第 1 / 2 页')
    expect(wrapper.text()).toContain('25010')
    expect(wrapper.text()).not.toContain('25011')

    await wrapper.find('[data-test="hit-detail-next-page"]').trigger('click')

    expect(wrapper.findAll('.hit-detail-table tbody tr')).toHaveLength(2)
    expect(wrapper.text()).toContain('共 12 条，第 2 / 2 页')
    expect(wrapper.text()).toContain('25011')
    expect(wrapper.text()).toContain('25012')
  })

  it('creates favorite from analyze result when user is logged in', async () => {
    const wrapper = mount(AnalyzeTab, {
      props: {
        currentUser: {
          userId: 10,
          nickname: '本地普通用户',
          avatarUrl: null,
          roles: ['USER'],
        },
      },
    })
    const inputs = wrapper.findAll('input.number-input')
    const values = ['01', '05', '12', '23', '35', '03', '11']

    for (const [index, value] of values.entries()) {
      await inputs[index]?.setValue(value)
    }
    await wrapper.find('button.primary-button').trigger('click')
    await flushPromises()
    await wrapper.find('[data-test="favorite-analyze-number"]').trigger('click')
    await flushPromises()

    expect(createFavorite).toHaveBeenCalledWith({
      lotteryType: 'DLT',
      frontNumbers: [1, 5, 12, 23, 35],
      backNumbers: [3, 11],
      favoriteName: '01 05 12 23 35 + 03 11',
      remark: '来自号码分析结果',
    })
    expect(wrapper.text()).toContain('号码已收藏')
  })

  it('redirects guest to login before favorite analyze result', async () => {
    const wrapper = mount(AnalyzeTab)
    const inputs = wrapper.findAll('input.number-input')
    const values = ['01', '05', '12', '23', '35', '03', '11']

    for (const [index, value] of values.entries()) {
      await inputs[index]?.setValue(value)
    }
    await wrapper.find('button.primary-button').trigger('click')
    await flushPromises()
    await wrapper.find('[data-test="login-before-favorite"]').trigger('click')

    expect(createFavorite).not.toHaveBeenCalled()
    expect(push).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/lottery-assistant?tab=analyze' },
    })
  })
})
