import { get, post } from '@/api/http'
import type { AuthSession, CurrentUser, PasswordLoginRequest } from '@/types/auth'

/**
 * 使用用户名、手机号或邮箱加密码登录；Web 端会由后端写入 HttpOnly Cookie。
 */
export function loginWithPassword(requestBody: PasswordLoginRequest): Promise<AuthSession> {
  return post<AuthSession>('/api/auth/login', requestBody)
}

/**
 * 查询当前登录用户，用于页面初始化时恢复登录态。
 */
export function fetchCurrentUser(): Promise<CurrentUser> {
  return get<CurrentUser>('/api/auth/me')
}

/**
 * 退出当前会话；后端会清理 Redis 会话并清除认证 Cookie。
 */
export function logout(): Promise<void> {
  return post<void>('/api/auth/logout')
}
