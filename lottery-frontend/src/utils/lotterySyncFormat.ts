import type { LotterySyncTask, LotterySyncTaskStatus, LotterySyncType } from '@/types/lottery'

export const statusOptions: Array<{ value: '' | LotterySyncTaskStatus; label: string }> = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待执行' },
  { value: 'RUNNING', label: '进行中' },
  { value: 'SUCCESS', label: '成功' },
  { value: 'PARTIAL_SUCCESS', label: '部分成功' },
  { value: 'FAILED', label: '失败' },
  { value: 'RETRIED', label: '已重试' },
]

export function statusLabel(status: LotterySyncTaskStatus | null | undefined): string {
  return statusOptions.find((item) => item.value === status)?.label ?? status ?? '-'
}

export function syncTypeLabel(syncType: LotterySyncType | null | undefined): string {
  const labels: Record<LotterySyncType, string> = {
    LATEST: '同步最新开奖',
    HISTORY_PAGE: '同步历史分页',
    HISTORY: '历史分页批量同步',
    ISSUE_RANGE: '按期号范围同步',
    DATE_RANGE: '按日期范围同步',
  }

  return syncType ? labels[syncType] : '-'
}

export function triggerSourceLabel(triggerSource: string | null | undefined): string {
  const labels: Record<string, string> = {
    ADMIN: '管理员',
    SCHEDULED: '定时任务',
    SYSTEM: '系统',
  }

  return triggerSource ? labels[triggerSource] ?? triggerSource : '-'
}

export function formatLotteryType(lotteryType: string | null | undefined): string {
  return lotteryType === 'DLT' ? '大乐透' : lotteryType || '-'
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '-'
  }

  return value.replace('T', ' ').slice(0, 19)
}

export function taskTime(task: LotterySyncTask): string {
  return formatDateTime(task.finishTime ?? task.startTime)
}

export function progressText(task: LotterySyncTask): string {
  const current = task.currentPage ?? task.startPage
  const max = task.maxPages
  if (!current && !max) {
    return '-'
  }

  return max ? `第 ${current ?? '-'} / ${max} 页` : `第 ${current ?? '-'} 页`
}

export function taskScopeText(task: LotterySyncTask): string {
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

export function formatBoolean(value: boolean | null | undefined): string {
  if (value === null || value === undefined) {
    return '-'
  }

  return value ? '是' : '否'
}

export function formatValue(value: string | number | boolean | null | undefined): string {
  if (value === null || value === undefined || value === '') {
    return '-'
  }

  return String(value)
}

export function requestFields(task: LotterySyncTask): Array<{ label: string; value: string }> {
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
