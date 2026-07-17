export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
}

export interface LotteryPrizeTier {
  prizeName: string
  stakeCount: number | null
  stakeAmount: number | string | null
  totalPrizeAmount: number | string | null
  sortOrder: number | null
  prizeGroup: string | null
}

export interface LotteryDrawSummary {
  lotteryType: string
  issueNo: string
  drawDate: string
  frontNumbers: string
  backNumbers: string
  poolBalance: number | string | null
  salesAmount: number | string | null
}

export interface LotteryDrawDetail extends LotteryDrawSummary {
  sourceUrl: string | null
  pdfUrl: string | null
  prizeTiers: LotteryPrizeTier[]
}

export interface LotteryDrawPage {
  pageNo: number
  pageSize: number
  total: number
  pages: number
  draws: LotteryDrawSummary[]
}

export interface LotteryDltAnalyzeRequest {
  numbers: string[]
}

export interface LotteryDltAnalyzeHitDetail {
  issueNo: string
  drawDate: string
  drawFrontNumbers: string
  drawBackNumbers: string
  frontHitCount: number
  backHitCount: number
  winning: boolean
  prizeLevel: number | null
  prizeName: string
  ruleVersion: string
}

export interface LotteryDltAnalyzeNumberResult {
  lineNo: number
  inputText: string
  displayText: string
  frontNumbers: number[]
  backNumbers: number[]
  winning: boolean
  winningHitCount: number
  bestPrizeLevel: number | null
  bestPrizeName: string
  hitDetails: LotteryDltAnalyzeHitDetail[]
}

export interface LotteryDltAnalyzeResponse {
  totalNumberCount: number
  analyzedDrawCount: number
  winningNumberCount: number
  winningHitCount: number
  bestPrizeLevel: number | null
  bestPrizeName: string
  results: LotteryDltAnalyzeNumberResult[]
}

export type LotterySyncTaskStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCESS'
  | 'PARTIAL_SUCCESS'
  | 'FAILED'
  | 'RETRIED'

export type LotterySyncType =
  | 'LATEST'
  | 'HISTORY_PAGE'
  | 'HISTORY'
  | 'ISSUE_RANGE'
  | 'DATE_RANGE'

export interface LotteryDrawSyncResult {
  taskNo: string
  lotteryType: string
  issueNo: string | null
  status: LotterySyncTaskStatus
  successCount: number
  skippedCount: number
  failedCount: number
}

export interface LotterySyncTaskStatistics {
  runningCount: number
  pendingCount: number
  failedCount: number
  successCountToday: number
  latestSuccessTime: string | null
  latestFailureTime: string | null
  latestFailureMessage: string | null
}

export interface LotterySyncTask {
  taskNo: string
  lotteryType: string
  syncType: LotterySyncType
  triggerSource: string
  status: LotterySyncTaskStatus
  requestParams: string
  requestParamMap: Record<string, string>
  startPage: number | null
  currentPage: number | null
  lastSuccessPage: number | null
  failedPage: number | null
  pageSize: number | null
  maxPages: number | null
  pageDelayMillis: number | null
  stopWhenLastPage: boolean | null
  successCount: number
  skippedCount: number
  failedCount: number
  failureReason: string | null
  startTime: string | null
  finishTime: string | null
}

export interface LotterySyncTaskPage {
  pageNo: number
  pageSize: number
  total: number
  pages: number
  status: LotterySyncTaskStatus | null
  tasks: LotterySyncTask[]
}

export interface LotterySyncTaskPageRequest {
  pageNo: number
  pageSize: number
  status?: LotterySyncTaskStatus
}

export interface LotteryHistorySyncRequest {
  startPage: number
  pageSize: number
  maxPages: number
  pageDelayMillis: number
  stopWhenLastPage: boolean
}

export interface LotteryIssueRangeSyncRequest {
  startIssueNo: string
  endIssueNo: string
  startPage: number
  pageSize: number
  pageDelayMillis: number
  stopWhenLastPage: boolean
}

export interface LotteryDateRangeSyncRequest {
  startDate: string
  endDate: string
  startPage: number
  pageSize: number
  pageDelayMillis: number
  stopWhenLastPage: boolean
}
