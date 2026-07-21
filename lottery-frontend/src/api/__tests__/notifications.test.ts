import { beforeEach, describe, expect, it, vi } from 'vitest'

import { get, post } from '@/api/http'
import {
  fetchNotificationPage,
  fetchUnreadNotificationCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from '@/api/notifications'

vi.mock('@/api/http', () => ({
  get: vi.fn(),
  post: vi.fn(),
}))

describe('notifications api', () => {
  beforeEach(() => {
    vi.mocked(get).mockReset()
    vi.mocked(post).mockReset()
  })

  it('requests notification page with pagination query', async () => {
    vi.mocked(get).mockResolvedValue({
      pageNo: 2,
      pageSize: 10,
      total: 0,
      pages: 0,
      notifications: [],
    })

    await fetchNotificationPage({ pageNo: 2, pageSize: 10 })

    expect(get).toHaveBeenCalledWith('/api/user/notifications/page', {
      pageNo: 2,
      pageSize: 10,
    })
  })

  it('uses camelCase paths for unread count and read all', async () => {
    vi.mocked(get).mockResolvedValue(3)
    vi.mocked(post).mockResolvedValue(3)

    await fetchUnreadNotificationCount()
    await markAllNotificationsAsRead()

    expect(get).toHaveBeenCalledWith('/api/user/notifications/unreadCount')
    expect(post).toHaveBeenCalledWith('/api/user/notifications/readAll')
  })

  it('marks one notification as read', async () => {
    vi.mocked(post).mockResolvedValue({
      id: 8,
      userId: 10,
      notificationType: 'FAVORITE_WINNING',
      businessType: 'LOTTERY_FAVORITE_WINNING',
      businessKey: 'DLT:26076:FAVORITE:101',
      title: '收藏号码中奖提醒',
      content: '你收藏的号码中奖了。',
      readStatus: 'READ',
      readTime: '2026-07-21T10:00:00',
      createTime: '2026-07-21T09:00:00',
      updateTime: '2026-07-21T10:00:00',
    })

    await markNotificationAsRead(8)

    expect(post).toHaveBeenCalledWith('/api/user/notifications/8/read')
  })
})
