import { get, post } from '@/api/http'
import type { UserNotification, UserNotificationPage } from '@/types/notification'

/**
 * 分页查询当前登录用户站内通知。
 */
export function fetchNotificationPage(query: { pageNo?: number, pageSize?: number }): Promise<UserNotificationPage> {
  return get<UserNotificationPage>('/api/user/notifications/page', {
    pageNo: query.pageNo,
    pageSize: query.pageSize,
  })
}

/**
 * 查询当前登录用户未读通知数量。
 */
export function fetchUnreadNotificationCount(): Promise<number> {
  return get<number>('/api/user/notifications/unreadCount')
}

/**
 * 将一条通知标记为已读。
 */
export function markNotificationAsRead(notificationId: number): Promise<UserNotification> {
  return post<UserNotification>(`/api/user/notifications/${notificationId}/read`)
}

/**
 * 将全部未读通知标记为已读。
 */
export function markAllNotificationsAsRead(): Promise<number> {
  return post<number>('/api/user/notifications/readAll')
}
