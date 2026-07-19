export type FavoriteStatus = 'ACTIVE' | 'CANCELLED'

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
