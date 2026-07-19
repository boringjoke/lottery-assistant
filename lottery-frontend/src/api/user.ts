import { get, post } from '@/api/http'
import type { UserProfile, UserProfileUpdateRequest } from '@/types/user'

/**
 * 查询当前登录用户个人中心资料。
 */
export function fetchUserProfile(): Promise<UserProfile> {
  return get<UserProfile>('/api/user/profile/detail')
}

/**
 * 修改当前登录用户基础资料。
 */
export function updateUserProfile(requestBody: UserProfileUpdateRequest): Promise<UserProfile> {
  return post<UserProfile>('/api/user/profile/update', requestBody)
}
