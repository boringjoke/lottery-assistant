import { get, post } from '@/api/http'
import type {
  FavoritePageQuery,
  LotteryNumberFavorite,
  LotteryNumberFavoriteCreateRequest,
  LotteryNumberFavoritePage,
  LotteryNumberFavoriteUpdateRequest,
} from '@/types/favorite'

/**
 * 分页查询当前登录用户收藏号码。
 */
export function fetchFavoritePage(query: FavoritePageQuery): Promise<LotteryNumberFavoritePage> {
  return get<LotteryNumberFavoritePage>('/api/lottery/favorites/page', {
    pageNo: query.pageNo,
    pageSize: query.pageSize,
    status: query.status,
    keyword: query.keyword,
  })
}

/**
 * 新增收藏号码；后端会对重复收藏做幂等处理。
 */
export function createFavorite(requestBody: LotteryNumberFavoriteCreateRequest): Promise<LotteryNumberFavorite> {
  return post<LotteryNumberFavorite>('/api/lottery/favorites/create', requestBody)
}

/**
 * 修改收藏号码名称和备注。
 */
export function updateFavorite(requestBody: LotteryNumberFavoriteUpdateRequest): Promise<LotteryNumberFavorite> {
  return post<LotteryNumberFavorite>('/api/lottery/favorites/update', requestBody)
}

/**
 * 取消当前用户收藏号码。
 */
export function deactivateFavorite(favoriteId: number): Promise<LotteryNumberFavorite> {
  return post<LotteryNumberFavorite>('/api/lottery/favorites/deactivate', {
    favoriteId,
    favoriteName: '',
    remark: '',
  })
}

/**
 * 重新启用当前用户收藏号码。
 */
export function activateFavorite(favoriteId: number): Promise<LotteryNumberFavorite> {
  return post<LotteryNumberFavorite>('/api/lottery/favorites/activate', {
    favoriteId,
    favoriteName: '',
    remark: '',
  })
}

/**
 * 删除已取消收藏号码。
 */
export function deleteFavorite(favoriteId: number): Promise<void> {
  return post<void>('/api/lottery/favorites/delete', {
    favoriteId,
    favoriteName: '',
    remark: '',
  })
}
