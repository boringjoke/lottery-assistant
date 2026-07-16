import { describe, expect, it } from 'vitest'

import {
  buildSingleAnalyzeLine,
  formatCountdown,
  formatCurrency,
  getNextDltDrawTime,
  formatTwoDigit,
  parseNumberText,
  splitBatchAnalyzeInput,
} from '../lotteryFormat'

describe('lotteryFormat', () => {
  it('formats numbers as two digits', () => {
    expect(formatTwoDigit(1)).toBe('01')
    expect(formatTwoDigit('9')).toBe('09')
    expect(formatTwoDigit('12')).toBe('12')
  })

  it('parses stored lottery number text', () => {
    expect(parseNumberText('1, 5,12,23,35')).toEqual(['01', '05', '12', '23', '35'])
    expect(parseNumberText([3, 11])).toEqual(['03', '11'])
  })

  it('formats currency values for display', () => {
    expect(formatCurrency(1234567)).toBe('¥1,234,567')
    expect(formatCurrency('1234.5')).toBe('¥1,234.5')
    expect(formatCurrency(null)).toBe('-')
  })

  it('builds single analyze request line', () => {
    expect(buildSingleAnalyzeLine(['01', '05', '12', '23', '35'], ['03', '11'])).toBe(
      '01 05 12 23 35 + 03 11',
    )
  })

  it('splits batch analyze input and removes blank lines', () => {
    expect(splitBatchAnalyzeInput('01 05 12 23 35 + 03 11\n\n02 09 16 22 33 + 04 09')).toEqual([
      '01 05 12 23 35 + 03 11',
      '02 09 16 22 33 + 04 09',
    ])
  })

  it('calculates next DLT draw day from a normal day', () => {
    expect(getNextDltDrawTime(new Date(2026, 6, 16, 10, 0))).toEqual({
      drawDate: '2026-07-18',
      weekday: '周六',
      estimatedTime: '20:30左右',
      targetTimeMillis: new Date(2026, 6, 18, 20, 30).getTime(),
      source: '根据大乐透固定开奖日推算',
    })
  })

  it('keeps current draw day before estimated draw time', () => {
    expect(getNextDltDrawTime(new Date(2026, 6, 20, 19, 0)).drawDate).toBe('2026-07-20')
  })

  it('moves to following draw day after estimated draw time', () => {
    expect(getNextDltDrawTime(new Date(2026, 6, 20, 21, 0)).drawDate).toBe('2026-07-22')
  })

  it('formats countdown to target draw time', () => {
    expect(
      formatCountdown(
        new Date(2026, 6, 18, 20, 30, 0).getTime(),
        new Date(2026, 6, 16, 18, 29, 30),
      ),
    ).toEqual({
      days: '2',
      hours: '02',
      minutes: '00',
      seconds: '30',
    })
  })

  it('does not show negative countdown values', () => {
    expect(
      formatCountdown(
        new Date(2026, 6, 18, 20, 30, 0).getTime(),
        new Date(2026, 6, 18, 20, 31, 0),
      ),
    ).toEqual({
      days: '0',
      hours: '00',
      minutes: '00',
      seconds: '00',
    })
  })
})
