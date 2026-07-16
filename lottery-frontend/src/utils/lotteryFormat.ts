export type MoneyValue = number | string | null | undefined

export interface NextDltDrawTime {
  drawDate: string
  weekday: string
  estimatedTime: string
  targetTimeMillis: number
  source: string
}

export interface CountdownTime {
  days: string
  hours: string
  minutes: string
  seconds: string
}

/**
 * 将号码统一格式化为两位展示文本，兼容后端返回的数字或字符串。
 */
export function formatTwoDigit(value: number | string): string {
  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) {
    return String(value)
  }

  return String(numericValue).padStart(2, '0')
}

/**
 * 解析逗号、中文逗号或空白分隔的号码文本，并统一补零展示。
 */
export function parseNumberText(numbers: string | Array<string | number> | null | undefined): string[] {
  if (Array.isArray(numbers)) {
    return numbers.map(formatTwoDigit)
  }
  if (!numbers || !String(numbers).trim()) {
    return []
  }

  return String(numbers)
    .split(/[,，\s]+/)
    .map((token) => token.trim())
    .filter(Boolean)
    .map(formatTwoDigit)
}

/**
 * 将金额格式化为人民币展示；空值展示为占位符，避免页面出现 NaN。
 */
export function formatCurrency(value: MoneyValue): string {
  if (value === null || value === undefined || value === '') {
    return '-'
  }

  const numericValue = Number(value)
  if (!Number.isFinite(numericValue)) {
    return String(value)
  }

  return `¥${numericValue.toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  })}`
}

/**
 * 将单注输入框中的前区和后区号码拼成后端分析接口要求的一行文本。
 */
export function buildSingleAnalyzeLine(frontNumbers: string[], backNumbers: string[]): string {
  const normalizedFront = frontNumbers.map((item) => item.trim()).filter(Boolean)
  const normalizedBack = backNumbers.map((item) => item.trim()).filter(Boolean)

  return `${normalizedFront.join(' ')} + ${normalizedBack.join(' ')}`
}

/**
 * 将批量输入按行拆分，并忽略空行，保证请求体只包含有效行。
 */
export function splitBatchAnalyzeInput(text: string): string[] {
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
}

/**
 * 将后端票种编码转换为页面展示名称。
 */
export function formatLotteryType(lotteryType: string | null | undefined): string {
  return lotteryType === 'DLT' ? '大乐透' : lotteryType || '-'
}

/**
 * 根据大乐透每周一、三、六开奖的公开规则，推算下一次开奖时间。
 */
export function getNextDltDrawTime(baseDate = new Date()): NextDltDrawTime {
  const drawWeekdays = new Set([1, 3, 6])
  const weekdayTexts = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const estimatedHour = 20
  const estimatedMinute = 30

  for (let offset = 0; offset <= 7; offset += 1) {
    const candidate = new Date(baseDate)
    candidate.setHours(0, 0, 0, 0)
    candidate.setDate(candidate.getDate() + offset)

    if (!drawWeekdays.has(candidate.getDay())) {
      continue
    }

    const hasPassedTodayDraw =
      offset === 0 &&
      (baseDate.getHours() > estimatedHour ||
        (baseDate.getHours() === estimatedHour && baseDate.getMinutes() >= estimatedMinute))

    if (hasPassedTodayDraw) {
      continue
    }

    const year = candidate.getFullYear()
    const month = String(candidate.getMonth() + 1).padStart(2, '0')
    const day = String(candidate.getDate()).padStart(2, '0')
    const targetDate = new Date(candidate)
    targetDate.setHours(estimatedHour, estimatedMinute, 0, 0)

    return {
      drawDate: `${year}-${month}-${day}`,
      weekday: weekdayTexts[candidate.getDay()] ?? '-',
      estimatedTime: '20:30左右',
      targetTimeMillis: targetDate.getTime(),
      source: '根据大乐透固定开奖日推算',
    }
  }

  return {
    drawDate: '-',
    weekday: '-',
    estimatedTime: '20:30左右',
    targetTimeMillis: baseDate.getTime(),
    source: '根据大乐透固定开奖日推算',
  }
}

/**
 * 将目标时间与当前时间的差值格式化为倒计时展示字段。
 */
export function formatCountdown(targetTimeMillis: number, baseDate = new Date()): CountdownTime {
  const remainingSeconds = Math.max(0, Math.floor((targetTimeMillis - baseDate.getTime()) / 1000))
  const days = Math.floor(remainingSeconds / 86400)
  const hours = Math.floor((remainingSeconds % 86400) / 3600)
  const minutes = Math.floor((remainingSeconds % 3600) / 60)
  const seconds = remainingSeconds % 60

  return {
    days: String(days),
    hours: String(hours).padStart(2, '0'),
    minutes: String(minutes).padStart(2, '0'),
    seconds: String(seconds).padStart(2, '0'),
  }
}

/**
 * 根据大乐透奖级名称展示中奖规则，补齐后端当前未返回的展示字段。
 */
export function getDltPrizeRule(prizeName: string): string {
  const rules: Record<string, string> = {
    一等奖: '5 + 2',
    二等奖: '5 + 1',
    三等奖: '5 + 0',
    四等奖: '4 + 2',
    五等奖: '4 + 1',
    六等奖: '3 + 2',
    七等奖: '4 + 0',
    八等奖: '3 + 1 / 2 + 2',
    九等奖: '3 + 0 / 2 + 1 / 1 + 2 / 0 + 2',
  }

  return rules[prizeName] ?? '-'
}

/**
 * 从未知异常中提取可展示错误文案，兜底为通用失败提示。
 */
export function getErrorMessage(error: unknown, fallback = '请求失败，请稍后重试'): string {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}
