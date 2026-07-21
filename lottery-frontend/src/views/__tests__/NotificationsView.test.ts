import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { logout } from '@/api/auth'
import {
  fetchNotificationPage,
  fetchUnreadNotificationCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from '@/api/notifications'
import { fetchUserProfile } from '@/api/user'
import NotificationsView from '../NotificationsView.vue'

const push = vi.fn()

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>',
  },
  useRoute: () => ({ fullPath: '/profile/notifications' }),
  useRouter: () => ({ push }),
}))

vi.mock('@/api/auth', () => ({
  logout: vi.fn(),
}))

vi.mock('@/api/notifications', () => ({
  fetchNotificationPage: vi.fn(),
  fetchUnreadNotificationCount: vi.fn(),
  markAllNotificationsAsRead: vi.fn(),
  markNotificationAsRead: vi.fn(),
}))

vi.mock('@/api/user', () => ({
  fetchUserProfile: vi.fn(),
}))

const profile = {
  userId: 10,
  nickname: '本地普通用户',
  avatarUrl: null,
  status: 'ACTIVE',
  roles: ['USER'],
  username: 'normal',
  maskedPhone: '138****8000',
  maskedEmail: 'n****l@example.com',
  emailNotificationEnabled: false,
  createTime: '2026-07-18T10:00:00',
  lastLoginTime: '2026-07-18T12:00:00',
}

const unreadNotification = {
  id: 201,
  userId: 10,
  notificationType: 'FAVORITE_WINNING',
  businessType: 'LOTTERY_FAVORITE_WINNING',
  businessKey: 'DLT:26076:FAVORITE:101',
  title: '收藏号码中奖提醒',
  content: '你收藏的「生日组合」在大乐透第 26076 期命中一等奖。',
  readStatus: 'UNREAD' as const,
  readTime: null,
  createTime: '2026-07-21T09:30:00',
  updateTime: '2026-07-21T09:30:00',
}

const readNotification = {
  ...unreadNotification,
  readStatus: 'READ' as const,
  readTime: '2026-07-21T10:00:00',
  updateTime: '2026-07-21T10:00:00',
}

function mockNotificationPage(notifications = [unreadNotification]) {
  vi.mocked(fetchNotificationPage).mockResolvedValue({
    pageNo: 1,
    pageSize: 10,
    total: notifications.length,
    pages: notifications.length ? 1 : 0,
    notifications,
  })
}

describe('NotificationsView', () => {
  beforeEach(() => {
    push.mockReset()
    vi.mocked(fetchUserProfile).mockReset()
    vi.mocked(fetchNotificationPage).mockReset()
    vi.mocked(fetchUnreadNotificationCount).mockReset()
    vi.mocked(markNotificationAsRead).mockReset()
    vi.mocked(markAllNotificationsAsRead).mockReset()
    vi.mocked(logout).mockReset()
    vi.mocked(fetchUserProfile).mockResolvedValue(profile)
    vi.mocked(fetchUnreadNotificationCount).mockResolvedValue(1)
    vi.mocked(markNotificationAsRead).mockResolvedValue({ ...unreadNotification, readStatus: 'READ' })
    vi.mocked(markAllNotificationsAsRead).mockResolvedValue(1)
    vi.mocked(logout).mockResolvedValue(undefined)
    mockNotificationPage()
  })

  it('loads current user and renders notification list', async () => {
    const wrapper = mount(NotificationsView)
    await flushPromises()

    expect(fetchUserProfile).toHaveBeenCalledOnce()
    expect(fetchNotificationPage).toHaveBeenCalledWith({
      pageNo: 1,
      pageSize: 10,
    })
    expect(wrapper.text()).toContain('我的通知')
    expect(wrapper.text()).toContain('收藏号码中奖提醒')
    expect(wrapper.text()).toContain('第 26076 期')
    expect(wrapper.text()).toContain('未读')
    expect(wrapper.text()).toContain('2026-07-21 09:30')
    expect(wrapper.find('.account-trigger__badge').text()).toBe('1')
  })

  it('marks one unread notification as read and refreshes list', async () => {
    vi.mocked(fetchNotificationPage)
      .mockResolvedValueOnce({
        pageNo: 1,
        pageSize: 10,
        total: 1,
        pages: 1,
        notifications: [unreadNotification],
      })
      .mockResolvedValueOnce({
        pageNo: 1,
        pageSize: 10,
        total: 1,
        pages: 1,
        notifications: [readNotification],
      })
    vi.mocked(fetchUnreadNotificationCount)
      .mockResolvedValueOnce(1)
      .mockResolvedValueOnce(0)
    const wrapper = mount(NotificationsView)
    await flushPromises()

    expect(wrapper.find('.account-trigger__badge').text()).toBe('1')

    await wrapper.find('[data-test="read-notification"]').trigger('click')
    await flushPromises()

    expect(markNotificationAsRead).toHaveBeenCalledWith(201)
    expect(fetchNotificationPage).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('通知已标记为已读')
    expect(wrapper.find('.account-trigger__badge').exists()).toBe(false)
  })

  it('marks all notifications as read', async () => {
    const wrapper = mount(NotificationsView)
    await flushPromises()

    const readAllButton = wrapper.findAll('button').find((button) => button.text() === '全部已读')
    expect(readAllButton?.exists()).toBe(true)

    await readAllButton?.trigger('click')
    await flushPromises()

    expect(markAllNotificationsAsRead).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('已标记 1 条通知')
  })

  it('redirects anonymous visitor to login page', async () => {
    vi.mocked(fetchUserProfile).mockRejectedValue(new Error('请先登录'))

    mount(NotificationsView)
    await flushPromises()

    expect(push).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '/profile/notifications' },
    })
    expect(fetchNotificationPage).not.toHaveBeenCalled()
  })
})
