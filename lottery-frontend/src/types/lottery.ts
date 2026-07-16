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
