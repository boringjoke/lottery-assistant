import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import AnalyzeTab from '../AnalyzeTab.vue'
import { analyzeDltNumbers } from '@/api/lottery'
import type { LotteryDltAnalyzeResponse } from '@/types/lottery'

vi.mock('@/api/lottery', () => ({
  analyzeDltNumbers: vi.fn(),
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

describe('AnalyzeTab', () => {
  beforeEach(() => {
    vi.mocked(analyzeDltNumbers).mockReset()
    vi.mocked(analyzeDltNumbers).mockResolvedValue(analyzeResponse)
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
})
