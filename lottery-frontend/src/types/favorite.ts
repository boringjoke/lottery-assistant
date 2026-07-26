export type FavoriteStatus = 'ACTIVE' | 'CANCELLED'

export interface FavoriteDrawHistoryItem {
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

export interface FavoritePrizeCount {
  prizeLevel: number | null
  prizeName: string
  count: number
}

export interface FavoriteWinningSummary {
  winning: boolean
  totalWinningCount: number
  bestPrizeLevel: number | null
  bestPrizeName: string | null
  bestIssueNo: string | null
  bestDrawDate: string | null
  prizeCounts: FavoritePrizeCount[]
}

export interface LotteryNumberFavorite {
  id: number
  lotteryType: string
  frontNumbers: string
  backNumbers: string
  displayText: string
  favoriteName: string
  remark: string | null
  status: FavoriteStatus
  favoriteTime: string | null
  effectiveTime: string | null
  cancelTime: string | null
  latestDrawResult: FavoriteDrawHistoryItem | null
  winningSummary?: FavoriteWinningSummary | null
}

export interface FavoritePageQuery {
  pageNo?: number
  pageSize?: number
  status?: FavoriteStatus
  keyword?: string
}

export interface LotteryNumberFavoritePage {
  pageNo: number
  pageSize: number
  total: number
  pages: number
  status: FavoriteStatus | null
  keyword: string | null
  favorites: LotteryNumberFavorite[]
}

export interface FavoriteDrawHistoryPage {
  pageNo: number
  pageSize: number
  total: number
  pages: number
  favoriteId: number
  lotteryType: string
  frontNumbers: string
  backNumbers: string
  displayText: string
  latestDrawResult: FavoriteDrawHistoryItem | null
  winningSummary?: FavoriteWinningSummary | null
  results: FavoriteDrawHistoryItem[]
}

export interface LotteryNumberFavoriteUpdateRequest {
  favoriteId: number
  favoriteName: string
  remark: string
}

export interface LotteryNumberFavoriteCreateRequest {
  lotteryType: string
  frontNumbers: number[]
  backNumbers: number[]
  favoriteName?: string
  remark?: string
}
