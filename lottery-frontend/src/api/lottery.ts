import { get, post } from '@/api/http'
import type {
  LotteryDltAnalyzeRequest,
  LotteryDltAnalyzeResponse,
  LotteryDrawDetail,
  LotteryDrawPage,
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
