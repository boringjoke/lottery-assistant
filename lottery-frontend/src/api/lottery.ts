import { get, post, request } from '@/api/http'
import type {
  LotteryDltAnalyzeRequest,
  LotteryDltAnalyzeResponse,
  LotteryDateRangeSyncRequest,
  LotteryDrawSyncResult,
  LotteryDrawDetail,
  LotteryDrawPage,
  LotteryHistorySyncRequest,
  LotteryIssueRangeSyncRequest,
  LotterySyncTask,
  LotterySyncTaskPage,
  LotterySyncTaskPageRequest,
  LotterySyncTaskStatistics,
} from '@/types/lottery'

export interface DltDrawPageQuery {
  pageNo: number
  pageSize: number
  issueNo?: string
  startDate?: string
  endDate?: string
}

/**
 * 查询大乐透最新一期开奖详情。
 */
export function fetchLatestDltDraw(): Promise<LotteryDrawDetail> {
  return get<LotteryDrawDetail>('/api/draws/dlt/latest')
}

/**
 * 分页查询大乐透历史开奖记录。
 */
export function fetchDltDrawPage(query: DltDrawPageQuery): Promise<LotteryDrawPage> {
  return get<LotteryDrawPage>('/api/draws/dlt', { ...query })
}

/**
 * 按期号查询大乐透开奖详情，用于详情弹窗和期号直查。
 */
export function fetchDltDrawDetail(issueNo: string): Promise<LotteryDrawDetail> {
  return get<LotteryDrawDetail>(`/api/draws/dlt/${encodeURIComponent(issueNo)}`)
}

/**
 * 提交一注或多注大乐透号码，分析历史命中情况。
 */
export function analyzeDltNumbers(
  request: LotteryDltAnalyzeRequest,
): Promise<LotteryDltAnalyzeResponse> {
  return post<LotteryDltAnalyzeResponse>('/api/lottery/dlt/analyze', request)
}

/**
 * 查询同步任务状态统计，用于管理页顶部概览。
 */
export function fetchSyncTaskStatistics(): Promise<LotterySyncTaskStatistics> {
  return get<LotterySyncTaskStatistics>('/api/admin/draws/sync/tasks/statistics')
}

/**
 * 分页查询同步任务列表。
 */
export function fetchSyncTasks(requestBody: LotterySyncTaskPageRequest): Promise<LotterySyncTaskPage> {
  return post<LotterySyncTaskPage>('/api/admin/draws/sync/tasks', requestBody)
}

/**
 * 按任务编号查询同步任务详情。
 */
export function fetchSyncTask(taskNo: string): Promise<LotterySyncTask> {
  return get<LotterySyncTask>(`/api/admin/draws/sync/tasks/${encodeURIComponent(taskNo)}`)
}

/**
 * 触发最新一期开奖同步。
 */
export function syncLatestDraw(): Promise<LotteryDrawSyncResult> {
  return post<LotteryDrawSyncResult>('/api/admin/draws/sync/latest')
}

/**
 * 同步指定历史分页。
 */
export function syncHistoryPage(pageNo: number, pageSize: number): Promise<LotteryDrawSyncResult> {
  return request<LotteryDrawSyncResult>('/api/admin/draws/sync/historyPage', {
    method: 'POST',
    query: { pageNo, pageSize },
  })
}

/**
 * 创建历史分页批量同步任务。
 */
export function startHistorySync(requestBody: LotteryHistorySyncRequest): Promise<LotteryDrawSyncResult> {
  return post<LotteryDrawSyncResult>('/api/admin/draws/sync/history', requestBody)
}

/**
 * 创建按期号范围同步任务。
 */
export function startIssueRangeSync(
  requestBody: LotteryIssueRangeSyncRequest,
): Promise<LotteryDrawSyncResult> {
  return post<LotteryDrawSyncResult>('/api/admin/draws/sync/issueRange', requestBody)
}

/**
 * 创建按开奖日期范围同步任务。
 */
export function startDateRangeSync(
  requestBody: LotteryDateRangeSyncRequest,
): Promise<LotteryDrawSyncResult> {
  return post<LotteryDrawSyncResult>('/api/admin/draws/sync/dateRange', requestBody)
}

/**
 * 从失败页重试同步任务。
 */
export function retrySyncTask(taskNo: string): Promise<LotteryDrawSyncResult> {
  return post<LotteryDrawSyncResult>(
    `/api/admin/draws/sync/tasks/${encodeURIComponent(taskNo)}/retry`,
  )
}
